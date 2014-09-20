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

/*
 * ConnectionManager.java
 *
 * Create on March 3, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.connection;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.api.persistence.support.Transaction;
import com.sun.jdo.spi.persistence.utility.DoubleLinkedList;
import com.sun.jdo.spi.persistence.utility.Linkable;
import com.sun.jdo.spi.persistence.support.sqlstore.utility.StringScanner;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperSQLStore;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.ResourceBundle;

/**
 * <P>This class represents a connection manager, which creates a
 * JDBC driver manager and manages
 * established database connections. This class lets you specify the following
 * settings for JDBC connections:
 * <UL>
 * <LI>JDBC driver type
 * <LI>data source name (JDBC URL)
 * <LI>user name
 * <LI>password
 * <LI>number of pooled connections (settable when running)
 * <LI>how often to try to get a connection after failing the first time and how long
 * to retry (settable when running)
 * </UL>
 * If you define a connection manager as a component, you can define
 * these settings when you partition the application instead of when you write the application.
 * <P>You can change only the following settings when the connection manager is
 * running:
 * <UL>
 * <LI> minimum and maximum number of pooled connections, although not whether pooling is
 * on or off (<code>setMinPool</code> and <code>setMaxPool</code>)
 * <LI> how often to try to get a connection after failing the first time and how
 * long to retry for a connection (<code>setMsWait</code> and <code>setMsInterval</code>)
 * </UL>
 * <P>You cannot set any other setting while the connection manager is running.
 * To change other settings, shut down the connection manager using the
 * <code>shutDown</code> method, then change
 * the settings. Then, start the connection manager
 * using the <code>startUp</code> method.
 * <P>If you use a connection manager to manage your database connections,
 * the connection manager can also extend a SynerJ transaction to include
 * JDBC database transactions. In other words, if the JDBC database
 * transactions occur within a SynerJ transaction, the JDBC database
 * transactions are committed or rolled back when the SynerJ transaction
 * committed or rolled back.
 * <P>You can set up your connection manager to manage database connections in
 * the following ways:
 * <UL>
 * <LI>Start a new connection every time you request a connection. This approach is
 * often referred to as <i>client-based security</i>, because the security
 * for the connection is based on a particular database client and can be
 * different for each client.
 * <LI>Maintain a pool of connections for a given user name and password,
 * and return one of these connections when you request a connection using the
 * getConnection method with no parameters.
 * This approach is often referred to as <i>application-based security</i>, because
 * the security information for the pool of connections is the same and is the
 * same for all clients of the application.
 * <LI>Maintain a pool of connections for a given user name and password
 * and start a new connection if you specify another user name and password
 * with the getConnection method.
 * This approach is a blend of client-based and application-based security.
 * </UL>
 * <P>You also have the choice of either defining the connection manager
 * as a service object typed as a ConnectionManager object, or
 * defining the connection manager by dynamically creating a
 * ConnectionManager object in your code. If you define the
 * connection manager as a service object, the SynerJ partitioning system
 * can help you determine how to define partitions that contain the
 * service object by being aware of where JDBC drivers are installed,
 * for example. If you define the ConnectionManager object
 * dynamically, then you need to keep such issues in mind when you
 * define the partitions whose code defines the ConnectionManager
 * objects; the partitioning system will not help you.
 *
 * <H4>Starting a New Database Connection for Each Request (Client-based Security)</H4>
 * <P>In this situation, the connection
 * manager establishes a new connection each time you request a connection.
 * By default, the connection manager does not establish and maintain a
 * pool of connections. In this case, you can leave the maximum and minimum
 * number of pooled connections set to 0.
 * Each time you need a connection to the database,
 * use the getConnection method.
 * You can use the default user name, password, and database URL for the current
 * connection manager by invoking the getConnection method with no parameters.
 * The default settings are specified in one of the ConnectionManager
 * constructors when you create the connection manager.
 * You can also specify a user name, password, and database URL on the
 * getConnection method.
 *
 * <H5>Example of Using ConnectionManager without Pooling</H4>
 * <PRE>
 * import java.sql.*;
 * import com.sun.jdo.api.persistence.support.*;
 * Connection con = myTransaction.getConnection();
 * Statement stmt = con.createStatement();
 * ResultSet rs = stmt.executeQuery("SELECT * FROM T1");
 * </PRE>
 *
 * <H4>Using a Pool of Database Connections (Application-based Security)</H4>
 * <P>When you create a connection manager using the
 * ConnectionManager class, you can have the connection manager
 * establish a pool of connections for a given user name and password
 * to allow a thread to using an existing connection instead of waiting
 * for a new database connection to be created.
 * <P>To create a connection manager with a pool of connections using
 * a service object:
 * <OL>
 * <LI>Define a service object of the class ConnectionManager.
 * <LI>In the Partition Workshop, set the component properties for
 * the component instance represented by the service
 * object to define the driver name and the default database URL,
 * user name, and password. You can also define the values for the
 * minimum connections and the maximum connections for the connection pool, as well
 * as for how often and how long to try to get a connection after an initial
 * failure to do so.
 * <P>If you set the values for the minimum and maximum connections to
 * 0, then no pool of connections is established. Any value greater
 * than 0 means that a pool of connections is established. The maximum
 * value must be equal to or greater than the value of the minimum
 * value. If you set the maximum and minimum as equal values, the
 * number of connections established for the pool will be constant.
 * <P>The connection manager establishes the minimum number of connections
 * when it starts up using the default database URL, user name, and password.
 * </OL>
 * <P>To get one of the pooled connections, you can use the
 * <CODE>getConnection</CODE>
 * method with no parameters. If all the existing connections are in
 * use, the connection manager establishes another connection with
 * the same database URL, user name, and password and adds it to the pool,
 * up to the specified maximum number of connections.
 * <P>When you have finished using a connection, you can use the
 * <CODE>close()</CODE> method to return the connection to the pool
 * of available connections.
 * The connection manager periodically checks its pool of connections,
 * and can reduce the number of established connections to the minimum
 * number, if enough connections are not in use.
 * At runtime, you can change the maximum and minimum number of connections,
 * because the ConnectionManager
 * is a component.
 *
 * <H4>Using a Pool of Connections and Starting New Connections</H4>
 * <P>You can have the connection manager establish and maintain a
 * pool of connections and have the connection manager establish new
 * connections on a request-by-request basis.
 *
 * <H5>Example of Using ConnectionManager with Pooling</H5>
 * <PRE>
 * import com.sun.jdo.api.persistence.support.*;
 * void getT1Data() {
 * 	// The following connection is from the connection pool.
 * 	Connection myConn = myTransaction.getConnection();
 * 	Statement myStatement = myConn.createStatement();
 * 	ResultSet myResults = myStatement.executeQuery(
 * 		"SELECT * FROM T1);
 * 	// Free the connection; it is returned to the connection pool.
 * 	myConn.close();
 *
 * 	// The connection manager creates a new connection for the
 * 	// following request.
 * 	Connection yourConn = myConnMgr.getConnection(
 * 		"data:oracle:thin:@CUSTOMERDB:1521:ORCL", "paul", "omni8");
 * 	Statement yourStatement = yourConn.createStatement();
 * 	ResultSet yourResults = yourStatement.executeQuery(
 * 	"SELECT Customer, Date, Amount FROM Orders");
 * 	.
 * 	.
 * 	.
 * }
 * </PRE>
 */
public class ConnectionManager {
    /**
     * Name of the driver; e.g. "oracle.jdbc.driver.OracleDriver"
     * @serial
     */
    private String driverName;

    /**
     * Expanded name of the driver
     * @serial
     */
    private transient String expandedDriverName;

    /**
     * Datasource url; e.g. "jdbc:oracle:oci7:@ABYSS_ORACLE"
     * @serial
     */
    private String url;

    /**
     * Expanded datasource url
     * @serial
     */
    private transient String expandedUrl;

    /**
     * DBMS Username.
     * @serial
     */
    private String userName;

