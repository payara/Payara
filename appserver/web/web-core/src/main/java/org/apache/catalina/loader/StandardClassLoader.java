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

package org.apache.catalina.loader;

import org.apache.catalina.core.StandardServer;
import org.apache.naming.JndiPermission;
import org.glassfish.web.loader.Reloader;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Subclass implementation of <b>java.net.URLClassLoader</b> that knows how
 * to load classes from disk directories, as well as local and remote JAR
 * files.  It also implements the <code>Reloader</code> interface, to provide
 * automatic reloading support to the associated loader.
 * <p>
 * In all cases, URLs must conform to the contract specified by
 * <code>URLClassLoader</code> - any URL that ends with a "/" character is
 * assumed to represent a directory; all other URLs are assumed to be the
 * address of a JAR file.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong> - Local repositories are searched in
 * the order they are added via the initial constructor and/or any subsequent
 * calls to <code>addRepository()</code>.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong> - At present, there are no dependencies
 * from this class to any other Catalina class, so that it could be used
 * independently.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.3 $ $Date: 2006/03/12 01:27:02 $
 */

public class StandardClassLoader
    extends URLClassLoader
    implements Reloader {

    private static final Logger log = StandardServer.log;


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new ClassLoader with no defined repositories and no
     * parent ClassLoader.
     */
    public StandardClassLoader() {

        super(new URL[0]);
        this.parent = getParent();
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();

    }


    /**
     * Construct a new ClassLoader with no defined repositories and no
     * parent ClassLoader, but with a stream handler factory.
     *
     * @param factory the URLStreamHandlerFactory to use when creating URLs
     */
    public StandardClassLoader(URLStreamHandlerFactory factory) {

        super(new URL[0], null, factory);
        this.factory = factory;

    }


    /**
     * Construct a new ClassLoader with no defined repositories and the
     * specified parent ClassLoader.
     *
     * @param parent The parent ClassLoader
     */
    public StandardClassLoader(ClassLoader parent) {

        super((new URL[0]), parent);
        this.parent = parent;
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();

    }


    /**
     * Construct a new ClassLoader with no defined repositories and the
     * specified parent ClassLoader.
     *
     * @param parent The parent ClassLoader
     * @param factory the URLStreamHandlerFactory to use when creating URLs
     */
    public StandardClassLoader(ClassLoader parent,
                               URLStreamHandlerFactory factory) {

        super((new URL[0]), parent, factory);
        this.factory = factory;

    }


    /**
     * Construct a new ClassLoader with the specified repositories and
     * no parent ClassLoader.
     *
     * @param repositories The initial set of repositories
     */
    public StandardClassLoader(String repositories[]) {
        this(convert(repositories));
    }


    /**
     * Construct a new ClassLoader with the specified repositories and
     * parent ClassLoader.
     *
     * @param repositories The initial set of repositories
     * @param parent The parent ClassLoader
     */
    public StandardClassLoader(String repositories[], ClassLoader parent) {
        this(convert(repositories), parent);
    }


    /**
     * Construct a new ClassLoader with the specified repositories and
     * no parent ClassLoader.
     *
     * @param repositories The initial set of repositories
     */
    public StandardClassLoader(URL repositories[]) {

        super(repositories);
        this.parent = getParent();
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
        if (repositories != null) {
            for (int i = 0; i < repositories.length; i++) {
                addRepositoryInternal(repositories[i].toString());
            }
        }
    }


    /**
     * Construct a new ClassLoader with the specified repositories and
     * parent ClassLoader.
     *
     * @param repositories The initial set of repositories
     * @param parent The parent ClassLoader
     */
    public StandardClassLoader(URL repositories[], ClassLoader parent) {

        super(repositories, parent);
        this.parent = parent;
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
        if (repositories != null) {
            for (int i = 0; i < repositories.length; i++) {
                addRepositoryInternal(repositories[i].toString());
            }
        }
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The debugging detail level of this component.
     */
    protected int debug = 0;


    /**
     * Should this class loader delegate to the parent class loader
     * <strong>before</strong> searching its own repositories (i.e. the
     * usual Java2 delegation model)?  If set to <code>false</code>,
     * this class loader will search its own repositories first, and
     * delegate to the parent only if the class or resource is not
     * found locally.
     */
    protected boolean delegate = false;


    /**
     * The list of local repositories, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected ArrayList<String> repositories = new ArrayList<String>();


    /**
     * A list of read File and Jndi Permission's required if this loader
     * is for a web application context.
     */
    private ArrayList<Permission> permissionList = new ArrayList<Permission>();


    /**
     * The PermissionCollection for each CodeSource for a web
     * application context.
     */
    private HashMap<String, PermissionCollection> loaderPC =
        new HashMap<String, PermissionCollection>();


    /**
     * Instance of the SecurityManager installed.
     */
    private SecurityManager securityManager = null;


    /**
     * Flag that the security policy has been refreshed from file.
     */
    private boolean policy_refresh = false;

    /**
     * The parent class loader.
     */
    private ClassLoader parent = null;


    /**
     * The system class loader.
     */
    private ClassLoader system = null;


    /**
     * URL stream handler for additional protocols.
     */
    protected URLStreamHandlerFactory factory = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (this.debug);

    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {

        this.debug = debug;

    }


    /**
     * Return the "delegate first" flag for this class loader.
     */
    public boolean getDelegate() {

        return (this.delegate);

    }


    /**
     * Set the "delegate first" flag for this class loader.
     *
     * @param delegate The new "delegate first" flag
     */
    public void setDelegate(boolean delegate) {

        this.delegate = delegate;

    }


    /**
     * If there is a Java SecurityManager create a read FilePermission
     * or JndiPermission for the file directory path.
     *
     * @param path file directory path
     */
    protected void setPermissions(String path) {
        if( securityManager != null ) {
            if( path.startsWith("jndi:") || path.startsWith("jar:jndi:") ) {
                permissionList.add(new JndiPermission(path + "*"));
            } else {
                permissionList.add(new FilePermission(path + "-","read"));
            }
        }
    }


    /**
     * If there is a Java SecurityManager add a read FilePermission
     * or JndiPermission for URL.
     *
     * @param url URL for a file or directory on local system
     */
    protected void setPermissions(URL url) {
        setPermissions(url.toString());
    }


    // ------------------------------------------------------- Reloader Methods


    /**
     * Add a new repository to the set of places this ClassLoader can look for
     * classes to be loaded.
     *
     * @param repository Name of a source of classes to be loaded, such as a
     *  directory pathname, a JAR file pathname, or a ZIP file pathname
     *
     * @exception IllegalArgumentException if the specified repository is
     *  invalid or does not exist
     */
    public void addRepository(String repository) {

        if (debug >= 1)
            log("addRepository(" + repository + ")");

        // Add this repository to our underlying class loader
        try {
            URLStreamHandler streamHandler = null;
            String protocol = parseProtocol(repository);
            if (factory != null)
                streamHandler = factory.createURLStreamHandler(protocol);
            URL url = new URL(null, repository, streamHandler);
            super.addURL(url);
        } catch (MalformedURLException e) {
            IllegalArgumentException iae = new IllegalArgumentException
                ("Invalid repository: " + repository);
            iae.initCause(e);
            throw iae;
        }

        // Add this repository to our internal list
        addRepositoryInternal(repository);

    }


    /**
     * This class loader doesn't check for reloading.
     */
    public boolean modified() {

        return (false);

    }


    /**
     * Render a String representation of this object.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("StandardClassLoader\r\n");
        sb.append("  delegate: ");
        sb.append(delegate);
        sb.append("\r\n");
        sb.append("  repositories:\r\n");
        Iterator<String> iter = repositories.iterator();
        while (iter.hasNext()) {
            sb.append("    ");
            sb.append(iter.next());
            sb.append("\r\n");
        }
        if (this.parent != null) {
            sb.append("----------> Parent Classloader:\r\n");
            sb.append(this.parent.toString());
            sb.append("\r\n");
        }
        return (sb.toString());

    }


    // ---------------------------------------------------- ClassLoader Methods


    /**
     * Find the specified class in our local repositories, if possible.  If
     * not found, throw <code>ClassNotFoundException</code>.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class findClass(String name) throws ClassNotFoundException {

        if (debug >= 3)
            log("    findClass(" + name + ")");

        // (1) Permission to define this class when using a SecurityManager
        if (securityManager != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    if (debug >= 4)
                        log("      securityManager.checkPackageDefinition");
                    securityManager.checkPackageDefinition(name.substring(0,i));
                } catch (Exception se) {
                    if (debug >= 4)
                        log("      -->Exception-->ClassNotFoundException", se);
                    throw new ClassNotFoundException(name, se);
                }
            }
        }

        // Ask our superclass to locate this class, if possible
        // (throws ClassNotFoundException if it is not found)
        Class clazz = null;
        try {
            if (debug >= 4)
                log("      super.findClass(" + name + ")");
            try {
                synchronized (this) {
                    clazz = findLoadedClass(name);
                    if (clazz != null)
                        return clazz;
                    clazz = super.findClass(name);
                }
            } catch(AccessControlException ace) {
                throw new ClassNotFoundException(name, ace);
            } catch (RuntimeException e) {
                if (debug >= 4)
                    log("      -->RuntimeException Rethrown", e);
                throw e;
            }
            if (clazz == null) {
                if (debug >= 3)
                    log("    --> Returning ClassNotFoundException");
                throw new ClassNotFoundException(name);
            }
        } catch (ClassNotFoundException e) {
            if (debug >= 3)
                log("    --> Passing on ClassNotFoundException", e);
            throw e;
        }

        // Return the class we have located
        if (debug >= 4) {
            log("      Returning class " + clazz);
            log("      Loaded by " + clazz.getClassLoader());
        }
        return (clazz);

    }


    /**
     * Find the specified resource in our local repository, and return a
     * <code>URL</code> refering to it, or <code>null</code> if this resource
     * cannot be found.
     *
     * @param name Name of the resource to be found
     */
    public URL findResource(String name) {

        if (debug >= 3)
            log("    findResource(" + name + ")");

        URL url = super.findResource(name);
        if (debug >= 3) {
            if (url != null)
                log("    --> Returning '" + url.toString() + "'");
            else
                log("    --> Resource not found, returning null");
        }
        return (url);

    }


    /**
     * Return an enumeration of <code>URLs</code> representing all of the
     * resources with the given name.  If no resources with this name are
     * found, return an empty enumeration.
     *
     * @param name Name of the resources to be found
     *
     * @exception IOException if an input/output error occurs
     */
    public Enumeration<URL> findResources(String name) throws IOException {

        if (debug >= 3)
            log("    findResources(" + name + ")");
        return (super.findResources(name));

    }


    /**
     * Find the resource with the given name.  A resource is some data
     * (images, audio, text, etc.) that can be accessed by class code in a
     * way that is independent of the location of the code.  The name of a
     * resource is a "/"-separated path name that identifies the resource.
     * If the resource cannot be found, return <code>null</code>.
     * <p>
     * This method searches according to the following algorithm, returning
     * as soon as it finds the appropriate URL.  If the resource cannot be
     * found, returns <code>null</code>.
     * <ul>
     * <li>If the <code>delegate</code> property is set to <code>true</code>,
     *     call the <code>getResource()</code> method of the parent class
     *     loader, if any.</li>
     * <li>Call <code>findResource()</code> to find this resource in our
     *     locally defined repositories.</li>
     * <li>Call the <code>getResource()</code> method of the parent class
     *     loader, if any.</li>
     * </ul>
     *
     * @param name Name of the resource to return a URL for
     */
    public URL getResource(String name) {

        if (debug >= 2)
            log("getResource(" + name + ")");
        URL url = null;

        // (1) Delegate to parent if requested
        if (delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            url = loader.getResource(name);
            if (url != null) {
                if (debug >= 2)
                    log("  --> Returning '" + url.toString() + "'");
                return (url);
            }
        }

        // (2) Search local repositories
        if (debug >= 3)
            log("  Searching local repositories");
        url = findResource(name);
        if (url != null) {
            if (debug >= 2)
                log("  --> Returning '" + url.toString() + "'");
            return (url);
        }

        // (3) Delegate to parent unconditionally if not already attempted
        if( !delegate ) {
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            url = loader.getResource(name);
            if (url != null) {
                if (debug >= 2)
                    log("  --> Returning '" + url.toString() + "'");
                return (url);
            }
        }

        // (4) Resource was not found
        if (debug >= 2)
            log("  --> Resource not found, returning null");
        return (null);

    }


    /**
     * Find the resource with the given name, and return an input stream
     * that can be used for reading it.  The search order is as described
     * for <code>getResource()</code>, after checking to see if the resource
     * data has been previously cached.  If the resource cannot be found,
     * return <code>null</code>.
     *
     * @param name Name of the resource to return an input stream for
     */
    public InputStream getResourceAsStream(String name) {

        if (debug >= 2)
            log("getResourceAsStream(" + name + ")");
        InputStream stream = null;

        // (0) Check for a cached copy of this resource
        stream = findLoadedResource(name);
        if (stream != null) {
            if (debug >= 2)
                log("  --> Returning stream from cache");
            return (stream);
        }

        // (1) Delegate to parent if requested
        if (delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                // FIXME - cache???
                if (debug >= 2)
                    log("  --> Returning stream from parent");
                return (stream);
            }
        }

        // (2) Search local repositories
        if (debug >= 3)
            log("  Searching local repositories");
        URL url = findResource(name);
        if (url != null) {
            // FIXME - cache???
            if (debug >= 2)
                log("  --> Returning stream from local");
            try {
               return (url.openStream());
            } catch (IOException e) {
               log("url.openStream(" + url.toString() + ")", e);
               return (null);
            }
        }

        // (3) Delegate to parent unconditionally
        if (!delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                // FIXME - cache???
                if (debug >= 2)
                    log("  --> Returning stream from parent");
                return (stream);
            }
        }

        // (4) Resource was not found
        if (debug >= 2)
            log("  --> Resource not found, returning null");
        return (null);

    }


    /**
     * Load the class with the specified name.  This method searches for
     * classes in the same manner as <code>loadClass(String, boolean)</code>
     * with <code>false</code> as the second argument.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class loadClass(String name) throws ClassNotFoundException {

        return (loadClass(name, false));

    }


    /**
     * Load the class with the specified name, searching using the following
     * algorithm until it finds and returns the class.  If the class cannot
     * be found, returns <code>ClassNotFoundException</code>.
     * <ul>
     * <li>Call <code>findLoadedClass(String)</code> to check if the
     *     class has already been loaded.  If it has, the same
     *     <code>Class</code> object is returned.</li>
     * <li>If the <code>delegate</code> property is set to <code>true</code>,
     *     call the <code>loadClass()</code> method of the parent class
     *     loader, if any.</li>
     * <li>Call <code>findClass()</code> to find this class in our locally
     *     defined repositories.</li>
     * <li>Call the <code>loadClass()</code> method of our parent
     *     class loader, if any.</li>
     * </ul>
     * If the class was found using the above steps, and the
     * <code>resolve</code> flag is <code>true</code>, this method will then
     * call <code>resolveClass(Class)</code> on the resulting Class object.
     *
     * @param name Name of the class to be loaded
     * @param resolve If <code>true</code> then resolve the class
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException {

        if (debug >= 2)
            log("loadClass(" + name + ", " + resolve + ")");
        Class clazz = null;

        // (0) Check our previously loaded class cache
        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (debug >= 3)
                log("  Returning class from cache");
            if (resolve)
                resolveClass(clazz);
            return (clazz);
        }

        // If a system class, use system class loader
        if( name.startsWith("java.") ) {
            ClassLoader loader = system;
            clazz = loader.loadClass(name);
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
            throw new ClassNotFoundException(name);
        }

        // (.5) Permission to access this class when using a SecurityManager
        if (securityManager != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    securityManager.checkPackageAccess(name.substring(0,i));
                } catch (SecurityException se) {
                    String error = "Security Violation, attempt to use " +
                        "Restricted Class: " + name;
                    log(error);
                    throw new ClassNotFoundException(error, se);
                }
            }
        }

        // (1) Delegate to our parent if requested
        if (delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    if (debug >= 3)
                        log("  Loading class from parent");
                    if (resolve)
                        resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }

        // (2) Search local repositories
        if (debug >= 3)
            log("  Searching local repositories");
        try {
            clazz = findClass(name);
            if (clazz != null) {
                if (debug >= 3)
                    log("  Loading class from local repository");
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
        } catch (ClassNotFoundException e) {
            ;
        }

        // (3) Delegate to parent unconditionally
        if (!delegate) {
            if (debug >= 3)
                log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    if (debug >= 3)
                        log("  Loading class from parent");
                    if (resolve)
                        resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }

        // This class was not found
        throw new ClassNotFoundException(name);

    }


    /**
     * Get the Permissions for a CodeSource.  If this instance
     * of StandardClassLoader is for a web application context,
     * add read FilePermissions for the base directory (if unpacked),
     * the context URL, and jar file resources.
     *
     * @param codeSource where the code was loaded from
     * @return PermissionCollection for CodeSource
     */
    protected final PermissionCollection getPermissions(CodeSource codeSource) {
        if (!policy_refresh) {
            // Refresh the security policies
            Policy policy = Policy.getPolicy();
            policy.refresh();
            policy_refresh = true;
        }
        String codeUrl = codeSource.getLocation().toString();
        PermissionCollection pc;
        if ((pc = loaderPC.get(codeUrl)) == null) {
            pc = super.getPermissions(codeSource);
            if (pc != null) {
                Iterator<Permission> perms = permissionList.iterator();
                while (perms.hasNext()) {
                    Permission p = perms.next();
                    pc.add(p);
                }
                loaderPC.put(codeUrl,pc);
            }
        }
        return (pc);

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Parse URL protocol.
     *
     * @return String protocol
     */
    protected static String parseProtocol(String spec) {
        if (spec == null)
            return "";
        int pos = spec.indexOf(':');
        if (pos <= 0)
            return "";
        return spec.substring(0, pos).trim();
    }


    /**
     * Add a repository to our internal array only.
     *
     * @param repository The new repository
     *
     * @exception IllegalArgumentException if the manifest of a JAR file
     *  cannot be processed correctly
     */
    protected void addRepositoryInternal(String repository) {

        URLStreamHandler streamHandler = null;
        String protocol = parseProtocol(repository);
        if (factory != null)
            streamHandler = factory.createURLStreamHandler(protocol);

        // Validate the manifest of a JAR file repository
        if (!repository.endsWith(File.separator) &&
            !repository.endsWith("/")) {
            JarFile jarFile = null;
            try {
                if (repository.startsWith("jar:")) {
                    URL url = new URL(null, repository, streamHandler);
                    JarURLConnection conn =
                        (JarURLConnection) url.openConnection();
                    conn.setAllowUserInteraction(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(false);
                    conn.connect();
                    jarFile = conn.getJarFile();
                } else if (repository.startsWith("file://")) {
                    jarFile = new JarFile(repository.substring(7));
                } else if (repository.startsWith("file:")) {
                    jarFile = new JarFile(repository.substring(5));
                } else if (repository.endsWith(".jar")) {
                    URL url = new URL(null, repository, streamHandler);
                    URLConnection conn = url.openConnection();
                    JarInputStream jis =
                        new JarInputStream(conn.getInputStream());
                    try {
                        jis.getManifest();
                    } finally {
                        try {
                            jis.close();
                        } catch(Throwable t) {
                        }
                    }
                } else {
                    throw new IllegalArgumentException
                        ("addRepositoryInternal:  Invalid URL '" +
                         repository + "'");
                }
            } catch (Throwable t) {
                IllegalArgumentException iae = new IllegalArgumentException
                    ("addRepositoryInternal");
                iae.initCause(t);
                throw iae;
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (Throwable t) {}
                }
            }
        }

        // Add this repository to our internal list
        repositories.add(repository);
    }


    /**
     * Convert an array of String to an array of URL and return it.
     *
     * @param input The array of String to be converted
     */
    protected static URL[] convert(String input[]) {
        return convert(input, null);
    }


    /**
     * Convert an array of String to an array of URL and return it.
     *
     * @param input The array of String to be converted
     * @param factory Handler factory to use to generate the URLs
     */
    protected static URL[] convert(String input[],
                                   URLStreamHandlerFactory factory) {

        URLStreamHandler streamHandler = null;

        URL url[] = new URL[input.length];
        for (int i = 0; i < url.length; i++) {
            try {
                String protocol = parseProtocol(input[i]);
                if (factory != null)
                    streamHandler = factory.createURLStreamHandler(protocol);
                else
                    streamHandler = null;
                url[i] = new URL(null, input[i], streamHandler);
            } catch (MalformedURLException e) {
                url[i] = null;
            }
        }
        return (url);

    }


    /**
     * Finds the resource with the given name if it has previously been
     * loaded and cached by this class loader, and return an input stream
     * to the resource data.  If this resource has not been cached, return
     * <code>null</code>.
     *
     * @param name Name of the resource to return
     */
    protected InputStream findLoadedResource(String name) {

        return (null);  // FIXME - findLoadedResource()

    }


    /**
     * Log a debugging output message.
     *
     * @param message Message to be logged
     */
    private void log(String message) {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "StandardClassLoader: " + message);

    }


    /**
     * Log a debugging output message with an exception.
     *
     * @param message Message to be logged
     * @param throwable Exception to be logged
     */
    private void log(String message, Throwable throwable) {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "StandardClassLoader: " + message, throwable);

    }


}

