/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web.logger;

/**
 * An implementation of FileLoggerHandler which logs to virtual-server property 
 * log-file when enabled
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class FileLoggerHandler extends Handler {
    private static final int LOG_QUEUE_SIZE = 5000;
    private static final int FLUSH_FREQUENCY = 1;

    private volatile PrintWriter printWriter;
    private String logFile;

    private AtomicInteger association = new AtomicInteger(0);
    private AtomicBoolean done = new AtomicBoolean(false);
    private BlockingQueue<LogRecord> pendingRecords = new ArrayBlockingQueue<LogRecord>(LOG_QUEUE_SIZE);
    private Thread pump;

    FileLoggerHandler(String logFile) {
        setLevel(Level.ALL);
        this.logFile = logFile;
    
        try {
            printWriter = new PrintWriter(new FileOutputStream(logFile, true));
    	} catch (IOException e) {
            //throw new RuntimeException(e);
    	}

        pump = new Thread() {
            public void run() {
                try {
                    while (!done.get()) {
                        log();
                    }
                } catch(RuntimeException ex) {
                }
            }
        };
        pump.start();
    }

    private void writeLogRecord(LogRecord record) {
        if (printWriter != null) {
            printWriter.write(getFormatter().format(record)); 
            printWriter.flush();
        }
    }

    private void log() {
        // write the first record
        try {
            writeLogRecord(pendingRecords.take());
        } catch(InterruptedException e) {
            // ignore
        }

        // write FLUSH_FREQUENCY record(s) more
        List<LogRecord> list = new ArrayList<LogRecord>();
        int numOfRecords = pendingRecords.drainTo(list, FLUSH_FREQUENCY);
        for (int i = 0; i < numOfRecords; i++) {
            writeLogRecord(list.get(i));
        }
        flush();
    }

    /**
     * Increment the associations and return the result.
     */
    public int associate() {
        return association.incrementAndGet();
    }

    /**
     * Decrement the associations and return the result.
     */
    public int disassociate() {
        return association.decrementAndGet();
    }

    public boolean isAssociated() {
        return (association.get() > 0);
    }
    
    /**
     * Overridden method used to capture log entries   
     *
     * @param record The log record to be written out.
     */
    @Override
    public void publish(LogRecord record) {
        if (done.get()) {
            return;
        }

        // first see if this entry should be filtered out
        // the filter should keep anything
        if ( getFilter()!=null ) {
            if ( !getFilter().isLoggable(record) )
                return;
        }
        
        try {
            pendingRecords.add(record);
        } catch(IllegalStateException e) {
            // queue is full, start waiting
            try {
                pendingRecords.put(record);
            } catch(InterruptedException ex) {
                // too bad, record is lost...
            }
        }
    }

    
    /**
     * Called to close this log handler.
     */
    @Override
    public void close() {
        done.set(true);

        pump.interrupt();

        int size = pendingRecords.size();
        if (size > 0) {
            List<LogRecord> records = new ArrayList<LogRecord>(size);
            pendingRecords.drainTo(records, size);
            for (LogRecord record : records) {
                writeLogRecord(record);
            }
        }

        if (printWriter != null) {
            try {
                printWriter.flush();
            } catch(Exception ex) {
                // ignore
            } finally {
                printWriter.close();
            }
        }
    }
 
    
    /**
     * Called to flush any cached data that
     * this log handler may contain.
     */
    @Override
    public void flush() {
        if (printWriter != null) {
            printWriter.flush();
        }
    }

    /**
     * Return location of log file associated to this handler.
     */
    public String getLogFile() {
        return logFile;
    }
}
