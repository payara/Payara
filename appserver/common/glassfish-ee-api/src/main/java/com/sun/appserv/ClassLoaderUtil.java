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
// Portions Copyright 2017-2022 Payara Foundation and/or affiliates.
package com.sun.appserv;

import com.sun.logging.LogDomains;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;


/**
 * Provides utility functions related to URLClassLoaders or subclasses of it.
 *
 *                  W  A  R  N  I  N  G    
 *
 *This class uses undocumented, unpublished, private data structures inside
 *java.net.URLClassLoader and sun.misc.URLClassPath.  Use with extreme caution.
 *
 * @author tjquinn
 */
public class ClassLoaderUtil {
    
    // ### Names of classes and fields of interest for closing the loader's jar files
    
    private static final String URLCLASSLOADER_UCP_FIELD_NAME = "ucp"; // URLClassPath reference

    private static final String URLCLASSPATH_JDK9_CLASS_NAME = "jdk.internal.loader.URLClassPath";
    
    private static final String URLCLASSPATH_LOADERS_FIELD_NAME = "loaders"; // ArrayList of URLClassPath.Loader 
    private static final String URLCLASSPATH_URLS_FIELD_NAME = "unopenedUrls";
    private static final String URLCLASSPATH_LMAP_FIELD_NAME = "lmap"; // HashMap of String -> URLClassPath.Loader

    private static final String URLCLASSPATH_JARLOADER_INNER_CLASS_NAME = "$JarLoader";
    private static final String URLCLASSPATH_JARLOADER_JARFILE_FIELD_NAME = "jar";
    
    
    // ### Fields used during processing - they can be set up once and then used repeatedly 
    
    private static Field ucpField;      // The search path for classes and resources           - URLClassPath java.net.URLClassLoader.ucp;
    private static Field loadersField;  // The resulting search path of Loaders                - ArrayList<Loader> java.net.URLClassLoader.loaders = new ArrayList<>();
    private static Field unopenedUrlsField;     // The deque of unopened URLs                          - ArrayDeque<URL> urls = new ArrayDeque<>();
    private static Field lmapField;     //  Map of each URL opened to its corresponding Loader - HashMap<String, Loader> lmap = new HashMap<>();
    
    private static Class<?> jarLoaderInnerClass; // Nested class class used to represent a Loader of resources from a JAR URL. - static class JarLoader extends Loader
    private static Field jarFileField;        // JarFile JarLoader.jar;
    
    private static boolean initDone;
    
    
    /**
     * Releases resources held by the URLClassLoader.  Notably, close the jars
     * opened by the loader. This does not prevent the class loader from
     * continuing to return classes it has already resolved.
     *
     * @param classLoader the instance of URLClassLoader (or a subclass) 
     *
     * @return array of IOExceptions reporting jars that failed to close
     */
    public static void releaseLoader(URLClassLoader classLoader) {
        releaseLoader(classLoader, null);
    }
    
    /**
     * Releases resources held by the URLClassLoader.  Notably, close the jars
     * opened by the loader. This does not prevent the class loader from
     * continuing to return classes it has already resolved although that is not
     * what we intend to happen. Initializes and updates the Vector of 
     * jars that have been successfully closed.
     *
     * <p>
     * Any errors are logged.
     *
     * @param classLoader the instance of URLClassLoader (or a subclass) 
     * @param jarsClosed a Vector of Strings that will contain the names of jars 
     *        successfully closed; can be null if the caller does not need the information returned
     * 
     * @return array of IOExceptions reporting jars that failed to close; null
     *         indicates that an error other than an IOException occurred attempting to
     *         release the loader; empty indicates a successful release; non-empty 
     *         indicates at least one error attempting to close an open jar.
     */
    @SuppressWarnings("unchecked")
    public static IOException [] releaseLoader(URLClassLoader classLoader, Vector<String> jarsClosed) {
        
        IOException[] result = null;
        
        try { 
            init();

            /* Records all IOExceptions thrown while closing jar files. */
            Vector<IOException> ioExceptions = new Vector<IOException>();

            if (jarsClosed != null) {
                jarsClosed.clear();
            }

            Object ucp = ucpField.get(classLoader);
            List<?> loaders = (List<?>) loadersField.get(ucp);
            Collection<?> unopenedUrls = (Collection<?>) unopenedUrlsField.get(ucp);
            Map<String, Object> lmap = (Map<String, Object>) lmapField.get(ucp);

            /*
             * The unopenedUrls variable in the URLClassPath object holds unopened URLs that have not yet
             * been used to resolve a resource or load a class and, therefore, do
             * not yet have a loader associated with them.  Clear the stack so any
             * future requests that might incorrectly reach the loader cannot be 
             * resolved and cannot open a jar file after we think we've closed 
             * them all.
             */
            synchronized(unopenedUrls) {
                unopenedUrls.clear();
            }

            /*
             * Also clear the map of URLs to loaders so the class loader cannot use
             * previously-opened jar files - they are about to be closed.
             */
            synchronized(lmap) {
                lmap.clear();
            }

            /*
             * The URLClassPath object's path variable records the list of all URLs that are on
             * the URLClassPath's class path.  Leave that unchanged.  This might
             * help someone trying to debug why a released class loader is still used.
             * Because the stack and lmap are now clear, code that incorrectly uses a
             * the released class loader will trigger an exception if the 
             * class or resource would have been resolved by the class
             * loader (and no other) if it had not been released.  
             *
             * The list of URLs might provide some hints to the person as to where
             * in the code the class loader was set up, which might in turn suggest
             * where in the code the class loader needs to stop being used.
             * The URLClassPath does not use the path variable to open new jar 
             * files - it uses the urls Stack for that - so leaving the path variable
             * will not by itself allow the class loader to continue handling requests.
             */

            /*
             * For each loader, close the jar file associated with that loader.  
             *
             * The URLClassPath's use of loaders is sync-ed on the entire URLClassPath 
             * object.
             * 
             * NOTE: THIS IS ESSENTIALLY WHAT JDK 1.7'S URLCLASSLOADER.CLOSE() METHOD IS DOING AS WELL.
             *       THE CODE BELOW MIGHT NOT BE NEEDED ANYMORE.
             * 
             */
            synchronized (ucp) {
                for (Object loader : loaders) {
                    if (loader != null) {
                        /*
                         * If the loader is a JarLoader inner class and its jarFile
                         * field is non-null then try to close that jar file.  Add
                         * it to the list of closed files if successful.
                         */
                        if (jarLoaderInnerClass.isInstance(loader)) {
                            try {
                                JarFile jarFile = (JarFile) jarFileField.get(loader);
                                try {
                                    if (jarFile != null) {
                                        jarFile.close();
                                        if (jarsClosed != null) {
                                            jarsClosed.add(jarFile.getName());
                                        }
                                    }
                                } catch (IOException ioe) {
                                    /*
                                     *Wrap the IOException to identify which jar 
                                     *could not be closed and add it to the list
                                     *of IOExceptions to be returned to the caller.
                                     */
                                    String msg = getMessage("classloaderutil.errorClosingJar", jarFile.getName());
                                    IOException newIOE = new IOException(msg, ioe);
                                    ioExceptions.add(newIOE);
                                    
                                    /*
                                     *Log the error also.
                                     */
                                    getLogger().log(WARNING, msg, ioe);
                                }
                            } catch (Throwable thr) {
                                getLogger().log(WARNING, "classloaderutil.errorReleasingJarNoName", thr);
                            }
                        }
                    }
                }
                
                /*
                 * Now clear the loaders ArrayList.
                 */
                loaders.clear();
            }
            result = ioExceptions.toArray(new IOException[ioExceptions.size()]);
        } catch (Throwable thr) {
            getLogger().log(Level.WARNING, "classloaderutil.errorReleasingLoader", thr);
            result = null;
        }
        
        return result;
    }
    
