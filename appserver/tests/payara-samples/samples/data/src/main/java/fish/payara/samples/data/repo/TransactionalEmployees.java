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
package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Employee;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Jakarta Data repository for Employee with {@code @Transactional} declared at the
 * <strong>interface level</strong> (REQUIRED) and per-method overrides for
 * REQUIRES_NEW, MANDATORY and NEVER.
 *
 * <p>According to the Jakarta Data specification (Jakarta EE integration):
 * <em>"In the Jakarta EE environment, the @Transactional annotation is automatically
 * inherited by the repository implementation from the user-written repository
 * interface, and the semantics of the @Transactional annotation are applied
 * automatically by the implementation of Jakarta Interceptors supplied by the
 * Jakarta EE container."</em>
 *
 * <p>Note: query methods are declared here (instead of overriding inherited
 * {@code BasicRepository} methods) so that the provider generates concrete
 * proxy methods that the {@code @Transactional} interceptor can intercept
 * unambiguously.
 */
@Repository
@Transactional(Transactional.TxType.REQUIRED)
public interface TransactionalEmployees extends BasicRepository<Employee, UUID> {

    List<Employee> findByLastName(String lastName);

    long countByActive(boolean active);

    /**
     * REQUIRES_NEW override. Must suspend any caller transaction and execute
     * within a new, independent transaction.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    List<Employee> findByActive(boolean active);

    /**
     * MANDATORY override. Must throw TransactionalException when invoked
     * outside any active transaction.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    long countByLastName(String lastName);

    /**
     * NEVER override. Must throw TransactionalException when invoked while
     * a transaction is already active on the calling thread.
     */
    @Transactional(Transactional.TxType.NEVER)
    boolean existsByEmail(String email);
}
