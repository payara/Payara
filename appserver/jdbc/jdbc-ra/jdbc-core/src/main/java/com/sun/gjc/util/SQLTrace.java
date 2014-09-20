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

/**
 * Store the sql queries executed by applications along with the number of
 * times executed and the time stamp of the last usage.
 * Used for monitoring information.
 * 
 * @author Shalini M
 */
public class SQLTrace implements Comparable {

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

    /**
     * Check for equality of the SQLTrace with the object passed by
     * comparing the queryName stored.
     *
     * @param obj against which the equality is to be checked.
     * @return true or false
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if(!(obj instanceof SQLTrace)) {
            return false;
        }
        final SQLTrace other = (SQLTrace) obj;
        if ((this.queryName == null) || (other.queryName == null) ||
                !this.queryName.equals(other.queryName)) {
            return false;
        }
        return true;
    }

    /**
     * Generate hash code for this obejct using all the fields.
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + (this.queryName != null ? this.queryName.hashCode() : 0);
        return hash;
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof SQLTrace)) {
            throw new ClassCastException("SqlTraceCache object is expected");
        }
        int number = ((SQLTrace) o).numExecutions;
        long t = ((SQLTrace) o).getLastUsageTime();

        int compare = 0;
        if (number == this.numExecutions) {
            compare = 0;
        } else if (number < this.numExecutions) {
            compare = -1;
        } else {
            compare = 1;
        }
        if (compare == 0) {
            //same number of executions. Hence compare based on time.
            long timeCompare = this.getLastUsageTime() - t;
            if(timeCompare == 0) {
                compare = 0;
            } else if(timeCompare < 0) {
                //compare = -1;
                compare = 1;
            } else {
                //compare = 1;
                compare = -1;
            }
        }
        return compare;
    }
}
