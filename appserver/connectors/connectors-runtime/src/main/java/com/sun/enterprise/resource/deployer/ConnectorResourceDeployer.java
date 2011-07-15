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

package com.sun.enterprise.resource.deployer;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import org.glassfish.resource.common.PoolInfo;
import org.glassfish.resource.common.ResourceInfo;
import com.sun.enterprise.config.serverbeans.ConnectorResource;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.logging.LogDomains;
import com.sun.appserv.connectors.internal.spi.ResourceDeployer;
import com.sun.enterprise.config.serverbeans.ConnectorConnectionPool;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.connectors.util.ResourcesUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Singleton;

/**
 * @author Srikanth P
 */
@Service
@Scoped(Singleton.class)
public class ConnectorResourceDeployer implements ResourceDeployer {

    @Inject
    private ConnectorRuntime runtime;
    private static Logger _logger = LogDomains.getLogger(ConnectorResourceDeployer.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    public synchronized void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        //deployResource is not synchronized as there is only one caller
        //ResourceProxy which is synchronized
        ConnectorResource domainResource = (ConnectorResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(domainResource.getJndiName(), applicationName, moduleName);
        PoolInfo poolInfo = new PoolInfo(domainResource.getPoolName(), applicationName, moduleName);
        createConnectorResource(domainResource, resourceInfo, poolInfo);
    }
    
    /**
     * {@inheritDoc}
     */
    public void deployResource(Object resource) throws Exception {
        //deployResource is not synchronized as there is only one caller
        //ResourceProxy which is synchronized
        ConnectorResource domainResource = (ConnectorResource) resource;
        String poolName = domainResource.getPoolName();
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(domainResource);
        PoolInfo poolInfo = new PoolInfo(poolName, resourceInfo.getApplicationName(), resourceInfo.getModuleName());
        createConnectorResource(domainResource, resourceInfo, poolInfo);
    }

    private void createConnectorResource(ConnectorResource connectorResource, ResourceInfo resourceInfo,
                                         PoolInfo poolInfo) throws ConnectorRuntimeException {
        if (ResourcesUtil.createInstance().isEnabled(connectorResource, resourceInfo)){
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Calling backend to add connector resource",
                        resourceInfo);
            }
            runtime.createConnectorResource(resourceInfo, poolInfo, null);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Added connector resource in backend",
                        resourceInfo);
            }
        } else {
            _logger.log(Level.INFO, "core.resource_disabled",
                    new Object[]{connectorResource.getJndiName(), ConnectorConstants.RES_TYPE_CR});

        }
    }

    /**
     * {@inheritDoc}
     */
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception{
        ConnectorResource domainResource = (ConnectorResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(domainResource.getJndiName(), applicationName, moduleName);
        deleteConnectorResource(domainResource, resourceInfo);
    }
    /**
     * {@inheritDoc}
     */
    public synchronized void undeployResource(Object resource)
            throws Exception {
        ConnectorResource domainResource = (ConnectorResource) resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(domainResource);
        deleteConnectorResource(domainResource, resourceInfo);
    }

    private void deleteConnectorResource(ConnectorResource connectorResource, ResourceInfo resourceInfo)
            throws Exception {

        if (ResourcesUtil.createInstance().isEnabled(connectorResource, resourceInfo)) {
            runtime.deleteConnectorResource(resourceInfo);

            //Since 8.1 PE/SE/EE - if no more resource-ref to the pool
            //of this resource in this server instance, remove pool from connector
            //runtime
            checkAndDeletePool(connectorResource);
        } else {
            _logger.log(Level.FINEST, "core.resource_disabled", new Object[]{connectorResource.getJndiName(),
                    ConnectorConstants.RES_TYPE_CR});
        }
    }

    /**
     * {@inheritDoc}
     */
    public void redeployResource(Object resource) throws Exception {
        undeployResource(resource);
        deployResource(resource);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void disableResource(Object resource)
                  throws Exception {
        undeployResource(resource);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void enableResource(Object resource) throws Exception {
        deployResource(resource);
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(Object resource){
        return resource instanceof ConnectorResource;
    }

    /**
     * @inheritDoc
     */
    public boolean supportsDynamicReconfiguration() {
        return false;
    }

    /**
     * @inheritDoc
     */
    public Class[] getProxyClassesForDynamicReconfiguration() {
        return new Class[0];
    }

    /**
     * Checks if no more resource-refs to resources exists for the
     * connector connection pool and then deletes the pool
     * @param cr ConnectorResource
     * @throws Exception (ConfigException / undeploy exception)
     * @since 8.1 pe/se/ee
     */
    private void checkAndDeletePool(ConnectorResource cr) throws Exception {
        String poolName = cr.getPoolName();
        try {
            ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(cr);
            ConnectorConnectionPool ccp = ResourcesUtil.createInstance().getConnectorConnectionPoolOfResource(resourceInfo);
            PoolInfo poolInfo = ConnectorsUtil.getPoolInfo(ccp);

            boolean poolReferred =
                ResourcesUtil.createInstance().isPoolReferredInServerInstance(poolInfo);
            if (!poolReferred) {
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Deleting pool [" + poolName + "] as there are no more " +
                        "resource-refs to the pool in this server instance");
                }
                //Delete/Undeploy Pool
                runtime.getResourceDeployer(ccp).undeployResource(ccp);
            }
        } catch (Exception ce) {
            _logger.warning(ce.getMessage());
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("Exception while deleting pool [ "+poolName+" ] : " + ce );
            }
            throw ce;
        }
    }
}
