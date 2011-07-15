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
 * CMPHelper.java
 *
 * Created on April 25, 2002
 */
package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import java.util.ResourceBundle;

import javax.ejb.EJBObject;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBContext;
import javax.ejb.EntityContext;

import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.api.persistence.support.PersistenceManager;
import com.sun.jdo.api.persistence.support.PersistenceManagerFactory;
import com.sun.jdo.api.persistence.support.Transaction;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperPersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.impl.PersistenceManagerImpl;
import com.sun.jdo.spi.persistence.support.sqlstore.impl.PersistenceManagerWrapper;

  /** Provides helper methods for CMP support implementation with the
   * application server specific information. Calls corresponding methods 
   * on the registered class which implements ContainerHelper interface.
   */
public class CMPHelper {

    /** I18N message handler */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
        "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
        CMPHelper.class.getClassLoader());

    /** The logger */
    private static Logger logger = LogHelperPersistenceManager.getLogger();

   /** Reference to a class that implements ContainerHelper interface for this
    * particular application server. In a non-managed environment the
    * will throw JDOFatalInternalException for any method invocation.
    */
    private static ContainerHelper containerHelper = null;

    /** This counter is used to populate primary key columns for CMP beans with
     * an unknown Primary Key Class. It is a single counter per vm and is
     * initialized at the start up time.
     */
   private static long counter = System.currentTimeMillis();

   /** Register class that implements ContainerHelper interface
    * Should be called by a static method at class initialization time.
    *
    * @param 	h 	application server specific implemetation of the ContainerHelper
    * 			interface.
    */
    public static void registerContainerHelper (ContainerHelper h) {
        containerHelper = h;
    }

    /** Increments the counter and returns the value. Used to populate primary
     * key columns for EJB with an unknown Primary Key Class. 
     * @return the next value for the counter.
     */

    public synchronized static long getNextId() {
        counter++;
        return counter;
    }

    /** Called in a CMP supported environment to get a Container instance that
     * will be passed unchanged to the required methods.  In a non-managed environment
     * throws JDOFatalInternalException.
     * The info argument can be an array of Objects if necessary.
     *
     * @see getEJBObject(Object, Object)
     * @see getEJBLocalObject(Object, Object)
     * @see getEJBLocalObject(Object, Object, EJBContext)
     * @see removeByEJBLocalObject(EJBLocalObject, Object)
     * @see removeByPK(Object, Object)
     * @param info Object with the request information that is application server
     * specific.
     * @return a Container instance as an Object.
     * @throws JDOFatalInternalException if ContainerHelper instance is not registered.
     */
    public static Object getContainer(Object info) {
        return getContainerHelper().getContainer(info);
    }

    /** Called in a CMP supported environment to get an EJBObject reference for this
     * primary key instance and Container object. 
     * The Container instance is acquired via #getContainer(Object).
     *   
     * @see getContainer(Object)
     * @param pk the primary key instance.
     * @param container a Container instance for the request.
     * @return a corresponding EJBObject (as an Object) to be used by
     * the client.
     */  
    public static EJBObject getEJBObject(Object pk, Object container) {
        return getContainerHelper().getEJBObject(pk, container);
    }
 
    /** Called in a managed environment to get an EJBLocalObject reference for this
     * primary key instance and Container object. 
     * The Container instance is acquired via #getContainer(Object).
     *   
     * @see getContainer(Object)
     * @param pk the primary key instance.
     * @param container a Container instance for the request.
     * @return a corresponding EJBLocalObject (as an Object) to be used by
     * the client.
     */   
    public static EJBLocalObject getEJBLocalObject(Object pk, Object container) {
        return getContainerHelper().getEJBLocalObject(pk, container); 
    }
 
    /** Called in a managed environment to get an EJBLocalObject reference for this
     * primary key instance, Container object, and EJBContext of the calling bean.
     * Allows the container to check if this method is called during ejbRemove
     * that is part of a cascade-delete remove.
     * The Container instance is acquired via #getContainer(Object).
     *   
     * @see getContainer(Object)
     * @param pk the primary key instance.
     * @param container a Container instance for the request.
     * @param context an EJBContext of the calling bean.
     * @return a corresponding EJBLocalObject (as an Object) to be used by
     * the client.
     */   
    public static EJBLocalObject getEJBLocalObject(Object pk, Object container,
        EJBContext context) {
        return getContainerHelper().getEJBLocalObject(pk, container, context); 
    }
 
    /** Called in a managed environment to remove a bean for a given EJBLocalObject,
     * and Container instance.
     * The Container instance is acquired via #getContainer(Object).
     *   
     * @see getContainer(Object)
     * @param ejb the EJBLocalObject for the bean to be removed.
     * @param container a Container instance for the request. 
     */  
    public static void removeByEJBLocalObject(EJBLocalObject ejb, Object container) {
        getContainerHelper().removeByEJBLocalObject(ejb, container);  
    }
 
    /** Called in a managed environment to remove a bean for a given primary key 
     * and Container instance.
     * The Container instance is acquired via #getContainer(Object).
     *
     * @see getContainer(Object)
     * @param pk the primary key for the bean to be removed.
     * @param container a Container instance for the request.
     */ 
    public static void removeByPK(Object pk, Object container) {
        getContainerHelper().removeByPK(pk, container);  
    }

    /** Called in a managed environment to mark EntityContext of the
     * bean as already removed during cascade-delete operation.
     * Called by the generated ejbRemove method before calling ejbRemove of the 
     * related beans (via removeByEJBLocalObject) that are to be cascade-deleted.
     *
     * The Container instance is acquired via #getContainer(Object).
     *   
     * @param context the EntityContext of the bean beeing removed.
     */  
    public static void setCascadeDeleteAfterSuperEJBRemove(EntityContext context) {
        getContainerHelper().setCascadeDeleteAfterSuperEJBRemove(context);  
    }

    /** Called in a CMP environment to lookup PersistenceManagerFactory
     * referenced by this Container instance as the CMP resource.
     * The Container instance is acquired via #getContainer(Object).
     *
     * @see getContainer(Object)
     * @param container a Container instance for the request.
     */  
    public static PersistenceManagerFactory getPersistenceManagerFactory(Object container) {
        return getContainerHelper().getPersistenceManagerFactory(container);
    }

    /** Called in a CMP environment to verify that the specified object
     * is of a valid local interface type.
     * The Container instance is acquired via #getContainer(Object).
     *
     * @see getContainer(Object)
     * @param o the instance to validate.
     * @param container a Container instance for the request.
     */
    public static void assertValidLocalObject(Object o, Object container) {
        getContainerHelper().assertValidLocalObject(o, container);
    }

    /** Called in a CMP environment to verify that the specified object
     * is of a valid remote interface type.
     * The Container instance is acquired via #getContainer(Object).
     *
     * @see getContainer(Object)
     * @param o the instance to validate.
     * @param container a Container instance for the request.
     */
    public static void assertValidRemoteObject(Object o, Object container) {
        getContainerHelper().assertValidRemoteObject(o, container);
    }

    /** Called in a CMP supported environment. Notifies the container that 
     * ejbSelect had been called.
     * The Container instance is acquired via #getContainer(Object).
     *   
     * @see getContainer(Object)
     * @param container a Container instance for the request.
     */  
    public static void preSelect(Object container) {
        getContainerHelper().preSelect(container);
    }
 
    /**
     * Called in CMP environment to get NumericConverter policy referenced
     * by this Container instance.
     * @see getContainer(Object)
     * @param container a Container instance for the request
     * @return a valid NumericConverter policy type
     */
    public static int getNumericConverterPolicy(Object container) {
        return getContainerHelper().getNumericConverterPolicy(container);
    }

    /** Called in a unspecified transaction context of a managed environment.
     * According to p.364 of EJB 2.0 spec, CMP may need to manage
     * its own transaction when its transaction attribute is
     * NotSupported, Never, Supports.
     *
     * @param pm PersistenceManager
     */
    public static void beginInternalTransaction(PersistenceManager pm) {
        getContainerHelper().beginInternalTransaction(pm);
    }

    /** Called in a unspecified transaction context of a managed environment.
     * According to p.364 of EJB 2.0 spec, CMP may need to manage
     * its own transaction when its transaction attribute is
     * NotSupported, Never, Supports.
     *
     * @param pm PersistenceManager
     */
    public static void commitInternalTransaction(PersistenceManager pm) {
        getContainerHelper().commitInternalTransaction(pm);
    }

    /** Called in a unspecified transaction context of a managed environment.
     * According to p.364 of EJB 2.0 spec, CMP may need to manage
     * its own transaction when its transaction attribute is
     * NotSupported, Never, Supports.
     *
     * @param pm PersistenceManager
     */
    public static void rollbackInternalTransaction(PersistenceManager pm) {
        getContainerHelper().rollbackInternalTransaction(pm);
    }

    /** Called from read-only beans to suspend current transaction.
     * This will guarantee that PersistenceManager is not bound to 
     * any transaction.
     *
     * @return javax.transaction.Transaction object representing 
     * the suspended transaction. 
     * Returns null if the calling thread is not associated
     * with a transaction.
     */
    public static javax.transaction.Transaction suspendCurrentTransaction() {
        return getContainerHelper().suspendCurrentTransaction();
    }

    /** Called from read-only beans to resume current transaction.
     * This will guarantee that the transaction continues to run after
     * read-only bean accessed its PersistenceManager.
     *
     * @param tx - The javax.transaction.Transaction object that 
     * represents the transaction to be resumed.
     */
    public static void resumeCurrentTransaction(
            javax.transaction.Transaction tx) {

        getContainerHelper().resumeCurrentTransaction(tx);
    }

    /** Flush transactional changes to the database.
     * @param pm PersistenceManager
     */
    public static void flush(PersistenceManager pm) {
        Transaction tx = pm.currentTransaction();
        // flush updates to the database if transaction is active.
        if (tx != null && tx.isActive()) {
            PersistenceManagerWrapper pmw = (PersistenceManagerWrapper)pm;
            PersistenceManagerImpl pmi = 
                    (PersistenceManagerImpl)pmw.getPersistenceManager();
            pmi.internalFlush();
        }
    }

    /**
     * @return true if the container had been registered correctly.
     */
    public static boolean isContainerReady() {
        return (containerHelper != null);
    }

    /** Returns a ContainerHelper instance that can be used to invoke
     * the corresponding method.
     * @return a ContainerHelper instance registered with this class.
     * @throws JDOFatalInternalException if the instance is null.
     */  
    private static ContainerHelper getContainerHelper() {
        if (containerHelper == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                    messages, "ejb.cmphelper.nonmanaged")); //NOI18N
        }

        return containerHelper;
    }
}
