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

package org.apache.catalina.valves;

import org.apache.catalina.*;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.util.LifecycleSupport;
import org.glassfish.web.valve.GlassFishValve;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Convenience base class for implementations of the <b>Valve</b> interface.
 * A subclass <strong>MUST</strong> implement an <code>invoke()</code>
 * method to provide the required functionality, and <strong>MAY</strong>
 * implement the <code>Lifecycle</code> interface to provide configuration
 * management and lifecycle support.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.7 $ $Date: 2007/05/05 05:32:41 $
 */

public abstract class ValveBase
/** CR 6411114
    implements Contained, Valve {
*/
// START CR 6411114
    implements Contained, Lifecycle, Valve, GlassFishValve {
// END CR 6411114
    protected static final Logger log = LogFacade.getLogger();
    protected static final ResourceBundle rb = log.getResourceBundle();

    //------------------------------------------------------ Instance Variables


    /**
     * The Container whose pipeline this Valve is a component of.
     */
    protected Container container = null;


    /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;


    // START CR 6411114
    /**
     * Has this component been started yet?
     */
    protected boolean started = false;


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    // END CR 6411114
    /**
     * Descriptive information about this Valve implementation.  This value
     * should be overridden by subclasses.
     */
    protected static final String info =
        "org.apache.catalina.core.ValveBase/1.0";


    /**
     * The next Valve in the pipeline this Valve is a component of.
     */
    protected Valve next = null;



    //-------------------------------------------------------------- Properties

    /**
     * Return the Container with which this Valve is associated, if any.
     */
    public Container getContainer() {

        return (container);
    }


    /**
     * Set the Container with which this Valve is associated, if any.
     *
     * @param container The new associated container
     */
    public void setContainer(Container container) {

        this.container = container;
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

        this.debug = debug;
    }


    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo() {

        return (info);
    }


    /**
     * Return the next Valve in this pipeline, or <code>null</code> if this
     * is the last Valve in the pipeline.
     */
    public Valve getNext() {

        return next;
    }


    /**
     * Set the Valve that follows this one in the pipeline it is part of.
     *
     * @param valve The new next valve
     */
    public void setNext(Valve valve) {

        this.next = valve;
    }


    //---------------------------------------------------------- Public Methods

    /**
     * Execute a periodic task, such as reloading, etc. This method will be
     * invoked inside the classloading context of this container. Unexpected
     * throwables will be caught and logged.
     */
    public void backgroundProcess() {
        // Deliberate no-op
    }


    /**
     * The implementation-specific logic represented by this Valve.  See the
     * Valve description for the normal design patterns for this method.
     * <p>
     * This method <strong>MUST</strong> be provided by a subclass.
     *
     * @param request The servlet request to be processed
     * @param response The servlet response to be created
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public abstract int invoke(Request request, Response response)
        throws IOException, ServletException;


    /**
     * A post-request processing implementation that does nothing.
     *
     * Very few Valves override this behaviour as most Valve logic
     * is used for request processing.
     */
    public void postInvoke(Request request, Response response)
            throws IOException, ServletException {
        // Deliberate no-op
    }


    /**
     * Tomcat-style invocation.
     */
    public void invoke(org.apache.catalina.connector.Request request,
                       org.apache.catalina.connector.Response response)
            throws IOException, ServletException {
        // Deliberate no-op
    }


    /**
     * Process a Comet event. This method will rarely need to be provided by
     * a subclass, unless it needs to reassociate a particular object with
     * the thread that is processing the request.
     *
     * @param request The servlet request to be processed
     * @param response The servlet response to be created
     *
     * @exception IOException if an input/output error occurs, or is thrown
     *  by a subsequently invoked Valve, Filter, or Servlet
     * @exception ServletException if a servlet error occurs, or is thrown
     *  by a subsequently invoked Valve, Filter, or Servlet
     */
    public void event(org.apache.catalina.connector.Request request,
                      org.apache.catalina.connector.Response response,
                      CometEvent event)
            throws IOException, ServletException {
        // Perform the request
        if (getNext() != null) {
            getNext().event(request, response, event);
        }
    }
    
        
    /**    
     * @return true if this access log valve has been started, false
     * otherwise.
     */
    public boolean isStarted() {
        return started;
    }


    // START CR 6411114
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
     * with this Valve.
     */
    public List<LifecycleListener> findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to add
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
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started)
            return;
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;
    }


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            return;
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

    }




    // END CR 6411114
    // -------------------- JMX and Registration  --------------------
    protected String domain;
    protected ObjectName oname;
    protected ObjectName controller;

    public ObjectName getObjectName() {
        return oname;
    }

    public void setObjectName(ObjectName oname) {
        this.oname = oname;
    }

    public String getDomain() {
        return domain;
    }

    public ObjectName getController() {
        return controller;
    }

    public void setController(ObjectName controller) {
        this.controller = controller;
    }

    /** From the name, extract the parent object name
     *
     * @param valveName
     * @return parentName
     */
    public ObjectName getParentName( ObjectName valveName ) {
        return null;
    }

    public ObjectName createObjectName(String domain, ObjectName parent)
            throws MalformedObjectNameException
    {
        Container container=this.getContainer();
        if( container == null || ! (container instanceof ContainerBase) )
            return null;
        ContainerBase containerBase=(ContainerBase)container;
        Pipeline pipe=containerBase.getPipeline();
        GlassFishValve valves[]=pipe.getValves();

        /* Compute the "parent name" part */
        String parentName="";
        if (container instanceof Engine) {
        } else if (container instanceof Host) {
            parentName=",host=" +container.getName();
        } else if (container instanceof Context) {
            String path = ((Context)container).getPath();
            if (path.length() < 1) {
                path = "/";
            }
            Host host = (Host) container.getParent();
            parentName=",path=" + path + ",host=" + host.getName();
        } else if (container instanceof Wrapper) {
            Context ctx = (Context) container.getParent();
            String path = ctx.getPath();
            if (path.length() < 1) {
                path = "/";
            }
            Host host = (Host) ctx.getParent();
            parentName=",servlet=" + container.getName() +
                    ",path=" + path + ",host=" + host.getName();
        }
        
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "valve parent=" + parentName + " " + parent);
        }

        String className=this.getClass().getName();
        int period = className.lastIndexOf('.');
        if (period >= 0)
            className = className.substring(period + 1);

        int seq=0;
        for( int i=0; i<valves.length; i++ ) {
            // Find other valves with the same name
            if (valves[i]==this) {
                break;
            }
            if( valves[i]!=null &&
                    valves[i].getClass() == this.getClass()) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Duplicate " + valves[i] + " " + this + " " +
                            container);
                }
                seq++;
            }
        }
        String ext="";
        if( seq > 0 ) {
            ext=",seq=" + seq;
        }

        ObjectName objectName = 
            new ObjectName( domain + ":type=Valve,name=" + className + ext + parentName);

        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "valve objectname = "+objectName);
        }

        return objectName;
    }

}
