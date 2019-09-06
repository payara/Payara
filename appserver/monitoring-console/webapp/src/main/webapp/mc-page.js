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

var MonitoringConsolePage = (function() {

    function download(filename, text) {
        var pom = document.createElement('a');
        pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
        pom.setAttribute('download', filename);

        if (document.createEvent) {
            var event = document.createEvent('MouseEvents');
            event.initEvent('click', true, true);
            pom.dispatchEvent(event);
        }
        else {
            pom.click();
        }
    }

    function renderPageTabs() {
        var bar = $("#pagesTabs"); 
        bar.empty();
        MonitoringConsole.listPages().forEach(function(page) {
            var tabId = page.id + '-tab';
            var css = "page-tab" + (page.active ? ' page-selected' : '');
            //TODO when clicking active page tab the options could open/close
            var newTab = $('<span/>', {id: tabId, "class": css, text: page.name});
            newTab.click(function() {
                onPageChange(MonitoringConsole.Page.changeTo(page.id));
            });
            bar.append(newTab);
        });
        var addPage = $('<span/>', {id: 'addPageTab', 'class': 'page-tab'}).html('&plus;');
        addPage.click(function() {
            onPageChange(MonitoringConsole.Page.create('(Unnamed)'));
        });
        bar.append(addPage);
    }

    /**
     * Each chart needs to be in a relative positioned box to allow responsive sizing.
     * This fuction creates this box including the canvas element the chart is drawn upon.
     */
    function renderChartBox(cell) {
        var boxId = cell.widget.target + '-box';
        var box = $('#'+boxId);
        if (box.length > 0)
            return box.first();
        box = $('<div/>', { id: boxId, "class": "chart-box" });
        var win = $(window);
        box.append($('<canvas/>',{ id: cell.widget.target }));
        return box;
    }

    /**
     * This function refleshes the page with the given layout.
     */
    function renderPage(layout) {
        var table = $("<table/>", { id: 'chart-grid'});
        var numberOfColumns = layout.length;
        var maxRow = 0;
        for (var col = 0; col < numberOfColumns; col++) {
            maxRow = Math.max(maxRow, layout[col].length);
        }
        var rowHeight = Math.round(($(window).height() - 100) / numberOfColumns);
        for (var row = 0; row < maxRow; row++) {
            var tr = $("<tr/>");
            for (var col = 0; col < numberOfColumns; col++) {
                if (layout[col].length <= row) {
                    tr.append($("<td/>"));
                } else {
                    var cell = layout[col][row];
                    if (cell) {
                        var span = cell.span;
                        var td = $("<td/>", { colspan: span, rowspan: span, 'class': 'box', style: 'height: '+(span * rowHeight)+"px;"});
                        td.append(renderChartCaption(cell));
                        var status = $('<div/>', { "class": 'status-nodata'});
                        status.append($('<div/>', {text: 'No Data'}));
                        td.append(status);
                        td.append(renderChartBox(cell));
                        tr.append(td);
                    } else if (row == 0 && layout[col].length == 0) {
                        // a column with no content, span all rows
                        var td = $("<td/>", { rowspan: maxRow });
                        tr.append(td);                  
                    }
                }
            }
            table.append(tr);
        }
        $('#chart-container').empty();
        $('#chart-container').append(table);
    }

    function camelCaseToWords(str) {
        return str.replace(/([A-Z]+)/g, " $1").replace(/([A-Z][a-z])/g, " $1");
    }

    function renderChartCaption(cell) {
        var bar = $('<div/>', {"class": "caption-bar"});
        var series = cell.widget.series;
        var endOfTags = series.lastIndexOf(' ');
        var text = endOfTags <= 0 
           ? camelCaseToWords(series) 
           : '<i>'+series.substring(0, endOfTags)+'</i> '+camelCaseToWords(series.substring(endOfTags + 1));
        if (cell.widget.options.perSec) {
            text += ' <i>(per second)</i>';
        }
        var caption = $('<h3/>', {title: 'Select '+series}).html(text);
        caption.click(function() {
            if (MonitoringConsole.Page.Widgets.Selection.toggle(series)) {
                bar.parent().addClass('chart-selected');
            } else {
                bar.parent().removeClass('chart-selected');
            }
            renderChartOptions();
        });
        bar.append(caption);
        var btnClose = $('<button/>', { "class": "btnIcon btnClose", title:'Remove chart from page' }).html('&times;');
        btnClose.click(function() {
            if (window.confirm('Do you really want to remove the chart from the page?')) {
                renderPage(MonitoringConsole.Page.Widgets.remove(series));
            }
        });
        bar.append(btnClose);
        var btnLarger = $('<button/>', { "class": "btnIcon", title:'Enlarge this chart' }).html('&plus;');
        btnLarger.click(function() {
            renderPage(MonitoringConsole.Page.Widgets.configure(series, function(widget) {
                if (widget.grid.span < 4) {
                    widget.grid.span = !widget.grid.span ? 1 : widget.grid.span + 1;
                }
            }));
        });
        bar.append(btnLarger);
        var btnSmaller = $('<button/>', { "class": "btnIcon", title:'Shrink this chart' }).html('&minus;');
        btnSmaller.click(function() {
            renderPage(MonitoringConsole.Page.Widgets.configure(series, function(widget) {
                if (!widget.grid.span || widget.grid.span > 1) {
                    widget.grid.span = !widget.grid.span ? 1 : widget.grid.span - 1;
                }
            }));
        });
        bar.append(btnSmaller);
        var btnRight = $('<button/>', { "class": "btnIcon", title:'Move to right' }).html('&#9655;');
        btnRight.click(function() {
            renderPage(MonitoringConsole.Page.Widgets.configure(series, function(widget) {
                if (!widget.grid.column || widget.grid.column < 4) {
                    widget.grid.item = undefined;
                    widget.grid.column = widget.grid.column ? widget.grid.column + 1 : 1;
                }
            }));
        });
        bar.append(btnRight);
        var btnLeft = $('<button/>', { "class": "btnIcon", title:'Move to left'}).html('&#9665;');
        btnLeft.click(function() {
            renderPage(MonitoringConsole.Page.Widgets.configure(series, function(widget) {
                if (!widget.grid.column || widget.grid.column > 0) {
                    widget.grid.item = undefined;
                    widget.grid.column = widget.grid.column ? widget.grid.column - 1 : 1;
                }
            }));
        });
        bar.append(btnLeft);
        return bar;
    }

    function renderChartOptions() {
        var panelConsole = $('#console');
        if (MonitoringConsole.Settings.isDispayed()) {
            if (!panelConsole.hasClass('state-show-properties')) {
                panelConsole.addClass('state-show-properties');
                var instancePanel = $("#panelCfgInstances");
                instancePanel.empty();
                $.getJSON("api/instances/", function(instances) {
                    var select = $('<select />', {id: 'cfgInstances', multiple: true});
                    for (var i = 0; i < instances.length; i++) {
                        select.append($('<option/>', { value: instances[i], text: instances[i], selected:true}))
                    }
                    instancePanel.append(select);
                });
                $('#cftPageName').val(MonitoringConsole.Page.name());
            }
        } else {
            panelConsole.removeClass('state-show-properties');
        }
    }

    /**
     * This function is called when data was received or was failed to receive so the new data can be applied to the page.
     *
     * Depending on the update different content is rendered within a chart box.
     */
    function onDataUpdate(update) {
        var boxId = update.widget.target + '-box';
        var box = $('#'+boxId);
        if (box.length == 0) {
            if (console)
                console.log('WARN: Box for chart ' + update.widget.series + ' not ready.');
            return;
        }
        var td = box.closest('td');
        if (update.data) {
            td.children('.status-nodata').hide();
            var points = update.data[0].points;
            if (points.length == 4 && points[1] === points[3] && !update.widget.options.perSec) {
                if (td.children('.stable').length == 0) {
                    var info = $('<div/>', { 'class': 'stable' });
                    info.append($('<span/>', { text: points[1] }));
                    td.append(info);
                    box.hide();
                }
            } else {
                td.children('.stable').remove();
                box.show();
                MonitoringConsoleRender.chart(update);
            }
        } else {
            td.children('.status-nodata').width(box.width()-10).height(box.height()-10).show();
        }
        
        if (update.widget.selected) {
            td.addClass('chart-selected');
        } else {
            td.removeClass('chart-selected');
        }
        renderChartOptions();
    }

    /**
     * Method to call when page changes to update UI elements accordingly
     */
    function onPageChange(layout) {
        renderPage(layout);
        renderPageTabs();
        renderChartOptions();
    }

    function init() {
        $('#btnExport').click(function() {
            download('monitoring-console-config.json', MonitoringConsole.$export());
        });
        $('#btnImport').click(function() {
            $('#cfgImport').click();
        });

        $("#cfgPerSec").on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.perSec = checked;
            });
        });

        $("#cfgStartAtZero").on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.beginAtZero = checked;
            });
        });

        $("#cfgAutoTimeTicks").on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.autoTimeTicks = checked;
            });
        });
        $("#cfgDrawCurves").on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.drawCurves = checked;
            });
        });
        $('#cfgDrawAnimations').on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.drawAnimations = checked;
            });
        });
        $("#cfgRotateTimeLabels").on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.rotateTimeLabels = checked;
            });
        });
        $('#cfgDrawAvgLine').on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.drawAvgLine = checked;
            });
        });
        $('#cfgDrawMinLine').on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.drawMinLine = checked;
            });
        });
        $('#cfgDrawMaxLine').on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.drawMaxLine = checked;
            });
        });
        $('#cfgShowLegend').on('change', function() {
            var checked = this.checked;
            MonitoringConsole.Page.Widgets.Selection.configure(function(widget) {
                widget.options.showLegend = checked;
            });
        });
        $('#btnRemovePage').click(function() {
            if (window.confirm("Do you really want to delete the current page?")) { 
                renderPage(MonitoringConsole.Page.erase());
                renderPageTabs();
            }
        });
        $('#btnMenu').click(function() {
            MonitoringConsole.Settings.toggle();
            renderChartOptions();
        });
        $('#cftPageName').on("propertychange change keyup paste input", function() {
            MonitoringConsole.Page.rename(this.value);
            renderPageTabs();
        });
        $('#btnAddChart').click(function() {
            $("#options option:selected").each(function() {
                var series = this.value;
                renderPage(MonitoringConsole.Page.Widgets.add(series));
            });
        });

        MonitoringConsole.listSeries(function(names) {
            var lastNs;
            var options = $("#options");
            $.each(names, function() {
                var key = this; //.replace(/ /g, ',');
                var ns =  this.substring(3, this.indexOf(' '));
                var $option = $("<option />").val(key).text(this.substring(this.indexOf(' ')));
                if (ns == lastNs) {
                    options.find('optgroup').last().append($option);
                } else {
                    var group = $('<optgroup/>').attr('label', ns);
                    group.append($option);
                    options.append(group);
                }
                lastNs = ns;
            });
        });
    }

//TODO add helper functions that based on a model for the properties build the property settings

    return {
        onPageReady: function() {
            init();
            renderPage(MonitoringConsole.init(onDataUpdate));
            renderPageTabs();
        },
        onPageChange: (layout) => onPageChange(layout),
        onPageUpdate: (layout) => renderPage(layout),
    };
})();
