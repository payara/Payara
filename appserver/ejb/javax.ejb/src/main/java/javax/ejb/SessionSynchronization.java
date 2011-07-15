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

package javax.ejb;

import java.rmi.RemoteException;

/**
 * <p> The SessionSynchronization interface allows a stateful session
 * bean instance to be notified by its container of transaction
 * boundaries.
 *
 * <p> Only a stateful session bean with container-managed transaction 
 * demarcation can receive session synchronization notifications. Other bean
 * types must not implement the SessionSynchronization interface or use 
 * the session synchronization annotations.
 *
 * <p>  A stateful session bean class is not required to implement this interface.
 *
 * @since EJB 1.0
 */
public interface SessionSynchronization {
    /**
     * The <code>afterBegin</code> method notifies a stateful session bean instance that a new
     * transaction has started, and that the subsequent business methods on the
     * instance will be invoked in the context of the transaction.
     *
     * <p> The instance can use this method, for example, to read data
     * from a database and cache the data in the instance fields.
     *
     * <p> This method executes in the proper transaction context.
     *
     * @exception EJBException Thrown by the method to indicate a failure
     *    caused by a system-level error.
     *
     * @exception RemoteException This exception is defined in the method
     *    signature to provide backward compatibility for enterprise beans 
     *    written for the EJB 1.0 specification. Enterprise beans written 
     *    for the EJB 1.1 and later specifications should throw the
     *    javax.ejb.EJBException instead of this exception. 
     *    Enterprise beans written for the EJB 2.0 and later specifications 
     *    must not throw the java.rmi.RemoteException.
     *
     * @see AfterBegin
     */
    public void afterBegin() throws EJBException, RemoteException;

    /**
     * The <code>beforeCompletion</code> method notifies a stateful session bean instance that
     * a transaction is about to be committed. The instance can use this
     * method, for example, to write any cached data to a database.
     *
     * <p> This method executes in the proper transaction context.
     *
     * <p><b>Note:</b> The instance may still cause the container to
     * rollback the transaction by invoking the <code>setRollbackOnly</code> method
     * on the session context, or by throwing an exception.
     *
     * @exception EJBException Thrown by the method to indicate a failure
     *    caused by a system-level error.
     *
     * @exception RemoteException This exception is defined in the method
     *    signature to provide backward compatibility for enterprise beans 
     *    written for the EJB 1.0 specification. Enterprise beans written 
     *    for the EJB 1.1 and later specification should throw the
     *    javax.ejb.EJBException instead of this exception.
     *    Enterprise beans written for the EJB 2.0 and later specifications 
     *    must not throw the java.rmi.RemoteException.
     *
     * @see BeforeCompletion
     */
    public void beforeCompletion() throws EJBException, RemoteException;

    /**
     * The <code>afterCompletion</code> method notifies a stateful session bean instance that a
     * transaction commit protocol has completed, and tells the instance
     * whether the transaction has been committed or rolled back.
     *
     * <p> This method executes with no transaction context.
     *
     * @param committed True if the transaction has been committed, false
     *    if is has been rolled back.
     *
     * @exception EJBException Thrown by the method to indicate a failure
     *    caused by a system-level error.
     *
     * @exception RemoteException This exception is defined in the method
     *    signature to provide backward compatibility for enterprise beans 
     *    written for the EJB 1.0 specification. Enterprise beans written 
     *    for the EJB 1.1 and later specification should throw the
     *    javax.ejb.EJBException instead of this exception. 
     *    Enterprise beans written for the EJB 2.0 and later specifications 
     *    must not throw the java.rmi.RemoteException.
     *
     * @see AfterCompletion
     */
    public void afterCompletion(boolean committed) throws EJBException,
	    RemoteException;
}
