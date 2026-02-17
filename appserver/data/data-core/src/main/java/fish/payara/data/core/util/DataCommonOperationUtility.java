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

import fish.payara.data.core.cdi.extension.EntityMetadata;
import fish.payara.data.core.cdi.extension.QueryData;
import fish.payara.data.core.cdi.extension.QueryMetadata;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.repository.By;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;

import static fish.payara.data.core.cdi.extension.DynamicInterfaceDataProducer.isEntityCandidate;

/**
 * Class for common utility methods
 */
public class DataCommonOperationUtility {

    private static final String PERSISTENCE_UNIT_ENABLED_PROPERTY = "fish.payara.data.usePU";

    public static Predicate<Class<?>> evaluateReturnTypeVoidPredicate = returnType -> void.class.equals(returnType)
            || Void.class.equals(returnType);

    public static final Predicate<Method> paginationPredicate = m -> Page.class.equals(m.getReturnType()) ||
            CursoredPage.class.equals(m.getReturnType());

    public static Object processReturnType(QueryMetadata dataForQuery, List<Object> results) {
        Class<?> returnType = dataForQuery.getMethod().getReturnType();

        if (evaluateReturnTypeVoidPredicate.test(returnType)) {
            return null;
        }

        if (List.class.isAssignableFrom(returnType)) {
            return results;
        }

        if (Stream.class.isAssignableFrom(returnType)) {
            return results.stream();
        }

        if (Optional.class.isAssignableFrom(returnType)) {
            if (results.size() > 1) {
                throw new NonUniqueResultException("There are more than one result for the query");
            }
            return results.isEmpty() ? Optional.empty() : Optional.ofNullable(results.get(0));
        }

        if (returnType.isArray()) {
            if (dataForQuery.getDeclaredEntityClass() != null &&
                    returnType.getComponentType().isAssignableFrom(dataForQuery.getDeclaredEntityClass())) {
                Object[] returnValue = (Object[]) Array.newInstance(dataForQuery.getDeclaredEntityClass(), results.size());
                return results.toArray(returnValue);
            } else {
                Object[] returnValue = (Object[]) Array.newInstance(returnType.getComponentType(), results.size());
                return results.toArray(returnValue);
            }
        }

        if (results.size() > 1) {
            if (returnType.equals(dataForQuery.getDeclaredEntityClass())) {
                throw new NonUniqueResultException("There are more than one result for the query");
            }
            return results;
        }

        if (results.isEmpty()) {
            if (returnType.equals(dataForQuery.getDeclaredEntityClass())) {
                throw new EmptyResultException("There are no results for the query");
            }
            return null;
        }

        return results.get(0);
    }

    public static EntityManager getEntityManager(String applicationName) {
        ApplicationRegistry applicationRegistry = getRegistry();
        ApplicationInfo applicationInfo = applicationRegistry.get(applicationName);
        List<EntityManagerFactory> factoryList = applicationInfo.getTransientAppMetaData(EntityManagerFactory.class.toString(), List.class);
        if (factoryList.size() == 1) {
            EntityManagerFactory factory = factoryList.getFirst();
            return factory.createEntityManager();
        }

        List<EntityManagerFactory> result = factoryList.stream().filter(factory -> {
            Map<String, Object> properties = factory.getProperties();
            if (properties == null || properties.isEmpty()) {
                return false;
            }
            if (!properties.containsKey(PERSISTENCE_UNIT_ENABLED_PROPERTY)) {
                return false;
            }
            return Boolean.parseBoolean((String) properties.get(PERSISTENCE_UNIT_ENABLED_PROPERTY));
        }).toList();

        if (result.size() == 1) {
            EntityManagerFactory factory = result.getFirst();
            return factory.createEntityManager();
        }

        throw new AmbiguousPersistenceUnitException(String.format("For the application '%s', specify a single persistence unit for Jakarta Data by setting the property '%s' to 'true' in its persistence.xml.", applicationName, PERSISTENCE_UNIT_ENABLED_PROPERTY));
    }

    public static ApplicationRegistry getRegistry() {
        ApplicationRegistry registry = Globals.get(ApplicationRegistry.class);
        return registry;
    }

