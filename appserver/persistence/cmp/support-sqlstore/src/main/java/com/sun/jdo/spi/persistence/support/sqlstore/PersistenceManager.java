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
 * PersistenceManager.java
 *
 * Create on March 3, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore;

import com.sun.jdo.spi.persistence.support.sqlstore.impl.PersistenceManagerWrapper;

/**
 */
public interface PersistenceManager
        extends com.sun.jdo.api.persistence.support.PersistenceManager
{
        public PersistenceManagerWrapper getCurrentWrapper();

        public Object newInstance(StateManager sm);

        public void setStateManager(Object pc, StateManager sm);

        public void setFlags(Object pc, byte flags);

        public byte getFlags(Object pc);

        public StateManager getStateManager(Object pc);

        public void setField(Object pc, int fieldNumber, Object value);

        public Object getField(Object pc, int fieldNumber);

        public void clearFields(Object pc);

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
        public Object retrieve(RetrieveDesc action, ValueFetcher parameters);

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
        public Object retrieve(RetrieveDesc action);

        /**
         * Return a RetrieveDesc given a Class object.
         */
        public RetrieveDesc getRetrieveDesc(Class classType);

        /**
         * Return a RetrieveDesc for a foreign field (relationship) given the
         * Class object for the parent class.
         */
        public RetrieveDesc getRetrieveDesc(String fieldName, Class classType);

        /**
         * Called by Transaction commit() or rollback()
         * cleans up transactional cache
         * @param		status		javax.transaction.Status
         */
        public void afterCompletion(int status);

        /**
         * Called by Transaction commit()
         * Loops through transactional cache and calls PersistentStore.updatePersistent()
         * on each instance
         */
        public void beforeCompletion();

        /**
         * Called by Query in pessimistic transaction
         * to flush changes to the database
         */
        public void internalFlush();

        /**
         * Called by StateManager to register new instance. This method will throw
         * an JDOUserException if throwDuplicateException is true and the object being
         * registered already exists in the pm cache.
         */
        public void registerInstance(StateManager sm, Object oid,
             boolean throwDuplicateException, boolean forceRegister);

        /**
         * Called by StateManager to register persistent instance at the rollback if
         * it was removed from the global (weak) cache as the result of the replace
         * operation.
         */
        public void registerInstance(StateManager sm, Object oid);

        /**
         * Deregister an instance.
         */
        public void deregisterInstance(Object oid);

        /**
         * Deregister an instance with this object Id, only if it holds the same instance.
         */
        public void deregisterInstance(Object oid, StateManager sm);

        /**
         * For Transaction to notify PersistenceManager that
         * status is changed
         */
        public void notifyStatusChange(boolean isActive);

        /**
         * For Transaction to notify PersistenceManager that
         * optimistic flag is changed
         */
        public void notifyOptimistic(boolean optimistic);

        /**
         * Returns true if associated transaction is optimistic
         */
        public boolean isOptimisticTransaction();

        /**
         * For Transaction to notify PersistenceManager that
         * optimistic flag is changed
         */
        public void notifyNontransactionalRead(boolean nontransactionalRead);

        /**
         * Returns true if nontransactionalRead flag is set to true.
         */
        public boolean isNontransactionalRead();

        /**
         * Returns true if associated transaction is active
         */
        public boolean isActiveTransaction();

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
        public Object newSCOInstanceInternal (Class type, Object owner, String fieldName);

        /**
         * Called by newCollectionInstance from the public interface
         * or internally by the runtime
         * Will not result in marking field as dirty
         *
         * @param type Class of the new SCO instance
         * @param owner the owner to notify upon changes
         * @param fieldName the field to notify upon changes
         * @param elementType the element types allowed
         * @param allowNulls true if allowed
         * @param initialSize initial size of the Collection
         * @return the object of the class type
         */
    Object newCollectionInstanceInternal (Class type, Object owner, String fieldName,
                Class elementType, boolean allowNulls, int initialSize);

        /**
         * Serialize field updates
         */
        public void acquireFieldUpdateLock();

        /**
         * Allow other threads to update fields
         */
        public void releaseFieldUpdateLock();

        /**
     * Acquires a share lock from the persistence manager. This method will
     * put the calling thread to sleep if another thread is holding the exclusive lock.
         */
        public void acquireShareLock();

        /**
     * Releases the share lock and notify any thread waiting to get an exclusive lock.
         * Note that every releaseShareLock() call needs to be preceeded by an acquireShareLock() call.
         */
        public void releaseShareLock();

        /**
     * Acquires an exclusive lock from the persistence manager. By acquiring an
         * exclusive lock, a thread is guaranteed to have exclusive right to the persistence
     * runtime meaning no other threads can perform any operation in the runtime.
         */
        public void acquireExclusiveLock();

        /**
     * Release the exclusive lock and notify any thread waiting to get an exclusive or
         * share lock. Note that every releaseShareLock() call needs to be preceeded by
         * an acquireExclusiveLock() call.
         */
        public void releaseExclusiveLock();

    /**
     * Force to close the persistence manager. Called by
     * TransactionImpl.afterCompletion in case of the CMT transaction
     * and the status value passed to the method cannot be resolved.
     */
    public void forceClose();

    /**
     * Returns StateManager instance for this Object Id.
     * @param oid the ObjectId to look up.
     * @param pcClass the expected Class type of the new PC instance.
     */
    public StateManager findOrCreateStateManager(Object oid, Class pcClass);

    /**
     * Lock cache for getObjectById and result processing synchronization.
     */
    public void acquireCacheLock();

    /** Release cache lock.
     */
    public void releaseCacheLock();

    /**
     * Looks up the given instance in the Version Consistency cache and
     * if found, populates it from the cached values.
     * @param sm Instance to be looked up in the version consistency cache.
     * If found, it is populated with values from the cache.
     * @return true if the <code>sm</code> was found and populated, false
     * otherwise.
     */
    public boolean initializeFromVersionConsistencyCache(StateManager sm);

}
