/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2025] [Payara Foundation and/or affiliates]
package com.sun.common.util.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    private final String lineSeparator;

    private Logger logger;
    private final Level level;

    private final ThreadLocal reentrant = new ThreadLocal();

    private final BlockingQueue<LogRecord> pendingRecords = new ArrayBlockingQueue<>(MAX_RECORDS);

    private final BooleanLatch done = new BooleanLatch();
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
        lineSeparator = System.lineSeparator();
        initializePump();
    }

    /**
     * upon flush() write the existing contents of the OutputStream
     * to the logger as a log record.
     *
     * @throws java.io.IOException in case of error
     */
    @Override
    public void flush() throws IOException {

        String logMessage = null;
        synchronized (this) {
            super.flush();
            logMessage = this.toString(StandardCharsets.UTF_8);
            super.reset();
        }
        if (logMessage.trim().length() == 0 || logMessage.trim().equals(lineSeparator)) {
            // avoid empty records
            return;
        }
        LogRecord logRecord = new LogRecord(level, logMessage);

        // JUL LogRecord does not capture thread-name. Create a wrapper to
        // capture the name of the logging thread so that a formatter can
        // output correct thread-name if done asynchronously. Note that
        // this fix is limited to records published through this handler only.
        GFLogRecord logRecordWrapper = new GFLogRecord(logRecord);
        logRecordWrapper.setThreadName(Thread.currentThread().getName());

        if (!pendingRecords.offer(logRecordWrapper)) {
            logger.fine("LoggingOutputStream.flush(): pending records queue is full");
        }
    }

    private void initializePump() {
        pump = new Thread() {
            @Override
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
        pump.setName("Logging output pump");
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
        List<LogRecord> v = new ArrayList<>();
        final int size = pendingRecords.size();
        int msgs = pendingRecords.drainTo(v, size);
        for (int j = 0; j < msgs; j++) {
            logger.log(v.get(j));
        }

    }

    @Override
    public void close() throws IOException {
        done.tryReleaseShared(1);
        pump.interrupt();

        // Drain and log the remaining messages
        final int size = pendingRecords.size();
        if (size > 0) {
            Collection<LogRecord> records = new ArrayList<>(size);
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

        private ThreadLocal<StackTraceObjects> perThreadStObjects = new ThreadLocal<>();

        public LoggingPrintStream(ByteArrayOutputStream os) {
            super(os, true, StandardCharsets.UTF_8);
        }

        public void setLogger(Logger l) {
            logger = l;
        }

        @Override
        public void println(Object x) {
            if (!checkLocks()) return;

            if (!(x instanceof java.lang.Throwable)) {
                // No special processing if it is not an exception.
                println(String.valueOf(x));
            } else {
                StackTraceObjects sTO = new StackTraceObjects((Throwable) x);
                perThreadStObjects.set(sTO);
                super.println(sTO.toString());
            }

        }

        @Override
        public PrintStream printf(String str, Object... args) {
            StringBuilder sb = new StringBuilder();
            try (Formatter formatter = new Formatter(sb, Locale.getDefault())) {
                formatter.format(str, args);
            }
            print(sb.toString());
            return  null;
        }

        @Override
        public PrintStream printf(Locale locale, String str, Object... args) {
            StringBuilder sb = new StringBuilder();
            try (Formatter formatter = new Formatter(sb, locale)) {
                formatter.format(str,args);
            }
            print(sb.toString());
            return  null;
        }

        @Override
        public PrintStream format(String format, Object... args) {
            StringBuilder sb = new StringBuilder();
            try (Formatter formatter = new Formatter(sb, Locale.getDefault())) {
                formatter.format(Locale.getDefault(), format, args);
            }
            print(sb.toString());
            return  null;
        }

        @Override
        public PrintStream format(Locale locale,String format, Object... args) {
            StringBuilder sb = new StringBuilder();
            try (Formatter formatter = new Formatter(sb, locale)) {
                formatter.format(locale, format, args);
            }
            print(sb.toString());
            return  null;
        }

        @Override
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

        @Override
        public void print(String x) {
            if (checkLocks())
                super.print(x);
        }


        @Override
        public void print(Object x) {
            if (checkLocks())
                super.print(x);
        }

        @Override
        public void print(boolean x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        @Override
        public void println(boolean x) {
            if (checkLocks())
                super.println(x);
        }

        @Override
        public void print(char x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        @Override
        public void println(char x) {
            if (checkLocks())
                super.println(x);
        }

        @Override
        public void print(int x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        @Override
        public void println(int x) {
            if (checkLocks())
                super.println(x);
        }

        @Override
        public void print(long x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        @Override
        public void println(long x) {
            if (checkLocks())
                super.println(x);
        }

        @Override
        public void print(float x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        @Override
        public void println(float x) {
            if (checkLocks())
                super.println(x);
        }

        @Override
        public void print(double x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        @Override
        public void println(double x) {
            if (checkLocks())
                super.println(x);
        }

        @Override
        public void print(char[] x) {
            if (checkLocks()) {
                super.print(x);
            }
        }

        @Override
        public void println(char[] x) {
            if (checkLocks())
                super.println(x);
        }


        @Override
        public void println() {
            if (checkLocks()) {
                super.println();
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            try {
                synchronized (this) {
                    if (out == null)
                        throw new IOException("Stream closed");
                    out.write(buf, off, len);
                    if (len <= 8129)
                        out.flush();
                }
            }
            catch (InterruptedIOException x) {
                Thread.currentThread().interrupt();
            }
            catch (IOException x) {
                setError();
            }
        }

        @Override
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
            return !t.holdsLock(logger) && !t.holdsLock(logManager);
        }
    }

/*
 * StackTraceObjects keeps track of StackTrace printed
 * by a thread as a result of println(Throwable) and
 * it keeps track of subsequent println(String) to
 * avoid duplicate logging of stacktrace
 */

    private static class StackTraceObjects {

        private final ByteArrayOutputStream stackTraceBuf;
        private final PrintStream stStream;
        private final String stString;
        private final ByteArrayOutputStream comparisonBuf;
        private final PrintStream cbStream;
        private int stackTraceBufBytes = 0;
        private int charsIgnored = 0;

        private StackTraceObjects(Throwable x) {
            // alloc buffer for getting stack trace.
            stackTraceBuf = new ByteArrayOutputStream();
            stStream = new PrintStream(stackTraceBuf, true, StandardCharsets.UTF_8);
            comparisonBuf = new ByteArrayOutputStream();
            cbStream = new PrintStream(comparisonBuf, true, StandardCharsets.UTF_8);
            ((Throwable) x).printStackTrace(stStream);
            stString = stackTraceBuf.toString(StandardCharsets.UTF_8);
            stackTraceBufBytes = stackTraceBuf.size();
            // helps keep our stack trace skipping logic simpler.
            cbStream.println(x);
        }

        @Override
        public String toString() {
            return stString;
        }

        boolean ignorePrintln(String str) {
            String cbString;
            int cbLen;
            cbStream.println(str);
            cbString = comparisonBuf.toString(StandardCharsets.UTF_8);
            cbLen = cbString.length();
            if (stString.regionMatches(charsIgnored, cbString, 0, cbLen)) {
                charsIgnored += cbLen;
                comparisonBuf.reset();
                return true;
            }

            return false;

        }

        boolean checkCompletion() {
            return charsIgnored >= stackTraceBufBytes;
        }
    }

}
