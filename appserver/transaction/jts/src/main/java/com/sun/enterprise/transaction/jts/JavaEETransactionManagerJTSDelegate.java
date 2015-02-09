/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.transaction.jts;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;

import javax.transaction.*;
import javax.transaction.xa.*;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkException;

import com.sun.enterprise.transaction.config.TransactionService;
import com.sun.jts.jta.TransactionManagerImpl;
import com.sun.jts.jta.TransactionServiceProperties;
import com.sun.jts.CosTransactions.Configuration;
import com.sun.jts.CosTransactions.DefaultTransactionService;
import com.sun.jts.CosTransactions.RecoveryManager;
import com.sun.jts.CosTransactions.DelegatedRecoveryManager;
import com.sun.jts.CosTransactions.RWLock;

import com.sun.enterprise.config.serverbeans.ServerTags;

import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.api.TransactionAdminBean;
import com.sun.enterprise.transaction.api.XAResourceWrapper;
import com.sun.enterprise.transaction.spi.JavaEETransactionManagerDelegate;
import com.sun.enterprise.transaction.spi.TransactionalResource;
import com.sun.enterprise.transaction.spi.TransactionInternal;

import com.sun.enterprise.transaction.jts.recovery.OracleXAResource;
import com.sun.enterprise.transaction.jts.recovery.SybaseXAResource;
import com.sun.enterprise.transaction.jts.recovery.GMSCallBack;

import com.sun.enterprise.transaction.JavaEETransactionManagerSimplified;
import com.sun.enterprise.transaction.JavaEETransactionImpl;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.api.admin.ServerEnvironment;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

/**
 ** Implementation of JavaEETransactionManagerDelegate that supports XA
 * transactions with JTS.
 *
 * @author Marina Vatkina
 */
