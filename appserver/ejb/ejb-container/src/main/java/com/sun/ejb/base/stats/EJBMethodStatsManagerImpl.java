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

package com.sun.ejb.base.stats;

import com.sun.ejb.spi.stats.EJBMethodStatsManager;
import com.sun.enterprise.admin.monitor.registry.MonitoredObjectType;
import com.sun.enterprise.admin.monitor.registry.MonitoringRegistrationException;
import com.sun.enterprise.admin.monitor.registry.MonitoringRegistry;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A class that manages all the monitored EJB methods
 *
 * @author Mahesh Kannan
 *
 */

public final class EJBMethodStatsManagerImpl
    implements EJBMethodStatsManager
{
    private Logger _logger;

    private MonitoringRegistry		registry;

    private final String			appName;
    private final String			modName;
    private final String			ejbName;
    private final MonitoredObjectType ejbType;

    private Method[]			methods;
    private boolean			methodMonitorOn;
    

    private MethodMonitor[]		methodMonitors;
    private HashMap			methodMonitorMap;
    private Object			lock = new Object();
    private Object[]			logParams = null;

    private boolean prefixWithClassName;
    
    EJBMethodStatsManagerImpl(MonitoringRegistry registry, 
	    MonitoredObjectType ejbType, String ejbName, String modName, String appName)
    {
	this.registry = registry;
    
    this.ejbType   = ejbType;

	this.ejbName = ejbName;
	this.modName = modName;
	this.appName = appName;

	logParams = new Object[] {ejbName, modName, appName};
    }

    public final boolean isMethodMonitorOn() {
	return methodMonitorOn;
    }

    public final void preInvoke(Method method) {
	if (methodMonitorOn) {
	    MethodMonitor monitor = null;
	    synchronized (lock) {
		if (methodMonitorOn) {
		    monitor = (MethodMonitor) methodMonitorMap.get(method);
		}
	    }
	    if (monitor != null) {
            monitor.preInvoke();
	    }
	}
    }

    public final void postInvoke(Method method, Throwable th) {
	if (methodMonitorOn) {
	    MethodMonitor monitor = null;
	    synchronized (lock) {
		if (methodMonitorOn) {
		    monitor = (MethodMonitor) methodMonitorMap.get(method);
		}
	    }
	    if (monitor != null) {
		monitor.postInvoke(th);
	    }
	}
    }

    public MethodMonitor[]  getMethodMonitors() {
	return this.methodMonitors;
    }

    public void undeploy() {
	synchronized (lock) {
	    methodMonitorOn = false;
	}
	deregisterStats();

	methods = null;
	methodMonitors = null;
	methodMonitorMap = null;
	registry = null;
    }
    
    void registerMethods(Method[] methods, boolean prefixWithClassName) {
        this.prefixWithClassName = prefixWithClassName;
        this.methods = methods;
    }
    
    void setMethodMonitorOn(boolean monitorOn) {
	if (methods == null) {
	    _logger.log(Level.WARNING, "base.stats.method.nomethods", logParams);
	    return;
	}
	int size = methods.length;
	if (monitorOn == true) {
	    this.methodMonitors = new MethodMonitor[size];
	    HashMap map = new HashMap();
	    for (int i=0; i<size; i++) {
		methodMonitors[i] = new MethodMonitor(methods[i], prefixWithClassName);
		map.put(methods[i], methodMonitors[i]);

		EJBMethodStatsImpl impl =
		    new EJBMethodStatsImpl(methodMonitors[i]);
		try {
		    if (_logger.isLoggable(Level.FINE)) {
			_logger.log(Level.FINE, "Registering method: "
			    + methodMonitors[i].getMethodName()
			    + "; for " + appName + "; " + modName
			    + "; " + ejbName);
		    }
		    registry.registerEJBMethodStats(impl, 
			methodMonitors[i].getMethodName(),
			ejbType, ejbName, modName, appName, null);
		    if (_logger.isLoggable(Level.FINE)) {
			_logger.log(Level.FINE, "Registered method: "
			    + methodMonitors[i].getMethodName()
			    + "; for " + appName + "; " + modName
			    + "; " + ejbName);
		    }
		} catch (MonitoringRegistrationException monRegEx) {
		    Object[] params = new Object[] {ejbName, modName,
			    appName, methodMonitors[i].getMethodName()};
		    _logger.log(Level.WARNING,
			    "base.stats.method.register.monreg.error", params);
		    _logger.log(Level.FINE, "", monRegEx);
		} catch (Exception ex) {
		    Object[] params = new Object[] {ejbName, modName,
			    appName, methodMonitors[i].getMethodName()};
		    _logger.log(Level.WARNING,
			    "base.stats.method.register.error", params);
		    _logger.log(Level.FINE, "", ex);
		}
	    }
	    this.methodMonitorMap = map;
	    synchronized (lock) {
		this.methodMonitorOn = true;
	    }
	} else {
	    synchronized (lock) {
		this.methodMonitorOn = false;
	    }
	    deregisterStats();

	    this.methodMonitorMap = null;
	    this.methodMonitors = null;

	}
    }

    void appendStats(StringBuffer sbuf) {
	if (methodMonitors != null) {
	    int size = methods.length;
	    for (int i=0; i<size; i++) {
		MethodMonitor monitor =
		    (MethodMonitor) methodMonitors[i];
		monitor.appendStats(sbuf);
	    }
	}
    }

    private void deregisterStats() {
	if (methodMonitors == null) {
	    return;
	}
	int size = methodMonitors.length;
	for (int i=0; i<size; i++) {
	    try {
		registry.unregisterEJBMethodStats(
		    methodMonitors[i].getMethodName(),
		     ejbType, ejbName, modName, appName);
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, "Unregistered method: "
			    + methodMonitors[i].getMethodName()
			    + "; for " + appName + "; " + modName
			    + "; " + ejbName);
		}
	    } catch (MonitoringRegistrationException monRegEx) {
		Object[] params = new Object[] {ejbName, modName,
		    appName, methodMonitors[i].getMethodName()};
		_logger.log(Level.FINE,
			"base.stats.method.unregister.monreg.error", params);
		_logger.log(Level.FINE, "", monRegEx);
	    } catch (Exception ex) {
		Object[] params = new Object[] {ejbName, modName,
		    appName, methodMonitors[i].getMethodName()};
		_logger.log(Level.WARNING,
			"base.stats.method.unregister.error", params);
		_logger.log(Level.FINE, "", ex);
	    }
	}

	methodMonitors = null;
    }

}
