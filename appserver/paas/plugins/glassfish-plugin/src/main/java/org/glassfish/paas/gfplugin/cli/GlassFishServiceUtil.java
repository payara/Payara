/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.paas.gfplugin.cli;

import org.glassfish.hk2.PostConstruct;
import org.glassfish.hk2.scopes.Singleton;
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@org.jvnet.hk2.annotations.Service
@Scoped(Singleton.class)
public class GlassFishServiceUtil{

    private static final String SEPARATOR = ".";
    public static final String NODE_PREFIX = "node-";
    public static final String INSTANCE_PREFIX = "instance-";

    @Inject
    private ServiceUtil serviceUtil;

    public boolean isServiceAlreadyConfigured(String serviceName, String appName, ServiceType type) {
        return serviceUtil.isServiceAlreadyConfigured(serviceName, appName, type);
    }

    public String getServiceState(String serviceName, String appName, ServiceType type) {
        return serviceUtil.getServiceState(serviceName, appName, type);
    }

    public void updateInstanceID(String serviceName,  String appName,String instanceID, ServiceType type) {
        serviceUtil.updateInstanceID(serviceName, appName, instanceID, type);
    }

    // Set the value of a property in application-scoped-service config element for given service name.
    public void setProperty(String serviceName, String appName, String propName, String propValue) {
        serviceUtil.setProperty(serviceName, appName, propName, propValue);
    }

    // Get the value of a property in application-scoped-service config element for given service name.
    public String getProperty(String serviceName, String property) {
        for(Service service : serviceUtil.getServices().getServices()) {
            return service.getServiceName().equals(serviceName) ?
                    service.getProperty(property).getValue() : null;
        }
        return null;
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

    public void registerASInfo(ServiceInfo entry) {
        serviceUtil.registerCloudEntry(entry);
    }

    public void unregisterASInfo(String serviceName, String appName) {
        serviceUtil.unregisterCloudEntry(serviceName, appName);
    }

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
            if (serviceType != null && serviceType.equalsIgnoreCase(ServiceInfo.Type.Cluster.toString())) {
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
            if (serviceType != null && serviceType.equalsIgnoreCase(ServiceInfo.Type.StandAloneInstance.toString())) {
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
            final String query = "select * from " + ProvisionerUtil.CLOUD_TABLE_NAME + " where CLOUD_NAME like '" + serviceName + ".%'";
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
            query.append("select * from " + ProvisionerUtil.CLOUD_TABLE_NAME + " where CLOUD_NAME like '" + clusterName + ".%' and " +
                    ProvisionerUtil.CLOUD_COLUMN_SERVER_TYPE + "='" + ServiceInfo.Type.StandAloneInstance.toString() + "' " +
                    "or " + ProvisionerUtil.CLOUD_COLUMN_SERVER_TYPE + "='" + ServiceInfo.Type.ClusterInstance + "'");

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
