package fish.payara.samples.data.support;

import fish.payara.samples.data.entity.Box;
import fish.payara.samples.data.repo.Boxes;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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

    @Override
    public Box[] addMultiple(Box... boxes) {
        try {
            Box[] result = new Box[boxes.length];
            for (int i = 0; i < boxes.length; i++) {
                Box box = boxes[i];
                if (box.code == null) {
                    throw new IllegalArgumentException("Box code cannot be null");
                }
                em.persist(box);
                result[i] = box;
            }
            em.flush();
            return result;
        } catch (RuntimeException e) {
            throw DataExceptionMapper.map(e);
        }
    }

    @Override
    public Box[] modifyMultiple(Box... boxes) {
        try {
            Box[] result = new Box[boxes.length];
            for (int i = 0; i < boxes.length; i++) {
                result[i] = em.merge(boxes[i]);
            }
            em.flush();
            return result;
        } catch (RuntimeException e) {
            throw DataExceptionMapper.map(e);
        }
    }

    @Override
    public void removeMultiple(Box... boxes) {
        try {
            for (Box box : boxes) {
                Box found = em.find(Box.class, box.code);
                if (found == null) {
                    throw new jakarta.data.exceptions.OptimisticLockingFailureException(
                        "Entity not found for deletion: " + box.code);
                }
                em.remove(found);
            }
            em.flush();
        } catch (RuntimeException e) {
            throw DataExceptionMapper.map(e);
        }
    }

    @Override
    public long deleteByCodeLike(String pattern) {
        try {
            return em.createQuery("DELETE FROM Box b WHERE b.code LIKE :pattern")
                    .setParameter("pattern", pattern)
                    .executeUpdate();
        } catch (RuntimeException e) {
            throw DataExceptionMapper.map(e);
        }
    }

    @Override
    public List<Box> findByCodeBetween(String from, String to, Order order) {
        StringBuilder jpql = new StringBuilder("SELECT b FROM Box b WHERE b.code BETWEEN :from AND :to");
        
        if (order != null && order.sorts() != null && !order.sorts().isEmpty()) {
            jpql.append(" ORDER BY ");
            boolean first = true;
            for (Object sortObj : order.sorts()) {
                Sort sort = (Sort) sortObj;
                if (!first) jpql.append(", ");
                jpql.append("b.").append(sort.property()).append(" ").append(sort.isAscending() ? "ASC" : "DESC");
                first = false;
            }
        }
        
        TypedQuery<Box> query = em.createQuery(jpql.toString(), Box.class);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
    }

    @Override
    public Stream<Box> findByVolumeWithFactorBetween(double min, double max, double factor) {
        TypedQuery<Box> query = em.createQuery(
            "SELECT b FROM Box b WHERE b.widthCm * b.heightCm * b.depthCm * :factor BETWEEN :min AND :max", 
            Box.class);
        query.setParameter("min", min);
        query.setParameter("max", max);
        query.setParameter("factor", factor);
        return query.getResultList().stream();
    }

    @Override
    public List<Box> findByNameLengthAndVolumeBelow(int nameLength, double maxVolume) {
        TypedQuery<Box> query = em.createQuery(
            "SELECT b FROM Box b WHERE LENGTH(b.name) = ?1 AND b.widthCm * b.heightCm * b.depthCm < ?2", 
            Box.class);
        query.setParameter(1, nameLength);
        query.setParameter(2, maxVolume);
        return query.getResultList();
    }
}