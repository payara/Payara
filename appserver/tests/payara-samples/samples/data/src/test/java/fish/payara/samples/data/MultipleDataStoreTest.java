package fish.payara.samples.data;

import fish.payara.samples.data.entity.Product;
import fish.payara.samples.data.repo.ProductsFirstPU;
import fish.payara.samples.data.repo.ProductsSecondPU;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.annotation.Resource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests for FISH-12774: Validate property dataStore from the Repository Annotation
 * to resolve persistence unit name when having multiple persistence units.
 *
 * This test deploys an application with TWO persistence units (firstPU and secondPU)
 * and verifies that @Repository(dataStore = "...") correctly resolves each repository
 * to the appropriate persistence unit.
 */
@RunWith(Arquillian.class)
public class MultipleDataStoreTest {

    private static final String MULTI_PU_PERSISTENCE_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\"\n" +
            "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "             xsi:schemaLocation=\"https://jakarta.ee/xml/ns/persistence\n" +
            "             https://jakarta.ee/xml/ns/persistence/persistence_3_1.xsd\"\n" +
            "             version=\"3.1\">\n" +
            "  <persistence-unit name=\"firstPU\" transaction-type=\"JTA\">\n" +
            "    <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>\n" +
            "    <jta-data-source>jdbc/__default</jta-data-source>\n" +
            "    <exclude-unlisted-classes>false</exclude-unlisted-classes>\n" +
            "    <properties>\n" +
            "      <property name=\"eclipselink.ddl-generation\" value=\"drop-and-create-tables\"/>\n" +
            "      <property name=\"eclipselink.logging.level\" value=\"FINE\"/>\n" +
            "      <property name=\"jakarta.persistence.schema-generation.database.action\" value=\"drop-and-create\"/>\n" +
            "      <property name=\"jakarta.persistence.validation.mode\" value=\"NONE\"/>\n" +
            "    </properties>\n" +
            "  </persistence-unit>\n" +
            "  <persistence-unit name=\"secondPU\" transaction-type=\"JTA\">\n" +
            "    <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>\n" +
            "    <jta-data-source>jdbc/__default</jta-data-source>\n" +
            "    <exclude-unlisted-classes>false</exclude-unlisted-classes>\n" +
            "    <properties>\n" +
            "      <property name=\"eclipselink.ddl-generation\" value=\"drop-and-create-tables\"/>\n" +
            "      <property name=\"eclipselink.logging.level\" value=\"FINE\"/>\n" +
            "      <property name=\"jakarta.persistence.schema-generation.database.action\" value=\"drop-and-create\"/>\n" +
            "      <property name=\"jakarta.persistence.validation.mode\" value=\"NONE\"/>\n" +
            "    </properties>\n" +
            "  </persistence-unit>\n" +
            "</persistence>";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "multiple-datastore-test.war")
                .addClass(Product.class)
                .addClass(ProductsFirstPU.class)
                .addClass(ProductsSecondPU.class)
                .addAsResource(new StringAsset(MULTI_PU_PERSISTENCE_XML), "META-INF/persistence.xml")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @PersistenceContext(unitName = "firstPU")
    private EntityManager emFirst;

    @PersistenceContext(unitName = "secondPU")
    private EntityManager emSecond;

    @Resource
    private UserTransaction utx;

    @Inject
    private ProductsFirstPU productsFirst;

    @Inject
    private ProductsSecondPU productsSecond;

    @Before
    public void setUp() throws Exception {
        utx.begin();
        emFirst.createQuery("DELETE FROM Product").executeUpdate();
        utx.commit();
    }

    @After
    public void teardown() throws Exception {
        if (utx.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
            utx.rollback();
        }
    }

    /**
     * Verify that both repositories are correctly injected when using
     * @Repository(dataStore) with multiple persistence units.
     */
    @Test
    public void testRepositoryInjectionWithMultiplePUs() {
        assertNotNull("ProductsFirstPU repository should be injected", productsFirst);
        assertNotNull("ProductsSecondPU repository should be injected", productsSecond);
    }

    /**
     * Test basic CRUD operations through the first persistence unit repository.
     */
    @Test
    public void testSaveAndFindViaFirstPU() throws Exception {
        Product product = Product.of(UUID.randomUUID(), "Laptop", "Electronics",
                BigDecimal.valueOf(999.99), true);

        utx.begin();
        productsFirst.save(product);
        utx.commit();

        var found = productsFirst.findById(product.id);
        assertTrue("Product should be found via firstPU repository", found.isPresent());
        assertEquals("Laptop", found.get().name);
    }

    /**
     * Test basic CRUD operations through the second persistence unit repository.
     */
    @Test
    public void testSaveAndFindViaSecondPU() throws Exception {
        Product product = Product.of(UUID.randomUUID(), "Desk", "Furniture",
                BigDecimal.valueOf(299.50), true);

        utx.begin();
        productsSecond.save(product);
        utx.commit();

        var found = productsSecond.findById(product.id);
        assertTrue("Product should be found via secondPU repository", found.isPresent());
        assertEquals("Desk", found.get().name);
    }

    /**
     * Test query-by-method-name operations through both repositories.
     */
    @Test
    public void testQueryByMethodNameWithBothPUs() throws Exception {
        Product electronics = Product.of(UUID.randomUUID(), "Phone", "Electronics",
                BigDecimal.valueOf(699.00), true);
        Product furniture = Product.of(UUID.randomUUID(), "Chair", "Furniture",
                BigDecimal.valueOf(150.00), true);

        utx.begin();
        productsFirst.save(electronics);
        productsFirst.save(furniture);
        utx.commit();

        List<Product> electronicsFound = productsFirst.findByCategory("Electronics");
        assertEquals("Should find 1 electronics product via firstPU", 1, electronicsFound.size());
        assertEquals("Phone", electronicsFound.get(0).name);

        List<Product> furnitureFound = productsSecond.findByCategory("Furniture");
        assertEquals("Should find 1 furniture product via secondPU", 1, furnitureFound.size());
        assertEquals("Chair", furnitureFound.get(0).name);
    }

    /**
     * Test count operations through the first PU repository.
     */
    @Test
    public void testCountViaFirstPU() throws Exception {
        utx.begin();
        productsFirst.save(Product.of(UUID.randomUUID(), "Monitor", "Electronics",
                BigDecimal.valueOf(450.00), true));
        productsFirst.save(Product.of(UUID.randomUUID(), "Keyboard", "Electronics",
                BigDecimal.valueOf(79.99), true));
        productsFirst.save(Product.of(UUID.randomUUID(), "Table", "Furniture",
                BigDecimal.valueOf(200.00), true));
        utx.commit();

        long count = productsFirst.countByCategory("Electronics");
        assertEquals("Should count 2 electronics products via firstPU", 2, count);
    }

    /**
     * Test existence check through the second PU repository.
     */
    @Test
    public void testExistsViaSecondPU() throws Exception {
        utx.begin();
        productsSecond.save(Product.of(UUID.randomUUID(), "Sofa", "Furniture",
                BigDecimal.valueOf(800.00), true));
        utx.commit();

        assertTrue("Sofa should exist via secondPU", productsSecond.existsByName("Sofa"));
        assertFalse("NonExistent should not exist via secondPU", productsSecond.existsByName("NonExistent"));
    }

    /**
     * Test delete operations through a specific PU repository.
     */
    @Test
    public void testDeleteViaFirstPU() throws Exception {
        Product product = Product.of(UUID.randomUUID(), "Webcam", "Electronics",
                BigDecimal.valueOf(59.99), true);

        utx.begin();
        productsFirst.save(product);
        utx.commit();

        assertTrue("Product should exist before delete", productsFirst.findById(product.id).isPresent());

        utx.begin();
        productsFirst.deleteById(product.id);
        utx.commit();

        assertFalse("Product should not exist after delete", productsFirst.findById(product.id).isPresent());
    }
}
