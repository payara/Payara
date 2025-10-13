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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package fish.payara.jdbc.stats;

import com.sun.gjc.util.SQLTrace;
import com.sun.gjc.util.SQLTraceCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Maintains the Sql Tracing Cache used to store SQL statements used by the
 * applications. This is used by the JDBCRA monitoring to display the most
 * frequently used queries by applications.
 *
 * @author Shalini M
 */
public class FrequentSQLTraceCache extends SQLTraceCache {

    //Maximum size of the cache.
    protected long maxStoredEntries = 10000;
    
    public FrequentSQLTraceCache(String poolName, int numToReport, long timeToKeepQueries) {
        super(poolName, numToReport, timeToKeepQueries);
    }
    
    public FrequentSQLTraceCache(String poolName, int numToReport, long timeToKeepQueries, long maxStoredEntries) {
        super(poolName, numToReport, timeToKeepQueries);
        this.maxStoredEntries = maxStoredEntries;
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
    @Override
    public void checkAndUpdateCache(SQLTrace cacheObj) {
        if (cacheObj != null) {
            SQLTrace trace = cache.get(cacheObj.getQueryName());
            if (trace != null) {
                //If already found in the cache
                trace.setNumExecutions(trace.getNumExecutions() + 1);
                trace.setLastUsageTime(System.currentTimeMillis());
            } else {
                if (cache.size() < maxStoredEntries){
                    cache.put(cacheObj.getQueryName(), cacheObj);
                } else {
                    if (_logger.isLoggable(Level.CONFIG)) {
                        _logger.log(Level.CONFIG, "Frequent SQL Trace Cache full, {0} not stored.", cacheObj.getQueryName());
                    } else {
                        _logger.log(Level.INFO, "Frequent SQL Trace Cache full, enable CONFIG log level for more info.");
                    }
                }
            }
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
    public List<SQLTrace> getTopQueries() {
        purgeEntries();
        
        List<SQLTrace> sqlTraceList = new ArrayList(cache.values());
        Collections.sort(sqlTraceList, SQLTrace.SQLTraceFrequencyComparator);
        
        return sqlTraceList;
    }
}
