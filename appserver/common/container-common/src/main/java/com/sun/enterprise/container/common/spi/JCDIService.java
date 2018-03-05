/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2017 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.container.common.spi;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.Map;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbInterceptor;

import org.jvnet.hk2.annotations.Contract;


/**
 */
@Contract
public interface JCDIService {

    boolean isCurrentModuleJCDIEnabled();
    boolean isJCDIEnabled(BundleDescriptor bundle);
    boolean isCDIScoped(Class<?> clazz);

    void setELResolver(ServletContext servletContext) throws NamingException;

    <T> JCDIInjectionContext<T> createManagedObject(Class<T> managedClass, BundleDescriptor bundle);
    <T> JCDIInjectionContext<T> createManagedObject(Class<T> managedClass, BundleDescriptor bundle, boolean invokePostConstruct);

    <T> void injectManagedObject(T managedObject, BundleDescriptor bundle);

    /**
     * Create an interceptor instance for an ejb.
     * @param <T> instance type
     * @param interceptorClass The interceptor class.
     * @param ejbDesc The ejb descriptor of the ejb for which the interceptor is created.
     * @param ejbContext The ejb context.
     * @param ejbInterceptors All of the ejb interceptors for the ejb.
     *
     * @return The interceptor instance.
     */
    <T> T createInterceptorInstance( Class<T> interceptorClass,
                                     EjbDescriptor ejbDesc,
                                     JCDIService.JCDIInjectionContext<T> ejbContext,
                                     Set<EjbInterceptor> ejbInterceptors );

    /**
     * Create an ejb via CDI.
     *
     * @param <T> instance type
     * @param ejbDesc The ejb descriptor
     * @param ejbInfo Information about the ejb.  Entries are the com.sun.ejb.containers.BaseContainer
     *                and com.sun.ejb.containers.EJBContextImpl
     * @return The created EJB.
     */
    <T> JCDIInjectionContext<T> createJCDIInjectionContext(EjbDescriptor ejbDesc, Map<Class<?>, Object> ejbInfo);

    <T> JCDIInjectionContext<T> createJCDIInjectionContext(EjbDescriptor ejbDesc, T instance, Map<Class<?>, Object> ejbInfo);

    <T> void injectEJBInstance(JCDIInjectionContext<T> injectionCtx);

    /**
     * Create an empty JCDIInjectionContext.
     * @param <T> instance type
     * @return The empty JCDIInjectionContext.
     */
    <T> JCDIInjectionContext<T> createEmptyJCDIInjectionContext();

    public interface JCDIInjectionContext<T> {

        /**
         * @return The instance associated with this context.
         */
        T getInstance();

        /**
         * Set the instance on this context
         * @param instance The instance to set.
         */
        void setInstance( T instance );

        void cleanup(boolean callPreDestroy);

        /**
         * @return The injection target.
         */
        InjectionTarget<T> getInjectionTarget();

        /**
         * Set the injection target.
         * @param injectionTarget The injection target to set.
         */
        void setInjectionTarget( InjectionTarget<T> injectionTarget );

        /**
         * @return The creational context.
         */
        CreationalContext<T> getCreationalContext();

        /**
         * Set the creational context.
         * @param creationalContext The creational context.
         */
        void setCreationalContext( CreationalContext<T> creationalContext );

        /**
         * Add a dependent context to this context so that the dependent
         * context can be cleaned up when this one is.
         *
         * @param dependentContext
         *            The dependenct context.
         */
        void addDependentContext( JCDIInjectionContext<T> dependentContext );

        /**
         * @return The dependent contexts.
         */
        Collection<JCDIInjectionContext<T>> getDependentContexts();

        /**
         * Create the EJB and perform constructor injection, if applicable.  This should only happen when the
         * last interceptor method in the AroundConstruct interceptor chain invokes the InvocationContext.proceed
         * method. If the InvocationContext.proceed method is not invoked by an interceptor method,
         * the target instance will not be created.
         * @return ejb
         */
        T createEjbAfterAroundConstruct();
    }
}
