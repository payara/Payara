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
import java.io.Serializable;
import com.sun.enterprise.util.i18n.StringManager;

/**
 * An abstract class providing implementation of the Statistic interface
 * The intent is for this to be subclassed by all the StatisticImpls.
 * @author Muralidhar Vempaty
 * @since S1AS8.0
 * @version 1.0
 */

public abstract class StatisticImpl implements Statistic,Serializable {
    
    private String statisticName;
    private String statisticDesc;
    private String statisticUnit;
    private long startTime;
    private long sampleTime;
    
	/** DEFAULT_UNIT is an empty string */
	public static final String	DEFAULT_UNIT;
    public static final StringManager localStrMgr;
	/** DEFAULT_VALUE of any statistic is 0 */
	public static final long	DEFAULT_VALUE	= java.math.BigInteger.ZERO.longValue();

    static {
        localStrMgr = StringManager.getManager(StatisticImpl.class);
        DEFAULT_UNIT = localStrMgr.getString("count_string");
    }

    protected static final String NEWLINE = System.getProperty( "line.separator" );
    /** 
     * Constructor
     * @param name      The name of the statistic
     * @param unit      The unit of measurement for this statistic
     * @param desc      A brief description of the statistic
     * @param startTime Time in milliseconds at which the measurement was started
     * @param sampleTime Time at which the last measurement was done.
     **/
    protected StatisticImpl(String name, String unit, String desc, 
                          long start_time, long sample_time) {
        
        statisticName = name;
        statisticUnit = unit;
        statisticDesc = desc;
        startTime = start_time;
        sampleTime = sample_time;
    }
    
    /**
     * returns the name of the statistic
     */
    public String getName() {
        return this.statisticName;
    }
    
    /**
     * returns the description of the statistic
     */
    public String getDescription() {
        return this.statisticDesc;
    }
    
    /**
     * returns the unit of measurement for the statistic
     */
    public String getUnit() {
        return this.statisticUnit;
    }
    
    /**
     * returns the time in millis, at which the last measurement was taken
     */
    public long getLastSampleTime() {
        return this.sampleTime;
    }
    
    /**
     * returns the time in millis, at which the first measurement was taken
     */
    public long getStartTime() {
        return this.startTime;
    }

    /** This is a hack. This method allows us to internatinalize the descriptions.
        See bug Id: 5045413
    */
    public void setDescription(final String desc) {
        this.statisticDesc = desc;
    }
    
    public String toString() {
        return "Statistic " + getClass().getName() + NEWLINE +
            "Name: " + getName() + NEWLINE +
            "Description: " + getDescription() + NEWLINE +
            "Unit: " + getUnit() + NEWLINE +
            "LastSampleTime: " + getLastSampleTime() + NEWLINE +
            "StartTime: " + getStartTime();
    }
}






