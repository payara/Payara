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
import org.glassfish.j2ee.statistics.RangeStatistic;

/** An implementation of a RangeStatistic. All instances of this class are
 * immutable. Provides all the necessary accessors for properties.
 * @author Muralidhar Vempaty
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @verison 1.0
 */

public final class RangeStatisticImpl extends StatisticImpl implements RangeStatistic {
    
    private final long currentVal;
    private final long highWaterMark;
    private final long lowWaterMark;
    
    /** Constructs an immutable instance of RangeStatistic.
     * @param curVal    The current value of this statistic
     * @param highMark  The highest value of this statistic, since measurement 
     *                  started
     * @param lowMark   The lowest value of this statistic, since measurement
     *                  started
     * @param name      The name of the statistic
     * @param unit      The unit of measurement for this statistic
     * @param desc      A brief description of the statistic
     * @param startTime Time in milliseconds at which the measurement was started
     * @param sampleTime Time at which the last measurement was done.
	 */
    public RangeStatisticImpl(long curVal, long highMark, long lowMark, 
                              String name, String unit, String desc, 
                              long startTime, long sampleTime) {
        
        super(name, unit, desc, startTime, sampleTime);
        currentVal = curVal;
        highWaterMark = highMark;
        lowWaterMark = lowMark;
    }
    
    /**
     * Returns the current value of this statistic.
	 * @return long indicating the current value
     */
    public long getCurrent() {
        return currentVal;
    }
    
    /**
     * Returns the highest value of this statistic, since measurement started.
	 * @return long indicating high water mark
     */
    public long getHighWaterMark() {
        return highWaterMark;
    }
    
    /**
     * Returns the lowest value of this statistic, since measurement started.
	 * @return long indicating low water mark
     */
    public long getLowWaterMark() {
        return lowWaterMark;
    }
    
    public final String toString() {
        return super.toString() + NEWLINE + 
            "Current: " + getCurrent() + NEWLINE +
            "LowWaterMark: " + getLowWaterMark() + NEWLINE +
            "HighWaterMark: " + getHighWaterMark();
    }

}
