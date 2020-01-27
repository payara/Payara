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
 */

package fish.payara.requesttracing.jaxrs.client;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;

import io.opentracing.Tracer;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

/**
 * This is a class hiding internal mechanism of lookup of HK2 services.
 * The lookup is lazy, done with first request, but this may be simply changed later.
 * <p>
 * The lazy lookup prevents problems with embedded distributions, when jersey lookups
 * may detect filters in this package and try to use them before Payara started, which
 * is not supported use case.
 *
 * @author David Matejcek
 */
public final class PayaraTracingServices {

    private static volatile boolean initialized;

    private static ServiceLocator basicServiceLocator;
    private static RequestTracingService requestTracingService;
    private static OpenTracingService openTracingService;


    private static void checkInitialized() {
        if (initialized) {
            return;
        }
        synchronized (PayaraTracingServices.class) {
            if (initialized) {
                return;
            }
            basicServiceLocator = Globals.getStaticBaseServiceLocator();
            requestTracingService = basicServiceLocator.getService(RequestTracingService.class);
            openTracingService = basicServiceLocator.getService(OpenTracingService.class);
            initialized = true;
        }
    }


    /**
     * @return default service locator, same as {@link Globals#getStaticBaseServiceLocator()}.
     */
    public ServiceLocator getBasicServiceLocator() {
        checkInitialized();
        return basicServiceLocator;
    }


    /**
     * @return {@link RequestTracingService}
     */
    public RequestTracingService getRequestTracingService() {
        checkInitialized();
        return requestTracingService;
    }


    /**
     * @return {@link OpenTracingService}
     */
    public OpenTracingService getOpenTracingService() {
        checkInitialized();
        return openTracingService;
    }


    /**
     * @return {@link InvocationManager}
     */
    public InvocationManager getInvocationManager() {
        checkInitialized();
        return basicServiceLocator.getService(InvocationManager.class);
    }


    /**
     * @return application name known to the actual {@link InvocationManager}.
     */
    public String getApplicationName() {
        final InvocationManager invocationManager = getInvocationManager();
        if (invocationManager == null) {
            return null;
        }
        final OpenTracingService otService = getOpenTracingService();
        return otService == null ? null : otService.getApplicationName(invocationManager);
    }


    /**
     * @return actually active {@link Tracer} for the current application.
     */
    public Tracer getActiveTracer() {
        final String applicationName = getApplicationName();
        if (applicationName == null) {
            return null;
        }
        final OpenTracingService otService = getOpenTracingService();
        if (otService == null) {
            return null;
        }
        return otService.getTracer(applicationName);
    }
}
