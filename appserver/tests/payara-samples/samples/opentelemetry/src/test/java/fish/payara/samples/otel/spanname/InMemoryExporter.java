/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023-2026 Payara Foundation and/or its affiliates. All rights reserved.
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

import io.opentelemetry.api.common.AttributeKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * In-memory {@link SpanExporter} for test assertions.
 *
 * <h3>Waiting strategy</h3>
 * <p>{@link #getSpans(Predicate)} accepts a {@code isNoise} predicate that identifies
 * spans which should not affect the wait logic (e.g. Arquillian runner spans or Payara
 * infrastructure spans). The method uses a <em>quiet-period</em> strategy:
 * <ol>
 *   <li>Wait until at least one <em>interesting</em> (non-noise) span has arrived.</li>
 *   <li>Once seen, restart a {@code 2 × bsp.schedule.delay} clock each time another
 *       interesting span lands.</li>
 *   <li>Return when that clock expires (quiet period elapsed) or after 2 seconds total.</li>
 * </ol>
 * Only interesting spans reset the clock, so pure-noise batches (e.g. background
 * concurrent-waiting spans from server infrastructure) do not delay the return.
 */
@ApplicationScoped
public class InMemoryExporter implements SpanExporter {

    private static final Logger LOGGER = Logger.getLogger(InMemoryExporter.class.getName());

    private volatile boolean stopped;
    private final List<SpanData> exported = new ArrayList<>();

    @Inject
    @ConfigProperty(name = "otel.bsp.schedule.delay")
    long bspScheduleDelayMs;

    // -------------------------------------------------------------------------
    // Default noise predicate
    // -------------------------------------------------------------------------

    private static final AttributeKey<String> PAYARA_SUBSYSTEM =
            AttributeKey.stringKey("payara.subsystem");

    /**
     * Standard noise predicate: Arquillian runner spans and Payara Jakarta Concurrency
     * waiting spans ({@code payara.subsystem = "jakarta-concurrency"}) are considered
     * infrastructure noise for most tests. Override per call-site via {@link #getSpans(Predicate)}.
     */
    public static final Predicate<SpanData> DEFAULT_NOISE =
            span -> span.getName().contains("ArquillianServletRunnerEE9")
                 || "jakarta-concurrency".equals(span.getAttributes().get(PAYARA_SUBSYSTEM));

    /**
     * Noise predicate that only suppresses Arquillian runner spans, allowing
     * Payara's Jakarta Concurrency waiting spans through. Use this in tests
     * that specifically assert concurrent instrumentation.
     */
    public static final Predicate<SpanData> ARQUILLIAN_ONLY_NOISE =
            span -> span.getName().contains("ArquillianServletRunnerEE9");

    // -------------------------------------------------------------------------
    // SpanExporter
    // -------------------------------------------------------------------------

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (stopped) {
            return CompletableResultCode.ofFailure();
        }
        synchronized (exported) {
            exported.addAll(spans);
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
        return CompletableResultCode.ofSuccess();
    }

    // -------------------------------------------------------------------------
    // Test API
    // -------------------------------------------------------------------------

    /**
     * Resets collected spans and the internal state. Call from {@code @Before} / {@code @BeforeMethod}.
     */
    public void reset() {
        synchronized (exported) {
            exported.clear();
        }
    }

    /**
     * Waits until interesting (non-noise) spans stop arriving, then returns them.
     *
     * @param isNoise predicate identifying spans that should not influence the wait
     *                clock or appear in the returned list
     * @return all non-noise spans collected since the last {@link #reset()}
     */
    public List<SpanData> getSpans(Predicate<SpanData> isNoise) {
        long deadline        = System.currentTimeMillis() + 2_000;
        long lastInteresting = -1;
        int  highWatermark   = 0;   // how many exported entries we checked last iteration

        while (System.currentTimeMillis() < deadline) {
            synchronized (exported) {
                int currentSize = exported.size();
                if (currentSize > highWatermark) {
                    // Check newly arrived spans
                    boolean sawInteresting = exported.subList(highWatermark, currentSize)
                            .stream().anyMatch(isNoise.negate());
                    highWatermark = currentSize;
                    if (sawInteresting) {
                        lastInteresting = System.currentTimeMillis();
                    }
                }
            }
            // Once we have seen at least one interesting span, wait for quiet period
            if (lastInteresting >= 0
                    && System.currentTimeMillis() - lastInteresting > bspScheduleDelayMs * 2) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        synchronized (exported) {
            List<SpanData> result = exported.stream()
                    .filter(isNoise.negate())
                    .collect(Collectors.toList());
            LOGGER.info(() -> "Returning spans: " + result.stream()
                    .map(s -> s.getName() + "(" + s.getKind() + ")").toList());
            exported.clear();
            return result;
        }
    }

    /**
     * Convenience overload using {@link #DEFAULT_NOISE} — filters out Arquillian runner
     * spans and Payara concurrent waiting spans ({@code concurrent/* run}).
     */
    public List<SpanData> getSpans() {
        return getSpans(DEFAULT_NOISE);
    }

    // -------------------------------------------------------------------------
    // Provider
    // -------------------------------------------------------------------------

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
