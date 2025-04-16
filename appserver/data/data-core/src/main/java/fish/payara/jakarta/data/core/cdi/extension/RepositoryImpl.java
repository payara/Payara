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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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
        Stream<?> stream = null;
        switch (dataForQuery.getQueryType()) {
            case SAVE -> processSaveOperation(args);
            case INSERT -> processInsertOperation(args);
            case DELETE -> processDeleteOperation(args);
            case UPDATE -> processUpdateOperation(args, dataForQuery.getEntityParamType());
            case FIND -> stream = processFindAllOperation(args, dataForQuery.getDeclaredEntityClass());
        }

        return stream;
    }

    public void preProcessQuery() {
        for (Map.Entry<Class<?>, List<QueryData>> entry : queriesPerEntityClass.entrySet()) {
            for (QueryData queryData : entry.getValue()) {
                queries.put(queryData.getMethod(), queryData);
            }
        }
    }


    public void processSaveOperation(Object[] args) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        TransactionManager transactionManager = getTransactionManager();
        EntityManager em = getEntityManager();
        if (args[0] != null) {
            transactionManager.begin();
            em.joinTransaction();
            Object entity = em.merge(args[0]);
            em.flush();
            transactionManager.commit();
        }
    }

    public void processInsertOperation(Object[] args) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        TransactionManager transactionManager = getTransactionManager();
        EntityManager em = getEntityManager();
        if (args[0] != null) {
            transactionManager.begin();
            em.joinTransaction();
            em.persist(args[0]);
            em.flush();
            transactionManager.commit();
        }
    }

    public void processDeleteOperation(Object[] args) throws SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        TransactionManager transactionManager = getTransactionManager();
        EntityManager em = getEntityManager();
        if (args[0] != null) {
            transactionManager.begin();
            em.joinTransaction();
            em.remove(em.merge(args[0]));
            em.flush();
            transactionManager.commit();
        }
    }

    public void processUpdateOperation(Object[] args, Class<?> entityParam) throws SystemException,
            NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        logger.info("Need to process update operation with query based on jpql");
        TransactionManager transactionManager = getTransactionManager();
        EntityManager em = getEntityManager();
        if (args[0] != null) {
            transactionManager.begin();
            em.joinTransaction();
            Object entity = em.merge(args[0]);
            em.flush();
            transactionManager.commit();
        }
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

}
