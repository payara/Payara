/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.server.logging.commands;

import com.sun.common.util.logging.LoggingConfigImpl;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.server.logging.LogFacade;
import com.sun.enterprise.server.logging.logviewer.backend.LogFilterForInstance;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: naman
 * Date: 6 Jul, 2010
 * Time: 3:27:24 PM
 * To change this template use File | Settings | File Templates.
 */
@ExecuteOn({RuntimeType.DAS})
@Service(name = "collect-log-files")
@PerLookup
@I18n("collect.log.files")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="collect-log-files", 
        description="collect-log-files")
})
public class CollectLogFiles implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CollectLogFiles.class);

    private static final Logger LOGGER = LogFacade.LOGGING_LOGGER;

    @Param(optional = true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    @Param(name = "retrieve", optional = true, defaultValue = "false")
    boolean retrieve;

    @Param(primary = true, optional = true, defaultValue = ".")
    private String retrieveFilePath;

    @Inject
    ServerEnvironment env;

    @Inject
    Domain domain;

    @Inject
    private ServiceLocator habitat;

    @Inject
    LoggingConfigImpl loggingConfig;

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        Properties props = initFileXferProps();

        Server targetServer = domain.getServerNamed(target);

        if (targetServer != null && targetServer.isDas()) {

            // This loop if target instance is DAS
            String logFileDetails = "";
            String zipFile = "";

            try {
                // getting log file values from logging.propertie file.
                logFileDetails = loggingConfig.getLoggingFileDetails();
            } catch (Exception ex) {
                final String errorMsg = localStrings.getLocalString(
                        "collectlogfiles.errGettingLogFiles", "Error while getting log file attribute for {0}.", target);
                report.setMessage(errorMsg);
                report.setFailureCause(ex);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            File targetDir = makingDirectoryOnDas(targetServer.getName(), report);

            try {

                String sourceDir = "";
                if (logFileDetails.contains("${com.sun.aas.instanceRoot}/logs")) {
                    sourceDir = env.getDomainRoot() + File.separator + "logs";
                } else {
                    sourceDir = logFileDetails.substring(0, logFileDetails.lastIndexOf(File.separator));
                }

                copyLogFilesForLocalhost(sourceDir, targetDir.getAbsolutePath(), report, targetServer.getName());
            } catch (Exception ex) {
                final String errorMsg = localStrings.getLocalString(
                        "collectlogfiles.errInstanceDownloading", "Error while downloading log files from {0}.", target);
                report.setMessage(errorMsg);
                report.setFailureCause(ex);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }


            try {
                String zipFilePath = getZipFilePath().getAbsolutePath();
                zipFile = loggingConfig.createZipFile(zipFilePath);
                if (zipFile == null || new File(zipFile) == null) {
                    // Failure during zip
                    final String errorMsg = localStrings.getLocalString(
                            "collectlogfiles.creatingZip", "Error while creating zip file {0}.", zipFilePath);
                    report.setMessage(errorMsg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }

            } catch (Exception e) {
                // Catching Exception if any
                final String errorMsg = localStrings.getLocalString(
                        "collectlogfiles.creatingZip", "Error while creating zip file {0}.", zipFile);
                report.setMessage(errorMsg);
                report.setFailureCause(e);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            if (this.retrieve) {
                retrieveFile(zipFile, context, getZipFilePath(), props, report);
                report.setMessage(localStrings.getLocalString(
                        "collectlogfiles.instance.success", "Created Zip file under {0}.", retrieveFilePath + File.separator + new File(zipFile).getName()));
            } else {
                report.setMessage(localStrings.getLocalString(
                        "collectlogfiles.instance.success", "Created Zip file under {0}.", zipFile));
            }

        } else if (targetServer != null && targetServer.isInstance()) {

            // This loop if target standalone instance
            String instanceName = targetServer.getName();
            String serverNode = targetServer.getNodeRef();
            Node node = domain.getNodes().getNode(serverNode);
            String zipFile = "";
            File targetDir = null;

            String logFileDetails = "";
            try {
                // getting log file values from logging.propertie file.
                logFileDetails = getInstanceLogFileDirectory(targetServer);
            } catch (Exception ex) {
                final String errorMsg = localStrings.getLocalString(
                        "collectlogfiles.errGettingLogFiles", "Error while getting log file attribute for {0}.", target);
                report.setMessage(errorMsg);
                report.setFailureCause(ex);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            targetDir = makingDirectoryOnDas(targetServer.getName(), report);

            try {
                if (node.isLocal()) {
                    String sourceDir = getLogDirForLocalNode(logFileDetails, node, serverNode, instanceName);
                    copyLogFilesForLocalhost(sourceDir, targetDir.getAbsolutePath(), report, instanceName);
                } else {
                    new LogFilterForInstance().downloadAllInstanceLogFiles(habitat, targetServer,
                            domain, LOGGER, instanceName, targetDir.getAbsolutePath(), logFileDetails);
                }
            }
            catch (Exception ex) {
                final String errorMsg = localStrings.getLocalString(
                        "collectlogfiles.errInstanceDownloading", "Error while downloading log files from {0}.", instanceName);
                report.setMessage(errorMsg);
                report.setFailureCause(ex);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            try {
                // Creating zip file and returning zip file absolute path.
                String zipFilePath = getZipFilePath().getAbsolutePath();
                zipFile = loggingConfig.createZipFile(zipFilePath);
                if (zipFile == null || new File(zipFile) == null) {
                    // Failure during zip
                    final String errorMsg = localStrings.getLocalString(
                            "collectlogfiles.creatingZip", "Error while creating zip file {0}.", zipFilePath);
                    report.setMessage(errorMsg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
            catch (Exception ex) {
                final String errorMsg = localStrings.getLocalString(
                        "collectlogfiles.creatingZip", "Error while creating zip file {0}.", zipFile);
                report.setMessage(errorMsg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            if (this.retrieve) {
                retrieveFile(zipFile, context, getZipFilePath(), props, report);
                report.setMessage(localStrings.getLocalString(
                        "collectlogfiles.instance.success", "Created Zip file under {0}.", retrieveFilePath + File.separator + new File(zipFile).getName()));
            } else {
                report.setMessage(localStrings.getLocalString(
                        "collectlogfiles.instance.success", "Created Zip file under {0}.", zipFile));
            }

        } else {
            // This loop if target is cluster

            String finalMessage = "";
            String zipFile = "";
            File targetDir = null;


            // code to download server.log file for DAS. Bug fix 16088
            String logFileDetails = "";
            try {
                // getting log file values from logging.propertie file.
                logFileDetails = loggingConfig.getLoggingFileDetails();
            } catch (Exception ex) {
                final String errorMsg = localStrings.getLocalString(
                        "collectlogfiles.errGettingLogFiles", "Error while getting log file attribute for {0}.", target);
                report.setMessage(errorMsg);
                report.setFailureCause(ex);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            targetDir = makingDirectoryOnDas(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME, report);

            try {
                String sourceDir = "";
                if (logFileDetails.contains("${com.sun.aas.instanceRoot}/logs")) {
                    sourceDir = env.getDomainRoot() + File.separator + "logs";
                } else {
                    sourceDir = logFileDetails.substring(0, logFileDetails.lastIndexOf(File.separator));
                }

                copyLogFilesForLocalhost(sourceDir, targetDir.getAbsolutePath(), report,
                        SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME);
            } catch (Exception ex) {
                final String errorMsg = localStrings.getLocalString(
                        "collectlogfiles.errInstanceDownloading", "Error while downloading log files from {0}.", target);
                report.setMessage(errorMsg);
                report.setFailureCause(ex);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            /******************************************************/


            com.sun.enterprise.config.serverbeans.Cluster cluster = domain.getClusterNamed(target);

            List<Server> instances = cluster.getInstances();

            int instanceCount = 0;
            int errorCount = 0;
            for (Server instance : instances) {
                // downloading log files for all instances which is part of cluster under temp directory.
                String instanceName = instance.getName();
                String serverNode = instance.getNodeRef();
                Node node = domain.getNodes().getNode(serverNode);
                boolean errorOccur = false;
                instanceCount++;

                logFileDetails = "";
                try {
                    // getting log file values from logging.propertie file.
                    logFileDetails = getInstanceLogFileDirectory(domain.getServerNamed(instanceName));
                } catch (Exception ex) {
                    final String errorMsg = localStrings.getLocalString(
                            "collectlogfiles.errGettingLogFiles", "Error while getting log file attribute for {0}.", target);
                    report.setMessage(errorMsg);
                    report.setFailureCause(ex);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }

                try {
                    targetDir = makingDirectoryOnDas(instanceName, report);

                    if (node.isLocal()) {
                        String sourceDir = getLogDirForLocalNode(logFileDetails, node, serverNode, instanceName);
                        copyLogFilesForLocalhost(sourceDir, targetDir.getAbsolutePath(), report, instanceName);
                    } else {
                        new LogFilterForInstance().downloadAllInstanceLogFiles(habitat, instance,
                                domain, LOGGER, instanceName, targetDir.getAbsolutePath(), logFileDetails);
                    }
                }
                catch (Exception ex) {
                    errorCount++;
                    final String errorMsg = localStrings.getLocalString(
                            "collectlogfiles.errInstanceDownloading", "Error while downloading log files from {0}.", instanceName);
                    errorOccur = true;
                    finalMessage += errorMsg + "\n";
                }
                if (!errorOccur) {
                    final String successMsg = localStrings.getLocalString(
                            "collectlogfiles.successInstanceDownloading", "Log files are downloaded for {0}.", instanceName);
                    finalMessage += successMsg + "\n";
                }
            }
            report.setMessage(finalMessage);

            if (instanceCount != errorCount) {
                try {
                    String zipFilePath = getZipFilePath().getAbsolutePath();
                    // Creating zip file and returning zip file absolute path.
                    zipFile = loggingConfig.createZipFile(zipFilePath);
                    if (zipFile == null || new File(zipFile) == null) {
                        // Failure during zip
                        final String errorMsg = localStrings.getLocalString(
                                "collectlogfiles.creatingZip", "Error while creating zip file {0}.", zipFilePath);
                        report.setMessage(errorMsg);
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return;
                    }
                }
                catch (Exception ex) {
                    final String errorMsg = localStrings.getLocalString(
                            "collectlogfiles.creatingZip", "Error while creating zip file {0}.", zipFile);
                    report.setMessage(errorMsg);
                    report.setFailureCause(ex);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }

                if (this.retrieve) {
                    retrieveFile(zipFile, context, getZipFilePath(), props, report);
                    report.setMessage(localStrings.getLocalString(
                            "collectlogfiles.cluster.success", "{0} Created Zip file under {1}.", finalMessage, retrieveFilePath + File.separator + new File(zipFile).getName()));
                } else {
                    report.setMessage(localStrings.getLocalString(
                            "collectlogfiles.cluster.success", "{0} Created Zip file under {1}.", finalMessage, zipFile));
                }

                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            } else if (instanceCount == 0) {
                report.setMessage(localStrings.getLocalString(
                        "collectlogfiles.noinstance", "No instances are defined as part of {0}. So there are no files to zip.", target));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            } else {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }

        }

        deleteDir(new File(env.getInstanceRoot() + File.separator + "collected-logs" + File.separator + "logs"));
    }

    private void copyLogFilesForLocalhost(String sourceDir, String targetDir, ActionReport report, String instanceName) throws IOException {
        // Getting all Log Files
        File logsDir = new File(sourceDir);
        File allLogFileNames[] = logsDir.listFiles();
        if (allLogFileNames == null) {
            throw new IOException("");
        }
        for (File logFile : allLogFileNames) {

            if (logFile.isFile()) {
                // File to copy in output file path.
                File toFile = new File(targetDir, logFile.getName());

                FileInputStream from = null;
                FileOutputStream to = null;

                // Copying File
                try {
                    from = new FileInputStream(logFile);
                    to = new FileOutputStream(toFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = from.read(buffer)) != -1)
                        to.write(buffer, 0, bytesRead); // write
                }
                catch (Exception ex) {
                    final String errorMsg = localStrings.getLocalString(
                            "collectlogfiles.errInstanceDownloading", "Error while downloading log file from {0}.", instanceName);
                    report.setMessage(errorMsg);
                    report.setFailureCause(ex);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                } finally {
                    if (from != null) try { from.close(); } catch (Exception ex) {}
                    if (to != null) try { to.close(); } catch (Exception ex) {}
                }


                if (!toFile.exists()) {
                    final String errorMsg = localStrings.getLocalString(
                            "collectlogfiles.errInstanceDownloading", "Error while downloading log file from {0}.", instanceName);
                    report.setMessage(errorMsg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }
    }

    private Properties initFileXferProps() {
        final Properties props = new Properties();
        props.setProperty("file-xfer-root", retrieveFilePath.replace("\\", "/"));
        return props;
    }

    private void retrieveFile(String zipFileName, AdminCommandContext context,
                              File tempDirectory, Properties props, ActionReport report) {

        // Playing with outbound payload to attach zip file..
        Payload.Outbound outboundPayload = context.getOutboundPayload();
        // GLASSFISH-17627: pass to DownloadServlet
        boolean retrieveFiles = false; 
        if (outboundPayload == null) {
            outboundPayload = PayloadImpl.Outbound.newInstance();
            retrieveFiles = true;
        }

        //code to files to output directory
        try {
            // GLASSFISH-17627: ignore zipFile, but add files directly, similarly to zipFile
            List<File> files = new ArrayList<File>(Arrays.asList(tempDirectory.listFiles()));

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                //if the file is directory, "recurse"
                if (file.isDirectory()) {
                    files.addAll(Arrays.asList(file.listFiles()));
                    continue;
                }

                if (file.getAbsolutePath().contains(".zip")) {
                    continue;
                }

                outboundPayload.attachFile(
                    "application/octet-stream",
                    tempDirectory.toURI().relativize(file.toURI()),
                    "files",
                    props,
                    file);
            }

            if (retrieveFiles) {
                File targetLocalFile = new File(retrieveFilePath); // CAUTION: file instead of dir
                if (targetLocalFile.exists()) {
                    throw new Exception("File exists");
                }

                if (!targetLocalFile.getParentFile().exists()) {
                    throw new Exception("Parent directory does not exist");
                }
                FileOutputStream targetStream = null;
                try {
                targetStream = new FileOutputStream(targetLocalFile);
                outboundPayload.writeTo(targetStream);
                targetStream.flush();
                } finally {
                    if (targetStream != null) targetStream.close();
                }
            }
        }
        catch (Exception ex) {
            final String errorMsg = localStrings.getLocalString(
                    "collectlogfiles.copyingZip", "Error while copying zip file to {0}.", retrieveFilePath);
            report.setMessage(errorMsg);
            report.setFailureCause(ex);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

    }

    public boolean deleteDir(File dir) {
        boolean ret = true;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                ret = ret && deleteDir(f);
            } else {
                ret = ret && f.delete();
            }
        }
        return ret && dir.delete();
    }

    /*
       This function is used to get log file details from logging.properties file for given target.
    */

    private String getInstanceLogFileDirectory(Server targetServer) throws IOException {

        String logFileDetailsForServer = "";
        String targetConfigName = "";

        Cluster clusterForInstance = targetServer.getCluster();
        if (clusterForInstance != null) {
            targetConfigName = clusterForInstance.getConfigRef();
        } else {
            targetConfigName = targetServer.getConfigRef();
        }

        logFileDetailsForServer = loggingConfig.getLoggingFileDetails(targetConfigName);

        return logFileDetailsForServer;

    }

    private File makingDirectory(String path, ActionReport report, String errorMsg) {
        File targetDir = new File(path);
        boolean created = false;
        if (!targetDir.exists()) {
            created = targetDir.mkdir();
            if (!created) {
                report.setMessage(errorMsg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return null;
            } else {
                return targetDir;
            }
        } else {
            return targetDir;
        }
    }

    private File makingDirectory(File parent, String path, ActionReport report, String errorMsg) {
        File targetDir = new File(parent, path);
        boolean created = false;
        if (!targetDir.exists()) {
            created = targetDir.mkdir();
            if (!created) {
                report.setMessage(errorMsg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return null;
            } else {
                return targetDir;
            }
        } else {
            return targetDir;
        }
    }

    private String getLogDirForLocalNode(String instanceLogFileName, Node node, String serverNode, String instanceName) {
        String loggingDir;
        loggingDir = new LogFilterForInstance().getLoggingDirectoryForNode(instanceLogFileName, node, serverNode, instanceName);

        File logsDir = new File(loggingDir);
        File allLogFileNames[] = logsDir.listFiles();


        boolean noFileFound = true;

        if (allLogFileNames != null) { // This check for,  if directory doesn't present or missing on machine. It happens due to bug 16451
            for (int i = 0; i < allLogFileNames.length; i++) {
                File file = allLogFileNames[i];
                String fileName = file.getName();
                // code to remove . and .. file which is return
                if (file.isFile() && !fileName.equals(".") && !fileName.equals("..") && fileName.contains(".log")
                        && !fileName.contains(".log.")) {
                    noFileFound = false;
                    break;
                }
            }
        }

        if (noFileFound) {
            // this loop is used when user has changed value for server.log but not restarted the server.
            loggingDir = new LogFilterForInstance().getLoggingDirectoryForNodeWhenNoFilesFound(instanceLogFileName, node, serverNode, instanceName);

        }

        return loggingDir;
    }

    private File makingDirectoryOnDas(String serverName, ActionReport report) {
        File tempDirectory = makingDirectory(env.getInstanceRoot(), "collected-logs", report, localStrings.getLocalString(
                "collectlogfiles.creatingTempDirectory", "Error while creating temp directory on server for downloading log files."));

        String targetDirPath = tempDirectory.getAbsolutePath() + File.separator + "logs";
        File targetDir = makingDirectory(targetDirPath, report, localStrings.getLocalString(
                "collectlogfiles.creatingTempDirectory", "Error while creating temp directory on server for downloading log files."));

        targetDirPath = targetDir.getAbsolutePath() + File.separator + serverName;
        targetDir = makingDirectory(targetDirPath, report, localStrings.getLocalString(
                "collectlogfiles.creatingTempDirectory", "Error while creating temp directory on server for downloading log files."));

        return targetDir;
    }

    private File getZipFilePath() {
        return new File(env.getInstanceRoot() + File.separator + "collected-logs");
    }
}
