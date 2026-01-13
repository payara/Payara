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

import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.inject.Qualifier;
import jakarta.decorator.Decorator;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A simple DTO to hold metadata about a CDI Decorator.
 */
public class DecoratorInfo extends BeanInfo {

    private final Set<String> decoratedTypes;
    private final String delegateType;
    private final Set<String> delegateQualifiers;
    private final Set<String> classQualifiers;
    private final String scope;

    public DecoratorInfo(String className,
                         Set<String> decoratedTypes,
                         String delegateType,
                         Set<String> delegateQualifiers,
                         Set<String> classQualifiers,
                         String scope) {
        super(className);
        this.decoratedTypes = decoratedTypes;
        this.delegateType = delegateType;
        this.delegateQualifiers = delegateQualifiers;
        this.classQualifiers = classQualifiers;
        this.scope = scope;
    }

    public Set<String> getDecoratedTypes() {
        return decoratedTypes;
    }

    public String getDelegateType() {
        return delegateType;
    }

    public Set<String> getDelegateQualifiers() {
        return delegateQualifiers;
    }

    public Set<String> getClassQualifiers() {
        return classQualifiers;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return "DecoratorInfo{" +
                "className='" + className + '\'' +
                ", decoratedTypes=" + decoratedTypes +
                ", delegateType='" + delegateType + '\'' +
                ", delegateQualifiers=" + delegateQualifiers +
                ", classQualifiers=" + classQualifiers +
                ", scope='" + scope + '\'' +
                '}';
    }

    /**
     * Factory method: extract metadata from an AnnotatedType
     */
    public static DecoratorInfo fromAnnotatedType(AnnotatedType<?> at) {
        if (!at.isAnnotationPresent(Decorator.class)) {
            throw new IllegalArgumentException("Not a @Decorator type: " + at.getJavaClass());
        }

        // Class name
        String className = at.getJavaClass().getName();

        // Interfaces (decorated types)
        Set<String> decoratedTypes = Set.of(at.getJavaClass().getInterfaces())
                .stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        // Delegate injection point
        AnnotatedField<?> delegateField = at.getFields()
                .stream()
                .filter(f -> f.isAnnotationPresent(jakarta.decorator.Delegate.class))
                .findFirst()
                .orElse(null);

        String delegateType = delegateField != null
                ? delegateField.getBaseType().getTypeName()
                : null;

        // Delegate qualifiers
        Set<String> delegateQualifiers = delegateField != null
                ? delegateField.getAnnotations()
                    .stream()
                    .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                    .map(a -> a.annotationType().getName())
                    .collect(Collectors.toSet())
                : Set.of();

        // Class-level qualifiers
        Set<String> classQualifiers = at.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .map(a -> a.annotationType().getName())
                .collect(Collectors.toSet());

        // Scope
        String scope = at.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(jakarta.enterprise.context.NormalScope.class) ||
                             a.annotationType().isAnnotationPresent(jakarta.inject.Scope.class))
                .map(a -> a.annotationType().getName())
                .findFirst()
                .orElse("jakarta.enterprise.context.Dependent"); // default for decorators

        return new DecoratorInfo(className, decoratedTypes, delegateType, delegateQualifiers, classQualifiers, scope);
    }
}
