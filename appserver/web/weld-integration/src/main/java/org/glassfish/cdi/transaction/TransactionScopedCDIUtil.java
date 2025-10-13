/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
// Portions Copyright [2017-2022] [Payara Foundation and/or its affiliates]

package org.glassfish.cdi.transaction;

import static java.util.Arrays.asList;
import static java.util.logging.Level.WARNING;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import jakarta.enterprise.util.AnnotationLiteral;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.api.JavaEEContextUtil.Context;

import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * This class contains utility methods used for TransactionScoped related CDI event processing.
 *
 * TODO: Merge these and other CDU util classes
 *
 * @author <a href="mailto:arjav.desai@oracle.com">Arjav Desai</a>
 */
public class TransactionScopedCDIUtil {

    public static final String INITIALIZED_EVENT = "INITIALIZED_EVENT";
    public static final String DESTORYED_EVENT = "DESTORYED_EVENT";

    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.cdi.LogMessages";

    @LoggerInfo(subsystem = "AS-CDI-JTA", description = "CDI-JTA", publish = true)
    public static final String CDI_JTA_LOGGER_SUBSYSTEM_NAME = "javax.enterprise.resource.jta";
    private static final Logger _logger = Logger.getLogger(CDI_JTA_LOGGER_SUBSYSTEM_NAME, SHARED_LOGMESSAGE_RESOURCE);

    public static void log(String message) {
        _logger.log(WARNING, message);
    }

    /* Copied from Security */
    public static <A extends Annotation> Optional<A> getAnnotation(BeanManager beanManager, Class<?> annotatedClass, Class<A> annotationType) {

        if (annotatedClass.isAnnotationPresent(annotationType)) {
            return Optional.of(annotatedClass.getAnnotation(annotationType));
        }

        Queue<Annotation> annotations = new LinkedList<>(asList(annotatedClass.getAnnotations()));

        while (!annotations.isEmpty()) {
            Annotation annotation = annotations.remove();

            if (annotation.annotationType().equals(annotationType)) {
                return Optional.of(annotationType.cast(annotation));
            }

            if (beanManager.isStereotype(annotation.annotationType())) {
                annotations.addAll(
                    beanManager.getStereotypeDefinition(
                        annotation.annotationType()
                    )
                );
            }
        }

        return Optional.empty();
    }

    public static <A extends Annotation> Optional<A> getAnnotation(BeanManager beanManager, Method annotatedMethod, Class<A> annotationType) {

        if (annotatedMethod.isAnnotationPresent(annotationType)) {
            return Optional.of(annotatedMethod.getAnnotation(annotationType));
        }

        Queue<Annotation> annotations = new LinkedList<>(asList(annotatedMethod.getAnnotations()));

        while (!annotations.isEmpty()) {
            Annotation annotation = annotations.remove();

            if (annotation.annotationType().equals(annotationType)) {
                return Optional.of(annotationType.cast(annotation));
            }

            if (beanManager.isStereotype(annotation.annotationType())) {
                annotations.addAll(
                    beanManager.getStereotypeDefinition(
                        annotation.annotationType()
                    )
                );
            }
        }

        return Optional.empty();
    }

    /* Copied from JSF */
    public static Bean<?> createHelperBean(BeanManager beanManager, Class<? extends Object> beanClass) {
        AnnotatedType<? extends Object> annotatedType = beanManager.createAnnotatedType(beanClass);
        BeanWrapper result = new BeanWrapper(beanClass);

        // Use this to create the class and inject dependencies
        @SuppressWarnings("unchecked")
        InjectionTargetFactory<Object> factory = (InjectionTargetFactory<Object>) beanManager.getInjectionTargetFactory(annotatedType);
        InjectionTarget<Object> injectionTarget = factory.createInjectionTarget(result);
        result.setInjectionTarget(injectionTarget);

        return result;
    }

    public static void fireEvent(String eventType) {
        BeanManager beanManager = null;
        try {
            beanManager = CDI.current().getBeanManager();
        } catch (Exception e) {
            log("Can't get instance of BeanManager to process TransactionScoped CDI Event!");
        }

        if (beanManager != null) {
            // TransactionScopedCDIEventHelperImpl AnnotatedType is created in Extension
            Set<Bean<?>> availableBeans = beanManager.getBeans(TransactionScopedCDIEventHelperImpl.class);

            if (null != availableBeans && !availableBeans.isEmpty()) {
                Bean<?> bean = beanManager.resolve(availableBeans);
                TransactionScopedCDIEventHelper eventHelper = (TransactionScopedCDIEventHelper)
                        beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(null));

                if (eventType.equalsIgnoreCase(INITIALIZED_EVENT)) {
                    eventHelper.fireInitializedEvent(new TransactionScopedCDIEventPayload());
                }
                else {
                    eventHelper.fireDestroyedEvent(new TransactionScopedCDIEventPayload());
                }
            }
        } else {
            log("Can't get instance of BeanManager to process TransactionScoped CDI Event!");
        }
    }

    /* Copied from JSF */
    private static class BeanWrapper implements Bean<Object> {

        private Class<?> beanClass;
        private InjectionTarget<Object> injectionTarget = null;
        private final Optional<JavaEEContextUtil.Instance> ctxUtil;

        public BeanWrapper(Class<?> beanClass) {
            this.beanClass = beanClass;
            Optional<JavaEEContextUtil> ctxUtil = Optional.empty();
            try {
                ServiceLocator serviceLocator = Globals.getDefaultHabitat();
                if (serviceLocator == null) {
                    serviceLocator = Globals.getStaticHabitat();
                }
                if (serviceLocator != null) {
                    ctxUtil = Optional.ofNullable(serviceLocator.getService(JavaEEContextUtil.class));
                }
            }
            catch(MultiException e) {
                log(e.getMessage());
            }
            this.ctxUtil = ctxUtil.map(ctx -> ctx.getInvocationComponentId() != null ?
                    ctx.currentInvocation() : ctx.empty());
        }

        private void setInjectionTarget(InjectionTarget<Object> injectionTarget) {
            this.injectionTarget = injectionTarget;
        }

        @Override
        public Class<?> getBeanClass() {
            return beanClass;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return injectionTarget.getInjectionPoints();
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            Set<Annotation> qualifiers = new HashSet<>();
            qualifiers.add(new DefaultAnnotationLiteral());
            qualifiers.add(new AnyAnnotationLiteral());
            return qualifiers;
        }

        public static class DefaultAnnotationLiteral extends AnnotationLiteral<Default> {
            private static final long serialVersionUID = -9065007202240742004L;

        }

        public static class AnyAnnotationLiteral extends AnnotationLiteral<Any> {
            private static final long serialVersionUID = -4700109250603725375L;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return Dependent.class;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public Set<Type> getTypes() {
            Set<Type> types = new HashSet<Type>();
            types.add(beanClass);
            types.add(Object.class);
            return types;
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public Object create(CreationalContext<Object> ctx) {
            try (Context eeCtx = ctxUtil.isPresent() ? ctxUtil.get().pushContext() : null) {
                Object instance = injectionTarget.produce(ctx);
                injectionTarget.inject(instance, ctx);
                injectionTarget.postConstruct(instance);
                return instance;
            }
        }

        @Override
        public void destroy(Object instance, CreationalContext<Object> ctx) {
            try (Context eeCtx = ctxUtil.isPresent() ? ctxUtil.get().pushContext() : null) {
                injectionTarget.preDestroy(instance);
                injectionTarget.dispose(instance);
            }
            ctx.release();
        }
    }
}
