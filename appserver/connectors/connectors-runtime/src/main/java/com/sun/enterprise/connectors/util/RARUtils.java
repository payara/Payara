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
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This is a utility class to obtain the properties of a 
 * RA JavaBean housed in a RAR module/deployment dir, without exploding the RAR
 * contents. This method would be used by the admin-gui to configure
 * RA properties during RA deployment to a cluster.
 *  
 * @author Sivakumar Thyagarajan
 */
public class RARUtils {
    private final static Logger _logger = LogDomains.getLogger(RARUtils.class, LogDomains.RSR_LOGGER);
    private static StringManager localStrings = 
        StringManager.getManager( RARUtils.class );

    /**
     * Finds the properties of a RA JavaBean bundled in a RAR
     * without exploding the RAR
     * 
     * @param pathToDeployableUnit a physical,accessible location of the connector module.
     * [either a RAR for RAR-based deployments or a directory for Directory based deployments] 
     * @return A Map that is of <String RAJavaBeanPropertyName, String defaultPropertyValue>
     * An empty map is returned in the case of a 1.0 RAR 
     */
/* TODO V3
    public static Map getRABeanProperties (String pathToDeployableUnit) throws ConnectorRuntimeException {
        File f = new File(pathToDeployableUnit);
        if (!f.exists()){
            String i18nMsg = localStrings.getString(
                "rar_archive_not_found", pathToDeployableUnit);
            throw new ConnectorRuntimeException( i18nMsg );
        }
        if(f.isDirectory()) {
            return getRABeanPropertiesForDirectoryBasedDeployment(pathToDeployableUnit);
        } else {
            return getRABeanPropertiesForRARBasedDeployment(pathToDeployableUnit);
        }
    }
*/

/*
    private static Map getRABeanPropertiesForRARBasedDeployment(String rarLocation){
        ConnectorRARClassLoader jarCL = 
                            (new ConnectorRARClassLoader(rarLocation,  
                             ApplicationServer.getServerContext().getCommonClassLoader()));
        String raClassName = ConnectorDDTransformUtils.
                                    getResourceAdapterClassName(rarLocation);
        _logger.finer("RA class :  " + raClassName);
        Map hMap = new HashMap();
        try {
           hMap = extractRABeanProps(raClassName, jarCL);
        } catch (ClassNotFoundException e) {
            _logger.info(e.getMessage());
            _logger.log(Level.FINE, "Error while trying to find class " 
                            + raClassName + "in RAR at " + rarLocation, e);
        }
        return hMap;
    }
*/

/*
    private static Map getRABeanPropertiesForDirectoryBasedDeployment(
                    String directoryLocation) {
        Map hMap = new HashMap();
        //Use the deployment APIs to get the name of the resourceadapter
        //class through the connector descriptor
        try {
            ConnectorDescriptor cd = ConnectorDDTransformUtils.
                                getConnectorDescriptor(directoryLocation);
            String raClassName = cd.getResourceAdapterClass();
            
            File f = new File(directoryLocation);
            URLClassLoader ucl = new URLClassLoader(new URL[]{f.toURI().toURL()}, 
                                  ApplicationServer.getServerContext().getCommonClassLoader());
            hMap = extractRABeanProps(raClassName, ucl);
        } catch (IOException e) {
            _logger.info(e.getMessage());
            _logger.log(Level.FINE, "IO Error while trying to read connector" +
                   "descriptor to get resource-adapter properties", e);
        } catch (ClassNotFoundException e) {
            _logger.info(e.getMessage());
            _logger.log(Level.FINE, "Unable to find class while trying to read connector" +
                   "descriptor to get resource-adapter properties", e);
        } catch (ConnectorRuntimeException e) {
            _logger.info(e.getMessage());
            _logger.log(Level.FINE, "Error while trying to read connector" +
                   "descriptor to get resource-adapter properties", e);
        } catch (Exception e) {
            _logger.info(e.getMessage());
            _logger.log(Level.FINE, "Error while trying to read connector" +
                   "descriptor to get resource-adapter properties", e);
        }
        return hMap;
    }
*/

    /**
     * A valid resource adapter java bean property should either be one of the
     * following  
     * 1. A Java primitive or a primitve wrapper
     * 2. A String
     */
    public static boolean isValidRABeanConfigProperty(Class clz) {
        return (clz.isPrimitive() || clz.equals(String.class) 
                        || isPrimitiveWrapper(clz));
    }

    /**
     * Determines if a class is one of the eight java primitive wrapper classes
     */
    private static boolean isPrimitiveWrapper(Class clz) {
        return (clz.equals(Boolean.class) || clz.equals(Character.class) 
                 || clz.equals(Byte.class) || clz.equals(Short.class) 
                 || clz.equals(Integer.class) || clz.equals(Long.class)
                 || clz.equals(Float.class) || clz.equals(Double.class));
    }

