/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import javax.ejb.TimerService;
import javax.transaction.TransactionManager;
import javax.transaction.Status;
import javax.naming.InitialContext;
import java.util.logging.Level;

/**
 * Implementation of EJBContext for Singleton SessionBeans
 *
 * @author Mahesh Kannan
 */

public final class SingletonContextImpl
        extends AbstractSessionContextImpl {


    SingletonContextImpl(Object ejb, BaseContainer container) {
        super(ejb, container);
        try {
            initialContext = new InitialContext();
        } catch(Exception ex) {
            _logger.log(Level.FINE, "Exception in creating InitialContext",
                ex);
        }   

    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {

        // Instance key is first set after dependency injection but
        // before ejbCreate
        if ( instanceKey == null ) {
            throw new IllegalStateException("Operation not allowed");
        }

        return EJBTimerService.getEJBTimerServiceWrapper(this);
    }

    @Override
    public void setRollbackOnly()
        throws IllegalStateException
    {
        if (instanceKey == null) {
            throw new IllegalStateException("Singleton setRollbackOnly not allowed");
        }

        if ( container.isBeanManagedTran ) {
            throw new IllegalStateException(
                "Illegal operation for bean-managed transactions");
        }

        doGetSetRollbackTxAttrCheck();

        TransactionManager tm = EjbContainerUtilImpl.getInstance().getTransactionManager();

        try {
            if ( tm.getStatus() == Status.STATUS_NO_TRANSACTION ) {
                // EJB might be in a non-business method (for SessionBeans)
                // or afterCompletion.
                // OR this was a NotSupported/Never/Supports
                // EJB which was invoked without a global transaction.
                // In that case the JDBC connection would have autoCommit=true
                // so the container doesnt have to do anything.
                throw new IllegalStateException("No transaction context.");
            }

            tm.setRollbackOnly();

        } catch (Exception ex) {
            IllegalStateException illEx = new IllegalStateException(ex.toString());
            illEx.initCause(ex);
            throw illEx;
        }
    }

    @Override
    public boolean getRollbackOnly()
        throws IllegalStateException
    {
        if (instanceKey == null) {
            throw new IllegalStateException("Singleton getRollbackOnly not allowed");
        }


        if ( container.isBeanManagedTran ) {
            throw new IllegalStateException(
                "Illegal operation for bean-managed transactions");
        }

        doGetSetRollbackTxAttrCheck();
        
        TransactionManager tm = EjbContainerUtilImpl.getInstance().getTransactionManager();

        try {
            int status = tm.getStatus();
            if ( status == Status.STATUS_NO_TRANSACTION ) {
                // EJB which was invoked without a global transaction.
                throw new IllegalStateException("No transaction context.");
            }

            return ( status == Status.STATUS_MARKED_ROLLBACK ||
                     status == Status.STATUS_ROLLEDBACK      ||
                     status == Status.STATUS_ROLLING_BACK );

        } catch (Exception ex) {
            IllegalStateException illEx = new IllegalStateException(ex.toString());
            illEx.initCause(ex);
            throw illEx;
        }
    }

    @Override
    public void checkTimerServiceMethodAccess()
        throws IllegalStateException
    {
        if ( instanceKey == null ) {
            throw new IllegalStateException
            ("EJB Timer method calls cannot be called in this context");
        }
    }

    @Override
    public synchronized Object lookup(String name) {
        Object o = null;

        if( name == null ) {
            throw new IllegalArgumentException("Argument is null");
        }
        if( initialContext == null ) {
            throw new IllegalArgumentException("InitialContext is null");
        }
        try {
            // if name starts with java: use it as is.  Otherwise, treat it
            // as relative to the private component namespace.
            String lookupString = name.startsWith("java:") ?
                    name : "java:comp/env/" + name;

            o = initialContext.lookup(lookupString);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
        return o;
    }

}
