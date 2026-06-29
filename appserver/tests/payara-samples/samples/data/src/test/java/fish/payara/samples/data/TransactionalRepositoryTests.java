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

import fish.payara.samples.data.entity.Employee;
import fish.payara.samples.data.repo.Employees;
import fish.payara.samples.data.repo.TransactionalEmployees;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionalException;
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
 * Verifies the spec rule that {@code jakarta.transaction.Transactional} is
 * inherited by the repository implementation and that its semantics are
 * enforced by Jakarta Interceptors when CDI + Jakarta Transactions are
 * available.
 *
 * <p>All operations here go through {@link TransactionalEmployees}, which
 * declares {@code @Transactional(REQUIRED)} at the interface level and
 * REQUIRES_NEW / MANDATORY / NEVER on individual methods.
 */
@RunWith(Arquillian.class)
public class TransactionalRepositoryTests {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "transactional-repository-tests.war")
                .addPackages(true, "fish.payara.samples.data.entity")
                .addPackages(true, "fish.payara.samples.data.repo")
                .addAsResource("META-INF/persistence.xml")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @PersistenceContext(unitName = "samples-dataPU")
    private EntityManager em;

    @Resource
    private UserTransaction utx;

    @Inject
    private TransactionalEmployees txEmployees;

    @Inject
    private Employees employees;

    @After
    public void rollbackLeftovers() throws Exception {
        if (utx.getStatus() == Status.STATUS_ACTIVE
                || utx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            utx.rollback();
        }
    }

    /**
     * No active caller transaction → interface-level @Transactional(REQUIRED)
     * should cause the interceptor to start a new transaction for the call,
     * then commit it on return. We verify visibility from a plain caller after
     * the call returns (no manual UserTransaction).
     */
    @Test
    public void interfaceLevelRequiredCreatesTransactionWhenNoneActive() throws Exception {
        assertEquals("precondition: no caller transaction",
                Status.STATUS_NO_TRANSACTION, utx.getStatus());

        UUID id = UUID.randomUUID();
        txEmployees.save(newEmployee(id, "Auto", "Required"));

        // Read back outside a UserTransaction. This depends on JTA enlistment
        // *and* on the @Transactional interceptor having committed.
        utx.begin();
        try {
            assertTrue(
                    "Interface-level @Transactional(REQUIRED) should have created and "
                            + "committed a new transaction wrapping the save() call.",
                    txEmployees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    /**
     * REQUIRES_NEW on a query method: even when called inside an active caller
     * transaction that is later rolled back, the inner read participates in
     * its own (committed) transaction. Combined with a write whose surrounding
     * tx is rolled back, this proves the inner method ran on a different tx
     * boundary.
     *
     * <p><strong>Currently ignored:</strong> same provider behaviour as the
     * MANDATORY / NEVER cases below — method-level
     * {@code @Transactional(REQUIRES_NEW)} is not enforced by the current
     * Payara Jakarta Data provider; only interface-level {@code @Transactional}
     * is applied, so the caller transaction is not suspended and the inner
     * read sees the uncommitted write. Re-enable once the provider applies
     * method-level overrides.
     */
    @Test
    public void requiresNewSuspendsCallerTransaction() throws Exception {
        UUID stableId = UUID.randomUUID();

        // Persist one Employee with active=true in a committed tx.
        utx.begin();
        employees.save(newEmployee(stableId, "Outer", "Committed", true));
        utx.commit();

        // Now begin a new caller tx that we will roll back. Inside it, write
        // a second active Employee, then call findByActive(true) which is
        // REQUIRES_NEW. The inner read uses its own tx and must see the
        // committed entity but NOT the uncommitted one (because the caller's
        // tx is suspended for the duration of the inner call).
        UUID volatileId = UUID.randomUUID();
        utx.begin();
        try {
            employees.save(newEmployee(volatileId, "Outer", "Volatile", true));

            var seen = txEmployees.findByActive(true);
            boolean sawCommitted = seen.stream().anyMatch(e -> e.id.equals(stableId));
            boolean sawVolatile = seen.stream().anyMatch(e -> e.id.equals(volatileId));

            assertTrue("REQUIRES_NEW read must see committed data", sawCommitted);
            assertFalse(
                    "REQUIRES_NEW read must NOT see uncommitted writes from the suspended "
                            + "caller transaction; if this fails, the interceptor did not suspend.",
                    sawVolatile);
        } finally {
            utx.rollback();
        }
    }

    /**
     * MANDATORY without an active transaction must throw TransactionalException.
     *
     * <p><strong>Currently ignored:</strong> the Payara Jakarta Data provider
     * does not seem to apply method-level {@code @Transactional(MANDATORY)} on
     * generated repository proxy methods — the call returns the query result
     * normally instead of failing. Interface-level {@code @Transactional} is
     * applied (covered by the other tests in this class). Re-enable once the
     * provider applies method-level overrides.
     */
    @Test
    public void mandatoryWithoutActiveTransactionFails() throws Exception {
        assertEquals(Status.STATUS_NO_TRANSACTION, utx.getStatus());
        try {
            long count = txEmployees.countByLastName("Mandatory");
            fail("Expected TransactionalException, got count=" + count);
        } catch (TransactionalException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    /**
     * MANDATORY with an active transaction must succeed and run inside that
     * caller-provided transaction (no new one created).
     */
    @Test
    public void mandatoryParticipatesInCallerTransaction() throws Exception {
        utx.begin();
        try {
            // No employees match yet; the call should simply return 0.
            long count = txEmployees.countByLastName("DoesNotExist-" + UUID.randomUUID());
            assertEquals(0L, count);

            assertEquals("MANDATORY must not commit caller's tx",
                    Status.STATUS_ACTIVE, utx.getStatus());
        } finally {
            utx.rollback();
        }
    }

    /**
     * NEVER with an active transaction must throw TransactionalException.
     *
     * <p><strong>Currently ignored:</strong> same provider behaviour as the
     * MANDATORY case above — method-level {@code @Transactional(NEVER)} is not
     * enforced by the current Payara Jakarta Data provider; only interface-level
     * {@code @Transactional} is applied. Re-enable once the provider applies
     * method-level overrides.
     */
    @Test
    public void neverInsideActiveTransactionFails() throws Exception {
        utx.begin();
        try {
            try {
                boolean exists = txEmployees.existsByEmail("x@y");
                fail("Expected TransactionalException, got exists=" + exists);
            } catch (TransactionalException expected) {
                assertNotNull(expected.getMessage());
            }
        } finally {
            utx.rollback();
        }
    }

    /**
     * Default-inherited REQUIRED method joins the caller's transaction and
     * its writes commit/rollback with that caller transaction.
     */
    @Test
    public void inheritedRequiredJoinsCallerAndRollsBackWithIt() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        try {
            txEmployees.save(newEmployee(id, "Joined", "Caller"));
            assertEquals("REQUIRED must join — not start a new — caller transaction",
                    Status.STATUS_ACTIVE, utx.getStatus());
        } finally {
            utx.rollback();
        }

        utx.begin();
        try {
            assertFalse(
                    "If REQUIRED started a new tx instead of joining, the rollback above "
                            + "would not have discarded the save().",
                    txEmployees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    private static Employee newEmployee(UUID id, String first, String last) {
        return newEmployee(id, first, last, true);
    }

    private static Employee newEmployee(UUID id, String first, String last, boolean active) {
        return Employee.of(id, first, last,
                first.toLowerCase() + "." + last.toLowerCase() + "@x",
                null, null, "Engineer", new BigDecimal("80000.00"), active, LocalDate.now());
    }
}
