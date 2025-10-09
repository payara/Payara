/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */
package fish.payara.microprofile.telemetry.tracing.jaxrs;

import fish.payara.microprofile.telemetry.tracing.WithSpanMethodInterceptor;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.container.ResourceInfo;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;

/**
 * Utilities class for various CDI-based operations used by the OpenTracing Service classes.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 * @author Arjan Tijms <arjan.tijms@payara.fish>
 */
final class OpenTracingCdiUtils {

    private static final Logger LOG = Logger.getLogger(OpenTracingCdiUtils.class.getName());

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
        return WithSpanMethodInterceptor.getAnnotation(
            beanManager, annotationClass,
            resourceInfo.getResourceClass(),
            resourceInfo.getResourceMethod());
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
    public static <A extends Annotation, T> Optional<T> getConfigOverrideValue(Class<A> annotationClass, String parameterName, InvocationContext invocationContext, Class<T> parameterType) {
        return  getConfigOverrideValue(annotationClass, parameterName, invocationContext.getMethod(), parameterType);
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
    public static <A extends Annotation, T> Optional<T> getConfigOverrideValue(Class<A> annotationClass, String parameterName, Method method, Class<T> parameterType) {

        final Config config = getConfig();
        if (config == null) {
            LOG.log(FINE, "No config to get override parameters from.");
            return Optional.empty();
        }

        // Get the annotation, method, and class names
        final String annotationName = annotationClass.getSimpleName();
        final String annotatedMethodName = method.getName();
        final String annotatedClassCanonicalName = method.getDeclaringClass().getCanonicalName();

        // Check if there's a config override for the method
        LOG.log(FINER, "Getting config override for annotated method...");

        final Optional<T> methodValue = config.getOptionalValue( //
            annotatedClassCanonicalName + "/" + annotatedMethodName + "/" + annotationName + "/" + parameterName, //
            parameterType);
        if (methodValue.isPresent()) {
            return methodValue;
        }

        LOG.log(FINER, "No config override for annotated method, checking if the method is annotated directly...");
        if (method.getAnnotation(annotationClass) != null) {
            LOG.log(FINER, "Using method annotation.");
            return methodValue; // it is empty here
        }

        // If there wasn't a config override for the method, check if there's one for the class
        LOG.log(FINER, "No config override or annotated method, getting config override for the annotated class...");
        final Optional<T> classValue = config.getOptionalValue( //
            annotatedClassCanonicalName + "/" + annotationName + "/" + parameterName, parameterType);
        if (classValue.isPresent()) {
            return classValue;
        }

        // If there wasn't a config override for the class either, check if there's a global one
        LOG.log(FINER, "No config override for the annotated class, getting application wide config override...");
        final Optional<T> appValue = config.getOptionalValue(annotationName + "/" + parameterName, parameterType);
        if (!appValue.isPresent()) {
            LOG.log(FINER, "No config overrides");
        }
        return appValue;
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
    public static <A extends Annotation, T> Optional<T> getConfigOverrideValue(Class<A> annotationClass,
            String parameterName, ResourceInfo resourceInfo, Class<T> parameterType) {

        final Config config = getConfig();
        if (config == null) {
            LOG.log(FINE, "No config to get override parameters from.");
            return Optional.empty();
        }

        LOG.log(FINER, "Getting config override for annotated method...");
        final String annotationName = annotationClass.getSimpleName();
        final String annotatedMethodName = resourceInfo.getResourceMethod().getName();
        final String annotatedClassCanonicalName = resourceInfo.getResourceClass().getCanonicalName();

        final Optional<T> methodValue = config.getOptionalValue( //
            annotatedClassCanonicalName + "/" + annotatedMethodName + "/" + annotationName + "/" + parameterName,
            parameterType);
        if (methodValue.isPresent()) {
            return methodValue;
        }

        LOG.log(FINEST, "No config override for annotated method, checking if the method is annotated directly...");
        if (resourceInfo.getResourceMethod().getAnnotation(annotationClass) != null) {
            LOG.log(FINER, "Using method annotation.");
            return methodValue; // it is empty here
        }

        LOG.log(FINER, "No config override or annotated method, getting config override for the annotated class...");
        final Optional<T> classValue = config.getOptionalValue( //
            annotatedClassCanonicalName + "/" + annotationName + "/" + parameterName, parameterType);
        if (classValue.isPresent()) {
            return classValue;
        }

        LOG.log(FINER, "No config override for the annotated class, getting application wide config override...");
        final Optional<T> appValue = config.getOptionalValue(annotationName + "/" + parameterName, parameterType);
        if (!appValue.isPresent()) {
            LOG.log(FINER, "No config overrides");
        }
        return appValue;
    }

    private static Config getConfig() {
        try {
            return ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.INFO, "No config could be found", ex);
        }

        return null;
    }


}
