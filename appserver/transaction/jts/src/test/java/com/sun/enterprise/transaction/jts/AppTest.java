/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.transaction.jts;

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
import com.sun.enterprise.transaction.UserTransactionImpl;
import com.sun.enterprise.transaction.JavaEETransactionManagerSimplified;
import com.sun.enterprise.transaction.JavaEETransactionManagerSimplifiedDelegate;
import com.sun.enterprise.transaction.TransactionServiceConfigListener;
import com.sun.enterprise.transaction.TransactionSynchronizationRegistryImpl;

import com.sun.jts.jtsxa.OTSResourceImpl;
import com.sun.jts.jta.SynchronizationImpl;

import com.sun.enterprise.resource.*;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.pool.PoolManagerImpl;

import com.sun.logging.LogDomains;

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
            JavaEETransactionManagerDelegate d = new JavaEETransactionManagerJTSDelegate();
            t.setDelegate(d);
            d.setTransactionManager(t);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

    }

    /**
     * Test that you can't replace delegate with a lower order.
     */
    public void testReplaceDelegate() {
        JavaEETransactionManagerDelegate d = new JavaEETransactionManagerSimplifiedDelegate();
        t.setDelegate(d);
        assertFalse(((JavaEETransactionManagerSimplified)t).isDelegate(d));
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

/**
    public void testUTXTimeout() {
        System.out.println("**Testing UTX Timeout ===>");
        TestSync s = new TestSync(false);

        long now = System.currentTimeMillis();
        try {
            UserTransaction utx = createUtx();
            System.out.println("**Calling UTX setTransactionTimeout ===>");
            utx.setTransactionTimeout(45);
            utx.begin();

            Transaction tx = t.getTransaction();
            System.out.println("**Registering Synchronization ....");
            tx.registerSynchronization(s);

            TestResource theResource = new TestResource(tx);
            t.enlistResource(tx, new TestResourceHandle(theResource));

            Thread.sleep(40000);
            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);

            utx.commit(); 
            //System.out.println("**WRONG: UTX commit successful <===");
            System.out.println("**UTX commit successful after <===" + (System.currentTimeMillis() - now) + "millis");
            assert (true);
            //assert (false);
        } catch (RollbackException ex) {
            //ex.printStackTrace();
            System.out.println("**Caught UNexpected RollbackException <===");
            //assertFalse ("beforeCompletion was called", s.called_beforeCompletion);
            //assertTrue ("afterCompletion was not called", s.called_afterCompletion);
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }
**/

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

    public void testResourceStatus() {
        System.out.println("**=================Testing Resource Status ===>");
        Transaction tx = null;
        try {
            t.begin();
            tx = t.getTransaction();
            System.out.println("**Testing Resource Status in 2PC ===>");
            //TestResource theResource = new TestResource(tx);
            TestResource theResource = new TestResource(tx, 1L);
            TestResource theResource2 = new TestResource(tx, 1L);
            TestResource theResource1 = new TestResource(tx);
            t.enlistResource(tx, new TestResourceHandle(theResource));
            t.enlistResource(tx, new TestResourceHandle(theResource2));
            t.enlistResource(tx, new TestResourceHandle(theResource1));
            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);
            t.delistResource(tx, new TestResourceHandle(theResource2), XAResource.TMSUCCESS);
            t.delistResource(tx, new TestResourceHandle(theResource1), XAResource.TMSUCCESS);
            t.commit();

            String status = JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus());
            System.out.println("**Status after commit: "  + status + " <===");
            assert(theResource.prepareStatusOK());
            assert(theResource1.prepareStatusOK());
            assert(theResource.commitStatusOK());
            assert(theResource1.commitStatusOK());

        } catch (Throwable ex) {
            ex.printStackTrace();
            assert (false);
        }
        System.out.println("**=================End part1 Resource Status ===>");

        try {
            t.begin();
            tx = t.getTransaction();
            System.out.println("**Testing resource status in rollback ===>");
            TestResource theResource = new TestResource(tx, 1L);
            TestResource theResource2 = new TestResource(tx, 1L);
            TestResource theResource1 = new TestResource(tx);
            t.enlistResource(tx, new TestResourceHandle(theResource));
            t.enlistResource(tx, new TestResourceHandle(theResource2));
            t.enlistResource(tx, new TestResourceHandle(theResource1));
            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);
            t.delistResource(tx, new TestResourceHandle(theResource2), XAResource.TMSUCCESS);
            t.delistResource(tx, new TestResourceHandle(theResource1), XAResource.TMSUCCESS);

            t.rollback();

            String status = JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus());
            System.out.println("**Status after rollback: "  + status + " <===");
            assert(theResource.rollbackStatusOK());
            assert(theResource1.rollbackStatusOK());
            assert(!theResource2.isAssociated());

        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
        System.out.println("**=================End part2 Resource Status ===>");

        try {
            t.begin();
            tx = t.getTransaction();
            System.out.println("**Testing resource status in 1PC ===>");
            TestResource theResource = new TestResource(tx);
            t.enlistResource(tx, new TestResourceHandle(theResource));
            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);
            t.commit();

            String status = JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus());
            System.out.println("**Status after commit: "  + status + " <===");
            assert(theResource.commitStatusOK());

        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

    }

    public void testSetRollbackOnlyBeforeEnlist() {
      try {
            t.begin();
            Transaction tx = t.getTransaction();
            System.out.println("**Testing resource status in setRollbackOnly ===>");
            tx.setRollbackOnly();
            String status = JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus());
            System.out.println("**Status after setRollbackOnly: "  + status + " <===");
            assertEquals(status, "MarkedRollback");

            TestResource theResource = new TestResource(tx);
            try {
                t.enlistResource(tx, new TestResourceHandle(theResource));
                t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);
                System.out.println("**Did NOT get RollbackException from t.enlistResource <===");
                assert (false);
            } catch (RollbackException ex) {
                System.out.println("**Caught expected RollbackException from t.enlistResource <===");
            }

            status = JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus());
            System.out.println("**Status after setRollbackOnly & enlist: "  + status + " <===");
            assertEquals(status, "MarkedRollback");
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        } finally {
            try {
                t.rollback();
                String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
                System.out.println("**Status after rollback: "  + status + " <===");
            } catch (Exception ex) {
                ex.printStackTrace();
                assert (false);
            }
        }

    }

    public void testWrongTXOperationsAfterCommit() {
        System.out.println("**Testing Wrong Tx Operations After Commit ===>");
        Transaction tx = null;
        try {
            t.begin();
            tx = t.getTransaction();
            System.out.println("**Calling Tx enlistResource ===>");
            TestResource theResource = new TestResource();
            t.enlistResource(tx, new TestResourceHandle(theResource));
            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);
            t.commit();

            String status = JavaEETransactionManagerSimplified.getStatusAsString(tx.getStatus());
            System.out.println("**Status after commit: "  + status + " <===");

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
            System.out.println("**Calling begin-resume(null) ===>");
            t.begin();
            tx = t.getTransaction();
            t.resume(null);
            System.out.println("**WRONG: begin-resume(null) successful <===");
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
            t.rollback();
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }

        try {
            System.out.println("**Calling resume(tx) ===>");
            t.resume(tx);
            System.out.println("**WRONG: resume(tx) successful <===");
            assert (false);
        } catch (InvalidTransactionException ex) {
            System.out.println("**Caught InvalidTransactionException <===");
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

    public void testWrongUTXBegin() {
        System.out.println("**Testing Wrong UTX begin ===>");
        try {
            UserTransaction utx = createUtx();
            utx.begin();
            System.out.println("**Calling TWICE UTX begin ===>");
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

    public void testTxCommitFailBC2PC() {
        System.out.println("**Testing TX commit with exception in beforeCompletion of 2PC ===>");
        try {
            // Suppress warnings from beforeCompletion() logging
            ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);
            LogDomains.getLogger(SynchronizationImpl.class, LogDomains.TRANSACTION_LOGGER).setLevel(Level.SEVERE);

            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync(true);
            tx.registerSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            TestResource theResource = new TestResource();
            TestResource theResource1 = new TestResource();

            t.enlistResource(tx, new TestResourceHandle(theResource));
            t.enlistResource(tx, new TestResourceHandle(theResource1));

            theResource.setCommitErrorCode(9999);
            theResource1.setCommitErrorCode(9999);

            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);
            t.delistResource(tx, new TestResourceHandle(theResource1), XAResource.TMSUCCESS);

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

    public void testTMCommitFailBC2PC() {
        System.out.println("**Testing TM commit with exception in beforeCompletion of 2PC ===>");
        try {
            // Suppress warnings from beforeCompletion() logging
            ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);
            LogDomains.getLogger(SynchronizationImpl.class, LogDomains.TRANSACTION_LOGGER).setLevel(Level.SEVERE);

            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync(true);
            tx.registerSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            TestResource theResource = new TestResource();
            TestResource theResource1 = new TestResource();

            t.enlistResource(tx, new TestResourceHandle(theResource));
            t.enlistResource(tx, new TestResourceHandle(theResource1));

            theResource.setCommitErrorCode(9999);
            theResource1.setCommitErrorCode(9999);

            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);
            t.delistResource(tx, new TestResourceHandle(theResource1), XAResource.TMSUCCESS);

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

    public void testTxCommitFailBC1PC() {
        System.out.println("**Testing TX commit with exception in beforeCompletion of 1PC ===>");
        try {
            // Suppress warnings from beforeCompletion() logging
            ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);
            LogDomains.getLogger(SynchronizationImpl.class, LogDomains.TRANSACTION_LOGGER).setLevel(Level.SEVERE);

            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync(true);
            tx.registerSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            TestResource theResource = new TestResource();
            t.enlistResource(tx, new TestResourceHandle(theResource));

            theResource.setCommitErrorCode(9999);
            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);

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

    public void testTxCommitFailBC2PCInterposedSynchronization() {
        System.out.println("**Testing TX commit with exception in InterposedSynchronization in beforeCompletion of 2PC ===>");
        try {
            // Suppress warnings from beforeCompletion() logging
            ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);
            LogDomains.getLogger(SynchronizationImpl.class, LogDomains.TRANSACTION_LOGGER).setLevel(Level.SEVERE);

            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync(true);

            TransactionSynchronizationRegistry ts = new TransactionSynchronizationRegistryImpl(t);
            ts.registerInterposedSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            TestResource theResource = new TestResource();
            TestResource theResource1 = new TestResource();

            t.enlistResource(tx, new TestResourceHandle(theResource));
            t.enlistResource(tx, new TestResourceHandle(theResource1));

            theResource.setCommitErrorCode(9999);
            theResource1.setCommitErrorCode(9999);

            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);
            t.delistResource(tx, new TestResourceHandle(theResource1), XAResource.TMSUCCESS);

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

    public void testTxCommitFailBC1PCInterposedSynchronization() {
        System.out.println("**Testing TX commit with exception in InterposedSynchronization in beforeCompletion of 1PC ===>");
        try {
            // Suppress warnings from beforeCompletion() logging
            ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);
            LogDomains.getLogger(SynchronizationImpl.class, LogDomains.TRANSACTION_LOGGER).setLevel(Level.SEVERE);

            System.out.println("**Starting transaction ....");
            t.begin();
            Transaction tx = t.getTransaction();

            System.out.println("**Registering Synchronization ....");
            TestSync s = new TestSync(true);
            TransactionSynchronizationRegistry ts = new TransactionSynchronizationRegistryImpl(t);
            ts.registerInterposedSynchronization(s);

            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**TX Status after begin: " + status);

            assertEquals (status, "Active");

            TestResource theResource = new TestResource();
            t.enlistResource(tx, new TestResourceHandle(theResource));

            theResource.setCommitErrorCode(9999);
            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);

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

    public void testCommit2PCWithRollbackExc1() {
        System.out.println("**Testing XA_RBROLLBACK in prepare & XA_HEURRB in rollback ===>");
        _testCommit2PCWithRollbackExc(XAException.XA_RBROLLBACK, XAException.XA_HEURRB);
    }

    public void testCommit2PCWithRollbackExc2() {
        System.out.println("**Testing XA_RBROLLBACK in prepare & 2 XA_HEURRB in rollback ===>");
        _testCommit2PCWithRollbackExc(XAException.XA_RBROLLBACK, XAException.XA_HEURRB, XAException.XA_HEURRB);
    }

    public void testCommit2PCWithRollbackExc3() {
        System.out.println("**Testing XA_RDONLY in prepare & 2 XA_HEURRB in rollback ===>");
        _testCommit2PCWithRollbackExc(XAException.XA_RDONLY, XAException.XA_HEURRB, 9999);
    }

    private void _testCommit2PCWithRollbackExc(int preapareErrCode, int... rollbackErrorCode) {
        System.out.println("**Testing different rollback errors in prepare & rollback ===>");
        TestResource theResourceP = new TestResource();
        TestResource[] theResourceR = null;
        try {
            System.out.println("**Starting transaction ....");
            t.begin();
            assertEquals (JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus()), 
                "Active");

            // Create and set invMgr
            createUtx();
            Transaction tx = t.getTransaction();
            t.enlistResource(tx, new TestResourceHandle(theResourceP));
            theResourceP.setPrepareErrorCode(preapareErrCode);
            theResourceR = enlistForRollback(tx, rollbackErrorCode);

            t.delistResource(tx, new TestResourceHandle(theResourceP), XAResource.TMSUCCESS);
            
            System.out.println("**Calling TM commit ===>");
            t.commit();
            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            System.out.println("**Error - successful commit - Status after commit: " + status + " <===");
            assert (false);
        } catch (RollbackException ex) {
            System.out.println("**Caught expected RollbackException...");
            try {
                String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
                System.out.println("**Status after commit: " + status + " <===");
            } catch (Exception ex1) {
                System.out.println("**Caught exception checking for status ...");
                ex1.printStackTrace();
            }
            boolean status = theResourceP.forgetCalled();
            System.out.println("**Forget 1 was called: " + theResourceP.forgetCalled());

            for (int i = 0; i < theResourceR.length; i++) {
                System.out.println("**Forget 2 was called for resourceR " + i + ": " + theResourceR[i].forgetCalled());
                status = status && theResourceR[i].forgetCalled();
            }
            assert (status);
        } catch (Throwable ex) {
            System.out.println("**Caught NOT a RollbackException during 2PC...");
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testCommitOnePhaseWithHeuristicRlbExc1() {
        _testCommitOnePhaseWithExc(XAException.XA_HEURRB, HeuristicRollbackException.class, false, true);
    }

    public void testCommitOnePhaseWithHeuristicMixedExc2() {
        _testCommitOnePhaseWithExc(XAException.XA_HEURMIX, HeuristicMixedException.class, false, true);
    }

    public void testCommitOnePhaseWithRlbExc1() {
        System.out.println("**Testing XAER_NOTA in 1PC ===>");
        _testCommitOnePhaseWithExc(XAException.XAER_NOTA, RollbackException.class, false, false);
    }

    public void testCommitOnePhaseWithRlbExc2() {
        System.out.println("**Testing XAER_RMERR in 1PC ===>");
        _testCommitOnePhaseWithExc(XAException.XAER_RMERR, RollbackException.class, false, false);
    }

    public void testCommitOnePhaseWithRlbExc3() {
        System.out.println("**Testing XA_RBROLLBACK in rollback part of 1PC ===>");
        _testCommitOnePhaseWithExc(XAException.XA_RBROLLBACK, RollbackException.class, true, false);
    }

    public void testCommitOnePhaseWithRlbExc4() {
        System.out.println("**Testing XAER_RMERR in rollback part of 1PC ===>");
        _testCommitOnePhaseWithExc(XAException.XAER_RMERR, RollbackException.class, true, false);
    }

    public void testCommitOnePhaseWithRlbExc5() {
        System.out.println("**Testing XAER_RMFAIL in rollback part of 1PC ===>");
        _testCommitOnePhaseWithExc(XAException.XAER_RMFAIL, RollbackException.class, true, false);
    }

    public void testCommitOnePhaseWithRlbExc6() {
        System.out.println("================= **Testing XA Exception on start ===>");
        int falures[] = {XAException.XAER_RMFAIL, 
                XAException.XAER_RMERR, 
                XAException.XAER_NOTA, 
                XAException.XAER_INVAL, 
                XAException.XAER_PROTO, 
                XAException.XAER_DUPID};
        boolean result = true;
        for (int failure : falures) {
            try {
                boolean rc = _testCommitOnePhaseWithExc(failure, SystemException.class, false, false, true, true);
                result &= rc;
                if (rc) {
                    System.out.println("================= **Testing code " + failure + " OK");
                } else {
                    System.out.println("================= **ERROR Testing code " + failure);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        assert (result);
    }

    public void testCommitOnePhaseWithXAExc1() {
        System.out.println("**Testing TM with failed 1PC commit ===>");
        _testCommitOnePhaseWithExc(XAException.XAER_RMFAIL, SystemException.class, false, false);
    }

    public void testCommitOnePhaseWithXAExc2() {
        System.out.println("**Testing TM with XA_HEURCOM in 1PC commit ===>");
        _testCommitOnePhaseWithExc(XAException.XA_HEURCOM, null, false, true);
    }

    public void testRollbackWithErrorNoExc1() {
        System.out.println("**Testing XA_RBROLLBACK in rollback ===>");
        _testXARollback(XAException.XA_RBROLLBACK);
    }

    public void testRollbackWithErrorNoExc2() {
        System.out.println("**Testing XAER_RMERR in rollback ===>");
        _testXARollback(XAException.XAER_RMERR);
    }

    public void testRollbackWithErrorNoExc3() {
        System.out.println("**Testing XAER_NOTA in rollback ===>");
        _testXARollback(XAException.XAER_NOTA);
    }

    public void testRollbackWithErrorNoExc4() {
        System.out.println("**Testing XAER_RMFAIL in rollback ===>");
        _testXARollback(XAException.XAER_RMFAIL);
    }

    public void testRollbackWithErrorNoExc5() {
        System.out.println("**Testing XA_HEURRB in rollback ===>");
        _testXARollback(XAException.XA_HEURRB);
    }

    public void testRollbackWithErrorNoExc6() {
        System.out.println("**Testing 2 XA_HEURRB in rollback ===>");
        _testXARollback(XAException.XA_HEURRB, XAException.XA_HEURRB);
    }

    private void _testCommitOnePhaseWithExc(int errorCode, Class exType, boolean setRollbackOnly, boolean isHeuristic) {
        _testCommitOnePhaseWithExc(errorCode, exType, setRollbackOnly, isHeuristic, false, false);
    }

    private boolean _testCommitOnePhaseWithExc(int errorCode, Class exType, 
            boolean setRollbackOnly, boolean isHeuristic, boolean failAtEnlist, boolean returnStatus) {
        boolean rc = true;

        System.out.println("**Testing TM with " + ((exType == null)? "success" : exType.getName()) + " during 1PC commit ===>");
        ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);
        LogDomains.getLogger(OTSResourceImpl.class, LogDomains.TRANSACTION_LOGGER).setLevel(Level.SEVERE);
        TestResource theResource = new TestResource();
        boolean enlist_status = true;
        try {
            System.out.println("**Starting transaction ....");
            t.begin();
            assertEquals (JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus()), 
                "Active");

            // Create and set invMgr
            createUtx();
            Transaction tx = t.getTransaction();
            if (setRollbackOnly) {
                theResource.setRollbackErrorCode(errorCode);
            } else if (failAtEnlist) {
                theResource.setStartErrorCode(errorCode);
            } else {
                theResource.setCommitErrorCode(errorCode);
            }

            try {
                t.enlistResource(tx, new TestResourceHandle(theResource));
            } catch (Exception ex) {
                if (failAtEnlist && exType != null && exType.isInstance(ex)) {
                    System.out.println("**Caught expected " + exType.getName() + " ...");
                } else {
                    System.out.println("**Caught unexpected exception" );
                    ex.printStackTrace();
                    enlist_status = false;
                }
            }

            t.delistResource(tx, new TestResourceHandle(theResource), XAResource.TMSUCCESS);
            
            if (setRollbackOnly) {
                System.out.println("**Calling TM setRollbackOnly ===>");
                t.setRollbackOnly();
            }

            System.out.println("**Calling TM commit ===>");
            t.commit();
            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            if (!failAtEnlist && exType != null) {
                System.out.println("**Error - successful commit - Status after commit: " + status + " <===");
                if (returnStatus) {
                    rc = false;
                } else {
                    assert (false);
                }
            } else {
                System.out.println("**Successful commit - Status after commit: " + status + " <===");
                if (returnStatus) {
                    rc = rc && enlist_status;
                } else {
                    assert (enlist_status);
                }
            }
        } catch (Exception ex) {
            if (exType != null && exType.isInstance(ex)) {
                System.out.println("**Caught expected " + exType.getName() + " ...");
                try {
                    String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
                    System.out.println("**Status after commit: " + status + " <===");
                } catch (Exception ex1) {
                    ex1.printStackTrace();
                }

                boolean status = true;
                if (isHeuristic) {
                   status = theResource.forgetCalled();
                   System.out.println("**Forget was called: " + status);
                }

                if (returnStatus) {
                    rc = rc && enlist_status && status;
                } else {
                    assert (enlist_status && status);
                }
            } else {
                System.out.println("**Caught " + ((exType == null)? " unexpected " : " NOT a " + exType.getName()) + " during 2PC...");
                ex.printStackTrace();
                if (returnStatus) {
                    rc = false;
                } else {
                    assert (false);
                }
            }
        }
        return rc;
    }

    private void _testXARollback(int... errorCode) {
        System.out.println("**Testing TM with XA error during XA rollback ===>");
        ((JavaEETransactionManagerSimplified)t).getLogger().setLevel(Level.SEVERE);
        LogDomains.getLogger(OTSResourceImpl.class, LogDomains.TRANSACTION_LOGGER).setLevel(Level.SEVERE);
        try {
            System.out.println("**Starting transaction ....");
            t.begin();
            assertEquals (JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus()),
                "Active");

            // Create and set invMgr
            createUtx();
            Transaction tx = t.getTransaction();
            enlistForRollback(tx, errorCode);

            System.out.println("**Calling TM rollback ===>");
            t.rollback();
            System.out.println("**Status after rollback: "
                    + JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus())
                    + " <===");
            assert (true);
        } catch (Exception ex) {
            System.out.println("**Caught unexpected exception");
            ex.printStackTrace();
            assert (false);
        }
    }

    public void testCommit2PCWithXAExc1() {
        System.out.println("**Testing TM with 1 XAER_RMFAIL 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XAER_RMFAIL, 9999, SystemException.class);
    }

    public void testCommit2PCWithXAExc2() {
        System.out.println("**Testing TM with both XA_HEURRB 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XA_HEURRB, XAException.XA_HEURRB, HeuristicRollbackException.class, true);
    }

    public void testCommit2PCWithXAExc3() {
        System.out.println("**Testing TM with 1 XA_HEURRB & 1 XA_HEURMIX 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XA_HEURRB, XAException.XA_HEURMIX, HeuristicMixedException.class, true);
    }

    public void testCommit2PCWithXAExc4() {
        System.out.println("**Testing TM with 1 XA_HEURRB & 1 XA_HEURCOM 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XA_HEURRB, XAException.XA_HEURCOM, HeuristicMixedException.class, true);
    }

    public void testCommit2PCWithXAExc5() {
        System.out.println("**Testing TM with 1 XA_HEURCOM & 1 XA_HEURRB 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XA_HEURCOM, XAException.XA_HEURRB, HeuristicMixedException.class, true);
    }

    public void testCommit2PCWithXAExc6() {
        System.out.println("**Testing TM with 2 XA_HEURCOM 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XA_HEURCOM, XAException.XA_HEURCOM, true, null, true);
    }

    public void testCommit2PCWithXAExc7() {
        System.out.println("**Testing TM with 1 XA_HEURCOM & 1 OK 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XA_HEURCOM, 9999, true, null, true);
    }

    public void testCommit2PCWithXAExc8() {
        System.out.println("**Testing TM with 1 XA_HEURRB 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XA_HEURRB, 9999, HeuristicMixedException.class, true);
    }

    public void testCommit2PCWithXAExc9() {
        System.out.println("**Testing TM with 2nd XAER_PROTO in *prepare* of 2PC commit ===>");
        _testCommit2PCWithXAExc(9999, XAException.XAER_PROTO, false, SystemException.class);
    }

    public void testCommit2PCWithXAExc10() {
        System.out.println("**Testing TM with 1st XAER_PROTO in *prepare* of 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XAER_PROTO, 9999, false, SystemException.class);
    }

    public void testCommit2PCWithXAExc11() {
        System.out.println("**Testing TM with 2nd XAER_INVAL in *prepare* of 2PC commit ===>");
        _testCommit2PCWithXAExc(9999, XAException.XAER_INVAL, false, SystemException.class);
    }

    public void testCommit2PCWithXAExc12() {
        System.out.println("**Testing TM with 1st XAER_INVAL in *prepare* of 2PC commit ===>");
        _testCommit2PCWithXAExc(XAException.XAER_INVAL, 9999, false, SystemException.class);

    }

    private void _testCommit2PCWithXAExc(int errorCode1, int errorCode2, Class exType) {
        _testCommit2PCWithXAExc(errorCode1, errorCode2, true, exType, false);
    }

    private void _testCommit2PCWithXAExc(int errorCode1, int errorCode2, boolean failOnCommit, Class exType) {
        _testCommit2PCWithXAExc(errorCode1, errorCode2, failOnCommit, exType, false);
    }

    private void _testCommit2PCWithXAExc(int errorCode1, int errorCode2, Class exType, boolean isHeuristic) {
        _testCommit2PCWithXAExc(errorCode1, errorCode2, true, exType, isHeuristic);
    }

    private void _testCommit2PCWithXAExc(int errorCode1, int errorCode2, boolean failOnCommit, Class exType, boolean isHeuristic) {
        System.out.println("**Testing TM with " + ((exType == null)? "success" : exType.getName()) + " during 2PC commit ===>");
        TestResource theResource1 = new TestResource();
        TestResource theResource2 = new TestResource();
        TestSync s = new TestSync(false);
        try {
            System.out.println("**Starting transaction ....");
            t.begin();
            assertEquals (JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus()), 
                "Active");

            // Create and set invMgr
            createUtx();
            Transaction tx = t.getTransaction();

            tx.registerSynchronization(s);

            t.enlistResource(tx, new TestResourceHandle(theResource1));
            t.enlistResource(tx, new TestResourceHandle(theResource2));

            if (failOnCommit) {
                theResource1.setCommitErrorCode(errorCode1);
                theResource2.setCommitErrorCode(errorCode2);
            } else {
                theResource1.setPrepareErrorCode(errorCode1);
                theResource2.setPrepareErrorCode(errorCode2);
            }

            t.delistResource(tx, new TestResourceHandle(theResource1), XAResource.TMSUCCESS);
            t.delistResource(tx, new TestResourceHandle(theResource2), XAResource.TMSUCCESS);
            
            System.out.println("**Calling TM commit ===>");
            t.commit();
            String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
            if (exType != null) {
                System.out.println("**Error - successful commit - Status after commit: " + status + " <===");
                assert (false);
            } else {
                System.out.println("**Successful commit - Status after commit: " + status + " <===");
                assertTrue ("beforeCompletion was not called", s.called_beforeCompletion);
                assertTrue ("afterCompletion was not called", s.called_afterCompletion);
                assert (true);
            }
        } catch (Throwable ex) {
            if (exType != null && exType.isInstance(ex)) {
                System.out.println("**Caught expected " + exType.getName() + " ...");
                try {
                    String status = JavaEETransactionManagerSimplified.getStatusAsString(t.getStatus());
                    System.out.println("**Status after commit: " + status + " <===");
                } catch (Exception ex1) {
                    System.out.println("**Caught exception checking for status ...");
                    ex1.printStackTrace();
                }
                boolean status = true;
                if (isHeuristic) {
                   status = theResource1.forgetCalled() && theResource2.forgetCalled();
                   System.out.println("**Forget 1 was called: " + theResource1.forgetCalled());
                   System.out.println("**Forget 2 was called: " + theResource2.forgetCalled());
                }

                assertTrue ("beforeCompletion was not called", s.called_beforeCompletion);
                assertTrue ("afterCompletion was not called", s.called_afterCompletion);
                assert (status);
            } else {
                System.out.println("**Caught " + ((exType == null)? " unexpected " : " NOT a " + exType.getName()) + " during 2PC...");
                ex.printStackTrace();
                assert (false);
            }
        }
    }

    private UserTransaction createUtx() throws javax.naming.NamingException {
        UserTransaction utx = new UserTransactionImpl();
        InvocationManager im = new org.glassfish.api.invocation.InvocationManagerImpl();
        ((UserTransactionImpl)utx).setForTesting(t, im);
        return utx;
    }

    private TestResource[] enlistForRollback(Transaction tx, int... errorCode) throws Exception {
        TestResource[] theResources = new TestResource[errorCode.length];
        for (int i = 0; i < errorCode.length; i++) {
            theResources[i] = new TestResource();
            t.enlistResource(tx, new TestResourceHandle(theResources[i]));
            theResources[i].setRollbackErrorCode(errorCode[i]);
        }

        for (int i = 0; i < errorCode.length; i++) {
            t.delistResource(tx, new TestResourceHandle(theResources[i]), XAResource.TMSUCCESS);
        }

        return theResources;
    }


    static class TestSync implements Synchronization {

        // Used to validate the calls
        private boolean fail = false;
        private TransactionManager t = null;

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
    
      // allow only one resource in use at a time
      private boolean inUse;
      private boolean _forgetCalled = false;
      private boolean _isHeuristic = false;
    
      private int commitErrorCode = 9999;
      private int rollbackErrorCode = 9999;
      private int prepareErrorCode = 9999;
      private int startErrorCode = 9999;

      private Transaction tx;
      private int commit_status = -1;
      private int rollback_status = -1;
      private int prepare_status = -1;

      private static long id0 = System.currentTimeMillis();
      private long id = ++id0;
    
      TestResource() {}

      TestResource(Transaction tx) {
         this.tx = tx;
         
      }

      TestResource(Transaction tx, long id) {
         this.tx = tx;
         this.id = id;
      }

      // to test different xaexception error codes
      public void setCommitErrorCode(int errorCode) {
        this.commitErrorCode = errorCode;
        setHeuristic(errorCode);
      }
    
      public void setStartErrorCode(int errorCode) {
        this.startErrorCode = errorCode;
      }
    
      public void setRollbackErrorCode(int errorCode) {
        this.rollbackErrorCode = errorCode;
        setHeuristic(errorCode);
      }
    
      public void setPrepareErrorCode(int errorCode) {
        this.prepareErrorCode = errorCode;
      }
    
      private void setHeuristic(int errorCode) {
          if (errorCode == XAException.XA_HEURCOM ||
                errorCode == XAException.XA_HEURHAZ || 
                errorCode == XAException.XA_HEURMIX ||
                errorCode == XAException.XA_HEURRB) {
             _isHeuristic = true;
          }
      }

      public void commit(Xid xid, boolean onePhase) throws XAException{
        // test goes here
        System.out.println("in XA commit");
        commit_status = getStatus("COMMIT");
        if (commitErrorCode != 9999) {
          System.out.println("throwing XAException." + commitErrorCode + " during commit of " + (onePhase? "1" : "2") + "pc");
          throw new XAException(commitErrorCode);
        }
      }
    
      public boolean isSameRM(XAResource xaresource)
        throws XAException {
          return xaresource == this || this.id == ((TestResource)xaresource).id;
      }
    
    
      public void rollback(Xid xid)
            throws XAException {
          System.out.println("in XA rollback");
        rollback_status = getStatus("ROLLBACK");
        if (rollbackErrorCode != 9999) {
          System.out.println("throwing XAException." + rollbackErrorCode + " during rollback" );
          throw new XAException(rollbackErrorCode);
        }
      }
    
      public int prepare(Xid xid)
            throws XAException {
          System.out.println("in XA prepare");
        prepare_status = getStatus("PREPARE");
        if (prepareErrorCode != 9999) {
          System.out.println("throwing XAException." + prepareErrorCode + " during prepare" );
          throw new XAException(prepareErrorCode);
        }
          return XAResource.XA_OK;
      }
    
      public boolean setTransactionTimeout(int i)
            throws XAException {
          return true;
       }
    
       public int getTransactionTimeout()
            throws XAException {
            return 0;
        }
       public void forget(Xid xid)
            throws XAException {
            _forgetCalled = true;
            inUse = false;
        }
    
       public void start(Xid xid, int flags)
            throws XAException {
              if (inUse)
                throw new XAException(XAException.XAER_NOTA);
              inUse = true;
              if (startErrorCode != 9999) {
                System.out.println("throwing XAException." + startErrorCode + " during start" );
                throw new XAException(startErrorCode);
              }
       }
    
    
         public void end(Xid xid, int flags)
            throws XAException {
              inUse = false;
        }
    
    
       public Xid[] recover(int flags)
            throws XAException {
            return null;
        }

       public boolean forgetCalled() {
            return !_isHeuristic || _forgetCalled;
       }

       public boolean commitStatusOK() {
            return commit_status==Status.STATUS_COMMITTING;
       }

       public boolean rollbackStatusOK() {
            return rollback_status==Status.STATUS_ROLLING_BACK;
       }

       public boolean prepareStatusOK() {
            return prepare_status==Status.STATUS_PREPARING;
       }

       public boolean isAssociated() {
            return inUse;
       }

       private int getStatus(String name) {
        int status = -1;
        try {
            if (tx != null) {
                status = tx.getStatus();
                System.out.println("Status in " + name + ": " + JavaEETransactionManagerSimplified.getStatusAsString(status));
            }
         } catch (Exception ex) {
            ex.printStackTrace();
         }

         return status;
       }
    
    }
  
    static class TestResourceHandle extends ResourceHandle {
        private XAResource resource;
        private static PoolManagerImpl poolMgr = new PoolManagerImpl(); 

        public TestResourceHandle(XAResource resource) {
          super(null,new ResourceSpec("testResource",0) ,null,null);
          this.resource = resource;
        }
  
        public boolean isTransactional() {
          return true;
        }
  
        public boolean isShareable() {
          return true;
        }
  
        public boolean supportsXA() {
          return true;
        }
         
        public ResourceAllocator getResourceAllocator() {
          return null;
        }
  
        public Object getResource() {
          return resource;
        }
  
        public XAResource getXAResource() {
          return resource;
        }
  
        public Object getUserConnection() {
          return null;
        }
  
        public ClientSecurityInfo getClientSecurityInfo() {
          return null;
        }
  
        public void fillInResourceObjects(Object userConnection, XAResource xares) {
        }
  
        public void enlistedInTransaction(Transaction tran) throws IllegalStateException {
            poolMgr.resourceEnlisted(tran, this);
        }
    }

}
