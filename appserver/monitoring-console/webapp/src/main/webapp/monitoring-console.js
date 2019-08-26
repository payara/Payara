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
	 * Key used in local stage for the pages configuration
	 */
	const LOCAL_PAGES_KEY = 'fish.payara.monitoring-console.defaultConfigs';
	
    /**
     * {function} - a function called when charts are updated with new data.
     */
    var updateCallback;

	/**
	 * {function} - interval function updating the graphs
	 */
	var updater;
	/**
	 * {object} - map of the charts objects for active page as created by Chart.js with series as key
	 */
	var charts = {};
	
	/**
	 * All page properties must not be must be values as page objects are converted to JSON and back for local storage.
	 * 
	 * {object} - map of pages, name of page as key/field;
	 * {string} name - name of the page
	 * {object} configs -  map of the chart configurations with series as key
	 * 
	 * Each page is an object describing a page or tab containing one or more graphs by their configuration.
	 */
	var pages = {};
	var page = createPage('Home');
	pages[page.id] = page;
	
	var activePageId = page.id;
	
	/**
	 * Loads and returns the configuration from the local storage
	 */
	function loadLocalPages() {
		var localStorage = window.localStorage;
		var item = localStorage.getItem(LOCAL_PAGES_KEY);
		if (item) {
			var localPages = JSON.parse(item);
			for (let [id, page] of Object.entries(localPages)) {
				pages[id] = page; // override or add the entry in pages from local storage
			}
			updatePage();
		}
	}
	
	/**
	 * Updates the current configuration in the local storage
	 */
	function storeLocalSets() {
		window.localStorage.setItem(LOCAL_PAGES_KEY, JSON.stringify(pages));
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
	
	function createPage(name) {
		if (!name)
			throw "New page must have a unique name";
		var id = name.replace(/[^-a-zA-Z]/g, '_').toLowerCase();
		if (pages[id])
			throw "A page with name "+name+" already exist";
		return {
			id: id,
			name: name,
			configs: {},
			numberOfColumns: 1,
		};
	}
	
	function getActivePage() {
		return pages[activePageId];
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
		var page = getActivePage();
		var config = page.configs[series];
		var update = { config: config, chart: function() {
			var res = charts[series];
			if (!res) { // might be one newly added
				res = createChart(config);
				charts[series] = res;
			}
			return res;
		}};
		$.getJSON('api/series/' + series + '/statistics', function(data) {
			update.data = data;
			updateCallback(update);
		}).fail(function(jqXHR, textStatus, errorThrown) { 
			updateCallback(update); 
		});
	}
	
	function removeChart(configOrSeries) {
		var series = seriesName(configOrSeries);
		var configs = getActivePage().configs;
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
			var configs = getActivePage().configs;
			configs[config.series] = config;
		} else {
			throw 'configuration was no object but: ' + config;
		}
	}
	
	function clearPage() {
		if (updater) {
			clearInterval(updater);
		}
		applyToAllCharts(removeChart);
	}
	
	/**
	 * Updates the charts of the current configuration.
	 */
	function updatePage() {
		if (pages[activePageId]) {
			applyToAllCharts(updateChart);
			if (!updater) {
				updater = setInterval(updatePage, 1000);
			}
		}
	}
	
	/**
	 * Applies a function to all charts.
	 * 
	 * @param {function} fn - a function that accepts a single argument of the string series name
	 */
	function applyToAllCharts(fn) {
		Object.keys(getActivePage().configs).forEach(fn);
	}
	
	function arrangeLayout() {
		var page = getActivePage();
		if (!page)
			return [];
		var numberOfColumns = page.numberOfColumns || 1;
		var configs = page.configs;
		var configsByColumn = new Array(numberOfColumns);
		for (var col = 0; col < numberOfColumns; col++)
			configsByColumn[col] = [];
		// insert order configs
		Object.values(configs).forEach(function(config) {
			var column = config.position && config.position.column ? config.position.column : 0;
			configsByColumn[Math.min(Math.max(column, 0), configsByColumn.length - 1)].push(config);
		});
		// build up rows with columns, occupy spans with empty 
		var layout = new Array(numberOfColumns);
		for (var col = 0; col < numberOfColumns; col++)
			layout[col] = [];
		for (var col = 0; col < numberOfColumns; col++) {
			var orderedConfigs = configsByColumn[col].sort(function (a, b) {
				if (!a.position || !a.position.item)
					return -1;
				if (!b.position || !b.position.item)
					return 1;
				return a.position.item - b.position.item;
			});
			orderedConfigs.forEach(function(config) {
				var span = config.position && config.position.span ? config.position.span : 1;
				if (typeof span === 'string') {
					if (span === 'full') {
						span = numberOfColumns;
					} else {
						span = parseInt(span);
					}
				}
				if (span > numberOfColumns - col) {
					span = numberOfColumns - col;
				}
				var info = { target: config.target, span: span, series: config.series};
				for (var spanX = 0; spanX < span; spanX++) {
					var column = layout[col + spanX];
					if (spanX == 0) {
						if (!config.position)
							config.position = { column: col, span: span }; // init position
						config.position.item = column.length; // update item position
					}
					for (var spanY = 0; spanY < span; spanY++) {
						column.push(spanX === 0 && spanY === 0 ? info : null);
					}
				}
			});
		}
		return layout;
	}
	
	return {
		
		init: function(callback) {
			updateCallback = callback;
			loadLocalPages();
			return arrangeLayout();
		},
		
		/**
		 * @param {function} consumer - a function with one argument accepting the array of series names
		 */
		fetchSeries: function(consumer) {
			$.getJSON("api/series/", consumer);
		},
		
		pages: function() {
			return Object.values(pages).map(function(page) { 
				return { id: page.id, name: page.name, active: page.id === getActivePage().id };
			});
		},
		
		ActivePage: {
			
			id: function() {
				return getActivePage().id;
			},

			title: function() {
				return getActivePage().title;
			},
			
			/**
			 * @param {function} optionsUpdate - a function accepting chart options applied to each chart
			 */
			configure: function(optionsUpdate) {
				Object.values(getActivePage().configs).forEach(function(config) {
					optionsUpdate.call(window, config.options);
					var chart = charts[config.series];
					applyChartOptions(config, chart);
					chart.update();
				});
				storeLocalSets();
			},			
			
			create: function(name) {
				var page = createPage(name);
				pages[page.id] = page;
				storeLocalSets();
				Object.keys(charts).forEach(destroyChart);
				activePageId = page.id;
				return arrangeLayout();
			},
			
			changeTo: function(setId) {
				if (!pages[setId])
					throw "No such page with id: " + setId;
				Object.keys(charts).forEach(destroyChart);
				activePageId = setId;
				updatePage();
				return arrangeLayout();
			},
			
			add: function(config) {
				addChart(config);
				var layout = arrangeLayout();
				storeLocalSets();
				return layout;
			},
			
			dispose: function(configOrSeries) {
				removeChart(configOrSeries);
				var layout = arrangeLayout();
				storeLocalSets();
				return layout;
			},
			
			/**
			 * Removes and destroys all chart objects of the active page
			 */
			clear: function() {
				clearPage();
				var layout = arrangeLayout();
				storeLocalSets();
				return layout;
			},
			
			/**
			 * Returns a layout model for the active pages charts and the given number of columns.
			 * This also updates the position object of the active pages configuration.
			 * 
			 * @param {number} numberOfColumns - the number of columns the charts should be arrange in
			 */
			rearrange: function(numberOfColumns) {
				getActivePage().numberOfColumns = numberOfColumns;
				var layout = arrangeLayout();
				storeLocalSets();
				return layout;
			},
		},

	};
})();

var MonitoringConsoleRender = (function() {
	
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
	
	return {
		chart: function(update) {
			var stats = update.data;
			var config = update.config;
			var chart = update.chart();
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
		}
	};
})();
