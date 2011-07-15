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

package com.sun.enterprise.transaction.spi;

import java.util.concurrent.locks.Lock;

import javax.transaction.*;
import javax.transaction.xa.*;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkException;

import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.api.TransactionAdminBean;
import com.sun.enterprise.transaction.api.XAResourceWrapper;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface JavaEETransactionManagerDelegate {

    /**
     * Returns <code>true</code> if this implementation supports 
     * last agent optimization.
     */
    public boolean useLAO();

    /**
     * Reset LAO value.
     *
     * @param b the boolean value for the LAO flag.
     */
    public void setUseLAO(boolean b);

    /** 
     * Commit distributed transaction if there is any.
     */
    public void commitDistributedTransaction() throws RollbackException,
            HeuristicMixedException, HeuristicRollbackException, 
            SecurityException,
            IllegalStateException, SystemException;

    /** 
     * Rollback distributed transaction if there is any.
     */
    public void rollbackDistributedTransaction() throws IllegalStateException, 
            SecurityException, SystemException;

    /**
     * Get implementation specific status of the transaction associated with 
     * the current thread.
     *
     * @return the status value as an int.
     */
    public int getStatus() throws SystemException;

    /**
     * Get implementation specific transaction object that represents the transaction 
     * context of the calling thread.
     *
     * @return the transaction object.
     */
    public Transaction getTransaction() throws SystemException;

    /**
     * Get local transaction object that corresponds to this transaction instance.
     *
     * @return the transaction object.
     */
    public JavaEETransaction getJavaEETransaction(Transaction t);

    /**
     * Perform implementation specific steps to enlist a non-XA resource
     * with a distribute transaction.
     *
     * @param tran the Transaction object to be used to enlist the resource.
     * @param h the TransactionalResource object to be enlisted.
     * @return <code>true</code> if the resource was enlisted successfully.
     */
    public boolean enlistDistributedNonXAResource(
            Transaction tran, TransactionalResource h)
            throws RollbackException, IllegalStateException, SystemException;

    /**
     * Perform implementation specific steps to enlist resource as a LAO.
     *
     * @param tran the Transaction object to be used to enlist the resource.
     * @param h the TransactionalResource object to be enlisted.
     * @return <code>true</code> if the resource was enlisted successfully.
     */
    public boolean enlistLAOResource(Transaction tran, TransactionalResource h)
        throws RollbackException, IllegalStateException, SystemException;

    /**
     * Perform implementation specific steps to set setRollbackOnly status
     * for distributed transaction if there is any.
     */
    public void setRollbackOnlyDistributedTransaction() 
            throws IllegalStateException, SystemException;

    /**
     * Perform implementation specific steps to suspend a JavaEETransaction.
     *
     * @param tx the JavaEETransaction object to be suspend.
     * @return Transaction object representing the suspended transaction.
     */
    public Transaction suspend(JavaEETransaction tx) throws SystemException;

    /**
     * Perform implementation specific steps to resume a Transaction.
     *
     * @param tx the Transaction object that represents the transaction to be resumed.
     */
    public void resume(Transaction tx)
            throws InvalidTransactionException, IllegalStateException,
            SystemException;

    /**
     * Remove the Transaction object from the cache.
     *
     * @param tx the Transaction object to be removed.
     */
    public void removeTransaction(Transaction tx);

    /**
     * The delegate with the largest order will be used.
     *
     * @return the order in which this delegate should be used.
     */
    public int getOrder();

    /**
     * Set the JavaEETransactionManager reference.
     *
     * @param tm the JavaEETransactionManager object.
     */
    public void setTransactionManager(JavaEETransactionManager tm);

    /**
     * Returns <code>true</> if this delegate supports XA resources.
     */
    public boolean supportsXAResource();

    /**
     * Start new JTS transaction for the existing local transaction object.
     *
     * @param t the JavaEETransaction object.
     * @param isAssociatedTimeout <code>true</code> if transaction has a timeout
     * associated with it.
     * @return the new JTS Transaction instance.
     */
    public TransactionInternal startJTSTx(JavaEETransaction t, boolean isAssociatedTimeout)
            throws RollbackException, IllegalStateException, SystemException;

    /**
     * Recover an array of XAResource objects for a failed XA transaction.
     *
     * @param resourceList the array of XAResource objects to recover.
     * @throws UnsupportedOperationException if a delegate doesn't support
     * this functionality.
     */
    public void recover(XAResource[] resourceList);

    /**
     * Initialize recovery framework.
     *
     * Is a no-op if a delegate doesn't support this functionality.
     */
    public void initRecovery(boolean force);

    /**
     * This is used by importing transactions via the Connector contract.
     * Should not be called
     *
     * @return a <code>XATerminator</code> instance.
     * @throws UnsupportedOperationException if a delegate doesn't support
     * this functionality.
     */
    public XATerminator getXATerminator();

    /**
     * Release a transaction. This call causes the calling thread to be
     * dissociated from the specified transaction. <p>
     * This is used by importing transactions via the Connector contract.
     *
     * @param xid the Xid object representing a transaction.
     * @throws UnsupportedOperationException if a delegate doesn't support
     * this functionality.
     */
    public void release(Xid xid) throws WorkException;

    /**
     * Recreate a transaction based on the Xid. This call causes the calling
     * thread to be associated with the specified transaction. <p>
     * This is used by importing transactions via the Connector contract.
     *
     * @param xid the Xid object representing a transaction.
     * @param timeout the timeout for the transaction to be recreated.
     * @throws UnsupportedOperationException if a delegate doesn't support
     * this functionality.
     */
    public void recreate(Xid xid, long timeout) throws WorkException;

    /**
     * Returns an instance of an XAResourceWrapper if this delegate supports 
     * transaction recovery and there is a wrapper available for this class name.
     * Returns <code>null</code> otherwise.
     *
     *
     * @return an instance of an XAResourceWrapper or <code>null</code> if this 
     * delegate doesn't support transaction recovery or a wrapper is not available.
     */
    public XAResourceWrapper getXAResourceWrapper(String clName);

    /**
     * Handle configuration change.
     *
     * @param name the name of the configuration property.
     * @param value the ne value of the configuration.
     */
    public void handlePropertyUpdate(String name, Object value);

    /**
     * Recover the populated array of XAResource if this delegate supports
     * transaction recovery.
     *
     * @param delegated <code>true</code> if the recovery process is owned by this instance.
     * @param logPath the name of the transaction logging directory
     * @param xaresArray the array of XA Resources to be recovered.
     * @return true if the recovery has been successful.
     */
    public boolean recoverIncompleteTx(boolean delegated, String logPath, 
            XAResource[] xaresArray) throws Exception;

    /**
     * Return the delegate specific read lock that implements Lock interface.
     * 
     */
    public Lock getReadLock();

    /**
     * Return <code>true</code> if the delegate had its write lock acquired.
     */
    public boolean isWriteLocked();

    /**
     * Allows the delegate to acquire a write lock.
     */
    public void acquireWriteLock();

    /**
     * Allows the delegate to release a write lock.
     */
    public void releaseWriteLock();

    /**
     * Return the delegate specific implementation when a "null transaction context" 
     * was received from the client. See EJB2.0 spec section 19.6.2.1.
     * A null tx context has no Coordinator objref. It indicates
     * that the client had an active
     * tx but the client container did not support tx interop.
     */
    public boolean isNullTransaction();

    /**
     * Return TransactionAdminBean with delegate specific implementation details 
     * of an active Transaction.
     */
    public TransactionAdminBean getTransactionAdminBean(Transaction t) 
            throws javax.transaction.SystemException;

    /**
     * Return location of transaction logs
     *
     * @return String location of transaction logs
     */
    public String getTxLogLocation();

    /**
     * Allows an arbitrary XAResource to register for recovery
     *
     * @param xaResource XAResource to register for recovery
     */
    public void registerRecoveryResourceHandler(XAResource xaResource);
}
