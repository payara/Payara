/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 *
 * *****************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package fish.payara.microprofile.metrics.writer;

import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;

public class JsonMetricWriter extends JsonWriter {
    
    // Dropwizard Histogram, Meter, Gauge or Timer Constants
    private static final String COUNT = "count";
    private static final String CURRENT = "current";
    private static final String MEAN_RATE = "meanRate";
    private static final String ONE_MINUTE_RATE = "oneMinRate";
    private static final String FIVE_MINUTE_RATE = "fiveMinRate";
    private static final String FIFTEEN_MINUTE_RATE = "fifteenMinRate";
    private static final String MAX = "max";
    private static final String MEAN = "mean";
    private static final String MIN = "min";
    private static final String STD_DEV = "stddev";
    private static final String MEDIAN = "p50";
    private static final String PERCENTILE_75TH = "p75";
    private static final String PERCENTILE_95TH = "p95";
    private static final String PERCENTILE_98TH = "p98";
    private static final String PERCENTILE_99TH = "p99";
    private static final String PERCENTILE_999TH = "p999";

    private static final long NANOSECOND_CONVERSION = 1L;
    private static final long MICROSECOND_CONVERSION = 1_000L;
    private static final long MILLISECOND_CONVERSION = 1_000_000L;
    private static final long SECOND_CONVERSION = 1_000_000_000L;
    private static final long MINUTE_CONVERSION = 60 * 1_000_000_000L;
    private static final long HOUR_CONVERSION = 60 * 60 * 1_000_000_000L;
    private static final long DAY_CONVERSION = 24 * 60 * 60 * 1000_000_000L;

    public JsonMetricWriter(Writer writer) {
        super(writer);
    }

    @Override
    protected JsonObjectBuilder getJsonData(String registryName) throws NoSuchRegistryException {
        Map<String, Metadata> metadataMap = service.getMetadataAsMap(registryName);
        Map<MetricID, Metric> metricMap = service.getMetricsAsMap(registryName);
        return getJsonFromMetrics(metricMap, metadataMap);
    }

