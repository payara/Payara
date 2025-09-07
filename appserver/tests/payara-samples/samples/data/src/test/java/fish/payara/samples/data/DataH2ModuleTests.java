package fish.payara.samples.data;

import fish.payara.samples.data.entity.Address;
import fish.payara.samples.data.entity.Company;
import fish.payara.samples.data.entity.Employee;
import fish.payara.samples.data.repo.Addresses;
import fish.payara.samples.data.repo.Companies;
import fish.payara.samples.data.repo.Employees;
import fish.payara.samples.data.support.JpaAddresses;
import fish.payara.samples.data.support.JpaCompanies;
import fish.payara.samples.data.support.JpaEmployees;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class DataH2ModuleTests {

    // Packages a test WAR with module classes + persistence.xml + H2
    @Deployment
    public static WebArchive createDeployment() {
        var libs = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("jakarta.data:jakarta.data-api")
                .withTransitivity()
                .asFile();

        return ShrinkWrap.create(WebArchive.class, "samples-data-tests.war")
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

    private Addresses addresses;
    private Companies companies;
    private Employees employees;

    private UUID addr1 = UUID.randomUUID();
    private UUID addr2 = UUID.randomUUID();
    private UUID addr3 = UUID.randomUUID();

    private UUID comp1 = UUID.randomUUID();
    private UUID comp2 = UUID.randomUUID();

    private UUID emp1 = UUID.randomUUID();
    private UUID emp2 = UUID.randomUUID();
    private UUID emp3 = UUID.randomUUID();
    private UUID emp4 = UUID.randomUUID();
    private UUID emp5 = UUID.randomUUID();

    @Before
    public void setup() throws Exception {
        addresses = new JpaAddresses(em);
        companies = new JpaCompanies(em);
        employees = new JpaEmployees(em);
    }

    // Completely clears tables in safe order (FKs) and commits.
    private void clearDatabase() throws Exception {
        utx.begin();
        em.createQuery("DELETE FROM Employee").executeUpdate();
        em.createQuery("DELETE FROM Company").executeUpdate();
        em.createQuery("DELETE FROM Address").executeUpdate();
        utx.commit();
    }

    // Uses JTA + container-managed EM to persist data.
    // Avoids using repositories for "save" (which may assume RESOURCE_LOCAL),
    // ensuring consistent data for tests.
    private void setupTestData() throws Exception {
        utx.begin();

        // Clean data from previous executions (safe order to avoid FKs)
        em.createQuery("DELETE FROM Employee").executeUpdate();
        em.createQuery("DELETE FROM Company").executeUpdate();
        em.createQuery("DELETE FROM Address").executeUpdate();

        // (Re)generate IDs for each execution, avoiding conflicts
        addr1 = UUID.randomUUID();
        addr2 = UUID.randomUUID();
        addr3 = UUID.randomUUID();
        comp1 = UUID.randomUUID();
        comp2 = UUID.randomUUID();
        emp1 = UUID.randomUUID();
        emp2 = UUID.randomUUID();
        emp3 = UUID.randomUUID();
        emp4 = UUID.randomUUID();
        emp5 = UUID.randomUUID();

        // Persist directly via EM (participates in JTA transaction)
        em.persist(Address.of(addr1, "1 First St", "Springfield", "MA", "01101", "US"));
        em.persist(Address.of(addr2, "2 Second Ave", "Metropolis", "NY", "10001", "US"));
        em.persist(Address.of(addr3, "3 Third Blvd", "Gotham", "NJ", "07001", "US"));

        em.persist(Company.of(comp1, "Acme Corporation", "Manufacturing", true, addr1, Instant.parse("2022-01-01T00:00:00Z")));
        em.persist(Company.of(comp2, "Globex LLC", "Technology", false, addr2, Instant.parse("2021-12-31T00:00:00Z")));

        em.persist(Employee.of(emp1, "Alice", "Anderson", "alice@acme.com", comp1, addr1, "Developer", new BigDecimal("95000"), true, LocalDate.of(2021,1,10)));
        em.persist(Employee.of(emp2, "Bob", "Barker", "bob@acme.com", comp1, addr1, "Developer", new BigDecimal("105000"), true, LocalDate.of(2020,5,1)));
        em.persist(Employee.of(emp3, "Carol", "Clark", "carol@globex.com", comp2, addr2, "QA Engineer", new BigDecimal("87000"), false, LocalDate.of(2022,3,15)));
        em.persist(Employee.of(emp4, "Dan", "Doe", "dan@acme.com", comp1, addr3, "Manager", new BigDecimal("130000"), true, LocalDate.of(2019,9,9)));
        em.persist(Employee.of(emp5, "Eve", "Evans", "eve@globex.com", comp2, addr2, "Developer", new BigDecimal("99000"), true, LocalDate.of(2023,1,5)));

        utx.commit();
    }

    @After
    public void teardown() throws Exception {
        // Container managed EntityManager doesn't need to be closed
        // UserTransaction is managed by the container
        try {
            if (utx.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                utx.rollback();
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testBasicJpaOperations() throws Exception {
        // Ensures test isolation: starts with empty DB
        clearDatabase();

        utx.begin();
        var address = Address.of(addr1, "1 First St", "Springfield", "MA", "01101", "US");
        var company = Company.of(comp1, "Acme Corporation", "Manufacturing", true, addr1, Instant.parse("2022-01-01T00:00:00Z"));
        var employee = Employee.of(emp1, "Alice", "Anderson", "alice@acme.com", comp1, addr1, "Developer", new BigDecimal("95000"), true, LocalDate.of(2021,1,10));

        em.persist(address);
        em.persist(company);
        em.persist(employee);
        em.flush();

        var count = em.createQuery("SELECT COUNT(e) FROM Employee e", Long.class).getSingleResult();
        assertEquals(1L, count.longValue());
        // rollback will be done by @After (transaction still active)
    }

    @Test
    public void testBasicRepositoryBuiltInMethods_Addresses() throws Exception {
        setupTestData();
        utx.begin();
        
        // Don't assume UUID order; validate content
        List<Address> list = addresses.findAll()
                .sorted(Comparator.comparing(a -> a.id))
                .collect(Collectors.toList());

        assertEquals(3, list.size());
        var ids = list.stream().map(a -> a.id).collect(Collectors.toSet());
        assertTrue(ids.containsAll(Set.of(addr1, addr2, addr3)));

        // Test findById operation
        assertTrue(addresses.findById(addr1).isPresent());
        assertTrue(addresses.findById(addr2).isPresent());
        assertTrue(addresses.findById(addr3).isPresent());
        
        utx.commit();
    }

    @Test
    public void testCompaniesContainsIgnoreCase() throws Exception {
        setupTestData();
        utx.begin();
        var active = ((JpaCompanies) companies).findByNameContainsIgnoreCase("ACME");
        assertEquals(1, active.size());
        assertEquals(comp1, active.get(0).id);

        var byName = companies.findByNameContainsIgnoreCase("glob");
        assertEquals(1, byName.size());
        assertEquals(comp2, byName.get(0).id);
        
        utx.commit();
    }

    @Test
    public void testEmployeesQueriesInspiredByTCK() throws Exception {
        setupTestData();
        utx.begin();
        // Active by company: expected result is 3 (alice, bob, dan).
        var acmeActive = ((JpaEmployees) employees).findActiveByCompanyJPQL(comp1)
                .stream().map(e -> e.email).collect(Collectors.toList());
        assertEquals(Set.of("alice@acme.com", "bob@acme.com", "dan@acme.com"), new HashSet<>(acmeActive));

        // ignore-case (list)
        var byEmail = employees.findByEmailIgnoreCase("EVE@globex.com");
        assertEquals(1, byEmail.size());
        assertEquals(emp5, byEmail.get(0).id);

        // ignore-case "single" with exceptions like in TCK
        try {
            ((JpaEmployees) employees).findByEmailIgnoreCaseSingle("alice@acme.com");
        } catch (Exception e) {
            fail("Should not throw exception for existing email");
        }
        
        try {
            ((JpaEmployees) employees).findByEmailIgnoreCaseSingle("nobody@acme.com");
            fail("Should throw EmptyResultException");
        } catch (jakarta.data.exceptions.EmptyResultException expected) {
            // ok
        }

        // Trigger NonUniqueResultException by inserting duplicate email (with different case)
        UUID dupId = UUID.randomUUID();
        employees.save(Employee.of(dupId, "ALICE", "ANDERSON", "ALICE@ACME.COM",
                comp1, addr1, "Developer", new BigDecimal("95000"), true, LocalDate.of(2021,1,11)));
        
        try {
            ((JpaEmployees) employees).findByEmailIgnoreCaseSingle("alice@acme.com");
            fail("Should throw NonUniqueResultException");
        } catch (jakarta.data.exceptions.NonUniqueResultException expected) {
            // ok
        }
        
        utx.commit();
    }

    @Test
    public void testTopNBySalary_Descending() throws Exception {
        setupTestData();
        utx.begin();
        var top3 = ((JpaEmployees) employees).findTopNBySalaryDescJPQL(3);
        assertEquals(List.of(emp4, emp2, emp5),
                top3.stream().map(e -> e.id).collect(Collectors.toList()));
        
        utx.commit();
    }

    @Test
    public void testMixedSortingViaStream() throws Exception {
        setupTestData();
        utx.begin();
        var sorted = employees.findAll()
                .sorted(Comparator
                        .comparing((Employee e) -> e.lastName)
                        .thenComparing((Employee e) -> e.salary, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(e -> e.lastName + ":" + e.salary)
                .collect(Collectors.toList());

        assertEquals(
                List.of(
                        "Anderson:95000",
                        "Barker:105000",
                        "Clark:87000",
                        "Doe:130000",
                        "Evans:99000"
                ),
                sorted
        );
        
        utx.commit();
    }

    @Test
    public void testEdgeCases_emptyAndNotFound() throws Exception {
        // Ensures empty DB for this scenario
        clearDatabase();
        utx.begin();

        Employees emptyEmployees = new JpaEmployees(em);
        assertEquals(0, emptyEmployees.findAll().count());
        assertFalse(emptyEmployees.findById(UUID.randomUUID()).isPresent());

        emptyEmployees.deleteById(UUID.randomUUID());
        emptyEmployees.deleteAll(List.of());

        utx.commit();
    }

    private EntityManager newAutoCloseableEM() {
        // In JEE environment, we cannot create/close EntityManager directly.
        // Method kept only for compatibility (do not use with try-with-resources).
        return em;
    }

    // ====== Level 2 - Comprehensive Query Operations Tests ======

    @Test
    public void testEmployeeSalaryRangeQueries() throws Exception {
        setupTestData();
        utx.begin();

        // Test findBySalaryBetween
        var midRange = employees.findBySalaryBetween(new BigDecimal("90000"), new BigDecimal("110000"));
        assertEquals(3, midRange.size()); // Alice(95k), Bob(105k), Eve(99k)
        
        // Test findBySalaryGreaterThan
        var highSalary = employees.findBySalaryGreaterThan(new BigDecimal("100000"));
        assertEquals(2, highSalary.size()); // Bob(105k), Dan(130k)
        
        // Test findBySalaryIn
        var specificSalaries = employees.findBySalaryIn(
            List.of(new BigDecimal("95000"), new BigDecimal("130000")));
        assertEquals(2, specificSalaries.size()); // Alice(95k), Dan(130k)

        utx.commit();
    }

    @Test
    public void testEmployeeNamePatternQueries() throws Exception {
        setupTestData();
        utx.begin();

        // Test findByLastNameLikeIgnoreCase with pattern
        var namesWithA = employees.findByLastNameLikeIgnoreCase("%a%");
        assertTrue(namesWithA.size() >= 3); // Anderson, Barker, Clark
        
        // Test findByLastNameNotLike
        var notEndingWithE = employees.findByLastNameNotLike("%e");
        assertTrue(notEndingWithE.size() >= 3); // Should exclude none from our test data
        
        // Test boolean combinations
        var aliceAnderson = employees.findByFirstNameAndLastName("Alice", "Anderson");
        assertEquals(1, aliceAnderson.size());
        assertEquals(emp1, aliceAnderson.get(0).id);
        
        var aliceOrBob = employees.findByFirstNameOrLastName("Alice", "Barker");
        assertEquals(2, aliceOrBob.size()); // Alice Anderson + Bob Barker

        utx.commit();
    }

    @Test
    public void testEmployeeNullValueQueries() throws Exception {
        setupTestData();
        
        // Try to add an employee with null addressId for testing
        utx.begin();
        UUID empNoAddr = UUID.randomUUID();
        boolean nullAddressAllowed = false;
        try {
            em.persist(Employee.of(empNoAddr, "John", "Doe", "john@test.com", comp1, null, 
                                  "Intern", new BigDecimal("50000"), true, LocalDate.of(2023,6,1)));
            em.flush(); // Force constraint check
            utx.commit();
            nullAddressAllowed = true;
        } catch (Exception e) {
            // addressId column doesn't allow null - rollback
            utx.rollback();
        }
        
        utx.begin();
        
        // Test null checks based on schema capabilities
        var noAddress = employees.findByAddressIdIsNull();
        if (nullAddressAllowed) {
            assertEquals(1, noAddress.size());
            assertEquals(empNoAddr, noAddress.get(0).id);
        } else {
            assertEquals(0, noAddress.size()); // No employees with null addressId in schema
        }
        
        var withAddress = employees.findByAddressIdIsNotNull();
        int expectedWithAddress = nullAddressAllowed ? 5 : 5; // Original 5 employees have addresses
        assertEquals(expectedWithAddress, withAddress.size());

        utx.commit();
    }

    @Test
    public void testEmployeeAggregationQueries() throws Exception {
        setupTestData();
        utx.begin();

        // Test countByActive
        long activeCount = employees.countByActive(true);
        assertEquals(4L, activeCount); // Alice, Bob, Dan, Eve are active
        
        long inactiveCount = employees.countByActive(false);
        assertEquals(1L, inactiveCount); // Carol is inactive
        
        // Test existsByEmailIgnoreCase
        assertTrue(employees.existsByEmailIgnoreCase("ALICE@ACME.COM"));
        assertTrue(employees.existsByEmailIgnoreCase("eve@globex.com"));
        assertFalse(employees.existsByEmailIgnoreCase("nonexistent@test.com"));

        utx.commit();
    }

    @Test
    public void testEmployeePaginationQueries() throws Exception {
        setupTestData();
        utx.begin();

        // Test findAllOrderBySalaryDesc with pagination
        var topEarners = employees.findAllOrderBySalaryDesc(0, 2);
        assertEquals(2, topEarners.size());
        assertEquals(emp4, topEarners.get(0).id); // Dan (130k)
        assertEquals(emp2, topEarners.get(1).id); // Bob (105k)
        
        var nextEarners = employees.findAllOrderBySalaryDesc(2, 2);
        assertEquals(2, nextEarners.size());
        
        // Test findByCompanyOrderByLastNameAsc with pagination
        var acmeEmployees = employees.findByCompanyOrderByLastNameAsc(comp1, 0, 2);
        assertEquals(2, acmeEmployees.size());
        
        utx.commit();
    }

    @Test
    public void testCompanyEnhancedQueries() throws Exception {
        setupTestData();
        utx.begin();

        // Test new company query methods
        var techCompanies = companies.findByIndustry("Technology");
        assertEquals(1, techCompanies.size());
        assertEquals(comp2, techCompanies.get(0).id);
        
        var activeCompanies = companies.findByActive(true);
        assertEquals(1, activeCompanies.size());
        assertEquals(comp1, activeCompanies.get(0).id);
        
        var inactiveCompanies = companies.findByActive(false);
        assertEquals(1, inactiveCompanies.size());
        assertEquals(comp2, inactiveCompanies.get(0).id);
        
        // Test pattern matching
        var companiesWithCorp = companies.findByNameLikeIgnoreCase("%corp%");
        assertEquals(1, companiesWithCorp.size());
        assertEquals(comp1, companiesWithCorp.get(0).id);
        
        // Test aggregations
        long activeCount = companies.countByActive(true);
        assertEquals(1L, activeCount);
        
        assertTrue(companies.existsByNameIgnoreCase("ACME CORPORATION"));
        assertFalse(companies.existsByNameIgnoreCase("Nonexistent Company"));

        utx.commit();
    }

    @Test
    public void testAddressEnhancedQueries() throws Exception {
        setupTestData();
        utx.begin();

        // Test address query methods
        var springfieldAddresses = addresses.findByCity("Springfield");
        assertEquals(1, springfieldAddresses.size());
        assertEquals(addr1, springfieldAddresses.get(0).id);
        
        var massachusettsAddresses = addresses.findByState("MA");
        assertEquals(1, massachusettsAddresses.size());
        assertEquals(addr1, massachusettsAddresses.get(0).id);
        
        var usAddresses = addresses.findByCountry("US");
        assertEquals(3, usAddresses.size()); // All test addresses are in US
        
        // Test pattern matching on city
        var citiesWithMetro = addresses.findByCityLikeIgnoreCase("%metro%");
        assertEquals(1, citiesWithMetro.size());
        assertEquals(addr2, citiesWithMetro.get(0).id); // Metropolis
        
        // Test aggregations
        long usCount = addresses.countByCountry("US");
        assertEquals(3L, usCount);
        
        assertTrue(addresses.existsByPostalCode("01101"));
        assertFalse(addresses.existsByPostalCode("99999"));

        utx.commit();
    }

    @Test
    public void testComprehensiveCrudOperations() throws Exception {
        clearDatabase();
        utx.begin();

        // Test comprehensive CRUD for all repositories
        
        // Create new entities
        UUID newAddrId = UUID.randomUUID();
        Address newAddr = Address.of(newAddrId, "123 Test St", "TestCity", "TS", "12345", "US");
        addresses.save(newAddr);
        
        UUID newCompId = UUID.randomUUID();
        Company newComp = Company.of(newCompId, "Test Corp", "Testing", true, newAddrId, Instant.now());
        companies.save(newComp);
        
        UUID newEmpId = UUID.randomUUID();
        Employee newEmp = Employee.of(newEmpId, "Test", "User", "test@test.com", newCompId, 
                                     newAddrId, "Tester", new BigDecimal("75000"), true, LocalDate.now());
        employees.save(newEmp);
        
        // Verify saves
        assertTrue(addresses.existsById(newAddrId));
        assertTrue(companies.existsById(newCompId));
        assertTrue(employees.existsById(newEmpId));
        
        // Test counts
        assertEquals(1L, addresses.count());
        assertEquals(1L, companies.count());
        assertEquals(1L, employees.count());
        
        // Test findAll
        assertEquals(1, addresses.findAll().count());
        assertEquals(1, companies.findAll().count());
        assertEquals(1, employees.findAll().count());
        
        // Test updates
        newEmp.salary = new BigDecimal("80000");
        employees.save(newEmp);
        
        var updatedEmp = employees.findById(newEmpId);
        assertTrue(updatedEmp.isPresent());
        assertEquals(new BigDecimal("80000"), updatedEmp.get().salary);
        
        // Test deletes
        employees.deleteById(newEmpId);
        companies.deleteById(newCompId);
        addresses.deleteById(newAddrId);
        
        assertEquals(0L, addresses.count());
        assertEquals(0L, companies.count());
        assertEquals(0L, employees.count());

        utx.commit();
    }

    @Test
    public void testStreamOperationsAndFiltering() throws Exception {
        setupTestData();
        utx.begin();

        // Complex stream operations
        var highEarnersInAcme = employees.findAll()
                .filter(e -> comp1.equals(e.companyId))
                .filter(e -> e.salary != null && e.salary.compareTo(new BigDecimal("100000")) >= 0)
                .sorted(Comparator.comparing((Employee emp) -> emp.salary).reversed())
                .map(e -> e.firstName + " " + e.lastName + " (" + e.salary + ")")
                .collect(Collectors.toList());
        
        assertEquals(2, highEarnersInAcme.size());
        assertTrue(highEarnersInAcme.get(0).contains("Dan Doe")); // Highest salary first
        
        // Test complex filtering and grouping
        var employeesByCompany = employees.findAll()
                .collect(Collectors.groupingBy(e -> e.companyId, Collectors.counting()));
        
        assertEquals(2L, employeesByCompany.size()); // 2 companies
        assertEquals(3L, employeesByCompany.get(comp1).longValue()); // 3 employees in Acme
        assertEquals(2L, employeesByCompany.get(comp2).longValue()); // 2 employees in Globex

        utx.commit();
    }
}