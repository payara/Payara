/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.j2ee.statistics.TimeStatistic;

import com.sun.ejb.spi.stats.MonitorableSFSBStoreManager;

import com.sun.enterprise.admin.monitor.stats.CountStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableCountStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableTimeStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.TimeStatisticImpl;

/**
 * An instance of this class is used by the StatefulContainer to update monitoring 
 *  data. There is once instance of this class per StatefulEJBContainer
 *
 * @author Mahesh Kannan
 */

public class StatefulSessionStoreMonitor {

    private StatefulSessionStoreStatsImpl statsImpl;

    void setDelegate(StatefulSessionStoreStatsImpl delegate) {
	this.statsImpl = delegate;
    }

    final void appendStats(StringBuffer sbuf) {
	if (statsImpl != null) {
	    statsImpl.appendStats(sbuf);
	}
    }

    //The following methods are called from StatefulSessionContainer
    //
    public final boolean isMonitoringOn() {
	return (statsImpl != null);
    }

    public final void incrementActivationCount(boolean success) {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	if (delegate != null) {
	    delegate.incrementActivationCount(success);
	}
    }

    public final void incrementPassivationCount(boolean success) {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	if (delegate != null) {
	    delegate.incrementPassivationCount(success);
	}
    }

    public final void setActivationSize(long val) {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	if (delegate != null) {
	    delegate.setActivationSize(val);
	}
    }

    public final void setActivationTime(long val) {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	if (delegate != null) {
	    delegate.setActivationTime(val);
	}
    }

    public final void setPassivationSize(long val) {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	if (delegate != null) {
	    delegate.setPassivationSize(val);
	}
    }

    public final void setPassivationTime(long val) {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	if (delegate != null) {
	    delegate.setPassivationTime(val);
	}
    }

    public final void incrementExpiredSessionsRemoved(long val) {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	if (delegate != null) {
	    delegate.incrementExpiredSessionCountVal(val);
	}
    }

    public void incrementCheckpointCount(boolean success) {
	throw new RuntimeException("Checkpoint operation not allowed on non-HA store");
    }

    public void setCheckpointSize(long val) {
	throw new RuntimeException("Checkpoint operation not allowed on non-HA store");
    }

    public void setCheckpointTime(long val) {
	throw new RuntimeException("Checkpoint operation not allowed on non-HA store");
    }

    //The following methods are maintained for backward compatibility
    //Called from LruSessionCache
    public int getNumExpiredSessionsRemoved() {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	return (delegate != null)
	    ? delegate.getNumExpiredSessionCount()
	    : 0;
    }

    public int getNumPassivationErrors() {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	return (delegate != null)
	    ? delegate.getNumPassivationErrorCount()
	    : 0;
    }

    public int getNumPassivations() {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	return (delegate != null)
	    ? delegate.getNumPassivationCount()
	    : 0;
    }

    public int getNumPassivationSuccess() {
	StatefulSessionStoreStatsImpl delegate = statsImpl;
	return (delegate != null)
	    ? delegate.getNumPassivationSuccessCount()
	    : 0;
    }

}
