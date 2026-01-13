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

/**
 * Servlet exposing CDI extensions discovered in the application.
 *
 * <p>
 * This endpoint lists all CDI extensions that are present in the
 * application and visible to the Dev Console. Extensions are identified
 * by beans whose classes implement {@link jakarta.enterprise.inject.spi.Extension}.
 * </p>
 *
 * <h2>Supported endpoint</h2>
 *
 * <ul>
 *   <li><b>GET /cdi/extension</b><br>
 *       Returns a sorted list of fully-qualified class names of CDI
 *       extensions.</li>
 * </ul>
 *
 * <h2>Response format</h2>
 *
 * <p>
 * The response is serialized to JSON using JSON-B and contains a JSON
 * array of strings, where each string represents the fully-qualified
 * class name of a CDI extension.
 * </p>
 *
 * <h2>Implementation details</h2>
 *
 * <ul>
 *   <li>Extension classes are discovered by inspecting registered CDI beans.</li>
 *   <li>Only classes implementing {@code Extension} are included.</li>
 *   <li>Duplicate entries are removed and results are returned in
 *       lexicographical order.</li>
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
public class ExtensionsServlet extends AbstractConsoleServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (!guard(resp)) return;

        String app = resolveAppName(req);
        DevConsoleRegistry registry = getRegistry(app);
        writeJson(resp,
                registry.getBeans().stream()
                        .map(Bean::getBeanClass)
                        .filter(Extension.class::isAssignableFrom)
                        .map(Class::getName)
                        .distinct()
                        .sorted()
                        .toList()
        );
    }
}
