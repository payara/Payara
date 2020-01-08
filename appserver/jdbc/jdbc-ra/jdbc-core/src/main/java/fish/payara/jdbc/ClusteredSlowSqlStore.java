package fish.payara.jdbc;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.glassfish.api.jdbc.SQLTraceRecord;
import org.jvnet.hk2.annotations.Service;

import fish.payara.monitoring.collect.MonitoringData;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.nucleus.store.ClusteredStore;

@Service
public class ClusteredSlowSqlStore implements MonitoringDataSource {

    private static final class SlowSQLEntry implements Serializable {
        final long thresholdMillis;
        final SQLTraceRecord trace;
        final String sql;

        SlowSQLEntry(long thresholdMillis, SQLTraceRecord trace, String sql) {
            this.thresholdMillis = thresholdMillis;
            this.trace = trace;
            this.sql = sql;
        }
    }

    private static final class PoolStatistics {

        static final PoolStatistics ZERO = new PoolStatistics(0, 0L, 0L, 0L);

        final int queryCount;
        final long totalExecutionTime;
        final long maxExecutionTime;
        final long minExecutionTime;

        public PoolStatistics(int queryCount, long totalExecutionTime, long maxExecutionTime, long minExecutionTime) {
            this.queryCount = queryCount;
            this.totalExecutionTime = totalExecutionTime;
            this.maxExecutionTime = maxExecutionTime;
            this.minExecutionTime = minExecutionTime;
        }

        PoolStatistics updatedBy(SQLTraceRecord record) {
            return new PoolStatistics( //
                    queryCount + 1, //
                    totalExecutionTime + record.getExecutionTime(), //
                    Math.max(maxExecutionTime, record.getExecutionTime()), //
                    Math.min(minExecutionTime, record.getExecutionTime()));
        }
    }

    @Inject
    private ClusteredStore clusteredStore;
    private final Map<String, PoolStatistics> poolStatisticsByPoolName = new ConcurrentHashMap<>();

    void onSlowExecutionTime(long thresholdTime, SQLTraceRecord record, String sql) {
        poolStatisticsByPoolName.compute(record.getPoolName(), //
                (key, stats) -> (stats == null ? PoolStatistics.ZERO : stats).updatedBy(record));
    }

    @Override
    @MonitoringData(ns = "sql")
    public void collect(MonitoringDataCollector collector) {
        for (Entry<String, PoolStatistics> statsEntry : poolStatisticsByPoolName.entrySet()) {
            PoolStatistics stats = statsEntry.getValue();
            collector.group(statsEntry.getKey())
            .collect("QueryCount", stats.queryCount)
            .collect("TotalExecutionTime", stats.totalExecutionTime)
            .collect("AvgExecutionTime", stats.queryCount / stats.totalExecutionTime)
            .collect("MaxExecutionTime", stats.maxExecutionTime)
            .collect("MinExecutionTime", stats.minExecutionTime)
            ;
        }
    }

}
