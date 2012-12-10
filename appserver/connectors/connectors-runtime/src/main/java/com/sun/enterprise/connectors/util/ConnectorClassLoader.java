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

import com.sun.enterprise.loader.ASURLClassLoader;
import com.sun.logging.LogDomains;
import org.glassfish.internal.api.DelegatingClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class loader is responsible for loading standalone
 * RAR files. This class loader is the parent of JarClassLoader
 * (the application class loader)
 *
 * @author Tony Ng, Sivakumar Thyagarajan
 */
public class ConnectorClassLoader extends ASURLClassLoader
{

    private static final Logger _logger = LogDomains.getLogger(ConnectorClassLoader.class, LogDomains.RSR_LOGGER);

    private volatile static ConnectorClassLoader classLoader = null;

    /**
     * A linked list of URL classloaders representing each deployed connector
     * module
     */
    private final List classLoaderChain = new LinkedList();

    /**
     * The parent class loader for the connector Class Loader [ie the common
     * Classloader]
     */
    private ClassLoader parent = null;

    /**
     * Maintains a mapping between rar name and a classloader that has services
     * that RAR module.
     */
    private final Map rarModuleClassLoaders = new HashMap();

    public static synchronized ConnectorClassLoader getInstance() {
        if (classLoader == null) {
            classLoader =
                    AccessController.doPrivileged(new PrivilegedAction<ConnectorClassLoader>() {
                        public ConnectorClassLoader run() {
                            return new ConnectorClassLoader();
                        }
                    });
        }
        return classLoader;
    }

    private ConnectorClassLoader() {
        super();
    }

    private ConnectorClassLoader(ClassLoader parent) {
        super(parent);
        this.parent = parent;
    }

    /**
     * Initializes this singleton with the given parent class loader
     * if not already created.
     *
     * @param parent parent class loader
     * @return the instance
     */
    public static ConnectorClassLoader getInstance(final ClassLoader parent) {
        if (classLoader == null) {
            synchronized (ConnectorClassLoader.class) {
                if (classLoader == null) {
                    classLoader = AccessController.doPrivileged(new PrivilegedAction<ConnectorClassLoader>() {
                        public ConnectorClassLoader run() {
                            return new ConnectorClassLoader(parent);
                        }
                    });
                }
            }
        }
        return classLoader;
    }

    /**
     * Adds the requested resource adapter to the ConnectorClassLoader. A
     * ConnectorClassLoader is created with the moduleDir as its search path
     * and this classloader is added to the classloader chain.
     *
     * @param rarName   the resourceAdapter module name to add
     * @param moduleDir the directory location where the RAR contents are exploded
     */
    public void addResourceAdapter(String rarName, String moduleDir) {

        try {
            File file = new File(moduleDir);
            ASURLClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ASURLClassLoader>() {
                        public ASURLClassLoader run() {
                            return new ASURLClassLoader(parent);
                        }
                    });

            cl.appendURL(file.toURI().toURL());
            appendJars(file, cl);
            classLoaderChain.add(cl);
            rarModuleClassLoaders.put(rarName, cl);
        } catch (MalformedURLException ex) {
            _logger.log(Level.SEVERE, "enterprise_util.connector_malformed_url", ex);
        }
    }

    //TODO V3 handling "unexploded jars" for now, V2 deployment module used to explode the jars also
    private void appendJars(File moduleDir, ASURLClassLoader cl) throws MalformedURLException {
        if (moduleDir.isDirectory()) {
            for (File file : moduleDir.listFiles()) {
                if (file.getName().toUpperCase(Locale.getDefault()).endsWith(".JAR")) {
                    cl.appendURL(file.toURI().toURL());
                } else if (file.isDirectory()) {
                    appendJars(file, cl); //recursive add
                }
            }
        }
    }

    /**
     * Removes the resource adapter's class loader from the classloader linked
     * list
     *
     * @param moduleName the connector module that needs to be removed.
     */
    public void removeResourceAdapter(String moduleName) {
        ASURLClassLoader classLoaderToRemove =
                (ASURLClassLoader) rarModuleClassLoaders.get(moduleName);
        if (classLoaderToRemove != null) {
            classLoaderChain.remove(classLoaderToRemove);
            rarModuleClassLoaders.remove(moduleName);
            _logger.log(
                    Level.WARNING,
                    "enterprise_util.remove_connector", 
                    moduleName);
        }
    }


    /*
      * Loads the class with the specified name and resolves it if specified.
      *
      * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
      */
    public synchronized Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class clz = null;
        //Use the delegation model to service class requests that could be
        //satisfied by parent [common class loader].

        if (parent != null) {
            try {
                clz = parent.loadClass(name);
                if (clz != null) {
                    if (resolve) {
                        resolveClass(clz);
                    }
                    return clz;
                }
            } catch (ClassNotFoundException e) {
                //ignore and try the connector modules classloader
                //chain.
            }
        } else {
            return super.loadClass(name, resolve);
        }

        //Going through the connector module classloader chain to find
        // class and return the first match.
        for (Iterator iter = classLoaderChain.iterator(); iter.hasNext();) {
            ASURLClassLoader ccl = (ASURLClassLoader) iter.next();
            try {
                clz = ccl.loadClass(name);
                if (clz != null) {
                    if (resolve) {
                        resolveClass(clz);
                    }
                    return clz;
                }
            } catch (ClassNotFoundException cnfe) {
                //ignore this exception and continue with next classloader in
                // chain
                continue;
            }
        }

        //Can't find requested class in parent and in our classloader chain
        throw new ClassNotFoundException(name);
    }

    /**
     * Returns all the resources of the connector classloaders in the chain,
     * concatenated to a classpath string.
     * <p/>
     * Notice that this method is called by the setClassPath() method of
     * org.apache.catalina.loader.WebappLoader, since the ConnectorClassLoader does
     * not extend off of URLClassLoader.
     *
     * @return Classpath string containing all the resources of the connectors
     *         in the chain. An empty string if there exists no connectors in the chain.
     */

    public String getClasspath() {
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < classLoaderChain.size(); i++) {
            ASURLClassLoader ecl = (ASURLClassLoader) classLoaderChain.get(i);
            String eclClasspath = ecl.getClasspath();
            if (eclClasspath != null) {
                if (i > 0) strBuf.append(File.pathSeparator);
                strBuf.append(eclClasspath);
            }
        }
        return strBuf.toString();
    }
}
