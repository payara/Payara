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
 * EJBHelper.java
 *
 * Created on December 15, 2000, 10:15 AM
 */
package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import java.util.ResourceBundle;

import javax.transaction.*;
import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.api.persistence.support.PersistenceManagerFactory;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperPersistenceManager;

  /** Provides helper methods for a Forte for Java implementation with the
   * application server specific information in the distributed transaction
   * environment. Calls corresponding methods on the registered class which
   * implements TransactionHelper interface.
   */
public class EJBHelper {

    /** I18N message handler */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
        "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
        EJBHelper.class.getClassLoader());

    /** The logger */
    private static Logger logger = LogHelperPersistenceManager.getLogger();

   /** Reference to a class that implements TransactionHelper interface for this
    * particular application server, or DefaultTransactionHelper for a non-managed
    * environment.
    */
    static TransactionHelper myHelper = DefaultTransactionHelper.getInstance();

   /** Register class that implements TransactionHelper interface
    * Should be called by a static method at class initialization time.
    * If null is passed, sets the reference to the DefaultTransactionHelper.
    *
    * @param 	h 	application server specific implemetation of the TransactionHelper
    * 			interface.
    */
    public static void registerTransactionHelper (TransactionHelper h) {
        myHelper = h;
        if (myHelper == null) {
            myHelper = DefaultTransactionHelper.getInstance();
        }
    }

   /** Returns Transaction instance that can be used to register synchronizations.
    * In a non-managed environment or if there is no transaction associated with
    * the current thread, this method returns null.
    *
    * @see TransactionHelper#getTransaction()
    * @return the Transaction instance for the calling thread
    */
    public static Transaction getTransaction() {
        return myHelper.getTransaction();
    }

   /** Returns the UserTransaction associated with the calling thread.  In a
    * non-managed environment or if there is no transaction currently in progress,
    * this method returns null.
    *
    * @see TransactionHelper#getUserTransaction()
    * @return the UserTransaction instance for the calling thread
    */
    public static UserTransaction getUserTransaction() {
        return myHelper.getUserTransaction();
    }

    /** Identifies the managed environment behavior.
     * @return true if this implementation represents the managed environment.
     */
    public static boolean isManaged() {
        return myHelper.isManaged();
    }

   /** Translates local representation of the Transaction Status to
    * javax.transaction.Status value. In a non-managed environment
    * returns the value passed to it as an argument.
    *
    * @see TransactionHelper#translateStatus(int st)
    * @param 	st 	Status value
    * @return 	the javax.transaction.Status value of the status
    */
    public static int translateStatus(int st) {
        return myHelper.translateStatus(st);
    }

   /** Returns the hashed instance of PersistenceManagerFactory
    * that compares equal to the newly created instance or the instance
    * itself if it is not found. In a non-managed environment returns the value
    * passed to it as an argument.
    *
    * @see TransactionHelper#replaceInternalPersistenceManagerFactory(
    * 	PersistenceManagerFactory pmf)
    * @param 	pmf 	PersistenceManagerFactory instance to be replaced
    * @return 	the PersistenceManagerFactory known to the runtime
    */
    public static PersistenceManagerFactory replaceInternalPersistenceManagerFactory(
                PersistenceManagerFactory pmf) {
        return myHelper.replaceInternalPersistenceManagerFactory(pmf);
    }

   /** Called at the beginning of the Transaction.beforeCompletion() to register
    * the component with the app server if necessary. In a non-managed environment
    * or if the postInvoke method does not use the value, this method returns null.
    *
    * @see TransactionHelper#preInvoke(Object component)
    * @param 	component 	an array of Objects
    * @return implementation-specific Object
    */
    public static Object preInvoke(Object component) {
        return myHelper.preInvoke(component);
    }

   /** Called in a managed environment at the end of the Transaction.beforeCompletion()
    * to de-register the component with the app server if necessary.
    *
    * @see TransactionHelper#postInvoke(Object im)
    * @param im implementation-specific Object
    */
    public static void postInvoke(Object im) {
        myHelper.postInvoke(im);
    }

   /** Called in a managed environment to register internal Synchronization object
    * with the Transaction Synchronization. If available, this registration
    * provides special handling of the registered instance, calling it after
    * all user defined Synchronization instances.
    *
    * @see Transaction#registerSynchronization(Synchronization sync)
    * @see TransactionHelper#registerSynchronization(Transaction jta, 
    * Synchronization sync)
    * @param jta the Transaction instance for the calling thread.
    * @param sync the internal Synchronization instance to register.
    * @throws javax.transaction.RollbackException.
    * @throws javax.transaction.SystemException.
    */
    public static void registerSynchronization(Transaction jta, 
        Synchronization sync) throws RollbackException, SystemException {
        myHelper.registerSynchronization(jta, sync);
    }

    /** Called in a managed environment to get a Connection from the application
     * server specific resource. In a non-managed environment throws an Exception 
     * as it should not be called.
     *
     * @param resource the application server specific resource.
     * @param username the resource username. If null, Connection is requested
     * without username and password validation.
     * @param password the password for the resource username.
     * @return a Connection.
     * @throws java.sql.SQLException.
     */
    public static java.sql.Connection getConnection(Object resource, 
        String username, String password) throws java.sql.SQLException {
        return myHelper.getConnection(resource, username, password);
    }

    /** Called in a managed environment to get a non-transactional Connection
     * from the application server specific resource. In a non-managed 
     * environment throws an Exception as it should not be called.
     *
     * @param resource the application server specific resource.
     * @param username the resource username. If null, Connection is requested
     * without username and password validation.
     * @param password the password for the resource username.
     * @return a Connection.
     * @throws java.sql.SQLException.
     */
    public static java.sql.Connection getNonTransactionalConnection(
        Object resource, String username, String password)
        throws java.sql.SQLException {

        return myHelper.getNonTransactionalConnection(resource, 
            username, password);
    }


    /** Called in a managed environment to access a TransactionManager
     * for managing local transaction boundaries and synchronization
     * for local transaction completion.
     * 
     * @return javax.transaction.TransactionManager
     */
    public static TransactionManager getLocalTransactionManager() {
        return myHelper.getLocalTransactionManager();
    }

    /**
     * This method unwraps given Statement and return the Statement from
     * JDBC driver if possible.
     */
    public static java.sql.Statement unwrapStatement(java.sql.Statement stmt) {
        return myHelper.unwrapStatement(stmt);
    }
    
    /**
     * Set environment specific default values for the given PersistenceManagerFactory.
     *   
     * @param pmf the PersistenceManagerFactory.
     */  
    public static void setPersistenceManagerFactoryDefaults(PersistenceManagerFactory pmf) {
        myHelper.setPersistenceManagerFactoryDefaults(pmf);
    }

    /**
     * Returns name prefix for DDL files extracted from the info instance by the
     * application server specific code.
     *   
     * @param info the instance to use for the name generation.
     * @return name prefix as String.
     */  
    public static String getDDLNamePrefix(Object info) {
        return myHelper.getDDLNamePrefix(info);
    }
      
    /**
     * Called to register a ApplicationLifeCycleEventListener. If
     * ApplicationLifeCycle management is active (typically in managed
     * environment), the registered listener will receive a call back
     * for lifecycle events.
     *
     * @param listener An instance of ApplicationLifeCycleEventListener. 
     */  
    public static void registerApplicationLifeCycleEventListener(
            ApplicationLifeCycleEventListener listener) {
        myHelper.registerApplicationLifeCycleEventListener(listener);        
    }
    /**
     * Called to notify a ApplicationLifeCycleEventListeners that an application
     * is unloaded. If ApplicationLifeCycle management is active (typically in managed
     * environment), the registered listener will handle the notification.
     *
     * @param cl An instance of the ClassLoader that loaded the application.
     */  
    public static void notifyApplicationUnloaded(ClassLoader cl) {
        myHelper.notifyApplicationUnloaded(cl);        
    }

      /**
     * This is the default implementation of the TransactionHelper interface
     * for a non-mananged environment execution.
     * In the managed environment the application server specific implementation
     * registers itself with the EJBHelper to override this behavior.
     */
    private static class DefaultTransactionHelper implements TransactionHelper {
        
       private static final DefaultTransactionHelper instance = new DefaultTransactionHelper();

       /**
        * Returns instance of this class.
        */
       public static DefaultTransactionHelper getInstance() {return instance;}

       /** 
        * In a non-managed environment there is no transaction associated with
        * the current thread, this method returns null.
        *
        * @see TransactionHelper#getTransaction()
        * @return null;
        */
        public Transaction getTransaction() { return null; }
    
       /** 
        * In a non-managed environment there is no transaction currently in progress,
        * this method returns null.
        *
        * @see TransactionHelper#getUserTransaction()
        * @return the null.
        */
        public UserTransaction getUserTransaction() { return null; }
    
        /** Identifies the non-managed environment behavior.
         * @return false.
         */
        public boolean isManaged() { return false; }
    
       /** 
        * In a non-managed environment returns the value passed to it as an argument.
        *
        * @see TransactionHelper#translateStatus(int st)
        * @param 	local 	Status value
        * @return 	the status value
        */
        public int translateStatus(int st) { return st; }
    
       /** 
        * In a non-managed environment returns the value passed to it as an argument.
        *
        * @see TransactionHelper#replaceInternalPersistenceManagerFactory(
        * 	PersistenceManagerFactory pmf)
        * @param 	pmf 	PersistenceManagerFactory instance to be replaced
        * @return 	the pmf value.
        */
        public PersistenceManagerFactory replaceInternalPersistenceManagerFactory(
                    PersistenceManagerFactory pmf) { return pmf; }
    
       /** Called at the beginning of the Transaction.beforeCompletion() to register
        * the component with the app server if necessary. 
        * In a non-managed environment throws JDOFatalInternalException.
        *
        * @see TransactionHelper#preInvoke(Object component)
        * @param 	component 	an array of Objects
        * @throw JDOFatalInternalException if called.
        */
        public Object preInvoke(Object component) { 
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages, "ejb.ejbhelper.nonmanaged", "preInvoke")); //NOI18N
        }
    
       /** Called in a managed environment at the end of the Transaction.beforeCompletion()
        * to de-register the component with the app server if necessary.
        * In a non-managed environment throws JDOFatalInternalException.
        *
        * @see TransactionHelper#postInvoke(Object im)
        * @param im implementation-specific Object
        * @throw JDOFatalInternalException if called.
        */
        public void postInvoke(Object im) { 
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages, "ejb.ejbhelper.nonmanaged", "postInvoke")); //NOI18N
        }
    
       /** Called in a managed environment to register internal Synchronization object
        * with the Transaction Synchronization. 
        * In a non-managed environment it is a no-op.
        *
        * @see Transaction#registerSynchronization(Synchronization sync)
        * @see TransactionHelper#registerSynchronization(Transaction jta, 
        * Synchronization sync)
        * @param jta the Transaction instance for the calling thread.
        * @param sync the internal Synchronization instance to register.
        * @throws javax.transaction.RollbackException.
        * @throws javax.transaction.SystemException.
        */
        public void registerSynchronization(Transaction jta, 
            Synchronization sync) throws RollbackException, SystemException { }
    
        /** Called in a managed environment to get a Connection from the application
         * server specific resource. 
         * In a non-managed environment throws JDOFatalInternalException.
         *
         * @param resource the application server specific resource.
         * @param username the resource username. If null, Connection is requested
         * without username and password validation.
         * @param password the password for the resource username.
         * @throw JDOFatalInternalException if called.
         * @throw java.sql.SQLException.
         */
        public java.sql.Connection getConnection(Object resource, 
            String username, String password) throws java.sql.SQLException {
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages, "ejb.ejbhelper.nonmanaged", "getConnection")); //NOI18N
        }
    
        /** Called in a managed environment to get a non-transactional Connection
         * from the application server specific resource.
         * In a non-managed environment throws JDOFatalInternalException.
         *
         * @param resource the application server specific resource.
         * @param username the resource username. If null, Connection is requested
         * without username and password validation.
         * @param password the password for the resource username.
         * @throw JDOFatalInternalException if called.
         * @throw java.sql.SQLException.
         */
        public java.sql.Connection getNonTransactionalConnection(
            Object resource, String username, String password)
            throws java.sql.SQLException {

            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages, "ejb.ejbhelper.nonmanaged", //NOI18N
                "getNonTransactionalConnection")); //NOI18N
        }
    
        /** Called in a managed environment to access a TransactionManager
         * for managing local transaction boundaries and synchronization
         * for local transaction completion.
         * 
         * @return javax.transaction.TransactionManager
         */
        public TransactionManager getLocalTransactionManager() {
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages, "ejb.ejbhelper.nonmanaged", "getLocalTransactionManager")); //NOI18N
        }
    
        /**
         * This method unwraps given Statement and return the Statement from
         * JDBC driver if possible.
         */
        public java.sql.Statement unwrapStatement(java.sql.Statement stmt) {
            //Nothing to unwrap in unmanaged environment
            return stmt;
        }

        /** 
         * Set environment specific default values for the given PersistenceManagerFactory. 
         * In a non-managed this is a no-op.
         *   
         * @param pmf the PersistenceManagerFactory. 
         */   
        public void setPersistenceManagerFactoryDefaults(PersistenceManagerFactory pmf) {}

        /** 
         * Returns name prefix for DDL files extracted from the info instance by the 
         * application server specific code. 
         * In a non-managed environment throws JDOFatalInternalException.
         *   
         * @param info the instance to use for the name generation. 
         * @return name prefix as String. 
         */   
        public String getDDLNamePrefix(Object info) { 
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages, "ejb.ejbhelper.nonmanaged", "getDDLNamePrefix")); //NOI18N
        }
          
        /**
         * @inheritDoc
         */ 
        public void registerApplicationLifeCycleEventListener(
                ApplicationLifeCycleEventListener listener) {
            // The default implementation is no-op 
        }
          
        /**
         * @inheritDoc
         */ 
        public void notifyApplicationUnloaded(ClassLoader cl) {
            // The default implementation is no-op 
        }
   }
}
