/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright 2024 Payara Foundation and/or its affiliates

package org.glassfish.faces.integration;

import static com.sun.enterprise.web.Constants.DEPLOYMENT_CONTEXT_ATTRIBUTE;
import static com.sun.enterprise.web.Constants.ENABLE_HA_ATTRIBUTE;
import static com.sun.enterprise.web.Constants.IS_DISTRIBUTABLE_ATTRIBUTE;
import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.EnableDistributable;
import static java.lang.Boolean.TRUE;
import static java.security.AccessController.doPrivileged;
import static java.util.logging.Level.FINE;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.servlet.ServletContext;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;

import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.InjectionInfo;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.faces.config.WebConfiguration;
import com.sun.faces.spi.AnnotationScanner;
import com.sun.faces.spi.DiscoverableInjectionProvider;
import com.sun.faces.spi.HighAvailabilityEnabler;
import com.sun.faces.spi.InjectionProviderException;
import com.sun.faces.spi.ThreadContext;
import com.sun.faces.util.FacesLogger;

/**
 * <p>
 * This <code>InjectionProvider</code> is specific to the Payara/GlassFish/SJSAS 9.x PE/EE application servers.
 * </p>
 */
public class GlassFishInjectionProvider extends DiscoverableInjectionProvider implements AnnotationScanner, HighAvailabilityEnabler, ThreadContext {

    private static final Logger LOGGER = FacesLogger.APPLICATION.getLogger();
    private static final String HABITAT_ATTRIBUTE = "org.glassfish.servlet.habitat";
    
    private ComponentEnvManager compEnvManager;
    private InjectionManager injectionManager;
    private InvocationManager invocationManager;
    private JCDIService cdiService;

    /**
     * <p>
     * Constructs a new <code>GlassFishInjectionProvider</code> instance.
     * </p>
     *
     * @param servletContext
     */
    public GlassFishInjectionProvider(ServletContext servletContext) {
        ServiceLocator defaultServices = (ServiceLocator) servletContext.getAttribute(HABITAT_ATTRIBUTE);
        
        compEnvManager = defaultServices.getService(ComponentEnvManager.class);
        invocationManager = defaultServices.getService(InvocationManager.class);
        injectionManager = defaultServices.getService(InjectionManager.class);
        cdiService = defaultServices.getService(JCDIService.class);
    }

    @Override
    public Map<String, List<ScannedAnnotation>> getAnnotatedClassesInCurrentModule(ServletContext servletContext) throws InjectionProviderException {

        Map<String, List<ScannedAnnotation>> classesByAnnotation = new HashMap<String, List<ScannedAnnotation>>();
        
        Collection<Type> allTypes = ((DeploymentContext) 
                servletContext.getAttribute(DEPLOYMENT_CONTEXT_ATTRIBUTE))
                              .getTransientAppMetaData(Types.class.getName(), Types.class)
                              .getAllTypes();
        
        for (Type type : allTypes) {
            
            for (AnnotationModel annotationModel : type.getAnnotations()) {
                String annotationName = annotationModel.getType().getName();
                
                List<ScannedAnnotation> classesWithThisAnnotation = classesByAnnotation.get(annotationName);
                
                if (classesWithThisAnnotation == null) {
                    classesWithThisAnnotation = new ArrayList<ScannedAnnotation>();
                    classesByAnnotation.put(annotationName, classesWithThisAnnotation);
                }
                
                ScannedAnnotation toAdd = new ScannedAnnotation() {

                    @Override
                    public boolean equals(Object obj) {
                        boolean result = false;
                        if (obj instanceof ScannedAnnotation) {
                            String otherName = ((ScannedAnnotation) obj).getFullyQualifiedClassName();
                            if (otherName != null) {
                                result = type.getName().equals(otherName);
                            }
                        }

                        return result;
                    }

                    @Override
                    public int hashCode() {
                        String str = getFullyQualifiedClassName();
                        Collection<URI> obj = getDefiningURIs();
                        int result = str != null ? str.hashCode() : 0;
                        result = 31 * result + (obj != null ? obj.hashCode() : 0);
                        return result;
                    }

                    @Override
                    public String getFullyQualifiedClassName() {
                        return type.getName();
                    }

                    @Override
                    public Collection<URI> getDefiningURIs() {
                        return type.getDefiningURIs();
                    }

                };
                
                if (!classesWithThisAnnotation.contains(toAdd)) {
                    classesWithThisAnnotation.add(toAdd);
                }
            }
        }
        
        return classesByAnnotation;
    }

