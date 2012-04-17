/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.elasticity.metrics.jvm.memory;

import org.glassfish.api.ActionReport;
import org.glassfish.elasticity.api.AbstractMetricGatherer;
import org.glassfish.elasticity.metric.MetricAttribute;
import org.glassfish.elasticity.metric.MetricNode;
import org.glassfish.elasticity.metric.TabularMetricEntry;
import org.glassfish.elasticity.metrics.util.CollectMetricData;
import org.glassfish.elasticity.util.TabularMetricHolder;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;

import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import com.sun.enterprise.config.serverbeans.Clusters;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ParameterMap;
import com.sun.logging.LogDomains;
import com.sun.enterprise.v3.common.PropsFileActionReporter;

/**
 * @author Mahesh.Kannan@Oracle.Com
 * @author  carla.mott@oracle.com
 */
@Service(name = "jvm_memory")
public class JVMMemoryMetricHolder
    extends AbstractMetricGatherer<JVMMetricGathererConfig>
    implements MetricNode, PostConstruct {

    static final String _NAME = "jvm_memory";

    public Long max;

    private static final String baseURL = "http://localhost:4848/monitoring/domain/";
    private static final String jvm_memory = "jvm/memory";
    private static final String heap_used = "usedheapsize-count";
    private static final String heap_commited = "committedheapsize-count";
    private static final String maxheapsize = "maxheapsize-count";
    private AdminCommandContext context = null;

    // need to store instance name, instance port, instance host and data for the instance
    HashMap<String, JVMInstanceMemoryHolder>  instancesTable = new HashMap<String, JVMInstanceMemoryHolder>();

    MetricAttribute[] attributes;

    MemoryMXBean memBean;

    String serviceName;

    @Inject
    private CommandRunner cr;

    @Inject
    private Clusters clusters;

    private static final Logger _logger = Logger.getLogger("elastic-metrics93");

    @Override
    public void postConstruct() {
    }

    @Override
    protected void initialize(org.glassfish.paas.orchestrator.service.spi.Service service, JVMMetricGathererConfig config) {
        serviceName = service.getName();

       //create a hash map with all instance names in this service
        // each entry is a name of instance and the object which holds the data as in previous design
        // init the host and port for the instance when creating the object

        Cluster cluster = clusters.getCluster(serviceName);
        if (cluster == null) {
            System.out.println("No cluster called "+ serviceName);
            return;
        }
        List<Server> serverList = cluster.getInstances();
        if (serverList == null) {
            System.out.println("no server instances found.");
            return;
        }
        // until we have metric gatherer factories init this way
        for (Server server : serverList) {
            Map<String , Object> instanceInfo= new HashMap<String, Object>();
            String instanceName = server.getName();
            JVMInstanceMemoryHolder jvmInstanceHolder = new JVMInstanceMemoryHolder("localhost", "4848", instanceName);

            instancesTable.put(instanceName,jvmInstanceHolder);
            turnOnMonitoring(instanceName);
            System.out.println("Starting JVM Memory Metric Gatherer for  "+instanceName+" in service "+serviceName);

        }
    }

    @Override
    public void gatherMetric() {
        //only because we don't have metric factories and callback
        //  for each instance in the service get the data
        // use another class so can be done concurrently if needed
        for (String name : instancesTable.keySet()) {
            JVMInstanceMemoryHolder instanceMemoryHolder = (JVMInstanceMemoryHolder)instancesTable.get(name);
            instanceMemoryHolder.gatherMetric();
        }
    }

    @Override
    public void purgeDataOlderThan(int time, TimeUnit unit) {
        for (String name : instancesTable.keySet()) {
            Map<String , Object> instanceInfo= (Map)instancesTable.get(name);
            if (instanceInfo != null) {
                TabularMetricHolder<MemoryStat> table= (TabularMetricHolder) instanceInfo.get("table");
                try {
                table.purgeEntriesOlderThan(time, unit);
                } catch (Exception ex) {
                    _logger.log(Level.WARNING, "Exception during purging old entries", ex);
                }
            }
        }
        
    }

    @Override
    public void stop() {

    }

    @Override
    public String getName() {
        return _NAME;
    }

    private void turnOnMonitoring(String insName) {
        context = new AdminCommandContext(
                LogDomains.getLogger(JVMMemoryMetricHolder.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
        ActionReport report = context.getActionReport();

        CommandRunner.CommandInvocation ci = cr.getCommandInvocation("set", report);
        ParameterMap map = new ParameterMap();
        String monitorIns = insName+".monitoring-service.module-monitoring-levels.jvm=HIGH";
        map.add("DEFAULT", monitorIns);
        ci.parameters(map);
        ci.execute();

    }

    @Override
    public MetricAttribute[] getAttributes() {
        return attributes;
    }

    public long getMax() {
        return max;
    }

    @Override
    public MetricNode[] getChildren() {
        return new MetricNode[0];
    }

    private class MaxSizeAttribute
            implements MetricAttribute {

        @Override
        public String getName() {
            return "maxMemory";
        }

        @Override
        public Long getValue() {
            return max;
        }
    }

    public static class MemoryStat {

        private long used;

        private long committed;

        MemoryStat(long used, long committed) {
            this.used = used;
            this.committed = committed;
        }

        public long getUsed() {
            return used;
        }

        public long getCommitted() {
            return committed;
        }

        public String toString() {
            return "used=" + used + "; committed=" + committed;
        }
    }

    public static class JVMInstanceMemoryHolder  {

        String hostName= "localhost";
        String hostPort = "4848";
        String instanceName = null;

        long maxHeap = -1;
        TabularMetricHolder<MemoryStat> table;

        public   JVMInstanceMemoryHolder(String host, String port, String instName){
            hostName = host;
            hostPort = port;
            instanceName = instName;
            table = new TabularMetricHolder<MemoryStat>("heap", MemoryStat.class);

        }

        public void gatherMetric() {
            String jvmMemURL = baseURL + instanceName +"/"+  jvm_memory ;
            try {
                Map<String, Object> res = CollectMetricData.getRestData(jvmMemURL);
                if (res == null ) return;

                if (maxHeap == -1) {
                    // should only do this once
                    Map<String, Object> jvmMemMax = (Map) res.get(maxheapsize);
                    maxHeap = (Integer) jvmMemMax.get("count");
                }

                MemoryStat memStat = new MemoryStat(
                        (Integer)((Map) res.get(heap_used)).get("count"),
                        (Integer) ((Map) res.get(heap_commited)).get("count"));
                table.add(System.currentTimeMillis(), memStat);
            } catch (Exception ex)  {
                _logger.log(Level.WARNING, "Exception while collecting metric " + ex);
                _logger.log(Level.FINE, "Exception while collecting metric", ex);
            }
    }

    }
}
