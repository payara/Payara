package org.glassfish.resourcebase.resources;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * User: naman
 * Date: 4/12/12
 * Time: 1:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
public @interface ResourceTypeOrder {
    org.glassfish.resourcebase.resources.ResourceDeploymentOrder deploymentOrder();
}
