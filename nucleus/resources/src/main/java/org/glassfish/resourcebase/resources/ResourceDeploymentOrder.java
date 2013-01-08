package org.glassfish.resourcebase.resources;

/**
 * Created with IntelliJ IDEA.
 * User: naman
 * Date: 3/1/13
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public enum ResourceDeploymentOrder {

    /*
    The number indicates the deployment order for particular resources.

    To add new resource order add constant here and give the deployment order number for that resource.
    Define @ResourceTypeOrder(deploymentOrder=ResourceDeploymentOrder.<your resource>) for your resource. For example
    check JdbcResource class.
     */

    JDBC_RESOURCE(1) , JDBC_POOL(2), CONNECTOR_RESOURCE(3), CONNECTOR_POOL(4), ADMIN_OBJECT_RESOURCE(5),
    DIAGNOSTIC_RESOURCE(6), MAIL_RESOURCE(7), CUSTOM_RESOURCE(8), EXTERNALJNDI_RESOURCE(9),
    RESOURCEADAPTERCONFIG_RESOURCE(10), WORKSECURITYMAP_RESOURCE(11), PERSISTENCE_RESOURCE(12);

    private int value;

    private ResourceDeploymentOrder(int value) {
        this.value = value;
    }

    public int getResourceDeploymentOrder() {
        return value;
    }
};