@Service
public class JavaEETransactionManagerJTSDelegate 
            implements JavaEETransactionManagerDelegate, PostConstruct {

    @Inject private ServiceLocator serviceLocator;

    // an implementation of the JavaEETransactionManager that calls
    // this object.
    // @Inject 
    private JavaEETransactionManager javaEETM;

    // an implementation of the JTA TransactionManager provided by JTS.
    private ThreadLocal<TransactionManager> tmLocal = new ThreadLocal();

    private Hashtable globalTransactions;
    private Hashtable<String, XAResourceWrapper> xaresourcewrappers =
            new Hashtable<String, XAResourceWrapper>();

    private Logger _logger;

    // Use JavaEETransactionManagerSimplified logger and Sting Manager for Localization
    private static StringManager sm
           = StringManager.getManager(JavaEETransactionManagerSimplified.class);

    private boolean lao = true;
    private final static ReadWriteLock lock = new ReadWriteLock();
    private static JavaEETransactionManagerJTSDelegate instance = null;
    private volatile TransactionManager transactionManagerImpl = null;
    private TransactionService txnService = null;

    public JavaEETransactionManagerJTSDelegate() {
        globalTransactions = new Hashtable();
    }

    public void postConstruct() {
        if (javaEETM != null) {
            // JavaEETransactionManager has been already initialized
            javaEETM.setDelegate(this);
        }

        _logger = LogDomains.getLogger(JavaEETransactionManagerSimplified.class, LogDomains.JTA_LOGGER);
        initTransactionProperties();

        setInstance(this);
        transactionManagerImpl = TransactionManagerImpl.getTransactionManagerImpl();
    }

    public boolean useLAO() {
         return lao;
    }

    public void setUseLAO(boolean b) {
        lao = b;
    }

    /** An XA transaction commit
     */
    public void commitDistributedTransaction() throws 
            RollbackException, HeuristicMixedException, 
            HeuristicRollbackException, SecurityException, 
            IllegalStateException, SystemException {

        if (_logger.isLoggable(Level.FINE))
                _logger.log(Level.FINE,"TM: commit");
        validateTransactionManager();
        TransactionManager tm = tmLocal.get();
        Object obj = tm.getTransaction(); // monitoring object

        JavaEETransactionManagerSimplified javaEETMS = 
                (JavaEETransactionManagerSimplified)javaEETM;
        
        boolean success = false;
        if (javaEETMS.isInvocationStackEmpty()) {
            try{
                tm.commit();
                success = true;
            }catch(HeuristicMixedException e){
                success = true;
                throw e;
            } finally {
                javaEETMS.monitorTxCompleted(obj, success);
            }
        } else {
            try {
                javaEETMS.setTransactionCompeting(true);
                tm.commit();
                success = true;
/**
            } catch (InvocationException ex) {
                assert false;
**/
            }catch(HeuristicMixedException e){
                success = true;
                throw e;
            } finally {
                javaEETMS.monitorTxCompleted(obj, success);
                javaEETMS.setTransactionCompeting(false);
            }
        }
    }

    /** An XA transaction rollback
    */
    public void rollbackDistributedTransaction() throws IllegalStateException, 
            SecurityException, SystemException {

        if (_logger.isLoggable(Level.FINE))
                _logger.log(Level.FINE,"TM: rollback");
        validateTransactionManager();

        TransactionManager tm = tmLocal.get();
        Object obj = tm.getTransaction(); // monitoring object
        
        JavaEETransactionManagerSimplified javaEETMS = 
                (JavaEETransactionManagerSimplified)javaEETM;
        
        try {
            if (javaEETMS.isInvocationStackEmpty()) {
                tm.rollback();
            } else {
                try {
                    javaEETMS.setTransactionCompeting(true);
                    tm.rollback();
/**
                } catch (InvocationException ex) {
                    assert false;
**/
                } finally {
                    javaEETMS.setTransactionCompeting(false);
                }
            }
        } finally {
            javaEETMS.monitorTxCompleted(obj, false);
        }
    }

    public int getStatus() throws SystemException {

        JavaEETransaction tx = javaEETM.getCurrentTransaction();
        int status = javax.transaction.Status.STATUS_NO_TRANSACTION;

        TransactionManager tm = tmLocal.get();
        if ( tx != null) 
            status = tx.getStatus();
        else if (tm != null) 
            status = tm.getStatus();

        if (_logger.isLoggable(Level.FINE))
            _logger.log(Level.FINE,"TM: status: " + JavaEETransactionManagerSimplified.getStatusAsString(status));

        return status;
    }

    public Transaction getTransaction() 
            throws SystemException {
        JavaEETransaction tx = javaEETM.getCurrentTransaction();
        if (_logger.isLoggable(Level.FINE))
            _logger.log(Level.FINE,"TM: getTransaction: tx=" + tx + ", tm=" + tmLocal.get());

        if ( tx != null )
            return tx;

        // Check for a JTS imported tx
        TransactionInternal jtsTx = null;
        TransactionManager tm = tmLocal.get();
        if (tm != null) {
            jtsTx = (TransactionInternal)tm.getTransaction();
        }

        if ( jtsTx == null )
            return null;
        else {
            // check if this JTS Transaction was previously active
            // in this JVM (possible for distributed loopbacks).
            tx = (JavaEETransaction)globalTransactions.get(jtsTx);
            if (_logger.isLoggable(Level.FINE))
                _logger.log(Level.FINE,"TM: getTransaction: tx=" + tx + ", jtsTx=" + jtsTx);

            if ( tx == null ) {
                tx = ((JavaEETransactionManagerSimplified)javaEETM).createImportedTransaction(jtsTx);
                globalTransactions.put(jtsTx, tx);
            }
            javaEETM.setCurrentTransaction(tx); // associate tx with thread
            return tx;
        }
    }

    public JavaEETransaction getJavaEETransaction(Transaction t) {
        if(t instanceof JavaEETransaction){
            return  (JavaEETransaction)t;
        }

        return (JavaEETransaction)globalTransactions.get(t);

    }
    public boolean enlistDistributedNonXAResource(Transaction tx, TransactionalResource h)
           throws RollbackException, IllegalStateException, SystemException {
        if(useLAO()) {
            if (((JavaEETransactionManagerSimplified)javaEETM).resourceEnlistable(h)) {
                XAResource res = h.getXAResource();
                boolean result = tx.enlistResource(res);
                if (!h.isEnlisted())
                    h.enlistedInTransaction(tx);
                    return result;
                } else {
                    return true;
            }
        } else {
            throw new IllegalStateException(
                    sm.getString("enterprise_distributedtx.nonxa_usein_jts"));
        }
    }

    public boolean enlistLAOResource(Transaction tran, TransactionalResource h)
           throws RollbackException, IllegalStateException, SystemException {

        if (tran instanceof JavaEETransaction) {
            JavaEETransaction tx = (JavaEETransaction)tran;
            ((JavaEETransactionManagerSimplified) javaEETM).startJTSTx(tx);

            //If transaction conatains a NonXA and no LAO, convert the existing
            //Non XA to LAO
            if(useLAO()) {
                if(h != null && (tx.getLAOResource() == null) ) {
                    tx.setLAOResource(h);
                    if (h.isTransactional()) {
                        XAResource res = h.getXAResource();
                        return tran.enlistResource(res);
                    }
                }
            }
            return true;
        } else {
            // Should not be called
            return false;
        }

    }

    public void setRollbackOnlyDistributedTransaction()
            throws IllegalStateException, SystemException {
        if (_logger.isLoggable(Level.FINE))
                _logger.log(Level.FINE,"TM: setRollbackOnly");

        validateTransactionManager();
        tmLocal.get().setRollbackOnly();
    }

    public Transaction suspend(JavaEETransaction tx) throws SystemException {
        if ( tx != null ) {
            if ( !tx.isLocalTx() )
                suspendXA();

            javaEETM.setCurrentTransaction(null);
            return tx;
        } else if (tmLocal.get() != null) {
            return suspendXA(); // probably a JTS imported tx
        }

        return null;
    }

    public void resume(Transaction tx)
        throws InvalidTransactionException, IllegalStateException,
        SystemException {
        if (_logger.isLoggable(Level.FINE))
            _logger.log(Level.FINE,"TM: resume");

        if (transactionManagerImpl != null) {
            setTransactionManager();
            tmLocal.get().resume(tx);
        }
    }

    public void removeTransaction(Transaction tx) {
        globalTransactions.remove(tx);
    }

    public int getOrder() {
        return 3;
    }

    public void setTransactionManager(JavaEETransactionManager tm) {
        javaEETM = tm;
        _logger = ((JavaEETransactionManagerSimplified)javaEETM).getLogger();
    }

    public TransactionInternal startJTSTx(JavaEETransaction tran, boolean isAssociatedTimeout) 
            throws RollbackException, IllegalStateException, SystemException {
        setTransactionManager();

        JavaEETransactionImpl tx = (JavaEETransactionImpl)tran;
        try {
            if (isAssociatedTimeout) {
                // calculate the timeout for the transaction, this is required as the local tx
                // is getting converted to a global transaction
                int timeout = tx.cancelTimerTask();
                int newtimeout = (int) ((System.currentTimeMillis() - tx.getStartTime()) / 1000);
                newtimeout = (timeout -   newtimeout);
                beginJTS(newtimeout);
            } else {
                beginJTS(((JavaEETransactionManagerSimplified)javaEETM).getEffectiveTimeout());
            }
        } catch ( NotSupportedException ex ) {
            throw new RuntimeException(sm.getString("enterprise_distributedtx.lazy_transaction_notstarted"),ex);
        }

        TransactionInternal jtsTx = (TransactionInternal)tmLocal.get().getTransaction();
        globalTransactions.put(jtsTx, tx);

        return jtsTx;
    }

    public void initRecovery(boolean force) {
        TransactionServiceProperties.initRecovery(force);
    }

    public void recover(XAResource[] resourceList) {
        setTransactionManager();
        TransactionManagerImpl.recover(
                Collections.enumeration(Arrays.asList(resourceList)));
    }

    public void release(Xid xid) throws WorkException {
        setTransactionManager();
        TransactionManagerImpl.release(xid);
    }

    public void recreate(Xid xid, long timeout) throws WorkException {
        setTransactionManager();
        TransactionManagerImpl.recreate(xid, timeout);
    }

    public XATerminator getXATerminator() {
        setTransactionManager();
        return TransactionManagerImpl.getXATerminator();
    }

    private Transaction suspendXA() throws SystemException {
        if (_logger.isLoggable(Level.FINE))
            _logger.log(Level.FINE,"TM: suspend");
        validateTransactionManager();
        return tmLocal.get().suspend();
    }

    private void validateTransactionManager() throws IllegalStateException {
        if (tmLocal.get() == null) {
            throw new IllegalStateException
            (sm.getString("enterprise_distributedtx.transaction_notactive"));
        }
    }

    private void setTransactionManager() {
        if (_logger.isLoggable(Level.FINE))
            _logger.log(Level.FINE,"TM: setTransactionManager: tm=" + tmLocal.get());

        if (transactionManagerImpl == null) {
           transactionManagerImpl = TransactionManagerImpl.getTransactionManagerImpl();
        }

        if (tmLocal.get() == null)
            tmLocal.set(transactionManagerImpl);
    }

    public XAResourceWrapper getXAResourceWrapper(String clName) {
        XAResourceWrapper rc = xaresourcewrappers.get(clName);

        if (rc != null)
            return rc.getInstance();

        return null;
    }

    public void handlePropertyUpdate(String name, Object value) {
        if (name.equals(ServerTags.KEYPOINT_INTERVAL)) {
            Configuration.setKeypointTrigger(Integer.parseInt((String)value,10));

        } else if (name.equals(ServerTags.RETRY_TIMEOUT_IN_SECONDS)) {
            Configuration.setCommitRetryVar((String)value);

        }
    }

    public boolean recoverIncompleteTx(boolean delegated, String logPath, 
            XAResource[] xaresArray) throws Exception {
        boolean result = false;
        if (!delegated) {
            RecoveryManager.recoverIncompleteTx(xaresArray);
            result = true;
        }
        else
            result = DelegatedRecoveryManager.delegated_recover(logPath, xaresArray);

        return result;
    }

    public void beginJTS(int timeout) throws NotSupportedException, SystemException {
        TransactionManagerImpl tm = (TransactionManagerImpl)tmLocal.get();
        tm.begin(timeout);
        ((JavaEETransactionManagerSimplified)javaEETM).monitorTxBegin(tm.getTransaction());
    }

    public boolean supportsXAResource() {
        return true;
    }

    public void initTransactionProperties() {
        if (serviceLocator != null) {
            txnService = serviceLocator.getService(TransactionService.class,
                    ServerEnvironment.DEFAULT_INSTANCE_NAME);

            if (txnService != null) {
                String value = txnService.getPropertyValue("use-last-agent-optimization");
                if (value != null && "false".equals(value)) {
                    setUseLAO(false);
                    if (_logger.isLoggable(Level.FINE))
                        _logger.log(Level.FINE,"TM: LAO is disabled");
                }
        
                value = txnService.getPropertyValue("oracle-xa-recovery-workaround");
                if (value == null || "true".equals(value)) {
                    xaresourcewrappers.put(
                        "oracle.jdbc.xa.client.OracleXADataSource",
                        new OracleXAResource());
                }
        
                if (Boolean.parseBoolean(txnService.getPropertyValue("sybase-xa-recovery-workaround"))) {
                    xaresourcewrappers.put(
                        "com.sybase.jdbc2.jdbc.SybXADataSource",
                        new SybaseXAResource());
                }
        
                if (Boolean.parseBoolean(txnService.getAutomaticRecovery())) {
                    // If recovery on server startup is set, initialize other properties as well
                    Properties props = TransactionServiceProperties.getJTSProperties(serviceLocator, false);
                    DefaultTransactionService.setServerName(props);

                    if (Boolean.parseBoolean(txnService.getPropertyValue("delegated-recovery"))) {
                        // Register GMS notification callback
                        if (_logger.isLoggable(Level.FINE))
                            _logger.log(Level.FINE,"TM: Registering for GMS notification callback");

                        int waitTime = 60;
                        value = txnService.getPropertyValue("wait-time-before-recovery-insec");
                        if (value != null) {
                            try {
                                waitTime = Integer.parseInt(value);
                            } catch(Exception e) {
                                _logger.log(Level.WARNING,"error_wait_time_before_recovery",e);
                            }
                        }
                        new GMSCallBack(waitTime, serviceLocator);
                    }
                }
            }
        }
    }

    /**
     * Return true if a "null transaction context" was received
     * from the client. See EJB2.0 spec section 19.6.2.1.
     * A null tx context has no Coordinator objref. It indicates
     * that the client had an active
     * tx but the client container did not support tx interop.
     */
    public boolean isNullTransaction() {
        try {
            return com.sun.jts.pi.InterceptorImpl.isTxCtxtNull();
        } catch ( Exception ex ) {
            // sometimes JTS throws an EmptyStackException if isTxCtxtNull
            // is called outside of any CORBA invocation.
            return false;
        }
    }

    public TransactionAdminBean getTransactionAdminBean(Transaction t) 
            throws javax.transaction.SystemException {
        TransactionAdminBean tBean = null;
        if(t instanceof com.sun.jts.jta.TransactionImpl) {
            String id = ((com.sun.jts.jta.TransactionImpl)t).getTransactionId();
            long startTime = ((com.sun.jts.jta.TransactionImpl)t).getStartTime();
            long elapsedTime = System.currentTimeMillis() - startTime;
            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());

            JavaEETransactionImpl tran = (JavaEETransactionImpl)globalTransactions.get(t);
            if(tran != null) {
                tBean = ((JavaEETransactionManagerSimplified)javaEETM).getTransactionAdminBean(tran);

                // Override with JTS values
                tBean.setIdentifier(t);
                tBean.setId(id);
                tBean.setStatus(status);
                tBean.setElapsedTime(elapsedTime);
                if (tBean.getComponentName() == null) {
                    tBean.setComponentName("unknown");
                }
            } else {
                tBean = new TransactionAdminBean(t, id, status, elapsedTime,
                                             "unknown", null);
            }
        } else {
            tBean = ((JavaEETransactionManagerSimplified)javaEETM).getTransactionAdminBean(t);
        }
        return tBean;
    }

    /** {@inheritDoc}
    */
    public String getTxLogLocation() {
        if (Configuration.getServerName() == null) {
            // If server name is null, the properties were not fully initialized
            Properties props = TransactionServiceProperties.getJTSProperties(serviceLocator, false);
            DefaultTransactionService.setServerName(props);
        }

        return Configuration.getDirectory(Configuration.LOG_DIRECTORY,
                                                 Configuration.JTS_SUBDIRECTORY,
                                                 new int[1]);
    }

    /** {@inheritDoc}
    */
    public void registerRecoveryResourceHandler(XAResource xaResource) {
            ResourceRecoveryManagerImpl.registerRecoveryResourceHandler(xaResource);
    }

    public Lock getReadLock() {
        return lock;
    }

    public void acquireWriteLock() {
        if(com.sun.jts.CosTransactions.AdminUtil.isFrozenAll()){
            //multiple freezes will hang this thread, therefore just return
            return;
        }
        com.sun.jts.CosTransactions.AdminUtil.freezeAll();

/** XXX Do we need to check twice? XXX **
        if(lock.isWriteLocked()){
            //multiple freezes will hang this thread, therefore just return
            return;
        }
** XXX Do we need to check twice? XXX **/

        lock.acquireWriteLock();
    }

    public void releaseWriteLock() {
        if(com.sun.jts.CosTransactions.AdminUtil.isFrozenAll()){
            com.sun.jts.CosTransactions.AdminUtil.unfreezeAll();
        }

/** XXX Do we need to check twice? XXX **
        if(lock.isWriteLocked()){
            lock.releaseWriteLock();
        }
** XXX Do we need to check twice? XXX **/

        lock.releaseWriteLock();
    }

    public boolean isWriteLocked() {
        return com.sun.jts.CosTransactions.AdminUtil.isFrozenAll();
    }

    public static JavaEETransactionManagerJTSDelegate getInstance() {
        return instance;
    }

    private static void setInstance(JavaEETransactionManagerJTSDelegate new_instance) {
        if (instance == null) {
            instance = new_instance;
        }
    }

    public void initXA() {
        setTransactionManager();
    }

    private static class ReadWriteLock implements Lock {
        private static final RWLock freezeLock = new RWLock();

        public void lock() { 
            freezeLock.acquireReadLock(); 
        }
        
        public void unlock() { 
            freezeLock.releaseReadLock(); 
        }

        private void acquireWriteLock() { 
            freezeLock.acquireWriteLock();
        }

        private void releaseWriteLock() { 
            freezeLock.releaseWriteLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        public  boolean tryLock() {
            throw new UnsupportedOperationException();
        }

        public boolean tryLock(long timeout, TimeUnit unit) 
                throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
