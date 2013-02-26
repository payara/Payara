/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.session;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.util.CustomObjectInputStream;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.io.*;
import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Implementation of the <code>Store</code> interface that stores
 * serialized session objects in a database.  Sessions that are
 * saved are still subject to being expired based on inactivity.
 *
 * @author Bip Thelin
 * @version $Revision: 1.4 $, $Date: 2006/11/09 01:12:51 $
 */

public class JDBCStore extends StoreBase {

    private static final ResourceBundle rb = StandardServer.log.getResourceBundle();

    @LogMessageInfo(
            message = "SQL Error {0}",
            level = "FINE"
    )
    public static final String SQL_ERROR = "AS-WEB-CORE-00343";

    @LogMessageInfo(
            message = "Loading Session {0} from database {1}",
            level = "FINE"
    )
    public static final String LOADING_SESSION = "AS-WEB-CORE-00344";

    @LogMessageInfo(
            message = "Removing Session {0} at database {1}",
            level = "FINE"
    )
    public static final String REMOVING_SESSION = "AS-WEB-CORE-00345";

    @LogMessageInfo(
            message = "Saving Session {0} to database {1}",
            level = "FINE"
    )
    public static final String SAVING_SESSION = "AS-WEB-CORE-00346";

    @LogMessageInfo(
            message = "The database connection is null or was found to be closed. Trying to re-open it.",
            level = "FINE"
    )
    public static final String DATABASE_CONNECTION_CLOSED = "AS-WEB-CORE-00347";

    @LogMessageInfo(
            message = "The re-open on the database failed. The database could be down.",
            level = "FINE"
    )
    public static final String RE_OPEN_DATABASE_FAILED = "AS-WEB-CORE-00348";

    @LogMessageInfo(
            message = "A SQL exception occurred {0}",
            level = "FINE"
    )
    public static final String SQL_EXCEPTION = "AS-WEB-CORE-00349";

    @LogMessageInfo(
            message = "JDBC driver class not found {0}",
            level = "FINE"
    )
    public static final String JDBC_DRIVER_CLASS_NOT_FOUND = "AS-WEB-CORE-00350";

    /**
     * The descriptive information about this implementation.
     */
    protected static final String info = "JDBCStore/1.0";

    /**
     * Context name associated with this Store
     */
    private String name = null;

    /**
     * Name to register for this Store, used for logging.
     */
    protected static final String storeName = "JDBCStore";

    /**
     * Name to register for the background thread.
     */
    protected String threadName = "JDBCStore";

    /**
     * Connection string to use when connecting to the DB.
     */
    protected String connString = null;

    /**
     * The database connection.
     */
    private Connection conn = null;

    /**
     * Driver to use.
     */
    protected String driverName = null;

    // ------------------------------------------------------------- Table & cols

    /**
     * Table to use.
     */
    protected String sessionTable = "tomcat$sessions";

    /**
     * Column to use for /Engine/Host/Context name
     */
    protected String sessionAppCol = "app";

    /**
     * Id column to use.
     */
    protected String sessionIdCol = "id";

    /**
     * Data column to use.
     */
    protected String sessionDataCol = "data";

    /**
     * Is Valid column to use.
     */
    protected String sessionValidCol = "valid";

    /**
     * Max Inactive column to use.
     */
    protected String sessionMaxInactiveCol = "maxinactive";

    /**
     * Last Accessed column to use.
     */
    protected String sessionLastAccessedCol = "lastaccess";

    // ------------------------------------------------------------- SQL Variables

    /**
     * Variable to hold the <code>getSize()</code> prepared statement.
     */
    protected PreparedStatement preparedSizeSql = null;

    /**
     * Variable to hold the <code>keys()</code> prepared statement.
     */
    protected PreparedStatement preparedKeysSql = null;

    /**
     * Variable to hold the <code>save()</code> prepared statement.
     */
    protected PreparedStatement preparedSaveSql = null;

    /**
     * Variable to hold the <code>clear()</code> prepared statement.
     */
    protected PreparedStatement preparedClearSql = null;

    /**
     * Variable to hold the <code>remove()</code> prepared statement.
     */
    protected PreparedStatement preparedRemoveSql = null;

    /**
     * Variable to hold the <code>load()</code> prepared statement.
     */
    protected PreparedStatement preparedLoadSql = null;

    // ------------------------------------------------------------- Properties

