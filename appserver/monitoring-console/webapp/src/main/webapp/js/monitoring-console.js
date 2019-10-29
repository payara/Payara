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

	const UI_PRESETS = {
			pages: {
				core: {
					name: 'Core',
					numberOfColumns: 3,
					widgets: [
						{ series: 'ns:jvm HeapUsage', unit: 'percent',  
							grid: { item: 0, column: 0, span: 1}, 
							axis: { min: 0, max: 100 },
							decorations: {
								thresholds: { reference: 'now', alarming: { value: 50, display: true }, critical: { value: 80, display: true }}}},
						{ series: 'ns:jvm CpuUsage', unit: 'percent',
							grid: { item: 1, column: 0, span: 1}, 
							axis: { min: 0, max: 100 },
							decorations: {
								thresholds: { reference: 'now', alarming: { value: 50, display: true }, critical: { value: 80, display: true }}}},							
						{ series: 'ns:jvm ThreadCount', unit: 'count',  
							grid: { item: 0, column: 1, span: 1}},
						{ series: 'ns:http ThreadPoolCurrentThreadCount', unit: 'count',
							grid: { item: 1, column: 1, span: 1},
							status: { missing: { hint: TEXT_HTTP_HIGH }}},
						{ series: 'ns:web RequestCount', unit: 'count',
							grid: { item: 0, column: 2, span: 1}, 
							options: { perSec: true },
							status: { missing: { hint: TEXT_WEB_HIGH }}},
						{ series: 'ns:web ActiveSessions', unit: 'count',
							grid: { item: 1, column: 2, span: 1},
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
							status: { missing: { hint: TEXT_REQUEST_TRACING }}}
					]
				},
				http: {
					name: 'HTTP',
					numberOfColumns: 3,
					widgets: [
						{ series: 'ns:http ConnectionQueueCountOpenConnections', unit: 'count',
							grid: { column: 0, item: 0},
							status: { missing : { hint: TEXT_HTTP_HIGH }}},
						{ series: 'ns:http ThreadPoolCurrentThreadCount', unit: 'count',
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
				}
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
		var settings = {};
		
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
			if (!widget.unit)
				widget.unit = 'count';
			if (typeof widget.options !== 'object')
				widget.options = {};
			//TODO no data can be a good thing (a series hopefully does not come up => render differently to "No Data" => add a config for that switch)
			if (typeof widget.grid !== 'object')
				widget.grid = {};
			if (typeof widget.decorations !== 'object')
				widget.decorations = {};
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
			let isPagesOnly = !userInterface.pages || !userInterface.settings;
			if (!isPagesOnly)
				settings = userInterface.settings;
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
			return Math.max(column.length, column.findIndex((elem,index,array) => array.slice(index, index + n).every(e => e === null))); 
      	}
		
		return {
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
				if (!pages[pageId])
					return undefined;
				settings.home = pageId;
				return pages[settings.home];
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
				let widgets = pages[settings.home].widgets;
				let widget = { series: series };
				widgets[series] = sanityCheckWidget(widget);
				widget.selected = true;
			},
			
			configureWidget: function(widgetUpdate, series) {
				let selected = series
					? [pages[settings.home].widgets[series]]
					: Object.values(pages[settings.home].widgets).filter(widget => widget.selected);
				selected.forEach(widget => widgetUpdate(widget));
				doStore();
			},
			
			select: function(series) {
				let widget = pages[settings.home].widgets[series];
				widget.selected = widget.selected !== true;
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
			
			pause: doPause,
			isPaused: () => intervalFn === undefined,
		};
	})();
	
	let Update = (function() {

		function retainLastMinute(data) {
			let startOfLastMinute = Date.now() - 65000;
			data.forEach(function(seriesData) {
				let src = seriesData.points;
				if (src.length == 4 && src[2] >= startOfLastMinute) {
					seriesData.points = [src[2] - 59000, src[1], src[2], src[3]];
				} else {
					let points = [];
					for (let i = 0; i < src.length; i += 2) {
						if (src[i] >= startOfLastMinute) {
							points.push(src[i]);
							points.push(src[i+1]);
						}
					}
					seriesData.points = points;				
				}
			});
			return data.filter(seriesData => seriesData.points.length >= 2);
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
				Object.values(widgets).forEach(function(widget) {
					let data = retainLastMinute(response[widget.series]);
					if (widget.options.perSec)
						perSecond(data);
					addAssessment(widget, data);
					onDataUpdate({
						widget: widget,
						data: data,
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


	function doInit(onDataUpdate) {
		UI.load();
		Interval.init(function() {
			let widgets = UI.currentPage().widgets;
			let payload = {};
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
			request.done(Update.createOnSuccess(widgets, onDataUpdate));
			request.fail(Update.createOnError(widgets, onDataUpdate));
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
				Charts.update(widget);
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
			isPaused: Interval.isPaused,
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

			hasPreset: UI.hasPreset,
			
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
                    widget.grid.item = 0;
	            }),

	            moveDown: (series) => doConfigureWidget(series, function(widget) {
                    widget.grid.item += 1.1;
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
      if (!factors)
         return formatDecimal(valueAsNumber);
      let largestFactorUnit;
      let largestFactor = 0;
      for (let [unit, factor] of Object.entries(factors)) {
         if (unit != '_' && (valueAsNumber >= 0 && factor > 0 || valueAsNumber < 0 && factor < 0)
            && (useDecimals && (valueAsNumber / factor) >= 1 || !hasDecimalPlaces(valueAsNumber / factor))
            && factor > largestFactor) {
            largestFactor = factor;
            largestFactorUnit = unit;
         }
      }
      if (!largestFactorUnit) {
         return formatDecimal(valueAsNumber);
      }
      if (useDecimals) {
         return formatDecimal(valueAsNumber / largestFactor) + largestFactorUnit;
      }
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

   /**
    * Public API below:
    */
   return {
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
         return {
            format: function(valueAsNumber, useDecimals) {
               switch(unit) {
                  case 'ms': return formatNumber(valueAsNumber, MS_FACTORS, useDecimals);
                  case 'ns': return formatNumber(valueAsNumber, NS_FACTORS, useDecimals);
                  case 'bytes': return formatNumber(valueAsNumber, BYTES_FACTORS, useDecimals);
                  case 'percent': return formatNumber(valueAsNumber, PERCENT_FACTORS, useDecimals);
                  default: return formatNumber(valueAsNumber);
               }
            },
            parse: function(valueAsString) {
               switch(unit) {
                  case 'ms': return parseNumber(valueAsString, MS_FACTORS);
                  case 'ns': return parseNumber(valueAsString, NS_FACTORS);
                  case 'bytes': return parseNumber(valueAsString, BYTES_FACTORS);
                  case 'percent': return parseNumber(valueAsString, PERCENT_FACTORS);
                  default: return parseNumber(valueAsString);
               }
            },
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
 * Data/Model driven view components.
 *
 * Each of them gets passed a model which updates the view of the component to the model state.
 **/
MonitoringConsole.View.Components = (function() {

   const Units = MonitoringConsole.View.Units;
   const Selection = MonitoringConsole.Model.Page.Widgets.Selection;

   /**
    * This is the side panel showing the details and settings of widgets
    */
   let Settings = (function() {

      function emptyPanel() {
         return $('#Settings').empty();
      }

      function createHeaderRow(model) {
         let caption = model.label;
         return $('<tr/>').append($('<th/>', {colspan: 2})
             .html(caption)
             .click(function() {
                 let tr = $(this).closest('tr').next();
                 let toggleAll = tr.children('th').length > 0;
                 while (tr.length > 0 && (toggleAll || tr.children('th').length == 0)) {
                     if (tr.children('th').length == 0) {
                         tr.children().toggle();                    
                     }
                     tr = tr.next();
                 }
         }));
      }

      function createTable(model) {
         let table = $('<table />', { id: model.id });
         if (model.caption)
            table.append(createHeaderRow({ label: model.caption }));
         return table;
      }

      function createRow(model, inputs) {
         let components = $.isFunction(inputs) ? inputs() : inputs;
         if (typeof components === 'string') {
            components = document.createTextNode(components);
         }
         return $('<tr/>').append($('<td/>').text(model.label)).append($('<td/>').append(components));   
      }

      function createCheckboxInput(model) {
         return $("<input/>", { type: 'checkbox', checked: model.value })
             .on('change', function() {
                 let checked = this.checked;
                 Selection.configure((widget) => model.onChange(widget, checked));
             });
      }

      function createRangeInput(model) {
         let attributes = {type: 'number', value: model.value};
         if (model.min)
            attributes.min = model.min;
         if (model.max)
            attributes.max = model.max;         
         return $('<input/>', attributes)
             .on('input change', function() {  
                 let val = this.valueAsNumber;
                 MonitoringConsole.View.onPageUpdate(Selection.configure((widget) => model.onChange(widget, val)));
             });
      }

      function createDropdownInput(model) {
         let dropdown = $('<select/>');
         Object.keys(model.options).forEach(option => dropdown.append($('<option/>', {text:model.options[option], value:option, selected: model.value === option})));
         dropdown.change(() => MonitoringConsole.View.onPageUpdate(Selection.configure((widget) => model.onChange(widget, dropdown.val()))));
         return dropdown;
      }

      function createValueInput(model) {
         if (model.unit === 'percent')
            return createRangeInput({min: 0, max: 100, value: model.value, onChange: model.onChange });
         return createTextInput(model, Units.converter(model.unit));
      }

      function createTextInput(model, converter) {
         if (!converter)
            converter = { format: (str) => str, parse: (str) => str };
         let input = $('<input/>', {type: 'text', value: converter.format(model.value) });
         input.on('input change', function() {
            let val = converter.parse(this.value);
            MonitoringConsole.View.onPageUpdate(Selection.configure((widget) => model.onChange(widget, val)));
         });
         return input;
      }

      function createInput(model) {
         switch (model.type) {
            case 'checkbox': return createCheckboxInput(model);
            case 'dropdown': return createDropdownInput(model);
            case 'range'   : return createRangeInput(model);
            case 'value'   : return createValueInput(model);
            case 'text'    : return createTextInput(model);
            default        : return model.input;
         }
      }

      function onUpdate(model) {
         let panel = emptyPanel();
         for (let t = 0; t < model.length; t++) {
            let group = model[t];
            let table = createTable(group);
            panel.append(table);
            for (let r = 0; r < group.entries.length; r++) {
               let entry = group.entries[r];
               let type = entry.type;
               let auto = type === undefined;
               let input = entry.input;
               if (type == 'header' || auto && input === undefined) {
                  table.append(createHeaderRow(entry));
               } else if (!auto) {
                  table.append(createRow(entry, createInput(entry)));
               } else {
                  if (Array.isArray(input)) {
                     let multiInput = $('<div/>');
                     for (let i = 0; i < input.length; i++) {
                        multiInput.append(createInput(input[i]));
                        if (input[i].label) {
                           multiInput.append($('<label/>').html(input[i].label));
                        }                        
                     }
                     input = multiInput;
                  }
                  table.append(createRow(entry, input));
               }
            }
         }
      }

      return { onUpdate: onUpdate };
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
         return $('<li/>', attrs)
               .append($('<span/>').text(label))
               .append($('<strong/>').text(strong))
               .append($('<span/>').text(normal));
      }

      function onCreation(model) {
         let legend = $('<ol/>',  {'class': 'Legend'});
         for (let i = 0; i < model.length; i++) {
            legend.append(createItem(model[i]));
         }
         return legend;
      }

      return { onCreation: onCreation };
   })();

  /**
   * Component drawn for each widget legend item to indicate data status.
   */
  let Indicator = (function() {

    function onCreation(model) {
       if (!model.text) {
          return $('<div/>', {'class': 'Indicator', style: 'display: none;'});
       }
       let html = model.text.replace(/\*([^*]+)\*/g, '<b>$1</b>').replace(/_([^_]+)_/g, '<i>$1</i>');
       return $('<div/>', { 'class': 'Indicator status-' + model.status }).html(html);
    }

    return { onCreation: onCreation };
  })();

  /**
   * Component for any of the text+icon menus/toolbars.
   */
  let Menu = (function() {

    function onCreation(model) {
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

    return { onCreation: onCreation };
  })();

  /*
  * Public API below:
  */
  return {
      /**
       * Call to update the settings side panel with the given model
       */
      onSettingsUpdate: (model) => Settings.onUpdate(model),
      /**
       * Returns a jquery legend element reflecting the given model to be inserted into the DOM
       */
      onLegendCreation: (model) => Legend.onCreation(model),
      /**
       * Returns a jquery indicator element reflecting the given model to be inserted into the DOM
       */
      onIndicatorCreation: (model) => Indicator.onCreation(model),
      /**
       * Returns a jquery menu element reflecting the given model to inserted into the DOM
       */
      onMenuCreation: (model) => Menu.onCreation(model),
      //TODO add id to model and make it an update?
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

   const DEFAULT_BG_COLORS = [
      'rgba(240, 152, 27, 0.2)',
      'rgba(0, 140, 196, 0.2)',
      'rgba(153, 102, 255, 0.2)',
      'rgba(255, 99, 132, 0.2)',
      'rgba(54, 162, 235, 0.2)',
      'rgba(255, 206, 86, 0.2)',
      'rgba(75, 192, 192, 0.2)',
      'rgba(255, 159, 64, 0.2)'
   ];
   const DEFAULT_LINE_COLORS = [
      'rgba(240, 152, 27, 1)',
      'rgba(0, 140, 196, 1)',
      'rgba(153, 102, 255, 1)',
      'rgba(255, 99, 132, 1)',
      'rgba(54, 162, 235, 1)',
      'rgba(255, 206, 86, 1)',
      'rgba(75, 192, 192, 1)',
      'rgba(255, 159, 64, 1)'
   ];

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
      backgroundColor: (n) => DEFAULT_BG_COLORS[n],
      backgroundColors: () => DEFAULT_BG_COLORS,
      lineColor: (n) => DEFAULT_LINE_COLORS[n],
      lineColors: () => DEFAULT_LINE_COLORS,
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
  const Common = MonitoringConsole.Chart.Common;

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
              let isLive = new Date() - reference < 5000; // is within the last 5 secs
              if (isLive) {
                return index == 0 ? (((values[lastIndex].value - values[0].value)/1000)+1) +'s ago' : index == lastIndex ? 'now' : undefined;
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
    let bgColor = seriesData.legend.backgroundColor;
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
    if (widget.decorations.waterline) {
      datasets.push(createHorizontalLineDataset(' waterline ', points, widget.decorations.waterline, 'Aqua', [2,2]));
    }
    if (widget.decorations.thresholds.alarming.display) {
      datasets.push(createHorizontalLineDataset(' alarming ', points, widget.decorations.thresholds.alarming.value, 'gold', [2,2]));
    }
    if (widget.decorations.thresholds.critical.display) {
      datasets.push(createHorizontalLineDataset(' critical ', points, widget.decorations.thresholds.critical.value, 'crimson', [2,2]));      
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
  const Common = MonitoringConsole.Chart.Common;

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
        bgColors.push(seriesData.legend.backgroundColor);
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
               operations[span.operation] = {
                  color: Common.lineColor(colorCounter),
                  bgColor: Common.backgroundColor(colorCounter++),
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
      $('#trace-legend').empty().append(Components.onLegendCreation(legend));
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
      return $(document.createTextNode(text));
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
      $('#trace-menu').replaceWith(Components.onMenuCreation(menu));
      onDataRefresh();
   }

   function onClosePopup() {
      if (chart) {
         chart.destroy();
         chart = undefined;
      }
      $('#panel-trace').hide();
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

    const Components = MonitoringConsole.View.Components;
    const Units = MonitoringConsole.View.Units;

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
        $('#Navigation').replaceWith(Components.onMenuCreation(nav));
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
            { icon: '&#128472;', label: 'Auto-Refresh', clickable: true, items: [
                { label: 'Pause', icon: '&#9208;', description: 'Pause Data Update', hidden: isPaused, onClick: function() { MonitoringConsole.Model.Refresh.pause(); updateMenu(); } },
                { label: 'Resume', icon: '&#9654;', description: 'Resume Normal Data Update', hidden: !isPaused, onClick: function() { MonitoringConsole.Model.Refresh.resume(); updateMenu(); } },
                { label: 'Slow', icon: '&#128034;', description: 'Slow Data Update', onClick: function() { MonitoringConsole.Model.Refresh.slow(); updateMenu(); } },
                { label: 'Fast', icon: '&#128007;', description: 'Fast Data Update', onClick: function() { MonitoringConsole.Model.Refresh.fast(); updateMenu(); } },
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
        $('#Menu').replaceWith(Components.onMenuCreation(menu));
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
            let settings = [];
            settings.push(createPageSettings());
            settings.push(createDataSettings());
            if (MonitoringConsole.Model.Page.Widgets.Selection.isSingle()) {
                settings = settings.concat(createWidgetSettings(MonitoringConsole.Model.Page.Widgets.Selection.first()));
            }
            Components.onSettingsUpdate(settings);
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
                parent.append(Components.onLegendCreation([]));                
                parent.append(Components.onIndicatorCreation({}));
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
        return $('<div/>', { id: widget.target + '-box', "class": "widget-chart-box" })
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

    function formatSeriesName(widget) {
        let series = widget.series;
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

    function createWidgetToolbar(widget, expanded) {
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
        return $('<div/>', {"class": "widget-title-bar"})
            .append($('<h3/>', {title: 'Select '+series}).html(formatSeriesName(widget))
                .click(() => onWidgetToolbarClick(widget)))
            .append(Components.onMenuCreation(menu))
            ;
    }

    function createWidgetSettings(widget) {
        let options = widget.options;
        let unit = widget.unit;
        let thresholds = widget.decorations.thresholds;
        let settings = [];
        settings.push({ id: 'settings-widget', caption: 'Widget', entries: [
            { label: 'Type', type: 'dropdown', options: {line: 'Time Curve', bar: 'Range Indicator'}, value: widget.type, onChange: (widget, selected) => widget.type = selected},
            { label: 'Column / Item', input: [
                { type: 'range', min: 1, max: 4, value: 1 + (widget.grid.column || 0), onChange: (widget, value) => widget.grid.column = value - 1},
                { type: 'range', min: 1, max: 4, value: 1 + (widget.grid.item || 0), onChange: (widget, value) => widget.grid.item = value - 1},
            ]},             
            { label: 'Span', type: 'range', min: 1, max: 4, value: widget.grid.span || 1, onChange: (widget, value) => widget.grid.span = value},
        ]});
        settings.push({ id: 'settings-data', caption: 'Data', entries: [
            { label: 'Unit', input: [
                { type: 'dropdown', options: {count: 'Count', ms: 'Milliseconds', ns: 'Nanoseconds', bytes: 'Bytes', percent: 'Percentage'}, value: widget.unit, onChange: (widget, selected) => widget.unit = selected},
                { label: '1/sec', type: 'checkbox', value: options.perSec, onChange: (widget, checked) => options.perSec = checked},
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
            { label: 'Waterline', type: 'value', unit: unit, value: widget.decorations.waterline, onChange: (widget, value) => widget.decorations.waterline = value },
            { label: 'Threshold Reference', type: 'dropdown', options: { off: 'Off', now: 'Most Recent Value', min: 'Minimum Value', max: 'Maximum Value', avg: 'Average Value'}, value: thresholds.reference, onChange: (widget, selected) => thresholds.reference = selected},
            { label: 'Alarming Threshold', input: [
                { type: 'value', unit: unit, value: thresholds.alarming.value, onChange: (widget, value) => thresholds.alarming.value = value },
                { label: 'Line', type: 'checkbox', value: thresholds.alarming.display, onChange: (widget, checked) => thresholds.alarming.display = checked },
            ]},
            { label: 'Critical Threshold', input: [
                { type: 'value', unit: unit, value: thresholds.critical.value, onChange: (widget, value) => thresholds.critical.value = value },
                { label: 'Line', type: 'checkbox', value: thresholds.critical.display, onChange: (widget, checked) => thresholds.critical.display = checked },
            ]},                
            //TODO add color for each threshold
        ]});
        settings.push({ id: 'settings-status', caption: 'Status', entries: [
            { label: '"No Data"', type: 'text', value: widget.status.missing.hint, onChange: (widget, text) => widget.status.missing.hint = text},
            { label: '"Alaraming"', type: 'text', value: widget.status.alarming.hint, onChange: (widget, text) => widget.status.alarming.hint = text},
            { label: '"Critical"', type: 'text', value: widget.status.critical.hint, onChange: (widget, text) => widget.status.critical.hint = text},
        ]});
        return settings;       
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
        let widgetSeries = $('<input />', {type: 'text'});
        widgetsSelection.change(() => widgetSeries.val(widgetsSelection.val()));
        
        return { id: 'settings-page', caption: 'Page', entries: [
            { label: 'Name', input: () => 
                $('<input/>', { type: 'text', value: MonitoringConsole.Model.Page.name() })
                .on("propertychange change keyup paste input", function() {
                    if (MonitoringConsole.Model.Page.rename(this.value)) {
                        updatePageNavigation();                        
                    }
                })
            },
            { label: 'Widgets', input: () => 
                $('<span/>')
                .append(widgetsSelection)
                .append(widgetSeries)
                .append($('<button/>', {title: 'Add selected metric', text: 'Add'})
                    .click(() => onPageUpdate(MonitoringConsole.Model.Page.Widgets.add(widgetSeries.val()))))
            },
        ]};
    }

    function createDataSettings() {
        let instanceSelection = $('<select />', {multiple: true});
        $.getJSON("api/instances/", function(instances) {
            for (let i = 0; i < instances.length; i++) {
                instanceSelection.append($('<option/>', { value: instances[i], text: instances[i], selected:true}));
            }
        });
        return { id: 'settings-data', caption: 'Data', entries: [
            { label: 'Instances', input: instanceSelection }
        ]};
    }

    

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~[ Event Handlers ]~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    function onWidgetToolbarClick(widget) {
        MonitoringConsole.Model.Page.Widgets.Selection.toggle(widget.series);
        updateDomOfWidget(undefined, widget);
        updatePageAndSelectionSettings();
    }

    function onWidgetDelete(series) {
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

    function createLegendComponent(widget, data) {
        if (!data)
            return [{ label: 'Connection Lost', value: '?', color: 'red', assessments: { status: 'error' } }];
        if (Array.isArray(data) && data.length == 0) {
            return [{ label: 'No Data', value: '?', color: 'Violet', assessments: {status: 'missing' }}];
        }
        let legend = [];
        let format = Units.converter(widget.unit).format;
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
            let value = format(avg, widget.unit === 'bytes');
            if (widget.options.perSec)
                value += ' /s';
            let item = { 
                label: label, 
                value: value, 
                color: MonitoringConsole.Chart.Common.lineColor(j),
                backgroundColor: MonitoringConsole.Chart.Common.backgroundColor(j),
                assessments: seriesData.assessments,
            };
            legend.push(item);
            data[j].legend = item;
        }
        return legend;
    }

    function createIndicatorComponent(widget, data) {
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

    /**
     * This function is called when data was received or was failed to receive so the new data can be applied to the page.
     *
     * Depending on the update different content is rendered within a chart box.
     */
    function onDataUpdate(update) {
        let widget = update.widget;
        let data = update.data;
        updateDomOfWidget(undefined, widget);
        let widgetNode = $('#widget-'+widget.target);
        let legendNode = widgetNode.find('.Legend').first();
        let indicatorNode = widgetNode.find('.Indicator').first();
        let legend = createLegendComponent(widget, data); // OBS this has side effect of setting .legend attribute in series data
        if (data) {
            MonitoringConsole.Chart.getAPI(widget).onDataUpdate(update);
        }
        legendNode.replaceWith(Components.onLegendCreation(legend));
        indicatorNode.replaceWith(Components.onIndicatorCreation(createIndicatorComponent(widget, data)));
    }

    /**
     * This function refleshes the page with the given layout.
     */
    function onPageUpdate(layout) {
        let numberOfColumns = layout.length;
        let maxRows = layout[0].length;
        let table = $("<table/>", { id: 'chart-grid', 'class': 'columns-'+numberOfColumns + ' rows-'+maxRows });
        let rowHeight = Math.round(($(window).height() - 100) / maxRows) - 30; // padding is subtracted
        for (let row = 0; row < maxRows; row++) {
            let tr = $("<tr/>");
            for (let col = 0; col < numberOfColumns; col++) {
                let cell = layout[col][row];
                if (cell) {
                    let span = cell.span;
                    let td = $("<td/>", { id: 'widget-'+cell.widget.target, colspan: span, rowspan: span, 'class': 'widget', style: 'height: '+(span * rowHeight)+"px;"});
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
        updatePageAndSelectionSettings();
        updateMenu();
    }

    function onOpenWidgetSettings(series) {
        MonitoringConsole.Model.Page.Widgets.Selection.clear();
        MonitoringConsole.Model.Page.Widgets.Selection.toggle(series);
        MonitoringConsole.Model.Settings.open();
        updatePageAndSelectionSettings();
    }

    /**
     * Public API of the View object:
     */
    return {
        Units: Units,
        Components: Components,
        onPageReady: function() {
            // connect the view to the model by passing the 'onDataUpdate' function to the model
            // which will call it when data is received
            onPageChange(MonitoringConsole.Model.init(onDataUpdate));
        },
        onPageChange: (layout) => onPageChange(layout),
        onPageUpdate: (layout) => onPageUpdate(layout),
        onPageReset: () => onPageChange(MonitoringConsole.Model.Page.reset()),
        onPageImport: (src) => MonitoringConsole.Model.importPages(src, onPageChange),
        onPageExport: () => onPageExport('monitoring-console-config.json', MonitoringConsole.Model.exportPages()),
        onPageMenu: function() { MonitoringConsole.Model.Settings.toggle(); updatePageAndSelectionSettings(); },
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
