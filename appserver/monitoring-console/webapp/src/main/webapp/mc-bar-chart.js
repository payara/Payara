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

   const DEFAULT_BG_COLORS = [
    'rgba(153, 102, 255, 0.2)',
    'rgba(255, 99, 132, 0.2)',
    'rgba(54, 162, 235, 0.2)',
    'rgba(255, 206, 86, 0.2)',
    'rgba(75, 192, 192, 0.2)',
    'rgba(255, 159, 64, 0.2)'
  ];
  const DEFAULT_LINE_COLORS = [
    'rgba(153, 102, 255, 1)',
    'rgba(255, 99, 132, 1)',
    'rgba(54, 162, 235, 1)',
    'rgba(255, 206, 86, 1)',
    'rgba(75, 192, 192, 1)',
    'rgba(255, 159, 64, 1)'
  ];

   function createData(widget, response) {
      let labels = [];
      let series = [];
      let zeroToMinValues = [];
      let observedMinToMinValues = [];
      let minToMaxValues = [];
      let maxToObservedMaxValues = [];
      let showObservedMin = widget.options.drawMinLine;
      let startOfLastMinute = Date.now() - 60000;
      for (let i = 0; i < response.length; i++) {
         let seriesResponse = response[i];
         if (seriesResponse.observedValues > 0) {
            let points = seriesResponse.points;
            let min;
            let max;
            let count = 0;
            for (let j = 0; j < points.length; j+=2) {
               if (points[j] > startOfLastMinute) {
                  let value = points[j+1];
                  count++;
                  min = min === undefined ? value : Math.min(min, value);
                  max = max === undefined ? value : Math.max(max, value);
               }
            }
            if (min && max) {
               series.push(seriesResponse.series);
               let label = widget.series.indexOf('*') < 0 ? '' : seriesResponse.series.replace(new RegExp(widget.series.replace('*', '(.*)')), '$1').replace('_', ' ');
               label += '    x ' + count;
               labels.push(label);               
               zeroToMinValues.push(showObservedMin ? seriesResponse.observedMin : min);
               observedMinToMinValues.push(min - seriesResponse.observedMin);
               minToMaxValues.push(max - min);
               maxToObservedMaxValues.push(seriesResponse.observedMax - max);
            }
         }
      }
      let datasets = [];
      let offset = {
        data: zeroToMinValues,
        backgroundColor: 'transparent',
      };
      datasets.push(offset);  
      if (showObservedMin) {
         datasets.push({
            data: observedMinToMinValues,
            backgroundColor: DEFAULT_BG_COLORS,
            borderColor: DEFAULT_LINE_COLORS,
            borderWidth: { right: 2 },
         });       
      }
      offset.borderColor = DEFAULT_LINE_COLORS;
      offset.borderWidth = { right: 2 };  
      datasets.push({
         data: minToMaxValues,
         backgroundColor: DEFAULT_BG_COLORS,
         borderColor: DEFAULT_LINE_COLORS,
         borderWidth: { right: 2 },
      });
      if (widget.options.drawMaxLine) {
         datasets.push({
           data: maxToObservedMaxValues,
           backgroundColor: DEFAULT_BG_COLORS,
         }); 
      }
      return {
         series: series,
         labels: labels,
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
               }],
               yAxes: [{
                  maxBarThickness: 50, //px
                  barPercentage: 1.0,
                  categoryPercentage: 1.0,
                  borderSkipped: false,
                  stacked: true,
                  ticks: {
                     mirror: true,
                     padding: -10,
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