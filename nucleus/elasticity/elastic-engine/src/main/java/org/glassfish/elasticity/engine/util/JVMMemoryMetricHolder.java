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
