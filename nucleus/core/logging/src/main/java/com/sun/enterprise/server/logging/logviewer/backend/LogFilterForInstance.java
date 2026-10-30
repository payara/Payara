/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright 2017-2026 Payara Foundation and/or affiliates
 */

package com.sun.enterprise.server.logging.logviewer.backend;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import org.apache.sshd.sftp.client.SftpClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.glassfish.cluster.ssh.launcher.SSHConnection;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * @author Naman Mehta, 6 Aug, 2010
 */
public class LogFilterForInstance {

    public File downloadGivenInstanceLogFile(ServiceLocator habitat, Server targetServer, Domain domain, Logger logger,
                                             String instanceName, String domainRoot, String logFileName, String instanceLogFileName)
            throws IOException {

        File instanceLogFile = null;

        SSHLauncher sshL = getSSHL(habitat);
        String sNode = targetServer.getNodeRef();
        Nodes nodes = domain.getNodes();
        Node node = nodes.getNode(sNode);

        if (node.getType().equals("SSH")) {

            sshL.init(node, logger);

            try (SFTPClient sftpClient = sshL.getSFTPClient()) {
                File logFileDirectoryOnServer = makingDirectory(domainRoot + File.separator + "logs"
                        + File.separator + instanceName);

                boolean noFileFound = true;

                String loggingDir = getLoggingDirectoryForNode(instanceLogFileName, node, sNode, instanceName);
                logger.fine("[LogFilter] downloadGivenInstanceLogFile: instanceLogFileName=" + instanceLogFileName
                        + " loggingDir=" + loggingDir + " logFileName=" + logFileName
                        + " nodeDir=" + node.getNodeDir() + " installDir=" + node.getInstallDir());

                try {
                    List<SftpClient.DirEntry> instanceLogFileNames = sftpClient.ls(loggingDir);
                    logger.fine("[LogFilter] ls(" + loggingDir + ") returned " + instanceLogFileNames.size() + " entries");

                    for (SftpClient.DirEntry file : instanceLogFileNames) {
                        String fileName = file.getFilename();
                        if (!file.getAttributes().isDirectory() && !fileName.equals(".") && !fileName.equals("..")
                                && fileName.contains(".log") && !fileName.contains(".log.")) {
                            noFileFound = false;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // if directory doesn't present or missing on remote machine. It happens due to bug 16451
                    logger.warning("[LogFilter] ls(" + loggingDir + ") FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    noFileFound = true;
                }

                if (noFileFound) {
                    loggingDir = getLoggingDirectoryForNodeWhenNoFilesFound(instanceLogFileName, node, sNode, instanceName);
                    logger.fine("[LogFilter] noFileFound=true, fallback loggingDir=" + loggingDir);
                }

                String loggingFile = loggingDir + "/" + logFileName;
                logger.fine("[LogFilter] checking loggingFile=" + loggingFile);
                if (!sftpClient.exists(loggingFile)) {
                    loggingFile = loggingDir + "/server.log";
                    logger.fine("[LogFilter] original not found, trying server.log: " + loggingFile);
                }

                // creating local file name on DAS
                long instanceLogFileSize = 0;
                instanceLogFile = new File(logFileDirectoryOnServer.getAbsolutePath() + File.separator
                        + loggingFile.substring(loggingFile.lastIndexOf('/') + 1));

                // getting size of the file on DAS
                if (instanceLogFile.exists()) {
                    instanceLogFileSize = instanceLogFile.length();
                }

                SftpClient.Attributes remoteAttrs = sftpClient._stat(loggingFile);

                if (remoteAttrs == null) {
                    logger.warning("[LogFilter] Remote log file not found on instance node: " + loggingFile);
                } else {
                    // getting size of the file on instance machine
                    long fileSizeOnNode = remoteAttrs.getSize();

                    // if differ both size then downloading
                    if (instanceLogFileSize != fileSizeOnNode) {
                        try (BufferedInputStream in = new BufferedInputStream(sftpClient.read(loggingFile));
                             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(instanceLogFile))) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = in.read(buf)) >= 0) {
                                out.write(buf, 0, len);
                            }
                        }
                    }
                }
            }
        }

