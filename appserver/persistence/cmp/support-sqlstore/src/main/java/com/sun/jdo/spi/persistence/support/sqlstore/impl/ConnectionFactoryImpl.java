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
 * ConnectionFactoryImpl.java
 *
 * Created on March 10, 2000, 5:09 PM
 */
 
package com.sun.jdo.spi.persistence.support.sqlstore.impl;

import java.lang.String;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ResourceBundle;


import com.sun.jdo.api.persistence.support.*;
import com.sun.jdo.spi.persistence.support.sqlstore.connection.ConnectionManager;
import org.glassfish.persistence.common.I18NHelper;

/** 
 *
 * @author  Craig Russell
 * @version 0.1
 */
public class ConnectionFactoryImpl implements ConnectionFactory, java.io.Serializable {

	// Delegate all connection variables to ConnectionManager: 

        private String URL = null;
        private String userName = null;
        private String password = null;
        private String driverName = null;
        private int maxPool = 0;
        private int minPool = 0;
        private int msInterval = 0;
        private int loginTimeout = 0;
        private int msWait = 0;
        private int _txIsolation = -1;
        private transient PrintWriter logWriter = null;

	private transient boolean _configured = false;
	private transient ConnectionManager connectionManager = null;

        /**
         * I18N message handler
         */
        private transient final static ResourceBundle messages = I18NHelper.loadBundle(
                                ConnectionFactoryImpl.class);

	

  /**
   * Creates new default <code>ConnectionFactoryImpl</code> object
   */
  public ConnectionFactoryImpl()
  {
	//connectionManager = new ConnectionManager();
  }

  /**
   * Creates new <code>ConnectionFactoryImpl</code> object with user info
   * @param URL           connection URL
   * @param userName      database user
   * @param password      database user password
   * @param driverName    driver name
   */
  public ConnectionFactoryImpl(
		String URL, 
		String userName, 
		String password, 
		String driverName
		)
  {
		this.driverName = driverName;
		this.URL = URL;
		this.userName = userName;
		this.password = password;

  }

  /**
   * Creates new <code>ConnectionFactoryImpl</code> object with user  and connection info
   * @param URL           connection URL
   * @param userName      database user
   * @param password      database user password
   * @param driverName    driver name
   * @param minPool       minimum number of connections
   * @param maxPool       maximum number of connections
   */
  public ConnectionFactoryImpl(
		String URL, 
		String userName, 
		String password, 
		String driverName,
		int    minPool,
                int    maxPool
		)
  {
		this.driverName = driverName;
		this.URL = URL;
		this.userName = userName;
		this.password = password;
		this.minPool = minPool;
		this.maxPool = maxPool;

  }

  /**
   * Sets JDBC driver name
   * @param driverName    JDBC driver name
   */
  public void setDriverName (String driverName)
  {
	// REMOVE WHEN SUPPORTED:
	//unsupported();

	assertNotConfigured();
	// Delegate to ConnectionManager: this.driverName = driverName;
	if(connectionManager == null)
	{
		this.driverName = driverName;
	}
	else
	{
		try {
			connectionManager.setDriverName(driverName);
		} catch (Exception e) {
			throw new JDOFatalException(null, e);
		}
	}
  }

  /**
   * Returns JDBC driver name
   * @return      driver name
   */
  public String getDriverName ()
  {
	return driverName;
	//return connectionManager.getDriverName();
  }

  
  /**
   * Sets JDBC connection URL
   * @param URL   connection URL
   */
  public void setURL (String URL)
  {
	// REMOVE WHEN SUPPORTED:
	//unsupported();

	assertNotConfigured();
	// Delegate to ConnectionManager: this.URL = URL;
	if(connectionManager == null) 
        {
                this.URL = URL;
        } 
        else 
        { 
                try {
                	connectionManager.setURL(URL);
		} catch (Exception e) {
			throw new JDOFatalException(null, e);
		}
	}
  }

