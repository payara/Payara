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
import fish.payara.tools.dev.dto.DecoratorDTO;
import fish.payara.tools.dev.model.InstanceStats;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Servlet exposing CDI decorator metadata and runtime statistics for the Dev
 * Console.
 *
 * <p>
 * This servlet provides both a summary view of all decorators and a detailed
 * view for an individual decorator.
 * </p>
 *
 * <h2>Supported endpoints</h2>
 *
 * <ul>
 * <li><b>GET /cdi/decorators</b><br>
 * Returns a list of all discovered CDI decorators as
 * {@link fish.payara.tools.dev.dto.DecoratorDTO}.</li>
 *
 * <li><b>GET /cdi/decorators/{className}</b><br>
 * Returns full metadata and invocation statistics for the specified decorator
 * as {@link fish.payara.tools.dev.dto.DecoratorDTO}.</li>
 * </ul>
 *
 * <h2>Response format</h2>
 *
 * <p>
 * Responses are serialized to JSON using JSON-B, ensuring type-safe output
 * consistent with the former JAX-RS implementation.
 * </p>
 *
 * <h2>Implementation details</h2>
 *
 * <ul>
 * <li>Decorator metadata is obtained from the {@code DevConsoleRegistry}.</li>
 * <li>Runtime statistics are populated from {@code InstanceStats} when
 * available.</li>
 * <li>The detailed endpoint additionally exposes invocation records.</li>
 * <li>Routing is performed based on the request path to distinguish between
 * list and detail requests.</li>
 * </ul>
 *
 * <h2>Error handling</h2>
 *
 * <ul>
 * <li>{@code 404 Not Found} is returned if the Dev Console is disabled.</li>
 * <li>{@code 404 Not Found} is returned if the requested decorator class cannot
 * be found.</li>
 * </ul>
 *
 * @author Gaurav Gupta
 */
public class DecoratorsServlet extends AbstractConsoleServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (!guard(resp)) {
            return;
        }

        String path = req.getPathInfo();
        String app = resolveAppName(req);
        DevConsoleRegistry registry = getRegistry(app);

        // GET /dev/decorators
        if (path == null || "/".equals(path)) {
            listDecorators(registry, resp);
            return;
        }

        // GET /dev/decorators/{className}
        String className = path.substring(1);
        getDecoratorByClassName(registry, className, resp);
    }

    /* ---------------------------------------------------------- */
 /* GET /dev/decorators                                        */
 /* ---------------------------------------------------------- */
    private void listDecorators(DevConsoleRegistry registry, HttpServletResponse resp)
            throws IOException {

        List<DecoratorDTO> decorators = registry.getDecorators().stream()
                .map(bean -> {

                    DecoratorDTO info = new DecoratorDTO(bean);

                    InstanceStats stats
                            = registry.getStats(bean.getClassName());

                    if (stats != null) {
                        info.setCreatedCount(stats.getCreatedCount().get());
                        info.setLastCreated(stats.getLastCreated().get());
                        info.setInvokedCount(stats.getInvocationCount().get());
                        info.setLastInvoked(stats.getLastInvoked().get());
                    }

                    return info;
                })
                .toList();

        writeJson(resp, decorators);
    }

    /* ---------------------------------------------------------- */
 /* GET /dev/decorators/{className}                             */
 /* ---------------------------------------------------------- */
    private void getDecoratorByClassName(DevConsoleRegistry registry,
            String className,
            HttpServletResponse resp)
            throws IOException {

        var bean = registry.getDecorators().stream()
                .filter(i -> i.getClassName().equals(className))
                .findFirst()
                .orElse(null);

        if (bean == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Decorator not found: " + className);
            return;
        }

        DecoratorDTO info = new DecoratorDTO(bean);

        InstanceStats stats = registry.getStats(bean.getClassName());
        if (stats != null) {
            info.setCreatedCount(stats.getCreatedCount().get());
            info.setLastCreated(stats.getLastCreated().get());
            info.setInvokedCount(stats.getInvocationCount().get());
            info.setLastInvoked(stats.getLastInvoked().get());
            info.setInvocationRecords(stats.getInvocationRecords());
        }

        writeJson(resp, info);
    }
}
