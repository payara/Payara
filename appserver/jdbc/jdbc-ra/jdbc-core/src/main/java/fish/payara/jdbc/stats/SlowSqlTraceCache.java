/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jdbc.stats;

import com.sun.gjc.util.SQLTrace;
import com.sun.gjc.util.SQLTraceCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Andrew Pielage
 */
public class SlowSqlTraceCache extends SQLTraceCache {
    
    public SlowSqlTraceCache(String poolName, int maxNumberOfQueries, long timeToKeepQueries) {
        super(poolName, maxNumberOfQueries, timeToKeepQueries);
    }
    
    /**
     * Request for adding a sql query in the form of SlowSqlTrace to this cache.
     * If the query is already found in the list, the number of times it is executed is incremented by one
     * along with the timestamp. If its slowest execution time has also increased, this is updated.
     * If the query is a new one, it is added to the list.
     * 
     * @param sqlTrace
     */
    @Override
    public void checkAndUpdateCache(SQLTrace sqlTrace) {    
        if (sqlTrace != null) {
            // Cast to a slowSqlTrace object
            SlowSqlTrace slowSqlTrace = (SlowSqlTrace) sqlTrace;
            SlowSqlTrace storedSlowSqlTrace = (SlowSqlTrace) cache.get(slowSqlTrace.getQueryName());

            if (storedSlowSqlTrace != null) {
                //If already found in the cache
                storedSlowSqlTrace.setNumExecutions(storedSlowSqlTrace.getNumExecutions() + 1);
                storedSlowSqlTrace.setLastUsageTime(System.currentTimeMillis());
                
                long newExecutionTime = slowSqlTrace.getSlowestExecutionTime();
                if (storedSlowSqlTrace.getSlowestExecutionTime() < newExecutionTime) {
                    storedSlowSqlTrace.setSlowestExecutionTime(newExecutionTime);
                }
            } else {
                cache.put(slowSqlTrace.getQueryName(), slowSqlTrace);
            }
        }
    }
    
    /**
     * Overrides the purgeEntries method to purge the quickest running SQL Traces
     */
    @Override
    public void purgeEntries() {
        List<SlowSqlTrace> slowSqlTraceList = new ArrayList(cache.values());
        Collections.sort(slowSqlTraceList, SlowSqlTrace.SlowSqlTraceSlowestExecutionComparator);
        
        int elementCount = slowSqlTraceList.size();        
        while (elementCount > numTopQueriesToReport) {
            SlowSqlTrace slowSqlTrace = slowSqlTraceList.get(elementCount - 1);
            cache.remove(slowSqlTrace.getQueryName());
            slowSqlTraceList.remove(slowSqlTrace);
            elementCount --;
        }
    }
    
    /**
     * Returns the slowest SQL traces.
     * @return A String representation of the slowest SQL Traces
     */
    public List<SlowSqlTrace> getSlowestSqlQueries() {
        purgeEntries();
        
        List<SlowSqlTrace> slowSqlTraceList = new ArrayList(cache.values());
        Collections.sort(slowSqlTraceList, SlowSqlTrace.SlowSqlTraceSlowestExecutionComparator);
        
        return slowSqlTraceList;
    }
}
