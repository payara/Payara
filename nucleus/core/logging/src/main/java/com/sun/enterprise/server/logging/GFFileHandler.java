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
import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.logging.AgentFormatterDelegate;

import fish.payara.enterprise.server.logging.JSONLogFormatter;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.zip.GZIPOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import static java.security.AccessController.doPrivileged;
import org.glassfish.internal.api.Globals;

/**
 * GFFileHandler publishes formatted log Messages to a FILE.
 *
 * @author Jerome Dochez
 * @author Carla Mott
 * @author David Matejcek
 */
@Service
@Singleton
@ContractsProvided({ //
    GFFileHandler.class, java.util.logging.Handler.class, LogEventBroadcaster.class, LoggingRuntime.class //
})
public class GFFileHandler extends StreamHandler
    implements PostConstruct, PreDestroy, LogEventBroadcaster, LoggingRuntime {

    private static final Logger LOG = LogFacade.LOGGING_LOGGER;
    private static final LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(GFFileHandler.class);
    private static final String GF_FILE_HANDLER_ID = GFFileHandler.class.getCanonicalName() ;

    private static final int DEFAULT_BUFFER_CAPACITY = 10000;
    private static final int DEFAULT_BUFFER_TIMEOUT = 0;
    private static final int DEFAULT_ROTATION_LIMIT_BYTES = 2000000;
    public static final int DISABLE_LOG_FILE_ROTATION_VALUE = 0;

    private static final String LOGS_DIR = "logs";
    private static final String LOG_FILE_NAME = "server.log";
    private static final String GZIP_EXTENSION = ".gz";

    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]";
    private static final String RECORD_FIELD_SEPARATOR = "|";
    private static final String RECORD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @Inject
    protected ServerEnvironmentImpl env;

    @Inject @Optional
    private Agent agent;

    // This is a OutputStream to keep track of number of bytes
    // written out to the stream
    private MeteredStream meter;

    /** Configuration value, evaluated to absolute path */
    private File configuredLogFile = null;
    /** Effective value */
    private File currentLogFile = null;
    /** Count of flushed records in one batch, not a frequency at all */
    private int flushFrequency = 1;
    private int maxHistoryFiles = 10;
    private boolean logToFile;
    private boolean rotationOnDateChange;
    private String excludeFields;
    private Integer rotationLimitAttrValue;
    private Long rotationTimeLimitValue;
    private boolean compressionOnRotation;
    private boolean multiLineMode;
    private String fileHandlerFormatter = "";
    private String currentFileHandlerFormatter = "";

    private boolean logStandardStreams;
    private final PrintStream oStdOutBackup = System.out;
    private final PrintStream oStdErrBackup = System.err;
    private LoggingOutputStream stdoutOutputStream = null;
    private LoggingOutputStream stderrOutputStream = null;

    /** Initially the LogRotation will be off until the domain.xml value is read. */
    private int limitForFileRotation = 0;

    private LogRecordBuffer logRecordBuffer;
    private ConcurrentLinkedQueue<LogRecord> startupQueue;
    private LogRotationTimerTask rotationTimerTask;

    private final Object rotationLock = new Object();

    private static final String LOG_ROTATE_DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ss";
    private static final String DEFAULT_LOG_FILE_FORMATTER_CLASS_NAME = UniformLogFormatter.class.getName();
    public static final int MINIMUM_ROTATION_LIMIT_VALUE = 500*1000;

    private Thread pump;

    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final List<LogEventListener> logEventListeners = new ArrayList<>();
    private final LogManager manager = LogManager.getLogManager();
    private final String className = getClass().getName();
    private final Timer rotationTimer = new Timer("log-rotation-timer-for-" + getClass().getSimpleName());


    public GFFileHandler() {
        this.startupQueue = new ConcurrentLinkedQueue<>();
    }


    @Override
    public void postConstruct() {
        configuredLogFile = evaluateFile();
        changeFileName(configuredLogFile);

        // Reading just few lines of log file to get the log formatter used.
        String strLine;
        int odlFormatter = 0;
        int uniformLogFormatter = 0;
        int jsonLogFormatter = 0;
        int otherFormatter = 0;
        boolean mustRotate = false;

        try (BufferedReader br = new BufferedReader(new FileReader(configuredLogFile))) {
            while ((strLine = br.readLine()) != null) {
                strLine = strLine.trim();
                if (!strLine.isEmpty()) {
                    if (LogFormatHelper.isUniformFormatLogHeader(strLine)) {  // for ufl formatter
                        uniformLogFormatter++;
                    } else if (LogFormatHelper.isODLFormatLogHeader(strLine)) {
                        // for ODL formatter
                        odlFormatter++;
                    } else if (LogFormatHelper.isJSONFormatLogHeader(strLine)) {
                        //for JSON Log format
                        jsonLogFormatter++;
                    } else {
                        otherFormatter++;  // for other formatter
                    }

                    // Rotate on startup for custom log files
                    if (otherFormatter > 0) {
                        mustRotate = true;
                    }
                    // Read only first log record line and break out of the loop
                    break;
                }
            }
        } catch (Exception e) {
            ErrorManager em = getErrorManager();
            if (em != null) {
                em.error(e.getMessage(), e, ErrorManager.GENERIC_FAILURE);
            }
        }

        if (odlFormatter > 0) {
            currentFileHandlerFormatter = "com.sun.enterprise.server.logging.ODLLogFormatter";
        } else if (uniformLogFormatter > 0) {
            currentFileHandlerFormatter = "com.sun.enterprise.server.logging.UniformLogFormatter";
        } else if (jsonLogFormatter > 0) {
            currentFileHandlerFormatter = "fish.payara.enterprise.server.logging.JSONLogFormatter";
        }

        initLogToFile();
        log(Level.INFO, LogFacade.GF_VERSION_INFO, Version.getFullVersion());
        configureTimeBasedRotation();

        String propertyValue = manager.getProperty(className + ".rotationLimitInBytes");
        rotationOnFileSizeLimit(propertyValue);

        //setLevel(Level.ALL);
        propertyValue = manager.getProperty(className + ".flushFrequency");
        if (propertyValue != null) {
            try {
                flushFrequency = Integer.parseInt(propertyValue);
            } catch (NumberFormatException e) {
                log(Level.WARNING, LogFacade.INVALID_ATTRIBUTE_VALUE, propertyValue, "flushFrequency");
            }
        }
        if (flushFrequency <= 0) {
            flushFrequency = 1;
        }

        propertyValue = manager.getProperty(className + ".maxHistoryFiles");
        try {
            if (propertyValue != null) {
                maxHistoryFiles = Integer.parseInt(propertyValue);
            }
        } catch (NumberFormatException e) {
            log(Level.WARNING, LogFacade.INVALID_ATTRIBUTE_VALUE, propertyValue, "maxHistoryFiles");
        }

        if (maxHistoryFiles < 0) {
            maxHistoryFiles = 10;
        }

        propertyValue = manager.getProperty(className + ".compressOnRotation");
        compressionOnRotation = false;
        if (propertyValue != null) {
            compressionOnRotation = Boolean.parseBoolean(propertyValue);
        }

        propertyValue = manager.getProperty(className + ".logStandardStreams");
        if (propertyValue != null) {
            logStandardStreams = Boolean.parseBoolean(propertyValue);
            if (logStandardStreams) {
                logStandardStreams();
            }
        }

        String formatterName = manager.getProperty(className + ".formatter");
        formatterName = formatterName == null ? DEFAULT_LOG_FILE_FORMATTER_CLASS_NAME : formatterName;

        // Below snapshot of the code is used to rotate server.log file on startup. It is used to avoid different format
        // log messages logged under same server.log file.
        fileHandlerFormatter = formatterName;
        if (mustRotate) {
            rotate();
        } else if (fileHandlerFormatter != null && !fileHandlerFormatter.equals(currentFileHandlerFormatter)) {
            rotate();
        }
        excludeFields = manager.getProperty(LogManagerService.EXCLUDE_FIELDS_PROPERTY);
        multiLineMode = Boolean.parseBoolean(manager.getProperty(LogManagerService.MULTI_LINE_MODE_PROPERTY));
        configureLogFormatter(formatterName, excludeFields, multiLineMode);
    }

    @Override
    public void preDestroy() {
        LOG.fine("Logger handler killed");

        System.setOut(oStdOutBackup);
        System.setErr(oStdErrBackup);

        try {
            if (stdoutOutputStream != null) {
                stdoutOutputStream.close();
            }

            if (stderrOutputStream != null) {
                stderrOutputStream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        shutdown.set(true);
        if (pump != null) {
            pump.interrupt();
        }

        drainAllPendingRecords();
        flush();
    }

    @Override
    public File getCurrentLogFile() {
        return currentLogFile;
    }

    /**
     * @return if the file is not configured, this filename in standard server directory structure
     *         will be used.
     */
    protected String getDefaultFileName() {
        return LOG_FILE_NAME;
    }

    private void initLogToFile() {
        final String propertyValue = manager.getProperty(className + ".logtoFile");
        final boolean logToFileProperty;
        if (propertyValue == null) {
            logToFileProperty = true;
        } else {
            logToFileProperty = Boolean.parseBoolean(propertyValue);
        }
        if (logToFileProperty) {
            final String capacityProp = manager.getProperty(className + ".bufferCapacity");
            final int capacity = capacityProp == null ? DEFAULT_BUFFER_CAPACITY : Integer.parseInt(capacityProp);
            final String timeoutProp = manager.getProperty(className + ".bufferTimeout");
            final int timeout = timeoutProp == null ? DEFAULT_BUFFER_TIMEOUT : Integer.parseInt(timeoutProp);
            enableLogToFile(capacity, timeout);
        }
    }

    private void configureLogFormatter(String formatterName, String excludeFields, boolean multiLineMode) {
        if (UniformLogFormatter.class.getName().equals(formatterName)) {
            configureUniformLogFormatter(excludeFields, multiLineMode);
        } else if (ODLLogFormatter.class.getName().equals(formatterName)) {
            configureODLFormatter(excludeFields, multiLineMode);
        } else if (JSONLogFormatter.class.getName().equals(formatterName)) {
            configureJSONFormatter(excludeFields);
        } else {
            // Custom formatter is configured in logging.properties
            // Check if the user specified formatter is in play else
            // log an error message
            Formatter currentFormatter = this.getFormatter();
            if (currentFormatter == null || !currentFormatter.getClass().getName().equals(formatterName)) {
                Formatter formatter = findFormatterService(formatterName);
                if (formatter == null) {
                    log(Level.SEVERE, LogFacade.INVALID_FORMATTER_CLASS_NAME, formatterName);
                    // Fall back to the GlassFish default
                    configureDefaultFormatter(excludeFields, multiLineMode);
                } else {
                    setFormatter(formatter);
                }
            }
        }

        log(Level.INFO, LogFacade.LOG_FORMATTER_INFO, getFormatter().getClass().getName());
    }

    private void configureTimeBasedRotation() {
        final String dateChangeProperty = manager.getProperty(className + ".rotationOnDateChange");
        if (dateChangeProperty == null) {
            rotationOnDateChange = false;
        } else {
            rotationOnDateChange = Boolean.parseBoolean(dateChangeProperty);
        }
        if (rotationOnDateChange) {
            initRotationOnDateChange();
            return;
        }

        rotationTimeLimitValue = 0L;
        final String propertyValue = manager.getProperty(className + ".rotationTimelimitInMinutes");
        try {
            if (propertyValue != null) {
                rotationTimeLimitValue = Long.parseLong(propertyValue);
            }
        } catch (NumberFormatException e) {
            log(Level.WARNING, LogFacade.INVALID_ATTRIBUTE_VALUE, propertyValue, "rotationTimelimitInMinutes");
        }

        initRotationOnTimeLimit();
    }

     private void initRotationOnDateChange() {
         if (rotationTimerTask != null) {
             rotationTimerTask.cancel();
         }
         if (this.rotationOnDateChange) {
            final LogRotationTimerTask task = new DailyLogRotationTimerTask(this::rotate);
            scheduleNew(task);
         }
    }

    private void initRotationOnTimeLimit() {
        if (rotationTimerTask != null) {
            rotationTimerTask.cancel();
        }
        if (!this.rotationOnDateChange && this.rotationTimeLimitValue > 0) {
            final long delayInMillis = rotationTimeLimitValue * 60 * 1000L;
            final LogRotationTimerTask task = new PeriodicalLogRotationTimerTask(this::rotate, delayInMillis);
            scheduleNew(task);
        }
    }

    private void rotationOnFileSizeLimit(String propertyValue) {
        try {
            if (propertyValue != null) {
                rotationLimitAttrValue = Integer.parseInt(propertyValue);
            } else {
                rotationLimitAttrValue = DEFAULT_ROTATION_LIMIT_BYTES;
            }
        } catch (NumberFormatException e) {
            log(Level.WARNING, LogFacade.INVALID_ATTRIBUTE_VALUE, propertyValue, "rotationLimitInBytes");
        }
        // We set the LogRotation limit here. The rotation limit is the
        // Threshold for the number of bytes in the log file after which
        // it will be rotated.
        if (rotationLimitAttrValue >= MINIMUM_ROTATION_LIMIT_VALUE || rotationLimitAttrValue == DISABLE_LOG_FILE_ROTATION_VALUE) {
            setLimitForRotation(rotationLimitAttrValue);
        }
    }

    private File evaluateFile() {
        final String filename = evaluateFileName();
        final File logFile = new File(filename);
        if (logFile.isAbsolute()) {
            return logFile;
        }
        return new File(env.getInstanceRoot(), filename);
    }

    private String evaluateFileName() {
        final String logFileProperty = manager.getProperty(getClass().getName() + ".file");
        if (logFileProperty == null || logFileProperty.trim().isEmpty()) {
            return TranslatedConfigView.expandConfigValue(env.getInstanceRoot().getAbsolutePath() + File.separator
                + LOGS_DIR + File.separator + getDefaultFileName());
        }
        return TranslatedConfigView.expandConfigValue(logFileProperty);
    }

    Formatter findFormatterService(String formatterName) {
        List<Formatter> formatterServices = Globals.getDefaultHabitat().getAllServices(Formatter.class);
        for (Formatter formatter : formatterServices) {
            if (formatter.getClass().getName().equals(formatterName)) {
                return formatter;
            }
        }
        return null;
    }

    private void configureDefaultFormatter(String excludeFields, boolean multiLineMode) {
        configureUniformLogFormatter(excludeFields, multiLineMode);
    }

    private void configureODLFormatter(String excludeFields, boolean multiLineMode) {
        // this loop is used for ODL formatter
        ODLLogFormatter formatterClass;
        // set the formatter
        if (agent == null) {
            formatterClass = new ODLLogFormatter();
            setFormatter(formatterClass);
        } else {
            formatterClass = new ODLLogFormatter(new AgentFormatterDelegate(agent));
            setFormatter(formatterClass);
        }
        formatterClass.setExcludeFields(excludeFields);
        formatterClass.setMultiLineMode(multiLineMode);
        formatterClass.noAnsi();
        formatterClass.setLogEventBroadcaster(this);
    }

    private void configureUniformLogFormatter(String excludeFields, boolean multiLineMode) {
        String cname = getClass().getName();
        // this loop is used for UFL formatter
        UniformLogFormatter formatterClass;
        // set the formatter
        if (agent == null) {
            formatterClass = new UniformLogFormatter();
            setFormatter(formatterClass);
        } else {
            formatterClass = new UniformLogFormatter(new AgentFormatterDelegate(agent));
            setFormatter(formatterClass);
        }

        formatterClass.setExcludeFields(excludeFields);
        formatterClass.setMultiLineMode(multiLineMode);
        formatterClass.noAnsi();
        formatterClass.setLogEventBroadcaster(this);
        String recordBeginMarker = manager.getProperty(cname + ".logFormatBeginMarker");
        if (recordBeginMarker == null || recordBeginMarker.isEmpty()) {
            recordBeginMarker = RECORD_BEGIN_MARKER;
        }

        String recordEndMarker = manager.getProperty(cname + ".logFormatEndMarker");
        if (recordEndMarker == null || recordEndMarker.isEmpty()) {
            recordEndMarker = RECORD_END_MARKER;
        }

        String recordFieldSeparator = manager.getProperty(cname + ".logFormatFieldSeparator");
        if (recordFieldSeparator == null || recordFieldSeparator.isEmpty() || recordFieldSeparator.length() > 1) {
            recordFieldSeparator = RECORD_FIELD_SEPARATOR;
        }

        String recordDateFormat = manager.getProperty(cname + ".logFormatDateFormat");
        if (recordDateFormat != null && !recordDateFormat.isEmpty()) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern(recordDateFormat);
            try {
                df.format(LocalDateTime.now());
            } catch (Exception e) {
                recordDateFormat = RECORD_DATE_FORMAT;
            }
        } else {
            recordDateFormat = RECORD_DATE_FORMAT;
        }

        formatterClass.setRecordBeginMarker(recordBeginMarker);
        formatterClass.setRecordEndMarker(recordEndMarker);
        formatterClass.setRecordDateFormat(recordDateFormat);
        formatterClass.setRecordFieldSeparator(recordFieldSeparator);
    }

    private void drainAllPendingRecords() {
        if (!logToFile) {
            return;
        }
        while (true) {
            if (!publishRecord(this.logRecordBuffer.poll())) {
                return;
            }
        }
    }

    private void changeFileName(final File file) {
        // If the file name is same as the current file name, there
        // is no need to change the filename
        if (file == null || file.equals(currentLogFile)) {
            return;
        }
        synchronized (rotationLock) {
            super.flush();
            super.close();
            try {
                openFile(file);
                currentLogFile = file;
            } catch (IOException ix) {
                new ErrorManager().error(
                        "FATAL ERROR: COULD NOT OPEN LOG FILE. " +
                                "Please Check to make sure that the directory for " +
                                "Logfile exists. Currently reverting back to use the " +
                                " default server.log", ix, ErrorManager.OPEN_FAILURE);
                try {
                    // Reverting back to the old server.log
                    openFile(currentLogFile);
                } catch (Exception e) {
                    new ErrorManager().error(
                            "FATAL ERROR: COULD NOT RE-OPEN SERVER LOG FILE. ", e,
                            ErrorManager.OPEN_FAILURE);
                }
            }
        }
    }


    /**
     * A package private method to set the limit for File Rotation.
     */
    private synchronized void setLimitForRotation(int rotationLimitInBytes) {
        limitForFileRotation = rotationLimitInBytes;
    }

    private void configureJSONFormatter(String excludeFields) {
        // this loop is used for JSON formatter
        JSONLogFormatter formatterClass;
        // set the formatter
        if (agent != null) {
            formatterClass = new JSONLogFormatter(new AgentFormatterDelegate(agent));
            setFormatter(formatterClass);
        } else {
            formatterClass = new JSONLogFormatter();
            setFormatter(formatterClass);
        }
        formatterClass.setExcludeFields(excludeFields);
        formatterClass.setLogEventBroadcaster(this);
    }

    /**
     * @author David Matejcek
     *
     */
    private final class LoggingPump extends Thread {

        LoggingPump(String threadName) {
            super(threadName);
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            // we have to wait and buffer messages until logging is completely initialized.
            while (LogManager.getLogManager() == null) {
                Thread.yield();
            }
            // FIXME: Because configuration and initialization is not atomic, this thread can start too early and some messages
            //        would be formatted by default formatter.
            //        This is not a fix, rather hack than a fix of this problem. "JVM invocation command line" is still affected.
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                // nothing
            }
            log(Level.CONFIG, "Logging pump for {0} started.", logRecordBuffer);
            LOG.log(Level.CONFIG, "Processed {0} early records.", startupQueue.size());
            final ConcurrentLinkedQueue<LogRecord> toTransform = startupQueue;
            startupQueue = new ConcurrentLinkedQueue<>();
            for (final LogRecord logRecord : toTransform) {
                publishRecord(evaluateMessage(logRecord));
            }
            flush();
            while (logToFile && !shutdown.get()) {
                try {
                    publishBatchFromBuffer();
                } catch (Exception e) {
                    // Continue the loop without exiting
                    // Something is broken, but we cannot log it
                }
            }
        }
    }


    /**
     * Creates the file and initialized MeteredStream and passes it on to
     * Superclass (java.util.logging.StreamHandler).
     */
    private void openFile(File file) throws IOException {
        // check that the parent directory exists.
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException(LOCAL_STRINGS.getLocalString("parent.dir.create.failed",
                    "Failed to create the parent dir {0}", parent.getAbsolutePath()));
        }
        FileOutputStream fout = new FileOutputStream(file, true);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        meter = new MeteredStream(bout, file.length());
        setOutputStream(meter);
    }


    /**
     * cleanup the history log file based on attributes set under logging.properties file".
     * <p/>
     * If it is defined with valid number, we only keep that number of history logfiles;
     * If "max_history_files" is defined without value, then default that number to be 10;
     * If "max_history_files" is defined with value 0, any number of history files are kept.
     */
    private void cleanUpHistoryLogFiles() {
        if (maxHistoryFiles == 0) {
            return;
        }

        synchronized (rotationLock) {
            File dir = currentLogFile.getParentFile();
            String logFileName = currentLogFile.getName();
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
            if (candidates.size() <= maxHistoryFiles) {
                return;
            }
            Object[] paths = candidates.toArray();
            java.util.Arrays.sort(paths);
            try {
                for (int i = 0; i < paths.length - maxHistoryFiles; i++) {
                    File logFile = new File((String) paths[i]);
                    boolean delFile = logFile.delete();
                    if (!delFile) {
                        throw new IOException("Could not delete log file: "
                                + logFile.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                new ErrorManager().error(
                        "FATAL ERROR: COULD NOT DELETE LOG FILE.", e,
                        ErrorManager.GENERIC_FAILURE);
            }
        }
    }


    /**
     * A Simple rotate method to close the old file and start the new one
     * when the limit is reached.
     */
    public void rotate() {
        final GFFileHandler thisInstance = this;
		final PrivilegedAction<Void> action = () -> {
            synchronized (thisInstance.rotationLock) {
                // schedule next execution, it has no effect on current execution.
                scheduleNext();
                if (thisInstance.meter != null && thisInstance.meter.written <= 0) {
                    return null;
                }
                thisInstance.flush();
                thisInstance.close();
                try {
                    if (!currentLogFile.exists()) {
                        File creatingDeletedLogFile = currentLogFile.getAbsoluteFile();
                        if (creatingDeletedLogFile.createNewFile()) {
                            currentLogFile = creatingDeletedLogFile;
                        }
                        return null;
                    }
                    File oldFile = currentLogFile;
                    String renamedFileName = currentLogFile + "_"
                        + DateTimeFormatter.ofPattern(LOG_ROTATE_DATE_FORMAT).format(LocalDateTime.now());
                    File rotatedFile = new File(renamedFileName);
                    boolean renameSuccess = oldFile.renameTo(rotatedFile);
                    if (!renameSuccess) {
                        // If we don't succeed with file rename which
                        // most likely can happen on Windows because
                        // of multiple file handles opened. We go through
                        // Plan B to copy bytes explicitly to a renamed
                        // file.
                        FileUtils.copy(currentLogFile, rotatedFile);
                        File freshServerLogFile = getLogFileName();
                        // We do this to make sure that server.log
                        // contents are flushed out to start from a
                        // clean file again after the rename..
                        FileOutputStream fo = new FileOutputStream(freshServerLogFile);
                        fo.close();
                    }
                    FileOutputStream oldFileFO = new FileOutputStream(oldFile);
                    oldFileFO.close();
                    openFile(getLogFileName());
                    currentLogFile = getLogFileName();
                    if (compressionOnRotation) {
                        boolean compressed = gzipFile(rotatedFile);
                        if (compressed) {
                            boolean deleted = rotatedFile.delete();
                            if (!deleted) {
                                 throw new IOException("Could not delete uncompressed log file: "
                                        + rotatedFile.getAbsolutePath());
                            }
                        } else {
                            throw new IOException("Could not compress log file: "
                                    + rotatedFile.getAbsolutePath());
                        }
                    }

                    cleanUpHistoryLogFiles();
                } catch (IOException ix) {
                    new ErrorManager().error("Error, could not rotate log file", ix, ErrorManager.GENERIC_FAILURE);
                }
                return null;
            }
        };
        doPrivileged(action);
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


    /**
     * 5005
     * Retrieves the LogRecord from our Queue and store them in the file
     */
    private void publishBatchFromBuffer() {
        if (!publishRecord(logRecordBuffer.pollOrWait())) {
            return;
        }
        if (flushFrequency > 1) {
            // starting from 1, one record was already published
            for (int i = 1; i < flushFrequency; i++) {
                if (!publishRecord(logRecordBuffer.poll())) {
                    break;
                }
            }
        }
        flush();
    }


    @Override
    public void flush() {
        super.flush();
        if (limitForFileRotation > 0 && meter.written >= limitForFileRotation) {
            // If we have written more than the limit set for the
            // file, or rotation requested from the Timer Task or LogMBean
            // start fresh with a new file after renaming the old file.
            rotate();
        }
    }

    /**
     * Really publishes record via super.publish method call.
     *
     * @param record
     * @return true if the record was not null
     */
    private boolean publishRecord(final EnhancedLogRecord record) {
        if (record == null) {
            return false;
        }
        super.publish(record);
        return true;
    }


    /**
     * Does not publish the record, but puts it into the queue buffer to be processed by an internal
     * thread.
     */
    @Override
    public void publish(final LogRecord record) {

        // shutdown invoked, we are not processing any more records
        if (shutdown.get()) {
            return;
        }

        // logging not initialized yet, so we put the record to the special buffer
        // and for now we are done.
        // If the logging is not initialized yet, we cannot even resolve if the
        // record is loggable. Events are not supported in this state too.
        if (LogManager.getLogManager() == null) {
            this.startupQueue.add(record);
            return;
        }

        // don't process records which passed logger's level,
        // but would not pass the level of this handler.
        if (!isLoggable(record)) {
            return;
        }

        final EnhancedLogRecord recordWrapper = evaluateMessage(record);
        if (logToFile) {
            logRecordBuffer.add(recordWrapper);
        }
        final Formatter formatter = this.getFormatter();
        if (!LogEventBroadcaster.class.isInstance(formatter)) {
            final LogEvent logEvent = new LogEventImpl(recordWrapper);
            informLogEventListeners(logEvent);
        }
    }

    protected File getLogFileName() {
        return configuredLogFile;
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

    @Override
    public void informLogEventListeners(LogEvent logEvent) {
        for (LogEventListener listener : logEventListeners) {
            listener.messageLogged(logEvent);
        }
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

    private void logStandardStreams() {
        final Logger stdoutLogger = LogFacade.STDOUT_LOGGER;
        // FIXME: capacity not configurable
        stdoutOutputStream = new LoggingOutputStream(stdoutLogger, Level.INFO, 5000);
        LoggingOutputStream.LoggingPrintStream pout = stdoutOutputStream.new LoggingPrintStream(stdoutOutputStream);
        System.setOut(pout);

        final Logger errorLogger = LogFacade.STDERR_LOGGER;
        // FIXME: capacity not configurable
        stderrOutputStream = new LoggingOutputStream(errorLogger, Level.SEVERE, 1000);
        LoggingOutputStream.LoggingPrintStream perr = stderrOutputStream.new LoggingPrintStream(stderrOutputStream);
        System.setErr(perr);
    }

    public synchronized void setLogFile(String fileName) {
        String logFileName = TranslatedConfigView.expandConfigValue(fileName);
        File logFile = new File(logFileName);
         if (!logFile.isAbsolute()) {
            logFile = new File(env.getInstanceRoot(), logFileName);
         }
        changeFileName(logFile);
        configuredLogFile = logFile;
    }

    public synchronized void enableLogToFile(final int bufferCapacity, final int bufferTimeout) {
        // enable before pump initialization - thread checks this field
        this.logToFile = true;
        if (this.logToFile) {
            this.logRecordBuffer = new LogRecordBuffer(bufferCapacity, bufferTimeout);
            // Not using the PayaraExecutorService here as it prevents shutdown happening quickly
            pump = new LoggingPump("GFFileHandler log pump");
            pump.start();
        }
    }

    public synchronized void disableLogToFile() {
        // this is all, pump will take it as a signal to stop.
        this.logToFile = false;
    }

    public synchronized void setMultiLineMode(boolean multiLineMode) {
        this.multiLineMode = multiLineMode;
        // Rotate log file to avoid different log format to be displayed in same log file
        rotate();
        configureLogFormatter(fileHandlerFormatter, excludeFields, multiLineMode);
    }

    public synchronized void setFileHandlerFormatter(String fileHandlerFormatter) {
        this.fileHandlerFormatter = fileHandlerFormatter;
        // Rotate log file to avoid different log format to be displayed in same log file
        rotate();
        configureLogFormatter(fileHandlerFormatter, excludeFields, multiLineMode);
    }

    public synchronized void setExcludeFields(String excludeFields) {
        this.excludeFields = excludeFields;
// FIXME: changing three properties can cause threee immediate rotations where could be only one
        // Rotate log file to avoid different log format to be displayed in same log file
        rotate();
        configureLogFormatter(fileHandlerFormatter, excludeFields, multiLineMode);
    }

    public synchronized void setRotationLimitAttrValue(Integer rotationLimitAttrValue) {
        this.rotationLimitAttrValue = rotationLimitAttrValue;
        // We set the LogRotation limit here. The rotation limit is the
        // Threshold for the number of bytes in the log file after which
        // it will be rotated.
        if (rotationLimitAttrValue >= MINIMUM_ROTATION_LIMIT_VALUE || rotationLimitAttrValue == DISABLE_LOG_FILE_ROTATION_VALUE) {
            setLimitForRotation(rotationLimitAttrValue);
        }
    }

    public synchronized void setRotationOnDateChange(boolean rotationOnDateChange) {
        this.rotationOnDateChange = rotationOnDateChange;
        initRotationOnDateChange();
    }

    public synchronized void setRotationTimeLimitValue(Long rotationTimeLimitValue) {
        this.rotationTimeLimitValue = rotationTimeLimitValue;
        initRotationOnTimeLimit();
    }

    public synchronized void setMaxHistoryFiles(int maxHistoryFiles) {
        this.maxHistoryFiles = maxHistoryFiles;
    }

    public synchronized void setFlushFrequency(int flushFrequency) {
        this.flushFrequency = flushFrequency;
    }

    public synchronized void setCompressionOnRotation(boolean compressionOnRotation) {
        this.compressionOnRotation = compressionOnRotation;
    }

    public synchronized void setLogStandardStreams(boolean logStandardStreams) {
        this.logStandardStreams = logStandardStreams;

        if (logStandardStreams) {
            logStandardStreams();
        } else {
            System.setOut(oStdOutBackup);
            System.setErr(oStdErrBackup);
        }
    }

    public void setLogToFile(boolean logToFile) {
        if (logToFile) {
            // FIXME: read it from configuration!
            enableLogToFile(DEFAULT_BUFFER_CAPACITY, DEFAULT_BUFFER_TIMEOUT);
        } else {
            disableLogToFile();
        }
    }


    /**
     * Safe logging usable before it is configured.
     */
    private void log(final Level level, final String messageKey, final Object... messageParameters) {
        final LogRecord logRecord = createLogRecord(level, messageKey);
        logRecord.setParameters(messageParameters);
        logRecord.setResourceBundle(ResourceBundle.getBundle(LogFacade.LOGGING_RB_NAME));
        publish(logRecord);
    }

    private LogRecord createLogRecord(final Level level, final String message) {
        final LogRecord logRecord = new LogRecord(level, message);
        logRecord.setThreadID((int) Thread.currentThread().getId());
        logRecord.setLoggerName(LogFacade.LOGGING_LOGGER_NAME);
        return logRecord;
    }

    private EnhancedLogRecord evaluateMessage(final LogRecord record) {
        if (record instanceof EnhancedLogRecord) {
            return (EnhancedLogRecord) record;
        }
        final EnhancedLogRecord enhancedLogRecord = new EnhancedLogRecord(record);
        final ResolvedLogMessage message = resolveMessage(record);
        enhancedLogRecord.setMessageKey(message.key);
        enhancedLogRecord.setMessage(message.message);
        // values were used and they are not required any more.
        enhancedLogRecord.setResourceBundle(null);
        enhancedLogRecord.setParameters(null);
        return enhancedLogRecord;
    }

    /**
     * This is a mechanism extracted from the StreamHandler.
     * If the message is loggable should be decided before creation of this instance to avoid
     * resolving a message which would not be used. And it is in done - in
     * {@link Logger#log(LogRecord)} and in {@link #publish(LogRecord)}
     */
    private ResolvedLogMessage resolveMessage(final LogRecord record) {
        final ResourceBundle bundle = getResourceBundle(record.getResourceBundle(), record.getLoggerName());
        final ResolvedLogMessage localizedTemplate = tryToLocalizeTemplate(record.getMessage(), bundle);
        final Object[] parameters = record.getParameters();
        if (parameters == null || parameters.length == 0) {
            return localizedTemplate;
        }
        final String localizedMessage = toMessage(localizedTemplate.message, parameters);
        return new ResolvedLogMessage(localizedTemplate.key, localizedMessage);
    }

    private String toMessage(final String template, final Object[] parameters) {
        try {
            return MessageFormat.format(template, parameters);
        } catch (final Exception e) {
            return template;
        }
    }


    private ResolvedLogMessage tryToLocalizeTemplate(final String originalMessage, final ResourceBundle bundle) {
        if (bundle == null) {
            return new ResolvedLogMessage(null, originalMessage);
        }
        try {
            final String localizedMessage = bundle.getString(originalMessage);
            return new ResolvedLogMessage(originalMessage, localizedMessage);
        } catch (final MissingResourceException e) {
            return new ResolvedLogMessage(null, originalMessage);
        }
    }

    private ResourceBundle getResourceBundle(final ResourceBundle bundle, final String loggerName) {
        if (bundle != null) {
            return bundle;
        }
        final Logger logger = this.manager.getLogger(loggerName);
        return logger == null ? null : logger.getResourceBundle();
    }


    private static class ResolvedLogMessage {

        final String key;
        final String message;

        ResolvedLogMessage(final String key, final String message) {
            this.key = key;
            this.message = message;
        }
    }
}
