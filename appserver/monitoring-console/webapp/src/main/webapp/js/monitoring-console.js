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
Chart.defaults.global.tooltips.enabled = false;

/**
 * The different parts of the Monitoring Console are added as the below properties by the individual files.
 */
var MonitoringConsole =  {
   /**
    * Functions to update the actual HTML page of the MC
    **/
	View: {},
   /**
    * Functions of manipulate the model of the MC (often returns a layout that is applied to the View)
    **/ 
	Model: {},
   /**
    * Functions specifically to take the data and prepare the display particular chart type using the underlying charting library.
    *
    * Each of the type objects below shares the same public API that is used by Model and View to apply the model to the chart to update the view properly.
    **/
	Chart: {

    /**
     * A collection of general adapter functions for the underlying chart library
     */ 
    Common: {},
   /**
    * Line chart adapter API for monitoring series data
    **/
    Line: {},
   /**
    * Bar chart adapter API for monitoring series data
    **/
    Bar: {},

    /**
     * Trace 'gantt chart' like API, this is not a strict adapter API as the other two as the data to populate this is specific to traces
     */
    Trace: {},

  }
};
MonitoringConsole.Chart.getAPI = function(widget) {
  switch (widget.type) {
    default:
    case 'line': return MonitoringConsole.Chart.Line;
    case 'bar': return MonitoringConsole.Chart.Bar;
    case 'alert': return MonitoringConsole.Chart.Line;
  }
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
				health: {
					name: 'Health Checks',
					numberOfColumns: 4,
					widgets: [
						{ series: 'ns:health CpuUsage', unit: 'percent',
          					grid: { column: 0, item: 0},
          					axis: { max: 100 },
          					status: { missing : { hint: TEXT_CPU_USAGE }}},
						{ series: 'ns:health HeapUsage', unit: 'percent',
          					grid: { column: 0, item: 1},
          					axis: { max: 100 },
          					status: { missing : { hint: TEXT_HEAP_USAGE }}},
						{ series: 'ns:health TotalGcPercentage', unit: 'percent',
          					grid: { column: 0, item: 2},
          					axis: { max: 30 },
          					status: { missing : { hint: TEXT_GC_PERCENTAGE }}},
						{ series: 'ns:health PhysicalMemoryUsage', unit: 'percent',
          					grid: { column: 0, item: 4},
          					axis: { max: 100 },
          					status: { missing : { hint: TEXT_MEM_USAGE }}},
						{ series: 'ns:health @:* PoolUsage', unit: 'percent', coloring: 'series',
          					grid: { column: 1, item: 3},
          					axis: { max: 100 },          					
          					status: { missing : { hint: TEXT_POOL_USAGE }}},
						{ series: 'ns:health LivelinessDownCount', unit: 'count',
          					grid: { column: 3, item: 3},
          					options: { noCurves: true },
          					status: { missing : { hint: TEXT_LIVELINESS }}},
          				{ series: 'ns:health LivelinessErrorCount', unit: 'count',
          					grid: { column: 2, item: 3},
          					options: { noCurves: true },
          					status: { missing : { hint: TEXT_LIVELINESS }}},
						{ series: 'ns:health *', unit: 'percent', type: 'alert',
          					grid: { column: 1, item: 0, span: 3},
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
			if (settings.colors === undefined)
				settings.colors = {};
			if (settings.colors.defaults === undefined)
				settings.colors.defaults = {};
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
			colorPalette: function(colors) {
				if (colors === undefined)
					return settings.colors.palette;
				settings.colors.palette = colors;
				doStore();
			},

			colorOpacity: function(opacity) {
				if (opacity === undefined)
					return settings.colors.opacity;
				settings.colors.opacity = opacity;
				doStore();
			},

			colorDefault: function(name, color) {
				if (color === undefined)
					return settings.colors.defaults[name];
				settings.colors.defaults[name] = color;
				doStore();
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
			let startOfLastMinute = Date.now() - 62000;
			let startOfShortestSeries = data.reduce((high, e) => Math.max(e.points[0], high), 0);
			let startCutoff = Math.min(startOfLastMinute, startOfShortestSeries);
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
						if (src[i] >= startCutoff) {
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

		function addAssessment(widget, data) {
			data.forEach(function(seriesData) {
				let status = 'normal';
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
					addAssessment(widget, data);
					onDataUpdate({
						widget: widget,
						data: data,
						alerts: widgetResponse.alerts,
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

		Colors: {
			palette: UI.colorPalette,
			opacity: UI.colorOpacity,
			default: UI.colorDefault,
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
 * Utilities to convert values given in units from and to string.
 **/
MonitoringConsole.View.Units = (function() {

   const PERCENT_FACTORS = {
      '%': 1
   };

   /**
    * Factors used for any time unit to milliseconds
    */
   const SEC_FACTORS = {
      h: 60 * 60, hours: 60 * 60,
      m: 60, min: 60, mins: 60,
      s: 1, sec: 1, secs: 1,
      _: [['h', 'm', 's'], ['m', 's'], ['h', 'm']]
   };

   /**
    * Factors used for any time unit to milliseconds
    */
   const MS_FACTORS = {
      h: 60 * 60 * 1000, hours: 60 * 60 * 1000,
      m: 60 * 1000, min: 60 * 1000, mins: 60 * 1000,
      s: 1000, sec: 1000, secs: 1000,
      ms: 1,
      us: 1/1000, μs: 1/1000,
      ns: 1/1000000,
      _: [['m', 's', 'ms'], ['s', 'ms'], ['h', 'm', 's'], ['m', 's'], ['h', 'm']]
   };

   /**
    * Factors used for any time unit to nanoseconds
    */
   const NS_FACTORS = {
      h: 60 * 60 * 1000 * 1000000, hours: 60 * 60 * 1000 * 1000000,
      m: 60 * 1000 * 1000000, min: 60 * 1000 * 1000000, mins: 60 * 1000 * 1000000,
      s: 1000 * 1000000, sec: 1000 * 1000000, secs: 1000 * 1000000,
      ms: 1000000,
      us: 1000, μs: 1000,
      ns: 1,
      _: [['m', 's', 'ms'], ['s', 'ms'], ['h', 'm', 's'], ['m', 's'], ['h', 'm']]
   };

   /**
    * Factors used for any memory unit to bytes
    */
   const BYTES_FACTORS = {
      kb: 1024,
      mb: 1024 * 1024,
      gb: 1024 * 1024 * 1024,
      tb: 1024 * 1024 * 1024 * 1024,
   };

   /**
    * Factors by unit
    */
   const FACTORS = {
      sec: SEC_FACTORS,
      ms: MS_FACTORS,
      ns: NS_FACTORS,
      bytes: BYTES_FACTORS,
      percent: PERCENT_FACTORS,
   };

   function parseNumber(valueAsString, factors) {
      if (!valueAsString || typeof valueAsString === 'string' && valueAsString.trim() === '')
         return undefined;
      let valueAsNumber = Number(valueAsString);
      if (!Number.isNaN(valueAsNumber))
         return valueAsNumber;
      let valueAndUnit = valueAsString.split(/(-?[0-9]+\.?[0-9]*)/);
      let sum = 0;
      for (let i = 1; i < valueAndUnit.length; i+=2) {
         valueAsNumber = Number(valueAndUnit[i].trim());
         let unit = valueAndUnit[i+1].trim().toLowerCase();
         let factor = factors[unit];
         sum += valueAsNumber * factor;               
      }
      return sum;
   }

   function formatDecimal(valueAsNumber) {
      if (!hasDecimalPlaces(valueAsNumber)) {
         return Math.round(valueAsNumber).toString();
      } 
      let text = valueAsNumber.toFixed(1);
      return text.endsWith('.0') ? Math.round(valueAsNumber).toString() : text;
   }

   function formatNumber(valueAsNumber, factors, useDecimals) {
      if (valueAsNumber === undefined)
         return undefined;
      if (valueAsNumber === 0)
         return '0';
      if (!factors)
         return formatDecimal(valueAsNumber);
      if (valueAsNumber < 0)
         return '-' + formatNumber(-valueAsNumber, factors, useDecimals);
      let largestFactorUnit;
      let largestFactor = 0;
      let factor1Unit = '';
      for (let [unit, factor] of Object.entries(factors)) {
         if (unit != '_' && (valueAsNumber >= 0 && factor > 0 || valueAsNumber < 0 && factor < 0)
            && (useDecimals && (valueAsNumber / factor) >= 1 || !hasDecimalPlaces(valueAsNumber / factor))
            && factor > largestFactor) {
            largestFactor = factor;
            largestFactorUnit = unit;
         }
         if (factor === 1)
            factor1Unit = unit;
      }
      if (!largestFactorUnit)
         return formatDecimal(valueAsNumber) + factor1Unit;
      if (useDecimals)
         return formatDecimal(valueAsNumber / largestFactor) + largestFactorUnit;
      let valueInUnit = Math.round(valueAsNumber / largestFactor);
      if (factors._) {
         for (let i = 0; i < factors._.length; i++) {
            let combination = factors._[i];
            let rest = valueAsNumber;
            let text = '';
            if (combination[combination.length - 1] == largestFactorUnit) {
               for (let j = 0; j < combination.length; j++) {
                  let unit = combination[j];
                  let factor = factors[unit];
                  let times = Math.floor(rest / factor);
                  if (times === 0)
                     break;
                  rest -= times * factor;
                  text += times + unit;                      
               }
            }
            if (rest === 0) {
               return text;
            }
         }
      }
      // TODO look for a combination that has the found unit as its last member
      // try if value can eben be expressed as the combination, otherwise try next that might fulfil firs condition
      return valueInUnit + largestFactorUnit;
   }

   function hasDecimalPlaces(number) {
      return number % 1 != 0;
   }

   function formatTime(hourOrDateOrTimestamp, minute, second) {
      if (typeof hourOrDateOrTimestamp === 'number' && hourOrDateOrTimestamp > 24) { // assume timestamp
         hourOrDateOrTimestamp = new Date(hourOrDateOrTimestamp);
      }
      if (typeof hourOrDateOrTimestamp === 'object') { // assume Date
         minute = hourOrDateOrTimestamp.getMinutes();
         second = hourOrDateOrTimestamp.getSeconds();
         hourOrDateOrTimestamp = hourOrDateOrTimestamp.getHours();
      }
      let str = as2digits(hourOrDateOrTimestamp);
      str += ':' + as2digits(minute ? minute : 0);
      if (second)
         str += ':' +  as2digits(second);
      return str;
   }

   function as2digits(number) {
      return number.toString().padStart(2, '0');
   }

   const DECIMAL_NUMBER_PATTERN = '([0-9]+\.)?[0-9]+';

   function pattern(factors) {
      if (!factors)
         return DECIMAL_NUMBER_PATTERN;
      return '(' + DECIMAL_NUMBER_PATTERN + '(' + Object.keys(factors).filter(unit => unit != '_').join('|') + ')? *)+';
   }

   function patternHint(factors) {
      if (!factors)
         return 'a integer or decimal number';
      return 'a integer or decimal number, optionally followed by a unit: ' + Object.keys(factors).filter(unit => unit != '_').join(', ');
   }

   function maxAlertLevel(a, b) {
      const table = ['white', 'green', 'amber', 'red'];
      return table[Math.max(0, Math.max(table.indexOf(a), table.indexOf(b)))];
   }

   /**
    * Public API below:
    */
   return {
      Alerts: {
         maxLevel: maxAlertLevel,
      },
      
      formatTime: formatTime,
      formatNumber: formatNumber,
      formatMilliseconds: (valueAsNumber) => formatNumber(valueAsNumber, MS_FACTORS),
      formatNanoseconds: (valueAsNumber) => formatNumber(valueAsNumber, NS_FACTORS),
      formatBytes: (valueAsNumber) => formatNumber(valueAsNumber, BYTES_FACTORS),
      parseNumber: parseNumber,
      parseMilliseconds: (valueAsString) => parseNumber(valueAsString, MS_FACTORS),
      parseNanoseconds: (valueAsString) => parseNumber(valueAsString, NS_FACTORS),
      parseBytes: (valueAsString) => parseNumber(valueAsString, BYTES_FACTORS),
      converter: function(unit) {
         let factors = FACTORS[unit];
         return {
            format: (valueAsNumber, useDecimals) => formatNumber(valueAsNumber, factors, useDecimals),
            parse: (valueAsString) => parseNumber(valueAsString, factors),
            pattern: () =>  pattern(factors),
            patternHint: () => patternHint(factors),
         };
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
 * Utilities to convert color values and handle color schemes.
 *
 * Color schemes are named sets of colors that are applied to the model.
 * This is build purely on top of the model. 
 * The model does not remember a scheme, 
 * schemes are just a utility to reset color configurations to a certain set.
 **/
MonitoringConsole.View.Colors = (function() {

   const Colors = MonitoringConsole.Model.Colors;

   const SCHEMES = {
      Payara: {
         name: 'Payara',
         palette: [ '#F0981B', '#008CC4', '#8B79BC', '#87BC25', '#FF70DA' ],
         opacity: 20,
         defaults:  { 
            waterline: '#00ffff', alarming: '#FFD700', critical: '#dc143c',
            white: '#ffffff', green: '#87BC25', amber: '#f0981b', red: '#dc143c',
          }
      },

      a: {
         name: '80s',
         opacity: 30,
         palette: [ '#ff48c4', '#2bd1fc', '#f3ea5f', '#c04df9', '#ff3f3f'],
         defaults:  { waterline: '#2bd1fc', alarming: '#f3ea5f', critical: '#ff3f3f' }
      },

      b: {
         name: '80s Pastel',
         opacity: 40,
         palette: [ '#bd6283', '#96c0bc', '#dbd259', '#d49e54', '#b95f51'],
         defaults:  { waterline: '#96c0bc', alarming: '#dbd259', critical: '#b95f51' }
      },

      c: {
         name: '80s Neon',
         opacity: 15,
         palette: [ '#cb268b', '#f64e0c', '#eff109', '#6cf700', '#00aff3'],
         defaults:  { waterline: '#00aff3', alarming: '#eff109', critical: '#f64e0c' }
      },

      d: {
         name: 'VaporWave',
         opacity: 20,
         palette: [ '#05ffa1', '#b8a9df', '#01cdfe', '#b967ff', '#fffb96'],
         defaults:  { waterline: '#01cdfe', alarming: '#fffb96', critical: '#FB637A' }
      },

      e: {
         name: 'Solarized',
         opacity: 20,
         palette: [ '#b58900', '#cb4b16', '#dc322f', '#d32682', '#c671c4', '#268bd2', '#2aa198', '#859900'],
         defaults:  { waterline: '#268bd2', alarming: '#b58900', critical: '#dc322f' }
      }
   };

   /**
    * Object used as map to remember the colors by coloring stratgey.
    * Each strategy leads to an object map that remebers the key as fields 
    * and the index associated with the key as value.
    * This is a mapping from e.g. the name 'DAS' to index 0. 
    * The index is then used to pick a color form the palette.
    * This makes sure that same key, for example the instance name,
    * always uses the same color accross widgets and pages.
    */
   let colorIndexMaps = {};

   function lookup(coloring, key, palette) {
      let mapName = coloring || 'instance';
      let map = colorIndexMaps[mapName];
      if (map === undefined) {
         map = {};
         colorIndexMaps[mapName] = map;
      }
      let index = map[key];
      if (index === undefined) {
         index = Object.keys(map).length;
         map[key] = index;
      }
      return derive(palette, index);
   }

   /**
    * Returns a palette color.
    * If index is outside of the palette given a color is derived from the palette.
    * The derived colors are intentionally biased towards light non-grayish colors.
    */
   function derive(palette, index = 1) {
      let color = palette[index % palette.length];
      if (index < palette.length)
         return color;
      let [r, g, b] = hex2rgb(color);
      let offset = index - palette.length + 1;
      let shift = (offset * 110) % 255;
      r = (r + shift) % 255;
      g = (g + shift) % 255;
      b = (b + shift) % 255;
      let variant = offset % 6;
      if (variant == 0 || variant == 3 || variant == 4)
         r = Math.round(r / 2);
      if (variant == 1 || variant == 3 || variant == 5)
         g = Math.round(g / 2);
      if (variant == 2 || variant == 4 || variant == 5)
         b = Math.round(b / 2);
      if (r + g + b < 380 && r < 120) r = 255 - r;
      if (r + g + b < 380 && g < 120) g = 255 - g;
      if (r + g + b < 380 && b < 120) b = 255 - b;
      return rgb2hex(r, g, b);
   }

   function random(palette) {
      if (Array.isArray(palette))
         return derive([palette[0]], palette.length); 
      const letters = '0123456789ABCDEF';
      let color = '#';
      for (let i = 0; i < 6; i++) {
         color += letters[Math.floor(Math.random() * 16)];
      }
      return color;
   }

   function hex2rgb(hex) {
      return hex.match(/\w\w/g).map(x => parseInt(x, 16));
   }

   function rgb2hex(r, g, b) {
      return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
   }

   function hex2rgba(hex, alpha = 1) {
      const [r, g, b] = hex2rgb(hex);
      return `rgba(${r},${g},${b},${alpha})`;
   }

   function schemeOptions() {
      return Object.keys(SCHEMES).reduce(function(result, key) {
         result[key] = SCHEMES[key].name;
         return result;
      }, { _: '(Select to apply)' });
   }

   function applyScheme(name, override = true) {
      let scheme = SCHEMES[name];
      if (scheme) {
         if (override || Colors.palette() === undefined)
            Colors.palette(scheme.palette);
         if (override || Colors.opacity() === undefined)
            Colors.opacity(scheme.opacity);
         if (scheme.defaults) {
            for (let [name, color] of Object.entries(scheme.defaults)) {
               if (override || Colors.default(name) === undefined)
                  Colors.default(name, color);
            }
         }            
      }
   }

   /**
    * Public API of the color utility module.
    */
   return {
      lookup: lookup,
      random: random,
      hex2rgba: hex2rgba,
      schemes: schemeOptions,
      scheme: applyScheme,
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
 * Data/Model driven view components.
 *
 * Each of them gets passed a model creates a DOM structure in form of a jQuery object 
 * that should be inserted into the page DOM using jQuery.
 *
 * Besides general encapsulation the idea is to benefit of a function approch 
 * that utilises pure functions to compute the page context from a fixed state input.
 * This makes the code much easier to understand and maintain as it is free of overall page state.
 **/
MonitoringConsole.View.Components = (function() {

   const Units = MonitoringConsole.View.Units;
   const Colors = MonitoringConsole.View.Colors;
   const Selection = MonitoringConsole.Model.Page.Widgets.Selection;

   /**
    * This is the side panel showing the details and settings of widgets
    */
   let Settings = (function() {

      function createHeaderRow(model) {
        let caption = model.label;
        let config = {colspan: 2};
        if (model.description)
          config.title = model.description;
        let th = $('<th/>', config);
        let showHide = function() {
          let tr = th.closest('tr').next();
          let toggleAll = tr.children('th').length > 0;
          while (tr.length > 0 && (toggleAll || tr.children('th').length == 0)) {
              if (tr.children('th').length == 0) {
                  tr.toggle();                    
              }
              tr = tr.next();
          }
        };
        return $('<tr/>').append(
            th.html(caption).click(showHide));
      }

      function createTable(model) {
        let table = $('<table />', { id: model.id });
        if (model.caption)
          table.append(createHeaderRow({ label: model.caption, description: model.description, collapsed: model.collapsed }));
        return table;
      }

      function createRow(model, inputs) {
        let components = $.isFunction(inputs) ? inputs() : inputs;
        if (typeof components === 'string')
            components = document.createTextNode(components);
        let config = {};
        if (model.description)
          config.title = model.description;
        let tr = $('<tr/>');
        tr.append($('<td/>', config).text(model.label)).append($('<td/>').append(components));
        if (model.collapsed)
          tr.css('display', 'none');
        return tr;
      }

      function enhancedOnChange(onChange, updatePage) {
        if (onChange.length == 2) {
          return (checked) => {
            let layout = Selection.configure((widget) => onChange(widget, checked));
            if (updatePage) {
              MonitoringConsole.View.onPageUpdate(layout);
            }
          };
        }
        return onChange;
      }

      function createCheckboxInput(model) {
        let config = { id: model.id, type: 'checkbox', checked: model.value };
        if (model.description && !model.label)
          config.title = model.description;
        let onChange = enhancedOnChange(model.onChange);
        return $("<input/>", config)
          .on('change', function() {
            let checked = this.checked;
            onChange(checked);
          });
      }

      function createRangeInput(model) {
        let config = { id: model.id, type: 'number', value: model.value};
        if (model.min)
          config.min = model.min;
        if (model.max)
          config.max = model.max;
        if (model.description && !model.label)
          config.title = model.description;
        let onChange = enhancedOnChange(model.onChange, true);
        return $('<input/>', config)
          .on('input change', function() {  
            let val = this.valueAsNumber;
            if (Number.isNaN(val))
              val = undefined;
            onChange(val);
          });
      }

      function createDropdownInput(model) {
         let config = { id: model.id };
         if (model.description && !model.label)
          config.title = description;
         let dropdown = $('<select/>',  );
         Object.keys(model.options).forEach(option => dropdown.append($('<option/>', 
            {text:model.options[option], value:option, selected: model.value === option})));
         let onChange = enhancedOnChange(model.onChange, true);
         dropdown.change(() => onChange(dropdown.val()));
         return dropdown;
      }

      function createValueInput(model) {
        if (model.unit === 'percent')
          return createRangeInput({id: model.id, min: 0, max: 100, value: model.value, onChange: model.onChange });
        if (model.unit === 'count')
          return createRangeInput(model);
        return createTextInput(model, Units.converter(model.unit));
      }

      function createTextInput(model, converter) {
        if (!converter)
          converter = { format: (str) => str, parse: (str) => str };
        let config = { 
          id: model.id,
          type: 'text', 
          value: converter.format(model.value), 
        };
        if (model.description && !model.label)
          config.title = description;
        let readonly = model.onChange === undefined;
        if (!readonly) {
          if (converter.pattern !== undefined)
            config.pattern = converter.pattern();
          if (converter.patternHint !== undefined)
            config.title = (config.title ? config.title + ' ' : '') + converter.patternHint();
        }
        let input = $('<input/>', config);
        if (!readonly) {
          let onChange = enhancedOnChange(model.onChange, true);
          input.on('input change paste', function() {
            let val = converter.parse(this.value);
            onChange(val);
          });          
        } else {
          input.prop('readonly', true);
        }
        return input;
      }

      function createColorInput(model) {
        let value = model.value;
        if (value === undefined && model.defaultValue !== undefined)
          value = model.defaultValue;
        if (Array.isArray(value))
          return createMultiColorInput(model);
        let config = { id: model.id, type: 'color', value: value };
        if (model.description && !model.label)
          config.title = model.description;
        let onChange = enhancedOnChange(model.onChange);
        let input = $('<input/>', config)
          .on('input change', function() { 
            let val = this.value;
            if (val === model.defaultValue)
              val = undefined;
            onChange(val);
          });
        if (model.defaultValue === undefined)
          return input;
        return $('<span/>').append(input).append($('<input/>', { 
          type: 'button', 
          value: ' ',
          title: 'Reset to default color', 
          style: 'background-color: ' + model.defaultValue,
        }).click(() => {
          onChange(undefined);
          input.val(model.defaultValue);
        }));
      }

      function createMultiColorInput(model) {
        let value = model.value;
        if (value === undefined && model.defaultValue !== undefined)
          value = model.defaultValue;
        if (!Array.isArray(value))
          value = [value];
        let list = $('<span/>');
        //TODO model id goes where?
        let colors = [...value];
        let onChange = enhancedOnChange(model.onChange);
        for (let i = 0; i < value.length; i++) {
          list.append(createMultiColorItemInput(colors, i, onChange));
        }
        let add = $('<input/>', {type: 'button', value: '+'});
        add.click(() => {
          colors.push(Colors.random(colors));
          createMultiColorItemInput(colors, colors.length-1, onChange).insertBefore(add);
          onChange(colors);
        });
        let remove = $('<input/>', {type: 'button', value: '-'});
        remove.click(() => {
          if (colors.length > 1) {
            colors.length -= 1;
            list.children('input[type=color]').last().remove();
            onChange(colors);
          }
        });
        list.append(add);
        list.append(remove);
        return list;
      }

      function createMultiColorItemInput(colors, index, onChange) {
        return createColorInput({ value: colors[index], onChange: (val) => {
          colors[index] = val;
          onChange(colors);
        }});
      }

      function createInput(model) {
         switch (model.type) {
            case 'checkbox': return createCheckboxInput(model);
            case 'dropdown': return createDropdownInput(model);
            case 'range'   : return createRangeInput(model);
            case 'value'   : return createValueInput(model);
            case 'text'    : return createTextInput(model);
            case 'color'   : return createColorInput(model);
            default        : return model.input;
         }
      }

      function createComponent(model) {
         let panel = $('<div/>', { id: model.id });
         let syntheticId = 0;
         let collapsed = false;
         for (let t = 0; t < model.groups.length; t++) {
            let group = model.groups[t];
            let table = createTable(group);
            collapsed = group.collapsed === true;
            panel.append(table);
            for (let r = 0; r < group.entries.length; r++) {
               syntheticId++;
               let entry = group.entries[r];
               let type = entry.type;
               let auto = type === undefined;
               let input = entry.input;
               if (entry.id === undefined)
                 entry.id = 'setting_' + syntheticId;
               entry.collapsed = collapsed;
               if (type == 'header' || auto && input === undefined) {
                  collapsed = entry.collapsed === true;
                  table.append(createHeaderRow(entry));
               } else if (!auto) {
                  table.append(createRow(entry, createInput(entry)));
               } else {
                  if (Array.isArray(input)) {
                    let [innerInput, innerSyntheticId] = createMultiInput(input, syntheticId, 'x-input');
                    input = innerInput;
                    syntheticId = innerSyntheticId;
                  }
                  table.append(createRow(entry, input));
               }
            }
         }
         return panel;
      }

      function createMultiInput(inputs, syntheticId, css) {
        let box = $('<div/>', {'class': css});
        for (let i = 0; i < inputs.length; i++) {
          let entry = inputs[i];
          if (Array.isArray(entry)) {
            let [innerBox, innerSyntheticId] = createMultiInput(entry, syntheticId);
            box.append(innerBox);
            syntheticId = innerSyntheticId;
          } else {
            syntheticId++;
            if (entry.id === undefined)
              entry.id = 'setting_' + syntheticId;
            let input = createInput(entry);
            if (entry.label) {
              let config = { 'for': entry.id };
              if (entry.description)
                config.title = entry.description;
              box.append($('<span/>').append(input).append($('<label/>', config).html(entry.label))).append("\n");
            } else {
              box.append(input);
            }
          }                    
        }
        return [box, syntheticId];
      }

      return { createComponent: createComponent };
   })();

   /**
   * Legend is a generic component showing a number of current values annotated with label and color.
   */ 
   let Legend = (function() {

      function createItem(model) {
         let label = model.label;
         let value = model.value;
         let color = model.color;
         let assessments = model.assessments;
         let strong = value;
         let normal = '';
         if (typeof value === 'string' && value.indexOf(' ') > 0) {
            strong = value.substring(0, value.indexOf(' '));
            normal = value.substring(value.indexOf(' '));
         }
         let attrs = { style: 'border-color: ' + color + ';' };
         if (assessments && assessments.status)
            attrs.class = 'status-' + assessments.status;
         if (label === 'server') { // special rule for DAS
            label = 'DAS'; 
            attrs.title = "Data for the Domain Administration Server (DAS); plain instance name is 'server'";
         }
         let textAttrs = {};
         if (assessments && assessments.color)
           textAttrs.style = 'color: '+assessments.color + ';';
         return $('<li/>', attrs)
               .append($('<span/>').text(label))
               .append($('<strong/>', textAttrs).text(strong))
               .append($('<span/>').text(normal));
      }

      function createComponent(model) {
         let legend = $('<ol/>',  {'class': 'Legend'});
         for (let i = 0; i < model.length; i++) {
            legend.append(createItem(model[i]));
         }
         return legend;
      }

      return { createComponent: createComponent };
   })();

  /**
   * Component drawn for each widget legend item to indicate data status.
   */
  let Indicator = (function() {

    function createComponent(model) {
       if (!model.text) {
          return $('<div/>', {'class': 'Indicator', style: 'display: none;'});
       }
       let html = model.text.replace(/\*([^*]+)\*/g, '<b>$1</b>').replace(/_([^_]+)_/g, '<i>$1</i>');
       return $('<div/>', { 'class': 'Indicator status-' + model.status }).html(html);
    }

    return { createComponent: createComponent };
  })();

  /**
   * Component for any of the text+icon menus/toolbars.
   */
  let Menu = (function() {

    function createComponent(model) {
      let attrs = { 'class': 'Menu' };
      if (model.id)
        attrs.id = model.id;
      let menu = $('<span/>', attrs);
      let groups = model.groups;
      for (let g = 0; g < groups.length; g++) {
        let group = groups[g];
        if (group.items) {
          let groupBox = $('<span/>', { class: 'Group' });
          let groupLabel = $('<a/>').html(createText(group));
          let groupItem = $('<span/>', { class: 'Item' })
            .append(groupLabel)
            .append(groupBox)
            ;
          if (group.clickable) {
            groupLabel
              .click(group.items.find(e => e.hidden !== true && e.disabled !== true).onClick)
              .addClass('clickable');
          }
          menu.append(groupItem);
          for (let i = 0; i < group.items.length; i++) {
            let item = group.items[i];
            if (item.hidden !== true)
              groupBox.append(createButton(item));
          }          
        } else {
          if (group.hidden !== true)
            menu.append(createButton(group).addClass('Item'));
        }
      }
      return menu;
    }

    function createText(button) {
      let text = '';
      if (button.icon)
        text += '<strong>'+button.icon+'</strong>';
      if (button.label)
        text += button.label;
      if (button.label && button.items)
        text += " &#9013;";
      return text;
    }

    function createButton(button) {
      let attrs = { title: button.description };
      if (button.disabled)
        attrs.disabled = true;
      return $('<button/>', attrs)
            .html(createText(button))
            .click(button.onClick)
            .addClass('clickable');
    }

    return { createComponent: createComponent };
  })();

  /**
   * An alert table is a widget that shows a table of alerts that have occured for the widget series.
   */
  let AlertTable = (function() {

    function createComponent(model) {
      let items = model.items === undefined ? [] : model.items.sort(sortMostUrgetFirst);
      config = { 'class': 'AlertTable' };
      if (model.id)
        config.id = model.id;
      if (items.length == 0)
        config.style = 'display: none';
      let table = $('<div/>', config);
      let lastOngoing;
      for (let i = 0; i < items.length; i++) {
        let item = items[i];
        item.frames = item.frames.sort(sortMostRecentFirst); //NB. even though sortMostUrgetFirst does this as well we have to redo it here - JS...
        let endFrame = item.frames[0];
        let startFrame = item.frames[frames.length - 1];
        let ongoing = endFrame.until === undefined;
        let level = endFrame.level;
        let color = !item.acknowledged && ongoing ? endFrame.color : Colors.hex2rgba(endFrame.color, 0.6);
        let box = $('<div/>', { style: 'border-color:' + color + ';' });
        box.append(createGeneralGroup(item));
        box.append(createStatisticsGroup(item));
        if (ongoing)
          box.append(createConditionGroup(item));
        let row = $('<div/>', { id: 'Alert-' + item.serial, class: 'Item ' + level, style: 'border-color:'+item.color+';' });
        table.append(row.append(box));
        lastOngoing = ongoing;
      }
      return table;
    }

    function createConditionGroup(item) {
      let endFrame = item.frames[0];
      let circumstance = item.watch[endFrame.level];
      let group = $('<div/>', { 'class': 'Group' });
      appendProperty(group, 'Start', formatCondition(item, circumstance.start));
      if (circumstance.stop) {
        appendProperty(group, 'Stop', formatCondition(item, circumstance.stop));
      }
      return group;
    }

    function formatCondition(item, condition) {
      if (condition === undefined)
        return '';
      let threshold = Units.converter(item.unit).format(condition.threshold);
      let title = 'value is ' + condition.operator + ' ' + threshold;
      let desc = $('<span/>'); 
      desc.append(condition.operator + ' ' + threshold);
      let text = condition.onAverage ? ' on average for last ' : ' for last ';
      if (condition.forTimes > 0) {
        desc.append($('<small/>', { text: text})).append(condition.forTimes + 'x');
        title += text + condition.forTimes + ' values';
      }
      if (condition.forMillis > 0) {
        let time = Units.converter('ms').format(condition.forMillis);
        desc.append($('<small/>', { text: text})).append(time);
        title += text + time;          
      }
      desc.attr('title', title);
      return desc;
    }

    function createStatisticsGroup(item) {
        let endFrame = item.frames[0];
        let startFrame = item.frames[item.frames.length - 1];
        let duration = durationMs(startFrame, endFrame);
        let amberStats = computeStatistics(item, 'amber');
        let redStats = computeStatistics(item, 'red');
        let group = $('<div/>', { 'class': 'Group' });
        appendProperty(group, 'Since', Units.formatTime(startFrame.since));
        appendProperty(group, 'For', formatDuration(duration));
        if (redStats.count > 0)
          appendProperty(group, 'Red', redStats.text);
        if (amberStats.count > 0)
          appendProperty(group, 'Amber', amberStats.text);
      return group;
    }

    function createGeneralGroup(item) {
      let group = $('<div/>', { 'class': 'Group' });
      appendProperty(group, 'Alert', item.serial);
      appendProperty(group, 'Watch', item.name);
      if (item.series)
        appendProperty(group, 'Series', item.series);
      if (item.instance)
        appendProperty(group, 'Instance', item.instance === 'server' ? 'DAS' : item.instance);
      return group;
    }

    function computeStatistics(item, level) {
      const reduceCount = (count, frame) => count + 1;
      const reduceDuration = (duration, frame) => duration + durationMs(frame, frame);
      let frames = item.frames;
      let matches = frames.filter(frame => frame.level == level);
      let count = matches.reduce(reduceCount, 0);
      let duration = matches.reduce(reduceDuration, 0);
      return { 
        count: count,
        duration: duration,
        text: formatDuration(duration) + ' x' + count, 
      };
    }

    function durationMs(frame0, frame1) {
      return (frame1.until === undefined ? new Date() : frame1.until) - frame0.since;
    }

    function appendProperty(parent, label, value) {
      parent.append($('<span/>')
        .append($('<small/>', { text: label + ':' }))
        .append($('<strong/>').append(value))
        ).append('\n'); // so browser will line break;
    }

    function formatDuration(ms) {
      return Units.converter('sec').format(Math.round(ms/1000));
    }

    /**
     * Sorts alerts starting with ongoing most recent red and ending with ended most past amber.
     */
    function sortMostUrgetFirst(a, b) {
      a.frames = a.frames.sort(sortMostRecentFirst);
      b.frames = b.frames.sort(sortMostRecentFirst);
      let aFrame = a.frames[0]; // most recent frame is not at 0
      let bFrame = b.frames[0];
      let aOngoing = aFrame.until === undefined;
      let bOngoing = bFrame.until === undefined;
      if (aOngoing != bOngoing)
        return aOngoing ? -1 : 1;
      let aLevel = aFrame.level;
      let bLevel = bFrame.level;
      if (aLevel != bLevel)
        return aLevel === 'red' ? -1 : 1;
      return bFrame.since - aFrame.since; // start with most recent item
    }

    function sortMostRecentFirst(a, b) {
      return b.since - a.since; // sort most recent frame first 
    }

    return { createComponent: createComponent };
  })();

  /*
  * Public API below:
  *
  * All methods return a jquery element reflecting the given model to be inserted into the DOM using jQuery
  */
  return {
      createSettings: (model) => Settings.createComponent(model),
      createLegend: (model) => Legend.createComponent(model),
      createIndicator: (model) => Indicator.createComponent(model),
      createMenu: (model) => Menu.createComponent(model),
      createAlertTable: (model) => AlertTable.createComponent(model),
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

MonitoringConsole.Chart.Common = (function() {

   function createCustomTooltipFunction(createHtmlTooltip) {
      return function(tooltipModel) {
        let tooltip = $('#chartjs-tooltip');
        if (tooltipModel.opacity === 0) {
          tooltip.css({opacity: 0}); // without this the tooptip sticks and is not removed when moving the mouse away
          return;
        }
        tooltipModel.opacity = 1;
        $(tooltip).empty().append(createHtmlTooltip(tooltipModel.dataPoints));
        var position = this._chart.canvas.getBoundingClientRect(); // `this` will be the overall tooltip
        tooltip.css({opacity: 1, left: position.left + tooltipModel.caretX, top: position.top + tooltipModel.caretY - 20});
      };
   }

   function formatDate(date) {
      if (typeof date === 'number') {
         date = new Date(date);
      }
      let dayOfMonth = date.getDate();
      let month = date.getMonth() + 1;
      let year = date.getFullYear();
      let hour = date.getHours();
      let min = date.getMinutes().toString().padStart(2, '0');
      let sec = date.getSeconds().toString().padStart(2, '0');
      let ms = date.getMilliseconds().toString().padStart(3, '0');
      let now = new Date();
      let diffMs =  now - date;
      let text = `Today ${hour}:${min}:${sec}.${ms}`; 
      if (diffMs < 5000) {
         return text + ' (just now)';
      }
      if (diffMs < 60 * 1000) { // less then a minute ago
         let diffSecs = diffMs / 1000;
         return text + ' (about '+ diffSecs.toFixed(0) + ' seconds ago)';
      }
      if (diffMs < 60 * 60 * 1000) { // less then an hour ago
         let diffMins = diffMs / (60*1000);
         return text + ' (about '+ diffMins.toFixed(0) + ' minutes ago)';  
      }
      let dayOfMonthNow = now.getDate();
      if (dayOfMonth == dayOfMonthNow) {
         return text;
      }
      if (dayOfMonthNow - 1 == dayOfMonth) {
         return `Yesterday ${hour}:${min}:${sec}.${ms}`; 
      }
      return `${dayOfMonth}.${month}.${year} ${hour}:${min}:${sec}.${ms}`; 
   }

   /**
    * Public API below:
    */
   return {
      /**
       * @param {function} createHtmlTooltip - a function that given dataPoints (see Chartjs docs) returns the tooltip HTML jQuery object
       */
      createCustomTooltipFunction: (createHtmlTooltip) => createCustomTooltipFunction(createHtmlTooltip),
      formatDate: (date) => formatDate(date),
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
 * Adapter to line charts of Chart.js
 */ 
MonitoringConsole.Chart.Line = (function() {
	
  const Units = MonitoringConsole.View.Units;
  const Colors = MonitoringConsole.View.Colors;
  const ColorModel = MonitoringConsole.Model.Colors;

  function timeLable(secondsAgo, index, lastIndex, secondsInterval) {
    if (index == lastIndex && secondsAgo == 0)
      return 'now';
    if (index == 0 || index == lastIndex && secondsAgo > 0) {
      if (Math.abs(secondsAgo - 60) <= secondsInterval)
        return '60s ago'; // this corrects off by 1 which is technically inaccurate but still 'more readable' for the user
      if (Math.abs((secondsAgo % 60) - 60) <= secondsInterval)
        return Math.round(secondsAgo / 60) + 'mins ago';
      if (secondsAgo <= 60)
        return secondsAgo +'s ago';
      return Math.floor(secondsAgo / 60) + 'mins ' + (secondsAgo % 60) + 's ago';
    }
    return undefined;
  }

  /**
   * This is like a constant but it needs to yield new objects for each chart.
   */
  function onCreation(widget) {
    let options = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        xAxes: [{
          display: true,
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
            minRotation: 0,
            maxRotation: 0,
            callback: function(value, index, values) {
              if (values.length == 0)
                return value;
              let lastIndex = values.length - 1;
              let reference = new Date(values[lastIndex].value);
              let now = new Date();
              let isLive = now - reference < 5000; // is within the last 5 secs
              if (values.length == 1)
                return isLive ? 'now' : Units.formatTime(new Date(reference));
              let secondsInterval = (values[1].value - values[0].value) / 1000;
              let secondsAgo = (values[lastIndex].value - values[index].value) / 1000;
              if (isLive) {
                return timeLable(secondsAgo, index, lastIndex, secondsInterval);
              }
              let reference2 = new Date(values[lastIndex-1].value);
              let isRecent = now - reference < (5000 + (reference - reference2));
              if (isRecent) {
                return timeLable(secondsAgo, index, lastIndex, secondsInterval);
              }
              if (index != 0 && index != lastIndex)
                return undefined;
              return Units.formatTime(new Date(values[index].value));
            },
          }
        }],
        yAxes: [{
          display: true,
          gridLines: {
            color: 'rgba(100,100,100,0.7)',
            lineWidth: 0.5,
          },
          ticks: {
            beginAtZero: true,
            precision:0, // no decimal places in labels
          },
        }],
      },
      legend: {
          display: false,
      }
    };
    return new Chart(widget.target, {
          type: 'line',
          data: { datasets: [], },
          options: options,
        });
  }

  /**
   * Convertes a array of points given as one dimensional array with alternativ time value elements 
   * to a 2-dimensional array of points with t and y attribute.
   */
  function points1Dto2D(points1d) {
    if (!points1d)
      return [];
    let points2d = new Array(points1d.length / 2);
    for (let i = 0; i < points2d.length; i++)
      points2d[i] = { t: new Date(points1d[i*2]), y: points1d[i*2+1] };
    return points2d;      
  }
	
  function createMinimumLineDataset(seriesData, points, lineColor) {
		return createHorizontalLineDataset(' min ', points, seriesData.observedMin, lineColor, [3, 3]);
  }
    
  function createMaximumLineDataset(seriesData, points, lineColor) {
  	return createHorizontalLineDataset(' max ', points, seriesData.observedMax, lineColor, [15, 3]);
  }
    
  function createAverageLineDataset(seriesData, points, lineColor) {
		return createHorizontalLineDataset(' avg ', points, seriesData.observedSum / seriesData.observedValues, lineColor, [9, 3]);
  }

  function createHorizontalLineDataset(label, points, y, lineColor, dash) {
    let line = {
      data: [{t:points[0].t, y:y}, {t:points[points.length-1].t, y:y}],
      label: label,
      fill:  false,
      borderColor: lineColor,
      borderWidth: 1,
      pointRadius: 0
    };
    if (dash)
      line.borderDash = dash;
    return line;
  }  
    
  function createCurrentLineDataset(widget, seriesData, points, lineColor, bgColor) {
		let pointRadius = widget.options.drawPoints ? 2 : 0;
    let label = seriesData.instance;
    if (widget.series.indexOf('*') > 0)
      label += ': '+ (seriesData.series.replace(new RegExp(widget.series.replace('*', '(.*)')), '$1'));
    return {
			data: points,
			label: label,
			backgroundColor: bgColor,
			borderColor: lineColor,
			borderWidth: 2.5,
      pointRadius: pointRadius,
		};
  }
    
  /**
   * Creates one or more lines for a single series dataset related to the widget.
   * A widget might display multiple series in the same graph generating one or more dataset for each of them.
   */
  function createSeriesDatasets(widget, seriesData) {
    let lineColor = seriesData.legend.color;
    let bgColor = seriesData.legend.background;
  	let points = points1Dto2D(seriesData.points);
  	let datasets = [];
  	datasets.push(createCurrentLineDataset(widget, seriesData, points, lineColor, bgColor));
  	if (points.length > 0 && widget.options.drawAvgLine) {
			datasets.push(createAverageLineDataset(seriesData, points, lineColor));
		}
		if (points.length > 0 && widget.options.drawMinLine && seriesData.observedMin > 0) {
			datasets.push(createMinimumLineDataset(seriesData, points, lineColor));
		}
		if (points.length > 0 && widget.options.drawMaxLine) {
			datasets.push(createMaximumLineDataset(seriesData, points, lineColor));
		}
    let decorations = widget.decorations;
    if (decorations.waterline && decorations.waterline.value) {
      let color = decorations.waterline.color || ColorModel.default('waterline');
      datasets.push(createHorizontalLineDataset(' waterline ', points, decorations.waterline.value, color, [2,2]));
    }
    if (decorations.thresholds.alarming.display) {
      let color = decorations.thresholds.alarming.color || ColorModel.default('alarming');
      datasets.push(createHorizontalLineDataset(' alarming ', points, decorations.thresholds.alarming.value, color, [2,2]));
    }
    if (decorations.thresholds.critical.display) {
      let color = decorations.thresholds.critical.color || ColorModel.default('critical');
      datasets.push(createHorizontalLineDataset(' critical ', points, decorations.thresholds.critical.value, color, [2,2]));      
    }
	  return datasets;
  }

  /**
   * Should be called whenever the configuration of the widget changes in way that needs to be transfered to the chart options.
   * Basically translates the MC level configuration options to Chart.js options
   */
  function onConfigUpdate(widget, chart) {
    let options = chart.options;
    options.elements.line.tension = widget.options.noCurves ? 0 : 0.4;
    let time = 0; //widget.options.drawAnimations ? 1000 : 0;
    options.animation.duration = time;
    options.responsiveAnimationDuration = time;
    let yAxis = options.scales.yAxes[0];
    let converter = Units.converter(widget.unit);
    yAxis.ticks.callback = function(value, index, values) {
      let text = converter.format(value, widget.unit === 'bytes');
      return widget.options.perSec ? text + ' /s' : text;
    };
    yAxis.ticks.suggestedMin = widget.axis.min;
    yAxis.ticks.suggestedMax = widget.axis.max;
    let xAxis = options.scales.xAxes[0];
    xAxis.ticks.source = 'data'; // 'auto' does not allow to put labels at first and last point
    xAxis.ticks.display = widget.options.noTimeLabels !== true;
    options.elements.line.fill = widget.options.noFill !== true;
    return chart;
  }

  function onDataUpdate(update) {
    let data = update.data;
    let widget = update.widget;
    let chart = update.chart();
    let datasets = [];
    for (let j = 0; j < data.length; j++) {
      datasets = datasets.concat(createSeriesDatasets(widget, data[j]));
    }
    chart.data.datasets = datasets;
    chart.update(0);
  }
  
  /**
   * Public API if this chart type (same for all types).
   */
	return {
    onCreation: (widget) => onConfigUpdate(widget, onCreation(widget)),
    onConfigUpdate: (widget, chart) => onConfigUpdate(widget, chart),
    onDataUpdate: (update) => onDataUpdate(update),
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
 * Adapter to horizontal bar charts of Chart.js
 */ 
MonitoringConsole.Chart.Bar = (function() {

  const Units = MonitoringConsole.View.Units;

   function createData(widget, response) {
      let series = [];
      let labels = [];
      let zeroToMinValues = [];
      let observedMinToMinValues = [];
      let minToMaxValues = [];
      let maxToObservedMaxValues = [];
      let showObservedMin = widget.options.drawMinLine;
      let lineColors = [];
      let bgColors = [];
      for (let i = 0; i < response.length; i++) {
        let seriesData = response[i];
        let points = seriesData.points;
        let min = points[1];
        let max = points[1];
        for (let j = 0; j < points.length; j+=2) {
              let value = points[j+1];
              min = Math.min(min, value);
              max = Math.max(max, value);
        }
        labels.push(seriesData.series);
        series.push(seriesData.series);          
        zeroToMinValues.push(showObservedMin ? seriesData.observedMin : min);
        observedMinToMinValues.push(min - seriesData.observedMin);
        minToMaxValues.push(max - min);
        maxToObservedMaxValues.push(seriesData.observedMax - max);
        lineColors.push(seriesData.legend.color);
        bgColors.push(seriesData.legend.background);
      }
      let datasets = [];
      let offset = {
        data: zeroToMinValues,
        backgroundColor: 'transparent',
        borderWidth: {right: 1},
        borderColor: lineColors,
      };
      datasets.push(offset);
      if (showObservedMin) {
         datasets.push({
            data: observedMinToMinValues,
            backgroundColor: bgColors,
            borderWidth: 0,
         });       
      }
      datasets.push({
         data: minToMaxValues,
         backgroundColor: bgColors,
         borderColor: lineColors,
         borderWidth: 1,
         borderSkipped: false,
      });
      if (widget.options.drawMaxLine) {
         datasets.push({
           data: maxToObservedMaxValues,
           backgroundColor: bgColors,
           borderWidth: 0,
         }); 
      }
      return {
        labels: labels,
        series: series,
        datasets: datasets,
      };
   }

   function onCreation(widget) {
      return new Chart(widget.target, {
         type: 'horizontalBar',
         data: { datasets: [] },
         options: {
            maintainAspectRatio: false,
            scales: {
               xAxes: [{
                  stacked: true,
                  ticks: {
                    callback: function(value, index, values) {
                      let converter = Units.converter(widget.unit);
                      return converter.format(converter.parse(value));
                    },
                  },                 
               }],
               yAxes: [{
                  maxBarThickness: 15, //px
                  barPercentage: 1.0,
                  categoryPercentage: 1.0,
                  borderSkipped: false,
                  stacked: true,
                  ticks: {
                     display: false,
                  }
               }]
            },
            legend: {
               display: false,
            },
            onClick: function (event) {
               let bar = this.getElementsAtEventForMode(event, "y", 1)[0];
               let series = bar._chart.config.data.series[bar._index]; 
               if (series.startsWith('ns:trace ') && series.endsWith(' Duration')) {
                  MonitoringConsole.Model.Settings.close();
                  MonitoringConsole.View.onPageMenu();
                  MonitoringConsole.Chart.Trace.onOpenPopup(series);
               }
            }
         }
      });
   }

   function onConfigUpdate(widget, chart) {
      let options = chart.options; 
      return chart;
   }

   function onDataUpdate(update) {
      let data = update.data;
      let widget = update.widget;
      let chart = update.chart();
      chart.data = createData(widget, data);
      chart.update(0);
   }

  /**
   * Public API if this chart type (same for all types).
   */
   return {
      onCreation: (widget) => onConfigUpdate(widget, onCreation(widget)),
      onConfigUpdate: (widget, chart) => onConfigUpdate(widget, chart),
      onDataUpdate: (update) => onDataUpdate(update),
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
 * Horizontal bar charts of Chart.js as used for the gantt chart like trace details view
 */ 
MonitoringConsole.Chart.Trace = (function() {

   const Components = MonitoringConsole.View.Components;
   const Colors = MonitoringConsole.View.Colors;
   const Common = MonitoringConsole.Chart.Common;

   var model = {};
   var chart;

   function onDataUpdate(data) {
      model.data = data;
      onSortByDuration();
   }

   function updateChart() {
      let data = model.data;
      let zeroToMinValues = [];
      let minToMaxValues = [];
      let spans = [];
      let labels = [];
      let operations = {};
      let colorCounter = 0;
      let colors = [];
      let bgColors = [];
      let alpha = MonitoringConsole.Model.Colors.opacity() / 100;
      let palette = MonitoringConsole.Model.Colors.palette();
      data.sort(model.sortBy);
      for (let i = 0; i < data.length; i++) {
         let trace = data[i]; 
         let startTime = trace.startTime;
         for (let j = 0; j < trace.spans.length; j++) {
            let span = trace.spans[j];
            spans.push(span);
            zeroToMinValues.push(span.startTime - startTime);
            minToMaxValues.push(span.endTime - span.startTime);
            labels.push(span.operation);
            if (!operations[span.operation]) {
               let color = Colors.lookup('index', 'line-' + colorCounter, palette);
               colorCounter++;
               operations[span.operation] = {
                  color: color,
                  bgColor: Colors.hex2rgba(color, alpha),
                  count: 1,
                  duration: span.endTime - span.startTime,
               };
            } else {
               let op = operations[span.operation];
               op.count += 1;
               op.duration += span.endTime - span.startTime;
            }
            colors.push(operations[span.operation].color);
            bgColors.push(operations[span.operation].bgColor);
         }
         spans.push(null);
         zeroToMinValues.push(0);
         minToMaxValues.push(0);
         labels.push('');
         colors.push('transparent');
         bgColors.push('transparent');
      }
      let datasets = [ {
            data: zeroToMinValues,
            backgroundColor: 'transparent',
         }, {
            data: minToMaxValues,
            backgroundColor: bgColors, //'rgba(153, 153, 153, 0.2)',
            borderColor: colors,
            borderWidth: {top: 1, right: 1},
         }
      ];
      if (!chart) {
         chart = onCreation();
      }
      let legend = [];
      for (let [label, operationData] of Object.entries(operations)) {
         legend.push({label: label, value: (operationData.duration / operationData.count).toFixed(2) + 'ms (avg)', color: operationData.color});
      }
      $('#trace-legend').empty().append(Components.createLegend(legend));
      $('#trace-chart-box').height(10 * spans.length + 30);
      chart.data = { 
         datasets: datasets,
         spans: spans,
         labels: labels,
      };
      chart.options.onClick = function(event)  {
        updateDomSpanDetails(data, spans[this.getElementsAtEventForMode(event, "y", 1)[0]._index]); 
      };
      addCustomTooltip(chart, spans);
      chart.update(0);
   }

   function autoLink(text) {
      if (text.startsWith('http://') || text.startsWith('https://'))
         return $('<a/>', { href: text, text: text});
      return $('<span/>', { text: text });
   }

   function addCustomTooltip(chart, spans) {
      chart.options.tooltips.custom = Common.createCustomTooltipFunction(function(dataPoints) {
         let index = dataPoints[0].index;
         let span = spans[index];
         let body = $('<div/>', {'class': 'Tooltip'});
         body
            .append($('<div/>').text("ID: "+span.id))
            .append($('<div/>').text("Start: "+Common.formatDate(span.startTime)))
            .append($('<div/>').text("End: "+Common.formatDate(span.endTime)));
         return body;
      });      
   }

   function onCreation() {
      return new Chart('trace-chart', {
         type: 'horizontalBar',
         data: { datasets: [] },
         options: {
            maintainAspectRatio: false,
            scales: {
               xAxes: [{
                  stacked: true,
                  position: 'top',
                  ticks: {
                     callback: function(value, index, values) {
                        if (value > 1000) {
                           return (value / 1000).toFixed(1)+"s";
                        }
                        return value+"ms";
                     }
                  },
                  scaleLabel: {
                     display: true,
                     labelString: 'Relative Timeline'
                  }
               }],
               yAxes: [{
                  maxBarThickness: 15, //px
                  barPercentage: 1.0,
                  categoryPercentage: 1.0,
                  borderSkipped: false,
                  stacked: true,
                  gridLines: {
                     display:false
                  },
                  ticks: {
                     display: false,
                  },
               }]
            },
            legend: {
               display: false,
            },
            tooltips: {
               enabled: false,
               position: 'nearest',
               filter: (tooltipItem) => tooltipItem.datasetIndex > 0, // remove offsets (not considered data, just necessary to offset the visible bars)
            },
         }
      });
   }

   function updateDomSpanDetails(data, span) {
      if (!span)
         return;
      let tags = { id: 'settings-tags', caption: 'Tags' , entries: []};
      let settings = [
         { id: 'settings-span', caption: 'Span' , entries: [
            { label: 'ID', input: span.id},
            { label: 'Operation', input: span.operation},
            { label: 'Start', input: Common.formatDate(new Date(span.startTime))},
            { label: 'End', input:  Common.formatDate(new Date(span.endTime))},
            { label: 'Duration', input: (span.duration / 1000000) + 'ms'},
         ]},
         tags,
      ];
      for (let [key, value] of Object.entries(span.tags)) {
         if (value.startsWith('[') && value.endsWith(']')) {
            value = value.slice(1,-1);
         }
         tags.entries.push({ label: key, input: autoLink(value)});
      }
      Components.onSettingsUpdate(settings);
   }


   function onDataRefresh() {
      $.getJSON("api/trace/data/"+model.series, onDataUpdate);
   }

   function onOpenPopup(series) {
      $('#chart-grid').hide();
      $('#panel-trace').show();
      model.series = series;
      let menu = { id: 'TraceMenu', groups: [
         { icon: '&#128472;', description: 'Refresh', onClick: onDataRefresh },
         { label: 'Sorting', items: [
            { icon: '&#9202;', label: 'Sort By Wall Time (past to recent)', onClick: onSortByWallTime },
            { icon: '&#8987;', label: 'Sort By Duration (slower to faster)', onClick: onSortByDuration },
         ]},
         { icon: '&times;', description: 'Back to main view', onClick: onClosePopup },
      ]};
      $('#trace-menu').replaceWith(Components.createMenu(menu));
      onDataRefresh();
   }

   function onClosePopup() {
      if (chart) {
         chart.destroy();
         chart = undefined;
      }
      $('#panel-trace').hide();
      $('#chart-grid').show();
   }

   function onSortByWallTime() {
      model.sortBy = (a,b) => a.startTime - b.startTime; // past to recent
      updateChart();
   }

   function onSortByDuration() {
      model.sortBy = (a,b) => b.elapsedTime - a.elapsedTime; // slow to fast
      updateChart();
   }

   /**
    * Public API below:
    */
   return {
      onOpenPopup: (series) => onOpenPopup(series),
      onClosePopup: () => onClosePopup(),
      onDataRefresh: () => onDataRefresh(),
      onSortByWallTime: () => onSortByWallTime(),
      onSortByDuration: () => onSortByDuration(),
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
 * Main API to update or manipulate the view of the generic page.
 **/
MonitoringConsole.View = (function() {

    const NS_TEXTS = {
        web: 'Web Statistics',
        http: 'HTTP Statistics',
        jvm: 'JVM Statistics',
        metric: 'MP Metrics',
        trace: 'Request Tracing',
        map: 'Cluster Map Storage Statistics',
        topic: 'Cluster Topic IO Statistics',
        monitoring: 'Monitoring Console Internals',
        health: 'Health Checks',
        other: 'Other',
    };

    const Components = MonitoringConsole.View.Components;
    const Units = MonitoringConsole.View.Units;
    const Colors = MonitoringConsole.View.Colors;

    /**
     * Updates the DOM with the page navigation tabs so it reflects current model state
     */ 
    function updatePageNavigation() {
        let pages = MonitoringConsole.Model.listPages();
        let activePage = pages.find(page => page.active);
        let items = pages.filter(page => !page.active).map(function(page) {
            return { label: page.name ? page.name : '(Unnamed)', onClick: () => onPageChange(MonitoringConsole.Model.Page.changeTo(page.id)) };
        });
        let nav = { id: 'Navigation', groups: [
            {label: activePage.name, items: items }
        ]};
        $('#Navigation').replaceWith(Components.createMenu(nav));
    }

    function updateMenu() {
        let hasPreset = MonitoringConsole.Model.Page.hasPreset();
        let isPaused = MonitoringConsole.Model.Refresh.isPaused();
        let settingsOpen = MonitoringConsole.Model.Settings.isDispayed();
        let toggleSettings = function() { MonitoringConsole.View.onPageMenu(); updateMenu(); };
        let menu = { id: 'Menu', groups: [
            { icon: '&#128463;', label: 'Page', items: [
                { label: 'New...', icon: '&#128459;', description: 'Add a new blank page...', onClick: () => MonitoringConsole.View.onPageChange(MonitoringConsole.Model.Page.create('(Unnamed)')) },
                { label: 'Delete', icon: '&times;', description: 'Delete current page', disabled: hasPreset, onClick: MonitoringConsole.View.onPageDelete },
                { label: 'Reset', icon: '&#128260;', description: 'Reset Page to Preset', disabled: !hasPreset, onClick: MonitoringConsole.View.onPageReset },
            ]},
            { icon: '&#128472;', label: 'Data Refresh', items: [
                { label: 'Pause', icon: '&#9208;', description: 'Pause Data Refresh', hidden: isPaused, onClick: function() { MonitoringConsole.Model.Refresh.pause(); updateMenu(); updateSettings(); } },
                { label: 'Unpause', icon: '&#9654;', description: 'Unpause Data Refresh', hidden: !isPaused, onClick: function() { MonitoringConsole.Model.Refresh.resume(); updateMenu(); updateSettings(); } },
                { label: 'Slow', icon: '&#128034;', description: 'Slow Data Refresh Rate', onClick: function() { MonitoringConsole.Model.Refresh.resume(4); updateMenu(); updateSettings(); } },
                { label: 'Normal', icon: '&#128008;', description: 'Normal Data Refresh Rate', onClick: function() { MonitoringConsole.Model.Refresh.resume(2); updateMenu(); updateSettings(); } },
                { label: 'Fast', icon: '&#128007;', description: 'Fast Data Refresh Rate', onClick: function() { MonitoringConsole.Model.Refresh.resume(1); updateMenu(); updateSettings(); } },
            ]},
            { icon: '&#9707;', label: 'Layout', items: [
                { label: '1 Column', icon: '&#11034;', description: 'Use one column layout', onClick: () => MonitoringConsole.View.onPageLayoutChange(1) },
                { label: ' 2 Columns', icon: '&#11034;&#11034;', description: 'Use two column layout', onClick: () => MonitoringConsole.View.onPageLayoutChange(2) },
                { label: ' 3 Columns', icon: '&#11034;&#11034;&#11034;', description: 'Use three column layout', onClick: () => MonitoringConsole.View.onPageLayoutChange(3) },
                { label: ' 4 Columns', icon: '&#11034;&#11034;&#11034;&#11034;', description: 'Use four column layout', onClick: () => MonitoringConsole.View.onPageLayoutChange(4) },
            ]},
            { icon: '&#9881;', label: 'Settings', clickable: true, items: [
                { label: 'Hide', icon: '&times;', hidden: !settingsOpen, onClick: toggleSettings },
                { label: 'Show', icon: '&plus;', hidden: settingsOpen, onClick: toggleSettings },
                { label: 'Import...', icon: '&#9111;', description: 'Import Configuration...', onClick: () => $('#cfgImport').click() },
                { label: 'Export...', icon: '&#9112;', description: 'Export Configuration...', onClick: MonitoringConsole.View.onPageExport },                
            ]},
        ]};
        $('#Menu').replaceWith(Components.createMenu(menu));
    }

    /**
     * Updates the DOM with the page and selection settings so it reflects current model state
     */ 
    function updateSettings() {
        let panelConsole = $('#console');
        if (MonitoringConsole.Model.Settings.isDispayed()) {
            if (!panelConsole.hasClass('state-show-settings')) {
                panelConsole.addClass('state-show-settings');                
            }
            let singleSelection = MonitoringConsole.Model.Page.Widgets.Selection.isSingle();
            let groups = [];
            groups.push(createGlobalSettings(singleSelection));
            groups.push(createColorSettings());
            groups.push(createPageSettings());
            if (singleSelection) {
                groups = groups.concat(createWidgetSettings(MonitoringConsole.Model.Page.Widgets.Selection.first()));
            }
            $('#Settings').replaceWith(Components.createSettings({id: 'Settings', groups: groups }));
        } else {
            panelConsole.removeClass('state-show-settings');
        }
    }

    function updateDomOfWidget(parent, widget) {
        if (!parent) {
            parent = $('#widget-'+widget.target);
            if (!parent) {
                return; // can't update
            }
        }
        if (parent.children().length == 0) {
            let previousParent = $('#widget-'+widget.target);
            if (previousParent.length > 0 && previousParent.children().length > 0) {
                previousParent.children().appendTo(parent);
            } else {
                parent.append(createWidgetToolbar(widget));
                parent.append(createWidgetTargetContainer(widget));
                parent.append(Components.createAlertTable({}));
                parent.append(Components.createLegend([]));                
                parent.append(Components.createIndicator({}));
            }
        }
        if (widget.selected) {
            parent.addClass('chart-selected');
        } else {
            parent.removeClass('chart-selected');
        }
    }

    /**
     * Each chart needs to be in a relative positioned box to allow responsive sizing.
     * This fuction creates this box including the canvas element the chart is drawn upon.
     */
    function createWidgetTargetContainer(widget) {
        return $('<div/>', { id: widget.target + '-box', "class": "widget-chart-box", style: 'width: calc(100% - 20px); height: calc(100% - 100px);' })
            .append($('<canvas/>',{ id: widget.target }));
    }

    function toWords(str) {
        // camel case to words
        let res = str.replace(/([A-Z]+)/g, " $1").replace(/([A-Z][a-z])/g, " $1");
        if (res.indexOf('.') > 0) {
            // dots to words with upper casing each word
            return res.replace(/\.([a-z])/g, " $1").split(' ').map((s) => s.charAt(0).toUpperCase() + s.substring(1)).join(' ');
        }
        return res;
    }

    function formatSeriesName(series) {
        let endOfTags = series.lastIndexOf(' ');
        let metric = endOfTags <= 0 ? series : series.substring(endOfTags + 1);
        if (endOfTags <= 0 )
            return toWords(metric);
        let tags = series.substring(0, endOfTags).split(' ');
        let text = '';
        let metricText = toWords(metric);
        let grouped = false;
        for (let i = 0; i < tags.length; i++) {
            let tag = tags[i];
            let tagName = tag.substring(0, tag.indexOf(':'));
            let tagValue = tag.substring(tag.indexOf(':') + 1);
            if (tagName ===  '@') {
                grouped = true;
                text += metricText;
                text += ': <strong>'+tagValue+'</strong> ';
            } else {
                text +=' <span>'+tagName+':<strong>'+tagValue+'</strong></span> ';
            }
        }
        if (!grouped)
            text += metricText;
        return text;
    }

    function createWidgetToolbar(widget) {
        let series = widget.series;
        let menu = { groups: [
            { icon: '&#9881;', items: [
                { icon: '&times;', label: 'Remove', onClick: () => onWidgetDelete(series)},
                { icon: '&ltri;&rtri;', label: 'Larger', onClick: () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanMore(series)) },
                { icon: '&rtri;&ltri;', label: 'Samller', onClick: () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanLess(series)) },
                { icon: '&rtri;', label: 'Move Right', onClick: () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveRight(series)) },
                { icon: '&ltri;', label: 'Move Left', onClick: () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveLeft(series)) },
                { icon: '&triangle;', label: 'Move Up', onClick: () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveUp(series)) },
                { icon: '&dtri;', label: 'Move Down', onClick: () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveDown(series)) },
                { icon: '&#9881;', label: 'More...', onClick: () => onOpenWidgetSettings(series) },
            ]},
        ]};
        let title = widget.displayName ? widget.displayName : formatSeriesName(widget.series);
        return $('<div/>', {"class": "widget-title-bar"})
            .append($('<h3/>', {title: 'Select '+series})
                .html(title)
                .click(() => onWidgetToolbarClick(widget)))
            .append(Components.createMenu(menu));
    }

    function createGlobalSettings(initiallyCollapsed) {
        return { id: 'settings-global', caption: 'Global', collapsed: initiallyCollapsed, entries: [
            { label: 'Data Refresh', input: [
                { type: 'value', unit: 'sec', value: MonitoringConsole.Model.Refresh.interval(), onChange: (val) => MonitoringConsole.Model.Refresh.interval(val) },
                { label: 'paused', type: 'checkbox', value: MonitoringConsole.Model.Refresh.isPaused(), onChange: function(checked) { MonitoringConsole.Model.Refresh.paused(checked); updateMenu(); } },
            ]},
            { label: 'Page Rotation', input: [
                { type: 'value', unit: 'sec', value: MonitoringConsole.Model.Settings.Rotation.interval(), onChange: (val) => MonitoringConsole.Model.Settings.Rotation.interval(val) },
                { label: 'enabled', type: 'checkbox', value: MonitoringConsole.Model.Settings.Rotation.isEnabled(), onChange: (checked) => MonitoringConsole.Model.Settings.Rotation.enabled(checked) },
            ]},
        ]};
    }

    function createColorSettings() {
        let colorModel = MonitoringConsole.Model.Colors;
        function createChangeColorDefaultFn(name) {
            return (color) => { colorModel.default(name, color); updateSettings(); };
        }
        function createColorDefaultSettingMapper(name) {
            return { label: name[0].toUpperCase() + name.slice(1), type: 'color', value: colorModel.default(name), onChange: createChangeColorDefaultFn(name) };
        }        
        return { id: 'settings-colors', caption: 'Colors', collapsed: $('#settings-colors').children('tr:visible').length == 1, entries: [
            { label: 'Scheme', type: 'dropdown', options: Colors.schemes(), value: undefined, onChange: (name) => { Colors.scheme(name); updateSettings(); } },
            { label: 'Data #', type: 'color', value: colorModel.palette(), onChange: (colors) => colorModel.palette(colors) },
            { label: 'Defaults', input: [
                ['waterline', 'alarming', 'critical'].map(createColorDefaultSettingMapper),
                ['white', 'green', 'amber', 'red'].map(createColorDefaultSettingMapper)]},
            { label: 'Opacity', type: 'value', unit: 'percent', value: colorModel.opacity(), onChange: (opacity) => colorModel.opacity(opacity) },
        ]};
    }

    function createWidgetSettings(widget) {
        let colorModel = MonitoringConsole.Model.Colors;
        let options = widget.options;
        let unit = widget.unit;
        let thresholds = widget.decorations.thresholds;
        let settings = [];
        settings.push({ id: 'settings-widget', caption: 'Widget', entries: [
            { label: 'Display Name', type: 'text', value: widget.displayName, onChange: (widget, value) => widget.displayName = value},
            { label: 'Type', type: 'dropdown', options: {line: 'Time Curve', bar: 'Range Indicator', alert: 'Alerts Table'}, value: widget.type, onChange: (widget, selected) => widget.type = selected},
            { label: 'Column / Item', input: [
                { type: 'range', min: 1, max: 4, value: 1 + (widget.grid.column || 0), onChange: (widget, value) => widget.grid.column = value - 1},
                { type: 'range', min: 1, max: 4, value: 1 + (widget.grid.item || 0), onChange: (widget, value) => widget.grid.item = value - 1},
            ]},             
            { label: 'Span', type: 'range', min: 1, max: 4, value: widget.grid.span || 1, onChange: (widget, value) => widget.grid.span = value},
        ]});
        settings.push({ id: 'settings-data', caption: 'Data', entries: [
            { label: 'Series', input: widget.series },
            { label: 'Unit', input: [
                { type: 'dropdown', options: {count: 'Count', ms: 'Milliseconds', ns: 'Nanoseconds', bytes: 'Bytes', percent: 'Percentage'}, value: widget.unit, onChange: function(widget, selected) { widget.unit = selected; updateSettings(); }},
                { label: '1/sec', type: 'checkbox', value: options.perSec, onChange: (widget, checked) => options.perSec = checked},
            ]},
            { label: 'Coloring', type: 'dropdown', options: { instance: 'Instance Name', series: 'Series Name', index: 'Result Set Index' }, value: widget.coloring, onChange: (widget, value) => widget.coloring = value,
                description: 'What value is used to select the index from the color palette' },            
            { label: 'Upscaling', description: 'Upscaling is sometimes needed to convert the original value range to a more user freindly display value range', input: [
                { type: 'range', min: 1, value: widget.scaleFactor, onChange: (widget, value) => widget.scaleFactor = value, 
                    description: 'A factor multiplied with each value to upscale original values in a graph, e.g. to move a range 0-1 to 0-100%'},
                { label: 'decimal value', type: 'checkbox', value: options.decimalMetric, onChange: (widget, checked) => options.decimalMetric = checked,
                    description: 'Values that are collected as decimal are converted to a integer with 4 fix decimal places. By checking this option this conversion is reversed to get back the original decimal range.'},
            ]},
            { label: 'Extra Lines', input: [
                { label: 'Min', type: 'checkbox', value: options.drawMinLine, onChange: (widget, checked) => options.drawMinLine = checked},
                { label: 'Max', type: 'checkbox', value: options.drawMaxLine, onChange: (widget, checked) => options.drawMaxLine = checked},
                { label: 'Avg', type: 'checkbox', value: options.drawAvgLine, onChange: (widget, checked) => options.drawAvgLine = checked},            
            ]},
            { label: 'Display', input: [
                { label: 'Points', type: 'checkbox', value: options.drawPoints, onChange: (widget, checked) => options.drawPoints = checked },
                { label: 'Fill', type: 'checkbox', value: !options.noFill, onChange: (widget, checked) => options.noFill = !checked},
                { label: 'Curvy', type: 'checkbox', value: !options.noCurves, onChange: (widget, checked) => options.noCurves = !checked},
            ]},
            { label: 'X-Axis', input: [
                { label: 'Labels', type: 'checkbox', value: !options.noTimeLabels, onChange: (widget, checked) => options.noTimeLabels = !checked},
            ]},            
            { label: 'Y-Axis', input: [
                { label: 'Min', type: 'value', unit: unit, value: widget.axis.min, onChange: (widget, value) => widget.axis.min = value},
                { label: 'Max', type: 'value', unit: unit, value: widget.axis.max, onChange: (widget, value) => widget.axis.max = value},
            ]},
        ]});
        settings.push({ id: 'settings-decorations', caption: 'Decorations', entries: [
            { label: 'Waterline', input: [
                { type: 'value', unit: unit, value: widget.decorations.waterline.value, onChange: (widget, value) => widget.decorations.waterline.value = value },
                { type: 'color', value: widget.decorations.waterline.color, defaultValue: colorModel.default('waterline'), onChange: (widget, value) => widget.decorations.waterline.color = value },
            ]},
            { label: 'Alarming Threshold', input: [
                { type: 'value', unit: unit, value: thresholds.alarming.value, onChange: (widget, value) => thresholds.alarming.value = value },
                { type: 'color', value: thresholds.alarming.color, defaultValue: colorModel.default('alarming'), onChange: (widget, value) => thresholds.alarming.color = value },
                { label: 'Line', type: 'checkbox', value: thresholds.alarming.display, onChange: (widget, checked) => thresholds.alarming.display = checked },
            ]},
            { label: 'Critical Threshold', input: [
                { type: 'value', unit: unit, value: thresholds.critical.value, onChange: (widget, value) => thresholds.critical.value = value },
                { type: 'color', value: thresholds.critical.color, defaultValue: colorModel.default('critical'), onChange: (widget, value) => thresholds.critical.color = value },
                { label: 'Line', type: 'checkbox', value: thresholds.critical.display, onChange: (widget, checked) => thresholds.critical.display = checked },
            ]},                
            { label: 'Threshold Reference', type: 'dropdown', options: { off: 'Off', now: 'Most Recent Value', min: 'Minimum Value', max: 'Maximum Value', avg: 'Average Value'}, value: thresholds.reference, onChange: (widget, selected) => thresholds.reference = selected},
            //TODO add color for each threshold
        ]});
        settings.push({ id: 'settings-status', caption: 'Status', collapsed: true, description: 'Set a text for an assessment status', entries: [
            { label: '"No Data"', type: 'text', value: widget.status.missing.hint, onChange: (widget, text) => widget.status.missing.hint = text},
            { label: '"Alaraming"', type: 'text', value: widget.status.alarming.hint, onChange: (widget, text) => widget.status.alarming.hint = text},
            { label: '"Critical"', type: 'text', value: widget.status.critical.hint, onChange: (widget, text) => widget.status.critical.hint = text},
        ]});
        return settings;       
    }

    function createPageSettings() {
        let nsSelection = $('<select/>');
        nsSelection.append($('<option/>').val('-').text('(Please Select)'));
        nsSelection.on('mouseenter focus', function() {
            if (nsSelection.children().length == 1) {
                 let nsAdded = [];
                 MonitoringConsole.Model.listSeries(function(names) {
                    $.each(names, function() {
                        let key = this;
                        let ns =  this.substring(3, this.indexOf(' '));
                        if (NS_TEXTS[ns] === undefined) {
                            ns = 'other';
                        }
                        if (nsAdded.indexOf(ns) < 0) {
                            nsAdded.push(ns);
                            nsSelection.append($('<option/>').val(ns).text(NS_TEXTS[ns]));
                        }
                    });
                });
            }
        });
        let widgetsSelection = $('<select/>').attr('disabled', true);
        nsSelection.change(function() {
            widgetsSelection.empty();
            widgetsSelection.append($('<option/>').val('').text('(Please Select)'));
            MonitoringConsole.Model.listSeries(function(names) {
                $.each(names, function() {
                    let key = this;
                    let ns =  this.substring(3, this.indexOf(' '));
                    if (NS_TEXTS[ns] === undefined) {
                        ns = 'other';
                    }
                    if (ns === nsSelection.val()) {
                        widgetsSelection.append($("<option />").val(key).text(this.substring(this.indexOf(' '))));                        
                    }
                });
                widgetsSelection.attr('disabled', false);
            });
        });
        let widgetSeries = $('<input />', {type: 'text'});
        widgetsSelection.change(() => widgetSeries.val(widgetsSelection.val()));
        let pageNameOnChange = MonitoringConsole.Model.Page.hasPreset() ? undefined : function(text) {
            if (MonitoringConsole.Model.Page.rename(text)) {
                updatePageNavigation();                        
            }
        };
        return { id: 'settings-page', caption: 'Page', entries: [
            { label: 'Name', type: 'text', value: MonitoringConsole.Model.Page.name(), onChange: pageNameOnChange },
            { label: 'Page Rotation', input: [
                { label: 'Include in Rotation', type: 'checkbox', value: MonitoringConsole.Model.Page.rotate(), onChange: (checked) => MonitoringConsole.Model.Page.rotate(checked) },
            ]},
            { label: 'Add Widgets', input: () => 
                $('<span/>')
                .append(nsSelection)
                .append(widgetsSelection)
                .append(widgetSeries)
                .append($('<button/>', {title: 'Add selected metric', text: 'Add'})
                    .click(() => onPageChange(MonitoringConsole.Model.Page.Widgets.add(widgetSeries.val()))))
            },
        ]};
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~[ Event Handlers ]~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    function onWidgetToolbarClick(widget) {
        MonitoringConsole.Model.Page.Widgets.Selection.toggle(widget.series, true);
        updateDomOfWidget(undefined, widget);
        updateSettings();
    }

    function onWidgetDelete(series) {
        if (window.confirm('Do you really want to remove the chart from the page?')) {
            onPageChange(MonitoringConsole.Model.Page.Widgets.remove(series));
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

    function createLegendModel(widget, data, alerts) {
        if (!data)
            return [{ label: 'Connection Lost', value: '?', color: 'red', assessments: { status: 'error' } }];
        if (widget.type == 'alert')
            return createLegendModelFromAlerts(widget, data, alerts);
        if (Array.isArray(data) && data.length == 0)
            return [{ label: 'No Data', value: '?', color: '#0096D6', assessments: {status: 'missing' }}];
        let legend = [];
        let format = Units.converter(widget.unit).format;
        let palette = MonitoringConsole.Model.Colors.palette();
        let alpha = MonitoringConsole.Model.Colors.opacity() / 100;
        for (let j = 0; j < data.length; j++) {
            let seriesData = data[j];
            let label = seriesData.instance;
            if (widget.series.indexOf('*') > 0) {
                label = seriesData.series.replace(new RegExp(widget.series.replace('*', '(.*)')), '$1').replace('_', ' ');
            }
            let points = seriesData.points;
            let avgOffN = widget.options.perSec ? Math.min(points.length / 2, 4) : 1;
            let avg = 0;
            for (let n = 0; n < avgOffN; n++)
                avg += points[points.length - 1 - (n * 2)];
            avg /= avgOffN;
            let value = format(avg, widget.unit === 'bytes' || widget.unit === 'ns');
            if (widget.options.perSec)
                value += ' /s';
            let color = Colors.lookup(widget.coloring, getColorKey(widget, seriesData, j), palette);
            let background = Colors.hex2rgba(color, alpha);
            let item = { 
                label: label, 
                value: value, 
                color: color,
                background: background,
                assessments: seriesData.assessments,
            };
            legend.push(item);
            seriesData.legend = item;
        }
        return legend;
    }

    function createLegendModelFromAlerts(widget, data, alerts) {
        if (!Array.isArray(alerts))
            return []; //TODO use white, green, amber and red to describe the watch in case of single watch
        const colorModel = MonitoringConsole.Model.Colors;
        let palette = colorModel.palette();
        let alpha = colorModel.opacity() / 100;
        let instances = {};
        for (let i = 0; i < alerts.length; i++) {
            let alert = alerts[i];
            instances[alert.instance] = Units.Alerts.maxLevel(alert.level, instances[alert.instance]);
        }
        return Object.entries(instances).map(function([instance, level]) {
            let color = Colors.lookup('instance', instance, palette);
            return {
                label: instance,
                value: level == undefined ? 'White' : level.charAt(0).toUpperCase() + level.slice(1),
                color: color,
                background: Colors.hex2rgba(color, alpha),
                assessments: { status: level, color: colorModel.default(level) },                
            };
        });
    }

    function getColorKey(widget, seriesData, index) {
        switch (widget.coloring) {
            case 'index': return 'line-' + index;
            case 'series': return seriesData.series;
            case 'instance': 
            default: return seriesData.instance;
        }
    } 

    function createIndicatorModel(widget, data) {
        if (!data)
            return { status: 'error' };
        if (Array.isArray(data) && data.length == 0)
            return { status: 'missing', text: widget.status.missing.hint };
        let status = 'normal';
        for (let j = 0; j < data.length; j++) {
            let seriesData = data[j];
            if (seriesData.assessments.status == 'alarming' && status != 'critical' || seriesData.assessments.status == 'critical')
                status = seriesData.assessments.status;
        }
        let statusInfo = widget.status[status] || {};
        return { status: status, text: statusInfo.hint };
    }

    function createAlertTableModel(widget, alerts) {
        let items = [];
        if (Array.isArray(alerts)) {
            const colorModel = MonitoringConsole.Model.Colors;
            let palette = MonitoringConsole.Model.Colors.palette();
            for (let i = 0; i < alerts.length; i++) {
                let alert = alerts[i];
                let include = widget.type === 'alert' || alert.level === 'red' || alert.level === 'amber';
                if (include) {
                    let frames = alert.frames.map(function(frame) {
                        return {
                            level: frame.level,
                            since: frame.start,
                            until: frame.end,
                            color: colorModel.default(frame.level),
                        };
                    });
                    let instanceColoring = widget.coloring === 'instance' || widget.coloring === undefined;
                    items.push({
                        serial: alert.serial,
                        name: alert.initiator.name,
                        unit: alert.initiator.unit,
                        acknowledged: alert.acknowledged,
                        series: alert.series == widget.series ? undefined : alert.series,
                        instance: alert.instance,
                        color: instanceColoring ? Colors.lookup('instance', alert.instance, palette) : undefined,
                        frames: frames,
                        watch: alert.initiator,
                    });                    
                }
            }
        }
        return { id: widget.target + '_alerts', items: items };
    }

    /**
     * This function is called when data was received or was failed to receive so the new data can be applied to the page.
     *
     * Depending on the update different content is rendered within a chart box.
     */
    function onDataUpdate(update) {
        let widget = update.widget;
        let data = update.data;
        let alerts = update.alerts;
        updateDomOfWidget(undefined, widget);
        let widgetNode = $('#widget-' + widget.target);
        let legendNode = widgetNode.find('.Legend').first();
        let indicatorNode = widgetNode.find('.Indicator').first();
        let alertsNode = widgetNode.find('.AlertTable').first();
        let legend = createLegendModel(widget, data, alerts); // OBS this has side effect of setting .legend attribute in series data
        if (data !== undefined && widget.type !== 'alert') {
            MonitoringConsole.Chart.getAPI(widget).onDataUpdate(update);
        }
        alertsNode.replaceWith(Components.createAlertTable(createAlertTableModel(widget, alerts)));
        legendNode.replaceWith(Components.createLegend(legend));
        indicatorNode.replaceWith(Components.createIndicator(createIndicatorModel(widget, data)));
    }

    /**
     * This function refleshes the page with the given layout.
     */
    function onPageUpdate(layout) {
        let numberOfColumns = layout.length;
        let maxRows = layout[0].length;
        let table = $("<table/>", { id: 'chart-grid', 'class': 'columns-'+numberOfColumns + ' rows-'+maxRows });
        let padding = 30;
        let headerHeight = 40;
        let minRowHeight = 160;
        let rowsPerScreen = maxRows;
        let windowHeight = $(window).height();
        let rowHeight = 0;
        while (rowsPerScreen > 0 && rowHeight < minRowHeight) {
            rowHeight = Math.round((windowHeight - headerHeight) / rowsPerScreen) - padding; // padding is subtracted
            rowsPerScreen--; // in case we do another round try one less per screen
        }
        if (rowHeight == 0) {
            rowHeight = windowHeight - headerHeight - padding;
        }
        for (let row = 0; row < maxRows; row++) {
            let tr = $("<tr/>");
            for (let col = 0; col < numberOfColumns; col++) {
                let cell = layout[col][row];
                if (cell) {
                    let span = cell.span;
                    let height = (span * rowHeight);
                    let td = $("<td/>", { id: 'widget-'+cell.widget.target, colspan: span, rowspan: span, 'class': 'widget', style: 'height: '+height+"px;"});
                    updateDomOfWidget(td, cell.widget);
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
        MonitoringConsole.Chart.Trace.onClosePopup();
        onPageUpdate(layout);
        updatePageNavigation();
        updateSettings();
        updateMenu();
    }

    function onOpenWidgetSettings(series) {
        MonitoringConsole.Model.Page.Widgets.Selection.clear();
        MonitoringConsole.Model.Page.Widgets.Selection.toggle(series);
        MonitoringConsole.Model.Settings.open();
        updateSettings();
    }

    /**
     * Public API of the View object:
     */
    return {
        Units: Units,
        Colors: Colors,
        Components: Components,
        onPageReady: function() {
            // connect the view to the model by passing the 'onDataUpdate' function to the model
            // which will call it when data is received
            onPageChange(MonitoringConsole.Model.init(onDataUpdate, onPageChange));
            Colors.scheme('Payara', false);
        },
        onPageChange: (layout) => onPageChange(layout),
        onPageUpdate: (layout) => onPageUpdate(layout),
        onPageReset: () => onPageChange(MonitoringConsole.Model.Page.reset()),
        onPageImport: (src) => MonitoringConsole.Model.importPages(src, onPageChange),
        onPageExport: () => onPageExport('monitoring-console-config.json', MonitoringConsole.Model.exportPages()),
        onPageMenu: function() { MonitoringConsole.Model.Settings.toggle(); updateSettings(); },
        onPageLayoutChange: (numberOfColumns) => onPageUpdate(MonitoringConsole.Model.Page.arrange(numberOfColumns)),
        onPageDelete: function() {
            if (window.confirm("Do you really want to delete the current page?")) { 
                onPageUpdate(MonitoringConsole.Model.Page.erase());
                updatePageNavigation();
            }
        },
        onOpenWidgetSettings: (series) => onOpenWidgetSettings(series),
    };
})();
