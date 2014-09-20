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
import com.sun.gjc.util.MethodExecutor;
import com.sun.gjc.util.StatementLeakDetector;
import com.sun.gjc.util.StatementLeakListener;
import com.sun.logging.LogDomains;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.ResourceException;

/**
 * Abstract class for wrapping Statement<br>
 */
public abstract class StatementWrapper implements Statement, StatementLeakListener {

    protected Connection connection = null;
    protected Statement jdbcStatement = null;
    protected StatementLeakDetector leakDetector = null;
    private boolean markedForReclaim = false;
    protected final static Logger _logger;
    protected MethodExecutor executor = null;
    private boolean closeOnCompletion = false;
    protected AtomicInteger resultSetCount = new AtomicInteger();


    static {
        _logger = LogDomains.getLogger(MethodExecutor.class, LogDomains.RSR_LOGGER);
    }
    
    /**
     * Abstract class for wrapping Statement<br>
     *
     * @param con       ConnectionWrapper <br>
     * @param statement Statement that is to be wrapped<br>
     */
    public StatementWrapper(Connection con, Statement statement) {
        connection = con;
        jdbcStatement = statement;
        executor = new MethodExecutor();
        //Start leak tracing if statement is a pure Statement & stmtWrapping is ON
        //Check if this is an instanceof PS/CS. There could exist
        //a CustomStatement class in a jdbc driver that implements PS/CS as well
        //as Statement
        if(!(this instanceof PreparedStatement) &&
                !(this instanceof CallableStatement)) {
            ConnectionHolder wrappedCon = (ConnectionHolder) con;

            leakDetector = wrappedCon.getManagedConnection().getLeakDetector();
            if(leakDetector != null) {
                leakDetector.startStatementLeakTracing(jdbcStatement, this);
            }
        }
        //If PS or CS, do not start leak tracing here
    }

    /**
     * Executes the given SQL statement, which may be an <code>INSERT</code>,
     * <code>UPDATE</code>, or <code>DELETE</code> statement or an
     * SQL statement that returns nothing, such as an SQL DDL statement.
     *
     * @param sql an SQL <code>INSERT</code>, <code>UPDATE</code> or
     *            <code>DELETE</code> statement or an SQL statement that returns nothing
     * @return either the row count for <code>INSERT</code>, <code>UPDATE</code>
     *         or <code>DELETE</code> statements, or <code>0</code> for SQL statements
     *         that return nothing
     * @throws java.sql.SQLException if a database access error occurs or the given
     *                               SQL statement produces a <code>ResultSet</code> object
     */
    public int executeUpdate(final String sql) throws SQLException {
        return jdbcStatement.executeUpdate(sql);
    }

    /**
     * Releases this <code>Statement</code> object's database
     * and JDBC resources immediately instead of waiting for
     * this to happen when it is automatically closed.
     * It is generally good practice to release resources as soon as
     * you are finished with them to avoid tying up database
     * resources.
     * <p/>
     * Calling the method <code>close</code> on a <code>Statement</code>
     * object that is already closed has no effect.
     * <p/>
     * <B>Note:</B> A <code>Statement</code> object is automatically closed
     * when it is garbage collected. When a <code>Statement</code> object is
     * closed, its current <code>ResultSet</code> object, if one exists, is
     * also closed.
     *
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void close() throws SQLException {
        //Stop leak tracing
        if(leakDetector != null) {
            leakDetector.stopStatementLeakTracing(jdbcStatement, this);
        }
        jdbcStatement.close();
    }

    /**
     * Retrieves the maximum number of bytes that can be
     * returned for character and binary column values in a <code>ResultSet</code>
     * object produced by this <code>Statement</code> object.
     * This limit applies only to <code>BINARY</code>,
     * <code>VARBINARY</code>, <code>LONGVARBINARY</code>, <code>CHAR</code>,
     * <code>VARCHAR</code>, and <code>LONGVARCHAR</code>
     * columns.  If the limit is exceeded, the excess data is silently
     * discarded.
     *
     * @return the current column size limit for columns storing character and
     *         binary values; zero means there is no limit
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setMaxFieldSize
     */
    public int getMaxFieldSize() throws SQLException {
        return jdbcStatement.getMaxFieldSize();
    }

