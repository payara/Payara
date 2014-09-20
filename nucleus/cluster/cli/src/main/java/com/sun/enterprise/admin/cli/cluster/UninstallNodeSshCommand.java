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

import java.io.IOException;
import java.util.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.cluster.ssh.util.SSHUtil;
import javax.inject.Inject;

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

    @Override
    void deleteFromHosts() throws CommandException {
        SFTPClient sftpClient = null;
        try {
            List<String> files = getListOfInstallFiles(getInstallDir());

            for (String host : hosts) {
                sshLauncher.init(getRemoteUser(), host, getRemotePort(), sshpassword, sshkeyfile, sshkeypassphrase, logger);

                if (sshkeyfile != null && !sshLauncher.checkConnection()) {
                    //key auth failed, so use password auth
                    promptPass = true;
                }

                if (promptPass) {
                    sshpassword = getSSHPassword(host);
                    //re-initialize
                    sshLauncher.init(getRemoteUser(), host, getRemotePort(), sshpassword, sshkeyfile, sshkeypassphrase, logger);
                }

                sftpClient = sshLauncher.getSFTPClient();

                if (!sftpClient.exists(getInstallDir())) {
                    throw new IOException(getInstallDir() + " Directory does not exist");
                }

                deleteRemoteFiles(sftpClient, files, getInstallDir(), getForce());

                if (isRemoteDirectoryEmpty(sftpClient, getInstallDir())) {
                    sftpClient.rmdir(getInstallDir());
                }
                sftpClient.close();
            }
        }
        catch (CommandException ce) {
            throw ce;
        }
        catch (Exception ex) {
            throw new CommandException(ex);
        } finally {
            if (sftpClient != null) {
                sftpClient.close();
            }
        }
    }
}
