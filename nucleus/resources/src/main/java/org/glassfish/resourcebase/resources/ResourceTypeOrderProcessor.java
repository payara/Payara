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

package org.glassfish.resourcebase.resources;

import com.sun.enterprise.config.serverbeans.Resource;
import org.jvnet.hk2.annotations.Service;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: naman
 * Date: 4/12/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */

@Service
public class ResourceTypeOrderProcessor {

    public Collection<Resource> getOrderedResources(Collection<Resource> resources) {

        List resourceList = new ArrayList(resources);
        Collections.sort(resourceList,new ResourceComparator());
        return resourceList;
    }

    private class ResourceComparator implements Comparator<Resource> {

        @Override
        public int compare(Resource o1, Resource o2) {

            Class o1Class = null;
            Class o2Class = null;
            Class[] interfaces = o1.getClass().getInterfaces();
            if(interfaces != null){
                for(Class clz : interfaces){
                    if(Resource.class.isAssignableFrom(clz)){
                        o1Class =  clz;
                    }
                }
            }

            interfaces = o2.getClass().getInterfaces();
            if(interfaces != null){
                for(Class clz : interfaces){
                    if(Resource.class.isAssignableFrom(clz)){
                        o2Class =  clz;
                    }
                }
            }

            if(!o1Class.equals(o2Class)) {
                int o1deploymentOrder = 100;
                int o2deploymentOrder = 100;
                Class<?>[] allInterfaces = o1.getClass().getInterfaces();
                for (Class<?> resourceInterface : allInterfaces) {
                    ResourceTypeOrder resourceTypeOrder = (org.glassfish.resourcebase.resources.ResourceTypeOrder) resourceInterface.getAnnotation(org.glassfish.resourcebase.resources.ResourceTypeOrder.class);
                    if (resourceTypeOrder != null) {
                        o1deploymentOrder = resourceTypeOrder.deploymentOrder().getResourceDeploymentOrder();
                    }
                }
                allInterfaces = o2.getClass().getInterfaces();
                for (Class<?> resourceInterface : allInterfaces) {
                    ResourceTypeOrder resourceTypeOrder = (ResourceTypeOrder) resourceInterface.getAnnotation(ResourceTypeOrder.class);
                    if (resourceTypeOrder != null) {
                        o2deploymentOrder = resourceTypeOrder.deploymentOrder().getResourceDeploymentOrder();
                    }
                }
                return (o2deploymentOrder>o1deploymentOrder ? -1 : (o2deploymentOrder==o1deploymentOrder ? 0 : 1));

            } else {
                int i1 = Integer.parseInt(o1.getDeploymentOrder());
                int i2 = Integer.parseInt(o2.getDeploymentOrder());
                return (i2>i1 ? -1 : (i1==i2 ? 0 : 1));
            }
        }
    }

}
