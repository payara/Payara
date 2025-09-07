package fish.payara.samples.data.support;

import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.impl.PageRecord;
import jakarta.data.repository.BasicRepository;
import jakarta.data.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * JPA-based implementation of BasicRepository for local tests with H2.
 * Uses RESOURCE_LOCAL transactions started within each operation for simplicity.
 */
public abstract class JpaBasicRepository<E, ID> implements BasicRepository<E, ID> {

    protected final EntityManager em;
    private final Class<E> entityClass;

    protected JpaBasicRepository(EntityManager em, Class<E> entityClass) {
        this.em = Objects.requireNonNull(em, "EntityManager must not be null");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
    }

    // BasicRepository: <S extends E> S save(S entity)
    @Override
    public <S extends E> S save(S entity) {
        return em.merge(entity);
    }

    // BasicRepository: <S extends E> Iterable<S> saveAll(Iterable<S> entities)
    public <S extends E> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        for (S e : entities) {
            result.add(em.merge(e));
        }
        return result;
    }

    // BasicRepository: <S extends E> List<S> saveAll(List<S> entities)
    @Override
    public <S extends E> List<S> saveAll(List<S> entities) {
        List<S> result = new ArrayList<>();
        for (S e : entities) {
            result.add(em.merge(e));
        }
        return result;
    }

    // BasicRepository: void delete(E entity)
    @Override
    public void delete(E entity) {
        E managed = em.contains(entity) ? entity : em.merge(entity);
        em.remove(managed);
    }

    // BasicRepository: void deleteAll(Iterable<E> entities)
    public void deleteAll(Iterable<E> entities) {
        for (E e : entities) {
            E managed = em.contains(e) ? e : em.merge(e);
            em.remove(managed);
        }
    }

    // BasicRepository 1.0.1 also declares: void deleteAll(List<? extends E> entities)
    @Override
    public void deleteAll(List<? extends E> entities) {
        // Delegate to the Iterable<E> overload
        deleteAll((Iterable<E>) entities);
    }

    public void deleteAll() {
        em.createQuery("DELETE FROM " + entityClass.getSimpleName()).executeUpdate();
    }

    @Override
    public void deleteById(ID id) {
        try {
            E ref = em.getReference(entityClass, id);
            em.remove(ref);
        } catch (jakarta.persistence.EntityNotFoundException ignore) {
            // Idempotent when entity does not exist
        }
    }

    @Override
    public Optional<E> findById(ID id) {
        return Optional.ofNullable(em.find(entityClass, id));
    }

    @Override
    public Stream<E> findAll() {
        TypedQuery<E> q = em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass);
        return q.getResultStream();
    }

    @Override
    public Page<E> findAll(PageRequest pageRequest, Order<E> order) {
        StringBuilder queryBuilder = new StringBuilder("SELECT e FROM " + entityClass.getSimpleName() + " e");
        
        // Add ordering if provided
        if (order != null) {
            queryBuilder.append(" ORDER BY ");
            // Simple implementation - in real world, would need to parse Order properly
            queryBuilder.append("e.id");
        }
        
        TypedQuery<E> query = em.createQuery(queryBuilder.toString(), entityClass);
        
        // Apply pagination
        int pageSize = (int) pageRequest.size();
        int pageNumber = (int) pageRequest.page();
        query.setFirstResult(pageNumber * pageSize);
        query.setMaxResults(pageSize);
        
        List<E> content = query.getResultList();
        
        // Get total count
        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class);
        long totalElements = countQuery.getSingleResult();
        long totalPages = (totalElements + pageSize - 1) / pageSize;
        
        return new PageRecord<>(pageRequest, content, totalElements, totalPages > pageNumber + 1);
    }

    public long count() {
        TypedQuery<Long> q = em.createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class);
        return q.getSingleResult();
    }

    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }


    protected <R> R singleOrThrow(List<R> list) {
        if (list.isEmpty()) {
            throw new EmptyResultException("No result for query");
        }
        if (list.size() > 1) {
            throw new NonUniqueResultException("More than one result: " + list.size());
        }
        return list.get(0);
    }

}