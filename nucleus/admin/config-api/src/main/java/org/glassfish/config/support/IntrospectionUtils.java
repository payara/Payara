/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.config.util.ConfigApiLoggerInfo;

public final class IntrospectionUtils {
    private static final Logger logger = ConfigApiLoggerInfo.getLogger();
    private static final int debugLevel = 0;

    @SuppressWarnings("unchecked")
    public static Method[] findMethods(Class<?> c) {
        return c.getMethods();
    }

    /**
     * Find a method with the right name If found, call the method ( if param is int or boolean we'll convert value to
     * the right type before) - that means you can have setDebug(1).
     */
    public static boolean setProperty(Object o, String name, String value) {
        if (debugLevel > 1) {
            debug("setProperty(" + o.getClass() + " " + name + "=" + value + ")");
        }
        String setter = "set" + capitalize(name);
        try {
            Method methods[] = findMethods(o.getClass());
            Method setPropertyMethodVoid = null;
            Method setPropertyMethodBool = null;
            // First, the ideal case - a setFoo( String ) method
            for (Method method : methods) {
                Class<?> paramTypes[] = method.getParameterTypes();
                if (setter.equals(method.getName()) && paramTypes.length == 1
                    && "java.lang.String".equals(paramTypes[0].getName())) {
                    method.invoke(o, value);
                    return true;
                }
            }
            // Try a setFoo ( int ) or ( boolean )
            for (Method method : methods) {
                boolean ok = true;
                if (setter.equals(method.getName())
                    && method.getParameterTypes().length == 1) {
                    // match - find the type and invoke it
                    Class<?> paramType = method.getParameterTypes()[0];
                    Object params[] = new Object[1];
                    // Try a setFoo ( int )
                    if ("java.lang.Integer".equals(paramType.getName()) || "int".equals(paramType.getName())) {
                        try {
                            params[0] = new Integer(value);
                        } catch (NumberFormatException ex) {
                            ok = false;
                        }
                        // Try a setFoo ( long )
                    } else if ("java.lang.Long".equals(paramType.getName()) || "long".equals(paramType.getName())) {
                        try {
                            params[0] = new Long(value);
                        } catch (NumberFormatException ex) {
                            ok = false;
                        }
                        // Try a setFoo ( boolean )
                    } else if ("java.lang.Boolean".equals(paramType.getName()) || "boolean".equals(paramType.getName())) {
                        params[0] = Boolean.valueOf(value);
                        // Try a setFoo ( InetAddress )
                    } else if ("java.net.InetAddress".equals(paramType.getName())) {
                        try {
                            params[0] = InetAddress.getByName(value);
                        } catch (UnknownHostException exc) {
                            debug("Unable to resolve host name:" + value);
                            ok = false;
                        }
                    } else {
                        debug("Unknown type " + paramType.getName());
                    }
                    if (ok) {
                        method.invoke(o, params);
                        return true;
                    }
                }
                // save "setProperty" for later
                if ("setProperty".equals(method.getName())) {
                    if (method.getReturnType().equals(Boolean.TYPE)) {
                        setPropertyMethodBool = method;
                    } else {
                        setPropertyMethodVoid = method;
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
            logger.log(Level.INFO, "IAE " + o + " " + name + " " + value, ex2);
        } catch (SecurityException ex1) {
            if (debugLevel > 0) {
                debug("SecurityException for " + o.getClass() + " " + name + "="
                    + value + ")");
            }
            if (debugLevel > 1) {
                ex1.printStackTrace();
            }
        } catch (IllegalAccessException iae) {
            if (debugLevel > 0) {
                debug("IllegalAccessException for " + o.getClass() + " " + name
                    + "=" + value + ")");
            }
            if (debugLevel > 1) {
                iae.printStackTrace();
            }
        } catch (InvocationTargetException ie) {
            if (debugLevel > 0) {
                debug("InvocationTargetException for " + o.getClass() + " " + name
                    + "=" + value + ")");
            }
            if (debugLevel > 1) {
                ie.printStackTrace();
            }
        }
        return false;
    }

    private static void debug(String s) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("IntrospectionUtils: " + s);
        }
    }

    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }


}
