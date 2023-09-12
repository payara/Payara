package fish.payara.microprofile.metrics.impl;

public class HistogramAdapter {
    
    private Double[] percentiles;
    
    private Double[] percentilesFromConfig;

    public HistogramAdapter(Double[] percentiles, Double[] percentilesFromConfig) {
        this.percentiles = percentiles;
        this.percentilesFromConfig = percentilesFromConfig;
    }

    public Double[] percentileValues() {
        if(percentilesFromConfig != null && percentilesFromConfig.length > 0) {
            return percentilesFromConfig;
        } else {
            return percentiles;
        }
    }
}
