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
 *
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */

package org.glassfish.resources.javamail.deployer;


import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.deployment.MailConfiguration;
import com.sun.enterprise.repository.ResourceProperty;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.resources.api.*;
import org.glassfish.resources.javamail.beans.MailBean;
import org.glassfish.resources.javamail.config.MailResource;
import org.glassfish.resources.javamail.naming.MailNamingObjectFactory;
import org.glassfish.resources.naming.SerializableObjectRefAddr;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.glassfish.resourcebase.resources.util.ResourceUtil;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.NamingException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.config.support.TranslatedConfigView;

/**
 * Handles mail resource events in the server instance.
 * <p/>
 * The mail resource events from the admin instance are propagated
 * to this object.
 * <p/>
 * The methods can potentially be called concurrently, therefore implementation
 * need to be synchronized.
 *
 * @author James Kong
 * @since JDK1.4
 */
@Service
@ResourceDeployerInfo(MailResource.class)
@Singleton
public class MailResourceDeployer extends GlobalResourceDeployer
        implements ResourceDeployer {


    @Inject
    private ResourceNamingService namingService;

    // StringManager for this deployer
    private static final StringManager localStrings =
            StringManager.getManager(MailResourceDeployer.class);

    // logger for this deployer
    private static Logger _logger = LogDomains.getLogger(MailResourceDeployer.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        MailResource mailRes =
                (MailResource) resource;


        if (mailRes == null) {
            _logger.log(Level.INFO, "Error in resource deploy.");
        } else {
            ResourceInfo resourceInfo = new ResourceInfo(mailRes.getJndiName(), applicationName, moduleName);
            //registers the jsr77 object for the mail resource deployed
            /* TODO Not needed any more ?
            /*ManagementObjectManager mgr =
                getAppServerSwitchObject().getManagementObjectManager();
            mgr.registerJavaMailResource(mailRes.getJndiName());*/

            installResource(mailRes, resourceInfo);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void deployResource(Object resource) throws Exception {
        MailResource mailResource =
                (MailResource) resource;
        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(mailResource);
        deployResource(resource, resourceInfo.getApplicationName(), resourceInfo.getModuleName());
    }

    /**
     * Local method for calling the ResourceInstaller for installing
     * mail resource in runtime.
     *
     * @param mailResource The mail resource to be installed.
     * @throws Exception when not able to create a resource
     */
    void installResource(MailResource mailResource,
                         ResourceInfo resourceInfo) throws Exception {
        // Converts the config data to j2ee resource ;
        // retieves the resource installer ; installs the resource ;
        // and adds it to a collection in the installer
        org.glassfish.resources.api.JavaEEResource j2eeRes = toMailBean(mailResource, resourceInfo);
        //ResourceInstaller installer = runtime.getResourceInstaller();
        installMailResource((MailBean) j2eeRes, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception {
        MailResource mailRes =
                (MailResource) resource;
        // converts the config data to j2ee resource
        ResourceInfo resourceInfo = new ResourceInfo(mailRes.getJndiName(), applicationName, moduleName);
        deleteResource(mailRes, resourceInfo);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void undeployResource(Object resource) throws Exception {
        MailResource mailRes =
                (MailResource) resource;
        // converts the config data to j2ee resource
        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(mailRes);
        deleteResource(mailRes, resourceInfo);
    }

    private void deleteResource(MailResource mailRes, ResourceInfo resourceInfo)
            throws NamingException {
        //JavaEEResource javaEEResource = toMailJavaEEResource(mailRes, resourceInfo);
        // removes the resource from jndi naming
        namingService.unpublishObject(resourceInfo, mailRes.getJndiName());

        /* TODO Not needed any more ?
            ManagementObjectManager mgr =
                    getAppServerSwitchObject().getManagementObjectManager();
            mgr.unregisterJavaMailResource(mailRes.getJndiName());
        */

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
    public boolean handles(Object resource) {
        return resource instanceof MailResource;
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
     * Installs the given mail resource. This method gets called during server
     * initialization and from mail resource deployer to handle resource events.
     *
     * @param mailResource mail resource
     * @param resourceInfo
     */
    public void installMailResource(MailBean mailResource, ResourceInfo resourceInfo) {

        try {

            MailConfiguration config = new MailConfiguration(mailResource);

            javax.naming.Reference ref = new javax.naming.Reference(javax.mail.Session.class.getName(),
                    MailNamingObjectFactory.class.getName(), null);
            SerializableObjectRefAddr serializableRefAddr = new SerializableObjectRefAddr("jndiName", config);
            ref.add(serializableRefAddr);

            // Publish the object
            namingService.publishObject(resourceInfo, ref, true);
        } catch (Exception ex) {
            _logger.log(Level.SEVERE, "mailrsrc.create_obj_error", resourceInfo);
            _logger.log(Level.SEVERE, "mailrsrc.create_obj_error_excp", ex);
        }
    }

    /**
     * Returns a new instance of j2ee mail resource from the given config bean.
     * <p/>
     * This method gets called from the mail resource deployer to convert mail
     * config bean into mail j2ee resource.
     *
     * @param mailResourceConfig mail-resource config bean
     * @param resourceInfo
     * @return a new instance of j2ee mail resource
     */
    public static MailBean toMailBean(
            MailResource mailResourceConfig, ResourceInfo resourceInfo) {

        MailBean mailResource =
                new MailBean(resourceInfo);

        //jr.setDescription(rbean.getDescription()); // FIXME: getting error
        mailResource.setEnabled(Boolean.valueOf(mailResourceConfig.getEnabled()));
        mailResource.setStoreProtocol(mailResourceConfig.getStoreProtocol());
        mailResource.setStoreProtocolClass(mailResourceConfig.getStoreProtocolClass());
        mailResource.setTransportProtocol(mailResourceConfig.getTransportProtocol());
        mailResource.setTransportProtocolClass(mailResourceConfig.getTransportProtocolClass());
        mailResource.setMailHost(TranslatedConfigView.expandValue(mailResourceConfig.getHost()));
        mailResource.setUsername(TranslatedConfigView.expandValue(mailResourceConfig.getUser()));
        mailResource.setPassword(TranslatedConfigView.expandValue(mailResourceConfig.getPassword()));
        mailResource.setAuth(Boolean.valueOf(mailResourceConfig.getAuth()));
        mailResource.setMailFrom(TranslatedConfigView.expandValue(mailResourceConfig.getFrom()));
        mailResource.setDebug(Boolean.valueOf(mailResourceConfig.getDebug()));

        // sets the properties
        List<Property> properties = mailResourceConfig.getProperty();
        if (properties != null) {

            for (Property property : properties) {
                ResourceProperty rp = new org.glassfish.resources.api.ResourcePropertyImpl(property.getName(), property.getValue());
                mailResource.addProperty(rp);
            }
        }
        return mailResource;
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
