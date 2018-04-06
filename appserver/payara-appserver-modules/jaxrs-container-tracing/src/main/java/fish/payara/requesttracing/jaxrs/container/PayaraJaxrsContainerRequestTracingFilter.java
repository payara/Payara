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
package fish.payara.requesttracing.jaxrs.container;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;
import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Provider
public class PayaraJaxrsContainerRequestTracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private ServiceLocator serviceLocator;
    private RequestTracingService requestTracing;
    private OpenTracingService openTracing;
    
    private ThreadLocal<Continuation> continuation;
    
    @PostConstruct
    public void postConstruct() {
        // Get the default Payara service locator - injecting a service locator will give you the one used by Jersey
        serviceLocator = Globals.getDefaultBaseServiceLocator();
        
        if (serviceLocator != null) {
            requestTracing = serviceLocator.getService(RequestTracingService.class);
            openTracing = serviceLocator.getService(OpenTracingService.class);
        }
        
        continuation = new ThreadLocal<>();
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (requestTracing!= null && requestTracing.isRequestTracingEnabled() && requestTracing.isTraceInProgress()) {
            Tracer tracer = openTracing.getTracer();
            ActiveSpan activeSpan = tracer.buildSpan(requestContext.getMethod())
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                    .withTag(Tags.HTTP_METHOD.getKey(), requestContext.getMethod())
                    .withTag(Tags.HTTP_URL.getKey(), requestContext.getUriInfo().getRequestUri().toURL().toString())
                    .startActive();
            
            continuation.set(activeSpan.capture());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (requestTracing!= null && requestTracing.isRequestTracingEnabled() && requestTracing.isTraceInProgress()) {
            
            try (ActiveSpan activeSpan = continuation.get().activate()) {
                Response.StatusType statusInfo = responseContext.getStatusInfo();
                
                activeSpan.setTag(Tags.HTTP_STATUS.getKey(), statusInfo.getStatusCode());
            
                if (statusInfo.getFamily() == Response.Status.Family.CLIENT_ERROR || statusInfo.getFamily() == Response.Status.Family.SERVER_ERROR) {
                    activeSpan.setTag(Tags.ERROR.getKey(), true);
                }
            } finally {
                continuation.set(null);
            }
        }
    }
    
}
