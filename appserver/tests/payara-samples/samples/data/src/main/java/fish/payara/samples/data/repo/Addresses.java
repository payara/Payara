package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Address;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Pure Jakarta Data repository for Address - all methods implemented by provider
 */
@Repository
public interface Addresses extends BasicRepository<Address, UUID> {
    
    // Basic query methods - Jakarta Data will implement automatically
    List<Address> findByStreet(String street);
    List<Address> findByCity(String city);
    List<Address> findByState(String state);
    List<Address> findByCountry(String country);
    List<Address> findByPostalCode(String postalCode);
    
    // Combination queries
    List<Address> findByCityAndState(String city, String state);
    List<Address> findByStateAndCountry(String state, String country);
    List<Address> findByCityAndStateAndCountry(String city, String state, String country);
    
    // Pattern matching
    List<Address> findByStreetLike(String pattern);
    List<Address> findByCityLike(String pattern);
    List<Address> findByPostalCodeLike(String pattern);
    
    // Count operations
    long countByCity(String city);
    long countByState(String state);
    long countByCountry(String country);
    
    // Existence checks
    boolean existsByPostalCode(String postalCode);
    boolean existsByCityAndState(String city, String state);
    
    // Missing methods that need default implementations
    default List<Address> findByCityLikeIgnoreCase(String pattern) {
        String regex = pattern.toLowerCase().replace("%", ".*");
        return findAll().filter(a -> a.city != null && 
                               a.city.toLowerCase().matches(regex)).toList();
    }
    
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
}