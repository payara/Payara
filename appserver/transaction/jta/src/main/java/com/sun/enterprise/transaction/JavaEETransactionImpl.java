/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.transaction;

import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.api.SimpleResource;
import com.sun.enterprise.transaction.spi.TransactionInternal;
import com.sun.enterprise.transaction.spi.TransactionalResource;
import com.sun.enterprise.util.Utility;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * This class implements the JTA Transaction API for the J2EE RI.
 * It is a wrapper over the JTS Transaction object that provides optimized local
 * transaction support when a transaction uses zero/one non-XA resource,
 * and delegates to JTS otherwise.
 * This object can be in two states: local tx (jtsTx==null) or global (JTS) tx.
 * If jtsTx!=null, all calls are delegated to jtsTx.
 *

 * Time out capability is added to the local transactions. This class extends the TimerTask.
 * When the transaction needs to be timedout, this schedules with the timer. At the commit
 * and rollback time, task will be cancelled.  If the transaction is timedout, run() method
 * will be called and transaction will be marked for rollback.
 */
public final class JavaEETransactionImpl implements Runnable, JavaEETransaction {

    private static final Logger _logger = LogDomains.getLogger(JavaEETransactionImpl.class, LogDomains.JTA_LOGGER);

    // Sting Manager for Localization
    private static final StringManager sm = StringManager.getManager(JavaEETransactionImpl.class);

    // Local Tx ids are just numbers: they dont need to be unique across
    // processes or across multiple activations of this server process.
    private static final AtomicLong TX_ID_COUNTER = new AtomicLong(1L);

    // Fall back to the old (wrong) behavior for the case when setRollbackOnly
    // was called before XA transaction started
    private static final boolean DISABLE_STATUS_CHECK_ON_SWITCH_TO_XA = //
        Boolean.getBoolean("com.sun.jts.disable_status_check_on_switch_to_xa");

    private final JavaEETransactionManager javaEETM;
    private final long txId;
    private final JavaEEXid xid;
    private TransactionInternal jtsTx;
    private TransactionalResource nonXAResource;
    private TransactionalResource laoResource;
    private int localTxStatus;
    private final Vector syncs = new Vector();
    private final Vector interposedSyncs = new Vector();
    private boolean commitStarted = false;
    // START 4662745
    private final long startTime;
    // END 4662745

    // START: local transaction timeout
    private boolean timedOut = false;
    private boolean isTimerTask = false;
    private int timeout = 0;
    // END: local transaction timeout
    private boolean imported = false;

    private final Map resourceTable;
    private Map<Object, Object> userResourceMap;

    //This cache contains the EntityContexts in this Tx
    private Object activeTxCache;

    // SimpleResource mapping for EMs with TX persistent context type
    private Map<EntityManagerFactory, SimpleResource> txEntityManagerMap;

    // SimpleResource mapping for EMs with EXTENDED persistence context type
    private Map<EntityManagerFactory, SimpleResource> extendedEntityManagerMap;
    private String componentName = null;
    private List<String> resourceNames = null;

    // tx-specific ejb container info associated with this tx
    private Object containerData = null;

    private ScheduledFuture<?> scheduledTimeoutFuture;

