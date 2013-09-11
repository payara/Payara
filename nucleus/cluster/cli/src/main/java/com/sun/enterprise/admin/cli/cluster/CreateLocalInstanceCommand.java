/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 *  This is a local command that calls the primitive remote _register-instance to add the
 *  entries in domain.xml and then the primitive local command _create-instance-filesystem
 *  to create the empty directory structure and das.properties
 *
 */
@Service(name = "create-local-instance")
@PerLookup
@I18n("create.local.instance")
public final class CreateLocalInstanceCommand extends CreateLocalInstanceFilesystemCommand {
    private final String CONFIG = "config";
    private final String CLUSTER = "cluster";
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(CreateLocalInstanceCommand.class);

    @Param(name = CONFIG, optional = true)
    private String configName;

    @Param(name = CLUSTER, optional = true)
    private String clusterName;

    @Param(name="lbenabled", optional = true)
    private Boolean lbEnabled;

    @Param(name = "systemproperties", optional = true, separator = ':')
    private String systemProperties;     // XXX - should it be a Properties?

    @Param(name = "portbase", optional = true)
    private String portBase;

    @Param(name = "checkports", optional = true, defaultValue = "true")
    private boolean checkPorts = true;

    @Param(name = "savemasterpassword", optional = true, defaultValue = "false")
    private boolean saveMasterPassword = false;

    @Param(name = "usemasterpassword", optional = true, defaultValue = "false")
    private boolean useMasterPassword = false;

    private String masterPassword = null;

    private static final String RENDEZVOUS_PROPERTY_NAME = "rendezvousOccurred";
    private String INSTANCE_DOTTED_NAME;
    private String RENDEZVOUS_DOTTED_NAME;
    private boolean _rendezvousOccurred;
    private String _node;
    private static final String DEFAULT_MASTER_PASSWORD = KeystoreManager.DEFAULT_MASTER_PASSWORD;
    private ParamModelData masterPasswordOption;
    private static final String MASTER_PASSWORD_ALIAS="master-password";

    /**
     */
    @Override
    protected void validate()
            throws CommandException {
        echoCommand();
        if (configName != null && clusterName != null) {
            throw new CommandException(
                    Strings.get("ConfigClusterConflict"));
        }

        if (lbEnabled != null && clusterName == null) {
            throw new CommandException(
                    Strings.get("lbenabledNotForStandaloneInstance"));
        }

        setDasDefaultsOnly = true; //Issue 12847 - Call super.validate to setDasDefaults only
        super.validate();          //so _validate-node uses das host from das.properties. No dirs created.
        if (node != null) {
            //BugDB 13431949 - If installdir is not specified on node, call _validate-node on DAS to populate installdir.
            //If installdir is specified on node, validate installdir locally so we can take advantage of java path processing to
            //normalize the installdir from the node.
            //If installdir has tokens, call _validate-node on DAS to have DAS resolve the tokens
            //If we are on Windows, call _validate-node on DAS instead of relying on the path processing in the local validation.
            String nodeInstallDir = getNodeInstallDir();
            if (nodeInstallDir == null || nodeInstallDir.isEmpty() || TokenResolver.hasToken(nodeInstallDir) || OS.isWindows()) {
                validateNode(node, getProductRootPath(), getInstanceHostName(true));
            } else {
                validateNodeInstallDirLocal(nodeInstallDir, getProductRootPath());
                validateNode(node, null, getInstanceHostName(true));
            }

        }

        if (!rendezvousWithDAS()) {
            throw new CommandException(
                    Strings.get("Instance.rendezvousFailed", DASHost, "" + DASPort));
        }
        if (instanceName != null && instanceName.equals(SystemPropertyConstants.DAS_SERVER_NAME)) {
            throw new CommandException(
                    Strings.get("Instance.alreadyExists", SystemPropertyConstants.DAS_SERVER_NAME));
        }
        setDomainName();
        setDasDefaultsOnly = false;
        super.validate();  // instanceName is validated and set in super.validate(), directories created
        INSTANCE_DOTTED_NAME = "servers.server." + instanceName;
        RENDEZVOUS_DOTTED_NAME = INSTANCE_DOTTED_NAME + ".property." + RENDEZVOUS_PROPERTY_NAME;

        _rendezvousOccurred = rendezvousOccurred();
        if (_rendezvousOccurred) {
            throw new CommandException(
                    Strings.get("Instance.rendezvousAlready", instanceName));
        }
    }