    /**
     * <p>
     * The implementation of this method must perform the following steps:
     * <ul>
     * <li>Inject the supported resources per the Servlet 2.5 specification into the provided object</li>
     * </ul>
     * </p>
     *
     * @param managedBean
     *            the target managed bean
     */
    public void inject(Object managedBean) throws InjectionProviderException {
        try {
            injectionManager.injectInstance(managedBean, getComponentEnvironment(), false);

            if (cdiService.isCurrentModuleJCDIEnabled()) {
                cdiService.injectManagedObject(managedBean, getBundle());

            }

        } catch (InjectionException ie) {
            throw new InjectionProviderException(ie);
        }
    }
    
    /**
     * <p>
     * The implemenation of this method must invoke any method marked with the <code>@PostConstruct</code> annotation (per
     * the Common Annotations Specification).
     *
     * @param managedBean
     *            the target managed bean
     *
     * @throws com.sun.faces.spi.InjectionProviderException
     *             if an error occurs when invoking the method annotated by the <code>@PostConstruct</code> annotation
     */
    public void invokePostConstruct(Object managedBean) throws InjectionProviderException {
        try {
            invokePostConstruct(managedBean, getComponentEnvironment());
        } catch (InjectionException ie) {
            throw new InjectionProviderException(ie);
        }

    }

    /**
     * <p>
     * The implemenation of this method must invoke any method marked with the <code>@PreDestroy</code> annotation (per the
     * Common Annotations Specification).
     *
     * @param managedBean
     *            the target managed bean
     */
    public void invokePreDestroy(Object managedBean) throws InjectionProviderException {
        try {
            injectionManager.invokeInstancePreDestroy(managedBean);
        } catch (InjectionException ie) {
            throw new InjectionProviderException(ie);
        }
    }
    

    // --------------------------------------------------------- ThreadContext
    
