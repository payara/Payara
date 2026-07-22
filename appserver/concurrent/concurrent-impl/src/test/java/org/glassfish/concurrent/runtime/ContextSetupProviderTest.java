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
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.concurrent.runtime;

import com.sun.enterprise.deployment.JndiNameEnvironment;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.concurro.spi.ContextHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ContextSetupProviderImpl} using test doubles and real OpenTelemetry SDK.
 * Validates context save/setup/reset lifecycle and the {@link OtelContextProvider}
 * waiting-span behaviour without reflection or mocking.
 */
public class ContextSetupProviderTest {

    private static final String POOL_NAME = "concurrent/__defaultManagedExecutorService";

    private OpenTelemetrySdk sdk;
    private Tracer tracer;
    private TestSpanProcessor spanProcessor;
    private ContextSetupProviderImpl provider;
    private TestMonitoringFacade monitoringFacade;
    private TestInvocationFacade invocationFacade;

    @Before
    public void setUp() {
        spanProcessor = new TestSpanProcessor();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .build();
        sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        io.opentelemetry.api.GlobalOpenTelemetry.resetForTest();
        io.opentelemetry.api.GlobalOpenTelemetry.set(sdk);
        tracer = sdk.getTracer("test-tracer");

        monitoringFacade = new TestMonitoringFacade(tracer, sdk);
        invocationFacade = new TestInvocationFacade();

        provider = new ContextSetupProviderImpl(
                invocationFacade,
                monitoringFacade,
                POOL_NAME,
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet()
        );
    }

    @After
    public void tearDown() {
        if (sdk != null) {
            sdk.close();
        }
    }

    /**
     * After {@code setup()}, the parent span should be current on the executor thread.
     * After {@code reset()}, the context should be restored to what it was before.
     */
    @Test
    public void testContextRestoredAfterSetupAndReset() {
        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (io.opentelemetry.context.Scope parentScope = parentSpan.makeCurrent()) {
            // Save context on submitting thread
            ContextHandle handle = provider.saveContext(null);

            // Simulate executor thread: no context yet
            Span beforeSetup = Span.current();

            // Setup: restores parent context + emits waiting span
            ContextHandle restorerHandle = provider.setup(handle);

            // After setup, Span.current() should be the parent span
            assertEquals("Parent span should be current after setup",
                    parentSpan.getSpanContext().getSpanId(),
                    Span.current().getSpanContext().getSpanId());

            // The waiting span should have been emitted and ended already
            assertEquals("Waiting span should have been emitted",
                    1, spanProcessor.waitingSpans.size());
            assertEquals("Waiting span name should match pool name",
                    "concurrent/" + POOL_NAME + " wait",
                    spanProcessor.waitingSpans.get(0).getName());
            assertEquals("Waiting span parent should be the original parent span",
                    parentSpan.getSpanContext().getSpanId(),
                    spanProcessor.waitingSpans.get(0).getParentSpanId());

            // Reset: should restore context to state before setup
            provider.reset(restorerHandle);
        } finally {
            parentSpan.end();
        }
    }

    /**
     * Nested setup/reset: the inner context is cleaned up first, then the outer.
     * Verifies no context leakage between the two levels.
     */
    @Test
    public void testNestedContextSetupSafety() {
        Span outer = tracer.spanBuilder("outer").startSpan();
        try (io.opentelemetry.context.Scope outerScope = outer.makeCurrent()) {
            ContextHandle outerHandle = provider.saveContext(null);
            ContextHandle outerRestorer = provider.setup(outerHandle);

            // After outer setup, outer span is current
            assertEquals(outer.getSpanContext().getSpanId(),
                    Span.current().getSpanContext().getSpanId());

            // Nested: create an inner span and save a new context on top
            Span inner = tracer.spanBuilder("inner").startSpan();
            try (io.opentelemetry.context.Scope innerScope = inner.makeCurrent()) {
                ContextHandle innerHandle = provider.saveContext(null);
                ContextHandle innerRestorer = provider.setup(innerHandle);

                assertEquals("Inner span should be current after inner setup",
                        inner.getSpanContext().getSpanId(),
                        Span.current().getSpanContext().getSpanId());

                provider.reset(innerRestorer);
            } finally {
                inner.end();
            }

            // Two waiting spans should have been emitted (one per setup call)
            assertEquals("Two waiting spans should be emitted for nested setups",
                    2, spanProcessor.waitingSpans.size());

            provider.reset(outerRestorer);
        } finally {
            outer.end();
        }
    }

