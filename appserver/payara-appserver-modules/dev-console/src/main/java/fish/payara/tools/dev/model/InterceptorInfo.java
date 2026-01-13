/*
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.tools.dev.model;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedType;
import java.time.Instant;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A DTO holding metadata about a CDI Interceptor.
 */
public class InterceptorInfo extends BeanInfo {

    private final Set<String> interceptorBindings;
    private final int priority;
    private final String scope;
    private final Set<String> classQualifiers;
    
    public InterceptorInfo(String className,
            Set<String> interceptorBindings,
            int priority,
            String scope,
            Set<String> classQualifiers) {
        super(className);
        this.interceptorBindings = interceptorBindings;
        this.priority = priority;
        this.scope = scope;
        this.classQualifiers = classQualifiers;
    }

    public Set<String> getInterceptorBindings() {
        return interceptorBindings;
    }

    public int getPriority() {
        return priority;
    }

    public String getScope() {
        return scope;
    }

    public Set<String> getClassQualifiers() {
        return classQualifiers;
    }

    @Override
    public String toString() {
        return "InterceptorInfo{"
                + "className='" + className + '\''
                + ", interceptorBindings=" + interceptorBindings
                + ", priority=" + priority
                + ", scope='" + scope + '\''
                + ", classQualifiers=" + classQualifiers
                + '}';
    }

    public static InterceptorInfo fromAnnotated(Annotated annotated) {
        if (!(annotated instanceof AnnotatedType<?> at)) {
            throw new IllegalArgumentException("Expected AnnotatedType for interceptor, got: " + annotated);
        }

        if (!at.isAnnotationPresent(jakarta.interceptor.Interceptor.class)) {
            throw new IllegalArgumentException("Not an @Interceptor type: " + at.getJavaClass());
        }

        String className = at.getJavaClass().getName();

        // Extract interceptor bindings
        Set<String> interceptorBindings = at.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(jakarta.interceptor.InterceptorBinding.class))
                .map(a -> a.annotationType().getName())
                .collect(Collectors.toSet());

        // Extract @Priority if present
        int priority = at.isAnnotationPresent(jakarta.annotation.Priority.class)
                ? at.getAnnotation(jakarta.annotation.Priority.class).value()
                : 0;

        // Scope (default is @Dependent)
        String scope = at.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(jakarta.enterprise.context.NormalScope.class)
                || a.annotationType().isAnnotationPresent(jakarta.inject.Scope.class))
                .map(a -> a.annotationType().getName())
                .findFirst()
                .orElse("jakarta.enterprise.context.Dependent");

        // Class-level qualifiers
        Set<String> classQualifiers = at.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(jakarta.inject.Qualifier.class))
                .map(a -> a.annotationType().getName())
                .collect(Collectors.toSet());

        return new InterceptorInfo(className, interceptorBindings, priority, scope, classQualifiers);
    }

}
