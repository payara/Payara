package fish.payara.monitoring.collect;

public class HealthMonitorDataSource implements MonitoringDataSource {

    @Override
    public void collect(MonitoringDataCollector collector) {
//        MonitoringDataCollector nsCollector = collector.in("health-check");
//        nsCollector.collect("size", healthChecks.size());
//        for (Entry<String, Set<HealthCheck>> entry : healthChecks.entrySet()) {
//            nsCollector.tag("entry", entry.getKey()).collect("size", entry.getValue().size());
//        }
    }

}
