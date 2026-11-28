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
package org.glassfish.weld.services;

import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.weld.resources.spi.ClassFileInfo;
import org.jboss.weld.resources.spi.ClassFileInfoException;
import org.jboss.weld.resources.spi.ClassFileServices;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Weld's fast bootstrap path, backed by the deployment's Jandex class indexes.
 * <p>
 * With this service Weld answers questions about discovered classes (annotations, class hierarchy,
 * constructors, vetoed status, nesting) from the index and only loads the classes that turn out to
 * be beans, observers or extensions. Classes that are not in the index are reported through
 * {@link ClassFileInfoException}, for which Weld falls back to loading the class.
 */
public class JandexClassFileServices implements ClassFileServices {

    private static final Logger LOGGER = Logger.getLogger(JandexClassFileServices.class.getName());
    private static final DotName INJECT = DotName.createSimple(Inject.class.getName());
    private static final DotName VETOED = DotName.createSimple(Vetoed.class.getName());
    private static final String PACKAGE_INFO = "package-info";

    private final IndexView index;
    private final ClassLoader classLoader;
    /** names of the annotations declared on an annotation type */
    private final Map<DotName, Set<DotName>> metaAnnotations = new ConcurrentHashMap<>();

    /**
     * @param index       the class indexes of the deployment
     * @param classLoader used for classes outside the index, e.g. super types provided by the server
     */
    public JandexClassFileServices(IndexView index, ClassLoader classLoader) {
        this.index = Objects.requireNonNull(index);
        this.classLoader = Objects.requireNonNull(classLoader);
    }

