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
// Portions Copyright [2016-2019] [Payara Foundation and/or affiliates]

package org.apache.catalina.core;

import org.apache.catalina.*;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.connector.*;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.web.valve.GlassFishValve;
import org.glassfish.web.valve.GlassFishValveAdapter;
import org.glassfish.web.valve.TomcatValveAdapter;

import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpUpgradeHandler;

import static java.text.MessageFormat.format;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.apache.catalina.ContainerEvent.AFTER_UPGRADE_HANDLER_INITIALIZED;
import static org.apache.catalina.ContainerEvent.BEFORE_UPGRADE_HANDLER_INITIALIZED;
import static org.apache.catalina.Globals.WRAPPED_REQUEST;
import static org.apache.catalina.Globals.WRAPPED_RESPONSE;
import static org.apache.catalina.LogFacade.INTERNAL_ERROR;
import static org.apache.catalina.LogFacade.NO_VALVES_IN_PIPELINE_EXCEPTION;
import static org.apache.catalina.LogFacade.PROTOCOL_HANDLER_REQUIRED_EXCEPTION;
import static org.apache.catalina.LogFacade.STANDARD_PIPELINE_INFO;
import static org.apache.catalina.LogFacade.STANDARD_PIPELINE_NULL_INFO;
import static org.glassfish.web.valve.GlassFishValve.INVOKE_NEXT;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

/** CR 6411114 (Lifecycle implementation moved to ValveBase)
import org.apache.tomcat.util.modeler.Registry;
*/

/**
 * Standard implementation of a processing <b>Pipeline</b> that will invoke a series of Valves that
 * have been configured to be called in order. This implementation can be used for any type of
 * Container.
 *
 * <b>IMPLEMENTATION WARNING</b> - This implementation assumes that no calls to
 * <code>addValve()</code> or <code>removeValve</code> are allowed while a request is currently
 * being processed. Otherwise, the mechanism by which per-thread state is maintained will need to be
 * modified.
 *
 * @author Craig R. McClanahan
 */

public class StandardPipeline implements Pipeline, Contained, Lifecycle {

    private static final Logger log = LogFacade.getLogger();
    private static final ResourceBundle rb = log.getResourceBundle();

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new StandardPipeline instance with no associated Container.
     */
    public StandardPipeline() {

        this(null);

    }

    /**
     * Construct a new StandardPipeline instance that is associated with the specified Container.
     *
     * @param container The container we should be associated with
     */
    public StandardPipeline(Container container) {

        super();
        setContainer(container);

    }

    // ----------------------------------------------------- Instance Variables

    /**
     * The basic Valve (if any) associated with this Pipeline.
     */
    protected GlassFishValve basic;

    /**
     * The Container with which this Pipeline is associated.
     */
    protected Container container;

