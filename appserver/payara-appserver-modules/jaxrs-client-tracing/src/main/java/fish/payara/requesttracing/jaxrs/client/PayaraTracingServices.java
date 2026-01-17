/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
 */

package fish.payara.requesttracing.jaxrs.client;

import fish.payara.opentracing.OpenTelemetryService;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.Deployment;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;
import io.opentracing.Tracer;

/**
 * This is a class hiding internal mechanism of lookup of HK2 services. The
 * required services will be eagerly initialised using their service handles,
 * and throwing an exception if the handle is available but not the service
 * itself.
 *
 * @author David Matejcek
 */
public final class PayaraTracingServices {

    private final RequestTracingService requestTracingService;
    private final OpenTracingService openTracingService;

    private final InvocationManager invocationManager;
    private final Deployment deployment;
    
    private final OpenTelemetryService openTelemetryService;

    /**
     * Initialise the tracing services if they are available.
     * 
     * @throws RuntimeException if an exception occurs initialising the services.
     */
    public PayaraTracingServices() {
        final ServiceLocator baseServiceLocator = Globals.getStaticBaseServiceLocator();

        requestTracingService = getFromServiceHandle(baseServiceLocator, RequestTracingService.class);
        openTracingService = getFromServiceHandle(baseServiceLocator, OpenTracingService.class);
        invocationManager = getFromServiceHandle(baseServiceLocator, InvocationManager.class);
        deployment = getFromServiceHandle(baseServiceLocator, Deployment.class);
        openTelemetryService = getFromServiceHandle(baseServiceLocator, OpenTelemetryService.class);
    }

    /**
     * @return true if the Request Tracing services are available and have been
     *         initialised, or false if the services are not available.
     */
    public boolean isTracingAvailable() {
        return requestTracingService != null
                && openTracingService != null
                && openTelemetryService != null;
    }

    /**
     * @return {@link RequestTracingService}, or null if the HK2 service couldn't be
     *         initialised.
     */
    public RequestTracingService getRequestTracingService() {
        if (isTracingAvailable()) {
            return requestTracingService;
        }
        return null;
    }

    /**
     * @return {@link OpenTracingService}, or null if the HK2 service couldn't be
     *         initialised.
     */
    public OpenTracingService getOpenTracingService() {
        if (isTracingAvailable()) {
            return openTracingService;
        }
        return null;
    }

    /**
     * @return {@link OpenTracingService}, or null if the HK2 service couldn't be
     *         initialised.
     */
    public OpenTelemetryService getOpenTelemetryService() {
        if (isTracingAvailable()) {
            return openTelemetryService;
        }
        return null;
    }

    /**
     * @return {@link InvocationManager}
     */
    public InvocationManager getInvocationManager() {
        return invocationManager;
    }

    /**
     * @return {@link Deployment}
     */
    public Deployment getDeployment() {
        return deployment;
    }

    /**
     * @return application name known to the actual {@link InvocationManager}, or
     *         null if no invocation manager can be found.
     */
    public String getApplicationName() {
        if (isTracingAvailable()) {
            return openTracingService.getApplicationName(invocationManager);
        }
        return null;
    }

    /**
     * @return actually active {@link Tracer} for the current application, or null
     *         if the tracing service is not available.
     */
    public Tracer getActiveTracer() {
        final String applicationName = getApplicationName();
        if (applicationName == null || !isTracingAvailable()) {
            return null;
        }
        return openTracingService.getTracer(applicationName);
    }

    /**
     * Create a service from the given service locator. Throw an exception if the
     * service handle is available but not the service.
     * 
     * @return the specified service, or null if the service handle isn't available.
     * @throws RuntimeException if the service initialisation failed.
     */
    private static final <T> T getFromServiceHandle(ServiceLocator serviceLocator, Class<T> serviceClass) {
        ServiceHandle<T> serviceHandle = serviceLocator.getServiceHandle(serviceClass);
        if (serviceHandle != null && serviceHandle.isActive()) {
            return serviceHandle.getService();
        }
        return null;
    }
}
