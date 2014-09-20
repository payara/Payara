/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.util;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.logging.LogDomains;

import java.lang.reflect.Method;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes setter methods on java beans.
 *
 * @author Qingqing Ouyang, Binod P.G, Sivakumar Thyagarajan
 */
public final class SetMethodAction implements PrivilegedExceptionAction {

    private Object bean;
    private Set props;
    private Method[] methods;

    private static final Logger logger =
            LogDomains.getLogger(SetMethodAction.class, LogDomains.RSR_LOGGER);
    private final static Locale locale = Locale.getDefault();

    /**
     * Accepts java bean object and properties to be set.
     */
    public SetMethodAction(Object bean, Set props) {
        this.bean = bean;
        this.props = props;
    }

    /**
     * Executes the setter methods in the java bean.
     */
    public Object run() throws Exception {
        Iterator it = props.iterator();
        methods = bean.getClass().getMethods();
        while (it.hasNext()) {
            EnvironmentProperty prop = (EnvironmentProperty) it.next();
            String propName = prop.getName();
            Class type = getTypeOf(prop);
            //If there were no getter, use the EnvironmentProperty's
            //property type
            if (type == null) {
                type = Class.forName(prop.getType());
            }

            if (prop.getResolvedValue() != null &&
                    prop.getResolvedValue().trim().length() != 0) {
                Method meth = getMutatorMethod(propName, type);
                
                if (meth == null) {
                    //log WARNING, deployment can continue.
                    logger.log(Level.WARNING, "rardeployment.no_setter_method",
                            new Object[]{prop.getName(), bean.getClass().getName()});
                } else {
                    try {
                        if(logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, "Invoking" + meth + " on "
                                + bean.getClass().getName() + "with " +
                                "value [" + prop.getResolvedValueObject().getClass()
                                + "  , " + getFilteredPropValue(prop) + " ] ");
                        }
                        meth.invoke(bean, new Object[]{prop.getResolvedValueObject()});
                    } catch (IllegalArgumentException ia) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "IllegalException while trying to set "
                                    + prop.getName() + " and value " + getFilteredPropValue(prop),
                                    ia + " on an instance of " + bean.getClass()
                                    + " -- trying again with the type from bean");
                        }
                        boolean prevBoundsChecking = EnvironmentProperty.isBoundsChecking();
                        try {
                            EnvironmentProperty.setBoundsChecking(false);
                            prop.setType(type.getName());
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "2nd try :: Invoking" + meth + " on "
                                        + bean.getClass().getName() + "with value ["
                                        + prop.getResolvedValueObject().getClass()
                                        + "  , " + getFilteredPropValue(prop) + " ] ");
                            }
                            meth.invoke(bean, new Object[]{prop.getResolvedValueObject()});
                        } catch (Exception e) {
                            handleException(e, prop, bean);
                        } finally {
                            //restore boundsChecking
                            EnvironmentProperty.setBoundsChecking(prevBoundsChecking);
                        }
                    } catch (Exception ex) {
                        handleException(ex, prop, bean);
                    }
                }
            }
        }
        return null;
    }

    private void handleException(Exception ex, EnvironmentProperty prop, Object bean) throws ConnectorRuntimeException {
        logger.log(Level.WARNING, "rardeployment.exception_on_invoke_setter",
                new Object[]{prop.getName(), getFilteredPropValue(prop),
                        ex.getMessage()});
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Exception while trying to set "
                    + prop.getName() + " and value " + getFilteredPropValue(prop),
                    ex + " on an instance of " + bean.getClass());
        }
        throw(ConnectorRuntimeException)
                (new ConnectorRuntimeException(ex.getMessage()).initCause(ex));
    }

    private static String getFilteredPropValue(EnvironmentProperty prop) {
        if (prop == null)
            return "null";

        String propname = prop.getName();
        if (propname.toLowerCase(locale).contains("password"))
            return "********";

        return (prop.getResolvedValue());
    }


    /**
     * Retrieves the appropriate setter method in the resurce adapter java bean
     * class
     */
    private Method getMutatorMethod(String propertyName, Class type) {
        String setterMethodName = "set" + getCamelCasedPropertyName(propertyName);
        Method m = null;

        //Get all setter methods for property
        Method[] setterMethods = findMethod(setterMethodName);

        if (setterMethods.length == 1) {
            //Only one setter method for this property
            m = (Method) setterMethods[0];
        } else {
            //When more than one setter for the property, do type
            //checking to determine property
            //This check is very important, because the resource
            //adapter java-bean's methods might be overridden and calling
            //set over the wrong method will result in an exception
            for (int i = 0; i < setterMethods.length; i++) {
                Class[] paramTypes = setterMethods[i].getParameterTypes();
                if (paramTypes.length > 0) {
                    if (paramTypes[0].equals(type) && paramTypes.length == 1) {
                        if(logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, "Method [ " + methods[i] +
                                " ] matches with the right arg type");
                        }
                        m = setterMethods[i];
                    }
                }
            }
        }

        if (m != null) {
            return m;
        } else {
            logger.log(Level.WARNING, "no.such.method",
                    new Object[]{setterMethodName, bean.getClass().getName()});
            return null;
        }
    }

    /**
     * Use a property's accessor method in the resource adapter
     * javabean to get the Type of the property
     * <p/>
     * This helps in ensuring that the type as coded in the java-bean
     * is used while setting values on a java-bean instance,
     * rather than on the values specified in ra.xml
     */
    private Class getTypeOf(EnvironmentProperty prop) {
        String name = prop.getName();
        Method accessorMeth = getAccessorMethod(name);
        if (accessorMeth != null) {
            return accessorMeth.getReturnType();
        }
        //not having a getter is not a WARNING.
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "method.name.nogetterforproperty",
                    new Object[]{prop.getName(), bean.getClass()});
        }
        return null;
    }

    /**
     * Gets the accessor method for a property
     */
    private Method getAccessorMethod(String propertyName){
       String getterName = "get" + getCamelCasedPropertyName(propertyName);
       Method[] getterMethods = findMethod(getterName);
       if (getterMethods.length > 0) {
           return getterMethods[0];
       } else {
           getterName = "is"+getCamelCasedPropertyName(propertyName);
           Method[] getterMethodsWithIsPrefix = findMethod(getterName);

           if(getterMethodsWithIsPrefix.length > 0 &&
                   (getterMethodsWithIsPrefix[0].getReturnType().equals(java.lang.Boolean.class) ||
                   getterMethodsWithIsPrefix[0].getReturnType().equals(boolean.class))){

               return getterMethodsWithIsPrefix[0];
           }else{
               return null;
           }
       }
    }

    /**
     * Finds methods in the resource adapter java bean class with the same name
     * RA developers could inadvertently not camelCase getters and/or setters
     * and this implementation of findMethod returns both camelCased and non-Camel
     * cased methods.
     */
    private Method[] findMethod(String methodName) {
        List<Method> matchedMethods = new ArrayList<Method>();

        //check for CamelCased Method(s)
        for (int i = 0; i < this.methods.length; i++) {
            if (methods[i].getName().equals(methodName)) {
                matchedMethods.add(methods[i]);
            }
        }

        //check for nonCamelCased Method(s)
        for (int i = 0; i < this.methods.length; i++) {
            if (methods[i].getName().equalsIgnoreCase(methodName)) {
                matchedMethods.add(methods[i]);
            }
        }
        Method[] methodArray = new Method[matchedMethods.size()];
        return matchedMethods.toArray(methodArray);
    }

    /**
     * Returns camel-cased version of a propertyName. Used to construct
     * correct accessor and mutator method names for a give property.
     */
    private String getCamelCasedPropertyName(String propertyName) {
        return propertyName.substring(0, 1).toUpperCase(locale) +
                propertyName.substring(1);
    }

}