    /**
     * Sets the limit for the maximum number of bytes in a <code>ResultSet</code>
     * column storing character or binary values to
     * the given number of bytes.  This limit applies
     * only to <code>BINARY</code>, <code>VARBINARY</code>,
     * <code>LONGVARBINARY</code>, <code>CHAR</code>, <code>VARCHAR</code>, and
     * <code>LONGVARCHAR</code> fields.  If the limit is exceeded, the excess data
     * is silently discarded. For maximum portability, use values
     * greater than 256.
     *
     * @param max the new column size limit in bytes; zero means there is no limit
     * @throws java.sql.SQLException if a database access error occurs
     *                               or the condition max >= 0 is not satisfied
     * @see #getMaxFieldSize
     */
    public void setMaxFieldSize(int max) throws SQLException {
        jdbcStatement.setMaxFieldSize(max);
    }

    /**
     * Retrieves the maximum number of rows that a
     * <code>ResultSet</code> object produced by this
     * <code>Statement</code> object can contain.  If this limit is exceeded,
     * the excess rows are silently dropped.
     *
     * @return the current maximum number of rows for a <code>ResultSet</code>
     *         object produced by this <code>Statement</code> object;
     *         zero means there is no limit
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setMaxRows
     */
    public int getMaxRows() throws SQLException {
        return jdbcStatement.getMaxRows();
    }

    /**
     * Sets the limit for the maximum number of rows that any
     * <code>ResultSet</code> object can contain to the given number.
     * If the limit is exceeded, the excess
     * rows are silently dropped.
     *
     * @param max the new max rows limit; zero means there is no limit
     * @throws java.sql.SQLException if a database access error occurs
     *                               or the condition max >= 0 is not satisfied
     * @see #getMaxRows
     */
    public void setMaxRows(int max) throws SQLException {
        jdbcStatement.setMaxRows(max);
    }

    /**
     * Sets escape processing on or off.
     * If escape scanning is on (the default), the driver will do
     * escape substitution before sending the SQL statement to the database.
     * <p/>
     * Note: Since prepared statements have usually been parsed prior
     * to making this call, disabling escape processing for
     * <code>PreparedStatements</code> objects will have no effect.
     *
     * @param enable <code>true</code> to enable escape processing;
     *               <code>false</code> to disable it
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setEscapeProcessing(boolean enable) throws SQLException {
        jdbcStatement.setEscapeProcessing(enable);
    }

    /**
     * Retrieves the number of seconds the driver will
     * wait for a <code>Statement</code> object to execute. If the limit is exceeded, a
     * <code>SQLException</code> is thrown.
     *
     * @return the current query timeout limit in seconds; zero means there is
     *         no limit
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setQueryTimeout
     */
    public int getQueryTimeout() throws SQLException {
        return jdbcStatement.getQueryTimeout();
    }

    /**
     * Sets the number of seconds the driver will wait for a
     * <code>Statement</code> object to execute to the given number of seconds.
     * If the limit is exceeded, an <code>SQLException</code> is thrown.
     *
     * @param seconds the new query timeout limit in seconds; zero means
     *                there is no limit
     * @throws java.sql.SQLException if a database access error occurs
     *                               or the condition seconds >= 0 is not satisfied
     * @see #getQueryTimeout
     */
    public void setQueryTimeout(int seconds) throws SQLException {
        jdbcStatement.setQueryTimeout(seconds);
    }

