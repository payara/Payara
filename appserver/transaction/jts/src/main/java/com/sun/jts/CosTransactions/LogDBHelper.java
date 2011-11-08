/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jts.CosTransactions;

import java.util.Map;
import java.util.HashMap;
import java.sql.*;
import javax.sql.*;
import javax.naming.*;
import java.lang.reflect.Method;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/** The LogDBHelper class takes care of writing the transaction logs
  *  into database.
  * @author Sun Micro Systems, Inc
*/

class LogDBHelper {

    String resName = "jdbc/TxnDS";
    DataSource ds = null;
    Method getNonTxConnectionMethod = null;
    static final String insertStatement = 
	         System.getProperty("com.sun.jts.dblogging.insertquery",
                 "insert into  txn_log_table values ( ? , ? , ? , ? )");
    static final String deleteStatement = 
	         System.getProperty("com.sun.jts.dblogging.deletequery",
                 "delete from txn_log_table where localtid = ? and servername = ? ");
    static final String selectStatement = 
	         System.getProperty("com.sun.jts.dblogging.selectquery",
                 "select * from txn_log_table where servername = ? ");
    static final String selectServerNameStatement = 
	         System.getProperty("com.sun.jts.dblogging.selectservernamequery",
                 "select distinct servername from txn_log_table where instancename = ? ");
    static final String createTableStatement = 
                 "create table txn_log_table (localtid varchar(20), servername varchar(150), instancename varchar(150), gtrid blob)";
    static final boolean useNonTxConnectionForAddRecord = Boolean.getBoolean("com.sun.jts.dblogging.use.nontx.connection.for.add");
    static Logger _logger = LogDomains.getLogger(LogDBHelper.class, LogDomains.TRANSACTION_LOGGER);
    static LogDBHelper _instance = new LogDBHelper();

    static LogDBHelper getInstance() {
        return _instance;
    }

    LogDBHelper() {
        if (Configuration.getPropertyValue(Configuration.DB_LOG_RESOURCE) != null) {
            resName = Configuration.getPropertyValue(Configuration.DB_LOG_RESOURCE);
        }
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource)ctx.lookup(resName);
	    Class cls = ds.getClass();
	    getNonTxConnectionMethod = cls.getMethod("getNonTxConnection", null);

