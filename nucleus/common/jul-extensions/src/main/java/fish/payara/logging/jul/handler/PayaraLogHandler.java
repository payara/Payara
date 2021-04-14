/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.logging.jul.handler;

import fish.payara.logging.jul.cfg.JulConfigurationFactory;
import fish.payara.logging.jul.cfg.LoggingConfigurationHelper;
import fish.payara.logging.jul.cfg.PayaraLogHandlerConfiguration;
import fish.payara.logging.jul.env.LoggingSystemEnvironment;
import fish.payara.logging.jul.formatter.LogFormatHelper;
import fish.payara.logging.jul.formatter.UniformLogFormatter;
import fish.payara.logging.jul.record.EnhancedLogRecord;
import fish.payara.logging.jul.record.MessageResolver;
import fish.payara.logging.jul.rotation.DailyLogRotationTimerTask;
import fish.payara.logging.jul.rotation.LogFileManager;
import fish.payara.logging.jul.rotation.LogRotationTimerTask;
import fish.payara.logging.jul.rotation.PeriodicalLogRotationTimerTask;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedAction;
import java.util.Timer;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import static fish.payara.logging.jul.cfg.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.DEFAULT_BUFFER_CAPACITY;
import static fish.payara.logging.jul.cfg.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.DEFAULT_BUFFER_TIMEOUT;
import static fish.payara.logging.jul.cfg.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.DEFAULT_ROTATION_LIMIT_BYTES;
import static fish.payara.logging.jul.cfg.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.MINIMUM_ROTATION_LIMIT_BYTES;
import static fish.payara.logging.jul.cfg.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.ROTATION_LIMIT_SIZE;
import static fish.payara.logging.jul.cfg.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.ROTATION_LIMIT_TIME;
import static fish.payara.logging.jul.cfg.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty.ROTATION_ON_DATE_CHANGE;
import static fish.payara.logging.jul.tracing.PayaraLoggingTracer.error;
import static fish.payara.logging.jul.tracing.PayaraLoggingTracer.trace;
import static java.security.AccessController.doPrivileged;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

/**
 * Payara log handler
 * <ul>
 * <li>can redirect output going through STDOUT and STDERR
 * <li>buffers log records
 * </ul>
 * <b>WARNING</b>: If you configure this handler to redirect standard output, you have to prevent
 * the situation when any other handler would use it.
 *
 * @author David Matejcek
 */
public class PayaraLogHandler extends StreamHandler implements ExternallyManagedLogHandler {

    private static final String LOGGER_NAME_STDOUT = "javax.enterprise.logging.stdout";
    private static final String LOGGER_NAME_STDERR = "javax.enterprise.logging.stderr";
    private static final Logger STDOUT_LOGGER = Logger.getLogger(LOGGER_NAME_STDOUT);
    private static final Logger STDERR_LOGGER = Logger.getLogger(LOGGER_NAME_STDERR);
    private static final MessageResolver MSG_RESOLVER = new MessageResolver();

    private LoggingPrintStream stdoutStream;
    private LoggingPrintStream stderrStream;

    private final LogRecordBuffer logRecordBuffer;
    private LogRotationTimerTask rotationTimerTask;

    private PayaraLogHandlerConfiguration configuration;

    private final Timer rotationTimer = new Timer("log-rotation-timer-for-" + getClass().getSimpleName());

    private volatile PayaraLogHandlerStatus status;
    private LoggingPump pump;
    private LogFileManager logFileManager;


