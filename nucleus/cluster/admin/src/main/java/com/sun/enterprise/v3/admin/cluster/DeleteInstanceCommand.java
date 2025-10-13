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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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

import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;
import static org.glassfish.api.ActionReport.ExitCode.WARNING;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.util.StringUtils;

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
    
    @Param(name = "instance_name", primary = true)
    private String instanceName;

    @Param(defaultValue = "false", optional = true)
    private boolean terse;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Servers servers;

    @Inject
    private Nodes nodes;

    private Server instance;
    private String noderef;
    private String nodedir;
    private Logger logger;
    private String instanceHost;
    private Node theNode;

    @Override
    public void execute(AdminCommandContext ctx) {
        ActionReport report = ctx.getActionReport();
        logger = ctx.getLogger();
        String msg = "";
        boolean fsfailure = false;
        boolean configfailure = false;

        // We are going to delete a server instance. Get the instance
        instance = servers.getServer(instanceName);

        if (instance == null) {
            msg = Strings.get("start.instance.noSuchInstance", instanceName);
            logger.warning(msg);
            report.setActionExitCode(FAILURE);
            report.setMessage(msg);
            
            return;
        }
        
        instanceHost = instance.getAdminHost();

        // make sure instance is not running.
        if (instance.isRunning()) {
            msg = Strings.get("instance.shutdown", instanceName);
            logger.warning(msg);
            report.setActionExitCode(FAILURE);
            report.setMessage(msg);
            
            return;
        }

        // We attempt to delete the instance filesystem first by running
        // _delete-instance-filesystem. We then remove the instance
        // from the config no matter if we could delete the files or not.

        // Get the name of the node from the instance's node-ref field
        noderef = instance.getNodeRef();
        if (!StringUtils.ok(noderef)) {
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

            if (theNode.getType().equals("DOCKER")) {
                deleteDockerContainer(ctx);
            } else if (!theNode.getType().equals("TEMP")){
                deleteInstanceFilesystem(ctx);
            }

            report = ctx.getActionReport();
            if (report.getActionExitCode() != SUCCESS) {
                fsfailure = true;
            }
            msg = report.getMessage();
        }

        // Now remove the instance from domain.xml.
        CommandInvocation commandInvocation = commandRunner.getCommandInvocation("_unregister-instance", report, ctx.getSubject());
        ParameterMap commandParameters = new ParameterMap();
        commandParameters.add("DEFAULT", instanceName);
        commandInvocation.parameters(commandParameters);
        commandInvocation.execute();

        if (report.getActionExitCode() != SUCCESS && report.getActionExitCode() != WARNING) {
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
            msg = msg + NL + NL + Strings.get("delete.instance.failed", instanceName, instanceHost);
        } else if (configfailure && !fsfailure) {
            msg = msg + NL + NL + Strings.get("delete.instance.config.failed", instanceName, instanceHost);
        } else if (!configfailure && fsfailure) {
            report.setActionExitCode(WARNING);
            // leave msg as is
        }

        if (configfailure) {
            report.setActionExitCode(FAILURE);
            report.setMessage(msg);
        } else {
            if (theNode.getType().equals("TEMP") && !theNode.nodeInUse()) {
                deleteTempNode(ctx);
            }
        }
    }

    private void deleteInstanceFilesystem(AdminCommandContext ctx) {
        NodeUtils nodeUtils = new NodeUtils(serviceLocator, logger);
        ArrayList<String> command = new ArrayList<>();

        command.add("_delete-instance-filesystem");

        if (nodedir != null) {
            command.add("--nodedir");
            command.add(nodedir); // XXX escape spaces?
        }

        command.add("--node");
        command.add(noderef);

        command.add(instanceName);

        String humanCommand = makeCommandHuman(command);

        // First error message displayed if we fail
        String firstErrorMessage = Strings.get("delete.instance.filesystem.failed", instanceName, noderef, theNode.getNodeHost());

        StringBuilder output = new StringBuilder();

        // Run the command on the node and handle errors.
        nodeUtils.runAdminCommandOnNode(theNode, command, ctx, firstErrorMessage, humanCommand, output);

        ActionReport report = ctx.getActionReport();

        if (report.getActionExitCode() != SUCCESS) {
            return;
        }

        // If it was successful say so and display the command output
        String msg = Strings.get("delete.instance.success", instanceName, theNode.getNodeHost());
        if (!terse) {
            msg = StringUtils.cat(NL, output.toString().trim(), msg);
        }
        
        report.setMessage(msg);
    }

    private void deleteDockerContainer(AdminCommandContext ctx) {
        ActionReport actionReport = ctx.getActionReport();

        CommandInvocation commandInvocation = commandRunner.getCommandInvocation("_delete-docker-container",
                actionReport, ctx.getSubject());

        ParameterMap commandParameters = new ParameterMap();
        commandParameters.add("node", noderef);
        commandParameters.add("DEFAULT", instanceName);
        commandInvocation.parameters(commandParameters);
        commandInvocation.execute();

        if (actionReport.getActionExitCode() != SUCCESS) {
            actionReport.setMessage(Strings.get("delete.docker.container.failure",
                    instanceName, noderef, theNode.getNodeHost()));
            return;
        }

        actionReport.setMessage(Strings.get("delete.docker.container.success",
                instanceName, noderef, theNode.getNodeHost()));
    }

    private void deleteTempNode(AdminCommandContext ctx) {
        ActionReport actionReport = ctx.getActionReport().addSubActionsReport();

        if (theNode.nodeInUse()) {
            logger.log(Level.FINE, "Temporary node " + theNode.getName()
                    + " still detected as having instances registered to it, skipping deletion.");
        } else {
            CommandInvocation commandInvocation = commandRunner.getCommandInvocation("_delete-node-temp",
                    actionReport, ctx.getSubject());
            ParameterMap commandParameters = new ParameterMap();
            commandParameters.add("DEFAULT", theNode.getName());
            commandInvocation.parameters(commandParameters);
            commandInvocation.execute();
        }
    }

    private String makeCommandHuman(List<String> commands) {
        StringBuilder fullCommand = new StringBuilder();

        fullCommand.append("lib");
        fullCommand.append(System.getProperty("file.separator"));
        fullCommand.append("nadmin ");

        for (String command : commands) {
            if (command.equals("_delete-instance-filesystem")) {
                // We tell the user to run delete-local-instance, not the
                // hidden command
                fullCommand.append(" ");
                fullCommand.append("delete-local-instance");
            } else {
                fullCommand.append(" ");
                fullCommand.append(command);
            }
        }

        return fullCommand.toString();
    }

}