            initTable();

        } catch (Throwable t) {
            _logger.log(Level.SEVERE,"jts.unconfigured_db_log_resource",resName);
            _logger.log(Level.SEVERE,"",t);
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("LogDBHelper.resName: " + resName);
            _logger.fine("LogDBHelper.ds: " + ds);
            _logger.fine("LogDBHelper.getNonTxConnectionMethod: " + getNonTxConnectionMethod);
        }
    }


    boolean addRecord(long localTID, byte[] data) {
        if (ds != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("LogDBHelper.addRecord for localTID: " + localTID);
                _logger.fine("LogDBHelper.addRecord for serverName: " + Configuration.getServerName());
                _logger.fine("LogDBHelper.addRecord for instanceName: " + Configuration.getPropertyValue(Configuration.INSTANCE_NAME));
            }
            Connection conn = null;
            PreparedStatement prepStmt1 = null;    
            try {
                if (useNonTxConnectionForAddRecord)
		    conn = (Connection)(getNonTxConnectionMethod.invoke(ds, null)); 
                else 
                    conn = ds.getConnection();
                prepStmt1 = conn.prepareStatement(insertStatement);
                prepStmt1.setString(1,Long.toString(localTID));
                prepStmt1.setString(2,Configuration.getServerName());
                prepStmt1.setString(3,Configuration.getPropertyValue(Configuration.INSTANCE_NAME));
                prepStmt1.setBytes(4,data);
                prepStmt1 .executeUpdate();
                return true;
            } catch (Throwable ex) {
                _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex);
                return false;
            } finally {
                try {
                    if (prepStmt1 != null) 
                        prepStmt1.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
                try {
                    if (conn != null)
                        conn.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
            }
        }
        return false;
    }

    boolean deleteRecord(long localTID) {
        return deleteRecord(localTID, Configuration.getServerName());
    }

    boolean deleteRecord(long localTID, String serverName) {
        if (ds != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("LogDBHelper.deleteRecord for localTID: " + localTID + " and serverName: " + serverName);
            }
            Connection conn = null;
            PreparedStatement prepStmt1 = null;    
            try {
		 // To avoid compile time dependency to get NonTxConnection
		conn = (Connection)(getNonTxConnectionMethod.invoke(ds, null)); 
                prepStmt1 = conn.prepareStatement(deleteStatement);
                prepStmt1.setString(1,Long.toString(localTID));
                prepStmt1.setString(2,serverName); //Configuration.getServerName());
                prepStmt1 .executeUpdate();
                return true;
            } catch (Exception ex) {
                _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex);
                return false;
            } finally {
                try {
                if (prepStmt1 != null) 
                    prepStmt1.close();
                if (conn != null)
                    conn.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
            }
        }
        return false;
    }

    Map getGlobalTIDMap() {
        return getGlobalTIDMap(Configuration.getServerName());
    }

    Map getGlobalTIDMap(String serverName) {
        Map gtidMap = new HashMap();
        if (ds != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("LogDBHelper get records for serverName: " + serverName);
            }
            Connection conn = null;
            PreparedStatement prepStmt1 = null;    
            ResultSet rs = null;
            try {
                //conn = ds.getConnection();
		conn = (Connection)(getNonTxConnectionMethod.invoke(ds, null)); 
                prepStmt1 = conn.prepareStatement(selectStatement);
                prepStmt1.setString(1,serverName); //Configuration.getServerName());
                rs = prepStmt1.executeQuery();
                while (rs.next()) {
                    Long localTID = Long.valueOf(rs.getString(1));
                    byte[] gtridbytes = rs.getBytes(4);
                    if (gtridbytes != null) {
                        // Skip mapping record
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.fine("LogDBHelper found record for localTID: " + localTID + " and serverName: " + serverName);
                            _logger.fine("LogDBHelper GlobalTID for localTID: " + localTID + " : " + GlobalTID.fromTIDBytes(gtridbytes));
                        }
                        gtidMap.put(GlobalTID.fromTIDBytes(gtridbytes), localTID);
                    }
                }
            } catch (Exception ex) {
                _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex);
            } finally {
                try {
                    if (rs != null) 
                        rs.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
                try {
                    if (prepStmt1 != null) 
                        prepStmt1.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
                try {
                    if (conn != null)
                        conn.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
            }
        }
        return gtidMap;
    }

    String getServerNameForInstanceName(String instanceName) {
        String serverName = null;
        if (ds != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("LogDBHelper get serverName for instanceName: " + instanceName);
            }
            Connection conn = null;
            PreparedStatement prepStmt1 = null;    
            ResultSet rs = null;
            try {
                //conn = ds.getConnection();
		conn = (Connection)(getNonTxConnectionMethod.invoke(ds, null)); 
                prepStmt1 = conn.prepareStatement(selectServerNameStatement);
                prepStmt1.setString(1,instanceName);
                rs = prepStmt1.executeQuery();
                if (rs.next()) {
                    serverName = rs.getString(1);
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("LogDBHelper found serverName: " + serverName + " for instanceName: " + instanceName);
                    }
                }
            } catch (Exception ex) {
                _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex);
            } finally {
                try {
                    if (rs != null) 
                        rs.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
                try {
                    if (prepStmt1 != null) 
                        prepStmt1.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
                try {
                    if (conn != null)
                        conn.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
            }
        }
        return serverName;
    }

    private void initTable() {
        if (ds != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("LogDBHelper.initTable for serverName: " + Configuration.getServerName());
                _logger.fine("LogDBHelper.initTable for instanceName: " + Configuration.getPropertyValue(Configuration.INSTANCE_NAME));
            }
            Connection conn = null;
            Statement stmt1 = null;
            try {
                conn = (Connection)(getNonTxConnectionMethod.invoke(ds, null));
                stmt1 = conn.createStatement();
                stmt1.execute(createTableStatement);
            } catch (Exception ex) {
                _logger.log(Level.INFO,"jts.exception_in_db_log_resource_create");
                _logger.log(Level.FINE,ex.getMessage(), ex);
            } finally {
                try {
                if (stmt1 != null)
                    stmt1.close();
                if (conn != null)
                    conn.close();
                } catch (Exception ex1) {
                    _logger.log(Level.SEVERE,"jts.exception_in_db_log_resource",ex1);
                }
            }
            // Add a mapping between the serverName and the instanceName
            if (getServerNameForInstanceName(Configuration.getPropertyValue(Configuration.INSTANCE_NAME)) == null) {
                addRecord(0, null);
            }
        }
    }   
}
