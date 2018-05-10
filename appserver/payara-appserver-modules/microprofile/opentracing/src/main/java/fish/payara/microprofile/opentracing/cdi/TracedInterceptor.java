/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
import io.opentracing.ActiveSpan;
import io.opentracing.tag.Tags;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.opentracing.Traced;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Interceptor
@Traced
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class TracedInterceptor {

    private static final Logger logger = Logger.getLogger(TracedInterceptor.class.getName());

    @Inject
    private BeanManager beanManager;

    @Inject
    HttpServletRequest request;
    
    @Inject
    HttpServletResponse response;
    
    @AroundInvoke
    public Object traceCdiCall(InvocationContext invocationContext) throws Exception {
        OpenTracingService openTracing = Globals.getDefaultHabitat().getService(OpenTracingService.class);
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator().getService(InvocationManager.class);

        Config config = null;

        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }

        Object proceed = null;

        if (openTracing != null && openTracing.isEnabled()) {
            Traced traced = OpenTracingCdiUtils.getAnnotation(beanManager, Traced.class, invocationContext);

            String operationName = (String) OpenTracingCdiUtils
                    .getOverrideValue(config, Traced.class, "operationName", invocationContext, String.class)
                    .orElse(traced.operationName());

            String jaxRsMethodAnnotationName = getJaxRsMethodAnnotationName(invocationContext);
            if (operationName.equals("")) {
                if (jaxRsMethodAnnotationName != null) {
                    operationName = jaxRsMethodAnnotationName + ":";
                }
                
                operationName = invocationContext.getMethod().getDeclaringClass().getCanonicalName()
                        + "."
                        + invocationContext.getMethod().getName();
            }

            boolean tracingEnabled = (boolean) OpenTracingCdiUtils
                    .getOverrideValue(config, Traced.class, "value", invocationContext, boolean.class)
                    .orElse(traced.value());

            if (tracingEnabled) {
                try (ActiveSpan activeSpan = openTracing
                        .getTracer(openTracing.getApplicationName(invocationManager, invocationContext))
                        .buildSpan(operationName).startActive()) {

                    proceed = invocationContext.proceed();
                    
                    if (jaxRsMethodAnnotationName != null) {
                        activeSpan.setTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
                        activeSpan.setTag(Tags.HTTP_METHOD.getKey(), request.getMethod());
                        activeSpan.setTag(Tags.HTTP_URL.getKey(), request.getRequestURL().toString());
                        activeSpan.setTag(Tags.HTTP_STATUS.getKey(), response.getStatus());
                        
                        // CHECK ME!!!!!!!!!!!
                        
                        if (response.getStatus() >= 400) {
                            activeSpan.setTag(Tags.ERROR.getKey(), true);
                        }
                    }
                }
            } else {
                proceed = invocationContext.proceed();
            }
        } else {
            proceed = invocationContext.proceed();
        }

        return proceed;
    }

    private String getJaxRsMethodAnnotationName(InvocationContext invocationContext) {
        String jaxRsHttpMethodName = null;

        // There must be a better way of doing this...
        if (OpenTracingCdiUtils.getAnnotation(beanManager, GET.class, invocationContext) != null) {
            jaxRsHttpMethodName = "GET";
        } else if (OpenTracingCdiUtils.getAnnotation(beanManager, POST.class, invocationContext) != null) {
            jaxRsHttpMethodName = "POST";
        } else if (OpenTracingCdiUtils.getAnnotation(beanManager, DELETE.class, invocationContext) != null) {
            jaxRsHttpMethodName = "DELETE";
        } else if (OpenTracingCdiUtils.getAnnotation(beanManager, PUT.class, invocationContext) != null) {
            jaxRsHttpMethodName = "PUT";
        } else if (OpenTracingCdiUtils.getAnnotation(beanManager, HEAD.class, invocationContext) != null) {
            jaxRsHttpMethodName = "HEAD";
        } else if (OpenTracingCdiUtils.getAnnotation(beanManager, PATCH.class, invocationContext) != null) {
            jaxRsHttpMethodName = "PATCH";
        } else if (OpenTracingCdiUtils.getAnnotation(beanManager, OPTIONS.class, invocationContext) != null) {
            jaxRsHttpMethodName = "OPTIONS";
        }
        
        return jaxRsHttpMethodName;
    }
    
}
