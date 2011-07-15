/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.cli.resources;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.resource.common.ResourceStatus;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;

/**
 * @author Jagadish Ramu
 */
@Service
public class BindableResourcesHelper {

    @Inject
    private Domain domain;

    @Inject
    private ConfigBeansUtilities configBeanUtilities;

    @Inject
    private ResourceUtil resourceUtil;

    private final static String DOMAIN = "domain";

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(BindableResourcesHelper.class);


    public boolean resourceExists(String jndiName, String target) {
        boolean exists = false;
       if(target.equals(DOMAIN)){
            //if target is "domain", as long as the resource is present in "resources" section,
            //it is valid.
            exists = true;
        }else if(ConfigBeansUtilities.getServerNamed(target) != null){
            Server server = configBeanUtilities.getServerNamed(target);
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
        BindableResource duplicateResource = (BindableResource)
                resources.getResourceByName(BindableResource.class, jndiName);
        if (duplicateResource != null) {
            String msg ;
            if(validateResourceRef && (getResourceByClass(duplicateResource).equals(resourceTypeToValidate))){
                if(target.equals("domain")){
                    msg = localStrings.getLocalString("duplicate.resource.found",
                            "A {0} by name {1} already exists.", getResourceTypeName(duplicateResource), jndiName);
                }else if(resourceUtil.getTargetsReferringResourceRef(jndiName).contains(target)){
                    msg = localStrings.getLocalString("duplicate.resource.found.in.target",
                            "A {0} by name {1} already exists with resource reference in target {2}.",
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
        if (resource instanceof JdbcResource) {
            type = JdbcResource.class;
        } else if (resource instanceof ConnectorResource) {
            type = ConnectorResource.class;
        } else if (resource instanceof ExternalJndiResource) {
            type = ExternalJndiResource.class;
        } else if (resource instanceof CustomResource) {
            type = CustomResource.class;
        } else if (resource instanceof AdminObjectResource) {
            type = AdminObjectResource.class;
        } else if (resource instanceof MailResource) {
            type = MailResource.class;
        }
        return type;
    }

    public String getResourceTypeName(BindableResource resource){
        String type = "Resource";
        if (resource instanceof JdbcResource) {
            type = "JdbcResource";
        } else if (resource instanceof ConnectorResource) {
            type = "ConnectorResource";
        } else if (resource instanceof ExternalJndiResource) {
            type = "ExternalJndiResource";
        } else if (resource instanceof CustomResource) {
            type = "CustomResource";
        } else if (resource instanceof AdminObjectResource) {
            type = "AdminObjectResource";
        } else if (resource instanceof MailResource) {
            type = "MailResource";
        }
        return type;
    }
}
