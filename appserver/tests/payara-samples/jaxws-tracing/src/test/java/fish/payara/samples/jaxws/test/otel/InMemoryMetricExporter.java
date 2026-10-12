/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.jaxws.test.otel;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
public class InMemoryMetricExporter implements MetricExporter {

    private volatile boolean stopped;
    private final List<MetricData> exported = new ArrayList<>();
    private final AtomicInteger batches = new AtomicInteger(0);

    @Inject
    @ConfigProperty(name = "otel.metric.export.interval", defaultValue = "100")
    long exportIntervalMs;

    public void reset() {
        synchronized (exported) {
            exported.clear();
            batches.set(0);
        }
    }

    /**
     * Returns all collected {@link HistogramPointData} for a given metric name,
     * waiting up to 3 seconds for at least one export batch to arrive.
     */
    public List<HistogramPointData> getHistogramPoints(String metricName) {
        waitForExport();
        synchronized (exported) {
            return exported.stream()
                    .filter(m -> metricName.equals(m.getName()))
                    .flatMap(m -> m.getHistogramData().getPoints().stream())
                    .collect(Collectors.toList());
        }
    }

    private void waitForExport() {
        int gen = batches.get();
        long deadline = System.currentTimeMillis() + 3000;
        while (batches.get() == gen && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(10); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted waiting for metric export", e);
            }
        }
        // Sleep one more export cycle to catch any trailing data — do NOT loop here because
        // other metrics (JVM, Hazelcast) are exported continuously and would cause an infinite loop.
        try { Thread.sleep(exportIntervalMs * 2); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for metric export", e);
        }
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.DELTA;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        if (stopped) return CompletableResultCode.ofFailure();
        synchronized (exported) {
            exported.addAll(metrics);
            batches.incrementAndGet();
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }

    @Override
    public CompletableResultCode shutdown() {
        stopped = true;
        return CompletableResultCode.ofSuccess();
    }

    public static class Provider implements ConfigurableMetricExporterProvider {
        @Override
        public MetricExporter createExporter(ConfigProperties c) {
            return CDI.current().select(InMemoryMetricExporter.class).get();
        }
        @Override public String getName() { return "in-memory-jaxws"; }
    }
}
