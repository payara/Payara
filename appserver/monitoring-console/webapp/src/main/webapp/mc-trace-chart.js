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

   const DEFAULT_BG_COLORS = 'rgba(153, 102, 255, 0.2)';
   const DEFAULT_LINE_COLORS = 'rgba(153, 102, 255, 1)';

   const Settings = MonitoringConsole.View.Widgets.Settings;
   const util = MonitoringConsole.Chart.Common;

   var model = {};
   var chart;

   function onDataUpdate(data) {
      let zeroToMinValues = [];
      let minToMaxValues = [];
      let spans = [];
      let labels = [];
      for (let i = 0; i < data.length; i++) {
         let trace = data[i]; 
         let startTime = trace.startTime;
         for (let j = 0; j < trace.spans.length; j++) {
            let span = trace.spans[j];
            spans.push(span);
            zeroToMinValues.push(span.startTime - startTime);
            minToMaxValues.push(span.endTime - span.startTime);
            labels.push(span.operation);
         }        
      }
      let datasets = [ {
            data: zeroToMinValues,
            backgroundColor: 'transparent',
         }, {
            data: minToMaxValues,
            backgroundColor: DEFAULT_BG_COLORS,
            borderColor: DEFAULT_LINE_COLORS,
            borderWidth: { right: 2 },
         }
      ];
      if (!chart) {
         chart = onCreation();
      }
      $('#trace-chart').height(labels.length * 15 + 30);
      chart.data = { 
         datasets: datasets,
         labels: labels,
         spans: spans,
      };
      chart.options.onClick = function(event)  {
        updateDomSpanDetails(data, spans[this.getElementsAtEventForMode(event, "y", 1)[0]._index]); 
      };
      chart.update(0);
   }

   function autoLink(text) {
      if (text.startsWith('http://') || text.startsWith('https://'))
         return $('<a/>', { href: text, text: text});
      return $(document.createTextNode(text));
   }

   function addCustomTooltip(chart) {
      chart.options.tooltips.custom = util.createCustomTooltipFunction(function(dataPoints) {
         let index = dataPoints[0].index;
         let span = spans[index];
         let body = $('<div/>');
         //...
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
               }],
               yAxes: [{
                  maxBarThickness: 15, //px
                  barThickness: 15, //px
                  barPercentage: 1.0,
                  categoryPercentage: 1.0,
                  borderSkipped: false,
                  stacked: true,
                  gridLines: {
                     display:false
                  }
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
      let settingsPanel = Settings.emptyPanel();
      settingsPanel.append(Settings.createTable('settings-span', 'Span')
         .append(Settings.createRow('ID', span.id))
         .append(Settings.createRow('Operation', span.operation))
         .append(Settings.createRow('Start', util.formatDate(new Date(span.startTime))))
         .append(Settings.createRow('End', util.formatDate(new Date(span.endTime))))
         .append(Settings.createRow('Duration', (span.duration / 1000000) + 'ms'))
         );

      let tags = Settings.createTable('settings-tags', 'Tags'); 
      for (let [key, value] of Object.entries(span.tags)) {
         if (value.startsWith('[') && value.endsWith(']')) {
            value = value.slice(1,-1);
         }
         tags.append(Settings.createRow(key, autoLink(value)));
      }
      settingsPanel.append(tags);
   }


   function onDataRefresh() {
      $.getJSON("api/trace/data/"+model.series, onDataUpdate);
   }

   function onOpenPopup(series) {
      $('#panel-trace').show();
      model.series = series;
      onDataRefresh();
   }

   function onClosePopup() {
      if (chart) {
         chart.destroy();
         chart = undefined;
      }
      $('#panel-trace').hide();
   }

   /**
    * Public API below:
    */
   return {
      onOpenPopup: (series) => onOpenPopup(series),
      onClosePopup: () => onClosePopup(),
      onDataRefresh: () => onDataRefresh(),
   };
})();