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

import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import javax.inject.Inject;
import java.util.logging.Logger;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Remote AdminCommand to validate the connection to an SSH node.
 * Refactored for DCOM Sept 2011 by Byron Nevins
 * @author Joe Di Pol
 * @author Byron Nevins
 */
public abstract class PingNodeRemoteCommand implements AdminCommand {
    @Inject
    ServiceLocator habitat;
    @Inject
    private Nodes nodes;
    
    @Param(name = "name", primary = true)
    protected String name;
    @Param(optional = true, name = "validate", shortName = "v", alias = "full", defaultValue = "false")
    private boolean validate;
    
    private static final String NL = System.lineSeparator();
    protected abstract String validateSubType(Node node);

    protected final void executeInternal(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        StringBuilder msg = new StringBuilder();
        Node theNode = null;

        Logger logger = context.getLogger();
        NodeUtils nodeUtils = new NodeUtils(habitat, logger);

        // Make sure Node is valid
        theNode = nodes.getNode(name);
        if (theNode == null) {
            String m = Strings.get("noSuchNode", name);
            logger.warning(m);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(m);
            return;
        }

        String err = validateSubType(theNode);
        if (err != null) {
            logger.warning(err);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(err);
            return;
        }

        try {
            String version = "";
            if (validate) {
                // Validates all parameters
                nodeUtils.validate(theNode);
                version = Strings.get("ping.glassfish.version",
                        theNode.getInstallDir(),
                        nodeUtils.getGlassFishVersionOnNode(theNode, context));
            }
            else {
                // Just does a basic connection check
                nodeUtils.pingRemoteConnection(theNode);
            }
            String m1 = Strings.get("ping.node.success", name,
                    theNode.getNodeHost(), theNode.getType());
            if (StringUtils.ok(version)) {
                m1 = m1 + NL + version;
            }
            report.setMessage(m1);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
        catch (CommandValidationException e) {
            String m1 = Strings.get("ping.node.failure", name,
                    theNode.getNodeHost(), theNode.getType());
            msg.append(StringUtils.cat(NL, m1, e.getMessage()));
            report.setMessage(msg.toString());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
}
