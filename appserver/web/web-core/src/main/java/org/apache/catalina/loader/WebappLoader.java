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


import org.apache.catalina.*;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.DirContextURLStreamHandlerFactory;
import org.apache.naming.resources.Resource;
import org.apache.tomcat.util.modeler.Registry;
import org.glassfish.web.loader.WebappClassLoader;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classloader implementation which is specialized for handling web
 * applications in the most efficient way, while being Catalina aware (all
 * accesses to resources are made through the DirContext interface).
 * This class loader supports detection of modified
 * Java classes, which can be used to implement auto-reload support.
 * <p>
 * This class loader is configured by adding the pathnames of directories,
 * JAR files, and ZIP files with the <code>addRepository()</code> method,
 * prior to calling <code>start()</code>.  When a new class is required,
 * these repositories will be consulted first to locate the class.  If it
 * is not present, the system class loader will be used instead.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.10 $ $Date: 2007/05/05 05:32:09 $
 */

public class WebappLoader
    implements Lifecycle, Loader, PropertyChangeListener, MBeanRegistration  {

    /**
     * First load of the class.
     */
    private static boolean first = true;

    private static Logger log = Logger.getLogger(
        WebappLoader.class.getName());

    // --------------------------------------------------------- Constructors

    /**
     * Construct a new WebappLoader with no defined parent class loader
     * (so that the actual parent will be the system class loader).
     */
    public WebappLoader() {

        this(null);

    }


    /**
     * Construct a new WebappLoader with the specified class loader
     * to be defined as the parent of the ClassLoader we ultimately create.
     *
     * @param parent The parent class loader
     */
    public WebappLoader(ClassLoader parent) {
        super();
        this.parentClassLoader = parent;
    }


    // --------------------------------------------------- Instance Variables

    private ObjectName oname;
    private ObjectName controller;

    /**
     * The class loader being managed by this Loader component.
     */
    private WebappClassLoader classLoader = null;


    /**
     * The Container with which this Loader has been associated.
     */
    private Container container = null;


    /**
     * The debugging detail level for this component.
     */
    private int debug = 0;


    /**
     * The DefaultContext with which this Loader is associated.
     */
    protected DefaultContext defaultContext = null;
    
    
    /**
     * The "follow standard delegation model" flag that will be used to
     * configure our ClassLoader.
     */
    private boolean delegate = false;


    /**
     * The descriptive information about this Loader implementation.
     */
    private static final String info =
        "org.apache.catalina.loader.WebappLoader/1.0";


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The Java class name of the ClassLoader implementation to be used.
     * This class should extend WebappClassLoader, otherwise, a different 
     * loader implementation must be used.
     */
    private String loaderClass =
        "org.glassfish.web.loader.WebappClassLoader";


    /**
     * The parent class loader of the class loader we will create.
     */
    private ClassLoader parentClassLoader = null;


    /**
     * The reloadable flag for this Loader.
     */
    private boolean reloadable = false;


    /**
     * The set of repositories associated with this class loader.
     */
    private String repositories[] = new String[0];


    /**
     * The string manager for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(WebappLoader.class.getPackage().getName());


    /**
     * Has this component been started?
     */
    private boolean started = false;


    /**
     * The property change support for this component.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    /**
     * Classpath set in the loader.
     */
    private String classpath = null;


    // START PE 4985680`
    /**
     * List of packages that may always be overridden, regardless of whether
     * they belong to a protected namespace (i.e., a namespace that may never
     * be overridden by a webapp)  
     */
    private ArrayList<String> overridablePackages;
    // END PE 4985680


    // START PWC 1.1 6314481
    private boolean ignoreHiddenJarFiles;
    // END PWC 1.1 6314481
    
    private boolean useMyFaces;

    // ------------------------------------------------------------- Properties


    /**
     * Return the Java class loader to be used by this Container.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }


    /**
     * Return the Container with which this Logger has been associated.
     */
    public Container getContainer() {
        return (container);
    }


    /**
     * Set the Container with which this Logger has been associated.
     *
     * @param container The associated Container
     */
    public void setContainer(Container container) {

        // Deregister from the old Container (if any)
        if ((this.container != null) && (this.container instanceof Context))
            ((Context) this.container).removePropertyChangeListener(this);

        // Process this property change
        Container oldContainer = this.container;
        this.container = container;
        support.firePropertyChange("container", oldContainer, this.container);

        // Register with the new Container (if any)
        if ((this.container != null) && (this.container instanceof Context)) {
            setReloadable( ((Context) this.container).getReloadable() );
            ((Context) this.container).addPropertyChangeListener(this);
        }
    }


    /**
     * Return the DefaultContext with which this Loader is associated.
     * 4985680 What is that ???
     */
    public DefaultContext getDefaultContext() {
        return (this.defaultContext);
    }


    /**
     * Set the DefaultContext with which this Loader is associated.
     *
     * @param defaultContext The newly associated DefaultContext
     */
    public void setDefaultContext(DefaultContext defaultContext) {

        DefaultContext oldDefaultContext = this.defaultContext;
        this.defaultContext = defaultContext;
        support.firePropertyChange("defaultContext", oldDefaultContext, this.defaultContext);

    }


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
        int oldDebug = this.debug;
        this.debug = debug;
        support.firePropertyChange("debug", Integer.valueOf(oldDebug),
                                   Integer.valueOf(this.debug));
    }


    /**
     * Return the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     */
    public boolean getDelegate() {
        return (this.delegate);
    }


    /**
     * Set the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     *
     * @param delegate The new flag
     */
    public void setDelegate(boolean delegate) {
        boolean oldDelegate = this.delegate;
        this.delegate = delegate;
        support.firePropertyChange("delegate", Boolean.valueOf(oldDelegate),
                                   Boolean.valueOf(this.delegate));
    }


    /**
     * Return descriptive information about this Loader implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return (info);
    }


    /**
     * Return the ClassLoader class name.
     */
    public String getLoaderClass() {
        return (this.loaderClass);
    }


    /**
     * Set the ClassLoader class name.
     *
     * @param loaderClass The new ClassLoader class name
     */
    public void setLoaderClass(String loaderClass) {
        this.loaderClass = loaderClass;
    }


    /**
     * Return the reloadable flag for this Loader.
     */
    public boolean getReloadable() {
        return (this.reloadable);
    }


    /**
     * Set the reloadable flag for this Loader.
     *
     * @param reloadable The new reloadable flag
     */
    public void setReloadable(boolean reloadable) {

        // Process this property change
        boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
        support.firePropertyChange("reloadable",
                                   Boolean.valueOf(oldReloadable),
                                   Boolean.valueOf(this.reloadable));
    }


    public void setUseMyFaces(boolean useMyFaces) {
        this.useMyFaces = useMyFaces;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Add a property change listener to this component.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    /**
     * Add a new repository to the set of repositories for this class loader.
     *
     * @param repository Repository to be added
     */
    public void addRepository(String repository) {

        if (log.isLoggable(Level.FINEST))
            log.finest(sm.getString("webappLoader.addRepository",
                                    repository));

        for (int i = 0; i < repositories.length; i++) {
            if (repository.equals(repositories[i]))
                return;
        }
        String results[] = new String[repositories.length + 1];
        for (int i = 0; i < repositories.length; i++)
            results[i] = repositories[i];
        results[repositories.length] = repository;
        repositories = results;

        if (started && (classLoader != null)) {
            classLoader.addRepository(repository);
            setClassPath();
        }

    }


    /**
     * Return the set of repositories defined for this class loader.
     * If none are defined, a zero-length array is returned.
     * For security reason, returns a clone of the Array (since 
     * String are immutable).
     */
    public String[] findRepositories() {
        return repositories.clone();
    }

    public String[] getRepositories() {
        return repositories.clone();
    }


    /** 
     * Classpath, as set in org.apache.catalina.jsp_classpath context
     * property
     *
     * @return The classpath
     */
    public String getClasspath() {
        return classpath;
    }


    /**
     * Has the internal repository associated with this Loader been modified,
     * such that the loaded classes should be reloaded?
     */
    public boolean modified() {
        return (classLoader.modified());
    }


    /**
     * Used to periodically signal to the classloader to release JAR resources.
     */
    public void closeJARs(boolean force) {
        if (classLoader !=null){
            classLoader.closeJARs(force);
        }
    }


    /**
     * Remove a property change listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    /**
     * Return a String representation of this component.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("WebappLoader[");
        if (container != null)
            sb.append(container.getName());
        sb.append("]");
        return (sb.toString());
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * Gets the (possibly empty) list of lifecycle listeners associated
     * with this WebappLoader.
     */
    public List<LifecycleListener> findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    private boolean initialized=false;

    public void init() {
        initialized=true;

        if( oname==null ) {
            // not registered yet - standalone or API
            if( container instanceof StandardContext) {
                // Register ourself. The container must be a webapp
                try {
                    StandardContext ctx=(StandardContext)container;
                    String path = ctx.getEncodedPath();
                    if (path.equals("")) {
                        path = "/";
                    }   
                    oname = new ObjectName(ctx.getEngineName() +
                                           ":type=Loader,path=" +
                                           path + ",host=" +
                                           ctx.getParent().getName());
                    // Do not register unused tomcat mbeans
                    //Registry.getRegistry(null, null).registerComponent(this, oname,
                    //                                         null);
                    controller = oname;
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error registering loader", e);
                }
            }
        }

        /*
        if( container == null ) {
            // JMX created the loader
            // TODO
        }
        */
    }

    public void destroy() {
        if( controller==oname ) {
            // Do not register unused tomcat mbeans
            //Registry.getRegistry(null, null).unregisterComponent(oname);
            oname = null;
        }
        initialized = false;

    }

    private static synchronized void initStreamHandlerFactory() {
        // Register a stream handler factory for the JNDI protocol
        URLStreamHandlerFactory streamHandlerFactory =
            new DirContextURLStreamHandlerFactory();

        synchronized (WebappLoader.class) {
            if (first) {
                first = false;
                try {
                    URL.setURLStreamHandlerFactory(streamHandlerFactory);
                } catch (Exception e) {
                    // Log and continue anyway, this is not critical
                    log.log(Level.SEVERE,
                            "Error registering jndi stream handler", e);
                } catch (Throwable t) {
                    // This is likely a dual registration
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("Dual registration of jndi stream handler: " +
                            t.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Start this component, initializing our associated class loader.
     *
     * @exception LifecycleException if a lifecycle error occurs
     */
    public void start() throws LifecycleException {
        // Validate and update our current component state
        if( ! initialized ) init();
        if (started)
            throw new LifecycleException
                (sm.getString("webappLoader.alreadyStarted"));
        if (log.isLoggable(Level.FINEST))
            log.finest(sm.getString("webappLoader.starting"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        if (container.getResources() == null) {
            if (log.isLoggable(Level.INFO)) {
                log.info("No resources for " + container);
            }
            return;
        }
        // Register a stream handler factory for the JNDI protocol
        initStreamHandlerFactory();

        // Construct a class loader based on our current repositories list
        try {

            ClassLoader cl = createClassLoader();
            if (cl instanceof WebappClassLoader) {
                classLoader = (WebappClassLoader) cl;
            } else {
                classLoader = new WebappClassLoader(cl);
            }
            classLoader.setResources(container.getResources());
            classLoader.setDebug(this.debug);
            classLoader.setDelegate(this.delegate);

            for (int i = 0; i < repositories.length; i++) {
                classLoader.addRepository(repositories[i]);
            }
            
            // START OF PE 4985680
            if (overridablePackages != null){
                for (int i = 0; i < overridablePackages.size(); i++) {
                    classLoader.addOverridablePackage(
                                            overridablePackages.get(i));
                }
                overridablePackages = null;
            }
            // END OF PE 4985680

            // Configure our repositories
            setRepositories();
            setClassPath();

            setPermissions();

            // Binding the Webapp class loader to the directory context
            DirContextURLStreamHandler.bind(classLoader,
                    this.container.getResources());

        } catch (Throwable t) {
            log.log(Level.SEVERE, "LifecycleException ", t);
            throw new LifecycleException("start: ", t);
        }

    }


    /**
     * Stop this component, finalizing our associated class loader.
     *
     * @exception LifecycleException if a lifecycle error occurs
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("webappLoader.notStarted"));
        if (log.isLoggable(Level.FINEST))
            log.finest(sm.getString("webappLoader.stopping"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Remove context attributes as appropriate
        if (container instanceof Context) {
            ServletContext servletContext =
                ((Context) container).getServletContext();
            servletContext.removeAttribute(Globals.CLASS_PATH_ATTR);
        }

        // Throw away our current class loader
        stopNestedClassLoader();
        DirContextURLStreamHandler.unbind(classLoader);

        classLoader = null;

        destroy();
    }


    /**
     * Stops the nested classloader
     */
    public void stopNestedClassLoader() throws LifecycleException {
        try {
            classLoader.stop();
        } catch (Exception e) {
            throw new LifecycleException(e);
        }
    }


    // ----------------------------------------- PropertyChangeListener Methods


    /**
     * Process property change events from our associated Context.
     *
     * @param event The property change event that has occurred
     */
    public void propertyChange(PropertyChangeEvent event) {

        // Validate the source of this event
        if (!(event.getSource() instanceof Context))
            return;

        // Process a relevant property change
        if (event.getPropertyName().equals("reloadable")) {
            try {
                setReloadable
                    ( ((Boolean) event.getNewValue()).booleanValue() );
            } catch (NumberFormatException e) {
                log.log(Level.SEVERE,
                        sm.getString("webappLoader.reloadable",
                                     event.getNewValue().toString()));
            }
        }
    }


    // ------------------------------------------------------- Private Methods


    /**
     * Create associated classLoader.
     */
    protected ClassLoader createClassLoader()
        throws Exception {

        Class<?> clazz = Class.forName(loaderClass);
        WebappClassLoader classLoader = null;

        if (parentClassLoader == null) {
            parentClassLoader = Thread.currentThread().getContextClassLoader();
        }
        Class<?>[] argTypes = { ClassLoader.class };
        Object[] args = { parentClassLoader };
        Constructor<?> constr = clazz.getConstructor(argTypes);
        classLoader = (WebappClassLoader) constr.newInstance(args);

        classLoader.setUseMyFaces(useMyFaces);

        /*
         * Start the WebappClassLoader here as opposed to in the course of
         * WebappLoader#start, in order to prevent it from being started
         * twice (during normal deployment, the WebappClassLoader is created
         * by the deployment backend without calling
         * WebappLoader#createClassLoader, and will have been started
         * by the time WebappLoader#start is called)
         */
        try {
            classLoader.start();
        } catch (Exception e) {
            throw new LifecycleException(e);
        }

        return classLoader;

    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        org.apache.catalina.Logger logger = null;
        String containerName = null;
        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }
        if (logger != null) {
            logger.log("WebappLoader[" + containerName + "]: " +
                message);
        } else {
            if (log.isLoggable(Level.INFO)) {
                log.info("WebappLoader[" + containerName + "]: " + message);
            }
        }
    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     * @param t Associated exception
     */
    private void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = null;
        String containerName = null;
        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }
        if (logger != null) {
            logger.log("WebappLoader[" + containerName + "] " + message, t);
        } else {
            log.log(Level.WARNING,
                "WebappLoader[" + containerName + "]: " + message, t);
        }
    }


    /**
     * Configure associated class loader permissions.
     */
    private void setPermissions() {

        if (!Globals.IS_SECURITY_ENABLED)
            return;
        if (!(container instanceof Context))
            return;

        // Tell the class loader the root of the context
        ServletContext servletContext =
            ((Context) container).getServletContext();

        // Assigning permissions for the work directory
        File workDir =
            (File) servletContext.getAttribute(ServletContext.TEMPDIR);
        if (workDir != null) {
            try {
                String workDirPath = workDir.getCanonicalPath();
                classLoader.addPermission
                    (new FilePermission(workDirPath, "read,write"));
                classLoader.addPermission
                    (new FilePermission(workDirPath + File.separator + "-", 
                                        "read,write,delete"));
            } catch (IOException e) {
                // Ignore
            }
        }

        try {

            URL rootURL = servletContext.getResource("/");
            classLoader.addPermission(rootURL);

            String contextRoot = servletContext.getRealPath("/");
            if (contextRoot != null) {
                try {
                    contextRoot = (new File(contextRoot)).getCanonicalPath();
                    classLoader.addPermission(contextRoot);
                } catch (IOException e) {
                    // Ignore
                }
            }

            URL classesURL = servletContext.getResource("/WEB-INF/classes/");
            classLoader.addPermission(classesURL);
            URL libURL = servletContext.getResource("/WEB-INF/lib/");
            classLoader.addPermission(libURL);

            if (contextRoot != null) {

                if (libURL != null) {
                    File rootDir = new File(contextRoot);
                    File libDir = new File(rootDir, "WEB-INF/lib/");
                    try {
                        String path = libDir.getCanonicalPath();
                        classLoader.addPermission(path);
                    } catch (IOException e) {
                    }
                }

            } else {

                if (workDir != null) {
                    if (libURL != null) {
                        File libDir = new File(workDir, "WEB-INF/lib/");
                        try {
                            String path = libDir.getCanonicalPath();
                            classLoader.addPermission(path);
                        } catch (IOException e) {
                        }
                    }
                    if (classesURL != null) {
                        File classesDir = new File(workDir, "WEB-INF/classes/");
                        try {
                            String path = classesDir.getCanonicalPath();
                            classLoader.addPermission(path);
                        } catch (IOException e) {
                        }
                    }
                }

            }

        } catch (MalformedURLException e) {
        }

    }


    /**
     * Configure the repositories for our class loader, based on the
     * associated Context.
     */
    private void setRepositories() {

        if (!(container instanceof Context))
            return;
        ServletContext servletContext =
            ((Context) container).getServletContext();
        if (servletContext == null)
            return;

        // Loading the work directory
        File workDir =
            (File) servletContext.getAttribute(ServletContext.TEMPDIR);
        if (workDir == null) {
            if (log.isLoggable(Level.INFO)) {
                log.info("No work dir for " + servletContext);
            }
        }

        if (log.isLoggable(Level.FINEST) && workDir != null) 
            log.finest(sm.getString("webappLoader.deploy",
                                    workDir.getAbsolutePath()));

        DirContext resources = container.getResources();

        // Setting up the class repository (/WEB-INF/classes), if it exists

        String classesPath = "/WEB-INF/classes";
        DirContext classes = null;

        try {
            Object object = resources.lookup(classesPath);
            if (object instanceof DirContext) {
                classes = (DirContext) object;
            }
        } catch(NamingException e) {
            // Silent catch: it's valid that no /WEB-INF/classes collection
            // exists
        }

        if (classes != null) {

            File classRepository = null;

            String absoluteClassesPath =
                servletContext.getRealPath(classesPath);

            if (absoluteClassesPath != null) {

                classRepository = new File(absoluteClassesPath);

            } else {

                classRepository = new File(workDir, classesPath);
                classRepository.mkdirs();
                copyDir(classes, classRepository);

            }

            if (log.isLoggable(Level.FINEST))
                log.finest(sm.getString("webappLoader.classDeploy",
                                        classesPath,
                                        classRepository.getAbsolutePath()));
        }

        // Setting up the JAR repository (/WEB-INF/lib), if it exists

        String libPath = "/WEB-INF/lib";

        classLoader.setJarPath(libPath);

        DirContext libDir = null;
        // Looking up directory /WEB-INF/lib in the context
        try {
            Object object = resources.lookup(libPath);
            if (object instanceof DirContext)
                libDir = (DirContext) object;
        } catch(NamingException e) {
            // Silent catch: it's valid that no /WEB-INF/lib collection
            // exists
        }

        if (libDir != null) {

            boolean copyJars = false;
            String absoluteLibPath = servletContext.getRealPath(libPath);

            File destDir = null;

            if (absoluteLibPath != null) {
                destDir = new File(absoluteLibPath);
            } else {
                copyJars = true;
                destDir = new File(workDir, libPath);
                destDir.mkdirs();
            }

            if (!copyJars) {
                return;
            }

            // Looking up directory /WEB-INF/lib in the context
            try {
                NamingEnumeration<Binding> enumeration =
                    resources.listBindings(libPath);
                while (enumeration.hasMoreElements()) {

                    Binding binding = enumeration.nextElement();
                    String filename = libPath + "/" + binding.getName();
                    // START OF IASRI 4657979
                    if (!filename.endsWith(".jar") &&
                        !filename.endsWith(".zip"))
                    // END OF IASRI 4657979
                        continue;

                    // START PWC 1.1 6314481
                    if (binding.getName() != null
                            && binding.getName().startsWith(".")
                            && ignoreHiddenJarFiles) {
                        continue;
                    }
                    // END PWC 1.1 6314481

                    File destFile = new File(destDir, binding.getName());

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest(sm.getString("webappLoader.jarDeploy",
                                                filename,
                                                destFile.getAbsolutePath()));
                    }

                    Object obj = binding.getObject();

                    if (!(obj instanceof Resource))
                        continue;

                    Resource jarResource = (Resource) obj;

                    if (!copy(jarResource.streamContent(),
                              new FileOutputStream(destFile))) {
                        continue;
                    }
                }
            } catch (NamingException e) {
                // Silent catch: it's valid that no /WEB-INF/lib directory
                // exists
            } catch (IOException e) {
                log("Unable to configure repositories", e);
            }
        }
    }


    /**
     * Set the appropriate context attribute for our class path.  This
     * is required only because Jasper depends on it.
     */
    private void setClassPath() {

        // Validate our current state information
        if (!(container instanceof Context))
            return;
        ServletContext servletContext =
            ((Context) container).getServletContext();
        if (servletContext == null)
            return;

        if (container instanceof StandardContext) {
            String baseClasspath = 
                ((StandardContext) container).getCompilerClasspath();
            if (baseClasspath != null) {
                servletContext.setAttribute(Globals.CLASS_PATH_ATTR,
                                            baseClasspath);
                return;
            }
        }

        StringBuilder classpath = new StringBuilder();

        // Assemble the class path information from our class loader chain
        ClassLoader loader = getClassLoader();
        boolean first = true;
        while (loader != null) {
            if (!(loader instanceof URLClassLoader)) {
                String cp = getClasspath(loader);
                if (cp != null) {
                    if (!first) {
                        classpath.append(File.pathSeparator);
                    } else {
                        first = false;
                    }
                    classpath.append(cp);
                }
            } else {
                URL[] repositories = ((URLClassLoader) loader).getURLs();
                for (int i = 0; i < repositories.length; i++) {
                    if (repositories[i] == null) {
                        continue;
                    }
                    String repository = repositories[i].toString();
                    if (repository.startsWith("file://")) {
                        repository = repository.substring(7);
                    } else if (repository.startsWith("file:")) {
                        repository = repository.substring(5);
                    } else if (repository.startsWith("jndi:")) {
                        repository = servletContext.getRealPath(
                            repository.substring(5));
                    } else {
                        continue;
                    }
                    if (!repository.isEmpty()) {
                        if (!first) {
                            classpath.append(File.pathSeparator);
                        } else {
                            first = false;
                        }
                        classpath.append(repository);
                    }
                }
            }

            loader = loader.getParent();
        }

        this.classpath = classpath.toString();

        // Store the assembled class path as a servlet context attribute
        servletContext.setAttribute(Globals.CLASS_PATH_ATTR,
                                    classpath.toString());

    }

    // try to extract the classpath from a loader that is not URLClassLoader
    private String getClasspath( ClassLoader loader ) {
        try {
            Method m=loader.getClass().getMethod("getClasspath", new Class[] {});
            if (log.isLoggable(Level.FINEST))
                log.finest("getClasspath " + m );
            Object o=m.invoke( loader, new Object[] {} );
            if (log.isLoggable(Level.FINEST))
                log.finest("gotClasspath " + o);
            if (o instanceof String )
                return (String)o;
            return null;
        } catch (Exception ex) {
            if (log.isLoggable(Level.FINEST))
                log.log(Level.FINEST, "getClasspath ", ex);
        }
        return null;
    }

    /**
     * Copy directory.
     */
    private boolean copyDir(DirContext srcDir, File destDir) {

        try {

            NamingEnumeration<NameClassPair> enumeration = srcDir.list("");
            while (enumeration.hasMoreElements()) {
                NameClassPair ncPair = enumeration.nextElement();
                String name = ncPair.getName();
                Object object = srcDir.lookup(name);
                File currentFile = new File(destDir, name);
                if (object instanceof Resource) {
                    InputStream is = ((Resource) object).streamContent();
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy(is, os))
                        return false;
                } else if (object instanceof InputStream) {
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy((InputStream) object, os))
                        return false;
                } else if (object instanceof DirContext) {
                    currentFile.mkdir();
                    copyDir((DirContext) object, currentFile);
                }
            }

        } catch (NamingException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        return true;

    }


    /**
     * Copy a file to the specified temp directory. This is required only
     * because Jasper depends on it.
     */
    private boolean copy(InputStream is, OutputStream os) {

        try {
            byte[] buf = new byte[4096];
            while (true) {
                int len = is.read(buf);
                if (len < 0)
                    break;
                os.write(buf, 0, len);
            }
        } catch (IOException e) {
            return false;
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                // do nothing
            }
            try {
                os.close();
            } catch (Exception e) {
                // do nothing
            }
        }

        return true;

    }

    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name) throws Exception {
        oname=name;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
    }

    public ObjectName getController() {
        return controller;
    }

    public void setController(ObjectName controller) {
        this.controller = controller;
    }

    // START OF PE 4985680
    /**
     * Adds the given package name to the list of packages that may always be
     * overriden, regardless of whether they belong to a protected namespace
     */
    public void addOverridablePackage(String packageName){
       if ( overridablePackages == null){
           overridablePackages = new ArrayList<String>();
       }
        
       overridablePackages.add( packageName ); 
    }
    // END OF PE 4985680


    // START PWC 1.1 6314481
    public void setIgnoreHiddenJarFiles(boolean ignoreHiddenJarFiles) {
        this.ignoreHiddenJarFiles = ignoreHiddenJarFiles;
    }
    // END PWC 1.1 6314481

}
