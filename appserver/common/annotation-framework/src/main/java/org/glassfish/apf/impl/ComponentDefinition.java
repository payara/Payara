/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package org.glassfish.apf.impl;

import org.glassfish.apf.ComponentInfo;
import org.glassfish.apf.factory.Factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class represents the view of a class from annotation.
 *
 * @author Shing Wai Chan
 */
public class ComponentDefinition implements ComponentInfo {
    final private Class clazz;
    final private List<Constructor> constructors = new ArrayList<Constructor>();
    final private List<Class> classes = new ArrayList<Class>();
    final private List<Field> fields = new ArrayList<Field>();
    final private Map<MethodKey, Method> methodMap = new HashMap<MethodKey, Method>();

    public ComponentDefinition(Class clazz) {
        this.clazz = clazz;
        constructClassList();
        initializeConstructors();
        initializeFields();
        initializeMethods();
    }

    public Field[] getFields() {
        return fields.toArray(new Field[fields.size()]);
    }

    public Method[] getMethods() {
        return methodMap.values().toArray(new Method[methodMap.size()]);
    }

    public Constructor[] getConstructors() {
        return constructors.toArray(new Constructor[constructors.size()]);
    }

    private void constructClassList() {
        // check whether this class is in the skip list
        if (!Factory.isSkipAnnotationProcessing(clazz.getName())) {
            classes.add(clazz);
        }
        Class parent = clazz;
        while ((parent = parent.getSuperclass()) != null) {
            if (parent.getPackage() == null ||
                    !parent.getPackage().getName().startsWith("java.lang")) {
                // always check whether this class is in the class list
                // for skipping annotation processing
                if (!Factory.isSkipAnnotationProcessing(parent.getName())) {
                    classes.add(0, parent);
                }
            } 
        }
    }

    /**
     * In P.148 of "The Java Langugage Specification 2/e", 
     * Constructors, static initializers, and instance initializers are not
     * members and therefore not inherited.
     */
    private void initializeConstructors() {
        for (Class cl : classes) {
            for (Constructor constr : cl.getConstructors()) {
                constructors.add(constr);           
            }
        }
    }

    private void initializeFields() {
        for (Class cl : classes) {
            for (Field f : cl.getDeclaredFields()) {
                fields.add(f);           
            }
        }
    }

    private void initializeMethods() {
        for (Class cl : classes) {
            for (Method method : cl.getDeclaredMethods()) {
                if (!method.isBridge()) {
                    methodMap.put(new MethodKey(method), method);
                }
            }
        }
    }

    private static class MethodKey {
        private Method m = null;
        private int hashCode;
        private String className = null;
        private Package classPackage = null;

        private MethodKey(Method m) {
            this.m = m;
            hashCode = m.getName().hashCode();
            // store className and classPackage as getters are native
            className = m.getDeclaringClass().getName();
            classPackage = m.getDeclaringClass().getPackage();
        }

        public int hashCode() { 

            return hashCode;
        }

        /**
         * This equals method is defined in terms of inheritance overriding.
         * We depends on java compiler to rule out irrelvant cases here.
         * @return true for overriding and false otherwise
         */
        public boolean equals(Object o) {
            if (!(o instanceof MethodKey)) {
                return false;
            }

            MethodKey mk2 = (MethodKey)o;
            Method m2 = mk2.m;
            if (m.getName().equals(m2.getName()) && Arrays.equals(
                    m.getParameterTypes(), m2.getParameterTypes())) {
                int modifiers = m.getModifiers();
                int modifiers2 = m2.getModifiers();
                boolean isPackageProtected2 = !Modifier.isPublic(modifiers2) &&
                        !Modifier.isProtected(modifiers2) &&
                        !Modifier.isPrivate(modifiers2);
                boolean isSamePackage = 
                        (classPackage == null && mk2.classPackage == null) ||
                        (classPackage != null && mk2.classPackage != null &&
                            classPackage.getName().equals(
                            mk2.classPackage.getName()));
                if (Modifier.isPrivate(modifiers)) {
                    // need exact match
                    return Modifier.isPrivate(modifiers2) && isSamePackage
                            && className.equals(mk2.className);
                } else { // public, protected, package protected
                    return Modifier.isPublic(modifiers2) ||
                            Modifier.isProtected(modifiers2) ||
                            isPackageProtected2 && isSamePackage;
                }
            }

            return false;
        }
    }
}
