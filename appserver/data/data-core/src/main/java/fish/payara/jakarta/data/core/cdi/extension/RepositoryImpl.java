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
package fish.payara.jakarta.data.core.cdi.extension;

import fish.payara.jakarta.data.core.util.DeleteOperationUtility;
import fish.payara.jakarta.data.core.util.FindOperationUtility;
import fish.payara.jakarta.data.core.util.QueryByNameOperationUtility;
import fish.payara.jakarta.data.core.util.QueryOperationUtility;
import jakarta.data.Limit;
import jakarta.data.Sort;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.Page;
import jakarta.data.repository.By;
import jakarta.data.repository.OrderBy;
import jakarta.persistence.EntityManager;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import static fish.payara.jakarta.data.core.util.DataCommonOperationUtility.evaluateReturnTypeVoidPredicate;
import static fish.payara.jakarta.data.core.util.DataCommonOperationUtility.findEntityTypeInMethod;
import static fish.payara.jakarta.data.core.util.DataCommonOperationUtility.getEntityManager;
import static fish.payara.jakarta.data.core.util.DataCommonOperationUtility.processReturnQueryUpdate;
import static fish.payara.jakarta.data.core.util.DataCommonOperationUtility.processReturnType;
import static fish.payara.jakarta.data.core.util.InsertAndSaveOperationUtility.processInsertAndSaveOperationForArray;

/**
 * This is a generic class that represent the proxy to be used during runtime
 *
 * @param <T>
 */
public class RepositoryImpl<T> implements InvocationHandler {

    public static final Logger logger = Logger.getLogger(RepositoryImpl.class.getName());

    private final Class<T> repositoryInterface;
    private Map<Class<?>, List<QueryData>> queriesPerEntityClass;
    private final Map<Method, QueryData> queries = new HashMap<>();
    private final String applicationName;
    private TransactionManager transactionManager;
    private EntityManager em;

    public RepositoryImpl(Class<T> repositoryInterface, Map<Class<?>, List<QueryData>> queriesPerEntityClass, String applicationName) {
        this.repositoryInterface = repositoryInterface;
        this.queriesPerEntityClass = queriesPerEntityClass;
        this.applicationName = applicationName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //In this method we can add implementation to execute dynamic queries
        logger.info("executing method:" + method.getName());
        preProcessQuery();
        QueryData dataForQuery = queries.get(method);
        evaluateDataQuery(dataForQuery, method);
        Object objectToReturn = null;
        switch (dataForQuery.getQueryType()) {
            case SAVE -> objectToReturn = processSaveOperation(args, dataForQuery);
            case INSERT -> objectToReturn = processInsertOperation(args, dataForQuery);
            case DELETE ->
                    objectToReturn = processDeleteOperation(args, dataForQuery.getDeclaredEntityClass(), dataForQuery.getMethod());
            case UPDATE -> objectToReturn = processUpdateOperation(args, dataForQuery);
            case FIND -> objectToReturn = processFindOperation(args, dataForQuery);
            case QUERY -> objectToReturn = processQueryOperation(args, dataForQuery);
            case FIND_BY_NAME -> objectToReturn = QueryByNameOperationUtility.processFindByNameOperation(args, dataForQuery, getEntityManager(this.applicationName));
            case DELETE_BY_NAME -> objectToReturn = QueryByNameOperationUtility.processDeleteByNameOperation(args, dataForQuery, getEntityManager(this.applicationName), getTransactionManager());
            case COUNT_BY_NAME -> objectToReturn = QueryByNameOperationUtility.processCountByNameOperation(args, dataForQuery, getEntityManager(this.applicationName));
            case EXISTS_BY_NAME -> objectToReturn = QueryByNameOperationUtility.processExistsByNameOperation(args, dataForQuery, getEntityManager(this.applicationName));
            default -> throw new UnsupportedOperationException("QueryType " + dataForQuery.getQueryType() + " not supported.");
        }

        return objectToReturn;
    }

    private void evaluateDataQuery(QueryData dataForQuery, Method method) {
        if (dataForQuery.getDeclaredEntityClass() == null) {
            Class<?> entityType = findEntityTypeInMethod(method);
            if (entityType != null) {
                dataForQuery.setDeclaredEntityClass(entityType);
                return;
            }
            Class<?> declaringClass = method.getDeclaringClass();
            Method[] allMethods = declaringClass.getMethods();
            for (Method interfaceMethod : allMethods) {
                if (interfaceMethod.equals(method)) {
                    continue;
                }
                entityType = findEntityTypeInMethod(interfaceMethod);
                if (entityType != null) {
                    dataForQuery.setDeclaredEntityClass(entityType);
                    return;
                }
            }
            throw new MappingException(
                    String.format("Could not determine primary entity type for repository method '%s' in %s. " +
                                    "Either extend a repository interface with entity type parameters or " +
                                    "ensure entity type is determinable from method signature.",
                            method.getName(), declaringClass.getName())
            );
        }
    }

