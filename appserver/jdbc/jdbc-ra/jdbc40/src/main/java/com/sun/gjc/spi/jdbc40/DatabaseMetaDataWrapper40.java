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

import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.gjc.spi.base.DatabaseMetaDataWrapper;

import java.sql.*;
import java.util.logging.Level;
import javax.resource.ResourceException;

/**
 * Wrapper class for DatabaseMetaData for JDBC 4.0 <br>
 */
public class DatabaseMetaDataWrapper40 extends DatabaseMetaDataWrapper {

    /**
     * Creates a new instance of DatabaseMetaDataWrapper40 for JDBC 4.0
     *
     * @param con      Connection that is wrapped
     * @param metaData DatabaseMetaData that is wrapped
     */
    public DatabaseMetaDataWrapper40(Connection con, DatabaseMetaData metaData) {
        super(con, metaData);
    }

    /**
     * Indicates whether or not this data source supports the SQL <code>ROWID</code> type,
     * and if so  the lifetime for which a <code>RowId</code> object remains valid.
     * <p/>
     * The returned int values have the following relationship:
     * <pre>
     *     ROWID_UNSUPPORTED < ROWID_VALID_OTHER < ROWID_VALID_TRANSACTION
     *         < ROWID_VALID_SESSION < ROWID_VALID_FOREVER
     * </pre>
     * so conditional logic such as
     * <pre>
     *     if (metadata.getRowIdLifetime() > DatabaseMetaData.ROWID_VALID_TRANSACTION)
     * </pre>
     * can be used. Valid Forever means valid across all Sessions, and valid for
     * a Session means valid across all its contained Transactions.
     *
     * @return the status indicating the lifetime of a <code>RowId</code>
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        return databaseMetaData.getRowIdLifetime();
    }

    /**
     * Retrieves the schema names available in this database.  The results
     * are ordered by <code>TABLE_CATALOG</code> and
     * <code>TABLE_SCHEM</code>.
     * <p/>
     * <P>The schema columns are:
     * <OL>
     * <LI><B>TABLE_SCHEM</B> String => schema name
     * <LI><B>TABLE_CATALOG</B> String => catalog name (may be <code>null</code>)
     * </OL>
     *
     * @param catalog       a catalog name; must match the catalog name as it is stored
     *                      in the database;"" retrieves those without a catalog; null means catalog
     *                      name should not be used to narrow down the search.
     * @param schemaPattern a schema name; must match the schema name as it is
     *                      stored in the database; null means
     *                      schema name should not be used to narrow down the search.
     * @return a <code>ResultSet</code> object in which each row is a
     *         schema description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.6
     */
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        return databaseMetaData.getSchemas(catalog, schemaPattern);
    }

    /**
     * Retrieves whether this database supports invoking user-defined or vendor functions
     * using the stored procedure escape syntax.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return databaseMetaData.supportsStoredFunctionsUsingCallSyntax();
    }

    /**
     * Retrieves whether a <code>SQLException</code> while autoCommit is <code>true</code> inidcates
     * that all open ResultSets are closed, even ones that are holdable.  When a <code>SQLException</code> occurs while
     * autocommit is <code>true</code>, it is vendor specific whether the JDBC driver responds with a commit operation, a
     * rollback operation, or by doing neither a commit nor a rollback.  A potential result of this difference
     * is in whether or not holdable ResultSets are closed.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return databaseMetaData.autoCommitFailureClosesAllResultSets();
    }

    /**
     * Retrieves a list of the client info properties
     * that the driver supports.  The result set contains the following columns
     * <p/>
     * <ol>
     * <li><b>NAME</b> String=> The name of the client info property<br>
     * <li><b>MAX_LEN</b> int=> The maximum length of the value for the property<br>
     * <li><b>DEFAULT_VALUE</b> String=> The default value of the property<br>
     * <li><b>DESCRIPTION</b> String=> A description of the property.  This will typically
     * contain information as to where this property is
     * stored in the database.
     * </ol>
     * <p/>
     * The <code>ResultSet</code> is sorted by the NAME column
     * <p/>
     *
     * @throws SQLException if a database access error occurs
     *                      <p/>
     * @return A <code>ResultSet</code> object; each row is a supported client info
     * property
     * <p/>
     * @since 1.6
     */
    public ResultSet getClientInfoProperties() throws SQLException {
        return databaseMetaData.getClientInfoProperties();
    }

    /**
     * Retrieves a description of the  system and user functions available
     * in the given catalog.
     * <p/>
     * Only system and user function descriptions matching the schema and
     * function name criteria are returned.  They are ordered by
     * <code>FUNCTION_CAT</code>, <code>FUNCTION_SCHEM</code>,
     * <code>FUNCTION_NAME</code> and
     * <code>SPECIFIC_ NAME</code>.
     * <p/>
     * <P>Each function description has the the following columns:
     * <OL>
     * <LI><B>FUNCTION_CAT</B> String => function catalog (may be <code>null</code>)
     * <LI><B>FUNCTION_SCHEM</B> String => function schema (may be <code>null</code>)
     * <LI><B>FUNCTION_NAME</B> String => function name.  This is the name
     * used to invoke the function
     * <LI><B>REMARKS</B> String => explanatory comment on the function
     * <LI><B>FUNCTION_TYPE</B> short => kind of function:
     * <UL>
     * <LI>functionResultUnknown - Cannot determine if a return value
     * or table will be returned
     * <LI> functionNoTable- Does not return a table
     * <LI> functionReturnsTable - Returns a table
     * </UL>
     * <LI><B>SPECIFIC_NAME</B> String  => the name which uniquely identifies
     * this function within its schema.  This is a user specified, or DBMS
     * generated, name that may be different then the <code>FUNCTION_NAME</code>
     * for example with overload functions
     * </OL>
     * <p/>
     * A user may not have permission to execute any of the functions that are
     * returned by <code>getFunctions</code>
     *
     * @param catalog             a catalog name; must match the catalog name as it
     *                            is stored in the database; "" retrieves those without a catalog;
     *                            <code>null</code> means that the catalog name should not be used to narrow
     *                            the search
     * @param schemaPattern       a schema name pattern; must match the schema name
     *                            as it is stored in the database; "" retrieves those without a schema;
     *                            <code>null</code> means that the schema name should not be used to narrow
     *                            the search
     * @param functionNamePattern a function name pattern; must match the
     *                            function name as it is stored in the database
     * @return <code>ResultSet</code> - each row is a function description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.6
     */
    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        return databaseMetaData.getFunctions(catalog, schemaPattern, functionNamePattern);
    }

    /**
     * Retrieves a description of the given catalog's system or user
     * function parameters and return type.
     * <p/>
     * <P>Only descriptions matching the schema,  function and
     * parameter name criteria are returned. They are ordered by
     * <code>FUNCTION_CAT</code>, <code>FUNCTION_SCHEM</code>,
     * <code>FUNCTION_NAME</code> and
     * <code>SPECIFIC_ NAME</code>. Within this, the return value,
     * if any, is first. Next are the parameter descriptions in call
     * order. The column descriptions follow in column number order.
     * <p/>
     * <P>Each row in the <code>ResultSet</code>
     * is a parameter description, column description or
     * return type description with the following fields:
     * <OL>
     * <LI><B>FUNCTION_CAT</B> String => function catalog (may be <code>null</code>)
     * <LI><B>FUNCTION_SCHEM</B> String => function schema (may be <code>null</code>)
     * <LI><B>FUNCTION_NAME</B> String => function name.  This is the name
     * used to invoke the function
     * <LI><B>COLUMN_NAME</B> String => column/parameter name
     * <LI><B>COLUMN_TYPE</B> Short => kind of column/parameter:
     * <UL>
     * <LI> functionColumnUnknown - nobody knows
     * <LI> functionColumnIn - IN parameter
     * <LI> functionColumnInOut - INOUT parameter
     * <LI> functionColumnOut - OUT parameter
     * <LI> functionColumnReturn - function return value
     * <LI> functionColumnResult - Indicates that the parameter or column
     * is a column in the <code>ResultSet</code>
     * </UL>
     * <LI><B>DATA_TYPE</B> int => SQL type from java.sql.Types
     * <LI><B>TYPE_NAME</B> String => SQL type name, for a UDT type the
     * type name is fully qualified
     * <LI><B>PRECISION</B> int => precision
     * <LI><B>LENGTH</B> int => length in bytes of data
     * <LI><B>SCALE</B> short => scale -  null is returned for data types where
     * SCALE is not applicable.
     * <LI><B>RADIX</B> short => radix
     * <LI><B>NULLABLE</B> short => can it contain NULL.
     * <UL>
     * <LI> functionNoNulls - does not allow NULL values
     * <LI> functionNullable - allows NULL values
     * <LI> functionNullableUnknown - nullability unknown
     * </UL>
     * <LI><B>REMARKS</B> String => comment describing column/parameter
     * <LI><B>CHAR_OCTET_LENGTH</B> int  => the maximum length of binary
     * and character based parameters or columns.  For any other datatype the returned value
     * is a NULL
     * <LI><B>ORDINAL_POSITION</B> int  => the ordinal position, starting
     * from 1, for the input and output parameters. A value of 0
     * is returned if this row describes the function's return value.
     * For result set columns, it is the
     * ordinal position of the column in the result set starting from 1.
     * <LI><B>IS_NULLABLE</B> String  => ISO rules are used to determine
     * the nullability for a parameter or column.
     * <UL>
     * <LI> YES           --- if the parameter or column can include NULLs
     * <LI> NO            --- if the parameter or column  cannot include NULLs
     * <LI> empty string  --- if the nullability for the
     * parameter  or column is unknown
     * </UL>
     * <LI><B>SPECIFIC_NAME</B> String  => the name which uniquely identifies
     * this function within its schema.  This is a user specified, or DBMS
     * generated, name that may be different then the <code>FUNCTION_NAME</code>
     * for example with overload functions
     * </OL>
     * <p/>
     * <p>The PRECISION column represents the specified column size for the given
     * parameter or column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog             a catalog name; must match the catalog name as it
     *                            is stored in the database; "" retrieves those without a catalog;
     *                            <code>null</code> means that the catalog name should not be used to narrow
     *                            the search
     * @param schemaPattern       a schema name pattern; must match the schema name
     *                            as it is stored in the database; "" retrieves those without a schema;
     *                            <code>null</code> means that the schema name should not be used to narrow
     *                            the search
     * @param functionNamePattern a procedure name pattern; must match the
     *                            function name as it is stored in the database
     * @param columnNamePattern   a parameter name pattern; must match the
     *                            parameter or column name as it is stored in the database
     * @return <code>ResultSet</code> - each row describes a
     *         user function parameter, column  or return type
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.6
     */
    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        return databaseMetaData.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern);
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
            result = databaseMetaData.unwrap(iface);
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
            result = databaseMetaData.isWrapperFor(iface);
        }
        return result;
    }

    public ResultSet getPseudoColumns(String catalog, String schemaPattern,
            String tableNamePattern, String columnNamePattern) throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            Class<?>[] valueTypes = 
                    new Class<?>[]{String.class, String.class, String.class, String.class};
            try {
                return (ResultSet) getMethodExecutor().invokeMethod(databaseMetaData,
                        "getPseudoColumns", valueTypes, catalog, schemaPattern,
                        tableNamePattern, columnNamePattern);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_dmd_wrapper", ex);
                throw new SQLException(ex);
            }
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }

    public boolean generatedKeyAlwaysReturned() throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            try {
                return (Boolean) getMethodExecutor().invokeMethod(databaseMetaData,
                        "generatedKeyAlwaysReturned", null);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_dmd_wrapper", ex);
                throw new SQLException(ex);
            }
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }
}
