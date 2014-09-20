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

package com.sun.ejb.base.stats;

import org.glassfish.j2ee.statistics.CountStatistic;
import org.glassfish.j2ee.statistics.RangeStatistic;
import org.glassfish.j2ee.statistics.TimeStatistic;

import com.sun.ejb.spi.stats.MonitorableSFSBStoreManager;

import com.sun.enterprise.admin.monitor.stats.AverageRangeStatistic;
import com.sun.enterprise.admin.monitor.stats.BoundedRangeStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.CountStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableAverageRangeStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableBoundedRangeStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableCountStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableTimeStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.TimeStatisticImpl;

/**
 * Implementation of StatefulSessionStoreStats
 *  There is once instance of this class per StatefulEJBContainer
 *
 * @author Mahesh Kannan
 */

public class HAStatefulSessionStoreStatsImpl
    extends StatefulSessionStoreStatsImpl
    implements com.sun.enterprise.admin.monitor.stats.HAStatefulSessionStoreStats
{

    private MutableCountStatisticImpl		checkpointCount;
    private MutableCountStatisticImpl		checkpointSuccessCount;
    private MutableCountStatisticImpl		checkpointErrorCount;
    private MutableAverageRangeStatisticImpl	checkpointSize;
    private MutableAverageRangeStatisticImpl	checkpointTime;

    private Object checkpointCountLock = new Object();
    private Object checkpointSizeLock = new Object();
    private Object checkpointTimeLock = new Object();

    private long    checkpointCountVal;
    private long    checkpointSuccessCountVal;
    private long    checkpointErrorCountVal;

    public HAStatefulSessionStoreStatsImpl(
	MonitorableSFSBStoreManager provider)
    {
	super(provider, "com.sun.enterprise.admin.monitor.stats.HAStatefulSessionStoreStats");
	initialize();
    }

    protected void initialize() {
	super.initialize();

	synchronized (checkpointCountLock) {
	    checkpointCount = new MutableCountStatisticImpl(
		new CountStatisticImpl("CheckpointCount"));
	    checkpointSuccessCount = new MutableCountStatisticImpl(
		new CountStatisticImpl("CheckpointSuccessCount"));
	    checkpointErrorCount = new MutableCountStatisticImpl(
		new CountStatisticImpl("CheckpointErrorCount"));
	}

	synchronized (checkpointTimeLock) {
	    checkpointTime = new MutableAverageRangeStatisticImpl(
	    	new BoundedRangeStatisticImpl(0, 0, 0,
                                     0, 0, "CheckpointTime",
                                     "millis", "Time spent on checkpointing", 0, 0)
	    );
	}

	synchronized (checkpointSizeLock) {
	    checkpointSize = new MutableAverageRangeStatisticImpl(
	    	new BoundedRangeStatisticImpl(0, 0, 0,
                                     0, 0, "CheckpointSize",
                                     "millis", "Number of bytes checkpointed", 0, 0)
	    );
	}
    }

    /**
     * Returns the total number of sessions checkpointed into the store
     */
    public CountStatistic getCheckpointCount() {
	synchronized (checkpointCountLock) {
	    checkpointCount.setCount(checkpointCountVal);
	   return (CountStatistic) checkpointCount.unmodifiableView();
	}
    }

    /**
     * Returns the total number of sessions successfully Checkpointed into the store
     */
    public CountStatistic getCheckpointSuccessCount() {
	synchronized (checkpointCountLock) {
	    checkpointSuccessCount.setCount(checkpointSuccessCountVal);
	    return (CountStatistic) checkpointSuccessCount.unmodifiableView();
	}
    }

    /**
     * Returns the total number of sessions that couldn't be Checkpointed into the store
     */
    public CountStatistic getCheckpointErrorCount() {
	synchronized (checkpointCountLock) {
	    checkpointErrorCount.setCount(checkpointErrorCountVal);
	    return (CountStatistic) checkpointErrorCount.unmodifiableView();
	}
    }

    /**
     * Returns the number of bytes checkpointed
     */
    public AverageRangeStatistic getCheckpointedBeanSize() {
	synchronized (checkpointTimeLock) {
	    return (AverageRangeStatistic) checkpointSize.unmodifiableView();
	}
    }

    /**
     * Returns the time spent on passivating beans to the store including total, min, max
     */
    public AverageRangeStatistic getCheckpointTime() {
	synchronized (checkpointTimeLock) {
	    return (AverageRangeStatistic) checkpointTime.unmodifiableView();
	}
    }

    public void incrementCheckpointCount(boolean success) {
	synchronized (checkpointCountLock) {
	    checkpointCountVal++;
	    if (success) {
		checkpointSuccessCountVal++;
	    } else {
		checkpointErrorCountVal++;
	    }
	}     
    }

    public void setCheckpointSize(long val) {
	synchronized (checkpointSizeLock) {
	    checkpointSize.setCount(val);
	}
    }

    public void setCheckpointTime(long val) {
	synchronized (checkpointTimeLock) {
	    checkpointTime.setCount(val);
	}
    }

    protected void appendStats(StringBuffer sbuf) {
	super.appendStats(sbuf);
	sbuf.append("CheckpointCount: ").append(checkpointCountVal)
	    .append("; ")
	    .append("CheckpointSuccessCount: ").append(checkpointSuccessCountVal)
	    .append("; ")
	    .append("CheckpointErrorCount: ").append(checkpointErrorCountVal)
	    .append("; ");

	appendTimeStatistic(sbuf, "CheckpointTime", checkpointTime);
    }

}
