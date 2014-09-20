/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.transaction.xa;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.*;
import javax.transaction.*;

import org.glassfish.api.invocation.InvocationManager;
import com.sun.enterprise.transaction.spi.JavaEETransactionManagerDelegate;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.UserTransactionImpl;
import com.sun.enterprise.transaction.JavaEETransactionManagerSimplified;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {

    TransactionManager t;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() throws Exception {
        return new TestSuite(AppTest.class);
    }

    public void setUp() {
        try {
            t = new JavaEETransactionManagerSimplified();
            ((JavaEETransactionManager)t).setDelegate(new JavaEETransactionManagerXADelegate());
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

    }

    public void testBegin() {
        System.out.println("**Testing TM begin ===>");
        try {
            System.out.println("**Status before begin: " 
                    + JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus()));

            t.begin();
            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**Status after begin: "  + status + " <===");
            assertEquals (status, "Active");
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testCommit() {
        System.out.println("**Testing TM commit ===>");
        try {
            System.out.println("**Starting transaction ....");
            t.begin();
            assertEquals (JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus()), 
                "Active");

            System.out.println("**Calling TM commit ===>");
            t.commit();
            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**Status after commit: " + status + " <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testRollback() {
        System.out.println("**Testing TM rollback ===>");
        try {
            System.out.println("**Starting transaction ....");
            t.begin();
            assertEquals (JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus()), 
                "Active");

            System.out.println("**Calling TM rollback ===>");
            t.rollback();
            System.out.println("**Status after rollback: " 
                    + JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus()) 
                    + " <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testTxCommit() {
        System.out.println("**Testing TX commit ===>");
        try {
            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync();
            tx.registerSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            System.out.println("**Calling TX commit ===>");
            tx.commit();
            System.out.println("**Status after commit: "
                    + JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus())
                    + " <===");
            assertTrue ("beforeCompletion was not called", s.called_beforeCompletion);
            assertTrue ("afterCompletion was not called", s.called_afterCompletion);
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testTxSuspendResume() {
        System.out.println("**Testing TM suspend ===>");
        try {
            System.out.println("**Starting transaction ....");
            t.begin();

            Transaction tx = t.suspend();
            assertNotNull(tx);

            System.out.println("**TX suspended ....");
            System.out.println("**Calling TM resume ===>");
            t.resume(tx);

            assertEquals (JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus()), 
                "Active");

            System.out.println("**Calling TX commit ===>");
            tx.commit();
            String status = JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus());
            System.out.println("**Status after commit: " + status + " <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testTxRollback() {
        System.out.println("**Testing TX rollback ===>");
        try {
            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync();
            tx.registerSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            System.out.println("**Calling TX rollback ===>");
            tx.rollback();
            System.out.println("**Status after rollback: "
                    + JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus())
                    + " <===");
            assertFalse ("beforeCompletion was called", s.called_beforeCompletion);
            assertTrue ("afterCompletion was not called", s.called_afterCompletion);
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testUTxCommit() {
        System.out.println("**Testing UTX commit ===>");
        try {
            UserTransaction utx = createUtx();
            System.out.println("**Starting transaction ....");
            utx.begin();
            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**UTX Status after begin: " + status);

            assertEquals (status, "Active");

            System.out.println("**Calling UTX commit ===>");
            utx.commit();
            System.out.println("**Status after commit: "
                    + JavaEETransactionManagerSimplified.getStatusAsString(utx.getStatus())
                    + " <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testUTxRollback() {
        System.out.println("**Testing UTX rollback ===>");
        try {
            UserTransaction utx = createUtx();
            System.out.println("**Starting transaction ....");
            utx.begin();

            assertEquals (JavaEETransactionManagerSimplified.getStatusAsString(utx.getStatus()), 
                "Active");

            System.out.println("**Calling UTX rollback ===>");
            utx.rollback();
            System.out.println("**Status after rollback: "
                    + JavaEETransactionManagerSimplified.getStatusAsString(utx.getStatus())
                    + " <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    private UserTransaction createUtx() throws javax.naming.NamingException {
        UserTransaction utx = new UserTransactionImpl();
        InvocationManager im = new org.glassfish.api.invocation.InvocationManagerImpl();
        ((UserTransactionImpl)utx).setForTesting(t, im);
        return utx;
    }

    static class TestSync implements Synchronization {

        // Used to validate the calls
        protected boolean called_beforeCompletion = false;
        protected boolean called_afterCompletion = false;

        public void beforeCompletion() {
            System.out.println("**Called beforeCompletion  **");
            called_beforeCompletion = true;
        }

        public void afterCompletion(int status) {
            System.out.println("**Called afterCompletion with status:  "
                    + JavaEETransactionManagerSimplified.getStatusAsString(status));
            called_afterCompletion = true;
        }
    }
}
