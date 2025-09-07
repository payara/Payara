package fish.payara.samples.data.support;

import fish.payara.samples.data.entity.Rectangle;
import fish.payara.samples.data.repo.Rectangles;
import jakarta.persistence.EntityManager;

import java.util.UUID;

/**
 * JPA implementation of the Rectangles repository.
 * Used for testing Bean Validation scenarios.
 */
public class JpaRectangles extends JpaBasicRepository<Rectangle, UUID> implements Rectangles {
    
    public JpaRectangles(EntityManager entityManager) {
        super(entityManager, Rectangle.class);
    }
}