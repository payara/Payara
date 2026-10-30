/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2025] [Payara Foundation and/or its affiliates]

package org.glassfish.weld.services;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.InjectionCapable;
import com.sun.enterprise.deployment.InjectionInfo;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.ManagedBeanDescriptor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.InjectionTarget;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.ejb.api.EjbContainerServices;
import org.glassfish.internal.api.Globals;
import org.glassfish.weld.DeploymentImpl;
import org.glassfish.weld.connector.WeldUtils;
import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.jboss.weld.injection.spi.InjectionContext;
import org.jboss.weld.injection.spi.InjectionServices;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.xml.ws.WebServiceRef;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.glassfish.api.invocation.ComponentInvocation;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Class to provide actual injection of an annotation and related services
 */
public class InjectionServicesImpl implements InjectionServices {

    private static final Logger logger = Logger.getLogger(InjectionServicesImpl.class.getName());

    private static final String TRANSACTIONAL_EXTENSION_NAME = "org.glassfish.cdi.transaction.TransactionalExtension";

    private static final String TRANSACTION_EXTENSION_NAME = "org.glassfish.cdi.transaction.TransactionScopedContextExtension";

    private InjectionManager injectionManager;

    // Associated bundle context
    private BundleDescriptor bundleContext;

    private DeploymentImpl deployment;

    private Predicate<BackedAnnotatedType> availableAnnotatedType = n -> n != null && n.getIdentifier() != null;

    private Predicate<AnnotatedTypeIdentifier> isTransactionExtension = t -> t.getBdaId().equals(TRANSACTIONAL_EXTENSION_NAME)
            || t.getBdaId().equals(TRANSACTION_EXTENSION_NAME);

    public InjectionServicesImpl(InjectionManager injectionMgr, BundleDescriptor context, DeploymentImpl deployment) {
        injectionManager = injectionMgr;
        bundleContext = context;
        this.deployment = deployment;
    }

    /**
     * Checks if the specified class is annotated with @Interceptor
     * @see jakarta.interceptor.Interceptor
     * @param beanClass
     * @return 
     */
    private boolean isInterceptor( Class beanClass ) {
      final Set<String> annos = Collections.singleton(jakarta.interceptor.Interceptor.class.getName());
      boolean res = false;
      while ( !res && beanClass != Object.class ) {
        res = WeldUtils.hasValidAnnotation( beanClass, annos, null );
        beanClass  = beanClass.getSuperclass();
      }
      return res;
    }

