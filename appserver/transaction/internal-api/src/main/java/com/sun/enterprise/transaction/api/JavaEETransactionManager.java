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

package com.sun.enterprise.transaction.api;

import java.util.List;

import javax.transaction.*;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.rmi.RemoteException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkException;

import org.jvnet.hk2.annotations.Contract;

import com.sun.enterprise.transaction.spi.TransactionalResource;
import com.sun.enterprise.transaction.spi.JavaEETransactionManagerDelegate;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.api.invocation.ResourceHandler;

/**
 *
 * Manages transactions, acting as a gateway to the TM state machine.
 *
 * @author Tony Ng
 */
@Contract
public interface JavaEETransactionManager extends TransactionManager {

    /**
     * register a synchronization object with the transaction
     * associated with the current thread
     *
     * @param sync the synchronization object
     *
     * @exception IllegalStateException Thrown if the transaction in the
     *    target object is in prepared state or the transaction is inactive.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition
     *
     */
    public void registerSynchronization(Synchronization sync)
        throws RollbackException, IllegalStateException, SystemException;


    /**
     * Enlist the resource specified with the transaction
     *
     *
     * @param tran The transaction object
     *
     * @param h The resource handle object
     *
     * @return <i>true</i> if the resource was enlisted successfully; otherwise     *    false.
     *
     * @exception RollbackException Thrown to indicate that
     *    the transaction has been marked for rollback only.
     *
     * @exception IllegalStateException Thrown if the transaction in the
     *    target object is in prepared state or the transaction is inactive.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition
     *
     */
    public boolean enlistResource(Transaction tran,
                                  TransactionalResource h)
        throws RollbackException,
               IllegalStateException, SystemException;

    /**
     * Delist the resource specified from the transaction
     *
     * @param tran The transaction object
     *
     * @param h The resource handle object
     *
     * @param flag One of the values of TMSUCCESS, TMSUSPEND, or TMFAIL.
     *
     * @exception IllegalStateException Thrown if the transaction in the
     *    target object is inactive.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition
     *
     */
    public boolean delistResource(Transaction tran,
                                  TransactionalResource h,
                                  int flag)
        throws IllegalStateException, SystemException;

    /**
     * This is called by the Container to ask the Transaction
     * Manager to enlist all resources held by a component and
     * to associate the current Transaction with the current
     * Invocation
     * The TM finds the component through the InvocationManager
     */
    public void enlistComponentResources() throws RemoteException;

    /**
     * This is called by the Container to ask the Transaction
     * Manager to delist all resources held by a component
     *
     * The TM finds the component through the InvocationManager
     *
     * @param suspend true if the resources should be delisted
     * with TMSUSPEND flag; false otherwise
     *
     */
    public void delistComponentResources(boolean suspend)
        throws RemoteException;

    /**
     * This is called by Container to indicate that a component
     * is being destroyed. All resources registered in the context
     * should be released. The ComponentInvocation will be used for 
     * callback to calculate the resource table key.
     *
     * @param instance The component instance
     * @param inv The ComponentInvocation
     */
    public void componentDestroyed(Object instance, ComponentInvocation inv);

    /**
     * This is called by Container to indicate that a component
     * is being destroyed. All resources registered in the context
     * should be released
     *
     * @param instance The component instance
     */
    public void componentDestroyed(Object instance);

    /**
     * This is called by Container to indicate that a component
     * is being destroyed. All resources registered with this ResourceHandler
     * should be released. 
     *
     * @param rh The ResourceHandler
     */
    public void componentDestroyed(ResourceHandler rh);

    /**
     * Called by InvocationManager
     */

    public void preInvoke(ComponentInvocation prev)
	throws InvocationException;

    /**
     * Called by InvocationManager
     */

    public void postInvoke(ComponentInvocation curr, ComponentInvocation prev)
	throws InvocationException;

    public void setDefaultTransactionTimeout(int seconds);
    public void cleanTxnTimeout(); // clean up thread specific timeout
    /**
     * Returns a list of resource handles held by the component
     */

    public List getExistingResourceList(Object instance, ComponentInvocation inv);

    public void registerComponentResource(TransactionalResource h);

    public void unregisterComponentResource(TransactionalResource h);

    public void recover(XAResource[] resourceList);

    /**
     * Initialize recovery framework
     * @param force if true, forces initialization, otherwise relies on the TimerService 
     * configuration.
     */
    public void initRecovery(boolean force);

    /**
     * Perform shutdown cleanup.
     */
    public void shutdown();

    public void begin(int timeout)
        throws NotSupportedException, SystemException;

    /**
     * Return true if a "null transaction context" was received
     * from the client or if the server's transaction.interoperability
     * flag is false.
     * A null tx context indicates that the client had an active
     * tx but the client container did not support tx interop.
     * See EJB2.0 spec section 18.5.2.1.
     */
    public boolean isNullTransaction();

