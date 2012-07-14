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

import javax.transaction.Synchronization;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.RollbackException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import com.sun.enterprise.util.i18n.StringManager;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.ContractsProvided;

@Service
@ContractsProvided({TransactionSynchronizationRegistryImpl.class,
                    TransactionSynchronizationRegistry.class}) // Needed because we can't change spec provided class
public class TransactionSynchronizationRegistryImpl 
             implements TransactionSynchronizationRegistry {

    @Inject
    private transient TransactionManager transactionManager;

    private static StringManager sm = 
                   StringManager.getManager(TransactionSynchronizationRegistryImpl.class);

    public TransactionSynchronizationRegistryImpl() {
    }

    public TransactionSynchronizationRegistryImpl(TransactionManager t) {
        transactionManager = t;
    }

    /**
     * Return an opaque object to represent the transaction bound to the
     * current thread at the time this method is called. The returned object
     * overrides <code>hashCode</code> and <code>equals</code> methods to allow
     * its use as the key in a <code>java.util.HashMap</code> for use by the
     * caller. If there is no transaction currently active, null is returned.
     *
     * <P>The returned object will return the same <code>hashCode</code> and
     * compare equal to all other objects returned by calling this method
     * from any component executing in the same transaction context in the
     * same application server.
     *
     * <P>The <code>toString</code> method returns a <code>String</code>
     * that might be usable by a human reader to usefully understand the
     * transaction context. The result of the <code>toString</code> method
     * is otherwise not defined. Specifically, there is no forward or backward
     * compatibility guarantee for the result returned by the
     * <code>toString</code> method.
     *
     * <P>The object is not necessarily serializable, and is not useful
     * outside the virtual machine from which it was obtained.
     *
     * @return    An object representing the current transaction,
     *            or null if no transaction is active.
     */
    public Object getTransactionKey() {
        try {
            return transactionManager.getTransaction();
        } catch (SystemException ex) {
            return null;
        }
    }
    
    /**
     * Add an object to the map of resources being managed for
     * the current transaction. The supplied key must be of a caller-
     * defined class so as not to conflict with other users. The class
     * of the key must guarantee that the <code>hashCode</code> and
     * <code>equals</code> methods are suitable for keys in a map.
     * The key and value are not examined or used by the implementation.
     *
     * @param    key    The key for looking up the associated value object.
     *
     * @param    value  The value object to keep track of.
     *
     * @exception IllegalStateException Thrown if the current thread
     * is not associated with a transaction.
     */
    public void putResource(Object key, Object value) {
        try {
            JavaEETransactionImpl tran = 
                    (JavaEETransactionImpl)transactionManager.getTransaction();    
            if (tran == null)
                throw new IllegalStateException(
                      sm.getString("enterprise_distributedtx.no_transaction"));
            tran.putUserResource(key, value);
        } catch (SystemException ex) {
            throw new IllegalStateException(
                      sm.getString("enterprise_distributedtx.no_transaction"));
        }
    }
    
    /**
     * Get an object from the map of resources being managed for
     * the current transaction. The key must have been supplied earlier
     * by a call to <code>putResouce</code> in the same transaction. If the key 
     * cannot be found in the current resource map, null is returned.
     *
     * @param    key  The key for looking up the associated value object.
     *
     * @return   The value object, or null if not found.
     *
     * @exception IllegalStateException Thrown if the current thread
     * is not associated with a transaction.
     */
    public Object getResource(Object key){
        try {
            JavaEETransactionImpl tran = 
                    (JavaEETransactionImpl)transactionManager.getTransaction();    
            if (tran == null)
                throw new IllegalStateException(
                      sm.getString("enterprise_distributedtx.no_transaction"));
            return tran.getUserResource(key);
        } catch (SystemException ex) {
            throw new IllegalStateException(
                      sm.getString("enterprise_distributedtx.no_transaction"));
        }
    }

    /**
     * Register a <code>Synchronization</code> instance with special ordering
     * semantics. The <code>beforeCompletion</code> method on the registered
     * <code>Synchronization</code> will be called after all user and
     * system component <code>beforeCompletion</code> callbacks, but before
     * the 2-phase commit process starts. This allows user and system components to
     * flush state changes to the caching manager, during their
     * <code>SessionSynchronization</code> callbacks, and allows  managers
     * to flush state changes to Connectors, during the callbacks
     * registered with this method. Similarly, the <code>afterCompletion</code>
     * callback will be called after 2-phase commit completes but before
     * any user and system <code>afterCompletion</code> callbacks.
     *
     * <P>The <code>beforeCompletion</code> callback will be invoked in the
     * transaction context of the current transaction bound to the thread
     * of the caller of this method, which is the same transaction context
     * active at the time this method is called. Allowable methods include
     * access to resources, for example, Connectors. No access is allowed to 
     * user components, for example, timer services or bean methods,
     * as these might change the state of POJOs, or plain old Java objects, 
     * being managed by the caching manager.
     *
     * <P>The <code>afterCompletion</code> callback will be invoked in an
     * undefined transaction context. No access is permitted to resources or
     * user components as defined above.  Resources can be closed, but
     * no transactional work can be performed with them.
     *
     * <P>Other than the transaction context, no component J2EE context is
     * active during either of the callbacks.
     *
     * @param    sync    The synchronization callback object.
     *
     * @exception IllegalStateException Thrown if the current thread
     * is not associated with a transaction.
     */
    public void registerInterposedSynchronization(Synchronization sync) {
        try {
            JavaEETransactionImpl tran = 
                    (JavaEETransactionImpl)transactionManager.getTransaction();    
            if (tran == null)
                throw new IllegalStateException(
                      sm.getString("enterprise_distributedtx.no_transaction"));
            tran.registerInterposedSynchronization(sync);
        } catch (SystemException ex) {
            throw new IllegalStateException(
                      sm.getString("enterprise_distributedtx.no_transaction"));
        } catch (RollbackException ex) {
            throw new IllegalStateException(
                      sm.getString("enterprise_distributedtx.mark_rollback"));
        }
    }

    /**
     * Returns the status of the transaction bound to the current thread.
     * This is the result of executing <code>getStatus</code> method on the
     * <code>TransactionManager</code>, in the current transaction context.
     *
     * @return The status of the current transaction.
     */
    public int getTransactionStatus() {
        try {
            return transactionManager.getStatus();    
        } catch (SystemException ex) {
           return Status.STATUS_NO_TRANSACTION;
        }
    }

    /**
     * Set the <code>rollbackOnly</code> status of the transaction bound to the
     * current thread.
     *
     * @exception IllegalStateException Thrown if the current thread
     * is not associated with a transaction.
     */
    public void setRollbackOnly() {
        try {
            transactionManager.setRollbackOnly();    
        } catch (SystemException ex) {
            throw new IllegalStateException(
                      sm.getString("enterprise_distributedtx.no_transaction"));
        }
    }

    /**
     * Get the <code>rollbackOnly</code> status of the transaction bound to the
     * current thread.
     *
     * @return  true, if the current transaction is marked for rollback only.
     *
     * @exception IllegalStateException Thrown if the current thread
     * is not associated with a transaction.
     */
    public boolean getRollbackOnly() {
    {
        int status = getTransactionStatus();
        if ( status == Status.STATUS_NO_TRANSACTION ) {
            throw new IllegalStateException(
                      sm.getString("enterprise_distributedtx.no_transaction"));
        }
        if ( status == Status.STATUS_MARKED_ROLLBACK
            || status == Status.STATUS_ROLLING_BACK )
                return true;
            else
                return false;
        }
    }
}
