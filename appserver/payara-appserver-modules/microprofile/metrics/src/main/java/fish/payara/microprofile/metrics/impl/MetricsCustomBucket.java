package fish.payara.microprofile.metrics.impl;

public class MetricsCustomBucket extends PropertyConfig {
    private Double[] buckets;

    public MetricsCustomBucket(String name, Double[] buckets) {
        this.metricName = name;
        this.buckets = buckets;
    }
    
    public Double[] getBuckets() {
        return buckets;
    }

    public void setBuckets(Double[] buckets) {
        this.buckets = buckets;
    }
}
