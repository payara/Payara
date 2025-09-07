package fish.payara.samples.data.repo;


import fish.payara.samples.data.entity.Address;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/**
 * A repository that inherits from the built-in BasicRepository and adds query methods for comprehensive testing.
 */
@Repository
public interface Addresses extends BasicRepository<Address, UUID> {
    
    /**
     * Find addresses by city (exact match).
     */
    default List<Address> findByCity(String city) {
        return this.findAll()
                .filter(a -> city == null ? a.city == null : city.equals(a.city))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find addresses by state (exact match).
     */
    default List<Address> findByState(String state) {
        return this.findAll()
                .filter(a -> state == null ? a.state == null : state.equals(a.state))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find addresses by country (exact match).
     */
    default List<Address> findByCountry(String country) {
        return this.findAll()
                .filter(a -> country == null ? a.country == null : country.equals(a.country))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find addresses where city matches pattern (case insensitive).
     * Pattern should use % as wildcard.
     */
    default List<Address> findByCityLikeIgnoreCase(String pattern) {
        if (pattern == null) return java.util.List.of();
        String regex = pattern.toLowerCase(java.util.Locale.ROOT).replace("%", ".*");
        return this.findAll()
                .filter(a -> a.city != null && 
                            a.city.toLowerCase(java.util.Locale.ROOT).matches(regex))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Count addresses by country.
     */
    default long countByCountry(String country) {
        return this.findAll()
                .filter(a -> country == null ? a.country == null : country.equals(a.country))
                .count();
    }

    /**
     * Check if address with given postal code exists.
     */
    default boolean existsByPostalCode(String postalCode) {
        return this.findAll()
                .anyMatch(a -> postalCode == null ? a.postalCode == null : postalCode.equals(a.postalCode));
    }
    
    // Add missing count() and existsById() methods
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
}
