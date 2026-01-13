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
import fish.payara.tools.dev.model.InstanceStats;
import fish.payara.tools.dev.model.ProducerInfo;
import fish.payara.tools.dev.model.ScopedBeanInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Servlet exposing scoped CDI bean information for the Dev Console.
 *
 * <p>
 * This endpoint provides a summary view of all CDI beans that declare
 * an explicit scope. For each bean, it reports the declared scope,
 * qualifiers, bean types, and lifecycle statistics observed at runtime.
 * </p>
 *
 * <h2>Supported endpoint</h2>
 *
 * <ul>
 *   <li><b>GET /cdi/scoped-beans</b><br>
 *       Returns a list of scoped beans as
 *       {@link fish.payara.tools.dev.model.ScopedBeanInfo} objects.</li>
 * </ul>
 *
 * <h2>Response format</h2>
 *
 * <p>
 * The response is serialized to JSON using JSON-B and contains a JSON
 * array of scoped bean descriptors.
 * </p>
 *
 * <h2>Implementation details</h2>
 *
 * <ul>
 *   <li>Each bean is represented using {@link ScopedBeanInfo}, which
 *       includes scope name, qualifiers, exposed types, and bean name.</li>
 *   <li>If the bean is produced via a producer method or field, the
 *       producing member signature is included when available.</li>
 *   <li>Lifecycle statistics such as creation count, destruction count,
 *       and current instance count are populated from
 *       {@link fish.payara.tools.dev.model.InstanceStats}.</li>
 *   <li>Beans without an explicit scope are excluded from this endpoint.</li>
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
public class ScopedBeansServlet extends AbstractConsoleServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // Same guard semantics as JAX-RS
        if (!guard(resp)) {
            return;
        }

        String app = resolveAppName(req);
        DevConsoleRegistry registry = getRegistry(app);
        List<ScopedBeanInfo> beans = registry.getBeans().stream()
                .map(bean -> {

                    ScopedBeanInfo info = new ScopedBeanInfo(
                            bean,
                            registry.findProducerForBean(bean.getBeanClass())
                                    .map(ProducerInfo::getMemberSignature)
                                    .orElse(null)
                    );

                    InstanceStats stats = registry.getStats(bean.getBeanClass());

                    // stats is always expected to exist, but be defensive
                    if (stats != null) {
                        info.setCreatedCount(stats.getCreatedCount().get());
                        info.setLastCreated(stats.getLastCreated().get());
                        info.setCurrentCount(stats.getCurrentCount().get());
                        info.setMaxCount(stats.getMaxCount().get());
                        info.setDestroyedCount(stats.getDestroyedCount().get());
                    }

                    return info;
                })
                .toList();

        writeJson(resp, beans);
    }
}
