package fish.payara.samples.data;

import fish.payara.samples.data.entity.Box;
import fish.payara.samples.data.repo.Boxes;
import fish.payara.samples.data.support.JpaBoxes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.annotation.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

// Arquillian + ShrinkWrap
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for Box entity with custom ID field name.
 * Tests Jakarta Data integration with entities using non-standard ID field names.
 */
@RunWith(Arquillian.class)
public class CustomIdTests {

    @Deployment
    public static WebArchive createDeployment() {
        var libs = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("jakarta.data:jakarta.data-api")
                .withTransitivity()
                .asFile();

        return ShrinkWrap.create(WebArchive.class, "custom-id-tests.war")
                .addPackages(true, "fish.payara.samples.data") // includes entities/repos/support classes
                .addAsResource("META-INF/persistence.xml")     // test persistence.xml (JTA + Payara default)
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml") // basic web.xml
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml") // enables CDI (harmless)
                .addAsLibraries(libs); // includes Jakarta Data API
    }

    @PersistenceContext(unitName = "samples-dataPU")
    private EntityManager em;
    
    @Resource
    private UserTransaction utx;

    private Boxes boxes;
    private JpaBoxes jpaBoxes;

    @Before
    public void setup() throws Exception {
        boxes = new JpaBoxes(em);
        jpaBoxes = new JpaBoxes(em);
    }

    // Clear boxes table and commit
    private void clearDatabase() throws Exception {
        utx.begin();
        em.createQuery("DELETE FROM Box").executeUpdate();
        utx.commit();
    }

    // Setup test data with various boxes
    private void setupTestData() throws Exception {
        utx.begin();

        // Clean data from previous executions
        em.createQuery("DELETE FROM Box").executeUpdate();

        // Persist boxes directly via EM (participates in JTA transaction)
        em.persist(Box.of("BOX-001", "Small Cardboard Box", "For shipping small items", 
                         20, 15, 10, "CARDBOARD", "BROWN", true));
        em.persist(Box.of("BOX-002", "Medium Plastic Box", "Storage container", 
                         40, 30, 25, "PLASTIC", "BLUE", true));
        em.persist(Box.of("BOX-003", "Large Wooden Crate", "Heavy duty shipping", 
                         100, 80, 60, "WOOD", "NATURAL", false));
        em.persist(Box.of("CUBE-001", "Perfect Cube", "Cubic storage unit", 
                         25, 25, 25, "PLASTIC", "WHITE", true));

        utx.commit();
    }

