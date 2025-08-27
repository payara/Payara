package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Company;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BasicRepository + some default methods to mimic TCK-style custom capabilities without a provider.
 */
@Repository
public interface Companies extends BasicRepository<Company, UUID> {

    /**
     * Example: case-insensitive contains on company name.
     * Implemented as a default method using findAll().
     */
    default List<Company> findByNameContainsIgnoreCase(String token) {
        String t = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return this.findAll()
                .filter(c -> c.name != null && c.name.toLowerCase(Locale.ROOT).contains(t))
                .collect(Collectors.toList());
    }

    /**
     * Example: active companies only.
     */
    default List<Company> findActive() {
        return this.findAll().filter(c -> c.active).collect(Collectors.toList());
    }
}
