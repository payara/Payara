/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.resource.pool;

import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connection leak detector, book keeps the caller stack-trace during getConnection()<br>
 * Once the leak-timeout expires, assumes a connection leak and prints the caller stack-trace<br>
 * Also, reclaims the connection if connection-leak-reclaim in ON<br>
 *
 * @author Kshitiz Saxena, Jagadish Ramu
 */
public class ConnectionLeakDetector {
    private HashMap<ResourceHandle, StackTraceElement[]> connectionLeakThreadStackHashMap;
    private HashMap<ResourceHandle, ConnectionLeakTask> connectionLeakTimerTaskHashMap;
    private boolean connectionLeakTracing;
    private long connectionLeakTimeoutInMillis;
    private boolean connectionLeakReclaim;
    private PoolInfo connectionPoolInfo;
    private Map<ResourceHandle, ConnectionLeakListener> listeners;

    //Lock on HashMap to trace connection leaks
    private final Object connectionLeakLock;

    private final static Logger _logger = LogDomains.getLogger(ConnectionLeakDetector.class, LogDomains.RSR_LOGGER);
    private final static StringManager localStrings =
            StringManager.getManager(ConnectionPool.class);


    public ConnectionLeakDetector(PoolInfo poolInfo, boolean leakTracing, long leakTimeoutInMillis, boolean leakReclaim) {
        connectionPoolInfo = poolInfo;
        connectionLeakThreadStackHashMap = new HashMap<ResourceHandle, StackTraceElement[]>();
        connectionLeakTimerTaskHashMap = new HashMap<ResourceHandle, ConnectionLeakTask>();
        listeners = new HashMap<ResourceHandle, ConnectionLeakListener>();
        connectionLeakLock = new Object();
        connectionLeakTracing = leakTracing;
        connectionLeakTimeoutInMillis = leakTimeoutInMillis;
        connectionLeakReclaim = leakReclaim;
    }

    public void reset(boolean leakTracing, long leakTimeoutInMillis, boolean leakReclaim) {
        if (!connectionLeakTracing && leakTracing) {
            clearAllConnectionLeakTasks();
        }
        connectionLeakTracing = leakTracing;
        connectionLeakTimeoutInMillis = leakTimeoutInMillis;
        connectionLeakReclaim = leakReclaim;
    }

    private void registerListener(ResourceHandle handle, ConnectionLeakListener listener) {
        listeners.put(handle, listener);
    }

    private void unRegisterListener(ResourceHandle handle) {
        listeners.remove(handle);
    }

    /**
     * starts connection leak tracing
     *
     * @param resourceHandle Resource which needs to be traced
     * @param listener       Leak Listener
     */
    public void startConnectionLeakTracing(ResourceHandle resourceHandle, ConnectionLeakListener listener) {
        if (connectionLeakTracing) {
            synchronized (connectionLeakLock) {
                if (!connectionLeakThreadStackHashMap.containsKey(resourceHandle)) {
                    connectionLeakThreadStackHashMap.put(resourceHandle, Thread.currentThread().getStackTrace());
                    ConnectionLeakTask connectionLeakTask = new ConnectionLeakTask(resourceHandle);
                    connectionLeakTimerTaskHashMap.put(resourceHandle, connectionLeakTask);
                    registerListener(resourceHandle, listener);
                    if (getTimer() != null)
                        getTimer().schedule(connectionLeakTask, connectionLeakTimeoutInMillis);
                }
            }
        }
    }

    /**
     * stops connection leak tracing
     *
     * @param resourceHandle Resource which needs to be traced
     * @param listener       Leak Listener
     */
    public void stopConnectionLeakTracing(ResourceHandle resourceHandle, ConnectionLeakListener listener) {
        if (connectionLeakTracing) {
            synchronized (connectionLeakLock) {
                if (connectionLeakThreadStackHashMap.containsKey(resourceHandle)) {
                    connectionLeakThreadStackHashMap.remove(resourceHandle);
                    ConnectionLeakTask connectionLeakTask = connectionLeakTimerTaskHashMap.remove(resourceHandle);
                    connectionLeakTask.cancel();
                    getTimer().purge();
                    unRegisterListener(resourceHandle);
                }
            }
        }
    }

    /**
     * Logs the potential connection leaks
     *
     * @param resourceHandle Resource that is not returned by application
     */
    private void potentialConnectionLeakFound(ResourceHandle resourceHandle) {
        synchronized (connectionLeakLock) {
            if (connectionLeakThreadStackHashMap.containsKey(resourceHandle)) {
                StackTraceElement[] threadStack = connectionLeakThreadStackHashMap.remove(resourceHandle);
                ConnectionLeakListener connLeakListener = listeners.get(resourceHandle);
                connLeakListener.potentialConnectionLeakFound();
                printConnectionLeakTrace(threadStack, connLeakListener);
                connectionLeakTimerTaskHashMap.remove(resourceHandle);
                if (connectionLeakReclaim) {
                    resourceHandle.markForReclaim(true);
                    connLeakListener.reclaimConnection(resourceHandle);
                }
                //Unregister here as the listeners would still be present in the map.
                unRegisterListener(resourceHandle);
            }
        }
    }

    /**
     * Prints the stack trace of thread leaking connection to server logs
     *
     * @param threadStackTrace Application(caller) thread stack trace
     */
    private void printConnectionLeakTrace(StackTraceElement[] threadStackTrace,
            ConnectionLeakListener connLeakListener) {
        StringBuffer stackTrace = new StringBuffer();
        String msg = localStrings.getStringWithDefault(
                "potential.connection.leak.msg",
                "A potential connection leak detected for connection pool " + connectionPoolInfo +
                        ". The stack trace of the thread is provided below : ",
                new Object[]{connectionPoolInfo});
        stackTrace.append(msg);
        stackTrace.append("\n");
        for (int i = 2; i < threadStackTrace.length; i++) {
            stackTrace.append(threadStackTrace[i].toString());
            stackTrace.append("\n");
        }
        connLeakListener.printConnectionLeakTrace(stackTrace);
        _logger.log(Level.WARNING, stackTrace.toString(), "ConnectionPoolName=" + connectionPoolInfo);
    }

    /**
     * Clear all connection leak tracing tasks in case of connection leak
     * tracing being turned off
     */
    private void clearAllConnectionLeakTasks() {
        synchronized (connectionLeakLock) {
            for (ResourceHandle resourceHandle : connectionLeakTimerTaskHashMap.keySet()) {
                ConnectionLeakTask connectionLeakTask = connectionLeakTimerTaskHashMap.get(resourceHandle);
                connectionLeakTask.cancel();
            }
            if (getTimer() != null)
                getTimer().purge();
            connectionLeakThreadStackHashMap.clear();
            connectionLeakTimerTaskHashMap.clear();
        }
    }

    private Timer getTimer() {
        return ConnectorRuntime.getRuntime().getTimer();
    }

    private class ConnectionLeakTask extends TimerTask {

        private ResourceHandle resourceHandle;

        ConnectionLeakTask(ResourceHandle resourceHandle) {
            this.resourceHandle = resourceHandle;
        }

        public void run() {
            potentialConnectionLeakFound(resourceHandle);
        }
    }
}
