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

package org.glassfish.resources.javamail.admin.cli;

import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.I18n;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.glassfish.resourcebase.resources.util.BindableResourcesHelper;
import org.glassfish.resources.javamail.config.MailResource;
import org.glassfish.resourcebase.resources.util.ResourceUtil;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import javax.resource.ResourceException;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Properties;

import static org.glassfish.resources.admin.cli.ResourceConstants.*;


@Service(name = ServerTags.MAIL_RESOURCE)
@I18n("add.resources")
public class JavaMailResourceManager implements org.glassfish.resources.admin.cli.ResourceManager {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(JavaMailResourceManager.class);
    private static final String DESCRIPTION = ServerTags.DESCRIPTION;

    private String mailHost = null;
    private String mailUser = null;
    private String fromAddress = null;
    private String jndiName = null;
    private String storeProtocol = null;
    private String storeProtocolClass = null;
    private String transportProtocol = null;
    private String transportProtocolClass = null;
    private String enabled = null;
    private String enabledValueForTarget = null;
    private String debug = null;
    private String description = null;

    @Inject
    private org.glassfish.resourcebase.resources.admin.cli.ResourceUtil resourceUtil;

    @Inject
    private BindableResourcesHelper resourcesHelper;

    public String getResourceType() {
        return ServerTags.MAIL_RESOURCE;
    }

    public ResourceStatus create(Resources resources, HashMap attributes, final Properties properties,
                                 String target) throws Exception {
        setAttributes(attributes, target);

        ResourceStatus validationStatus = isValid(resources, true, target);
        if(validationStatus.getStatus() == ResourceStatus.FAILURE){
            return validationStatus;
        }

        // ensure we don't already have one of this name
        if (ResourceUtil.getBindableResourceByName(resources, jndiName) != null) {
            String msg = localStrings.getLocalString(
                    "create.mail.resource.duplicate.1",
                    "A Mail Resource named {0} already exists.",
                    jndiName);
            return new ResourceStatus(ResourceStatus.FAILURE, msg, true);
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<Resources>() {

                public Object run(Resources param) throws PropertyVetoException,
                        TransactionFailure {
                    MailResource newResource = createConfigBean(param, properties);
                    param.getResources().add(newResource);
                    return newResource;
                }
            }, resources);

            resourceUtil.createResourceRef(jndiName, enabledValueForTarget, target);

            String msg = localStrings.getLocalString(
                    "create.mail.resource.success",
                    "Mail Resource {0} created.", jndiName);
            return new ResourceStatus(ResourceStatus.SUCCESS, msg, true);
        } catch (TransactionFailure tfe) {
            String msg = localStrings.getLocalString("" +
                    "create.mail.resource.fail",
                    "Unable to create Mail Resource {0}.", jndiName) +
                    " " + tfe.getLocalizedMessage();
            return new ResourceStatus(ResourceStatus.FAILURE, msg, true);
        }
    }

    private ResourceStatus isValid(Resources resources, boolean validateResourceRef, String target){
        ResourceStatus status ;
        if (mailHost == null) {
            String msg = localStrings.getLocalString("create.mail.resource.noHostName",
                    "No host name defined for Mail Resource.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg, true);
        }

        if (mailUser == null) {
            String msg = localStrings.getLocalString("create.mail.resource.noUserName",
                    "No user name defined for Mail Resource.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg, true);
        }

        if (fromAddress == null) {
            String msg = localStrings.getLocalString("create.mail.resource.noFrom",
                    "From not defined for Mail Resource.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg, true);
        }


        status = resourcesHelper.validateBindableResourceForDuplicates(resources, jndiName, validateResourceRef,
                target, MailResource.class);
        if(status.getStatus() == ResourceStatus.FAILURE){
            return status;
        }

        return status;
    }

    private MailResource createConfigBean(Resources param, Properties props) throws PropertyVetoException,
            TransactionFailure {
        MailResource newResource = param.createChild(MailResource.class);
        newResource.setJndiName(jndiName);
        newResource.setFrom(fromAddress);
        newResource.setUser(mailUser);
        newResource.setHost(mailHost);
        newResource.setEnabled(enabled);
        newResource.setStoreProtocol(storeProtocol);
        newResource.setStoreProtocolClass(storeProtocolClass);
        newResource.setTransportProtocol(transportProtocol);
        newResource.setTransportProtocolClass(
                transportProtocolClass);
        newResource.setDebug(debug);
        if (description != null) {
            newResource.setDescription(description);
        }
        if (props != null) {
            for (java.util.Map.Entry e : props.entrySet()) {
                Property prop = newResource.createChild(Property.class);
                prop.setName((String) e.getKey());
                prop.setValue((String) e.getValue());
                newResource.getProperty().add(prop);
            }
        }
        return newResource;
    }

    private void setAttributes(HashMap attributes, String target) {
        jndiName = (String) attributes.get(JNDI_NAME);
        mailHost = (String) attributes.get(MAIL_HOST);
        mailUser = (String) attributes.get(MAIL_USER);
        fromAddress = (String) attributes.get(MAIL_FROM_ADDRESS);
        storeProtocol = (String) attributes.get(MAIL_STORE_PROTO);
        storeProtocolClass = (String) attributes.get(MAIL_STORE_PROTO_CLASS);
        transportProtocol = (String) attributes.get(MAIL_TRANS_PROTO);
        transportProtocolClass = (String) attributes.get(MAIL_TRANS_PROTO_CLASS);
        debug = (String) attributes.get(MAIL_DEBUG);
        if(target != null){
            enabled = resourceUtil.computeEnabledValueForResourceBasedOnTarget((String)attributes.get(ENABLED), target);
        }else{
            enabled = (String) attributes.get(ENABLED);
        }
        enabledValueForTarget = (String) attributes.get(ENABLED);
        description = (String) attributes.get(DESCRIPTION);
    }

    public Resource createConfigBean(Resources resources, HashMap attributes, Properties properties, boolean validate)
            throws Exception {
        setAttributes(attributes, null);
        ResourceStatus status = null;
        if(!validate){
            status = new ResourceStatus(ResourceStatus.SUCCESS,"");
        }else{
            status = isValid(resources, false, null);
        }
        if(status.getStatus() == ResourceStatus.SUCCESS){
            return createConfigBean(resources, properties);
        }else{
            throw new ResourceException(status.getMessage());
        }
    }
}
