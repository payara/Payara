/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.InvocationContext;
import javax.ws.rs.container.ResourceInfo;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class OpenTracingCdiUtils {
    
    private static final Logger logger = Logger.getLogger(OpenTracingCdiUtils.class.getName());
    
    public static <A extends Annotation> A getAnnotation(BeanManager beanManager, Class<A> annotationClass, 
            InvocationContext invocationContext) {
        A annotation = null;
        Class<?> annotatedClass = invocationContext.getMethod().getDeclaringClass();
        
        logger.log(Level.FINER, "Attempting to get annotation {0} from {1}", 
                new String[]{annotationClass.getSimpleName(), invocationContext.getMethod().getName()});
        // Try to get the annotation from the method, otherwise attempt to get it from the class
        if (invocationContext.getMethod().isAnnotationPresent(annotationClass)) {
            logger.log(Level.FINER, "Annotation was directly present on the method");
            annotation = invocationContext.getMethod().getAnnotation(annotationClass);
        } else {
            if (annotatedClass.isAnnotationPresent(annotationClass)) {
                logger.log(Level.FINER, "Annotation was directly present on the class");
                annotation = annotatedClass.getAnnotation(annotationClass);
            } else {
                logger.log(Level.FINER, "Annotation wasn't directly present on the method or class, "
                        + "checking stereotypes");
                // Account for Stereotypes
                Queue<Annotation> annotations = new LinkedList<>(Arrays.asList(annotatedClass.getAnnotations()));

                while (!annotations.isEmpty()) {
                    Annotation a = annotations.remove();

                    if (a.annotationType().equals(annotationClass)) {
                        logger.log(Level.FINER, "Annotation was found in a stereotype");
                        annotation = annotationClass.cast(a);
                        break;
                    }

                    if (beanManager.isStereotype(a.annotationType())) {
                        annotations.addAll(beanManager.getStereotypeDefinition(a.annotationType()));
                    }
                }
            }
        }

        return annotation;
    }
    
    public static <A extends Annotation> A getAnnotation(BeanManager beanManager, Class<A> annotationClass, 
            ResourceInfo resourceInfo) {
        A annotation = null;
        Class<?> annotatedClass = resourceInfo.getResourceClass();
        
        logger.log(Level.FINER, "Attempting to get annotation {0} from {1}", 
                new String[]{annotationClass.getSimpleName(), resourceInfo.getResourceMethod().getName()});
        // Try to get the annotation from the method, otherwise attempt to get it from the class
        if (resourceInfo.getResourceMethod().isAnnotationPresent(annotationClass)) {
            logger.log(Level.FINER, "Annotation was directly present on the method");
            annotation = resourceInfo.getResourceMethod().getAnnotation(annotationClass);
        } else {
            if (annotatedClass.isAnnotationPresent(annotationClass)) {
                logger.log(Level.FINER, "Annotation was directly present on the class");
                annotation = annotatedClass.getAnnotation(annotationClass);
            } else {
                logger.log(Level.FINER, "Annotation wasn't directly present on the method or class, "
                        + "checking stereotypes");
                // Account for Stereotypes
                Queue<Annotation> annotations = new LinkedList<>(Arrays.asList(annotatedClass.getAnnotations()));

                while (!annotations.isEmpty()) {
                    Annotation a = annotations.remove();

                    if (a.annotationType().equals(annotationClass)) {
                        logger.log(Level.FINER, "Annotation was found in a stereotype");
                        annotation = annotationClass.cast(a);
                        break;
                    }

                    if (beanManager.isStereotype(a.annotationType())) {
                        annotations.addAll(beanManager.getStereotypeDefinition(a.annotationType()));
                    }
                }
            }
        }

        return annotation;
    }
    
    /**
     * Gets overriding config parameter values if they're present from an invocation context.
     * @param <A> The annotation type
     * @param annotationClass The annotation class
     * @param parameterName The name of the parameter to get the override value of
     * @param invocationContext The context of the invoking request
     * @param parameterType The type of the parameter to get the override value of
     * @return 
     */
    public static <A extends Annotation> Optional getConfigOverrideValue(Class<A> annotationClass, 
            String parameterName, InvocationContext invocationContext, Class parameterType) {
        Optional value = Optional.empty();
        
        Config config = null;

        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(Level.INFO, "No config could be found", ex);
        }
        
        // Get the annotation, method, and class names
        String annotationName = annotationClass.getSimpleName();
        String annotatedMethodName = invocationContext.getMethod().getName();
        String annotatedClassCanonicalName = invocationContext.getMethod().getDeclaringClass().getCanonicalName();
        
        // Check if there's a config override for the method
        if (config != null) {
            logger.log(Level.FINER, "Getting config override for annotated method...");
            value = config.getOptionalValue(annotatedClassCanonicalName + "/" + annotatedMethodName 
                    + "/" + annotationName + "/" + parameterName, parameterType);
            
            // If there wasn't a config override for the method, check if the method has the annotation
            if (!value.isPresent()) {
                logger.log(Level.FINER, "No config override for annotated method, checking if the method is "
                        + "annotated directly...");
                // If the method is annotated directly, simply return
                if (invocationContext.getMethod().getAnnotation(annotationClass) != null) {
                    logger.log(Level.FINER, "Method is annotated directly, returning.");
                    return value;
                }
                
                // If there wasn't a config override for the method, check if there's one for the class
                if (!value.isPresent()) {
                    logger.log(Level.FINER, "No config override for annotated method, getting config override for the "
                        + "annotated class...");
                    value = config.getOptionalValue(annotatedClassCanonicalName + "/" + annotationName 
                            + "/" + parameterName, parameterType);

                    // If there wasn't a config override for the class either, check if there's a global one
                    if (!value.isPresent()) {
                        logger.log(Level.FINER, "No config override for the annotated class, getting application wide "
                            + "config override...");
                        value = config.getOptionalValue(annotationName + "/" + parameterName, parameterType);
                        
                        if (!value.isPresent()) {
                            logger.log(Level.FINER, "No config overrides");
                        }
                    }
                }
            } 
        } else {
            logger.log(Level.FINE, "No config to get override parameters from.");
        }
        
        return value;
    }
    
    public static <A extends Annotation> Optional getConfigOverrideValue(Class<A> annotationClass, 
            String parameterName, ResourceInfo resourceInfo, Class parameterType) {
        Optional value = Optional.empty();
        
        Config config = null;

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
            logger.log(Level.FINER, "Getting config override for annotated method...");
            value = config.getOptionalValue(annotatedClassCanonicalName + "/" + annotatedMethodName 
                    + "/" + annotationName + "/" + parameterName, parameterType);
            
            // If there wasn't a config override for the method, check if the method has the annotation
            if (!value.isPresent()) {
                logger.log(Level.FINER, "No config override for annotated method, checking if the method is "
                        + "annotated directly...");
                // If the method is annotated directly, simply return
                if (resourceInfo.getResourceMethod().getAnnotation(annotationClass) != null) {
                    logger.log(Level.FINER, "Method is annotated directly, returning.");
                    return value;
                }
                
                // If there wasn't a config override for the method, check if there's one for the class
                if (!value.isPresent()) {
                    logger.log(Level.FINER, "No config override for annotated method, getting config override for the "
                        + "annotated class...");
                    value = config.getOptionalValue(annotatedClassCanonicalName + "/" + annotationName 
                            + "/" + parameterName, parameterType);

                    // If there wasn't a config override for the class either, check if there's a global one
                    if (!value.isPresent()) {
                        logger.log(Level.FINER, "No config override for the annotated class, getting application wide "
                            + "config override...");
                        value = config.getOptionalValue(annotationName + "/" + parameterName, parameterType);
                        
                        if (!value.isPresent()) {
                            logger.log(Level.FINER, "No config overrides");
                        }
                    }
                }
            } 
        } else {
            logger.log(Level.FINE, "No config to get override parameters from.");
        }
        
        return value;
    }
}