  /**
   * Returns connection URL
   * @return      connection URL
   */
  public String getURL ()
  {
	return URL;
	//return connectionManager.getURL();
  }

  
  /**
   * Sets database user
   * @param userName      database user
   */
  public void setUserName (String userName)
  {
	// REMOVE WHEN SUPPORTED:
	//unsupported();

	assertNotConfigured();
	// Delegate to ConnectionManager: this.userName = userName;
	if(connectionManager == null)
        {
                this.userName = userName;
	}
	else
        {
                try {
                	connectionManager.setUserName(userName);
		} catch (Exception e) {
			throw new JDOFatalException(null, e);
		}
	}
  }

  /**
   * Returns database user name
   * @return      current database user name
   */
  public String getUserName ()
  {
	return userName;
	//return connectionManager.getUserName();
  }

  
  /**
   * Sets database user password
   * @param password      database user password
   */
  public void setPassword (String password)
  {
	// REMOVE WHEN SUPPORTED:
	//unsupported();

	assertNotConfigured();
	// Delegate to ConnectionManager: this.password = password;
	if(connectionManager == null)
        {
                this.password = password;
        }
        else
        {
		try {
                	connectionManager.setPassword(password);
		} catch (Exception e) {
			throw new JDOFatalException(null, e);
		}
	}
  }

  
  /**
   * Sets minimum number of connections in the connection pool
   * @param minPool       minimum number of connections
   */
  public void setMinPool (int minPool)
  {
	assertNotConfigured();
	if(connectionManager == null) { 
                // Nothing to do yet 
                this.minPool = minPool; 
                return; 
        } 

	// Delegate to ConnectionManager: this.minPool = minPool;
	try {
                connectionManager.setMinPool(minPool);
	} catch (Exception e) {
		throw new JDOFatalException(null, e);
	}
  }

  /**
   * Returns minimum number of connections in the connection pool
   * @return      connection minPool
   */
  public int getMinPool ()
  {
	return minPool;
	//return connectionManager.getMinPool();
  }

  
  /**
   * Sets maximum number of connections in the connection pool
   * @param maxPool       maximum number of connections
   */
  public void setMaxPool (int maxPool)
  {
	assertNotConfigured();
	if(connectionManager == null) { 
                // Nothing to do yet 
                this.maxPool = maxPool; 
                return; 
        } 

	// Delegate to ConnectionManager: this.maxPool = maxPool;
	try {
                connectionManager.setMaxPool(maxPool);
	} catch (Exception e) {
		throw new JDOFatalException(null, e);
	}
  }

  /**
   * Returns maximum number of connections in the connection pool
   * @return      connection maxPool
   */
  public int getMaxPool ()
  {
	return maxPool;
	// return connectionManager.getMaxPool();
  }

  
  /**
   * Sets the amount of time, in milliseconds, between the connection
   * manager's attempts to get a pooled connection.
   * @param msInterval    the interval between attempts to get a database
   *                      connection, in milliseconds.
   */
  public void setMsInterval (int msInterval)
  {
	assertNotConfigured();
	if(connectionManager == null) { 
                // Nothing to do yet 
                this.msInterval = msInterval; 
                return; 
        } 

	// Delegate to ConnectionManager: this.msInterval = msInterval;
	try {
                connectionManager.setMsInterval(msInterval);
	} catch (Exception e) {
		throw new JDOFatalException(null, e);
	}
  }

  /**
   * Returns the amount of time, in milliseconds, between the connection
   * manager's attempts to get a pooled connection.
   * @return      the length of the interval between tries in milliseconds
   */
  public int getMsInterval ()
  {
	if (connectionManager == null)
		return msInterval;

	return connectionManager.getMsInterval();
  }

  
  /**
   * Sets the number of milliseconds to wait for an available connection
   * from the connection pool before throwing an exception
   * @param msWait        number in milliseconds
   */
  public void setMsWait (int msWait)
  {
	assertNotConfigured();
	if(connectionManager == null) { 
                // Nothing to do yet 
                this.msWait = msWait; 
                return; 
        } 

	// Delegate to ConnectionManager: this.msWait = msWait;
	try {
                connectionManager.setMsWait(msWait);
	} catch (Exception e) {
		throw new JDOFatalException(null, e);
	}
  }

