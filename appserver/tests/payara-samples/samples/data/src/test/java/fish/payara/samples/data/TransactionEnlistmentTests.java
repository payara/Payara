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
import fish.payara.samples.data.entity.Company;
import fish.payara.samples.data.entity.Employee;
import fish.payara.samples.data.repo.Addresses;
import fish.payara.samples.data.repo.Companies;
import fish.payara.samples.data.repo.Employees;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies the JTA enlistment guarantees that the Jakarta Data specification
 * places on repository operations (Jakarta EE integration section).
 *
 * <p>The spec only mandates <em>enlistment</em> — i.e. when a global
 * transaction is already active on the calling thread, the data source backing
 * the repository must enlist in it. The spec does <em>not</em> mandate the
 * implicit <em>creation</em> of a new transaction by repository operations.
 *
 * <p>Each test isolates one of the following invariants:
 * <ol>
 *     <li>Repository writes commit when the caller-driven transaction commits.</li>
 *     <li>Repository writes are discarded when the caller-driven transaction rolls back.</li>
 *     <li>The repository operation does <em>not</em> commit the caller's transaction.</li>
 *     <li>The repository operation does <em>not</em> roll back the caller's transaction.</li>
 *     <li>Multiple repositories rollback atomically with a caller-driven rollback.</li>
 *     <li>Multiple repositories commit atomically with a caller-driven commit.</li>
 *     <li>A repository write is visible via the shared transaction-scoped persistence context.</li>
 * </ol>
 */
