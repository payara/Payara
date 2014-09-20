/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.gjc.util;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.gjc.monitoring.StatementLeakProbeProvider;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Statement leak detector that prints the stack trace of the thread when a
 * statement object is leaked. Once the leak timeout expires, a statement leak
 * is assumed and the caller stack trace is printed. When statement-leak-reclaim
 * is set to true, the statement object is reclaimed.
 *
 * @author Shalini M
 */
public class StatementLeakDetector {
    private HashMap<Statement, StackTraceElement[]> statementLeakThreadStackHashMap;
    private HashMap<Statement, StatementLeakTask> statementLeakTimerTaskHashMap;
    private PoolInfo poolInfo;
    private boolean statementLeakTracing;
    private long statementLeakTimeoutInMillis;
    private boolean statementLeakReclaim;
    //Lock on HashMap to trace statement leaks
    private final Object statementLeakLock;
    private Map<Statement, StatementLeakListener> listeners;
    private final static Logger _logger = LogDomains.getLogger(
            StatementLeakDetector.class, LogDomains.RSR_LOGGER);
    private final static StringManager localStrings =
            StringManager.getManager(StatementLeakDetector.class);
    private Timer timer;
    private StatementLeakProbeProvider stmtLeakProbeProvider = null;

    public StatementLeakDetector(PoolInfo poolInfo, boolean leakTracing,
            long leakTimeoutInMillis, boolean leakReclaim, Timer timer) {
        this.poolInfo = poolInfo;
        statementLeakThreadStackHashMap = new HashMap<Statement, StackTraceElement[]>();
        statementLeakTimerTaskHashMap = new HashMap<Statement, StatementLeakTask>();
        listeners = new HashMap<Statement, StatementLeakListener>();
        statementLeakLock = new Object();
        statementLeakTracing = leakTracing;
        statementLeakTimeoutInMillis = leakTimeoutInMillis;
        statementLeakReclaim = leakReclaim;
        this.timer = timer;
        stmtLeakProbeProvider = new StatementLeakProbeProvider();
    }

    public void reset(boolean leakTracing, long leakTimeoutInMillis, boolean leakReclaim) {
        if (!statementLeakTracing && leakTracing) {
            clearAllStatementLeakTasks();
        }
        statementLeakTracing = leakTracing;
        statementLeakTimeoutInMillis = leakTimeoutInMillis;
        statementLeakReclaim = leakReclaim;
    }


    private void registerListener(Statement stmt, StatementLeakListener listener) {
        listeners.put(stmt, listener);
    }

    private void unRegisterListener(Statement stmt) {
        listeners.remove(stmt);
    }


    /**
     * Starts statement leak tracing
     *
     * @param stmt Statement which needs to be traced
     * @param listener       Leak Listener
     */
    public void startStatementLeakTracing(Statement stmt, StatementLeakListener listener) {
        synchronized (statementLeakLock) {
            if (!statementLeakThreadStackHashMap.containsKey(stmt)) {
                statementLeakThreadStackHashMap.put(stmt, Thread.currentThread().getStackTrace());
                StatementLeakTask statementLeakTask = new StatementLeakTask(stmt);
                statementLeakTimerTaskHashMap.put(stmt, statementLeakTask);
                registerListener(stmt, listener);
                if (timer != null) {
                    timer.schedule(statementLeakTask, statementLeakTimeoutInMillis);
                    if(_logger.isLoggable(Level.FINEST)) {
                        _logger.finest("Scheduled Statement leak tracing timer task");
                    }
                }
            }
        }
    }

    /**
     * Stops statement leak tracing
     *
     * @param stmt Statement which needs to be traced
     * @param listener       Leak Listener
     */
    public void stopStatementLeakTracing(Statement stmt, StatementLeakListener listener) {
        synchronized (statementLeakLock) {
            if (statementLeakThreadStackHashMap.containsKey(stmt)) {
                statementLeakThreadStackHashMap.remove(stmt);
                StatementLeakTask statementLeakTask =
                        statementLeakTimerTaskHashMap.remove(stmt);
                statementLeakTask.cancel();
                timer.purge();
                if(_logger.isLoggable(Level.FINEST)) {
                    _logger.finest("Stopped Statement leak tracing timer task");
                }
                unRegisterListener(stmt);
            }
        }
    }


    /**
     * Logs the potential statement leaks
     *
     * @param stmt Statement that is not closed by application
     */
    private void potentialStatementLeakFound(Statement stmt) {
        synchronized (statementLeakLock) {
            if (statementLeakThreadStackHashMap.containsKey(stmt)) {
                StackTraceElement[] threadStack = statementLeakThreadStackHashMap.remove(stmt);
                StatementLeakListener stmtLeakListener = listeners.get(stmt);
                stmtLeakProbeProvider.potentialStatementLeakEvent(poolInfo.getName(),
                        poolInfo.getApplicationName(), poolInfo.getModuleName());
                printStatementLeakTrace(threadStack);
                statementLeakTimerTaskHashMap.remove(stmt);
                if (statementLeakReclaim) {
                    try {
                        stmtLeakListener.reclaimStatement();
                    } catch (SQLException ex) {
                        Object[] params = new Object[]{poolInfo, ex};
                        _logger.log(Level.WARNING, 
                                "statement.leak.detector_reclaim_statement_failure",
                                params);
                    }
                }
                //Unregister here as the listeners would still be present in the map.
                unRegisterListener(stmt);
            }
        }
    }

    /**
     * Prints the stack trace of thread leaking statement to server logs
     *
     * @param threadStackTrace Application(caller) thread stack trace
     */
    private void printStatementLeakTrace(StackTraceElement[] threadStackTrace) {
        StringBuffer stackTrace = new StringBuffer();
        String msg = localStrings.getStringWithDefault(
                "potential.statement.leak.msg",
                "A potential statement leak detected for connection pool " + poolInfo +
                        ". The stack trace of the thread is provided below : ",
                new Object[]{poolInfo});
        stackTrace.append(msg);
        stackTrace.append("\n");
        for (int i = 2; i < threadStackTrace.length; i++) {
            stackTrace.append(threadStackTrace[i].toString());
            stackTrace.append("\n");
        }
        _logger.log(Level.WARNING, stackTrace.toString(), "ConnectionPoolName=" + poolInfo);
    }

    /**
     * Clear all statement leak tracing tasks in case of statement leak
     * tracing being turned off
     */
    public void clearAllStatementLeakTasks() {
        synchronized (statementLeakLock) {
            for (Statement stmt : statementLeakTimerTaskHashMap.keySet()) {
                StatementLeakTask statementLeakTask = statementLeakTimerTaskHashMap.get(stmt);
                statementLeakTask.cancel();
            }
            if (timer != null)
                timer.purge();
            statementLeakThreadStackHashMap.clear();
            statementLeakTimerTaskHashMap.clear();
        }
    }


    private class StatementLeakTask extends TimerTask {

        private Statement statement;

        StatementLeakTask(Statement stmt) {
            this.statement = stmt;
        }

        public void run() {
            potentialStatementLeakFound(statement);
        }
    }

}