    /**
     * Cancels this <code>Statement</code> object if both the DBMS and
     * driver support aborting an SQL statement.
     * This method can be used by one thread to cancel a statement that
     * is being executed by another thread.
     *
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void cancel() throws SQLException {
        jdbcStatement.cancel();
    }

    /**
     * Retrieves the first warning reported by calls on this <code>Statement</code> object.
     * Subsequent <code>Statement</code> object warnings will be chained to this
     * <code>SQLWarning</code> object.
     * <p/>
     * <p>The warning chain is automatically cleared each time
     * a statement is (re)executed. This method may not be called on a closed
     * <code>Statement</code> object; doing so will cause an <code>SQLException</code>
     * to be thrown.
     * <p/>
     * <P><B>Note:</B> If you are processing a <code>ResultSet</code> object, any
     * warnings associated with reads on that <code>ResultSet</code> object
     * will be chained on it rather than on the <code>Statement</code>
     * object that produced it.
     *
     * @return the first <code>SQLWarning</code> object or <code>null</code>
     *         if there are no warnings
     * @throws java.sql.SQLException if a database access error occurs or this
     *                               method is called on a closed statement
     */
    public SQLWarning getWarnings() throws SQLException {
        return jdbcStatement.getWarnings();
    }

    /**
     * Clears all the warnings reported on this <code>Statement</code>
     * object. After a call to this method,
     * the method <code>getWarnings</code> will return
     * <code>null</code> until a new warning is reported for this
     * <code>Statement</code> object.
     *
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void clearWarnings() throws SQLException {
        jdbcStatement.clearWarnings();
    }

    /**
     * Sets the SQL cursor name to the given <code>String</code>, which
     * will be used by subsequent <code>Statement</code> object
     * <code>execute</code> methods. This name can then be
     * used in SQL positioned update or delete statements to identify the
     * current row in the <code>ResultSet</code> object generated by this
     * statement.  If the database does not support positioned update/delete,
     * this method is a noop.  To insure that a cursor has the proper isolation
     * level to support updates, the cursor's <code>SELECT</code> statement
     * should have the form <code>SELECT FOR UPDATE</code>.  If
     * <code>FOR UPDATE</code> is not present, positioned updates may fail.
     * <p/>
     * <P><B>Note:</B> By definition, the execution of positioned updates and
     * deletes must be done by a different <code>Statement</code> object than
     * the one that generated the <code>ResultSet</code> object being used for
     * positioning. Also, cursor names must be unique within a connection.
     *
     * @param name the new cursor name, which must be unique within
     *             a connection
     * @throws java.sql.SQLException if a database access error occurs
     */
    public void setCursorName(String name) throws SQLException {
        jdbcStatement.setCursorName(name);
    }

    /**
     * Executes the given SQL statement, which may return multiple results.
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p/>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *
     * @param sql any SQL statement
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there are
     *         no results
     * @throws java.sql.SQLException if a database access error occurs
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     */
    public boolean execute(final String sql) throws SQLException {
        return jdbcStatement.execute(sql);
    }


    /**
     * Retrieves the current result as an update count;
     * if the result is a <code>ResultSet</code> object or there are no more results, -1
     * is returned. This method should be called only once per result.
     *
     * @return the current result as an update count; -1 if the current result is a
     *         <code>ResultSet</code> object or there are no more results
     * @throws java.sql.SQLException if a database access error occurs
     * @see #execute
     */
    public int getUpdateCount() throws SQLException {
        return jdbcStatement.getUpdateCount();
    }

    /**
     * Moves to this <code>Statement</code> object's next result, returns
     * <code>true</code> if it is a <code>ResultSet</code> object, and
     * implicitly closes any current <code>ResultSet</code>
     * object(s) obtained with the method <code>getResultSet</code>.
     * <p/>
     * <P>There are no more results when the following is true:
     * <PRE>
     * // stmt is a Statement object
     * ((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1))
     * </PRE>
     *
     * @return <code>true</code> if the next result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there are
     *         no more results
     * @throws java.sql.SQLException if a database access error occurs
     * @see #execute
     */
    public boolean getMoreResults() throws SQLException {
        return jdbcStatement.getMoreResults();
    }

