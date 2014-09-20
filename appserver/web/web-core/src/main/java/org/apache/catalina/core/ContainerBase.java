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

package org.apache.catalina.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.naming.resources.ProxyDirContext;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.valve.GlassFishValve;


/**
 * Abstract implementation of the <b>Container</b> interface, providing common
 * functionality required by nearly every implementation.  Classes extending
 * this base class must implement <code>getInfo()</code>, and may implement
 * a replacement for <code>invoke()</code>.
 * <p>
 * All subclasses of this abstract base class will include support for a
 * Pipeline object that defines the processing to be performed for each request
 * received by the <code>invoke()</code> method of this class, utilizing the
 * "Chain of Responsibility" design pattern.  A subclass should encapsulate its
 * own processing functionality as a <code>Valve</code>, and configure this
 * Valve into the pipeline by calling <code>setBasic()</code>.
 * <p>
 * This implementation fires property change events, per the JavaBeans design
 * pattern, for changes in singleton properties.  In addition, it fires the
 * following <code>ContainerEvent</code> events to listeners who register
 * themselves with <code>addContainerListener()</code>:
 * <table border=1>
 *   <tr>
 *     <th>Type</th>
 *     <th>Data</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td align=center><code>addChild</code></td>
 *     <td align=center><code>Container</code></td>
 *     <td>Child container added to this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>addValve</code></td>
 *     <td align=center><code>Valve</code></td>
 *     <td>Valve added to this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>removeChild</code></td>
 *     <td align=center><code>Container</code></td>
 *     <td>Child container removed from this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>removeValve</code></td>
 *     <td align=center><code>Valve</code></td>
 *     <td>Valve removed from this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>start</code></td>
 *     <td align=center><code>null</code></td>
 *     <td>Container was started.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>stop</code></td>
 *     <td align=center><code>null</code></td>
 *     <td>Container was stopped.</td>
 *   </tr>
 * </table>
 * Subclasses that fire additional events should document them in the
 * class comments of the implementation class.
 *
 * @author Craig R. McClanahan
 */

