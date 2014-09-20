/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.gjc.spi.base;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.gjc.spi.ManagedConnectionImpl;
import com.sun.gjc.util.MethodExecutor;
import com.sun.logging.LogDomains;

import javax.resource.ResourceException;
import java.sql.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the java.sql.Connection object, which is to be
 * passed to the application program.
 *
 * @author Binod P.G
 * @version 1.0, 02/07/23
 */
public abstract class ConnectionHolder implements Connection {

    protected Connection con;

    protected ManagedConnectionImpl mc;

    protected boolean wrappedAlready = false;

    protected boolean isClosed = false;

    protected boolean valid = true;

    protected boolean active = false;

    private javax.resource.spi.LazyAssociatableConnectionManager lazyAssocCm_;
    private javax.resource.spi.LazyEnlistableConnectionManager lazyEnlistCm_;

    private javax.resource.spi.ConnectionRequestInfo cxReqInfo_;

    private javax.resource.spi.ManagedConnectionFactory mcf_;

    protected int statementTimeout;
    protected boolean statementTimeoutEnabled;

    protected final static Logger _logger;

    static {
        _logger = LogDomains.getLogger(ManagedConnectionImpl.class, LogDomains.RSR_LOGGER);
    }
    private MethodExecutor executor = null;

    public static enum ConnectionType {
        LAZY_ENLISTABLE, LAZY_ASSOCIATABLE, STANDARD
    }

    private ConnectionType myType_ = ConnectionType.STANDARD;

    /**
     * The active flag is false when the connection handle is
     * created. When a method is invoked on this object, it asks
     * the ManagedConnection if it can be the active connection
     * handle out of the multiple connection handles. If the
     * ManagedConnection reports that this connection handle
     * can be active by setting this flag to true via the setActive
     * function, the above method invocation succeeds; otherwise
     * an exception is thrown.
     */

    protected final static StringManager sm = StringManager.getManager(
            DataSourceObjectBuilder.class);


    /**
     * Constructs a Connection holder.
     *
     * @param con <code>java.sql.Connection</code> object.
     */
    public ConnectionHolder(Connection con, ManagedConnectionImpl mc,
                            javax.resource.spi.ConnectionRequestInfo cxRequestInfo) {
        this.con = con;
        this.mc = mc;
        mcf_ = mc.getMcf();
        cxReqInfo_ = cxRequestInfo;
        statementTimeout = mc.getStatementTimeout();
        executor = new MethodExecutor();
        if (statementTimeout > 0) {
            statementTimeoutEnabled = true;
        }
    }

    /**
     * Returns the actual connection in this holder object.
     *
     * @return Connection object.
     */
    public Connection getConnection() {
        return con;
    }

    /**
     * Sets the flag to indicate that, the connection is wrapped already or not.
     *
     * @param wrapFlag
     */
    public void wrapped(boolean wrapFlag) {
        this.wrappedAlready = wrapFlag;
    }

    /**
     * Returns whether it is wrapped already or not.
     *
     * @return wrapped flag.
     */
    public boolean isWrapped() {
        return wrappedAlready;
    }

    /**
     * Returns the <code>ManagedConnection</code> instance responsible
     * for this connection.
     *
     * @return <code>ManagedConnection</code> instance.
     */
    public ManagedConnectionImpl getManagedConnection() {
        return mc;
    }

    /**
     * Replace the actual <code>java.sql.Connection</code> object with the one
     * supplied. Also replace <code>ManagedConnection</code> link.
     *
     * @param con <code>Connection</code> object.
     * @param mc  <code> ManagedConnection</code> object.
     */
    public void associateConnection(Connection con, ManagedConnectionImpl mc) {
        this.mc = mc;
        this.con = con;
    }

    /**
     * Dis-associate ManagedConnection and actual-connection from this user connection.
     * Used when lazy-connection-association is ON.
     */
    public void dissociateConnection() {
        this.mc = null;
        this.con = null;
    }

