package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Product;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository bound to the "firstPU" persistence unit via dataStore.
 * Used to test multiple persistence unit resolution with @Repository(dataStore).
 */
@Repository(dataStore = "firstPU")
public interface ProductsFirstPU extends BasicRepository<Product, UUID> {

    List<Product> findByCategory(String category);

    List<Product> findByAvailable(boolean available);

    long countByCategory(String category);
}
