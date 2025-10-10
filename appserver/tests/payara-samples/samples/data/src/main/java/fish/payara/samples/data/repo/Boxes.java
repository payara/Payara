package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Box;
import jakarta.data.Order;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Boxes extends BasicRepository<Box, String> {

    // ---------- Utilities for ID "code" ----------
    default boolean existsByCode(String code) {
        return this.findById(code).isPresent();
    }

    default Optional<Box> findByCode(String code) {
        return this.findById(code);
    }

    // Single delete: should throw OptimisticLockingFailureException if it doesn't exist
    default void deleteByCode(String code) {
        // Check if exists before trying to delete
        if (!existsByCode(code)) {
            throw new OptimisticLockingFailureException("Entity with code '" + code + "' not found for deletion");
        }
        this.deleteById(code);
    }

    // Optional: standardize delete(E) to also throw if it doesn't exist
    // (Some providers handle delete(E) more "tolerantly")
    default void delete(Box entity) {
        if (entity == null || entity.code == null) {
            throw new OptimisticLockingFailureException("Attempted to delete an entity that does not exist in the database.");
        }
        // Confirm non-existence before delegating
        boolean exists;
        try {
            exists = this.existsById(entity.code);
        } catch (Throwable t) {
            exists = this.findById(entity.code).isPresent();
        }
        if (!exists) {
            throw new OptimisticLockingFailureException("Attempted to delete an entity that does not exist in the database.");
        }
        this.deleteById(entity.code); // remove existing
    }

    // ---------- LIKE + bulk delete via iteration ----------
    List<Box> findByCodeLike(String pattern);

    default long deleteByCodeLike(String pattern) {
        List<Box> list = findByCodeLike(pattern);
        long count = 0L;
        for (Box b : list) {
            // Only call delete when it exists; avoid marking transaction as rollback
            boolean exists;
            try {
                exists = this.existsById(b.code);
            } catch (Throwable t) {
                exists = this.findById(b.code).isPresent();
            }
            if (exists) {
                this.deleteById(b.code);
                count++;
            }
        }
        return count;
    }

    // removeMultiple: remove existing ones; ignore non-existent; preserve order; don't throw
    default Box[] removeMultiple(Box... items) {
        if (items == null) return new Box[0];
        for (Box b : items) {
            if (b == null || b.code == null) continue;
            // Try to delete directly; ignore silently if it doesn't exist (TCK bulk delete behavior)
            try {
                this.deleteById(b.code);
            } catch (Exception e) {
                // Ignore any exception - bulk operations should be tolerant
            }
        }
        return items; // maintain input order (TCK-style)
    }

    // Bulk operations methods - TCK style implementation
    default Box[] addMultiple(Box... boxes) {
        Box[] result = new Box[boxes.length];
        for (int i = 0; i < boxes.length; i++) {
            result[i] = save(boxes[i]);
        }
        return result;
    }
    
    default Box[] modifyMultiple(Box... boxes) {
        Box[] result = new Box[boxes.length];
        for (int i = 0; i < boxes.length; i++) {
            result[i] = save(boxes[i]);
        }
        return result;
    }

    // ---------- Queries used in tests ----------
    List<Box> findByName(String name);
    List<Box> findByMaterial(String material);
    List<Box> findByColor(String color);
    List<Box> findByStackable(boolean stackable);
    List<Box> findByMaterialAndColor(String material, String color);
    long countByMaterial(String material);
    boolean existsByName(String name);

    default boolean existsStackableBoxes() {
        return findAll().anyMatch(b -> Boolean.TRUE.equals(b.stackable));
    }

    default List<Box> findByCodeBetween(String from, String to, Order order) {
        return findAll()
                .filter(b -> b.code.compareTo(from) >= 0 && b.code.compareTo(to) <= 0)
                .sorted((a, b) -> a.code.compareTo(b.code))
                .toList();
    }

    // Volume/category methods as defaults
    default List<Box> findCubes() {
        return findAll().filter(Box::isCube).toList();
    }
    
    default List<Box> findLargeBoxes() {
        return findAll().filter(Box::isLarge).toList();
    }
    
    default List<Box> findByVolumeGreaterThan(long volume) {
        return findAll().filter(b -> b.getVolume() > volume).toList();
    }
    
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(String code) {
        return findById(code).isPresent();
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