    /**
     * Expanded user name
     * @serial
     */
    private transient String expandedUserName;

    /**
     * DBMS password.
     * @serial
     */
    private String password;

    /**
     * Expanded DBMS password.
     * @serial
     */
    private transient String expandedPassword;

    /**
     * The minimum size of the connection pool.
     * @serial
     */
    private int minPool;

    /**
     * The maximum size of the connection pool.
     * @serial
     */
    private int maxPool;

    /**
     * The current size of the connection pool.
     * @serial
     */
    private transient int poolSize;

    /**
     * True if connection pooling is enabled.
     * @serial
     */
    private transient boolean pooling;

    /**
     * The linked list of idle DB connections.
     * @serial
     */
    transient DoubleLinkedList freeList;

    /**
     * The linked list of in-use DB connections.
     * @serial
     */
    transient DoubleLinkedList busyList;

    /**
     * List of Connections associated with transactions, indexed by
     * transaction object (javax.transaction.Transaction).
     * @serial
     */
    private transient Hashtable xactConnections;

    /**
     * Flag that a shutdown to this ConnectionManager object is pending.
     * @serial
     */
    transient boolean shutDownPending;

    /**
     * Flag that specifies we are using default connection blocking.
     * @serial
     */
    private transient boolean connectionBlocking;

    /**
     * Millisecond time to wait between attempts to connect
     * to the database.
     * @serial
     */
    private int msInterval;

    /**
     * Millisecond time to block while attempting to connect
     * to the database.
     * @serial
     */
    private int msWait;

    //
    // Default number of milliseconds to block while retrying to get
    // a pooled connection to the database.
    //
    private static final int DEFAULT_RETRY_INTERVAL = 1000;

    /**
     * Indicates whether this ConnectionManager is properly initialized.
     * @serial
     */
    private transient boolean initialized;

    /**
     * Maximumn number of seconds this DataSource will wait while attempting to
     * connection to a database.
     */
    private int loginTimeout;

