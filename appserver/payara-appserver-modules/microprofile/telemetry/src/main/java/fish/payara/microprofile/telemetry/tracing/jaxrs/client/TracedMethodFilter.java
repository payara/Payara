/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.microprofile.telemetry.tracing.jaxrs.client;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.client.ClientRequestContext;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.opentracing.Traced;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Checks whether REST Client invoked method should be traced according to configuration and class annotations.
 *
 * A method {@code M} of Microprofile REST Client interface {@code C} is traced if:
 * <ol>
 *     <li> boolean config value{@code [className]/[methodName]/Traced/value} is not present, or is true; or</li>
 *     <li> method's {@code @Traced} annotation is not present, or has {@code value()} of true; or</li>
 *     <li> boolean config value {@code [className]/Traced/value} is not present, or is true; or</li>
 *     <li> class' {@code @Traced} annotation is not present, or has {@code value()} of true</li>
 * </ol>
 */
class TracedMethodFilter implements Predicate<ClientRequestContext> {
    private final boolean classDefault;
    private final Map<Method, Boolean> methodOverrides;

    @Override
    public boolean test(ClientRequestContext clientRequestContext) {
        Object invokedMethod = clientRequestContext.getProperty(RestClientTelemetryListener.REST_CLIENT_INVOKED_METHOD);
        if (invokedMethod instanceof Method) {
            return test((Method) invokedMethod);
        } else {
            return true;
        }
    }

    private boolean test(Method invokedMethod) {
        return methodOverrides.getOrDefault(invokedMethod, classDefault);
    }

    TracedMethodFilter(Config config, Class<?> clientClass) {
        this.classDefault = determineClassDefault(config, clientClass);
        this.methodOverrides = new HashMap<>();

        for (Method method : clientClass.getMethods()) {
            if (!isRestMethod(method)) {
                // sub resources are not yet defined by the spec
                continue;
            }
            determineMethodValue(config, clientClass, method)
                    .ifPresent(value -> methodOverrides.put(method, value));
        }
    }

    private static boolean isRestMethod(Method m) {
        return getHttpMethodName(m) != null;
    }

    private static boolean determineClassDefault(Config config, Class<?> clientClass) {
        // Priorities:
        // 1. Config value of <className>/Traced/value
        // 2. Class' @Traced annotation value
        // 3. true

        return Optional.ofNullable(config)
                .flatMap(cfg -> cfg.getOptionalValue(classOverrideProperty(clientClass), boolean.class))
                .orElseGet(() -> tracedAnnotationValue(clientClass::getAnnotation)
                        .orElse(true));
    }

    private static Optional<Boolean> determineMethodValue(Config config, Class<?> clientClass, Method method) {
        // 1. config value of <className>/<methodName>/Traced/value
        // 2. Method's @Traced annotation value
        // (3. class default)
        Optional<Boolean> configValue = Optional.ofNullable(config)
                .flatMap(cfg -> cfg.getOptionalValue(methodOverrideProperty(clientClass, method), boolean.class));
        return configValue.isPresent() ? configValue : tracedAnnotationValue(method::getAnnotation);
    }

    private static String getHttpMethodName(Method method) {
        // Initialise an Array with all supported JaxRs HTTP methods
        Class[] httpMethods = {GET.class, POST.class, DELETE.class, PUT.class, HEAD.class, PATCH.class, OPTIONS.class};

        // Check if any of the HTTP Method annotations are present on the intercepted method
        for (Class httpMethod : httpMethods) {
            if (method.getAnnotation(httpMethod) != null) {
                return httpMethod.getSimpleName();
            }
        }

        return null;
    }
    private static String classOverrideProperty(Class<?> clientClass) {
        return clientClass.getCanonicalName() + "/" + Traced.class.getSimpleName() + "/value";
    }


    private static String methodOverrideProperty(Class<?> clientClass, Method method) {
        return clientClass.getCanonicalName() + "/" + method.getName() + "/" + Traced.class.getSimpleName() + "/value";
    }

    private static Optional<Boolean> tracedAnnotationValue(Function<Class<Traced>, Traced> annotationSource) {
        return Optional.ofNullable(annotationSource.apply(Traced.class)).map(Traced::value);
    }

}
