/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.weld.BeanDeploymentArchiveImpl;
import org.glassfish.weld.WeldDeployer;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.manager.api.WeldManager;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;


@Service
public class JCDIServiceImpl implements JCDIService
{
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
        return (clazz.isAnnotationPresent(RequestScoped.class) ||
                clazz.isAnnotationPresent(ApplicationScoped.class) ||
                clazz.isAnnotationPresent(SessionScoped.class) ||
                clazz.isAnnotationPresent(ConversationScoped.class));
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

    public JCDIInjectionContext createJCDIInjectionContext(EjbDescriptor ejb, Object instance) {
	return _createJCDIInjectionContext(ejb, instance);
    }

    public JCDIInjectionContext createJCDIInjectionContext(EjbDescriptor ejb) {
	return _createJCDIInjectionContext(ejb, null);
    }

    // instance could be null. If null, create a new one
    private JCDIInjectionContext _createJCDIInjectionContext(EjbDescriptor ejb, 
							     Object instance) {

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
            logger.log(Level.FINE,"getBDAForBeanClass -- search in " + bundleDesc.getModuleName() + " for " + beanClassName);
        }
        BeanDeploymentArchive topLevelBDA = weldDeployer.getBeanDeploymentArchiveForBundle(bundleDesc);
        if (topLevelBDA.getBeanClasses().contains(beanClassName)){
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "JCDIServiceImpl.getBDAForBeanClass:: TopLevelBDA " 
                        + topLevelBDA.getId() + " contains beanClassName:" + beanClassName);
            }
            return topLevelBDA;
        }
        
        //for all sub-BDAs
        for (BeanDeploymentArchive bda: topLevelBDA.getBeanDeploymentArchives()){
            if (bda.getBeanClasses().contains(beanClassName)){
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "JCDIServiceImpl.getBDAForBeanClass:: subBDA " 
                            + bda.getId() + " contains beanClassName:" + beanClassName);
                }
                return bda;
            }
        }

        //If not found in any BDA's subclasses, return topLevel BDA
        return topLevelBDA;
    }
    

    public void injectEJBInstance(JCDIInjectionContext injectionCtx) {
    	JCDIInjectionContextImpl injectionCtxImpl = 
    	    (JCDIInjectionContextImpl) injectionCtx;
    
    	// Perform injection and call initializers
    	injectionCtxImpl.it.inject(injectionCtxImpl.instance, injectionCtxImpl.cc);
    
    	// NOTE : PostConstruct is handled by ejb container
    }

    public JCDIInjectionContext createManagedObject(Class managedClass, BundleDescriptor bundle) {
        return createManagedObject(managedClass, bundle, true);
    }


    /**
     * Perform 299 style injection on the <code>managedObject</code> argument.
     * @param managedObject the managed object
     * @param bundle  the bundle descriptor
     */
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

    public JCDIInjectionContext createManagedObject(Class managedClass, BundleDescriptor bundle,
                                                    boolean invokePostConstruct) {

        Object managedObject = null;

        BundleDescriptor topLevelBundleDesc = (BundleDescriptor) bundle.getModuleDescriptor().getDescriptor();

        // First get BeanDeploymentArchive for this ejb
        BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(topLevelBundleDesc);
        //BeanDeploymentArchive bda = getBDAForBeanClass(topLevelBundleDesc, managedClass.getName());

        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(bundle.getApplication());

        BeanManager beanManager = bootstrap.getManager(bda);

        AnnotatedType annotatedType = beanManager.createAnnotatedType(managedClass);
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

    private class JCDIInjectionContextImpl implements JCDIInjectionContext {

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

        public void cleanup(boolean callPreDestroy) {

            if( callPreDestroy ) {
                it.preDestroy(instance);
            }

            it.dispose(instance);
            cc.release();

        }
    }
}
