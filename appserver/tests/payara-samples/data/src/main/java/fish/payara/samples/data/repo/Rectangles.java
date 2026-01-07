package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Rectangle;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

/**
 * Pure Jakarta Data repository for Rectangle - all methods implemented by provider
 */
@Repository
public interface Rectangles extends BasicRepository<Rectangle, UUID> {
    
    // Override save methods with @Valid validation like TCK  
    @Save
    Rectangle save(@Valid Rectangle entity);
    
    @Save  
    <S extends Rectangle> Iterable<S> saveAll(@Valid Iterable<S> entities);
    
    // Basic query methods - Jakarta Data will implement automatically
    List<Rectangle> findByName(String name);
    List<Rectangle> findByWidth(int width);
    List<Rectangle> findByHeight(int height);
    
    // Comparison queries
    List<Rectangle> findByWidthGreaterThan(int width);
    List<Rectangle> findByWidthLessThan(int width);
    List<Rectangle> findByHeightGreaterThan(int height);
    List<Rectangle> findByHeightLessThan(int height);
    List<Rectangle> findByWidthBetween(int minWidth, int maxWidth);
    List<Rectangle> findByHeightBetween(int minHeight, int maxHeight);
    
    // Combination queries
    List<Rectangle> findByWidthAndHeight(int width, int height);
    List<Rectangle> findByNameAndWidth(String name, int width);
    
    // Pattern matching
    List<Rectangle> findByNameLike(String pattern);
    
    // Count operations
    long countByWidth(int width);
    long countByHeight(int height);
    
    // Existence checks
    boolean existsByName(String name);
    boolean existsByWidthAndHeight(int width, int height);
    
    // Missing methods that need default implementations
    default List<Rectangle> findSquares() {
        return findAll().filter(Rectangle::isSquare).toList();
    }
    
    default List<Rectangle> findByAreaGreaterThan(int area) {
        return findAll().filter(r -> r.getArea() > area).toList();
    }
    
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
}