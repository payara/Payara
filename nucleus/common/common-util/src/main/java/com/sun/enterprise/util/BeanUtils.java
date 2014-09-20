/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Static methods which are handy to manipulate java beans
 *
 * @author martinmares
 */
public class BeanUtils {
    
    /** Loads all getters to the Map.
     */
    public static Map<String, Object> beanToMap(Object bean) throws InvocationTargetException {
        if (bean == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<String, Object>();
        Collection<Method> getters = getGetters(bean);
        for (Method method : getGetters(bean)) {
            try {
                result.put(toAttributeName(method), method.invoke(bean));
            } catch (IllegalAccessException ex) {
                //Checked - can not happen
            } catch (IllegalArgumentException ex) {
                //Checked - can not happen
            }
        }
        return result;
    }
    
    /** Sets values from map to provided bean.
     * 
     * @param bean Set to its setters
     * @param data key is attribute name and value is value to set
     * @param ignoreNotExistingSetter if {@code false} and data contains key which
     *        does not point to any setter then IllegalArgumentException will be thrown
     */
    public static void mapToBean(Object bean, Map<String, Object> data, boolean ignoreNotExistingSetter) 
            throws InvocationTargetException, IllegalArgumentException {
        if (data == null || bean == null) {
            return;
        }
        Class clazz = bean.getClass();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            try {
                Method mtd = getSetter(bean, entry.getKey());
                if (mtd == null) {
                    if (!ignoreNotExistingSetter) {
                        throw new IllegalArgumentException();
                    }
                    continue;
                }
                mtd.invoke(bean, entry.getValue());
            } catch (IllegalAccessException ex) {
            } 
        }
    }
    
    public static Collection<Method> getGetters(Object bean) {
        if (bean == null) {
            return null;
        }
        Collection<Method> result = new ArrayList<Method>();
        for (Method method : bean.getClass().getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            if (method.getParameterTypes().length == 0) {
                if ((method.getName().length() > 3 && method.getName().startsWith("get"))
                        || (method.getName().length() > 2 && method.getName().startsWith("is"))) {
                    result.add(method);
                }
            }
        }
        return result;
    }
    
    public static Collection<Method> getSetters(Object bean) {
        if (bean == null) {
            return null;
        }
        Collection<Method> result = new ArrayList<Method>();
        for (Method method : bean.getClass().getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            if (method.getParameterTypes().length == 1) {
                if (method.getName().length() > 3 && method.getName().startsWith("set")) {
                    result.add(method);
                }
            }
        }
        return result;
    }
    
    /** Extract attribute name from getter or setter.
     * 
     * @return IllegalArgumentException if method is not getter or setter.
     */
    public static String toAttributeName(Method m) throws IllegalArgumentException {
        String name = m.getName();
        String result;
        if (name.startsWith("get") || name.startsWith("set")) {
            result = name.substring(3);
        } else if (name.startsWith("is")) {
            result = name.substring(2);
        } else {
            throw new IllegalArgumentException();
        }
        if (result.length() == 0) {
            throw new IllegalArgumentException();
        }
        result = Character.toLowerCase(result.charAt(0)) + result.substring(1);
        return result;
    }
    
    public static Method getSetter(Object bean, String attributeName) {
        String methodName = "set" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1);
        for (Method m : bean.getClass().getMethods()) {
            if (m.getParameterTypes().length == 1 && m.getName().equals(methodName)) {
                return m;
            }
        }
        return null;
    }
    
}
