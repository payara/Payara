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
// Portions Copyright 2018-2026 Payara Foundation and/or its affiliates

package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.util.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.sshd.sftp.client.SftpClient;
import org.glassfish.internal.api.RelativePathResolver;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import com.sun.enterprise.admin.cli.CLICommand;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;


import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.io.DomainDirs;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.StringUtils;


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
        remotePort = getRawRemotePort();
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
                char[] pArr = readPassword(Strings.get("SSHPasswordPrompt", getRemoteUser(), node));
                password = pArr != null ? new String(pArr) : null;
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
                char[] pArr = readPassword(Strings.get("SSHPassphrasePrompt", getSshKeyFile()));
                passphrase = pArr != null ? new String(pArr) : null;
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
                char[] mpArr = readPassword(Strings.get("DomainMasterPasswordPrompt", domain));
                masterPass = mpArr != null ? new String(mpArr) : null;
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
     * Method to delete files and directories on remote host.
     * 'nodes' directory is not considered for deletion since it would contain
     * configuration information.
     */
    void deleteRemoteFiles(SFTPClient sftpClient, List<String> dasFiles, String dir, boolean force)
            throws IOException {

        for (SftpClient.DirEntry directoryEntry : sftpClient.ls(dir)) {
            if (directoryEntry.getFilename().equals(".") || directoryEntry.getFilename().equals("..")
                    || directoryEntry.getFilename().equals("nodes")) {
                continue;
            } else if (directoryEntry.getAttributes().isDirectory()) {
                String f1 = dir + "/" + directoryEntry.getFilename();
                deleteRemoteFiles(sftpClient, dasFiles, f1, force);
                if (force) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Force removing directory " + f1);
                    }
                    if (isRemoteDirectoryEmpty(sftpClient, f1)) {
                        sftpClient.rmdir(f1);
                    }
                } else {
                    if (dasFiles.contains(f1) && isRemoteDirectoryEmpty(sftpClient, f1)) {
                        sftpClient.rmdir(f1);
                    }
                }
            } else {
                String f2 = dir + "/" + directoryEntry.getFilename();
                if (force) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Force removing file " + f2);
                    }
                    sftpClient.rm(f2);
                } else {
                    if (dasFiles.contains(f2)) {
                        sftpClient.rm(f2);
                    }
                }
            }
        }
    }

    /**
     * Method to check if specified remote directory contains files.
     */
    boolean isRemoteDirectoryEmpty(SFTPClient sftp, String file) throws IOException {
        List<SftpClient.DirEntry> l = sftp.ls(file);
        return l.size() <= 2;
    }

    /**
     * Obtains the real password from the domain specific keystore given an alias.
     */
    String expandPasswordAlias(String host, String alias, boolean verifyConn) {
        String expandedPassword = null;
        boolean connStatus = false;

        try {
            File domainsDirFile = DomainDirs.getDefaultDomainsDir();
            if (domainsDirFile != null) {
                File[] files = domainsDirFile.listFiles(File::isDirectory);
                if (files != null) {
                    for (File f : files) {
                        System.setProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY, f.getAbsolutePath());
                        try {
                            final PasswordAdapter pa = new PasswordAdapter(null);
                            final boolean exists = pa.aliasExists(alias);
                            if (exists) {
                                String mPass = getMasterPassword(f.getName());
                                expandedPassword = new PasswordAdapter(mPass.toCharArray()).getPasswordForAlias(alias);
                            }
                        } catch (Exception e) {
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
                            } else {
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
            }
        } catch (IOException ioe) {
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
     * &lt;remote-install-path&gt;/glassfish/lib/appserv-rt.jar
     * @return List of files and directories
     * @throws IOException
     */
    List<String> getListOfInstallFiles(String installDir) throws IOException {
        String ins = resolver.resolve("${com.sun.aas.productRoot}");
        Set files = FileUtils.getAllFilesAndDirectoriesUnder(new File(ins));
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "Total number of files under {0} = {1}", new Object[]{ins, files.size()});
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
