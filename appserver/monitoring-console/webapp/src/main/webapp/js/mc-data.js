/*
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
  
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
 * Conains the "static" data of the monitoring console.
 * Most of all this are the page presets.
 */
MonitoringConsole.Data = (function() {

	/**
	 * List of known name-spaces and their text description.
	 */
    const NAMESPACES = {
        web: 'Web Statistics',
        http: 'HTTP Statistics',
        jvm: 'JVM Statistics',
        metric: 'MP Metrics',
        trace: 'Request Tracing',
        map: 'Cluster Map Storage Statistics',
        topic: 'Cluster Topic IO Statistics',
        monitoring: 'Monitoring Console Internals',
        health: 'Health Checks',
        sql: 'SQL Tracing',
        other: 'Other',
    };

    /**
     * Description texts used in the preset pages...
     */
	const TEXT_HTTP_HIGH = "Requires *HTTP monitoring* to be enabled: Goto _Configurations_ => _Monitoring_ and set *'HTTP Service'* to *'HIGH'*.";
	const TEXT_WEB_HIGH = "Requires *WEB monitoring* to be enabled: Goto _Configurations_ => _Monitoring_ and set *'Web Container'* to *'HIGH'*.";
	const TEXT_REQUEST_TRACING = "If you did enable request tracing at _Configurations_ => _Request Tracing_ not seeing any data means no requests passed the tracing threshold which is a good thing.";

	const TEXT_CPU_USAGE = "Requires *CPU Usage HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _CPU Usage_ tab and check *'enabled'*";
	const TEXT_HEAP_USAGE = "Requires *Heap Usage HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _Heap Usage_ tab and check *'enabled'*";
	const TEXT_GC_PERCENTAGE = "Requires *Garbage Collector HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _Garbage Collector_ tab and check *'enabled'*";
	const TEXT_MEM_USAGE = "Requires *Machine Memory HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _Machine Memory Usage_ tab and check *'enabled'*";
	const TEXT_POOL_USAGE = "Requires *Connection Pool HealthCheck* to be enabled: Goto _Configurations_ => _HealthCheck_ => _Connection Pool_ tab and check *'enabled'*";
	const TEXT_LIVELINESS = "Requires *MicroProfile HealthCheck Checker* to be enabled: Goto _Configurations_ => _HealthCheck_ => _MicroProfile HealthCheck Checker_ tab and check *'enabled'*";

	/**
	 * Page preset information improted on page load.
	 */
	const PAGES = {
		core: {
			name: 'Core',
			numberOfColumns: 3,
			widgets: [
				{ series: 'ns:jvm HeapUsage', unit: 'percent',  
					grid: { item: 1, column: 0}, 
					axis: { min: 0, max: 100 },
					decorations: {
						thresholds: { reference: 'now', alarming: { value: 50, display: true }, critical: { value: 80, display: true }}}},
				{ series: 'ns:jvm CpuUsage', unit: 'percent',
					grid: { item: 1, column: 1}, 
					axis: { min: 0, max: 100 },
					decorations: {
						thresholds: { reference: 'now', alarming: { value: 50, display: true }, critical: { value: 80, display: true }}}},							
				{ series: 'ns:jvm ThreadCount', unit: 'count',  
					grid: { item: 0, column: 1}},
				{ series: 'ns:http ThreadPoolCurrentThreadUsage', unit: 'percent',
					grid: { item: 1, column: 2},
					status: { missing: { hint: TEXT_HTTP_HIGH }},
					axis: { min: 0, max: 100 },
					decorations: {
						thresholds: { reference: 'avg', alarming: { value: 50, display: true }, critical: { value: 80, display: true }}}},														
				{ series: 'ns:web RequestCount', unit: 'count',
					grid: { item: 0, column: 2}, 
					options: { perSec: true },
					status: { missing: { hint: TEXT_WEB_HIGH }}},
				{ series: 'ns:web ActiveSessions', unit: 'count',
					grid: { item: 0, column: 0},
					status: { missing: { hint: TEXT_WEB_HIGH }}},
			]
		},
		request_tracing: {
			name: 'Request Tracing',
			numberOfColumns: 4,
			widgets: [
				{ id: '1 ns:trace @:* Duration', series: 'ns:trace @:* Duration', type: 'bar', unit: 'ms',
					displayName: 'Trace Duration Range',
					grid: { item: 0, column: 0, colspan: 4, rowspan: 1 },
					axis: { min: 0, max: 5000 },
					options: { drawMinLine: true },
					status: { missing: { hint: TEXT_REQUEST_TRACING }},
					coloring: 'instance-series',
					decorations: { alerts: { noAmber: true, noRed: true }}},
				{ id: '2 ns:trace @:* Duration', series: 'ns:trace @:* Duration', type: 'line', unit: 'ms', 
					displayName: 'Trace Duration Above Threshold',
					grid: { item: 1, column: 0, colspan: 2, rowspan: 3 },
					options: { noFill: true },
					coloring: 'instance-series'},
				{ id: '3 ns:trace @:* Duration', series: 'ns:trace @:* Duration', type: 'annotation', unit: 'ms',
					displayName: 'Trace Data',
					grid: { item: 1, column: 2, colspan: 2, rowspan: 3 },
					coloring: 'instance-series'},
			]
		},
		http: {
			name: 'HTTP',
			numberOfColumns: 3,
			widgets: [
				{ series: 'ns:http ConnectionQueueCountOpenConnections', unit: 'count',
					grid: { column: 0, item: 0},
					status: { missing : { hint: TEXT_HTTP_HIGH }}},
				{ series: 'ns:http ThreadPoolCurrentThreadsBusy', unit: 'count',
					grid: { column: 0, item: 1},
					status: { missing : { hint: TEXT_HTTP_HIGH }}},
				{ series: 'ns:http ServerCount2xx', unit: 'count', 
					grid: { column: 1, item: 0},
					options: { perSec: true },
					status: { missing : { hint: TEXT_HTTP_HIGH }}},
				{ series: 'ns:http ServerCount3xx', unit: 'count', 
					grid: { column: 1, item: 1},
					options: { perSec: true },
					status: { missing : { hint: TEXT_HTTP_HIGH }}},
				{ series: 'ns:http ServerCount4xx', unit: 'count', 
					grid: { column: 2, item: 0},
					options: { perSec: true },
					status: { missing : { hint: TEXT_HTTP_HIGH }}},
				{ series: 'ns:http ServerCount5xx', unit: 'count', 
					grid: { column: 2, item: 1},
					options: { perSec: true },
					status: { missing : { hint: TEXT_HTTP_HIGH }}},
			]
		},
		health_checks: {
			name: 'Health Checks',
			numberOfColumns: 4,
			widgets: [
				{ series: 'ns:health CpuUsage', unit: 'percent', displayName: 'CPU',
  					grid: { column: 0, item: 0},
  					axis: { max: 100 },
  					status: { missing : { hint: TEXT_CPU_USAGE }}},
				{ series: 'ns:health HeapUsage', unit: 'percent', displayName: 'Heap',
  					grid: { column: 1, item: 1},
  					axis: { max: 100 },
  					status: { missing : { hint: TEXT_HEAP_USAGE }}},
				{ series: 'ns:health TotalGcPercentage', unit: 'percent', displayName: 'GC',
  					grid: { column: 1, item: 0},
  					axis: { max: 30 },
  					status: { missing : { hint: TEXT_GC_PERCENTAGE }}},
				{ series: 'ns:health PhysicalMemoryUsage', unit: 'percent', displayName: 'Memory',
  					grid: { column: 0, item: 1},
  					axis: { max: 100 },
  					status: { missing : { hint: TEXT_MEM_USAGE }}},
				{ series: 'ns:health @:* PoolUsage', unit: 'percent', coloring: 'series', displayName: 'Connection Pools',
  					grid: { column: 1, item: 2},
  					axis: { max: 100 },          					
  					status: { missing : { hint: TEXT_POOL_USAGE }}},
				{ series: 'ns:health LivelinessUp', unit: 'percent', displayName: 'MP Health',
  					grid: { column: 0, item: 2},
  					axis: { max: 100 },
  					options: { noCurves: true },
  					status: { missing : { hint: TEXT_LIVELINESS }}},
				{ series: 'ns:health ?:* *', unit: 'percent', type: 'alert', displayName: 'Alerts',
  					grid: { column: 2, item: 0, colspan: 2, rowspan: 3}}, 
			]
		},
		monitoring: {
			name: 'Monitoring',
			numberOfColumns: 3,
			widgets: [
				{ series: 'ns:monitoring @:* CollectionDuration', unit: 'ms', displayName: 'Sources Time',
					grid: { column: 0, item: 0, span: 2},
					axis: { max: 200 },
					coloring: 'series'},
				{ series: 'ns:monitoring @:* AlertCount', displayName: 'Alerts',
					grid: { column: 2, item: 2},
					coloring: 'series'},
				{ series: 'ns:monitoring CollectedSourcesCount', displayName: 'Sources',
					grid: { column: 0, item: 2}},
				{ series: 'ns:monitoring CollectedSourcesErrorCount', displayName: 'Sources with Errors', 
					grid: { column: 1, item: 2}},
				{ series: 'ns:monitoring CollectionDuration', unit: 'ms', displayName: 'Metrics Time',
					grid: { column: 2, item: 0},
					axis: { max: 1000},
					options: { drawMaxLine: true }},
				{ series: 'ns:monitoring WatchLoopDuration', unit: 'ms', displayName: 'Watches Time', 
					grid: { column: 2, item: 1},
					options: { drawMaxLine: true }},
			],
		},
		jvm: {
			name: 'JVM',
			numberOfColumns: 3,
			widgets: [
				{ series: 'ns:jvm TotalLoadedClassCount', displayName: 'Loaded Classes', 
					grid: { column: 2, item: 0}},
				{ series: 'ns:jvm UnLoadedClassCount', displayName: 'Unloaded Classes',
					grid: { column: 2, item: 1}},
				{ series: 'ns:jvm CommittedHeapSize', unit: 'bytes', displayName: 'Heap Size',
					grid: { column: 1, item: 0}},
				{ series: 'ns:jvm UsedHeapSize', unit: 'bytes', displayName: 'Used Heap', 
					grid: { column: 0, item: 0}},
				{ series: 'ns:jvm ThreadCount', displayName: 'Live Threads', 
					grid: { column: 1, item: 1}},
				{ series: 'ns:jvm DaemonThreadCount', displayName: 'Daemon Threads',
					grid: { column: 0, item: 1}},
			],
		},
		sql: {
			name: 'SQL',
			numberOfColumns: 3,
			widgets: [
				{ id: '1 ns:sql @:* MaxExecutionTime', 
					unit: 'ms',
					type: 'annotation',
					series: 'ns:sql @:* MaxExecutionTime',
					displayName: 'Slow SQL Queries',
					grid: { column: 0, item: 1, colspan: 2, rowspan: 2},
					mode: 'table',
					sort: 'value',
					fields: ['Timestamp', 'SQL', 'Value']},
				{ id: '2 ns:sql @:* MaxExecutionTime',
					unit: 'ms',
					series: 'ns:sql @:* MaxExecutionTime',
					displayName: 'Worst SQL Execution Time',
					grid: { column: 2, item: 1 },
					coloring: 'series' },						
				{ id: '3 ns:sql @:* MaxExecutionTime',
					unit: 'ms',
					type: 'alert',
					series: 'ns:sql @:* MaxExecutionTime',
					displayName: 'Slow SQL Alerts',
					grid: { column: 2, item: 2 },
					options: { noAnnotations: true }},
			],
		},
		alerts: {
			name: 'Alerts',
			numberOfColumns: 1,
			widgets: [
				{id: '1 ?:* *', series: '?:* *', type: 'alert', displayName: 'Ongoing Alerts',
					grid: {column: 0, item: 1},
					decorations: { alerts: { noStopped: true }},
					options: { noAnnotations: true}},
				{id: '2 ?:* *', series: '?:* *', type: 'alert', displayName: 'Past Unacknowledged Alerts',
					grid: {column: 0, item: 2},
					decorations: { alerts: { noOngoing: true, noAcknowledged: true}},
					options: { noAnnotations: true}},
			],
		},
		threads: {
			name: 'Threads',
			numberOfColumns: 4,
			widgets: [
				{ series: 'ns:health StuckThreadDuration', type: 'annotation', mode: 'table', unit: 'ms',
					displayName: 'Stuck Thread Incidents',
					grid: {column: 0, item: 1, colspan: 3, rowspan: 1},
					fields: ["Thread", "Started", "Value", "Threshold", "Suspended", "Locked", "State"]},
				{ series: 'ns:health HoggingThreadDuration', type: 'annotation', mode: 'table', unit: 'ms',
					displayName: 'Hogging Thread Incidents',
					grid: {column: 0, item: 2, colspan: 3, rowspan: 1},
					fields: ["Thread", "When", "Value", "Usage%", "Threshold%", "Method", "Exited"]},
				{ series: 'ns:jvm ThreadCount', displayName: 'Live Threads', 
					grid: {column: 3, item: 1}},
				{ series: 'ns:jvm DaemonThreadCount', displayName: 'Daemon Threads', 
					grid: {column: 3, item: 2}},							
			],
		}
	};


	/**
	 * Public API for data access
	 */ 
	return { 
		PAGES: PAGES,
		NAMESPACES: NAMESPACES,
	};
})();