    /**
     * Creates the configuration object for this class or it's descendants.
     *
     * @param handlerClass
     * @return the configuration parsed from the property file, usable to call
     *         the {@link #reconfigure(PayaraLogHandlerConfiguration)} method.
     */
    public static PayaraLogHandlerConfiguration createPayaraLogHandlerConfiguration(
        final Class<? extends PayaraLogHandler> handlerClass) {
        final LoggingConfigurationHelper helper = new LoggingConfigurationHelper(handlerClass);
        final PayaraLogHandlerConfiguration configuration = new PayaraLogHandlerConfiguration();
        configuration.setLevel(helper.getLevel("level", Level.ALL));
        configuration.setEncoding(helper.getCharset("encoding", StandardCharsets.UTF_8));
        configuration.setLogToFile(helper.getBoolean("logtoFile", true));
        configuration.setLogFile(helper.getFile("file", null));
        configuration.setLogStandardStreams(helper.getBoolean("logStandardStreams", Boolean.FALSE));

        configuration.setFlushFrequency(helper.getNonNegativeInteger("flushFrequency", 1));
        configuration.setBufferCapacity(helper.getInteger("bufferCapacity", DEFAULT_BUFFER_CAPACITY));
        configuration.setBufferTimeout(helper.getInteger("bufferTimeout", DEFAULT_BUFFER_TIMEOUT));

        final Integer rotationLimit = helper.getInteger(ROTATION_LIMIT_SIZE.getPropertyName(), DEFAULT_ROTATION_LIMIT_BYTES);
        configuration.setLimitForFileRotation(
            rotationLimit >= MINIMUM_ROTATION_LIMIT_BYTES ? rotationLimit : DEFAULT_ROTATION_LIMIT_BYTES);
        configuration.setCompressionOnRotation(helper.getBoolean("compressOnRotation", Boolean.FALSE));
        configuration.setRotationOnDateChange(helper.getBoolean(ROTATION_ON_DATE_CHANGE.getPropertyName(), Boolean.FALSE));
        configuration.setRotationTimeLimitValue(helper.getNonNegativeInteger(ROTATION_LIMIT_TIME.getPropertyName(), 0));
        configuration.setMaxHistoryFiles(helper.getNonNegativeInteger("maxHistoryFiles", 10));

        final Formatter formatter = helper.getFormatter("formatter", UniformLogFormatter.class.getName());
        new JulConfigurationFactory().configureFormatter(formatter, helper);
        configuration.setFormatterConfiguration(formatter);
        return configuration;
    }


    public PayaraLogHandler() {
        this(createPayaraLogHandlerConfiguration(PayaraLogHandler.class));
    }


    public PayaraLogHandler(final PayaraLogHandlerConfiguration configuration) {
        trace(PayaraLogHandler.class, () -> "PayaraLogHandler(configuration=" + configuration + ")");
        // parent StreamHandler already set level, filter, encoding and formatter.
        setLevel(configuration.getLevel());
        setEncoding(configuration.getEncoding());

        this.logRecordBuffer = new LogRecordBuffer( //
            configuration.getBufferCapacity(), configuration.getBufferTimeout());

        reconfigure(configuration);
    }


    @Override
    public boolean isReady() {
        return status == PayaraLogHandlerStatus.ON;
    }


    private void setEncoding(final Charset encoding) {
        try {
            super.setEncoding(encoding.name());
        } catch (final SecurityException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Reached unreachable exception.", e);
        }
    }


    public PayaraLogHandlerConfiguration getConfiguration() {
        return this.configuration.clone();
    }


    public synchronized void reconfigure(final PayaraLogHandlerConfiguration newConfiguration) {
        trace(PayaraLogHandler.class, () -> "reconfigure(configuration=" + newConfiguration + ")");
        // stop using output, but allow collecting records. Logging system can continue to work.
        this.status = PayaraLogHandlerStatus.ACCEPTING;
        if (this.rotationTimerTask != null) {
            // to avoid another task from last configuration runs it's action.
            this.rotationTimerTask.cancel();
            this.rotationTimerTask = null;
        }
        // stop pump. If reconfiguration would fail, it is better to leave it down.
        // records from the buffer will be processed if the last configuration was valid.
        stopPump();
        if (this.logFileManager != null) {
            this.logFileManager.disableOutput();
            this.logFileManager = null;
        }
        this.configuration = newConfiguration;

        try {
            this.status = startLoggingIfPossible();
        } catch (final Exception e) {
            this.status = PayaraLogHandlerStatus.OFF;
            throw e;
        }
    }


