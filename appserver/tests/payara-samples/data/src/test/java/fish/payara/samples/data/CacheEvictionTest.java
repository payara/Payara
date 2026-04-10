/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2025-2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.samples.data;

import fish.payara.samples.data.entity.Employee;
import fish.payara.samples.data.repo.Employees;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Tests for Jakarta Data L2 cache eviction behavior.
 *
 * Tests verify that mutation operations (INSERT, UPDATE, DELETE) properly
 * evict affected entities from cache without clearing unrelated entities.
 *
 * Related Issue: FISH-11607
 */
@RunWith(Arquillian.class)
public class CacheEvictionTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "cache-eviction-test.war")
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
    private Employees employees;

    private Cache cache;

    @Before
    public void setUp() throws Exception {
        cache = em.getEntityManagerFactory().getCache();

        utx.begin();
        em.createQuery("DELETE FROM Employee").executeUpdate();
        utx.commit();

        cache.evictAll();
    }

    /**
     * ISSUE #1: Test that Query by Method Name DELETE only evicts deleted instances,
     * not all instances of the entity type.
     *
     * Before fix: deleteByFirstNameAndLastName() would call cache.evictAll()
     * clearing ALL Employee instances from cache.
     *
     * After fix: Only the deleted Employee instance(s) are evicted.
     */
    @Test
    public void testQueryByMethodNameDeleteEvictsOnlyDeletedInstances() throws Exception {
        // Create test employees
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        utx.begin();

        Employee emp1 = createEmployee(id1, "John", "Doe", "john@test.com");
        Employee emp2 = createEmployee(id2, "Jane", "Doe", "jane@test.com");
        Employee emp3 = createEmployee(id3, "Bob", "Smith", "bob@test.com");

        employees.save(emp1);
        employees.save(emp2);
        employees.save(emp3);

        utx.commit();

        // Load all into cache
        utx.begin();
        employees.findById(id1);
        employees.findById(id2);
        employees.findById(id3);
        utx.commit();

        // Verify all are in cache
        assertTrue("Employee 1 should be in cache", cache.contains(Employee.class, id1));
        assertTrue("Employee 2 should be in cache", cache.contains(Employee.class, id2));
        assertTrue("Employee 3 should be in cache", cache.contains(Employee.class, id3));

        // Delete one employee via Query by Method Name
        utx.begin();
        long deleted = employees.deleteByFirstNameAndLastName("John", "Doe");
        assertEquals("Should delete 1 employee", 1, deleted);
        utx.commit();

        // Verify: only deleted employee should be evicted from cache
        assertFalse("Deleted employee should NOT be in cache", cache.contains(Employee.class, id1));

        // CRITICAL: Other employees should STILL be in cache (this was the bug!)
        assertTrue("Unrelated employee 2 should STILL be in cache", cache.contains(Employee.class, id2));
        assertTrue("Unrelated employee 3 should STILL be in cache", cache.contains(Employee.class, id3));
    }

    /**
     * ISSUE #2: Test that DELETE via @Delete + @By annotation evicts deleted entities from cache.
     *
     * Before fix: DeleteOperationUtility did not call cache.evict() after deletion,
     * causing stale data reads.
     *
     * After fix: Deleted entities are properly evicted from cache.
     */
    @Test
    public void testDeleteByAnnotationEvictsFromCache() throws Exception {
        UUID id = UUID.randomUUID();

        utx.begin();
        Employee emp = createEmployee(id, "Alice", "Johnson", "alice@test.com");
        employees.save(emp);
        utx.commit();

        // Load into cache
        utx.begin();
        var found = employees.findById(id);
        assertTrue("Employee should be found", found.isPresent());
        utx.commit();

        // Verify in cache
        assertTrue("Employee should be in cache before delete", cache.contains(Employee.class, id));

        // Delete via @Delete + @By
        utx.begin();
        employees.delete(id);
        utx.commit();

        // Verify: deleted employee should be evicted from cache
        assertFalse("Deleted employee should NOT be in cache", cache.contains(Employee.class, id));
    }

    /**
     * ISSUE #2 Variant: Test that DELETE via @Query evicts cache.
     *
     * Before fix: QueryOperationUtility did not call cache.evict() after @Query DELETE,
     * causing stale data reads.
     *
     * After fix: Cache is properly evicted. Note: @Query operations evict the entire entity type
     * because we cannot determine which specific instances were affected by arbitrary JPQL queries.
     */
    @Test
    public void testDeleteViaQueryEvictsFromCache() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        utx.begin();
        Employee emp1 = createEmployee(id1, "Charlie", "Brown", "charlie@test.com");
        Employee emp2 = createEmployee(id2, "Diana", "Prince", "diana@test.com");
        employees.save(emp1);
        employees.save(emp2);
        utx.commit();

        // Load into cache
        utx.begin();
        employees.findById(id1);
        employees.findById(id2);
        utx.commit();

        // Verify in cache
        assertTrue("Employee 1 should be in cache", cache.contains(Employee.class, id1));
        assertTrue("Employee 2 should be in cache", cache.contains(Employee.class, id2));

        // Delete via @Query
        utx.begin();
        long deleted = employees.deleteByFirstName("Charlie");
        assertEquals("Should delete 1 employee", 1, deleted);
        utx.commit();

        // Verify: entire Employee cache should be evicted
        // Note: @Query operations clear the entire type because we can't determine
        // which instances were affected by the arbitrary JPQL query
        assertFalse("Deleted employee should NOT be in cache", cache.contains(Employee.class, id1));
        assertFalse("All Employee instances evicted (expected for @Query operations)", cache.contains(Employee.class, id2));
    }

    /**
     * ISSUE #3: Test that UPDATE via @Query evicts cache.
     *
     * Before fix: QueryOperationUtility did not call cache.evict() after @Query UPDATE,
     * causing subsequent reads to return stale (old) data from cache.
     *
     * After fix: Cache is properly evicted after UPDATE. Note: @Query operations evict
     * the entire entity type to prevent stale reads.
     */
    @Test
    public void testUpdateViaQueryEvictsFromCache() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        utx.begin();
        Employee emp1 = createEmployee(id1, "Eve", "Smith", "eve@test.com");
        Employee emp2 = createEmployee(id2, "Frank", "Jones", "frank@test.com");
        employees.save(emp1);
        employees.save(emp2);
        utx.commit();

        // Load into cache
        utx.begin();
        employees.findById(id1);
        employees.findById(id2);
        utx.commit();

        // Verify both in cache
        assertTrue("Employee 1 should be in cache before update", cache.contains(Employee.class, id1));
        assertTrue("Employee 2 should be in cache before update", cache.contains(Employee.class, id2));

        // Update via @Query
        utx.begin();
        long updated = employees.updateFirstName(id1, "UpdatedEve");
        assertEquals("Should update 1 employee", 1, updated);
        utx.commit();

        // Verify: entire Employee cache should be evicted after UPDATE
        // This prevents stale reads for all Employee instances
        assertFalse("Employee cache should be evicted after UPDATE", cache.contains(Employee.class, id1));
        assertFalse("All Employee instances evicted (expected for @Query operations)", cache.contains(Employee.class, id2));
    }

    private Employee createEmployee(UUID id, String firstName, String lastName, String email) {
        Employee emp = new Employee();
        emp.id = id;
        emp.firstName = firstName;
        emp.lastName = lastName;
        emp.title = "Developer";
        emp.hiredOn = LocalDate.now();
        emp.salary = BigDecimal.valueOf(50000.0);
        emp.email = email;
        emp.active = true;
        return emp;
    }
}
