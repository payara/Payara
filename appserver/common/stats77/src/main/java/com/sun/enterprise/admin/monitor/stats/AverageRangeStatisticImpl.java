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

import org.glassfish.j2ee.statistics.BoundedRangeStatistic;

/** An implementation of a AverageRangeStatistic. All instances of this class are
 * immutable. Provides all the necessary accessors for properties.
 * @author Larry White
 * @author Kedar Mhaswade
 * @since S1AS8.1
 * @version 1.0
 */

/**
 *
 * @author  lwhite
 */
public class AverageRangeStatisticImpl implements 
	AverageRangeStatistic /*BoundedRangeStatistic*/ {
    
    private BoundedRangeStatisticImpl boundedRangeStatistic = null;
    private long                                numberOfSamples;
    private long                                runningTotal;    
    
    
	/**
     * Constructs an immutable instance of AverageRangeStatisticImpl.
     * @param curVal    The current value of this statistic
     * @param highMark  The highest value of this statistic, since measurement 
     *                  started
     * @param lowMark   The lowest value of this statistic, since measurement
     *                  started
     * @param upper     The upper limit of this statistic
     * @param lower     The lower limit of this statistic
     * @param name      The name of the statistic
     * @param unit      The unit of measurement for this statistic
     * @param desc      A brief description of the statistic
     * @param startTime Time in milliseconds at which the measurement was started
     * @param sampleTime Time at which the last measurement was done.
     * @param numberOfSamples number of samples at present
     * @param runningTotal running total of sampled data at present
     **/    
	public AverageRangeStatisticImpl(long curVal, long highMark, long lowMark,
                                     long upper, long lower, String name,
                                     String unit, String desc, long startTime,
                                     long sampleTime, long numberOfSamples,
                                     long runningTotal) {
                                         
        boundedRangeStatistic = new BoundedRangeStatisticImpl(curVal, highMark, lowMark,
                                     upper, lower, name,
                                     unit, desc, startTime,
                                     sampleTime);                                 
                                         
        this.numberOfSamples = numberOfSamples;
        this.runningTotal = runningTotal;
    } 
        
	/**
     * Constructs an immutable instance of AverageRangeStatisticImpl.
     * @param stats a BoundedRangeStatisticImpl
     * @param numberOfSamples number of samples at present
     * @param runningTotal running total of sampled data at present
     **/    
	public AverageRangeStatisticImpl(BoundedRangeStatisticImpl stats, 
                long numberOfSamples, long runningTotal) {                                         
        boundedRangeStatistic = stats;                                                                          
        this.numberOfSamples = numberOfSamples;
        this.runningTotal = runningTotal;
    }         
   
    public long getCurrent() {
        return boundedRangeStatistic.getCurrent();
    }

    public String getDescription() {
        return boundedRangeStatistic.getDescription();
    }

    public long getHighWaterMark() {
        return boundedRangeStatistic.getHighWaterMark();
    }

    public long getLastSampleTime() {
        return boundedRangeStatistic.getLastSampleTime();
    }

    public long getLowWaterMark() {
        return boundedRangeStatistic.getLowWaterMark();
    }
/*
    public long getLowerBound() {
        return boundedRangeStatistic.getLowerBound();
    }
     */

    public String getName() {
        return boundedRangeStatistic.getName();
    }

    public long getStartTime() {
        return boundedRangeStatistic.getStartTime();
    }

    public String getUnit() {
        return boundedRangeStatistic.getUnit();
    }
/*
    public long getUpperBound() {
        return boundedRangeStatistic.getUpperBound();
    }
     */
        
    public long getAverage() {
        if(numberOfSamples == 0) {
            return -1;
        } else {
            return runningTotal / numberOfSamples;
        }        
    }
    /** This is a hack. This method allows us to internatinalize the descriptions.
        See bug Id: 5045413
    */
    public void setDescription(final String desc) {
        this.boundedRangeStatistic.setDescription(desc);
    }
    
}
