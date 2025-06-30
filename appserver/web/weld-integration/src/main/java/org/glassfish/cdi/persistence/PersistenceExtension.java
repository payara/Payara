/*
 * Copyright (c) 2022, 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.cdi.persistence;

import com.sun.enterprise.container.common.impl.ComponentEnvManagerImpl;
import com.sun.enterprise.deployment.EntityManagerReferenceDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitsDescriptor;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;

public class PersistenceExtension implements Extension  {

    public void afterBean(final @Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        var container = Globals.get(InvocationManager.class).getCurrentInvocation().getContainer();

        if (container instanceof ApplicationInfo applicationInfo) {
            var persistenceUnits = applicationInfo.getMetaData(PersistenceUnitsDescriptor.class);

            if (persistenceUnits != null) {
                for (PersistenceUnitDescriptor descriptor : persistenceUnits.getPersistenceUnitDescriptors()) {
                    addBeanForEntityManager(afterBeanDiscovery, descriptor);
                    addBeanForEntityManagerFactory(afterBeanDiscovery, descriptor);

                    addBeanForCriteriaBuilder(afterBeanDiscovery, descriptor);
                    addBeanForPersistenceUnitUtil(afterBeanDiscovery, descriptor);
                    addBeanForCache(afterBeanDiscovery, descriptor);
                    addBeanForSchemaManager(afterBeanDiscovery, descriptor);
                    addBeanForMetamodel(afterBeanDiscovery, descriptor);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addBeanForEntityManager(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitDescriptor descriptor) {
        var bean = addQualifiedBean(afterBeanDiscovery, descriptor, EntityManager.class);

        if (descriptor.getScope() != null) {
            bean.scope((Class<? extends Annotation>) loadClass(descriptor.getScope()));
        }

        bean.createWith(e -> createEntityManager(descriptor.getName()));
    }

    @SuppressWarnings("unchecked")
    private void addBeanForEntityManagerFactory(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitDescriptor descriptor) {
        var bean = addQualifiedBean(afterBeanDiscovery, descriptor, EntityManagerFactory.class);

        if (descriptor.getScope() != null) {
            bean.scope((Class<? extends Annotation>) loadClass(descriptor.getScope()));
        }

        bean.createWith(e -> createEntityManagerFactory(descriptor.getName()));
    }

    private void addBeanForCriteriaBuilder(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitDescriptor descriptor) {
        addQualifiedBean(afterBeanDiscovery, descriptor, CriteriaBuilder.class)
                .scope(Dependent.class)
                .createWith(e -> getEntityManagerFactory(descriptor).getCriteriaBuilder());
    }

    private void addBeanForPersistenceUnitUtil(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitDescriptor descriptor) {
        addQualifiedBean(afterBeanDiscovery, descriptor, PersistenceUnitUtil.class)
                .scope(Dependent.class)
                .createWith(e -> getEntityManagerFactory(descriptor).getPersistenceUnitUtil());
    }

    private void addBeanForCache(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitDescriptor descriptor) {
        addQualifiedBean(afterBeanDiscovery, descriptor, Cache.class)
                .scope(Dependent.class)
                .createWith(e -> getEntityManagerFactory(descriptor).getCache());
    }

    private void addBeanForSchemaManager(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitDescriptor descriptor) {
        addQualifiedBean(afterBeanDiscovery, descriptor, SchemaManager.class)
                .scope(Dependent.class)
                .createWith(e -> getEntityManagerFactory(descriptor).getSchemaManager());
    }

    private void addBeanForMetamodel(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitDescriptor descriptor) {
        addQualifiedBean(afterBeanDiscovery, descriptor, Metamodel.class)
                .scope(Dependent.class)
                .createWith(e -> getEntityManagerFactory(descriptor).getMetamodel());
    }

    private EntityManagerFactory getEntityManagerFactory(PersistenceUnitDescriptor descriptor) {
        return CDI.current().select(EntityManagerFactory.class, descriptor.getQualifiers().stream().map(
                e -> createAnnotationInstance(loadClass(e))).toArray(Annotation[]::new)).get();
    }

    private BeanConfigurator<Object> addQualifiedBean(AfterBeanDiscovery afterBeanDiscovery, PersistenceUnitDescriptor descriptor, Type type) {
        var bean = afterBeanDiscovery.addBean().addType(type);

        for (String qualifierClassName : descriptor.getQualifiers()) {
            bean.addQualifier(createAnnotationInstance(loadClass(qualifierClassName)));
        }

        return bean;
    }

    private EntityManager createEntityManager(String unitName) {
        return Globals.get(ComponentEnvManagerImpl.class).createFactoryForEntityManager(
                new EntityManagerReferenceDescriptor(unitName)).create(null);

    }

    private EntityManagerFactory createEntityManagerFactory(String unitName) {
        return Globals.get(ComponentEnvManagerImpl.class).createFactoryForEntityManagerFactory(unitName).create(null);

    }

    private Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private Annotation createAnnotationInstance(Class<?> type) {
        return (Annotation) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type },
                new AnnotationInvocationHandler(type));
    }

}