    public Object processFindOperation(Object[] args, QueryData dataForQuery) {
        Limit limit = null;
        Annotation[][] parameterAnnotations = dataForQuery.getMethod().getParameterAnnotations();
        boolean evaluatePages = Page.class.equals(dataForQuery.getMethod().getReturnType());
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Limit) {
                    limit = (Limit) arg;
                }
            }
        }

        if (parameterAnnotations.length > 0) {
            Object returnObject = FindOperationUtility.processFindByOperation(
                    args, getEntityManager(this.applicationName),
                    dataForQuery, limit, evaluatePages);

            if (returnObject != null && (returnObject instanceof List<?>)) {
                List<Object> resultList = (List<Object>) returnObject;
                return processReturnType(dataForQuery, resultList);
            } else {
                //here to return the instance of a page
                return returnObject;
            }
        } else {
            // For "findAll" operations
            return FindOperationUtility.processFindAllOperation(dataForQuery.getDeclaredEntityClass(), getEntityManager(this.applicationName),
                    extractOrderByClause(dataForQuery.getMethod()), dataForQuery.getEntityMetadata(), limit
            );
        }
    }

    private String extractOrderByClause(Method method) {
        OrderBy.List orderByList = method.getAnnotation(OrderBy.List.class);
        if (orderByList != null && orderByList.value().length > 0) {
            return String.join(", ", Arrays.stream(orderByList.value())
                    .map(this::formatOrderByClause)
                    .toArray(String[]::new));
        }
        OrderBy orderBy = method.getAnnotation(OrderBy.class);
        if (orderBy != null) {
            return formatOrderByClause(orderBy);
        }
        return null;
    }

    private String formatOrderByClause(OrderBy orderBy) {
        StringBuilder clause = new StringBuilder();
        if (orderBy.ignoreCase()) {
            clause.append("LOWER(").append(orderBy.value()).append(")");
        } else {
            clause.append(orderBy.value());
        }
        if (orderBy.descending()) {
            clause.append(" DESC");
        }
        return clause.toString();
    }

    public void preProcessQuery() {
        Map<Method, QueryData> r = queriesPerEntityClass.entrySet().stream().map(e -> e.getValue())
                .flatMap(List::stream).collect(Collectors.toMap(QueryData::getMethod, Function.identity()));
        queries.putAll(r);
    }

    public Object processSaveOperation(Object[] args, QueryData dataForQuery) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        List<Object> results = null;
        Object entity = null;
        Object arg = args[0] instanceof Stream ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) : args[0];
        startTransactionComponents();

        if (dataForQuery.getEntityParamType().isArray()) { //save multiple entities from array reference
            return processInsertAndSaveOperationForArray(args, getTransactionManager(), getEntityManager(this.applicationName), dataForQuery);
        } else if (arg instanceof Iterable toIterate) { //save multiple entities from list reference
            results = new ArrayList<>();
            startTransactionAndJoin();
            for (Object e : ((Iterable<?>) toIterate)) {
                results.add(em.merge(e));
            }
            endTransaction();
            if (!results.isEmpty()) {
                return processReturnType(dataForQuery, results);
            }
        } else if (args[0] != null) { //save a single entity
            startTransactionAndJoin();
            entity = em.merge(args[0]);
            endTransaction();
        }

        if (evaluateReturnTypeVoidPredicate.test(dataForQuery.getMethod().getReturnType())) {
            entity = null;
        }
        return entity;
    }

    public Object processInsertOperation(Object[] args, QueryData dataForQuery) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        List<Object> results = null;
        Object entity = null;
        Object arg = args[0] instanceof Stream ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) : args[0];
        startTransactionComponents();

        if (dataForQuery.getEntityParamType().isArray()) { //insert multiple entities from array reference
            return processInsertAndSaveOperationForArray(args, getTransactionManager(), getEntityManager(this.applicationName), dataForQuery);
        } else if (arg instanceof Iterable toIterate) {  //insert multiple entities from list reference
            results = new ArrayList<>();
            startTransactionAndJoin();
            for (Object e : ((Iterable<?>) toIterate)) {
                em.persist(e);
                results.add(e);
            }
            endTransaction();

            if (!results.isEmpty()) {
                return processReturnType(dataForQuery, results);
            }
        } else if (arg != null) { //insert a single entity
            startTransactionAndJoin();
            entity = args[0];
            em.persist(args[0]);
            endTransaction();
        }

        if (evaluateReturnTypeVoidPredicate.test(dataForQuery.getMethod().getReturnType())) {
            entity = null;
        }

        return entity;
    }

    public Object processDeleteOperation(Object[] args, Class<?> declaredEntityClass, Method method)
            throws SystemException, NotSupportedException, HeuristicRollbackException,
            HeuristicMixedException, RollbackException {
        int returnValue = 0;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        if (args == null) { // delete all records
            startTransactionComponents();
            startTransactionAndJoin();
            String deleteAllQuery = "DELETE FROM " + declaredEntityClass.getSimpleName();
            returnValue = em.createQuery(deleteAllQuery).executeUpdate();
            endTransaction();
        } else if (args[0] instanceof List arr) {
            // existing list handling code
            startTransactionComponents();
            startTransactionAndJoin();
            List<Object> ids = getIds((List<?>) arr);
            if (!ids.isEmpty()) {
                String deleteQuery = "DELETE FROM " + declaredEntityClass.getSimpleName() + " e WHERE e.id IN :ids";
                returnValue = em.createQuery(deleteQuery)
                        .setParameter("ids", ids)
                        .executeUpdate();
            }
            endTransaction();
        } else {
            // Handle @By annotation cases
            Optional<Annotation> byFound = Arrays.stream(parameterAnnotations)
                    .flatMap(Arrays::stream)
                    .filter(a -> a instanceof By)
                    .findFirst();
            final boolean hasByAnnotation = byFound.isPresent();

            if (hasByAnnotation) {
                startTransactionComponents();
                QueryData queryData = queries.get(method);
                returnValue = DeleteOperationUtility.processDeleteByOperation(
                        args,
                        declaredEntityClass,
                        getTransactionManager(),
                        getEntityManager(this.applicationName),
                        queryData.getEntityMetadata(),
                        method
                );
            } else if (args[0] != null) { // delete single entity
                startTransactionComponents();
                startTransactionAndJoin();
                try {
                    Method getId = args[0].getClass().getMethod("getId");
                    Object id = getId.invoke(args[0]);
                    String deleteQuery = "DELETE FROM " + declaredEntityClass.getSimpleName() + " e WHERE e.id = :id";
                    returnValue = em.createQuery(deleteQuery)
                            .setParameter("id", id)
                            .executeUpdate();
                } catch (Exception e) {
                    throw new RuntimeException("Error to get entity ID", e);
                }
                endTransaction();
            }
        }

        return processReturnQueryUpdate(method, returnValue);
    }

    private static List<Object> getIds(List<?> arr) {
        List<Object> ids = arr.stream()
                .map(entity -> {
                    try {
                        Method getId = entity.getClass().getMethod("getId");
                        return getId.invoke(entity);
                    } catch (Exception e) {
                        throw new RuntimeException("Error to get entity ID", e);
                    }
                })
                .collect(Collectors.toList());
        return ids;
    }

    public Object processUpdateOperation(Object[] args, QueryData dataForQuery) throws SystemException,
            NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        List<Object> results = null;
        Object entity = null;
        Object arg = args[0] instanceof Stream ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) : args[0];
        startTransactionComponents();

        if (dataForQuery.getEntityParamType().isArray()) { //update multiple entities from array reference
            int length = Array.getLength(args[0]);
            results = new ArrayList<>(length);
            startTransactionAndJoin();
            for (int i = 0; i < length; i++) {
                em.merge(Array.get(args[0], i));
                results.add(Array.get(args[0], i));
            }
            endTransaction();

            if (!results.isEmpty()) {
                return processReturnType(dataForQuery, results);
            }
        } else if (arg instanceof List toIterate) { //update multiple entities
            results = new ArrayList<>();
            startTransactionAndJoin();
            for (Object e : ((Iterable<?>) toIterate)) {
                entity = em.merge(e);
                results.add(entity);
            }
            endTransaction();

            if (!results.isEmpty()) {
                return processReturnType(dataForQuery, results);
            }
        } else if (arg != null) { //update single entity
            startTransactionAndJoin();
            entity = em.merge(args[0]);
            endTransaction();
        }

        if (evaluateReturnTypeVoidPredicate.test(dataForQuery.getMethod().getReturnType())) {
            entity = null;
        }

        return entity;
    }

    public Object processQueryOperation(Object[] args, QueryData dataForQuery) throws HeuristicRollbackException, SystemException, HeuristicMixedException, RollbackException, NotSupportedException {
        Limit limit = null;
        List<Sort<?>> sortList = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Limit) {
                    limit = (Limit) arg;
                } else if (arg instanceof Sort) {
                    sortList.add((Sort<?>) arg);
                } else if (arg instanceof Sort[]) {
                    Collections.addAll(sortList, (Sort<?>[]) arg);
                }
            }
        }
        return QueryOperationUtility.processQueryOperation(args, dataForQuery,
                getEntityManager(this.applicationName), getTransactionManager(), limit, sortList);
    }

    public TransactionManager getTransactionManager() {
        ServiceLocator locator = Globals.get(ServiceLocator.class);
        ServiceHandle<TransactionManager> inhabitant =
                locator.getServiceHandle(TransactionManager.class);
        if (inhabitant != null && inhabitant.isActive()) {
            TransactionManager txmgr = inhabitant.getService();
            return txmgr;
        }
        return null;
    }

    public void startTransactionComponents() {
        transactionManager = getTransactionManager();
        em = getEntityManager(this.applicationName);
    }

    public void startTransactionAndJoin() throws SystemException, NotSupportedException {
        transactionManager.begin();
        em.joinTransaction();
    }

    public void endTransaction() throws HeuristicRollbackException, SystemException, HeuristicMixedException, RollbackException {
        em.flush();
        transactionManager.commit();
    }

}
