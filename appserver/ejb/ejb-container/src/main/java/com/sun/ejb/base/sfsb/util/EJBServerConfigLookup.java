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

package com.sun.ejb.base.sfsb.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;

import com.sun.logging.LogDomains;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.runtime.IASEjbExtraDescriptors;
import com.sun.enterprise.config.serverbeans.*;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jvnet.hk2.component.PerLookup;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.ejb.config.EjbContainer;
import org.glassfish.ejb.config.EjbContainerAvailability;

/**
 * @author lwhite
 * @author Mahesh Kannan
 */
@Service
@Scoped(PerLookup.class)
public final class EJBServerConfigLookup {
   
    /**
     * The default store-pool-jndi-name (used by Ejb Container for
     * Stateful Session Bean checkpointing and passivation to HADB
     */
    private final String DEFAULT_STORE_POOL_JNDI_NAME = "jdbc/hastore";

    /**
     * The default sfsb-ha-persistence-type (used by Ejb Container for
     * Stateful Session Bean checkpointing and passivation to HADB
     */
    private final String DEFAULT_SFSB_HA_PERSISTENCE_TYPE = "file";

    /**
     * The default sfsb-non-ha-persistence-type (used by Ejb Container for
     * Stateful Session Bean checkpointing and passivation to HADB
     */
    private final String DEFAULT_SFSB_NON_HA_PERSISTENCE_TYPE = "file";

    private static final String REPLICATED_TYPE = "replicated";

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    AvailabilityService availabilityService;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    EjbContainer ejbContainer;

    @Inject
    Applications applications;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Server server;

    /**
     * The ejbDescriptor
     */
    private EjbDescriptor _ejbDescriptor = null;
    private boolean _haEnabled = false;

    /**
     * Creates a new instance of EJBServerConfigLookup
     */
    public EJBServerConfigLookup() {
    }

    /**
     * initializes this config object with a specific EJB descriptor
     */
    public void initWithEjbDescriptor(EjbDescriptor ejbDescriptor) {
        _ejbDescriptor = ejbDescriptor;
    }

    /**
     * is monitoring enabled
     */
    private static boolean _isDebugMonitoringEnabled = false;

    static {
        _isDebugMonitoringEnabled = checkDebugMonitoringEnabled();
    }

    /**
     * Is (any) monitoring enabled -- private or public
     * Statistics gathering is based on this value
     */
    public static boolean isMonitoringEnabled() {
        //return (isDebugMonitoringEnabled() || isPublicMonitoringEnabled());
        return isDebugMonitoringEnabled();
    }

    /**
     * Is private (internal) monitoring enabled
     */
    public static boolean isDebugMonitoringEnabled() {
        return _isDebugMonitoringEnabled;
    }

    /**
     * Get the availability-enabled from domain.xml.
     * return false if not found
     */
    public boolean getAvailabilityEnabledFromConfig() {
        if (availabilityService == null) {
            _logger.fine("AvailabilityService was not defined - check domain.xml");
            return false;
        }
        return Boolean.valueOf(availabilityService.getAvailabilityEnabled());
    }

    /**
     * Get the server name from domain.xml.
     * return null if not found
     */
    private String getServerName() {
        return (server != null) ? server.getName() : null;
    }

    /**
     * Get the availability-enabled from domain.xml.
     * This takes into account:
     * global
     * ejb-container-availability
     * j2ee app if not stand-alone
     * ejb-module (if stand-alone)
     * return false if not found
     * FIXME: need to add taking the availability-enabled of the bean itself
     */
    public boolean calculateEjbAvailabilityEnabledFromConfig() {
        _logger.finest("in EJBServerConfigLookup>>calculateEjbAvailabilityEnabledFromConfig");
        boolean isVirtual = this.isVirtualApplication();
        String appName = this.getApplicationName();

        boolean globalAvailability =
                this.getAvailabilityEnabledFromConfig();
        boolean ejbContainerAvailability =
                this.getEjbContainerAvailabilityEnabledFromConfig(globalAvailability);
        boolean ejbDescriptorAvailability = true;
        if (isVirtual) {
            ejbDescriptorAvailability =
                    this.getAvailabilityEnabledFromEjbDescriptor();
            _haEnabled = globalAvailability
                    && ejbContainerAvailability
                    && ejbDescriptorAvailability;
        } else {
            ejbDescriptorAvailability =
                    this.getAvailabilityEnabledFromEjbDescriptor();
            _haEnabled = globalAvailability
                    && ejbContainerAvailability
                    && ejbDescriptorAvailability;
        }

        return _haEnabled;
    }

    public String getPersistenceStoreType() {
        return (_haEnabled)
                ? getSfsbHaPersistenceTypeFromConfig()
                : getSfsbNonHaPersistenceTypeFromConfig();
    }

