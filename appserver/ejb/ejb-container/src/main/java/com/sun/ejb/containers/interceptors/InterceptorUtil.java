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

/*
 */
package com.sun.ejb.containers.interceptors;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;


/**
 */
public class InterceptorUtil {

    private static Map<Class, Set<Class>> compatiblePrimitiveWrapper
         = new HashMap<Class, Set<Class>>();

     static {

         Set<Class> smallerPrimitiveWrappers = null;

         smallerPrimitiveWrappers = new HashSet<Class>();
         smallerPrimitiveWrappers.add(Byte.class);
         compatiblePrimitiveWrapper.put(byte.class, smallerPrimitiveWrappers);

         smallerPrimitiveWrappers = new HashSet<Class>();
         smallerPrimitiveWrappers.add(Boolean.class);
         compatiblePrimitiveWrapper.put(boolean.class, smallerPrimitiveWrappers);

         smallerPrimitiveWrappers = new HashSet<Class>();
         smallerPrimitiveWrappers.add(Character.class);
         compatiblePrimitiveWrapper.put(char.class, smallerPrimitiveWrappers);

         smallerPrimitiveWrappers = new HashSet<Class>();
         smallerPrimitiveWrappers.add(Byte.class);
         smallerPrimitiveWrappers.add(Short.class);
         smallerPrimitiveWrappers.add(Integer.class);
         smallerPrimitiveWrappers.add(Float.class);
         smallerPrimitiveWrappers.add(Double.class);
         compatiblePrimitiveWrapper.put(double.class, smallerPrimitiveWrappers);

         smallerPrimitiveWrappers = new HashSet<Class>();
         smallerPrimitiveWrappers.add(Byte.class);
         smallerPrimitiveWrappers.add(Short.class);
         smallerPrimitiveWrappers.add(Integer.class);
         smallerPrimitiveWrappers.add(Float.class);
         compatiblePrimitiveWrapper.put(float.class, smallerPrimitiveWrappers);

         smallerPrimitiveWrappers = new HashSet<Class>();
         smallerPrimitiveWrappers.add(Byte.class);
         smallerPrimitiveWrappers.add(Short.class);
         smallerPrimitiveWrappers.add(Integer.class);
         compatiblePrimitiveWrapper.put(int.class, smallerPrimitiveWrappers);

         smallerPrimitiveWrappers = new HashSet<Class>();
         smallerPrimitiveWrappers.add(Byte.class);
         smallerPrimitiveWrappers.add(Short.class);
         smallerPrimitiveWrappers.add(Integer.class);
         smallerPrimitiveWrappers.add(Long.class);
         compatiblePrimitiveWrapper.put(long.class, smallerPrimitiveWrappers);

         smallerPrimitiveWrappers = new HashSet<Class>();
         smallerPrimitiveWrappers.add(Byte.class);
         smallerPrimitiveWrappers.add(Short.class);
         compatiblePrimitiveWrapper.put(short.class, smallerPrimitiveWrappers);
     }

    public static boolean hasCompatiblePrimitiveWrapper(Class type, Class typeTo) {
        Set<Class> compatibles = compatiblePrimitiveWrapper.get(type);
        return compatibles.contains(typeTo);
    }

    public static void checkSetParameters(Object[] params, Method method) {

        if( method != null) {

            Class[] paramTypes = method.getParameterTypes();
            if ((params == null) && (paramTypes.length != 0)) {
                throw new IllegalArgumentException("Wrong number of parameters for "
                        + " method: " + method);
            }
            if (paramTypes.length != params.length) {
                throw new IllegalArgumentException("Wrong number of parameters for "
                        + " method: " + method);
            }
            int index = 0 ;
            for (Class type : paramTypes) {
                if (params[index] == null) {
                    if (type.isPrimitive()) {
                        throw new IllegalArgumentException("Parameter type mismatch for method "
                                + method.getName() + ".  Attempt to set a null value for Arg["
                            + index + "]. Expected a value of type: " + type.getName());
                    }
                } else if (type.isPrimitive()) {
                    Set<Class> compatibles = compatiblePrimitiveWrapper.get(type);
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
        } else {
            throw new IllegalStateException("Internal Error: Got null method");
        }

    }



}
