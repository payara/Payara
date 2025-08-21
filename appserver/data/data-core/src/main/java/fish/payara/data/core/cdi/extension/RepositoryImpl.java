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
package fish.payara.data.core.cdi.extension;

import fish.payara.data.core.util.DataParameter;
import fish.payara.data.core.util.EntityIntrospectionUtil;
import fish.payara.data.core.util.FindOperationUtility;
import fish.payara.data.core.util.QueryByNameOperationUtility;
import fish.payara.data.core.util.QueryOperationUtility;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.repository.OrderBy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fish.payara.data.core.util.DataCommonOperationUtility.endTransaction;
import static fish.payara.data.core.util.DataCommonOperationUtility.evaluateReturnTypeVoidPredicate;
import static fish.payara.data.core.util.DataCommonOperationUtility.extractDataParameter;
import static fish.payara.data.core.util.DataCommonOperationUtility.getEntityManager;
import static fish.payara.data.core.util.DataCommonOperationUtility.paginationPredicate;
import static fish.payara.data.core.util.DataCommonOperationUtility.processReturnQueryUpdate;
import static fish.payara.data.core.util.DataCommonOperationUtility.processReturnType;
import static fish.payara.data.core.util.DataCommonOperationUtility.startTransactionAndJoin;
import static fish.payara.data.core.util.InsertAndSaveOperationUtility.processInsertAndSaveOperationForArray;

/**
 * This is a generic class that represent the proxy to be used during runtime
 *
 * @param <T>
 */
public class RepositoryImpl<T> implements InvocationHandler {

    public static final Logger logger = Logger.getLogger(RepositoryImpl.class.getName());

    private final Class<T> repositoryInterface;
    private final Map<Method, QueryData> queries = new HashMap<>();
    private final String applicationName;
    private TransactionManager transactionManager;
    private EntityManager em;
    private final Map<Class<?>, Member> idAccessorCache = new ConcurrentHashMap<>();

