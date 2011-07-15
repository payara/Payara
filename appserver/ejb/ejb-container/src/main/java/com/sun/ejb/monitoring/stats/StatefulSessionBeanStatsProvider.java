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

package com.sun.ejb.monitoring.stats;

import com.sun.ejb.containers.StatefulSessionContainer;

import org.glassfish.external.probe.provider.annotations.*;
import org.glassfish.external.statistics.*;
import org.glassfish.external.statistics.impl.*;
import org.glassfish.gmbal.*;

/**
 * Probe listener for the Stateful Session Beans part of the EJB monitoring events. 
 *
 * @author Marina Vatkina
 */
@AMXMetadata(type="stateful-session-bean-mon", group="monitoring", isSingleton=false)
@ManagedObject
public class StatefulSessionBeanStatsProvider extends EjbMonitoringStatsProvider {

    private BoundedRangeStatisticImpl methodReadyStat = null;
    private BoundedRangeStatisticImpl passiveCount = null;

    private int methodReadyCount = 0;
    private StatefulSessionContainer delegate;

    public StatefulSessionBeanStatsProvider(StatefulSessionContainer delegate,
            long beanId, String appName, String moduleName, String beanName) {

        super(beanId, appName, moduleName, beanName);
        this.delegate = delegate;

        long now = System.currentTimeMillis();

        methodReadyStat = new BoundedRangeStatisticImpl(
            0, 0, 0, delegate.getMaxCacheSize(), 0,
            "MethodReadyCount", "count", "Number of stateful session beans in MethodReady state",
            now, now);

        passiveCount = new BoundedRangeStatisticImpl(
            0, 0, 0, Long.MAX_VALUE, 0,
            "PassiveCount", "count", "Number of stateful session beans in Passive state",
            now, now);
    }

    @ManagedAttribute(id="methodreadycount")
    @Description( "Number of stateful session beans in MethodReady state")
    public RangeStatistic getMethodReadyCount() {
        methodReadyStat.setCurrent(methodReadyCount);
        return methodReadyStat.getStatistic();
    }

    @ManagedAttribute(id="passivecount")
    @Description( "Number of stateful session beans in Passive state")
    public RangeStatistic getPassiveCount() {
        passiveCount.setCurrent(delegate.getPassiveCount());
        return passiveCount.getStatistic();
    }

    @ProbeListener("glassfish:ejb:bean:methodReadyAddEvent")
    public void methodReadyAddEvent(
            @ProbeParam("beanId") long beanId,
            @ProbeParam("appName") String appName,
            @ProbeParam("modName") String modName,
            @ProbeParam("ejbName") String ejbName) {
        if (this.beanId == beanId) {
            log ("methodReadyAddEvent", "StatefulSessionBeanStatsProvider");
            methodReadyCount++;
        }
    }

    @ProbeListener("glassfish:ejb:bean:methodReadyRemoveEvent")
    public void methodReadyRemoveEvent(
            @ProbeParam("beanId") long beanId,
            @ProbeParam("appName") String appName,
            @ProbeParam("modName") String modName,
            @ProbeParam("ejbName") String ejbName) {
        if (this.beanId == beanId) {
            log ("methodReadyRemoveEvent", "StatefulSessionBeanStatsProvider");
            methodReadyCount--;
        }
    }

}
