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

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This is a generic class that works as a producer for a Bean that is going to be used during
 * injection point resolution
 *
 * @param <T>
 */
public class DynamicInterfaceDataProducer<T> implements Bean<T>, PassivationCapable {

    private static final Logger logger = Logger.getLogger(DynamicInterfaceDataProducer.class.getName());

    private Class<T> repository;
    private BeanManager beanManager;
    private JakartaDataExtension jakartaDataExtension;
    private Set<Type> beanTypes = null;
    private Map<Class<?>, List<QueryData>> queriesForEntity = new HashMap<>();

    DynamicInterfaceDataProducer(Class<?> instance, BeanManager beanManager, JakartaDataExtension jakartaDataExtension) {
        this.repository = (Class<T>) instance;
        this.beanManager = beanManager;
        this.jakartaDataExtension = jakartaDataExtension;
        this.beanTypes = Set.of(instance);
        processQueriesForEntity();
    }

    @Override
    public Class<T> getBeanClass() {
        return repository;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public T create(CreationalContext<T> context) {
        RepositoryImpl<?> handler = new RepositoryImpl<>(repository, queriesForEntity, jakartaDataExtension.getApplicationName());
        return (T) Proxy.newProxyInstance(repository.getClassLoader(), new Class[]{repository},
                handler);
    }

    @Override
    public void destroy(T t, CreationalContext<T> creationalContext) {
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
        return "";
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
        Set<Class<?>> lifecycleMethodEntityClasses = new HashSet<>();
        for (Method method : this.repository.getMethods()) {
            logger.info("Processing query for " + (declaredEntityClass != null ? declaredEntityClass.getName() : "null") + "." + method.getName());
            //skip if method is default 
            if (method.isDefault()) {
                continue;
            }
            Class<?> entityParamType = null;
            entityParamType = getEntityParamClass(method);
            addQueries(repository, entityParamType, method);
        }
    }

    /**
     * This method review all the interfaces implemented by this class and get the entity type mapped
     *
     * @param repositoryInterface This is the interface class from the application
     * @return the entity class used of the operation
     */
    private Class<?> getEntityType(Class<?> repositoryInterface) {
        Class<?>[] interfaceClasses = repositoryInterface.getInterfaces();
        for (Type type : repositoryInterface.getGenericInterfaces()) {
            if (type instanceof ParameterizedType parameterizedType) {
                for (Class<?> interfaceClass : interfaceClasses) {
                    if (interfaceClass.equals(parameterizedType.getRawType())) {
                        if (DataRepository.class.isAssignableFrom(interfaceClass)) {
                            Type[] typeParams = parameterizedType.getActualTypeArguments();
                            Type firstParamType = typeParams.length > 0 ? typeParams[0] : null;
                            if (firstParamType instanceof Class) {
                                return (Class<?>) firstParamType;
                            }
                        }
                        break;
                    }
                }
            }
        }
        return null;
    }

    public Class<?> getEntityParamClass(Method method) {
        Class<?> entityParamType = null;
        // Determine entity class from parameters
        if (method.getParameterCount() == 1 && !method.isDefault() &&
                (method.isAnnotationPresent(Insert.class) || method.isAnnotationPresent(Update.class)
                        || method.isAnnotationPresent(Save.class) || method.isAnnotationPresent(Delete.class))) {
            Class<?> c = method.getParameterTypes()[0];
            if (Object.class.equals(c)) {
                entityParamType = c;
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
    public void addQueries(Class<?> entityClass, Class<?> entityParamType, Method method) {
        List<QueryData> queries;
        queries = queriesForEntity.get(entityClass);
        if (queries == null) {
            queriesForEntity.put(entityClass, queries = new ArrayList<>());
        }
        QueryData.QueryType queryType = null;
        if (method.isAnnotationPresent(Save.class)) {
            queryType = QueryData.QueryType.SAVE;
        } else if (method.isAnnotationPresent(Delete.class)) {
            queryType = QueryData.QueryType.DELETE;
        } else if (method.isAnnotationPresent(Update.class)) {
            queryType = QueryData.QueryType.UPDATE;
        } else if (method.isAnnotationPresent(Insert.class)) {
            queryType = QueryData.QueryType.INSERT;
        } else if (method.isAnnotationPresent(Find.class)) {
            queryType = QueryData.QueryType.FIND;
        }
        queries.add(new QueryData(repository, method, entityParamType, queryType));
    }
}