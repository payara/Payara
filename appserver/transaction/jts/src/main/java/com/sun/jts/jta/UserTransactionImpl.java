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

package com.sun.jts.jta;

import javax.transaction.*;
import javax.naming.*;
import java.util.Properties;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
/**
 * This class implements the javax.transaction.UserTransaction interface
 * which defines methods that allow an application to explicitly manage
 * transaction boundaries.
 *
 * @author Ram Jeyaraman
 * @version 1.0 Feb 09, 1999
 */
public class UserTransactionImpl implements javax.transaction.UserTransaction,
	javax.naming.Referenceable, java.io.Serializable {

	// Instance variables

    private transient TransactionManager transactionManager;

	/*
		Logger to log transaction messages
	*/  
    static Logger _logger = LogDomains.getLogger(UserTransactionImpl.class, LogDomains.TRANSACTION_LOGGER);
    // Constructor

    public UserTransactionImpl() {}

    // Implementation of javax.transaction.UserTransaction interface

    /**
     * Create a new transaction and associate it with the current thread.
     *
     * @exception IllegalStateException Thrown if the thread is already
     *    associated with a transaction.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition
     *
     */
    public void begin() throws NotSupportedException, SystemException {
        if (transactionManager == null) init();
    	this.transactionManager.begin();
    }

    /**
     * Complete the transaction associated with the current thread. When this
     * method completes, the thread becomes associated with no transaction.
     *
     * @exception TransactionRolledbackException Thrown to indicate that
     *    the transaction has been rolled back rather than committed.
     *
     * @exception HeuristicMixedException Thrown to indicate that a heuristic
     *    decision was made and that some relevant updates have been committed
     *    while others have been rolled back.
     *
     * @exception HeuristicRollbackException Thrown to indicate that a
     *    heuristic decision was made and that all relevant updates have been
     *    rolled back.
     *
     * @exception SecurityException Thrown to indicate that the thread is
     *    not allowed to commit the transaction.
     *
     * @exception IllegalStateException Thrown if the current thread is
     *    not associated with a transaction.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition
    */
    public void commit() throws RollbackException,
	HeuristicMixedException, HeuristicRollbackException, SecurityException,
	IllegalStateException, SystemException {
        if (transactionManager == null) init();
    	this.transactionManager.commit();
    }

    /**
     * Roll back the transaction associated with the current thread. When this
     * method completes, the thread becomes associated with no transaction.
     *
     * @exception SecurityException Thrown to indicate that the thread is
     *    not allowed to roll back the transaction.
     *
     * @exception IllegalStateException Thrown if the current thread is
     *    not associated with a transaction.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition
     *
     */
    public void rollback() throws IllegalStateException, SecurityException,
        SystemException {
        if (transactionManager == null) init();
    	this.transactionManager.rollback();
    }

    /**
     * Modify the transaction associated with the current thread such that
     * the only possible outcome of the transaction is to roll back the
     * transaction.
     *
     * @exception IllegalStateException Thrown if the current thread is
     *    not associated with a transaction.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition
     *
     */
    public void setRollbackOnly() throws IllegalStateException,
    	SystemException {
        if (transactionManager == null) init();
    	this.transactionManager.setRollbackOnly();
    }

    /**
     * Obtain the status of the transaction associated with the current thread.
     *
     * @return The transaction status. If no transaction is associated with
     *    the current thread, this method returns the Status.NoTransaction
     *    value.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition
     *
     */
    public int getStatus() throws SystemException {
        if (transactionManager == null) init();
    	return this.transactionManager.getStatus();
    }

    /**
     * Modify the timeout value that is associated with transactions started
     * by subsequent invocations of the begin method.
     *
     * <p> If an application has not called this method, the transaction
     * service uses some default value for the transaction timeout.
     *
     * @param seconds The value of the timeout in seconds. If the value is zero,
     *        the transaction service restores the default value. If the value
     *        is negative a SystemException is thrown.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition.
     *
     */
    public void setTransactionTimeout(int seconds) throws SystemException {
        if (transactionManager == null) init();
    	this.transactionManager.setTransactionTimeout(seconds);
    }

    // Implementation of the javax.naming.Referenceable interface

    /**
     * This method is used by JNDI to store a referenceable object.
     */
    public Reference getReference() throws NamingException {
		//_logger.log(Level.FINE,"Referenceable object invoked");
    	return new Reference(this.getClass().getName(),
        	UserTransactionFactory.class.getName(), null);
    }

    // serializable interface related


    private void init() {
        this.transactionManager =
            TransactionManagerImpl.getTransactionManagerImpl();
    }
}

