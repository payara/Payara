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

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import com.sun.logging.LogDomains;
import com.sun.enterprise.connectors.ConnectorRuntime;

import java.util.logging.*;

/**
 * A simple class to get the properties of a ConnectionDefinition class , that
 * could be overridden by the administrator during deployment.
 * 
 * @author Sivakumar Thyagarjan
 */
public class ConnectionDefinitionUtils {

    private final static Logger _logger= LogDomains.getLogger(ConnectionDefinitionUtils.class,LogDomains.RSR_LOGGER);
    private final static Locale locale = Locale.getDefault();

    /**
	 * Gets the properties of the Java bean connection definition class that
	 * have setter methods defined
	 * 
	 * @param connectionDefinitionClassName
	 *                     The Connection Definition Java bean class for which
	 *                     overrideable properties are required.
	 * @return A Set of properties that have a setter method defined in the
	 *                Connection Definition class
	 */
    public static Set getConnectionDefinitionProperties(String connectionDefinitionClassName) {
        TreeMap propertySet= new TreeMap();
        try {
            Method[] methods=
                ConnectorRuntime.getRuntime().getConnectorClassLoader().loadClass(
                        connectionDefinitionClassName).getMethods();
            for (int i= 0; i < methods.length; i++) {
                //Method starts with "set" and has only one parameter and has
                // a
                // valid argument
                if (isValidSetterMethod(methods[i])) {
                    String name= methods[i].getName();
                    String propertyName=
                        name.substring(
                            (name.indexOf("set") + "set".length()),
                            name.length());
                    propertySet.put(propertyName, propertyName);
                }
            }
        } catch (SecurityException e) {
            handleException(e, connectionDefinitionClassName);
        } catch (ClassNotFoundException e) {
            handleException(e, connectionDefinitionClassName);
        }
        ignoreOracle10gProperties(connectionDefinitionClassName, propertySet);
        return propertySet.keySet();
    }

    /**
     * Oracle 10g (10.x) jdbc driver has two properties,
     * <i>connectionCachingEnabled</i>, <i>fastConnectionFailoverEnabled</i> when set with
     * their default values(false) throws exceptions. This workaround (IT 2525)  will remove
     * these properties during connection pool creation.
     * @param className DataSource Classname
     * @param map list of properties
     */
    private static void ignoreOracle10gProperties(String className, Map map){

           Set<String> oracleClasses = new HashSet<String>();
           oracleClasses.add("oracle.jdbc.pool.oracledatasource");
           oracleClasses.add("oracle.jdbc.pool.oracleconnectionpooldatasource");
           oracleClasses.add("oracle.jdbc.xa.client.oraclexadatasource");
           oracleClasses.add("oracle.jdbc.xa.oraclexadataSource");
           if(oracleClasses.contains(className.toLowerCase(locale))){
               boolean property1Removed = removePropertyFromMap("connectionCachingEnabled", map);
               boolean property2Removed = removePropertyFromMap("fastConnectionFailoverEnabled",map);
               if(property1Removed || property2Removed){
                   if (_logger.isLoggable(Level.FINE)) {
                       _logger.log(Level.FINE, "Removing properties 'connectionCachingEnabled',"
                               + " 'fastConnectionFailoverEnabled' from Datasource : " + className);
                   }
               }
           }
       }

    private static boolean removePropertyFromMap(String property, Map map){
        boolean  entryRemoved = false ;
        Iterator iterator = map.keySet().iterator();
        while(iterator.hasNext()){
            String key = (String)iterator.next();
            if(property.equalsIgnoreCase(key)){
                iterator.remove();
                entryRemoved = true;
            }
        }
        return entryRemoved;
    }

    private static boolean isValidSetterMethod(Method method) {
        return (
            (method.getName().startsWith("set"))
                && (method.getParameterTypes().length == 1)
                && (isValidArgumentType(method)));
    }

    private static boolean isValidArgumentType(Method method) {
        Class[] parameters= method.getParameterTypes();
        boolean isValid= true;
        for (int i= 0; i < parameters.length; i++) {
            Class param= parameters[i];
            if (!(param.isPrimitive() || param.equals(String.class)))
                return false;
        }
        return isValid;
    }

