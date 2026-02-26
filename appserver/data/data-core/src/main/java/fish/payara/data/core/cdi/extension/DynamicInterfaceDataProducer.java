/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.data.core.cdi.extension;

import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.ProducerFactory;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static fish.payara.data.core.util.DataCommonOperationUtility.findEntityTypeInMethod;
import static fish.payara.data.core.util.DataCommonOperationUtility.preprocesEntityMetadata;

/**
 * This is a generic class that works as a producer for a Bean that is going to be used during
 * injection point resolution
 *
 * @param <T>
 */
public class DynamicInterfaceDataProducer<T> implements Producer<T>, ProducerFactory<T>, BeanAttributes<T>, PassivationCapable {

    private static final Logger logger = Logger.getLogger(DynamicInterfaceDataProducer.class.getName());

    private Class<T> repository;
    private BeanManager beanManager;
    private JakartaDataExtension jakartaDataExtension;
    private Set<Type> beanTypes = null;
    private Map<Class<?>, List<QueryMetadata>> queriesForEntity = new HashMap<>();
    private Map<Class<?>, EntityMetadata> mapOfMetaData = new HashMap<>();
    private Predicate<Method> methodAnnotationValidationPredicate = method -> method.getParameterCount() == 1 &&
            !method.isDefault() && (method.isAnnotationPresent(Insert.class) || method.isAnnotationPresent(Update.class)
            || method.isAnnotationPresent(Save.class) || method.isAnnotationPresent(Delete.class));
    private Predicate<Class<?>> classTypeValidationPredicate = clazz -> Iterable.class.isAssignableFrom(clazz)
            || Stream.class.isAssignableFrom(clazz);

    private Predicate<Class<?>> classValidationParameter = clazz -> clazz != null
            && !clazz.isPrimitive() && !clazz.isInterface();

    DynamicInterfaceDataProducer(Class<?> instance, BeanManager beanManager, JakartaDataExtension jakartaDataExtension) {
        this.repository = (Class<T>) instance;
        this.beanManager = beanManager;
        this.jakartaDataExtension = jakartaDataExtension;
        this.beanTypes = Set.of(instance);
        processQueriesForEntity();
    }

    @Override
    public T produce(CreationalContext<T> creationalContext) {
        RepositoryImpl<?> handler = new RepositoryImpl<>(repository, queriesForEntity, jakartaDataExtension.getApplicationName());
        return (T) Proxy.newProxyInstance(repository.getClassLoader(), new Class[]{repository},
                handler);
    }

    @Override
    public void dispose(T t) {

    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return beanTypes;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Set.of(Default.Literal.INSTANCE);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return this.repository.getName();
    }

    private void processQueriesForEntity() {
        logger.info("Processing query for entity class: " + repository);
        //get entity type
        Class<?> declaredEntityClass = getEntityType(this.repository);
        logger.info("Processing entity class " + (declaredEntityClass != null ? declaredEntityClass.getName() : "null"));
        for (Method method : this.repository.getMethods()) {
            logger.info("Processing query for " + (declaredEntityClass != null ? declaredEntityClass.getName() : "null") + "." + method.getName());
            //skip if method is default 
            if (method.isDefault()) {
                continue;
            }
            Class<?> entityParamType = null;
            entityParamType = getEntityParamClass(method);
            addQueries(repository, declaredEntityClass, entityParamType, method);
        }
    }

    /**
     * This method review all the interfaces implemented by this class and get the entity type mapped
     *
     * @param repositoryInterface This is the interface class from the application
     * @return the entity class used of the operation
     */
    private Class<?> getEntityType(Class<?> repositoryInterface) {
        Class<?> entityType = getEntityTypeFromGenerics(repositoryInterface);
        if (entityType != null) {
            return entityType;
        }
        return inferEntityTypeFromMethods(repositoryInterface);
    }

