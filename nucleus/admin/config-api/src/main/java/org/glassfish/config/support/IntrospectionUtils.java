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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package org.glassfish.config.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.config.util.ConfigApiLoggerInfo;

public final class IntrospectionUtils {

    private static final Logger LOGGER = ConfigApiLoggerInfo.getLogger();
    private static final int DEBUG_LEVEL = 0;

    @SuppressWarnings("unchecked")
    public static Method[] findMethods(Class<?> c) {
        return c.getMethods();
    }

    /**
     * Find a method with the right name If found, call the method ( if param is int or boolean we'll convert value to the right type before) - that means you
     * can have setDebug(1).
     *
     * @param o
     * @param name
     * @param value
     * @return
     */
    public static boolean setProperty(Object o, String name, String value) {
        if (DEBUG_LEVEL > 1) {
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

                    if (null == paramType.getName()) {
                        debug("Unknown type " + paramType.getName());
                    } else { 
                        switch (paramType.getName()) {
                            // Try a setFoo ( int )
                            case "java.lang.Integer":
                            case "int":
                                try {
                                    params[0] = new Integer(value);
                                } catch (NumberFormatException ex) {
                                    ok = false;
                                }
                                break;
                            // Try a setFoo ( long )
                            case "java.lang.Long":
                            case "long":
                                try {
                                    params[0] = new Long(value);
                                } catch (NumberFormatException ex) {
                                    ok = false;
                                }
                                break;
                            // Try a setFoo ( boolean )
                            case "java.lang.Boolean":
                            case "boolean":
                                params[0] = Boolean.valueOf(value);
                                break;
                            // Try a setFoo ( InetAddress )
                            case "java.net.InetAddress":
                                try {
                                    params[0] = InetAddress.getByName(value);
                                } catch (UnknownHostException exc) {
                                    debug("Unable to resolve host name:" + value);
                                    ok = false;
                                }
                                break;
                            default:
                                debug("Unknown type " + paramType.getName());
                                break;
                        }
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
            LOGGER.log(Level.INFO, "IAE " + o + " " + name + " " + value, ex2);
        } catch (SecurityException ex1) {
            if (DEBUG_LEVEL > 0) {
                debug("SecurityException for " + o.getClass() + " " + name + "=" + value + ")");
            }
            if (DEBUG_LEVEL > 1) {
                ex1.printStackTrace();
            }
        } catch (IllegalAccessException iae) {
            if (DEBUG_LEVEL > 0) {
                debug("IllegalAccessException for " + o.getClass() + " " + name + "=" + value + ")");
            }
            if (DEBUG_LEVEL > 1) {
                iae.printStackTrace();
            }
        } catch (InvocationTargetException ie) {
            if (DEBUG_LEVEL > 0) {
                debug("InvocationTargetException for " + o.getClass() + " " + name + "=" + value + ")");
            }
            if (DEBUG_LEVEL > 1) {
                ie.printStackTrace();
            }
        }
        return false;
    }

    private static void debug(String s) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "IntrospectionUtils: {0}", s);
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
