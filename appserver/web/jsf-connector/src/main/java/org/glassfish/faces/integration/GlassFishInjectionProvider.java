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

package org.glassfish.faces.integration;

import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.InjectionInfo;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.faces.spi.DiscoverableInjectionProvider;
import com.sun.faces.spi.AnnotationScanner;
import com.sun.faces.spi.AnnotationScanner.ScannedAnnotation;
import com.sun.faces.spi.InjectionProviderException;
import com.sun.faces.spi.HighAvailabilityEnabler;
import com.sun.faces.util.FacesLogger;
import java.net.URI;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;

import javax.servlet.ServletContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;
import com.sun.enterprise.web.Constants;
import com.sun.faces.config.WebConfiguration;
import org.apache.catalina.core.StandardContext;
/**
 * <p>This <code>InjectionProvider</code> is specific to the
 * GlassFish/SJSAS 9.x PE/EE application servers.</p>
 */
public class GlassFishInjectionProvider extends DiscoverableInjectionProvider implements AnnotationScanner, HighAvailabilityEnabler {

    private static final Logger LOGGER = FacesLogger.APPLICATION.getLogger();
    private static final String HABITAT_ATTRIBUTE =
            "org.glassfish.servlet.habitat";
    private ComponentEnvManager compEnvManager;
    private InjectionManager injectionManager;
    private InvocationManager invokeMgr;
    private JCDIService jcdiService;

    /**
     * <p>Constructs a new <code>GlassFishInjectionProvider</code> instance.</p>
     *
     * @param servletContext
     */
    public GlassFishInjectionProvider(ServletContext servletContext) {
        ServiceLocator defaultServices = (ServiceLocator)servletContext.getAttribute(
                HABITAT_ATTRIBUTE);
        compEnvManager = defaultServices.getService(ComponentEnvManager.class);
        invokeMgr = defaultServices.getService(InvocationManager.class);
        injectionManager = defaultServices.getService(InjectionManager.class);
        jcdiService = defaultServices.getService(JCDIService.class);
        
    }

