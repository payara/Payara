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
package org.glassfish.elasticity.metrics.sessions;

import org.glassfish.elasticity.api.AbstractMetricGatherer;
import org.glassfish.elasticity.metric.MetricAttribute;
import org.glassfish.elasticity.metric.MetricNode;
import org.glassfish.elasticity.util.TabularMetricHolder;
import javax.inject.Inject;

import org.glassfish.hk2.Services;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;

import org.glassfish.flashlight.datatree.TreeNode;
import org.glassfish.flashlight.MonitoringRuntimeDataRegistry;
import org.glassfish.external.statistics.RangeStatistic;

import java.util.concurrent.TimeUnit;


/**
 * @author Jennifer.Chou@Oracle.Com
 */
@Service(name="session_count")
public class ActiveSessionsMetricHolder
    extends AbstractMetricGatherer
    implements MetricNode, PostConstruct {

    static final String _NAME = "session_count";
    private String instanceName;
    private static final String WEB_SESSION_ACTIVESESSIONSCURRENT = "web.session.activesessionscurrent";

    TabularMetricHolder<ActiveSessionsStat> table = null;

    MetricAttribute[] attributes;
    
    TreeNode rootNode = null;
    
    @Inject
    Services services;

    @Override
    public void postConstruct() {
        this.table = new TabularMetricHolder<ActiveSessionsStat>("activeSessions", ActiveSessionsStat.class);
        this.attributes = new MetricAttribute[] {new InstanceAttribute(), table};
        this.instanceName = System.getProperty("com.sun.aas.instanceName");
        MonitoringRuntimeDataRegistry monitoringRegistry = services.forContract(MonitoringRuntimeDataRegistry.class).get();
        rootNode = monitoringRegistry.get(this.instanceName);
    }

    @Override
    public void gatherMetric() {
        //TreeNode activeSessionsNode = rootNode.getNode("applications.*.*.activesessionscurrent");
        if (rootNode != null) {
            TreeNode activeSessionsNode = rootNode.getNode(WEB_SESSION_ACTIVESESSIONSCURRENT);
            
            if (activeSessionsNode != null) {
                Object value = activeSessionsNode.getValue();
                
                if (value != null) {
                    if (value instanceof RangeStatistic) {
                        RangeStatistic statisticObject = (RangeStatistic) value;
                        
                        table.add(System.currentTimeMillis(), new ActiveSessionsStat(
                                statisticObject.getHighWaterMark(),
                                statisticObject.getLastSampleTime(),
                                statisticObject.getDescription(),
                                statisticObject.getUnit(),
                                statisticObject.getName(),
                                statisticObject.getStartTime(),
                                statisticObject.getCurrent(),
                                statisticObject.getLowWaterMark()));

                        //Iterator<TabularMetricEntry<ActiveSessionsStat>> iter = table.iterator(10, TimeUnit.SECONDS);
                        //while (iter.hasNext()) {
                        //    TabularMetricEntry<ActiveSessionsStat> tme = iter.next();
                        //System.out.println("SessionCountMetricHolder Gathered metric: " + tme.getTimestamp() + " " + tme.getV());
                        //}
                    }
                }
            }
        }
    }
    
    @Override
    public void purgeDataOlderThan(int time, TimeUnit unit) {
        table.purgeEntriesOlderThan(time, unit);
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
        return new MetricNode[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
    
    private class InstanceAttribute
        implements MetricAttribute {

        @Override
        public String getName() {
            return "instance";
        }

        @Override
        public Object getValue() {
            return instanceName;
        }
    }

    private class ActiveSessionsStat {

        private long highWaterMark;
        private long lastSampleTime;
        private String description;
        private String unit;
        private String name;
        private long startTime;
        private long current;
        private long lowWaterMark;

        ActiveSessionsStat(long highWaterMark, long lastSampleTime, String description, 
                String unit, String name, long startTime, long current, long lowWaterMark) {
            this.highWaterMark = highWaterMark;
            this.lastSampleTime = lastSampleTime;
            this.description = description;
            this.unit = unit;
            this.name = name;
            this.startTime = startTime;
            this.current = current;
            this.lowWaterMark = lowWaterMark;
        }

        public long getHighWatermark() {
            return highWaterMark;
        }

        public long getLastSampleTime() {
            return lastSampleTime;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getUnit() {
            return unit;
        }
        
        public String getName() {
            return name;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getCurrent() {
            return current;
        }
        
        public long getLowWaterMark() {
            return lowWaterMark;
        }

        public String toString() {
            return "highWaterMark=" + highWaterMark + "; lastSampleTime=" + lastSampleTime + "; description=" + description + "; unit=" + unit +
                    "; name=" +name + "; startTime=" + startTime + "; current="+ current + "; + lowWaterMark=" + lowWaterMark;
        }
    }
}
