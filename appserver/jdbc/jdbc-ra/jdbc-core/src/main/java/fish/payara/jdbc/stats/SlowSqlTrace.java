/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.jdbc.stats;

import com.sun.gjc.util.SQLTrace;
import java.util.Comparator;

/**
 *
 * @author Andrew Pielage
 */
public class SlowSqlTrace extends SQLTrace {
    
    private long slowestExecutionTime;
    
    public SlowSqlTrace(String query, int numExecutions, long time, long executionTime) {
        super(query, numExecutions, time);
        this.slowestExecutionTime = executionTime;
    }
    
    public long getSlowestExecutionTime() {
        return slowestExecutionTime;
    }
    
    public void setSlowestExecutionTime(long slowestExecutionTime) {
        this.slowestExecutionTime = slowestExecutionTime;
    }
    
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof SlowSqlTrace)) {
            throw new ClassCastException("SqlTraceCache object is expected");
        }
        
        long lastUsageTime = ((SlowSqlTrace) o).getLastUsageTime();
        long execTime = ((SlowSqlTrace) o).getSlowestExecutionTime();
        
        int compare = 0;
        
        // Compare the execution times
        if (execTime == this.getSlowestExecutionTime()) {
            compare = 0;
        } else if (execTime < this.getSlowestExecutionTime()) {
            compare = -1;
        } else {
            compare = 1;
        }
        
        // If the slowestExecutionTime is the same, compare against last used
        if (compare == 0) {
            if(lastUsageTime == this.getLastUsageTime()) {
                compare = 0;
            } else if(lastUsageTime < this.getLastUsageTime()) {
                compare = -1;
            } else {
                compare = 1;
            }
        }

        return compare;
    }
    
    public static Comparator<SlowSqlTrace> SlowSqlTraceSlowestExecutionComparator = new Comparator<SlowSqlTrace>() {
        
        @Override
        public int compare(SlowSqlTrace sqlTrace1, SlowSqlTrace sqlTrace2) {
            final long sqlTrace1SlowestExecution = sqlTrace1.getSlowestExecutionTime();
            final long sqlTrace2SlowestExecution = sqlTrace2.getSlowestExecutionTime();
            
            
            int compare = 0;
            if (sqlTrace1SlowestExecution < sqlTrace2SlowestExecution) {
                compare = 1;
            } else if (sqlTrace1SlowestExecution > sqlTrace2SlowestExecution) {
                compare = -1;
            }
            
            if (compare == 0) {
                final int sqlTrace1Frequency = sqlTrace1.getNumExecutions();
                final int sqlTrace2Frequency = sqlTrace2.getNumExecutions();
                
                if (sqlTrace1Frequency < sqlTrace2Frequency) {
                    compare = 1;
                } else if (sqlTrace1Frequency > sqlTrace2Frequency) {
                    compare = -1;
                }
                
                if (compare == 0) {
                    final long sqlTrace1LastUsageTime = sqlTrace1.getLastUsageTime();
                    final long sqlTrace2LastUsageTime = sqlTrace2.getLastUsageTime();
                    
                    if (sqlTrace1LastUsageTime < sqlTrace2LastUsageTime) {
                        compare = 1;
                    } else if (sqlTrace1LastUsageTime > sqlTrace2LastUsageTime) {
                        compare = -1;
                    }
                }
            }
            
            return compare;
        }
    };
}
