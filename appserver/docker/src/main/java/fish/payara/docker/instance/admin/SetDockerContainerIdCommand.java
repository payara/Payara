/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.docker.instance.admin;

import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

import jakarta.inject.Inject;
import java.util.logging.Logger;

/**
 * Internal Asadmin command that sets the Docker container ID registered to an instance. This is used by the
 * Docker entrypoint script to register a container ID against a newly created instance.
 *
 * @author Andrew Pielage
 */
@Service(name = "_set-docker-container-id")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Server.class,
                opType = RestEndpoint.OpType.POST,
                path = "_set-docker-container-id",
                description = "Sets the Docker Container ID for an Instance")
})
public class SetDockerContainerIdCommand implements AdminCommand {

    private static final Logger logger = Logger.getLogger(SetDockerContainerIdCommand.class.getName());

    @Param(name = "instanceName", alias = "instance")
    private String instanceName;

    @Param(name = "containerId", alias = "id")
    private String containerId;

    @Inject
    Servers servers;

    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        // Validate
        if (servers == null) {
            adminCommandContext.getActionReport().failure(logger, "Could not retrieve Servers");
            return;
        }
        if (instanceName == null || instanceName.isEmpty() || containerId == null || containerId.isEmpty()) {
            adminCommandContext.getActionReport().failure(logger, "Instance Name or Container ID empty");
            return;
        }
        Server server = servers.getServer(instanceName);
        if (server == null) {
            adminCommandContext.getActionReport().failure(logger, "Could not find instance with name: "
                    + instanceName);
            return;
        }

        // Attempt to set container ID
        try {
            ConfigSupport.apply(serverProxy -> {
                serverProxy.setDockerContainerId(containerId);
                return serverProxy;
            }, server);
        } catch (TransactionFailure transactionFailure) {
            adminCommandContext.getActionReport().failure(logger, "Could not set Docker Container ID",
                    transactionFailure);
        }
    }
}
