/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.logging.jul;

import fish.payara.logging.jul.internal.PayaraLoggingTracer;
import fish.payara.logging.jul.internal.StartupQueue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fish.payara.logging.jul.LoggingConfigurationHelper.PRINT_TO_STDERR;
import static java.util.logging.Logger.GLOBAL_LOGGER_NAME;


/**
 * @author David Matejcek
 */
public class PayaraLogManager extends LogManager {

    /** Property key for a list of root handler implementations */
    public static final String KEY_ROOT_HANDLERS = "handlers";
    /** Property key for a level of system root logger. System root loggers children are not configurable. */
    public static final String KEY_SYS_ROOT_LOGGER_LEVEL = "systemRootLoggerLevel";
    /** Property key for a level of user root logger. User root loggers children can have own level.  */
    public static final String KEY_USR_ROOT_LOGGER_LEVEL = ".level";
    /** Empty string - standard root logger name */
    public static final String ROOT_LOGGER_NAME = "";

    private static volatile boolean isAfterInitialization;

    private static final AtomicBoolean protectBeforeReset = new AtomicBoolean(true);

    // Cannot be static, log manager is initialized very early
    private final PrintStream originalStdOut;
    private final PrintStream originalStdErr;

    private volatile PayaraLoggingStatus status = PayaraLoggingStatus.UNCONFIGURED;
    private volatile PayaraLogger systemRootLogger;
    private volatile PayaraLogger userRootLogger;
    private volatile PayaraLogger globalLogger;

    private PayaraLogManagerConfiguration cfg;

    private static boolean isAfterInitialization() {
        return isAfterInitialization;
    }


    static synchronized boolean initialize(final Properties configuration) throws SecurityException, IOException {
        PayaraLoggingTracer.trace(PayaraLogManager.class, "initialize(configuration)");
        if (isAfterInitialization) {
            PayaraLoggingTracer.error(PayaraLogManager.class,
                "Initialization of the logging system failed - it was already executed");
            return false;
        }
        try {
            // We must respect that LogManager.getLogManager()
            // - creates final root and global loggers,
            // - calls also addLogger.
            // - calls setLevel if the level was not set in addLogger.
            final PayaraLogManager logManager = getLogManager();
            logManager.doFirstInitialization(configuration);
            return true;
        } finally {
            isAfterInitialization = true;
        }
    }


    public static boolean isPayaraLogManager() {
        return isPayaraLogManager(LogManager.getLogManager());
    }


    public static PayaraLogManager getLogManager() {
        final LogManager logManager = LogManager.getLogManager();
        if (isPayaraLogManager(logManager)) {
            return (PayaraLogManager) logManager;
        }
        PayaraLoggingTracer.error(PayaraLogManager.class, "PayaraLogManager not available, using " + logManager);
        PayaraLoggingTracer.error(PayaraLogManager.class, "Classloader used:" //
            + "\n here:  " + PayaraLogManager.class.getClassLoader() //
            + "\n there: " + logManager.getClass().getClassLoader());
        return null;
    }


    private static boolean isPayaraLogManager(final LogManager logManager) {
        return logManager instanceof PayaraLogManager;
    }


    /**
     * Don't call this constructor directly. Use {@link LogManager#getLogManager()} instead.
     * See {@link LogManager} javadoc for more.
     */
    public PayaraLogManager() {
        PayaraLoggingTracer.trace(PayaraLogManager.class, "new PayaraLogManager()");
        this.originalStdOut = System.out;
        this.originalStdErr = System.err;
        this.cfg = new PayaraLogManagerConfiguration(new Properties());
    }


