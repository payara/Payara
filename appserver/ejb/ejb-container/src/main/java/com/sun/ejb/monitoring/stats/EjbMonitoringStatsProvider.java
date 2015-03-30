/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.external.probe.provider.annotations.*;
import org.glassfish.external.statistics.*;
import org.glassfish.external.statistics.impl.*;
import org.glassfish.gmbal.*;

import com.sun.ejb.containers.EjbContainerUtilImpl;

/**
 * Event listener for the Ejb monitoring events. Used by the probe framework 
 * to collect and display the data.
 *
 * @author Marina Vatkina
 * @author Mahesh Kannan
 */
// @AMXMetadata and @ManagedObject should NOT be added here on this base class.
// Need to be added to derived classes to define the individual types.
public abstract class EjbMonitoringStatsProvider {

    Map<String, EjbMethodStatsProvider> methodMonitorMap;

    String appName = null;
    String moduleName = null;
    String beanName = null;
    boolean registered = false;
    protected long beanId;

    private CountStatisticImpl createStat = new CountStatisticImpl("CreateCount", 
            "count", "Number of times EJB create method is called or 3.x bean is looked up");

    private CountStatisticImpl removeStat = new CountStatisticImpl("RemoveCount", 
            "count", "Number of times EJB remove method is called");

    static final Logger _logger = EjbContainerUtilImpl.getLogger();

    public EjbMonitoringStatsProvider(long beanId, String appName, String moduleName,
            String beanName) {
        this.beanId = beanId;
        this.appName = appName;
        this.moduleName = moduleName;
        this.beanName = beanName;
    }

    public void addMethods(long beanId, String appName, String moduleName,
            String beanName, String[] method_sigs) {

        int bucketSz = (method_sigs.length == 0) ? 16 : method_sigs.length << 1;
        methodMonitorMap = new HashMap<String, EjbMethodStatsProvider>(bucketSz);

        if (this.beanId == beanId) {
            for (String method_sig : method_sigs) {
                EjbMethodStatsProvider monitor = methodMonitorMap.get(method_sig);
                if (monitor == null) {
                    monitor = new EjbMethodStatsProvider(method_sig);
                    methodMonitorMap.put(method_sig, monitor);
                }
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "[EJBMonitoringStatsProvider] : "
                        + EjbMonitoringUtils.getLoggingName(appName, moduleName, beanName)
                        + ":" + methodMonitorMap.size());
            }
        }
    }

    public void register() {
        String invokerId = EjbMonitoringUtils.getInvokerId(appName, moduleName, beanName);
        String beanSubTreeNode = EjbMonitoringUtils.registerComponent(
                appName, moduleName, beanName, this, invokerId);
        if (beanSubTreeNode != null) {
            registered = true;
            for (Map.Entry<String, EjbMethodStatsProvider> entry : methodMonitorMap.entrySet()) {
                EjbMethodStatsProvider monitor = entry.getValue();
                if (!monitor.isRegistered()) {
                    String node = EjbMonitoringUtils.registerMethod(beanSubTreeNode,
                            monitor.getStringifiedMethodName(), monitor, invokerId);
                    if (node != null) {
                        monitor.registered();
                    }
                }
            }
        }
    }

    public void unregister() {
        boolean debug = _logger.isLoggable(Level.FINE);
        if (registered) {
            if (debug) {
                _logger.log(Level.FINE, "[EJBMonitoringStatsProvider] unregister: " 
                       + EjbMonitoringUtils.getLoggingName(appName, moduleName, beanName));
            }
            registered = false;
            StatsProviderManager.unregister(this);
            for ( EjbMethodStatsProvider monitor : methodMonitorMap.values()) {
                if (monitor.isRegistered()) {
                    if (debug) {
                        _logger.log(Level.FINE, "[EJBMonitoringStatsProvider] unregister method: " 
                                + monitor.getStringifiedMethodName());
                    }
                    monitor.unregistered();
                    StatsProviderManager.unregister(monitor);
                }
            }

            methodMonitorMap.clear();
        }
    }

    @ProbeListener("glassfish:ejb:bean:methodStartEvent")
    public void ejbMethodStartEvent(
            @ProbeParam("beanId") long beanId,
            @ProbeParam("appName") String appName,
            @ProbeParam("modName") String modName,
            @ProbeParam("ejbName") String ejbName,
            @ProbeParam("method") String method_sig) {
        if (this.beanId == beanId) {
            _log("ejbMethodStartEvent", method_sig);
            EjbMethodStatsProvider monitor = methodMonitorMap.get(method_sig);
            if (monitor != null) {
                monitor.methodStart();
            }
        }
    }

    @ProbeListener("glassfish:ejb:bean:methodEndEvent")
    public void ejbMethodEndEvent(
            @ProbeParam("beanId") long beanId,
            @ProbeParam("appName") String appName,
            @ProbeParam("modName") String modName,
            @ProbeParam("ejbName") String ejbName,
            @ProbeParam("exception") Throwable exception,
            @ProbeParam("method") String method_sig) {
        if (this.beanId == beanId) {
            _log("ejbMethodEndEvent", method_sig);
            EjbMethodStatsProvider monitor = methodMonitorMap.get(method_sig);
            if (monitor != null) {
                monitor.methodEnd((exception == null));
            }
        }
    }

    @ProbeListener("glassfish:ejb:bean:beanCreatedEvent")
    public void ejbBeanCreatedEvent(
            @ProbeParam("beanId") long beanId,
            @ProbeParam("appName") String appName,
            @ProbeParam("modName") String modName,
            @ProbeParam("ejbName") String ejbName) {
        if (this.beanId == beanId) {
            _log("ejbBeanCreatedEvent");
            createStat.increment();
        }
    }

    @ProbeListener("glassfish:ejb:bean:beanDestroyedEvent")
    public void ejbBeanDestroyedEvent(
            @ProbeParam("beanId") long beanId,
            @ProbeParam("appName") String appName,
            @ProbeParam("modName") String modName,
            @ProbeParam("ejbName") String ejbName) {
        if (this.beanId == beanId) {
            _log("ejbBeanDestroyedEvent");
            removeStat.increment();
        }
    }

    @ManagedAttribute(id="createcount")
    @Description( "Number of times EJB create method is called")
    public CountStatistic getCreateCount() {
        return createStat;
    }

    @ManagedAttribute(id="removecount")
    @Description( "Number of times EJB remove method is called")
    public CountStatistic getRemoveCount() {
        return removeStat;
    }

    protected void log(String mname, String provider) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("===> In " + provider + " for: [" 
                    + mname + "] " + EjbMonitoringUtils.getLoggingName(appName, moduleName, beanName));
        }
    }

    private void _log(String mname) {
        log(mname, "EjbMonitoringStatsProvider");
    }

    private void _log(String mname, String method_sig) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("===> In EjbMonitoringStatsProvider for: [" 
                    + mname + "] " + EjbMonitoringUtils.getLoggingName(appName, moduleName, beanName)
                    + "::" + method_sig);
        }
    }

}
