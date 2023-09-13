package fish.payara.microprofile.metrics.impl;

public abstract class AbstractConfigAdapter {
    Double[] percentiles;

    Double[] percentilesFromConfig;
    
    Double[] bucketValues;
    
    Double[] bucketValuesFromConfig;
    public Double[] percentileValues() {
        if(percentilesFromConfig != null && percentilesFromConfig.length > 0) {
            return percentilesFromConfig;
        } else {
            return percentiles;
        }
    }
    
    public Double[] bucketValues() {
        if(bucketValuesFromConfig != null && bucketValuesFromConfig.length > 0) {
            return bucketValuesFromConfig;
        } else {
            return bucketValues;
        }
    }

    public Double[] getPercentiles() {
        return percentiles;
    }

    public void setPercentiles(Double[] percentiles) {
        this.percentiles = percentiles;
    }

    public Double[] getPercentilesFromConfig() {
        return percentilesFromConfig;
    }

    public void setPercentilesFromConfig(Double[] percentilesFromConfig) {
        this.percentilesFromConfig = percentilesFromConfig;
    }

    public Double[] getBucketValues() {
        return bucketValues;
    }

    public void setBucketValues(Double[] bucketValues) {
        this.bucketValues = bucketValues;
    }

    public Double[] getBucketValuesFromConfig() {
        return bucketValuesFromConfig;
    }

    public void setBucketValuesFromConfig(Double[] bucketValuesFromConfig) {
        this.bucketValuesFromConfig = bucketValuesFromConfig;
    }
}
