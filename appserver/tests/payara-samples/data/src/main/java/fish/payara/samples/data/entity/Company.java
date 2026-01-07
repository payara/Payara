package fish.payara.samples.data.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Company")
public class Company {
    @Id
    public UUID id;

    public String name;

    public String industry;

    public boolean active;

    public UUID headquartersAddressId;

    public Instant createdAt;

    public static Company of(UUID id, String name, String industry, boolean active, UUID headquartersAddressId, Instant createdAt) {
        Company c = new Company();
        c.id = id;
        c.name = name;
        c.industry = industry;
        c.active = active;
        c.headquartersAddressId = headquartersAddressId;
        c.createdAt = createdAt;
        return c;
    }

    @Override
    public String toString() {
        return "Company@" + Integer.toHexString(hashCode()) + ":" + id + ":" + name + ":" + industry + ":" + active;
    }
}
