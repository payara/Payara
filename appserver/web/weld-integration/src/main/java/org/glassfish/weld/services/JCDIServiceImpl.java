/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld.services;

import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.cdi.CDILoggerInfo;
import org.glassfish.hk2.api.Rank;
import org.glassfish.weld.BeanDeploymentArchiveImpl;
import org.glassfish.weld.WeldDeployer;
import org.glassfish.weld.connector.WeldUtils;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.manager.api.WeldManager;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import javax.inject.Scope;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


@Service
@Rank(10)
public class JCDIServiceImpl implements JCDIService {

    private static final HashSet<String> validScopes = new HashSet<String>();
    static {
        validScopes.add(Scope.class.getName());
        validScopes.add(NormalScope.class.getName());
        validScopes.add(RequestScoped.class.getName());
        validScopes.add(SessionScoped.class.getName());
        validScopes.add(ApplicationScoped.class.getName());
        validScopes.add(ConversationScoped.class.getName());
    }

    private static final HashSet<String> excludedScopes = new HashSet<String>();
    static {
        excludedScopes.add(Dependent.class.getName());
    }


    @Inject
    private WeldDeployer weldDeployer;

    @Inject
    private ComponentEnvManager compEnvManager;

    @Inject
    private InvocationManager invocationManager;

    private Logger logger = Logger.getLogger(JCDIServiceImpl.class.getName());


    public boolean isCurrentModuleJCDIEnabled() {

        BundleDescriptor bundle = null;

        ComponentInvocation inv = invocationManager.getCurrentInvocation();

        if( inv == null ) {
            return false;
        }

        JndiNameEnvironment componentEnv =
            compEnvManager.getJndiNameEnvironment(inv.getComponentId());

        if( componentEnv != null ) {

            if( componentEnv instanceof BundleDescriptor ) {
                bundle = (BundleDescriptor) componentEnv;
            } else if( componentEnv instanceof EjbDescriptor ) {
                bundle = ((EjbDescriptor) componentEnv).getEjbBundleDescriptor();
            }
        }

        return (bundle != null) ? isJCDIEnabled(bundle) : false;

    }

    public boolean isJCDIEnabled(BundleDescriptor bundle) {

        // Get the top-level bundle descriptor from the given bundle.
        // E.g. allows EjbBundleDescriptor from a .war to be handled correctly.
        BundleDescriptor topLevelBundleDesc = (BundleDescriptor) bundle.getModuleDescriptor().getDescriptor();

        return weldDeployer.is299Enabled(topLevelBundleDesc);

    }

    public boolean isCDIScoped(Class<?> clazz) {
        // Check all the annotations on the specified Class to determine if the class is annotated
        // with a supported CDI scope
        return WeldUtils.hasValidAnnotation(clazz, validScopes, excludedScopes);
    }

    public void setELResolver(ServletContext servletContext) throws NamingException {
        InitialContext context = new InitialContext();
        BeanManager beanManager = (BeanManager)
            context.lookup("java:comp/BeanManager");
        if (beanManager != null) {
            servletContext.setAttribute(
                "org.glassfish.jsp.beanManagerELResolver",
                beanManager.getELResolver());
        }
    }

    public <T> JCDIInjectionContext<T> createJCDIInjectionContext(EjbDescriptor ejb, T instance) {
	    return _createJCDIInjectionContext(ejb, instance);
    }

    public <T> JCDIInjectionContext<T> createJCDIInjectionContext(EjbDescriptor ejb) {
	    return _createJCDIInjectionContext(ejb, null);
    }

