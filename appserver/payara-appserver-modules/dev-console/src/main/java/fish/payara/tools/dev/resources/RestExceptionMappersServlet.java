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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet exposing REST exception mapper information for the Dev Console.
 *
 * <p>
 * This endpoint provides visibility into exception mappers registered
 * for REST endpoints in the application. Each entry describes the
 * exception type and the corresponding mapper class responsible for
 * handling it.
 * </p>
 *
 * <h2>Supported endpoint</h2>
 *
 * <ul>
 *   <li><b>GET /rest/exception-mappers</b><br>
 *       Returns a mapping of exception types to their corresponding
 *       exception mapper class names.</li>
 * </ul>
 *
 * <h2>Response format</h2>
 *
 * <p>
 * The response is serialized to JSON using JSON-B and consists of a
 * JSON object where keys represent exception type names and values
 * represent the associated mapper class names.
 * </p>
 *
 * <h2>Implementation details</h2>
 *
 * <ul>
 *   <li>Exception mapper information is obtained from the
 *       {@code DevConsoleRegistry}.</li>
 *   <li>Only exception mappers registered with the REST subsystem
 *       are included.</li>
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
public class RestExceptionMappersServlet extends AbstractConsoleServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (!guard(resp)) return;

        String app = resolveAppName(req);
        DevConsoleRegistry registry = getRegistry(app);
        writeJson(resp, registry.getRestExceptionMappers());
    }
}
