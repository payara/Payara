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

package org.glassfish.weld;

import org.glassfish.hk2.api.ServiceLocator;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import org.glassfish.api.naming.GlassfishNamingManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;
import java.util.Locale;


public class InjectionPointHelper {

    private final ServiceLocator services;
    private final ComponentEnvManager compEnvManager;
    private final GlassfishNamingManager namingManager;

    public InjectionPointHelper(ServiceLocator h) {
        services = h;

        compEnvManager = services.getService(ComponentEnvManager.class);
        namingManager = services.getService(GlassfishNamingManager.class);

    }

    public Object resolveInjectionPoint(java.lang.reflect.Member member, Application app)
        throws javax.naming.NamingException {

        if ( member == null ) {
          throw new IllegalArgumentException("Member cannot be null.");
        }

        if ( app == null ) {
          throw new IllegalArgumentException("Application cannot be null.");
        }

        Object result = null;

        Field field = null;
        Method method = null;
        Annotation[] annotations;

        if( member instanceof Field ) {
            field = (Field) member;
            annotations = field.getDeclaredAnnotations();
        } else if( member instanceof Method ) {
            method = (Method) member;
            annotations = method.getDeclaredAnnotations();
        } else {
            throw new IllegalArgumentException("Member must be Field or Method");
        }

        Annotation envAnnotation = getEnvAnnotation(annotations);

        if( envAnnotation == null ) {
            throw new IllegalArgumentException("No Java EE env dependency annotation found on " +
                   member);
        }

        String envAnnotationName = null;
        try {
            Method m = envAnnotation.annotationType().getDeclaredMethod("name");
            envAnnotationName = (String) m.invoke(envAnnotation);
        } catch(Exception e) {
            throw new IllegalArgumentException("Invalid annotation : must have name() attribute " +
                           envAnnotation.toString(), e);
        }

        String envDependencyName = envAnnotationName;
        Class declaringClass = member.getDeclaringClass();

        if( (envAnnotationName == null) || envAnnotationName.equals("") ) {
            if( field != null ) {
                envDependencyName = declaringClass.getName() + "/" + field.getName();
            } else {
                envDependencyName = declaringClass.getName() + "/" +
                        getInjectionMethodPropertyName(method);
            }
        }

        if( envAnnotationName != null && envAnnotationName.startsWith("java:global/") ) {
            javax.naming.Context ic = namingManager.getInitialContext();
            result = ic.lookup(envAnnotationName);
        } else {
            BundleDescriptor matchingBundle = null;

            for(BundleDescriptor bundle : app.getBundleDescriptors()) {

                if( (bundle instanceof EjbBundleDescriptor) ||
                    (bundle instanceof WebBundleDescriptor) ) {

                    JndiNameEnvironment jndiEnv = (JndiNameEnvironment) bundle;

                    // TODO normalize for java:comp/env/ prefix
                    for(InjectionCapable next :
                            jndiEnv.getInjectableResourcesByClass(declaringClass.getName())) {
                        if( next.getComponentEnvName().equals(envDependencyName) ) {
                            matchingBundle = bundle;
                            break;
                        }
                    }
                }

                if( matchingBundle != null ) {
                    break;
                }
            }

            if( matchingBundle == null ) {
                throw new IllegalArgumentException("Cannot find matching env dependency for " +
                  member + " in Application " + app.getAppName());
            }

            String componentId = compEnvManager.getComponentEnvId((JndiNameEnvironment)matchingBundle);
            String lookupName = envDependencyName.startsWith("java:") ?
                    envDependencyName : "java:comp/env/" + envDependencyName;
            result = namingManager.lookup(componentId, lookupName);
        }

        return result;

    }

    private String getInjectionMethodPropertyName(Method method)
    {
        String methodName = method.getName();
        String propertyName;

        if( (methodName.length() > 3) &&
            methodName.startsWith("set") ) {
            // Derive javabean property name.
            propertyName =
                methodName.substring(3, 4).toLowerCase( Locale.ENGLISH ) +
                methodName.substring(4);
        } else {
           throw new IllegalArgumentException("Illegal env dependency setter name" +
            method.getName());
        }

        return propertyName;
    }


    private Annotation getEnvAnnotation(Annotation[] annotations) {

        Annotation envAnnotation = null;

        for(Annotation next : annotations) {

            String className = next.annotationType().getName();
            if( className.equals("javax.ejb.EJB") ||
                className.equals("javax.annotation.Resource") ||
                className.equals("javax.persistence.PersistenceContext") ||
                className.equals("javax.persistence.PersistenceUnit") ||
                className.equals("javax.xml.ws.WebServiceRef") ) {
                envAnnotation = next;
                break;
            }
        }

        return envAnnotation;

    }
}
