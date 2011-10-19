/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.FileUtils;
import com.trilead.ssh2.SCPClient;
import java.io.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.cluster.ssh.util.SSHUtil;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * @author Byron Nevins
 */
@Service(name = "install-node-ssh")
@Scoped(PerLookup.class)
public class InstallNodeSshCommand extends InstallNodeBaseCommand {
    @Param(name = "sshuser", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(optional = true, defaultValue = "22", name = "sshport")
    int port;
    @Param(optional = true)
    String sshkeyfile;
    @Inject
    private SSHLauncher sshLauncher;

    @Override
    String getRawRemoteUser() {
        return user;
    }

    @Override
    int getRawRemotePort() {
        return port;
    }

    @Override
    String getSshKeyFile() {
        return sshkeyfile;
    }

    @Override
    protected void validate() throws CommandException {
        super.validate();
        if (sshkeyfile == null) {
            //if user hasn't specified a key file check if key exists in
            //default location
            String existingKey = SSHUtil.getExistingKeyFile();
            if (existingKey == null) {
                promptPass = true;
            }
            else {
                sshkeyfile = existingKey;
            }
        }
        else {
            validateKey(sshkeyfile);
        }

        //we need the key passphrase if key is encrypted
        if (sshkeyfile != null && isEncryptedKey()) {
            sshkeypassphrase = getSSHPassphrase(true);
        }
    }

    @Override
    void copyToHosts(File zipFile, ArrayList<String> binDirFiles) throws CommandException {
        // exception handling is too complicated to mess with in the real method.
        // the idea is to catch everything here and re-throw as one kind
        // the caller is just going to do it anyway so we may as well do it here.
        // And it makes the signature simpler for other subclasses...
        try {
            copyToHostsInternal(zipFile, binDirFiles);
        }
        catch (CommandException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new CommandException(ex);
        }
        catch (InterruptedException ex) {
            throw new CommandException(ex);
        }
    }

    private void copyToHostsInternal(File zipFile, ArrayList<String> binDirFiles) throws IOException, InterruptedException, CommandException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        for (String host : hosts) {
            sshLauncher.init(getRemoteUser(), host, getRemotePort(), sshpassword, getSshKeyFile(), sshkeypassphrase, logger);

            if (getSshKeyFile() != null && !sshLauncher.checkConnection()) {
                //key auth failed, so use password auth
                promptPass = true;
            }

            if (promptPass) {
                sshpassword = getSSHPassword(host);
                //re-initialize
                sshLauncher.init(getRemoteUser(), host, getRemotePort(), sshpassword, getSshKeyFile(), sshkeypassphrase, logger);
            }

            String sshInstallDir = getInstallDir().replace('\\', '/');

            SFTPClient sftpClient = sshLauncher.getSFTPClient();
            SCPClient scpClient = sshLauncher.getSCPClient();
            try {
                if (!sftpClient.exists(sshInstallDir)) {
                    sftpClient.mkdirs(sshInstallDir, 0755);
                }
            }
            catch (IOException ioe) {
                logger.info(Strings.get("mkdir.failed", sshInstallDir, host));
                throw new IOException(ioe);
            }

            if (checkIfAlreadyInstalled(host, sshInstallDir))
                continue;

            //delete the sshInstallDir contents if non-empty
            try {
                //get list of file in DAS sshInstallDir
                List<String> files = getListOfInstallFiles(sshInstallDir);
                deleteRemoteFiles(sftpClient, files, sshInstallDir, getForce());
            }
            catch (IOException ex) {
                logger.finer("Failed to remove sshInstallDir contents");
                throw new IOException(ex);
            }

            String zip = zipFile.getCanonicalPath();
            try {
                logger.info("Copying " + zip + " (" + zipFile.length() + " bytes)"
                        + " to " + host + ":" + sshInstallDir);
                // Looks like we need to quote the paths to scp in case they
                // contain spaces.
                scpClient.put(zipFile.getAbsolutePath(), FileUtils.quoteString(sshInstallDir));
                logger.finer("Copied " + zip + " to " + host + ":" + sshInstallDir);
            }
            catch (IOException ex) {
                logger.info(Strings.get("cannot.copy.zip.file", zip, host));
                throw new IOException(ex);
            }

            try {
                logger.info("Installing " + getArchiveName() + " into " + host + ":" + sshInstallDir);
                String unzipCommand = "cd '" + sshInstallDir + "'; jar -xvf " + getArchiveName();
                int status = sshLauncher.runCommand(unzipCommand, outStream);
                if (status != 0) {
                    logger.info(Strings.get("jar.failed", host, outStream.toString()));
                    throw new CommandException("Remote command output: " + outStream.toString());
                }
                logger.finer("Installed " + getArchiveName() + " into " + host + ":" + sshInstallDir);
            }
            catch (IOException ioe) {
                logger.info(Strings.get("jar.failed", host, outStream.toString()));
                throw new IOException(ioe);
            }

            try {
//                String zipFileName = getArchiveName();
                logger.info("Removing " + host + ":" + sshInstallDir + "/" + getArchiveName());
                sftpClient.rm(sshInstallDir + "/" + getArchiveName());
                logger.finer("Removed " + host + ":" + sshInstallDir + "/" + getArchiveName());
            }
            catch (IOException ioe) {
                logger.info(Strings.get("remove.glassfish.failed", host, sshInstallDir));
                throw new IOException(ioe);
            }


            logger.info("Fixing file permissions of all files under " + host + ":" + sshInstallDir + "/bin");
            try {
                if (binDirFiles.isEmpty()) {
                    //binDirFiles can be empty if the archive isn't a fresh one
                    String cmd = "cd '" + sshInstallDir + "/" + SystemPropertyConstants.getComponentName() + "/bin'; chmod 0755 *";
                    int status = sshLauncher.runCommand(cmd, outStream);
                    if (status != 0) {
                        logger.info(Strings.get("jar.failed", host, outStream.toString()));
                        throw new CommandException("Remote command output: " + outStream.toString());
                    }
                }
                else {
                    for (String binDirFile : binDirFiles) {
                        sftpClient.chmod((sshInstallDir + "/" + binDirFile), 0755);
                    }
                }
                logger.finer("Fixed file permissions of all files under " + host + ":" + sshInstallDir + "/bin");
            }
            catch (IOException ioe) {
                logger.info(Strings.get("fix.permissions.failed", host, sshInstallDir));
                throw new IOException(ioe);
            }

            if (Constants.v4) {
                logger.info("Fixing file permissions for nadmin file under " + host + ":" + sshInstallDir + "/lib");
                try {
                    sftpClient.chmod((sshInstallDir + "/" + SystemPropertyConstants.getComponentName() + "/lib/nadmin"), 0755);
                    logger.finer("Fixed file permission for nadmin under " + host + ":" + sshInstallDir + "/" + SystemPropertyConstants.getComponentName() + "/lib/nadmin");
                }
                catch (IOException ioe) {
                    logger.info(Strings.get("fix.permissions.failed", host, sshInstallDir));
                    throw new IOException(ioe);
                }
            }
        }
    }

