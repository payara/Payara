/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.server.util.Version;
import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.module.bootstrap.EarlyLogHandler;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.logging.AgentFormatterDelegate;
import com.sun.logging.LogDomains;
import org.glassfish.api.logging.Task;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.ContractProvided;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.PreDestroy;
import org.jvnet.hk2.component.Singleton;

import java.io.*;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

/**
 * GFFileHandler publishes formatted log Messages to a FILE.
 *
 * @AUTHOR: Jerome Dochez
 * @AUTHOR: Carla Mott
 */
@Service
@Scoped(Singleton.class)
@ContractProvided(java.util.logging.Handler.class)
public class GFFileHandler extends StreamHandler implements PostConstruct, PreDestroy {


    @Inject
    ServerContext serverContext;

    @Inject
    ServerEnvironmentImpl env;

    @Inject @Optional
    Agent agent;

    @Inject
    Version version;

    // This is a OutputStream to keep track of number of bytes
    // written out to the stream
    private MeteredStream meter;

    private static final String LOGS_DIR = "logs";
    private String logFileName = "server.log";
    private String absoluteServerLogName = null;

    private File absoluteFile = null;

    private int flushFrequency = 1;

    private int maxHistoryFiles = 10;


    private String gffileHandlerFormatter = "";
    private String currentgffileHandlerFormatter = "";

    // For now the mimimum rotation value is 0.5 MB.
    private static final int MINIMUM_FILE_ROTATION_VALUE = 500000;

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

    private static final String LOG_ROTATE_DATE_FORMAT =
            "yyyy-MM-dd'T'HH-mm-ss";

    private static final SimpleDateFormat logRotateDateFormatter =
            new SimpleDateFormat(LOG_ROTATE_DATE_FORMAT);

    private BooleanLatch done = new BooleanLatch();
    private Thread pump;

    // We maintain a list of the last MAX_RECENT_ERRORS WARNING
    // or SEVERE error messages that were logged. The DAS (or any other 
    // client) can then obtain these messages through the ServerRuntimeMBean
    // and determine if the server instance (or Node Agent) is in an 
    // error state.
    private static final int MAX_RECENT_ERRORS = 4;

    boolean dayBasedFileRotation = false;

    private String RECORD_BEGIN_MARKER = "[#|";
    private String RECORD_END_MARKER = "|#]";
    private String RECORD_FIELD_SEPARATOR = "|";
    private String RECORD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    String recordBeginMarker;
    String recordEndMarker;
    String recordFieldSeparator;
    String recordDateFormat;

    String logFileProperty = "";

