package fish.payara.samples.data.support;

import fish.payara.samples.data.entity.Box;
import fish.payara.samples.data.repo.Boxes;
import jakarta.persistence.EntityManager;

/**
 * JPA implementation of the Boxes repository.
 * Used for testing custom ID field name scenarios (using 'code' instead of 'id').
 */
public class JpaBoxes extends JpaBasicRepository<Box, String> implements Boxes {
    
    public JpaBoxes(EntityManager entityManager) {
        super(entityManager, Box.class);
    }

    /**
     * Find box by code (ID field).
     * This demonstrates that the repository can handle custom ID field names.
     */
    public java.util.Optional<Box> findByCode(String code) {
        return findById(code);
    }

    /**
     * Delete box by code (ID field).
     */
    public void deleteByCode(String code) {
        deleteById(code);
    }

    /**
     * Check if box exists by code (ID field).
     */
    public boolean existsByCode(String code) {
        return existsById(code);
    }
}