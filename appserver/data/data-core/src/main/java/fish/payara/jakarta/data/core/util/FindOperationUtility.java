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
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.jakarta.data.core.util;

import fish.payara.jakarta.data.core.cdi.extension.EntityMetadata;
import fish.payara.jakarta.data.core.cdi.extension.PageImpl;
import fish.payara.jakarta.data.core.cdi.extension.QueryData;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility class used to process Jakarta Data find operations
 */
public class FindOperationUtility {

    private static final List<Class<?>> parametersToExclude = List.of(PageRequest.class, Limit.class, Order.class, Sort.class, Sort[].class);

    public static Stream<?> processFindAllOperation(Class<?> entityClass, EntityManager em, String orderByClause, EntityMetadata entityMetadata) {
        Query q = em.createQuery(createBaseFindQuery(entityClass, orderByClause, entityMetadata));
        return q.getResultStream();
    }

    public static Object processFindByOperation(Object[] args, EntityManager em, QueryData dataForQuery, boolean evaluatePages) {
        Class<?> entityClass = dataForQuery.getDeclaredEntityClass();
        EntityMetadata entityMetadata = dataForQuery.getEntityMetadata();
        Method method = dataForQuery.getMethod();
        StringBuilder builder = new StringBuilder();
        builder.append(createBaseFindQuery(entityClass, null, entityMetadata));
        String attributeValue = null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        int queryPosition = 1;
        boolean hasWhere = false;
        for (Annotation[] annotations : parameterAnnotations) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof By) {
                    attributeValue = ((By) annotation).value();
                }
                if (!hasWhere) {
                    builder.append(" WHERE (");
                    hasWhere = true;
                } else {
                    builder.append(" AND ");
                }

