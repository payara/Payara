/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import org.eclipse.microprofile.opentracing.Traced;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Interceptor for MicroProfile Traced annotation.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Interceptor
@Traced
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class TracedInterceptor implements Serializable {

    private static final Logger logger = Logger.getLogger(TracedInterceptor.class.getName());

    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object traceCdiCall(InvocationContext invocationContext) throws Exception {
        // Get the required HK2 services
        ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
        OpenTracingService openTracing = serviceLocator.getService(OpenTracingService.class);
        InvocationManager invocationManager = serviceLocator.getService(InvocationManager.class);
        
        // Initialise return value
        Object proceed = null;

        // If Request Tracing is enabled, and this isn't a JaxRs method
        if (openTracing != null && openTracing.isEnabled() && !isJaxRsMethod(invocationContext)) {
            // Get the Traced annotation present on the method or class
            Traced traced = OpenTracingCdiUtils.getAnnotation(beanManager, Traced.class, invocationContext);

            // Get the operationName variable from a config override, or from the annotation if there is no override
            String operationName = (String) OpenTracingCdiUtils
                    .getConfigOverrideValue(Traced.class, "operationName", invocationContext, String.class)
                    .orElse(traced.operationName());
            
            // If the operation name is blank, set it to the full method signature
            if (operationName.equals("")) {
                operationName = invocationContext.getMethod().getDeclaringClass().getCanonicalName()
                        + "."
                        + invocationContext.getMethod().getName();
            }

            // Get the enabled (value) variable from a config override, or from the annotation if there is no override
            boolean tracingEnabled = (boolean) OpenTracingCdiUtils
                    .getConfigOverrideValue(Traced.class, "value", invocationContext, boolean.class)
                    .orElse(traced.value());

            // Only trace if we've explicitly been told to (which is the default behaviour)
            if (tracingEnabled) {
                // If we *have* been told to, get the application's Tracer instance and start an active span.
                Tracer tracer = openTracing.getTracer(openTracing.getApplicationName(invocationManager, invocationContext));
                Span activeSpan = tracer.buildSpan(operationName).start();
                try (Scope scope = tracer.scopeManager().activate(activeSpan, true)) {

                    // Proceed the invocation
                    try {
                        proceed = invocationContext.proceed();
                    } catch (Exception ex) {
                        // If an exception occurs during processing the method, add error info to the span
                        activeSpan.setTag(Tags.ERROR.getKey(), true);
                        Map<String, Object> errorInfoMap = new HashMap<>();
                        errorInfoMap.put(Fields.EVENT, "error");
                        errorInfoMap.put(Fields.ERROR_OBJECT, ex);
                        activeSpan.log(errorInfoMap);
                        // Don't deal with the error here, let it propagate upwards
                        throw ex;
                    }
                }
            } else {
                // If we've explicitly been told not to trace the method: don't!
                proceed = invocationContext.proceed();
            }
        } else {
            // If request tracing was turned off, or this is a JaxRs method, just carry on
            proceed = invocationContext.proceed();
        }

        return proceed;
    }

    /**
     * Helper method that determines if the annotated method is a JaxRs method or not by inspected it for HTTP Method 
     * annotations. JaxRs method tracing is handled by the Client/Container filters, so we don't process them using this
     * interceptor.
     * 
     * @param invocationContext The invocation context from the AroundInvoke method.
     * @return True if the method is a JaxRs method.
     */
    private boolean isJaxRsMethod(InvocationContext invocationContext) {
        // Initialise an Array with all supported JaxRs HTTP methods
        Class[] httpMethods = {GET.class, POST.class, DELETE.class, PUT.class, HEAD.class, OPTIONS.class};

        // Check if any of the HTTP Method annotations are present on the intercepted method
        for (Class httpMethod : httpMethods) {
            if (OpenTracingCdiUtils.getAnnotation(beanManager, httpMethod, invocationContext) != null) {
                return true;
            }
        }

        return false;
    }
    
}
