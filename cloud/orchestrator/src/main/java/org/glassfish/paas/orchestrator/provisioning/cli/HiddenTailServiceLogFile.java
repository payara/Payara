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

import com.sun.common.util.logging.LoggingConfigImpl;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.paas.orchestrator.PaaSAppInfoRegistry;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.service.spi.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: naman
 * Date: 21/2/12
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */

@org.jvnet.hk2.annotations.Service(name = "_hidden-tail-service-log-file")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
@I18n("hidden.tail.server.logfile")
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "_hidden-tail-service-log-file",
                description = "Tail Service Log file")
})
public class HiddenTailServiceLogFile implements AdminCommand {

    @Inject
    protected PaaSAppInfoRegistry appInfoRegistry;

    @Param(name = "servicename", optional = false)
    private String serviceName;

    @Param(name = "logtype", optional = false)
    private String logtype;

    @Param(name = "filepointer", optional = false)
    private String filepointer;


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

    Map<Service, List<ServiceLogRecord>> serviceLogRecordMap;

    String recordBeginMarker = "[#|";
    String recordEndMarker = "|#]";
    String fieldSeparator = "|";

    long dateinmilliseconds = 0L;

    private static Logger logger = LogDomains.getLogger(CollectServiceLogFiles.class, LogDomains.PAAS_LOGGER);

    StringBuffer reportMessage = new StringBuffer();

    StringBuffer fileData = new StringBuffer();

