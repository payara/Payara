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

package org.glassfish.web.loader;

import org.glassfish.deployment.common.ClassDependencyBuilder;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.hk2.classmodel.reflect.*;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class - contains util methods used for implementation of
 * pluggable Shared Library features
 *
 *  @author Vijay Ramachandran
 */
public class ServletContainerInitializerUtil {

    private static final Logger log = WebappClassLoader.logger;

    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "Unexpected type of ClassLoader. Expected: java.net.URLClassLoader, got: {0}",
            level = "WARNING")
    public static final String WRONG_CLASSLOADER_TYPE = "AS-WEB-UTIL-00034";

    @LogMessageInfo(
            message = "Unable to load class {0}, reason: {1}",
            level = "FINE")
    public static final String CLASS_LOADING_ERROR = "AS-WEB-UTIL-00035";

    @LogMessageInfo(
            message = "Invalid URLClassLoader path component: [{0}] is neither a JAR file nor a directory",
            level = "WARNING")
    public static final String INVALID_URL_CLASS_LOADER_PATH = "AS-WEB-UTIL-00036";

    @LogMessageInfo(
            message = "Error trying to scan the classes at {0} for annotations in which a ServletContainerInitializer has expressed interest",
            level = "SEVERE",
            cause = "An IOException is encountered",
            action = "Verify if the path is correct")
    public static final String IO_ERROR = "AS-WEB-UTIL-00037";

    /**
     * Given a class loader, check for ServletContainerInitializer
     * implementations in any JAR file in the classpath
     *
     * @param cl The ClassLoader to be used to find JAR files
     *
     * @return Iterable over all ServletContainerInitializers that were found
     */
    public static Iterable<ServletContainerInitializer> getServletContainerInitializers(
            Map<String, String> webFragmentMap, List<Object> absoluteOrderingList,
            boolean hasOthers, ClassLoader cl) {
        /*
         * If there is an absoluteOrderingList specified, then make sure that
         * any ServletContainerInitializers included in fragment JARs 
         * NOT listed in the absoluteOrderingList will be ignored.
         * For this, we remove any unwanted fragment JARs from the class
         * loader's URL
         */
        if((absoluteOrderingList != null) && !hasOthers) {
            if(!(cl instanceof URLClassLoader)) {
                log.log(Level.WARNING,
                        WRONG_CLASSLOADER_TYPE,
                        cl.getClass().getCanonicalName());
                return null;
            }
            final URLClassLoader webAppCl = (URLClassLoader) cl;

            // Create a new List of URLs with missing fragments removed from
            // the currentUrls
            List<URL> newClassLoaderUrlList = new ArrayList<URL>();
            for (URL classLoaderUrl : webAppCl.getURLs()) {
                // Check that the URL is using file protocol, else ignore it
                if (!"file".equals(classLoaderUrl.getProtocol())) {
                    continue;
                }
                File file = new File(classLoaderUrl.getFile());
                try {
                    file = file.getCanonicalFile();
                } catch (IOException e) {
                    // Ignore
                }
                if (!file.exists()) {
                    continue;
                }
                String path = file.getAbsolutePath();
                if (!path.endsWith(".jar")) {
                    continue;
                }
                if (!isFragmentMissingFromAbsoluteOrdering(file.getName(),
                        webFragmentMap, absoluteOrderingList)) {
                    newClassLoaderUrlList.add(classLoaderUrl);
                }
            }

            // Create temporary classloader for ServiceLoader#load
            // TODO: Have temporary classloader honor delegate flag from
            // sun-web.xml
            URL[] urlsForNewClassLoader =
                new URL[newClassLoaderUrlList.size()];
            final URL[] urlArray = newClassLoaderUrlList.toArray(urlsForNewClassLoader);

            cl = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
                @Override
                public URLClassLoader run() {
                    return new URLClassLoader(urlArray, webAppCl.getParent());
                }
            });
        }

        return ServiceLoader.load(ServletContainerInitializer.class, cl);
    }


    /**
     * Builds a mapping of classes to the list of ServletContainerInitializers
     * interested in them
     *
     * @param initializers an Iterable over all ServletContainerInitializers
     * that need to be considered
     *
     * @return Mapping of classes to list of ServletContainerInitializers
     * interested in them
     */
    public static Map<Class<?>, List<Class<? extends ServletContainerInitializer>>> getInterestList(Iterable<ServletContainerInitializer> initializers) {

        if (null == initializers) {
            return null;
        }

        Map<Class<?>, List<Class<? extends ServletContainerInitializer>>> interestList = null;

        // Build a list of the classes / annotations in which the
        // initializers are interested
        for (ServletContainerInitializer sc : initializers) {
            if(interestList == null) {
                interestList = new HashMap<Class<?>, List<Class<? extends ServletContainerInitializer>>>();
            }
            Class<? extends ServletContainerInitializer> sciClass = sc.getClass();
            HandlesTypes ann = (HandlesTypes) sciClass.getAnnotation(HandlesTypes.class);
            if(ann == null) {
                // This initializer does not contain @HandlesTypes
                // This means it should always be called for all web apps
                // So map it with a special token
                List<Class<? extends ServletContainerInitializer>> currentInitializerList =
                        interestList.get(ServletContainerInitializerUtil.class);
                if(currentInitializerList == null) {
                    List<Class<? extends ServletContainerInitializer>> arr =
                            new ArrayList<Class<? extends ServletContainerInitializer>>();
                    arr.add(sciClass);
                    interestList.put(ServletContainerInitializerUtil.class, arr);
                } else {
                    currentInitializerList.add(sciClass);
                }
            } else {
                Class[] interestedClasses = ann.value();
                if( (interestedClasses != null) && (interestedClasses.length != 0) ) {
                    for(Class c : interestedClasses) {
                        List<Class<? extends ServletContainerInitializer>> currentInitializerList =
                                interestList.get(c);
                        if(currentInitializerList == null) {
                            List<Class<? extends ServletContainerInitializer>> arr =
                                    new ArrayList<Class<? extends ServletContainerInitializer>>();
                            arr.add(sciClass);
                            interestList.put(c, arr);
                        } else {
                            currentInitializerList.add(sciClass);
                        }
                    }
                }
            }
        }

        return interestList;
    }

    /**
     * Given an interestlist that was built above, and a class loader, scan the entire web app's classes and libraries
     * looking for classes that extend/implement/use the annotations of a class present in the interest list
     *
     * @param initializers Iterable over all ServletContainerInitializers that
     * were discovered
     * @param interestList The interestList built by the previous util method
     * @param cl The classloader to be used to load classes in WAR
     * @return Map&lt;Class&lt;? extends ServletContainerInitializer&gt;, Set&lt;Class&lt;?&gt;&gt;&gt;
     *                          A Map of ServletContainerInitializer classes to be called and arguments to be passed
     *                          to them
     */
    public  static Map<Class<? extends ServletContainerInitializer>, Set<Class<?>>> getInitializerList(
            Iterable<ServletContainerInitializer> initializers,
            Map<Class<?>, List<Class<? extends ServletContainerInitializer>>> interestList,
            Types types,
            ClassLoader cl) {

        if (interestList == null) {
            return null;
        }

        // This contains the final list of initializers and the set of
        // classes to be passed to them as arg
        Map<Class<? extends ServletContainerInitializer>, Set<Class<?>>> initializerList = null;

        // If an initializer was present without any @HandleTypes, it 
        // must be called with a null set of classes
        if(interestList.containsKey(ServletContainerInitializerUtil.class)) {
            initializerList = new HashMap<Class<? extends ServletContainerInitializer>, Set<Class<?>>>();
            List<Class<? extends ServletContainerInitializer>> initializersWithoutHandleTypes =
                    interestList.get(ServletContainerInitializerUtil.class);
            for(Class<? extends ServletContainerInitializer> c : initializersWithoutHandleTypes) {
                initializerList.put(c, null);
            }
        }

        /*
         * Now scan every class in this app's WEB-INF/classes and WEB-INF/lib
         * to see if any class uses the annotation or extends/implements a
         * class in our interest list.
         * Do this scanning only if we have ServletContainerinitializers that
         * have expressed specific interest
         */
        if( (interestList.keySet().size() > 1) ||
            ((interestList.keySet().size() == 1) &&
                    (!interestList.containsKey(ServletContainerInitializerUtil.class)))) {
            /*
             * Create an instance of ClassDependencyBuilder that looks at the byte code and keeps
             * the information for every class in this app
             *
             */
            if (types==null || Boolean.getBoolean("org.glassfish.web.parsing")) {
                ClassDependencyBuilder classInfo = new ClassDependencyBuilder();
                if (cl instanceof URLClassLoader) {
                    URLClassLoader ucl = (URLClassLoader) cl;
                    for(URL u : ucl.getURLs()) {
                        String path = u.getPath();
                        try {
                            if(path.endsWith(".jar")) {
                                JarFile jf = new JarFile(path);
                                try {
                                    Enumeration<JarEntry> entries = jf.entries();
                                    while(entries.hasMoreElements()) {
                                        JarEntry anEntry = entries.nextElement();
                                        if(anEntry.isDirectory())
                                            continue;
                                        if(!anEntry.getName().endsWith(".class"))
                                            continue;
                                        InputStream jarInputStream = null;
                                        try {
                                            jarInputStream = jf.getInputStream(anEntry);
                                            int size = (int) anEntry.getSize();
                                            byte[] classData = new byte[size];
                                            for(int bytesRead = 0; bytesRead < size;) {
                                                int r2 = jarInputStream.read(classData, bytesRead, size - bytesRead);
                                                bytesRead += r2;
                                            }
                                            classInfo.loadClassData(classData);
                                        } catch (Throwable t) {
                                            if (log.isLoggable(Level.FINE)) {
                                                log.log(Level.FINE,
                                                    CLASS_LOADING_ERROR,
                                                    new Object[] {
                                                        anEntry.getName(),
                                                        t.toString()});
                                            }
                                            continue;
                                        } finally {
                                            if(jarInputStream != null) {
                                                jarInputStream.close();
                                            }
                                        }
                                    }
                                } finally {
                                    jf.close();
                                }
                            } else {
                                File file = new File(path);
                                if (file.exists()) {
                                    if (file.isDirectory()) {
                                        scanDirectory(file, classInfo);
                                    } else {
                                        log.log(Level.WARNING,
                                            INVALID_URL_CLASS_LOADER_PATH,
                                            path);
                                    }
                                }
                            }
                        } catch(IOException ioex) {
                            String msg = rb.getString(IO_ERROR);
                            msg = MessageFormat.format(msg,
                                new Object[] { path });
                            log.log(Level.SEVERE, msg, ioex);
                            return null;
                        }
                    }
                }

                initializerList = checkAgainstInterestList(classInfo, interestList, initializerList, cl);
            } else {
                initializerList = checkAgainstInterestList(types, interestList, initializerList, cl);
            }
        }

        /*
         * If a ServletContainerInitializer was annotated with HandlesTypes,
         * but none of the application classes match, we must still invoke
         * it at its onStartup method, passing in a null Set of classes
         */ 
        for (ServletContainerInitializer initializer : initializers) {
            if (!initializerList.containsKey(initializer.getClass())) {
                initializerList.put(initializer.getClass(), null);
            }
        }

        return initializerList;
    }

    /**
     * Checks if a given JAR file is to be excluded while searching for ServletContainerInitializer implementations
     * @param jarName the JAR file
     * @param webFragmentMap fragment information from deployment desc
     * @param absoluteOrderingList give ordering list
     * @return true if the given JAR file is NOT present in the absolute ordering list
     */
    private static boolean isFragmentMissingFromAbsoluteOrdering(
           String jarName, Map<String, String> webFragmentMap,
           List<Object> absoluteOrderingList) {
        return (webFragmentMap != null &&
            absoluteOrderingList != null &&
            !absoluteOrderingList.contains(
                webFragmentMap.get(jarName)));
   }

    /**
     * Given a directory, scan all sub directories looking for classes and
     * build the interest list
     *
     * @param dir the directory to be scanned
     * @param classInfo the ClassDependencyBuilder that holds info on all classes
     */
    private static void scanDirectory(File dir, ClassDependencyBuilder classInfo) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getPath();
                if (fileName.endsWith(".class")) {
                    try {
                        InputStream is = null;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            is = new BufferedInputStream(new FileInputStream(fileName));
                            byte[] bs = new byte[2048];
                            int size = -1;
                            while ((size = is.read(bs)) >= 0) {
                                baos.write(bs, 0, size);
                            } 
                            classInfo.loadClassData(baos.toByteArray());
                        } finally {
                            if (is != null) {
                                is.close();
                            }
                            baos.close();
                        }
                    } catch (Throwable t) {
                        if (log.isLoggable(Level.WARNING)) {
                            log.log(Level.WARNING,
                                CLASS_LOADING_ERROR,
                                new Object[] {fileName, t.toString()});
                        }
                        continue;
                    }
                }
            } else {
                scanDirectory(file, classInfo);
            }
        }
        return;
    }

    /**
     * Given the interestList, checks in the Types metadata if a given class
     * uses any of the annotations, subclasses any of the type; If so, builds
     * the initializer list
     *
     */
    private static Map<Class<? extends ServletContainerInitializer>, Set<Class<?>>> checkAgainstInterestList(
                                Types classInfo,
                                Map<Class<?>, List<Class<? extends ServletContainerInitializer>>> interestList,
                                Map<Class<? extends ServletContainerInitializer>, Set<Class<?>>> initializerList,
                                ClassLoader cl) {

        if (classInfo==null) {
            return initializerList;
        }
        for (Map.Entry<Class<?>, List<Class<? extends ServletContainerInitializer>>> e:
                interestList.entrySet()) {

            Class<?> c = e.getKey();
            Type type = classInfo.getBy(c.getName());
            if (type==null)
                continue;

            Set<Class<?>> resultSet = new HashSet<Class<?>>();
            if (type instanceof AnnotationType) {
                for (AnnotatedElement ae : ((AnnotationType) type).allAnnotatedTypes()) {
                    if (ae instanceof Member) {
                        ae = ((Member) ae).getDeclaringType();
                    } else if (ae instanceof Parameter) {
                        ae = ((Parameter) ae).getMethod().getDeclaringType();
                    }
                    if (ae instanceof Type) {
                        try {
                            resultSet.add(cl.loadClass(ae.getName()));
                        } catch (Throwable t) {
                            if (log.isLoggable(Level.WARNING)) {
                                log.log(Level.WARNING,
                                    CLASS_LOADING_ERROR,
                                    new Object[] {ae.getName(), t.toString()});
                            }
                        }     
                    }
                }
            } else {
                Collection<ClassModel> classes;
                if (type instanceof InterfaceModel) {
                    classes = ((InterfaceModel) type).allImplementations();
                } else {
                    classes = ((ClassModel) type).allSubTypes();
                }
                for (ClassModel classModel : classes) {
                    try {
                        resultSet.add(cl.loadClass(classModel.getName()));
                    } catch (Throwable t) {
                        if (log.isLoggable(Level.WARNING)) {
                            log.log(Level.WARNING,
                                CLASS_LOADING_ERROR,
                                new Object[] {classModel.getName(), t.toString()});
                        }
                    }
                }
            }
            if(initializerList == null) {
                initializerList = new HashMap<Class<? extends ServletContainerInitializer>, Set<Class<?>>>();
            }
            List<Class<? extends ServletContainerInitializer>> containerInitializers = e.getValue();
            for(Class<? extends ServletContainerInitializer> initializer : containerInitializers) {
                Set<Class<?>> classSet = initializerList.get(initializer);
                if(classSet == null) {
                    classSet = new HashSet<Class<?>>();
                }
                classSet.addAll(resultSet);
                initializerList.put(initializer, classSet);
            }
            
        }

        return initializerList;
    }
    /**
     * Given the interestList, checks if a given class uses any of the
     * annotations; If so, builds the initializer list
     *
     * @param classInfo the ClassDependencyBuilder instance that holds info on all classes
     * @param interestList the interestList built earlier
     * @param initializerList the initializerList built so far
     * @param cl the ClassLoader to be used to load the class
     * @return the updated initializer list
     */
    private static Map<Class<? extends ServletContainerInitializer>, Set<Class<?>>> checkAgainstInterestList(
                                ClassDependencyBuilder classInfo,
                                Map<Class<?>, List<Class<? extends ServletContainerInitializer>>> interestList,
                                Map<Class<? extends ServletContainerInitializer>, Set<Class<?>>> initializerList,
                                ClassLoader cl) {
        for(Map.Entry<Class<?>, List<Class<? extends ServletContainerInitializer>>> e :
                interestList.entrySet()) {

            Class<?> c = e.getKey();
            Set<String> resultFromClassInfo = classInfo.computeResult(c.getName());
            if(resultFromClassInfo.isEmpty()) {
                continue;
            }
            Set<Class<?>> resultSet = new HashSet<Class<?>>();
            for(Iterator<String> iter = resultFromClassInfo.iterator(); iter.hasNext();) {
                String className = iter.next().replace('/', '.');
                try {
                    Class aClass = cl.loadClass(className);
                    resultSet.add(aClass);
                } catch (Throwable t) {
                    if (log.isLoggable(Level.WARNING)) {
                        log.log(Level.WARNING,
                            CLASS_LOADING_ERROR,
                            new Object[] {className, t.toString()});
                    }
                }
            }
            if(initializerList == null) {
                initializerList = new HashMap<Class<? extends ServletContainerInitializer>, Set<Class<?>>>();
            }
            List<Class<? extends ServletContainerInitializer>> containerInitializers = e.getValue();
            for(Class<? extends ServletContainerInitializer> initializer : containerInitializers) {
                Set<Class<?>> classSet = initializerList.get(initializer);
                if(classSet == null) {
                    classSet = new HashSet<Class<?>>();
                }
                classSet.addAll(resultSet);
                initializerList.put(initializer, classSet);
            }
        }
        return initializerList;
    }
}
