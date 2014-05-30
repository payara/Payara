/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.common.util.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Implementation of a OutputStream that flush the records to a Logger.
 * This is useful to redirect stderr and stdout to loggers.
 * <p/>
 * User: Jerome Dochez
 * author: Jerome Dochez, Carla Mott
 */
public class LoggingOutputStream extends ByteArrayOutputStream {

    private static final int MAX_RECORDS = 5000;

    private String lineSeparator;

    private Logger logger;
    private Level level;

    private ThreadLocal reentrant = new ThreadLocal();
    
    private BlockingQueue<LogRecord> pendingRecords = new ArrayBlockingQueue<LogRecord>(MAX_RECORDS);

    private BooleanLatch done = new BooleanLatch();
    private Thread pump;

    /**
     * Constructor
     *
     * @param logger Logger to write to
     * @param level  Level at which to write the log message
     */
    public LoggingOutputStream(Logger logger, Level level) {
        super();
        this.logger = logger;
        this.level = level;
        lineSeparator = System.getProperty("line.separator");
        initializePump();
    }

    /**
     * upon flush() write the existing contents of the OutputStream
     * to the logger as a log record.
     *
     * @throws java.io.IOException in case of error
     */
    public void flush() throws IOException {

        String logMessage = null;
        synchronized (this) {
            super.flush();
            logMessage = this.toString();
            super.reset();
        }
        logMessage = logMessage.trim();
        if (logMessage.length() == 0 || logMessage.equals(lineSeparator)) {
            // avoid empty records
            return;
        }
        LogRecord logRecord = new LogRecord(level, logMessage);
        pendingRecords.offer(logRecord);
    }

    private void initializePump() {
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

    /**
     * Retrieves the LogRecord from our Queue and log them
     */
    public void log() {

        LogRecord record;
        
        // take is blocking so we take one record off the queue
        try {
            record = pendingRecords.take();
            if (reentrant.get() != null) {
                return;
            }
            try {
                reentrant.set(this);
                logger.log(record);
            } finally {
                reentrant.set(null);
            }
        } catch (InterruptedException e) {
            return;
        }

        // now try to read more.  we end up blocking on the above take call if nothing is in the queue
        Vector<LogRecord> v = new Vector<LogRecord>();
        final int size = pendingRecords.size();
        int msgs = pendingRecords.drainTo(v, size);
        for (int j = 0; j < msgs; j++) {
            logger.log(v.get(j));
        }

    }
    
    public void close() throws IOException {
        done.tryReleaseShared(1);
        pump.interrupt();

        // Drain and log the remaining messages
        final int size = pendingRecords.size();
        if (size > 0) {
            Collection<LogRecord> records = new ArrayList<LogRecord>(size);
            pendingRecords.drainTo(records, size);
            for (LogRecord record : records) {
                logger.log(record);
            }
        }
        
        super.close();
    }
    
/*
 * LoggingPrintStream creates a PrintStream with a
 * LoggingByteArrayOutputStream as its OutputStream. Once it is
 * set as the System.out or System.err, all outputs to these
 * PrintStreams will end up in LoggingByteArrayOutputStream
 * which will log these on a flush.
 * This simple behavious has a negative side effect that
 * stack traces are logged with each line being a new log record.
 * The reason for above is that printStackTrace converts each line
 * into a separate println, causing a flush at the end of each.
 * One option that was thought of to smooth this over was to see
 * if the caller of println is Throwable.[some set of methods].
 * Unfortunately, there are others who interpose on System.out and err
 * (like jasper) which makes that check untenable.
 * Hence the logic currently used is to see if there is a println(Throwable)
 * and do a printStackTrace and log the complete StackTrace ourselves.
 * If this is followed by a series of printlns, then we keep ignoring
 * those printlns as long as they were the same as that recorded by
 * the stackTrace. This needs to be captured on a per thread basis
 * to take care of potentially parallel operations.
 * Care is taken to optimise the frequently used path where exceptions
 * are not being printed.
 */

    public class LoggingPrintStream extends PrintStream {
        LogManager logManager = LogManager.getLogManager();

        private ThreadLocal perThreadStObjects = new ThreadLocal();

        public LoggingPrintStream(ByteArrayOutputStream os) {
            super(os, true);

        }

        public void setLogger(Logger l) {
            logger = l;
        }

        public void println(Object x) {
            if (!checkLocks()) return;

            if (!(x instanceof java.lang.Throwable)) {
                // No special processing if it is not an exception.
                println(x.toString());
                return;
            } else {
                StackTraceObjects sTO = new StackTraceObjects((Throwable) x);
                perThreadStObjects.set(sTO);
                super.println(sTO.toString());                
            }

        }

        public PrintStream printf(String str, Object... args) {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.getDefault());
            formatter.format(str,args);
            print(sb.toString());
            return  null;
        }

        public PrintStream printf(Locale locale, String str, Object... args) {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, locale);
            formatter.format(str,args);
            print(sb.toString());
            return  null;
        }