    /**
     * Initializes the class.
     * <p>
     * Each utility method should invoke init() before doing their own work
     * to make sure the initialization is done.
     * @throws any Throwable detected during static init.
     */
    private static void init() throws Throwable {
        if (!initDone) {
            initForClosingJars();
            initDone = true;
        }
    }
    
    /**
     * Sets up variables used in closing a loader's jar files.
     *@ throws NoSuchFieldException in case a field of interest is not found where expected
     */
    private static void initForClosingJars() throws NoSuchFieldException {
        ucpField = getField(URLClassLoader.class, URLCLASSLOADER_UCP_FIELD_NAME);
        
        Class<?> ucpCLass = null;
        String jarLoaderClass = null;

        try {
            ucpCLass = Class.forName(URLCLASSPATH_JDK9_CLASS_NAME, false, Thread.currentThread().getContextClassLoader());
            jarLoaderClass = URLCLASSPATH_JDK9_CLASS_NAME + URLCLASSPATH_JARLOADER_INNER_CLASS_NAME;
        } catch (ClassNotFoundException ee) {
            // ignore for now will throw npe later
        }
        
        loadersField = getField(ucpCLass, URLCLASSPATH_LOADERS_FIELD_NAME);
        unopenedUrlsField = getField(ucpCLass, URLCLASSPATH_URLS_FIELD_NAME);
        lmapField = getField(ucpCLass, URLCLASSPATH_LMAP_FIELD_NAME);
        
        jarLoaderInnerClass = getInnerClass(ucpCLass, jarLoaderClass);
        jarFileField = getField(jarLoaderInnerClass, URLCLASSPATH_JARLOADER_JARFILE_FIELD_NAME);
    }
    
    /**
     * Retrieves a Field object for a given field on the specified class, having
     * set it accessible.
     * @param cls the Class on which the field is expected to be defined
     * @param fieldName the name of the field of interest
     * @throws NoSuchFieldException in case of any error retriving information about the field
     */
    private static Field getField(Class<?> cls, String fieldName) throws NoSuchFieldException {
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException nsfe) {
            NoSuchFieldException e = new NoSuchFieldException(getMessage("classloaderutil.errorGettingField", fieldName));
            e.initCause(nsfe);
            throw e;
        }

    }
    
    /**
     * Retrieves a given inner class definition from the specified outer class.
     *
     * @param cls the outer Class
     * @param innerClassName the fully-qualified name of the inner class of interest
     */
    private static Class<?> getInnerClass(Class<?> cls, String innerClassName) {
        Class<?> result = null;
        Class<?>[] innerClasses = cls.getDeclaredClasses();
        for (Class<?> c : innerClasses) {
            if (c.getName().equals(innerClassName)) {
                result = c;
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Returns the logger for the common utils component.
     * @return the Logger for this component
     */
    private static Logger getLogger() {
        return LogDomains.getLogger(ClassLoaderUtil.class, LogDomains.UTIL_LOGGER);
    }
    
    /**
     * Returns a formatted string, using the key to find the full message and 
     * substituting any parameters.
     * @param key the message key with which to locate the message of interest
     * @param o the object(s), if any, to be substituted into the message
     * @return a String containing the formatted message
     */
    private static String getMessage(String key, Object... o) {
        String msg = getLogger().getResourceBundle().getString(key);
        return MessageFormat.format(msg, o);
    }
}
