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
import com.sun.enterprise.admin.monitor.stats.BoundedRangeStatisticImpl;
import org.glassfish.j2ee.statistics.Statistic;

/** An implementation of MutableCountStatistic that provides ways to change the state externally through mutators.
 * Convenience class that is useful for components that gather the statistical data.
 * By merely changing the count (which is a mandatory measurement), rest of the statistical
 * information could be deduced.
 * @author Kedar Mhaswade
 * @see BoundedRangeStatisticImpl for an immutable implementation
 * @since S1AS8.0
 * @version 1.0
 */

public class MutableBoundedRangeStatisticImpl implements BoundedRangeStatistic, MutableCountStatistic {
    
    private final BoundedRangeStatistic     initial;
    private long                            current;
    private long                            lastSampleTime;
    private long                            startTime;
    private long                            lowWaterMark;
    private long                            highWaterMark;

    /** Constructs an instance of MutableCountStatistic that encapsulates the given Statistic.
     * The only parameter denotes the initial state of this statistic. It is
     * guaranteed that the initial state is preserved internally, so that one
     * can reset to the initial state.
     * @param initial           an instance of BoundedRangeStatistic that represents initial state
     */
    public MutableBoundedRangeStatisticImpl(BoundedRangeStatistic initial) {
        this.initial        = initial;
        this.current        = initial.getCurrent();
        this.lastSampleTime = initial.getLastSampleTime();
        this.startTime      = initial.getStartTime();
        this.lowWaterMark   = initial.getLowWaterMark();
        this.highWaterMark  = initial.getHighWaterMark();
    }
    
    /** Resets to the initial state. It is guaranteed that following changes occur
     * to the statistic if this method is called:
     * <ul>
     *  <li> The current value is reset to initial value. </li>
     *  <li> The lastSampleTime is reset to <b> current time in milliseconds. </b> </li>
     *  <li> The startTime is reset to lastSampleTime. </li>
     *  <li> The highWaterMark is reset to the initial value. </li>
     *  <li> The lowWaterMark is reset to the initial value. </li>
     * </ul>
     * The remaining meta data in the encapsulated statistic is unchanged. The
     * upper and lower bounds are untouched.
    */
    public void reset() {
        this.current                = initial.getCurrent();
        this.lastSampleTime         = System.currentTimeMillis();
        this.startTime              = this.lastSampleTime;
        this.highWaterMark          = initial.getHighWaterMark();
        this.lowWaterMark           = initial.getLowWaterMark();
    }
    
    /** Changes the current value of the encapsulated BoundedRangeStatistic to the given value.
     * Since this is the only mutator exposed here, here are the other side effects
     * of calling this method:
     * <ul>
     *  <li> lastSampleTime is set to <b> current time in milliseconds. </b> </li>
     *  <li> highWaterMark is accordingly adjusted. </li>
     *  <li> lowWaterMark is accordingly adjusted. </li>
     * </ul> 
     * In a real-time system with actual probes for measurement, the lastSampleTime
     * could be different from the instant when this method is called, but that is deemed insignificant.
     * @param count         long that represents the current value of the Statistic.
     */
    public void setCount(long current) {
        this.current            = current;
        this.lastSampleTime     = System.currentTimeMillis();
        
        this.lowWaterMark   = (current < this.lowWaterMark) ? (current) : (this.lowWaterMark);
        this.highWaterMark  = (current > this.highWaterMark) ? (current) : (this.highWaterMark);
	this.lastSampleTime = System.currentTimeMillis();
    }
    
    /** This method is the essence of this class. It provides the read-only view of encapsulated
     * Statistic. If the clients have to know the Statistic, this is what should
     * be called by actual data collecting component to return the value to them.
     * The principle advantage is from the data collecting component's standpoint, in 
     * that it does not have to create instances of BoundedRangeStatistic when its
     * current value is queried/measured.
     * @see #reset
     * @see #setCount
     * @return      instance of BoundedRangeStatistic
     */
    public Statistic unmodifiableView() {
        return ( new BoundedRangeStatisticImpl(
            this.current,               // this is the actual changing statistic
            this.highWaterMark,         // highWaterMark may change per current
            this.lowWaterMark,          // lowWaterMark may change per current
            initial.getUpperBound(),    // upperBound is not designed to change
            initial.getLowerBound(),    // lowerBound is not designed to change
            initial.getName(),          // name does not change
            initial.getUnit(),          // unit does not change
            initial.getDescription(),   // description does not change
            this.startTime,              // changes if reset is called earlier
            this.lastSampleTime        // changes all the time!
        ));
    }
    
    public String getDescription() {
	return ( initial.getDescription());
    }
    
    public long getLastSampleTime() {
	return ( this.lastSampleTime );
    }
    
    public String getName() {
	return ( initial.getName() );
    }
    
    public long getStartTime() {
	return ( initial.getStartTime() );
    }
    
    public String getUnit() {
	return ( initial.getUnit() );
    }
    
    public Statistic modifiableView() {
	return ( this );
    }
    
    public long getCurrent() {
	return ( this.current );
    }
    
    public long getHighWaterMark() {
	return ( this.highWaterMark );
    }
    
    public long getLowWaterMark() {
	return ( this.lowWaterMark );
    }
    
    public long getLowerBound() {
	return ( initial.getLowerBound() );
    }
    
    public long getUpperBound() {
	return ( initial.getUpperBound() );
    }
    /* hack: bug 5045413 */
    public void setDescription (final String s) {
        try {
            ((StatisticImpl)this.initial).setDescription(s);
        }
        catch(final Exception e) {
        }
    }
}
