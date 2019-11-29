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
        mc: 'Monitoring Console Internals',
        other: 'Other',
    };

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
        $('#Menu').replaceWith(Components.onMenuCreation(menu));
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
            let settings = [];
            settings.push(createGlobalSettings());
            settings.push(createPageSettings());
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
            .append(Components.onMenuCreation(menu));
    }

    function createGlobalSettings() {
        return { id: 'settings-global', caption: 'Global', entries: [
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

    function createWidgetSettings(widget) {
        let options = widget.options;
        let unit = widget.unit;
        let thresholds = widget.decorations.thresholds;
        let settings = [];
        settings.push({ id: 'settings-widget', caption: 'Widget', entries: [
            { label: 'Display Name', type: 'text', value: widget.displayName, onChange: (widget, value) => widget.displayName = value},
            { label: 'Type', type: 'dropdown', options: {line: 'Time Curve', bar: 'Range Indicator'}, value: widget.type, onChange: (widget, selected) => widget.type = selected},
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
                { type: 'color', value: widget.decorations.waterline.color, defaultValue: '#00ffff', onChange: (widget, value) => widget.decorations.waterline.color = value },
            ]},
            { label: 'Alarming Threshold', input: [
                { type: 'value', unit: unit, value: thresholds.alarming.value, onChange: (widget, value) => thresholds.alarming.value = value },
                { type: 'color', value: thresholds.alarming.color, defaultValue: '#FFD700', onChange: (widget, value) => thresholds.alarming.color = value },
                { label: 'Line', type: 'checkbox', value: thresholds.alarming.display, onChange: (widget, checked) => thresholds.alarming.display = checked },
            ]},
            { label: 'Critical Threshold', input: [
                { type: 'value', unit: unit, value: thresholds.critical.value, onChange: (widget, value) => thresholds.critical.value = value },
                { type: 'color', value: thresholds.critical.color, defaultValue: '#dc143c', onChange: (widget, value) => thresholds.critical.color = value },
                { label: 'Line', type: 'checkbox', value: thresholds.critical.display, onChange: (widget, checked) => thresholds.critical.display = checked },
            ]},                
            { label: 'Threshold Reference', type: 'dropdown', options: { off: 'Off', now: 'Most Recent Value', min: 'Minimum Value', max: 'Maximum Value', avg: 'Average Value'}, value: thresholds.reference, onChange: (widget, selected) => thresholds.reference = selected},
            //TODO add color for each threshold
        ]});
        settings.push({ id: 'settings-status', caption: 'Status', description: 'Set a text for an assessment status', entries: [
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
            let value = format(avg, widget.unit === 'bytes' || widget.unit === 'ns');
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
        Components: Components,
        onPageReady: function() {
            // connect the view to the model by passing the 'onDataUpdate' function to the model
            // which will call it when data is received
            onPageChange(MonitoringConsole.Model.init(onDataUpdate, onPageChange));
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
