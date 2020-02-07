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
                  gridLines: {
                    color: 'rgba(0, 127, 255,0.3)',
                    lineWidth: 0.5,
                  },                  
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
                  },
                  gridLines: {
                    color: 'rgba(0, 127, 255,0.7)',
                    lineWidth: 0.5,
                  },
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