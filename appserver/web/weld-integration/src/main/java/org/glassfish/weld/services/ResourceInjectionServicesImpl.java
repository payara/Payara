/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package org.glassfish.weld.services;

import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;

import jakarta.annotation.Resource;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourceInjectionServicesImpl implements ResourceInjectionServices {
    static final String ENV = "java:comp/env";
    private static final Logger LOG = Logger.getLogger(ResourceInjectionServicesImpl.class.getName());


    private final InitialContext namingContext;

    public ResourceInjectionServicesImpl() {
        try {
            this.namingContext = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException("Initial context not available at deployment time", e);
        }
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(InjectionPoint injectionPoint) {
        if (getResourceAnnotation(injectionPoint) == null) {
            throw new IllegalArgumentException("No @Resource annotation found on injection point " + injectionPoint);
        }
        if (injectionPoint.getMember() instanceof Method
                && ((Method) injectionPoint.getMember()).getParameterCount() != 1) {
            throw new IllegalArgumentException(
                    "Resource injection point represents a method which doesn't have exactly one parameter "
                            + injectionPoint);
        }
        return () -> new SimpleResourceReference<>(resolveResource(injectionPoint));
    }

    private Context getContext() {
        return this.namingContext;
    }

    private Resource getResourceAnnotation(InjectionPoint injectionPoint) {
        Annotated annotated = injectionPoint.getAnnotated();
        if (annotated instanceof AnnotatedParameter) {
            // the injection point is parameter, however, it's method that's annotated
            annotated = ((AnnotatedParameter) annotated).getDeclaringCallable();
        }
        return annotated.getAnnotation(Resource.class);
    }

    private Object resolveResource(InjectionPoint injectionPoint) {
        String name = determineResourceName(injectionPoint);
        try {
            return getContext().lookup(name);
        } catch (NamingException e) {
            // Missing resource injections shall be quietly ignored, just like before.
            LOG.log(Level.FINE, e, () -> "Failed to find "+name+" in JNDI");
        }
        return null;
    }

    private String determineResourceName(InjectionPoint injectionPoint) {
        Resource resource = getResourceAnnotation(injectionPoint);
        String lookup = resource.lookup();
        if (!lookup.isEmpty()) {
            return lookup;
        }
        String mappedName = resource.mappedName();
        if (!mappedName.isEmpty()) {
            return mappedName;
        }
        String name = resource.name();
        if (!name.isEmpty()) {
            return ENV + "/" + name;
        }

        String propertyName;
        if (injectionPoint.getMember() instanceof Field) {
            propertyName = injectionPoint.getMember().getName();
        } else if (injectionPoint.getMember() instanceof Method) {
            propertyName = determinePropertyName((Method) injectionPoint.getMember(), injectionPoint);
        } else {
            throw new AssertionError("Unable to inject Resource into " + injectionPoint);
        }

        String className = injectionPoint.getMember().getDeclaringClass().getName();
        return ENV + "/" + className + "/" + propertyName;
    }

    static String determinePropertyName(Method method, InjectionPoint injectionPoint) {
        String methodName = method.getName();
        if (methodName.startsWith("get") && method.getParameterCount() == 0) {
            return Introspector.decapitalize(methodName.substring(3));
        } else if (methodName.startsWith("set") && method.getParameterCount() == 1) {
            return Introspector.decapitalize(methodName.substring(3));
        } else if (methodName.startsWith("is") && method.getParameterCount() == 0) {
            return Introspector.decapitalize(methodName.substring(2));
        } else {
            throw new IllegalArgumentException("Injection point doesn't follow "
                    + "JavaBean conventions (unable to determine property name) " + injectionPoint);
        }
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(String jndiName, String mappedName) {
        return () -> new SimpleResourceReference<>(resolveResource(jndiName, mappedName));
    }

    private Object resolveResource(String jndiName, String mappedName) {
        if (mappedName != null && !mappedName.isEmpty()) {
            return mappedName;
        } else {
            return jndiName;
        }
    }


    @Override
    public void cleanup() {

    }
}