    /**
     */
    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {
        int exitCode = -1;

        if (node == null) {
            if(nodeDirChild == null)
                throw new CommandException(Strings.get("internal.error",
                        "nodeDirChild was null.  The Base Class is supposed to "
                        + "guarantee that this won't happen"));
            _node = nodeDirChild.getName();
            String nodeHost = getInstanceHostName(true);
            createNodeImplicit(_node, getProductRootPath(), nodeHost);
        } else {
            _node = node;
        }

        if (isRegisteredToDAS()) {
            if (!_rendezvousOccurred) {
                setRendezvousOccurred("true");
                _rendezvousOccurred = true;
            }

        } else {
            validateInstanceDirUnique();
            try {
                registerToDAS();
                _rendezvousOccurred = true;
            } catch (CommandException ce) {
                FileUtils.deleteFileNowOrLater(instanceDir);
                throw ce;
            }
        }
        bootstrapSecureAdminFiles();
        try {
            exitCode = super.executeCommand();
            if (exitCode == SUCCESS) {
                saveMasterPassword();
            }
        } catch (CommandException ce) {
            String msg = "Something went wrong in creating the local filesystem for instance " + instanceName;
            if (ce.getLocalizedMessage() != null) {
                msg = msg + ": " + ce.getLocalizedMessage();
            }
            logger.severe(msg);
            setRendezvousOccurred("false");
            _rendezvousOccurred = false;

            throw new CommandException(msg, ce);
        }
        return exitCode;
    }

    private void validateInstanceDirUnique() throws CommandException {
        RemoteCLICommand rc = new RemoteCLICommand("list-instances", this.programOpts, this.env);
        String returnOutput =
                rc.executeAndReturnOutput("list-instances", "--nostatus", _node);
        if (returnOutput == null)
            return;
        String[] registeredInstanceNamesOnThisNode = returnOutput.split("\r?\n");
        for (String registeredInstanceName : registeredInstanceNamesOnThisNode) {
            File instanceListDir = new File(nodeDirChild, registeredInstanceName);
            if (instanceName != null && registeredInstanceName.equalsIgnoreCase(instanceName)) {
                if (instanceDir != null && instanceListDir.equals(instanceDir)){
                    throw new CommandException(
                            Strings.get("Instance.duplicateInstanceDir",
                                    instanceName, registeredInstanceName));
                }
            }
        }
    }
    
    private int bootstrapSecureAdminFiles() throws CommandException {
        RemoteCLICommand rc = new RemoteCLICommand("_bootstrap-secure-admin", this.programOpts, this.env);
        rc.setFileOutputDirectory(instanceDir);
        final int result = rc.execute(new String[] {"_bootstrap-secure-admin"});
        return result;
    }

    /**
     * If --savemasterpassword=true,
     * then --usemasterpassword is set to true also
     * If AS_ADMIN_MASTERPASSWORD from --passwordfile exists that is used.
     * If it does not exist, the user is asked to enter the master password.
     * The password is validated against the keystore if it exists. If successful, master-password
     * is saved to the server instance directory <glassfish-install>/nodes/<host name>/master-password.
     * If the password entered does not match the keystore, master-password is not
     * saved and a warning is displayed. The command is still successful.
     * The default value of --usemasterpassword is false.
     *
     * When savemasterpassword is false, the keystore is encrypted with a well-known password
     * that is built into the system, thus affording no additional security.
     * The master password must be the same for all instances in a domain.

     * @throws CommandException
     */
    private void saveMasterPassword() throws CommandException {
        masterPasswordOption = new ParamModelData(CLIConstants.MASTER_PASSWORD,
                String.class, false, null);
        masterPasswordOption.prompt = Strings.get("MasterPassword");
        masterPasswordOption.promptAgain = Strings.get("MasterPasswordAgain");
        masterPasswordOption.param._password = true;
        if (saveMasterPassword)
            useMasterPassword = true;
        if (useMasterPassword)
            masterPassword = getPassword(masterPasswordOption,
                DEFAULT_MASTER_PASSWORD, true);
        if (masterPassword == null)
            masterPassword = DEFAULT_MASTER_PASSWORD;

        if (saveMasterPassword) {
            File mp = new File(new File(getServerDirs().getServerDir(), "config"), "keystore.jks");
            if (mp.canRead()) {
                if (verifyMasterPassword(masterPassword)) {

                    createMasterPasswordFile(masterPassword);
                } else {
                    logger.info(Strings.get("masterPasswordIncorrect"));
                }
            } else {
                createMasterPasswordFile(masterPassword);
            }
        }

    }