    /**
     * Descriptive information about this implementation.
     */
    protected static final String info = "org.apache.catalina.core.StandardPipeline/1.0";

    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);

    /**
     * Has this component been started yet?
     */
    protected boolean started;

    // START OF IASRI# 4647091
    /*
     * The per-thread execution state for processing through this pipeline. The actual value is a
     * java.lang.Integer object containing the subscript into the <code>values</code> array, or a
     * subscript equal to <code>values.length</code> if the basic Valve is currently being processed.
     */
    // protected ThreadLocal state = new ThreadLocal();
    // END OF IASRI# 4647091

    /**
     * The set of Valves (not including the Basic one, if any) associated with this Pipeline.
     */
    protected GlassFishValve valves[] = new GlassFishValve[0];

    // The first Tomcat-style valve in the pipeline, if any
    private Valve firstTcValve;

    // The last Tomcat-style valve (immediately preceding the basic valve)
    // in the pipeline, if any
    private Valve lastTcValve;

    // --------------------------------------------------------- Public Methods

    /**
     * Return descriptive information about this implementation class.
     */
    public String getInfo() {
        return info;
    }

    // ------------------------------------------------------ Contained Methods

    /**
     * Return the Container with which this Pipeline is associated.
     */
    @Override
    public Container getContainer() {
        return container;
    }

    /**
     * Set the Container with which this Pipeline is associated.
     *
     * @param container The new associated container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    // --------------------------------------------------- Lifecycle Methods

    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
     * Gets the (possibly empty) list of lifecycle listeners associated with this Pipeline.
     */
    @Override
    public List<LifecycleListener> findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    /**
     * Prepare for active use of the public methods of this Component.
     *
     * @exception IllegalStateException if this component has already been started
     * @exception LifecycleException if this component detects a fatal error that prevents it from being
     * started
     */
    @Override
    public synchronized void start() throws LifecycleException {

        // Validate and update our current component state
        if (started)
            throw new LifecycleException(rb.getString(LogFacade.PIPLINE_STARTED));

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        started = true;

        // Start the Valves in our pipeline (including the basic), if any
        for (GlassFishValve valve : valves) {
            if (valve instanceof Lifecycle) {
                ((Lifecycle) valve).start();
                /**
                 * CR 6411114 (MBean registration moved to ValveBase.start()) registerValve(valves[i]);
                 */
            }
        }
        if ((basic != null) && (basic instanceof Lifecycle))
            ((Lifecycle) basic).start();

        /**
         * CR 6411114 (MBean registration moved to ValveBase.start()) if( basic!=null )
         * registerValve(basic);
         */

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(START_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);

    }

    /**
     * Gracefully shut down active use of the public methods of this Component.
     *
     * @exception IllegalStateException if this component has not been started
     * @exception LifecycleException if this component detects a fatal error that needs to be reported
     */
    @Override
    public synchronized void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException(rb.getString(LogFacade.PIPLINE_NOT_STARTED));

        started = false;

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);

        // Stop the Valves in our pipeline (including the basic), if any
        if ((basic != null) && (basic instanceof Lifecycle))
            ((Lifecycle) basic).stop();
        /**
         * CR 6411114 (MBean deregistration moved to ValveBase.stop()) if( basic!=null ) {
         * unregisterValve(basic); }
         */
        for (GlassFishValve valve : valves) {
            if (valve instanceof Lifecycle) {
                ((Lifecycle) valve).stop();
                /**
                 * CR 6411114 (MBean deregistration moved to ValveBase.stop()) unregisterValve(valves[i]);
                 */
            }
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);

    }

    // ------------------------------------------------------- Pipeline Methods

    /**
     * <p>
     * Return the Valve instance that has been distinguished as the basic Valve for this Pipeline (if
     * any).
     */
    @Override
    public GlassFishValve getBasic() {
        return basic;
    }

    /**
     * <p>
     * Set the Valve instance that has been distinguished as the basic Valve for this Pipeline (if any).
     * Prior to setting the basic Valve, the Valve's <code>setContainer()</code> will be called, if it
     * implements <code>Contained</code>, with the owning Container as an argument. The method may throw
     * an <code>IllegalArgumentException</code> if this Valve chooses not to be associated with this
     * Container, or <code>IllegalStateException</code> if it is already associated with a different
     * Container.
     * </p>
     *
     * @param valve Valve to be distinguished as the basic Valve
     */
    @Override
    public void setBasic(GlassFishValve valve) {

        // Change components if necessary
        GlassFishValve oldBasic = null;
        synchronized (this) {
            oldBasic = this.basic;
        }
        if (oldBasic == valve) {
            return;
        }

        // Stop the old component if necessary
        if (oldBasic != null) {
            synchronized (this) {
                if (started && (oldBasic instanceof Lifecycle)) {
                    try {
                        ((Lifecycle) oldBasic).stop();
                    } catch (LifecycleException e) {
                        log.log(Level.SEVERE, LogFacade.SET_BASIC_STOP_EXCEPTION, e);
                    }
                }
            }
            if (oldBasic instanceof Contained) {
                try {
                    ((Contained) oldBasic).setContainer(null);
                } catch (Throwable t) {
                    // Ignore
                }
            }
        }

        // Start the new component if necessary
        if (valve == null) {
            return;
        }
        if (valve instanceof Contained) {
            ((Contained) valve).setContainer(this.container);
        }

        /**
         * CR 6411114 if (valve instanceof Lifecycle) {
         */
        // START CR 6411114
        // Start the valve if the pipeline has already been started
        if (started && (valve instanceof Lifecycle)) {
            // END CR 6411114
            try {
                ((Lifecycle) valve).start();
            } catch (LifecycleException e) {
                log.log(Level.SEVERE, LogFacade.SET_BASIC_START_EXCEPTION, e);
                return;
            }
        }

        synchronized (this) {
            this.basic = valve;
        }

    }

    /**
     * <p>
     * Add a new Valve to the end of the pipeline associated with this Container. Prior to adding the
     * Valve, the Valve's <code>setContainer()</code> method will be called, if it implements
     * <code>Contained</code>, with the owning Container as an argument. The method may throw an
     * <code>IllegalArgumentException</code> if this Valve chooses not to be associated with this
     * Container, or <code>IllegalStateException</code> if it is already associated with a different
     * Container.
     * </p>
     *
     * @param valve Valve to be added
     *
     * @exception IllegalArgumentException if this Container refused to accept the specified Valve
     * @exception IllegalArgumentException if the specified Valve refuses to be associated with this
     * Container
     * @exception IllegalStateException if the specified Valve is already associated with a different
     * Container
     */
    @Override
    public void addValve(GlassFishValve valve) {

        if (firstTcValve != null) {
            // Wrap GlassFish-style valve inside Tomcat-style valve
            addValve(new TomcatValveAdapter(valve));
            return;
        }

        // Validate that we can add this Valve
        if (valve instanceof Contained)
            ((Contained) valve).setContainer(this.container);

        // Start the new component if necessary
        if (started) {
            if (valve instanceof Lifecycle) {
                try {
                    ((Lifecycle) valve).start();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, LogFacade.ADD_VALVE_EXCEPTION, e);
                }
            }
            /**
             * CR 6411114 (MBean registration moved to ValveBase.start()) // Register the newly added valve
             * registerValve(valve);
             */
        }

        // Add this Valve to the set associated with this Pipeline
        GlassFishValve results[] = new GlassFishValve[valves.length + 1];
        System.arraycopy(valves, 0, results, 0, valves.length);
        results[valves.length] = valve;
        valves = results;
    }

    /**
     * Add Tomcat-style valve.
     */
    @Override
    public synchronized void addValve(Valve valve) {

        /*
         * Check if this is a GlassFish-style valve that was compiled against the old
         * org.apache.catalina.Valve interface (from GlassFish releases prior to V3), which has since been
         * renamed to org.glassfish.web.valve.GlassFishValve (in V3)
         */
        if (isGlassFishValve(valve)) {
            try {
                addValve(new GlassFishValveAdapter(valve));
            } catch (Exception e) {
                String msg = MessageFormat.format(rb.getString(LogFacade.ADD_TOMCAT_STYLE_VALVE_EXCEPTION), valve);
                log.log(Level.SEVERE, msg, e);
            }
            return;
        }

        if (valve instanceof Contained)
            ((Contained) valve).setContainer(this.container);

        // Start the new Valve if necessary
        if (started) {
            if (valve instanceof Lifecycle) {
                try {
                    ((Lifecycle) valve).start();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, LogFacade.ADD_VALVE_EXCEPTION, e);
                }
            }
        }

        if (firstTcValve == null) {
            firstTcValve = lastTcValve = valve;
        } else {
            lastTcValve.setNext(valve);
            lastTcValve = valve;
        }

        if (basic != null) {
            valve.setNext((Valve) basic);
        }
    }

    /**
     * Return the set of Valves in the pipeline associated with this Container, including the basic
     * Valve (if any). If there are no such Valves, a zero-length array is returned.
     */
    @Override
    public GlassFishValve[] getValves() {
        if (basic == null) {
            return (valves);
        }

        GlassFishValve results[] = new GlassFishValve[valves.length + 1];
        System.arraycopy(valves, 0, results, 0, valves.length);
        results[valves.length] = basic;

        return results;
    }

    /**
     * @return true if this pipeline has any non basic valves, false otherwise
     */
    @Override
    public boolean hasNonBasicValves() {
        return ((valves != null && valves.length > 0) || firstTcValve != null);
    }

    public ObjectName[] getValveObjectNames() {
        ObjectName oname[] = new ObjectName[valves.length + 1];
        for (int i = 0; i < valves.length; i++) {
            if (valves[i] instanceof ValveBase)
                oname[i] = ((ValveBase) valves[i]).getObjectName();
        }
        if (basic instanceof ValveBase) {
            oname[valves.length] = ((ValveBase) basic).getObjectName();
        }

        return oname;
    }

    /**
     * Cause the specified request and response to be processed by the Valves associated with this
     * pipeline, until one of these valves causes the response to be created and returned. The
     * implementation must ensure that multiple simultaneous requests (on different threads) can be
     * processed through the same Pipeline without interfering with each other's control flow.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception is thrown
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        doInvoke(request, response, false);
    }

    public void doChainInvoke(Request request, Response response) throws IOException, ServletException {
        doInvoke(request, response, true);
    }

    /**
     * Remove the specified Valve from the pipeline associated with this Container, if it is found;
     * otherwise, do nothing. If the Valve is found and removed, the Valve's
     * <code>setContainer(null)</code> method will be called if it implements <code>Contained</code>.
     *
     * @param valve Valve to be removed
     */
    @Override
    public void removeValve(GlassFishValve valve) {

        // Locate this Valve in our list
        int j = -1;
        for (int i = 0; i < valves.length; i++) {
            if (valve == valves[i]) {
                j = i;
                break;
            }
        }
        if (j < 0) {
            return;
        }

        // Remove this valve from our list
        GlassFishValve results[] = new GlassFishValve[valves.length - 1];
        int n = 0;
        for (int i = 0; i < valves.length; i++) {
            if (i == j) {
                continue;
            }
            results[n++] = valves[i];
        }
        valves = results;
        try {
            if (valve instanceof Contained) {
                ((Contained) valve).setContainer(null);
            }
        } catch (Throwable t) {
            ;
        }

        // Stop this valve if necessary
        if (started) {
            if (valve instanceof ValveBase) {
                if (((ValveBase) valve).isStarted()) {
                    try {
                        ((Lifecycle) valve).stop();
                    } catch (LifecycleException e) {
                        log.log(Level.SEVERE, LogFacade.REMOVE_VALVE_EXCEPTION, e);
                    }
                }
            } else if (valve instanceof Lifecycle) {
                try {
                    ((Lifecycle) valve).stop();
                } catch (LifecycleException e) {
                    log.log(Level.SEVERE, LogFacade.REMOVE_VALVE_EXCEPTION, e);
                }
            }

            /**
             * CR 6411114 (MBean deregistration moved to ValveBase.stop()) // Unregister the removed valve
             * unregisterValve(valve);
             */
        }

    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        org.apache.catalina.Logger logger = null;
        if (container != null) {
            logger = container.getLogger();

            String msg = MessageFormat.format(rb.getString(LogFacade.STANDARD_PIPELINE_INFO),
                    new Object[] { container.getName(), message });

            if (logger != null) {
                logger.log(msg);
            } else {
                log.log(INFO, msg);
            }
        } else {
            if (log.isLoggable(INFO)) {
                log.log(INFO, format(rb.getString(STANDARD_PIPELINE_NULL_INFO), message));
            }
        }
    }

    /**
     * Logs the given message to the Logger associated with the Container (if any) of this
     * StandardPipeline.
     *
     * @param message the message
     * @param t the Throwable
     */
    protected void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = null;
        if (container != null) {
            logger = container.getLogger();

            String msg = format(rb.getString(STANDARD_PIPELINE_INFO), new Object[] { container.getName(), message });

            if (logger != null) {
                logger.log(msg, t, org.apache.catalina.Logger.WARNING);
            } else {
                log.log(WARNING, msg, t);
            }
        } else {
            log.log(Level.WARNING, format(rb.getString(STANDARD_PIPELINE_NULL_INFO), message), t);// INFO set to WARNING
        }
    }


    // ------------------------------------------------------ Private Methods

    private void doInvoke(Request request, Response response, boolean chaining) throws IOException, ServletException {
        if (valves.length > 0 || basic != null) {

            // Set the status so that if there are no valves (other than the
            // basic one), the basic valve's request processing logic will
            // be invoked
            int status = INVOKE_NEXT;

            // Iterate over all the valves in the pipeline and invoke
            // each valve's processing logic and then move onto to the
            // next valve in the pipeline only if the previous valve indicated
            // that the pipeline should proceed.
            int i;
            for (i = 0; i < valves.length; i++) {
                Request req = request;
                Response resp = response;
                if (chaining) {
                    // Check if the (JASPIC) authentication module has provided a wrapped
                    // request and response
                    req = getWrappedRequest(request);
                    resp = getWrappedResponse(request, response);
                }
                status = valves[i].invoke(req, resp);
                if (status != INVOKE_NEXT) {
                    break;
                }
            }

            // Save a reference to the valve[], to ensure that postInvoke()
            // is invoked on the original valve[], in case a valve gets added
            // or removed during the invocation of the basic valve (e.g.,
            // in case access logging is enabled or disabled by some kind of
            // admin servlet), in which case the indices used for postInvoke
            // invocations below would be off
            GlassFishValve[] savedValves = valves;

            // Invoke the basic valve's request processing and post-request
            // logic only if the pipeline was not aborted (i.e. no valve
            // returned END_PIPELINE).
            // In addition, the basic valve needs to be invoked by the
            // pipeline only if no Tomcat-style valves have been added.
            // Otherwise, it will be invoked by the last Tomcat-style valve
            // directly.
            if (status == INVOKE_NEXT) {
                if (firstTcValve != null) {
                    firstTcValve.invoke((org.apache.catalina.connector.Request) request, (org.apache.catalina.connector.Response) response);
                } else if (basic != null) {
                    Request req = request;
                    Response resp = response;
                    if (chaining) {
                        req = getWrappedRequest(request);
                        resp = getWrappedResponse(request, response);
                    }
                    basic.invoke(req, resp);
                    postInvoke(basic, req, resp);
                }
            }

            // Invoke the post-request processing logic only on those valves
            // that returned a status of INVOKE_NEXT
            for (int j = i - 1; j >= 0; j--) {
                Request req = request;
                Response resp = response;
                if (chaining) {
                    req = getWrappedRequest(request);
                    resp = getWrappedResponse(request, response);
                }

                postInvoke(savedValves[j], req, resp);
            }

            savedValves = null;

        } else {
            throw new ServletException(rb.getString(NO_VALVES_IN_PIPELINE_EXCEPTION));
        }

        // Calls the protocol handler's init method if the request is marked to be upgraded
        if (request instanceof org.apache.catalina.connector.Request) {
            org.apache.catalina.connector.Request req = (org.apache.catalina.connector.Request) request;
            if (req.isUpgrade()) {
                HttpUpgradeHandler handler = req.getHttpUpgradeHandler();
                if (handler != null) {
                    WebConnectionImpl wc = new WebConnectionImpl(req.getInputStream(),
                            ((org.apache.catalina.connector.Response) req.getResponse()).getOutputStream());
                    wc.setRequest(req);
                    req.setWebConnection(wc);
                    if (response instanceof org.apache.catalina.connector.Response) {
                        wc.setResponse((org.apache.catalina.connector.Response) response);
                    }
                    Context context = req.getContext();
                    try {
                        context.fireContainerEvent(BEFORE_UPGRADE_HANDLER_INITIALIZED, handler);
                        req.initialiseHttpUpgradeHandler(wc);
                    } finally {
                        context.fireContainerEvent(AFTER_UPGRADE_HANDLER_INITIALIZED, handler);
                    }
                } else {
                    log.log(Level.SEVERE, PROTOCOL_HANDLER_REQUIRED_EXCEPTION);
                }
                // req.setUpgrade(false);
            }
        }
    }

    private void postInvoke(GlassFishValve savedValve, Request request, Response response) throws IOException, ServletException {
        if (request.getRequest().isAsyncSupported() && request.getRequest().isAsyncStarted()) {
            request.getRequest().getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) throws IOException {
                    try {
                        savedValve.postInvoke(request, response);
                    } catch (ServletException ex) {
                        log.log(SEVERE, INTERNAL_ERROR, ex);
                    }
                }

                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    try {
                        savedValve.postInvoke(request, response);
                    } catch (ServletException ex) {
                        log.log(SEVERE, INTERNAL_ERROR, ex);
                    }
                }

                @Override
                public void onError(AsyncEvent event) throws IOException {
                    try {
                        savedValve.postInvoke(request, response);
                    } catch (ServletException ex) {
                        log.log(SEVERE, INTERNAL_ERROR, ex);
                    }
                }

                @Override
                public void onStartAsync(AsyncEvent event) throws IOException {
                }
            });
        } else {
            savedValve.postInvoke(request, response);
        }
    }

    /*
     * Checks if the give valve is a GlassFish-style valve that was compiled against the old
     * org.apache.catalina.Valve interface (from GlassFish releases prior to V3), which has since been
     * renamed to org.glassfish.web.valve.GlassFishValve (in V3).
     *
     * The check is done by checking that it is not an abstract method with return type int. Note that
     * invoke method in the original Tomcat-based Valve interface is declared to be void.
     *
     * @param valve the valve to check
     *
     * @return true if the given valve is a GlassFish-style valve, false otherwise
     */
    private boolean isGlassFishValve(Valve valve) {
        try {
            Method m = valve.getClass().getMethod("invoke", org.apache.catalina.Request.class, org.apache.catalina.Response.class);
            return (m != null && int.class.equals(m.getReturnType()) && (!Modifier.isAbstract(m.getModifiers())));
        } catch (Exception e) {
            return false;
        }
    }

    private Request getWrappedRequest(Request request) {
        Request wrappedRequest = (Request) request.getNote(WRAPPED_REQUEST);
        if (wrappedRequest == null) {
            wrappedRequest = request;
        }

        return wrappedRequest;
    }

    private Response getWrappedResponse(Request request, Response response) {
        Response wrappedResponse = (Response) request.getNote(WRAPPED_RESPONSE);
        if (wrappedResponse == null) {
            wrappedResponse = response;
        }

        return wrappedResponse;
    }
}
