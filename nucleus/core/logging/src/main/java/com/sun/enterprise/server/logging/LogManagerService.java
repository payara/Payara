/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.server.logging;

import com.sun.common.util.logging.LoggingConfigImpl;
import com.sun.common.util.logging.LoggingOutputStream;
import com.sun.common.util.logging.LoggingXMLNames;
import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.module.bootstrap.EarlyLogHandler;
import com.sun.enterprise.util.EarlyLogger;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.logging.AgentFormatterDelegate;
import org.glassfish.api.admin.FileMonitoring;
import org.glassfish.common.util.Constants;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.Rank;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.api.InitRunLevel;
import org.glassfish.internal.config.UnprocessedConfigListener;
import org.glassfish.server.ServerEnvironmentImpl;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.*;
import java.util.logging.Formatter;

/**
 * Reinitialzie the log manager using our logging.properties file.
 *
 * @author Jerome Dochez
 * @author Carla Mott
 * @author Naman Mehta
 */

@Service
@RunLevel(InitRunLevel.VAL)
@Rank(Constants.IMPORTANT_RUN_LEVEL_SERVICE)
public class LogManagerService implements PostConstruct, PreDestroy, org.glassfish.internal.api.LogManager {

    @Inject
    ServerEnvironmentImpl env;

    @Inject
    ServiceLocator habitat;

    @Inject @Optional
    Agent agent = null;

    @Inject
    FileMonitoring fileMonitoring;

    @Inject
    LoggingConfigImpl loggingConfig;

    @Inject
    UnprocessedConfigListener ucl;

    @Inject
    Domain domain;

    final Map<String, Handler> gfHandlers = new HashMap<String, Handler>();

    private static final Logger LOGGER = LogFacade.LOGGING_LOGGER;

    PrintStream oStdOutBackup = System.out;
    PrintStream oStdErrBackup = System.err;

    String serverLogFileDetail = "";
    String handlerDetail = "";
    String handlerServices = "";
    String consoleHandlerFormatterDetail = "";
    String gffileHandlerFormatterDetail = "";
    String rotationOnTimeLimitInMinutesDetail = "";
    String flushFrequencyDetail = "";
    String filterHandlerDetails = "";
    String logToConsoleDetail = "";
    String rotationInTimeLimitInBytesDetail = "";
    String useSystemLoggingDetail = "";
    String fileHandlerCountDetail = "";
    String retainErrorsStaticticsDetail = "";
    String log4jVersionDetail = "";
    String maxHistoryFilesDetail = "";
    String rotationOnDateChangeDetail = "";
    String fileHandlerPatternDetail = "";
    String fileHandlerFormatterDetail = "";
    String logFormatDateFormatDetail = "";

    private static final String SERVER_LOG_FILE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.file";
    private static final String HANDLER_PROPERTY = "handlers";
    private static final String HANDLER_SERVICES_PROPERTY = "handlerServices";
    private static final String CONSOLEHANDLER_FORMATTER_PROPERTY = "java.util.logging.ConsoleHandler.formatter";
    private static final String GFFILEHANDLER_FORMATTER_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.formatter";
    private static final String ROTATIONTIMELIMITINMINUTES_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.rotationTimelimitInMinutes";
    private static final String FLUSHFREQUENCY_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.flushFrequency";
    private static final String FILEHANDLER_LIMIT_PROPERTY = "java.util.logging.FileHandler.limit";
    private static final String LOGTOCONSOLE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logtoConsole";
    private static final String ROTATIONLIMITINBYTES_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.rotationLimitInBytes";
    private static final String USESYSTEMLOGGING_PROPERTY = "com.sun.enterprise.server.logging.SyslogHandler.useSystemLogging";
    private static final String FILEHANDLER_COUNT_PROPERTY = "java.util.logging.FileHandler.count";
    private static final String RETAINERRORSSTATICTICS_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.retainErrorsStasticsForHours";
    private static final String LOG4J_VERSION_PROPERTY = "log4j.logger.org.hibernate.validator.util.Version";
    private static final String MAXHISTORY_FILES_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.maxHistoryFiles";
    private static final String ROTATIONONDATECHANGE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.rotationOnDateChange";
    private static final String FILEHANDLER_PATTERN_PROPERTY = "java.util.logging.FileHandler.pattern";
    private static final String FILEHANDLER_FORMATTER_PROPERTY = "java.util.logging.FileHandler.formatter";
    private static final String LOGFORMAT_DATEFORMAT_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logFormatDateFormat";