   /**
     * Prepares the name/value pairs for ActivationSpec. <p>
     * Rule: <p>
     * 1. The name/value pairs are the union of activation-config on 
     *    standard DD (message-driven) and runtime DD (mdb-resource-adapter) 
     * 2. If there are duplicate property settings, the value in runtime 
     *    activation-config will overwrite the one in the standard 
     *    activation-config.
     */
    public static Set getMergedActivationConfigProperties(EjbMessageBeanDescriptor msgDesc) {
        
        Set mergedProps = new HashSet();
        Set runtimePropNames = new HashSet();
        
        Set runtimeProps = msgDesc.getRuntimeActivationConfigProperties();
        if(runtimeProps != null){
            Iterator iter = runtimeProps.iterator();
            while (iter.hasNext()) {
                EnvironmentProperty entry = (EnvironmentProperty) iter.next();
                mergedProps.add(entry);
                String propName = (String) entry.getName();
                runtimePropNames.add(propName);
            }
        }
        
        Set standardProps = msgDesc.getActivationConfigProperties();
        if(standardProps != null){
            Iterator iter = standardProps.iterator();
            while (iter.hasNext()) {
                EnvironmentProperty entry = (EnvironmentProperty) iter.next();
                String propName = (String) entry.getName();
                if (runtimePropNames.contains(propName))
                    continue;
                mergedProps.add(entry);
            }
        }
        
        return mergedProps;
        
    }

    public static Class loadClassFromRar(String rarName, String beanClassName) throws ConnectorRuntimeException{
        String rarLocation = getRarLocation(rarName);
        return loadClass(rarLocation, beanClassName);
    }

    /**
     * given the rar name, location of rar will be returned
     * @param rarName resource-adapter name
     * @return location of resource-adapter
     */
    private static String getRarLocation(String rarName) throws ConnectorRuntimeException{
        return ConnectorsUtil.getLocation(rarName);
    }


    /**
     * given the location of .rar (archive / exploded dir), the specified class will be loaded
     * @param pathToDeployableUnit location of .rar (archive / exploded dir)
     * @param beanClassName class that has to be loaded
     * @return loaded class
     * @throws ConnectorRuntimeException when unable to load the class
     */
    private static Class loadClass(String pathToDeployableUnit, String beanClassName) throws ConnectorRuntimeException {
        Class cls = null;

        ClassLoader cl = getClassLoader(pathToDeployableUnit);

        try {
            //Only if RA is a 1.5 RAR, we need to get RA JavaBean properties, else
            //return an empty map.

            if (beanClassName != null && beanClassName.trim().length() != 0) {
                cls = cl.loadClass(beanClassName);
            }
            return cls;
        } catch (ClassNotFoundException e) {
            _logger.info(e.getMessage());
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Unable to find class while trying to read connector" +
                    "descriptor to get resource-adapter properties", e);
            }
            ConnectorRuntimeException cre = new ConnectorRuntimeException("unable to find class : " + beanClassName);
            cre.setStackTrace(e.getStackTrace());
            throw cre;
        }
    }

    /**
     * based on the provided file type (dir or archive) appropriate class-loader will be selected
     * @param file file (dir/ archive)
     * @return classloader that is capable of loading the .rar
     * @throws ConnectorRuntimeException when unable to load the .rar
     */
    private static ClassLoader getClassLoader(String file) throws ConnectorRuntimeException {
        ClassLoader cl = null;
        File f = new File(file);
        validateRARLocation(f);
        try {
            ClassLoader commonClassLoader =
                    ConnectorRuntime.getRuntime().getClassLoaderHierarchy().getCommonClassLoader();
            if (f.isDirectory()) {
                List<URL> urls = new ArrayList<URL>();
                urls.add(f.toURI().toURL());
                appendURLs(urls, f);
                cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), commonClassLoader);
            } else {
                cl = new ConnectorRARClassLoader(file, commonClassLoader);
            }
            return cl;
        } catch (IOException ioe) {
            _logger.info(ioe.getMessage());
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "IO Error while trying to read connector"
                        + "descriptor to get resource-adapter properties", ioe);
            }
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                    "unable to read connector descriptor from : " + file);
            cre.setStackTrace(ioe.getStackTrace());
            throw cre;
        }
    }

    private static void appendURLs(List<URL> urls, File f) throws MalformedURLException {
        for (File file : f.listFiles()) {
            if (file.getName().toUpperCase(Locale.getDefault()).endsWith(".JAR")) {
                urls.add(file.toURI().toURL());
            } else if (file.isDirectory()) {
                appendURLs(urls, file);
            }
        }
    }

    /**
     * check whether the provided location is valid
     * @param f location where the .rar is present
     * @throws ConnectorRuntimeException
     */
    private static void validateRARLocation(File f) throws ConnectorRuntimeException {
        if (!f.exists()) {
            String i18nMsg = localStrings.getString(
                    "rar_archive_not_found", f);
            throw new ConnectorRuntimeException(i18nMsg);
        }
    }

    
}
