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

package org.apache.catalina.startup;

import org.apache.catalina.core.StandardServer;
import org.apache.catalina.loader.StandardClassLoader;

import java.io.File;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>Utility class for building class loaders for Catalina.  The factory
 * method requires the following parameters in order to build a new class
 * loader (with suitable defaults in all cases):</p>
 * <ul>
 * <li>A set of directories containing unpacked classes (and resources)
 *     that should be included in the class loader's
 *     repositories.</li>
 * <li>A set of directories containing classes and resources in JAR files.
 *     Each readable JAR file discovered in these directories will be
 *     added to the class loader's repositories.</li>
 * <li><code>ClassLoader</code> instance that should become the parent of
 *     the new class loader.</li>
 * </ul>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.3 $ $Date: 2006/03/12 01:27:06 $
 */

public final class ClassLoaderFactory {


    // ------------------------------------------------------- Static Variables

    private static final Logger log = StandardServer.log;
    /**
     * Debugging detail level for processing the startup.
     */
    private static int debug = 0;


    // ------------------------------------------------------ Static Properties


    /**
     * Return the debugging detail level.
     */
    public static int getDebug() {

        return (debug);

    }


    /**
     * Set the debugging detail level.
     *
     * @param newDebug The new debugging detail level
     */
    public static void setDebug(int newDebug) {

        debug = newDebug;

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Create and return a new class loader, based on the configuration
     * defaults and the specified directory paths:
     *
     * @param unpacked Array of pathnames to unpacked directories that should
     *  be added to the repositories of the class loader, or <code>null</code> 
     * for no unpacked directories to be considered
     * @param packed Array of pathnames to directories containing JAR files
     *  that should be added to the repositories of the class loader, 
     * or <code>null</code> for no directories of JAR files to be considered
     * @param parent Parent class loader for the new class loader, or
     *  <code>null</code> for the system class loader.
     *
     * @exception Exception if an error occurs constructing the class loader
     */
    public static ClassLoader createClassLoader(File unpacked[],
                                                File packed[],
                                                ClassLoader parent)
        throws Exception {
        return createClassLoader(unpacked, packed, null, parent);
    }


    /**
     * Create and return a new class loader, based on the configuration
     * defaults and the specified directory paths:
     *
     * @param unpacked Array of pathnames to unpacked directories that should
     *  be added to the repositories of the class loader, or <code>null</code> 
     * for no unpacked directories to be considered
     * @param packed Array of pathnames to directories containing JAR files
     *  that should be added to the repositories of the class loader, 
     * or <code>null</code> for no directories of JAR files to be considered
     * @param urls Array of URLs to remote repositories, designing either JAR 
     *  resources or uncompressed directories that should be added to 
     *  the repositories of the class loader, or <code>null</code> for no 
     *  directories of JAR files to be considered
     * @param parent Parent class loader for the new class loader, or
     *  <code>null</code> for the system class loader.
     *
     * @exception Exception if an error occurs constructing the class loader
     */
    public static ClassLoader createClassLoader(File unpacked[],
                                                File packed[],
                                                URL urls[],
                                                ClassLoader parent)
        throws Exception {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Creating new class loader");

        // Construct the "class path" for this class loader
        Set<URL> set = new LinkedHashSet<URL>();

        // Add unpacked directories
        if (unpacked != null) {
            for (int i = 0; i < unpacked.length; i++)  {
                File file = unpacked[i];
                if (!file.exists() || !file.canRead())
                    continue;
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Including directory or JAR "
                            + file.getAbsolutePath());
                URL url = new URL("file", null,
                                  file.getCanonicalPath() + File.separator);
                set.add(url);
            }
        }

        // Add packed directory JAR files
        if (packed != null) {
            for (int i = 0; i < packed.length; i++) {
                File directory = packed[i];
                if (!directory.isDirectory() || !directory.exists() ||
                    !directory.canRead())
                    continue;
                String filenames[] = directory.list();
                for (int j = 0; j < filenames.length; j++) {
                    String filename = filenames[j].toLowerCase(Locale.ENGLISH);
                    if (!filename.endsWith(".jar"))
                        continue;
                    File file = new File(directory, filenames[j]);
                    if (log.isLoggable(Level.FINE))
                        log.log(Level.FINE, "Including jar file " +
                                file.getAbsolutePath());
                    URL url = new URL("file", null,
                                      file.getCanonicalPath());
                    set.add(url);
                }
            }
        }

        // Add remote URLs
        if (urls != null) {
            for (int i = 0; i < urls.length; i++) {
                set.add(urls[i]);
            }
        }

        // Construct the class loader itself
        final URL array[] = set.toArray(new URL[set.size()]);
        final ClassLoader parentCL = parent;
        StandardClassLoader classLoader = null;
        if (parentCL == null)  {
            classLoader = AccessController.doPrivileged(new PrivilegedAction<StandardClassLoader>() {
                @Override
                public StandardClassLoader run() {
                    return new StandardClassLoader(array);
                }
            });
        } else {
            classLoader = AccessController.doPrivileged(new PrivilegedAction<StandardClassLoader>() {
                @Override
                public StandardClassLoader run() {
                    return new StandardClassLoader(array, parentCL);
                }
            });
        }
        classLoader.setDelegate(true);
        return (classLoader);

    }


    // -------------------------------------------------------- Private Methods



}
