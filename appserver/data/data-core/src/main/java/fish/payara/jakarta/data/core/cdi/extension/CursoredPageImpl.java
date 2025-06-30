package fish.payara.jakarta.data.core.cdi.extension;

import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static fish.payara.jakarta.data.core.util.DataCommonOperationUtility.getCursorValues;
import static fish.payara.jakarta.data.core.util.FindOperationUtility.excludeParameter;

/**
 * This class represent the CursoredPage<T> implementation for pagination from the cursor model
 *
 * @param <T>
 */
public class CursoredPageImpl<T> implements CursoredPage<T> {

    private final Object[] args;
    private final PageRequest pageRequest;
    private final QueryData queryData;
    private EntityManager entityManager;
    private final List<T> results;
    private long totalElements = -1;
    private boolean isPrevious;
    
    public CursoredPageImpl(QueryData queryData, Object[] args, PageRequest pageRequest, EntityManager em){
        this.queryData = queryData;
        this.args = args;
        this.pageRequest = pageRequest;
        this.entityManager = em;
        this.isPrevious = this.pageRequest.mode() == PageRequest.Mode.CURSOR_PREVIOUS;
        
        Optional<PageRequest.Cursor> cursor = this.pageRequest.cursor();

        int maxPageSize = pageRequest.size();
        String sql = cursor.isEmpty() ? queryData.getQueryString() : "";

        TypedQuery<T> query = (TypedQuery<T>) em.createQuery(sql, queryData.getDeclaredEntityClass());
        if (!queryData.getJpqlParameters().isEmpty()) {
            Object[] params = queryData.getJpqlParameters().toArray();
            for (int i = 0; i < params.length; i++) {
                query.setParameter((String) params[i], args[i]);
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                if (!excludeParameter(args[i])) {
                    query.setParameter(i + 1, args[i]);
                }
            }
        }
        
        if (cursor.isPresent()) {
           //check how to set the parameter for cursor 
        }
        
        query.setFirstResult(this.processOffset());
        query.setMaxResults(pageRequest.size() + 1);
        results = query.getResultList();
    }
    
    @Override
    public PageRequest.Cursor cursor(int index) {
        return null;
    }

    @Override
    public List<T> content() {
        return results;
    }

    @Override
    public boolean hasContent() {
        return !results.isEmpty();
    }

    @Override
    public int numberOfElements() {
        return results.size();
    }

    @Override
    public boolean hasNext() {
        return results.size() >=  pageRequest.size();
    }

    @Override
    public boolean hasPrevious() {
        return pageRequest.page() > 1;
    }

    @Override
    public PageRequest pageRequest() {
        return this.pageRequest;
    }

    @Override
    public PageRequest nextPageRequest() {
        return PageRequest.afterCursor(PageRequest.Cursor.forKey(getCursorValues(results.get(results.size() - 1), 
                this.queryData.getOrders(), this.queryData)),
                pageRequest.page() + 1, pageRequest.size(), pageRequest.requestTotal());
    }

    @Override
    public PageRequest previousPageRequest() {
        return PageRequest.beforeCursor(PageRequest.Cursor.forKey(getCursorValues(results.get(0), this.queryData.getOrders(), this.queryData)),
                pageRequest.page() == 1 ? 1 : pageRequest.page() - 1, pageRequest.size(), pageRequest.requestTotal());
    }

    @Override
    public boolean hasTotals() {
        return pageRequest.requestTotal();
    }

    @Override
    public long totalElements() {
        if (totalElements == -1) {
            totalElements = countTotalElements();
        }
        return totalElements;
    }

    @Override
    public long totalPages() {
        return totalElements / pageRequest.size() + (totalElements % pageRequest.size() > 0 ? 1 : 0);
    }

    @Override
    public Iterator<T> iterator() {
        return results.iterator();
    }

    public long countTotalElements() {
        if (!pageRequest.requestTotal()) {
            throw new IllegalArgumentException("Page request does not contain any elements");
        }

        TypedQuery<Long> queryCount = this.entityManager.createQuery(queryData.getCountQueryString(), Long.class);
        if (!queryData.getJpqlParameters().isEmpty()) {
            Object[] params = queryData.getJpqlParameters().toArray();
            for (int i = 0; i < params.length; i++) {
                queryCount.setParameter((String) params[i], args[i]);
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                if (!excludeParameter(args[i])) {
                    queryCount.setParameter(i + 1, args[i]);
                }
            }
        }
        List results = queryCount.getResultList();
        if (results.size() == 1) {
            return (Long) results.get(0);
        } else if (results.size() > 1) {
            return results.size();
        } else {
            return 0;
        }
    }

    public int processOffset() {
        if (this.pageRequest.mode() == PageRequest.Mode.OFFSET) {
            return (int) ((pageRequest.page() - 1) * pageRequest.size());
        } else {
            return 0;
        }
    }
    
    @Override
    public Stream<T> stream() {
        return content().stream();
    }
}