    /**
	 * Gets the properties of the Java bean connection definition class that
	 * have setter methods defined and the default values as provided by the
	 * Connection Definition java bean developer.<br>
         * This util method is used to get properties of jdbc-data-source<br>
         * To get Connection definition properties for Connector Connection Pool,
         * use ConnectorRuntime.getMCFConfigProperties()<br>
         * When the connection definition class is not found, standard JDBC
         * properties (of JDBC 3.0 Specification) will be returned.<br>
	 * 
	 * @param connectionDefinitionClassName
	 *                     The Connection Definition Java bean class for which
	 *                     overrideable properties are required.
	 * @return Map<String, Object> String represents property name
         * and Object is the defaultValue that is a primitive type or String
	 */
    public static Map<String, Object> getConnectionDefinitionPropertiesAndDefaults(String connectionDefinitionClassName, 
            String resType) {
        TreeMap hm= new TreeMap();
        if(connectionDefinitionClassName == null || ("").equals(connectionDefinitionClassName)) {
            if(resType != null && resType.equals(ConnectorConstants.JAVA_SQL_DRIVER)) {
                addDefaultJDBCDriverProperties(hm);
            } else {
                addDefaultJDBCProperties(hm);
            }
            return hm;            
        }
        Set s= getConnectionDefinitionProperties(connectionDefinitionClassName);
	Class connectionDefinitionClass;
        try {
            connectionDefinitionClass=
                ConnectorRuntime.getRuntime().getConnectorClassLoader().loadClass(connectionDefinitionClassName);
            Object obj= connectionDefinitionClass.newInstance();
            for (Iterator iter= s.iterator(); iter.hasNext();) {
                String property= (String) iter.next();
                Object defaultVal= null;
                try {
                    Method m=
                        connectionDefinitionClass.getMethod(
                            "get" + property,
                            new Class[] {});
                    defaultVal= m.invoke(obj, new Object[] {});
                    //ignore these exceptions. Some drivers have a setter but
                    // no getters for properties [example the password property
                    // in the OracleDataSource
                } catch (NoSuchMethodException e) {
                    //ignore
                } catch (IllegalArgumentException e) {
                    //ignore
                } catch (InvocationTargetException e) {
                    //ignore
                }
                //If the property does not have a corresponding getter method,
                // a null is placed as the default value.
                hm.put(property, defaultVal);
            }
            if(resType != null && resType.equals("java.sql.Driver")) {
                addDefaultJDBCDriverProperties(hm);
            }
        } catch (ClassNotFoundException e) {
            handleException(e, connectionDefinitionClassName);
            //since the specified connectionDefinitionClassName is not found, 
            //return the standard JDBC properties
            if(resType != null && resType.equals("java.sql.Driver")) {
                addDefaultJDBCDriverProperties(hm);
            } else {
                addDefaultJDBCProperties(hm);
            }
        } catch (InstantiationException e) {
            handleException(e, connectionDefinitionClassName);
        } catch (IllegalAccessException e) {
            handleException(e, connectionDefinitionClassName);
        } catch (SecurityException e) {
            handleException(e, connectionDefinitionClassName);
        }
        return hm;
    }
    
    private static void addDefaultJDBCProperties(Map map){
        String[] defaultProperties = {
             "databaseName", "serverName", "portNumber", "networkProtocol",
             "user", "password", "roleName", "datasourceName" };

        //assuming that the provided map is not null
        for(int i=0; i<defaultProperties.length; i++){
            map.put(defaultProperties[i],null);
        }
    }

    private static void addDefaultJDBCDriverProperties(Map map){
        String[] defaultProperties = {"URL", "user", "password"};

        //assuming that the provided map is not null
        for(int i=0; i<defaultProperties.length; i++){
            if(!containsProperty(defaultProperties[i], map)) {
                map.put(defaultProperties[i], null);
            }
        }
    }

    private static boolean containsProperty(String prop, Map map) {
        boolean propFound = false;
        Set<String> keys = map.keySet();
        for(String key : keys) {
            if(prop.equalsIgnoreCase(key)) {
                propFound = true;
                break;
            }
        }
        return propFound;
    }
    
    private static void handleException(Exception ex, String className) {
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE, "Exception while trying to find properties of class [ "+className+" ]", ex);
        }
        Object param[] = new Object[]{className, ex.getMessage()};
        _logger.log(Level.SEVERE,"error.finding.properties", param);
    }

    //test code
    public static void main(String[] args) {

        //oracle.jdbc.xa.client.OracleXADataSource
        //com.pointbase.jdbc.jdbcDataSource
        Map m=
            ConnectionDefinitionUtils
                .getConnectionDefinitionPropertiesAndDefaults(
                "sun.jdbc.odbc.ee.DataSource", "javax.sql.DataSource");

        Set<Map.Entry> elements = m.entrySet();
        for(Map.Entry element : elements) {
            System.out.println(element.getKey() + " : " + element.getValue());
        }
    }
}
