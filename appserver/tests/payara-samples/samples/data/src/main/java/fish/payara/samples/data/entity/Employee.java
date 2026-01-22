package fish.payara.samples.data.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Employee")
@Cacheable  // Enable L2 cache for cache eviction testing
public class Employee {
    @Id
    public UUID id;

    public String firstName;

    public String lastName;

    public String email;

    public UUID companyId;

    public UUID addressId;

    public String title;

    public BigDecimal salary;

    public boolean active;

    public LocalDate hiredOn;

    public static Employee of(
            UUID id, String firstName, String lastName, String email,
            UUID companyId, UUID addressId, String title, BigDecimal salary,
            boolean active, LocalDate hiredOn) {
        Employee e = new Employee();
        e.id = id;
        e.firstName = firstName;
        e.lastName = lastName;
        e.email = email;
        e.companyId = companyId;
        e.addressId = addressId;
        e.title = title;
        e.salary = salary;
        e.active = active;
        e.hiredOn = hiredOn;
        return e;
    }

    @Override
    public String toString() {
        return "Employee@" + Integer.toHexString(hashCode()) + ":" + id + ":" + firstName + " " + lastName + " (" + email + ")";
    }
}
