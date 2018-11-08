/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.StringUtils;
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
import java.util.List;
import java.util.ArrayList;

/**
 * Remote AdminCommand to delete an instance.  This command is run only on DAS.
 *
 * @author Jennifer Chou
 */
@Service(name = "delete-instance")
@I18n("delete.instance")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.DELETE, 
        path="delete-instance", 
        description="Delete Instance",
        params={
            @RestParam(name="id", value="$parent")
        })
})
public class DeleteInstanceCommand implements AdminCommand {

    private static final String NL = System.lineSeparator();

    @Inject
    private CommandRunner cr;

    @Inject
    ServiceLocator habitat;

    @Inject
    private Servers servers;

    @Inject
    private Nodes nodes;

    @Param(name = "instance_name", primary = true)
    private String instanceName;

    @Param(defaultValue = "false", optional=true)
    private boolean terse;

    private Server instance;
    private String noderef;
    private String nodedir;
    @SuppressWarnings("NonConstantLogger")
    private Logger logger;    
    private String instanceHost;
    private Node theNode = null;

    @Override
    public void execute(AdminCommandContext ctx) {
        ActionReport report = ctx.getActionReport();
        logger = ctx.getLogger();
        String msg = "";
        boolean  fsfailure = false;
        boolean  configfailure = false;

        // We are going to delete a server instance. Get the instance
        instance = servers.getServer(instanceName);

        if (instance == null) {
            msg = Strings.get("start.instance.noSuchInstance", instanceName);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
        instanceHost = instance.getAdminHost();

        // make sure instance is not running.
        if (instance.isRunning()){
            msg = Strings.get("instance.shutdown", instanceName);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        // We attempt to delete the instance filesystem first by running
        // _delete-instance-filesystem. We then remove the instance
        // from the config no matter if we could delete the files or not.

        // Get the name of the node from the instance's node-ref field
        noderef = instance.getNodeRef();
        if(!StringUtils.ok(noderef)) {
            msg = Strings.get("missingNodeRef", instanceName);
            fsfailure = true;
        } else {
            theNode = nodes.getNode(noderef);
            if (theNode == null) {
                msg = Strings.get("noSuchNode", noderef);
                fsfailure = true;
            }
        }

        if (!fsfailure) {
            nodedir = theNode.getNodeDirAbsolute();
            deleteInstanceFilesystem(ctx);
            report = ctx.getActionReport();
            if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS) {
                fsfailure = true;
            }
            msg = report.getMessage();
        }

        // Now remove the instance from domain.xml.
        CommandInvocation ci = cr.getCommandInvocation("_unregister-instance", report, ctx.getSubject());
        ParameterMap map = new ParameterMap();
        map.add("DEFAULT", instanceName);
        ci.parameters(map);
        ci.execute();

        if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS &&
            report.getActionExitCode() != ActionReport.ExitCode.WARNING) {
            // Failed to delete from domain.xml
            configfailure = true;
            if (fsfailure) {
                // Failed to delete instance from fs too
                msg = msg + NL + report.getMessage();
            } else {
                msg = report.getMessage();
            }
        }

        // OK, try to give a helpful message depending on the failure
        if (configfailure && fsfailure) {
            msg = msg + NL + NL + Strings.get("delete.instance.failed",
                    instanceName, instanceHost);
        } else if (configfailure && !fsfailure) {
            msg = msg + NL + NL + Strings.get("delete.instance.config.failed",
                    instanceName, instanceHost);
        } else if (!configfailure && fsfailure) {
            report.setActionExitCode(ActionReport.ExitCode.WARNING);            
            // leave msg as is
        }

        if (configfailure) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
        }
    }

    private void deleteInstanceFilesystem(AdminCommandContext ctx) {

        NodeUtils nodeUtils = new NodeUtils(habitat, logger);
        ArrayList<String> command = new ArrayList<String>();
        String humanCommand = null;

        command.add("_delete-instance-filesystem");

        if (nodedir != null) {
            command.add("--nodedir");
            command.add(nodedir); //XXX escape spaces?
        }

        command.add("--node");
        command.add(noderef);

        command.add(instanceName);

        humanCommand = makeCommandHuman(command);

        // First error message displayed if we fail
        String firstErrorMessage = Strings.get("delete.instance.filesystem.failed",
                        instanceName, noderef, theNode.getNodeHost() );

        StringBuilder output = new StringBuilder();

        // Run the command on the node and handle errors.
        nodeUtils.runAdminCommandOnNode(theNode, command, ctx, firstErrorMessage,
                humanCommand, output);

        ActionReport report = ctx.getActionReport();

        if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS) {
            return;
        }

        // If it was successful say so and display the command output
        String msg = Strings.get("delete.instance.success",
                    instanceName, theNode.getNodeHost());
        if (!terse) {
            msg = StringUtils.cat(NL, output.toString().trim(), msg);
        }
        report.setMessage(msg);
    }

    private String makeCommandHuman(List<String> command) {
        StringBuilder fullCommand = new StringBuilder();

        fullCommand.append("lib");
        fullCommand.append(System.getProperty("file.separator"));
        fullCommand.append("nadmin ");

        for (String s : command) {
            if (s.equals("_delete-instance-filesystem")) {
                // We tell the user to run delete-local-instance, not the
                // hidden command
                fullCommand.append(" ");
                fullCommand.append("delete-local-instance");
            } else {
                fullCommand.append(" ");
                fullCommand.append(s);
            }
        }

        return fullCommand.toString();
    }

}
