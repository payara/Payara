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

import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.spi.stats.*;
import com.sun.enterprise.admin.monitor.registry.*;

import org.glassfish.j2ee.statistics.Stats;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that acts as an Mediator between admin.registry.* objects
 *  and EJBContainers. There is one instance of MonitoringRegistryMediator
 *  per EJBContainer. Containers interact only with this object
 *  and are completely unaware of any the MonitoringRegistry classes
 *
 * @author Mahesh Kannan
 */

public class MonitoringRegistryMediator
    implements MonitoringLevelListener
{
    private static final int	    ENTITY_CONTAINER_TYPE = 0;
    private static final int	    STATELESS_CONTAINER_TYPE = 1;
    private static final int	    STATEFUL_CONTAINER_TYPE = 2;
    private static final int	    MESSAGE_CONTAINER_TYPE = 3;

    private static final Logger _logger = EjbContainerUtilImpl.getInstance().getLogger();

    private final String			appName;
    private final String			modName;
    private final String			ejbName;
    private final MonitoredObjectType ejbType;
    
    private int				containerType;
    private MonitoringLevel		currentMonitoringLevel;
    private MonitoringRegistry		registry;

    private EJBStatsProvider		ejbStatsProvider;
    private EJBCacheStatsProvider	ejbCacheStatsProvider;
    private EJBPoolStatsProvider	ejbPoolStatsProvider;
    private MonitorableSFSBStoreManager sfsbStoreStatsProvider;
    private EJBTimedObjectStatsProvider	ejbTimedObjectStatsProvider;

    private EJBStatsImpl		ejbStatsImpl;
    private EJBCacheStatsImpl		ejbCacheStatsImpl;
    private EJBPoolStatsImpl		ejbPoolStatsImpl;
    private EJBTimedObjectStatsImpl	ejbTimedObjectStatsImpl;
    private EJBMethodStatsManagerImpl	ejbMethodStatsManager;

    private StatefulSessionStoreMonitor	sfsbStoreMonitor;
    private boolean			isHAStore;

    private Object[]			logParams = null;

    public MonitoringRegistryMediator( MonitoredObjectType ejbType, String ejbName, String modName,
	    String appName)
    {
	this.appName = appName;
	this.modName = modName;
    this.ejbType = ejbType;
	this.ejbName = ejbName;
    
	logParams = new Object[] {ejbName, modName, appName};

	this.registry = null;//TODO this.registry = ApplicationServer.getServerContext().getMonitoringRegistry();

	this.ejbMethodStatsManager = new EJBMethodStatsManagerImpl(
		registry, ejbType, ejbName, modName, appName);
    }

    public void registerProvider(StatelessSessionBeanStatsProvider provider) {
	this.ejbStatsProvider = provider;
	this.containerType = STATELESS_CONTAINER_TYPE;
	registry.registerMonitoringLevelListener(this, MonitoredObjectType.STATELESS_BEAN);
	this.currentMonitoringLevel = getCurrentMonitoringLevel();
	if (! isMonitoringOff()) {
	    registerContainerStats();
	}
    }

    public void registerProvider(StatefulSessionBeanStatsProvider provider) {
	this.ejbStatsProvider = provider;
	this.containerType = STATEFUL_CONTAINER_TYPE;
	registry.registerMonitoringLevelListener(this, MonitoredObjectType.STATEFUL_BEAN);
	this.currentMonitoringLevel = getCurrentMonitoringLevel();
	if (! isMonitoringOff()) {
	    registerContainerStats();
	}
    }

    public void registerProvider(EntityBeanStatsProvider provider) {
	this.ejbStatsProvider = provider;
	this.containerType = ENTITY_CONTAINER_TYPE;
	registry.registerMonitoringLevelListener(this,
		MonitoredObjectType.ENTITY_BEAN);
	this.currentMonitoringLevel = getCurrentMonitoringLevel();
	if (! isMonitoringOff()) {
	    registerContainerStats();
	}
    }

    public void registerProvider(MessageDrivenBeanStatsProvider provider) {
	this.ejbStatsProvider = provider;
	this.containerType = MESSAGE_CONTAINER_TYPE;
	registry.registerMonitoringLevelListener(this,
		MonitoredObjectType.MESSAGE_DRIVEN_BEAN);
	this.currentMonitoringLevel = getCurrentMonitoringLevel();
	if (! isMonitoringOff()) {
	    registerContainerStats();
	}
    }

    public void registerProvider(EJBCacheStatsProvider provider) {
	this.ejbCacheStatsProvider = provider;
	if (! isMonitoringOff()) {
	    registerCacheStats();
	}
    }

    public void registerProvider(EJBPoolStatsProvider provider) {
	this.ejbPoolStatsProvider = provider;
	if (! isMonitoringOff()) {
	    registerPoolStats();
	}
    }

    public StatefulSessionStoreMonitor registerProvider(
	    MonitorableSFSBStoreManager provider, boolean isHAStore)
    {
	this.sfsbStoreStatsProvider = provider;
	this.isHAStore = isHAStore;
	if (isHAStore) {
	    this.sfsbStoreMonitor = new HAStatefulSessionStoreMonitor();
	} else {
	    this.sfsbStoreMonitor = new StatefulSessionStoreMonitor();
	}
	if (! isMonitoringOff()) {
	    registerSFSBStoreStats();
	}

	return sfsbStoreMonitor;
    }

    public EJBMethodStatsManager getEJBMethodStatsManager() {
	return this.ejbMethodStatsManager;
    }

    public void registerEJBMethods(Method[] methods, boolean prefixWithClassName) {
        this.ejbMethodStatsManager.registerMethods(methods, prefixWithClassName);
        if (isMonitoringHigh()) {
            this.ejbMethodStatsManager.setMethodMonitorOn(true);
        }
    }
    
    public void registerProvider(EJBTimedObjectStatsProvider provider) {
	this.ejbTimedObjectStatsProvider = provider;
	if (! isMonitoringOff()) {
	    registerTimedObjectStats();
	}
    }

    //Methods for MonitoringLevelListener interface
    public void setLevel(MonitoringLevel level) {}

    public void changeLevel(MonitoringLevel from, MonitoringLevel to, 
	    Stats type)
    {
	//No op. Deprecated
    }
	
        private boolean
    isEJBType( final MonitoredObjectType type )
    {
        return
            type == MonitoredObjectType.STATELESS_BEAN ||
            type == MonitoredObjectType.STATEFUL_BEAN ||
            type == MonitoredObjectType.ENTITY_BEAN ||
            type == MonitoredObjectType.MESSAGE_DRIVEN_BEAN;
    }
    
    public void changeLevel(MonitoringLevel fromLevel,
	    MonitoringLevel toLevel, MonitoredObjectType type)
    {
	if ( isEJBType(type) ) {
	    synchronized (this) {
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, "[MonitoringMediator] Level "
			+ "changed from: "
			+ monitoringLevelAsString(currentMonitoringLevel)
			+ " to: "
			+ monitoringLevelAsString(toLevel));
		}
	    
	    this.currentMonitoringLevel = toLevel;

	    if (toLevel == MonitoringLevel.OFF) {
		deregisterPoolStats();
		deregisterCacheStats();
		ejbMethodStatsManager.setMethodMonitorOn(false);
		deregisterSFSBStoreStats();
		deregisterContainerStats();
		deregisterTimedObjectStats();
	    } else {
		registerContainerStats();
		registerPoolStats();
		registerCacheStats();
		registerSFSBStoreStats();
		ejbMethodStatsManager.setMethodMonitorOn(
		    toLevel == MonitoringLevel.HIGH);
		registerTimedObjectStats();
	    }
        }
	}
    }

    public void undeploy() {
	try {
	    deregisterPoolStats();
	    deregisterCacheStats();
	    ejbMethodStatsManager.undeploy();
	    deregisterContainerStats();
	    deregisterSFSBStoreStats();
	    deregisterTimedObjectStats();
	} finally {
	    ejbStatsProvider = null;
	    ejbCacheStatsProvider = null;
	    ejbPoolStatsProvider = null;
	    ejbMethodStatsManager = null;
	    sfsbStoreStatsProvider = null;
	    sfsbStoreMonitor = null;
	    ejbTimedObjectStatsProvider = null;

	    if (registry != null) registry.unregisterMonitoringLevelListener(this);
	}
    }

    /************** Internal methods ********************/

    private void registerContainerStats() {
	if ((ejbStatsProvider == null) || (this.ejbStatsImpl != null)) {
	    return;
	}

	try {
	    switch (containerType) {
		case STATELESS_CONTAINER_TYPE:
		    StatelessSessionBeanStatsImpl slsbImpl =
			new StatelessSessionBeanStatsImpl(
			(StatelessSessionBeanStatsProvider) ejbStatsProvider);
		    registry.registerStatelessSessionBeanStats(slsbImpl,
			ejbName, modName, appName, null);
		    this.ejbStatsImpl = slsbImpl;
		    break;
		case STATEFUL_CONTAINER_TYPE:
		    StatefulSessionBeanStatsImpl sfsbImpl =
			new StatefulSessionBeanStatsImpl(
			(StatefulSessionBeanStatsProvider) ejbStatsProvider);
		    registry.registerStatefulSessionBeanStats(sfsbImpl,
			    ejbName, modName, appName, null);
		    this.ejbStatsImpl = sfsbImpl;
		    break;
		case ENTITY_CONTAINER_TYPE:
		    EntityBeanStatsImpl entityImpl =
			new EntityBeanStatsImpl(
			(EntityBeanStatsProvider) ejbStatsProvider);
		    registry.registerEntityBeanStats(entityImpl,
			    ejbName, modName, appName, null);
		    this.ejbStatsImpl = entityImpl;
		    break;
		case MESSAGE_CONTAINER_TYPE:
		    MessageDrivenBeanStatsImpl mdbImpl =
			new MessageDrivenBeanStatsImpl(
			(MessageDrivenBeanStatsProvider) ejbStatsProvider);
		    registry.registerMessageDrivenBeanStats(mdbImpl,
			    ejbName, modName, appName, null);
		    this.ejbStatsImpl = mdbImpl;
		    break;
	    }
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, "[MonitoringMediator] registered "
			+ " container stats for " + appName
			+ "; " + modName + "; " + ejbName);
	    }
	} catch (MonitoringRegistrationException monRegEx) {
	    ejbStatsImpl = null;
	    _logger.log(Level.WARNING,
		"base.stats.mediator.ejb.register.monreg.error", logParams);
	    _logger.log(Level.FINE, "", monRegEx);
	} catch (Exception ex) {
	    ejbStatsImpl = null;
	    _logger.log(Level.WARNING,
		"base.stats.mediator.ejb.register.error", logParams);
	    _logger.log(Level.FINE, "", ex);
	}
    }

    private void deregisterContainerStats() {
	try {
	    if (ejbStatsImpl != null) {
		switch (containerType) {
		    case STATELESS_CONTAINER_TYPE:
			registry.unregisterStatelessSessionBeanStats(
				ejbName, modName, appName);
			break;
		    case STATEFUL_CONTAINER_TYPE:
			registry.unregisterStatefulSessionBeanStats(
				ejbName, modName, appName);
			break;
		    case ENTITY_CONTAINER_TYPE:
			registry.unregisterEntityBeanStats(
				ejbName, modName, appName);
			break;
		    case MESSAGE_CONTAINER_TYPE:
			registry.unregisterMessageDrivenBeanStats(
				ejbName, modName, appName);
			break;
		}
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, "[MonitoringMediator] unregistered "
			    + " container stats for " + appName
			    + "; " + modName + "; " + ejbName);
		}
	    }
	} catch (MonitoringRegistrationException monRegEx) {
	    _logger.log(Level.WARNING,
		"base.stats.mediator.ejb.unregister.monreg.error", logParams);
	    _logger.log(Level.FINE, "", monRegEx);
	} catch (Exception ex) {
	    _logger.log(Level.WARNING,
		"base.stats.mediator.ejb.unregister.error", logParams);
	    _logger.log(Level.FINE, "", ex);
	} finally {
	    ejbStatsImpl = null;
	}
    }

    private void registerPoolStats() {
	if ((ejbPoolStatsProvider != null) && (ejbPoolStatsImpl == null)) {
	    try {
		ejbPoolStatsImpl = new EJBPoolStatsImpl(ejbPoolStatsProvider);
		registry.registerEJBPoolStats(ejbPoolStatsImpl,
		    ejbType, ejbName, modName, appName, null);
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, "[MonitoringMediator] registered "
			    + " pool stats for " + appName
			    + "; " + modName + "; " + ejbName);
		}
	    } catch (MonitoringRegistrationException monRegEx) {
		ejbPoolStatsImpl = null;
		_logger.log(Level.WARNING,
		    "base.stats.mediator.pool.register.monreg.error", logParams);
		_logger.log(Level.FINE, "", monRegEx);
	    } catch (Exception ex) {
		ejbPoolStatsImpl = null;
		_logger.log(Level.WARNING,
		    "base.stats.mediator.pool.register.error", logParams);
		_logger.log(Level.FINE, "", ex);
	    }
	}
    }

    private void registerCacheStats() {
	if ((ejbCacheStatsProvider != null) && (ejbCacheStatsImpl == null)) {
	    try {
		ejbCacheStatsImpl = new EJBCacheStatsImpl(ejbCacheStatsProvider);
		registry.registerEJBCacheStats(ejbCacheStatsImpl,
		    ejbType, ejbName, modName, appName, null);
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, "[MonitoringMediator] registered "
			    + " cache stats for " + appName
			    + "; " + modName + "; " + ejbName);
		}
	    } catch (MonitoringRegistrationException monRegEx) {
		ejbCacheStatsImpl = null;
		_logger.log(Level.WARNING,
		    "base.stats.mediator.cache.register.monreg.error", logParams);
		_logger.log(Level.FINE, "", monRegEx);
	    } catch (Exception ex) {
		ejbCacheStatsImpl = null;
		_logger.log(Level.WARNING,
		    "base.stats.mediator.cache.register.error", logParams);
		_logger.log(Level.FINE, "", ex);
	    }
	}
    }

    private void registerSFSBStoreStats() {
	if (sfsbStoreMonitor != null) {
	    StatefulSessionStoreStatsImpl statsImpl = null;
	    statsImpl = (isHAStore)
		? new HAStatefulSessionStoreStatsImpl(sfsbStoreStatsProvider)
		: new StatefulSessionStoreStatsImpl(sfsbStoreStatsProvider);
	    sfsbStoreMonitor.setDelegate(statsImpl);
	    //FIXME: registry.registerStatefulSessionStoreStats(statsImpl);
	    sfsbStoreStatsProvider.monitoringLevelChanged(true);
	}
    }

    private void registerTimedObjectStats() {
	if (ejbTimedObjectStatsProvider != null) {
            ejbTimedObjectStatsProvider.monitoringLevelChanged(true);
            if (ejbTimedObjectStatsImpl == null) {
	        try {
		    ejbTimedObjectStatsImpl = 
                        new EJBTimedObjectStatsImpl(
                            ejbTimedObjectStatsProvider );
		    registry.registerTimerStats(ejbTimedObjectStatsImpl,
		        ejbType, ejbName, modName, appName, null);
		    if (_logger.isLoggable(Level.FINE)) {
		        _logger.log(Level.FINE, "[MonitoringMediator] registered "
			        + " timed Object stats for " + appName
			        + "; " + modName + "; " + ejbName);
		    }
	        } catch (MonitoringRegistrationException monRegEx) {
		    ejbTimedObjectStatsImpl = null;
		    _logger.log(Level.WARNING,
		        "base.stats.mediator.timedObject.register.monreg.error", logParams);
		    _logger.log(Level.FINE, "", monRegEx);
	        } catch (Exception ex) {
		    ejbTimedObjectStatsImpl = null;
		    _logger.log(Level.WARNING,
		        "base.stats.mediator.timedObject.register.error", logParams);
		    _logger.log(Level.FINE, "", ex);
	        }
	    } 
        }
    }

    private void deregisterPoolStats() {
	if ((ejbPoolStatsProvider != null) && (ejbPoolStatsImpl != null)) {
	    try {
		registry.unregisterEJBPoolStats( ejbType, ejbName, modName, appName);
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, "[MonitoringMediator] unregistered "
			    + " pool stats for " + appName
			    + "; " + modName + "; " + ejbName);
		}
	    } catch (MonitoringRegistrationException monRegEx) {
		_logger.log(Level.WARNING,
		    "base.stats.mediator.pool.unregister.monreg.error", logParams);
		_logger.log(Level.FINE, "", monRegEx);
	    } catch (Exception ex) {
		_logger.log(Level.WARNING,
		    "base.stats.mediator.pool.unregister.error", logParams);
		_logger.log(Level.FINE, "", ex);
	    } finally {
		ejbPoolStatsImpl = null;
	    }
	}
    }

    private void deregisterCacheStats() {
	if ((ejbCacheStatsProvider != null) && (ejbCacheStatsImpl != null)) {
	    try {
		registry.unregisterEJBCacheStats( ejbType, ejbName, modName, appName);
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, "[MonitoringMediator] unregistered "
			    + " cache stats for " + appName
			    + "; " + modName + "; " + ejbName);
		}
	    } catch (MonitoringRegistrationException monRegEx) {
		_logger.log(Level.WARNING,
		    "base.stats.mediator.cache.unregister.monreg.error", logParams);
		_logger.log(Level.FINE, "", monRegEx);
	    } catch (Exception ex) {
		_logger.log(Level.WARNING,
		    "base.stats.mediator.cache.unregister.error", logParams);
		_logger.log(Level.FINE, "", ex);
	    } finally {
		ejbCacheStatsImpl = null;
	    }
	}
    }

    private void deregisterSFSBStoreStats() {
	//FIXME: registry.unregisterStatefulSessionStoreStats(ejbName, modName, appName);
	if (sfsbStoreMonitor != null) {
	    sfsbStoreStatsProvider.monitoringLevelChanged(false);
	    sfsbStoreMonitor.setDelegate(null);
	}
    }

    private void deregisterTimedObjectStats() {
	if ((ejbTimedObjectStatsProvider != null) && (ejbTimedObjectStatsImpl != null)) {
	    try {
                ejbTimedObjectStatsProvider.monitoringLevelChanged(false);
		registry.unregisterTimerStats( ejbType, ejbName, modName, appName);
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, "[MonitoringMediator] unregistered "
			    + " timed Object stats for " + appName
			    + "; " + modName + "; " + ejbName);
		}
	    } catch (MonitoringRegistrationException monRegEx) {
		_logger.log(Level.WARNING,
		    "base.stats.mediator.timedObject.unregister.monreg.error", logParams);
		_logger.log(Level.FINE, "", monRegEx);
	    } catch (Exception ex) {
		_logger.log(Level.WARNING,
		    "base.stats.mediator.timedObject.unregister.error", logParams);
		_logger.log(Level.FINE, "", ex);
	    } finally {
		ejbTimedObjectStatsImpl = null;
	    }
	}
    }

    public void logMonitoredComponentsData(boolean logMethodData) {
	StringBuffer sbuf = new StringBuffer();
	sbuf.append("[BEGIN Container-Stats: ").append(ejbName).append("]");
	ejbStatsProvider.appendStats(sbuf);
	if (ejbCacheStatsProvider != null) {
	    ejbCacheStatsProvider.appendStats(sbuf);
	}
	if (ejbPoolStatsProvider != null) {
	    ejbPoolStatsProvider.appendStats(sbuf);
	}
	if (logMethodData) {
	    ejbMethodStatsManager.appendStats(sbuf);
	}
	if (ejbTimedObjectStatsProvider != null) {
	    ejbTimedObjectStatsProvider.appendStats(sbuf);
	}
	if (sfsbStoreMonitor != null) {
	    sbuf.append((isHAStore ? "[HASFSBStore ": "[SFSBStore "));
	    sfsbStoreMonitor.appendStats(sbuf);
	    sfsbStoreStatsProvider.appendStats(sbuf);
	    sbuf.append("]");
	}
	sbuf.append("[END   Container-Stats: ").append(ejbName).append("]");
	_logger.log(Level.INFO, sbuf.toString());
    }

    private final synchronized MonitoringLevel getCurrentMonitoringLevel() {
	MonitoringLevel level = MonitoringLevel.OFF;
        /*TODO
    try {
	    Config cfg = ServerBeansFactory.getConfigBean(ApplicationServer.
		getServerContext().getConfigContext());
	    String levelStr = cfg.getMonitoringService().
		getModuleMonitoringLevels().getEjbContainer();
	    level = MonitoringLevel.instance(levelStr); 
	} catch (ConfigException configEx) {
	    _logger.log(Level.WARNING, "ejb_base_stats_mediator_configex",
		    configEx);
	}
	*/

    if (_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE, "[MonitoringMediator] currentLevel: "
		    + monitoringLevelAsString(level));
	}
	return level;
    }

    private final synchronized boolean isMonitoringOff() {
	return (this.currentMonitoringLevel == MonitoringLevel.OFF);
    }

    private final synchronized boolean isMonitoringLow() {
	return (this.currentMonitoringLevel == MonitoringLevel.LOW);
    }

    private final synchronized boolean isMonitoringHigh() {
	return (this.currentMonitoringLevel == MonitoringLevel.HIGH);
    }

    private static final String monitoringLevelAsString(MonitoringLevel level) {
	if (level == MonitoringLevel.OFF) {
	    return "OFF";
	} else if (level == MonitoringLevel.HIGH) {
	    return "HIGH";
	} else {
	    return "LOW";
	}
    }

}
