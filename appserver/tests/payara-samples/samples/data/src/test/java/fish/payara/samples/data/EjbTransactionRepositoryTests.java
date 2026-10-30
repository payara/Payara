/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 */
package fish.payara.samples.data;

import fish.payara.samples.data.entity.Address;
import fish.payara.samples.data.entity.Employee;
import fish.payara.samples.data.repo.Addresses;
import fish.payara.samples.data.repo.Employees;
import fish.payara.samples.data.service.EmployeeService;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.EJBTransactionRequiredException;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies that Jakarta Data repositories enlist correctly when invoked from
 * EJB transactional boundaries — the most common Jakarta EE topology.
 *
 * <p>Spec emphasis: repositories must <em>enlist</em> in caller-managed
 * transactions; they must not commit nor roll back transactions they did not
 * create. EJB transaction attributes drive the boundaries here.
 */
@RunWith(Arquillian.class)
public class EjbTransactionRepositoryTests {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "ejb-transaction-repository-tests.war")
                .addPackages(true, "fish.payara.samples.data.entity")
                .addPackages(true, "fish.payara.samples.data.repo")
                .addPackages(true, "fish.payara.samples.data.service")
                .addAsResource("META-INF/persistence.xml")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @PersistenceContext(unitName = "samples-dataPU")
    private EntityManager em;

    @Resource
    private UserTransaction utx;

    @Inject
    private EmployeeService service;

    @Inject
    private Employees employees;

    @Inject
    private Addresses addresses;

    @After
    public void rollbackLeftovers() throws Exception {
        if (utx.getStatus() == Status.STATUS_ACTIVE
                || utx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            utx.rollback();
        }
    }

    /**
     * Repository called from an EJB REQUIRED method: the method establishes
     * the transaction (because none is active on call), and the repository
     * write commits with it.
     */
    @Test
    public void ejbRequiredEstablishesTransactionForRepositoryWrite() throws Exception {
        UUID id = UUID.randomUUID();
        service.saveInRequired(newEmployee(id, "Ejb", "Required"));

        utx.begin();
        try {
            assertTrue(employees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    /**
     * Repository write inside an EJB REQUIRED method that throws after the
     * write must roll back. This is the canonical "all-or-nothing" check the
     * spec implies: repository is enlisted, EJB drives commit/rollback.
     */
    @Test
    public void ejbRequiredRollsBackBothRepositoryWritesAtomically() throws Exception {
        UUID addrId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        Address a = Address.of(addrId, "1 Atomic Way", "Town", "ST", "12345", "ZZ");
        Employee e = newEmployee(empId, "Atomic", "Rollback");

        try {
            service.saveAddressThenEmployeeThenFail(a, e);
            fail("EJB method was supposed to throw");
        } catch (EJBException expected) {
            // pass
        }

        utx.begin();
        try {
            assertFalse("Address must be rolled back with the EJB transaction",
                    addresses.findById(addrId).isPresent());
            assertFalse("Employee must be rolled back with the EJB transaction",
                    employees.findById(empId).isPresent());
        } finally {
            utx.commit();
        }
    }

    /**
     * REQUIRES_NEW: outer caller's transaction is suspended; the inner save
     * commits independently. After the call, the caller's transaction can
     * still be rolled back without affecting the inner write.
     */
    @Test
    public void ejbRequiresNewCommitsIndependentlyOfCallerRollback() throws Exception {
        UUID innerId = UUID.randomUUID();
        UUID outerId = UUID.randomUUID();

        utx.begin();
        try {
            // Outer write — will be rolled back at the end of this block.
            employees.save(newEmployee(outerId, "Outer", "Doomed"));

            // Inner save in a brand-new tx that commits before returning.
            service.saveInRequiresNew(newEmployee(innerId, "Inner", "Survivor"));

            assertEquals("Caller tx must be reinstated as ACTIVE after REQUIRES_NEW returns",
                    Status.STATUS_ACTIVE, utx.getStatus());
        } finally {
            utx.rollback();
        }

        utx.begin();
        try {
            assertTrue("REQUIRES_NEW write must survive caller rollback",
                    employees.findById(innerId).isPresent());
            assertFalse("Outer write must NOT survive caller rollback",
                    employees.findById(outerId).isPresent());
        } finally {
            utx.commit();
        }
    }

    /**
     * MANDATORY without an active caller transaction must fail with
     * EJBTransactionRequiredException.
     */
    @Test
    public void ejbMandatoryWithoutCallerTransactionFails() throws Exception {
        assertEquals(Status.STATUS_NO_TRANSACTION, utx.getStatus());
        try {
            service.saveInMandatory(newEmployee(UUID.randomUUID(), "Mandatory", "Fail"));
            fail("Expected EJBTransactionRequiredException");
        } catch (EJBTransactionRequiredException expected) {
            assertNotNull(expected);
        }
    }

    /**
     * MANDATORY with an active caller tx joins it and the repository write
     * commits on caller commit.
     */
    @Test
    public void ejbMandatoryJoinsCallerTransaction() throws Exception {
        UUID id = UUID.randomUUID();
        utx.begin();
        try {
            service.saveInMandatory(newEmployee(id, "Mandatory", "Joined"));
            // Caller decides to commit:
            utx.commit();
        } catch (RuntimeException re) {
            if (utx.getStatus() == Status.STATUS_ACTIVE
                    || utx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                utx.rollback();
            }
            throw re;
        }

        utx.begin();
        try {
            assertTrue(employees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    /**
     * Application-driven setRollbackOnly inside the EJB tx must cause the
     * repository write to be discarded. Payara's container performs the
     * rollback silently when the EJB method returns normally; the spec only
     * mandates the rollback effect, not that an EJBTransactionRolledbackException
     * be surfaced when the application code itself completed without throwing.
     */
    @Test
    public void setRollbackOnlyInsideEjbDiscardsRepositoryWrite() throws Exception {
        UUID id = UUID.randomUUID();
        try {
            service.saveAndMarkRollback(newEmployee(id, "MarkedFor", "Rollback"));
        } catch (EJBException acceptable) {
            // Some containers do throw EJBTransactionRolledbackException — also valid.
        }

        utx.begin();
        try {
            assertFalse(
                    "setRollbackOnly inside the EJB tx must discard the repository write",
                    employees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    /**
     * NEVER with an active caller tx must fail. Verifies that test-side
     * transaction propagation actually reaches the EJB layer.
     */
    @Test
    public void ejbNeverInsideCallerTransactionFails() throws Exception {
        utx.begin();
        try {
            try {
                long c = service.countWithoutTransaction();
                fail("Expected EJBException for NEVER inside an active caller tx, got count=" + c);
            } catch (EJBException expected) {
                // pass
            }
        } finally {
            utx.rollback();
        }
    }

    /**
     * NOT_SUPPORTED: the caller's transaction is suspended. The repository read
     * inside the EJB must observe a Status.STATUS_NO_TRANSACTION context and
     * still return the previously committed entity.
     */
    @Test
    public void notSupportedSuspendsCallerForRepositoryRead() throws Exception {
        UUID id = UUID.randomUUID();
        utx.begin();
        employees.save(newEmployee(id, "Suspended", "Read"));
        utx.commit();

        utx.begin();
        try {
            // Add a new not-yet-committed write
            UUID volatileId = UUID.randomUUID();
            employees.save(newEmployee(volatileId, "Volatile", "InCaller"));

            boolean foundCommitted = service.existsOutsideTransaction(id);
            boolean foundVolatile = service.existsOutsideTransaction(volatileId);

            assertTrue("Committed entity must be visible through suspended-tx read",
                    foundCommitted);
            assertFalse(
                    "Uncommitted caller-tx writes must NOT be visible to a NOT_SUPPORTED read",
                    foundVolatile);
        } finally {
            utx.rollback();
        }
    }

    /**
     * Inside an EJB MANDATORY method, the transaction status observed by the
     * EJB code must be ACTIVE, proving the caller's UserTransaction propagated
     * into the EJB call. This pins down the "enlistment, not creation" rule:
     * there is exactly one transaction across the whole call chain.
     */
    @Test
    public void callerTransactionPropagatesIntoMandatoryEjb() throws Exception {
        utx.begin();
        try {
            int innerStatus = service.currentTransactionStatus();
            assertEquals("EJB must observe the caller's ACTIVE transaction",
                    Status.STATUS_ACTIVE, innerStatus);
            assertEquals("Caller tx must remain ACTIVE on return",
                    Status.STATUS_ACTIVE, utx.getStatus());
        } finally {
            utx.rollback();
        }
    }

    private static Employee newEmployee(UUID id, String first, String last) {
        return Employee.of(id, first, last,
                first.toLowerCase() + "." + last.toLowerCase() + "@x",
                null, null, "Engineer", new BigDecimal("90000.00"), true, LocalDate.now());
    }
}
