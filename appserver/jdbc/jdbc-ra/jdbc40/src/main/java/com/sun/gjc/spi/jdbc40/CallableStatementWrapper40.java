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

package com.sun.gjc.spi.jdbc40;

import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.gjc.spi.base.CallableStatementWrapper;

import java.io.InputStream;
import java.io.Reader;
import java.sql.*;
import java.util.logging.Level;
import javax.resource.ResourceException;

/**
 * Wrapper for JDBC 4.0 CallableStatement
 */
public class CallableStatementWrapper40 extends CallableStatementWrapper {
    /**
     * Creates a new instance of CallableStatement wrapper for JDBC 3.0<br>
     *
     * @param con       ConnectionWrapper<br>
     * @param statement CallableStatement that is wrapped<br>
     */
    public CallableStatementWrapper40(Connection con, CallableStatement statement,
                                      boolean cachingEnabled)
            throws SQLException {
        super(con, statement, cachingEnabled);
    }

    /**
     * Retrieves the value of the designated parameter as a
     * <code>java.io.Reader</code> object in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @return a <code>java.io.Reader</code> object that contains the parameter
     *         value; if the value is SQL <code>NULL</code>, the value returned is
     *         <code>null</code> in the Java programming language.
     * @throws SQLException if the parameterIndex is not valid; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @since 1.6
     */
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return callableStatement.getCharacterStream(parameterIndex);
    }

    /**
     * Retrieves the value of the designated parameter as a
     * <code>java.io.Reader</code> object in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return a <code>java.io.Reader</code> object that contains the parameter
     *         value; if the value is SQL <code>NULL</code>, the value returned is
     *         <code>null</code> in the Java programming language
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public Reader getCharacterStream(String parameterName) throws SQLException {
        return callableStatement.getCharacterStream(parameterName);
    }

    /**
     * Retrieves the value of the designated parameter as a
     * <code>java.io.Reader</code> object in the Java programming language.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> parameters.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @return a <code>java.io.Reader</code> object that contains the parameter
     *         value; if the value is SQL <code>NULL</code>, the value returned is
     *         <code>null</code> in the Java programming language.
     * @throws SQLException if the parameterIndex is not valid;
     *                      if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return callableStatement.getNCharacterStream(parameterIndex);
    }

    /**
     * Retrieves the value of the designated parameter as a
     * <code>java.io.Reader</code> object in the Java programming language.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> parameters.
     *
     * @param parameterName the name of the parameter
     * @return a <code>java.io.Reader</code> object that contains the parameter
     *         value; if the value is SQL <code>NULL</code>, the value returned is
     *         <code>null</code> in the Java programming language
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return callableStatement.getNCharacterStream(parameterName);
    }

    /**
     * Retrieves the value of the designated JDBC <code>NCLOB</code> parameter as a
     * <code>java.sql.NClob</code> object in the Java programming language.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, and
     *                       so on
     * @return the parameter value as a <code>NClob</code> object in the
     *         Java programming language.  If the value was SQL <code>NULL</code>, the
     *         value <code>null</code> is returned.
     * @throws SQLException if the parameterIndex is not valid;
     *                      if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public NClob getNClob(int parameterIndex) throws SQLException {
        return callableStatement.getNClob(parameterIndex);
    }

    /**
     * Retrieves the value of a JDBC <code>NCLOB</code> parameter as a
     * <code>java.sql.NClob</code> object in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return the parameter value as a <code>NClob</code> object in the
     *         Java programming language.  If the value was SQL <code>NULL</code>,
     *         the value <code>null</code> is returned.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public NClob getNClob(String parameterName) throws SQLException {
        return callableStatement.getNClob(parameterName);
    }

    /**
     * Retrieves the value of the designated <code>NCHAR</code>,
     * <code>NVARCHAR</code>
     * or <code>LONGNVARCHAR</code> parameter as
     * a <code>String</code> in the Java programming language.
     * <p/>
     * For the fixed-length type JDBC <code>NCHAR</code>,
     * the <code>String</code> object
     * returned has exactly the same value the SQL
     * <code>NCHAR</code> value had in the
     * database, including any padding added by the database.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @return a <code>String</code> object that maps an
     *         <code>NCHAR</code>, <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
     * @throws SQLException if the parameterIndex is not valid;
     *                      if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @see #setNString
     * @since 1.6
     */
    public String getNString(int parameterIndex) throws SQLException {
        return callableStatement.getNString(parameterIndex);
    }

    /**
     * Retrieves the value of the designated <code>NCHAR</code>,
     * <code>NVARCHAR</code>
     * or <code>LONGNVARCHAR</code> parameter as
     * a <code>String</code> in the Java programming language.
     * <p/>
     * For the fixed-length type JDBC <code>NCHAR</code>,
     * the <code>String</code> object
     * returned has exactly the same value the SQL
     * <code>NCHAR</code> value had in the
     * database, including any padding added by the database.
     *
     * @param parameterName the name of the parameter
     * @return a <code>String</code> object that maps an
     *         <code>NCHAR</code>, <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter;
     *                      if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @see #setNString
     * @since 1.6
     */
    public String getNString(String parameterName) throws SQLException {
        return callableStatement.getNString(parameterName);
    }

    /**
     * Retrieves the value of the designated JDBC <code>ROWID</code> parameter as a
     * <code>java.sql.RowId</code> object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2,...
     * @return a <code>RowId</code> object that represents the JDBC <code>ROWID</code>
     *         value is used as the designated parameter. If the parameter contains
     *         a SQL <code>NULL</code>, then a <code>null</code> value is returned.
     * @throws SQLException if the parameterIndex is not valid;
     *                      if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public RowId getRowId(int parameterIndex) throws SQLException {
        return callableStatement.getRowId(parameterIndex);
    }

    /**
     * Retrieves the value of the designated JDBC <code>ROWID</code> parameter as a
     * <code>java.sql.RowId</code> object.
     *
     * @param parameterName the name of the parameter
     * @return a <code>RowId</code> object that represents the JDBC <code>ROWID</code>
     *         value is used as the designated parameter. If the parameter contains
     *         a SQL <code>NULL</code>, then a <code>null</code> value is returned.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public RowId getRowId(String parameterName) throws SQLException {
        return callableStatement.getRowId(parameterName);
    }

    /**
     * Retrieves the value of the designated <code>SQL XML</code> parameter as a
     * <code>java.sql.SQLXML</code> object in the Java programming language.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
     * @throws SQLException if the parameterIndex is not valid;
     *                      if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return callableStatement.getSQLXML(parameterIndex);
    }

    /**
     * Retrieves the value of the designated <code>SQL XML</code> parameter as a
     * <code>java.sql.SQLXML</code> object in the Java programming language.
     *
     * @param parameterName the name of the parameter
     * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return callableStatement.getSQLXML(parameterName);
    }

    /**
     * Sets the designated parameter to the given input stream.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setAsciiStream</code> which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param x             the Java input stream that contains the ASCII parameter value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        callableStatement.setAsciiStream(parameterName, x);
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
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        callableStatement.setAsciiStream(parameterName, x, length);
    }

    /**
     * Sets the designated parameter to the given input stream.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBinaryStream</code> which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param x             the java input stream which contains the binary parameter value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        callableStatement.setBinaryStream(parameterName, x);
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
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        callableStatement.setBinaryStream(parameterName, x, length);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Blob</code> object.
     * The driver converts this to an SQL <code>BLOB</code> value when it
     * sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             a <code>Blob</code> object that maps an SQL <code>BLOB</code> value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setBlob(String parameterName, Blob x) throws SQLException {
        callableStatement.setBlob(parameterName, x);
    }

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.
     * This method differs from the <code>setBinaryStream (int, InputStream)</code>
     * method because it informs the driver that the parameter value should be
     * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
     * <p/>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBlob</code> which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param inputStream   An object that contains the data to set the parameter
     *                      value to.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        callableStatement.setBlob(parameterName, inputStream);
    }

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.  The <code>inputstream</code> must contain  the number
     * of characters specified by length, otherwise a <code>SQLException</code> will be
     * generated when the <code>CallableStatement</code> is executed.
     * This method differs from the <code>setBinaryStream (int, InputStream, int)</code>
     * method because it informs the driver that the parameter value should be
     * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
     *
     * @param parameterName the name of the parameter to be set
     *                      the second is 2, ...
     * @param inputStream   An object that contains the data to set the parameter
     *                      value to.
     * @param length        the number of bytes in the parameter data.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if the length specified
     *                      is less than zero; if the number of bytes in the inputstream does not match
     *                      the specfied length; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        callableStatement.setBlob(parameterName, inputStream, length);
    }

    /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setCharacterStream</code> which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param reader        the <code>java.io.Reader</code> object that contains the
     *                      Unicode data
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        callableStatement.setCharacterStream(parameterName, reader);
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
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        callableStatement.setCharacterStream(parameterName, reader, length);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Clob</code> object.
     * The driver converts this to an SQL <code>CLOB</code> value when it
     * sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param x             a <code>Clob</code> object that maps an SQL <code>CLOB</code> value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setClob(String parameterName, Clob x) throws SQLException {
        callableStatement.setClob(parameterName, x);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.
     * This method differs from the <code>setCharacterStream (int, Reader)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
     * <p/>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setClob</code> which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param reader        An object that contains the data to set the parameter value to.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or this method is called on
     *                      a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setClob(String parameterName, Reader reader) throws SQLException {
        callableStatement.setClob(parameterName, reader);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNCharacterStream</code> which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param value         the parameter value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs; or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        callableStatement.setNCharacterStream(parameterName, value);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     *
     * @param parameterName the name of the parameter to be set
     * @param value         the parameter value
     * @param length        the number of characters in the parameter data.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        callableStatement.setNCharacterStream(parameterName, value, length);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.
     * This method differs from the <code>setCharacterStream (int, Reader)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNClob</code> which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param reader        An object that contains the data to set the parameter value to.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if the driver does not support national character sets;
     *                      if the driver can detect that a data conversion
     *                      error could occur;  if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setNClob(String parameterName, NClob reader) throws SQLException {
        callableStatement.setNClob(parameterName, reader);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The <code>reader</code> must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>CallableStatement</code> is executed.
     * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
     *
     * @param parameterName the name of the parameter to be set
     * @param reader        An object that contains the data to set the parameter value to.
     * @param length        the number of characters in the parameter data.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if the length specified is less than zero;
     *                      a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        callableStatement.setClob(parameterName, reader, length);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.
     * This method differs from the <code>setCharacterStream (int, Reader)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNClob</code> which takes a length parameter.
     *
     * @param parameterName the name of the parameter
     * @param reader        An object that contains the data to set the parameter value to.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if the driver does not support national character sets;
     *                      if the driver can detect that a data conversion
     *                      error could occur;  if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setNClob(String parameterName, Reader reader) throws SQLException {
        callableStatement.setNClob(parameterName, reader);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The <code>reader</code> must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>CallableStatement</code> is executed.
     * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be send to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     *
     * @param parameterName the name of the parameter to be set
     * @param reader        An object that contains the data to set the parameter value to.
     * @param length        the number of characters in the parameter data.
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if the length specified is less than zero;
     *                      if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        callableStatement.setNClob(parameterName, reader, length);
    }

    /**
     * Sets the designated parameter to the given <code>String</code> object.
     * The driver converts this to a SQL <code>NCHAR</code> or
     * <code>NVARCHAR</code> or <code>LONGNVARCHAR</code>
     *
     * @param parameterName the name of the parameter to be set
     * @param value         the parameter value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setNString(String parameterName, String value) throws SQLException {
        callableStatement.setNString(parameterName, value);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.RowId</code> object. The
     * driver converts this to a SQL <code>ROWID</code> when it sends it to the
     * database.
     *
     * @param parameterName the name of the parameter
     * @param x             the parameter value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs or
     *                      this method is called on a closed <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setRowId(String parameterName, RowId x) throws SQLException {
        callableStatement.setRowId(parameterName, x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.SQLXML</code> object. The driver converts this to an
     * <code>SQL XML</code> value when it sends it to the database.
     *
     * @param parameterName the name of the parameter
     * @param xmlObject     a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
     * @throws SQLException if parameterName does not correspond to a named
     *                      parameter; if a database access error occurs;
     *                      this method is called on a closed <code>CallableStatement</code> or
     *                      the <code>java.xml.transform.Result</code>,
     *                      <code>Writer</code> or <code>OutputStream</code> has not been closed for the <code>SQLXML</code> object
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support
     *                      this method
     * @since 1.6
     */
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        callableStatement.setSQLXML(parameterName, xmlObject);
    }

    /**
     * Sets the designated parameter to the given input stream.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setAsciiStream</code> which takes a length parameter.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the Java input stream that contains the ASCII parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        callableStatement.setAsciiStream(parameterIndex, x);
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
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @since 1.6
     */
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        callableStatement.setAsciiStream(parameterIndex, x, length);
    }

    /**
     * Sets the designated parameter to the given input stream.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBinaryStream</code> which takes a length parameter.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the java input stream which contains the binary parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        callableStatement.setBinaryStream(parameterIndex, x);
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
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @since 1.6
     */
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        callableStatement.setBinaryStream(parameterIndex, x, length);
    }

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.
     * This method differs from the <code>setBinaryStream (int, InputStream)</code>
     * method because it informs the driver that the parameter value should be
     * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
     * <p/>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBlob</code> which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1,
     *                       the second is 2, ...
     * @param inputStream    An object that contains the data to set the parameter
     *                       value to.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs;
     *                      this method is called on a closed <code>PreparedStatement</code> or
     *                      if parameterIndex does not correspond
     *                      to a parameter marker in the SQL statement,
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        callableStatement.setBlob(parameterIndex, inputStream);
    }

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.  The inputstream must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>PreparedStatement</code> is executed.
     * This method differs from the <code>setBinaryStream (int, InputStream, int)</code>
     * method because it informs the driver that the parameter value should be
     * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
     *
     * @param parameterIndex index of the first parameter is 1,
     *                       the second is 2, ...
     * @param inputStream    An object that contains the data to set the parameter
     *                       value to.
     * @param length         the number of bytes in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs;
     *                      this method is called on a closed <code>PreparedStatement</code>;
     *                      if the length specified
     *                      is less than zero or if the number of bytes in the inputstream does not match
     *                      the specfied length.
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        callableStatement.setBlob(parameterIndex, inputStream, length);
    }

    /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setCharacterStream</code> which takes a length parameter.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param reader         the <code>java.io.Reader</code> object that contains the
     *                       Unicode data
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        callableStatement.setCharacterStream(parameterIndex, reader);
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
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @since 1.6
     */
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        callableStatement.setCharacterStream(parameterIndex, reader, length);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.
     * This method differs from the <code>setCharacterStream (int, Reader)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
     * <p/>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setClob</code> which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader         An object that contains the data to set the parameter value to.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs; this method is called on
     *                      a closed <code>PreparedStatement</code>or if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        callableStatement.setClob(parameterIndex, reader);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>PreparedStatement</code> is executed.
     * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader         An object that contains the data to set the parameter value to.
     * @param length         the number of characters in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs; this method is called on
     *                      a closed <code>PreparedStatement</code> or if the length specified is less than zero.
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        callableStatement.setClob(parameterIndex, reader, length);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * <p/>
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNCharacterStream</code> which takes a length parameter.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value          the parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs; or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        callableStatement.setNCharacterStream(parameterIndex, value);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value          the parameter value
     * @param length         the number of characters in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs; or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        callableStatement.setNCharacterStream(parameterIndex, value, length);
    }

    /**
     * Sets the designated parameter to a <code>java.sql.NClob</code> object. The driver converts this to a
     * SQL <code>NCLOB</code> value when it sends it to the database.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value          the parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs; or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        callableStatement.setNClob(parameterIndex, value);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.
     * This method differs from the <code>setCharacterStream (int, Reader)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNClob</code> which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader         An object that contains the data to set the parameter value to.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement;
     *                      if the driver does not support national character sets;
     *                      if the driver can detect that a data conversion
     *                      error could occur;  if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        callableStatement.setNClob(parameterIndex, reader);
    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>PreparedStatement</code> is executed.
     * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader         An object that contains the data to set the parameter value to.
     * @param length         the number of characters in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if the length specified is less than zero;
     *                      if the driver does not support national character sets;
     *                      if the driver can detect that a data conversion
     *                      error could occur;  if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        callableStatement.setNClob(parameterIndex, reader, length);
    }

    /**
     * Sets the designated paramter to the given <code>String</code> object.
     * The driver converts this to a SQL <code>NCHAR</code> or
     * <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
     * (depending on the argument's
     * size relative to the driver's limits on <code>NVARCHAR</code> values)
     * when it sends it to the database.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value          the parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if the driver does not support national
     *                      character sets;  if the driver can detect that a data conversion
     *                      error could occur; if a database access error occurs; or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setNString(int parameterIndex, String value) throws SQLException {
        callableStatement.setNString(parameterIndex, value);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.RowId</code> object. The
     * driver converts this to a SQL <code>ROWID</code> value when it sends it
     * to the database
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        callableStatement.setRowId(parameterIndex, x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.SQLXML</code> object.
     * The driver converts this to an
     * SQL <code>XML</code> value when it sends it to the database.
     * <p/>
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param xmlObject      a <code>SQLXML</code> object that maps an SQL <code>XML</code> value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs;
     *                      this method is called on a closed <code>PreparedStatement</code>
     *                      or the <code>java.xml.transform.Result</code>,
     *                      <code>Writer</code> or <code>OutputStream</code> has not been closed for
     *                      the <code>SQLXML</code> object
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.6
     */
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        callableStatement.setSQLXML(parameterIndex, xmlObject);
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
        return callableStatement.isClosed();
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
        return callableStatement.isPoolable();
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
        callableStatement.setPoolable(poolable);
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
            result = callableStatement.isWrapperFor(iface);
        }
        return result;
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
        T result = null;
        if (iface.isInstance(this)) {
            result = iface.cast(this);
        } else {
            result = callableStatement.unwrap(iface);
        }
        return result;
    }

    /**
     * Executes the SQL query in this <code>PreparedStatement</code> object
     * and returns the <code>ResultSet</code> object generated by the query.
     *
     * @return a <code>ResultSet</code> object that contains the data produced by the
     *         query; never <code>null</code>
     * @throws SQLException if a database access error occurs;
     *                      this method is called on a closed  <code>PreparedStatement</code> or the SQL
     *                      statement does not return a <code>ResultSet</code> object
     */
    public java.sql.ResultSet executeQuery() throws java.sql.SQLException {
        ResultSet rs = callableStatement.executeQuery();
        incrementResultSetReferenceCount();
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
    public java.sql.ResultSet executeQuery(String sql) throws
            java.sql.SQLException {
        ResultSet rs = callableStatement.executeQuery(sql);
        incrementResultSetReferenceCount();
        return new ResultSetWrapper40(this, rs);
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
     * @throws SQLFeatureNotSupportedException
     *                      if the JDBC driver does not support this method
     * @since 1.4
     */
    public java.sql.ResultSet getGeneratedKeys() throws java.sql.SQLException {
        ResultSet rs = callableStatement.getGeneratedKeys();
        if (rs == null)
            return null;
        incrementResultSetReferenceCount();
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
        ResultSet rs = callableStatement.getResultSet();
        if (rs == null)
            return null;
        incrementResultSetReferenceCount();
        return new ResultSetWrapper40(this, rs);
    }

    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            Class<?>[] valueTypes = new Class<?>[]{Integer.TYPE, Class.class};
            try {
                return (T) executor.invokeMethod(jdbcStatement, "getObject",
                        valueTypes, parameterIndex, type);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_get_object", ex);
                throw new SQLException(ex);
            }
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }

    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            Class<?>[] valueTypes = new Class<?>[]{String.class, Class.class};
            try {
                return (T) executor.invokeMethod(jdbcStatement, "getObject",
                        valueTypes, parameterName, type);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_get_object", ex);
                throw new SQLException(ex);
            }
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }
}