  /**
   * Returns the number of milliseconds to wait for an available connection
   * from the connection pool before throwing an exception
   * @return      number in milliseconds
   */
  public int getMsWait ()
  {
	if (connectionManager == null)
                return msWait;

	return connectionManager.getMsWait();
  }

  
  /**
   * Sets the LogWriter to which messages should be sent
   * @param logWriter            logWriter
   */
  public void setLogWriter (PrintWriter logWriter)
  {
	assertNotConfigured();
	this.logWriter = logWriter; //RESOLVE
  }

  /**
   * Returns the LogWriter to which messages should be sent
   * @return      logWriter
   */
  public PrintWriter getLogWriter ()
  {
	return logWriter;
  }

 
  /**
   * Sets the number of seconds to wait for a new connection to be
   * established to the data source
   * @param loginTimeout           wait time in seconds
   */
  public void setLoginTimeout (int loginTimeout)
  {
	assertNotConfigured();
	if(connectionManager == null) { 
                // Nothing to do yet 
                this.loginTimeout = loginTimeout; 
                return; 
        } 

	// Delegate to ConnectionManager: this.loginTimeout = loginTimeout;
	try {
                connectionManager.setLoginTimeout(loginTimeout);
	} catch (Exception e) {
		throw new JDOFatalException(null, e);
	}
  }

  /**
   * Returns the number of seconds to wait for a new connection to be
   * established to the data source
   * @return      wait time in seconds
   */
  public int getLoginTimeout ()
  {
	if (connectionManager == null)
                return loginTimeout;

	try {
                return connectionManager.getLoginTimeout();
	} catch (Exception e) {
                return 0;
	}
  }
  /**
   **
   * Sets transaction isolation level for all connections of this ConnectionFactory.
   * All validation is done by java.sql.Connection itself, so e.g. while Oracle    
   * will not allow to set solation level to TRANSACTION_REPEATABLE_READ, this method
   * does not have any explicit restrictions
   *
   * @param level - one of the java.sql.Connection.TRANSACTION_* isolation values
   */
  public void setTransactionIsolation (int level)
  {
	assertNotConfigured();
	if(connectionManager == null) {
		// Nothing to do yet
		_txIsolation = level;
		return;
	}

	// verify that database supports it
	Connection conn = null;
	try {
		conn = connectionManager.getConnection();
		DatabaseMetaData dbMetaData = conn.getMetaData();
		if(dbMetaData.supportsTransactionIsolationLevel(level)) 
		{
                	_txIsolation = level;
		} 
		else 
		{
			throw new JDOFatalException(I18NHelper.getMessage(messages,
                                "connectionefactoryimpl.isolationlevel_notsupported", //NOI18N
				level)); 
		}
	} catch (Exception e) {
		throw new JDOFatalException(null, e);
	}
	finally
	{
		closeConnection(conn);
	}
  }

  /**
   * Gets this ConnectionFactory's current transaction isolation level.
   * @return      the current transaction isolation mode value as java.sql.Connection.TRANSACTION_*
   */
  public int getTransactionIsolation ()
  {
	if (connectionManager == null)
                return _txIsolation;

	Connection conn = null;
	try {
		// Delegate to the Connection
                if (_txIsolation == -1)
		{
		    synchronized(this)
		    {
			//Double check that it was not set before
                	if (_txIsolation == -1)
			{
				conn = connectionManager.getConnection();
				_txIsolation = conn.getTransactionIsolation();
			}
		    }
		}

		return _txIsolation;
	} catch (Exception e) {
                throw new JDOFatalException(null, e);
	}
	finally
	{
		closeConnection(conn);
	}
  }


  /**
   * Returns java.sql.Connection
   * @return      connection as java.sql.Connection
   */
  public Connection getConnection() 
  {
	// Delegate to ConnectionManager
	try {
		if (connectionManager == null)
			initialize();

                Connection conn = connectionManager.getConnection();
		conn.setTransactionIsolation(_txIsolation);

		return conn;

	} catch (SQLException e) {
		String sqlState = e.getSQLState();
		int  errorCode = e.getErrorCode();

		if (sqlState == null)
		{
			throw new JDODataStoreException(I18NHelper.getMessage(messages,
                                "connectionefactoryimpl.sqlexception", "null", "" + errorCode), e); //NOI18N
		}
		else
		{
			throw new JDODataStoreException(I18NHelper.getMessage(messages,
                                "connectionefactoryimpl.sqlexception", sqlState, "" + errorCode), e); //NOI18N
		}
	} catch (Exception e) {
                throw new JDOCanRetryException(I18NHelper.getMessage(messages, 
                                "connectionefactoryimpl.getconnection"), e); //NOI18N
	}
  }

