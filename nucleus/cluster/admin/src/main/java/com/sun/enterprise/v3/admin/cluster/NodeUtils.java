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
import org.glassfish.cluster.ssh.util.DcomUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.RelativePathResolver;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.config.serverbeans.SshAuth;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.ExceptionUtil;
import com.sun.enterprise.util.net.NetUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.cluster.windows.process.WindowsCredentials;
import com.sun.enterprise.util.cluster.windows.process.WindowsRemotePinger;
import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFile;
import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFileSystem;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.glassfish.cluster.ssh.connect.NodeRunner;

/**
 * Utility methods for operating on Nodes
 *
 * @author Joe Di Pol
 * @author Byron Nevins
 */
public class NodeUtils {
    public static final String NODE_DEFAULT_SSH_PORT = "22";
    public static final String NODE_DEFAULT_DCOM_PORT = "135";
    public static final String NODE_DEFAULT_REMOTE_USER = "${user.name}";
    static final String NODE_DEFAULT_INSTALLDIR =
            "${com.sun.aas.productRoot}";
    // Command line option parameter names
    static final String PARAM_NODEHOST = "nodehost";
    static final String PARAM_INSTALLDIR = "installdir";
    static final String PARAM_NODEDIR = "nodedir";
    static final String PARAM_REMOTEPORT = "sshport";
    public static final String PARAM_REMOTEUSER = "sshuser";
    static final String PARAM_SSHKEYFILE = "sshkeyfile";
    static final String PARAM_REMOTEPASSWORD = "sshpassword";
    static final String PARAM_SSHKEYPASSPHRASE = "sshkeypassphrase";
    static final String PARAM_WINDOWSDOMAINNAME = "windowsdomain";
    static final String PARAM_TYPE = "type";
    static final String PARAM_INSTALL = "install";
    public static final String PARAM_WINDOWS_DOMAIN = "windowsdomain";
    static final String LANDMARK_FILE = "glassfish/modules/admin-cli.jar";
    private static final String NL = System.getProperty("line.separator");
    private TokenResolver resolver = null;
    private Logger logger = null;
    private ServiceLocator habitat = null;
    SSHLauncher sshL = null;

    NodeUtils(ServiceLocator habitat, Logger logger) {
        this.logger = logger;
        this.habitat = habitat;

        // Create a resolver that can replace system properties in strings
        Map<String, String> systemPropsMap =
                new HashMap<String, String>((Map) (System.getProperties()));
        resolver = new TokenResolver(systemPropsMap);
        sshL = habitat.getService(SSHLauncher.class);
    }

    static boolean isSSHNode(Node node) {
        if (node == null)
            return false;
        return node.getType().equals("SSH");
    }

    public static boolean isDcomNode(Node node) {
        if (node == null)
            return false;
        return node.getType().equals("DCOM");
    }

    /**
     * Get the version string from a glassfish installation on the node.
     * @param node
     * @return version string
     */
    String getGlassFishVersionOnNode(Node node, AdminCommandContext context) throws CommandValidationException {
        if (node == null)
            return "";

        List<String> command = new ArrayList<String>();
        command.add("version");
        command.add("--local");
        command.add("--terse");
        NodeRunner nr = new NodeRunner(habitat, logger);

        StringBuilder output = new StringBuilder();
        try {
            int commandStatus = nr.runAdminCommandOnNode(node, output, command, context);
            if (commandStatus != 0) {
                return "unknown version: " + output.toString();
            }
        }
        catch (Exception e) {
            throw new CommandValidationException(
                    Strings.get("failed.to.run", command.toString(),
                    node.getNodeHost()), e);
        }
        return output.toString().trim();
    }

