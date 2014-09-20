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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/* jvm.garbage-collectors */
// v2 mbean: com.sun.appserv:name=Copy,type=garbage-collector,category=monitor,server=server
// v3 mbean:
@AMXMetadata(type="garbage-collector-mon", group="monitoring")
@ManagedObject
@Description( "JVM Garbage Collectors Statistics" )
public class JVMGCStatsProvider {

    private List<GarbageCollectorMXBean> gcBeanList = ManagementFactory.getGarbageCollectorMXBeans();
    private String gcName = null;

    private CountStatisticImpl collectionCount = new CountStatisticImpl(
            "CollectionCount", CountStatisticImpl.UNIT_COUNT,
                "Total number of collections that have occurred" );

    private CountStatisticImpl collectionTimeCount = new CountStatisticImpl(
            "CollectionTime", CountStatisticImpl.UNIT_MILLISECOND,
                "Approximate accumulated collection elapsed time in milliseconds" );

    public JVMGCStatsProvider(String gcName) {
        this.gcName = gcName;
    }

    @ManagedAttribute(id="collectioncount-count")
    @Description( "total number of collections that have occurred" )
    public CountStatistic getCollectionCount() {
        long counts = -1;
        for (GarbageCollectorMXBean gcBean : gcBeanList) {
            if (gcBean.getName().equals(gcName)) {
                counts = gcBean.getCollectionCount();
            }
        }
        collectionCount.setCount(counts);
        return collectionCount;
    }

    @ManagedAttribute(id="collectiontime-count")
    @Description( "approximate accumulated collection elapsed time in milliseconds" )
    public CountStatistic getCollectionTime() {
        long times = -1;
        int i = 0;
        for (GarbageCollectorMXBean gcBean : gcBeanList) {
            if (gcBean.getName().equals(gcName)) {
                times = gcBean.getCollectionTime();
            }
        }
        collectionTimeCount.setCount(times);
        return collectionTimeCount;
    }

}
