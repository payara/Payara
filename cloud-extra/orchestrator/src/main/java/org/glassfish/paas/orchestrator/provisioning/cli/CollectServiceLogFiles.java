/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator.provisioning.cli;

/**
 * Created by IntelliJ IDEA.
 * User: naman
 * Date: 7/2/12
 * Time: 11:57 AM
 * To change this template use File | Settings | File Templates.
 */

import com.sun.common.util.logging.LoggingConfigImpl;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.logging.LogDomains;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.PaaSAppInfoRegistry;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.service.spi.ConfiguredService;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceLogRecord;
import org.glassfish.paas.orchestrator.service.spi.ServiceLogType;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.glassfish.hk2.api.PerLookup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@org.jvnet.hk2.annotations.Service(name = "collect-service-log-files")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "collect-service-log-files", description = "Collect Log files for Services")
})
public class CollectServiceLogFiles implements AdminCommand {

    @Inject
    protected PaaSAppInfoRegistry appInfoRegistry;

    @Param(name = "appname", optional = true)
    private String appName;

    @Param(name = "servicename", optional = true)
    private String serviceName;

    @Param(name = "datetime", optional = true, defaultValue = "01/01/1970 00:00:00.001")
    private String datetime;

    @Param(name = "retrieve", optional = true, defaultValue = "false")
    boolean retrieve;

    @Param(primary = true, optional = true, defaultValue = ".")
    private String retrieveFilePath;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    protected ServiceOrchestratorImpl orchestrator;

    @Inject
    ServerEnvironment env;

    @Inject
    LoggingConfigImpl loggingConfig;

    Map<org.glassfish.paas.orchestrator.service.spi.Service, List<ServiceLogRecord>> serviceLogRecordMap;

    String recordBeginMarker = "[#|";
    String recordEndMarker = "|#]";
    String fieldSeparator = "|";

    long dateinmilliseconds = 0L;

    private static Logger logger = LogDomains.getLogger(CollectServiceLogFiles.class, LogDomains.PAAS_LOGGER);

    StringBuffer reportMessage = new StringBuffer();

    String zipFileName = "";

