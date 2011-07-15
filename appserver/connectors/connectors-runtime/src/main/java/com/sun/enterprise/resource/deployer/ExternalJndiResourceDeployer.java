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
import com.sun.enterprise.resource.beans.ExternalJndiResource;
import com.sun.enterprise.resource.naming.JndiProxyObjectFactory;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import com.sun.enterprise.resource.naming.SerializableObjectRefAddr;
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.repository.ResourceProperty;
import com.sun.appserv.connectors.internal.spi.ResourceDeployer;
import org.glassfish.resource.common.ResourceInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.config.types.Property;
import org.glassfish.api.naming.GlassfishNamingManager;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.InitialContextFactory;

/**
 * Handles external-jndi resource events in the server instance.
 *
 * The external-jndi resource events from the admin instance are propagated
 * to this object.
 *
 * The methods can potentially be called concurrently, therefore implementation
 * need to be synchronized.
 *
 * @author  Nazrul Islam
 * @since   JDK1.4
 */
@Service
@Scoped(Singleton.class)
public class ExternalJndiResourceDeployer implements ResourceDeployer {

    @Inject
    private ResourceNamingService namingService;

    /** StringManager for this deployer */
    private static final StringManager localStrings =
        StringManager.getManager(ExternalJndiResourceDeployer.class);
    /** logger for this deployer */
    private static Logger _logger=LogDomains.getLogger(ExternalJndiResourceDeployer.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    public synchronized void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        com.sun.enterprise.config.serverbeans.ExternalJndiResource jndiRes =
                (com.sun.enterprise.config.serverbeans.ExternalJndiResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(jndiRes.getJndiName(), applicationName, moduleName);
        createExternalJndiResource(jndiRes, resourceInfo);
    }
    
    /**
     * {@inheritDoc}
     */
	public synchronized void deployResource(Object resource) throws Exception {

        com.sun.enterprise.config.serverbeans.ExternalJndiResource jndiRes =
                (com.sun.enterprise.config.serverbeans.ExternalJndiResource) resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(jndiRes);
        createExternalJndiResource(jndiRes, resourceInfo);
    }

    private void createExternalJndiResource(com.sun.enterprise.config.serverbeans.ExternalJndiResource jndiRes,
                                            ResourceInfo resourceInfo) {
        if (ResourcesUtil.createInstance().isEnabled(jndiRes, resourceInfo)){
            // converts the config data to j2ee resource
            JavaEEResource j2eeRes = toExternalJndiJavaEEResource(jndiRes, resourceInfo);

            // installs the resource
            installExternalJndiResource((ExternalJndiResource) j2eeRes, resourceInfo);

        } else {
            _logger.log(Level.INFO, "core.resource_disabled",
                new Object[] {jndiRes.getJndiName(),
                              ConnectorConstants.EXT_JNDI_RES_TYPE});
        }

    }

    /**
     * {@inheritDoc}
     */
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception{
        com.sun.enterprise.config.serverbeans.ExternalJndiResource jndiRes =
            (com.sun.enterprise.config.serverbeans.ExternalJndiResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(jndiRes.getJndiName(), applicationName, moduleName);
        deleteResource(jndiRes, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
	public synchronized void undeployResource(Object resource)
            throws Exception {

        com.sun.enterprise.config.serverbeans.ExternalJndiResource jndiRes =
            (com.sun.enterprise.config.serverbeans.ExternalJndiResource) resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(jndiRes);
        deleteResource(jndiRes, resourceInfo);
    }

    private void deleteResource(com.sun.enterprise.config.serverbeans.ExternalJndiResource jndiResource,
                                ResourceInfo resourceInfo) {
        if (ResourcesUtil.createInstance().isEnabled(jndiResource, resourceInfo)){
            // converts the config data to j2ee resource
            JavaEEResource j2eeResource = toExternalJndiJavaEEResource(jndiResource, resourceInfo);
            // un-installs the resource
            uninstallExternalJndiResource(j2eeResource, resourceInfo);
        }else{
            _logger.log(Level.FINEST, "core.resource_disabled", new Object[] {jndiResource.getJndiName(),
                    ConnectorConstants.RES_TYPE_EXTERNAL_JNDI});
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
        return resource instanceof com.sun.enterprise.config.serverbeans.ExternalJndiResource;
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
     * Installs the given external jndi resource. This method gets called
     * during server initialization and from external jndi resource
     * deployer to handle resource events.
     *
     * @param extJndiRes external jndi resource
     */
    public void installExternalJndiResource(ExternalJndiResource extJndiRes, ResourceInfo resourceInfo) {

        try {
            // create the external JNDI factory, its initial context and
            // pass them as references.
            String factoryClass = extJndiRes.getFactoryClass();
            String jndiLookupName = extJndiRes.getJndiLookupName();

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "installExternalJndiResources resourceName "
                        + resourceInfo + " factoryClass " + factoryClass
                        + " jndiLookupName = " + jndiLookupName);
            }


            Object factory = ConnectorsUtil.loadObject(factoryClass);
            if (factory == null) {
                _logger.log(Level.WARNING, "jndi.factory_load_error", factoryClass);
                return;

            } else if (!(factory instanceof javax.naming.spi.InitialContextFactory)) {
                _logger.log(Level.WARNING, "jndi.factory_class_unexpected", factoryClass);
                return;
            }

            // Get properties to create the initial naming context
            // for the target JNDI factory
            Hashtable env = new Hashtable();
            for (Iterator props = extJndiRes.getProperties().iterator();
                 props.hasNext();) {

                ResourceProperty prop = (ResourceProperty) props.next();
                env.put(prop.getName(), prop.getValue());
            }

            Context context = null;
            try {
                context =
                        ((InitialContextFactory) factory).getInitialContext(env);

            } catch (NamingException ne) {
                _logger.log(Level.SEVERE, "jndi.initial_context_error", factoryClass);
                _logger.log(Level.SEVERE, "jndi.initial_context_error_excp", ne.getMessage());
            }

            if (context == null) {
                _logger.log(Level.SEVERE, "jndi.factory_create_error", factoryClass);
                return;
            }

            // Bind a Reference to the proxy object factory; set the
            // initial context factory.
            //JndiProxyObjectFactory.setInitialContext(bindName, context);

            Reference ref = new Reference(extJndiRes.getResType(),
                    "com.sun.enterprise.resource.naming.JndiProxyObjectFactory",
                    null);

            // unique JNDI name within server runtime
            ref.add(new SerializableObjectRefAddr("resourceInfo", resourceInfo));

            // target JNDI name
            ref.add(new StringRefAddr("jndiLookupName", jndiLookupName));

            // target JNDI factory class
            ref.add(new StringRefAddr("jndiFactoryClass", factoryClass));

            // add Context info as a reference address
            ref.add(new com.sun.enterprise.resource.naming.ProxyRefAddr(extJndiRes.getResourceInfo().getName(), env));

            // Publish the reference
            namingService.publishObject(resourceInfo, ref, true);

        } catch (Exception ex) {
            _logger.log(Level.SEVERE, "customrsrc.create_ref_error", resourceInfo);
            _logger.log(Level.SEVERE, "customrsrc.create_ref_error_excp", ex);

        }
    }

    /**
     * Un-installs the external jndi resource.
     *
     * @param resource external jndi resource
     */
    public void uninstallExternalJndiResource(JavaEEResource resource, ResourceInfo resourceInfo) {

        // removes the jndi context from the factory cache
        //String bindName = resource.getResourceInfo().getName();
        JndiProxyObjectFactory.removeInitialContext(resource.getResourceInfo());

        // removes the resource from jndi naming
        try {
            namingService.unpublishObject(resourceInfo, resourceInfo.getName());
            /* TODO V3 handle jms later
            //START OF IASRI 4660565
            if (((ExternalJndiResource)resource).isJMSConnectionFactory()) {
                nm.unpublishObject(IASJmsUtil.getXAConnectionFactoryName(resourceName));
            }
            //END OF IASRI 4660565
            */
        } catch (javax.naming.NamingException e) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "Error while unpublishing resource: " + resourceInfo, e);
            }
        }
    }


    /**
     * Returns a new instance of j2ee external jndi resource from the given
     * config bean.
     *
     * This method gets called from the external resource
     * deployer to convert external-jndi-resource config bean into
     * external-jndi  j2ee resource.
     *
     * @param    rbean    external-jndi-resource config bean
     *
     * @return   a new instance of j2ee external jndi resource
     *
     */
    public static JavaEEResource toExternalJndiJavaEEResource(
            com.sun.enterprise.config.serverbeans.ExternalJndiResource rbean, ResourceInfo resourceInfo) {

        ExternalJndiResource jr = new com.sun.enterprise.resource.beans.ExternalJndiResource(resourceInfo);

        //jr.setDescription( rbean.getDescription() ); // FIXME: getting error

        // sets the enable flag
        jr.setEnabled( ConnectorsUtil.parseBoolean(rbean.getEnabled()) );

        // sets the jndi look up name
        jr.setJndiLookupName( rbean.getJndiLookupName() );

        // sets the resource type
        jr.setResType( rbean.getResType() );

        // sets the factory class name
        jr.setFactoryClass( rbean.getFactoryClass() );

        // sets the properties
        List<Property> properties = rbean.getProperty();
        if (properties!= null) {
            for(Property property : properties){
                ResourceProperty rp =
                    new ResourcePropertyImpl(property.getName(), property.getValue());
                jr.addProperty(rp);
            }
        }
        return jr;
    }
}
