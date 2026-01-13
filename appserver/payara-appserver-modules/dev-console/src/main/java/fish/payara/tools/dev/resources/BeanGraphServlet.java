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
import fish.payara.tools.dev.dto.BeanGraphDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Gaurav Gupta
 */
public class BeanGraphServlet extends AbstractConsoleServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (!guard(resp)) {
            return;
        }

        String path = req.getPathInfo();
        String app = resolveAppName(req);
        DevConsoleRegistry registry = getRegistry(app);

        if (path == null || "/".equals(path)) {
            writeJson(resp, registry.getBeanGraph());
            return;
        }

        String id = path.substring(1);
        BeanGraphDTO graph = registry.getBeanGraph();

        if (graph == null || graph.getNodes() == null) {
            resp.sendError(500, "Bean graph not available");
            return;
        }

        BeanGraphDTO.BeanNode root = graph.getNodes().get(id);
        if (root == null) {
            resp.sendError(404, "Bean with id '" + id + "' not found");
            return;
        }

        // Same recursive logic as JAX-RS
        Map<String, BeanGraphDTO.BeanNode> subgraphNodes = new LinkedHashMap<>();
        collectRecursive(root, graph.getNodes(), subgraphNodes, new HashSet<>());

        BeanGraphDTO subgraph = new BeanGraphDTO();
        subgraphNodes.forEach((beanId, node) -> {
            subgraph.addNode(beanId, node.getDescription());
            subgraph.getNodes().get(beanId).setCircular(node.isCircular());
        });

        subgraphNodes.forEach((beanId, node) -> {
            for (var dep : node.getDependencies()) {
                if (subgraphNodes.containsKey(dep.getBeanId())) {
                    subgraph.addDependency(beanId, dep.getBeanId());
                }
            }
        });

        writeJson(resp, subgraph);
    }

    private void collectRecursive(
            BeanGraphDTO.BeanNode node,
            Map<String, BeanGraphDTO.BeanNode> allNodes,
            Map<String, BeanGraphDTO.BeanNode> result,
            Set<String> visiting) {

        String id = node.getBeanId();

        // Cycle detected
        if (visiting.contains(id)) {
            node.setCircular(true);
            return;
        }

        if (result.containsKey(id)) {
            return;
        }

        visiting.add(id);
        result.put(id, node);

        for (BeanGraphDTO.BeanNode dep : node.getDependencies()) {
            collectRecursive(dep, allNodes, result, visiting);

            // Propagate cycle flag upwards if needed
            if (dep.isCircular()) {
                node.setCircular(true);
            }
        }

        visiting.remove(id);
    }

}