    // instance could be null. If null, create a new one
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> JCDIInjectionContext<T> _createJCDIInjectionContext(EjbDescriptor ejb, T instance) {

        BundleDescriptor topLevelBundleDesc = (BundleDescriptor)
                ejb.getEjbBundleDescriptor().getModuleDescriptor().getDescriptor();

        // First get BeanDeploymentArchive for this ejb
        BeanDeploymentArchive bda = getBDAForBeanClass(topLevelBundleDesc, ejb.getEjbClassName());

        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(ejb.getEjbBundleDescriptor().getApplication());
        WeldManager weldManager = bootstrap.getManager(bda);

        org.jboss.weld.ejb.spi.EjbDescriptor ejbDesc = weldManager.getEjbDescriptor(ejb.getName());

        // Get an the Bean object
        Bean<?> bean = weldManager.getBean(ejbDesc);

        // Create the injection target
        InjectionTarget it = weldManager.createInjectionTarget(ejbDesc);

        // Per instance required, create the creational context
        CreationalContext<?> cc = weldManager.createCreationalContext(bean);

    	Object beanInstance = instance;

    	if( beanInstance == null ) {
    	    // Create instance , perform constructor injection.
    	    beanInstance = it.produce(cc);
    	}

    	// Injection is not performed yet. Separate injectEJBInstance() call is required.
        return new JCDIInjectionContextImpl(it, cc, beanInstance);

    }

