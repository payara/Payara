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

import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.cluster.winrm.WinRMHelper;
import jakarta.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static com.sun.enterprise.admin.cli.cluster.InstallNodeSshCommand.buildExtractCommand;

@Service(name = "install-node-winrm")
@PerLookup
public class InstallNodeWinrmCommand extends InstallNodeBaseCommand {
    @Param(name = "winrmuser", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(name = "winrmpassword", optional = true, password = true)
    private String password;
    @Param(optional = true, defaultValue = "5985", name = "winrmport")
    int port;

    @Inject
    WinRMHelper winrm;

    @Override
    void copyToHosts(File zipFile, ArrayList<String> binDirFiles) throws CommandException {
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

    @Override
    void precopy() throws CommandException {
        if (getForce()) {
            return;
        }

        for (String host : hosts) {
            String winrmInstallDir = getInstallDir().replaceAll("\\\\", "/");
            winrm.init(getRemoteUser(), password, host, getRemotePort(), winrmInstallDir);

            try {
                if (winrm.exists(winrmInstallDir)) {
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    try {
                        String nadminBase = winrmInstallDir + "/" + SystemPropertyConstants.getComponentName() + "/lib/nadmin";
                        String cmd = "\"" + nadminBase + ".bat\" version --local --terse";
                        int status = winrm.executeCommand(cmd, outStream).getStatusCode();
                        if (status == 0) {
                            if (logger.isLoggable(Level.FINER))
                                logger.log(Level.FINER, "{0}:''{1}'' returned [{2}]", new Object[]{host, cmd, outStream.toString(StandardCharsets.UTF_8)});
                            throw new CommandException(Strings.get("install.dir.exists", winrmInstallDir));
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
            } catch (IOException ex) {
                throw new CommandException(ex);
            }
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
        // TODO: does this even make sense for WinRM?
        return null;
    }

    private void copyToHostsInternal (File zipFile, ArrayList<String> binDirFiles) throws CommandException, InterruptedException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (String host : hosts) {
            String winrmInstallDir = getInstallDir().replace("\\", "/");
            winrm.init(getRemoteUser(), password, host, port, winrmInstallDir);

            if (!winrm.exists(getInstallDir())) {
                winrm.makeDirectory(winrmInstallDir);
            }

            try {
                List<String> files = getListOfInstallFiles(getInstallDir());
                deleteRemoteFiles(files, winrmInstallDir, getForce());
            } catch (IOException ex) {
                logger.finer("Failed to remove winrmInstallDir contents");
                throw new IOException(ex);
            }

            String remoteZipPath = winrmInstallDir + "/" + zipFile.getName();
            try {
                logger.log(Level.INFO, "Copying {0} ({1} bytes) to {2}:{3}",
                        new Object[]{zipFile.getCanonicalPath(), zipFile.length(), host, winrmInstallDir});
                winrm.sendFile(zipFile, remoteZipPath);
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Copied {0} to {1}:{2}", new Object[]{zipFile.getCanonicalPath(), host, winrmInstallDir});
                }
            } catch (IOException ex) {
                logger.info(Strings.get("cannot.copy.zip.file", zipFile.getCanonicalPath(), host));
                throw new IOException(ex);
            }

            try {
                logger.log(Level.INFO, "Installing {0} into {1}:{2}", new Object[]{getArchiveName(), host, winrmInstallDir});
                String unzipCommand = buildExtractCommand(winrmInstallDir, getArchiveName(), true);
                int status = winrm.executeCommand(unzipCommand, outputStream).getStatusCode();
                if (status != 0) {
                    String outStreamToString = outputStream.toString(StandardCharsets.UTF_8);
                    logger.info(Strings.get("jar.failed", host, outStreamToString));
                    throw new CommandException("Remote command output: " + outStreamToString);
                }
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Installed {0} into {1}:{2}", new Object[]{getArchiveName(), host, winrmInstallDir});
                }
            } catch (IOException ioe) {
                logger.info(Strings.get("jar.failed", host, outputStream.toString(StandardCharsets.UTF_8)));
                throw new IOException(ioe);
            }

            logger.log(Level.INFO, "Removing {0}:{1}/{2}", new Object[]{host, winrmInstallDir, getArchiveName()});
            winrm.remove(winrmInstallDir + "/" + getArchiveName());
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Removed {0}:{1}/{2}", new Object[]{host, winrmInstallDir, getArchiveName()});
            }

//            logger.log(Level.INFO, "Fixing file permissions of all bin files under {0}:{1}", new Object[]{host, winrmInstallDir});
//            try {
//                if (binDirFiles.isEmpty()) {
//                    searchAndFixBinDirectoryFiles(winrmInstallDir, sftpClient);
//                } else {
//                    for (String binDirFile : binDirFiles) {
//                        sftpClient.chmod((winrmInstallDir + "/" + binDirFile), 0755);
//                    }
//                }
//                if (logger.isLoggable(Level.FINER))
//                    logger.log(Level.FINER, "Fixed file permissions of all bin files under {0}:{1}", new Object[]{host, winrmInstallDir});
//            } catch (IOException ioe) {
//                logger.info(Strings.get("fix.permissions.failed", host, winrmInstallDir));
//                throw new IOException(ioe);
//            }
//
//            String componentName = SystemPropertyConstants.getComponentName();
//            logger.log(Level.INFO, "Fixing file permissions for nadmin file under {0}:{1}/{2}/lib",
//                    new Object[]{host, winrmInstallDir, componentName});
//            try {
//                sftpClient.chmod(winrmInstallDir + "/" + componentName + "/lib/nadmin", 0755);
//                if (logger.isLoggable(Level.FINER)) {
//                    logger.log(Level.FINER, "Fixed file permission for nadmin under {0}:{1}/{2}/lib/nadmin",
//                            new Object[]{host, winrmInstallDir, componentName});
//                }
//            } catch (IOException ioe) {
//                logger.info(Strings.get("fix.permissions.failed", host, winrmInstallDir));
//                throw new IOException(ioe);
//            }
        }
    }

    private void deleteRemoteFiles (List<String> dasFiles, String dir, boolean force) throws IOException {
        for (WinRMHelper.PathEntry entry : winrm.ls(dir)) {
            if (entry.getFilename().equals(".") || entry.getFilename().equals("..") || entry.getFilename().equals("nodes")) {
                continue;
            }

            if (entry.isDirectory()) {
                String dirPath = entry.getPath();
                deleteRemoteFiles(dasFiles, dirPath, force);
                if (force) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Force removing directory " + dirPath);
                    }
                    if (winrm.isDirectoryEmpty(dirPath)) {
                        winrm.remove(dirPath);
                    }
                } else {
                    if (dasFiles.contains(dirPath) && winrm.isDirectoryEmpty(dirPath)) {
                        winrm.remove(dirPath);
                    }
                }
            } else {
                String filePath = entry.getPath();
                if (force) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Force removing file " + filePath);
                    }
                    winrm.remove(filePath);
                } else {
                    if (dasFiles.contains(filePath)) {
                        winrm.remove(filePath);
                    }
                }
            }
        }
    }
}