    final static String EXCLUDE_FIELDS_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.excludeFields";
    final static String MULTI_LINE_MODE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.multiLineMode";

    private String RECORD_BEGIN_MARKER = "[#|";
    private String RECORD_END_MARKER = "|#]";
    private String RECORD_FIELD_SEPARATOR = "|";
    private String RECORD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    String recordBeginMarker;
    String recordEndMarker;
    String recordFieldSeparator;
    String recordDateFormat;

    Vector<Logger> loggerReference = new Vector<Logger>();

    private String excludeFields;

    private boolean multiLineMode = false;

    private LoggingOutputStream stdoutOutputStream=null;

    private LoggingOutputStream stderrOutputStream=null;

    /*
        Returns properties based on the DAS/Cluster/Instance
      */

    public Map<String, String> getLoggingProperties() throws IOException {

        Server targetServer = domain.getServerNamed(env.getInstanceName());

        Map<String, String> props = null;

        if (targetServer != null) {
            if (targetServer.isDas()) {
                props = loggingConfig.getLoggingProperties();
            } else if (targetServer.getCluster() != null) {
                props = loggingConfig.getLoggingProperties(targetServer.getCluster().getConfigRef());
            } else if (targetServer.isInstance()) {
                props = loggingConfig.getLoggingProperties(targetServer.getConfigRef());
            } else {
                props = loggingConfig.getLoggingProperties();
            }
        } else {
            props = loggingConfig.getLoggingProperties();
        }

        return props;
    }

    /*
        Returns logging file to be monitor during server is running.
     */

    public File getLoggingFile() throws IOException {

        File file = null;

        Server targetServer = domain.getServerNamed(env.getInstanceName());

        if (targetServer != null) {
            if (targetServer.isDas()) {
                file = new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
            } else if (targetServer.getCluster() != null) {
                String pathForLogging = env.getConfigDirPath() + File.separator + targetServer.getCluster().getConfigRef();
                File dirForLogging = new File(pathForLogging);

                file = new File(dirForLogging, ServerEnvironmentImpl.kLoggingPropertiesFileName);

            } else if (targetServer.isInstance()) {
                String pathForLogging = env.getConfigDirPath() + File.separator + targetServer.getConfigRef();
                File dirForLogging = new File(pathForLogging);

                file = new File(dirForLogging, ServerEnvironmentImpl.kLoggingPropertiesFileName);

            } else {
                file = new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
            }
        } else {
            file = new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
        }
        return file;
    }

    /**
     * Initialize the loggers
     */
    public void postConstruct() {

        // if the system property is already set, we don't need to do anything
        if (System.getProperty("java.util.logging.config.file") != null) {
            System.out.println("\n\n\n#!## LogManagerService.postConstruct : java.util.logging.config.file=" +System.getProperty("java.util.logging.config.file"));

            return;
        }

        // logging.properties massaging.
        final LogManager logMgr = LogManager.getLogManager();
        File logging = null;

        // reset settings
        try {


            logging = getLoggingFile();
            System.setProperty("java.util.logging.config.file", logging.getAbsolutePath());



            String rootFolder = env.getProps().get(com.sun.enterprise.util.SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
            String templateDir = rootFolder + File.separator + "lib" + File.separator + "templates";
            File src = new File(templateDir, ServerEnvironmentImpl.kLoggingPropertiesFileName);
            File dest = new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);

            System.out.println("\n\n\n#!## LogManagerService.postConstruct : rootFolder=" +rootFolder);
            System.out.println("#!## LogManagerService.postConstruct : templateDir=" +templateDir);
            System.out.println("#!## LogManagerService.postConstruct : src=" +src);
            System.out.println("#!## LogManagerService.postConstruct : dest=" +dest);

            if (!logging.exists()) {
                LOGGER.log(Level.FINE, logging.getAbsolutePath() + " not found, creating new file from template.");
                FileUtils.copy(src, dest);
                logging = new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
            }
            logMgr.readConfiguration();


        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, LogFacade.ERROR_READING_CONF_FILE, e);
        }

