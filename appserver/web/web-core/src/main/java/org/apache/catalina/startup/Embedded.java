/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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


import org.apache.catalina.core.*;
import org.glassfish.web.util.IntrospectionUtils;
import org.apache.catalina.*;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.net.ServerSocketFactory;
import org.apache.catalina.security.SecurityConfig;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.ServerInfo;
import org.glassfish.web.valve.GlassFishValve;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Convenience class to embed a Catalina servlet container environment
 * inside another application.  You must call the methods of this class in the
 * following order to ensure correct operation.
 *
 * <ul>
 * <li>Instantiate a new instance of this class.</li>
 * <li>Set the relevant properties of this object itself.  In particular,
 *     you will want to establish the default Logger to be used, as well
 *     as the default Realm if you are using container-managed security.</li>
 * <li>Call <code>createEngine()</code> to create an Engine object, and then
 *     call its property setters as desired.</li>
 * <li>Call <code>createHost()</code> to create at least one virtual Host
 *     associated with the newly created Engine, and then call its property
 *     setters as desired.  After you customize this Host, add it to the
 *     corresponding Engine with <code>engine.addChild(host)</code>.</li>
 * <li>Call <code>createContext()</code> to create at least one Context
 *     associated with each newly created Host, and then call its property
 *     setters as desired.  You <strong>SHOULD</strong> create a Context with
 *     a pathname equal to a zero-length string, which will be used to process
 *     all requests not mapped to some other Context.  After you customize
 *     this Context, add it to the corresponding Host with
 *     <code>host.addChild(context)</code>.</li>
 * <li>Call <code>addEngine()</code> to attach this Engine to the set of
 *     defined Engines for this object.</li>
 * <li>Call <code>createConnector()</code> to create at least one TCP/IP
 *     connector, and then call its property setters as desired.</li>
 * <li>Call <code>addConnector()</code> to attach this Connector to the set
 *     of defined Connectors for this object.  The added Connector will use
 *     the most recently added Engine to process its received requests.</li>
 * <li>Repeat the above series of steps as often as required (although there
 *     will typically be only one Engine instance created).</li>
 * <li>Call <code>start()</code> to initiate normal operations of all the
 *     attached components.</li>
 * </ul>
 *
 * After normal operations have begun, you can add and remove Connectors,
 * Engines, Hosts, and Contexts on the fly.  However, once you have removed
 * a particular component, it must be thrown away -- you can create a new one
 * with the same characteristics if you merely want to do a restart.
 * <p>
 * To initiate a normal shutdown, call the <code>stop()</code> method of
 * this object.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong>:  The <code>main()</code> method of
 * this class is a simple example that exercizes the features of dynamically
 * starting and stopping various components.  You can execute this by executing
 * the following steps (on a Unix platform):
 * <pre>
 *   cd $CATALINA_HOME
 *   ./bin/catalina.sh embedded
 * </pre>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.12 $ $Date: 2007/03/29 00:59:41 $
 */

public class Embedded  extends StandardService {

    protected static final Logger log = LogFacade.getLogger();
    protected static final ResourceBundle rb = log.getResourceBundle();

    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new instance of this class with default properties.
     */
    public Embedded() {

        this(null, null);

    }


