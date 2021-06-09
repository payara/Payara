/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.certificate.management.admin;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.SSHCommandExecutionException;
import org.glassfish.cluster.ssh.connect.NodeRunner;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import java.io.File;

/**
 * Remote Admin Command that adds a certificate or bundle to the truststore.
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Service(name = "_add-truststore-entry")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value = {RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE})
@RestEndpoints({
        @RestEndpoint(configBean = Server.class,
                opType = RestEndpoint.OpType.POST,
                path = "add-truststore-entry",
                description = "Removes Keys and Certificates from trust store",
                params = {
                        @RestParam(name = "target", value = "$parent")
                }
        )
})
public class AddTruststoreEntryCommand extends AbstractRemoteCertificateManagementCommand {

    @Param(name = "filePath", alias="filepath")
    private String filePath;

    @Param(name = "alias", primary = true)
    private String alias;

    @Inject
    private Nodes nodes;

    @Override
    public void execute(AdminCommandContext context) {
        // Check if this instance is the target - we only want to run on the local instance
        if (StringUtils.ok(target) && !target.equals(serverEnvironment.getInstanceName())) {
            return;
        }

        resolveTrustStore();

        try {
            addToTrustStore(context);
        } catch (CommandException e) {
            context.getActionReport().failure(context.getLogger(), "Error adding truststore entry", e);
            return;
        }
    }

    /**
     * Runs the 'add-to-truststore' command on the local instance.
     * @param context The admin command context.
     * @throws CommandException If there's an issue running the command.
     */
    private void addToTrustStore(AdminCommandContext context) throws CommandException {
        NodeRunner nodeRunner = new NodeRunner(serviceLocator, context.getLogger());
        Node node = nodes.getNode(servers.getServer(serverEnvironment.getInstanceName()).getNodeRef());

        // The DAS doesn't have a node-ref
        if (node == null && serverEnvironment.isDas()) {
            node = nodes.getDefaultLocalNode();
        }

        try {
            StringBuilder stringBuilder = new StringBuilder();

            nodeRunner.runAdminCommandOnNode(node, stringBuilder,
                    createAddToStoreCommand("add-to-truststore", node, new File(filePath), alias), context);

            if (stringBuilder.toString().contains("Command add-to-truststore failed")) {
                throw new CommandException();
            }
        } catch (SSHCommandExecutionException | ProcessManagerException e) {
            throw new CommandException(e);
        }
    }
}