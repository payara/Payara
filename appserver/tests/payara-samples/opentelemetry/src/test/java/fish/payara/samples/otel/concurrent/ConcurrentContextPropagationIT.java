/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.samples.otel.concurrent;

import fish.payara.samples.otel.spanname.InMemoryExporter;
import static fish.payara.samples.otel.spanname.InMemoryExporter.ARQUILLIAN_ONLY_NOISE;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Integration tests for OpenTelemetry context propagation through Jakarta Concurrency's
 * {@link ManagedExecutorService}.
 *
 * <h3>Trace shape</h3>
 * <pre>
 * parentSpan (submitting thread)
 *   ├── concurrent/&lt;pool&gt; wait  [waiting span — ends at task pickup, ~queue latency]
 *   └── user task span         [direct child of parentSpan, on worker thread]
 * </pre>
 *
 * <p>Both instrumentation styles are exercised:
 * <ul>
 *   <li>{@code @WithSpan} on a CDI bean method — annotation-driven span creation.</li>
 *   <li>Manual {@link Tracer#spanBuilder} — raw OTel API inside a task.</li>
 * </ul>
 */
@RunWith(Arquillian.class)
public class ConcurrentContextPropagationIT {

    // -------------------------------------------------------------------------
    // CDI bean providing @WithSpan-annotated task methods
    // -------------------------------------------------------------------------

    @ApplicationScoped
    public static class ConcurrentTaskBean {

        @Inject
        Tracer tracer;

        @WithSpan("annotatedTask")
        public String annotatedTask() {
            return Span.current().getSpanContext().getSpanId();
        }

        @WithSpan("annotatedTaskWithManualChild")
        public String annotatedTaskWithManualChild() {
            Span child = tracer.spanBuilder("manualChild").startSpan();
            try (io.opentelemetry.context.Scope s = child.makeCurrent()) {
                return "ok";
            } finally {
                child.end();
            }
        }

        @WithSpan("failingAnnotatedTask")
        public String failingAnnotatedTask() {
            Span.current().setStatus(StatusCode.ERROR, "intentional failure");
            throw new RuntimeException("intentional failure");
        }
    }

    // -------------------------------------------------------------------------
    // Config
    // -------------------------------------------------------------------------

    public static class OtelConfig implements ConfigSource {
        private final Map<String, String> props;
        public OtelConfig() {
            var map = new HashMap<String, String>();
            map.put("otel.sdk.disabled", "false");
            map.put("otel.traces.exporter", "in-memory");
            map.put("otel.bsp.schedule.delay", "10");
            this.props = Collections.unmodifiableMap(map);
        }
        @Override public Map<String, String> getProperties() { return props; }
        @Override public Set<String> getPropertyNames() { return props.keySet(); }
        @Override public String getValue(String key) { return props.get(key); }
        @Override public String getName() { return "concurrent-otel-test-config"; }
    }

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(InMemoryExporter.class)
                .addClass(OtelConfig.class)
                .addClass(ConcurrentTaskBean.class)
                .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemoryExporter.Provider.class)
                .addAsServiceProvider(ConfigSource.class, OtelConfig.class)
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml")
                        .resolve("org.assertj:assertj-core").withTransitivity().asFile());
    }

    // -------------------------------------------------------------------------
    // Test infrastructure
    // -------------------------------------------------------------------------

    @Resource
    ManagedExecutorService mes;

    @Inject
    Tracer tracer;

    @Inject
    InMemoryExporter exporter;

    @Inject
    ConcurrentTaskBean taskBean;

    @Before
    public void resetExporter() {
        exporter.reset();
    }

    // -------------------------------------------------------------------------
    // Waiting span tests
    // -------------------------------------------------------------------------

    /**
     * Verifies the waiting span is emitted and is a <em>sibling</em> (not parent) of the
     * user task span — both share the same parent.
     *
     * <pre>
     * parentSpan
     *   ├── concurrent/__defaultManagedExecutorService wait  [waiting span]
     *   └── annotatedTask                                   [user span]
     * </pre>
     */
    @Test
    public void testWaitingSpanIsSiblingOfUserSpan() throws ExecutionException, InterruptedException {
        var parentSpan = tracer.spanBuilder("parentSpan").startSpan();
        try (var ignored = parentSpan.makeCurrent()) {
            mes.supplyAsync(taskBean::annotatedTask).get();
        } finally {
            parentSpan.end();
        }

        var spans = exporter.getSpans(ARQUILLIAN_ONLY_NOISE);
        var parent  = findSpan(spans, "parentSpan");
        var waiting = findWaitingSpan(spans);
        var task    = findSpan(spans, "annotatedTask");

        assertNotNull("parentSpan must be exported", parent);
        assertNotNull("concurrent waiting span must be exported", waiting);
        assertNotNull("annotatedTask must be exported", task);

        assertEquals("all spans must share the same trace", parent.getTraceId(), waiting.getTraceId());
        assertEquals("all spans must share the same trace", parent.getTraceId(), task.getTraceId());

        // Waiting span and annotatedTask are BOTH direct children of parentSpan (siblings)
        assertEquals("waiting span must be direct child of parentSpan",
                parent.getSpanId(), waiting.getParentSpanId());
        assertEquals("annotatedTask must be direct child of parentSpan (not of waiting span)",
                parent.getSpanId(), task.getParentSpanId());
    }

    /**
     * Verifies the waiting span carries the expected attributes.
     */
    @Test
    public void testWaitingSpanAttributes() throws ExecutionException, InterruptedException {
        var parentSpan = tracer.spanBuilder("parentSpan").startSpan();
        try (var ignored = parentSpan.makeCurrent()) {
            mes.supplyAsync(taskBean::annotatedTask).get();
        } finally {
            parentSpan.end();
        }

        var spans   = exporter.getSpans(ARQUILLIAN_ONLY_NOISE);
        var waiting = findWaitingSpan(spans);
        assertNotNull("concurrent waiting span must be exported", waiting);

        var attrs = waiting.getAttributes();
        var allSpans = spans.stream().map(s -> s.getName() + "[" + s.getSpanId() + "]").toList();

        assertThat(attrs.get(AttributeKey.stringKey("payara.subsystem")))
                .as("payara.subsystem must be 'jakarta-concurrency'. attrs=%s spans=%s", attrs, allSpans)
                .isEqualTo("jakarta-concurrency");
        assertThat(attrs.get(AttributeKey.stringKey("thread.name")))
                .as("thread.name must be set. attrs=%s spans=%s", attrs, allSpans)
                .isNotNull();
        assertThat(attrs.get(AttributeKey.longKey("thread.id")))
                .as("thread.id must be set. attrs=%s spans=%s", attrs, allSpans)
                .isNotNull();
        assertThat(attrs.get(AttributeKey.doubleKey("scheduling.wait_ms")))
                .as("scheduling.wait_ms must be set. attrs=%s spans=%s", attrs, allSpans)
                .isNotNull()
                .isGreaterThanOrEqualTo(0.0);
    }

    // -------------------------------------------------------------------------
    // Thread stamping tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@code thread.id} and {@code thread.name} are stamped on every span
     * (including user spans and the waiting span) by the global {@code ThreadStampingProcessor}.
     */
    @Test
    public void testThreadAttributesStampedOnAllSpans() throws ExecutionException, InterruptedException {
        var parentSpan = tracer.spanBuilder("parentSpan").startSpan();
        try (var ignored = parentSpan.makeCurrent()) {
            mes.supplyAsync(taskBean::annotatedTask).get();
        } finally {
            parentSpan.end();
        }

        var spans = exporter.getSpans(ARQUILLIAN_ONLY_NOISE);
        for (SpanData span : spans) {
            assertThat(span.getAttributes().get(AttributeKey.longKey("thread.id")))
                    .as("thread.id must be on span '%s'", span.getName())
                    .isNotNull();
            assertThat(span.getAttributes().get(AttributeKey.stringKey("thread.name")))
                    .as("thread.name must be on span '%s'", span.getName())
                    .isNotNull();
        }
    }

    /**
     * Worker-thread spans must carry a different {@code thread.name} from the submitting
     * thread's spans, proving the task actually ran on a managed executor thread.
     */
    @Test
    public void testWorkerThreadDiffersFromSubmittingThread() throws ExecutionException, InterruptedException {
        var parentSpan = tracer.spanBuilder("parentSpan").startSpan();
        try (var ignored = parentSpan.makeCurrent()) {
            mes.supplyAsync(taskBean::annotatedTask).get();
        } finally {
            parentSpan.end();
        }

        var spans  = exporter.getSpans(ARQUILLIAN_ONLY_NOISE);
        var parent = findSpan(spans, "parentSpan");
        var task   = findSpan(spans, "annotatedTask");

        assertNotNull(parent);
        assertNotNull(task);

        String submittingThread = parent.getAttributes().get(AttributeKey.stringKey("thread.name"));
        String workerThread     = task.getAttributes().get(AttributeKey.stringKey("thread.name"));

        assertThat(workerThread)
                .as("annotatedTask must run on a different (worker) thread than the submitting span")
                .isNotEqualTo(submittingThread);
    }

    // -------------------------------------------------------------------------
    // Context propagation correctness tests
    // -------------------------------------------------------------------------

    /**
     * User span created inside a managed-executor task is a direct child of the submitting span.
     */
    @Test
    public void testManualSpanTaskIsDirectChildOfParent() throws ExecutionException, InterruptedException {
        var parentSpan = tracer.spanBuilder("parentSpan").startSpan();
        try (var ignored = parentSpan.makeCurrent()) {
            mes.supplyAsync(this::manualTask).get();
        } finally {
            parentSpan.end();
        }

        var spans  = exporter.getSpans(ARQUILLIAN_ONLY_NOISE);
        var parent = findSpan(spans, "parentSpan");
        var task   = findSpan(spans, "manualTask");

        assertNotNull(parent);
        assertNotNull(task);
        assertEquals("manualTask must be direct child of parentSpan",
                parent.getSpanId(), task.getParentSpanId());
    }

    /**
     * Context isolation: two consecutive tasks from different parent spans must land on
     * separate traces.
     */
    @Test
    public void testContextIsolationBetweenConsecutiveTasks() throws ExecutionException, InterruptedException {
        var firstParent = tracer.spanBuilder("firstParent").setNoParent().startSpan();
        try (var rootScope = Context.root().makeCurrent(); var ignored = firstParent.makeCurrent()) {
            mes.supplyAsync(taskBean::annotatedTask).get();
        } finally {
            firstParent.end();
        }

        var secondParent = tracer.spanBuilder("secondParent").setNoParent().startSpan();
        try (var rootScope = Context.root().makeCurrent(); var ignored = secondParent.makeCurrent()) {
            mes.supplyAsync(taskBean::annotatedTask).get();
        } finally {
            secondParent.end();
        }

        var spans  = exporter.getSpans(ARQUILLIAN_ONLY_NOISE);
        var first  = findSpan(spans, "firstParent");
        var second = findSpan(spans, "secondParent");

        var taskSpans = spans.stream()
                .filter(s -> "annotatedTask".equals(s.getName()))
                .toList();
        assertEquals("Expect exactly two annotatedTask spans", 2, taskSpans.size());

        var firstTask  = taskSpans.stream().filter(s -> s.getTraceId().equals(first.getTraceId())).findFirst().orElse(null);
        var secondTask = taskSpans.stream().filter(s -> s.getTraceId().equals(second.getTraceId())).findFirst().orElse(null);

        assertNotNull("first annotatedTask must share firstParent's trace", firstTask);
        assertNotNull("second annotatedTask must share secondParent's trace", secondTask);
        assertThat(firstTask.getTraceId()).isNotEqualTo(secondTask.getTraceId());
    }

    /**
     * After a failing task, the context is correctly restored so subsequent tasks are clean.
     */
    @Test
    public void testContextRestoredAfterTaskFailure() throws InterruptedException, ExecutionException {
        var firstParent = tracer.spanBuilder("firstParent").setNoParent().startSpan();
        try (var ignored = firstParent.makeCurrent()) {
            mes.supplyAsync(taskBean::failingAnnotatedTask).exceptionally(t -> null).get();
        } finally {
            firstParent.end();
        }

        var secondParent = tracer.spanBuilder("secondParent").setNoParent().startSpan();
        try (var rootScope = Context.root().makeCurrent(); var ignored = secondParent.makeCurrent()) {
            mes.supplyAsync(taskBean::annotatedTask).get();
        } finally {
            secondParent.end();
        }

        var spans   = exporter.getSpans(ARQUILLIAN_ONLY_NOISE);
        var failing = findSpan(spans, "failingAnnotatedTask");
        var second  = findSpan(spans, "secondParent");
        var success = findSpan(spans, "annotatedTask");

        assertNotNull("failing task span must be exported — cleanup must run even on exception", failing);
        assertNotNull(second);
        assertNotNull(success);

        assertEquals(StatusCode.ERROR, failing.getStatus().getStatusCode());
        assertThat(failing.getTraceId()).isNotEqualTo(success.getTraceId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String manualTask() {
        Span span = tracer.spanBuilder("manualTask").startSpan();
        try (io.opentelemetry.context.Scope s = span.makeCurrent()) {
            return Span.current().getSpanContext().getSpanId();
        } finally {
            span.end();
        }
    }

    private static SpanData findSpan(List<SpanData> spans, String name) {
        return spans.stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
    }

    /** Finds a Payara concurrent waiting span via {@code payara.subsystem = "jakarta-concurrency"} attribute. */
    private static SpanData findWaitingSpan(List<SpanData> spans) {
        return spans.stream()
                .filter(s -> "jakarta-concurrency".equals(
                        s.getAttributes().get(AttributeKey.stringKey("payara.subsystem"))))
                .findFirst().orElse(null);
    }
}
