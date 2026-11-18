/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright [2018-2026] Payara Foundation and/or its affiliates

package org.glassfish.apf.impl;

import org.glassfish.apf.ComponentInfo;
import org.glassfish.apf.factory.Factory;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents the view of a class from annotation: its public constructors, declared
 * fields and declared methods, including those of its superclasses outside {@code java.lang},
 * with overridden methods counted once.
 *
 * @author Shing Wai Chan
 */
public class ComponentDefinition implements ComponentInfo {

    private final List<ClassInfo> classes = new ArrayList<>();
    private final List<MethodInfo> constructors = new ArrayList<>();
    private final List<FieldInfo> fields = new ArrayList<>();
    private final Map<MethodKey, MethodInfo> methods = new LinkedHashMap<>();

    public ComponentDefinition(ClassInfo clazz, IndexView index) {
        constructClassList(clazz, index);
        for (ClassInfo cl : classes) {
            for (MethodInfo constructor : cl.constructors()) {
                if (Modifier.isPublic(constructor.flags())) {
                    constructors.add(constructor);
                }
            }
            fields.addAll(cl.unsortedFields());
            for (MethodInfo method : cl.unsortedMethods()) {
                if (!method.isBridge() && !method.name().startsWith("<")) {
                    methods.put(new MethodKey(method), method);
                }
            }
        }
    }

    @Override
    public List<FieldInfo> getFields() {
        return fields;
    }

    @Override
    public List<MethodInfo> getMethods() {
        return new ArrayList<>(methods.values());
    }

    @Override
    public List<MethodInfo> getConstructors() {
        return constructors;
    }

    /**
     * The class and its superclasses, topmost first, skipping {@code java.lang} and the classes
     * listed in the skip-annotation-class-list.
     */
    private void constructClassList(ClassInfo clazz, IndexView index) {
        if (!Factory.isSkipAnnotationProcessing(clazz.name().toString())) {
            classes.add(clazz);
        }
        for (ClassInfo parent = ClassHierarchy.superClass(index, clazz); parent != null;
             parent = ClassHierarchy.superClass(index, parent)) {
            String packageName = parent.name().packagePrefix();
            if ((packageName == null || !packageName.startsWith("java.lang"))
                    && !Factory.isSkipAnnotationProcessing(parent.name().toString())) {
                classes.add(0, parent);
            }
        }
    }

    /**
     * Identifies a method by name and parameter types, so that a method overriding or hiding one of a
     * superclass replaces it: private methods never override, package-private ones only within the
     * same package.
     */
    private static final class MethodKey {
        private final MethodInfo method;
        private final String[] parameterTypes;
        private final String className;
        private final String packageName;
        private final int hashCode;

        private MethodKey(MethodInfo method) {
            this.method = method;
            this.parameterTypes = ClassHierarchy.javaParameterNames(method);
            this.className = method.declaringClass().name().toString();
            this.packageName = method.declaringClass().name().packagePrefix();
            this.hashCode = method.name().hashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MethodKey)) {
                return false;
            }
            MethodKey other = (MethodKey) o;
            if (!method.name().equals(other.method.name()) || !Arrays.equals(parameterTypes, other.parameterTypes)) {
                return false;
            }
            int modifiers = method.flags();
            int otherModifiers = other.method.flags();
            boolean otherPackageProtected = !Modifier.isPublic(otherModifiers)
                    && !Modifier.isProtected(otherModifiers) && !Modifier.isPrivate(otherModifiers);
            boolean samePackage = Objects.equals(packageName, other.packageName);
            if (Modifier.isPrivate(modifiers)) {
                // need exact match
                return Modifier.isPrivate(otherModifiers) && samePackage && className.equals(other.className);
            }
            // public, protected, package protected
            return Modifier.isPublic(otherModifiers) || Modifier.isProtected(otherModifiers)
                    || otherPackageProtected && samePackage;
        }
    }
}
