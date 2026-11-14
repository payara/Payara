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

package fish.payara.admin.cluster;

import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.v3.admin.cluster.DeleteNodeRemoteCommand;
import com.sun.enterprise.v3.admin.cluster.NodeUtils;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.util.List;

import static org.glassfish.api.admin.RestEndpoint.OpType.DELETE;

@Service(name = "delete-node-winrm")
@I18n("delete.node.winrm")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
        @RestEndpoint(configBean = Nodes.class,
                opType = DELETE,
                path = "delete-node-winrm",
                description = "Delete Node WinRM")
})
public class DeleteNodeWinrmCommand extends DeleteNodeRemoteCommand {
    @Override
    protected List<String> getPasswords() {
        // TODO: WinRM. Do we need this?
        return List.of();
    }

    @Override
    protected String getUninstallCommandName() {
        return "uninstall-node-winrm";
    }

    @Override
    protected void setTypeSpecificOperands(List<String> command, ParameterMap map) {
        command.add("--winrmport");
        command.add(map.getOne(NodeUtils.PARAM_REMOTE_WINRM_PORT));

        command.add("--winrmuser");
        command.add(map.getOne(NodeUtils.PARAM_REMOTE_WINRM_USER));

        command.add("--winrmpassword");
        command.add(map.getOne(NodeUtils.PARAM_REMOTE_WINRM_PASSWORD));
    }

    @Override
    public void execute(AdminCommandContext context) {
        executeInternal(context);
    }
}
