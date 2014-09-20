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
 * TransactionHelper.java
 *
 * Created on December 15, 2000, 10:06 AM
 */

package com.sun.jdo.spi.persistence.support.sqlstore.ejb;

import javax.transaction.*;

import com.sun.jdo.api.persistence.support.PersistenceManagerFactory;

    /** Provide a Forte for Java implementation with information about the distributed
     * transaction environment.  This is an interface that a helper class
     * implements that is specific to a managed environment.
     * <P><B>This interface is specific to Forte for Java, version 3.0,
     * and is subject to change without notice.  In particular, as additional
     * experience is gained with specific application servers, this interface
     * may have methods added and removed, even with patch releases.
     * Therefore, this interface should be considered very volatile, and
     * any class that implements it might have to be reimplemented whenever
     * an upgrade to either the application server or Forte for Java occurs.</B></P>
     * The class that implements this interface must register itself
     * by a static method at class initialization time.  For example,
     * <blockquote><pre>
     * import com.sun.jdo.spi.persistence.support.sqlstore.ejb.*;
     * class blackHerringTransactionHelper implements TransactionHelper {
     *    static EJBHelper.register(new blackHerringTransactionHelper());
     *    ...
     * }
     * </pre></blockquote>
     */
    public interface TransactionHelper {

    /** Returns the UserTransaction associated with the calling thread.  If there
     * is no transaction currently in progress, this method returns null.
     * @return the UserTransaction instance for the calling thread
     */
    UserTransaction getUserTransaction();

    /** Identify the Transaction context for the calling thread, and return a
     * Transaction instance that can be used to register synchronizations,
     * and used as the key for HashMaps. The returned Transaction must implement
     * <code>equals()</code> and <code>hashCode()</code> based on the global transaction id.
     * <P>All Transaction instances returned by this method called in the same
     * Transaction context must compare equal and return the same hashCode.
     * The Transaction instance returned will be held as the key to an
     * internal HashMap until the Transaction completes. If there is no transaction
     * associated with the current thread, this method returns null.
     * @return the Transaction instance for the calling thread
     */
    Transaction getTransaction();

    /** Translate local representation of the Transaction Status to
     * javax.transaction.Status value if necessary. Otherwise this method
     * should return the value passed to it as an argument.
     * <P>This method is used during afterCompletion callbacks to translate
     * the parameter value passed by the application server to the
     * afterCompletion method.  The return value must be one of:
     * <code>javax.transaction.Status.STATUS_COMMITTED</code> or
     * <code>javax.transaction.Status.STATUS_ROLLED_BACK</code>.
     * @param 	st 	local Status value
     * @return the javax.transaction.Status value of the status
     */
    int translateStatus(int st);

    /** Replace newly created instance of PersistenceManagerFactory
     * with the hashed one if it exists. The replacement is necessary only if
     * the JNDI lookup always returns a new instance. Otherwise this method
     * returns the object passed to it as an argument.
     *
     * PersistenceManagerFactory is uniquely identified by
     * ConnectionFactory.hashCode() if ConnectionFactory is
     * not null; otherwise by ConnectionFactoryName.hashCode() if
     * ConnectionFactoryName is not null; otherwise
     * by the combination of URL.hashCode() + userName.hashCode() +
     * password.hashCode() + driverName.hashCode();
     *
     * @param 	pmf 	PersistenceManagerFactory instance to be replaced
     * @return 	the PersistenceManagerFactory known to the runtime
     */
    PersistenceManagerFactory replaceInternalPersistenceManagerFactory(
	PersistenceManagerFactory pmf);

    /** Called at the beginning of the Transaction.beforeCompletion() to
     * register the component with the app server if necessary.
     * The component argument is an array of Objects. 
     * The first element is com.sun.jdo.spi.persistence.support.sqlstore.Transaction 
     * object responsible for transaction completion.
     * The second element is com.sun.jdo.api.persistence.support.PersistenceManager 
     * object that has been associated with the Transaction context for the 
     * calling thread. 
     * The third element is javax.transaction.Transaction object that has been 
     * associated with the given instance of PersistenceManager. 
     * The return value is passed unchanged to the postInvoke method.
     *
     * @param 	component 	an array of Objects
     * @return 	implementation-specific Object
     */
    Object preInvoke(Object component);

    /** Called at the end of the Transaction.beforeCompletion() to
     * de-register the component with the app server if necessary.
     * The parameter is the return value from preInvoke, and can be any
     * Object.
     *
     * @param 	im 	implementation-specific Object
     */
    void postInvoke(Object im);

   /** Called in a managed environment to register internal Synchronization object
    * with the Transaction Synchronization. If available, this registration
    * provides special handling of the registered instance, calling it after
    * all user defined Synchronization instances.
    *
    * @param jta the Transaction instance for the calling thread.
    * @param sync the internal Synchronization instance to register.
    * @throws javax.transaction.RollbackException.
    * @throws javax.transaction.SystemException
    */
    void registerSynchronization(Transaction jta, Synchronization sync)
        throws RollbackException, SystemException;

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
    java.sql.Connection getConnection(Object resource, String username, String password)
        throws java.sql.SQLException;

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
    java.sql.Connection getNonTransactionalConnection(
        Object resource, String username, String password)
        throws java.sql.SQLException;

    /** Called in a managed environment to access a TransactionManager
     * for managing local transaction boundaries and registering synchronization
     * for call backs during completion of a local transaction.
     * 
     * @return javax.transaction.TransactionManager
     */
    TransactionManager getLocalTransactionManager();

    /** Identifies the managed environment behavior.
     * @return true if this implementation represents the managed environment.
     */  
    boolean isManaged();

    /**
     * This method unwrap given Statement and return the Statement from
     * JDBC driver.
     */
    java.sql.Statement unwrapStatement(java.sql.Statement stmt);


    /**
     * Set environment specific default values for the given PersistenceManagerFactory.
     * 
     * @param pmf the PersistenceManagerFactory.
     */
    void setPersistenceManagerFactoryDefaults(PersistenceManagerFactory pmf);

    /** 
     * Returns name prefix for DDL files extracted from the info instance by the
     * application server specific code.
     *   
     * @param info the instance to use for the name generation.
     * @return name prefix as String. 
     */   
    String getDDLNamePrefix(Object info);
        
    /**
     * Called to register a ApplicationLifeCycleEventListener. If
     * ApplicationLifeCycle management is active (typically in managed
     * environment), the registered listener will receive a call back
     * for lifecycle events.
     *
     * @param listener An instance of ApplicationLifeCycleEventListener. 
     */ 
    void registerApplicationLifeCycleEventListener(
            ApplicationLifeCycleEventListener listener);
        
    /**
     * Called to notify a ApplicationLifeCycleEventListeners that an application
     * is unloaded. If ApplicationLifeCycle management is active (typically in managed
     * environment), the registered listener will handle the notification.
     *
     * @param cl An instance of the ClassLoader that loaded the application.
     */ 
    void notifyApplicationUnloaded(ClassLoader cl);

}

