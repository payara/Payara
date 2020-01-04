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
	
	const TEXT_HTTP_HIGH = "Requires *HTTP monitoring* to be enabled: Goto _Configurations_ => _Monitoring_ and set *'HTTP Service'* to *'HIGH'*.";
	const TEXT_WEB_HIGH = "Requires *WEB monitoring* to be enabled: Goto _Configurations_ => _Monitoring_ and set *'Web Container'* to *'HIGH'*.";
	const TEXT_REQUEST_TRACING = "If you did enable request tracing at _Configurations_ => _Request Tracing_ not seeing any data means no requests passed the tracing threshold which is a good thing.";

	const TEXT_CPU_USAGE = "Requires *CPU Usage HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _CPU Usage_ tab and check *'enabled'*";
	const TEXT_HEAP_USAGE = "Requires *Heap Usage HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _Heap Usage_ tab and check *'enabled'*";
	const TEXT_GC_PERCENTAGE = "Requires *Garbage Collector HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _Garbage Collector_ tab and check *'enabled'*";
	const TEXT_MEM_USAGE = "Requires *Machine Memory HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _Machine Memory Usage_ tab and check *'enabled'*";
	const TEXT_POOL_USAGE = "Requires *Connection Pool HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _Connection Pool_ tab and check *'enabled'*";
	const TEXT_LIVELINESS = "Requires *MicroProfile HealthCheck Checker* to be enabled: Goto _Configurations_ => _HealthCheck_ => _MicroProfile HealthCheck Checker_ tab and check *'enabled'*";

	const UI_PRESETS = {
			pages: {
				core: {
					name: 'Core',
					numberOfColumns: 3,
					widgets: [
						{ series: 'ns:jvm HeapUsage', unit: 'percent',  
							grid: { item: 1, column: 0, span: 1}, 
							axis: { min: 0, max: 100 },
							decorations: {
								thresholds: { reference: 'now', alarming: { value: 50, display: true }, critical: { value: 80, display: true }}}},
						{ series: 'ns:jvm CpuUsage', unit: 'percent',
							grid: { item: 1, column: 1, span: 1}, 
							axis: { min: 0, max: 100 },
							decorations: {
								thresholds: { reference: 'now', alarming: { value: 50, display: true }, critical: { value: 80, display: true }}}},							
						{ series: 'ns:jvm ThreadCount', unit: 'count',  
							grid: { item: 0, column: 1, span: 1}},
						{ series: 'ns:http ThreadPoolCurrentThreadUsage', unit: 'percent',
							grid: { item: 1, column: 2, span: 1},
							status: { missing: { hint: TEXT_HTTP_HIGH }},
							axis: { min: 0, max: 100 },
							decorations: {
								thresholds: { reference: 'avg', alarming: { value: 50, display: true }, critical: { value: 80, display: true }}}},														
						{ series: 'ns:web RequestCount', unit: 'count',
							grid: { item: 0, column: 2, span: 1}, 
							options: { perSec: true },
							status: { missing: { hint: TEXT_WEB_HIGH }}},
						{ series: 'ns:web ActiveSessions', unit: 'count',
							grid: { item: 0, column: 0, span: 1},
							status: { missing: { hint: TEXT_WEB_HIGH }}},
					]
				},
				request_tracing: {
					name: 'Request Tracing',
					numberOfColumns: 1,
					widgets: [
						{ series: 'ns:trace @:* Duration', type: 'bar', unit: 'ms',
							grid: { item: 0, column: 0, span: 1 }, 
							axis: { min: 0, max: 5000 },
							options: { drawMinLine: true },
							status: { missing: { hint: TEXT_REQUEST_TRACING }},
							coloring: 'series',
						}
					]
				},
				http: {
					name: 'HTTP',
					numberOfColumns: 3,
					widgets: [
						{ series: 'ns:http ConnectionQueueCountOpenConnections', unit: 'count',
							grid: { column: 0, item: 0},
							status: { missing : { hint: TEXT_HTTP_HIGH }}},
						{ series: 'ns:http ThreadPoolCurrentThreadsBusy', unit: 'count',
							grid: { column: 0, item: 1},
							status: { missing : { hint: TEXT_HTTP_HIGH }}},
						{ series: 'ns:http ServerCount2xx', unit: 'count', 
							grid: { column: 1, item: 0},
							options: { perSec: true },
							status: { missing : { hint: TEXT_HTTP_HIGH }}},
						{ series: 'ns:http ServerCount3xx', unit: 'count', 
							grid: { column: 1, item: 1},
							options: { perSec: true },
							status: { missing : { hint: TEXT_HTTP_HIGH }}},
						{ series: 'ns:http ServerCount4xx', unit: 'count', 
							grid: { column: 2, item: 0},
							options: { perSec: true },
							status: { missing : { hint: TEXT_HTTP_HIGH }}},
						{ series: 'ns:http ServerCount5xx', unit: 'count', 
							grid: { column: 2, item: 1},
							options: { perSec: true },
							status: { missing : { hint: TEXT_HTTP_HIGH }}},
					]
				},
				health_checks: {
					name: 'Health Checks',
					numberOfColumns: 4,
					widgets: [
						{ series: 'ns:health CpuUsage', unit: 'percent', displayName: 'CPU',
          					grid: { column: 0, item: 0},
          					axis: { max: 100 },
          					status: { missing : { hint: TEXT_CPU_USAGE }}},
						{ series: 'ns:health HeapUsage', unit: 'percent', displayName: 'Heap',
          					grid: { column: 1, item: 1},
          					axis: { max: 100 },
          					status: { missing : { hint: TEXT_HEAP_USAGE }}},
						{ series: 'ns:health TotalGcPercentage', unit: 'percent', displayName: 'GC',
          					grid: { column: 1, item: 0},
          					axis: { max: 30 },
          					status: { missing : { hint: TEXT_GC_PERCENTAGE }}},
						{ series: 'ns:health PhysicalMemoryUsage', unit: 'percent', displayName: 'Memory',
          					grid: { column: 0, item: 1},
          					axis: { max: 100 },
          					status: { missing : { hint: TEXT_MEM_USAGE }}},
						{ series: 'ns:health @:* PoolUsage', unit: 'percent', coloring: 'series', displayName: 'Connection Pools',
          					grid: { column: 1, item: 2},
          					axis: { max: 100 },          					
          					status: { missing : { hint: TEXT_POOL_USAGE }}},
						{ series: 'ns:health LivelinessUp', unit: 'percent', displayName: 'MP Health',
          					grid: { column: 0, item: 2},
          					axis: { max: 100 },
          					options: { noCurves: true },
          					status: { missing : { hint: TEXT_LIVELINESS }}},
						{ series: 'ns:health *', unit: 'percent', type: 'alert', displayName: 'Alerts',
          					grid: { column: 2, item: 0, span: 2},
          				}, 
					]
				},
			},
	};

	//TODO idea: Classification. one can setup a table where a value range is assigned a certain state - this table is used to show that state in the UI, simple but effective

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
		var settings = sanityCheckSettings({});
		
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
			if (page.rotate === undefined)
				page.rotate = true;
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
			if (!widget.unit)
				widget.unit = 'count';
			if (typeof widget.options !== 'object')
				widget.options = {};
			//TODO no data can be a good thing (a series hopefully does not come up => render differently to "No Data" => add a config for that switch)
			if (typeof widget.grid !== 'object')
				widget.grid = {};
			if (typeof widget.decorations !== 'object')
				widget.decorations = {};
			if (typeof widget.decorations.waterline !== 'object') {
				let value = typeof widget.decorations.waterline === 'number' ? widget.decorations.waterline : undefined;
				widget.decorations.waterline = { value: value };
			}
			if (typeof widget.decorations.thresholds !== 'object')
				widget.decorations.thresholds = {};
			if (typeof widget.decorations.alerts !== 'object')
				widget.decorations.alerts = {};			
			if (typeof widget.decorations.thresholds.alarming !== 'object')
				widget.decorations.thresholds.alarming = {};			
			if (typeof widget.decorations.thresholds.critical !== 'object')
				widget.decorations.thresholds.critical = {};			
			if (typeof widget.axis !== 'object')
				widget.axis = {};
			if (typeof widget.status !== 'object')
				widget.status = {};
			if (typeof widget.status.missing !== 'object')
				widget.status.missing = {};
			if (typeof widget.status.alarming !== 'object')
				widget.status.alarming = {};
			if (typeof widget.status.critical !== 'object')
				widget.status.critical = {};
			return widget;
		}

		function sanityCheckSettings(settings) {
			if (settings === undefined)
				settings = {};
			if (settings.theme === undefined)
				settings.theme = {};
			if (settings.theme.colors === undefined)
				settings.theme.colors = {};
			if (settings.theme.options === undefined)
				settings.theme.options = {};			
			return settings;
		}
		
		function doStore() {
			window.localStorage.setItem(LOCAL_UI_KEY, doExport());
		}
		
		function doDeselect() {
			Object.values(pages[settings.home].widgets)
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
			settings.home = page.id;
			return page;
		}

		
		function doImport(userInterface, replaceExisting) {
			if (!userInterface) {
				return false;
			}
			if (userInterface.pages && userInterface.settings)
				settings = sanityCheckSettings(userInterface.settings);
			let importedPages = !userInterface.pages ? userInterface : userInterface.pages;
			// override or add the entry in pages from userInterface
			if (Array.isArray(importedPages)) {
				for (let i = 0; i < importedPages.length; i++) {
					try {
						let page = sanityCheckPage(importedPages[i]);
						if (replaceExisting || pages[page.id] === undefined) {
							pages[page.id] = page;
						}
					} catch (ex) {
					}
				}
			} else {
				for (let [id, page] of Object.entries(importedPages)) {
					try {
						if (replaceExisting || pages[id] === undefined) {
							page.id = id;
							pages[id] = sanityCheckPage(page); 
						}
					} catch (ex) {
					}
				}
			}
			if (settings.home === undefined && Object.keys(pages).length > 0) {
				settings.home = Object.keys(pages)[0];
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
			let page = pages[settings.home];
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
							widget.grid.item = row0; // update item position
						} else {
							while (column.length < row0)
								column.push(null); // null marks empty cells
						}
						for (let spanY = 0; spanY < span; spanY++) {
							let cell = spanX === 0 && spanY === 0 ? info : undefined;
							if (row0 + spanY > column.length) {
								column.push(cell);	
							} else {
								column[row0 + spanY] = cell;
							}
						}
					}
				}
			}
			// give the layout a uniform row number
			let maxRows = layout.map(column => column.length).reduce((acc, cur) => acc ? Math.max(acc, cur) : cur);
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
      		let row = column.findIndex((elem,index,array) => array.slice(index, index + n).every(e => e === null));
			return row < 0 ? column.length : row;
      	}
		
		return {
			themeConfigure(fn) {
				fn(settings.theme);
				doStore();
			},

			themePalette: function(colors) {
				return settings.theme.palette;
			},

			themeOption: function(name, defaultValue) {
				let value = settings.theme.options[name];
				return Number.isNaN(value) || value === undefined ? defaultValue : value;
			},

			themeColor: function(name) {
				return settings.theme.colors[name];
			},

			currentPage: function() {
				return pages[settings.home];
			},
			
			listPages: function() {
				return Object.values(pages).map(function(page) { 
					return { id: page.id, name: page.name, active: page.id === settings.home };
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
						doImport(JSON.parse(json), true);
					}
				} else {
					doImport(userInterface, true);
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
				if (ui)
				doImport(JSON.parse(ui), true);
				doImport(JSON.parse(JSON.stringify(UI_PRESETS)), false);
				return pages[settings.home];
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
				let page = pages[settings.home];
				page.name = name;
				page.id = pageId;
				pages[pageId] = page;
				delete pages[settings.home];
				settings.home = pageId;
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
				delete pages[settings.home];
				settings.home = pageIds[0];
				return pages[settings.home];
			},

			resetPage: function() {
				let presets = UI_PRESETS;
				if (presets && presets.pages && presets.pages[settings.home]) {
					let preset = presets.pages[settings.home];
					pages[settings.home] = sanityCheckPage(JSON.parse(JSON.stringify(preset)));
					doStore();
					return true;
				}
				return false;
			},

			hasPreset: function() {
				let presets = UI_PRESETS;
				return presets && presets.pages && presets.pages[settings.home];
			},
			
			switchPage: function(pageId) {
				if (pageId === undefined) { // rotate to next page from current page
					let pageIds = Object.values(pages).filter(page => page.rotate).map(page => page.id);
					pageId = pageIds[(pageIds.indexOf(settings.home) + 1) % pageIds.length];
				}
				if (!pages[pageId])
					return undefined;
				settings.home = pageId;
				return pages[settings.home];
			},

			rotatePage: function(rotate) {
				if (rotate === undefined)
					return pages[settings.home].rotate;
				pages[settings.home].rotate = rotate;
				doStore();
			},
			
			removeWidget: function(series) {
				let widgets = pages[settings.home].widgets;
				if (series && widgets) {
					delete widgets[series];
				}
			},
			
			addWidget: function(series) {
				if (typeof series !== 'string')
					throw 'configuration object requires string property `series`';
				doDeselect();
				let layout = doLayout();
				let page = pages[settings.home];
				let widgets = page.widgets;
				let widget = { series: series };
				widgets[series] = sanityCheckWidget(widget);
				widget.selected = true;
				// automatically fill most empty column
				let usedCells = new Array(layout.length);
				for (let i = 0; i < usedCells.length; i++) {
					usedCells[i] = 0;
					for (let j = 0; j < layout[i].length; j++) {
						let cell = layout[i][j];
						if (cell === undefined || cell !== null && typeof cell === 'object')
							usedCells[i]++;
					}
				}
				let indexOfLeastUsedCells = usedCells.reduce((iMin, x, i, arr) => x < arr[iMin] ? i : iMin, 0);
				widget.grid.column = indexOfLeastUsedCells;
				widget.grid.item = 100;
				doStore();
			},
			
			configureWidget: function(widgetUpdate, series) {
				let selected = series
					? [pages[settings.home].widgets[series]]
					: Object.values(pages[settings.home].widgets).filter(widget => widget.selected);
				selected.forEach(widget => widgetUpdate(widget));
				doStore();
			},
			
			select: function(series, exclusive) {
				let page = pages[settings.home];
				let widget = page.widgets[series];
				widget.selected = widget.selected !== true;
				if (exclusive) {
					Object.values(page.widgets).forEach(function(widget) {
						if (widget.series != series) {
							widget.selected = false;
						}
					});
				}
				doStore();
				return widget.selected === true;
			},
			
			deselect: function() {
				doDeselect();
				doStore();
			},
			
			selected: function() {
				return Object.values(pages[settings.home].widgets)
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
					|| Object.keys(pages[settings.home].widgets).length == 0
					|| Object.values(pages[settings.home].widgets).filter(widget => widget.selected).length > 0;
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

			Rotation: {
				isEnabled: () => settings.rotation && settings.rotation.enabled,
				enabled: function(enabled) {
					settings.rotation.enabled = enabled === undefined ? true : enabled;
					doStore();
				},
				interval: function(duration) {
					if (!settings.rotation)
						settings.rotation = {};
					if (duration === undefined)
						return settings.rotation.interval;
					if (typeof duration === 'number') {
						settings.rotation.interval = duration;
						doStore();
					}
				}
			},

			Refresh: {
				isPaused: () => settings.refresh && settings.refresh.paused,
				paused: function(paused) {
					settings.refresh.paused = paused;
					doStore();
				},
				interval: function(duration) {
					if (!settings.refresh)
						settings.refresh = {};
					if (duration == undefined)
						return settings.refresh.interval;
					if (typeof duration === 'number') {
						settings.refresh.interval = duration;
						doStore();
					}
				}
			}
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
	
	const DEFAULT_INTERVAL = 2;

	/**
	 * Internal API for data loading from server
	 */
	var Interval = (function() {
		
	    /**
	     * {function} - a function called with no extra arguments when interval tick occured
	     */
	    var onIntervalTick;

		/**
		 * {function} - underlying interval function causing the ticks to occur
		 */
		var intervalFn;
		
		/**
		 * {number} - tick interval in seconds
		 */
		var refreshInterval = DEFAULT_INTERVAL;
		
		function pause() {
			if (intervalFn) {
				clearInterval(intervalFn);
				intervalFn = undefined;
			}
		}

		function resume(atRefreshInterval) {
			onIntervalTick();
			if (atRefreshInterval && atRefreshInterval != refreshInterval) {
				pause();
				refreshInterval = atRefreshInterval;
			}
			if (refreshInterval === 0)
				refreshInterval = DEFAULT_INTERVAL;
			if (intervalFn === undefined) {
				intervalFn = setInterval(onIntervalTick, refreshInterval * 1000);
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
			resume: resume,
			
			pause: pause,
			isPaused: () => intervalFn === undefined,

			interval: function(duration) {
				if (duration === undefined)
					return refreshInterval;
				resume(duration);
			}
		};
	})();

	/**
	 * Internal API for the page rotation interval handling.
	 */ 
	let Rotation = (function() {

	    /**
	     * {function} - a function called with no extra arguments when interval tick occured
	     */
	    var onIntervalTick;

		/**
		 * {function} - underlying interval function causing the ticks to occur
		 */
		var intervalFn;

		return {

			init: function(onIntervalFn) {
				onIntervalTick = onIntervalFn;
			},

			resume: function(atRefreshInterval) {
				if (intervalFn)
					clearInterval(intervalFn); // free old 
				if (atRefreshInterval)
					intervalFn = setInterval(onIntervalTick, atRefreshInterval * 1000);
			}
		};
	})();
	
	/**
	 * Internal API for creating data update messages send to the view from server responses.
	 */ 
	let Update = (function() {

		/**
		 * Shortens the shown time frame to one common to all series but at least to the last minute.
		 */
		function retainCommonTimeFrame(data) {
			if (!data || data.length == 0)
				return [];
			let now = Date.now();
			let startOfLastMinute = now - 60000;
			let startOfShortestSeries = data.reduce((high, e) => Math.max(e.points[0], high), 0);
			let startCutoff = data.length == 1 ? startOfShortestSeries : Math.min(startOfLastMinute, startOfShortestSeries);
			let endOfShortestSeries = data.reduce((low, e) =>  {
				let endTime = e.points[e.points.length - 2];
				return endTime > now - 4000 ? Math.min(endTime, low) : low;
			}, now);
			let endCutoff = endOfShortestSeries;
			data.forEach(function(seriesData) {
				let src = seriesData.points;
				if (src.length == 4 && src[2] >= startCutoff) {
					if (src[1] == src[3]) {
						// extend a straight line between 2 points to cutoff
						seriesData.points = [Math.max(seriesData.observedSince, Math.min(startOfShortestSeries, src[2] - 59000)), src[1], src[2], src[3]];						
					}
				} else {
					let points = [];
					for (let i = 0; i < src.length; i += 2) {
						if (src[i] >= startCutoff && src[i] <= endCutoff) {
							points.push(src[i]);
							points.push(src[i+1]);
						}
					}
					seriesData.points = points;				
				}
			});
			return data.filter(seriesData => seriesData.points.length >= 2 
				&& seriesData.points[seriesData.points.length - 2] > startOfLastMinute);
		}

		function adjustDecimals(data, factor, divisor) {
			data.forEach(function(seriesData) {
				seriesData.points = seriesData.points.map((val, index) => index % 2 == 0 ? val : val * factor / divisor);
				seriesData.observedMax = seriesData.observedMax * factor / divisor; 
				seriesData.observedMin = seriesData.observedMin * factor / divisor;
				seriesData.observedSum = seriesData.observedSum * factor / divisor;
			});
		}

		function perSecond(data) {
			data.forEach(function(seriesData) {
				let points = seriesData.points;
				if (!points)
				  return;
				let pointsPerSec = new Array(points.length - 2);
				for (let i = 0; i < pointsPerSec.length; i+=2) {
				  let t0 = points[i];
				  let t1 = points[i+2];
				  let y0 = points[i+1];
				  let y1 = points[i+3];
				  let dt = t1 - t0;
				  let dy = y1 - y0;
				  let y = (dt / 1000) * dy;
				  pointsPerSec[i] = t1;
				  pointsPerSec[i+1] = y;				  
				}
				if (pointsPerSec.length === 2)
				  pointsPerSec = [points[0], pointsPerSec[1], pointsPerSec[0], pointsPerSec[1]];
				seriesData.points = pointsPerSec;
				//TODO update min/max/avg per sec 
			});
		}

		function addAssessment(widget, data, alerts, watches) {
			data.forEach(function(seriesData) {
				let instance = seriesData.instance;
				let series = seriesData.series;
				let status = 'normal';
				if (Array.isArray(watches) && watches.length == 1) {
					let states = watches
						.map(watch => watch.states[series]).filter(e => e != undefined)
						.map(states => states[instance]).filter(e => e != undefined);
					if (states.length == 1) {
						status = states[0];
					}					
				}
				let thresholds = widget.decorations.thresholds;
				if (thresholds.reference && thresholds.reference !== 'off') {
					let value = seriesData.points[seriesData.points.length-1];
					switch (thresholds.reference) {
						case 'min': value = seriesData.observedMin; break;
						case 'max': value = seriesData.observedMax; break;
						case 'avg': value = seriesData.observedSum / seriesData.observedValues; break;
					}
					let alarming = thresholds.alarming.value;
					let critical = thresholds.critical.value;
					let desc = alarming && critical && critical < alarming;
					if (alarming && ((!desc && value >= alarming) || (desc && value <= alarming))) {
						status = 'alarming';
					}
					if (critical && ((!desc && value >= critical) || (desc && value <= critical))) {
						status = 'critical';
					}
				}
				if (Array.isArray(alerts) && alerts.length > 0) {					
					if (alerts.filter(alert => alert.instance == instance && alert.level === 'red').length > 0) {
						status = 'red';
					} else if (alerts.filter(alert => alert.instance == instance && alert.level === 'amber').length > 0) {
						status = 'amber';
					}
				}
				seriesData.assessments = { status: status };
			});
		}

		function createOnSuccess(widgets, onDataUpdate) {
			return function(response) {
				Object.values(widgets).forEach(function(widget, index) {
					let widgetResponse = response.matches[index];
					let data = retainCommonTimeFrame(widgetResponse.data);
					if (widget.options.decimalMetric || widget.scaleFactor !== undefined && widget.scaleFactor !== 1)
						adjustDecimals(data, widget.scaleFactor ? widget.scaleFactor : 1,  widget.options.decimalMetric ? 10000 : 1);
					if (widget.options.perSec)
						perSecond(data);
					addAssessment(widget, data, widgetResponse.alerts, widgetResponse.watches);
					onDataUpdate({
						widget: widget,
						data: data,
						alerts: widgetResponse.alerts,
						watches: widgetResponse.watches,
						chart: () => Charts.getOrCreate(widget),
					});
				});
			};
		}

		function createOnError(widgets, onDataUpdate) {
			return function(jqXHR, textStatus) {
				Object.values(widgets).forEach(function(widget) {
					onDataUpdate({
						widget: widget,
						chart: () => Charts.getOrCreate(widget),
					});
				});
			};
		}

		return {
			createOnSuccess: createOnSuccess,
			createOnError: createOnError,
		};
	})();


	function doInit(onDataUpdate, onPageUpdate) {
		UI.load();
		Interval.init(function() {
			let widgets = UI.currentPage().widgets;
			let payload = {};
			payload.queries = Object.values(widgets).map(function(widget) { 
				return { 
					series: widget.series,
					truncateAlerts: widget.type !== 'alert',
					truncatePoints: widget.type === 'alert',
					instances: undefined, // all
				}; 
			});
			let request = $.ajax({
				url: 'api/series/data/',
				type: 'POST',
				data: JSON.stringify(payload),
				contentType:"application/json; charset=utf-8",
				dataType:"json",
			});
			request.done(Update.createOnSuccess(widgets, onDataUpdate));
			request.fail(Update.createOnError(widgets, onDataUpdate));
		});
		if (UI.Refresh.interval() === undefined) {
			UI.Refresh.interval(DEFAULT_INTERVAL);
		}
		if (!UI.Refresh.isPaused())
			Interval.resume(UI.Refresh.interval());
		Rotation.init(() => onPageUpdate(doSwitchPage()));
		if (UI.Rotation.isEnabled()) {
			Rotation.resume(UI.Rotation.interval());	
		}
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
				Charts.update(widget);
			} else {
				Charts.destroy(widget.series);
			}
		};
	}

	function doSwitchPage(pageId) {
		if (UI.switchPage(pageId)) {
			Charts.clear();
			Interval.tick();
		}
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
			pause: function() { 
				Interval.pause();
				UI.Refresh.paused(true);
			},
			paused: function(paused) {
				if (paused === undefined)
					return UI.Refresh.isPaused();
				UI.Refresh.paused(paused);
				if (paused) {
					Interval.pause();
				} else {
					Interval.resume(UI.Refresh.interval());
				}
			},
			isPaused: UI.Refresh.isPaused,
			resume: function(duration) {
				if (duration !== undefined) {
					UI.Refresh.interval(duration);
				}
				UI.Refresh.paused(false);
				Interval.resume(UI.Refresh.interval());
			},
			interval: function(duration) {
				if (duration === undefined)
					return UI.Refresh.interval();
				UI.Refresh.interval(duration);
				Interval.resume(UI.Refresh.interval());				
			},
		},

		Theme: {
			palette: UI.themePalette,
			option: UI.themeOption,
			color: UI.themeColor,
			configure: UI.themeConfigure,
		},
		
		Settings: {
			isDispayed: UI.showSettings,
			open: UI.openSettings,
			close: UI.closeSettings,
			toggle: () => UI.showSettings() ? UI.closeSettings() : UI.openSettings(),
			
			Rotation: {
				isEnabled: UI.Rotation.isEnabled,
				enabled: function(enabled) {
					UI.Rotation.enabled(enabled);
					Rotation.resume(UI.Rotation.isEnabled() ? UI.Rotation.interval() : 0);
				},
				interval: function(duration) {
					if (duration === undefined)
						return UI.Rotation.interval();
					UI.Rotation.interval(duration);
					Rotation.resume(UI.Rotation.isEnabled() ? UI.Rotation.interval() : 0);
				}
			}
		},
		
		/**
		 * API to control the active page manipulate the set of charts contained on it.
		 */
		Page: {
			
			id: () => UI.currentPage().id,
			name: () => UI.currentPage().name,
			rename: UI.renamePage,
			rotate: UI.rotatePage,
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

			hasPreset: UI.hasPreset,
			
			changeTo: function(pageId) {
				return doSwitchPage(pageId);
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
					if (series.trim()) {
						UI.addWidget(series);
						Interval.tick();
					}
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

				moveUp: (series) => doConfigureWidget(series, function(widget) {
                    widget.grid.item = Math.max(0, widget.grid.item - widget.grid.span);
	            }),

	            moveDown: (series) => doConfigureWidget(series, function(widget) {
                    widget.grid.item += widget.grid.span;
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