public abstract class ContainerBase
    implements Container, Lifecycle, Pipeline {

    protected static final Logger log = StandardServer.log;
    protected static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
        message = "ContainerBase.setLoader: stop: ",
        level = "SEVERE",
        cause = "Could not stop previous loader",
        action = "Verify previous loader"
    )
    public static final String CONTAINER_BASE_SET_LOADER_STOP = "AS-WEB-CORE-00099";

    @LogMessageInfo(
        message = "ContainerBase.setLoader: start:",
        level = "SEVERE",
        cause = "Could not start new loader",
        action = "Verify the configuration of container"
    )
    public static final String CONTAINER_BASE_SET_LOADER_START = "AS-WEB-CORE-00100";

    @LogMessageInfo(
        message = "ContainerBase.setLogger: stop: ",
        level = "SEVERE",
        cause = "Could not stop previous logger",
        action = "Verify previous logger"
    )
    public static final String CONTAINER_BASE_SET_LOGGER_STOP = "AS-WEB-CORE-00101";

    @LogMessageInfo(
        message = "ContainerBase.setLogger: start: ",
        level = "SEVERE",
        cause = "Could not start new logger",
        action = "Verify the configuration of container"
    )
    public static final String CONTAINER_BASE_SET_LOGGER_START = "AS-WEB-CORE-00102";

    @LogMessageInfo(
        message = "ContainerBase.setManager: stop: ",
        level = "SEVERE",
        cause = "Could not stop previous manager",
        action = "Verify previous manager"
    )
    public static final String CONTAINER_BASE_SET_MANAGER_STOP = "AS-WEB-CORE-00103";

    @LogMessageInfo(
        message = "ContainerBase.setManager: start: ",
        level = "SEVERE",
        cause = "Could not start new manager",
        action = "Verify the configuration of container"
    )
    public static final String CONTAINER_BASE_SET_MANAGER_START = "AS-WEB-CORE-00104";

    @LogMessageInfo(
        message = "ContainerBase.setRealm: stop: ",
        level = "SEVERE",
        cause = "Could not stop previous realm",
        action = "Verify previous realm"
    )
    public static final String CONTAINER_BASE_SET_REALM_STOP = "AS-WEB-CORE-00105";

    @LogMessageInfo(
        message = "ContainerBase.setRealm: start: ",
        level = "SEVERE",
        cause = "Could not start new realm",
        action = "Verify the configuration of container"
    )
    public static final String CONTAINER_BASE_SET_REALM_START = "AS-WEB-CORE-00106";

    @LogMessageInfo(
        message = "addChild: Child name {0} is not unique",
        level = "WARNING"
    )
    public static final String DUPLICATE_CHILD_NAME_EXCEPTION = "AS-WEB-CORE-00107";

    @LogMessageInfo(
        message = "ContainerBase.addChild: start: ",
        level = "SEVERE",
        cause = "Could not start new child container",
        action = "Verify the configuration of parent container"
    )
    public static final String CONTAINER_BASE_ADD_CHILD_START = "AS-WEB-CORE-00108";

    @LogMessageInfo(
        message = "ContainerBase.removeChild: stop: ",
        level = "SEVERE",
        cause = "Could not stop existing child container",
        action = "Verify existing child container"
    )
    public static final String CONTAINER_BASE_REMOVE_CHILD_STOP = "AS-WEB-CORE-00109";

    @LogMessageInfo(
        message = "Container {0} has already been started",
        level = "INFO"
    )
    public static final String CONTAINER_STARTED = "AS-WEB-CORE-00110";

    @LogMessageInfo(
        message = "Container {0} has not been started",
        level = "SEVERE",
        cause = "Current container has not been started",
        action = "Verify the current container"
    )
    public static final String CONTAINER_NOT_STARTED_EXCEPTION = "AS-WEB-CORE-00111";

    @LogMessageInfo(
        message = "Error stopping container {0}",
        level = "SEVERE",
        cause = "Could not stop child container",
        action = "Verify the existence of current child container"
    )
    public static final String ERROR_STOPPING_CONTAINER = "AS-WEB-CORE-00112";

    @LogMessageInfo(
        message = "Error unregistering ",
        level = "SEVERE",
        cause = "Could not unregister current container",
        action = "Verify if the container has been registered"
    )
    public static final String ERROR_UNREGISTERING = "AS-WEB-CORE-00113";

    @LogMessageInfo(
        message = "Exception invoking periodic operation: ",
        level = "SEVERE",
        cause = "Could not set the context ClassLoader",
        action = "Verify the security permission"
    )
    public static final String EXCEPTION_INVOKES_PERIODIC_OP = "AS-WEB-CORE-00114";



    /**
     * Perform addChild with the permissions of this class.
     * addChild can be called with the XML parser on the stack,
     * this allows the XML parser to have fewer privileges than
     * Tomcat.
     */
    protected class PrivilegedAddChild
        implements PrivilegedAction<Void> {

        private Container child;

        PrivilegedAddChild(Container child) {
            this.child = child;
        }

        public Void run() {
            addChildInternal(child);
            return null;
        }

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The child Containers belonging to this Container, keyed by name.
     */
    protected Map<String, Container> children = new LinkedHashMap<String, Container>();


    /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;


    /**
     * The processor delay for this component.
     */
    protected int backgroundProcessorDelay = -1;


    /**
     * Flag indicating whether a check to see if the request is secure is
     * required before adding Pragma and Cache-Control headers when proxy 
     * caching has been disabled
     */
    protected boolean checkIfRequestIsSecure = false;


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The container event listeners for this Container.
     */
    protected ArrayList<ContainerListener> listeners =
        new ArrayList<ContainerListener>();
    private ContainerListener[] listenersArray = new ContainerListener[0];


    /**
     * The Loader implementation with which this Container is associated.
     */
    protected Loader loader = null;

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    protected Lock readLock = lock.readLock();
    protected Lock writeLock = lock.writeLock();

    /**
     * The Logger implementation with which this Container is associated.
     */
    protected org.apache.catalina.Logger logger = null;

    /**
     * The Manager implementation with which this Container is associated.
     */
    protected Manager manager = null;

    /**
     * The human-readable name of this Container.
     */
    protected String name = null;


    /**
     * The parent Container to which this Container is a child.
     */
    protected Container parent = null;


    /**
     * The parent class loader to be configured when we install a Loader.
     */
    protected ClassLoader parentClassLoader = null;


    /**
     * The Pipeline object with which this Container is associated.
     */
    protected Pipeline pipeline = new StandardPipeline(this);


    protected boolean hasCustomPipeline = false;


    /**
     * The Realm with which this Container is associated.
     */
    protected Realm realm = null;

    /**
     * The resources DirContext object with which this Container is associated.
     */
    protected DirContext resources = null;

    /**
     * Has this component been started?
     */
    protected volatile boolean started = false;

    protected boolean initialized=false;

    /**
     * The property change support for this component.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    /**
     * The background thread.
     */
    private Thread thread = null;


    /**
     * The background thread completion semaphore.
     */
    private volatile boolean threadDone = false;


    /**
     * Indicates whether ContainerListener instances need to be notified
     * of a particular configuration event.
     */
    protected boolean notifyContainerListeners = true;


    // ------------------------------------------------------------- Properties

    /**
     * @return true if ContainerListener instances need to be notified
     * of a particular configuration event, and false otherwise
     */
    boolean isNotifyContainerListeners() {
        return notifyContainerListeners;
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
     * Get the delay between the invocation of the backgroundProcess method on
     * this container and its children. Child containers will not be invoked
     * if their delay value is not negative (which would mean they are using 
     * their own thread). Setting this to a positive value will cause 
     * a thread to be spawn. After waiting the specified amount of time, 
     * the thread will invoke the executePeriodic method on this container 
     * and all its children.
     */
    public int getBackgroundProcessorDelay() {
        return backgroundProcessorDelay;
    }


    /**
     * Set the delay between the invocation of the execute method on this
     * container and its children.
     * 
     * @param delay The delay in seconds between the invocation of 
     *              backgroundProcess methods
     */
    public void setBackgroundProcessorDelay(int delay) {
        backgroundProcessorDelay = delay;
    }


    /**
     * Return descriptive information about this Container implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return this.getClass().getName();
    }


    /**
     * Return the Loader with which this Container is associated.  If there is
     * no associated Loader, return the Loader associated with our parent
     * Container (if any); otherwise, return <code>null</code>.
     */
    public Loader getLoader() {

        try {
            readLock.lock();
            if (loader != null)
                return (loader);
        } finally {
            readLock.unlock();
        }

        if (parent != null)
            return (parent.getLoader());

        return (null);
    }


    /**
     * Set the Loader with which this Container is associated.
     *
     * @param loader The newly associated loader
     */
    public void setLoader(Loader loader) {

        Loader oldLoader;

        try {
	    writeLock.lock();

            // Change components if necessary
            oldLoader = this.loader;
            if (oldLoader == loader)
                return;
            this.loader = loader;

            // Stop the old component if necessary
            if (started && (oldLoader != null) &&
                    (oldLoader instanceof Lifecycle)) {
                try {
                    ((Lifecycle) oldLoader).stop();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, CONTAINER_BASE_SET_LOADER_STOP, e);
                }
            }

            // Start the new component if necessary
            if (loader != null)
                loader.setContainer(this);
            if (started && (loader != null) &&
                (loader instanceof Lifecycle)) {
                try {
                    ((Lifecycle) loader).start();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, CONTAINER_BASE_SET_LOADER_START, e);
                }
            }
        } finally {
	    writeLock.unlock();
        }

        // Report this property change to interested listeners
        support.firePropertyChange("loader", oldLoader, this.loader);

    }


    /**
     * Return the Logger with which this Container is associated.  If there is
     * no associated Logger, return the Logger associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public org.apache.catalina.Logger getLogger() {

        try {
            readLock.lock();
            if (logger != null)
                return (logger);
        } finally {
            readLock.unlock();
        }

        if (parent != null)
            return (parent.getLogger());

        return (null);
    }


    /**
     * Set the Logger with which this Container is associated.
     *
     * @param logger The newly associated Logger
     */
    public void setLogger(org.apache.catalina.Logger logger) {

        org.apache.catalina.Logger oldLogger;

        try {
            writeLock.lock();
            // Change components if necessary
            oldLogger = this.logger;
            if (oldLogger == logger)
                return;
            this.logger = logger;

            // Stop the old component if necessary
            if (started && (oldLogger != null) &&
                    (oldLogger instanceof Lifecycle)) {
                try {
                    ((Lifecycle) oldLogger).stop();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, CONTAINER_BASE_SET_LOGGER_STOP, e);
                }
            }

        
            // Start the new component if necessary
            if (logger != null)
                logger.setContainer(this);
            if (started && (logger != null) &&
                (logger instanceof Lifecycle)) {
                try {
                    ((Lifecycle) logger).start();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, CONTAINER_BASE_SET_LOGGER_START, e);
                }
            }
        } finally {
            writeLock.unlock();
        }

        // Report this property change to interested listeners
        support.firePropertyChange("logger", oldLogger, this.logger);

    }


    /**
     * Return the Manager with which this Container is associated.  If there is
     * no associated Manager, return the Manager associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Manager getManager() {

        try {
            readLock.lock();
            if (manager != null)
                return (manager);
        } finally {
            readLock.unlock();
        }

        if (parent != null)
            return (parent.getManager());

        return (null);
    }


    /**
     * Set the Manager with which this Container is associated.
     *
     * @param manager The newly associated Manager
     */
    public void setManager(Manager manager) {

        Manager oldManager;

        try {
            writeLock.lock();
            // Change components if necessary
            oldManager = this.manager;
            if (oldManager == manager)
                return;
            this.manager = manager;

            // Stop the old component if necessary
            if (started && (oldManager != null) &&
                    (oldManager instanceof Lifecycle)) {
                try {
                    ((Lifecycle) oldManager).stop();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, CONTAINER_BASE_SET_MANAGER_STOP, e);
                }
            }

            // Start the new component if necessary
            if (manager != null)
                manager.setContainer(this);
            if (started && (manager != null) &&
                    (manager instanceof Lifecycle)) {
                try {
                    ((Lifecycle) manager).start();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, CONTAINER_BASE_SET_MANAGER_START, e);
                }
            }
        } finally {
            writeLock.unlock();
        }

        // Report this property change to interested listeners
        support.firePropertyChange("manager", oldManager, this.manager);
    }


    /**
     * Return an object which may be utilized for mapping to this component.
     */
    public Object getMappingObject() {
        return this;
    }


    /**
     * Return a name string (suitable for use by humans) that describes this
     * Container.  Within the set of child containers belonging to a particular
     * parent, Container names must be unique.
     */
    public String getName() {

        return (name);
    }


    /**
     * Set a name string (suitable for use by humans) that describes this
     * Container.  Within the set of child containers belonging to a particular
     * parent, Container names must be unique.
     *
     * @param name New name of this container
     *
     * @exception IllegalStateException if this Container has already been
     *  added to the children of a parent Container (after which the name
     *  may not be changed)
     */
    public void setName(String name) {

        String oldName = this.name;
        this.name = name;
        support.firePropertyChange("name", oldName, this.name);
    }


    /**
     * Return the Container for which this Container is a child, if there is
     * one.  If there is no defined parent, return <code>null</code>.
     */
    public Container getParent() {

        return (parent);
    }


    /**
     * Set the parent Container to which this Container is being added as a
     * child.  This Container may refuse to become attached to the specified
     * Container by throwing an exception.
     *
     * @param container Container to which this Container is being added
     *  as a child
     *
     * @exception IllegalArgumentException if this Container refuses to become
     *  attached to the specified Container
     */
    public void setParent(Container container) {

        Container oldParent = this.parent;
        this.parent = container;
        support.firePropertyChange("parent", oldParent, this.parent);
    }


    /**
     * Return the parent class loader (if any) for this web application.
     * This call is meaningful only <strong>after</strong> a Loader has
     * been configured.
     */
    public ClassLoader getParentClassLoader() {
        if (parentClassLoader != null)
            return (parentClassLoader);
        if (parent != null) {
            return (parent.getParentClassLoader());
        }
        return (ClassLoader.getSystemClassLoader());
    }


    /**
     * Set the parent class loader (if any) for this web application.
     * This call is meaningful only <strong>before</strong> a Loader has
     * been configured, and the specified value (if non-null) should be
     * passed as an argument to the class loader constructor.
     *
     *
     * @param parent The new parent class loader
     */
    public void setParentClassLoader(ClassLoader parent) {
        ClassLoader oldParentClassLoader = this.parentClassLoader;
        this.parentClassLoader = parent;
        support.firePropertyChange("parentClassLoader", oldParentClassLoader,
                                   this.parentClassLoader);
    }


    /**
     * Return the Pipeline object that manages the Valves associated with
     * this Container.
     */
    public Pipeline getPipeline() {
        return (this.pipeline);
    }


    /**
     * @return true if this container was configured with a custom pipeline,
     * false otherwise
     */
    public boolean hasCustomPipeline() {
        return hasCustomPipeline;
    }


    /**
     * Indicates whether the request will be checked to see if it is secure
     * before adding Pragma and Cache-control headers when proxy caching has
     * been disabled.
     *
     * @return true if the check is required; false otherwise.
     */
    public boolean isCheckIfRequestIsSecure() {
        return checkIfRequestIsSecure;
    }


    /**
     * Sets the checkIfRequestIsSecure property of this Container.
     *
     * Setting this property to true will check if the request is secure
     * before adding Pragma and Cache-Control headers when proxy caching has
     * been disabled.
     *
     * @param checkIfRequestIsSecure true if check is required, false
     * otherwise
     */
    public void setCheckIfRequestIsSecure(boolean checkIfRequestIsSecure) {
        this.checkIfRequestIsSecure = checkIfRequestIsSecure;
    }


    /**
     * Return the Realm with which this Container is associated.  If there is
     * no associated Realm, return the Realm associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Realm getRealm() {
        try {
            readLock.lock();
            if (realm != null)
                return (realm);
        } finally {
            readLock.unlock();
        }

        if (parent != null)
            return (parent.getRealm());

        return (null);
    }


    /**
     * Set the Realm with which this Container is associated.
     *
     * @param realm The newly associated Realm
     */
    public void setRealm(Realm realm) {

        Realm oldRealm;

        try {
            writeLock.lock();
            // Change components if necessary
            oldRealm = this.realm;
            if (oldRealm == realm)
                return;
            this.realm = realm;

            // Stop the old component if necessary
            if (started && (oldRealm != null) &&
                    (oldRealm instanceof Lifecycle)) {
                try {
                    ((Lifecycle) oldRealm).stop();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, CONTAINER_BASE_SET_REALM_STOP, e);
                }
            }

            // Start the new component if necessary
            if (realm != null)
                realm.setContainer(this);
            if (started && (realm != null) &&
                    (realm instanceof Lifecycle)) {
                try {
                    ((Lifecycle) realm).start();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, CONTAINER_BASE_SET_REALM_START, e);
                }
            }
        } finally {
            writeLock.unlock();
        }

        // Report this property change to interested listeners
        support.firePropertyChange("realm", oldRealm, this.realm);
    }


    /**
      * Return the resources DirContext object with which this Container is
      * associated.  If there is no associated resources object, return the
      * resources associated with our parent Container (if any); otherwise
      * return <code>null</code>.
     */
    public DirContext getResources() {

        try {
            readLock.lock();
            if (resources != null)
                return (resources);
        } finally {
            readLock.unlock();
        }

        if (parent != null)
            return (parent.getResources());

        return (null);
    }


    /**
     * Set the resources DirContext object with which this Container is
     * associated.
     *
     * @param resources The newly associated DirContext
     */
    public void setResources(DirContext resources) throws Exception {
        // Called from StandardContext.setResources()
        //              <- StandardContext.start() 
        //              <- ContainerBase.addChildInternal() 

        // Change components if necessary
        DirContext oldResources;

        try {
            writeLock.lock();
            oldResources = this.resources;
            if (oldResources == resources)
                return;
            Hashtable<String, String> env = new Hashtable<String, String>();
            if (getParent() != null)
                env.put(ProxyDirContext.HOST, getParent().getName());
            env.put(ProxyDirContext.CONTEXT, getName());
            this.resources = new ProxyDirContext(env, resources);
            // Report this property change to interested listeners
        } finally {
            writeLock.unlock();
        }

        support.firePropertyChange("resources", oldResources,
                                   this.resources);
    }


    // ------------------------------------------------------ Container Methods


    /**
     * Add a new child Container to those associated with this Container,
     * if supported.  Prior to adding this Container to the set of children,
     * the child's <code>setParent()</code> method must be called, with this
     * Container as an argument.  This method may thrown an
     * <code>IllegalArgumentException</code> if this Container chooses not
     * to be attached to the specified Container, in which case it is not added
     *
     * @param child New child Container to be added
     *
     * @exception IllegalArgumentException if this exception is thrown by
     *  the <code>setParent()</code> method of the child Container
     * @exception IllegalArgumentException if the new child does not have
     *  a name unique from that of existing children of this Container
     * @exception IllegalStateException if this Container does not support
     *  child Containers
     */
    public void addChild(Container child) {
        if (Globals.IS_SECURITY_ENABLED) {
            PrivilegedAction<Void> dp =
                new PrivilegedAddChild(child);
            AccessController.doPrivileged(dp);
        } else {
            addChildInternal(child);
        }
    }

    private void addChildInternal(Container child) {
        
        if(log.isLoggable(Level.FINEST))
            log.log(Level.FINEST, "Add child " + child + " " + this);
        synchronized(children) {
            if (children.get(child.getName()) != null) {
                String msg = MessageFormat.format(rb.getString(DUPLICATE_CHILD_NAME_EXCEPTION),
                                                               child.getName());
            throw new IllegalArgumentException(msg);
            }
            child.setParent(this);  // May throw IAE
            if (started && (child instanceof Lifecycle)) {
                try {
                    ((Lifecycle) child).start();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, CONTAINER_BASE_ADD_CHILD_START, e);
                    throw new IllegalStateException
                            (rb.getString(CONTAINER_BASE_ADD_CHILD_START) + e);
                }
            }
            children.put(child.getName(), child);

            if (notifyContainerListeners) {
                fireContainerEvent(ADD_CHILD_EVENT, child);
            }
        }

    }


    /**
     * Add a container event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addContainerListener(ContainerListener listener) {

        synchronized (listeners) {
            listeners.add(listener);
            listenersArray = listeners.toArray(
                new ContainerListener[listeners.size()]);
        }

    }


    /**
     * Add a property change listener to this component.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {

        support.addPropertyChangeListener(listener);

    }


    /**
     * Return the child Container, associated with this Container, with
     * the specified name (if any); otherwise, return <code>null</code>
     *
     * @param name Name of the child Container to be retrieved
     */
    public Container findChild(String name) {

        if (name == null)
            return (null);
        synchronized (children) {       // Required by post-start changes
            return children.get(name);
        }

    }


    /**
     * Return the set of children Containers associated with this Container.
     * If this Container has no children, a zero-length array is returned.
     */
    public Container[] findChildren() {

        synchronized (children) {
            return children.values().toArray(new Container[children.size()]);
        }

    }


    /**
     * Return the set of container listeners associated with this Container.
     * If this Container has no registered container listeners, a zero-length
     * array is returned.
     */
    public ContainerListener[] findContainerListeners() {

        synchronized (listeners) {
            return listenersArray;
        }
    }


    /**
     * Process the specified Request, to produce the corresponding Response,
     * by invoking the first Valve in our pipeline (if any), or the basic
     * Valve otherwise.
     *
     * @param request Request to be processed
     * @param response Response to be produced
     *
     * @exception IllegalStateException if neither a pipeline or a basic
     *  Valve have been configured for this Container
     * @exception IOException if an input/output error occurred while
     *  processing
     * @exception ServletException if a ServletException was thrown
     *  while processing this request
     */
    public void invoke(Request request, Response response)
        throws IOException, ServletException {

        pipeline.invoke(request, response);
    }


    /**
     * Remove an existing child Container from association with this parent
     * Container.
     *
     * @param child Existing child Container to be removed
     */
    public void removeChild(Container child) {
        if (child == null) {
            return;
        }

        synchronized(children) {
            if (children.get(child.getName()) == null)
                return;
            children.remove(child.getName());
        }

        if (started && (child instanceof Lifecycle)) {
            try {
                if( child instanceof ContainerBase ) {
                    if( ((ContainerBase)child).started ) {
                        ((Lifecycle) child).stop();
                    }
                } else {
                    ((Lifecycle) child).stop();
                }
            } catch (LifecycleException e) {
                log.log(Level.SEVERE, CONTAINER_BASE_REMOVE_CHILD_STOP, e);
            }
        }
        
        if (notifyContainerListeners) {
            fireContainerEvent(REMOVE_CHILD_EVENT, child);
        }
    
        // child.setParent(null);
    }


    /**
     * Remove a container event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeContainerListener(ContainerListener listener) {

        synchronized (listeners) {
            listeners.remove(listener);
            listenersArray = listeners.toArray(
                new ContainerListener[listeners.size()]);
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
     * with this Container.
     */
    public List<LifecycleListener> findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * Removes the given lifecycle event listener from this Container.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * Removes any lifecycle event listeners from this Container.
     */
    public void removeLifecycleListeners() {
        lifecycle.removeLifecycleListeners();
    }


    /**
     * Prepare for active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public synchronized void start() throws LifecycleException {

        // Validate and update our current component state
        if (started) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, CONTAINER_STARTED, logName());
            }
            return;
        }
        
        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        started = true;

        // Start our subordinate components, if any
        if ((loader != null) && (loader instanceof Lifecycle))
            ((Lifecycle) loader).start();
        if ((logger != null) && (logger instanceof Lifecycle))
            ((Lifecycle) logger).start();
        if ((manager != null) && (manager instanceof Lifecycle))
            ((Lifecycle) manager).start();
        if ((realm != null) && (realm instanceof Lifecycle))
            ((Lifecycle) realm).start();
        if ((resources != null) && (resources instanceof Lifecycle))
            ((Lifecycle) resources).start();

        // Start our child containers, if any
        startChildren();

        // Start the Valves in our pipeline (including the basic), if any
        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).start();
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(START_EVENT, null);

        // Start our thread
        threadStart();

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
    }


    /**
     * Gracefully shut down active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public synchronized void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, CONTAINER_NOT_STARTED_EXCEPTION, logName());
            }
            return;
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // Stop our thread
        threadStop();

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the Valves in our pipeline (including the basic), if any
        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).stop();
        }

        // Stop our child containers, if any
        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Lifecycle) {
                try {
                    ((Lifecycle) children[i]).stop();
                } catch (Throwable t) {
                    String msg = MessageFormat.format(rb.getString(ERROR_STOPPING_CONTAINER), children[i]);
                    log.log(Level.SEVERE, msg, t);
                }
            }
        }

        // Remove children - so next start can work
        children = findChildren();
        for (int i = 0; i < children.length; i++) {
            removeChild(children[i]);
        }

        // Stop our subordinate components, if any
        if ((resources != null) && (resources instanceof Lifecycle)) {
            ((Lifecycle) resources).stop();
        }
        if ((realm != null) && (realm instanceof Lifecycle)) {
            ((Lifecycle) realm).stop();
        }
        if ((manager != null) && (manager instanceof Lifecycle)) {
            ((Lifecycle) manager).stop();
        }
        if ((logger != null) && (logger instanceof Lifecycle)) {
            ((Lifecycle) logger).stop();
        }
        if ((loader != null) && (loader instanceof Lifecycle)) {
            ((Lifecycle) loader).stop();
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
    }


    /** Init method, part of the MBean lifecycle.
     *  If the container was added via JMX, it'll register itself with the 
     * parent, using the ObjectName conventions to locate the parent.
     * 
     *  If the container was added directly and it doesn't have an ObjectName,
     * it'll create a name and register itself with the JMX console. On destroy(), 
     * the object will unregister.
     * 
     * @throws Exception
     */
    public void init() throws Exception {
        initialized=true;
    }
    
    public ObjectName getParentName() throws MalformedObjectNameException {
        return null;
    }
    
    public void destroy() throws Exception {
        if( started ) {
            stop();
        }
        initialized=false;

        // unregister this component
        if( oname != null ) {
            try {
                if( controller == oname ) {
                    if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "unregistering " + oname);
                    }
                }
            } catch( Throwable t ) {
                log.log(Level.SEVERE, ERROR_UNREGISTERING, t);
            }
        }

        if (parent != null) {
            parent.removeChild(this);
        }

        // Stop our child containers, if any
        Container children[] = findChildren();
        for(Container aChildren : children) {
            removeChild(aChildren);
        }

        // START SJSAS 6330332
        // Remove LifecycleListeners
        removeLifecycleListeners();
        // Release realm
        setRealm(null);
        // END SJSAS 6330332                
    }

    // ------------------------------------------------------- Pipeline Methods


    /**
     * Add a new Valve to the end of the pipeline associated with this
     * Container.  Prior to adding the Valve, the Valve's
     * <code>setContainer</code> method must be called, with this Container
     * as an argument.  The method may throw an
     * <code>IllegalArgumentException</code> if this Valve chooses not to
     * be associated with this Container, or <code>IllegalStateException</code>
     * if it is already associated with a different Container.
     *
     * @param valve Valve to be added
     *
     * @exception IllegalArgumentException if this Container refused to
     *  accept the specified Valve
     * @exception IllegalArgumentException if the specified Valve refuses to be
     *  associated with this Container
     * @exception IllegalStateException if the specified Valve is already
     *  associated with a different Container
     */
    public synchronized void addValve(GlassFishValve valve) {

        pipeline.addValve(valve);

        if (notifyContainerListeners) {
            fireContainerEvent(ADD_VALVE_EVENT, valve);
        }
    }


    /**
     * Add Tomcat-style valve.
     */
    public synchronized void addValve(Valve valve) {

        pipeline.addValve(valve);

        if (notifyContainerListeners) {
            fireContainerEvent(ADD_VALVE_EVENT, valve);
        }
    }


    public ObjectName[] getValveObjectNames() {
        return ((StandardPipeline)pipeline).getValveObjectNames();
    }
    
    /**
     * <p>Return the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).
     */
    public GlassFishValve getBasic() {
        return (pipeline.getBasic());
    }


    /**
     * Return the set of Valves in the pipeline associated with this
     * Container, including the basic Valve (if any).  If there are no
     * such Valves, a zero-length array is returned.
     */
    public GlassFishValve[] getValves() {
        return (pipeline.getValves());
    }


    /**
     * @return true if this pipeline has any non basic valves, false
     * otherwise
     */
    public boolean hasNonBasicValves() {
        return pipeline.hasNonBasicValves();
    }


    /**
     * Remove the specified Valve from the pipeline associated with this
     * Container, if it is found; otherwise, do nothing.
     *
     * @param valve Valve to be removed
     */
    public synchronized void removeValve(GlassFishValve valve) {

        pipeline.removeValve(valve);
 
        if (notifyContainerListeners) {
            fireContainerEvent(REMOVE_VALVE_EVENT, valve);
        }
    }


    /**
     * <p>Set the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).  Prior to setting the basic Valve,
     * the Valve's <code>setContainer()</code> will be called, if it
     * implements <code>Contained</code>, with the owning Container as an
     * argument.  The method may throw an <code>IllegalArgumentException</code>
     * if this Valve chooses not to be associated with this Container, or
     * <code>IllegalStateException</code> if it is already associated with
     * a different Container.</p>
     *
     * @param valve Valve to be distinguished as the basic Valve
     */
    public void setBasic(GlassFishValve valve) {

        pipeline.setBasic(valve);

    }


    /**
     * Execute a periodic task, such as reloading, etc. This method will be
     * invoked inside the classloading context of this container. Unexpected
     * throwables will be caught and logged.
     */
    public void backgroundProcess() {
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Notify all container event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireContainerEvent(String type, Object data) {

        ContainerListener[] list = null;

        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            list = listenersArray;
        }

        ContainerEvent event = new ContainerEvent(this, type, data);
        for (int i = 0; i < list.length; i++) {
            list[i].containerEvent(event);
        }
    }


    /**   
     * Starts the children of this container.
     */
    protected void startChildren() {

        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Lifecycle) {
                try {
                    ((Lifecycle) children[i]).start();
                } catch (Throwable t) {
                    String msg = MessageFormat.format(rb.getString(CONTAINER_NOT_STARTED_EXCEPTION), children[i]);
                    log.log(Level.SEVERE, msg, t);
                    if (children[i] instanceof Context) {
                        ((Context) children[i]).setAvailable(false);
                    } else if (children[i] instanceof Wrapper) {
                        ((Wrapper) children[i]).setAvailable(Long.MAX_VALUE);
                    }
                }
            }
        }
    }


    /**
     * Log the specified message to our current Logger (if any).
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

//         Logger logger = getLogger();
//         if (logger != null)
//             logger.log(logName() + ": " + message);
//         else
            log.log(Level.INFO, message);
    }


    /**
     * Log the specified message and exception to our current Logger
     * (if any).
     *
     * @param message Message to be logged
     * @param throwable Related exception
     */
    protected void log(String message, Throwable throwable) {

        org.apache.catalina.Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message, throwable);
        else {
            log.log(Level.SEVERE, message, throwable);
        }

    }


    /**
     * Return the abbreviated name of this container for logging messages
     */
    protected String logName() {

        String className = this.getClass().getName();
        int period = className.lastIndexOf(".");
        if (period >= 0)
            className = className.substring(period + 1);
        return (className + "[" + getName() + "]");

    }

    // -------------------- JMX and Registration  --------------------
    protected String domain;
    protected ObjectName oname;
    protected ObjectName controller;

    public ObjectName getJmxName() {
        synchronized (this) {
            return oname;
        }
    }
    
    public String getObjectName() {
        if (oname != null) {
            return oname.toString();
        } else return null;
    }

    public String getDomain() {
        if( domain==null ) {
            Container parent=this;
            while( parent != null &&
                    !( parent instanceof StandardEngine) ) {
                parent=parent.getParent();
            }
            if( parent != null ) {
                // parent will always be an instanceof StandardEngine unless it is null
                domain=((StandardEngine)parent).getDomain();
            } 
        }
        return domain;
    }

    public void setDomain(String domain) {
        this.domain=domain;
    }


    public ObjectName[] getChildren() {
        synchronized(children) {
            ObjectName result[]=new ObjectName[children.size()];
            Iterator<Container> it=children.values().iterator();
            int i=0;
            while( it.hasNext() ) {
                Object next=it.next();
                if( next instanceof ContainerBase ) {
                    result[i++]=((ContainerBase)next).getJmxName();
                }
            }
            return result;
        }
    }

    public ObjectName createObjectName(String domain, ObjectName parent)
        throws Exception
    {
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Create ObjectName " + domain + " " + parent);
        return null;
    }

    public String getContainerSuffix() {
        Container container=this;
        Container context=null;
        Container host=null;
        Container servlet=null;
        
        StringBuilder suffix=new StringBuilder();
        
        if( container instanceof StandardHost ) {
            host=container;
        } else if( container instanceof StandardContext ) {
            host=container.getParent();
            context=container;
        } else if( container instanceof StandardWrapper ) {
            context=container.getParent();
            host=context.getParent();
            servlet=container;
        }
        if( context!=null ) {
            String path=((StandardContext)context).getEncodedPath();
            suffix.append(",path=").append((path.equals("")) ? "/" : path);
        } 
        if( host!=null ) suffix.append(",host=").append( host.getName() );
        if (servlet != null) {
            String containerName = container.getName();
            suffix.append(",servlet=");
            suffix.append("".equals(containerName) ? "/" : containerName);
        }
        return suffix.toString();
    }


    /**
     * Start the background thread that will periodically check for
     * session timeouts.
     */
    protected void threadStart() {

        if (thread != null)
            return;
        if (backgroundProcessorDelay <= 0)
            return;

        threadDone = false;
        String threadName = "ContainerBackgroundProcessor[" + toString() + "]";
        thread = new Thread(new ContainerBackgroundProcessor(), threadName);
        thread.setDaemon(true);
        thread.start();

    }


    /**
     * Stop the background thread that is periodically checking for
     * session timeouts.
     */
    protected void threadStop() {

        if (thread == null)
            return;

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            // Ignore
        }

        thread = null;

    }


    // -------------------------------------- ContainerExecuteDelay Inner Class


    /**
     * Private thread class to invoke the backgroundProcess method 
     * of this container and its children after a fixed delay.
     */
    protected class ContainerBackgroundProcessor implements Runnable {

        public void run() {
            while (!threadDone) {
                try {
                    Thread.sleep(backgroundProcessorDelay * 1000L);
                } catch (InterruptedException e) {
                    // Ignore
                }
                if (!threadDone) {
                    Container parent = (Container) getMappingObject();
                    ClassLoader cl = 
                        Thread.currentThread().getContextClassLoader();
                    if (parent.getLoader() != null) {
                        cl = parent.getLoader().getClassLoader();
                    }
                    processChildren(parent, cl);
                }
            }
        }

        protected void processChildren(Container container, ClassLoader cl) {
            try {
                if (container.getLoader() != null) {
                    Thread.currentThread().setContextClassLoader
                        (container.getLoader().getClassLoader());
                }
                container.backgroundProcess();
            } catch (Throwable t) {
                log.log(Level.SEVERE, EXCEPTION_INVOKES_PERIODIC_OP, t);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
            Container[] children = container.findChildren();
            for (int i = 0; i < children.length; i++) {
                if (children[i].getBackgroundProcessorDelay() <= 0) {
                    processChildren(children[i], cl);
                }
            }
        }

    }

}
