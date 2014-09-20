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
 * TransactionImpl.java
 *
 * Create on March 3, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.impl;

import javax.transaction.*;

import java.util.Hashtable;
import java.util.ArrayList;
import java.sql.Connection;
import javax.sql.DataSource;
import java.util.Locale;
import java.util.ResourceBundle;

import com.sun.jdo.api.persistence.support.ConnectionFactory;
import com.sun.jdo.api.persistence.support.JDOException;
import com.sun.jdo.api.persistence.support.JDOUnsupportedOptionException;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.api.persistence.support.JDODataStoreException;

import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManagerFactory;
import org.glassfish.persistence.common.I18NHelper;

import com.sun.jdo.spi.persistence.support.sqlstore.connection.ConnectionImpl;

import com.sun.jdo.spi.persistence.support.sqlstore.utility.*;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.EJBHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperTransaction;

/**
 *
 * The Transaction interface allows operations to be performed against
 * the transaction in the target Transaction object. A Transaction
 * object is created corresponding to each global transaction creation.
 * The Transaction object can be used for resource enlistment,
 * synchronization registration, transaction completion and status
 * query operations.
 *
 * This implementation is completely internal.
 * All externally documented methods are on the Transaction interface.
 *
 * Note on synchronized(this):
 *        There are a number of places that calls are made outside the
 *        transaction system.  For example, Synchronization.beforeCompletion()
 *        and XAResource.start().  It is important that no locks are held
 *        when these callbacks are made.  This requires more state checking
 *        up return from these calls, and the methods are careful to check
 *        state transitions after release the lock and reacquiring it.
 *
 *      Also be careful NOT to call into a lower method with the lock, if that
 *      method may call out of the transaction system.
 *
 *        Take care if you make methods "synchronized" because:
 *            synchronized methodName()
 *        is not the same lock as a more localized:
 *            synchronized(this)
 *
 *        This is tested in the regression tests (see "Lock Test")
 */
