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

// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package com.sun.gjc.monitoring;

import com.sun.gjc.util.SQLTrace;
import fish.payara.jdbc.stats.FrequentSQLTraceCache;
import fish.payara.jdbc.stats.SlowSqlTrace;
import fish.payara.jdbc.stats.SlowSqlTraceCache;
import java.util.List;
import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.ListStatistic;
import org.glassfish.external.statistics.impl.ListStatisticImpl;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.resourcebase.resources.api.PoolInfo;

/**
 * Provides the monitoring data for JDBC RA module
 *
 * @author Shalini M
 */
@AMXMetadata(type="jdbcra-mon", group="monitoring")
@ManagedObject
@Description("JDBC RA Statistics")
public class JdbcStatsProvider {

    private ListStatisticImpl freqUsedSqlQueries = new ListStatisticImpl(
            "FreqUsedSqlQueries", "List",
            "Most frequently used sql queries");
    
    private ListStatisticImpl slowSqlQueries = new ListStatisticImpl("SlowSqlQueries", "List", "Slowest SQL Queries");
    
    private CountStatisticImpl numStatementCacheHit = new CountStatisticImpl(
            "NumStatementCacheHit", StatisticImpl.UNIT_COUNT,
            "The total number of Statement Cache hits.");

    private CountStatisticImpl numStatementCacheMiss = new CountStatisticImpl(
            "NumStatementCacheMiss", StatisticImpl.UNIT_COUNT,
            "The total number of Statement Cache misses.");

    private CountStatisticImpl numPotentialStatementLeak = new CountStatisticImpl(
            "NumPotentialStatementLeak", StatisticImpl.UNIT_COUNT,
            "The total number of potential Statement leaks");

    private PoolInfo poolInfo;
    private FrequentSQLTraceCache freqSqlTraceCache;
    private SlowSqlTraceCache slowSqlTraceCache;

    public JdbcStatsProvider(String poolName, String appName, String moduleName, int sqlTraceCacheSize,
            long timeToKeepQueries) {
        poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(sqlTraceCacheSize > 0) {
            this.freqSqlTraceCache = new FrequentSQLTraceCache(poolName, sqlTraceCacheSize, timeToKeepQueries);
            this.slowSqlTraceCache = new SlowSqlTraceCache(poolName, sqlTraceCacheSize, timeToKeepQueries);
        }
    }
    
    public JdbcStatsProvider(String poolName, String appName, String moduleName, int sqlTraceCacheSize,
            long timeToKeepQueries, int maxStoredEntries) {
        poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(sqlTraceCacheSize > 0) {
            this.freqSqlTraceCache = new FrequentSQLTraceCache(poolName, sqlTraceCacheSize, timeToKeepQueries, maxStoredEntries);
            this.slowSqlTraceCache = new SlowSqlTraceCache(poolName, sqlTraceCacheSize, timeToKeepQueries);
        }
    }

