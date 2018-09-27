/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.cdi;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.InvocationContext;
import org.eclipse.microprofile.config.Config;

/**
 * Utility Methods for the Fault Tolerance Interceptors.
 * @author Andrew Pielage
 */
public class FaultToleranceCdiUtils {
    
    private static final Logger logger = Logger.getLogger(FaultToleranceCdiUtils.class.getName());
    
    /**
     * Gets the annotation from the method that triggered the interceptor.
     * @param <A> The annotation type to return
     * @param beanManager The invoking interceptor's BeanManager
     * @param annotationClass The class of the annotation to get
     * @param invocationContext The context of the method invocation
     * @return The annotation that triggered the interceptor.
     */
    public static <A extends Annotation> A getAnnotation(BeanManager beanManager, Class<A> annotationClass, 
            InvocationContext invocationContext) {
        A annotation = null;
        Class<?> annotatedClass = getAnnotatedMethodClass(invocationContext, annotationClass);
        
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
    
    /**
     * Gets overriding config parameter values if they're present.
     * @param <A> The annotation type
     * @param config The config to get the overriding parameter values from
     * @param annotationClass The annotation class
     * @param parameterName The name of the parameter to get the override value of
     * @param annotatedMethodName The annotated method name
     * @param annotatedClassCanonicalName The canonical name of the annotated method class
     * @param parameterType The type of the parameter to get the override value of
     * @return 
     */
    public static <A extends Annotation> Optional getOverrideValue(Config config, Class<A> annotationClass, 
            String parameterName, String annotatedMethodName, String annotatedClassCanonicalName, Class parameterType) {
        Optional value = Optional.empty();
        
        String annotationName = annotationClass.getSimpleName();
        
        // Check if there's a config override for the method
        if (config != null) {
            logger.log(Level.FINER, "Getting config override for annotated method...");
            value = config.getOptionalValue(annotatedClassCanonicalName + "/" + annotatedMethodName 
                    + "/" + annotationName + "/" + parameterName, parameterType);
                
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
        } else {
            logger.log(Level.FINE, "No config to get override parameters from.");
        }
        
        return value;
    }
    
    /**
     * Gets overriding config parameter values if they're present from an invocation context.
     * @param <A> The annotation type
     * @param config The config to get the overriding parameter values from
     * @param annotationClass The annotation class
     * @param parameterName The name of the parameter to get the override value of
     * @param invocationContext The context of the invoking request
     * @param parameterType The type of the parameter to get the override value of
     * @return 
     */
    public static <A extends Annotation> Optional getOverrideValue(Config config, Class<A> annotationClass, 
            String parameterName, InvocationContext invocationContext, Class parameterType) {
        Optional value = Optional.empty();
        
        // Get the annotation, method, and class names
        String annotationName = annotationClass.getSimpleName();
        String annotatedMethodName = invocationContext.getMethod().getName();
        String annotatedClassCanonicalName = getAnnotatedMethodClassCanonicalName(
                getAnnotatedMethodClass(invocationContext, annotationClass));
        
        // Check if there's a config override
        if (config != null) {
            logger.log(Level.FINER, "Getting config override for annotated method...");
            
            // Check if there's a config override for this specific method
            value = config.getOptionalValue(annotatedClassCanonicalName + "/" 
                    + annotatedMethodName + "/" + annotationName + "/" + parameterName, parameterType);
            
            if (!value.isPresent()) {
                logger.log(Level.FINER, "No config override for annotated method, checking if the method is "
                        + "annotated directly...");
                // If the method is annotated directly, check for a global level override and return
                if (invocationContext.getMethod().getAnnotation(annotationClass) != null) {
                    logger.log(Level.FINER, "Method is annotated directly, checking for global override...");
                    // The only thing that should override a method-level annotation is a method or global-level config
                    return checkForGlobalLevelOverride(annotationName, parameterName, parameterType, config);
                }
                
                // If the method wasn't annotated directly, check if there's an override for the class
                if (!value.isPresent()) {
                    value = checkForClassLevelOverride(annotatedClassCanonicalName, annotationName, parameterName, 
                            parameterType, config);
                    
                    // If there wasn't a config override for the class, check if there's a global one
                    if (!value.isPresent()) {
                        value = checkForGlobalLevelOverride(annotationName, parameterName, parameterType, config);
                    }
                }
            }
        } else {
            logger.log(Level.FINE, "No config to get override parameters from.");
        }
        
        return value;
    }
    
    /**
     * Gets overriding config enabled parameter value if it's present from an invocation context. 
     * This follows a different priority logic than other parameter overrides.
     * 
     * @param <A> The annotation type
     * @param config The config to get the overriding enabled value from
     * @param annotationClass The annotation class
     * @param invocationContext The context of the invoking request
     * @return 
     */
    public static <A extends Annotation> Optional getEnabledOverrideValue(Config config, Class<A> annotationClass,
            InvocationContext invocationContext) {
        Optional value = Optional.empty();
        
        // Get the annotation, method, and class names
        String annotationName = annotationClass.getSimpleName();
        String annotatedMethodName = invocationContext.getMethod().getName();
        String annotatedClassCanonicalName = getAnnotatedMethodClassCanonicalName(
                getAnnotatedMethodClass(invocationContext, annotationClass));
        
        // Check if there's a config override
        if (config != null) {
            logger.log(Level.FINER, "Getting config override for annotated method...");
            
            // Check if there's a config override for this specific method
            value = config.getOptionalValue(annotatedClassCanonicalName + "/" 
                    + annotatedMethodName + "/" + annotationName + "/" + "enabled", Boolean.class);
            
            if (!value.isPresent()) {
                logger.log(Level.FINER, "No config override for annotated method, checking if the method is "
                        + "annotated directly...");
                // If the method is annotated directly, check for a cless or global-level override
                if (invocationContext.getMethod().getAnnotation(annotationClass) != null) {
                    logger.log(Level.FINER, "Method is annotated directly, checking for class or global override...");
                    checkAnnotatedMethodForEnabledOverride(annotatedClassCanonicalName, annotationName, config);
                }
                
                // If the method wasn't annotated directly, check if there's an override for the class
                if (!value.isPresent()) {
                    value = checkForClassLevelOverride(annotatedClassCanonicalName, annotationName, "enabled", 
                            Boolean.class, config);
                    
                    // If there wasn't a config override for the class, check if there's a global one
                    if (!value.isPresent()) {
                        value = checkForGlobalLevelOverride(annotationName, "enabled", Boolean.class, config);
                    }
                }
            }
        } else {
            logger.log(Level.FINE, "No config to get override parameters from.");
        }
        
        return value;
    }
    
    /**
     * Helper method that logs whether an override was found.
     * @param value The Optional value to check if an override was found for
     * @param level A String denoting the level of override you were checking for
     */
    private static void logOverride(Optional value, String level) {
        if (value.isPresent()) {
            logger.log(Level.FINER, "{0} override found.", level);
        } else {
            logger.log(Level.FINER, "No config overrides.");
        }
    }
    
    /**
     * Helper method that checks if a directly annotated method has a class or global-level override for the enabled 
     * parameter. This gets its own method as the enabled parameter has a different override priority.
     * @param annotatedClassCanonicalName The canonical name of the annotated class
     * @param annotationName The name of the annotation
     * @param config The config to get the overriding enabled value from
     * @return An overriding enabled value if there is one
     */
    private static Optional checkAnnotatedMethodForEnabledOverride(String annotatedClassCanonicalName, 
            String annotationName, Config config) {
        Optional value = checkForClassLevelOverride(annotatedClassCanonicalName, annotationName, "enabled", 
                Boolean.class, config);
        
        if (!value.isPresent()) {
            checkForGlobalLevelOverride(annotationName,  "enabled", Boolean.class, config);
        }
        
        return value;
    }
    
    /**
     * Helper method that checks if there is an override for a parameter at the class level.
     * @param annotatedClassCanonicalName The canonical name of the annotated class
     * @param annotationName The name of the annotation
     * @param parameterName The name of the parameter
     * @param parameterType The parameter class
     * @param config The config to get the overriding value from
     * @return An overriding value for the provided parameter if there is one
     */
    private static Optional checkForClassLevelOverride(String annotatedClassCanonicalName, String annotationName, 
            String parameterName, Class parameterType, Config config) {
        logger.log(Level.FINER, "No config override for annotated method, getting config override for the "
                + "annotated class...");
        Optional value = config.getOptionalValue(annotatedClassCanonicalName + "/" + annotationName 
                + "/" + parameterName, parameterType);
        logOverride(value, "Class-level");
        
        return value;
    }
    
    /**
     * Helper method that checks if there is an override for a parameter at the global level.
     * @param annotationName The name of the annotation
     * @param parameterName The name of the parameter
     * @param parameterType The parameter class
     * @param config The config to get the overriding value from
     * @return 
     */
    private static Optional checkForGlobalLevelOverride(String annotationName, String parameterName, Class parameterType, 
            Config config) {
        logger.log(Level.FINER, "No config override for annotated class, checking for global override.");
        Optional value = config.getOptionalValue(annotationName + "/" + parameterName, parameterType);
        logOverride(value, "Global");
        
        return value;
    }
    
    /**
     * Returns either the CDI Proxy class (for cases where a class extends another without overriding the target method), 
     * or the actual declaring class if the annotation isn't present on the proxy class.
     * @param <A> The class of the annotation
     * @param invocationContext The context of the method invocation
     * @param annotationClass The class of the annotation
     * @return  The class of the annotated method
     */
    public static <A extends Annotation> Class getAnnotatedMethodClass(InvocationContext invocationContext, 
            Class<A> annotationClass) {
        Class targetClass = invocationContext.getTarget().getClass();
        
        if (targetClass.isAnnotationPresent(annotationClass)) {
            return targetClass;
        } else {
            return invocationContext.getMethod().getDeclaringClass();
        }
    }
    
    /**
     * Helper method that gets the canonical name of an annotated class. This is used to strip out any Weld proxy
     * naming that gets appended to the real class name.
     * @param <A> The class of the annotation
     * @param annotatedClass The class of the annotation
     * @return The canonical name of the annotated method's class
     */
    public static <A extends Annotation> String getAnnotatedMethodClassCanonicalName(Class annotatedClass) {
        String canonicalClassName = annotatedClass.getCanonicalName();
        
        // If the class was a proxy from Weld, cut away the appended gubbins
        // Author Note: There's probably a better way of doing this...
        if (canonicalClassName.contains("$Proxy$_$$_WeldSubclass")) {
            return canonicalClassName.split("\\$Proxy\\$_\\$\\$_WeldSubclass")[0];
        } else {
            return canonicalClassName;
        }
    }
    
    /**
     * Gets the full method signature from an invocation context, stripping out any Weld proxy name gubbins.
     * @param <A> The class of the annotation
     * @param invocationContext The context of the method invocation
     * @param annotationClass The class of the annotation
     * @return 
     */
    public static <A extends Annotation> String getFullAnnotatedMethodSignature(InvocationContext invocationContext,
            Class<A> annotationClass) {
        return getAnnotatedMethodClassCanonicalName(getAnnotatedMethodClass(invocationContext, annotationClass)) + "." 
                + invocationContext.getMethod().getName();
    }
}
