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
//Portions Copyright [2016] [Payara Foundation and/or its affiliates]
package com.sun.gjc.util;

import com.sun.gjc.monitoring.JdbcRAConstants;
import com.sun.gjc.monitoring.SQLTraceProbeProvider;
import com.sun.logging.LogDomains;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.jdbc.SQLTraceListener;
import org.glassfish.api.jdbc.SQLTraceRecord;

/**
 * Implementation of SQLTraceListener to listen to events related to a sql
 * record tracing. The registry allows multiple listeners to listen to the sql
 * tracing events. Maintains a list of listeners.
 *
 * @author Shalini M
 */
//@Singleton
public class SQLTraceDelegator implements SQLTraceListener {

    //List of listeners 
    protected List<SQLTraceListener> sqlTraceListenersList;
    private String poolName;
    private String appName;
    private String moduleName;
    private static final Logger logger = LogDomains.getLogger(SQLTraceLogger.class, LogDomains.SQL_TRACE_LOGGER);

    private SQLTraceProbeProvider probeProvider = null;

    public SQLTraceProbeProvider getProbeProvider() {
        return probeProvider;
    }

    public SQLTraceDelegator(String poolName, String appName, String moduleName) {
        this.poolName = poolName;
        this.appName = appName;
        this.moduleName = moduleName;
        probeProvider = new SQLTraceProbeProvider();
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Add a listener to the list of sql trace listeners maintained by this
     * registry.
     *
     * @param listener
     */
    public void registerSQLTraceListener(SQLTraceListener listener) {
        if (sqlTraceListenersList == null) {
            sqlTraceListenersList = new ArrayList<>();
        }
        // check there isn't already a listener of the specified type
        for (SQLTraceListener test : sqlTraceListenersList) {
            if (test.getClass().equals(listener.getClass())) {
                return;
            }
        }
        sqlTraceListenersList.add(listener);
    }

    /**
     * Removes a listener from the list of SQL trace listeners maintained by
     * this registry.
     * @param listener The class of listener to remove
     */
    public void deregisterSQLTraceListener(Class listener) {
        if (sqlTraceListenersList == null) {
            return;
        }
        
        for (SQLTraceListener registeredListener : sqlTraceListenersList) {
            if (registeredListener.getClass().equals(listener)) {
                sqlTraceListenersList.remove(registeredListener);
                break;
            }
        }
    }
    
    /**
     * Checks whether any SQLTraceListeners are registered to this delegator.
     * @return true if there are listeners registered.
     */
    public boolean listenersRegistered() {
        boolean listenersRegistered = false;
        
        if (!sqlTraceListenersList.isEmpty()) {
            listenersRegistered = true;
        }
        
        return listenersRegistered;
    }
    
    @Override
    public void sqlTrace(SQLTraceRecord record) {
        if (record != null) {
            record.setPoolName(poolName);
            if (sqlTraceListenersList != null) {
                for (SQLTraceListener listener : sqlTraceListenersList) {
                    try {
                        listener.sqlTrace(record);
                    } catch (Throwable t) { // don't let a broken listener break the JDBC calls
                      logger.log(Level.WARNING, "SQL Trace Listener threw exception", t);
                    }
                }
            }

            String methodName = record.getMethodName();
            //Check if the method name is one in which sql query is used
            if (isMethodValidForCaching(methodName)) {
                Object[] params = record.getParams();
                if (params != null && params.length > 0) {
                    String sqlQuery = null;
                    for (Object param : params) {
                        if (param instanceof String) {
                            sqlQuery = param.toString();
                        }
                        break;
                    }
                    if (sqlQuery != null) {
                        probeProvider.traceSQLEvent(poolName, appName, moduleName, sqlQuery);
                    }
                }
            }
        }
    }
    
    /**
     * Check if the method name from the sql trace record can be used to retrieve a
     * sql string for caching purpose. Most of the method names do not contain a sql
     * string and hence are unusable for caching the sql strings. These method names
     * are filtered in this method.
     *
     * @param methodName
     * @return true if method name can be used to get a sql string for caching.
     */
    private boolean isMethodValidForCaching(String methodName) {
        return JdbcRAConstants.validSqlTracingMethodNames.contains(methodName);
    }
}
