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

package com.sun.enterprise.resource.deployer;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.logging.LogDomains;
import org.glassfish.connectors.config.AdminObjectResource;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Srikanth P
 */

@Service
@ResourceDeployerInfo(AdminObjectResource.class)
@Singleton
public class AdminObjectResourceDeployer extends AbstractConnectorResourceDeployer {

    @Inject
    private ConnectorRuntime runtime;

    private static Logger _logger = LogDomains.getLogger(AdminObjectResourceDeployer.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    public synchronized void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        final AdminObjectResource aor = (AdminObjectResource) resource;
        String jndiName = aor.getJndiName();
        ResourceInfo resourceInfo = new ResourceInfo(jndiName, applicationName, moduleName);
        createAdminObjectResource(aor, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void deployResource(Object resource) throws Exception {

        final AdminObjectResource aor = (AdminObjectResource) resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(aor);
        createAdminObjectResource(aor, resourceInfo);
    }

    private void createAdminObjectResource(AdminObjectResource aor, ResourceInfo resourceInfo)
            throws ConnectorRuntimeException {
        /* TODO Not needed any more ?

                        if (aor.isEnabled()) {
                            //registers the jsr77 object for the admin object resource deployed
                            final ManagementObjectManager mgr =
                                getAppServerSwitchObject().getManagementObjectManager();
                            mgr.registerAdminObjectResource(jndiName,
                                aor.getResAdapter(), aor.getResType(),
                                getPropNamesAsStrArr(aor.getElementProperty()),
                                getPropValuesAsStrArr(aor.getElementProperty()));
                        } else {
                                _logger.log(Level.INFO, "core.resource_disabled",
                                        new Object[] {jndiName,
                                        IASJ2EEResourceFactoryImpl.JMS_RES_TYPE});
                        }
                */

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Calling backend to add adminObject", resourceInfo);
        }
        runtime.addAdminObject(null, aor.getResAdapter(), resourceInfo,
                aor.getResType(), aor.getClassName(), transformProps(aor.getProperty()));
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Added adminObject in backend", resourceInfo);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception {
        final AdminObjectResource aor = (AdminObjectResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(aor.getJndiName(), applicationName, moduleName);
        deleteAdminObjectResource(aor, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void undeployResource(Object resource)
            throws Exception {
        final AdminObjectResource aor = (AdminObjectResource) resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(aor);
        deleteAdminObjectResource(aor, resourceInfo);
    }

    private void deleteAdminObjectResource(AdminObjectResource adminObject, ResourceInfo resourceInfo)
            throws ConnectorRuntimeException {

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Calling backend to delete adminObject", resourceInfo);
        }
        runtime.deleteAdminObject(resourceInfo);
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Deleted adminObject in backend", resourceInfo);
        }

        //unregister the managed object
        /* TODO Not needed any more ?
            final ManagementObjectManager mgr =
                    getAppServerSwitchObject().getManagementObjectManager();
            mgr.unregisterAdminObjectResource(aor.getJndiName(), aor.getResType());
        */
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(Object resource) {
        return resource instanceof AdminObjectResource;
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
    public synchronized void disableResource(Object resource)
            throws Exception {
        undeployResource(resource);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void enableResource(Object resource)
            throws Exception {
        deployResource(resource);
    }

    private Properties transformProps(List<Property> domainProps) {

        Properties props = new Properties();
        for (Property domainProp : domainProps) {
            props.setProperty(domainProp.getName(), domainProp.getValue());
        }
        return props;
    }
}
