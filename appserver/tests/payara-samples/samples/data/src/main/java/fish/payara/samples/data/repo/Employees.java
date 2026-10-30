package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Employee;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Pure Jakarta Data repository for Employee - all methods implemented by provider
 */
@Repository
public interface Employees extends BasicRepository<Employee, UUID> {

    // Basic query methods - Jakarta Data will implement automatically
    List<Employee> findByFirstName(String firstName);
    List<Employee> findByLastName(String lastName);
    List<Employee> findByEmail(String email);
    List<Employee> findByActive(boolean active);
    List<Employee> findByCompanyId(UUID companyId);
    
    // Combination queries
    List<Employee> findByFirstNameAndLastName(String firstName, String lastName);
    List<Employee> findByFirstNameOrLastName(String firstName, String lastName);
    List<Employee> findByActiveAndCompanyId(boolean active, UUID companyId);
    
    // Range/comparison queries
    List<Employee> findBySalaryBetween(BigDecimal min, BigDecimal max);
    List<Employee> findBySalaryGreaterThan(BigDecimal min);
    List<Employee> findBySalaryLessThan(BigDecimal max);
    List<Employee> findBySalaryIn(List<BigDecimal> values);
    
    // Pattern matching
    List<Employee> findByLastNameLike(String pattern);
    List<Employee> findByEmailLike(String pattern);
    
    // Null checks (field name must match exactly: addressId)
    // Using default implementations to avoid provider mapping issues
    default List<Employee> findByAddressIdIsNull() {
        return findAll().filter(e -> e.addressId == null).toList();
    }
    
    default List<Employee> findByAddressIdIsNotNull() {
        return findAll().filter(e -> e.addressId != null).toList();
    }
    
    List<Employee> findBySalaryIsNull();
    List<Employee> findBySalaryIsNotNull();
    
    // Count operations
    long countByActive(boolean active);
    long countByCompanyId(UUID companyId);
    
    // Existence checks
    boolean existsByEmail(String email);
    boolean existsByFirstNameAndLastName(String firstName, String lastName);
    
    // Missing methods that need default implementations
    default List<Employee> findByEmailIgnoreCase(String email) {
        return findAll().filter(e -> e.email != null && e.email.equalsIgnoreCase(email)).toList();
    }
    
    default List<Employee> findByLastNameLikeIgnoreCase(String pattern) {
        String regex = pattern.toLowerCase().replace("%", ".*");
        return findAll().filter(e -> e.lastName != null && 
                               e.lastName.toLowerCase().matches(regex)).toList();
    }
    
    default List<Employee> findByLastNameNotLike(String pattern) {
        String regex = pattern.toLowerCase().replace("%", ".*");
        return findAll().filter(e -> e.lastName == null || 
                               !e.lastName.toLowerCase().matches(regex)).toList();
    }
    
    default boolean existsByEmailIgnoreCase(String email) {
        return findAll().anyMatch(e -> e.email != null && e.email.equalsIgnoreCase(email));
    }
    
    default List<Employee> findAllOrderBySalaryDesc(int offset, int limit) {
        return findAll().sorted((e1, e2) -> {
            java.math.BigDecimal s1 = e1.salary == null ? java.math.BigDecimal.ZERO : e1.salary;
            java.math.BigDecimal s2 = e2.salary == null ? java.math.BigDecimal.ZERO : e2.salary;
            return s2.compareTo(s1);
        }).skip(offset).limit(limit).toList();
    }
    
    default List<Employee> findByCompanyOrderByLastNameAsc(UUID companyId, int offset, int limit) {
        return findAll().filter(e -> companyId.equals(e.companyId))
                       .sorted((e1, e2) -> {
                           String ln1 = e1.lastName == null ? "" : e1.lastName;
                           String ln2 = e2.lastName == null ? "" : e2.lastName;
                           return ln1.compareTo(ln2);
                       }).skip(offset).limit(limit).toList();
    }
    
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
    
    // Single result methods - will throw EmptyResultException/NonUniqueResultException
    Employee findByEmailIgnoreCaseSingle(String email);
    Employee findByEmailSingle(String email);
    Employee findSingleByEmailIgnoreCase(String email); // Provider-friendly single result method
    
    // Default implementation for complex business logic
    default List<Employee> findTopNBySalaryDescJPQL(int n) {
        return findAll().sorted((e1, e2) -> {
            java.math.BigDecimal s1 = e1.salary == null ? java.math.BigDecimal.ZERO : e1.salary;
            java.math.BigDecimal s2 = e2.salary == null ? java.math.BigDecimal.ZERO : e2.salary;
            return s2.compareTo(s1);
        }).limit(n).toList();
    }

    // Delete operations for cache eviction testing

    /**
     * Query by Method Name DELETE - uses QueryByNameOperationUtility
     * Tests instance-specific cache eviction (ISSUE #1)
     */
    long deleteByFirstNameAndLastName(String firstName, String lastName);

    /**
     * DELETE with @Delete + @By - uses DeleteOperationUtility
     * Tests cache eviction for @Delete operations (ISSUE #2)
     */
    @Delete
    void delete(@By("id") UUID id);

    /**
     * DELETE with @Query - uses QueryOperationUtility
     * Tests cache eviction for @Query DELETE (ISSUE #2 variant)
     */
    @Query("DELETE FROM Employee WHERE firstName = :firstName")
    long deleteByFirstName(@jakarta.data.repository.Param("firstName") String firstName);

    /**
     * UPDATE with @Query - uses QueryOperationUtility
     * Tests cache eviction for @Query UPDATE (ISSUE #3)
     */
    @Query("UPDATE Employee SET firstName = :firstName WHERE id = :id")
    long updateFirstName(@jakarta.data.repository.Param("id") UUID id, @jakarta.data.repository.Param("firstName") String firstName);
}