    /**
     * return whether the bean is a "virtual" app - i.e. a stand-alone
     * ejb module
     */
    private boolean isVirtualApplication() {
        Application application = _ejbDescriptor.getApplication();
        return application.isVirtual();
    }

    /**
     * return the name of the application to which the bean belongs
     * it will be the j2ee app name if the bean is part of a j2ee app
     * it will be the ejb module name if the bean is a stand-alone ejb module
     */
    private String getApplicationName() {
        Application application = _ejbDescriptor.getApplication();
        return application.getRegistrationName();
    }

    /**
     * Get the availability-enabled for the ejb container from domain.xml.
     * return inherited global availability-enabled if not found
     */
    public boolean getEjbContainerAvailabilityEnabledFromConfig() {
        boolean globalAvailabilityEnabled = this.getAvailabilityEnabledFromConfig();
        _logger.finest("in EJBServerConfigLookup>>getEjbContainerAvailabilityEnabledFromConfig");
        EjbContainerAvailability eas = this.getEjbContainerAvailability();
        if (eas == null) {
            _logger.fine("EjbContainerAvailability was not defined - check domain.xml");
            return globalAvailabilityEnabled;
        }

        String easString = eas.getAvailabilityEnabled();
        if (easString == null) {
            return globalAvailabilityEnabled;
        } else {
            return toBoolean(easString);
        }
    }

    /**
     * Get the availability-enabled for the ejb container from domain.xml.
     * return inherited global availability-enabled if not found
     */
    public boolean getEjbContainerAvailabilityEnabledFromConfig(boolean inheritedValue) {
        _logger.finest("in EJBServerConfigLookup>>getEjbContainerAvailabilityEnabledFromConfig");
        EjbContainerAvailability eas = this.getEjbContainerAvailability();
        if (eas == null) {
            _logger.fine("EjbContainerAvailability was not defined - check domain.xml");
            return inheritedValue;
        }

        String easString = eas.getAvailabilityEnabled();
        if (easString == null) {
            return inheritedValue;
        } else {
            return toBoolean(easString);
        }
    }

    /**
     * return the ejb-container-availability element from domain.xml
     */
    private EjbContainerAvailability getEjbContainerAvailability() {
        if (availabilityService == null) {
            return null;
        }
        return availabilityService.getExtensionByType(EjbContainerAvailability.class);
    }

    /**
     * Get the availability-enabled for the bean from sun-ejb-jar.xml.
     * return true if not found
     */
    public boolean getAvailabilityEnabledFromEjbDescriptor() {
        _logger.finest("in EJBServerConfigLookup>>getAvailabilityEnabledFromEjbDescriptor");
        IASEjbExtraDescriptors extraDescriptors =
                _ejbDescriptor.getIASEjbExtraDescriptors();
        if (extraDescriptors == null) {
            return true;
        }
        String availabilityEnabledString =
                extraDescriptors.getAttributeValue(IASEjbExtraDescriptors.AVAILABILITY_ENABLED);

        if (availabilityEnabledString == null) {
            return true;
        } else {
            return toBoolean(availabilityEnabledString);
        }

    }

    /**
     * Get the availability-enabled for the bean from sun-ejb-jar.xml.
     * return defaultValue if not found
     */
    public boolean getAvailabilityEnabledFromEjbDescriptor(boolean inheritedValue) {
        _logger.finest("in EJBServerConfigLookup>>getAvailabilityEnabledFromEjbDescriptor");
        IASEjbExtraDescriptors extraDescriptors =
                _ejbDescriptor.getIASEjbExtraDescriptors();
        if (extraDescriptors == null) {
            return inheritedValue;
        }
        String availabilityEnabledString =
                extraDescriptors.getAttributeValue(IASEjbExtraDescriptors.AVAILABILITY_ENABLED);

        if (availabilityEnabledString == null) {
            return inheritedValue;
        } else {
            return toBoolean(availabilityEnabledString);
        }

    }

    /**
     * Get the store-pool-jndi-name from domain.xml.
     * This is the store-pool-name in <availability-service> element
     * it represents the default for both web & ejb container
     * return DEFAULT_STORE_POOL_JNDI_NAME if not found
     */
    public String getStorePoolJndiNameFromConfig() {
        _logger.finest("in ServerConfigLookup>>getStorePoolJndiNameFromConfig");
        String result = DEFAULT_STORE_POOL_JNDI_NAME;
        if (availabilityService == null) {
            return result;
        }
        String storePoolJndiName = availabilityService.getStorePoolName();
        if (storePoolJndiName != null) {
            result = storePoolJndiName;
        }
        return result;
    }

