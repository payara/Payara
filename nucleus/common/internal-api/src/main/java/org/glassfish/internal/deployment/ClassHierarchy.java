/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package org.glassfish.internal.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;

import java.lang.annotation.Inherited;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Type-hierarchy and naming queries over a Jandex {@link IndexView}, giving deployment code the
 * answers it used to get from {@code java.lang.Class} without loading any class. The index is
 * expected to resolve classes outside the deployment too, see {@link ClassLoaderIndex}.
 */
public final class ClassHierarchy {

    public static final DotName OBJECT = DotName.createSimple(Object.class);
    private static final DotName INHERITED = DotName.createSimple(Inherited.class);

    private ClassHierarchy() {
    }

    /**
     * @return the superclass of the type, or null for {@code java.lang.Object}, interfaces and
     * superclasses the index cannot resolve
     */
    public static ClassInfo superClass(IndexView index, ClassInfo type) {
        DotName superName = type.superName();
        return superName == null || OBJECT.equals(superName) ? null : index.getClassByName(superName);
    }

    /**
     * @return the type followed by its superclasses, nearest first, {@code java.lang.Object} excluded
     */
    public static List<ClassInfo> hierarchy(IndexView index, ClassInfo type) {
        List<ClassInfo> result = new ArrayList<>();
        for (ClassInfo current = type; current != null; current = superClass(index, current)) {
            result.add(current);
        }
        return result;
    }

    /**
     * @return the directly implemented (or, for an interface, extended) interfaces the index can resolve
     */
    public static List<ClassInfo> interfaces(IndexView index, ClassInfo type) {
        List<ClassInfo> result = new ArrayList<>();
        for (DotName name : type.interfaceNames()) {
            ClassInfo itf = index.getClassByName(name);
            if (itf != null) {
                result.add(itf);
            }
        }
        return result;
    }

    /**
     * @return every interface the type implements, directly, through superclasses or through
     * super-interfaces, by name
     */
    public static Set<DotName> allInterfaces(IndexView index, ClassInfo type) {
        Set<DotName> result = new LinkedHashSet<>();
        Deque<ClassInfo> queue = new ArrayDeque<>();
        queue.add(type);
        while (!queue.isEmpty()) {
            ClassInfo current = queue.poll();
            for (DotName name : current.interfaceNames()) {
                if (result.add(name)) {
                    ClassInfo itf = index.getClassByName(name);
                    if (itf != null) {
                        queue.add(itf);
                    }
                }
            }
            ClassInfo superClass = superClass(index, current);
            if (superClass != null) {
                queue.add(superClass);
            }
        }
        return result;
    }

    /**
     * The equivalent of {@code superType.isAssignableFrom(type)}: true when the type is the super type
     * itself, extends it or implements it, directly or transitively.
     */
    public static boolean isSubtype(IndexView index, ClassInfo type, DotName superType) {
        if (OBJECT.equals(superType)) {
            return true;
        }
        Set<DotName> seen = new HashSet<>();
        Deque<ClassInfo> queue = new ArrayDeque<>();
        queue.add(type);
        while (!queue.isEmpty()) {
            ClassInfo current = queue.poll();
            if (current.name().equals(superType)) {
                return true;
            }
            if (!seen.add(current.name())) {
                continue;
            }
            for (DotName name : current.interfaceNames()) {
                if (name.equals(superType)) {
                    return true;
                }
                ClassInfo itf = index.getClassByName(name);
                if (itf != null) {
                    queue.add(itf);
                }
            }
            DotName superName = current.superName();
            if (superName != null && !OBJECT.equals(superName)) {
                if (superName.equals(superType)) {
                    return true;
                }
                ClassInfo superClass = index.getClassByName(superName);
                if (superClass != null) {
                    queue.add(superClass);
                }
            }
        }
        return false;
    }

    public static boolean isSubtype(IndexView index, DotName type, DotName superType) {
        if (type.equals(superType) || OBJECT.equals(superType)) {
            return true;
        }
        ClassInfo classInfo = index.getClassByName(type);
        return classInfo != null && isSubtype(index, classInfo, superType);
    }

    public static boolean isSubtype(IndexView index, DotName type, Class<?> superType) {
        return isSubtype(index, type, DotName.createSimple(superType));
    }

    public static boolean isSubtype(IndexView index, ClassInfo type, Class<?> superType) {
        return isSubtype(index, type, DotName.createSimple(superType));
    }

    public static boolean isSubtype(IndexView index, Type type, Class<?> superType) {
        return type.kind() != Type.Kind.PRIMITIVE && type.kind() != Type.Kind.VOID
                && isSubtype(index, DotName.createSimple(rawName(type)), DotName.createSimple(superType));
    }

    public static boolean isInterface(ClassInfo type) {
        return Modifier.isInterface(type.flags());
    }

    public static boolean isAbstract(ClassInfo type) {
        return Modifier.isAbstract(type.flags());
    }

    /**
     * @return whether the annotation type is declared {@code @Inherited}
     */
    public static boolean isInherited(IndexView index, DotName annotationType) {
        ClassInfo annotation = index.getClassByName(annotationType);
        return annotation != null && annotation.hasDeclaredAnnotation(INHERITED);
    }

