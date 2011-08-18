package org.glassfish.paas.gfplugin.cli;

import org.glassfish.hk2.PostConstruct;
import org.glassfish.hk2.scopes.Singleton;
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryEntry;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@org.jvnet.hk2.annotations.Service
@Scoped(Singleton.class)
public class GlassFishServiceUtil implements PostConstruct {

    private static final String SEPARATOR = ".";
    public static final String NODE_PREFIX = "node-";
    public static final String INSTANCE_PREFIX = "instance-";

    //private DataSource ds = null;

    @Inject
    private ServiceUtil serviceUtil;

    public void postConstruct() {
        /*InitialContext ic = null;
        try {
            ic = new InitialContext();
            ds = (DataSource) ic.lookup(CloudRegistryService.RESOURCE_NAME);
        } catch (NamingException e) {
            throw new RuntimeException("Unable to get datasource : " + CloudRegistryService.RESOURCE_NAME);
        }*/
    }

    public boolean isServiceAlreadyConfigured(String serviceName, String appName, ServiceType type) {
        return serviceUtil.isServiceAlreadyConfigured(serviceName, appName, type);
    }

    public String getServiceState(String serviceName, String appName, ServiceType type) {
        return serviceUtil.getServiceState(serviceName, appName, type);
    }

    public void updateInstanceID(String serviceName,  String appName,String instanceID, ServiceType type) {
        serviceUtil.updateInstanceID(serviceName, appName, instanceID, type);
    }

    public void updateIPAddress(String serviceName, String appName, String IPAddress, ServiceType type) {
        serviceUtil.updateIPAddress(serviceName, appName, IPAddress, type);
    }

    public String getIPAddress(String serviceName, String appName,ServiceType type) {
        return serviceUtil.getIPAddress(serviceName, appName, type);
    }

    public String getInstanceID(String serviceName,  String appName,ServiceType type) {
        return serviceUtil.getInstanceID(serviceName, appName, type);
    }

/*
    public String getServiceName(final String ipAddress, ServiceType type) {
        return serviceUtil.getServiceName(ipAddress, type);
    }
*/

    public void registerASInfo(CloudRegistryEntry entry) {
        serviceUtil.registerCloudEntry(entry, null, "APPLICATION_SERVER");
    }

/*
    public void closeDBObjects(Connection con, Statement stmt, ResultSet rs) {
        serviceUtil.closeDBObjects(con, stmt, rs);
    }
*/

    public boolean isInstance(String serviceName) {
        boolean instance = false;

        if (/*isStandaloneInstance(serviceName) ||*/ isClusteredInstance(serviceName)) {
            instance = true;
        }
        return instance;
    }

    public String getInstanceName(String serviceName) {
        String instanceName = null;
        if (isInstance(serviceName)) {
/*            if (isStandaloneInstance(serviceName)) {
                instanceName = getStandaloneInstanceName(serviceName);
            } else*/ if (isClusteredInstance(serviceName)) {
                instanceName = getClusteredInstanceName(serviceName);
            }
        } else {
            throw new RuntimeException("not an instance [" + serviceName + "]");
        }
        return instanceName;
    }

/*
    public String getStandaloneInstanceName(String serviceName) {
        String standaloneInstanceName = null;
        if (isStandaloneInstance(serviceName)) {
            standaloneInstanceName = serviceName.substring(serviceName.indexOf(SEPARATOR) + 1);
        }
        return standaloneInstanceName;
    }
*/

    public String getClusterName(String serviceName, String appName) {
        String clusterName = null;
        if (isCluster(serviceName, appName)) {
            clusterName = serviceName;
        } else if (isClusteredInstance(serviceName)) {
            clusterName = getClusterNameFromInstanceName(serviceName);
        }
        return clusterName;
    }


    public String getClusterNameFromInstanceName(String serviceName) {
        String clusterName = null;
        if (isClusteredInstance(serviceName)) {
            int firstIndex = serviceName.indexOf(SEPARATOR) + 1;
            int lastIndex = serviceName.lastIndexOf(SEPARATOR);
            clusterName = serviceName.substring(firstIndex, lastIndex);
        }
        return clusterName;
    }

    public String getClusteredInstanceName(String serviceName) {
        String instanceName = null;
        if (isClusteredInstance(serviceName)) {
            int lastIndex = serviceName.lastIndexOf(SEPARATOR);
            instanceName = serviceName.substring(lastIndex + 1);
        }
        return instanceName;
    }

    public boolean isClusteredInstance(String serviceName) {
        boolean isInstance = false;
        if (serviceName.contains(SEPARATOR)) {
            int count = 0;
            CharSequence sequence = serviceName.subSequence(0, serviceName.length() - 1);
            for (int i = 0; i < sequence.length(); i++) {
                if (sequence.charAt(i) == '.') {
                    count++;
                }
            }
            if (count == 1) {
                isInstance = true;
            }
        }
        return isInstance;
    }

