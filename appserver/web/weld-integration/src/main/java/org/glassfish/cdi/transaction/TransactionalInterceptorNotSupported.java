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

// Portions Copyright [2014-2015] [C2B2 Consulting Limited]

package org.glassfish.cdi.transaction;

import org.glassfish.logging.annotation.LoggerInfo;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionalException;
import java.util.logging.Logger;

/**
 * Transactional annotation Interceptor class for NotSupported transaction type,
 * ie javax.transaction.Transactional.TxType.NOT_SUPPORTED
 * If called outside a transaction context, managed bean method execution will then
 * continue outside a transaction context.
 * If called inside a transaction context, the current transaction context will be suspended,
 * the managed bean method execution will then continue outside a transaction context,
 * and the previously suspended transaction will be resumed.
 *
 * @author Paul Parkinson
 */
@javax.annotation.Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
@Interceptor
@javax.transaction.Transactional(javax.transaction.Transactional.TxType.NOT_SUPPORTED)
public class TransactionalInterceptorNotSupported extends TransactionalInterceptorBase {

    private static final Logger _logger = Logger.getLogger(CDI_JTA_LOGGER_SUBSYSTEM_NAME, SHARED_LOGMESSAGE_RESOURCE);

    @AroundInvoke
    public Object transactional(InvocationContext ctx) throws Exception {
        _logger.log(java.util.logging.Level.FINE, CDI_JTA_NOTSUPPORTED);
        if (isLifeCycleMethod(ctx)) return proceed(ctx);
        setTransactionalTransactionOperationsManger(true);
        try {
            Transaction transaction = null;
            if (getTransactionManager().getTransaction() != null) {
                _logger.log(java.util.logging.Level.FINE, CDI_JTA_MBNOTSUPPORTED);
                try {
                    transaction = getTransactionManager().suspend();
                } catch (Exception exception) {
                    String messageString =
                            "Managed bean with Transactional annotation and TxType of NOT_SUPPORTED " +
                                    "called inside a transaction context.  Suspending transaction failed due to " +
                                    exception;
                    _logger.log(java.util.logging.Level.FINE, 
                        CDI_JTA_MBNOTSUPPORTEDTX, exception);
                    throw new TransactionalException(messageString, exception);
                }
            }
            Object proceed = null;
            try {
                proceed = proceed(ctx);
            } finally {
                if (transaction != null) {
                    try {
                        getTransactionManager().resume(transaction);
                    } catch (Exception exception) {
                        String messageString =
                                "Managed bean with Transactional annotation and TxType of NOT_SUPPORTED " +
                                        "encountered exception during resume " +
                                        exception;
                        throw new TransactionalException(messageString, exception);
                    }
                }
            }
            return proceed;

        } finally {
            resetTransactionOperationsManager();
        }
    }
}
