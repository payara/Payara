/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.cdi.transaction;


import com.sun.enterprise.transaction.spi.TransactionOperationsManager;
import com.sun.logging.LogDomains;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Base class for all interceptors providing common logic for exception handling, etc.
 *
 * @author Paul Parkinson
 */
public class TransactionalInterceptorBase implements Serializable {
    private static TransactionManager testTransactionManager;
    volatile private static TransactionManager transactionManager;
    transient private TransactionOperationsManager preexistingTransactionOperationsManager;
    private static final TransactionOperationsManager
            transactionalTransactionOperationsManagerTransactionMethodsAllowed =
            new TransactionalTransactionOperationsManagerTransactionMethodsAllowed();
    private static final TransactionOperationsManager
            transactionalTransactionOperationsManagerTransactionMethodsNotAllowed =
            new TransactionalTransactionOperationsManagerTransactionMethodsNotAllowed();

    private static Logger _logger = LogDomains.getLogger(
            TransactionalInterceptorBase.class, LogDomains.JTA_LOGGER);

    /**
     * Must not return null
     * @return TransactionManager
     */
    public TransactionManager getTransactionManager() {
        if (testTransactionManager != null) return testTransactionManager;
        if (transactionManager == null) {
            try {
                synchronized (TransactionalInterceptorBase.class) {
                    if (transactionManager == null)
                        transactionManager = (TransactionManager)
                                new InitialContext().lookup("java:appserver/TransactionManager");
                }
            } catch (NamingException e) {
                _logger.severe(
                        "Encountered NamingException while attempting to acquire transaction manager for " +
                                "Transactional annotation interceptors " + e);
                throw new RuntimeException("Unable to obtain TransactionManager for Transactional Interceptor", e);
            }
        }
        return transactionManager;
    }

    static void setTestTransactionManager(TransactionManager transactionManager) {
        testTransactionManager = transactionManager;
    }

    boolean isLifeCycleMethod(InvocationContext ctx) {
        return (ctx.getMethod().getAnnotation(PostConstruct.class) != null) ||
                (ctx.getMethod().getAnnotation(PreDestroy.class) != null);
    }

    public Object proceed(InvocationContext ctx) throws Exception {
        javax.transaction.Transactional transactionalAnnotation =
                ctx.getMethod().getAnnotation(javax.transaction.Transactional.class);
        Class[] rollbackOn = null;
        Class[] dontRollbackOn = null;
        if(transactionalAnnotation != null) { //if at method level
            rollbackOn = transactionalAnnotation.rollbackOn();
            dontRollbackOn = transactionalAnnotation.dontRollbackOn();
        } else {  //if not, at class level
            Class<?> targetClass = ctx.getTarget().getClass();
            transactionalAnnotation = targetClass.getAnnotation(javax.transaction.Transactional.class);
            if (transactionalAnnotation != null) {
                rollbackOn = transactionalAnnotation.rollbackOn();
                dontRollbackOn = transactionalAnnotation.dontRollbackOn();
            }
        }
        Object object;
        try {
            object = ctx.proceed();
        } catch (RuntimeException runtimeException) {
            if(dontRollbackOn!=null) {
                for (Class aDontRollbackOn : dontRollbackOn) {
                    if (aDontRollbackOn.equals(runtimeException.getClass())) {
                        throw runtimeException;
                    }
                }
                markRollbackIfActiveTransaction();
            } else {
                markRollbackIfActiveTransaction();
            }
            throw runtimeException;
        } catch (Exception checkException) {
            if(rollbackOn!=null) {
                for (Class aRollbackOn : rollbackOn) {
                    if (aRollbackOn.equals(checkException.getClass())) markRollbackIfActiveTransaction();
                }
            }
            throw checkException;
        }
        return object;
    }

    private void markRollbackIfActiveTransaction() throws SystemException {
        Transaction transaction = getTransactionManager().getTransaction();
        if(transaction!=null) {
            _logger.info("About to setRollbackOnly from @Transactional interceptor on transaction:" + transaction);
            getTransactionManager().setRollbackOnly();
        }
    }

    void setTransactionalTransactionOperationsManger(boolean userTransactionMethodsAllowed) {
        if(testTransactionManager != null) return; //test
            ComponentInvocation currentInvocation = getCurrentInvocation();
            if (currentInvocation == null) {
                _logger.warning(
                        "No ComponentInvocation present for @Transactional annotation processing.  " +
                                "Restriction on use of UserTransaction will not be enforced");
                return;
            }
            preexistingTransactionOperationsManager =
                    (TransactionOperationsManager) currentInvocation.getTransactionOperationsManager();
            currentInvocation.setTransactionOperationsManager(userTransactionMethodsAllowed?
                    transactionalTransactionOperationsManagerTransactionMethodsAllowed:
                    transactionalTransactionOperationsManagerTransactionMethodsNotAllowed);
    }

    void resetTransactionOperationsManager() {
        if(testTransactionManager != null) return; //test
        ComponentInvocation currentInvocation = getCurrentInvocation();
        if (currentInvocation == null) {
            //there should always be a currentInvocation and so this would seem a bug
            // but not a fatal one as app should not be relying on this, so log warning only
            System.out.println("TransactionalInterceptorBase.markThreadAsTransactional currentInvocation==null");
            return;
        }
        currentInvocation.setTransactionOperationsManager(preexistingTransactionOperationsManager);
    }

    ComponentInvocation getCurrentInvocation() {
        ServiceLocator serviceLocator = Globals.getDefaultHabitat();
        InvocationManager invocationManager =
                serviceLocator == null ? null : serviceLocator.getService(InvocationManager.class);
        return invocationManager == null ? null : invocationManager.getCurrentInvocation();
    }

    private static final class TransactionalTransactionOperationsManagerTransactionMethodsAllowed
            implements TransactionOperationsManager {

        public boolean userTransactionMethodsAllowed() {
            return true;
        }

        public void userTransactionLookupAllowed() throws NameNotFoundException {
        }

        public void doAfterUtxBegin() {
        }
    }

    private static final class TransactionalTransactionOperationsManagerTransactionMethodsNotAllowed
            implements TransactionOperationsManager {

        public boolean userTransactionMethodsAllowed() {
            return false;
        }

        public void userTransactionLookupAllowed() throws NameNotFoundException {
        }

        public void doAfterUtxBegin() {
        }
    }
}
