/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.v3.admin.cluster;

import static com.sun.enterprise.util.SystemPropertyConstants.AGENT_ROOT_PROPERTY;
import static java.util.logging.Level.SEVERE;
import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;
import static org.glassfish.api.ActionReport.ExitCode.WARNING;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.inject.Inject;

import fish.payara.util.cluster.PayaraServerNameGenerator;
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
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.util.ExceptionUtil;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.InstanceDirs;

/**
 * Remote AdminCommand to create an instance.  This command is run only on DAS.
 *  1. Register the instance on DAS
 *  2. Create the file system on the instance node via ssh, DCOM, node agent, or other
 *  3. Bootstrap a minimal set of config files on the instance for secure admin.
 *
 * @author Jennifer Chou
 */
@Service(name = "create-instance")
@I18n("create.instance")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=POST, 
        path="create-instance", 
        description="Create Instance")
})
public class CreateInstanceCommand implements AdminCommand {
    private static final String NEWLINE = System.lineSeparator();
    
    @Param(name = "node", alias = "nodeagent")
    String node;
    
    @Param(name = "config", optional = true)
    @I18n("generic.config")
    String configRef;
    
    @Param(name = "cluster", optional = true)
    String clusterName;
    
    @Param(name = "deploymentgroup", optional = true)
    String deploymentGroup;
    
    @Param(name = "lbenabled", optional = true)
    private Boolean lbEnabled;
    
    @Param(name = "checkports", optional = true, defaultValue = "true")
    private boolean checkPorts;
    
    @Param(optional = true, defaultValue = "false")
    private boolean terse;
    
    @Param(name = "portbase", optional = true)
    private String portBase;
    
    @Param(name = "systemproperties", optional = true, separator = ':')
    private String systemProperties;

    @Param(name = "autoname", optional = true, shortName = "a", defaultValue = "false")
    private boolean autoName;

    @Param(name = "extraterse", optional = true, shortName = "T", defaultValue = "false")
    private boolean extraTerse;

    @Param(name = "instance_name", primary = true)
    private String instance;
    
    @Inject
    private CommandRunner commandRunner;
    
    @Inject
    private ServiceLocator serviceLocator;
    
    @Inject
    private Nodes nodes;
    
    @Inject
    private Servers servers;
    
    @Inject
    private ServerEnvironment env;
    
    private Logger logger; // set in execute and all references occur after that assignment
    private AdminCommandContext ctx;
    private Node theNode;
    private String nodeHost;
    private String nodeDir;
    private String installDir;
    private String registerInstanceMessage;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        ctx = context;
        logger = context.getLogger();

        if (!env.isDas()) {
            String msg = Strings.get("notAllowed");
            logger.warning(msg);
            report.setActionExitCode(FAILURE);
            report.setMessage(msg);
            return;
        }

        // Make sure Node is valid
        theNode = nodes.getNode(node);
        if (theNode == null) {
            String msg = Strings.get("noSuchNode", node);
            logger.warning(msg);
            report.setActionExitCode(FAILURE);
            report.setMessage(msg);
            return;
        }

        if (lbEnabled != null && clusterName == null) {
            String msg = Strings.get("lbenabledNotForStandaloneInstance");
            logger.warning(msg);
            report.setActionExitCode(FAILURE);
            report.setMessage(msg);
            return;
        }

        nodeHost = theNode.getNodeHost();
        nodeDir = theNode.getNodeDirAbsolute();
        installDir = theNode.getInstallDir();

        if (autoName) {
            instance = PayaraServerNameGenerator.validateInstanceNameUnique(instance, context);
        } else {
            if (theNode.isLocal()) {
                validateInstanceDirUnique(report, context);
                if (report.getActionExitCode() != SUCCESS && report.getActionExitCode() != WARNING) {
                    // If we couldn't update domain.xml then stop!
                    return;
                }
            }
        }

