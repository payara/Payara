package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Box;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Boxes extends BasicRepository<Box, String> {

    // ---------- Métodos usados pelos testes ----------
    
    // Basic query methods - Jakarta Data will implement automatically
    List<Box> findByName(String name);
    List<Box> findByMaterial(String material);
    List<Box> findByColor(String color);
    List<Box> findByStackable(boolean stackable);
    List<Box> findByMaterialAndColor(String material, String color);
    
    long countByMaterial(String material);
    boolean existsByName(String name);

    // LIKE pattern support - implemented by provider
    List<Box> findByCodeLike(String pattern);

    // Custom ID field methods - implemented as defaults to avoid provider dependency
    default boolean existsByCode(String code) {
        return this.findById(code).isPresent();
    }

    default Optional<Box> findByCode(String code) {
        return this.findById(code);
    }

    // Returns 1 if deleted, 0 if not found (TCK-style compatible)
    default long deleteByCode(String code) {
        return this.findById(code)
                .map(b -> {
                    this.delete(b);
                    return 1L;
                })
                .orElse(0L);
    }

    // LIKE delete using find + delete combination (no bulk JPQL dependency)
    default long deleteByCodeLike(String pattern) {
        List<Box> list = findByCodeLike(pattern);
        for (Box b : list) {
            this.delete(b);
        }
        return list.size();
    }

    // Volume/category methods as defaults
    default List<Box> findCubes() {
        return findAll().filter(Box::isCube).toList();
    }
    
    default List<Box> findLargeBoxes() {
        return findAll().filter(Box::isLarge).toList();
    }
    
    default List<Box> findSmallBoxes() {
        return findAll().filter(Box::isSmall).toList();
    }
    
    default List<Box> findByVolumeGreaterThan(long volume) {
        return findAll().filter(b -> b.getVolume() > volume).toList();
    }
    
    default boolean existsStackableBoxes() {
        return findAll().anyMatch(b -> Boolean.TRUE.equals(b.stackable));
    }
    
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(String code) {
        return findById(code).isPresent();
    }

    // Methods used by tests - simplified implementations
    default Box[] addMultiple(Box... boxes) {
        for (Box box : boxes) {
            save(box);
        }
        return boxes;
    }
    
    default Box[] modifyMultiple(Box... boxes) {
        for (Box box : boxes) {
            save(box);
        }
        return boxes;
    }
    
    default void removeMultiple(Box... boxes) {
        for (Box box : boxes) {
            delete(box);
        }
    }
    
    // Order support - simplified
    default List<Box> findByCodeBetween(String from, String to, Object order) {
        return findAll()
                .filter(b -> b.code.compareTo(from) >= 0 && b.code.compareTo(to) <= 0)
                .sorted((a, b) -> a.code.compareTo(b.code))
                .toList();
    }
    
    // Volume factor method - simplified
    default java.util.stream.Stream<Box> findByVolumeWithFactorBetween(double min, double max, double factor) {
        return findAll()
                .filter(b -> {
                    double volume = factor * b.getVolume();
                    return volume >= min && volume <= max;
                });
    }
    
    // Name length and volume method - simplified  
    default List<Box> findByNameLengthAndVolumeBelow(int nameLength, double maxVolume) {
        return findAll()
                .filter(b -> b.name != null && b.name.length() == nameLength && b.getVolume() < maxVolume)
                .toList();
    }
}