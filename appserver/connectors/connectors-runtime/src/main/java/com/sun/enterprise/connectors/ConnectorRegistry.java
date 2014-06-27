/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors;

import com.sun.enterprise.connectors.authentication.RuntimeSecurityMap;
import com.sun.enterprise.connectors.module.ConnectorApplication;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.resource.DynamicallyReconfigurableResource;
import com.sun.logging.LogDomains;
import org.glassfish.connectors.config.ResourceAdapterConfig;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;

import javax.resource.spi.ManagedConnectionFactory;
import javax.validation.Validator;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.*;


/**
 * This is an registry class which holds various objects in hashMaps,
 * hash tables, and vectors. These objects are updated and queried
 * during various funtionalities of rar deployment/undeployment, resource
 * creation/destruction
 * Ex. of these objects are ResourcesAdapter instances, security maps for
 * pool managed connection factories.
 * It follows singleton pattern. i.e only one instance at any point of time.
 *
 * @author Binod P.G and Srikanth P
 */

public class ConnectorRegistry {

    static final Logger _logger = LogDomains.getLogger(ConnectorRegistry.class, LogDomains.RSR_LOGGER);

    protected static final ConnectorRegistry connectorRegistryInstance = new ConnectorRegistry();

    /**
     * <code>resourceAdapters</code> keeps track of all active resource
     * adapters in the connector runtime.
     * String:resourceadapterName Vs ActiveResourceAdapter
     */
    protected final Map<String, ActiveResourceAdapter> resourceAdapters;

    protected final Map<PoolInfo, PoolMetaData> factories;
    protected final Map<String, ResourceAdapterConfig> resourceAdapterConfig;
    protected final Map<String, ConnectorApplication> rarModules;
    protected final Map<String, Validator> beanValidators;
    protected final ConcurrentMap<ResourceInfo, AtomicLong> resourceInfoVersion;
    protected final Set<ResourceInfo> resourceInfos;
    protected final Set<PoolInfo> transparentDynamicReconfigPools;
    protected final Map<String, Object> locks;

