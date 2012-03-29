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
package org.glassfish.elasticity.engine.util;

import org.glassfish.elasticity.api.MetricGatherer;
import org.glassfish.elasticity.metric.MetricAttribute;
import org.glassfish.elasticity.metric.MetricNode;
import org.glassfish.elasticity.metric.TabularMetricAttribute;
import org.glassfish.elasticity.metric.TabularMetricEntry;
import org.glassfish.elasticity.util.TabularMetricHolder;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.Client;
import javax.ws.rs.core.MediaType;
import com.sun.enterprise.config.serverbeans.Clusters;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Server;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
@Service(name = "jvm_memory")
public class JVMMemoryMetricHolder
        implements MetricNode, MetricGatherer, PostConstruct {

    static final String _NAME = "jvm_memory";

    public Long max;

    private static final String baseURL = "http://localhost:4848/monitoring/domain/";
    private static final String jvm_memory = "jvm/memory";
    private static final String heap_used = "usedheapsize-count";
    private static final String heap_commited = "committedheapsize-count";
    private static final String maxheapsize = "maxheapsize-count";
    private static final String RESPONSE_TYPE = MediaType.APPLICATION_JSON;

    // need to store instance name, instance port, instance host and data for the instance
    HashMap<String, Object>  instancesTable= null;

    MetricAttribute[] attributes;

    MemoryMXBean memBean;

    private boolean debug;

    @Inject
    private Clusters clusters;

    @Override
    public void postConstruct() {

//        this.max = memBean.getHeapMemoryUsage().getMax();
    }

    private void init() {
       //create a hash map with all instance names in this service
        //each instance is a TabularMetricHolder for data with name, host and port

        // need to get the service name  hard code for now
        Cluster cluster = clusters.getCluster("c1");
        if (cluster == null) {
            System.out.println("No cluster called "+ "c1");
            return;
        }
        List<Server> serverList = cluster.getInstances();
        if (serverList == null) {
            System.out.println("no server instances found.");
            return;
        }
        // until we have metric gatherer factories init this way
        if (instancesTable == null ){
            instancesTable= new HashMap<String, Object>();
            for (Server server : serverList) {
                Map<String , Object> instanceInfo= new HashMap<String, Object>();
                String instanceName = server.getName();
                instanceInfo.put("name",instanceName);
                // hardcode for now.
                instanceInfo.put("hostname","localhost");
                instanceInfo.put("port","4848"); // or make it an Integer
                TabularMetricHolder<MemoryStat> table = new TabularMetricHolder<MemoryStat>("heap", MemoryStat.class);
                instanceInfo.put("table",table);
                MetricAttribute[] attributes = new MetricAttribute[]{new MaxSizeAttribute(), table};
                instanceInfo.put("attributes", attributes);
                instanceInfo.put("max",0); //can we add this later
                instancesTable.put("name",instanceInfo);
            }
        }

    }

    @Override
    public String getSchedule() {
        return "10s";
    }

    @Override
    public void gatherMetric() {
        //only because we don't have metric factories and callback
         init();
        //  for each instance in the service get the data
        // use another class so can be done concurrently if needed
        for (String name : instancesTable.keySet()) {
            Map<String , Object> instanceInfo= (Map)instancesTable.get(name);
            String instanceName = (String)instanceInfo.get("name");
            String jvmMemURL = baseURL + instanceName +"/"+  jvm_memory ;
            //catch exception
            try {
                Map<String, Object> res = CollectMetricData.getRestData(jvmMemURL);
                if (res == null ) return;
                Map<String, Object> jvmMemHeap = (Map) res.get(heap_used);
                Integer heapUsed = (Integer)jvmMemHeap.get("count");

                Map<String, Object> jvmMemCommited = (Map) res.get(heap_commited);
                Integer commitedUsed = (Integer)jvmMemCommited.get("count");
                // should only do this once
                Map<String, Object> jvmMemMax = (Map) res.get(maxheapsize);
                Integer jvmMax = (Integer)jvmMemMax.get("count");
                instanceInfo.put("max",jvmMax);
                TabularMetricHolder<MemoryStat> table= (TabularMetricHolder)instanceInfo.get("table");
                table.add(System.currentTimeMillis(), new MemoryStat(heapUsed.longValue(), commitedUsed.longValue()));
                instanceInfo.put("table", table);
                System.out.println("Instance name " + instanceName + " Used heap " + heapUsed);
            } catch (Exception ex)  {

            }
        }

/*
        if (debug) {
            Iterator<TabularMetricEntry<MemoryStat>> iter = table.iterator(2 * 60 * 60, TimeUnit.SECONDS);
            int count = 1;
            TabularMetricEntry<MemoryStat> lastEntry = null;
            while (iter.hasNext()) {
                TabularMetricEntry<MemoryStat> tme = iter.next();
                if (count == 1) {
                    System.out.println(" " + count + " | " + new Date(tme.getTimestamp()) + " | " + tme.getV() + "; ");
                }
                lastEntry = tme;
                count++;
            }
            if (lastEntry != null) {
                System.out.println(" " + count + " | " + new Date(lastEntry.getTimestamp()) + " | " + lastEntry.getV() + "; ");
            }
            System.out.println("So far collected: " + count);
        }
        */
    }

    @Override
    public void purgeDataOlderThan(int time, TimeUnit unit) {
        //go to each instance and purge old data
        for (String name : instancesTable.keySet()) {
            Map<String , Object> instanceInfo= (Map)instancesTable.get(name);
            TabularMetricHolder<MemoryStat> table= (TabularMetricHolder)instanceInfo.get("table");
            table.purgeEntriesOlderThan(time, unit);
            instanceInfo.put("table", table);
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public String getName() {
        return _NAME;
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

    private class MemoryStat {

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
}
