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
import com.sun.enterprise.util.i18n.StringManager;

/** An implementation of a TimeStatistic. All instances of this class are
 * immutable. Provides all the necessary accessors for properties.
 * @author Muralidhar Vempaty
 * @author Kedar Mhaswade
 * @since S1AS8.0
 */

public class TimeStatisticImpl extends StatisticImpl implements TimeStatistic {
    
    private final long count;
    private final long maxTime;
    private final long minTime;
    private final long totTime;
    private static final StringManager localStrMgr = 
                StringManager.getManager(TimeStatisticImpl.class);

    public final String toString() {
        return super.toString() + NEWLINE + 
            "Count: " + getCount() + NEWLINE +
            "MinTime: " + getMinTime() + NEWLINE +
            "MaxTime: " + getMaxTime() + NEWLINE +
            "TotalTime: " + getTotalTime();
    }

	public TimeStatisticImpl(String name) {
		this(name, StatisticImpl.DEFAULT_UNIT);
	}
	public TimeStatisticImpl(String name, String unit) {
		this(StatisticImpl.DEFAULT_VALUE, StatisticImpl.DEFAULT_VALUE, 
			StatisticImpl.DEFAULT_VALUE, StatisticImpl.DEFAULT_VALUE, name,  unit,
			Util.getDescriptionFromName(name), Util.getInitTime()[0], Util.getInitTime()[1]);
	}

        /** Constructs an immutable instance of TimeStatistic.
     * @param name  The name of the statistic
     * @param unit  The unit of measurement for this statistic
     * @param desc  A brief description of the statistic
	 */
    public TimeStatisticImpl(String name, String unit, String desc) {
		this(StatisticImpl.DEFAULT_VALUE, StatisticImpl.DEFAULT_VALUE, 
			StatisticImpl.DEFAULT_VALUE, StatisticImpl.DEFAULT_VALUE, name,  unit,
			desc, Util.getInitTime()[0], Util.getInitTime()[1]);
        
    }

    /** Constructs an immutable instance of TimeStatistic.
     * @deprecated use the other TimeStatisticImpl constructors.
     *             Counter, maxtime, mintime, totaltime, starttime
     *              last sampletime are automatically calculated
     *              at the first measurement.
     * @param counter   The number of times an operation has been invoked since
     *                  measurement started
     * @param maximumTime   The maximum time it took to complete one invocation 
     *                      of an operation, since the measurement started 
     * @param minimumTime   The minimum time it took to complete one invocation
     *                      of an opeation, since the measurement started 
     * @param totalTime     The total amount of time spent in all invocations, 
     *                      over the duration of the measurement 
     * @param name  The name of the statistic
     * @param unit  The unit of measurement for this statistic
     * @param desc  A brief description of the statistic
     * @param startTime Time in milliseconds at which the measurement was started
     * @param sampleTime    Time at which the last measurement was done.
	 */
     public TimeStatisticImpl(long counter, long maximumTime, long minimumTime,
                              long totalTime, String name, String unit, 
                             String desc, long startTime, long sampleTime) {
        
        super(name, unit, desc, startTime, sampleTime);
        count = counter;
        maxTime = maximumTime;
        minTime = minimumTime;
        totTime = totalTime;
    }
    
    /**
     * Returns the number of times an operation was invoked 
     * @return long indicating the number of invocations 
     */
    public long getCount() {
        return count;
    }
    
    /**
     * Returns the maximum amount of time that it took for one invocation of an
     * operation, since measurement started.
     * @return long indicating the maximum time for one invocation
     */
    public long getMaxTime() {
        return maxTime;
    }
    
    /**
     * Returns the minimum amount of time that it took for one invocation of an
     * operation, since measurement started.
     * @return long indicating the minimum time for one invocation 
     */
    public long getMinTime() {
        return minTime;
    }    

    /**
     * Returns the amount of time that it took for all invocations, 
     * since measurement started.
     * @return long indicating the total time for all invocation 
     */
    public long getTotalTime() {
        return totTime;
    }
	
	private static class Util {
		/** A method to get the description from a name. Can be simple property file
		 * pair reader. Note that name is invariant, whereas the descriptions are
		 * localizable.
		 */
		private static String getDescriptionFromName(String name) {
			return (localStrMgr.getString("describes_string")  + name);
		}

		/** Returns an array of two longs, that represent the times at the time of call.
		 * The idea is not to call expensive System#currentTimeMillis twice for two
		 * successive operations.
		 */
		private static long[] getInitTime() {
			final long time = System.currentTimeMillis();
			return ( new long[]{time, time} );
		}
	}	
}