    void validate(Node node) throws CommandValidationException {

        // Put node values into parameter map and validate
        ParameterMap map = new ParameterMap();
        map.add("DEFAULT", node.getName());
        map.add(NodeUtils.PARAM_INSTALLDIR, node.getInstallDir());
        map.add(NodeUtils.PARAM_NODEHOST, node.getNodeHost());
        map.add(NodeUtils.PARAM_NODEDIR, node.getNodeDirAbsolute());

        SshConnector sshc = node.getSshConnector();
        if (sshc != null) {
            map.add(NodeUtils.PARAM_REMOTEPORT, sshc.getSshPort());
            SshAuth ssha = sshc.getSshAuth();
            map.add(NodeUtils.PARAM_REMOTEUSER, ssha.getUserName());
            map.add(NodeUtils.PARAM_SSHKEYFILE, ssha.getKeyfile());
            map.add(NodeUtils.PARAM_REMOTEPASSWORD, ssha.getPassword());
            map.add(NodeUtils.PARAM_SSHKEYPASSPHRASE, ssha.getKeyPassphrase());
            map.add(NodeUtils.PARAM_TYPE, node.getType());
        }

        validate(map);
    }

    /**
     * Validate all the parameters used to create a remote node
     * @param map   Map with all parameters used to create a remote node.
     *              The map values can contain system property tokens.
     * @throws CommandValidationException
     */
    void validate(ParameterMap map) throws CommandValidationException {

        validatePassword(map.getOne(PARAM_REMOTEPASSWORD));
        String nodehost = map.getOne(PARAM_NODEHOST);
        validateHostName(nodehost);
        validateRemote(map, nodehost);
    }

    private void validateRemote(ParameterMap map, String nodehost) throws
            CommandValidationException {

        // guaranteed to either get a valid type -- or a CommandValidationException
        RemoteType type = parseType(map);

        if (type == RemoteType.SSH) {
            validateSsh(map);
        }

        // bn: shouldn't this be something more sophisticated than just the standard string?!?
        // i.e. check to see if the hostname is this machine?
        // todo
        if (nodehost.equals("localhost")) {
            return;
        }

        // BN says: Shouldn't this be a fatal error?!?  TODO
        if (sshL == null)
            return;

        validateRemoteConnection(map);
    }

    /**
     * Validate all the parameters used to create an ssh node
     * @param map   Map with all parameters used to create an ssh node.
     *              The map values can contain system property tokens.
     * @throws CommandValidationException
     */
    private void validateSsh(ParameterMap map) throws CommandValidationException {

        String sshkeyfile = map.getOne(PARAM_SSHKEYFILE);
        if (StringUtils.ok(sshkeyfile)) {
            // User specified a key file. Make sure we get use it
            File kfile = new File(resolver.resolve(sshkeyfile));
            if (!kfile.isAbsolute()) {
                throw new CommandValidationException(
                        Strings.get("key.path.not.absolute",
                        kfile.getPath()));
            }
            if (!kfile.exists()) {
                throw new CommandValidationException(
                        Strings.get("key.path.not.found",
                        kfile.getPath()));
            }
            if (!kfile.canRead()) {
                throw new CommandValidationException(
                        Strings.get("key.path.not.readable",
                        kfile.getPath(), System.getProperty("user.name")));
            }
        }
    }

    void validateHostName(String hostName)
            throws CommandValidationException {

        if (!StringUtils.ok(hostName)) {
            throw new CommandValidationException(
                    Strings.get("nodehost.required"));
        }
        try {
            // Check if hostName is valid by looking up it's address
            InetAddress.getByName(hostName);
        }
        catch (UnknownHostException e) {
            throw new CommandValidationException(
                    Strings.get("unknown.host", hostName),
                    e);
        }
    }

    private String resolvePassword(String p) {
        try {
            return RelativePathResolver.getRealPasswordFromAlias(p);
        }
        catch (Exception e) {
            return p;
        }
    }

    private void validatePassword(String p) throws CommandValidationException {

        String expandedPassword = null;

        // Make sure if a password alias is used we can expand it
        if (StringUtils.ok(p)) {
            try {
                expandedPassword = RelativePathResolver.getRealPasswordFromAlias(p);
            }
            catch (IllegalArgumentException e) {
                throw new CommandValidationException(
                        Strings.get("no.such.password.alias", p));
            }
            catch (Exception e) {
                throw new CommandValidationException(
                        Strings.get("no.such.password.alias", p),
                        e);
            }

            if (expandedPassword == null) {
                throw new CommandValidationException(
                        Strings.get("no.such.password.alias", p));
            }
        }
    }

