/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.jul.record.EnhancedLogRecord;
import fish.payara.jul.record.MessageResolver;
import fish.payara.jul.tracing.PayaraLoggingTracer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fish.payara.jul.env.LoggingSystemEnvironment.isResolveLevelWithIncompleteConfiguration;
import static java.util.Objects.requireNonNull;


/**
 * Custom {@link Logger}, integrated with {@link PayaraLogManager}, so if the log manager is reconfiguring,
 * the logger reacts on states of the log manager. Then log records can be redirected to a startup queue
 * (if the logging is not configured or is reconfiguring) or the calling thread is blocked until the log
 * manager finishes flushing of the queue) or processed as usually.
 *
 * @author David Matejcek
 */
public class PayaraLogger extends Logger {

    private static final MessageResolver MSG_RESOLVER = new MessageResolver();
    private static final Function<Logger, Stream<Handler>> LOGER_TO_HANDLER = x -> Arrays.stream(x.getHandlers());

    /**
     * Creates a logger without predefined resource bundle.
     *
     * @param name a logger name
     */
    protected PayaraLogger(final String name) {
        super(name, null);
    }


    /**
     * Creates a copy of the logger. Used just in PayaraLogManager for default system loggers.
     *
     * @param logger
     */
    PayaraLogger(final Logger logger) {
        // resource bundle name is taken from the set resource bundle
        super(requireNonNull(logger, "logger is null!").getName(), null);
        setLevel(logger.getLevel());
        setUseParentHandlers(logger.getUseParentHandlers());
        setFilter(logger.getFilter());
        if (logger.getParent() != null) {
            setParent(logger.getParent());
        }
        if (logger.getResourceBundle() != null) {
            setResourceBundle(logger.getResourceBundle());
        }
        for (final Handler handler : logger.getHandlers()) {
            addHandler(handler);
        }
    }


    @Override
    public void setLevel(final Level newLevel) throws SecurityException {
        PayaraLoggingTracer.trace(PayaraLogger.class, () -> "setLevel(" + newLevel + "); this: " + this);
        super.setLevel(newLevel);
    }


    @Override
    public void addHandler(final Handler handler) throws SecurityException {
        PayaraLoggingTracer.trace(PayaraLogger.class, () -> "addHandler(" + handler + "); this: " + this);
        super.addHandler(handler);
    }


    @Override
    public void removeHandler(final Handler handler) throws SecurityException {
        PayaraLoggingTracer.trace(PayaraLogger.class, () -> "removeHandler(" + handler + "); this: " + this);
        super.removeHandler(handler);
    }


    @Override
    public String toString() {
        return super.toString() + "['" + getName() + "':" + getLevel() + "]";
    }

    /**
     * Searches for a handler of some type and returns the first found.
     * Note: the comparison uses Class.equals, so it respects classloaders.
     *
     * @param <T> {@link Handler} type.
     * @param type
     * @return found handler or null.
     */
    public <T extends Handler> T getHandler(final Class<T> type) {
        return type.cast(Arrays.stream(getHandlers()).filter(h -> h.getClass().equals(type)).findFirst().orElse(null));
    }


    /**
     * Uses {@link #getHandlers()} and filters output to return only handlers implementing the class
     * from a parameter.
     *
     * @param type type used for filtering; can be also an interface
     * @return list of found handlers (may be empty but never null)
     */
    public List<Handler> getHandlers(final Class<?> type) {
        return Arrays.stream(getHandlers()).filter(type::isInstance).collect(Collectors.toList());
    }


