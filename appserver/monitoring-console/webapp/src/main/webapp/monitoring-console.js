/*
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
  
   The contents of this file are subject to the terms of either the GNU
   General Public License Version 2 only ("GPL") or the Common Development
   and Distribution License("CDDL") (collectively, the "License").  You
   may not use this file except in compliance with the License.  You can
   obtain a copy of the License at
   https://github.com/payara/Payara/blob/master/LICENSE.txt
   See the License for the specific
   language governing permissions and limitations under the License.
  
   When distributing the software, include this License Header Notice in each
   file and include the License file at glassfish/legal/LICENSE.txt.
  
   GPL Classpath Exception:
   The Payara Foundation designates this particular file as subject to the "Classpath"
   exception as provided by the Payara Foundation in the GPL Version 2 section of the License
   file that accompanied this code.
  
   Modifications:
   If applicable, add the following below the License Header, with the fields
   enclosed by brackets [] replaced by your own identifying information:
   "Portions Copyright [year] [name of copyright owner]"
  
   Contributor(s):
   If you wish your version of this file to be governed by only the CDDL or
   only the GPL Version 2, indicate your decision by adding "[Contributor]
   elects to include this software in this distribution under the [CDDL or GPL
   Version 2] license."  If you don't indicate a single choice of license, a
   recipient has the option to distribute your version of this file under
   either the CDDL, the GPL Version 2 or to extend the choice of license to
   its licensees as provided above.  However, if you add GPL Version 2 code
   and therefore, elected the GPL Version 2 license, then the option applies
   only if the new code is made subject to such option by the copyright
   holder.
*/
Chart.defaults.global.defaultFontColor = "#fff";

