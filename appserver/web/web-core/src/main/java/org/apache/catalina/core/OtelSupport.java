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
package org.apache.catalina.core;

import fish.payara.opentracing.OtelRouteState;
import fish.payara.opentracing.OpenTelemetryService;
import fish.payara.telemetry.service.PayaraTelemetryConstants;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper that centralises all OpenTelemetry HTTP server instrumentation for
 * {@link StandardWrapper}. All methods are static; this class is never instantiated.
 *
 * <h3>Design constraints</h3>
 * <ul>
 *   <li>Telemetry resolution ({@link OpenTelemetryService#getCurrentTracer()} etc.) must be called
 *       while the web {@code ComponentInvocation} is active — i.e. inside
 *       {@code StandardWrapper.service()} after {@code BEFORE_SERVICE_EVENT}.</li>
 *   <li>The {@code Scope} returned by {@link #startServerSpan} must be closed on the <em>same
 *       thread</em> in a {@code finally} block. It is never stored beyond the handler thread.</li>
 *   <li>All other per-request state (span, context, histogram, start nanos) is stashed in servlet
 *       request attributes so async completion callbacks can use them without re-resolving the
 *       app-mode SDK off-invocation.</li>
 * </ul>
 */
final class OtelSupport {

    private static final Logger LOG = Logger.getLogger(OtelSupport.class.getName());

    private OtelSupport() {
    }

    // -------------------------------------------------------------------------
    // Header extraction
    // -------------------------------------------------------------------------

    /** Extracts OTel trace context from inbound HTTP request headers. */
    static final TextMapGetter<HttpServletRequest> HTTP_HEADER_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest request) {
            return Collections.list(request.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest request, String key) {
            return request == null ? null : request.getHeader(key);
        }
    };

    // -------------------------------------------------------------------------
    // Span start
    // -------------------------------------------------------------------------

    /**
     * Starts the OTel SERVER span for this request, makes it current, and stashes all per-request
     * telemetry state in request attributes. Must be called while the application invocation is
     * active (after {@code BEFORE_SERVICE_EVENT}).
     *
     * <p>Returns {@code null} — a safe no-op for the caller — in three cases:
     * <ul>
     *   <li>OTel is disabled ({@code otelService.isEnabled()} is false)</li>
     *   <li>A parent {@code StandardWrapper.service()} call already owns the span
     *       (forward/include re-entry detected via {@link PayaraTelemetryConstants#PAYARA_OTEL_SERVER_SPAN})</li>
     *   <li>Span creation threw an unexpected exception (telemetry must not break request processing)</li>
     * </ul>
     *
     * @param otelService the application's {@link OpenTelemetryService}
     * @param request     the inbound HTTP request
     * @return the {@link Scope} that {@link #finishServerSpan} will close on this thread,
     *         or {@code null} if this call is not the span owner
     */
    static Scope startServerSpan(OpenTelemetryService otelService, HttpServletRequest request) {
        // First-entry guard: OTel disabled, or a parent service() call already owns the span.
        if (!otelService.isEnabled()
                || request.getAttribute(PayaraTelemetryConstants.PAYARA_OTEL_SERVER_SPAN) != null) {
            return null;
        }
        try {
            Tracer tracer = otelService.getCurrentTracer();
            DoubleHistogram histogram = otelService.getRequestDurationHistogram();

            // Extract remote trace context from inbound headers.
            Context extracted =
                    otelService.getCurrentSdk().getPropagators().getTextMapPropagator()
                            .extract(Context.current(), request, HTTP_HEADER_GETTER);

            String method = normalizeHttpMethod(request.getMethod());

            // Build OtelRouteState with the servlet-layer prefix. Framework layers (JAX-RS etc.)
            // will call setFullRoute / setRoute on this instance via Context.current().
            OtelRouteState routeState = new OtelRouteState();
            routeState.setServletMapping(method, request.getContextPath(), resolveServletBase(request));

            Span span = tracer.spanBuilder(method)   // route unknown yet; refined at span-end
                    .setSpanKind(SpanKind.SERVER)
                    .setParent(extracted)
                    .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, method)
                    .setAttribute(UrlAttributes.URL_SCHEME, request.getScheme())
                    .setAttribute(UrlAttributes.URL_PATH, request.getRequestURI())
                    .setAttribute(ServerAttributes.SERVER_ADDRESS, request.getServerName())
                    .setAttribute(ServerAttributes.SERVER_PORT, (long) request.getServerPort())
                    .startSpan();

            String queryString = request.getQueryString();
            if (queryString != null) {
                span.setAttribute(UrlAttributes.URL_QUERY, queryString);
            }
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                span.setAttribute(UserAgentAttributes.USER_AGENT_ORIGINAL, userAgent);
            }
            String clientAddress = request.getRemoteAddr();
            if (clientAddress != null) {
                span.setAttribute(ClientAttributes.CLIENT_ADDRESS, clientAddress);
            }

            // Fold both the span and the route-state into the context so framework layers
            // can detect ownership (OtelRouteState.fromContext != null) and contribute routes.
            Context spanContext =
                    routeState.storeInContext(extracted.with(span));
            Scope scope = spanContext.makeCurrent();

            // Stash for async completion callbacks (must survive to off-thread listener).
            // setAttribute is safe here: Catalina context is mapped (postParseRequest has run).
            request.setAttribute(PayaraTelemetryConstants.PAYARA_OTEL_SERVER_SPAN, span);
            request.setAttribute(PayaraTelemetryConstants.PAYARA_OTEL_SERVER_CONTEXT, spanContext);
            request.setAttribute(PayaraTelemetryConstants.PAYARA_OTEL_REQUEST_HISTOGRAM, histogram);
            request.setAttribute(PayaraTelemetryConstants.PAYARA_OTEL_START_NANOS, System.nanoTime());

            return scope;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to start OTel SERVER span", e);
            return null;
        }
    }

    /**
     * Returns the low-cardinality servlet base path for use in the route prefix. Prefers the
     * mapping <em>pattern</em> for DEFAULT and EXTENSION match types (where
     * {@code getServletPath()} would expose the full file path) to avoid cardinality explosions.
     */
    private static String resolveServletBase(HttpServletRequest request) {
        var mapping = request.getHttpServletMapping();
        if (mapping != null) {
            var matchType = mapping.getMappingMatch();
            if (matchType != null) {
                switch (matchType) {
                    case PATH:
                    case EXACT:
                        return nullToEmpty(request.getServletPath());
                    default:
                        // DEFAULT / EXTENSION: use pattern (e.g. "/" or "*.jsp") not raw path
                        String pattern = mapping.getPattern();
                        if (pattern != null && !pattern.isEmpty() && !"/".equals(pattern)) {
                            return pattern;
                        }
                        return "";
                }
            }
        }
        return nullToEmpty(request.getServletPath());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // -------------------------------------------------------------------------
    // Span finish
    // -------------------------------------------------------------------------

    /**
     * Closes the handler-thread {@link Scope} and finalises the OTel SERVER span.
     *
     * <p>The {@code scope} parameter is the value returned by {@link #startServerSpan}; passing
     * {@code null} is a safe no-op so the caller needs no guard.
     *
     * <p>The scope is always closed on the calling (handler) thread first — it is thread-local
     * and must never cross threads. Span end and histogram recording then follow:
     * <ul>
     *   <li>Sync requests: ended inline.</li>
     *   <li>Async requests: an {@link AsyncListener} is registered; span end deferred to true
     *       async completion.</li>
     * </ul>
     *
     * @param scope    the scope from {@link #startServerSpan}, or {@code null} (no-op)
     * @param request  the servlet request carrying the stashed telemetry attributes
     * @param response the servlet response (for status code)
     * @param error    a {@link Throwable} if the request ended with an unhandled error, else null
     */
    static void finishServerSpan(Scope scope, HttpServletRequest request,
                                 HttpServletResponse response, Throwable error) {
        // Scope is thread-local — always close it on this thread, regardless of async.
        if (scope != null) {
            scope.close();
        }

        Span span = (Span) request.getAttribute(PayaraTelemetryConstants.PAYARA_OTEL_SERVER_SPAN);
        if (span == null) {
            return;
        }

        if (request.isAsyncStarted()) {
            // Defer to AsyncListener — never end the span on the handler thread for async.
            // Guard against double-registration on async re-dispatch.
            if (request.getAttribute(PayaraTelemetryConstants.PAYARA_OTEL_SERVER_SPAN + ".async") == null) {
                request.setAttribute(PayaraTelemetryConstants.PAYARA_OTEL_SERVER_SPAN + ".async", Boolean.TRUE);
                request.getAsyncContext().addListener(new ServerSpanAsyncListener(request));
            }
            return;
        }

        endAndRecord(span, request, response, error);
    }

    /**
     * Sets final span attributes, ends the span, and records the request-duration histogram.
     * Safe to call from any thread — uses only the already-stashed references; never re-resolves
     * the app-mode SDK.
     */
    static void endAndRecord(Span span, HttpServletRequest request,
                             HttpServletResponse response, Throwable error) {
        try {
            int status = response.getStatus();

            // Read route/span-name from OtelRouteState embedded in the stashed context.
            // Framework layers (JAX-RS etc.) contributed to it via Context.current() during request.
            Context otelContext = (Context) request.getAttribute(
                    PayaraTelemetryConstants.PAYARA_OTEL_SERVER_CONTEXT);
            OtelRouteState routeState = otelContext != null
                    ? OtelRouteState.fromContext(otelContext) : null;

            String method = normalizeHttpMethod(request.getMethod());
            String finalRoute = routeState != null ? routeState.route() : "";
            String spanName  = routeState != null ? routeState.spanName() : method;

            if (!finalRoute.isEmpty()) {
                span.setAttribute(HttpAttributes.HTTP_ROUTE, finalRoute);
            }
            span.updateName(spanName);
            span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, (long) status);
            span.setAttribute(NetworkAttributes.NETWORK_PROTOCOL_NAME, "http");

            if (error != null) {
                span.recordException(error);
                span.setStatus(StatusCode.ERROR);
            } else if (status >= 500) {
                span.setStatus(StatusCode.ERROR);
            }

            span.end();

            // Record histogram using stashed context for exemplar linkage.
            DoubleHistogram histogram = (DoubleHistogram) request.getAttribute(
                    PayaraTelemetryConstants.PAYARA_OTEL_REQUEST_HISTOGRAM);
            Long startNanos = (Long) request.getAttribute(PayaraTelemetryConstants.PAYARA_OTEL_START_NANOS);

            if (histogram != null && startNanos != null) {
                double seconds = (System.nanoTime() - startNanos) * PayaraTelemetryConstants.NANO_CONVERSION;

                AttributesBuilder attrs = Attributes.builder()
                        .put(HttpAttributes.HTTP_REQUEST_METHOD, method)
                        .put(UrlAttributes.URL_SCHEME, request.getScheme())
                        .put(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, (long) status);
                if (!finalRoute.isEmpty()) {
                    attrs.put(HttpAttributes.HTTP_ROUTE, finalRoute);
                }
                if (status >= 500) {
                    // error.type omitted when there is no error (never set to empty string)
                    attrs.put(ErrorAttributes.ERROR_TYPE, Integer.toString(status));
                }

                histogram.record(seconds, attrs.build(),
                        otelContext != null ? otelContext : Context.current());
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to finish OTel SERVER span", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static final Set<String> KNOWN_HTTP_METHODS = Set.of(
            "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE", "CONNECT");

    /** Normalises the HTTP method to a known semconv value; unknown methods → {@code "HTTP"}. */
    static String normalizeHttpMethod(String method) {
        if (method == null) return "HTTP";
        String upper = method.toUpperCase();
        return KNOWN_HTTP_METHODS.contains(upper) ? upper : "HTTP";
    }

    // -------------------------------------------------------------------------
    // Async listener
    // -------------------------------------------------------------------------

    /**
     * Completes the OTel SERVER span and histogram at true async request completion.
     * Re-registers itself in {@link #onStartAsync} so async re-dispatches don't lose it.
     * Uses an {@link AtomicBoolean} to guarantee exactly-once span end across
     * concurrent timeout+complete or error+complete races.
     */
    static final class ServerSpanAsyncListener implements AsyncListener {

        private final HttpServletRequest request;
        private final AtomicBoolean ended = new AtomicBoolean();

        ServerSpanAsyncListener(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            end(event, null);
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            end(event, null);
        }

        @Override
        public void onError(AsyncEvent event) {
            end(event, event.getThrowable());
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            // Re-register so the listener survives async re-dispatch
            event.getAsyncContext().addListener(this);
        }

        private void end(AsyncEvent event, Throwable error) {
            if (ended.compareAndSet(false, true)) {
                HttpServletResponse response = (HttpServletResponse) event.getSuppliedResponse();
                if (response != null) {
                    Span span = (Span) request.getAttribute(PayaraTelemetryConstants.PAYARA_OTEL_SERVER_SPAN);
                    endAndRecord(span, request, response, error);
                }
            }
        }
    }
}
