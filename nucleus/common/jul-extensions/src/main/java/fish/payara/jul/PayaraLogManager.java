/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.jul;

import fish.payara.jul.cfg.ConfigurationHelper;
import fish.payara.jul.cfg.PayaraLogManagerConfiguration;
import fish.payara.jul.cfg.PayaraLogManagerProperty;
import fish.payara.jul.cfg.SortedProperties;
import fish.payara.jul.env.LoggingSystemEnvironment;
import fish.payara.jul.handler.ExternallyManagedLogHandler;
import fish.payara.jul.handler.PayaraLogHandler;
import fish.payara.jul.handler.SimpleLogHandler;
import fish.payara.jul.handler.SimpleLogHandler.SimpleLogHandlerProperty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fish.payara.jul.cfg.PayaraLogManagerProperty.KEY_RELEASE_PARAMETERS_EARLY;
import static fish.payara.jul.cfg.PayaraLogManagerProperty.KEY_RESOLVE_LEVEL_WITH_INCOMPLETE_CONFIGURATION;
import static fish.payara.jul.cfg.PayaraLogManagerProperty.KEY_ROOT_HANDLERS;
import static fish.payara.jul.cfg.PayaraLogManagerProperty.KEY_SYS_ROOT_LOGGER_LEVEL;
import static fish.payara.jul.cfg.PayaraLogManagerProperty.KEY_USR_ROOT_LOGGER_LEVEL;
import static fish.payara.jul.cfg.PayaraLoggingJvmOptions.JVM_OPT_LOGGING_CFG_DEFAULT_LEVEL;
import static fish.payara.jul.cfg.PayaraLoggingJvmOptions.JVM_OPT_LOGGING_CFG_FILE;
import static fish.payara.jul.cfg.PayaraLoggingJvmOptions.JVM_OPT_LOGGING_CFG_USE_DEFAULTS;
import static fish.payara.jul.env.LoggingSystemEnvironment.isReleaseParametersEarly;
import static fish.payara.jul.env.LoggingSystemEnvironment.isResolveLevelWithIncompleteConfiguration;
import static fish.payara.jul.env.LoggingSystemEnvironment.setReleaseParametersEarly;
import static fish.payara.jul.env.LoggingSystemEnvironment.setResolveLevelWithIncompleteConfiguration;
import static fish.payara.jul.tracing.PayaraLoggingTracer.error;
import static fish.payara.jul.tracing.PayaraLoggingTracer.isTracingEnabled;
import static fish.payara.jul.tracing.PayaraLoggingTracer.setTracingEnabled;
import static fish.payara.jul.tracing.PayaraLoggingTracer.stacktrace;
import static fish.payara.jul.tracing.PayaraLoggingTracer.trace;
import static java.util.logging.Logger.GLOBAL_LOGGER_NAME;


/**
 * The custom {@link LogManager} implementation.
 * Main differences:
 * <ul>
 * <li>Well defined lifecycle, see {@link PayaraLoggingStatus}
 * <li>{@link #reset()} method is not available except internal usage.
 * <li>You can use {@link #reconfigure(PayaraLogManagerConfiguration)} method instead.
 * <li>Or you can use {@link #reconfigure(PayaraLogManagerConfiguration, Action, Action)} method,
 * which provides a way to add programatical extension points.
 * </ul>
 * <p>
 * Note: Some methods have complicated implementation, the reason is that JDK {@link LogManager} is
 * not an example of well usable and well extensible class at all.
 *
 * @author David Matejcek
 */
public class PayaraLogManager extends LogManager {

    /** Empty string - standard root logger name */
    public static final String ROOT_LOGGER_NAME = "";

    private static final AtomicBoolean RESET_PROTECTION = new AtomicBoolean(true);
    private static volatile PayaraLoggingStatus status = PayaraLoggingStatus.UNINITIALIZED;
    private static PayaraLogManager payaraLogManager;

    private volatile PayaraLogger systemRootLogger;
    private volatile PayaraLogger userRootLogger;
    private volatile PayaraLogger globalLogger;

