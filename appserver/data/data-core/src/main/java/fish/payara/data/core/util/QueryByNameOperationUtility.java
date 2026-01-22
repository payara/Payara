/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2025-2026] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.data.core.util;

import fish.payara.data.core.cdi.extension.CursoredPageImpl;
import fish.payara.data.core.cdi.extension.EntityMetadata;
import fish.payara.data.core.cdi.extension.PageImpl;
import fish.payara.data.core.cdi.extension.QueryData;
import fish.payara.data.core.querymethod.QueryMethodParser;
import fish.payara.data.core.querymethod.QueryMethodSyntaxException;
import jakarta.data.Limit;
import jakarta.data.Sort;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.transaction.TransactionManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fish.payara.data.core.util.DataCommonOperationUtility.endTransaction;
import static fish.payara.data.core.util.DataCommonOperationUtility.extractDataParameter;
import static fish.payara.data.core.util.DataCommonOperationUtility.paginationPredicate;
import static fish.payara.data.core.util.DataCommonOperationUtility.processReturnQueryUpdate;
import static fish.payara.data.core.util.DataCommonOperationUtility.startTransactionAndJoin;
import static fish.payara.data.core.util.FindOperationUtility.createQueriesForPagination;
import static fish.payara.data.core.util.FindOperationUtility.excludeParameter;
import static fish.payara.data.core.util.FindOperationUtility.getPageRequest;
import static fish.payara.data.core.util.QueryOperationUtility.handleArrays;

/**
 * Utility class used to process Jakarta Data "Query by Method Name" operations.
 * This class builds and executes JPQL queries dynamically based on repository method names.
 */
public class QueryByNameOperationUtility {

    // --- PUBLIC METHODS FOR EACH "BY NAME" ACTION ---

    /**
     * Processes a FIND query derived from the method name (e.g., findBy...).
     */
    public static Object processFindByNameOperation(Object[] args, QueryData dataForQuery, EntityManager entityManager) {
        return buildAndExecuteQuery(args, dataForQuery, entityManager, QueryMethodParser.Action.FIND);
    }

    /**
     * Processes a DELETE operation derived from the method name (e.g., deleteBy...).
     * This is a two-step process to correctly handle JPA cascades:
     * 1. It internally calls a helper method to run a 'FIND' query, fetching all entities that match the criteria.
     * 2. It then iterates through the fetched entities and calls entityManager.remove() on each within a new transaction.
     * This method requires a TransactionManager to perform the modification.
     */
    public static Object processDeleteByNameOperation(Object[] args,
                                                      QueryData dataForQuery,
                                                      EntityManager entityManager,
                                                      TransactionManager transactionManager) {
        List<?> entitiesToDelete = findEntitiesForModification(args, dataForQuery, entityManager);

        if (entitiesToDelete.isEmpty()) {
            return processReturnQueryUpdate(dataForQuery.getMethod(), 0);
        }

        try {
            startTransactionAndJoin(transactionManager, entityManager, dataForQuery);

            for (Object entity : entitiesToDelete) {
                Object managed = entityManager.contains(entity) ? entity : entityManager.merge(entity);
                entityManager.remove(managed);
            }

            endTransaction(transactionManager, entityManager, dataForQuery);

            // Evict specific instances from cache instead of all instances of the entity type
            clearCachesForInstances(entityManager, dataForQuery.getDeclaredEntityClass(), entitiesToDelete);
            return processReturnQueryUpdate(dataForQuery.getMethod(), entitiesToDelete.size());

        } catch (OptimisticLockException ole) {
            doTransactionRollback(dataForQuery, transactionManager);
            throw new jakarta.data.exceptions.OptimisticLockingFailureException(ole.getMessage(), ole);
        } catch (Exception e) {
            doTransactionRollback(dataForQuery, transactionManager);
            throw new MappingException("Failed to execute delete operation", e);
        }
    }

