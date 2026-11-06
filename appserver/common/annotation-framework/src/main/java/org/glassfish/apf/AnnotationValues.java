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
package org.glassfish.apf;

import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed access to annotation attributes of a Jandex {@link AnnotationInstance} with the annotation
 * type's defaults applied, as the getters of a {@code java.lang.annotation.Annotation} would return
 * them. The annotation type is resolved through the index, which must therefore see the server's
 * API classes (see {@code JandexIndexer.compositeIndex(indexes, classLoader)}).
 */
public final class AnnotationValues {

    private AnnotationValues() {
    }

    /**
     * @return the explicit value or the annotation type's default
     * @throws IllegalStateException when the attribute has neither, i.e. the annotation type is
     * not resolvable in the index
     */
    public static AnnotationValue value(IndexView index, AnnotationInstance annotation, String name) {
        AnnotationValue value = annotation.valueWithDefault(index, name);
        if (value == null) {
            throw new IllegalStateException("No value and no default for attribute " + name + " of @" + annotation.name()
                    + ", the annotation type is not resolvable in " + index);
        }
        return value;
    }

    public static String string(IndexView index, AnnotationInstance annotation, String name) {
        return value(index, annotation, name).asString();
    }

    public static String[] strings(IndexView index, AnnotationInstance annotation, String name) {
        return value(index, annotation, name).asStringArray();
    }

    public static boolean bool(IndexView index, AnnotationInstance annotation, String name) {
        return value(index, annotation, name).asBoolean();
    }

    public static int integer(IndexView index, AnnotationInstance annotation, String name) {
        return value(index, annotation, name).asInt();
    }

    public static long longValue(IndexView index, AnnotationInstance annotation, String name) {
        return value(index, annotation, name).asLong();
    }

    /**
     * @return the name of the enum constant
     */
    public static String enumName(IndexView index, AnnotationInstance annotation, String name) {
        return value(index, annotation, name).asEnum();
    }

    public static <E extends Enum<E>> E enumValue(IndexView index, AnnotationInstance annotation, String name,
                                                  Class<E> enumType) {
        return Enum.valueOf(enumType, enumName(index, annotation, name));
    }

    /**
     * @return the name of the class-valued attribute as {@code Class.getName()} returns it
     */
    public static String className(IndexView index, AnnotationInstance annotation, String name) {
        return ClassHierarchy.javaName(value(index, annotation, name).asClass());
    }

    public static String[] classNames(IndexView index, AnnotationInstance annotation, String name) {
        return Arrays.stream(value(index, annotation, name).asClassArray())
                .map(ClassHierarchy::javaName).toArray(String[]::new);
    }

    public static Type[] classes(IndexView index, AnnotationInstance annotation, String name) {
        return value(index, annotation, name).asClassArray();
    }

    public static AnnotationInstance nested(IndexView index, AnnotationInstance annotation, String name) {
        return value(index, annotation, name).asNested();
    }

    public static AnnotationInstance[] nestedArray(IndexView index, AnnotationInstance annotation, String name) {
        return value(index, annotation, name).asNestedArray();
    }

    /**
     * @return every attribute of the annotation with the annotation type's defaults applied, as plain
     * serializable objects: boxed primitives, Strings (also for enum constants and, in the form of
     * {@link ClassHierarchy#javaName}, for classes), nested annotations as attribute maps and arrays
     * as {@code Object[]} of those
     */
    public static Map<String, Object> attributes(IndexView index, AnnotationInstance annotation) {
        ClassInfo annotationType = index.getClassByName(annotation.name());
        if (annotationType == null) {
            throw new IllegalStateException("Annotation type " + annotation.name() + " is not resolvable in " + index);
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (MethodInfo attribute : annotationType.methods()) {
            if (attribute.parametersCount() == 0 && !Modifier.isStatic(attribute.flags()) && !attribute.name().startsWith("<")) {
                attributes.put(attribute.name(), toObject(index, value(index, annotation, attribute.name())));
            }
        }
        return attributes;
    }

    /**
     * @return the value as a plain object, see {@link #attributes}
     */
    public static Object toObject(IndexView index, AnnotationValue value) {
        switch (value.kind()) {
            case ENUM:
                return value.asEnum();
            case CLASS:
                return ClassHierarchy.javaName(value.asClass());
            case NESTED:
                return attributes(index, value.asNested());
            case ARRAY:
                return value.asArrayList().stream().map(component -> toObject(index, component)).toArray();
            default:
                return value.value();
        }
    }
}
