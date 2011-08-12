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

import org.glassfish.j2ee.statistics.Statistic;
import org.glassfish.j2ee.statistics.BoundedRangeStatistic;

/** An implementation of AverageRangeStatistic that provides ways to change the state externally through mutators.
 * Convenience class that is useful for components that gather the statistical data.
 * By merely changing the count (which is a mandatory measurement), rest of the statistical
 * information could be deduced.
 * @author Larry White
 * @author Kedar Mhaswade
 * @see AverageRangeStatisticImpl for an immutable implementation
 * @since S1AS8.1
 * @version 1.0
 */

/**
 *
 * @author  lwhite
 */
public class MutableAverageRangeStatisticImpl implements AverageRangeStatistic, MutableCountStatistic {

    /** DEFAULT_UPPER_BOUND is maximum value Long can attain */
    public static final long DEFAULT_MAX_BOUND = java.lang.Long.MAX_VALUE;
        
    private MutableBoundedRangeStatisticImpl    mutableBoundedRangeStat = null;
    private long                                numberOfSamples;
    private long                                runningTotal;
    private String                              description = null;
    
    /** Constructs an instance of MutableAverageRangeStatisticImpl that encapsulates the given Statistic.
     * The only parameter denotes the initial state of this statistic. It is
     * guaranteed that the initial state is preserved internally, so that one
     * can reset to the initial state.
     * @param initial           an instance of BoundedRangeStatistic that represents initial state
     */
    public MutableAverageRangeStatisticImpl(BoundedRangeStatistic initial) {
        mutableBoundedRangeStat = new MutableBoundedRangeStatisticImpl(initial);
        numberOfSamples = 0L;
        runningTotal = 0L;
        description = initial.getDescription();        
    }
    
    public Statistic modifiableView() {        
        return this;
    }
    
    public Statistic unmodifiableView() {        
        return ( new AverageRangeStatisticImpl(
            this.getCurrent(),               // this is the actual changing statistic
            this.getHighWaterMark(),         // highWaterMark may change per current
            this.getLowWaterMark(),          // lowWaterMark may change per current
            mutableBoundedRangeStat.getUpperBound(),    // upperBound is not designed to change
            mutableBoundedRangeStat.getLowerBound(),    // lowerBound is not designed to change
            mutableBoundedRangeStat.getName(),          // name does not change
            mutableBoundedRangeStat.getUnit(),          // unit does not change
            mutableBoundedRangeStat.getDescription(),   // description does not change
            this.getLastSampleTime(),        // changes all the time!
            this.getStartTime(),             // changes if reset is called earlier
            this.numberOfSamples,       // this is the current number of samples
            this.runningTotal           // this is the current running total
        ));        
    }    

    public void reset() {
        mutableBoundedRangeStat.reset();
        this.resetAverageStats();
    }
    
    private void resetAverageStats() {
        numberOfSamples = 0L;
        runningTotal = 0L;
    }    
    
    public void setCount(long current) {
        mutableBoundedRangeStat.setCount(current);
        if(DEFAULT_MAX_BOUND - runningTotal < current) {
            this.resetAverageStats();
        }
        numberOfSamples++;
        runningTotal += current;
    }
    
    public long getAverage() {
        if(numberOfSamples == 0) {
            return -1;
        } else {
            return runningTotal / numberOfSamples;
        }
    }
    
    public long getCurrent() {
        return mutableBoundedRangeStat.getCurrent();
    }
    
    public String getDescription() {
        return description;
        //return mutableBoundedRangeStat.getDescription();
    }
    
    public long getHighWaterMark() {
        return mutableBoundedRangeStat.getHighWaterMark();
    }
    
    public long getLastSampleTime() {
        return mutableBoundedRangeStat.getLastSampleTime();
    }
    
    public long getLowWaterMark() {
        long result = mutableBoundedRangeStat.getLowWaterMark();
        if(result == DEFAULT_MAX_BOUND) {
            result = 0L;
        }
        return result;
    }
    
    public String getName() {
        return mutableBoundedRangeStat.getName();
    }
    
    public long getStartTime() {
        return mutableBoundedRangeStat.getStartTime();
    }
    
    public String getUnit() {
        return mutableBoundedRangeStat.getUnit();
    }
    
}
