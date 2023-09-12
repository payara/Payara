package fish.payara.microprofile.metrics.impl;

public class MetricCustomPercentile {
    
    private String name;
    
    private Double[] percentiles;

    public MetricCustomPercentile(String name, Double[] percentiles) {
        this.name = name;
        this.percentiles = percentiles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double[] getPercentiles() {
        return percentiles;
    }

    public void setPercentiles(Double[] percentiles) {
        this.percentiles = percentiles;
    }
}
