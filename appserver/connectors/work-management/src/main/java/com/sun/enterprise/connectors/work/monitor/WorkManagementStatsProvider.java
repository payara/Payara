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

package com.sun.enterprise.connectors.work.monitor;

import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.external.statistics.impl.RangeStatisticImpl;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.RangeStatistic;
import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;

/**
 * Provides the monitoring data for Connector Work Manager
 *
 * @author Jagadish Ramu
 */
@AMXMetadata(type="connector-service-mon", group="monitoring")
@ManagedObject
@Description("Connector Container Work Management Statistics")
public class WorkManagementStatsProvider {

    private String moduleName; //ra-name
    private static final String DOTTED_NAME = "glassfish:jca:work-management:";

    public WorkManagementStatsProvider(String moduleName) {
        this.moduleName = moduleName;
    }

    long time = System.currentTimeMillis();

    private final CountStatisticImpl submittedWorkCount =
            new CountStatisticImpl("SubmittedWorkCount", StatisticImpl.UNIT_COUNT,
                    "Number of work objects submitted by a connector module for execution");
    private final CountStatisticImpl rejectedWorkCount =
            new CountStatisticImpl("RejectedWorkCount", StatisticImpl.UNIT_COUNT,
                    "Number of work objects rejected by the application server");
    private final CountStatisticImpl completedWorkCount =
            new CountStatisticImpl("CompletedWorkCount", StatisticImpl.UNIT_COUNT,
                    "Number of work objects completed execution");

    private final RangeStatisticImpl activeWorkCount =
            new RangeStatisticImpl(0, 0, 0, "ActiveWorkCount", StatisticImpl.UNIT_COUNT,
                    "Number of active work objects", time, time);

    private final RangeStatisticImpl waitQueueLength =
            new RangeStatisticImpl(0, 0, 0, "WaitQueueLength", StatisticImpl.UNIT_COUNT,
                    "Number of work objects waiting in the queue for execution", time, time);

    private final RangeStatisticImpl workRequestWaitTime =
            new RangeStatisticImpl(0, 0, 0, "WorkRequestWaitTime", StatisticImpl.UNIT_COUNT,
                    "Wait time of a work object before it gets executed", time, time);


    @ManagedAttribute(id = "submittedworkcount")
    @Description("Number of work objects submitted by a connector module for execution")
    public CountStatistic getSubmittedWorkCount() {
        return submittedWorkCount;
    }

    @ManagedAttribute(id = "rejectedworkcount")
    @Description("Number of work objects rejected by the application server")
    public CountStatistic getRejectedWorkCount() {
        return rejectedWorkCount;
    }

    @ManagedAttribute(id = "completedworkcount")
    @Description("Number of work objects completed execution")
    public CountStatistic getCompletedWorkCount() {
        return completedWorkCount;
    }

    @ManagedAttribute(id = "activeworkcount")
    @Description("Number of active work objects")
    public RangeStatistic getActiveWorkCount() {
        return activeWorkCount;
    }

    @ManagedAttribute(id = "waitqueuelength")
    @Description("Number of work objects waiting in the queue for execution")
    public RangeStatistic getWaitQueueLength() {
        return waitQueueLength;
    }

    @ManagedAttribute(id = "workrequestwaittime")
    @Description("Wait time of a work object before it gets executed")
    public RangeStatistic getWorkRequestWaitTime() {
        return workRequestWaitTime;
    }

    private boolean isValidEvent(String raName) {
        return (raName != null && moduleName.equals(raName));
    }


    @ProbeListener(DOTTED_NAME + "workSubmitted")
    public void workSubmitted(
            @ProbeParam("raName") String raName
    ) {
        if (isValidEvent(raName)) {
            submittedWorkCount.increment();
        }
    }

    @ProbeListener(DOTTED_NAME + "workQueued")
    public void workQueued(
            @ProbeParam("raName") String raName
    ) {
        if (isValidEvent(raName)) {
            synchronized (waitQueueLength) {
                waitQueueLength.setCurrent(waitQueueLength.getCurrent() + 1);
            }
        }
    }


    @ProbeListener(DOTTED_NAME + "workWaitedFor")
    public void workWaitedFor(
            @ProbeParam("raName") String raName,
            @ProbeParam("elapsedTime") long elapsedTime
    ) {
        if (isValidEvent(raName)) {
            workRequestWaitTime.setCurrent(elapsedTime);
        }
    }

    @ProbeListener(DOTTED_NAME + "workDequeued")
    public void workDequeued(
            @ProbeParam("raName") String raName
    ) {
        if (isValidEvent(raName)) {
            synchronized (waitQueueLength) {
                waitQueueLength.setCurrent(waitQueueLength.getCurrent() - 1);
            }
        }
    }

    @ProbeListener(DOTTED_NAME + "workProcessingStarted")
    public void workProcessingStarted(
            @ProbeParam("raName") String raName
    ) {
        if (isValidEvent(raName)) {
            synchronized (activeWorkCount) {
                activeWorkCount.setCurrent(activeWorkCount.getCurrent() + 1);
            }
        }
    }

    @ProbeListener(DOTTED_NAME + "workProcessingCompleted")
    public void workProcessingCompleted(
            @ProbeParam("raName") String raName
    ) {
        if (isValidEvent(raName)) {
            synchronized (activeWorkCount) {
                activeWorkCount.setCurrent(activeWorkCount.getCurrent() - 1);
            }
        }
    }

    @ProbeListener(DOTTED_NAME + "workProcessed")
    public void workProcessed(
            @ProbeParam("raName") String raName
    ) {
        if (isValidEvent(raName)) {
            completedWorkCount.increment();
        }
    }


    @ProbeListener(DOTTED_NAME + "workTimedOut")
    public void workTimedOut(
            @ProbeParam("raName") String raName
    ) {
        if (isValidEvent(raName)) {
            rejectedWorkCount.increment();
        }
    }
}
