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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.server.logging;

import com.sun.appserv.server.util.Version;
import com.sun.common.util.logging.LoggingConfig;
import com.sun.common.util.logging.LoggingConfigFactory;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.module.bootstrap.EarlyLogHandler;
import com.sun.enterprise.util.PropertyPlaceholderHelper;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.FileUtils;

import fish.payara.enterprise.server.logging.PayaraNotificationFileHandler;
import fish.payara.jul.PayaraLogManager;
import fish.payara.jul.PayaraLogger;
import fish.payara.jul.PayaraLogManager.Action;
import fish.payara.jul.cfg.PayaraLogManagerConfiguration;
import fish.payara.jul.cfg.SortedProperties;
import fish.payara.jul.env.LoggingSystemEnvironment;
import fish.payara.jul.handler.PayaraLogHandler;
import fish.payara.jul.handler.PayaraLogHandlerConfiguration;
import fish.payara.jul.handler.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty;
import fish.payara.jul.tracing.PayaraLoggingTracer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
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
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import static com.sun.enterprise.util.PropertyPlaceholderHelper.ENV_REGEX;
import static fish.payara.jul.cfg.PayaraLoggingJvmOptions.JVM_OPT_LOGGING_CFG_FILE;
import static fish.payara.jul.handler.PayaraLogHandler.createPayaraLogHandlerConfiguration;
import static fish.payara.jul.handler.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.MINIMUM_ROTATION_LIMIT_BYTES;
import static fish.payara.jul.handler.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.ROTATION_LIMIT_SIZE;
import static fish.payara.jul.handler.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.ROTATION_LIMIT_TIME;

/**
 * Reinitialise the log manager using our logging.properties file.
 *
 * @author Jerome Dochez
 * @author Carla Mott
 * @author Naman Mehta
 * @author David Matejcek
 */
@Service
@InitRunLevel
@ContractsProvided({LogManagerService.class, org.glassfish.internal.api.LogManager.class})
@Rank(Constants.IMPORTANT_RUN_LEVEL_SERVICE)
public final class LogManagerService implements PostConstruct, PreDestroy, org.glassfish.internal.api.LogManager {

    private static final Logger LOG = LogFacade.LOGGING_LOGGER;
    private static final String H_CONSOLE_HANDLER = "fish.payara.jul.handler.SimpleLogHandler";
    private static final String H_FILE_HANDLER = "java.util.logging.FileHandler";


    @Inject
    private ServerEnvironmentImpl env;

    @Inject
    private FileMonitoring fileMonitoring;

    @Inject
    private LoggingConfigFactory loggingConfigFactory;

    @Inject
    private Domain domain;

    private static final Consumer<Entry<String, String>> PROPERTY_VALUE_RESOLVER = e -> {
        e.setValue(TranslatedConfigView.expandConfigValue(e.getValue()));
    };


    /**
     * Initializes the automatic reconfiguration of the logging system.
     */
    @Override
    public void postConstruct() {
        if (!PayaraLogManager.isPayaraLogManager()) {
            LOG.info(() -> "LogManagerService does not support any other log manager than PayaraLogManager."
                + " Used log manager: " + LogManager.getLogManager());
            return;
        }
        setProductId();

        final File loggingPropertiesFile = getOrCreateLoggingProperties();
        reconfigure(loggingPropertiesFile);
        LOG.config("Configuring change detection of the configuration file ...");
        fileMonitoring.monitors(loggingPropertiesFile, new LoggingCfgFileChangeListener(this::reconfigure));

        LOG.config("LogManagerService completed successfuly ...");
        LOG.log(Level.INFO, LogFacade.GF_VERSION_INFO, Version.getFullVersion());
    }

    /**
     * @return a file used as the main server log file
     */
    public File getCurrentLogFile() {
        final PayaraLogManager logManager = PayaraLogManager.getLogManager();
        final PayaraLogHandler payaraLogHandler = logManager == null ? null : logManager.getPayaraLogHandler();
        return payaraLogHandler == null ? null : payaraLogHandler.getConfiguration().getLogFile();
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
        final SortedProperties instanceLogCfg = new SortedProperties();
        instanceLogCfg.putAll(instanceLogCfgMap);
        return new PayaraLogManagerConfiguration(instanceLogCfg);
    }


    private Map<String, String> getResolvedLoggingProperties() throws IOException {
        final Map<String, String> properties = getLoggingProperties();
        properties.entrySet().stream().forEach(PROPERTY_VALUE_RESOLVER);
        return properties;
    }

