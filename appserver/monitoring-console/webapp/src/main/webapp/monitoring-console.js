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
		
		getSpan: function(widget, numberOfColumns, currentColumn) {
			let span = widget.grid && widget.grid.span ? widget.grid.span : 1;
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
	 * Key used in local stage for the userInterface
	 */
	const LOCAL_UI_KEY = 'fish.payara.monitoring-console.defaultConfigs';
	
	const HOME_PAGE = [
		{ series: 'ns:jvm HeapUsage', grid: { item: 0, column: 0, span: 1} },
		{ series: 'ns:jvm CpuUsage', grid: { item: 1, column: 0, span: 1} },
		{ series: 'ns:jvm ThreadCount', grid: { item: 0, column: 1, span: 1} },
		{ series: 'ns:web RequestCount', grid: { item: 0, column: 2, span: 1} },
		{ series: 'ns:web ActiveSessions', grid: { item: 1, column: 2, span: 1} },
	];
	
	/**
	 * Internal API for managing set model of the user interface.
	 */
	var UI = (function() {

		/**
		 * All page properties must not be must be values as page objects are converted to JSON and back for local storage.
		 * 
		 * {object} - map of pages, name of page as key/field;
		 * {string} name - name of the page
		 * {object} widgets -  map of the chart configurations with series as key
		 * 
		 * Each page is an object describing a page or tab containing one or more graphs by their configuration.
		 */
		var pages = {};
		
		/**
		 * General settings for the user interface
		 */
		var settings = {};
		
		var currentPageId = MonitoringConsoleUtils.getPageId('Home');
		
		/**
		 * Makes sure the page data structure has all required attributes.
		 */
		function sanityCheckPage(page) {
			if (!page.id)
				page.id = MonitoringConsoleUtils.getPageId(page.name);
			if (page.configs) {
				if (!page.widgets)
					page.widgets = page.configs;
				delete page['configs'];
			}
			if (!page.widgets)
				page.widgets = {};
			if (!page.numberOfColumns || page.numberOfColumns < 1)
				page.numberOfColumns = 1;
			// make widgets from array to object if needed
			if (typeof page.widgets === 'array') {
				let widgets = {};
				for (let i = 0; i < page.widgets.length; i++) {
					let widget = page.widgets[i];
					widgets[widget.series] = widget;
				}
				page.widgets = widgets;
			}
			Object.values(page.widgets).forEach(sanityCheckWidget);
			return page;
		}
		
		/**
		 * Makes sure a widget (configiguration for a chart) within a page has all required attributes
		 */
		function sanityCheckWidget(widget) {
			if (!widget.target)
				widget.target = 'chart-' + widget.series.replace(/[^-a-zA-Z0-9_]/g, '_');
			if (!widget.options) {
				widget.options = { 
					beginAtZero: true,
					autoTimeTicks: true,
				};
			}
			if (!widget.grid)
				widget.grid = {};
			if (widget.position)
				delete widget['position'];
			return widget;
		}
		
		function doStore() {
			window.localStorage.setItem(LOCAL_UI_KEY, doExport());
		}
		
		function doDeselect() {
			Object.values(pages[currentPageId].widgets)
				.forEach(widget => widget.selected = false);
		}
		
		function doCreate(name) {
			if (!name)
				throw "New page must have a unique name";
			var id = MonitoringConsoleUtils.getPageId(name);
			if (pages[id])
				throw "A page with name "+name+" already exist";
			let page = sanityCheckPage({name: name});
			pages[page.id] = page;
			currentPageId = page.id;
			return page;
		}
		
		function doImport(userInterface) {
			if (!userInterface) {
				return false;
			}
			let isPagesOnly = !userInterface.pages || !userInterface.settings;
			if (!isPagesOnly)
				settings = userInterface.settings;
			let importedPages = isPagesOnly ? userInterface : userInterface.pages;
			let pagesType = typeof importedPages;
			// override or add the entry in pages from userInterface
			if (pagesType === 'object') {
				for (let [id, page] of Object.entries(importedPages)) {
					try {
						page.id = id;
						pages[id] = sanityCheckPage(page); 
					} catch (ex) {
					}
				}
			} else if (pagesType == 'array') {
				for (let i = 0; i < importedPages.length; i++) {
					try {
						let page = sanityCheckPage(importedPages[i]);
						pages[page.id] = page;
					} catch (ex) {
					}
				}
			}
			if (Object.keys(pages).length == 0) {
				let page = doCreate('Home');
				pages[page.id] = page;
				currentPageId = page.id;
			} else {
				currentPageId = Object.keys(pages)[0];
			}
			doStore();
			return true;
		}
		
		function doExport(prettyPrint) {
			let ui = { pages: pages, settings: settings };
			return prettyPrint ? JSON.stringify(ui, null, 2) : JSON.stringify(ui);
		}
		
		function readTextFile(file){
		    return new Promise(function(resolve, reject){
		        var reader = new FileReader();
		        reader.onload = function(evt){
		            resolve(evt.target.result);
		        };
		        reader.onerror = function(err) {
		            reject(err);
		        };
		        reader.readAsText(file);
		    });
		}
		
		return {
			currentPage: function() {
				return pages[currentPageId];
			},
			
			listPages: function() {
				return Object.values(pages).map(function(page) { 
					return { id: page.id, name: page.name, active: page.id === currentPageId };
				});
			},
			
			$export: function() {
				return doExport(true);
			},
			
			/**
			 * @param {FileList|object} userInterface - a plain user interface configuration object or a file containing such an object
			 * @param {function} onImportComplete - optional function to call when import is done
			 */
			$import: async (userInterface, onImportComplete) => {
				if (userInterface instanceof FileList) {
					let file = userInterface[0];
					if (file) {
						let json = await readTextFile(file);
						doImport(JSON.parse(json));
					}
				} else {
					doImport(userInterface)
				}
				if (onImportComplete)
					onImportComplete();
			},
			
			/**
			 * Loads and returns the userInterface from the local storage
			 */
			load: function() {
				let localStorage = window.localStorage;
				let ui = localStorage.getItem(LOCAL_UI_KEY);
				doImport(ui ? JSON.parse(ui) : undefined);
				return pages[currentPageId];
			},
			
			/**
			 * Creates a new page with given name, ID is derived from name.
			 * While name can be changed later on the ID is fixed.
			 */
			createPage: function(name) {
				return doCreate(name);
			},
			
			renamePage: function(name) {
				pages[currentPageId].name = name;
				doStore();
			},
			
			/**
			 * Deletes the active page and changes to the first page.
			 * Does not delete the last page.
			 */
			deletePage: function() {
				let pageIds = Object.keys(pages);
				if (pageIds.length <= 1)
					return undefined;
				delete pages[currentPageId];
				currentPageId = pageIds[0];
				return pages[currentPageId];
			},
			
			switchPage: function(pageId) {
				if (!pages[pageId])
					return undefined;
				currentPageId = pageId;
				return pages[currentPageId];
			},
			
			removeWidget: function(series) {
				let widgets = pages[currentPageId].widgets;
				if (series && widgets) {
					delete widgets[series];
				}
			},
			
			addWidget: function(series) {
				if (typeof series !== 'string')
					throw 'configuration object requires string property `series`';
				doDeselect();
				let widgets = pages[currentPageId].widgets;
				let widget = { series: series };
				widgets[series] = sanityCheckWidget(widget);
				widget.selected = true;
			},
			
			configureWidget: function(widgetUpdate, series) {
				let selected = series
					? [pages[currentPageId].widgets[series]]
					: Object.values(pages[currentPageId].widgets).filter(widget => widget.selected);
				selected.forEach(widget => widgetUpdate.call(window, widget));
				doStore();
				return selected;
			},
			
			select: function(series) {
				let widget = pages[currentPageId].widgets[series];
				widget.selected = !(widget.selected === true);
				doStore();
				return widget.selected === true;
			},
			
			deselect: function() {
				doDeselect();
				doStore();
			},
			
			selected: function() {
				return Object.values(pages[currentPageId].widgets)
					.filter(widget => widget.selected)
					.map(widget => widget.series);
			},
			
			arrange: function(columns) {
				let page = pages[currentPageId];
				if (!page)
					return [];
				if (columns)
					page.numberOfColumns = columns;
				let numberOfColumns = page.numberOfColumns || 1;
				let widgets = page.widgets;
				let configsByColumn = new Array(numberOfColumns);
				for (let col = 0; col < numberOfColumns; col++)
					configsByColumn[col] = [];
				// insert order widgets
				Object.values(widgets).forEach(function(widget) {
					let column = widget.grid && widget.grid.column ? widget.grid.column : 0;
					configsByColumn[Math.min(Math.max(column, 0), configsByColumn.length - 1)].push(widget);
				});
				// build up rows with columns, occupy spans with empty 
				var layout = new Array(numberOfColumns);
				for (let col = 0; col < numberOfColumns; col++)
					layout[col] = [];
				for (let col = 0; col < numberOfColumns; col++) {
					let orderedConfigs = configsByColumn[col].sort(function (a, b) {
						if (!a.grid || !a.grid.item)
							return -1;
						if (!b.grid || !b.grid.item)
							return 1;
						return a.grid.item - b.grid.item;
					});
					orderedConfigs.forEach(function(widget) {
						let span = MonitoringConsoleUtils.getSpan(widget, numberOfColumns, col);
						let info = { span: span, widget: widget};
						for (let spanX = 0; spanX < span; spanX++) {
							let column = layout[col + spanX];
							if (spanX == 0) {
								if (!widget.grid)
									widget.grid = { column: col, span: span }; // init grid
								widget.grid.item = column.length; // update item position
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
		function syncOptions(widget, chart) {
			let options = chart.options;
			options.scales.yAxes[0].ticks.beginAtZero = widget.options.beginAtZero;
			options.scales.xAxes[0].ticks.source = widget.options.autoTimeTicks ? 'auto' : 'data';
			options.elements.line.tension = widget.options.drawCurves ? 0.4 : 0;
			let time = widget.options.drawAnimations ? 1000 : 0;
			options.animation.duration = time;
			options.responsiveAnimationDuration = time;
	    	let rotation = widget.options.rotateTimeLabels ? 90 : undefined;
	    	options.scales.xAxes[0].ticks.minRotation = rotation;
	    	options.scales.xAxes[0].ticks.maxRotation = rotation;
		}
		
		return {
			/**
			 * Returns a new Chart.js chart object initialised for the given MC level configuration to the charts object
			 */
			getOrCreate: function(widget) {
				let series = widget.series;
				let chart = charts[series];
				if (chart)
					return chart;
				chart = new Chart(widget.target, {
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
									beginAtZero: widget.options.beginAtZero,
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
				syncOptions(widget, chart);
				charts[series] = chart;
				return chart;
			},
			
			clear: function() {
				Object.keys(charts).forEach(doDestroy)
			},
			
			destroy: function(series) {
				doDestroy(series);
			},
			
			update: function(widget) {
				let chart = charts[widget.series];
				if (chart) {
					syncOptions(widget, chart);
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
			UI.load();
			Interval.init(function() {
				let widgets = UI.currentPage().widgets;
				let payload = {
				};
				let instances = $('#cfgInstances').val();
				payload.series = Object.keys(widgets).map(function(series) { 
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
					Object.values(widgets).forEach(function(widget) {
						onDataUpdate({
							widget: widget,
							data: response[widget.series],
							chart: () => Charts.getOrCreate(widget),
						});
					});
				});
				request.fail(function(jqXHR, textStatus) {
					Object.values(widgets).forEach(function(widget) {
						onDataUpdate({
							widget: widget,
							chart: () => Charts.getOrCreate(widget),
						});
					});
				});
			});
			Interval.resume();
			return UI.arrange();
		},
		
		$export: UI.$export,
		$import: function(userInterface, onImportComplete) {
			UI.$import(userInterface, () => onImportComplete(UI.arrange()));
		},
		
		/**
		 * @param {function} consumer - a function with one argument accepting the array of series names
		 */
		listSeries: function(consumer) {
			$.getJSON("api/series/", consumer);
		},
		
		listPages: UI.listPages,
		
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
			
			id: () => UI.currentPage().id,
			name: () => UI.currentPage().name,
			rename: UI.renamePage,
			isEmpty: () => (Object.keys(UI.currentPage().widgets).length === 0),
			
			create: function(name) {
				UI.createPage(name);
				Charts.clear();
				return UI.arrange();
			},
			
			erase: function() {
				if (UI.deletePage()) {
					Charts.clear();
					Interval.tick();
				}
				return UI.arrange();
			},
			
			changeTo: function(pageId) {
				if (UI.switchPage(pageId)) {
					Charts.clear();
					Interval.tick();
				}
				return UI.arrange();
			},
			
			/**
			 * Returns a layout model for the active pages charts and the given number of columns.
			 * This also updates the grid object of the active pages configuration.
			 * 
			 * @param {number} numberOfColumns - the number of columns the charts should be arrange in
			 */
			arrange: UI.arrange,
			
			Widgets: {
				
				add: function(series) {
					UI.addWidget(series);
					Interval.tick();
					return UI.arrange();
				},
				
				remove: function(series) {
					Charts.destroy(series);
					UI.removeWidget(series);
					return UI.arrange();
				},
				
				configure: function(series, widgetUpdate) {
					UI.configureWidget(widgetUpdate, series).forEach(Charts.update);
					return UI.arrange();
				},

				/**
				 * API for the set of selected widgets on the current page.
				 */
				Selection: {
					
					listSeries: UI.selected,
					toggle: UI.select,
					clear: UI.deselect,
					
					/**
					 * @param {function} widgetUpdate - a function accepting chart configuration applied to each chart
					 */
					configure: function(widgetUpdate) {
						UI.configureWidget(widgetUpdate).forEach(Charts.update);
						return UI.arrange();
					},
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
	
    function createMinimumLineDataset(data, points, lineColor) {
		var min = data.observedMin;
		var minPoints = [{t:points[0].t, y:min}, {t:points[points.length-1].t, y:min}];
		
		return {
			data: minPoints,
			label: ' min ',
			fill:  false,
			borderColor: lineColor,
			borderWidth: 1,
			borderDash: [2, 2],
			pointRadius: 0
		};
    }
    
    function createMaximumLineDataset(data, points, lineColor) {
    	var max = data.observedMax;
		var maxPoints = [{t:points[0].t, y:max}, {t:points[points.length-1].t, y:max}];
		
		return {
			data: maxPoints,
			label: ' max ',
			fill:  false,
			borderColor: lineColor,
			borderWidth: 1,
			pointRadius: 0
		};
    }
    
    function createAverageLineDataset(data, points, lineColor) {
		var avg = data.observedSum / data.observedValues;
		var avgPoints = [{t:points[0].t, y:avg}, {t:points[points.length-1].t, y:avg}];
		
		return {
			data: avgPoints,
			label: ' avg ',
			fill:  false,
			borderColor: lineColor,
			borderWidth: 1,
			borderDash: [10, 4],
			pointRadius: 0
		};
    }
    
    function createMainLineDataset(data, points, lineColor, bgColor) {
		return {
			data: points,
			label: data.instance,
			backgroundColor: bgColor,
			borderColor: lineColor,
			borderWidth: 1
		};
    }
    
    function createInstancePoints(points1d) {
    	if (!points1d)
    		return [];
    	let points2d = new Array(points1d.length / 2);
		for (var i = 0; i < points2d.length; i++) {
			points2d[i] = { t: new Date(points1d[i*2]), y: points1d[i*2+1] };
		}
		return points2d;
    }
    
    function createInstancePerSecPoints(points1d) {
    	if (!points1d)
    		return [];
    	let points2d = new Array((points1d.length / 2) - 1);
    	for (var i = 0; i < points2d.length; i++) {
    		let t0 = points1d[i*2];
    		let t1 = points1d[i*2+2];
    		let y0 = points1d[i*2+1];
    		let y1 = points1d[i*2+3];
    		let dt = t1 - t0;
    		let dy = y1 - y0;
    		let y = (dt / 1000) * dy;
    		points2d[i] = { t: new Date(t1), y: y };
    	}
    	return points2d;
    }
    
    function createInstanceDatasets(widget, data, lineColor, bgColor) {
    	if (widget.options.perSec) {
    		return [ createMainLineDataset(data, createInstancePerSecPoints(data.points), lineColor, bgColor) ];
    	}
    	let points = createInstancePoints(data.points)
    	let datasets = [];
    	datasets.push(createMainLineDataset(data, points, lineColor, bgColor));
    	if (points.length > 0 && widget.options.drawAvgLine) {
			datasets.push(createAverageLineDataset(data, points, lineColor));
		}
		if (points.length > 0 && widget.options.drawMinLine && data.observedMin > 0) {
			datasets.push(createMinimumLineDataset(data, points, lineColor));
		}
		if (points.length > 0 && widget.options.drawMaxLine) {
			datasets.push(createMaximumLineDataset(data, points, lineColor));
		}
		return datasets;
    }
    
	return {
		chart: function(update) {
			var data = update.data;
			var widget = update.widget;
			var chart = update.chart();
			var datasets = [];
			for (var j = 0; j < data.length; j++) {
				datasets = datasets.concat(
						createInstanceDatasets(widget, data[j], DEFAULT_LINE_COLORS[j], DEFAULT_BG_COLORS[j]));
			}
			chart.data.datasets = datasets;
			chart.update(0);
		}
	};
})();