    String newLine = System.getProperty("line.separator");

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        if (!isLongNumber(filepointer)) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Invalid file pointer [" + filepointer + "]. Please enter valid numeric value.");
            return;
        }

        long _filePointer = Long.valueOf(filepointer);

        logger.log(Level.INFO, "Collecting Service Log Records");

        ServiceLogType serviceLogType = new ServiceLogType() {
            @Override
            public String getName() {
                return logtype;
            }
        };

        if (serviceName != null) {

            Services services = serviceUtil.getServices();

            List<org.glassfish.paas.orchestrator.config.Service> allServices = services.getServices();

            boolean serviceFound = false;
            boolean appScopedServiceFound = false;
            boolean configuredServiceFound = false;
            boolean provisionedServiceFound = false;
            String appName = "";
            File serviceLogFile = null;

            for (org.glassfish.paas.orchestrator.config.Service service : allServices) {
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
                Set<Service> appServices = orchestrator.getServices(appName);
                for (org.glassfish.paas.orchestrator.service.spi.Service service : appServices) {
                    if (service.getName().equals(serviceName)) {
                        try {
                            serviceLogRecordMap = service.collectLogs(serviceLogType, Level.INFO, new Date());
                            serviceLogFile = creatingLogFilesOnDAS(serviceLogRecordMap, report);
                            reportMessage.append("Logs are collected for [" + service.getName() + "] having service " +
                                    "type [ " + service.getServiceType().getName() + " ] " +
                                    "which is used by [" + appName + "]" + "\n");
                        } catch (UnsupportedOperationException ex) {
                            reportMessage.append("Tailing Log is unsupported for [" + service.getName() + "] having " +
                                    "service type [ " + service.getServiceType().getName() + " ] " +
                                    "which is used by [" + appName + "]" + "\n");
                        }
                    }
                }
            }

            if (configuredServiceFound) {
                ConfiguredService configuredService = orchestrator.getConfiguredService(serviceName);
                if (configuredService != null) {
                    try {
                        serviceLogRecordMap = configuredService.collectLogs(serviceLogType, Level.INFO, new Date());
                        serviceLogFile = creatingLogFilesOnDAS(serviceLogRecordMap, report);
                        reportMessage.append("Logs are collected for [" + serviceName + "] having service " +
                                "type [ " + configuredService.getServiceType().getName() + " ]" + "\n");
                    } catch (UnsupportedOperationException ex) {
                        reportMessage.append("Tailing Log is unsupported for [" + serviceName + "] having " +
                                "service type [ " + configuredService.getServiceType().getName() + " ]" + "\n");
                    }
                }
            }

            if (provisionedServiceFound) {
                ProvisionedService provisionedService = orchestrator.getSharedService(serviceName);
                if (provisionedService != null) {
                    try {
                        serviceLogRecordMap = provisionedService.collectLogs(serviceLogType, Level.INFO, new Date());
                        serviceLogFile = creatingLogFilesOnDAS(serviceLogRecordMap, report);
                        reportMessage.append("Logs are collected for [" + serviceName + "] having service type " +
                                "[ " + provisionedService.getServiceType().getName() + " ]" + "\n");
                    } catch (UnsupportedOperationException ex) {
                        reportMessage.append("Tailing Log is unsupported for [" + serviceName + "] having service " +
                                "type [ " + provisionedService.getServiceType().getName() + " ]" + "\n");
                    }
                }
            }

            if (!serviceLogFile.exists() || serviceLogFile.isDirectory() || !serviceLogFile.canRead()) {
                reportMessage.append("Cannot Find log file for given [" + serviceName + "]" + "\n");
            } else {

                try {
                    long len = serviceLogFile.length();
                    if (len < _filePointer) {
                        this.appendMessage("Log file was reset. Restarting logging from start of file.");
                        _filePointer = len;
                    } else if (len > _filePointer) {
                        // File must have had something added to it!

                        RandomAccessFile raf = new RandomAccessFile(serviceLogFile, "r");
                        raf.seek(_filePointer);
                        String line = null;
                        while ((line = raf.readLine()) != null) {
                            this.appendLine(line);
                        }
                        _filePointer = raf.getFilePointer();
                        raf.close();
                    }

                    report.getTopMessagePart().addProperty("filedata", fileData.toString());
                    report.getTopMessagePart().addProperty("filepointer", String.valueOf(_filePointer));

                } catch (Exception e) {
                    System.out.println(e);
                }
            }


        }
    }

    /**
     * Helper method to append the line to string buffer
     * @param line
     */
    private void appendLine(String line) {
        fileData.append(line);
        fileData.append(newLine);
    }

    /**
     * Helper method to append message to the string buffer
     * @param message
     */
    private void appendMessage(String message) {
        this.appendLine("[" + new Date().toString() + ", " + message + "]");
    }

    /**
     * Helper method to verify number is numeric or not
     * @param num
     * @return
     */
    private boolean isLongNumber(String num) {
        try {
            Long.parseLong(num);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }


    /**
     * Used to create directory on DAS and writing Log Data to destination file
     *
     * @param serviceLogRecordMap
     * @param report
     */
    private File creatingLogFilesOnDAS(Map<org.glassfish.paas.orchestrator.service.spi.Service, List<ServiceLogRecord>> serviceLogRecordMap
            , ActionReport report) {

        for (org.glassfish.paas.orchestrator.service.spi.Service service : serviceLogRecordMap.keySet()) {
            String serviceName = service.getName();
            File serviceDirectoryOnDAS = makingDirectoryOnDas(serviceName, report);

            List<ServiceLogRecord> serviceLogRecords = serviceLogRecordMap.get(service);

            File serviceLogFile = writingServiceLogRecordToFile(serviceDirectoryOnDAS, serviceLogRecords);

            if (serviceLogFile == null) {
                reportMessage.append("Log details are missing for [" + serviceName + "]" + "\n");
                return null;
            } else {
                return serviceLogFile;
            }
        }
        return null;
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

        // creating target directory either
        // domains/domain1/collect-logs/logs/<serviceName> (if no appName found)
        File targetDir = makingDirectory(logsDir, serviceName, report);

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
     * Writing log data (ServiceLogRecord) to the destination file on DAS
     *
     * @param serviceDirectoryOnDAS
     * @param serviceLogRecords
     * @return
     */
    private File writingServiceLogRecordToFile(File serviceDirectoryOnDAS, List<ServiceLogRecord> serviceLogRecords) {

        try {
            if (!serviceDirectoryOnDAS.exists()) {
                serviceDirectoryOnDAS.mkdirs();
            }

            File serviceLogFile = new File(serviceDirectoryOnDAS, logtype);

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
            return serviceLogFile;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception during creating file on DAS: " + e);
            return null;
        }
    }
}