    /**
     * Free non-pooled connection to reduce time for getting a new connection.
     */
    private transient ConnectionImpl freeConn = null;

    
    /**
     * The logger
     */
    private static Logger logger = LogHelperSQLStore.getLogger();
    
    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            ConnectionManager.class.getClassLoader());


    //
    // SQL92 State Codes.
    //

    //
    // SQL92 "00000" Successful completion.
    //
    static final String SQL_SUCCESS = "00000"; // NOI18N

    //
    // SQL92 "01000" Warning.
    //
    static final String SQL_WARNING = "01000"; // NOI18N

    //
    // SQL92 "01001" Warning; cursor operation conflict.
    //
    static final String SQL_CURSOR_OP = "01001"; // NOI18N

    //
    // SQL92 "01002" Warning; disconnect error.
    //
    static final String SQL_DISCONNECT = "01002"; // NOI18N

    //
    // SQL92 "01003" Warning; null value eliminated in set function.
    //
    static final String SQL_NULL_ELIM = "01003"; // NOI18N

    //
    // SQL92 "01004" Warning; string date, right truncation.
    //
    static final String SQL_R_TRUNC = "01004"; // NOI18N

    //
    // SQL92 "01005" Warning; insufficient item descriptor areas.
    //
    static final String SQL_INSUFF_ITEM = "01005"; // NOI18N

    //
    // SQL92 "01006" Warning; privilege not revoked.
    //
    static final String SQL_NOT_REVOKED = "01006"; // NOI18N

    //
    // SQL92 "01007" Warning; privilege not granted.
    //
    static final String SQL_NOT_GRANTED = "01007"; // NOI18N

    //
    // SQL92 "01008" Warning; implicit zero-bit padding.
    //
    static final String SQL_ZERO_BIT_PAD = "01008"; // NOI18N

    //
    // SQL92 "01009" Warning; search condition too long for
    // information schema.
    //
    static final String SQL_COND_TOO_LONG = "01009"; // NOI18N

    //
    // SQL92 "0100A" Warning; query condition too long for
    // information schema.
    //
    static final String SQL_QUERY_TOO_LONG = "0100A"; // NOI18N

    //
    // SQL92 "02000" No data.
    //
    static final String SQL_NO_DATA = "02000"; // NOI18N

    //
    // SQL92 "07000" Dynamic SQL error.
    //
    static final String SQL_DYN_ERROR = "07000"; // NOI18N

    //
    // SQL92 "07001" Dynamic SQL error; using clause does not
    // match dynamic parameter specifications.
    //
    static final String SQL_USING_NO_PARAM = "07001"; // NOI18N

    //
    // SQL92 "07002" Dynamic SQL error; using clause does not
    // match target specifications.
    //
    static final String SQL_USING_NO_TARGET = "07002"; // NOI18N

    //
    // SQL92 "07003" Dynamic SQL error; cursor specification
    // cannot be executed.
    //
    static final String SQL_CURSOR_NOEXE = "07003"; // NOI18N

    //
    // SQL92 "07004" Dynamic SQL error; using clause
    // required for dynamic parameters.
    //
    static final String SQL_USING_REQ = "07004"; // NOI18N

    //
    // SQL92 "07005" Dynamic SQL error; prepared statement
    // not a cursor specification.
    //
    static final String SQL_PREP_NO_CURSOR = "07005"; // NOI18N

    //
    // SQL92 "07006" Dynamic SQL error; restricted datatype
    // attribute violation.
    //
    static final String SQL_RESTRIC_ATTR = "07006"; // NOI18N

    //
    // SQL92 "07007" Dynamic SQL error; using caluse required
    // for result fields.
    //
    static final String SQL_USING_RESULTS = "07007"; // NOI18N

    //
    // SQL92 "07008" Dynamic SQL error; invalid descriptor count.
    //
    static final String SQL_INVAL_DESC_CNT = "07008"; // NOI18N

    //
    // SQL92 "07009" Dynamic SQL error; invalid descriptor index.
    //
    static final String SQL_INVAL_DESC_IDX = "07009"; // NOI18N

    //
    // SQL92 "08000" Connection exception.
    //
    static final String SQL_CONN = "08000"; // NOI18N

    //
    // SQL92 "08001" Connection exception; SQL-client unable
    // to establish SQL-connection.
    //
    static final String SQL_CLIENT_NO_CONN = "08001"; // NOI18N

    //
    // SQL92 "08002" Connection exception; connection name
    // in use.
    //
    static final String SQL_CONN_IN_USE = "08002"; // NOI18N

    //
    // SQL92 "08003" Connection exception; connection does not exist.
    //
    static final String SQL_NO_CONN = "08003"; // NOI18N

    //
    // SQL92 "08004" Connection exception; SQL-server rejected
    // establishment of SQL-connection.
    //
    static final String SQL_REJECT_CONN = "08004"; // NOI18N

    //
    // SQL92 "08006" Connection exception; connection failure.
    //
    static final String SQL_CONN_FAIL = "08006"; // NOI18N

    //
    // SQL92 "08007" Connection exception; transaction resolution unknown.
    //
    static final String SQL_TRANS_UNK = "08007"; // NOI18N

    //
    // SQL92 "0A000" Feature not supported.
    //
    static final String SQL_NO_SUPPORT = "0A000"; // NOI18N

    //
    // SQL92 "0A001" Feature not supported; multiple
    // server transactions
    //
    static final String SQL_NO_SUPPORT_MULTI = "0A001"; // NOI18N

    //
    // SQL92 "21000" Cardinality violation.
    //
    static final String SQL_INVALID_VALUE = "21000"; // NOI18N

    //
    // SQL92 "22000" Data exception.
    //
    static final String SQL_DATA = "22000"; // NOI18N

    //
    // SQL92 "22001" Data exception; string data,
    // right trunctation.
    //
    static final String SQL_DATA_RTRUNC = "22001"; // NOI18N

    //
    // SQL92 "22002" Data exception; null value, no
    // indicator parameter.
    //
    static final String SQL_DATA_NULL = "22002"; // NOI18N

    //
    // SQL92 "22003" Data exception; numeric value out
    // of range.
    //
    static final String SQL_OUT_OF_RANGE = "22003"; // NOI18N

    //
    // SQL92 "22005" Data exception; error in assignment.
    //
    static final String SQL_DATA_EXCEPT = "22005"; // NOI18N

    //
    // SQL92 "22007" Data exception; invalid datetime format.
    //
    static final String SQL_DATETIME_FMT = "22007"; // NOI18N

    //
    // SQL92 "22008" Data exception; datetime field overflow.
    //
    static final String SQL_DATETIME_OVFLO = "22008"; // NOI18N

    //
    // SQL92 "22009" Data exception; invalid time zone
    // displacement value.
    //
    static final String SQL_TIMEZONE = "22009"; // NOI18N

    //
    // SQL92 "22011" Data exception; substring error.
    //
    static final String SQL_SUBSTR_ERROR = "22011"; // NOI18N

    //
    // SQL92 "22012" Data exception; division by zero.
    //
    static final String SQL_DIV_BY_ZERO = "22012"; // NOI18N

    //
    // SQL92 "22015" Data exception; interval field overflow.
    //
    static final String SQL_INTERVAL_OVFLO = "22015"; // NOI18N

    //
    // SQL92 "22018" Data exception; invalid character value
    // for cast.
    //
    static final String SQL_INVAL_CHAR_CAST = "22018"; // NOI18N

    //
    // SQL92 "22019" Data exception; invalid escape character.
    //
    static final String SQL_INVAL_ESCAPE_CHAR = "22019"; // NOI18N

    //
    // SQL92 "22021" Data exception; character not in repertoire.
    //
    static final String SQL_CHAR_NOT_REP = "22021"; // NOI18N

    //
    // SQL92 "22022" Data exception; indicator overflow.
    //
    static final String SQL_IND_OVERFLOW = "22022"; // NOI18N

    //
    // SQL92 "22023" Data exception; invalid parameter value.
    //
    static final String SQL_INVAL_PARAM_VALUE = "22023"; // NOI18N

    //
    // SQL92 "22024" Data exception; unterminated C string.
    //
    static final String SQL_UNTERM_C_STR = "22024"; // NOI18N

    //
    // SQL92 "22025" Data exception; invalid escape sequence.
    //
    static final String SQL_INVAL_ESCAPE_SEQ = "22025"; // NOI18N

    //
    // SQL92 "22026" Data exception; string data, length mismatch.
    //
    static final String SQL_STR_LEN_MISMATCH = "22026"; // NOI18N

    //
    // SQL92 "22027" Data exception; trim error.
    //
    static final String SQL_TRIM_ERROR = "22027"; // NOI18N

    //
    // SQL92 "23000" Integrity constraint violation.
    //
    static final String SQL_INTEG_CONSTRAINT = "23000"; // NOI18N

    //
    // SQL92 "24000" Invalid cursor state.
    //
    static final String SQL_INVAL_CURSOR_STATE = "24000"; // NOI18N

    //
    // SQL92 "25000" Invalid transaction state
    //
    static final String SQL_INVAL_TRANS_STATE = "25000"; // NOI18N

    //
    // SQL92 "26000" Invalid SQL statement name.
    //
    static final String SQL_INVAL_SQL_NAME = "26000"; // NOI18N

    //
    // SQL92 "28000" Invalid authorization specification.
    //
    static final String SQL_INVAL_AUTH = "28000"; // NOI18N

    //
    // SQL92 "2A000" Syntax error or access rule violation
    // in direct SQL statement.
    //
    static final String SQL_SYNTAX_DIRECT = "2A000"; // NOI18N

    //
    // SQL92 "2B000" Dependent privilege descriptors
    // still exist.
    //
    static final String SQL_DESC_EXIST = "2B000"; // NOI18N

    //
    // SQL92 "2C000" Invalid character set name.
    //
    static final String SQL_INVAL_CHAR_SET = "2C000"; // NOI18N

    //
    // SQL92 "2D000" Invalid transaction termination.
    //
    static final String SQL_INVAL_TRANS_TERM = "2D000"; // NOI18N

    //
    // SQL92 "2E000" Invalid connection name.
    //
    static final String SQL_INVAL_CONN_NAME = "2E000"; // NOI18N

    //
    // SQL92 "33000" Invalid SQL descriptor name
    //
    static final String SQL_INVAL_SQL_DESC_NAME = "33000"; // NOI18N

    //
    // SQL92 "34000" Invalid cursor name.
    //
    static final String SQL_INVAL_CURSOR_NAME = "34000"; // NOI18N

    //
    // SQL92 "35000" Invalid condition number
    //
    static final String SQL_INVAL_COND_NUM = "35000"; // NOI18N

    //
    // SQL92 "37000" Syntax error or access rule violation
    // in dynamic SQL statement.
    //
    static final String SQL_SYNTAX_DYNAMIC = "37000"; // NOI18N

    //
    // SQL92 "3C000" Ambiguous cursor name.
    //
    static final String SQL_AMBIG_CURSOR = "3C000"; // NOI18N

    //
    // SQL92 "3D000" Invalid catalog name.
    //
    static final String SQL_INVAL_CATALOG = "3D000"; // NOI18N

    //
    // SQL92 "3F000" Invalid schema name.
    //
    static final String SQL_INVAL_SCHEMA_NAME = "3F000"; // NOI18N

    //
    // SQL92 "40000" Transaction rollback.
    //
    static final String SQL_TRANS_ROLLBACK = "40000"; // NOI18N

    //
    // SQL92 "40001" Transaction rollback; serialization
    // failure.
    //
    static final String SQL_TRANS_SERIAL_FAIL = "40001"; // NOI18N

    //
    // SQL92 "40002" Transaction rollback; integrity
    // constraint violation.
    //
    static final String SQL_TRANS_INTEG = "40002"; // NOI18N

    //
    // SQL92 "40003" Transaction rollback; statement
    // completion unknown.
    //
    static final String SQL_TRANS_COMP_UNK = "40003"; // NOI18N

    //
    // SQL92 "42000" Syntax error or access rule violation.
    //
    static final String SQL_SYNTAX = "42000"; // NOI18N

    //
    // SQL92 "44000" With check option violation.
    //
    static final String SQL_CHECK_OPT = "44000"; // NOI18N

    //
    // SQL92 "HZ   " Remote Database Access.
    //
    static final String SQL_RMT_DB_ACCESS = "HZ   "; // NOI18N


    /**
     * Default constructor.
     * Creates a new connection manager and loads the generic JDBC driver.
     * You should typically use one of the other constructors, because
     * you cannot change JDBC drivers after the connection manager has
     * been created.
     */
    public ConnectionManager() {
        super();
        this.driverName = null;
        this.expandedDriverName = null;
        this.url = null;
        this.expandedUrl = null;
        this.userName = null;
        this.expandedUserName = null;
        this.password = null;
        this.expandedPassword = null;
        this.minPool = 0;
        this.maxPool = 0;
        this.busyList = null;
        this.freeList = null;
        this.poolSize = 0;
        this.pooling = false;
        this.xactConnections = null;
        this.shutDownPending = false;
        this.connectionBlocking = false;
        this.msWait = 0;
        this.msInterval = 0;
        this.busyList = null;
        this.xactConnections = null;
        this.initialized = false;
    }

    // --------------- Overloaded Constructors -----------------

    /**
     * Creates a new connection manager and loads the named
     * JDBC driver.
     * <p>
     * @param      driverName  name of JDBC driver.
     * @exception  ClassNotFoundException if the driver cannot be found.
     * @exception  SQLException  if a SQL error is encountered.
     */
    public ConnectionManager(String driverName)
            throws ClassNotFoundException, SQLException {
        this();
        try {
            this.setDriverName(driverName);
            startUp();
        } catch (SQLException se) {
            throw se;
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }

    /**
     * Creates a new connection manager, loads the named JDBC driver, and
     * sets the default database URL for the new connection manager.
     * <p>
     * @param      driverName  the name of JDBC driver.
     * @param      url         the database URL for the data source.
     * @exception  ClassNotFoundException if the driver cannot be found.
     * @exception  SQLException  if a SQL error is encountered.
     */
    public ConnectionManager(String driverName, String url)
            throws ClassNotFoundException, SQLException {
        this();
        try {
            this.setDriverName(driverName);
            this.setURL(url);
            startUp();
        } catch (SQLException se) {
            throw se;
        }
    }

    /**
     * Creates a new connection manager, loads the named JDBC driver, and
     * sets the default database URL and user name for the new
     * connection manager.
     * @param      driverName  the name of JDBC driver.
     * @param      url         the default database URL for the data source.
     * @param      userName    the default user name for database connections.
     * @exception  ClassNotFoundException if the driver cannot be found.
     * @exception  SQLException  if a SQL error is encountered.
     */
    public ConnectionManager(String driverName, String url,
                             String userName) throws ClassNotFoundException, SQLException {
        this();
        try {
            this.setDriverName(driverName);
            this.setURL(url);
            this.setUserName(userName);
            startUp();
        } catch (SQLException se) {
            throw se;
        }
    }

    /**
     * Creates a new connection manager, loads the named JDBC driver, and
     * sets the default database URL, user name, and password for the new
     * connection manager.
     * @param	driverName  the name of JDBC driver.
     * @param   url         the default database URL for the data source.
     * @param   userName    the default user name for database connections.
     * @param	password		the default password for database connections.
     * @exception  ClassNotFoundException if the driver cannot be found.
     * @exception  SQLException  if a SQL error is encountered.
     */
    public ConnectionManager
            (
            String driverName,
            String url,
            String userName,
            String password
            ) throws ClassNotFoundException, SQLException {
        this();
        try {
            this.setDriverName(driverName);
            this.setURL(url);
            this.setUserName(userName);
            this.setPassword(password);
            startUp();
        } catch (SQLException se) {
            throw se;
        }
    }

    /**
     * Creates a new connection manager, loads the named JDBC driver, and sets
     * the default database URL, user name, password and minimum and maximum
     * connection pool sizes for the new
     * connection manager.
     * <P>If minPool and maxPool are 0, connection pooling is disabled.
     * If minPool is greater than 0 and maxPool is greater than or equal
     * to minPool, this constructor creates a connection pool containing
     * minPool connections.
     * @param	driverName	the name of JDBC driver.
     * @param   url			the default database URL for the data source.
     * @param   userName	the default user name for database connections.
     * @param	password	the default password for database connections.
     * @param	minPool		the default minimum size of the connection pool.
     * @param	maxPool		the default maximum size of the connection pool.
     * @exception  ClassNotFoundException if the driver cannot be found.
     * @exception  SQLException  if a SQL error is encountered or if the
     * specified  value of minPool is not less than or equal to the specified
     * value of maxPool.
     */
    public ConnectionManager
            (
            String driverName,
            String url,
            String userName,
            String password,
            int minPool,
            int maxPool
            ) throws ClassNotFoundException, SQLException {
        this();
        try {
            this.setDriverName(driverName);
            this.setURL(url);
            this.setUserName(userName);
            this.setPassword(password);
            this.setMinPool(minPool);
            this.setMaxPool(maxPool);
            startUp();
        } catch (SQLException se) {
            throw se;
        }
    }

    /**
     * Creates a new connection manager, loads the named JDBC driver, and defines
     * the default values for the database URL, user name, password, minimum and maximum
     * connection pool sizes, and the length of time to wait for a
     * database connection.
     * <P>If minPool and maxPool are 0, connection pooling is disabled.
     * If minPool is greater than 0 and maxPool is greater than or equal
     * to minPool, this constructor creates a connection pool containing
     * minPool connections.
     * <P>If msWait is set to 0, the connection manager does not try again to
     * create or return a database connection if the first try fails.
     * For any other value, the connection manager waits 1000 milliseconds (ms)
     * (1 second) before trying again.
     * If the msWait value is less than 1000 ms, the connection manager
     * waits 1000 ms before trying.
     * The connection manager continues trying until the value specified
     * by msWait is met or exceeded.
     * <P>If you want to set the interval length yourself, you can use
     * the <code>ConnectionManager</code> constructor that
     * specifies the msInterval parameter or the <code>setInterval</code>
     * method.
     * @param	driverName	the name of JDBC driver.
     * @param   url			the default database URL for the data source.
     * @param   userName	the default user name for database connections.
     * @param	password	the default password for database connections.
     * @param	minPool		the default minimum size of the connection pool.
     * @param	maxPool		the default maximum size of the connection pool.
     * @param      msWait   the total number of milliseconds to wait for a successful connection.
     * @exception  ClassNotFoundException if the driver cannot be found.
     * @exception  SQLException  if a SQL error is encountered or if the
     * specified  value of minPool is not less than or equal to the specified
     * value of maxPool.
     */
    public ConnectionManager
            (
            String driverName,
            String url,
            String userName,
            String password,
            int minPool,
            int maxPool,
            int msWait
            ) throws ClassNotFoundException, SQLException {
        this();
        try {
            this.setDriverName(driverName);
            this.setURL(url);
            this.setUserName(userName);
            this.setPassword(password);
            this.setMinPool(minPool);
            this.setMaxPool(maxPool);
            this.setMsWait(msWait);
            this.setMsInterval(DEFAULT_RETRY_INTERVAL);
            startUp();
        } catch (SQLException se) {
            throw se;
        }
    }

    /**
     * Creates a new connection manager, loads the named JDBC driver, and
     * defines the default values for the database URL, user name, password, minimum and maximum
     * connection pool sizes, the length of time to wait for a database connection,
     * and how frequently to try again to get a database connection.
     * <P>If minPool and maxPool are 0, connection pooling is disabled.
     * If minPool is greater than 0 and maxPool is greater than or equal
     * to minPool, this constructor creates a connection pool containing
     * minPool connections.
     * <P>If msWait or msInterval is set to 0, the connection
     * manager does not try again to create or return a database connection
     * if the first try fails.
     * <P>For any other values greater than 0, the The connection manager
     * continues trying after every specified value for msInterval
     * until the value specified by msWait is met or exceeded.
     * If the value for msInterval is greater than the value for msWait,
     * the connection manager tries again to return a connection once, then
     * fails if it could get a connection.
     * @param	driverName	the name of JDBC driver.
     * @param   url			the default database URL for the data source.
     * @param   userName	the default user name for database connections.
     * @param	password	the default password for database connections.
     * @param	minPool		the default minimum size of the connection pool.
     * @param	maxPool		the default maximum size of the connection pool.
     * @param      msWait   the total number of milliseconds to wait to get a connection.
     * @param msInterval the number of milliseconds to wait before trying again to get a connection.
     * @exception  ClassNotFoundException if the driver cannot be found.
     * @exception  SQLException  if a SQL error is encountered or if the
     * specified  value of minPool is not less than or equal to the specified
     * value of maxPool.
     */

    public ConnectionManager
            (
            String driverName,
            String url,
            String userName,
            String password,
            int minPool,
            int maxPool,
            int msWait,
            int msInterval
            ) throws ClassNotFoundException, SQLException {
        this();
        try {
            this.setDriverName(driverName);
            this.setURL(url);
            this.setUserName(userName);
            this.setPassword(password);
            this.setMinPool(minPool);
            this.setMaxPool(maxPool);
            this.setMsWait(msWait);
            this.setMsInterval(msInterval);
            startUp();
        } catch (SQLException se) {
            throw se;
        }
    }

    // ---------------------  Public Methods -----------------------


    /**
     * Establishes a connection to the default database URL
     * using the default user name and password.
     * <P>If the current connection manager maintains a
     * connection pool, this method returns a pooled connection
     * instead of establishing a new connection.
     * If all pooled connections are in use, and the total wait time (msWait)
     * for the connection manager and the retry interval (msInterval) are
     * not 0, then the connection manager tries to get a database connection
     * after the retry interval. The connection manager continues to
     * try until a pooled connection becomes available or
     * until the total time equals or exceeds the wait time.
     * If the wait time expires before the connection manager returns
     * a database connection, this method throws a <code>SQLException</code>
     * exception with SQLState = "08006".
     *
     * <P>If the current connection manager is not set to try again for connections
     * (the wait time is 0) and no pooled connections are available, this
     * method throws a <code>SQLException</code> exception
     * with SQLState = "08006".
     *
     * <P>If the current connection manager is being shut down,
     * this method throws a <code>SQLException</code> exception with
     * SQLState = "08003".
     *
     * @return      A new or pooled database connection.
     * @exception	SQLException	if no database connection is available.
     *
     */
    public synchronized Connection getConnection() throws SQLException {
        if (this.shutDownPending == true) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.isdown") // NOI18N
                    ),
                            SQL_NO_CONN
                    );
            throw se;
        }

        ConnectionImpl conn = this.checkXact();

        if (conn != null) {
            // We already know about this transaction.
        } else if (!this.pooling)					// Get a non-pooled connection.
        {
            conn = (ConnectionImpl) this.getConnection(this.userName,
                    this.password);
            conn.setPooled(false);
            conn.checkXact();
        } else	// This is a pooled connection.
        {
            if (this.freeList.size <= 0)			// Is pool empty?
            {
                if (this.poolSize < this.maxPool)	// Can we expand the pool?
                {
                    try {
                        this.expandPool(1); // Add new connection to the pool.
                    } catch (SQLException se) {
                        throw se;
                    }
                } else if (this.connectionBlocking != true)	// Can't expand the pool.
                {
                    // If not blocking, give up.
                    SQLException se = new SQLException
                            (
                                    StringScanner.createParamString
                            (
                                    I18NHelper.getMessage(messages,
                                            "connection.connectionmanager.maxpool") // NOI18N
                            ),
                                    SQL_INVAL_PARAM_VALUE // 22023
                            );
                    throw se;
                } else								// We are blocking, so...
                {
                    try {
                        this.waitForConnection();	// wait for a connection.
                    } catch (SQLException se) {
                        throw se;
                    }
                }
            }
            conn = (ConnectionImpl) (this.freeList.removeFromHead());
            if (conn == null) {
                // Shouldn't happen.
                SQLException se = new SQLException
                        (
                                StringScanner.createParamString
                        (
                                I18NHelper.getMessage(messages,
                                        "connection.connectionmanager.badvalue") // NOI18N
                        ),
                                SQL_INVAL_PARAM_VALUE	// 22023
                        );
                throw se;
            }
            conn.setPooled(true);
            conn.checkXact();
            this.busyList.insertAtTail((Linkable) conn);
        }
        conn.setFreePending(false);
        return ((Connection) conn);
    }

    /**
     * Establishes a connection to the database at the default database URL using
     * the specified user name and password.
     *
     * @param       userName the database user name.
     * @param       password the database password.
     * @return      A new database connection.
     * @exception  SQLException  if the connection fails.
     */
    public synchronized Connection getConnection
            (
            String userName,
            String password
            ) throws SQLException {
        boolean debug = logger.isLoggable(Logger.FINEST);


        if (this.shutDownPending == true) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.isdown") // NOI18N
                    ),
                            SQL_CONN_FAIL
                    );
            throw se;
        }

        ConnectionImpl conn = this.checkXact();

        if (conn == null) {
            if (freeConn != null) {
                // We have one available - use it
                if (debug) {
                    logger.finest("sqlstore.connection.conncectiomgr.found",freeConn); // NOI18N
                }
                conn = freeConn;
                freeConn = null;
            } else {
                // No connection is available - get new
                try {
                    // By default, a new ConnectionImpl is non-pooled.
                    conn = new ConnectionImpl
                            (
                                    DriverManager.getConnection
                            (
                                    this.expandedUrl,
                                    this.expandAttribute(userName),
                                    this.expandAttribute(password)
                            ),
                                    this.expandedUrl,
                                    this.expandAttribute(userName),
                                    this
                            );
                    if (debug) {
                        logger.finest("sqlstore.connection.conncectiomgr.getnewconn",conn); // NOI18N
                    }
                } catch (SQLException se) {
                    throw se;
                }
            }
            conn.checkXact();
        } else {
            if (!conn.getUserName().equals(this.expandAttribute(userName))) {
                SQLException se = new SQLException
                        (
                                StringScanner.createParamString
                        (
                                I18NHelper.getMessage(messages,
                                        "connection.connectionmanager.getconnection.mismatch") // NOI18N
                        ),
                                SQL_NO_CONN		// 08003
                        );
                throw se;
            }
        }
        conn.setFreePending(false);
        conn.setPooled(false);
        this.busyList.insertAtTail((Linkable) conn);
        return ((Connection) conn);
    }

    /**
     * Establishes a connection to the specified
     * database URL using the specified user name and password.
     *
     * @param       url      the database URL for the database.
     * @param       userName the database user name.
     * @param       password the database password.
     * @return      A new database connection.
     * @exception  SQLException  if the connection fails.
     */
    public synchronized Connection getConnection
            (
            String url,
            String userName,
            String password
            ) throws SQLException {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (this.shutDownPending == true) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.isdown") // NOI18N
                    ),
                            SQL_CONN_FAIL
                    );
            throw se;
        }

        ConnectionImpl conn = this.checkXact();

        if (conn == null) {
            if (freeConn != null) {
                // We have one available - use it
                if (debug) {
                    logger.finest("sqlstore.connection.conncectiomgr.found",freeConn); // NOI18N
                }
                conn = freeConn;
                freeConn = null;
            } else {
                // No connection is available - get new
                try {
                    // By default, a new ConnectionImpl is non-pooled.
                    conn = new ConnectionImpl
                            (
                                    DriverManager.getConnection
                            (
                                    this.expandAttribute(url),
                                    this.expandAttribute(userName),
                                    this.expandAttribute(password)
                            ),
                                    this.expandAttribute(url),
                                    this.expandAttribute(userName),
                                    this
                            );
                    if (debug) {
                        logger.finest("sqlstore.connection.conncectiomgr.getnewconn",conn); // NOI18N
                    }
                } catch (SQLException se) {
                    throw se;
                }
            }
            conn.checkXact();
        } else {
            if ((!conn.getURL().equals(this.expandAttribute(url))) ||
                    (!conn.getUserName().equals(this.expandAttribute(userName)))) {
                SQLException se = new SQLException
                        (
                                StringScanner.createParamString
                        (
                                I18NHelper.getMessage(messages,
                                        "connection.connectionmanager.getconnection.mismatch") // NOI18N
                        ),
                                SQL_NO_CONN	// 08003
                        );
                throw se;
            }
        }
        conn.setFreePending(false);
        conn.setPooled(false);
        this.busyList.insertAtTail((Linkable) conn);
        return ((Connection) conn);
    }

    /**
     * Check whether a ConnectionImpl is already associated with
     * the transaction on the current thread.
     * <p>
     * @return  ConnectionImpl associated with the transaction on
     *          the current thread; null otherwise.
     */
    private synchronized ConnectionImpl checkXact() {
        Transaction tran = null;

        /* RESOLVE: Need to reimplement this???
		try
		{
			// Is this ForteJDBCConnet participating in a transaction?
			tran = ThreadContext.transactionContext().getTransaction();
		}
		catch (SystemException ex)
		{
			// There is no transaction.
			return null;
		}
		*/

        // Return Connection associated with this transaction - maybe null?
        if (tran == null)
            return null;
        return (ConnectionImpl) this.xactConnections.get(tran);
    }

    /**
     * Starts up this ConnectionManager by loading the proper
     * JDBC driver class and initializing the pool if necessary.
     * <p>
     * You need to call this method if you are using the ConnectionManager
     * as a component, or if you use the default constructor and set the
     * attributes via the <code>set</code><I>XXX</I> methods.
     * @exception  ClassNotFoundException if the driver cannot be found.
     * @exception  SQLException  if a SQL error is encountered.
     */
    public void startUp() throws ClassNotFoundException, SQLException {
        if (this.initialized == true) return;

        this.busyList = new DoubleLinkedList();
        this.xactConnections = new Hashtable();
        this.expandedDriverName = this.expandAttribute(this.driverName);
        if (this.expandedDriverName == null) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.nulldriver") // NOI18N
                    ),
                            SQL_INVALID_VALUE // 21000
                    );
            throw se;
        }
        this.expandedUrl = this.expandAttribute(this.url);
        if (this.expandedUrl == null) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.nullurl") // NOI18N
                    ),
                            SQL_INVALID_VALUE // 21000
                    );
            throw se;
        }
        this.expandedUserName = this.expandAttribute(this.userName);
        if (this.expandedUserName == null) {
            this.expandedUserName = ""; // Allow null username. // NOI18N
        }
        this.expandedPassword = this.expandAttribute(this.password);
        if (this.expandedPassword == null) {
            this.expandedPassword = ""; // Allow null password. // NOI18N
        }
        try {
            Class.forName(this.expandedDriverName);

            // Check if connection pooling is requested.
            if ((this.minPool > 0) && (this.maxPool >= this.minPool)) {
                // Yes, create a connection of minPool size.
                this.pooling = true;
                this.freeList = new DoubleLinkedList();
                expandPool(this.minPool);
            } else if ((this.minPool == 0) && (this.maxPool == 0)) {
                // No, pooling is to be disabled.
                this.pooling = false;
            }

        } catch (SQLException se) {
            throw se;
        } catch (ClassNotFoundException e) {
            throw e;
        }
        this.initialized = true;
    }

    /**
     * Disconnects all free database connections managed by
     * the current connection manager and sets the shutDownPending
     * flag to true.
     * All busy connections that are not participating
     * in a transaction will be closed when a yieldConnection() is
     * performed.  If a connection is participating in a transaction,
     * the connection will be closed after the transaction is commited
     * or rolledback.
     *
     */
    public synchronized void shutDown() throws SQLException {
        this.shutDownPending = true;

        if (this.pooling == true) {
            ConnectionImpl conn;
            this.connectionBlocking = false;
            this.pooling = false;
            this.initialized = false;
            for
                    (
                    conn = (ConnectionImpl) this.freeList.getHead();
                    conn != null;
                    conn = (ConnectionImpl) conn.getNext()
                    ) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw e;
                }
            }
            this.freeList = null;
        }
    }


    /**
     * Disconnects all free database connections managed by
     * the current connection manager and sets the shutDownPending
     * flag to true.
     * All busy connections that are not participating
     * in a transaction will be closed when a yieldConnection() is
     * performed.  If a connection is participating in a transaction,
     * the connection will be closed after the transaction is commited
     * or rolledback.
     *
     */
    protected void finalize() {
        try {
            shutDown();
        } catch (SQLException se) {
            ; // Ignore it.
        }
    }

    // ----------- Public Methods to get and set properties --------------

    /**
     * Gets the name of the JDBC driver.
     * @return Name of the JDBC driver.
     * @see #setDriverName
     */
    public synchronized String getDriverName() {
        return (this.driverName);
    }


    /**
     * Sets the name of the JDBC driver.
     * @param driverName the name of the JDBC driver.
     * @exception SQLException if the driverName is NULL.
     * @see #getDriverName
     */
    public synchronized void setDriverName(String driverName) throws SQLException {
        if (driverName == null) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.nulldriver") // NOI18N
                    ),
                            SQL_INVALID_VALUE // 21000
                    );
            throw se;
        }
        this.driverName = driverName;
    }

    /**
     * Gets the default database URL for the data source. This default is only for
     * the current connection manager and was set by the
     * <code>ConnectionManager</code> constructor.
     * This default is used if you don't specify another database URL
     * with a <code>getConnection</code> method.
     * To change this default value, use the <code>setURL</code> method.
     * @return The name of the default database URL.
     * @see #getConnection
     * @see #setURL
     */
    public synchronized String getURL() {
        return (this.url);
    }

    /**
     * Sets the default database URL for the data source. This default is only
     * for the current connection manager.
     * To get a connection using a different data source than the default, use the
     * <code>getConnection</code> method that specifies a database URL as a parameter.
     * @param url  URL for this connection manager.
     * @exception SQLException if the URL is NULL.
     * @see #getConnection
     * @see #getURL
     */
    public synchronized void setURL(String url) throws SQLException {
        if (url == null) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.nullurl") // NOI18N
                    ),
                            SQL_INVALID_VALUE // 21000
                    );
            throw se;
        }
        this.url = url;
    }

    /**
     * Gets the default database user name for the current
     * connection manager.
     * This default was set by the <code>ConnectionManager</code>
     * constructor, and is used if you don't specify another user name
     * with a <code>getConnection</code> method.
     * To change this default value, use the <code>setUserName</code> method.
     * @return The default database user name.
     * @see #getConnection
     * @see #setUserName
     */
    public synchronized String getUserName() {
        return (this.userName);
    }

    /**
     * Sets the default database user name for the current
     * connection manager.
     *
     * @param userName  the default user name for the current connection manager.
     * @see #getUserName
     */
    public synchronized void setUserName(String userName) throws SQLException {
        this.userName = userName;
    }

    /**
     * Gets the default database password for the current
     * connection manager.
     * This default was set by the <code>ConnectionManager</code>
     * constructor, and is used if you don't specify another password
     * with a <code>getConnection</code> method.
     * To change this default value, use the <code>setPassword</code> method.
     * @return The default database password.
     * @see #getConnection
     * @see #setPassword
     */
    public synchronized String getPassword() {
        return (this.password);
    }

    /**
     * Sets the default database password for the current connection manager.
     * @param password  the default password for the current connection manager.
     * @see #getPassword
     */
    public synchronized void setPassword(String password) throws SQLException {
        this.password = password;
    }

    /**
     * Gets the minimum number of pooled connections for the current
     * connection manager.
     * If this value is 0, the connection manager does not maintain
     * a connection pool until a connection is requested using the
     * getConnection method with no parameters.
     * <P>To change the minimum number of pooled connections, use the
     * <CODE>setMinPool</CODE> method.
     * @return The minimum number of pooled connections.
     * @see #getConnection
     * @see #setMinPool
     *
     */
    public synchronized int getMinPool() {
        return (this.minPool);
    }

    /**
     * Sets the minimum number of pooled connections for the current
     * connection manager.
     * The default minimum number of pooled connections is 0, which means that
     * no connections are pooled until a pooled connection is requested.
     * <P>The specified value of the minPool parameter must be:
     * <UL>
     * <LI>greater than or equal to 0.
     * <LI>greater than or equal to the current minimum number of pooled
     * connections
     * <LI>less than or equal to the maximum number of pooled connections
     * </UL>
     * Otherwise, this method throws a SQLException.
     * @param minPool the minimum number of pooled connections.
     * @exception SQLException if the connection manager is being shut down or
     * if the minPool value is not valid.
     * @see #getMaxPool
     * @see #getMinPool
     * @see #setMaxPool
     */
    public synchronized void setMinPool(int minPool) throws SQLException {
        if (shutDownPending == true) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.isdown") // NOI18N
                    ),
                            SQL_CONN_FAIL // 08006
                    );

            throw se;
        }
        if (minPool < 0) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.zero") // NOI18N
                    ),
                            SQL_INVAL_PARAM_VALUE	// 22023
                    );
            throw se;
        }
        if (minPool < this.minPool) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.badnew"), // NOI18N
                            Integer.toString(minPool),
                            Integer.toString(minPool)
                    ),
                            SQL_INVAL_PARAM_VALUE	// 22023
                    );
            throw se;
        }
        if (pooling == true) {
            if (minPool > maxPool) {
                SQLException se = new SQLException
                        (
                                StringScanner.createParamString
                        (
                                I18NHelper.getMessage(messages,
                                        "connection.connectionmanager.poolsize") // NOI18N
                        ),
                                SQL_INVAL_PARAM_VALUE	// 22023
                        );
                throw se;
            }
        }
        this.minPool = minPool;
    }

    /**
     * Gets the maximum number of pooled connections for the current
     * connection manager.
     * If this value is 0, the connection manager does not maintain
     * a connection pool.
     * When you request a connection with the <CODE>getConnection</CODE>
     * method, the <CODE>getConnection</CODE>
     * method always returns a new connection.
     * <P>To change the maximum number of pooled connections, use the
     * <CODE>setMaxPool</CODE> method.
     * @return The maximum number of pooled connections the current connection
     * manager maintains.
     * @see #setMaxPool
     *
     */
    public synchronized int getMaxPool() {
        return (this.maxPool);
    }

    /**
     * Sets the maximum number of pooled connections for the current
     * connection manager.
     * The default maximum number of pooled connections is 0, which means
     * that no connections are pooled.
     * <P>The specified value of the maxPool parameter must be:
     * <UL>
     * <LI>greater than or equal to 0.
     * <LI>greater than or equal to the current maximum number of pooled
     * connections
     * <LI>greater than or equal to the minimum number of pooled connections
     * </UL>
     * Otherwise, this method throws a SQLException.
     *
     * @param maxPool the maximum number of pooled connections.
     * @exception SQLException if the connection manager is being shut down or
     * if the maxPool value is not valid.
     * @see #getMaxPool
     * @see #getMinPool
     * @see #setMinPool
     */
    public synchronized void setMaxPool(int maxPool) throws SQLException {
        if (shutDownPending == true) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.isdown") // NOI18N
                    ),
                            SQL_CONN_FAIL	// 08006
                    );
            throw se;
        }
        if (maxPool < 0) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.zero"), // NOI18N
                            Integer.toString(maxPool)
                    ),
                            SQL_INVAL_PARAM_VALUE	// 22023
                    );
            throw se;
        }
        if (pooling == true) {
            if (maxPool < this.maxPool) {
                SQLException se = new SQLException
                        (
                                StringScanner.createParamString
                        (
                                I18NHelper.getMessage(messages,
                                        "connection.connectionmanager.badnew"), // NOI18N
                                Integer.toString(maxPool),
                                Integer.toString(maxPool)
                        ),
                                SQL_INVAL_PARAM_VALUE	// 22023
                        );
                throw se;
            }
        }

        if (maxPool < this.minPool) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.poolsize") // NOI18N
                    ),
                            SQL_INVAL_PARAM_VALUE	// 22023
                    );
            throw se;
        }
        this.maxPool = maxPool;
    }

    /**
     * Gets the amount of time, in milliseconds, the connection manager should spend trying
     * to get a pooled connection, which is the amount of time a requester might wait.
     * <p> This value is only meaningful when you use the <code>getConnection</code>
     * to get a pooled connection, which means that no database URL, user name, or
     * password is specified.
     * @return The wait time in milliseconds.
     * @see #getConnection
     * @see #setMsInterval
     *
     */
    public synchronized int getMsWait() {
        return (this.msWait);
    }

    /**
     * Sets the amount of time, in milliseconds, the connection manager should spend trying
     * to get a pooled connection, which is the amount of time a requester might wait.
     * Setting this value to 0 means that the connection manager does not try again to
     * get a database connection if it fails on the first try.
     * <p> This value is only meaningful when you use the <code>getConnection</code>
     * to get a pooled connection, which means that no database URL, user name, or
     * password is specified.
     * <p> The connection manager retries after the set interval until the total wait
     * time is equal to or greater than the specified wait time.
     * You can determine the total number of tries for a connection based on wait time
     * and interval settings using the following formula,
     * where msWait is the wait time, msInterval is
     * the time between attempts to get a connection:
     * <pre>
     * tries = msWait/msInterval + 2
     * </pre>
     * For example, if msWait is set to 2000 ms and msInterval is set to 1500 ms, then the
     * connection manager will try to get a database connection 3 times before throwing
     * an exception if it has failed.
     * <p> If the wait time value is less than the set value for the interval between retries,
     * but not zero, the connection manager waits the amount of time specified by the
     * interval, tries once, then returns an exception if it still could not get a connection.
     * @param msWait the wait time in milliseconds.
     * @see #getConnection
     * @see #getMsInterval
     * @see #getMsWait
     * @see #setMsInterval
     *
     */
    public synchronized void setMsWait(int msWait) throws SQLException {
        if (msWait < 0) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.badvalue"), // NOI18N
                            Integer.toString(msWait)
                    ),
                            SQL_INVAL_PARAM_VALUE	// 22023
                    );
            throw se;
        } else if (msWait > 0) {
            this.msWait = msWait;
            this.connectionBlocking = true;
        } else {
            this.msWait = msWait;
            this.connectionBlocking = false;
        }
    }

    /**
     * Gets the amount of time, in milliseconds,
     * between the connection manager's attempts to get a pooled connection.
     * <p> This value is only meaningful when you use the <code>getConnection</code>
     * to get a pooled connection, which means that no database URL, user name, or
     * password is specified.
     * @return The length of the interval between tries in milliseconds.
     * @see #getConnection
     * @see #setMsInterval
     */
    public synchronized int getMsInterval() {
        return (this.msInterval);
    }

    /**
     * Sets the amount of time, in milliseconds, between the connection
     * manager's attempts to get a pooled connection.
     * <p> This value is only meaningful when you use the <code>getConnection</code>
     * to get a pooled connection, which means that no database URL, user name, or
     * password is specified.
     * <p> The connection manager retries after the specified interval until the total wait
     * time is equal to or greater than the set wait time.
     * You can determine the total number of tries for a connection based on wait time
     * and interval settings using the following formula,
     * where msWait is the wait time, msInterval is
     * the time between attempts to get a connection:
     * <pre>
     * tries = msWait/msInterval + 2
     * </pre>
     * For example, if msWait is set to 2000 ms and msInterval is set to 1500 ms, then the
     * connection manager will try to get a database connection 3 times before throwing
     * an exception if it has failed.
     * <p> If the wait time value is greater than 0 but less than the
     * set value for the interval between retries,
     * the connection manager waits the amount of time specified by the
     * interval, tries once, then returns an exception if it still could not get a connection.
     * @param msInterval the interval between attempts to get a database connection, in milliseconds.
     * @see #getConnection
     * @see #getMsInterval
     * @see #getMsWait
     * @see #setMsWait
     *
     */
    public synchronized void setMsInterval(int msInterval) throws SQLException {
        if ((msInterval < 0) || (this.msWait < msInterval)) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.badnew"), // NOI18N
                            "MsInterval", // NOI18N
                            "MsWait" // NOI18N
                    ),
                            SQL_INVAL_PARAM_VALUE	// 22023
                    );
            throw se;
        } else {
            this.msInterval = msInterval;
        }
    }

    /**
     * Returns a string representation of the current ConnectionManager object.
     * <p>
     * @return  A <code>String</code> decribing the contents of the current
     * ConnectionManager object.
     */
    public synchronized String toString() {
        /*
		TraceLogger lgr = ThreadContext.lgr();
		// Check for trace flag sp:1:1
		boolean dif = ThreadContext.lgr().test
		(
			TraceLogger.CONFIGURATION,
			TraceLogger.SVC_SP,
			SPLogFlags.CFG_DIFFABLE_EXCEPTS,
			1
		);
		String buf = "ConnectManager@\n"; // NOI18N
		if (dif == false)
		{
			buf = buf + "    busyList = " + this.busyList + "\n"; // NOI18N
		}
		if (this.busyList != null)
		{
			buf = buf + "    busyList Object = " + this.busyList.toString(); // NOI18N
		}
		buf = buf + "    connectionBlocking = " + this.connectionBlocking + "\n"; // NOI18N
		buf = buf + "    driverName = " + this.driverName + "\n"; // NOI18N
		if (dif == false)
		{
			buf = buf + "    expandedDriverName = " + this.expandedDriverName + "\n"; // NOI18N
			buf = buf + "    expandedPassword = " + this.expandedPassword + "\n"; // NOI18N
			buf = buf + "    expandedUrl = " + this.expandedUrl	+ "\n"; // NOI18N
			buf = buf + "    expandedUserName = " + this.expandedUserName + "\n"; // NOI18N
			buf = buf + "    freeList = " + this.freeList + "\n"; // NOI18N
		}
		if (this.freeList != null)
		{
			buf = buf + "    freeList Object = " + this.freeList.toString(); // NOI18N
		}
		if (dif == false)
		{
			buf = buf + "    hashCode = " + this.hashCode() + "\n"; // NOI18N
		}
		buf = buf + "    maxPool = " + this.maxPool + "\n"; // NOI18N
		buf = buf + "    minPool = " + this.minPool	+ "\n"; // NOI18N
		buf = buf + "    msInterval = " + this.msInterval + "\n"; // NOI18N
		buf = buf + "    msWait = " + this.msWait + "\n"; // NOI18N
		buf = buf + "    password = " + this.password + "\n"; // NOI18N
		buf = buf + "    pooling = " + this.pooling + "\n"; // NOI18N
		buf = buf + "    poolSize = " + this.poolSize + "\n"; // NOI18N
		buf = buf + "    shutDownPending = " + this.shutDownPending + "\n"; // NOI18N
		buf = buf + "    url = " + this.url + "\n"; // NOI18N
		buf = buf + "    userName = " + this.userName + "\n"; // NOI18N

        return buf;
		*/

        return null;
    }

    // ----------  Private and default (friendly) methods -----------

    /**
     * Associate a Connection with a transaction.
     * <p>
     * @param  tran    The Transaction's object.
     * @param  conn    The Connection.
     */
    synchronized void associateXact(Transaction tran, ConnectionImpl conn) {
        if (tran != null)
            this.xactConnections.put((Object) tran, (Object) conn);
    }

    /**
     * Disassociate a Connection with a transaction.
     * <p>
     * @param  tran    The Transaction's object.
     * @param  conn   The ForteJDBCConnect hosting the transaction.
     * @param  free    The ConnectionImpl should be returned to freePool.
     * @ForteInternal
     */
    synchronized void disassociateXact
            (
            Transaction tran,
            ConnectionImpl conn,
            boolean free
            ) throws SQLException {
        ConnectionImpl xactConn = null;

        if (tran != null)
            xactConn = (ConnectionImpl) this.xactConnections.remove((Object) tran);

        if (tran == null || xactConn.equals((Object) conn)) {
            if (free == true) {
                if (conn.connectionManager.shutDownPending == false) {
                    this.freeList.insertAtTail((Linkable) conn);
                } else {
                    conn.close();
                }
            }
        } else {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            //MsgCat.getStr(DbmsMsgCat.DB_ERR_XACT_MISMATCH)
                            "Internal Error: transaction mismatch" // NOI18N
                    ),
                            SQL_TRANS_UNK // 08007
                    );
            throw se;
        }
    }

    /**
     * Expand connection pool by connections size.
     * <p>
     * @param     connections   Number of connections to add to pool.
     * @exception SQLException	if connection fails.
     * @ForteInternal
     */
    private synchronized void expandPool(int connections) throws SQLException {
        ConnectionImpl conn = null;

        if (this.shutDownPending == true) {
            SQLException se = new SQLException
                    (
                            StringScanner.createParamString
                    (
                            I18NHelper.getMessage(messages,
                                    "connection.connectionmanager.isdown") // NOI18N
                    ),
                            SQL_CONN_FAIL
                    );
            throw se;
        }

        for (int i = 0; i < connections; i++) {
            if (this.poolSize >= this.maxPool) {
                // There is no room for a new connection.
                SQLException se = new SQLException
                        (
                                StringScanner.createParamString
                        (
                                I18NHelper.getMessage(messages,
                                        "connection.connectionmanager.maxpool") // NOI18N
                        ),
                                SQL_CONN_FAIL
                        );
                throw se;
            } else // There is room in the pool, so get a new connection.
            {
                try {
                    conn = new ConnectionImpl
                            (
                                    DriverManager.getConnection
                            (
                                    this.expandedUrl,
                                    this.expandedUserName,
                                    this.expandedPassword
                            ),
                                    this.expandedUrl,
                                    this.expandedUserName,
                                    this
                            );
                    conn.setPooled(true);
                    this.freeList.insertAtTail((Linkable) conn);
                    this.poolSize++;
                } catch (SQLException e) {
                    throw e;
                }
            }
        }
    }

    /**
     * Expand an environment variable specified in attributes into their
     * corresponding values; e.g, if url = ${MYURL}, expand ${MYURL} into
     * its corresponding value.
     * <P>
     * @param envname	environment variable name.
     * @exceptions  SQLException  We should come up with a better one.
     */
    private String expandAttribute(String envname) throws SQLException {
        String attribute = null;
        /*RESOLVE:
		try
		{
			attribute = ForteProperties.expandVars(envname);
		}
		catch (EnvVariableException e)
		{
			SQLException se = new SQLException
			(
				StringScanner.createParamString
				(
				  I18NHelper.getMessage(messages,
                                             "connection.connectionmanager.badvalue"), // NOI18N
					envname
				),
				SQL_INVAL_PARAM_VALUE
			);
			throw se;
		}
		*/
        if (attribute != null) {
            return attribute;
        } else {
            return envname;
        }
    }

    /**
     * Wait for a pool connection.  The thread will wait msInterval
     * milliseconds between tries to get a connection from the pool.
     * If no connection is available after msWait milliseconds, an
     * exception is thrown.
     * <p>
     * @exception SQLException	if connection fails.
     * @ForteInternal
     */
    private synchronized void waitForConnection() throws SQLException {
        int interval = this.msInterval;
        int wait = this.msWait;
        int totalTime = 0;
        boolean done = false;
        Thread t = Thread.currentThread();
        do {
            // If there are idle connections in the pool
            if (this.freeList.size > 0) {
                done = true;
            } else // There are no idle connection in the pool
            {
                // Can the pool be expanded?
                if (this.poolSize < this.maxPool) {
                    // Yes, try to expand the pool.
                    try {
                        expandPool(1);
                        done = true;
                    } catch (SQLException se) {
                        throw se;
                    }
                } else // the pool is at maximum size...
                {
                    // If we have waited long enough, throw an exception.
                    if (totalTime >= wait) {
                        SQLException se = new SQLException
                                (
                                        StringScanner.createParamString
                                (
                                        I18NHelper.getMessage(messages,
                                                "connection.connectionmanager.conntimeout") // NOI18N
                                ),
                                        SQL_CONN_FAIL
                                );
                        throw se;
                    } else // Timeout has not expired, sleep for awhile.
                    {
                        try {
                            this.wait(interval);
                        } catch (InterruptedException ie) {
                            SQLException se = new SQLException
                                    (
                                            StringScanner.createParamString
                                    (
                                            I18NHelper.getMessage(messages,
                                                    "connection.connectionmanager.threaditerupted") // NOI18N
                                    ),
                                            SQL_CONN_FAIL
                                    );
                            throw se;
                        }
                    }
                    totalTime += interval;
                }
            }
        } while (!done);
    }


    public void setLoginTimeout(int seconds)
            throws SQLException {
        loginTimeout = seconds;
    }

    public int getLoginTimeout()
            throws SQLException {
        return loginTimeout;
    }

    /**
     * Called by ConnectionImpl to save a connection when it is no longer used.
     * Previous free connection can be released (closed) when a new one becomes available.
     */
    protected synchronized void replaceFreeConnection(ConnectionImpl c) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
            logger.finest("sqlstore.connection.conncectiomgr.replacefreeconn",freeConn); // NOI18N
        }
        if (freeConn != null) {
            // Release (close) the old connection.
            freeConn.release();
        }
        freeConn = c;
    }
}
