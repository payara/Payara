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
// Portions Copyright [2016-2020] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.server.logging;

import com.sun.appserv.server.util.Version;
import com.sun.common.util.logging.LoggingConfig;
import com.sun.common.util.logging.LoggingConfigFactory;
import com.sun.common.util.logging.LoggingPropertyNames;
import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.module.bootstrap.EarlyLogHandler;
import com.sun.enterprise.server.logging.jul.ExtendedJulConfigurationFactory;
import com.sun.enterprise.util.PropertyPlaceholderHelper;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.logging.AgentFormatterDelegate;

import fish.payara.enterprise.server.logging.PayaraNotificationFileHandler;
import fish.payara.logging.jul.LoggingConfigurationHelper;
import fish.payara.logging.jul.PayaraLogHandler;
import fish.payara.logging.jul.PayaraLogHandlerConfiguration;
import fish.payara.logging.jul.PayaraLogManager;
import fish.payara.logging.jul.PayaraLogManager.Action;
import fish.payara.logging.jul.PayaraLogManagerConfiguration;
import fish.payara.logging.jul.PayaraLogManagerConfigurationParser;
import fish.payara.logging.jul.PayaraLogger;
import fish.payara.logging.jul.formatter.AnsiColorFormatter;
import fish.payara.logging.jul.formatter.FormatterDelegate;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.validation.ValidationException;

import org.glassfish.api.VersionInfo;
import org.glassfish.api.admin.FileMonitoring;
import org.glassfish.common.util.Constants;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.Rank;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.InitRunLevel;
import org.glassfish.internal.config.UnprocessedConfigListener;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import static com.sun.enterprise.util.PropertyPlaceholderHelper.ENV_REGEX;
import static fish.payara.logging.jul.JulConfigurationFactory.MINIMUM_ROTATION_LIMIT_VALUE;
import static fish.payara.logging.jul.LoggingConfigurationHelper.PRINT_TO_STDERR;
import static fish.payara.logging.jul.PayaraLoggingConstants.JVM_OPT_LOGGING_CFG_FILE;

/**
 * Reinitialise the log manager using our logging.properties file.
 *
 * @author Jerome Dochez
 * @author Carla Mott
 * @author Naman Mehta
 */
@Service
@InitRunLevel
@ContractsProvided({LogManagerService.class, org.glassfish.internal.api.LogManager.class})
@Rank(Constants.IMPORTANT_RUN_LEVEL_SERVICE)
public final class LogManagerService implements PostConstruct, PreDestroy, org.glassfish.internal.api.LogManager {

    private static final String H_CONSOLE_HANDLER = "java.util.logging.ConsoleHandler";
    private static final String H_FILE_HANDLER = "java.util.logging.FileHandler";

