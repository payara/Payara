/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
import java.util.logging.Level;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;

public class JsonMetricWriter extends JsonWriter {
    
    // Dropwizard Histogram, Meter, or Timer Constants
    private static final String COUNT = "count";
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

    public JsonMetricWriter(Writer writer) {
        super(writer);
    }

    @Override
    protected JsonObject getJsonData(String registryName) throws NoSuchRegistryException {
        return getJsonFromMetrics(service.getMetricsAsMap(registryName));
    }

    @Override
    protected JsonObject getJsonData(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException {
        return getJsonFromMetrics(service.getMetricsAsMap(registryName, metricName));
    }

    private JsonObject getJsonFromMetrics(Map<String, Metric> metricMap) {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        for (Map.Entry<String, Metric> entry : metricMap.entrySet()) {
            String metricName = entry.getKey();
            Metric metric = entry.getValue();
            if (Counter.class.isInstance(metric)) {
                payloadBuilder.add(metricName, ((Counter) metric).getCount());
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
                    LOGGER.log(Level.FINER, "Skipping JSON output for Gauge: {0} of type {1}", new Object[]{metricName, gaugeValue.getClass()});
                    continue;
                }
                value = (Number) gaugeValue;
                addValueToJsonObject(payloadBuilder, metricName, value);
            } else if (Histogram.class.isInstance(metric)) {
                payloadBuilder.add(metricName, getJsonFromMap(getHistogramNumbers((Histogram) metric)));
            } else if (Meter.class.isInstance(metric)) {
                payloadBuilder.add(metricName, getJsonFromMap(getMeterNumbers((Meter) metric)));
            } else if (Timer.class.isInstance(metric)) {
                payloadBuilder.add(metricName, getJsonFromMap(getTimerNumbers((Timer) metric)));
            } else {
                LOGGER.log(Level.WARNING, "Metric type '{0} for {1} is invalid", new Object[]{metric.getClass(), metricName});
            }
        }
        return payloadBuilder.build();
    }
    
    private Map<String, Number> getTimerNumbers(Timer timer) {
        Map<String, Number> results = new HashMap<>();
        results.putAll(getMeteredNumbers(timer));
        results.putAll(getSnapshotNumbers(timer.getSnapshot()));
        return results;
    }

    private Map<String, Number> getHistogramNumbers(Histogram histogram) {
        Map<String, Number> results = new HashMap<>();
        results.put(COUNT, histogram.getCount());
        results.putAll(getSnapshotNumbers(histogram.getSnapshot()));
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

    private Map<String, Number> getSnapshotNumbers(Snapshot snapshot) {
        Map<String, Number> results = new HashMap<>();
        results.put(MAX, snapshot.getMax());
        results.put(MEAN, snapshot.getMean());
        results.put(MIN, snapshot.getMin());
        results.put(STD_DEV, snapshot.getStdDev());
        results.put(MEDIAN, snapshot.getMedian());
        results.put(PERCENTILE_75TH, snapshot.get75thPercentile());
        results.put(PERCENTILE_95TH, snapshot.get95thPercentile());
        results.put(PERCENTILE_98TH, snapshot.get98thPercentile());
        results.put(PERCENTILE_99TH, snapshot.get99thPercentile());
        results.put(PERCENTILE_999TH, snapshot.get999thPercentile());
        return results;
    }

}