    /**
     * Return the info for this Store.
     */
    public String getInfo() {
        return(info);
    }

    /**
     * Return the name for this instance (built from container name)
     */
    public String getName() {
        if (name == null) {
            Container container = manager.getContainer();
            String contextName = container.getName();
            String hostName = "";
            String engineName = "";

            if (container.getParent() != null) {
                Container host = container.getParent();
                hostName = host.getName();
                if (host.getParent() != null) {
                    engineName = host.getParent().getName();
                }
            }
            name = "/" + engineName + "/" + hostName + contextName;
        }
        return name;
    }

    /**
     * Return the thread name for this Store.
     */
    public String getThreadName() {
        return(threadName);
    }

    /**
     * Return the name for this Store, used for logging.
     */
    public String getStoreName() {
        return(storeName);
    }

    /**
     * Set the driver for this Store.
     *
     * @param driverName The new driver
     */
    public void setDriverName(String driverName) {
        String oldDriverName = this.driverName;
        this.driverName = driverName;
        support.firePropertyChange("driverName",
                                   oldDriverName,
                                   this.driverName);
        this.driverName = driverName;
    }

    /**
     * Return the driver for this Store.
     */
    public String getDriverName() {
        return(this.driverName);
    }

    /**
     * Set the Connection URL for this Store.
     *
     * @param connectionURL The new Connection URL
     */
    public void setConnectionURL(String connectionURL) {
        String oldConnString = this.connString;
        this.connString = connectionURL;
        support.firePropertyChange("connString",
                                   oldConnString,
                                   this.connString);
    }

    /**
     * Return the Connection URL for this Store.
     */
    public String getConnectionURL() {
        return(this.connString);
    }

    /**
     * Set the table for this Store.
     *
     * @param sessionTable The new table
     */
    public void setSessionTable(String sessionTable) {
        String oldSessionTable = this.sessionTable;
        this.sessionTable = sessionTable;
        support.firePropertyChange("sessionTable",
                                   oldSessionTable,
                                   this.sessionTable);
    }

    /**
     * Return the table for this Store.
     */
    public String getSessionTable() {
        return(this.sessionTable);
    }

    /**
     * Set the App column for the table.
     *
     * @param sessionAppCol the column name
     */
    public void setSessionAppCol(String sessionAppCol) {
        String oldSessionAppCol = this.sessionAppCol;
        this.sessionAppCol = sessionAppCol;
        support.firePropertyChange("sessionAppCol",
                                   oldSessionAppCol,
                                   this.sessionAppCol);
    }

    /**
     * Return the web application name column for the table.
     */
    public String getSessionAppCol() {
        return(this.sessionAppCol);
    }

    /**
     * Set the Id column for the table.
     *
     * @param sessionIdCol the column name
     */
    public void setSessionIdCol(String sessionIdCol) {
        String oldSessionIdCol = this.sessionIdCol;
        this.sessionIdCol = sessionIdCol;
        support.firePropertyChange("sessionIdCol",
                                   oldSessionIdCol,
                                   this.sessionIdCol);
    }

    /**
     * Return the Id column for the table.
     */
    public String getSessionIdCol() {
        return(this.sessionIdCol);
    }

    /**
     * Set the Data column for the table
     *
     * @param sessionDataCol the column name
     */
    public void setSessionDataCol(String sessionDataCol) {
        String oldSessionDataCol = this.sessionDataCol;
        this.sessionDataCol = sessionDataCol;
        support.firePropertyChange("sessionDataCol",
                                   oldSessionDataCol,
                                   this.sessionDataCol);
    }

    /**
     * Return the data column for the table
     */
    public String getSessionDataCol() {
        return(this.sessionDataCol);
    }

    /**
     * Set the Is Valid column for the table
     *
     * @param sessionValidCol The column name
     */
    public void setSessionValidCol(String sessionValidCol) {
        String oldSessionValidCol = this.sessionValidCol;
        this.sessionValidCol = sessionValidCol;
        support.firePropertyChange("sessionValidCol",
                                   oldSessionValidCol,
                                   this.sessionValidCol);
    }

    /**
     * Return the Is Valid column
     */
    public String getSessionValidCol() {
        return(this.sessionValidCol);
    }