    public RepositoryImpl(Class<T> repositoryInterface, Map<Class<?>, List<QueryData>> queriesPerEntityClass, String applicationName) {
        this.repositoryInterface = repositoryInterface;
        this.applicationName = applicationName;

        Map<Method, QueryData> r = queriesPerEntityClass.entrySet().stream().map(e -> e.getValue())
                .flatMap(List::stream).collect(Collectors.toMap(QueryData::getMethod, Function.identity()));
        queries.putAll(r);
    }

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public static void validateMethodArguments(Method method, Object[] args) {
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < paramAnnotations.length; i++) {
            boolean hasValidation = false;
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation.annotationType().equals(Valid.class)) {
                    hasValidation = true;
                    break;
                }
            }
            if (hasValidation && args != null && args.length > i && args[i] != null) {
                Object arg = args[i];
                if (arg instanceof Iterable<?> it) {
                    List<ConstraintViolation<Object>> allViolations = new ArrayList<>();
                    for (Object e : it) {
                        Set<ConstraintViolation<Object>> violations = validator.validate(e);
                        allViolations.addAll(violations);
                    }
                    if (!allViolations.isEmpty()) {
                        throw new ConstraintViolationException(new HashSet<>(allViolations));
                    }
                } else {
                    Set<ConstraintViolation<Object>> violations = validator.validate(arg);
                    if (!violations.isEmpty()) {
                        throw new ConstraintViolationException(violations);
                    }
                }
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //In this method we can add implementation to execute dynamic queries
        logger.info("executing method:" + method.getName());
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        
        QueryData dataForQuery = queries.get(method);
        Object objectToReturn;
        startTransactionComponents();
        prevalidateTransaction(dataForQuery);
        try {
            switch (dataForQuery.getQueryType()) {
                case SAVE -> objectToReturn = processSaveOperation(args, dataForQuery);
                case INSERT -> objectToReturn = processInsertOperation(args, dataForQuery);
                case DELETE ->
                        objectToReturn = processDeleteOperation(args, dataForQuery.getDeclaredEntityClass(), 
                                dataForQuery.getMethod(), dataForQuery);
                case UPDATE -> objectToReturn = processUpdateOperation(args, dataForQuery);
                case FIND -> objectToReturn = processFindOperation(proxy, args, dataForQuery);
                case QUERY -> objectToReturn = processQueryOperation(args, dataForQuery);
                case FIND_BY_NAME -> objectToReturn = QueryByNameOperationUtility.processFindByNameOperation(args, 
                        dataForQuery, getEntityManager(this.applicationName));
                case DELETE_BY_NAME -> objectToReturn = QueryByNameOperationUtility.processDeleteByNameOperation(args, 
                        dataForQuery, getEntityManager(this.applicationName), getTransactionManager());
                case COUNT_BY_NAME -> objectToReturn = QueryByNameOperationUtility.processCountByNameOperation(args, 
                        dataForQuery, getEntityManager(this.applicationName));
                case EXISTS_BY_NAME -> objectToReturn = QueryByNameOperationUtility.processExistsByNameOperation(args, 
                        dataForQuery, getEntityManager(this.applicationName));
                default -> throw new UnsupportedOperationException("QueryType " + dataForQuery.getQueryType() + " not supported.");
            }
        } catch (jakarta.persistence.OptimisticLockException e) {
            // Expected in Data TCK
            throw new jakarta.data.exceptions.OptimisticLockingFailureException(e.getMessage(), e);
        }

        return objectToReturn;
    }

    private void prevalidateTransaction(QueryData dataForQuery) throws SystemException {
        dataForQuery.setUserTransaction(transactionManager.getStatus() == Status.STATUS_ACTIVE);
    }

    public Object processFindOperation(Object proxy, Object[] args, QueryData dataForQuery) {
        Annotation[][] parameterAnnotations = dataForQuery.getMethod().getParameterAnnotations();
        boolean evaluatePages = paginationPredicate.test(dataForQuery.getMethod());
        DataParameter dataParameter = extractDataParameter(args);

        if (parameterAnnotations.length > 0) {
            Object returnObject = FindOperationUtility.processFindByOperation(
                    args, getEntityManager(this.applicationName),
                    dataForQuery, dataParameter, evaluatePages);

            if (returnObject instanceof List<?>) {
                List<Object> resultList = (List<Object>) returnObject;
                return processReturnType(dataForQuery, resultList);
            } else {
                return returnObject;
            }
        } else {
            // For "findAll" operations
            Object result = FindOperationUtility.processFindAllOperation(
                    dataForQuery.getDeclaredEntityClass(),
                    getEntityManager(this.applicationName),
                    extractOrderByClause(dataForQuery.getMethod()),
                    dataForQuery,
                    dataParameter
            );

            if (result instanceof List<?> resultList) {
                Method method = dataForQuery.getMethod();
                validateReturnValue(proxy, method, resultList != null ? resultList : Collections.emptyList());
                if (Stream.class.isAssignableFrom(method.getReturnType())) {
                    return resultList.stream();
                }
                return resultList;
            }

            return result;
        }
    }

    private void validateReturnValue(Object bean, Method method, Object returnValue) {
        Set<ConstraintViolation<Object>> violations = validator.forExecutables()
                .validateReturnValue(bean, method, returnValue);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
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

    public Object processSaveOperation(Object[] args, QueryData dataForQuery) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        validateMethodArguments(dataForQuery.getMethod(), args);
        List<Object> results;
        Object entity = null;
        Object arg = args[0] instanceof Stream ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) : args[0];

        if (dataForQuery.getEntityParamType().isArray()) {
            return processInsertAndSaveOperationForArray(args, getTransactionManager(), getEntityManager(this.applicationName), dataForQuery);
        } else if (arg instanceof Iterable toIterate) {
            results = new ArrayList<>();
            startTransactionAndJoin(transactionManager, em, dataForQuery);
            for (Object e : toIterate) {
                results.add(em.merge(e));
            }
            endTransaction(transactionManager, em, dataForQuery);
            if (!results.isEmpty()) {
                return processReturnType(dataForQuery, results);
            }
        } else if (args[0] != null) {
            startTransactionAndJoin(transactionManager, em, dataForQuery);
            entity = em.merge(args[0]);
            endTransaction(transactionManager, em, dataForQuery);
        }

        if (evaluateReturnTypeVoidPredicate.test(dataForQuery.getMethod().getReturnType())) {
            entity = null;
        }
        return entity;
    }

    private boolean entityExistsConstraintViolation(Throwable t) {
        while (t != null) {
            if (t instanceof jakarta.persistence.EntityExistsException) {
                return true;
            }
            if (t instanceof java.sql.SQLIntegrityConstraintViolationException) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("constraint") && lower.contains("violation")) {
                    return true;
                }
                if (lower.contains("unique") && lower.contains("violation")) {
                    return true;
                }
                if (lower.contains("duplicate") && lower.contains("key")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    public Object processInsertOperation(Object[] args, QueryData dataForQuery) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        validateMethodArguments(dataForQuery.getMethod(), args);
        List<Object> results;
        Object entity = null;
        Object arg = args[0] instanceof Stream ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) : args[0];

        try {
            if (dataForQuery.getEntityParamType().isArray()) { //insert multiple entities from array reference
                return processInsertAndSaveOperationForArray(args, getTransactionManager(), getEntityManager(this.applicationName), dataForQuery);
            } else if (arg instanceof Iterable toIterate) {  //insert multiple entities from list reference
                results = new ArrayList<>();
                startTransactionAndJoin(transactionManager, em, dataForQuery);
                for (Object e : ((Iterable<?>) toIterate)) {
                    em.persist(e);
                    results.add(e);
                }
                endTransaction(transactionManager, em, dataForQuery);

                if (!results.isEmpty()) {
                    return processReturnType(dataForQuery, results);
                }
            } else if (arg != null) { //insert a single entity
                startTransactionAndJoin(transactionManager, em, dataForQuery);
                entity = args[0];
                em.persist(args[0]);
                endTransaction(transactionManager, em, dataForQuery);
            }
        } catch (Throwable t) {
            try {
                if (transactionManager != null) {
                    int status = transactionManager.getStatus();
                    if (status == jakarta.transaction.Status.STATUS_ACTIVE ||
                            status == jakarta.transaction.Status.STATUS_MARKED_ROLLBACK) {
                        transactionManager.rollback();
                    }
                }
            } catch (Exception ex) {}
            if (entityExistsConstraintViolation(t)) {
                throw new jakarta.data.exceptions.EntityExistsException("Entity already exists", t);
            }
            throw t;
        }

        if (evaluateReturnTypeVoidPredicate.test(dataForQuery.getMethod().getReturnType())) {
            entity = null;
        }

        return entity;
    }

    /**
     * Processes standard delete operations, such as delete(entity) or deleteById(id).
     * This version has been completely rewritten to be more robust and TCK-compliant.
     *
     * @param args The arguments passed to the repository method.
     * @param declaredEntityClass The primary entity class for the repository.
     * @param method The repository method that was invoked.
     * @return The number of deleted entities, converted to the method's return type.
     * @throws ... various transaction exceptions
     */
    public Object processDeleteOperation(Object[] args, Class<?> declaredEntityClass,
                                         Method method, QueryData dataForQuery)
            throws SystemException, NotSupportedException {
        boolean isDeleteById = method.getName().startsWith("deleteById");

        if (args == null) {
            startTransactionAndJoin(transactionManager, em, dataForQuery);
            try {
                String deleteAllQuery = "DELETE FROM " + declaredEntityClass.getSimpleName();
                int deletedCount = em.createQuery(deleteAllQuery).executeUpdate();
                endTransaction(transactionManager, em, dataForQuery);
                return processReturnQueryUpdate(method, deletedCount);
            } catch (Exception e) {
                safeRollback();
                Throwable cause = e;
                while (cause != null) {
                    if (cause instanceof OptimisticLockException || cause instanceof jakarta.persistence.OptimisticLockException) {
                        throw new OptimisticLockingFailureException(cause);
                    }
                    cause = cause.getCause();
                }
                throw new RuntimeException("Error during bulk delete operation", e);
            }
        }

        Object arg = args[0];

        if (isDeleteById) {
            List<?> ids;
            if (arg instanceof Stream) {
                ids = ((Stream<?>) arg).toList();
            } else if (arg instanceof List) {
                ids = (List<?>) arg;
            } else if (arg.getClass().isArray()) {
                ids = Arrays.asList((Object[]) arg);
            } else {
                ids = Collections.singletonList(arg);
            }

            if (ids.isEmpty()) {
                return processReturnQueryUpdate(method, 0);
            }

            startTransactionAndJoin(transactionManager, em, dataForQuery);
            try {
                int deletedCount = 0;
                for (Object id : ids) {
                    Object entity = em.find(declaredEntityClass, id);
                    if (entity != null) {
                        em.remove(entity);
                        deletedCount++;
                    }
                }
                endTransaction(transactionManager, em, dataForQuery);
                return processReturnQueryUpdate(method, deletedCount);
            } catch (Exception e) {
                safeRollback();
                throw new RuntimeException("Error during deleteById operation", e);
            }
        } else {
            List<?> entitiesToDelete;
            if (arg instanceof Stream) {
                entitiesToDelete = ((Stream<?>) arg).toList();
            } else if (arg instanceof List) {
                entitiesToDelete = (List<?>) arg;
            } else if (arg.getClass().isArray()) {
                entitiesToDelete = Arrays.asList((Object[]) arg);
            } else {
                entitiesToDelete = Collections.singletonList(arg);
            }

            if (entitiesToDelete.isEmpty()) {
                return processReturnQueryUpdate(method, 0);
            }

            startTransactionAndJoin(transactionManager, em, dataForQuery);
            try {
                for (Object entity : entitiesToDelete) {
                    if (!em.contains(entity)) {
                        Object id = getId(entity);
                        if (id == null) {
                            throw new OptimisticLockingFailureException(
                                    "Attempted to delete a transient entity (ID is null)."
                            );
                        }
                        Object found = em.find(declaredEntityClass, id);
                        if (found == null) {
                            throw new OptimisticLockingFailureException(
                                    "Attempted to delete an entity that does not exist in the database."
                            );
                        }
                        entity = em.merge(entity); // Pode lançar OptimisticLockException se versão não bater
                    }
                    em.remove(entity);
                }

                endTransaction(transactionManager, em, dataForQuery);
                return processReturnQueryUpdate(method, entitiesToDelete.size());

            } catch (OptimisticLockException jpaOlex) {
                safeRollback();
                throw new OptimisticLockingFailureException(jpaOlex);
            } catch (OptimisticLockingFailureException ole) {
                safeRollback();
                throw ole;
            } catch (Exception e) {
                safeRollback();
                Throwable cause = e;
                while (cause != null) {
                    if (cause instanceof OptimisticLockException ||
                            cause instanceof jakarta.persistence.OptimisticLockException) {
                        throw new OptimisticLockingFailureException(cause);
                    }
                    cause = cause.getCause();
                }
                throw new RuntimeException("Error during delete operation", e);
            }
        }
    }

    private void safeRollback() {
        try {
            if (transactionManager != null) {
                int status = transactionManager.getStatus();
                if (status == jakarta.transaction.Status.STATUS_ACTIVE
                        || status == jakarta.transaction.Status.STATUS_MARKED_ROLLBACK) {
                    transactionManager.rollback();
                }
            }
        } catch (Exception ignore) {
            // intentionally ignore rollback exceptions
        }
    }

    /**
     * Helper method to extract the ID from a single entity.
     */
    private Object getId(Object entity) {
        if (entity == null) {
            return null;
        }
        Member idAccessor = EntityIntrospectionUtil.findIdAccessor(entity.getClass());
        try {
            if (idAccessor instanceof Method) {
                return ((Method) idAccessor).invoke(entity);
            } else if (idAccessor instanceof Field) {
                return ((Field) idAccessor).get(entity);
            }
            throw new MappingException("No ID accessor found for entity: " + entity.getClass().getName());
        } catch (Exception e) {
            throw new MappingException("Failed to get ID from entity of type " + entity.getClass().getName(), e);
        }
    }

    public Object processUpdateOperation(Object[] args, QueryData dataForQuery) throws SystemException,
            NotSupportedException {
        validateMethodArguments(dataForQuery.getMethod(), args);
        List<Object> results;
        Object entity = null;
        Object arg = args[0] instanceof Stream ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) : args[0];

        if (dataForQuery.getEntityParamType().isArray()) { //update multiple entities from array reference
            int length = Array.getLength(args[0]);
            results = new ArrayList<>(length);
            startTransactionAndJoin(transactionManager, em, dataForQuery);
            try {
                for (int i = 0; i < length; i++) {
                    Object original = Array.get(args[0], i);
                    Object merged = em.merge(original);
                    results.add(merged);
                }
                endTransaction(transactionManager, em, dataForQuery);
            } catch (OptimisticLockException optimisticLockException) {
                transactionManager.rollback();
                throw new OptimisticLockingFailureException(optimisticLockException);
            } catch (Exception e) {
                if (transactionManager != null && transactionManager.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                    transactionManager.rollback();
                }
                throw new RuntimeException("Error during update operation for multiple entities", e);
            }

            if (!results.isEmpty()) {
                return processReturnType(dataForQuery, results);
            }
        } else if (arg instanceof List toIterate) { //update multiple entities
            results = new ArrayList<>();
            startTransactionAndJoin(transactionManager, em, dataForQuery);
            try {
                for (Object e : ((Iterable<?>) toIterate)) {
                    entity = em.merge(e);
                    results.add(entity);
                }
                endTransaction(transactionManager, em, dataForQuery);
            } catch (OptimisticLockException jpaOlex) {
                transactionManager.rollback();
                throw new OptimisticLockingFailureException(jpaOlex);
            } catch (Exception e) {
                if (transactionManager != null && transactionManager.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                    transactionManager.rollback();
                }
                throw new RuntimeException("Error during update operation for multiple entities", e);
            }

            if (!results.isEmpty()) {
                return processReturnType(dataForQuery, results);
            }
        } else if (arg != null) { //update single entity
            startTransactionAndJoin(transactionManager, em, dataForQuery);
            try {
                entity = em.merge(args[0]);
                endTransaction(transactionManager, em, dataForQuery);
            } catch (OptimisticLockException optimisticLockException) {
                transactionManager.rollback();
                throw new OptimisticLockingFailureException(optimisticLockException);
            } catch (Exception e) {
                if (transactionManager != null && transactionManager.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                    transactionManager.rollback();
                }
                throw new RuntimeException("Error during update operation", e);
            }
        }

        if (evaluateReturnTypeVoidPredicate.test(dataForQuery.getMethod().getReturnType())) {
            entity = null;
        }

        return entity;
    }

    public Object processQueryOperation(Object[] args, QueryData dataForQuery) throws HeuristicRollbackException, SystemException, HeuristicMixedException, RollbackException, NotSupportedException {
        DataParameter dataParameter = extractDataParameter(args);
        return QueryOperationUtility.processQueryOperation(args, dataForQuery,
                getEntityManager(this.applicationName), getTransactionManager(), dataParameter);
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
    
}
