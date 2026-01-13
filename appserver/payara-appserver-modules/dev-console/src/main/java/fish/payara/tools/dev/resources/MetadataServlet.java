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
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet exposing aggregated Dev Console metadata.
 *
 * <p>
 * This endpoint provides a high-level overview of the CDI and REST
 * infrastructure detected by the Dev Console. It aggregates counts
 * of beans, scopes, interceptors, decorators, producers, observers,
 * REST resources, and other runtime components.
 * </p>
 *
 * <h2>Supported endpoint</h2>
 *
 * <ul>
 *   <li><b>GET /metadata</b><br>
 *       Returns a JSON object containing aggregated metadata counts.</li>
 * </ul>
 *
 * <h2>Response format</h2>
 *
 * <p>
 * The response is serialized to JSON using JSON-B and consists of a
 * single JSON object whose keys represent metadata categories and
 * whose values represent counts derived from the current application
 * state.
 * </p>
 *
 * <h2>Included metadata</h2>
 *
 * <ul>
 *   <li>Total number of CDI beans</li>
 *   <li>Number of scoped CDI beans</li>
 *   <li>Interceptor and decorated class counts</li>
 *   <li>Decorator counts</li>
 *   <li>Producer counts</li>
 *   <li>REST resource, method, and exception mapper counts</li>
 *   <li>Observer and recent event counts</li>
 *   <li>Security annotation counts</li>
 *   <li>CDI extension counts</li>
 * </ul>
 *
 * <h2>Implementation details</h2>
 *
 * <ul>
 *   <li>Metadata is derived from the {@code DevConsoleRegistry}.</li>
 *   <li>Interceptor and decorator summaries reuse shared logic from
 *       {@link AbstractSummarySupport}.</li>
 *   <li>All values represent a snapshot of the registry state at the
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
public class MetadataServlet extends AbstractSummarySupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (!guard(resp)) {
            return;
        }

        String app = resolveAppName(req);
        DevConsoleRegistry registry = getRegistry(app);
        Map<String, Object> meta = new LinkedHashMap<>();

        meta.put("beanCount", registry.getBeans().size());
        meta.put("scopedBeanCount",
                registry.getBeans().stream()
                        .map(Bean::getScope)
                        .filter(s -> s != null)
                        .count());

        meta.put("interceptorCount", registry.getInterceptors().size());
        meta.put("interceptedClassesCount",
                interceptorSummary(registry.getInterceptorChains()).size());

        meta.put("decoratorCount", registry.getDecorators().size());
        meta.put("decoratedClassesCount",
                decoratorSummary(registry.getDecoratorChains()).size());

        meta.put("producerCount", registry.getProducers().size());
        meta.put("restResourceCount", registry.getRestResourcePaths().size());
        meta.put("restMethodCount", registry.getRestMethodInfoMap().size());
        meta.put("restExceptionMapperCount", registry.getRestExceptionMappers().size());
        meta.put("observerCount", registry.getObservers().size());
        meta.put("recentEventCount", registry.getRecentEvents().size());
        meta.put("securityAnnotationCount", registry.getSecurityAnnotations().size());
        meta.put("injectionPointsCount", registry.getAllInjectionPoints().size());

        meta.put("extensionCount",
                registry.getBeans().stream()
                        .map(Bean::getBeanClass)
                        .filter(Extension.class::isAssignableFrom)
                        .count());

        writeJson(resp, meta);
    }

}
