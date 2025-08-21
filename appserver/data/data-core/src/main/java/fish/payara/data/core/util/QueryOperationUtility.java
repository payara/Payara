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

import fish.payara.data.core.cdi.extension.QueryData;
import jakarta.annotation.Nullable;
import jakarta.data.Limit;
import jakarta.data.Sort;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.persistence.EntityManager;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static fish.payara.data.core.util.DataCommonOperationUtility.endTransaction;
import static fish.payara.data.core.util.DataCommonOperationUtility.handleSort;
import static fish.payara.data.core.util.DataCommonOperationUtility.paginationPredicate;
import static fish.payara.data.core.util.DataCommonOperationUtility.processReturnQueryUpdate;
import static fish.payara.data.core.util.DataCommonOperationUtility.startTransactionAndJoin;
import static fish.payara.data.core.util.FindOperationUtility.excludeParameter;
import static fish.payara.data.core.util.FindOperationUtility.getSingleEntityName;
import static fish.payara.data.core.util.FindOperationUtility.parametersToExclude;
import static fish.payara.data.core.util.FindOperationUtility.processPagination;

/**
 * Utility class used to process Jakarta Data query operations
 */
public class QueryOperationUtility {

    private static final List<String> selectQueryPatterns = List.of("SELECT", "FROM", "WHERE", "ORDER", "BY", "GROUP", "HAVING");
    private static final List<String> deleteQueryPatterns = List.of("DELETE", "FROM", "WHERE");
    private static final List<String> updateQueryPatterns = List.of("UPDATE", "SET", "WHERE");

    private static final Predicate<Character> deletePredicate = c -> c == 'D' || c == 'd';
    private static final Predicate<Character> updatePredicate = c -> c == 'U' || c == 'u';


    public static Object processQueryOperation(Object[] args, QueryData dataForQuery, EntityManager entityManager,
                                               TransactionManager transactionManager, DataParameter dataParameter) throws HeuristicRollbackException, SystemException, HeuristicMixedException, RollbackException, NotSupportedException {
        Method method = dataForQuery.getMethod();
        Query queryAnnotation = method.getAnnotation(Query.class);
        String mappedQuery = queryAnnotation.value();

        mappedQuery = handlesEmptyQuery(dataForQuery, mappedQuery);

        boolean evaluatePages = paginationPredicate.test(dataForQuery.getMethod());
        int length = mappedQuery.length();
        int firstCharPosition = 0;
        char firstChar = ' ';
        while (firstCharPosition < length && Character.isWhitespace(firstChar)) {
            firstChar = mappedQuery.charAt(firstCharPosition);
            firstCharPosition++;
        }

        Map<Integer, String> patternSelectPositions = new LinkedHashMap<>();
        Map<Integer, String> patternDeletePositions;
        Map<Integer, String> patternUpdatePositions;
        Map<String, Set<String>> queryMapping = null;
        if (queryAnnotation != null) {
            if (!mappedQuery.isEmpty()) {
                if (deletePredicate.test(firstChar)) {
                    patternDeletePositions = preprocessQueryString(mappedQuery, deleteQueryPatterns);
                    queryMapping = processQuery(mappedQuery, patternDeletePositions, dataForQuery);
                } else if (updatePredicate.test(firstChar)) {
                    patternUpdatePositions = preprocessQueryString(mappedQuery, updateQueryPatterns);
                    queryMapping = processQuery(mappedQuery, patternUpdatePositions, dataForQuery);
                } else {
                    patternSelectPositions = preprocessQueryString(mappedQuery, selectQueryPatterns);
                    queryMapping = processQuery(mappedQuery, patternSelectPositions, dataForQuery);
                }
            }
        }

        Object objectToReturn = null;
        if (!evaluatePages) {
            for (Map.Entry<String, Set<String>> entry : queryMapping.entrySet()) {
                String query = entry.getKey();
                List<Sort<?>> sortList = dataParameter.sortList();
                if (!sortList.isEmpty()) {
                    query = handleSort(dataForQuery, sortList, query, false, false, false);
                }
                jakarta.persistence.Query q = entityManager.createQuery(query);
                validateParameters(dataForQuery, entry.getValue(), queryAnnotation.value());
                if (!entry.getValue().isEmpty()) {
                    Object[] params = dataForQuery.getJpqlParameters().toArray();
                    for (int i = 0; i < params.length; i++) {
                        q.setParameter((String) params[i], args[i]);
                    }
                } else {
                    for (int i = 1; args != null && i <= args.length; i++) {
                        if (!excludeParameter(args[i - 1])) {
                            q.setParameter(i, args[i - 1]);
                        }
                    }
                }
                Limit limit = dataParameter.limit();
                if (limit != null) {
                    q.setFirstResult((int) (limit.startAt() - 1));
                    q.setMaxResults(limit.maxResults());
                }

                if (deletePredicate.test(firstChar) || updatePredicate.test(firstChar)) {
                    startTransactionAndJoin(transactionManager, entityManager, dataForQuery);
                    int deleteReturn = q.executeUpdate();
                    endTransaction(transactionManager, entityManager, dataForQuery);

                    return processReturnQueryUpdate(method, deleteReturn);
                } else {
                    objectToReturn = processReturnType(dataForQuery, q.getResultList());
                }
            }
        } else {
            for (Map.Entry<String, Set<String>> entry : queryMapping.entrySet()) {
                validateParameters(dataForQuery, entry.getValue(), queryAnnotation.value());
            }
            objectToReturn = processPagination(entityManager, dataForQuery, args,
                    method, new StringBuilder(dataForQuery.getQueryString()), 
                    patternSelectPositions.containsValue("WHERE"), dataParameter);
        }

        return objectToReturn;
    }

