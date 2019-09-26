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
	View: undefined,
   /**
    * Functions of manipulate the model of the MC (often returns a layout that is applied to the View)
    **/ 
	Model: undefined,
   /**
    * Functions specifically to take the data and prepare the display particular chart type using the underlying charting library.
    *
    * Each of the type objects below shares the same public API that is used by Model and View to apply the model to the chart to update the view properly.
    **/
	Chart: {
   /**
    * Line chart adapter API for monitoring series data
    **/
    Line: undefined,
   /**
    * Bar chart adapter API for monitoring series data
    **/
    Bar: undefined,

    /**
     * Trace 'gantt chart' like API, this is not a strict adapter API as the other two as the data to populate this is specific to traces
     */
    Trace: undefined,

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
 * Adapter to line charts of Chart.js
 */ 
MonitoringConsole.Chart.Line = (function() {
	
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

  /**
   * This is like a constant but it needs to yield new objects for each chart.
   */
  function onCreation(widget) {
    let options = {
      scales: {
        xAxes: [{
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
            callback: getTimeLabel,
            minRotation: 90,
            maxRotation: 90,
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
          labels: {
              filter: function(item, chart) {
                return !item.text.startsWith(" ");
              }
          }
      }
    };
    return new Chart(widget.target, {
          type: 'line',
          data: { datasets: [], },
          options: options,
        });
  } 

  function getTimeLabel(value, index, values) {
    if (values.length == 0 || index == 0)
      return value;
    let span = values[values.length -1].value - values[0].value;
    if (span < 120000) { // less then two minutes
      let lastMinute = new Date(values[index-1].value).getMinutes();
      return new Date(values[index].value).getMinutes() != lastMinute ? value : ''+new Date(values[index].value).getSeconds();
    }
    return value;
  }

  /**
   * Convertes a array of points given as one dimensional array with alternativ time value elements 
   * to a 2-dimensional array of points with t and y attribute.
   */
  function points1Dto2D(points1d, lastMinute) {
    if (!points1d)
      return [];
    if (lastMinute) {
      let points2d = [];
      let startOfLastMinute = Date.now() - 60000;
      for (let i = 0; i < points1d.length; i+=2) {
        if (points1d[i] > startOfLastMinute)
          points2d.push({ t: new Date(points1d[i]), y: points1d[i+1] });
      }
      return points2d;
    } else {
      let points2d = new Array(points1d.length / 2);
      for (let i = 0; i < points2d.length; i++) {
        points2d[i] = { t: new Date(points1d[i*2]), y: points1d[i*2+1] };
      }
      return points2d;      
    }
  }
    
  /**
   * Convertes a array of points given as one dimensional array with alternativ time value elements 
   * to a 2-dimensional array of points with t and y attribute where y reflects the delta between 
   * nearest 2 points of the input array. This means the result array has one less point as the input.
   */
  function points1Dto2DPerSec(points1d) {
    if (!points1d)
      return [];
    let points2d = new Array((points1d.length / 2) - 1);
    for (let i = 0; i < points2d.length; i++) {
      let t0 = points1d[i*2];
      let t1 = points1d[i*2+2];
      let y0 = points1d[i*2+1];
      let y1 = points1d[i*2+3];
      let dt = t1 - t0;
      let dy = y1 - y0;
      let y = (dt / 1000) * dy;
      points2d[i] = { t: new Date(t1), y: y };
    }
    return points2d;
  }  
	
  function createMinimumLineDataset(seriesResponse, points, lineColor) {
		let min = seriesResponse.observedMin;
		let minPoints = [{t:points[0].t, y:min}, {t:points[points.length-1].t, y:min}];
		
		return {
			data: minPoints,
			label: ' min ',
			fill:  false,
			borderColor: lineColor,
			borderWidth: 1,
			borderDash: [2, 2],
			pointRadius: 0
		};
  }
    
  function createMaximumLineDataset(seriesResponse, points, lineColor) {
  	let max = seriesResponse.observedMax;
		let maxPoints = [{t:points[0].t, y:max}, {t:points[points.length-1].t, y:max}];
		
		return {
			data: maxPoints,
			label: ' max ',
			fill:  false,
			borderColor: lineColor,
			borderWidth: 1,
			pointRadius: 0
		};
  }
    
  function createAverageLineDataset(seriesResponse, points, lineColor) {
		let avg = seriesResponse.observedSum / seriesResponse.observedValues;
		let avgPoints = [{t:points[0].t, y:avg}, {t:points[points.length-1].t, y:avg}];
		
		return {
			data: avgPoints,
			label: ' avg ',
			fill:  false,
			borderColor: lineColor,
			borderWidth: 1,
			borderDash: [10, 4],
			pointRadius: 0
		};
  }
    
  function createCurrentLineDataset(widget, seriesResponse, points, lineColor, bgColor) {
		let pointRadius = widget.options.drawPoints ? 3 : 0;
    let label = seriesResponse.instance;
    if (widget.series.indexOf('*') > 0)
      label += ': '+ (seriesResponse.series.replace(new RegExp(widget.series.replace('*', '(.*)')), '$1'));
    return {
			data: points,
			label: label,
			backgroundColor: bgColor,
			borderColor: lineColor,
			borderWidth: 1,
      pointRadius: pointRadius,
		};
  }
    
  /**
   * Creates one or more lines for a single series dataset related to the widget.
   * A widget might display multiple series in the same graph generating one or more dataset for each of them.
   */
  function createSeriesDatasets(widget, seriesResponse, lineColor, bgColor) {
    if (widget.options.perSec && seriesResponse.points.length > 4) {    
  		//TODO add min/max/avg per sec lines
      return [ createCurrentLineDataset(widget, seriesResponse, points1Dto2DPerSec(seriesResponse.points), lineColor, bgColor) ];
  	}
  	let points = points1Dto2D(seriesResponse.points, true);
  	let datasets = [];
  	datasets.push(createCurrentLineDataset(widget, seriesResponse, points, lineColor, bgColor));
  	if (points.length > 0 && widget.options.drawAvgLine) {
			datasets.push(createAverageLineDataset(seriesResponse, points, lineColor));
		}
		if (points.length > 0 && widget.options.drawMinLine && seriesResponse.observedMin > 0) {
			datasets.push(createMinimumLineDataset(seriesResponse, points, lineColor));
		}
		if (points.length > 0 && widget.options.drawMaxLine) {
			datasets.push(createMaximumLineDataset(seriesResponse, points, lineColor));
		}
	  return datasets;
  }

  /**
   * Should be called whenever the configuration of the widget changes in way that needs to be transfered to the chart options.
   * Basically translates the MC level configuration options to Chart.js options
   */
  function onConfigUpdate(widget, chart) {
    let options = chart.options;
    options.scales.yAxes[0].ticks.beginAtZero = widget.options.beginAtZero;
    options.scales.xAxes[0].ticks.source = widget.options.autoTimeTicks ? 'auto' : 'data';
    options.elements.line.tension = widget.options.drawCurves ? 0.4 : 0;
    let time = widget.options.drawAnimations ? 1000 : 0;
    options.animation.duration = time;
    options.responsiveAnimationDuration = time;
    let rotation = widget.options.rotateTimeLabels ? 90 : undefined;
    options.scales.xAxes[0].ticks.minRotation = rotation;
    options.scales.xAxes[0].ticks.maxRotation = rotation;
    options.scales.xAxes[0].ticks.display = widget.options.showTimeLabels === true;
    options.legend.display = widget.options.showLegend === true;
    return chart;
  }

  function onDataUpdate(update) {
    let data = update.data;
    let widget = update.widget;
    let chart = update.chart();
    let datasets = [];
    for (let j = 0; j < data.length; j++) {
      datasets = datasets.concat(
          createSeriesDatasets(widget, data[j], DEFAULT_LINE_COLORS[j], DEFAULT_BG_COLORS[j]));
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

   var chart;

   function onDataUpdate(data) {
      let zeroToMinValues = [];
      let minToMaxValues = [];
      let labels = [];
      for (let i = 0; i < data.length; i++) {
         let trace = data[i]; 
         let startTime = trace.startTime;
         for (let j = 0; j < trace.spans.length; j++) {
            let span = trace.spans[j];
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
      };
      chart.update(0);
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
         }
      });
   }

   function onOpenPopup(series) {
      $.getJSON("api/trace/data/"+series, function(data) {
         $('#trace-details').show();
         onDataUpdate(data);
      });
   }

   function onClosePopup() {
      if (chart) {
         chart.destroy();
         chart = undefined;
      }
      $('#trace-details').hide();
   }

   /**
    * Public API below:
    */
   return {
      onOpenPopup: (series) => onOpenPopup(series),
      onClosePopup: () => onClosePopup(),
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
 * The object that manages the internal state of the monitoring console page.
 * 
 * It depends on the MonitoringConsole.Utils object.
 */
MonitoringConsole.Model = (function() {
	/**
	 * Key used in local stage for the userInterface
	 */
	const LOCAL_UI_KEY = 'fish.payara.monitoring-console.defaultConfigs';
	
	const UI_PRESETS = {
			pages: {
				core: {
					name: 'Core',
					numberOfColumns: 3,
					widgets: [
						{ series: 'ns:jvm HeapUsage',      grid: { item: 0, column: 0, span: 1} },
						{ series: 'ns:jvm CpuUsage',       grid: { item: 1, column: 0, span: 1} },
						{ series: 'ns:jvm ThreadCount',    grid: { item: 0, column: 1, span: 1} },
						{ series: 'ns:web RequestCount',   grid: { item: 0, column: 2, span: 1}, options: { perSec: true, autoTimeTicks: true } },
						{ series: 'ns:web ActiveSessions', grid: { item: 1, column: 2, span: 1} },
					]
				}
			},
			settings: {},
	};

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
		
		var currentPageId;
		
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
			if (!widget.options) {
				widget.options = { 
					beginAtZero: true,
					autoTimeTicks: true,
					//TODO no data can be a good thing (a series hopefully does not come up => render differently to "No Data" => add a config for that switch)
				};
			}
			if (!widget.grid)
				widget.grid = {};
			return widget;
		}
		
		function doStore() {
			window.localStorage.setItem(LOCAL_UI_KEY, doExport());
		}
		
		function doDeselect() {
			Object.values(pages[currentPageId].widgets)
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
			currentPageId = page.id;
			return page;
		}
		
		function doImport(userInterface) {
			if (!userInterface) {
				return false;
			}
			let isPagesOnly = !userInterface.pages || !userInterface.settings;
			if (!isPagesOnly)
				settings = userInterface.settings;
			let importedPages = isPagesOnly ? userInterface : userInterface.pages;
			// override or add the entry in pages from userInterface
			if (Array.isArray(importedPages)) {
				for (let i = 0; i < importedPages.length; i++) {
					try {
						let page = sanityCheckPage(importedPages[i]);
						pages[page.id] = page;
					} catch (ex) {
					}
				}
			} else {
				for (let [id, page] of Object.entries(importedPages)) {
					try {
						page.id = id;
						pages[id] = sanityCheckPage(page); 
					} catch (ex) {
					}
				}
			}
			if (Object.keys(pages).length > 0) {
				currentPageId = Object.keys(pages)[0];
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
			let page = pages[currentPageId];
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
			let maxRows = Math.max(numberOfColumns, layout.map(column => column.length).reduce((acc, cur) => acc ? Math.max(acc, cur) : cur));
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
				return pages[currentPageId];
			},
			
			listPages: function() {
				return Object.values(pages).map(function(page) { 
					return { id: page.id, name: page.name, active: page.id === currentPageId };
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
						doImport(JSON.parse(json));
					}
				} else {
					doImport(userInterface);
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
				doImport(ui ? JSON.parse(ui) : JSON.parse(JSON.stringify(UI_PRESETS)));
				return pages[currentPageId];
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
				let page = pages[currentPageId];
				page.name = name;
				page.id = pageId;
				pages[pageId] = page;
				delete pages[currentPageId];
				currentPageId = pageId;
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
				delete pages[currentPageId];
				currentPageId = pageIds[0];
				return pages[currentPageId];
			},

			resetPage: function() {
				let presets = UI_PRESETS;
				if (presets && presets.pages && presets.pages[currentPageId]) {
					let preset = presets.pages[currentPageId];
					pages[currentPageId] = sanityCheckPage(JSON.parse(JSON.stringify(preset)));
					doStore();
					return true;
				}
				return false;
			},
			
			switchPage: function(pageId) {
				if (!pages[pageId])
					return undefined;
				currentPageId = pageId;
				return pages[currentPageId];
			},
			
			removeWidget: function(series) {
				let widgets = pages[currentPageId].widgets;
				if (series && widgets) {
					delete widgets[series];
				}
			},
			
			addWidget: function(series) {
				if (typeof series !== 'string')
					throw 'configuration object requires string property `series`';
				doDeselect();
				let widgets = pages[currentPageId].widgets;
				let widget = { series: series };
				widgets[series] = sanityCheckWidget(widget);
				widget.selected = true;
			},
			
			configureWidget: function(widgetUpdate, series) {
				let selected = series
					? [pages[currentPageId].widgets[series]]
					: Object.values(pages[currentPageId].widgets).filter(widget => widget.selected);
				selected.forEach(widget => widgetUpdate(widget));
				doStore();
			},
			
			select: function(series) {
				let widget = pages[currentPageId].widgets[series];
				widget.selected = widget.selected !== true;
				doStore();
				return widget.selected === true;
			},
			
			deselect: function() {
				doDeselect();
				doStore();
			},
			
			selected: function() {
				return Object.values(pages[currentPageId].widgets)
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
					|| Object.keys(pages[currentPageId].widgets).length == 0
					|| Object.values(pages[currentPageId].widgets).filter(widget => widget.selected).length > 0;
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
			
			pause: function() {
				doPause();
			},
		};
	})();
	
	function doInit(onDataUpdate) {
		UI.load();
		Interval.init(function() {
			let widgets = UI.currentPage().widgets;
			let payload = {
			};
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
			request.done(function(response) {
				Object.values(widgets).forEach(function(widget) {
					onDataUpdate({
						widget: widget,
						data: response[widget.series],
						chart: () => Charts.getOrCreate(widget),
					});
				});
			});
			request.fail(function(jqXHR, textStatus) {
				Object.values(widgets).forEach(function(widget) {
					onDataUpdate({
						widget: widget,
						chart: () => Charts.getOrCreate(widget),
					});
				});
			});
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
				Charts.update(widget, );
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
					UI.addWidget(series);
					Interval.tick();
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
        box.append($('<canvas/>',{ id: cell.widget.target }));
        return box;
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
        for (let i = 0; i < tags.length; i++) {
            let tag = tags[i];
            if (tag.startsWith('@:')) {
                text += '<code>'+tag.substring(2)+'</code> ';
            } else {
                text +='<i>'+tag+'</i> ';
            }
        }
        text += toWords(metric);
        if (widget.options.perSec) {
            text += ' <i>(1/sec)</i>';
        }
        return text;
    }

    function createWidgetToolbar(cell) {
        let series = cell.widget.series;
        return $('<div/>', {"class": "caption-bar"})
            .append($('<h3/>', {title: 'Select '+series}).html(formatSeriesName(cell.widget))
                .click(() => onWidgetToolbarClick(cell.widget)))
            .append(createToolbarButton('Remove chart from page', '&times;', () => onWidgetDelete(series)))
            .append(createToolbarButton('Enlarge this chart', '&plus;', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanMore(series))))
            .append(createToolbarButton('Shrink this chart', '&minus;', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.spanLess(series))))
            .append(createToolbarButton('Move to right', '&#9655;', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveRight(series))))
            .append(createToolbarButton('Move to left', '&#9665;', () => onPageUpdate(MonitoringConsole.Model.Page.Widgets.moveLeft(series))));
    }

    function createToolbarButton(title, icon, onClick) {
        return $('<button/>', { "class": "btnIcon", title: title}).html(icon).click(onClick);
    }


    function createWidgetSettings(widget) {
        let settings = createSettingsTable('settings-widget')
            .append(createSettingsHeaderRow(formatSeriesName(widget)))
            .append(createSettingsHeaderRow('General'))
            .append(createSettingsDropdownRow('Type', {line: 'Time Curve', bar: 'Range Indicator'}, widget.type, (widget, selected) => widget.type = selected))
            .append(createSettingsSliderRow('Span', 1, 4, widget.grid.span || 1, (widget, value) => widget.grid.span = value))
            .append(createSettingsSliderRow('Column', 1, 4, 1 + (widget.grid.column || 0), (widget, value) => widget.grid.column = value - 1))
            .append(createSettingsSliderRow('Item', 1, 4, 1 + (widget.grid.item || 0), (widget, value) => widget.grid.item = value - 1))
            .append(createSettingsHeaderRow('Data'))
            .append(createSettingsCheckboxRow('Add Minimum', widget.options.drawMinLine, (widget, checked) => widget.options.drawMinLine = checked))
            .append(createSettingsCheckboxRow('Add Maximum', widget.options.drawMaxLine, (widget, checked) => widget.options.drawMaxLine = checked))            
            ;
        if (widget.type === 'line') {
            settings
            .append(createSettingsCheckboxRow('Add Average', widget.options.drawAvgLine, (widget, checked) => widget.options.drawAvgLine = checked))
            .append(createSettingsCheckboxRow('Per Second', widget.options.perSec, (widget, checked) => widget.options.perSec = checked))
            .append(createSettingsHeaderRow('Display Options'))
            .append(createSettingsCheckboxRow('Begin at Zero', widget.options.beginAtZero, (widget, checked) => widget.options.beginAtZero = checked))
            .append(createSettingsCheckboxRow('Automatic Labels', widget.options.autoTimeTicks, (widget, checked) => widget.options.autoTimeTicks = checked))
            .append(createSettingsCheckboxRow('Use Bezier Curves', widget.options.drawCurves, (widget, checked) => widget.options.drawCurves = checked))
            .append(createSettingsCheckboxRow('Use Animations', widget.options.drawAnimations, (widget, checked) => widget.options.drawAnimations = checked))
            .append(createSettingsCheckboxRow('Label X-Axis at 90°', widget.options.rotateTimeLabels, (widget, checked) => widget.options.rotateTimeLabels = checked))
            .append(createSettingsCheckboxRow('Show Points', widget.options.drawPoints, (widget, checked) => widget.options.drawPoints = checked))            
            .append(createSettingsCheckboxRow('Show Stabe', widget.options.drawStableLine, (widget, checked) => widget.options.drawStableLine = checked))
            .append(createSettingsCheckboxRow('Show Legend', widget.options.showLegend, (widget, checked) => widget.options.showLegend = checked))
            .append(createSettingsCheckboxRow('Show Time Labels', widget.options.showTimeLabels, (widget, checked) => widget.options.showTimeLabels = checked))
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
        return createSettingsTable('settings-data')
            .append(createSettingsHeaderRow('Data'))
            .append(createSettingsRow('Instances', () => instanceSelection));
    }

    function createSettingsHeaderRow(caption) {
        return $('<tr/>').append($('<th/>', {colspan: 2}).html(caption).click(function() {
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

    function createSettingsCheckboxRow(label, checked, onChange) {
        return createSettingsRow(label, () => createSettingsCheckbox(checked, onChange));
    }

    function createSettingsTable(id) {
        return $('<table />', { 'class': 'settings', id: id });
    }

    function createSettingsRow(label, createInput) {
        return $('<tr/>').append($('<td/>').text(label)).append($('<td/>').append(createInput()));   
    }

    /**
     * Creates a checkbox to configure the attributes of a widget.
     *
     * @param {boolean} isChecked - if the created checkbox should be checked
     * @param {function} onChange - a function accepting two arguments: the updated widget and the checked state of the checkbox after a change
     */
    function createSettingsCheckbox(isChecked, onChange) {
        return $("<input/>", { type: 'checkbox', checked: isChecked })
            .on('change', function() {
                let checked = this.checked;
                MonitoringConsole.Model.Page.Widgets.Selection.configure((widget) => onChange(widget, checked));
            });
    }

    function createSettingsSliderRow(label, min, max, value, onChange) {
        return createSettingsRow(label, () => $('<input/>', {type: 'number', min:min, max:max, value: value})
            .on('input change', function() {  
                let val = this.valueAsNumber;
                onPageUpdate(MonitoringConsole.Model.Page.Widgets.Selection.configure((widget) => onChange(widget, val)));
            }));
    }

    function createSettingsDropdownRow(label, options, value, onChange) {
        let dropdown = $('<select/>');
        Object.keys(options).forEach(option => dropdown.append($('<option/>', {text:options[option], value:option, selected: value === option})));
        dropdown.change(() => onPageUpdate(MonitoringConsole.Model.Page.Widgets.Selection.configure((widget) => onChange(widget, dropdown.val()))));
        return createSettingsRow(label, () => dropdown);
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~[ Event Handlers ]~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    function onWidgetToolbarClick(widget) {
        MonitoringConsole.Model.Page.Widgets.Selection.toggle(widget.series);
        onWidgetUpdate(widget);
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
        let boxId = widget.target + '-box';
        let box = $('#'+boxId);
        if (box.length == 0) {
            if (console && console.log)
                console.log('WARN: Box for chart ' + widget.series + ' not ready.');
            return;
        }
        let td = box.closest('.widget');
        if (update.data) {
            td.children('.status-nodata').hide();
            let points = update.data[0].points;
            let stable = points.length === 4 && points[1] === points[3] && widget.type === 'line';
            if (stable && !widget.options.drawStableLine) {
                if (td.children('.stable').length == 0) {
                    let info = $('<div/>', { 'class': 'stable' });
                    info.append($('<span/>', { text: points[1] }));
                    td.append(info);
                    box.hide();
                }
            } else {
                td.children('.stable').remove();
                box.show();
                MonitoringConsole.Chart.getAPI(widget).onDataUpdate(update);
            }
        } else {
            td.children('.status-nodata').width(box.width()-10).height(box.height()-10).show();
        }
        
        onWidgetUpdate(widget);
    }

    /**
     * Called when changes to the widget require to update the view of the widget (non data related changes)

     * TODO this should be called by the model in the same way onDataUpdate is whenever config of a widget is configured - also rename to onWidgetConfigurationUpdate?
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