    /**
     * Gives the driver a hint as to the direction in which
     * rows will be processed in <code>ResultSet</code>
     * objects created using this <code>Statement</code> object.  The
     * default value is <code>ResultSet.FETCH_FORWARD</code>.
     * <p/>
     * Note that this method sets the default fetch direction for
     * result sets generated by this <code>Statement</code> object.
     * Each result set has its own methods for getting and setting
     * its own fetch direction.
     *
     * @param direction the initial direction for processing rows
     * @throws java.sql.SQLException if a database access error occurs
     *                               or the given direction
     *                               is not one of <code>ResultSet.FETCH_FORWARD</code>,
     *                               <code>ResultSet.FETCH_REVERSE</code>, or <code>ResultSet.FETCH_UNKNOWN</code>
     * @see #getFetchDirection
     * @since 1.2
     */
    public void setFetchDirection(int direction) throws SQLException {
        jdbcStatement.setFetchDirection(direction);
    }

    /**
     * Retrieves the direction for fetching rows from
     * database tables that is the default for result sets
     * generated from this <code>Statement</code> object.
     * If this <code>Statement</code> object has not set
     * a fetch direction by calling the method <code>setFetchDirection</code>,
     * the return value is implementation-specific.
     *
     * @return the default fetch direction for result sets generated
     *         from this <code>Statement</code> object
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setFetchDirection
     * @since 1.2
     */
    public int getFetchDirection() throws SQLException {
        return jdbcStatement.getFetchDirection();
    }

    /**
     * Gives the JDBC driver a hint as to the number of rows that should
     * be fetched from the database when more rows are needed.  The number
     * of rows specified affects only result sets created using this
     * statement. If the value specified is zero, then the hint is ignored.
     * The default value is zero.
     *
     * @param rows the number of rows to fetch
     * @throws java.sql.SQLException if a database access error occurs, or the
     *                               condition 0 <= <code>rows</code> <= <code>this.getMaxRows()</code>
     *                               is not satisfied.
     * @see #getFetchSize
     * @since 1.2
     */
    public void setFetchSize(int rows) throws SQLException {
        jdbcStatement.setFetchSize(rows);
    }

    /**
     * Retrieves the number of result set rows that is the default
     * fetch size for <code>ResultSet</code> objects
     * generated from this <code>Statement</code> object.
     * If this <code>Statement</code> object has not set
     * a fetch size by calling the method <code>setFetchSize</code>,
     * the return value is implementation-specific.
     *
     * @return the default fetch size for result sets generated
     *         from this <code>Statement</code> object
     * @throws java.sql.SQLException if a database access error occurs
     * @see #setFetchSize
     * @since 1.2
     */
    public int getFetchSize() throws SQLException {
        return jdbcStatement.getFetchSize();
    }

    /**
     * Retrieves the result set concurrency for <code>ResultSet</code> objects
     * generated by this <code>Statement</code> object.
     *
     * @return either <code>ResultSet.CONCUR_READ_ONLY</code> or
     *         <code>ResultSet.CONCUR_UPDATABLE</code>
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public int getResultSetConcurrency() throws SQLException {
        return jdbcStatement.getResultSetConcurrency();
    }

    /**
     * Retrieves the result set type for <code>ResultSet</code> objects
     * generated by this <code>Statement</code> object.
     *
     * @return one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *         <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *         <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public int getResultSetType() throws SQLException {
        return jdbcStatement.getResultSetType();
    }

    /**
     * Adds the given SQL command to the current list of commmands for this
     * <code>Statement</code> object. The commands in this list can be
     * executed as a batch by calling the method <code>executeBatch</code>.
     * <p/>
     * <B>NOTE:</B>  This method is optional.
     *
     * @param sql typically this is a static SQL <code>INSERT</code> or
     *            <code>UPDATE</code> statement
     * @throws java.sql.SQLException if a database access error occurs, or the
     *                               driver does not support batch updates
     * @see #executeBatch
     * @since 1.2
     */
    public void addBatch(String sql) throws SQLException {
        jdbcStatement.addBatch(sql);
    }