    /**
     * Construct a new instance of this class with specified properties.
     *
     * @param logger Logger implementation to be inherited by all components
     *  (unless overridden further down the container hierarchy)
     * @param realm Realm implementation to be inherited by all components
     *  (unless overridden further down the container hierarchy)
     */
    public Embedded(org.apache.catalina.Logger logger, Realm realm) {

        super();
        setLogger(logger);
        setRealm(realm);
        setSecurityProtection();
        
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Is naming enabled ?
     */
    protected boolean useNaming = true;


    /**
     * The set of Engines that have been deployed in this server.  Normally
     * there will only be one.
     */
    protected Engine engines[] = new Engine[0];


    /**
     * Custom mappings of login methods to authenticators
     */
    protected HashMap<String, Authenticator> authenticators;


    /**
     * Descriptive information about this server implementation.
     */
    protected static final String info =
        "org.apache.catalina.startup.Embedded/1.0";


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The default logger to be used by this component itself.  Unless this
     * is overridden, log messages will be writted to standard output.
     */
    protected org.apache.catalina.Logger logger = null;


    /**
     * The default realm to be used by all containers associated with
     * this compoennt.
     */
    protected Realm realm = null;

    /**
     * The socket factory that will be used when a <code>secure</code>
     * Connector is created.  If a standard Connector is created, the
     * internal (to the Connector class default socket factory class)
     * will be used instead.
     */
    protected String socketFactory =
        "org.apache.catalina.net.SSLSocketFactory";


    /**
     * Has this component been started yet?
     */
    protected boolean started = false;

    /**
     * Use await.
     */
    protected boolean await = false;

    protected boolean embeddedDirectoryListing = false;

    // ------------------------------------------------------------- Properties


    /**
     * Return true if naming is enabled.
     */
    public boolean isUseNaming() {

        return (this.useNaming);

    }


    /**
     * Enables or disables naming support.
     *
     * @param useNaming The new use naming value
     */
    public void setUseNaming(boolean useNaming) {

        boolean oldUseNaming = this.useNaming;
        this.useNaming = useNaming;
        support.firePropertyChange("useNaming", Boolean.valueOf(oldUseNaming),
                                   Boolean.valueOf(this.useNaming));

    }


    /**
     * Return the Logger for this component.
     */
    public org.apache.catalina.Logger getLogger() {
        return (this.logger);
    }


    /**
     * Set the Logger for this component.
     *
     * @param logger The new logger
     */
    public void setLogger(org.apache.catalina.Logger logger) {
        org.apache.catalina.Logger oldLogger = this.logger;
        this.logger = logger;
        support.firePropertyChange("logger", oldLogger, this.logger);
    }


    /**
     * Return the default Realm for our Containers.
     */
    public Realm getRealm() {
        return (this.realm);
    }


    /**
     * Set the default Realm for our Containers.
     *
     * @param realm The new default realm
     */
    public void setRealm(Realm realm) {

        Realm oldRealm = this.realm;
        this.realm = realm;
        support.firePropertyChange("realm", oldRealm, this.realm);

    }


    /**
     * Return the secure socket factory class name.
     */
    public String getSocketFactory() {

        return (this.socketFactory);

    }


    /**
     * Set the secure socket factory class name.
     *
     * @param socketFactory The new secure socket factory class name
     */
    public void setSocketFactory(String socketFactory) {

        this.socketFactory = socketFactory;

    }

    public void setAwait(boolean b) {
        await = b;
    }

    public boolean isAwait() {
        return await;
    }

    public void setCatalinaHome( String s ) {
        System.setProperty( "catalina.home", s);
    }

    public void setCatalinaBase( String s ) {
        System.setProperty( "catalina.base", s);
    }

    public String getCatalinaHome() {
        return System.getProperty("catalina.home");
    }

    public String getCatalinaBase() {
        return System.getProperty("catalina.base");
    }

    public void setDirectoryListing(boolean listings) {
        embeddedDirectoryListing = listings;
    }

    public boolean isDirectoryListing() {
        return embeddedDirectoryListing;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Add a new Connector to the set of defined Connectors.  The newly
     * added Connector will be associated with the most recently added Engine.
     *
     * @param connector The connector to be added
     *
     * @exception IllegalStateException if no engines have been added yet
     */
    public synchronized void addConnector(Connector connector) {

        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Adding connector (" + connector.getInfo() + ")");
        }

        // Make sure we have a Container to send requests to
        if (engines.length < 1)
            throw new IllegalStateException
                (rb.getString(LogFacade.NO_ENGINES_DEFINED));

        /*
         * Add the connector. This will set the connector's container to the
         * most recently added Engine
         */
        super.addConnector(connector);
    }


    /**
     * Add a new Engine to the set of defined Engines.
     *
     * @param engine The engine to be added
     */
    public synchronized void addEngine(Engine engine) {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Adding engine (" + engine.getInfo() + ")");

        // Add this Engine to our set of defined Engines
        Engine results[] = new Engine[engines.length + 1];
        for (int i = 0; i < engines.length; i++)
            results[i] = engines[i];
        results[engines.length] = engine;
        engines = results;

        // Start this Engine if necessary
        if (started && (engine instanceof Lifecycle)) {
            try {
                ((Lifecycle) engine).start();
            } catch (LifecycleException e) {
                log.log(Level.SEVERE, LogFacade.ENGINE_START_EXCEPTION, e);
            }
        }

        this.container = engine;
    }


    public Engine[] getEngines() {
        return engines;
    }
    
    
    /**
     * Create, configure, and return a new TCP/IP socket connector
     * based on the specified properties.
     *
     * @param address InetAddress to bind to, or <code>null</code> if the
     * connector is supposed to bind to all addresses on this server
     * @param port Port number to listen to
     * @param secure true if the generated connector is supposed to be
     * SSL-enabled, and false otherwise
     */
    public Connector createConnector(InetAddress address, int port,
                                     boolean secure) {
	return createConnector(address != null? address.toString() : null,
			       port, secure);
    }

    public Connector createConnector(String address, int port,
                                     boolean secure) {
        String protocol = "http";
        if (secure) {
            protocol = "https";
        }

        return createConnector(address, port, protocol);
    }


    public Connector createConnector(InetAddress address, int port,
                                     String protocol) {
	return createConnector(address != null? address.toString() : null,
			       port, protocol);
    }

    public Connector createConnector(String address, int port,
				     String protocol) {

        Connector connector = null;

	if (address != null) {
	    /*
	     * InetAddress.toString() returns a string of the form
	     * "<hostname>/<literal_IP>". Get the latter part, so that the
	     * address can be parsed (back) into an InetAddress using
	     * InetAddress.getByName().
	     */
	    int index = address.indexOf('/');
	    if (index != -1) {
		address = address.substring(index + 1);
	    }
	}

	if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Creating connector for address='" +
                    ((address == null) ? "ALL" : address) +
                    "' port='" + port + "' protocol='" + protocol + "'");
	}

