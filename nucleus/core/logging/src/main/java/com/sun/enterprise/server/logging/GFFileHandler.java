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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.api.logging.Task;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import com.sun.appserv.server.util.Version;
import com.sun.common.util.logging.BooleanLatch;
import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.module.bootstrap.EarlyLogHandler;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.logging.AgentFormatterDelegate;

/**
 * GFFileHandler publishes formatted log Messages to a FILE.
 *
 * @AUTHOR: Jerome Dochez
 * @AUTHOR: Carla Mott
 */
@Service
@Singleton
@ContractsProvided({GFFileHandler.class, java.util.logging.Handler.class, 
    LogEventBroadcaster.class, LoggingRuntime.class})
public class GFFileHandler extends StreamHandler implements 
PostConstruct, PreDestroy, LogEventBroadcaster, LoggingRuntime {

    private static final int DEFAULT_ROTATION_LIMIT_BYTES = 2000000;

    private final static LocalStringManagerImpl LOCAL_STRINGS = 
        new LocalStringManagerImpl(GFFileHandler.class);
    
    @Inject
    ServerContext serverContext;

    @Inject
    ServerEnvironmentImpl env;

    @Inject @Optional
    Agent agent;
    
    @Inject
    private ServiceLocator habitat;
    
    // This is a OutputStream to keep track of number of bytes
    // written out to the stream
    private MeteredStream meter;

    private static final String LOGS_DIR = "logs";
    private static final String LOG_FILE_NAME = "server.log";
    
    private String absoluteServerLogName = null;

    private File absoluteFile = null;

    private int flushFrequency = 1;

    private int maxHistoryFiles = 10;


    private String gffileHandlerFormatter = "";
    private String currentgffileHandlerFormatter = "";

    // Initially the LogRotation will be off until the domain.xml value is read.
    private int limitForFileRotation = 0;

    private BlockingQueue<LogRecord> pendingRecords = new ArrayBlockingQueue<LogRecord>(5000);

    // Rotation can be done in 3 ways
    // 1. Based on the Size: Rotate when some Threshold number of bytes are 
    //    written to server.log
    // 2. Based on the Time: Rotate ever 'n' minutes, mostly 24 hrs
    // 3. Rotate now
    // For mechanisms 2 and 3 we will use this flag. The rotate() will always
    // be fired from the publish( ) method for consistency
    private AtomicBoolean rotationRequested = new AtomicBoolean(false);
    
    private Object rotationLock = new Object();

    private static final String LOG_ROTATE_DATE_FORMAT =
            "yyyy-MM-dd'T'HH-mm-ss";

    private static final SimpleDateFormat logRotateDateFormatter =
            new SimpleDateFormat(LOG_ROTATE_DATE_FORMAT);

    private static final String DEFAULT_LOG_FILE_FORMATTER_CLASS_NAME = UniformLogFormatter.class.getName();

    public static final int MINIMUM_ROTATION_LIMIT_VALUE = 500*1000;

    private BooleanLatch done = new BooleanLatch();
    private Thread pump;

    boolean dayBasedFileRotation = false;

    private String RECORD_BEGIN_MARKER = "[#|";
    private String RECORD_END_MARKER = "|#]";
    private String RECORD_FIELD_SEPARATOR = "|";
    private String RECORD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    
    private List<LogEventListener> logEventListeners = new ArrayList<LogEventListener>();

    String recordBeginMarker;
    String recordEndMarker;
    String recordFieldSeparator;
    String recordDateFormat;

    String logFileProperty = "";

    public void postConstruct() {

        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();

        logFileProperty = manager.getProperty(cname + ".file");
        if(logFileProperty==null || logFileProperty.trim().equals("")) {
            logFileProperty = env.getInstanceRoot().getAbsolutePath() + File.separator + LOGS_DIR + File.separator +
                    LOG_FILE_NAME;
        }

        String filename = TranslatedConfigView.getTranslatedValue(logFileProperty).toString();

        File serverLog = new File(filename);
        absoluteServerLogName = filename;
        if (!serverLog.isAbsolute()) {
            serverLog = new File(env.getDomainRoot(), filename);
            absoluteServerLogName = env.getDomainRoot() + File.separator + filename;
        }
        changeFileName(serverLog);

        // Reading just few lines of log file to get the log fomatter used.
        BufferedReader br = null;
        String strLine = "";
        int odlFormatter = 0;
        int uniformLogFormatter = 0;
        int otherFormatter = 0;
        boolean mustRotate = false;
        
        try {
            br = new BufferedReader(new FileReader(serverLog));            
            while ((strLine = br.readLine()) != null) {
                strLine = strLine.trim();
                if (!strLine.equals("")) {
                    if (LogFormatHelper.isUniformFormatLogHeader(strLine)) {  // for ufl formatter
                        uniformLogFormatter++;
                    } else if (LogFormatHelper.isODLFormatLogHeader(strLine)) { 
                        // for ODL formatter
                        odlFormatter++;
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
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch(Exception e) {}
            }
        }

        if (odlFormatter > 0) {
            currentgffileHandlerFormatter = "com.sun.enterprise.server.logging.ODLLogFormatter";
        } else if (uniformLogFormatter > 0) {
            currentgffileHandlerFormatter = "com.sun.enterprise.server.logging.UniformLogFormatter";
        }

        // start the Queue consumer thread.
        initializePump();
        
        LogRecord lr = new LogRecord(Level.INFO, LogFacade.GF_VERSION_INFO);
        lr.setParameters(new Object[]{ Version.getFullVersion()});        
        lr.setResourceBundle(ResourceBundle.getBundle(LogFacade.LOGGING_RB_NAME));
        lr.setThreadID((int) Thread.currentThread().getId());
        lr.setLoggerName(LogFacade.LOGGING_LOGGER_NAME);
        EarlyLogHandler.earlyMessages.add(lr);

        String propValue = null;
        propValue = manager.getProperty(cname + ".rotationOnDateChange");
        boolean rotationOnDateChange = false;
        if (propValue != null) {
            rotationOnDateChange = Boolean.parseBoolean(propValue);
        }
        if (rotationOnDateChange) {

            dayBasedFileRotation = true;

            Long rotationTimeLimitValue = 0L;

            int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

            long systime = System.currentTimeMillis();
            String nextDate = dateFormat.format(date.getTime() + MILLIS_IN_DAY);
            Date nextDay = null;
            try {
                nextDay = dateFormat.parse(nextDate);
            } catch (ParseException e) {
                nextDay = new Date();
                lr = new LogRecord(Level.WARNING, LogFacade.DATE_PARSING_FAILED);
                lr.setParameters(new Object[]{nextDate});
                lr.setResourceBundle(ResourceBundle.getBundle(LogFacade.LOGGING_RB_NAME));
                lr.setThreadID((int) Thread.currentThread().getId());
                lr.setLoggerName(LogFacade.LOGGING_LOGGER_NAME);
                EarlyLogHandler.earlyMessages.add(lr);
            }
            long nextsystime = nextDay.getTime();

            rotationTimeLimitValue = nextsystime - systime;

            Task rotationTask = new Task() {
                public Object run() {
                    rotate();
                    return null;
                }
            };

            LogRotationTimer.getInstance().startTimer(
                    new LogRotationTimerTask(rotationTask,
                            rotationTimeLimitValue / 60000));

        } else {

            Long rotationTimeLimitValue = 0L;
            try {
                propValue = manager.getProperty(cname + ".rotationTimelimitInMinutes");
                if (propValue != null) {
                    rotationTimeLimitValue = Long.parseLong(propValue);                    
                }
            } catch (NumberFormatException e) {
                lr = new LogRecord(Level.WARNING, LogFacade.INVALID_ATTRIBUTE_VALUE);
                lr.setParameters(new Object[]{propValue, "rotationTimelimitInMinutes"});
                lr.setResourceBundle(ResourceBundle.getBundle(LogFacade.LOGGING_RB_NAME));
                lr.setThreadID((int) Thread.currentThread().getId());
                lr.setLoggerName(LogFacade.LOGGING_LOGGER_NAME);
                EarlyLogHandler.earlyMessages.add(lr);
            }

            if (rotationTimeLimitValue > 0) {

                Task rotationTask = new Task() {
                    public Object run() {
                        rotate();
                        return null;
                    }
                };

                LogRotationTimer.getInstance().startTimer(
                        new LogRotationTimerTask(rotationTask,
                                rotationTimeLimitValue));
            } 
        }

        // Also honor the size based rotation if configured.
        Integer rotationLimitAttrValue = DEFAULT_ROTATION_LIMIT_BYTES;
        try {
            propValue = manager.getProperty(cname + ".rotationLimitInBytes");
            if (propValue != null) {
                rotationLimitAttrValue = Integer.parseInt(propValue);
            }
        } catch (NumberFormatException e) {
            lr = new LogRecord(Level.WARNING, LogFacade.INVALID_ATTRIBUTE_VALUE);
            lr.setParameters(new Object[]{propValue, "rotationLimitInBytes"});
            lr.setResourceBundle(ResourceBundle.getBundle(LogFacade.LOGGING_RB_NAME));
            lr.setThreadID((int) Thread.currentThread().getId());
            lr.setLoggerName(LogFacade.LOGGING_LOGGER_NAME);
            EarlyLogHandler.earlyMessages.add(lr);
        }
        // We set the LogRotation limit here. The rotation limit is the
        // Threshold for the number of bytes in the log file after which
        // it will be rotated.
        if (rotationLimitAttrValue >= MINIMUM_ROTATION_LIMIT_VALUE ) {
            setLimitForRotation(rotationLimitAttrValue);        
        }

        //setLevel(Level.ALL);
        propValue = manager.getProperty(cname + ".flushFrequency");
        if (propValue != null) {
            try {
                flushFrequency = Integer.parseInt(propValue);
            } catch (NumberFormatException e) {
                lr = new LogRecord(Level.WARNING, LogFacade.INVALID_ATTRIBUTE_VALUE);
                lr.setParameters(new Object[]{propValue, "flushFrequency"});
                lr.setResourceBundle(ResourceBundle.getBundle(LogFacade.LOGGING_RB_NAME));
                lr.setThreadID((int) Thread.currentThread().getId());
                lr.setLoggerName(LogFacade.LOGGING_LOGGER_NAME);
                EarlyLogHandler.earlyMessages.add(lr);
            }
        }
        if (flushFrequency <= 0)
            flushFrequency = 1;

        String formatterName = manager.getProperty(cname + ".formatter");
        formatterName = (formatterName == null) ? DEFAULT_LOG_FILE_FORMATTER_CLASS_NAME : formatterName; 
        
        // Below snapshot of the code is used to rotate server.log file on startup. It is used to avoid different format
        // log messages logged under same server.log file.
        gffileHandlerFormatter = formatterName;
        if (mustRotate) {
            rotate();
        } else if (gffileHandlerFormatter != null 
                && !gffileHandlerFormatter
                        .equals(currentgffileHandlerFormatter)) {
            rotate();
        }
        
        String excludeFields = manager.getProperty(LogManagerService.EXCLUDE_FIELDS_PROPERTY);
        boolean multiLineMode = Boolean.parseBoolean(manager.getProperty(LogManagerService.MULTI_LINE_MODE_PROPERTY));
                
        if (UniformLogFormatter.class.getName().equals(formatterName)) {
            configureUniformLogFormatter(excludeFields, multiLineMode);
        } else if (ODLLogFormatter.class.getName().equals(formatterName)) {
            configureODLFormatter(excludeFields, multiLineMode);
        } else {
            // Custom formatter is configured in logging.properties
            // Check if the user specified formatter is in play else
            // log an error message
            Formatter currentFormatter = this.getFormatter();
            if (currentFormatter == null || !currentFormatter.getClass().getName().equals(formatterName)) {
                Formatter formatter = findFormatterService(formatterName);
                if (formatter == null) {
                    lr = new LogRecord(Level.SEVERE, LogFacade.INVALID_FORMATTER_CLASS_NAME);
                    lr.setParameters(new Object[]{formatterName});
                    lr.setThreadID((int) Thread.currentThread().getId());
                    lr.setResourceBundle(ResourceBundle.getBundle(LogFacade.LOGGING_RB_NAME));
                    lr.setLoggerName(LogFacade.LOGGING_LOGGER_NAME);
                    EarlyLogHandler.earlyMessages.add(lr);
                    // Fall back to the GlassFish default
                    configureDefaultFormatter(excludeFields, multiLineMode);                                    
                } else {
                    setFormatter(formatter);
                }
            }
        }
        
        formatterName = this.getFormatter().getClass().getName();
        lr = new LogRecord(Level.INFO, LogFacade.LOG_FORMATTER_INFO);        
        lr .setParameters(new Object[] {formatterName});
        lr.setResourceBundle(ResourceBundle.getBundle(LogFacade.LOGGING_RB_NAME));
        lr.setThreadID((int) Thread.currentThread().getId());
        lr.setLoggerName(LogFacade.LOGGING_LOGGER_NAME);
        EarlyLogHandler.earlyMessages.add(lr);
        
        propValue = manager.getProperty(cname + ".maxHistoryFiles");
        try {
            if (propValue != null) {
                maxHistoryFiles = Integer.parseInt(propValue);
            }
        } catch (NumberFormatException e) {
            lr = new LogRecord(Level.WARNING, LogFacade.INVALID_ATTRIBUTE_VALUE);
            lr.setParameters(new Object[]{propValue, "maxHistoryFiles"});
            lr.setResourceBundle(ResourceBundle.getBundle(LogFacade.LOGGING_RB_NAME));
            lr.setThreadID((int) Thread.currentThread().getId());
            lr.setLoggerName(LogFacade.LOGGING_LOGGER_NAME);
            EarlyLogHandler.earlyMessages.add(lr);
        }
        
        if (maxHistoryFiles < 0)
            maxHistoryFiles = 10;

    }

    Formatter findFormatterService(String formatterName) {
        List<Formatter> formatterServices = habitat.getAllServices(Formatter.class);
        for (Formatter formatter : formatterServices) {
            if (formatter.getClass().getName().equals(formatterName)) {
                return formatter;
            }
        }
        return null;
    }

    private void configureDefaultFormatter(String excludeFields,
            boolean multiLineMode) {
        configureUniformLogFormatter(excludeFields, multiLineMode);
    }

    private void configureODLFormatter(String excludeFields, boolean multiLineMode) {
        // this loop is used for ODL formatter
        ODLLogFormatter formatterClass = null;
        // set the formatter
        if (agent != null) {
            formatterClass = new ODLLogFormatter(new AgentFormatterDelegate(agent));
            setFormatter(formatterClass);
        } else {
            formatterClass = new ODLLogFormatter();
            setFormatter(formatterClass);
        }
        formatterClass.setExcludeFields(excludeFields);
        formatterClass.setMultiLineMode(multiLineMode);
        formatterClass.setLogEventBroadcaster(this);        
    }

    private void configureUniformLogFormatter(String excludeFields, boolean multiLineMode) {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        // this loop is used for UFL formatter
        UniformLogFormatter formatterClass = null;
        // set the formatter
        if (agent != null) {
            formatterClass = new UniformLogFormatter(new AgentFormatterDelegate(agent));
            setFormatter(formatterClass);
        } else {
            formatterClass = new UniformLogFormatter();
            setFormatter(formatterClass);
        }

        formatterClass.setExcludeFields(excludeFields);
        formatterClass.setMultiLineMode(multiLineMode);
        formatterClass.setLogEventBroadcaster(this);

        if (formatterClass != null) {
            recordBeginMarker = manager.getProperty(cname + ".logFormatBeginMarker");
            if (recordBeginMarker == null || ("").equals(recordBeginMarker)) {
                recordBeginMarker = RECORD_BEGIN_MARKER;
            }

            recordEndMarker = manager.getProperty(cname + ".logFormatEndMarker");
            if (recordEndMarker == null || ("").equals(recordEndMarker)) {
                recordEndMarker = RECORD_END_MARKER;
            }

            recordFieldSeparator = manager.getProperty(cname + ".logFormatFieldSeparator");
            if (recordFieldSeparator == null || ("").equals(recordFieldSeparator) || recordFieldSeparator.length() > 1) {
                recordFieldSeparator = RECORD_FIELD_SEPARATOR;
            }

            recordDateFormat = manager.getProperty(cname + ".logFormatDateFormat");
            if (recordDateFormat != null && !("").equals(recordDateFormat)) {
                SimpleDateFormat sdf = new SimpleDateFormat(recordDateFormat);
                try {
                    sdf.format(new Date());
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
    }
    
    void initializePump() {
        pump = new Thread() {
            public void run() {
                while (!done.isSignalled()) {
                    try {
                        log();
                    } catch (Exception e) {
                        // GLASSFISH-19125
                        // Continue the loop without exiting
                    }
                }
            }
        };
        pump.setDaemon(true);
        pump.start();        
    }

    public void preDestroy() {
        // stop the Queue consummer thread.
        if (LogFacade.LOGGING_LOGGER.isLoggable(Level.FINE)) {
            LogFacade.LOGGING_LOGGER.fine("Logger handler killed");            
        }
        done.tryReleaseShared(1);
        pump.interrupt();

        // drain and return
        final int size = pendingRecords.size();
        if (size > 0) {
            Collection<LogRecord> records = new ArrayList<LogRecord>(size);
            pendingRecords.drainTo(records, size);
            for (LogRecord record : records) {
                super.publish(record);
            }
        }

    }

    /**
     * This method is invoked from LogManager.reInitializeLoggers() to
     * change the location of the file.
     */
    void changeFileName(File file) {
        // If the file name is same as the current file name, there
        // is no need to change the filename
        if (file.equals(absoluteFile)) {
            return;
        }
        synchronized (rotationLock) {
            super.flush();
            super.close();
            try {
                openFile(file);
                absoluteFile = file;
            } catch (IOException ix) {
                new ErrorManager().error(
                        "FATAL ERROR: COULD NOT OPEN LOG FILE. " +
                                "Please Check to make sure that the directory for " +
                                "Logfile exists. Currently reverting back to use the " +
                                " default server.log", ix, ErrorManager.OPEN_FAILURE);
                try {
                    // Reverting back to the old server.log
                    openFile(absoluteFile);
                } catch (Exception e) {
                    new ErrorManager().error(
                            "FATAL ERROR: COULD NOT RE-OPEN SERVER LOG FILE. ", e,
                            ErrorManager.OPEN_FAILURE);
                }
            }
        }
    }


    /**
     * A simple getter to access the current log file written by
     * this FileHandler.
     */
    public File getCurrentLogFile() {
        return absoluteFile;
    }
    
    /**
     * A package private method to set the limit for File Rotation.
     */
    private synchronized void setLimitForRotation(int rotationLimitInBytes) {
        limitForFileRotation = rotationLimitInBytes;
    }

    // NOTE: This private class is copied from java.util.logging.FileHandler
    // A metered stream is a subclass of OutputStream that
    //   (a) forwards all its output to a target stream
    //   (b) keeps track of how many bytes have been written

    private static final class MeteredStream extends OutputStream {

        private volatile boolean isOpen = false;

        OutputStream out;
        long written;

        MeteredStream(OutputStream out, long written) {
            this.out = out;
            this.written = written;
            isOpen = true;
        }

        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        public void write(byte buff[]) throws IOException {
            out.write(buff);
            written += buff.length;
        }

        public void write(byte buff[], int off, int len) throws IOException {
            out.write(buff, off, len);
            written += len;
        }

        public void flush() throws IOException {
            out.flush();
        }

        public void close() throws IOException {
            if (isOpen) {
                isOpen = false;
                flush();
                out.close();
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
     * Request Rotation called from Rotation Timer Task or LogMBean
     */
    void requestRotation() {
        rotationRequested.set(true);
    }

    /**
     * cleanup the history log file based on attributes set under logging.properties file".
     * <p/>
     * If it is defined with valid number, we only keep that number of history logfiles;
     * If "max_history_files" is defined without value, then default that number to be 10;
     * If "max_history_files" is defined with value 0, any number of history files are kept.
     */
    public void cleanUpHistoryLogFiles() {
        if (maxHistoryFiles == 0)
            return;
        
        synchronized (rotationLock) {
            File dir = absoluteFile.getParentFile();
            if (dir == null)
                return;
            File[] fset = dir.listFiles();
            ArrayList candidates = new ArrayList();
            for (int i = 0; fset != null && i < fset.length; i++) {
                if (!LOG_FILE_NAME.equals(fset[i].getName()) && fset[i].isFile()
                        && fset[i].getName().startsWith(LOG_FILE_NAME)) {
                    candidates.add(fset[i].getAbsolutePath());
                }
            }
            if (candidates.size() <= maxHistoryFiles)
                return;
            Object[] pathes = candidates.toArray();
            java.util.Arrays.sort(pathes);
            try {
                for (int i = 0; i < pathes.length - maxHistoryFiles; i++) {
                    File logFile = new File((String) pathes[i]);
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
        java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
                    public Object run() {
                        synchronized (thisInstance.rotationLock) {
                            if (thisInstance.meter != null
                                    && thisInstance.meter.written <= 0) {
                                return null;
                            }
                            thisInstance.flush();
                            thisInstance.close();
                            try {
                                if (!absoluteFile.exists()) {
                                    File creatingDeletedLogFile = new File(
                                            absoluteFile.getAbsolutePath());
                                    if (creatingDeletedLogFile.createNewFile()) { 
                                        absoluteFile = creatingDeletedLogFile;
                                    } 
                                } else {
                                    File oldFile = absoluteFile;
                                    StringBuffer renamedFileName = new StringBuffer(
                                            absoluteFile + "_");
                                    logRotateDateFormatter.format(new Date(),
                                            renamedFileName, new FieldPosition(0));
                                    File rotatedFile = new File(renamedFileName
                                            .toString());
                                    boolean renameSuccess = oldFile
                                            .renameTo(rotatedFile);
                                    if (!renameSuccess) {
                                        // If we don't succeed with file rename which
                                        // most likely can happen on Windows because
                                        // of multiple file handles opened. We go through
                                        // Plan B to copy bytes explicitly to a renamed
                                        // file.
                                        FileUtils.copy(absoluteFile, rotatedFile);
                                        File freshServerLogFile = getLogFileName();
                                        // We do this to make sure that server.log
                                        // contents are flushed out to start from a
                                        // clean file again after the rename..
                                        FileOutputStream fo = new FileOutputStream(
                                                freshServerLogFile);
                                        fo.close();
                                    }
                                    FileOutputStream oldFileFO = new FileOutputStream(
                                            oldFile);
                                    oldFileFO.close();
                                    openFile(getLogFileName());
                                    absoluteFile = getLogFileName();
                                    // This will ensure that the log rotation timer
                                    // will be restarted if there is a value set
                                    // for time based log rotation
                                    if (dayBasedFileRotation) {
                                        LogRotationTimer.getInstance()
                                                .restartTimerForDayBasedRotation();
                                    } else {
                                        LogRotationTimer.getInstance()
                                                .restartTimer();
                                    }

                                    cleanUpHistoryLogFiles();                                    
                                }
                            } catch (IOException ix) {
                                new ErrorManager().error("Error, could not rotate log file", ix, ErrorManager.GENERIC_FAILURE);
                            }
                            return null;
                        }
                    }
                }
        );
    }


    /**
     * 5005
     * Retrieves the LogRecord from our Queue and store them in the file
     */
    public void log() {

        LogRecord record;
        
        // take is blocking so we take one record off the queue
        try {
            record = pendingRecords.take();
            super.publish(record);
        } catch (InterruptedException e) {
            return;
        }

        // now try to read more.  we end up blocking on the above take call if nothing is in the queue
        Vector<LogRecord> v = new Vector<LogRecord>();
        int msgs = pendingRecords.drainTo(v, flushFrequency);
        for (int j = 0; j < msgs; j++) {
            super.publish(v.get(j));
        }

        flush();
        if ((rotationRequested.get())
                || ((limitForFileRotation > 0)
                && (meter.written >= limitForFileRotation))) {
            // If we have written more than the limit set for the
            // file, or rotation requested from the Timer Task or LogMBean
            // start fresh with a new file after renaming the old file.
            synchronized (rotationLock) {
                rotate();
                rotationRequested.set(false);
            }
        }

    }

    /**
     * Publishes the logrecord storing it in our queue
     */
    public void publish(LogRecord record) {

        // the queue has shutdown, we are not processing any more records
        if (done.isSignalled()) {
            return;
        }
        
        // JUL LogRecord does not capture thread-name. Create a wrapper to
        // capture the name of the logging thread so that a formatter can
        // output correct thread-name if done asynchronously. Note that 
        // this fix is limited to records published through this handler only.
        GFLogRecord recordWrapper = new GFLogRecord(record);
        recordWrapper.setThreadName(Thread.currentThread().getName());

        try {
            // set the thread id to be the current thread that is logging the message
//            record.setThreadID((int)Thread.currentThread().getId());
            pendingRecords.add(recordWrapper);
        } catch (IllegalStateException e) {
            // queue is full, start waiting.
            try {
                pendingRecords.put(recordWrapper);
            } catch (InterruptedException e1) {
                // too bad, record is lost...
            }
        }
        
        Formatter formatter = this.getFormatter();
        if (!(formatter instanceof LogEventBroadcaster)) {
            LogEvent logEvent = new LogEventImpl(record);
            informLogEventListeners(logEvent);
        }
        
    }

    protected File getLogFileName() {
//        return new File(new File(env.getDomainRoot(),LOGS_DIR), logFileName);
        return new File(absoluteServerLogName);

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
    
    
    public void informLogEventListeners(LogEvent logEvent) {
        for (LogEventListener listener : logEventListeners) {
            listener.messageLogged(logEvent);
        }
    }

}

