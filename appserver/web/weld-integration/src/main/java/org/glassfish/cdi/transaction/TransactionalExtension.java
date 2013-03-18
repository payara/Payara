/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package org.glassfish.cdi.transaction;

import com.sun.logging.LogDomains;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.*;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The CDI Portable Extension for @Transactional.
 */
public class TransactionalExtension implements Extension {

  private static Logger _logger = LogDomains.getLogger(
            TransactionalInterceptorMandatory.class, LogDomains.JTA_LOGGER);

  public void afterBeanDiscovery( @Observes AfterBeanDiscovery event, BeanManager manager ) {
      _logger.fine("org.glassfish.cdi.transaction.TransactionalExtension.afterBeanDiscovery event = [" + event + "], manager = [" + manager + "] start");
//      Bean transactionalInterceptorBaseBean = createLocalBean(manager, TransactionalInterceptorBase.class);
//      event.addBean(transactionalInterceptorBaseBean);
      Bean transactionalInterceptorMandatoryBean = createLocalBean(manager, TransactionalInterceptorMandatory.class);
      event.addBean(transactionalInterceptorMandatoryBean);
      Bean transactionalInterceptorNeverBean = createLocalBean(manager, TransactionalInterceptorNever.class);
      event.addBean(transactionalInterceptorNeverBean);
      Bean transactionalInterceptorNotSupportedBean = createLocalBean(manager, TransactionalInterceptorNotSupported.class);
      event.addBean(transactionalInterceptorNotSupportedBean);
      Bean transactionalInterceptorRequiredBean = createLocalBean(manager, TransactionalInterceptorRequired.class);
      event.addBean(transactionalInterceptorRequiredBean);
      Bean transactionalInterceptorRequiresNewBean = createLocalBean(manager, TransactionalInterceptorRequiresNew.class);
      event.addBean(transactionalInterceptorRequiresNewBean);
      Bean transactionalInterceptorSupportsBean = createLocalBean(manager, TransactionalInterceptorSupports.class);
      event.addBean(transactionalInterceptorSupportsBean);
      _logger.fine("org.glassfish.cdi.transaction.TransactionalExtension.afterBeanDiscovery event = [" + event + "], manager = [" + manager + "] end");
    }

    private Bean createLocalBean(BeanManager beanManager, Class beanClass) {
        AnnotatedType annotatedType = beanManager.createAnnotatedType(beanClass);
        LocalBean localBean = new LocalBean(beanClass);
        InjectionTargetFactory injectionTargetFactory = beanManager.getInjectionTargetFactory(annotatedType);
        localBean.setInjectionTarget(injectionTargetFactory.createInjectionTarget(localBean));
        return localBean;
    }

    public class LocalBean implements Bean {
        private Class beanClass;
        private InjectionTarget injectionTarget;

        public LocalBean(Class beanClass) {
            this.beanClass = beanClass;
        }

        public LocalBean(Class beanClass, InjectionTarget injectionTarget) {
            this.beanClass = beanClass;
            this.injectionTarget = injectionTarget;
        }

        public void setInjectionTarget(InjectionTarget injectionTarget) {
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
            return beanClass.getName();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            Set<Annotation> qualifiers = new HashSet<Annotation>();
//            qualifiers.add(new AnnotationLiteral<Default>() {});
//            qualifiers.add(new AnnotationLiteral<Any>() {});
            return qualifiers;
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
            boolean loop = true;
            Class clazz = beanClass;
            while (loop) {
                Class[] interfaces = clazz.getInterfaces();
                for (Class t : interfaces) {
                    types.add(t);
                }
                clazz = clazz.getSuperclass();
                if (clazz == null) {
                    loop = false;
                    break;
                } else {
                    types.add(clazz);
                }
            }
            return types;
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public Object create(CreationalContext ctx) {
            Object instance = injectionTarget.produce(ctx);
            injectionTarget.inject(instance, ctx);
            injectionTarget.postConstruct(instance);
            return instance;
        }

        @Override
        public void destroy(Object instance, CreationalContext ctx) {
            injectionTarget.preDestroy(instance);
            injectionTarget.dispose(instance);
            ctx.release();
        }
    }

 }
