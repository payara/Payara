package fish.payara.samples.data;

import fish.payara.samples.data.entity.Employee;
import fish.payara.samples.data.repo.Employees;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.annotation.Resource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Pure Jakarta Data test - testing only @Inject without JPA implementations
 */
@RunWith(Arquillian.class)
public class PureJakartaDataTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "pure-jakarta-data-test.war")
                .addPackages(true, "fish.payara.samples.data.entity")  // entities only
                .addPackages(true, "fish.payara.samples.data.repo")    // repository interfaces only
                .addAsResource("META-INF/persistence.xml")     // persistence.xml
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml") // web.xml
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"); // enables CDI
    }

    @PersistenceContext(unitName = "samples-dataPU")
    private EntityManager em;
    
    @Resource
    private UserTransaction utx;

    @Inject
    private Employees employees;

    @Test
    public void testPureJakartaDataInjection() throws Exception {
        assertNotNull("Employees repository should be injected by Jakarta Data provider", employees);
        
        utx.begin();
        
        // Clean any existing data
        em.createQuery("DELETE FROM Employee").executeUpdate();
        
        // Create a simple employee using Jakarta Data repository
        Employee emp = new Employee();
        emp.id = UUID.randomUUID();
        emp.firstName = "Test";
        emp.lastName = "Employee";
        emp.title = "Developer";
        emp.hiredOn = LocalDate.now();
        emp.salary = BigDecimal.valueOf(50000.0);
        emp.email = "test@example.com";
        emp.active = true;
        
        employees.save(emp);
        
        utx.commit();
        
        // Verify it was saved
        utx.begin();
        var found = employees.findById(emp.id);
        assertTrue("Employee should be found", found.isPresent());
        assertEquals("Test", found.get().firstName);
        utx.commit();
    }
}