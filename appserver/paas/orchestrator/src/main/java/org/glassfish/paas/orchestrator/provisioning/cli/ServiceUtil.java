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
package org.glassfish.paas.orchestrator.provisioning.cli;


import org.glassfish.paas.orchestrator.provisioning.CloudRegistryEntry;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryService;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryEntry.Type;

import static org.glassfish.paas.orchestrator.provisioning.CloudRegistryService.*;


import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ServiceUtil implements PostConstruct {

    private static ExecutorService threadPool = Executors.newFixedThreadPool(1);

    public static final String NODE_PREFIX = "node-";
    public static final String INSTANCE_PREFIX = "instance-";

    public static enum SERVICE_TYPE {APPLICATION_SERVER, DATABASE, LOAD_BALANCER}

    public final static Map<SERVICE_TYPE, String> serviceTypeTableMapping;

    static {
        serviceTypeTableMapping = new HashMap<SERVICE_TYPE, String>();
        serviceTypeTableMapping.put(SERVICE_TYPE.APPLICATION_SERVER, CLOUD_TABLE_NAME);
        serviceTypeTableMapping.put(SERVICE_TYPE.DATABASE, CLOUD_DB_TABLE_NAME);
        serviceTypeTableMapping.put(SERVICE_TYPE.LOAD_BALANCER, CLOUD_LB_TABLE_NAME);
    }

    @Inject
    private CloudRegistryService cloudRegistryService;

    public static ExecutorService getThreadPool() {
        return threadPool;
    }

    private static final String SEPARATOR = ".";
    private DataSource ds = null;


    public boolean hasDomainName(String serviceName) {
        boolean hasDomainName = false;
        if (serviceName != null && !serviceName.isEmpty()) {
            hasDomainName = true;
        }
        return hasDomainName;
    }

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

    public boolean isDomain(String serviceName) {
        boolean isDomain = false;
        if (!serviceName.contains(SEPARATOR)) {
            isDomain = true;
        }
        return isDomain;
    }

    public boolean isCluster(String serviceName) {
        boolean isCluster = false;
        if (serviceName.contains(SEPARATOR) && serviceName.indexOf(SEPARATOR) == serviceName.lastIndexOf(SEPARATOR)) {
            String serviceType = getServiceType(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            if (serviceType != null && serviceType.equalsIgnoreCase(Type.Cluster.toString())) {
                isCluster = true;
            }
        }
        return isCluster;
    }

    public boolean isStandaloneInstance(String serviceName) {
        boolean isStandaloneInstance = false;
        if (serviceName.contains(SEPARATOR) && serviceName.indexOf(SEPARATOR) == serviceName.lastIndexOf(SEPARATOR)) {
            String serviceType = getServiceType(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            if (serviceType != null && serviceType.equalsIgnoreCase(Type.StandAloneInstance.toString())) {
                isStandaloneInstance = true;
            }
        }
        return isStandaloneInstance;
    }

    public boolean isInstance(String serviceName) {
        boolean instance = false;

        if (isStandaloneInstance(serviceName) || isClusteredInstance(serviceName)) {
            instance = true;
        }
        return instance;
    }

    public String getInstanceName(String serviceName) {
        String instanceName = null;
        if (isInstance(serviceName)) {
            if (isStandaloneInstance(serviceName)) {
                instanceName = getStandaloneInstanceName(serviceName);
            } else if (isClusteredInstance(serviceName)) {
                instanceName = getClusteredInstanceName(serviceName);
            }
        } else {
            throw new RuntimeException("not an instance [" + serviceName + "]");
        }
        return instanceName;
    }

    public String getStandaloneInstanceName(String serviceName) {
        String standaloneInstanceName = null;
        if (isStandaloneInstance(serviceName)) {
            standaloneInstanceName = serviceName.substring(serviceName.indexOf(SEPARATOR) + 1);
        }
        return standaloneInstanceName;
    }

    public String getClusterName(String serviceName) {
        String clusterName = null;
        if (isCluster(serviceName)) {
            if (serviceName.contains(SEPARATOR) && serviceName.indexOf(SEPARATOR) == serviceName.lastIndexOf(SEPARATOR)) {
                clusterName = serviceName.substring(serviceName.indexOf(SEPARATOR) + 1);
            }
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
            if (count == 2) {
                isInstance = true;
            }
        }
        return isInstance;
    }

    public void postConstruct() {
        InitialContext ic = null;
        try {
            ic = new InitialContext();
            ds = (DataSource) ic.lookup(CloudRegistryService.RESOURCE_NAME);
        } catch (NamingException e) {
            throw new RuntimeException("Unable to get datasource : " + CloudRegistryService.RESOURCE_NAME);
        }
    }

    public boolean isValidService(String serviceName, SERVICE_TYPE type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        return entry != null;
    }

    public String getTableName(SERVICE_TYPE type) {
        String tableName = serviceTypeTableMapping.get(type);
        if (tableName == null) {
            throw new RuntimeException("Unable to find TABLE_NAME for service type [" + type + "], " +
                    "service type must be one of [" + Arrays.toString(SERVICE_TYPE.values()) + "]");
        }
        return tableName;
    }

    public void updateInstanceID(String serviceName, String instanceID, SERVICE_TYPE type) {
        String tableName = getTableName(type);
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            //check whether the serviceName exists
            final String query = "update " + tableName +
                    " set " + CloudRegistryService.CLOUD_COLUMN_INSTANCE_ID + "='" + instanceID + "'  " +
                    "where " + CLOUD_COLUMN_CLOUD_NAME + " = '" + serviceName + "'";
            System.out.println("Executing query : " + query);
            con = ds.getConnection();
            stmt = prepareStatement(con, query);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDBObjects(con, stmt, null);
        }
    }

    public void updateState(String serviceName, String state, SERVICE_TYPE type) {
        String tableName = getTableName(type);
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            //check whether the serviceName exists
            final String query = "update " + tableName +
                    " set " + CloudRegistryService.CLOUD_COLUMN_STATE + "='" + state + "'  " +
                    "where " + CLOUD_COLUMN_CLOUD_NAME + " = '" + serviceName + "'";
            System.out.println("Executing query : " + query);
            con = ds.getConnection();
            stmt = prepareStatement(con, query);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDBObjects(con, stmt, null);
        }
    }


    public void updateIPAddress(String serviceName, String IPAddress, SERVICE_TYPE type) {
        String tableName = getTableName(type);
        Connection con = null;
        PreparedStatement stmt = null;
        boolean valid = false;
        try {
            //check whether the serviceName exists
            final String query = "update " + tableName +
                    " set " + CloudRegistryService.CLOUD_COLUMN_IP_ADDRESS + "='" + IPAddress + "'  " +
                    "where " + CLOUD_COLUMN_CLOUD_NAME + " = '" + serviceName + "'";
            System.out.println("Executing query : " + query);
            con = ds.getConnection();
            stmt = prepareStatement(con, query);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDBObjects(con, stmt, null);
        }
    }


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

    public String getServiceType(String serviceName, SERVICE_TYPE type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        if (entry != null) {
            return entry.getServerType();
        } else {
            return null;
        }
    }

    public String getServiceState(String serviceName, SERVICE_TYPE type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        if (entry != null) {
            return entry.getState();
        } else {
            return null;
        }
    }

    public String getIPAddress(String serviceName, SERVICE_TYPE type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        if (entry != null) {
            return entry.getIpAddress();
        } else {
            return null;
        }
    }

    public String getInstanceID(String serviceName, SERVICE_TYPE type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        if (entry != null) {
            return entry.getInstanceId();
        } else {
            return null;
        }
    }


    public String getServiceName(final String ipAddress, SERVICE_TYPE type) {
        String tableName = getTableName(type);

        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String serviceName = null;
        try {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("select ").append(CLOUD_COLUMN_CLOUD_NAME).append(" from ").append(tableName).append(" where IP_ADDRESS = ?");
            con = ds.getConnection();
            final String query = stringBuffer.toString();
            stmt = prepareStatement(con, query);
            stmt.setString(1, ipAddress);
            rs = stmt.executeQuery();
            if (rs.next()) {
                serviceName = rs.getString("CLOUD_NAME");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDBObjects(con, stmt, rs);
        }
        return serviceName;
    }

    private PreparedStatement prepareStatement(Connection con, final String query)
            throws SQLException {
        return con.prepareStatement(query);
    }

    public String generateNodeName(String suffix) {
        return NODE_PREFIX + suffix;
    }

    public String generateInstanceName(String suffix) {
        return INSTANCE_PREFIX + suffix;
    }


    public String getNextID(String serviceName) {
        String domainName = getDomainName(serviceName);

        List<String> instances = new ArrayList<String>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            StringBuffer query = new StringBuffer();
            query.append("select * from " + CloudRegistryService.CLOUD_TABLE_NAME + " where CLOUD_NAME like '" + domainName + ".%' and " +
                    CloudRegistryService.CLOUD_COLUMN_SERVER_TYPE + "='" + Type.StandAloneInstance.toString() + "' " +
                    "or " + CloudRegistryService.CLOUD_COLUMN_SERVER_TYPE + "='" + Type.ClusterInstance + "'");

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

    public void closeDBObjects(Connection con, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                //ignore
            }
        }

        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                //ignore
            }
        }

        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    public CloudRegistryEntry retrieveCloudEntry(String serviceName, SERVICE_TYPE type) {
        String tableName = getTableName(type);
        DataSource ds = cloudRegistryService.getDataSource();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        CloudRegistryEntry entry = null;
        try {
            conn = ds.getConnection();
            final String sql = "select * from " + tableName + " where " +
                    CLOUD_COLUMN_CLOUD_NAME + "='" + serviceName + "'";

            stmt = prepareStatement(conn, sql);
            rs = stmt.executeQuery();
            if (rs.next()) {
                entry = new CloudRegistryEntry();
                entry.setCloudName(rs.getString(CLOUD_COLUMN_CLOUD_NAME));
                entry.setIpAddress(rs.getString(CLOUD_COLUMN_IP_ADDRESS));
                entry.setInstanceId(rs.getString(CLOUD_COLUMN_INSTANCE_ID));
                entry.setState(rs.getString(CLOUD_COLUMN_STATE));
                entry.setServerType(rs.getString(CLOUD_COLUMN_SERVER_TYPE));
            }
            System.out.println("retrieved cloud entry [ " + entry + "] of type [" + type + "]");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeDBObjects(conn, stmt, rs);
        }
        return entry;
    }


    private void registerCloudEntry(CloudRegistryEntry entry, String tableName, String type) {
        DataSource ds = cloudRegistryService.getDataSource();

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = ds.getConnection();
            String sql = "INSERT into " + tableName + " values(?,?,?,?,?)";

            stmt = prepareStatement(conn, sql);
            stmt.setString(1, entry.getCloudName());
            stmt.setString(2, entry.getIpAddress());
            stmt.setString(3, entry.getInstanceId());
            stmt.setString(4, entry.getServerType());
            stmt.setString(5, entry.getState());

            int retVal = stmt.executeUpdate();
            System.out.println("Executed [ " + stmt + " ], retVal = [" + retVal + "]");

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeDBObjects(conn, stmt, null);
        }
        System.out.println("Registered cloud entry [ " + entry + "] for type [" + type + "]");
    }


    public boolean isServiceAlreadyConfigured(String serviceName, SERVICE_TYPE type) {
        String tableName = getTableName(type);
        DataSource ds = cloudRegistryService.getDataSource();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet resultSet = null;

        try {
            conn = ds.getConnection();
            final String sql = "SELECT " + CLOUD_COLUMN_CLOUD_NAME + " from " + tableName;
            stmt = prepareStatement(conn, sql);
            resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                if (serviceName.equals(resultSet.getString(CLOUD_COLUMN_CLOUD_NAME).trim())) {
                    return true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeDBObjects(conn, stmt, resultSet);
        }
        return false;
    }

    public void registerLBInfo(CloudRegistryEntry entry) {
        registerCloudEntry(entry, CLOUD_LB_TABLE_NAME, "LOAD_BALANCER");
    }

    public void registerDBInfo(CloudRegistryEntry entry) {
        registerCloudEntry(entry, CLOUD_DB_TABLE_NAME, "DATABASE");
    }

    public void registerASInfo(CloudRegistryEntry entry) {
        registerCloudEntry(entry, CLOUD_TABLE_NAME, "APPLICATION_SERVER");
    }

    public void createTable(ServiceUtil.SERVICE_TYPE type) {
        String tableName = getTableName(type);
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            InitialContext ic = new InitialContext();
            ds = (DataSource) ic.lookup(RESOURCE_NAME);
            con = ds.getConnection();
            final String query = "create table " + tableName + " (" + CLOUD_COLUMN_CLOUD_NAME + " varchar(50), " + CLOUD_COLUMN_IP_ADDRESS + " varchar(100), " +
                    "" + CLOUD_COLUMN_INSTANCE_ID + " varchar(15), " + CLOUD_COLUMN_SERVER_TYPE + " varchar(20), " + CLOUD_COLUMN_STATE + " varchar(20))";
            stmt = prepareStatement(con, query);
            stmt.executeUpdate();
            debug("create table " + tableName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            closeDBObjects(con, stmt, null);
        }
    }

    private void debug(String message) {
        System.out.println("[ServiceUtil] " + message);
    }

}