    public static EntityMetadata preprocesEntityMetadata(Class<?> repository, Map<Class<?>, EntityMetadata> mapOfMetaData, Class<?> declaredEntityClass,
                                                         Method method, String applicationName) {
        if (declaredEntityClass == null) {
            declaredEntityClass = findEntityTypeInMethod(method);
        }

        if (mapOfMetaData != null && mapOfMetaData.containsKey(declaredEntityClass)) {
            return mapOfMetaData.get(declaredEntityClass);
        }
        EntityManager entityManager = getEntityManager(applicationName);
        Metamodel metamodel = entityManager.getMetamodel();
        try {
            for (EntityType<?> entityType : metamodel.getEntities()) {
                Map<String, String> attributeNames = new HashMap<>();
                Map<String, Member> attributeAccessors = new HashMap<>();
                Map<String, Class<?>> attributeTypes = new HashMap<>();
                Class<?> idType = null;

                Class<?> entityClassType = entityType.getJavaType();
                if (!entityClassType.equals(declaredEntityClass)) {
                    continue;
                }

                for (Attribute<?, ?> attribute : entityType.getAttributes()) {
                    String attributeName = attribute.getName();
                    Attribute.PersistentAttributeType persistentAttributeType = attribute.getPersistentAttributeType();
                    switch (persistentAttributeType) {
                        case BASIC, ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY, ELEMENT_COLLECTION, MANY_TO_ONE,
                             EMBEDDED -> {
                        }
                        default -> {
                            throw new IllegalArgumentException("Unsupported attribute type: " + persistentAttributeType);
                        }
                    }

                    Member accessor = attribute.getJavaMember();
                    attributeNames.put(attributeName.toLowerCase(), attributeName);
                    attributeAccessors.put(attributeName, accessor);
                    attributeTypes.put(attributeName, attribute.getJavaType());

                    SingularAttribute<?, ?> singularAttribute = attribute instanceof SingularAttribute ? (SingularAttribute<?, ?>) attribute : null;

                    if (singularAttribute != null && singularAttribute.isId()) {
                        attributeNames.put(By.ID, attributeName);
                        idType = singularAttribute.getJavaType();
                    }

                }

                EntityMetadata entityMetadata = new EntityMetadata(entityClassType.getName(), entityClassType, attributeNames, attributeTypes, attributeAccessors, idType);

                mapOfMetaData.computeIfAbsent(entityClassType, key -> entityMetadata);

                return entityMetadata;
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }


        return null;
    }

    public static Class<?> findEntityTypeInMethod(Method method) {
        Class<?> returnType = method.getReturnType();
        if (!void.class.equals(returnType) && !Void.class.equals(returnType)) {
            if (Iterable.class.isAssignableFrom(returnType)
                    || Stream.class.isAssignableFrom(returnType)
                    || Optional.class.isAssignableFrom(returnType) || Page.class.isAssignableFrom(returnType)) {
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) genericReturnType;
                    Type typeArgument = paramType.getActualTypeArguments()[0];
                    Class<?> cl = getGenericClass(typeArgument);
                    return evaluateReturnEntity(cl);
                }
            } else if (returnType.isArray()) {
                Class<?> cl = returnType.getComponentType();
                return evaluateReturnEntity(cl);
            } else if (!returnType.isPrimitive() && !returnType.equals(String.class)) {
                return evaluateReturnEntity(returnType);
            }
        }
        for (Parameter param : method.getParameters()) {
            Class<?> paramType = param.getType();
            if (!paramType.isPrimitive() && !paramType.equals(String.class)) {
                if (Iterable.class.isAssignableFrom(paramType)
                        || Stream.class.isAssignableFrom(paramType)) {
                    Type paramGenericType = param.getParameterizedType();
                    if (paramGenericType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) paramGenericType;
                        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
                        Class<?> cl = getGenericClass(typeArgument);
                        return evaluateReturnEntity(cl);
                    }
                } else if (paramType.isArray()) {
                    Class<?> cl = paramType.getComponentType();
                    return evaluateReturnEntity(cl);
                } else {
                    return evaluateReturnEntity(paramType);
                }
            }
        }
        return null;
    }
    
    public static Class<?> evaluateReturnEntity(Class<?> cl) {
        if (isEntityCandidate(cl)) {
            return cl;
        } else {
            return null;
        }
    }

    public static Class<?> getGenericClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return (Class<?>) paramType.getRawType();
        }
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0) {
                return getGenericClass(upperBounds[0]);
            }
        }
        return Object.class;
    }

    public static Object processReturnQueryUpdate(Method method, int returnValue) {
        if (method.getReturnType().equals(Integer.TYPE)) {
            return Integer.valueOf(returnValue);
        } else if (method.getReturnType().equals(Void.TYPE)) {
            return null;
        } else if (method.getReturnType().equals(Boolean.TYPE)) {
            return returnValue != 0;
        } else {
            return Long.valueOf(returnValue);
        }
    }

    public static Object[] getCursorValues(Object entity, List<Sort<?>> sorts, QueryMetadata queryMetadata) {
        ArrayList<Object> cursorValues = new ArrayList<>();
        for (Sort<?> sort : sorts)
            try {
                Member member = queryMetadata.getEntityMetadata().getAttributeAccessors().get(sort.property());
                Object value = entity;

                if (member instanceof Method) {
                    value = ((Method) member).invoke(value);
                } else {
                    value = ((Field) member).get(value);
                }

                cursorValues.add(value);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new DataException(x instanceof InvocationTargetException ? x.getCause() : x);
            }
        return cursorValues.toArray();
    }

    public static String handleSort(QueryData dataForQuery, List<Sort<?>> sortList, String query,
                                    boolean isFindOperation, boolean hasPagination, boolean isForward) {
        StringBuilder sortedQuery = new StringBuilder(query);
        if (!sortList.isEmpty()) {
            appendSortQuery(dataForQuery, sortList, sortedQuery, isFindOperation, hasPagination, isForward, null);
        }
        return sortedQuery.toString();
    }

    public static void handleSort(QueryData dataForQuery, List<Sort<?>> sortList, StringBuilder query,
                                  boolean isFindOperation, boolean hasPagination, boolean isForward, String rootAlias) {
        if (!sortList.isEmpty()) {
            appendSortQuery(dataForQuery, sortList, query, isFindOperation, hasPagination, isForward, rootAlias);
        }
    }

    private static void appendSortQuery(QueryData dataForQuery,
                                        List<Sort<?>> sortList, StringBuilder sortedQuery, 
                                        boolean isFindOperation, boolean hasPagination, 
                                        boolean isForward, String rootAlias) {
        String upper = sortedQuery.toString().toUpperCase();
        if (upper.contains(" ORDER BY ")) {
            throw new IllegalArgumentException("The query cannot contain multiple ORDER BY keywords : '" + sortedQuery + "'");
        }
        EntityMetadata entityMetadata = dataForQuery.getQueryMetadata().getEntityMetadata();
        StringBuilder sortCriteria = new StringBuilder(" ORDER BY ");
        boolean firstItem = true;
        for (Sort<?> sort : sortList) {
            String propertyName = sort.property();
            if (!entityMetadata.getAttributeNames().containsKey(propertyName.toLowerCase())) {
                throw new IllegalArgumentException("The attribute " + propertyName +
                        " is not mapped on the entity " + entityMetadata.getEntityName());
            }
            propertyName = entityMetadata.getAttributeNames().get(propertyName.toLowerCase());

            if (!firstItem) {
                sortCriteria.append(", ");
            }

            boolean isFunctionExpr = propertyName.charAt(propertyName.length() - 1) == ')';

            if (!isFunctionExpr) {
                if (isFindOperation) {
                    sortCriteria.append("o.");
                } else if (rootAlias != null && !rootAlias.isEmpty()) {
                    sortCriteria.append(rootAlias).append(".");
                }
            }

            if (sort.ignoreCase()) {
                sortCriteria.append("LOWER(");
            }
            sortCriteria.append(propertyName);
            if (sort.ignoreCase()) {
                sortCriteria.append(")");
            }

            if (!hasPagination) {
                sortCriteria.append(sort.isAscending() ? " ASC" : " DESC");
            } else {
                boolean ascDirection = isForward ? sort.isAscending() : sort.isDescending();
                sortCriteria.append(ascDirection ? " ASC" : " DESC");
            }

            firstItem = false;
        }
        sortedQuery.append(sortCriteria);
        dataForQuery.setQueryOrder(sortCriteria.toString());
    }

    public static DataParameter extractDataParameter(Object[] args) {
        Limit limit = null;
        List<Sort<?>> sortList = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Limit l) {
                    limit = l;
                } else if (arg instanceof Sort<?> sort) {
                    sortList.add(sort);
                } else if (arg instanceof Order<?> order) {
                    order.forEach(sortList::add);
                } else if (arg instanceof Sort<?>[] sorts) {
                    Collections.addAll(sortList, sorts);
                }
            }
        }
        return new DataParameter(limit, sortList);
    }

    public static void startTransactionAndJoin(TransactionManager transactionManager,
                                               EntityManager em, QueryData dataForQuery) throws SystemException, NotSupportedException {
        
        if (dataForQuery.isUserTransaction()) {
            return;
        }
        
        if (transactionManager.getStatus() == jakarta.transaction.Status.STATUS_NO_TRANSACTION) {
            transactionManager.begin();
            dataForQuery.setNewTransaction(true);
        } 
        
        em.joinTransaction();
    }

    public static void endTransaction(TransactionManager transactionManager,
                                      EntityManager em, QueryData dataForQuery) throws HeuristicRollbackException, SystemException, HeuristicMixedException, RollbackException {
        if(!dataForQuery.isUserTransaction()) {
            em.flush();
            if (dataForQuery.isNewTransaction()) {
                transactionManager.commit();
                dataForQuery.setNewTransaction(false);
            }
        }
    }
}
