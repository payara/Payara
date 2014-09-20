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

package org.apache.naming.factory;

import java.util.Hashtable;
import java.util.Enumeration;
import javax.naming.Name;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.RefAddr;
import javax.naming.spi.ObjectFactory;
import org.apache.naming.ResourceRef;

import java.beans.Introspector;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;

import java.lang.reflect.Method;

/**
 * Object factory for any Resource conforming to the JavaBean spec.
 * 
 * <p>This factory can be configured in a <code>&lt;DefaultContext&gt;</code>
 * or <code>&lt;Context&gt;</code> element in your <code>conf/server.xml</code>
 * configuration file.  An example of factory configuration is:</p>
 * <pre>
 * &lt;Resource name="jdbc/myDataSource" auth="SERVLET"
 *   type="oracle.jdbc.pool.OracleConnectionCacheImpl"/&gt;
 * &lt;ResourceParams name="jdbc/myDataSource"&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;factory&lt;/name&gt;
 *     &lt;value&gt;org.apache.naming.factory.BeanFactory&lt;/value&gt;
 *   &lt;/parameter&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;driverType&lt;/name&gt;
 *     &lt;value&gt;thin&lt;/value&gt;
 *   &lt;/parameter&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;serverName&lt;/name&gt;
 *     &lt;value&gt;hue&lt;/value&gt;
 *   &lt;/parameter&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;networkProtocol&lt;/name&gt;
 *     &lt;value&gt;tcp&lt;/value&gt;
 *   &lt;/parameter&gt; 
 *   &lt;parameter&gt;
 *     &lt;name&gt;databaseName&lt;/name&gt;
 *     &lt;value&gt;XXXX&lt;/value&gt;
 *   &lt;/parameter&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;portNumber&lt;/name&gt;
 *     &lt;value&gt;NNNN&lt;/value&gt;
 *   &lt;/parameter&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;user&lt;/name&gt;
 *     &lt;value&gt;XXXX&lt;/value&gt;
 *   &lt;/parameter&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;password&lt;/name&gt;
 *     &lt;value&gt;XXXX&lt;/value&gt;
 *   &lt;/parameter&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;maxLimit&lt;/name&gt;
 *     &lt;value&gt;5&lt;/value&gt;
 *   &lt;/parameter&gt;
 * &lt;/ResourceParams&gt;
 * </pre>
 *
 * @author <a href="mailto:aner at ncstech.com">Aner Perez</a>
 */
public class BeanFactory
    implements ObjectFactory {

    // ----------------------------------------------------------- Constructors


    // -------------------------------------------------------------- Constants


    // ----------------------------------------------------- Instance Variables


    // --------------------------------------------------------- Public Methods


    // -------------------------------------------------- ObjectFactory Methods


    /**
     * Create a new Bean instance.
     * 
     * @param obj The reference object describing the Bean
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                    Hashtable<?,?> environment)
        throws NamingException {

        if (obj instanceof ResourceRef) {

            try {
                
                Reference ref = (Reference) obj;
                String beanClassName = ref.getClassName();
                Class<?> beanClass = null;
                ClassLoader tcl = 
                    Thread.currentThread().getContextClassLoader();
                if (tcl != null) {
                    try {
                        beanClass = tcl.loadClass(beanClassName);
                    } catch(ClassNotFoundException e) {
                        throw (NamingException)
                            new NamingException().initCause(e);
                    }
                } else {
                    try {
                        beanClass = Class.forName(beanClassName);
                    } catch(ClassNotFoundException e) {
                        throw (NamingException)
                            new NamingException().initCause(e);
                    }
                }
                if (beanClass == null) {
                    throw new NamingException
                        ("Class not found: " + beanClassName);
                }
                
                BeanInfo bi = Introspector.getBeanInfo(beanClass);
                PropertyDescriptor[] pda = bi.getPropertyDescriptors();
                
                Object bean = beanClass.newInstance();
                
                Enumeration<RefAddr> e = ref.getAll();
                while (e.hasMoreElements()) {
                    
                    RefAddr ra = e.nextElement();
                    String propName = ra.getType();
                    
                    if (propName.equals(Constants.FACTORY) ||
                        propName.equals("scope") || propName.equals("auth")) {
                        continue;
                    }
                    
                    String value = (String)ra.getContent();
                    
                    Object[] valueArray = new Object[1];
                    
                    int i = 0;
                    for (i = 0; i<pda.length; i++) {

                        if (pda[i].getName().equals(propName)) {

                            Class<?> propType = pda[i].getPropertyType();

                            if (propType.equals(String.class)) {
                                valueArray[0] = value;
                            } else if (propType.equals(Character.class) 
                                       || propType.equals(char.class)) {
                                valueArray[0] = Character.valueOf(value.charAt(0));
                            } else if (propType.equals(Byte.class) 
                                       || propType.equals(byte.class)) {
                                valueArray[0] = Byte.valueOf(value);
                            } else if (propType.equals(Short.class) 
                                       || propType.equals(short.class)) {
                                valueArray[0] = Short.valueOf(value);
                            } else if (propType.equals(Integer.class) 
                                       || propType.equals(int.class)) {
                                valueArray[0] = Integer.valueOf(value);
                            } else if (propType.equals(Long.class) 
                                       || propType.equals(long.class)) {
                                valueArray[0] = Long.valueOf(value);
                            } else if (propType.equals(Float.class) 
                                       || propType.equals(float.class)) {
                                valueArray[0] = Float.valueOf(value);
                            } else if (propType.equals(Double.class) 
                                       || propType.equals(double.class)) {
                                valueArray[0] = Double.valueOf(value);
                            } else {
                                throw new NamingException
                                    ("String conversion for property type '"
                                     + propType.getName() + "' not available");
                            }
                            
                            Method setProp = pda[i].getWriteMethod();
                            if (setProp != null) {
                                setProp.invoke(bean, valueArray);
                            } else {
                                throw new NamingException
                                    ("Write not allowed for property: " 
                                     + propName);
                            }

                            break;

                        }

                    }

                    if (i == pda.length) {
                        throw new NamingException
                            ("No set method found for property: " + propName);
                    }

                }

                return bean;

            } catch (java.beans.IntrospectionException ie) {
                throw new NamingException(ie.getMessage());
            } catch (java.lang.IllegalAccessException iae) {
                throw new NamingException(iae.getMessage());
            } catch (java.lang.InstantiationException ie2) {
                throw new NamingException(ie2.getMessage());
            } catch (java.lang.reflect.InvocationTargetException ite) {
                throw new NamingException(ite.getMessage());
            }

        } else {
            return null;
        }

    }
}
