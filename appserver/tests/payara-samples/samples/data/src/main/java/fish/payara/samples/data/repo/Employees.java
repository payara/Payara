package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Employee;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BasicRepository + richer default methods for sample queries like the TCK patterns.
 */
@Repository
public interface Employees extends BasicRepository<Employee, UUID> {

    default List<Employee> findActiveByCompany(UUID companyId) {
        return this.findAll()
                .filter(e -> e.active && companyId.equals(e.companyId))
                .collect(Collectors.toList());
    }

    default List<Employee> findByEmailIgnoreCase(String email) {
        String target = email == null ? "" : email.toLowerCase(Locale.ROOT);
        return this.findAll()
                .filter(e -> e.email != null && e.email.toLowerCase(Locale.ROOT).equals(target))
                .collect(Collectors.toList());
    }

    default List<Employee> findTopNBySalaryDesc(int n) {
        return this.findAll()
                .sorted(Comparator.comparing((Employee e) -> e.salary == null ? BigDecimal.ZERO : e.salary).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    default List<Employee> findByLastNamePrefixSorted(String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return this.findAll()
                .filter(e -> e.lastName != null && e.lastName.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted(Comparator.comparing((Employee e) -> e.lastName).thenComparing(e -> e.firstName))
                .collect(Collectors.toList());
    }

    // Level 2 - Query operators (Between, GreaterThan, In, Like, Null checks)
    
    /**
     * Find employees with salary between min and max (inclusive).
     */
    default List<Employee> findBySalaryBetween(BigDecimal min, BigDecimal max) {
        return this.findAll()
                .filter(e -> e.salary != null && 
                            e.salary.compareTo(min) >= 0 && 
                            e.salary.compareTo(max) <= 0)
                .collect(Collectors.toList());
    }

    /**
     * Find employees with salary greater than the specified minimum.
     */
    default List<Employee> findBySalaryGreaterThan(BigDecimal min) {
        return this.findAll()
                .filter(e -> e.salary != null && e.salary.compareTo(min) > 0)
                .collect(Collectors.toList());
    }

    /**
     * Find employees with salary in the provided list of values.
     */
    default List<Employee> findBySalaryIn(List<BigDecimal> values) {
        return this.findAll()
                .filter(e -> e.salary != null && values.contains(e.salary))
                .collect(Collectors.toList());
    }

    /**
     * Find employees where last name matches the pattern (case insensitive).
     * Pattern should use % as wildcard (e.g., "%son%" for names containing "son").
     */
    default List<Employee> findByLastNameLikeIgnoreCase(String pattern) {
        if (pattern == null) return List.of();
        String regex = pattern.toLowerCase(Locale.ROOT).replace("%", ".*");
        return this.findAll()
                .filter(e -> e.lastName != null && 
                            e.lastName.toLowerCase(Locale.ROOT).matches(regex))
                .collect(Collectors.toList());
    }

    /**
     * Find employees where last name does NOT match the pattern.
     */
    default List<Employee> findByLastNameNotLike(String pattern) {
        if (pattern == null) return this.findAll().collect(Collectors.toList());
        String regex = pattern.toLowerCase(Locale.ROOT).replace("%", ".*");
        return this.findAll()
                .filter(e -> e.lastName == null || 
                            !e.lastName.toLowerCase(Locale.ROOT).matches(regex))
                .collect(Collectors.toList());
    }

    /**
     * Find employees with no address assigned.
     */
    default List<Employee> findByAddressIdIsNull() {
        return this.findAll()
                .filter(e -> e.addressId == null)
                .collect(Collectors.toList());
    }

    /**
     * Find employees with an address assigned.
     */
    default List<Employee> findByAddressIdIsNotNull() {
        return this.findAll()
                .filter(e -> e.addressId != null)
                .collect(Collectors.toList());
    }

    // Boolean combinations (AND/OR)
    
    /**
     * Find employees matching both first name AND last name.
     */
    default List<Employee> findByFirstNameAndLastName(String firstName, String lastName) {
        return this.findAll()
                .filter(e -> (firstName == null ? e.firstName == null : firstName.equals(e.firstName)) &&
                            (lastName == null ? e.lastName == null : lastName.equals(e.lastName)))
                .collect(Collectors.toList());
    }

    /**
     * Find employees matching first name OR last name.
     */
    default List<Employee> findByFirstNameOrLastName(String firstName, String lastName) {
        return this.findAll()
                .filter(e -> (firstName != null && firstName.equals(e.firstName)) ||
                            (lastName != null && lastName.equals(e.lastName)))
                .collect(Collectors.toList());
    }

    // Aggregations (count/exists)
    
    /**
     * Count employees by active status.
     */
    default long countByActive(boolean active) {
        return this.findAll()
                .filter(e -> e.active == active)
                .count();
    }

    /**
     * Check if employee with given email exists (case insensitive).
     */
    default boolean existsByEmailIgnoreCase(String email) {
        String target = email == null ? "" : email.toLowerCase(Locale.ROOT);
        return this.findAll()
                .anyMatch(e -> e.email != null && e.email.toLowerCase(Locale.ROOT).equals(target));
    }

    // Pagination/slicing with ordering
    
    /**
     * Find all employees ordered by salary descending with offset and limit.
     */
    default List<Employee> findAllOrderBySalaryDesc(int offset, int limit) {
        return this.findAll()
                .sorted(Comparator.comparing((Employee e) -> e.salary == null ? BigDecimal.ZERO : e.salary).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Find employees by company ordered by last name ascending with offset and limit.
     */
    default List<Employee> findByCompanyOrderByLastNameAsc(UUID companyId, int offset, int limit) {
        return this.findAll()
                .filter(e -> companyId.equals(e.companyId))
                .sorted(Comparator.comparing((Employee e) -> e.lastName == null ? "" : e.lastName))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    // Add missing count() and existsById() methods
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
}