    @Override
    protected JsonObjectBuilder getJsonData(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException {
        Map<String, Metadata> metadataMap = service.getMetadataAsMap(registryName, metricName);
        Map<MetricID, Metric> metricMap = service.getMetricsAsMap(registryName, metricName);
        return getJsonFromMetrics(metricMap, metadataMap);
    }

    private JsonObjectBuilder getJsonFromMetrics(
            Map<MetricID, Metric> metricMap,
            Map<String, Metadata> metadataMap) {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        for (Map.Entry<MetricID, Metric> entry : metricMap.entrySet()) {
            MetricID metricID = entry.getKey();
            String metricIDString = metricIDTranslation(metricID);
            Set<Entry<String, String>> tagsSet = entry.getKey().getTags().entrySet();
            Metric metric = entry.getValue();
            if (Counter.class.isInstance(metric)) {
                payloadBuilder.add(metricIDString, ((Counter) metric).getCount());
            } else if (ConcurrentGauge.class.isInstance(metric)) {
                payloadBuilder = addOrExtendMap(payloadBuilder, entry.getKey().getName(), getConcurrentGaugeNumbers((ConcurrentGauge) metric), tagsToStringSuffix(tagsSet));
            } else if (Gauge.class.isInstance(metric)) {
                Number value;
                Object gaugeValue;
                try {
                    gaugeValue = ((Gauge) metric).getValue();
                } catch (IllegalStateException e) {
                    // The forwarding gauge is unloaded
                    continue;
                }
                if (!Number.class.isInstance(gaugeValue)) {
                    LOGGER.log(Level.FINER, "Skipping JSON output for Gauge: {0} of type {1}", new Object[]{metricIDString, gaugeValue.getClass()});
                    continue;
                }
                value = (Number) gaugeValue;
                addValueToJsonObject(payloadBuilder, metricIDString, value);
            } else if (Histogram.class.isInstance(metric)) {
                payloadBuilder = addOrExtendMap(payloadBuilder, entry.getKey().getName(), getHistogramNumbers((Histogram) metric, 1L), tagsToStringSuffix(tagsSet));
            } else if (Meter.class.isInstance(metric)) {
                payloadBuilder = addOrExtendMap(payloadBuilder, entry.getKey().getName(), getMeterNumbers((Meter) metric), tagsToStringSuffix(tagsSet));
            } else if (Timer.class.isInstance(metric)) {
                Metadata metricMetaData = metadataMap.get(metricID.getName());
                String unit = metricMetaData.getUnit().orElse(MetricUnits.NANOSECONDS);
                payloadBuilder = addOrExtendMap(payloadBuilder, entry.getKey().getName(), getTimerNumbers((Timer) metric, getConversionFactor(unit)), tagsToStringSuffix(tagsSet));
            } else {
                LOGGER.log(Level.WARNING, "Metric type {0} for {1} is invalid", new Object[]{metric.getClass(), metricIDString});
            }
        }
        return payloadBuilder;
    }

    private long getConversionFactor(String unit) {
        long conversionFactor;
        switch (unit) {
            case MetricUnits.NANOSECONDS:
                conversionFactor = NANOSECOND_CONVERSION;
                break;
            case MetricUnits.MICROSECONDS:
                conversionFactor = MICROSECOND_CONVERSION;
                break;
            case MetricUnits.MILLISECONDS:
                conversionFactor = MILLISECOND_CONVERSION;
                break;
            case MetricUnits.SECONDS:
                conversionFactor = SECOND_CONVERSION;
                break;
            case MetricUnits.MINUTES:
                conversionFactor = MINUTE_CONVERSION;
                break;
            case MetricUnits.HOURS:
                conversionFactor = HOUR_CONVERSION;
                break;
            case MetricUnits.DAYS:
                conversionFactor = DAY_CONVERSION;
                break;
            default:
                conversionFactor = NANOSECOND_CONVERSION;
                break;
        }
        return conversionFactor;
    }

    private Map<String, Number> getConcurrentGaugeNumbers(ConcurrentGauge gauge) {
        Map<String, Number> results = new HashMap<>();
        results.put(CURRENT, gauge.getCount());
        results.put(MIN, gauge.getMin());
        results.put(MAX, gauge.getMax());
        return results;
    }
    
    private Map<String, Number> getTimerNumbers(Timer timer, long conversionFactor) {
        Map<String, Number> results = new HashMap<>();
        results.putAll(getMeteredNumbers(timer));
        results.putAll(getSnapshotNumbers(timer.getSnapshot(), conversionFactor));
        return results;
    }

    private Map<String, Number> getHistogramNumbers(Histogram histogram, long conversionFactor) {
        Map<String, Number> results = new HashMap<>();
        results.put(COUNT, histogram.getCount());
        results.putAll(getSnapshotNumbers(histogram.getSnapshot(), conversionFactor));
        return results;
    }

    private Map<String, Number> getMeterNumbers(Meter meter) {
        Map<String, Number> results = new HashMap<>();
        results.putAll(getMeteredNumbers(meter));
        return results;
    }

    private Map<String, Number> getMeteredNumbers(Metered metered) {
        Map<String, Number> results = new HashMap<>();
        results.put(COUNT, metered.getCount());
        results.put(MEAN_RATE, metered.getMeanRate());
        results.put(ONE_MINUTE_RATE, metered.getOneMinuteRate());
        results.put(FIVE_MINUTE_RATE, metered.getFiveMinuteRate());
        results.put(FIFTEEN_MINUTE_RATE, metered.getFifteenMinuteRate());
        return results;
    }

    private Map<String, Number> getSnapshotNumbers(Snapshot snapshot, long conversionFactor) {
        Map<String, Number> results = new HashMap<>();
        results.put(MAX, snapshot.getMax() / conversionFactor);
        results.put(MEAN, snapshot.getMean() / conversionFactor);
        results.put(MIN, snapshot.getMin() / conversionFactor);
        results.put(STD_DEV, snapshot.getStdDev() / conversionFactor);
        results.put(MEDIAN, snapshot.getMedian() / conversionFactor);
        results.put(PERCENTILE_75TH, snapshot.get75thPercentile() / conversionFactor);
        results.put(PERCENTILE_95TH, snapshot.get95thPercentile() / conversionFactor);
        results.put(PERCENTILE_98TH, snapshot.get98thPercentile() / conversionFactor);
        results.put(PERCENTILE_99TH, snapshot.get99thPercentile() / conversionFactor);
        results.put(PERCENTILE_999TH, snapshot.get999thPercentile() / conversionFactor);
        return results;
    }
    
    /**
     * Converts a {@link MetricID} into the format required by spec
     * @param metricID
     * @return
     * @see Section 3.1.1 of MP Metrics 2.0 Specification
     */
    private String metricIDTranslation(MetricID metricID) {
        StringBuilder jsonKey = new StringBuilder(metricID.getName());
        jsonKey.append(tagsToStringSuffix(metricID.getTags().entrySet()));
        return jsonKey.toString();
    }
    
    private String tagsToStringSuffix(Set<Entry<String, String>> tagsSet) {
        StringBuilder tags = new StringBuilder();
        for (Entry<String,String> tag: tagsSet) {
            tags.append(';').append(tag.getKey());
            tags.append('=').append(tag.getValue().replace(';', '_'));
        }
        
        return tags.toString();
    }
    
    private JsonObjectBuilder addOrExtendMap(JsonObjectBuilder original, String key, Map<String, Number> values, String suffix) {
        JsonObject built = original.build();
        JsonObjectBuilder clone = Json.createObjectBuilder(built);

        if (built.containsKey(key)) {
            JsonObject current = built.getJsonObject(key);
            JsonObjectBuilder extended = Json.createObjectBuilder(current);
            JsonObjectBuilder newData = Json.createObjectBuilder(getJsonFromMap(values, suffix));
            extended.addAll(newData);
            clone.add(key, extended.build());
        } else {
            clone.add(key, getJsonFromMap(values, suffix));
        }
       
        return clone;
    }

}
