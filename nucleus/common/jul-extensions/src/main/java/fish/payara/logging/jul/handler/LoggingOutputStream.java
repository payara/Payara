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
class LoggingOutputStream extends ByteArrayOutputStream {

    private final String lineSeparator;
    private final Level level;
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final LogRecordBuffer logRecordBuffer;

    private final Logger logger;
    private Thread pump;

    /**
     * Constructor
     *
     * @param logger Logger to write to
     * @param level  Level at which to write the log message
     * @param bufferCapacity maximal count of unprocessed records.
     */
    public LoggingOutputStream(final Logger logger, final Level level, final int bufferCapacity) {
        super();
        this.logger = logger;
        this.level = level;
        this.logRecordBuffer = new LogRecordBuffer(bufferCapacity);
        lineSeparator = System.lineSeparator();
        initializePump();
    }

    /**
     * Upon flush() write the existing contents of the OutputStream
     * to the logger as a log record.
     *
     * @throws IOException in case of error
     */
    @Override
    public void flush() throws IOException {
        final String logMessage;
        synchronized (this) {
            super.flush();
            logMessage = this.toString().trim();
            super.reset();
        }
        if (logMessage.isEmpty() || lineSeparator.equals(logMessage)) {
            // avoid empty records
            return;
        }
        final EnhancedLogRecord logRecord = new EnhancedLogRecord(level, logMessage);
        logRecord.setLoggerName(this.logger.getName());
        logRecordBuffer.add(logRecord);
    }


    @Override
    public void close() throws IOException {
        shutdown.set(true);
        while (true) {
            if (!logRecord(logRecordBuffer.poll())) {
                // end if there was nothing more to log
                break;
            }
        }
        super.close();
    }


    private void initializePump() {
        pump = new Thread() {
            @Override
            public void run() {
                while (!shutdown.get()) {
                    try {
                        logAllPendingRecords();
                    } catch (Exception e) {
                        // Continue the loop without exiting
                        // Something is broken, but we cannot log it
                    }
                }
            }
        };
        pump.setName("Logging pump for '" + logger.getName() + "'");
        pump.setDaemon(true);
        pump.setPriority(Thread.MAX_PRIORITY);
        pump.start();
    }


    /**
     * Retrieves all log records from the buffer and logs them
     */
    private void logAllPendingRecords() {
        if (!logRecord(logRecordBuffer.pollOrWait())) {
            return;
        }
        while (true) {
            if (!logRecord(logRecordBuffer.poll())) {
                // end if there was nothing more to log
                return;
            }
        }
    }


    /**
     * @return true if the record was not null
     */
    private boolean logRecord(final LogRecord record) {
        if (record == null) {
            return false;
        }
        // FIXME: possible deadlock
        logger.log(record);
        return true;
    }
}