        try {

            Class clazz = 
                Class.forName("org.apache.catalina.connector.Connector");
            connector = (Connector) clazz.newInstance();

            if (address != null) {
                IntrospectionUtils.setProperty(connector, "address", 
                                               "" + address);
            }
            IntrospectionUtils.setProperty(connector, "port", "" + port);

            if (protocol.equals("ajp")) {
                IntrospectionUtils.setProperty
                    (connector, "protocolHandlerClassName",
                     "org.apache.jk.server.JkCoyoteHandler");
            } else if (protocol.equals("https")) {
                connector.setScheme("https");
                connector.setSecure(true);
                try {
                    Class serverSocketFactoryClass = Class.forName
                       ("org.apache.catalina.connector.CoyoteServerSocketFactory");
                    ServerSocketFactory factory = 
                        (ServerSocketFactory) 
                        serverSocketFactoryClass.newInstance();
                    connector.setFactory(factory);
                } catch (Exception e) {
                    log.log(Level.SEVERE, LogFacade.COULD_NOT_LOAD_SSL_SERVER_SOCKET_FACTORY_EXCEPTION);
                }
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, LogFacade.COULD_NOT_CREATE_CONNECTOR_EXCEPTION);
        }

        return (connector);

    }

    /**
     * Create, configure, and return a Context that will process all
     * HTTP requests received from one of the associated Connectors,
     * and directed to the specified context path on the virtual host
     * to which this Context is connected.
     * <p>
     * After you have customized the properties, listeners, and Valves
     * for this Context, you must attach it to the corresponding Host
     * by calling:
     * <pre>
     *   host.addChild(context);
     * </pre>
     * which will also cause the Context to be started if the Host has
     * already been started.
     *
     * @param path Context path of this application ("" for the default
     *  application for this host, must start with a slash otherwise)
     * @param docBase Absolute pathname to the document base directory
     *  for this web application
     *
     * @exception IllegalArgumentException if an invalid parameter
     *  is specified
     */
    public Context createContext(String path, String docBase) {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Creating context '" + path + "' with docBase '" +
                    docBase + "'");

        StandardContext context = new StandardContext();

        context.setDebug(debug);
        context.setDocBase(docBase);
        context.setPath(path);
        
        ContextConfig config = new ContextConfig();
        config.setCustomAuthenticators(authenticators);
        config.setDebug(debug);
        ((Lifecycle) context).addLifecycleListener(config);

        return (context);

    }


    /**
     * Create, configure, and return an Engine that will process all
     * HTTP requests received from one of the associated Connectors,
     * based on the specified properties.
     */
    public Engine createEngine() {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Creating engine");

        StandardEngine engine = new StandardEngine();

        engine.setDebug(debug);
        // Default host will be set to the first host added
        engine.setLogger(logger);       // Inherited by all children
        engine.setRealm(realm);         // Inherited by all children

        return (engine);

    }


    /**
     * Create, configure, and return a Host that will process all
     * HTTP requests received from one of the associated Connectors,
     * and directed to the specified virtual host.
     * <p>
     * After you have customized the properties, listeners, and Valves
     * for this Host, you must attach it to the corresponding Engine
     * by calling:
     * <pre>
     *   engine.addChild(host);
     * </pre>
     * which will also cause the Host to be started if the Engine has
     * already been started.  If this is the default (or only) Host you
     * will be defining, you may also tell the Engine to pass all requests
     * not assigned to another virtual host to this one:
     * <pre>
     *   engine.setDefaultHost(host.getName());
     * </pre>
     *
     * @param name Canonical name of this virtual host
     * @param appBase Absolute pathname to the application base directory
     *  for this virtual host
     *
     * @exception IllegalArgumentException if an invalid parameter
     *  is specified
     */
    public Host createHost(String name, String appBase) {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Creating host '" + name + "' with appBase '" +
                    appBase + "'");

        StandardHost host = new StandardHost();

        host.setAppBase(appBase);
        host.setDebug(debug);
        host.setName(name);

        return (host);

    }


    /**
     * Create and return a class loader manager that can be customized, and
     * then attached to a Context, before it is started.
     *
     * @param parent ClassLoader that will be the parent of the one
     *  created by this Loader
     */
    public Loader createLoader(ClassLoader parent) {

        if (log.isLoggable(Level.FINEST))
            log.log(Level.FINEST, "Creating Loader with parent class loader '" +
                    parent + "'");

        WebappLoader loader = new WebappLoader(parent);
        return (loader);

    }


    /**
     * Return descriptive information about this Server implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return (this.info);

    }


    /**
     * Remove the specified Context from the set of defined Contexts for its
     * associated Host.  If this is the last Context for this Host, the Host
     * will also be removed.
     *
     * @param context The Context to be removed
     */
    public synchronized void removeContext(Context context) {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Removing context[" + context.getPath() + "]");

        // Is this Context actually among those that are defined?
        boolean found = false;
        for (int i = 0; i < engines.length; i++) {
            Container hosts[] = engines[i].findChildren();
            for (int j = 0; j < hosts.length; j++) {
                Container contexts[] = hosts[j].findChildren();
                for (int k = 0; k < contexts.length; k++) {
                    if (context == (Context) contexts[k]) {
                        found = true;
                        break;
                    }
                }
                if (found)
                    break;
            }
            if (found)
                break;
        }
        if (!found)
            return;

        // Remove this Context from the associated Host
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, " Removing this Context");
        context.getParent().removeChild(context);

    }


    /**
     * Remove the specified Engine from the set of defined Engines, along with
     * all of its related Hosts and Contexts.  All associated Connectors are
     * also removed.
     *
     * @param engine The Engine to be removed
     */
    public synchronized void removeEngine(Engine engine) {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Removing engine (" + engine.getInfo() + ")");

        // Is the specified Engine actually defined?
        int j = -1;
        for (int i = 0; i < engines.length; i++) {
            if (engine == engines[i]) {
                j = i;
                break;
            }
        }
        if (j < 0)
            return;

        // Remove any Connector that is using this Engine
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, " Removing related Containers");
        while (true) {
            int n = -1;
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i].getContainer() == (Container) engine) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                break;
            // START SJSAS 6231069
            //removeConnector(connectors[n]);
            try{
                removeConnector(connectors[n]);
            } catch (Exception ex){
                log.log(Level.SEVERE, LogFacade.CONNECTOR_STOP_EXCEPTION, ex);
            }
            // END SJSAS 6231069
        }

        // Stop this Engine if necessary
        if (engine instanceof Lifecycle) {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, " Stopping this Engine");
            try {
                ((Lifecycle) engine).stop();
            } catch (LifecycleException e) {
                log.log(Level.SEVERE,LogFacade. ENGINE_STOP_EXCEPTION, e);
            }
        }

        // Remove this Engine from our set of defined Engines
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, " Removing this Engine");
        int k = 0;
        Engine results[] = new Engine[engines.length - 1];
        for (int i = 0; i < engines.length; i++) {
            if (i != j)
                results[k++] = engines[i];
        }
        engines = results;

    }


    /**
     * Remove the specified Host, along with all of its related Contexts,
     * from the set of defined Hosts for its associated Engine.  If this is
     * the last Host for this Engine, the Engine will also be removed.
     *
     * @param host The Host to be removed
     */
    public synchronized void removeHost(Host host) {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Removing host[" + host.getName() + "]");

        // Is this Host actually among those that are defined?
        boolean found = false;
        for (int i = 0; i < engines.length; i++) {
            Container hosts[] = engines[i].findChildren();
            for (int j = 0; j < hosts.length; j++) {
                if (host == (Host) hosts[j]) {
                    found = true;
                    break;

                }
            }
            if (found)
                break;
        }
        if (!found)
            return;

        // Remove this Host from the associated Engine
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, " Removing this Host");
        host.getParent().removeChild(host);

    }


    // START PWC 6392537
    /*
     * Maps the specified login method to the specified authenticator, allowing
     * the mappings in org/apache/catalina/startup/Authenticators.properties
     * to be overridden.
     *
     * <p>If <code>authenticator</code> is null, the associated login method
     * will be disabled.
     *
     * @param authenticator Authenticator to handle authentication for the
     * specified login method, or <code>null</code> if the specified login
     * method is to be disabled
     * @param loginMethod Login method that maps to the specified authenticator
     *
     * @throws IllegalArgumentException if the specified authenticator is not
     * null and does not implement the org.apache.catalina.Valve interface
     */
    public synchronized void addAuthenticator(Authenticator authenticator,
                                 String loginMethod) {
        if ((authenticator != null) && !(authenticator instanceof GlassFishValve)) {
            throw new IllegalArgumentException(rb.getString(LogFacade.AUTH_IS_NOT_VALVE_EXCEPTION));
        }
        if (authenticators == null) {
            authenticators = new HashMap<String, Authenticator>();
        }
        authenticators.put(loginMethod, authenticator);
    }
    // END PWC 6392537


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
     * with this Embedded instance.
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


    /**
     * Prepare for the beginning of active use of the public methods of this
     * component.  This method should be called after <code>configure()</code>,
     * and before any of the public methods of the component are utilized.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    public void start() throws LifecycleException {

        /* SJSAS 5022949
        if( log.isInfoEnabled() )
            log.info("Starting tomcat server");
         */
        // START SJSAS 6340446
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Starting Servlet container component of "
                    + ServerInfo.getServerInfo());
        }
        // END SJSAS 6340446

        // Validate the setup of our required system properties
        initDirs();

        // Initialize some naming specific properties
        initNaming();

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (rb.getString(LogFacade.SERVICE_BEEN_STARTED_EXCEPTION));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;
        initialized = true;

        // Start our defined Connectors first
        for (int i = 0; i < connectors.length; i++) {
            connectors[i].initialize();
            if (connectors[i] instanceof Lifecycle)
                ((Lifecycle) connectors[i]).start();
        }

        // Start our defined Engines second
        for (int i = 0; i < engines.length; i++) {
            if (engines[i] instanceof Lifecycle)
                ((Lifecycle) engines[i]).start();
        }
    }


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    @Override
    public void stop() throws LifecycleException {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Stopping embedded server");

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (rb.getString(LogFacade.SERVICE_NOT_BEEN_STARTED_EXCEPTION));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop our defined Connectors first
        for (int i = 0; i < connectors.length; i++) {
            if (connectors[i] instanceof Lifecycle)
                ((Lifecycle) connectors[i]).stop();
        }

        // Stop our defined Engines second
        for (int i = 0; i < engines.length; i++) {
            if (engines[i] instanceof Lifecycle)
                ((Lifecycle) engines[i]).stop();
        }

    }

    @Override
    public void destroy() throws LifecycleException {
        if( started ) stop();
        if (initialized) {
            initialized = false;
        }
    }



    // ------------------------------------------------------ Protected Methods


    /** Initialize naming - this should only enable java:env and root naming.
     * If tomcat is embeded in an application that already defines those -
     * it shouldn't do it.
     *
     * XXX The 2 should be separated, you may want to enable java: but not
     * the initial context and the reverse
     * XXX Can we "guess" - i.e. lookup java: and if something is returned assume
     * false ?
     * XXX We have a major problem with the current setting for java: url
     */
    protected void initNaming() {
        // Setting additional variables
        if (!useNaming) {
            // START SJSAS 5031700
            //log.info( "Catalina naming disabled");
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Catalina naming disabled");
            }
            // END SJSAS 5031700
            System.setProperty("catalina.useNaming", "false");
        } else {
            System.setProperty("catalina.useNaming", "true");
            String value = "org.apache.naming";
            String oldValue =
                System.getProperty(javax.naming.Context.URL_PKG_PREFIXES);
            if (oldValue != null) {
                value = value + ":" + oldValue;
            }
            System.setProperty(javax.naming.Context.URL_PKG_PREFIXES, value);
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "Setting naming prefix=" + value);
            value = System.getProperty
                (javax.naming.Context.INITIAL_CONTEXT_FACTORY);
            if (value == null) {
                System.setProperty
                    (javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                     "org.apache.naming.java.javaURLContextFactory");
            } else {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "INITIAL_CONTEXT_FACTORY alread set " + value);
                }
            }
        }
    }


    protected void initDirs() {

        String catalinaHome = System.getProperty("catalina.home");
        if (catalinaHome == null) {
            // Backwards compatibility patch for J2EE RI 1.3
            String j2eeHome = System.getProperty("com.sun.enterprise.home");
            if (j2eeHome != null) {
                catalinaHome=System.getProperty("com.sun.enterprise.home");
            } else if (System.getProperty("catalina.base") != null) {
                catalinaHome = System.getProperty("catalina.base");
            } else {
                // Use IntrospectionUtils and guess the dir
                catalinaHome = IntrospectionUtils.guessInstall
                    ("catalina.home", "catalina.base", "catalina.jar");
                if (catalinaHome == null) {
                    catalinaHome = IntrospectionUtils.guessInstall
                        ("tomcat.install", "catalina.home", "tomcat.jar");
                }
            }
        }
        // last resort - for minimal/embedded cases. 
        if(catalinaHome==null) {
            catalinaHome=System.getProperty("user.dir");
        }
        if (catalinaHome != null) {
            File home = new File(catalinaHome);
            if (!home.isAbsolute()) {
                try {
                    catalinaHome = home.getCanonicalPath();
                } catch (IOException e) {
                    catalinaHome = home.getAbsolutePath();
                }
            }
            System.setProperty("catalina.home", catalinaHome);
        }

        if (System.getProperty("catalina.base") == null) {
            System.setProperty("catalina.base",
                               catalinaHome);
        } else {
            String catalinaBase = System.getProperty("catalina.base");
            File base = new File(catalinaBase);
            if (!base.isAbsolute()) {
                try {
                    catalinaBase = base.getCanonicalPath();
                } catch (IOException e) {
                    catalinaBase = base.getAbsolutePath();
                }
            }
            System.setProperty("catalina.base", catalinaBase);
        } 

    }


    // -------------------------------------------------------- Private Methods

    /**
     * Customize the specified context to have its own log file instead of
     * inheriting the default one.  This is just an example of what you can
     * do; pretty much anything (such as installing special Valves) can
     * be done prior to calling <code>start()</code>.
     *
     * @param context Context to receive a specialized logger
     *
    private static void customize(Context context) {

        // Create a customized file logger for this context
        String basename = context.getPath();
        if (basename.length() < 1)
            basename = "ROOT";
        else
            basename = basename.substring(1);

        FileLogger special = new FileLogger();
        special.setPrefix(basename + "_log.");
        special.setSuffix(".txt");
        special.setTimestamp(true);

        // Override the default logger for this context
        context.setLogger(special);

    }
    */

    /**
     * Set the security package access/protection.
     */
    protected void setSecurityProtection(){
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    SecurityConfig securityConfig = SecurityConfig.newInstance();
                    securityConfig.setPackageDefinition();
                    securityConfig.setPackageAccess();
                    return null;
                }
            });
        }
    }

}
