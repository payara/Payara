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

MonitoringConsole.LineChart = (function() {
	
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
	
    function createMinimumLineDataset(data, points, lineColor) {
		let min = data.observedMin;
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
    
    function createMaximumLineDataset(data, points, lineColor) {
    	let max = data.observedMax;
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
    
    function createAverageLineDataset(data, points, lineColor) {
		let avg = data.observedSum / data.observedValues;
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
    
    function createMainLineDataset(data, points, lineColor, bgColor) {
		return {
			data: points,
			label: data.instance,
			backgroundColor: bgColor,
			borderColor: lineColor,
			borderWidth: 1
		};
    }
    
    function createInstancePoints(points1d) {
    	if (!points1d)
    		return [];
    	let points2d = new Array(points1d.length / 2);
		for (let i = 0; i < points2d.length; i++) {
			points2d[i] = { t: new Date(points1d[i*2]), y: points1d[i*2+1] };
		}
		return points2d;
    }
    
    function createInstancePerSecPoints(points1d) {
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
    
    function createInstanceDatasets(widget, data, lineColor, bgColor) {
    	if (widget.options.perSec) {
    		return [ createMainLineDataset(data, createInstancePerSecPoints(data.points), lineColor, bgColor) ];
    	}
    	let points = createInstancePoints(data.points);
    	let datasets = [];
    	datasets.push(createMainLineDataset(data, points, lineColor, bgColor));
    	if (points.length > 0 && widget.options.drawAvgLine) {
			datasets.push(createAverageLineDataset(data, points, lineColor));
		}
		if (points.length > 0 && widget.options.drawMinLine && data.observedMin > 0) {
			datasets.push(createMinimumLineDataset(data, points, lineColor));
		}
		if (points.length > 0 && widget.options.drawMaxLine) {
			datasets.push(createMaximumLineDataset(data, points, lineColor));
		}
		return datasets;
    }
    
	return {
		onDataUpdate: function(update) {
			let data = update.data;
			let widget = update.widget;
			let chart = update.chart();
			let datasets = [];
			for (let j = 0; j < data.length; j++) {
				datasets = datasets.concat(
						createInstanceDatasets(widget, data[j], DEFAULT_LINE_COLORS[j], DEFAULT_BG_COLORS[j]));
			}
			chart.data.datasets = datasets;
			chart.update(0);
		}
	};
})();