    /**
     * Whenever statement cache is hit, increment numStatementCacheHit count.
     * @param poolName JdbcConnectionPool that has got a statement cache hit event.
     */
    @ProbeListener(JdbcRAConstants.STATEMENT_CACHE_DOTTED_NAME + JdbcRAConstants.STATEMENT_CACHE_HIT)
    public void statementCacheHitEvent(@ProbeParam("poolName") String poolName,
                                       @ProbeParam("appName") String appName,
                                       @ProbeParam("moduleName") String moduleName
                                       ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)){
            numStatementCacheHit.increment();
        }
    }

    /**
     * Whenever statement cache miss happens, increment numStatementCacheMiss count.
     * @param poolName JdbcConnectionPool that has got a statement cache miss event.
     */
    @ProbeListener(JdbcRAConstants.STATEMENT_CACHE_DOTTED_NAME + JdbcRAConstants.STATEMENT_CACHE_MISS)
    public void statementCacheMissEvent(@ProbeParam("poolName") String poolName,
                                        @ProbeParam("appName") String appName,
                                        @ProbeParam("moduleName") String moduleName
                                        ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)){
            numStatementCacheMiss.increment();
        }
    }

    /**
     * Whenever a sql statement that is traced is to be cache for monitoring
     * purpose, the SQLTrace object is created for the specified sql and
     * updated in the SQLTraceCache. This is used to update the
     * frequently used and slowest sql queries.
     *
     * @param poolName
     * @param appName
     * @param sql
     * @param moduleName
     * @param executionTime
     */
    @ProbeListener(JdbcRAConstants.SQL_TRACING_DOTTED_NAME + JdbcRAConstants.TRACE_SQL)
    public void traceSQLEvent(
                                   @ProbeParam("poolName") String poolName,
                                   @ProbeParam("appName") String appName,
                                   @ProbeParam("moduleName") String moduleName,
                                   @ProbeParam("sql") String sql,
                                   @ProbeParam("executionTime") long executionTime) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)){
            if(freqSqlTraceCache != null) {
                if (sql != null) {
                    SQLTrace cacheObj = new SQLTrace(sql, 1, System.currentTimeMillis());
                    freqSqlTraceCache.checkAndUpdateCache(cacheObj);
                }
            }
            
            // If the slowSqlTraceCache has been initialised, create a new SlowSqlTrace object and add it to or update 
            // the cache
            if (slowSqlTraceCache != null && sql != null) {
                SQLTrace cacheObj = new SlowSqlTrace(sql, 1, System.currentTimeMillis(), executionTime);
                slowSqlTraceCache.checkAndUpdateCache(cacheObj);
            }
        }
    }

    /**
     * Whenever statement leak happens, increment numPotentialStatementLeak count.
     * @param poolName JdbcConnectionPool that has got a statement leak event.
     */
    @ProbeListener(JdbcRAConstants.STATEMENT_LEAK_DOTTED_NAME + JdbcRAConstants.POTENTIAL_STATEMENT_LEAK)
    public void potentialStatementLeakEvent(
                                   @ProbeParam("poolName") String poolName,
                                   @ProbeParam("appName") String appName,
                                   @ProbeParam("moduleName") String moduleName) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)){
            numPotentialStatementLeak.increment();
        }
    }


    @ManagedAttribute(id="numstatementcachehit")
    public CountStatistic getNumStatementCacheHit() {
        return numStatementCacheHit;
    }

    @ManagedAttribute(id="numstatementcachemiss")
    public CountStatistic getNumStatementCacheMiss() {
        return numStatementCacheMiss;
    }

    @ManagedAttribute(id="frequsedsqlqueries")
    public ListStatistic getfreqUsedSqlQueries() {
        if (freqSqlTraceCache != null) {
            List<SQLTrace> sqlTraces = freqSqlTraceCache.getTopQueries();
            freqUsedSqlQueries = new ListStatisticImpl("frequsedsqlqueries", "List", "Most frequently used sql queries");

            for (SQLTrace trace : sqlTraces) {
                CountStatisticImpl stat = new CountStatisticImpl(trace.getQueryName(), "Count", "");
                stat.setCount(trace.getNumExecutions());
                freqUsedSqlQueries.add(stat);
            }
        }

        return freqUsedSqlQueries;
    }
    
    @ManagedAttribute(id = "slowSqlQueries")
    public ListStatistic getSlowSqlQueries() {
        //Make sure no data from previous execution is kept
        slowSqlQueries.reset();
        slowSqlQueries.clear();
        
        if (slowSqlTraceCache != null) {
            //Get slow queries and process them
            List<SlowSqlTrace> slowTraces = slowSqlTraceCache.getSlowestSqlQueries();
            for (SlowSqlTrace trace : slowTraces) {
                CountStatisticImpl stat = new CountStatisticImpl(trace.getQueryName(), StatisticImpl.UNIT_MILLISECOND, "Longest execution time");
                stat.setCount(trace.getSlowestExecutionTime());
                slowSqlQueries.add(stat);
            }
        }
        
        return slowSqlQueries;
    }

    @ManagedAttribute(id="numpotentialstatementleak")
    public CountStatistic getNumPotentialStatementLeak() {
        return numPotentialStatementLeak;
    }
    
    /**
     * Get the FrequentSQLTraceCache associated with this stats provider.
     * @return FrequentSQLTraceCache The FrequentSQLTraceCache associated with this stats provider
     */
    public FrequentSQLTraceCache getFreqSqlTraceCache() {
        return freqSqlTraceCache;
    }
    
    /**
     * Get the SlowSqlTraceCache associated with this stats provider.
     * @return SlowSqlTraceCache The SlowSqlTraceCache associated with this stats provider
     */
    public SlowSqlTraceCache getSlowSqlTraceCache() {
        return slowSqlTraceCache;
    }
}