    private BeanDeploymentArchive getBDAForBeanClass(BundleDescriptor bundleDesc, String beanClassName){
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,
                       CDILoggerInfo.GET_BDA_FOR_BEAN_CLASS_SEARCH,
                       new Object [] {bundleDesc.getModuleName(), beanClassName});
        }

        BeanDeploymentArchive topLevelBDA = weldDeployer.getBeanDeploymentArchiveForBundle(bundleDesc);
        if (topLevelBDA.getBeanClasses().contains(beanClassName)){
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                           CDILoggerInfo.TOP_LEVEL_BDA_CONTAINS_BEAN_CLASS_NAME,
                           new Object[]{ topLevelBDA.getId(), beanClassName});
            }
            return topLevelBDA;
        }

        //for all sub-BDAs
        for (BeanDeploymentArchive bda: topLevelBDA.getBeanDeploymentArchives()){
            if (bda.getBeanClasses().contains(beanClassName)){
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                               CDILoggerInfo.SUB_BDA_CONTAINS_BEAN_CLASS_NAME,
                               new Object[]{bda.getId(), beanClassName});
                }
                return bda;
            }
        }

        //If not found in any BDA's subclasses, return topLevel BDA
        return topLevelBDA;
    }


    @SuppressWarnings("unchecked")
    public <T> void injectEJBInstance(JCDIInjectionContext<T> injectionCtx) {
    	JCDIInjectionContextImpl<T> injectionCtxImpl = (JCDIInjectionContextImpl<T>) injectionCtx;

    	// Perform injection and call initializers
    	injectionCtxImpl.it.inject(injectionCtxImpl.instance, injectionCtxImpl.cc);

    	// NOTE : PostConstruct is handled by ejb container
    }

    public <T>JCDIInjectionContext<T> createManagedObject(Class<T> managedClass, BundleDescriptor bundle) {
        return createManagedObject(managedClass, bundle, true);
    }


    /**
     * Perform 299 style injection on the <code>managedObject</code> argument.
     * @param managedObject the managed object
     * @param bundle  the bundle descriptor
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void injectManagedObject(Object managedObject, BundleDescriptor bundle) {

        BundleDescriptor topLevelBundleDesc = (BundleDescriptor) bundle.getModuleDescriptor().getDescriptor();

        // First get BeanDeploymentArchive for this ejb
        BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(topLevelBundleDesc);
        //BeanDeploymentArchive bda = getBDAForBeanClass(topLevelBundleDesc, managedObject.getClass().getName());
        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(bundle.getApplication());
        BeanManager beanManager = bootstrap.getManager(bda);
        AnnotatedType annotatedType = beanManager.createAnnotatedType(managedObject.getClass());
        InjectionTarget it = beanManager.createInjectionTarget(annotatedType);
        CreationalContext cc = beanManager.createCreationalContext(null);
        it.inject(managedObject, cc);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> T createInterceptorInstance(Class<T> interceptorClass,  BundleDescriptor bundle) {
        BundleDescriptor topLevelBundleDesc = (BundleDescriptor) bundle.getModuleDescriptor().getDescriptor();

        // First get BeanDeploymentArchive for this ejb
        BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(topLevelBundleDesc);
        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(bundle.getApplication());
        BeanManager beanManager = bootstrap.getManager(bda);
        CreationalContext cc = beanManager.createCreationalContext(null);

        AnnotatedType annotatedType = beanManager.createAnnotatedType(interceptorClass);
        InjectionTarget it =
            ((WeldManager) beanManager).getInjectionTargetFactory(annotatedType).createInterceptorInjectionTarget();
        T interceptorInstance = (T) it.produce(cc);
        it.inject(interceptorInstance, cc);

        return interceptorInstance;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> JCDIInjectionContext<T> createManagedObject(Class<T> managedClass, BundleDescriptor bundle,
                                                    boolean invokePostConstruct) {

        Object managedObject = null;

        BundleDescriptor topLevelBundleDesc = (BundleDescriptor) bundle.getModuleDescriptor().getDescriptor();

        // First get BeanDeploymentArchive for this ejb
        BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(topLevelBundleDesc);

        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(bundle.getApplication());

        BeanManager beanManager = bootstrap.getManager(bda);

        AnnotatedType annotatedType = beanManager.createAnnotatedType(managedClass);
        if (!invokePostConstruct) {
            annotatedType = new NoPostConstructPreDestroyAnnotatedType(annotatedType);
        }

        InjectionTarget it = ((BeanDeploymentArchiveImpl)bda).getInjectionTarget(annotatedType);
        if (it == null) {
            it = beanManager.createInjectionTarget(annotatedType);
        }

        CreationalContext cc = beanManager.createCreationalContext(null);

        managedObject = it.produce(cc);

        it.inject(managedObject, cc);

        if( invokePostConstruct ) {
            it.postConstruct(managedObject);
        }

        return new JCDIInjectionContextImpl(it, cc, managedObject);

    }

    /**
     * This class is here to exclude the post-construct and pre-destroy methods from the AnnotatedType.
     * This is done in cases where Weld will not be calling those methods and we therefore do NOT want
     * Weld to validate them, as they may be of the form required for interceptors rather than
     * Managed objects
     *
     * @author jwells
     *
     * @param <X>
     */
    private static class NoPostConstructPreDestroyAnnotatedType<X> implements AnnotatedType<X> {
        private final AnnotatedType<X> delegate;

        private NoPostConstructPreDestroyAnnotatedType(AnnotatedType<X> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Type getBaseType() {
            return delegate.getBaseType();
        }

        @Override
        public Set<Type> getTypeClosure() {
            return delegate.getTypeClosure();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            return delegate.getAnnotation(annotationType);
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return delegate.getAnnotations();
        }

        @Override
        public boolean isAnnotationPresent(
                Class<? extends Annotation> annotationType) {
            return delegate.isAnnotationPresent(annotationType);
        }

        @Override
        public Class<X> getJavaClass() {
            return delegate.getJavaClass();
        }

        @Override
        public Set<AnnotatedConstructor<X>> getConstructors() {
            return delegate.getConstructors();
        }

        @Override
        public Set<AnnotatedMethod<? super X>> getMethods() {
            HashSet<AnnotatedMethod<? super X>> retVal = new HashSet<AnnotatedMethod<? super X>>();
            for (AnnotatedMethod<? super X> m : delegate.getMethods()) {
                if (m.isAnnotationPresent(PostConstruct.class) ||
                        m.isAnnotationPresent(PreDestroy.class)) {
                    // Do not include the post-construct or pre-destroy
                    continue;
                }

                retVal.add(m);
            }
            return retVal;
        }

        @Override
        public Set<AnnotatedField<? super X>> getFields() {
            return delegate.getFields();
        }



    }

    @SuppressWarnings("rawtypes")
    private static class JCDIInjectionContextImpl<T> implements JCDIInjectionContext<T> {

        InjectionTarget it;
        CreationalContext cc;
        T instance;

        JCDIInjectionContextImpl(InjectionTarget it, CreationalContext cc, T i) {
            this.it = it;
            this.cc = cc;
            this.instance = i;
        }


        public T getInstance() {
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
