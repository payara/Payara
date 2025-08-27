package fish.payara.samples.data.support;

import fish.payara.samples.data.entity.Company;
import fish.payara.samples.data.repo.Companies;
import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class JpaCompanies extends JpaBasicRepository<Company, UUID> implements Companies {
    public JpaCompanies(EntityManager em) {
        super(em, Company.class);
    }

    // Implementação mais eficiente (JPQL) da busca por contains ignore-case
    @Override
    public List<Company> findByNameContainsIgnoreCase(String token) {
        String t = token == null ? "" : token.toLowerCase(Locale.ROOT);
        TypedQuery<Company> q = em.createQuery(
                "SELECT c FROM Company c WHERE LOWER(c.name) LIKE :p ORDER BY c.name ASC", Company.class);
        q.setParameter("p", "%" + t + "%");
        return q.getResultList();
    }

    @Override
    public <S extends Company> List<S> saveAll(List<S> entities) {
        return List.of();
    }

    @Override
    public Page<Company> findAll(PageRequest pageRequest, Order<Company> sortBy) {
        return null;
    }
}