    private static String handlesEmptyQuery(QueryData dataForQuery, String mappedQuery) {
        if (mappedQuery == null || mappedQuery.trim().isEmpty()) {
            String entityName = dataForQuery.getDeclaredEntityClass().getSimpleName();
            mappedQuery = "FROM " + entityName;
            dataForQuery.setQueryString(mappedQuery);
        }
        return mappedQuery;
    }

    public static Map<String, Set<String>> processQuery(String queryString,
                                                        Map<Integer, String> patternPositions, QueryData dataForQuery) {
        int querySize = queryString.length();
        int startIndex = 0;
        Set<String> parameters = new HashSet<>();
        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder paramName = null;
        while (startIndex < querySize) {
            char queryCharacter = queryString.charAt(startIndex);
            if (Character.isWhitespace(queryCharacter)) {
                startIndex++;
                if (paramName != null) {
                    parameters.add(paramName.toString());
                    paramName = null;
                }
                continue;
            } else if (queryCharacter == ':') {
                paramName = new StringBuilder();
            } else if (patternPositions.containsKey(startIndex)
                    && queryString.regionMatches(true, startIndex, patternPositions.get(startIndex), 0, patternPositions.get(startIndex).length())
                    && !Character.isJavaIdentifierPart(queryString.charAt(startIndex + patternPositions.get(startIndex).length()))) {
                addQueryPart(queryBuilder, patternPositions.get(startIndex),
                        queryString, dataForQuery.getDeclaredEntityClass(), patternPositions);
                startIndex += patternPositions.get(startIndex).length();
                continue;
            } else if (Character.isJavaIdentifierStart(queryCharacter)) {
                if (paramName != null) {
                    paramName.append(queryCharacter);
                    while (querySize > startIndex + 1 &&
                            Character.isJavaIdentifierPart(queryCharacter = queryString.charAt(startIndex + 1))) {
                        paramName.append(queryCharacter);
                        startIndex++;
                    }
                }
            } else if (Character.isDigit(queryCharacter)) {
                if (paramName != null) {
                    paramName.append(queryCharacter);
                }
            }
            startIndex++;
        }

        if (paramName != null) {
            parameters.add(paramName.toString());
        }

        Map<String, Set<String>> queryMapping = new LinkedHashMap<>();
        queryMapping.put(queryBuilder.toString(), parameters);
        dataForQuery.setQueryString(queryBuilder.toString());
        return queryMapping;
    }

    public static Map<Integer, String> preprocessQueryString(String queryString, List<String> patterns) {
        Map<Integer, String> patternPositions = new LinkedHashMap<>();
        for (String p : patterns) {
            int startIndex = queryString.toLowerCase().indexOf(p.toLowerCase());
            if (startIndex != -1) {
                patternPositions.put(startIndex, p);
            }
        }
        return patternPositions;
    }


