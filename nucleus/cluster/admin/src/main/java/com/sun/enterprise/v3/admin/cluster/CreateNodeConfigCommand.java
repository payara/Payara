/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import static com.sun.enterprise.util.net.NetUtils.isThisHostLocal;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.LANDMARK_FILE;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_INSTALLDIR;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_NODEDIR;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_NODEHOST;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_TYPE;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.sanitizeReport;
import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.StringUtils;

/**
 * Remote AdminCommand to create a config node. This command is run only on DAS.
 * Register the config node on DAS
 *
 * @author Carla Mott
 */
@Service(name = "create-node-config")
@I18n("create.node.config")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = Nodes.class,
        opType = POST,
        path = "create-node-config",
        description = "Create Node Config")
})
public class CreateNodeConfigCommand implements AdminCommand {

    @Param(name = "name", primary = true)
    String name;

    @Param(name = "nodedir", optional = true)
    String nodedir;

    @Param(name = "nodehost", optional = true)
    String nodehost;

    @Param(name = "installdir", optional = true)
    String installdir;

    @Inject
    private CommandRunner commandRunner;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        // Validate installdir if passed and running on localhost
        if (StringUtils.ok(nodehost) && isThisHostLocal(nodehost) && StringUtils.ok(installdir)) {
            TokenResolver resolver = null;

            // Create a resolver that can replace system properties in strings
            Map<String, String> systemPropsMap = new HashMap<String, String>((Map) (System.getProperties()));
            resolver = new TokenResolver(systemPropsMap);
            String resolvedInstallDir = resolver.resolve(installdir);
            File actualInstallDir = new File(resolvedInstallDir + File.separator + LANDMARK_FILE);

            if (!actualInstallDir.exists()) {
                report.setMessage(Strings.get("invalid.installdir", installdir));
                report.setActionExitCode(FAILURE);

                return;
            }
        }

        CommandInvocation commandInvocation = commandRunner.getCommandInvocation("_create-node", report, context.getSubject());
        ParameterMap commandParameters = new ParameterMap();

        commandParameters.add("DEFAULT", name);
        if (StringUtils.ok(nodedir)) {
            commandParameters.add(PARAM_NODEDIR, nodedir);
        }
        if (StringUtils.ok(installdir)) {
            commandParameters.add(PARAM_INSTALLDIR, installdir);
        }
        if (StringUtils.ok(nodehost)) {
            commandParameters.add(PARAM_NODEHOST, nodehost);
        }
        commandParameters.add(PARAM_TYPE, "CONFIG");

        commandInvocation.parameters(commandParameters);
        commandInvocation.execute();

        sanitizeReport(report);
    }
}
