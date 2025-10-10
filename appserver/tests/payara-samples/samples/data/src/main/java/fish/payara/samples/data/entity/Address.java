package fish.payara.samples.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "Address")
public class Address {
    @Id
    public UUID id;

    public String street;

    public String city;

    public String state;

    public String postalCode;

    public String country;

    public static Address of(UUID id, String street, String city, String state, String postalCode, String country) {
        Address a = new Address();
        a.id = id;
        a.street = street;
        a.city = city;
        a.state = state;
        a.postalCode = postalCode;
        a.country = country;
        return a;
    }

    @Override
    public String toString() {
        return "Address@" + Integer.toHexString(hashCode()) + ":" + id + ":" + street + "," + city + "," + state + " " + postalCode + "," + country;
    }
}
