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

package fish.payara.logging.jul;

import fish.payara.logging.jul.event.LogEvent;
import fish.payara.logging.jul.event.LogEventBroadcaster;
import fish.payara.logging.jul.event.LogEventImpl;
import fish.payara.logging.jul.event.LogEventListener;
import fish.payara.logging.jul.formatter.AnsiColorFormatter;
import fish.payara.logging.jul.formatter.BroadcastingFormatter;
import fish.payara.logging.jul.i18n.MessageResolver;
import fish.payara.logging.jul.internal.EnhancedLogRecord;
import fish.payara.logging.jul.internal.LogRecordBuffer;
import fish.payara.logging.jul.internal.MeteredStream;
import fish.payara.logging.jul.internal.PayaraLoggingTracer;
import fish.payara.logging.jul.rotation.DailyLogRotationTimerTask;
import fish.payara.logging.jul.rotation.LogRotationTimerTask;
import fish.payara.logging.jul.rotation.PeriodicalLogRotationTimerTask;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.PrivilegedAction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.zip.GZIPOutputStream;

import static java.security.AccessController.doPrivileged;

/**
 * @author David Matejcek
 */
// FIXME: uses the file until the end, but another run is already starting. _ThreadName=FelixStartLevel vs _ThreadName=main
public class PayaraLogHandler extends StreamHandler implements LogEventBroadcaster, ExternallyManagedLogHandler {

    private static final String LOGGER_NAME_STDOUT = "javax.enterprise.logging.stdout";
    private static final String LOGGER_NAME_STDERR = "javax.enterprise.logging.stderr";
    private static final Logger STDOUT_LOGGER = Logger.getLogger(LOGGER_NAME_STDOUT);
    private static final Logger STDERR_LOGGER = Logger.getLogger(LOGGER_NAME_STDERR);

    private static final MessageResolver MSG_RESOLVER = new MessageResolver();

    private static final String LOG_ROTATE_DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ss";
    private static final String GZIP_EXTENSION = ".gz";

    // This is a OutputStream to keep track of number of bytes
    // written out to the stream
    // FIXME: can be null if there are no rolling limit set
    private MeteredStream meter;

    private LoggingOutputStream stdoutOutputStream;
    private LoggingOutputStream stderrOutputStream;

    private final LogRecordBuffer logRecordBuffer;
    private LogRotationTimerTask rotationTimerTask;

    private PayaraLogHandlerConfiguration configuration;

    private final Object rotationLock = new Object();
    private final Timer rotationTimer = new Timer("log-rotation-timer-for-" + getClass().getSimpleName());
    private final List<LogEventListener> logEventListeners = new ArrayList<>();

    private volatile PayaraLogHandlerStatus status;
    private LoggingPump pump;

    public PayaraLogHandler() {
        this(new JulConfigurationFactory().createPayaraLogHandlerConfiguration(PayaraLogHandler.class, "server.log"));
    }


