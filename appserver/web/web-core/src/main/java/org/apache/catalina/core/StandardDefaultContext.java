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

import org.apache.catalina.*;
import org.apache.catalina.deploy.*;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.util.StringManager;
import org.apache.naming.ContextAccessController;
import org.apache.tomcat.util.modeler.ManagedBean;
import org.apache.tomcat.util.modeler.Registry;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.directory.DirContext;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Used to store the default configuration a Host will use
 * when creating a Context.  A Context configured in server.xml
 * can override these defaults by setting the Context attribute
 * <CODE>override="true"</CODE>.
 *
 * @author Glenn Nielsen
 * @version $Revision: 1.4 $ $Date: 2006/03/12 01:27:01 $
 */

public class StandardDefaultContext 
    implements DefaultContext, LifecycleListener, MBeanRegistration {


    // ----------------------------------------------------------- Constructors


    /**
     * Create the DefaultContext
     */
    public StandardDefaultContext() {

        namingResources.setContainer(this);

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Contexts we are currently associated with.
     */
    private Hashtable<StandardContext, StandardContext> contexts =
        new Hashtable<StandardContext, StandardContext>();


    /**
     * The list of application listener class names configured for this
     * application, in the order they were encountered in the web.xml file.
     */
    private List<String> applicationListeners = new ArrayList<String>();


    /**
     * The set of application parameters defined for this application.
     */
    private List<ApplicationParameter> applicationParameters =
        new ArrayList<ApplicationParameter>();


    /**
     * Should we attempt to use cookies for session id communication?
     */
    private boolean cookies = true;


    /**
     * Should we allow the <code>ServletContext.getContext()</code> method
     * to access the context of other web applications in this server?
     */
    private boolean crossContext = true;


    /**
     * The descriptive information string for this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.DefaultContext/1.0";


    /**
     * The list of classnames of InstanceListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private ArrayList<String> instanceListeners = new ArrayList<String>();


    /**
     * The associated naming resources.
     */
    private NamingResources namingResources = new NamingResources();


    /**
     * The context initialization parameters for this web application,
     * keyed by name.
     */
    private HashMap<String, String> parameters = new HashMap<String, String>();


    /**
     * The reloadable flag for this web application.
     */
    private boolean reloadable = false;


    /**
     * The list of classnames of LifecycleListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private ArrayList<String> wrapperLifecycles = new ArrayList<String>();


    /**
     * The list of classnames of ContainerListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private List<String> wrapperListeners = new ArrayList<String>();


    /**
     * JNDI use flag.
     */
    private boolean useNaming = true;


    /**
     * The resources DirContext object with which this Container is
     * associated.
     *
     */
    DirContext dirContext = null;


    /**
     * The human-readable name of this Container.
     */
    protected String name = "defaultContext";


    /**
     * The parent Container to which this Container is a child.
     */
    protected Container parent = null;


    /**
     * The Context LifecycleListener's
     */
    protected Vector<LifecycleListener> lifecycle = new Vector<LifecycleListener>();


    /**
     * The Loader implementation with which this Container is associated.
     */
    protected Loader loader = null;


    /**
     * The Manager implementation with which this Container is associated.
     */
    protected Manager manager = null;


    /**
     * Case sensitivity.
     */
    protected boolean caseSensitive = true;


    /**
     * Allow linking.
     */
    protected boolean allowLinking = false;


    /**
     * Cache max size in KB.
     */
    protected int cacheMaxSize = 10240; // 10 MB


    /**
     * Cache TTL in ms.
     */
    protected int cacheTTL = 5000;


    /**
     * CachingAllowed.
     */
    protected boolean cachingAllowed = true;


    /**
     * Frequency of manager checks.
     */
    protected int managerChecksFrequency = 6;


    /**
     * The string manager for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * The property change support for this component.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    // ----------------------------------------------------- Context Properties


    /**
     * Set case sensitivity.
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }


    /**
     * Is case sensitive ?
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }


    /**
     * Set allow linking.
     */
    public void setAllowLinking(boolean allowLinking) {
        this.allowLinking = allowLinking;
    }


    /**
     * Is linking allowed.
     */
    public boolean isAllowLinking() {
        return allowLinking;
    }


    /**
     * Set cache TTL.
     */
    public void setCacheTTL(int cacheTTL) {
        this.cacheTTL = cacheTTL;
    }


    /**
     * Get cache TTL.
     */
    public int getCacheTTL() {
        return cacheTTL;
    }


    /**
     * Return the maximum size of the cache in KB.
     */
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }


    /**
     * Set the maximum size of the cache in KB.
     */
    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }


    /**
     * Set cachingAllowed.
     */
    public void setCachingAllowed(boolean cachingAllowed) {
        this.cachingAllowed = cachingAllowed;
    }


    /**
     * Is cachingAllowed ?
     */
    public boolean isCachingAllowed() {
        return cachingAllowed;
    }


    /**
     * Set the manager checks frequency.
     */
    public void setManagerChecksFrequency(int managerChecksFrequency) {
        this.managerChecksFrequency = managerChecksFrequency;
    }


    /**
     * Get manager checks frquency.
     */
    public int getManagerChecksFrequency() {
        return managerChecksFrequency;
    }
 

    /**
     * Returns true if the internal naming support is used.
     */
    public boolean isUseNaming() {

        return (useNaming);

    }


    /**
     * Enables or disables naming.
     */
    public void setUseNaming(boolean useNaming) {
        this.useNaming = useNaming;
    }


    /**
     * Return the "use cookies for session ids" flag.
     */
    public boolean getCookies() {

        return (this.cookies);

    }


    /**
     * Set the "use cookies for session ids" flag.
     *
     * @param cookies The new flag
     */
    public void setCookies(boolean cookies) {
        this.cookies = cookies;

    }


    /**
     * Return the "allow crossing servlet contexts" flag.
     */
    public boolean getCrossContext() {

        return (this.crossContext);

    }


    /**
     * Set the "allow crossing servlet contexts" flag.
     *
     * @param crossContext The new cross contexts flag
     */
    public void setCrossContext(boolean crossContext) {
        this.crossContext = crossContext;

    }


    /**
     * Return descriptive information about this Container implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return (info);

    }


    /**
     * Return the reloadable flag for this web application.
     */
    public boolean getReloadable() {

        return (this.reloadable);

    }


    /**
     * Set the reloadable flag for this web application.
     *
     * @param reloadable The new reloadable flag
     */
    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;

    }


    /**
     * Set the resources DirContext object with which this Container is
     * associated.
     *
     * @param resources The newly associated DirContext
     */
    public void setResources(DirContext resources) {
        this.dirContext = resources;

    }

    /**
     * Get the resources DirContext object with which this Container is
     * associated.
     *
     * @return the resources DirContext object with which this Container is
     * associated
     */
    public DirContext getResources() {
        return this.dirContext;
    }


    /**
     * Return the Loader with which this Container is associated.  If there is
     * no associated Loader return <code>null</code>.
     */
    public Loader getLoader() {

        return loader;

    }


    /**
     * Set the Loader with which this Context is associated.
     *
     * @param loader The newly associated loader
     */
    public void setLoader(Loader loader) {
        Loader oldLoader = this.loader;
        this.loader = loader;

        // Report this property change to interested listeners
        support.firePropertyChange("loader", oldLoader, this.loader);
    }


    /**
     * Return the Manager with which this Container is associated.  If there is
     * no associated Manager return <code>null</code>.
     */
    public Manager getManager() {

        return manager;

    }


    /**
     * Set the Manager with which this Container is associated.
     *
     * @param manager The newly associated Manager
     */
    public void setManager(Manager manager) {
        Manager oldManager = this.manager;
        this.manager = manager;
        
        // Report this property change to interested listeners
        support.firePropertyChange("manager", oldManager, this.manager);
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.add(listener);
    }


    // ------------------------------------------------------ Public Properties

    /**
     * The name of this DefaultContext
     */

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
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

    // -------------------------------------------------------- Context Methods


    /**
     * Adds the Listener with the given class name that is declared in the
     * deployment descriptor to the set of Listeners configured for this
     * application.
     *
     * @param listener Java class name of a listener class
     */
    public void addApplicationListener(String listener) {
        applicationListeners.add(listener);
    }


    /**
     * Add a new application parameter for this application.
     *
     * @param parameter The new application parameter
     */
    public void addApplicationParameter(ApplicationParameter parameter) {
        String newName = parameter.getName();
        synchronized (applicationParameters) {
            Iterator<ApplicationParameter> i =
                applicationParameters.iterator(); 
            while (i.hasNext()) {
                ApplicationParameter applicationParameter = i.next();
                if (newName.equals(applicationParameter.getName())) {
                    if (applicationParameter.getOverride()) {
                        applicationParameter.setValue(parameter.getValue());
                    }
                    return;
                }
            }
            applicationParameters.add(parameter);
        }
    }


    /**
     * Add an EJB resource reference for this web application.
     *
     * @param ejb New EJB resource reference
     */
    public void addEjb(ContextEjb ejb) {
        namingResources.addEjb(ejb);
    }


    /**
     * Add an environment entry for this web application.
     *
     * @param environment New environment entry
     */
    public void addEnvironment(ContextEnvironment environment) {
        namingResources.addEnvironment(environment);
    }


    /**
     * Add resource parameters for this web application.
     *
     * @param resourceParameters New resource parameters
     */
    public void addResourceParams(ResourceParams resourceParameters) {

        namingResources.addResourceParams(resourceParameters);

    }


    /**
     * Add the classname of an InstanceListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of an InstanceListener class
     */
    public void addInstanceListener(String listener) {
        instanceListeners.add(listener);
    }


    /**
     * Add a new context initialization parameter, replacing any existing
     * value for the specified name.
     *
     * @param name Name of the new parameter
     * @param value Value of the new  parameter
     *
     * @exception IllegalArgumentException if the name or value is missing,
     *  or if this context initialization parameter has already been
     *  registered
     */
    public void addParameter(String name, String value) {
        // Validate the proposed context initialization parameter
        if ((name == null) || (value == null))
            throw new IllegalArgumentException
                (sm.getString("standardContext.parameter.required"));
        if (parameters.get(name) != null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.parameter.duplicate", name));

        // Add this parameter to our defined set
        synchronized (parameters) {
            parameters.put(name, value);
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
     * Add a resource reference for this web application.
     *
     * @param resource New resource reference
     */
    public void addResource(ContextResource resource) {

        namingResources.addResource(resource);

    }


    /**
     * Add a resource environment reference for this web application.
     *
     * @param name The resource environment reference name
     * @param type The resource environment reference type
     */
    public void addResourceEnvRef(String name, String type) {

        namingResources.addResourceEnvRef(name, type);

    }


    /**
     * Add a resource link for this web application.
     *
     * @param resourceLink New resource link
     */
    public void addResourceLink(ContextResourceLink resourceLink) {
        namingResources.addResourceLink(resourceLink);
    }


    /**
     * Add the classname of a LifecycleListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a LifecycleListener class
     */
    public void addWrapperLifecycle(String listener) {
        wrapperLifecycles.add(listener);
    }


    /**
     * Add the classname of a ContainerListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a ContainerListener class
     */
    public void addWrapperListener(String listener) {
        wrapperListeners.add(listener);
    }


    /**
     * Return the list of application listener class names configured
     * for this application.
     */
    public List<String> findApplicationListeners() {
        return applicationListeners;
    }


    /**
     * Return the set of application parameters for this application.
     */
    public List<ApplicationParameter> findApplicationParameters() {
        return applicationParameters;
    }


    /**
     * Return the EJB resource reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    public ContextEjb findEjb(String name) {

        return namingResources.findEjb(name);

    }


    /**
     * Return the defined EJB resource references for this application.
     * If there are none, a zero-length array is returned.
     */
    public ContextEjb[] findEjbs() {

        return namingResources.findEjbs();

    }


    /**
     * Return the environment entry with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired environment entry
     */
    public ContextEnvironment findEnvironment(String name) {

        return namingResources.findEnvironment(name);

    }


    /**
     * Return the set of defined environment entries for this web
     * application.  If none have been defined, a zero-length array
     * is returned.
     */
    public ContextEnvironment[] findEnvironments() {

        return namingResources.findEnvironments();

    }


    /**
     * Return the set of defined resource parameters for this web
     * application.  If none have been defined, a zero-length array
     * is returned.
     */
    public ResourceParams[] findResourceParams() {

        return namingResources.findResourceParams();

    }


    /**
     * Return the list of InstanceListener classes that will be added to
     * newly created Wrappers automatically.
     */
    public List<String> findInstanceListeners() {
        return instanceListeners;
    }


    /**
     * Return the value for the specified context initialization
     * parameter name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the parameter to return
     */
    public String findParameter(String name) {

        synchronized (parameters) {
            return parameters.get(name);
        }

    }


    /**
     * Return the names of all defined context initialization parameters
     * for this Context.  If no parameters are defined, a zero-length
     * array is returned.
     */
    public String[] findParameters() {

        synchronized (parameters) {
            String results[] = new String[parameters.size()];
            return parameters.keySet().toArray(results);
        }

    }


    /**
     * Return the resource reference with the specified name, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource reference
     */
    public ContextResource findResource(String name) {

        return namingResources.findResource(name);

    }


    /**
     * Return the resource environment reference type for the specified
     * name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource environment reference
     */
    public String findResourceEnvRef(String name) {

        return namingResources.findResourceEnvRef(name);

    }


    /**
     * Return the set of resource environment reference names for this
     * web application.  If none have been specified, a zero-length
     * array is returned.
     */
    public String[] findResourceEnvRefs() {

        return namingResources.findResourceEnvRefs();

    }


    /**
     * Return the resource link with the specified name, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource link
     */
    public ContextResourceLink findResourceLink(String name) {

        return namingResources.findResourceLink(name);

    }


    /**
     * Return the defined resource links for this application.  If
     * none have been defined, a zero-length array is returned.
     */
    public ContextResourceLink[] findResourceLinks() {

        return namingResources.findResourceLinks();

    }


    /**
     * Return the defined resource references for this application.  If
     * none have been defined, a zero-length array is returned.
     */
    public ContextResource[] findResources() {

        return namingResources.findResources();

    }


    /**
     * Return the list of LifecycleListener classes that will be added to
     * newly created Wrappers automatically.
     */
    public List<String> findWrapperLifecycles() {
        return wrapperLifecycles;
    }


    /**
     * Return the list of ContainerListener classes that will be added to
     * newly created Wrappers automatically.
     */
    public List<String> findWrapperListeners() {
        return wrapperListeners;
    }


    /**
     * Return the naming resources associated with this web application.
     */
    public NamingResources getNamingResources() {

        return (this.namingResources);

    }


    /**
     * Removes any application listeners from this default Context
     */
    public void removeApplicationListeners() {
        applicationListeners.clear();
    }


    /**
     * Remove the application parameter with the specified name from
     * the set for this application.
     *
     * @param name Name of the application parameter to remove
     */
    public void removeApplicationParameter(String name) {
        synchronized (applicationParameters) {
            ApplicationParameter match = null;
            Iterator<ApplicationParameter> i =
                applicationParameters.iterator(); 
            while (i.hasNext()) {
                ApplicationParameter applicationParameter = i.next();
                // Make sure this parameter is currently present
                if (name.equals(applicationParameter.getName())) {
                    match = applicationParameter;
                    break;
                }
            }
            if (match != null) {
                applicationParameters.remove(match);
            }
        }
    }


    /**
     * Remove any EJB resource reference with the specified name.
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeEjb(String name) {

        namingResources.removeEjb(name);

    }


    /**
     * Remove a class name from the list of InstanceListener classes that
     * will be added to newly created Wrappers.
     *
     * @param listener Class name of an InstanceListener class to be removed
     */
    public void removeInstanceListener(String listener) {
        instanceListeners.remove(listener);
    }


    /**
     * Remove the context initialization parameter with the specified
     * name, if it exists; otherwise, no action is taken.
     *
     * @param name Name of the parameter to remove
     */
    public void removeParameter(String name) {

        synchronized (parameters) {
            parameters.remove(name);
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
     * Remove any environment entry with the specified name.
     *
     * @param envName Name of the environment entry to remove
     */
    public void removeEnvironment(String envName) {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return;
        }
        ContextEnvironment env = nresources.findEnvironment(envName);
        if (env == null) {
            throw new IllegalArgumentException
                ("Invalid environment name '" + envName + "'");
        }
        nresources.removeEnvironment(envName);
    }


    /**
     * Remove any resource reference with the specified name.
     *
     * @param resourceName Name of the resource reference to remove
     */
    public void removeResource(String resourceName) {

        // That should be done in the UI
        // resourceName = URLDecoder.decode(resourceName);
        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return;
        }
        ContextResource resource = nresources.findResource(resourceName);
        if (resource == null) {
            throw new IllegalArgumentException
                ("Invalid resource name '" + resourceName + "'");
        }
        nresources.removeResource(resourceName);
    }


    /**
     * Remove any resource link with the specified name.
     *
     * @param resourceLinkName Name of the resource reference to remove
     */
    public void removeResourceLink(String resourceLinkName) {

        //resourceLinkName = URLDecoder.decode(resourceLinkName);
        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return;
        }
        ContextResourceLink resource = nresources.findResourceLink(resourceLinkName);
        if (resource == null) {
            throw new IllegalArgumentException
                ("Invalid resource name '" + resourceLinkName + "'");
        }
        nresources.removeResourceLink(resourceLinkName);
    }


    /**
     * Remove any resource environment reference with the specified name.
     *
     * @param name Name of the resource environment reference to remove
     */
    public void removeResourceEnvRef(String name) {
        namingResources.removeResourceEnvRef(name);
    }


    @Override
    public void removeWrapperLifecycles() {
        wrapperLifecycles.clear();
    }


    @Override
    public void removeWrapperListeners() {
        wrapperListeners.clear();
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Process the START event for an associated Context.
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        StandardContext context = null;
        NamingContextListener listener = null;

        if (event.getLifecycle() instanceof StandardContext) {
            context = (StandardContext) event.getLifecycle();
            Iterator<LifecycleListener> lifecycleIter =
                context.findLifecycleListeners().iterator();
            while (lifecycleIter.hasNext()) {
                LifecycleListener lifecycleListener = lifecycleIter.next();
                if (lifecycleListener instanceof NamingContextListener) {
                    listener = (NamingContextListener) lifecycleListener;
                    break;
                }
            }
        }

        if (listener == null) {
            return;
        }

        if ((event.getType().equals(Lifecycle.BEFORE_STOP_EVENT))
            || (event.getType().equals(Context.RELOAD_EVENT))) {

            // Remove context
            contexts.remove(context);

            // Remove listener from the NamingResource listener list
            namingResources.removePropertyChangeListener(listener);

            // Remove listener from lifecycle listeners
            if (!(event.getType().equals(Context.RELOAD_EVENT))) {
                context.removeLifecycleListener(this);
            }

        }

        if ((event.getType().equals(Lifecycle.AFTER_START_EVENT))
            || (event.getType().equals(Context.RELOAD_EVENT))) {

            // Add context
            contexts.put(context, context);

            NamingResources contextResources = context.getNamingResources();

            // Setting the context in read/write mode
            ContextAccessController.setWritable(listener.getName(), context);

            // Send notifications to the listener to add the appropriate 
            // resources
            ContextEjb [] contextEjb = findEjbs();
            for (int i = 0; i < contextEjb.length; i++) {
                ContextEjb contextEntry = contextEjb[i];
                if (contextResources.exists(contextEntry.getName())) {
                    listener.removeEjb(contextEntry.getName());
                }
                listener.addEjb(contextEntry);
            }
            ContextEnvironment [] contextEnv = findEnvironments();
            for (int i = 0; i < contextEnv.length; i++) {
                ContextEnvironment contextEntry = contextEnv[i];
                if (contextResources.exists(contextEntry.getName())) {
                    listener.removeEnvironment(contextEntry.getName());
                }
                listener.addEnvironment(contextEntry);
            }
            ContextResource [] resources = findResources();
            for (int i = 0; i < resources.length; i++) {
                ContextResource contextEntry = resources[i];
                if (contextResources.exists(contextEntry.getName())) {
                    listener.removeResource(contextEntry.getName());
                }
                listener.addResource(contextEntry);
            }
            ContextResourceLink [] resourceLinks = findResourceLinks();
            for (int i = 0; i < resourceLinks.length; i++) {
                ContextResourceLink contextEntry = resourceLinks[i];
                if (contextResources.exists(contextEntry.getName())) {
                    listener.removeResourceLink(contextEntry.getName());
                }
                listener.addResourceLink(contextEntry);
            }
            String [] envRefs = findResourceEnvRefs();
            for (int i = 0; i < envRefs.length; i++) {
                if (contextResources.exists(envRefs[i])) {
                    listener.removeResourceEnvRef(envRefs[i]);
                }
                listener.addResourceEnvRef
                    (envRefs[i], findResourceEnvRef(envRefs[i]));
            }

            // Setting the context in read only mode
            ContextAccessController.setReadOnly(listener.getName());

            // Add listener to the NamingResources listener list
            namingResources.addPropertyChangeListener(listener);

        }

    }


    /**
     * Install the StandardContext portion of the DefaultContext
     * configuration into current Context.
     *
     * @param context current web application context
     */
    public void installDefaultContext(Context context) {
  
        if (context instanceof StandardContext) {
            StandardContext stContext = (StandardContext)context;
            stContext.setUseNaming(isUseNaming());
            stContext.setCachingAllowed(isCachingAllowed());
            stContext.setCacheTTL(getCacheTTL());
            stContext.setCacheMaxSize(getCacheMaxSize());
            stContext.setAllowLinking(isAllowLinking());
            stContext.setCaseSensitive(isCaseSensitive());
            stContext.setManagerChecksFrequency
                (getManagerChecksFrequency());
            if (!contexts.containsKey(stContext)) {
                stContext.addLifecycleListener(this);
            }
            Enumeration<LifecycleListener> lifecycleListeners = lifecycle.elements();
            while (lifecycleListeners.hasMoreElements()) {
                stContext.addLifecycleListener(
                    lifecycleListeners.nextElement());
              }
        }

        if (!context.getPrivileged() && loader != null) {
            ClassLoader parentClassLoader = context.getParent().getParentClassLoader();
            Class<? extends Loader> clazz = loader.getClass();
            Class<?> types[] = { ClassLoader.class };
            Object args[] = { parentClassLoader };
            try {
                Constructor<? extends Loader> constructor = clazz.getDeclaredConstructor(types);
                Loader context_loader = constructor.newInstance(args);
                context_loader.setDelegate(loader.getDelegate());
                context_loader.setReloadable(loader.getReloadable());
                if (loader instanceof WebappLoader) {
                    ((WebappLoader)context_loader).setDebug
                        (((WebappLoader)loader).getDebug());
                    ((WebappLoader)context_loader).setLoaderClass
                        (((WebappLoader)loader).getLoaderClass());
                }
                context.setLoader(context_loader);
            } catch(Exception e) {
                IllegalArgumentException iae = new IllegalArgumentException
                   ("DefaultContext custom Loader install failed");
                iae.initCause(e);
                throw iae;
            }
        }
    }


    /**
     * Import the configuration from the DefaultContext into
     * current Context.
     *
     * @param context current web application context
     */
    public void importDefaultContext(Context context) {

        context.setCookies(getCookies());
        context.setCrossContext(getCrossContext());
        context.setReloadable(getReloadable());

        Iterator<String> iter = findApplicationListeners().iterator(); 
        while (iter.hasNext()) {
            context.addApplicationListener(iter.next());
        }
        iter = findInstanceListeners().iterator();
        while (iter.hasNext()) {
            context.addInstanceListener(iter.next());
        }
        iter = findWrapperListeners().iterator(); 
        while (iter.hasNext()) {
            context.addWrapperListener(iter.next());
        }
        iter = findWrapperLifecycles().iterator();
        while (iter.hasNext()) {
            context.addWrapperLifecycle(iter.next());
        }
        String[] parameters = findParameters();
        for( int i = 0; i < parameters.length; i++ ) {
            context.addParameter(parameters[i],findParameter(parameters[i]));
        }
        Iterator<ApplicationParameter> appParamIter =
            findApplicationParameters().iterator(); 
        while (appParamIter.hasNext()) {
            context.addApplicationParameter(appParamIter.next());
        }

        if (!(context instanceof StandardContext)) {
            ContextEjb [] contextEjb = findEjbs();
            for( int i = 0; i < contextEjb.length; i++ ) {
                context.addEjb(contextEjb[i]);
            }
            ContextEnvironment [] contextEnv = findEnvironments();
            for( int i = 0; i < contextEnv.length; i++ ) {
                context.addEnvironment(contextEnv[i]);
            }
            /*
            if (context instanceof StandardContext) {
                ResourceParams [] resourceParams = findResourceParams();
                for( int i = 0; i < resourceParams.length; i++ ) {
                    ((StandardContext)context).addResourceParams
                        (resourceParams[i]);
                }
            }
            */
            ContextResource [] resources = findResources();
            for( int i = 0; i < resources.length; i++ ) {
                context.addResource(resources[i]);
            }
            ContextResourceLink [] resourceLinks = findResourceLinks();
            for( int i = 0; i < resourceLinks.length; i++ ) {
                context.addResourceLink(resourceLinks[i]);
            }
            String [] envRefs = findResourceEnvRefs();
            for( int i = 0; i < envRefs.length; i++ ) {
                context.addResourceEnvRef
                    (envRefs[i],findResourceEnvRef(envRefs[i]));
            }
        }

    }

    /**
     * Return a String representation of this component.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder();
        if (getParent() != null) {
            sb.append(getParent().toString());
            sb.append(".");
        }
        sb.append("DefaultContext[");
        sb.append("]");
        return (sb.toString());

    }

    // -------------------- JMX stuff  --------------------
    protected String type;
    protected String domain;
    protected String suffix;
    protected ObjectName oname;
    protected MBeanServer mserver;

    public ObjectName getObjectName() {
        return oname;
    }

    public String getDomain() {
        return domain;
    }

    public String getType() {
        return type;
    }

    protected String getJSR77Suffix() {
        return suffix;
    }

    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name) throws Exception {
        oname=name;
        mserver=server;
        domain=name.getDomain();

        type=name.getKeyProperty("type");
        if( type==null ) {
            type=name.getKeyProperty("j2eeType");
        }

        String j2eeApp=name.getKeyProperty("J2EEApplication");
        String j2eeServer=name.getKeyProperty("J2EEServer");
        if( j2eeApp==null ) {
            j2eeApp="none";
        }
        if( j2eeServer==null ) {
            j2eeServer="none";
        }
        suffix=",J2EEApplication=" + j2eeApp + ",J2EEServer=" + j2eeServer;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
    }

    /**
     * Return the MBean Names of the set of defined environment entries for
     * this web application
     */
    public String[] getEnvironments() {
        ContextEnvironment[] envs = getNamingResources().findEnvironments();
        ArrayList<String> results = new ArrayList<String>();
        for (int i = 0; i < envs.length; i++) {
            try {
                ObjectName oname =
                    MBeanUtils.createObjectName(this.getDomain(), envs[i]);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                IllegalArgumentException iae = new IllegalArgumentException
                    ("Cannot create object name for environment " + envs[i]);
                iae.initCause(e);
                throw iae;
            }
        }
        return results.toArray(new String[results.size()]);

    }


    /**
     * Return the MBean Names of all the defined resource references for this
     * application.
     * XXX This changed - due to conflict
     */
    public String[] getResourceNames() {

        ContextResource[] resources = getNamingResources().findResources();
        ArrayList<String> results = new ArrayList<String>();
        for (int i = 0; i < resources.length; i++) {
            try {
                ObjectName oname =
                    MBeanUtils.createObjectName(getDomain(), resources[i]);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                IllegalArgumentException iae = new IllegalArgumentException
                    ("Cannot create object name for resource " + resources[i]);
                iae.initCause(e);
                throw iae;
            }
        }
        return results.toArray(new String[results.size()]);

    }


    /**
     * Return the MBean Names of all the defined resource links for this
     * application
     */
    public String[] getResourceLinks() {

        ContextResourceLink[] links = getNamingResources().findResourceLinks();
        ArrayList<String> results = new ArrayList<String>();
        for (int i = 0; i < links.length; i++) {
            try {
                ObjectName oname =
                    MBeanUtils.createObjectName(getDomain(), links[i]);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                IllegalArgumentException iae = new IllegalArgumentException
                    ("Cannot create object name for resource " + links[i]);
                iae.initCause(e);
                throw iae;
            }
        }
        return results.toArray(new String[results.size()]);

    }

    // ------------------------------------------------------------- Operations


    /**
     * Add an environment entry for this web application.
     *
     * @param envName New environment entry name
     */
    public String addEnvironment(String envName, String type)
        throws MalformedObjectNameException {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return null;
        }
        ContextEnvironment env = nresources.findEnvironment(envName);
        if (env != null) {
            throw new IllegalArgumentException
                ("Invalid environment name - already exists '" + envName + "'");
        }
        env = new ContextEnvironment();
        env.setName(envName);
        env.setType(type);
        nresources.addEnvironment(env);

        // Return the corresponding MBean name
        ManagedBean managed = Registry.getRegistry(null, null).findManagedBean("ContextEnvironment");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), env);
        return (oname.toString());

    }


    /**
     * Add a resource reference for this web application.
     *
     * @param resourceName New resource reference name
     */
    public String addResource(String resourceName, String type)
        throws MalformedObjectNameException {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return null;
        }
        ContextResource resource = nresources.findResource(resourceName);
        if (resource != null) {
            throw new IllegalArgumentException
                ("Invalid resource name - already exists'" + resourceName + "'");
        }
        resource = new ContextResource();
        resource.setName(resourceName);
        resource.setType(type);
        nresources.addResource(resource);

        // Return the corresponding MBean name
        ManagedBean managed = Registry.getRegistry(null, null).findManagedBean("ContextResource");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), resource);

        return (oname.toString());
    }


    /**
     * Add a resource link for this web application.
     *
     * @param resourceLinkName New resource link name
     */
    public String addResourceLink(String resourceLinkName, String global,
                String name, String type) throws MalformedObjectNameException {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return null;
        }
        ContextResourceLink resourceLink =
                                nresources.findResourceLink(resourceLinkName);
        if (resourceLink != null) {
            throw new IllegalArgumentException
                ("Invalid resource link name - already exists'" +
                                                        resourceLinkName + "'");
        }
        resourceLink = new ContextResourceLink();
        resourceLink.setGlobal(global);
        resourceLink.setName(resourceLinkName);
        resourceLink.setType(type);
        nresources.addResourceLink(resourceLink);

        // Return the corresponding MBean name
        ManagedBean managed = Registry.getRegistry(null, null).findManagedBean("ContextResourceLink");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), resourceLink);
        return (oname.toString());
    }
}
