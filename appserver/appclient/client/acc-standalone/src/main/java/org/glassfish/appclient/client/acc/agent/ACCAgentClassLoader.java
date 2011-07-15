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

package org.glassfish.appclient.client.acc.agent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

/**
 * Used as the system class loader during app client launch.
 * <p>
 * The main role of this class loader is to find a splash screen image
 * that might be specified in the manifest of the app client.  Once the
 * ACC begins working it will create an ACCClassLoader for loading
 * client classes and locating client resources.
 *
 * @author tjquinn
 */
public class ACCAgentClassLoader extends URLClassLoader {

    private boolean isActive = true;

    public ACCAgentClassLoader(ClassLoader parent) {
        /*
         * This constructor is used by the VM to create a
         * system class loader (as specified by -Djava.system.class.loader
         * on the java command created from the appclient script).
         * <p>
         * Actually create two new loaders.  One will handle the GlassFish system JARs
         * and the second will temporarily handle the application resources -
         * typically for a splash screen.
         */
        super(userClassPath(),
                prepareLoader(GFSystemClassPath(), parent.getParent()));
    }
    
    private static URLClassLoader prepareLoader(final URL[] urls, final ClassLoader parent) {
        return AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {

            @Override
            public URLClassLoader run() {
                return new URLClassLoader(urls, parent);
            }
            
        });
    }

    public ACCAgentClassLoader(URL[] urls) {
        super(urls);
    }

    public ACCAgentClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public ACCAgentClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }


    @Override
    public synchronized Class<?> loadClass(String name) throws ClassNotFoundException {
        if (isActive && isStillActive()) {
            return super.loadClass(name);
        } else {
            return getParent().loadClass(name);
        }
    }

    @Override
    public URL getResource(String name) {
        if (isActive && isStillActive()) {
            return super.getResource(name);
        } else {
            return getParent().getResource(name);
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (isActive && isStillActive()) {
            return super.getResources(name);
        } else {
            return getParent().getResources(name);
        }
    }

    private boolean isStillActive() {
        if (isActive) {
            final String propValue = System.getProperty("org.glassfish.appclient.acc.agentLoaderDone");
            isActive = (propValue != null);
        }
        return isActive;
    }

    private static URL[] userClassPath() {
        final URI GFSystemURI = GFSystemURI();
        final List<URL> result = classPathToURLs(System.getProperty("java.class.path"));
        for (ListIterator<URL> it = result.listIterator(); it.hasNext();) {
            final URL url = it.next();
            try {
                if (url.toURI().equals(GFSystemURI)) {
                    it.remove();
                }
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }

//        result.addAll(classPathToURLs(System.getenv("APPCPATH")));

        return result.toArray(new URL[result.size()]);
    }

    private static URL[] GFSystemClassPath() {
        try {
            return new URL[] {GFSystemURI().normalize().toURL()};
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static URI GFSystemURI() {
        try {
            Class agentClass = Class.forName("org.glassfish.appclient.client.acc.agent.AppClientContainerAgent");
            return agentClass.getProtectionDomain().getCodeSource().getLocation().toURI().normalize();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static List<URL> classPathToURLs(final String classPath) {
        if (classPath == null) {
            return Collections.EMPTY_LIST;
        }
        final List<URL> result = new ArrayList<URL>();
        try {
            for (String classPathElement : classPath.split(File.pathSeparator)) {
                result.add(new File(classPathElement).toURI().normalize().toURL());
            }
            return result;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
