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

package org.glassfish.resources.custom.factory;


import javax.naming.spi.ObjectFactory;
import javax.naming.*;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Enumeration;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Locale;


public class JavaBeanFactory implements Serializable, ObjectFactory {

    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference reference = (Reference) obj;

        try {
            Class beanClass;
            try {
                beanClass = Thread.currentThread().getContextClassLoader().loadClass(reference.getClassName());
            } catch (ClassNotFoundException e) {
                throw new NamingException("Unable to load class : " + reference.getClassName());
            }

            Object bean = beanClass.newInstance();

            BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
            PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();

            Enumeration enumeration = reference.getAll();

            while (enumeration.hasMoreElements()) {

                RefAddr ra = (RefAddr) enumeration.nextElement();
                String propertyName = ra.getType();
                String value = (String) ra.getContent();

                for (PropertyDescriptor desc : properties) {
                    if (desc.getName().equals(propertyName)) {
                        String type = desc.getPropertyType().getName();
                        Object result = null;

                        if(type != null){
                            type = type.toUpperCase(Locale.getDefault());
                            if(type.endsWith("INT") || type.endsWith("INTEGER")){
                                result =  Integer.valueOf(value);
                            } else if (type.endsWith("LONG")){
                                result = Long.valueOf(value);
                            } else if(type.endsWith("DOUBLE")){
                                result = Double.valueOf(value);
                            } else if(type.endsWith("FLOAT") ){
                                result = Float.valueOf(value);
                            } else if(type.endsWith("CHAR") || type.endsWith("CHARACTER")){
                                result = value.charAt(0);
                            } else if(type.endsWith("SHORT")){
                                result = Short.valueOf(value);
                            } else if(type.endsWith("BYTE")){
                                result = Byte.valueOf(value);
                            } else if(type.endsWith("BOOLEAN")){
                                result = Boolean.valueOf(value);
                            } else if(type.endsWith("STRING")){
                                result = value;
                            }
                        } else {
                            throw new NamingException("Unable to find the type of property : " + propertyName);
                        }

                        Method setter = desc.getWriteMethod();
                        if (setter != null) {
                            setter.invoke(bean, result);
                        } else {
                            throw new NamingException
                                    ("Unable to find the setter method for property : "+ propertyName);
                        }
                        break;
                    }
                }
            }
            return bean;
        } catch(Exception e){
            NamingException ne = new NamingException("Unable to instantiate JavaBean");
            ne.setRootCause(e);
            throw ne;
        }
    }
}
