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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2017-2020] [Payara Foundation and/or its affiliates]

package com.sun.gjc.spi.jdbc40;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.gjc.spi.ManagedConnectionFactoryImpl;
import com.sun.gjc.spi.base.StatementWrapper;

import java.sql.*;

/**
 * Wrapper for JDBC 4.0 Statement
 */
public class StatementWrapper40 extends StatementWrapper {

    protected final static StringManager localStrings =
            StringManager.getManager(ManagedConnectionFactoryImpl.class);

    /**
     * Creates a new instance of StatementWrapper for JDBC 3.0<br>
     *
     * @param con       ConnectionWrapper <br>
     * @param statement Statement that is to be wrapped<br>
     */
    public StatementWrapper40(Connection con, Statement statement) {
        super(con, statement);
    }

    /**
     * Retrieves whether this <code>Statement</code> object has been closed. A <code>Statement</code> is closed if the
     * method close has been called on it, or if it is automatically closed.
     *
     * @return true if this <code>Statement</code> object is closed; false if it is still open
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    @Override
    public boolean isClosed() throws SQLException {
        return jdbcStatement.isClosed();
    }

    /**
     * Requests that a <code>Statement</code> be pooled or not pooled.  The value
     * specified is a hint to the statement pool implementation indicating
     * whether the application wants the statement to be pooled.  It is up to
     * the statement pool manager as to whether the hint is used.
     * <p>
     * The poolable value of a statement is applicable to both internal
     * statement caches implemented by the driver and external statement caches
     * implemented by application servers and other applications.
     * </p>
     * By default, a <code>Statement</code> is not poolable when created, and
     * a <code>PreparedStatement</code> and <code>CallableStatement</code>
     * are poolable when created.
     *
     * @param poolable requests that the statement be pooled if true and
     *                 that the statement not be pooled if false
     * @throws SQLException if this method is called on a closed
     *                      <code>Statement</code>
     * @since 1.6
     */
    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        jdbcStatement.setPoolable(poolable);
    }

    /**
     * Returns a  value indicating whether the <code>Statement</code>
     * is poolable or not.
     *
     * @throws SQLException if this method is called on a closed <code>Statement</code>
     * @return        <code>true</code> if the <code>Statement</code>
     * is poolable; <code>false</code> otherwise
     * @see Statement#setPoolable(boolean)
     * @since 1.6
     */
    @Override
    public boolean isPoolable() throws SQLException {
        return jdbcStatement.isPoolable();
    }

    /**
     * Returns an object that implements the given interface to allow access to
     * non-standard methods, or standard methods not exposed by the proxy.
     * <p>
     * If the receiver implements the interface then the result is the receiver
     * or a proxy for the receiver. If the receiver is a wrapper
     * and the wrapped object implements the interface then the result is the
     * wrapped object or a proxy for the wrapped object. Otherwise return the
     * the result of calling <code>unwrap</code> recursively on the wrapped object
     * or a proxy for that result. If the receiver is not a
     * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
     * </p>
     * @param iface A Class defining an interface that the result must implement.
     * @return an object that implements the interface. May be a proxy for the actual implementing object.
     * @throws java.sql.SQLException If no object found that implements the interface
     * @since 1.6
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        T result;
        if (iface.isInstance(this)) {
            result = iface.cast(this);
        } else {
            result = jdbcStatement.unwrap(iface);
        }
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
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {

        boolean result;
        if (iface.isInstance(this)) {
            result = true;
        } else {
            result = jdbcStatement.isWrapperFor(iface);
        }
        return result;
    }

    /**
     * Retrieves any auto-generated keys created as a result of executing this
     * <code>Statement</code> object. If this <code>Statement</code> object did
     * not generate any keys, an empty <code>ResultSet</code>
     * object is returned.
     * <p>
     * </p><B>Note:</B>If the columns which represent the auto-generated keys were not specified,
     * the JDBC driver implementation will determine the columns which best represent the auto-generated keys.
     *
     * @return a <code>ResultSet</code> object containing the auto-generated key(s)
     *         generated by the execution of this <code>Statement</code> object
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed <code>Statement</code>
     * @throws java.sql.SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.4
     */
    @Override
    public java.sql.ResultSet getGeneratedKeys() throws java.sql.SQLException {
        ResultSet rs = jdbcStatement.getGeneratedKeys();
        if (rs == null)
            return null;
        return new ResultSetWrapper40(this, rs);
    }

    /**
     * Retrieves the current result as a <code>ResultSet</code> object.
     * This method should be called only once per result.
     *
     * @return the current result as a <code>ResultSet</code> object or
     *         <code>null</code> if the result is an update count or there are no more results
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed <code>Statement</code>
     * @see #execute
     */
    @Override
    public java.sql.ResultSet getResultSet() throws java.sql.SQLException {
        ResultSet rs = jdbcStatement.getResultSet();
        if (rs == null)
            return null;
        return new ResultSetWrapper40(this, rs);
    }

    /**
     * Executes the given SQL statement, which returns a single
     * <code>ResultSet</code> object.
     *
     * @param sql an SQL statement to be sent to the database, typically a
     *            static SQL <code>SELECT</code> statement
     * @return a <code>ResultSet</code> object that contains the data produced
     *         by the given query; never <code>null</code>
     * @throws SQLException if a database access error occurs,
     *                      this method is called on a closed <code>Statement</code> or the given
     *                      SQL statement produces anything other than a single
     *                      <code>ResultSet</code> object
     */
    @Override
    public java.sql.ResultSet executeQuery(final String sql) throws
            java.sql.SQLException {
        ResultSet rs = jdbcStatement.executeQuery(sql);
        return new ResultSetWrapper40(this, rs);
    }
    
    //-------------------------------- JDBC 4.2 --------------------------------
   
    /**
     *  Retrieves the current result as an update count; if the result
     * is a <code>ResultSet</code> object or there are no more results, -1
     *  is returned. 
     *
     * @return the current result as an update count; -1 if the current result
     * is a <code>ResultSet</code> object or there are no more results
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>Statement</code>
     */
    @Override
    public long getLargeUpdateCount() throws SQLException {
        return jdbcStatement.getLargeUpdateCount();
    }

    /**
     * Sets the limit for the maximum number of rows that any
     * <code>ResultSet</code> object  generated by this <code>Statement</code>
     * object can contain to the given number.
     *
     * @param max the new max rows limit; zero means there is no limit
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>
     *            or the condition {@code max >= 0} is not satisfied
     */
    @Override
    public void setLargeMaxRows(long max) throws SQLException {
        jdbcStatement.setLargeMaxRows(max);
    }

    /**
     * Retrieves the maximum number of rows that a
     * <code>ResultSet</code> object produced by this
     * <code>Statement</code> object can contain.
     * 
     * @return the current maximum number of rows for a <code>ResultSet</code>
     *         object produced by this <code>Statement</code> object;
     *         zero means there is no limit
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>Statement</code>
     */
    @Override
    public long getLargeMaxRows() throws SQLException {
        return jdbcStatement.getLargeMaxRows();
    }

    /**
     * Submits a batch of commands to the database for execution and
     * if all commands execute successfully, returns an array of update counts.
     *
     * @return an array of update counts containing one element for each
     * command in the batch.  The elements of the array are ordered according
     * to the order in which commands were added to the batch.
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code> or the
     * driver does not support batch statements. Throws {@link BatchUpdateException}
     * (a subclass of <code>SQLException</code>) if one of the commands sent to the
     * database fails to execute properly or attempts to return a result set.
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     */
    @Override
    public long[] executeLargeBatch() throws SQLException {
        return jdbcStatement.executeLargeBatch();
    }

    /**
     * Executes the given SQL statement, which may be an <code>INSERT</code>,
     * <code>UPDATE</code>, or <code>DELETE</code> statement or an
     * SQL statement that returns nothing, such as an SQL DDL statement.
     *
     * @param sql an SQL Data Manipulation Language (DML) statement,
     * such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @return either (1) the row count for SQL Data Manipulation Language
     * (DML) statements or (2) 0 for SQL statements that return nothing
     *
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>, the given
     * SQL statement produces a <code>ResultSet</code> object, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     */
    @Override
    public long executeLargeUpdate(String sql) throws SQLException {
        return jdbcStatement.executeLargeUpdate(sql);
    }

    /**
     * Executes the given SQL statement and signals the driver with the
     * given flag about whether the auto-generated keys produced by this 
     * <code>Statement</code> object should be made available for retrieval.
     *
     * @param sql an SQL Data Manipulation Language (DML) statement,
     * such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *        should be made available for retrieval;
     *         one of the following constants:
     *         <code>Statement.RETURN_GENERATED_KEYS</code>
     *         <code>Statement.NO_GENERATED_KEYS</code>
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     *
     * @exception SQLException if a database access error occurs,
     *  this method is called on a closed <code>Statement</code>, the given
     *            SQL statement returns a <code>ResultSet</code> object,
     *            the given constant is not one of those allowed, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     */
    @Override
    public long executeLargeUpdate(String sql, int autoGeneratedKeys)
            throws SQLException {
        return jdbcStatement.executeLargeUpdate(sql, autoGeneratedKeys);
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.
     * 
     * @param sql an SQL Data Manipulation Language (DML) statement,
     * such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @param columnIndexes an array of column indexes indicating the columns
     *        that should be returned from the inserted row
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     *
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>, the SQL
     * statement returns a <code>ResultSet</code> object,the second argument
     * supplied to this method is not an
     * <code>int</code> array whose elements are valid column indexes, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     */
    @Override
    public long executeLargeUpdate(String sql, int columnIndexes[]) throws SQLException {
        return jdbcStatement.executeLargeUpdate(sql, columnIndexes);
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.
     *
     * @param sql an SQL Data Manipulation Language (DML) statement,
     * such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     * @param columnNames an array of the names of the columns that should be
     *        returned from the inserted row
     * @return either the row count for <code>INSERT</code>, <code>UPDATE</code>,
     *         or <code>DELETE</code> statements, or 0 for SQL statements
     *         that return nothing
     * @exception SQLException if a database access error occurs,
     *  this method is called on a closed <code>Statement</code>, the SQL
     *            statement returns a <code>ResultSet</code> object, the
     *            second argument supplied to this method is not a <code>String</code> array
     *            whose elements are valid column names, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     */
    @Override
    public long executeLargeUpdate(String sql, String columnNames[])
            throws SQLException {
        return jdbcStatement.executeLargeUpdate(sql, columnNames);
    }

}
