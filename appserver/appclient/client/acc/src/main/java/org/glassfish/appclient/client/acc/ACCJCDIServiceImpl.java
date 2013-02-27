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
package org.glassfish.appclient.client.acc;

import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jvnet.hk2.annotations.Service;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.naming.NamingException;
import javax.servlet.ServletContext;

/**
 * @author <a href="mailto:phil.zampino@oracle.com">Phil Zampino</a>
 */
@Service
public class ACCJCDIServiceImpl implements JCDIService {

    private WeldContainer weldContainer = null;


    @Override
    public boolean isCurrentModuleJCDIEnabled() {
        return true;
    }


    @Override
    public boolean isJCDIEnabled(BundleDescriptor bundle) {
        return true;
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
    public JCDIInjectionContext createManagedObject(Class managedClass, BundleDescriptor bundle) {
        return createManagedObject(managedClass, bundle, true);
    }


    @Override
    @SuppressWarnings("unchecked")
    public JCDIInjectionContext createManagedObject(Class            managedClass,
                                                    BundleDescriptor bundle,
                                                    boolean          invokePostConstruct) {
        JCDIInjectionContext context = null;

        WeldContainer wc = getWeldContainer();
        if (wc != null) {
            BeanManager beanManager = wc.getBeanManager();

            AnnotatedType annotatedType = beanManager.createAnnotatedType(managedClass);
            InjectionTarget target = beanManager.createInjectionTarget(annotatedType);

            CreationalContext cc = beanManager.createCreationalContext(null);

            Object managedObject = target.produce(cc);

            target.inject(managedObject, cc);

            if( invokePostConstruct ) {
                target.postConstruct(managedObject);
            }

            context = new JCDIInjectionContextImpl(target, cc, managedObject);
        }

        return context;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void injectManagedObject(Object managedObject, BundleDescriptor bundle) {
        WeldContainer wc = getWeldContainer();

        if (wc != null) {
            BeanManager beanManager = wc.getBeanManager();

            AnnotatedType annotatedType = beanManager.createAnnotatedType(managedObject.getClass());
            InjectionTarget target = beanManager.createInjectionTarget(annotatedType);

            CreationalContext cc = beanManager.createCreationalContext(null);

            target.inject(managedObject, cc);
        }
    }


    @Override
    public JCDIInjectionContext createJCDIInjectionContext(EjbDescriptor ejbDesc) {
        return createJCDIInjectionContext(ejbDesc, null);
    }


    @Override
    public JCDIInjectionContext createJCDIInjectionContext(EjbDescriptor ejbDesc, Object instance) {
        throw new UnsupportedOperationException("Application Client Container");
    }


    @Override
    public void injectEJBInstance(JCDIInjectionContext injectionCtx) {
        throw new UnsupportedOperationException("Application Client Container");
    }


    private WeldContainer getWeldContainer() {
        if (weldContainer == null) {
            weldContainer = (new Weld()).initialize();
        }
        return weldContainer;
    }


    private static class JCDIInjectionContextImpl implements JCDIInjectionContext {

        InjectionTarget it;
        CreationalContext cc;
        Object instance;

        JCDIInjectionContextImpl(InjectionTarget it, CreationalContext cc, Object i) {
            this.it = it;
            this.cc = cc;
            this.instance = i;
        }

        public Object getInstance() {
            return instance;
        }

        @SuppressWarnings("unchecked")
        public void cleanup(boolean callPreDestroy) {

            if( callPreDestroy ) {
                it.preDestroy(instance);
            }

            it.dispose(instance);
            cc.release();
        }
    }


}