    /**
     * Create the master password keystore. This routine can also modify the master password
     * if the keystore already exists
     * @param masterPassword
     * @throws CommandException
     */
    protected void createMasterPasswordFile(String masterPassword) throws CommandException {
        final File pwdFile = new File(this.getServerDirs().getAgentDir(), MASTER_PASSWORD_ALIAS);
        try {
            PasswordAdapter p = new PasswordAdapter(pwdFile.getAbsolutePath(),
                MASTER_PASSWORD_ALIAS.toCharArray());
            p.setPasswordForAlias(MASTER_PASSWORD_ALIAS, masterPassword.getBytes());
            chmod("600", pwdFile);
        } catch (Exception ex) {
            throw new CommandException(Strings.get("masterPasswordFileNotCreated", pwdFile),
                ex);
        }
    }

    protected void chmod(String args, File file) throws IOException {
        if (OS.isUNIX()) {
            if (!file.exists()) throw new IOException(Strings.get("fileNotFound", file.getAbsolutePath()));

            // " +" regular expression for 1 or more spaces
            final String[] argsString = args.split(" +");
            List<String> cmdList = new ArrayList<String>();
            cmdList.add("/bin/chmod");
            for (String arg : argsString)
                cmdList.add(arg);
            cmdList.add(file.getAbsolutePath());
            new ProcessBuilder(cmdList).start();
        }
    }

    private boolean rendezvousWithDAS() {
        try {
            //logger.info(Strings.get("Instance.rendezvousAttempt", DASHost, "" + DASPort));
            getUptime();
            logger.info(Strings.get("Instance.rendezvousSuccess", DASHost, "" + DASPort));
            return true;
        } catch (CommandException ex) {
            return false;
        }
    }

    private int registerToDAS() throws CommandException {
        ArrayList<String> argsList = new ArrayList<String>();
        argsList.add(0, "_register-instance");
        if (clusterName != null) {
            argsList.add("--cluster");
            argsList.add(clusterName);
        }
        if (lbEnabled != null) {
            argsList.add("--lbenabled");
            argsList.add(lbEnabled.toString());
        }
        if (configName != null) {
            argsList.add("--config");
            argsList.add(configName);
        }
        if (_node != null) {
            argsList.add("--node");
            argsList.add(_node);
        }
        argsList.add("--checkports");
        argsList.add(String.valueOf(checkPorts));
        if (portBase != null) {
            argsList.add("--portbase");
            argsList.add(portBase);
        }
        if (systemProperties != null) {
            argsList.add("--systemproperties");
            argsList.add(systemProperties);
        }
        argsList.add("--properties");
        argsList.add(RENDEZVOUS_PROPERTY_NAME+"=true");
        argsList.add(this.instanceName);

        String[] argsArray = new String[argsList.size()];
        argsArray = argsList.toArray(argsArray);

        RemoteCLICommand rc = new RemoteCLICommand("_register-instance", this.programOpts, this.env);
        return rc.execute(argsArray);
    }

    private boolean isRegisteredToDAS() {
        boolean isRegistered = false;
        try {
            RemoteCLICommand rc = new RemoteCLICommand("get", this.programOpts, this.env);
            rc.executeAndReturnOutput("get", INSTANCE_DOTTED_NAME);
            isRegistered = true;
        } catch (CommandException ex) {
            logger.log(Level.FINER, "{0} is not yet registered to DAS.", instanceName);
            isRegistered=false;
        }
        return isRegistered;
    }

    private boolean rendezvousOccurred() {
        boolean rendezvousOccurred = false;
        RemoteCLICommand rc = null;
        try {
            rc = new RemoteCLICommand("get", this.programOpts, this.env);
            String s = rc.executeAndReturnOutput("get", RENDEZVOUS_DOTTED_NAME);
            String val = s.substring(s.indexOf("=") + 1);
            rendezvousOccurred = Boolean.parseBoolean(val);
            logger.log(Level.FINER, "rendezvousOccurred = {0} for instance {1}", new Object[]{val, instanceName});
        } catch (CommandException ce) {
            logger.log(Level.FINER,RENDEZVOUS_PROPERTY_NAME + " property may not be set yet on {0}", instanceName);
        }
        return rendezvousOccurred;
    }