        return instanceLogFile;

    }

    public void downloadAllInstanceLogFiles(ServiceLocator habitat, Server targetServer,
            Domain domain, Logger logger, String instanceName,
            String tempDirectoryOnServer, String instanceLogFileDirectory)
            throws IOException {

        SSHLauncher sshL = getSSHL(habitat);
        String sNode = targetServer.getNodeRef();
        Nodes nodes = domain.getNodes();
        Node node = nodes.getNode(sNode);

        if (node.getType().equals("SSH")) {
            sshL.init(node, logger);

            try (SSHConnection conn = sshL.openConnection();
                 SFTPClient sftpClient = conn.openSftp()) {

                // Determine source directory, falling back when no .log files are present
                String sourceDir = getLoggingDirectoryForNode(
                        instanceLogFileDirectory, node, sNode, instanceName);

                List<String> fileNames = new ArrayList<>();
                boolean noFileFound = true;

                try {
                    for (SftpClient.DirEntry entry : sftpClient.ls(sourceDir)) {
                        String fileName = entry.getFilename();
                        if (!entry.getAttributes().isDirectory()
                                && !fileName.equals(".") && !fileName.equals("..")
                                && fileName.contains(".log") && !fileName.contains(".log.")) {
                            fileNames.add(fileName);
                            noFileFound = false;
                        }
                    }
                } catch (IOException ex) {
                    noFileFound = true;
                }

                if (noFileFound) {
                    sourceDir = getLoggingDirectoryForNodeWhenNoFilesFound(
                            instanceLogFileDirectory, node, sNode, instanceName);
                    try {
                        for (SftpClient.DirEntry entry : sftpClient.ls(sourceDir)) {
                            String fileName = entry.getFilename();
                            if (!entry.getAttributes().isDirectory()
                                    && !fileName.equals(".") && !fileName.equals("..")
                                    && fileName.contains(".log") && !fileName.contains(".log.")) {
                                fileNames.add(fileName);
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }

                for (String remoteFileName : fileNames) {
                    String remoteFilePath = sourceDir + "/" + remoteFileName;
                    File localFile = new File(tempDirectoryOnServer, remoteFileName);
                    try (InputStream in = sftpClient.read(remoteFilePath);
                         BufferedOutputStream out = new BufferedOutputStream(
                                 new FileOutputStream(localFile))) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = in.read(buf)) >= 0) {
                            out.write(buf, 0, len);
                        }
                    } catch (IOException ex) {
                        logger.warning("Failed to download log file "
                                + remoteFilePath + ": " + ex.getMessage());
                    }
                }
            }
        }
    }

    public List<String> getInstanceLogFileNames(ServiceLocator habitat, Server targetServer, Domain domain, Logger logger,
                                          String instanceName, String instanceLogFileDetails) throws IOException {

        // helper method to get all log file names for given instance
        String sNode = targetServer.getNodeRef();
        Node node = domain.getNodes().getNode(sNode);
        List<String> instanceLogFileNamesAsString = new ArrayList<>();

        // this code is used when DAS and instances are running on the same machine
        if (node.isLocal()) {
            String loggingDir = getLoggingDirectoryForNode(instanceLogFileDetails, node, sNode, instanceName);

            File logsDir = new File(loggingDir);
            File allLogFileNames[] = logsDir.listFiles();

            boolean noFileFound = true;

            if (allLogFileNames != null) {
                for (File file: allLogFileNames) {
                    String fileName = file.getName();
                    if (file.isFile() && !fileName.equals(".") && !fileName.equals("..") && fileName.contains(".log")
                            && !fileName.contains(".log.")) {
                        instanceLogFileNamesAsString.add(fileName);
                        noFileFound = false;
                    }
                }
            }

            if (noFileFound) {
                loggingDir = getLoggingDirectoryForNodeWhenNoFilesFound(instanceLogFileDetails, node, sNode, instanceName);
                logsDir = new File(loggingDir);
                allLogFileNames = logsDir.listFiles();
                if (allLogFileNames != null) {
                    for (File file: allLogFileNames) {
                        String fileName = file.getName();
                        if (file.isFile() && !fileName.equals(".") && !fileName.equals("..") && fileName.contains(".log")
                                && !fileName.contains(".log.")) {
                            instanceLogFileNamesAsString.add(fileName);
                        }
                    }
                }
            }
        } else if (node.getType().equals("SSH")) {
            SSHLauncher sshL = getSSHL(habitat);
            sshL.init(node, logger);
            try (SFTPClient sftpClient = sshL.getSFTPClient()) {
                boolean noFileFound = true;

                String loggingDir = getLoggingDirectoryForNode(instanceLogFileDetails, node, sNode, instanceName);
                logger.fine("[LogFilter] getInstanceLogFileNames: loggingDir=" + loggingDir
                        + " instanceLogFileDetails=" + instanceLogFileDetails
                        + " nodeDir=" + node.getNodeDir() + " installDir=" + node.getInstallDir());

                try {
                    List<SftpClient.DirEntry> instanceLogFileNames = sftpClient.ls(loggingDir);
                    logger.fine("[LogFilter] ls(" + loggingDir + ") returned " + instanceLogFileNames.size() + " entries");
                    for (SftpClient.DirEntry file : instanceLogFileNames) {
                        String fileName = file.getFilename();
                        logger.fine("[LogFilter] entry: " + fileName + " isDir=" + file.getAttributes().isDirectory());
                        if (!file.getAttributes().isDirectory() && !fileName.equals(".") && !fileName.equals("..")
                                && fileName.contains(".log") && !fileName.contains(".log.")) {
                            instanceLogFileNamesAsString.add(fileName);
                            noFileFound = false;
                        }
                    }
                } catch (Exception ex) {
                    // if directory doesn't present or missing on remote machine. It happens due to bug 16451
                    logger.warning("[LogFilter] ls(" + loggingDir + ") FAILED: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    noFileFound = true;
                }

                if (noFileFound) {
                    loggingDir = getLoggingDirectoryForNodeWhenNoFilesFound(instanceLogFileDetails, node, sNode, instanceName);
                    logger.fine("[LogFilter] fallback loggingDir=" + loggingDir);
                    try {
                        List<SftpClient.DirEntry> instanceLogFileNames = sftpClient.ls(loggingDir);
                        logger.fine("[LogFilter] fallback ls returned " + instanceLogFileNames.size() + " entries");
                        for (SftpClient.DirEntry file : instanceLogFileNames) {
                            String fileName = file.getFilename();
                            if (!file.getAttributes().isDirectory() && !fileName.equals(".") && !fileName.equals("..")
                                    && fileName.contains(".log") && !fileName.contains(".log.")) {
                                instanceLogFileNamesAsString.add(fileName);
                            }
                        }
                    } catch (Exception ex) {
                        logger.warning("[LogFilter] fallback ls FAILED: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    }
                }
            }


        }

        return instanceLogFileNamesAsString;
    }

    private SSHLauncher getSSHL(ServiceLocator habitat) {
        return habitat.getService(SSHLauncher.class);
    }

    private File makingDirectory(String path) {
        File targetDir = new File(path);
        boolean created = false;
        boolean deleted = false;
        if (targetDir.exists()) {
            deleted = targetDir.delete();
            if (!deleted) {
                return targetDir;
            }

        }
        created = targetDir.mkdir();
        if (!created) {
            return null;
        } else {
            return targetDir;
        }

    }

    public String getLoggingDirectoryForNode(String instanceLogFileDirectory, Node node, String sNode, String instanceName) {
        String loggingDir = "";

        if (instanceLogFileDirectory.contains("${com.sun.aas.instanceRoot}/logs") && node.getNodeDirAbsoluteUnixStyle() != null) {
            // Use getNodeDirAbsoluteUnixStyle() so that a relative nodeDir (e.g. "nodes")
            // is resolved to the full path (e.g. "C:/payara7/glassfish/nodes") before being
            // used as an SFTP path to the remote node.
            loggingDir = node.getNodeDirAbsoluteUnixStyle() + "/" + sNode + "/" + instanceName + "/logs";
        } else if (instanceLogFileDirectory.contains("${com.sun.aas.instanceRoot}/logs") && node.getInstallDir() != null) {
            loggingDir = node.getInstallDir().replace("\\", "/") + "/glassfish/nodes/"
                    + sNode + "/" + instanceName + "/logs";
        } else {
            // user changed GFFileHandler.file in logging.properties
            loggingDir = instanceLogFileDirectory.substring(0, instanceLogFileDirectory.lastIndexOf("/"));
        }

        return loggingDir;
    }

    public String getLoggingDirectoryForNodeWhenNoFilesFound(String instanceLogFileDirectory, Node node, String sNode, String instanceName) {
        String loggingDir;

        if (node.getNodeDirAbsoluteUnixStyle() != null) {
            loggingDir = node.getNodeDirAbsoluteUnixStyle() + "/" + sNode + "/" + instanceName + "/logs";
        } else if (node.getInstallDir() != null) {
            loggingDir = node.getInstallDir().replace("\\", "/") + "/glassfish/nodes/"
                    + sNode + "/" + instanceName + "/logs";
        } else {
            loggingDir = instanceLogFileDirectory.substring(0, instanceLogFileDirectory.lastIndexOf("/"));
        }

        return loggingDir;
    }
}
