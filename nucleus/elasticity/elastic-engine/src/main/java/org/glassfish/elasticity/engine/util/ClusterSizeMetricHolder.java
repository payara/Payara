/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.elasticity.group.GroupMemberEventListener;
import org.glassfish.elasticity.metric.MetricAttribute;
import org.glassfish.elasticity.metric.MetricNode;
import org.glassfish.elasticity.util.TabularMetricHolder;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.annotations.Inject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

import com.sun.logging.LogDomains;

import com.sun.enterprise.config.serverbeans.*;


/**
 * @author Chris.Kasso@Oracle.Com
 */
@Service(name="cluster_instance_size")
public class ClusterSizeMetricHolder
    implements MetricNode, MetricGatherer, PostConstruct {

    @Inject
    Domain domain;

    static final String _NAME = "cluster_instance_size";

    private AtomicInteger currentSize = new AtomicInteger(0);

    public long max = 0;

    Map<String,InstanceCountMetricNode> clusterMap = 
            new HashMap<String,InstanceCountMetricNode>();


    MetricAttribute[] attributes;

    private static final Logger logger =
            LogDomains.getLogger(ClusterSizeMetricHolder.class, LogDomains.EJB_LOGGER);

    @Override
    public void postConstruct() {
        this.attributes = new MetricAttribute[] {new CurrentSizeAttribute()};
    }

    @Override
    public String getSchedule() {
        return "10s";
    }

    public int getCurrentSize() {
        return currentSize.get();
    }

    @Override
    public void gatherMetric() {

        List<Cluster> clusterList = null;

        Clusters clusters = domain.getClusters();
        clusterList = clusters.getCluster();

        // There may be no clusters...
        if (clusterList.size() < 1) {
            clusterMap.clear();
            return;
        }

        // Deal with any clusters that have been deleted.
        removeMissingClusters(clusterMap, clusterList);

        // XXX: This is counting the instance regardless of whether the 
        //      instance is running.   That may not be the behavior we 
        //      want.
        for (Cluster cluster : clusterList) {
            InstanceCountMetricNode instanceCountMetricNode;

            String clusterName = cluster.getName();
            List<Server> servers = cluster.getInstances();

            // See if the cluster is in the list
            if (clusterMap.containsKey(clusterName)) {
                instanceCountMetricNode = clusterMap.get(clusterName);
            } else {
                instanceCountMetricNode = new InstanceCountMetricNode(clusterName);
                clusterMap.put(clusterName, instanceCountMetricNode);
            }

            TabularMetricHolder<ClusterInstanceCountStat> clusterSizeStat = 
                    (TabularMetricHolder<ClusterInstanceCountStat>) instanceCountMetricNode.getMetricHolder();

            if (servers.isEmpty()) {
                clusterSizeStat.add(System.currentTimeMillis(), new ClusterInstanceCountStat(0));
            } else {
                clusterSizeStat.add(System.currentTimeMillis(), new ClusterInstanceCountStat(servers.size()));
                currentSize.set(servers.size());
            }
        }
    }

    // With each invocation of the metrics gatherer the list of clusters
    // may change.  A cluster that was there before may no longer be there.
    // When that happens it must be removed from the clusterMap.
    private void removeMissingClusters(
            Map<String,InstanceCountMetricNode> clusterMap, 
            List<Cluster> newClusterList) {
        Set<String> oldClusterNames = new HashSet<String>(clusterMap.size());
        Set<String> newClusterNames = new HashSet<String>(newClusterList.size());

        for (Cluster cluster : newClusterList) {
            newClusterNames.add(cluster.getName());
        }

        for (String clusterName : clusterMap.keySet())  {
            oldClusterNames.add(clusterName);
        }

        oldClusterNames.removeAll(newClusterNames);

        for (String clusterName : oldClusterNames) {
            clusterMap.remove(clusterName);
        }
    }

    @Override
    public void purgeDataOlderThan(int time, TimeUnit unit) {
        for (InstanceCountMetricNode instanceCountMetricNode  : clusterMap.values())
            instanceCountMetricNode.getMetricHolder().purgeEntriesOlderThan(time, unit);
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

    @Override
    public MetricNode[] getChildren() {

        if (clusterMap.isEmpty())
            return new MetricNode[0]; 

        return clusterMap.values().toArray(new MetricNode[0]);
    }

    private class CurrentSizeAttribute
            implements MetricAttribute {

        @Override
        public String getName() {
            return "currentSize";
        }

        @Override
        public Integer getValue() {
            return currentSize.get();
        }
    }

    private class InstanceCountMetricNode implements MetricNode {

        MetricAttribute[] attrs;
        TabularMetricHolder<ClusterInstanceCountStat> table;

        private String _name;

        public InstanceCountMetricNode(String name) {
            _name = name;
            table = new TabularMetricHolder<ClusterInstanceCountStat>("count",
                        ClusterInstanceCountStat.class);
            attrs = new MetricAttribute[] { table };
        }

        public TabularMetricHolder<ClusterInstanceCountStat> getMetricHolder() {
            return table;
        }

        @Override
        public String getName() {
            return _name;
        }

        @Override
        public MetricAttribute[] getAttributes() {
            return attrs;
        }

        @Override
        public MetricNode[] getChildren() {
            return new MetricNode[0];
        }
    }

    private class ClusterInstanceCountStat {

        private long size;

        ClusterInstanceCountStat(long size) {
            this.size = size;
        }

        public long getInstanceCount() {
            return size;
        }

        public String toString() {
            return "instance count =" + size + ";";
        }
    }

    public int getSize() {
        return currentSize.get();
    }

}