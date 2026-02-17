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

import jakarta.data.Sort;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represent the structure of a query to be resolved during runtime
 */
public class QueryData {

    private Set<String> jpqlParameters = new LinkedHashSet<>();
    private String queryString;
    private String countQueryString;
    private String queryNext;
    private String queryPrevious;
    private String queryOrder;
    private List<Sort<?>> orders;
    private boolean isUserTransaction = false;
    private boolean isNewTransaction = false;
    private final QueryMetadata queryMetadata;

    public QueryData(QueryMetadata queryMetadata) {
        this.queryMetadata = queryMetadata;
    }

    public QueryMetadata getQueryMetadata() {
        return queryMetadata;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getCountQueryString() {
        return countQueryString;
    }

    public void setCountQueryString(String countQueryString) {
        this.countQueryString = countQueryString;
    }

    public Set<String> getJpqlParameters() {
        return jpqlParameters;
    }

    public void setJpqlParameters(Set<String> jpqlParameters) {
        this.jpqlParameters = jpqlParameters;
    }

    public String getQueryNext() {
        return queryNext;
    }

    public void setQueryNext(String queryNext) {
        this.queryNext = queryNext;
    }

    public String getQueryPrevious() {
        return queryPrevious;
    }

    public void setQueryPrevious(String queryPrevious) {
        this.queryPrevious = queryPrevious;
    }

    public List<Sort<?>> getOrders() {
        return orders;
    }

    public void setOrders(List<Sort<?>> orders) {
        this.orders = orders;
    }

    public String getQueryOrder() {
        return queryOrder;
    }

    public void setQueryOrder(String queryOrder) {
        this.queryOrder = queryOrder;
    }

    public boolean isUserTransaction() {
        return isUserTransaction;
    }

    public void setUserTransaction(boolean userTransaction) {
        isUserTransaction = userTransaction;
    }

    public boolean isNewTransaction() {
        return isNewTransaction;
    }

    public void setNewTransaction(boolean newTransaction) {
        isNewTransaction = newTransaction;
    }
}
