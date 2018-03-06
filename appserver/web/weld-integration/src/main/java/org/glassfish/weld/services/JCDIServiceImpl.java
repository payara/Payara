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

package org.glassfish.weld.services;

import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EJBContextImpl;
import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.cdi.CDILoggerInfo;
import org.glassfish.hk2.api.Rank;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.weld.BeanDeploymentArchiveImpl;
import org.glassfish.weld.WeldDeployer;
import org.glassfish.weld.connector.WeldUtils;
import org.jboss.weld.bean.InterceptorImpl;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.contexts.CreationalContextImpl;
import org.jboss.weld.contexts.WeldCreationalContext;
import org.jboss.weld.interceptor.proxy.InterceptionContext;
import org.jboss.weld.interceptor.spi.metadata.InterceptorClassMetadata;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.weld.util.reflection.Reflections;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.*;
import javax.inject.Inject;
import javax.inject.Scope;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Rank(10)
public class JCDIServiceImpl implements JCDIService {
	
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.cdi.LogMessages";

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

    @LoggerInfo(subsystem = "AS-WELD", description = "WELD", publish = true)
    public static final String WELD_LOGGER_SUBSYSTEM_NAME = "javax.enterprise.resource.weld";
    
    private static final Logger logger = Logger.getLogger(WELD_LOGGER_SUBSYSTEM_NAME,
            SHARED_LOGMESSAGE_RESOURCE);


    @Override
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

    @Override
    public boolean isJCDIEnabled(BundleDescriptor bundle) {

        // Get the top-level bundle descriptor from the given bundle.
        // E.g. allows EjbBundleDescriptor from a .war to be handled correctly.
        BundleDescriptor topLevelBundleDesc = (BundleDescriptor) bundle.getModuleDescriptor().getDescriptor();

        return weldDeployer.is299Enabled(topLevelBundleDesc);

    }

    @Override
    public boolean isCDIScoped(Class<?> clazz) {
        // Check all the annotations on the specified Class to determine if the class is annotated
        // with a supported CDI scope
        return WeldUtils.hasValidAnnotation(clazz, validScopes, excludedScopes);
    }

    @Override
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

    @Override
    public <T> JCDIInjectionContext<T> createJCDIInjectionContext(EjbDescriptor ejbDesc, T instance, Map<Class<?>, Object> ejbInfo ) {
        return _createJCDIInjectionContext(ejbDesc, instance, ejbInfo);
    }

    @Override
    public <T> JCDIInjectionContext<T> createJCDIInjectionContext(EjbDescriptor ejbDesc, Map<Class<?>, Object> ejbInfo) {
        return _createJCDIInjectionContext(ejbDesc, null, ejbInfo);
    }