    // this is only to be able to provide the handle to the LogFileManager
    @Override
    public void setOutputStream(final OutputStream out) throws SecurityException {
        super.setOutputStream(out);
    }


    /**
     * Does not publish the record, but puts it into the queue buffer to be processed by an internal
     * thread.
     */
    @Override
    public void publish(final LogRecord record) {
        if (this.status == PayaraLogHandlerStatus.OFF) {
            return;
        }
        if (this.status == PayaraLogHandlerStatus.ACCEPTING) {
            // The configuration is incomplete, but acceptation can start.
            // This prevents deadlocks.
            // At this state we cannot decide if the record is loggable
            logRecordBuffer.add(MSG_RESOLVER.resolve(record));
            return;
        }
        if (!isLoggable(record)) {
            return;
        }

        final EnhancedLogRecord enhancedLogRecord = MSG_RESOLVER.resolve(record);
        logRecordBuffer.add(enhancedLogRecord);
    }


    @Override
    public boolean isLoggable(final LogRecord record) {
        // pump might be closed, super.isLoggable would refuse all records then.
        return this.configuration.isLogToFile()
            && (this.status == PayaraLogHandlerStatus.ACCEPTING || super.isLoggable(record));
    }


    @Override
    public void flush() {
        super.flush();
        if (this.logFileManager != null) {
            this.logFileManager.rollIfFileTooBig();
        }
    }


    /**
     * Explicitly rolls the log file.
     */
    public synchronized void roll() {
        trace(PayaraLogHandler.class, "roll()");
        final PrivilegedAction<Void> action = () -> {
            this.logFileManager.roll();
            updateRollSchedule();
            return null;
        };
        doPrivileged(action);
    }


    /**
     * First stops all dependencies using this handler (changes status to
     * {@link PayaraLogHandlerStatus#OFF}, then closes all resources managed
     * by this handler and finally closes the output stream.
     */
    @Override
    public synchronized void close() {
        trace(PayaraLogHandler.class, "close()");
        this.status = PayaraLogHandlerStatus.OFF;
        stopPump();
        try {
            LoggingSystemEnvironment.resetStandardOutputs();
            if (this.stdoutStream != null) {
                this.stdoutStream.close();
                this.stdoutStream = null;
            }

            if (this.stderrStream != null) {
                this.stderrStream.close();
                this.stderrStream = null;
            }
        } catch (final RuntimeException e) {
            error(PayaraLogHandler.class, "close partially failed!", e);
        }

        if (this.logFileManager != null) {
            this.logFileManager.disableOutput();
        }
    }


    @Override
    public String toString() {
        return super.toString() + "[status=" + status + ", buffer=" + this.logRecordBuffer //
            + ", file=" + this.configuration.getLogFile() + "]";
    }


    private PayaraLogHandlerStatus startLoggingIfPossible() {
        trace(PayaraLogHandler.class, "startLoggingIfPossible()");

        if (!this.configuration.isLogToFile()) {
            trace(PayaraLogHandler.class, "logToFile is false, the handler will not process any records.");
            return PayaraLogHandlerStatus.OFF;
        }
        if (this.configuration.getLogFile() == null) {
            trace(PayaraLogHandler.class, "Configuration is incomplete, but acceptation will start.");
            return PayaraLogHandlerStatus.ACCEPTING;
        }

        this.logFileManager = new LogFileManager(this.configuration.getLogFile(),
            this.configuration.getLimitForFileRotation(), this.configuration.isCompressionOnRotation(),
            this.configuration.getMaxHistoryFiles(), this::setOutputStream, super::close);

        final Formatter formatter = configuration.getFormatterConfiguration();
        final String detectedFormatterName = new LogFormatHelper().detectFormatter(configuration.getLogFile());
        if (detectedFormatterName != null && !formatter.getClass().getName().equals(detectedFormatterName)) {
            this.logFileManager.roll();
        }
        setFormatter(formatter);
        this.logFileManager.enableOutput();
        updateRollSchedule();

        // enable only if everything else was ok to prevent situation when
        // something would break and we would redirect STDOUT+STDERR
        if (this.configuration.isLogStandardStreams()) {
            initStandardStreamsLogging();
        } else {
            LoggingSystemEnvironment.resetStandardOutputs();
        }

        this.pump = new LoggingPump("PayaraLogHandler log pump", this.logRecordBuffer);
        this.pump.start();
        return PayaraLogHandlerStatus.ON;
    }


