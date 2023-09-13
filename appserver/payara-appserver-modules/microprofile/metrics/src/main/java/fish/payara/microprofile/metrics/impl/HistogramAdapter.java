package fish.payara.microprofile.metrics.impl;

public class HistogramAdapter extends AbstractConfigAdapter {
    
    public HistogramAdapter() {
        this.percentiles = null;
        this.percentilesFromConfig = null;
        this.bucketValuesFromConfig = null;
        this.bucketValues = null;
    }
    
    public HistogramAdapter(Double[] percentiles, Double[] percentilesFromConfig, 
                            Double[] bucketValues, Double[] bucketValuesFromConfig) {
        this.percentiles = percentiles;
        this.percentilesFromConfig = percentilesFromConfig;
        this.bucketValues = bucketValues;
        this.bucketValuesFromConfig = bucketValuesFromConfig;
    }
    
    


}
