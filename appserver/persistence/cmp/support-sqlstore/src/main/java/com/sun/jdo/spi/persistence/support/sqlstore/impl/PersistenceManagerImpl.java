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
 * PersistenceManagerimpl.java
 *
 * Created on March 6, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.impl;

import com.sun.jdo.api.persistence.support.*;
import com.sun.jdo.api.persistence.support.Transaction;
import com.sun.jdo.spi.persistence.support.sqlstore.*;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceCapable;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManagerFactory;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.EJBHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.query.QueryImpl;
import com.sun.jdo.spi.persistence.utility.NullSemaphore;
import com.sun.jdo.spi.persistence.utility.Semaphore;
import com.sun.jdo.spi.persistence.utility.SemaphoreImpl;
import com.sun.jdo.spi.persistence.utility.logging.Logger;

import org.glassfish.persistence.common.I18NHelper;
import javax.transaction.Status;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class PersistenceManagerImpl implements PersistenceManager {
    /**
     * True if this PersistenceManager is closed
     */
    private boolean _isClosed = true;

    // Current wrapper
    private PersistenceManagerWrapper current = null;

    // Reference to the associated JTA Transaction if any
    private javax.transaction.Transaction _jta = null;

    /**
     * Reference to global PersistenceStore
     */
    private PersistenceStore _store = null;

    /**
     * Associated Transaction
     */
    private TransactionImpl _transaction = null;

    /**
     * PersistenceManagerFactory that created (and could be pooling)
     * this PersistenceManager
     */
    private PersistenceManagerFactory persistenceManagerFactory = null;


    /**
     * List of all Persistent instances used
     * in this Transaction not yet flushed to the datastore.
     */
    private List _txCache;

    /**
     * List of all Persistent instances used
     * in this Transaction.
     */
    private Set _flushedCache;

    /**
     * Map of Persistent instances accessed by this PersistenceManager. Ideally it should be
     * a weak-value HashMap to allow for garbage collection of instances that are not
     * referenced any more. java.util.WeakHashMap is a weak-key HashMap and thus does not
     * solve this purpose.
     */
    private Map _weakCache;

    /**
     * Intended to be set to true if there is an exception during flush in
     * {@link #beforeCompletion}. If true, then the Version Consistency cache
     * should be have instances removed during
     * {@link #afterCompletion}.
     */
    private boolean cleanupVersionConsistencyCache = false;

    //
    // Properties for controlling construction of caches.
    // For the integer properties, we can use Integer.getInteger.  There is
    // no corresponding method for floats :-(
    //

    /**
     * Properties with "dirtyCache" in their name apply to the _txCache.
     * This initializes the _txCache's initialCapacity.  If unspecified, uses
     * 20.
     */
    private static final int _txCacheInitialCapacity = Integer.getInteger(
      "com.sun.jdo.api.persistence.support.PersistenceManager.dirtyCache.initialCapacity", // NOI18N
      20).intValue();

    /**
     * Properties with "transactionalCache" in their name apply to the _flushedCache.
     * This initializes the _flushedCache's initialCapacity.  If unspecified,
     * uses 20.
     */
    private static final int _flushedCacheInitialCapacity = Integer.getInteger(
      "com.sun.jdo.api.persistence.support.PersistenceManager.transactionalCache.initialCapacity", // NOI18N
      20).intValue();

    /**
     * Properties with "transactionalCache" in their name apply to the _flushedCache.
     * This initializes the _flushedCache's loadFactor.  If unspecified, uses
     * 0.75, which is the default load factor for a HashSet.
     */
    private static final float _flushedCacheLoadFactor;

    static {
        float f = (float) 0.75;
        try {
            f =
                Float.valueOf(
                  System.getProperty(
                    "com.sun.jdo.api.persistence.support.PersistenceManager.transactionalCache.loadFactor", // NOI18N
                    "0.75")).floatValue(); // NOI18N
        } finally {
            _flushedCacheLoadFactor = f;
        }
    }

    /**
     * Properties with "globalCache" in their name apply to the _weakCache.
     * This initializes the _weakCache's initialCapacity.  If unspecified,
     * uses 20.
     */
    private static final int _weakCacheInitialCapacity = Integer.getInteger(
      "com.sun.jdo.api.persistence.support.PersistenceManager.globalCache.initialCapacity", // NOI18N
      20).intValue();

    /**
     * Properties with "globalCache" in their name apply to the _weakCache.
     * This initializes the _weakCache's loadFactor.  If unspecified, uses
     * 0.75, which is the default load factor for a WeakHashMap.
     */
    private static final float _weakCacheLoadFactor;

    static {
        float f = (float) 0.75;
        try {
            f =
                Float.valueOf(
                  System.getProperty(
                    "com.sun.jdo.api.persistence.support.PersistenceManager.globalCache.loadFactor", // NOI18N
                    "0.75")).floatValue(); // NOI18N
        } finally {
            _weakCacheLoadFactor = f;
        }
    }

    /** Collection of Query instances created for this pm. */
    private Collection queries = new ArrayList();

    /**
     * Flag for Query
     */
    private boolean _ignoreCache = true;

    /**
     * Flag for optimistic transaction
     */
    private boolean _optimistic = true;

    /**
     * Flag for supersedeDeletedInstance
     */
    private boolean _supersedeDeletedInstance = true;

    /**
     * Flag for requireCopyObjectId
     */
    private boolean _requireCopyObjectId = true;

    /**
     * Flag for requireTrackedSCO
     */
    private boolean _requireTrackedSCO = true;

    /**
     * Flag for nontransactionalRead
     */
    private boolean _nontransactionalRead = true;

    /**
     * Flag for active transaction
     */
    private boolean _activeTransaction = false;

    /**
     * User Object
     */
    private Object _userObject = null;

    /**
     * Flag for commit process
     */
    private boolean _insideCommit = false;

    /**
     * Flag for flush processing
     */
    private boolean _insideFlush = false;

    /**
     * Pattern for OID class names
     */
    private static final String oidName_OID = "OID";// NOI18N
    private static final String oidName_KEY = "KEY";// NOI18N

    /**
     * Properies object
     */
    private Properties _properties = null;

    /**
     * Lock used for synchronizing access to the _txCache and _weakCache
     */
    private final Semaphore _cacheLock;

    /**
     * Lock used for synchronizing field updates. It should be used
     * at the discretion of the StateManager.
     */
    private final Semaphore _fieldUpdateLock;

    /**
     * Lock used for implementing read-write barrier.
     */
    private Object _readWriteLock = new Object();

    /**
     * The count for the current reader (> 0) or writer (< 0)
     * Note that the exclusive lock can go multi-level deep
     * as long as the same thread is acquiring it.
     */
    private long _readWriteCount = 0;

    /**
     * The number of threads that are currently waiting on
     * the _readWriteLock.
     */
    private long _waiterCount = 0;

    /**
     * The thread that has the write (exclusive) lock.
     */
    private Thread _exclusiveLockHolder = null;

    /** Indicates if this PM is running in a multithreaded environment, i.e.,
     * if it has to do locking.
     */
    private final boolean _multithreaded;

    /**
     * The logger
     */
    private static Logger logger = LogHelperPersistenceManager.getLogger();

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle",  // NOI18N
            PersistenceManagerImpl.class.getClassLoader());

    /**
     * Constructor
     */
    PersistenceManagerImpl(PersistenceManagerFactory pmf, javax.transaction.Transaction t,
                           String username, String password) {
        persistenceManagerFactory = pmf;

        // Initialize caches as per property values.
        if (logger.isLoggable(Logger.FINEST)) {
            Object[] items = new Object[] { new Integer(_txCacheInitialCapacity),
                                            new Integer(_flushedCacheInitialCapacity),
                                            new Float(_flushedCacheLoadFactor),
                                            new Integer(_weakCacheInitialCapacity),
                                            new Float(_weakCacheLoadFactor) };
            logger.finest("sqlstore.persistencemgr.cacheproperties", items); // NOI18N
        }
        _txCache = new ArrayList(_txCacheInitialCapacity);
        _flushedCache = new LinkedHashSet(_flushedCacheInitialCapacity, _flushedCacheLoadFactor);
        _weakCache = new HashMap(_weakCacheInitialCapacity, _weakCacheLoadFactor);

        // create new Transaction object and set defaults

        _transaction = new TransactionImpl(this,
                username, password, TransactionImpl.TRAN_DEFAULT_TIMEOUT); // VERIFY!!!!

        _ignoreCache = pmf.getIgnoreCache();
        _optimistic = pmf.getOptimistic();
        _nontransactionalRead = pmf.getNontransactionalRead();
        _supersedeDeletedInstance = pmf.getSupersedeDeletedInstance();
        _requireCopyObjectId = pmf.getRequireCopyObjectId();
        _requireTrackedSCO = pmf.getRequireTrackedSCO();

        this._jta = t;	// if null, nothing is changed
        _isClosed = false;

        _multithreaded = ( ! EJBHelper.isManaged());

        if (_multithreaded) {
            _cacheLock =
                new SemaphoreImpl("PersistenceManagerImpl.cacheLock"); // NOI18N
            _fieldUpdateLock =
                new SemaphoreImpl("PersistenceManagerImpl.fieldUpdateLock"); // NOI18N
        } else {
            if (_jta == null) {
                // Non-transactional PersistenceManager can be used in a multithreaded
                // environment.
                _cacheLock =
                    new SemaphoreImpl("PersistenceManagerImpl.cacheLock"); // NOI18N
            } else {
                _cacheLock =
                    new NullSemaphore("PersistenceManagerImpl.cacheLock"); // NOI18N
            }
            _fieldUpdateLock =
                new NullSemaphore("PersistenceManagerImpl.fieldUpdateLock"); // NOI18N
        }
    }

    /**
     *
     */
    protected void setStore(PersistenceStore store) {
        _store = store;
    }

    /**
     *
     */
    protected PersistenceStore getStore() {
        return _store;
    }

    /**
     *
     */
    protected boolean getIgnoreCache() {
        return _ignoreCache;
    }

    /**
     *
     */
    protected boolean verify(String username, String password) {
        return _transaction.verify(username, password);
    }

    /**
     *
     */
    public boolean isClosed() {
        return _isClosed;
    }

    /**
     * Force to close the persistence manager. Called by
     * TransactionImpl.afterCompletion in case of the CMT transaction
     * and the status value passed to the method cannot be resolved.
     */
    public void forceClose() {

        // Return to pool - TBD if we use pooling of free PMs.
        //persistenceManagerFactory.returnToPool((com.sun.jdo.api.persistence.support.PersistenceManager)this);

        persistenceManagerFactory.releasePersistenceManager(this, _jta);

        // Closing PMWrappers need isClosed() to return true to avoid
        // endless recursion - last PMWrapper tries to close PM.
        _isClosed = true;
        while (current != null) {
            current.close();
        }

        Collection c = _weakCache.values();
        for (Iterator it = c.iterator(); it.hasNext();) {
            StateManager sm = (StateManager)it.next();

            // RESOLVE - do we want to release all references in SM?
            // 1 of two calls below should be removed.

            // Only nullify PM reference in SM.
            //sm.setPersistenceManager(null);

            // Nullify all references in SM.
            sm.release(); // This requires 'release()' to be added to SM interface.
        }

        disconnectQueries();

        persistenceManagerFactory = null;
        _jta = null;

        _weakCache.clear();
        _txCache.clear();
        _flushedCache.clear();

        _flushedCache = null;
        _txCache = null;
        _weakCache = null;

        _store = null;
        _transaction = null;

        _exclusiveLockHolder = null;
    }

    /**
     * close the persistence manager
     */
    public void close() {

        acquireExclusiveLock();

        try {
            if (_jta != null) {
                // Called by the transaction completion - deregister.
                persistenceManagerFactory.releasePersistenceManager(this, _jta);
                _jta = null;
            }

            if (current != null && _transaction.getTransactionType() != TransactionImpl.CMT) {
                /*
                                  if (_transaction.getTransactionType() == TransactionImpl.BMT_JDO) {
                                  persistenceManagerFactory.releasePersistenceManager(this, _jta);
                                  _jta = null;
                                  }
                                */
                return;     // or throw an exception ???
            }

            if (_activeTransaction || _flushedCache.size() > 0) {
                throw new JDOException(I18NHelper.getMessage(messages,
                        "jdo.persistencemanagerimpl.close.activetransaction"));// NOI18N
            }

            forceClose();

        } finally {
            releaseExclusiveLock();
        }
    }


    /**
     * Returns transaction associated with this persistence manager
     * @return transaction	current transaction
     */
    public Transaction currentTransaction() {
        assertIsOpen();
        return _transaction;
    }


    /** Create a new Query with no elements.
     * @return a new Query instance with no elements.
     */
    public Query newQuery() {
        assertIsOpen();
        QueryImpl q = new QueryImpl(this);
        registerQuery(q);
        return q;
    }

    /** Create a new Query using elements from another Query.  The other Query
     * must have been created by the same JDO implementation.  It might be active
     * in a different PersistenceManager or might have been serialized and
     * restored.
     * @return the new Query
     * @param compiled another Query from the same JDO implementation
     */
    public Query newQuery(Object compiled) {
        assertIsOpen();
        QueryImpl q = new QueryImpl(this, compiled);
        registerQuery(q);
        return q;
    }

    /** Create a new Query specifying the Class of the results.
     * @param cls the Class of the results
     * @return the new Query
     */
    public Query newQuery(Class cls) {
        assertIsOpen();
        QueryImpl q = new QueryImpl(this, cls);
        registerQuery(q);
        return q;
    }

    /** Create a new Query with the Class of the results and candidate Collection.
     * specified.
     * @param cls the Class of results
     * @param cln the Collection of candidate instances
     * @return the new Query
     */
    public Query newQuery(Class cls, Collection cln) {
        assertIsOpen();
        QueryImpl q = new QueryImpl(this, cls, cln);
        registerQuery(q);
        return q;
    }

    /** Create a new Query with the Class of the results and Filter.
     * specified.
     * @param cls the Class of results
     * @param filter the Filter for candidate instances
     * @return the new Query
     */
    public Query newQuery(Class cls, String filter) {
        assertIsOpen();
        QueryImpl q = new QueryImpl(this, cls, filter);
        registerQuery(q);
        return q;
    }

    /** Create a new Query with the Class of the results, candidate Collection,
     * and Filter.
     * @param cls the Class of results
     * @param cln the Collection of candidate instances
     * @param filter the Filter for candidate instances
     * @return the new Query
     */
    public Query newQuery(Class cls, Collection cln, String filter) {
        assertIsOpen();
        QueryImpl q = new QueryImpl(this, cls, cln, filter);
        registerQuery(q);
        return q;
    }

    /** The PersistenceManager may manage a collection of instances in the data
     * store based on the class of the instances.  This method returns a
     * Collection of instances in the data store that might be iterated or
     * given to a Query as the Collection of candidate instances.
     * @param persistenceCapableClass Class of instances
     * @param subclasses whether to include instances of subclasses
     * @return a Collection of instances
     * @see #newQuery
     */
    public Collection getExtent(Class persistenceCapableClass,
                                boolean subclasses) {
        assertIsOpen();
        return new ExtentCollection(this, persistenceCapableClass, subclasses);
    }

    /** This method locates a persistent instance in the cache of instances
     * managed by this PersistenceManager.  If an instance with the same ObjectId
     * is found it is returned.  Otherwise, a new instance is created and
     * associated with the ObjectId.
     *
     * <P>If the instance does not exist in the data store, then this method will
     * not fail.  However, a request to access fields of the instance will
     * throw an exception.
     * @see #getObjectById(Object, boolean)
     * @param oid an ObjectId
     * @return the PersistenceCapable instance with the specified
     * ObjectId
     */
    public Object getObjectById(Object oid) {
        return getObjectById(oid, false);
    }

    /** This method locates a persistent instance in the cache of instances
     * managed by this <code>PersistenceManager</code>.
     * The <code>getObjectByIdInternal</code> method attempts
     * to find an instance in the cache with the specified JDO identity.
     * <P>If the <code>PersistenceManager</code> is unable to resolve the <code>oid</code> parameter
     * to an ObjectId instance, then it throws a <code>JDOUserException</code>.
     * <P>If the <code>validate</code> flag is <code>false</code>, and there is already an instance in the
     * cache with the same JDO identity as the <code>oid</code> parameter, then this method
     * returns it. There is no change made to the state of the returned
     * instance.
     * <P>If there is not an instance already in the cache with the same JDO
     * identity as the <code>oid</code> parameter, then this method creates an instance
     * with the specified JDO identity and returns it. If there is no
     * transaction in progress, the returned instance will be hollow.
     * <P>If there is a transaction in progress, the returned instance will
     * persistent-nontransactional in an optimistic transaction, or persistent-clean in a
     * datastore transaction.
     * @return the <code>PersistenceCapable</code> instance with the specified ObjectId
     * @param oid an ObjectId
     * @param validate if the existence of the instance is to be validated
     */
    public Object getObjectById(Object oid, boolean validate) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        assertIsOpen();
        assertActiveTransaction(true);

        Object rc = null;

        if (debug) {
            Object[] items = new Object[] {oid, this,_jta};
            logger.finest("sqlstore.persistencemgr.getbyobjid", items); // NOI18N
        }

        if (oid == null)
            return null;

        StateManager sm = lookupObjectById(oid, null);
        rc = sm.getPersistent();
        if (!JDOHelper.isTransactional(rc)) {

            // Instance was not found in _weakCache, or the found instance is
            // non transactional.  Check the version consistency cache.
            boolean foundInstance = initializeFromVersionConsistencyCache(sm);

            if (validate && !foundInstance) {

                // Not found in the cache, or non transactional.
                try {
                    sm.reload();

                } catch (JDOException e) {
                    if (!sm.isValid()) {
                        // This StateManager had never been used before.
                        deregisterInstance(oid);
                        sm.release();
                    }

                    throw e;
                } catch (Exception e) {
                    throw new JDOUserException(I18NHelper.getMessage(messages,
                       "jdo.persistencemanagerimpl.fetchinstance.none"), e);// NOI18N
                }
            }
        }

        sm.setValid();
        return rc;
    }

    /**
     * Called internally by <code>RetrieveDesc</code> to lookup an instance
     * in the cache, or prepare new instance to be populated with values from the datastore.
     * @return the <code>StateManager</code> instance with the specified ObjectId
     * @param oid an ObjectId
     * @param pcClass the Class type of the PersistenceCapable instance to be associated
     * with this StateManager.
     */
    public StateManager findOrCreateStateManager(Object oid, Class pcClass) {
        return lookupObjectById(oid, pcClass);
    }

    /** This is the actual implementation of the #getObjectById(Object oid, validate) and
     * #getObjectByIdInternal(Object oid, Class pcClass).
     * @param oid an ObjectId
     * @param classType the Class type of the returned object or null if not known.
     */
    private StateManager lookupObjectById(Object oid, Class classType) {
        StateManager sm = null;

        // Check the _weakCache for the instance
        try {
            acquireCacheLock();

            sm = (StateManager)_weakCache.get(oid);
            if (sm == null) {
                boolean external = false;
                // Do NOT look in DB, but create a Hollow instance:
                if (classType == null) {
                    classType = loadClassForOid(oid);

                    if (classType == null) {
                        // Class not found, report an error
                        throw new JDOUserException(I18NHelper.getMessage(messages,
                                "jdo.persistencemanagerimpl.getobjectbyid.nometadata"), // NOI18N
                                new Object[]{oid});
                    }
                    external = true;
                }

                try {
                    // create new instance and register it
                    sm = createStateManager(classType);
                    if (external) {
                        // oid needs to be cloned for extenral cases
                        // as the user might modify oid
                        oid = internalCloneOid(oid, sm);
                    }
                    sm.setObjectId(oid);
                    setKeyFields(sm);

                    if (external) {
                        sm.initialize(false);
                    } else {
                        // put it in the weak cache only as it is Hollow.
                        _weakCache.put(oid, sm);
                    }
                } catch (JDOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new JDOUserException(I18NHelper.getMessage(messages,
                            "jdo.persistencemanagerimpl.fetchinstance.none"), e);// NOI18N
                }
            }
        } finally {
            releaseCacheLock();
        }

        return sm;
    }

    /**
     * Create a StateManager.
     * @param classType Class of the PersistenceCapable.
     */
    private StateManager createStateManager(Class classType) {
        StateManager rc = _store.getStateManager(classType);
        newInstance(rc);

        return rc;
    }

    /** The ObjectId returned by this method represents the JDO identity of
     * the instance.  The ObjectId is a copy (clone) of the internal state
     * of the instance, and changing it does not affect the JDO identity of
     * the instance.
     * Delegates actual execution to the internal method.
     * @param pc the PersistenceCapable instance
     * @return the ObjectId of the instance
     */
    public Object getObjectId(Object pc) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        assertIsOpen();
        assertActiveTransaction(true);

        if (debug) {
            Object[] items = new Object[] {Thread.currentThread(),pc, this, _jta};
            logger.finest("sqlstore.persistencemgr.getobjid",items); // NOI18N
        }

        try {
            assertPersistenceCapable(pc);
        } catch (Exception e) {
            if (debug) {
                Object[] items = new Object[] {pc, this};
                logger.finest("sqlstore.persistencemgr.getobjid.notpc",items); // NOI18N
            }

            return null;
        }

        StateManager sm = ((PersistenceCapable)pc).jdoGetStateManager();
        if (sm == null) {
            // Not persistent
            return null;

        } else if (sm.getPersistenceManagerInternal() != this) {
            if (debug) {
                Object[] items = new Object[] {pc, this, _jta};
                logger.finest("sqlstore.persistencemgr.getobjid.notpm",items); // NOI18N
            }

            return null;
        }

        return internalGetObjectId(sm);
    }

    /** This method is used to get a PersistenceCapable instance
     * representing the same data store object as the parameter, that is valid
     * for this PersistenceManager.
     * @param pc a PersistenceCapable instance
     * @return the PersistenceCapable instance representing the
     * same data store object
     */
    public Object getTransactionalInstance(Object pc) {
        assertIsOpen();
        assertActiveTransaction(false);
        if (!(pc instanceof PersistenceCapable)) {
            return pc;
        }

        PersistenceCapable mypc = (PersistenceCapable) pc;

        // The PC.jdoGetPersistenceManager() returns PersistenceManagerWrapper:
        PersistenceManagerWrapper pmw = (PersistenceManagerWrapper) mypc.jdoGetPersistenceManager();
        PersistenceManagerImpl pm = (PersistenceManagerImpl) pmw.getPersistenceManager();

        if (pm == null || pm == this) {
            return pc;
        }

        return getObjectById(pm.internalGetObjectId(mypc.jdoGetStateManager()));
    }

    /** Make the transient instance persistent in this PersistenceManager.
     * This method must be called in an active transaction.
     * The PersistenceManager assigns an ObjectId to the instance and
     * transitions it to persistent-new.
     * The instance will be managed in the Extent associated with its Class.
     * The instance will be put into the data store at commit.
     * @param pc a transient instance of a Class that implements
     * PersistenceCapable
     */
    public void makePersistent(Object pc) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug)
            {
            Object[] items = new Object[] {Thread.currentThread(), pc, this, _jta};
            logger.finest("sqlstore.persistencemgr.makepersistent",items); // NOI18N
            }

        if (pc == null)
            return;		// ignore

        acquireShareLock();
        try {
            assertIsOpen();
            assertActiveTransaction(false);
            assertPersistenceCapable(pc);
            internalMakePersistent((PersistenceCapable) pc);
            if (debug)
                {
                 Object[] items = new Object[] {pc, this, _jta};
                 logger.finest("sqlstore.persistencemgr.makepersistent.done",items); // NOI18N
                }

        } finally {
            releaseShareLock();
        }
    }

    /** Make an array of instances persistent.
     * @param pcs an array of transient instances
     * @see #makePersistent(Object pc)
     */
    public void makePersistent(Object[] pcs) {
        if (pcs == null)
            return;		// ignore

        for (int i = 0; i < pcs.length; i++) {
            makePersistent(pcs[i]);
        }
    }

    public void makePersistent(Collection pcs) {
        if (pcs == null)
            return;		// ignore

        makePersistent(pcs.toArray());
    }

    public void deletePersistent(Object pc) {
        if (pc == null)
            return;		// ignore

        acquireShareLock();

        try {
            assertIsOpen();
            assertActiveTransaction(false);
            assertPersistenceCapable(pc);
            internalDeletePersistent((PersistenceCapable) pc);
        } finally {
            releaseShareLock();
        }
    }

    public void deletePersistent(Object[] pcs) {
        if (pcs == null)
            return;		// ignore

        for (int i = 0; i < pcs.length; i++) {
            deletePersistent(pcs[i]);
        }
    }

    public void deletePersistent(Collection pcs) {
        if (pcs == null)
            return;		// ignore

        deletePersistent(pcs.toArray());
    }


    /** This method returns the PersistenceManagerFactory used to create
     * this PersistenceManager.  It returns null if this instance was
     * created via a constructor.
     * @return the PersistenceManagerFactory that created
     * this PersistenceManager
     */
    public com.sun.jdo.api.persistence.support.PersistenceManagerFactory getPersistenceManagerFactory() {
        return persistenceManagerFactory;
    }

    void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
        if (persistenceManagerFactory == null)
            persistenceManagerFactory = pmf;
    }

    /** The application can manage the PersistenceManager instances
     * more easily by having an application object associated with each
     * PersistenceManager instance.
     * @param o the user instance to be remembered by the PersistenceManager
     * @see #getUserObject
     */
    public void setUserObject(Object o) {
        this._userObject = o;
    }

    /** The application can manage the PersistenceManager instances
     * more easily by having an application object associated with each
     * PersistenceManager instance.
     * @return the user object associated with this PersistenceManager
     * @see #setUserObject
     */
    public Object getUserObject() {
        return _userObject;
    }

    /** The JDO vendor might store certain non-operational properties and
     * make those properties available to applications (for troubleshooting).
     *
     * <P>Standard properties include:
     * <li>VendorName</li>
     * <li>VersionNumber</li>
     * @return the Properties of this PersistenceManager
     */
    public Properties getProperties() {
        if (_properties == null) {
            _properties = RuntimeVersion.getVendorProperties(
                    "/com/sun/jdo/spi/persistence/support/sqlstore/sys.properties");// NOI18N
        }
        return _properties;
    }

    /**
     * Returns the boolean value of the supersedeDeletedInstance flag
     * for this PersistenceManager. If set to true, deleted instances are
     * allowed to be replaced with persistent-new instances with the equal
     * Object Id.
     * @return      boolean supersedeDeletedInstance flag
     */
    public boolean getSupersedeDeletedInstance () {
        return _supersedeDeletedInstance;
    }


    /**
     * Sets the supersedeDeletedInstance flag for this PersistenceManager.
     * @param flag          boolean supersedeDeletedInstance flag
     */
    public void setSupersedeDeletedInstance (boolean flag) {
        // RESOLVE: synchronization
        _supersedeDeletedInstance = flag;
    }

    /**
     * Returns the boolean value of the requireCopyObjectId flag
     * for this PersistenceManager. If set to false, the PersistenceManager
     * does not create a copy of an ObjectId for <code>PersistenceManager.getObjectId(Object pc)</code>
     * and <code>PersistenceManager.getObjectById(Object oid)</code> requests.
     *
     * @see PersistenceManager#getObjectId(Object pc)
     * @see PersistenceManager#getObjectById(Object oid)
     * @return      boolean requireCopyObjectId flag
     */
    public boolean getRequireCopyObjectId() {
        return _requireCopyObjectId;
    }


    /**
     * Sets the requireCopyObjectId flag for this PersistenceManager.
     * If set to false, the PersistenceManager will not create a copy of
     * an ObjectId for <code>PersistenceManager.getObjectId(Object pc)</code>
     * and <code>PersistenceManager.getObjectById(Object oid)</code> requests.
     *
     * @see PersistenceManager#getObjectId(Object pc)
     * @see PersistenceManager#getObjectById(Object oid)
     * @param flag          boolean requireCopyObjectId flag
     */
    public void setRequireCopyObjectId (boolean flag) {
        // RESOLVE: synchronization
        _requireCopyObjectId = flag;
    }

    /**
     * Returns the boolean value of the requireTrackedSCO flag
     * for this PersistenceManager. If set to false, this PersistenceManager
     * will not create tracked SCO instances for
     * new persistent instances at commit with retainValues set to true
     * and while retrieving data from a datastore.
     *
     * @return      boolean requireTrackedSCO flag
     */
    public boolean getRequireTrackedSCO() {
        return _requireTrackedSCO;
    }

    /**
     * Sets the requireTrackedSCO flag for this PersistenceManager.
     * If set to false, this PersistenceManager will not create tracked
     * SCO instances for new persistent instances at commit with retainValues
     * set to true and while retrieving data from a datastore.
     *
     * @param flag          boolean requireTrackedSCO flag
     */
    public void setRequireTrackedSCO (boolean flag) {
        // RESOLVE: synchronization
        _requireTrackedSCO = flag;
    }

    /** In order for the application to construct instance of the ObjectId class
     * it needs to know the class being used by the JDO implementation.
     * @param cls the PersistenceCapable Class
     * @return the Class of the ObjectId of the parameter
     */
    public Class getObjectIdClass(Class cls) {
        PersistenceConfig config = loadPersistenceConfig(cls);
        return config.getOidClass();
    }

    /**
     * Returns a new instance of the object defined by the given
     * StateManager
     * @param 	sm	StateManager
     * @return	new instance of the object
     */
    public Object newInstance(StateManager sm) {
        Object o = null;

        PersistenceConfig config = sm.getPersistenceConfig();

        if (config == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.newinstance.badsm"));// NOI18N
        }

        Constructor constr = config.getConstructor();
        try {
            if (constr != null) {
                o = constr.newInstance(new Object[]{sm});

                // Initalize the state manager to reference this pm and the newly created instance
                sm.setPersistenceManager(this);
                sm.setPersistent(o);
            }
        } catch (Exception e) {
            throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.assertpersistencecapable.error", // NOI18N
                    config.getPersistenceCapableClass().getName()), e);
        }

        return o;
    }

    /**
     * Executes the given retrieve descriptor. The result
     * is a collection unless an aggregate query was specified.
     * In most cases the query result is a collection of
     * persistent objects. In case of a projection
     * on a local field the collection holds objects of that
     * type. For aggregate queries the result is a
     * single object, which type was defined by the caller.
     *
     * @param action The retrieve descriptor.
     * @param parameters The input parameters for the query.
     * @return A collection of (persistent) objects unless
     * an aggregate query was specified.
     */
    public Object retrieve(RetrieveDesc action, ValueFetcher parameters)
    {
        acquireShareLock();

        try {
            assertActiveTransaction(true);
            return _store.retrieve(this, action, parameters);
        } finally {
            releaseShareLock();
        }
    }

    /**
     * Executes the given retrieve descriptor. The result
     * is a collection unless an aggregate query was specified.
     * In most cases the query result is a collection of
     * persistent objects. In case of a projection
     * on a local field the collection holds objects of that
     * type. For aggregate queries the result is a
     * single object, which type was defined by the caller.
     *
     * @param action The retrieve descriptor.
     * @return A collection of (persistent) objects unless
     * an aggregate query was specified.
     */
    public Object retrieve(RetrieveDesc action) {
        return retrieve(action, null);
    }

    /**
     * Return a RetrieveDesc given a Class object.
     */
    public RetrieveDesc getRetrieveDesc(Class classType) {
        acquireShareLock();

        try {
            loadPersistenceConfig(classType);
            return _store.getRetrieveDesc(classType);
        } finally {
            releaseShareLock();
        }
    }

    /**
     * Return a RetrieveDesc for a foreign field (relationship) given the
     * Class object for the parent class.
     */
    public RetrieveDesc getRetrieveDesc(String fieldName, Class classType) {
        acquireShareLock();

        try {
            loadPersistenceConfig(classType);
            return _store.getRetrieveDesc(fieldName, classType);
        } finally {
            releaseShareLock();
        }
    }


    /**
     * Register instance in the weak cache only. Used to restore persistent instance
     * at the rollback if it was replaced during transaction execution with another
     * instance with the same object Id.
     */
    public void registerInstance(StateManager sm, Object oid) {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug)
            {
            Object[] items = new Object[] {Thread.currentThread(), oid, this, _jta};
            logger.finest("sqlstore.persistencemgr.registerinstance",items); // NOI18N
            }

        try {
            acquireCacheLock();
            if (debug)
                logger.finest("sqlstore.persistencemgr.registerinstancein_wkc"); // NOI18N

            _weakCache.put(oid, sm);

            if (sm.needsRegisterWithVersionConsistencyCache()) {
                addToVersionConsistencyCache(sm);
            }
        } finally {
            releaseCacheLock();
        }
    }

    /**
     * Register instance in the transactional cache
     */
    public void registerInstance(StateManager sm, Object oid,
                boolean throwDuplicateException,
                boolean forceRegister) {
        if (oid == null) {
            oid = sm.getObjectId();
        }

        boolean debug = logger.isLoggable();
        if (debug) {
            Object[] items = new Object[] {Thread.currentThread(), oid, sm, this, _jta};
            logger.finest("sqlstore.persistencemgr.registerinstance",items); // NOI18N
        }

        //
        // register in all caches
        // We do explicit synchronization here using the cacheMutex. Note that for
        // performance reason, we only synchronize in the case where the instance
        // is not already in the cache.
        //
        try {
            acquireCacheLock();
            if (!_weakCache.containsKey(oid)) {
                if (debug)
                    logger.finest("sqlstore.persistencemgr.registerinstancein_wkc"); // NOI18N

                _weakCache.put(oid, sm);
            } else if (throwDuplicateException) {
                StateManager old = (StateManager)_weakCache.get(oid);
                if (_supersedeDeletedInstance && old.isDeleted()) {

                if (debug)
                    logger.finer(I18NHelper.getMessage(messages,
                        "sqlstore.persistencemgr.replacingdeletedinstance", oid)); // NOI18N

                    old.markNotRegistered();
                    old.markVerifyAtDeregister();
                    sm.markVerifyAtDeregister();
                    sm.markReplacement();

                    // Add the dependency management, so that delete happens
                    // before the insert. This operation registers the new instance.
                    old.addDependency(sm);

                    // Now we need to replace the old StateManager with the new
                    // in the _weakCache, because of the StateManager's dependency
                    // process.
                    _weakCache.put(oid, sm);

                    // Do not proceed as addDependency registered instance already.
                    return;

                } else {
                    throw new JDODuplicateObjectIdException(I18NHelper.getMessage(messages,
                        "jdo.persistencemanagerimpl.internalmakepersistent.dups"), // NOI18N
                        new Object[]{sm.getPersistent()});
                }
            }

            if (_activeTransaction && (sm.isTransactional() || forceRegister)) {
                if (debug) {
                    Object[] items = new Object[] {oid,sm.getPersistent(),this, _jta};
                    logger.finest("sqlstore.persistencemgr.registerinstancein_txc",items); // NOI18N
                }

                // Need to be carefull not to request registerInstance twice
                // for a dirty instance.
                if (sm.isDirty()) {
                    _txCache.add(sm);
                }

                // _flushedCache is a Set so it cannot have duplicates.
                _flushedCache.add(sm);

                if (sm.needsRegisterWithVersionConsistencyCache()) {
                    addToVersionConsistencyCache(sm);
                }
            }

        } finally {
            releaseCacheLock();
        }
    }

    public void deregisterInstance(Object oid) {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            Object[] items = new Object[] {oid,this,_jta};
            logger.finest("sqlstore.persistencemgr.deregisterinstance",items); // NOI18N
        }

        if (oid != null) {
            try {
                acquireCacheLock();
                StateManager sm = (StateManager) _weakCache.remove(oid);
                removeFromCaches(sm);
            } finally {
                releaseCacheLock();
            }
        }
    }

    public void deregisterInstance(Object oid, StateManager sm) {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            Object[] items = new Object[] {oid,this,_jta};
            logger.finest("sqlstore.persistencemgr.deregisterinstance.verify",items); // NOI18N
        }

        try {
            acquireCacheLock();
            Object known = _weakCache.get(oid);
            if (known == sm) {
                //deregister the instance from weak cache only if it is registered.
                 _weakCache.remove(oid);
                if (debug)
                    logger.finest("sqlstore.persistencemgr.deregisterinstance.verified"); // NOI18N
            }

            removeFromCaches(sm);
        } finally {
            releaseCacheLock();
        }
    }

    /**
     * If a transaction is active, removes the given StateManger from all
     * caches, otherwise just from the Version Consistency cache.
     * @param sm StateManager to remove
     */
    private void removeFromCaches(StateManager sm) {
        if (sm != null) {
            if (_activeTransaction) {
                // RESOLVE: Duplicates are not removed!
                _txCache.remove(sm);
                _flushedCache.remove(sm);
            }
            removeFromVersionConsistencyCache(sm);
        }
    }


    /**
     * Called by Transaction commit(). Flushes dirty instances to the store.
     * Clean instances registered for Version Consistency are verified with
     * the store.
     */
    public void beforeCompletion() {
        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest("sqlstore.persistencemgr.beforecompletion"); // NOI18N
        }

        assertIsOpen();
        assertActiveTransaction(false);
        _insideCommit = true;

        prepareToUpdate();

        try {
            flushTxCache();

            // Verify version consistent instances on commit only.
            if (!_insideFlush) {
                verifyFlushedCache();
            }
        } catch (JDODataStoreException ex) {

            // If an instance failed to flush, remember to cleanup the Version
            // Consistency cache (see afterCompletion).
            cleanupVersionConsistencyCache = true;
            throw ex;
        }
    }

    /**
     * Calls the state manager's prepare to update phases I-III.
     * Phase II and III are called during commit processing only.
     *
     * @see StateManager#prepareToUpdatePhaseI
     * @see StateManager#prepareToUpdatePhaseII
     * @see StateManager#prepareToUpdatePhaseIII
     */
    private void prepareToUpdate() {
        for (int i = 0; i < _txCache.size(); i++) {
            StateManager sm = (StateManager)_txCache.get(i);
            // NOTE: prepareToUpdatePhaseI has the side-effect of adding more objects
            // to the transaction cache.

            sm.prepareToUpdatePhaseI();
        }

        // We only do phase 2 and 3 during commit only.
        if (!_insideFlush) {
            HashSet phase3sms = new HashSet();

            for (Iterator iter = _flushedCache.iterator(); iter.hasNext(); ) {
                StateManager sm = (StateManager)iter.next();
                // NOTE: prepareToUpdatePhaseII has the side-effect of adding state managers
                // to the phase3sms HashSet which need to have prepareToUpdatePhaseIII()
                // called on them

                sm.prepareToUpdatePhaseII(phase3sms);
            }

            Iterator iter = phase3sms.iterator();

            // phase3sms should contain all the non-reachable autopersistence instance.
            // We need to call prepareToUpdatePhaseIII on them to make sure we roll
            // back any changes that may have been flushed to the datastore.

            while (iter.hasNext()) {
                StateManager sm = (StateManager) iter.next();
                sm.prepareToUpdatePhaseIII();
            }
        }
    }

    /**
     * Writes the instances from the transactional cache to the store.
     * The transactional cache contains modified instances only.
     *
     * @exception JDOUserException if instances can't be flushed
     *  because of circular dependencies.
     */
    private void flushTxCache() {
        List err = flushToDataStore(_txCache);

        // Try to resolve dependencies.
        if (err != null && err.size() > 0) {
            Iterator iter = err.iterator();
            while (iter.hasNext()) {
                ((StateManager) iter.next()).resolveDependencies();
            }
            // Second flush.
            err = flushToDataStore(err);
        }

        if (err != null && err.size() > 0) {
            _transaction.setRollbackOnly();
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.notprocessed"), // NOI18N
                    toPCArray(err));
        }
    }

    /**
     * Loops through flushed cache and calls PersistentStore.verifyPersistent()
     * on each instance. The flushed cache contains all instances accessed in
     * this transaction. To prevent database deadlocks, it's important to
     * iterate the flushed cache in the order the instances were used in the
     * transaction, as garanteed by {@link LinkedHashSet}.
     *
     * @exception JDODataStoreException if a cached instance has been updated
     * from outside.
     */
    private void verifyFlushedCache() {
        Iterator iter = _flushedCache.iterator();

        while (iter.hasNext()) {
            StateManager sm = (StateManager)iter.next();

            if (sm.hasVersionConsistency() && !sm.verifyPersistent()) {
                Object [] items = { sm.getPersistent() };

                // The instance failed the verification with the data store.
                sm.setVerificationFailed();
                throw new JDODataStoreException(I18NHelper.getMessage(messages,
                        "jdo.persistencemanagerimpl.verificationfailed"), items);  // NOI18N
            }
        }
    }

    /**
     * Writes the instances in <code>flushList</code> to the data store.
     * Loops through the list and calls StateManager.updatePersistent()
     * on each instance.
     *
     * @param flushList List of state managers to be flushed.
     * @return List containing state managers not flushed
     *   because of unresolved dependencies, null if all
     *   instances could be processed.
     */
    static private List flushToDataStore(List flushList) {
        int size = flushList.size();
        List errorList = null;

        // The connection initialisation is not neccessary. There
        // are two conditions in TransactionImpl assuring connections
        // are not released in releaseConnections,
        // even if the internal reference count on the connection is 0:
        // - we are in the commit processing
        // - in a non managed environment, connections aquired for queries in pessimistic
        // transactions are not released until commit
        // Please refer to TransactionImpl.releaseConnection
        for (int i = 0; i < size; i++) {
            StateManager sm = (StateManager)flushList.get(i);
            StateManager smNext =
                (i+1 < size)? (StateManager)flushList.get(i+1) : null;
            sm.updatePersistent(smNext);
        }

        for (int i = 0; i < size; i++) {
            StateManager sm = (StateManager)flushList.get(i);
            if (!sm.isProcessed()) {
                if (errorList == null) {
                    errorList = new ArrayList();
                }

                // Dependencies have not been resolved.
                errorList.add(sm);
            }
        }
        return errorList;
    }

    /**
     * Converts the list <code>smList</code> of state managers into
     * an Array of persistence capable instances.
     *
     * @param smList List of state managers.
     * @return Array of persistence capable instances.
     */
    static private Object[] toPCArray(List smList) {
        final int size = smList.size();
        if (size > 0) {
            List pcList = new ArrayList(size);

            for (int i = 0; i < size; i++) {
                StateManager sm = (StateManager)smList.get(i);
                pcList.add(sm.getPersistent());
            }
            return pcList.toArray();
        }
        return null;
    }

    /**
     * Called by Transaction commit() or rollback()
     * cleans up transactional cache
     * @param	status		javax.transaction.Status
     */
    public void afterCompletion(int status) {
        assertIsOpen();
        _insideCommit = true;
        boolean abort = ((status == Status.STATUS_ROLLEDBACK) ||
                (status == Status.STATUS_ROLLING_BACK) ||
                (status == Status.STATUS_MARKED_ROLLBACK));
        boolean debug = false;
        debug = logger.isLoggable(Logger.FINEST);
        if (debug)
            logger.finest("sqlstore.persistencemgr.aftercompletion",new Boolean(abort)); // NOI18N

        boolean retainValues = _transaction.getRetainValues();

        for (Iterator iter = _flushedCache.iterator(); iter.hasNext(); ) {
            StateManager sm = (StateManager)iter.next();
            if (debug)
                logger.finest("sqlstore.persistencemgr.aftercompletion.process",sm.getObjectId()); // NOI18N

            if (abort) {
                rollback(sm, retainValues);
            } else {
                commit(sm, retainValues);
            }
        }

        // Clear the transactional caches
        _txCache.clear();
        _flushedCache.clear();

        _insideCommit = false;
        cleanupVersionConsistencyCache = false;
    }

    /**
     * Commits the given StateManager instance after first adding it to the
     * Version Consistency cache (if necessary).
     * @param sm Instance to be comitted and possibly added to the cache.
     * @param retainValues as per the current transaction.
     */
    private void commit(StateManager sm, boolean retainValues) {

        if (sm.needsUpdateInVersionConsistencyCache()) {
            StateManager nonTxSM = lookupFromVersionConsistencyCache(sm);

            if (null != nonTxSM) {
                nonTxSM.copyFields(sm);
            } else {
                addToVersionConsistencyCache(sm);
            }
        }
        sm.commit(retainValues);
    }

    /**
     * Does a rollback on the given StateManager instance after first removing
     * it from the Version Consistency cache (if necessary).
     * @param sm Instance to be rolled back and possibly removed from the
     * cache.
     * @param retainValues as per the current transaction.
     */
    private void rollback(StateManager sm, boolean retainValues) {
        if (cleanupVersionConsistencyCache && sm.isVerificationFailed()) {
            removeFromVersionConsistencyCache(sm);
        }
        sm.rollback(retainValues);
    }

    /**
     * Adds given instance to the Version Consistency cache, if the
     * instance supports Version Consistency.
     * @param sm Instance to be added to the cache.
     * @return If an instance was already in the cache for the given
     * <code>sm</code>, it is returned; otherwise null.
     */
    private StateManager addToVersionConsistencyCache(StateManager sm) {
        StateManager rc = null;

        if (null != sm && sm.hasVersionConsistency()) {
            Class pcType = sm.getPersistent().getClass();
            Object oid = sm.getObjectId();
            VersionConsistencyCache vcCache =
                    persistenceManagerFactory.getVersionConsistencyCache();

            if (vcCache.get(pcType, oid) == null) {
                StateManager nonTxSM = createStateManager(pcType);

                nonTxSM.copyFields(sm);
                nonTxSM.setPersistenceManager(null); // Disconnect SM from PM

                rc = vcCache.put(pcType, oid, nonTxSM);
            }
        }
        return rc;
    }

    /**
     * Removes given instance from the Version Consistency cache, if the
     * instance supports Version Consistency.
     * @param sm Instance to be removed from the cache.
     * @return If an instance was already in the cache for the given
     * <code>sm</code>, it is returned; otherwise null.
     */
    private StateManager removeFromVersionConsistencyCache(StateManager sm) {
        StateManager rc = null;

        if (null != sm && sm.hasVersionConsistency()) {
            Class pcType = sm.getPersistent().getClass();
            Object oid = sm.getObjectId();
            VersionConsistencyCache vcCache =
                    persistenceManagerFactory.getVersionConsistencyCache();
            rc = vcCache.remove(pcType, oid);

            if (null == rc) {
                // XXX should not happen; throw exception?
            }
        }
        return rc;
    }

    /**
     * @inheritDoc
     */
    public boolean initializeFromVersionConsistencyCache(StateManager sm) {
        boolean rc = false;
        StateManager nonTxSM = lookupFromVersionConsistencyCache(sm);

        if (null != nonTxSM) {
            rc = true;

            // Synchronize so that no other threads change/access the
            // cache'd nonTxSm while copying fields.
            synchronized (nonTxSM) {
                sm.copyFields(nonTxSM);
            }
            sm.initialize(true);
        }
        return rc;
    }

    /**
     * Looks up given instance from the Version Consistency cache, if the
     * instance supports Version Consistency.
     * @param sm Instance to be looked up from the cache.
     * @return If an instance was already in the cache for the given
     * <code>sm</code>, it is returned; otherwise null.
     */
    private StateManager lookupFromVersionConsistencyCache(StateManager sm) {
        StateManager rc = null;

        if (null != sm && sm.hasVersionConsistency()) {
            Class pcType = sm.getPersistent().getClass();
            Object oid = sm.getObjectId();
            VersionConsistencyCache vcCache =
                persistenceManagerFactory.getVersionConsistencyCache();

            rc = vcCache.get(pcType, oid);
        }
        return rc;
    }

    public void setStateManager(Object pc, StateManager sm) {
        if (pc instanceof PersistenceCapable) {
            ((PersistenceCapable) pc).jdoSetStateManager(sm);
        }

        //RESOLVE: Otherwise, should throw an exception.
    }


    public void setFlags(Object pc, byte flags) {
        if (pc instanceof PersistenceCapable) {
            ((PersistenceCapable) pc).jdoSetFlags(flags);
        }

        //RESOLVE: Otherwise, should throw an exception.
    }

    public byte getFlags(Object pc) {
        if (pc instanceof PersistenceCapable) {
            return ((PersistenceCapable) pc).jdoGetFlags();
        }

        return 0;
        //RESOLVE: Otherwise, should throw an exception.
    }

    public StateManager getStateManager(Object pc) {
        if (pc instanceof PersistenceCapable) {
            return ((PersistenceCapable) pc).jdoGetStateManager();
        }

        return null;
        //RESOLVE: Otherwise, should throw an exception.
    }


    public void setField(Object o, int fieldNumber, Object value) {
        if (o instanceof PersistenceCapable) {
            PersistenceCapable pc = (PersistenceCapable) o;
            pc.jdoSetField(fieldNumber, value);
        }

        //RESOLVE: Otherwise, should throw an exception.
    }

    public Object getField(Object pc, int fieldNumber) {
        if (pc instanceof PersistenceCapable) {
            return ((PersistenceCapable) pc).jdoGetField(fieldNumber);
        }

        //RESOLVE: Otherwise, should throw an exception.
        return null;
    }

    public void clearFields(Object pc) {
        if (pc instanceof PersistenceCapable) {
            ((PersistenceCapable) pc).jdoClear();
        }
    }

    /**
     * Returns a new Second Class Object instance of the type specified,
     * with the owner and field name to notify upon changes to the value
     * of any of its fields. If a collection class is created, then the
     * class does not restrict the element types, and allows nulls to be added as elements.
     *
     * @param type Class of the new SCO instance
     * @param owner the owner to notify upon changes
     * @param fieldName the field to notify upon changes
     * @return the object of the class type
     */
    public Object newSCOInstance(Class type, Object owner, String fieldName) {
        Object obj = null;

        if (Collection.class.isAssignableFrom(type)) {
            obj = this.newCollectionInstanceInternal(type, owner, fieldName, null, true, 0);
        } else {
            obj = newSCOInstanceInternal(type, owner, fieldName);

        }

        this.replaceSCO(fieldName, owner, obj);


        return obj;
    }

    /**
     * Called by newSCOInstance from the public interface or internally
     * by the runtime
     * Will not result in marking field as dirty
     *
     * Returns a new Second Class Object instance of the type specified,
     * @param type Class of the new SCO instance
     * @param owner the owner to notify upon changes
     * @param fieldName the field to notify upon changes
     * @return the object of the class type
     */
    public Object newSCOInstanceInternal(Class type, Object owner, String fieldName) {

        Object obj = null;

        if (type == java.sql.Date.class
                || type == com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlDate.class) {
            obj = new com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlDate(owner, fieldName);

        } else if (type == java.sql.Time.class
                || type == com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlTime.class) {
            obj = new com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlTime(owner, fieldName);

        } else if (type == java.sql.Timestamp.class
                || type == com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlTimestamp.class) {
            obj = new com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlTimestamp(owner, fieldName);

        } else if (type == com.sun.jdo.spi.persistence.support.sqlstore.sco.Date.class
                || Date.class.isAssignableFrom(type)) {
            obj = new com.sun.jdo.spi.persistence.support.sqlstore.sco.Date(owner, fieldName);

        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.newscoinstance.wrongclass", // NOI18N
                    type.getName()));
        }

        return obj;
    }


    /**
     * Returns a new Collection instance of the type specified, with the
     * owner and field name to notify upon changes to the value of any of its fields.
     * The collection class restricts the element types allowed to the elementType or
     * instances assignable to the elementType, and allows nulls to be added as
     * elements based on the setting of allowNulls. The Collection has an initial size
     * as specified by the initialSize parameter.
     * We choose to use HashSet as a default collection (no specific type is chosen)
     * because we do not support duplicate objects in DB
     *
     * @param type Class of the new SCO instance
     * @param owner the owner to notify upon changes
     * @param fieldName the field to notify upon changes
     * @param elementType the element types allowed
     * @param allowNulls true if allowed
     * @param initialSize initial size of the Collection
     * @return the object of the class type
     */
    public Object newCollectionInstance(Class type, Object owner, String fieldName,
                                        Class elementType, boolean allowNulls, int initialSize) {
        Object obj = newCollectionInstanceInternal(type, owner, fieldName,
                elementType, allowNulls, initialSize);

        this.replaceSCO(fieldName, owner, obj);

        return obj;
    }

    /**
     * Called by newCollectionInstance from the public interface or internally
     * by the runtime
     * Will not result in marking field as dirty
     *
     * @see #newCollectionInstance for more information
     * @param type Class of the new SCO instance
     * @param owner the owner to notify upon changes
     * @param fieldName the field to notify upon changes
     * @param elementType the element types allowed
     * @param allowNulls true if allowed
     * @param initialSize initial size of the Collection
     * @return the object of the class type
     */
    public Object newCollectionInstanceInternal(Class type, Object owner, String fieldName,
                                                Class elementType, boolean allowNulls, int initialSize) {
        Object obj = null;

        // Make sure that the order of type comparison will go from
        // narrow to wide:
        if (type == HashSet.class
                || type == com.sun.jdo.spi.persistence.support.sqlstore.sco.HashSet.class) {
            if (initialSize == 0)
                initialSize = 101;
            obj = new com.sun.jdo.spi.persistence.support.sqlstore.sco.HashSet(
                owner, fieldName, elementType, allowNulls, initialSize);
/*
        } else if (type == Vector.class
                || type == com.sun.jdo.spi.persistence.support.sqlstore.sco.Vector.class) {
            newType = com.sun.jdo.spi.persistence.support.sqlstore.sco.Vector.class;
        } else if (type == ArrayList.class
                || type == com.sun.jdo.spi.persistence.support.sqlstore.sco.ArrayList.class) {
            newType = com.sun.jdo.spi.persistence.support.sqlstore.sco.ArrayList.class;
        } else if (List.class.isAssignableFrom(type)) {
            newType = com.sun.jdo.spi.persistence.support.sqlstore.sco.Vector.class;
*/
        } else if (Set.class.isAssignableFrom(type)) {
            if (initialSize == 0)
                initialSize = 101;
            obj = new com.sun.jdo.spi.persistence.support.sqlstore.sco.HashSet(
                owner, fieldName, elementType, allowNulls, initialSize);

        } else if (Collection.class.isAssignableFrom(type)) {
            // We choose to use HashSet as a default collection
            // because we do not support duplicate objects in DB
            if (initialSize == 0)
                initialSize = 101;
            obj = new com.sun.jdo.spi.persistence.support.sqlstore.sco.HashSet(
                owner, fieldName, elementType, allowNulls, initialSize);

        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.newscoinstance.wrongclass", // NOI18N
                    type.getName()));
        }
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug)
            logger.finest("sqlstore.persistencemgr.newcollection",obj.getClass()); // NOI18N

        return obj;
    }

    /**
     * Called by Query to flush updates to the database
     * in pessimistic transaction. Calls internaly beforeCompletion() to do actual
     * flush
     * @see #beforeCompletion()
     */
    public void internalFlush() {
        acquireExclusiveLock();

        try {
            //
            // Only flush if we are not in optimistic transaction.
            //
            if (_optimistic == false) {
                //
                // Note: no need to lock _cacheLock because we already have
                // exclusive using of the _weakCache and _txCache due to
                // the exclusive lock.
                //

                _insideFlush = true;
                beforeCompletion();
                _insideCommit = false;

                int status = _transaction.getStatus();

                if ((status == Status.STATUS_ROLLEDBACK) ||
                        (status == Status.STATUS_ROLLING_BACK) ||
                        (status == Status.STATUS_MARKED_ROLLBACK)) {
                    return; // it is user's responsibility to rollback the transaction
                    //_transaction.rollback();
                } else {
                    for (int i = 0; i < _txCache.size(); i++) {
                        StateManager sm = (StateManager)_txCache.get(i);
                        sm.flushed();
                    }
                }
                _insideFlush = false;

                // Clear the dirty cache
                _txCache.clear();
            }
        } finally {
            releaseExclusiveLock();
        }
    }

    /**
     * For Transaction to notify PersistenceManager that
     * status is changed
     */
    public synchronized void notifyStatusChange(boolean isActive) {
        _activeTransaction = isActive;
    }

    /**
     * For Transaction to notify PersistenceManager that
     * optimistic flag is changed
     */
    public synchronized void notifyOptimistic(boolean optimistic) {
        this._optimistic = optimistic;
    }

    /**
     * Returns true if associated transaction is optimistic
     */
    public boolean isOptimisticTransaction() {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug)
            logger.finest("sqlstore.persistencemgr.isoptimistic", new Boolean(_optimistic)); // NOI18N

        return _optimistic;
    }


    /**
     * For Transaction to notify PersistenceManager that
     * nontransactionalRead flag is changed
     */
    public synchronized void notifyNontransactionalRead(boolean nontransactionalRead) {
        this._nontransactionalRead = nontransactionalRead;
    }

    /**
     * Returns true if associated transaction is optimistic
     */
    public boolean isNontransactionalRead() {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            logger.finest("sqlstore.persistencemgr.isnontxread",new Boolean(_nontransactionalRead)); // NOI18N
        }

        return _nontransactionalRead;
    }


    /**
     * Returns true if associated transaction is active
     */
    public boolean isActiveTransaction() {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug)
             logger.finest("sqlstore.persistencemgr.isactivetx",new Boolean(_activeTransaction)); // NOI18N

        return _activeTransaction;
    }

    // Returns current wrapper
    public PersistenceManagerWrapper getCurrentWrapper() {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            logger.finest("sqlstore.persistencemgr.getcurrentwrapper",current); // NOI18N
        }

        return current;
    }





    // Remember the current wrapper
    protected void pushCurrentWrapper(PersistenceManagerWrapper pmw) {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            Object[] items = new Object[] {current,pmw};
            logger.finest("sqlstore.persistencemgr.pushcurrentwrapper",items); // NOI18N
        }

        current = pmw;
    }

    // Replace current wrapper with the previous
    protected void popCurrentWrapper(PersistenceManagerWrapper prev) {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            Object[] items = new Object[] {current,prev};
            logger.finest("sqlstore.persistencemgr.popcurrentwrapper",items); // NOI18N
        }

        current = prev;
        if (!_isClosed && _jta == null && current == null) {
            this.close();
        }
    }

    /**
     * Assigns reference to javax.transaction.Transaction associated
     * with the current thread in the managed environment
     */
    protected void setJTATransaction(javax.transaction.Transaction t) {
        if (this._jta != null) {
            Object[] items = new Object[] {this._jta, t};
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.setjtatransaction.notnulljta", // NOI18N
                    items));
        }
        this._jta = t;
    }

    /** Add a new created query instance to the queries collection. */
    private void registerQuery(QueryImpl q)
    {
        acquireExclusiveLock();

        try {
            queries.add(q);
        } finally {
            releaseExclusiveLock();
        }
    }

    /**
     * Disconnects all query instances created for this pm and nullifies the
     * collection. This is to allow this pm to be gargabe collected.
     */
    private void disconnectQueries()
    {
        for (Iterator i = queries.iterator(); i.hasNext();)
        {
            QueryImpl q = (QueryImpl)i.next();
            q.clearPersistenceManager();
        }
        queries.clear();
        queries = null;
    }

    /** --------------Private Methods--------------  */

    /**
     * Replace previous value of the SCO field with the newly created
     *
     * @param fieldName the field to notify upon changes
     * @param owner the owner to notify upon changes
     * @param obj new SCO Instance
     */
    private void replaceSCO(String fieldName, Object owner, Object obj) {
        if (owner instanceof PersistenceCapable) {
            acquireShareLock();

            try {
                // Assign this SCO Collection to owner as reference
                PersistenceCapable pc = (PersistenceCapable) owner;
                StateManager sm = pc.jdoGetStateManager();

                if (obj instanceof SCOCollection) {
                    acquireFieldUpdateLock();
                    try {
                        sm.replaceObjectField(fieldName, obj);
                    } finally {
                        releaseFieldUpdateLock();
                    }
                } else {
                    sm.replaceObjectField(fieldName, obj);
                }
            } finally {
                releaseShareLock();
            }
        }
    }

    private Object internalGetObjectId(StateManager sm) {
        Object oid = sm.getObjectId();

        // create a copy
        return internalCloneOid(oid, sm);
    }

    private void internalMakePersistent(PersistenceCapable pc) {
        //
        // We need to lock _fieldUpdateLock here because of
        // the persitence-by-reacheability algorithm that can
        // touch other instances.
        // RESOLVE: We can optimize here to not have to lock
        // _fieldUpdateLock if the instance does not contain any
        // relationship field.
        //
        acquireFieldUpdateLock();
        try {
            synchronized (pc) {
                StateManager sm = null;

                if (pc.jdoIsPersistent()) {
                    sm = pc.jdoGetStateManager();

                    if (this != pc.jdoGetStateManager().getPersistenceManagerInternal()) {
                        throw new JDOUserException(I18NHelper.getMessage(messages,
                                "jdo.persistencemanagerimpl.another_pm"), // NOI18N
                                new Object[]{pc});
                    }
                } else {
                    Class classType = pc.getClass();
                    loadPersistenceConfig(classType);
                    sm = _store.getStateManager(classType);
                }

                sm.makePersistent(this, pc);
            }
        } finally {
            releaseFieldUpdateLock();
        }
    }

    private void internalDeletePersistent(PersistenceCapable pc) {
        if (!(pc.jdoIsPersistent())) {
            throw new JDOException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.internaldeletepersistent.transient"), // NOI18N
                    new Object[]{pc});
        }

        StateManager sm = pc.jdoGetStateManager();
        PersistenceManager pm = (PersistenceManager) sm.getPersistenceManagerInternal();

        if (this != pm) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.another_pm"), // NOI18N
                    new Object[]{pc});
        }

        if (!pc.jdoIsDeleted()) {
            //
            // Synchronization is done in the state manager.
            //
            sm.deletePersistent();
        }
    }

    /**
     * Load metadata for the given OID Object
     * @param oid  the Oid
     * @return classtype for the owning Class
     */
    private Class loadClassForOid(Object oid) {
        Class oidClass = oid.getClass();
        Class classType = _store.getClassByOidClass(oidClass);
        if (classType != null) {
            // Found in the DataStore
            return classType;
        }

        // loadDirectory(oidClass.getName());
        // loadClassByMethod(oid, oidClass);
        loadByName(oidClass.getName(), oidClass.getClassLoader());

        return _store.getClassByOidClass(oidClass);
    }

    /**
     * Load meta data for Object Class from package info
     *
     * @param s     OID class name as String
     * @param classLoader the classLoader of the oid class
     */
    private void loadByName(String s, ClassLoader classLoader) {
        int l = s.length();

        if (l < 4) {
            // Does not fit the pattern
            return;
        }

        String s1 = s.substring(l - 3);

        if (s1.equalsIgnoreCase(oidName_OID) &&
                (s.charAt(l - 4) == '.' || s.charAt(l - 4) == '$')) {
            s = s.substring(0, l - 4);
        } else if (s1.equalsIgnoreCase(oidName_KEY)) {
            s = s.substring(0, l - 3);
        } else {
            return;
        }

        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug)
            logger.finest("sqlstore.persistencemgr.loadingclass",s); // NOI18N


        Class oidClass = null;

        try {
            // take current class loader if not specified
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }
            oidClass = Class.forName(s, true, classLoader);

        } catch (Exception e) {
            throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.loadclassforoid.wrongoidclass"), e);// NOI18N
        }

        loadPersistenceConfig(oidClass);
    }

    /**
     * assert this PM instance is open
     */
    private void assertIsOpen() {
        if (_isClosed) {
            boolean debug = logger.isLoggable(Logger.FINEST);
            if (debug) {
                logger.finest("sqlstore.persistencemgr.assertisopen",this); // NOI18N
            }
            throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.assertclosed.closed"));// NOI18N
        }
    }

    /**
     * assert that the associated Transaction is active but allows to do commit processing.
     */
    private void assertActiveTransaction(boolean insideQuery) {
        boolean debug = false;

        debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
            logger.finest("sqlstore.persistencemgr.assertactivetx",_transaction); // NOI18N
        }

        if (_insideCommit || (insideQuery && _transaction.getNontransactionalRead()))
            return;

        if (!_activeTransaction) {
            if (debug) {
                logger.finest("sqlstore.persistencemgr.assertactivetx.closed",this); // NOI18N
            }
            throw new JDOException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.assertactivetransaction.error"));// NOI18N
        }
    }

    /**
     * assert Object is PersistenceCapable
     */
    private void assertPersistenceCapable(Object pc) {
        if (!(pc instanceof PersistenceCapable)) {
            throw new JDOException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.assertpersistencecapable.error", // NOI18N
                    pc.getClass().getName()), new Object[]{pc});
        }
    }

    /**
     * Set key field values from Oid into the Object
     *
     * @param sm	StateManager of the Object to set key field values to
     */
    private void setKeyFields(StateManager sm) {
        Object o = sm.getPersistent();
        if (o == null)
            return;

        Object oid = sm.getObjectId();
        try {
            // Set key field vaues and mark them as present
            PersistenceConfig config = sm.getPersistenceConfig();
            Field keyFields[] = config.getKeyFields();
            String keynames[] = config.getKeyFieldNames();
            for (int i = 0; i < keyFields.length; i++) {
                Field keyField = keyFields[i];
                sm.makePresent(keynames[i], keyField.get(oid));
            }

        } catch (Exception e) {
            //e.printStackTrace();
            boolean debug = logger.isLoggable(Logger.FINEST);
            if (debug)
                logger.finest("sqlstore.persistencemgr.setkeyfields",e); // NOI18N
        }
    }

    private PersistenceConfig loadPersistenceConfig(Class classType) {
        return _store.getPersistenceConfig(classType);
    }

    /**
     * Creates local copy of an oid object
     * @param oid 	original object
     * @param sm 	StateManager to be used for the field info.
     * @return 	object 	local copy
     */
    private Object internalCloneOid(Object oid, StateManager sm) {
        if (oid == null)
            return null;

        if (!_requireCopyObjectId) {
            return oid;
        }

        boolean debug = logger.isLoggable(Logger.FINEST);

        Object newoid = null;
        try {
            Class oidClass = oid.getClass();
            newoid = oidClass.newInstance();
            PersistenceConfig config = sm.getPersistenceConfig();
            Field keyFields[] = config.getKeyFields();

            // Copy key field vaues
            for (int i = 0; i < keyFields.length; i++) {
                Field keyField = keyFields[i];
                keyField.set(newoid, keyField.get(oid));
            }

        } catch (Exception e) {
            //e.printStackTrace();
            if (debug)
                logger.finest("sqlstore.persistencemgr.internalcloneoid",e); // NOI18N
            newoid = null;
        }

        if (debug)
            {
            Object[] items = new Object[] {oid , newoid, new Boolean((oid == newoid))};
            logger.finest("sqlstore.persistencemgr.internalcloneoid.old",items); // NOI18N
            }

        return newoid;
    }


    /**
     * Acquires a share lock from the persistence manager. This method will
     * put the calling thread to sleep if another thread is holding the exclusive lock.
     */
    public void acquireShareLock() {
        if ( ! _multithreaded) {
            return;
        }

        boolean debug = logger.isLoggable(Logger.FINEST);

        synchronized (_readWriteLock) {
            //
            // If the current thread is already holding the exclusive lock,
            // we simply grant the share lock without incrementing
            // the counter.
            //
            if ((_readWriteCount < 0) &&
                    (_exclusiveLockHolder == Thread.currentThread())) {
                return;
            }

            //
            // If _readWriteCount is negative, it means a thread is holding the exclusive lock.
            // We simply put this thread to sleep and wait for it to be notified by the
            // thread releasing the exclusive lock.
            //
            while (_readWriteCount < 0) {
                _waiterCount++;

                try {
                    if (debug) {
                        logger.finest("sqlstore.persistencemgr.acquiresharedlock",Thread.currentThread()); // NOI18N
                    }

                    _readWriteLock.wait();
                } catch (InterruptedException e) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "jdo.persistencemanagerimpl.acquiresharelock.interrupted"), e);// NOI18N
                } finally {
                    _waiterCount--;
                }
            }

            try {
                //
                // Make sure no one has closed the pm.
                //
                assertIsOpen();

            } catch (JDOException ex) {
                //
                // If _readWriteCount is 0, it means that no thread is holding a share
                // or exclusive lock. If there is a thread waiting, we wake it up by
                // notifying it.
                //
                if (_readWriteCount == 0 && _waiterCount > 0) {
                    _readWriteLock.notify();
                }
                throw ex;
           }

            _readWriteCount++;
            if (debug) {
              logger.finest("sqlstore.persistencemgr.acquiresharedlock.rdwrcount", // NOI18N
                   Thread.currentThread(),new Long(_readWriteCount));
            }

            if (_readWriteCount <= 0) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "jdo.persistencemanagerimpl.acquiresharelock.failed"));// NOI18N
            }
        }
    }

    /**
     * Releases the share lock and notify any thread waiting to get an exclusive lock.
     * Note that every releaseShareLock() call needs to be preceeded by an acquireShareLock() call.
     */
    public void releaseShareLock() {
        if ( ! _multithreaded) {
            return;
        }

        boolean debug = logger.isLoggable(Logger.FINEST);


        synchronized (_readWriteLock) {
            //
            // If the current thread is already holding the exclusive lock,
            // we simply release the share lock without decrementing
            // the counter.
            //
            if ((_readWriteCount < 0) &&
                    (_exclusiveLockHolder == Thread.currentThread())) {
                return;
            }

            try {
                if (_readWriteCount == 0) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "jdo.persistencemanagerimpl.releasesharelock.failed"));// NOI18N
                }

                _readWriteCount--;
            } finally {
                //
                // If _readWriteCount is 0, it means that no thread is holding a share
                // or exclusive lock. If there is a thread waiting, we wake it up by
                // notifying it.
                //
                if ((_waiterCount > 0) && (_readWriteCount == 0)) {
                    _readWriteLock.notify();
                }

                if (debug) {
                    Object[] items = new Object[] {Thread.currentThread(),new Long(_readWriteCount)};
                    logger.finest("sqlstore.persistencemgr.releasesharedlock",items); // NOI18N
                }
            }
        }
    }

    /**
     * Acquires an exclusive lock from the persistence manager. By acquiring an
     * exclusive lock, a thread is guaranteed to have exclusive right to the persistence
     * runtime meaning no other threads can perform any operation in the sqlstore.
     * NOTE: This implementation does not detect if a thread holding a share lock
     * attempts to acquire an exclusive lock. It is up to the callers to make sure
     * this does not happen.
     */
    public void acquireExclusiveLock() {
        if ( ! _multithreaded) {
            return;
        }

        boolean debug = logger.isLoggable(Logger.FINEST);

        synchronized (_readWriteLock) {
            Thread currentThread = Thread.currentThread();

            //
            // If the current thread already holds the exclusive lock, we simply
            // decrement _readWriteCount to indicate the current level of the exclusive
            // lock.
            //
            if (currentThread == _exclusiveLockHolder) {
                _readWriteCount--;
            } else {
                //
                // If _readWriteCount is not 0, it means that there is either a share lock
                // or an exclusive outstanding. We simply put the current thread to sleep
                // any wait for it to be notified by the thread releasing the lock.
                //
                while (_readWriteCount != 0) {
                    _waiterCount++;

                    try {

                        if (debug) {
                             logger.finest("sqlstore.persistencemgr.acquireexclusivelock",currentThread); // NOI18N
                        }

                        _readWriteLock.wait();
                    } catch (InterruptedException e) {
                        throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                                "jdo.persistencemanagerimpl.acquireexclusivelock.interrupted"), e);// NOI18N
                    } finally {
                        _waiterCount--;
                    }
                }

                try {
                    //
                    // Make sure no one has closed the pm.
                    //
                    assertIsOpen();

                } catch (JDOException ex) {
                    //
                    // If _readWriteCount is 0 and _waiterCount is greater than 0,
                    // we need to notify a thread waiting to acquire a lock.
                    //
                    if (_readWriteCount == 0 && _waiterCount > 0) {
                        _readWriteLock.notify();
                    }
                    throw ex;
               }

                _readWriteCount = -1;
                _exclusiveLockHolder = currentThread;

                if (debug) {
                    Object[] items = new Object[] {currentThread,new Long(_readWriteCount)};
                    logger.fine("sqlstore.persistencemgr.acquireexclusivelock.count",items); // NOI18N
                }

            }
        }
    }

    /**
     * Release the exclusive lock and notify any thread waiting to get an exclusive or
     * share lock. Note that every releaseShareLock() call needs to be preceeded by
     * an acquireExclusiveLock() call.
     */
    public void releaseExclusiveLock() {
        if ( ! _multithreaded) {
            return;
        }

        boolean debug = logger.isLoggable(Logger.FINEST);


        synchronized (_readWriteLock) {
            try {
                if (_readWriteCount >= 0) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "jdo.persistencemanagerimpl.releaseexclusivelock.failed"));// NOI18N
                }

                _readWriteCount++;
            } finally {
                if (debug) {
                    Object[] items = new Object[] {Thread.currentThread(),new Long(_readWriteCount)};
                    logger.finest("sqlstore.persistencemgr.releaseexclusivelock",items); // NOI18N
                }

                //
                // If _readWriteCount is 0 and _waiterCount is greater than 0,
                // we need to notify a thread waiting to acquire a lock.
                //
                if (_readWriteCount == 0) {
                    if (_waiterCount > 0) {
                        _readWriteLock.notify();
                    }

                    _exclusiveLockHolder = null;
                }
            }
        }
    }

    /**
     * Acquire lock for synchronizing field updates.
     */
    public void acquireFieldUpdateLock() {
        _fieldUpdateLock.acquire();
    }

    /**
     * Release lock for synchronizing field updates.
     */
    public void releaseFieldUpdateLock() {
        _fieldUpdateLock.release();
    }

    /**
     * Lock cache for getObjectById and result processing synchronization.
     */
    public void acquireCacheLock() {
        _cacheLock.acquire();
    }

    /** Release cache lock.
     */
    public void releaseCacheLock() {
        _cacheLock.release();
    }

    /** --------------Inner Class--------------  */

    /**
     * Class that implements interface FilenameFilter.
     * Used to filter directory listings in the list method of class File.
     */
    static class ExtensionFilter implements FilenameFilter {
        private String ext;

        public ExtensionFilter(String ext) {
            this.ext = ext;
        }

        /**
         * Tests if a specified file should be included in a file list.
         * @param dir 	the directory in which the file was found
         * @param name	the name of the file
         * @return true if the name should be included in the file list
         */
        public boolean accept(File dir, String name) {
            return name.endsWith(ext);
        }
    }

}
