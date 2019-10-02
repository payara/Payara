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

    const Settings = MonitoringConsole.View.Widgets.Settings;

    /**
     * Updates the DOM with the page navigation tabs so it reflects current model state
     */ 
    function updatePageNavigation() {
        let nav = $("#panel-nav"); 
        nav.empty();
        let dropdown = $('<span/>', {'class': 'nav-page-dropdown'});
        MonitoringConsole.Model.listPages().forEach(function(page) {            
            if (page.active) {
                nav.append($('<h2/>', {text: page.name})
                    .click(() => dropdown.toggle()));
            } else {
                dropdown.append($('<span/>', {text: page.name})
                    .click(() => onPageChange(MonitoringConsole.Model.Page.changeTo(page.id))));                
            }
        });
        nav.append(dropdown);
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
            let panelSettings = Settings.emptyPanel()
                .append(createPageSettings())
                .append(createDataSettings());
            if (MonitoringConsole.Model.Page.Widgets.Selection.isSingle()) {
                panelSettings.append(createWidgetSettings(MonitoringConsole.Model.Page.Widgets.Selection.first()));
            }
        } else {
            panelConsole.removeClass('state-show-settings');
        }
    }

    function createWidgetLegend(widget) {
        return $('<ol/>',  {'class': 'widget-legend-bar'});
    }

    function createWidgetLegendItem(data, color) {
        let value = data.points[data.points.length-1];
        return $('<li/>', {style: 'border-color: '+color+';'}).append($('<span/>').text(data.instance)).append($('<span/>').text(value));
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
                parent.append(createWidgetLegend(widget));                
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
        if (widget.options.perSec) {
            metricText += ' <i>(1/sec)</i>';
        }
        let grouped = false;
        for (let i = 0; i < tags.length; i++) {
            let tag = tags[i];
            if (tag.startsWith('@:')) {
                grouped = true;
                text += metricText;
                text += ': <code>'+tag.substring(2)+'</code> ';
            } else {
                text +=' <i>'+tag+'</i> ';
            }
        }
        if (!grouped)
            text += metricText;
        return text;
    }

    function createWidgetToolbar(widget, expanded) {
        let series = widget.series;
        let settings = $('<span/>', {'class': 'widget-settings-bar', style: expanded ? 'display: inline;' : ''})
            .append(createToolbarButton('Remove chart from page', '&times; Remove', () => onWidgetDelete(series)))
            .append(createToolbarButton('Enlarge this chart', '&ltri;&rtri; Larger', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanMore(series))))
            .append(createToolbarButton('Shrink this chart', '&rtri;&ltri; Smaller', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanLess(series))))
            .append(createToolbarButton('Move to right', '&rtri; Move Right', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveRight(series))))
            .append(createToolbarButton('Move to left', '&ltri; Move Left', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveLeft(series))))
            .append(createToolbarButton('Move up', '&triangle; Move Up', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveUp(series))))
            .append(createToolbarButton('Move down', '&dtri; Move Down', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveDown(series))))
            .append(createToolbarButton('Open in Side Panel', '&#9881; More...', () => onOpenWidgetSettings(series)))
            ;
        return $('<div/>', {"class": "widget-title-bar"})
            .append($('<h3/>', {title: 'Select '+series}).html(formatSeriesName(widget))
                .click(() => onWidgetToolbarClick(widget)))
            .append(createToolbarButton('Settings', '&#9881;', () => $(settings).toggle())
            .append(settings));
    }

    function createToolbarButton(title, icon, onClick) {
        return $('<button/>', { "class": "btnIcon", title: title}).html(icon).click(onClick);
    }


    function createWidgetSettings(widget) {
        let settings = Settings.createTable('settings-widget')
            .append(Settings.createHeaderRow(formatSeriesName(widget)))
            .append(Settings.createHeaderRow('General'))
            .append(Settings.createDropdownRow('Type', {line: 'Time Curve', bar: 'Range Indicator'}, widget.type, (widget, selected) => widget.type = selected))
            .append(Settings.createSliderRow('Span', 1, 4, widget.grid.span || 1, (widget, value) => widget.grid.span = value))
            .append(Settings.createSliderRow('Column', 1, 4, 1 + (widget.grid.column || 0), (widget, value) => widget.grid.column = value - 1))
            .append(Settings.createSliderRow('Item', 1, 4, 1 + (widget.grid.item || 0), (widget, value) => widget.grid.item = value - 1))
            .append(Settings.createHeaderRow('Data'))
            .append(Settings.createCheckboxRow('Add Minimum', widget.options.drawMinLine, (widget, checked) => widget.options.drawMinLine = checked))
            .append(Settings.createCheckboxRow('Add Maximum', widget.options.drawMaxLine, (widget, checked) => widget.options.drawMaxLine = checked))            
            ;
        if (widget.type === 'line') {
            settings
            .append(Settings.createCheckboxRow('Add Average', widget.options.drawAvgLine, (widget, checked) => widget.options.drawAvgLine = checked))
            .append(Settings.createCheckboxRow('Per Second', widget.options.perSec, (widget, checked) => widget.options.perSec = checked))
            .append(Settings.createHeaderRow('Display Options'))
            .append(Settings.createCheckboxRow('Begin at Zero', widget.options.beginAtZero, (widget, checked) => widget.options.beginAtZero = checked))
            .append(Settings.createCheckboxRow('Automatic Labels', widget.options.autoTimeTicks, (widget, checked) => widget.options.autoTimeTicks = checked))
            .append(Settings.createCheckboxRow('Use Bezier Curves', widget.options.drawCurves, (widget, checked) => widget.options.drawCurves = checked))
            .append(Settings.createCheckboxRow('Use Animations', widget.options.drawAnimations, (widget, checked) => widget.options.drawAnimations = checked))
            .append(Settings.createCheckboxRow('Label X-Axis at 90°', widget.options.rotateTimeLabels, (widget, checked) => widget.options.rotateTimeLabels = checked))
            .append(Settings.createCheckboxRow('Show Points', widget.options.drawPoints, (widget, checked) => widget.options.drawPoints = checked))
            .append(Settings.createCheckboxRow('Show Fill', widget.options.drawFill, (widget, checked) => widget.options.drawFill = checked))
            .append(Settings.createCheckboxRow('Show Stabe', widget.options.drawStableLine, (widget, checked) => widget.options.drawStableLine = checked))
            .append(Settings.createCheckboxRow('Show Legend', widget.options.showLegend, (widget, checked) => widget.options.showLegend = checked))
            .append(Settings.createCheckboxRow('Show Time Labels', widget.options.showTimeLabels, (widget, checked) => widget.options.showTimeLabels = checked))
            ;            
        }
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
        return Settings.createTable('settings-page')
            .append(Settings.createHeaderRow('Page'))
            .append(Settings.createRow('Name', () => $('<input/>', { type: 'text', value: MonitoringConsole.Model.Page.name() })
                .on("propertychange change keyup paste input", function() {
                    if (MonitoringConsole.Model.Page.rename(this.value)) {
                        updatePageNavigation();                        
                    }
                })))
            .append(Settings.createRow('Widgets', () => $('<span/>')
                .append(widgetsSelection)
                .append(widgetSeries)
                .append($('<button/>', {title: 'Add selected metric', text: 'Add'})
                    .click(() => onPageUpdate(MonitoringConsole.Model.Page.Widgets.add(widgetSeries.val()))))
                ));
    }

    function createDataSettings() {
        let instanceSelection = $('<select />', {multiple: true});
        $.getJSON("api/instances/", function(instances) {
            for (let i = 0; i < instances.length; i++) {
                instanceSelection.append($('<option/>', { value: instances[i], text: instances[i], selected:true}));
            }
        });
        return Settings.createTable('settings-data')
            .append(Settings.createHeaderRow('Data'))
            .append(Settings.createRow('Instances', () => instanceSelection));
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

    /**
     * This function is called when data was received or was failed to receive so the new data can be applied to the page.
     *
     * Depending on the update different content is rendered within a chart box.
     */
    function onDataUpdate(update) {
        let widget = update.widget;
        updateDomOfWidget(undefined, widget);
        let widgetNode = $('#widget-'+widget.target);
        let legendNode = widgetNode.find('.widget-legend-bar').first();
        if (update.data) {
            MonitoringConsole.Chart.getAPI(widget).onDataUpdate(update);
            legendNode.empty();
            for (let j = 0; j < update.data.length; j++) {
                legendNode.append(createWidgetLegendItem(update.data[j], MonitoringConsole.Chart.Common.lineColor(j)));
            }
        } else {
            //TODO
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
        onPageUpdate(layout);
        updatePageNavigation();
        updatePageAndSelectionSettings();
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
        onOpenWidgetSettings: (series) => onOpenWidgetSettings(series),
    };
})();
