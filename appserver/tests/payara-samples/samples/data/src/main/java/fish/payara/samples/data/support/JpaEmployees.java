package fish.payara.samples.data.support;

import fish.payara.samples.data.entity.Employee;
import fish.payara.samples.data.repo.Employees;
import jakarta.data.Order;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class JpaEmployees extends JpaBasicRepository<Employee, UUID> implements Employees {
    public JpaEmployees(EntityManager em) {
        super(em, Employee.class);
    }

    // "Single" version mirroring TCK behavior: throw EmptyResult/NonUniqueResult
    public Employee findByEmailIgnoreCaseSingle(String email) {
        TypedQuery<Employee> q = em.createQuery(
                "SELECT e FROM Employee e WHERE LOWER(e.email) = :eml", Employee.class);
        q.setParameter("eml", email == null ? "" : email.toLowerCase(Locale.ROOT));
        List<Employee> found = q.getResultList();
        if (found.isEmpty()) {
            throw new EmptyResultException("No employee found for email (ignore case): " + email);
        }
        if (found.size() > 1) {
            throw new NonUniqueResultException("Multiple employees found for email (ignore case): " + email);
        }
        return found.get(0);
    }

    // Top-N by salary desc using JPQL + setMaxResults
    public List<Employee> findTopNBySalaryDescJPQL(int n) {
        TypedQuery<Employee> q = em.createQuery(
                "SELECT e FROM Employee e ORDER BY e.salary DESC NULLS LAST, e.lastName ASC, e.firstName ASC", Employee.class);
        q.setMaxResults(Math.max(0, n));
        return q.getResultList();
    }

    public List<Employee> findActiveByCompanyJPQL(UUID companyId) {
        TypedQuery<Employee> q = em.createQuery(
                "SELECT e FROM Employee e WHERE e.active = true AND e.companyId = :cid ORDER BY e.lastName ASC, e.firstName ASC",
                Employee.class);
        q.setParameter("cid", companyId);
        return q.getResultList();
    }

    @Override
    public <S extends Employee> List<S> saveAll(List<S> entities) {
        return List.of();
    }

    @Override
    public Page<Employee> findAll(PageRequest pageRequest, Order<Employee> sortBy) {
        return null;
    }
}