    @Override
    public void log(final Level level, final String msg) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        final LogRecord record = new EnhancedLogRecord(level, msg);
        record.setLoggerName(getName());
        record.setResourceBundle(getResourceBundle());
        record.setResourceBundleName(getResourceBundleName());
        logOrQueue(record, status);
    }


    @Override
    public void log(final Level level, final Supplier<String> msgSupplier) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        final LogRecord record = new EnhancedLogRecord(level, msgSupplier.get());
        record.setLoggerName(getName());
        logOrQueue(record, status);
    }


    @Override
    public void log(final Level level, final String msg, final Object param) {
        log(level, msg, new Object[] {param});
    }


    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        final LogRecord record = new EnhancedLogRecord(level, msg);
        record.setLoggerName(getName());
        record.setResourceBundle(getResourceBundle());
        record.setResourceBundleName(getResourceBundleName());
        record.setParameters(params);
        logOrQueue(record, status);
    }


    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        final LogRecord record = new EnhancedLogRecord(level, msg);
        record.setLoggerName(getName());
        record.setResourceBundle(getResourceBundle());
        record.setResourceBundleName(getResourceBundleName());
        record.setThrown(thrown);
        logOrQueue(record, status);
    }


    @Override
    public void log(final Level level, final Throwable thrown, final Supplier<String> msgSupplier) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        final LogRecord record = new EnhancedLogRecord(level, msgSupplier.get());
        record.setLoggerName(getName());
        record.setThrown(thrown);
        logOrQueue(record, status);
    }


    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        logOrQueue(level, status, sourceClass, sourceMethod, msg, null);
    }


    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod,
        final Supplier<String> msgSupplier) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        logOrQueue(level, status, sourceClass, sourceMethod, msgSupplier.get(), null);
    }


    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg,
        final Object param) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        logOrQueue(level, status, sourceClass, sourceMethod, msg, null, param);
    }



    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg,
        final Object[] params) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        logOrQueue(level, status, sourceClass, sourceMethod, msg, null, params);
    }


    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg,
        final Throwable thrown) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        logOrQueue(level, status, sourceClass, sourceMethod, msg, thrown);
    }


    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final Throwable thrown,
        final Supplier<String> msgSupplier) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        logOrQueue(level, status, sourceClass, sourceMethod, msgSupplier.get(), thrown);
    }


    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
        final ResourceBundle bundle, final String msg, final Object... params) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        final LogRecord record = new EnhancedLogRecord(level, msg, false);
        record.setLoggerName(getName());
        record.setSourceClassName(sourceClass);
        record.setSourceMethodName(sourceMethod);
        record.setParameters(params);
        if (bundle != null) {
            record.setResourceBundleName(bundle.getBaseBundleName());
            record.setResourceBundle(bundle);
        }
        logOrQueue(record, status);
    }


    @Override
    public void logrb(final Level level, final String sourceClass, final String sourceMethod,
        final ResourceBundle bundle, final String msg, final Throwable thrown) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(level, status)) {
            return;
        }
        final LogRecord record = new EnhancedLogRecord(level, msg, false);
        record.setLoggerName(getName());
        record.setSourceClassName(sourceClass);
        record.setSourceMethodName(sourceMethod);
        record.setThrown(thrown);
        if (bundle != null) {
            record.setResourceBundleName(bundle.getBaseBundleName());
            record.setResourceBundle(bundle);
        }
        logOrQueue(record, status);
    }


    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(Level.FINER, status)) {
            return;
        }
        String msg = "ENTRY";
        if (params == null || params.length == 0) {
            logp(Level.FINER, sourceClass, sourceMethod, msg);
            return;
        }
        // ' {i}' are 4 characters per parameter. Methods with many parameters will inflate.
        final StringBuilder template = new StringBuilder(msg.length() + 4 * params.length).append(msg);
        for (int i = 0; i < params.length; i++) {
            template.append(" {").append(i).append('}');
        }
        logp(Level.FINER, sourceClass, sourceMethod, template.toString(), params);
    }


    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        logp(Level.FINER, sourceClass, sourceMethod, "THROW", thrown);
    }


    @Override
    public void log(final LogRecord record) {
        final PayaraLoggingStatus status = getLoggingStatus();
        if (!isProcessible(record.getLevel(), status)) {
            return;
        }
        logOrQueue(record, status);
    }


    /**
     * {@inheritDoc}
     * <p>
     * This method is <b>overriden</b> because it is called from clients to check if it makes
     * sense to do some expensive preparation and call of any log method, but we don't
     * know it yet, so we accept everything to queue (it should not be so many records).
     */
    @Override
    public boolean isLoggable(final Level level) {
        return isProcessible(level, getLoggingStatus());
    }


    /**
     * @param level a message logging level
     * @param status the state of the logging system
     * @return true if the given message level is currently being logged or if we don't know yet.
     */
    protected boolean isProcessible(final Level level, final PayaraLoggingStatus status) {
        return isLoggableLevel(level) || !isLevelResolutionPossible(status);
    }


    /**
     * @param level - log record level
     * @return true if level has higher importance than level set to this logger (or parent)
     */
    protected boolean isLoggableLevel(final Level level) {
        return super.isLoggable(level);
    }


    private boolean isFullService(final PayaraLoggingStatus status) {
        return status == PayaraLoggingStatus.FULL_SERVICE;
    }


    private boolean isLevelResolutionPossible(final PayaraLoggingStatus status) {
        return isResolveLevelWithIncompleteConfiguration() || status == PayaraLoggingStatus.FLUSHING_BUFFERS
            || status == PayaraLoggingStatus.FULL_SERVICE;
    }


    private boolean isToQueue(final PayaraLoggingStatus status) {
        return status == PayaraLoggingStatus.UNCONFIGURED || status == PayaraLoggingStatus.CONFIGURING;
    }


    void checkAndLog(final LogRecord record) {
        if (!isLoggableLevel(record.getLevel())) {
            return;
        }
        final Filter filter = getFilter();
        if (filter != null && !filter.isLoggable(record)) {
            return;
        }

        final Predicate<Handler> handlerFilter = h -> h.isLoggable(record);
        final Iterator<Handler> handlers = getLoggers().stream().flatMap(LOGER_TO_HANDLER).filter(handlerFilter)
            .iterator();
        if (!handlers.hasNext()) {
            return;
        }
        final EnhancedLogRecord resolvedLogRecord = MSG_RESOLVER.resolve(record);
        handlers.forEachRemaining(h -> h.publish(resolvedLogRecord));
    }


    private List<Logger> getLoggers() {
        final ArrayList<Logger> loggers = new ArrayList<>();
        Logger log = this;
        do {
            loggers.add(log);
            final boolean useParentHandlers = log.getUseParentHandlers();
            if (!useParentHandlers) {
                break;
            }
            log = log.getParent();
        } while (log != null);
        return loggers;
    }


    private PayaraLoggingStatus getLoggingStatus() {
        if (PayaraLogManager.isPayaraLogManager()) {
            return PayaraLogManager.getLogManager().getLoggingStatus();
        }
        return PayaraLoggingStatus.FULL_SERVICE;
    }


    private void logOrQueue(final Level level, final PayaraLoggingStatus status, final String sourceClass,
        final String sourceMethod, final String msg, final Throwable thrown, final Object... params) {
        final LogRecord record = new EnhancedLogRecord(level, msg, false);
        record.setLoggerName(getName());
        record.setResourceBundle(getResourceBundle());
        record.setResourceBundleName(getResourceBundleName());
        record.setSourceClassName(sourceClass);
        record.setSourceMethodName(sourceMethod);
        record.setThrown(thrown);
        record.setParameters(params);
        logOrQueue(record, status);
    }


    private void logOrQueue(final LogRecord record, final PayaraLoggingStatus status) {
        if (isFullService(status)) {
            checkAndLog(record);
            return;
        }
        if (isToQueue(status)) {
            StartupQueue.getInstance().add(this, record);
            return;
        }
        while (!isFullService(getLoggingStatus())) {
            Thread.yield();
        }
        checkAndLog(record);
    }
}
