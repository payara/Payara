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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]

package fish.payara.logging.jul.handler;

import fish.payara.logging.jul.handler.Syslog.SyslogLevel;
import fish.payara.logging.jul.i18n.MessageResolver;
import fish.payara.logging.jul.record.EnhancedLogRecord;

import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;


public class SyslogHandler extends Handler {
    private static final MessageResolver MSG_RESOLVER = new MessageResolver();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss");

    private final SimpleFormatter simpleFormatter = new SimpleFormatter();
    private final LogRecordBuffer pendingRecords = new LogRecordBuffer(5000);

    private Syslog sysLogger;
    private LoggingPump pump;

    public SyslogHandler() {
        final String cname = getClass().getName();
        final LogManager manager = LogManager.getLogManager();
        final String systemLogging = manager.getProperty(cname + ".useSystemLogging");
        if (systemLogging == null || systemLogging.equals("false")) {
            return;
        }
        sysLogger = setupConnection();
        pump = new LoggingPump("SyslogHandler log pump", this.pendingRecords);
        pump.start();
    }


    @Override
    public void publish(final LogRecord record) {
        if (pump == null || record == null) {
            return;
        }
        pendingRecords.add(MSG_RESOLVER.resolve(record));
    }


    @Override
    public void flush() {
        // nothing to do
    }


    @Override
    public synchronized void close() {
        if (pump != null && pump.isAlive()) {
            pump.interrupt();
            pump = null;
        }
    }


    private void log(final EnhancedLogRecord record) {
        if (sysLogger == null) {
            return;
        }
        final SyslogLevel syslogLevel = SyslogLevel.of(record.getLevel());
        final StringBuilder sb = new StringBuilder();
        sb.append(TIME_FORMATTER.format(record.getTime()));
        sb.append(" [ ");
        sb.append(syslogLevel.name());
        sb.append(" glassfish ] ");
        sb.append(simpleFormatter.formatMessage(record));
        sysLogger.log(syslogLevel, sb.toString());
    }


    private static Syslog setupConnection() {
        try {
            // for now only write to this host
            return new Syslog("localhost");
        } catch (final UnknownHostException e) {
            throw new IllegalStateException("Could not initialize the SyslogHandler's connection.", e);
        }
    }

    private final class LoggingPump extends LoggingPumpThread {

        private LoggingPump(final String threadName, final LogRecordBuffer buffer) {
            super(threadName, buffer);
        }


        @Override
        protected boolean isShutdownRequested() {
            return sysLogger == null || pump == null;
        }


        @Override
        protected int getFlushFrequency() {
            return 1;
        }


        @Override
        protected boolean logRecord(final EnhancedLogRecord record) {
            if (record == null) {
                return false;
            }
            log(record);
            return true;
        }


        @Override
        protected void flushOutput() {
            flush();
        }
    }
}

