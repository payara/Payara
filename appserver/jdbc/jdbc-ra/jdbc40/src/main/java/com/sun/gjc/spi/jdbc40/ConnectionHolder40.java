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

package com.sun.gjc.spi.jdbc40;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.gjc.spi.ManagedConnectionFactoryImpl;
import com.sun.gjc.spi.ManagedConnectionImpl;
import com.sun.gjc.spi.base.ConnectionHolder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import javax.resource.ResourceException;


/**
 * Holds the java.sql.Connection object, which is to be
 * passed to the application program.
 *
 * @author Jagadish Ramu
 * @version 1.0, 25-Aug-2006
 */
public class ConnectionHolder40 extends ConnectionHolder {

    protected Properties defaultClientInfo;
    protected final static StringManager localStrings =
            StringManager.getManager(ManagedConnectionFactoryImpl.class);
    protected boolean jdbc30Connection;

    /**
     * Connection wrapper given to application program
     *
     * @param con           Connection that is wrapped
     * @param mc            ManagedConnection
     * @param cxRequestInfo Connection Request Information
     */
    public ConnectionHolder40(Connection con, ManagedConnectionImpl mc,
                              javax.resource.spi.ConnectionRequestInfo cxRequestInfo,
                              boolean jdbc30Connection) {
        super(con, mc, cxRequestInfo);
        this.jdbc30Connection = jdbc30Connection;
        if (!jdbc30Connection)
            init();
    }

    /**
     * cache the default client info which can will set back during close()<br>
     * as this connection may be re-used by connection pool of application server<br>
     */
    protected void init() {
        try {
            if (isSupportClientInfo()) {
                defaultClientInfo = getClientInfo();
            }
        } catch (Throwable e) {
            _logger.log(Level.INFO, "jdbc.unable_to_get_client_info", e.getMessage());
            if(_logger.isLoggable(Level.FINEST)) {
                _logger.log(Level.FINEST, "jdbc.unable_to_get_client_info", e);
            }
        }
    }
 
