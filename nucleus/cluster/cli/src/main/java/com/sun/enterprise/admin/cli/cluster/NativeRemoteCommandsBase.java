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

import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.bootstrap.HK2Populator;
import org.glassfish.hk2.bootstrap.impl.ClasspathDescriptorFileFinder;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.RelativePathResolver;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import com.sun.enterprise.admin.cli.CLICommand;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Node;

import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.io.DomainDirs;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.net.NetUtils;

import com.trilead.ssh2.SFTPv3DirectoryEntry;

import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;

import com.sun.enterprise.security.store.PasswordAdapter;

/**
 *  Base class for SSH provisioning commands.
 *
 *  Byron Nevins Aug 2011.  SSH was hard-coded in.  Now we
 *  want to use jcifs (SAMBA) for Windows.
 */
abstract class NativeRemoteCommandsBase extends CLICommand {
    @Param(optional = false, primary = true, multiple = true)
    String[] hosts;
    String sshpassword;
    String sshkeypassphrase = null;
    boolean promptPass = false;
    TokenResolver resolver = null;
    private String remoteUser;
    private int remotePort;

    NativeRemoteCommandsBase() {
        // Create a resolver that can replace system properties in strings
        resolver = new TokenResolver();
    }

    // all of this rigamarole is to get the right names for parameters in front
    // of user eyeballs
    abstract String getRawRemoteUser();
    abstract int getRawRemotePort();
    abstract String getSshKeyFile();

    @Override
    protected void validate() throws CommandException {
        remoteUser = resolver.resolve(getRawRemoteUser());
    }

    final String getRemoteUser() {
        return remoteUser;
    }
    final int getRemotePort() {
        return remotePort;
    }
    final void setRemotePort(int newPort) {
        remotePort = newPort;
    }

    /**
     * Get SSH password from password file or user.
     */
    String getSSHPassword(String node) throws CommandException {
        return getRemotePassword(node, "AS_ADMIN_SSHPASSWORD");
    }

    /**
     * Get DCOM password from password file or user.
     */
    String getWindowsPassword(String node) throws CommandException {
        return getRemotePassword(node, "AS_ADMIN_WINDOWSPASSWORD");
    }

    /**
     * Get SSH password from password file or user.
     */
    private String getRemotePassword(String node, String key) throws CommandException {
        String password = getFromPasswordFile(key);

        if (password != null) {
            String alias = RelativePathResolver.getAlias(password);
            if (alias != null)
                password = expandPasswordAlias(node, alias, true);
        }

        //get password from user if not found in password file
        if (password == null) {
            if (programOpts.isInteractive()) {
                password = readPassword(Strings.get("SSHPasswordPrompt", getRemoteUser(), node));
            }
            else {
                throw new CommandException(Strings.get("SSHPasswordNotFound"));
            }
        }
        return password;
    }

    /**
     * Get SSH key passphrase from password file or user.
     */
    String getSSHPassphrase(boolean verifyConn) throws CommandException {
        String passphrase = getFromPasswordFile("AS_ADMIN_SSHKEYPASSPHRASE");

        if (passphrase != null) {
            String alias = RelativePathResolver.getAlias(passphrase);

            if (alias != null)
                passphrase = expandPasswordAlias(null, alias, verifyConn);
        }

        //get password from user if not found in password file
        if (passphrase == null) {
            if (programOpts.isInteractive()) {
                //i18n
                passphrase = readPassword(Strings.get("SSHPassphrasePrompt", getSshKeyFile()));
            }
            else {
                passphrase = ""; //empty passphrase
            }
        }
        return passphrase;
    }

    /**
     * Get domain master password from password file or user.
     */
    String getMasterPassword(String domain) throws CommandException {
        String masterPass = getFromPasswordFile("AS_ADMIN_MASTERPASSWORD");

        //get password from user if not found in password file
        if (masterPass == null) {
            if (programOpts.isInteractive()) {
                //i18n
                masterPass = readPassword(Strings.get("DomainMasterPasswordPrompt", domain));
            }
            else {
                masterPass = "changeit"; //default
            }
        }
        return masterPass;
    }

    private String getFromPasswordFile(String name) {
        return passwords.get(name);
    }

    boolean isValidAnswer(String val) {
        return val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("no")
                || val.equalsIgnoreCase("y") || val.equalsIgnoreCase("n");
    }