    /**
     * Make sure we can make an SSH or DCOM connection using an existing node.
     *
     * @param node  Node to connect to
     * @throws CommandValidationException
     */
    void pingRemoteConnection(Node node) throws CommandValidationException {
        RemoteType type = RemoteType.valueOf(node.getType());
        validateHostName(node.getNodeHost());

        switch (type) {
            case SSH:
                pingSSHConnection(node);
                break;
            case DCOM:
                pingDcomConnection(node);
                break;
            default:
                throw new CommandValidationException("Internal Error: unknown type");
        }
    }

    /**
     * Make sure we can make an SSH connection using an existing node.
     *
     * @param node  Node to connect to
     * @throws CommandValidationException
     */
    private void pingSSHConnection(Node node) throws
            CommandValidationException {
        try {
            sshL.init(node, logger);
            sshL.pingConnection();
        }
        catch (Exception e) {
            String m1 = e.getMessage();
            String m2 = "";
            Throwable e2 = e.getCause();
            if (e2 != null) {
                m2 = e2.getMessage();
            }
            String msg = Strings.get("ssh.bad.connect", node.getNodeHost(), "SSH");
            logger.warning(StringUtils.cat(": ", msg, m1, m2,
                    sshL.toString()));
            throw new CommandValidationException(StringUtils.cat(NL,
                    msg, m1, m2));
        }
    }

    /**
     * Make sure we can make a DCOM connection using an existing node.
     * Exception...
     * @param node  Node to connect to
     * @throws CommandValidationException
     */
    private void pingDcomConnection(Node node) throws CommandValidationException {
        try {
            SshConnector connector = node.getSshConnector();
            SshAuth auth = connector.getSshAuth();
            String host = connector.getSshHost();

            if (!StringUtils.ok(host))
                host = node.getNodeHost();

            String username = auth.getUserName();
            String password = resolvePassword(auth.getPassword());
            String installdir = node.getInstallDirUnixStyle();
            String domain = node.getWindowsDomain();

            if (!StringUtils.ok(domain))
                domain = host;

            if (!StringUtils.ok(installdir))
                throw new CommandValidationException(Strings.get("dcom.no.installdir"));

            pingDcomConnection(host, domain, username, password, getInstallRoot(installdir));
        }
        // very complicated catch copied from pingssh above...
        catch (CommandValidationException cve) {
            throw cve;
        }
        catch (Exception e) {
            String m1 = e.getMessage();
            String m2 = "";
            Throwable e2 = e.getCause();
            if (e2 != null) {
                m2 = e2.getMessage();
            }
            String msg = Strings.get("ssh.bad.connect", node.getNodeHost(), "DCOM");
            logger.warning(StringUtils.cat(": ", msg, m1, m2));
            throw new CommandValidationException(StringUtils.cat(NL, msg, m1, m2));
        }
    }

    /**
     * Make sure GF is installed and available.
     *
     * @throws CommandValidationException
     */
    void pingDcomConnection(String host, String domain, String username,
            String password, String installRoot) throws CommandValidationException {
        // don't bother trying to connect if we have no password!

        if (!StringUtils.ok(password))
            throw new CommandValidationException(Strings.get("dcom.nopassword"));

        // resolve password aliases
        password = DcomUtils.resolvePassword(resolver.resolve(password));

        if (NetUtils.isThisHostLocal(host))
            throw new CommandValidationException(Strings.get("dcom.yes.local", host));

        try {
            installRoot = installRoot.replace('/', '\\');
            WindowsRemoteFileSystem wrfs = new WindowsRemoteFileSystem(host, username, password);
            WindowsRemoteFile wrf = new WindowsRemoteFile(wrfs, installRoot);
            WindowsCredentials creds = new WindowsCredentials(host, domain, username, password);

            // also looking for side-effect of Exception getting thrown...
            if (!wrf.exists()) {
                throw new CommandValidationException(Strings.get("dcom.no.remote.install",
                        host, installRoot));
            }

            if (!WindowsRemotePinger.ping(installRoot, creds))
                throw new CommandValidationException(Strings.get("dcom.no.connection", host));
        }
        catch (CommandValidationException cve) {
            throw cve;
        }
        catch (Exception ex) {
            throw new CommandValidationException(ex);
        }
    }

