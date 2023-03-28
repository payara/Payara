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
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
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
import fish.payara.opentracing.PropagationHelper;
import fish.payara.requesttracing.jaxrs.client.PayaraTracingServices;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
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

import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interceptor for CDI beans that adds tracing spans using OpenTelemetry.
 */
public class WithSpanMethodInterceptor {

    private static final Logger LOG = Logger.getLogger(WithSpanMethodInterceptor.class.getName());

    private static final Class[] HTTP_METHODS = //
            new Class[] {GET.class, POST.class, DELETE.class, PUT.class, HEAD.class, PATCH.class, OPTIONS.class};

    private final BeanManager bm;

    public WithSpanMethodInterceptor(final BeanManager bm) {
        this.bm = bm;
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

        // If Request Tracing is enabled, and this isn't a JaxRs method
        if (openTelemetryService == null || !openTelemetryService.isEnabled() //
                || isJaxRsMethod(invocationContext) || isWebServiceMethod(invocationContext, invocationManager)) {
            // If request tracing was turned off, or this is a JaxRs method, just carry on
            LOG.finest("The call is already monitored by some different component, proceeding the invocation.");
            var currentSpan = Span.current();
            currentSpan.setAllAttributes(attributes);
            return invocationContext.proceed();
        }

        // Get the WithSpan annotation present on the method
        final WithSpan withSpan = OpenTracingCdiUtils.getAnnotation(bm, WithSpan.class, invocationContext);

        // If we *have* been told to, get the application's Tracer instance and start an active span.
        SpanBuilder spanBuilder = openTelemetryService.getCurrentTracer()
                .spanBuilder(getWithSpanValue(invocationContext, withSpan))
                .setSpanKind(getWithSpanKind(invocationContext, withSpan))
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
            if (OpenTracingCdiUtils.getAnnotation(bm, httpMethod, invocationContext) != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method that determines if the annotated method is the monitored JAX-WS method or not by inspecting the invocation manager.
     * <p>
     * JaxWs method tracing is handled by the JAX-WS monitoring pipe/tube, so we don't process them using this
     * interceptor.
     *
     * @param invocationContext The invocation context from the AroundInvoke method.
     * @param invocationManager The current invocation manager for this thread
     * @return True if the method is a JaxRs method.
     */
    private boolean isWebServiceMethod(InvocationContext invocationContext, InvocationManager invocationManager) {
        return invocationContext.getMethod().equals(invocationManager.peekWebServiceMethod());
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
        final String withSpanValue = OpenTracingCdiUtils
                .getConfigOverrideValue(WithSpan.class, "value", invocationContext, String.class)
                .orElse(withSpan.value());
        if (withSpanValue.isEmpty()) {
            return invocationContext.getMethod().getDeclaringClass().getCanonicalName()
                    + "." + invocationContext.getMethod().getName();
        }
        return withSpanValue;
    }

    /**
     * Returns the SpanKind of the {@link WithSpan} annotation on the intercepted method,
     * or the default value if the annotation is not present.
     *
     * @param invocationContext The context of the intercepted method invocation.
     * @param withSpan The {@link WithSpan} annotation that may be present on the intercepted method.
     * @return The SpanKind of the {@link WithSpan} annotation on the intercepted method,
     *         or the default value if the annotation is not present.
     */
    private SpanKind getWithSpanKind(final InvocationContext invocationContext, final WithSpan withSpan) {
        return OpenTracingCdiUtils.getConfigOverrideValue(WithSpan.class, "kind", invocationContext, SpanKind.class)
                .orElse(withSpan.kind());
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
                .newInstance(
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
        span.setAttribute("error", true);
        span.setAttribute(SemanticAttributes.EXCEPTION_TYPE, Throwable.class.getName());
        span.addEvent(SemanticAttributes.EXCEPTION_EVENT_NAME,
                Attributes.of(SemanticAttributes.EXCEPTION_MESSAGE, ex.getMessage()));
        span.recordException(ex);
    }
}
