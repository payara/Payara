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


    public final static Map<ServiceType, String> serviceTypeTableMapping;

    static {
        serviceTypeTableMapping = new HashMap<ServiceType, String>();
        serviceTypeTableMapping.put(ServiceType.APPLICATION_SERVER, CLOUD_TABLE_NAME);
        serviceTypeTableMapping.put(ServiceType.DATABASE, CLOUD_DB_TABLE_NAME);
        serviceTypeTableMapping.put(ServiceType.LOAD_BALANCER, CLOUD_LB_TABLE_NAME);
    }

    @Inject
    private CloudRegistryService cloudRegistryService;

    public static ExecutorService getThreadPool() {
        return threadPool;
    }


    private DataSource ds = null;


    public void postConstruct() {
        InitialContext ic = null;
        try {
            ic = new InitialContext();
            ds = (DataSource) ic.lookup(CloudRegistryService.RESOURCE_NAME);
        } catch (NamingException e) {
            throw new RuntimeException("Unable to get datasource : " + CloudRegistryService.RESOURCE_NAME);
        }
    }

    public boolean isValidService(String serviceName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        return entry != null;
    }

    public String getTableName(ServiceType type) {
        String tableName = serviceTypeTableMapping.get(type);
        if (tableName == null) {
            throw new RuntimeException("Unable to find TABLE_NAME for service type [" + type + "], " +
                    "service type must be one of [" + Arrays.toString(ServiceType.values()) + "]");
        }
        return tableName;
    }

    public void updateInstanceID(String serviceName, String instanceID, ServiceType type) {
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

    public void updateState(String serviceName, String state, ServiceType type) {
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


    public void updateIPAddress(String serviceName, String IPAddress, ServiceType type) {
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



    public String getServiceType(String serviceName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        if (entry != null) {
            return entry.getServerType();
        } else {
            return null;
        }
    }

    public String getServiceState(String serviceName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        if (entry != null) {
            return entry.getState();
        } else {
            return null;
        }
    }

    public String getIPAddress(String serviceName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        if (entry != null) {
            return entry.getIpAddress();
        } else {
            return null;
        }
    }

    public String getInstanceID(String serviceName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, type);
        if (entry != null) {
            return entry.getInstanceId();
        } else {
            return null;
        }
    }


    public String getServiceName(final String ipAddress, ServiceType type) {
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

    public CloudRegistryEntry retrieveCloudEntry(String serviceName, ServiceType type) {
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


    public void registerCloudEntry(CloudRegistryEntry entry, String tableName, String type) {
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


    public boolean isServiceAlreadyConfigured(String serviceName, ServiceType type) {
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


    public void createTable(ServiceType type) {
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
