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
import org.glassfish.j2ee.statistics.CountStatistic;
import com.sun.enterprise.util.i18n.StringManager;

/** An implementation of a CountStatistic. All instances of this class are
 * immutable. Provides all the necessary accessors for properties.
 * @author Muralidhar Vempaty
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @verison 1.0
 */

public class CountStatisticImpl extends StatisticImpl implements CountStatistic {
    
    private long count;
    private static final StringManager localStrMgr = 
                StringManager.getManager(CountStatisticImpl.class);

	/**
	 * Constructs an instance of this class with following default values:
	 * <ul>
	 *	<li> Unit is empty string. </li>
	 *	<li> Current Value is StatisticImpl#DEFAULT_VALUE. </li>
	 *	<li> Description is calculated from the name passed in. This may well be read from a properties file to address i18n. </li>
	 *	<li> LastSampleTime is time at the time of calling this method.</li>
	 *	<li> StartTime is the same as LastSampleTime. </li>
	 * </ul>
	 * @param	name			String indicating the name of the statistic
	 */
	public CountStatisticImpl(String name) {
		this(name, DEFAULT_UNIT);
	}
	/**
	 * Constructs an instance of this class with following default values:
	 * <ul>
	 *	<li> Current Value is StatisticImpl#DEFAULT_VALUE. </li>
	 *	<li> Description is calculated from the name passed in. This may well be read from a properties file to address i18n. </li>
	 *	<li> LastSampleTime is time at the time of calling this method.</li>
	 *	<li> StartTime is the same as LastSampleTime. </li>
	 * </ul>
	 * @param	name			String indicating the name of the statistic
	 * @param	unit			String indicating the unit of the statistic
	 */
	public CountStatisticImpl(String name, String unit) {
		this(name, unit, DEFAULT_VALUE);
	}
    /**
	 * Constructs an instance of this class with following default values:
	 * <ul>
	 *	<li> Description is calculated from the name passed in. This may well be read from a properties file to address i18n. </li>
	 *	<li> LastSampleTime is time at the time of calling this method.</li>
	 *	<li> StartTime is the same as LastSampleTime. </li>
	 * </ul>
	 * @param	name			String indicating the name of the statistic
	 * @param	unit			String indicating the unit of the statistic
     * @param   desc            A brief description of the statistic
	 */
	public CountStatisticImpl(String name, String unit, String desc) {
		this(DEFAULT_VALUE, name, unit, desc, Util.getInitTime()[0], Util.getInitTime()[1]);
	}
	/**
	 * Constructs an instance of this class with following default values:
	 * <ul>
	 *	<li> Description is calculated from the name passed in. This may well be read from a properties file to address i18n. </li>
	 *	<li> LastSampleTime is time at the time of calling this method.</li>
	 *	<li> StartTime is the same as LastSampleTime. </li>
	 * </ul>
	 * @param	name			String indicating the name of the statistic
	 * @param	unit			String indicating the unit of the statistic
	 * @param	value			long indicating the unit of the statistic
	 */
	public CountStatisticImpl(String name, String unit, long value) {
		this(value, name, unit, Util.getDescriptionFromName(name), Util.getInitTime()[0], Util.getInitTime()[1]);
	}

	/** Constructs an immutable instance of CountStatistic with given parameters.
     * @param curVal    The current value of this statistic
     * @param name      The name of the statistic
     * @param unit      The unit of measurement for this statistic
     * @param desc      A brief description of the statistic
     * @param startTime Time in milliseconds at which the measurement was started
     * @param sampleTime Time at which the last measurement was done.
     **/
    public CountStatisticImpl(long countVal, String name, String unit, 
                              String desc, long sampleTime, long startTime) {
        
        super(name, unit, desc, startTime, sampleTime);
        count = countVal; 
    }
    
    public String toString() {
        return super.toString() + NEWLINE + "Count: " + getCount();
    }


    /**
     * Returns the current value of this statistic.
	 * @return long indicating current value
     */
    public long getCount() {
        return count;
    }
	
	private static class Util {
		/** A method to get the description from a name. Can be simple property file
		 * pair reader. Note that name is invariant, whereas the descriptions are
		 * localizable.
		 */
		private static String getDescriptionFromName(String name) {
			return (localStrMgr.getString("describes_string") + name);
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
