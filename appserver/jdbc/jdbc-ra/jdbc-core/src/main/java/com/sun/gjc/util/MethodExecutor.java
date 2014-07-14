/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.gjc.util;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.logging.LogDomains;

import javax.resource.ResourceException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Execute the methods based on the parameters.
 *
 * @author Binod P.G
 * @version 1.0, 02/07/23
 */
public class MethodExecutor implements java.io.Serializable {

    private static Logger _logger;

    static {
        _logger = LogDomains.getLogger(MethodExecutor.class, LogDomains.RSR_LOGGER);
    }

    private boolean debug = false;
    
    private final static String newline = System.getProperty("line.separator");

    private static StringManager sm = StringManager.getManager(
            DataSourceObjectBuilder.class);

    /**
     * Exceute a simple set Method.
     *
     * @param value  Value to be set.
     * @param method <code>Method</code> object.
     * @param obj    Object on which the method to be executed.
     * @throws <code>ResourceException</code>,
     *          in case of the mismatch of parameter values or
     *          a security violation.
     */
    public void runJavaBeanMethod(String value, Method method, Object obj) throws ResourceException {
        if (value == null || value.trim().equals("")) {
            return;
        }
        try {
            Class[] parameters = method.getParameterTypes();
            if (parameters.length == 1) {
                Object[] values = new Object[1];
                values[0] = convertType(parameters[0], value);
                method.invoke(obj, values);
            }
        } catch (IllegalAccessException iae) {
            _logger.log(Level.SEVERE, "jdbc.exc_jb_val", value);
            _logger.log(Level.SEVERE, "", iae);
            String msg = sm.getString("me.access_denied", method.getName());
            throw new ResourceException(msg);
        } catch (IllegalArgumentException ie) {
            _logger.log(Level.SEVERE, "jdbc.exc_jb_val", value);
            _logger.log(Level.SEVERE, "", ie);
            String msg = sm.getString("me.illegal_args", method.getName());
            throw new ResourceException(msg);
        } catch (InvocationTargetException ite) {
            _logger.log(Level.SEVERE, "jdbc.exc_jb_val", value);
            _logger.log(Level.SEVERE, "", ite);
            String msg = sm.getString("me.access_denied", method.getName());
            throw new ResourceException(msg);
        }
    }

