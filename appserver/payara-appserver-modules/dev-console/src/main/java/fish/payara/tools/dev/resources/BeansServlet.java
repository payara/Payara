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
import fish.payara.tools.dev.dto.BeanDTO;
import fish.payara.tools.dev.dto.InjectionPointDTO;
import fish.payara.tools.dev.model.InstanceStats;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Servlet providing bean-related endpoints for the Dev Console.
 * <p>
 * This servlet exposes bean-centric operations using JSON-B for type-safe
 * serialization.
 *
 * <h2>Supported endpoints</h2>
 *
 * <ul>
 * <li><b>GET /cdi/beans</b><br>
 * Returns a list of all discovered CDI beans as {@link BeanDTO}.</li>
 *
 * <li><b>GET /cdi/beans/{id}</b><br>
 * Returns full metadata and lifecycle statistics for a single bean, identified
 * by its fully-qualified class name, as {@link BeanDTO}.</li>
 *
 * <li><b>GET /cdi/beans/{id}/injection-points</b><br>
 * Returns all injection points belonging to the given bean as a list of
 * {@link InjectionPointDTO}.</li>
 * </ul>
 *
 * <h2>Design notes</h2>
 *
 * <ul>
 * <li>This servlet owns all <em>bean-centric</em> endpoints. Injection-point
 * queries scoped to a specific bean are modeled as sub-resources of the
 * bean.</li>
 * <li>Global injection-point queries (e.g. unresolved or ambiguous injection
 * points) are handled by {@code InjectionPointsServlet}.</li>
 * <li>JSON serialization is performed via JSON-B to ensure type safety and
 * consistency with the former JAX-RS implementation.</li>
 * <li>Access is guarded by
 * {@link AbstractConsoleServlet#guard(HttpServletResponse)}; if the Dev Console
 * is disabled, all endpoints return {@code 404 Not Found}.</li>
 * </ul>
 *
 * <h2>Error handling</h2>
 *
 * <ul>
 * <li>{@code 404 Not Found} if the Dev Console is disabled.</li>
 * <li>{@code 404 Not Found} if a requested bean does not exist.</li>
 * </ul>
 *
 * @author Gaurav Gupta
 */
public class BeansServlet extends AbstractConsoleServlet {

    private static final String INJECTION_POINTS_SUFFIX = "/injection-points";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (!guard(resp)) {
            return;
        }

        String path = req.getPathInfo();
        String app = resolveAppName(req);
        DevConsoleRegistry registry = getRegistry(app);

        // GET /cdi/beans
        if (path == null || "/".equals(path)) {
            listBeans(registry, resp);
            return;
        }

        // GET /cdi/beans/{id}/injection-points
        if (path.endsWith(INJECTION_POINTS_SUFFIX)) {
            String beanClass = path.substring(
                    1,
                    path.length() - INJECTION_POINTS_SUFFIX.length()
            );
            getInjectionPointsForBean(registry, beanClass, resp);
            return;
        }

        // GET /cdi/beans/{id}
        getBeanById(registry, path.substring(1), resp);
    }

    /* ---------------------------------------------------------- */
 /* GET /cdi/beans                                   */
 /* ---------------------------------------------------------- */
    private void listBeans(DevConsoleRegistry registry, HttpServletResponse resp) throws IOException {

        List<BeanDTO> beans = registry.getBeans().stream()
                .map(bean -> new BeanDTO(bean))
                .toList();

        writeJson(resp, beans);
    }

    /* ---------------------------------------------------------- */
 /* GET /cdi/beans/{id}                              */
 /* ---------------------------------------------------------- */
    private void getBeanById(DevConsoleRegistry registry, String className, HttpServletResponse resp)
            throws IOException {

        Bean<?> bean = registry.getBeans().stream()
                .filter(b -> b.getBeanClass().getName().equals(className))
                .findFirst()
                .orElse(null);

        if (bean == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        InstanceStats stats = registry.getStats(bean.getBeanClass());
        String producedBy = registry.findProducerForBean(bean.getBeanClass())
                .map(info -> info.getMemberSignature())
                .orElse(null);

        BeanDTO dto = new BeanDTO(bean, stats, producedBy);

        writeJson(resp, dto);
    }

    /* ---------------------------------------------------------- */
 /* GET /cdi/beans/{id}/injection-points             */
 /* ---------------------------------------------------------- */
    private void getInjectionPointsForBean(DevConsoleRegistry registry,
            String beanClass,
            HttpServletResponse resp)
            throws IOException {

        var list = registry.getInjectionPointsForBean(beanClass);

        if (list == null || list.isEmpty()) {
            writeJson(resp, List.of());
            return;
        }

        writeJson(resp,
                list.stream()
                        .map(InjectionPointDTO::new)
                        .toList()
        );
    }
}
