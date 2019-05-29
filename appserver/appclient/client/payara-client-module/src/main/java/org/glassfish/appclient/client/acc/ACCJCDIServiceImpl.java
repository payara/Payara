/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.container.common.spi.InterceptorInvoker;
import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.JavaEEInterceptorBuilder;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.ManagedBeanDescriptor;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.manager.api.WeldManager;
import org.jvnet.hk2.annotations.Service;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import java.util.*;

/**
 * @author <a href="mailto:phil.zampino@oracle.com">Phil Zampino</a>
 */
@Service
public class ACCJCDIServiceImpl implements JCDIService {

    private WeldContainer weldContainer = null;

    @Inject
    private InjectionManager injectionMgr;

    @Override
    public boolean isCurrentModuleJCDIEnabled() {
        return hasBeansXML(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public boolean isJCDIEnabled(BundleDescriptor bundle) {
        return hasBeansXML(bundle.getClassLoader());
    }

    @Override
    public boolean isCDIScoped(Class<?> clazz) {
        throw new UnsupportedOperationException("Application Client Container");
    }

    @Override
    public void setELResolver(ServletContext servletContext) throws NamingException {
        throw new UnsupportedOperationException("Application Client Container");
    }

    @Override
    public<T> JCDIInjectionContext<T> createManagedObject(Class<T> managedClass, BundleDescriptor bundle) {
        return createManagedObject(managedClass, bundle, true);
    }

    private <T> T createEEManagedObject(ManagedBeanDescriptor desc) throws Exception {
        JavaEEInterceptorBuilder interceptorBuilder = (JavaEEInterceptorBuilder) desc.getInterceptorBuilder();

        InterceptorInvoker interceptorInvoker = interceptorBuilder.createInvoker(null);

        Object[] interceptorInstances = interceptorInvoker.getInterceptorInstances();

        // Inject interceptor instances
        for(int i = 0; i < interceptorInstances.length; i++) {
            injectionMgr.injectInstance(interceptorInstances[i], desc.getGlobalJndiName(), false);
        }

        interceptorInvoker.invokeAroundConstruct();

        // This is the managed bean class instance
        @SuppressWarnings("unchecked")
        T managedBean = (T) interceptorInvoker.getTargetInstance();

        injectionMgr.injectInstance(managedBean, desc);

        interceptorInvoker.invokePostConstruct();

        desc.addBeanInstanceInfo(managedBean, interceptorInvoker);

        return managedBean;
    }


    @Override
    public<T> JCDIInjectionContext<T> createManagedObject(Class<T> managedClass,
                                                    BundleDescriptor bundle,
                                                    boolean invokePostConstruct) {
        JCDIInjectionContext<T> context = null;

        T managedObject = null;

        try {
            managedObject =
                createEEManagedObject(bundle.getManagedBeanByBeanClass(managedClass.getName()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        WeldContainer wc = getWeldContainer();
        if (wc != null) {
            BeanManager beanManager = wc.getBeanManager();

            AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(managedClass);
            InjectionTarget<T> target = beanManager.createInjectionTarget(annotatedType);

            CreationalContext<T> cc = beanManager.createCreationalContext(null);

            target.inject(managedObject, cc);

            if( invokePostConstruct ) {
                target.postConstruct(managedObject);
            }

            context = new JCDIInjectionContextImpl<>(target, cc, managedObject);
        }

        return context;
    }


    @Override
    public<T> void injectManagedObject(T managedObject, BundleDescriptor bundle) {
        WeldContainer wc = getWeldContainer();

        if (wc != null) {
            BeanManager beanManager = wc.getBeanManager();

            @SuppressWarnings("unchecked")
            AnnotatedType<T> annotatedType = beanManager.createAnnotatedType((Class<T>) managedObject.getClass());
            InjectionTarget<T> target = beanManager.createInjectionTarget(annotatedType);

            CreationalContext<T> cc = beanManager.createCreationalContext(null);

            target.inject(managedObject, cc);
        }
    }

    @Override
    public <T> T createInterceptorInstance( Class<T> interceptorClass,
                                     EjbDescriptor ejbDesc,
                                     JCDIService.JCDIInjectionContext<T> ejbContext,
                                     Set<EjbInterceptor> ejbInterceptors ) {
        T interceptorInstance = null;

        WeldContainer wc = getWeldContainer();
        if (wc != null) {
            BeanManager beanManager = wc.getBeanManager();

            AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(interceptorClass);
            InjectionTarget<T> target =
                ((WeldManager) beanManager).getInjectionTargetFactory(annotatedType).createInterceptorInjectionTarget();

            CreationalContext<T> cc = beanManager.createCreationalContext(null);

            interceptorInstance = target.produce(cc);
            target.inject(interceptorInstance, cc);
        }

        return interceptorInstance;
    }


    @Override
    public <T> JCDIInjectionContext<T> createJCDIInjectionContext(EjbDescriptor ejbDesc, Map<Class<?>, Object> ejbInfo) {
        return createJCDIInjectionContext(ejbDesc, null, null);
    }


    @Override
    public <T> JCDIInjectionContext<T> createJCDIInjectionContext(EjbDescriptor ejbDesc, T instance, Map<Class<?>, Object> ejbInfo) {
        throw new UnsupportedOperationException("Application Client Container");
    }

    @Override
    public<T> JCDIInjectionContext<T> createEmptyJCDIInjectionContext() {
        return new JCDIInjectionContextImpl<>();
    }


    @Override
    public<T> void injectEJBInstance(JCDIInjectionContext<T> injectionCtx) {
        throw new UnsupportedOperationException("Application Client Container");
    }


    private WeldContainer getWeldContainer() {
        if (weldContainer == null) {
            try {
                weldContainer = (new Weld()).initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return weldContainer;
    }


    private boolean hasBeansXML(ClassLoader cl) {
        return (cl.getResource("META-INF/beans.xml") != null);
    }


    private static class JCDIInjectionContextImpl<T> implements JCDIInjectionContext<T> {

        InjectionTarget<T> it;
        CreationalContext<T> cc;
        T instance;

        JCDIInjectionContextImpl() {
        }

        JCDIInjectionContextImpl(InjectionTarget<T> it, CreationalContext<T> cc, T i) {
            this.it = it;
            this.cc = cc;
            this.instance = i;
        }

        @Override
        public T getInstance() {
            return instance;
        }

        @Override
        public void setInstance(T instance) {
            this.instance = instance;
        }

        @Override
        public void cleanup(boolean callPreDestroy) {

            if( callPreDestroy ) {
                it.preDestroy(instance);
            }

            it.dispose(instance);
            cc.release();
        }
        
        @Override
        public InjectionTarget<T> getInjectionTarget() {
            return it;
        }

        @Override
        public void setInjectionTarget(InjectionTarget<T> injectionTarget) {
            this.it = injectionTarget;
        }

        @Override
        public CreationalContext<T> getCreationalContext() {
            return cc;
        }

        @Override
        public void setCreationalContext(CreationalContext<T> creationalContext) {
            this.cc = creationalContext;
        }

        @Override
        public void addDependentContext( JCDIInjectionContext<T> dependentContext ) {
            // nothing for now
        }

        @Override
        public Collection<JCDIInjectionContext<T>> getDependentContexts() {
            return new ArrayList<>();
        }

        @Override
        public T createEjbAfterAroundConstruct() {
            // nothing for now
            return null;
        }
    }
}