    /**
     * Return the ConnectorRegistry instance
     *
     * @return ConnectorRegistry instance which is a singleton
     */
    public static ConnectorRegistry getInstance() {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("returning the connector registry");
        }
        return connectorRegistryInstance;
    }

    /**
     * Protected constructor.
     * It is protected as it follows singleton pattern.
     */

    protected ConnectorRegistry() {
        resourceAdapters = Collections.synchronizedMap(new HashMap<String, ActiveResourceAdapter>());
        factories = Collections.synchronizedMap(new HashMap<PoolInfo, PoolMetaData>());
        resourceAdapterConfig = Collections.synchronizedMap(new HashMap<String, ResourceAdapterConfig>());
        rarModules = Collections.synchronizedMap(new HashMap<String, ConnectorApplication>());
        beanValidators = Collections.synchronizedMap(new HashMap<String, Validator>());
        resourceInfoVersion = new ConcurrentHashMap<ResourceInfo, AtomicLong>();
        resourceInfos = new HashSet<ResourceInfo>();
        transparentDynamicReconfigPools = new HashSet<PoolInfo>();
        locks = new HashMap<String, Object>();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "initialized the connector registry");
        }
    }

    /**
     * Adds the object implementing ActiveResourceAdapter
     * interface to the registry.
     *
     * @param rarModuleName RarName which is the key
     * @param rar           ActiveResourceAdapter instance which is the value.
     */

    public void addActiveResourceAdapter(String rarModuleName,
                                         ActiveResourceAdapter rar) {
        resourceAdapters.put(rarModuleName, rar);
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,
                    "Added the active resource adapter to connector registry",
                    rarModuleName);
        }
    }

    /**
     * get the version counter of  a resource info 
     * @param resourceInfo resource-name
     * @return version counter. {@code -1L} if the resource is invalid
     */
    public long getResourceInfoVersion(ResourceInfo resourceInfo) {
    	AtomicLong version = resourceInfoVersion.get(resourceInfo);
    	if (version == null) {
    	   // resource is no longer valid
    	   return -1L;
    	} else {
    		return version.get();
    	}
    }
    
    /**
     * Update version information for a resource.
     * @param resourceInfo resource info to be updated.
     * @return new version couter
     */
    public long updateResourceInfoVersion(ResourceInfo resourceInfo) {
    	AtomicLong version = resourceInfoVersion.get(resourceInfo);
    	if (version == null) {
    		AtomicLong newVersion = new AtomicLong();
    		version = resourceInfoVersion.putIfAbsent(resourceInfo, newVersion);
    	   	if (version == null) {
    	      version = newVersion;
    		}
    	}    	
    	return version.incrementAndGet();
    }

    /**
     * remove and invalidate factories (proxy to actual factory) using the resource.
     * @param resourceInfo resource-name
     * @return boolean indicating whether the factories will get invalidated
     */
    public boolean removeResourceFactories(ResourceInfo resourceInfo){
    	resourceInfoVersion.remove(resourceInfo);
    	return false; // we actually don't know if there are any resource factories instantiated.
    }

    /**
     * Add resourceInfo that is deployed for book-keeping purposes.
     * @param resourceInfo Resource being deployed.
     */
    public void addResourceInfo(ResourceInfo resourceInfo){
        if(resourceInfo != null){
            synchronized (resourceInfos){
                resourceInfos.add(resourceInfo);
            }
            updateResourceInfoVersion(resourceInfo);
        }
    }

    /**
     * Remove ResourceInfo from registry. Called when resource is disabled/undeployed.
     * @param resourceInfo ResourceInfo
     * @return boolean indicating whether resource exists and removed.
     */
    public boolean removeResourceInfo(ResourceInfo resourceInfo){
        boolean removed = false;
        if(resourceInfo != null){
            synchronized (resourceInfos){
                removed = resourceInfos.remove(resourceInfo);
            }
            resourceInfoVersion.remove(resourceInfo);
        }
        return removed;
    }

    /**
     * indicates whether the resource is deployed (enabled)
     * @param resourceInfo resource-info
     * @return boolean indicating whether the resource is deployed.
     */
    public boolean isResourceDeployed(ResourceInfo resourceInfo){
        boolean isDeployed = false;
        if(resourceInfo != null){
            isDeployed = resourceInfos.contains(resourceInfo);
        }
        return isDeployed;
    }

    /**
     * Add PoolInfo that has transparent-dynamic-reconfiguration enabled .
     * @param poolInfo Pool being deployed.
     */
    public void addTransparentDynamicReconfigPool(PoolInfo poolInfo){
        if(poolInfo != null){
            synchronized (transparentDynamicReconfigPools){
                transparentDynamicReconfigPools.add(poolInfo);
            }
        }
    }

    /**
     * Remove ResourceInfo from registry. Called when resource is disabled/undeployed.
     * @param poolInfo poolInfo
     * @return boolean indicating whether the pool exists and removed.
     */
    public boolean removeTransparentDynamicReconfigPool(PoolInfo poolInfo){
        boolean removed = false;
        if(poolInfo != null){
            synchronized (transparentDynamicReconfigPools){
                removed = transparentDynamicReconfigPools.remove(poolInfo);
            }
        }
        return removed;
    }

    /**
     * indicates whether the pool has transparent-dynamic-reconfiguration property enabled
     * @param poolInfo poolInfo
     * @return boolean false if pool is not deployed
     */
    public boolean isTransparentDynamicReconfigPool(PoolInfo poolInfo){
        boolean isDeployed = false;
        if(poolInfo != null){
            isDeployed = transparentDynamicReconfigPools.contains(poolInfo);
        }
        return isDeployed;
    }

    /**
     * Removes the object implementing ActiveResourceAdapter
     * interface from the registry.
     * This method is called whenever an active connector module
     * is removed from the Connector runtime. [eg. undeploy/recreate etc]
     *
     * @param rarModuleName RarName which is the key
     * @return true if successfully removed
     *         false if deletion fails.
     */

    public boolean removeActiveResourceAdapter(String rarModuleName) {
        Object o = resourceAdapters.remove(rarModuleName);

        if (o == null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("Failed to remove the resource adapter from connector registry" +
                    rarModuleName);
            }
            return false;
        } else {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("removed the active resource adapter from connector registry" +
                    rarModuleName);
            }
            return true;
        }
    }

    /**
     * Retrieves the object implementing ActiveResourceAdapter interface
     * from the registry. Key is the rarName.
     *
     * @param rarModuleName Rar name. It is the key
     * @return object implementing ActiveResourceAdapter interface
     */

    public ActiveResourceAdapter getActiveResourceAdapter(
            String rarModuleName) {
        if (rarModuleName != null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine(
                    "returning/found the resource adapter from connector registry " +
                            rarModuleName);
            }
            return resourceAdapters.get(rarModuleName);
        } else {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine(
                    "resource-adapter not found in connector registry. Returning null");
            }
            return null;
        }
    }

    /**
     * lock object that will be used by ResourceAdapterAdminService
     * to avoid multiple calls to create ActiveRA for same resource-adapter
     * @param rarName resource-adapter name
     * @return lock object for the resource-adapter
     */
    public Object getLockObject(String rarName) {
        if (rarName == null) {
            return null;
        }
        Object lock;
        synchronized (locks) {
            lock = locks.get(rarName);
            if (lock == null) {
                lock = new Object();
                locks.put(rarName, lock);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("added lock-object [ " + lock + " ] for rar : " + rarName);
                }
            }
        }
        return lock;
    }

    /**
     * removes the lock-object used for creating the ActiveRA for a particular RAR
     * @param rarName resource-adapter
     */
    public void removeLockObject(String rarName) {
        if (rarName == null) {
            return;
        }
        synchronized (locks) {
            Object lock = locks.remove(rarName);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("removed lock-object [ " + lock + " ] for rar : " + rarName);
            }
        }
    }


    /**
     * Adds the bean validator to the registry.
     *
     * @param rarModuleName RarName which is the key
     * @param validator to be added to registry
     */
    public void addBeanValidator(String rarModuleName, Validator validator){
        beanValidators.put(rarModuleName, validator);
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Added the bean validator for RAR [ "+rarModuleName+" ] to connector registry");
        }
    }

    /**
     * Retrieves the bean validator of a resource-adapter
     * from the registry. Key is the rarName.
     *
     * @param rarModuleName Rar name. It is the key
     * @return bean validator
     */
    public Validator getBeanValidator(String rarModuleName){
        if (rarModuleName != null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine(
                    "returning/found the validator for RAR [ "+rarModuleName+" ] from connector registry");
            }
            return beanValidators.get(rarModuleName);
        } else {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine(
                    "bean validator for RAR not found in connector registry.Returning null");
            }
            return null;
        }
    }

    /**
     * Removes the bean validator of a resource-adapter
     * from the registry.
     * This method is called whenever an active connector module
     * is removed from the Connector runtime. [eg. undeploy/recreate etc]
     *
     * @param rarModuleName RarName which is the key
     * @return true if successfully removed
     *         false if deletion fails.
     */
    public boolean removeBeanValidator(String rarModuleName) {
        Object o = beanValidators.remove(rarModuleName);

        if (o == null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("Failed to remove the bean validator for RAR [ "+rarModuleName+" ] from connector registry");
            }
            return false;
        } else {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("removed the active bean validator for RAR [ "+rarModuleName +" ] from connector registry");
            }
            return true;
        }
    }

    /**
     * Checks if the MCF pertaining to the pool is instantiated and present
     * in the registry. Each pool has its own MCF instance.
     *
     * @param poolInfo Name of the pool
     * @return true if the MCF is found.
     *         false if MCF is not found
     */


    public boolean isMCFCreated(PoolInfo poolInfo) {
        boolean created = factories.containsKey(poolInfo);
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("isMCFCreated " + poolInfo + " - " + created);
        }
        return created;
    }


    /**
     * Remove MCF instance pertaining to the poolName from the registry.
     *
     * @param poolInfo Name of the pool
     * @return true if successfully removed.
     *         false if removal fails.
     */

    public boolean removeManagedConnectionFactory(PoolInfo poolInfo) {
        if (factories.remove(poolInfo) == null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "Failed to remove the MCF from connector registry.", poolInfo);
            }
            return false;
        } else {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("removeMCF " + poolInfo + " - " + !factories.containsKey(poolInfo));
            }
            return true;
        }
    }

    /**
     * Add MCF instance pertaining to the poolName to the registry.
     *
     * @param poolInfo Name of the pool
     * @param pmd      MCF instance to be added.
     */
    public void addManagedConnectionFactory(PoolInfo poolInfo,
                                            PoolMetaData pmd) {
        factories.put(poolInfo, pmd);
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("Added MCF to connector registry for: " + poolInfo);
        }
    }

    /**
     * Retrieve MCF instance pertaining to the poolName from the registry.
     *
     * @param poolInfo Name of the pool
     * @return factory MCF instance retrieved.
     */


    public ManagedConnectionFactory getManagedConnectionFactory(
            PoolInfo poolInfo) {
        if (poolInfo != null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "Returning the MCF from connector registry.", poolInfo);
            }

            PoolMetaData pmd = factories.get(poolInfo);
            if (pmd != null) {
                return pmd.getMCF();
            }

        }
        return null;
    }

    /**
     * Checks whether the rar is already deployed i.e registered with
     * connector registry
     *
     * @param rarModuleName rar Name.
     * @return true if rar is registered
     *         false if rar is not registered.
     */

    public boolean isRegistered(String rarModuleName) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,
                "Checking for MCF presence in connector registry.", rarModuleName);
        }
        return resourceAdapters.containsKey(rarModuleName);
    }

    /**
     * Gets the connector descriptor pertaining the rar
     *
     * @param rarModuleName rarName
     * @return ConnectorDescriptor which represents the ra.xml
     */

    public ConnectorDescriptor getDescriptor(String rarModuleName) {
        ActiveResourceAdapter ar = null;
        if (rarModuleName != null) {
            ar = resourceAdapters.get(rarModuleName);
        }
        if (ar != null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "Found/returing Connector descriptor in connector registry.",
                    rarModuleName);
            }
            return ar.getDescriptor();
        } else {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "Couldnot find Connector descriptor in connector registry.",
                    rarModuleName);
            }
            return null;
        }
    }

    /** Gets the runtime equivalent of policies enforced by the Security Maps 
     *  pertaining to a pool from the Pool's Meta Data.   
     *  @param poolInfo pool information
     *  @return runtimeSecurityMap in the form of HashMap of
     *   HashMaps (user and groups). 
     *  @see SecurityMapUtils.processSecurityMaps( SecurityMap[])
     */


    public RuntimeSecurityMap getRuntimeSecurityMap(PoolInfo poolInfo) {
        if(poolInfo != null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Returing the security map from connector registry.", poolInfo);
            }
            PoolMetaData pmd = factories.get(poolInfo);
            return pmd.getRuntimeSecurityMap();
        } else {
            return null;
        }
    }

    /**
     * Get the resource adapter config properties object registered with
     * registry against the rarName.
     *
     * @param rarName Name of the rar
     * @return ResourceAdapter configuration object
     */

    public ResourceAdapterConfig getResourceAdapterConfig(String rarName) {
        if (rarName != null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "Returing the resourceadapter Config from registry.", rarName);
            }
            return resourceAdapterConfig.get(rarName);
        } else {
            return null;
        }
    }

    /**
     * Add the resource adapter config properties object to registry
     * against the rarName.
     *
     * @param rarName  Name of the rar
     * @param raConfig ResourceAdapter configuration object
     */

    public void addResourceAdapterConfig(String rarName,
                                         ResourceAdapterConfig raConfig) {
        if (rarName != null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "Adding the resourceAdapter Config to connector registry.",
                    rarName);
            }
            resourceAdapterConfig.put(rarName, raConfig);
        }
    }

    /**
     * Remove the resource adapter config properties object from registry
     *
     * @param rarName Name of the rar
     * @return true if successfully deleted
     *         false if deletion fails
     */

    public boolean removeResourceAdapterConfig(String rarName) {
        if (resourceAdapterConfig.remove(rarName) == null) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "failed to remove the resourceAdapter config from registry.",
                    rarName);
            }
            return false;
        } else {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "Removed the resourceAdapter config map from registry.",
                    rarName);
            }
            return true;
        }
    }

    /**
     * Returns all Active Resource Adapters in the connector runtime.
     *
     * @return All active resource adapters in the connector runtime
     */
    public ActiveResourceAdapter[] getAllActiveResourceAdapters() {
        ActiveResourceAdapter araArray[]  = new ActiveResourceAdapter[this.resourceAdapters.size()];
        return this.resourceAdapters.values().toArray(araArray);
    }

    public PoolMetaData getPoolMetaData(PoolInfo poolInfo) {
        return factories.get(poolInfo);
    }

    /**
     * register a connector application (rarModule) with the registry
     * @param rarModule resource-adapter module
     */
    public void addConnectorApplication(ConnectorApplication rarModule){
        rarModules.put(rarModule.getModuleName(), rarModule);
    }

    /**
     * retrieve a connector application (rarModule) from the registry
     * @param rarName resource-adapter name
     * @return ConnectorApplication app
     */
    public ConnectorApplication getConnectorApplication(String rarName){
        return rarModules.get(rarName);
    }

    /**
     * remove a connector application (rarModule) from the registry
     * @param rarName resource-adapter module
     */
    public void removeConnectorApplication(String rarName){
        rarModules.remove(rarName);
    }

    /**
     * get the list of resource-adapters that support this message-listener-type
     * @param messageListener message-listener class-name
     * @return List of resource-adapters
     */
    public List<String> getConnectorsSupportingMessageListener(String messageListener){

        List<String> rars = new ArrayList<String>();
        for(ActiveResourceAdapter ara : resourceAdapters.values()){
            ConnectorDescriptor desc = ara.getDescriptor();
            if(desc.getInBoundDefined()){
                if(desc.getInboundResourceAdapter().getMessageListener(messageListener) != null){
                    rars.add(ara.getModuleName());
                }
            }
        }
        return rars;
    }
}
