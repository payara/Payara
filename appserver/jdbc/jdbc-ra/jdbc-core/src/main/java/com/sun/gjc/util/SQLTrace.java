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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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

// Portions Copyright [2016] [Payara Foundation and/or its affiliates]

package com.sun.gjc.util;

import java.util.Comparator;

/**
 * Store the sql queries executed by applications along with the number of
 * times executed and the time stamp of the last usage.
 * Used for monitoring information.
 * 
 * @author Shalini M
 */
public class SQLTrace {

    private String queryName;
    private int numExecutions;
    private long lastUsageTime;

    public SQLTrace(String query, int numExecutions, long time) {
        this.queryName = query;
        this.numExecutions = numExecutions;
        this.lastUsageTime = time;
    }
    
    /**
     * Get the value of queryName
     *
     * @return the value of queryName
     */
    public String getQueryName() {
        return queryName;
    }

    /**
     * Set the value of queryName
     *
     * @param queryName new value of queryName
     */
    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    /**
     * Get the value of numExecutions
     *
     * @return the value of numExecutions
     */
    public int getNumExecutions() {
        return numExecutions;
    }

    /**
     * Set the value of numExecutions
     *
     * @param numExecutions new value of numExecutions
     */
    public void setNumExecutions(int numExecutions) {
        this.numExecutions = numExecutions;
    }

    /**
     * Get the value of lastUsageTime
     *
     * @return the value of lastUsageTime
     */
    public long getLastUsageTime() {
        return lastUsageTime;
    }

    /**
     * Set the value of lastUsageTime
     *
     * @param lastUsageTime new value of lastUsageTime
     */
    public void setLastUsageTime(long lastUsageTime) {
        this.lastUsageTime = lastUsageTime;
    }
    
    // Comparator that orders based upon the number of executions and the last usage time.
    public static Comparator<SQLTrace> SQLTraceFrequencyComparator = new Comparator<SQLTrace>() {
        
        @Override
        public int compare(SQLTrace sqlTrace1, SQLTrace sqlTrace2) {
            final int sqlTrace1Frequency = sqlTrace1.getNumExecutions();
            final int sqlTrace2Frequency = sqlTrace2.getNumExecutions();
            
            // Initialise the comparison variable to be equal
            int compare = 0;
            
            // Compare the number of executions
            if (sqlTrace1Frequency < sqlTrace2Frequency) {
                compare = 1;
            } else if (sqlTrace1Frequency > sqlTrace2Frequency) {
                compare = -1;
            }
            
            // If the number of executions are the same, compare the last usage time
            if (compare == 0) {
                final long sqlTrace1LastUsageTime = sqlTrace1.getLastUsageTime();
                final long sqlTrace2LastUsageTime = sqlTrace2.getLastUsageTime();
                
                if (sqlTrace1LastUsageTime < sqlTrace2LastUsageTime) {
                    compare = 1;
                } else if (sqlTrace1LastUsageTime > sqlTrace2LastUsageTime) {
                    compare = -1;
                }
            }
            
            return compare;
        }
    };
}
