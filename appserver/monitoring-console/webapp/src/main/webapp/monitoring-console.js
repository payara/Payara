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

/**
 * A utility with 'static' helper functions that have no side effect.
 * 
 * Extracting such function into this object should help organise the code and allow context independent testing 
 * of the helper functions in the browser.
 * 
 * The MonitoringConsole object is dependent on this object but not vice versa.
 */
var MonitoringConsoleUtils = (function() {
	
	return {
		
		getSpan: function(config, numberOfColumns, currentColumn) {
			let span = config.position && config.position.span ? config.position.span : 1;
			if (typeof span === 'string') {
				if (span === 'full') {
					span = numberOfColumns;
				} else {
					span = parseInt(span);
				}
			}
			if (span > numberOfColumns - currentColumn) {
				span = numberOfColumns - currentColumn;
			}
			return span;
		},
		
		getSeriesId: function(configOrSeries) {
			let type = typeof configOrSeries;
			if (type === 'string')
				return configOrSeries;
			if (type === 'object')
				return configOrSeries.series;
			return undefined;
		},
		
		getPageId: function(name) {
			return name.replace(/[^-a-zA-Z0-9]/g, '_').toLowerCase();
		},
		
		getTimeLabel: function(value, index, values) {
			if (values.length == 0 || index == 0)
				return value;
			let span = values[values.length -1].value - values[0].value;
			if (span < 120000) { // less then two minutes
				let lastMinute = new Date(values[index-1].value).getMinutes();
				return new Date(values[index].value).getMinutes() != lastMinute ? value : ''+new Date(values[index].value).getSeconds();
			}
			return value;
		},
	}
})();

/**
 * The object that manages the internal state of the monitoring console page.
 * 
 * It depends on the MonitoringConsoleUtils object.
 */
