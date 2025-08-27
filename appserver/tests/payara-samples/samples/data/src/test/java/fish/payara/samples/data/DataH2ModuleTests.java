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
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class DataH2ModuleTests {

    private EntityManagerFactory emf;
    private EntityManager em;

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

    @BeforeEach
    void setup() {
        emf = Persistence.createEntityManagerFactory("samples-dataPU");
        em = emf.createEntityManager();

        addresses = new JpaAddresses(em);
        companies = new JpaCompanies(em);
        employees = new JpaEmployees(em);

        addresses.saveAll(List.of(
                Address.of(addr1, "1 First St", "Springfield", "MA", "01101", "US"),
                Address.of(addr2, "2 Second Ave", "Metropolis", "NY", "10001", "US"),
                Address.of(addr3, "3 Third Blvd", "Gotham", "NJ", "07001", "US")
        ));

        companies.saveAll(List.of(
                Company.of(comp1, "Acme Corporation", "Manufacturing", true, addr1, Instant.parse("2022-01-01T00:00:00Z")),
                Company.of(comp2, "Globex LLC", "Technology", false, addr2, Instant.parse("2021-12-31T00:00:00Z"))
        ));

        employees.saveAll(List.of(
                Employee.of(emp1, "Alice", "Anderson", "alice@acme.com", comp1, addr1, "Developer", new BigDecimal("95000"), true, LocalDate.of(2021,1,10)),
                Employee.of(emp2, "Bob", "Barker", "bob@acme.com", comp1, addr1, "Developer", new BigDecimal("105000"), true, LocalDate.of(2020,5,1)),
                Employee.of(emp3, "Carol", "Clark", "carol@globex.com", comp2, addr2, "QA Engineer", new BigDecimal("87000"), false, LocalDate.of(2022,3,15)),
                Employee.of(emp4, "Dan", "Doe", "dan@acme.com", comp1, addr3, "Manager", new BigDecimal("130000"), true, LocalDate.of(2019,9,9)),
                Employee.of(emp5, "Eve", "Evans", "eve@globex.com", comp2, addr2, "Developer", new BigDecimal("99000"), true, LocalDate.of(2023,1,5))
        ));
    }

    @AfterEach
    void teardown() {
        if (em != null) em.close();
        if (emf != null) emf.close();
    }

    @Test
    void testBasicRepositoryBuiltInMethods_Employees() {
        assertEquals(5, employees.findAll().count());

        // findById
        Employee e2 = employees.findById(emp2).orElseThrow();
        assertEquals("Bob", e2.firstName);
        assertEquals(new BigDecimal("105000"), e2.salary);

        // save (update)
        e2.salary = new BigDecimal("107500");
        employees.save(e2);
        assertEquals(new BigDecimal("107500"), employees.findById(emp2).orElseThrow().salary);

        // save (create)
        UUID emp6 = UUID.randomUUID();
        employees.save(Employee.of(emp6, "Frank", "Foley", "frank@acme.com",
                comp1, addr3, "Ops", new BigDecimal("80000"), true, LocalDate.of(2024, 6, 30)));
        assertTrue(employees.findById(emp6).isPresent());

        // delete(E)
        employees.delete(e2);
        assertTrue(employees.findById(emp2).isEmpty());

        // deleteById
        employees.deleteById(emp3);
        assertTrue(employees.findById(emp3).isEmpty());

        // deleteAll(Iterable)
        var rest = employees.findAll().collect(Collectors.toList());
        employees.deleteAll(rest);
        assertEquals(0, employees.findAll().count());
    }

    @Test
    void testBasicRepositoryBuiltInMethods_Addresses() {
        List<Address> list = addresses.findAll()
                .sorted(Comparator.comparing(a -> a.id))
                .collect(Collectors.toList());
        assertEquals(3, list.size());
        assertEquals(addr1, list.get(0).id);

        addresses.deleteById(addr1);
        assertTrue(addresses.findById(addr1).isEmpty());

        addresses.save(Address.of(addr1, "1 First St", "Springfield", "MA", "01101", "US"));
        assertTrue(addresses.findById(addr1).isPresent());
    }

    @Test
    void testCompaniesContainsIgnoreCase() {
        var active = ((JpaCompanies) companies).findByNameContainsIgnoreCase("ACME");
        assertEquals(1, active.size());
        assertEquals(comp1, active.get(0).id);

        var byName = companies.findByNameContainsIgnoreCase("glob");
        assertEquals(1, byName.size());
        assertEquals(comp2, byName.get(0).id);
    }

    @Test
    void testEmployeesQueriesInspiredByTCK() {
        // active by company (JPQL)
        var acmeActive = ((JpaEmployees) employees).findActiveByCompanyJPQL(comp1)
                .stream().map(e -> e.email).collect(Collectors.toList());
        assertEquals(List.of("alice@acme.com", "dan@acme.com"), acmeActive);

        // ignore-case (lista)
        var byEmail = employees.findByEmailIgnoreCase("EVE@globex.com");
        assertEquals(1, byEmail.size());
        assertEquals(emp5, byEmail.get(0).id);

        // ignore-case "single" com exceções como no TCK
        assertDoesNotThrow(() -> ((JpaEmployees) employees).findByEmailIgnoreCaseSingle("alice@acme.com"));
        assertThrows(jakarta.data.exceptions.EmptyResultException.class,
                () -> ((JpaEmployees) employees).findByEmailIgnoreCaseSingle("nobody@acme.com"));

        // Provoque NonUniqueResultException inserindo duplicata de email (com case diferente)
        UUID dupId = UUID.randomUUID();
        employees.save(Employee.of(dupId, "ALICE", "ANDERSON", "ALICE@ACME.COM",
                comp1, addr1, "Developer", new BigDecimal("95000"), true, LocalDate.of(2021,1,11)));
        assertThrows(jakarta.data.exceptions.NonUniqueResultException.class,
                () -> ((JpaEmployees) employees).findByEmailIgnoreCaseSingle("alice@acme.com"));
    }

    @Test
    void testTopNBySalary_Descending() {
        var top3 = ((JpaEmployees) employees).findTopNBySalaryDescJPQL(3);
        assertEquals(List.of(emp4, emp2, emp5),
                top3.stream().map(e -> e.id).collect(Collectors.toList()));
    }

    @Test
    void testMixedSortingViaStream() {
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
    }

    @Test
    void testEdgeCases_emptyAndNotFound() {
        // Novo EM/Repos vazios
        try (var ignored = newAutoCloseableEM()) {
            EntityManager em2 = ignored;
            Employees emptyEmployees = new JpaEmployees(em2);
            assertEquals(0, emptyEmployees.findAll().count());
            assertTrue(emptyEmployees.findById(UUID.randomUUID()).isEmpty());

            // Deletar inexistentes
            emptyEmployees.deleteById(UUID.randomUUID());
            emptyEmployees.deleteAll(List.of());
        }
    }

    private EntityManager newAutoCloseableEM() {
        return emf.createEntityManager();
    }
}