    private void doFirstInitialization(final Properties configuration) throws SecurityException, IOException {
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "Initializing logManager: " + this);
        try {
            protectBeforeReset.set(false);
            this.cfg = new PayaraLogManagerConfiguration(configuration);
            initializeRootLoggers();
            reconfigure(this.cfg);
        } finally {
            protectBeforeReset.set(true);
        }
    }


    @Override
    public String getProperty(final String name) {
        return this.cfg.getProperty(name);
    }


    @Override
    public boolean addLogger(final Logger logger) {
        Objects.requireNonNull(logger, "logger is null");
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "addLogger(logger.name=" + logger.getName() + ")");

        if (!isAfterInitialization) {
            // initialization of system loggers in LogManager.ensureLogManagerInitialized
            // ignores output of addLogger. That's why we use wrappers.
            if (ROOT_LOGGER_NAME.equals(logger.getName())) {
                PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "System root logger catched: " + logger + ")");
                this.systemRootLogger = new PayaraLoggerWrapper(logger);
                // do not add system logger to user context. Create own root instead.
                // reason: LogManager.ensureLogManagerInitialized ignores result of addLogger,
                // so there is no way to override it. So leave it alone.
                this.userRootLogger = new PayaraLogger(ROOT_LOGGER_NAME);
                return super.addLogger(userRootLogger);
            }
            if (GLOBAL_LOGGER_NAME.equals(logger.getName())) {
                PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "System global logger catched: " + logger + ")");
                this.globalLogger = new PayaraLoggerWrapper(Logger.getGlobal());
                return super.addLogger(globalLogger);
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
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "getLogger(name=" + name + ")");
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
        // first request calls addLogger, which caches the logger, but returns unwrapped logger.
        // second request is from the cache OR is special logger like global.
        return ensurePayaraLoggerOrWrap(super.getLogger(name));
    }


    /**
     * Don't call this method. It is used by {@link LogManager} and removes all handlers, so
     * Payara logging will not work after that.
     */
    @Override
    public synchronized void reset() {
        // reset causes closing of current handlers
        // reset is invoked automatically also in the begining of super.readConfiguration(is).
        // btw LogManager.createLogHandlers exists in JDK11, but not in JDK8
        if (protectBeforeReset.get()) {
            PayaraLoggingTracer.trace(PayaraLogManager.class, "reset() ignored.");
            return;
        }
        super.reset();
        PayaraLoggingTracer.trace(PayaraLogManager.class, "reset() done.");
    }


    /**
     * Ignored. Does nothing!
     */
    @Override
    public synchronized void readConfiguration() throws SecurityException, IOException {
        PayaraLoggingTracer.trace(PayaraLogManager.class, "readConfiguration() ignored.");
    }


    @Override
    public synchronized void readConfiguration(final InputStream input) throws SecurityException, IOException {
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "readConfiguration(ins=" + input + ")");
        this.cfg = new PayaraLogManagerConfigurationParser().parse(input);
        PayaraLoggingTracer.trace(PayaraLogManager.class, "readConfiguration(input) done.");
    }

    public void reconfigure(final PayaraLogManagerConfiguration cfg) {
        reconfigure(cfg, null, null);
    }


    public synchronized void resetAndReadConfiguration(final InputStream input) throws SecurityException, IOException {
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "resetAndReadConfiguration(ins=" + input + ")");
        try {
            protectBeforeReset.set(false);
            reset();
            readConfiguration(input);
        } finally {
            protectBeforeReset.set(true);
        }
    }


    public synchronized void resetAndReadConfiguration(final File loggingProperties) throws SecurityException, IOException {
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "resetAndReadConfiguration(file=" + loggingProperties + ")");
        try (InputStream is = new FileInputStream(loggingProperties)) {
            resetAndReadConfiguration(is);
        }
    }

    public PayaraLoggingStatus getLoggingStatus() {
        return status;
    }


    private void setLoggingStatus(final PayaraLoggingStatus status) {
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "setLoggingStatus(status=" + status + ")");
        this.status = status;
    }


    public PayaraLogHandler getPayaraLogHandler() {
        return getRootLogger().getHandler(PayaraLogHandler.class);
    }


    public List<PayaraLogger> getAllLoggers() {
        return Collections.list(getLoggerNames()).stream().map(this::getLogger).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }


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


    public void resetStandardOutputs() {
        System.setOut(originalStdOut);
        System.setErr(originalStdErr);
    }


    public PrintStream getOriginalStdOut() {
        return originalStdOut;
    }


    public PrintStream getOriginalStdErr() {
        return originalStdErr;
    }

    @FunctionalInterface
    public interface Action {
        void run();
    }


    public synchronized void reconfigure(final PayaraLogManagerConfiguration cfg, final Action reconfigureAction,
        final Action flushAction) {
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "reconfigure(cfg, action, action); Configuration:\n" + cfg);
        setLoggingStatus(PayaraLoggingStatus.CONFIGURING);
        this.cfg = cfg;
        // it is immediately used to configure new objects in LogManager class
        final Thread currentThread = Thread.currentThread();
        final ClassLoader originalCL = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(getLoggingBootClassloader());
            PayaraLoggingTracer.trace(PayaraLogManager.class, "Reconfiguring logger levels...");
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
                    PayaraLoggingTracer.trace(PayaraLogManager.class,
                        "Configuring logger level for '" + existingLoggerName + "' to '" + level + "'");
                    // null means inherit from parent
                    logger.setLevel(level);
                }
            }
            PayaraLoggingTracer.trace(PayaraLogManager.class, "Updated logger levels successfully.");

            if (reconfigureAction != null) {
                reconfigureAction.run();
            }

            final Predicate<Handler> isReady = h -> !PayaraLogHandler.class.isInstance(h)
                || PayaraLogHandler.class.cast(h).isReady();
            if (Arrays.stream(getRootLogger().getHandlers()).allMatch(isReady)) {
                setLoggingStatus(PayaraLoggingStatus.FLUSHING_BUFFERS);
                if (flushAction != null) {
                    flushAction.run();
                }
                final StartupQueue queue = StartupQueue.getInstance();
                queue.toStream().forEach(o -> o.getLogger().checkAndLog(o.getRecord()));
                queue.reset();
                setLoggingStatus(PayaraLoggingStatus.FULL_SERVICE);
            }
        } finally {
            currentThread.setContextClassLoader(originalCL);
            PayaraLoggingTracer.setTracing(cfg.isTracingEnabled());
        }
    }


    public ClassLoader getLoggingBootClassloader() {
        return PayaraLogManagerInitializer.class.getClassLoader();
    }

    public void closeAllExternallyManagedLogHandlers() {
        PayaraLoggingTracer.trace(PayaraLogManager.class, "closeAllExternallyManagedLogHandlers()");
        final List<PayaraLogger> loggers = getAllLoggers();
        // single handler instance can be used by more loggers
        final Set<Handler> handlersToClose = new HashSet<>();
        final Consumer<PayaraLogger> remover = logger -> {
            final List<Handler> handlers = logger.getHandlers(ExternallyManagedLogHandler.class);
            handlersToClose.addAll(handlers);
            handlers.forEach(logger::removeHandler);
        };
        loggers.forEach(remover);
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "Handlers to be closed: " + handlersToClose);
        handlersToClose.forEach(Handler::close);
    }

    private static PayaraLogger replaceWithPayaraLogger(final Logger logger) {
        PayaraLoggingTracer.trace(PayaraLogManager.class, "replaceWithPayaraLogger(" + logger.getName() + ")");
        if (logger instanceof PayaraLogger) {
            return (PayaraLogger) logger;
        }
        return new PayaraLogger(logger);
    }


    // FIXME: delete!
    private PayaraLogger ensurePayaraLoggerOrWrap(final Logger logger) {
        if (logger instanceof PayaraLogger) {
            return (PayaraLogger) logger;
        }
        PayaraLoggingTracer.error(getClass(), "Emergency wrapping logger!", new RuntimeException());
        return new PayaraLoggerWrapper(logger);
    }


    private void initializeRootLoggers() {
        PayaraLoggingTracer.trace(PayaraLogManager.class, "initializeRootLoggers()");
        this.globalLogger.setParent(getRootLogger());
        final PayaraLogger referenceLogger = getRootLogger();
        final LoggingConfigurationHelper parser = new LoggingConfigurationHelper(PRINT_TO_STDERR);
        final List<String> requestedHandlers = parser.getList(KEY_ROOT_HANDLERS, null);
        final List<Handler> currentHandlers = Arrays.asList(referenceLogger.getHandlers());

        // this is to have a single handler for both loggers without common parent logger.
        final List<Handler> handlersToAdd = new ArrayList<>();
        for (final String handlerClass : requestedHandlers) {
            if (currentHandlers.stream().noneMatch(h -> h.getClass().getName().equals(handlerClass))) {
                final Handler newHandler = create(handlerClass);
                if (newHandler != null) {
                    handlersToAdd.add(newHandler);
                }
            }
        }

        final Level systemRootLevel = getLevel(KEY_SYS_ROOT_LOGGER_LEVEL, Level.INFO);
        configureRootLogger(systemRootLogger, systemRootLevel, requestedHandlers, handlersToAdd);

        final Level rootLoggerLevel = getLevel(KEY_USR_ROOT_LOGGER_LEVEL, Level.INFO);
        configureRootLogger(userRootLogger, rootLoggerLevel, requestedHandlers, handlersToAdd);
        setUserRootLoggerMissingParents(userRootLogger);

        // TODO: probably check for system root loggers as parents and move them to user?
    }

    private void setUserRootLoggerMissingParents(final PayaraLogger rootParentLogger) {
        final Enumeration<String> names = getLoggerNames();
        while (names.hasMoreElements()) {
            final String name = names.nextElement();
            final PayaraLogger logger = getLogger(name);
            if (logger != null && logger.getParent() == null && !ROOT_LOGGER_NAME.equals(logger.getName())) {
                PayaraLoggingTracer.error(getClass(), "Setting parent to logger: " + logger.getName() + "/" + logger);
                logger.setParent(rootParentLogger);
            }
        }
    }


    private void configureRootLogger(final PayaraLogger rootLogger, final Level level, final List<String> requestedHandlers, final List<Handler> handlersToAdd) {
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "configureRootLogger(rootLogger=" + rootLogger + ", level=" + level
            + ", requestedHandlers=" + requestedHandlers + ")");
        rootLogger.setLevel(level);
        final List<Handler> currentHandlers = Arrays.asList(rootLogger.getHandlers());
        if (requestedHandlers == null || requestedHandlers.isEmpty()) {
            PayaraLoggingTracer.error(PayaraLogManager.class, "No handlers set for the root logger!");
            return;
        }
        for (final Handler handler : currentHandlers) {
            if (requestedHandlers.stream().noneMatch(name -> name.equals(handler.getClass().getName()))) {
                // FIXME: does not respect handlerServices, will remove them, but they are in separate property and cofigured by different Action
                rootLogger.removeHandler(handler);
                handler.close();
            }
        }
        for (final Handler handler : handlersToAdd) {
            rootLogger.addHandler(handler);
        }
        // handlers which are already registered will not be removed and added again.
    }


    private Level getLevel(final String property, final Level defaultLevel) {
        final String levelProperty = this.cfg.getProperty(property);
        if (levelProperty == null || levelProperty.isEmpty()) {
            return defaultLevel;
        }
        try {
            return Level.parse(levelProperty);
        } catch (final IllegalArgumentException e) {
            PayaraLoggingTracer.error(PayaraLogManager.class,
                "Could not parse level " + levelProperty + ", returning " + defaultLevel + ".", e);
            return defaultLevel;
        }
    }


    @SuppressWarnings("unchecked") // always safe
    private static <T> T create(final String clazz) {
        PayaraLoggingTracer.trace(PayaraLogManager.class, () -> "create(clazz=" + clazz + ")");
        try {
            return (T) Class.forName(clazz).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            PayaraLoggingTracer.error(PayaraLogManager.class, "Could not create " + clazz, e);
            return null;
        }
    }
}
