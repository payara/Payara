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

import com.sun.enterprise.deployment.*;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.ejb.api.EjbContainerServices;
import org.glassfish.internal.api.Globals;
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
import javax.naming.InitialContext;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.WebServiceRef;
import java.util.List;


public class InjectionServicesImpl implements InjectionServices {

    private InjectionManager injectionManager;

    // Associated bundle context
    private BundleDescriptor bundleContext;

    public InjectionServicesImpl(InjectionManager injectionMgr, BundleDescriptor context) {
        injectionManager = injectionMgr;
        bundleContext = context;
    }

    public <T> void aroundInject(InjectionContext<T> injectionContext) {
        try {
            ServiceLocator serviceLocator = Globals.getDefaultHabitat();
            ComponentEnvManager compEnvManager = serviceLocator.getService(ComponentEnvManager.class);
            EjbContainerServices containerServices = serviceLocator.getService(EjbContainerServices.class);

            JndiNameEnvironment componentEnv = compEnvManager.getCurrentJndiNameEnvironment();

            ManagedBeanDescriptor mbDesc = null;

            JndiNameEnvironment injectionEnv = (JndiNameEnvironment) bundleContext;
            
            Object target = injectionContext.getTarget();
            String targetClass = target.getClass().getName();

            if( componentEnv == null ) {
                //throw new IllegalStateException("No valid EE environment for injection of " + targetClass);
                System.err.println("No valid EE environment for injection of " + targetClass);
                injectionContext.proceed();
                return; 
            }

            // Perform EE-style injection on the target.  Skip PostConstruct since
            // in this case 299 impl is responsible for calling it.

            if( componentEnv instanceof EjbDescriptor ) {

                EjbDescriptor ejbDesc = (EjbDescriptor) componentEnv;

                if( containerServices.isEjbManagedObject(ejbDesc, target.getClass())) {
                    injectionEnv = componentEnv;
                } else {

                    if( bundleContext instanceof EjbBundleDescriptor ) {

                        // Check if it's a @ManagedBean class within an ejb-jar.  In that case,
                        // special handling is needed to locate the EE env dependencies
                        mbDesc = bundleContext.getManagedBeanByBeanClass(targetClass);
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
                    injectionManager.injectInstance(target, compEnvManager.getComponentEnvId(injectionEnv)
                        ,false);

                } else {
                    injectionManager.injectInstance(target, injectionEnv, false);
                }
            }

            injectionContext.proceed();

        } catch(InjectionException ie) {
            throw new IllegalStateException(ie.getMessage(), ie);
        }
    }

    public <T> void registerInjectionTarget(InjectionTarget<T> injectionTarget, AnnotatedType<T> annotatedType) {
        // We are only validating producer fields of resources.  See spec section 3.7.1
        // Not doing this for ejbs either.


        Class annotatedClass = annotatedType.getJavaClass();
        JndiNameEnvironment jndiNameEnvironment = (JndiNameEnvironment) bundleContext;
        ServiceLocator serviceLocator = Globals.getDefaultHabitat();
        InvocationManager invocationManager = serviceLocator.getService(InvocationManager.class);
        ComponentEnvManager compEnvManager = serviceLocator.getService(ComponentEnvManager.class);
        String componentId = compEnvManager.getComponentEnvId(jndiNameEnvironment);
        String appName = bundleContext.getApplication().getAppName();
        String moduleName = bundleContext.getModuleName();

        ComponentInvocation componentInvocation = null;

        InjectionInfo injectionInfo = jndiNameEnvironment.getInjectionInfoByClass(annotatedClass);
        List<InjectionCapable> injectionResources = injectionInfo.getInjectionResources();
        if ( injectionResources.size() > 0 ) {
        }

        boolean lookupsWillWork = true;
        for (AnnotatedField<? super T> annotatedField : annotatedType.getFields()) {
            if ( lookupsWillWork && annotatedField.isAnnotationPresent( Produces.class ) ) {
                if ( componentInvocation == null ) {
                    componentInvocation = createComponentInvocation( componentId,
                                                                     appName,
                                                                     moduleName);
                    try {
                        invocationManager.preInvoke(componentInvocation);
                    } catch ( Exception preInvokeException ) {
                        lookupsWillWork = false;
                    }
                }

                if ( lookupsWillWork ) {
                    String lookupName = getLookupName( annotatedClass,
                                                       annotatedField,
                                                       injectionResources );
                    Object resource = getResource( lookupName );
                    if ( resource == null ) {
                        // we have to retry using the component environment name because it could be a env entry with a
                        // name specified in the annotation but getLookupName returned that instead of the
                        // field's component env entry name...convoluted but necessary
                        lookupName = getComponentEnvName( annotatedClass,
                                                          annotatedField.getJavaMember().getName(),
                                                          injectionResources );
                        resource = getResource( lookupName );
                    }
                    if ( resource != null ) {
                        validateResource( annotatedField, resource);
                    }
                }
            }
        }

        if ( componentInvocation != null ) {
            try {
                invocationManager.postInvoke(componentInvocation);
            } catch ( Exception ignore ) {
            }
        }
    }

    private void validateResource( AnnotatedField annotatedField, Object resource) {
        if ( ! annotatedField.getJavaMember().getType().isAssignableFrom( resource.getClass() ) ) {
            throw new DefinitionException( "The type of the injection point " +
                                               annotatedField.getJavaMember().getName() +
                                               "is " +
                                               annotatedField.getJavaMember().getType().getName() +
                                               ".  The type of the physical resource is " +
                                               resource.getClass().getName() +
                                               " They are incompatible. ");
        }
    }


    private Object getResource( String lookupName ) {
        try {
            InitialContext initialContext = new InitialContext();
            return initialContext.lookup( lookupName );
        } catch ( Exception ignore ) {
        }

        return null;
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

    private ComponentInvocation createComponentInvocation( String componentId,
                                                           String appName,
                                                           String moduleName) {
        ComponentInvocation.ComponentInvocationType componentInvocationType =
            ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION;

        ComponentInvocation componentInvocation =
            new ComponentInvocation( componentId,
                                     componentInvocationType,
                                     null,
                                     appName,
                                     moduleName);
        componentInvocation.setJNDIEnvironment( bundleContext );
        return componentInvocation;
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