                if (attributeValue != null) {
                    attributeValue = preprocessAttributeName(entityMetadata, attributeValue);
                }
                builder.append("o.").append(attributeValue).append("=?").append(queryPosition);
            }
            queryPosition++;
        }

        if (hasWhere) {
            builder.append(")");
        }

        //here place to process pagination
        if (evaluatePages) {
            int maxResults = 0;
            PageRequest pageRequest = null;
            Object returnValue = null;
            List<Sort<Object>> orders = new ArrayList<>();
            createCountQuery(dataForQuery, hasWhere);
            //evaluating parameters for pagination
            for (Object param : args) {
                if (param instanceof PageRequest) { //Get info for PageRequest
                    if (maxResults == 0) {
                        pageRequest = (PageRequest) param;
                        maxResults = pageRequest.size();
                    }
                } else if (param instanceof Order) { //Get info for orders
                    Iterable<Sort<Object>> order = (Iterable<Sort<Object>>) param;
                    preprocessOrder(orders, order, dataForQuery);
                }
            }


            StringBuilder orderQuery = null;
            //create order query
            for (Sort<?> sort : orders) {
                if (orderQuery == null) {
                    orderQuery = new StringBuilder(" ORDER BY ");
                } else {
                    orderQuery.append(", ");
                }

                String propertyName = sort.property();
                if (sort.ignoreCase()) {
                    orderQuery.append("LOWER(");
                }

                if (propertyName.charAt(propertyName.length() - 1) != ')') {
                    orderQuery.append("o.");
                }
                orderQuery.append(propertyName);
            }

            if (pageRequest.mode() == PageRequest.Mode.OFFSET) {
                builder.append(orderQuery.toString());
                dataForQuery.setQueryString(builder.toString());
            }

            if (Page.class.equals(method.getReturnType())) {
                returnValue = new PageImpl(dataForQuery, args, pageRequest, em);
            }

            return returnValue;
        } else {
            //check order conditions to improve select queries
            dataForQuery.setQueryString(builder.toString());
            Query q = em.createQuery(dataForQuery.getQueryString());
            for (int i = 0; i < args.length; i++) {
                if (!excludeParameter(args[i])) {
                    q.setParameter(i + 1, args[i]);
                }
            }
            return q.getResultList();
        }
    }

    public static void preprocessOrder(List<Sort<Object>> orders, Iterable<Sort<Object>> order, QueryData dataForQuery) {
        for (Sort<Object> sort : order) {
            if (sort == null) {
                throw new MappingException("sort is null");
            } else {
                orders.add(validateSort(sort, dataForQuery.getEntityMetadata(), sort.property()));
            }
        }

    }

    public static <T> Sort<T> validateSort(Sort<T> sort, EntityMetadata entityMetadata, String attributeName) {
        String name = preprocessAttributeName(entityMetadata, attributeName);
        if (name.equals(sort.property())) {
            return sort;
        } else {
            return sort.isAscending() ? sort.ignoreCase() ? Sort.ascIgnoreCase(name) : Sort.asc(name)
                    : sort.ignoreCase() ? Sort.descIgnoreCase(name) : Sort.desc(name);
        }
    }

    public static void createCountQuery(QueryData dataForQuery, boolean hasWhere) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT COUNT(").append("o").append(") FROM ")
                .append(getSingleEntityName(dataForQuery.getDeclaredEntityClass().getName())).append(" o");
        if (hasWhere) {
            builder.append(" WHERE ");
        }
        dataForQuery.setCountQueryString(builder.toString());
    }

    public static boolean excludeParameter(Object arg) {
        Optional<Class<?>> found = parametersToExclude.stream().filter(c -> c.isInstance(arg)).findAny();
        return found.isPresent();
    }

    public static String preprocessAttributeName(EntityMetadata entityMetadata, String attributeValue) {
        if (attributeValue.endsWith(")")) {
            //process this(id)
            return getIDParameterName(attributeValue);
        } else {
            if (entityMetadata.getAttributeNames().containsKey(attributeValue.toLowerCase())) {
                return entityMetadata.getAttributeNames().get(attributeValue.toLowerCase());
            } else {
                throw new IllegalArgumentException("The attribute " + attributeValue +
                        " is not mapped on the entity " + entityMetadata.getEntityName());
            }
        }
    }

    public static String createBaseFindQuery(Class<?> entityClass, String orderByClause, EntityMetadata entityMetadata) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ").append("o").append(" FROM ").append(getSingleEntityName(entityClass.getName())).append(" o");

        if (orderByClause != null && !orderByClause.isEmpty()) {
            builder.append(processOrderByClause(orderByClause, entityMetadata));
        }

        return builder.toString();
    }

    private static String processOrderByClause(String orderByClause, EntityMetadata entityMetadata) {
        String[] orderByClauses = orderByClause.split(",");
        StringBuilder orderByBuilder = new StringBuilder(" ORDER BY ");

        for (int i = 0; i < orderByClauses.length; i++) {
            String clause = orderByClauses[i].trim();
            String[] parts = clause.split(" ", 2);
            String fieldNamePart = parts[0].trim();

            // Extract field name from LOWER() function if present
            String fieldName;
            boolean hasLowerFunction = false;
            if (fieldNamePart.startsWith("LOWER(") && fieldNamePart.endsWith(")")) {
                fieldName = fieldNamePart.substring(6, fieldNamePart.length() - 1).toLowerCase();
                hasLowerFunction = true;
            } else {
                fieldName = fieldNamePart.toLowerCase();
            }

            if (!entityMetadata.getAttributeNames().containsKey(fieldName)) {
                throw new MappingException(
                        "The attribute '" + fieldName + "' is not mapped on the entity " +
                                entityMetadata.getEntityName());
            }

            String actualFieldName = entityMetadata.getAttributeNames().get(fieldName);

            if (i > 0) {
                orderByBuilder.append(", ");
            }

            if (hasLowerFunction) {
                orderByBuilder.append("LOWER(o.").append(actualFieldName).append(")");
            } else {
                orderByBuilder.append("o.").append(actualFieldName);
            }

            if (parts.length > 1) {
                orderByBuilder.append(" ").append(parts[1].trim());
            }
        }

        return orderByBuilder.toString();
    }


    public static String getSingleEntityName(String entityName) {
        if (entityName != null) {
            int idx = entityName.lastIndexOf(".");
            return entityName.substring(idx + 1);
        }
        return null;
    }

    public static String getIDParameterName(String idNameValue) {
        if (idNameValue != null) {
            int idx = idNameValue.lastIndexOf("(");
            return idNameValue.substring(0, idx);
        }
        return null;
    }
}