        FormatterDelegate agentDelegate = null;
        if (agent != null) {
            agentDelegate = new AgentFormatterDelegate(agent);

        }

        // force the ConsoleHandler to use GF formatter
        String formatterClassName = null;
        try {

            Map<String, String> props = getLoggingProperties();
            formatterClassName = props.get(CONSOLEHANDLER_FORMATTER_PROPERTY);
            if (formatterClassName == null || formatterClassName.isEmpty()) {
                formatterClassName = UniformLogFormatter.class.getName();
            }
            consoleHandlerFormatterDetail = formatterClassName;
            excludeFields  = props.get(EXCLUDE_FIELDS_PROPERTY);
            multiLineMode  = Boolean.parseBoolean(props.get(MULTI_LINE_MODE_PROPERTY));
            if (formatterClassName.equals(UniformLogFormatter.class.getName())) {
                // used to support UFL formatter in GF.
                UniformLogFormatter formatter = new UniformLogFormatter();
                String cname = "com.sun.enterprise.server.logging.GFFileHandler";
                recordBeginMarker = props.get(cname + ".logFormatBeginMarker");
                if (recordBeginMarker == null || ("").equals(recordBeginMarker)) {
                    LOGGER.log(Level.FINE, "Record begin marker is not a proper value so using default.");
                    recordBeginMarker = RECORD_BEGIN_MARKER;
                }

                recordEndMarker = props.get(cname + ".logFormatEndMarker");
                if (recordEndMarker == null || ("").equals(recordEndMarker)) {
                    LOGGER.log(Level.FINE, "Record end marker is not a proper value so using default.");
                    recordEndMarker = RECORD_END_MARKER;
                }

                recordFieldSeparator = props.get(cname + ".logFormatFieldSeparator");
                if (recordFieldSeparator == null || ("").equals(recordFieldSeparator) || recordFieldSeparator.length() > 1) {
                    LOGGER.log(Level.FINE, "Log Format field separator is not a proper value so using default.");
                    recordFieldSeparator = RECORD_FIELD_SEPARATOR;
                }

                recordDateFormat = props.get(cname + ".logFormatDateFormat");
                if (recordDateFormat != null && !("").equals(recordDateFormat)) {
                    SimpleDateFormat sdf = new SimpleDateFormat(recordDateFormat);
                    try {
                        sdf.format(new Date());
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "Date Format specified is wrong so using default.");
                        recordDateFormat = RECORD_DATE_FORMAT;
                    }
                } else {
                    LOGGER.log(Level.FINE, "Date Format specified is wrong so using default.");
                    recordDateFormat = RECORD_DATE_FORMAT;
                }

                formatter.setRecordBeginMarker(recordBeginMarker);
                formatter.setRecordEndMarker(recordEndMarker);
                formatter.setRecordDateFormat(recordDateFormat);
                formatter.setRecordFieldSeparator(recordFieldSeparator);
                formatter.setExcludeFields(excludeFields);
                formatter.setMultiLineMode(multiLineMode);
                for (Handler handler : logMgr.getLogger("").getHandlers()) {                    
                    // only get the ConsoleHandler
                    if (handler.getClass().equals(ConsoleHandler.class)) {
                        handler.setFormatter(formatter);
                        break;
                    }
                }
            } else if (formatterClassName.equals(ODLLogFormatter.class.getName())) {
                // used to support ODL formatter in GF.
                ODLLogFormatter formatter = new ODLLogFormatter();
                formatter.setExcludeFields(excludeFields);
                formatter.setMultiLineMode(multiLineMode);
                for (Handler handler : logMgr.getLogger("").getHandlers()) {
                    // only get the ConsoleHandler
                    if (handler.getClass().equals(ConsoleHandler.class)) {
                        handler.setFormatter(formatter);
                        break;
                    }
                }
            }