    public PayaraLogHandler(final PayaraLogHandlerConfiguration configuration) {
        PayaraLoggingTracer.trace(PayaraLogHandler.class, () -> "PayaraLogHandler(configuration=" + configuration + ")");
        // parent StreamHandler already set level, filter, encoding and formatter.
        setLevel(configuration.getLevel());
        setEncoding(configuration.getEncoding());

        this.logRecordBuffer = new LogRecordBuffer( //
            configuration.getBufferCapacity(), configuration.getBufferTimeout());

        reconfigure(configuration);
    }


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
        return this.configuration;
    }


    public boolean addLogEventListener(LogEventListener listener) {
        if (logEventListeners.contains(listener)) {
            return false;
        }
        return logEventListeners.add(listener);
    }


    public boolean removeLogEventListener(LogEventListener listener) {
        return logEventListeners.remove(listener);
    }


    public synchronized void reconfigure(final PayaraLogHandlerConfiguration newConfiguration) {
        PayaraLoggingTracer.trace(PayaraLogHandler.class, () -> "reconfigure(configuration=" + configuration + ")");
        this.status = PayaraLogHandlerStatus.ACCEPTING;
        stopPump();
        this.configuration = newConfiguration;
        try {
            this.status = startLoggingIfPossible();
        } catch (final Exception e) {
            this.status = PayaraLogHandlerStatus.OFF;
            throw e;
        }
    }


    private PayaraLogHandlerStatus startLoggingIfPossible() {
        PayaraLoggingTracer.trace(PayaraLogHandler.class, "startLoggingIfPossible()");

        if (!this.configuration.isLogToFile() || this.configuration.getLogFile() == null) {
            PayaraLoggingTracer.trace(PayaraLogHandler.class, "Configuration is incomplete, but acceptation will start.");
            return PayaraLogHandlerStatus.ACCEPTING;
        }

        closeOutputStream();

        final Formatter formatter = configuration.getFormatterConfiguration();
        if (formatter instanceof AnsiColorFormatter) {
            ((AnsiColorFormatter) formatter).setDelegate(this.configuration.getFormatterDelegate());
        }
        if (BroadcastingFormatter.class.isInstance(formatter)) {
            final BroadcastingFormatter broadcast = (BroadcastingFormatter) formatter;
            broadcast.setProductId(this.configuration.getProductId());
            broadcast.setLogEventBroadcaster(this);
        }
        final String detectedFormatterName = detectFormatter(configuration.getLogFile());
        if (detectedFormatterName != null && !formatter.getClass().getName().equals(detectedFormatterName)) {
            rotate(this);
        }
        setFormatter(formatter);

        if (this.configuration.isRotationOnDateChange()) {
            initRotationOnDateChange();
        } else if (this.configuration.getRotationTimeLimitValue() > 0) {
            initRotationOnTimeLimit();
        }

        openOutputStream(this.configuration.getLogFile());

        // enable only if everything else was ok to prevent situation when
        // something would break and we would redirect STDOUT+STDERR
        if (this.configuration.isLogStandardStreams()) {
            initStandardStreamsLogging();
        } else if (PayaraLogManager.isPayaraLogManager()) {
            PayaraLogManager.getLogManager().resetStandardOutputs();
        }

        this.pump = new LoggingPump("PayaraLogHandler log pump");
        this.pump.start();
        return PayaraLogHandlerStatus.ON;
    }


    private synchronized void stopPump() {
        if (this.pump != null) {
            this.pump.interrupt();
            this.pump = null;
        }
        drainAllPendingRecords();
        flush();
    }


    @Override
    public synchronized void close() {
        PayaraLoggingTracer.trace(PayaraLogHandler.class, "close()");
        this.status = PayaraLogHandlerStatus.OFF;
        stopPump();
        try {
            if (PayaraLogManager.isPayaraLogManager()) {
                PayaraLogManager.getLogManager().resetStandardOutputs();
            }
            if (this.stdoutOutputStream != null) {
                this.stdoutOutputStream.close();
                this.stdoutOutputStream = null;
            }

            if (this.stderrOutputStream != null) {
                this.stderrOutputStream.close();
                this.stderrOutputStream = null;
            }
        } catch (IOException e) {
            PayaraLoggingTracer.error(PayaraLogHandler.class, "close partially failed!", e);
        }

        super.close();
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
            // configuration is incomplete, but acceptation can start
            // this prevents deadlocks.
            // At this state we cannot decide if the record is loggable
            logRecordBuffer.add(MSG_RESOLVER.resolve(record));
            return;
        }
        if (!isLoggable(record)) {
            return;
        }

        final EnhancedLogRecord recordWrapper = MSG_RESOLVER.resolve(record);
        logRecordBuffer.add(recordWrapper);
        // if we have formatter with this capability, it will do that.
        if (!LogEventBroadcaster.class.isInstance(getFormatter())) {
            final LogEvent logEvent = new LogEventImpl(recordWrapper);
            informLogEventListeners(logEvent);
        }
    }


    @Override
    public boolean isLoggable(final LogRecord record) {
        return this.configuration.isLogToFile() && super.isLoggable(record);
    }


    @Override
    public void informLogEventListeners(LogEvent logEvent) {
        for (LogEventListener listener : logEventListeners) {
            listener.messageLogged(logEvent);
        }
    }


    @Override
    public void flush() {
        super.flush();
        if (this.configuration != null && this.configuration.getLimitForFileRotation() > 0 && meter != null
            && meter.getBytesWritten() >= this.configuration.getLimitForFileRotation()) {
            // If we have written more than the limit set for the
            // file, or rotation requested from the Timer Task or LogMBean
            // start fresh with a new file after renaming the old file.
            rotate();
        }
    }


    @Override
    public String toString() {
        return super.toString() + "[status=" + status + ", buffer=" + this.logRecordBuffer + ", file="
            + configuration.getLogFile() + "]";
    }

    // FIXME move to a class responsible for maintaining stderr/stdout
    private void initStandardStreamsLogging() {
        PayaraLoggingTracer.trace(PayaraLogHandler.class, "initStandardStreamsLogging()");
        this.stdoutOutputStream = new LoggingOutputStream(STDOUT_LOGGER, Level.INFO, 5000);
        LoggingOutputStream.LoggingPrintStream pout = stdoutOutputStream.new LoggingPrintStream(stdoutOutputStream);
        System.setOut(pout);

        // FIXME: capacity not configurable
        this.stderrOutputStream = new LoggingOutputStream(STDERR_LOGGER, Level.SEVERE, 1000);
        LoggingOutputStream.LoggingPrintStream perr = stderrOutputStream.new LoggingPrintStream(stderrOutputStream);
        System.setErr(perr);
    }

    private void initRotationOnDateChange() {
        if (rotationTimerTask != null) {
            rotationTimerTask.cancel();
        }
        if (this.configuration.isRotationOnDateChange()) {
           final LogRotationTimerTask task = new DailyLogRotationTimerTask(this::rotate);
           scheduleNew(task);
        }
    }

    private void initRotationOnTimeLimit() {
        if (rotationTimerTask != null) {
            rotationTimerTask.cancel();
        }
        if (!this.configuration.isRotationOnDateChange() && this.configuration.getRotationTimeLimitValue() > 0) {
            final long delayInMillis = this.configuration.getRotationTimeLimitValue() * 60 * 1000L;
            final LogRotationTimerTask task = new PeriodicalLogRotationTimerTask(this::rotate, delayInMillis);
            scheduleNew(task);
        }
    }

    private void scheduleNew(final LogRotationTimerTask timerTask) {
        rotationTimerTask = timerTask;
        rotationTimer.schedule(rotationTimerTask, rotationTimerTask.computeDelayInMillis());
    }

    private void scheduleNext() {
        if (rotationTimerTask == null) {
            return;
        }
        rotationTimerTask.cancel();
        rotationTimerTask = rotationTimerTask.createNewTask();
        rotationTimer.schedule(rotationTimerTask, rotationTimerTask.computeDelayInMillis());
    }


    public void rotate() {
        rotate(this);
    }


    /**
     * A Simple rotate method to close the old file and start the new one
     * when the limit is reached.
     */
    private static void rotate(final PayaraLogHandler handler) {
        PayaraLoggingTracer.trace(PayaraLogHandler.class, () -> "rotate(handler=" + handler + "");

        handler.log(Level.CONFIG, "Executing log rotation action ...");
        final PrivilegedAction<Void> action = () -> {
            synchronized (handler.rotationLock) {
                // schedule next execution, it has no effect on current execution.
                handler.scheduleNext();

                if (handler.meter != null && handler.meter.getBytesWritten() <= 0) {
                    return null;
                }
                handler.closeOutputStream();
                try {
                    if (!handler.configuration.getLogFile().exists()) {
                        final File creatingDeletedLogFile = handler.configuration.getLogFile();
                        creatingDeletedLogFile.createNewFile();
                        return null;
                    }
                    handler.log(Level.INFO, "Rotating log file: " + handler.configuration.getLogFile());
                    final File oldFile = handler.configuration.getLogFile();
                    final String renamedFileName = handler.configuration.getLogFile() + "_"
                        + DateTimeFormatter.ofPattern(LOG_ROTATE_DATE_FORMAT).format(LocalDateTime.now());
                    File rotatedFile = new File(renamedFileName);
                    boolean renameSuccess = oldFile.renameTo(rotatedFile);
                    if (!renameSuccess) {
                        // If we don't succeed with file rename which
                        // most likely can happen on Windows because
                        // of multiple file handles opened. We go through
                        // Plan B to copy bytes explicitly to a renamed
                        // file.
                        Files.copy(handler.configuration.getLogFile().toPath(), rotatedFile.toPath(),
                            StandardCopyOption.ATOMIC_MOVE);
                        final File freshServerLogFile = handler.configuration.getLogFile();
                        // We do this to make sure that server.log
                        // contents are flushed out to start from a
                        // clean file again after the rename..
                        final FileOutputStream fo = new FileOutputStream(freshServerLogFile);
                        fo.close();
                    }
                    final FileOutputStream oldFileFO = new FileOutputStream(oldFile);
                    oldFileFO.close();
                    handler.openOutputStream(handler.configuration.getLogFile());
                    if (handler.configuration.isCompressionOnRotation()) {
                        boolean compressed = handler.gzipFile(rotatedFile);
                        if (compressed) {
                            boolean deleted = rotatedFile.delete();
                            if (!deleted) {
                                throw new IOException(
                                    "Could not delete uncompressed log file: " + rotatedFile.getAbsolutePath());
                            }
                        } else {
                            throw new IOException("Could not compress log file: " + rotatedFile.getAbsolutePath());
                        }
                    }

                    handler.cleanUpHistoryLogFiles();
                } catch (Exception ix) {
                    new ErrorManager().error("Error, could not rotate log file", ix, ErrorManager.GENERIC_FAILURE);
                }
                return null;
            }
        };
        doPrivileged(action);
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


    /**
     * Creates the file and initialized MeteredStream and passes it on to
     * Superclass (java.util.logging.StreamHandler).
     */
    private void openOutputStream(final File file) {
        PayaraLoggingTracer.trace(getClass(), () -> "openOutputStream(" + file + ")");
        // check that the parent directory exists.
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create the parent directory " + parent.getAbsolutePath());
        }
        try {
            FileOutputStream fout = new FileOutputStream(file, true);
            BufferedOutputStream bout = new BufferedOutputStream(fout);
            this.meter = new MeteredStream(bout, file.length());
            setOutputStream(this.meter);
        } catch (Exception e) {
            throw new IllegalStateException("Could not open file for logging: " + file, e);
        }
    }


    private void closeOutputStream() {
        super.close();
    }


    /**
     * cleanup the history log file based on attributes set under logging.properties file".
     * <p/>
     * If it is defined with valid number, we only keep that number of history logfiles;
     * If "max_history_files" is defined without value, then default that number to be 10;
     * If "max_history_files" is defined with value 0, any number of history files are kept.
     */
    private void cleanUpHistoryLogFiles() {
        if (this.configuration.getMaxHistoryFiles() == 0) {
            return;
        }

        synchronized (this.rotationLock) {
            File dir = this.configuration.getLogFile().getParentFile();
            String logFileName = this.configuration.getLogFile().getName();
            if (dir == null) {
                return;
            }
            File[] fset = dir.listFiles();
            List<String> candidates = new ArrayList<>();
            for (int i = 0; fset != null && i < fset.length; i++) {
                if (!logFileName.equals(fset[i].getName()) && fset[i].isFile()
                    && fset[i].getName().startsWith(logFileName)) {
                    candidates.add(fset[i].getAbsolutePath());
                }
            }
            if (candidates.size() <= this.configuration.getMaxHistoryFiles()) {
                return;
            }
            Object[] paths = candidates.toArray();
            Arrays.sort(paths);
            try {
                for (int i = 0; i < paths.length - this.configuration.getMaxHistoryFiles(); i++) {
                    File file = new File((String) paths[i]);
                    boolean delFile = file.delete();
                    if (!delFile) {
                        throw new IOException("Could not delete log file: " + file.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                new ErrorManager().error("FATAL ERROR: COULD NOT DELETE LOG FILE.", e, ErrorManager.GENERIC_FAILURE);
            }
        }
    }



    /**
     * Checks if the file is not null, it is does not need property evaluation, if it is absolute
     * path and if it is not a directory, then it can be used for logging.
     *
     * @param file
     * @return same file if it is usable for logging output
     */
    private static File getUsableFile(final File file) {
        if (file == null || file.getPath().contains("${") || !file.isAbsolute() || file.isDirectory()) {
            return null;
        }
        return file;
    }


    private static String detectFormatter(final File configuredLogFile) {
        // if it is not readable, better throw an io exception than returning null.
        // if the file does not exist, null is the right answer.
        if (configuredLogFile == null || !configuredLogFile.exists()) {
            return null;
        }
        final String strLine;
        try (BufferedReader br = new BufferedReader(new FileReader(configuredLogFile))) {
            strLine = br.readLine();
        } catch (Exception e) {
            new ErrorManager().error(e.getMessage(), e, ErrorManager.GENERIC_FAILURE);
            return null;
        }
        if (strLine == null || strLine.isEmpty()) {
            return null;
        }
        if (LogFormatHelper.isUniformFormatLogHeader(strLine)) {
            return "fish.payara.logging.jul.formatter.UniformLogFormatter";
        }
        if (LogFormatHelper.isODLFormatLogHeader(strLine)) {
            return "fish.payara.logging.jul.formatter.ODLLogFormatter";
        }
        if (LogFormatHelper.isJSONFormatLogHeader(strLine)) {
            return "fish.payara.logging.jul.formatter.JSONLogFormatter";
        }
        return "unknown";
    }


    private boolean gzipFile(File infile) {
        try ( //
            FileInputStream fis = new FileInputStream(infile); //
            FileOutputStream fos = new FileOutputStream(infile.getCanonicalPath() + GZIP_EXTENSION); //
            GZIPOutputStream gzos = new GZIPOutputStream(fos) //
        ) { //
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
            gzos.finish();
            return true;
        } catch (IOException ix) {
            new ErrorManager().error("Error gzipping log file", ix, ErrorManager.GENERIC_FAILURE);
            return false;
        }
    }

    private void drainAllPendingRecords() {
        if (this.logRecordBuffer == null || this.configuration == null || !this.configuration.isLogToFile()) {
            return;
        }
        while (true) {
            if (!publishRecord(this.logRecordBuffer.poll())) {
                return;
            }
        }
    }


    /**
     * Safe logging usable before it is configured.
     */
    private void log(final Level level, final String messageKey, final Object... messageParameters) {
        final LogRecord logRecord = createLogRecord(level, messageKey);
        logRecord.setParameters(messageParameters);
        publish(logRecord);
    }

    private LogRecord createLogRecord(final Level level, final String message) {
        final LogRecord logRecord = new LogRecord(level, message);
        logRecord.setThreadID((int) Thread.currentThread().getId());
        logRecord.setLoggerName(getClass().getName());
        return logRecord;
    }


    private final class LoggingPump extends Thread {

        private LoggingPump(String threadName) {
            super(threadName);
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY);
        }


        @Override
        public void run() {
            PayaraLoggingTracer.trace(PayaraLogHandler.class, () -> "Logging pump for " + logRecordBuffer + " started.");
            while (configuration.isLogToFile() && status == PayaraLogHandlerStatus.ON) {
                try {
                    publishBatchFromBuffer();
                } catch (Exception e) {
                    // Continue the loop without exiting
                    // Something is broken, but we cannot log it
                }
            }
        }


        /**
         * 5005
         * Retrieves the LogRecord from our Queue and store them in the file
         */
        private void publishBatchFromBuffer() {
            if (!publishRecord(logRecordBuffer.pollOrWait())) {
                return;
            }
            if (configuration.getFlushFrequency() > 1) {
                // starting from 1, one record was already published
                for (int i = 1; i < configuration.getFlushFrequency(); i++) {
                    if (!publishRecord(logRecordBuffer.poll())) {
                        break;
                    }
                }
            }
            flush();
        }
    }


    private enum PayaraLogHandlerStatus {
        OFF,
        ACCEPTING,
        ON
    }

}
