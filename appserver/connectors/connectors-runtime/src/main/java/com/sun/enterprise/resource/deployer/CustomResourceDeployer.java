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

import com.sun.appserv.connectors.internal.api.*;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.resource.beans.CustomResource;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.List;

import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.repository.ResourceProperty;
import com.sun.appserv.connectors.internal.spi.ResourceDeployer;
import org.glassfish.resource.common.ResourceInfo;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Singleton;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

/**
 * Handles custom resource events in the server instance.
 *
 * The custom resource events from the admin instance are propagated
 * to this object.
 *
 * The methods can potentially be called concurrently, therefore implementation
 * need to be synchronized.
 *
 * <P>
 * Note: Since a notification is not sent to the user of the custom
 *       resources upon undeploy, it is possible that there would be
 *       stale objects not being garbage collected. Future versions
 *       should take care of this problem.
 *
 * @author  Nazrul Islam
 * @since   JDK1.4
 */
@Service
@Scoped(Singleton.class)
public class CustomResourceDeployer implements ResourceDeployer {

    /** Stringmanager for this deployer */
    private static final StringManager localStrings =
        StringManager.getManager(CustomResourceDeployer.class);

    @Inject
    private ResourceNamingService cns;
    /** logger for this deployer */
    private static Logger _logger=LogDomains.getLogger(CustomResourceDeployer.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    public synchronized void deployResource(Object resource, String applicationName, String moduleName)
            throws Exception {
        com.sun.enterprise.config.serverbeans.CustomResource customResource =
                (com.sun.enterprise.config.serverbeans.CustomResource)resource;
        ResourceInfo resourceInfo = new ResourceInfo(customResource.getJndiName(), applicationName, moduleName);
        deployResource(resource, resourceInfo);
    }
    
    /**
     * {@inheritDoc}
     */
	public synchronized void deployResource(Object resource) throws Exception {
        com.sun.enterprise.config.serverbeans.CustomResource customResource =
                (com.sun.enterprise.config.serverbeans.CustomResource)resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(customResource);
        deployResource(customResource, resourceInfo);
    }

    private void deployResource(Object resource, ResourceInfo resourceInfo){

        com.sun.enterprise.config.serverbeans.CustomResource customRes =
            (com.sun.enterprise.config.serverbeans.CustomResource) resource;

        if (ResourcesUtil.createInstance().isEnabled(customRes, resourceInfo)){
            // converts the config data to j2ee resource
            JavaEEResource j2eeResource = toCustomJavaEEResource(customRes, resourceInfo);

            // installs the resource
            installCustomResource((CustomResource) j2eeResource, resourceInfo);

        } else {
            _logger.log(Level.INFO, "core.resource_disabled",
                new Object[] {customRes.getJndiName(),
                              ConnectorConstants.RES_TYPE_CUSTOM});
        }

    }

    /**
     * {@inheritDoc}
     */
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception{
        com.sun.enterprise.config.serverbeans.CustomResource customResource =
            (com.sun.enterprise.config.serverbeans.CustomResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(customResource.getJndiName(), applicationName, moduleName);
        deleteResource(customResource, resourceInfo);
    }
    /**
     * {@inheritDoc}
     */
	public synchronized void undeployResource(Object resource)
            throws Exception {

        com.sun.enterprise.config.serverbeans.CustomResource customResource =
            (com.sun.enterprise.config.serverbeans.CustomResource) resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(customResource);
        deleteResource(customResource, resourceInfo);
    }

    private void deleteResource(com.sun.enterprise.config.serverbeans.CustomResource customResource,
                                ResourceInfo resourceInfo) throws NamingException {
        if (ResourcesUtil.createInstance().isEnabled(customResource, resourceInfo)){
            // converts the config data to j2ee resource
            //JavaEEResource j2eeResource = toCustomJavaEEResource(customRes, resourceInfo);
            // removes the resource from jndi naming
            cns.unpublishObject(resourceInfo, resourceInfo.getName());
        }else{
            _logger.log(Level.FINEST, "core.resource_disabled", new Object[] {customResource.getJndiName(),
                    ConnectorConstants.RES_TYPE_CUSTOM});
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(Object resource){
        return resource instanceof com.sun.enterprise.config.serverbeans.CustomResource;
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
     * Installs the given custom resource. It publishes the resource as a
     * javax.naming.Reference with the naming manager (jndi). This method gets
     * called during server initialization and custom resource deployer to
     * handle custom resource events.
     *
     * @param customRes custom resource
     */
    public void installCustomResource(CustomResource customRes, ResourceInfo resourceInfo) {

        try {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"installCustomResource by jndi-name : "+ resourceInfo);
            }

            // bind a Reference to the object factory
            Reference ref = new Reference(customRes.getResType(), customRes.getFactoryClass(), null);

            // add resource properties as StringRefAddrs
            for (Iterator props = customRes.getProperties().iterator();
                 props.hasNext();) {

                ResourceProperty prop = (ResourceProperty) props.next();

                ref.add(new StringRefAddr(prop.getName(),
                        (String) prop.getValue()));
            }

            // publish the reference
            cns.publishObject(resourceInfo, ref, true);
        } catch (Exception ex) {
            _logger.log(Level.SEVERE, "customrsrc.create_ref_error", resourceInfo);
            _logger.log(Level.SEVERE, "customrsrc.create_ref_error_excp", ex);
        }
    }

    /**
     * Returns a new instance of j2ee custom resource from the given
     * config bean.
     *
     * This method gets called from the custom resource deployer
     * to convert custom-resource config bean into custom j2ee resource.
     *
     * @param    rbean   custom-resource config bean
     *
     * @return   new instance of j2ee custom resource
     */
    public static JavaEEResource toCustomJavaEEResource(
            com.sun.enterprise.config.serverbeans.CustomResource rbean, ResourceInfo resourceInfo) {


        CustomResource jr = new CustomResource(resourceInfo);

        //jr.setDescription(rbean.getDescription()); // FIXME: getting error

        // sets the enable flag
        jr.setEnabled( ConnectorsUtil.parseBoolean(rbean.getEnabled()) );

        // sets the resource type
        jr.setResType( rbean.getResType() );

        // sets the factory class name
        jr.setFactoryClass( rbean.getFactoryClass() );

        // sets the properties
        List<Property> properties = rbean.getProperty();
        if (properties!= null) {
            for(Property property : properties) {
                ResourceProperty rp =
                    new ResourcePropertyImpl(property.getName(), property.getValue());
                jr.addProperty(rp);
            }
        }
        return jr;
    }
}
