/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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
 */

package fish.payara.microprofile.metrics.writer;

import fish.payara.microprofile.metrics.MetricHelper;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.Timer;

public class JsonMetricWriter extends JsonWriter {

    public JsonMetricWriter(Writer writer, MetricHelper helper) {
        super(writer, helper);
    }

    @Override
    protected JsonObject getJsonData(String registryName) throws NoSuchRegistryException {
        return getJsonFromMetricMap(helper.getMetricsAsMap(registryName));
    }

    @Override
    protected JsonObject getJsonData(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException {
        return getJsonFromMetricMap(helper.getMetricsAsMap(registryName, metricName));
    }

    private JsonObject getJsonFromMetricMap(Map<String, Metric> metricMap) {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        for (Map.Entry<String, Metric> entry : metricMap.entrySet()) {
            String metricName = entry.getKey();
            Metric metric = entry.getValue();
            if (Counter.class.isInstance(metric)) {
                payloadBuilder.add(metricName, ((Counter) metric).getCount());
            } else if (Gauge.class.isInstance(metric)) {
                try {
                    payloadBuilder.add(metricName, ((Gauge) metric).getValue().toString());
                } catch (IllegalStateException e) {
                    // The forwarding gauge is likely unloaded.
                }
            } else if (Timer.class.isInstance(metric)) {
                payloadBuilder.add(metricName, getJsonFromMap(MetricHelper.getTimerNumbers((Timer) metric)));
            } else if (Histogram.class.isInstance(metric)) {
                payloadBuilder.add(metricName, getJsonFromMap(MetricHelper.getHistogramNumbers((Histogram) metric)));
            } else if (Meter.class.isInstance(metric)) {
                payloadBuilder.add(metricName, getJsonFromMap(MetricHelper.getMeterNumbers((Meter) metric)));
            } else {
                LOGGER.log(Level.WARNING, "Metric type '{0} for {1} is invalid", new Object[]{metric.getClass(), metricName});
            }
        }
        return payloadBuilder.build();
    }

}
