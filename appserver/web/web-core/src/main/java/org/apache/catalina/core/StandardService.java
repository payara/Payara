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

package org.apache.catalina.core;


import org.apache.catalina.*;
import org.apache.catalina.util.LifecycleSupport;

import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Standard implementation of the <code>Service</code> interface.  The
 * associated Container is generally an instance of Engine, but this is
 * not required.
 *
 * @author Craig R. McClanahan
 */

public class StandardService
        implements Lifecycle, Service
 {

     private static final Logger log = LogFacade.getLogger();
     private static final ResourceBundle rb = log.getResourceBundle();

    // ----------------------------------------------------- Instance Variables


    /**
     * Descriptive information about this component implementation.
     */
    private static final String info =
        "org.apache.catalina.core.StandardService/1.0";


    /**
     * The name of this service.
     */
    private String name = null;


    /**
     * The lifecycle event support for this component.
     */
    private LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The <code>Server</code> that owns this Service, if any.
     */
    private Server server = null;

    /**
     * Has this component been started?
     */
    private boolean started = false;


    /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;


    /**
     * The property change support for this component.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    /**
     * The set of Connectors associated with this Service.
     */
    protected Connector connectors[] = new Connector[0];

    protected final Object connectorsMonitor = new Object();

    /**
     * The Container associated with this Service. (In the case of the
     * org.apache.catalina.startup.Embedded subclass, this holds the most
     * recently added Engine.)
     */
    protected Container container = null;


    /**
     * Has this component been initialized?
     */
    protected boolean initialized = false;

    /**
     * The broadcaster that sends j2ee notifications.
     */
    private NotificationBroadcasterSupport broadcaster = null;


    // ----------------------------------------------------------- Constructors

     /**
      * Construct a default instance of this class.
      */

     public StandardService() {
         this.broadcaster = new NotificationBroadcasterSupport();
     }


    // ------------------------------------------------------------- Properties


    /**
     * Return the <code>Container</code> that handles requests for all
     * <code>Connectors</code> associated with this Service.
     */
    public Container getContainer() {

        return (this.container);

    }


    /**
     * Set the <code>Container</code> that handles requests for all
     * <code>Connectors</code> associated with this Service.
     *
     * @param container The new Container
     */
    public void setContainer(Container container) {

        Container oldContainer = this.container;
        if ((oldContainer != null) && (oldContainer instanceof Engine))
            ((Engine) oldContainer).setService(null);
        this.container = container;
        if (this.container instanceof Engine)
            ((Engine) this.container).setService(this);
        if (started && (this.container != null) &&
            (this.container instanceof Lifecycle)) {
            try {
                ((Lifecycle) this.container).start();
            } catch (LifecycleException e) {
                // Ignore
            }
        }
        synchronized (connectorsMonitor) {
            for (int i = 0; i < connectors.length; i++)
                connectors[i].setContainer(this.container);
        }
        if (started && (oldContainer != null) &&
            (oldContainer instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldContainer).stop();
            } catch (LifecycleException e) {
                // Ignore
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("container", oldContainer, this.container);

    }

    public ObjectName getContainerName() {
        if( container instanceof ContainerBase ) {
            return ((ContainerBase)container).getJmxName();
        }
        return null;
    }


    /**
     * Return the debugging detail level of this component.
     */
    public int getDebug() {

        return (this.debug);

    }


    /**
     * Set the debugging detail level of this component.
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
     * Return descriptive information about this Service implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return (this.info);

    }


    /**
     * Return the name of this Service.
     */
    public String getName() {

        return (this.name);

    }


    /**
     * Set the name of this Service.
     *
     * @param name The new service name
     */
    public void setName(String name) {

        this.name = name;

    }


    /**
     * Return the <code>Server</code> with which we are associated (if any).
     */
    public Server getServer() {

        return (this.server);

    }


    /**
     * Set the <code>Server</code> with which we are associated (if any).
     *
     * @param server The server that owns this Service
     */
    public void setServer(Server server) {

        this.server = server;

    }

     /**
      * Return the <code>NotificationBroadcasterSupport</code> that sends notification for this Service.
      */
     public NotificationBroadcasterSupport getBroadcaster() {

         return broadcaster;

     }

     /**
      * Set the <code>NotificationBroadcasterSupport</code> that sends notification for this Service
      *
      * @param broadcaster The new NotificationBroadcasterSupport
      */

     public void setBroadcaster(NotificationBroadcasterSupport broadcaster) {

         this.broadcaster = broadcaster;

     }


    // --------------------------------------------------------- Public Methods


    /**
     * Add a new Connector to the set of defined Connectors, and associate it
     * with this Service's Container.
     *
     * @param connector The Connector to be added
     */
    public void addConnector(Connector connector) {

        synchronized (connectorsMonitor) {
            connector.setContainer(this.container);
            connector.setService(this);
            Connector results[] = new Connector[connectors.length + 1];
            System.arraycopy(connectors, 0, results, 0, connectors.length);
            results[connectors.length] = connector;
            connectors = results;

            if (initialized) {
                try {
                    connector.initialize();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, "Connector.initialize", e);
                }
            }

            if (started && (connector instanceof Lifecycle)) {
                try {
                    ((Lifecycle) connector).start();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, "Connector.start", e);
                }
            }

            // Report this property change to interested listeners
            support.firePropertyChange("connector", null, connector);
        }

    }

    public ObjectName[] getConnectorNames() {
        ObjectName results[] = new ObjectName[connectors.length];
        for( int i=0; i<results.length; i++ ) {
            // if it's a coyote connector
            //if( connectors[i] instanceof CoyoteConnector ) {
            //    results[i]=((CoyoteConnector)connectors[i]).getJmxName();
            //}
        }
        return results;
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
     * Find and return the set of Connectors associated with this Service.
     */
    public Connector[] findConnectors() {

        return (connectors);

    }


    /**
     * Find and return the Connector associated with this Service and Connector name.
     */
    public Connector findConnector(String name) {

        for (Connector connector : connectors) {
            if (connector.getName().equals(name)) {
                return connector;
            }
        }
        return null;

    }


    /**
     * Remove the specified Connector from the set associated from this
     * Service.  The removed Connector will also be disassociated from our
     * Container.
     *
     * @param connector The Connector to be removed
     */
    // START SJSAS 6231069
    //public void removeConnector(Connector connector) {
    public void removeConnector(Connector connector) throws LifecycleException{
    // END SJSAS 6231069

        synchronized (connectorsMonitor) {
            int j = -1;
            for (int i = 0; i < connectors.length; i++) {
                if (connector == connectors[i]) {
                    j = i;
                    break;
                }
            }
           
            if (j < 0)
                return;

            // START SJSAS 6231069
            /*if (started && (connectors[j] instanceof Lifecycle)) {
                try {
                   ((Lifecycle) connectors[j]).stop();
               } catch (LifecycleException e) {
                   log.error("Connector.stop", e);
               }
            }*/
            ((Lifecycle) connectors[j]).stop();
            // END SJSAS 6231069

            // START SJSAS 6231069
            /*connectors[j].setContainer(null);
            connector.setService(null);*/
            // END SJSAS 6231069           
            int k = 0;
            Connector results[] = new Connector[connectors.length - 1];
            for (int i = 0; i < connectors.length; i++) {
                if (i != j)
                    results[k++] = connectors[i];
            }
            connectors = results;

            // Report this property change to interested listeners
            support.firePropertyChange("connector", connector, null);
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

        StringBuilder sb = new StringBuilder("StandardService[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a LifecycleEvent listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * Gets the (possibly empty) list of lifecycle listeners
     * associated with this StandardService.
     */
    public List<LifecycleListener> findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * Remove a LifecycleEvent listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * Prepare for the beginning of active use of the public methods of this
     * component.  This method should be called before any of the public
     * methods of this component are utilized.  It should also send a
     * LifecycleEvent of type START_EVENT to any registered listeners.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, LogFacade.SERVICE_STARTED);
            }
        }
        
        if( ! initialized )
            init(); 

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.STARTING_SERVICE, this.name);
        }
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Start our defined Container first
        if (container != null) {
            synchronized (container) {
                if (container instanceof Lifecycle) {
                    ((Lifecycle) container).start();
                }
            }
        }

        // Start our defined Connectors second
        synchronized (connectorsMonitor) {
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i] instanceof Lifecycle)
                    ((Lifecycle) connectors[i]).start();
            }
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);

    }


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.  It should also send a LifecycleEvent
     * of type STOP_EVENT to any registered listeners.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            return;
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        lifecycle.fireLifecycleEvent(STOP_EVENT, null);

        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.STOPPING_SERVICE, this.name);
        }
        started = false;

        // Stop our defined Connectors first
        synchronized (connectorsMonitor) {
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i] instanceof Lifecycle)
                    ((Lifecycle) connectors[i]).stop();
            }
        }

        // Stop our defined Container second
        if (container != null) {
            synchronized (container) {
                if (container instanceof Lifecycle) {
                    ((Lifecycle) container).stop();
                }
            }
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);

    }


    /**
     * Invoke a pre-startup initialization. This is used to allow connectors
     * to bind to restricted ports under Unix operating environments.
     */
    public void initialize()
            throws LifecycleException
    {
        // Service shouldn't be used with embedded, so it doesn't matter
        if (initialized) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, LogFacade.SERVICE_HAS_BEEN_INIT);
            }
            return;
        }
        initialized = true;

        if( oname==null ) {
            try {
                // Hack - Server should be deprecated...
                Container engine=this.getContainer();
                domain=engine.getName();
                oname=new ObjectName(domain + ":type=Service,serviceName="+name);
            } catch (Exception e) {
                String msg = MessageFormat.format(rb.getString(LogFacade.ERROR_REGISTER_SERVICE_EXCEPTION), domain);
                log.log(Level.SEVERE, msg, e);
            }
            
            
        }
        if( server==null ) {
            // Register with the server 
            // HACK: ServerFactory should be removed...
            
            ServerFactory.getServer().addService(this);
        }
               

        // Initialize our defined Connectors
        synchronized (connectorsMonitor) {
                for (int i = 0; i < connectors.length; i++) {
                    connectors[i].initialize();
                }
        }
    }
    
    public void destroy() throws LifecycleException {
        if( started ) stop();
        // unregister should be here probably
        // START CR 6368091
        if (initialized) {
            initialized = false;
        }
        // END CR 6368091
    }

    public void init() {
        try {
            initialize();
        } catch( Throwable t ) {
            String msg = MessageFormat.format(rb.getString(LogFacade.FAILED_SERVICE_INIT_EXCEPTION), domain);
            log.log(Level.SEVERE, msg, t);
        }
    }

    protected String domain;
    protected ObjectName oname;

    public ObjectName getObjectName() {
        return oname;
    }

    public String getDomain() {
        return domain;
    }

}
