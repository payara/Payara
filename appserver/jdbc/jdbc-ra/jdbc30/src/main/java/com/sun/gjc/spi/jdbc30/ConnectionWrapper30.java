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

package com.sun.gjc.spi.jdbc30;

import com.sun.gjc.spi.ManagedConnectionImpl;
import com.sun.gjc.spi.base.ConnectionWrapper;

import java.sql.*;

/**
 * Wrapper class that aids to provide wrapper for the following JDBC objects : <br>
 * Statement, PreparedStatement, CallableStatement, DatabaseMetaData
 */
public class ConnectionWrapper30 extends ConnectionHolder30 implements ConnectionWrapper {

    /**
     * Instantiates connection wrapper to wrap JDBC objects.
     *
     * @param con           Connection that is wrapped
     * @param mc            Managed Connection
     * @param cxRequestInfo Connection Request Info
     */
    public ConnectionWrapper30(Connection con, ManagedConnectionImpl mc,
                               javax.resource.spi.ConnectionRequestInfo cxRequestInfo) {
        super(con, mc, cxRequestInfo);
    }

    /**
     * Creates a statement from the underlying Connection
     *
     * @return <code>Statement</code> object.
     * @throws java.sql.SQLException In case of a database error.
     */
    public Statement createStatement() throws SQLException {
        return new StatementWrapper30(this, super.createStatement());
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
        return new StatementWrapper30(this, super.createStatement(resultSetType,
                resultSetConcurrency));
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
        return new StatementWrapper30(this, super.createStatement(resultSetType,
                resultSetConcurrency, resultSetHoldability));
    }

    /**
     * Retrieves the <code>DatabaseMetaData</code>object from the underlying
     * <code> Connection </code> object.
     *
     * @return <code>DatabaseMetaData</code> object.
     * @throws SQLException In case of a database error.
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        return new DatabaseMetaDataWrapper30(this, super.getMetaData());
    }


    /**
     * Creates a <code> CallableStatement </code> object for calling database
     * stored procedures.
     *
     * @param sql SQL Statement
     * @return <code> CallableStatement</code> object.
     * @throws java.sql.SQLException In case of a database error.
     */
    public CallableStatement prepareCall(String sql) throws SQLException {
        return mc.prepareCachedCallableStatement(this, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
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
        return mc.prepareCachedCallableStatement(this, sql, resultSetType, resultSetConcurrency);
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
        return mc.prepareCachedCallableStatement(this, sql, resultSetType, 
                resultSetConcurrency, resultSetHoldability);
    }

    /**
     * Creates a <code> PreparedStatement </code> object for sending
     * paramterized SQL statements to database
     *
     * @param sql SQL Statement
     * @return <code> PreparedStatement</code> object.
     * @throws SQLException In case of a database error.
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return mc.prepareCachedStatement(this, sql, ResultSet.TYPE_FORWARD_ONLY, 
                ResultSet.CONCUR_READ_ONLY);
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
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return mc.prepareCachedStatement(this, sql, autoGeneratedKeys);
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
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return mc.prepareCachedStatement(this, sql, columnIndexes);
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
    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency) throws SQLException {
        return mc.prepareCachedStatement(this, sql, resultSetType, resultSetConcurrency);
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
    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException {
        return mc.prepareCachedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
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
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return mc.prepareCachedStatement(this, sql, columnNames);
    }

    public PreparedStatementWrapper30 prepareCachedStatement(String sql, 
            int resultSetType, int resultSetConcurrency, boolean enableCaching) 
            throws SQLException {
        return new PreparedStatementWrapper30(this, super.prepareStatement(sql,
                resultSetType, resultSetConcurrency), enableCaching);
    }

    public PreparedStatementWrapper30 prepareCachedStatement(String sql, 
            int resultSetType, int resultSetConcurrency, 
            int resultSetHoldability, boolean enableCaching) 
            throws SQLException {
        return new PreparedStatementWrapper30(this, super.prepareStatement(sql,
                resultSetType, resultSetConcurrency, resultSetHoldability), enableCaching);
    }

    public PreparedStatementWrapper30 prepareCachedStatement(String sql,
            String[] columnNames, boolean enableCaching)
            throws SQLException {
        return new PreparedStatementWrapper30(this,
                super.prepareStatement(sql, columnNames), enableCaching);
    }
    
    public PreparedStatementWrapper30 prepareCachedStatement(String sql,
            int[] columnIndexes, boolean enableCaching)
            throws SQLException {
        return new PreparedStatementWrapper30(this,
                super.prepareStatement(sql, columnIndexes), enableCaching);
    }

    public PreparedStatementWrapper30 prepareCachedStatement(String sql,
            int autoGeneratedKeys, boolean enableCaching)
            throws SQLException {
        return new PreparedStatementWrapper30(this,
                super.prepareStatement(sql, autoGeneratedKeys), enableCaching);
    }

    public CallableStatementWrapper30 callableCachedStatement(String sql, 
            int resultSetType, int resultSetConcurrency, boolean enableCaching) 
            throws SQLException {
        return new CallableStatementWrapper30(this, super.prepareCall(sql,
                resultSetType, resultSetConcurrency), enableCaching);
    }

    public CallableStatementWrapper30 callableCachedStatement(String sql, 
            int resultSetType, int resultSetConcurrency, 
            int resultSetHoldability, boolean enableCaching) 
            throws SQLException {
        return new CallableStatementWrapper30(this, super.prepareCall(sql,
                resultSetType, resultSetConcurrency, resultSetHoldability), enableCaching);
    }

}
