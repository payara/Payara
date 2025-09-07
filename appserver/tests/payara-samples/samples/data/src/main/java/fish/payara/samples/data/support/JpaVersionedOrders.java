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
}