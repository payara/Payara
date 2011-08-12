/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.monitor.stats;
import org.glassfish.j2ee.statistics.Stats;
import org.glassfish.j2ee.statistics.RangeStatistic;
import org.glassfish.j2ee.statistics.CountStatistic;

/** 
 * A Stats interface to represent the statistical data exposed by a Connection 
 * Pool. All the Connection Pool implementations should expose statistical data 
 * by implementing this interface.
 */

public interface ConnectionPoolStats extends Stats {
    
    /**
     * Statistic to represent the Connection Usage
     * In addition to information about the number of connections being
     * used currently, this also contains information about the 
     * Maximum number of connections that were used(High Watermark)
     * @return RangeStatistic
     */
    public RangeStatistic getNumConnUsed();
    
    /*
     * represents the number of free connections in the pool.
     * @return CountStatistic
     */
    //public CountStatistic getNumConnFree() ;
    
    /**
     * represents the number of connections that failed validation
     * @return CountStatistic
     */
    public CountStatistic getNumConnFailedValidation() ;
   
    /**
     * represents the number of connection requests that timed out
     * @return CountStatistic
     */
    public CountStatistic getNumConnTimedOut();
	
	/**
     * Indicates the number of free connections in the pool in addition
	 * to their high and low watermarks.
     * @return RangeStatistic
	 */
	 public RangeStatistic getNumConnFree();
     
	
	/**
	 * Indicates the average wait time of connections, for successful
	 * connection request attempts to the connector connection pool
	 * @return CountStatistic
	 */
	public CountStatistic getAverageConnWaitTime();
	
	/**
	 * Indicates the number of connection requests in the queue waiting 
	 * to be serviced
	 * @return CountStatistic
	 */
	public CountStatistic getWaitQueueLength();
	
	/**
	 * Indicates the longest, shortest wait times of connection 
	 * requests. The current value indicates the wait time of 
	 * the last request that was serviced by the pool.
	 * @return RangeStatistic
	 */
	public RangeStatistic getConnRequestWaitTime();
	
	/** 
	 * indicates the number of physical EIS/JDBC connections that were created, 
     * since the last reset
	 * @return CountStatistic
	 */
	public CountStatistic getNumConnCreated();
	
	/**
	 * indicates the number of physical EIS/JDBC connections that were destroyed
     * , since the last reset
	 * @return CountStatistic
	 */
	public CountStatistic getNumConnDestroyed();

    /**
     * indicates the number of logical EIS/JDBC connections that were acquired 
     * from the pool, since the last reset
     * @return CountStatistic
     * @since 8.1
     */
    public CountStatistic getNumConnAcquired();
    
    /**
     * indicates the number of logical EIS/JDBC connections that were released 
     * to the pool, since the last reset
     * @return CountStatistic
     * @since 8.1
     */
    public CountStatistic getNumConnReleased();      

    /**
     * Indicates the number of connections that were successfully matched by 
     * the Managed Connection Factory.
     * 
     * @return CountStatistic
     * @since 9.0
     */
    public CountStatistic getNumConnSuccessfullyMatched();
 
    /**
     * Indicates the number of connections that were rejected by the
     * Managed Connection Factory during matching.
     * 
     * @return CountStatistic
     * @since 9.0
     */
    public CountStatistic getNumConnNotSuccessfullyMatched(); 
    
    /**
     * Indicates the number of potential connection leaks
     * 
     * @return CountStatistic
     * @since 9.1
     */
    public CountStatistic getNumPotentialConnLeak();

}
