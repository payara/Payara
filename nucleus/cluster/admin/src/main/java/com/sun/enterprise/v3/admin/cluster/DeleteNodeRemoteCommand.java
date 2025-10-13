/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.v3.admin.cluster;

import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_INSTALLDIR;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_NODEHOST;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_SSHPASSWORD;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_REMOTEPORT;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_REMOTEUSER;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_SSHKEYFILE;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_SSHKEYPASSPHRASE;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_WINDOWS_DOMAIN;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.INFO;
import static org.glassfish.api.ActionReport.ExitCode.FAILURE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.SshAuth;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;

/**
 * Remote AdminCommand to delete a config node. This command is run only on DAS.
 *
 * @author Carla Mott
 */
public abstract class DeleteNodeRemoteCommand implements AdminCommand {
    private static final int DEFAULT_TIMEOUT_MSEC = 300000; // 5 minutes
    private static final String NL = System.getProperty("line.separator");

    @Param(name = "name", primary = true)
    String name;

    @Param(optional = true, defaultValue = "false")
    boolean uninstall;

    @Param(optional = true, defaultValue = "false")
    boolean force;

    @Inject
    protected ServiceLocator serviceLocator;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    IterableProvider<Node> nodeList;

    @Inject
    Nodes nodes;

    protected String remotepassword;
    protected String sshkeypassphrase;
    protected Logger logger;

    protected abstract List<String> getPasswords();

    protected abstract String getUninstallCommandName();

    protected abstract void setTypeSpecificOperands(List<String> command, ParameterMap map);

    protected final void executeInternal(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        logger = context.getLogger();
        Node node = nodes.getNode(name);

        if (node == null) {
            // No node to delete nothing to do here
            String msg = Strings.get("noSuchNode", name);
            logger.warning(msg);
            report.setActionExitCode(FAILURE);
            report.setMessage(msg);
            return;
        }

        String type = node.getType();
        if (type == null || type.equals("CONFIG")) {
            // No node to delete nothing to do here
            String msg = Strings.get("notRemoteNodeType", name);
            logger.warning(msg);
            report.setActionExitCode(FAILURE);
            report.setMessage(msg);
            return;
        }

        ParameterMap info = new ParameterMap();

        if (uninstall) {
            // Store needed info for uninstall
            SshConnector sshC = node.getSshConnector();
            SshAuth sshAuth = sshC.getSshAuth();

            if (sshAuth.getPassword() != null) {
                info.add(PARAM_SSHPASSWORD, sshAuth.getPassword());
            }

            if (sshAuth.getKeyPassphrase() != null) {
                info.add(PARAM_SSHKEYPASSPHRASE, sshAuth.getKeyPassphrase());
            }

            if (sshAuth.getKeyfile() != null) {
                info.add(PARAM_SSHKEYFILE, sshAuth.getKeyfile());
            }

            info.add(PARAM_INSTALLDIR, node.getInstallDir());
            info.add(PARAM_REMOTEPORT, sshC.getSshPort());
            info.add(PARAM_REMOTEUSER, sshAuth.getUserName());
            info.add(PARAM_NODEHOST, node.getNodeHost());
            info.add(PARAM_WINDOWS_DOMAIN, node.getWindowsDomain());
        }

        CommandInvocation commandInvocation = commandRunner.getCommandInvocation("_delete-node", report, context.getSubject());
        ParameterMap commandParameters = new ParameterMap();
        commandParameters.add("DEFAULT", name);
        commandInvocation.parameters(commandParameters);

        commandInvocation.execute();

        // Uninstall Payara after deleting the node
        if (uninstall) {
            if (!uninstallNode(context, info, node) && !force) {
                report.setActionExitCode(FAILURE);
                return;
            }
        }
    }

    /**
     * Prepares for invoking uninstall-node on DAS
     *
     * @param ctx command context
     * @return true if uninstall-node succeeds, false otherwise
     */
    private boolean uninstallNode(AdminCommandContext ctx, ParameterMap map, Node node) {
        boolean res = false;

        remotepassword = map.getOne(PARAM_SSHPASSWORD);
        sshkeypassphrase = map.getOne(PARAM_SSHKEYPASSPHRASE);

        ArrayList<String> command = new ArrayList<>();

        command.add(getUninstallCommandName());
        command.add("--installdir");
        command.add(map.getOne(PARAM_INSTALLDIR));

        if (force) {
            command.add("--force");
        }

        setTypeSpecificOperands(command, map);
        String host = map.getOne(PARAM_NODEHOST);
        command.add(host);

        String firstErrorMessage = Strings.get("delete.node.ssh.uninstall.failed", node.getName(), host);
        StringBuilder out = new StringBuilder();
        int exitCode = execCommand(command, out);

        // capture the output in server.log
        logger.info(out.toString().trim());

        ActionReport report = ctx.getActionReport();
        if (exitCode == 0) {
            // If it was successful say so and display the command output
            report.setMessage(Strings.get("delete.node.ssh.uninstall.success", host));
            res = true;
        } else {
            report.setMessage(firstErrorMessage);
        }

        return res;
    }

    /**
     * Invokes install-node using ProcessManager and returns the exit message/status.
     *
     * @param cmdLine list of args
     * @param output contains output message
     * @return exit status of uninstall-node
     */
    private int execCommand(List<String> cmdLine, StringBuilder output) {
        int exit = -1;

        List<String> fullcommand = new ArrayList<>();
        String installDir = nodes.getDefaultLocalNode().getInstallDirUnixStyle() + "/glassfish";
        if (!StringUtils.ok(installDir)) {
            throw new IllegalArgumentException(Strings.get("create.node.ssh.no.installdir"));
        }

        File asadmin = new File(SystemPropertyConstants.getAsAdminScriptLocation(installDir));
        fullcommand.add(asadmin.getAbsolutePath());

        // if password auth is used for deleting the node, use the same auth mechanism for
        // uinstall-node as well. The passwords are passed directly through input stream
        List<String> passwords = new ArrayList<>();
        if (remotepassword != null) {
            fullcommand.add("--passwordfile");
            fullcommand.add("-");
            passwords = getPasswords();
        }

        fullcommand.add("--interactive=false");
        fullcommand.addAll(cmdLine);

        ProcessManager processManager = new ProcessManager(fullcommand);
        if (!passwords.isEmpty()) {
            processManager.setStdinLines(passwords);
        }

        if (logger.isLoggable(INFO)) {
            logger.info("Running command on DAS: " + commandListToString(fullcommand));
        }

        processManager.setTimeoutMsec(DEFAULT_TIMEOUT_MSEC);

        if (logger.isLoggable(FINER)) {
            processManager.setEcho(true);
        } else {
            processManager.setEcho(false);
        }

        try {
            exit = processManager.execute();
        } catch (ProcessManagerException ex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Error while executing command: " + ex.getMessage());
            }
            exit = 1;
        }

        String stdout = processManager.getStdout();
        String stderr = processManager.getStderr();

        if (output != null) {
            if (StringUtils.ok(stdout)) {
                output.append(stdout);
            }

            if (StringUtils.ok(stderr)) {
                if (output.length() > 0) {
                    output.append(NL);
                }
                output.append(stderr);
            }
        }
        return exit;
    }

    private String commandListToString(List<String> command) {
        StringBuilder fullCommand = new StringBuilder();

        for (String commandPart : command) {
            fullCommand.append(" ");
            fullCommand.append(commandPart);
        }

        return fullCommand.toString();
    }
}
