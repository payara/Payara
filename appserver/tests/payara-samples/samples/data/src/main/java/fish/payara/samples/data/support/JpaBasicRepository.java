package fish.payara.samples.data.support;

import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.repository.BasicRepository;
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