var MonitoringConsole = (function() {
	/**
	 * Key used in local stage for the pages configuration
	 */
	const LOCAL_PAGES_KEY = 'fish.payara.monitoring-console.defaultConfigs';
	
	/**
	 * Internal API for managing set of pages.
	 */
	var Pages = (function() {

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
		
		var currentPageId = MonitoringConsoleUtils.getPageId('Home');
		
		function doStore() {
			window.localStorage.setItem(LOCAL_PAGES_KEY, JSON.stringify(pages));
		}
		
		function doDeselect() {
			Object.values(pages[currentPageId].configs)
				.forEach(config => config.selected = false);
		}
		
		function doCreate(name) {
			if (!name)
				throw "New page must have a unique name";
			if (name === 'options')
				throw "Illegal name for a page: " + name;
			var id = MonitoringConsoleUtils.getPageId(name);
			if (pages[id])
				throw "A page with name "+name+" already exist";
			let page = {
				id: id,
				name: name,
				configs: {},
				numberOfColumns: 1,
			};
			pages[page.id] = page;
			currentPageId = page.id;
			return page;
		}
		
		return {
			current: function() {
				return pages[currentPageId];
			},
			
			list: function() {
				return Object.values(pages).filter(page => page.id !== undefined).map(function(page) { 
					return { id: page.id, name: page.name, active: page.id === currentPageId };
				});
			},
			
			/**
			 * Loads and returns the configuration from the local storage
			 */
			load: function() {
				let localStorage = window.localStorage;
				let item = localStorage.getItem(LOCAL_PAGES_KEY);
				if (item) {
					var localPages = JSON.parse(item);
					for (let [id, page] of Object.entries(localPages)) {
						pages[id] = page; // override or add the entry in pages from local storage
					}
				} else {
					let page = doCreate('Home');
					pages[page.id] = page;
					currentPageId = page.id;
				}
				return pages[currentPageId];
			},
			
			/**
			 * Creates a new page with given name, ID is derived from name.
			 * While name can be changed later on the ID is fixed.
			 */
			create: function(name) {
				return doCreate(name);
			},
			
			rename: function(name) {
				pages[currentPageId].name = name;
				doStore();
			},
			
			/**
			 * Deletes the active page and changes to the home page
			 */
			erase: function() {
				let homePageId = MonitoringConsoleUtils.getPageId('Home');
				if (currentPageId === homePageId)
					return undefined;
				delete pages[currentPageId];
				currentPageId = homePageId;
				return pages[currentPageId];
			},
			
			changeTo: function(pageId) {
				if (!pages[pageId])
					return undefined;
				currentPageId = pageId;
				return pages[currentPageId];
			},
			
			remove: function(configOrSeries) {
				let series = MonitoringConsoleUtils.getSeriesId(configOrSeries);
				let configs = pages[currentPageId].configs;
				if (series && configs) {
					delete configs[series];
				}
			},
			
			add: function(config) {
				if (typeof config === 'object') {
					if (typeof config.series !== 'string')
						throw 'configuration object requires string property `series`';
					if (typeof config.target !== 'string')
						throw 'configuration object requires string property `target`';
					doDeselect();
					var configs = pages[currentPageId].configs;
					configs[config.series] = config;
					config.selected = true;
				} else {
					throw 'configuration was no object but: ' + config;
				}
			},
			
			configure: function(configUpdate, series) {
				let selected = series
					? [pages[currentPageId].configs[series]]
					: Object.values(pages[currentPageId].configs).filter(config => config.selected);
				selected.forEach(config => configUpdate.call(window, config));
				doStore();
				return selected;
			},
			
			select: function(configOrSeries) {
				let series = MonitoringConsoleUtils.getSeriesId(configOrSeries);
				let config = pages[currentPageId].configs[series];
				config.selected = !(config.selected === true);
				doStore();
				return config.selected === true;
			},
			
			deselect: function() {
				doDeselect();
				doStore();
			},
			
			selected: function() {
				return Object.values(pages[currentPageId].configs)
					.filter(config => config.selected)
					.map(config => config.series);
			},
			
			layout: function(columns) {
				let page = pages[currentPageId];
				if (!page)
					return [];
				if (columns)
					page.numberOfColumns = columns;
				let numberOfColumns = page.numberOfColumns || 1;
				let configs = page.configs;
				let configsByColumn = new Array(numberOfColumns);
				for (let col = 0; col < numberOfColumns; col++)
					configsByColumn[col] = [];
				// insert order configs
				Object.values(configs).forEach(function(config) {
					let column = config.position && config.position.column ? config.position.column : 0;
					configsByColumn[Math.min(Math.max(column, 0), configsByColumn.length - 1)].push(config);
				});
				// build up rows with columns, occupy spans with empty 
				var layout = new Array(numberOfColumns);
				for (let col = 0; col < numberOfColumns; col++)
					layout[col] = [];
				for (let col = 0; col < numberOfColumns; col++) {
					let orderedConfigs = configsByColumn[col].sort(function (a, b) {
						if (!a.position || !a.position.item)
							return -1;
						if (!b.position || !b.position.item)
							return 1;
						return a.position.item - b.position.item;
					});
					orderedConfigs.forEach(function(config) {
						let span = MonitoringConsoleUtils.getSpan(config, numberOfColumns, col);
						let info = { target: config.target, span: span, series: config.series};
						for (let spanX = 0; spanX < span; spanX++) {
							let column = layout[col + spanX];
							if (spanX == 0) {
								if (!config.position)
									config.position = { column: col, span: span }; // init position
								config.position.item = column.length; // update item position
							}
							for (let spanY = 0; spanY < span; spanY++) {
								column.push(spanX === 0 && spanY === 0 ? info : null);
							}
						}
					});
				}
				doStore();
				return layout;
			},
		};
	})();
	
	/**
	 * Internal API for managing charts on a page
	 */
	var Charts = (function() {

		/**
		 * {object} - map of the charts objects for active page as created by Chart.js with series as key
		 */
		var charts = {};
		
		function doDestroy(series) {
			let chart = charts[series]
			if (chart) {
				delete charts[series];
				chart.destroy();
			}
		}
		
		/**
		 * Basically translates the MC level configuration options to Chart.js options
		 */
		function syncOptions(config, chart) {
			let options = chart.options;
			options.scales.yAxes[0].ticks.beginAtZero = config.options.beginAtZero;
			options.scales.xAxes[0].ticks.source = config.options.autoTimeTicks ? 'auto' : 'data';
			options.elements.line.tension = config.options.drawCurves ? 0.4 : 0;
			let time = config.options.drawAnimations ? 1000 : 0;
			options.animation.duration = time;
			options.responsiveAnimationDuration = time;
	    	let rotation = config.options.rotateTimeLabels ? 90 : undefined;
	    	options.scales.xAxes[0].ticks.minRotation = rotation;
	    	options.scales.xAxes[0].ticks.maxRotation = rotation;
		}
		
		return {
			/**
			 * Returns a new Chart.js chart object initialised for the given MC level configuration to the charts object
			 */
			getOrCreate: function(config) {
				let series = MonitoringConsoleUtils.getSeriesId(config);
				let chart = charts[series];
				if (chart)
					return chart;
				chart = new Chart(config.target, {
					type: 'line',
					data: {
						datasets: [],
					},
					options: {
						scales: {
							xAxes: [{
								type: 'time',
								gridLines: {
									color: 'rgba(100,100,100,0.3)',
									lineWidth: 0.5,
								},
								time: {
									minUnit: 'second',
									round: 'second',
								},
								ticks: {
									callback: MonitoringConsoleUtils.getTimeLabel,
									minRotation: 90,
									maxRotation: 90,
								}
							}],
							yAxes: [{
								display: true,
								gridLines: {
									color: 'rgba(100,100,100,0.7)',
									lineWidth: 0.5,
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
				syncOptions(config, chart);
				charts[series] = chart;
				return chart;
			},
			
			clear: function() {
				Object.keys(charts).forEach(doDestroy)
			},
			
			destroy: function(configOrSeries) {
				doDestroy(MonitoringConsoleUtils.getSeriesId(configOrSeries));
			},
			
			update: function(config) {
				let chart = charts[config.series];
				if (chart) {
					syncOptions(config, chart);
					chart.update();
				}
			},
		};
	})();
	
	/**
	 * Internal API for data loading from server
	 */
	var Interval = (function() {
		
		const DEFAULT_INTERVAL = 2000;
		
	    /**
	     * {function} - a function called with no extra arguments when interval tick occured
	     */
	    var onIntervalTick;

		/**
		 * {function} - underlying interval function causing the ticks to occur
		 */
		var intervalFn;
		
		/**
		 * {number} - tick interval in milliseconds
		 */
		var refreshInterval = DEFAULT_INTERVAL;
		
		function doPause() {
			if (intervalFn) {
				clearInterval(intervalFn);
				intervalFn = undefined;
			}
		}
		
		return {
			
			init: function(onIntervalFn) {
				onIntervalTick = onIntervalFn;
			},
			
			/**
			 * Causes an immediate invocation of the tick target function
			 */
			tick: function() {
				onIntervalTick(); //OBS wrapper function needed as onIntervalTick is set later
			},
			
			/**
			 * Causes an immediate invocation of the tick target function and makes sure an interval is present or started
			 */
			resume: function(atRefreshInterval) {
				onIntervalTick();
				if (atRefreshInterval && atRefreshInterval != refreshInterval) {
					doPause();
					refreshInterval = atRefreshInterval;
				}
				if (refreshInterval === 0)
					refreshInterval = DEFAULT_INTERVAL;
				if (intervalFn === undefined) {
					intervalFn = setInterval(onIntervalTick, refreshInterval);
				}
			},
			
			pause: function() {
				doPause();
			},
		};
	})();
	
	return {
		
		init: function(onDataUpdate) {
			Pages.load();
			Interval.init(function() {
				let configs = Pages.current().configs;
				let payload = {
				};
				let instances = $('#cfgInstances').val();
				payload.series = Object.keys(configs).map(function(series) { 
					return { 
						series: series,
						instances: instances
					}; 
				});
				let request = $.ajax({
					url: 'api/series/data/',
					type: 'POST',
					data: JSON.stringify(payload),
					contentType:"application/json; charset=utf-8",
					dataType:"json",
				});
				request.done(function(response) {
					Object.values(configs).forEach(function(config) {
						onDataUpdate({
							config: config,
							data: response[config.series],
							chart: () => Charts.getOrCreate(config),
						});
					});
				});
				request.fail(function(jqXHR, textStatus) {
					Object.values(configs).forEach(function(config) {
						onDataUpdate({
							config: config,
							chart: () => Charts.getOrCreate(config),
						});
					});
				});
			});
			Interval.resume();
			return Pages.layout();
		},
		
		/**
		 * @param {function} consumer - a function with one argument accepting the array of series names
		 */
		getSeries: function(consumer) {
			$.getJSON("api/series/", consumer);
		},
		
		getPages: Pages.list,
		
		/**
		 * API to control the chart refresh interval.
		 */
		Refresh: {
			pause : Interval.pause,
			resume: () => Interval.resume(2000),
			slow: () => Interval.resume(4000),
			fast: () => Interval.resume(1000),
		},
		
		/**
		 * API to control the active page manipulate the set of charts contained on it.
		 */
		Page: {
			
			id: () => Pages.current().id,
			name: () => Pages.current().name,
			isEmpty: () => (Object.keys(Pages.current().configs).length === 0),
			rename: Pages.rename,
			
			create: function(name) {
				Pages.create(name);
				Charts.clear();
				return Pages.layout();
			},
			erase: function() {
				if (Pages.erase()) {
					Charts.clear();
					Interval.tick();
				}
				return Pages.layout();
			},
			
			changeTo: function(pageId) {
				if (Pages.changeTo(pageId)) {
					Charts.clear();
					Interval.tick();
				}
				return Pages.layout();
			},
			
			add: function(config) {
				Pages.add(config);
				Interval.tick();
				return Pages.layout();
			},
			
			remove: function(configOrSeries) {
				Charts.destroy(configOrSeries);
				Pages.remove(configOrSeries);
				return Pages.layout();
			},
			
			/**
			 * Removes and destroys all chart objects of the active page
			 */
			clear: function() {
				Charts.clear();
				return Pages.layout();
			},
			
			/**
			 * Returns a layout model for the active pages charts and the given number of columns.
			 * This also updates the position object of the active pages configuration.
			 * 
			 * @param {number} numberOfColumns - the number of columns the charts should be arrange in
			 */
			rearrange: Pages.layout,
			
			configure: function(series, configUpdate) {
				Pages.configure(configUpdate, series).forEach(Charts.update);
				return Pages.layout();
			},
			
			/**
			 * API for the set of selected charts within the active page.
			 */
			Selection: {
				
				getSeries: Pages.selected,
				toggle: Pages.select,
				clear: Pages.deselect,
				
				/**
				 * @param {function} configUpdate - a function accepting chart configuration applied to each chart
				 */
				configure: function(configUpdate) {
					Pages.configure(configUpdate).forEach(Charts.update);
					return Pages.layout();
				},
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
				if (data.length > 0 && config.options.drawAvgLine) {
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
				if (data.length > 0 && config.options.drawMinLine && instanceStats.observedMin > 0) {
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
				if (data.length > 0 && config.options.drawMaxLine) {
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
			chart.update(0);
		}
	};
})();