    private PayaraLogManagerConfiguration configuration;


    static synchronized boolean initialize(final Properties configuration) {
        trace(PayaraLogManager.class, "initialize(configuration)");
        if (status.ordinal() > PayaraLoggingStatus.UNINITIALIZED.ordinal()) {
            error(PayaraLogManager.class, "Initialization of the logging system failed - it was already executed");
            return false;
        }
        // We must respect that LogManager.getLogManager()
        // - creates final root and global loggers,
        // - calls also addLogger.
        // - calls setLevel if the level was not set in addLogger.
        // OR something already configured another log manager implementation
        // Therefore status is now moved directly to UNCONFIGURED
        status = PayaraLoggingStatus.UNCONFIGURED;
        final PayaraLogManager logManager = getLogManager();
        if (logManager == null) {
            // oh no, another LogManager implementation is already used.
            return false;
        }
        logManager.doFirstInitialization(ensureSortedProperties(configuration));
        return true;
    }


    /**
     * @return true if {@link PayaraLogManager} is configured as the JVM log manager.
     */
    public static boolean isPayaraLogManager() {
        if (payaraLogManager == null) {
            return getLogManager() != null;
        }
        return true;
    }


    /**
     * Returns current {@link PayaraLogManager} instance.
     * <p>
     * If it is not initialized yet, starts the initialization.
     *
     * @return null if the current {@link LogManager} is not an instance of this class.
     */
    public static PayaraLogManager getLogManager() {
        if (payaraLogManager != null) {
            return payaraLogManager;
        }
        synchronized (PayaraLogManager.class) {
            final LogManager logManager = LogManager.getLogManager();
            if (logManager instanceof PayaraLogManager) {
                payaraLogManager = (PayaraLogManager) logManager;
                return payaraLogManager;
            }
            // If the tracing is off and another LogManager implementation is used,
            // we don't need to spam stderr so much
            // But if the tracing is on, do spam a lot, because even tracing is not much useful
            // except this message.
            if (isTracingEnabled()) {
                stacktrace(PayaraLogManager.class,
                    "PayaraLogManager not available, using " + logManager + ". Classloader used:" //
                        + "\n here:  " + PayaraLogManager.class.getClassLoader() //
                        + "\n there: " + logManager.getClass().getClassLoader());
            }
            return null;
        }
    }


    /**
     * @deprecated Don't call this constructor directly. Use {@link LogManager#getLogManager()} instead.
     * See {@link LogManager} javadoc for more.
     */
    @Deprecated
    public PayaraLogManager() {
        trace(getClass(), "new PayaraLogManager()");
        LoggingSystemEnvironment.initialize();
    }


    @Override
    public String getProperty(final String name) {
        return this.configuration == null ? null : this.configuration.getProperty(name);
    }


    /**
     * @return clone of internal configuration properties
     */
    public PayaraLogManagerConfiguration getConfiguration() {
        return this.configuration.clone();
    }