    private void setRendezvousOccurred(String rendezVal) throws CommandException {
        String dottedName = RENDEZVOUS_DOTTED_NAME + "=" + rendezVal;
        RemoteCLICommand rc = new RemoteCLICommand("set", this.programOpts, this.env);
        logger.log(Level.FINER, "Setting rendezvousOccurred to {0} for instance {1}", new Object[]{rendezVal, instanceName});
        rc.executeAndReturnOutput("set", dottedName);
    }

    /* installdir is product install dir (parent of glassfish install root) */
    private int createNodeImplicit(String name, String installdir, String nodeHost) throws CommandException {
        ArrayList<String> argsList = new ArrayList<String>();
        argsList.add(0, "_create-node-implicit");
        if (name != null) {
            argsList.add("--name");
            argsList.add(name);
        }
        if (nodeDir != null) {
            argsList.add("--nodedir");
            argsList.add(nodeDir);
        }
        if (installdir != null) {
            argsList.add("--installdir");
            argsList.add(installdir);
        }
        argsList.add(nodeHost);

        String[] argsArray = new String[argsList.size()];
        argsArray = argsList.toArray(argsArray);

        RemoteCLICommand rc = new RemoteCLICommand("_create-node-implicit", this.programOpts, this.env);
        return rc.execute(argsArray);
    }

    /* installdir is product install dir (parent of glassfish install root) */
    private int validateNode(String name, String installdir, String nodeHost) throws CommandException {
        ArrayList<String> argsList = new ArrayList<String>();
        argsList.add(0, "_validate-node");

        if (nodeDir != null) {
            argsList.add("--nodedir");
            argsList.add(nodeDir);
        }
        if (nodeHost != null) {
            argsList.add("--nodehost");
            argsList.add(nodeHost);
        }
        if (installdir != null) {
            argsList.add("--installdir");
            argsList.add(installdir);
        }

        argsList.add(name);

        String[] argsArray = new String[argsList.size()];
        argsArray = argsList.toArray(argsArray);

        RemoteCLICommand rc = new RemoteCLICommand("_validate-node", this.programOpts, this.env);
        return rc.execute(argsArray);
    }

    private void validateNodeInstallDirLocal(String nodeInstallDir, String installDir) throws CommandValidationException {
        String canonicalNodeInstallDir = FileUtils.safeGetCanonicalPath(new File(nodeInstallDir));
        String canonicalInstallDir = FileUtils.safeGetCanonicalPath(new File(installDir));
        if (canonicalNodeInstallDir == null || canonicalInstallDir == null) {
            throw new CommandValidationException(
                Strings.get("Instance.installdir.null", node,
                           canonicalInstallDir, canonicalNodeInstallDir));
        }

        if ( !canonicalInstallDir.equals(canonicalNodeInstallDir) ) {
            throw new CommandValidationException(
                Strings.get("Instance.installdir.mismatch", node,
                           canonicalInstallDir, canonicalNodeInstallDir));
        }
    }

    private String getInstanceHostName(boolean isCanonical) throws CommandException {
        String instanceHostName = null;
        InetAddress localHost = null;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            throw new CommandException(Strings.get("cantGetHostName", ex));
        }
        if (localHost != null) {
            if (isCanonical) {
                instanceHostName = localHost.getCanonicalHostName();
            } else {
                instanceHostName = localHost.getHostName();
            }
        }
        return instanceHostName;
    }

    private void setDomainName() throws CommandException {
        RemoteCLICommand rc = new RemoteCLICommand("_get-runtime-info", this.programOpts, this.env);
        ActionReport report = rc.executeAndReturnActionReport("_get-runtime-info", "--target", "server");
        this.domainName = report.findProperty("domain_name");
    }

    @Override
    protected boolean mkdirs(File f) {
        if (setDasDefaultsOnly) {
            return true;
        } else {
            return f.mkdirs();
        }
    }

    @Override
    protected boolean isDirectory(File f) {
        if (setDasDefaultsOnly) {
            return true;
        } else {
            return f.isDirectory();
        }
    }

    @Override
    protected boolean setServerDirs() {
        if (setDasDefaultsOnly) {
            return false;
        } else {
            return true;
        }
    }

    private void echoCommand() {
        if (this.programOpts.isEcho()) {
            logger.info(this.toString());
            programOpts.setEcho(false); // only echo this command
        }
    }
}