    private static final String SERVER_LOG_FILE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.file";
    private static final String HANDLERS_PROPERTY = "handlers";
    private static final String HANDLER_SERVICES_PROPERTY = "handlerServices";
    private static final String CONSOLEHANDLER_FORMATTER_PROPERTY = "java.util.logging.ConsoleHandler.formatter";
    private static final String GFFILEHANDLER_FORMATTER_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.formatter";
    private static final String ROTATIONTIMELIMITINMINUTES_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.rotationTimelimitInMinutes";
    private static final String BATCHSIZE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.flushFrequency";
    private static final String FILEHANDLER_LIMIT_PROPERTY = "java.util.logging.FileHandler.limit";
    private static final String LOGTOFILE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logtoFile";
    private static final String ROTATIONLIMITINBYTES_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.rotationLimitInBytes";
    private static final String USESYSTEMLOGGING_PROPERTY = "com.sun.enterprise.server.logging.SyslogHandler.useSystemLogging";
    private static final String FILEHANDLER_COUNT_PROPERTY = "java.util.logging.FileHandler.count";
    private static final String RETAINERRORSSTATICTICS_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.retainErrorsStasticsForHours";
    private static final String MAXHISTORY_FILES_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.maxHistoryFiles";
    private static final String ROTATIONONDATECHANGE_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.rotationOnDateChange";
    private static final String FILEHANDLER_PATTERN_PROPERTY = "java.util.logging.FileHandler.pattern";
    private static final String FILEHANDLER_FORMATTER_PROPERTY = "java.util.logging.FileHandler.formatter";
    private static final String LOGFORMAT_DATEFORMAT_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logFormatDateFormat";
    private static final String COMPRESS_ON_ROTATION_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.compressOnRotation";
    private static final String LOG_STANDARD_STREAMS_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logStandardStreams";

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

    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]";
    private static final String RECORD_FIELD_SEPARATOR = "|";
    private static final String RECORD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";


    private static final String NOTIFIER_LOGGER_NAME = "fish.payara.nucleus.notification.log.LogNotifierService";

    private static final Logger LOG = LogFacade.LOGGING_LOGGER;

    @Inject
    private ServerEnvironmentImpl env;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject @Optional
    private Agent agent;

    @Inject
    private FileMonitoring fileMonitoring;

    @Inject
    private LoggingConfigFactory loggingConfigFactory;

    @Inject
    private UnprocessedConfigListener unprocessedConfigListener;

    @Inject
    private Domain domain;

    private static final Consumer<Entry<String, String>> PROPERTY_VALUE_RESOLVER = e -> {
        e.setValue(TranslatedConfigView.expandConfigValue(e.getValue()));
    };


    /**
     * Initialize the loggers
     */
    @Override
    public void postConstruct() {
        if (!PayaraLogManager.isPayaraLogManager()) {
            LOG.info(() -> "LogManagerService does not support log manager different than PayaraLogManager."
                + " Used log manager: " + LogManager.getLogManager());
            // Used different implementation, Payara will not touch it.
            // HK2 services will run, but will not reconfigure logging from file.
            return;
        }

        final File loggingPropertiesFile = getOrCreateLoggingProperties();
        reconfigure(loggingPropertiesFile);
        LOG.config("Configuring change detection of the configuration file ...");
        fileMonitoring.monitors(loggingPropertiesFile, new LoggingCfgFileChangeListener(this::reconfigure));

        LOG.config("LogManagerService completed successfuly ...");
        LOG.log(Level.INFO, LogFacade.GF_VERSION_INFO, Version.getFullVersion());
    }


    public File getCurrentLogFile() {
        final PayaraLogManager logManager = PayaraLogManager.getLogManager();
        final PayaraLogHandler payaraLogHandler = logManager == null ? null : logManager.getPayaraLogHandler();
        return payaraLogHandler == null ?  null : payaraLogHandler.getConfiguration().getLogFile();
    }


    private File getOrCreateLoggingProperties() {
        final String loggingPropertiesJvmOption = System.getProperty(JVM_OPT_LOGGING_CFG_FILE);
        LOG.finest(() -> "Logging configuration from JVM option " + JVM_OPT_LOGGING_CFG_FILE + "="
            + loggingPropertiesJvmOption);
        if (loggingPropertiesJvmOption == null) {
            return getExistingLoggingPropertiesFile();
        }
        return new File(loggingPropertiesJvmOption);
    }


    private PayaraLogManagerConfiguration getRuntimeConfiguration() throws IOException {
        final Map<String, String> instanceLogCfgMap = getResolvedLoggingProperties();
        final Properties instanceLogCfg = new Properties();
        instanceLogCfg.putAll(instanceLogCfgMap);
        return new PayaraLogManagerConfiguration(instanceLogCfg);
    }


    private Map<String, String> getResolvedLoggingProperties() throws IOException {
        final Map<String, String> properties = getLoggingProperties();
        properties.entrySet().stream().forEach(PROPERTY_VALUE_RESOLVER);
        return properties;
    }


    private PayaraLogManagerConfiguration loadAndResolve(final File loggingPropertiesFile) throws IOException {
        LOG.finest(() -> "loadAndResolve(loggingPropertiesFile=" + loggingPropertiesFile + ")");
        final PayaraLogManagerConfigurationParser parser = new PayaraLogManagerConfigurationParser();
        final Properties loadedProperties = parser.load(loggingPropertiesFile);
        final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(System.getenv(), ENV_REGEX);
        return new PayaraLogManagerConfiguration(helper.replacePropertiesPlaceholder(loadedProperties));
    }


    /**
     * Returns properties based on the DAS/Cluster/Instance.
     * Values are not resolved, so can contain ${com.sun...} properties
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
        Map<String, String> loggingProperties = loggingConfig.getLoggingProperties();
        Map<String, String> invalidProps = validateProps(loggingProperties);
        if (!invalidProps.isEmpty()) {
            loggingProperties = loggingConfig.deleteLoggingProperties(invalidProps.keySet());
        }

        return loggingProperties;
    }

    @Override
    public File getLoggingPropertiesFile() throws IOException {
        final Server targetServer = domain.getServerNamed(env.getInstanceName());
        if (targetServer == null) {
            return new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
        }
        if (targetServer.isDas()) {
            return new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
        }
        if (targetServer.getCluster() != null) {
            File dirForLogging = new File(env.getConfigDirPath(), targetServer.getCluster().getConfigRef());
            return new File(dirForLogging, ServerEnvironmentImpl.kLoggingPropertiesFileName);
        }
        if (targetServer.isInstance()) {
            File dirForLogging = new File(env.getConfigDirPath(), targetServer.getConfigRef());
            return new File(dirForLogging, ServerEnvironmentImpl.kLoggingPropertiesFileName);
        }
        return new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
    }

    @Override
    public void addHandler(Handler handler) {
        LOG.config(() -> "LogManagerService.addHandler(" + handler + ")");
        final PayaraLogger rootLogger = getRootLogger();
        if (rootLogger != null && rootLogger.getHandler(handler.getClass()) == null) {
            rootLogger.addHandler(handler);
        }
    }

    @Override
    public void preDestroy() {
        LOG.config("Completed shutdown of Log manager service");
    }

    @Override
    public PrintStream getErrStream() {
        return PayaraLogManager.getLogManager().getOriginalStdErr();
    }

    @Override
    public PrintStream getOutStream() {
        return PayaraLogManager.getLogManager().getOriginalStdOut();
    }


    /**
     * Validates the map of logging properties. Will remove any properties from the
     * map that don't pass the validation, and then throw an exception at the very
     * end.
     *
     * @param loggingProperties the map of properties to validate. WILL BE MODIFIED.
     * @return a map of invalid properties. Will never be null.
     */
    public Map<String, String> validateProps(Map<String, String> loggingProperties) {
        Map<String, String> invalidProps = new HashMap<>();
        Iterator<Entry<String, String>> propertyIterator = loggingProperties.entrySet().iterator();
        while (propertyIterator.hasNext()) {
            Entry<String, String> propertyEntry = propertyIterator.next();
            try {
                validateProp(propertyEntry.getKey(), propertyEntry.getValue());
            } catch (ValidationException ex) {
                LOG.log(Level.WARNING, "Error validating log property.", ex);
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
            if (rotationSizeLimit != 0 && rotationSizeLimit < MINIMUM_ROTATION_LIMIT_VALUE) {
                throw new ValidationException(String.format("'%s' value must be greater than %d, but was %d.",
                    ROTATIONLIMITINBYTES_PROPERTY, MINIMUM_ROTATION_LIMIT_VALUE, rotationSizeLimit));
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

    private File getExistingLoggingPropertiesFile() {
        try {
            final File configuredFile = getLoggingPropertiesFile();
            if (configuredFile.exists()) {
                return configuredFile;
            }
            final String rootFolder = env.getProps().get(SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
            final String templateDir = rootFolder + File.separator + "lib" + File.separator + "templates";
            final File src = new File(templateDir, ServerEnvironmentImpl.kLoggingPropertiesFileName);
            final File dest = new File(env.getConfigDirPath(), ServerEnvironmentImpl.kLoggingPropertiesFileName);
            LOG.log(Level.INFO, "{0} not found, creating new file from template {1}.", new Object[] {dest, src});
            FileUtils.copy(src, dest);
            return dest;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, LogFacade.ERROR_READING_CONF_FILE, e);
            return null;
        }
    }

    private void createOrUpdatePayaraLogHandler(final FormatterDelegate agentDelegate) {
        final PayaraLogManager manager = PayaraLogManager.getLogManager();
        final PayaraLogHandler payaraLogHandler = manager.getPayaraLogHandler();
        final ExtendedJulConfigurationFactory factory = new ExtendedJulConfigurationFactory();
        final PayaraLogHandlerConfiguration payaraLogHandlerCfg = factory
            .createPayaraLogHandlerConfiguration(PayaraLogHandler.class, "server.log");
        payaraLogHandlerCfg.setFormatterDelegate(agentDelegate);
        payaraLogHandlerCfg.setProductId(resolveProductId());
        if (payaraLogHandler == null) {
           addHandler(new PayaraLogHandler(payaraLogHandlerCfg));
        } else {
            // could be precreated
            payaraLogHandler.reconfigure(payaraLogHandlerCfg);
        }
    }


    private Handler[] getNotifierHandlers() {
        final PayaraLogger notifierLogger = PayaraLogManager.getLogManager().getLogger(NOTIFIER_LOGGER_NAME);
        return notifierLogger == null ? new Handler[0] : notifierLogger.getHandlers();
    }


    private Handler[] getRootHandlers() {
        return getRootLogger().getHandlers();
    }


    private PayaraLogger getRootLogger() {
        return PayaraLogManager.getLogManager().getRootLogger();
    }


    private <T extends Handler> T findHandler(final Handler[] handlers, final Class<T> clazz) {
        return Arrays.stream(handlers).filter(handler -> handler.getClass().equals(clazz)).map(clazz::cast).findAny()
            .orElse(null);
    }


    private void setConsoleHandlerLogFormat(String formatterClassName, Map<String, String> loggingProperties) {
//        if (formatterClassName == null || formatterClassName.isEmpty()) {
//            formatterClassName = UniformLogFormatter.class.getName();
//        }
//        final String cname = GFFileHandler.class.getName();
//        configuration.setChFormatterClass(formatterClassName);
//        configuration.setGfExcludedFields(loggingProperties.get(cname + ".excludeFields"));
//        configuration.setGfMultiLineMode(Boolean.parseBoolean(loggingProperties.get(cname + ".multiLineMode")));
//        if (formatterClassName.equals(UniformLogFormatter.class.getName())) {
//            UniformLogFormatter formatter = new UniformLogFormatter();
//            recordBeginMarker = loggingProperties.get(cname + ".logFormatBeginMarker");
//            if (recordBeginMarker == null || recordBeginMarker.isEmpty()) {
//                LOGGER.log(Level.FINE, "Record begin marker is not a proper value so using default.");
//                recordBeginMarker = RECORD_BEGIN_MARKER;
//            }
//
//            recordEndMarker = loggingProperties.get(cname + ".logFormatEndMarker");
//            if (recordEndMarker == null || recordEndMarker.isEmpty()) {
//                LOGGER.log(Level.FINE, "Record end marker is not a proper value so using default.");
//                recordEndMarker = RECORD_END_MARKER;
//            }
//
//            recordFieldSeparator = loggingProperties.get(cname + ".logFormatFieldSeparator");
//            if (recordFieldSeparator == null || recordFieldSeparator.isEmpty() || recordFieldSeparator.length() > 1) {
//                LOGGER.log(Level.FINE, "Log Format field separator is not a proper value so using default.");
//                recordFieldSeparator = RECORD_FIELD_SEPARATOR;
//            }
//
//            recordDateFormat = loggingProperties.get(cname + ".logFormatDateFormat");
//            if (recordDateFormat != null && !recordDateFormat.isEmpty()) {
//                try {
//                    DateTimeFormatter.ofPattern(recordDateFormat).format(OffsetDateTime.now());
//                } catch (Exception e) {
//                    LOGGER.log(Level.FINE, "Date Format specified is wrong so using default.");
//                    recordDateFormat = RECORD_DATE_FORMAT;
//                }
//            } else {
//                LOGGER.log(Level.FINE, "Date Format specified is wrong so using default.");
//                recordDateFormat = RECORD_DATE_FORMAT;
//            }
//
//            formatter.setRecordBeginMarker(recordBeginMarker);
//            formatter.setRecordEndMarker(recordEndMarker);
//            formatter.setRecordDateFormat(recordDateFormat);
//            formatter.setRecordFieldSeparator(recordFieldSeparator);
//            formatter.setExcludeFields(configuration.getGfExcludedFields());
//            formatter.setMultiLineMode(configuration.isGfMultiLineMode());
//            for (Handler handler : getRootLogger().getHandlers()) {
//                // only get the ConsoleHandler
//                if (handler.getClass().equals(ConsoleHandler.class)) {
//                    handler.setFormatter(formatter);
//                    break;
//                }
//            }
//        } else if (formatterClassName.equals(ODLLogFormatter.class.getName())) {
//            // used to support ODL formatter in GF.
//            ODLLogFormatter formatter = new ODLLogFormatter();
//            formatter.setExcludeFields(configuration.getGfExcludedFields());
//            formatter.setMultiLineMode(configuration.isGfMultiLineMode());
//            for (Handler handler : getRootLogger().getHandlers()) {
//                // only get the ConsoleHandler
//                if (handler.getClass().equals(ConsoleHandler.class)) {
//                    handler.setFormatter(formatter);
//                    break;
//                }
//            }
//        } else if (formatterClassName.equals(JSONLogFormatter.class.getName())) {
//            JSONLogFormatter formatter = new JSONLogFormatter();
//            formatter.setExcludeFields(configuration.getGfExcludedFields());
//            for (Handler handler : getRootLogger().getHandlers()) {
//                // only get the ConsoleHandler
//                if (handler.getClass().equals(ConsoleHandler.class)) {
//                    handler.setFormatter(formatter);
//                    break;
//                }
//            }
//        }
    }

//    private LogManagerConfig parseConfiguration(Map<String, String> loggingProperties) {
//        configuration = new LogManagerConfig();
//        configuration.setHandlers(loggingProperties.get(HANDLERS_PROPERTY));
//        configuration.setHandlerServices(loggingProperties.get(HANDLER_SERVICES_PROPERTY));
//
//        configuration.setEnableSysLogHandler(loggingProperties.get(USESYSTEMLOGGING_PROPERTY));
//
//        configuration.setChFormatterClass(loggingProperties.get(CONSOLEHANDLER_FORMATTER_PROPERTY));
//
//        configuration.setFhOutputFileSizeLimit(loggingProperties.get(FILEHANDLER_LIMIT_PROPERTY));
//        configuration.setFhMaxCountOfFiles(loggingProperties.get(FILEHANDLER_COUNT_PROPERTY));
//        configuration.setFhFormatterClass(loggingProperties.get(FILEHANDLER_PATTERN_PROPERTY));
//        configuration.setFhFormatterClass(loggingProperties.get(FILEHANDLER_FORMATTER_PROPERTY));
//
//        configuration.setGfhOutputFile(new File(loggingProperties.get(SERVER_LOG_FILE_PROPERTY)));
//        configuration.setGfhFormatterClass(loggingProperties.get(GFFILEHANDLER_FORMATTER_PROPERTY));
//        configuration.setGfhRotationOnTimeLimit(loggingProperties.get(ROTATIONTIMELIMITINMINUTES_PROPERTY));
//        configuration.setGfhBatchSize(loggingProperties.get(BATCHSIZE_PROPERTY));
//        configuration.setGfhLogToFile(loggingProperties.get(LOGTOFILE_PROPERTY));
//        configuration.setGfhOutputFileSizeLimit(loggingProperties.get(ROTATIONLIMITINBYTES_PROPERTY));
//        configuration.setGfhRetainErrorsStatictics(loggingProperties.get(RETAINERRORSSTATICTICS_PROPERTY));
//        configuration.setGfhMaxCountOfFiles(loggingProperties.get(MAXHISTORY_FILES_PROPERTY));
//        configuration.setGfhEnableRotationOnDateChange(loggingProperties.get(ROTATIONONDATECHANGE_PROPERTY));
//        configuration.setGfhTimeStampPattern(loggingProperties.get(LOGFORMAT_DATEFORMAT_PROPERTY));
//        configuration.setGfhCompressOnRotation(loggingProperties.get(COMPRESS_ON_ROTATION_PROPERTY));
//        configuration.setGfhLogStandardStreams(loggingProperties.get(LOG_STANDARD_STREAMS_PROPERTY));
//
//        configuration.setPnOutputFile(loggingProperties.get(PAYARA_NOTIFICATION_LOG_FILE_PROPERTY));
//        configuration.setPnLogToFile(loggingProperties.get(PAYARA_NOTIFICATION_LOGTOFILE_PROPERTY));
//        configuration.setPnEnableRotationOnDateChange(loggingProperties.get(PAYARA_NOTIFICATION_LOG_ROTATIONONDATECHANGE_PROPERTY));
//        configuration.setPnRotationOnTimeLimit(loggingProperties.get(PAYARA_NOTIFICATION_LOG_ROTATIONTIMELIMITINMINUTES_PROPERTY));
//        configuration.setPnOutputFileSizeLimit(loggingProperties.get(PAYARA_NOTIFICATION_LOG_ROTATIONLIMITINBYTES_PROPERTY));
//        configuration.setPnMaxCountOfFiles(loggingProperties.get(PAYARA_NOTIFICATION_LOG_MAXHISTORY_FILES_PROPERTY));
//        configuration.setPnCompressOnRotation(loggingProperties.get(PAYARA_NOTIFICATION_LOG_COMPRESS_ON_ROTATION_PROPERTY));
//        configuration.setPnFormatterClass(loggingProperties.get(PAYARA_NOTIFICATION_LOG_FORMATTER_PROPERTY));
//        return configuration;
//    }

    private List<Handler> getHandlerServices(final PayaraLogManagerConfiguration configuration) {
        final Set<String> requestedHandlerServices = new HashSet<>();
        {
            final String handlerServicesCfg = configuration.getProperty(HANDLER_SERVICES_PROPERTY);
            if (handlerServicesCfg != null) {
                for (final String handlerService : handlerServicesCfg.split(",")) {
                    requestedHandlerServices.add(handlerService);
                }
            }
        }
        final List<Handler> hk2HandlerServices = serviceLocator.getAllServices(Handler.class);
        final List<Handler> foundAndRequested = new ArrayList<>();
        final List<Handler> otherHandlers = new ArrayList<>();
        for (final Handler handler: hk2HandlerServices) {
            final String handlerClassName = handler.getClass().getName();
            if (requestedHandlerServices.contains(handlerClassName)) {
                foundAndRequested.add(handler);
            }
            if (!handlerClassName.equals(PayaraNotificationFileHandler.class.getName())) {
                otherHandlers.add(handler);
            }
        }

        for (final Handler handler : otherHandlers) {
            final LoggingConfigurationHelper helper = new LoggingConfigurationHelper(handler.getClass(),
                PRINT_TO_STDERR);
            final Formatter formatter = helper.getFormatter("formatter", null);
            if (formatter != null) {
                // if null, default is specified by the handler implementation.
                final ExtendedJulConfigurationFactory factory = new ExtendedJulConfigurationFactory();
                factory.configureFormatter(formatter, helper);
                handler.setFormatter(formatter);
            }
        }
        return foundAndRequested;
    }


    private void generateAttributeChangeEvent(String key, String oldValue, String newValue) {
        PropertyChangeEvent event = new PropertyChangeEvent(this, key, oldValue, newValue);
        UnprocessedChangeEvents events = new UnprocessedChangeEvents(
            new UnprocessedChangeEvent(event, "server log file attribute " + key + " changed."));
        // potential memory leak - it only adds but never removes.
        unprocessedConfigListener.unprocessedTransactedEvents(Collections.singletonList(events));
    }


    private void reconfigure(final File configFile) {
        final PayaraLogManager manager = PayaraLogManager.getLogManager();
        manager.getOriginalStdErr().println("reconfigure(" + configFile + ")");
        LOG.info(() -> "Using property file: " + configFile);
        try {
            final PayaraLogManagerConfiguration cfg = getRuntimeConfiguration();
            if (cfg == null) {
                manager.getOriginalStdErr().println("null logging properties!");
                return;
            }
            final ReconfigurationAction reconfig = new ReconfigurationAction(manager, cfg);
            manager.reconfigure(cfg, reconfig, null);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, LogFacade.ERROR_APPLYING_CONF, e);
        }
    }


    private boolean checkLevels(final String key, final String value,
        final Map<String, Level> handlerLevels, final Map<String, Level> loggerLevels) {
        if (key.endsWith(".level")) {
            final String name = key.substring(0, key.lastIndexOf(".level"));
            final Level level = Level.parse(value);
            if (name.equals(PayaraNotificationFileHandler.class.getName()) //
                || name.equals(PayaraLogHandler.class.getName()) //
                || name.equals(H_CONSOLE_HANDLER) //
                || name.equals(H_FILE_HANDLER)) {
                handlerLevels.put(name, level);
            } else {
                loggerLevels.put(name, level);
            }
            return true;
        }
        return false;
    }
//
//    private boolean checkHandlers(final String key, final String value) {
//        if (key.equals(HANDLERS_PROPERTY)) {
//            final String oldValue = configuration.getHandlers();
//            if (!value.equals(oldValue)) {
//                generateAttributeChangeEvent(HANDLERS_PROPERTY, oldValue, value);
//            }
//            return true;
//        } if (key.equals(HANDLER_SERVICES_PROPERTY)) {
//            final String oldValue = configuration.getHandlerServices();
//            if (!value.equals(oldValue)) {
//                generateAttributeChangeEvent(HANDLER_SERVICES_PROPERTY, oldValue, value);
//            }
//            return true;
//        }
//        return false;
//    }
//
//
//    private boolean checkConsoleHandler(final String key, final String value,
//        final Map<String, String> loggingProperties) {
//        if (key.equals(CONSOLEHANDLER_FORMATTER_PROPERTY)) {
//            if (!value.equals(configuration.getChFormatterClass())) {
//                setConsoleHandlerLogFormat(value, loggingProperties);
//            }
//            return true;
//        }
//        return false;
//    }
//
//
//    private boolean checkFileHandler(String key, String value) {
//        if (key.equals(FILEHANDLER_PATTERN_PROPERTY)) {
//            if (!value.equals(configuration.getFhFileNamePattern())) {
//                generateAttributeChangeEvent(FILEHANDLER_PATTERN_PROPERTY, configuration.getFhFileNamePattern(),
//                    value);
//            }
//            return true;
//        }
//        if (key.equals(FILEHANDLER_FORMATTER_PROPERTY)) {
//            if (!value.equals(configuration.getFhFormatterClass())) {
//                generateAttributeChangeEvent(FILEHANDLER_FORMATTER_PROPERTY, configuration.getFhFormatterClass(),
//                    value);
//            }
//            return true;
//        }
//        if (key.equals(FILEHANDLER_COUNT_PROPERTY)) {
//            if (!value.equals(configuration.getFhMaxCountOfFiles())) {
//                generateAttributeChangeEvent(FILEHANDLER_COUNT_PROPERTY, configuration.getFhMaxCountOfFiles(),
//                    value);
//            }
//            return true;
//        }
//        return false;
//    }
//
//    private boolean checkSysLogHandler(final String key, final String value, final Handler[] rootHandlers) {
//        if (key.equals(USESYSTEMLOGGING_PROPERTY)) {
//            if (!value.equals(configuration.getEnableSysLogHandler())) {
//                configuration.setEnableSysLogHandler(value);
//                for (final Handler handler : rootHandlers) {
//                    if (handler.getClass().equals(SyslogHandler.class)) {
//                        final SyslogHandler syslogHandler = (SyslogHandler) handler;
//                        syslogHandler.setSystemLogging(Boolean.parseBoolean(value));
//                        break;
//                    }
//                }
//            }
//            return true;
//        }
//        return false;
//    }
//
//
//    private boolean checkPayaraLogHandler(String key, final String value, final PayaraLogHandler handler,
//        final AtomicBoolean reconfigureGfFormatter) {
//        if (key.equals(SERVER_LOG_FILE_PROPERTY)) {
//            if (!value.equals(configuration.getGfhOutputFile().getAbsolutePath())) {
//                configuration.setGfhOutputFile(new File(value));
//                if (handler != null) {
//                    handler.getConfiguration().setLogFile(configuration.getGfhOutputFile());
//                }
//            }
//            return true;
//        } else if (key.equals(LOGFORMAT_DATEFORMAT_PROPERTY)) {
//            // FIXME: no timestamp pattern can be changed?!
//            if (!value.equals(configuration.getGfhTimeStampPattern())) {
//                generateAttributeChangeEvent(LOGFORMAT_DATEFORMAT_PROPERTY,
//                    configuration.getGfhTimeStampPattern(), value);
//            }
//            return true;
//        } else if (key.equals(GFFILEHANDLER_FORMATTER_PROPERTY)) {
//            if (!value.equals(configuration.getGfhFormatterClass())) {
//                configuration.setGfhFormatterClass(value);
//                reconfigureGfFormatter.set(true);
//            }
//            return true;
//        } else if (key.equals(ROTATIONTIMELIMITINMINUTES_PROPERTY)) {
//            if (!value.equals(configuration.getGfhRotationOnTimeLimit())) {
//                configuration.setGfhRotationOnTimeLimit(value);
//                if (handler != null) {
//                    handler.setRotationTimeLimitValue(Long.parseLong(value));
//                }
//            }
//            return true;
//        } else if (key.equals(BATCHSIZE_PROPERTY)) {
//            if (!value.equals(configuration.getGfhBatchSize())) {
//                configuration.setGfhBatchSize(value);
//                if (handler != null) {
//                    handler.setFlushFrequency(Integer.parseInt(value));
//                }
//            }
//            return true;
//        } else if (key.equals(FILEHANDLER_LIMIT_PROPERTY)) {
//            if (!value.equals(configuration.getFhOutputFileSizeLimit())) {
//                generateAttributeChangeEvent(FILEHANDLER_LIMIT_PROPERTY,
//                    configuration.getFhOutputFileSizeLimit(), value);
//            }
//            return true;
//        } else if (key.equals(LOGTOFILE_PROPERTY)) {
//            if (!value.equals(configuration.getGfhLogToFile())) {
//                configuration.setGfhLogToFile(value);
//                if (handler != null) {
//                    handler.setLogToFile(Boolean.parseBoolean(value));
//                }
//            }
//            return true;
//        } else if (key.equals(ROTATIONLIMITINBYTES_PROPERTY)) {
//            if (!value.equals(configuration.getGfhOutputFileSizeLimit())) {
//                configuration.setGfhOutputFileSizeLimit(value);
//                if (handler != null) {
//                    handler.setRotationLimitAttrValue(Integer.valueOf(value));
//                }
//            }
//            return true;
//        } else if (key.equals(RETAINERRORSSTATICTICS_PROPERTY)) {
//            if (!value.equals(configuration.getGfhRetainErrorsStatictics())) {
//                generateAttributeChangeEvent(RETAINERRORSSTATICTICS_PROPERTY,
//                    configuration.getGfhRetainErrorsStatictics(), value);
//            }
//            return true;
//        } else if (key.equals(MAXHISTORY_FILES_PROPERTY)) {
//            if (!value.equals(configuration.getGfhMaxCountOfFiles())) {
//                configuration.setGfhMaxCountOfFiles(value);
//                if (handler != null) {
//                    handler.setMaxHistoryFiles(Integer.parseInt(value));
//                }
//            }
//            return true;
//        } else if (key.equals(ROTATIONONDATECHANGE_PROPERTY)) {
//            if (!value.equals(configuration.getGfhEnableRotationOnDateChange())) {
//                configuration.setGfhEnableRotationOnDateChange(value);
//                if (handler != null) {
//                    handler.setRotationOnDateChange(Boolean.parseBoolean(value));
//                }
//            }
//            return true;
//        } else if (key.equals(COMPRESS_ON_ROTATION_PROPERTY)) {
//            if (!value.equals(configuration.getGfhCompressOnRotation())) {
//                configuration.setGfhCompressOnRotation(value);
//                if (handler != null) {
//                    handler.setCompressionOnRotation(Boolean.parseBoolean(value));
//                }
//            }
//            return true;
//        } else if (key.equals(LOG_STANDARD_STREAMS_PROPERTY)) {
//            if (!value.equals(configuration.getGfhLogStandardStreams())) {
//                configuration.setGfhLogStandardStreams(value);
//                if (handler != null) {
//                    handler.setLogStandardStreams(Boolean.parseBoolean(value));
//                }
//            }
//            return true;
//        } else if (key.endsWith(".excludeFields")) {
//            if (!Objects.equals(value, configuration.getGfExcludedFields())) {
//                configuration.setGfExcludedFields(value);
//                reconfigureGfFormatter.set(true);
//            }
//            return true;
//        } else if (key.endsWith(".multiLineMode")) {
//            String oldVal = Boolean.toString(configuration.isGfMultiLineMode());
//            if (!value.equalsIgnoreCase(oldVal)) {
//                configuration.setGfMultiLineMode(Boolean.parseBoolean(value));
//                reconfigureGfFormatter.set(true);
//            }
//            return true;
//        }
//        return false;
//    }

//    private boolean checkPayaraNotificationHandler(String key, String value,
//        PayaraNotificationFileHandler pnFileHandler, AtomicBoolean reconfigurePnFormatter) {
//        if (key.equals(PAYARA_NOTIFICATION_LOG_FORMATTER_PROPERTY)) {
//            if (!value.equals(configuration.getPnFormatterClass())) {
//                Handler[] payaraNotificationLogFileHandlers = getNotifierHandlers();
//                if (payaraNotificationLogFileHandlers.length > 0) {
//                    configuration.setPnFormatterClass(value);
//                    reconfigurePnFormatter.set(true);
//                } else {
//                    LOG.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
//                }
//            }
//        } else if (key.equals(PAYARA_NOTIFICATION_LOG_FILE_PROPERTY)) {
//            if (!value.equals(configuration.getPnOutputFile())) {
//                Handler[] payaraNotificationLogFileHandlers = getNotifierHandlers();
//                if (payaraNotificationLogFileHandlers.length > 0) {
//                    configuration.setPnOutputFile(value);
//                    if (pnFileHandler != null) {
//                        pnFileHandler.setLogFile(value);
//                    }
//                } else {
//                    LOG.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
//                }
//            }
//        } else if (key.equals(PAYARA_NOTIFICATION_LOGTOFILE_PROPERTY)) {
//            if (!value.equals(configuration.getPnLogToFile())) {
//                Handler[] payaraNotificationLogFileHandlers = getNotifierHandlers();
//                if (payaraNotificationLogFileHandlers.length > 0) {
//                    configuration.setPnLogToFile(value);
//                    if (pnFileHandler != null) {
//                        pnFileHandler.setLogToFile(Boolean.parseBoolean(value));
//                    }
//                } else {
//                    LOG.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
//                }
//            }
//        } else if (key.equals(PAYARA_NOTIFICATION_LOG_ROTATIONTIMELIMITINMINUTES_PROPERTY)) {
//            if (!value.equals(configuration.getPnRotationOnTimeLimit())) {
//                Handler[] payaraNotificationLogFileHandlers = getNotifierHandlers();
//                if (payaraNotificationLogFileHandlers.length > 0) {
//                    configuration.setPnRotationOnTimeLimit(value);
//                    if (pnFileHandler != null) {
//                        pnFileHandler.setRotationTimeLimitValue(Long.parseLong(value));
//                    }
//                } else {
//                    LOG.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
//                }
//            }
//        } else if (key.equals(PAYARA_NOTIFICATION_LOG_ROTATIONLIMITINBYTES_PROPERTY)) {
//            if (!value.equals(configuration.getPnOutputFileSizeLimit())) {
//                Handler[] payaraNotificationLogFileHandlers = getNotifierHandlers();
//                if (payaraNotificationLogFileHandlers.length > 0) {
//                    configuration.setPnOutputFileSizeLimit(value);
//                    if (pnFileHandler != null) {
//                        pnFileHandler.setRotationLimitAttrValue(Integer.valueOf(value));
//                    }
//                } else {
//                    LOG.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
//                }
//            }
//        } else if (key.equals(PAYARA_NOTIFICATION_LOG_ROTATIONONDATECHANGE_PROPERTY)) {
//            if (!value.equals(configuration.getPnEnableRotationOnDateChange())) {
//                Handler[] payaraNotificationLogFileHandlers = getNotifierHandlers();
//                if (payaraNotificationLogFileHandlers.length > 0) {
//                    configuration.setPnEnableRotationOnDateChange(value);
//                    if (pnFileHandler != null) {
//                        pnFileHandler.setRotationOnDateChange(Boolean.parseBoolean(value));
//                    }
//                } else {
//                    LOG.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
//                }
//            }
//        } else if (key.equals(PAYARA_NOTIFICATION_LOG_MAXHISTORY_FILES_PROPERTY)) {
//            if (!value.equals(configuration.getPnMaxCountOfFiles())) {
//                Handler[] payaraNotificationLogFileHandlers = getNotifierHandlers();
//                if (payaraNotificationLogFileHandlers.length > 0) {
//                    configuration.setPnMaxCountOfFiles(value);
//                    if (pnFileHandler != null) {
//                        pnFileHandler.setMaxHistoryFiles(Integer.parseInt(value));
//                    }
//                } else {
//                    LOG.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
//                }
//            }
//        } else if (key.equals(PAYARA_NOTIFICATION_LOG_COMPRESS_ON_ROTATION_PROPERTY)) {
//            if (!value.equals(configuration.getPnCompressOnRotation())) {
//                Handler[] payaraNotificationLogFileHandlers = getNotifierHandlers();
//                if (payaraNotificationLogFileHandlers.length > 0) {
//                    configuration.setPnCompressOnRotation(value);
//                    if (pnFileHandler != null) {
//                        pnFileHandler.setCompressionOnRotation(Boolean.parseBoolean(value));
//                    }
//                } else {
//                    LOG.log(Level.INFO, PAYARA_NOTIFICATION_NOT_USING_SEPARATE_LOG);
//                }
//            }
//        }
//        return false;
//    }

    private String resolveProductId() {
        final ServiceLocator locator = Globals.getDefaultBaseServiceLocator();
        final VersionInfo versionInfo = locator.getService(VersionInfo.class);
        if (versionInfo == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(versionInfo.getAbbreviatedProductName());
        sb.append(' ');
        sb.append(versionInfo.getVersionPrefix());
        sb.append(versionInfo.getMajorVersion());
        sb.append('.');
        sb.append(versionInfo.getMinorVersion());
        sb.append('.');
        sb.append(versionInfo.getUpdateVersion());
        return sb.toString();
    }

    private final class ReconfigurationAction implements Action {

        private final PayaraLogManager manager;
        private final PayaraLogManagerConfiguration cfg;

        private ReconfigurationAction(final PayaraLogManager manager, final PayaraLogManagerConfiguration cfg) {
            this.manager = manager;
            this.cfg = cfg;
        }


        @Override
        public ClassLoader getClassLoader() {
            return LogManagerService.class.getClassLoader();
        }


        @Override
        public void run() {
            final FormatterDelegate agentDelegate = agent == null ? null : new AgentFormatterDelegate(agent);
            createOrUpdatePayaraLogHandler(agentDelegate);
            final List<Handler> handlerServicesToSet = getHandlerServices(cfg);
            for (Handler handlerService : handlerServicesToSet) {
                addHandler(handlerService);
            }

            // add the filter if there is one
            final String filterClassName = cfg.getProperty(LoggingPropertyNames.logFilter);
            final Logger rootLogger = getRootLogger();
            if (filterClassName == null) {
                rootLogger.setFilter(null);
            } else {
                final Filter filter = serviceLocator.getService(Filter.class, filterClassName);
                if (rootLogger != null && rootLogger.getFilter() == null) {
                    rootLogger.setFilter(filter);
                }
            }
            if (agentDelegate != null) {
                final Enumeration<String> loggerNames = manager.getLoggerNames();
                LOG.config(() -> "Configuring formatters of handlers of existing non-root loggers ... \n" + loggerNames);
                while (loggerNames.hasMoreElements()) {
                    String loggerName = loggerNames.nextElement();
                    Logger logger = manager.getLogger(loggerName);
                    if (logger == null || logger.getName().isEmpty()) {
                        // skip root logger (managed by createOrUpdatePayaraLogHandler)
                        continue;
                    }
                    for (Handler handler : logger.getHandlers()) {
                        Formatter formatter = handler.getFormatter();
                        if (formatter != null && formatter instanceof AnsiColorFormatter) {
                            ((AnsiColorFormatter) formatter).setDelegate(agentDelegate);
                        }
                    }
                }
            }

            final Map<String, Level> loggerLevels = new HashMap<>();
            final Map<String, Level> handlerLevels = new HashMap<>();
            final Handler[] rootHandlers = getRootHandlers();
//            final PayaraLogHandler payaraLogHandler = findHandler(rootHandlers, PayaraLogHandler.class);
//            final PayaraNotificationFileHandler pnFileHandler = findHandler(getNotifierHandlers(),
   //                PayaraNotificationFileHandler.class);
            LOG.config(() -> "Actual root handlers=" + Arrays.toString(rootHandlers));
//            final AtomicBoolean reconfigurePayaraLogHandlerFormatter = new AtomicBoolean();
//            final AtomicBoolean reconfigurePnFormatter = new AtomicBoolean();

            // FIXME: does not respect deleted items, they will remain set
            for (Entry<Object, Object> entry : cfg.getProperties().entrySet()) {
                final String key = (String) entry.getKey();
                final String value = (String) entry.getValue();
                if (checkLevels(key, value, handlerLevels, loggerLevels)) {
                    continue;
                }
//                if (checkHandlers(key, value)) {
//                    continue;
//                }
//                if (checkConsoleHandler(key, value, loggingProperties)) {
//                    continue;
//                }
//                if (checkFileHandler(key, value)) {
//                    continue;
//                }
//                if (checkSysLogHandler(key, value, rootHandlers)) {
//                    continue;
//                }
//                if (checkPayaraLogHandler(key, value, payaraLogHandler, reconfigurePayaraLogHandlerFormatter)) {
//                    continue;
//                }
//                if (checkPayaraNotificationHandler(key, value, pnFileHandler, reconfigurePnFormatter)) {
//                    continue;
//                }
        } // for
//
//            if (reconfigurePayaraLogHandlerFormatter.get() && payaraLogHandler != null) {
//                payaraLogHandler.reconfigure(null);configureLogFormatter(configuration.getGfhFormatterClass());
//            }
//            if (reconfigurePnFormatter.get() && pnFileHandler != null) {
//                 FIXME: finish it.
//                pnFileHandler.configureLogFormatter(configuration.getPnFormatterClass());
//            }
            for (Handler handler : rootHandlers) {
                handler.setLevel(handlerLevels.getOrDefault(handler.getClass().getName(), Level.INFO));
            }

            final ArrayBlockingQueue<LogRecord> catchEarlyMessage = EarlyLogHandler.earlyMessages;
            while (!catchEarlyMessage.isEmpty()) {
                LogRecord logRecord = catchEarlyMessage.poll();
                if (logRecord != null) {
                    LOG.log(logRecord);
                }
            }
        }
    }

    private static final class LoggingCfgFileChangeListener implements FileMonitoring.FileChangeListener {

        private final Consumer<File> action;


        LoggingCfgFileChangeListener(final Consumer<File> action) {
            this.action = action;
        }


        @Override
        public void changed(File changedFile) {
            LOG.info(() -> "Detected change of file: " + changedFile);
            action.accept(changedFile);
        }


        @Override
        public void deleted(File deletedFile) {
            LOG.log(Level.WARNING, LogFacade.CONF_FILE_DELETED, deletedFile.getAbsolutePath());
        }
    }
}