    JavaEETransactionImpl(JavaEETransactionManager javaEETM) {
        this.javaEETM = javaEETM;
        this.txId = getNewTxId();
        this.xid = new JavaEEXid(txId);
        this.resourceTable = new HashMap<>();
        localTxStatus = Status.STATUS_ACTIVE;
        startTime=System.currentTimeMillis();
        if (_logger != null && _logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "--Created new JavaEETransactionImpl, txId = " + txId);
        }
    }

    // START: local transaction timeout
    JavaEETransactionImpl(int timeout, JavaEETransactionManager javaEETM) {
        this(javaEETM);
        isTimerTask = true;
        this.timeout = timeout;
    }
    // END: local transaction timeout

    JavaEETransactionImpl(TransactionInternal jtsTx, JavaEETransactionManager javaEETM) {
        this(javaEETM);
        this.jtsTx = jtsTx;
        imported = true;
    }

    void setScheduledTimeoutFuture(ScheduledFuture<?> scheduledTimeoutFuture) {
        this.scheduledTimeoutFuture = scheduledTimeoutFuture;
    }

    // START: local transaction timeout
    // TimerTask run() method implementation
    @Override
    public void run() {
        timedOut = true;
        final long time = System.currentTimeMillis() - startTime;
        _logger.warning(() -> String.format("Transaction with id=%s timed out after %s ms.", txId, time));
        try {
            setRollbackOnly();
        } catch (Exception e) {
            _logger.log(Level.WARNING, "enterprise_distributedtx.some_excep", e);
        }
    }

    @Override
    public Object getContainerData() {
        return containerData;
    }

    @Override
    public void setContainerData(Object data) {
        containerData = data;
    }

    boolean isAssociatedTimeout() {
        return isTimerTask;
    }

    // Cancels the timertask and returns the timeout
    public int cancelTimerTask() {
        cancel();
        return timeout;
    }

    private void cancel() {
        if (scheduledTimeoutFuture != null) {
            if(!scheduledTimeoutFuture.isCancelled() && !scheduledTimeoutFuture.isDone()) {
                scheduledTimeoutFuture.cancel(false);
            }
            scheduledTimeoutFuture = null;
        }
    }

    @Override
    public boolean isTimedOut() {
        return timedOut;
    }
    // END: local transaction timeout

    private static long getNewTxId() {
        return TX_ID_COUNTER.getAndIncrement();
    }

    @Override
    public boolean equals(Object other) {
        if ( other == this ) {
            return true;
        }
        if ( other instanceof JavaEETransactionImpl ) {
            JavaEETransactionImpl othertx = (JavaEETransactionImpl)other;
            return ( txId == othertx.txId );
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int)txId;
    }


    Xid getLocalXid() {
        return xid;
    }

    @Override
    public TransactionalResource getNonXAResource() {
        return nonXAResource;
    }

    void setNonXAResource(TransactionalResource h) {
        nonXAResource = h;
    }

    @Override
    public TransactionalResource getLAOResource() {
        return laoResource;
    }

    @Override
    public void setLAOResource(TransactionalResource h) {
        laoResource = h;
    }

    boolean isImportedTransaction() {
        return imported;
    }

    synchronized void putUserResource(Object key, Object value) {
        if (userResourceMap == null) {
            userResourceMap = new HashMap<>();
        }
        userResourceMap.put(key, value);
    }

    synchronized Object getUserResource(Object key) {
        if (userResourceMap == null) {
            return null;
        }
        return userResourceMap.get(key);
    }

    void registerInterposedSynchronization(Synchronization sync)
                                          throws RollbackException,
                                          SystemException {
        interposedSyncs.add(sync);
        if (jtsTx != null) {
            jtsTx.registerInterposedSynchronization(sync);
        }
    }

    void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    String getComponentName() {
        return componentName;
    }

    synchronized void addResourceName(String resourceName) {
        if (resourceNames == null) {
            resourceNames = new ArrayList<>();
        }
        if( !resourceNames.contains(resourceName) ) {
            resourceNames.add(resourceName);
        }
    }

    synchronized List<String> getResourceNames() {
        return resourceNames;
    }


    @Override
    public void addTxEntityManagerMapping(EntityManagerFactory emf,
                                          SimpleResource em) {
        getTxEntityManagerMap().put(emf, em);
    }

    @Override
    public SimpleResource getTxEntityManagerResource(EntityManagerFactory emf) {
        return getTxEntityManagerMap().get(emf);
    }

    private Map<EntityManagerFactory, SimpleResource>
        getTxEntityManagerMap() {
        if( txEntityManagerMap == null ) {
            txEntityManagerMap = new HashMap<>();
        }
        return txEntityManagerMap;
    }

    protected void onTxCompletion(boolean status) {
        if( txEntityManagerMap == null ) {
            return;
        }

        for (Map.Entry<EntityManagerFactory, SimpleResource> entry :
            getTxEntityManagerMap().entrySet()) {

            SimpleResource em = entry.getValue();
            if (em.isOpen()) {
                try {
                    em.close();
                } catch (Throwable th) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "Exception while closing em.", th);
                    }
                }
            }
        }
    }

    @Override
    public void addExtendedEntityManagerMapping(EntityManagerFactory emf,
                                                SimpleResource em) {
        getExtendedEntityManagerMap().put(emf, em);
    }

    @Override
    public void removeExtendedEntityManagerMapping(EntityManagerFactory emf) {
        getExtendedEntityManagerMap().remove(emf);
    }

    @Override
    public SimpleResource getExtendedEntityManagerResource(EntityManagerFactory emf) {
        return getExtendedEntityManagerMap().get(emf);
    }

    private Map<EntityManagerFactory, SimpleResource>
        getExtendedEntityManagerMap() {
        if( extendedEntityManagerMap == null ) {
            extendedEntityManagerMap = new HashMap<>();
        }
        return extendedEntityManagerMap;
    }

    @Override
    public boolean isLocalTx() {
        return (jtsTx==null);
    }

    void setJTSTx(TransactionInternal jtsTx) throws RollbackException, SystemException {
        // Remember the status from this transaction
        boolean marked_for_rollback = isRollbackOnly();

        this.jtsTx = jtsTx;

        if ( !commitStarted ) {
            // register syncs
            for ( int i=0; i<syncs.size(); i++ ) {
                jtsTx.registerSynchronization((Synchronization)syncs.elementAt(i));
            }

            for ( int i=0; i<interposedSyncs.size(); i++ ) {
                jtsTx.registerInterposedSynchronization(
                        (Synchronization)interposedSyncs.elementAt(i));
            }
        }

        // Now adjust the status
        if (!DISABLE_STATUS_CHECK_ON_SWITCH_TO_XA && marked_for_rollback) {
            jtsTx.setRollbackOnly();
        }
    }

    TransactionInternal getJTSTx() {
        return jtsTx;
    }


    @Override
    public void commit() throws RollbackException,
                HeuristicMixedException, HeuristicRollbackException,
                SecurityException, IllegalStateException, SystemException {

        checkTransationActive();

        // START local transaction timeout
        // If this transaction is set for timeout, cancel it as it is in the commit state
        if (isTimerTask) {
            cancelTimerTask();
        }

        // END local transaction timeout
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,
                "--In JavaEETransactionImpl.commit, jtsTx=" + jtsTx + " nonXAResource=" + nonXAResource);
        }

        commitStarted = true;
        boolean success = false;
        if ( jtsTx != null ) {
            try {
                jtsTx.commit();
                success = true;
            } catch(HeuristicMixedException e) {
                success = true;
                throw e;
            } finally {
                ((JavaEETransactionManagerSimplified) javaEETM).monitorTxCompleted(this, success);
                ((JavaEETransactionManagerSimplified) javaEETM).clearThreadTx();
                onTxCompletion(success);
                try {
                    localTxStatus = jtsTx.getStatus();
                } catch (Exception e) {
                    localTxStatus = Status.STATUS_NO_TRANSACTION;
                }
                jtsTx = null;
            }

        } else { // local tx
            Exception caughtException = null;
            try {
                if ( timedOut ) {
                    // rollback nonXA resource
                    if ( nonXAResource != null ) {
                        nonXAResource.getXAResource().rollback(xid);
                    }
                    localTxStatus = Status.STATUS_ROLLEDBACK;
                    throw new RollbackException(sm.getString("enterprise_distributedtx.rollback_timeout"));
                }

                if ( isRollbackOnly() ) {
                    // rollback nonXA resource
                    if ( nonXAResource != null ) {
                        nonXAResource.getXAResource().rollback(xid);
                    }

                    localTxStatus = Status.STATUS_ROLLEDBACK;
                    throw new RollbackException(sm.getString("enterprise_distributedtx.mark_rollback"));
                }

                // call beforeCompletion
                for ( int i=0; i<syncs.size(); i++ ) {
                    try {
                        Synchronization sync = (Synchronization)syncs.elementAt(i);
                        sync.beforeCompletion();
                    } catch ( RuntimeException ex ) {
                        _logger.log(Level.WARNING, "enterprise_distributedtx.before_completion_excep", ex);
                        setRollbackOnly();
                        caughtException = ex;
                        break;
                    } catch (Exception ex) {
                        _logger.log(Level.WARNING, "enterprise_distributedtx.before_completion_excep", ex);
                        // XXX-V2 no setRollbackOnly() ???
                    }

                }

                for ( int i=0; i<interposedSyncs.size(); i++ ) {
                    try {
                        Synchronization sync = (Synchronization)interposedSyncs.elementAt(i);
                        sync.beforeCompletion();
                    } catch ( RuntimeException ex ) {
                        _logger.log(Level.WARNING, "enterprise_distributedtx.before_completion_excep", ex);
                        setRollbackOnly();
                        caughtException = ex;
                        break;
                    } catch (Exception ex) {
                        _logger.log(Level.WARNING, "enterprise_distributedtx.before_completion_excep", ex);
                        // XXX-V2 no setRollbackOnly() ???
                    }

                }

                // check rollbackonly again, in case any of the beforeCompletion
                // calls marked it for rollback.
                if ( isRollbackOnly()) {
                    //Check if it is a Local Transaction
                    RollbackException rbe = null;
                    if(jtsTx == null) {
                        if ( nonXAResource != null ) {
                            nonXAResource.getXAResource().rollback(xid);
                        }
                        localTxStatus = Status.STATUS_ROLLEDBACK;
                        rbe = new RollbackException(sm.getString("enterprise_distributedtx.mark_rollback"));

                    // else it is a global transaction
                    } else {
                        jtsTx.rollback();
                        localTxStatus = Status.STATUS_ROLLEDBACK;
                        rbe = new RollbackException(sm.getString("enterprise_distributedtx.mark_rollback"));
                    }

                    // RollbackException doesn't have a constructor that takes a Throwable.
                    if (caughtException != null) {
                        rbe.initCause(caughtException);
                    }
                    throw rbe;
                }

                // check if there is a jtsTx active, in case any of the
                // beforeCompletions registered the first XA resource.
                if ( jtsTx != null ) {
                    jtsTx.commit();

                    // Note: JTS will not call afterCompletions in this case,
                    // because no syncs have been registered with JTS.
                    // So afterCompletions are called in finally block below.

                } else {
                    // do single-phase commit on nonXA resource
                    if ( nonXAResource != null ) {
                        nonXAResource.getXAResource().commit(xid, true);
                    }

                }
                // V2-XXX should this be STATUS_NO_TRANSACTION ?
                localTxStatus = Status.STATUS_COMMITTED;
                success = true;

            } catch ( RollbackException ex ) {
                localTxStatus = Status.STATUS_ROLLEDBACK; // V2-XXX is this correct ?
                throw ex;

            } catch ( SystemException ex ) {
                // localTxStatus = Status.STATUS_ROLLEDBACK; // V2-XXX is this correct ?
                localTxStatus = Status.STATUS_COMMITTING;
                success = true;
                throw ex;

            } catch ( Exception ex ) {
                localTxStatus = Status.STATUS_ROLLEDBACK; // V2-XXX is this correct ?
                SystemException exc = new SystemException();
                exc.initCause(ex);
                throw exc;

            } finally {
                ((JavaEETransactionManagerSimplified) javaEETM).monitorTxCompleted(this, success);
                ((JavaEETransactionManagerSimplified) javaEETM).clearThreadTx();
                for ( int i=0; i<interposedSyncs.size(); i++ ) {
                    try {
                        Synchronization sync = (Synchronization)interposedSyncs.elementAt(i);
                        sync.afterCompletion(localTxStatus);
                    } catch ( Exception ex ) {
                        _logger.log(Level.WARNING, "enterprise_distributedtx.after_completion_excep", ex);
                    }
                }

                // call afterCompletions
                for ( int i=0; i<syncs.size(); i++ ) {
                    try {
                        Synchronization sync = (Synchronization)syncs.elementAt(i);
                        sync.afterCompletion(localTxStatus);
                    } catch ( Exception ex ) {
                        _logger.log(Level.WARNING, "enterprise_distributedtx.after_completion_excep", ex);
                    }
                }

                onTxCompletion(success);
                jtsTx = null;
            }
        }
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {

        // START local transaction timeout
        // If this transaction is set for timeout, cancel it as it is in the rollback state
        if (isTimerTask) {
            cancelTimerTask();
            // END local transaction timeout
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,
                "--In JavaEETransactionImpl.rollback, jtsTx=" + jtsTx + " nonXAResource=" + nonXAResource);
        }

        if (jtsTx == null) {
            // non-xa transaction can't be in prepared state, xa code will do its check
            checkTransationActive();
        }

        try {
            if (jtsTx == null) { // rollback nonXA resource
                if (nonXAResource != null) {
                    nonXAResource.getXAResource().rollback(xid);
                }

            } else {
                jtsTx.rollback();
            }

        } catch ( SystemException | IllegalStateException ex ) {
            throw ex;
        } catch ( Exception ex ) {
            _logger.log(Level.WARNING, "enterprise_distributedtx.some_excep", ex);
        } finally {
            // V2-XXX should this be STATUS_NO_TRANSACTION ?
            localTxStatus = Status.STATUS_ROLLEDBACK;

            ((JavaEETransactionManagerSimplified) javaEETM).monitorTxCompleted(this, false);
            ((JavaEETransactionManagerSimplified) javaEETM).clearThreadTx();
            if ( jtsTx == null ) {
                for ( int i=0; i<interposedSyncs.size(); i++ ) {
                    try {
                        Synchronization sync = (Synchronization)interposedSyncs.elementAt(i);
                        sync.afterCompletion(Status.STATUS_ROLLEDBACK);
                    } catch ( Exception ex ) {
                        _logger.log(Level.WARNING, "enterprise_distributedtx.after_completion_excep", ex);
                    }
                }

                // call afterCompletions
                for ( int i=0; i<syncs.size(); i++ ) {
                    try {
                        Synchronization sync = (Synchronization)syncs.elementAt(i);
                        sync.afterCompletion(Status.STATUS_ROLLEDBACK);
                    } catch ( Exception ex ) {
                        _logger.log(Level.WARNING, "enterprise_distributedtx.after_completion_excep", ex);
                    }

                }

            }
            onTxCompletion(false);
            jtsTx = null;
        }
    }

    @Override
    public boolean delistResource(XAResource xaRes, int flag)
            throws IllegalStateException, SystemException {
        // START OF IASRI 4660742
        if (_logger.isLoggable(Level.FINE)) {
              _logger.log(Level.FINE,"--In JavaEETransactionImpl.delistResource: "
                          + xaRes + " from " + this);
        }
        // END OF IASRI 4660742

        checkTransationActive();
        if (jtsTx == null) {
            throw new IllegalStateException(sm.getString("enterprise_distributedtx.deleteresource_for_localtx"));
        }
        return jtsTx.delistResource(xaRes, flag);
    }

    @Override
    public boolean enlistResource(XAResource xaRes)
            throws RollbackException, IllegalStateException,
            SystemException {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"--In JavaEETransactionImpl.enlistResource, jtsTx="
                    +jtsTx+" nonXAResource="+nonXAResource);
        }

        checkTransationActive();
        if (jtsTx != null) {
            return jtsTx.enlistResource(xaRes);
        } else if (nonXAResource != null) {
            throw new IllegalStateException(sm.getString("enterprise_distributedtx.already_has_nonxa"));
        } else { // Start a new JTS tx
            ((JavaEETransactionManagerSimplified) javaEETM).startJTSTx(this);
            return jtsTx.enlistResource(xaRes);
        }
        // IASRI END 4723068
    }

    @Override
    public int getStatus() throws SystemException {
        if (jtsTx == null) {
            return localTxStatus;
        }
        return jtsTx.getStatus();
    }

    @Override
    public void registerSynchronization(Synchronization sync)
                throws RollbackException, IllegalStateException,
                SystemException {
        // START OF IASRI 4660742
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"--In JavaEETransactionImpl.registerSynchronization, jtsTx="
                    +jtsTx+" nonXAResource="+nonXAResource);
        }
        // END OF IASRI 4660742

        checkTransationActive();
        if (jtsTx == null) {
            syncs.add(sync);
        } else {
            jtsTx.registerSynchronization(sync);
        }
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        checkTransationActive();
        if (jtsTx == null) {
            localTxStatus = Status.STATUS_MARKED_ROLLBACK;
        } else {
            jtsTx.setRollbackOnly();
        }
    }

    private boolean isRollbackOnly() throws IllegalStateException,
            SystemException {
        final int status;
        if (jtsTx == null) {
            status = localTxStatus;
        } else {
            status = jtsTx.getStatus();
        }
        return status == Status.STATUS_MARKED_ROLLBACK;
    }

    private void checkTransationActive() throws SystemException {
        int status = getStatus();
        if (status != Status.STATUS_MARKED_ROLLBACK &&
            status != Status.STATUS_ACTIVE) {
            throw new IllegalStateException(sm.getString(
                "enterprise_distributedtx.transaction_notactive"));
        }
    }

    @Override
    public String toString() {
        return "JavaEETransactionImpl: txId="+txId+" nonXAResource="+nonXAResource
                +" jtsTx="+jtsTx+" localTxStatus="+localTxStatus
                +" syncs="+syncs;
    }

    // START IASRI 4662745
    /*
     * This method is used for the Admin Framework displaying
     * of Transactions Ids
     */
    public String getTransactionId(){
        return xid.toString();
    }

    /*
     * This method returns the time this transaction was started
     */
    public long getStartTime(){
        return startTime;
    }
    // END IASRI 4662745

    @Override
    public void setResources(Set resources, Object poolInfo) {
        resourceTable.put(poolInfo, resources);
    }

    @Override
    public Set getResources(Object poolInfo) {
        return (Set) resourceTable.get(poolInfo);
    }

    /**
     * Return all pools registered in the resourceTable. This
     * will cut down the scope of pools on which transactionComplted
     * is called by the PoolManagerImpl. This method will return
     * only those pools that have ever participated in a tx
     */
    @Override
    public Set getAllParticipatingPools() {
        return resourceTable.keySet();
    }

    // Assume that there is only one instance of this class per local tx.
    private static class JavaEEXid implements javax.transaction.xa.Xid {
        private static final int formatId = 987654321;

        private static final byte[] bqual = new byte[]{0};

        private final byte[] gtrId;

        // START IASRI 4662745
        private String stringForm=null;
        // END IASRI 4662745

        JavaEEXid(long txId) {
            gtrId = new byte[8];
            Utility.longToBytes(txId, gtrId, 0);
        }

        @Override
        public int getFormatId() {
            return formatId;
        }

        @Override
        public byte[] getGlobalTransactionId() {
            return gtrId;
        }

        @Override
        public byte[] getBranchQualifier() {
            return bqual; // V2-XXX check if its ok to always have same bqual
        }

        // START IASRI 4662745
        /*
         * returens the Transaction id of this transaction
         */
        @Override
        public String toString(){

            // If we have a cached copy of the string form of the global identifier, return
            // it now.
            if( stringForm != null ) {
                return stringForm;
            }

            // Otherwise format the global identifier.
            //char[] buff = new char[gtrId.length*2 + 2/*'[' and ']'*/ + 3/*bqual and ':'*/];
            char[] buff = new char[gtrId.length*2 + 3/*bqual and ':'*/];
            int pos = 0;
            //buff[pos++] = '[';

            // Convert the global transaction identifier into a string of hex digits.

            int globalLen = gtrId.length ;
            for( int i = 0; i < globalLen; i++ ) {
                int currCharHigh = (gtrId[i]&0xf0) >> 4;
                int currCharLow  = gtrId[i]&0x0f;
                buff[pos++] = (char)(currCharHigh + (currCharHigh > 9 ? 'A'-10 : '0'));
                buff[pos++] = (char)(currCharLow  + (currCharLow  > 9 ? 'A'-10 : '0'));
            }

            //buff[pos++] = ':';
            buff[pos++] = '_';
            int currCharHigh = (0&0xf0) >> 4;
            int currCharLow  = 0&0x0f;
            buff[pos++] = (char)(currCharHigh + (currCharHigh > 9 ? 'A'-10 : '0'));
            buff[pos++] = (char)(currCharLow  + (currCharLow  > 9 ? 'A'-10 : '0'));
            //buff[pos] = ']';

            // Cache the string form of the global identifier.
            stringForm = new String(buff);

            return stringForm;
        }
        // END IASRI 4662745
    }

    public void setActiveTxCache(Object cache) {
        this.activeTxCache = cache;
    }

    public Object getActiveTxCache() {
        return this.activeTxCache;
    }

    /**
     * Return duration in seconds before transaction would timeout.
     *
     * Returns zero if this transaction has no timeout set.
     * Returns negative value if already timed out.
     */
    public int getRemainingTimeout() {
        if (timeout == 0) {
            return timeout;
        } else if (timedOut) {
            return -1;
        } else {
            // compute how much time left before transaction times out
            return timeout - (int)((System.currentTimeMillis() - startTime) / 1000L);
        }
    }

}