    public boolean isCluster(String serviceName, String appName) {
        boolean isCluster = false;
        if(!serviceName.contains(SEPARATOR)){
            String serviceType = serviceUtil.getServiceType(serviceName, appName, ServiceType.APPLICATION_SERVER);
            if (serviceType != null && serviceType.equalsIgnoreCase(CloudRegistryEntry.Type.Cluster.toString())) {
                isCluster = true;
            }

            isCluster = true;
        }
        return isCluster;
    }

/*
    public boolean isStandaloneInstance(String serviceName) {
        boolean isStandaloneInstance = false;
        if (serviceName.contains(SEPARATOR) && serviceName.indexOf(SEPARATOR) == serviceName.lastIndexOf(SEPARATOR)) {
            String serviceType = serviceUtil.getServiceType(serviceName, ServiceType.APPLICATION_SERVER);
            if (serviceType != null && serviceType.equalsIgnoreCase(CloudRegistryEntry.Type.StandAloneInstance.toString())) {
                isStandaloneInstance = true;
            }
        }
        return isStandaloneInstance;
    }
*/

/*
    public boolean hasDomainName(String serviceName) {
        boolean hasDomainName = false;
        if (serviceName != null && !serviceName.isEmpty()) {
            hasDomainName = true;
        }
        return hasDomainName;
    }
*/

/*
    public String getDomainName(String serviceName) {
        if (hasDomainName(serviceName)) {
            if (!serviceName.contains(SEPARATOR)) {
                return serviceName;
            } else {
                return serviceName.substring(0, serviceName.indexOf(SEPARATOR));
            }
        } else {
            throw new RuntimeException("Invalid service-name  [" + serviceName + "]");
        }
    }
*/

/*
    public boolean isDomain(String serviceName) {
        boolean isDomain = false;
        if (!serviceName.contains(SEPARATOR)) {
            isDomain = true;
        }
        return isDomain;
    }
*/

/*
    private PreparedStatement prepareStatement(Connection con, final String query)
            throws SQLException {
        return con.prepareStatement(query);
    }
*/

    public Collection<String> getAllSubComponents(String serviceName, String appName){
        Services services = serviceUtil.getServices();
        List<String> subComponents = new ArrayList<String>();
        for(Service service : services.getServices()){
            if(service.getServiceName().startsWith(serviceName+".")){
                if(appName != null){
                    if(service instanceof ApplicationScopedService){
                        if(appName.equals(((ApplicationScopedService) service).getApplicationName())){
                            subComponents.add(service.getServiceName());
                        }
                    }
                }else{
                    subComponents.add(service.getServiceName());
                }
            }
        }
        return subComponents;
    }

/*
    public Collection<String> getAllSubComponents(String serviceName) {
        List<String> subComponents = new ArrayList<String>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            final String query = "select * from " + CloudRegistryService.CLOUD_TABLE_NAME + " where CLOUD_NAME like '" + serviceName + ".%'";
            con = ds.getConnection();
            stmt = prepareStatement(con, query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                subComponents.add(rs.getString("CLOUD_NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDBObjects(con, stmt, rs);
        }
        return subComponents;
    }
*/

    public String getNextID(String serviceName, String appName) {
        String clusterName = getClusterName(serviceName, appName);

        //TODO cannot assume that all service names follow a pattern. eg: mydomain.* may not belong to same service.
        Collection<String> instances = getAllSubComponents(clusterName, appName);

        int maxValue = 0;
        for (String instanceServiceName : instances) {
            if (instanceServiceName.contains(INSTANCE_PREFIX)) {
                String instanceName = getInstanceName(instanceServiceName);
                String suffix = instanceName.substring(INSTANCE_PREFIX.length());
                if (suffix != null) {
                    try {
                        int suffixValue = Integer.parseInt(suffix);
                        if (suffixValue > maxValue) {
                            maxValue = suffixValue;
                        }
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                    }
                }
            }
        }
        return Integer.toString(maxValue + 1);
    }

/*
    public String getNextID(String serviceName, String appName) {
        String clusterName = getClusterName(serviceName, appName);

        List<String> instances = new ArrayList<String>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            StringBuffer query = new StringBuffer();
            query.append("select * from " + CloudRegistryService.CLOUD_TABLE_NAME + " where CLOUD_NAME like '" + clusterName + ".%' and " +
                    CloudRegistryService.CLOUD_COLUMN_SERVER_TYPE + "='" + CloudRegistryEntry.Type.StandAloneInstance.toString() + "' " +
                    "or " + CloudRegistryService.CLOUD_COLUMN_SERVER_TYPE + "='" + CloudRegistryEntry.Type.ClusterInstance + "'");

            con = ds.getConnection();
            stmt = prepareStatement(con,query.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                System.out.println("CLOUD_NAME-getNextID : " + rs.getString("CLOUD_NAME"));
                instances.add(rs.getString("CLOUD_NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDBObjects(con, stmt, rs);
        }

        int maxValue = 0;
        for (String instanceServiceName : instances) {
            if (instanceServiceName.contains(INSTANCE_PREFIX)) {
                String instanceName = getInstanceName(instanceServiceName);
                String suffix = instanceName.substring(INSTANCE_PREFIX.length());
                if (suffix != null) {
                    try {
                        int suffixValue = Integer.parseInt(suffix);
                        if (suffixValue > maxValue) {
                            maxValue = suffixValue;
                        }
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                    }
                }
            }
        }
        return Integer.toString(maxValue + 1);
    }
*/

    public String generateNodeName(String suffix) {
        return NODE_PREFIX + suffix;
    }

    public String generateInstanceName(String suffix) {
        return INSTANCE_PREFIX + suffix;
    }

    public void updateState(String serviceName, String appName, String state, ServiceType type) {
        serviceUtil.updateState(serviceName, appName, state, type);
    }

    public boolean isValidService(String serviceName, String appName, ServiceType type) {
        return serviceUtil.isValidService(serviceName, appName, type);
    }

    public String getDASIPAddress(String serviceName){
    /*
        String domainName = getDomainName(serviceName);
        String dasIPAddress = getIPAddress(domainName, ServiceType.APPLICATION_SERVER);
    */
        //TODO for now CPAS is DAS.
        String dasIPAddress = "localhost";
        return dasIPAddress;
    }

    public Services getServices(){
        return serviceUtil.getServices();
    }

}
