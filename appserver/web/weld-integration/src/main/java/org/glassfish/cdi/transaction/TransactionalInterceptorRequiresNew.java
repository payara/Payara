/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]

package org.glassfish.cdi.transaction;

import static java.util.logging.Level.FINE;
import static jakarta.transaction.Status.STATUS_MARKED_ROLLBACK;

import java.util.logging.Logger;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionalException;

import com.sun.enterprise.transaction.TransactionManagerHelper;
import org.glassfish.api.invocation.ComponentInvocation;

/**
 * Transactional annotation Interceptor class for RequiresNew transaction type, ie
 * jakarta.transaction.Transactional.TxType.REQUIRES_NEW If called outside a transaction context, a
 * new JTA transaction will begin, the managed bean method execution will then continue inside this
 * transaction context, and the transaction will be committed. If called inside a transaction
 * context, the current transaction context will be suspended, a new JTA transaction will begin, the
 * managed bean method execution will then continue inside this transaction context, the transaction
 * will be committed, and the previously suspended transaction will be resumed.
 *
 * @author Paul Parkinson
 */
@jakarta.annotation.Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
@Interceptor
@jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.REQUIRES_NEW)
public class TransactionalInterceptorRequiresNew extends TransactionalInterceptorBase {

    private static final long serialVersionUID = 1L;
    private static final Logger _logger = Logger.getLogger(CDI_JTA_LOGGER_SUBSYSTEM_NAME, SHARED_LOGMESSAGE_RESOURCE);

    @AroundInvoke
    public Object transactional(InvocationContext ctx) throws Exception {
        _logger.log(FINE, CDI_JTA_REQNEW);

        if (isLifeCycleMethod(ctx)) {
            return proceed(ctx);
        }


        setTransactionalTransactionOperationsManger(false);

        boolean currentInvocationHasTransaction = false;
        try {
            Transaction suspendedTransaction = null;
            if (getTransactionManager().getTransaction() != null) {
                _logger.log(FINE, CDI_JTA_MBREQNEW);

                ComponentInvocation currentInvocation = getCurrentInvocation();
                currentInvocationHasTransaction = (currentInvocation != null) && currentInvocation.getTransaction() != null;

                suspendedTransaction = getTransactionManager().suspend();
                // todo catch, wrap in new transactional exception and throw

            }
            try {
                getTransactionManager().begin();
                TransactionManager tm = getTransactionManager();
                if (tm instanceof TransactionManagerHelper) {
                    ((TransactionManagerHelper) tm).preInvokeTx(true);
                }
            } catch (Exception exception) {
                _logger.log(FINE, CDI_JTA_MBREQNEWBT, exception);

                throw new TransactionalException(
                        "Managed bean with Transactional annotation and TxType of REQUIRES_NEW " +
                        "encountered exception during begin " + exception,
                        exception);
            }

            Object proceed = null;

            try {
                proceed = proceed(ctx);
            } finally {
                try {
                    TransactionManager tm = getTransactionManager();
                    if (tm instanceof TransactionManagerHelper) {
                        ((TransactionManagerHelper) tm).postInvokeTx(false, true);
                    }

                    // Exception handling for proceed method call above can set TM/TRX as setRollbackOnly
                    if (getTransactionManager().getTransaction().getStatus() == STATUS_MARKED_ROLLBACK) {
                        getTransactionManager().rollback();
                    } else {
                        getTransactionManager().commit();
                    }
                } catch (Exception exception) {
                    _logger.log(FINE, CDI_JTA_MBREQNEWCT, exception);

                    throw new TransactionalException(
                            "Managed bean with Transactional annotation and TxType of REQUIRES_NEW " +
                            "encountered exception during commit " + exception,
                            exception);
                }

                if (suspendedTransaction != null) {
                    try {
                        getTransactionManager().resume(suspendedTransaction);

                        ComponentInvocation currentInvocation = getCurrentInvocation();
                        if (!currentInvocationHasTransaction && currentInvocation != null) {
                            getCurrentInvocation().setTransaction(null);
                        }
                    } catch (Exception exception) {
                        throw new TransactionalException(
                                "Managed bean with Transactional annotation and TxType of REQUIRED " +
                                "encountered exception during resume " + exception,
                                exception);
                    }
                }
            }

            return proceed;

        } finally {
            resetTransactionOperationsManager();
        }
    }
}
