/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.telemetry.service;

import java.util.HashSet;
import java.util.Set;

public class PayaraTelemetryConstants {

    public static final String OTEL_PROPERTIES_PREFIX = "otel";
    public static final String OTEL_SYSTEM_PROPERTY_NAME = "otel.sdk.disabled";
    public static final String OTEL_ENVIRONMENT_PROPERTY_NAME = "OTEL_SDK_DISABLED";
    public static final String OTEL_SERVICE_NAME = "otel.service.name";
    public static final String ATTRIBUTE_SERVICE_NAME = "service.name";
    public static final String OTEL_METRICS_EXPORTER = "otel.metrics.exporter";
    public static final String OTEL_TRACES_EXPORTER = "otel.traces.exporter";
    public static final String OTEL_LOGS_EXPORTER = "otel.logs.exporter";
    public static final String OTEL_PROPAGATORS = "otel.propagators";
    public static final String OTEL_RESOURCE_ATTRIBUTES = "otel.resource.attributes";
    public static final String OTEL_BSP_SCHEDULE_DELAY= "otel.bsp.schedule.delay";
    public static final String OTEL_BSP_MAX_QUEUE = "otel.bsp.max.queue.size";
    public static final String OTEL_BSP_MAX_EXPORT_BATCH_SIZE = "otel.bsp.max.export.batch.size";
    public static final String OTEL_BSP_EXPORT_TIMEOUT = "otel.bsp.export.timeout";
    public static final String OTEL_TRACES_SAMPLER = "otel.traces.sampler";
    public static final String OTEL_TRACES_SAMPLER_ARG ="otel.traces.sampler.arg";
    public static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
    public static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
    public static final String OTEL_EXPORTER_OTLP_CERTIFICATE = "otel.exporter.otlp.certificate";
    public static final String OTEL_EXPORTER_OTLP_CLIENT_KEY = "otel.exporter.otlp.client.key";
    public static final String OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE = "otel.exporter.otlp.client.certificate";
    public static final String OTEL_EXPORTER_OTLP_HEADERS = "otel.exporter.otlp.headers";
    public static final String OTEL_EXPORTER_OTLP_COMPRESSION = "otel.exporter.otlp.compression";
    public static final String OTEL_EXPORTER_OTLP_TIMEOUT = "otel.exporter.otlp.timeout";
    public static final String OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE = "otel.exporter.otlp.metrics.temporality.preference";
    public static final String OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION = "otel.exporter.otlp.metrics.default.histogram.aggregation";
    public static final String OTEL_METRICS_EXEMPLAR_FILTER = "otel.metrics.exemplar.filter";
    public static final String OTEL_METRIC_EXPORT_INTERVAL = "otel.metric.export.interval";
    public static final String OTEL_BLRP_SCHEDULE_DELAY = "otel.blrp.schedule.delay";
    public static final String OTEL_BLRP_MAX_QUEUE_SIZE = "otel.blrp.max.queue.size";
    public static final String OTEL_BLRP_MAX_EXPORT_BATCH_SIZE = "otel.blrp.max.export.batch.size";
    public static final String OTEL_BLRP_EXPORT_TIMEOUT = "otel.blrp.export.timeout";
    public static final String PAYARA_OTEL_RUNTIME_INSTANCE_NAME = "fish.payara.otel.runtime.intance";
    public static final Set<String> otelProperties;
    
    static {
        otelProperties = new HashSet<>();
        otelProperties.add(OTEL_SERVICE_NAME);
        otelProperties.add(OTEL_METRICS_EXPORTER);
        otelProperties.add(OTEL_TRACES_EXPORTER);
        otelProperties.add(OTEL_LOGS_EXPORTER);
        otelProperties.add(OTEL_PROPAGATORS);
        otelProperties.add(OTEL_RESOURCE_ATTRIBUTES);
        otelProperties.add(OTEL_BSP_SCHEDULE_DELAY);
        otelProperties.add(OTEL_BSP_MAX_QUEUE);
        otelProperties.add(OTEL_BSP_MAX_EXPORT_BATCH_SIZE);
        otelProperties.add(OTEL_BSP_EXPORT_TIMEOUT);
        otelProperties.add(OTEL_TRACES_SAMPLER);
        otelProperties.add(OTEL_TRACES_SAMPLER_ARG);
        otelProperties.add(OTEL_EXPORTER_OTLP_PROTOCOL);
        otelProperties.add(OTEL_EXPORTER_OTLP_ENDPOINT);
        otelProperties.add(OTEL_EXPORTER_OTLP_CERTIFICATE);
        otelProperties.add(OTEL_EXPORTER_OTLP_CLIENT_KEY);
        otelProperties.add(OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE);
        otelProperties.add(OTEL_EXPORTER_OTLP_HEADERS);
        otelProperties.add(OTEL_EXPORTER_OTLP_COMPRESSION);
        otelProperties.add(OTEL_EXPORTER_OTLP_TIMEOUT);
        otelProperties.add(OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE);
        otelProperties.add(OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION);
        otelProperties.add(OTEL_METRICS_EXEMPLAR_FILTER);
        otelProperties.add(OTEL_METRIC_EXPORT_INTERVAL);
        otelProperties.add(OTEL_BLRP_SCHEDULE_DELAY);
        otelProperties.add(OTEL_BLRP_MAX_QUEUE_SIZE);
        otelProperties.add(OTEL_BLRP_MAX_EXPORT_BATCH_SIZE);
        otelProperties.add(OTEL_BLRP_EXPORT_TIMEOUT);       
    }
}
