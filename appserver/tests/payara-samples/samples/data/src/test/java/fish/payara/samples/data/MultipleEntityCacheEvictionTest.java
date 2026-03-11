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

import fish.payara.samples.data.entity.Box;
import fish.payara.samples.data.entity.Coordinate;
import fish.payara.samples.data.repo.MultipleEntityRepo;
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

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests that @Query annotated methods in multi-entity repositories resolve the
 * correct entity type by parsing the JPQL query string.
 *
 * <p>Bug (FISH-13049): When a {@code @Repository} has no primary entity type and
 * contains methods for different entities, the entity type for {@code @Query}
 * methods is wrongly inferred from other method signatures instead of the JPQL
 * query string. This causes cache eviction to target the wrong entity type.</p>
 *
 * <p>Example: {@code MultipleEntityRepo#deleteIfPositive()} has JPQL
 * {@code DELETE FROM Coordinate WHERE ...} but the implementation resolves its
 * entity type as {@code Box} (inferred from {@code addAll(Box...)}).</p>
 */
@RunWith(Arquillian.class)
public class MultipleEntityCacheEvictionTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "multi-entity-cache-test.war")
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
    private MultipleEntityRepo multipleEntityRepo;

    private Cache cache;

    @Before
    public void setUp() throws Exception {
        cache = em.getEntityManagerFactory().getCache();

        utx.begin();
        em.createQuery("DELETE FROM Coordinate").executeUpdate();
        em.createQuery("DELETE FROM Box").executeUpdate();
        utx.commit();

        cache.evictAll();
    }

    /**
     * FISH-13049: Verifies that a @Query DELETE targeting Coordinate does NOT
     * evict Box entities from the cache.
     *
     * <p>The repository method {@code deleteIfPositive()} has JPQL:
     * {@code DELETE FROM Coordinate WHERE x > 0.0d AND y > 0.0f}
     * Its entity type should be Coordinate, resolved from the JPQL query.
     * However, the current implementation wrongly resolves it as Box (inferred
     * from the {@code addAll(Box...)} method), causing Box cache entries to be
     * evicted instead of Coordinate entries.</p>
     *
     * <p>This test will FAIL until FISH-13049 is fixed.</p>
     */
    @Test
    public void testQueryDeleteEvictsCorrectEntityType() throws Exception {
        // 1. Insert Box entities via the repository
        Box box1 = Box.of("B1", "TestBox1", "desc1", 10, 10, 10, "CARDBOARD", "RED", true);
        Box box2 = Box.of("B2", "TestBox2", "desc2", 20, 20, 20, "WOOD", "BLUE", false);

        utx.begin();
        multipleEntityRepo.addAll(box1, box2);
        utx.commit();

        // 2. Insert Coordinate entities via the repository
        UUID c1Id = UUID.randomUUID();
        UUID c2Id = UUID.randomUUID();

        utx.begin();
        multipleEntityRepo.create(Coordinate.of(c1Id, 5.0, 3.0f));   // positive x and y — will be deleted
        multipleEntityRepo.create(Coordinate.of(c2Id, -1.0, -2.0f)); // negative — will NOT be deleted
        utx.commit();

        // 3. Load all entities into L2 cache
        utx.begin();
        em.find(Box.class, "B1");
        em.find(Box.class, "B2");
        em.find(Coordinate.class, c1Id);
        em.find(Coordinate.class, c2Id);
        utx.commit();

        // 4. Verify all are in cache
        assertTrue("Box B1 should be in cache", cache.contains(Box.class, "B1"));
        assertTrue("Box B2 should be in cache", cache.contains(Box.class, "B2"));
        assertTrue("Coordinate C1 should be in cache", cache.contains(Coordinate.class, c1Id));
        assertTrue("Coordinate C2 should be in cache", cache.contains(Coordinate.class, c2Id));

        // 5. Execute @Query("DELETE FROM Coordinate WHERE x > 0.0d AND y > 0.0f")
        utx.begin();
        long deleted = multipleEntityRepo.deleteIfPositive();
        utx.commit();

        assertEquals("Should delete 1 coordinate (C1)", 1, deleted);

        // 6. CRITICAL ASSERTIONS:
        // Coordinate cache should be evicted (correct entity type from JPQL)
        assertFalse("Coordinate C1 (deleted) should NOT be in cache",
                cache.contains(Coordinate.class, c1Id));

        // Box cache should NOT be evicted — they are unrelated to this query!
        // BUG: Current implementation evicts Box instead of Coordinate because
        // it resolves entity type as Box (from addAll method) instead of
        // parsing the JPQL to find Coordinate.
        assertTrue("Box B1 should STILL be in cache (unrelated to Coordinate delete)",
                cache.contains(Box.class, "B1"));
        assertTrue("Box B2 should STILL be in cache (unrelated to Coordinate delete)",
                cache.contains(Box.class, "B2"));
    }

    /**
     * FISH-13049 supplementary: Verifies that the @Query method actually
     * executes the JPQL against the correct entity (Coordinate, not Box).
     *
     * <p>If the entity type is wrongly resolved as Box, the JPQL query
     * {@code DELETE FROM Coordinate WHERE x > 0.0d AND y > 0.0f} might still
     * execute correctly against the database (since it's raw JPQL), but the
     * cache eviction will target Box instead of Coordinate.</p>
     */
    @Test
    public void testQueryDeleteActuallyDeletesCoordinates() throws Exception {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();
        UUID p4 = UUID.randomUUID();

        // Insert coordinates with varying x,y values
        utx.begin();
        multipleEntityRepo.create(Coordinate.of(p1, 10.0, 5.0f));   // positive: should be deleted
        multipleEntityRepo.create(Coordinate.of(p2, 1.0, 0.5f));     // positive: should be deleted
        multipleEntityRepo.create(Coordinate.of(p3, -3.0, 7.0f));    // negative x: should NOT be deleted
        multipleEntityRepo.create(Coordinate.of(p4, 2.0, -1.0f));    // negative y: should NOT be deleted
        utx.commit();

        // Also insert a Box — should remain untouched
        utx.begin();
        multipleEntityRepo.addAll(
                Box.of("BX1", "SafeBox", "safe", 5, 5, 5, "METAL", "BLACK", true));
        utx.commit();

        // Execute the @Query DELETE
        utx.begin();
        long deleted = multipleEntityRepo.deleteIfPositive();
        utx.commit();

        assertEquals("Should delete 2 coordinates with positive x and y", 2, deleted);

        // Verify coordinates
        utx.begin();
        assertNull("P1 should be deleted", em.find(Coordinate.class, p1));
        assertNull("P2 should be deleted", em.find(Coordinate.class, p2));
        assertNotNull("P3 should still exist (negative x)", em.find(Coordinate.class, p3));
        assertNotNull("P4 should still exist (negative y)", em.find(Coordinate.class, p4));

        // Verify Box is untouched
        assertNotNull("Box BX1 should still exist", em.find(Box.class, "BX1"));
        utx.commit();
    }
}
