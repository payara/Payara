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
// Portions Copyright [2016-2025] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.server.logging;

import com.sun.common.util.logging.GFLogRecord;
import com.sun.common.util.logging.LoggingConfig;
import com.sun.common.util.logging.LoggingConfigFactory;
import com.sun.common.util.logging.LoggingXMLNames;
import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.module.bootstrap.EarlyLogHandler;
import com.sun.enterprise.util.EarlyLogger;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.logging.AgentFormatterDelegate;
import fish.payara.enterprise.server.logging.JSONLogFormatter;
import fish.payara.enterprise.server.logging.PayaraNotificationFileHandler;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
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
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * Reinitialise the log manager using our logging.properties file.
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
    LoggingConfigFactory loggingConfigFactory;

    @Inject
    UnprocessedConfigListener ucl;

    @Inject
    Domain domain;
    
    final Map<String, Handler> gfHandlers = new HashMap<>();

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
    String logToFileDetail = "";
    String logToConsoleDetail = "";
    String rotationInTimeLimitInBytesDetail = "";
    String useSystemLoggingDetail = "";
    String systemLoggingHostDetail = "";
    String fileHandlerCountDetail = "";
    String retainErrorsStaticticsDetail = "";
    String log4jVersionDetail = "";
    String maxHistoryFilesDetail = "";
    String rotationOnDateChangeDetail = "";
    String fileHandlerPatternDetail = "";
    String fileHandlerFormatterDetail = "";
    String logFormatDateFormatDetail = "";
    String compressOnRotationDetail = "";
    String logStandardStreamsDetail = "";
    String fastLoggingDetail = "";
    
    //Payara Notification Logging   
    String payaraNotificationLogFileDetail = "";
    String payaraNotificationlogToFileDetail = "";
    String payaraNotificationLogRotationOnTimeLimitInMinutesDetail = "";
    String payaraNotificationLogRotationOnDateChangeDetail = "";
    String payaraNotificationLogRotationLimitInBytesDetail = "";
    String payaraNotificationLogmaxHistoryFilesDetail = "";
    String payaraNotificationLogCompressOnRotationDetail = "";
    String payaraNotificationLogFormatterDetail = "";
    
    String payaraJsonUnderscorePrefix = "";

    private static final String SERVER_LOG_FILE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.file";
    private static final String HANDLER_PROPERTY = "handlers";
    private static final String HANDLER_SERVICES_PROPERTY = "handlerServices";
    private static final String CONSOLEHANDLER_FORMATTER_PROPERTY = "java.util.logging.ConsoleHandler.formatter";
    private static final String GFFILEHANDLER_FORMATTER_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.formatter";
    private static final String ROTATIONTIMELIMITINMINUTES_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.rotationTimelimitInMinutes";
    private static final String FLUSHFREQUENCY_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.flushFrequency";
    private static final String FILEHANDLER_LIMIT_PROPERTY = "java.util.logging.FileHandler.limit";
    private static final String LOGTOFILE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logtoFile";
    private static final String LOGTOCONSOLE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logtoConsole";
    private static final String ROTATIONLIMITINBYTES_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.rotationLimitInBytes";
    private static final String USESYSTEMLOGGING_PROPERTY = "com.sun.enterprise.server.logging.SyslogHandler.useSystemLogging";
    private static final String SYSTEMLOGGINGHOST_PROPERTY = "com.sun.enterprise.server.logging.SyslogHandler.host";
    private static final String FILEHANDLER_COUNT_PROPERTY = "java.util.logging.FileHandler.count";
    private static final String RETAINERRORSSTATICTICS_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.retainErrorsStasticsForHours";
    private static final String LOG4J_VERSION_PROPERTY = "log4j.logger.org.hibernate.validator.util.Version";
    private static final String MAXHISTORY_FILES_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.maxHistoryFiles";
    private static final String ROTATIONONDATECHANGE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.rotationOnDateChange";
    private static final String FILEHANDLER_PATTERN_PROPERTY = "java.util.logging.FileHandler.pattern";
    private static final String FILEHANDLER_FORMATTER_PROPERTY = "java.util.logging.FileHandler.formatter";
    private static final String LOGFORMAT_DATEFORMAT_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logFormatDateFormat";
    private static final String COMPRESS_ON_ROTATION_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.compressOnRotation";
    private static final String LOG_STANDARD_STREAMS_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logStandardStreams";
    private static final String FAST_LOGGER_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.fastLogging";
    
    //Payara Notification Logging
    private static final String PAYARA_NOTIFICATION_LOG_FILE_PROPERTY = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.file";
    private static final String PAYARA_NOTIFICATION_LOGTOFILE_PROPERTY = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.logtoFile";
    private static final String PAYARA_NOTIFICATION_LOG_ROTATIONONDATECHANGE_PROPERTY = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.rotationOnDateChange";
    private static final String PAYARA_NOTIFICATION_LOG_ROTATIONTIMELIMITINMINUTES_PROPERTY = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.rotationTimelimitInMinutes";
    private static final String PAYARA_NOTIFICATION_LOG_ROTATIONLIMITINBYTES_PROPERTY = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.rotationLimitInBytes";
    private static final String PAYARA_NOTIFICATION_LOG_MAXHISTORY_FILES_PROPERTY = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.maxHistoryFiles";
    private static final String PAYARA_NOTIFICATION_LOG_COMPRESS_ON_ROTATION_PROPERTY = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.compressOnRotation";
    private static final String PAYARA_NOTIFICATION_LOG_FORMATTER_PROPERTY = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.formatter";
     
    private static final String PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG = "Payara Notification Service isn't using a separate Log File";
    /**
     * @deprecated for backwards compatibility pre-5.182
     */
    @Deprecated
    private static final String PAYARA_JSONLOGFORMATTER_UNDERSCORE="fish.payara.deprecated.jsonlogformatter.underscoreprefix";
    
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

    private  GFFileHandler gfFileHandler = null;
    
    private  PayaraNotificationFileHandler pyFileHandler = null;
    
    private String payaraNotificationLogger = "fish.payara.nucleus.notification.log.LogNotifier";
    
    /**
     * Returns properties based on the DAS/Cluster/Instance
     */
    @Override
    public Map<String, String> getLoggingProperties() throws IOException {

        Server targetServer = domain.getServerNamed(env.getInstanceName());

        // Find the logging config
        LoggingConfig loggingConfig = loggingConfigFactory.provide();
        if (targetServer != null && !targetServer.isDas()) {
            if (targetServer.getCluster() != null) {
                loggingConfig = loggingConfigFactory.provide(targetServer.getCluster().getConfigRef());
            } else if (targetServer.isInstance()) {
                loggingConfig = loggingConfigFactory.provide(targetServer.getConfigRef());
            }
        }

        // Validate the properties
        Map<String, String> props = loggingConfig.getLoggingProperties();
        Map<String, String> invalidProps = validateProps(props);
        if (!invalidProps.isEmpty()) {
            loggingConfig.deleteLoggingProperties(invalidProps);
            props = loggingConfig.getLoggingProperties();
        }

        return props;
    }

    /**
     * Validates the map of logging properties. Will remove any properties from the
     * map that don't pass the validation, and then throw an exception at the very
     * end.
     * 
     * @param props the map of properties to validate. WILL BE MODIFIED.
     * @return a map of invalid properties. Will never be null.
     */
    public Map<String, String> validateProps(Map<String, String> props) {
        Map<String, String> invalidProps = new HashMap<>();
        Iterator<Entry<String, String>> propertyIterator = props.entrySet().iterator();
        while (propertyIterator.hasNext()) {
            Entry<String, String> propertyEntry = propertyIterator.next();

            try {
                validateProp(propertyEntry.getKey(), propertyEntry.getValue());
            } catch (ValidationException ex) {
                LOGGER.log(Level.WARNING, "Error validating log property.", ex);
                propertyIterator.remove();
                invalidProps.put(propertyEntry.getKey(), propertyEntry.getValue());
            }
        }
        return invalidProps;
    }

    /**
     * Validates a property. Throws an exception if validation fails.
     * 
     * @param key   the attribute name to validate.
     * @param value the attribute value to validate.
     * @throws ValidationException if validation fails.
     */
    public void validateProp(String key, String value) {
        if (key.equals(ROTATIONLIMITINBYTES_PROPERTY)) {
            int rotationSizeLimit = Integer.parseInt(value);
            if (rotationSizeLimit != GFFileHandler.DISABLE_LOG_FILE_ROTATION_VALUE
                    && rotationSizeLimit < GFFileHandler.MINIMUM_ROTATION_LIMIT_VALUE) {
                throw new ValidationException(String.format("'%s' value must be greater than %d, but was %d.",
                        ROTATIONLIMITINBYTES_PROPERTY, GFFileHandler.MINIMUM_ROTATION_LIMIT_VALUE, rotationSizeLimit));
            }
        } else if (key.equals(ROTATIONTIMELIMITINMINUTES_PROPERTY)) {
            int rotationTimeLimit = Integer.parseInt(value);
            if (rotationTimeLimit < 0) {
                throw new ValidationException(String.format("'%s' value must be greater than %d, but was %d.",
                        ROTATIONTIMELIMITINMINUTES_PROPERTY, 0, rotationTimeLimit));
            }

        } else if (key.equals(PAYARA_NOTIFICATION_LOG_ROTATIONTIMELIMITINMINUTES_PROPERTY)) {
            int PayaraNotificationRotationTimeLimit = Integer.parseInt(value);
            if (PayaraNotificationRotationTimeLimit < 0) {
                throw new ValidationException(String.format("'%s' value must be greater than %d, but was %d.",
                        PAYARA_NOTIFICATION_LOG_ROTATIONTIMELIMITINMINUTES_PROPERTY, 0,
                        PayaraNotificationRotationTimeLimit));
            }
        }
    }

    /**
     * Returns logging file to be monitor during server is running.
     */
    @Override
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
    @Override
    public void postConstruct() {

        // if the system property is already set, we don't need to do anything
         if (System.getProperty("java.util.logging.config.file") != null) {
            System.out.println("\n\n\n#!## LogManagerService.postConstruct : java.util.logging.config.file=" +System.getProperty("java.util.logging.config.file"));

            return;
        }

        // logging.properties massaging.
        final LogManager logMgr = LogManager.getLogManager();
        File loggingPropertiesFile = null;

        // reset settings
        try {

            loggingPropertiesFile = getLoggingFile();
            System.setProperty("java.util.logging.config.file", loggingPropertiesFile.getAbsolutePath());
            
            String rootFolder = env.getProps().get(com.sun.enterprise.util.SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
            String templateDir = rootFolder + File.separator + "lib" + File.separator + "templates";
            File src = new File(templateDir, ServerEnvironmentImpl.kLoggingPropertiesFileName);
            File dest = new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);

            System.out.println("\n\n\n#!## LogManagerService.postConstruct : rootFolder=" +rootFolder);
            System.out.println("#!## LogManagerService.postConstruct : templateDir=" +templateDir);
            System.out.println("#!## LogManagerService.postConstruct : src=" +src);
            System.out.println("#!## LogManagerService.postConstruct : dest=" +dest);

            if (!loggingPropertiesFile.exists()) {
                LOGGER.log(Level.FINE, "{0} not found, creating new file from template.", loggingPropertiesFile.getAbsolutePath());
                FileUtils.copy(src, dest);
                loggingPropertiesFile = new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
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
            setConsoleHandlerLogFormat(formatterClassName, props, logMgr);

            //setting default attributes value for all properties
            setDefaultLoggingProperties(props);

            Collection<Handler> handlers = getHandlerServices(props);
            if (handlers != null && handlers.size() > 0) {
                // add the new handlers to the root logger
                for (Handler handler : handlers) {
                    addHandler(handler);
                }
            }

            // add the filter if there is one
            String filterClassName = props.get(LoggingXMLNames.xmltoPropsMap.get("log-filter"));
            if (filterClassName != null) {
                Filter filterClass = habitat.getService(Filter.class, filterClassName);
                Logger rootLogger = Logger.getLogger("");
                if (rootLogger != null) {
                    rootLogger.setFilter(filterClass);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, LogFacade.ERROR_APPLYING_CONF, e);
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
             
        // finally listen to changes to the loggingPropertiesFile.properties file
        listenToChangesOnloggingPropsFile(loggingPropertiesFile, logMgr);
   
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

    public void listenToChangesOnloggingPropsFile(File loggingPropertiesFile, LogManager logMgr){
             if (loggingPropertiesFile != null) {
            fileMonitoring.monitors(loggingPropertiesFile, new FileMonitoring.FileChangeListener() {
                @Override
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
                                    if (!val.equals(serverLogFileDetail)) {
                                        serverLogFileDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setLogFile(serverLogFileDetail);
                                                break;
                                            }
                                        }
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
                                        consoleHandlerFormatterDetail = val;
                                        setConsoleHandlerLogFormat(consoleHandlerFormatterDetail, props, logMgr);
                                    }
                                } else if (a.equals(GFFILEHANDLER_FORMATTER_PROPERTY)) {
                                    if (!val.equals(gffileHandlerFormatterDetail)) {
                                        gffileHandlerFormatterDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setFileHandlerFormatter(gffileHandlerFormatterDetail);
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(PAYARA_NOTIFICATION_LOG_FORMATTER_PROPERTY)) {
                                    if (!val.equals(payaraNotificationLogFormatterDetail)) {
                                        Handler[] payaraNotificationLogFileHandlers = logMgr.getLogger(payaraNotificationLogger).getHandlers();
                                        if (payaraNotificationLogFileHandlers.length > 0) {
                                            payaraNotificationLogFormatterDetail = val;
                                            for (Handler handler : payaraNotificationLogFileHandlers) {
                                                if (handler.getClass().equals(PayaraNotificationFileHandler.class)) {
                                                    pyFileHandler = (PayaraNotificationFileHandler) handler;
                                                    pyFileHandler.setFileHandlerFormatter(payaraNotificationLogFormatterDetail);
                                                    break;
                                                }
                                            }
                                        } else {
                                            LOGGER.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
                                        }
                                    }
                                } else if (a.equals(ROTATIONTIMELIMITINMINUTES_PROPERTY)) {
                                    if (!val.equals(rotationOnTimeLimitInMinutesDetail)) {
                                        rotationOnTimeLimitInMinutesDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setRotationTimeLimitValue(Long.parseLong(rotationOnTimeLimitInMinutesDetail));
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(FLUSHFREQUENCY_PROPERTY)) {
                                    if (!val.equals(flushFrequencyDetail)) {
                                        flushFrequencyDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setFlushFrequency(Integer.parseInt(flushFrequencyDetail));
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(FILEHANDLER_LIMIT_PROPERTY)) {
                                    if (!val.equals(filterHandlerDetails)) {
                                        generateAttributeChangeEvent(FILEHANDLER_LIMIT_PROPERTY, filterHandlerDetails, props);
                                    }
                                } else if (a.equals(LOGTOFILE_PROPERTY)) {
                                    if (!val.equals(logToFileDetail)) {
                                        logToFileDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setLogToFile(Boolean.parseBoolean(logToFileDetail));
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(LOGTOCONSOLE_PROPERTY)) {
                                    if (!val.equals(logToConsoleDetail)) {
                                        //generateAttributeChangeEvent(LOGTOCONSOLE_PROPERTY, logToConsoleDetail, props);
                                    }
                                } else if (a.equals(ROTATIONLIMITINBYTES_PROPERTY)) {
                                    if (!val.equals(rotationInTimeLimitInBytesDetail)) {
                                        rotationInTimeLimitInBytesDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setRotationLimitAttrValue(Integer.valueOf(rotationInTimeLimitInBytesDetail));
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(USESYSTEMLOGGING_PROPERTY)) {
                                    if (!val.equals(useSystemLoggingDetail)) {
                                        useSystemLoggingDetail = val;
                                        SyslogHandler syslogHandler;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(SyslogHandler.class)) {
                                                syslogHandler = (SyslogHandler) handler;
                                                syslogHandler.setSystemLogging(Boolean.parseBoolean(useSystemLoggingDetail), systemLoggingHostDetail);
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(SYSTEMLOGGINGHOST_PROPERTY)) {
                                    if (!val.equals(systemLoggingHostDetail)) {
                                        systemLoggingHostDetail = val;
                                        SyslogHandler syslogHandler;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(SyslogHandler.class)) {
                                                syslogHandler = (SyslogHandler) handler;
                                                syslogHandler.setSystemLogging(Boolean.parseBoolean(useSystemLoggingDetail), systemLoggingHostDetail);
                                                break;
                                            }
                                        }
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
                                        maxHistoryFilesDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setMaxHistoryFiles(Integer.parseInt(maxHistoryFilesDetail));
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(ROTATIONONDATECHANGE_PROPERTY)) {
                                    if (!val.equals(rotationOnDateChangeDetail)) {
                                        rotationOnDateChangeDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;

                                                gfFileHandler.setRotationOnDateChange(Boolean.parseBoolean(rotationOnDateChangeDetail));
                                                break;
                                            }
                                        }
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
                                        excludeFields = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setExcludeFields(excludeFields);
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(MULTI_LINE_MODE_PROPERTY)) {
                                    String oldVal = Boolean.toString(multiLineMode);
                                    if (!val.equalsIgnoreCase(oldVal)) {
                                        multiLineMode = Boolean.parseBoolean(val);
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setMultiLineMode(multiLineMode);
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(FAST_LOGGER_PROPERTY)) {
                                    if (!val.equals(fastLoggingDetail)) {
                                        fastLoggingDetail = val;
                                        GFLogRecord.fastLoggingAtomic.set(Boolean.parseBoolean(fastLoggingDetail));
                                    }
                                } else if (a.equals(COMPRESS_ON_ROTATION_PROPERTY)) {
                                    if (!val.equals(compressOnRotationDetail)) {
                                        compressOnRotationDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setCompressionOnRotation(Boolean.parseBoolean(compressOnRotationDetail));
                                                break;
                                            }
                                        }
                                    }
                                } else if (a.equals(LOG_STANDARD_STREAMS_PROPERTY)) {
                                    if (!val.equals(logStandardStreamsDetail)) {
                                        logStandardStreamsDetail = val;
                                        for (Handler handler : logMgr.getLogger("").getHandlers()) {
                                            // only get the GFFileHandler
                                            if (handler.getClass().equals(GFFileHandler.class)) {
                                                gfFileHandler = (GFFileHandler) handler;
                                                gfFileHandler.setLogStandardStreams(Boolean.parseBoolean(logStandardStreamsDetail));
                                                break;
                                            }
                                        }
                                    }
                                }else if (a.equals(PAYARA_NOTIFICATION_LOG_FILE_PROPERTY)) {
                                    if (!val.equals(payaraNotificationLogFileDetail)) {
                                        Handler[] payaraNotificationLogFileHandlers = logMgr.getLogger(payaraNotificationLogger).getHandlers();
                                        if (payaraNotificationLogFileHandlers.length > 0) {
                                            payaraNotificationLogFileDetail = val;
                                            for (Handler handler : payaraNotificationLogFileHandlers) {
                                                if (handler.getClass().equals(PayaraNotificationFileHandler.class)) {
                                                    pyFileHandler = (PayaraNotificationFileHandler) handler;
                                                    pyFileHandler.setLogFile(payaraNotificationLogFileDetail);
                                                    break;
                                                }
                                            }
                                        } else {
                                            LOGGER.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
                                        }
                                    }
                                } else if (a.equals(PAYARA_NOTIFICATION_LOGTOFILE_PROPERTY)) {
                                    if (!val.equals(payaraNotificationlogToFileDetail)) {
                                        Handler[] payaraNotificationLogFileHandlers = logMgr.getLogger(payaraNotificationLogger).getHandlers();
                                        if (payaraNotificationLogFileHandlers.length > 0) {
                                            payaraNotificationlogToFileDetail = val;
                                            for (Handler handler : payaraNotificationLogFileHandlers) {
                                                if (handler.getClass().equals(PayaraNotificationFileHandler.class)) {
                                                    pyFileHandler = (PayaraNotificationFileHandler) handler;
                                                    pyFileHandler.setLogToFile(Boolean.parseBoolean(val));
                                                    break;
                                                }
                                            }
                                        } else {
                                            LOGGER.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
                                        }
                                    }
                                } else if (a.equals(PAYARA_NOTIFICATION_LOG_ROTATIONTIMELIMITINMINUTES_PROPERTY)) {
                                    if (!val.equals(payaraNotificationLogRotationOnTimeLimitInMinutesDetail)) {
                                        Handler[] payaraNotificationLogFileHandlers = logMgr.getLogger(payaraNotificationLogger).getHandlers();
                                        if (payaraNotificationLogFileHandlers.length > 0) {
                                            payaraNotificationLogRotationOnTimeLimitInMinutesDetail = val;
                                            for (Handler handler : payaraNotificationLogFileHandlers) {
                                                if (handler.getClass().equals(PayaraNotificationFileHandler.class)) {
                                                    pyFileHandler = (PayaraNotificationFileHandler) handler;
                                                    pyFileHandler.setRotationTimeLimitValue(Long.parseLong(val));
                                                    break;
                                                }
                                            }
                                        } else {
                                            LOGGER.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
                                        }
                                    }
                                } else if (a.equals(PAYARA_NOTIFICATION_LOG_ROTATIONLIMITINBYTES_PROPERTY)) {
                                    if (!val.equals(payaraNotificationLogRotationLimitInBytesDetail)) {
                                        Handler[] payaraNotificationLogFileHandlers = logMgr.getLogger(payaraNotificationLogger).getHandlers();
                                        if (payaraNotificationLogFileHandlers.length > 0) {
                                            payaraNotificationLogRotationLimitInBytesDetail = val;
                                            for (Handler handler : payaraNotificationLogFileHandlers) {
                                                if (handler.getClass().equals(PayaraNotificationFileHandler.class)) {
                                                    pyFileHandler = (PayaraNotificationFileHandler) handler;
                                                    pyFileHandler.setRotationLimitAttrValue(Integer.valueOf(val));
                                                    break;
                                                }
                                            }                                           
                                        } else {
                                            LOGGER.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
                                        }
                                    }
                                } else if (a.equals(PAYARA_NOTIFICATION_LOG_ROTATIONONDATECHANGE_PROPERTY)) {
                                    if (!val.equals(payaraNotificationLogRotationOnDateChangeDetail)) {
                                        Handler[] payaraNotificationLogFileHandlers = logMgr.getLogger(payaraNotificationLogger).getHandlers();
                                        if (payaraNotificationLogFileHandlers.length > 0) {
                                            payaraNotificationLogRotationOnDateChangeDetail = val;
                                            for (Handler handler : payaraNotificationLogFileHandlers) {
                                                if (handler.getClass().equals(PayaraNotificationFileHandler.class)) {
                                                    pyFileHandler = (PayaraNotificationFileHandler) handler;
                                                    pyFileHandler.setRotationOnDateChange(Boolean.parseBoolean(val));
                                                    break;
                                                }
                                            }
                                        } else {
                                            LOGGER.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
                                        }
                                    }
                                } else if (a.equals(PAYARA_NOTIFICATION_LOG_MAXHISTORY_FILES_PROPERTY)) {
                                    if (!val.equals(payaraNotificationLogmaxHistoryFilesDetail)) {
                                        Handler[] payaraNotificationLogFileHandlers = logMgr.getLogger(payaraNotificationLogger).getHandlers();
                                        if (payaraNotificationLogFileHandlers.length > 0) {
                                             payaraNotificationLogmaxHistoryFilesDetail = val;
                                            for (Handler handler : payaraNotificationLogFileHandlers) {
                                                if (handler.getClass().equals(PayaraNotificationFileHandler.class)) {
                                                    pyFileHandler = (PayaraNotificationFileHandler) handler;
                                                    pyFileHandler.setMaxHistoryFiles(Integer.parseInt(val));
                                                    break;
                                                }
                                            }                                           
                                        } else {
                                            LOGGER.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
                                        }
                                    }
                                } else if (a.equals(PAYARA_NOTIFICATION_LOG_COMPRESS_ON_ROTATION_PROPERTY)) {
                                    if (!val.equals(payaraNotificationLogCompressOnRotationDetail)) {
                                        Handler[] payaraNotificationLogFileHandlers = logMgr.getLogger(payaraNotificationLogger).getHandlers();
                                        if (payaraNotificationLogFileHandlers.length > 0) {
                                            payaraNotificationLogCompressOnRotationDetail = val;
                                            for (Handler handler : payaraNotificationLogFileHandlers) {
                                                if (handler.getClass().equals(PayaraNotificationFileHandler.class)) {
                                                    pyFileHandler = (PayaraNotificationFileHandler) handler;
                                                    pyFileHandler.setCompressionOnRotation(Boolean.parseBoolean(val));
                                                    break;
                                                }
                                            }                                          
                                        } else {
                                            LOGGER.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
                                        }
                                    }
                                }
                            }

                            LOGGER.log(Level.INFO, LogFacade.UPDATED_LOG_LEVELS);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, LogFacade.ERROR_APPLYING_CONF, e);
                        }
                    }

                }

                @Override
                public void deleted(File deletedFile) {
                    LOGGER.log(Level.WARNING, LogFacade.CONF_FILE_DELETED, deletedFile.getAbsolutePath());
                }
            });
        }
    }
    private void setConsoleHandlerLogFormat(String formatterClassName, Map<String, String> props, LogManager logMgr) {
        if (formatterClassName == null || formatterClassName.isEmpty()) {
            formatterClassName = UniformLogFormatter.class.getName();
        }
        consoleHandlerFormatterDetail = formatterClassName;
        excludeFields = props.get(EXCLUDE_FIELDS_PROPERTY);
        multiLineMode = Boolean.parseBoolean(props.get(MULTI_LINE_MODE_PROPERTY));
        if (formatterClassName.equals(UniformLogFormatter.class.getName())) {
            // used to support UFL formatter in GF.
            UniformLogFormatter formatter = new UniformLogFormatter(excludeFields);
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
            ODLLogFormatter formatter = new ODLLogFormatter(excludeFields);
            formatter.setMultiLineMode(multiLineMode);
            for (Handler handler : logMgr.getLogger("").getHandlers()) {
                // only get the ConsoleHandler
                if (handler.getClass().equals(ConsoleHandler.class)) {
                    handler.setFormatter(formatter);
                    break;
                }
            }
        } else if (formatterClassName.equals(JSONLogFormatter.class.getName())) {
            JSONLogFormatter formatter = new JSONLogFormatter(excludeFields);
            for (Handler handler : logMgr.getLogger("").getHandlers()) {
                // only get the ConsoleHandler
                if (handler.getClass().equals(ConsoleHandler.class)) {
                    handler.setFormatter(formatter);
                    break;
                }
            }
        }
    }
    
    private void setDefaultLoggingProperties(Map<String, String> props) {
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
        logToFileDetail = props.get(LOGTOFILE_PROPERTY);
        logToConsoleDetail = props.get(LOGTOCONSOLE_PROPERTY);
        rotationInTimeLimitInBytesDetail = props.get(ROTATIONLIMITINBYTES_PROPERTY);
        useSystemLoggingDetail = props.get(USESYSTEMLOGGING_PROPERTY);
        systemLoggingHostDetail = props.get(SYSTEMLOGGINGHOST_PROPERTY);
        fileHandlerCountDetail = props.get(FILEHANDLER_COUNT_PROPERTY);
        retainErrorsStaticticsDetail = props.get(RETAINERRORSSTATICTICS_PROPERTY);
        log4jVersionDetail = props.get(LOG4J_VERSION_PROPERTY);
        maxHistoryFilesDetail = props.get(MAXHISTORY_FILES_PROPERTY);
        rotationOnDateChangeDetail = props.get(ROTATIONONDATECHANGE_PROPERTY);
        fileHandlerPatternDetail = props.get(FILEHANDLER_PATTERN_PROPERTY);
        fileHandlerFormatterDetail = props.get(FILEHANDLER_FORMATTER_PROPERTY);
        logFormatDateFormatDetail = props.get(LOGFORMAT_DATEFORMAT_PROPERTY);
        compressOnRotationDetail = props.get(COMPRESS_ON_ROTATION_PROPERTY);
        logStandardStreamsDetail = props.get(LOG_STANDARD_STREAMS_PROPERTY);
        fastLoggingDetail = props.get(FAST_LOGGER_PROPERTY);

        //Payara Notification Logging
        payaraNotificationLogFileDetail = props.get(PAYARA_NOTIFICATION_LOG_FILE_PROPERTY);
        payaraNotificationlogToFileDetail = props.get(PAYARA_NOTIFICATION_LOGTOFILE_PROPERTY);
        payaraNotificationLogRotationOnDateChangeDetail = props.get(PAYARA_NOTIFICATION_LOG_ROTATIONONDATECHANGE_PROPERTY);
        payaraNotificationLogRotationOnTimeLimitInMinutesDetail = props.get(PAYARA_NOTIFICATION_LOG_ROTATIONTIMELIMITINMINUTES_PROPERTY);
        payaraNotificationLogRotationLimitInBytesDetail = props.get(PAYARA_NOTIFICATION_LOG_ROTATIONLIMITINBYTES_PROPERTY);
        payaraNotificationLogmaxHistoryFilesDetail = props.get(PAYARA_NOTIFICATION_LOG_MAXHISTORY_FILES_PROPERTY);
        payaraNotificationLogCompressOnRotationDetail = props.get(PAYARA_NOTIFICATION_LOG_COMPRESS_ON_ROTATION_PROPERTY);
        payaraNotificationLogFormatterDetail = props.get(PAYARA_NOTIFICATION_LOG_FORMATTER_PROPERTY);

        payaraJsonUnderscorePrefix = props.get(PAYARA_JSONLOGFORMATTER_UNDERSCORE);
    }

    private Collection<Handler> getHandlerServices(Map<String, String> props) {
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
            }
            else if (!handlerClassName.equals(PayaraNotificationFileHandler.class.getName())) {
                customHandlers.add(handler);
            }
        }
        
        // Set formatter on custom handler service if configured
        for (Handler handler : customHandlers) {
            String handlerClassName = handler.getClass().getName();
            String formatterClassName = props.get(handlerClassName+".formatter");
            Formatter formatter = getCustomFormatter(formatterClassName, gfFileHandler);
            if (formatter != null) {
                handler.setFormatter(formatter);
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

    @Override
    public void addHandler(Handler handler) {
        Logger rootLogger = Logger.getLogger("");
        if (rootLogger != null) {
            synchronized (gfHandlers) {
                rootLogger.addHandler(handler);
                String handlerName = handler.toString();
                gfHandlers.put(handlerName.substring(0, handlerName.indexOf('@')), handler);
            }
        }
    }

    @Override
    public void preDestroy() {
        //destroy the handlers
        for (ServiceHandle<?> i : habitat.getAllServiceHandles(BuilderHelper.createContractFilter(Handler.class.getName()))) {
            i.destroy();
        }
        System.setOut(oStdOutBackup);
        System.setErr(oStdErrBackup);
        System.out.println("Completed shutdown of Log manager service");
    }

    @Override
    public PrintStream getErrStream() {
        return oStdErrBackup;
    }

    @Override
    public PrintStream getOutStream() {
        return oStdOutBackup;
    }
}
