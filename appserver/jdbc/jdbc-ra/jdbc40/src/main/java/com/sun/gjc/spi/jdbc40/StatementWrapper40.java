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
    public boolean isClosed() throws SQLException {
        return jdbcStatement.isClosed();
    }

    /**
     * Requests that a <code>Statement</code> be pooled or not pooled.  The value
     * specified is a hint to the statement pool implementation indicating
     * whether the applicaiton wants the statement to be pooled.  It is up to
     * the statement pool manager as to whether the hint is used.
     * <p/>
     * The poolable value of a statement is applicable to both internal
     * statement caches implemented by the driver and external statement caches
     * implemented by application servers and other applications.
     * <p/>
     * By default, a <code>Statement</code> is not poolable when created, and
     * a <code>PreparedStatement</code> and <code>CallableStatement</code>
     * are poolable when created.
     * <p/>
     *
     * @param poolable requests that the statement be pooled if true and
     *                 that the statement not be pooled if false
     *                 <p/>
     * @throws SQLException if this method is called on a closed
     *                      <code>Statement</code>
     *                      <p/>
     * @since 1.6
     */
    public void setPoolable(boolean poolable) throws SQLException {
        jdbcStatement.setPoolable(poolable);
    }

    /**
     * Returns a  value indicating whether the <code>Statement</code>
     * is poolable or not.
     * <p/>
     *
     * @throws SQLException if this method is called on a closed
     *                      <code>Statement</code>
     *                      <p/>
     * @return        <code>true</code> if the <code>Statement</code>
     * is poolable; <code>false</code> otherwise
     * <p/>
     * @see java.sql.Statement#setPoolable(boolean) setPoolable(boolean)
     * @since 1.6
     *        <p/>
     */
    public boolean isPoolable() throws SQLException {
        return jdbcStatement.isPoolable();
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
     * <p/>
     * <p><B>Note:</B>If the columns which represent the auto-generated keys were not specified,
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
    public java.sql.ResultSet executeQuery(final String sql) throws
            java.sql.SQLException {
        ResultSet rs = jdbcStatement.executeQuery(sql);
        return new ResultSetWrapper40(this, rs);
    }
}