    /**
     * Empties this <code>Statement</code> object's current list of
     * SQL commands.
     * <p/>
     * <B>NOTE:</B>  This method is optional.
     *
     * @throws java.sql.SQLException if a database access error occurs or the
     *                               driver does not support batch updates
     * @see #addBatch
     * @since 1.2
     */
    public void clearBatch() throws SQLException {
        jdbcStatement.clearBatch();
    }

    /**
     * Submits a batch of commands to the database for execution and
     * if all commands execute successfully, returns an array of update counts.
     * The <code>int</code> elements of the array that is returned are ordered
     * to correspond to the commands in the batch, which are ordered
     * according to the order in which they were added to the batch.
     * The elements in the array returned by the method <code>executeBatch</code>
     * may be one of the following:
     * <OL>
     * <LI>A number greater than or equal to zero -- indicates that the
     * command was processed successfully and is an update count giving the
     * number of rows in the database that were affected by the command's
     * execution
     * <LI>A value of <code>SUCCESS_NO_INFO</code> -- indicates that the command was
     * processed successfully but that the number of rows affected is
     * unknown
     * <p/>
     * If one of the commands in a batch update fails to execute properly,
     * this method throws a <code>BatchUpdateException</code>, and a JDBC
     * driver may or may not continue to process the remaining commands in
     * the batch.  However, the driver's behavior must be consistent with a
     * particular DBMS, either always continuing to process commands or never
     * continuing to process commands.  If the driver continues processing
     * after a failure, the array returned by the method
     * <code>BatchUpdateException.getUpdateCounts</code>
     * will contain as many elements as there are commands in the batch, and
     * at least one of the elements will be the following:
     * <p/>
     * <LI>A value of <code>EXECUTE_FAILED</code> -- indicates that the command failed
     * to execute successfully and occurs only if a driver continues to
     * process commands after a command fails
     * </OL>
     * <p/>
     * A driver is not required to implement this method.
     * The possible implementations and return values have been modified in
     * the Java 2 SDK, Standard Edition, version 1.3 to
     * accommodate the option of continuing to proccess commands in a batch
     * update after a <code>BatchUpdateException</code> obejct has been thrown.
     *
     * @return an array of update counts containing one element for each
     *         command in the batch.  The elements of the array are ordered according
     *         to the order in which commands were added to the batch.
     * @throws java.sql.SQLException if a database access error occurs or the
     *                               driver does not support batch statements. Throws {@link java.sql.BatchUpdateException}
     *                               (a subclass of <code>SQLException</code>) if one of the commands sent to the
     *                               database fails to execute properly or attempts to return a result set.
     * @since 1.3
     */
    public int[] executeBatch() throws SQLException {
        return jdbcStatement.executeBatch();
    }

    /**
     * Retrieves the <code>Connection</code> object
     * that produced this <code>Statement</code> object.
     *
     * @return the connection that produced this statement
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.2
     */
    public Connection getConnection() throws SQLException {
        return connection;
    }

    /**
     * Returns the underlying physical connection.<br>
     *
     * @return the actual connection that produced this statement<br>
     * @throws SQLException
     */
    public Connection getActualConnection() throws SQLException {
        return jdbcStatement.getConnection();
    }

    /**
     * Moves to this <code>Statement</code> object's next result, deals with
     * any current <code>ResultSet</code> object(s) according  to the instructions
     * specified by the given flag, and returns
     * <code>true</code> if the next result is a <code>ResultSet</code> object.
     * <p/>
     * <P>There are no more results when the following is true:
     * <PRE>
     * // stmt is a Statement object
     * ((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1))
     * </PRE>
     *
     * @param current one of the following <code>Statement</code>
     *                constants indicating what should happen to current
     *                <code>ResultSet</code> objects obtained using the method
     *                <code>getResultSet</code>:
     *                <code>Statement.CLOSE_CURRENT_RESULT</code>,
     *                <code>Statement.KEEP_CURRENT_RESULT</code>, or
     *                <code>Statement.CLOSE_ALL_RESULTS</code>
     * @return <code>true</code> if the next result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there are no
     *         more results
     * @throws java.sql.SQLException if a database access error occurs or the argument
     *                               supplied is not one of the following:
     *                               <code>Statement.CLOSE_CURRENT_RESULT</code>,
     *                               <code>Statement.KEEP_CURRENT_RESULT</code>, or
     *                               <code>Statement.CLOSE_ALL_RESULTS</code>
     * @see #execute
     * @since 1.4
     */
    public boolean getMoreResults(int current) throws SQLException {
        return jdbcStatement.getMoreResults(current);
    }