            //setting default attributes value for all properties
            serverLogFileDetail = props.get(SERVER_LOG_FILE_PROPERTY);
            handlerDetail = props.get(HANDLER_PROPERTY);   
            handlerServices = props.get(HANDLER_SERVICES_PROPERTY);
            if (handlerServices == null) {
                handlerServices = "";
            }
            consoleHandlerFormatterDetail = props.get(CONSOLEHANDLER_FORMATTER_PROPERTY);
            gffileHandlerFormatterDetail = props.get(GFFILEHANDLER_FORMATTER_PROPERTY);
            rotationOnTimeLimitInMinutesDetail = props.get(ROTATIONTIMELIMITINMINUTES_PROPERTY);
            flushFrequencyDetail = props.get(FLUSHFREQUENCY_PROPERTY);
            filterHandlerDetails = props.get(FILEHANDLER_LIMIT_PROPERTY);
            logToConsoleDetail = props.get(LOGTOCONSOLE_PROPERTY);
            rotationInTimeLimitInBytesDetail = props.get(ROTATIONLIMITINBYTES_PROPERTY);
            useSystemLoggingDetail = props.get(USESYSTEMLOGGING_PROPERTY);
            fileHandlerCountDetail = props.get(FILEHANDLER_COUNT_PROPERTY);
            retainErrorsStaticticsDetail = props.get(RETAINERRORSSTATICTICS_PROPERTY);
            log4jVersionDetail = props.get(LOG4J_VERSION_PROPERTY);
            maxHistoryFilesDetail = props.get(MAXHISTORY_FILES_PROPERTY);
            rotationOnDateChangeDetail = props.get(ROTATIONONDATECHANGE_PROPERTY);
            fileHandlerPatternDetail = props.get(FILEHANDLER_PATTERN_PROPERTY);
            fileHandlerFormatterDetail = props.get(FILEHANDLER_FORMATTER_PROPERTY);
            logFormatDateFormatDetail = props.get(LOGFORMAT_DATEFORMAT_PROPERTY);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, LogFacade.ERROR_APPLYING_CONF, e);
        }

        Collection<Handler> handlers = getHandlerServices();        
        if (handlers != null && handlers.size() > 0) {
            // add the new handlers to the root logger
            for (Handler handler : handlers) {
                addHandler(handler);
            }            
        }

        // Need to lock Logger.class first before locking LogManager to avoid deadlock.
        // See GLASSFISH-7274 for more details.
        // The synchronization in Logger class is addressed in JDK 1.7 but in JDK 1.6
        // Logger.getLogger() is still synchronized.
        synchronized (java.util.logging.Logger.class) {
            synchronized (logMgr) {
                Enumeration<String> loggerNames = logMgr.getLoggerNames();
                while (loggerNames.hasMoreElements()) {
                    String loggerName = loggerNames.nextElement();
                    Logger logger = logMgr.getLogger(loggerName);
                    if (logger == null) {
                        continue;
                    }
                    for (Handler handler : logger.getHandlers()) {
                        Formatter formatter = handler.getFormatter();
                        if (formatter != null
                                && formatter instanceof UniformLogFormatter) {
                            ((UniformLogFormatter) formatter)
                                    .setDelegate(agentDelegate);
                        }
                    }
                }
            }
        }
        
        // add the filter if there is one
        try {

            Map<String, String> map = getLoggingProperties();

            String filterClassName = map.get(LoggingXMLNames.xmltoPropsMap.get("log-filter"));
            if (filterClassName != null) {
                Filter filterClass = habitat.getService(Filter.class, filterClassName);
                Logger rootLogger = Logger.getLogger("");
                if (rootLogger != null) {
                    rootLogger.setFilter(filterClass);
                }
            }
        } catch (java.io.IOException ex) {

        }

        // redirect stderr and stdout, a better way to do this
        //http://blogs.sun.com/nickstephen/entry/java_redirecting_system_out_and

        Logger _ologger = LogFacade.STDOUT_LOGGER;
        stdoutOutputStream = new LoggingOutputStream(_ologger, Level.INFO);
        LoggingOutputStream.LoggingPrintStream pout = stdoutOutputStream.new LoggingPrintStream(stdoutOutputStream);
        System.setOut(pout);

        Logger _elogger = LogFacade.STDERR_LOGGER;
        stderrOutputStream = new LoggingOutputStream(_elogger, Level.SEVERE);
        LoggingOutputStream.LoggingPrintStream perr = stderrOutputStream.new LoggingPrintStream(stderrOutputStream);
        System.setErr(perr);
                
        // finally listen to changes to the logging.properties file
        if (logging != null) {
            fileMonitoring.monitors(logging, new FileMonitoring.FileChangeListener() {
                public void changed(File changedFile) {
                    synchronized (gfHandlers) {
                        try {

                            Map<String, String> props = getLoggingProperties();
                            loggerReference = new Vector<Logger>();
                            if (props == null)
                                return;
                            // Set<String> keys = props.keySet();
                            for (Map.Entry<String,String> entry : props.entrySet()) {
                                String a = entry.getKey();
                                String val = entry.getValue();
                                if (a.endsWith(".level")) {
                                    String n = a.substring(0, a.lastIndexOf(".level"));
                                    Level l = Level.parse(val);
                                    if (gfHandlers.containsKey(n)) {
                                        // check if this is one of our handlers
                                        Handler h = (Handler) gfHandlers.get(n);
                                        h.setLevel(l);
                                    } else if (n.equals("java.util.logging.ConsoleHandler")) {
                                        Logger logger = Logger.getLogger("");
                                        Handler[] h = logger.getHandlers();
                                        for (int i = 0; i < h.length; i++) {
                                            String name = h[i].toString();
                                            if (name.contains("java.util.logging.ConsoleHandler"))
                                                h[i].setLevel(l);
                                        }
                                    } else {
                                        // Assume it is a logger
                                        Logger appLogger = Logger.getLogger(n);
                                        appLogger.setLevel(l);
                                        loggerReference.add(appLogger);
                                    }
                                } else if (a.equals(SERVER_LOG_FILE_PROPERTY)) {
                                    //check if file name was changed and send notification
                                    if (!val.equals(serverLogFileDetail)) {
                                        PropertyChangeEvent pce = new PropertyChangeEvent(this, a, serverLogFileDetail, val);
                                        UnprocessedChangeEvents ucel = new UnprocessedChangeEvents(new UnprocessedChangeEvent(pce, "server log filename changed."));
                                        List<UnprocessedChangeEvents> b = new ArrayList();
                                        b.add(ucel);
                                        ucl.unprocessedTransactedEvents(b);
                                    }
                                } else if (a.equals(HANDLER_PROPERTY)) {
                                    if (!val.equals(handlerDetail)) {
                                        generateAttributeChangeEvent(HANDLER_PROPERTY, handlerDetail, props);
                                    }
                                } else if (a.equals(HANDLER_SERVICES_PROPERTY)) {
                                    if (!val.equals(handlerServices)) {
                                        generateAttributeChangeEvent(HANDLER_SERVICES_PROPERTY, handlerServices, props);
                                    }
                                } else if (a.equals(CONSOLEHANDLER_FORMATTER_PROPERTY)) {
                                    if (!val.equals(consoleHandlerFormatterDetail)) {
                                        generateAttributeChangeEvent(CONSOLEHANDLER_FORMATTER_PROPERTY, consoleHandlerFormatterDetail, props);
                                    }
                                } else if (a.equals(GFFILEHANDLER_FORMATTER_PROPERTY)) {
                                    if (!val.equals(gffileHandlerFormatterDetail)) {
                                        generateAttributeChangeEvent(GFFILEHANDLER_FORMATTER_PROPERTY, gffileHandlerFormatterDetail, props);
                                    }
                                } else if (a.equals(ROTATIONTIMELIMITINMINUTES_PROPERTY)) {
                                    if (!val.equals(rotationOnTimeLimitInMinutesDetail)) {
                                        generateAttributeChangeEvent(ROTATIONTIMELIMITINMINUTES_PROPERTY, rotationOnTimeLimitInMinutesDetail, props);
                                    }
                                } else if (a.equals(FLUSHFREQUENCY_PROPERTY)) {
                                    if (!val.equals(flushFrequencyDetail)) {
                                        generateAttributeChangeEvent(FLUSHFREQUENCY_PROPERTY, flushFrequencyDetail, props);
                                    }
                                } else if (a.equals(FILEHANDLER_LIMIT_PROPERTY)) {
                                    if (!val.equals(filterHandlerDetails)) {
                                        generateAttributeChangeEvent(FILEHANDLER_LIMIT_PROPERTY, filterHandlerDetails, props);
                                    }
                                } else if (a.equals(LOGTOCONSOLE_PROPERTY)) {
                                    if (!val.equals(logToConsoleDetail)) {
                                        generateAttributeChangeEvent(LOGTOCONSOLE_PROPERTY, logToConsoleDetail, props);
                                    }
                                } else if (a.equals(ROTATIONLIMITINBYTES_PROPERTY)) {
                                    if (!val.equals(rotationInTimeLimitInBytesDetail)) {
                                        generateAttributeChangeEvent(ROTATIONLIMITINBYTES_PROPERTY, rotationInTimeLimitInBytesDetail, props);
                                    }
                                } else if (a.equals(USESYSTEMLOGGING_PROPERTY)) {
                                    if (!val.equals(useSystemLoggingDetail)) {
                                        generateAttributeChangeEvent(USESYSTEMLOGGING_PROPERTY, useSystemLoggingDetail, props);
                                    }
                                } else if (a.equals(FILEHANDLER_COUNT_PROPERTY)) {
                                    if (!val.equals(fileHandlerCountDetail)) {
                                        generateAttributeChangeEvent(FILEHANDLER_COUNT_PROPERTY, fileHandlerCountDetail, props);
                                    }
                                } else if (a.equals(RETAINERRORSSTATICTICS_PROPERTY)) {
                                    if (!val.equals(retainErrorsStaticticsDetail)) {
                                        generateAttributeChangeEvent(RETAINERRORSSTATICTICS_PROPERTY, retainErrorsStaticticsDetail, props);
                                    }
                                } else if (a.equals(LOG4J_VERSION_PROPERTY)) {
                                    if (!val.equals(log4jVersionDetail)) {
                                        generateAttributeChangeEvent(LOG4J_VERSION_PROPERTY, log4jVersionDetail, props);
                                    }
                                } else if (a.equals(MAXHISTORY_FILES_PROPERTY)) {
                                    if (!val.equals(maxHistoryFilesDetail)) {
                                        generateAttributeChangeEvent(MAXHISTORY_FILES_PROPERTY, maxHistoryFilesDetail, props);
                                    }
                                } else if (a.equals(ROTATIONONDATECHANGE_PROPERTY)) {
                                    if (!val.equals(rotationOnDateChangeDetail)) {
                                        generateAttributeChangeEvent(ROTATIONONDATECHANGE_PROPERTY, rotationOnDateChangeDetail, props);
                                    }
                                } else if (a.equals(FILEHANDLER_PATTERN_PROPERTY)) {
                                    if (!val.equals(fileHandlerPatternDetail)) {
                                        generateAttributeChangeEvent(FILEHANDLER_PATTERN_PROPERTY, fileHandlerPatternDetail, props);
                                    }
                                } else if (a.equals(FILEHANDLER_FORMATTER_PROPERTY)) {
                                    if (!val.equals(fileHandlerFormatterDetail)) {
                                        generateAttributeChangeEvent(FILEHANDLER_FORMATTER_PROPERTY, fileHandlerFormatterDetail, props);
                                    }
                                } else if (a.equals(LOGFORMAT_DATEFORMAT_PROPERTY)) {
                                    if (!val.equals(logFormatDateFormatDetail)) {
                                        generateAttributeChangeEvent(LOGFORMAT_DATEFORMAT_PROPERTY, logFormatDateFormatDetail, props);
                                    }
                                } else if (a.equals(EXCLUDE_FIELDS_PROPERTY)) {
                                    val = (val == null) ? "" : val;
                                    excludeFields = (excludeFields == null) ? "" : excludeFields;
                                    if (!val.equals(excludeFields)) {
                                        generateAttributeChangeEvent(EXCLUDE_FIELDS_PROPERTY, excludeFields, props);
                                    }
                                } else if (a.equals(MULTI_LINE_MODE_PROPERTY)) {
                                    String oldVal = Boolean.toString(multiLineMode);
                                    if (!val.equalsIgnoreCase(oldVal)) {
                                        generateAttributeChangeEvent(MULTI_LINE_MODE_PROPERTY, oldVal, props);
                                    }
                                }
                            }

                            LOGGER.log(Level.INFO, LogFacade.UPDATED_LOG_LEVELS);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, LogFacade.ERROR_APPLYING_CONF, e);
                        }
                    }

                }

                public void deleted(File deletedFile) {
                    LOGGER.log(Level.WARNING, LogFacade.CONF_FILE_DELETED, deletedFile.getAbsolutePath());
                }
            });
        }
        // Log the messages that were generated very early before this Service
        // started.  Just use our own logger...
        List<EarlyLogger.LevelAndMessage> catchUp = EarlyLogger.getEarlyMessages();

        if (!catchUp.isEmpty()) {
            for (EarlyLogger.LevelAndMessage levelAndMessage : catchUp) {
                LOGGER.log(levelAndMessage.getLevel(), levelAndMessage.getMessage());
            }
            catchUp.clear();
        }

        ArrayBlockingQueue<LogRecord> catchEarlyMessage = EarlyLogHandler.earlyMessages;

        while (!catchEarlyMessage.isEmpty()) {
            LogRecord logRecord = catchEarlyMessage.poll();
            if (logRecord != null) {
                LOGGER.log(logRecord);
            }
        }
    }

    private Collection<Handler> getHandlerServices() {
        Set<String> handlerServicesSet = new HashSet<String>();
        handlerServicesSet.add(GFFileHandler.class.getName());
        String[] handlerServicesArray = handlerServices.split(",");
        for (String handlerService : handlerServicesArray) {
            handlerServicesSet.add(handlerService);
        }
        List<Handler> handlers = habitat.getAllServices(Handler.class);
        List<Handler> result = new ArrayList<Handler>();
        List<Handler> customHandlers = new ArrayList<Handler>();
        GFFileHandler gfFileHandler = null;
        for (Handler handler: handlers) {
            String handlerClassName = handler.getClass().getName();
            if (handlerServicesSet.contains(handlerClassName )) {
                result.add(handler);
            }
            if (handlerClassName.equals(GFFileHandler.class.getName())) {
                gfFileHandler = (GFFileHandler) handler;
            } else {
                customHandlers.add(handler);
            }
        }
        
        // Set formatter on custom handler service if configured
        for (Handler handler : customHandlers) {
            try {
                Map<String, String> props = getLoggingProperties();
                String handlerClassName = handler.getClass().getName();
                String formatterClassName = props.get(handlerClassName+".formatter");
                Formatter formatter = getCustomFormatter(formatterClassName, gfFileHandler);
                if (formatter != null) {
                    handler.setFormatter(formatter);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, LogFacade.ERROR_APPLYING_CONF, e);
            }
        }        
        return result;
    }

    private Formatter getCustomFormatter(String formatterClassName, 
            GFFileHandler gfFileHandler) 
    {
        try {
            Class customFormatterClass = 
                    ClassLoader.getSystemClassLoader().loadClass(
                            formatterClassName);
            return (Formatter) customFormatterClass.newInstance();
        } catch (Exception e) {
            return gfFileHandler.findFormatterService(formatterClassName);
        }
    }

    public void generateAttributeChangeEvent(String property, String propertyDetail, Map props) {
        PropertyChangeEvent pce = new PropertyChangeEvent(this, property, propertyDetail, props.get(property));
        UnprocessedChangeEvents ucel = new UnprocessedChangeEvents(new UnprocessedChangeEvent(pce, "server log file attribute " + property + " changed."));
        List<UnprocessedChangeEvents> b = new ArrayList();
        b.add(ucel);
        ucl.unprocessedTransactedEvents(b);
    }

    public void addHandler(Handler handler) {
        Logger rootLogger = Logger.getLogger("");
        if (rootLogger != null) {
            synchronized (gfHandlers) {
                rootLogger.addHandler(handler);
                String handlerName = handler.toString();
                gfHandlers.put(handlerName.substring(0, handlerName.indexOf("@")), handler);
            }
        }
    }


    public void preDestroy() {
        //destroy the handlers
        try {
            for (ServiceHandle<?> i : habitat.getAllServiceHandles(BuilderHelper.createContractFilter(Handler.class.getName()))) {
                i.destroy();
            }
            System.setOut(oStdOutBackup);
            System.setErr(oStdErrBackup);
            if (stdoutOutputStream != null) {
                stdoutOutputStream.close();
            }
            if (stderrOutputStream != null) {
                stderrOutputStream.close();
            }
            System.out.println("Completed shutdown of Log manager service");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
