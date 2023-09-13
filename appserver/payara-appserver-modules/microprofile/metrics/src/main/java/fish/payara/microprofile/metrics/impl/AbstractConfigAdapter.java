package fish.payara.microprofile.metrics.impl;

public abstract class AbstractConfigAdapter {
    Double[] percentilesFromConfig;
    Double[] bucketValuesFromConfig;
    public Double[] percentileValues() {
        if(percentilesFromConfig != null && percentilesFromConfig.length > 0) {
            return percentilesFromConfig;
        } else {
            Double[] percentiles = {0.5, 0.75, 0.95, 0.98, 0.99, 0.999};
            return percentiles;
        }
    }
    
    public Double[] bucketValues() {
        if(bucketValuesFromConfig != null && bucketValuesFromConfig.length > 0) {
            return bucketValuesFromConfig;
        } else {
            return new Double[0];
        }
    }
    

    public Double[] getPercentilesFromConfig() {
        return percentilesFromConfig;
    }

    public void setPercentilesFromConfig(Double[] percentilesFromConfig) {
        this.percentilesFromConfig = percentilesFromConfig;
    }

    public Double[] getBucketValuesFromConfig() {
        return bucketValuesFromConfig;
    }

    public void setBucketValuesFromConfig(Double[] bucketValuesFromConfig) {
        this.bucketValuesFromConfig = bucketValuesFromConfig;
    }
}