    /**
     * Executes the given SQL statement and signals the driver with the
     * given flag about whether the
     * auto-generated keys produced by this <code>Statement</code> object
     * should be made available for retrieval.
     *
     * @param sql               must be an SQL <code>INSERT</code>, <code>UPDATE</code> or
     *                          <code>DELETE</code> statement or an SQL statement that
     *                          returns nothing
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *                          should be made available for retrieval;
     *                          one of the following constants:
     *                          <code>Statement.RETURN_GENERATED_KEYS</code>
     *                          <code>Statement.NO_GENERATED_KEYS</code>
     * @return either the row count for <code>INSERT</code>, <code>UPDATE</code>
     *         or <code>DELETE</code> statements, or <code>0</code> for SQL
     *         statements that return nothing
     * @throws java.sql.SQLException if a database access error occurs, the given
     *                               SQL statement returns a <code>ResultSet</code> object, or
     *                               the given constant is not one of those allowed
     * @since 1.4
     */
    public int executeUpdate(final String sql, int autoGeneratedKeys) throws SQLException {
        return jdbcStatement.executeUpdate(sql, autoGeneratedKeys);
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.  The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement.
     *
     * @param sql           an SQL <code>INSERT</code>, <code>UPDATE</code> or
     *                      <code>DELETE</code> statement or an SQL statement that returns nothing,
     *                      such as an SQL DDL statement
     * @param columnIndexes an array of column indexes indicating the columns
     *                      that should be returned from the inserted row
     * @return either the row count for <code>INSERT</code>, <code>UPDATE</code>,
     *         or <code>DELETE</code> statements, or 0 for SQL statements
     *         that return nothing
     * @throws java.sql.SQLException if a database access error occurs, the SQL
     *                               statement returns a <code>ResultSet</code> object, or the
     *                               second argument supplied to this method is not an <code>int</code> array
     *                               whose elements are valid column indexes
     * @since 1.4
     */
    public int executeUpdate(final String sql, int columnIndexes[]) throws SQLException {
        return jdbcStatement.executeUpdate(sql, columnIndexes);
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.  The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement.
     *
     * @param sql         an SQL <code>INSERT</code>, <code>UPDATE</code> or
     *                    <code>DELETE</code> statement or an SQL statement that returns nothing
     * @param columnNames an array of the names of the columns that should be
     *                    returned from the inserted row
     * @return either the row count for <code>INSERT</code>, <code>UPDATE</code>,
     *         or <code>DELETE</code> statements, or 0 for SQL statements
     *         that return nothing
     * @throws java.sql.SQLException if a database access error occurs, the SQL
     *                               statement returns a <code>ResultSet</code> object, or the
     *                               second argument supplied to this method is not a <code>String</code> array
     *                               whose elements are valid column names
     * @since 1.4
     */
    public int executeUpdate(final String sql, String columnNames[]) throws SQLException {
        return jdbcStatement.executeUpdate(sql, columnNames);
    }

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that any
     * auto-generated keys should be made available
     * for retrieval.  The driver will ignore this signal if the SQL statement
     * is not an <code>INSERT</code> statement.
     * <p/>
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p/>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *
     * @param sql               any SQL statement
     * @param autoGeneratedKeys a constant indicating whether auto-generated
     *                          keys should be made available for retrieval using the method
     *                          <code>getGeneratedKeys</code>; one of the following constants:
     *                          <code>Statement.RETURN_GENERATED_KEYS</code> or
     *                          <code>Statement.NO_GENERATED_KEYS</code>
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there are
     *         no results
     * @throws java.sql.SQLException if a database access error occurs or the second
     *                               parameter supplied to this method is not
     *                               <code>Statement.RETURN_GENERATED_KEYS</code> or
     *                               <code>Statement.NO_GENERATED_KEYS</code>.
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @see #getGeneratedKeys
     * @since 1.4
     */
    public boolean execute(final String sql, int autoGeneratedKeys) throws SQLException {
        return jdbcStatement.execute(sql, autoGeneratedKeys);
    }

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.  This array contains the indexes of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the given SQL statement
     * is not an <code>INSERT</code> statement.
     * <p/>
     * Under some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p/>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *
     * @param sql           any SQL statement
     * @param columnIndexes an array of the indexes of the columns in the
     *                      inserted row that should be  made available for retrieval by a
     *                      call to the method <code>getGeneratedKeys</code>
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there
     *         are no results
     * @throws java.sql.SQLException if a database access error occurs or the
     *                               elements in the <code>int</code> array passed to this method
     *                               are not valid column indexes
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @since 1.4
     */
    public boolean execute(final String sql, int columnIndexes[]) throws SQLException {
        return jdbcStatement.execute(sql, columnIndexes);
    }

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval. This array contains the names of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the given SQL statement
     * is not an <code>INSERT</code> statement.
     * <p/>
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p/>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *
     * @param sql         any SQL statement
     * @param columnNames an array of the names of the columns in the inserted
     *                    row that should be made available for retrieval by a call to the
     *                    method <code>getGeneratedKeys</code>
     * @return <code>true</code> if the next result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there
     *         are no more results
     * @throws java.sql.SQLException if a database access error occurs or the
     *                               elements of the <code>String</code> array passed to this
     *                               method are not valid column names
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @see #getGeneratedKeys
     * @since 1.4
     */
    public boolean execute(final String sql, String columnNames[]) throws SQLException {
        return jdbcStatement.execute(sql, columnNames);
    }

    /**
     * Retrieves the result set holdability for <code>ResultSet</code> objects
     * generated by this <code>Statement</code> object.
     *
     * @return either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @throws java.sql.SQLException if a database access error occurs
     * @since 1.4
     */
    public int getResultSetHoldability() throws SQLException {
        return jdbcStatement.getResultSetHoldability();
    }

    public void reclaimStatement() throws SQLException {
        markForReclaim(true);
        close();
    }

    public void markForReclaim(boolean reclaimStatus) {
        markedForReclaim = reclaimStatus;
    }

    public boolean isMarkedForReclaim() {
        return markedForReclaim;
    }

    public void closeOnCompletion() throws SQLException {
        if (leakDetector != null) {
            _logger.log(Level.INFO, "jdbc.invalid_operation.close_on_completion");
            throw new UnsupportedOperationException("Not supported yet.");
        }
        if (DataSourceObjectBuilder.isJDBC41()) {
            closeOnCompletion = true;
            return;
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }

    public void actualCloseOnCompletion() throws SQLException {
        try {
            executor.invokeMethod(jdbcStatement, "closeOnCompletion", null);
        } catch (ResourceException ex) {
            _logger.log(Level.SEVERE, "jdbc.ex_stmt_wrapper", ex);
            throw new SQLException(ex);
        }
        return;
    }

    public boolean isCloseOnCompletion() throws SQLException {
        if (DataSourceObjectBuilder.isJDBC41()) {
            try {
                return (Boolean) executor.invokeMethod(jdbcStatement,
                        "isCloseOnCompletion", null);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "jdbc.ex_stmt_wrapper", ex);
                throw new SQLException(ex);
            }
        }
        throw new UnsupportedOperationException("Operation not supported in this runtime.");
    }

    public boolean getCloseOnCompletion() {
        return closeOnCompletion;
    }

    public void incrementResultSetCount() {
        resultSetCount.incrementAndGet();
    }

    public void decrementResultSetCount() {
        resultSetCount.decrementAndGet();
    }

    public int getResultSetCount() {
        return resultSetCount.get();
    }
}