    private void validateRemoteConnection(ParameterMap map) throws
            CommandValidationException {
        // guaranteed to either get a valid type -- or a CommandValidationException
        RemoteType type = parseType(map);

        // just too difficult to refactor now...
        if (type == RemoteType.SSH)
            validateSSHConnection(map);
        else if (type == RemoteType.DCOM)
            validateDcomConnection(map);
    }

    private void validateDcomConnection(ParameterMap map) throws CommandValidationException {
        if (Boolean.parseBoolean(map.getOne(PARAM_INSTALL))) {
            // we don't want to insist that there is an installation - there isn't one probably!!
            return;
        }

        String nodehost = resolver.resolve(map.getOne(PARAM_NODEHOST));
        String installdir = resolver.resolve(map.getOne(PARAM_INSTALLDIR));
        String user = resolver.resolve(map.getOne(PARAM_REMOTEUSER));
        String password = map.getOne(PARAM_REMOTEPASSWORD);
        String domain = nodehost;

        pingDcomConnection(nodehost, domain, user, password, getInstallRoot(installdir));
    }

    private void validateSSHConnection(ParameterMap map) throws
            CommandValidationException {

        String nodehost = map.getOne(PARAM_NODEHOST);
        String installdir = map.getOne(PARAM_INSTALLDIR);
        String sshport = map.getOne(PARAM_REMOTEPORT);
        String sshuser = map.getOne(PARAM_REMOTEUSER);
        String sshkeyfile = map.getOne(PARAM_SSHKEYFILE);
        String sshpassword = map.getOne(PARAM_REMOTEPASSWORD);
        String sshkeypassphrase = map.getOne(PARAM_SSHKEYPASSPHRASE);
        boolean installFlag = Boolean.parseBoolean(map.getOne(PARAM_INSTALL));

        // We use the resolver to expand any system properties
        if (!NetUtils.isPortStringValid(resolver.resolve(sshport))) {
            throw new CommandValidationException(Strings.get(
                    "ssh.invalid.port", sshport));
        }

        int port = Integer.parseInt(resolver.resolve(sshport));

        try {
            // sshpassword and sshkeypassphrase may be password alias.
            // Those aliases are handled by sshLauncher
            String resolvedInstallDir = resolver.resolve(installdir);

            sshL.validate(resolver.resolve(nodehost),
                    port,
                    resolver.resolve(sshuser),
                    sshpassword,
                    resolver.resolve(sshkeyfile),
                    sshkeypassphrase,
                    resolvedInstallDir,
                    // Landmark file to ensure valid GF install
                    LANDMARK_FILE,
                    logger);
        } catch (FileNotFoundException ex) {
            if (!installFlag) {
                    logger.warning(StringUtils.cat(": ", ex.getMessage(), "", sshL.toString()));
                    throw new CommandValidationException(ex.getMessage());
                }
            
        } catch (IOException e) {
            String m1 = e.getMessage();
            String m2 = "";
            Throwable e2 = e.getCause();
            if (e2 != null) {
                m2 = e2.getMessage();
            }
            
            String msg = Strings.get("ssh.bad.connect", nodehost, "SSH");
            logger.warning(StringUtils.cat(": ", msg, m1, m2, sshL.toString()));
            throw new CommandValidationException(StringUtils.cat(NL, msg, m1, m2));
        }
    }

    /**
     * Takes an action report and updates the message in the report with
     * the message from the root cause of the report.
     *
     * @param report
     */
    static void sanitizeReport(ActionReport report) {
        if (report != null && report.hasFailures()
                && report.getFailureCause() != null) {
            Throwable rootCause = ExceptionUtil.getRootCause(
                    report.getFailureCause());
            if (rootCause != null && StringUtils.ok(rootCause.getMessage())) {
                report.setMessage(rootCause.getMessage());
            }
        }
    }