    /**
     * The runtime-visible annotations {@code Class.getAnnotations()} would return: those declared on
     * the class plus the {@code @Inherited} ones of its superclasses that the class does not redeclare.
     */
    public static List<AnnotationInstance> classAnnotations(IndexView index, ClassInfo type) {
        List<AnnotationInstance> result = new ArrayList<>();
        Set<DotName> present = new HashSet<>();
        for (AnnotationInstance annotation : type.declaredAnnotations()) {
            if (annotation.runtimeVisible()) {
                result.add(annotation);
                present.add(annotation.name());
            }
        }
        for (ClassInfo superClass = superClass(index, type); superClass != null; superClass = superClass(index, superClass)) {
            for (AnnotationInstance annotation : superClass.declaredAnnotations()) {
                if (annotation.runtimeVisible() && !present.contains(annotation.name()) && isInherited(index, annotation.name())) {
                    result.add(annotation);
                    present.add(annotation.name());
                }
            }
        }
        return result;
    }

    /**
     * The equivalent of {@code AnnotatedElement.getAnnotation(type)}: for a class this includes
     * inherited annotations, for members only the declared ones.
     */
    public static AnnotationInstance annotation(IndexView index, AnnotationTarget target, DotName annotationType) {
        if (target.kind() == AnnotationTarget.Kind.CLASS) {
            for (AnnotationInstance annotation : classAnnotations(index, target.asClass())) {
                if (annotation.name().equals(annotationType)) {
                    return annotation;
                }
            }
            return null;
        }
        AnnotationInstance annotation = target.declaredAnnotation(annotationType);
        return annotation != null && annotation.runtimeVisible() ? annotation : null;
    }

    public static AnnotationInstance annotation(IndexView index, AnnotationTarget target, Class<?> annotationType) {
        return annotation(index, target, DotName.createSimple(annotationType));
    }

    public static ClassInfo declaringClass(AnnotationTarget target) {
        return JandexIndexer.Index.getDeclaringClass(target);
    }

    /**
     * @return what {@code Class.getSimpleName()} returns for the type
     */
    public static String simpleName(ClassInfo type) {
        String name = type.name().toString();
        return name.substring(Math.max(name.lastIndexOf('.'), name.lastIndexOf('$')) + 1);
    }

    /**
     * @return the name {@code Class.getName()} returns for the type: {@code int}, {@code [I},
     * {@code java.lang.String}, {@code [Ljava.lang.String;}; type variables resolve to their first bound
     */
    public static String javaName(Type type) {
        if (type.kind() == Type.Kind.ARRAY) {
            ArrayType array = type.asArrayType();
            Type element = array.elementType();
            String descriptor = element.kind() == Type.Kind.PRIMITIVE
                    ? descriptor(element.asPrimitiveType()) : "L" + rawName(element) + ";";
            return "[".repeat(array.deepDimensions()) + descriptor;
        }
        return rawName(type);
    }

    /**
     * @return the source form of the type name used in deployment descriptors: {@code int},
     * {@code int[]}, {@code java.lang.String[]}
     */
    public static String sourceName(Type type) {
        if (type.kind() == Type.Kind.ARRAY) {
            ArrayType array = type.asArrayType();
            return rawName(array.elementType()) + "[]".repeat(array.deepDimensions());
        }
        return rawName(type);
    }

    /**
     * @return the erased class name of a non-array type, {@code int} and {@code void} for the primitives
     */
    public static String rawName(Type type) {
        switch (type.kind()) {
            case TYPE_VARIABLE:
                List<Type> bounds = type.asTypeVariable().bounds();
                return bounds.isEmpty() ? OBJECT.toString() : rawName(bounds.get(0));
            case TYPE_VARIABLE_REFERENCE:
                return rawName(type.asTypeVariableReference().follow());
            case WILDCARD_TYPE:
                return rawName(type.asWildcardType().extendsBound());
            case ARRAY:
                return javaName(type);
            default:
                return type.name().toString();
        }
    }

    public static String[] javaParameterNames(MethodInfo method) {
        return method.parameterTypes().stream().map(ClassHierarchy::javaName).toArray(String[]::new);
    }

    public static String[] sourceParameterNames(MethodInfo method) {
        return method.parameterTypes().stream().map(ClassHierarchy::sourceName).toArray(String[]::new);
    }

    /**
     * @param javaParameterNames the parameter class names in {@link #javaName(Type)} form, or null to
     * match on the method name alone
     */
    public static boolean matches(MethodInfo method, String name, String[] javaParameterNames) {
        return method.name().equals(name)
                && (javaParameterNames == null || Arrays.equals(javaParameterNames(method), javaParameterNames));
    }

    private static String descriptor(PrimitiveType type) {
        switch (type.primitive()) {
            case BOOLEAN: return "Z";
            case BYTE: return "B";
            case CHAR: return "C";
            case SHORT: return "S";
            case INT: return "I";
            case LONG: return "J";
            case FLOAT: return "F";
            case DOUBLE: return "D";
            default: throw new IllegalArgumentException(type.toString());
        }
    }
}