    /**
     * {@inheritDoc}
     * @return false to force caller to refind the new logger, true to inform him that we did not add it.
     */
    @Override
    public boolean addLogger(final Logger logger) {
        Objects.requireNonNull(logger, "logger is null");
        Objects.requireNonNull(logger.getName(), "logger.name is null");
        trace(getClass(), () -> "addLogger(logger.name=" + logger.getName() + ")");

        if (getLoggingStatus().ordinal() < PayaraLoggingStatus.CONFIGURING.ordinal()) {
            try {
                // initialization of system loggers in LogManager.ensureLogManagerInitialized
                // ignores output of addLogger. That's why we use wrappers.
                if (ROOT_LOGGER_NAME.equals(logger.getName())) {
                    trace(getClass(), () -> "System root logger catched: " + logger + ")");
                    this.systemRootLogger = new PayaraLoggerWrapper(logger);
                    // do not add system logger to user context. Create own root instead.
                    // reason: LogManager.ensureLogManagerInitialized ignores result of addLogger,
                    // so there is no way to override it. So leave it alone.
                    this.userRootLogger = new PayaraLogger(ROOT_LOGGER_NAME);
                    return super.addLogger(userRootLogger);
                }
                if (GLOBAL_LOGGER_NAME.equals(logger.getName())) {
                    trace(getClass(), () -> "System global logger catched: " + logger + ")");
                    this.globalLogger = new PayaraLoggerWrapper(Logger.getGlobal());
                    return super.addLogger(globalLogger);
                }
            } finally {
                // if we go directly through constructor without initialize(cfg)
                if (this.systemRootLogger != null && this.globalLogger != null
                    && getLoggingStatus() == PayaraLoggingStatus.UNINITIALIZED) {
                    doFirstInitialization(provideProperties());
                }
            }
        }

        final PayaraLogger replacementLogger = replaceWithPayaraLogger(logger);
        final boolean loggerAdded = super.addLogger(replacementLogger);
        if (loggerAdded && replacementLogger.getParent() == null
            && !ROOT_LOGGER_NAME.equals(replacementLogger.getName())) {
            replacementLogger.setParent(getRootLogger());
        }
        // getLogger must refetch if we wrapped the original instance.
        // note: JUL ignores output for system loggers
        return loggerAdded && replacementLogger == logger;
    }


    @Override
    public PayaraLogger getLogger(final String name) {
        trace(getClass(), "getLogger(name=" + name + ")");
        Objects.requireNonNull(name, "logger name is null");
        // we are hiding the real root and global loggers, because they cannot be overriden
        // directly by PayaraLogger
        if (ROOT_LOGGER_NAME.equals(name)) {
            return getRootLogger();
        }
        if (GLOBAL_LOGGER_NAME.equals(name)) {
            return this.globalLogger;
        }
        final Logger logger = super.getLogger(name);
        if (logger == null) {
            return null;
        }
        if (logger instanceof PayaraLogger) {
            return (PayaraLogger) logger;
        }
        // First request to Logger.getLogger calls LogManager.demandLogger which calls
        // addLogger, which caches the logger and can be overriden, but returns unwrapped
        // logger.
        // Second request is from the cache OR is a special logger like the global logger.
        return ensurePayaraLoggerOrWrap(super.getLogger(name));
    }


    /**
     * Don't use this method, it will not do anything in most cases.
     * It is used just by the {@link LogManager} on startup and removes all handlers.
     */
    @Override
    @Deprecated
    public synchronized void reset() {
        // reset causes closing of current handlers
        // reset is invoked automatically also in the begining of super.readConfiguration(is).
        // btw LogManager.createLogHandlers exists in JDK11, but not in JDK8
        if (RESET_PROTECTION.get()) {
            stacktrace(PayaraLogManager.class, "reset() ignored.");
            return;
        }
        super.reset();
        trace(getClass(), "reset() done.");
    }


    /**
     * Does nothing!
     */
    @Override
    @Deprecated
    public synchronized void readConfiguration() throws SecurityException, IOException {
        trace(getClass(), "readConfiguration() ignored.");
    }


    /**
     * Don't use this method, it is here just for the {@link LogManager}.
     * Use {@link #reconfigure(PayaraLogManagerConfiguration)} instead.
     */
    @Override
    @Deprecated
    public synchronized void readConfiguration(final InputStream input) throws SecurityException, IOException {
        trace(getClass(), () -> "readConfiguration(ins=" + input + ")");
        this.configuration = PayaraLogManagerConfiguration.parse(input);
        trace(getClass(), "readConfiguration(input) done.");
    }


    /**
     * @return {@link PayaraLoggingStatus}, never null
     */
    public PayaraLoggingStatus getLoggingStatus() {
        return status;
    }


    /**
     * Asks the root logger for his {@link PayaraLogHandler} instance. If no such handler used,
     * returns null.
     *
     * @return null or {@link PayaraLogHandler} instance
     */
    public PayaraLogHandler getPayaraLogHandler() {
        return getRootLogger().getHandler(PayaraLogHandler.class);
    }


