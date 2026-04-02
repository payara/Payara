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
// Portions Copyright 2018-2026 Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import static com.sun.enterprise.v3.admin.cluster.NodeUtils.NODE_DEFAULT_INSTALLDIR;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.NODE_DEFAULT_REMOTE_USER;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_INSTALL;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_INSTALLDIR;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_NODEDIR;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_NODEHOST;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_SSHPASSWORD;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_REMOTEPORT;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_REMOTEUSER;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_TYPE;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.sanitizeReport;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.INFO;
import static org.glassfish.api.ActionReport.ExitCode.FAILURE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.cluster.RemoteType;

/**
 * Refactored from CreateNodeSshCommand.java on 9/10/11 Note the use of "protected" visibility is one of those rare
 * times when it is actually necessary. This class is sub-classed in a different package so protected is needed...
 *
 * @author Carla Mott
 * @author Byron Nevins
 */
public abstract class CreateRemoteNodeCommand implements AdminCommand {
    static final int DEFAULT_TIMEOUT_MSEC = 300000; // 5 minutes
    static final String NL = System.lineSeparator();

    @Param(name = "name", primary = true)
    private String name;

    @Param(name = "nodehost")
    protected String nodehost;

    @Param(name = "installdir", optional = true, defaultValue = NODE_DEFAULT_INSTALLDIR)
    private String installdir;

    @Param(name = "nodedir", optional = true)
    private String nodedir;

    @Param(name = "force", optional = true, defaultValue = "false")
    private boolean force;

    @Param(optional = true, defaultValue = "false")
    boolean install;

    @Param(optional = true)
    String archive;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    Nodes nodes;

    Logger logger;
    NodeUtils nodeUtils;
    protected String remotePort;
    protected String remoteUser;
    protected String remotePassword;

    protected abstract void populateBaseClass();

    protected abstract void initialize();

    protected abstract void populateParameters(ParameterMap pmap);

    protected abstract void populateCommandArgs(List<String> args);

    protected abstract RemoteType getType();

    protected abstract void validate() throws CommandValidationException;

    protected abstract List<String> getPasswords();

    protected abstract String getInstallNodeCommandName();

    public final void executeInternal(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        StringBuilder msg = new StringBuilder();
        logger = context.getLogger();

        initialize();
        populateBaseClass();
        checkDefaults();
        ParameterMap commandParameters = new ParameterMap();
        populateParametersInternal(commandParameters);

        try {
            validate();
        } catch (CommandValidationException ex) {
            report.setMessage(ex.getLocalizedMessage());
            report.setActionExitCode(FAILURE);
            return;
        }

        try {
            nodeUtils = new NodeUtils(serviceLocator, logger);
            nodeUtils.validate(commandParameters);
            if (install) {
                if (!installNode(context) && !force) {
                    report.setActionExitCode(FAILURE);
                    return;
                }
            }
        } catch (CommandValidationException e) {
            String invalidParamsMsg = Strings.get("node.ssh.invalid.params");
            if (!force) {
                msg.append(StringUtils.cat(NL, invalidParamsMsg, Strings.get("create.node.ssh.not.created", getType().toString()), e.getMessage()));
                report.setMessage(msg.toString());
                report.setActionExitCode(FAILURE);
                return;
            } else {
                msg.append(StringUtils.cat(NL, invalidParamsMsg, e.getMessage(), Strings.get("create.node.ssh.continue.force")));
            }
        }

        commandParameters.remove(PARAM_INSTALL);
        CommandInvocation commandInvocation = commandRunner.getCommandInvocation("_create-node", report, context.getSubject());
        commandInvocation.parameters(commandParameters);
        commandInvocation.execute();

        sanitizeReport(report);

        if (StringUtils.ok(report.getMessage())) {
            if (msg.length() > 0) {
                msg.append(NL);
            }
            msg.append(report.getMessage());
        }

        report.setMessage(msg.toString());
    }

    private void populateParametersInternal(ParameterMap parameters) {
        parameters.add("DEFAULT", name);
        parameters.add(PARAM_INSTALLDIR, installdir);
        parameters.add(PARAM_NODEHOST, nodehost);
        parameters.add(PARAM_NODEDIR, nodedir);
        parameters.add(PARAM_REMOTEPORT, remotePort);
        parameters.add(PARAM_REMOTEUSER, remoteUser);
        parameters.add(PARAM_SSHPASSWORD, remotePassword);
        parameters.add(PARAM_TYPE, getType().toString());
        parameters.add(PARAM_INSTALL, Boolean.toString(install));

        // Let subclasses overwrite our values if they like.
        populateParameters(parameters);
    }

    /**
     * Prepares for invoking install-node on DAS
     *
     * @param ctx command context
     * @return true if install-node succeeds, false otherwise
     */
    private boolean installNode(AdminCommandContext ctx) throws CommandValidationException {
        boolean res = false;
        ArrayList<String> command = new ArrayList<>();
        command.add(getInstallNodeCommandName());
        command.add("--installdir");
        command.add(installdir);

        if (force) {
            command.add("--force");
        }

        if (archive != null) {
            File archiveFile = new File(archive);
            if (archiveFile.exists() && archiveFile.canRead()) {
                command.add("--archive");
                command.add(archive);
            }
        }

        populateCommandArgs(command);
        command.add(nodehost);

        StringBuilder out = new StringBuilder();
        int exitCode = execCommand(command, out);

        // capture the output in server.log
        logger.info(out.toString().trim());

        ActionReport report = ctx.getActionReport();
        if (exitCode == 0) {
            // If it was successful say so and display the command output
            report.setMessage(Strings.get("create.node.ssh.install.success", nodehost));
            res = true;
        } else {
            report.setMessage(out.toString().trim());
        }

        return res;
    }

    /**
     * Sometimes the console passes an empty string for a parameter. This makes sure those are defaulted correctly.
     */
    protected void checkDefaults() {
        if (!StringUtils.ok(installdir)) {
            installdir = NODE_DEFAULT_INSTALLDIR;
        }

        if (!StringUtils.ok(remoteUser)) {
            remoteUser = NODE_DEFAULT_REMOTE_USER;
        }
    }

    /**
     * Invokes install-node using ProcessManager and returns the exit message/status.
     *
     * @param cmdLine list of args
     * @param output contains output message
     * @return exit status of install-node
     *
     * This method was copied over from CreateNodeSshCommand on 9/14/11
     */
    final int execCommand(List<String> cmdLine, StringBuilder output) {
        int exit = -1;
        List<String> fullcommand = new ArrayList<>();
        String installDir = nodes.getDefaultLocalNode().getInstallDirUnixStyle() + "/glassfish";

        if (!StringUtils.ok(installDir)) {
            throw new IllegalArgumentException(Strings.get("create.node.ssh.no.installdir"));
        }

        File asadmin = new File(SystemPropertyConstants.getAsAdminScriptLocation(installDir));
        fullcommand.add(asadmin.getAbsolutePath());

        // if password auth is used for creating node, use the same auth mechanism for
        // install-node as well. The passwords are passed directly through input stream
        List<String> passwords = new ArrayList<>();
        if (remotePassword != null) {
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
            if (logger.isLoggable(FINE)) {
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

    final String commandListToString(List<String> commands) {
        StringBuilder fullCommand = new StringBuilder();

        for (String command : commands) {
            fullCommand.append(" ");
            fullCommand.append(command);
        }

        return fullCommand.toString();
    }
}