    /**
     * Set the Max Inactive column for the table
     *
     * @param sessionMaxInactiveCol The column name
     */
    public void setSessionMaxInactiveCol(String sessionMaxInactiveCol) {
        String oldSessionMaxInactiveCol = this.sessionMaxInactiveCol;
        this.sessionMaxInactiveCol = sessionMaxInactiveCol;
        support.firePropertyChange("sessionMaxInactiveCol",
                                   oldSessionMaxInactiveCol,
                                   this.sessionMaxInactiveCol);
    }

    /**
     * Return the Max Inactive column
     */
    public String getSessionMaxInactiveCol() {
        return(this.sessionMaxInactiveCol);
    }

    /**
     * Set the Last Accessed column for the table
     *
     * @param sessionLastAccessedCol The column name
     */
    public void setSessionLastAccessedCol(String sessionLastAccessedCol) {
        String oldSessionLastAccessedCol = this.sessionLastAccessedCol;
        this.sessionLastAccessedCol = sessionLastAccessedCol;
        support.firePropertyChange("sessionLastAccessedCol",
                                   oldSessionLastAccessedCol,
                                   this.sessionLastAccessedCol);
    }

    /**
     * Return the Last Accessed column
     */
    public String getSessionLastAccessedCol() {
        return(this.sessionLastAccessedCol);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return an array containing the session identifiers of all Sessions
     * currently saved in this Store.  If there are no such Sessions, a
     * zero-length array is returned.
     *
     * @exception IOException if an input/output error occurred
     */
    public String[] keys() throws IOException {
        String keysSql =
            "SELECT " + sessionIdCol + " FROM " + sessionTable +
            " WHERE " + sessionAppCol + " = ?";
        ResultSet rst = null;
        String keys[] = null;
        int i;

        synchronized(this) {
            Connection _conn = getConnection();

            if(_conn == null) {
                return(new String[0]);
            }

            try {
                if(preparedKeysSql == null) {
                    preparedKeysSql = _conn.prepareStatement(keysSql);
                }

                preparedKeysSql.setString(1, getName());
                rst = preparedKeysSql.executeQuery();
                ArrayList<String> tmpkeys = new ArrayList<String>();
                if (rst != null) {
                    while(rst.next()) {
                        tmpkeys.add(rst.getString(1));
                    }
                }
                keys = tmpkeys.toArray(new String[tmpkeys.size()]);
            } catch(SQLException e) {
                String msg = MessageFormat.format(rb.getString(SQL_ERROR),
                                                  e);
                log(msg);
            } finally {
                try {
                    if(rst != null) {
                        rst.close();
                    }
                } catch(SQLException e) {
                    // Ignore
                }

                release(_conn);
            }
        }

        return(keys);
    }

    /**
     * Return an integer containing a count of all Sessions
     * currently saved in this Store.  If there are no Sessions,
     * <code>0</code> is returned.
     *
     * @exception IOException if an input/output error occurred
     */
    public int getSize() throws IOException {
        int size = 0;
        String sizeSql = 
            "SELECT COUNT(" + sessionIdCol + ") FROM " + sessionTable +
            " WHERE " + sessionAppCol + " = ?";
        ResultSet rst = null;

        synchronized(this) {
            Connection _conn = getConnection();

            if(_conn == null) {
                return(size);
            }

            try {
                if(preparedSizeSql == null) {
                    preparedSizeSql = _conn.prepareStatement(sizeSql);
                }

                preparedSizeSql.setString(1, getName());
                rst = preparedSizeSql.executeQuery();
                if (rst.next()) {
                    size = rst.getInt(1);
                }
            } catch(SQLException e) {
                String msg = MessageFormat.format(rb.getString(SQL_ERROR),
                                                  e);
                log(msg);
            } finally {
                try {
                    if(rst != null)
                        rst.close();
                } catch(SQLException e) {
                    // Ignore
                }

                release(_conn);
            }
        }
        return(size);
    }

    /**
     * Load the Session associated with the id <code>id</code>.
     * If no such session is found <code>null</code> is returned.
     *
     * @param id a value of type <code>String</code>
     * @return the stored <code>Session</code>
     * @exception ClassNotFoundException if an error occurs
     * @exception IOException if an input/output error occurred
     */
    public Session load(String id)
        throws ClassNotFoundException, IOException {
        ResultSet rst = null;
        StandardSession _session = null;
        Loader loader = null;
        ClassLoader classLoader = null;
        ObjectInputStream ois = null;
        BufferedInputStream bis = null;
        Container container = manager.getContainer();
        String loadSql =
            "SELECT " + sessionIdCol + ", " + sessionDataCol + " FROM " +
            sessionTable + " WHERE " + sessionIdCol + " = ? AND " +
            sessionAppCol + " = ?";

        synchronized(this) {
            Connection _conn = getConnection();
            if(_conn == null) {
                return(null);
            }

            try {
                if(preparedLoadSql == null) {
                    preparedLoadSql = _conn.prepareStatement(loadSql);
                }

                preparedLoadSql.setString(1, id);
                preparedLoadSql.setString(2, getName());
                rst = preparedLoadSql.executeQuery();
                if (rst.next()) {
                    bis = new BufferedInputStream(rst.getBinaryStream(2));

                    if (container != null) {
                        loader = container.getLoader();
                    }
                    if (loader != null) {
                        classLoader = loader.getClassLoader();
                    }
                    if (classLoader != null) {
                        ois = new CustomObjectInputStream(bis,
                                                          classLoader);
                    } else {
                        ois = new ObjectInputStream(bis);
                    }

                    if (debug > 0) {
                        String msg = MessageFormat.format(rb.getString(LOADING_SESSION),
                                                          new Object[] {id, sessionTable});
                        log(msg);
                    }

                    _session = StandardSession.deserialize(ois, manager);
                    _session.setManager(manager);

                } else if (debug > 0) {
                    log(getStoreName()+": No persisted data object found");
                }
            } catch(SQLException e) {
                String msg = MessageFormat.format(rb.getString(SQL_ERROR),
                                                  e);
                log(msg);
            } finally {
                try {
                    if(rst != null) {
                        rst.close();
                    }
                } catch(SQLException e) {
                    // Ignore
                }
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                release(_conn);
            }
        }

        return(_session);
    }

    /**
     * Remove the Session with the specified session identifier from
     * this Store, if present.  If no such Session is present, this method
     * takes no action.
     *
     * @param id Session identifier of the Session to be removed
     *
     * @exception IOException if an input/output error occurs
     */
    public void remove(String id) throws IOException {
        String removeSql =
            "DELETE FROM " + sessionTable + " WHERE " + sessionIdCol +
            " = ?  AND " + sessionAppCol + " = ?";

        synchronized(this) {
            Connection _conn = getConnection();

            if(_conn == null) {
                return;
            }

            try {
                if(preparedRemoveSql == null) {
                    preparedRemoveSql = _conn.prepareStatement(removeSql);
                }

                preparedRemoveSql.setString(1, id);
                preparedRemoveSql.setString(2, getName());
                preparedRemoveSql.execute();
            } catch(SQLException e) {
                String msg = MessageFormat.format(rb.getString(SQL_ERROR),
                                                  e);
                log(msg);
            } finally {
                release(_conn);
            }
        }

        if (debug > 0) {
            String msg = MessageFormat.format(rb.getString(REMOVING_SESSION),
                                              new Object[] {id, sessionTable});
            log(msg);
        }
    }

    /**
     * Remove all of the Sessions in this Store.
     *
     * @exception IOException if an input/output error occurs
     */
    public void clear() throws IOException {
        String clearSql =
            "DELETE FROM " + sessionTable + " WHERE " + sessionAppCol + " = ?";

        synchronized(this) {
            Connection _conn = getConnection();
            if(_conn == null) {
                return;
            }

            try {
                if(preparedClearSql == null) {
                    preparedClearSql = _conn.prepareStatement(clearSql);
                }

                preparedClearSql.setString(1, getName());
                preparedClearSql.execute();
            } catch(SQLException e) {
                String msg = MessageFormat.format(rb.getString(SQL_ERROR),
                                                  e);
                log(msg);
            } finally {
                release(_conn);
            }
        }
    }

    /**
     * Save a session to the Store.
     *
     * @param session the session to be stored
     * @exception IOException if an input/output error occurs
     */
    public void save(Session session) throws IOException {
        String saveSql =
            "INSERT INTO " + sessionTable + " (" + sessionIdCol + ", " +
            sessionAppCol + ", " +
            sessionDataCol + ", " +
            sessionValidCol + ", " +
            sessionMaxInactiveCol + ", " +
            sessionLastAccessedCol + ") VALUES (?, ?, ?, ?, ?, ?)";
        ObjectOutputStream oos = null;
        ByteArrayOutputStream bos = null;
        ByteArrayInputStream bis = null;
        InputStream in = null;

        synchronized(this) {
            Connection _conn = getConnection();
            if(_conn == null) {
                return;
            }

            // If sessions already exist in DB, remove and insert again.
            // TODO:
            // * Check if ID exists in database and if so use UPDATE.
            remove(session.getIdInternal());

            try {
                bos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(new BufferedOutputStream(bos));

                oos.writeObject(session);
                oos.close();

                byte[] obs = bos.toByteArray();
                int size = obs.length;
                bis = new ByteArrayInputStream(obs, 0, size);
                in = new BufferedInputStream(bis, size);

                if(preparedSaveSql == null) {
                    preparedSaveSql = _conn.prepareStatement(saveSql);
                }

                preparedSaveSql.setString(1, session.getIdInternal());
                preparedSaveSql.setString(2, getName());
                preparedSaveSql.setBinaryStream(3, in, size);
                preparedSaveSql.setString(4, session.isValid()?"1":"0");
                preparedSaveSql.setInt(5, session.getMaxInactiveInterval());
                preparedSaveSql.setLong(6, session.getLastAccessedTime());
                preparedSaveSql.execute();
            } catch(SQLException e) {
                String msg = MessageFormat.format(rb.getString(SQL_ERROR),
                                                  e);
                log(msg);
            } catch (IOException e) {
                // Ignore
            } finally {
                if (oos != null) {
                    oos.close();
                }
                if(bis != null) {
                    bis.close();
                }
                if(in != null) {
                    in.close();
                }

                release(_conn);
            }
        }

        if (debug > 0) {
            String msg = MessageFormat.format(rb.getString(SAVING_SESSION),
                                              new Object[] {session.getIdInternal(), sessionTable});
            log(msg);
        }
    }

    // --------------------------------------------------------- Protected Methods

    /**
     * Check the connection associated with this store, if it's
     * <code>null</code> or closed try to reopen it.
     * Returns <code>null</code> if the connection could not be established.
     *
     * @return <code>Connection</code> if the connection succeeded
     */
    protected Connection getConnection(){
        try {
            if(conn == null || conn.isClosed()) {
                Class.forName(driverName);
                String databaseConnClosedMsg = rb.getString(DATABASE_CONNECTION_CLOSED);
                log(databaseConnClosedMsg);
                conn = DriverManager.getConnection(connString);
                conn.setAutoCommit(true);

                if(conn == null || conn.isClosed()) {
                    String openDatabaseFailedMsg = rb.getString(RE_OPEN_DATABASE_FAILED);
                    log(openDatabaseFailedMsg);
                }
            }
        } catch (SQLException ex){
            String msg = MessageFormat.format(rb.getString(SQL_EXCEPTION),
                                              ex.toString());
            log(msg);
        } catch (ClassNotFoundException ex) {
            String msg = MessageFormat.format(rb.getString(JDBC_DRIVER_CLASS_NOT_FOUND),
                                              ex.toString());
            log(msg);
        }

        return conn;
    }

    /**
     * Release the connection, not needed here since the
     * connection is not associated with a connection pool.
     *
     * @param conn The connection to be released
     */
    protected void release(Connection conn) {
        // NOOP
    }

    /**
     * Called once when this Store is first started.
     */
    public void start() throws LifecycleException {
        super.start();

        // Open connection to the database
        this.conn = getConnection();
    }

    /**
     * Gracefully terminate everything associated with our db.
     * Called once when this Store is stoping.
     *
     */
    public void stop() throws LifecycleException {
        super.stop();

        // Close and release everything associated with our db.
        if(conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                // Ignore
            }

            if( preparedSizeSql != null ) {
                try {
                    preparedSizeSql.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }

            if( preparedKeysSql != null ) { 
                try {
                    preparedKeysSql.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }

            if( preparedSaveSql != null ) { 
                try {
                    preparedSaveSql.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }

            if( preparedClearSql != null ) { 
                try {
                    preparedClearSql.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }

            if( preparedRemoveSql != null ) { 
                try {
                    preparedRemoveSql.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }

            if( preparedLoadSql != null ) { 
                try {
                    preparedLoadSql.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }

            try {
                conn.close();
            } catch (SQLException e) {
                // Ignore
            }

            this.preparedSizeSql = null;
            this.preparedKeysSql = null;
            this.preparedSaveSql = null;
            this.preparedClearSql = null;
            this.preparedRemoveSql = null;
            this.preparedLoadSql = null;
            this.conn = null;
        }
    }
}