    /**
     * @return all loggers currently managed by thus log manager (only from user context)
     */
    public List<PayaraLogger> getAllLoggers() {
        return Collections.list(getLoggerNames()).stream().map(this::getLogger).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }


    /**
     * @return all handlers currently managed by this log manager (only from user context)
     */
    public List<Handler> getAllHandlers() {
        final Function<PayaraLogger, Stream<Handler>> toHandler = logger -> Arrays.stream(logger.getHandlers());
        return Collections.list(getLoggerNames()).stream().map(this::getLogger).filter(Objects::nonNull)
            .flatMap(toHandler).collect(Collectors.toList());
    }


    /**
     * @return can be null only when {@link LogManager} does initialization.
     */
    public PayaraLogger getRootLogger() {
        return this.userRootLogger;
    }


    /**
     * Reconfigures the logging system.
     *
     * @param cfg
     */
    public void reconfigure(final PayaraLogManagerConfiguration cfg) {
        reconfigure(cfg, null, null);
    }


    /**
     * Reconfigures the logging system.
     *
     * @param cfg
     * @param reconfigureAction - a callback executed after the reconfiguration of logger levels is
     *            finished. This action may perform some programmatic configuration.
     * @param flushAction - a callback executed after reconfigureAction to flush program's
     *            {@link LogRecord} buffers waiting until the reconfiguration is completed.
     */
    public synchronized void reconfigure(final PayaraLogManagerConfiguration cfg, final Action reconfigureAction,
        final Action flushAction) {
        final long start = System.nanoTime();
        trace(getClass(), () -> "reconfigure(cfg, action, action); Configuration:\n"
            + cfg + "\n reconfigureAction: " + reconfigureAction + "\n flushAction: " + flushAction);
        if (cfg.isTracingEnabled()) {
            // if enabled, start immediately. If not, don't change it yet, it could be set by JVM option.
            setTracingEnabled(cfg.isTracingEnabled());
        }
        setStatus(PayaraLoggingStatus.CONFIGURING);
        this.configuration = cfg;
        final ConfigurationHelper configurationHelper = getConfigurationHelper();
        setReleaseParametersEarly(
            configurationHelper.getBoolean(KEY_RELEASE_PARAMETERS_EARLY, isReleaseParametersEarly()));
        setResolveLevelWithIncompleteConfiguration(configurationHelper
            .getBoolean(KEY_RESOLVE_LEVEL_WITH_INCOMPLETE_CONFIGURATION, isResolveLevelWithIncompleteConfiguration()));
        // it is used to configure new objects in LogManager class
        final Thread currentThread = Thread.currentThread();
        final ClassLoader originalCL = currentThread.getContextClassLoader();
        try {
            trace(PayaraLogManager.class, "Reconfiguring logger levels...");
            final Enumeration<String> existingLoggerNames = getLoggerNames();
            while (existingLoggerNames.hasMoreElements()) {
                final String existingLoggerName = existingLoggerNames.nextElement();
                if (ROOT_LOGGER_NAME.equals(existingLoggerName)) {
                    this.systemRootLogger.setLevel(getLevel(KEY_SYS_ROOT_LOGGER_LEVEL, Level.INFO));
                    this.userRootLogger.setLevel(getLevel(KEY_USR_ROOT_LOGGER_LEVEL, Level.INFO));
                    continue;
                }
                final PayaraLogger logger = getLogger(existingLoggerName);
                if (logger != null) {
                    final Level level = getLevel(existingLoggerName + ".level", null);
                    trace(getClass(), "Configuring logger level for '" + existingLoggerName + "' to '" + level + "'");
                    // null means inherit from parent
                    logger.setLevel(level);
                }
            }
            trace(getClass(), "Updated logger levels successfully.");

            initializeRootLoggers();
            if (reconfigureAction != null) {
                try {
                    currentThread.setContextClassLoader(reconfigureAction.getClassLoader());
                    reconfigureAction.run();
                } finally {
                    currentThread.setContextClassLoader(originalCL);
                }
            }

            final Predicate<Handler> isReadyPredicate = h -> !ExternallyManagedLogHandler.class.isInstance(h)
                || ExternallyManagedLogHandler.class.cast(h).isReady();
            final List<Handler> handlers = getAllHandlers();
            if (handlers.isEmpty() || handlers.stream().allMatch(isReadyPredicate)) {
                setStatus(PayaraLoggingStatus.FLUSHING_BUFFERS);
                if (flushAction != null) {
                    try {
                        currentThread.setContextClassLoader(flushAction.getClassLoader());
                        flushAction.run();
                    } finally {
                        currentThread.setContextClassLoader(originalCL);
                    }
                }
                final StartupQueue queue = StartupQueue.getInstance();
                trace(getClass(), () -> "Count of records waiting in the queue: " + queue.getSize());
                queue.toStream().forEach(o -> o.getLogger().checkAndLog(o.getRecord()));
                queue.reset();
                setStatus(PayaraLoggingStatus.FULL_SERVICE);
            }
        } finally {
            trace(getClass(), "Reconfiguration finished in " + (System.nanoTime() - start) + " ns");
            // regardless of the result, set tracing.
            setTracingEnabled(cfg.isTracingEnabled());
        }
    }


