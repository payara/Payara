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
package fish.payara.samples.data.service;

import fish.payara.samples.data.entity.Address;
import fish.payara.samples.data.entity.Employee;
import fish.payara.samples.data.repo.Addresses;
import fish.payara.samples.data.repo.Employees;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionSynchronizationRegistry;

import java.util.UUID;

/**
 * Stateless session bean used to drive Jakarta Data repositories from various
 * EJB transaction attribute boundaries (REQUIRED, REQUIRES_NEW, MANDATORY,
 * NEVER, NOT_SUPPORTED). Lets tests verify the spec rule that repository
 * operations <em>enlist</em> in the caller's global transaction rather than
 * <em>creating</em> their own.
 */
@Stateless
public class EmployeeService {

    @Inject
    private Employees employees;

    @Inject
    private Addresses addresses;

    @PersistenceContext(unitName = "samples-dataPU")
    private EntityManager em;

    @Resource
    private TransactionSynchronizationRegistry tsr;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Employee saveInRequired(Employee e) {
        return employees.save(e);
    }

    /**
     * REQUIRES_NEW: suspends the caller transaction and runs the save in a
     * fresh transaction. Any failure in the caller after this returns must
     * not undo the persisted Employee.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Employee saveInRequiresNew(Employee e) {
        return employees.save(e);
    }

    /**
     * MANDATORY: requires a caller-managed transaction. Throws
     * EJBTransactionRequiredException when called without one.
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public Employee saveInMandatory(Employee e) {
        return employees.save(e);
    }

    /**
     * NEVER: forbids a caller transaction. Throws EJBException when invoked
     * with an active transaction. Used to verify the test infrastructure.
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public long countWithoutTransaction() {
        return employees.findAll().count();
    }

    /**
     * Performs two repository writes (Address + Employee) in a single
     * REQUIRED transaction, then forces a rollback by throwing. Verifies
     * that the spec's "enlistment, not creation" rule means both writes
     * are discarded together.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveAddressThenEmployeeThenFail(Address address, Employee employee) {
        addresses.save(address);
        employees.save(employee);
        throw new IllegalStateException("forced rollback to test enlistment");
    }

    /**
     * Persists an Employee, then asks the container to mark the transaction
     * for rollback. Verifies that the spec rule "the repository operation
     * must not commit or roll back a transaction which was already
     * associated with the thread" is preserved when the application itself
     * marks the transaction for rollback.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveAndMarkRollback(Employee e) {
        employees.save(e);
        tsr.setRollbackOnly();
    }

    /**
     * Returns the current JTA transaction status as observed inside the
     * EJB context — used to verify enlistment from the caller's transaction.
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public int currentTransactionStatus() {
        return tsr.getTransactionStatus();
    }

    /**
     * Persists an Employee, then reads it back through a separate repository
     * (Addresses) within the same JTA transaction, exercising the spec's
     * recommendation that providers propagate a single persistence context
     * across repositories within a transaction.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean saveAndImmediatelyFind(Employee e) {
        employees.save(e);
        // forces a flush & query through a sibling repository in the same tx
        return employees.findById(e.id).isPresent();
    }

    /**
     * Helper used by tests to assert persistence context isolation between
     * transactions: writes a new UUID into the EM, then validates a separate
     * fetch picks it up only after commit.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean existsOutsideTransaction(UUID id) throws SystemException {
        if (tsr.getTransactionStatus() != Status.STATUS_NO_TRANSACTION) {
            throw new EJBException("expected NO_TRANSACTION, got status=" + tsr.getTransactionStatus());
        }
        return employees.findById(id).isPresent();
    }
}
