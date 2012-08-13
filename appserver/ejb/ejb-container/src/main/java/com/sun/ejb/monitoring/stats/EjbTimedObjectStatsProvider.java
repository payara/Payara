/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.ejb.containers.EjbContainerUtilImpl;

import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.external.probe.provider.annotations.*;
import org.glassfish.external.statistics.*;
import org.glassfish.external.statistics.impl.*;
import org.glassfish.gmbal.*;

/**
 * Probe listener for the Ejb Timed Object monitoring events. 
 *
 * @author Marina Vatkina
 */
@AMXMetadata(type="ejb-timed-object-mon", group="monitoring", isSingleton=false)
@ManagedObject
@Description("Ejb Timed Object Statistics")
public class EjbTimedObjectStatsProvider {

    private CountStatisticImpl timerCreateStat = new CountStatisticImpl("NumTimersCreated",
            "count", "Number of timers created in the system");

    private CountStatisticImpl timerRemoveStat = new CountStatisticImpl("NumTimersRemoved",
            "count", "Number of timers removed from the system");

    private CountStatisticImpl timerDeliveredStat = new CountStatisticImpl("NumTimersDelivered",
            "count", "Number of timers delivered by the system");

    private static final Logger _logger = EjbContainerUtilImpl.getLogger();

    private String appName = null;
    private String moduleName = null;
    private String beanName = null;
    private boolean registered = false;

    public EjbTimedObjectStatsProvider(String appName, String moduleName,
            String beanName) {
        this.appName = appName;
        this.moduleName = moduleName;
        this.beanName = beanName;
    }

    public void register() {
        String invokerId = EjbMonitoringUtils.getInvokerId(appName, moduleName, beanName);
        String node = EjbMonitoringUtils.registerSubComponent(
                appName, moduleName, beanName, "timers", this, invokerId);
        if (node != null) {
            registered = true;
        }
    }

    public void unregister() {
        if (registered) {
            registered = false;
            StatsProviderManager.unregister(this);
        }
    }

    @ProbeListener("glassfish:ejb:timers:timerCreatedEvent")
    public void ejbTimerCreatedEvent() {
        _logger.fine("=== timerCreatedEvent");
        timerCreateStat.increment();
    }

    @ProbeListener("glassfish:ejb:timers:timerRemovedEvent")
    public void ejbTimerRemovedEvent() {
        _logger.fine("=== timerRemovedEvent");
        timerRemoveStat.increment();
    }

    @ProbeListener("glassfish:ejb:timers:timerDeliveredEvent")
    public void ejbTimerDeliveredEvent() {
        _logger.fine("=== timerDeliveredEvent");
        timerDeliveredStat.increment();
    }

    @ManagedAttribute(id="numtimerscreated")
    @Description( "Number of timers created in the system")
    public CountStatistic getNumTimersCreated() {
        return timerCreateStat.getStatistic();
    }

    @ManagedAttribute(id="numtimersremoved")
    @Description( "Number of timers removed from the system")
    public CountStatistic getNumTimersRemoved() {
        return timerRemoveStat.getStatistic();
    }

    @ManagedAttribute(id="numtimersdelivered")
    @Description( "Number of timers delivered by the system")
    public CountStatistic getNumTimersDelivered() {
        return timerDeliveredStat.getStatistic();
    }
}
