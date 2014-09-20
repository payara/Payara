/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.logging.LogDomains;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maintains the Sql Tracing Cache used to store SQL statements used by the
 * applications. This is used by the JDBCRA monitoring to display the most
 * frequently used queries by applications.
 *
 * @author Shalini M
 */
public class SQLTraceCache {

    //List of sql trace objects
    private final List<SQLTrace> list;
    //Maximum size of the cache.
    private int numTopQueriesToReport = 10;
    private long timeToKeepQueries = 60 * 1000;
    private SQLTraceTimerTask sqlTraceTimerTask;
    private String poolName;
    private String appName;
    private String moduleName;
    private final static Logger _logger = LogDomains.getLogger(SQLTraceCache.class,
            LogDomains.RSR_LOGGER);
    private static final String LINE_BREAK = "%%%EOL%%%";

    public SQLTraceCache(String poolName, String appName, String moduleName, int maxSize, long timeToKeepQueries) {
        this.poolName = poolName;
        this.appName = appName;
        this.moduleName = moduleName;
        this.numTopQueriesToReport = maxSize;
        list = new ArrayList<SQLTrace>();
        this.timeToKeepQueries = timeToKeepQueries * 60 * 1000;
    }

    public List<SQLTrace> getSqlTraceList() {
        return list;
    }

    public String getPoolName() {
        return poolName;
    }
    
    /**
     * Schedule timer to perform purgeEntries on the cache after the
     * specified timeToKeepQueries delay and period.
     */
    public void scheduleTimerTask(Timer timer) {

        if(sqlTraceTimerTask != null) {
            sqlTraceTimerTask.cancel();
            sqlTraceTimerTask = null;
        }

        sqlTraceTimerTask = initializeTimerTask();

        if(timer != null) {

            timer.scheduleAtFixedRate(sqlTraceTimerTask, timeToKeepQueries, timeToKeepQueries);
        }
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("Scheduled Sql Trace Caching timer task");
        }
    }

    /**
     * Cancel the timer task used to perform a purgeEntries on the cache.
     */
    public synchronized void cancelTimerTask() {

        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("Cancelling Sql Trace Caching timer task");
        }
        if (sqlTraceTimerTask != null) {
            sqlTraceTimerTask.cancel();
        }
        sqlTraceTimerTask = null;
    }

    /**
     * Instantiate the timer task used to perform a purgeEntries on the cache
     *
     * @return SQLTraceTimerTask
     */
    private SQLTraceTimerTask initializeTimerTask() {
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("Initializing Sql Trace Caching timer task");
        }
        return new SQLTraceTimerTask(this);
    }

    /**
     * Request for adding a sql query in the form of SQLTrace to this cache.
     * If the query is already found
     * in the list, the number of times it is executed is incremented by one
     * along with the timestamp.
     * If the query is a new one, it is added to the list.
     * 
     * @param cacheObj
     * @return
     */
    public void checkAndUpdateCache(SQLTrace cacheObj) {
        synchronized (list) {
            if (cacheObj != null) {
                int index = list.indexOf(cacheObj);
                if (index != -1) {
                    //If already found in the cache
                    //equals is invoked here and hence comparison based on query name is done
                    //Get the object at the index to update the numExecutions
                    SQLTrace cache = (SQLTrace) list.get(index);
                    cache.setNumExecutions(cache.getNumExecutions() + 1);
                    cache.setLastUsageTime(System.currentTimeMillis());
                } else {
                    //First occurrence of the query. query to be added.
                    cacheObj.setNumExecutions(1);
                    cacheObj.setLastUsageTime(System.currentTimeMillis());
                    list.add(cacheObj);
                }
            }
        }
    }

    /**
     * Entries are removed from the list after sorting
     * them in the least frequently used order. Only numTopQueriesToReport number of
     * entries are maintained in the list after the purgeEntries.
     */
    public void purgeEntries() {
        synchronized(list) {
            Collections.sort(list, Collections.reverseOrder());
            Iterator i = list.iterator();
            while (i.hasNext()) {
                SQLTrace cacheObj = (SQLTrace) i.next();
                if (list.size() > numTopQueriesToReport) {
                    if(_logger.isLoggable(Level.FINEST)) {
                        _logger.finest("removing sql=" + cacheObj.getQueryName());
                    }
                    i.remove();
                } else {
                    break;
                }
            }
            //sort by most frequently used queries first
            Collections.sort(list);
        }
    }

    /**
     * Returns the String representation of the list of traced sql queries
     * ordered by the number most frequently used, followed by the usage
     * timestamp. Only the top 'n' queries represented by the numTopQueriesToReport are
     * chosen for display.
     *
     * @return string representation of the list of sql queries sorted
     */
    public String getTopQueries() {
        purgeEntries();
        StringBuffer sb = new StringBuffer();
        for(SQLTrace cache : list) {
            sb.append(LINE_BREAK);
            sb.append(cache.getQueryName());
        }
        return sb.toString();
    }
}
