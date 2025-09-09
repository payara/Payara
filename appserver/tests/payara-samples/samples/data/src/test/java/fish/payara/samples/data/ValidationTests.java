package fish.payara.samples.data;

import fish.payara.samples.data.entity.Rectangle;
import fish.payara.samples.data.repo.Rectangles;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolationException;
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
 * Comprehensive Bean Validation tests using Rectangle entity.
 * Tests Jakarta Data integration with Bean Validation constraints.
 */
@RunWith(Arquillian.class)
public class ValidationTests {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "validation-tests.war")
                .addPackages(true, "fish.payara.samples.data.entity")  // entities
                .addPackages(true, "fish.payara.samples.data.repo")    // repository interfaces
                .addPackages(true, "fish.payara.samples.data.support") // JPA implementations
                .addAsResource("META-INF/persistence.xml")     // test persistence.xml (JTA + Payara default)
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml") // basic web.xml
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"); // enables CDI
    }

    @PersistenceContext(unitName = "samples-dataPU")
    private EntityManager em;
    
    @Resource
    private UserTransaction utx;

    @Inject
    private Rectangles rectangles;

    private UUID rect1 = UUID.randomUUID();
    private UUID rect2 = UUID.randomUUID();
    private UUID rect3 = UUID.randomUUID();
    private UUID rect4 = UUID.randomUUID();

    // Clear rectangles table and commit
    private void clearDatabase() throws Exception {
        utx.begin();
        em.createQuery("DELETE FROM Rectangle").executeUpdate();
        utx.commit();
    }

    // Setup test data with various rectangles
    private void setupTestData() throws Exception {
        utx.begin();

        // Clean data from previous executions
        em.createQuery("DELETE FROM Rectangle").executeUpdate();

        // Regenerate IDs for each execution, avoiding conflicts
        rect1 = UUID.randomUUID();
        rect2 = UUID.randomUUID();
        rect3 = UUID.randomUUID();
        rect4 = UUID.randomUUID();

        // Persist valid rectangles directly via EM (participates in JTA transaction)
        em.persist(Rectangle.of(rect1, "Small Square", 10, 10));
        em.persist(Rectangle.of(rect2, "Medium Rectangle", 20, 15));
        em.persist(Rectangle.of(rect3, "Large Rectangle", 50, 30));
        em.persist(Rectangle.of(rect4, "Tall Rectangle", 5, 25));

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
    public void testValidRectangleSave() throws Exception {
        clearDatabase();
        utx.begin();

        // Create valid rectangle
        Rectangle validRect = Rectangle.of(UUID.randomUUID(), "Valid Rectangle", 15, 20);
        rectangles.save(validRect);
        
        utx.commit(); // Commit save to ensure visibility
        utx.begin();
        
        // Verify it was saved - now count should see the persisted entity
        long count = rectangles.count();
        assertEquals(1L, count);

        utx.commit();
    }

    @Test
    public void testNullNameValidation() throws Exception {
        clearDatabase();
        utx.begin();

        try {
            // Should fail validation due to null name - following TCK pattern
            Rectangle invalidRect = Rectangle.of(UUID.randomUUID(), null, 10, 10);
            
            // TCK expects this to throw ConstraintViolationException
            try {
                rectangles.save(invalidRect);
                fail("Expected ConstraintViolationException to be thrown");
            } catch (ConstraintViolationException e) {
                // Expected - this is the correct behavior
            }
            
            utx.rollback();
            utx.begin();
            
            // Verify no entities were persisted due to validation failure
            long count = rectangles.count();
            assertEquals("No rectangles should persist when violating constraints", 0L, count);
            
        } finally {
            if (utx.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                utx.rollback();
            }
        }
    }

    @Test
    public void testMinWidthValidation() throws Exception {
        clearDatabase();
        utx.begin();

        try {
            // Should fail validation due to width < 1 - following TCK pattern
            Rectangle invalidRect = Rectangle.of(UUID.randomUUID(), "Invalid Rectangle", 0, 10);
            
            // TCK expects this to throw ConstraintViolationException
            try {
                rectangles.save(invalidRect);
                fail("Expected ConstraintViolationException to be thrown");
            } catch (ConstraintViolationException e) {
                // Expected - this is the correct behavior
            }
            
            utx.rollback();
            utx.begin();
            
            // Verify no entities were persisted due to validation failure
            long count = rectangles.count();
            assertEquals("No rectangles should persist when violating constraints", 0L, count);
            
        } finally {
            if (utx.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                utx.rollback();
            }
        }
    }

    @Test
    public void testRectangleQueryMethods() throws Exception {
        setupTestData();
        utx.begin();

        // Test findByName
        var smallSquares = rectangles.findByName("Small Square");
        assertEquals(1, smallSquares.size());
        assertEquals(rect1, smallSquares.get(0).id);

        // Test findByWidthGreaterThan
        var wideRectangles = rectangles.findByWidthGreaterThan(15);
        assertEquals(2, wideRectangles.size()); // Medium Rectangle (20) and Large Rectangle (50)

        // Test findSquares
        var squares = rectangles.findSquares();
        assertEquals(1, squares.size());
        assertEquals(rect1, squares.get(0).id); // Only Small Square (10x10)

        // Test findByAreaGreaterThan
        var largeAreaRectangles = rectangles.findByAreaGreaterThan(500);
        assertEquals(1, largeAreaRectangles.size()); // Only Large Rectangle (50x30=1500)

        // Test countByWidth
        long count10Width = rectangles.countByWidth(10);
        assertEquals(1L, count10Width); // Only Small Square

        // Test existsByName
        assertTrue(rectangles.existsByName("Medium Rectangle"));
        assertFalse(rectangles.existsByName("Nonexistent Rectangle"));

        utx.commit();
    }

    @Test
    public void testRectangleMethods() throws Exception {
        setupTestData();
        utx.begin();

        var smallSquare = rectangles.findById(rect1);
        assertTrue(smallSquare.isPresent());
        
        Rectangle rect = smallSquare.get();
        
        // Test area calculation
        assertEquals(100, rect.getArea()); // 10 * 10
        
        // Test square detection
        assertTrue(rect.isSquare());
        
        // Test non-square rectangle
        var mediumRect = rectangles.findById(rect2);
        assertTrue(mediumRect.isPresent());
        assertEquals(300, mediumRect.get().getArea()); // 20 * 15
        assertFalse(mediumRect.get().isSquare());

        utx.commit();
    }
}