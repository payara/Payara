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
 * SQLPersistenceManagerFactory.java
 *
 * Created on March 6, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.impl;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.api.persistence.support.*;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceStore;
import com.sun.jdo.spi.persistence.support.sqlstore.SQLStoreManager;
import com.sun.jdo.spi.persistence.support.sqlstore.VersionConsistencyCache;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.EJBHelper;
import com.sun.jdo.spi.persistence.utility.BucketizedHashtable;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperPersistenceManager;

//Remove this once checkLogger() is removed from initialize

import javax.sql.DataSource;
import java.sql.SQLException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 *
 * @author  Marina Vatkina
 * @version 0.1
 */

public class SQLPersistenceManagerFactory
        implements com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManagerFactory {

    private PersistenceStore _store = null;
    private ConnectionFactory _connectionFactory = null;
    private Object _dataSource = null;
    private PersistenceManagerFactory _persistenceManagerFactory = null;

    /**
     * PersistenceManager and Transaction default flags
     */
    private boolean optimistic = true;
    private boolean retainValues = true;
    private boolean nontransactionalRead = true;
    private boolean ignoreCache = true;

    /**
     * sql Statement timeouts
     */
    private int queryTimeout = 0;
    private int updateTimeout = 0;

    /** Pooling size
     */
    private int minPool = 0;
    private int maxPool = 0;

    /**
     * The logger
     */
    private static Logger logger = LogHelperPersistenceManager.getLogger();

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", //NOI18N 
            SQLPersistenceManagerFactory.class.getClassLoader());

    /**
     * bucket size for Transactional cache of PersistenceManager instances
     */
    private static int pmCacheBucketSize;

    /**
     * initial capacity for Transactional cache of PersistenceManager instances
     */
    private static int pmCacheInitialCapacity;

    static {
        pmCacheBucketSize = Integer.getInteger(
                "com.sun.jdo.spi.persistence.support.sqlstore.impl.SQLPersistenceManagerFactory.pmCacheBucketSize", // NOI18N
                11).intValue();

        pmCacheInitialCapacity = Integer.getInteger(
                "com.sun.jdo.spi.persistence.support.sqlstore.impl.SQLPersistenceManagerFactory.pmCacheInitialCapacity", // NOI18N
                11 * pmCacheBucketSize).intValue();

        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest(
                "sqlstore.sqlpersistencemgrfactory.pmCacheBucketSize", // NOI18N
                 String.valueOf(pmCacheBucketSize));
            logger.finest(
                "sqlstore.sqlpersistencemgrfactory.pmCacheInitialCapacity", // NOI18N
                 String.valueOf(pmCacheInitialCapacity));
        }
    }

    /**
     * Transactional cache of PersistenceManager instances
     */
    private Map pmCache = new BucketizedHashtable(pmCacheBucketSize,
            pmCacheInitialCapacity);

    /**
     * Cache of StateManager instances that support version consistency
     */
    private VersionConsistencyCache vcCache = null;

    /**
     * Creates new <code>SQLPersistenceManagerFactory</code> without any user info
     */
    public SQLPersistenceManagerFactory() {

    }

    /**
     * Creates new <code>SQLPersistenceManagerFactory</code> with user info
     * @param connectionFactory	Connection Factory as java.lang.Object
     */
    public SQLPersistenceManagerFactory(Object connectionFactory) {
        if (connectionFactory instanceof ConnectionFactory)
            _connectionFactory = (ConnectionFactory) connectionFactory;
        else
            _dataSource = connectionFactory;

        if (this instanceof PersistenceManagerFactory)
            _persistenceManagerFactory = this;

        initialize();
    }

    /**
     * Creates new <code>SQLPersistenceManagerFactory</code> with user parameters
     * @param persistenceManagerFactory	PersistenceManagerFactory instance
     */
    public SQLPersistenceManagerFactory(
            PersistenceManagerFactory persistenceManagerFactory) {
        _persistenceManagerFactory = persistenceManagerFactory;
        Object cf = _persistenceManagerFactory.getConnectionFactory();

        if (cf instanceof ConnectionFactory)
            _connectionFactory = (ConnectionFactory) cf;
        else
            _dataSource = cf;

        initialize();
    }

    /**
     * Sets database user name
     * @param userName	 user name
     */
    public void setConnectionUserName(String userName) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns database user name
     * @return	current database user name
     */
    public String getConnectionUserName() {
        return _persistenceManagerFactory.getConnectionUserName();
    }

    /**
     * Sets database user password
     * @param       password user password
     */
    public void setConnectionPassword(String password) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Sets connection URL
     * @param	url connection URL
     */
    public void setConnectionURL(String url) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns connection URL
     * @return	connection URL
     */
    public String getConnectionURL() {
        return _persistenceManagerFactory.getConnectionURL();

    }

    /**
     * Sets JDBC driver name
     * @param	driverName driver name
     */
    public void setConnectionDriverName(String driverName) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns JDBC driver name
     * @return	driver name
     */
    public String getConnectionDriverName() {
        return _persistenceManagerFactory.getConnectionDriverName();

    }

    /**
     * Sets ConnectionFactory
     * @param	cf as java.lang.Object
     */
    public void setConnectionFactory(Object cf) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns ConnectionFactory
     * @return	Connection Factory as java.lang.Object
     */
    public Object getConnectionFactory() {
        if (_dataSource != null)
            return _dataSource;

        return _connectionFactory;
    }

    /**
     * Sets the optimistic flag for all PersistenceManagers
     * @param flag          boolean optimistic flag
     */
    public void setOptimistic(boolean flag) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns the boolean value of the optimistic flag for all PersistenceManagers
     * @return      boolean optimistic flag
     */
    public boolean getOptimistic() {
        return _persistenceManagerFactory.getOptimistic();
    }

    /**
     * Sets flag that will not cause the eviction of persistent instances after transaction completion.
     * @param flag          boolean flag passed
     */
    public void setRetainValues(boolean flag) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns the boolean value for the flag that will not cause the eviction of persistent
     * instances after transaction completion.
     * @return      boolean setting for the flag
     */
    public boolean getRetainValues() {
        return _persistenceManagerFactory.getRetainValues();
    }

    /**
     * Sets the flag that allows non-transactional instances to be managed in the cache.
     * @param flag          boolean flag passed
     */
    public void setNontransactionalRead(boolean flag) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns the boolean value for the flag that allows non-transactional instances to be
     * managed in the cache.
     * @return      boolean setting for the flag
     */
    public boolean getNontransactionalRead() {
        return _persistenceManagerFactory.getNontransactionalRead();
    }


    /**
     * Sets the flag that allows the user to request that queries be optimized to return
     * approximate results by ignoring changed values in the cache.
     * @param flag          boolean flag passed
     */
    public void setIgnoreCache(boolean flag) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns the boolean value for the flag that allows the user to request that queries
     * be optimized to return approximate results by ignoring changed values in the cache.
     * @return      boolean setting for the flag
     */
    public boolean getIgnoreCache() {
        return _persistenceManagerFactory.getIgnoreCache();
    }

    /**
     * Sets the number of seconds to wait for a query statement
     * to execute in the datastore associated with this PersistenceManagerFactory.
     * @param timeout          new timout value in seconds; zero means unlimited
     */
    public void setQueryTimeout(int timeout) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Gets the number of seconds to wait for a query statement
     * to execute in the datastore associated with this PersistenceManagerFactory.
     * @return      timout value in seconds; zero means unlimited
     */
    public int getQueryTimeout() {
        return _persistenceManagerFactory.getQueryTimeout();
    }

    /**
     * Sets maximum number of connections in the connection pool
     * @param MaxPool       maximum number of connections
     */
    public void setConnectionMaxPool(int MaxPool) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns maximum number of connections in the connection pool
     * @return      connectionMaxPool
     */
    public int getConnectionMaxPool() {
        return _persistenceManagerFactory.getConnectionMaxPool();
    }

    /**
     * Sets minimum number of connections in the connection pool
     * @param MinPool       minimum number of connections
     */
    public void setConnectionMinPool(int MinPool) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns minimum number of connections in the connection pool
     * @return      connectionMinPool
     */
    public int getConnectionMinPool() {
        return _persistenceManagerFactory.getConnectionMinPool();
    }

    /**
     * Sets the number of milliseconds to wait for an available connection
     * from the connection pool before throwing an exception
     * @param MsWait        number in milliseconds
     */
    public void setConnectionMsWait(int MsWait) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns the number of milliseconds to wait for an available connection
     * from the connection pool before throwing an exception
     * @return      number in milliseconds
     */
    public int getConnectionMsWait() {
        return _persistenceManagerFactory.getConnectionMsWait();
    }

    /**
     * Sets the amount of time, in milliseconds, between the connection
     * manager's attempts to get a pooled connection.
     * @param MsInterval    the interval between attempts to get a database
     *                      connection, in milliseconds.
     *
     */
    public void setConnectionMsInterval(int MsInterval) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns the amount of time, in milliseconds, between the connection
     * manager's attempts to get a pooled connection.
     * @return      the length of the interval between tries in milliseconds
     */
    public int getConnectionMsInterval() {
        return _persistenceManagerFactory.getConnectionMsInterval();
    }

    /**
     * Sets the number of seconds to wait for a new connection to be
     * established to the data source
     * @param LoginTimeout           wait time in seconds
     */
    public void setConnectionLoginTimeout(int LoginTimeout) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns the number of seconds to wait for a new connection to be
     * established to the data source
     * @return      wait time in seconds
     */
    public int getConnectionLoginTimeout() {
        return _persistenceManagerFactory.getConnectionLoginTimeout();
    }

    /**
     * Sets the LogWriter to which messages should be sent
     * @param pw            LogWriter
     */
    public void setConnectionLogWriter(PrintWriter pw) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns the LogWriter to which messages should be sent
     * @return      LogWriter
     */
    public PrintWriter getConnectionLogWriter() {
        return _persistenceManagerFactory.getConnectionLogWriter();
    }

    /**
     * Sets transaction isolation level for all connections of this PersistenceManagerFactory.
     * All validation is done by java.sql.Connection itself, so e.g. while Oracle
     * will not allow to set solation level to TRANSACTION_REPEATABLE_READ, this method
     * does not have any explicit restrictions
     *
     * @param level - one of the java.sql.Connection.TRANSACTION_* isolation values
     */
    public void setConnectionTransactionIsolation(int level) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns current transaction isolation level for connections of this PersistenceManagerFactory.
     * @return      the current transaction isolation mode value as java.sql.Connection.TRANSACTION_*
     */
    public int getConnectionTransactionIsolation() {
        return _persistenceManagerFactory.getConnectionTransactionIsolation();
    }

    /**
     * Sets ConnectionFactory name
     * @param connectionFactoryName     ConnectionFactory name
     */
    public void setConnectionFactoryName(String connectionFactoryName) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns ConnectionFactory name
     * @return      ConnectionFactoryName
     */
    public String getConnectionFactoryName() {
        return _persistenceManagerFactory.getConnectionFactoryName();
    }

    /**
     * Sets Identifier.
     * @param identifier
     */
    public void setIdentifier(String identifier) {
		throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
				"jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Gets Identifier.
     * @return identifier
     */
    public String getIdentifier() {
        return _persistenceManagerFactory.getIdentifier();
    }

    /**
     * Returns maximum number of PersistenceManager instances in the pool
     * @return maxPool
     */
    public int getMaxPool() {
        return maxPool;
    }


    /**
     * Sets maximum number of PersistenceManager instances in the pool
     * @param MaxPool       maximum number of PersistenceManager instances
     */
    public void setMaxPool(int MaxPool) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Returns minimum number of PersistenceManager instances in the pool
     * @return minPool
     */
    public int getMinPool() {
        return minPool;
    }


    /**
     * Sets minimum number of PersistenceManager instances in the pool
     * @param MinPool       minimum number of PersistenceManager instances
     */
    public void setMinPool(int MinPool) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }


    /**
     * Sets the number of seconds to wait for an update statement
     * to execute in the datastore associated with this PersistenceManagerFactory.
     * @param timeout          new timout value in seconds; zero means unlimited
     */
    public void setUpdateTimeout(int timeout) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Gets the number of seconds to wait for an update statement
     * to execute in the datastore associated with this PersistenceManagerFactory.
     * @return      timout value in seconds; zero means unlimited
     */
    public int getUpdateTimeout() {
        return _persistenceManagerFactory.getUpdateTimeout();
    }

    /**
     * Returns the boolean value of the supersedeDeletedInstance flag
     * for all PersistenceManagers. If set to true, deleted instances are
     * allowed to be replaced with persistent-new instances with the equal
     * Object Id.
     * @return      boolean supersedeDeletedInstance flag
     */
    public boolean getSupersedeDeletedInstance () {
        return _persistenceManagerFactory.getSupersedeDeletedInstance();
    }


    /**
     * Sets the supersedeDeletedInstance flag for all PersistenceManagers.
     * @param flag          boolean supersedeDeletedInstance flag
     */
    public void setSupersedeDeletedInstance (boolean flag) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
      * Returns the default value of the requireCopyObjectId flag. If set to false, PersistenceManagers
      * will not create a copy of an ObjectId for <code>PersistenceManager.getObjectId(Object pc)</code>
      * and <code>PersistenceManager.getObjectById(Object oid)</code> requests.
      *
      * @see PersistenceManager#getObjectId(Object pc)
      * @see PersistenceManager#getObjectById(Object oid)
      * @return      boolean requireCopyObjectId flag
      */
     public boolean getRequireCopyObjectId() {
         return _persistenceManagerFactory.getRequireCopyObjectId();
     }


     /**
      * Sets the default value of the requireCopyObjectId.
      * If set to false, PersistenceManagers will not create a copy of
      * an ObjectId for <code>PersistenceManager.getObjectId(Object pc)</code>
      * and <code>PersistenceManager.getObjectById(Object oid)</code> requests.
      *
      * @see PersistenceManager#getObjectId(Object pc)
      * @see PersistenceManager#getObjectById(Object oid)
      * @param flag          boolean requireCopyObjectId flag
      */
     public void setRequireCopyObjectId (boolean flag) {
         throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
     }

    /**
     * Returns the boolean value of the requireTrackedSCO flag
     * for this PersistenceManagerFactory. If set to false, by default the
     * PersistenceManager will not create tracked SCO instances for
     * new persistent instances at commit with retainValues set to true and while
     * retrieving data from a datastore.
     *
     * @return      boolean requireTrackedSCO flag
     */
    public boolean getRequireTrackedSCO() {
        return _persistenceManagerFactory.getRequireTrackedSCO();
    }
  
    /**
     * Sets the requireTrackedSCO flag for this PersistenceManagerFactory.
     * If set to false, by default the PersistenceManager will not create tracked
     * SCO instances for new persistent instances at commit with retainValues set to true
     * requests and while retrieving data from a datastore.
     *
     * @param flag          boolean requireTrackedSCO flag
     */
    public void setRequireTrackedSCO (boolean flag) {
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "jdo.persistencemanagerfactoryimpl.notsupported")); //NOI18N
    }

    /**
     * Creates new <a href="PersistenceManager.html">PersistenceManager</a> without specific
     * info.
     * @return      the persistence manager
     * @exception JDOUserException if data source info is not set
     */
    public PersistenceManager getPersistenceManager() {
        return getPersistenceManager(null, null);
    }

    /**
     * Creates new <a href="PersistenceManager.html">PersistenceManager</a> with specific
     * username and password. Used to call ConnectionFactory.getConnection(String, String)
     * @param       username      datasource user
     * @param       password      datasource user password
     * @return	the persistence manager
     * @exception JDOUserException if data source info is not set
     */
    public PersistenceManager getPersistenceManager(String username, String password) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (_connectionFactory == null && _dataSource == null) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerfactoryimpl.getpersistencemanager.notconfigured"));// NOI18N
        }

        if (debug) {
             logger.finest("sqlstore.sqlpersistencemgrfactory.getpersistencemgr",Thread.currentThread()); // NOI18N
        }

        // Check if we are in managed environment and PersistenceManager is cached
        PersistenceManagerImpl pm = null;
        javax.transaction.Transaction t = EJBHelper.getTransaction();

        if (t != null) {
            if (debug) {
                Object[] items = new Object[] {Thread.currentThread(),t};
                logger.finest("sqlstore.sqlpersistencemgrfactory.getpersistencemgr.found",items); // NOI18N
            }

            pm = (PersistenceManagerImpl) pmCache.get(t);
            if (pm == null) {
                // Not found
                pm = getFromPool(t, username, password);
                pmCache.put(t, pm);
            } else if(pm.isClosed()) {
                if (debug) {
                     Object[] items = new Object[] {Thread.currentThread(),t};
                     logger.finest("sqlstore.sqlpersistencemgrfactory.getpersistencemgr.pmclosedfor",items); // NOI18N
                }
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "jdo.persistencemanagerfactoryimpl.getpersistencemanager.closed", // NOI18N
                        t));
            }

            // We know we are in the managed environment and
            // JTA transaction is  active. We need to start
            // JDO Transaction internally if it is not active.

            com.sun.jdo.spi.persistence.support.sqlstore.Transaction tx =
                    (com.sun.jdo.spi.persistence.support.sqlstore.Transaction) pm.currentTransaction();
            if (debug) {
                logger.finest("sqlstore.sqlpersistencemgrfactory.getpersistencemgr.jdotx",tx); // NOI18N
            }

            if (!tx.isActive()) {
                tx.begin(t);
            }

            if (!(pm.verify(username, password))) {
                ;
                throw new JDOUserException(I18NHelper.getMessage(messages,
                        "jdo.persistencemanagerfactoryimpl.getpersistencemanager.error")); // NOI18N
            }
        } else {
            if (debug) {
                logger.finest("sqlstore.sqlpersistencemgrfactory.getpersistencemgr.jdotx.notfound"); // NOI18N
            }
            // We don't know if we are in the managed environment or not
            // If Yes, it is BMT with JDO Transaction and it will register
            // itself at the begin().
            pm = getFromPool(null, username, password);
        }

        if (debug) {
            Object[] items = new Object[] {Thread.currentThread(),pm,t};
            logger.finest("sqlstore.sqlpersistencemgrfactory.getpersistencemgr.pmt",items); // NOI18N
        }

        // Always return a wrapper
        return new PersistenceManagerWrapper(pm);

    }

    /**
     * Returns non-operational properties to be available to the application via a Properties instance.
     * @return      Properties object
     */
    public Properties getProperties() {
        return _persistenceManagerFactory.getProperties();
    }

    /**
     * Returns instance of PersistenceManagerFactory
     */
    public PersistenceManagerFactory getPersistenceManagerFactory() {
        return _persistenceManagerFactory;
    }

    /**
     * Registers PersistenceManager in the transactional cache in
     * managed environment in case of BMT with JDO Transaction.
     * There is no javax.transaction.Transaction
     * available before the user starts the transaction.
     */
    public void registerPersistenceManager(
            com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager pm,
            javax.transaction.Transaction t) {

        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            Object[] items = new Object[] {pm,t};
            logger.finest("sqlstore.sqlpersistencemgrfactory.registerpersistencemgr.pmt",items); // NOI18N
        }
        PersistenceManager pm1 = (PersistenceManager) pmCache.get(t);
        // double-check locking has been removed
        if (pm1 == null) {
            pmCache.put(t, pm);
            ((PersistenceManagerImpl) pm).setJTATransaction(t);
            return;
        }

        if (pm1 != pm) {
            Object[] items = new Object[] {t, pm1};
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerfactoryimpl.registerpm.registered", // NOI18N
                    items));
        } else {
            // do nothing ???
        }
    }

    /**
     * Returns an instance of PersistenceManagerImpl from available pool
     * or creates a new one
     */
    private PersistenceManagerImpl getFromPool(javax.transaction.Transaction tx,
                                               String username, String password) {

        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            logger.finest("sqlstore.sqlpersistencemgrfactory.getfrompool"); // NOI18N
        }

        synchronized (this) {
            if (_store == null) {
                initializeSQLStoreManager(username, password);
            }
        }

        // create new PersistenceManager object and set its atributes
        PersistenceManagerImpl pm = new PersistenceManagerImpl(this, tx, username, password);
        pm.setStore(_store);
        if (debug) {
            Object[] items = new Object[] {pm,tx};
            logger.finest("sqlstore.sqlpersistencemgrfactory.getfrompool.pmt",items); // NOI18N
        }

        return pm;

    }
    
    /**
     * Returns unused PersistenceManager to the free pool
     */
    private void returnToPool(PersistenceManager pm) {
        // do nothing for now
        logger.finest("sqlstore.sqlpersistencemgrfactory.returnToPool"); // NOI18N
    }

    /** Releases closed PersistenceManager that is not in use
     */
    public void releasePersistenceManager(com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager pm,
                                          javax.transaction.Transaction t) {


        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            Object[] items = new Object[] {pm,t};
            logger.finest("sqlstore.sqlpersistencemgrfactory.releasepm.pmt",items); // NOI18N
            
        }

        if (t != null) {
            // Managed environment
            // Deregister only
            PersistenceManager pm1 = (PersistenceManager) pmCache.get(t);
            if (pm1 == null || pm1 != pm) {
                Object[] items = new Object[] {t, pm1};
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "jdo.persistencemanagerfactoryimpl.registerpm.registered", // NOI18N
                        items));
            } else {
                pmCache.remove(t);
            }
        } else {
            returnToPool(pm);
        }
    }

    private void initialize() {

        logger.finest("sqlstore.sqlpersistencemgrfactory.init"); // NOI18N
        optimistic = _persistenceManagerFactory.getOptimistic();
        retainValues = _persistenceManagerFactory.getRetainValues();
        nontransactionalRead = _persistenceManagerFactory.getNontransactionalRead();
        ignoreCache = _persistenceManagerFactory.getIgnoreCache();
        queryTimeout = _persistenceManagerFactory.getQueryTimeout();
        updateTimeout = _persistenceManagerFactory.getUpdateTimeout();
        minPool = _persistenceManagerFactory.getMinPool();
        maxPool = _persistenceManagerFactory.getMaxPool();
    }


    private void initializeSQLStoreManager(String username, String password) {
        Connection conn = null;
        try { 
             conn = getConnection(username, password);
            if (conn != null) {
                _store = new SQLStoreManager(conn.getMetaData(),
                            getIdentifier() );
            }
        } catch(Exception e) {
            if (logger.isLoggable(Logger.WARNING)) {
                logger.log(Logger.WARNING, "jdo.sqlpersistencemanagerfactory.errorgettingDatabaseInfo", e); //NOI18N
            }

            if (e instanceof JDOException) {
                throw (JDOException) e;
            } else {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.configuration.getvendortypefailed"), e); // NOI18N
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch(Exception ex) {}
            }
        }
        
    }
    
    /**
     * Get Database connection for username and password.
     */
    private Connection getConnection(String username, String password) throws SQLException{
        Connection conn = null;

        if (_connectionFactory != null) {
            conn = _connectionFactory.getConnection();
        } else if (EJBHelper.isManaged()) {
            conn = EJBHelper.getConnection(_dataSource, username, password);
        } else {
            if (username == null) {
                conn = ((DataSource)_dataSource).getConnection();
            } else {
                conn = ((DataSource)_dataSource).getConnection(username, password);
            }
        }

        return conn;            
    }
    
    /**
     * Determines whether obj is a SQLPersistenceManagerFactory with the same configuration
     *
     * @param obj The possibly null object to check.
     * @return true if obj is equal to this SQLPersistenceManagerFactory; false otherwise.
     */
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof SQLPersistenceManagerFactory)) {
            SQLPersistenceManagerFactory pmf = (SQLPersistenceManagerFactory) obj;
            return (pmf._persistenceManagerFactory.equals(this._persistenceManagerFactory));

        }
        return false;
    }

    /**
     * Computes the hash code of this PersistenceManagerFactory.
     *
     * @return A hash code of the owning PersistenceManagerFactory as an int.
     */
    public int hashCode() {
        return this._persistenceManagerFactory.hashCode();
    }

    /**
     * Creates if necessary, and returns, this PMF's instance of its
     * VersionConsistencyCache.
     * @return This PMF's VersionConsistencyCache.
     */
    public VersionConsistencyCache getVersionConsistencyCache() {
        if (null == vcCache) {
            if (_store == null) {
                // Store should be configured already.
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "jdo.persistencemanagerfactoryimpl.getversionconsistencycache.nullstore")); // NOI18N
            }
            vcCache = VersionConsistencyCacheImpl.create();
            _store.getConfigCache().setVersionConsistencyCache(vcCache);
        }
        return vcCache;
    }
}
