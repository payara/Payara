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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]
package com.sun.ejb.containers.interceptors;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class InterceptorUtil {

    private static final Map<Class<?>, Set<Class<?>>> compatiblePrimitiveWrapper = createMapping();

    static Map<Class<?>, Set<Class<?>>> createMapping() {
        Map<Class<?>, Set<Class<?>>> mapping = new HashMap<>();
        mapping.put(byte.class, asSet(Byte.class));
        mapping.put(boolean.class, asSet(Boolean.class));
        mapping.put(char.class, asSet(Character.class));
        mapping.put(double.class, asSet(Byte.class, Short.class, Integer.class, Float.class, Double.class));
        mapping.put(float.class, asSet(Byte.class, Short.class, Integer.class, Float.class));
        mapping.put(int.class, asSet(Byte.class, Short.class, Integer.class));
        mapping.put(long.class, asSet(Byte.class, Short.class, Integer.class, Long.class));
        mapping.put(short.class, asSet(Byte.class, Short.class));
        return Collections.unmodifiableMap(mapping);
    }

    private static Set<Class<?>> asSet(Class<?>... classes) {
        Set<Class<?>> set = new HashSet<>(classes.length);
        for (Class<?> c : classes) {
            set.add(c);
        }
        return Collections.unmodifiableSet(set);
    }

    public static boolean hasCompatiblePrimitiveWrapper(Class<?> type, Class<?> typeTo) {
        Set<Class<?>> compatibles = compatiblePrimitiveWrapper.get(type);
        return compatibles != null && compatibles.contains(typeTo);
    }

    public static void checkSetParameters(Object[] params, Method method) {
        if( method == null) {
            throw new IllegalStateException("Internal Error: Got null method");
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        if (params == null) {
            if (paramTypes.length != 0) {
                throw new IllegalArgumentException("Wrong number of parameters for "
                        + " method: " + method);
            }
        } else {
            if (paramTypes.length != params.length) {
                throw new IllegalArgumentException("Wrong number of parameters for "
                        + " method: " + method);
            }
            int index = 0 ;
            for (Class<?> type : paramTypes) {
                if (params[index] == null) {
                    if (type.isPrimitive()) {
                        throw new IllegalArgumentException("Parameter type mismatch for method "
                                + method.getName() + ".  Attempt to set a null value for Arg["
                                + index + "]. Expected a value of type: " + type.getName());
                    }
                } else if (type.isPrimitive()) {
                    Set<Class<?>> compatibles = compatiblePrimitiveWrapper.get(type);
                    if (! compatibles.contains(params[index].getClass())) {
                        throw new IllegalArgumentException("Parameter type mismatch for method "
                                + method.getName() + ".  Arg["
                                + index + "] type: " + params[index].getClass().getName()
                                + " is not compatible with the expected type: " + type.getName());
                    }
                } else if (! type.isAssignableFrom(params[index].getClass())) {
                    throw new IllegalArgumentException("Parameter type mismatch for method "
                            + method.getName() + ".  Arg["
                            + index + "] type: " + params[index].getClass().getName()
                            + " does not match the expected type: " + type.getName());
                }
                index++;
            }
        }
    }

}
