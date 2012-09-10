/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.transaction;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.*;
import javax.transaction.*;
import javax.transaction.xa.*;

import java.beans.PropertyChangeEvent;
import com.sun.enterprise.config.serverbeans.ServerTags;

import org.glassfish.api.invocation.InvocationManager;
import com.sun.enterprise.transaction.spi.JavaEETransactionManagerDelegate;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {

    JavaEETransactionManager t; 

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
            JavaEETransactionManagerDelegate d = new JavaEETransactionManagerSimplifiedDelegate();
            t.setDelegate(d);
            d.setTransactionManager(t);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

    }

    /**
     * Can't test more than null (but no NPE)
     */
    public void testXAResourceWrapper() {
        assertNull(t.getXAResourceWrapper("xxx"));
        assertNull(t.getXAResourceWrapper("oracle.jdbc.xa.client.OracleXADataSource"));
    }

    /**
     * Test ConfigListener call
     */
    public void testTransactionServiceConfigListener() {
        PropertyChangeEvent e1 = new PropertyChangeEvent("", ServerTags.KEYPOINT_INTERVAL, "1", "10");
        PropertyChangeEvent e2 = new PropertyChangeEvent("", ServerTags.RETRY_TIMEOUT_IN_SECONDS, "1", "10");
        try {
            TransactionServiceConfigListener l = new TransactionServiceConfigListener();
            l.setTM(t);
            l.changed(new PropertyChangeEvent[] {e1, e2});
            assert(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testWrongResume() {
        try {
            System.out.println("**Null resume ....");
            t.resume(null);
            System.out.println("**WRONG: TM resume null successful <===");
            assert (false);
        } catch (InvalidTransactionException ex) {
            System.out.println("**Caught InvalidTransactionException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        Transaction tx = null;
        try {
            System.out.println("**Null resume on active tx....");
            t.begin();
            tx = t.getTransaction();
            t.resume(null);
            System.out.println("**WRONG: TM resume null on active tx successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
        
        try {
            t.rollback();
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Wrong resume ....");
            t.resume(tx);
            System.out.println("**WRONG: TM resume successful <===");
            assert (false);
        } catch (InvalidTransactionException ex) {
            System.out.println("**Caught InvalidTransactionException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
        
    }

    public void testWrongTMOperationsAfterCommit() {
        System.out.println("**Testing Wrong TM Operations After Commit ===>");
        try {
            t.begin();
            t.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling TM commit ===>");
            t.commit();
            System.out.println("**WRONG: TM commit successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling TM rollback ===>");
            t.rollback();
            System.out.println("**WRONG: TM rollback successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling TM setRollbackOnly ===>");
            t.setRollbackOnly();
            System.out.println("**WRONG: TM setRollbackOnly successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

    }

    public void testWrongTXOperationsAfterCommit() {
        System.out.println("**Testing Wrong Tx Operations After Commit ===>");
        Transaction tx = null;
        try {
            t.begin();
            tx = t.getTransaction();
            t.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling Tx commit ===>");
            tx.commit();
            System.out.println("**WRONG: Tx commit successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling Tx rollback ===>");
            tx.rollback();
            System.out.println("**WRONG: Tx rollback successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling Tx setRollbackOnly ===>");
            tx.setRollbackOnly();
            System.out.println("**WRONG: Tx setRollbackOnly successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling Tx enlistResource ===>");
            tx.enlistResource(new TestResource());
            System.out.println("**WRONG: Tx enlistResource successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling Tx delistResource ===>");
            tx.delistResource(new TestResource(), XAResource.TMSUCCESS);
            System.out.println("**WRONG: Tx delistResource successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling Tx registerSynchronization ===>");
            TestSync s = new TestSync(false);
            tx.registerSynchronization(s);
            System.out.println("**WRONG: Tx registerSynchronization successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

    }

    public void testWrongTMCommit() {
        System.out.println("**Testing Wrong TM commit ===>");
        try {
            System.out.println("**Calling TM commit ===>");
            t.commit();
            System.out.println("**WRONG: TM commit successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testWrongTMRollback() {
        System.out.println("**Testing Wrong TM Rollback ===>");
        try {
            System.out.println("**Calling TM rollback ===>");
            t.rollback();
            System.out.println("**WRONG: TM rollback successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testWrongTMTimeout() {
        System.out.println("**Testing Wrong TM Timeout ===>");
        try {
            System.out.println("**Calling TM setTransactionTimeout ===>");
            t.setTransactionTimeout(-1);
            System.out.println("**WRONG: TM setTransactionTimeout successful <===");
            assert (false);
        } catch (SystemException ex) {
            System.out.println("**Caught SystemException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testWrongUTXTimeout() {
        System.out.println("**Testing Wrong UTX Timeout ===>");
        try {
            UserTransaction utx = createUtx();
            System.out.println("**Calling UTX setTransactionTimeout ===>");
            utx.setTransactionTimeout(-1);
            System.out.println("**WRONG: UTX setTransactionTimeout successful <===");
            assert (false);
        } catch (SystemException ex) {
            System.out.println("**Caught SystemException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testWrongUTXCommit() {
        System.out.println("**Testing Wrong UTX commit ===>");
        try {
            UserTransaction utx = createUtx();
            System.out.println("**Calling UTX commit ===>");
            utx.commit();
            System.out.println("**WRONG: UTX commit successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testWrongUTXBegin() {
        System.out.println("**Testing Wrong UTX begin ===>");
        try {
            UserTransaction utx = createUtx();
            System.out.println("**Calling TWICE UTX begin ===>");
            utx.begin();
            utx.begin();
            System.out.println("**WRONG: TWICE UTX begin successful <===");
            assert (false);
        } catch (NotSupportedException ne) {
            System.out.println("**Caught NotSupportedException <===");
            assert (true);
        } catch (SystemException ne) {
            System.out.println("**Caught SystemException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testWrongUTXOperationsAfterCommit() {
        System.out.println("**Testing Wrong UTx Operations After Commit ===>");
        UserTransaction utx = null;
        try {
            t.begin();
            utx = createUtx();
            t.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling UTx commit ===>");
            utx.commit();
            System.out.println("**WRONG: UTx commit successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling UTx rollback ===>");
            utx.rollback();
            System.out.println("**WRONG: UTx rollback successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling UTx setRollbackOnly ===>");
            utx.setRollbackOnly();
            System.out.println("**WRONG: UTx setRollbackOnly successful <===");
            assert (false);
        } catch (IllegalStateException ex) {
            System.out.println("**Caught IllegalStateException <===");
            assert (true);
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
            TestSync s = new TestSync(false);
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
            System.out.println("**No-tx suspend ....");
            assertNull(t.suspend());

            System.out.println("**Starting transaction ....");
            t.begin();

            Transaction tx = t.suspend();
            assertNotNull(tx);

            System.out.println("**TX suspended ....");

            System.out.println("**No-tx suspend ....");
            assertNull(t.suspend());

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
            TestSync s = new TestSync(false);
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

    public void testTxCommitFailBC() {
        System.out.println("**Testing TX commit with exception in beforeCompletion ===>");
        try {
            // Suppress warnings from beforeCompletion() logging
            ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);

            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync(true);
            tx.registerSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            System.out.println("**Calling TX commit ===>");
            try {
                tx.commit();
                assert (false);
            } catch (RollbackException ex) {
                System.out.println("**Caught expected exception...");

                Throwable te = ex.getCause();
                if (te != null && te instanceof MyRuntimeException) {
                    System.out.println("**Caught expected nested exception...");
                } else {
                    System.out.println("**Unexpected nested exception: " + te);
                    assert (false);
                }
            }
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

    public void testTMCommitFailBC() {
        System.out.println("**Testing TM commit with exception in beforeCompletion ===>");
        try {
            // Suppress warnings from beforeCompletion() logging
            ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);

            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync(true);
            tx.registerSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            System.out.println("**Calling TM commit ===>");
            try {
                t.commit();
                assert (false);
            } catch (RollbackException ex) {
                System.out.println("**Caught expected exception...");

                Throwable te = ex.getCause();
                if (te != null && te instanceof MyRuntimeException) {
                    System.out.println("**Caught expected nested exception...");
                } else {
                    System.out.println("**Unexpected nested exception: " + te);
                    assert (false);
                }
            }
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

    public void testTxCommitFailInterposedSyncBC() {
        System.out.println("**Testing TX commit with exception in InterposedSync in beforeCompletion ===>");
        try {
            // Suppress warnings from beforeCompletion() logging
            ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);

            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering InterposedSynchronization ....");
            TestSync s = new TestSync(true);
            ((JavaEETransactionImpl)tx).registerInterposedSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            System.out.println("**Calling TX commit ===>");
            try {
                tx.commit();
                assert (false);
            } catch (RollbackException ex) {
                System.out.println("**Caught expected exception...");

                Throwable te = ex.getCause();
                if (te != null && te instanceof MyRuntimeException) {
                    System.out.println("**Caught expected nested exception...");
                } else {
                    System.out.println("**Unexpected nested exception: " + te);
                    assert (false);
                }
            }
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

    public void testTxCommitRollbackBC() {
        System.out.println("**Testing TX commit with rollback in beforeCompletion ===>");
        try {
            // Suppress warnings from beforeCompletion() logging
            ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);

            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync(t);
            tx.registerSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            System.out.println("**Calling TX commit ===>");
            try {
                tx.commit();
                assert (false);
            } catch (RollbackException ex) {
                System.out.println("**Caught expected exception...");
            }
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

    private UserTransaction createUtx() throws javax.naming.NamingException {
        UserTransaction utx = new UserTransactionImpl();
        InvocationManager im = new org.glassfish.api.invocation.InvocationManagerImpl();
        ((UserTransactionImpl)utx).setForTesting((JavaEETransactionManager)t, im);
        return utx;
    }

    static class TestSync implements Synchronization {

        // Used to validate the calls
        private boolean fail = false;
        private TransactionManager t;

        protected boolean called_beforeCompletion = false;
        protected boolean called_afterCompletion = false;

        public TestSync(boolean fail) {
            this.fail = fail;
        }

        public TestSync(TransactionManager t) {
            fail = true;
            this.t = t;
        }

        public void beforeCompletion() {
            System.out.println("**Called beforeCompletion  **");
            called_beforeCompletion = true;
            if (fail) {
                System.out.println("**Failing in beforeCompletion  **");
                if (t != null) {
                    try {
                        System.out.println("**Calling setRollbackOnly **");
                        t.setRollbackOnly();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("**Throwing MyRuntimeException... **");
                    throw new MyRuntimeException("test");
                }
            }
        }

        public void afterCompletion(int status) {
            System.out.println("**Called afterCompletion with status:  "
                    + JavaEETransactionManagerSimplified.getStatusAsString(status));
            called_afterCompletion = true;
        }
    }

    static class MyRuntimeException extends RuntimeException {
        public MyRuntimeException(String msg) {
            super(msg);
        }
    }

    static class TestResource implements XAResource {

      public void commit(Xid xid, boolean onePhase) throws XAException{}
      public boolean isSameRM(XAResource xaresource) throws XAException { return false; }
      public void rollback(Xid xid) throws XAException {}
      public int prepare(Xid xid) throws XAException { return XAResource.XA_OK; }
      public boolean setTransactionTimeout(int i) throws XAException { return true; }
      public int getTransactionTimeout() throws XAException { return 0; }
      public void forget(Xid xid) throws XAException { }
      public void start(Xid xid, int flags) throws XAException { }
      public void end(Xid xid, int flags) throws XAException { }
      public Xid[] recover(int flags) throws XAException { return null; }

    }


}
