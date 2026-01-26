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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright 2018-2026 Payara Foundation and/or its affiliates

package com.sun.enterprise.server.logging;

import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.ContractsProvided;
import jakarta.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;

import com.sun.common.util.logging.BooleanLatch;
import com.sun.common.util.logging.GFLogRecord;

import jakarta.inject.Singleton;

import java.text.SimpleDateFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.*;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: Mar 11, 2009
 * Time: 1:41:30 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
@Singleton
@ContractsProvided({SyslogHandler.class, java.util.logging.Handler.class})
public class SyslogHandler extends Handler implements PostConstruct, PreDestroy {

    @Inject
    ServerEnvironmentImpl env;

    private Syslog sysLogger;
    private Thread pump= null;
    private BooleanLatch done = new BooleanLatch();
    private BlockingQueue<LogRecord> pendingRecords = new ArrayBlockingQueue<LogRecord>(5000);
    private SimpleFormatter simpleFormatter = new SimpleFormatter();
    private int facility;


    public void postConstruct() {

        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();

        String systemLogging = TranslatedConfigView.expandValue(manager.getProperty(cname + ".useSystemLogging"));
        // Added below 2 lines of code to avoid NPE as per the bug http://java.net/jira/browse/GLASSFISH-16162
        if(systemLogging==null || systemLogging.equals("false")) {
            return;
        }

        String host = TranslatedConfigView.expandValue(manager.getProperty(cname + ".host"));
        if (host == null) {
            host = "localhost";
        }

        String facility = TranslatedConfigView.expandValue(manager.getProperty(cname + ".facility"));
        try {
            this.facility = Facility.fromString(facility);
        } catch (IllegalArgumentException e) {
            this.facility = Facility.DAEMON.getValue();
        }

        //set up the connection
        setupConnection(host);
        initializePump();
    }

    private void initializePump() {
        // start the Queue consummer thread.
        pump = new Thread() {
            public void run() {
                try {
                    while (!done.isSignalled()) {
                        if (pump.isInterrupted()) {
                            break;
                        }
                        log();
                    }
                } catch (RuntimeException e) {

                }
            }
        };
        pump.start();
    }

    @Deprecated
    private void setupConnection(){
        setupConnection("localhost");
    }

    private void setupConnection(String host){
        try {
            sysLogger = new Syslog(host);
        } catch (java.net.UnknownHostException e) {
            LogFacade.LOGGING_LOGGER.log(Level.SEVERE, LogFacade.ERROR_INIT_SYSLOG, e);
            return;
        }
    }

    public void preDestroy() {
        if (LogFacade.LOGGING_LOGGER.isLoggable(Level.FINE)) {
            LogFacade.LOGGING_LOGGER.fine("SysLog Logger handler killed");
        }
    }

    /**
     * Retrieves the LogRecord from our Queue and store them in the file
     *
     */
    public void log() {

            LogRecord record;

            try {
                record = pendingRecords.take();
            } catch (InterruptedException e) {
                return;
            }
        Level level= record.getLevel();
            long millisec = record.getMillis();
            int syslogLevel = Syslog.INFO;
            String logLevel = "INFO";

            if (level.equals(Level.SEVERE)) {
                syslogLevel = Syslog.CRIT;
                logLevel = "CRIT";
        } else if (level.equals(Level.WARNING)){
                syslogLevel = Syslog.WARNING;
                logLevel = "WARNING";
        } else if(level.intValue() <= Level.FINE.intValue())   {
                syslogLevel = Syslog.DEBUG;
                logLevel = "DEBUG";
            }

            //format the message
            StringBuilder sb = new StringBuilder();
            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd HH:mm:ss");
            sb.append(formatter.format(millisec));
            sb.append(" [ ");
            sb.append(logLevel);
            sb.append(" ");
            if (record.getLoggerName() == null) {
                sb.append("Payara");
            } else {
                sb.append(record.getLoggerName());
            }
            sb.append(" ] ");
            String formattedMsg = simpleFormatter.formatMessage(record);
            sb.append(formattedMsg);
            //send message
            if (sysLogger != null) {
                sysLogger.log(facility, syslogLevel, sb.toString());
            }

        }

    /**
     * Publishes the logrecord storing it in our queue
     */
    @Override
    public void publish( LogRecord record ) {
        if (pump == null)
            return;

        GFLogRecord wrappedRecord = GFLogRecord.wrap(record, false);
        try {
            pendingRecords.add(wrappedRecord);
        } catch(IllegalStateException e) {
            // queue is full, start waiting.
            try {
                pendingRecords.put(wrappedRecord);
            } catch (InterruptedException e1) {
                // to bad, record is lost...
            }
        }
    }

    public void close() {

    }

    public void flush() {

    }

    @Deprecated
    public void setSystemLogging(boolean systemLogging) {
        setSystemLogging(systemLogging, "localhost");
    }


    public void setSystemLogging(boolean systemLogging, String host) {
        if (systemLogging) {
            //set up the connection
            setupConnection(host);
            initializePump();
        } else {
            if (pump != null && pump.isAlive()) {
                pump.interrupt();
            }
        }
    }

}