    // FIXME: This is not used, so something is broken! Write test first, then think how to integrate it correctly.
    private PayaraLogManagerConfiguration loadAndResolve(final File loggingPropertiesFile) throws IOException {
        LOG.finest(() -> "loadAndResolve(loggingPropertiesFile=" + loggingPropertiesFile + ")");
        final SortedProperties loadedProperties = SortedProperties.loadFrom(loggingPropertiesFile);
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

    // FIXME: Remove from LogManager's @Contract
    @Override
    public void addHandler(Handler handler) {
        LOG.config(() -> "LogManagerService.addHandler(" + handler + ")");
        final PayaraLogger rootLogger = getRootLogger();
        if (rootLogger != null && rootLogger.getHandler(handler.getClass()) == null) {
            rootLogger.addHandler(handler);
        }
    }

    @Override
    public PrintStream getErrStream() {
        return LoggingSystemEnvironment.getOriginalStdErr();
    }

    @Override
    public PrintStream getOutStream() {
        return LoggingSystemEnvironment.getOriginalStdOut();
    }

    @Override
    public void preDestroy() {
        LOG.config("Completed shutdown of Log manager service");
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
        if (isOneOf(key, ROTATION_LIMIT_SIZE, PayaraLogHandler.class, PayaraNotificationFileHandler.class)) {
            int rotationSizeLimit = Integer.parseInt(value);
            if (rotationSizeLimit != 0 && rotationSizeLimit < MINIMUM_ROTATION_LIMIT_BYTES) {
                throw new ValidationException(String.format("'%s' value must be greater than %d, but was %d.",
                    key, MINIMUM_ROTATION_LIMIT_BYTES, rotationSizeLimit));
            }
        } else if (isOneOf(key, ROTATION_LIMIT_TIME, PayaraLogHandler.class, PayaraNotificationFileHandler.class)) {
            int rotationTimeLimit = Integer.parseInt(value);
            if (rotationTimeLimit < 0) {
                throw new ValidationException(String.format("'%s' value must be greater than %d, but was %d.",
                    key, 0, rotationTimeLimit));
            }
        }
    }

    private void setProductId() {
        final ServiceLocator locator = Globals.getDefaultBaseServiceLocator();
        final VersionInfo versionInfo = locator.getService(VersionInfo.class);
        if (versionInfo == null) {
            LoggingSystemEnvironment.setProductId(null);
            return;
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
        LoggingSystemEnvironment.setProductId(sb.toString());
    }

    private boolean isOneOf(final String key, final PayaraLogHandlerProperty attribute, final Class<?>... handlerClasses) {
        for (Class<?> handlerClass : handlerClasses) {
            if (attribute.getPropertyFullName(handlerClass).equals(key)) {
                return true;
            }
        }
        return false;
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

    private void createOrUpdatePayaraLogHandler() {
        final PayaraLogManager manager = PayaraLogManager.getLogManager();
        final PayaraLogHandler payaraLogHandler = manager.getPayaraLogHandler();
        final PayaraLogHandlerConfiguration cfg = createPayaraLogHandlerConfiguration(PayaraLogHandler.class);
        if (payaraLogHandler == null) {
           addHandler(new PayaraLogHandler(cfg));
        } else {
            payaraLogHandler.reconfigure(cfg);
        }
    }


    private Handler[] getRootHandlers() {
        return getRootLogger().getHandlers();
    }


    private PayaraLogger getRootLogger() {
        return PayaraLogManager.getLogManager().getRootLogger();
    }


    private void reconfigure(final File configFile) {
        final PayaraLogManager manager = PayaraLogManager.getLogManager();
        PayaraLoggingTracer.trace(getClass(), () -> "reconfigure(" + configFile + ")");
        LOG.info(() -> "Using property file: " + configFile);
        try {
            final PayaraLogManagerConfiguration cfg = getRuntimeConfiguration();
            if (cfg == null) {
                PayaraLoggingTracer.error(getClass(), "Logging configuration is not available!");
                return;
            }
            final ReconfigurationAction reconfig = new ReconfigurationAction(cfg);
            manager.reconfigure(cfg, reconfig, this::flushEarlyMessages);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, LogFacade.ERROR_APPLYING_CONF, e);
        }
    }


    private void flushEarlyMessages() {
        final ArrayBlockingQueue<LogRecord> catchEarlyMessage = EarlyLogHandler.earlyMessages;
        while (!catchEarlyMessage.isEmpty()) {
            LogRecord logRecord = catchEarlyMessage.poll();
            if (logRecord != null) {
                LOG.log(logRecord);
            }
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

    private final class ReconfigurationAction implements Action {

        private final PayaraLogManagerConfiguration cfg;
        private final ClassLoader classLoader;

        private ReconfigurationAction(final PayaraLogManagerConfiguration cfg) {
            this.cfg = cfg;
            this.classLoader = Thread.currentThread().getContextClassLoader();
        }


        @Override
        public ClassLoader getClassLoader() {
            return this.classLoader;
        }


        @Override
        public void run() {
            createOrUpdatePayaraLogHandler();

            final Map<String, Level> loggerLevels = new HashMap<>();
            final Map<String, Level> handlerLevels = new HashMap<>();
            final Handler[] rootHandlers = getRootHandlers();
            LOG.config(() -> "Actual root handlers=" + Arrays.toString(rootHandlers));
            cfg.toStream().forEach(entry -> {
                if (checkLevels(entry.getKey(), entry.getValue(), handlerLevels, loggerLevels)) {
                    return;
                }
            });

            for (Handler handler : rootHandlers) {
                handler.setLevel(handlerLevels.getOrDefault(handler.getClass().getName(), Level.INFO));
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