    @Override
    public ClassFileInfo getClassFileInfo(String className) {
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            throw new ClassFileInfoException(className + " is not in the class index of the deployment");
        }
        LOGGER.log(Level.FINEST, "Class file info from index for {0}", className);
        return new JandexClassFileInfo(classInfo);
    }

    @Override
    public void cleanupAfterBoot() {
        metaAnnotations.clear();
    }

    @Override
    public void cleanup() {
        cleanupAfterBoot();
    }

    /**
     * @return the names of the annotations declared on the given annotation type, looked up in the index
     * or, for annotation types outside the deployment, through the class loader
     */
    private Set<DotName> getMetaAnnotations(DotName annotationName) {
        return metaAnnotations.computeIfAbsent(annotationName, name -> {
            ClassInfo annotationClass = index.getClassByName(name);
            if (annotationClass != null) {
                return annotationClass.declaredAnnotations().stream().map(AnnotationInstance::name).collect(Collectors.toSet());
            }
            try {
                return Arrays.stream(classLoader.loadClass(name.toString()).getAnnotations())
                        .map(annotation -> DotName.createSimple(annotation.annotationType().getName()))
                        .collect(Collectors.toSet());
            } catch (ClassNotFoundException | LinkageError e) {
                LOGGER.log(Level.FINE, e, () -> "Unable to load annotation type " + name);
                return Collections.emptySet();
            }
        });
    }

    private Class<?> loadClass(DotName className) {
        try {
            return classLoader.loadClass(className.toString());
        } catch (ClassNotFoundException | LinkageError e) {
            throw new ClassFileInfoException("Unable to load " + className + " which is referenced by the deployment", e);
        }
    }

    /**
     * Reflective counterpart of {@link JandexClassFileInfo#containsAnnotation(ClassInfo, DotName)} for
     * super classes that are not part of the index.
     */
    private static boolean containsAnnotation(Class<?> type, Class<? extends Annotation> annotation) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            if (hasAnnotation(current, annotation)) {
                return true;
            }
            for (Field field : current.getDeclaredFields()) {
                if (hasAnnotation(field, annotation)) {
                    return true;
                }
            }
            for (Executable executable : current.getDeclaredConstructors()) {
                if (hasAnnotation(executable, annotation)) {
                    return true;
                }
            }
            for (Method method : current.getDeclaredMethods()) {
                if (hasAnnotation(method, annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotation) {
        if (element instanceof Executable) {
            for (Annotation[] parameterAnnotations : ((Executable) element).getParameterAnnotations()) {
                for (Annotation parameterAnnotation : parameterAnnotations) {
                    if (hasAnnotation(parameterAnnotation.annotationType(), annotation)
                            || parameterAnnotation.annotationType() == annotation) {
                        return true;
                    }
                }
            }
        }
        for (Annotation declared : element.getAnnotations()) {
            // the annotation itself or, one level deep, a meta-annotation
            if (declared.annotationType() == annotation || declared.annotationType().isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSubtype(Class<?> type, String superTypeName) {
        if (type == null) {
            return false;
        }
        if (type.getName().equals(superTypeName) || isSubtype(type.getSuperclass(), superTypeName)) {
            return true;
        }
        for (Class<?> implemented : type.getInterfaces()) {
            if (isSubtype(implemented, superTypeName)) {
                return true;
            }
        }
        return false;
    }

    private final class JandexClassFileInfo implements ClassFileInfo {

        private final ClassInfo classInfo;

        private JandexClassFileInfo(ClassInfo classInfo) {
            this.classInfo = classInfo;
        }

        @Override
        public String getClassName() {
            return classInfo.name().toString();
        }

        @Override
        public String getSuperclassName() {
            return classInfo.superName() == null ? null : classInfo.superName().toString();
        }

        @Override
        public boolean isAnnotationDeclared(Class<? extends Annotation> annotation) {
            return classInfo.hasDeclaredAnnotation(DotName.createSimple(annotation.getName()));
        }

        @Override
        public boolean containsAnnotation(Class<? extends Annotation> annotation) {
            return containsAnnotation(classInfo, DotName.createSimple(annotation.getName()), annotation);
        }

        /**
         * Whether the class, one of its members or parameters, one of their meta-annotations, or one of
         * the super classes carries the annotation.
         */
        private boolean containsAnnotation(ClassInfo current, DotName required, Class<? extends Annotation> annotation) {
            Set<DotName> present = current.annotationsMap().keySet();
            if (present.contains(required) || present.stream().anyMatch(name -> getMetaAnnotations(name).contains(required))) {
                return true;
            }
            DotName superName = current.superName();
            if (superName == null || DotName.OBJECT_NAME.equals(superName)) {
                return false;
            }
            ClassInfo superClass = index.getClassByName(superName);
            return superClass == null
                    ? JandexClassFileServices.containsAnnotation(loadClass(superName), annotation)
                    : containsAnnotation(superClass, required, annotation);
        }

        @Override
        public int getModifiers() {
            return classInfo.flags();
        }

        @Override
        public boolean hasCdiConstructor() {
            for (MethodInfo method : classInfo.methods()) {
                if (method.isConstructor() && (method.parametersCount() == 0 || method.hasAnnotation(INJECT))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isAssignableFrom(Class<?> fromClass) {
            return isSubtype(fromClass, getClassName());
        }

        @Override
        public boolean isAssignableTo(Class<?> toClass) {
            return isAssignableTo(classInfo.name(), toClass);
        }

        private boolean isAssignableTo(DotName name, Class<?> toClass) {
            if (toClass.getName().equals(name.toString())) {
                return true;
            }
            if (DotName.OBJECT_NAME.equals(name)) {
                return false;
            }
            ClassInfo current = index.getClassByName(name);
            if (current == null) {
                // a super type outside the deployment, e.g. provided by the server
                return toClass.isAssignableFrom(loadClass(name));
            }
            if (current.superName() != null && isAssignableTo(current.superName(), toClass)) {
                return true;
            }
            for (DotName implemented : current.interfaceNames()) {
                if (isAssignableTo(implemented, toClass)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isVetoed() {
            if (classInfo.hasDeclaredAnnotation(VETOED)) {
                return true;
            }
            DotName packageName = classInfo.name().packagePrefixName();
            ClassInfo packageInfo = packageName == null ? null
                    : index.getClassByName(DotName.createComponentized(packageName, PACKAGE_INFO));
            return packageInfo != null && packageInfo.hasDeclaredAnnotation(VETOED);
        }

        @Override
        public NestingType getNestingType() {
            switch (classInfo.nestingType()) {
                case TOP_LEVEL:
                    return NestingType.TOP_LEVEL;
                case LOCAL:
                    return NestingType.NESTED_LOCAL;
                case ANONYMOUS:
                    return NestingType.NESTED_ANONYMOUS;
                default:
                    return Modifier.isStatic(classInfo.flags()) ? NestingType.NESTED_STATIC : NestingType.NESTED_INNER;
            }
        }
    }
}
