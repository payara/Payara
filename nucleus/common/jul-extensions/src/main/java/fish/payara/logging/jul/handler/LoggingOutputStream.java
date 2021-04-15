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
// Portions Copyright [2016-2021] [Payara Foundation and/or affiliates]
package fish.payara.logging.jul.handler;

import fish.payara.logging.jul.record.EnhancedLogRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Implementation of a {@link ByteArrayOutputStream} that flush the records to a {@link Logger}.
 * This is useful to redirect stderr and stdout to loggers.
 * <p/>
 *
 * @author Jerome Dochez
 * @author Carla Mott
 * @author David Matejcek
 */
final class LoggingOutputStream extends ByteArrayOutputStream {

    private final String lineSeparator;
    private final Level logRecordLevel;
    private final LogRecordBuffer logRecordBuffer;
    private final String loggerName;
    private final Pump pump;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Constructor
     *
     * @param logger Logger to write to
     * @param logRecordLevel  Level at which to write the log message
     * @param bufferCapacity maximal count of unprocessed records.
     */
    LoggingOutputStream(final Logger logger, final Level logRecordLevel, final int bufferCapacity) {
        super();
        this.lineSeparator = System.lineSeparator();
        this.loggerName = logger.getName();
        this.logRecordLevel = logRecordLevel;
        this.logRecordBuffer = new LogRecordBuffer(bufferCapacity);
        this.pump = new Pump(logger, this.logRecordBuffer);
    }

    /**
     * Upon flush() write the existing contents of the OutputStream
     * to the logger as a log record.
     *
     * @throws IOException in case of error
     */
    @Override
    public void flush() throws IOException {
        if (closed.get()) {
            return;
        }
        final String logMessage = getMessage();
        if (logMessage.isEmpty() || lineSeparator.equals(logMessage)) {
            // avoid empty records
            return;
        }
        final EnhancedLogRecord logRecord = new EnhancedLogRecord(logRecordLevel, logMessage, false);
        logRecord.setLoggerName(this.loggerName);
        logRecordBuffer.add(logRecord);
    }

    private synchronized String getMessage() throws IOException {
        super.flush();
        final String logMessage = super.toString(StandardCharsets.UTF_8.name()).trim();
        super.reset();
        return logMessage;
    }


    /**
     * Shutdown the internal logging pump.
     */
    @Override
    public void close() throws IOException {
        closed.set(true);
        pump.shutdown();
        super.close();
    }


    /**
     * Paren't {@link ByteArrayOutputStream#toString()} is synchronized. This method isn't.
     *
     * @return name of the class and information about the logger
     */
    @Override
    public String toString() {
        return getClass().getName() + " redirecting messages to the " + loggerName;
    }


    private static final class Pump extends Thread {

        private final LogRecordBuffer buffer;
        private final Logger logger;

        private Pump(final Logger logger, final LogRecordBuffer buffer) {
            this.buffer = buffer;
            this.logger = logger;
            setName("Logging pump for '" + logger.getName() + "'");
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY);
            start();
        }


        @Override
        public void run() {
            // the thread will be interrupted by it's owner finally
            while (true) {
                try {
                    logAllPendingRecordsOrWait();
                } catch (Exception e) {
                    // Continue the loop without exiting
                    // Something is broken, but we cannot log it
                }
            }
        }


        /**
         * Kindly asks the pump to closed it's service. If the pump is locked waiting
         * on the buffer, interrupts the thread.
         * <p>
         * The pump can be locked, waiting
         */
        void shutdown() {
            this.interrupt();
            // we interrupted waiting or working thread, now we have to process remaining records.
            logAllPendingRecords();
        }


        /**
         * Retrieves all log records from the buffer and logs them or waits for some.
         */
        private void logAllPendingRecordsOrWait() {
            if (!logRecord(buffer.pollOrWait())) {
                return;
            }
            logAllPendingRecords();
        }


        private void logAllPendingRecords() {
            while (true) {
                if (!logRecord(buffer.poll())) {
                    // end if there was nothing more to log
                    return;
                }
            }
        }


        /**
         * @return false if the record was null
         */
        private boolean logRecord(final LogRecord record) {
            if (record == null) {
                return false;
            }
            logger.log(record);
            return true;
        }
    }
}