    @Override
    public Map<String, List<ScannedAnnotation>> getAnnotatedClassesInCurrentModule(ServletContext servletContext)
    throws InjectionProviderException {

        DeploymentContext dc = (DeploymentContext)servletContext.getAttribute(Constants.DEPLOYMENT_CONTEXT_ATTRIBUTE);
        Types types = dc.getTransientAppMetaData(Types.class.getName(), Types.class);
        Collection<Type> allTypes = types.getAllTypes();
        Collection<AnnotationModel> annotations = null;
        Map<String, List<ScannedAnnotation>> classesByAnnotation = 
                new HashMap<String, List<ScannedAnnotation>>();
        List<ScannedAnnotation> classesWithThisAnnotation = null;
        for (final Type cur : allTypes) {
            annotations = cur.getAnnotations();
            ScannedAnnotation toAdd = null;
            for (AnnotationModel curAnnotation : annotations) {
                String curAnnotationName = curAnnotation.getType().getName();
                if (null == (classesWithThisAnnotation = classesByAnnotation.get(curAnnotationName))) {
                    classesWithThisAnnotation = new ArrayList<ScannedAnnotation>();
                    classesByAnnotation.put(curAnnotationName, classesWithThisAnnotation);
                }
                toAdd = new ScannedAnnotation() {

                    @Override
                    public boolean equals(Object obj) {
                        boolean result = false;
                        if (obj instanceof ScannedAnnotation) {
                            String otherName = ((ScannedAnnotation)obj).getFullyQualifiedClassName();
                            if (null != otherName) {
                                result = cur.getName().equals(otherName);
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
                        return cur.getName();
                    }

                    @Override
                    public Collection<URI> getDefiningURIs() {
                        return cur.getDefiningURIs();
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
     * <p>The implementation of this method must perform the following
     * steps:
     * <ul>
     * <li>Inject the supported resources per the Servlet 2.5
     * specification into the provided object</li>    
     * </ul>
     * </p>
     *
     * @param managedBean the target managed bean
     */
    public void inject(Object managedBean) throws InjectionProviderException {
        try {
            injectionManager.injectInstance(managedBean,
                                            getNamingEnvironment(),
                                            false);

            if (jcdiService.isCurrentModuleJCDIEnabled()) {
                jcdiService.injectManagedObject(managedBean, getBundle());
  
            }

        } catch (InjectionException ie) {
            throw new InjectionProviderException(ie);
        }
    }

    /**
     * <p>The implemenation of this method must invoke any
     * method marked with the <code>@PreDestroy</code> annotation
     * (per the Common Annotations Specification).
     *
     * @param managedBean the target managed bean
     */
    public void invokePreDestroy(Object managedBean)
    throws InjectionProviderException {
        try {
            injectionManager.invokeInstancePreDestroy(managedBean);
        } catch (InjectionException ie) {
            throw new InjectionProviderException(ie);
        }
    }


    /**
     * <p>The implemenation of this method must invoke any
     * method marked with the <code>@PostConstruct</code> annotation
     * (per the Common Annotations Specification).
     *
     * @param managedBean the target managed bean
     *
     * @throws com.sun.faces.spi.InjectionProviderException
     *          if an error occurs when invoking
     *          the method annotated by the <code>@PostConstruct</code> annotation
     */
    public void invokePostConstruct(Object managedBean)
          throws InjectionProviderException {
            try {
                this.invokePostConstruct(managedBean, getNamingEnvironment());
            } catch (InjectionException ie) {
                throw new InjectionProviderException(ie);
            }

    }


    // --------------------------------------------------------- Private Methods

    /**
     * <p>This is based off of code in <code>InjectionManagerImpl</code>.</p>
     * @return <code>JndiNameEnvironment</code>
     * @throws InjectionException if we're unable to obtain the
     *  <code>JndiNameEnvironment</code>
     */
    private JndiNameEnvironment getNamingEnvironment()
         throws InjectionException {
        ComponentInvocation inv = invokeMgr.getCurrentInvocation();

        if (inv != null) {

            if (inv.getInvocationType()== ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION) {

                JndiNameEnvironment componentEnv = (JndiNameEnvironment)
                     inv.jndiEnvironment;

                if (componentEnv != null) {
                    return componentEnv;
                } else {
                    throw new InjectionException("No descriptor registered for " + " current invocation : " + inv);
                }
            } else {
                throw new InjectionException("Wrong invocation type");
            }
        } else {
            throw new InjectionException("null invocation context");
        }
    }


    /**
     * <p>This is based off of code in <code>InjectionManagerImpl</code>.</p>
     *
     * @param instance managed bean instance
     * @param envDescriptor JNDI environment
     * @throws InjectionException if an error occurs
     */
    private void invokePostConstruct(Object instance,
                                     JndiNameEnvironment envDescriptor)
    throws InjectionException {
        LinkedList<Method> postConstructMethods = new LinkedList<Method>();

        Class<? extends Object> nextClass = instance.getClass();

        // Process each class in the inheritance hierarchy, starting with
        // the most derived class and ignoring java.lang.Object.
        while ((!Object.class.equals(nextClass)) && (nextClass != null)) {

            InjectionInfo injInfo =
                 envDescriptor.getInjectionInfoByClass(nextClass);

            if (injInfo.getPostConstructMethodName() != null) {

                Method postConstructMethod = getPostConstructMethod
                     (injInfo, nextClass);

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
     * <p>This is based off of code in <code>InjectionManagerImpl</code>.</p>
     * @param injInfo InjectionInfo
     * @param resourceClass target class
     * @return a Method marked with the @PostConstruct annotation
     * @throws InjectionException if an error occurs
     */
    private Method getPostConstructMethod(InjectionInfo injInfo,
                                          Class<? extends Object> resourceClass)
        throws InjectionException {

        Method m = injInfo.getPostConstructMethod();

        if( m == null ) {
            String postConstructMethodName =
                injInfo.getPostConstructMethodName();

            // Check for the method within the resourceClass only.
            // This does not include super-classses.
            for(Method next : resourceClass.getDeclaredMethods()) {
                // InjectionManager only handles injection into PostConstruct
                // methods with no arguments.
                if( next.getName().equals(postConstructMethodName) &&
                    (next.getParameterTypes().length == 0) ) {
                    m = next;
                    injInfo.setPostConstructMethod(m);
                    break;
                }
            }
        }

        if( m == null ) {
            throw new InjectionException
                ("InjectionManager exception. PostConstruct method " +
                 injInfo.getPostConstructMethodName() +
                 " could not be found in class " +
                 injInfo.getClassName());
        }

        return m;
    }


    /**
     * <p>This is based off of code in <code>InjectionManagerImpl</code>.</p>
     * @param lifecycleMethod the method to invoke
     * @param instance the instanced to invoke the method against
     * @throws InjectionException if an error occurs
     */
     private void invokeLifecycleMethod(final Method lifecycleMethod,
                                       final Object instance)
        throws InjectionException {

        try {

            if(LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Calling lifecycle method " +
                             lifecycleMethod + " on class " +
                             lifecycleMethod.getDeclaringClass());
            }

            // Wrap actual value insertion in doPrivileged to
            // allow for private/protected field access.
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedExceptionAction<Object>() {
                    public java.lang.Object run() throws Exception {
                        if( !lifecycleMethod.isAccessible() ) {
                            lifecycleMethod.setAccessible(true);
                        }
                        lifecycleMethod.invoke(instance);
                        return null;
                    }
                });
        } catch( Exception t) {

                String msg = "Exception attempting invoke lifecycle "
                    + " method " + lifecycleMethod;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, msg, t);
                }
                InjectionException ie = new InjectionException(msg);
                Throwable cause = (t instanceof InvocationTargetException) ?
                    t.getCause() : t;
                ie.initCause( cause );
                throw ie;

        }

        return;

    }

    private BundleDescriptor getBundle() {

        JndiNameEnvironment env = compEnvManager.getCurrentJndiNameEnvironment();

        BundleDescriptor bundle = null;

        if( env instanceof BundleDescriptor) {

           bundle = (BundleDescriptor) env;

        }

        if( bundle == null ) {
           throw new IllegalStateException("Invalid context for managed bean creation");
        }

        return bundle;

    }

    /**
     * Method to test with HA has been enabled.
     * If so, then set the JSF context param
     * com.sun.faces.enableAgressiveSessionDirtying to true
     * @param ctx
     */
    public void enableHighAvailability(ServletContext ctx) {
        //look at the following values for the web app
        //1> has <distributable /> in the web.xml
        //2> Was deployed with --availabilityenabled --target <clustername>
        WebConfiguration config = WebConfiguration.getInstance(ctx);
        if (!config.isSet(WebConfiguration.BooleanWebContextInitParameter.EnableAgressiveSessionDirtying)) {
            Object isDistributableObj = ctx.getAttribute(Constants.IS_DISTRIBUTABLE_ATTRIBUTE);
            Object enableHAObj = ctx.getAttribute(Constants.ENABLE_HA_ATTRIBUTE);
            if (isDistributableObj instanceof Boolean
                    && enableHAObj instanceof Boolean) {
                boolean isDistributable = ((Boolean)isDistributableObj).booleanValue();
                boolean enableHA = ((Boolean)enableHAObj).booleanValue();

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE,
                            "isDistributable = {0} enableHA = {1}",
                            new Object[]{isDistributable, enableHA});
                }
                if (isDistributable && enableHA) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("setting the EnableAgressiveSessionDirtying to true");
                    }
                    config.overrideContextInitParameter(WebConfiguration.BooleanWebContextInitParameter.EnableAgressiveSessionDirtying,
                            Boolean.TRUE);
                }
            }
        }
    }
} // END GlassFishInjectionProvider
