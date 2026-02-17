/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.data.core.cdi.extension;

import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static fish.payara.data.core.util.DataCommonOperationUtility.getCursorValues;
import static fish.payara.data.core.util.FindOperationUtility.excludeParameter;
import static fish.payara.data.core.util.FindOperationUtility.setParameterFromCursor;

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
    private List<T> results;
    private long totalElements = -1;
    private boolean isForward;

    public CursoredPageImpl(QueryData queryData, Object[] args, PageRequest pageRequest, EntityManager em) {
        this.queryData = queryData;
        this.args = args;
        this.pageRequest = pageRequest;
        this.entityManager = em;
        this.isForward = this.pageRequest.mode() != PageRequest.Mode.CURSOR_PREVIOUS;

        Optional<PageRequest.Cursor> cursor = this.pageRequest.cursor();
        String sql = cursor.isEmpty() ? queryData.getQueryString() : isForward ? queryData.getQueryNext() : queryData.getQueryPrevious();

        TypedQuery<T> query = (TypedQuery<T>) em.createQuery(sql, queryData.getQueryMetadata().getDeclaredEntityClass());
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
            setParameterFromCursor(query, cursor.get(), queryData.getOrders(), queryData, args);
        }

        query.setFirstResult(this.processOffset());
        query.setMaxResults(pageRequest.size() + 1);

        results = query.getResultList();
        if (!isForward && !results.isEmpty()) {
            if (results.size() > pageRequest.size()) {
                results = results.subList(0, pageRequest.size());
            }

            Collections.reverse(results);
        }

        if (pageRequest.requestTotal()) {
            totalElements();
        }
    }

    @Override
    public PageRequest.Cursor cursor(int index) {
        return null;
    }

    @Override
    public List<T> content() {
        return results.size() > pageRequest.size() ? results.subList(0, pageRequest.size()) : results;
    }

    @Override
    public boolean hasContent() {
        return !results.isEmpty();
    }

    @Override
    public int numberOfElements() {
        return results.size() > pageRequest.size() ? pageRequest.size() : results.size();
    }

    @Override
    public boolean hasNext() {
        return results.size() >= (isForward ? pageRequest.size() + 1 : 1);
    }

    @Override
    public boolean hasPrevious() {
        return results.size() >= (isForward ? 1 : pageRequest.size() + 1);
    }

    @Override
    public PageRequest pageRequest() {
        return this.pageRequest;
    }

    @Override
    public PageRequest nextPageRequest() {
        if (hasNext()) {
            return PageRequest.afterCursor(PageRequest.Cursor
                            .forKey(getCursorValues(results.get(Math.min(pageRequest.size(), results.size()) - 1),
                                    this.queryData.getOrders(), this.queryData.getQueryMetadata())),
                    pageRequest.page() + 1, pageRequest.size(), pageRequest.requestTotal());
        } else {
            throw new NoSuchElementException("Not an available page for cursor");
        }
    }

    @Override
    public PageRequest previousPageRequest() {
        if (hasPrevious()) {
            return PageRequest.beforeCursor(
                    PageRequest.Cursor.forKey(getCursorValues(results.get(0), this.queryData.getOrders(), this.queryData.getQueryMetadata())),
                    pageRequest.page() == 1 ? 1 : pageRequest.page() - 1,
                    pageRequest.size(),
                    pageRequest.requestTotal()
            );
        } else {
            throw new NoSuchElementException("Not an available page for cursor");
        }
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
        totalElements = totalElements();
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

    @Override
    public String toString() {
        return "CursoredPage " +
                "page=" + pageRequest.page() +
                ", pageRequest=" + pageRequest +
                ", from Entity=" + queryData.getQueryMetadata().getDeclaredEntityClass().getName();
    }
}
