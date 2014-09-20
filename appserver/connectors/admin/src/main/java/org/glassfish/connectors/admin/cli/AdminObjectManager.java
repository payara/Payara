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

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.connectors.config.AdminObjectResource;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resources.admin.cli.ResourceManager;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.appserv.connectors.internal.api.ConnectorConstants.EMBEDDEDRAR_NAME_DELIMITER;
import static org.glassfish.resources.admin.cli.ResourceConstants.*;

/**
 *
 * @author Jennifer Chou
 */
@Service (name=ServerTags.ADMIN_OBJECT_RESOURCE)
@PerLookup
@I18n("create.admin.object")
public class AdminObjectManager implements ResourceManager {

    @Inject
    private Applications applications;

    @Inject
    private ConnectorRuntime connectorRuntime;

    @Inject
    private org.glassfish.resourcebase.resources.admin.cli.ResourceUtil resourceUtil;

    @Inject
    private org.glassfish.resourcebase.resources.util.BindableResourcesHelper resourcesHelper;

    private static final String DESCRIPTION = ServerTags.DESCRIPTION;

    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(AdminObjectManager.class);

    private String resType = null;
    private String className = null;
    private String raName = null;
    private String enabled = Boolean.TRUE.toString();
    private String enabledValueForTarget = Boolean.TRUE.toString();
    private String jndiName = null;
    private String description = null;

    @Inject
    private ServerEnvironment environment;

    public AdminObjectManager() {
    }

    public String getResourceType() {
        return ServerTags.ADMIN_OBJECT_RESOURCE;
    }

    public ResourceStatus create(Resources resources, HashMap attributes, final Properties properties, String target)
            throws Exception {
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
            Logger.getLogger(AdminObjectManager.class.getName()).log(Level.SEVERE,
                    "Unabled to create administered object", tfe);
            String msg = localStrings.getLocalString("create.admin.object.fail",
                    "Unable to create administered object {0}.", jndiName) +
                    " " + tfe.getLocalizedMessage();
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        String msg = localStrings.getLocalString(
                "create.admin.object.success",
                "Administered object {0} created.", jndiName);
        return new ResourceStatus(ResourceStatus.SUCCESS, msg);

    }

