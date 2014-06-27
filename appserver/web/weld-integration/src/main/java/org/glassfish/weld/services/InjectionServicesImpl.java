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

import com.sun.enterprise.deployment.*;
import org.glassfish.ejb.api.EjbContainerServices;
import org.glassfish.internal.api.Globals;
import org.glassfish.weld.DeploymentImpl;
import org.glassfish.weld.connector.WeldUtils;
import org.jboss.weld.injection.spi.InjectionContext;
import org.jboss.weld.injection.spi.InjectionServices;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.*;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.WebServiceRef;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


public class InjectionServicesImpl implements InjectionServices {

    private InjectionManager injectionManager;

    // Associated bundle context
    private BundleDescriptor bundleContext;

    private DeploymentImpl deployment;

    public InjectionServicesImpl(InjectionManager injectionMgr, BundleDescriptor context, DeploymentImpl deployment) {
        injectionManager = injectionMgr;
        bundleContext = context;
        this.deployment = deployment;
    }

    private boolean isInterceptor( Class beanClass ) {
      HashSet<String> annos = new HashSet<>();
      annos.add( javax.interceptor.Interceptor.class.getName() );
      boolean res = false;
      while ( ! res && beanClass != Object.class ) {
        res = WeldUtils.hasValidAnnotation( beanClass, annos, null );
        beanClass  = beanClass.getSuperclass();
      }
      return res;
    }

    public <T> void aroundInject(InjectionContext<T> injectionContext) {
        try {
            ServiceLocator serviceLocator = Globals.getDefaultHabitat();
            ComponentEnvManager compEnvManager = serviceLocator.getService(ComponentEnvManager.class);
            EjbContainerServices containerServices = serviceLocator.getService(EjbContainerServices.class);

            JndiNameEnvironment componentEnv = compEnvManager.getCurrentJndiNameEnvironment();

            ManagedBeanDescriptor mbDesc = null;

            JndiNameEnvironment injectionEnv = (JndiNameEnvironment) bundleContext;

            AnnotatedType annotatedType = injectionContext.getAnnotatedType();
            Class targetClass = annotatedType.getJavaClass();
            String targetClassName = targetClass.getName();
            Object target = injectionContext.getTarget();

            if ( isInterceptor( targetClass ) && ( ! componentEnv.equals(injectionEnv) ) ) {
              // Resources injected into interceptors must come from the environment in which the interceptor is
              // intercepting, not the environment in which the interceptor resides (for everything else!)
              // Must use the injectionEnv to get the injection info to determine where in jndi to look for the objects to inject.
              // must use the current jndi component env to lookup the objects to inject
              injectionManager.inject( targetClass, target, injectionEnv, null, false );
            } else {
              if( componentEnv == null ) {
                //throw new IllegalStateException("No valid EE environment for injection of " + targetClassName);
                System.err.println("No valid EE environment for injection of " + targetClassName);
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
                } else {
                  if ( target == null ) {
                    injectionManager.injectClass(targetClass, injectionEnv, false);
                  } else {
                    injectionManager.injectInstance(target, injectionEnv, false);
                  }
                }
              }

            }

            injectionContext.proceed();

        } catch(InjectionException ie) {
            throw new IllegalStateException(ie.getMessage(), ie);
        }
    }

    public <T> void registerInjectionTarget(InjectionTarget<T> injectionTarget, AnnotatedType<T> annotatedType) {
        if ( bundleContext instanceof EjbBundleDescriptor ) {
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
            if ( javax.xml.ws.Service.class.isAssignableFrom(annotatedField.getJavaMember().getType())) {
                return;
            }

            if ( annotatedField.getJavaMember().getType().isInterface() ) {
                Class serviceClass = webServiceRef.value();
                if ( serviceClass != null ) {
                    if ( ! javax.xml.ws.Service.class.isAssignableFrom(serviceClass)) {
                        throw new DefinitionException( "The type of the injection point " +
                                                       annotatedField.getJavaMember().getName() +
                                                       " is an interface: " +
                                                       annotatedField.getJavaMember().getType().getName() +
                                                       ".  The @WebSreviceRef value of " +
                                                       serviceClass +
                                                       " is not assignable from " +
                                                       javax.xml.ws.Service.class.getName());
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



    public void cleanup() {

    }
}
