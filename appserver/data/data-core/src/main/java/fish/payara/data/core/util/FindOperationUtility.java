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
package fish.payara.data.core.util;

import fish.payara.data.core.cdi.extension.CursoredPageImpl;
import fish.payara.data.core.cdi.extension.EntityMetadata;
import fish.payara.data.core.cdi.extension.PageImpl;
import fish.payara.data.core.cdi.extension.QueryData;
import fish.payara.data.core.cdi.extension.QueryType;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static fish.payara.data.core.util.DataCommonOperationUtility.handleSort;

/**
 * Utility class used to process Jakarta Data find operations
 */
public class FindOperationUtility {

    public static final List<Class<?>> parametersToExclude = List.of(PageRequest.class, Limit.class, Order.class, Sort.class, Sort[].class);

    public static Object processFindAllOperation(Class<?> entityClass, EntityManager em, String orderByClause,
                                                    QueryData dataForQuery, DataParameter dataParameter) {
        final boolean ADD_DISTINCT_CLAUSE = true;
        String qlString = createBaseFindQuery(entityClass, orderByClause, dataForQuery.getEntityMetadata(), ADD_DISTINCT_CLAUSE);
        List<Sort<?>> sortList = dataParameter.sortList();
        if (!sortList.isEmpty()) {
            qlString = handleSort(dataForQuery, sortList, qlString, true, false, false);
        }
        Query query = em.createQuery(qlString);
        verifyLimit(dataParameter.limit(), query);
        Class<?> returnType = dataForQuery.getMethod().getReturnType();
        if (List.class.isAssignableFrom(returnType)) {
            return query.getResultList();
        } else if (Stream.class.isAssignableFrom(returnType)) {
            return query.getResultStream();
        } else {
            return query.getResultList();
        }
    }

    public static Object processFindByOperation(Object[] args, EntityManager em, QueryData dataForQuery,
                                                DataParameter dataParameter, boolean evaluatePages) {
        StringBuilder builder = new StringBuilder();
        builder.append(createBaseFindQuery(dataForQuery.getDeclaredEntityClass(),
                null, dataForQuery.getEntityMetadata()));

        Annotation[][] parameterAnnotations = dataForQuery.getMethod().getParameterAnnotations();
        Parameter[] parameters = dataForQuery.getMethod().getParameters();
        int queryPosition = 1;
        boolean hasWhere = false;

        // Process each parameter
        for (int i = 0; i < parameters.length; i++) {
            if (i < args.length && excludeParameter(args[i])) {
                continue;
            }

            String attributeValue = null;
            boolean foundBy = false;

            // Check for @By annotation
            if (i < parameterAnnotations.length) {
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (annotation instanceof By) {
                        attributeValue = ((By) annotation).value();
                        // Handle special case for id(this)
                        if ("id(this)".equals(attributeValue)) {
                            attributeValue = EntityIntrospectionUtil.getIdFieldName(dataForQuery.getDeclaredEntityClass());
                        }
                        foundBy = true;
                        break;
                    }
                }
            }

            // If no @By annotation, try to infer attribute name
            if (!foundBy) {
                String paramName = parameters[i].getName();
                if (!paramName.startsWith("arg")) {
                    attributeValue = paramName;
                } else {
                    // Try to infer by type
                    attributeValue = inferAttributeNameByType(
                            parameters[i].getType(),
                            dataForQuery.getDeclaredEntityClass()
                    );

                    // If still null, try to infer from method name
                    if (attributeValue == null) {
                        attributeValue = inferAttributeFromMethodName(
                                dataForQuery.getMethod().getName(),
                                i,
                                parameters[i].getType(),
                                dataForQuery.getDeclaredEntityClass()
                        );
                    }

                    // For methods like findById without @By, use ID field
                    if (attributeValue == null && parameters.length == 1 &&
                            !dataForQuery.getMethod().isAnnotationPresent(Find.class)) {
                        attributeValue = EntityIntrospectionUtil.getIdFieldName(dataForQuery.getDeclaredEntityClass());
                    }
                }
            }

            // Validate we have an attribute name
            if (attributeValue == null) {
                throw new MappingException(
                        "Cannot determine attribute name for parameter " + i +
                                " of method " + dataForQuery.getMethod().getName() +
                                ". Consider using @By annotation or compiling with -parameters flag."
                );
            }

            // Preprocess attribute name if needed
            attributeValue = preprocessAttributeName(dataForQuery.getEntityMetadata(), attributeValue);

            // Build WHERE clause
            if (!hasWhere) {
                builder.append(" WHERE (");
                hasWhere = true;
            } else {
                builder.append(" AND ");
            }

            builder.append("o.").append(attributeValue).append("=?").append(queryPosition);
            queryPosition++;
        }

