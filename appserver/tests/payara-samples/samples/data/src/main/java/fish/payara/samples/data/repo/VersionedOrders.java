package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.VersionedOrder;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface VersionedOrders extends BasicRepository<VersionedOrder, UUID> {
    
    // Basic query methods - Jakarta Data will implement automatically
    List<VersionedOrder> findByStatus(String status);
    List<VersionedOrder> findByCustomerName(String customerName);
    List<VersionedOrder> findByNotes(String notes);
    
    // Comparison queries
    List<VersionedOrder> findByTotalAmountGreaterThan(BigDecimal amount);
    List<VersionedOrder> findByTotalAmountLessThan(BigDecimal amount);
    List<VersionedOrder> findByTotalAmountBetween(BigDecimal min, BigDecimal max);
    
    // Combination queries
    List<VersionedOrder> findByStatusAndCustomerName(String status, String customerName);
    
    // Count operations
    long countByStatus(String status);
    long countByCustomerName(String customerName);
    
    // Existence checks
    boolean existsByCustomerName(String customerName);
    boolean existsByStatus(String status);
    boolean existsByOrderNumber(String orderNumber);
    
    // Version queries
    List<VersionedOrder> findByVersion(Long version);
    
    // Missing methods that need default implementations  
    default List<VersionedOrder> findModifiableOrders() {
        return findAll().filter(VersionedOrder::isModifiable).toList();
    }
    
    default List<VersionedOrder> findFinalStateOrders() {
        return findAll().filter(VersionedOrder::isFinalState).toList();
    }
}