    /**
     * Main function which is used to collect log records for given appName or serviceName
     * First, validates data if missing values then give appropriate error to user
     * Second, if appName is not null then get all services used by that app and download logs from those services and
     * copying log data to DAS. if serviceName is not null then download logs from that service and copying data to DAS.
     *
     * @param context
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        logger.log(Level.INFO, "Collecting Service Log Records");

        if (appName == null && serviceName == null) {
            report.setMessage("Missing application name or service name.");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (appName != null && serviceName != null) {
            report.setMessage("Enter either application name or service name.");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (!retrieve && !retrieveFilePath.equals(".") && retrieveFilePath.length() > 1) {
            reportMessage.append("WARNING: Retrieve flag is false so zip file is not copied to " + retrieveFilePath + "\n");
        }

        if (datetime != null) {

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
            Date date = null;
            try {
                date = dateFormat.parse(datetime);
            } catch (ParseException ex) {
                logger.warning("Error while parsing date: " + ex);
                report.setMessage("Error while parsing date [" + datetime + "]. Please use dd/MM/yyyy HH:mm:ss.SSS formatter for passing date.");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            dateinmilliseconds = date.getTime();
        }

        if (appName != null) {

            zipFileName = appName;

            if (domain.getApplications().getApplication(appName) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such application [" + appName + "] is deployed");
                return;
            }

            Set<org.glassfish.paas.orchestrator.service.spi.Service> appServices = appInfoRegistry.getServices(appName);
            for (org.glassfish.paas.orchestrator.service.spi.Service service : appServices) {
                try {
                    serviceLogRecordMap = service.collectLogs(service.getDefaultLogType(), Level.ALL, new Date());
                    if (serviceLogRecordMap != null) {
                        creatingLogFilesOnDAS(serviceLogRecordMap, report, service.getName());
                        reportMessage.append("Logs are collected for [" + service.getName() + "] having service type [" + service.getServiceType().getName() + "] " +
                                "which is used by [" + appName + "]" + "\n");
                    } else {
                        reportMessage.append("Collect Logs are unsupported for [" + service.getName() + "]  having service type [" + service.getServiceType().getName() + "] " +
                                "which is used by [" + appName + "]" + "\n");
                    }
                } catch (UnsupportedOperationException ex) {
                    reportMessage.append("Collect Logs are unsupported for [" + service.getName() + "]  having service type [" + service.getServiceType().getName() + "] " +
                            "which is used by [" + appName + "]" + "\n");
                }
            }
        }


        if (serviceName != null) {

            zipFileName = serviceName;

            Services services = serviceUtil.getServices();

            List<Service> allServices = services.getServices();

            boolean serviceFound = false;
            boolean appScopedServiceFound = false;
            boolean configuredServiceFound = false;
            boolean provisionedServiceFound = false;
            String appName = "";

            for (Service service : allServices) {
                if (service.getServiceName().equals(serviceName)) {
                    if (service instanceof ApplicationScopedService) {
                        appName = ((ApplicationScopedService) service).getApplicationName();
                        appScopedServiceFound = true;
                    } else if (service instanceof ConfiguredService) {
                        configuredServiceFound = true;
                    } else if (service instanceof ProvisionedService) {
                        provisionedServiceFound = true;
                    }
                    serviceFound = true;
                    break;
                }
            }

            if (!serviceFound) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such service [" + serviceName + "] is available");
                return;
            }

            if (appScopedServiceFound) {
                Set<org.glassfish.paas.orchestrator.service.spi.Service> appServices = orchestrator.getServices(appName);
                for (org.glassfish.paas.orchestrator.service.spi.Service service : appServices) {
                    if (service.getName().equals(serviceName)) {
                        try {
                            serviceLogRecordMap = service.collectLogs(service.getDefaultLogType(), Level.ALL, new Date());
                            creatingLogFilesOnDAS(serviceLogRecordMap, report, service.getName());
                            reportMessage.append("Logs are collected for [" + service.getName() + "] having service " +
                                    "type [" + service.getServiceType().getName() + "] " +
                                    "which is used by [" + appName + "]" + "\n");
                        } catch (UnsupportedOperationException ex) {
                            reportMessage.append("Collect Logs are unsupported for [" + service.getName() + "] having " +
                                    "service type [" + service.getServiceType().getName() + "] " +
                                    "which is used by [" + appName + "]" + "\n");
                        }
                    }
                }
            }

            if (configuredServiceFound) {
                ConfiguredService configuredService = orchestrator.getConfiguredService(serviceName);
                if (configuredService != null) {
                    try {
                        serviceLogRecordMap = configuredService.collectLogs(configuredService.getDefaultLogType(), Level.ALL, new Date());
                        creatingLogFilesOnDAS(serviceLogRecordMap, report, configuredService.getName());
                        reportMessage.append("Logs are collected for [" + serviceName + "] having service " +
                                "type [" + configuredService.getServiceType().getName() + "]" + "\n");
                    } catch (UnsupportedOperationException ex) {
                        reportMessage.append("Collect Logs are unsupported for [" + serviceName + "] having " +
                                "service type [" + configuredService.getServiceType().getName() + "]" + "\n");
                    }
                }
            }

            if (provisionedServiceFound) {
                ProvisionedService provisionedService = orchestrator.getSharedService(serviceName);
                if (provisionedService != null) {
                    try {
                        serviceLogRecordMap = provisionedService.collectLogs(provisionedService.getDefaultLogType(), Level.ALL, new Date());
                        creatingLogFilesOnDAS(serviceLogRecordMap, report, provisionedService.getName());
                        reportMessage.append("Logs are collected for [" + serviceName + "] having service type " +
                                "[" + provisionedService.getServiceType().getName() + "]" + "\n");
                    } catch (UnsupportedOperationException ex) {
                        reportMessage.append("Collect Logs are unsupported for [" + serviceName + "] having service " +
                                "type [" + provisionedService.getServiceType().getName() + "]" + "\n");
                    }
                }
            }
        }

        String zipFile = "";
        File zipFilePath = getZipFilePath();
        if (zipFilePath != null && zipFilePath.exists()) {
            try {
                zipFile = loggingConfig.createZipFile(zipFilePath.getAbsolutePath(), zipFileName);
                if (zipFile == null || new File(zipFile) == null) {
                    // Failure during zip
                    reportMessage.append("File is downloaded on DAS [" + getZipFilePath().getAbsolutePath() + "] but " +
                            "error occur while creating zip file");
                    report.setMessage(reportMessage.toString());
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception during creating zip file on DAS: " + e);
                reportMessage.append("File is downloaded on DAS [" + getZipFilePath().getAbsolutePath() + "] but " +
                        "error occur while creating zip file");
                report.setMessage(reportMessage.toString());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            } finally {
                deleteDir(new File(getZipFilePath().getAbsolutePath() + File.separator + "logs"));
            }

            if (retrieve) {
                zipFile = retrieveFile(zipFile, context, getZipFilePath(), initFileXferProps(), report);
            }

            if (zipFile != null) {
                reportMessage.append("Zip file is available at: " + zipFile + "\n");
            }

        } else {
            reportMessage.append("As collect logs are unsupported for each of the services so no zip file available on DAS." + "\n");
        }

        report.setMessage(reportMessage.toString());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    /**
     * Used to create directory on DAS and writing Log Data to destination file
     *
     * @param serviceLogRecordMap
     * @param report
     */
    private void creatingLogFilesOnDAS(Map<org.glassfish.paas.orchestrator.service.spi.Service, List<ServiceLogRecord>> serviceLogRecordMap
            , ActionReport report, String fileName) {

        for (org.glassfish.paas.orchestrator.service.spi.Service service : serviceLogRecordMap.keySet()) {
            String serviceName = service.getName();
            File serviceDirectoryOnDAS = makingDirectoryOnDas(serviceName, report);

            List<ServiceLogRecord> serviceLogRecords = serviceLogRecordMap.get(service);

            boolean dataWrittenToFile = writingServiceLogRecordToFile(serviceDirectoryOnDAS, serviceLogRecords, fileName);

            if (!dataWrittenToFile) {
                reportMessage.append("Log details are missing for [" + serviceName + "]" + "\n");
            }
        }
    }