    /**
     * Executes the method.
     *
     * @param method <code>Method</code> object.
     * @param obj    Object on which the method to be executed.
     * @param values Parameter values for executing the method.
     * @throws <code>ResourceException</code>,
     *          in case of the mismatch of parameter values or
     *          a security violation.
     */
    public void runMethod(Method method, Object obj, Vector values) throws ResourceException {
        try {
            Class[] parameters = method.getParameterTypes();
            if (values.size() != parameters.length) {
                return;
            }
            Object[] actualValues = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                String val = (String) values.get(i);
                if (val.trim().equals("NULL")) {
                    actualValues[i] = null;
                } else {
                    actualValues[i] = convertType(parameters[i], val);
                }
            }
            method.invoke(obj, actualValues);
        } catch (IllegalAccessException iae) {
            _logger.log(Level.SEVERE, "jdbc.exc_jb_val", values);
            _logger.log(Level.SEVERE, "", iae);
            String msg = sm.getString("me.access_denied", method.getName());
            throw new ResourceException(msg);
        } catch (IllegalArgumentException ie) {
            _logger.log(Level.SEVERE, "jdbc.exc_jb_val", values);
            _logger.log(Level.SEVERE, "", ie);
            String msg = sm.getString("me.illegal_args", method.getName());
            throw new ResourceException(msg);
        } catch (InvocationTargetException ite) {
            _logger.log(Level.SEVERE, "jdbc.exc_jb_val", values);
            _logger.log(Level.SEVERE, "", ite);
            String msg = sm.getString("me.access_denied", method.getName());
            throw new ResourceException(msg);
        }
    }

    /**
     * Converts the type from String to the Class type.
     *
     * @param type      Class name to which the conversion is required.
     * @param parameter String value to be converted.
     * @return Converted value.
     * @throws <code>ResourceException</code>,
     *          in case of the mismatch of parameter values or
     *          a security violation.
     */
    private Object convertType(Class type, String parameter) throws ResourceException {
        try {
            String typeName = type.getName();
            if (typeName.equals("java.lang.String") || typeName.equals("java.lang.Object")) {
                return parameter;
            }

            if (typeName.equals("int") || typeName.equals("java.lang.Integer")) {
                return new Integer(parameter);
            }

            if (typeName.equals("short") || typeName.equals("java.lang.Short")) {
                return new Short(parameter);
            }

            if (typeName.equals("byte") || typeName.equals("java.lang.Byte")) {
                return new Byte(parameter);
            }

            if (typeName.equals("long") || typeName.equals("java.lang.Long")) {
                return new Long(parameter);
            }

            if (typeName.equals("float") || typeName.equals("java.lang.Float")) {
                return new Float(parameter);
            }

            if (typeName.equals("double") || typeName.equals("java.lang.Double")) {
                return new Double(parameter);
            }

            if (typeName.equals("java.math.BigDecimal")) {
                return new java.math.BigDecimal(parameter);
            }

            if (typeName.equals("java.math.BigInteger")) {
                return new java.math.BigInteger(parameter);
            }

            if (typeName.equals("boolean") || typeName.equals("java.lang.Boolean")) {
                return Boolean.valueOf(parameter);
            }
            
            if (typeName.equals("java.util.Properties")) {
                Properties p = stringToProperties(parameter);
                if (p!= null) return p;
            }

            return parameter;
        } catch (NumberFormatException nfe) {
            _logger.log(Level.SEVERE, "jdbc.exc_nfe", parameter);
            String msg = sm.getString("me.invalid_param", parameter);
            throw new ResourceException(msg);
        }
    }

    public Object invokeMethod(Object object, String methodName, 
            Class<?>[] valueTypes, Object... values) throws ResourceException {
        Object returnValue = null;
        Method actualMethod = null;
        try {
            actualMethod = object.getClass().getMethod(methodName, valueTypes);
        } catch (NoSuchMethodException ex) {
            throw new ResourceException(ex);
        } catch (SecurityException ex) {
            throw new ResourceException(ex);
        }
        if (actualMethod != null) {
            try {
                returnValue = actualMethod.invoke(object, values);
            } catch (IllegalAccessException ex) {
                throw new ResourceException(ex);
            } catch (IllegalArgumentException ex) {
                throw new ResourceException(ex);
            } catch (InvocationTargetException ex) {
                throw new ResourceException(ex);
            }
        }
        return returnValue;
    }

    private Properties stringToProperties(String parameter)
    {
         if (parameter == null) return null;
         String s = parameter.trim();
         if (!((s.startsWith("(") && s.endsWith(")")))) {
            return null; // not a "( .... )" syntax
         }
         s = s.substring(1,s.length()-1);
         s = s.replaceAll("(?<!\\\\),",newline); // , -> \n
         s = s.replaceAll("\\\\,",",");  // escape-"," -> ,

         Properties p = new Properties();
         Properties prop = new Properties();
         try {
            p.load(new java.io.StringBufferInputStream(s));
         } catch (java.io.IOException ex) {
            if (_logger.isLoggable(Level.FINEST)) {
               _logger.log(Level.FINEST, "Parsing string to properties: {0}", ex.getMessage());
            }
            return null;
         }
         // cleanup trailing whitespace in value
         for (java.util.Enumeration propKeys = p.propertyNames();
               propKeys.hasMoreElements();) {
             String tmpKey = (String)propKeys.nextElement();
             String tmpValue = p.getProperty(tmpKey);
             // Trim spaces
             tmpValue = tmpValue.trim();
             // Quoted string.
             if (tmpValue.length() > 1 && tmpValue.startsWith("\"")
                     && tmpValue.endsWith("\"")) {
                tmpValue = tmpValue.substring(1,tmpValue.length()-1);
             }
             prop.put(tmpKey, tmpValue);
         }
         if (_logger.isLoggable(Level.FINEST)) {
               _logger.log(Level.FINEST, "Parsing string to properties: {0}size:{1}", new Object[]{prop, prop.size()});
         }
         return prop;
    }
}
