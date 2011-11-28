/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.ModuleLifecycleListener;
import com.sun.enterprise.module.common_impl.CompositeEnumeration;
import com.sun.logging.LogDomains;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import java.net.URL;

/**
 * This class is responsible for creating a ClassLoader that can
 * load classes exported by any OSGi bundle in the system for public use.
 * Such classes include Java EE API, AMX API, appserv-ext API, etc.
 * CommonClassLoader delegates to this class loader..
 * It does special treatment of META-INF/mailcap file. For such resources,
 * it searches all available bundles.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
@Service
public class APIClassLoaderServiceImpl implements PostConstruct {

    /*
     * Implementation Note:
     * 1. This class depends on OSGi runtime, so not portable on HK2.
     * 2. APIClassLoader maintains a blacklist, i.e., classes and resources that could not be loaded to avoid
     * unnecessary delegation. It flushes that list everytime a new bundle is installed in the system.
     * This takes care of performance problem in typical production use of GlassFish.
     * TODO: We need to add an upper threshold for blacklist to avoid excessive use of memory.
     */

    private ClassLoader theAPIClassLoader;
    @Inject
    ModulesRegistry mr;
    private static final String APIExporterModuleName =
            "GlassFish-Application-Common-Module"; // NOI18N
    private static final String MAILCAP = "META-INF/mailcap";
    private static final String META_INF_SERVICES = "META-INF/services/";
    private static final Enumeration<URL> EMPTY_URLS = new Enumeration<URL>() {

        public boolean hasMoreElements() {
            return false;
        }

        public URL nextElement() {
            throw new NoSuchElementException();
        }
    };
    final static Logger logger = LogDomains.getLogger(APIClassLoaderServiceImpl.class, LogDomains.LOADER_LOGGER);
    private Module APIModule;

    public void postConstruct() {
        try {
            createAPIClassLoader();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createAPIClassLoader() throws IOException {
        APIModule = mr.getModules(APIExporterModuleName).iterator().next();
        assert (APIModule != null);
        final ClassLoader apiModuleLoader = APIModule.getClassLoader();
        /*
         * We don't directly retrun APIModule's class loader, because
         * that class loader does not delegate to the parent. Instead, it
         * relies on OSGi bundle to load the classes. That behavior is
         * fine if we want to honor OSGi classloading semantics. APIClassLoader
         * wants to use delegation model so that we don't have to set
         * bootdelegation=* for OSGi bundles.
         * Since the parent of bundle classloader will have glassfish launching classes, felix or any other
         * OSGi framework classes and their dependencies, we don't want to delegate to such a class loader.
         * Instead, we delegate to JRE's extension class loader if we don't find any class via APIModuleLoader.
         * With this, user can actually embed a different version of Felix as part of their app.
         */
        theAPIClassLoader = new APIClassLoader(apiModuleLoader, getExtensionClassLoader());
        logger.logp(Level.FINE, "APIClassLoaderService", "createAPIClassLoader",
                "APIClassLoader = {0}", new Object[]{theAPIClassLoader});
    }

    private ClassLoader getExtensionClassLoader() {
        return ClassLoader.getSystemClassLoader().getParent();
    }

    public ClassLoader getAPIClassLoader() {
        return theAPIClassLoader;
    }

    private class APIClassLoader extends ClassLoader {

        // list of not found classes and resources.
        // the string represents resource name, so foo/Bar.class for foo.Bar
        private Set<String> blacklist;
        private final ClassLoader apiModuleLoader;

        /**
         * This method takes two classloaders which is unusual. Both the class loaders are consulted,
         * so they both are delegates, but depending on the class/resource being requested, one is preferred
         * over the other. The second argument is the classic parent class loader, where as the first one
         * is the OSGi gateway classloader. For all java.* names, we consult only the parent loader.
         * For any other names, we first consult the OSGi gateway loader and then parent. See more comments in
         * loadClass() method implementation of this class.
         * @param apiModuleLoader ClassLoader corresponding to the APIModule
         * @param parent ClassLoader that's consulted for all java.* classes and for classes
         * not found via apiModuleLoader
         */
        public APIClassLoader(ClassLoader apiModuleLoader, ClassLoader parent) {
            super(parent);
            this.apiModuleLoader = apiModuleLoader;
            blacklist = new HashSet<String>();

            // add a listener to manage blacklist in APIClassLoader
            mr.register(new ModuleLifecycleListener() {
                public void moduleInstalled(Module module) {
                    clearBlackList();
                }

                public void moduleResolved(Module module) {
                }

                public void moduleStarted(Module module) {
                }

                public void moduleStopped(Module module) {
                }

                public void moduleUpdated(Module module) {
                    clearBlackList();
                }
            });

        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return loadClass(name, false);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // First check if we know this can't be loaded
            final String resourceName = convertToResourceName(name);
            if (isBlackListed(resourceName)) {
                throw new ClassNotFoundException(name);
            }

            // Then check if the class has already been loaded
            Class c = findLoadedClass(name);
            if (c == null) {
                if (!name.startsWith("java.")) { // java classes always come from parent
                    try {
                        c = apiModuleLoader.loadClass(name); // we ignore the resolution flag
                    } catch (ClassNotFoundException cnfe) {
                        // punch in. find the provider class, no matter where we are.
                        Module m = mr.getProvidingModule(name);
                        if (m != null) {
                            return m.getClassLoader().loadClass(name); // abort search if we fail to load.
                        }
                    }
                }
                if (c == null) {
                    // Call super class implementation which takes care of
                    // delegating to parent.
                    try {
                        c = super.loadClass(name, resolve);
                    } catch (ClassNotFoundException e) {
                        addToBlackList(resourceName);
                        throw e;
                    }
                }
            }
            return c;
        }

        @Override
        public URL getResource(String name) {
            if (isBlackListed(name)) return null;
            URL url = null;
            if (!name.startsWith("java/")) {
                if (name.equals(MAILCAP)) {
                    // punch in for META-INF/mailcap files.
                    // see issue #8426
                    for (Module m : mr.getModules()) {
                        if ((url = m.getClassLoader().getResource(name)) != null) {
                            break;
                        }
                    }
                } else if(name.startsWith(META_INF_SERVICES)) {
                    // punch in to find the service loader from any module
                    // If this is a META-INF/services lookup, search in every
                    // modules that we know of.
                    String serviceName = name.substring(
                            META_INF_SERVICES.length());

                    for( Module m : mr.getModules() ) {
                        List<URL> list = m.getMetadata().getDescriptors(
                                serviceName);
                        if(!list.isEmpty())     return list.get(0);
                    }
                } else {
                    url = apiModuleLoader.getResource(name);
                }
            }
            if (url == null) {
                // Either requested resource belongs to java/ namespace or
                // it was not found in any of the bundles, so call
                // super class implementation which will delegate to parent.
                url = super.getResource(name);
            }
            if (url == null) {
                addToBlackList(name);
            }
            return url;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            List<Enumeration<URL>> enumerators = new ArrayList<Enumeration<URL>>();
            enumerators.add(findResources(name));
            if (getParent() != null) {
                enumerators.add(getParent().getResources(name));
            }
            return new CompositeEnumeration(enumerators);
        }

        // This method is needed to be compatible with ClassLoader implementation in IBM JRE.
        // In IBM JRE, ClassLoader.getResources() does not call parent.getResources(); instead it
        // recurssively calls findResources() for all parents in the delegation hierarchy.
        // See issue #16364 for details.
        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            if (!name.startsWith("java/")) {
                if (name.equals(MAILCAP)) {
                    // punch in for META-INF/mailcap files. see issue #8426
                    List<Enumeration<URL>> enumerators = new ArrayList<Enumeration<URL>>();
                    for (Module m : mr.getModules()) {
                        enumerators.add(m.getClassLoader().getResources(name));
                    }
                    return new CompositeEnumeration(enumerators);
                } else if (name.startsWith(META_INF_SERVICES)) {
                    // punch in. find the service loader from any module
                    String serviceName = name.substring(META_INF_SERVICES.length());
                    List<URL> punchedInURLs = new ArrayList<URL>();
                    for (Module m : mr.getModules()) {
                        punchedInURLs.addAll(m.getMetadata().getDescriptors(serviceName));
                    }
                    return Collections.enumeration(punchedInURLs);
                } else {
                    // search in the parent classloader
                    return apiModuleLoader.getResources(name);
                }
            }
            return EMPTY_URLS;
        }

        @Override
        public String toString() {
            return "APIClassLoader";
        }

        /**
         * Takes a class name as used in Class.forName and converts it to a resource name as used in
         * ClassLoader.getResource
         *
         * @param className className to be converted
         * @return equivalent resource name
         */
        private String convertToResourceName(String className) {
            return className.replace('.', '/').concat(".class");
        }

        private synchronized boolean isBlackListed(String name) {
            return blacklist.contains(name);
        }

        private synchronized void addToBlackList(String name) {
            blacklist.add(name);
        }

        private synchronized void clearBlackList() {
            blacklist.clear();
        }

    }
}
