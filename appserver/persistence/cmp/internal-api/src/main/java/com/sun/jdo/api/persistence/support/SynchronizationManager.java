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
 * SynchronizationManager.java
 *
 * Created on May 30, 2002, 8:43 AM
 */

package com.sun.jdo.api.persistence.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;

import javax.transaction.Synchronization;

/** This class allows for multiple instances to be called at transaction
 * completion, which JDO does not currently provide.  JDO only provides 
 * for a single instance to be registered.  This service exploits 
 * the JDO capability by registering an instance of SynchronizationManager
 * with JDO and then calling each instance registered with itself.
 *
 * @author  Craig Russell
 * @version 1.0
 */
public class SynchronizationManager implements Synchronization {

    /** Creates new SynchronizationManager instance specifying the initial
     * capacity of the list of Synchronization instances.
     * @param initialCapacity the initial capacity of the List of Synchronization instances
     */
    public SynchronizationManager(int initialCapacity) {
        synchronizations = new ArrayList(initialCapacity);
    }
    
    /** Creates new SynchronizationManager instance with a default
     * capacity of the List of Synchronization instances.
     */
    public SynchronizationManager() {
        this(defaultCapacity);
    }
    
    /** Register a new Synchronization with the current transaction.
     * @param instance the instance to be registered
     * @param pm the persistence manager which manages this transaction
     */    
    public static void registerSynchronization(Synchronization instance, PersistenceManager pm) {
        SynchronizationManager synchronizationManager = getSynchronizationManager(pm);
        synchronizationManager.registerSynchronization(instance);
    }
    
    /** Specify the default capacity of the list of Synchronizations.
     * @param capacity the default capacity of the List of Synchronizations
     */
    public static void setDefaultCapacity(int capacity) {
        defaultCapacity = capacity;
    }
    
    /** The default capacity of the List of Synchronizations.
     */
    protected static int defaultCapacity = 100;
    
    /** The list of instances to synchronize.  Duplicate registrations will
     * result in the instance being called multiple times.  Since we cannot
     * depend on the caller implementing hashCode and equals, we cannot use
     * a Set implementaion.
     */
    protected final List synchronizations;
    
    /** Creates new SynchronizationManager instance and registers it with
     * the persistence manager.
     * @param pm the persistence manager managing this transaction
     */
    protected SynchronizationManager(PersistenceManager pm) {
        this();
        Transaction tx = pm.currentTransaction();
        tx.setSynchronization((Synchronization)this);
    }
    
    /** Get the synchronization manager already registered with this persistence manager.
     * If the synchronization instance is not of the proper class, then replace it with 
     * a new instance of the synchronization manager, and register the previous synchronization
     * with the newly created synchronization manager.
     * @param pm the persistence manager
     * @return the synchronization manager
     */    
    protected static SynchronizationManager getSynchronizationManager(PersistenceManager pm) {
        Transaction tx = pm.currentTransaction();
        Synchronization oldsync = tx.getSynchronization();
        if (oldsync instanceof SynchronizationManager) {
            // This is the one we want.
            return (SynchronizationManager) oldsync;
        } else {
            // We need a new one.  The constructor automatically registers it 
            // with the persistence manager.
            SynchronizationManager newsync = new SynchronizationManager(pm);
            if (oldsync != null) {
                // There is an existing Synchronization to register with the new one
                newsync.registerSynchronization(oldsync);
            }
            return newsync;
        }
    }
    
    /** This method will be called during transaction completion.  Resource
     * access is allowed.
     * This method in turn calls each registered instance beforeCompletion 
     * method.
     */
    public void beforeCompletion() {
        int size = synchronizations.size();
        for (int i = 0; i < size; ++i) {
            Synchronization instance = (Synchronization) synchronizations.get(i);
            instance.beforeCompletion();
        }
    }
    
    /** This method will be called during transaction completion.  No resource
     * access is allowed.
     * This method in turn calls each registered instance afterCompletion
     * method.  After this method completes, 
     * instances must register again in the new transaction, but
     * the synchronization manager remains bound to the persistence manager
     * transaction instance.
     * @param status the completion status of the transaction
     */    
    public void afterCompletion(int status) {
        int size = synchronizations.size();
        StringBuffer sb = null;
        for (int i = 0; i < size; ++i) {
            Synchronization instance = (Synchronization) synchronizations.get(i);
            try {
                instance.afterCompletion(status);
            } catch (Exception e) {
                if (sb == null) {
                    sb = new StringBuffer();
                }
                sb.append(e.getMessage()).append('\n'); // NOI18N
            }
        }
        synchronizations.clear();
        if (sb != null) {
            throw new JDOUserException(sb.toString());
        }
    }
    
    /** Register an instance with this synchronization manager.
     * Note that this is not thread-safe.  If multiple threads call this method
     * at the same time, the synchronizations List might become corrupt.
     * The correct way to fix this is to ask the PersistenceManager for the
     * Multithreaded flag and perform a synchronized add if the flag is true.
     * We currently do not have the Multithreaded flag implemented.
     * @param instance the instance to be registered
     */    
    protected void registerSynchronization(Synchronization instance) {
        synchronizations.add(instance);
    }
    
}
