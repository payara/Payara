package fish.payara.microprofile.metrics.impl;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

public class MetricsConfigParserUtil {
    
    private static final Logger logger = Logger.getLogger(MetricsConfigParserUtil.class.getName());
    
    private static final String PERCENTILE_NAME_SEPARATOR = ";";
    
    private static final String PERCENTILE_KEY_VALUE_SEPARATOR = "=";

    private static final String PERCENTILE_VALUE_SEPARATOR = ",";
    public static Collection<MetricCustomPercentile> parsePercentile(String percentileProperty) {
        ArrayDeque<MetricCustomPercentile> metricPercentileCollection = new ArrayDeque<>();
        if(percentileProperty == null || percentileProperty.length() == 0) {
            return null;
        }
        MetricCustomPercentile customPercentile = null;
        String[] valuePairs = percentileProperty.split(PERCENTILE_NAME_SEPARATOR);
        for(String nameValue : valuePairs) {
            String[] resultKeyValueSplit = nameValue.split(PERCENTILE_KEY_VALUE_SEPARATOR);
            String metricName = resultKeyValueSplit[0];
            if(resultKeyValueSplit.length == 1) {
                //evaluate when no value, disabled
            } else {
                Double[] percentileValues = Arrays.asList(resultKeyValueSplit[1].split(PERCENTILE_VALUE_SEPARATOR)).stream()
                        .map(MetricsConfigParserUtil::evaluatePercentileValue)
                        .filter(d -> d != null && d >= 0.0 && d <= 1.0).toArray(Double[]::new);
                Arrays.sort(percentileValues);
                customPercentile = new MetricCustomPercentile(metricName, percentileValues);
            }
            metricPercentileCollection.addFirst(customPercentile);
        }
        return metricPercentileCollection;
    }
    
    public static Double evaluatePercentileValue(String percentile) {
        if(percentile.matches("[0][.][0-9]+")) {
            return Double.parseDouble(percentile);
        } else {
            logger.info(String.format("Error when trying to read property %s with %s name", HistogramImpl.METRIC_PERCENTILES_PROPERTY, percentile));
            return null;
        }
    }
}
