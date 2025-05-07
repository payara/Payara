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

import fish.payara.jakarta.data.core.util.FindOperationUtility;
import jakarta.data.repository.By;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;

import static fish.payara.jakarta.data.core.util.DeleteOperationUtility.processDeleteByIdOperation;
import static fish.payara.jakarta.data.core.util.FindOperationUtility.processFindByIdOperation;


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
        Object objectToReturn = null;
        switch (dataForQuery.getQueryType()) {
            case SAVE -> objectToReturn = processSaveOperation(args);
            case INSERT -> objectToReturn = processInsertOperation(args, dataForQuery);
            case DELETE ->
                    processDeleteOperation(args, dataForQuery.getDeclaredEntityClass(), dataForQuery.getMethod());
            case UPDATE -> objectToReturn = processUpdateOperation(args);
            case FIND ->
                    objectToReturn = processFindOperation(args, dataForQuery.getDeclaredEntityClass(), dataForQuery.getMethod());
        }

        return objectToReturn;
    }

    public Object processFindOperation(Object[] args, Class<?> declaredEntityClass, Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Class<?>[] types = method.getParameterTypes();
        if (parameterAnnotations.length == 1 && types.length == 1) {
            Annotation[] annotations = parameterAnnotations[0];
            for (Annotation annotation : annotations) {
                if (annotation instanceof By) {
                    //for now we are processing only By id operation, when custom By operation available we will provide 
                    // the metadata from the entity class to search specific column value for By
                    String byValue = ((By) annotation).value();
                    List<?> resultList = processFindByIdOperation(args, declaredEntityClass, getEntityManager(), byValue);
                    Object objectToReturn = null;
                    if (!resultList.isEmpty() && resultList.size() == 1) {
                        objectToReturn = resultList.get(0);
                    } else if (!resultList.isEmpty() && resultList.size() > 1) {
                        throw new IllegalArgumentException("Multiple results found for the given id");
                    }
                    return Optional.ofNullable(objectToReturn);
                }
            }
            return null;
        } else {
            return FindOperationUtility.processFindAllOperation(declaredEntityClass, getEntityManager());
        }
    }

    public void preProcessQuery() {
        Map<Method, QueryData> r = queriesPerEntityClass.entrySet().stream().map(e -> e.getValue())
                .flatMap(List::stream).collect(Collectors.toMap(QueryData::getMethod, Function.identity()));
        queries.putAll(r);
    }


    public Object processSaveOperation(Object[] args) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        List<Object> results = null;
        Object entity = null;
        startTransactionComponents();
        //save multiple entities
        if (args[0] instanceof List arr) {
            results = new ArrayList<>();
            startTransactionAndJoin();
            for (Object e : ((Iterable<?>) arr)) {
                results.add(em.merge(e));
            }
            endTransaction();
            if (!results.isEmpty()) {
                return results;
            }
        } else if (args[0] != null) { //save single entity
            startTransactionAndJoin();
            entity = em.merge(args[0]);
            endTransaction();
        }
        return entity;
    }

    public Object processInsertOperation(Object[] args, QueryData dataForQuery) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        List<Object> results = null;
        Object entity = null;
        Object arg = args[0] instanceof Stream ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) : args[0];
        startTransactionComponents();
        //insert multiple entities from array reference
        if (dataForQuery.getEntityParamType().isArray()) {
            int length = Array.getLength(args[0]);
            results = new ArrayList<>(length);
            startTransactionAndJoin();
            for (int i = 0; i < length; i++) {
                em.persist(Array.get(args[0], i));
                results.add(Array.get(args[0], i));
            }
            endTransaction();

            if (!results.isEmpty()) {
                Object objectToReturn = processReturnType(dataForQuery, results);
                if (objectToReturn != null) {
                    return objectToReturn;
                }
                return results;
            }

        } else if (arg instanceof List arr) {  //insert multiple entities from list reference
            results = new ArrayList<>();
            startTransactionAndJoin();
            for (Object e : ((Iterable<?>) arr)) {
                em.persist(e);
                results.add(e);
            }
            endTransaction();

            if (!results.isEmpty()) {
                Object objectToReturn = processReturnType(dataForQuery, results);
                if (objectToReturn != null) {
                    return objectToReturn;
                }
                return results;
            }
        } else if (args[0] != null) {
            startTransactionAndJoin();
            entity = args[0];
            em.persist(args[0]);
            endTransaction();
        }

        return entity;
    }

    public Object processReturnType(QueryData dataForQuery, List<Object> results) {
        Class<?> returnType = dataForQuery.getMethod().getReturnType();
        if (returnType.isArray()) {
            if (dataForQuery.getDeclaredEntityClass() != null && returnType.getComponentType().isAssignableFrom(dataForQuery.getDeclaredEntityClass())) {
                Object[] returnValue = (Object[]) Array.newInstance(dataForQuery.getDeclaredEntityClass(), results.size());
                return results.toArray(returnValue);
            } else {
                Object[] returnValue = (Object[]) Array.newInstance(returnType.getComponentType(), results.size());
                return results.toArray(returnValue);
            }
        } else if (Stream.class.equals(returnType)) {
            return results.stream();
        }
        return null;
    }

    public void processDeleteOperation(Object[] args, Class<?> declaredEntityClass, Method method) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Class<?>[] types = method.getParameterTypes();
        if (parameterAnnotations.length == 1 && types.length == 1) {
            Annotation[] annotations = parameterAnnotations[0];
            for (Annotation annotation : annotations) {
                if (annotation instanceof By) {
                    //for now we are processing only By id operation, when custom By operation available we will provide 
                    // the metadata from the entity class to search specific column value for By
                    String byValue = ((By) annotation).value();
                    processDeleteByIdOperation(args, declaredEntityClass, getTransactionManager(), getEntityManager(), byValue);
                }
            }
        } else {
            startTransactionComponents();
            //delete multiple entities
            if (args[0] instanceof List arr) {
                startTransactionAndJoin();
                for (Object e : ((Iterable<?>) arr)) {
                    em.remove(em.merge(e));
                }
                endTransaction();
            } else if (args[0] != null) { //delete single entity
                startTransactionAndJoin();
                em.remove(em.merge(args[0]));
                endTransaction();
            }
        }
    }

    public Object processUpdateOperation(Object[] args) throws SystemException,
            NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        List<Object> results = null;
        Object entity = null;
        startTransactionComponents();
        //update multiple entities
        if (args[0] instanceof List arr) {
            results = new ArrayList<>();
            startTransactionAndJoin();
            for (Object e : ((Iterable<?>) arr)) {
                entity = em.merge(e);
                results.add(entity);
            }
            endTransaction();

            if (!results.isEmpty()) {
                return results;
            }
        } else if (args[0] != null) { //update single entity
            startTransactionAndJoin();
            entity = em.merge(args[0]);
            endTransaction();
        }
        return entity;
    }


    public ApplicationRegistry getRegistry() {
        ApplicationRegistry registry = Globals.get(ApplicationRegistry.class);
        return registry;
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

    public EntityManager getEntityManager() {
        ApplicationRegistry applicationRegistry = getRegistry();
        ApplicationInfo applicationInfo = applicationRegistry.get(this.applicationName);
        List<EntityManagerFactory> factoryList = applicationInfo.getTransientAppMetaData(EntityManagerFactory.class.toString(), List.class);
        if (factoryList.size() == 1) {
            EntityManagerFactory factory = factoryList.get(0);
            return factory.createEntityManager();
        }
        return null;
    }

    public void startTransactionComponents() {
        transactionManager = getTransactionManager();
        em = getEntityManager();
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
