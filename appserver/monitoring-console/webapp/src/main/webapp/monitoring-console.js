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

/*jshint esversion: 8 */

Chart.defaults.global.defaultFontColor = "#fff";

/**
 * The different parts of the Monitoring Console are added as the below properties by the individual files.
 */
var MonitoringConsole =  {
   /**
    * Functions to update the actual HTML page of the MC
    **/
	View: undefined,
   /**
    * Functions of manipulate the model of the MC (often returns a layout that is applied to the View)
    **/ 
	Model: undefined,
   /**
    * Functions specifically to take the data and prepare the display of a line chart using the underlying charting library. 
    **/
	LineChart: undefined,
};
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

/*jshint esversion: 8 */

/**
 * The object that manages the internal state of the monitoring console page.
 * 
 * It depends on the MonitoringConsole.Utils object.
 */
MonitoringConsole.Model = (function() {
	/**
	 * Key used in local stage for the userInterface
	 */
	const LOCAL_UI_KEY = 'fish.payara.monitoring-console.defaultConfigs';
	
	const UI_PRESETS = {
			pages: {
				core: {
					name: 'Core',
					numberOfColumns: 3,
					widgets: [
						{ series: 'ns:jvm HeapUsage',      grid: { item: 0, column: 0, span: 1} },
						{ series: 'ns:jvm CpuUsage',       grid: { item: 1, column: 0, span: 1} },
						{ series: 'ns:jvm ThreadCount',    grid: { item: 0, column: 1, span: 1} },
						{ series: 'ns:web RequestCount',   grid: { item: 0, column: 2, span: 1}, options: { perSec: true, autoTimeTicks: true } },
						{ series: 'ns:web ActiveSessions', grid: { item: 1, column: 2, span: 1} },
					]
				}
			},
			settings: {},
	};

	function getPageId(name) {
    	return name.replace(/[^-a-zA-Z0-9]/g, '_').toLowerCase();
    }

	
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
		
		var currentPageId;
		
		/**
		 * Makes sure the page data structure has all required attributes.
		 */
		function sanityCheckPage(page) {
			if (!page.id)
				page.id = getPageId(page.name);
			if (!page.widgets)
				page.widgets = {};
			if (!page.numberOfColumns || page.numberOfColumns < 1)
				page.numberOfColumns = 1;
			// make widgets from array to object if needed
			if (Array.isArray(page.widgets)) {
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
			var id = getPageId(name);
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
			// override or add the entry in pages from userInterface
			if (Array.isArray(importedPages)) {
				for (let i = 0; i < importedPages.length; i++) {
					try {
						let page = sanityCheckPage(importedPages[i]);
						pages[page.id] = page;
					} catch (ex) {
					}
				}
			} else {
				for (let [id, page] of Object.entries(importedPages)) {
					try {
						page.id = id;
						pages[id] = sanityCheckPage(page); 
					} catch (ex) {
					}
				}
			}
			if (Object.keys(pages).length > 0) {
				currentPageId = Object.keys(pages)[0];
			}
			doStore();
			return true;
		}
		
		function doExport(prettyPrint) {
			let ui = { pages: pages, settings: settings };
			return prettyPrint ? JSON.stringify(ui, null, 2) : JSON.stringify(ui);
		}

		function readTextFile(file) {
          	return new Promise(function(resolve, reject) {
				let reader = new FileReader();
				reader.onload = function(evt){
				  resolve(evt.target.result);
				};
				reader.onerror = function(err) {
				  reject(err);
				};
				reader.readAsText(file);
          	});
      	}

      	function doLayout(columns) {
			let page = pages[currentPageId];
			if (!page)
				return [];
			if (columns)
				page.numberOfColumns = columns;
			let numberOfColumns = page.numberOfColumns || 1;
			let widgets = page.widgets;
			// init temporary and result data structure
			let widgetsByColumn = new Array(numberOfColumns);
			var layout = new Array(numberOfColumns);
			for (let col = 0; col < numberOfColumns; col++) {
				widgetsByColumn[col] = [];
				layout[col] = [];
			}
			// organise widgets in columns
			Object.values(widgets).forEach(function(widget) {
				let column = widget.grid && widget.grid.column ? widget.grid.column : 0;
				widgetsByColumn[Math.min(Math.max(column, 0), widgetsByColumn.length - 1)].push(widget);
			});
			// order columns by item position
			for (let col = 0; col < numberOfColumns; col++) {
				widgetsByColumn[col] = widgetsByColumn[col].sort(function (a, b) {
					if (!a.grid || !a.grid.item)
						return -1;
					if (!b.grid || !b.grid.item)
						return 1;
					return a.grid.item - b.grid.item;
				});
			}
			// do layout by marking cells with item (left top corner in case of span), null (empty) and undefined (spanned)
			for (let col = 0; col < numberOfColumns; col++) {
				let columnWidgets = widgetsByColumn[col];
				for (let item = 0; item < columnWidgets.length; item++) {
					let widget = columnWidgets[item];
					let span = getSpan(widget, numberOfColumns, col);
					let info = { span: span, widget: widget};
					let column0 = layout[col];
					let row0 = getEmptyRowIndex(column0, span);
					for (let spanX = 0; spanX < span; spanX++) {
						let column = layout[col + spanX];
						if (spanX == 0) {
							if (!widget.grid)
								widget.grid = { column: col, span: span }; // init grid
							widget.grid.item = column.length; // update item position
						} else {
							while (column.length < row0)
								column.push(null); // null marks empty cells
						}
						for (let spanY = 0; spanY < span; spanY++) {
							column.push(spanX === 0 && spanY === 0 ? info : undefined);
						}
					}
				}
			}
			// give the layout a uniform row number
			let maxRows = Math.max(numberOfColumns, layout.map(column => column.length).reduce((acc, cur) => acc ? Math.max(acc, cur) : cur));
			for (let col = 0; col < numberOfColumns; col++) {
				while (layout[col].length < maxRows) {
					layout[col].push(null);
				}
			}
			return layout;
      	}

      	function getSpan(widget, numberOfColumns, currentColumn) {
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
      	}

		/**
		 * @return {number} row position in column where n rows are still empty ('null' marks empty)
		 */
      	function getEmptyRowIndex(column, n) {
			return Math.max(column.length, column.findIndex((elem,index,array) => array.slice(index, index + n).every(e => e === null))); 
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
			
			exportPages: function() {
				return doExport(true);
			},
			
			/**
			 * @param {FileList|object} userInterface - a plain user interface configuration object or a file containing such an object
			 * @param {function} onImportComplete - optional function to call when import is done
			 */
			importPages: async (userInterface, onImportComplete) => {
				if (userInterface instanceof FileList) {
					let file = userInterface[0];
					if (file) {
						let json = await readTextFile(file);
						doImport(JSON.parse(json));
					}
				} else {
					doImport(userInterface);
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
				doImport(ui ? JSON.parse(ui) : JSON.parse(JSON.stringify(UI_PRESETS)));
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
				let pageId = getPageId(name);
				if (pages[pageId])
					return false;
				let page = pages[currentPageId];
				page.name = name;
				page.id = pageId;
				pages[pageId] = page;
				delete pages[currentPageId];
				currentPageId = pageId;
				doStore();
				return true;
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

			resetPage: function() {
				let presets = UI_PRESETS;
				if (presets && presets.pages && presets.pages[currentPageId]) {
					let preset = presets.pages[currentPageId];
					pages[currentPageId] = sanityCheckPage(JSON.parse(JSON.stringify(preset)));
					doStore();
					return true;
				}
				return false;
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
				widget.selected = widget.selected !== true;
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
				let layout = doLayout(columns);
				doStore();
				return layout;
			},
			
			/*
			 * Settings
			 */
			
			showSettings: function() {
				return settings.display === true
					|| Object.keys(pages[currentPageId].widgets).length == 0
					|| Object.values(pages[currentPageId].widgets).filter(widget => widget.selected).length > 0;
			},
			openSettings: function() {
				settings.display = true;
				doStore();
			},
			closeSettings: function() {
				settings.display = false;
				doDeselect();
				doStore();
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
			let chart = charts[series];
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
	    	options.legend.display = widget.options.showLegend === true;
		}

		function getTimeLabel(value, index, values) {
			if (values.length == 0 || index == 0)
				return value;
			let span = values[values.length -1].value - values[0].value;
			if (span < 120000) { // less then two minutes
				let lastMinute = new Date(values[index-1].value).getMinutes();
				return new Date(values[index].value).getMinutes() != lastMinute ? value : ''+new Date(values[index].value).getSeconds();
			}
			return value;
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
									callback: getTimeLabel,
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
				Object.keys(charts).forEach(doDestroy);
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
	
	function doInit(onDataUpdate) {
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
	}

	function doConfigureSelection(widgetUpdate) {
		UI.configureWidget(widgetUpdate).forEach(Charts.update);
		return UI.arrange();
	}

	function doConfigureWidget(series, widgetUpdate) {
		UI.configureWidget(widgetUpdate, series).forEach(Charts.update);
		return UI.arrange();
	}

	/**
	 * The public API object ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 */
	return {
		
		init: doInit,
		
		/**
		 * @param {function} consumer - a function with one argument accepting the array of series names
		 */
		listSeries: (consumer) => $.getJSON("api/series/", consumer),

		listPages: UI.listPages,
		exportPages: UI.exportPages,
		importPages: function(userInterface, onImportComplete) {
			UI.importPages(userInterface, () => onImportComplete(UI.arrange()));
		},
		
		/**
		 * API to control the chart refresh interval.
		 */
		Refresh: {
			pause: Interval.pause,
			resume: () => Interval.resume(2000),
			slow: () => Interval.resume(4000),
			fast: () => Interval.resume(1000),
		},
		
		Settings: {
			isDispayed: UI.showSettings,
			open: UI.openSettings,
			close: UI.closeSettings,
			toggle: () => UI.showSettings() ? UI.closeSettings() : UI.openSettings(),
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
			
			reset: function() {
				if (UI.resetPage()) {
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
				
				configure: doConfigureWidget,

				moveLeft: (series) => doConfigureWidget(series, function(widget) {
	                if (!widget.grid.column || widget.grid.column > 0) {
	                    widget.grid.item = undefined;
	                    widget.grid.column = widget.grid.column ? widget.grid.column - 1 : 1;
	                }
	            }),

	            moveRight: (series) => doConfigureWidget(series, function(widget) {
	                if (!widget.grid.column || widget.grid.column < 4) {
	                    widget.grid.item = undefined;
	                    widget.grid.column = widget.grid.column ? widget.grid.column + 1 : 1;
	                }
	            }),

	            spanMore: (series) => doConfigureWidget(series, function(widget) {
	                if (! widget.grid.span || widget.grid.span < 4) {
	                    widget.grid.span = !widget.grid.span ? 2 : widget.grid.span + 1;
	                }
	            }),

	            spanLess: (series) => doConfigureWidget(series, function(widget) {
	            	if (widget.grid.span > 1) {
	                    widget.grid.span -= 1;
	                }
	            }),

				/**
				 * API for the set of selected widgets on the current page.
				 */
				Selection: {
					
					listSeries: UI.selected,
					isSingle: () => UI.selected().length == 1,
					first: () => UI.currentPage().widgets[UI.selected()[0]],
					toggle: UI.select,
					clear: UI.deselect,
					
					/**
					 * @param {function} widgetUpdate - a function accepting chart configuration applied to each chart
					 */
					configure: doConfigureSelection,

				},
			},
			
		},

	};
})();
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

/*jshint esversion: 8 */

MonitoringConsole.LineChart = (function() {
	
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
		let min = data.observedMin;
		let minPoints = [{t:points[0].t, y:min}, {t:points[points.length-1].t, y:min}];
		
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
    	let max = data.observedMax;
		let maxPoints = [{t:points[0].t, y:max}, {t:points[points.length-1].t, y:max}];
		
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
		let avg = data.observedSum / data.observedValues;
		let avgPoints = [{t:points[0].t, y:avg}, {t:points[points.length-1].t, y:avg}];
		
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
		for (let i = 0; i < points2d.length; i++) {
			points2d[i] = { t: new Date(points1d[i*2]), y: points1d[i*2+1] };
		}
		return points2d;
    }
    
    function createInstancePerSecPoints(points1d) {
    	if (!points1d)
    		return [];
    	let points2d = new Array((points1d.length / 2) - 1);
    	for (let i = 0; i < points2d.length; i++) {
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
    	let points = createInstancePoints(data.points);
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
		onDataUpdate: function(update) {
			let data = update.data;
			let widget = update.widget;
			let chart = update.chart();
			let datasets = [];
			for (let j = 0; j < data.length; j++) {
				datasets = datasets.concat(
						createInstanceDatasets(widget, data[j], DEFAULT_LINE_COLORS[j], DEFAULT_BG_COLORS[j]));
			}
			chart.data.datasets = datasets;
			chart.update(0);
		}
	};
})();
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

/*jshint esversion: 8 */

/**
 *
 **/
MonitoringConsole.View = (function() {

    /**
     * Updates the DOM with the page navigation tabs so it reflects current model state
     */ 
    function updatePageNavigation() {
        let nav = $("#pagesTabs"); 
        nav.empty();
        MonitoringConsole.Model.listPages().forEach(function(page) {
            let tabId = page.id + '-tab';
            let css = "page-tab" + (page.active ? ' page-selected' : '');
            let pageTab = $('<span/>', {id: tabId, "class": css, text: page.name});
            if (page.active) {
                pageTab.click(function() {
                    MonitoringConsole.Model.Settings.toggle();
                    updatePageAndSelectionSettings();
                });
            } else {
                pageTab.click(() => onPageChange(MonitoringConsole.Model.Page.changeTo(page.id)));                
            }
            nav.append(pageTab);
        });
        let addPage = $('<span/>', {id: 'addPageTab', 'class': 'page-tab'}).html('&plus;');
        addPage.click(() => onPageChange(MonitoringConsole.Model.Page.create('(Unnamed)')));
        nav.append(addPage);
    }

    /**
     * Updates the DOM with the page and selection settings so it reflects current model state
     */ 
    function updatePageAndSelectionSettings() {
        let panelConsole = $('#console');
        if (MonitoringConsole.Model.Settings.isDispayed()) {
            if (!panelConsole.hasClass('state-show-settings')) {
                panelConsole.addClass('state-show-settings');                
            }
            let panelSettings = $('#panel-settings');
            panelSettings
                .empty()
                .append($('<button/>', { title: 'Delete current page', 'class': 'btnIcon btnClose' }).html('&times;').click(MonitoringConsole.View.onPageDelete))
                .append(createPageSettings())
                .append(createDataSettings());
            if (MonitoringConsole.Model.Page.Widgets.Selection.isSingle()) {
                panelSettings.append(createWidgetSettings(MonitoringConsole.Model.Page.Widgets.Selection.first()));
            }
        } else {
            panelConsole.removeClass('state-show-settings');
        }
    }

    /**
     * Each chart needs to be in a relative positioned box to allow responsive sizing.
     * This fuction creates this box including the canvas element the chart is drawn upon.
     */
    function createWidgetTargetContainer(cell) {
        let boxId = cell.widget.target + '-box';
        let box = $('#'+boxId);
        if (box.length > 0)
            return box.first();
        box = $('<div/>', { id: boxId, "class": "chart-box" });
        let win = $(window);
        box.append($('<canvas/>',{ id: cell.widget.target }));
        return box;
    }

    function camelCaseToWords(str) {
        return str.replace(/([A-Z]+)/g, " $1").replace(/([A-Z][a-z])/g, " $1");
    }

    function formatSeriesName(widget) {
        let series = widget.series;
        let endOfTags = series.lastIndexOf(' ');
        let text = endOfTags <= 0 
           ? camelCaseToWords(series) 
           : '<i>'+series.substring(0, endOfTags)+'</i> '+camelCaseToWords(series.substring(endOfTags + 1));
        if (widget.options.perSec) {
            text += ' <i>(per second)</i>';
        }
        return text;
    }

    function createWidgetToolbar(cell) {
        let series = cell.widget.series;
        return $('<div/>', {"class": "caption-bar"})
            .append($('<h3/>', {title: 'Select '+series}).html(formatSeriesName(cell.widget))
                .click(() => onWidgetToolbarClick(cell.widget)))
            .append(createToolbarButton('Remove chart from page', '&times;', onWidgetDelete))
            .append(createToolbarButton('Enlarge this chart', '&plus;', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanMore(series))))
            .append(createToolbarButton('Shrink this chart', '&minus;', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanLess(series))))
            .append(createToolbarButton('Move to right', '&#9655;', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveRight(series))))
            .append(createToolbarButton('Move to left', '&#9665;', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveLeft(series))));
    }

    function createToolbarButton(title, icon, onClick) {
        return $('<button/>', { "class": "btnIcon", title: title}).html(icon).click(onClick);
    }

    /**
     * Creates a checkbox to configure the attributes of a widget.
     *
     * @param {boolean} isChecked - if the created checkbox should be checked
     * @param {function} onChange - a function accepting two arguments: the updated widget and the checked state of the checkbox after a change
     */
    function createConfigurationCheckbox(isChecked, onChange) {
        return $("<input/>", { type: 'checkbox', checked: isChecked })
            .on('change', function() {
                let checked = this.checked;
                MonitoringConsole.Model.Page.Widgets.Selection.configure((widget) => onChange(widget, checked));
            });
    }

    function createSettingsSliderRow(label, min, max, value, onChange) {
        return createSettingsRow(label, () => $('<input/>', {type: 'range', min:min, max:max, value: value})
            .on('input', function() {  
                let val = this.valueAsNumber;
                onPageUpdate(MonitoringConsole.Model.Page.Widgets.Selection.configure((widget) => onChange(widget, val)));
            }));
    }

    function createWidgetSettings(widget) {
        return createSettingsTable('settings-widget')
            .append(createSettingsHeaderRow('Render Options'))
            .append(createSettingsCheckboxRow('Per Second', widget.options.perSec, (widget, checked) => widget.options.perSec = checked))
            .append(createSettingsCheckboxRow('Begin at Zero', widget.options.beginAtZero, (widget, checked) => widget.options.beginAtZero = checked))
            .append(createSettingsCheckboxRow('Automatic Labels', widget.options.autoTimeTicks, (widget, checked) => widget.options.autoTimeTicks = checked))
            .append(createSettingsCheckboxRow('Use Bezier Curves', widget.options.drawCurves, (widget, checked) => widget.options.drawCurves = checked))
            .append(createSettingsCheckboxRow('Use Animations', widget.options.drawAnimations, (widget, checked) => widget.options.drawAnimations = checked))
            .append(createSettingsCheckboxRow('Label X-Axis at 90°', widget.options.rotateTimeLabels, (widget, checked) => widget.options.rotateTimeLabels = checked))
            .append(createSettingsHeaderRow('Chart Options'))
            .append(createSettingsCheckboxRow('Show Average', widget.options.drawAvgLine, (widget, checked) => widget.options.drawAvgLine = checked))
            .append(createSettingsCheckboxRow('Show Minimum', widget.options.drawMinLine, (widget, checked) => widget.options.drawMinLine = checked))
            .append(createSettingsCheckboxRow('Show Maximum', widget.options.drawMaxLine, (widget, checked) => widget.options.drawMaxLine = checked))
            .append(createSettingsCheckboxRow('Show Legend', widget.options.showLegend, (widget, checked) => widget.options.showLegend = checked))
            .append(createSettingsHeaderRow('Layout'))
            .append(createSettingsSliderRow('Span', 1, 4, widget.grid.span || 1, (widget, value) => widget.grid.span = value))
            .append(createSettingsSliderRow('Column', 1, 4, 1 + (widget.grid.column || 0), (widget, value) => widget.grid.column = value - 1));
    }

    function createPageSettings() {
        let widgetsSelection = $('<select/>');
        MonitoringConsole.Model.listSeries(function(names) {
            let lastNs;
            $.each(names, function() {
                let key = this; //.replace(/ /g, ',');
                let ns =  this.substring(3, this.indexOf(' '));
                let $option = $("<option />").val(key).text(this.substring(this.indexOf(' ')));
                if (ns == lastNs) {
                    widgetsSelection.find('optgroup').last().append($option);
                } else {
                    let group = $('<optgroup/>').attr('label', ns);
                    group.append($option);
                    widgetsSelection.append(group);
                }
                lastNs = ns;
            });
        });            
        return createSettingsTable('settings-page')
            .append(createSettingsHeaderRow('Page'))
            .append(createSettingsRow('Name', () => $('<input/>', { type: 'text', value: MonitoringConsole.Model.Page.name() })
                .on("propertychange change keyup paste input", function() {
                    if (MonitoringConsole.Model.Page.rename(this.value)) {
                        updatePageNavigation();                        
                    }
                })))
            .append(createSettingsRow('Widgets', () => $('<span/>')
                .append(widgetsSelection)
                .append($('<button/>', {title: 'Add selected metric', text: 'Add'})
                    .click(() => onPageUpdate(MonitoringConsole.Model.Page.Widgets.add(widgetsSelection.val()))))
                ));
    }

    function createDataSettings() {
        let instanceSelection = $('<select />', {multiple: true});
        $.getJSON("api/instances/", function(instances) {
            for (let i = 0; i < instances.length; i++) {
                instanceSelection.append($('<option/>', { value: instances[i], text: instances[i], selected:true}))
            }
        });
        return createSettingsTable('settings-data')
            .append(createSettingsHeaderRow('Data'))
            .append(createSettingsRow('Instances', () => instanceSelection));
    }

    function createSettingsHeaderRow(caption) {
        return $('<tr/>').append($('<th/>', {colspan: 2, text: caption}).click(function() {
            let tr = $(this).closest('tr').next();
            while (tr.length > 0 && !tr.children('th').length > 0) {
                tr.children().toggle();
                tr = tr.next();
            }
        }));
    }

    function createSettingsCheckboxRow(label, checked, onChange) {
        return createSettingsRow(label, () => createConfigurationCheckbox(checked, onChange));
    }

    function createSettingsTable(id) {
        return $('<table />', { 'class': 'settings', id: id });
    }

    function createSettingsRow(label, createInput) {
        return $('<tr/>').append($('<td/>').text(label)).append($('<td/>').append(createInput()));   
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~[ Event Handlers ]~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    function onWidgetToolbarClick(widget) {
        MonitoringConsole.Model.Page.Widgets.Selection.toggle(widget.series);
        onWidgetUpdate(widget);
        updatePageAndSelectionSettings();
    }

    function onWidgetDelete() {
        if (window.confirm('Do you really want to remove the chart from the page?')) {
            onPageUpdate(MonitoringConsole.Model.Page.Widgets.remove(series));
        }
    }

    function onPageExport(filename, text) {
        let pom = document.createElement('a');
        pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
        pom.setAttribute('download', filename);

        if (document.createEvent) {
            let event = document.createEvent('MouseEvents');
            event.initEvent('click', true, true);
            pom.dispatchEvent(event);
        }
        else {
            pom.click();
        }
    }

    /**
     * This function is called when data was received or was failed to receive so the new data can be applied to the page.
     *
     * Depending on the update different content is rendered within a chart box.
     */
    function onDataUpdate(update) {
        let boxId = update.widget.target + '-box';
        let box = $('#'+boxId);
        if (box.length == 0) {
            if (console && console.log)
                console.log('WARN: Box for chart ' + update.widget.series + ' not ready.');
            return;
        }
        let td = box.closest('.widget');
        if (update.data) {
            td.children('.status-nodata').hide();
            let points = update.data[0].points;
            if (points.length == 4 && points[1] === points[3] && !update.widget.options.perSec) {
                if (td.children('.stable').length == 0) {
                    let info = $('<div/>', { 'class': 'stable' });
                    info.append($('<span/>', { text: points[1] }));
                    td.append(info);
                    box.hide();
                }
            } else {
                td.children('.stable').remove();
                box.show();
                MonitoringConsole.LineChart.onDataUpdate(update);
            }
        } else {
            td.children('.status-nodata').width(box.width()-10).height(box.height()-10).show();
        }
        
        onWidgetUpdate(update.widget);
    }

    /**
     * Called when changes to the widget require to update the view of the widget (non data related changes)
     */
    function onWidgetUpdate(widget) {
        let container = $('#' + widget.target + '-box').closest('.widget');
        if (widget.selected) {
            container.addClass('chart-selected');
        } else {
            container.removeClass('chart-selected');
        }
    }

    /**
     * This function refleshes the page with the given layout.
     */
    function onPageUpdate(layout) {
        let numberOfColumns = layout.length;
        let maxRows = layout[0].length;
        let table = $("<table/>", { id: 'chart-grid', 'class': 'columns-'+numberOfColumns + ' rows-'+maxRows });
        let rowHeight = Math.round(($(window).height() - 100) / numberOfColumns);
        for (let row = 0; row < maxRows; row++) {
            let tr = $("<tr/>");
            for (let col = 0; col < numberOfColumns; col++) {
                let cell = layout[col][row];
                if (cell) {
                    let span = cell.span;
                    let td = $("<td/>", { colspan: span, rowspan: span, 'class': 'widget', style: 'height: '+(span * rowHeight)+"px;"});
                    td.append(createWidgetToolbar(cell));
                    let status = $('<div/>', { "class": 'status-nodata'});
                    status.append($('<div/>', {text: 'No Data'}));
                    td.append(status);
                    td.append(createWidgetTargetContainer(cell));
                    tr.append(td);
                } else if (cell === null) {
                    tr.append($("<td/>", { 'class': 'widget', style: 'height: '+rowHeight+'px;'}));                  
                }
            }
            table.append(tr);
        }
        $('#chart-container').empty();
        $('#chart-container').append(table);
    }

    /**
     * Method to call when page changes to update UI elements accordingly
     */
    function onPageChange(layout) {
        onPageUpdate(layout);
        updatePageNavigation();
        updatePageAndSelectionSettings();
    }

    /**
     * Public API of the View object:
     */
    return {
        onPageReady: function() {
            // connect the view to the model by passing the 'onDataUpdate' function to the model
            // which will call it when data is received
            onPageUpdate(MonitoringConsole.Model.init(onDataUpdate));
            updatePageAndSelectionSettings();
            updatePageNavigation();
        },
        onPageChange: (layout) => onPageChange(layout),
        onPageUpdate: (layout) => onPageUpdate(layout),
        onPageReset: () => onPageChange(MonitoringConsole.Model.Page.reset()),
        onPageImport: () => MonitoringConsole.Model.importPages(this.files, onPageChange),
        onPageExport: () => onPageExport('monitoring-console-config.json', MonitoringConsole.Model.exportPages()),
        onPageMenu: function() { MonitoringConsole.Model.Settings.toggle(); updatePageAndSelectionSettings(); },
        onPageLayoutChange: (numberOfColumns) => onPageUpdate(MonitoringConsole.Model.Page.arrange(numberOfColumns)),
        onPageDelete: function() {
            if (window.confirm("Do you really want to delete the current page?")) { 
                onPageUpdate(MonitoringConsole.Model.Page.erase());
                updatePageNavigation();
            }
        },
    };
})();
