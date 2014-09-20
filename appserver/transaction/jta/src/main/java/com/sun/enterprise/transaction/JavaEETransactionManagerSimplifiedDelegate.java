/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.transaction;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.transaction.*;
import javax.transaction.xa.*;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkException;

import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.api.TransactionAdminBean;
import com.sun.enterprise.transaction.api.XAResourceWrapper;
import com.sun.enterprise.transaction.spi.JavaEETransactionManagerDelegate;
import com.sun.enterprise.transaction.spi.TransactionalResource;
import com.sun.enterprise.transaction.spi.TransactionInternal;

import com.sun.enterprise.util.i18n.StringManager;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Inject;

/**
 ** Implementation of JavaEETransactionManagerDelegate that supports only
 * local transactions with a single non-XA resource.
 *
 * @author Marina Vatkina
 */
@Service
public class JavaEETransactionManagerSimplifiedDelegate 
            implements JavaEETransactionManagerDelegate, PostConstruct {

    // @Inject 
    private JavaEETransactionManager tm;

    // Sting Manager for Localization
    private static StringManager sm
           = StringManager.getManager(JavaEETransactionManagerSimplified.class);

    private Logger _logger;

    private boolean lao = false;

    private static final ReentrantReadWriteLock.ReadLock readLock = 
            new ReentrantReadWriteLock().readLock();

    private final Semaphore writeLock = new Semaphore(1, true);

    public JavaEETransactionManagerSimplifiedDelegate() {
    }

    public void postConstruct() {
        // tm.setDelegate(this);
    }

    public boolean useLAO() {
         return lao;
    }

    public void setUseLAO(boolean b) {
        lao = b;
    }

    /** Throws an exception if called as it means that there is
     *  no active local transaction to commit.
     */
    public void commitDistributedTransaction() throws 
            RollbackException, HeuristicMixedException, 
            HeuristicRollbackException, SecurityException, 
            IllegalStateException, SystemException {

        throw new IllegalStateException(sm.getString(
                "enterprise_distributedtx.transaction_notactive"));
    } 

    /** Throws an exception if called as it means that there is
     *  no active local transaction to rollback.
     */
    public void rollbackDistributedTransaction() throws IllegalStateException, 
            SecurityException, SystemException {

        throw new IllegalStateException(sm.getString(
                "enterprise_distributedtx.transaction_notactive"));
    } 

    public int getStatus() throws SystemException {
        JavaEETransaction tx = tm.getCurrentTransaction();
        if ( tx != null && tx.isLocalTx())
            return tx.getStatus();
        else
            return javax.transaction.Status.STATUS_NO_TRANSACTION;
    }

    public Transaction getTransaction() throws SystemException {
        return  tm.getCurrentTransaction();
    }

    public JavaEETransaction getJavaEETransaction(Transaction t) {
        if(t instanceof JavaEETransaction){
            return  (JavaEETransaction)t;
        }

        throw new IllegalStateException(sm.getString("enterprise_distributedtx.nonxa_usein_jts"));
        
    }

    public boolean enlistDistributedNonXAResource(Transaction tran, TransactionalResource h)
           throws RollbackException, IllegalStateException, SystemException {
        throw new IllegalStateException(sm.getString("enterprise_distributedtx.nonxa_usein_jts"));
    }

    public boolean enlistLAOResource(Transaction tran, TransactionalResource h)
           throws RollbackException, IllegalStateException, SystemException {

        return false;
    }

    /** Throws an exception if called as it means that there is
     *  no active local transaction.
     */
    public void setRollbackOnlyDistributedTransaction()
            throws IllegalStateException, SystemException {

        throw new IllegalStateException(sm.getString(
                "enterprise_distributedtx.transaction_notactive"));
    }

    public Transaction suspend(JavaEETransaction tx) throws SystemException {
        if ( tx != null ) {
            tm.setCurrentTransaction(null);
        }

        return tx;
    }

    public void resume(Transaction tx)
        throws InvalidTransactionException, IllegalStateException,
        SystemException {
        /** XXX Throw an exception ??? The process should happen in the caller. XXX **/
    }

    public void removeTransaction(Transaction tx) {}

    public int getOrder() {
        return 1;
    }

    public void setTransactionManager(JavaEETransactionManager tm) {
        this.tm = (JavaEETransactionManagerSimplified)tm;
        _logger = ((JavaEETransactionManagerSimplified)tm).getLogger();
    }

    public TransactionInternal startJTSTx(JavaEETransaction t, boolean isAssociatedTimeout) 
            throws RollbackException, IllegalStateException, SystemException {
        throw new UnsupportedOperationException("startJTSTx");
    }

    public boolean supportsXAResource() {
        return false;
    }

    public void initRecovery(boolean force) {
        // No-op. Always called on server startup
    }

    public void recover(XAResource[] resourceList) {
        throw new UnsupportedOperationException("recover");
    }

    public XATerminator getXATerminator() {
        throw new UnsupportedOperationException("getXATerminator");
    }

    public void release(Xid xid) throws WorkException {
        throw new UnsupportedOperationException("release");
    }

    public void recreate(Xid xid, long timeout) throws WorkException {
        throw new UnsupportedOperationException("recreate");
    }

    public boolean recoverIncompleteTx(boolean delegated, String logPath, 
            XAResource[] xaresArray) throws Exception {
        throw new UnsupportedOperationException("recoverIncompleteTx");
    }


    public XAResourceWrapper getXAResourceWrapper(String clName) {
        return null;
    }

    public void handlePropertyUpdate(String name, Object value) {}

    public Lock getReadLock() {
        return readLock;
    }

    public boolean isWriteLocked() {
        return (writeLock.availablePermits() == 0);
    }

    public void acquireWriteLock() {
        try {
            writeLock.acquire();
        } catch(InterruptedException ie) {
            _logger.log(Level.FINE,"Error in acquireReadLock",ie);
        }
    }

    public void releaseWriteLock() {
        writeLock.release();
    }

    /**
     * Return false as this delegate doesn't support tx interop.
     */
    public boolean isNullTransaction() {
        return false;
    }

    public TransactionAdminBean getTransactionAdminBean(Transaction tran) 
            throws javax.transaction.SystemException {
        return ((JavaEETransactionManagerSimplified)tm).getTransactionAdminBean(tran);
    }

    /** {@inheritDoc}
    */
    public String getTxLogLocation() {
        throw new UnsupportedOperationException("getTxLogLocation");
    }

    /** {@inheritDoc}
    */
    public void registerRecoveryResourceHandler(XAResource xaResource) {
        throw new UnsupportedOperationException("registerRecoveryResourceHandler");
    }
}