    /**
     * Constructs an object that implements the <code>Clob</code> interface. The object
     * returned initially contains no data.  The <code>setAsciiStream</code>,
     * <code>setCharacterStream</code> and <code>setString</code> methods of
     * the <code>Clob</code> interface may be used to add data to the <code>Clob</code>.
     *
     * @return An object that implements the <code>Clob</code> interface
     * @throws java.sql.SQLException if an object that implements the
     *                               <code>Clob</code> interface can not be constructed, this method is
     *                               called on a closed connection or a database access error occurs.
     * @throws java.sql.SQLFeatureNotSupportedException
     *                               if the JDBC driver does not support
     *                               this data type
     * @since 1.6
     */
    public Clob createClob() throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        return con.createClob();
    }

    /**
     * Constructs an object that implements the <code>Blob</code> interface. The object
     * returned initially contains no data.  The <code>setBinaryStream</code> and
     * <code>setBytes</code> methods of the <code>Blob</code> interface may be used to add data to
     * the <code>Blob</code>.
     *
     * @return An object that implements the <code>Blob</code> interface
     * @throws java.sql.SQLException if an object that implements the
     *                               <code>Blob</code> interface can not be constructed, this method is
     *                               called on a closed connection or a database access error occurs.
     * @throws java.sql.SQLFeatureNotSupportedException
     *                               if the JDBC driver does not support
     *                               this data type
     * @since 1.6
     */
    public Blob createBlob() throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        return con.createBlob();
    }

    /**
     * Constructs an object that implements the <code>NClob</code> interface. The object
     * returned initially contains no data.  The <code>setAsciiStream</code>,
     * <code>setCharacterStream</code> and <code>setString</code> methods of the <code>NClob</code> interface may
     * be used to add data to the <code>NClob</code>.
     *
     * @return An object that implements the <code>NClob</code> interface
     * @throws java.sql.SQLException if an object that implements the
     *                               <code>NClob</code> interface can not be constructed, this method is
     *                               called on a closed connection or a database access error occurs.
     * @throws java.sql.SQLFeatureNotSupportedException
     *                               if the JDBC driver does not support
     *                               this data type
     * @since 1.6
     */
    public NClob createNClob() throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        return con.createNClob();
    }

    /**
     * Constructs an object that implements the <code>SQLXML</code> interface. The object
     * returned initially contains no data. The <code>createXmlStreamWriter</code> object and
     * <code>setString</code> method of the <code>SQLXML</code> interface may be used to add data to the <code>SQLXML</code>
     * object.
     *
     * @return An object that implements the <code>SQLXML</code> interface
     * @throws java.sql.SQLException if an object that implements the <code>SQLXML</code> interface can not
     *                               be constructed, this method is
     *                               called on a closed connection or a database access error occurs.
     * @throws java.sql.SQLFeatureNotSupportedException
     *                               if the JDBC driver does not support
     *                               this data type
     * @since 1.6
     */
    public SQLXML createSQLXML() throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        return con.createSQLXML();
    }

    /**
     * Returns true if the connection has not been closed and is still valid.
     * The driver shall submit a query on the connection or use some other
     * mechanism that positively verifies the connection is still valid when
     * this method is called.
     * <p/>
     * The query submitted by the driver to validate the connection shall be
     * executed in the context of the current transaction.
     *
     * @param timeout -		The time in seconds to wait for the database operation
     *                used to validate the connection to complete.  If
     *                the timeout period expires before the operation
     *                completes, this method returns false.  A value of
     *                0 indicates a timeout is not applied to the
     *                database operation.
     *                <p/>
     * @return true if the connection is valid, false otherwise
     * @throws java.sql.SQLException if the value supplied for <code>timeout</code>
     *                               is less then 0
     * @see java.sql.DatabaseMetaData#getClientInfoProperties
     * @since 1.6
     *        <p/>
     */
    public boolean isValid(int timeout) throws SQLException {
        checkValidity();
        return con.isValid(timeout);
    }

    /**
     * Sets the value of the client info property specified by name to the
     * value specified by value.
     * <p/>
     * Applications may use the <code>DatabaseMetaData.getClientInfoProperties</code>
     * method to determine the client info properties supported by the driver
     * and the maximum length that may be specified for each property.
     * <p/>
     * The driver stores the value specified in a suitable location in the
     * database.  For example in a special register, session parameter, or
     * system table column.  For efficiency the driver may defer setting the
     * value in the database until the next time a statement is executed or
     * prepared.  Other than storing the client information in the appropriate
     * place in the database, these methods shall not alter the behavior of
     * the connection in anyway.  The values supplied to these methods are
     * used for accounting, diagnostics and debugging purposes only.
     * <p/>
     * The driver shall generate a warning if the client info name specified
     * is not recognized by the driver.
     * <p/>
     * If the value specified to this method is greater than the maximum
     * length for the property the driver may either truncate the value and
     * generate a warning or generate a <code>SQLClientInfoException</code>.  If the driver
     * generates a <code>SQLClientInfoException</code>, the value specified was not set on the
     * connection.
     * <p/>
     * The following are standard client info properties.  Drivers are not
     * required to support these properties however if the driver supports a
     * client info property that can be described by one of the standard
     * properties, the standard property name should be used.
     * <p/>
     * <ul>
     * <li>ApplicationName	-	The name of the application currently utilizing
     * the connection</li>
     * <li>ClientUser		-	The name of the user that the application using
     * the connection is performing work for.  This may
     * not be the same as the user name that was used
     * in establishing the connection.</li>
     * <li>ClientHostname	-	The hostname of the computer the application
     * using the connection is running on.</li>
     * </ul>
     * <p/>
     *
     * @param name  The name of the client info property to set
     * @param value The value to set the client info property to.  If the
     *              value is null, the current value of the specified
     *              property is cleared.
     *              <p/>
     * @throws java.sql.SQLClientInfoException
     *          if the database server returns an error while
     *          setting the client info value on the database server or this method
     *          is called on a closed connection
     *          <p/>
     * @since 1.6
     */
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        try {
            checkValidity();
        } catch (SQLException sqe) {
            SQLClientInfoException sce = new SQLClientInfoException();
            sce.setStackTrace(sqe.getStackTrace());
            throw sce;
        }
        con.setClientInfo(name, value);
    }

    /**
     * Sets the value of the connection's client info properties.  The
     * <code>Properties</code> object contains the names and values of the client info
     * properties to be set.  The set of client info properties contained in
     * the properties list replaces the current set of client info properties
     * on the connection.  If a property that is currently set on the
     * connection is not present in the properties list, that property is
     * cleared.  Specifying an empty properties list will clear all of the
     * properties on the connection.  See <code>setClientInfo (String, String)</code> for
     * more information.
     * <p/>
     * If an error occurs in setting any of the client info properties, a
     * <code>SQLClientInfoException</code> is thrown. The <code>SQLClientInfoException</code>
     * contains information indicating which client info properties were not set.
     * The state of the client information is unknown because
     * some databases do not allow multiple client info properties to be set
     * atomically.  For those databases, one or more properties may have been
     * set before the error occurred.
     * <p/>
     *
     * @param properties the list of client info properties to set
     *                   <p/>
     * @throws java.sql.SQLClientInfoException
     *          if the database server returns an error while
     *          setting the clientInfo values on the database server or this method
     *          is called on a closed connection
     *          <p/>
     * @see java.sql.Connection#setClientInfo(String,String) setClientInfo(String, String)
     * @since 1.6
     *        <p/>
     */
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        try {
            checkValidity();
        } catch (SQLException sqe) {
            SQLClientInfoException sce = new SQLClientInfoException();
            sce.setStackTrace(sqe.getStackTrace());
            throw sce;
        }
        con.setClientInfo(properties);
    }

    /**
     * Returns the value of the client info property specified by name.  This
     * method may return null if the specified client info property has not
     * been set and does not have a default value.  This method will also
     * return null if the specified client info property name is not supported
     * by the driver.
     * <p/>
     * Applications may use the <code>DatabaseMetaData.getClientInfoProperties</code>
     * method to determine the client info properties supported by the driver.
     * <p/>
     *
     * @param name The name of the client info property to retrieve
     *             <p/>
     * @return The value of the client info property specified
     *         <p/>
     * @throws java.sql.SQLException if the database server returns an error when
     *                               fetching the client info value from the database
     *                               or this method is called on a closed connection
     *                               <p/>
     * @see java.sql.DatabaseMetaData#getClientInfoProperties
     * @since 1.6
     *        <p/>
     */
    public String getClientInfo(String name) throws SQLException {
        checkValidity();
        return con.getClientInfo(name);
    }

    /**
     * Returns a list containing the name and current value of each client info
     * property supported by the driver.  The value of a client info property
     * may be null if the property has not been set and does not have a
     * default value.
     * <p/>
     *
     * @return A <code>Properties</code> object that contains the name and current value of
     *         each of the client info properties supported by the driver.
     *         <p/>
     * @throws java.sql.SQLException if the database server returns an error when
     *                               fetching the client info values from the database
     *                               or this method is called on a closed connection
     *                               <p/>
     * @since 1.6
     */
    public Properties getClientInfo() throws SQLException {
        checkValidity();
        return con.getClientInfo();
    }

    /**
     * Returns true if the client info properties are supported.
     * The application server calls the <code>getClientInfo</code> method and the <code>setClientInfo</code> method
     * only if the driver supports the client info properties.
     * The <code>DatabaseMetaData#getClientInfoProperties</code> method is used to determine
     * whether the driver supports the client info properties or not.
     * Note that the <code>DatabaseMetaData</code> will be cached by <code>ManagedConnection</code>.
     * <p/>
     * 
     * @return true if the client info properties are supported, false otherwise
     *
     * @throws javax.resource.ResourceException if the access to connection is failed.
     *
     * @throws java.sql.SQLException if the database server returns an error when retrieving 
     *                               a list of the client info properties.
     *
     * @see java.sql.DatabaseMetaData#getClientInfoProperties
     * @since 1.6
     */
    private boolean isSupportClientInfo() throws ResourceException, SQLException {
        Boolean isSupportClientInfo = getManagedConnection().isClientInfoSupported();
        if (isSupportClientInfo != null) {
            return isSupportClientInfo;
        } else {
            ResultSet rs = getManagedConnection().getCachedDatabaseMetaData().getClientInfoProperties();
            try {
                isSupportClientInfo = rs.next();
                getManagedConnection().setClientInfoSupported(isSupportClientInfo);
                return isSupportClientInfo;
            } finally {
                try {
                rs.close();
                } catch(SQLException ex) {
                    if(_logger.isLoggable(Level.FINEST)) {
                        _logger.log(Level.FINEST, "jdbc.unable_to_get_client_info", ex);
                    }
                    return false;
                }
            }
        }
    }
    
    /**
     * Factory method for creating Array objects.
     *
     * @param typeName the SQL name of the type the elements of the array map to. The typeName is a
     *                 database-specific name which may be the name of a built-in type, a user-defined type or a standard  SQL type supported by this database. This
     *                 is the value returned by <code>Array.getBaseTypeName</code>
     * @param elements the elements that populate the returned object
     * @return an Array object whose elements map to the specified SQL type
     * @throws java.sql.SQLException if a database error occurs, the typeName is null or this method is called on a closed connection
     * @throws java.sql.SQLFeatureNotSupportedException
     *                               if the JDBC driver does not support this data type
     * @since 1.6
     */
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        return con.createArrayOf(typeName, elements);
    }

    /**
     * Factory method for creating Struct objects.
     *
     * @param typeName   the SQL type name of the SQL structured type that this <code>Struct</code>
     *                   object maps to. The typeName is the name of  a user-defined type that
     *                   has been defined for this database. It is the value returned by
     *                   <code>Struct.getSQLTypeName</code>.
     * @param attributes the attributes that populate the returned object
     * @return a Struct object that maps to the given SQL type and is populated with the given attributes
     * @throws java.sql.SQLException if a database error occurs, the typeName is null or this method is called on a closed connection
     * @throws java.sql.SQLFeatureNotSupportedException
     *                               if the JDBC driver does not support this data type
     * @since 1.6
     */
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        checkValidity();
        jdbcPreInvoke();
        return con.createStruct(typeName, attributes);
    }

    /**
     * Returns an object that implements the given interface to allow access to
     * non-standard methods, or standard methods not exposed by the proxy.
     * <p/>
     * If the receiver implements the interface then the result is the receiver
     * or a proxy for the receiver. If the receiver is a wrapper
     * and the wrapped object implements the interface then the result is the
     * wrapped object or a proxy for the wrapped object. Otherwise return the
     * the result of calling <code>unwrap</code> recursively on the wrapped object
     * or a proxy for that result. If the receiver is not a
     * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
     *
     * @param iface A Class defining an interface that the result must implement.
     * @return an object that implements the interface. May be a proxy for the actual implementing object.
     * @throws java.sql.SQLException If no object found that implements the interface
     * @since 1.6
     */
    public <T> T unwrap(Class<T> iface) throws SQLException {
        checkValidity();
        T result = null;
        if (iface.isInstance(this)) { //if iface is "java.sql.Connection"
            result = iface.cast(this);
        } else if (iface.isInstance(con)) {
            //if iface is not "java.sql.Connection" & implemented by native Connection
            Class<T> listIntf[] = new Class[]{iface};
            result = getProxyObject(con, listIntf);
        } else {
            //probably a proxy, delegating to native connection
            result = con.unwrap(iface);
            if (Connection.class.isInstance(result)) {
                // rare case : returned object implements both iface & java.sql.Connection
                Class<T> listIntf[] = new Class[]{iface, Connection.class};
                result = getProxyObject(result, listIntf);
            }
        }
        return result;
    }

    /**
     * @param actualObject Object from jdbc vendor connection's unwrap
     * @param ifaces       Interfaces for which proxy is needed
     * @return Proxy class implmenting the interfaces
     * @throws SQLException
     */
    private <T> T getProxyObject(final Object actualObject, Class<T>[] ifaces) throws SQLException {
        T result;
        InvocationHandler ih;
        try {
            ih = new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws SQLException,
                        IllegalAccessException, InvocationTargetException {
                    // When close() is called on proxy object, call close() on resource adapter's
                    // Connection Holder instead of physical connection.
                    if (method.getName().equals("close")
                            && method.getParameterTypes().length == 0) {
                        if (_logger.isLoggable(Level.FINE)) {
                            String msg = localStrings.getString("jdbc.close_called_on_proxy_object", actualObject);
                            _logger.log(Level.FINE, msg);
                        }
                        ConnectionHolder40.this.close();
                        return null;
                    }

                    // default
                    return method.invoke(actualObject, args);
                }
            };
        } catch (Exception e) {
            throw new SQLException(e.fillInStackTrace());
        }
        result = (T) Proxy.newProxyInstance(actualObject.getClass().getClassLoader(), ifaces, ih);
        return result;
    }

    /**
     * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
     * for an object that does. Returns false otherwise. If this implements the interface then return true,
     * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
     * object. If this does not implement the interface and is not a wrapper, return false.
     * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
     * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
     * returns true then calling <code>unwrap</code> with the same argument should succeed.
     *
     * @param iface a Class defining an interface.
     * @return true if this implements the interface or directly or indirectly wraps an object that does.
     * @throws java.sql.SQLException if an error occurs while determining whether this is a wrapper
     *                               for an object with the given interface.
     * @since 1.6
     */
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        checkValidity();
        boolean result;
        if (iface.isInstance(this)) {
            result = true;
        } else {
            result = con.isWrapperFor(iface);
        }
        return result;
    }

    /**
     * Closes the logical connection.<br>
     * Cleans up client specific details<br>
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
        if (!jdbc30Connection) {
            try {
                checkValidity();
                if (isSupportClientInfo()) {
                    if (defaultClientInfo == null) {
                        setClientInfo(new Properties());
                    } else {
                        setClientInfo(defaultClientInfo);
                    }
                }
            } catch (Throwable e) {
                _logger.log(Level.INFO, "jdbc.unable_to_set_client_info", e.getMessage());
                if(_logger.isLoggable(Level.FINEST)) {
                    _logger.log(Level.FINEST, "jdbc.unable_to_set_client_info", e);
                }
            }
        }
        super.close();
    }

    public void setSchema(String schema) throws SQLException {
        if(DataSourceObjectBuilder.isJDBC41()) {
            checkValidity();
            Class<?>[] valueTypes = new Class<?>[]{String.class};
            try {
                getMethodExecutor().invokeMethod(con, "setSchema", valueTypes, schema);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_connection_holder", ex);
                throw new SQLException(ex);
            }
            return;
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }

    public String getSchema() throws SQLException {
        if(DataSourceObjectBuilder.isJDBC41()) {
            checkValidity();
            try {
                return (String) getMethodExecutor().invokeMethod(con, "getSchema", null);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_connection_holder", ex);
                throw new SQLException(ex);
            }
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }

    public void setNetworkTimeout(Executor executorObj, int milliseconds)
            throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            checkValidity();
            Class<?>[] valueTypes = new Class<?>[]{Executor.class, Integer.TYPE};
            try {
                getMethodExecutor().invokeMethod(con, "setNetworkTimeout", valueTypes, executorObj, milliseconds);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_connection_holder", ex);
                throw new SQLException(ex);
            }
            return;
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }

    public int getNetworkTimeout() throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            checkValidity();
            try {
                return (Integer) getMethodExecutor().invokeMethod(con, "getNetworkTimeout", null);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_connection_holder", ex);
                throw new SQLException(ex);
            }
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }

    /**
     * Abort operation to mark the connection internally as a bad connection
     * for removal and to close the connection. This ensures that at the end
     * of the transaction, the connection is destroyed. A running thread
     * holding a connection will run to completion before the connection is
     * destroyed
     * 
     * @param executor
     * @throws SQLException
     */
    public void abort(Executor executor) throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            getManagedConnection().markForRemoval(true);
            getManagedConnection().setAborted(true);
            if(!getManagedConnection().isTransactionInProgress()) {
                close();
            }
        } else {
            throw new UnsupportedOperationException("Operation not supported in this runtime.");
        }
    }
}
