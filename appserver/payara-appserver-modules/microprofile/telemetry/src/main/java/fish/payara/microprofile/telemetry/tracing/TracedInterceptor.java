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
package fish.payara.microprofile.telemetry.tracing;

import fish.payara.microprofile.telemetry.tracing.jaxrs.OpenTracingCdiUtils;
import fish.payara.requesttracing.jaxrs.client.PayaraTracingServices;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.invocation.InvocationManager;

import static fish.payara.microprofile.telemetry.tracing.jaxrs.OpenTracingCdiUtils.getAnnotation;

@Interceptor
@Traced
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class TracedInterceptor implements Serializable {

    private static final long serialVersionUID = -7772038254542420659L;
    
    private static final Logger LOG = Logger.getLogger(TracedInterceptor.class.getName());

    @SuppressWarnings("rawtypes")
    private static final Class[] HTTP_METHODS = //
            new Class[] {GET.class, POST.class, DELETE.class, PUT.class, HEAD.class, PATCH.class, OPTIONS.class};
    
    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object traceCdiCall(final InvocationContext invocationContext) throws Exception {
        LOG.fine(() -> "traceCdiCall(" + invocationContext + ")");
        final PayaraTracingServices payaraTracingServices = new PayaraTracingServices();
        final InvocationManager invocationManager = payaraTracingServices.getInvocationManager();

        if (!payaraTracingServices.isTracingAvailable() || isJaxRsMethod(invocationContext) || isWebServiceMethod(invocationContext, invocationManager)) {
            LOG.finest("The call is already monitored by some different component, proceeding the invocation.");
            return invocationContext.proceed();
        }

        final Traced traced = getAnnotation(beanManager, Traced.class, invocationContext);

        // Get the enabled (value) variable from a config override, or from the annotation if there is no override
        final boolean tracingEnabled = OpenTracingCdiUtils
                .getConfigOverrideValue(Traced.class, "value", invocationContext, boolean.class)
                .orElse(traced.value());

        // If we've explicitly been told not to trace the method: don't!
        if (!tracingEnabled) {
            LOG.finest("Tracing is not enabled, nothing to do.");
            return invocationContext.proceed();
        }

        final String applicationName = payaraTracingServices.getApplicationName();
        final Tracer tracer = payaraTracingServices.getActiveTracer();
        final String operationName = getOperationName(invocationContext, traced);

        Span span = Span.current();
        if (span.isRecording()) {
            span = span.updateName(operationName).setAttribute("otel.service.name", applicationName);
            try (Scope scope = span.makeCurrent()) {
                try {
                    return invocationContext.proceed();
                } catch (Exception e) {
                    LOG.log(Level.FINEST, "Setting the error to the active span ...", e);
                    span.recordException(e);
                    throw e;
                }
            } finally {
                span.end();
            }
        } else {
            span = tracer.spanBuilder(operationName).setAttribute("otel.service.name", applicationName).startSpan();
            try (Scope scope = span.makeCurrent()) {
                try {
                    return invocationContext.proceed();
                } catch (Exception e) {
                    LOG.log(Level.FINEST, "Setting the error to the active span ...", e);
                    span.recordException(e);
                    throw e;
                }
            } finally {
                span.end();
            }
        }
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
            if (getAnnotation(beanManager, httpMethod, invocationContext) != null) {
                return true;
            }
        }

        return false;
    }

    
    private boolean isWebServiceMethod(InvocationContext invocationContext, InvocationManager invocationManager) {
        return invocationContext.getMethod().equals(invocationManager.peekWebServiceMethod());
    }

    /**
     * Get the operationName variable from a config override, or from the annotation if there is no override
     *
     * @return the name of the operation. Never null.
     */
    private String getOperationName(final InvocationContext invocationContext, final Traced traced) {
        final String operationName = OpenTracingCdiUtils
                .getConfigOverrideValue(Traced.class, "operationName", invocationContext, String.class)
                .orElse(traced.operationName());

        if (operationName.isEmpty()) {
            // If the operation name is blank, set it to the full method signature
            return invocationContext.getMethod().getDeclaringClass().getCanonicalName() + "."
                    + invocationContext.getMethod().getName();
        }
        return operationName;
    }
}