    /**
     * Writing log data (ServiceLogRecord) to the destination file on DAS
     *
     * @param serviceDirectoryOnDAS
     * @param serviceLogRecords
     * @return
     */
    private boolean writingServiceLogRecordToFile(File serviceDirectoryOnDAS, List<ServiceLogRecord> serviceLogRecords, String fileName) {

        try {
            if (!serviceDirectoryOnDAS.exists()) {
                serviceDirectoryOnDAS.mkdirs();
            }

            File serviceLogFile = new File(serviceDirectoryOnDAS, fileName);

            FileWriter fstream = new FileWriter(serviceLogFile);
            BufferedWriter out = new BufferedWriter(fstream);

            for (ServiceLogRecord serviceLogRecord : serviceLogRecords) {

                long millis = serviceLogRecord.getMillis();

                if (millis > dateinmilliseconds) {

                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(millis);
                    String dateTime = formatter.format(calendar.getTime());


                    StringBuffer logDetails = new StringBuffer();
                    logDetails.append(recordBeginMarker);
                    logDetails.append(dateTime);
                    logDetails.append(fieldSeparator);
                    logDetails.append(serviceLogRecord.getLevel().toString());
                    logDetails.append(fieldSeparator);
                    logDetails.append(serviceLogRecord.getLoggerName());
                    logDetails.append(fieldSeparator);
                    logDetails.append(serviceLogRecord.getApplicationName());
                    logDetails.append(fieldSeparator);
                    Object[] myObject = serviceLogRecord.getParameters();
                    if (myObject != null) {
                        for (Object object : myObject) {
                            logDetails.append(object.toString());
                            logDetails.append("|");
                        }
                    }
                    logDetails.append(serviceLogRecord.getMessage());
                    logDetails.append(recordEndMarker);
                    out.write(logDetails.toString());
                    out.newLine();
                }
            }
            out.close();
            fstream.close();
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception during creating file on DAS: " + e);
            return false;
        }
    }

    /**
     * Helper function used to create directory on DAS
     *
     * @param parent
     * @param path
     * @param report
     * @return
     */
    private File makingDirectory(File parent, String path, ActionReport report) {
        File targetDir = new File(parent, path);
        boolean created = false;
        if (!targetDir.exists()) {
            created = targetDir.mkdir();
            if (!created) {
                report.setMessage("Error while creating [" + targetDir.getAbsolutePath() + "]  on Server.");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return null;
            } else {
                return targetDir;
            }
        } else {
            return targetDir;
        }
    }