    /**
     * Closes all {@link ExternallyManagedLogHandler} instances managed by this log manager.
     * Should be called ie. by shutdown hooks to release all injected dependencies.
     * Handlers must stop processing records after that.
     */
    public void closeAllExternallyManagedLogHandlers() {
        trace(PayaraLogManager.class, "closeAllExternallyManagedLogHandlers()");
        final List<PayaraLogger> loggers = getAllLoggers();
        // single handler instance can be used by more loggers
        final Set<Handler> handlersToClose = new HashSet<>();
        final Consumer<PayaraLogger> remover = logger -> {
            final List<Handler> handlers = logger.getHandlers(ExternallyManagedLogHandler.class);
            handlersToClose.addAll(handlers);
            handlers.forEach(logger::removeHandler);
        };
        loggers.forEach(remover);
        trace(getClass(), () -> "Handlers to be closed: " + handlersToClose);
        handlersToClose.forEach(Handler::close);
    }


    private void setStatus(final PayaraLoggingStatus status) {
        trace(getClass(), () -> "setLoggingStatus(status=" + status + ")");
        PayaraLogManager.status = status;
    }


    private static PayaraLogger replaceWithPayaraLogger(final Logger logger) {
        trace(PayaraLogManager.class, "replaceWithPayaraLogger(" + logger.getName() + ")");
        if (logger instanceof PayaraLogger) {
            return (PayaraLogger) logger;
        }
        return new PayaraLogger(logger);
    }


    /**
     * This is a failsafe method to wrapp any logger which would miss standard mechanisms.
     * Invocation of this method would mean that something changed in JDK implementation
     * and this module must be updated.
     * <p>
     * Prints error to STDERR if the logger is not a {@link PayaraLogger} instance and wraps
     * it to {@link PayaraLoggerWrapper}.
     *
     * @param logger
     * @return {@link PayaraLogger} or {@link PayaraLoggerWrapper}
     */
    private PayaraLogger ensurePayaraLoggerOrWrap(final Logger logger) {
        if (logger instanceof PayaraLogger) {
            return (PayaraLogger) logger;
        }
        error(getClass(), "Emergency wrapping logger!", new RuntimeException());
        return new PayaraLoggerWrapper(logger);
    }


    private void doFirstInitialization(final SortedProperties properties) {
        trace(getClass(), () -> "Initializing logManager: " + this);
        try {
            RESET_PROTECTION.set(false);
            setStatus(PayaraLoggingStatus.UNCONFIGURED);
            this.configuration = new PayaraLogManagerConfiguration(properties);
            this.globalLogger.setParent(this.userRootLogger);
            initializeRootLoggers();
            reconfigure(this.configuration);
        } catch (final Exception e) {
            error(getClass(), "Initialization of " + this + " failed!", e);
            throw e;
        } finally {
            RESET_PROTECTION.set(true);
        }
    }


