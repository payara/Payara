/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.opentracing;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.interceptor.InvocationContext;

import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

/**
 * Service class for the OpenTracing integration.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Service(name = "opentracing-service")
public class OpenTracingService implements EventListener {

    // The tracer instances
    private static final Map<String, Tracer> tracers = new ConcurrentHashMap<>();
    
    // The name of the Corba RMI Tracer
    public static final String PAYARA_CORBA_RMI_TRACER_NAME = "__PAYARA_CORBA_RMI";

    private static final Logger logger = Logger.getLogger(OpenTracingService.class.getName());

    @PostConstruct
    void postConstruct() {
        // Listen for events
        Events events = getFromServiceHandle(Globals.getDefaultBaseServiceLocator(), Events.class);

        if (events != null) {
            events.register(this);
        } else {
            logger.log(Level.WARNING, "OpenTracing service not registered to Payara Events: "
                    + "The Tracer for an application won't be removed upon undeployment");
        }
    }

    @Override
    public void event(Event<?> event) {
        // Listen for application unloaded events (happens during undeployment), so that we remove the tracer instance
        // registered to that application (if there is one)
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            tracers.remove(info.getName());
        }
    }

    /**
     * Gets the tracer instance for the given application, or creates one if there isn't one.
     *
     * @param applicationName The name of the application to get or create the Tracer for
     * @return The Tracer instance for the given application
     */
    public synchronized Tracer getTracer(String applicationName) {
        if (applicationName == null) {
            return null;
        }

        // Get the tracer if there is one
        Tracer tracer = tracers.get(applicationName);

        // If there isn't a tracer for the application, create one
        if (tracer == null) {
            // Check which type of Tracer to create

            try {//See if an alternate implementation of Tracer is available in a library.
                ServiceLoader<Tracer> tracerLoader = ServiceLoader.load(Tracer.class);
                Iterator<Tracer> loadedTracer = tracerLoader.iterator();
                if (loadedTracer.hasNext()) {
                    tracer = loadedTracer.next();
                }
            } catch (NoClassDefFoundError ex) {
                logger.log(Level.SEVERE, "Unable to find Tracer implementation", ex);
            }

            if (Boolean.getBoolean("USE_OPENTRACING_MOCK_TRACER")) {
                tracer = new MockTracer(new ThreadLocalScopeManager(), MockTracer.Propagator.TEXT_MAP);
            } else if (tracer == null) {
                // Check if we have an ORB Tracer so that we can use its scope manager
                Tracer orbTracer = tracers.get(PAYARA_CORBA_RMI_TRACER_NAME);
                if (orbTracer != null) {
                    tracer = new fish.payara.opentracing.tracer.Tracer(applicationName, orbTracer.scopeManager());
                } else {
                    tracer = new fish.payara.opentracing.tracer.Tracer(applicationName);
                }
            }

            // Register the tracer instance to the application
            tracers.put(applicationName, tracer);
        }

        return tracer;
    }

    /**
     * Pass-through method that checks if Request Tracing is enabled.
     *
     * @return True if the Request Tracing Service is enabled
     */
    public boolean isEnabled() {
        RequestTracingService requestTracingService = getFromServiceHandle(Globals.getDefaultBaseServiceLocator(),
                RequestTracingService.class);

        if (requestTracingService != null) {
            return requestTracingService.isRequestTracingEnabled();
        }
        return false;
    }

    /**
     * Gets the application name from the invocation manager. Failing that, it will use the module name or component
     * name.
     *
     * @param invocationManager The invocation manager to get the application name from
     * @return The application name
     */
    public String getApplicationName(InvocationManager invocationManager) {
        final ComponentInvocation invocation = invocationManager.getCurrentInvocation();
        if (invocation == null) {
            // if the invocation context is not an application but some server component.
            return null;
        }
        String appName = invocation.getAppName();
        if (appName == null) {
            appName = invocation.getModuleName();

            if (appName == null) {
                appName = invocation.getComponentId();

                // If we've found a component name, check if there's an application registered with the same name
                if (appName != null) {
                    ApplicationRegistry applicationRegistry = Globals.getDefaultBaseServiceLocator()
                            .getService(ApplicationRegistry.class);

                    // If it's not directly in the registry, it's possible due to how the componentId is constructed
                    if (applicationRegistry.get(appName) == null) {
                        String[] componentIds = appName.split("_/");

                        // The application name should be the first component
                        appName = componentIds[0];
                    }
                }
            }
        }

        return appName;
    }

    /**
     * Gets the application name from the invocation manager. Failing that, it will use the module name, component name,
     * or method signature (in that order).
     *
     * @param invocationManager The invocation manager to get the application name from
     * @param invocationContext The context of the current invocation
     * @return The application name
     */
    public String getApplicationName(InvocationManager invocationManager, InvocationContext invocationContext) {
        // Check the obvious one first
        String appName = invocationManager.getCurrentInvocation().getAppName();

        if (appName == null) {
            // Set it to the module name if possible
            appName = invocationManager.getCurrentInvocation().getModuleName();

            if (appName == null) {
                // Set to the component name if possible
                appName = invocationManager.getCurrentInvocation().getComponentId();

                if (appName == null && invocationContext != null) {
                    // Set it to the full method signature of the method
                    appName = getFullMethodSignature(invocationContext.getMethod());
                }
            }
        }

        return appName;
    }

    /**
     * Helper method to generate a full method signature consisting of canonical class name, method name, parameter
     * types, and return type.
     *
     * @param annotatedMethod The annotated Method to generate the signature for
     * @return A String in the format of CanonicalClassName#MethodName({ParameterTypes})>ReturnType
     */
    private String getFullMethodSignature(Method annotatedMethod) {
        return annotatedMethod.getDeclaringClass().getCanonicalName()
                + "#" + annotatedMethod.getName()
                + "(" + Arrays.toString(annotatedMethod.getParameterTypes()) + ")"
                + ">" + annotatedMethod.getReturnType().getSimpleName();
    }

    private <T> T getFromServiceHandle(ServiceLocator serviceLocator, Class<T> serviceClass) {
        if (serviceLocator != null) {
            ServiceHandle<T> serviceHandle = serviceLocator.getServiceHandle(serviceClass);
            if (serviceHandle != null && serviceHandle.isActive()) {
                return serviceHandle.getService();
            }
        }

        return null;
    }
}
