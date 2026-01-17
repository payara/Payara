/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
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

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static fish.payara.data.core.util.FindOperationUtility.excludeParameter;

/**
 * This class represent the Page<T> implementation for pagination from the offset model
 *
 * @param <T>
 */
public class PageImpl<T> implements Page<T> {

    private final Object[] args;
    private final PageRequest pageRequest;
    private final QueryData queryData;
    private final List<T> results;
    private long totalElements = -1;
    private EntityManager entityManager;

    public PageImpl(QueryData queryData, Object[] args, PageRequest pageRequest, EntityManager em) {
        this.queryData = queryData;
        this.args = args;
        this.pageRequest = pageRequest;
        this.entityManager = em;

        TypedQuery<T> query = (TypedQuery<T>) em.createQuery(queryData.getQueryString(), queryData.getDeclaredEntityClass());
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
        query.setFirstResult(this.processOffset());
        query.setMaxResults(pageRequest.size() + (pageRequest.size() == Integer.MAX_VALUE ? 0 : 1));
        results = query.getResultList();
        if (pageRequest.requestTotal()) {
            totalElements();
        }
    }

    @Override
    public List<T> content() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? results.subList(0, max) : results;
    }

    @Override
    public boolean hasContent() {
        return !results.isEmpty();
    }

    @Override
    public int numberOfElements() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? max : size;
    }

    @Override
    public boolean hasNext() {
        return results.size() > pageRequest.size() ||
                pageRequest.size() == Integer.MAX_VALUE && results.size() == pageRequest.size();
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
        if (hasNext()) {
            return PageRequest.ofPage(pageRequest.page() + 1, pageRequest.size(), pageRequest.requestTotal());
        } else {
            throw new NoSuchElementException("No more elements");
        }
    }

    @Override
    public PageRequest previousPageRequest() {
        if (pageRequest.page() > 1) {
            return PageRequest.ofPage(pageRequest.page() - 1, pageRequest.size(), pageRequest.requestTotal());
        } else {
            throw new NoSuchElementException("No more elements");
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
        return totalElements / pageRequest.size() + (totalElements % pageRequest.size() > 0 ? 1 : 0);
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

    @Override
    public Iterator<T> iterator() {
        return content().iterator();
    }

    public int processOffset() {
        if (this.pageRequest.mode() != PageRequest.Mode.OFFSET) {
            throw new IllegalArgumentException("The page request does not support mode " + this.pageRequest.mode());
        }
        return (int) ((pageRequest.page() - 1) * pageRequest.size());
    }

    @Override
    public Stream<T> stream() {
        return content().stream();
    }

    @Override
    public String toString() {
        return "Page:" + this.pageRequest.page() +
                " From Entity:" + this.queryData.getDeclaredEntityClass().getName() +
                " page size:" + this.pageRequest.size();
    }
}