    private void initializeRootLoggers() {
        trace(getClass(), "initializeRootLoggers()");
        final PayaraLogger referenceLogger = getRootLogger();
        final List<String> requestedHandlerNames = getConfigurationHelper().getList(KEY_ROOT_HANDLERS, null);
        final List<Handler> currentHandlers = Arrays.asList(referenceLogger.getHandlers());

        final List<Handler> handlersToAdd = new ArrayList<>();
        final List<Handler> handlersToRemove = new ArrayList<>();
        for (final String handlerClass : requestedHandlerNames) {
            if (currentHandlers.stream().noneMatch(h -> h.getClass().getName().equals(handlerClass))) {
                final Handler newHandler = create(handlerClass);
                if (newHandler != null) {
                    handlersToAdd.add(newHandler);
                }
            }
            final List<Handler> existingToReinstantiate = currentHandlers.stream()
                .filter(h -> h.getClass().getName().equals(handlerClass)
                    && !ExternallyManagedLogHandler.class.isAssignableFrom(h.getClass()))
                .collect(Collectors.toList());
            handlersToRemove.addAll(existingToReinstantiate);
            final Function<Handler, Handler> mapper = h -> create(h.getClass().getName());
            handlersToAdd.addAll(
                existingToReinstantiate.stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList()));
        }

