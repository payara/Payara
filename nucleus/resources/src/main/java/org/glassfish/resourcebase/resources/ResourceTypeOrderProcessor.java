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
