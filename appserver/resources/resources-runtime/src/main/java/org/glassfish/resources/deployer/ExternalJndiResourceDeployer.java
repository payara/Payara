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

package org.glassfish.resources.deployer;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.repository.ResourceProperty;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.resources.api.*;
import org.glassfish.resources.config.ExternalJndiResource;
import org.glassfish.resources.naming.JndiProxyObjectFactory;
import org.glassfish.resources.naming.ProxyRefAddr;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;
import org.glassfish.resources.naming.SerializableObjectRefAddr;
import org.glassfish.resourcebase.resources.util.BindableResourcesHelper;
import org.glassfish.resourcebase.resources.util.ResourceUtil;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.InitialContextFactory;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles external-jndi resource events in the server instance.
 * <p/>
 * The external-jndi resource events from the admin instance are propagated
 * to this object.
 * <p/>
 * The methods can potentially be called concurrently, therefore implementation
 * need to be synchronized.
 *
 * @author Nazrul Islam
 * @since JDK1.4
 */
@Service
@ResourceDeployerInfo(ExternalJndiResource.class)
@Singleton
public class ExternalJndiResourceDeployer implements ResourceDeployer {

    @Inject
    private ResourceNamingService namingService;

    @Inject
    private BindableResourcesHelper bindableResourcesHelper;


    /**
     * StringManager for this deployer
     */
    private static final StringManager localStrings =
            StringManager.getManager(ExternalJndiResourceDeployer.class);
    /**
     * logger for this deployer
     */
    private static Logger _logger = LogDomains.getLogger(ExternalJndiResourceDeployer.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    public synchronized void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        ExternalJndiResource jndiRes =
                (ExternalJndiResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(jndiRes.getJndiName(), applicationName, moduleName);
        createExternalJndiResource(jndiRes, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void deployResource(Object resource) throws Exception {

        ExternalJndiResource jndiRes =
                (ExternalJndiResource) resource;
        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(jndiRes);
        createExternalJndiResource(jndiRes, resourceInfo);
    }

    private void createExternalJndiResource(ExternalJndiResource jndiRes,
                                            ResourceInfo resourceInfo) {
        // converts the config data to j2ee resource
        JavaEEResource j2eeRes = toExternalJndiJavaEEResource(jndiRes, resourceInfo);

        // installs the resource
        installExternalJndiResource((org.glassfish.resources.beans.ExternalJndiResource) j2eeRes, resourceInfo);

    }

    /**
     * {@inheritDoc}
     */
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception {
        ExternalJndiResource jndiRes =
                (ExternalJndiResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(jndiRes.getJndiName(), applicationName, moduleName);
        deleteResource(jndiRes, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void undeployResource(Object resource)
            throws Exception {

        ExternalJndiResource jndiRes =
                (ExternalJndiResource) resource;
        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(jndiRes);
        deleteResource(jndiRes, resourceInfo);
    }

    private void deleteResource(ExternalJndiResource jndiResource,
                                ResourceInfo resourceInfo) {
        // converts the config data to j2ee resource
        JavaEEResource j2eeResource = toExternalJndiJavaEEResource(jndiResource, resourceInfo);
        // un-installs the resource
        uninstallExternalJndiResource(j2eeResource, resourceInfo);

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
    public boolean handles(Object resource) {
        return resource instanceof ExternalJndiResource;
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
    public void installExternalJndiResource(org.glassfish.resources.beans.ExternalJndiResource extJndiRes, ResourceInfo resourceInfo) {

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


            Object factory = ResourceUtil.loadObject(factoryClass);
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
                 props.hasNext(); ) {

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
                    "org.glassfish.resources.naming.JndiProxyObjectFactory",
                    null);

            // unique JNDI name within server runtime
            ref.add(new SerializableObjectRefAddr("resourceInfo", resourceInfo));

            // target JNDI name
            ref.add(new StringRefAddr("jndiLookupName", jndiLookupName));

            // target JNDI factory class
            ref.add(new StringRefAddr("jndiFactoryClass", factoryClass));

            // add Context info as a reference address
            ref.add(new ProxyRefAddr(extJndiRes.getResourceInfo().getName(), env));

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
     * <p/>
     * This method gets called from the external resource
     * deployer to convert external-jndi-resource config bean into
     * external-jndi  j2ee resource.
     *
     * @param rbean external-jndi-resource config bean
     * @return a new instance of j2ee external jndi resource
     */
    public static org.glassfish.resources.api.JavaEEResource toExternalJndiJavaEEResource(
            ExternalJndiResource rbean, ResourceInfo resourceInfo) {

        org.glassfish.resources.beans.ExternalJndiResource jr = new org.glassfish.resources.beans.ExternalJndiResource(resourceInfo);

        //jr.setDescription( rbean.getDescription() ); // FIXME: getting error

        // sets the enable flag
        jr.setEnabled(Boolean.valueOf(rbean.getEnabled()));

        // sets the jndi look up name
        jr.setJndiLookupName(rbean.getJndiLookupName());

        // sets the resource type
        jr.setResType(rbean.getResType());

        // sets the factory class name
        jr.setFactoryClass(rbean.getFactoryClass());

        // sets the properties
        List<Property> properties = rbean.getProperty();
        if (properties != null) {
            for (Property property : properties) {
                ResourceProperty rp =
                        new ResourcePropertyImpl(property.getName(), property.getValue());
                jr.addProperty(rp);
            }
        }
        return jr;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canDeploy(boolean postApplicationDeployment, Collection<Resource> allResources, Resource resource) {
        if (handles(resource)) {
            if (!postApplicationDeployment) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void validatePreservedResource(Application oldApp, Application newApp, Resource resource,
                                          Resources allResources)
            throws ResourceConflictException {
        //do nothing.
    }
}