  /**
  * Determines whether obj is a ConnectionFactoryImpl with the same configuration
  *
  * @param obj The possibly null object to check.
  * @return true if obj is equal to this ConnectionFactoryImpl; false otherwise.
  */
  public boolean equals(Object obj) {
	if ((obj != null) && (obj instanceof ConnectionFactoryImpl) ) {
		ConnectionFactoryImpl cf = (ConnectionFactoryImpl)obj;
		return (cf.URL.equals(this.URL) && cf.userName.equals(this.userName) && cf.driverName.equals(this.driverName) && cf.password.equals(this.password));
	}
	return false;
  }

  /**
  * Computes the hash code of this ConnectionFactoryImpl.
  *
  * @return A hash code of the owning ConnectionFactoryImpl as an int.
  */
  public int hashCode() {
  	return URL.hashCode() + userName.hashCode() + password.hashCode() + driverName.hashCode();
  }


	/**
	 * INTERNAL
	 * Marks Connectionfactory as fully configured
	 * @param	flag	boolean flag 
	 */
	public void configured(boolean flag)
	{
		_configured = flag;
	}

        /**
         * INTERNAL
         * Asserts that change to the property is allowed
         */
        private void assertNotConfigured() {
                if ( _configured) {
                        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                                "persistencemanagerfactoryimpl.configured")); //NOI18N
                }
        }

        /**
         * INTERNAL
         * Asserts that MsWait and MsInterval are properly configured
         */
        private void assertConnectionWait() {
                if ( msWait < 0 )  
                {
                        throw new JDOUserException(I18NHelper.getMessage(messages,
                                             "connection.connectionmanager.mswaitvalue")); // NOI18N
                }
                else if ( msInterval < 0 || msInterval > msWait || (msWait > 0 && msInterval == 0) )
                {
                                throw new JDOUserException(I18NHelper.getMessage(messages,
                                             "connection.connectionmanager.msintervalvalue")); // NOI18N
                }
        }


	/**
         * INTERNAL
	 * Attempts to create new connectionManager
         * Throws JDOFatalException
	 */
	private synchronized void initialize() {
		// If connectionManager was already initialized by another thread
		if (connectionManager != null)
			return;

		try {
		// Verify msWait/msInterval values
			assertConnectionWait();

                // need to use this constructor, as it calls internaly startUp() method
                // to initialize extra variables and enable pooling
			//java.sql.DriverManager.setLogWriter(logWriter);
               		connectionManager = new ConnectionManager(
                        		driverName,
                        		URL,
                        		userName,
                        		password,
                        		minPool,
                        		maxPool
                        );
			// MsWait MUST be set BEFORE MsInterval
                        connectionManager.setMsWait(this.msWait);
			connectionManager.setMsInterval(this.msInterval);
                        connectionManager.setLoginTimeout(this.loginTimeout);

                        if (_txIsolation > 0)
                                setTransactionIsolation(_txIsolation);
                        else
                                _txIsolation = getTransactionIsolation();

			// finished all configuration
			this.configured(true);

        	} catch (JDOException e) {
                	throw e;
        	} catch (Exception e) {
                	throw new JDOFatalException(null, e);
		}
	}

	/**
         * INTERNAL
         * Throws JDOUnsupportedOptionException
	 */
	private void unsupported() {
		throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
			"persistencemanagerfactoryimpl.notsupported")); //NOI18N
	}

	/**
         * Close a connection
         */
        private void closeConnection(Connection conn)
        {
                try
                {
			if (conn != null) conn.close();
                }
                catch (Exception e)
                {
                        // Recover?
                }
 
                conn = null;
 
        }
}
