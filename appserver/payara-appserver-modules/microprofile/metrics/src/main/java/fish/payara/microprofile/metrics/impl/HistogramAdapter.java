package fish.payara.microprofile.metrics.impl;

public class HistogramAdapter extends AbstractConfigAdapter {
    
    public HistogramAdapter() {
        this.percentilesFromConfig = null;
        this.bucketValuesFromConfig = null;
    }

}