    // instance could be null. If null, create a new one
    @SuppressWarnings("unchecked")
    private <T> JCDIInjectionContext<T> _createJCDIInjectionContext(EjbDescriptor ejb,
                                                                    T instance,
                                                                    Map<Class<?>, Object> ejbInfo) {
        BaseContainer baseContainer = null;
        EJBContextImpl ejbContext = null;
        JCDIInjectionContextImpl<T> jcdiCtx = null;
        CreationalContext<T> creationalContext = null;
        if ( ejbInfo != null ) {
            baseContainer = ( BaseContainer ) ejbInfo.get( BaseContainer.class );
            ejbContext = ( EJBContextImpl ) ejbInfo.get( EJBContextImpl.class );
        }

        BundleDescriptor topLevelBundleDesc = (BundleDescriptor)
                ejb.getEjbBundleDescriptor().getModuleDescriptor().getDescriptor();

        // First get BeanDeploymentArchive for this ejb
        BeanDeploymentArchive bda = getBDAForBeanClass(topLevelBundleDesc, ejb.getEjbClassName());

        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(ejb.getEjbBundleDescriptor().getApplication());
        WeldManager weldManager = bootstrap.getManager(bda);

        org.jboss.weld.ejb.spi.EjbDescriptor<T> ejbDesc = weldManager.getEjbDescriptor(ejb.getName());

        // get or create the ejb's creational context
        if ( null != ejbInfo ) {
            jcdiCtx = ( JCDIInjectionContextImpl<T> ) ejbInfo.get( JCDIService.JCDIInjectionContext.class );
        }
        if ( null != jcdiCtx ) {
            creationalContext = jcdiCtx.getCreationalContext();
        }
        if ( null != jcdiCtx && creationalContext == null ) {
            // The creational context may have been created by interceptors because they are created first
            // (see createInterceptorInstance below.)
            // And we only want to create the ejb's creational context once or we will have a memory
            // leak there too.
            Bean<T> bean = weldManager.getBean(ejbDesc);
            creationalContext = weldManager.createCreationalContext(bean);
            jcdiCtx.setCreationalContext( creationalContext );
        }

        // Create the injection target
        InjectionTarget<T> it = weldManager.createInjectionTarget(ejbDesc);
        if (null != jcdiCtx) {
            jcdiCtx.setInjectionTarget( it );
        }

        // JJS: 7/20/17 We must perform the around_construct interception because Weld does not know about
        // interceptors defined by descriptors.
        WeldCreationalContext<T> weldCreationalContext = (WeldCreationalContext) creationalContext;
        weldCreationalContext.setConstructorInterceptionSuppressed(true);

        JCDIAroundConstructCallback<T> aroundConstructCallback =
                new JCDIAroundConstructCallback<>( baseContainer, ejbContext );
        weldCreationalContext.registerAroundConstructCallback(  aroundConstructCallback );
        if (null != jcdiCtx) {
            jcdiCtx.setJCDIAroundConstructCallback( aroundConstructCallback );
        }
        T beanInstance = instance;

        if( beanInstance == null ) {
            // Create instance , perform constructor injection.
            beanInstance = it.produce(creationalContext);
        }
        if (null != jcdiCtx) {
            jcdiCtx.setInstance( beanInstance );
        }
        return jcdiCtx;
        // Injection is not performed yet. Separate injectEJBInstance() call is required.
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
    @Override
    public <T> void injectEJBInstance(JCDIInjectionContext<T> injectionCtx) {
    	JCDIInjectionContextImpl<T> injectionCtxImpl = (JCDIInjectionContextImpl<T>) injectionCtx;

    	// Perform injection and call initializers
    	injectionCtxImpl.it.inject(injectionCtxImpl.instance, injectionCtxImpl.cc);

    	// NOTE : PostConstruct is handled by ejb container
    }

    @Override
    public <T> JCDIInjectionContext<T> createManagedObject(Class<T> managedClass, BundleDescriptor bundle) {
        return createManagedObject(managedClass, bundle, true);
    }


    /**
     * Perform 299 style injection on the <code>managedObject</code> argument.
     * @param <T> instance type
     * @param managedObject the managed object
     * @param bundle  the bundle descriptor
     */
    @Override
    public<T> void injectManagedObject(T managedObject, BundleDescriptor bundle) {

        BundleDescriptor topLevelBundleDesc = (BundleDescriptor) bundle.getModuleDescriptor().getDescriptor();

        // First get BeanDeploymentArchive for this ejb
        BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(topLevelBundleDesc);
        //BeanDeploymentArchive bda = getBDAForBeanClass(topLevelBundleDesc, managedObject.getClass().getName());
        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(bundle.getApplication());
        BeanManager beanManager = bootstrap.getManager(bda);
        @SuppressWarnings("unchecked")
        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType((Class<T>) managedObject.getClass());
        InjectionTarget<T> it = beanManager.createInjectionTarget(annotatedType);
        CreationalContext<T> cc = beanManager.createCreationalContext(null);
        it.inject(managedObject, cc);
    }

    @SuppressWarnings("rawtypes")
    private Interceptor findEjbInterceptor(Class interceptorClass, Set<EjbInterceptor> ejbInterceptors) {
        for (EjbInterceptor oneInterceptor : ejbInterceptors) {
            Interceptor interceptor = oneInterceptor.getInterceptor();
            if (interceptor != null) {
                if (interceptor.getBeanClass().equals(interceptorClass)) {
                    return oneInterceptor.getInterceptor();
                }
            }
        }

        return null;
    }

    private <T> T getAroundConstructInterceptorInstance(Interceptor<T> interceptor, CreationalContext<T> creationalContext) {
        T instance = null;

        if (creationalContext instanceof CreationalContextImpl<?>) {
            CreationalContextImpl<T> weldContext = Reflections.cast(creationalContext);
            @SuppressWarnings("unchecked")
            InterceptorImpl<T> interceptorImpl = (InterceptorImpl) interceptor;
            InterceptorClassMetadata<T> interceptorClassMetadata = interceptorImpl.getInterceptorMetadata();
            InterceptionContext interceptionContext = weldContext.getAroundConstructInterceptionContext();
            instance = interceptionContext.getInterceptorInstance(interceptorClassMetadata);
        }

        return instance;
    }

    /**
     *
     * @param <T> interceptor type
     * @param interceptorClass The interceptor class.
     * @param ejb The ejb descriptor.
     * @param ejbContext The ejb jcdi context.  This context is only used to store any contexts for interceptors
     *                   not bound to the ejb.  Nothing else in this context will be used in this method as they are
     *                   most likely null.
     * @param ejbInterceptors All of the ejb interceptors for the ejb.
     *
     * @return The interceptor instance.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T createInterceptorInstance(Class<T> interceptorClass,
                                           EjbDescriptor ejb,
                                           JCDIService.JCDIInjectionContext<T> ejbContext,
                                           Set<EjbInterceptor> ejbInterceptors ) {
        BundleDescriptor topLevelBundleDesc = (BundleDescriptor)
                ejb.getEjbBundleDescriptor().getModuleDescriptor().getDescriptor();

        // First get BeanDeploymentArchive for this ejb
        BeanDeploymentArchive bda = getBDAForBeanClass(topLevelBundleDesc, ejb.getEjbClassName());

        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(ejb.getEjbBundleDescriptor().getApplication());
        WeldManager beanManager = bootstrap.getManager(bda);

        org.jboss.weld.ejb.spi.EjbDescriptor<T> ejbDesc = beanManager.getEjbDescriptor( ejb.getName());

        // get or create the ejb's creational context
        CreationalContext<T> creationalContext = ejbContext.getCreationalContext();
        if ( creationalContext == null ) {
            // We have to do this because interceptors are created before the ejb but in certain cases we must associate
            // the interceptors with the ejb so that they are cleaned up correctly.
            // And we only want to create the ejb's creational context once or we will have a memory
            // leak there too.
            Bean<T> bean = beanManager.getBean(ejbDesc);
            creationalContext = beanManager.createCreationalContext(bean);
            ejbContext.setCreationalContext( creationalContext );
        }

        // first see if there's an Interceptor object defined for the interceptorClass
        // This happens when @Interceptor or @InterceptorBinding is used.
        Interceptor<T> interceptor = findEjbInterceptor( interceptorClass, ejbInterceptors );
        if ( interceptor != null ) {
            // using the ejb's creationalContext so we don't have to do any cleanup.
            // the cleanup will be handled by weld when it clean's up the ejb.
            Object instance = beanManager.getReference( interceptor, interceptorClass, creationalContext );
            return ( T ) instance;
        }

        // Check to see if the interceptor was defined as a Bean.
        // This can happen when using @Interceptors to define the interceptors.
        Set<Bean<?>> availableBeans = beanManager.getBeans( interceptorClass);
        if ( availableBeans != null && !availableBeans.isEmpty()) {
            // using the ejb's creationalContext so we don't have to do any cleanup.
            // the cleanup will be handled by weld when it clean's up the ejb.
            Bean<?> interceptorBean = beanManager.resolve( availableBeans );
            Object instance = beanManager.getReference(interceptorBean, interceptorClass, creationalContext );
            return ( T ) instance;
        }

        // There are other interceptors like SessionBeanInterceptor that are
        // defined via code and they are not beans.
        // Cannot use the ejb's creationalContext.
        creationalContext = beanManager.createCreationalContext(null);

        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(interceptorClass);
        InjectionTarget<T> it =
                beanManager.getInjectionTargetFactory(annotatedType).createInterceptorInjectionTarget();
        T interceptorInstance = it.produce(creationalContext);
        it.inject(interceptorInstance, creationalContext);

        // make sure the interceptor's cdi objects get cleaned up when the ejb is cleaned up.
        ejbContext.addDependentContext( new JCDIInjectionContextImpl<>( it, creationalContext, interceptorInstance ) );

        return interceptorInstance;
    }

    @Override
    public <T> JCDIInjectionContext<T> createManagedObject(Class<T> managedClass, BundleDescriptor bundle, boolean invokePostConstruct) {

        T managedObject;

        BundleDescriptor topLevelBundleDesc = (BundleDescriptor) bundle.getModuleDescriptor().getDescriptor();

        // First get BeanDeploymentArchive for this ejb
        BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(topLevelBundleDesc);

        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(bundle.getApplication());

        BeanManager beanManager = bootstrap.getManager(bda);

        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(managedClass);
        if (!invokePostConstruct) {
            annotatedType = new NoPostConstructPreDestroyAnnotatedType<>(annotatedType);
        }

        @SuppressWarnings("unchecked")
        InjectionTarget<T> it = (InjectionTarget<T>) ((BeanDeploymentArchiveImpl)bda).getInjectionTarget(annotatedType);
        if (it == null) {
            it = ((WeldManager) beanManager).fireProcessInjectionTarget(annotatedType);
        }

        CreationalContext<T> cc = beanManager.createCreationalContext(null);

        managedObject = it.produce(cc);

        it.inject(managedObject, cc);

        if( invokePostConstruct ) {
            it.postConstruct(managedObject);
        }

        return new JCDIInjectionContextImpl<>(it, cc, managedObject);
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
            HashSet<AnnotatedMethod<? super X>> retVal = new HashSet<>();
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

    @Override
    public<T> JCDIInjectionContext<T> createEmptyJCDIInjectionContext() {
        return new JCDIInjectionContextImpl<>();
    }

    @SuppressWarnings("rawtypes")
    private static class JCDIInjectionContextImpl<T> implements JCDIInjectionContext<T> {
        InjectionTarget<T> it;
        CreationalContext<T> cc;
        T instance;

        private final List<JCDIInjectionContext<T>> dependentContexts = new ArrayList<>();
        private JCDIAroundConstructCallback jcdiAroundConstructCallback;

        public JCDIInjectionContextImpl() {
        }

        public JCDIInjectionContextImpl(InjectionTarget<T> it, CreationalContext<T> cc, T i) {
            this.it = it;
            this.cc = cc;
            this.instance = i;
        }


        @Override
        public T getInstance() {
            return instance;
        }

        @Override
        public void setInstance( T instance ) {
            this.instance = instance;
        }

        @Override
        public void cleanup(boolean callPreDestroy) {
            for ( JCDIInjectionContext context : dependentContexts ) {
                context.cleanup( true );
            }

            if( callPreDestroy ) {
                if ( it != null ) {
                    it.preDestroy(instance);
                }
            }

            if ( it != null ) {
                it.dispose(instance);
            }

            if ( cc != null ) {
                cc.release();
            }
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
            dependentContexts.add( dependentContext );
        }

        @Override
        public Collection<JCDIInjectionContext<T>> getDependentContexts() {
            return dependentContexts;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T createEjbAfterAroundConstruct() {
            if( null != jcdiAroundConstructCallback) {
                setInstance( (T) jcdiAroundConstructCallback.createEjb() );
            }
            return instance;
        }

        public void setJCDIAroundConstructCallback( JCDIAroundConstructCallback jcdiAroundConstructCallback ) {
            this.jcdiAroundConstructCallback = jcdiAroundConstructCallback;
        }
    }
}