    private synchronized void stopPump() {
        trace(PayaraLogHandler.class, "stopPump()");
        if (this.pump != null) {
            this.pump.interrupt();
            this.pump = null;
        }
        // we cannot publish anything if we don't have the stream configured.
        if (this.logFileManager != null && this.logFileManager.isOutputEnabled()) {
            // This protects us from the risk that this thread will not be fast enough to process
            // all records and more is still coming. Records which would come after this
            // process started will not be processed.
            long counter = this.logRecordBuffer.getSize();
            while (counter-- >= 0) {
                if (!publishRecord(this.logRecordBuffer.poll())) {
                    return;
                }
            }
        }
    }


    private void initStandardStreamsLogging() {
        trace(PayaraLogHandler.class, "initStandardStreamsLogging()");
        // FIXME: capacity should be configurable
        this.stdoutStream = new LoggingPrintStream(STDOUT_LOGGER, INFO, 5000);
        this.stderrStream = new LoggingPrintStream(STDERR_LOGGER, SEVERE, 1000);
        System.setOut(this.stdoutStream);
        System.setErr(this.stderrStream);
    }


    private void updateRollSchedule() {
        trace(PayaraLogHandler.class, "updateRollSchedule()");
        if (rotationTimerTask != null) {
            rotationTimerTask.cancel();
        }
        if (this.configuration.isRotationOnDateChange()) {
            this.rotationTimerTask = new DailyLogRotationTimerTask(this::scheduledRoll);
            this.rotationTimer.schedule(rotationTimerTask, rotationTimerTask.computeDelayInMillis());
        } else if (this.configuration.getRotationTimeLimitValue() > 0) {
            final long delayInMillis = this.configuration.getRotationTimeLimitValue() * 60 * 1000L;
            this.rotationTimerTask = new PeriodicalLogRotationTimerTask(this::scheduledRoll, delayInMillis);
            this.rotationTimer.schedule(rotationTimerTask, rotationTimerTask.computeDelayInMillis());
        }
    }

    /**
     * If the file is not empty, rolls. Then updates the next roll schedule.
     */
    private void scheduledRoll() {
        this.logFileManager.rollIfFileNotEmpty();
        updateRollSchedule();
    }


    /**
     * Really publishes record via super.publish method call.
     *
     * @param record
     * @return true if the record was not null, false if nothing was done.
     */
    private boolean publishRecord(final EnhancedLogRecord record) {
        if (record == null) {
            return false;
        }
        super.publish(record);
        return true;
    }


    private final class LoggingPump extends LoggingPumpThread {

        private LoggingPump(String threadName, LogRecordBuffer buffer) {
            super(threadName, buffer);
        }


        @Override
        protected boolean isShutdownRequested() {
            return !configuration.isLogToFile() || !isReady();
        }


        @Override
        protected int getFlushFrequency() {
            return configuration.getFlushFrequency();
        }

        @Override
        protected boolean logRecord(final EnhancedLogRecord record) {
            return publishRecord(record);
        }

        @Override
        protected void flushOutput() {
            flush();
        }
    }

    private enum PayaraLogHandlerStatus {
        OFF,
        ACCEPTING,
        ON
    }
}
