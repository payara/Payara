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
  const Colors = MonitoringConsole.View.Colors;
  const Theme = MonitoringConsole.Model.Theme;

  function timeLable(secondsAgo, index, lastIndex, secondsInterval) {
    if (index == lastIndex && secondsAgo == 0)
      return 'now';
    if (index == 0 || index == lastIndex && secondsAgo > 0) {
      if (Math.abs(secondsAgo - 60) <= secondsInterval * 2)
        return '60s ago'; // this corrects off by 1 which is technically inaccurate but still 'more readable' for the user
      if (Math.abs((secondsAgo % 60) - 60) <= secondsInterval)
        return Math.round(secondsAgo / 60) + 'mins ago';
      if (secondsAgo <= 60)
        return secondsAgo +'s ago';
      return Math.floor(secondsAgo / 60) + 'mins ' + (secondsAgo % 60) + 's ago';
    }
    return undefined;
  }

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
              let now = new Date();
              let isLive = now - reference < 5000; // is within the last 5 secs
              if (values.length == 1)
                return isLive ? 'now' : Units.formatTime(new Date(reference));
              let secondsInterval = (values[1].value - values[0].value) / 1000;
              let secondsAgo = (values[lastIndex].value - values[index].value) / 1000;
              if (isLive) {
                return timeLable(secondsAgo, index, lastIndex, secondsInterval);
              }
              let reference2 = new Date(values[lastIndex-1].value);
              let isRecent = now - reference < (5000 + (reference - reference2));
              if (isRecent) {
                return timeLable(secondsAgo, index, lastIndex, secondsInterval);
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
    let backgroundPlugin = {
      beforeDraw: function (chart) {
        let yAxis = chart.chart.scales["y-axis-0"];
        let areas = chart.chart.data.areas;
        if (!Array.isArray(areas) || areas.length === 0)
          return;
        let ctx = chart.chart.ctx;
        let xAxis = chart.chart.scales["x-axis-0"];
        function yOffset(y) {
          let yMax = yAxis.ticksAsNumbers[0];
          if (y === undefined)
            y = yMax;
          let yMin = yAxis.ticksAsNumbers[yAxis.ticksAsNumbers.length - 1];
          let yVisible = y - yMin;
          let yRange = yMax - yMin;
          return yAxis.bottom - Math.max(0, (yAxis.height * yVisible / yRange));
        }
        for (let i = 0; i < areas.length; i++) {
          let area = areas[i];
          if (area.min != area.max) {
            let yAxisMin = yOffset(area.min);
            let yAxisMax = yOffset(area.max);
            let height = yAxisMax - yAxisMin;
            let width = 10;
            let left = xAxis.right + 1;
            ctx.fillStyle = '#333333'; // neutral back for transparent color on top to not become bluish
            ctx.fillRect(left, yAxisMin, width, height);
            let gradient = ctx.createLinearGradient(0, yAxisMin, 0, yAxisMax);
            if (i + 1 < areas.length && areas[i+1].max == area.min) {
              gradient.addColorStop(0.33, area.color);
              gradient.addColorStop(0, areas[i+1].color);
            } else if (i > 0 && areas[i-1].max == area.min) {
              gradient.addColorStop(0.33, area.color);
              gradient.addColorStop(0, areas[i-1].color);
            } else {
              gradient.addColorStop(0, area.color);  
            }            
            gradient.addColorStop(1, area.color);
            ctx.fillStyle = gradient;
            ctx.fillRect(left, yAxisMin, width, height);          
          }
        }
      }
    };
    return new Chart(widget.target, {
          type: 'line',
          data: { datasets: [], },
          options: options,
          plugins: [ backgroundPlugin ],       
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
    let lineWidth = Theme.option('line-width', 3) / 2;
    return {
			data: points,
			label: label,
      fill: widget.options.noFill !== true,
			backgroundColor: bgColor,
			borderColor: lineColor,
			borderWidth: lineWidth,
      pointRadius: pointRadius,
		};
  }
    
  /**
   * Creates one or more lines for a single series dataset related to the widget.
   * A widget might display multiple series in the same graph generating one or more dataset for each of them.
   */
  function createSeriesDatasets(widget, seriesData, watches) {
    let lineColor = seriesData.legend.color;
    let bgColor = seriesData.legend.background;
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
	  return datasets;
  }

  function createDecorationDatasets(widget, data) {
    let t0;
    let t1;
    for (let j = 0; j < data.length; j++) {
      let px = data[j].points;
      t0 = t0 === undefined ? px[0] : Math.min(t0, px[0]);
      t1 = t1 === undefined ? px[px.length-2] : Math.max(t1, px[px.length-2]);
    }
    let points = [{ t:t0, y: 0}, {t: t1, y: 0}];
    let decorations = widget.decorations;
    let datasets = [];
    if (decorations.waterline && decorations.waterline.value) {
      let color = decorations.waterline.color || Theme.color('waterline');
      datasets.push(createHorizontalLineDataset(' waterline ', points, decorations.waterline.value, color, [2,2]));
    }
    if (decorations.thresholds.alarming.display) {
      let color = decorations.thresholds.alarming.color || Theme.color('alarming');
      datasets.push(createHorizontalLineDataset(' alarming ', points, decorations.thresholds.alarming.value, color, [2,2]));
    }
    if (decorations.thresholds.critical.display) {
      let color = decorations.thresholds.critical.color || Theme.color('critical');
      datasets.push(createHorizontalLineDataset(' critical ', points, decorations.thresholds.critical.value, color, [2,2]));      
    }
    return datasets; 
  }

  function createBackgroundAreas(widget, watches) {    
    let areas = [];
    if (watches === undefined || watches.length == 0)
      return areas;
    let watch = watches[0];
    if (watch.red)
      areas.push(createBackgroundArea(watch.red, [watch.amber, watch.green]));
    if (watch.amber)
      areas.push(createBackgroundArea(watch.amber, [watch.red, watch.green])); 
    if (watch.green)
      areas.push(createBackgroundArea(watch.green, [watch.amber, watch.red]));
    return areas;
  }

  function createBackgroundArea(level, levels) {
    let color = Theme.color(level.level);
    let min = 0;
    let max;
    if (level.start.operator == '>' || level.start.operator == '>=') {
      min = level.start.threshold;
      for (let i = 0; i < levels.length; i++) {
        let other = levels[i];
        if (other !== undefined && other.start.threshold > min) {
          max = max === undefined ? other.start.threshold : Math.min(max, other.start.threshold);
        }
      }
    } else if (level.start.operator == '<' || level.start.operator == '<=') {
      max = level.start.threshold;
      for (let i = 0; i < levels.length; i++) {
        let other = levels[i];
        if (other !== undefined && other.start.threshold < max) {
          min = Math.max(min, other.start.threshold);
        }
      }
    }
    return { color: color, min: min, max: max };
  }

  /**
   * Should be called whenever the configuration of the widget changes in way that needs to be transfered to the chart options.
   * Basically translates the MC level configuration options to Chart.js options
   */
  function onConfigUpdate(widget, chart) {
    let options = chart.options;
    options.elements.line.tension = widget.options.drawCurves ? 0.4 : 0;
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
      datasets = datasets.concat(createSeriesDatasets(widget, data[j], update.watches));
    }
    datasets = datasets.concat(createDecorationDatasets(widget, data));
    chart.data.datasets = datasets;
    chart.data.areas = createBackgroundAreas(widget, update.watches);    
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
