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
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.cluster.RemoteType;
import com.sun.enterprise.v3.admin.cluster.CreateRemoteNodeCommand;
import com.sun.enterprise.v3.admin.cluster.NodeUtils;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.List;

import static com.sun.enterprise.v3.admin.cluster.NodeUtils.NODE_DEFAULT_REMOTE_USER;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;

@Service(name = "create-node-winrm")
@I18n("create.node.winrm")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
        @RestEndpoint(configBean = Nodes.class,
                opType = POST,
                path = "create-node-winrm",
                description = "Create Node WinRM")
})
public class CreateNodeWinrmCommand extends CreateRemoteNodeCommand {
    @Param(name = "winrmport", optional = true, defaultValue = NodeUtils.NODE_DEFAULT_WINRM_PORT)
    private String winrmPort;

    @Param(name = "winrmuser", optional = true, defaultValue = NODE_DEFAULT_REMOTE_USER)
    private String winrmUser;

    @Param(name = "winrmpassword", optional = true, password = true)
    private String winrmPassword;

    @Override
    protected void populateBaseClass() {
        remotePort = winrmPort;
        remoteUser = winrmUser;
        remotePassword = winrmPassword;
    }

    @Override
    protected void initialize() {
        // Nothing to do?
    }

    @Override
    protected void populateParameters(ParameterMap pmap) {
        pmap.add(NodeUtils.PARAM_REMOTE_WINRM_USER, remoteUser);
        pmap.add(NodeUtils.PARAM_REMOTE_WINRM_PASSWORD, remotePassword);
        pmap.add(NodeUtils.PARAM_REMOTE_WINRM_PORT, remotePort);
    }

    @Override
    protected void populateCommandArgs(List<String> args) {
        args.add("--winrmuser");
        args.add(remoteUser);
        args.add("--winrmpassword");
        args.add(remotePassword);
        args.add("--winrmport");
        args.add(remotePort);
    }

    @Override
    protected RemoteType getType() {
        return RemoteType.WINRM;
    }

    @Override
    protected void validate() throws CommandValidationException {
        // Nothing to do?
    }

    @Override
    protected List<String> getPasswords() {
        // TODO: WinRM. Do we need this?
        return new ArrayList<>();
    }

    @Override
    protected String getInstallNodeCommandName() {
        return "install-node-winrm";
    }

    @Override
    public void execute(AdminCommandContext context) {
        populateBaseClass();
        executeInternal(context);
    }

    @Override
    protected void checkDefaults() {
        super.checkDefaults();

        if (!StringUtils.ok(remotePort)) {
            remotePort = NodeUtils.NODE_DEFAULT_WINRM_PORT;
        }
    }
}
