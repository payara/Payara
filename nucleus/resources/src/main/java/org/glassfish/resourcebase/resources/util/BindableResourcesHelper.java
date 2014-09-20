/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.resourcebase.resources.util;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;


/**
 * @author Jagadish Ramu
 */
@Service
public class BindableResourcesHelper {

    @Inject
    ServiceLocator habitat;

    @Inject
    private ServerEnvironment environment;

    private final static String DOMAIN = "domain";

    @LogMessagesResourceBundle
    public static final String LOGMESSAGE_RESOURCE = "org.glassfish.resourcebase.resources.LogMessages";

    @LoggerInfo(subsystem="RESOURCE", description="Nucleus Resource", publish=true)

    public static final String LOGGER = "javax.enterprise.resources.util";

    private final Logger _logger = Logger.getLogger(LOGGER, LOGMESSAGE_RESOURCE);

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(BindableResourcesHelper.class);

    private Server server;


    public boolean resourceExists(String jndiName, String target) {
        boolean exists = false;
        Domain domain = habitat.getService(Domain.class);
       if(target.equals(DOMAIN)){
            //if target is "domain", as long as the resource is present in "resources" section,
            //it is valid.
            exists = true;
        }else if(habitat.<ConfigBeansUtilities>getService(ConfigBeansUtilities.class).getServerNamed(target) != null){
            Server server = habitat.<ConfigBeansUtilities>getService(ConfigBeansUtilities.class).getServerNamed(target);
            exists = server.isResourceRefExists(jndiName);
        }else if (domain.getClusterNamed(target) != null){
            Cluster cluster = domain.getClusterNamed(target);
            exists = cluster.isResourceRefExists(jndiName);
        }else{
            //if target is "CONFIG", as long as the resource is present in "resources" section,
            //it is valid.
            for(Config config : domain.getConfigs().getConfig()){
                if(config.getName().equals(target)){
                    exists = true;
                    break;
                }
            }
        }
        return exists;
    }

    /**
     * checks whether duplicate resource exists or resource is already created but not resource-ref or
     * resource-ref already exists.
     * @param resources resources
     * @param jndiName resource-name
     * @param validateResourceRef whether to validate resource-ref
     * @param target target instance/cluster/domain
     * @param resourceTypeToValidate type of resource
     * @return ResourceStatus indicating Success or Failure
     */
    public ResourceStatus validateBindableResourceForDuplicates(Resources resources, String jndiName,
                                                          boolean validateResourceRef, String target,
                                                          Class<? extends BindableResource> resourceTypeToValidate){
        // ensure we don't already have one of this name
        BindableResource duplicateResource =
                ResourceUtil.getBindableResourceByName(resources, jndiName);
        if (duplicateResource != null) {
            String msg ;
            if(validateResourceRef && (getResourceByClass(duplicateResource).equals(resourceTypeToValidate))){
                if(target.equals("domain")){
                    msg = localStrings.getLocalString("duplicate.resource.found",
                            "A {0} by name {1} already exists.", getResourceTypeName(duplicateResource), jndiName);
                } else if (habitat.<org.glassfish.resourcebase.resources.admin.cli.ResourceUtil>
                        getService(org.glassfish.resourcebase.resources.admin.cli.ResourceUtil.class).getTargetsReferringResourceRef(jndiName).contains(target)) {
                    msg = localStrings.getLocalString("duplicate.resource.found.in.target",
                            "A {0} by name {1} already exists with resource-ref in target {2}.",
                            getResourceTypeName(duplicateResource), jndiName, target);
                }else{
                    msg = localStrings.getLocalString("duplicate.resource.need.to.create.resource.ref",
                            "A {0} named {1} already exists. If you are trying to create the existing resource" +
                                    "configuration in target {2}, please use 'create-resource-ref' command " +
                                    "(or create resource-ref using admin console).",
                            getResourceTypeName(duplicateResource), jndiName, target);
                }
            }else{
                msg = localStrings.getLocalString("duplicate.resource.found",
                        "A {0} by name {1} already exists.", getResourceTypeName(duplicateResource), jndiName);
            }
            return new ResourceStatus(ResourceStatus.FAILURE, msg, true);
        }else{
            return new ResourceStatus(ResourceStatus.SUCCESS, "Validation Successful");
        }
    }

