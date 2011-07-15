/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.flashlight.statistics.impl;

import java.util.Collection;
import org.glassfish.flashlight.datatree.TreeNode;
import org.glassfish.flashlight.statistics.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import org.glassfish.flashlight.datatree.impl.AbstractTreeNode;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 *
 * @author Harpreet Singh
 */
@Service(name = "average")
@Scoped(PerLookup.class)
public class AverageImpl extends AbstractTreeNode implements Average {

    /** DEFAULT_UPPER_BOUND is maximum value Long can attain */
    public static final long DEFAULT_MAX_BOUND = java.lang.Long.MAX_VALUE;
    /** DEFAULT_LOWER_BOUND is same as DEFAULT_VALUE i.e. 0 */
    public static final long DEFAULT_VALUE = java.math.BigInteger.ZERO.longValue();
    public static final long DEFAULT_MIN_BOUND = DEFAULT_VALUE;
    /** DEFAULT_VALUE of any statistic is 0 */
    protected static final String NEWLINE = System.getProperty("line.separator");
   
    AtomicLong min = new AtomicLong (DEFAULT_MIN_BOUND);
    AtomicLong max = new AtomicLong(0);
    
    AtomicLong times = new AtomicLong(0);
    AtomicLong sum = new AtomicLong(0);
    private long startTime = 0;
    private AtomicLong lastSampleTime = new AtomicLong(0);
    
    private String NAME = "average";
    private String DESCRIPTION = "Average RangeStatistic";
    private String UNIT = java.lang.Long.class.toString();
    
    public AverageImpl() {
        super.name = NAME;
        super.enabled = true;
        startTime = System.currentTimeMillis();
    }

    /*
     * 
     * TBD: Remove reference to getSampleTime -> see comment on getSampleTime
     * method
     */
    public void addDataPoint(long value) {
        if (min.get() == DEFAULT_MIN_BOUND) // initial seeding
        {
            min.set(value);
        }
        if (value < min.get()) {
            min.set(value);
        } else if (value > max.get()) {
            max.set(value);
        }
        sum.addAndGet(value);
        times.incrementAndGet();
        // TBD: remove this code, once getSampleTime is refactored
        lastSampleTime.set(getSampleTime ());
    }

    public double getAverage() {
        double total = sum.doubleValue();
        double count = times.doubleValue();
        double avg =  total / count;
	return (Double.isNaN(avg) ? 0 : avg);
    }

    public void setReset() {
        times.set(0);
        sum.set(0);

    }

    public long getMin() {
        return min.get();
    }

    public long getMax() {
        return max.get();
    }

    public String toString() {
        return "Statistic " + getClass().getName() + NEWLINE +
                "Name: " + getName() + NEWLINE +
                "Description: " + getDescription() + NEWLINE +
                "Unit: " + getUnit() + NEWLINE +
                //           "LastSampleTime: " + getLastSampleTime() + NEWLINE +
                "StartTime: " + getStartTime();
    }

    @Override
    public Object getValue() {
        return getAverage();
    }

    public long getSize() {
        return times.get();
    }

    public long getHighWaterMark() {
        return getMax();
    }

    public long getLowWaterMark() {
        return getMin();
    }

    public long getCurrent() {
        Double d = new Double(getAverage());
        return d.longValue();
    }

    public String getUnit() {
        return this.UNIT;
    }

    public String getDescription() {
        return this.DESCRIPTION;
    }

    public long getStartTime() {
        return this.startTime;
    }
    public long getTotal (){
        return sum.get();

    }
    /*
     * TBD
     * This is an inefficient implementation. Should schedule a Timer task
     * that gets a timeout event every 30s or so and updates this value
     */
    private long getSampleTime (){
        return System.currentTimeMillis();
   
    }
    public long getLastSampleTime() {
        return this.lastSampleTime.longValue();
    }
}
