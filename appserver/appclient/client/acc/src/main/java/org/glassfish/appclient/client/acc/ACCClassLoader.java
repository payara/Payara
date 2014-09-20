/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client.acc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.Certificate;

import org.glassfish.appclient.common.ClientClassLoaderDelegate;

/**
 *
 * @author tjquinn
 */
public class ACCClassLoader extends URLClassLoader {

    private static final String AGENT_LOADER_CLASS_NAME =
            "org.glassfish.appclient.client.acc.agent.ACCAgentClassLoader";
    private static ACCClassLoader instance = null;

    private ACCClassLoader shadow = null;

    private boolean shouldTransform = false;
    
    private final List<ClassFileTransformer> transformers =
            Collections.synchronizedList(
                new ArrayList<ClassFileTransformer>());

    
    private ClientClassLoaderDelegate clientCLDelegate;
    
    public static synchronized ACCClassLoader newInstance(ClassLoader parent,
            final boolean shouldTransform) {
        if (instance != null) {
            throw new IllegalStateException("already set");
        }
        final ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        final boolean currentCLWasAgentCL = currentCL.getClass().getName().equals(
                    AGENT_LOADER_CLASS_NAME);
        final ClassLoader parentForACCCL = currentCLWasAgentCL ? currentCL.getParent() : currentCL;
        
        instance = AccessController.doPrivileged(new PrivilegedAction<ACCClassLoader>() {

            @Override
            public ACCClassLoader run() {
                return new ACCClassLoader(userClassPath(), parentForACCCL, shouldTransform);
            }
            
        });
        
        if (currentCLWasAgentCL) {
            try {
                adjustACCAgentClassLoaderParent(instance);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return instance;
    }

    public static ACCClassLoader instance() {
        return instance;
    }

    private static void adjustACCAgentClassLoaderParent(final ACCClassLoader instance)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if (systemClassLoader.getClass().getName().equals(AGENT_LOADER_CLASS_NAME)) {
            final Field jwsLoaderParentField = ClassLoader.class.getDeclaredField("parent");
            jwsLoaderParentField.setAccessible(true);
            jwsLoaderParentField.set(systemClassLoader, instance);
            System.setProperty("org.glassfish.appclient.acc.agentLoaderDone", "true");
        
        }
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

        result.addAll(classPathToURLs(System.getenv("APPCPATH")));

        return result.toArray(new URL[result.size()]);
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

    public ACCClassLoader(ClassLoader parent, final boolean shouldTransform) {
        super(new URL[0], parent);
        this.shouldTransform = shouldTransform;
        
        clientCLDelegate = new ClientClassLoaderDelegate(this);
    }
//
//    public ACCClassLoader(URL[] urls) {
//        super(urls);
//    }

    public ACCClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        
        clientCLDelegate = new ClientClassLoaderDelegate(this);
    }

//    public ACCClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
//        super(urls, parent, factory);
//    }

    private ACCClassLoader(URL[] urls, ClassLoader parent, boolean shouldTransform) {
        this(urls, parent);
        this.shouldTransform = shouldTransform;
    }
    
    public synchronized void appendURL(final URL url) {
        addURL(url);
        if (shadow != null) {
            shadow.addURL(url);
        }
    }

    public void addTransformer(final ClassFileTransformer xf) {
        transformers.add(xf);
    }

    public void setShouldTransform(final boolean shouldTransform) {
        this.shouldTransform = shouldTransform;
    }

    synchronized ACCClassLoader shadow() {
        if (shadow == null) {
            shadow = AccessController.doPrivileged(new PrivilegedAction<ACCClassLoader>() {

                @Override
                public ACCClassLoader run() {
                    return new ACCClassLoader(getURLs(), getParent());
                }
                
            });
        }
        return shadow;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if ( ! shouldTransform) {
            return super.findClass(name);
        }
        final ACCClassLoader s = shadow();
        final Class<?> c = s.findClassUnshadowed(name);
        return copyClass(c);
    }

    private Class<?> copyClass(final Class c) throws ClassNotFoundException {
        final String name = c.getName();
        final ProtectionDomain pd = c.getProtectionDomain();
        byte[] bytecode = readByteCode(name);

        for (ClassFileTransformer xf : transformers) {
            try {
                bytecode = xf.transform(this, name, null, pd, bytecode);
            } catch (IllegalClassFormatException ex) {
                throw new ClassNotFoundException(name, ex);
            }
        }
        return defineClass(name, bytecode, 0, bytecode.length, pd);
    }

    private Class<?> findClassUnshadowed(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    private byte[] readByteCode(final String className) throws ClassNotFoundException {
        final String resourceName = className.replace('.', '/') + ".class";
        InputStream is = getResourceAsStream(resourceName);
        if (is == null) {
            throw new ClassNotFoundException(className);
        }
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8196];
            int bytesRead;
            while ( (bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ClassNotFoundException(className, e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new ClassNotFoundException(className, e);
            }
        }
    }    

    
    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {

        if (System.getSecurityManager() == null)
            return super.getPermissions(codesource);
        
        //when security manager is enabled, find the declared permissions        
        if (clientCLDelegate.getCachedPerms(codesource) != null)
            return clientCLDelegate.getCachedPerms(codesource);

        return clientCLDelegate.getPermissions(codesource, 
                super.getPermissions(codesource));
    }
    
    
    public void processDeclaredPermissions() throws IOException  {

        if (clientCLDelegate == null)
            clientCLDelegate = new ClientClassLoaderDelegate(this);
    }    
    
}