    public Class getResourceByClass(BindableResource resource){
        Class<? extends BindableResource> type = BindableResource.class;
        if(Proxy.isProxyClass(resource.getClass())){
            Class[] interfaces = resource.getClass().getInterfaces();
            if(interfaces != null){
                for(Class clz : interfaces){
                    if(BindableResource.class.isAssignableFrom(clz)){
                        return clz;
                    }
                }
            }
        }
        return type;
    }


    public String getResourceTypeName(BindableResource resource){
        String type = "Resource";
        Class resourceType = getResourceByClass(resource);
        if(resourceType != null){
            type = resourceType.getSimpleName();
        }
        return type;
    }

    public boolean isBindableResourceEnabled(BindableResource br){
        boolean resourceRefEnabled = false;
        ResourceRef ref = getServer().getResourceRef(br.getJndiName());
        if (ref != null) {
            resourceRefEnabled = Boolean.valueOf(ref.getEnabled());
        } else {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("ResourcesUtil :: isResourceReferenceEnabled null ref");
            }
        }

        boolean resourceEnabled = Boolean.valueOf(br.getEnabled());
        return resourceEnabled && resourceRefEnabled;
    }

    //TODO duplicate code in com.sun.enterprise.connectors.util.ResourcesUtil.java
    /*public boolean isNonConnectorBindableResourceEnabled(BindableResource br, ResourceInfo resourceInfo){
        boolean enabled = false;
        //this cannot happen? need to remove later?
        if (br == null) {
            return false;
        }
        boolean resourceEnabled = Boolean.valueOf(br.getEnabled());

        //TODO can we also check whether the application in which it is defined is enabled (app and app-ref) ?
        if (resourceInfo.getName().contains(ResourceConstants.JAVA_SCOPE_PREFIX)) {
            return resourceEnabled;
        }

        boolean refEnabled = isResourceReferenceEnabled(resourceInfo);
        if(refEnabled && resourceEnabled){
            //other bindable resources need to be checked for "resource.enabled" and "resource-ref.enabled"
            enabled = true;
        }
        return enabled;

    }*/

    /**
     *
     * //TODO duplicate code in com.sun.enterprise.connectors.util.ResourcesUtil.java
     * Checks if a resource reference is enabled
     * For application-scoped-resource, checks whether application-ref is enabled
     *
     * @param resourceInfo resourceInfo ResourceInfo
     * @return boolean indicating whether the resource-ref/application-ref is enabled.
     */
    /*public boolean isResourceReferenceEnabled(ResourceInfo resourceInfo) {
        String enabled = "false";
        if (ResourceUtil.isModuleScopedResource(resourceInfo) ||
                ResourceUtil.isApplicationScopedResource(resourceInfo)) {
            ApplicationRef appRef = getServer().getApplicationRef(resourceInfo.getApplicationName());
            if (appRef != null) {
                enabled = appRef.getEnabled();
            } else {
                // for an application-scoped-resource, if the application is being deployed,
                // <application> element and <application-ref> will be null until deployment
                // is complete. Hence this workaround.
                enabled = "true";
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.fine("ResourcesUtil :: isResourceReferenceEnabled null app-ref");
                }
            }
        } else {
            ResourceRef ref = getServer().getResourceRef(resourceInfo.getName());
            if (ref != null) {
                enabled = ref.getEnabled();
            } else {
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.fine("ResourcesUtil :: isResourceReferenceEnabled null ref");
                }
            }
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ResourcesUtil :: isResourceReferenceEnabled ref enabled ?" + enabled);
        }

        return Boolean.valueOf(enabled);
    }*/

    private Server getServer(){
        if(server == null){
            server = habitat.<Domain>getService(Domain.class).getServerNamed(environment.getInstanceName());
        }
        return server;
    }

}
