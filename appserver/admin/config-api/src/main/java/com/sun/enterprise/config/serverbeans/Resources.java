/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.*;
import org.jvnet.hk2.component.Injectable;

import java.util.*;

/**
 * J2EE Applications look up resources registered with the Application server,
 * using portable JNDI names.                    
 */

/* @XmlType(name = "", propOrder = {
    "customResourceOrExternalJndiResourceOrJdbcResourceOrMailResourceOrAdminObjectResourceOrConnectorResourceOrResourceAdapterConfigOrJdbcConnectionPoolOrConnectorConnectionPool"
}) */


@Configured
public interface Resources extends ConfigBeanProxy, Injectable  {

    /**
     * Gets the value of the customResourceOrExternalJndiResourceOrJdbcResourceOrMailResourceOrAdminObjectResourceOrConnectorResourceOrResourceAdapterConfigOrJdbcConnectionPoolOrConnectorConnectionPool property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the customResourceOrExternalJndiResourceOrJdbcResourceOrMailResourceOrAdminObjectResourceOrConnectorResourceOrResourceAdapterConfigOrJdbcConnectionPoolOrConnectorConnectionPool property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCustomResourceOrExternalJndiResourceOrJdbcResourceOrMailResourceOrAdminObjectResourceOrConnectorResourceOrResourceAdapterConfigOrJdbcConnectionPoolOrConnectorConnectionPool().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link CustomResource }
     * {@link ExternalJndiResource }
     * {@link JdbcResource }                                       �
     * {@link MailResource }
     * {@link AdminObjectResource }
     * {@link ConnectorResource }
     * {@link ResourceAdapterConfig }
     * {@link JdbcConnectionPool }
     * {@link ConnectorConnectionPool }
     */
    @Element("*")
    public List<Resource> getResources();

    @DuckTyped
    public <T> Collection<T> getResources(Class<T> type);

    @DuckTyped
    public <T> Resource getResourceByName(Class<T> type, String name);

    @DuckTyped
    public Collection<BindableResource> getResourcesOfPool(String connectionPoolName);

    public class Duck {

        public static <T> Collection<T> getResources(Resources resources, Class<T> type){
            Collection<T> filteredResources = new ArrayList<T>();
            for(Resource resource : resources.getResources()){
                if (type.isInstance(resource)) {
                    filteredResources.add(type.cast(resource));
                }
            }
            return filteredResources;
        }

        public static Collection<BindableResource> getResourcesOfPool(Resources resources, String connectionPoolName){
            Set<BindableResource> resourcesReferringPool = new HashSet<BindableResource>();
            ResourcePool pool = (ResourcePool) resources.getResourceByName(ResourcePool.class, connectionPoolName);
            if (pool != null) {
                Collection<BindableResource> bindableResources = getResources(resources, BindableResource.class);
                for (BindableResource resource : bindableResources) {
                    if (ConnectorResource.class.isAssignableFrom(resource.getClass())) {
                        if ((((ConnectorResource) resource).getPoolName()).equals(connectionPoolName)) {
                            resourcesReferringPool.add(resource);
                        }
                    } else if (JdbcResource.class.isAssignableFrom(resource.getClass())) {
                        if ((((JdbcResource) resource).getPoolName()).equals(connectionPoolName)) {
                            resourcesReferringPool.add(resource);
                        }
                    }
                }
            }
            return resourcesReferringPool;
        }

        public static <T> Resource getResourceByName(Resources resources, Class<T> type, String name){
            Resource foundRes = null;
            Collection<T> typedResources ;
            boolean bindableResource = BindableResource.class.isAssignableFrom(type);
            boolean poolResource = ResourcePool.class.isAssignableFrom(type);

            boolean workSecurityMap = WorkSecurityMap.class.isAssignableFrom(type);
            boolean rac = ResourceAdapterConfig.class.isAssignableFrom(type);



            Class c;
            if(bindableResource){
                c = BindableResource.class;
            }else if(poolResource){
                c = ResourcePool.class;
            }else if(workSecurityMap){
                c = WorkSecurityMap.class;
            }else if(rac){
                c = ResourceAdapterConfig.class;
            }else{
                //do not handle any other resource type
                return null;
            }
            typedResources = resources.getResources(c);

            Iterator itr = typedResources.iterator();
            while(itr.hasNext()){
                String resourceName = null;
                Resource res = (Resource)itr.next();
                if(bindableResource){
                    resourceName = ((BindableResource)res).getJndiName();
                } else if(poolResource){
                    resourceName = ((ResourcePool)res).getName();
                } else if(rac){
                    resourceName = ((ResourceAdapterConfig)res).getResourceAdapterName();
                } else if(workSecurityMap){
                    resourceName = ((WorkSecurityMap)res).getName();
                }
                if(name.equals(resourceName)){
                    foundRes = res;
                    break;
                }
            }
            // make sure that the "type" provided and the matched resource are compatible.
            // eg: its possible that the requested resource is "ConnectorResource",
            // and matching resource is "JdbcResource" as we filter based on
            // the generic type (in this case BindableResource) and not on exact type.
            if(type != null && foundRes != null && type.isAssignableFrom(foundRes.getClass())){
                return foundRes;
            }else{
                return null;
            }
        }
    }
}
