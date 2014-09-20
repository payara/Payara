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
 * Transaction.java
 *
 * Created on February 25, 2000
 */
 
package com.sun.jdo.api.persistence.support;
import javax.transaction.*;

/** The JDO Transaction interface is a sub-interface of the PersistenceManager
 * that deals with options and completion of transactions under user control.
 *
 * <P>Transaction options include whether optimistic concurrency
 * control should be used for the current transaction, and whether values
 * should be retained in JDO instances after transaction completion.
 *
 * <P>Transaction completion methods have the same semantics as javax.transaction
 * UserTransaction, and are valid only in the non-managed, non-distributed
 * transaction environment.
 * @author Craig Russell
 * @version 0.1
 */

public interface Transaction
{
    /** Begin a transaction.  The type of transaction is determined by the
   * setting of the Optimistic flag.
   * @see #setOptimistic
   * @see #getOptimistic
   * @throws JDOUserException if a distributed transaction XAResource
   * is assigned to this Transaction
   */
    void begin();
    
    /** Commit the current transaction.
     */
    void commit();
    
    /** Roll back the current transaction.
     */
    void rollback();

    /** Returns whether there is a transaction currently active.
     * @return boolean
     */
    boolean isActive();
    
    /** If true, at commit instances retain their values and the instances
     * transition to persistent-nontransactional.
     * <P>Setting this flag also sets the NontransactionalRead flag.
     * @param retainValues the value of the retainValues property
     */
    void setRetainValues(boolean retainValues);
    
    /** If true, at commit time instances retain their field values.
     * @return the value of the retainValues property
     */
    boolean getRetainValues();
    
    /** If true, at rollback instances restore their values and the instances
     * transition to persistent-nontransactional.
     * @param restoreValues the value of the restoreValues property
     */
    void setRestoreValues(boolean restoreValues);
    
    /** If true, at rollback time instances restore their field values.
     * @return the value of the restoreValues property
     */
    boolean getRestoreValues();
    
    /** Optimistic transactions do not hold data store locks until commit time.
     * @param optimistic the value of the Optimistic flag.
     */
    void setOptimistic(boolean optimistic);
    
    /** Optimistic transactions do not hold data store locks until commit time.
     * @return the value of the Optimistic property.
     */
    boolean getOptimistic();

    /** If this flag is set to true, then queries and navigation are allowed 
     * without an active transaction
     * @param flag	 the value of the nontransactionalRead property.
     */
    void setNontransactionalRead (boolean flag);
    
    /** If this flag is set to true, then queries and navigation are allowed 
     * without an active transaction
     * @return the value of the nontransactionalRead property.
     */
    boolean getNontransactionalRead (); 

    /** The user can specify a Synchronization instance to be notified on
     * transaction completions.  The beforeCompletion method is called prior
     * to flushing instances to the data store.
     *
     * <P>The afterCompletion method is called after performing the data store
     * commit operation.
     * @param sync the Synchronization instance to be notified; null for none
     */
    void setSynchronization(Synchronization sync);
    
    /** The user-specified Synchronization instance for this Transaction instance.    
     * @return the user-specified Synchronization instance.
     */
    Synchronization getSynchronization();

   /**
    * Sets the number of seconds to wait for a query statement
    * to execute in the datastore associated with this  Transaction instance
    * @param timeout          new timout value in seconds; zero means unlimited
    */
   void setQueryTimeout (int timeout);

   /**
    * Gets the number of seconds to wait for a query statement
    * to execute in the datastore associated with this  Transaction instance
    * @return      timout value in seconds; zero means unlimited
    */
   int getQueryTimeout ();
   
   /**
    * Sets the number of seconds to wait for an update statement
    * to execute in the datastore associated with this  Transaction instance
    * @param timeout          new timout value in seconds; zero means unlimited
    */
   void setUpdateTimeout (int timeout);
   
   /**
    * Gets the number of seconds to wait for an update statement
    * to execute in the datastore associated with this  Transaction instance
    * @return      timout value in seconds; zero means unlimited
    */
   int getUpdateTimeout();

    /** The Tranansaction instance is always associated with exactly one
     * PersistenceManager.
     *
     * @return the PersistenceManager for this Transaction instance
     */
    PersistenceManager getPersistenceManager();
}
