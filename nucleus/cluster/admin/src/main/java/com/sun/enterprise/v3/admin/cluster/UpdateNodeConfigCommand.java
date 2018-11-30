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
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;


import org.jvnet.hk2.annotations.Service;
import java.util.logging.Logger;

/**
 * Remote AdminCommand to update a config node.
 *
 * @author Joe Di Pol
 */
@Service(name = "update-node-config")
@I18n("update.node.config")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Node.class,
        opType=RestEndpoint.OpType.POST, 
        path="update-node-config", 
        description="Update Node Config",
        params={
            @RestParam(name="id", value="$parent")
        })
})
public class UpdateNodeConfigCommand implements AdminCommand  {

    @Inject
    private CommandRunner cr;

    @Inject
    ServiceLocator habitat;

    @Inject
    private Nodes nodes;

    @Param(name="name", primary = true)
    private String name;

    @Param(name="nodehost", optional=true)
    private String nodehost;

    @Param(name = "installdir", optional=true)
    private String installdir;

    @Param(name = "nodedir", optional=true)
    private String nodedir;

    private static final String NL = System.lineSeparator();

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        StringBuilder msg = new StringBuilder();
        Node node = null;
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        Logger logger = context.getLogger();

        // Make sure Node is valid
        node = nodes.getNode(name);
        if (node == null) {
            String m = Strings.get("noSuchNode", name);
            logger.warning(m);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(m);
            return;
        }

        if (node.isDefaultLocalNode()) {
            String m = Strings.get("update.node.config.defaultnode", name);
            logger.warning(m);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(m);
            return;
        }

        // After updating the config node it needs to have a host
        if (!StringUtils.ok(nodehost) && !StringUtils.ok(node.getNodeHost())) {
            String m = Strings.get("update.node.config.missing.attribute",
                    node.getName(), NodeUtils.PARAM_NODEHOST);
            logger.warning(m);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(m);
            return;
        }

        ParameterMap map = new ParameterMap();
        map.add("DEFAULT", name);

        if (installdir != null) {
            map.add(NodeUtils.PARAM_INSTALLDIR, installdir);
        }
        if (nodehost != null) {
            map.add(NodeUtils.PARAM_NODEHOST, nodehost);
        }
        if (nodedir != null) {
            map.add(NodeUtils.PARAM_NODEDIR, nodedir);
        }

        map.add(NodeUtils.PARAM_TYPE, "CONFIG");

        if (map.size() > 1) {
            CommandInvocation ci = cr.getCommandInvocation("_update-node", report, context.getSubject());
            ci.parameters(map);
            ci.execute();

            if (StringUtils.ok(report.getMessage())) {
                if (msg.length() > 0) {
                    msg.append(NL);
                }
                msg.append(report.getMessage());
            }

            report.setMessage(msg.toString());
        }
    }
}
