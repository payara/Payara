/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.microprofile.faulttolerance.service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * Utility Methods for the Fault Tolerance Interceptors.
 * 
 * @author Andrew Pielage
 * @author Jan Bernitt (2.0)
 */
public class FaultToleranceUtils {

    private static final Logger logger = Logger.getLogger(FaultToleranceUtils.class.getName());

    /**
     * Gets the annotation from the method that triggered the interceptor.
     * @param <A> The annotation type to return
     * @param sterotypes The invoking interceptor's context on sterotype annotations
     * @param annotationClass The class of the annotation to get
     * @param context The context of the method invocation
     * @return The annotation that triggered the interceptor.
     */
    public static <A extends Annotation> A getAnnotation(Stereotypes sterotypes, Class<A> annotationClass, 
            InvocationContext context) {
        logger.log(Level.FINER, "Attempting to get annotation {0} from {1}", 
                new String[]{annotationClass.getSimpleName(), context.getMethod().getName()});
        // Try to get the annotation from the method, otherwise attempt to get it from the class
        if (context.getMethod().isAnnotationPresent(annotationClass)) {
            logger.log(Level.FINER, "Annotation was directly present on the method");
            return context.getMethod().getAnnotation(annotationClass);
        }
        Class<?> annotatedClass = getAnnotatedMethodClass(context, annotationClass);
        if (annotatedClass.isAnnotationPresent(annotationClass)) {
            logger.log(Level.FINER, "Annotation was directly present on the class");
            return annotatedClass.getAnnotation(annotationClass);
        }
        if (sterotypes == null) {
            return null;
        }
        logger.log(Level.FINER, "Annotation wasn't directly present on the method or class, checking stereotypes");
        for (Annotation annotation : annotatedClass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == annotationClass) {
                logger.log(Level.FINER, "Annotation was found in a stereotype");
                return annotationClass.cast(annotation);
            }
            // Account for Stereotypes
            if (sterotypes.isStereotype(annotationType)) {
                for (Annotation metaAnnotation : sterotypes.getStereotypeDefinition(annotationType)) {
                    if (metaAnnotation.annotationType() == annotationClass) {
                        logger.log(Level.FINER, "Annotation was found in a stereotype");
                        return annotationClass.cast(metaAnnotation);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets overriding config parameter values if they're present from an invocation context.
     * @param <A> The annotation type
     * @param config The config to get the overriding parameter values from
     * @param annotationType The annotation class
     * @param parameterName The name of the parameter to get the override value of
     * @param context The context of the invoking request
     * @param parameterType The type of the parameter to get the override value of
     * @return 
     */
    public static <A extends Annotation, T> T getOverrideValue(Config config, Class<A> annotationType,
            String parameterName, InvocationContext context, Class<T> parameterType, T defaultValue) {
        return getOverrideValue(config, context, annotationType, parameterName, parameterType, defaultValue, true);
    }

    /**
     * Gets overriding config enabled parameter value if it's present from an invocation context. 
     * This follows a different priority logic than other parameter overrides.
     * 
     * @param <A> The annotation type
     * @param config The config to get the overriding enabled value from
     * @param annotationType The annotation class
     * @param context The context of the invoking request
     * @return 
     */
    public static <A extends Annotation> boolean getEnabledOverrideValue(Config config, Class<A> annotationType,
            InvocationContext context, boolean defaultValue) {
        return getOverrideValue(config, context, annotationType, "enabled", Boolean.class, defaultValue, false);
    }

    private static <T, A extends Annotation> T getOverrideValue(Config config, InvocationContext context,
            Class<A> annotationType, String propertyName, Class<T> resultType, T defaultValue,
            boolean requiresAnnotation) {
        Class<?> targetClass = getAnnotatedMethodClass(context, annotationType);
        String annotationName = annotationType.getSimpleName();
        String methodName = context.getMethod().getName();
        String className = getPlainCanonicalName(targetClass);

        String key = String.format("%s/%s/%s/%s", className, methodName, annotationName, propertyName);
        Optional<T> overrideValue = config.getOptionalValue(key, resultType);
        boolean methodAnnotated = context.getMethod().isAnnotationPresent(annotationType);
        if (overrideValue.isPresent() && (!requiresAnnotation || methodAnnotated)) {
            return overrideValue.get();
        }
        if (!requiresAnnotation || !methodAnnotated) {
            key = String.format("%s/%s/%s", className, annotationName, propertyName);
            overrideValue = config.getOptionalValue(key, resultType);
            if (overrideValue.isPresent() && (!requiresAnnotation || targetClass.isAnnotationPresent(annotationType))) {
                return overrideValue.get();
            }
        }
        key = String.format("%s/%s", annotationName, propertyName);
        overrideValue = config.getOptionalValue(key, resultType);
        return overrideValue.orElse(defaultValue);
    }

    /**
     * Returns either the CDI Proxy class (for cases where a class extends another without overriding the target method), 
     * or the actual declaring class if the annotation isn't present on the proxy class.
     * @param <A> The class of the annotation
     * @param context The context of the method invocation
     * @param annotationClass The class of the annotation
     * @return  The class of the annotated method
     */
    public static <A extends Annotation> Class<?> getAnnotatedMethodClass(InvocationContext context, 
            Class<A> annotationClass) {
        Class<?> targetClass = context.getTarget().getClass();
        if (targetClass.isAnnotationPresent(annotationClass)) {
            return targetClass;
        }
        return context.getMethod().getDeclaringClass();
    }

    /**
     * Helper method that gets the canonical name of an annotated class. This is used to strip out any Weld proxy
     * naming that gets appended to the real class name.
     * @param <A> The class of the annotation
     * @param type The class of the annotation
     * @return The canonical name of the annotated method's class
     */
    public static <A extends Annotation> String getPlainCanonicalName(Class<?> type) {
        String canonicalName = type.getCanonicalName();
        // If the class was a proxy from Weld, cut away the appended gubbins
        // Author Note: There's probably a better way of doing this...
        if (canonicalName.contains("$Proxy$_$$_WeldSubclass")) {
            return canonicalName.split("\\$Proxy\\$_\\$\\$_WeldSubclass")[0];
        }
        return canonicalName;
    }

    /**
     * Gets the full method signature from an invocation context, stripping out any Weld proxy name gubbins.
     * 
     * @param context The context of the method invocation
     * @return full canonical name of the method as in {@code my.pack.MyClass.myMethod}
     */
    public static String getCanonicalMethodName(InvocationContext context) {
        return getPlainCanonicalName(context.getMethod().getDeclaringClass()) + "." + context.getMethod().getName();
    }

    public static boolean isAnnotatedWithFaultToleranceAnnotations(Annotated element) {
        return element.isAnnotationPresent(Asynchronous.class) 
                || element.isAnnotationPresent(Bulkhead.class)
                || element.isAnnotationPresent(CircuitBreaker.class)
                || element.isAnnotationPresent(Fallback.class)
                || element.isAnnotationPresent(Retry.class)
                || element.isAnnotationPresent(Timeout.class);
    }

    public static <T> Class<? extends T>[] toClassArray(String classNames, String attributeName,
            Class<? extends T>[] defaultValue) {
        if (classNames == null) {
            return defaultValue;
        }
        try {
            List<Class<?>> classList = new ArrayList<>();
            // Remove any curly or square brackets from the string, as well as any spaces and ".class"es
            for (String className : classNames.replaceAll("[\\{\\[ \\]\\}]", "").replaceAll("\\.class", "")
                    .split(",")) {
                classList.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
            }
            return classList.toArray(defaultValue);
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.INFO, "Could not find class from " + attributeName + " config, defaulting to annotation. "
                    + "Make sure you give the full canonical class name.", cnfe);
            return defaultValue;
        }
    }
}
