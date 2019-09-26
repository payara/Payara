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
			if (!widget.type)
				widget.type = 'line';
			if (!widget.options) {
				widget.options = { 
					beginAtZero: true,
					autoTimeTicks: true,
					//TODO no data can be a good thing (a series hopefully does not come up => render differently to "No Data" => add a config for that switch)
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
				selected.forEach(widget => widgetUpdate(widget));
				doStore();
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

		return {
			/**
			 * Returns a new Chart.js chart object initialised for the given MC level configuration to the charts object
			 */
			getOrCreate: function(widget) {
				let series = widget.series;
				let chart = charts[series];
				if (chart)
					return chart;
				chart = MonitoringConsole.Chart.getAPI(widget).onCreation(widget);			
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
					MonitoringConsole.Chart.getAPI(widget).onConfigUpdate(widget, chart);
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
			payload.queries = Object.keys(widgets).map(function(series) { 
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
		UI.configureWidget(createWidgetUpdate(widgetUpdate));
		return UI.arrange();
	}

	function doConfigureWidget(series, widgetUpdate) {
		UI.configureWidget(createWidgetUpdate(widgetUpdate), series);
		return UI.arrange();
	}

	function createWidgetUpdate(widgetUpdate) {
		return function(widget) {
			let type = widget.type;
			widgetUpdate(widget);
			if (widget.type === type) {
				Charts.update(widget, );
			} else {
				Charts.destroy(widget.series);
			}
		};
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
