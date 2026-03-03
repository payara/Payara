package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Product;
import jakarta.data.repository.BasicRepository;

import java.util.List;
import java.util.UUID;

public interface AbstractProducts extends BasicRepository<Product, UUID> {
    List<Product> findByCategory(String category);

    boolean existsByName(String name);

    long countByCategory(String category);
}
