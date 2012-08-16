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
package org.glassfish.cluster.ssh.connect;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.common.util.admin.AsadminInput;
import org.glassfish.api.admin.SSHCommandExecutionException;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.connect.NodeRunnerSsh;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.hk2.api.ServiceLocator;

public class NodeRunner {
    private static final String NL = System.getProperty("line.separator");
    private static final String AUTH_TOKEN_STDIN_LINE_PREFIX = "option." + AuthTokenManager.AUTH_TOKEN_OPTION_NAME + "=";
    private ServiceLocator habitat;
    private Logger logger;
    private String lastCommandRun = null;
    private final AuthTokenManager authTokenManager;

    public NodeRunner(ServiceLocator habitat, Logger logger) {
        this.logger = logger;
        this.habitat = habitat;
        authTokenManager = habitat.getService(AuthTokenManager.class);
    }

    public String getLastCommandRun() {
        return lastCommandRun;
    }

    public boolean isSshNode(Node node) {

        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }
        if (node.getType() == null)
            return false;
        return node.getType().equals("SSH");
    }

    public boolean isDcomNode(Node node) {

        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }
        if (node.getType() == null)
            return false;
        return node.getType().equals("DCOM");
    }

    /**
     * Run an asadmin command on a Node. The node may be local or remote. If
     * it is remote then SSH is used to execute the command on the node.
     * The args list is all parameters passed to "asadmin", but not
     * "asadmin" itself. So an example args is:
     *
     * "--host", "mydashost.com", "start-local-instance", "--node", "n1", "i1"
     *
     * @param node  The node to run the asadmin command on
     * @param output    A StringBuilder to hold the command's output in. Both
     *                  stdout and stderr are placed in output. null if you
     *                  don't want the output.
     * @param args  The arguments to the asadmin command. This includes
     *              parameters for asadmin (like --host) as well as the
     *              command (like start-local-instance) as well as an
     *              parameters for the command. It does not include the
     *              string "asadmin" itself.
     * @return      The status of the asadmin command. Typically 0 if the
     *              command was successful else 1.
     *
     * @throws SSHCommandExecutionException There was an error executing the
     *                                      command via SSH.
     * @throws ProcessManagerException      There was an error executing the
     *                                      command locally.
     * @throws UnsupportedOperationException The command needs to be run on
     *                                       a remote node, but the node is not
     *                                       of type SSH.
     * @throws IllegalArgumentException     The passed node is malformed.
     */
    public int runAdminCommandOnNode(Node node, StringBuilder output,
            List<String> args,
            AdminCommandContext context) throws
            SSHCommandExecutionException,
            ProcessManagerException,
            UnsupportedOperationException,
            IllegalArgumentException {

        return runAdminCommandOnNode(node, output, false, args, context);
    }

    public int runAdminCommandOnNode(Node node, StringBuilder output,
            boolean waitForReaderThreads,
            List<String> args,
            AdminCommandContext context) throws
            SSHCommandExecutionException,
            ProcessManagerException,
            UnsupportedOperationException,
            IllegalArgumentException {


        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }

        final List<String> stdinLines = new ArrayList<String>();
        stdinLines.add(AsadminInput.versionSpecifier());
        stdinLines.add(AUTH_TOKEN_STDIN_LINE_PREFIX + authTokenManager.createToken(context.getSubject()));
        args.add(0, "--interactive=false");            // No prompting!

        if (node.isLocal()) {
            return runAdminCommandOnLocalNode(node, output, waitForReaderThreads,
                    args, stdinLines);
        }
        else {
            return runAdminCommandOnRemoteNode(node, output, args, stdinLines);
        }
    }

    private int runAdminCommandOnLocalNode(Node node, StringBuilder output,
            boolean waitForReaderThreads,
            List<String> args,
            List<String> stdinLines) throws
            ProcessManagerException {
        args.add(0, AsadminInput.CLI_INPUT_OPTION);
        args.add(1, AsadminInput.SYSTEM_IN_INDICATOR); // specified to read from System.in
        List<String> fullcommand = new ArrayList<String>();
        String installDir = node.getInstallDirUnixStyle() + "/"
                + SystemPropertyConstants.getComponentName();
        if (!StringUtils.ok(installDir)) {
            throw new IllegalArgumentException("Node does not have an installDir");
        }

        File asadmin = new File(SystemPropertyConstants.getAsAdminScriptLocation(installDir));
        fullcommand.add(asadmin.getAbsolutePath());
        fullcommand.addAll(args);

        if (!asadmin.canExecute())
            throw new ProcessManagerException(asadmin.getAbsolutePath() + " is not executable.");

        lastCommandRun = commandListToString(fullcommand);

        trace("Running command locally: " + lastCommandRun);
        ProcessManager pm = new ProcessManager(fullcommand);
        pm.setStdinLines(stdinLines);

        // XXX should not need this after fix for 12777, but we seem to
        pm.waitForReaderThreads(waitForReaderThreads);
        pm.execute();  // blocks until command is complete

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
        return pm.getExitValue();
    }

    private int runAdminCommandOnRemoteNode(Node node, StringBuilder output,
            List<String> args,
            List<String> stdinLines) throws
            SSHCommandExecutionException, IllegalArgumentException,
            UnsupportedOperationException {

        // don't want to call a config object proxy more than absolutely necessary!
        String type = node.getType();

        if ("SSH".equals(type)) {
            NodeRunnerSsh nrs = new NodeRunnerSsh(habitat, logger);
            int result = nrs.runAdminCommandOnRemoteNode(node, output, args, stdinLines);
            lastCommandRun = nrs.getLastCommandRun();
            return result;
        }

        if ("DCOM".equals(type)) {
            NodeRunnerDcom nrd = new NodeRunnerDcom(logger);
            nrd.runAdminCommandOnRemoteNode(node, output, args, stdinLines);
            return determineStatus(args, output);
        }

        throw new UnsupportedOperationException("Node is not of type SSH or DCOM");
    }

    private void trace(String s) {
        logger.fine(String.format("%s: %s", this.getClass().getSimpleName(), s));
    }

    private String commandListToString(List<String> command) {
        StringBuilder fullCommand = new StringBuilder();

        for (String s : command) {
            fullCommand.append(" ");
            fullCommand.append(s);
        }

        return fullCommand.toString();
    }

    /* hack TODO do not know how to get int status back from Windows
     */
    private int determineStatus(List<String> args, StringBuilder output) {
        if (isDeleteFS(args) && output.toString().indexOf("UTIL6046") >= 0)
            return 1;
        return 0;
    }

    private boolean isDeleteFS(List<String> args) {
        for (String arg : args) {
            if ("_delete-instance-filesystem".equals(arg))
                return true;
        }
        return false;
    }
}
