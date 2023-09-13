package fish.payara.microprofile.metrics.impl;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

public class MetricsConfigParserUtil {
    
    private static final Logger logger = Logger.getLogger(MetricsConfigParserUtil.class.getName());
    
    private static final String PROPERTY_NAME_SEPARATOR = ";";
    
    private static final String PROPERTY_KEY_VALUE_SEPARATOR = "=";

    private static final String PROPERTY_VALUE_SEPARATOR = ",";
    public static Collection<MetricsCustomPercentile> parsePercentile(String percentileProperty) {
        ArrayDeque<MetricsCustomPercentile> metricPercentileCollection = new ArrayDeque<>();
        if(percentileProperty == null || percentileProperty.length() == 0) {
            return null;
        }
        MetricsCustomPercentile customPercentile = null;
        String[] valuePairs = percentileProperty.split(PROPERTY_NAME_SEPARATOR);
        for(String nameValue : valuePairs) {
            String[] resultKeyValueSplit = nameValue.split(PROPERTY_KEY_VALUE_SEPARATOR);
            String metricName = resultKeyValueSplit[0];
            if(resultKeyValueSplit.length == 1) {
                //evaluate when no value, disabled
            } else {
                Double[] percentileValues = Arrays.asList(resultKeyValueSplit[1].split(PROPERTY_VALUE_SEPARATOR)).stream()
                        .map(MetricsConfigParserUtil::evaluatePercentileValue)
                        .filter(d -> d != null && d >= 0.0 && d <= 1.0).toArray(Double[]::new);
                Arrays.sort(percentileValues);
                customPercentile = new MetricsCustomPercentile(metricName, percentileValues);
            }
            metricPercentileCollection.addFirst(customPercentile);
        }
        return metricPercentileCollection;
    }
    
    public static Collection<MetricsCustomBucket> parseHistogramBuckets(String histogramBucketsProperty) {
        ArrayDeque<MetricsCustomBucket> metricHistogramCollection = new ArrayDeque<>();
        if(histogramBucketsProperty == null || histogramBucketsProperty.length() == 0) {
            return null;
        }
        
        MetricsCustomBucket customBucket = null;
        String[] valuePairs = histogramBucketsProperty.split(PROPERTY_NAME_SEPARATOR);
        for (String nameValue: valuePairs) {
            String[] resultKeyValueSplit = nameValue.split(PROPERTY_KEY_VALUE_SEPARATOR);
            String metricName = resultKeyValueSplit[0];
            if(resultKeyValueSplit.length == 1) {
                //evaluate when no value, disabled
            } else {
                Double[] bucketValues = Arrays.asList(resultKeyValueSplit[1].split(PROPERTY_VALUE_SEPARATOR)).stream()
                        .map(MetricsConfigParserUtil::evaluateBucketValue)
                        .filter(d -> d != null).toArray(Double[]::new);
                Arrays.sort(bucketValues);
                customBucket = new MetricsCustomBucket(metricName, bucketValues);
            }
            metricHistogramCollection.addFirst(customBucket);
        }
        return metricHistogramCollection;
    }
    
    public static Double evaluatePercentileValue(String percentile) {
        if(percentile.matches("[0][.][0-9]+")) {
            return Double.parseDouble(percentile);
        } else {
            logger.info(String.format("Error when trying to read property %s with %s name", HistogramImpl.METRIC_PERCENTILES_PROPERTY, percentile));
            return null;
        }
    }

    public static Double evaluateBucketValue(String bucket) {
        if(bucket.matches("[0-9]+[.]*[0-9]*")) {
            return Double.parseDouble(bucket);
        } else {
            logger.info(String.format("Error when trying to read property %s with %s name", HistogramImpl.METRIC_HISTOGRAM_BUCKETS_PROPERTY, bucket));
            return null;
        }
    }
}
