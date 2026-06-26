/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright 2026 Payara Foundation and/or affiliates

package com.sun.enterprise.admin.cli.cluster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.cluster.ssh.connect.NodeRunnerSsh;
import org.glassfish.cluster.ssh.launcher.SSHAuthenticationException;
import org.glassfish.cluster.ssh.launcher.SSHConnection;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.cluster.ssh.util.SSHUtil;
import jakarta.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 *
 * @author Byron Nevins
 */
@Service(name = "uninstall-node-ssh")
@PerLookup
public class UninstallNodeSshCommand extends UninstallNodeBaseCommand {
    @Param(name = "sshuser", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(optional = true, defaultValue = "22", name = "sshport")
    private int port;
    @Param(optional = true)
    private String sshkeyfile;
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
        if (sshkeyfile != null && SSHUtil.isEncryptedKey(sshkeyfile)) {
            sshkeypassphrase = getSSHPassphrase(true);
        }
    }

    /**
     * Opens an SSH connection to the already-init'd host.
     * If key auth is rejected, prompts for password and retries once.
     * GeneralSecurityException propagates to the caller's catch-all in deleteFromHosts().
     */
    private SSHConnection openConnectionWithPasswordFallback(String host)
            throws IOException, GeneralSecurityException, CommandException {
        try {
            return sshLauncher.openConnection();
        } catch (SSHAuthenticationException ex) {
            sshpassword = getSSHPassword(host);
            sshLauncher.init(getRemoteUser(), host, getRemotePort(),
                    sshpassword, sshkeyfile, sshkeypassphrase, logger);
            return sshLauncher.openConnection();
        }
    }

    @Override
    void deleteFromHosts() throws CommandException {
        try {
            List<String> dasFiles = getListOfInstallFiles(getInstallDir());
            String installDir = SFTPClient.normalizePath(getInstallDir());

            for (String host : hosts) {
                sshLauncher.init(getRemoteUser(), host, getRemotePort(),
                        sshpassword, sshkeyfile, sshkeypassphrase, logger);

                try (SSHConnection conn = openConnectionWithPasswordFallback(host);
                     SFTPClient sftpClient = conn.openSftp()) {

                    if (!sftpClient.exists(installDir)) {
                        throw new IOException(installDir + " Directory does not exist");
                    }

                    if (NodeRunnerSsh.isWindowsInstallDir(installDir)) {
                        deleteWindowsInstallDir(conn, installDir, getForce());
                    } else {
                        deleteRemoteFiles(sftpClient, dasFiles, installDir, getForce());
                        if (getForce()) {
                            String nodesDir = installDir + "/nodes";
                            if (sftpClient.exists(nodesDir)) {
                                deleteRemoteFiles(sftpClient, Collections.emptyList(), nodesDir, true);
                                if (isRemoteDirectoryEmpty(sftpClient, nodesDir)) {
                                    sftpClient.rmdir(nodesDir);
                                }
                            }
                        }
                        if (isRemoteDirectoryEmpty(sftpClient, installDir)) {
                            sftpClient.rmdir(installDir);
                        }
                    }
                }
            }
        } catch (CommandException ce) {
            throw ce;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CommandException(ex);
        } catch (Exception ex) {
            throw new CommandException(ex);
        }
    }

    /**
     * Deletes the Payara installation on a Windows node using PowerShell
     * {@code Remove-Item -Recurse -Force}, which handles read-only and hidden files
     * that SFTP REMOVE cannot delete via a network logon.
     *
     * <ul>
     *   <li><b>Without {@code --force}</b>: deletes every entry in {@code installDir}
     *       except {@code nodes/}, leaving instance configuration intact.
     *   <li><b>With {@code --force}</b>: deletes the entire {@code installDir} tree
     *       (including {@code nodes/}) and then removes {@code installDir} itself.
     * </ul>
     */
    void deleteWindowsInstallDir(SSHConnection conn, String installDir, boolean force)
            throws IOException, InterruptedException {
        String escapedPath = installDir.replace("'", "''");
        String psCmd = force
                ? "Remove-Item -Recurse -Force '" + escapedPath + "'"
                : "Get-ChildItem -Path '" + escapedPath + "' -Exclude 'nodes'"
                        + " | Remove-Item -Recurse -Force";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exitCode = conn.runCommand(
                List.of("cmd.exe", "/c", "powershell", "-NonInteractive", "-Command", psCmd),
                out, null);
        checkDeleteExitCode(exitCode, installDir,
                out.toString(StandardCharsets.UTF_8));
    }

    static void checkDeleteExitCode(int exitCode, String installDir, String output)
            throws IOException {
        if (exitCode != 0) {
            throw new IOException("Delete of " + installDir + " exited " + exitCode
                    + ": " + output.trim());
        }
    }
}