        final Level systemRootLevel = getLevel(KEY_SYS_ROOT_LOGGER_LEVEL, Level.INFO);
        final Level rootLoggerLevel = getLevel(KEY_USR_ROOT_LOGGER_LEVEL, Level.INFO);
        // both loggers use same handler set.
        configureRootLogger(systemRootLogger, systemRootLevel, requestedHandlerNames, handlersToRemove, handlersToAdd);
        configureRootLogger(userRootLogger, rootLoggerLevel, requestedHandlerNames, handlersToRemove, handlersToAdd);
        setMissingParentToRootLogger(userRootLogger);
    }


    private ConfigurationHelper getConfigurationHelper() {
        return new ConfigurationHelper(null,  ConfigurationHelper.ERROR_HANDLER_PRINT_TO_STDERR);
    }


    private void setMissingParentToRootLogger(final PayaraLogger rootParentLogger) {
        final Enumeration<String> names = getLoggerNames();
        while (names.hasMoreElements()) {
            final String name = names.nextElement();
            final PayaraLogger logger = getLogger(name);
            if (logger != null && logger.getParent() == null && !ROOT_LOGGER_NAME.equals(logger.getName())) {
                error(getClass(), "Setting parent to logger: " + logger.getName() + "/" + logger);
                logger.setParent(rootParentLogger);
            }
        }
    }


    private void configureRootLogger(final PayaraLogger rootLogger, final Level level,
        final List<String> requestedHandlers, final List<Handler> handlersToRemove, final List<Handler> handlersToAdd) {
        trace(getClass(), () -> "configureRootLogger(rootLogger=" + rootLogger
            + ", level=" + level + ", requestedHandlers=" + requestedHandlers + ")");
        rootLogger.setLevel(level);
        final List<Handler> currentHandlers = Arrays.asList(rootLogger.getHandlers());
        if (requestedHandlers == null || requestedHandlers.isEmpty()) {
            error(getClass(), "No handlers set for the root logger!");
            return;
        }
        for (final Handler handler : handlersToRemove) {
            rootLogger.removeHandler(handler);
            handler.close();
        }
        for (final Handler handler : currentHandlers) {
            if (requestedHandlers.stream().noneMatch(name -> name.equals(handler.getClass().getName()))) {
                rootLogger.removeHandler(handler);
                handler.close();
            }
        }
        for (final Handler handler : handlersToAdd) {
            rootLogger.addHandler(handler);
        }
    }


    private Level getLevel(final PayaraLogManagerProperty property, final Level defaultLevel) {
        return getLevel(property.getPropertyName(), defaultLevel);
    }


    private Level getLevel(final String property, final Level defaultLevel) {
        final String levelProperty = getProperty(property);
        if (levelProperty == null || levelProperty.isEmpty()) {
            return defaultLevel;
        }
        try {
            return Level.parse(levelProperty);
        } catch (final IllegalArgumentException e) {
            error(getClass(), "Could not parse level " + levelProperty + ", returning " + defaultLevel + ".", e);
            return defaultLevel;
        }
    }


    @SuppressWarnings("unchecked") // always safe
    private static <T> T create(final String clazz) {
        trace(PayaraLogManager.class, () -> "create(clazz=" + clazz + ")");
        try {
            // JUL uses SystemClassloader, so with custom formatters always fallbacks to defaults
            // Don't use ConsoleHandler with custom formatters, use SimpleLogHandler instead
            return (T) Class.forName(clazz).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            error(PayaraLogManager.class, "Could not create " + clazz, e);
            return null;
        }
    }


    private static SortedProperties ensureSortedProperties(final Properties properties) {
        if (properties == null) {
            return provideProperties();
        }
        if (properties instanceof SortedProperties) {
            return (SortedProperties) properties;
        }
        return new SortedProperties(properties);
    }


    private static SortedProperties provideProperties() {
        try {
            final SortedProperties propertiesFromJvmOption = toProperties(System.getProperty(JVM_OPT_LOGGING_CFG_FILE));
            if (propertiesFromJvmOption != null) {
                return propertiesFromJvmOption;
            }
            final SortedProperties propertiesFromClasspath = loadFromClasspath();
            if (propertiesFromClasspath != null) {
                return propertiesFromClasspath;
            }
            if (Boolean.getBoolean(JVM_OPT_LOGGING_CFG_USE_DEFAULTS)) {
                return createDefaultProperties();
            }
            throw new IllegalStateException(
                "Could not find any logging.properties configuration file neither from JVM option ("
                    + JVM_OPT_LOGGING_CFG_FILE + ") nor from classpath and even " + JVM_OPT_LOGGING_CFG_USE_DEFAULTS
                    + " wasn't set to true.");
        } catch (final IOException e) {
            throw new IllegalStateException("Could not load logging configuration file.", e);
        }
    }


    private static SortedProperties createDefaultProperties() {
        final SortedProperties cfg = new SortedProperties();
        final String level = System.getProperty(JVM_OPT_LOGGING_CFG_DEFAULT_LEVEL, Level.INFO.getName());
        cfg.setProperty(KEY_SYS_ROOT_LOGGER_LEVEL.getPropertyName(), level);
        cfg.setProperty(KEY_USR_ROOT_LOGGER_LEVEL.getPropertyName(), level);
        cfg.setProperty(KEY_ROOT_HANDLERS.getPropertyName(), SimpleLogHandler.class.getName());
        cfg.setProperty(SimpleLogHandlerProperty.LEVEL.getPropertyFullName(), level);
        return cfg;
    }


    private static SortedProperties toProperties(final String absolutePath) throws IOException {
        if (absolutePath == null) {
            return null;
        }
        final File file = new File(absolutePath);
        if (!file.canRead()) {
            return null;
        }
        return SortedProperties.loadFrom(file);
    }


    private static SortedProperties loadFromClasspath() throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        trace(PayaraLogManager.class, () -> "loadFromClasspath(); classloader: " + classLoader);
        try (InputStream input = classLoader.getResourceAsStream("logging.properties")) {
            if (input == null) {
                return null;
            }
            return SortedProperties.loadFrom(input);
        }
    }


    /**
     * Action to be performed when client calls
     * {@link PayaraLogManager#reconfigure(PayaraLogManagerConfiguration, Action, Action)}
     */
    @FunctionalInterface
    public interface Action {

        /**
         * Custom action to be performed when executing the reconfiguration.
         */
        void run();


        /**
         * @return thread context classloader; can be overriden.
         */
        default ClassLoader getClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }
    }
}
