/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glassfish.extras.grizzly;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;

/**
 * Utils for introspection and reflection
 */
public final class IntrospectionUtils {

    private static final Logger LOGGER = Grizzly.logger(IntrospectionUtils.class);

    /**
     * Find a method with the right name If found, call the method ( if param is
     * int or boolean we'll convert value to the right type before) - that means
     * you can have setDebug(1).
     */
    public static boolean setProperty(Object o, String name, String value) {
        if (dbg > 1) {
            d("setProperty(" + o.getClass() + " " + name + "=" + value + ")");
        }

        String setter = "set" + capitalize(name);

        try {
            Method methods[] = findMethods(o.getClass());
            Method setPropertyMethodVoid = null;
            Method setPropertyMethodBool = null;

            // First, the ideal case - a setFoo( String ) method
            for (int i = 0; i < methods.length; i++) {
                Class<?> paramT[] = methods[i].getParameterTypes();
                if (setter.equals(methods[i].getName()) && paramT.length == 1
                        && "java.lang.String".equals(paramT[0].getName())) {

                    methods[i].invoke(o, value);
                    return true;
                }
            }

            // Try a setFoo ( int ) or ( boolean )
            for (int i = 0; i < methods.length; i++) {
                boolean ok = true;
                if (setter.equals(methods[i].getName())
                        && methods[i].getParameterTypes().length == 1) {

                    // match - find the type and invoke it
                    Class<?> paramType = methods[i].getParameterTypes()[0];
                    Object params[] = new Object[1];

                    // Try a setFoo ( int )
                    if ("java.lang.Integer".equals(paramType.getName())
                            || "int".equals(paramType.getName())) {
                        try {
                            params[0] = new Integer(value);
                        } catch (NumberFormatException ex) {
                            ok = false;
                        }
                        // Try a setFoo ( long )
                    } else if ("java.lang.Long".equals(paramType.getName())
                            || "long".equals(paramType.getName())) {
                        try {
                            params[0] = new Long(value);
                        } catch (NumberFormatException ex) {
                            ok = false;
                        }

                        // Try a setFoo ( boolean )
                    } else if ("java.lang.Boolean".equals(paramType.getName())
                            || "boolean".equals(paramType.getName())) {
                        params[0] = Boolean.valueOf(value);

                        // Try a setFoo ( InetAddress )
                    } else if ("java.net.InetAddress".equals(paramType.getName())) {
                        try {
                            params[0] = InetAddress.getByName(value);
                        } catch (UnknownHostException exc) {
                            d("Unable to resolve host name:" + value);
                            ok = false;
                        }

                        // Unknown type
                    } else {
                        d("Unknown type " + paramType.getName());
                    }

                    if (ok) {
                        methods[i].invoke(o, params);
                        return true;
                    }
                }

                // save "setProperty" for later
                if ("setProperty".equals(methods[i].getName())) {
                    if (methods[i].getReturnType() == Boolean.TYPE) {
                        setPropertyMethodBool = methods[i];
                    } else {
                        setPropertyMethodVoid = methods[i];
                    }

                }
            }

            // Ok, no setXXX found, try a setProperty("name", "value")
            if (setPropertyMethodBool != null || setPropertyMethodVoid != null) {
                Object params[] = new Object[2];
                params[0] = name;
                params[1] = value;
                if (setPropertyMethodBool != null) {
                    try {
                        return (Boolean) setPropertyMethodBool.invoke(o, params);
                    } catch (IllegalArgumentException biae) {
                        //the boolean method had the wrong
                        //parameter types. lets try the other
                        if (setPropertyMethodVoid != null) {
                            setPropertyMethodVoid.invoke(o, params);
                            return true;
                        } else {
                            throw biae;
                        }
                    }
                } else {
                    setPropertyMethodVoid.invoke(o, params);
                    return true;
                }
            }

        } catch (IllegalArgumentException ex2) {
            LOGGER.log(Level.INFO, "IAE " + o + " " + name + " " + value, ex2);
        } catch (SecurityException ex1) {
            if (dbg > 0) {
                d("SecurityException for " + o.getClass() + " " + name + "="
                        + value + ")");
            }
            if (dbg > 1) {
                LOGGER.log(Level.WARNING, "", ex1);
            }
        } catch (IllegalAccessException iae) {
            if (dbg > 0) {
                d("IllegalAccessException for " + o.getClass() + " " + name
                        + "=" + value + ")");
            }
            if (dbg > 1) {
                LOGGER.log(Level.WARNING, "", iae);
            }
        } catch (InvocationTargetException ie) {
            if (dbg > 0) {
                d("InvocationTargetException for " + o.getClass() + " " + name
                        + "=" + value + ")");
            }
            if (dbg > 1) {
                LOGGER.log(Level.WARNING, "", ie);
            }
        }
        return false;
    }

    /**
     * Reverse of Introspector.decapitalize
     */
    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }



    // -------------------- other utils --------------------

    @SuppressWarnings("UnusedDeclaration")
    public static void clear() {
        objectMethods.clear();
    }

    static Map<Class<?>, Method[]> objectMethods =
            new HashMap<Class<?>, Method[]>();

    @SuppressWarnings("unchecked")
    public static Method[] findMethods(Class<?> c) {
        Method methods[] = objectMethods.get(c);
        if (methods != null) {
            return methods;
        }

        methods = c.getMethods();
        objectMethods.put(c, methods);
        return methods;
    }

    // debug --------------------
    static final int dbg = 0;

    static void d(String s) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "IntrospectionUtils: {0}", s);
        }
    }
}