        if (hasWhere) {
            builder.append(")");
        }

        dataForQuery.setQueryString(builder.toString());

        // Process pagination or execute query
        if (evaluatePages) {
            return processPagination(em, dataForQuery, args, dataForQuery.getMethod(), builder, hasWhere, dataParameter);
        } else {
            if (dataParameter.sortList() != null && !dataParameter.sortList().isEmpty()) {
                handleSort(dataForQuery, dataParameter.sortList(), builder,
                        dataForQuery.getQueryType() == QueryType.FIND, false,
                        false, null);
                dataForQuery.setQueryString(builder.toString());
            }

            Query query = em.createQuery(dataForQuery.getQueryString());

            // Set parameters
            int paramIndex = 1;
            for (int i = 0; i < args.length; i++) {
                if (!excludeParameter(args[i])) {
                    query.setParameter(paramIndex++, args[i]);
                }
            }

            verifyLimit(dataParameter.limit(), query);

            return query.getResultList();
        }
    }

    private static String inferAttributeNameByType(Class<?> paramType, Class<?> entityClass) {
        String foundFieldName = null;
        int count = 0;
        for (Field field : entityClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) ||
                    Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            if (field.getType().equals(paramType) ||
                    (isPrimitiveWrapper(field.getType(), paramType))) {
                foundFieldName = field.getName();
                count++;
            }
        }
        return count == 1 ? foundFieldName : null;
    }

    private static String inferAttributeFromMethodName(String methodName, int paramIndex,
                                                       Class<?> paramType, Class<?> entityClass) {
        if (methodName.startsWith("find") && methodName.length() > 4) {
            String suffix = methodName.substring(4);
            String potentialName = suffix.substring(0, 1).toLowerCase() + suffix.substring(1);

            if (paramType == boolean.class || paramType == Boolean.class) {
                if (hasField(entityClass, "is" + suffix)) {
                    return "is" + suffix;
                }
                if (hasField(entityClass, potentialName)) {
                    return potentialName;
                }
            } else {
                if (hasField(entityClass, potentialName)) {
                    return potentialName;
                }
            }
        }
        return null;
    }

    private static boolean hasField(Class<?> clazz, String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private static boolean isPrimitiveWrapper(Class<?> type1, Class<?> type2) {
        return (type1 == Integer.class && type2 == int.class) ||
                (type1 == int.class && type2 == Integer.class) ||
                (type1 == Long.class && type2 == long.class) ||
                (type1 == long.class && type2 == Long.class) ||
                (type1 == Double.class && type2 == double.class) ||
                (type1 == double.class && type2 == Double.class) ||
                (type1 == Float.class && type2 == float.class) ||
                (type1 == float.class && type2 == Float.class) ||
                (type1 == Boolean.class && type2 == boolean.class) ||
                (type1 == boolean.class && type2 == Boolean.class) ||
                (type1 == Character.class && type2 == char.class) ||
                (type1 == char.class && type2 == Character.class) ||
                (type1 == Byte.class && type2 == byte.class) ||
                (type1 == byte.class && type2 == Byte.class) ||
                (type1 == Short.class && type2 == short.class) ||
                (type1 == short.class && type2 == Short.class);
    }

    public static Object processPagination(EntityManager em, QueryData dataForQuery, Object[] args,
                                           Method method, StringBuilder builder, boolean hasWhere, DataParameter dataParameter) {
        PageRequest pageRequest = null;
        Object returnValue = null;
        createCountQuery(dataForQuery, hasWhere, dataForQuery.getQueryString().indexOf("WHERE"));
        List<Sort<?>> sortList = dataParameter.sortList();
        pageRequest = getPageRequest(args);
        
        createQueriesForPagination(pageRequest, method, dataForQuery, builder, args, sortList, null);

        if (Page.class.equals(method.getReturnType())) {
            returnValue = new PageImpl<>(dataForQuery, args, pageRequest, em);
        } else {
            returnValue = new CursoredPageImpl<>(dataForQuery, args, pageRequest, em);
        }

        return returnValue;
    }

    public static void createQueriesForPagination(PageRequest pageRequest, Method method, QueryData dataForQuery,
                                                  StringBuilder builder,
                                                  Object[] args, List<Sort<?>> sortList, String rootAlias) {
        boolean forward = pageRequest == null || pageRequest.mode() != PageRequest.Mode.CURSOR_PREVIOUS;

        boolean isCursoredPage = CursoredPage.class.equals(method.getReturnType());
        if (isCursoredPage && dataForQuery.getQueryType() != QueryType.FIND) {
            //here to adapt query for Cursored mode
            preprocessQueryForCursoredMode(dataForQuery);
            builder = new StringBuilder(dataForQuery.getQueryString());
        }

        if (pageRequest.mode() != PageRequest.Mode.OFFSET) {
            createCursorQueries(dataForQuery, sortList, forward, args, rootAlias);
            builder = new StringBuilder(dataForQuery.getQueryString());
        }

        handleSort(dataForQuery, sortList, builder,
                dataForQuery.getQueryType() == QueryType.FIND, true, forward, rootAlias);
        dataForQuery.setQueryString(builder.toString());
        dataForQuery.setOrders(sortList);

        if (pageRequest.mode() != PageRequest.Mode.OFFSET) {
            updateCursorQueries(dataForQuery);
        }
    }
    
    private static void updateCursorQueries(QueryData dataForQuery) {
        if(dataForQuery.getQueryNext() != null && !dataForQuery.getQueryNext().contains("ORDER")) {
            dataForQuery.setQueryNext(dataForQuery.getQueryNext() + dataForQuery.getQueryOrder());
        }

        if(dataForQuery.getQueryPrevious() != null && !dataForQuery.getQueryPrevious().contains("ORDER")) {
            dataForQuery.setQueryPrevious(dataForQuery.getQueryPrevious() + dataForQuery.getQueryOrder());
        }
    }

    public static PageRequest getPageRequest(Object[] args) {
        for (Object param : args) {
            if (param instanceof PageRequest) { //Get info for PageRequest
                return (PageRequest) param;
            }
        }
        return null;
    }

    private static void preprocessQueryForCursoredMode(QueryData dataForQuery) {
        String sql = dataForQuery.getQueryString();
        int whereIndex = sql.indexOf("WHERE");
        StringBuilder sqlBuilder = new StringBuilder().append(sql.substring(0, whereIndex + 5)).append(" (")
                .append(sql.substring(whereIndex + 6, sql.length())).append(")");
        dataForQuery.setQueryString(sqlBuilder.toString());
    }

    public static void createCountQuery(QueryData dataForQuery, boolean hasWhere, int wherePosition) {
        StringBuilder builder = new StringBuilder();
        if (dataForQuery.getQueryType().equals(QueryType.FIND)) {
            builder.append("SELECT COUNT(").append("o").append(") FROM ")
                    .append(getSingleEntityName(dataForQuery.getDeclaredEntityClass().getName())).append(" o");
        } else if (dataForQuery.getQueryType().equals(QueryType.QUERY)) {
            builder.append("SELECT COUNT(").append("this").append(") FROM ")
                    .append(getSingleEntityName(dataForQuery.getDeclaredEntityClass().getName()));
        }

        if (hasWhere && dataForQuery.getQueryType().equals(QueryType.FIND)) {
            if (wherePosition >= 0) {
                builder.append(" WHERE").append(dataForQuery.getQueryString().substring(wherePosition + 5));
            } else {
                builder.append(" WHERE");
            }
        } else if (hasWhere && dataForQuery.getQueryType().equals(QueryType.QUERY)) {
            builder.append(" WHERE ").append(dataForQuery.getQueryString().substring(wherePosition + 5));
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
        return createBaseFindQuery(entityClass, orderByClause, entityMetadata, false);
    }

    public static String createBaseFindQuery(Class<?> entityClass, String orderByClause,
                                             EntityMetadata entityMetadata, boolean isAddDistinct) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT " + (isAddDistinct ? "DISTINCT " : ""))
               .append("o").append(" FROM ").append(getSingleEntityName(entityClass.getName())).append(" o");

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

    private static void verifyLimit(Limit limit, Query query) {
        if (limit != null) {
            // limit.startAt() is 1-based and guaranteed to be >= 1. JPA's setFirstResult is 0-based.
            query.setFirstResult((int) (limit.startAt() - 1));
            // limit.maxResults() is guaranteed to be >= 1.
            query.setMaxResults(limit.maxResults());
        }
    }

    private static void createCursorQueries(QueryData dataForQuery, List<Sort<?>> orders,
                                            boolean forward, Object[] args, String rootAlias) {
        String prefixForParams = dataForQuery.getJpqlParameters().isEmpty() ? "?" : ":cursor";
        boolean hasWhere = dataForQuery.getQueryString().contains("WHERE");
        StringBuilder cursorQuery = new StringBuilder().append(hasWhere ? " AND (" : " WHERE (");
        for (int i = 0; i < orders.size(); i++) {
            cursorQuery.append(i == 0 ? "(" : " OR (");
            int paramCount = getParamCount(dataForQuery, args);
            for (int k = 0; k <= i; k++) {
                Sort<?> sort = orders.get(k);
                String propertyName = sort.property();
                boolean asc = sort.isAscending();
                boolean lower = sort.ignoreCase();
                if (lower) {
                    cursorQuery.append(k == 0 ? "LOWER(" : " AND LOWER(");
                    appendAttribute(propertyName, cursorQuery, dataForQuery.getQueryType(), rootAlias);
                    cursorQuery.append(')');
                    if (forward) {
                        cursorQuery.append(k < i ? '=' : (asc ? '>' : '<'));
                    } else {
                        cursorQuery.append(k < i ? '=' : (asc ? '<' : '>'));
                    }
                    paramCount += 1;
                    cursorQuery.append("LOWER(").append(prefixForParams).append(paramCount).append(')');
                } else {
                    cursorQuery.append(k == 0 ? "" : " AND ");
                    appendAttribute(propertyName, cursorQuery, dataForQuery.getQueryType(), rootAlias);
                    if (forward) {
                        cursorQuery.append(k < i ? '=' : (asc ? '>' : '<'));
                    } else {
                        cursorQuery.append(k < i ? '=' : (asc ? '<' : '>'));
                    }
                    paramCount += 1;
                    cursorQuery.append(prefixForParams).append(paramCount);
                }
            }
            cursorQuery.append(")");
        }
        if (forward) {
            dataForQuery.setQueryNext(new StringBuilder(dataForQuery.getQueryString())
                    .append(cursorQuery).append(")").toString());
        } else {
            dataForQuery.setQueryPrevious(new StringBuilder(dataForQuery.getQueryString())
                    .append(cursorQuery).append(")").toString());
        }
    }
    
    public static int getParamCount(QueryData dataForQuery, Object[] args) {
        int paramCount = dataForQuery.getJpqlParameters().isEmpty() ? dataForQuery.getParamIndex() : dataForQuery.getJpqlParameters().size();
        if (paramCount == 0) {
            for (int i = 0; i < args.length; i++) {
                if (!excludeParameter(args[i])) {
                    paramCount = i + 1;
                }
            }
        }
        return paramCount;
    }

    public static void appendAttribute(String name, StringBuilder cursorQuery, QueryType queryType, String rootAlias) {
        if (name.charAt(name.length() - 1) != ')' && queryType == QueryType.FIND) {
            cursorQuery.append("o.");
        }
        
        if (rootAlias != null && !rootAlias.isEmpty()) {
            cursorQuery.append(rootAlias).append(".");
        }
        cursorQuery.append(name);
    }

    public static void setParameterFromCursor(Query query, PageRequest.Cursor cursor, List<Sort<?>> orders, QueryData dataForQuery, Object[] args) {
        int startValue = getParamCount(dataForQuery, args);
        if (dataForQuery.getJpqlParameters().isEmpty()) {
            for (Object cursorObject : cursor.elements()) {
                startValue += 1;
                query.setParameter(startValue, cursorObject);
            }
        } else {
            startValue = dataForQuery.getJpqlParameters().size();
            for (Object cursorObject : cursor.elements()) {
                startValue += 1;
                query.setParameter("cursor" + startValue, cursorObject);
            }
        }

    }
}
