/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.model.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the aggregated annotations on a type, its fields and methods including annotations "inherited" from
 * super-classes and implemented interfaces. Should a field or method from a super-class be overridden the
 * {@link Annotation} closest to the represented type (the overriding one) is kept.
 */
public final class AnnotationInfo<T> {

    private static final Map<Class<?>, AnnotationInfo<?>> TYPES = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> AnnotationInfo<T> valueOf(Class<T> type) {
        return (AnnotationInfo<T>) TYPES.computeIfAbsent(type, key -> new AnnotationInfo<>(key));
    }

    private final Class<T> type;
    private final Map<Class<? extends Annotation>, Annotation> typeAnnotations = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<? extends Annotation>, Annotation>> fieldAnnotations = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<? extends Annotation>, Annotation>> methodAnnotations = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<? extends Annotation>, Annotation>> methodParameterAnnotations = new ConcurrentHashMap<>();

    private AnnotationInfo(Class<T> type) {
        this.type = type;
        init(type);
    }

    public Class<T> getType() {
        return type;
    }

    /**
     * Version of {@link Class#getAnnotation(Class)} also considering annotations "inherited" from
     * super-types for this {@link AnnotationInfo#getType()}.
     * 
     * @param annotationType annotation type to check
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return (A) typeAnnotations.get(annotationType);
    }

    /**
     * Version of {@link Field#getAnnotation(Class)} also considering annotations "inherited" from overridden fields
     * of super-types.
     * 
     * @param annotationType annotation type to check
     * @param method         must be a {@link Field} defined or inherited by this {@link AnnotationInfo#getType()}
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType, Field field) {
        return (A) fieldAnnotations.get(field.getName()).get(annotationType);
    }

    /**
     * Version of {@link Method#getAnnotation(Class)} also considering annotations "inherited" from overridden methods
     * of super-types.
     * 
     * @param annotationType annotation type to check
     * @param method         must be a {@link Method} defined or inherited by this {@link AnnotationInfo#getType()}
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType, Method method) {
        return (A) methodAnnotations.get(getSignature(method)).get(annotationType);
    }

    /**
     * Version of {@link Parameter#getAnnotation(Class)} also considering annotations "inherited" from overridden
     * methods of super-types.
     * 
     * @param annotationType annotation type to check
     * @param parameter      must be this a {@link Parameter} of a {@link Method} defined or inherited by this
     *                       {@link AnnotationInfo#getType()}
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType, Parameter parameter) {
        return (A) methodParameterAnnotations.get(getIdentifier(parameter)).get(annotationType);
    }

    /**
     * Version of {@link AnnotatedElement#getAnnotation(Class)} also considering annotations "inherited" from
     * super-types.
     * 
     * @param annotationType annotation type to check
     * @param element        must be this {@link AnnotationInfo#getType()}'s {@link Class} or a {@link Field} or
     *                       {@link Method} defined or inherited by it or a {@link Parameter} of such a method.
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationType, AnnotatedElement element) {
        Class<?> kind = element.getClass();
        if (kind == Class.class) {
            return getAnnotation(annotationType);
        }
        if (kind == Field.class) {
            return getAnnotation(annotationType, (Field) element);
        }
        if (kind == Method.class) {
            return getAnnotation(annotationType, (Method) element);
        }
        if (kind == Parameter.class) {
            return getAnnotation(annotationType, (Parameter) element);
        }
        return null;
    }

    /**
     * Version of {@link Class#isAnnotationPresent(Class)} also considering annotations "inherited" from super-types.
     * 
     * @param annotationType annotation type to check
     * @return true in case it is present at this class or any of its super-types
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    /**
     * Version of {@link Field#isAnnotationPresent(Class)} also considering annotations "inherited" from super-types.
     * 
     * @param annotationType annotation type to check
     * @return true in case it is present at the given field. The field must be defined in this
     *         {@link AnnotationInfo#getType()} or any of its super-types.
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, Field field) {
        return getAnnotation(annotationType, field) != null;
    }

    /**
     * Version of {@link Method#isAnnotationPresent(Class)} also considering annotations "inherited" from super-types.
     * 
     * @param annotationType annotation type to check
     * @param method         the method checked for annotation. The method must be defined or inherited by this
     *                       {@link AnnotationInfo#getType()} or any of its super-types.
     * @return true in case it is present at the given method, else false
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, Method method) {
        return getAnnotation(annotationType, method) != null;
    }

    /**
     * Version of {@link Parameter#isAnnotationPresent(Class)} also considering annotations "inherited" from super-types.
     * 
     * @param annotationType annotation type to check
     * @param parameter      the parameter checked for annotations. The parameter must belong to a method defined or
     *                       inherited by this {@link AnnotationInfo#getType()}
     * @return true in case it is present at the given parameter, else false
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, Parameter parameter) {
        return getAnnotation(annotationType, parameter) != null;
    }

    /**
     * Version of {@link AnnotatedElement#isAnnotationPresent(Class)} also considering annotations "inherited" from
     * super-types.
     * 
     * @param annotationType annotation type to check
     * @param element        must be this {@link AnnotationInfo#getType()}'s {@link Class} or a {@link Field} or
     *                       {@link Method} defined or inherited by it or a {@link Parameter} of such a method.
     * @return true in case it is present at the given element, else false
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, AnnotatedElement element) {
        return getAnnotation(annotationType, element) != null;
    }

    /**
     * @see #isAnnotationPresent(Class, AnnotatedElement)
     */
    @SafeVarargs
    public final boolean isAnyAnnotationPresent(AnnotatedElement element, Class<? extends Annotation>... annotationTypes) {
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (isAnnotationPresent(annotationType, element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the annotation on a {@link Parameter} including those present on same parameter of an overridden method
     * should the method be overridden.
     * 
     * @param parameter the parameter of which to count annotations for
     * @return the number of annotation present on the given {@link Parameter} including annotations present on the same
     *         parameter of an potentially overridden method.
     */
    public int getAnnotationCount(Parameter parameter) {
        return methodParameterAnnotations.get(getIdentifier(parameter)).size();
    }

    private void init(Class<?> type) {
        // recurse first so that re-stated annotations "override"
        Class<?> supertype = type.getSuperclass();
        if (supertype != null && supertype != Object.class) {
            init(supertype);
        }
        for (Class<?> implementedInterface : type.getInterfaces()) {
            init(implementedInterface);
        }
        // collect annotations
        putAll(type.getDeclaredAnnotations(), typeAnnotations);
        for (Field field : type.getDeclaredFields()) {
            putAll(field.getDeclaredAnnotations(), 
                    fieldAnnotations.computeIfAbsent(field.getName(), key -> new ConcurrentHashMap<>()));
        }
        for (Method method : type.getDeclaredMethods()) {
            putAll(method.getDeclaredAnnotations(),
                methodAnnotations.computeIfAbsent(getSignature(method), key -> new ConcurrentHashMap<>()));
            for (Parameter parameter : method.getParameters()) {
                putAll(parameter.getDeclaredAnnotations(), 
                        methodParameterAnnotations.computeIfAbsent(getIdentifier(parameter), key -> new ConcurrentHashMap<>()));
            }
        }
    }

    private static void putAll(Annotation[] annotations, Map<Class<? extends Annotation>, Annotation> map) {
        for (Annotation a : annotations) {
            map.put(a.annotationType(), a);
        }
    }

    private static String getIdentifier(Parameter parameter) {
        try {
            java.lang.reflect.Field index = parameter.getClass().getDeclaredField("index");
            index.setAccessible(true);
            return getSignature(parameter.getDeclaringExecutable()) + "#" + index.getInt(parameter);
        } catch (Exception e) {
            return getSignature(parameter.getDeclaringExecutable()) + "#" + parameter.toString();
        }
    }

    private static String getSignature(Executable method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getName());
        signature.append('(');
        for (Class<?> parameterType : method.getParameterTypes()) {
            signature.append(parameterType.getName()).append(", ");
        }
        signature.append(')');
        return signature.toString();
    }
}