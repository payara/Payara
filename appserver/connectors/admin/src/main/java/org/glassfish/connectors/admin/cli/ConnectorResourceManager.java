/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.connectors.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.I18n;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.ConnectorResource;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resources.admin.cli.ResourceManager;
import org.glassfish.resourcebase.resources.admin.cli.ResourceUtil;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.glassfish.resourcebase.resources.util.BindableResourcesHelper;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
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
 * @author Jennifer Chou, Jagadish Ramu
 */
@Service(name = ServerTags.CONNECTOR_RESOURCE)
@PerLookup
@I18n("create.connector.resource")
public class ConnectorResourceManager implements ResourceManager {

    private static final String DESCRIPTION = ServerTags.DESCRIPTION;

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ConnectorResourceManager.class);

    private String poolName = null;
    private String enabled = Boolean.TRUE.toString();
    private String enabledValueForTarget = Boolean.TRUE.toString();
    private String jndiName = null;
    private String description = null;
    private String objectType = "user";

    @Inject
    private Domain domain;

    @Inject
    private ResourceUtil resourceUtil;

    @Inject
    private BindableResourcesHelper resourcesHelper;

    public ConnectorResourceManager() {
    }

    public String getResourceType() {
        return ServerTags.CONNECTOR_RESOURCE;
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
            String msg = localStrings.getLocalString("create.connector.resource.fail",
                    "Connector resource {0} create failed ", jndiName) +
                    " " + tfe.getLocalizedMessage();
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        String msg = localStrings.getLocalString(
                "create.connector.resource.success", "Connector resource {0} created successfully",
                jndiName);
        return new ResourceStatus(ResourceStatus.SUCCESS, msg);

    }

    private ResourceStatus isValid(Resources resources, boolean validateResourceRef, String target){
        ResourceStatus status ;
        if (jndiName == null) {
            String msg = localStrings.getLocalString("create.connector.resource.noJndiName",
                    "No JNDI name defined for connector resource.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        status = resourcesHelper.validateBindableResourceForDuplicates(resources, jndiName, validateResourceRef,
                target, ConnectorResource.class);
        if(status.getStatus() == ResourceStatus.FAILURE){
            return status;
        }

        if (!isConnPoolExists(resources, poolName)) {
            String msg = localStrings.getLocalString("create.connector.resource.connPoolNotFound",
                    "Attribute value (pool-name = {0}) is not found in list of connector connection pools.", poolName);
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }
        return status;
    }


    private ConnectorResource createResource(Resources param, Properties props) throws PropertyVetoException,
            TransactionFailure {
        ConnectorResource newResource = createConfigBean(param, props);
        param.getResources().add(newResource);
        return newResource;
    }


    private ConnectorResource createConfigBean(Resources param, Properties properties) throws PropertyVetoException,
            TransactionFailure {
        ConnectorResource newResource = param.createChild(ConnectorResource.class);
        newResource.setJndiName(jndiName);
        if (description != null) {
            newResource.setDescription(description);
        }
        newResource.setPoolName(poolName);
        newResource.setEnabled(enabled);
        newResource.setObjectType(objectType);
        if (properties != null) {
            for (Map.Entry e : properties.entrySet()) {
                Property prop = newResource.createChild(Property.class);
                prop.setName((String) e.getKey());
                prop.setValue((String) e.getValue());
                newResource.getProperty().add(prop);
            }
        }
        return newResource;
    }

    private void setAttributes(HashMap attributes, String target) {
        poolName = (String) attributes.get(POOL_NAME);
        if(target != null){
            enabled = resourceUtil.computeEnabledValueForResourceBasedOnTarget((String)attributes.get(ENABLED), target);
        }else{
            enabled = (String) attributes.get(ENABLED);
        }
        enabledValueForTarget = (String) attributes.get(ENABLED);
        jndiName = (String) attributes.get(JNDI_NAME);
        description = (String) attributes.get(DESCRIPTION);
        objectType = (String) attributes.get(ServerTags.OBJECT_TYPE);
    }

    private boolean isConnPoolExists(Resources resources, String poolName) {
        return ConnectorsUtil.getResourceByName(resources, ConnectorConnectionPool.class, poolName) != null;
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
