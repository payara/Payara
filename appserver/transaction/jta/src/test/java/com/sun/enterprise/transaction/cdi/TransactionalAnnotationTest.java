/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.transaction.cdi;

import junit.framework.TestCase;

import javax.interceptor.InvocationContext;
import javax.transaction.InvalidTransactionException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionRequiredException;

/**
 * User: paulparkinson
 * Date: 12/10/12
 * Time: 3:50 PM
 */
public class TransactionalAnnotationTest extends TestCase {
    static TransactionManager transactionManager = new TestTransactionManager();

    @Override
    protected void setUp() throws Exception {
        transactionManager = new TestTransactionManager();
        new TransactionalInterceptorBase().setTransactionManager(transactionManager);
        super.setUp();
    }

    public static void main(String args[]) throws Exception {
        new TransactionalInterceptorBase().setTransactionManager(transactionManager);
        TransactionalAnnotationTest transactionalAnnotationTest =
                new TransactionalAnnotationTest();
        transactionalAnnotationTest.testTransactionalInterceptorMANDATORY();
        transactionalAnnotationTest.testTransactionalInterceptorNEVER();
        transactionalAnnotationTest.testTransactionalInterceptorNOT_SUPPORTED();
        transactionalAnnotationTest.testTransactionalInterceptorREQUIRED();
        transactionalAnnotationTest.testTransactionalInterceptorREQUIRES_NEW();
        transactionalAnnotationTest.testTransactionalInterceptorSUPPORTS();
    }

    public void testTransactionalInterceptorMANDATORY() throws Exception {
        TransactionalInterceptorMandatory transactionalInterceptorMANDATORY =
                new TransactionalInterceptorMandatory();
        InvocationContext ctx =
                new TestInvocationContext(
                        TestBeanMandatory.class.getMethod("foo", String.class), null
                );
        try {
            transactionalInterceptorMANDATORY.transactional(ctx);
            fail("should have thrown TransactionRequiredException due to " +
                    "transactionalInterceptorMANDATORY and no tx in place");
        } catch (TransactionRequiredException TransactionRequiredException) {
        }
        transactionManager.begin();
        transactionalInterceptorMANDATORY.transactional(ctx);
        transactionManager.commit();
        try {
            transactionalInterceptorMANDATORY.transactional(ctx);
            fail("should have thrown TransactionRequiredException due to " +
                    "transactionalInterceptorMANDATORY and no tx in place");
        } catch (TransactionRequiredException transactionRequiredException) {
        //todo assert TransactionalException and nested TransactionRequiredException
        }
    }

    public void testTransactionalInterceptorNEVER() throws Exception {
        TransactionalInterceptorNever transactionalInterceptorNEVER =
                new TransactionalInterceptorNever();
        InvocationContext ctx =
                new TestInvocationContext(
                        TestBeanNever.class.getMethod("foo", String.class), null
                );
        transactionalInterceptorNEVER.transactional(ctx);
        transactionManager.begin();
        try {
            transactionalInterceptorNEVER.transactional(ctx);
            fail("should have thrown InvalidTransactionException due to " +
                    "TransactionalInterceptorNEVER and  tx in place");
        } catch (InvalidTransactionException invalidTransactionException) {
        //todo assert TransactionalException and nested InvalidTransactionException
        } finally {
            transactionManager.rollback();
        }
    }

    public void testTransactionalInterceptorNOT_SUPPORTED() throws Exception {
        TransactionalInterceptorNotSupported transactionalInterceptorNOT_SUPPORTED =
                new TransactionalInterceptorNotSupported();
        InvocationContext ctx =
                new TestInvocationContext(
                        TestBeanNotSupported.class.getMethod("foo", String.class), null
                );
        transactionalInterceptorNOT_SUPPORTED.transactional(ctx);
    }

    public void testTransactionalInterceptorREQUIRED() throws Exception {
        TransactionalInterceptorRequired transactionalInterceptorREQUIRED =
                new TransactionalInterceptorRequired();
        InvocationContext ctx =
                new TestInvocationContext(
                        TestBeanRequired.class.getMethod("foo", String.class), null
                );
        transactionalInterceptorREQUIRED.transactional(ctx);
        transactionManager.begin();
        transactionalInterceptorREQUIRED.transactional(ctx);
        transactionManager.commit();
        //todo equality check
    }

    public void testTransactionalInterceptorREQUIRES_NEW() throws Exception {
        TransactionalInterceptorRequiresNew transactionalInterceptorREQUIRES_NEW =
                new TransactionalInterceptorRequiresNew();
        InvocationContext ctx =
                new TestInvocationContext(
                        TestBeanRequiresNew.class.getMethod("foo", String.class), null
                );
        transactionalInterceptorREQUIRES_NEW.transactional(ctx);
        transactionManager.begin();
        transactionalInterceptorREQUIRES_NEW.transactional(ctx);
        transactionManager.commit();
        //todo equality check
    }

    public void testTransactionalInterceptorSUPPORTS() throws Exception {
        TransactionalInterceptorSupports transactionalInterceptorSUPPORTS =
                new TransactionalInterceptorSupports();
        InvocationContext ctx =
                new TestInvocationContext(
                        TestBeanSupports.class.getMethod("foo", String.class), null
                );
        transactionalInterceptorSUPPORTS.transactional(ctx);
        transactionManager.begin();
        transactionalInterceptorSUPPORTS.transactional(ctx);
        transactionManager.commit();
    }

}
