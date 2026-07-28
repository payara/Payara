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
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class InMemorySpanExporter implements SpanExporter {

    private volatile boolean stopped;
    private final List<SpanData> exported = new ArrayList<>();
    private final AtomicInteger batches = new AtomicInteger(0);

    @Inject
    @ConfigProperty(name = "otel.bsp.schedule.delay", defaultValue = "10")
    long batchDelayMs;

    public void reset() {
        synchronized (exported) {
            exported.clear();
            batches.set(0);
        }
    }

    public List<SpanData> getSpans() {
        waitForExport();
        synchronized (exported) {
            List<SpanData> result = new ArrayList<>(exported);
            exported.clear();
            return result;
        }
    }

    private void waitForExport() {
        int gen = batches.get();
        long deadline = System.currentTimeMillis() + 3000;
        while (batches.get() == gen && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        int current;
        do {
            current = batches.get();
            try { Thread.sleep(batchDelayMs * 2); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        } while (current != batches.get());
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (stopped) return CompletableResultCode.ofFailure();
        synchronized (exported) {
            for (SpanData s : spans) {
                if (!s.getName().contains("ArquillianServletRunnerEE9")) {
                    exported.add(s);
                }
            }
            batches.incrementAndGet();
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
    @Override public CompletableResultCode shutdown() { stopped = true; return CompletableResultCode.ofSuccess(); }

    public static class Provider implements ConfigurableSpanExporterProvider {
        @Override public SpanExporter createExporter(ConfigProperties c) { return CDI.current().select(InMemorySpanExporter.class).get(); }
        @Override public String getName() { return "in-memory-jaxws"; }
    }
}
