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
package fish.payara.microprofile.telemetry.tracing;

import fish.payara.opentracing.OpenTelemetryService;
import fish.payara.opentracing.OtelRouteState;
import fish.payara.opentracing.PropagationHelper;
import fish.payara.requesttracing.jaxrs.client.PayaraTracingServices;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import io.opentelemetry.semconv.ExceptionAttributes;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import org.glassfish.api.invocation.InvocationManager;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interceptor for CDI beans that adds tracing spans using OpenTelemetry.
 */
public class WithSpanMethodInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(WithSpanMethodInterceptor.class.getName());

    private static final Class[] HTTP_METHODS = //
            new Class[] {GET.class, POST.class, DELETE.class, PUT.class, HEAD.class, PATCH.class, OPTIONS.class};

    public WithSpanMethodInterceptor() {
    }

    @AroundInvoke
    public Object withSpanCdiCall(final InvocationContext invocationContext) throws Exception {
        LOG.fine(() -> "withSpanCdiCall(" + invocationContext + ")");
        // Get the required HK2 services
        final PayaraTracingServices payaraTracingServices = new PayaraTracingServices();
        final OpenTelemetryService openTelemetryService = payaraTracingServices.getOpenTelemetryService();
        final InvocationManager invocationManager = payaraTracingServices.getInvocationManager();

        // extract the span attribute annotation from the method
        var builder = Attributes.builder();
        extractSpanAttributes(invocationContext, builder);
        var attributes = builder.build();

        if (openTelemetryService == null || !openTelemetryService.isEnabled()) {
            return invocationContext.proceed();
        }

        // JAX-RS methods are traced by the container/client filters — skip.
        if (isJaxRsMethod(invocationContext)) {
            Span.current().setAllAttributes(attributes);
            return invocationContext.proceed();
        }

        // JAX-WS methods already have a SERVER span created by the JAX-WS infrastructure.
        // Enrich that span with the @WithSpan name override and @SpanAttribute parameters
        // instead of creating a redundant child span.
        if (isWebServiceMethod(invocationContext, invocationManager)) {
            enrichCurrentSpan(invocationContext.getMethod(), attributes);
            return invocationContext.proceed();
        }

        // CDI bean method: check for @WithSpan and create a new span.
        final WithSpan withSpan = invocationContext.getMethod().getAnnotation(WithSpan.class);

        if (withSpan == null) {
            LOG.finest("No @WithSpan annotation found on method, proceeding without creating a span.");
            return invocationContext.proceed();
        }

        if (WithSpanEnabledLookup.isDisabled(invocationContext.getMethod())) {
            LOG.finest("Tracing is disabled for this method via configuration, proceeding without creating a span.");
            return invocationContext.proceed();
        }

        SpanBuilder spanBuilder = openTelemetryService.getCurrentTracer()
                .spanBuilder(getWithSpanValue(invocationContext, withSpan))
                .setSpanKind(withSpan.kind())
                .setAllAttributes(attributes);
        spanBuilder.setParent(Context.current());
        final var span = spanBuilder.startSpan();

        if (invocationContext.getMethod().getReturnType().equals(CompletionStage.class)) {
            return handleAsyncInvocation(invocationContext, span);
        }
        return handleSyncInvocation(invocationContext, span);
    }

    /**
     * Helper method that determines if the annotated method is a JaxRs method or not by inspecting it for HTTP Method
     * annotations. JaxRs method tracing is handled by the Client/Container filters, so we don't process them using this
     * interceptor.
     *
     * @param invocationContext The invocation context from the AroundInvoke method.
     * @return True if the method is a JaxRs method.
     */
    @SuppressWarnings({"unchecked", "rawtypes"}) // safe here.
    private boolean isJaxRsMethod(InvocationContext invocationContext) {
        // Check if any of the HTTP Method annotations are present on the intercepted method
        for (Class httpMethod : HTTP_METHODS) {
            if (invocationContext.getMethod().isAnnotationPresent(httpMethod)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns {@code true} if the intercepted method is currently executing as a JAX-WS web service method.
     * The SERVER span for JAX-WS is owned by the JAX-WS infrastructure; the interceptor enriches it
     * rather than creating a new child span.
     */
    private boolean isWebServiceMethod(InvocationContext invocationContext, InvocationManager invocationManager) {
        return invocationContext.getMethod().equals(invocationManager.peekWebServiceMethod());
    }

    /**
     * Enriches the current JAX-WS SERVER span with {@link WithSpan} name and {@link SpanAttribute} parameters.
     *
     * <p>The SERVER span is already created by the JAX-WS infrastructure. If the method carries
     * {@link WithSpan} with a non-empty value, the span name is updated. Parameter attributes are
     * always applied when {@link WithSpan} is present and tracing is not disabled via config.
     *
     * <p>Two span-ownership paths are handled:
     * <ul>
     *   <li><b>Servlet endpoint</b> ({@code StandardWrapper} owns the span): an
     *       {@link OtelRouteState} is present in {@link Context#current()}. The name override is
     *       written there so that {@code OtelSupport.endAndRecord} applies it last — calling
     *       {@link Span#updateName} directly would be overwritten at span-end.</li>
     *   <li><b>EJB endpoint</b> (deferred span owned by {@link fish.payara.opentracing.OpenTelemetryService}):
     *       no {@code OtelRouteState} is present; {@link Span#updateName} is called directly.</li>
     * </ul>
     *
     * @param method     the intercepted business method
     * @param attributes span attributes extracted from {@link SpanAttribute}-annotated parameters
     */
    private void enrichCurrentSpan(Method method, Attributes attributes) {
        WithSpan withSpan = method.getAnnotation(WithSpan.class);
        if (withSpan != null && !WithSpanEnabledLookup.isDisabled(method)) {
            String name = withSpan.value();
            if (!name.isEmpty()) {
                OtelRouteState routeState = OtelRouteState.fromContext(Context.current());
                if (routeState != null) {
                    // Servlet path: StandardWrapper reads spanName() at span-end via OtelSupport.endAndRecord.
                    routeState.overrideSpanName(name);
                } else {
                    // EJB path: the deferred span is already the current span; update it directly.
                    Span.current().updateName(name);
                }
            }
        }
        // Always apply @SpanAttribute parameter attributes — mirrors JAX-RS behaviour where
        // parameter attributes are set on the SERVER span regardless of @WithSpan presence.
        Span.current().setAllAttributes(attributes);
    }

    /**
     * Returns the value of the {@link WithSpan} annotation on the intercepted method,
     * or the default value if the annotation is not present or its value is empty.
     *
     * @param invocationContext the context of the intercepted method invocation
     * @param withSpan the {@link WithSpan} annotation that may be present on the intercepted method
     * @return the value of the {@link WithSpan} annotation on the intercepted method,
     *         or the default value if the annotation is not present or its value is empty
     */
    private String getWithSpanValue(final InvocationContext invocationContext, final WithSpan withSpan) {
        final String withSpanValue = withSpan.value();
        if (withSpanValue.isEmpty()) {
            if (invocationContext.getMethod().getDeclaringClass().getName().contains("$")) {
                return invocationContext.getMethod().getDeclaringClass().getSimpleName()
                        + "." + invocationContext.getMethod().getName();
            } else {
                return invocationContext.getMethod().getDeclaringClass().getCanonicalName()
                        + "." + invocationContext.getMethod().getName();
            }
        }
        return withSpanValue;
    }

    /**
     *
     * Extracts span attributes from the given {@code invocationContext} and adds them to the given {@code builder}.
     * Uses a {@code MethodSpanAttributesExtractor} to retrieve the attributes from the method signature and annotations
     * on its parameters.
     * @param invocationContext the invocation context to extract the span attributes from
     * @param builder the attributes builder to add the extracted attributes to
     */
    private void extractSpanAttributes(final InvocationContext invocationContext, final AttributesBuilder builder) {
        MethodSpanAttributesExtractor<InvocationContext, Void> extractor = MethodSpanAttributesExtractor
                .create(
                        InvocationContext::getMethod,
                        (m, p) -> Arrays.stream(m.getParameters())
                                .map(v -> {
                                    SpanAttribute spanAttribute = v.getAnnotation(SpanAttribute.class);
                                    // we ignore unspecified argument name (compiled with -parameter) or spanAttribute value
                                    var name = v.isNamePresent() ? v.getName() : null;
                                    return spanAttribute != null ? spanAttribute.value().trim() : name;
                                }).toArray(String[]::new),
                        InvocationContext::getParameters);
        extractor.onStart(builder, Context.current(), invocationContext);
    }

    private Object handleSyncInvocation(InvocationContext invocationContext, Span span) throws Exception {
        try (var ignore = PropagationHelper.start(span, Context.current())) {
            try {
                return invocationContext.proceed();
            } catch (final Exception ex) {
                markSpanAsFailed(span, ex);
                throw ex;
            }
        }
    }

    private Object handleAsyncInvocation(InvocationContext invocationContext, Span span) throws Exception {
        var helper = PropagationHelper.startMultiThreaded(span, Context.current());
        CompletionStage<?> future = (CompletionStage<?>) invocationContext.proceed();
            return future.whenComplete((value, ex) -> {
                if (ex != null) {
                    markSpanAsFailed(helper.span(), ex);
                }
                helper.end();
                helper.close();
            });
    }

    private void markSpanAsFailed(Span span, Throwable ex) {
        LOG.log(Level.FINEST, "Setting the error to the active span ...", ex);
        span.setStatus(StatusCode.ERROR, ex.getMessage());
        span.setAttribute(ExceptionAttributes.EXCEPTION_TYPE, ex.getClass().getName());
        span.addEvent(ExceptionAttributes.EXCEPTION_TYPE.toString(),
                Attributes.of(ExceptionAttributes.EXCEPTION_MESSAGE, ex.getMessage()));
        span.recordException(ex);
    }
}
