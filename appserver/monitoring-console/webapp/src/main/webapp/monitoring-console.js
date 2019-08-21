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
Chart.defaults.global.defaultFontColor = "#fff";

var MonitoringConsole = (function() {
	const defaultBackgroundColors = [
        'rgba(153, 102, 255, 0.2)',
        'rgba(255, 99, 132, 0.2)',
        'rgba(54, 162, 235, 0.2)',
        'rgba(255, 206, 86, 0.2)',
        'rgba(75, 192, 192, 0.2)',
        'rgba(255, 159, 64, 0.2)'
    ];
    const defaultBorderColors = [
        'rgba(153, 102, 255, 1)',
        'rgba(255, 99, 132, 1)',
        'rgba(54, 162, 235, 1)',
        'rgba(255, 206, 86, 1)',
        'rgba(75, 192, 192, 1)',
        'rgba(255, 159, 64, 1)'
    ];
	
	/**
	 * {function} - interval function updating the graphs
	 */
	var updater;
	/**
	 * {object} - map of the charts objects as created by Chart.js with series as key
	 */
	var charts = {};
	
	function customTimeLables(value, index, values) {
		if (values.length == 0 || index == 0)
			return value;
		var span = values[values.length -1].value - values[0].value;
		if (span < 120000) { // less then two minite
			var lastMinute = new Date(values[index-1].value).getMinutes();
			return new Date(values[index].value).getMinutes() != lastMinute ? value : ''+new Date(values[index].value).getSeconds();
		}
		return value;
	}
	
	/**
	 * Adds a new Chart.js chart object initialised for the given MC level configuration to the charts object
	 */
	function initChart(config) {
		if (config.closed)
			return;
		charts[config.series] = {
				config: config,
				canvas: new Chart(config.target, {
					type: 'line',
					data: {
						datasets: [],
					},
					options: {
						scales: {
							xAxes: [{
								type: 'time',
								gridLines: {
									color: 'rgb(120,120,120)',
								},
								time: {
									minUnit: 'second',
									round: 'second',
								},
								ticks: {
									callback: customTimeLables,
									minRotation: 90,
									maxRotation: 90,
								}
							}],
							yAxes: [{
								display: true,
								gridLines: {
									color: 'rgb(120,120,120)',
								},
								ticks: {
									beginAtZero: config.beginAtZero,
									precision:0, // no decimal places in labels
								},
							}],
						}
					}
				}),
		};
	}
	
	/**
	 * Updates a single chart by fetching the newest data from the server and applying it to the given chart.
	 */
	function updateChart(mcChart) {
		var config = mcChart.config;
		var chart = mcChart.canvas;
		if (config.closed) {
			if (chart) {
				chart.destroy();
				mcChart.canvas = null;
			}
			return;
		}
		$.getJSON('api/series/' + config.series + '/statistics', function(stats) {
			var localStats = stats[0];
			var datasets = [];
			for (var j = 0; j < stats.length; j++) {
				var instanceStats = stats[j];
				var points = instanceStats.points;
				var data = [];
				if (points) {
					data = new Array(points.length / 2);
					for (var i = 0; i < data.length; i++) {
						data[i] = { t: new Date(points[i*2]), y: points[i*2+1] };
					}
				}
				datasets.push({
					data: data,
					label: instanceStats.instance,
					backgroundColor: defaultBackgroundColors[j],
					borderColor: defaultBorderColors[j],
					borderWidth: 1
				});
			}
			if (datasets.length > 0) {
				var avg = localStats.observedSum / localStats.observedValues;
				var data = datasets[0].data;
				var avgData = [{t:data[0].t, y:avg}, {t:data[data.length-1].t, y:avg}];

				datasets.push({
					data: avgData,
					label: 'avg',
					fill:  false,
					borderColor: [ 'rgba(75, 192, 192, 1)' ],
					borderWidth: 1,
					pointRadius: 0
				});
			}

			chart.data.datasets = datasets;
			chart.options.title.display = true;
			chart.options.title.text = config.title ? config.title : config.series;
			chart.update(0);
		});
	}
	
	function resetAllCharts() {
		if (updater) {
			clearInterval(updater);
		}
		Object.values(charts).forEach(chart => chart.canvas.destroy());
		charts = {};
	}
	
	/**
	 * Updates the charts of the current configuration.
	 */
	function updateAllCharts() {
		Object.values(charts).forEach(updateChart);
	}
	
	return {
		/**
		 * @param {function} consumer - a function with one argument accepting the array of series names
		 */
		loadAllSeries: function(consumer) {
			$.getJSON("api/series/", consumer);
		},
		
		/**
		 * @param {function} update - function applied to all chart options
		 */
		configure: function(update) {
			Object.values(charts).forEach(function(chart) {
				if (!chart.config.closed) {
					update.call(window, chart.canvas.options);
					chart.canvas.update();
				}
			});
		},
		
		/**
		 * Changes the active charts to those of the passed configuration.
		 * 
		 * @param {array} configs - array of chart configurations
		 * @param {object} configs[i] - MC level chart configuration for a single chart
		 * @param {string} configs[i].target - id of the DOM element to contain the chart
		 * @param {string} configs[i].series - name of the series to display
		 * @param {string} configs[i].title - title text of the chart
		 * @param {boolean} configs[i].beginAtZero - true to force y-axis to start at zero
		 */
		show: function(configs) {
			for (var i = 0; i < configs.length; i++) {
				var config = configs[i];
				var chart = charts[config.series];
				if (chart && chart.canvas) {
					if (config.closed) {
						chart.canvas.destroy();
						chart.canvas = null;
					} else if (JSON.stringify(chart.config) !== JSON.stringify(config)) {
						initChart(config);
					}
				} else {
					initChart(config);
				}
			}
			updater = setInterval(updateAllCharts, 1000);
		},
		
		/**
		 * Destroys all chart objects
		 */
		reset: function() {
			resetAllCharts();
		},
	};
})();
