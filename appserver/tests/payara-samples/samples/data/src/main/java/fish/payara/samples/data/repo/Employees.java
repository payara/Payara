package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Employee;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BasicRepository + richer default methods for sample queries like the TCK patterns.
 */
@Repository
public interface Employees extends BasicRepository<Employee, UUID> {

    default List<Employee> findActiveByCompany(UUID companyId) {
        return this.findAll()
                .filter(e -> e.active && companyId.equals(e.companyId))
                .collect(Collectors.toList());
    }

    default List<Employee> findByEmailIgnoreCase(String email) {
        String target = email == null ? "" : email.toLowerCase(Locale.ROOT);
        return this.findAll()
                .filter(e -> e.email != null && e.email.toLowerCase(Locale.ROOT).equals(target))
                .collect(Collectors.toList());
    }

    default List<Employee> findTopNBySalaryDesc(int n) {
        return this.findAll()
                .sorted(Comparator.comparing((Employee e) -> e.salary == null ? BigDecimal.ZERO : e.salary).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    default List<Employee> findByLastNamePrefixSorted(String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return this.findAll()
                .filter(e -> e.lastName != null && e.lastName.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted(Comparator.comparing((Employee e) -> e.lastName).thenComparing(e -> e.firstName))
                .collect(Collectors.toList());
    }
}