    @After
    public void teardown() throws Exception {
        try {
            if (utx.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                utx.rollback();
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testBasicCrudWithCustomId() throws Exception {
        clearDatabase();
        utx.begin();

        // Test save with custom ID field
        Box box = Box.of("TEST-001", "Test Box", "Custom ID test", 15, 12, 8, "CARDBOARD", "RED", true);
        boxes.save(box);
        
        // Verify save worked using findAll().count()
        long count = boxes.findAll().count();
        assertEquals(1L, count);

        // Test findById with custom ID
        var foundBox = boxes.findById("TEST-001");
        assertTrue(foundBox.isPresent());
        assertEquals("Test Box", foundBox.get().name);
        assertEquals("TEST-001", foundBox.get().code);

        // Test update
        box.description = "Updated description";
        boxes.save(box);
        
        var updatedBox = boxes.findById("TEST-001");
        assertTrue(updatedBox.isPresent());
        assertEquals("Updated description", updatedBox.get().description);

        // Test delete by custom ID
        boxes.deleteById("TEST-001");
        long finalCount = boxes.findAll().count();
        assertEquals(0L, finalCount);

        utx.commit();
    }

    @Test
    public void testCustomIdRepositoryMethods() throws Exception {
        clearDatabase();
        utx.begin();

        Box testBox = Box.of("CUSTOM-001", "Custom Test", "Testing custom ID methods", 
                           10, 10, 10, "PLASTIC", "GREEN", true);
        boxes.save(testBox);

        // Test JpaBoxes custom methods for ID handling
        assertTrue(jpaBoxes.existsByCode("CUSTOM-001"));
        assertFalse(jpaBoxes.existsByCode("NONEXISTENT"));

        var foundByCode = jpaBoxes.findByCode("CUSTOM-001");
        assertTrue(foundByCode.isPresent());
        assertEquals("Custom Test", foundByCode.get().name);

        // Test delete by code
        jpaBoxes.deleteByCode("CUSTOM-001");
        assertFalse(jpaBoxes.existsByCode("CUSTOM-001"));

        utx.commit();
    }

    @Test
    public void testBoxQueryMethods() throws Exception {
        setupTestData();
        utx.begin();

        // Test findByName
        var smallBoxes = boxes.findByName("Small Cardboard Box");
        assertEquals(1, smallBoxes.size());
        assertEquals("BOX-001", smallBoxes.get(0).code);

        // Test findByMaterial
        var plasticBoxes = boxes.findByMaterial("PLASTIC");
        assertEquals(2, plasticBoxes.size()); // BOX-002 and CUBE-001

        // Test findByColor
        var blueBoxes = boxes.findByColor("BLUE");
        assertEquals(1, blueBoxes.size());
        assertEquals("BOX-002", blueBoxes.get(0).code);

        // Test findByStackable
        var stackableBoxes = boxes.findByStackable(true);
        assertEquals(3, stackableBoxes.size()); // All except BOX-003

        var nonStackableBoxes = boxes.findByStackable(false);
        assertEquals(1, nonStackableBoxes.size());
        assertEquals("BOX-003", nonStackableBoxes.get(0).code);

        utx.commit();
    }

    @Test
    public void testBoxVolumeQueries() throws Exception {
        setupTestData();
        utx.begin();

        // Test findCubes
        var cubes = boxes.findCubes();
        assertEquals(1, cubes.size());
        assertEquals("CUBE-001", cubes.get(0).code);

        // Test findLargeBoxes (volume > 100000 cubic cm)
        var largeBoxes = boxes.findLargeBoxes();
        assertEquals(1, largeBoxes.size());
        assertEquals("BOX-003", largeBoxes.get(0).code); // 100*80*60 = 480000

        // Test findByVolumeGreaterThan
        var mediumPlusBoxes = boxes.findByVolumeGreaterThan(20000L);
        assertEquals(2, mediumPlusBoxes.size()); // BOX-002 and BOX-003

        utx.commit();
    }

    @Test
    public void testBoxCombinationQueries() throws Exception {
        setupTestData();
        utx.begin();

        // Test findByMaterialAndColor
        var plasticBlueBoxes = boxes.findByMaterialAndColor("PLASTIC", "BLUE");
        assertEquals(1, plasticBlueBoxes.size());
        assertEquals("BOX-002", plasticBlueBoxes.get(0).code);

        var plasticWhiteBoxes = boxes.findByMaterialAndColor("PLASTIC", "WHITE");
        assertEquals(1, plasticWhiteBoxes.size());
        assertEquals("CUBE-001", plasticWhiteBoxes.get(0).code);

        // Test countByMaterial
        long cardboardCount = boxes.countByMaterial("CARDBOARD");
        assertEquals(1L, cardboardCount);

        long plasticCount = boxes.countByMaterial("PLASTIC");
        assertEquals(2L, plasticCount);

        utx.commit();
    }

    @Test
    public void testBoxExistenceQueries() throws Exception {
        setupTestData();
        utx.begin();

        // Test existsByName
        assertTrue(boxes.existsByName("Small Cardboard Box"));
        assertFalse(boxes.existsByName("Nonexistent Box"));

        // Test existsStackableBoxes
        assertTrue(boxes.existsStackableBoxes());

        utx.commit();
    }

    @Test
    public void testBoxBusinessLogic() throws Exception {
        setupTestData();
        utx.begin();

        // Test cube detection
        var cubeBox = boxes.findById("CUBE-001").get();
        assertTrue(cubeBox.isCube());
        assertEquals(15625L, cubeBox.getVolume()); // 25^3

        var nonCubeBox = boxes.findById("BOX-001").get();
        assertFalse(nonCubeBox.isCube());

        // Test size categories
        var largeBox = boxes.findById("BOX-003").get();
        assertFalse(largeBox.isSmall());
        assertTrue(largeBox.isLarge());

        // Test surface area calculation
        var mediumBox = boxes.findById("BOX-002").get();
        long expectedSurfaceArea = 2L * (40*30 + 30*25 + 25*40);
        assertEquals(expectedSurfaceArea, mediumBox.getSurfaceArea());

        utx.commit();
    }

    @Test
    public void testIdFieldMappingConsistency() throws Exception {
        clearDatabase();
        utx.begin();

        // Create box and verify ID field mapping works correctly
        String customCode = "ID-MAPPING-TEST";
        Box box = Box.of(customCode, "ID Mapping Test", "Testing ID field consistency", 
                        10, 10, 10, "PLASTIC", "YELLOW", true);
        
        boxes.save(box);

        // Retrieve via repository
        var foundBox = boxes.findById(customCode);
        assertTrue(foundBox.isPresent());
        assertEquals(customCode, foundBox.get().code);

        // Retrieve via JPA directly to ensure consistency
        Box directBox = em.find(Box.class, customCode);
        assertNotNull(directBox);
        assertEquals(customCode, directBox.code);
        assertEquals(box.name, directBox.name);

        // Verify both retrieval methods return the same data
        assertEquals(foundBox.get().name, directBox.name);
        assertEquals(foundBox.get().description, directBox.description);

        utx.commit();
    }
}