    /**
     * Determines if GlassFish is installed on remote host at specified location.
     * Uses SSH launcher to execute 'asadmin version'
     * @param host remote host
     * @return true if GlassFish install is found, false otherwise.
     * @throws CommandException
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean checkIfAlreadyInstalled(String host, String sshInstallDir) throws CommandException, IOException, InterruptedException {
        //check if an installation already exists on remote host
        boolean res = false;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            String asadmin = Constants.v4 ? "/lib/nadmin' version --local --terse" : "/bin/asadmin' version --local --terse";
            String cmd = "'" + sshInstallDir + "/" + SystemPropertyConstants.getComponentName() + asadmin;
            int status = sshLauncher.runCommand(cmd, outStream);
            if (status == 0) {
                logger.finer(host + ":'" + cmd + "'" + " returned [" + outStream.toString() + "]");
                logger.info(Strings.get("found.glassfish.install", host, sshInstallDir));
                res = true;
            }
            else {
                logger.finer(host + ":'" + cmd + "'" + " fa iled [" + outStream.toString() + "]");
            }
        }
        catch (IOException ex) {
            logger.info(Strings.get("glassfish.install.check.failed", host));
            throw new IOException(ex);
        }
        return res;
    }

    @Override
    final void precopy() throws CommandException {
    }
}
