/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.opentracing.jaxrs;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

/**
 * {@link ApplicationEventListener} for configuring the {@link OpenTracingRequestEventListener}
 * instances for tracked requests.
 *
 * @author David Matejcek
 */
@Priority(Priorities.HEADER_DECORATOR - 500)
public class OpenTracingApplicationEventListener implements ApplicationEventListener {

    private static final Logger LOG = Logger.getLogger(OpenTracingApplicationEventListener.class.getName());

    private RequestTracingService requestTracing;
    private OpenTracingService openTracing;
    private String applicationName;

    @Context
    private ResourceInfo resourceInfo;


    /**
     * Initialization of internal services.
     */
    @PostConstruct
    public void postConstruct() {
        LOG.finest("postConstruct()");
        final ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
        if (serviceLocator == null) {
            LOG.config("Default base service locator is null, JAX-RS server tracing is disabled.");
            return;
        }
        final InvocationManager invocationManager = serviceLocator.getService(InvocationManager.class);
        this.requestTracing = serviceLocator.getService(RequestTracingService.class);
        this.openTracing = serviceLocator.getService(OpenTracingService.class);
        if (invocationManager == null || this.openTracing == null) {
            this.applicationName = null;
        } else {
            this.applicationName = this.openTracing.getApplicationName(invocationManager);
        }
    }


    @Override
    public void onEvent(final ApplicationEvent event) {
        LOG.config(() -> "onEvent(event.type=" + event.getType() + ")");
    }


    @Override
    public RequestEventListener onRequest(final RequestEvent event) {
        LOG.finer(() -> "onRequest(event.type=" + event.getType() + ")");
        if (!isRequestTracingInProgress()) {
            LOG.finest("isRequestTracingInProgress() returned false, nothing to do.");
            return null;
        }
        return new OpenTracingRequestEventListener(this.applicationName, this.resourceInfo, this.openTracing);
    }


    private boolean isRequestTracingInProgress() {
        return requestTracing != null && requestTracing.isRequestTracingEnabled() && requestTracing.isTraceInProgress();
    }
}
