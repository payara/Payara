/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.opentracing.cdi;

import static java.util.Arrays.asList;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.InvocationContext;
import javax.ws.rs.container.ResourceInfo;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.weld.interceptor.WeldInvocationContext;

/**
 * Utilities class for various CDI-based operations used by the OpenTracing Service classes.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 * @author Arjan Tijms <arjan.tijms@payara.fish>
 */
public class OpenTracingCdiUtils {

    private static final Logger logger = Logger.getLogger(OpenTracingCdiUtils.class.getName());

    /**
     * Gets the annotation from the method that triggered the interceptor.
     *
     * @param <A> The annotation type to return
     * @param beanManager The invoking interceptor's BeanManager
     * @param annotationClass The class of the annotation to get
     * @param invocationContext The context of the method invocation
     * @return The annotation that triggered the interceptor.
     */
    public static <A extends Annotation> A getAnnotation(BeanManager beanManager, Class<A> annotationClass, InvocationContext invocationContext) {
        A annotation = getInterceptedAnnotation(annotationClass, invocationContext);
        
        if (annotation == null) {
            annotation = getAnnotation(beanManager, annotationClass, invocationContext.getMethod().getDeclaringClass(), invocationContext.getMethod());
        }

        return annotation;
    }
    
    /**
     * Gets the annotation from the method that triggered the interceptor.
     *
     * @param <A> The annotation type to return
     * @param beanManager The invoking interceptor's BeanManager
     * @param annotationClass The class of the annotation to get
     * @param resourceInfo The targeted jaxrs resource
     * @return The annotation that triggered the interceptor.
     */
    public static <A extends Annotation> A getAnnotation(BeanManager beanManager, Class<A> annotationClass, ResourceInfo resourceInfo) {
        return getAnnotation(
            beanManager, annotationClass, 
            resourceInfo.getResourceClass(), 
            resourceInfo.getResourceMethod());
    }
    
    public static <A extends Annotation> A getAnnotation(BeanManager beanManager, Class<A> annotationClass, Class<?> annotatedClass, Method method) {
        logGetAnnotation(annotationClass, method);
        
        // Try to get the annotation from the method, otherwise attempt to get it from the class
        if (method.isAnnotationPresent(annotationClass)) {
            logger.log(FINER, "Annotation was directly present on the method");
            
            return method.getAnnotation(annotationClass);
        }
        
        if (annotatedClass.isAnnotationPresent(annotationClass)) {
            logger.log(FINER, "Annotation was directly present on the class");
            
            return annotatedClass.getAnnotation(annotationClass);
        }
        
        logger.log(FINER, "Annotation wasn't directly present on the method or class, checking stereotypes");

        // Account for Stereotypes
        Queue<Annotation> annotations = new LinkedList<>(asList(annotatedClass.getAnnotations()));

        A annotation = null;
        
        // Loop over each individual annotation
        while (!annotations.isEmpty()) {
            Annotation a = annotations.remove();

            // Check if this is the annotation we're looking for
            if (a.annotationType().equals(annotationClass)) {
                logger.log(FINER, "Annotation was found in a stereotype");
                annotation = annotationClass.cast(a);
                break;
            }

            // If the found annotation is a stereotype, get the individual annotations and add them to the list
            // to be iterated over
            if (beanManager.isStereotype(a.annotationType())) {
                annotations.addAll(beanManager.getStereotypeDefinition(a.annotationType()));
            }
        }

        return annotation;
    }
    
    /**
     * Gets overriding config parameter values if they're present from an invocation context.
     *
     * @param <A> The annotation type
     * @param annotationClass The annotation class
     * @param parameterName The name of the parameter to get the override value of
     * @param invocationContext The context of the invoking request
     * @param parameterType The type of the parameter to get the override value of
     * @return An Optional containing the override value from the config if there is one
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> Optional<Object> getConfigOverrideValue(Class<A> annotationClass, String parameterName, InvocationContext invocationContext, Class<?> parameterType) {
        return  (Optional<Object>) getConfigOverrideValue(annotationClass, parameterName, invocationContext.getMethod(), parameterType);
    }

    /**
     * Gets overriding config parameter values if they're present from an invocation context.
     *
     * @param <A> The annotation type
     * @param annotationClass The annotation class
     * @param parameterName The name of the parameter to get the override value of
     * @param method The method to be invoked
     * @param parameterType The type of the parameter to get the override value of
     * @return An Optional containing the override value from the config if there is one
     */
    public static <A extends Annotation> Optional<?> getConfigOverrideValue(Class<A> annotationClass, String parameterName, Method method, Class<?> parameterType) {
        Optional<?> value = Optional.empty();

        Config config = getConfig();
        if (config == null) {
            logger.log(FINE, "No config to get override parameters from.");
            return value;
        }

        // Get the annotation, method, and class names
        String annotationName = annotationClass.getSimpleName();
        String annotatedMethodName = method.getName();
        String annotatedClassCanonicalName = method.getDeclaringClass().getCanonicalName();

        // Check if there's a config override for the method
        logger.log(FINER, "Getting config override for annotated method...");
       
        value = config.getOptionalValue(
                    annotatedClassCanonicalName + "/" + 
                    annotatedMethodName + "/" + 
                    annotationName + "/" + parameterName, 
                    parameterType);

        // If there wasn't a config override for the method, check if the method has the annotation
        if (!value.isPresent()) {
            logger.log(FINER, "No config override for annotated method, checking if the method is " + "annotated directly...");
            
            // If the method is annotated directly, simply return
            if (method.getAnnotation(annotationClass) != null) {
                logger.log(FINER, "Method is annotated directly, returning.");
                return value;
            }

            // If there wasn't a config override for the method, check if there's one for the class
            if (!value.isPresent()) {
                logger.log(FINER, "No config override for annotated method, getting config override for the " + "annotated class...");
                value = config.getOptionalValue(annotatedClassCanonicalName + "/" + annotationName + "/" + parameterName, parameterType);

                // If there wasn't a config override for the class either, check if there's a global one
                if (!value.isPresent()) {
                    logger.log(FINER, "No config override for the annotated class, getting application wide " + "config override...");
                    value = config.getOptionalValue(annotationName + "/" + parameterName, parameterType);

                    if (!value.isPresent()) {
                        logger.log(FINER, "No config overrides");
                    }
                }
            }
        }

        return value;
    }