    /**
     * Method to delete files and directories on remote host
     * 'nodes' directory is not considered for deletion since it would contain
     * configuration information.
     * @param sftpClient sftp client instance
     * @param dasFiles file layout on DAS
     * @param dir directory to be removed
     * @param force true means delete all files, false means leave non-GlassFish files
     *              untouched
     * @throws IOException in case of error
     */
    // byron XXXX
    void deleteRemoteFiles(SFTPClient sftpClient, List<String> dasFiles, String dir, boolean force)
            throws IOException {

        for (SFTPv3DirectoryEntry directoryEntry : (List<SFTPv3DirectoryEntry>) sftpClient.ls(dir)) {
            if (directoryEntry.filename.equals(".") || directoryEntry.filename.equals("..")
                    || directoryEntry.filename.equals("nodes")) {
                continue;
            }
            else if (directoryEntry.attributes.isDirectory()) {
                String f1 = dir + "/" + directoryEntry.filename;
                deleteRemoteFiles(sftpClient, dasFiles, f1, force);
                //only if file is present in DAS, it is targeted for removal on remote host
                //using force deletes all files on remote host
                if (force) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Force removing directory " + f1);
                    if (isRemoteDirectoryEmpty(sftpClient, f1)) {
                        sftpClient.rmdir(f1);
                    }
                }
                else {
                    if (dasFiles.contains(f1)) {
                        if (isRemoteDirectoryEmpty(sftpClient, f1)) {
                            sftpClient.rmdir(f1);
                        }
                    }
                }
            }
            else {
                String f2 = dir + "/" + directoryEntry.filename;
                if (force) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Force removing file " + f2);
                    sftpClient.rm(f2);
                }
                else {
                    if (dasFiles.contains(f2))
                        sftpClient.rm(f2);
                }
            }
        }
    }

    /**
     * Method to check if specified remote directory contains files
     *
     * @param sftp SFTP client handle
     * @param file path to remote directory
     * @return true if empty, false otherwise
     * @throws IOException
     */
    boolean isRemoteDirectoryEmpty(SFTPClient sftp, String file) throws IOException {
        List<SFTPv3DirectoryEntry> l = (List<SFTPv3DirectoryEntry>) sftp.ls(file);
        if (l.size() > 2)
            return false;
        return true;
    }

    /**
     * Remove trailing slash from a path string
     * @param s
     * @return
     */
    String removeTrailingSlash(String s) {
        if (!StringUtils.ok(s))
            return s;

        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Obtains the real password from the domain specific keystore given an alias
     * @param host host that we are connecting to
     * @param alias password alias of form ${ALIAS=xxx}
     * @return real password of ssh user, null if not found
     */
    String expandPasswordAlias(String host, String alias, boolean verifyConn) {
        String expandedPassword = null;
        boolean connStatus = false;

        try {
            File domainsDirFile = DomainDirs.getDefaultDomainsDir();

            //get the list of domains
            File[] files = domainsDirFile.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory();
                }
            });

            for (File f : files) {
                //the following property is required for initializing the password helper
                System.setProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY, f.getAbsolutePath());
                try {
                    final PasswordAdapter pa = new PasswordAdapter(null);
                    final boolean exists = pa.aliasExists(alias);
                    if (exists) {
                        String mPass = getMasterPassword(f.getName());
                        expandedPassword = new PasswordAdapter(mPass.toCharArray()).getPasswordForAlias(alias);
                    }
                }
                catch (Exception e) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(StringUtils.cat(": ", alias, e.getMessage()));
                    }
                    logger.warning(Strings.get("GetPasswordFailure", f.getName()));
                    continue;
                }

                if (expandedPassword != null) {
                    SSHLauncher sshL = new SSHLauncher();
                    if (host != null) {
                        sshpassword = expandedPassword;
                        sshL.init(getRemoteUser(), host, getRemotePort(), sshpassword, null, null, logger);
                        connStatus = sshL.checkPasswordAuth();
                        if (!connStatus) {
                            logger.warning(Strings.get("PasswordAuthFailure", f.getName()));
                        }
                    }
                    else {
                        sshkeypassphrase = expandedPassword;
                        if (verifyConn) {
                            sshL.init(getRemoteUser(), hosts[0], getRemotePort(), sshpassword, getSshKeyFile(), sshkeypassphrase, logger);
                            connStatus = sshL.checkConnection();
                            if (!connStatus) {
                                logger.warning(Strings.get("PasswordAuthFailure", f.getName()));
                            }
                        }
                    }

                    if (connStatus) {
                        break;
                    }
                }
            }
        }
        catch (IOException ioe) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(ioe.getMessage());
            }
        }
        return expandedPassword;
    }

    /**
     * This method first obtains a list of files under the product installation
     * directory. It then modifies each path by prepending it with remote install dir path.
     * For ex. glassfish/lib/appserv-rt.jar becomes
     * <remote-install-path>/glassfish/lib/appserv-rt.jar
     * @return List of files and directories
     * @throws IOException
     */
    List<String> getListOfInstallFiles(String installDir) throws IOException {
        String ins = resolver.resolve("${com.sun.aas.productRoot}");
        Set files = FileUtils.getAllFilesAndDirectoriesUnder(new File(ins));
        if (logger.isLoggable(Level.FINER))
            logger.finer("Total number of files under " + ins + " = " +
                                                                files.size());
        String remoteDir = installDir;
        if (!installDir.endsWith("/")) {
            remoteDir = remoteDir + "/";
        }
        List<String> modList = new ArrayList<String>();
        for (Object f : files) {
            modList.add(remoteDir + FileUtils.makeForwardSlashes(((File) f).getPath()));
        }
        return modList;
    }

    /**
     * Check for existence of key file.
     * @param file
     * @throws CommandException
     */
    void validateKey(String file) throws CommandException {
        File f = new File(file);
        if (!f.exists()) {
            throw new CommandException(Strings.get("KeyDoesNotExist", file));
        }
    }
}
