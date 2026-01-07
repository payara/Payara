/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.samples.otel.spanname;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class InMemoryExporter implements SpanExporter {

    private volatile boolean stopped;
    private List<SpanData> exported = new ArrayList<>();
    private AtomicInteger batches = new AtomicInteger(0);

    private static final Logger LOGGER = Logger.getLogger(InMemoryExporter.class.getName());

    @Inject
    @ConfigProperty(name = "otel.bsp.schedule.delay")
    long batchProcessorScheduleMS;

    public void reset() {
        exported.clear();
        batches.set(0);
    }

    public List<SpanData> getSpans() {
        waitForNextExport();
        synchronized (exported) {
            var spanData = new ArrayList<SpanData>(exported);
            exported.clear();
            LOGGER.info(() -> "Passing exported list: "+spanData);
            return spanData;
        }
    }

    private void waitForNextExport() {
        int currentGen;
        do {
            currentGen = batches.get();
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(batchProcessorScheduleMS * 2));
            // no export in two periods means that we're out of fresh spans now.
        } while (currentGen != batches.get());
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (stopped) {
            return CompletableResultCode.ofFailure();
        }
        synchronized (exported) {
            exported.addAll(spans);
            batches.incrementAndGet();
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        stopped = true;
        flush();
        return CompletableResultCode.ofSuccess();
    }

    public static class Provider implements ConfigurableSpanExporterProvider {

        @Override
        public SpanExporter createExporter(ConfigProperties configProperties) {
            return CDI.current().select(InMemoryExporter.class).get();
        }

        @Override
        public String getName() {
            return "in-memory";
        }
    }
}
