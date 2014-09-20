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

package com.sun.gjc.spi.base;

import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.gjc.util.ResultSetClosedEventListener;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.logging.Level;

/**
 * Abstract class for wrapping PreparedStatement<br>
 */
public abstract class PreparedStatementWrapper extends StatementWrapper implements
        PreparedStatement, ResultSetClosedEventListener {
    protected PreparedStatement preparedStatement = null;
    private boolean busy = false;
    private boolean cached = false;
    private int defaultMaxFieldSize;
    private int defaultMaxRows;
    private int defaultQueryTimeout;
    private int defaultFetchDirection;
    private int defaultFetchSize;
    private int currentMaxFieldSize;
    private int currentMaxRows;
    private int currentQueryTimeout;
    private int currentFetchDirection;
    private int currentFetchSize;
    private boolean valid = true;

    /**
     * Abstract class for wrapping PreparedStatement <br>
     *
     * @param con       Connection Wrapper <br>
     * @param statement PreparedStatement that is to be wrapped.<br>
     * @param cachingEnabled boolean that enabled/ disables caching <br>
     * @throws SQLException Exception thrown from underlying statement<br>
    */
    public PreparedStatementWrapper(Connection con,
                                    PreparedStatement statement, boolean cachingEnabled) throws SQLException {
        super(con, statement);
        preparedStatement = statement;
        cached = cachingEnabled;
        ConnectionHolder wrappedCon = (ConnectionHolder) con;
        leakDetector = wrappedCon.getManagedConnection().getLeakDetector();

        if (cached) {

            defaultQueryTimeout = preparedStatement.getQueryTimeout();
            defaultMaxFieldSize = preparedStatement.getMaxFieldSize();
            defaultFetchSize = preparedStatement.getFetchSize();
            defaultMaxRows = preparedStatement.getMaxRows();
            defaultFetchDirection = preparedStatement.getFetchDirection();

            currentQueryTimeout = defaultQueryTimeout;
            currentMaxFieldSize = defaultMaxFieldSize;
            currentFetchSize = defaultFetchSize;
            currentMaxRows = defaultMaxRows;
            currentFetchDirection = defaultFetchDirection;

        } else {
            //Start Statement leak detection
            if(leakDetector != null) {
                leakDetector.startStatementLeakTracing(preparedStatement, this);
            }
        }
    }

    /**
     * Executes the SQL statement in this <code>PreparedStatement</code> object,
     * which must be an SQL <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code> statement; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @return either (1) the row count for <code>INSERT</code>, <code>UPDATE</code>,
     *         or <code>DELETE</code> statements
     *         or (2) 0 for SQL statements that return nothing
     * @throws java.sql.SQLException if a database access error occurs or the SQL
     *                               statement returns a <code>ResultSet</code> object
     */
    public int executeUpdate() throws SQLException {
        return preparedStatement.executeUpdate();
    }

    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     * <p/>
     * <P><B>Note:</B> You must specify the parameter's SQL type.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param sqlType        the SQL type code defined in <code>java.sql.Types</code>
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        preparedStatement.setNull(parameterIndex, sqlType);
    }

    /**
     * Sets the designated parameter to the given Java <code>boolean</code> value.
     * The driver converts this
     * to an SQL <code>BIT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        preparedStatement.setBoolean(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>byte</code> value.
     * The driver converts this
     * to an SQL <code>TINYINT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setByte(int parameterIndex, byte x) throws SQLException {
        preparedStatement.setByte(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>short</code> value.
     * The driver converts this
     * to an SQL <code>SMALLINT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setShort(int parameterIndex, short x) throws SQLException {
        preparedStatement.setShort(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>int</code> value.
     * The driver converts this
     * to an SQL <code>INTEGER</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setInt(int parameterIndex, int x) throws SQLException {
        preparedStatement.setInt(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>long</code> value.
     * The driver converts this
     * to an SQL <code>BIGINT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setLong(int parameterIndex, long x) throws SQLException {
        preparedStatement.setLong(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>float</code> value.
     * The driver converts this
     * to an SQL <code>FLOAT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setFloat(int parameterIndex, float x) throws SQLException {
        preparedStatement.setFloat(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>double</code> value.
     * The driver converts this
     * to an SQL <code>DOUBLE</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setDouble(int parameterIndex, double x) throws SQLException {
        preparedStatement.setDouble(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given <code>java.math.BigDecimal</code> value.
     * The driver converts this to an SQL <code>NUMERIC</code> value when
     * it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        preparedStatement.setBigDecimal(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given Java <code>String</code> value.
     * The driver converts this
     * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
     * (depending on the argument's
     * size relative to the driver's limits on <code>VARCHAR</code> values)
     * when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setString(int parameterIndex, String x) throws SQLException {
        preparedStatement.setString(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given Java array of bytes.  The driver converts
     * this to an SQL <code>VARBINARY</code> or <code>LONGVARBINARY</code>
     * (depending on the argument's size relative to the driver's limits on
     * <code>VARBINARY</code> values) when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setBytes(int parameterIndex, byte x[]) throws SQLException {
        preparedStatement.setBytes(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value.
     * The driver converts this
     * to an SQL <code>DATE</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setDate(int parameterIndex, Date x) throws SQLException {
        preparedStatement.setDate(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value.
     * The driver converts this
     * to an SQL <code>TIME</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setTime(int parameterIndex, Time x) throws SQLException {
        preparedStatement.setTime(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value.
     * The driver
     * converts this to an SQL <code>TIMESTAMP</code> value when it sends it to the
     * database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, x);
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
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the Java input stream that contains the ASCII parameter value
     * @param length         the number of bytes in the stream
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    /**
     * Sets the designated parameter to the given input stream, which
     * will have the specified number of bytes. A Unicode character has
     * two bytes, with the first byte being the high byte, and the second
     * being the low byte.
     * <p/>
     * When a very large Unicode value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from Unicode to the database char format.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              a <code>java.io.InputStream</code> object that contains the
     *                       Unicode parameter value as two-byte Unicode characters
     * @param length         the number of bytes in the stream
     * @throws java.sql.SQLException if a database access error occurs
     * @deprecated
     */
    @Deprecated
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setUnicodeStream(parameterIndex, x, length);
    }

    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the java input stream which contains the binary parameter value
     * @param length         the number of bytes in the stream
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    /**
     * Clears the current parameter values immediately.
     * <P>In general, parameter values remain in force for repeated use of a
     * statement. Setting a parameter value automatically clears its
     * previous value.  However, in some cases it is useful to immediately
     * release the resources used by the current parameter values; this can
     * be done by calling the method <code>clearParameters</code>.
     *
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void clearParameters() throws SQLException {
        preparedStatement.clearParameters();
    }

    /**
     * <p>Sets the value of the designated parameter with the given object. The second
     * argument must be an object type; for integral values, the
     * <code>java.lang</code> equivalent objects should be used.
     * <p/>
     * <p>The given Java object will be converted to the given targetSqlType
     * before being sent to the database.
     * <p/>
     * If the object has a custom mapping (is of a class implementing the
     * interface <code>SQLData</code>),
     * the JDBC driver should call the method <code>SQLData.writeSQL</code> to
     * write it to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>, <code>Struct</code>,
     * or <code>Array</code>, the driver should pass it to the database as a
     * value of the corresponding SQL type.
     * <p/>
     * <p>Note that this method may be used to pass database-specific
     * abstract data types.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the object containing the input parameter value
     * @param targetSqlType  the SQL type (as defined in java.sql.Types) to be
     *                       sent to the database. The scale argument may further qualify this type.
     * @param scale          for java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types,
     *                       this is the number of digits after the decimal point.  For all other
     *                       types, this value will be ignored.
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Types
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
        preparedStatement.setObject(parameterIndex, x, targetSqlType, scale);
    }

    /**
     * Sets the value of the designated parameter with the given object.
     * This method is like the method <code>setObject</code>
     * above, except that it assumes a scale of zero.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the object containing the input parameter value
     * @param targetSqlType  the SQL type (as defined in java.sql.Types) to be
     *                       sent to the database
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        preparedStatement.setObject(parameterIndex, x, targetSqlType);
    }

    /**
     * <p>Sets the value of the designated parameter using the given object.
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
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the object containing the input parameter value
     * @throws java.sql.SQLException if a database access error occurs or the type
     *                               of the given object is ambiguous
     */
    public void setObject(int parameterIndex, Object x) throws SQLException {
        preparedStatement.setObject(parameterIndex, x);
    }

    /**
     * Executes the SQL statement in this <code>PreparedStatement</code> object,
     * which may be any kind of SQL statement.
     * Some prepared statements return multiple results; the <code>execute</code>
     * method handles these complex statements as well as the simpler
     * form of statements handled by the methods <code>executeQuery</code>
     * and <code>executeUpdate</code>.
     * <p/>
     * The <code>execute</code> method returns a <code>boolean</code> to
     * indicate the form of the first result.  You must call either the method
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result; you must call <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     *         object; <code>false</code> if the first result is an update
     *         count or there is no result
     * @throws java.sql.SQLException if a database access error occurs or an argument
     *                               is supplied to this method
     * @see java.sql.Statement#execute
     * @see java.sql.Statement#getResultSet
     * @see java.sql.Statement#getUpdateCount
     * @see java.sql.Statement#getMoreResults
     */
    public boolean execute() throws SQLException {
        return preparedStatement.execute();
    }

    /**
     * Adds a set of parameters to this <code>PreparedStatement</code>
     * object's batch of commands.
     *
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.Statement#addBatch
     * @since 1.2
     */
    public void addBatch() throws SQLException {
        preparedStatement.addBatch();
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
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param reader         the <code>java.io.Reader</code> object that contains the
     *                       Unicode data
     * @param length         the number of characters in the stream
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    /**
     * Sets the designated parameter to the given
     * <code>REF(&lt;structured-type&gt;)</code> value.
     * The driver converts this to an SQL <code>REF</code> value when it
     * sends it to the database.
     *
     * @param i the first parameter is 1, the second is 2, ...
     * @param x an SQL <code>REF</code> value
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public void setRef(int i, Ref x) throws SQLException {
        preparedStatement.setRef(i, x);
    }

    /**
     * Sets the designated parameter to the given <code>Blob</code> object.
     * The driver converts this to an SQL <code>BLOB</code> value when it
     * sends it to the database.
     *
     * @param i the first parameter is 1, the second is 2, ...
     * @param x a <code>Blob</code> object that maps an SQL <code>BLOB</code> value
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public void setBlob(int i, Blob x) throws SQLException {
        preparedStatement.setBlob(i, x);
    }

    /**
     * Sets the designated parameter to the given <code>Clob</code> object.
     * The driver converts this to an SQL <code>CLOB</code> value when it
     * sends it to the database.
     *
     * @param i the first parameter is 1, the second is 2, ...
     * @param x a <code>Clob</code> object that maps an SQL <code>CLOB</code> value
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public void setClob(int i, Clob x) throws SQLException {
        preparedStatement.setClob(i, x);
    }

    /**
     * Sets the designated parameter to the given <code>Array</code> object.
     * The driver converts this to an SQL <code>ARRAY</code> value when it
     * sends it to the database.
     *
     * @param i the first parameter is 1, the second is 2, ...
     * @param x an <code>Array</code> object that maps an SQL <code>ARRAY</code> value
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public void setArray(int i, Array x) throws SQLException {
        preparedStatement.setArray(i, x);
    }

    /**
     * Retrieves a <code>ResultSetMetaData</code> object that contains
     * information about the columns of the <code>ResultSet</code> object
     * that will be returned when this <code>PreparedStatement</code> object
     * is executed.
     * <p/>
     * Because a <code>PreparedStatement</code> object is precompiled, it is
     * possible to know about the <code>ResultSet</code> object that it will
     * return without having to execute it.  Consequently, it is possible
     * to invoke the method <code>getMetaData</code> on a
     * <code>PreparedStatement</code> object rather than waiting to execute
     * it and then invoking the <code>ResultSet.getMetaData</code> method
     * on the <code>ResultSet</code> object that is returned.
     * <p/>
     * <B>NOTE:</B> Using this method may be expensive for some drivers due
     * to the lack of underlying DBMS support.
     *
     * @return the description of a <code>ResultSet</code> object's columns or
     *         <code>null</code> if the driver cannot return a
     *         <code>ResultSetMetaData</code> object
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public ResultSetMetaData getMetaData() throws SQLException {
        return preparedStatement.getMetaData();
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>DATE</code> value,
     * which the driver then sends to the database.  With
     * a <code>Calendar</code> object, the driver can calculate the date
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @param cal            the <code>Calendar</code> object the driver will use
     *                       to construct the date
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        preparedStatement.setDate(parameterIndex, x, cal);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIME</code> value,
     * which the driver then sends to the database.  With
     * a <code>Calendar</code> object, the driver can calculate the time
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @param cal            the <code>Calendar</code> object the driver will use
     *                       to construct the time
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        preparedStatement.setTime(parameterIndex, x, cal);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIMESTAMP</code> value,
     * which the driver then sends to the database.  With a
     * <code>Calendar</code> object, the driver can calculate the timestamp
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @param cal            the <code>Calendar</code> object the driver will use
     *                       to construct the timestamp
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, x, cal);
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
     * @param paramIndex the first parameter is 1, the second is 2, ...
     * @param sqlType    a value from <code>java.sql.Types</code>
     * @param typeName   the fully-qualified name of an SQL user-defined type;
     *                   ignored if the parameter is not a user-defined type or REF
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
        preparedStatement.setNull(paramIndex, sqlType, typeName);
    }

    /**
     * Sets the designated parameter to the given <code>java.net.URL</code> value.
     * The driver converts this to an SQL <code>DATALINK</code> value
     * when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the <code>java.net.URL</code> object to be set
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public void setURL(int parameterIndex, URL x) throws SQLException {
        preparedStatement.setURL(parameterIndex, x);
    }

    /**
     * Retrieves the number, types and properties of this
     * <code>PreparedStatement</code> object's parameters.
     *
     * @return a <code>ParameterMetaData</code> object that contains information
     *         about the number, types and properties of this
     *         <code>PreparedStatement</code> object's parameters
     * @throws java.sql.SQLException if a database access error occurs
     * @see java.sql.ParameterMetaData
     * @since 1.4
     */
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return preparedStatement.getParameterMetaData();
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        if (busy) {
            if (leakDetector != null) {
                leakDetector.startStatementLeakTracing(preparedStatement, this);
            }
        } else {
            if (leakDetector != null) {
                leakDetector.stopStatementLeakTracing(preparedStatement, this);
                if(cached && isMarkedForReclaim()) {
                    //When caching is on and is marked for reclaim, the statement
                    //would still remain in cache. Hence mark it as invalid and
                    //let the client that uses the statement, detect and purge
                    //it if found as invalid
                    setValid(false);
                }
            }
        }
    }

    public boolean getCached() {
        return cached;
    }

    public void close() throws SQLException {
        if (!cached) {
            //Stop leak tracing
            if(leakDetector != null) {
                leakDetector.stopStatementLeakTracing(preparedStatement, this);
            }
            preparedStatement.close();
        } else {
            //TODO-SC what if Exception is thrown in this block, should there be a way to indicate the
            // con. not to use this statement any more ?
            clearParameters();

            if (defaultQueryTimeout != currentQueryTimeout) {
                preparedStatement.setQueryTimeout(defaultQueryTimeout);
                currentQueryTimeout = defaultQueryTimeout;
            }
            if (defaultMaxFieldSize != currentMaxFieldSize) {
                preparedStatement.setMaxFieldSize(defaultMaxFieldSize);
                currentMaxFieldSize = defaultMaxFieldSize;
            }
            if (defaultFetchSize != currentFetchSize) {
                preparedStatement.setFetchSize(defaultFetchSize);
                currentFetchSize = defaultFetchSize;
            }
            if (defaultMaxRows != currentMaxRows) {
                preparedStatement.setMaxRows(defaultMaxRows);
                currentMaxRows = defaultMaxRows;
            }

            if (defaultFetchDirection != currentFetchDirection) {
                preparedStatement.setFetchDirection(defaultFetchDirection);
                currentFetchDirection = defaultFetchDirection;
            }

            setBusy(false);
        }
    }

    public void closeOnCompletion() throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            if(!cached) {
                //If statement caching is not turned on, call the driver implementation directly
                if (leakDetector != null) {
                    _logger.log(Level.INFO, "jdbc.invalid_operation.close_on_completion");
                    throw new UnsupportedOperationException("Not supported yet.");
                }
                actualCloseOnCompletion();
            } else {
                super.closeOnCompletion();
            }
        }
    }

    public boolean isCloseOnCompletion() throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            if(cached) {
                return getCloseOnCompletion();
            }
        }
        return super.isCloseOnCompletion();
    }

    public void setMaxFieldSize(int max) throws SQLException {
        preparedStatement.setMaxFieldSize(max);
        if (cached)
            currentMaxFieldSize = max;
    }

    public void setMaxRows(int max) throws SQLException {
        preparedStatement.setMaxRows(max);
        if (cached)
            currentMaxRows = max;
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        preparedStatement.setQueryTimeout(seconds);
        if (cached)
            currentQueryTimeout = seconds;
    }

    public void setFetchDirection(int direction) throws SQLException {
        preparedStatement.setFetchDirection(direction);
        if (cached)
            currentFetchDirection = direction;
    }

    public void setFetchSize(int rows) throws SQLException {
        preparedStatement.setFetchSize(rows);
        if (cached)
            currentFetchSize = rows;
    }

    public void setCached(boolean cached){
        this.cached =  cached;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void incrementResultSetReferenceCount() {
        //Update resultSetCount to be used in case of jdbc41 closeOnCompletion
        if (DataSourceObjectBuilder.isJDBC41() && getCached()) {
            incrementResultSetCount();
        }
    }

    public void resultSetClosed() throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41() && getCached()) {
            decrementResultSetCount();
            if (getCloseOnCompletion() && getResultSetCount() == 0) {
                ConnectionHolder wrappedCon = (ConnectionHolder) getConnection();
                wrappedCon.getManagedConnection().purgeStatementFromCache(this);
            }
        }
    }
}