    /**
     * Clears all warnings reported for the underlying connection  object.
     *
     * @throws SQLException In case of a database error.
     */
    public void clearWarnings() throws SQLException {
        checkValidity();
        con.clearWarnings();
    }

    /**
     * Closes the logical connection.
     *
     * @throws SQLException In case of a database error.
     */
    public void close() throws SQLException {
        if (isClosed) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "jdbc.duplicate_close_connection", this);
            }
            return;
        }

        isClosed = true;
        if (mc != null) {
            //mc might be null if this is a lazyAssociatable connection
            //and has not been associated yet or has been disassociated
            mc.connectionClosed(null, this);
        }
    }

    /**
     * Invalidates this object.
     */
    public void invalidate() {
        valid = false;
    }

    /**
     * Closes the physical connection involved in this.
     *
     * @throws SQLException In case of a database error.
     */
    void actualClose() throws SQLException {
        con.close();
    }

    /**
     * Commit the changes in the underlying Connection.
     *
     * @throws SQLException In case of a database error.
     */
    public void commit() throws SQLException {
        checkValidity();
        con.commit();
    }

    /**
     * Creates a statement from the underlying Connection
     *
     * @return <code>Statement</code> object.
     * @throws SQLException In case of a database error.
     */
    public Statement createStatement() throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        Statement stmt = con.createStatement();
        if (statementTimeoutEnabled) {
            try {
                stmt.setQueryTimeout(statementTimeout);
            } catch (SQLException ex) {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Creates a statement from the underlying Connection.
     *
     * @param resultSetType        Type of the ResultSet
     * @param resultSetConcurrency ResultSet Concurrency.
     * @return <code>Statement</code> object.
     * @throws SQLException In case of a database error.
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        Statement stmt = con.createStatement(resultSetType, resultSetConcurrency);
        if (statementTimeoutEnabled) {
            try {
                stmt.setQueryTimeout(statementTimeout);
            } catch (SQLException ex) {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Creates a statement from the underlying Connection.
     *
     * @param resultSetType        Type of the ResultSet
     * @param resultSetConcurrency ResultSet Concurrency.
     * @param resultSetHoldability ResultSet Holdability.
     * @return <code>Statement</code> object.
     * @throws SQLException In case of a database error.
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency,
                                     int resultSetHoldability) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        Statement stmt = con.createStatement(resultSetType, resultSetConcurrency,
                resultSetHoldability);
        if (statementTimeoutEnabled) {
            try {
                stmt.setQueryTimeout(statementTimeout);
            } catch (SQLException ex) {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Retrieves the current auto-commit mode for the underlying <code> Connection</code>.
     *
     * @return The current state of connection's auto-commit mode.
     * @throws SQLException In case of a database error.
     */
    public boolean getAutoCommit() throws SQLException {
        checkValidity();
        return con.getAutoCommit();
    }

    /**
     * Retrieves the underlying <code>Connection</code> object's catalog name.
     *
     * @return Catalog Name.
     * @throws SQLException In case of a database error.
     */
    public String getCatalog() throws SQLException {
        checkValidity();
        return con.getCatalog();
    }

    /**
     * Retrieves the current holdability of <code>ResultSet</code> objects created
     * using this connection object.
     *
     * @return holdability value.
     * @throws SQLException In case of a database error.
     */
    public int getHoldability() throws SQLException {
        checkValidity();
        return con.getHoldability();
    }

    /**
     * Retrieves the <code>DatabaseMetaData</code>object from the underlying
     * <code> Connection </code> object.
     *
     * @return <code>DatabaseMetaData</code> object.
     * @throws SQLException In case of a database error.
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        checkValidity();
        return con.getMetaData();
    }

    /**
     * Retrieves this <code>Connection</code> object's current transaction isolation level.
     *
     * @return Transaction level
     * @throws SQLException In case of a database error.
     */
    public int getTransactionIsolation() throws SQLException {
        checkValidity();
        return con.getTransactionIsolation();
    }

    /**
     * Retrieves the <code>Map</code> object associated with
     * <code> Connection</code> Object.
     *
     * @return TypeMap set in this object.
     * @throws SQLException In case of a database error.
     */
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        checkValidity();
        return con.getTypeMap();
    }

    /**
     * Retrieves the the first warning reported by calls on the underlying
     * <code>Connection</code> object.
     *
     * @return First <code> SQLWarning</code> Object or null.
     * @throws SQLException In case of a database error.
     */
    public SQLWarning getWarnings() throws SQLException {
        checkValidity();
        return con.getWarnings();
    }

    /**
     * Retrieves whether underlying <code>Connection</code> object is closed.
     *
     * @return true if <code>Connection</code> object is closed, false
     *         if it is closed.
     * @throws SQLException In case of a database error.
     */
    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    /**
     * Set the isClosed flag based on whether the underlying <code>Connection</code>
     * object is closed.
     *
     * @param flag true if <code>Connection</code> object is closed, false if
     * its not closed.
     */
    public void setClosed(boolean flag) {
        isClosed = flag;
    }

    /**
     * Retrieves whether this <code>Connection</code> object is read-only.
     *
     * @return true if <code> Connection </code> is read-only, false other-wise
     * @throws SQLException In case of a database error.
     */
    public boolean isReadOnly() throws SQLException {
        checkValidity();
        return con.isReadOnly();
    }

    /**
     * Converts the given SQL statement into the system's native SQL grammer.
     *
     * @param sql SQL statement , to be converted.
     * @return Converted SQL string.
     * @throws SQLException In case of a database error.
     */
    public String nativeSQL(String sql) throws SQLException {
        checkValidity();
        return con.nativeSQL(sql);
    }

    /**
     * Creates a <code> CallableStatement </code> object for calling database
     * stored procedures.
     *
     * @param sql SQL Statement
     * @return <code> CallableStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public CallableStatement prepareCall(String sql) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        CallableStatement stmt = con.prepareCall(sql);
        if (statementTimeoutEnabled) {
            stmt.setQueryTimeout(statementTimeout);
        }
        return stmt;
    }

    /**
     * Creates a <code> CallableStatement </code> object for calling database
     * stored procedures.
     *
     * @param sql                  SQL Statement
     * @param resultSetType        Type of the ResultSet
     * @param resultSetConcurrency ResultSet Concurrency.
     * @return <code> CallableStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        CallableStatement stmt = con.prepareCall(sql, resultSetType, resultSetConcurrency);
        if (statementTimeoutEnabled) {
            stmt.setQueryTimeout(statementTimeout);
        }
        return stmt;
    }

    /**
     * Creates a <code> CallableStatement </code> object for calling database
     * stored procedures.
     *
     * @param sql                  SQL Statement
     * @param resultSetType        Type of the ResultSet
     * @param resultSetConcurrency ResultSet Concurrency.
     * @param resultSetHoldability ResultSet Holdability.
     * @return <code> CallableStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        CallableStatement stmt = con.prepareCall(sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
        if (statementTimeoutEnabled) {
            stmt.setQueryTimeout(statementTimeout);
        }
        return stmt;
    }

    /**
     * Creates a <code> PreparedStatement </code> object for sending
     * paramterized SQL statements to database
     *
     * @param sql SQL Statement
     * @return <code> PreparedStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        PreparedStatement stmt = con.prepareStatement(sql);
        if (statementTimeoutEnabled) {
            try {
                stmt.setQueryTimeout(statementTimeout);
            } catch (SQLException ex) {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Creates a <code> PreparedStatement </code> object for sending
     * paramterized SQL statements to database
     *
     * @param sql               SQL Statement
     * @param autoGeneratedKeys a flag indicating AutoGeneratedKeys need to be returned.
     * @return <code> PreparedStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public PreparedStatement prepareStatement(final String sql, int autoGeneratedKeys) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        PreparedStatement stmt = con.prepareStatement(sql, autoGeneratedKeys);
        if (statementTimeoutEnabled) {
            try {
                stmt.setQueryTimeout(statementTimeout);
            } catch (SQLException ex) {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Creates a <code> PreparedStatement </code> object for sending
     * paramterized SQL statements to database
     *
     * @param sql           SQL Statement
     * @param columnIndexes an array of column indexes indicating the columns that should be
     *                      returned from the inserted row or rows.
     * @return <code> PreparedStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public PreparedStatement prepareStatement(final String sql, int[] columnIndexes) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        PreparedStatement stmt = con.prepareStatement(sql, columnIndexes);
        if (statementTimeoutEnabled) {
            try {
                stmt.setQueryTimeout(statementTimeout);
            } catch (SQLException ex) {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Creates a <code> PreparedStatement </code> object for sending
     * paramterized SQL statements to database
     *
     * @param sql                  SQL Statement
     * @param resultSetType        Type of the ResultSet
     * @param resultSetConcurrency ResultSet Concurrency.
     * @return <code> PreparedStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public PreparedStatement prepareStatement(final String sql, int resultSetType,
                                              int resultSetConcurrency) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        PreparedStatement stmt = con.prepareStatement(sql, resultSetType, resultSetConcurrency);
        if (statementTimeoutEnabled) {
            try {
                stmt.setQueryTimeout(statementTimeout);
            } catch (SQLException ex) {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Creates a <code> PreparedStatement </code> object for sending
     * paramterized SQL statements to database
     *
     * @param sql                  SQL Statement
     * @param resultSetType        Type of the ResultSet
     * @param resultSetConcurrency ResultSet Concurrency.
     * @param resultSetHoldability ResultSet Holdability.
     * @return <code> PreparedStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public PreparedStatement prepareStatement(final String sql, int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        PreparedStatement stmt = con.prepareStatement(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability);
        if (statementTimeoutEnabled) {
            try {
                stmt.setQueryTimeout(statementTimeout);
            } catch (SQLException ex) {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Creates a <code> PreparedStatement </code> object for sending
     * paramterized SQL statements to database
     *
     * @param sql         SQL Statement
     * @param columnNames Name of bound columns.
     * @return <code> PreparedStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public PreparedStatement prepareStatement(final String sql, String[] columnNames) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        PreparedStatement stmt = con.prepareStatement(sql, columnNames);
        if (statementTimeoutEnabled) {
            try {
                stmt.setQueryTimeout(statementTimeout);
            } catch (SQLException ex) {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Removes the given <code>Savepoint</code> object from the current transaction.
     *
     * @param savepoint <code>Savepoint</code> object
     * @throws SQLException In case of a database error.
     */
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        checkValidity();
        con.releaseSavepoint(savepoint);
    }

    /**
     * Rolls back the changes made in the current transaction.
     *
     * @throws SQLException In case of a database error.
     */
    public void rollback() throws SQLException {
        checkValidity();
        con.rollback();
    }

    /**
     * Rolls back the changes made after the savepoint.
     *
     * @throws SQLException In case of a database error.
     */
    public void rollback(Savepoint savepoint) throws SQLException {
        checkValidity();
        con.rollback(savepoint);
    }

    /**
     * Sets the auto-commmit mode of the <code>Connection</code> object.
     *
     * @param autoCommit boolean value indicating the auto-commit mode.
     * @throws SQLException In case of a database error.
     */
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkValidity();
        con.setAutoCommit(autoCommit);
        mc.setLastAutoCommitValue(autoCommit);
    }

    /**
     * Sets the catalog name to the <code>Connection</code> object
     *
     * @param catalog Catalog name.
     * @throws SQLException In case of a database error.
     */
    public void setCatalog(String catalog) throws SQLException {
        checkValidity();
        con.setCatalog(catalog);
    }

    /**
     * Sets the holdability of <code>ResultSet</code> objects created
     * using this <code>Connection</code> object.
     *
     * @param holdability A <code>ResultSet</code> holdability constant
     * @throws SQLException In case of a database error.
     */
    public void setHoldability(int holdability) throws SQLException {
        checkValidity();
        con.setHoldability(holdability);
    }

    /**
     * Puts the connection in read-only mode as a hint to the driver to
     * perform database optimizations.
     *
     * @param readOnly true enables read-only mode, false disables it.
     * @throws SQLException In case of a database error.
     */
    public void setReadOnly(boolean readOnly) throws SQLException {
        checkValidity();
        con.setReadOnly(readOnly);
    }

    /**
     * Creates and unnamed savepoint and returns an object corresponding to that.
     *
     * @return <code>Savepoint</code> object.
     * @throws SQLException In case of a database error.
     */
    public Savepoint setSavepoint() throws SQLException {
        checkValidity();
        return con.setSavepoint();
    }

    /**
     * Creates a savepoint with the name and returns an object corresponding to that.
     *
     * @param name Name of the savepoint.
     * @return <code>Savepoint</code> object.
     * @throws SQLException In case of a database error.
     */
    public Savepoint setSavepoint(String name) throws SQLException {
        checkValidity();
        return con.setSavepoint(name);
    }

    /**
     * Creates the transaction isolation level.
     *
     * @param level transaction isolation level.
     * @throws SQLException In case of a database error.
     */
    public void setTransactionIsolation(int level) throws SQLException {
        checkValidity();
        con.setTransactionIsolation(level);
        mc.setLastTransactionIsolationLevel(level);
    }


    /**
     * Checks the validity of this object
     */
    protected void checkValidity() throws SQLException {
        if (isClosed) throw new SQLException("Connection closed");
        if (!valid) throw new SQLException("Invalid Connection");
        if (active == false) {
            mc.checkIfActive(this);
        }
    }

    /**
     * Sets the active flag to true
     *
     * @param actv boolean
     */
    public void setActive(boolean actv) {
        active = actv;
    }

    /*
     * Here this is a no-op. In the LazyEnlistableConnectionHolder, it will
     * actually fire the lazyEnlist method of LazyEnlistableManagedConnection
     */
    protected void jdbcPreInvoke() throws SQLException {
        if (myType_ == ConnectionType.LAZY_ASSOCIATABLE) {
            performLazyAssociation();
        } else if (myType_ == ConnectionType.LAZY_ENLISTABLE) {
            performLazyEnlistment();
        }

    }

    protected void performLazyEnlistment() throws SQLException {
        try {
            if(lazyEnlistCm_ != null) {
                lazyEnlistCm_.lazyEnlist(mc);
            }
        } catch (ResourceException re) {
            String msg = sm.getString(
                    "jdbc.cannot_enlist", re.getMessage() +
                    " Cannnot Enlist ManagedConnection");

            SQLException sqle = new SQLException(msg);
            sqle.initCause(re);
            throw sqle;
        }

    }

    protected void performLazyAssociation() throws SQLException {
        if (mc == null) {
            try {
                if(lazyAssocCm_ != null) {
                    lazyAssocCm_.associateConnection(this, mcf_, cxReqInfo_);
                }
            } catch (ResourceException re) {
                String msg = sm.getString(
                        "jdbc.cannot_assoc", re.getMessage() +
                        " Cannnot Associate ManagedConnection");

                SQLException sqle = new SQLException(msg);
                sqle.initCause(re);
                throw sqle;
            }
        }
    }

    public void setConnectionType(ConnectionType type) {
        myType_ = type;
    }

    public ConnectionType getConnectionType() {
        return myType_;
    }

    public void setLazyAssociatableConnectionManager(
            javax.resource.spi.LazyAssociatableConnectionManager cm) {

        lazyAssocCm_ = cm;
    }

    public void setLazyEnlistableConnectionManager(
            javax.resource.spi.LazyEnlistableConnectionManager cm) {

        lazyEnlistCm_ = cm;
    }

/*
    public void setManagedConnection(ManagedConnection con) {
        this.mc = con;
    }
*/

    /**
     * Installs the given <code>Map</code> object as the tyoe map for this
     * <code> Connection </code> object.
     *
     * @param map <code>Map</code> a Map object to install.
     * @throws SQLException In case of a database error.
     */
    public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException {
        checkValidity();
        con.setTypeMap(map);
    }

    protected MethodExecutor getMethodExecutor() {
        return executor;
    }

}
