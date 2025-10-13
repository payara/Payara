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
// Portions Copyright [2016-2017] [Payara Foundation and/or its affiliates]

package com.sun.gjc.util;

import com.sun.logging.LogDomains;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for Sql Tracing Caches used to store SQL statements used by applications.
 *
 * @author Shalini M
 */
public abstract class SQLTraceCache {

    //Maximum size of the cache.
    protected int numTopQueriesToReport = 10;
    protected long timeToKeepQueries = 60000;
    protected final String poolName;
    protected ConcurrentSkipListMap<String, SQLTrace> cache;
    
    private SQLTraceTimerTask sqlTraceTimerTask;
    protected final static Logger _logger = LogDomains.getLogger(SQLTraceCache.class,
            LogDomains.RSR_LOGGER);
    protected static final String LINE_BREAK = "%%%EOL%%%";

    public SQLTraceCache(String poolName, int maxSize, long timeToKeepQueries) {
        this.cache = new ConcurrentSkipListMap<>();
        this.poolName = poolName;
        this.numTopQueriesToReport = maxSize;
        this.timeToKeepQueries = timeToKeepQueries * 60 * 1000;
    }

    public Collection<String> getSqlTraceList() {
        return cache.keySet();
    }

    public String getPoolName() {
        return poolName;
    }
    
    /**
     * Schedule timer to perform purgeEntries on the cache after the
     * specified timeToKeepQueries delay and period.
     * @param timer
     */
    public void scheduleTimerTask(Timer timer) {
        if(sqlTraceTimerTask != null) {
            sqlTraceTimerTask.cancel();
            sqlTraceTimerTask = null;
        }

        sqlTraceTimerTask = initializeTimerTask();

        if(timer != null && timeToKeepQueries > 0) {

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
     */
    public abstract void checkAndUpdateCache(SQLTrace cacheObj);

    /**
     * Entries are removed from the list after sorting
     * them in the least frequently used order. Only numTopQueriesToReport number of
     * entries are maintained in the list after the purgeEntries.
     */
    public void purgeEntries() {
        List<SQLTrace> sqlTraceList = new ArrayList(cache.values());
        Collections.sort(sqlTraceList, SQLTrace.SQLTraceFrequencyComparator);
        
        int elementCount = sqlTraceList.size();        
        while (elementCount > numTopQueriesToReport) {
            SQLTrace sqlTrace = sqlTraceList.get(elementCount - 1);
            cache.remove(sqlTrace.getQueryName());
            sqlTraceList.remove(sqlTrace);
            elementCount --;
        }
    }
}
