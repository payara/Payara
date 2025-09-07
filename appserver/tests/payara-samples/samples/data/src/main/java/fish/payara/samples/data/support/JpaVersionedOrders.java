package fish.payara.samples.data.support;

import fish.payara.samples.data.entity.VersionedOrder;
import fish.payara.samples.data.repo.VersionedOrders;
import jakarta.persistence.EntityManager;

import java.util.UUID;

/**
 * JPA implementation of the VersionedOrders repository.
 * Used for testing optimistic locking scenarios.
 */
public class JpaVersionedOrders extends JpaBasicRepository<VersionedOrder, UUID> implements VersionedOrders {
    
    public JpaVersionedOrders(EntityManager entityManager) {
        super(entityManager, VersionedOrder.class);
    }
    
    @Override
    public <S extends VersionedOrder> S save(S entity) {
        try {
            if (entity == null) return entity;

            // Generate ID if necessary
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
            }

            // If already managed in current context, changes will be flushed on commit
            if (em.contains(entity)) {
                // Nothing to do - entity is managed
                return entity;
            } else {
                // Check existence in database to decide between persist and merge
                VersionedOrder existing = em.find(VersionedOrder.class, entity.id);
                if (existing == null) {
                    em.persist(entity);
                } else {
                    // merge ensures changes to detached entity are applied
                    entity = em.merge(entity);
                }
            }

            // In JTA, commit also does flush; still, force flush to reflect in tests immediately
            em.flush();
            return entity;
        } catch (RuntimeException e) {
            throw DataExceptionMapper.map(e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        try {
            var found = em.find(VersionedOrder.class, id);
            if (found == null) {
                throw new jakarta.data.exceptions.OptimisticLockingFailureException("Entity not found for delete: " + id);
            }
            em.remove(found);
            em.flush();
        } catch (RuntimeException e) {
            throw DataExceptionMapper.map(e);
        }
    }
}