    /**
     * Get the sfsb-store-pool-name from domain.xml.
     * return DEFAULT_STORE_POOL_JNDI_NAME if not found
     */
    public String getHaStorePoolJndiNameFromConfig() {
        _logger.finest("in EJBServerConfigLookup>>getHaStorePoolJndiNameFromConfig");
        //String result = DEFAULT_STORE_POOL_JNDI_NAME;
        String result = this.getStorePoolJndiNameFromConfig();
        EjbContainerAvailability ejbContainerAvailabilityBean =
                this.getEjbContainerAvailability();
        if (ejbContainerAvailabilityBean == null) {
            return result;
        }
        String result2 = ejbContainerAvailabilityBean.getSfsbStorePoolName();
        if (result2 != null) {
            result = result2;
        }
        return result;
    }

    /**
     * Get the sfsb-ha-persistence-type from domain.xml.
     * return DEFAULT_SFSB_HA_PERSISTENCE_TYPE if not found
     */
    public String getSfsbHaPersistenceTypeFromConfig() {
        _logger.finest("in EJBServerConfigLookup>>getSfsbHaPersistenceTypeFromConfig");
        String result = DEFAULT_SFSB_HA_PERSISTENCE_TYPE;
        EjbContainerAvailability ejbContainerAvailabilityBean =
                this.getEjbContainerAvailability();
        if (ejbContainerAvailabilityBean == null) {
            return result;
        }
        String result2 = ejbContainerAvailabilityBean.getSfsbHaPersistenceType();
        if (result2 != null) {
            result = result2;
        }
        return result;
    }

    /**
     * Get the sfsb-non-ha-persistence-type from domain.xml.
     * return DEFAULT_SFSB_NON_HA_PERSISTENCE_TYPE if not found
     */
    public String getSfsbNonHaPersistenceTypeFromConfig() {
        _logger.finest("in EJBServerConfigLookup>>getSfsbNonHaPersistenceTypeFromConfig");
        String result = DEFAULT_SFSB_NON_HA_PERSISTENCE_TYPE;
        EjbContainerAvailability ejbContainerAvailabilityBean =
                this.getEjbContainerAvailability();
        if (ejbContainerAvailabilityBean == null) {
            return result;
        }

        String result2 = ejbContainerAvailabilityBean.getSfsbPersistenceType();
        if (result2 != null) {
            result = result2;
        }
        return result;
    }

    public static boolean checkDebugMonitoringEnabled() {
        boolean result = false;
        try {
            Properties props = System.getProperties();
            String str = props.getProperty("MONITOR_EJB_CONTAINER");
            if (null != str) {
                if (str.equalsIgnoreCase("TRUE"))
                    result = true;
            }
        } catch (Exception e) {
            //do nothing just return false
        }
        return result;
    }

    /**
     * convert the input value to the appropriate boolean value
     */
    private boolean toBoolean(String value) {
        if (value.equalsIgnoreCase("true"))
            return true;
        if (value.equalsIgnoreCase("yes"))
            return true;
        if (value.equalsIgnoreCase("on"))
            return true;
        if (value.equalsIgnoreCase("1"))
            return true;

        return false;
    }

    private boolean isReplicationTypeMemory() {
        return REPLICATED_TYPE.equalsIgnoreCase(getSfsbHaPersistenceTypeFromConfig());
    }

    public boolean needToAddSFSBVersionInterceptors() {
        boolean isClustered = false;
        boolean isEJBAvailabilityEnabled = false;
        boolean isStoreTypeMemory = false;


        try {

            isEJBAvailabilityEnabled = this.getEjbContainerAvailabilityEnabledFromConfig();
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "EJBSCLookup:: sc.getEjbContainerAvailabilityEnabledFromConfig() ==> "
                                + isEJBAvailabilityEnabled);
            }

            //TODO: find out if clustered

            isStoreTypeMemory = this.isReplicationTypeMemory();
        } catch (Exception ex) {
            _logger.log(Level.FINE,
                    "Got exception in needToAddSFSBVersionInterceptors ("
                            + ex + "). SFSBVersionInterceptors not added");
            _logger.log(Level.FINE, "Exception in needToAddSFSBVersionInterceptors", ex);
        }

        boolean result = isClustered && isEJBAvailabilityEnabled && isStoreTypeMemory;
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "EJBServerConfigLookup::==> isClustered:"
                    + isClustered + " ; isEJBAvailabilityEnabled: "
                    + isEJBAvailabilityEnabled + " ; isStoreTypeMemory ==> "
                    + isStoreTypeMemory + " ; result: " + result);
        }

        return result;
    }

    /**
     * The logger to use for logging ALL web container related messages.
     */
    private static final Logger _logger =
            LogDomains.getLogger(EJBServerConfigLookup.class, LogDomains.EJB_LOGGER);

}