        public PrintStream format(String format, Object... args) {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.getDefault());
            formatter.format(Locale.getDefault(), format, args);
            print(sb.toString());
            return  null;
        }

        public PrintStream format(Locale locale,String format, Object... args) {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, locale);
            formatter.format(locale, format, args);
            print(sb.toString());
            return  null;
        }

        public void println(String str) {
            if (!checkLocks()) return;

            StackTraceObjects sTO;
            sTO = (StackTraceObjects) perThreadStObjects.get();
            if (sTO == null) {
                // lets get done with the common case fast
                super.println(str);
                return;
            }

            if (!sTO.ignorePrintln(str)) {
                perThreadStObjects.set(null);
                super.println(str);
                return;
            }

            if (sTO.checkCompletion()) {
                perThreadStObjects.set(null);
                return;
            }
        }

        public void print(String x) {
            if (checkLocks())
                super.print(x);
        }


        public void print(Object x) {
            if (checkLocks())
                super.print(x);
        }

        public void print(boolean x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        public void println(boolean x) {
            if (checkLocks())
                super.println(x);
        }

        public void print(char x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        public void println(char x) {
            if (checkLocks())
                super.println(x);
        }

        public void print(int x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        public void println(int x) {
            if (checkLocks())
                super.println(x);
        }

        public void print(long x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        public void println(long x) {
            if (checkLocks())
                super.println(x);
        }

        public void print(float x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        public void println(float x) {
            if (checkLocks())
                super.println(x);
        }

        public void print(double x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        public void println(double x) {
            if (checkLocks())
                super.println(x);
        }

        public void print(char[] x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        public void println(char[] x) {
            if (checkLocks())
                super.println(x);
        }


        public void println() {
            if (checkLocks()) {
                super.println();
            }
        }

        public void write(byte[] buf, int off, int len) {
            if (checkLocks()) {
                super.write(buf, off, len);
            }
        }

        public void write(int b) {
            if (checkLocks()) {
                super.write(b);
            }
        }

        /*
          LoggingPrintStream class is to support the java System.err and System.out
          redirection to Appserver log file--server.log.
          When Java IO is redirected and System.out.println(...) is invoked by a thread with
          LogManager or Logger(SYSTEMERR_LOGGER,SYSTEOUT_LOGGER) locked, all kind of dead
          locks among threads will happen.
          These dead locks are easily reproduced when jvm system properties
          "-Djava.security.manager" and "-Djava.security.debug=access,failure" are defined.
          These dead lcoks are basically because each thread has its own sequence of
          acquiring lock objects(LogManager,Logger,FileandSysLogHandler, the buffer inside
          LoggingPrintStream).
          There is no obvious way to define the lock hierarchy and control the lock sequence;
          Trylock is not a strightforward solution either.Beside they both create heavy
          dependence on the detail implementation of JDK and Appserver.

          This method(checkLocks) is to find which locks current thread has and
          LoggingPrintStream object will decide whether to continue to do printing or
          give ip up to avoid the dead lock.
         */

        private boolean checkLocks() {
            Thread t = Thread.currentThread();
            if (!t.holdsLock(logger) && !t.holdsLock(logManager)) {
                return true;
            }
            return false;
        }
    }

/*
 * StackTraceObjects keeps track of StackTrace printed
 * by a thread as a result of println(Throwable) and
 * it keeps track of subsequent println(String) to
 * avoid duplicate logging of stacktrace
 */

    private static class StackTraceObjects {

        private ByteArrayOutputStream stackTraceBuf;
        private PrintStream stStream;
        private String stString;
        private ByteArrayOutputStream comparisonBuf;
        private PrintStream cbStream;
        private int stackTraceBufBytes = 0;
        private int charsIgnored = 0;

        private StackTraceObjects(Throwable x) {
            // alloc buffer for getting stack trace.
            stackTraceBuf = new ByteArrayOutputStream();
            stStream = new PrintStream(stackTraceBuf, true);
            comparisonBuf = new ByteArrayOutputStream();
            cbStream = new PrintStream(comparisonBuf, true);
            ((Throwable) x).printStackTrace(stStream);
            stString = stackTraceBuf.toString();
            stackTraceBufBytes = stackTraceBuf.size();
            // helps keep our stack trace skipping logic simpler.
            cbStream.println(x);
        }

        public String toString() {
            return stString;
        }

        boolean ignorePrintln(String str) {
            String cbString;
            int cbLen;
            cbStream.println(str);
            cbString = comparisonBuf.toString();
            cbLen = cbString.length();
            if (stString.regionMatches(charsIgnored, cbString, 0, cbLen)) {
                charsIgnored += cbLen;
                comparisonBuf.reset();
                return true;
            }

            return false;

        }

        boolean checkCompletion() {
            if (charsIgnored >= stackTraceBufBytes) {
                return true;
            } else {
                return false;
            }
        }
    }
    
}
