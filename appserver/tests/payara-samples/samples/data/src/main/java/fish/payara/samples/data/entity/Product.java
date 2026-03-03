package fish.payara.samples.data.entity;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Product")
public class Product {
    @Id
    public UUID id;

    public String name;

    public String category;

    public static Product of(UUID id, String name, String category) {
        Product p = new Product();
        p.id = id;
        p.name = name;
        p.category = category;
        return p;
    }

    @Override
    public String toString() {
        return "Product@" + Integer.toHexString(hashCode()) + ":" + id + ":" + name + ":" + category;
    }
}
