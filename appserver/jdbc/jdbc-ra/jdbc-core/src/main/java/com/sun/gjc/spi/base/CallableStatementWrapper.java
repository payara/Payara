/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;


/**
 * Abstract class for wrapping PreparedStatement<br>
 */
public abstract class CallableStatementWrapper extends PreparedStatementWrapper implements CallableStatement {
    protected CallableStatement callableStatement = null;

    /**
     * Creates a new instance of CallableStatementWrapper<br>
     *
     * @param con       ConnectionWrapper <br>
     * @param statement Statement that is to be wrapped<br>
     */
    public CallableStatementWrapper(Connection con, CallableStatement statement,
                                    boolean cachingEnabled) throws SQLException{
        super(con, statement, cachingEnabled);
        callableStatement = statement;
    }

    /**
     * Registers the OUT parameter in ordinal position
     * <code>parameterIndex</code> to the JDBC type
     * <code>sqlType</code>.  All OUT parameters must be registered
     * before a stored procedure is executed.
     * <p/>
     * The JDBC type specified by <code>sqlType</code> for an OUT
     * parameter determines the Java type that must be used
     * in the <code>get</code> method to read the value of that parameter.
     * <p/>
     * If the JDBC type expected to be returned to this output parameter
     * is specific to this particular database, <code>sqlType</code>
     * should be <code>java.sql.Types.OTHER</code>.  The method
     * {@link #getObject} retrieves the value.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @param sqlType        the JDBC type code defined by <code>java.sql.Types</code>.
     *                       If the parameter is of JDBC type <code>NUMERIC</code>
     *                       or <code>DECIMAL</code>, the version of
     *                       <code>registerOutParameter</code> that accepts a scale value
     *                       should be used.
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     */
    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        callableStatement.registerOutParameter(parameterIndex, sqlType);
    }

    /**
     * Registers the parameter in ordinal position
     * <code>parameterIndex</code> to be of JDBC type
     * <code>sqlType</code>.  This method must be called
     * before a stored procedure is executed.
     * <p/>
     * The JDBC type specified by <code>sqlType</code> for an OUT
     * parameter determines the Java type that must be used
     * in the <code>get</code> method to read the value of that parameter.
     * <p/>
     * This version of <code>registerOutParameter</code> should be
     * used when the parameter is of JDBC type <code>NUMERIC</code>
     * or <code>DECIMAL</code>.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @param sqlType        the SQL type code defined by <code>java.sql.Types</code>.
     * @param scale          the desired number of digits to the right of the
     *                       decimal point.  It must be greater than or equal to zero.
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     */
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        callableStatement.registerOutParameter(parameterIndex, sqlType, scale);
    }

    /**
     * Retrieves whether the last OUT parameter read had the value of
     * SQL <code>NULL</code>.  Note that this method should be called only after
     * calling a getter method; otherwise, there is no value to use in
     * determining whether it is <code>null</code> or not.
     *
     * @return <code>true</code> if the last parameter read was SQL
     *         <code>NULL</code>; <code>false</code> otherwise
     * @throws java.sql.SQLException if a database access error occurs
     */
    public boolean wasNull() throws SQLException {
        return callableStatement.wasNull();
    }

    /**
     * Retrieves the value of the designated JDBC <code>CHAR</code>,
     * <code>VARCHAR</code>, or <code>LONGVARCHAR</code> parameter as a
     * <code>String</code> in the Java programming language.
     * <p/>
     * For the fixed-length type JDBC <code>CHAR</code>,
     * the <code>String</code> object
     * returned has exactly the same value the JDBC
     * <code>CHAR</code> value had in the
     * database, including any padding added by the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value. If the value is SQL <code>NULL</code>,
     *         the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setString
     */
    public String getString(int parameterIndex) throws SQLException {
        return callableStatement.getString(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>BIT</code> parameter as a
     * <code>boolean</code> in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>,
     *         the result is <code>false</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setBoolean
     */
    public boolean getBoolean(int parameterIndex) throws SQLException {
        return callableStatement.getBoolean(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>TINYINT</code> parameter
     * as a <code>byte</code> in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setByte
     */
    public byte getByte(int parameterIndex) throws SQLException {
        return callableStatement.getByte(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>SMALLINT</code> parameter
     * as a <code>short</code> in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setShort
     */
    public short getShort(int parameterIndex) throws SQLException {
        return callableStatement.getShort(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>INTEGER</code> parameter
     * as an <code>int</code> in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setInt
     */
    public int getInt(int parameterIndex) throws SQLException {
        return callableStatement.getInt(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>BIGINT</code> parameter
     * as a <code>long</code> in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setLong
     */
    public long getLong(int parameterIndex) throws SQLException {
        return callableStatement.getLong(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>FLOAT</code> parameter
     * as a <code>float</code> in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setFloat
     */
    public float getFloat(int parameterIndex) throws SQLException {
        return callableStatement.getFloat(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>DOUBLE</code> parameter as a <code>double</code>
     * in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setDouble
     */
    public double getDouble(int parameterIndex) throws SQLException {
        return callableStatement.getDouble(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>NUMERIC</code> parameter as a
     * <code>java.math.BigDecimal</code> object with <i>scale</i> digits to
     * the right of the decimal point.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @param scale          the number of digits to the right of the decimal point
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setBigDecimal
     * @deprecated use <code>getBigDecimal(int parameterIndex)</code>
     *             or <code>getBigDecimal(String parameterName)</code>
     */
    @Deprecated
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return callableStatement.getBigDecimal(parameterIndex, scale);
    }

    /**
     * Retrieves the value of the designated JDBC <code>BINARY</code> or
     * <code>VARBINARY</code> parameter as an array of <code>byte</code>
     * values in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setBytes
     */
    public byte[] getBytes(int parameterIndex) throws SQLException {
        return callableStatement.getBytes(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>DATE</code> parameter as a
     * <code>java.sql.Date</code> object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setDate
     */
    public Date getDate(int parameterIndex) throws SQLException {
        return callableStatement.getDate(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>TIME</code> parameter as a
     * <code>java.sql.Time</code> object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setTime
     */
    public Time getTime(int parameterIndex) throws SQLException {
        return callableStatement.getTime(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>TIMESTAMP</code> parameter as a
     * <code>java.sql.Timestamp</code> object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setTimestamp
     */
    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return callableStatement.getTimestamp(parameterIndex);
    }

    /**
     * Retrieves the value of the designated parameter as an <code>Object</code>
     * in the Java programming language. If the value is an SQL <code>NULL</code>,
     * the driver returns a Java <code>null</code>.
     * <p/>
     * This method returns a Java object whose type corresponds to the JDBC
     * type that was registered for this parameter using the method
     * <code>registerOutParameter</code>.  By registering the target JDBC
     * type as <code>java.sql.Types.OTHER</code>, this method can be used
     * to read database-specific abstract data types.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return A <code>java.lang.Object</code> holding the OUT parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     * @see #setObject
     */
    public Object getObject(int parameterIndex) throws SQLException {
        return callableStatement.getObject(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>NUMERIC</code> parameter as a
     * <code>java.math.BigDecimal</code> object with as many digits to the
     * right of the decimal point as the value contains.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @return the parameter value in full precision.  If the value is
     *         SQL <code>NULL</code>, the result is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setBigDecimal
     * @since 1.2
     */
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return callableStatement.getBigDecimal(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>REF(&lt;structured-type&gt;)</code>
     * parameter as a {@link java.sql.Ref} object in the Java programming language.
     *
     * @param i the first parameter is 1, the second is 2,
     *          and so on
     * @return the parameter value as a <code>Ref</code> object in the
     *         Java programming language.  If the value was SQL <code>NULL</code>, the value
     *         <code>null</code> is returned.
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public Ref getRef(int i) throws SQLException {
        return callableStatement.getRef(i);
    }

    /**
     * Retrieves the value of the designated JDBC <code>BLOB</code> parameter as a
     * {@link java.sql.Blob} object in the Java programming language.
     *
     * @param i the first parameter is 1, the second is 2, and so on
     * @return the parameter value as a <code>Blob</code> object in the
     *         Java programming language.  If the value was SQL <code>NULL</code>, the value
     *         <code>null</code> is returned.
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public Blob getBlob(int i) throws SQLException {
        return callableStatement.getBlob(i);
    }

    /**
     * Retrieves the value of the designated JDBC <code>CLOB</code> parameter as a
     * <code>Clob</code> object in the Java programming language.
     *
     * @param i the first parameter is 1, the second is 2, and
     *          so on
     * @return the parameter value as a <code>Clob</code> object in the
     *         Java programming language.  If the value was SQL <code>NULL</code>, the
     *         value <code>null</code> is returned.
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public Clob getClob(int i) throws SQLException {
        return callableStatement.getClob(i);
    }

    /**
     * Retrieves the value of the designated JDBC <code>ARRAY</code> parameter as an
     * {@link java.sql.Array} object in the Java programming language.
     *
     * @param i the first parameter is 1, the second is 2, and
     *          so on
     * @return the parameter value as an <code>Array</code> object in
     *         the Java programming language.  If the value was SQL <code>NULL</code>, the
     *         value <code>null</code> is returned.
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public Array getArray(int i) throws SQLException {
        return callableStatement.getArray(i);
    }

    /**
     * Retrieves the value of the designated JDBC <code>DATE</code> parameter as a
     * <code>java.sql.Date</code> object, using
     * the given <code>Calendar</code> object
     * to construct the date.
     * With a <code>Calendar</code> object, the driver
     * can calculate the date taking into account a custom timezone and locale.
     * If no <code>Calendar</code> object is specified, the driver uses the
     * default timezone and locale.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @param cal            the <code>Calendar</code> object the driver will use
     *                       to construct the date
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setDate
     * @since 1.2
     */
    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return callableStatement.getDate(parameterIndex, cal);
    }

    /**
     * Retrieves the value of the designated JDBC <code>TIME</code> parameter as a
     * <code>java.sql.Time</code> object, using
     * the given <code>Calendar</code> object
     * to construct the time.
     * With a <code>Calendar</code> object, the driver
     * can calculate the time taking into account a custom timezone and locale.
     * If no <code>Calendar</code> object is specified, the driver uses the
     * default timezone and locale.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @param cal            the <code>Calendar</code> object the driver will use
     *                       to construct the time
     * @return the parameter value; if the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setTime
     * @since 1.2
     */
    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return callableStatement.getTime(parameterIndex, cal);
    }

    /**
     * Retrieves the value of the designated JDBC <code>TIMESTAMP</code> parameter as a
     * <code>java.sql.Timestamp</code> object, using
     * the given <code>Calendar</code> object to construct
     * the <code>Timestamp</code> object.
     * With a <code>Calendar</code> object, the driver
     * can calculate the timestamp taking into account a custom timezone and locale.
     * If no <code>Calendar</code> object is specified, the driver uses the
     * default timezone and locale.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,
     *                       and so on
     * @param cal            the <code>Calendar</code> object the driver will use
     *                       to construct the timestamp
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setTimestamp
     * @since 1.2
     */
    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return callableStatement.getTimestamp(parameterIndex, cal);
    }

    /**
     * Registers the designated output parameter.  This version of
     * the method <code>registerOutParameter</code>
     * should be used for a user-defined or <code>REF</code> output parameter.  Examples
     * of user-defined types include: <code>STRUCT</code>, <code>DISTINCT</code>,
     * <code>JAVA_OBJECT</code>, and named array types.
     * <p/>
     * Before executing a stored procedure call, you must explicitly
     * call <code>registerOutParameter</code> to register the type from
     * <code>java.sql.Types</code> for each
     * OUT parameter.  For a user-defined parameter, the fully-qualified SQL
     * type name of the parameter should also be given, while a <code>REF</code>
     * parameter requires that the fully-qualified type name of the
     * referenced type be given.  A JDBC driver that does not need the
     * type code and type name information may ignore it.   To be portable,
     * however, applications should always provide these values for
     * user-defined and <code>REF</code> parameters.
     * <p/>
     * Although it is intended for user-defined and <code>REF</code> parameters,
     * this method may be used to register a parameter of any JDBC type.
     * If the parameter does not have a user-defined or <code>REF</code> type, the
     * <i>typeName</i> parameter is ignored.
     * <p/>
     * <P><B>Note:</B> When reading the value of an out parameter, you
     * must use the getter method whose Java type corresponds to the
     * parameter's registered SQL type.
     *
     * @param paramIndex the first parameter is 1, the second is 2,...
     * @param sqlType    a value from {@link java.sql.Types}
     * @param typeName   the fully-qualified name of an SQL structured type
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     * @since 1.2
     */
    public void registerOutParameter(int paramIndex, int sqlType, String typeName) throws SQLException {
        callableStatement.registerOutParameter(paramIndex, sqlType, typeName);
    }

    /**
     * Registers the OUT parameter named
     * <code>parameterName</code> to the JDBC type
     * <code>sqlType</code>.  All OUT parameters must be registered
     * before a stored procedure is executed.
     * <p/>
     * The JDBC type specified by <code>sqlType</code> for an OUT
     * parameter determines the Java type that must be used
     * in the <code>get</code> method to read the value of that parameter.
     * <p/>
     * If the JDBC type expected to be returned to this output parameter
     * is specific to this particular database, <code>sqlType</code>
     * should be <code>java.sql.Types.OTHER</code>.  The method
     * {@link #getObject} retrieves the value.
     *
     * @param parameterName the name of the parameter
     * @param sqlType       the JDBC type code defined by <code>java.sql.Types</code>.
     *                      If the parameter is of JDBC type <code>NUMERIC</code>
     *                      or <code>DECIMAL</code>, the version of
     *                      <code>registerOutParameter</code> that accepts a scale value
     *                      should be used.
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     * @since 1.4
     */
    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        callableStatement.registerOutParameter(parameterName, sqlType);
    }

    /**
     * Registers the parameter named
     * <code>parameterName</code> to be of JDBC type
     * <code>sqlType</code>.  This method must be called
     * before a stored procedure is executed.
     * <p/>
     * The JDBC type specified by <code>sqlType</code> for an OUT
     * parameter determines the Java type that must be used
     * in the <code>get</code> method to read the value of that parameter.
     * <p/>
     * This version of <code>registerOutParameter</code> should be
     * used when the parameter is of JDBC type <code>NUMERIC</code>
     * or <code>DECIMAL</code>.
     *
     * @param parameterName the name of the parameter
     * @param sqlType       SQL type code defined by <code>java.sql.Types</code>.
     * @param scale         the desired number of digits to the right of the
     *                      decimal point.  It must be greater than or equal to zero.
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     * @since 1.4
     */
    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        callableStatement.registerOutParameter(parameterName, sqlType, scale);
    }

    /**
     * Registers the designated output parameter.  This version of
     * the method <code>registerOutParameter</code>
     * should be used for a user-named or REF output parameter.  Examples
     * of user-named types include: STRUCT, DISTINCT, JAVA_OBJECT, and
     * named array types.
     * <p/>
     * Before executing a stored procedure call, you must explicitly
     * call <code>registerOutParameter</code> to register the type from
     * <code>java.sql.Types</code> for each
     * OUT parameter.  For a user-named parameter the fully-qualified SQL
     * type name of the parameter should also be given, while a REF
     * parameter requires that the fully-qualified type name of the
     * referenced type be given.  A JDBC driver that does not need the
     * type code and type name information may ignore it.   To be portable,
     * however, applications should always provide these values for
     * user-named and REF parameters.
     * <p/>
     * Although it is intended for user-named and REF parameters,
     * this method may be used to register a parameter of any JDBC type.
     * If the parameter does not have a user-named or REF type, the
     * typeName parameter is ignored.
     * <p/>
     * <P><B>Note:</B> When reading the value of an out parameter, you
     * must use the <code>getXXX</code> method whose Java type XXX corresponds to the
     * parameter's registered SQL type.
     *
     * @param parameterName the name of the parameter
     * @param sqlType       a value from {@link java.sql.Types}
     * @param typeName      the fully-qualified name of an SQL structured type
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     * @since 1.4
     */
    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        callableStatement.registerOutParameter(parameterName, sqlType, typeName);
    }

    /**
     * Retrieves the value of the designated JDBC <code>DATALINK</code> parameter as a
     * <code>java.net.URL</code> object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,...
     * @return a <code>java.net.URL</code> object that represents the
     *         JDBC <code>DATALINK</code> value used as the designated
     *         parameter
     * @throws java.sql.SQLException if a database access error occurs,
     *                               or if the URL being returned is
     *                               not a valid URL on the Java platform
     * @see #setURL
     * @since 1.4
     */
    public URL getURL(int parameterIndex) throws SQLException {
        return callableStatement.getURL(parameterIndex);
    }

    /**
     * Sets the designated parameter to the given <code>java.net.URL</code> object.
     * The driver converts this to an SQL <code>DATALINK</code> value when
     * it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param val           the parameter value
     * @throws java.sql.SQLException if a database access error occurs,
     *                               or if a URL is malformed
     * @see #getURL
     * @since 1.4
     */
    public void setURL(String parameterName, URL val) throws SQLException {
        callableStatement.setURL(parameterName, val);
    }

    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     * <p/>
     * <P><B>Note:</B> You must specify the parameter's SQL type.
     *
     * @param parameterName the name of the parameter
     * @param sqlType       the SQL type code defined in <code>java.sql.Types</code>
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public void setNull(String parameterName, int sqlType) throws SQLException {
        callableStatement.setNull(parameterName, sqlType);
    }

    /**
     * Sets the designated parameter to the given Java <code>boolean</code> value.
     * The driver converts this
     * to an SQL <code>BIT</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getBoolean
     * @since 1.4
     */
    public void setBoolean(String parameterName, boolean x) throws SQLException {
        callableStatement.setBoolean(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>byte</code> value.
     * The driver converts this
     * to an SQL <code>TINYINT</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getByte
     * @since 1.4
     */
    public void setByte(String parameterName, byte x) throws SQLException {
        callableStatement.setByte(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>short</code> value.
     * The driver converts this
     * to an SQL <code>SMALLINT</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getShort
     * @since 1.4
     */
    public void setShort(String parameterName, short x) throws SQLException {
        callableStatement.setShort(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>int</code> value.
     * The driver converts this
     * to an SQL <code>INTEGER</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getInt
     * @since 1.4
     */
    public void setInt(String parameterName, int x) throws SQLException {
        callableStatement.setInt(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>long</code> value.
     * The driver converts this
     * to an SQL <code>BIGINT</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getLong
     * @since 1.4
     */
    public void setLong(String parameterName, long x) throws SQLException {
        callableStatement.setLong(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>float</code> value.
     * The driver converts this
     * to an SQL <code>FLOAT</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getFloat
     * @since 1.4
     */
    public void setFloat(String parameterName, float x) throws SQLException {
        callableStatement.setFloat(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>double</code> value.
     * The driver converts this
     * to an SQL <code>DOUBLE</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getDouble
     * @since 1.4
     */
    public void setDouble(String parameterName, double x) throws SQLException {
        callableStatement.setDouble(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given
     * <code>java.math.BigDecimal</code> value.
     * The driver converts this to an SQL <code>NUMERIC</code> value when
     * it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getBigDecimal
     * @since 1.4
     */
    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        callableStatement.setBigDecimal(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>String</code> value.
     * The driver converts this
     * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
     * (depending on the argument's
     * size relative to the driver's limits on <code>VARCHAR</code> values)
     * when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getString
     * @since 1.4
     */
    public void setString(String parameterName, String x) throws SQLException {
        callableStatement.setString(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given Java array of bytes.
     * The driver converts this to an SQL <code>VARBINARY</code> or
     * <code>LONGVARBINARY</code> (depending on the argument's size relative
     * to the driver's limits on <code>VARBINARY</code> values) when it sends
     * it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getBytes
     * @since 1.4
     */
    public void setBytes(String parameterName, byte x[]) throws SQLException {
        callableStatement.setBytes(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value.
     * The driver converts this
     * to an SQL <code>DATE</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getDate
     * @since 1.4
     */
    public void setDate(String parameterName, Date x) throws SQLException {
        callableStatement.setDate(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value.
     * The driver converts this
     * to an SQL <code>TIME</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getTime
     * @since 1.4
     */
    public void setTime(String parameterName, Time x) throws SQLException {
        callableStatement.setTime(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value.
     * The driver
     * converts this to an SQL <code>TIMESTAMP</code> value when it sends it to the
     * database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getTimestamp
     * @since 1.4
     */
    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        callableStatement.setTimestamp(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterName the name of the parameter
     * @param x             the Java input stream that contains the ASCII parameter value
     * @param length        the number of bytes in the stream
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
        callableStatement.setAsciiStream(parameterName, x, length);
    }

    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterName the name of the parameter
     * @param x             the java input stream which contains the binary parameter value
     * @param length        the number of bytes in the stream
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
        callableStatement.setBinaryStream(parameterName, x, length);
    }

    /**
     * Sets the value of the designated parameter with the given object. The second
     * argument must be an object type; for integral values, the
     * <code>java.lang</code> equivalent objects should be used.
     * <p/>
     * <p>The given Java object will be converted to the given targetSqlType
     * before being sent to the database.
     * <p/>
     * If the object has a custom mapping (is of a class implementing the
     * interface <code>SQLData</code>),
     * the JDBC driver should call the method <code>SQLData.writeSQL</code> to write it
     * to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>, <code>Struct</code>,
     * or <code>Array</code>, the driver should pass it to the database as a
     * value of the corresponding SQL type.
     * <p/>
     * Note that this method may be used to pass datatabase-
     * specific abstract data types.
     *
     * @param parameterName the name of the parameter
     * @param x             the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
     *                      sent to the database. The scale argument may further qualify this type.
     * @param scale         for java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types,
     *                      this is the number of digits after the decimal point.  For all other
     *                      types, this value will be ignored.
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     * @see #getObject
     * @since 1.4
     */
    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
        callableStatement.setObject(parameterName, x, targetSqlType, scale);
    }

    /**
     * Sets the value of the designated parameter with the given object.
     * This method is like the method <code>setObject</code>
     * above, except that it assumes a scale of zero.
     *
     * @param parameterName the name of the parameter
     * @param x             the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
     *                      sent to the database
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getObject
     * @since 1.4
     */
    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        callableStatement.setObject(parameterName, x, targetSqlType);
    }

    /**
     * Sets the value of the designated parameter with the given object.
     * The second parameter must be of type <code>Object</code>; therefore, the
     * <code>java.lang</code> equivalent objects should be used for built-in types.
     * <p/>
     * <p>The JDBC specification specifies a standard mapping from
     * Java <code>Object</code> types to SQL types.  The given argument
     * will be converted to the corresponding SQL type before being
     * sent to the database.
     * <p/>
     * <p>Note that this method may be used to pass datatabase-
     * specific abstract data types, by using a driver-specific Java
     * type.
     * <p/>
     * If the object is of a class implementing the interface <code>SQLData</code>,
     * the JDBC driver should call the method <code>SQLData.writeSQL</code>
     * to write it to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>, <code>Struct</code>,
     * or <code>Array</code>, the driver should pass it to the database as a
     * value of the corresponding SQL type.
     * <p/>
     * This method throws an exception if there is an ambiguity, for example, if the
     * object is of a class implementing more than one of the interfaces named above.
     *
     * @param parameterName the name of the parameter
     * @param x             the object containing the input parameter value
     * @throws java.sql.SQLException if a database access error occurs or if the given
     *                               <code>Object</code> parameter is ambiguous
     * @see #getObject
     * @since 1.4
     */
    public void setObject(String parameterName, Object x) throws SQLException {
        callableStatement.setObject(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterName the name of the parameter
     * @param reader        the <code>java.io.Reader</code> object that
     *                      contains the UNICODE data used as the designated parameter
     * @param length        the number of characters in the stream
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
        callableStatement.setCharacterStream(parameterName, reader, length);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>DATE</code> value,
     * which the driver then sends to the database.  With a
     * a <code>Calendar</code> object, the driver can calculate the date
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @param cal           the <code>Calendar</code> object the driver will use
     *                      to construct the date
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getDate
     * @since 1.4
     */
    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        callableStatement.setDate(parameterName, x, cal);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIME</code> value,
     * which the driver then sends to the database.  With a
     * a <code>Calendar</code> object, the driver can calculate the time
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @param cal           the <code>Calendar</code> object the driver will use
     *                      to construct the time
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getTime
     * @since 1.4
     */
    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        callableStatement.setTime(parameterName, x, cal);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIMESTAMP</code> value,
     * which the driver then sends to the database.  With a
     * a <code>Calendar</code> object, the driver can calculate the timestamp
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @param cal           the <code>Calendar</code> object the driver will use
     *                      to construct the timestamp
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getTimestamp
     * @since 1.4
     */
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        callableStatement.setTimestamp(parameterName, x, cal);
    }

    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     * This version of the method <code>setNull</code> should
     * be used for user-defined types and REF type parameters.  Examples
     * of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT, and
     * named array types.
     * <p/>
     * <P><B>Note:</B> To be portable, applications must give the
     * SQL type code and the fully-qualified SQL type name when specifying
     * a NULL user-defined or REF parameter.  In the case of a user-defined type
     * the name is the type name of the parameter itself.  For a REF
     * parameter, the name is the type name of the referenced type.  If
     * a JDBC driver does not need the type code or type name information,
     * it may ignore it.
     * <p/>
     * Although it is intended for user-defined and Ref parameters,
     * this method may be used to set a null parameter of any JDBC type.
     * If the parameter does not have a user-defined or REF type, the given
     * typeName is ignored.
     *
     * @param parameterName the name of the parameter
     * @param sqlType       a value from <code>java.sql.Types</code>
     * @param typeName      the fully-qualified name of an SQL user-defined type;
     *                      ignored if the parameter is not a user-defined type or
     *                      SQL <code>REF</code> value
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        callableStatement.setNull(parameterName, sqlType, typeName);
    }

    /**
     * Retrieves the value of a JDBC <code>CHAR</code>, <code>VARCHAR</code>,
     * or <code>LONGVARCHAR</code> parameter as a <code>String</code> in
     * the Java programming language.
     * <p/>
     * For the fixed-length type JDBC <code>CHAR</code>,
     * the <code>String</code> object
     * returned has exactly the same value the JDBC
     * <code>CHAR</code> value had in the
     * database, including any padding added by the database.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value. If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setString
     * @since 1.4
     */
    public String getString(String parameterName) throws SQLException {
        return callableStatement.getString(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>BIT</code> parameter as a
     * <code>boolean</code> in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>false</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setBoolean
     * @since 1.4
     */
    public boolean getBoolean(String parameterName) throws SQLException {
        return callableStatement.getBoolean(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>TINYINT</code> parameter as a <code>byte</code>
     * in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setByte
     * @since 1.4
     */
    public byte getByte(String parameterName) throws SQLException {
        return callableStatement.getByte(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>SMALLINT</code> parameter as a <code>short</code>
     * in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setShort
     * @since 1.4
     */
    public short getShort(String parameterName) throws SQLException {
        return callableStatement.getShort(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>INTEGER</code> parameter as an <code>int</code>
     * in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>,
     *         the result is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setInt
     * @since 1.4
     */
    public int getInt(String parameterName) throws SQLException {
        return callableStatement.getInt(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>BIGINT</code> parameter as a <code>long</code>
     * in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>,
     *         the result is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setLong
     * @since 1.4
     */
    public long getLong(String parameterName) throws SQLException {
        return callableStatement.getLong(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>FLOAT</code> parameter as a <code>float</code>
     * in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>,
     *         the result is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setFloat
     * @since 1.4
     */
    public float getFloat(String parameterName) throws SQLException {
        return callableStatement.getFloat(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>DOUBLE</code> parameter as a <code>double</code>
     * in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>,
     *         the result is <code>0</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setDouble
     * @since 1.4
     */
    public double getDouble(String parameterName) throws SQLException {
        return callableStatement.getDouble(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>BINARY</code> or <code>VARBINARY</code>
     * parameter as an array of <code>byte</code> values in the Java
     * programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result is
     *         <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setBytes
     * @since 1.4
     */
    public byte[] getBytes(String parameterName) throws SQLException {
        return callableStatement.getBytes(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>DATE</code> parameter as a
     * <code>java.sql.Date</code> object.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setDate
     * @since 1.4
     */
    public Date getDate(String parameterName) throws SQLException {
        return callableStatement.getDate(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>TIME</code> parameter as a
     * <code>java.sql.Time</code> object.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setTime
     * @since 1.4
     */
    public Time getTime(String parameterName) throws SQLException {
        return callableStatement.getTime(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>TIMESTAMP</code> parameter as a
     * <code>java.sql.Timestamp</code> object.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
     *         is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setTimestamp
     * @since 1.4
     */
    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return callableStatement.getTimestamp(parameterName);
    }

    /**
     * Retrieves the value of a parameter as an <code>Object</code> in the Java
     * programming language. If the value is an SQL <code>NULL</code>, the
     * driver returns a Java <code>null</code>.
     * <p/>
     * This method returns a Java object whose type corresponds to the JDBC
     * type that was registered for this parameter using the method
     * <code>registerOutParameter</code>.  By registering the target JDBC
     * type as <code>java.sql.Types.OTHER</code>, this method can be used
     * to read database-specific abstract data types.
     *
     * @param parameterName the name of the parameter
     * @return A <code>java.lang.Object</code> holding the OUT parameter value.
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     * @see #setObject
     * @since 1.4
     */
    public Object getObject(String parameterName) throws SQLException {
        return callableStatement.getObject(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>NUMERIC</code> parameter as a
     * <code>java.math.BigDecimal</code> object with as many digits to the
     * right of the decimal point as the value contains.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value in full precision.  If the value is
     *         SQL <code>NULL</code>, the result is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setBigDecimal
     * @since 1.4
     */
    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return callableStatement.getBigDecimal(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>REF(&lt;structured-type&gt;)</code>
     * parameter as a {@link java.sql.Ref} object in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value as a <code>Ref</code> object in the
     *         Java programming language.  If the value was SQL <code>NULL</code>,
     *         the value <code>null</code> is returned.
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public Ref getRef(String parameterName) throws SQLException {
        return callableStatement.getRef(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>BLOB</code> parameter as a
     * {@link java.sql.Blob} object in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value as a <code>Blob</code> object in the
     *         Java programming language.  If the value was SQL <code>NULL</code>,
     *         the value <code>null</code> is returned.
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public Blob getBlob(String parameterName) throws SQLException {
        return callableStatement.getBlob(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>CLOB</code> parameter as a
     * <code>Clob</code> object in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value as a <code>Clob</code> object in the
     *         Java programming language.  If the value was SQL <code>NULL</code>,
     *         the value <code>null</code> is returned.
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public Clob getClob(String parameterName) throws SQLException {
        return callableStatement.getClob(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>ARRAY</code> parameter as an
     * {@link java.sql.Array} object in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value as an <code>Array</code> object in
     *         Java programming language.  If the value was SQL <code>NULL</code>,
     *         the value <code>null</code> is returned.
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public Array getArray(String parameterName) throws SQLException {
        return callableStatement.getArray(parameterName);
    }

    /**
     * Retrieves the value of a JDBC <code>DATE</code> parameter as a
     * <code>java.sql.Date</code> object, using
     * the given <code>Calendar</code> object
     * to construct the date.
     * With a <code>Calendar</code> object, the driver
     * can calculate the date taking into account a custom timezone and locale.
     * If no <code>Calendar</code> object is specified, the driver uses the
     * default timezone and locale.
     *
     * @param parameterName the name of the parameter
     * @param cal           the <code>Calendar</code> object the driver will use
     *                      to construct the date
     * @return the parameter value.  If the value is SQL <code>NULL</code>,
     *         the result is <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setDate
     * @since 1.4
     */
    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return callableStatement.getDate(parameterName, cal);
    }

    /**
     * Retrieves the value of a JDBC <code>TIME</code> parameter as a
     * <code>java.sql.Time</code> object, using
     * the given <code>Calendar</code> object
     * to construct the time.
     * With a <code>Calendar</code> object, the driver
     * can calculate the time taking into account a custom timezone and locale.
     * If no <code>Calendar</code> object is specified, the driver uses the
     * default timezone and locale.
     *
     * @param parameterName the name of the parameter
     * @param cal           the <code>Calendar</code> object the driver will use
     *                      to construct the time
     * @return the parameter value; if the value is SQL <code>NULL</code>, the result is
     *         <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setTime
     * @since 1.4
     */
    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return callableStatement.getTime(parameterName, cal);
    }

    /**
     * Retrieves the value of a JDBC <code>TIMESTAMP</code> parameter as a
     * <code>java.sql.Timestamp</code> object, using
     * the given <code>Calendar</code> object to construct
     * the <code>Timestamp</code> object.
     * With a <code>Calendar</code> object, the driver
     * can calculate the timestamp taking into account a custom timezone and locale.
     * If no <code>Calendar</code> object is specified, the driver uses the
     * default timezone and locale.
     *
     * @param parameterName the name of the parameter
     * @param cal           the <code>Calendar</code> object the driver will use
     *                      to construct the timestamp
     * @return the parameter value.  If the value is SQL <code>NULL</code>, the result is
     *         <code>null</code>.
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setTimestamp
     * @since 1.4
     */
    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return callableStatement.getTimestamp(parameterName, cal);
    }

    /**
     * Retrieves the value of a JDBC <code>DATALINK</code> parameter as a
     * <code>java.net.URL</code> object.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value as a <code>java.net.URL</code> object in the
     *         Java programming language.  If the value was SQL <code>NULL</code>, the
     *         value <code>null</code> is returned.
     * @throws java.sql.SQLException if a database access error occurs,
     *                               or if there is a problem with the URL
     * @see #setURL
     * @since 1.4
     */
    public URL getURL(String parameterName) throws SQLException {
        return callableStatement.getURL(parameterName);
    }

    /**
     * Returns an object representing the value of OUT parameter
     * <code>i</code> and uses <code>map</code> for the custom
     * mapping of the parameter value.
     * <p/>
     * This method returns a Java object whose type corresponds to the
     * JDBC type that was registered for this parameter using the method
     * <code>registerOutParameter</code>.  By registering the target
     * JDBC type as <code>java.sql.Types.OTHER</code>, this method can
     * be used to read database-specific abstract data types.
     *
     * @param i   the first parameter is 1, the second is 2, and so on
     * @param map the mapping from SQL type names to Java classes
     * @return a <code>java.lang.Object</code> holding the OUT parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setObject
     * @since 1.2
     */
    public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
        return callableStatement.getObject(i, map);
    }

    /**
     * Returns an object representing the value of OUT parameter
     * <code>i</code> and uses <code>map</code> for the custom
     * mapping of the parameter value.
     * <p/>
     * This method returns a Java object whose type corresponds to the
     * JDBC type that was registered for this parameter using the method
     * <code>registerOutParameter</code>.  By registering the target
     * JDBC type as <code>java.sql.Types.OTHER</code>, this method can
     * be used to read database-specific abstract data types.
     *
     * @param parameterName the name of the parameter
     * @param map           the mapping from SQL type names to Java classes
     * @return a <code>java.lang.Object</code> holding the OUT parameter value
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setObject
     * @since 1.4
     */
    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        return callableStatement.getObject(parameterName, map);
    }
}
