package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Box;
import jakarta.data.Order;
import jakarta.data.repository.BasicRepository;

import java.util.List;
import java.util.stream.Stream;

public interface Boxes extends BasicRepository<Box, String> {
    
    // Query methods implemented as default stream-based operations
    default List<Box> findByName(String name) {
        return findAll()
            .filter(b -> name.equals(b.name))
            .toList();
    }
    
    default List<Box> findByMaterial(String material) {
        return findAll()
            .filter(b -> material.equals(b.material))
            .toList();
    }
    
    default List<Box> findByColor(String color) {
        return findAll()
            .filter(b -> color.equals(b.color))
            .toList();
    }
    
    default List<Box> findByStackable(boolean stackable) {
        return findAll()
            .filter(b -> b.stackable == stackable)
            .toList();
    }
    
    default List<Box> findCubes() {
        return findAll()
            .filter(Box::isCube)
            .toList();
    }
    
    default List<Box> findLargeBoxes() {
        return findAll()
            .filter(b -> b.getVolume() > 100000L)
            .toList();
    }
    
    default List<Box> findByVolumeGreaterThan(Long volume) {
        return findAll()
            .filter(b -> b.getVolume() > volume)
            .toList();
    }
    
    default List<Box> findByMaterialAndColor(String material, String color) {
        return findAll()
            .filter(b -> material.equals(b.material) && color.equals(b.color))
            .toList();
    }
    
    default long countByMaterial(String material) {
        return findAll()
            .filter(b -> material.equals(b.material))
            .count();
    }
    
    default boolean existsByName(String name) {
        return findAll()
            .anyMatch(b -> name.equals(b.name));
    }
    
    default boolean existsStackableBoxes() {
        return findAll()
            .anyMatch(b -> b.stackable);
    }
    
    // Add missing count() and existsById() methods
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(String id) {
        return findById(id).isPresent();
    }
    
    // TCK-like bulk operations
    Box[] addMultiple(Box... boxes);
    Box[] modifyMultiple(Box... boxes);
    void removeMultiple(Box... boxes);
    
    // TCK-like bulk delete with count
    long deleteByCodeLike(String pattern);
    
    // TCK-like methods with Order support
    List<Box> findByCodeBetween(String from, String to, Order order);
    
    // TCK-like custom query methods (implemented in JpaBoxes)
    Stream<Box> findByVolumeWithFactorBetween(double min, double max, double factor);
    
    // TCK-like custom query methods (implemented in JpaBoxes)
    List<Box> findByNameLengthAndVolumeBelow(int nameLength, double maxVolume);
}