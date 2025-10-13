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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package org.glassfish.flashlight.statistics.impl;

import org.glassfish.flashlight.datatree.impl.AbstractTreeNode;
import org.glassfish.flashlight.statistics.*;
import org.glassfish.flashlight.statistics.factory.AverageFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Harpreet Singh
 */
public abstract class TimeStatsAbstractImpl extends AbstractTreeNode implements TimeStats {

    protected static final String NEWLINE = System.lineSeparator();
    
    private final Average average = AverageFactory.createAverage();


    private final AtomicLong lastSampleTime = new AtomicLong(0);
    protected long startTime = 0;

    private final ThreadLocalTimeStatData individualData = new ThreadLocalTimeStatData();

    private static class ThreadLocalTimeStatData extends ThreadLocal<TimeStatData> {
        private TimeStatData tsd;

        @Override
        protected TimeStatData initialValue(){
            tsd = new TimeStatData ();
            return tsd;
        }
        
        @Override
        public TimeStatData get(){
            if (tsd == null)
                tsd = new TimeStatData();
            return tsd;
        }
        
    }

    @Override
    public double getTime() {
        return average.getAverage();
    }

    protected void postEntry(long entryTime) {
        if (startTime == 0) {
            startTime = entryTime;
        }
        this.setLastSampleTime(entryTime);
        individualData.get().setEntryTime(entryTime);
    }

    public void postExit(long exitTime) {
        TimeStatData tsd = individualData.get();
        tsd.setExitTime(exitTime);
        average.addDataPoint(tsd.getTotalTime());
    }

    @Override
    public long getMinimumTime() {
        return average.getMin();
    }

    @Override
    public long getMaximumTime() {
        return average.getMax();
    }

    // only for testing purposes.
    @Override
    public void setTime(long time) {
        individualData.get().setTotalTime(time);
        average.addDataPoint(time);
    }

    @Override
    public void setReset(boolean reset) {
        average.setReset();
        individualData.get().setReset();
    }

    @Override
    public long getTimesCalled() {
        return average.getSize();
    }
    
    // Implementations for TimeStatistic
    @Override
    public long getCount() {
        return getTimesCalled();
    }

    @Override
    public long getMaxTime() {
        return getMaximumTime();
    }

    @Override
    public long getMinTime() {
        return getMinimumTime();
    }

    @Override
    public long getTotalTime() {
        return average.getTotal();
    }

    @Override
    public long getLastSampleTime() {
        return this.lastSampleTime.get();
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    private void setLastSampleTime(long time) {
        this.lastSampleTime.set(time);
    }

    private static class TimeStatData {
        private long entryTime = 0;
        private long exitTime = 0;
        private long totalTime = 0;


        public long getEntryTime() {
            return entryTime;
        }

        public void setEntryTime(long entryTime) {
            this.entryTime = entryTime;
        }

        public long getExitTime() {
            return exitTime;
        }

        public void setExitTime(long exitTime) {
            this.exitTime = exitTime;
        }

        public long getTotalTime() {
            totalTime = exitTime - entryTime;
            return totalTime;
        }

        public void setTotalTime(long totalTime) {
            this.totalTime = totalTime;
        }
        public void setReset (){
            entryTime = 0;
            exitTime = 0;
            totalTime = 0;
        }
    }
}
