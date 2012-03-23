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
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
@Service(name = "jvm_memory")
public class JVMMemoryMetricHolder
        implements MetricNode, MetricGatherer, PostConstruct {

    static final String _NAME = "jvm_memory";

    public Long max;

    TabularMetricHolder<MemoryStat> table;

    MetricAttribute[] attributes;

    MemoryMXBean memBean;

    private boolean debug;

    @Override
    public void postConstruct() {
        this.table = new TabularMetricHolder<MemoryStat>("heap", MemoryStat.class);

        this.attributes = new MetricAttribute[]{new MaxSizeAttribute(), table};
        this.memBean = ManagementFactory.getMemoryMXBean();
        this.max = memBean.getHeapMemoryUsage().getMax();
    }

    @Override
    public String getSchedule() {
        return "10s";
    }

    @Override
    public void gatherMetric() {
        MemoryUsage memUsage = memBean.getHeapMemoryUsage();
        table.add(System.currentTimeMillis(), new MemoryStat(memUsage.getUsed(), memUsage.getCommitted()));

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