    @Override
    public <T> void aroundInject(InjectionContext<T> injectionContext) {
        try {
            ServiceLocator serviceLocator = Globals.getDefaultHabitat();
            ComponentEnvManager compEnvManager = serviceLocator.getService(ComponentEnvManager.class);
            EjbContainerServices containerServices = serviceLocator.getService(EjbContainerServices.class);

            JndiNameEnvironment componentEnv = compEnvManager.getCurrentJndiNameEnvironment();
            if(componentEnv == null) {
                InvocationManager invMgr = serviceLocator.getService(InvocationManager.class);
                if (invMgr.getCurrentInvocation() != null) {
                    componentEnv = (JndiNameEnvironment)invMgr.<ComponentInvocation>getCurrentInvocation().getJNDIEnvironment();
                }
            }

            ManagedBeanDescriptor mbDesc = null;

            JndiNameEnvironment injectionEnv = null;
            if (bundleContext instanceof JndiNameEnvironment) {
                injectionEnv = (JndiNameEnvironment) bundleContext;
            }

            AnnotatedType annotatedType = injectionContext.getAnnotatedType();
            Class targetClass = annotatedType.getJavaClass();
            String targetClassName = targetClass.getName();
            Object target = injectionContext.getTarget();

            if ( isInterceptor( targetClass ) && isValidBundleContext()
                    && (componentEnv != null && !componentEnv.equals(injectionEnv)) ) {
                // Resources injected into interceptors must come from the environment in which the interceptor is
                // intercepting, not the environment in which the interceptor resides (for everything else!)
                // Must use the injectionEnv to get the injection info to determine where in jndi to look for the objects to inject.
                // must use the current jndi component env to lookup the objects to inject
                injectionManager.inject( targetClass, target, injectionEnv, null, false );
            } else {
                if (annotatedType instanceof BackedAnnotatedType) {
                    BackedAnnotatedType backedAnnotatedType = ((BackedAnnotatedType) annotatedType);
                    // Added condition to skip printing logs when the TransactionScopedCDIEventHelperImpl is tried
                    // to be used for the TransactionalScoped CDI Bean
                    if (componentEnv == null && availableAnnotatedType.test(backedAnnotatedType)
                            && isTransactionExtension.test(backedAnnotatedType.getIdentifier())) {
                        injectionContext.proceed();
                        return;
                    }
                }

                if (componentEnv == null) {
                    logger.log(Level.FINE,
                            "No valid EE environment for injection of {0}. The methods that is missing the context is {1}",
                            new Object[] {targetClass, injectionContext.getAnnotatedType().getMethods()});

                    processResourceAnnotations(injectionContext, target, targetClass);
                    injectionContext.proceed();
                    return;
                }

                // Perform EE-style injection on the target.  Skip PostConstruct since
                // in this case 299 impl is responsible for calling it.

                if( componentEnv instanceof EjbDescriptor ) {

                    EjbDescriptor ejbDesc = (EjbDescriptor) componentEnv;

                    if( containerServices.isEjbManagedObject(ejbDesc, targetClass)) {
                        injectionEnv = componentEnv;
                    } else {

                        if( bundleContext instanceof EjbBundleDescriptor ) {

                            // Check if it's a @ManagedBean class within an ejb-jar.  In that case,
                            // special handling is needed to locate the EE env dependencies
                            mbDesc = bundleContext.getManagedBeanByBeanClass(targetClassName);
                        }
                    }
                }

                if( mbDesc != null ) {
                    injectionManager.injectInstance(target, mbDesc.getGlobalJndiName(), false);
                } else {
                    if( injectionEnv instanceof EjbBundleDescriptor ) {

                        // CDI-style managed bean that doesn't have @ManagedBean annotation but
                        // is injected within the context of an ejb.  Need to explicitly
                        // set the environment of the ejb bundle.
                        if ( target == null ) {
                            injectionManager.injectClass(targetClass, compEnvManager.getComponentEnvId(injectionEnv),false);
                        } else {
                            injectionManager.injectInstance(target, compEnvManager.getComponentEnvId(injectionEnv),false);
                        }
                    } else if (isValidBundleContext()) {
                        if ( target == null ) {
                            injectionManager.injectClass(targetClass, injectionEnv, false);
                        } else {
                            injectionManager.injectInstance(target, injectionEnv, false);
                        }
                    }
                }

                processResourceAnnotations(injectionContext, target, targetClass);
            }

            injectionContext.proceed();

        } catch(InjectionException ie) {
            throw new IllegalStateException(ie.getMessage(), ie);
        }
    }