    private static void doTransactionRollback(QueryData dataForQuery, TransactionManager transactionManager) {
        if (dataForQuery.isNewTransaction()) {
            try {
                int status = transactionManager.getStatus();
                if (status == jakarta.transaction.Status.STATUS_ACTIVE ||
                        status == jakarta.transaction.Status.STATUS_MARKED_ROLLBACK) {
                    transactionManager.rollback();
                }
                dataForQuery.setNewTransaction(false);
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Clears cache for specific entity instances instead of all instances of the type.
     * More efficient for large datasets as it only evicts the entities that were actually modified.
     * Falls back to type-level eviction if IDs cannot be extracted.
     */
    private static void clearCachesForInstances(EntityManager entityManager, Class<?> entityClass, List<?> entities) {
        EntityManagerFactory factory = entityManager.getEntityManagerFactory();
        if (factory != null) {
            Cache cache = factory.getCache();
            if (cache != null) {
                int evicted = 0;
                // Try to evict only the specific instances that were modified
                for (Object entity : entities) {
                    Object id = getEntityId(entity, entityClass);
                    if (id != null) {
                        cache.evict(entityClass, id);
                        evicted++;
                    }
                }
                // Fallback: if we couldn't extract any IDs, evict the entire type
                if (evicted == 0 && !entities.isEmpty()) {
                    cache.evict(entityClass);
                }
            }
        }
        entityManager.clear();
    }

    /**
     * Extracts the ID value from an entity instance using reflection.
     * Returns null if the ID cannot be extracted.
     */
    private static Object getEntityId(Object entity, Class<?> entityClass) {
        try {
            java.lang.reflect.Member idAccessor = EntityIntrospectionUtil.findIdAccessor(entityClass);
            if (idAccessor instanceof java.lang.reflect.Field) {
                java.lang.reflect.Field field = (java.lang.reflect.Field) idAccessor;
                field.setAccessible(true);
                return field.get(entity);
            } else if (idAccessor instanceof Method) {
                Method method = (Method) idAccessor;
                return method.invoke(entity);
            }
        } catch (Exception e) {
            // Log but don't throw - we'll handle this in the caller
            // by falling back to type-level eviction if needed
        }
        return null;
    }

    private static void clearCaches(EntityManager entityManager, Class<?> entityClass) {
        EntityManagerFactory factory = entityManager.getEntityManagerFactory();
        if (factory != null) {
            Cache cache = factory.getCache();
            if (cache != null) {
                // Only evict the affected entity type, not all entities
                cache.evict(entityClass);
            }
        }
        entityManager.clear();
    }

    /**
     * A specialized helper method that builds and executes a FIND query to fetch entities
     * that will be modified (e.g., deleted). It ALWAYS returns the full List of results,
     * bypassing the normal processReturnType logic which might mistakenly return a single element.
     */
    private static List<?> findEntitiesForModification(Object[] args, QueryData dataForQuery, EntityManager entityManager) {
        try {
            QueryMethodParser parser = new QueryMethodParser(dataForQuery.getMethod().getName()).parse();
            // We build the query as a FIND, regardless of the original method's action.
            jakarta.persistence.Query q = buildQueryFromParser(parser, args, dataForQuery, entityManager, QueryMethodParser.Action.FIND);
            return q.getResultList();
        } catch (QueryMethodSyntaxException | IllegalArgumentException e) {
            throw new MappingException("Failed to find entities for modification for method: " + dataForQuery.getMethod().getName(), e);
        }
    }

    /**
     * Processes a COUNT query derived from the method name (e.g., countBy...).
     */
    public static Object processCountByNameOperation(Object[] args, QueryData dataForQuery, EntityManager entityManager) {
        return buildAndExecuteQuery(args, dataForQuery, entityManager, QueryMethodParser.Action.COUNT);
    }

    /**
     * Processes an EXISTS query derived from the method name (e.g., existsBy...).
     */
    public static Object processExistsByNameOperation(Object[] args, QueryData dataForQuery, EntityManager entityManager) {
        return buildAndExecuteQuery(args, dataForQuery, entityManager, QueryMethodParser.Action.EXISTS);
    }

    // --- CENTRALIZED BUILD AND EXECUTION LOGIC ---

    private static Object buildAndExecuteQuery(Object[] args, QueryData dataForQuery, EntityManager entityManager, QueryMethodParser.Action expectedAction) {
        Method method = dataForQuery.getMethod();
        String methodName = method.getName();
        boolean evaluatePages = paginationPredicate.test(dataForQuery.getMethod());

        DataParameter parameter = extractDataParameter(args);
        Limit limitFromArgs = parameter.limit();

        try {
            QueryMethodParser parser = new QueryMethodParser(methodName).parse();
            if (parser.getAction() != expectedAction) {
                throw new IllegalStateException("Mismatched action type. Expected " + expectedAction + " but got " + parser.getAction());
            }
            if (evaluatePages) {
                return buildQueryFromParserWithPagination(parser, args, dataForQuery, entityManager, expectedAction);
            } else {
                jakarta.persistence.Query q = buildQueryFromParser(parser, args, dataForQuery, entityManager, expectedAction);
                if (limitFromArgs != null) {
                    if (limitFromArgs.startAt() > 1) {
                        q.setFirstResult((int) (limitFromArgs.startAt() - 1));
                    }
                    q.setMaxResults(limitFromArgs.maxResults());
                }
                return executeQuery(q, parser, dataForQuery);
            }
        } catch (QueryMethodSyntaxException | IllegalArgumentException e) {
            throw new MappingException("Failed to build or execute query from method name: " + methodName, e);
        }
    }

    /**
     * Builds a jakarta.persistence.Query object from a parsed method name.
     * This is a central helper used by all `...ByName` operations.
     */
    private static jakarta.persistence.Query buildQueryFromParser(QueryMethodParser parser, Object[] args,
                                                                  QueryData dataForQuery, EntityManager entityManager,
                                                                  QueryMethodParser.Action executionAction) {
        Metamodel metamodel = entityManager.getMetamodel();
        EntityType<?> rootEntityType = metamodel.entity(dataForQuery.getDeclaredEntityClass());

        String rootAlias = "e";
        StringBuilder jpql = new StringBuilder();
        StringBuilder joinClause = new StringBuilder();
        Map<String, String> joinAliases = new HashMap<>();

        buildQueryClause(jpql, executionAction, dataForQuery, rootAlias);

        DataParameter parameter = extractDataParameter(args);
        List<Sort<?>> dynamicSorts = parameter.sortList();
        buildJoins(joinClause, parser, rootEntityType, rootAlias, joinAliases);
        if (executionAction == QueryMethodParser.Action.FIND) {
            for (Sort<?> sort : dynamicSorts) {
                buildJoinsForPath(joinClause, rootEntityType, rootAlias, sort.property(), joinAliases);
            }
        }

        jpql.append(joinClause);

        if (!parser.getConditions().isEmpty()) {
            jpql.append(" WHERE ").append(buildWhereConditions(parser.getConditions(), rootEntityType, rootAlias, joinAliases, dataForQuery));
        }

        if (executionAction == QueryMethodParser.Action.FIND) {
            List<String> orderSegments = new ArrayList<>();
            if (!parser.getOrderBy().isEmpty()) {
                for (QueryMethodParser.OrderBy ob : parser.getOrderBy()) {
                    String propertyPath = findAliasedPath(ob.property(), rootEntityType, rootAlias, joinAliases);
                    String direction = (ob.ascDesc() == null || "Asc".equalsIgnoreCase(ob.ascDesc())) ? "ASC" : "DESC";
                    orderSegments.add(propertyPath + " " + direction);
                }
            }
            for (Sort<?> sort : dynamicSorts) {
                String propertyPath = findAliasedPath(sort.property(), rootEntityType, rootAlias, joinAliases);
                String expr = sort.ignoreCase() ? "UPPER(" + propertyPath + ")" : propertyPath;
                String direction = sort.isAscending() ? "ASC" : "DESC";
                orderSegments.add(expr + " " + direction);
            }
            if (!orderSegments.isEmpty()) {
                jpql.append(" ORDER BY ").append(String.join(", ", orderSegments));
            }
        }
        jakarta.persistence.Query q = entityManager.createQuery(jpql.toString());
        setQueryParameters(q, parser.getConditions(), args, dataForQuery);

        return q;
    }

    private static Object buildQueryFromParserWithPagination(QueryMethodParser parser, Object[] args,
                                                             QueryData dataForQuery, EntityManager entityManager,
                                                             QueryMethodParser.Action executionAction) {
        Metamodel metamodel = entityManager.getMetamodel();
        EntityType<?> rootEntityType = metamodel.entity(dataForQuery.getDeclaredEntityClass());
        PageRequest pageRequest = null;
        Object returnValue = null;
        String rootAlias = "e";
        StringBuilder jpql = new StringBuilder();
        StringBuilder joinClause = new StringBuilder();
        StringBuilder countClause = new StringBuilder();
        Map<String, String> joinAliases = new HashMap<>();
        DataParameter dataParameter = extractDataParameter(args);
        List<Sort<?>> sortList = dataParameter.sortList();
        pageRequest = getPageRequest(args);


        buildQueryClause(jpql, executionAction, dataForQuery, rootAlias);
        buildQueryClause(countClause, QueryMethodParser.Action.COUNT, dataForQuery, rootAlias);
        buildJoins(joinClause, parser, rootEntityType, rootAlias, joinAliases);

        jpql.append(joinClause);

        if (!parser.getConditions().isEmpty()) {
            jpql.append(" WHERE ").append(buildWhereConditions(parser.getConditions(), rootEntityType, rootAlias, joinAliases, dataForQuery));
            countClause.append(" WHERE ").append(buildWhereConditions(parser.getConditions(), rootEntityType, rootAlias, joinAliases, dataForQuery));
        }

        //consolidates order criteria from method name and order attributes
        List<Sort<?>> sortsFromMethodName = new ArrayList<>();
        if (!parser.getOrderBy().isEmpty()) {
            List<QueryMethodParser.OrderBy> orders = parser.getOrderBy();
            for (QueryMethodParser.OrderBy order : orders) {
                EntityMetadata entityMetadata = dataForQuery.getEntityMetadata();

                if (!entityMetadata.getAttributeNames().containsKey(order.property().toLowerCase())) {
                    throw new IllegalArgumentException("The attribute " + order.property() +
                            " is not mapped on the entity " + entityMetadata.getEntityName());
                }

                String direction = (order.ascDesc() == null || "Asc".equalsIgnoreCase(order.ascDesc())) ? "ASC" : "DESC";

                if (direction.equals("ASC")) {
                    sortsFromMethodName.add(Sort.asc(order.property()));
                } else {
                    sortsFromMethodName.add(Sort.desc(order.property()));
                }
            }
        }

        List<Sort<?>> finalOrders = new ArrayList<>();
        if (!parser.getOrderBy().isEmpty()) {
            finalOrders.addAll(sortsFromMethodName);
        }
        if (sortList != null && !sortList.isEmpty()) {
            Set<String> staticProperties = finalOrders.stream()
                    .map(Sort::property)
                    .collect(Collectors.toSet());
            sortList.stream()
                    .filter(sort -> !staticProperties.contains(sort.property()))
                    .forEach(finalOrders::add);
        }
        dataForQuery.setOrders(finalOrders);

        dataForQuery.setQueryString(jpql.toString());
        dataForQuery.setCountQueryString(jpql.toString());

        createQueriesForPagination(pageRequest, dataForQuery.getMethod(), dataForQuery,
                new StringBuilder(dataForQuery.getQueryString()), args, dataForQuery.getOrders(), rootAlias);

        if (Page.class.equals(dataForQuery.getMethod().getReturnType())) {
            returnValue = new PageImpl<>(dataForQuery, args, pageRequest, entityManager);
        } else {
            returnValue = new CursoredPageImpl<>(dataForQuery, args, pageRequest, entityManager);
        }

        return returnValue;
    }

    // --- QUERY BUILDING HELPER METHODS ---
    private static void buildQueryClause(StringBuilder jpql, QueryMethodParser.Action action, QueryData dataForQuery, String rootAlias) {
        String entityName = dataForQuery.getDeclaredEntityClass().getSimpleName();
        switch (action) {
            case FIND:
            case FIND_FOR_DELETE:
                jpql.append("SELECT DISTINCT ").append(rootAlias).append(" FROM ").append(entityName).append(" ").append(rootAlias);
                break;
            case COUNT:
            case EXISTS:
                jpql.append("SELECT COUNT(DISTINCT ").append(rootAlias).append(") FROM ").append(entityName).append(" ").append(rootAlias);
                break;
        }
    }

    private static void buildJoins(StringBuilder joinClause, QueryMethodParser parser, EntityType<?> rootEntityType, String rootAlias, Map<String, String> joinAliases) {
        for (QueryMethodParser.Condition condition : parser.getConditions()) {
            buildJoinsForPath(joinClause, rootEntityType, rootAlias, condition.property(), joinAliases);
        }
        if (parser.getAction() == QueryMethodParser.Action.FIND) {
            for (QueryMethodParser.OrderBy orderBy : parser.getOrderBy()) {
                buildJoinsForPath(joinClause, rootEntityType, rootAlias, orderBy.property(), joinAliases);
            }
        }
    }

    private static void buildJoinsForPath(StringBuilder joinClause, EntityType<?> currentEntityType, String currentAlias, String propertyPath, Map<String, String> joinAliases) {
        String[] parts = propertyPath.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Attribute<?, ?> attr = currentEntityType.getAttribute(part);
            String joinPath = currentAlias + "." + part;

            if (joinAliases.containsKey(joinPath)) {
                currentAlias = joinAliases.get(joinPath);
            } else {
                String newAlias = "j" + joinAliases.size();
                joinClause.append(" JOIN ").append(joinPath).append(" ").append(newAlias);
                joinAliases.put(joinPath, newAlias);
                currentAlias = newAlias;
            }
            currentEntityType = (EntityType<?>) (attr.isCollection() ?
                    ((jakarta.persistence.metamodel.PluralAttribute) attr).getElementType() :
                    ((jakarta.persistence.metamodel.SingularAttribute) attr).getType());
        }
    }

    private static String buildWhereConditions(List<QueryMethodParser.Condition> conditions, EntityType<?> rootEntityType, String rootAlias, Map<String, String> joinAliases, QueryData dataForQuery) {
        StringBuilder whereClause = new StringBuilder();
        boolean firstCondition = true;
        int paramIndex = 0;
        for (QueryMethodParser.Condition condition : conditions) {
            if (!firstCondition) {
                whereClause.append(" ").append(condition.precedingOperator().name()).append(" ");
            }
            firstCondition = false;

            String propertyPath = findAliasedPath(condition.property(), rootEntityType, rootAlias, joinAliases);
            String propertyExpression = condition.ignoreCase() ? "LOWER(" + propertyPath + ")" : propertyPath;

            if (condition.not()) whereClause.append("NOT (");
            whereClause.append(propertyExpression);

            if (condition.operator() == null) {
                whereClause.append(" = ").append(condition.ignoreCase() ? "LOWER(" : "").append("?").append(++paramIndex).append(condition.ignoreCase() ? ")" : "");
            } else {
                switch (condition.operator()) {
                    case "Like", "StartsWith", "EndsWith", "Contains" ->
                            whereClause.append(" LIKE ?").append(++paramIndex);
                    case "LessThan" -> whereClause.append(" < ?").append(++paramIndex);
                    case "LessThanEqual" -> whereClause.append(" <= ?").append(++paramIndex);
                    case "GreaterThan" -> whereClause.append(" > ?").append(++paramIndex);
                    case "GreaterThanEqual" -> whereClause.append(" >= ?").append(++paramIndex);
                    case "Between" ->
                            whereClause.append(" BETWEEN ?").append(++paramIndex).append(" AND ?").append(++paramIndex);
                    case "In" -> whereClause.append(" IN ?").append(++paramIndex);
                    case "Null" -> whereClause.append(" IS NULL");
                    case "True" -> whereClause.append(" = TRUE");
                    case "False" -> whereClause.append(" = FALSE");
                    default ->
                            throw new UnsupportedOperationException("Operator " + condition.operator() + " not supported.");
                }
            }
            if (condition.not()) whereClause.append(")");
        }
        return whereClause.toString();
    }

    private static String buildOrderByClause(List<QueryMethodParser.OrderBy> orderByList, EntityType<?> rootEntityType, String rootAlias, Map<String, String> joinAliases) {
        StringBuilder orderByClause = new StringBuilder(" ORDER BY ");
        StringJoiner joiner = new StringJoiner(", ");
        for (QueryMethodParser.OrderBy orderBy : orderByList) {
            String propertyPath = findAliasedPath(orderBy.property(), rootEntityType, rootAlias, joinAliases);
            String direction = (orderBy.ascDesc() == null || "Asc".equalsIgnoreCase(orderBy.ascDesc())) ? "ASC" : "DESC";
            joiner.add(propertyPath + " " + direction);
        }
        orderByClause.append(joiner.toString());
        return orderByClause.toString();
    }

    private static String findAliasedPath(String propertyPath, EntityType<?> rootEntityType, String rootAlias, Map<String, String> joinAliases) {
        String[] parts = propertyPath.split("\\.");
        String currentAlias = rootAlias;
        String resolvedPathSoFar = rootAlias;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            resolvedPathSoFar = currentAlias + "." + part;
            currentAlias = joinAliases.get(resolvedPathSoFar);
        }

        return currentAlias + "." + parts[parts.length - 1];
    }

    private static void setQueryParameters(jakarta.persistence.Query q, List<QueryMethodParser.Condition> conditions, Object[] args, QueryData dataForQuery) {
        List<Object> queryArgs = getQueryArguments(args);
        int argIndex = 0;
        int paramIndex = 0;
        for (QueryMethodParser.Condition condition : conditions) {
            if (condition.operator() != null && (condition.operator().equals("Null") || condition.operator().equals("True") || condition.operator().equals("False"))) {
                continue;
            }

            Object arg = queryArgs.get(argIndex);
            Object processedArg = condition.ignoreCase() && arg instanceof String ? ((String) arg).toLowerCase() : arg;

            if ("StartsWith".equals(condition.operator())) {
                processedArg = processedArg + "%";
            } else if ("EndsWith".equals(condition.operator())) {
                processedArg = "%" + processedArg;
            } else if ("Contains".equals(condition.operator()) || "Like".equals(condition.operator())) {
                processedArg = "%" + processedArg + "%";
            }

            q.setParameter(++paramIndex, processedArg);
            argIndex++;

            if ("Between".equals(condition.operator())) {
                Object arg2 = queryArgs.get(argIndex);
                Object processedArg2 = condition.ignoreCase() && arg2 instanceof String ? ((String) arg2).toLowerCase() : arg2;
                q.setParameter(++paramIndex, processedArg2);
                argIndex++;
            }
        }
    }

    private static Object executeQuery(jakarta.persistence.Query q, QueryMethodParser parser, QueryData dataForQuery) {
        QueryMethodParser.Action action = parser.getAction();
        switch (action) {
            case FIND:
                if (parser.getLimit() != null) {
                    q.setMaxResults(parser.getLimit());
                }
                return processReturnType(dataForQuery, q.getResultList());
            case COUNT:
                return q.getSingleResult();
            case EXISTS:
                long count = (long) q.getSingleResult();
                return count > 0;
            default:
                throw new IllegalStateException("Unexpected execution action: " + action);
        }
    }

    private static Object processReturnType(QueryData data, List<?> resultList) {
        Class<?> returnType = data.getMethod().getReturnType();

        if (List.class.isAssignableFrom(returnType)) {
            return resultList;
        }
        if (Stream.class.isAssignableFrom(returnType)) {
            return resultList.stream();
        }
        if (Optional.class.isAssignableFrom(returnType)) {
            return resultList.stream().findFirst();
        }
        if (resultList != null && resultList.isEmpty() && data.getDeclaredEntityClass().equals(returnType)) {
            throw new EmptyResultException("The expected result is empty, to return an empty result you should need to return a different type" +
                    "like: List, Optional, Page, CursorPage or Stream");
        }
        if (!returnType.isArray() && data.getDeclaredEntityClass().equals(returnType)) {
            if (resultList.size() > 1) {
                throw new NonUniqueResultException(
                        "Query method " + data.getMethod().getName() +
                                " is expected to return a single result but found " + resultList.size() + " results"
                );
            }
            return resultList.get(0);
        }
        return handleArrays(resultList, returnType);
    }

    private static List<Object> getQueryArguments(Object[] allArgs) {
        if (allArgs == null) {
            return Collections.emptyList();
        }
        List<Object> queryArgs = new ArrayList<>();
        for (Object arg : allArgs) {
            if (arg != null && !excludeParameter(arg)) {
                queryArgs.add(arg);
            }
        }
        return queryArgs;
    }
}