    private ResourceStatus isValid(Resources resources, boolean validateResourceRef, String target){
        ResourceStatus status ;
        if (jndiName == null) {
            String msg = localStrings.getLocalString("create.admin.object.noJndiName",
                            "No JNDI name defined for administered object.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        status = resourcesHelper.validateBindableResourceForDuplicates(resources, jndiName, validateResourceRef,
                target, AdminObjectResource.class);
        if(status.getStatus() == ResourceStatus.FAILURE){
            return status;
        }

        //no need to validate in remote instance as the validation would have happened in DAS.
        if(environment.isDas()){
            status = isValidRAName();
            if (status.getStatus() == ResourceStatus.FAILURE) {
                return status;
            }

            status = isValidAdminObject();
            if (status.getStatus() == ResourceStatus.FAILURE) {
                return status;
            }
        }
        return status;
    }

    private AdminObjectResource createResource(Resources param, Properties props) throws PropertyVetoException,
            TransactionFailure  {
        AdminObjectResource newResource = createConfigBean(param, props);
        param.getResources().add(newResource);
        return newResource;
    }

    private AdminObjectResource createConfigBean(Resources param, Properties props) throws PropertyVetoException,
            TransactionFailure {
        AdminObjectResource newResource = param.createChild(AdminObjectResource.class);
        newResource.setJndiName(jndiName);
        if (description != null) {
            newResource.setDescription(description);
        }
        newResource.setResAdapter(raName);
        newResource.setResType(resType);
        newResource.setClassName(className);
        newResource.setEnabled(enabled);
        if (props != null) {
            for ( Map.Entry e : props.entrySet()) {
                Property prop = newResource.createChild(Property.class);
                prop.setName((String)e.getKey());
                prop.setValue((String)e.getValue());
                newResource.getProperty().add(prop);
            }
        }
        return newResource;
    }

    public void setAttributes(HashMap attributes, String target) {
        resType = (String) attributes.get(RES_TYPE);
        className = (String)attributes.get(ADMIN_OBJECT_CLASS_NAME);
        if(target != null){
            enabled = resourceUtil.computeEnabledValueForResourceBasedOnTarget((String)attributes.get(ENABLED), target);
        }else{
            enabled = (String) attributes.get(ENABLED);
        }
        enabledValueForTarget = (String) attributes.get(ENABLED);
        jndiName = (String) attributes.get(JNDI_NAME);
        description = (String) attributes.get(DESCRIPTION);
        raName = (String) attributes.get(RES_ADAPTER);
    }
    
     //TODO Error checking taken from v2, need to refactor for v3
    private ResourceStatus isValidAdminObject() {
        // Check if the restype is valid -
        // To check this, we need to get the list of admin-object-interface
        // names and then find out if this list contains the restype.
        //boolean isValidAdminObject = true;
         boolean isValidAdminObject = false;

         //if classname is null, check whether the resType is present and only one adminobject must
         //be using that resType
         if (className == null) {

             String[] resTypes;
             try {
                 resTypes = connectorRuntime.getAdminObjectInterfaceNames(raName);
             } catch (ConnectorRuntimeException cre) {
                 Logger.getLogger(AdminObjectManager.class.getName()).log(Level.SEVERE,
                         "Could not find admin-ojbect-interface names (resTypes) from ConnectorRuntime for resource adapter.", cre);
                 String msg = localStrings.getLocalString(
                         "admin.mbeans.rmb.null_ao_intf",
                         "Resource Adapter {0} does not contain any resource type for admin-object. Please specify another res-adapter.",
                         raName) + " " + cre.getLocalizedMessage();
                 return new ResourceStatus(ResourceStatus.FAILURE, msg);
             }
             if (resTypes == null || resTypes.length <= 0) {
                 String msg = localStrings.getLocalString("admin.mbeans.rmb.null_ao_intf",
                         "Resource Adapter {0} does not contain any resource type for admin-object. Please specify another res-adapter.", raName);
                 return new ResourceStatus(ResourceStatus.FAILURE, msg);
             }

             int count = 0;
             for (int i = 0; i < resTypes.length; i++) {
                 if (resTypes[i].equals(resType)) {
                     isValidAdminObject = true;
                     count++;
                 }
             }
             if(count > 1){
                 String msg = localStrings.getLocalString(
                         "admin.mbeans.rmb.multiple_admin_objects.found.for.restype",
                         "Need to specify admin-object classname parameter (--classname) as multiple admin objects " +
                                 "use this resType [ {0} ]",  resType);

                 return new ResourceStatus(ResourceStatus.FAILURE, msg);
             }
         }else{
             try{
                isValidAdminObject = connectorRuntime.hasAdminObject(raName, resType, className);
             } catch (ConnectorRuntimeException cre) {
                 Logger.getLogger(AdminObjectManager.class.getName()).log(Level.SEVERE,
                         "Could not find admin-object-interface names (resTypes) and admin-object-classnames from " +
                                 "ConnectorRuntime for resource adapter.", cre);
                 String msg = localStrings.getLocalString(
                         "admin.mbeans.rmb.ao_intf_impl_check_failed",
                         "Could not determine admin object resource information of Resource Adapter [ {0} ] for" +
                                 "resType [ {1} ] and classname [ {2} ] ",
                         raName, resType, className) + " " + cre.getLocalizedMessage();
                 return new ResourceStatus(ResourceStatus.FAILURE, msg);
             }
         }

         if (!isValidAdminObject) {
            String msg = localStrings.getLocalString("admin.mbeans.rmb.invalid_res_type",
                "Invalid Resource Type: {0}", resType);
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }
        return new ResourceStatus(ResourceStatus.SUCCESS, "");
    }

    private ResourceStatus isValidRAName() {
        //TODO turn on validation.  For now, turn validation off until connector modules ready
        //boolean retVal = false;
        ResourceStatus status = new ResourceStatus(ResourceStatus.SUCCESS, "");

        if ((raName == null) || (raName.equals(""))) {
            String msg = localStrings.getLocalString("admin.mbeans.rmb.null_res_adapter",
                    "Resource Adapter Name is null.");
            status = new ResourceStatus(ResourceStatus.FAILURE, msg);
        } else {
            // To check for embedded connector module
            // System RA, so don't validate
            if (!ConnectorsUtil.getNonJdbcSystemRars().contains(raName)){
                // Check if the raName contains double underscore or hash.
                // If that is the case then this is the case of an embedded rar,
                // hence look for the application which embeds this rar,
                // otherwise look for the webconnector module with this raName.

                int indx = raName.indexOf(EMBEDDEDRAR_NAME_DELIMITER);
                if (indx != -1) {
                    String appName = raName.substring(0, indx);
                    Application app = applications.getModule(Application.class, appName);
                    if (app == null) {
                        String msg = localStrings.getLocalString("admin.mbeans.rmb.invalid_ra_app_not_found",
                                "Invalid raname. Application with name {0} not found.", appName);
                        status = new ResourceStatus(ResourceStatus.FAILURE, msg);
                    }
                } else {
                    Application app = applications.getModule(Application.class, raName);
                    if (app == null) {
                        String msg = localStrings.getLocalString("admin.mbeans.rmb.invalid_ra_cm_not_found",
                                "Invalid raname. Connector Module with name {0} not found.", raName);
                        status = new ResourceStatus(ResourceStatus.FAILURE, msg);
                    }
                }
            }
        }

        return status;
    }
    public Resource createConfigBean(Resources resources, HashMap attributes, Properties properties, boolean validate) throws Exception{
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
