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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

/**
 * This is a generic class that works as a producer for a Bean that is going to be used during 
 * injection point resolution
 * @param <T>
 */
public class DynamicInterfaceDataProducer <T> implements Bean<T>, PassivationCapable {

    Class<T> repository;
    BeanManager beanManager;
    JakartaDataExtension jakartaDataExtension;
    private Set<Type> beanTypes = null;

    DynamicInterfaceDataProducer(Class<?> instance, BeanManager beanManager, JakartaDataExtension jakartaDataExtension) {
        this.repository = (Class<T>) instance;
        this.beanManager = beanManager;
        this.jakartaDataExtension = jakartaDataExtension;
        this.beanTypes = Set.of(instance);
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
        Class<T> repositoryInterface = repository;
        RepositoryImpl<?> handler = new RepositoryImpl<>(repositoryInterface);
        return (T) Proxy.newProxyInstance(repositoryInterface.getClassLoader(), new Class[]{repositoryInterface},
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
}