    @Override
    public Object getParentWebContext() {
        return invocationManager.getAllInvocations();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void propagateWebContextToChild(Object context) {
        
        if (!(context instanceof List)) {
            throw new IllegalArgumentException("Context of incorrect type, was it obtained by calling getParentWebContext()?");
        }
        
        invocationManager.setThreadInheritableInvocation((List<? extends ComponentInvocation>) context);
    }
    
    @Override
    public void clearChildContext() {
        invocationManager.popAllInvocations();
    }
    

    
    // --------------------------------------------------------- Private Methods

    /**
     * <p>
     * This is based off of code in <code>InjectionManagerImpl</code>.
     * </p>
     * 
     * @return <code>JndiNameEnvironment</code>
     * @throws InjectionException
     *             if we're unable to obtain the <code>JndiNameEnvironment</code>
     */
    private JndiNameEnvironment getComponentEnvironment() throws InjectionException {
        ComponentInvocation invocation = invocationManager.getCurrentInvocation();
        
        if (invocation == null) {
            throw new InjectionException("null invocation context");
        }
        
        if (invocation.getInvocationType() != SERVLET_INVOCATION) {
            throw new InjectionException("Wrong invocation type");
        }

        JndiNameEnvironment componentEnvironment = (JndiNameEnvironment) invocation.getJndiEnvironment();
        
        if (componentEnvironment == null) {
            throw new InjectionException("No descriptor registered for " + " current invocation : " + invocation);
        }
        
        return componentEnvironment;
    }

    /**
     * <p>
     * This is based off of code in <code>InjectionManagerImpl</code>.
     * </p>
     *
     * @param instance
     *            managed bean instance
     * @param envDescriptor
     *            JNDI environment
     * @throws InjectionException
     *             if an error occurs
     */
    private void invokePostConstruct(Object instance, JndiNameEnvironment envDescriptor) throws InjectionException {
        LinkedList<Method> postConstructMethods = new LinkedList<Method>();

        Class<? extends Object> nextClass = instance.getClass();

        // Process each class in the inheritance hierarchy, starting with
        // the most derived class and ignoring java.lang.Object.
        while ((!Object.class.equals(nextClass)) && (nextClass != null)) {

            InjectionInfo injectionInfo = envDescriptor.getInjectionInfoByClass(nextClass);

            if (injectionInfo.getPostConstructMethodName() != null) {

                Method postConstructMethod = getPostConstructMethod(injectionInfo, nextClass);

                // Invoke the preDestroy methods starting from
                // the least-derived class downward.
                postConstructMethods.addFirst(postConstructMethod);
            }

            nextClass = nextClass.getSuperclass();
        }

        for (Method postConstructMethod : postConstructMethods) {
            invokeLifecycleMethod(postConstructMethod, instance);
        }

    }

    /**
     * <p>
     * This is based off of code in <code>InjectionManagerImpl</code>.
     * </p>
     * 
     * @param injectionInfo
     *            InjectionInfo
     * @param resourceClass
     *            target class
     * @return a Method marked with the @PostConstruct annotation
     * @throws InjectionException
     *             if an error occurs
     */
    private Method getPostConstructMethod(InjectionInfo injectionInfo, Class<? extends Object> resourceClass) throws InjectionException {
        
        Method postConstructMethod = injectionInfo.getPostConstructMethod();

        if (postConstructMethod == null) {
            String postConstructMethodName = injectionInfo.getPostConstructMethodName();

            // Check for the method within the resourceClass only.
            // This does not include super-classses.
            for (Method declaredMethod : resourceClass.getDeclaredMethods()) {
                
                // InjectionManager only handles injection into PostConstruct
                // methods with no arguments.
                if (declaredMethod.getName().equals(postConstructMethodName) && declaredMethod.getParameterTypes().length == 0) {
                    postConstructMethod = declaredMethod;
                    injectionInfo.setPostConstructMethod(postConstructMethod);
                    break;
                }
            }
        }

        if (postConstructMethod == null) {
            throw new InjectionException(
                    "InjectionManager exception. PostConstruct method " + injectionInfo.getPostConstructMethodName() + 
                    " could not be found in class " + injectionInfo.getClassName());
        }

        return postConstructMethod;
    }

    /**
     * <p>
     * This is based off of code in <code>InjectionManagerImpl</code>.
     * </p>
     * 
     * @param lifecycleMethod
     *            the method to invoke
     * @param instance
     *            the instanced to invoke the method against
     * @throws InjectionException
     *             if an error occurs
     */
    private void invokeLifecycleMethod(final Method lifecycleMethod, final Object instance) throws InjectionException {

        try {

            if (LOGGER.isLoggable(FINE)) {
                LOGGER.fine("Calling lifecycle method " + lifecycleMethod + " on class " + lifecycleMethod.getDeclaringClass());
            }

            // Wrap actual value insertion in doPrivileged to
            // allow for private/protected field access.
            doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    if (!lifecycleMethod.isAccessible()) {
                        lifecycleMethod.setAccessible(true);
                    }
                    lifecycleMethod.invoke(instance);
                    return null;
                }
            });
        } catch (Exception t) {

            String msg = "Exception attempting invoke lifecycle " + " method " + lifecycleMethod;
            LOGGER.log(FINE, msg, t);
            
            InjectionException ie = new InjectionException(msg);
            Throwable cause = (t instanceof InvocationTargetException) ? t.getCause() : t;
            ie.initCause(cause);
            throw ie;
        }

        return;
    }

    private BundleDescriptor getBundle() {
        JndiNameEnvironment componentEnvironment = compEnvManager.getCurrentJndiNameEnvironment();

        BundleDescriptor bundle = null;

        if (componentEnvironment instanceof BundleDescriptor) {
            bundle = (BundleDescriptor) componentEnvironment;
        }

        if (bundle == null) {
            throw new IllegalStateException("Invalid context for managed bean creation");
        }

        return bundle;
    }

    /**
     * Method to test with HA has been enabled. If so, then set the JSF context param
     * com.sun.faces.enableAgressiveSessionDirtying to true
     * 
     * @param ctx
     */
    public void enableHighAvailability(ServletContext ctx) {
        
        // look at the following values for the web app
        // 1> has <distributable /> in the web.xml
        // 2> Was deployed with --availabilityenabled --target <clustername>
        
        WebConfiguration config = WebConfiguration.getInstance(ctx);
        if (!config.isSet(EnableDistributable)) {
            Object isDistributableObj = ctx.getAttribute(IS_DISTRIBUTABLE_ATTRIBUTE);
            Object enableHAObj = ctx.getAttribute(ENABLE_HA_ATTRIBUTE);
            
            if (isDistributableObj instanceof Boolean && enableHAObj instanceof Boolean) {
                boolean isDistributable = (Boolean) isDistributableObj;
                boolean enableHA = (Boolean) enableHAObj;

                if (LOGGER.isLoggable(FINE)) {
                    LOGGER.log(FINE, "isDistributable = {0} enableHA = {1}", new Object[] { isDistributable, enableHA });
                }
                
                if (isDistributable && enableHA) {
                    LOGGER.fine("setting EnableDistributable to true");
                    config.overrideContextInitParameter(EnableDistributable, TRUE);
                }
            }
        }
    }
}
