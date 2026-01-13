/*
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.tools.dev.resources;

import fish.payara.tools.dev.core.DevConsoleRegistry;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servlet exposing detailed information about scoped CDI beans
 * for the Dev Console.
 *
 * <p>
 * This endpoint provides a per-scope summary of CDI beans, reporting
 * the number of beans declared for each scope and the number of
 * contextual instances currently active for that scope, when the
 * corresponding CDI context is active.
 * </p>
 *
 * <h2>Supported endpoint</h2>
 *
 * <ul>
 *   <li><b>GET /cdi/scoped-beans/detail</b><br>
 *       Returns a JSON object keyed by scope name, where each entry
 *       contains bean counts and active instance counts.</li>
 * </ul>
 *
 * <h2>Response format</h2>
 *
 * <p>
 * The response is serialized to JSON using JSON-B and consists of a
 * JSON object with the following structure:
 * </p>
 *
 * <pre>
 * {
 *   "@RequestScoped": {
 *     "beanCount": 3,
 *     "instances": 1
 *   },
 *   "@SessionScoped": {
 *     "beanCount": 2,
 *     "instances": null
 *   }
 * }
 * </pre>
 *
 * <p>
 * The {@code instances} value is {@code null} when the corresponding
 * CDI context is not active at the time of the request.
 * </p>
 *
 * <h2>Implementation details</h2>
 *
 * <ul>
 *   <li>Beans are grouped by their declared CDI scope.</li>
 *   <li>Beans with {@code @Dependent} scope are intentionally excluded
 *       from the result.</li>
 *   <li>Context availability is determined using the injected
 *       {@link jakarta.enterprise.inject.spi.BeanManager}.</li>
 *   <li>For active contexts, at most one contextual instance per bean
 *       is counted, in accordance with CDI context semantics.</li>
 *   <li>All values represent a snapshot of the container state at the
 *       time of the request.</li>
 * </ul>
 *
 * <h2>Error handling</h2>
 *
 * <ul>
 *   <li>{@code 404 Not Found} is returned if the Dev Console is disabled.</li>
 * </ul>
 *
 * @author Gaurav Gupta
 */
public class ScopedBeansDetailServlet extends AbstractConsoleServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (!guard(resp)) {
            return;
        }

        String app = resolveAppName(req);
        DevConsoleRegistry registry = getRegistry(app);
        // Group beans by declared scope (treat null as Dependent)
        Map<Class<? extends Annotation>, List<Bean<?>>> beansByScope =
                registry.getBeans().stream()
                        .collect(Collectors.groupingBy(b -> {
                            Class<? extends Annotation> sc = b.getScope();
                            return sc != null
                                    ? sc
                                    : jakarta.enterprise.context.Dependent.class;
                        }));

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<Class<? extends Annotation>, List<Bean<?>>> entry
                : beansByScope.entrySet()) {

            Class<? extends Annotation> scopeClass = entry.getKey();

            // Skip Dependent scope (same behavior as JAX-RS)
            if (scopeClass == jakarta.enterprise.context.Dependent.class) {
                continue;
            }

            String scopeKey = "@" + scopeClass.getSimpleName();
            List<Bean<?>> beanList = entry.getValue();

            int beanCount = beanList.size();
            Integer scopeInstancesSum = 0; // null if context not active

            Context context;
            boolean contextActive = true;

            try {
                context = registry.getBeanManager().getContext(scopeClass);
            } catch (Exception ex) {
                contextActive = false;
                context = null;
            }

            if (!contextActive) {
                scopeInstancesSum = null;
            } else {
                for (Bean<?> bean : beanList) {
                    Integer instancesForBean;

                    try {
                        @SuppressWarnings("unchecked")
                        Contextual<Object> contextual = (Contextual<Object>) bean;
                        Object instance = context.get(contextual);
                        instancesForBean = (instance != null) ? 1 : 0;
                    } catch (Throwable t) {
                        instancesForBean = null;
                    }

                    if (instancesForBean != null && instancesForBean > 0) {
                        scopeInstancesSum += instancesForBean;
                    }
                }
            }

            Map<String, Object> scopeSummary = new LinkedHashMap<>();
            scopeSummary.put("beanCount", beanCount);
            scopeSummary.put("instances", scopeInstancesSum);

            result.put(scopeKey, scopeSummary);
        }

        writeJson(resp, result);
    }
}
