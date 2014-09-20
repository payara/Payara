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

import javax.transaction.*;
import javax.resource.spi.XATerminator;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.invocation.ComponentInvocation;

import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.api.TransactionImport;

/**
* This class is wrapper for the actual transaction manager implementation.
* JNDI lookup name "java:appserver/TransactionManager"
* see the com/sun/enterprise/naming/java/javaURLContext.java
**/

@Service
@ContractsProvided({TransactionManagerHelper.class, TransactionManager.class}) // Needed because we can't change spec provided class
public class TransactionManagerHelper implements TransactionManager, TransactionImport {

    @Inject
    private transient JavaEETransactionManager transactionManager;

    @Inject
    private transient InvocationManager invocationManager;

    public void begin() throws NotSupportedException, SystemException {
        transactionManager.begin();
    }

    
    public void commit() throws RollbackException,
            HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {
        transactionManager.commit();
    }

    public int getStatus() throws SystemException {
        return transactionManager.getStatus();
    }

    public Transaction getTransaction() throws SystemException {
        return transactionManager.getTransaction();
    }

    
    public void resume(Transaction tobj)
            throws InvalidTransactionException, IllegalStateException,
            SystemException {
        transactionManager.resume(tobj);
        preInvokeTx(false);
    }

    
    public void rollback() throws IllegalStateException, SecurityException,
                            SystemException {
        transactionManager.rollback();
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        transactionManager.setRollbackOnly();
    }

    public void setTransactionTimeout(int seconds) throws SystemException {
        transactionManager.setTransactionTimeout(seconds);
    }

    public Transaction suspend() throws SystemException {
        postInvokeTx(true, false);
        return transactionManager.suspend();
    }

    public void recreate(Xid xid, long timeout) {
        final JavaEETransactionManager tm = transactionManager;
        
        try {
            tm.recreate(xid, timeout);
        } catch (javax.resource.spi.work.WorkException ex) {
            throw new IllegalStateException(ex);
        }
        preInvokeTx(true);
    }

    public void release(Xid xid) {
        final JavaEETransactionManager tm = transactionManager;
     
        postInvokeTx(false, true);
        try {
            tm.release(xid);    
        } catch (javax.resource.spi.work.WorkException ex) {
            throw new IllegalStateException(ex);
        }  finally { 
            if (tm instanceof JavaEETransactionManagerSimplified) {
                ((JavaEETransactionManagerSimplified) tm).clearThreadTx();
            }
        } 
    }
    
    public XATerminator getXATerminator() {
        return transactionManager.getXATerminator();
    }



    
    /**
     * PreInvoke Transaction configuration for Servlet Container.
     * BaseContainer.preInvokeTx() handles all this for CMT EJB.
     *
     * Compensate that JavaEEInstanceListener.handleBeforeEvent(
     * BEFORE_SERVICE_EVENT)
     * gets called before WSIT WSTX Service pipe associates a JTA txn with 
     * incoming thread.
     *
     * Precondition: assumes JTA transaction already associated with current 
     * thread.
     */
    public void preInvokeTx(boolean checkServletInvocation) {
        final ComponentInvocation inv = invocationManager.getCurrentInvocation();
        if (inv != null && (!checkServletInvocation ||
            inv.getInvocationType() == 
                     ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION)){
            try { 
                // Required side effect: note that 
                // enlistComponentResources calls
                // ComponentInvocation.setTransaction(currentJTATxn).
                // If this is not correctly set, managed XAResource connections
                // are not auto enlisted when they are created.
                transactionManager.enlistComponentResources();
            } catch (java.rmi.RemoteException re) {
                throw new IllegalStateException(re);
            }
        }
    }    
    
    /**
     * PostInvoke Transaction configuration for Servlet Container.
     * BaseContainer.preInvokeTx() handles all this for CMT EJB.
     *
     * Precondition: assumed called prior to current transcation being 
     * suspended or released.
     * 
     * @param suspend indicate whether the delisting is due to suspension or 
     * transaction completion(commmit/rollback)
     */
    public void postInvokeTx(boolean suspend, boolean checkServletInvocation) {
        final ComponentInvocation inv = invocationManager.getCurrentInvocation();
        if (inv != null && (!checkServletInvocation || inv.getInvocationType() == 
            ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION)) {
            try {
                transactionManager.delistComponentResources(suspend);
            } catch (java.rmi.RemoteException re) {
                throw new IllegalStateException(re);
            } finally {   
                inv.setTransaction(null);
            }
        }
    }
    
     /**
     * Return duration before current transaction would timeout.
     *
     * @return Returns the duration in seconds before current transaction would
     *         timeout.
     *         Returns zero if transaction has no timeout set and returns 
     *         negative value if transaction already timed out.
     *
     * @exception IllegalStateException Thrown if the current thread is
     *    not associated with a transaction.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition.
     */
    public int getTransactionRemainingTimeout() throws SystemException {
        int timeout = 0;
        Transaction txn = getTransaction(); 
        if (txn == null) {
            throw new IllegalStateException("no current transaction");
        } else if (txn instanceof JavaEETransactionImpl) {
            timeout = ((JavaEETransactionImpl)txn).getRemainingTimeout();
        }
        return timeout;
    }

    /** {@inheritDoc}
    */
    public void registerRecoveryResourceHandler(XAResource xaResource) {
        transactionManager.registerRecoveryResourceHandler(xaResource);
    }
}