    /**
     * When no span is active at save time, setup should still restore a valid (root) context
     * and emit no waiting span (since there is no parent to attach it to).
     */
    @Test
    public void testSetupWithNoActiveSpan() {
        // No span is current
        assertFalse("No span should be active", Span.current().getSpanContext().isValid());

        ContextHandle handle = provider.saveContext(null);
        ContextHandle restorerHandle = provider.setup(handle);

        // Still no active span
        assertFalse("Still no span should be active after setup without parent",
                Span.current().getSpanContext().isValid());

        // No waiting span should be emitted when there was no parent
        assertTrue("No waiting span should be emitted when no parent exists",
                spanProcessor.waitingSpans.isEmpty());

        provider.reset(restorerHandle);
    }

    /**
     * Waiting span attributes: payara.subsystem, thread.id, thread.name, scheduling.wait_ms.
     */
    @Test
    public void testWaitingSpanAttributes() {
        Span parent = tracer.spanBuilder("parent").startSpan();
        try (io.opentelemetry.context.Scope scope = parent.makeCurrent()) {
            ContextHandle handle = provider.saveContext(null);
            ContextHandle restorerHandle = provider.setup(handle);
            provider.reset(restorerHandle);
        } finally {
            parent.end();
        }

        assertEquals(1, spanProcessor.waitingSpans.size());
        var attrs = spanProcessor.waitingSpans.get(0).getAttributes();
        assertEquals("payara.subsystem must be 'jakarta-concurrency'", "jakarta-concurrency",
                attrs.get(io.opentelemetry.api.common.AttributeKey.stringKey("payara.subsystem")));
        assertNotNull("thread.name must be set",
                attrs.get(io.opentelemetry.api.common.AttributeKey.stringKey("thread.name")));
        assertNotNull("thread.id must be set",
                attrs.get(io.opentelemetry.api.common.AttributeKey.longKey("thread.id")));
        assertNotNull("scheduling.wait_ms must be set",
                attrs.get(io.opentelemetry.api.common.AttributeKey.doubleKey("scheduling.wait_ms")));
    }

    // -------------------------------------------------------------------------
    // Test doubles
    // -------------------------------------------------------------------------

    private static class TestMonitoringFacade implements MonitoringFacade {
        private final Tracer tracer;
        private final OpenTelemetrySdk sdk;

        TestMonitoringFacade(Tracer tracer, OpenTelemetrySdk sdk) {
            this.tracer = tracer;
            this.sdk = sdk;
        }

        @Override public Tracer getTracer() { return tracer; }
        @Override public io.opentelemetry.api.OpenTelemetry getOpenTelemetry() { return sdk; }
        @Override public boolean isRequestTracingEnabled() { return false; }
        @Override public void endTrace() {}
        @Override public void registerStuckThread(long threadId) {}
        @Override public void deregisterStuckThread(long threadId) {}
    }

    private static class TestInvocationFacade implements InvocationFacade {
        @Override public ComponentInvocation getCurrentInvocation() { return null; }
        @Override public JndiNameEnvironment getJndiNameEnvironment(String componentId) { return null; }
        @Override public void preInvoke(ComponentInvocation invocation) {}
        @Override public void postInvoke(ComponentInvocation invocation) {}
        @Override public void cleanupTransaction(boolean canCommitOrRollback) {}
        @Override public boolean isApplicationEnabled(String appName) { return true; }
        @Override public boolean isContextService() { return false; }
        @Override public ClassLoader getContextClassLoader(String appName) { return null; }
        @Override public Transaction suspend() throws SystemException { return null; }
        @Override public void resume(Transaction suspendedTxn) throws SystemException, InvalidTransactionException {}
    }

    /** Collects all waiting spans ({@code concurrent/* wait}) for assertion. */
    private static class TestSpanProcessor implements SpanProcessor {
        final List<SpanData> waitingSpans = new ArrayList<>();

        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {}

        @Override
        public boolean isStartRequired() { return false; }

        @Override
        public void onEnd(ReadableSpan span) {
            String name = span.getName();
            if (name.startsWith("concurrent/") && name.endsWith(" wait")) {
                waitingSpans.add(span.toSpanData());
            }
        }

        @Override public boolean isEndRequired() { return true; }
        @Override public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        @Override public CompletableResultCode forceFlush() { return CompletableResultCode.ofSuccess(); }
    }
}
