/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.opentracing.cdi;

import fish.payara.opentracing.OpenTracingService;
import fish.payara.requesttracing.jaxrs.client.PayaraTracingServices;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import org.eclipse.microprofile.opentracing.Traced;
import org.glassfish.api.invocation.InvocationManager;

import static fish.payara.microprofile.opentracing.cdi.OpenTracingCdiUtils.getAnnotation;

/**
 * Interceptor for MicroProfile Traced annotation.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 * @author David Matejcek
 */
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


    /**
     * If the tracing is enabled and possible for the invoked method, traces it's execution.
     * If not, only executes the invocation context.
     *
     * @param invocationContext the context to be executed
     * @return result of the invocation context execution.
     * @throws Exception any execution thrown from the invocation context execution.
     */
    @AroundInvoke
    public Object traceCdiCall(final InvocationContext invocationContext) throws Exception {
        LOG.fine(() -> "traceCdiCall(" + invocationContext + ")");
        // Get the required HK2 services
        final PayaraTracingServices payaraTracingServices = new PayaraTracingServices();
        final OpenTracingService openTracing = payaraTracingServices.getOpenTracingService();
        final InvocationManager invocationManager = payaraTracingServices.getInvocationManager();

        // If Request Tracing is enabled, and this isn't a JaxRs method
        if (openTracing == null || !openTracing.isEnabled() //
            || isJaxRsMethod(invocationContext) || isWebServiceMethod(invocationContext, invocationManager)) {
            // If request tracing was turned off, or this is a JaxRs method, just carry on
            LOG.finest("The call is already monitored by some different component, proceeding the invocation.");
            return invocationContext.proceed();
        }

        // Get the Traced annotation present on the method or class
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

        // If we *have* been told to, get the application's Tracer instance and start an active span.
        final String applicationName = openTracing.getApplicationName(invocationManager, invocationContext);
        final Tracer tracer = openTracing.getTracer(applicationName);
        final String operationName = getOperationName(invocationContext, traced);
        Span parentSpan = tracer.activeSpan();

        final Span span = tracer.buildSpan(operationName).asChildOf(parentSpan).start();
        try (Scope scope = tracer.scopeManager().activate(span)) {
            try {
                return invocationContext.proceed();
            } catch (final Exception ex) {
                LOG.log(Level.FINEST, "Setting the error to the active span ...", ex);
                span.setTag(Tags.ERROR.getKey(), true);
                final Map<String, Object> errorInfoMap = new HashMap<>();
                errorInfoMap.put(Fields.EVENT, "error");
                errorInfoMap.put(Fields.ERROR_OBJECT, ex.getClass().getName());
                span.log(errorInfoMap);
                throw ex;
            }
        } finally {
            span.finish();
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
