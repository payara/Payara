package fish.payara.microprofile.metrics.impl;

public class MetricsCustomPercentile extends PropertyConfig {
    
    private Double[] percentiles;

    public MetricsCustomPercentile(String name, Double[] percentiles) {
        this.metricName = name;
        this.percentiles = percentiles;
    }

    public Double[] getPercentiles() {
        return percentiles;
    }

    public void setPercentiles(Double[] percentiles) {
        this.percentiles = percentiles;
    }
}
