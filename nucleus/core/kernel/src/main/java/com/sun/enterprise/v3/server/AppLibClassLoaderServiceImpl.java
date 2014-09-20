/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.DelegatingClassLoader;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.io.IOException;

/**
 * This class is responsible for constructing class loader that has visibility
 * to deploy time libraries (--libraries and EXTENSION_LIST of MANIFEST.MF,
 * provided the library is available in 'applibs' directory) for an application.
 * It is different from CommonClassLoader in a sense that the libraries that are part of
 * common class loader are shared by all applications, where as this class
 * loader adds a scope to a library.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
@Service
@Singleton
public class AppLibClassLoaderServiceImpl {
    /*
     * TODO(Sahoo): Not Yet Properly Implemented, as we have to bring in
     * all the changes from
     * http://fisheye5.cenqua.com/browse/glassfish/appserv-core/src/java/com/sun/enterprise/loader/EJBClassPathUtils.java
     * To be specific, we have to bring in createApplicationLibrariesClassLoader().
     */

    @Inject
    ServiceLocator habitat;

    @Inject
    CommonClassLoaderServiceImpl commonCLS;

    private Map<URI, DelegatingClassLoader.ClassFinder> classFinderRegistry =
            new HashMap<URI, DelegatingClassLoader.ClassFinder>();

    /**
     * @see org.glassfish.internal.api.ClassLoaderHierarchy#getAppLibClassLoader(String, List<URI>) 
     */
    public ClassLoader getAppLibClassLoader(String application, List<URI> libURIs)
            throws MalformedURLException {

        ClassLoaderHierarchy clh = habitat.getService(ClassLoaderHierarchy.class);
        DelegatingClassLoader connectorCL = clh.getConnectorClassLoader(application);

        if (libURIs == null || libURIs.isEmpty()) {
            // Optimization: when there are no libraries, why create an empty
            // class loader in the hierarchy? Instead return the parent.
            return connectorCL;
        }

        final ClassLoader commonCL = commonCLS.getCommonClassLoader();
        DelegatingClassLoader applibCL = AccessController.doPrivileged(new PrivilegedAction<DelegatingClassLoader>() {
                       public DelegatingClassLoader run() {
                           return new DelegatingClassLoader(commonCL);
                       }
                   });

        // order of classfinders is important here :
        // connector's classfinders should be added before libraries' classfinders
        // as the delegation hierarchy is appCL->app-libsCL->connectorCL->commonCL->API-CL
        // since we are merging connector and applib classfinders to be at same level,
        // connector classfinders need to be be before applib classfinders in the horizontal
        // search path
        for (DelegatingClassLoader.ClassFinder cf : connectorCL.getDelegates()) {
            applibCL.addDelegate(cf);
        }
        addDelegates(libURIs, applibCL);

        return applibCL;
    }

    private void addDelegates(Collection<URI> libURIs, DelegatingClassLoader holder)
            throws MalformedURLException {

        ClassLoader commonCL = commonCLS.getCommonClassLoader();
        for (URI libURI : libURIs) {
            synchronized (this) {
                DelegatingClassLoader.ClassFinder libCF = classFinderRegistry.get(libURI);
                if (libCF == null) {
                    libCF = new URLClassFinder(new URL[]{libURI.toURL()}, commonCL);
                    classFinderRegistry.put(libURI, libCF);
                }
                holder.addDelegate(libCF);
            }
        }
    }

    /**
     * @see org.glassfish.internal.api.ClassLoaderHierarchy#getAppLibClassFinder(List<URI>) 
     */
    public DelegatingClassLoader.ClassFinder getAppLibClassFinder(Collection<URI> libURIs)
            throws MalformedURLException {
        final ClassLoader commonCL = commonCLS.getCommonClassLoader();
        DelegatingClassLoader appLibClassFinder = AccessController.doPrivileged(new PrivilegedAction<DelegatingClassLoader>() {
            public DelegatingClassLoader run() {
                return new AppLibClassFinder(commonCL);
            }
        });
        addDelegates(libURIs, appLibClassFinder);
        return (DelegatingClassLoader.ClassFinder)appLibClassFinder;
    }

    private static class URLClassFinder extends URLClassLoader
            implements DelegatingClassLoader.ClassFinder {

        public URLClassFinder(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public Class<?> findClass(String name) throws ClassNotFoundException {
            Class<?> c = this.findLoadedClass(name);
            if (c!=null) {
                return c;
            }
            return super.findClass(name);
        }

        public Class<?> findExistingClass(String name) {
            return super.findLoadedClass(name);
        }
    }

    private static class AppLibClassFinder extends DelegatingClassLoader implements DelegatingClassLoader.ClassFinder {

        public AppLibClassFinder(ClassLoader parent, List<DelegatingClassLoader.ClassFinder> delegates)
                throws IllegalArgumentException {
            super(parent, delegates);
        }

        public AppLibClassFinder(ClassLoader parent) {
            super(parent);
        }

        public Class<?> findExistingClass(String name) {
            // no action needed as parent is delegating classloader which will never be a defining classloader
            return null;
        }

        public URL findResource(String name) {
            return super.findResource(name);
        }

        public Enumeration<URL> findResources(String name) throws IOException {
            return super.findResources(name);
        }
    }
}
