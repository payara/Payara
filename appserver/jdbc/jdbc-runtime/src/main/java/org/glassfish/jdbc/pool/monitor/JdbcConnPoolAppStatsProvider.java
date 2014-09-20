/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jdbc.pool.monitor;

import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.RangeStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.impl.RangeStatisticImpl;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.resourcebase.resources.api.PoolInfo;

/**
 * StatsProvider object for Jdbc pool monitoring grouped by application names.
 *
 * Implements various events related to jdbc pool monitoring and provides
 * objects to the calling modules that retrieve monitoring information.
 *
 * @author Shalini M
 */
@AMXMetadata(type="jdbc-connection-pool-app-mon", group="monitoring")
@ManagedObject
@Description("JDBC Connection pool Application based Statistics")
public class JdbcConnPoolAppStatsProvider {

    private RangeStatisticImpl numConnUsed = new RangeStatisticImpl(
            0, 0, 0,
            "NumConnUsed", StatisticImpl.UNIT_COUNT, "Provides connection usage " +
            "statistics. The total number of connections that are currently being " +
            "used, as well as information about the maximum number of connections " +
            "that were used (the high water mark).",
            System.currentTimeMillis(), System.currentTimeMillis());
    private CountStatisticImpl numConnAcquired = new CountStatisticImpl(
            "NumConnAcquired", StatisticImpl.UNIT_COUNT, "Number of logical " +
            "connections acquired from the pool.");
    private CountStatisticImpl numConnReleased = new CountStatisticImpl(
            "NumConnReleased", StatisticImpl.UNIT_COUNT, "Number of logical " +
            "connections released to the pool.");
    private static final String JDBC_APP_PROBE_LISTENER = "glassfish:jdbc-pool:applications:";
    private String appName;
    private String poolName;

    public JdbcConnPoolAppStatsProvider(PoolInfo poolInfo, String appName) {
        this.poolName = poolInfo.getName();
        this.appName = appName;
    }

    public String getPoolName() {
        return this.poolName;
    }

    public String getAppName() {
        return this.appName;
    }
    
    @ProbeListener(JDBC_APP_PROBE_LISTENER + "decrementConnectionUsedEvent")
    public void decrementConnectionUsedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName) {
	// handle the num conn used decrement event
        if((poolName != null) && (poolName.equals(this.poolName))) {
            if (appName != null && appName.equals(this.appName)) {
                //Decrement numConnUsed counter
                synchronized (numConnUsed) {
                    numConnUsed.setCurrent(numConnUsed.getCurrent() - 1);
                }
            }
        }
    }

    /**
     * Connection used event
     * @param poolName
     * @param appName
     */
    @ProbeListener(JDBC_APP_PROBE_LISTENER + "connectionUsedEvent")
    public void connectionUsedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName) {
	// handle the connection used event
        if((poolName != null) && (poolName.equals(this.poolName))) {
            if (appName != null && appName.equals(this.appName)) {
                //increment numConnUsed
                synchronized (numConnUsed) {
                    numConnUsed.setCurrent(numConnUsed.getCurrent() + 1);
                }
            }
        }
    }

    /**
     * When a connection is acquired increment counter
     */
    @ProbeListener(JDBC_APP_PROBE_LISTENER + "connectionAcquiredEvent")
    public void connectionAcquiredEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName) {
        if((poolName != null) && (poolName.equals(this.poolName))) {
            if (appName != null && appName.equals(this.appName)) {
                numConnAcquired.increment();
            }
        }
    }

    /**
     * When a connection is released increment counter
     */
    @ProbeListener(JDBC_APP_PROBE_LISTENER + "connectionReleasedEvent")
    public void connectionReleasedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName) {
        if((poolName != null) && (poolName.equals(this.poolName))) {
            if (appName != null && appName.equals(this.appName)) {
                numConnReleased.increment();
            }
        }
    }

    @ManagedAttribute(id="numconnused")
    public RangeStatistic getNumConnUsed() {
        return numConnUsed.getStatistic();
    }

    @ManagedAttribute(id="numconnacquired")
    public CountStatistic getNumConnAcquired() {
        return numConnAcquired.getStatistic();
    }

    @ManagedAttribute(id="numconnreleased")
    public CountStatistic getNumConnReleased() {
        return numConnReleased.getStatistic();
    }

}