    /**
     * Run on admin command on a node and handle setting the message in the
     * ActionReport on an error. Note that on success no message is set in
     * the action report
     *
     * @param node  The node to run the command on. Can be local or remote
     * @param command  asadmin command to run. The list must contain all
     *                  parameters to asadmin, but not "asadmin" itself.
     * @param context   The command context. The ActionReport in this
     *                  context will be updated on an error to contain an
     *                  appropriate error message.
     * @param firstErrorMessage The first message to use if an error is
     *                          encountered. Usually something like
     *                          "Could not start instance".
     * @param humanCommand  The command the user should run on the node if
     *                      we failed to run the passed command.
     * @param output        Output from the run command.
     * @param waitForReaderThreads True: wait for the command IO to complete.
     *                      False: don't wait for IO to complete, just for
     *                      process to end.
     *                      Currently this only applies to locally run commands
     *                      and should only be set to false by start-instance
     *                      (see bug 12777).
     */
    void runAdminCommandOnNode(Node node, List<String> command,
            AdminCommandContext context, String firstErrorMessage,
            String humanCommand, StringBuilder output,
            boolean waitForReaderThreads) {

        ActionReport report = context.getActionReport();
        boolean failure = true;
        String msg1 = firstErrorMessage;
        String msg2 = "";
        String msg3 = "";
        String nodeHost = node.getNodeHost();
        String nodeName = node.getName();
        String installDir = node.getInstallDir();

        if (output == null) {
            output = new StringBuilder();
        }

        if (StringUtils.ok(humanCommand)) {
            msg3 = Strings.get("node.remote.tocomplete",
                    nodeHost, installDir, humanCommand);
        }

        NodeRunner nr = new NodeRunner(habitat, logger);
        try {
            int status = nr.runAdminCommandOnNode(node, output, waitForReaderThreads,
                    command, context);
            if (status != 0) {
                // Command ran, but didn't succeed. Log full information
                msg2 = Strings.get("node.command.failed", nodeName,
                        nodeHost, output.toString().trim(), nr.getLastCommandRun());
                logger.warning(StringUtils.cat(": ", msg1, msg2, msg3));
                // Don't expose command name to user in case it is a hidden command
                msg2 = Strings.get("node.command.failed.short", nodeName,
                        nodeHost, output.toString().trim());
            }
            else {
                failure = false;
                logger.info(output.toString().trim());
            }
        }
        catch (SSHCommandExecutionException ec) {
            msg2 = Strings.get("node.ssh.bad.connect",
                    nodeName, nodeHost, ec.getMessage());
            // Log some extra info
            String msg = Strings.get("node.command.failed.ssh.details",
                    nodeName, nodeHost, ec.getCommandRun(), ec.getMessage(),
                    ec.getSSHSettings());
            logger.warning(StringUtils.cat(": ", msg1, msg, msg3));
        }
        catch (ProcessManagerException ex) {
            msg2 = Strings.get("node.command.failed.local.details",
                    ex.getMessage(), nr.getLastCommandRun());
            logger.warning(StringUtils.cat(": ", msg1, msg2, msg3));
            // User message doesn't have command that was run
            msg2 = Strings.get("node.command.failed.local.exception",
                    ex.getMessage());
        }
        catch (UnsupportedOperationException e) {
            msg2 = Strings.get("node.not.ssh", nodeName, nodeHost);
            logger.warning(StringUtils.cat(": ", msg1, msg2, msg3));
        }
        catch (IllegalArgumentException e) {
            msg2 = e.getMessage();
            logger.warning(StringUtils.cat(": ", msg1, msg2, msg3));
        }

        if (failure) {
            report.setMessage(StringUtils.cat(NL + NL, msg1, msg2, msg3));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        else {
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }

    }

    void runAdminCommandOnNode(Node node, List<String> command,
            AdminCommandContext context, String firstErrorMessage,
            String humanCommand, StringBuilder output) {

        runAdminCommandOnNode(node, command, context, firstErrorMessage,
                humanCommand, output, true);
    }

    private RemoteType parseType(ParameterMap map) throws CommandValidationException {

        try {
            return RemoteType.valueOf(map.getOne(PARAM_TYPE));
        }
        catch (Exception e) {
            throw new CommandValidationException(e);
        }
    }

    // DCOMFIX - installroot is probably the parent of the glassfish directory
    // DCOMFIX it would be nice to have the actual install-root of GF in the config
    private String getInstallRoot(String installDir) {
        // Imagine if you send in "C:\" as installDir.  THat is NOT the same as "C:" !
        // that's why we need extra processing.
        char[] chars = installDir.toCharArray();
        char end = chars[chars.length - 1];

        if (end != '/' && end != '\\')
            return installDir + "/glassfish";
        else
            return installDir + "glassfish";
    }
}