    private Class<?> getEntityTypeFromGenerics(Class<?> repositoryInterface) {
        for (Type genericInterface : repositoryInterface.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pType) {
                if (pType.getRawType() instanceof Class<?> rawType && DataRepository.class.isAssignableFrom(rawType)) {
                    Type typeArgument = pType.getActualTypeArguments()[0];
                    if (typeArgument instanceof Class) {
                        return (Class<?>) typeArgument;
                    }
                }
            }
        }
        for (Class<?> superInterface : repositoryInterface.getInterfaces()) {
            Class<?> entityType = getEntityTypeFromGenerics(superInterface);
            if (entityType != null) {
                return entityType;
            }
        }
        return null;
    }

    private Class<?> inferEntityTypeFromMethods(Class<?> repositoryInterface) {
        logger.info("Inferring entity type from methods");
        for (Method method : repositoryInterface.getMethods()) {
            if (isRepositoryMethodCandidate(method)) {
                Class<?> entityType = findEntityTypeInMethodSignature(method);
                if (entityType != null) {
                    logger.info("Found entity type from method " + method.getName() + ": " + entityType.getName());
                    return entityType;
                }
            }
        }
        return null;
    }

    private boolean isRepositoryMethodCandidate(Method method) {
        if (method.isAnnotationPresent(Insert.class) ||
                method.isAnnotationPresent(Save.class) ||
                method.isAnnotationPresent(Update.class) ||
                method.isAnnotationPresent(Delete.class) ||
                method.isAnnotationPresent(Find.class)) {
            return true;
        }
        String methodName = method.getName();
        return methodName.startsWith("find") ||
                methodName.startsWith("delete") ||
                methodName.startsWith("count") ||
                methodName.startsWith("exists");
    }

    private Class<?> findEntityTypeInMethodSignature(Method method) {
        Class<?> returnType = method.getReturnType();
        if (isEntityCandidate(returnType)) {
            return returnType;
        }

        if (returnType.isArray()) {
            Class<?> componentType = returnType.getComponentType();
            if (isEntityCandidate(componentType)) {
                return componentType;
            }
        }

        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericReturnType;
            Type[] args = paramType.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class) {
                Class<?> argClass = (Class<?>) args[0];
                if (isEntityCandidate(argClass)) {
                    return argClass;
                }
            }
        }
        Class<?> entityClass = findEntityTypeInMethod(method);
        if (isEntityCandidate(entityClass)) {
            return entityClass;
        }
        return null;
    }

    public static boolean isEntityCandidate(Class<?> clazz) {
        if (clazz == null || clazz.isPrimitive() || clazz.equals(String.class) ||
                clazz.equals(Object.class) || clazz.equals(Void.class) || clazz.equals(void.class) ||
                clazz.equals(BigDecimal.class) || clazz.equals(BigInteger.class)) {
            return false;
        }
        return clazz.isAnnotationPresent(Entity.class) || clazz.isAnnotationPresent(Table.class);
    }

    public Class<?> getEntityParamClass(Method method) {
        Class<?> entityParamType = null;
        Type type;
        // Determine entity class from parameters
        if (methodAnnotationValidationPredicate.test(method)) {
            Class<?> classParameterType = method.getParameterTypes()[0];
            if (classTypeValidationPredicate.test(classParameterType)) {
                type = method.getGenericParameterTypes()[0];
                if (type instanceof ParameterizedType) {
                    Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
                    if (typeParams.length == 1 && typeParams[0] instanceof Class) {
                        classParameterType = (Class<?>) typeParams[0];
                    } else {
                        entityParamType = classParameterType;
                        classParameterType = null;
                    }
                } else {
                    classParameterType = null;
                }
            } else if (classParameterType.isArray()) {
                classParameterType = classParameterType.getComponentType();
            }

            if (Object.class.equals(classParameterType)) {
                entityParamType = classParameterType;
            } else if (classValidationParameter.test(classParameterType)) {
                Parameter param = method.getParameters()[0];
                entityParamType = param.getType();
            }
        }
        return entityParamType;
    }

    /**
     * Here is added the queries to be resolved for specific entity type
     *
     * @param entityClass
     * @param entityParamType
     * @param method
     */
    public void addQueries(Class<?> entityClass, Class<?> declaredEntityClass, Class<?> entityParamType, Method method) {
        List<QueryMetadata> queries;
        queries = queriesForEntity.computeIfAbsent(entityClass, k -> new ArrayList<>());
        QueryType queryType = null;
        if (method.isAnnotationPresent(Save.class)) {
            queryType = QueryType.SAVE;
        } else if (method.isAnnotationPresent(Delete.class)) {
            queryType = QueryType.DELETE;
        } else if (method.isAnnotationPresent(Update.class)) {
            queryType = QueryType.UPDATE;
        } else if (method.isAnnotationPresent(Insert.class)) {
            queryType = QueryType.INSERT;
        } else if (method.isAnnotationPresent(Find.class)) {
            queryType = QueryType.FIND;
        } else if (method.isAnnotationPresent(Query.class)) {
            queryType = QueryType.QUERY;
        } else {
            String methodName = method.getName();
            if (methodName.startsWith("delete")) {
                queryType = QueryType.DELETE_BY_NAME;
            } else if (methodName.startsWith("count")) {
                queryType = QueryType.COUNT_BY_NAME;
            } else if (methodName.startsWith("exists")) {
                queryType = QueryType.EXISTS_BY_NAME;
            } else {
                queryType = QueryType.FIND_BY_NAME;
            }
        }

        try {
            Class<?> entityTypeInMethod = findEntityTypeInMethod(method);
            if (entityTypeInMethod != null) {
                declaredEntityClass = entityTypeInMethod;
            }
            else if (declaredEntityClass == null) {
                throw new MappingException(
                        String.format("Could not determine primary entity type for repository method '%s' in %s. " +
                                        "Either extend a repository interface with entity type parameters or " +
                                        "ensure entity type is determinable from method signature.",
                                method.getName(), method.getDeclaringClass().getName())
                );
            }

            queries.add(new QueryMetadata(
                    repository, method, declaredEntityClass, entityParamType,
                    queryType, preprocesEntityMetadata(repository, mapOfMetaData, declaredEntityClass, method,
                    this.jakartaDataExtension.getApplicationName())));
        }
        catch (MappingException e) {
            logger.warning(e.getMessage());
        }
    }

    @Override
    public <T> Producer<T> createProducer(Bean<T> bean) {
        return (Producer<T>) this;
    }
}