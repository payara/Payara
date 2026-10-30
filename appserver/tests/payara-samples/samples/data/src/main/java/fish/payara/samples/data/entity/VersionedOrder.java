package fish.payara.samples.data.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Test entity for optimistic locking and versioning scenarios in Jakarta Data TCK.
 * This entity demonstrates the use of @Version for optimistic concurrency control.
 */
@Entity
@Table(name = "versioned_orders")
public class VersionedOrder {

    @Id
    public UUID id;

    @Column(name = "order_number")
    public String orderNumber;

    @Column(name = "customer_name")
    public String customerName;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2) // explicitly updatable
    public BigDecimal totalAmount;

    @Column(name = "order_date")
    public Instant orderDate;

    @Column(name = "status", nullable = false) // explicitly updatable
    public String status; // e.g., "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"

    @Column(name = "notes")
    public String notes;

    /**
     * Version field for optimistic locking.
     * JPA will automatically increment this field on each update.
     */
    @Version
    @Column(name = "version")
    public Long version;

    // Default constructor for JPA
    public VersionedOrder() {}

    // Constructor for creating instances
    public VersionedOrder(UUID id, String orderNumber, String customerName, 
                         BigDecimal totalAmount, Instant orderDate, String status, String notes) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.status = status;
        this.notes = notes;
        // version is managed by JPA
    }

    // Static factory method for convenience
    public static VersionedOrder of(UUID id, String orderNumber, String customerName, 
                                   BigDecimal totalAmount, Instant orderDate, String status, String notes) {
        return new VersionedOrder(id, orderNumber, customerName, totalAmount, orderDate, status, notes);
    }

    /**
     * Check if the order is in a final state.
     */
    public boolean isFinalState() {
        return "DELIVERED".equals(status) || "CANCELLED".equals(status);
    }

    /**
     * Check if the order can be modified.
     */
    public boolean isModifiable() {
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }

    /**
     * Get a copy of this order for testing purposes.
     * Note: This creates a detached copy with the same version.
     */
    public VersionedOrder copyForTesting() {
        VersionedOrder copy = new VersionedOrder();
        copy.id = this.id;
        copy.orderNumber = this.orderNumber;
        copy.customerName = this.customerName;
        copy.totalAmount = this.totalAmount;
        copy.orderDate = this.orderDate;
        copy.status = this.status;
        copy.notes = this.notes;
        copy.version = this.version;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("VersionedOrder{id=%s, orderNumber='%s', customerName='%s', " +
                           "totalAmount=%s, status='%s', version=%d}", 
                           id, orderNumber, customerName, totalAmount, status, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VersionedOrder that = (VersionedOrder) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}