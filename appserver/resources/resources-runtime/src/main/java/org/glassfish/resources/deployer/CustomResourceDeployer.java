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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */

package org.glassfish.resources.deployer;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.repository.ResourceProperty;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.resources.api.*;
import org.glassfish.resources.config.CustomResource;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.glassfish.resourcebase.resources.util.ResourceUtil;
import org.glassfish.resourcebase.resources.util.BindableResourcesHelper;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles custom resource events in the server instance.
 * <p/>
 * The custom resource events from the admin instance are propagated
 * to this object.
 * <p/>
 * The methods can potentially be called concurrently, therefore implementation
 * need to be synchronized.
 * <p/>
 * <p/>
 * Note: Since a notification is not sent to the user of the custom
 * resources upon undeploy, it is possible that there would be
 * stale objects not being garbage collected. Future versions
 * should take care of this problem.
 *
 * @author Nazrul Islam
 * @since JDK1.4
 */
@Service
@ResourceDeployerInfo(CustomResource.class)
@Singleton
public class CustomResourceDeployer implements ResourceDeployer {


    @Inject
    private BindableResourcesHelper bindableResourcesHelper;

    /**
     * Stringmanager for this deployer
     */
    private static final StringManager localStrings =
            StringManager.getManager(CustomResourceDeployer.class);

    @Inject
    private ResourceNamingService cns;
    /**
     * logger for this deployer
     */
    private static Logger _logger = LogDomains.getLogger(CustomResourceDeployer.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void deployResource(Object resource, String applicationName, String moduleName)
            throws Exception {
        CustomResource customResource =
                (CustomResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(customResource.getJndiName(), applicationName, moduleName);
        deployResource(resource, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void deployResource(Object resource) throws Exception {
        CustomResource customResource =
                (CustomResource) resource;
        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(customResource);
        deployResource(customResource, resourceInfo);
    }

    private void deployResource(Object resource, ResourceInfo resourceInfo) {

        CustomResource customRes =
                (CustomResource) resource;

        // converts the config data to j2ee resource
        JavaEEResource j2eeResource = toCustomJavaEEResource(customRes, resourceInfo);

        // installs the resource
        installCustomResource((org.glassfish.resources.beans.CustomResource) j2eeResource, resourceInfo);


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception {
        CustomResource customResource =
                (CustomResource) resource;
        ResourceInfo resourceInfo = new ResourceInfo(customResource.getJndiName(), applicationName, moduleName);
        deleteResource(customResource, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void undeployResource(Object resource)
            throws Exception {

        CustomResource customResource =
                (CustomResource) resource;
        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(customResource);
        deleteResource(customResource, resourceInfo);
    }

    private void deleteResource(CustomResource customResource,
                                ResourceInfo resourceInfo) throws NamingException {
        // converts the config data to j2ee resource
        //JavaEEResource j2eeResource = toCustomJavaEEResource(customRes, resourceInfo);
        // removes the resource from jndi naming
        cns.unpublishObject(resourceInfo, resourceInfo.getName());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handles(Object resource) {
        return resource instanceof CustomResource;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean supportsDynamicReconfiguration() {
        return false;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Class[] getProxyClassesForDynamicReconfiguration() {
        return new Class[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void redeployResource(Object resource)
            throws Exception {

        undeployResource(resource);
        deployResource(resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void enableResource(Object resource) throws Exception {
        deployResource(resource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * @param resourceInfo
     */
    public void installCustomResource(org.glassfish.resources.beans.CustomResource customRes, ResourceInfo resourceInfo) {

        try {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "installCustomResource by jndi-name : " + resourceInfo);
            }

            // bind a Reference to the object factory
            Reference ref = new Reference(customRes.getResType(), customRes.getFactoryClass(), null);

            // add resource properties as StringRefAddrs
            for (Iterator props = customRes.getProperties().iterator();
                 props.hasNext(); ) {

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
     * <p/>
     * This method gets called from the custom resource deployer
     * to convert custom-resource config bean into custom j2ee resource.
     *
     * @param rbean custom-resource config bean
     * @param resourceInfo the definition of the resources to create
     * @return new instance of j2ee custom resource
     */
    public static JavaEEResource toCustomJavaEEResource(
            CustomResource rbean, ResourceInfo resourceInfo) {


        org.glassfish.resources.beans.CustomResource jr =
                new org.glassfish.resources.beans.CustomResource(resourceInfo);

        //jr.setDescription(rbean.getDescription()); // FIXME: getting error

        // sets the enable flag
        jr.setEnabled(Boolean.valueOf(rbean.getEnabled()));

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
    @Override
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
    @Override
    public void validatePreservedResource(Application oldApp, Application newApp, Resource resource,
                                          Resources allResources)
            throws ResourceConflictException {
        //do nothing.
    }
}
