Chart.defaults.global.defaultFontColor = "#fff";

var MonitoringConsole = (function() {
	/**
	 * Key used in local stage for the default page configuration
	 */
	const defaultConfigKey = 'fish.payara.monitoring-console.defaultConfigs';
	
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
	
	/**
	 * {object} - map of the chart configurations. 
	 * 
	 * All configuration properties must not be complex objects (values).
	 */
	var configs = {};
	
	/**
	 * Loads and returns the configuration from the local storage
	 */
	function loadLocalDefaultConfigs(preConstructionFn) {
		var localStorage = window.localStorage;
		var item = localStorage.getItem(defaultConfigKey);
		if (item) {
			var defaultConfigs = JSON.parse(item);
			if (preConstructionFn) {
				Object.values(defaultConfigs).forEach(preConstructionFn);
			}
			configs = defaultConfigs;
			updateAllCharts();
		}
	}
	
	/**
	 * Updates the current configuration in the local storage
	 */
	function storeLocalDefaultConfig() {
		window.localStorage.setItem(defaultConfigKey, JSON.stringify(configs));
	}
	
	function customTimeLables(value, index, values) {
		if (values.length == 0 || index == 0)
			return value;
		var span = values[values.length -1].value - values[0].value;
		if (span < 120000) { // less then two minutes
			var lastMinute = new Date(values[index-1].value).getMinutes();
			return new Date(values[index].value).getMinutes() != lastMinute ? value : ''+new Date(values[index].value).getSeconds();
		}
		return value;
	}
	
	function seriesName(configOrSeries) {
		var type = typeof configOrSeries;
		if (type === 'string') {
			return configOrSeries;
		} 
		if (type === 'object') {
			return configOrSeries.series;
		}
		return undefined;
	}
	
	/**
	 * Adds a new Chart.js chart object initialised for the given MC level configuration to the charts object
	 */
	function createChart(config) {
		return new Chart(config.target, {
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
			},
		});
	}
	
	/**
	 * Updates the referenced chart by fetching the newest data from the server and applying it to the given chart.
	 * Should no chart exist (but a config) a new chart is created.
	 */
	function updateChart(configOrSeries) {
		var series = seriesName(configOrSeries);
		if (!series)
			return;
		var config = configs[series];
		var chart = charts[series];
		if (!chart) { // might be one newly added
			chart = createChart(config);
			charts[series] = chart;
		}
		$.getJSON('api/series/' + series + '/statistics', function(stats) {
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
	
	function removeChart(configOrSeries) {
		var series = seriesName(configOrSeries);
		if (series) {
			delete configs[series];
			var chart = charts[series];
			delete charts[series];
			if (chart) {
				chart.destroy();
			}
		}
	}
	
	function addChart(config) {
		if (typeof config === 'object') {
			if (typeof config.series !== 'string')
				throw 'configuration object requires string property `series`';
			if (typeof config.target !== 'string')
				throw 'configuration object requires string property `target`';
			configs[config.series] = config;
		} else {
			throw 'configuration was no object but: ' + config;
		}
	}
	
	function removeAllCharts() {
		if (updater) {
			clearInterval(updater);
		}
		applyToAllCharts(removeChart);
	}
	
	/**
	 * Updates the charts of the current configuration.
	 */
	function updateAllCharts() {
		applyToAllCharts(updateChart);
		if (!updater) {
			updater = setInterval(updateAllCharts, 1000);
		}
	}
	
	/**
	 * Applies a function to all charts.
	 * 
	 * @param {function} fn - a function that accepts a single argument of the string series name
	 */
	function applyToAllCharts(fn) {
		Object.keys(configs).forEach(fn);
	}
	
	return {
		/**
		 * @param {function} consumer - a function with one argument accepting the array of series names
		 */
		loadAllSeries: function(consumer) {
			$.getJSON("api/series/", consumer);
		},
		
		init: function(preConstructionFn) {
			loadLocalDefaultConfigs(preConstructionFn);
		},
		
		/**
		 * @param {function} optionsUpdate - a function accepting chart options applied to each chart
		 */
		configure: function(optionsUpdate) {
			Object.values(charts).forEach(function(chart) {
				optionsUpdate.call(window, chart.options);
				chart.update();
			});
		},
		
		add: function(config) {
			addChart(config);
			storeLocalDefaultConfig();
		},
		
		/**
		 * Changes the active charts to those of the passed configuration.
		 */
		update: function() {
			updateAllCharts();
		},
		
		dispose: function(configOrSeries) {
			removeChart(configOrSeries);
			storeLocalDefaultConfig();
		},
		
		/**
		 * Destroys all chart objects
		 */
		reset: function() {
			removeAllCharts();
		},
	};
})();