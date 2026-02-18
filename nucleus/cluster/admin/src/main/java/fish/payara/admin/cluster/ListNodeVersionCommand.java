/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package fish.payara.admin.cluster;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

@Service(name = "list-node-versions")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.node.versions.command")
@RestEndpoints({
        @RestEndpoint(configBean = Nodes.class,
                opType = RestEndpoint.OpType.GET,
                description = "list node versions")
})
public class ListNodeVersionCommand implements AdminCommand {
    @Inject
    private ServiceLocator habitat;

    @Inject
    private Nodes nodes;

    @Param(optional = true)
    private boolean terse;

    @Param(optional = true, name = "node")
    private String singleNode;

    private ActionReport report;
    Logger logger;

    @Override
    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        logger = context.getLogger();

        List<Map<String, String>> rows = new ArrayList<>();

        for (Node n : nodes.getNode()) {

            String name = n.getName();
            if (singleNode != null && !singleNode.isBlank() && !singleNode.equals(name)) {
                continue;
            }

            NodeVersionInfo info = ListNodeVersionHelper.collect(habitat, n, logger);

            Map<String, String> row = new LinkedHashMap<>();
            row.put("node", name);
            row.put("current", info.currentVersion());
            row.put("staged", info.stagedVersion());
            row.put("old", info.oldVersion());
            rows.add(row);
        }

        report.setMessage(NodeVersionTableFormatter.format(rows, terse));

        Properties extra = new Properties();
        extra.put("nodeVersions", rows);
        report.setExtraProperties(extra);

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}

final class NodeVersionTableFormatter {

    static String format(List<Map<String, String>> rows, boolean terse) {
        if (terse) {
            // e.g. node=current,staged,old
            StringBuilder sb = new StringBuilder();
            for (Map<String, String> r : rows) {
                sb.append(r.get("node")).append("=")
                        .append(r.getOrDefault("current", "")).append(",")
                        .append(r.getOrDefault("staged", "")).append(",")
                        .append(r.getOrDefault("old", ""))
                        .append("\n");
            }
            return sb.toString().trim();
        }

        int nodeW = "Nodes".length();
        int curW = "Current".length();
        int stgW = "Staged".length();
        int oldW = "Old".length();

        for (Map<String, String> r : rows) {
            nodeW = Math.max(nodeW, len(r.get("node")));
            curW = Math.max(curW, len(r.get("current")));
            stgW = Math.max(stgW, len(r.get("staged")));
            oldW = Math.max(oldW, len(r.get("old")));
        }

        String fmt = "%-" + nodeW + "s  %-" + curW + "s  %-" + stgW + "s  %-" + oldW + "s%n";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(fmt, "Nodes", "Current", "Staged", "Old"));
        for (Map<String, String> r : rows) {
            sb.append(String.format(fmt,
                    r.getOrDefault("node", ""),
                    r.getOrDefault("current", ""),
                    r.getOrDefault("staged", ""),
                    r.getOrDefault("old", "")
            ));
        }
        return sb.toString().trim();
    }

    private static int len(String s) {
        return s == null ? 0 : s.length();
    }
}