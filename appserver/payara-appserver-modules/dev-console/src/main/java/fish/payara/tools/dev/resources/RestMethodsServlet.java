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
import fish.payara.tools.dev.dto.RestMethodDTO;
import fish.payara.tools.dev.model.HTTPRecord;
import fish.payara.tools.dev.rest.RestMetricsRegistry;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Servlet exposing REST method metadata and runtime metrics
 * for the Dev Console.
 *
 * <p>
 * This endpoint provides visibility into REST methods discovered in the
 * application, including their signatures, paths, HTTP method and
 * produced media types, as well as invocation statistics recorded at
 * runtime.
 * </p>
 *
 * <h2>Supported endpoints</h2>
 *
 * <ul>
 *   <li><b>GET /rest/methods</b><br>
 *       Returns a list of all REST methods along with their invocation
 *       counts.</li>
 *
 *   <li><b>GET /rest/methods/{path}</b><br>
 *       Returns detailed information for a single REST method, including
 *       recorded invocation data.</li>
 * </ul>
 *
 * <h2>Response format</h2>
 *
 * <p>
 * Responses are serialized to JSON using JSON-B.
 * The list endpoint returns an array of
 * {@link fish.payara.tools.dev.dto.RestMethodDTO} objects.
 * The detail endpoint returns a
 * {@link fish.payara.tools.dev.dto.RestMethodDTO} object.
 * </p>
 *
 * <h2>Implementation details</h2>
 *
 * <ul>
 *   <li>REST method metadata is obtained from the {@code DevConsoleRegistry}.</li>
 *   <li>Invocation counts and records are populated from
 *       {@link fish.payara.tools.dev.rest.RestMetricsRegistry}.</li>
 *   <li>The detail lookup matches either the method signature or the
 *       registered REST path.</li>
 *   <li>Invocation counts are computed dynamically at request time.</li>
 * </ul>
 *
 * <h2>Error handling</h2>
 *
 * <ul>
 *   <li>{@code 404 Not Found} is returned if the Dev Console is disabled.</li>
 *   <li>{@code 404 Not Found} is returned if the requested REST method
 *       cannot be found.</li>
 * </ul>
 *
 * @author Gaurav Gupta
 */
public class RestMethodsServlet extends AbstractConsoleServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (!guard(resp)) return;

        String path = req.getPathInfo();

        String app = resolveAppName(req);
        DevConsoleRegistry registry = getRegistry(app);
//        RestMetricsRegistry restregistry = getBean(registry.getBeanManager(), RestMetricsRegistry.class);
//        RestMetricsRegistry restregistry =
//        CDI.current().select(RestMetricsRegistry.class).get();
        // list
        if (path == null || "/".equals(path)) {
            writeJson(resp,
                    registry.getRestMethodInfoMap().values().stream()
//                            .peek(v -> v.setInvoked(
//                                    restregistry.getMetrics()
//                                            .getOrDefault(
//                                                    v.getMethodSignature(),
//                                                    List.of())
//                                            .size()))
                            .toList()
            );
            return;
        }

        String lookup = path.substring(1);

        RestMethodDTO found = registry.getRestMethodInfoMap().values().stream()
                .filter(m -> lookup.equals(m.getMethodSignature())
                        || lookup.equals(m.getPath()))
                .findFirst()
                .orElse(null);

        if (found == null) {
            resp.sendError(404);
            return;
        }

//        List<HTTPRecord> records =
//                restregistry.getMetrics().get(found.getMethodSignature());
//
//        found.setRecords(records);

        writeJson(resp, found);
    }
}
