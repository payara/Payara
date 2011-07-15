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
import com.sun.enterprise.resource.beans.MailResource;
import com.sun.enterprise.resource.naming.SerializableObjectRefAddr;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;

import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.deployment.MailConfiguration;
import com.sun.enterprise.repository.ResourceProperty;
import com.sun.enterprise.container.common.impl.MailNamingObjectFactory;
import com.sun.appserv.connectors.internal.spi.ResourceDeployer;

import org.glassfish.resource.common.ResourceInfo;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Singleton;

import javax.naming.NamingException;

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
@Scoped(Singleton.class)
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
    public synchronized void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        com.sun.enterprise.config.serverbeans.MailResource mailRes =
                (com.sun.enterprise.config.serverbeans.MailResource) resource;


        if (mailRes == null) {
            _logger.log(Level.INFO, "core.resourcedeploy_error");
        } else {
            ResourceInfo resourceInfo = new ResourceInfo(mailRes.getJndiName(), applicationName, moduleName);
            if (ResourcesUtil.createInstance().isEnabled(mailRes, resourceInfo)){
            //registers the jsr77 object for the mail resource deployed
            /* TODO Not needed any more ?
            /*ManagementObjectManager mgr =
                getAppServerSwitchObject().getManagementObjectManager();
            mgr.registerJavaMailResource(mailRes.getJndiName());*/

            installResource(mailRes, resourceInfo);
            } else {
                _logger.log(Level.INFO, "core.resource_disabled",
                        new Object[] {mailRes.getJndiName(),
                        ConnectorConstants.RES_TYPE_MAIL});
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void deployResource(Object resource) throws Exception {
        com.sun.enterprise.config.serverbeans.MailResource mailResource =
                (com.sun.enterprise.config.serverbeans.MailResource)resource;
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(mailResource);
        deployResource(resource, resourceInfo.getApplicationName(), resourceInfo.getModuleName());
    }

    /**
     * Local method for calling the ResourceInstaller for installing
     * mail resource in runtime.
     *
     * @param mailResource The mail resource to be installed.
     * @throws Exception when not able to create a resource
     */
    void installResource(com.sun.enterprise.config.serverbeans.MailResource mailResource,
                         ResourceInfo resourceInfo) throws Exception {
        // Converts the config data to j2ee resource ;
        // retieves the resource installer ; installs the resource ;
        // and adds it to a collection in the installer
        JavaEEResource j2eeRes = toMailJavaEEResource(mailResource, resourceInfo);
        //ResourceInstaller installer = runtime.getResourceInstaller();
        installMailResource((MailResource) j2eeRes, resourceInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception{
        com.sun.enterprise.config.serverbeans.MailResource mailRes =
                (com.sun.enterprise.config.serverbeans.MailResource) resource;
        // converts the config data to j2ee resource
        ResourceInfo resourceInfo = new ResourceInfo(mailRes.getJndiName(), applicationName, moduleName);
        deleteResource(mailRes, resourceInfo);
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void undeployResource(Object resource) throws Exception {
        com.sun.enterprise.config.serverbeans.MailResource mailRes =
                (com.sun.enterprise.config.serverbeans.MailResource) resource;
        // converts the config data to j2ee resource
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(mailRes);
        deleteResource(mailRes, resourceInfo);
    }

    private void deleteResource(com.sun.enterprise.config.serverbeans.MailResource mailRes, ResourceInfo resourceInfo)
            throws NamingException {
        if (ResourcesUtil.createInstance().isEnabled(mailRes, resourceInfo)){
            //JavaEEResource javaEEResource = toMailJavaEEResource(mailRes, resourceInfo);
            // removes the resource from jndi naming
            namingService.unpublishObject(resourceInfo, mailRes.getJndiName());

            /* TODO Not needed any more ?
                ManagementObjectManager mgr =
                        getAppServerSwitchObject().getManagementObjectManager();
                mgr.unregisterJavaMailResource(mailRes.getJndiName());
            */
        }else{
            _logger.log(Level.FINEST, "core.resource_disabled", new Object[] {mailRes.getJndiName(),
                    ConnectorConstants.RES_TYPE_MAIL});
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
        return resource instanceof com.sun.enterprise.config.serverbeans.MailResource;
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
     * Installs the given mail resource. This method gets called during server
     * initialization and from mail resource deployer to handle resource events.
     *
     * @param mailResource mail resource
     */
    public void installMailResource(com.sun.enterprise.resource.beans.MailResource mailResource, ResourceInfo resourceInfo) {

        try {

            MailConfiguration config = new MailConfiguration(mailResource);

            javax.naming.Reference ref = new javax.naming.Reference(javax.mail.Session.class.getName(),
                    MailNamingObjectFactory.class.getName(),null);
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
     *
     * This method gets called from the mail resource deployer to convert mail
     * config bean into mail j2ee resource.
     *
     * @param    mailResourceConfig    mail-resource config bean
     *
     * @return   a new instance of j2ee mail resource
     *
     */
    public static JavaEEResource toMailJavaEEResource(
        com.sun.enterprise.config.serverbeans.MailResource mailResourceConfig, ResourceInfo resourceInfo) {

        com.sun.enterprise.resource.beans.MailResource mailResource = new MailResource(resourceInfo);

        //jr.setDescription(rbean.getDescription()); // FIXME: getting error
        mailResource.setEnabled(ConnectorsUtil.parseBoolean(mailResourceConfig.getEnabled()));
        mailResource.setStoreProtocol(mailResourceConfig.getStoreProtocol());
        mailResource.setStoreProtocolClass(mailResourceConfig.getStoreProtocolClass());
        mailResource.setTransportProtocol(mailResourceConfig.getTransportProtocol());
        mailResource.setTransportProtocolClass(mailResourceConfig.getTransportProtocolClass());
        mailResource.setMailHost(mailResourceConfig.getHost());
        mailResource.setUsername(mailResourceConfig.getUser());
        mailResource.setMailFrom(mailResourceConfig.getFrom());
        mailResource.setDebug(ConnectorsUtil.parseBoolean(mailResourceConfig.getDebug()));

        // sets the properties
        List<Property> properties = mailResourceConfig.getProperty();
        if (properties != null) {

            for(Property property : properties){
                ResourceProperty rp = new ResourcePropertyImpl(property.getName(), property.getValue());
                mailResource.addProperty(rp);
            }
        }
        return mailResource;
    }
}