    /**
     * Used to create directory on DAS
     *
     * @param serviceName
     * @param report
     * @return
     */
    private File makingDirectoryOnDas(String serviceName, ActionReport report) {

        //creating collect-logs directory under domains/domain1
        File collectLogsDir = makingDirectory(env.getInstanceRoot(), "collected-logs", report);

        //creating logs directory under domains/domain1/collect-logs
        File logsDir = makingDirectory(collectLogsDir, "logs", report);

        // assigning domains/domain1/collect-logs/logs as app directory
        File appDir = logsDir;

        if (appName != null) {
            // creating domains/domain1/collect-logs/logs/<appName> as app directory
            appDir = makingDirectory(logsDir, appName, report);
        }

        // creating target directory either
        // domains/domain1/collect-logs/logs/<appName>/<serviceName> (if appName is there) or
        // domains/domain1/collect-logs/logs/<serviceName> (if no appName found)
        File targetDir = makingDirectory(appDir, serviceName, report);

        return targetDir;
    }

    /**
     * Helper class to delete the directory
     *
     * @param dir
     * @return
     */
    private boolean deleteDir(File dir) {
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

    /**
     * Returns path where zip file needs to be created.
     *
     * @return
     */
    private File getZipFilePath() {
        return new File(env.getInstanceRoot() + File.separator + "collected-logs");
    }

    /**
     * Helper function to set the properties which is used by Payload
     *
     * @return
     */
    private Properties initFileXferProps() {
        final Properties props = new Properties();
        props.setProperty("file-xfer-root", retrieveFilePath.replace("\\", "/"));
        return props;
    }

    /**
     * Used to copy created zip file to the user specified directory. Used only when retrieve flag is set to true.
     *
     * @param zipFileName
     * @param context
     * @param sourceDir
     * @param props
     * @param report
     *
     * @return return zip file details from retrieve location
     */
    private String retrieveFile(String zipFileName, AdminCommandContext context,
                                File sourceDir, Properties props, ActionReport report) {

        String zipFileAtRetrieveLocation = "";

        // Playing with outbound payload to attach zip file..
        Payload.Outbound outboundPayload = context.getOutboundPayload();
        boolean fileAttachedToPayload = false;
        if (outboundPayload == null) {
            outboundPayload = PayloadImpl.Outbound.newInstance();
        }

        //code to files to output directory
        try {
            List<File> files = new ArrayList<File>(Arrays.asList(sourceDir.listFiles()));

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                //if the file is directory, "recurse"
                if (file.isDirectory()) {
                    files.addAll(Arrays.asList(file.listFiles()));
                    continue;
                }

                if (file.getAbsolutePath().equals(zipFileName)) {
                    outboundPayload.attachFile(
                            "application/octet-stream",
                            sourceDir.toURI().relativize(file.toURI()),
                            "files",
                            props,
                            file);

                    fileAttachedToPayload = true;
                    break;
                }
            }

            if (fileAttachedToPayload) {
                File targetLocalFile = new File(retrieveFilePath); // CAUTION: file instead of dir StringBuffer

                if (!targetLocalFile.exists()) {
                    boolean created = targetLocalFile.mkdirs();

                    if (!created) {
                        reportMessage.append("Retrieve File Path is not exists [" + retrieveFilePath + "]");
                        report.setMessage(reportMessage.toString());
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return null;
                    }
                }

                zipFileAtRetrieveLocation = targetLocalFile + File.separator
                        + zipFileName.substring(zipFileName.lastIndexOf(File.separator) + 1, zipFileName.length());

                FileOutputStream targetStream = new FileOutputStream(zipFileAtRetrieveLocation);
                outboundPayload.writeTo(targetStream);
                targetStream.flush();
                targetStream.close();
            }

        } catch (Exception ex) {
            logger.severe("Error while copying zip file [" + zipFileName + "] to [" + retrieveFilePath + " ]" + ex);
            reportMessage.append("Error while copying zip file [" + zipFileName + "] to [" + retrieveFilePath + " ]");
            report.setMessage(reportMessage.toString());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return null;
        }

        return zipFileAtRetrieveLocation;

    }

}