    /**
     * Process @Resource annotation directly, similar to old ResourceInjectionServices.
     */
    private <T> void processResourceAnnotations(InjectionContext<T> injectionContext, Object target, Class targetClass) {
        if (target == null) {
            return;
        }

        for (Field field : targetClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Resource.class)) {
                Resource resource = field.getAnnotation(Resource.class);
                injectResourceField(target, field, resource);
            }
        }

        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Resource.class) && isSetterMethod(method)) {
                Resource resource = method.getAnnotation(Resource.class);
                injectResourceMethod(target, method, resource);
            }
        }
    }

    private void injectResourceField(Object target, Field field, Resource resource) {
        String lookupName = determineResourceName(resource, field);

        if (lookupName != null && !lookupName.isEmpty()) {
            try {
                InitialContext ctx = new InitialContext();
                Object value = ctx.lookup(lookupName);

                if (value != null) {
                    field.setAccessible(true);
                    field.set(target, value);

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Successfully injected resource {0} into field {1}",
                                new Object[]{lookupName, field.getName()});
                    }
                }
            } catch (NamingException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Failed to find " + lookupName + " in JNDI", e);
                }
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING, "Failed to inject resource into field " + field.getName(), e);
            }
        }
    }

    private void injectResourceMethod(Object target, Method method, Resource resource) {
        String lookupName = determineResourceName(resource, method);

        if (lookupName != null && !lookupName.isEmpty()) {
            try {
                InitialContext ctx = new InitialContext();
                Object value = ctx.lookup(lookupName);

                if (value != null) {
                    method.setAccessible(true);
                    method.invoke(target, value);

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Successfully injected resource {0} into method {1}",
                                new Object[]{lookupName, method.getName()});
                    }
                }
            } catch (NamingException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Failed to find " + lookupName + " in JNDI", e);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.log(Level.WARNING, "Failed to inject resource into method " + method.getName(), e);
            }
        }
    }

    private String determineResourceName(Resource resource, Member member) {
        // Priority: lookup > mappedName > name
        String lookupName = resource.lookup();
        if (!lookupName.isEmpty()) {
            return lookupName;
        }

        String mappedName = resource.mappedName();
        if (!mappedName.isEmpty()) {
            return mappedName;
        }

        String name = resource.name();
        if (!name.isEmpty()) {
            return "java:comp/env/" + name;
        }

        String propertyName;
        if (member instanceof Field) {
            propertyName = member.getName();
        } else if (member instanceof Method) {
            Method method = (Method) member;
            propertyName = getPropertyNameFromSetter(method);
        } else {
            return null;
        }

        String className = member.getDeclaringClass().getName();
        return "java:comp/env/" + className + "/" + propertyName;
    }

    private boolean isSetterMethod(Method method) {
        return method.getName().startsWith("set")
                && method.getParameterCount() == 1
                && method.getReturnType() == void.class;
    }

    private String getPropertyNameFromSetter(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("set") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        return methodName;
    }

    @Override
    public <T> void registerInjectionTarget(InjectionTarget<T> injectionTarget, AnnotatedType<T> annotatedType) {
        if ( bundleContext instanceof EjbBundleDescriptor || !isValidBundleContext() ) {
            // we can't handle validting producer fields for ejb bundles because the JNDI environment is not setup
            // yet for ejbs and so we can't get the correct JndiNameEnvironment to call getInjectionInfoByClass.
            // getInjectionInfoByClass caches the results and so causes subsequent calls to return invalid information.
            return;
        }

        // We are only validating producer fields of resources.  See spec section 3.7.1
        Class annotatedClass = annotatedType.getJavaClass();
        JndiNameEnvironment jndiNameEnvironment = (JndiNameEnvironment) bundleContext;

        InjectionInfo injectionInfo = jndiNameEnvironment.getInjectionInfoByClass(annotatedClass);
        List<InjectionCapable> injectionResources = injectionInfo.getInjectionResources();

        for (AnnotatedField<? super T> annotatedField : annotatedType.getFields()) {
            if ( annotatedField.isAnnotationPresent( Produces.class ) ) {
                if ( annotatedField.isAnnotationPresent( EJB.class ) ) {
                    validateEjbProducer( annotatedClass, annotatedField, injectionResources );
                } else if ( annotatedField.isAnnotationPresent( Resource.class ) ) {
                    validateResourceProducer( annotatedClass, annotatedField, injectionResources );
                } else if ( annotatedField.isAnnotationPresent( PersistenceUnit.class ) ) {
                    validateResourceClass(annotatedField, EntityManagerFactory.class);
                } else if ( annotatedField.isAnnotationPresent( PersistenceContext.class ) ) {
                    validateResourceClass(annotatedField, EntityManager.class);
                } else if ( annotatedField.isAnnotationPresent( WebServiceRef.class ) ) {
                    validateWebServiceRef( annotatedField );
                }
            }
        }

    }

    /**
     * 
     * @param annotatedClass
     * @param annotatedField
     * @param injectionResources 
     */
    private void validateEjbProducer( Class annotatedClass,
                                      AnnotatedField annotatedField,
                                      List<InjectionCapable> injectionResources ) {
        EJB ejbAnnotation = annotatedField.getAnnotation(EJB.class);
        if ( ejbAnnotation != null ) {
            String lookupName = getLookupName(annotatedClass,
                                              annotatedField,
                                              injectionResources);

            EjbDescriptor foundEjb = null;
            Collection<EjbDescriptor> ejbs = deployment.getDeployedEjbs();
            for ( EjbDescriptor oneEjb : ejbs ) {
                String jndiName = oneEjb.getJndiName();
                if(jndiName.isEmpty()) {
                    jndiName = oneEjb.getName();
                }
                if (lookupName.contains(jndiName)) {
                    foundEjb = oneEjb;
                    break;
                }
            }
            if ( foundEjb != null ) {
                String className = foundEjb.getEjbImplClassName();
                try {
                    Class clazz = Class.forName( className, false, annotatedClass.getClassLoader() );
                    validateResourceClass(annotatedField, clazz);
                } catch (ClassNotFoundException ignore) {
                }
            }
        }
    }

    private void validateResourceProducer( Class annotatedClass,
                                           AnnotatedField annotatedField,
                                           List<InjectionCapable> injectionResources ) {
        Resource resourceAnnotation = annotatedField.getAnnotation(Resource.class);
        if ( resourceAnnotation != null ) {
            String lookupName = getLookupName(annotatedClass,
                                              annotatedField,
                                              injectionResources);
            if ( lookupName.equals( "java:comp/BeanManager" ) ) {
                validateResourceClass(annotatedField, BeanManager.class);
            } else {
                boolean done = false;
                for (InjectionCapable injectionCapable : injectionResources) {
                    for (com.sun.enterprise.deployment.InjectionTarget target : injectionCapable.getInjectionTargets()) {
                        if( target.isFieldInjectable() ) {  // make sure it's a field and not a method
                            if ( annotatedClass.getName().equals(target.getClassName() ) &&
                                target.getFieldName().equals( annotatedField.getJavaMember().getName() ) ) {
                                String type = injectionCapable.getInjectResourceType();
                                try {
                                    Class clazz = Class.forName( type, false, annotatedClass.getClassLoader() );
                                    validateResourceClass(annotatedField, clazz);
                                } catch (ClassNotFoundException ignore) {
                                } finally {
                                    done = true;
                                }
                            }
                        }
                        if ( done ) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void validateWebServiceRef( AnnotatedField annotatedField ) {
        WebServiceRef webServiceRef = annotatedField.getAnnotation(WebServiceRef.class);
        if ( webServiceRef != null ) {
            if ( jakarta.xml.ws.Service.class.isAssignableFrom(annotatedField.getJavaMember().getType())) {
                return;
            }

            if ( annotatedField.getJavaMember().getType().isInterface() ) {
                Class serviceClass = webServiceRef.value();
                if ( serviceClass != null ) {
                    if ( ! jakarta.xml.ws.Service.class.isAssignableFrom(serviceClass)) {
                        throw new DefinitionException( "The type of the injection point " +
                                                       annotatedField.getJavaMember().getName() +
                                                       " is an interface: " +
                                                       annotatedField.getJavaMember().getType().getName() +
                                                       ".  The @WebSreviceRef value of " +
                                                       serviceClass +
                                                       " is not assignable from " +
                                                       jakarta.xml.ws.Service.class.getName());
                    }
                }
            } else {
                throw new DefinitionException( "The type of the injection point " +
                                                   annotatedField.getJavaMember().getName() +
                                                   " is " +
                                                   annotatedField.getJavaMember().getType().getName() +
                                                   ".  This type is invalid for a field annotated with @WebSreviceRef");
            }
        }
    }

    /**
     * Make sure that the annotated field can be assigned the resource that is to be injected.
     * Otherwise, this throws a {@link DefinitionException}
     * @param annotatedField
     * @param resourceClass 
     */
    private void validateResourceClass(AnnotatedField annotatedField, Class resourceClass) {
        if ( ! annotatedField.getJavaMember().getType().isAssignableFrom( resourceClass ) ) {
            throwProducerDefinitionExeption( annotatedField.getJavaMember().getName(),
                                             annotatedField.getJavaMember().getType().getName(),
                                             resourceClass.getName() );
        }
    }

    private void throwProducerDefinitionExeption( String annotatedFieldName,
                                                  String annotatedFieldType,
                                                  String resourceClassName ) {
        throw new DefinitionException( "The type of the injection point " +
                                       annotatedFieldName +
                                       " is " +
                                       annotatedFieldType +
                                       ".  The type of the physical resource is " +
                                       resourceClassName +
                                       " They are incompatible. ");
    }

    private String getComponentEnvName( Class annotatedClass, String fieldName, List<InjectionCapable> injectionResources ) {
        for (InjectionCapable injectionCapable : injectionResources) {
            for (com.sun.enterprise.deployment.InjectionTarget target : injectionCapable.getInjectionTargets()) {
                if( target.isFieldInjectable() ) {  // make sure it's a field and not a method
                    if ( annotatedClass.getName().equals(target.getClassName() ) &&
                         target.getFieldName().equals( fieldName ) ) {
                        String name = injectionCapable.getComponentEnvName();
                        if ( ! name.startsWith("java:") ) {
                            name = "java:comp/env/" + name;
                        }

                        return name;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns the JNDI name to lookup
     * <p>
     * This calls getJndiName with the appropriate arguments for the annotation type
     * @param annotatedClass
     * @param annotatedField
     * @param injectionResources
     * @return 
     */
    private String getLookupName( Class annotatedClass, AnnotatedField annotatedField, List<InjectionCapable> injectionResources ) {
        String lookupName = null;
        if ( annotatedField.isAnnotationPresent( Resource.class ) ) {
            Resource resource = annotatedField.getAnnotation( Resource.class );
            lookupName = getJndiName( resource.lookup(), resource.mappedName(), resource.name() );
        } else if ( annotatedField.isAnnotationPresent( EJB.class ) ) {
            EJB ejb = annotatedField.getAnnotation( EJB.class );
            lookupName = getJndiName(ejb.lookup(), ejb.mappedName(), ejb.name());
        } else if ( annotatedField.isAnnotationPresent( WebServiceRef.class ) ) {
            WebServiceRef webServiceRef = annotatedField.getAnnotation( WebServiceRef.class );
            lookupName = getJndiName(webServiceRef.lookup(), webServiceRef.mappedName(), webServiceRef.name());
        } else if ( annotatedField.isAnnotationPresent( PersistenceUnit.class ) ) {
            PersistenceUnit persistenceUnit = annotatedField.getAnnotation( PersistenceUnit.class );
            lookupName = getJndiName( persistenceUnit.unitName(), null, persistenceUnit.name() );
        } else if ( annotatedField.isAnnotationPresent( PersistenceContext.class ) ) {
            PersistenceContext persistenceContext = annotatedField.getAnnotation( PersistenceContext.class );
            lookupName = getJndiName( persistenceContext.unitName(), null, persistenceContext.name() );
        }

        if ( lookupName == null || lookupName.trim().length() == 0 ) {
            lookupName = getComponentEnvName( annotatedClass,
                                              annotatedField.getJavaMember().getName(),
                                              injectionResources );
        }
        return lookupName;
    }

    /**
     * Returns JNDI name
     * <p>
     * lookup > mappedName > name
     * @param lookup
     * @param mappedName
     * @param name
     * @return 
     */
    private String getJndiName( String lookup, String mappedName, String name ) {
        String jndiName = lookup;
        if ( jndiName == null || jndiName.length() == 0 ) {
            jndiName = mappedName;
            if ( jndiName == null || jndiName.length() == 0 ) {
                jndiName = name;
            }
        }

        return jndiName;
    }

    private boolean isValidBundleContext() {
        if (bundleContext instanceof Application || bundleContext instanceof ConnectorDescriptor) {
            return false;
        }
        return true;
    }

    @Override
    public void cleanup() {

    }
}