public class TransactionImpl
    implements com.sun.jdo.spi.persistence.support.sqlstore.Transaction {
    /**
     * Trace level for sh:6.
     *
     * This is public to this transaction package and referenced:
     *         if (TransactionImpl.tracing)
     * This is reset by calling TransactionImpl.setTrace().
     */
    static boolean  tracing;
    private static final int    TRACE_THREADS            = 0x01;
    private static final int    TRACE_RESOURCES            = 0x02;
    private static final int    TRACE_SYNCHRONIZATIONS    = 0x04;
    private static final int    TRACE_ONE_PHASE            = 0x08;

    /**
     * Package-visible lock for static attributes.
     *
     * Note that globalLock can be a higher-level lock, in that it may be
     * locked before other lower-level objects are locked (i.e. the
     * transaction object).  It may NOT be locked the other way round.
     */
    static String    globalLock = "TranGlobalLock"; // NOI18N

    /**
     * Transaction status (from javax.transaction.Status).
     */
    private int            status;

    /**
     * Timeout for this transaction
     */
    private int         timeout;
    public static final int    TRAN_DEFAULT_TIMEOUT = 0;    // No timeout

    /**
     * Query and Update Statement timeouts for the datastore associated
     * with this transaction
     */
    private int         queryTimeout = 0;        // No limit
    private int         updateTimeout = 0;        // No limit

    /**
     * Number of threads participating in this transaction.
     */
    private int            threads;
    public static final int    TRAN_MAX_THREADS = 50;        // Public for test

    /**
     * The commit process has already begun (even though the status is still
     * STATUS_ACTIVE).  This is set during before-commit notification.
     */
    private boolean        startedCommit;

    /**
     * During prepare-processing we determine if we can commit this transaction
     * in one-phase. See check in commitPrepare().
     */
    private boolean        onePhase;

    /**
     * Array of registered Synchronization interfaces.
     */
    private    Synchronization    synchronization = null;

    /**
     * Array of registered Resource interfaces.
     */
    private    ArrayList    resources;
    private static final int    RESOURCE_START     = 0;
    private static final int    RESOURCE_END     = 1;

    /**
     * PersistenceManagerFactory associated with this transaction
     */
    private PersistenceManagerFactory pmFactory = null;

    /**
     * PersistenceManager associated with this transaction (1-1)
     */
    private PersistenceManager    persistenceManager     = null;

    /**
     * Connection Factory from which this transaction gets Connections
     */
    private Object     connectionFactory    = null;

    private javax.transaction.Transaction jta = null;

    /**
     * Type of the datasource. True if it javax.sql.DataSource
     */
    private boolean isDataSource = false;

    /** values for the datasource user and user password to access
     * security connections
     */
    private String         username     = null;
    private String         password     = null;

    /**
     * Associated Connection
     */
    private Connection         _connection         = null;

    /**
     * Number of users (or threads) currently using this connection.
     */
    private int _connectionReferenceCount = 0;

    /**
     * Flag that indicates how to handle objects after commit.
     * If true, at commit instances retain their values and the instances
     */
    private boolean retainValues = true;
    
    /**
     * Flag that indicates how to handle objects after roolback.
     * If true, at rollback instances restore their values and the instances
     */
    private boolean restoreValues = false;

    /**
     * Flag that indicates type of the transaction.
     * Optimistic transactions do not hold data store locks until commit time.
     */
    private boolean optimistic = true;

    /**
     * Flag that indicates if queries and navigation are allowed
     * without an active transaction
     */
    private boolean nontransactionalRead = true;

    /**
     * Possible values of txType
     */
    public static final int NON_MGD = 0;
    public static final int CMT = 1;
    public static final int BMT_UT = 2;
    public static final int BMT_JDO = 3;

    /**
     * Flag to indicate usage mode (non-managed vs. managed, etc.)
     */
    private int txType = -1;

    /**
     * The logger
     */
    private static Logger logger = LogHelperTransaction.getLogger();


    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            TransactionImpl.class);

    /**
     * Constructor
     */
    public TransactionImpl(PersistenceManager pm, String username, String password, int seconds) {
        this.status = Status.STATUS_NO_TRANSACTION;
        this.timeout = seconds;
        this.startedCommit = false;
        this.onePhase = false;
        this.resources = new ArrayList();

        persistenceManager = pm;
        this.username = username;
        this.password = password;

        pmFactory = (PersistenceManagerFactory)pm.getPersistenceManagerFactory();
        connectionFactory = pmFactory.getConnectionFactory();
        if (!(connectionFactory instanceof ConnectionFactory)) {
            isDataSource = true;
        }

        optimistic = pmFactory.getOptimistic();
        retainValues = pmFactory.getRetainValues();
        nontransactionalRead = pmFactory.getNontransactionalRead();
        queryTimeout = pmFactory.getQueryTimeout();
        updateTimeout = pmFactory.getUpdateTimeout();

    }

    /**
     * Set PersistenceManager
     */
    public void setPersistenceManager(PersistenceManager pm) {
    }

    /**
     * Returns PersistenceManager associated with this transaction
     */
    public com.sun.jdo.api.persistence.support.PersistenceManager getPersistenceManager() {
        return persistenceManager.getCurrentWrapper();
    }

    public boolean isActive() {
        return (this.status == Status.STATUS_ACTIVE ||
            this.status == Status.STATUS_MARKED_ROLLBACK);
    }

    public void setRetainValues(boolean flag) {
        //
        // First do a quick check to make sure the transaction is active.
        // This allows us to throw an exception immediately.
        // Cannot change flag to true inside an active pessimistic tx
        //
        if (isActive() && !optimistic && flag)
            throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.setoptimistic.notallowed")); // NOI18N

        //
        // Now get an exclusive lock so we can modify the retainValues flag.
        //
        persistenceManager.acquireExclusiveLock();

        try {
            // Cannot change flag to true inside an active pessimistic tx
            if (isActive() && !optimistic && flag)
                throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                    "transaction.transactionimpl.setoptimistic.notallowed")); // NOI18N

            this.retainValues = flag;

            // Adjust depending flags
            if (flag) {
                nontransactionalRead = flag;
                persistenceManager.notifyNontransactionalRead(flag);
            }
        } finally {
            persistenceManager.releaseExclusiveLock();
        }
    }

    public boolean getRetainValues() {
        return retainValues;
    }
    
    public void setRestoreValues(boolean flag) {
        //
        // First do a quick check to make sure the transaction is active.
        // This allows us to throw an exception immediately.
        // Cannot change flag to true inside an active tx
        //
        if (isActive())
            throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.setoptimistic.notallowed")); // NOI18N

        //
        // Now get an exclusive lock so we can modify the restoreValues flag.
        //
        persistenceManager.acquireExclusiveLock();

        try {
            // Cannot change flag to true inside an active  tx
            if (isActive())
                throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                    "transaction.transactionimpl.setoptimistic.notallowed")); // NOI18N

            this.restoreValues = flag;

        } finally {
            persistenceManager.releaseExclusiveLock();
        }
    }

    public boolean getRestoreValues() {
        return restoreValues;
    }
    

    public void setNontransactionalRead (boolean flag) {
        //
        // First do a quick check to make sure the transaction is active.
        // This allows us to throw an exception immediately.
        // Cannot change flag to false inside an active optimistic tx
        //
        if (isActive() && optimistic && !flag)
            throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.setoptimistic.notallowed")); // NOI18N

        //
        // Now get an exclusive lock so we can modify the nontransactionalRead flag.
        //
        persistenceManager.acquireExclusiveLock();

        try {
            // Cannot change flag to false inside an active optimistic tx
            if (isActive() && optimistic && !flag)
                throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                    "transaction.transactionimpl.setoptimistic.notallowed")); // NOI18N

            this.nontransactionalRead = flag;
            persistenceManager.notifyNontransactionalRead(flag);

            // Adjust depending flags
            if (flag == false) {
                retainValues = flag;
                optimistic = flag;

                // Notify PM about Tx type change
                persistenceManager.notifyOptimistic(flag);
            }
        } finally {
            persistenceManager.releaseExclusiveLock();
        }
    }

    public boolean getNontransactionalRead() {
        return nontransactionalRead;
    }

    /**
     * Sets the number of seconds to wait for a query statement
     * to execute in the datastore associated with this  Transaction instance
     * @param timeout          new timout value in seconds; zero means unlimited
     */
    public void setQueryTimeout(int timeout) {
        queryTimeout = timeout;
    }

    /**
     * Gets the number of seconds to wait for a query statement
     * to execute in the datastore associated with this  Transaction instance
     * @return      timout value in seconds; zero means unlimited
     */
    public int getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * Sets the number of seconds to wait for an update statement
     * to execute in the datastore associated with this  Transaction instance
     * @param timeout          new timout value in seconds; zero means unlimited
     */
    public void setUpdateTimeout(int timeout) {
        updateTimeout = timeout;
    }

    /**
     * Gets the number of seconds to wait for an update statement
     * to execute in the datastore associated with this  Transaction instance
     * @return      timout value in seconds; zero means unlimited
     */
    public int getUpdateTimeout() {
        return updateTimeout;
    }

    public void setOptimistic(boolean flag) {
        //
        // First do a quick check to make sure the transaction is active.
        // This allows us to throw an exception immediately.
        //
        if (!isTerminated()) {
            throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                  "transaction.transactionimpl.setoptimistic.notallowed")); // NOI18N
        }

        //
        // Now, get an exclusive lock in order to actually modify the optimistic flag.
        //
        persistenceManager.acquireExclusiveLock();

        try {
            if (isTerminated()) {
                this.optimistic = flag;

                // Adjust depending flags
                if (flag) {
                    nontransactionalRead = flag;
                    persistenceManager.notifyNontransactionalRead(flag);
                }
            } else {
                throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                      "transaction.transactionimpl.setoptimistic.notallowed")); // NOI18N
            }

            // Notify PM about Tx type change
            persistenceManager.notifyOptimistic(flag);
        } finally {
            persistenceManager.releaseExclusiveLock();
        }
    }

    public boolean getOptimistic() {
        return optimistic;
    }

    public void setSynchronization(Synchronization sync) {
        if (this.tracing)
        this.traceCall("setSynchronization"); // NOI18N

        persistenceManager.acquireExclusiveLock();

        try {
            synchronization = sync;

            if (this.tracing) {
                this.traceCallInfo("setSynchronization", // NOI18N
                    TRACE_SYNCHRONIZATIONS, null);
            }

        } finally {
            persistenceManager.releaseExclusiveLock();
        }
    }

    public Synchronization getSynchronization() {
        persistenceManager.acquireShareLock();

        try {
            return synchronization;
        } finally {
            persistenceManager.releaseShareLock();
        }
    }

    public int getTransactionType() {
        return txType;
    }

    /** Verify that username and password are equal to ones stored before
     *
     * @param username as String
     * @param password as String
     * @return true if they are equal
     */
    public boolean verify(String username, String password)
    {
        if ((this.username != null && !this.username.equals(username)) ||
            (this.username == null && username != null) ||
            (this.password != null && !this.password.equals(password)) ||
            (this.password  == null && password != null)) {
            return false;
        }
        return true;
    }

    //
    // ----- Methods from javax.transaction.Transaction interface ------
    //

    /**
     * Begin a transaction.
     */
    public void begin() {

        persistenceManager.acquireExclusiveLock();

        try {

            // Check and set status...
            beginInternal();

            // BMT with JDO Transaction
            if (EJBHelper.isManaged()) {
                txType = BMT_JDO;
                try {
                    TransactionManager tm = EJBHelper.getLocalTransactionManager();

                    tm.begin();
                    jta = tm.getTransaction();
                    EJBHelper.registerSynchronization(jta, this);
                    pmFactory.registerPersistenceManager(persistenceManager, jta);

                } catch (JDOException e) {
                    throw e;     // re-throw it.

                } catch (Exception e) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(
                        messages, "transaction.transactionimpl.begin.failedlocaltx"), e); // NOI18N
                }
            } else {
                txType = NON_MGD;
            }
        } finally {
            persistenceManager.releaseExclusiveLock();
        }
    }

    /**
     * Status change and validation
     */
    private void beginInternal() {
        this.setTrace();

        if (this.tracing)
            this.traceCall("begin");  // NOI18N

        // RESOLVE: need to reset to NO_TX
        if (this.isActive()) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.begin.notnew",  // NOI18N
                this.statusString(this.status)));

        }
        this.setStatus(Status.STATUS_ACTIVE);
        this.threads = 1;
    }

    /**
     * Begin a transaction in managed environment
     */
    public void begin(javax.transaction.Transaction t) {

        persistenceManager.acquireExclusiveLock();

        try {
            beginInternal();
            try {
                jta = t;
                EJBHelper.registerSynchronization(jta, this);
            } catch (Exception e) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(
                    messages, "transaction.transactionimpl.begin.registersynchfailed"), e); // NOI18N
            }

            txType = CMT;
        } finally {
            persistenceManager.releaseExclusiveLock();
        }
    }

    /**
     * Commit the transaction represented by this Transaction object
     *
     */
    public void commit() {

        persistenceManager.acquireExclusiveLock();

        try {
            if (txType == CMT || txType == BMT_UT) {
                // Error - should not be called
                throw new JDOUserException(I18NHelper.getMessage(messages,
                     "transaction.transactionimpl.mgd", "commit")); //NOI18N
            } else if (txType == BMT_JDO) {
                // Send request to the container:
                try {
                    EJBHelper.getLocalTransactionManager().commit();
                    return;
                } catch (Exception e) {
                    throw new JDOException("", e); // NOI18N
                }
            }

            this.setTrace();

            if (this.tracing)
                this.traceCall("commit"); // NOI18N

            this.commitBefore();
            this.commitPrepare();
            this.commitComplete();
            this.notifyAfterCompletion();
        } finally {
            persistenceManager.releaseExclusiveLock();
        }
    }

    /**
     * Called in the managed environment only for transaction completion
     */
    public void beforeCompletion() {

        if (txType == NON_MGD) {
            // Error - should not be called
            throw new JDOUserException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.nonmgd", "beforeCompletion")); //NOI18N
        }

        Object o = null;

        try {
            o = EJBHelper.preInvoke(new Object[] {this, persistenceManager, jta});
            this.commitBefore(); // do actual beforeComplition
            this.commitPrepare(); // check internal status
            this.commitComplete();
        } finally {
            this.closeConnection();
            EJBHelper.postInvoke(o);
        }
    }

    /**
     * Called in the managed environment only for transaction completion
     */
    public void afterCompletion(int st) {

        if (txType == NON_MGD) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                        "transaction.transactionimpl.nonmgd", "afterCompletion")); //NOI18N
        }
        st = EJBHelper.translateStatus(st); // translate Status

        if (this.tracing) {
            this.traceCallInfo("afterCompletion", TRACE_SYNCHRONIZATIONS, //NOI18N
            this.statusString(st));
        }

        if (st == Status.STATUS_ROLLEDBACK) {
            this.setStatus(Status.STATUS_ROLLING_BACK);
            this.internalRollback();
        }

        if (st != this.status) {
            if (synchronization != null) {
                // Allow user to do any cleanup.
                try {
                    synchronization.afterCompletion(st);
                } catch (Exception ex) {
                    logger.log(Logger.WARNING, I18NHelper.getMessage(
                           messages,
                           "transaction.transactionimpl.syncmanager.aftercompletion", // NOI18N
                           ex.getMessage()));
                }
            }

            // Force to close the persistence manager. 
            persistenceManager.forceClose();

            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.commitprepare.wrongstatus", // NOI18N
                "afterCompletion", this.statusString(this.status),  // NOI18N
                this.statusString(st)));
        }

        this.notifyAfterCompletion();
    }

    /**
     * Lower-level before-commit method - phase 0.
     *
     * This is called before commit processing actually begins.
     * State transition:
     *        STATUS_ACTIVE        starting state
     *        startedCommit        set to avoid concurrent commits
     *        beforeCompletion()    called while still active
     *        STATUS_PREPARING    no longer active, about to "really" commit
     *
     * The startedCommit status is an "internal state" which is still active
     * but prevents concurrent commits and some other operations.
     *
     * For exceptions see commit() method.
     */
    private void commitBefore() {
        boolean        rollbackOnly = false; //marked for rollback
        boolean        notified = false;

        if (this.tracing)
            this.traceCall("commitBefore"); // NOI18N
        //
        // Validate transaction state before we commit
        //

        if ((this.status == Status.STATUS_ROLLING_BACK)
            ||    (this.status == Status.STATUS_ROLLEDBACK)) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.rolledback", // NOI18N
                "commit", // NOI18N
                this.statusString(this.status)));
        }

        if (this.status == Status.STATUS_MARKED_ROLLBACK) {
            rollbackOnly = true;
        } else if (this.status != Status.STATUS_ACTIVE) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.commit_rollback.notactive", // NOI18N
                "commit", // NOI18N
                this.statusString(this.status)));
        }

        if (this.startedCommit) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.commitbefore.incommit", // NOI18N
                "commit")); // NOI18N
        }
        this.startedCommit = true;

        //
        // User notifications done outside of lock - check for concurrent
        // rollback or setRollbackOnly during notification.
        //
        if (!rollbackOnly) {
            this.notifyBeforeCompletion();
            notified = true;

            if (this.status == Status.STATUS_ACTIVE) {        // All ok
                this.setStatus(Status.STATUS_PREPARING);
            } else if (this.status == Status.STATUS_MARKED_ROLLBACK) {
                rollbackOnly = true;
            } else {    // Must have been concurrently rolled back
                throw new JDOUserException(I18NHelper.getMessage(messages,
                    "transaction.transactionimpl.commitbefore.rolledback")); // NOI18N
            }
        }
        if (rollbackOnly && txType == NON_MGD) {
            this.rollback();

            throw new JDOUserException(I18NHelper.getMessage(messages,
                notified ?
                   "transaction.transactionimpl.commitbefore.rollbackonly_insync" : // NOI18N
                   "transaction.transactionimpl.commitbefore.rollbackonly")); // NOI18N
        }
    }

    /**
     * Lower-level prepare-commit method - phase 1.
     *
     * This is called once we've started "real commit" processing.
     * State transition:
     *        STATUS_PREPARING    starting state
     *        prepareResources()        if not one-phase
     *        STATUS_PREPARED
     *
     * For exceptions see commit() method.
     */
    private void commitPrepare() {
        if (this.tracing)
            this.traceCall("commitPrepare"); // NOI18N
        //
        // Once we've reached the Status.STATUS_PREPARING state we do not need
        // to check for concurrent state changes.  All user-level methods
        // (rollback, setRollbackOnly, register, enlist, etc) are no longer
        // allowed.
        //

        //
        // Validate initial state
        //
        if (this.status != Status.STATUS_PREPARING) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
               "transaction.transactionimpl.commitprepare.wrongstatus", // NOI18N
                "commitPrepare",  // NOI18N
                "STATUS_PREPARING", // NOI18N
                this.statusString(this.status)));
        }

        //
        // If there is at most one resource then we can do this in 1-phase.
        //
        if (this.resources.size() <= 1)
            this.onePhase = true;

        /*
        //
        // Prepare resources if not one-phase
        //
        if (!this.onePhase) {
            int    error = this.prepareResources();
            if (error != XAResource.XA_OK) {
                this.forceRollback();

                throw (RollbackException)ErrorManager.createFormatAdd(
                        RollbackException.class,
                        ErrorManager.USER,
                        TransactionMsgCat.SH_ERR_TX_TR_CMT_RB_IN_PREP_XA,
                        this.XAErrorString(error));
            }
        }
        */

        this.setStatus(Status.STATUS_PREPARED);
    }

    /**
     * Lower-level complete-commit method - phase 2.
     *
     * State transition:
     *        STATUS_PREPARED        starting state
     *        STATUS_COMMITTING    starting to do final phase
     *        commitResources()        maybe one-phase
     *        STATUS_COMMITTED
     *        afterCompletion()    no longer active
     *
     * For exceptions see commit() method.
     */
    private void commitComplete() {
        if (this.tracing)
            this.traceCallInfo("commitComplete", TRACE_ONE_PHASE, null); // NOI18N

        //
        // Validate initial state
        //
        if (this.status == Status.STATUS_ROLLING_BACK) {
            this.setStatus(Status.STATUS_ROLLING_BACK); //st);
            internalRollback();
        } else if (this.status == Status.STATUS_PREPARED) {
            this.setStatus(Status.STATUS_COMMITTING);
            internalCommit();
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.commitprepare.wrongstatus", // NOI18N
                "commitComplete",  // NOI18N
                "STATUS_PREPARED", // NOI18N
                this.statusString(this.status)));
        }
    }

    /**
     * Internal method to process commit operation
     */
    private void internalCommit() {
        /*
         * CODE FOR XAResource support
        //
        // Commit resources.  If onePhase then we can expect a rollback
        // indication so be prepared to deal with that.
        //
        int    error = this.commitResources();
        if (error != XAResource.XA_OK) {
            this.forceRollback();

            throw (RollbackException)ErrorManager.createFormatAdd(
                    RollbackException.class,
                    ErrorManager.USER,
                    TransactionMsgCat.SH_ERR_TX_TR_CMT_RB_IN_CMT_COMP_XA,
                    this.XAErrorString(error));
        }
         * END CODE FOR XAResource support
         */

        if (txType == NON_MGD) {
            int     error = this.commitConnection();
            if (error != INTERNAL_OK) {
                this.forceRollback();
                throw new JDOUserException(I18NHelper.getMessage(messages,
                    "transaction.transactionimpl.commitcomplete.error", // NOI18N
                    "Connection Error")); // NOI18N
            }
            this.closeConnection();
        }
        this.setStatus(Status.STATUS_COMMITTED);
    }

    /**
     * Rollback the transaction represented by this transaction object.
     *
     */
    public void rollback() {

        persistenceManager.acquireExclusiveLock();

        try {
            if (txType == CMT || txType == BMT_UT) {
                // Error - should not be called
                throw new JDOUserException(I18NHelper.getMessage(messages,
                     "transaction.transactionimpl.mgd", "rollback")); //NOI18N
            }

            this.setTrace();

            if (this.tracing)
                this.traceCall("rollback"); // NOI18N

            if ((this.status != Status.STATUS_ACTIVE)
                &&    (this.status != Status.STATUS_MARKED_ROLLBACK)) {
                //
                // Once commit processing has started (PREPARING, COMMITTING
                // or COMITTED) the only way to rollback a transaction is
                // via the registered resource interface throwing an
                // XAException, which will use a lower-level rollback.
                //

                throw new JDOUserException(I18NHelper.getMessage(messages,
                    "transaction.transactionimpl.commit_rollback.notactive", // NOI18N
                    "rollback", // NOI18N
                    this.statusString(this.status)));
            }

            this.setStatus(Status.STATUS_ROLLING_BACK);

            this.internalRollback();
            this.closeConnection();

            if (txType == BMT_JDO) {
                // Send request to the container:
                try {
                    EJBHelper.getLocalTransactionManager().rollback();
                } catch (Exception e) {
                    throw new JDOException("", e); // NOI18N
                }
            } else { //NON_MGD
                //This has effect of rolling back changes also
                //which would not happen in case of BMT_JDO
                //Is this the desired behavior ?
                //TransactionImpl.notifyAfterCompletion()
                //PersistenceManagerImp.afterCompletion()
                //SQLStateManager.rollback()
                this.notifyAfterCompletion();
            }
        } finally {
            persistenceManager.releaseExclusiveLock();
        }
    }

    /**
     * Lower-level rollback method.  This is expected to be called once
     * the caller has confirmed that itself has set the status to
     * STATUS_ROLLING_BACK.  This is to avoid concurrent rollbacks.
     *
     */
    private void internalRollback() {
        if (this.tracing)
            this.traceCall("internalRollback"); // NOI18N

        if (this.status == Status.STATUS_ROLLEDBACK) {
            return;
        }

        if (this.status != Status.STATUS_ROLLING_BACK) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                "transaction.transactionimpl.commitprepare.wrongstatus", // NOI18N
                "internalRollback",  // NOI18N
                "STATUS_ROLLING_BACK", // NOI18N
                this.statusString(this.status)));
        }

        if (txType == NON_MGD) {
            this.rollbackConnection();
        }

        this.setStatus(Status.STATUS_ROLLEDBACK);
    }

    /**
     * [package-visible]
     *
     * Force rollback.  This is called when something goes wrong during
     * a late state check (i.e. some failure occurred during the prepare
     * stage).  Unless we're not already rolling back (or rolled back) this
     * will blindly change the state of the transaction and complete the
     * latter stage of rollback.
     *
     * @return the final status of the transaction.
     *
     * See internalRollback() for exceptions
     */
    int forceRollback() {
        if (this.tracing)
            this.traceCall("forceRollback"); // NOI18N

        if ((this.status == Status.STATUS_ROLLING_BACK)        // Already
            ||    (this.status == Status.STATUS_ROLLEDBACK)        // Done
            ||    (this.status == Status.STATUS_COMMITTED)        // Too late
            ||    (this.status == Status.STATUS_NO_TRANSACTION)    // Never was
           ) {
            return this.status;
        }
        this.internalRollback();
        this.notifyAfterCompletion();

        return this.status;
    }

    /**
     * Modify the transaction object such that the only possible outcome of
     * the transaction is to roll back.
     *
     */
    public void setRollbackOnly() {
        if (this.tracing)
            this.traceCall("setRollbackOnly"); // NOI18N

        if ((this.status == Status.STATUS_ROLLING_BACK)
                ||    (this.status == Status.STATUS_ROLLEDBACK)
                ||     (this.status == Status.STATUS_MARKED_ROLLBACK)) {
            //
            // Already rolled back, rollback in progress or already marked.
            //
            return;
        }

        if (txType != NON_MGD) {
            try {
                jta.setRollbackOnly();
            } catch (Exception e) {
                throw new JDOException("", e); // NOI18N
            }
        } else {
            this.setStatus(Status.STATUS_MARKED_ROLLBACK);
        }

    }

    /**
     * Obtain the status of this transaction object.
     *
     * @return The transaction status.
     *
     */
    public int getStatus() {
        //
        // Leave this as "synchronized" as I want to test that from callbacks
        // (i.e. beforeCompletion, xaRes.prepare) the lock is released, and
        // that other threads can access this object.  See note at top of file.
        //
        synchronized (this.globalLock) {
            return this.status;
        }
    }

    //
    // ----- Methods introduced in com.forte.transaction.ForteTran ------
    //


    /**
     * Confirm that transaction is terminated.
     *
     * @return True if transaction is completed.
     */
    boolean isTerminated() {
        synchronized (this.globalLock) {
            return ((this.status == Status.STATUS_COMMITTED)
                ||    (this.status == Status.STATUS_ROLLEDBACK)
                ||    (this.status == Status.STATUS_NO_TRANSACTION));
        }
    }

    //
    // ----- Private methods ------
    //

    /**
     * Notify registered Synchronization interfaces with beforeCompletion().
     * This method is only called before a commit so we stop iterating if
     * we encounter an invalid status.  Which could have been set by a
     * rollback during (or concurrent with) the notification.
     */
    //
    // CHANGES
    //     18-jan-1999
    //        written (ncg)
    //
    private void notifyBeforeCompletion() {
        if (synchronization != null) {
            try {
                synchronization.beforeCompletion();
            } catch (Exception ex) {
                //
                // RESOLVE: ignored ?
                //
            }
        }
        persistenceManager.beforeCompletion();
    }

    /**
     * Notify registered Synchronization interfaces with afterCompletion().
     * We assume that no status changes occur while executing this method.
     */
    private void notifyAfterCompletion() {
        try {
            persistenceManager.afterCompletion(this.status);

        } finally {
            if (synchronization != null) {
                try {
                    synchronization.afterCompletion(this.status);
                } catch (Exception ex) {
                    logger.log(Logger.WARNING, I18NHelper.getMessage(
                         messages,
                         "transaction.transactionimpl.syncmanager.aftercompletion", // NOI18N
                         ex.getMessage()));
                }
            }
        }

        this.forget();
    }



    /**
     * Set status under lock (may be a nested lock which is ok)
     */
    //
    // CHANGES
    //     27-jan-1999
    //        created (ncg)
    //
    private void setStatus(int status) {
        synchronized(this.globalLock) {
            if (this.tracing) {
                Object[] items= new Object[] {Thread.currentThread(),this.toString(), 
                    this.statusString(this.status), this.statusString(status), persistenceManager};
                logger.finest("sqlstore.transactionimpl.status",items); // NOI18N
            }
            this.status = status;
            persistenceManager.notifyStatusChange(isActive());
        }
    }

    /**
     * Forget this transaction
     */
    private void forget() {
        if (this.tracing)
            this.traceCall("forget"); // NOI18N

        //
        // Do not clear:
        //
        //    .tid            -- users can still browse transaction id
        //    .status            -- users can still check status
        //    .timeout        -- users can still check timeout
        //    .onePhase         -- remember if was committed 1-phase
        //
        this.threads = 0;
        this.startedCommit = false;

        // SHOULD BE CLOSED: closeConnection();
        if (_connection != null) {
            try {
                if (!_connection.isClosed()) {
                    closeConnection();
                    throw new JDOFatalInternalException(I18NHelper.getMessage(
                        messages, 
                        "transaction.transactionimpl.forget.connectionnotclosed")); // NOI18N
                }
            } catch (Exception e) {
            }
            _connection = null;
        }
        _connectionReferenceCount = 0;

        //
        // Do we need to invoke forget on the resource?
        //
        this.resources.clear();
            if (txType != NON_MGD) {
            persistenceManager.close();
        }

        jta = null;
        txType = NON_MGD;       // Restore the flag

        this.setTrace();
    }

    //
    // ----- Debugging utilities -----
    //

    /**
     * Set the global transaction tracing.
     */
    static void setTrace() {
        TransactionImpl.tracing = logger.isLoggable(Logger.FINEST);
    }

    /**
     * Trace method call.
     */
    private void traceCall(String call) {
        Object[] items = new Object[]{Thread.currentThread(),this.toString(),call,
             this.statusString(this.status),txTypeString(), persistenceManager};
        logger.finest("sqlstore.transactionimpl.call",items); // NOI18N
    }

    /**
     * Trace method call with extra info.
     */
    //
    // CHANGES
    //     11-jan-1999
    //        created (ncg)
    //
    private void traceCallInfo(String call, int info, String s) {

        //TODO : Optimize this when converting to resource budles
        StringBuffer logMessage = new StringBuffer();
        logMessage.append("Thread.currentThread()").append("Tran[") // NOI18N
            .append(this.toString()).append("].").append(call) // NOI18N
            .append(": status = ").append(this.statusString(this.status)); // NOI18N

        if ((info & TRACE_THREADS) != 0)
            logMessage.append(", threads = " + this.threads); // NOI18N
        if ((info & TRACE_SYNCHRONIZATIONS) != 0)
            logMessage.append(", sync = " + this.synchronization); // NOI18N
        if ((info & TRACE_RESOURCES) != 0)
            logMessage.append(", resources = " + this.resources.size()); // NOI18N
        if ((info & TRACE_ONE_PHASE) != 0 && this.onePhase)
            logMessage.append(", onePhase = true"); // NOI18N
        if (s != null)
            logMessage.append(", " + s + " for " + persistenceManager); // NOI18N
        
        logger.finest("sqlstore.transactionimpl.general",logMessage.toString()); // NOI18N
    }

    /**
     * Trace method call with extra string.
     */
    private void traceCallString(String call, String info) {
      Object[] items = new Object[] {Thread.currentThread(),this.toString(),call,info,persistenceManager};
      logger.finest("sqlstore.transactionimpl.call.info",items); // NOI18N
    }

    /**
     * Translates a txType value into a string.
     *
     * @return  Printable String for a txType value
     */
    private String txTypeString() {
        switch (txType) {
            case NON_MGD:                   return "NON_MGD"; // NOI18N
            case CMT:                       return "CMT"; // NOI18N
            case BMT_UT:                    return "BMT_UT"; // NOI18N
            case BMT_JDO:                   return "BMT_JDO"; // NOI18N
            default:                        break;
        }
        return "UNKNOWN"; // NOI18N
    }

    /**
     * Translates a javax.transaction.Status value into a string.
     *
     * @param   status   Status object to translate.
     * @return  Printable String for a Status object.
     */
    public static String statusString(int status) {
        switch (status) {
            case Status.STATUS_ACTIVE:            return "STATUS_ACTIVE"; // NOI18N
            case Status.STATUS_MARKED_ROLLBACK:   return "STATUS_MARKED_ROLLBACK"; // NOI18N
            case Status.STATUS_PREPARED:          return "STATUS_PREPARED"; // NOI18N
            case Status.STATUS_COMMITTED:         return "STATUS_COMMITTED"; // NOI18N
            case Status.STATUS_ROLLEDBACK:        return "STATUS_ROLLEDBACK"; // NOI18N
            case Status.STATUS_UNKNOWN:           return "STATUS_UNKNOWN"; // NOI18N
            case Status.STATUS_NO_TRANSACTION:    return "STATUS_NO_TRANSACTION"; // NOI18N
            case Status.STATUS_PREPARING:         return "STATUS_PREPARING"; // NOI18N
            case Status.STATUS_COMMITTING:        return "STATUS_COMMITTING"; // NOI18N
            case Status.STATUS_ROLLING_BACK:      return "STATUS_ROLLING_BACK"; // NOI18N
            default:                              break;
        }
        return "STATUS_Invalid[" + status + "]"; // NOI18N
    }

    /**
     * Returns a Connection. If there is no existing one, asks
     * ConnectionFactory for a new Connection
     */
    public synchronized Connection getConnection() {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (_connection == null) {
            // find a new connection
            if (connectionFactory == null) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages, 
                    "transaction.transactionimpl.getconnection.nullcf")); // NOI18N
            }

            _connection = this.getConnectionInternal();
        }

        _connectionReferenceCount++;

        if (debug) {
            Object[] items = new Object[] {_connection, Boolean.valueOf(optimistic), 
                new Integer(_connectionReferenceCount) , persistenceManager};
            logger.finest("sqlstore.transactionimpl.getconnection",items); // NOI18N
        }

        // We cannot depend on NON_MGD flag here as this method can be called
        // outside of an active transaction.
        if (!EJBHelper.isManaged()) {
            try {
                //
                // For active pessimistic transaction or a committing transaction, we need to set
                // auto-commit feature off.
                //
                if ((!optimistic && isActive()) || startedCommit) {
                    // Set autocommit to false *only* if it's true
                    // I.e., don't set to false multiple times (Sybase
                    // throws exception in that case).
                    if (_connection.getAutoCommit()) {
                        _connection.setAutoCommit(false);
                    }
                } else {
                    _connection.setAutoCommit(true);
                }
            } catch (java.sql.SQLException e) {
                logger.log(Logger.WARNING,"sqlstore.exception.log",e);  // NOI18N
            }
        }

        return _connection;
    }

    /**
     * Replace a connection. Used in a managed environment only.
     * In a J2EE RI Connection need to be replaced at the beforeCompletion.
     */
    public void replaceConnection() {
        if (EJBHelper.isManaged()) {
            this.releaseConnection();
            this.closeConnection();
            this.getConnection();
        }
    }

    /**
     * Close a connection.
     * Connection cannot be closed if it is part of the commit/rollback
     * operation or inside a pessimistic transaction
     */
    public synchronized void releaseConnection() {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (_connectionReferenceCount > 0) {
            _connectionReferenceCount--;
        }

        if (debug) {
            Object[] items = new Object[] {Boolean.valueOf(optimistic),
                Boolean.valueOf(startedCommit), 
                new Integer(_connectionReferenceCount) , persistenceManager};
            logger.finest("sqlstore.transactionimpl.releaseconnection",items); // NOI18N
        }

        // Fix for bug 4479807: Do not keep connection in the managed environment.
        if ( (!EJBHelper.isManaged() && optimistic == false) || startedCommit ) {
            // keep Connection. Do not close.
            return;
        }

        if (_connectionReferenceCount == 0) {
            //
            // For optimistic transaction, we only release the connection when
            // no one is using it.
            //
            closeConnection();
        }
    }

    private Connection getConnectionInternal() {
        if (isDataSource) {
            try {
                if (EJBHelper.isManaged()) {
                    // Delegate to the EJBHelper for details.
                    if (isActive()) {
                        return EJBHelper.getConnection(connectionFactory,
                            username, password);
                    } else {
                        return EJBHelper.getNonTransactionalConnection(
                            connectionFactory, username, password);
                    }
                } else if (username != null) {
                    return ((DataSource)connectionFactory).getConnection(
                        username, password);
                } else {
                     return ((DataSource)connectionFactory).getConnection();
                }

            } catch (java.sql.SQLException e) {
                String sqlState = e.getSQLState();
                int  errorCode = e.getErrorCode();

                if (sqlState == null) {
                    throw new JDODataStoreException(
                        I18NHelper.getMessage(messages,
                            "connectionefactoryimpl.sqlexception", // NOI18N
                            "null", "" + errorCode), e); // NOI18N
                } else {
                    throw new JDODataStoreException(
                        I18NHelper.getMessage(messages,
                            "connectionefactoryimpl.sqlexception", // NOI18N
                            sqlState, "" + errorCode), e); // NOI18N
                }
            }
        } else {
            return ((ConnectionFactory)connectionFactory).getConnection();
        }
    }

    /**
     * Always Close a connection
     */
    private void closeConnection() {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            Object[] items = new Object[] {_connection , persistenceManager};
            logger.finest("sqlstore.transactionimpl.closeconnection",items); // NOI18N
        }
        try {
            if (_connection != null) {
                _connection.close();
            }
        } catch (Exception e) {
            // Recover?
        }
        _connection = null;
    }

    /**
     * replaces rollbackResources() in ForteTran
     */
    private void rollbackConnection() {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (debug) {
            Object[] items = new Object[] {_connection , persistenceManager};
            logger.finest("sqlstore.transactionimpl.rollbackconnection",items); // NOI18N
        }
        if (_connection != null) {
            try {
                if (isDataSource)
                    _connection.rollback();
                else
                    ((ConnectionImpl)_connection).internalRollback();
            } catch (Exception e) {
                //Recover?
            }
        }
    }
    private int INTERNAL_ERROR = 1;
    private int INTERNAL_OK = 0;

    /**
     * replaces commitResources() in ForteTran
     */
    private int commitConnection() {
        if (_connection != null) {
            try {
                if (isDataSource)
                    _connection.commit();
                else
                    ((ConnectionImpl)_connection).internalCommit();
            } catch (Exception e) {
                return INTERNAL_ERROR;
            }
        }
        return INTERNAL_OK;
    }


    /**
     * Returns a string representation of this transaction object.
     *
     * @return  String describing contents of this Transaction object.
     */
    public String toString() {
        int        i;
        Object    o;

        String    s = "  Transaction: \n   status        = " + this.statusString(this.status)+ "\n" // NOI18N
                  +    "   Transaction Object       = Transaction@" + this.hashCode() + "\n" // NOI18N
                  +    "   threads       = " + this.threads + "\n"; // NOI18N

        if (this.timeout != 0)
            s = s + "   timeout       = " + this.timeout + "\n"; // NOI18N
        if (this.startedCommit)
            s = s +    "   startedCommit = true\n"; // NOI18N
        if (this.onePhase)
            s = s +    "   onePhase      = true\n"; // NOI18N

        if (synchronization != null) {
            s = s + "sync:     " + synchronization + "\n"; // NOI18N
        }
        if (!this.resources.isEmpty()) {
            s = s +    "   # resources   = " + this.resources.size() + "\n"; // NOI18N
            /*
            for (i = 0; i < this.resources.size(); i++) {
                XAResource        res = (XAResource)this.resources.get(i);
                //
                // Make be null if vote readonly during commit processing
                // Bug 48325: avoid recursive toString() calls
                //
                if (res != null) {
                    s = s + "    [" + i + "] " + // NOI18N
                        res.getClass().getName() + "\n"; // NOI18N
                }
            }
            */
        }
        return s;

    }
}