        // First, update domain.xml by calling _register-instance
        CommandInvocation commandInvocation = commandRunner.getCommandInvocation("_register-instance", report, context.getSubject());
        ParameterMap commandParameters = new ParameterMap();
        commandParameters.add("node", node);
        commandParameters.add("config", configRef);
        commandParameters.add("cluster", clusterName);
        commandParameters.add("deploymentgroup", deploymentGroup);
        if (lbEnabled != null) {
            commandParameters.add("lbenabled", lbEnabled.toString());
        }
        if (!checkPorts) {
            commandParameters.add("checkports", "false");
        }
        if (StringUtils.ok(portBase)) {
            commandParameters.add("portbase", portBase);
        }
        commandParameters.add("systemproperties", systemProperties);
        commandParameters.add("DEFAULT", instance);
        commandInvocation.parameters(commandParameters);
        
        commandInvocation.execute();

        if (report.getActionExitCode() != SUCCESS && report.getActionExitCode() != WARNING) {
            // If we couldn't update domain.xml then stop!
            return;
        }

        registerInstanceMessage = report.getMessage();

        // If nodehost is localhost and installdir is null and config node, update config node
        // so installdir is product root. see register-instance above
        if (theNode.isLocal() && installDir == null && theNode.getType().equals("CONFIG")) {
            commandInvocation = commandRunner.getCommandInvocation("_update-node", report, context.getSubject());
            commandParameters = new ParameterMap();
            commandParameters.add("installdir", "${com.sun.aas.productRoot}");
            commandParameters.add("type", "CONFIG");
            commandParameters.add("DEFAULT", theNode.getName());
            commandInvocation.parameters(commandParameters);
            commandInvocation.execute();

            if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS
                    && report.getActionExitCode() != ActionReport.ExitCode.WARNING) {
                // If we couldn't update domain.xml then stop!
                return;
            }
        }

        if (!validateDasOptions()) {
            report.setActionExitCode(WARNING);
            return;
        }

        if (theNode.getType().equals("DOCKER")) {
            report.appendMessage(
                    "\n\nSuccessfully registered instance with DAS, now attempting to create Docker container...");
            createDockerContainer();
        } else {
            // Then go create the instance filesystem on the node
            createInstanceFilesystem();
        }

        if (extraTerse) {
            report.setMessage(instance);
        }
    }

    private void validateInstanceDirUnique(ActionReport report, AdminCommandContext context) {
        CommandInvocation listInstancesCommand = commandRunner.getCommandInvocation("list-instances", report, context.getSubject());
        ParameterMap commandParameters = new ParameterMap();
        commandParameters.add("whichTarget", theNode.getName());
        listInstancesCommand.parameters(commandParameters);
        listInstancesCommand.execute();
        
        Properties extraProperties = listInstancesCommand.report().getExtraProperties();
        if (extraProperties != null) {
            List<Map<String, String>> instanceList = (List<Map<String, String>>) extraProperties.get("instanceList");
            if (instanceList == null) {
                return;
            }
            
            for (Map<String, String> instanceMap : instanceList) {
                File nodeDirFile = nodeDir != null ? new File(nodeDir) : defaultLocalNodeDirFile();
                File instanceDir = new File(new File(nodeDirFile.toString(), theNode.getName()), instance);
                String instanceName = instanceMap.get("name");
                File instanceListDir = new File(new File(nodeDirFile.toString(), theNode.getName()), instance);
                
                if (instance.equalsIgnoreCase(instanceName) && instanceDir.equals(instanceListDir)) {
                    String msg = Strings.get("Instance.duplicateInstanceDir", instance, instanceName);
                    logger.warning(msg);
                    report.setActionExitCode(FAILURE);
                    report.setMessage(msg);
                    
                    return;
                }
            }
        }
    }

    /**
     * Returns the directory for the selected instance that is on the local system.
     *
     * @return File for the local file system location of the instance directory
     * @throws IOException
     */
    private File getLocalInstanceDir() throws IOException {
        /*
         * Pass the node directory parent and the node directory name explicitly or else InstanceDirs will not work as we want
         * if there are multiple nodes registered on this node.
         *
         * If the configuration recorded an explicit directory for the node, then use it. Otherwise, use the default node
         * directory of ${installDir}/glassfish/nodes/${nodeName}.
         */
        File nodeDirFile = nodeDir != null ? new File(nodeDir) : defaultLocalNodeDirFile();
        
        return new InstanceDirs(nodeDirFile.toString(), theNode.getName(), instance).getInstanceDir();
    }

    private File defaultLocalNodeDirFile() {
        /*
         * The default "nodes" directory we want to use has been set in asenv.conf named as AS_DEF_NODES_PATH
         */
        return new File(new ASenvPropertyReader().getProps().get(AGENT_ROOT_PROPERTY));

    }

    private File getDomainInstanceDir() {
        return env.getInstanceRoot();
    }

    /**
     *
     * Delivers bootstrap files for secure admin locally, because the instance is on the same system as the DAS (and
     * therefore on the same system where this command is running).
     *
     * @return 0 if successful, 1 otherwise
     */
    private int bootstrapSecureAdminLocally() {
        ActionReport report = ctx.getActionReport();

        try {
            SecureAdminBootstrapHelper bootHelper = SecureAdminBootstrapHelper.getLocalHelper(env.getInstanceRoot(), getLocalInstanceDir());
            bootHelper.bootstrapInstance();
            bootHelper.close();
            
            return 0;
        } catch (IOException | SecureAdminBootstrapHelper.BootstrapException ex) {
            return reportFailure(ex, report);
        }
    }

    private int reportFailure(final Exception ex, final ActionReport report) {
        String msg = Strings.get("create.instance.local.boot.failed", instance, node, nodeHost);
        logger.log(SEVERE, msg, ex);
        report.setActionExitCode(FAILURE);
        report.setMessage(msg);
        
        return 1;
    }

    /**
     * Delivers bootstrap files for secure admin remotely, because the instance is NOT on the same system as the DAS.
     *
     * @return 0 if successful; 1 otherwise
     */
    private int bootstrapSecureAdminRemotely() {
        ActionReport report = ctx.getActionReport();
        
        // nodedir is the root of where all the node dirs will be created.
        // add the name of the node as that is where the instance files should be created
        String thisNodeDir = null;
        if (nodeDir != null) {
            thisNodeDir = nodeDir + "/" + node;
        }
        
        try {
            SecureAdminBootstrapHelper bootHelper = SecureAdminBootstrapHelper.getRemoteHelper(serviceLocator, getDomainInstanceDir(), thisNodeDir, instance,
                    theNode, logger);
            bootHelper.bootstrapInstance();
            bootHelper.close();
            
            return 0;
        } catch (Exception ex) {
            String exmsg = ex.getMessage();
            if (exmsg == null) {
                // The root cause message is better than no message at all
                exmsg = ExceptionUtil.getRootCause(ex).toString();
            }
            String msg = Strings.get("create.instance.remote.boot.failed", instance,

                    // DCOMFIX
                    (ex instanceof SecureAdminBootstrapHelper.BootstrapException ? ((SecureAdminBootstrapHelper.BootstrapException) ex).sshSettings() : null),
                    exmsg,

                    nodeHost);
            logger.log(SEVERE, msg, ex);
            report.setActionExitCode(FAILURE);
            report.setMessage(msg);
            
            return 1;
        }
    }

    private void createInstanceFilesystem() {
        ActionReport report = ctx.getActionReport();
        report.setActionExitCode(SUCCESS);

        NodeUtils nodeUtils = new NodeUtils(serviceLocator, logger);
        Server dasServer = servers.getServer(SystemPropertyConstants.DAS_SERVER_NAME);
        String dasHost = dasServer.getAdminHost();
        String dasPort = Integer.toString(dasServer.getAdminPort());

        ArrayList<String> command = new ArrayList<String>();
        String humanCommand = null;

        if (!theNode.isLocal()) {
            // Only specify the DAS host if the node is remote. See issue 13993
            command.add("--host");
            command.add(dasHost);
        }

        command.add("--port");
        command.add(dasPort);

        command.add("_create-instance-filesystem");

        if (nodeDir != null) {
            command.add("--nodedir");
            command.add(StringUtils.quotePathIfNecessary(nodeDir));
        }

        command.add("--node");
        command.add(node);

        command.add(instance);

        humanCommand = makeCommandHuman(command);
        if (userManagedNodeType()) {
            report.setMessage(
                StringUtils.cat(NEWLINE, registerInstanceMessage, Strings.get("create.instance.config", instance, humanCommand)));
            
            return;
        }

        // First error message displayed if we fail
        StringBuilder output = new StringBuilder();

        // Run the command on the node and handle errors.
        nodeUtils.runAdminCommandOnNode(
            theNode, command, ctx, Strings.get("create.instance.filesystem.failed", instance, node, nodeHost), humanCommand, output);

        if (report.getActionExitCode() != SUCCESS) {
            // Something went wrong with the nonlocal command don't continue but set status to warning
            // because config was updated correctly or we would not be here.
            report.setActionExitCode(WARNING);
            
            return;
        }

        // If it was successful say so and display the command output
        String msg = Strings.get("create.instance.success", instance, nodeHost);
        if (!terse) {
            msg = StringUtils.cat(NEWLINE, output.toString().trim(), registerInstanceMessage, msg);
        }
        report.setMessage(msg);

        // Bootstrap secure admin files
        if (theNode.isLocal()) {
            bootstrapSecureAdminLocally();
        } else {
            bootstrapSecureAdminRemotely();
        }
        
        if (report.getActionExitCode() != SUCCESS) {

            // something went wrong with the nonlocal command don't continue but set status to warning
            // because config was updated correctly or we would not be here.
            report.setActionExitCode(WARNING);
        }
    }

    private void createDockerContainer() {
        ActionReport actionReport = ctx.getActionReport();
        ActionReport subActionReport = actionReport.addSubActionsReport();

        ParameterMap parameterMap = new ParameterMap();

        parameterMap.add("node", theNode.getName());
        parameterMap.add("DEFAULT", instance);

        commandRunner.getCommandInvocation("_create-docker-container", subActionReport, ctx.getSubject())
                .parameters(parameterMap)
                .execute();

        if (subActionReport.getActionExitCode() != SUCCESS) {
            // Something went wrong with one of the sub-commands, so let's make sure this top level command fails as well
            actionReport.setActionExitCode(FAILURE);
        }
    }

    /**
     * This ensures we don't step on another domain's node files on a remote instance. See bug GLASSFISH-14985.
     */
    private boolean validateDasOptions() {
        boolean isDasOptionsValid = true;
        
        if (theNode.isLocal() || (!theNode.isLocal() && theNode.getType().equals("SSH"))) {
            ActionReport report = ctx.getActionReport();
            report.setActionExitCode(SUCCESS);

            NodeUtils nodeUtils = new NodeUtils(serviceLocator, logger);
            Server dasServer = servers.getServer(SystemPropertyConstants.DAS_SERVER_NAME);
            String dasHost = dasServer.getAdminHost();
            String dasPort = Integer.toString(dasServer.getAdminPort());

            ArrayList<String> command = new ArrayList<>();

            if (!theNode.isLocal()) {
                // Only specify the DAS host if the node is remote. See issue 13993
                command.add("--host");
                command.add(dasHost);
            }

            command.add("--port");
            command.add(dasPort);

            command.add("_validate-das-options");

            if (nodeDir != null) {
                command.add("--nodedir");
                command.add(nodeDir); // XXX escape spaces?
            }

            command.add("--node");
            command.add(node);

            command.add(instance);

            // Run the command on the node
            nodeUtils.runAdminCommandOnNode(theNode, command, ctx, "", null, null);

            if (report.getActionExitCode() != SUCCESS) {
                report.setActionExitCode(FAILURE);
                isDasOptionsValid = false;
            }
        }
        
        return isDasOptionsValid;
    }

    private String makeCommandHuman(List<String> commands) {
        StringBuilder fullCommand = new StringBuilder();

        fullCommand.append("lib");
        fullCommand.append(System.getProperty("file.separator"));
        fullCommand.append("nadmin ");

        for (String command : commands) {
            if (command.equals("_create-instance-filesystem")) {
                // We tell the user to run create-local-instance, not the
                // hidden command
                fullCommand.append(" ");
                fullCommand.append("create-local-instance");
            } else {
                fullCommand.append(" ");
                fullCommand.append(command);
            }
        }

        return fullCommand.toString();
    }

    // Verbose but very readable...
    private boolean userManagedNodeType() {
        if (theNode.isLocal()) {
            return false;
        }

        if (theNode.getType().equals("SSH")) {
            return false;
        }

        if (theNode.getType().equals("DCOM")) {
            return false;
        }

        return true;
    }
}
