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
import org.glassfish.j2ee.statistics.TimeStatistic;
import org.glassfish.j2ee.statistics.Statistic;


/** An implementation of {@link MutableTimeStatistic} that eases the various
 * statistical calculations.
 * @author  <a href="mailto:Kedar.Mhaswade@sun.com">Kedar Mhaswade</a>
 * @since S1AS8.0
 * @version $Revision: 1.2 $
 */
public class MutableTimeStatisticImpl implements TimeStatistic, MutableTimeStatistic {
	
	private final TimeStatistic initial;
	private long methodCount;
	private long min;
	private long max;
	private long total; //possibility of an overflow?
	private long lastSampleTime;
	/**
	 * Constructs an instance of this class from its immutable equivalent. Note that there are
	 * some constraints on the parameter passed:
	 * <ul>
	 *  <li> The maxTime, minTime and totTime of param must be same </li>
	 * </ul>
	 * @param       instance of (immutable) {@link TimeStatistic}
	 */
	public MutableTimeStatisticImpl(TimeStatistic initial) {
		this.initial        = initial;
		methodCount         = initial.getCount();
		min    = initial.getMinTime();
		max    = initial.getMaxTime();
		total  = initial.getTotalTime();
		final boolean minMax = min == max;
		final boolean minTot = min == total;
		if (! (minMax && minTot))
			throw new IllegalArgumentException("Invalid initial values: " + min + ", " + max + ", " + total);
		lastSampleTime = initial.getLastSampleTime();
	}
	
	/**
	 * Increments the count of operation execution by 1 and also increases the time
	 * consumed. A successful execution of method will have all the data updated as:
	 * <ul>
	 * <li> method count ++ </li>
	 * <li> max time, min time and total time are accordingly adjusted </li>
	 * </ul>
	 * @param       current     long indicating time in whatever unit this statistic is calculated
	 */
	public void incrementCount(long current) {
        if (methodCount == 0) {
            total = max = min = current;
        } else {
            total += current;
            max = current >= max ? current : max;
            min = current >= min ? min : current;
        }
		methodCount++;
		lastSampleTime = System.currentTimeMillis();
	}
	
	/**
	 * Resets the Statistic. Calling this method has following effect:
	 * <ul>
	 * <li> Initial state of this Statistic is restored as far as Count, Minimum/Maximum
	 * and Total time of execution is considered. </li>
	 * </ul>
	 */
	public void reset() {
		methodCount         = initial.getCount();
		min                 = initial.getMinTime();
		max                 = initial.getMaxTime();
		total               = initial.getTotalTime();
		lastSampleTime	    = initial.getLastSampleTime();
	}
	
	/**
	 * This method is the essence of this class. Returns the unmodifiable view
	 * of this instance.
	 * @return an instance of {@link TimeStatistic}
	 */
	public Statistic unmodifiableView() {
		return ( new TimeStatisticImpl(
		this.methodCount,
		this.max,
		this.min,
		this.total,
		initial.getName(),
		initial.getUnit(),
		initial.getDescription(),
		initial.getStartTime(),
		this.lastSampleTime )
		);
	}
	
	public Statistic modifiableView() {
		return ( this );
	}
	
	public long getCount() {
		return ( this.methodCount);
	}
	
	public String getDescription() {
		return ( initial.getDescription() );
	}
	
	public long getLastSampleTime() {
		return ( this.lastSampleTime );
	}
	
	public long getMaxTime() {
		return ( this.max );
	}
	
	public long getMinTime() {
		return ( this.min );
	}
	
	public String getName() {
		return ( initial.getName() );
	}
	
	public long getStartTime() {
		return ( initial.getStartTime() );
	}
	
	public long getTotalTime() {
		return ( this.total );
	}
	
	public String getUnit() {
		return ( initial.getUnit() );
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
