/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2024] Payara Foundation and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.loader;


import com.sun.enterprise.util.CULoggerInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CachingReflectionUtil {
    private static final Logger logger = CULoggerInfo.getLogger();

    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final Map<String, Field> fieldCache = new ConcurrentHashMap<>();

    public static Class<?> getClassFromCache(String className, ClassLoader classLoader) {
        var cls = classCache.computeIfAbsent(className, k -> {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                logger.log(Level.FINE, "Class not found: " + className, e);
                return null;
            }
        });
        if (cls != null && cls.getClassLoader() == classLoader) {
            classCache.remove(cls.getName());
        }
        return cls;
    }

    public static Method getMethodFromCache(Class<?> cls, String methodName, boolean isPrivate, Class<?>... parameterTypes) {
        return methodCache.computeIfAbsent(methodName, k -> {
            try {
                if (isPrivate) {
                    Method method = cls.getDeclaredMethod(methodName, parameterTypes);
                    method.setAccessible(true);
                    return method;
                } else {
                    return cls.getMethod(methodName, parameterTypes);
                }
            } catch (NoSuchMethodException e) {
                logger.log(Level.FINE, "Method not found: " + methodName, e);
                return null;
            }
        });
    }

    public static Field getFieldFromCache(Class<?> cls, String fieldName, boolean isPrivate) {
        return fieldCache.computeIfAbsent(fieldName, k -> {
            try {
                if (isPrivate) {
                    Field field = cls.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field;
                } else {
                    return cls.getField(fieldName);
                }
            } catch (NoSuchFieldException e) {
                logger.log(Level.FINE, "Field not found: " + fieldName, e);
                return null;
            }
        });
    }
}
