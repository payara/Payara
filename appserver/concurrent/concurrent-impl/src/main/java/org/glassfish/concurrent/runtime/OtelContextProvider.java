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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the
 * "Classpath" exception as provided by the Payara Foundation in the GPL
 * Version 2 section of the License file that accompanied this code.
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
 * only if the new code is made subject to such option by the copyright holder.
 */
package org.glassfish.concurrent.runtime;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.enterprise.concurrent.spi.ThreadContextProvider;
import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Built-in Payara {@link ThreadContextProvider} that propagates the active OpenTelemetry
 * {@link Context} to the worker thread and, on the worker thread, creates a short-lived
 * {@code INTERNAL} <em>waiting span</em> capturing queue latency before restoring the
 * original context for the task body.
 *
 * <h3>Trace shape</h3>
 * <pre>
 * parentSpan (submitting thread)
 *   ├── concurrent/&lt;pool&gt; wait  [ends at task pickup; duration = queue wait time]
 *   ├── @WithSpan taskSpan      [direct child of parentSpan, on worker thread]
 *   └── downstream HTTP/DB span [direct child of parentSpan, on worker thread]
 * </pre>
 *
 * <p>The waiting span ends <em>before</em> {@link Context#makeCurrent()} is called,
 * so it is never the parent of user-created spans.
 *
 * <h3>Serialization / Passivation</h3>
 * <p>The inner snapshot classes ({@link WaitingSpanSnapshot}, {@link RestoreOnlySnapshot},
 * {@link NoopSnapshot}) implement {@link Serializable} solely to support EJB
 * <em>passivation</em>. This occurs when a contextual proxy created via
 * {@link jakarta.enterprise.concurrent.ContextService#createContextualProxy} is held
 * inside a {@code @Stateful} EJB and the container decides to passivate that bean.
 *
 * <p>Constraints and assumptions:
 * <ul>
 *   <li>Passivation is <em>same-JVM</em> only — there are no remote calls or rolling
 *       upgrades involved. The instance is serialized to disk and deserialized back by
 *       the same JVM with the same classloader.</li>
 *   <li>Transient fields ({@code monitoringFacade}, {@code parentContext}) are valid at
 *       serialization time and are used by {@code writeObject} to encode the OTel context
 *       as W3C trace-context headers. After deserialization they are null and are
 *       re-obtained from {@link ConcurrentRuntime} lazily in {@code begin()}.</li>
 *   <li>Queue-latency timing ({@code submitNanos}) is meaningless after a passivation
 *       cycle; {@code begin()} detects this (transient {@code monitoringFacade} is null)
 *       and silently skips the waiting span, only restoring the OTel context.</li>
 * </ul>
 *
 * <p>This provider is NOT registered via the ServiceLoader — it is a hardcoded built-in
 * added by {@link ContextSetupProviderImpl} and always runs regardless of the executor's
 * context-type propagation configuration.
 */
class OtelContextProvider implements ThreadContextProvider {

    private static final Logger LOG = Logger.getLogger(OtelContextProvider.class.getName());

    static final String CONTEXT_TYPE = "Payara.OpenTelemetry";

    private final MonitoringFacade monitoringFacade;
    private final String poolName;

    OtelContextProvider(MonitoringFacade monitoringFacade, String poolName) {
        this.monitoringFacade = monitoringFacade;
        this.poolName = poolName;
    }

    @Override
    public String getThreadContextType() {
        return CONTEXT_TYPE;
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        if (monitoringFacade.getOpenTelemetry() == null) {
            return NoopSnapshot.INSTANCE;
        }
        Span current = Span.current();
        if (!current.getSpanContext().isValid()) {
            return new RestoreOnlySnapshot(Context.current(), monitoringFacade);
        }
        return new WaitingSpanSnapshot(System.nanoTime(), Context.current(), poolName, monitoringFacade);
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return NoopSnapshot.INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Snapshot: captures submit time + parent context; creates waiting span on begin()
    // -------------------------------------------------------------------------

    static final class WaitingSpanSnapshot implements ThreadContextSnapshot, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final long submitNanos;
        private final String poolName;
        /** W3C headers encoding the parent OTel context; null in normal use, set by writeObject. */
        private Map<String, String> w3cHeaders;

        /** Direct context reference — transient, valid at write time, null after deserialization. */
        private transient Context parentContext;
        /** Monitoring facade — transient, valid at write time, null after deserialization. */
        private transient MonitoringFacade monitoringFacade;

        WaitingSpanSnapshot(long submitNanos, Context parentContext, String poolName,
                MonitoringFacade monitoringFacade) {
            this.submitNanos = submitNanos;
            this.parentContext = parentContext;
            this.poolName = poolName;
            this.monitoringFacade = monitoringFacade;
        }

        @Serial
        private void writeObject(ObjectOutputStream out) throws IOException {
            // Encode the OTel context as W3C headers so it survives passivation.
            // monitoringFacade and parentContext are transient but still valid at write time.
            w3cHeaders = new HashMap<>();
            if (monitoringFacade != null && parentContext != null) {
                OpenTelemetry otel = monitoringFacade.getOpenTelemetry();
                if (otel != null) {
                    otel.getPropagators().getTextMapPropagator()
                            .inject(parentContext, w3cHeaders, Map::put);
                }
            }
            out.defaultWriteObject();
        }

        @Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            // monitoringFacade and parentContext are transient → null after deserialization.
            // They are resolved lazily in begin().
        }

        @Override
        public ThreadContextRestorer begin() {
            // Resolve monitoring facade — transient field is null after passivation/activation.
            MonitoringFacade mf = this.monitoringFacade != null
                    ? this.monitoringFacade
                    : ConcurrentRuntime.getRuntime().getMonitoringFacade();
            OpenTelemetry otel = mf != null ? mf.getOpenTelemetry() : null;
            if (otel == null) {
                return NoopSnapshot.NOOP_RESTORER;
            }

            // Resolve parent context — direct ref if available, W3C extraction after passivation.
            Context ctx = parentContext != null
                    ? parentContext
                    : extractFromHeaders(otel);

            // Create and immediately end the waiting span.
            // It ends BEFORE makeCurrent() so user spans remain direct children of parentContext.
            boolean wasPassivated = (this.monitoringFacade == null);
            if (!wasPassivated && mf.getTracer() != null) {
                try {
                    long queueNanos = System.nanoTime() - submitNanos;
                    Span waitingSpan = mf.getTracer()
                            .spanBuilder("concurrent/" + poolName + " wait")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setParent(ctx)
                            .setStartTimestamp(submitNanos, TimeUnit.NANOSECONDS)
                            .setAttribute("payara.subsystem", "jakarta-concurrency")
                            .setAttribute("thread.id", Thread.currentThread().threadId())
                            .setAttribute("thread.name", Thread.currentThread().getName())
                            .setAttribute("scheduling.wait_ms", queueNanos / 1_000_000.0)
                            .startSpan();
                    waitingSpan.end();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to create OTel waiting span for pool " + poolName, e);
                }
            }

            // Make the original parent context current on this thread.
            Scope scope = ctx.makeCurrent();
            return scope::close;
        }

        private Context extractFromHeaders(OpenTelemetry otel) {
            if (w3cHeaders == null || w3cHeaders.isEmpty()) {
                return Context.root();
            }
            return otel.getPropagators().getTextMapPropagator()
                    .extract(Context.root(), w3cHeaders, new TextMapGetter<Map<String, String>>() {
                        @Override
                        public Iterable<String> keys(Map<String, String> carrier) {
                            return carrier.keySet();
                        }
                        @Override
                        public String get(Map<String, String> carrier, String key) {
                            return carrier == null ? null : carrier.get(key);
                        }
                    });
        }
    }

    // -------------------------------------------------------------------------
    // Snapshot: only restores context, no waiting span (no active span at submit)
    // -------------------------------------------------------------------------

    static final class RestoreOnlySnapshot implements ThreadContextSnapshot, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Map<String, String> w3cHeaders;

        private transient Context parentContext;
        private transient MonitoringFacade monitoringFacade;

        RestoreOnlySnapshot(Context parentContext, MonitoringFacade monitoringFacade) {
            this.parentContext = parentContext;
            this.monitoringFacade = monitoringFacade;
        }

        @Serial
        private void writeObject(ObjectOutputStream out) throws IOException {
            w3cHeaders = new HashMap<>();
            if (monitoringFacade != null && parentContext != null) {
                OpenTelemetry otel = monitoringFacade.getOpenTelemetry();
                if (otel != null) {
                    otel.getPropagators().getTextMapPropagator()
                            .inject(parentContext, w3cHeaders, Map::put);
                }
            }
            out.defaultWriteObject();
        }

        @Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
        }

        @Override
        public ThreadContextRestorer begin() {
            Context ctx;
            if (parentContext != null) {
                ctx = parentContext;
            } else if (w3cHeaders != null && !w3cHeaders.isEmpty()) {
                MonitoringFacade mf = ConcurrentRuntime.getRuntime().getMonitoringFacade();
                OpenTelemetry otel = mf != null ? mf.getOpenTelemetry() : null;
                if (otel == null) return NoopSnapshot.NOOP_RESTORER;
                ctx = otel.getPropagators().getTextMapPropagator()
                        .extract(Context.root(), w3cHeaders, new TextMapGetter<Map<String, String>>() {
                            @Override
                            public Iterable<String> keys(Map<String, String> carrier) {
                                return carrier.keySet();
                            }
                            @Override
                            public String get(Map<String, String> carrier, String key) {
                                return carrier == null ? null : carrier.get(key);
                            }
                        });
            } else {
                return NoopSnapshot.NOOP_RESTORER;
            }
            Scope scope = ctx.makeCurrent();
            return scope::close;
        }
    }

    // -------------------------------------------------------------------------
    // No-op snapshot for cleared or disabled OTel
    // -------------------------------------------------------------------------

    static final class NoopSnapshot implements ThreadContextSnapshot, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        static final NoopSnapshot INSTANCE = new NoopSnapshot();

        static final ThreadContextRestorer NOOP_RESTORER = new NoopRestorer();

        @Override
        public ThreadContextRestorer begin() {
            return NOOP_RESTORER;
        }

        /** Serializable no-op restorer. */
        static final class NoopRestorer implements ThreadContextRestorer, Serializable {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void endContext() {
                // nothing to restore
            }
        }
    }
}
