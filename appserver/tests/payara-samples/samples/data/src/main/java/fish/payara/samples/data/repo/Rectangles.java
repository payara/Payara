package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Rectangle;
import jakarta.data.repository.BasicRepository;

import java.util.List;
import java.util.UUID;

public interface Rectangles extends BasicRepository<Rectangle, UUID> {
    
    // Query methods implemented as default stream-based operations
    default List<Rectangle> findByName(String name) {
        return findAll()
            .filter(r -> name.equals(r.name))
            .toList();
    }
    
    default List<Rectangle> findByWidthGreaterThan(int width) {
        return findAll()
            .filter(r -> r.width > width)
            .toList();
    }
    
    default List<Rectangle> findSquares() {
        return findAll()
            .filter(Rectangle::isSquare)
            .toList();
    }
    
    default List<Rectangle> findByAreaGreaterThan(long area) {
        return findAll()
            .filter(r -> r.getArea() > area)
            .toList();
    }
    
    default long countByWidth(int width) {
        return findAll()
            .filter(r -> r.width == width)
            .count();
    }
    
    default boolean existsByName(String name) {
        return findAll()
            .anyMatch(r -> name.equals(r.name));
    }
    
    // Add missing count() and existsById() methods
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
}