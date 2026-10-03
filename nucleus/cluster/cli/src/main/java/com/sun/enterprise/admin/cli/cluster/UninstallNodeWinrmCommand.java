/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

import fish.payara.cluster.winrm.WinRMHelper;
import jakarta.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.sun.enterprise.admin.cli.cluster.UninstallNodeSshCommand.checkDeleteExitCode;

@Service(name = "uninstall-node-winrm")
@PerLookup
public class UninstallNodeWinrmCommand extends UninstallNodeBaseCommand {
    @Param(name = "winrmuser", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(name = "winrmpassword", optional = true)
    private String password;
    @Param(optional = true, defaultValue = "5985", name = "winrmport")
    private int port;

    @Inject
    private WinRMHelper winrm;

    @Override
    void deleteFromHosts() throws CommandException {
        try {
            String installDir = SFTPClient.normalizePath(getInstallDir());

            for (String host : hosts) {
                winrm.init(getRemoteUser(), password, host, getRemotePort(), getInstallDir());

                if (!winrm.dirExists(installDir)) {
                    throw new IOException(installDir + " Directory does not exist");
                }

                String escapedPath = installDir.replace("'", "''");
                String psCmd = getForce()
                        ? "Remove-Item -Recurse -Force '" + escapedPath + "'"
                        : "Get-ChildItem -Path '" + escapedPath + "' -Exclude 'nodes'"
                          + " | Remove-Item -Recurse -Force";

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int exitCode = winrm.executePowershell(psCmd, out).getStatusCode();
                checkDeleteExitCode(exitCode, installDir, out.toString(StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            throw new CommandException(ex);
        }
    }

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
        return null;
    }
}