@RunWith(Arquillian.class)
public class TransactionEnlistmentTests {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "transaction-enlistment-tests.war")
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

    @Resource
    private TransactionSynchronizationRegistry tsr;

    @Inject
    private Employees employees;

    @Inject
    private Addresses addresses;

    @Inject
    private Companies companies;

    @After
    public void rollbackLeftovers() throws Exception {
        if (utx.getStatus() == Status.STATUS_ACTIVE
                || utx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            utx.rollback();
        }
    }

    @Test
    public void repositoryWritesEnlistAndCommitWithCallerTransaction() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        employees.save(newEmployee(id, "Enlist", "Commit"));
        utx.commit();

        utx.begin();
        try {
            assertTrue("Employee must be visible after caller commit",
                    employees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    @Test
    public void repositoryWritesAreDiscardedWhenCallerRollsBack() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        employees.save(newEmployee(id, "Enlist", "Rollback"));
        utx.rollback();

        utx.begin();
        try {
            assertFalse(
                    "Repository write must roll back together with the caller transaction. "
                            + "If this fails, the data source is not enlisted (auto-commit).",
                    employees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    @Test
    public void repositoryDoesNotCommitTheCallerTransaction() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        try {
            employees.save(newEmployee(id, "Caller", "Owns"));

            assertEquals(
                    "Spec: 'The repository operation must not commit ... a transaction which "
                            + "was already associated with the thread'. Status must remain ACTIVE.",
                    Status.STATUS_ACTIVE, utx.getStatus());
        } finally {
            utx.rollback();
        }

        // The save above was inside an active tx that we rolled back ourselves.
        utx.begin();
        try {
            assertFalse(
                    "If the repository had silently committed, the entity would still exist.",
                    employees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    @Test
    public void repositoryDoesNotRollBackTheCallerTransaction() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        employees.save(newEmployee(id, "Caller", "Decides"));

        assertEquals(
                "Spec: repository must not roll back caller's tx — status must still be ACTIVE.",
                Status.STATUS_ACTIVE, utx.getStatus());

        // Caller decides to commit — the repository must not have rolled back.
        utx.commit();

        utx.begin();
        try {
            assertTrue(employees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    /**
     * All writes to multiple repositories in one JTA transaction must be
     * discarded together when the caller rolls back.
     */
    @Test
    public void multipleRepositoriesRollbackAtomically() throws Exception {
        UUID addrId = UUID.randomUUID();
        UUID compId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();

        utx.begin();
        addresses.save(Address.of(addrId, "1 Spec St", "Rollbackville", "ZZ", "00000", "ZZ"));
        companies.save(newCompany(compId, "Spec Inc."));
        employees.save(newEmployee(empId, "Cross", "Repo"));
        utx.rollback();

        utx.begin();
        try {
            assertFalse("Address write must be enlisted with the rolled-back tx",
                    addresses.findById(addrId).isPresent());
            assertFalse("Company write must be enlisted with the rolled-back tx",
                    companies.findById(compId).isPresent());
            assertFalse("Employee write must be enlisted with the rolled-back tx",
                    employees.findById(empId).isPresent());
        } finally {
            utx.rollback(); // read-only verify — rollback avoids cascading RollbackException
        }
    }

    /**
     * All writes to multiple repositories in one JTA transaction must be
     * committed together when the caller commits.
     */
    @Test
    public void multipleRepositoriesCommitAtomically() throws Exception {
        UUID addrId = UUID.randomUUID();
        UUID compId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();

        utx.begin();
        addresses.save(Address.of(addrId, "2 Spec St", "Commitville", "ZZ", "00000", "ZZ"));
        companies.save(newCompany(compId, "Spec LLC"));
        employees.save(newEmployee(empId, "Multi", "Commit"));
        utx.commit();

        utx.begin();
        try {
            assertTrue("Address write must be visible after commit",
                    addresses.findById(addrId).isPresent());
            assertTrue("Company write must be visible after commit",
                    companies.findById(compId).isPresent());
            assertTrue("Employee write must be visible after commit",
                    employees.findById(empId).isPresent());
        } finally {
            utx.rollback(); // read-only verify
        }
    }

    @Test
    public void repositoryWriteIsVisibleViaSharedPersistenceContext() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        try {
            employees.save(newEmployee(id, "Read", "Own"));
            // JPA §7.6.3: all container-managed transaction-scoped EntityManagers
            // for the same persistence unit within the same JTA transaction share
            // one underlying persistence context (first-level / identity-map cache).
            // em.find() must return the entity the repository just enlisted —
            // no DB flush is required for an identity-map lookup.
            Employee found = em.find(Employee.class, id);
            assertNotNull(
                    "Repository save() must enlist in the caller's JTA transaction. "
                            + "em.find() within the same tx must return the entity from "
                            + "the shared transaction-scoped persistence context.",
                    found);
            assertEquals("Read", found.firstName);
        } finally {
            utx.rollback();
        }
    }

    /**
     * Spec: "A persistence context is never shared across transactions."
     * Once the writing tx commits, a fresh tx must observe the entity through
     * a brand-new persistence context, not a stale one.
     */
    @Test
    public void readsInANewTransactionUseAFreshPersistenceContext() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        employees.save(newEmployee(id, "Fresh", "Context"));
        utx.commit();

        // Fresh transaction => fresh PC
        utx.begin();
        try {
            Employee e = employees.findById(id).orElseThrow(() -> new AssertionError("not found"));
            assertNotNull(e.id);
            assertEquals("Fresh", e.firstName);
        } finally {
            utx.commit();
        }
    }

    /**
     * Jakarta Data's {@code BasicRepository.save()} is defined as an
     * <em>upsert</em>, so saving a second entity with an existing id quietly
     * updates the row instead of failing. Verify that semantic — and verify
     * that the upsert still rolls back together with a caller-driven rollback,
     * leaving the previously-committed row intact.
     */
    @Test
    public void upsertWithExistingIdRollsBackWithCallerTransaction() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        employees.save(newEmployee(id, "First", "Insert"));
        utx.commit();

        utx.begin();
        // save() is an upsert — this updates the row in-memory; rollback must discard it.
        employees.save(newEmployee(id, "Second", "Upsert"));
        utx.rollback();

        utx.begin();
        try {
            Employee e = employees.findById(id).orElseThrow(
                    () -> new AssertionError("First insert was unexpectedly removed"));
            assertEquals(
                    "Upsert in a rolled-back tx must leave the previously-committed value intact",
                    "First", e.firstName);
        } finally {
            utx.commit();
        }
    }

    /**
     * The transaction key reported by {@link TransactionSynchronizationRegistry}
     * before and after a repository write must be identical. This pins down the
     * spec invariant that the repository <em>enlists in</em> the caller's
     * transaction rather than substituting a new one of its own.
     */
    @Test
    public void repositoryEnlistsInTheSameTransactionAsTheCaller() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        try {
            Object keyBefore = tsr.getTransactionKey();
            assertNotNull("Caller began a tx — TSR must expose a non-null key", keyBefore);
            assertEquals(Status.STATUS_ACTIVE, tsr.getTransactionStatus());

            employees.save(newEmployee(id, "Same", "Tx"));

            Object keyAfter = tsr.getTransactionKey();
            assertNotNull(keyAfter);
            assertEquals(
                    "Spec invariant: repository must enlist in the caller's transaction, "
                            + "not switch the thread to a different one. TSR transaction key "
                            + "must remain identical across the repository call.",
                    keyBefore, keyAfter);
            assertEquals("Caller tx must remain ACTIVE",
                    Status.STATUS_ACTIVE, tsr.getTransactionStatus());
        } finally {
            utx.commit();
        }
    }

    /**
     * A {@link Synchronization} registered on the caller's transaction must be
     * the one whose {@code afterCompletion} fires at commit time, with status
     * COMMITTED. This is end-to-end proof that the repository write rode the
     * exact transaction we registered against — not a separately created one.
     */
    @Test
    public void synchronizationRegisteredOnCallerTxObservesCommit() throws Exception {
        UUID id = UUID.randomUUID();
        AtomicInteger beforeCalls = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();
        AtomicReference<Integer> afterStatus = new AtomicReference<>();
        AtomicReference<Object> txKeyAtBefore = new AtomicReference<>();

        utx.begin();
        try {
            Object callerKey = tsr.getTransactionKey();

            tsr.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    beforeCalls.incrementAndGet();
                    txKeyAtBefore.set(tsr.getTransactionKey());
                }

                @Override
                public void afterCompletion(int status) {
                    afterCalls.incrementAndGet();
                    afterStatus.set(status);
                }
            });

            employees.save(newEmployee(id, "Sync", "Commit"));

            utx.commit();

            assertEquals("beforeCompletion must fire exactly once on commit",
                    1, beforeCalls.get());
            assertEquals("afterCompletion must fire exactly once on commit",
                    1, afterCalls.get());
            assertEquals(
                    "Spec invariant: the repository write must run on the same tx the "
                            + "synchronization was registered on — afterCompletion(COMMITTED).",
                    Integer.valueOf(Status.STATUS_COMMITTED), afterStatus.get());
            assertEquals(
                    "TSR key observed inside beforeCompletion must match the caller's tx key.",
                    callerKey, txKeyAtBefore.get());
        } catch (RuntimeException re) {
            if (utx.getStatus() == Status.STATUS_ACTIVE
                    || utx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                utx.rollback();
            }
            throw re;
        }

        // And the entity must be visible to a brand-new tx.
        utx.begin();
        try {
            assertTrue(employees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    /**
     * Mirror of the commit case: a Synchronization registered on the caller's
     * tx receives {@code afterCompletion(STATUS_ROLLEDBACK)} when the caller
     * rolls back, proving once more the repository never silently switched
     * to another transaction.
     */
    @Test
    public void synchronizationRegisteredOnCallerTxObservesRollback() throws Exception {
        UUID id = UUID.randomUUID();
        AtomicInteger afterCalls = new AtomicInteger();
        AtomicReference<Integer> afterStatus = new AtomicReference<>();

        utx.begin();
        try {
            tsr.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    // not expected on rollback
                }

                @Override
                public void afterCompletion(int status) {
                    afterCalls.incrementAndGet();
                    afterStatus.set(status);
                }
            });

            employees.save(newEmployee(id, "Sync", "Rollback"));
        } finally {
            utx.rollback();
        }

        assertEquals(1, afterCalls.get());
        assertEquals(
                "afterCompletion must report STATUS_ROLLEDBACK on caller rollback",
                Integer.valueOf(Status.STATUS_ROLLEDBACK), afterStatus.get());

        utx.begin();
        try {
            assertFalse("Repository write must have been enlisted with the rolled-back tx",
                    employees.findById(id).isPresent());
        } finally {
            utx.commit();
        }
    }

    private static Employee newEmployee(UUID id, String first, String last) {
        return Employee.of(id, first, last, first.toLowerCase() + "." + last.toLowerCase() + "@x",
                null, null, "Engineer", new BigDecimal("75000.00"), true, LocalDate.now());
    }

    private static Company newCompany(UUID id, String name) {
        return Company.of(id, name, "Tech", true, UUID.randomUUID(), Instant.now());
    }
}
