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

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.connectors.*;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.module.ConnectorApplication;
import com.sun.logging.LogDomains;
import com.sun.appserv.connectors.internal.api.*;

import java.util.logging.*;
import java.util.*;
import java.lang.*;
import java.lang.reflect.*;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;


/**
 *  This is an util class containing methods for parsing connector 
 *  configurations present in ra.xml. 
 *
 *  @author Srikanth P
 *
 */
public class ConnectorConfigParserUtils {

    private final static Logger _logger = LogDomains.getLogger(ConnectorConfigParserUtils.class, LogDomains.RSR_LOGGER);

    /**
     *  Default constructor.
     *
     */
    public ConnectorConfigParserUtils() {
    }

    /**
     *  Merges the properties obtained by introspecting the javabean and the 
     *  properties present in ra.xml for the corresponding javabean.
     *
     *  @param ddVals Properties obtained from ra.xml for the javabean
     *  @param introspectedVals Properties obtained by introspecting javabean
     *  @return Merged Properties present in ra.xml and introspected properties 
     *          of javabean.
     *
     */
    public Properties mergeProps(Set ddVals,
                                 Properties introspectedVals)
    {
        Properties mergedVals = new Properties(introspectedVals);

        if(ddVals != null) {
            Object[] ddProps = ddVals.toArray();

            String name = null;
            String value = null;
            for (int i = 0; i < ddProps.length; i++) {
                name = ((ConnectorConfigProperty )ddProps[i]).getName();
                value =((ConnectorConfigProperty )ddProps[i]).getValue();
                mergedVals.setProperty(name,value);
            }
        }

        return mergedVals;
    }

    /**
     *  Merges the datatype of properties obtained by introspecting the 
     *  javabean and the datatypes of properties present in ra.xml for 
     *  the corresponding javabean. It is a Properties object consisting of
     *  property name and the property data type.
     *
     *  @param ddVals Properties obtained from ra.xml for the javabean
     *  @param introspectedVals Properties obtained by 
     *         introspecting javabean which consist of property name as key
     *         and datatype as the value. 
     *  @return Merged Properties present in ra.xml and introspected properties 
     *          of javabean. Properties consist of property name as the key
     *          and datatype as the value.
     *
     */

    public Properties mergePropsReturnTypes(Set ddVals, 
                                 Properties introspectedVals)
    {
        Properties mergedVals = new Properties(introspectedVals);

        if(ddVals != null) {
            Object[] ddProps = ddVals.toArray();

            String name = null;
            String value = null;
            for (int i = 0; i < ddProps.length; i++) {
                name = ((ConnectorConfigProperty )ddProps[i]).getName();
                value = ((ConnectorConfigProperty )ddProps[i]).getType();
                mergedVals.setProperty(name,value);
            }
        }

        return mergedVals;
    }
    
    public Properties introspectJavaBean(String className, Set ddPropsSet)
                            throws ConnectorRuntimeException {
        return introspectJavaBean(className, ddPropsSet, false, null);
    }
    
