package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.VersionedOrder;
import jakarta.data.repository.BasicRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface VersionedOrders extends BasicRepository<VersionedOrder, UUID> {
    
    // Query methods implemented as default stream-based operations
    default List<VersionedOrder> findByStatus(String status) {
        return findAll()
            .filter(o -> status.equals(o.status))
            .toList();
    }
    
    default List<VersionedOrder> findByCustomerName(String customerName) {
        return findAll()
            .filter(o -> customerName.equals(o.customerName))
            .toList();
    }
    
    default List<VersionedOrder> findByTotalAmountGreaterThan(BigDecimal amount) {
        return findAll()
            .filter(o -> o.totalAmount.compareTo(amount) > 0)
            .toList();
    }
    
    default List<VersionedOrder> findModifiableOrders() {
        return findAll()
            .filter(VersionedOrder::isModifiable)
            .toList();
    }
    
    default List<VersionedOrder> findFinalStateOrders() {
        return findAll()
            .filter(VersionedOrder::isFinalState)
            .toList();
    }
    
    default List<VersionedOrder> findByVersion(Long version) {
        return findAll()
            .filter(o -> version.equals(o.version))
            .toList();
    }
    
    default long countByStatus(String status) {
        return findAll()
            .filter(o -> status.equals(o.status))
            .count();
    }
    
    default boolean existsByOrderNumber(String orderNumber) {
        return findAll()
            .anyMatch(o -> orderNumber.equals(o.orderNumber));
    }
    
    // Add missing count() and existsById() methods
    default long count() {
        return findAll().count();
    }
    
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
}