    public static void addQueryPart(StringBuilder queryBuilder, String part, String query, Class<?> entityClass, Map<Integer, String> patternPositions) {
        switch (part) {
            case "SELECT" -> {
                queryBuilder.append("SELECT");
                int selectIndex = getIndexFromMap("SELECT", patternPositions);
                if (patternPositions.containsValue("FROM")) {
                    int fromIndex = getIndexFromMap("FROM", patternPositions);
                    queryBuilder.append(query.substring(selectIndex + 6, fromIndex));
                } else if (patternPositions.containsValue("WHERE")) {
                    int whereIndex = getIndexFromMap("WHERE", patternPositions);
                    queryBuilder.append(query.substring(selectIndex + 6, whereIndex));
                }
            }
            case "DELETE" -> {
                queryBuilder.append("DELETE");
                int deleteIndex = getIndexFromMap("DELETE", patternPositions);
                if (patternPositions.containsValue("FROM")) {
                    int fromIndex = getIndexFromMap("FROM", patternPositions);
                    queryBuilder.append(query.substring(deleteIndex + 6, fromIndex));
                }
            }
            case "UPDATE" -> {
                queryBuilder.append("UPDATE");
                int updateIndex = getIndexFromMap("UPDATE", patternPositions);
                if (patternPositions.containsValue("SET")) {
                    int setIndex = getIndexFromMap("SET", patternPositions);
                    queryBuilder.append(query.substring(updateIndex + 6, setIndex));
                }
            }
            case "FROM" -> {
                String entityName = getSingleEntityName(entityClass.getName());
                if (entityName != null) {
                    queryBuilder.append("FROM ").append(entityName);
                } else {
                    //need to see the resolution of entity from query path
                }
            }
            case "SET" -> {
                queryBuilder.append(" SET ");
                int setIndex = getIndexFromMap("SET", patternPositions);
                if (patternPositions.containsValue("WHERE")) {
                    int whereIndex = getIndexFromMap("WHERE", patternPositions);
                    queryBuilder.append(query.substring(setIndex + 3, whereIndex));
                }
            }
            case "WHERE" -> {
                if (!patternPositions.containsValue("FROM") && !patternPositions.containsValue("UPDATE")) {
                    queryBuilder.append(" FROM ").append(getSingleEntityName(entityClass.getName())).append(" WHERE ");
                } else {
                    queryBuilder.append(" WHERE ");
                }

                if (!patternPositions.containsValue("ORDER") && !patternPositions.containsValue("GROUP") && !patternPositions.containsValue("HAVING")) {
                    int whereIndex = getIndexFromMap("WHERE", patternPositions);
                    queryBuilder.append(query.substring(whereIndex + 6));
                }
            }
            case "ORDER" -> {
                int whereIndex = getIndexFromMap("WHERE", patternPositions);
                int orderIndex = getIndexFromMap("ORDER", patternPositions);
                queryBuilder.append(query.substring(whereIndex + 6, orderIndex));
                if (patternPositions.containsValue("BY")) {
                    queryBuilder.append(" ORDER BY ").append(query.substring(orderIndex + 9));
                }
            }
            case "GROUP" -> {
                int whereIndex = getIndexFromMap("WHERE", patternPositions);
                int groupIndex = getIndexFromMap("GROUP", patternPositions);
                int havingIndex = getIndexFromMap("HAVING", patternPositions);
                queryBuilder.append(query.substring(whereIndex + 6, groupIndex));
                if (patternPositions.containsValue("BY") && patternPositions.containsValue("HAVING")) {
                    queryBuilder.append(" GROUP BY ").append(query.substring(groupIndex + 9, havingIndex));
                } else {
                    queryBuilder.append(" GROUP BY ").append(query.substring(groupIndex + 9));
                }
            }
            case "HAVING" -> {
                int havingIndex = getIndexFromMap("HAVING", patternPositions);
                queryBuilder.append(" HAVING ").append(query.substring(havingIndex + 6));
            }
        }
    }

    public static Integer getIndexFromMap(String valueSearch, Map<Integer, String> patternPositions) {
        Optional<Map.Entry<Integer, String>> optionalEntry = patternPositions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(valueSearch)).findFirst();
        if (optionalEntry.isPresent()) {
            return optionalEntry.get().getKey();
        } else {
            return -1;
        }
    }

    public static void validateParameters(QueryData dataForQuery, Set<String> parameters, String query) {
        Method method = dataForQuery.getMethod();
        if (!parameters.isEmpty()) {
            Set<String> jpqlParameters = dataForQuery.getJpqlParameters();
            for (String parameter : parameters) {
                boolean found = false;
                for (Parameter parameterMethod : method.getParameters()) {
                    Param param = parameterMethod.getAnnotation(Param.class);
                    String paramName = null;
                    if (param != null) {
                        paramName = param.value();
                        jpqlParameters.add(paramName);
                    } else if (parameterMethod.isNamePresent()
                            && !isPaginationType(parameterMethod)) {
                        paramName = parameterMethod.getName();
                        jpqlParameters.add(paramName);
                    }

                    if (paramName != null && paramName.equals(parameter)) {
                        found = true;
                    }
                }
                if (!found) {
                    String message = String.format(
                            """
                                    One param from the method %s from the repository class %s
                                    is not defining a parameter with name :%s. The Query value: %s requires
                                    this value. Try use @Param annotation to define the parameter name in case you didn't add 
                                    the -parameters option to the java compiler that resolves the parameter names.
                                    """,
                            method.getName(), dataForQuery.getRepositoryInterface().getName(), parameter, query);
                    throw new MappingException(message);
                }
            }
        }
    }

    public static boolean isPaginationType(Parameter parameterMethod) {
        Optional<Class<?>> found = parametersToExclude.stream().filter(p -> p.equals(parameterMethod.getParameterizedType())
                || p.equals(parameterMethod.getType())).findAny();
        return found.isPresent();
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
            return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.get(0));
        }

        if (!returnType.isArray() && data.getDeclaredEntityClass().equals(returnType)) {
            if (resultList == null || resultList.isEmpty()) {
                throw new EmptyResultException("No result found for query");
            }
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

    @Nullable
    static Object handleArrays(List<?> resultList, Class<?> returnType) {
        if (returnType.isArray()) {
            Class<?> componentType = returnType.getComponentType();
            Object array = java.lang.reflect.Array.newInstance(componentType, resultList.size());
            for (int i = 0; i < resultList.size(); i++) {
                java.lang.reflect.Array.set(array, i, resultList.get(i));
            }
            return array;
        }

        return resultList.isEmpty() ? null : resultList.get(0);
    }

}
