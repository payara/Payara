/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.universal.process.WindowsException;
import com.sun.enterprise.util.io.WindowsRemoteFile;
import com.sun.enterprise.util.io.WindowsRemoteFileSystem;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.cluster.ssh.util.SSHUtil;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.internal.api.Globals;

import java.io.IOException;
import java.util.List;

/**
 * @author Rajiv Mordani
 */
@Service(name = "uninstall-node")
@Scoped(PerLookup.class)
public class UninstallNodeCommand extends NativeRemoteCommandsBase {
    @Param(name = "installdir", optional = true, defaultValue = "${com.sun.aas.productRoot}")
    private String installDir;
    @Param(optional = true, defaultValue = "false")
    private boolean force;
    @Param(optional = true, defaultValue = "SSH")
    private String type;
    @Inject
    private Habitat habitat;
    @Inject
    SSHLauncher sshLauncher;

    @Override
    protected void validate() throws CommandException {
        Globals.setDefaultHabitat(habitat);
        installDir = resolver.resolve(installDir);
        if (!force) {
            for (String host : hosts) {
                if (checkIfNodeExistsForHost(host, installDir)) {
                    throw new CommandException(Strings.get("call.delete.node.ssh", host));
                }
            }
        }
        sshuser = resolver.resolve(sshuser);
        if ("DCOM".equals(type)) {
        }
        else if (sshkeyfile == null) {
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
    protected int executeCommand() throws CommandException {

        try {
            deleteFromHosts();
        }
        catch (IOException ioe) {
            throw new CommandException(ioe);
        }
        catch (InterruptedException e) {
            throw new CommandException(e);
        }
        catch (WindowsException e) {
            throw new CommandException(e);
        }

        return SUCCESS;
    }

    private void deleteFromHosts() throws CommandException, IOException, InterruptedException, WindowsException {
        if ("SSH".equals(type))
            deleteFromHostsSsh();
        else if ("DCOM".equals(type))
            deleteFromHostsDcom();
    }

    private void deleteFromHostsSsh() throws CommandException, IOException, InterruptedException {

        List<String> files = getListOfInstallFiles(installDir);

        for (String host : hosts) {
            sshLauncher.init(sshuser, host, sshport, sshpassword, sshkeyfile, sshkeypassphrase, logger);

            if (sshkeyfile != null && !sshLauncher.checkConnection()) {
                //key auth failed, so use password auth
                promptPass = true;
            }

            if (promptPass) {
                sshpassword = getSSHPassword(host);
                //re-initialize
                sshLauncher.init(sshuser, host, sshport, sshpassword, sshkeyfile, sshkeypassphrase, logger);
            }

            SFTPClient sftpClient = sshLauncher.getSFTPClient();

            if (!sftpClient.exists(installDir)) {
                throw new IOException(installDir + " Directory does not exist");
            }

            deleteRemoteFiles(sftpClient, files, installDir, force);

            if (sftpClient.ls(installDir).isEmpty()) {
                sftpClient.rmdir(installDir);
            }
        }
    }

    private void deleteFromHostsDcom() throws WindowsException, IOException, CommandException {
        for (String host : hosts) {
            String pw = getDCOMPassword(host);
            WindowsRemoteFileSystem wrfs = new WindowsRemoteFileSystem(host, sshuser, pw);
            WindowsRemoteFile remoteInstallDir = new WindowsRemoteFile(wrfs, installDir);

            if (!remoteInstallDir.exists()) {
                throw new IOException(Strings.get("remote.install.dir.already.gone", installDir));
            }
            remoteInstallDir.delete();

           // make sure it's gone now...
            if (remoteInstallDir.exists()) {
                throw new IOException(Strings.get("remote.install.dir.cant.delete", installDir));
            }
        }
    }
}
