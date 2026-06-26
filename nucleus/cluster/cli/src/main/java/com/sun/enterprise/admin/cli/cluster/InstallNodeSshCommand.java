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
// Portions Copyright 2019-2026 Payara Foundation and/or affiliates

package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.util.SystemPropertyConstants;
import jakarta.inject.Inject;
import org.apache.sshd.sftp.client.SftpClient;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.cluster.ssh.connect.NodeRunnerSsh;
import org.glassfish.cluster.ssh.launcher.SSHAuthenticationException;
import org.glassfish.cluster.ssh.launcher.SSHConnection;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.cluster.ssh.util.SSHUtil;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Byron Nevins
 */
@Service(name = "install-node-ssh")
@PerLookup
public class InstallNodeSshCommand extends InstallNodeBaseCommand {
    @Param(name = "sshuser", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(optional = true, defaultValue = "22", name = "sshport")
    int port;
    @Param(optional = true)
    String sshkeyfile;
    @Inject
    private SSHLauncher sshLauncher;
    //storing password to prevent prompting twice
    private Map<String, char[]> sshPasswords = new HashMap<>();

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
            } else {
                sshkeyfile = existingKey;
            }
        } else {
            validateKey(sshkeyfile);
        }

        //we need the key passphrase if key is encrypted
        if (sshkeyfile != null && SSHUtil.isEncryptedKey(sshkeyfile)) {
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
        } catch (CommandException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CommandException(ex);
        } catch (IOException ex) {
            throw new CommandException(ex);
        }
    }

    private void copyToHostsInternal(File zipFile, ArrayList<String> binDirFiles) throws IOException, InterruptedException, CommandException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        for (String host : hosts) {
            outStream.reset();
            sshLauncher.init(getRemoteUser(), host, getRemotePort(), sshpassword, getSshKeyFile(), sshkeypassphrase, logger);

            String sshInstallDir = getInstallDir().replace('\\', '/');
            boolean remoteWindows = NodeRunnerSsh.isWindowsInstallDir(sshInstallDir);

            // One connection shared for all SFTP and exec operations on this host.
            try (SSHConnection conn = openConnectionWithPasswordFallback(host);
                 SFTPClient sftpClient = conn.openSftp()) {

                try {
                    if (!sftpClient.exists(sshInstallDir)) {
                        sftpClient.mkdirs(sshInstallDir, 0755);
                    }
                } catch (IOException ioe) {
                    logger.info(Strings.get("mkdir.failed", sshInstallDir, host));
                    throw new IOException(ioe);
                }

                try {
                    List<String> files = getListOfInstallFiles(sshInstallDir);
                    deleteRemoteFiles(sftpClient, files, sshInstallDir, getForce());
                } catch (IOException ex) {
                    logger.finer("Failed to remove sshInstallDir contents");
                    throw new IOException(ex);
                }

                String remoteZipPath = sshInstallDir + "/" + zipFile.getName();
                try {
                    logger.log(Level.INFO, "Copying {0} ({1} bytes) to {2}:{3}",
                            new Object[]{zipFile.getCanonicalPath(), zipFile.length(), host, sshInstallDir});
                    try (OutputStream out = sftpClient.writeToFile(remoteZipPath); FileInputStream fis = new FileInputStream(zipFile)) {
                        byte[] buf = new byte[32768];
                        int len;
                        while ((len = fis.read(buf)) >= 0) {
                            out.write(buf, 0, len);
                        }
                    }
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "Copied {0} to {1}:{2}", new Object[]{zipFile.getCanonicalPath(), host, sshInstallDir});
                    }
                } catch (IOException ex) {
                    logger.info(Strings.get("cannot.copy.zip.file", zipFile.getCanonicalPath(), host));
                    throw new IOException(ex);
                }

                try {
                    logger.log(Level.INFO, "Installing {0} into {1}:{2}", new Object[]{getArchiveName(), host, sshInstallDir});
                    String unzipCommand = buildExtractCommand(sshInstallDir, getArchiveName(), remoteWindows);
                    int status = conn.runCommand(unzipCommand, outStream);
                    if (status != 0) {
                        String outStreamToString = outStream.toString(StandardCharsets.UTF_8);
                        logger.info(Strings.get("jar.failed", host, outStreamToString));
                        throw new CommandException("Remote command output: " + outStreamToString);
                    }
                    if (logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER, "Installed {0} into {1}:{2}", new Object[]{getArchiveName(), host, sshInstallDir});
                } catch (IOException ioe) {
                    logger.info(Strings.get("jar.failed", host, outStream.toString(StandardCharsets.UTF_8)));
                    throw new IOException(ioe);
                }

                try {
                    logger.log(Level.INFO, "Removing {0}:{1}/{2}", new Object[]{host, sshInstallDir, getArchiveName()});
                    sftpClient.rm(sshInstallDir + "/" + getArchiveName());
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "Removed {0}:{1}/{2}", new Object[]{host, sshInstallDir, getArchiveName()});
                    }
                } catch (IOException ioe) {
                    logger.info(Strings.get("remove.glassfish.failed", host, sshInstallDir));
                    throw new IOException(ioe);
                }

                logger.log(Level.INFO, "Fixing file permissions of all bin files under {0}:{1}", new Object[]{host, sshInstallDir});
                try {
                    if (binDirFiles.isEmpty()) {
                        searchAndFixBinDirectoryFiles(sshInstallDir, sftpClient);
                    } else {
                        for (String binDirFile : binDirFiles) {
                            sftpClient.chmod((sshInstallDir + "/" + binDirFile), 0755);
                        }
                    }
                    if (logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER, "Fixed file permissions of all bin files under {0}:{1}", new Object[]{host, sshInstallDir});
                } catch (IOException ioe) {
                    logger.info(Strings.get("fix.permissions.failed", host, sshInstallDir));
                    throw new IOException(ioe);
                }

                String componentName = SystemPropertyConstants.getComponentName();
                logger.log(Level.INFO, "Fixing file permissions for nadmin file under {0}:{1}/{2}/lib",
                        new Object[]{host, sshInstallDir, componentName});
                try {
                    sftpClient.chmod(sshInstallDir + "/" + componentName + "/lib/nadmin", 0755);
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "Fixed file permission for nadmin under {0}:{1}/{2}/lib/nadmin",
                                new Object[]{host, sshInstallDir, componentName});
                    }
                } catch (IOException ioe) {
                    logger.info(Strings.get("fix.permissions.failed", host, sshInstallDir));
                    throw new IOException(ioe);
                }
            }
        }
    }

    /**
     * Recursively list install dir and identify "bin" directory. Change permissions
     * of files under "bin" directory.
     */
    private void searchAndFixBinDirectoryFiles(String installDir, SFTPClient sftpClient) throws IOException {
        for (SftpClient.DirEntry directoryEntry : sftpClient.ls(installDir)) {
            if (directoryEntry.getFilename().equals(".") || directoryEntry.getFilename().equals("..")) {
                continue;
            } else if (directoryEntry.getAttributes().isDirectory()) {
                String subDir = installDir + "/" + directoryEntry.getFilename();
                if (directoryEntry.getFilename().equals("bin")) {
                    fixAllFiles(subDir, sftpClient);
                } else {
                    searchAndFixBinDirectoryFiles(subDir, sftpClient);
                }
            }
        }
    }

    /**
     * Set permissions of all files under specified directory.
     */
    private void fixAllFiles(String binDir, SFTPClient sftpClient) throws IOException {
        for (SftpClient.DirEntry directoryEntry : sftpClient.ls(binDir)) {
            if (directoryEntry.getFilename().equals(".") || directoryEntry.getFilename().equals("..")) continue;
            String fName = binDir + "/" + directoryEntry.getFilename();
            sftpClient.chmod(fName, 0755);
        }
    }

    /**
     * Opens a shared SSH connection to the already-init'd host.
     * If key auth is rejected, prompts for (or retrieves cached) password and retries once.
     */
    private SSHConnection openConnectionWithPasswordFallback(String host) throws CommandException, IOException {
        try {
            return sshLauncher.openConnection();
        } catch (SSHAuthenticationException ex) {
            String password;
            if (sshPasswords.containsKey(host)) {
                password = String.valueOf(sshPasswords.get(host));
            } else {
                password = getSSHPassword(host);
                sshPasswords.put(host, password.toCharArray());
            }
            sshLauncher.init(getRemoteUser(), host, getRemotePort(), password, getSshKeyFile(), sshkeypassphrase, logger);
            return sshLauncher.openConnection();
        }
    }

    /**
     * Builds a shell command that changes to {@code installDir} and then runs
     * {@code jar -xvf archiveName}, using syntax appropriate for the remote OS.
     *
     * <ul>
     *   <li>Windows cmd.exe: {@code cd /d "path" && jar -xvf name}
     *       — {@code /d} allows changing across drive letters; {@code &&} is the
     *       Windows sequential-execution operator; double quotes handle spaces.
     *   <li>Unix: {@code cd 'path'; jar -xvf name}
     * </ul>
     */
    static String buildExtractCommand(String installDir, String archiveName, boolean remoteWindows) {
        if (remoteWindows) {
            // cmd.exe doubles a quote to escape it inside a double-quoted string.
            // Windows paths cannot legally contain '"', but escape defensively.
            String dirQ = installDir.replace("\"", "\"\"");
            String archiveQ = archiveName.replace("\"", "\"\"");
            return "cd /d \"" + dirQ + "\" && jar -xvf \"" + archiveQ + "\"";
        }
        // Unix single-quote: escape an embedded ' as '\''
        String dirQ = installDir.replace("'", "'\\''");
        String archiveQ = archiveName.replace("'", "'\\''");
        return "cd '" + dirQ + "'; jar -xvf '" + archiveQ + "'";
    }

    /**
     * Determines if GlassFish is installed on remote host at specified location.
     */
    private void checkIfAlreadyInstalled(String host, String sshInstallDir, SSHConnection conn,
            boolean remoteWindows) throws CommandException, IOException, InterruptedException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            String nadminBase = sshInstallDir + "/" + SystemPropertyConstants.getComponentName() + "/lib/nadmin";
            String cmd = remoteWindows
                    ? "\"" + nadminBase + ".bat\" version --local --terse"
                    : "'" + nadminBase + "' version --local --terse";
            int status = conn.runCommand(cmd, outStream);
            if (status == 0) {
                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "{0}:''{1}'' returned [{2}]", new Object[]{host, cmd, outStream.toString(StandardCharsets.UTF_8)});
                throw new CommandException(Strings.get("install.dir.exists", sshInstallDir));
            } else {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "{0}:''{1}'' failed [{2}]", new Object[]{host, cmd, outStream.toString(StandardCharsets.UTF_8)});
                }
            }
        } catch (IOException ex) {
            logger.info(Strings.get("glassfish.install.check.failed", host));
            throw new IOException(ex);
        }
    }

    @Override
    final void precopy() throws CommandException {
        if (getForce()) {
            return;
        }

        for (String host : hosts) {
            sshLauncher.init(getRemoteUser(), host, getRemotePort(), sshpassword, getSshKeyFile(), sshkeypassphrase, logger);

            String sshInstallDir = getInstallDir().replaceAll("\\\\", "/");

            try (SSHConnection conn = openConnectionWithPasswordFallback(host);
                 SFTPClient sftpClient = conn.openSftp()) {
                if (sftpClient.exists(sshInstallDir)) {
                    checkIfAlreadyInstalled(host, sshInstallDir, conn, NodeRunnerSsh.isWindowsInstallDir(sshInstallDir));
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new CommandException(ex);
            } catch (IOException ex) {
                throw new CommandException(ex);
            }
        }
    }
}
