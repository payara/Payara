/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
     * Request for adding a sql query in the form of SQLTrace to this cache.
     * If the query is already found
     * in the list, the number of times it is executed is incremented by one
     * along with the timestamp.
     * If the query is a new one, it is added to the list.
     * 
     * @param sqlTrace
     * @return
     */
    @Override
    public void checkAndUpdateCache(SQLTrace sqlTrace) {    
        if (sqlTrace != null) {
            SlowSqlTrace slowSqlTrace = (SlowSqlTrace) sqlTrace;
            SlowSqlTrace storedSlowSqlTrace = (SlowSqlTrace) cache.get(slowSqlTrace.getQueryName());

            if (storedSlowSqlTrace != null) {
                //If already found in the cache
                //equals is invoked here and hence comparison based on query name is done
                //Get the object at the index to update the numExecutions
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
    
    public String getSlowestSqlQueries() {
        purgeEntries();
        
        List<SlowSqlTrace> slowSqlTraceList = new ArrayList(cache.values());
        Collections.sort(slowSqlTraceList, SlowSqlTrace.SlowSqlTraceSlowestExecutionComparator);

        String slowestSqlQueries = "";
        for (SlowSqlTrace slowSqlTrace : slowSqlTraceList) {
            slowestSqlQueries += LINE_BREAK + slowSqlTrace.getQueryName() + ": " + slowSqlTrace.getSlowestExecutionTime();
        }
        
        return slowestSqlQueries;
    }
}
