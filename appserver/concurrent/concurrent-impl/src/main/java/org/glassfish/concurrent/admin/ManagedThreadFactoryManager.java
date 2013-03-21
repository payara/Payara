/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.concurrent.admin;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.concurrent.config.ManagedThreadFactory;
import org.glassfish.resources.admin.cli.ResourceManager;
import org.glassfish.resourcebase.resources.admin.cli.ResourceUtil;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.glassfish.resourcebase.resources.util.BindableResourcesHelper;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfiguredBy;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import javax.resource.ResourceException;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.glassfish.resources.admin.cli.ResourceConstants.*;

/**
 *
 * The managed thread factory manager allows you to create and delete 
 * the managed-thread-factory config element
 */
@Service (name=ServerTags.MANAGED_THREAD_FACTORY)
@I18n("managed.thread.factory.manager")
@ConfiguredBy(Resources.class)
public class ManagedThreadFactoryManager implements ResourceManager {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ManagedThreadFactoryManager.class);
    private static final String DESCRIPTION = ServerTags.DESCRIPTION;

    private String jndiName = null;
    private String description = null;
    private String threadPriority = ""+Thread.NORM_PRIORITY;
    private String contextInfoEnabled = Boolean.TRUE.toString();
    private String contextInfo = CONTEXT_INFO_DEFAULT_VALUE;
    private String enabled = Boolean.TRUE.toString();
    private String enabledValueForTarget = Boolean.TRUE.toString();

    @Inject
    private ResourceUtil resourceUtil;

    @Inject
    private ServerEnvironment environment;

    @Inject
    private BindableResourcesHelper resourcesHelper;

    public String getResourceType () {
        return ServerTags.MANAGED_THREAD_FACTORY;
    }

    public ResourceStatus create(Resources resources, HashMap attributes, final Properties properties,
                                 String target) throws Exception {

        setAttributes(attributes, target);

        ResourceStatus validationStatus = isValid(resources, true, target);
        if(validationStatus.getStatus() == ResourceStatus.FAILURE){
            return validationStatus;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<Resources>() {

                public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                    return createResource(param, properties);
                }
            }, resources);

                resourceUtil.createResourceRef(jndiName, enabledValueForTarget, target);
        } catch (TransactionFailure tfe) {
            String msg = localStrings.getLocalString("create.managed.thread.factory.failed", "Managed thread factory {0} creation failed", jndiName) + " " + tfe.getLocalizedMessage();
            ResourceStatus status = new ResourceStatus(ResourceStatus.FAILURE, msg);
            status.setException(tfe);
            return status;
        }
        String msg = localStrings.getLocalString("create.managed.thread.factory.success", "Managed thread factory {0} created successfully", jndiName);
        return new ResourceStatus(ResourceStatus.SUCCESS, msg);
    }

    private ResourceStatus isValid(Resources resources, boolean validateResourceRef, String target){
        ResourceStatus status ;


        if (jndiName == null) {
            String msg = localStrings.getLocalString("managed.thread.factory.noJndiName", "No JNDI name defined for managed thread factory.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        status = resourcesHelper.validateBindableResourceForDuplicates(resources, jndiName, validateResourceRef, target, ManagedThreadFactory.class);
        
        return status;
    }

    private void setAttributes(HashMap attributes, String target) {
        jndiName = (String) attributes.get(JNDI_NAME);
        description = (String) attributes.get(DESCRIPTION);
        contextInfoEnabled = (String) attributes.get(CONTEXT_INFO_ENABLED);
        contextInfo = (String) attributes.get(CONTEXT_INFO);
        threadPriority = (String) attributes.get(THREAD_PRIORITY);
        if(target != null){
            enabled = resourceUtil.computeEnabledValueForResourceBasedOnTarget((String)attributes.get(ENABLED), target);
        }else{
            enabled = (String) attributes.get(ENABLED);
        }
        enabledValueForTarget = (String) attributes.get(ENABLED); 
    }

    private ManagedThreadFactory createResource(Resources param, Properties properties) throws PropertyVetoException,
            TransactionFailure {
        ManagedThreadFactory newResource = createConfigBean(param, properties);
        param.getResources().add(newResource);
        return newResource;
    }

    private ManagedThreadFactory createConfigBean(Resources param, Properties properties) throws PropertyVetoException,
            TransactionFailure {
        ManagedThreadFactory managedThreadFactory = param.createChild(ManagedThreadFactory.class);
        managedThreadFactory.setJndiName(jndiName);
        if (description != null) {
            managedThreadFactory.setDescription(description);
        }
        managedThreadFactory.setContextInfoEnabled(contextInfoEnabled);
        managedThreadFactory.setContextInfo(contextInfo);
        managedThreadFactory.setThreadPriority(threadPriority);
        managedThreadFactory.setEnabled(enabled);
        if (properties != null) {
            for ( Map.Entry e : properties.entrySet()) {
                Property prop = managedThreadFactory.createChild(Property.class);
                prop.setName((String)e.getKey());
                prop.setValue((String)e.getValue());
                managedThreadFactory.getProperty().add(prop);
            }
        }
        return managedThreadFactory;
    }

    public Resource createConfigBean(final Resources resources, HashMap attributes, final Properties properties, boolean validate) throws Exception{
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

    public ResourceStatus delete (final Resources resources, final String jndiName, final String target)
            throws Exception {
        
        if (jndiName == null) {
            String msg = localStrings.getLocalString("managed.thread.factory.noJndiName", "No JNDI name defined for managed thread factory.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        Resource resource = ConnectorsUtil.getResourceByName(resources, ManagedThreadFactory.class, jndiName);

        // ensure we already have this resource
        if (resource == null){
            String msg = localStrings.getLocalString("delete.managed.thread.factory.notfound", "A managed thread factory named {0} does not exist.", jndiName);
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        if (SYSTEM_ALL_REQ.equals(resource.getObjectType())) {
            String msg = localStrings.getLocalString("delete.concurrent.resource.notAllowed", "The {0} resource cannot be deleted as it is required to be configured in the system.", jndiName);
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        if (environment.isDas()) {

            if ("domain".equals(target)) {
                if (resourceUtil.getTargetsReferringResourceRef(jndiName).size() > 0) {
                    String msg = localStrings.getLocalString("delete.managed.thread.factory.resource-ref.exist", "This managed thread factory [ {0} ] is referenced in an instance/cluster target, use delete-resource-ref on appropriate target", jndiName);
                    return new ResourceStatus(ResourceStatus.FAILURE, msg);
                }
            } else {
                if (!resourceUtil.isResourceRefInTarget(jndiName, target)) {
                    String msg = localStrings.getLocalString("delete.managed.thread.factory.no.resource-ref", "This managed thread factory [ {0} ] is not referenced in target [ {1} ]", jndiName, target);
                    return new ResourceStatus(ResourceStatus.FAILURE, msg);
                }

                if (resourceUtil.getTargetsReferringResourceRef(jndiName).size() > 1) {
                    String msg = localStrings.getLocalString("delete.managed.thread.factory.multiple.resource-refs", "This managed thread factory [ {0} ] is referenced in multiple instance/cluster targets, Use delete-resource-ref on appropriate target", jndiName);
                    return new ResourceStatus(ResourceStatus.FAILURE, msg);
                }
            }
        }

        try {
            // delete resource-ref
            resourceUtil.deleteResourceRef(jndiName, target);
            
            // delete managed-thread-factory
            if (ConfigSupport.apply(new SingleConfigCode<Resources>() {
                public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                    ManagedThreadFactory resource = (ManagedThreadFactory) ConnectorsUtil.getResourceByName(resources, ManagedThreadFactory.class, jndiName);
                    return param.getResources().remove(resource);
                }
            }, resources) == null) {
                String msg = localStrings.getLocalString("delete.managed.thread.factory.failed", "Managed thread factory {0} deletion failed", jndiName);
                return new ResourceStatus(ResourceStatus.FAILURE, msg);
            }
        } catch(TransactionFailure tfe) {
            String msg = localStrings.getLocalString("delete.managed.thread.factory.failed", "Managed thread factory {0} deletion failed ", jndiName);
            ResourceStatus status = new ResourceStatus(ResourceStatus.FAILURE, msg);
            status.setException(tfe);
            return status;
        }

        String msg = localStrings.getLocalString("delete.managed.thread.factory.success", "Managed thread factory {0} deleted successfully", jndiName);
        return new ResourceStatus(ResourceStatus.SUCCESS, msg);
    }
}