    public void postConstruct() {

        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();

        if(manager!=null) {
            logFileProperty = manager.getProperty(cname + ".file");
        }

        if(logFileProperty==null || logFileProperty.trim().equals("")) {
            logFileProperty = env.getInstanceRoot().getAbsolutePath() + File.separator + LOGS_DIR + File.separator +
                    logFileName;
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
        FileInputStream fs = null;
        String strLine = "";
        int odlFormatter = 0;
        int uflFormatter = 0;
        int otherFormatter = 0;
        boolean mustRotate = false;

        try {
            fs = new FileInputStream(serverLog);
            BufferedReader br = new BufferedReader(new InputStreamReader(fs));
            while ((strLine = br.readLine()) != null) {
                strLine = strLine.trim();
                if (!strLine.equals("")) {
                    if (strLine.startsWith("[[") && strLine.endsWith("]") && countOccurrences(strLine, '[') > 4) { // for odl formatter
                        odlFormatter++;
                    } else if (strLine.startsWith("[#|") && strLine.endsWith("|#]") && countOccurrences(strLine, '|') > 4) {  // for ufl formatter
                        uflFormatter++;
                    } else {
                        otherFormatter++;  // for other formatter
                    }

                    // multiple formatter found under log file then must rotate the log file
                    if (odlFormatter > 0 && uflFormatter > 0) {
                        mustRotate = true;
                        break;
                    } else if (uflFormatter > 0 && otherFormatter > 0) {
                        mustRotate = true;
                        break;
                    } else if (otherFormatter > 0 && odlFormatter > 0) {
                        mustRotate = true;
                        break;
                    }

                    // reading first few lines and breaking loop
                    if (odlFormatter > 2 || uflFormatter > 2 || odlFormatter > 2) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        if (odlFormatter > 0) {
            currentgffileHandlerFormatter = "com.sun.enterprise.server.logging.ODLLogFormatter";
        } else if (uflFormatter > 0) {
            currentgffileHandlerFormatter = "com.sun.enterprise.server.logging.UniformLogFormatter";
        }

        // start the Queue consummer thread.

        pump = new Thread() {
            public void run() {
                try {
                    while (!done.isSignalled()) {
                        log();
                    }
                } catch (RuntimeException e) {

                }
            }
        };
        pump.setDaemon(true);
        pump.start();
        LogRecord lr = new LogRecord(Level.INFO, "Running GlassFish Version: " + version.getFullVersion());
        lr.setThreadID((int) Thread.currentThread().getId());
        lr.setLoggerName(getClass().getName());
        EarlyLogHandler.earlyMessages.add(lr);

        lr = new LogRecord(Level.INFO, "GlassFis is using Log Formatter: " + manager.getProperty(cname + ".formatter"));
        lr.setThreadID((int) Thread.currentThread().getId());
        lr.setLoggerName(getClass().getName());
        EarlyLogHandler.earlyMessages.add(lr);


        String rotationOnDateChange = manager.getProperty(cname + ".rotationOnDateChange");
        if (rotationOnDateChange != null && !("").equals(rotationOnDateChange.trim()) && Boolean.parseBoolean(rotationOnDateChange)) {

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
                lr = new LogRecord(Level.WARNING,
                        "Cannot parse the date.");
                lr.setThreadID((int) Thread.currentThread().getId());
                lr.setLoggerName(getClass().getName());
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
            // Disable the Size Based Rotation if the Time Based
            // Rotation is set.
            setLimitForRotation(0);

        } else {

            Long rotationTimeLimitValue = 0L;
            try {
                rotationTimeLimitValue = Long.parseLong(manager.getProperty(cname + ".rotationTimelimitInMinutes"));
            } catch (NumberFormatException e) {
                lr = new LogRecord(Level.SEVERE,
                        "Can't find rotationTimelimitInMinutes property from logging config file");
                lr.setThreadID((int) Thread.currentThread().getId());
                lr.setLoggerName(getClass().getName());
                EarlyLogHandler.earlyMessages.add(lr);
            }

            if (rotationTimeLimitValue != 0) {

                Task rotationTask = new Task() {
                    public Object run() {
                        rotate();
                        return null;
                    }
                };

                // If there is a value specified for the rotation based on
                // time we set that first, if not then we will fall back to
                // size based rotation

                LogRotationTimer.getInstance().startTimer(
                        new LogRotationTimerTask(rotationTask,
                                rotationTimeLimitValue));
                // Disable the Size Based Rotation if the Time Based
                // Rotation is set.
                setLimitForRotation(0);
            } else {
                Integer rotationLimitAttrValue = 0;

                try {
                    rotationLimitAttrValue = Integer.parseInt(manager.getProperty(cname + ".rotationLimitInBytes"));
                } catch (NumberFormatException e) {
                    lr = new LogRecord(Level.WARNING,
                            "Can't find rotationLimitInBytes property from logging config file so using default.");
                    lr.setThreadID((int) Thread.currentThread().getId());
                    lr.setLoggerName(getClass().getName());
                    EarlyLogHandler.earlyMessages.add(lr);
                }
                // We set the LogRotation limit here. The rotation limit is the
                // Threshold for the number of bytes in the log file after which
                // it will be rotated.
                setLimitForRotation(rotationLimitAttrValue);
            }
        }

        // Below snapshot of the code is used to rotate server.log file on startup. It is used to avoid different format
        // log messages logged under same server.log file.
        gffileHandlerFormatter = manager.getProperty(cname + ".formatter");
        if (mustRotate) {
            rotate();
        } else if (!currentgffileHandlerFormatter.equals("") && gffileHandlerFormatter != null && !gffileHandlerFormatter.equals(currentgffileHandlerFormatter)) {
            rotate();
        }


        //setLevel(Level.ALL);
        String ff = manager.getProperty(cname + ".flushFrequency");
        if (ff != null)
            try {
                flushFrequency = Integer.parseInt(manager.getProperty(cname + ".flushFrequency"));
            } catch (NumberFormatException e) {
                lr = new LogRecord(Level.WARNING,
                        "Can't find flushFrequency property from logging config file so using default.");
                lr.setThreadID((int) Thread.currentThread().getId());
                lr.setLoggerName(getClass().getName());
                EarlyLogHandler.earlyMessages.add(lr);

            }
        if (flushFrequency <= 0)
            flushFrequency = 1;

        String formatterName = manager.getProperty(cname + ".formatter");

        if (formatterName == null || UniformLogFormatter.class.getName().equals(formatterName)) {
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

            if (formatterClass != null) {
                recordBeginMarker = manager.getProperty(cname + ".logFormatBeginMarker");
                if (recordBeginMarker == null || ("").equals(recordBeginMarker)) {
                    //lr = new LogRecord(Level.WARNING,
                    //        "Record begin marker is not a proper value so using default.");
                    //lr.setThreadID((int) Thread.currentThread().getId());
                    //this.publish(lr);
                    recordBeginMarker = RECORD_BEGIN_MARKER;
                }

                recordEndMarker = manager.getProperty(cname + ".logFormatEndMarker");
                if (recordEndMarker == null || ("").equals(recordEndMarker)) {
                    //lr = new LogRecord(Level.WARNING,
                    //        "Record end marker is not a proper value so using default.");
                    //lr.setThreadID((int) Thread.currentThread().getId());
                    //this.publish(lr);
                    recordEndMarker = RECORD_END_MARKER;
                }

                recordFieldSeparator = manager.getProperty(cname + ".logFormatFieldSeparator");
                if (recordFieldSeparator == null || ("").equals(recordFieldSeparator) || recordFieldSeparator.length() > 1) {
                    //lr = new LogRecord(Level.WARNING,
                    //       "Log Format field separator is not a proper value so using default.");
                    //lr.setThreadID((int) Thread.currentThread().getId());
                    //this.publish(lr);
                    recordFieldSeparator = RECORD_FIELD_SEPARATOR;
                }

                recordDateFormat = manager.getProperty(cname + ".logFormatDateFormat");
                if (recordDateFormat != null && !("").equals(recordDateFormat)) {
                    SimpleDateFormat sdf = new SimpleDateFormat(recordDateFormat);
                    try {
                        sdf.format(new Date());
                    } catch (Exception e) {
                        //lr = new LogRecord(Level.WARNING,
                        //        "Date Format specified is wrong so using default.");
                        //lr.setThreadID((int) Thread.currentThread().getId());
                        //this.publish(lr);
                        recordDateFormat = RECORD_DATE_FORMAT;
                    }
                } else {
                    //lr = new LogRecord(Level.WARNING,
                    //        "Date Format specified is wrong so using default.");
                    //lr.setThreadID((int) Thread.currentThread().getId());
                    //this.publish(lr);
                    recordDateFormat = RECORD_DATE_FORMAT;
                }

                formatterClass.setRecordBeginMarker(recordBeginMarker);
                formatterClass.setRecordEndMarker(recordEndMarker);
                formatterClass.setRecordDateFormat(recordDateFormat);
                formatterClass.setRecordFieldSeparator(recordFieldSeparator);
            }

        } else if (formatterName == null || ODLLogFormatter.class.getName().equals(formatterName)) {
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
        } else {
            // this loop is used for any other formatter
            try {
                setFormatter((Formatter) this.getClass().getClassLoader().loadClass(formatterName).newInstance());
            } catch (InstantiationException e) {
                lr = new LogRecord(Level.SEVERE,
                        "Cannot instantiate formatter class " + formatterName);
                lr.setThreadID((int) Thread.currentThread().getId());
                lr.setLoggerName(getClass().getName());
                EarlyLogHandler.earlyMessages.add(lr);
            } catch (IllegalAccessException e) {
                lr = new LogRecord(Level.SEVERE,
                        "Cannot instantiate formatter class " + formatterName);
                lr.setThreadID((int) Thread.currentThread().getId());
                lr.setLoggerName(getClass().getName());
                EarlyLogHandler.earlyMessages.add(lr);
            } catch (ClassNotFoundException e) {
                lr = new LogRecord(Level.SEVERE,
                        "Cannot load formatter class " + formatterName);
                lr.setThreadID((int) Thread.currentThread().getId());
                lr.setLoggerName(getClass().getName());
                EarlyLogHandler.earlyMessages.add(lr);
            }
        }

        try {
            maxHistoryFiles = Integer.parseInt(manager.getProperty(cname + ".maxHistoryFiles"));
        } catch (NumberFormatException e) {
            lr = new LogRecord(Level.WARNING,
                    "Can't find maxHistoryFiles property from logging config file so using default.");
            lr.setThreadID((int) Thread.currentThread().getId());
            lr.setLoggerName(getClass().getName());
            EarlyLogHandler.earlyMessages.add(lr);
        }
        if (maxHistoryFiles < 0)
            maxHistoryFiles = 10;

    }

    public void preDestroy() {
        // stop the Queue consummer thread.
        LogDomains.getLogger(GFFileHandler.class, LogDomains.CORE_LOGGER).fine("Logger handler killed");
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
        synchronized (this) {
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
    synchronized void setLimitForRotation(int rotationLimitInBytes) {
//        if ((rotationLimitInBytes == 0) ||
//	        (rotationLimitInBytes >= MINIMUM_FILE_ROTATION_VALUE )) {
        limitForFileRotation = rotationLimitInBytes;
//        }
    }


    // NOTE: This private class is copied from java.util.logging.FileHandler
    // A metered stream is a subclass of OutputStream that
    //   (a) forwards all its output to a target stream
    //   (b) keeps track of how many bytes have been written

    private final class MeteredStream extends OutputStream {

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
        if (!parent.exists()) {
            parent.mkdirs();
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

        File dir = absoluteFile.getParentFile();
        if (dir == null) return;

        File[] fset = dir.listFiles();
        ArrayList candidates = new ArrayList();
        for (int i = 0; fset != null && i < fset.length; i++) {
            if (!logFileName.equals(fset[i].getName()) &&
                    fset[i].isFile() &&
                    fset[i].getName().startsWith(logFileName)) {
                candidates.add(fset[i].getAbsolutePath());
            }
        }
        if (candidates.size() <= maxHistoryFiles) return;

        Object[] pathes = candidates.toArray();
        java.util.Arrays.sort(pathes);
        try {
            for (int i = 0; i < pathes.length - maxHistoryFiles; i++) {
                File logFile = new File((String) pathes[i]);
                boolean delFile = logFile.delete();
                if (!delFile) {
                    publish(new LogRecord(Level.SEVERE,
                            "Error, could not delete log file: " + logFile.getAbsolutePath()));
                }
            }
        } catch (Exception e) {
            new ErrorManager().error("FATAL ERROR: COULD NOT DELETE LOG FILE..",
                    e, ErrorManager.GENERIC_FAILURE);
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
                        thisInstance.flush();
                        thisInstance.close();
                        try {
                            if (!absoluteFile.exists()) {
                                File creatingDeletedLogFile = new File(absoluteFile.getAbsolutePath());
                                creatingDeletedLogFile.createNewFile();
                                absoluteFile = creatingDeletedLogFile;
                            }
                            File oldFile = absoluteFile;
                            StringBuffer renamedFileName = new StringBuffer(absoluteFile + "_");
                            logRotateDateFormatter.format(
                                    new Date(), renamedFileName,
                                    new FieldPosition(0));
                            File rotatedFile = new File(renamedFileName.toString());
                            boolean renameSuccess = oldFile.renameTo(rotatedFile);
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
                                FileOutputStream fo =
                                        new FileOutputStream(freshServerLogFile);
                                fo.close();
                            }
                            FileOutputStream oldFileFO = new FileOutputStream(oldFile);
                            oldFileFO.close();
                            openFile(getLogFileName());
                            absoluteFile = getLogFileName();
                            // This will ensure that the log rotation timer
                            // will be restarted if there is a value set
                            // for time based log rotation
                            if (dayBasedFileRotation) {
                                LogRotationTimer.getInstance().restartTimerForDayBasedRotation();
                            } else {
                                LogRotationTimer.getInstance().restartTimer();
                            }

                            cleanUpHistoryLogFiles();
                        } catch (IOException ix) {
                            publish(new LogRecord(Level.SEVERE,
                                    "Error, could not rotate log : " + ix.getMessage()));
                        }
                        return null;
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
            synchronized (rotationRequested) {
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

        try {
            // set the thread id to be the current thread that is logging the message
//            record.setThreadID((int)Thread.currentThread().getId());
            pendingRecords.add(record);
        } catch (IllegalStateException e) {
            // queue is full, start waiting.
            try {
                pendingRecords.put(record);
            } catch (InterruptedException e1) {
                // too bad, record is lost...
            }
        }
    }

    protected File getLogFileName() {
//        return new File(new File(env.getDomainRoot(),LOGS_DIR), logFileName);
        return new File(absoluteServerLogName);

    }

    private int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }

}