    /**
     * Gets overriding config parameter values if they're present from an invocation context.
     *
     * @param <A> The annotation type
     * @param annotationClass The annotation class
     * @param parameterName The name of the parameter to get the override value of
     * @param resourceInfo The targeted jaxrs resource
     * @param parameterType The type of the parameter to get the override value of
     * @return An Optional containing the override value from the config if there is one
     */
    public static <A extends Annotation> Optional getConfigOverrideValue(Class<A> annotationClass,
            String parameterName, ResourceInfo resourceInfo, Class parameterType) {
        Optional value = Optional.empty();

        Config config = getConfig();

        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }

        // Get the annotation, method, and class names
        String annotationName = annotationClass.getSimpleName();
        String annotatedMethodName = resourceInfo.getResourceMethod().getName();
        String annotatedClassCanonicalName = resourceInfo.getResourceClass().getCanonicalName();

        // Check if there's a config override for the method
        if (config != null) {
            logger.log(FINER, "Getting config override for annotated method...");
            value = config.getOptionalValue(annotatedClassCanonicalName + "/" + annotatedMethodName
                    + "/" + annotationName + "/" + parameterName, parameterType);

            // If there wasn't a config override for the method, check if the method has the annotation
            if (!value.isPresent()) {
                logger.log(FINER, "No config override for annotated method, checking if the method is "
                        + "annotated directly...");
                // If the method is annotated directly, simply return
                if (resourceInfo.getResourceMethod().getAnnotation(annotationClass) != null) {
                    logger.log(FINER, "Method is annotated directly, returning.");
                    return value;
                }

                // If there wasn't a config override for the method, check if there's one for the class
                if (!value.isPresent()) {
                    logger.log(FINER, "No config override for annotated method, getting config override for the "
                            + "annotated class...");
                    value = config.getOptionalValue(annotatedClassCanonicalName + "/" + annotationName
                            + "/" + parameterName, parameterType);

                    // If there wasn't a config override for the class either, check if there's a global one
                    if (!value.isPresent()) {
                        logger.log(FINER, "No config override for the annotated class, getting application wide "
                                + "config override...");
                        value = config.getOptionalValue(annotationName + "/" + parameterName, parameterType);

                        if (!value.isPresent()) {
                            logger.log(FINER, "No config overrides");
                        }
                    }
                }
            }
        } else {
            logger.log(FINE, "No config to get override parameters from.");
        }

        return value;
    }

    /**
     * Uses Weld to find an annotation. The annotation must be an intercepted annotation that has a binding
     * @param <A>
     * @param annotationClass {@link Annotation} to look for
     * @param invocationContext
     * @return the {@link Annotation} that the interceptor is inceptoring,or null if it cannot be found
     */
    private static <A extends Annotation> A getInterceptedAnnotation(Class<A> annotationClass, InvocationContext invocationContext){
         if (invocationContext instanceof WeldInvocationContext) {
            Set<Annotation> interceptorBindings = ((WeldInvocationContext) invocationContext).getInterceptorBindings();
            for (Annotation annotationBound : interceptorBindings) {
                if (annotationBound.annotationType().equals(annotationClass)) {
                    return (A) annotationBound;
                }
            }
        }
        return null;
    }
    
    private static Config getConfig() {
        try {
            return ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }
        
        return null;
    }
    
    private static void logGetAnnotation(Class<?> annotatedClass, Method method) {
        logger.log(FINER, "Attempting to get annotation {0} from {1}",
                new String[]{annotatedClass.getSimpleName(), method.getName()});
    }

}
