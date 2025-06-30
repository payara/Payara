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
import fish.payara.jakarta.data.core.cdi.extension.QueryData;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;

/**
 * Class for common utility methods
 */
public class DataCommonOperationUtility {

    public static Predicate<Class<?>> evaluateReturnTypeVoidPredicate = returnType -> void.class.equals(returnType)
            || Void.class.equals(returnType);

    public static final Predicate<Method> paginationPredicate = m -> Page.class.equals(m.getReturnType()) ||
            CursoredPage.class.equals(m.getReturnType());

    public static Object processReturnType(QueryData dataForQuery, List<Object> results) {
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
        } else if (evaluateReturnTypeVoidPredicate.test(returnType)) {
            return null;
        } else if (returnType.equals(Optional.class)) {
            if (results.isEmpty()) {
                return Optional.empty();
            } else {
                if (results.size() > 1) {
                    throw new NonUniqueResultException("There are more than one result for the query");
                }
                return Optional.ofNullable(results.get(0));
            }
        } else if (returnType.equals(dataForQuery.getDeclaredEntityClass())) {
            if (results.isEmpty()) {
                throw new EmptyResultException("There are no results for the query");
            }

            if (results.size() > 1) {
                throw new NonUniqueResultException("There are more than one result for the query");
            }

            return results.getFirst();
        } else if (!results.isEmpty()) {
            return results;
        }
        return null;
    }

    public static EntityManager getEntityManager(String applicationName) {
        ApplicationRegistry applicationRegistry = getRegistry();
        ApplicationInfo applicationInfo = applicationRegistry.get(applicationName);
        List<EntityManagerFactory> factoryList = applicationInfo.getTransientAppMetaData(EntityManagerFactory.class.toString(), List.class);
        if (factoryList.size() == 1) {
            EntityManagerFactory factory = factoryList.get(0);
            return factory.createEntityManager();
        }
        return null;
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
            if (Collection.class.isAssignableFrom(returnType)
                    || Stream.class.isAssignableFrom(returnType)
                    || Optional.class.isAssignableFrom(returnType) || Page.class.isAssignableFrom(returnType)) {
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) genericReturnType;
                    Type typeArgument = paramType.getActualTypeArguments()[0];
                    return getGenericClass(typeArgument);
                }
            } else if (returnType.isArray()) {
                return returnType.getComponentType();
            } else if (!returnType.isPrimitive() && !returnType.equals(String.class)) {
                return returnType;
            }
        }
        for (Parameter param : method.getParameters()) {
            Class<?> paramType = param.getType();
            if (!paramType.isPrimitive() && !paramType.equals(String.class)) {
                if (Collection.class.isAssignableFrom(paramType)
                        || Stream.class.isAssignableFrom(paramType)) {
                    Type paramGenericType = param.getParameterizedType();
                    if (paramGenericType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) paramGenericType;
                        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
                        return getGenericClass(typeArgument);
                    }
                } else if (paramType.isArray()) {
                    return paramType.getComponentType();
                } else {
                    return paramType;
                }
            }
        }
        return null;
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
        } else {
            return Long.valueOf(returnValue);
        }
    }

    public static Object[] getCursorValues(Object entity, List<Sort<Object>> sorts, QueryData queryData) {
        ArrayList<Object> cursorValues = new ArrayList<>();
        for (Sort<?> sort : sorts)
            try {
                Member member = queryData.getEntityMetadata().getAttributeAccessors().get(sort.property());
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
}
