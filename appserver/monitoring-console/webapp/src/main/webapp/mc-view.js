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
            .append($('<button/>', { "class": "btnIcon btnClose", title:'Remove chart from page' }).html('&times;')
                .click(onWidgetDelete))
            .append($('<button/>', { "class": "btnIcon", title:'Enlarge this chart' }).html('&plus;')
                .click(() => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanMore(series))))
            .append($('<button/>', { "class": "btnIcon", title:'Shrink this chart' }).html('&minus;')
                .click(() => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanLess(series))))
            .append($('<button/>', { "class": "btnIcon", title:'Move to right' }).html('&#9655;')
                .click(() => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveRight(series))))
            .append($('<button/>', { "class": "btnIcon", title:'Move to left'}).html('&#9665;')
                .click(() => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveLeft(series))));
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
        return $('<tr/>').append($('<th/>', {colspan: 2, text: caption}));
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
                    tr.append($("<td/>"));                  
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
