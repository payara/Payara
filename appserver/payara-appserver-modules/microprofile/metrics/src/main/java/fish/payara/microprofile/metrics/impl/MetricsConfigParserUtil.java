/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.metrics.impl;

import fish.payara.microprofile.metrics.cdi.MetricUtils;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.Config;

import static fish.payara.microprofile.metrics.impl.MetricRegistryImpl.*;

public class MetricsConfigParserUtil {
    
    private static final Logger logger = Logger.getLogger(MetricsConfigParserUtil.class.getName());
    
    private static final String PROPERTY_NAME_SEPARATOR = ";";
    
    private static final String PROPERTY_KEY_VALUE_SEPARATOR = "=";

    private static final String PROPERTY_VALUE_SEPARATOR = ",";

    public static Collection<MetricsCustomPercentiles> parsePercentile(String percentileProperty) {
        ArrayDeque<MetricsCustomPercentiles> metricPercentileCollection = new ArrayDeque<>();
        if (percentileProperty == null || percentileProperty.length() == 0) {
            return null;
        }
        MetricsCustomPercentiles customPercentile = null;
        String[] valuePairs = percentileProperty.split(PROPERTY_NAME_SEPARATOR);
        for (String nameValue : valuePairs) {
            String[] resultKeyValueSplit = nameValue.split(PROPERTY_KEY_VALUE_SEPARATOR);
            String metricName = resultKeyValueSplit[0];
            if (resultKeyValueSplit.length == 1) {
                customPercentile = new MetricsCustomPercentiles(metricName, true);
            } else {
                Double[] percentileValues = Arrays.asList(resultKeyValueSplit[1].split(PROPERTY_VALUE_SEPARATOR)).stream()
                        .map(MetricsConfigParserUtil::evaluatePercentileValue)
                        .filter(d -> d != null && d >= 0.0 && d <= 1.0).toArray(Double[]::new);
                Arrays.sort(percentileValues);
                customPercentile = new MetricsCustomPercentiles(metricName, percentileValues);
            }
            metricPercentileCollection.addFirst(customPercentile);
        }
        return metricPercentileCollection;
    }

    public static Collection<MetricsCustomBuckets> parseHistogramBuckets(String histogramBucketsProperty) {
        ArrayDeque<MetricsCustomBuckets> metricHistogramCollection = new ArrayDeque<>();
        if (histogramBucketsProperty == null || histogramBucketsProperty.length() == 0) {
            return null;
        }

        MetricsCustomBuckets customBucket = null;
        String[] valuePairs = histogramBucketsProperty.split(PROPERTY_NAME_SEPARATOR);
        for (String nameValue : valuePairs) {
            String[] resultKeyValueSplit = nameValue.split(PROPERTY_KEY_VALUE_SEPARATOR);
            String metricName = resultKeyValueSplit[0];
            if (resultKeyValueSplit.length == 1) {
                //evaluate when no value, disabled
            } else {
                Double[] bucketValues = Arrays.asList(resultKeyValueSplit[1].split(PROPERTY_VALUE_SEPARATOR)).stream()
                        .map(MetricsConfigParserUtil::evaluateHistogramBucketValue)
                        .filter(d -> d != null).toArray(Double[]::new);
                Arrays.sort(bucketValues);
                customBucket = new MetricsCustomBuckets(metricName, bucketValues);
            }
            metricHistogramCollection.addFirst(customBucket);
        }
        return metricHistogramCollection;
    }

    public static Collection<MetricsCustomBuckets> parseTimerBuckets(String timerBucketsProperty) {
        ArrayDeque<MetricsCustomBuckets> metricTimerCollection = new ArrayDeque<>();
        if (timerBucketsProperty == null || timerBucketsProperty.length() == 0) {
            return null;
        }

        MetricsCustomBuckets customBucket = null;
        String[] valuePairs = timerBucketsProperty.split(PROPERTY_NAME_SEPARATOR);
        for (String nameValue : valuePairs) {
            String[] resultKeyValueSplit = nameValue.split(PROPERTY_KEY_VALUE_SEPARATOR);
            String metricName = resultKeyValueSplit[0];
            if (resultKeyValueSplit.length != 1) {
                Duration[] bucketValues = Arrays.asList(resultKeyValueSplit[1].split(PROPERTY_VALUE_SEPARATOR)).stream()
                        .map(MetricsConfigParserUtil::evaluateTimerBucketValue)
                        .filter(d -> d != null).toArray(Duration[]::new);
                customBucket = new MetricsCustomBuckets(metricName,
                        Arrays.stream(bucketValues).mapToDouble(Duration::toNanos).boxed().toArray(Double[]::new));
            }
            metricTimerCollection.addFirst(customBucket);
        }
        return metricTimerCollection;
    }

    public static Collection<MetricsCustomPercentiles> processPercentileMap(String appName) {
        Config config = MetricUtils.getConfigProvider();
        if (config != null) {
            Optional<String> customPercentiles = config.getOptionalValue(METRIC_PERCENTILES_PROPERTY, String.class);
            return (customPercentiles.isPresent()) ? MetricsConfigParserUtil.parsePercentile(customPercentiles.get()) : null;
        }
        return null;
    }

    public static Double evaluatePercentileValue(String percentile) {
        if (percentile.matches("[0][.][0-9]+")) {
            return Double.parseDouble(percentile);
        } else {
            logger.info(String.format("Error when trying to read property %s with %s name", METRIC_PERCENTILES_PROPERTY, percentile));
            return null;
        }
    }

    public static Double evaluateHistogramBucketValue(String bucket) {
        if (bucket.matches("[0-9]+[.]*[0-9]*")) {
            return Double.parseDouble(bucket);
        } else {
            logger.info(String.format("Error when trying to read property %s with %s name", METRIC_HISTOGRAM_BUCKETS_PROPERTY, bucket));
            return null;
        }
    }

    public static Duration evaluateTimerBucketValue(String bucket) {
        bucket = bucket.trim();
        if (bucket.matches("[0-9]+ms")) { //case ms
            return Duration.ofMillis(Long.parseLong(bucket.substring(0, bucket.length() - 2)));
        } else if (bucket.matches("[0-9]+s")) { //case s
            return Duration.ofSeconds(Long.parseLong(bucket.substring(0, bucket.length() - 1)));
        } else if (bucket.matches("[0-9]+m")) { //case m
            return Duration.ofMinutes(Long.parseLong(bucket.substring(0, bucket.length() - 1)));
        } else if (bucket.matches("[0-9]+h")) { //case h
            return Duration.ofHours(Long.parseLong(bucket.substring(0, bucket.length() - 1)));
        } else if (bucket.matches("[0-9]+")) { //normal
            return Duration.ofMillis(Long.parseLong(bucket));
        } else {
            logger.info(String.format("Error when trying to read property %s with %s name", METRIC_TIMER_BUCKETS_PROPERTY, bucket));
            return null;
        }
    }
    
}