    /**
     * Perform checks during export of a transaction on a remote call.
     */
    public void checkTransactionExport(boolean isLocal);

    /**
     * Perform checks during import of a transaction on a remote call.
     * This is called from the reply interceptors after a remote call completes.
     */
    public void checkTransactionImport();


    /**
     * Utility for the ejb container to check if the transaction is marked for
     * rollback because of timeout. This is applicable only for local transactions
     * as jts transaction will rollback instead of setting the txn for rollback
     */
    public boolean isTimedOut();

    // START IASRI 4662745

    /*
     * Returns the list of ActiveTransactions. Called by Admin framework
     *  The ArrayList contains TransactionAdminBean
     */
    public java.util.ArrayList getActiveTransactions();

    /*
     * Called by Admin Framework. Forces the given transaction to be rolled back
     */
    public void forceRollback(String txnId) throws IllegalStateException, SystemException;

    /*
     * Called by Admin Framework.
     */
    public void setMonitoringEnabled(boolean enabled);

    /*
     * Called by Admin Framework.
     */
    public void freeze();
    /*
     * Called by Admin Framework.
     */
    public void unfreeze();

    /*
     * Called by Admin Framework
     */
    public boolean isFrozen();

    // END IASRI 4662745


   /**
     * recreate a transaction based on the Xid. This call causes the calling
     * thread to be associated with the specified transaction. <p>
     * This is used by importing transactions via the Connector contract.
     *
     * @param xid the Xid object representing a transaction.
     */
    public void recreate(Xid xid, long timeout) throws WorkException ;

    /**
     * Release a transaction. This call causes the calling thread to be
     * dissociated from the specified transaction. <p>
     * This is used by importing transactions via the Connector contract.
     *
     * @param xid the Xid object representing a transaction.
     */
    public void release(Xid xid) throws WorkException ;

    /**
     * Provides a handle to a <code>XATerminator</code> instance. The
     * <code>XATerminator</code> instance could be used by a resource adapter
     * to flow-in transaction completion and crash recovery calls from an EIS.
     * <p>
     * This is used by importing transactions via the Connector contract.
     *
     * @return a <code>XATerminator</code> instance.
     */
    public XATerminator getXATerminator() ;

    /**
     * Explicitly set the JavaEETransactionManagerDelegate instance
     * for implementation-specific callbacks.
     *
     * @param delegate the JavaEETransactionManagerDelegate instance.
     */
    public void setDelegate(JavaEETransactionManagerDelegate delegate);

    /**
     *
     * Return JavaEETransaction instance associated with the current thread.
     *
     * @return the JavaEETransaction associated with the current thread or null
     * if it there is none.
     */
    public JavaEETransaction getCurrentTransaction();

    /**
     *
     * Update JavaEETransaction associated with the current thread.
     *
     * @param tx the JavaEETransaction associated with the current thread or null
     * if the existing transaction had been completed.
     */
    public void setCurrentTransaction(JavaEETransaction tx);

    /**
     *
     * Return XAResourceWrapper instance specific to this datasource class name 
     * that can be used instead of the driver provided version for transaction recovery.
     *
     * @param clName the class name of a datasource.
     * @return the XAResourceWrapper instance specific to this datasource class 
     * name or null if there is no special wrapper available.
     */
    public XAResourceWrapper getXAResourceWrapper(String clName);

    /**
     * Handle configuration change. Actual change will be performed by the delegate.
     *
     * @param name the name of the configuration property.
     * @param value the ne value of the configuration.
     */
    public void handlePropertyUpdate(String name, Object value);

    /**
     * Called by the ResourceRecoveryManager to recover the populated
     * array of XAResource.
     *
     * @param delegated <code>true</code> if the recovery process is owned by this instance.
     * @param logPath the name of the transaction logging directory
     * @param xaresArray the array of XA Resources to be recovered.
     * @return true if the recovery has been successful.
     */
    public boolean recoverIncompleteTx(boolean delegated, String logPath, 
            XAResource[] xaresArray) throws Exception;

    /**
     * get the resources being used in the calling component's invocation context
     * @param instance calling component instance
     * @param inv Calling component's invocation information
     * @return List of resources
     */
    public List getResourceList(Object instance, ComponentInvocation inv);

    /**
     * Clears the transaction associated with the caller thread
     */
    public void clearThreadTx();

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

    /**
     * Returns the value to be used to purge transaction tasks after the specified number of cancelled tasks
     */
    public int getPurgeCancelledTtransactionsAfter();

    /**
     * Allows to purge transaction tasks after the specified value of cancelled tasks
     */
    public void setPurgeCancelledTtransactionsAfter(int value);


}
