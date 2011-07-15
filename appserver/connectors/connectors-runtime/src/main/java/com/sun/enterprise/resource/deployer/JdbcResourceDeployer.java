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
import org.glassfish.resource.common.PoolInfo;
import org.glassfish.resource.common.ResourceInfo;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.appserv.connectors.internal.spi.ResourceDeployer;
import com.sun.enterprise.config.serverbeans.JdbcConnectionPool;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.config.serverbeans.JdbcResource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.logging.LogDomains;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Singleton;


/**
 * Handles Jdbc resource events in the server instance. When user adds a
 * jdbc resource, the admin instance emits resource event. The jdbc
 * resource events are propagated to this object.
 * <p/>
 * The methods can potentially be called concurrently, therefore implementation
 * need to be synchronized.
 *
 * @author Nazrul Islam
 * @since JDK1.4
 */
@Service
@Scoped(Singleton.class)
public class JdbcResourceDeployer implements ResourceDeployer {

    @Inject
    private ConnectorRuntime runtime;

    private static final StringManager localStrings =
            StringManager.getManager(JdbcResourceDeployer.class);
    // logger for this deployer
    private static Logger _logger = LogDomains.getLogger(JdbcResourceDeployer.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    public synchronized void deployResource(Object resource, String applicationName, String moduleName)
            throws Exception {
        //deployResource is not synchronized as there is only one caller
        //ResourceProxy which is synchronized

        com.sun.enterprise.config.serverbeans.JdbcResource jdbcRes =
                (com.sun.enterprise.config.serverbeans.JdbcResource) resource;

        String jndiName = jdbcRes.getJndiName();
        String poolName = jdbcRes.getPoolName();
        PoolInfo poolInfo = new PoolInfo(poolName, applicationName, moduleName);
        ResourceInfo resourceInfo = new ResourceInfo(jndiName, applicationName, moduleName);

        if (ResourcesUtil.createInstance().isEnabled(jdbcRes, resourceInfo)){
            runtime.createConnectorResource(resourceInfo, poolInfo, null);
            //In-case the resource is explicitly created with a suffix (__nontx or __PM), no need to create one
            if(ConnectorsUtil.getValidSuffix(jndiName) == null){
                ResourceInfo pmResourceInfo = new ResourceInfo(ConnectorsUtil.getPMJndiName(jndiName),
                        resourceInfo.getApplicationName(), resourceInfo.getModuleName());
                runtime.createConnectorResource( pmResourceInfo, poolInfo, null);
            }
            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("deployed resource " + jndiName);
            }
        } else {
            _logger.log(Level.INFO, "core.resource_disabled",
                    new Object[]{jdbcRes.getJndiName(), ConnectorConstants.RES_TYPE_JDBC});
        }
    }
    /**
     * {@inheritDoc}
     */
    public void deployResource(Object resource) throws Exception {
        JdbcResource jdbcRes = (JdbcResource) resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(jdbcRes);
        deployResource(jdbcRes, resourceInfo.getApplicationName(), resourceInfo.getModuleName());
    }

    /**
     * {@inheritDoc}
     */
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception{
        JdbcResource jdbcRes = (JdbcResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(jdbcRes.getJndiName(), applicationName, moduleName);
        deleteResource(jdbcRes, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void undeployResource(Object resource)
            throws Exception {
        JdbcResource jdbcRes = (JdbcResource) resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(jdbcRes);
        deleteResource(jdbcRes, resourceInfo);
    }

    private void deleteResource(JdbcResource jdbcResource, ResourceInfo resourceInfo) throws Exception {

        if (ResourcesUtil.createInstance().isEnabled(jdbcResource, resourceInfo)) {

            runtime.deleteConnectorResource(resourceInfo);
            ConnectorRegistry.getInstance().removeResourceFactories(resourceInfo);
            //In-case the resource is explicitly created with a suffix (__nontx or __PM), no need to delete one
            if (ConnectorsUtil.getValidSuffix(resourceInfo.getName()) == null) {
                String pmJndiName = ConnectorsUtil.getPMJndiName(resourceInfo.getName());
                ResourceInfo pmResourceInfo = new ResourceInfo(pmJndiName, resourceInfo.getApplicationName(),
                        resourceInfo.getModuleName());
                runtime.deleteConnectorResource(pmResourceInfo);
                ConnectorRegistry.getInstance().removeResourceFactories(pmResourceInfo);
            }

            //Since 8.1 PE/SE/EE - if no more resource-ref to the pool
            //of this resource in this server instance, remove pool from connector
            //runtime
            checkAndDeletePool(jdbcResource);
        } else {
            _logger.log(Level.FINEST, "core.resource_disabled", new Object[]{jdbcResource.getJndiName(),
                    ConnectorConstants.RES_TYPE_JDBC});
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void redeployResource(Object resource)
            throws Exception {

        undeployResource(resource);
        deployResource(resource);
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(Object resource){
        return resource instanceof JdbcResource;
    }

    /**
     * @inheritDoc
     */
    public Class[] getProxyClassesForDynamicReconfiguration() {
        return new Class[0];
    }

    /**
     * @inheritDoc
     */
    public boolean supportsDynamicReconfiguration() {
        return false;
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
    public synchronized void disableResource(Object resource) throws Exception {
        undeployResource(resource);
    }

    /**
     * Checks if no more resource-refs to resources exists for the
     * JDBC connection pool and then deletes the pool
     * @param cr Jdbc Resource Config bean
     * @throws Exception if unable to access configuration/undeploy resource.
     * @since 8.1 pe/se/ee
     */
    private void checkAndDeletePool(JdbcResource cr) throws Exception {
        String poolName = cr.getPoolName();
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(cr);
        PoolInfo poolInfo = new PoolInfo(poolName, resourceInfo.getApplicationName(), resourceInfo.getModuleName());
        Resources resources = (Resources) cr.getParent();
        //Its possible that the JdbcResource here is a DataSourceDefinition. Ignore optimization.
        if(resources != null){
            try {
                boolean poolReferred =
                    ResourcesUtil.createInstance().isJdbcPoolReferredInServerInstance(poolInfo);
                if (!poolReferred) {
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.fine("Deleting JDBC pool [" + poolName + " ] as there are no more " +
                            "resource-refs to the pool in this server instance");
                    }

                    JdbcConnectionPool jcp = (JdbcConnectionPool)
                            resources.getResourceByName(JdbcConnectionPool.class, poolName);
                    //Delete/Undeploy Pool
                    runtime.getResourceDeployer(jcp).undeployResource(jcp);
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
}