var MonitoringConsole = (function() {
	/**
	 * Key used in local stage for the sets configuration
	 */
	const SETS_KEY = 'fish.payara.monitoring-console.defaultConfigs';
	
	const DEFAULT_BG_COLORS = [
        'rgba(153, 102, 255, 0.2)',
        'rgba(255, 99, 132, 0.2)',
        'rgba(54, 162, 235, 0.2)',
        'rgba(255, 206, 86, 0.2)',
        'rgba(75, 192, 192, 0.2)',
        'rgba(255, 159, 64, 0.2)'
    ];
    const DEFAULT_LINE_COLORS = [
        'rgba(153, 102, 255, 1)',
        'rgba(255, 99, 132, 1)',
        'rgba(54, 162, 235, 1)',
        'rgba(255, 206, 86, 1)',
        'rgba(75, 192, 192, 1)',
        'rgba(255, 159, 64, 1)'
    ];
	
    /**
     * {function} - a function called with text status and error thrown in case data requests did not succeed 
     *              or with no arguments in case of success.
     */
    var connectionCallback;

	/**
	 * {function} - interval function updating the graphs
	 */
	var updater;
	/**
	 * {object} - map of the charts objects for active set as created by Chart.js with series as key
	 */
	var charts = {};
	
	/**
	 * All set properties must not be must be values as set objects are converted to JSON and back for local storage.
	 * 
	 * {object} - map of sets, name of set as key/field;
	 * {string} name - name of the set
	 * {object} configs -  map of the chart configurations with series as key
	 * 
	 * Each set is an object describing a page or tab containing one or more graphs by their configuration.
	 */
	var sets = {};
	var set = createSet('Home');
	sets[set.id] = set;
	
	var activeSetId = set.id;
	
	/**
	 * Loads and returns the configuration from the local storage
	 */
	function loadLocalSets(preConstructionFn) {
		var localStorage = window.localStorage;
		var item = localStorage.getItem(SETS_KEY);
		if (item) {
			var storedSets = JSON.parse(item);
			for (let [id, set] of Object.entries(storedSets)) {
				sets[id] = set; // override or add the entry in sets from local storage
			}
			initActiveSet(preConstructionFn)
		}
	}
	
	function initActiveSet(preConstructionFn) {
		if (sets[activeSetId]) {
			if (preConstructionFn) {
				Object.values(sets[activeSetId].configs).forEach(preConstructionFn);
			}
			updateAllCharts();
		}
	}
	
	/**
	 * Updates the current configuration in the local storage
	 */
	function storeLocalSets() {
		window.localStorage.setItem(SETS_KEY, JSON.stringify(sets));
	}
	
	function customTimeLables(value, index, values) {
		if (values.length == 0 || index == 0)
			return value;
		var span = values[values.length -1].value - values[0].value;
		if (span < 120000) { // less then two minutes
			var lastMinute = new Date(values[index-1].value).getMinutes();
			return new Date(values[index].value).getMinutes() != lastMinute ? value : ''+new Date(values[index].value).getSeconds();
		}
		return value;
	}
	
	function seriesName(configOrSeries) {
		var type = typeof configOrSeries;
		if (type === 'string') {
			return configOrSeries;
		} 
		if (type === 'object') {
			return configOrSeries.series;
		}
		return undefined;
	}
	
	function createSet(name) {
		if (!name)
			throw "New set must have a unique name";
		var id = name.replace(/[^-a-zA-Z]/g, '_').toLowerCase();
		if (sets[id])
			throw "A set with name "+name+" already exist";
		return {
			id: id,
			name: name,
			configs: {},
		};
	}
	
	function getActiveSet() {
		return sets[activeSetId];
	}
	
	/**
	 * Returns a new Chart.js chart object initialised for the given MC level configuration to the charts object
	 */
	function createChart(config) {
		var chart = new Chart(config.target, {
			type: 'line',
			data: {
				datasets: [],
			},
			options: {
				scales: {
					xAxes: [{
						type: 'time',
						gridLines: {
							color: 'rgb(120,120,120)',
						},
						time: {
							minUnit: 'second',
							round: 'second',
						},
						ticks: {
							callback: customTimeLables,
							minRotation: 90,
							maxRotation: 90,
						}
					}],
					yAxes: [{
						display: true,
						gridLines: {
							color: 'rgb(120,120,120)',
						},
						ticks: {
							beginAtZero: config.options.beginAtZero,
							precision:0, // no decimal places in labels
						},
					}],
				},
		        legend: {
		            labels: {
		                filter: function(item, chart) {
		                	return !item.text.startsWith(" ");
		                }
		            }
		        }
			},
		});
		applyChartOptions(config, chart);
		return chart;
	}
	
	/**
	 * Basically translates the MC level configuration options to Chart.js options
	 */
	function applyChartOptions(config, chart) {
		var options = chart.options;
		options.scales.yAxes[0].ticks.beginAtZero = config.options.beginAtZero;
		options.scales.xAxes[0].ticks.source = config.options.autoTimeTicks ? 'auto' : 'data';
		options.elements.line.tension = config.options.drawCurves ? 0.4 : 0;
		var time = config.options.drawAnimations ? 1000 : 0;
		options.animation.duration = time;
		options.responsiveAnimationDuration = time;
    	var rotation = config.options.rotateTimeLabels ? 90 : undefined;
    	options.scales.xAxes[0].ticks.minRotation = rotation;
    	options.scales.xAxes[0].ticks.maxRotation = rotation;
	}
	
	/**
	 * Updates the referenced chart by fetching the newest data from the server and applying it to the given chart.
	 * Should no chart exist (but a config) a new chart is created.
	 */
	function updateChart(configOrSeries) {
		var series = seriesName(configOrSeries);
		if (!series)
			return;
		var set = getActiveSet();
		var config = set.configs[series];
		var chart = charts[series];
		if (!chart) { // might be one newly added
			chart = createChart(config);
			charts[series] = chart;
		}
		$.getJSON('api/series/' + series + '/statistics', function(stats) {
			var datasets = [];
			for (var j = 0; j < stats.length; j++) {
				var instanceStats = stats[j];
				var points = instanceStats.points;
				var data = [];
				if (points) {
					data = new Array(points.length / 2);
					for (var i = 0; i < data.length; i++) {
						data[i] = { t: new Date(points[i*2]), y: points[i*2+1] };
					}
				}
				var bgColor = DEFAULT_BG_COLORS[j];
				var lineColor = DEFAULT_LINE_COLORS[j];
				datasets.push({
					data: data,
					label: instanceStats.instance,
					backgroundColor: bgColor,
					borderColor: lineColor,
					borderWidth: 1
				});
				if (config.options.drawAvgLine) {
					var avg = instanceStats.observedSum / instanceStats.observedValues;
					var avgData = [{t:data[0].t, y:avg}, {t:data[data.length-1].t, y:avg}];
					
					datasets.push({
						data: avgData,
						label: ' avg ',
						fill:  false,
						borderColor: lineColor,
						borderWidth: 1,
						borderDash: [10, 4],
						pointRadius: 0
					});
				}
				if (config.options.drawMinLine && instanceStats.observedMin > 0) {
					var min = instanceStats.observedMin;
					var minData = [{t:data[0].t, y:min}, {t:data[data.length-1].t, y:min}];
					
					datasets.push({
						data: minData,
						label: ' min ',
						fill:  false,
						borderColor: lineColor,
						borderWidth: 1,
						borderDash: [2, 2],
						pointRadius: 0
					});
				}
				if (config.options.drawMaxLine) {
					var max = instanceStats.observedMax;
					var minData = [{t:data[0].t, y:max}, {t:data[data.length-1].t, y:max}];
					
					datasets.push({
						data: minData,
						label: ' max ',
						fill:  false,
						borderColor: lineColor,
						borderWidth: 1,
						pointRadius: 0
					});
				}
			}

			chart.data.datasets = datasets;
			chart.options.title.display = true;
			chart.options.title.text = config.title ? config.title : config.series;
			chart.update(0);
			connectionCallback();
		})
		.fail(function(jqXHR, textStatus, errorThrown) { connectionCallback(textStatus, errorThrown); });
	}
	
	function removeChart(configOrSeries) {
		var series = seriesName(configOrSeries);
		var configs = getActiveSet().configs;
		if (series) {
			delete configs[series];
			destroyChart(series);
		}
	}
	
	function destroyChart(series) {
		if (series) {
			var chart = charts[series];
			if (chart) {
				delete charts[series];
				chart.destroy();
			}
		}
	}
	
	function addChart(config) {
		if (typeof config === 'object') {
			if (typeof config.series !== 'string')
				throw 'configuration object requires string property `series`';
			if (typeof config.target !== 'string')
				throw 'configuration object requires string property `target`';
			var configs = getActiveSet().configs;
			configs[config.series] = config;
		} else {
			throw 'configuration was no object but: ' + config;
		}
	}
	
	function removeAllCharts() {
		if (updater) {
			clearInterval(updater);
		}
		applyToAllCharts(removeChart);
	}
	
	/**
	 * Updates the charts of the current configuration.
	 */
	function updateAllCharts() {
		applyToAllCharts(updateChart);
		if (!updater) {
			updater = setInterval(updateAllCharts, 1000);
		}
	}
	
	/**
	 * Applies a function to all charts.
	 * 
	 * @param {function} fn - a function that accepts a single argument of the string series name
	 */
	function applyToAllCharts(fn) {
		Object.keys(getActiveSet().configs).forEach(fn);
	}
	
	return {
		
		afterDataRequests: function(callback) {
			connectionCallback = callback;
		},
		
		/**
		 * @param {function} consumer - a function with one argument accepting the array of series names
		 */
		fetchSeries: function(consumer) {
			$.getJSON("api/series/", consumer);
		},
		
		getSets: function() {
			return Object.values(sets).map(function(set) { 
				return { id: set.id, name: set.name, active: set.id === getActiveSet().id };
			});
		},
		
		ActiveSet: {
			
			create: function(name) {
				var set = createSet(name);
				sets[set.id] = set;
				storeLocalSets();
				Object.keys(charts).forEach(destroyChart);
				activeSetId = set.id;
				return set;
			},
			
			changeTo: function(setId, preConstructionFn) {
				if (!sets[setId])
					throw "No such set with id: " + setId;
				Object.keys(charts).forEach(destroyChart);
				activeSetId = setId;
				initActiveSet(preConstructionFn);
			},
			
			id: function() {
				return getActiveSet().id;
			},

			title: function() {
				return getActiveSet().title;
			},

			init: function(preConstructionFn) {
				loadLocalSets(preConstructionFn);
			},
			
			/**
			 * @param {function} optionsUpdate - a function accepting chart options applied to each chart
			 */
			configure: function(optionsUpdate) {
				Object.values(getActiveSet().configs).forEach(function(config) {
					optionsUpdate.call(window, config.options);
					var chart = charts[config.series];
					applyChartOptions(config, chart);
					chart.update();
				});
				storeLocalSets();
			},
			
			add: function(config) {
				addChart(config);
				storeLocalSets();
			},
			
			/**
			 * Changes the active charts to those of the passed configuration.
			 */
			update: function() {
				updateAllCharts();
			},
			
			dispose: function(configOrSeries) {
				removeChart(configOrSeries);
				storeLocalSets();
			},
			
			/**
			 * Removes and destroys all chart objects of the active set
			 */
			reset: function() {
				removeAllCharts();
				storeLocalSets();
			},
		},

	};
})();