    public Properties introspectJavaBean(String className, Set ddPropsSet, 
                    boolean associateResourceAdapter, String resourceAdapterName)
                       throws ConnectorRuntimeException {
        Class loadedClass = loadClass(className, resourceAdapterName);

        Object loadedInstance = instantiate(loadedClass);
        try {
            if (associateResourceAdapter) {
                ActiveResourceAdapter activeRA = ConnectorRegistry.getInstance().
                                  getActiveResourceAdapter(resourceAdapterName);
                if (activeRA == null) {
                    //Check and Load RAR
                    ConnectorRuntime.getRuntime().loadDeferredResourceAdapter(
                                                      resourceAdapterName);
                    activeRA = ConnectorRegistry.getInstance().
                                  getActiveResourceAdapter(resourceAdapterName);
                }
                
                //Associate RAR
                if (activeRA instanceof ActiveOutboundResourceAdapter) {
                    ResourceAdapter raInstance =  activeRA.getResourceAdapter();
                    if (loadedInstance instanceof ResourceAdapterAssociation) {
                        ((ResourceAdapterAssociation)loadedInstance).
                                             setResourceAdapter(raInstance);
                    }
                }
            }
        } catch (Exception e) {
            _logger.log(Level.WARNING,
                            "rardeployment.error_associating_ra",e);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "Exception while associating the resource adapter"
                        + "to the JavaBean", e);
            }
        }
        return introspectJavaBean(loadedInstance, ddPropsSet);
    }
    
    

    /**
     * Introspects the javabean and returns only the introspected properties 
     * not present in the configuration in ra.xml for the corresponding 
     * javabean. If no definite value is obtained while introspection of 
     * a method empty string is taken as the  value.
     *
     * @param javaBeanInstance bean
     * @param ddPropsSet Set of Properties present in configuration in ra.xml for
     *                the corresponding javabean.
     * @return Introspected properties not present in the configuration in 
     *         ra.xml for the corresponding javabean.
     * @throws ConnectorRuntimeException if the Class could not be loaded 
     *         or instantiated. 
     */

    public Properties introspectJavaBean(
        Object javaBeanInstance ,Set ddPropsSet) throws ConnectorRuntimeException 
    {
        Class loadedClass = javaBeanInstance.getClass();

        Method[] methods = loadedClass.getMethods();
        Properties props = new Properties();
        String name = null;
        String value = null;
        Object[] ddProps = null;
        if(ddPropsSet != null) {
            ddProps = ddPropsSet.toArray();
        }

        for(int i=0; i<methods.length;++i) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("Method -> " + methods[i].getName() + ":" + methods[i].getReturnType());
            }
            if(isProperty(methods[i]) && !presentInDDProps(methods[i],ddProps)
                                      && isValid(methods[i], loadedClass)) {  
                name = getPropName(methods[i]);
                value = getPropValue(methods[i], loadedClass, javaBeanInstance);
                props.setProperty(name,value);
            }
        }
        return props;
    }

    /**
     * Introspects the javabean and returns only the introspected properties 
     * and their datatypes not present in the configuration in ra.xml for 
     * the corresponding javabean.  
     *
     * @param className Name of the class to be introspected.
     * @param ddPropsSet Set of Properties present in configuration in ra.xml for
     *                the corresponding javabean.
     * @return Introspected properties and their datatype not present in the 
     *         configuration in  ra.xml for the corresponding javabean. The 
     *         properties consist of property name as the key and datatype as
     *         the value
     * @throws ConnectorRuntimeException if the Class could not be loaded
     */

    public Properties introspectJavaBeanReturnTypes(
        String className,Set ddPropsSet, String rarName) throws ConnectorRuntimeException
    {

        Class loadedClass = loadClass(className, rarName);
        Method[] methods = loadedClass.getMethods();
        Properties props = new Properties();
        String name = null;
        String value = null;
        Object[] ddProps = null;
        if(ddPropsSet != null) {
            ddProps = ddPropsSet.toArray();
        }

        for(int i=0; i<methods.length;++i) {
            if(isProperty(methods[i])&&!presentInDDProps(methods[i],ddProps)) {
                name = getPropName(methods[i]);
                value = getPropType(methods[i]);
                if(value != null) {
                    props.setProperty(name,value);
                }
            }
        }
        return props;
    }
    /**
     * Checks whether the property pertaining to the method is already presenti
     * in the array of Properties passed as second argument. 
     * The properties already present in ra.xml for the corresponding 
     * javabean is passed as the second argument. 
     */

    private boolean presentInDDProps(Method method,Object[] ddProps) {

        String name = null;
        String ddPropName = null;
        int length = "set".length();
        if(method != null) {
            name = method.getName().substring(length);
        }
        for(int i=0; name != null && ddProps != null && i<ddProps.length;++i) {
            ddPropName = ((ConnectorConfigProperty )ddProps[i]).getName();
            if(name.equalsIgnoreCase(ddPropName) == true) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the property is valid or not.
     */
    private boolean isValid(Method setMethod, Class loadedClass) {
        Method getMethod = correspondingGetMethod( setMethod, loadedClass);
        if (getMethod != null) {
            return RARUtils.isValidRABeanConfigProperty(getMethod.getReturnType());
        } else {
            return false;
        }
    }

    /**
     * Checks whether the method pertains to a valid javabean property.
     * i.e it check whether the method starts with "set" and it has only 
     * one parameter. It more than one parameter is present it is taken as 
     * not a property
     * 
     */

    private boolean  isProperty(Method method) {

        if(method == null) {
            return false;
        }
        String methodName = method.getName();
        Class[] parameterTypes = method.getParameterTypes();
        if(methodName.startsWith("set") && 
           parameterTypes != null       && parameterTypes.length == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the property name of the method passed. It strips the first three 
     * charaters (size of "set") of the method name and converts the first 
     * character (for the string after stripping) to upper case and returns 
     * that string.
     *
     */
     
    private String getPropName(Method method) {

        if(method == null) {
            return null;
        }
        String methodName = method.getName();
        int length = "set".length();
        String retValue = 
            methodName.substring(length,length+1).toUpperCase(Locale.getDefault()) +
            methodName.substring(length+1);
        return retValue;
    }

    /** 
     * Returns the getXXX() or isXXX() for the setXXX method passed.
     * XXX is the javabean property.
     * Check is made if there are no parameters for the getXXX() and isXXX() 
     * methods. If there is any parameter, null is returned.
     */

    private Method correspondingGetMethod(Method setMethod, 
                                          Class loadedClass) {

        Method[] allMethods = loadedClass.getMethods();
        int length = "set".length();
        String methodName = setMethod.getName();
        Class[] parameterTypes = null;
        String[] possibleGetMethodNames = new String[2]; 
        possibleGetMethodNames[0] = "is"+methodName.substring(length);
        possibleGetMethodNames[1] = "get"+methodName.substring(length);

        for(int i = 0;i < allMethods.length;++i) {
            if(allMethods[i].getName().equals(possibleGetMethodNames[0]) || 
               allMethods[i].getName().equals(possibleGetMethodNames[1])) {
                parameterTypes = allMethods[i].getParameterTypes();
                if(parameterTypes != null && parameterTypes.length == 0) {
                    return allMethods[i];
                }
            }
        }
        return  null;
    }

    /**
     * Invokes the method passed and returns the value obtained. If method 
     * invocation fails empty string is returned. If the return type is not 
     * of Wrapper class of the primitive types, empty string is returned.
     */

    private String getPropValue(Method method, 
                   Class loadedClass, Object loadedInstance) {

        Object retValue = null;
        Method getMethod = correspondingGetMethod(method, loadedClass);

        if(getMethod != null) {
            try {
                retValue = getMethod.invoke(loadedInstance, (java.lang.Object[])null);
            } catch (IllegalAccessException ie) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE,
                            "rardeployment.illegalaccess_error", loadedClass.getName());
                }
            } catch (InvocationTargetException ie) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE,
                            "Failed to invoke the method", loadedClass.getName());
                }
            }
        }
        return convertToString(retValue); 
    }

    private String getPropType(Method method) {

        Class[] parameterTypeClass = method.getParameterTypes();
        if(parameterTypeClass.length != 1) {
            return null;
        }
        if(parameterTypeClass[0].isPrimitive() || 
                  parameterTypeClass[0].getName().equals("java.lang.String")) {
            return parameterTypeClass[0].getName();
        } else {
            return null;
        }
    }

    /**
     * Converts the object to String if it belongs to Wrapper class of primitive
     * type or a string itself. For all other types empty String is returned. 
     */

    private String convertToString(Object obj) {
        if(obj == null) {
            return "";
        }

        if(obj instanceof String) {
            return (String)obj;
        }else if( obj instanceof Integer ||
              obj instanceof Float   ||
              obj instanceof Long    ||
              obj instanceof Double  || 
              obj instanceof Character  || 
              obj instanceof Boolean  || 
              obj instanceof Byte  || 
              obj instanceof Short ) {  
            return String.valueOf(obj);
        } else {
            return "";
        }
    }
  

    /**
     * Loads and instantiates the class 
     * Throws ConnectorRuntimeException if loading or instantiation fails.
     */

    private Class loadClass(String className, String resourceAdapterName)
                   throws ConnectorRuntimeException 
    {
        Class loadedClass = null;
        try {
            if(ConnectorsUtil.belongsToSystemRA(resourceAdapterName)){
                ClassLoader classLoader = ConnectorRuntime.getRuntime().getConnectorClassLoader();
                loadedClass = classLoader.loadClass(className);
            }else{
                //try loading via ClassLoader of the RAR from ConnectorRegistry
                ConnectorApplication app = ConnectorRegistry.getInstance().getConnectorApplication(resourceAdapterName);

                if(app == null ){
                    _logger.log(Level.FINE, "unable to load class [ " + className + " ] of RAR " +
                            "[ " + resourceAdapterName + " ]" +
                            " from server instance, trying other instances' deployments");
                    //try loading via RARUtils
                    loadedClass = RARUtils.loadClassFromRar(resourceAdapterName, className);
                }else{
                    loadedClass = app.getClassLoader().loadClass(className);
                }
            }
        } catch (ClassNotFoundException e1) {
            _logger.log(Level.FINE, "rardeployment.class_not_found",className);
            throw new ConnectorRuntimeException("Class Not Found : " + className);
        }
        return loadedClass;
    }
    /**
     * Instantiates the class 
     */

    private Object instantiate(Class loadedClass) 
                   throws ConnectorRuntimeException 
    {
        try {
            return loadedClass.newInstance();
        } catch(InstantiationException ie) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "rardeployment.class_instantiation_error", loadedClass.getName());
            }
            throw new ConnectorRuntimeException(
                     "Could not instantiate class : " + loadedClass.getName());
        } catch (IllegalAccessException ie) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "rardeployment.illegalaccess_error", loadedClass.getName());
            }
            throw new ConnectorRuntimeException(
                       "Couldnot access class : "+loadedClass.getName());
        } 
    }
}
