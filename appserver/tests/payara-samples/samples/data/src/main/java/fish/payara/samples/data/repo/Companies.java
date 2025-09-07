package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Company;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BasicRepository + some default methods to mimic TCK-style custom capabilities without a provider.
 */
@Repository
public interface Companies extends BasicRepository<Company, UUID> {

    /**
     * Example: case-insensitive contains on company name.
     * Implemented as a default method using findAll().
     */
    default List<Company> findByNameContainsIgnoreCase(String token) {
        String t = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return this.findAll()
                .filter(c -> c.name != null && c.name.toLowerCase(Locale.ROOT).contains(t))
                .collect(Collectors.toList());
    }

    /**
     * Example: active companies only.
     */
    default List<Company> findActive() {
        return this.findAll().filter(c -> c.active).collect(Collectors.toList());
    }

    // Additional query methods for comprehensive TCK coverage

    /**
     * Find companies by industry (exact match).
     */
    default List<Company> findByIndustry(String industry) {
        return this.findAll()
                .filter(c -> industry == null ? c.industry == null : industry.equals(c.industry))
                .collect(Collectors.toList());
    }

    /**
     * Find companies by active status.
     */
    default List<Company> findByActive(boolean active) {
        return this.findAll()
                .filter(c -> c.active == active)
                .collect(Collectors.toList());
    }

    /**
     * Find companies where industry is null.
     */
    default List<Company> findByIndustryIsNull() {
        return this.findAll()
                .filter(c -> c.industry == null)
                .collect(Collectors.toList());
    }

    /**
     * Find companies where industry is not null.
     */
    default List<Company> findByIndustryIsNotNull() {
        return this.findAll()
                .filter(c -> c.industry != null)
                .collect(Collectors.toList());
    }

    /**
     * Find companies where name matches pattern (case insensitive).
     * Pattern should use % as wildcard.
     */
    default List<Company> findByNameLikeIgnoreCase(String pattern) {
        if (pattern == null) return List.of();
        String regex = pattern.toLowerCase(Locale.ROOT).replace("%", ".*");
        return this.findAll()
                .filter(c -> c.name != null && 
                            c.name.toLowerCase(Locale.ROOT).matches(regex))
                .collect(Collectors.toList());
    }

    /**
     * Count companies by active status.
     */
    default long countByActive(boolean active) {
        return this.findAll()
                .filter(c -> c.active == active)
                .count();
    }

    /**
     * Check if company with given name exists (case insensitive).
     */
    default boolean existsByNameIgnoreCase(String name) {
        String target = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return this.findAll()
                .anyMatch(c -> c.name != null && c.name.toLowerCase(Locale.ROOT).equals(target));
    }
    
    // Add missing count() and existsById() methods
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
}
