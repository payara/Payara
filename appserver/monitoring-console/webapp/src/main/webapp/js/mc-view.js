/*
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
  
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

    const Controller = MonitoringConsole.Controller;
    const Components = MonitoringConsole.View.Components;
    const Units = MonitoringConsole.View.Units;
    const Colors = MonitoringConsole.View.Colors;
    const Theme = MonitoringConsole.Model.Theme;

    /**
     * Updates the DOM with the page navigation tabs so it reflects current model state
     */ 
    function updatePageNavigation(selectedPage) {
        let pages = MonitoringConsole.Model.listPages();
        let activePage = selectedPage || pages.find(page => page.active).name;
        let items = pages.map(function(page) {
            return { label: page.name ? page.name : '(Unnamed)', onClick: () => onPageChange(MonitoringConsole.Model.Page.changeTo(page.id)) };
        });
        items.push({ label: 'Watches', onClick: onOpenWatchSettings });
        let nav = { id: 'Navigation', groups: [
            {label: activePage, items: items }
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
                parent.append(Components.createAnnotationTable({}));
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
        const Widgets = MonitoringConsole.Model.Page.Widgets;
        let widgetId = widget.id;
        let items = [
            { icon: '&times;', label: 'Remove', onClick: () => onWidgetDelete(widgetId)},
            { icon: '&ltri;', label: 'Move Left', onClick: () => onPageUpdate(Widgets.moveLeft(widgetId)) },
            { icon: '&rtri;', label: 'Move Right', onClick: () => onPageUpdate(Widgets.moveRight(widgetId)) },                
        ];
        if (widget.type === 'annotation') {
            items.push({ icon: '&#9202', label: 'Sort By Wall Time', onClick: () => Widgets.configure(widgetId, (widget) => widget.sort = 'time') });
            items.push({ icon: '&#128292;', label: 'Sort By Value', onClick: () => Widgets.configure(widgetId, (widget) => widget.sort = 'value') });
        }
        items.push({ icon: '&#128295;', label: 'Edit...', onClick: () => onOpenWidgetSettings(widgetId) });
        let menu = { groups: [
            { icon: '&#9881;', items: items },
        ]};
        let title = widget.displayName ? widget.displayName : formatSeriesName(widget.series);
        return $('<div/>', {"class": "widget-title-bar"})
            .append(Components.createMenu(menu))
            .append($('<h3/>', {title: widget.series})
                .html(title)
                .click(() => onWidgetToolbarClick(widget)))            
            ;
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
            { label: 'Watches', input: $('<button/>').text('Open Settings').click(onOpenWatchSettings) },
        ]};
    }

    function createColorSettings() {
        function createChangeColorDefaultFn(name) {
            return (color) => { Theme.configure(theme => theme.colors[name] = color); updateSettings(); };
        }
        function createChangeOptionFn(name) {
            return (value) => { Theme.configure(theme => theme.options[name] = value); };
        }    
        function createColorDefaultSettingMapper(name) {
            let label = Units.Alerts.name(name);
            if (label === undefined)
                label = name[0].toUpperCase() + name.slice(1);
            return { label: label, type: 'color', value: Theme.color(name), onChange: createChangeColorDefaultFn(name) };
        }
        let collapsed = $('#settings-colors').children('tr:visible').length <= 1;
        return { id: 'settings-colors', caption: 'Colors', collapsed: collapsed, entries: [
            { label: 'Scheme', type: 'dropdown', options: Colors.schemes(), value: undefined, onChange: (name) => { Colors.scheme(name); updateSettings(); } },
            { label: 'Data #', type: 'color', value: Theme.palette(), onChange: (colors) => Theme.palette(colors) },
            { label: 'Defaults', input: [
                ['error', 'missing'].map(createColorDefaultSettingMapper),
                ['alarming', 'critical', 'waterline'].map(createColorDefaultSettingMapper),
                ['white', 'green', 'amber', 'red'].map(createColorDefaultSettingMapper)]},
            { label: 'Opacity', description: 'Fill transparency 0-100%', input: [
                { type: 'value', unit: 'percent', value: Theme.option('opacity'), onChange: createChangeOptionFn('opacity') },
            ]},
            { label: 'Thickness', description: 'Line thickness 1-8 (each step is equivalent to 0.5px)', input: [
                { type: 'range', min: 1, max: 8, value: Theme.option('line-width'), onChange: createChangeOptionFn('line-width') },
            ]},
        ]};
    }

    function createWidgetSettings(widget) {
        let options = widget.options;
        let unit = widget.unit;
        let thresholds = widget.decorations.thresholds;
        let settings = [];
        let collapsed = $('#settings-widget').children('tr:visible').length <= 1;
        let typeOptions = { line: 'Time Curve', bar: 'Range Indicator', alert: 'Alerts', annotation: 'Annotations' };
        let modeOptions = widget.type == 'annotation' ? { table: 'Table', list: 'List' } : { list: '(Default)' };
        settings.push({ id: 'settings-widget', caption: 'Widget', collapsed: collapsed, entries: [
            { label: 'Display Name', type: 'text', value: widget.displayName, onChange: (widget, value) => widget.displayName = value},
            { label: 'Type', type: 'dropdown', options: typeOptions, value: widget.type, onChange: (widget, selected) => widget.type = selected},
            { label: 'Mode', type: 'dropdown', options: modeOptions, value: widget.mode, onChange: (widget, selected) => widget.mode = selected},
            { label: 'Column / Item', input: [
                { type: 'range', min: 1, max: 4, value: 1 + (widget.grid.column || 0), onChange: (widget, value) => widget.grid.column = value - 1},
                { type: 'range', min: 1, max: 8, value: 1 + (widget.grid.item || 0), onChange: (widget, value) => widget.grid.item = value - 1},
            ]},             
            { label: 'Size', input: [
                { label: '&nbsp;x', type: 'range', min: 1, max: 4, value: widget.grid.colspan || 1, onChange: (widget, value) => widget.grid.colspan = value},
                { type: 'range', min: 1, max: 4, value: widget.grid.rowspan || 1, onChange: (widget, value) => widget.grid.rowspan = value},
            ]},
        ]});
        settings.push({ id: 'settings-data', caption: 'Data', entries: [
            { label: 'Series', input: widget.series },
            { label: 'Unit', input: [
                { type: 'dropdown', options: Units.names(), value: widget.unit, onChange: function(widget, selected) { widget.unit = selected; updateSettings(); }},
                { label: '1/sec', type: 'checkbox', value: options.perSec, onChange: (widget, checked) => widget.options.perSec = checked},
            ]},
            { label: 'Upscaling', description: 'Upscaling is sometimes needed to convert the original value range to a more user freindly display value range', input: [
                { type: 'range', min: 1, value: widget.scaleFactor, onChange: (widget, value) => widget.scaleFactor = value, 
                    description: 'A factor multiplied with each value to upscale original values in a graph, e.g. to move a range 0-1 to 0-100%'},
                { label: 'decimal value', type: 'checkbox', value: options.decimalMetric, onChange: (widget, checked) => widget.options.decimalMetric = checked,
                    description: 'Values that are collected as decimal are converted to a integer with 4 fix decimal places. By checking this option this conversion is reversed to get back the original decimal range.'},
            ]},
            { label: 'Extra Lines', input: [
                { label: 'Min', type: 'checkbox', value: options.drawMinLine, onChange: (widget, checked) => widget.options.drawMinLine = checked},
                { label: 'Max', type: 'checkbox', value: options.drawMaxLine, onChange: (widget, checked) => widget.options.drawMaxLine = checked},
                { label: 'Avg', type: 'checkbox', value: options.drawAvgLine, onChange: (widget, checked) => widget.options.drawAvgLine = checked},            
            ]},
            { label: 'Lines', input: [
                { label: 'Points', type: 'checkbox', value: options.drawPoints, onChange: (widget, checked) => widget.options.drawPoints = checked},
                { label: 'Curvy', type: 'checkbox', value: options.drawCurves, onChange: (widget, checked) => widget.options.drawCurves = checked},
            ]},
            { label: 'Background', input: [
                { label: 'Fill', type: 'checkbox', value: !options.noFill, onChange: (widget, checked) => widget.options.noFill = !checked},
            ]},
            { label: 'X-Axis', input: [
                { label: 'Labels', type: 'checkbox', value: !options.noTimeLabels, onChange: (widget, checked) => widget.options.noTimeLabels = !checked},
            ]},            
            { label: 'Y-Axis', input: [
                { label: 'Min', type: 'value', unit: unit, value: widget.axis.min, onChange: (widget, value) => widget.axis.min = value},
                { label: 'Max', type: 'value', unit: unit, value: widget.axis.max, onChange: (widget, value) => widget.axis.max = value},
            ]},
            { label: 'Coloring', type: 'dropdown', options: { instance: 'Instance Name', series: 'Series Name', index: 'Result Set Index', 'instance-series': 'Instance and Series Name' }, value: widget.coloring, onChange: (widget, value) => widget.coloring = value,
                description: 'What value is used to select the index from the color palette' },
            { label: 'Fields', type: 'text', value: (widget.fields || []).join(' '), onChange: (widget, value) => widget.fields = value == undefined || value == '' ? undefined : value.split(/[ ,]+/),
                description: 'Selection and order of annotation fields to display, empty for auto selection and default order' },
            {label: 'Annotations', type: 'checkbox', value: !options.noAnnotations, onChange: (widget, checked) => widget.options.noAnnotations = !checked}                
        ]});
        settings.push({ id: 'settings-decorations', caption: 'Decorations', entries: [
            { label: 'Waterline', input: [
                { type: 'value', unit: unit, value: widget.decorations.waterline.value, onChange: (widget, value) => widget.decorations.waterline.value = value },
                { type: 'color', value: widget.decorations.waterline.color, defaultValue: Theme.color('waterline'), onChange: (widget, value) => widget.decorations.waterline.color = value },
            ]},
            { label: 'Alarming Threshold', input: [
                { type: 'value', unit: unit, value: thresholds.alarming.value, onChange: (widget, value) => widget.decorations.thresholds.alarming.value = value },
                { type: 'color', value: thresholds.alarming.color, defaultValue: Theme.color('alarming'), onChange: (widget, value) => thresholds.alarming.color = value },
                { label: 'Line', type: 'checkbox', value: thresholds.alarming.display, onChange: (widget, checked) => thresholds.alarming.display = checked },
            ]},
            { label: 'Critical Threshold', input: [
                { type: 'value', unit: unit, value: thresholds.critical.value, onChange: (widget, value) => widget.decorations.thresholds.critical.value = value },
                { type: 'color', value: thresholds.critical.color, defaultValue: Theme.color('critical'), onChange: (widget, value) => widget.decorations.thresholds.critical.color = value },
                { label: 'Line', type: 'checkbox', value: thresholds.critical.display, onChange: (widget, checked) => widget.decorations.thresholds.critical.display = checked },
            ]},                
            { label: 'Threshold Reference', type: 'dropdown', options: { off: 'Off', now: 'Most Recent Value', min: 'Minimum Value', max: 'Maximum Value', avg: 'Average Value'}, value: thresholds.reference, onChange: (widget, selected) => widget.decorations.thresholds.reference = selected},
        ]});
        settings.push({ id: 'settings-status', caption: 'Status', collapsed: true, description: 'Set a text for an assessment status', entries: [
            { label: '"No Data"', type: 'text', value: widget.status.missing.hint, onChange: (widget, text) => widget.status.missing.hint = text},
            { label: '"Alaraming"', type: 'text', value: widget.status.alarming.hint, onChange: (widget, text) => widget.status.alarming.hint = text},
            { label: '"Critical"', type: 'text', value: widget.status.critical.hint, onChange: (widget, text) => widget.status.critical.hint = text},
        ]});
        let alerts = widget.decorations.alerts;
        settings.push({ id: 'settings-alerts', caption: 'Alerts', collapsed: true, entries: [
            { label: 'Filter', input: [
                [
                    { label: 'Ambers', type: 'checkbox', value: alerts.noAmber, onChange: (widget, checked) => widget.decorations.alerts.noAmber = checked},
                    { label: 'Reds', type: 'checkbox', value: alerts.noRed, onChange: (widget, checked) => widget.decorations.alerts.noRed = checked},
                ],            
                [
                    { label: 'Ongoing', type: 'checkbox', value: alerts.noOngoing, onChange: (widget, checked) => widget.decorations.alerts.noOngoing = checked},
                    { label: 'Stopped', type: 'checkbox', value: alerts.noStopped, onChange: (widget, checked) => widget.decorations.alerts.noStopped = checked},
                ],
                [
                    { label: 'Acknowledged', type: 'checkbox', value: alerts.noAcknowledged, onChange: (widget, checked) => widget.decorations.alerts.noAcknowledged = checked},
                    { label: 'Unacknowledged', type: 'checkbox', value: alerts.noUnacknowledged, onChange: (widget, checked) => widget.decorations.alerts.noUnacknowledged = checked},
                ],
            ], description: 'Properties of alerts to show. Graphs hide stopped or acknowledged alerts automatically.' },
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
                        if (NAMESPACES[ns] === undefined) {
                            ns = 'other';
                        }
                        if (nsAdded.indexOf(ns) < 0) {
                            nsAdded.push(ns);
                            nsSelection.append($('<option/>').val(ns).text(NAMESPACES[ns]));
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
                    if (NAMESPACES[ns] === undefined) {
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
        let collapsed = $('#settings-page').children('tr:visible').length <= 1;
        return { id: 'settings-page', caption: 'Page', collapsed: collapsed, entries: [
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
        MonitoringConsole.Model.Page.Widgets.Selection.toggle(widget.id, true);
        updateDomOfWidget(undefined, widget);
        updateSettings();
    }

    function onWidgetDelete(widgetId) {
        if (window.confirm('Do you really want to remove the chart from the page?')) {
            onPageChange(MonitoringConsole.Model.Page.Widgets.remove(widgetId));
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

    function createLegendModel(widget, data, alerts, annotations) {
        if (!data)
            return [{ label: 'Connection Lost', value: '?', color: 'red', assessments: { status: 'error' } }];
        if (widget.type == 'alert')
            return createLegendModelFromAlerts(widget, alerts);
        if (widget.type == 'annotation')
            return createLegendModelFromAnnotations(widget, annotations);
        if (Array.isArray(data) && data.length == 0)
            return [{ label: 'No Data', value: '?', color: '#0096D6', assessments: {status: 'missing' }}];
        let legend = [];
        let format = Units.converter(widget.unit).format;
        let palette = Theme.palette();
        let alpha = Theme.option('opacity') / 100;
        for (let j = 0; j < data.length; j++) {
            let seriesData = data[j];
            let label = seriesData.instance;
            if (widget.series.includes('*') && !widget.series.includes('?')) {
                let tag = seriesData.series.replace(new RegExp(widget.series.replace('*', '(.*)')), '$1').replace('_', ' ');                
                label = widget.coloring == 'series' ? tag : [label, tag];
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
            let coloring = widget.coloring;
            if (coloring == 'series')
                coloring += ': ' + widget.series;
            let color = Colors.lookup(coloring, getColorKey(widget, seriesData.series, seriesData.instance, j), palette);
            let background = Colors.hex2rgba(color, alpha);
            if (Array.isArray(alerts) && alerts.length > 0) {
                let level;
                for (let i = 0; i < alerts.length; i++) {
                    let alert = alerts[i];
                    if (alert.instance == seriesData.instance && alert.series == seriesData.series && !alert.stopped) {
                        level = Units.Alerts.maxLevel(level, alert.level);
                    }
                }
                if (level == 'red' || level == 'amber') {
                    background = Colors.hex2rgba(Theme.color(level), Math.min(1, alpha * 2));
                }
            }
            let status = seriesData.assessments.status;
            let highlight = status === undefined ? undefined : Theme.color(status);
            let item = { 
                label: label, 
                value: value, 
                color: color,
                background: background,
                status: status,
                highlight: highlight,
            };
            legend.push(item);
            seriesData.legend = item;
        }
        return legend;
    }

    

    function createLegendModelFromAlerts(widget, alerts) {
        if (!Array.isArray(alerts))
            return []; //TODO use white, green, amber and red to describe the watch in case of single watch
        let palette = Theme.palette();
        let alpha = Theme.option('opacity') / 100;
        let instances = {};
        for (let alert of alerts) {
            instances[alert.instance] = Units.Alerts.maxLevel(alert.level, instances[alert.instance]);
        }
        
        return Object.entries(instances).map(function([instance, level]) {
            let color = Colors.lookup('instance', instance, palette);
            return {
                label: instance,
                value: Units.Alerts.name(level),
                color: color,
                background: Colors.hex2rgba(color, alpha),
                status: level, 
                highlight: Theme.color(level),                
            };
        });
    }

    function createLegendModelFromAnnotations(widget, annotations) {
        let coloring = widget.coloring || 'instance';
        if (!Array.isArray(annotations) || coloring === 'index')
            return [];
        let palette = Theme.palette();
        let entries = {};
        let index = 1;
        for (let annotation of annotations) {
            let series = annotation.series;
            let instance = annotation.instance;
            let label = coloring === 'series' ? [series] : coloring === 'instance' ? [instance] : [instance, series];
            let key = label.join('-');
            let entry = entries[key];
            if (entry === undefined) {
                let colorKey = getColorKey(widget, series, instance, index);
                entries[key] = { label: label, count: 1, color: Colors.lookup(coloring, colorKey, palette) };
            } else {
                entry.count += 1;
            }
            index++;
        }
        return Object.values(entries).map(function(entry) {
            return {
                label: entry.label,
                value: entry.count + 'x',
                color: entry.color,                
            };
        });
    }

    function getColorKey(widget, series, instance, index) {
        switch (widget.coloring) {
            case 'index': return 'line-' + index;
            case 'series': return series;
            case 'instance-series': return instance + ' ' + series;
            case 'instance': 
            default: return instance;
        }
    } 

    function createIndicatorModel(widget, data) {
        if (!data)
            return { status: 'error', color: Theme.color('error') };
        if (Array.isArray(data) && data.length == 0)
            return { status: 'missing', color: Theme.color('missing'), text: widget.status.missing.hint };
        let status = 'normal';
        for (let seriesData of data)
            status = Units.Alerts.maxLevel(status, seriesData.assessments.status);
        const infoKey = status == 'red' ? 'critical' : status == 'amber' ? 'alarming' : status;
        let statusInfo = widget.status[infoKey] || {};
        return { status: status, color: Theme.color(status), text: statusInfo.hint };
    }

    function createAlertTableModel(widget, alerts, annotations) {
        if (widget.type === 'annotation')
            return {};
        function createAlertAnnotationsFilter(alert) {
          return (annotation) => widget.options.noAnnotations !== true
                && annotation.series == alert.series 
                && annotation.instance == alert.instance
                && Math.round(annotation.time / 1000) >= Math.round(alert.since / 1000) // only same second needed
                && annotation.time <= (alert.until || new Date().getTime());  
        }
        let items = [];
        if (Array.isArray(alerts)) {
            let palette = Theme.palette();
            let fields = widget.fields;
            for (let i = 0; i < alerts.length; i++) {
                let alert = alerts[i];
                let autoInclude = widget.type === 'alert' || ((alert.level === 'red' || alert.level === 'amber') && !alert.acknowledged);
                let filters = widget.decorations.alerts;
                let lastAlertLevel = alert.frames[alert.frames.length - 1].level;
                if (lastAlertLevel == 'green' || lastAlertLevel == 'white')
                    lastAlertLevel = alert.frames[alert.frames.length - 2].level;
                let visible = (alert.acknowledged && filters.noAcknowledged !== true || !alert.acknowledged && filters.noUnacknowledged !== true)
                           && (alert.stopped && filters.noStopped !== true || !alert.stopped && filters.noOngoing !== true)
                           && (lastAlertLevel == 'red' && filters.noRed !== true || lastAlertLevel == 'amber' && filters.noAmber !== true);                  
                if (autoInclude && visible) {
                    let frames = alert.frames.map(function(frame) {
                        return {
                            level: frame.level,
                            since: frame.start,
                            until: frame.end,
                            color: Theme.color(frame.level),
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
                        annotations: annotations.filter(createAlertAnnotationsFilter(alert)).map(function(annotation) {
                            return {
                                time: annotation.time,
                                value: annotation.value,
                                attrs: annotation.attrs,
                                fields: fields,
                            };
                        }),
                    });
                }
            }
        }
        return { id: widget.target + '_alerts', verbose: widget.type === 'alert', items: items };
    }

    function createAnnotationTableModel(widget, annotations) {
        if (widget.type !== 'annotation')
            return {};
        let items = [];
        if (Array.isArray(annotations)) {
            let palette = Theme.palette();
            let index = 1;
            for (let annotation of annotations) {
                let colorKey = getColorKey(widget, annotation.series, annotation.instance, index);
                items.push({
                    color: Colors.lookup(widget.coloring, colorKey, palette),
                    series: annotation.series,
                    instance: annotation.instance,
                    unit: widget.unit,
                    time: annotation.time,
                    value: annotation.value,
                    attrs: annotation.attrs,
                    fields: widget.fields,
                });
                index++;
            }
        }
        return { id: widget.target + '_annotations', mode: widget.mode, sort: widget.sort, items: items };
    }

    /**
     * This function is called when the watch details settings should be opened
     */
    function onOpenWatchSettings() {
        function wrapOnSuccess(onSuccess) {
            return () => {
                if (typeof onSuccess === 'function')
                    onSuccess();
                onOpenWatchSettings();
            };
        }
        Controller.requestListOfWatches((watches) => {
            const manager = { 
                id: 'WatchManager', 
                items: watches, 
                colors: { red: Theme.color('red'), amber: Theme.color('amber'), green: Theme.color('green') },
                actions: { 
                    onCreate: (watch, onSuccess, onFailure) => Controller.requestCreateWatch(watch, wrapOnSuccess(onSuccess), onFailure),
                    onDelete: (name, onSuccess, onFailure) => Controller.requestDeleteWatch(name, wrapOnSuccess(onSuccess), onFailure),
                    onDisable: (name, onSuccess, onFailure) => Controller.requestDisableWatch(name, wrapOnSuccess(onSuccess), onFailure),
                    onEnable: (name, onSuccess, onFailure) => Controller.requestEnableWatch(name, wrapOnSuccess(onSuccess), onFailure),
                },
            };
            updatePageNavigation('Watches');
            $('#chart-grid').hide();
            $('#WatchManager').replaceWith(Components.createWatchManager(manager));
        });
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
        let annotations = update.annotations;
        updateDomOfWidget(undefined, widget);
        let widgetNode = $('#widget-' + widget.target);
        let legendNode = widgetNode.find('.Legend').first();
        let indicatorNode = widgetNode.find('.Indicator').first();
        let alertsNode = widgetNode.find('.AlertTable').first();
        let annotationsNode = widgetNode.find('.AnnotationTable').first();
        let legend = createLegendModel(widget, data, alerts, annotations); // OBS this has side effect of setting .legend attribute in series data
        if (data !== undefined && (widget.type === 'line' || widget.type === 'bar')) {
            MonitoringConsole.Chart.getAPI(widget).onDataUpdate(update);
        }
        alertsNode.replaceWith(Components.createAlertTable(createAlertTableModel(widget, alerts, annotations)));
        legendNode.replaceWith(Components.createLegend(legend));
        indicatorNode.replaceWith(Components.createIndicator(createIndicatorModel(widget, data)));
        annotationsNode.replaceWith(Components.createAnnotationTable(createAnnotationTableModel(widget, annotations)));
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
                    let rowspan = cell.rowspan;
                    let height = (rowspan * rowHeight);
                    let td = $("<td/>", { id: 'widget-'+cell.widget.target, colspan: cell.colspan, rowspan: rowspan, 'class': 'widget', style: 'height: '+height+"px;"});
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
        $('#WatchManager').hide();
        $('#chart-grid').show();
        onPageUpdate(layout);
        updatePageNavigation();
        updateSettings();
        updateMenu();
    }

    function onOpenWidgetSettings(widgetId) {
        MonitoringConsole.Model.Page.Widgets.Selection.clear();
        MonitoringConsole.Model.Page.Widgets.Selection.toggle(widgetId);
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
        onOpenWidgetSettings: (widgetId) => onOpenWidgetSettings(widgetId),
    };
})();
