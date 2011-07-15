/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.monitor.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/* jvm.memory */
// v2 mbean: com.sun.appserv:name=memory,type=memory,category=monitor,server=server
// v3 mbean:
@AMXMetadata(type="memory-mon", group="monitoring")
@ManagedObject
@Description( "JVM Memory Statistics" )
public class JVMMemoryStatsProvider {
    private MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

    private CountStatisticImpl committedHeap = new CountStatisticImpl(
            "CommittedHeapSize", "bytes",
                "Amount of memory in bytes that is committed for the Java virtual machine to use" );
    private CountStatisticImpl initHeap = new CountStatisticImpl(
            "InitialHeapSize", "bytes",
                "Amount of memory in bytes that the Java virtual machine initially requests from the operating system for memory management" );
    private CountStatisticImpl maxHeap = new CountStatisticImpl(
            "MaxHeapSize", "bytes",
                "Maximum amount of memory in bytes that can be used for memory management" );
    private CountStatisticImpl usedHeap = new CountStatisticImpl(
            "UsedHeapSize", "bytes",
                "Amount of used memory in bytes" );
    private CountStatisticImpl committedNonHeap = new CountStatisticImpl(
            "CommittedNonHeapSize", "bytes",
                "Amount of memory in bytes that is committed for the Java virtual machine to use" );
    private CountStatisticImpl initNonHeap = new CountStatisticImpl(
            "InitialNonHeapSize", "bytes",
                "Amount of memory in bytes that the Java virtual machine initially requests from the operating system for memory management" );
    private CountStatisticImpl maxNonHeap = new CountStatisticImpl(
            "MaxNonHeapSize", "bytes",
                "Maximum amount of memory in bytes that can be used for memory management" );
    private CountStatisticImpl usedNonHeap = new CountStatisticImpl(
            "UsedNonHeapSize", "bytes",
                "Amount of used memory in bytes" );
    private CountStatisticImpl objectPendingFinalizationCount = new CountStatisticImpl(
            "ObjectsPendingFinalization", CountStatisticImpl.UNIT_COUNT,
                "Approximate number of objects for which finalization is pending" );

    @ManagedAttribute(id="committedheapsize-count")
    @Description( "amount of memory in bytes that is committed for the Java virtual machine to use" )
    public CountStatistic getCommittedHeap() {
        committedHeap.setCount(memBean.getHeapMemoryUsage().getCommitted());
        return committedHeap;
    }

    @ManagedAttribute(id="initheapsize-count")
    @Description( "amount of memory in bytes that the Java virtual machine initially requests from the operating system for memory management" )
    public CountStatistic getInitHeap() {
        initHeap.setCount(memBean.getHeapMemoryUsage().getInit());
        return initHeap;
    }

    @ManagedAttribute(id="maxheapsize-count")
    @Description( "maximum amount of memory in bytes that can be used for memory management" )
    public CountStatistic getMaxHeap() {
        maxHeap.setCount(memBean.getHeapMemoryUsage().getMax());
        return maxHeap;
    }

    @ManagedAttribute(id="usedheapsize-count")
    @Description( "amount of used memory in bytes" )
    public CountStatistic getUsedHeap() {
        usedHeap.setCount(memBean.getHeapMemoryUsage().getUsed());
        return usedHeap;
    }

    @ManagedAttribute(id="committednonheapsize-count")
    @Description( "amount of memory in bytes that is committed for the Java virtual machine to use" )
    public CountStatistic getCommittedNonHeap() {
        committedNonHeap.setCount(memBean.getNonHeapMemoryUsage().getCommitted());
        return committedNonHeap;
    }

    @ManagedAttribute(id="initnonheapsize-count")
    @Description( "amount of memory in bytes that the Java virtual machine initially requests from the operating system for memory management" )
    public CountStatistic getInitNonHeap() {
        initNonHeap.setCount(memBean.getNonHeapMemoryUsage().getInit());
        return initNonHeap;
    }

    @ManagedAttribute(id="maxnonheapsize-count")
    @Description( "maximum amount of memory in bytes that can be used for memory management" )
    public CountStatistic getMaxNonHeap() {
        maxNonHeap.setCount(memBean.getNonHeapMemoryUsage().getMax());
        return maxNonHeap;
    }

    @ManagedAttribute(id="usednonheapsize-count")
    @Description( "amount of used memory in bytes" )
    public CountStatistic getUsedNonHeap() {
        usedNonHeap.setCount(memBean.getNonHeapMemoryUsage().getUsed());
        return usedNonHeap;
    }

    @ManagedAttribute(id="objectpendingfinalizationcount-count")
    @Description( "approximate number of objects for which finalization is pending" )
    public CountStatistic getObjectPendingFinalizationCount() {
        objectPendingFinalizationCount.setCount(memBean.getObjectPendingFinalizationCount());
        return objectPendingFinalizationCount;
    }
}
