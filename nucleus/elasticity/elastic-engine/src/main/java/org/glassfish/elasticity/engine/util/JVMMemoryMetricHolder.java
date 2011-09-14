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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
@Service(name="jvm_memory")
public class JVMMemoryMetricHolder
    implements MetricNode, MetricGatherer, PostConstruct {

    static final String _NAME = "jvm_memory";

    public long max;

    TabularMetricHolder<MemoryStat> table;

    MetricAttribute[] attributes;

    MemoryMXBean memBean;

    @Override
    public void postConstruct() {
        this.table = new TabularMetricHolder<MemoryStat>("heap", MemoryStat.class);

        this.attributes = new MetricAttribute[] {new MaxSizeAttribute(), table};
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

        Iterator<TabularMetricEntry<MemoryStat>> iter = table.iterator(10, TimeUnit.SECONDS);
        while (iter.hasNext()) {
            TabularMetricEntry<MemoryStat> tme = iter.next();
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
        return new MetricNode[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    private class MaxSizeAttribute
        implements MetricAttribute {

        @Override
        public String getName() {
            return "maxMemory";
        }

        @Override
        public Object getValue() {
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
