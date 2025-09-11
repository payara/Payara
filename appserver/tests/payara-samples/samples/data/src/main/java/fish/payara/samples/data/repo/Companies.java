package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Company;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Pure Jakarta Data repository for Company - all methods implemented by provider
 */
@Repository
public interface Companies extends BasicRepository<Company, UUID> {
    
    // Basic query methods - Jakarta Data will implement automatically
    List<Company> findByName(String name);
    List<Company> findByIndustry(String industry);
    List<Company> findByDescription(String description);
    
    // Pattern matching
    List<Company> findByNameLike(String pattern);
    List<Company> findByIndustryLike(String pattern);
    List<Company> findByDescriptionLike(String pattern);
    
    // Combination queries
    List<Company> findByNameAndIndustry(String name, String industry);
    
    // Count operations
    long countByIndustry(String industry);
    
    // Existence checks
    boolean existsByName(String name);
    boolean existsByNameAndIndustry(String name, String industry);
    
    // Missing methods that need default implementations
    List<Company> findByActive(boolean active);
    
    default List<Company> findByNameContainsIgnoreCase(String token) {
        String t = token == null ? "" : token.toLowerCase();
        return findAll().filter(c -> c.name != null && 
                               c.name.toLowerCase().contains(t)).toList();
    }
    
    default List<Company> findByNameLikeIgnoreCase(String pattern) {
        String regex = pattern.toLowerCase().replace("%", ".*");
        return findAll().filter(c -> c.name != null && 
                               c.name.toLowerCase().matches(regex)).toList();
    }
    
    default long countByActive(boolean active) {
        return findAll().filter(c -> c.active == active).count();
    }
    
    default boolean existsByNameIgnoreCase(String name) {
        return findAll().anyMatch(c -> c.name != null && c.name.equalsIgnoreCase(name));
    }
    
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
}