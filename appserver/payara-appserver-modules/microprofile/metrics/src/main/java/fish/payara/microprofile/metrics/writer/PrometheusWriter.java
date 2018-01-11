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

import static fish.payara.microprofile.metrics.Constants.EMPTY_STRING;
import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.APPLICATION;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.BASE;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.VENDOR;
import org.eclipse.microprofile.metrics.Timer;
import org.glassfish.internal.api.Globals;

public class PrometheusWriter implements MetricsWriter {

    private final Writer writer;
    
    private final MetricsService service;
    
    private static final Logger LOGGER = Logger.getLogger(PrometheusWriter.class.getName());

    public PrometheusWriter(Writer writer) {
        this.writer = writer;
        this.service = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);
    }

    @Override
    public void write(String registryName, String metricName) throws NoSuchMetricException, NoSuchRegistryException, IOException {
        StringBuilder builder = new StringBuilder();
        if (APPLICATION.getName().equals(registryName)) {
            for (String appRegistryName : service.getApplicationRegistryNames()) {
                writeMetrics(builder, appRegistryName, metricName);
            }
        } else {
            writeMetrics(builder, registryName, metricName);
        }
        serialize(builder);
    }

    @Override
    public void write(String registryName) throws NoSuchRegistryException, IOException {
        StringBuilder builder = new StringBuilder();
        if (APPLICATION.getName().equals(registryName)) {
            for (String appRegistryName : service.getApplicationRegistryNames()) {
                writeMetrics(builder, appRegistryName);
            }
        } else {
            writeMetrics(builder, registryName);
        }
        serialize(builder);
    }

    @Override
    public void write() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String registryName : service.getAllRegistryNames()) {
            try {
                writeMetrics(builder, registryName);
            } catch (NoSuchRegistryException e) { // Ignore
            }
        }
        serialize(builder);
    }

    private void writeMetrics(StringBuilder builder, String registryName) throws NoSuchRegistryException {
        writeMetricMap(
                builder,
                registryName,
                service.getMetricsAsMap(registryName),
                service.getMetadataAsMap(registryName)
        );
    }

    private void writeMetrics(StringBuilder builder, String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException {
        writeMetricMap(
                builder,
                registryName,
                service.getMetricsAsMap(registryName, metricName),
                service.getMetadataAsMap(registryName, metricName)
        );
    }

    private void writeMetricMap(StringBuilder builder, String registryName, Map<String, Metric> metricMap, Map<String, Metadata> metricMetadataMap) {
        for (Entry<String, Metric> entry : metricMap.entrySet()) {
            String metricName = entry.getKey();
            //Translation rules :
            //Scope is always specified at the start of the metric name
            //Scope and name are separated by colon (:)
            if(!BASE.getName().equals(registryName) 
                    && !VENDOR.getName().equals(registryName)){
                registryName = APPLICATION.getName();
            }
                
            String name = registryName + ":" + metricName;
            Metric metric = entry.getValue();
            Metadata metricMetadata = metricMetadataMap.get(metricName);

            String description = metricMetadata.getDescription() == null || metricMetadata.getDescription().trim().isEmpty()
                    ? EMPTY_STRING : metricMetadata.getDescription();
            String tags = metricMetadata.getTagsAsString();
            String unit = metricMetadata.getUnit();

            PrometheusExporter exporter = new PrometheusExporter(builder);

            if (Counter.class.isInstance(metric)) {
                exporter.exportCounter((Counter) metric, name, description, tags);
            } else if (Gauge.class.isInstance(metric)) {
                exporter.exportGauge((Gauge) metric, name, description, tags, unit);
            } else if (Histogram.class.isInstance(metric)) {
                exporter.exportHistogram((Histogram) metric, name, description, tags, unit);
            } else if (Meter.class.isInstance(metric)) {
                exporter.exportMeter((Meter) metric, name, description, tags);
            } else if (Timer.class.isInstance(metric)) {
                exporter.exportTimer((Timer) metric, name, description, tags, unit);
            } else {
                LOGGER.log(Level.WARNING, "Metric type {0} for {1} is invalid", new Object[]{metric.getClass(), metricName});
            }
        }
    }

    private void serialize(StringBuilder builder) throws IOException {
        try {
            writer.write(builder.toString());
        } finally {
            writer.close();
        }
    }
}
