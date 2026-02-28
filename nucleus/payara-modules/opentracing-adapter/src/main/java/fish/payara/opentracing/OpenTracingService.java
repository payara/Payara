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
package fish.payara.opentracing;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.interceptor.InvocationContext;
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for the OpenTracing integration.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Service(name = "opentracing-service")
public class OpenTracingService implements EventListener {

    // The name of the Corba RMI Tracer
    public static final String PAYARA_CORBA_RMI_TRACER_NAME = "__PAYARA_CORBA_RMI";

    // The tracer instances
    private static final Map<String, Tracer> tracers = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(OpenTracingService.class.getName());

    @Inject
    ServiceLocator locator;

    @Inject
    OpenTelemetryService otel;

    @Inject
    Events events;

    @PostConstruct
    void postConstruct() {
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
            Tracer tracer = tracers.remove(info.getName());
            if (tracer != null) {
                tracer.close();
            }
        }
    }

    /**
     * Gets the tracer instance for the given application, or creates one if there isn't one.
     *
     * @param applicationName The name of the application to get or create the Tracer for
     * @return The Tracer instance for the given application
     */
    public Tracer getTracer(String applicationName) {
        if (applicationName == null) {
            return null;
        }

        // Get the tracer if there is one
        Tracer tracer = tracers.get(applicationName);

        // If there isn't a tracer for the application, create one
        if (tracer == null) {
            tracer = createTracer(applicationName);
        }

        return tracer;
    }

    private Tracer createTracer(String applicationName) {
        // Double-checked locking - potentially naughty
        Tracer tracer = tracers.computeIfAbsent(applicationName, (appName) -> {
            // required for direct interaction with OpenTracing, i. e. in MP TCK
            if (otel == null) {
                return null;
            }
            // create default implementation (env / system property based) for the application
            otel.ensureAppInitialized(appName, null);
            return otel.getSdkDependency(applicationName, () -> tracers.remove(applicationName)).map(OpenTracingShim::createTracerShim).orElse(NoopTracerFactory.create());
        });

        return tracer;
    }

    /**
     * Pass-through method that checks if Request Tracing is enabled.
     *
     * @return True if the Request Tracing Service is enabled
     */
    public boolean isEnabled() {
        RequestTracingService requestTracingService = getFromServiceHandle(locator, RequestTracingService.class);
        return requestTracingService != null && requestTracingService.isRequestTracingEnabled();
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
}
