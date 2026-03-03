package fish.payara.samples.data;

import fish.payara.samples.data.entity.Product;
import fish.payara.samples.data.repo.AbstractProducts;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;
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

import java.util.UUID;
import java.util.function.Supplier;

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

    private static final String MULTI_PU_PERSISTENCE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence xmlns="https://jakarta.ee/xml/ns/persistence"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                         https://jakarta.ee/xml/ns/persistence/persistence_3_1.xsd"
                         version="3.1">
              <persistence-unit name="firstPU" transaction-type="JTA">
                <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
                <jta-data-source>jdbc/__default</jta-data-source>
                <exclude-unlisted-classes>false</exclude-unlisted-classes>
                <properties>
                  <property name="eclipselink.ddl-generation" value="drop-and-create-tables"/>
                  <property name="eclipselink.logging.level" value="FINE"/>
                  <property name="jakarta.persistence.schema-generation.database.action" value="drop-and-create"/>
                  <property name="jakarta.persistence.validation.mode" value="NONE"/>
                </properties>
              </persistence-unit>
              <persistence-unit name="secondPU" transaction-type="JTA">
                <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
                <exclude-unlisted-classes>false</exclude-unlisted-classes>
                <properties>
                  <property name="jakarta.persistence.jdbc.driver" value="org.h2.Driver"/>
                  <property name="jakarta.persistence.jdbc.url" value="jdbc:h2:mem:secondPU;DB_CLOSE_DELAY=-1"/>
                  <property name="jakarta.persistence.jdbc.user" value="sa"/>
                  <property name="jakarta.persistence.jdbc.password" value=""/>
                  <property name="eclipselink.ddl-generation" value="drop-and-create-tables"/>
                  <property name="eclipselink.logging.level" value="FINE"/>
                  <property name="jakarta.persistence.schema-generation.database.action" value="drop-and-create"/>
                  <property name="jakarta.persistence.validation.mode" value="NONE"/>
                </properties>
              </persistence-unit>
            </persistence>""";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "multiple-datastore-test.war")
                .addClass(Product.class)
                .addClass(AbstractProducts.class)
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

    private AbstractProducts isolatedRepository;
    private AbstractProducts targetedRepository;

    private final Supplier<Product> ELECTRONIC_PRODUCT = () -> Product.of(UUID.randomUUID(), "Phone", "Electronics");
    private final Supplier<Product> NOT_ELECTRONIC_PRODUCT = () -> Product.of(UUID.randomUUID(), "Table", "Furniture");

    @Before
    public void setUp() throws Exception {
        utx.begin();
        emFirst.createQuery("DELETE FROM Product").executeUpdate();
        emSecond.createQuery("DELETE FROM Product").executeUpdate();
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
     * Test existence check through the second PU repository.
     */
    @Test
    public void testRepository() throws Exception {
        targetedRepository = productsFirst;
        isolatedRepository = productsSecond;

        doTestRepository();

        targetedRepository = productsSecond;
        isolatedRepository = productsFirst;

        doTestRepository();
    }

    private void doTestRepository() throws Exception {
        Product product = ELECTRONIC_PRODUCT.get();

        // Test @Save and verify with @Find and "existsByName" (method name based query)
        saveProductAndAssertExists(product);

        // Test "findByCategory" and "countByCategory" (method name based query)
        assertElectronicProductsByCategory();

        saveProductAndAssertExists(NOT_ELECTRONIC_PRODUCT.get());

        // Repeat to ensure count is unaffected
        assertElectronicProductsByCategory();

        // Test @Delete and verify with @Find
        deleteProductAndAssertExists(product);
    }

    private void saveProductAndAssertExists(Product product) throws Exception {
        utx.begin();
        targetedRepository.save(product);
        utx.commit();

        assertFindById(targetedRepository, product);
        assertNotFindById(isolatedRepository, product);

        assertExistsByName(targetedRepository, product);
        assertNotExistByName(isolatedRepository, product);
    }

    private void deleteProductAndAssertExists(Product product) throws Exception {
        utx.begin();
        targetedRepository.deleteById(product.id);
        utx.commit();

        assertNotFindById(targetedRepository, product);
        assertNotExistByName(targetedRepository, product);
    }

    private void assertElectronicProductsByCategory() {
        assertElectronicProductsByCategory(targetedRepository, 1);
        assertElectronicProductsByCategory(isolatedRepository, 0);
    }

    private void assertFindById(BasicRepository<Product, UUID> repository, Product product) {
        var found = repository.findById(product.id);
        assertTrue("Product should be found via " + repository.getClass(), found.isPresent());
        assertEquals(product.name, found.get().name);
    }

    private void assertNotFindById(BasicRepository<Product, UUID> repository, Product product) {
        var found = repository.findById(product.id);
        assertFalse("Product should not be found via " + repository.getClass(), found.isPresent());
    }

    private void assertExistsByName(AbstractProducts repository, Product product) {
        assertTrue(product.name + " should exist via " + repository.getClass(), repository.existsByName(product.name));
    }

    private void assertNotExistByName(AbstractProducts repository, Product product) {
        assertFalse(product.name + " should not exist via " + repository.getClass(), repository.existsByName(product.name));
    }

    private void assertElectronicProductsByCategory(AbstractProducts repository, int expected) {
        assertEquals(
                "Should find " + expected + " electronic products via " + repository.getClass(),
                expected, repository.findByCategory("Electronics").size());
        assertEquals(
                "Should count " + expected + " electronic products via " + repository.getClass(),
                expected, repository.countByCategory("Electronics"));
    }

    /**
     * Repository bound to the "firstPU" persistence unit via dataStore.
     */
    @Repository(dataStore = "firstPU")
    private interface ProductsFirstPU extends AbstractProducts {}

    /**
     * Repository bound to the "secondPU" persistence unit via dataStore.
     */
    @Repository(dataStore = "secondPU")
    private interface ProductsSecondPU extends AbstractProducts {}
}
