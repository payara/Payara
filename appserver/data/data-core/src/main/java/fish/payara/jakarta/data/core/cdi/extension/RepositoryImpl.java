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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;


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
            case INSERT -> objectToReturn = processInsertOperation(args);
            case DELETE -> processDeleteOperation(args);
            case UPDATE -> objectToReturn = processUpdateOperation(args);
            case FIND -> objectToReturn = processFindAllOperation(args, dataForQuery.getDeclaredEntityClass());
        }

        return objectToReturn;
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

    public Object processInsertOperation(Object[] args) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        List<Object> results = null;
        Object entity = null;
        startTransactionComponents();
        //insert multiple entities
        if (args[0] instanceof List arr) {
            results = new ArrayList<>();
            startTransactionAndJoin();
            for (Object e : ((Iterable<?>) arr)) {
                em.persist(e);
                results.add(e);
            }
            endTransaction();

            if (!results.isEmpty()) {
                return results;
            }
        } else if (args[0] != null) { //insert single entity
            startTransactionAndJoin();
            entity = args[0];
            em.persist(args[0]);
            endTransaction();
        }

        return entity;
    }

    public void processDeleteOperation(Object[] args) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
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

    public Stream<?> processFindAllOperation(Object[] args, Class<?> entityClass) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ").append("o").append(" FROM ").append(getSingleEntityName(entityClass.getName())).append(" o");
        EntityManager em = getEntityManager();
        Query q = em.createQuery(builder.toString());
        return q.getResultStream();
    }

    public String getSingleEntityName(String entityName) {
        if (entityName != null) {
            int idx = entityName.lastIndexOf(".");
            return entityName.substring(idx + 1);
        }
        return null;
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
