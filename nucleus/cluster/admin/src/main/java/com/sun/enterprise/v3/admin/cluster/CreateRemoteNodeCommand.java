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

import com.sun.enterprise.util.cluster.RemoteType;
import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.*;
import java.util.logging.*;
import java.io.*;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;

/**
 * Refactored from CreateNodeSshCommand.java on 9/10/11
 * Note the use of "protected" visibility is one of those rare times when it is actually
 * necessary.  This class is sub-classed in a different package so protected is needed...
 * @author Carla Mott
 * @author Byron Nevins
 */
public abstract class CreateRemoteNodeCommand implements AdminCommand {
    static final int DEFAULT_TIMEOUT_MSEC = 300000; // 5 minutes
    @Inject
    private CommandRunner cr;
    @Inject
    ServiceLocator habitat;
    @Inject
    Nodes nodes;
    @Param(name = "name", primary = true)
    private String name;
    @Param(name = "nodehost")
    protected String nodehost;
    @Param(name = "installdir", optional = true, defaultValue = NodeUtils.NODE_DEFAULT_INSTALLDIR)
    private String installdir;
    @Param(name = "nodedir", optional = true)
    private String nodedir;
    @Param(name = "force", optional = true, defaultValue = "false")
    private boolean force;
    @Param(optional = true, defaultValue = "false")
    boolean install;
    @Param(optional = true)
    String archive;
    static final String NL = System.lineSeparator();
    @SuppressWarnings("NonConstantLogger")
    Logger logger = null;
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
        ParameterMap map = new ParameterMap();
        populateParametersInternal(map);
        try {
            validate();
        }
        catch (CommandValidationException ex) {
            report.setMessage(ex.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            nodeUtils = new NodeUtils(habitat, logger);
            nodeUtils.validate(map);
            if (install) {
                boolean s = installNode(context);
                if (!s && !force) {
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }
        catch (CommandValidationException e) {
            String m1 = Strings.get("node.ssh.invalid.params");
            if (!force) {
                String m2 = Strings.get("create.node.ssh.or.dcom.not.created", getType().toString());
                msg.append(StringUtils.cat(NL, m1, m2, e.getMessage()));
                report.setMessage(msg.toString());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            else {
                String m2 = Strings.get("create.node.ssh.continue.force");
                msg.append(StringUtils.cat(NL, m1, e.getMessage(), m2));
            }
        }

        map.remove(NodeUtils.PARAM_INSTALL);
        CommandInvocation ci = cr.getCommandInvocation("_create-node", report, context.getSubject());
        ci.parameters(map);
        ci.execute();

        NodeUtils.sanitizeReport(report);

        if (StringUtils.ok(report.getMessage())) {
            if (msg.length() > 0) {
                msg.append(NL);
            }
            msg.append(report.getMessage());
        }

        report.setMessage(msg.toString());
    }

    private void populateParametersInternal(ParameterMap map) {
        map.add("DEFAULT", name);
        map.add(NodeUtils.PARAM_INSTALLDIR, installdir);
        map.add(NodeUtils.PARAM_NODEHOST, nodehost);
        map.add(NodeUtils.PARAM_NODEDIR, nodedir);
        map.add(NodeUtils.PARAM_REMOTEPORT, remotePort);
        map.add(NodeUtils.PARAM_REMOTEUSER, remoteUser);
        map.add(NodeUtils.PARAM_REMOTEPASSWORD, remotePassword);
        map.add(NodeUtils.PARAM_TYPE, getType().toString());
        map.add(NodeUtils.PARAM_INSTALL, Boolean.toString(install));

        // let subclasses overwrite our values if they like.
        populateParameters(map);
    }

    /**
     * Prepares for invoking install-node on DAS
     * @param ctx command context
     * @return true if install-node succeeds, false otherwise
     */
    private boolean installNode(AdminCommandContext ctx) throws CommandValidationException {
        boolean res = false;
        ArrayList<String> command = new ArrayList<String>();
        command.add(getInstallNodeCommandName());
        command.add("--installdir");
        command.add(installdir);

        if (force) {
            command.add("--force");
        }

        if (archive != null) {
            File ar = new File(archive);
            if (ar.exists() && ar.canRead()) {
                command.add("--archive");
                command.add(archive);
            }
        }

        populateCommandArgs(command);
        command.add(nodehost);

        StringBuilder out = new StringBuilder();
        int exitCode = execCommand(command, out);

        //capture the output in server.log
        logger.info(out.toString().trim());

        ActionReport report = ctx.getActionReport();
        if (exitCode == 0) {
            // If it was successful say so and display the command output
            String msg = Strings.get("create.node.ssh.install.success", nodehost);
            report.setMessage(msg);
            res = true;
        }
        else {
            report.setMessage(out.toString().trim());
        }
        return res;
    }

    /**
     * Sometimes the console passes an empty string for a parameter. This
     * makes sure those are defaulted correctly.
     */
    protected void checkDefaults() {
        if (!StringUtils.ok(installdir)) {
            installdir = NodeUtils.NODE_DEFAULT_INSTALLDIR;
        }
        if (!StringUtils.ok(remoteUser)) {
            remoteUser = NodeUtils.NODE_DEFAULT_REMOTE_USER;
        }
    }

    /**
     * Invokes install-node using ProcessManager and returns the exit message/status.
     * @param cmdLine list of args
     * @param output contains output message
     * @return exit status of install-node
     *
     * This method was copied over from CreateNodeSshCommand on 9/14/11
     */
    final int execCommand(List<String> cmdLine, StringBuilder output) {
        int exit = -1;
        List<String> fullcommand = new ArrayList<String>();
        String installDir = nodes.getDefaultLocalNode().getInstallDirUnixStyle() + "/glassfish";

        if (!StringUtils.ok(installDir)) {
            throw new IllegalArgumentException(Strings.get("create.node.ssh.no.installdir"));
        }

        File asadmin = new File(SystemPropertyConstants.getAsAdminScriptLocation(installDir));
        fullcommand.add(asadmin.getAbsolutePath());

        //if password auth is used for creating node, use the same auth mechanism for
        //install-node as well. The passwords are passed directly through input stream
        List<String> pass = new ArrayList<String>();
        if (remotePassword != null) {
            fullcommand.add("--passwordfile");
            fullcommand.add("-");
            pass = getPasswords();
        }

        fullcommand.add("--interactive=false");
        fullcommand.addAll(cmdLine);

        ProcessManager pm = new ProcessManager(fullcommand);
        if (!pass.isEmpty())
            pm.setStdinLines(pass);

        if (logger.isLoggable(Level.INFO)) {
            logger.info("Running command on DAS: " + commandListToString(fullcommand));
        }
        pm.setTimeoutMsec(DEFAULT_TIMEOUT_MSEC);

        if (logger.isLoggable(Level.FINER))
            pm.setEcho(true);
        else
            pm.setEcho(false);

        try {
            exit = pm.execute();
        }
        catch (ProcessManagerException ex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Error while executing command: " + ex.getMessage());
            }
            exit = 1;
        }

        String stdout = pm.getStdout();
        String stderr = pm.getStderr();

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

    final String commandListToString(List<String> command) {
        StringBuilder fullCommand = new StringBuilder();

        for (String s : command) {
            fullCommand.append(" ");
            fullCommand.append(s);
        }

        return fullCommand.toString();
    }
}
