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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package org.apache.catalina.core;

import org.glassfish.jersey.servlet.ServletContainer;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.apache.catalina.InstanceEvent.EventType.*;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.LogFacade;
import org.apache.catalina.Wrapper;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.InstanceSupport;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;
import org.glassfish.web.valve.GlassFishValve;

import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;
import io.opentracing.tag.Tags;
import java.util.logging.Logger;

/**
 * Standard implementation of the <b>Wrapper</b> interface that represents
 * an individual servlet definition.  No child Containers are allowed, and
 * the parent Container must be a Context.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.12.2.1 $ $Date: 2008/04/17 18:37:09 $
 */
public class StandardWrapper
        extends ContainerBase
        implements ServletConfig, Wrapper {

    private static final String[] DEFAULT_SERVLET_METHODS = new String[] {
                                                    "GET", "HEAD", "POST" };

    private final RequestTracingService requestTracing;
    private final OpenTracingService openTracing;
    private static final ThreadLocal<Boolean> isInSuppressFFNFThread = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    
    
    // ----------------------------------------------------- Instance Variables


    /**
     * The date and time at which this servlet will become available (in
     * milliseconds since the epoch), or zero if the servlet is available.
     * If this value equals Long.MAX_VALUE, the unavailability of this
     * servlet is considered permanent.
     */
    private long available = 0L;


    /**
     * The broadcaster that sends j2ee notifications.
     */
    private NotificationBroadcasterSupport broadcaster = null;


    /**
     * The count of allocations that are currently active (even if they
     * are for the same instance, as will be true on a non-STM servlet).
     */
    private AtomicInteger countAllocated = new AtomicInteger(0);


    /**
     * The debugging detail level for this component.
     */
    private int debug = 0;


    /**
     * The facade associated with this wrapper.
     */
    private StandardWrapperFacade facade =
        new StandardWrapperFacade(this);


    /**
     * The descriptive information string for this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.StandardWrapper/1.0";


    /**
     * The (single) initialized instance of this servlet.
     */
    private volatile Servlet instance = null;


    /**
     * Flag that indicates if this instance has been initialized
     */
    protected volatile boolean instanceInitialized = false;
    

    /**
     * The support object for our instance listeners.
     */
    private InstanceSupport instanceSupport = new InstanceSupport(this);


    /**
     * The context-relative URI of the JSP file for this servlet.
     */
    private String jspFile = null;


    /**
     * The load-on-startup order value (negative value means load on
     * first call) for this servlet.
     */
    private int loadOnStartup = -1;


    /**
     * Mappings associated with the wrapper.
     */
    private ArrayList<String> mappings = new ArrayList<String>();


    /**
     * The initialization parameters for this servlet, keyed by
     * parameter name.
     */
    private Map<String, String> parameters = new HashMap<String, String>();


    /**
     * The security role references for this servlet, keyed by role name
     * used in the servlet.  The corresponding value is the role name of
     * the web application itself.
     */
    private HashMap<String, String> references = new HashMap<String, String>();


    /**
     * The run-as identity for this servlet.
     */
    private String runAs = null;


    /**
     * The notification sequence number.
     */
    private long sequenceNumber = 0;


    /**
     * The fully qualified servlet class name for this servlet.
     */
    private String servletClassName = null;


    /**
     * The class from which this servlet will be instantiated
     */
    private Class <? extends Servlet> servletClass = null;


    /**
     * Does this servlet implement the SingleThreadModel interface?
     */
    private volatile boolean singleThreadModel = false;


    /**
     * Are we unloading our servlet instance at the moment?
     */
    private boolean unloading = false;


    /**
     * Maximum number of STM instances.
     */
    private int maxInstances = 20;


    /**
     * Number of instances currently loaded for a STM servlet.
     */
    private int nInstances = 0;


    /**
     * Stack containing the STM instances.
     */
    private Stack<Servlet> instancePool = null;


    /**
     * Wait time for servlet unload in ms.
     */
    protected long unloadDelay = 2000;


    /**
     * True if this StandardWrapper is for the JspServlet
     */
    private boolean isJspServlet;


    /**
     * The ObjectName of the JSP monitoring mbean
     */
    private ObjectName jspMonitorON;


    // To support jmx attributes
    private StandardWrapperValve swValve;
    private long loadTime=0;
    private int classLoadTime=0;

    private String description;


    /**
     * Async support
     */
    private boolean isAsyncSupported = false;
    //private long asyncTimeout;


    /**
     * Static class array used when the SecurityManager is turned on and 
     * <code>Servlet.init</code> is invoked.
     */
    private static Class<?>[] classType = new Class[]{ServletConfig.class};
    
    
    /**
     * Static class array used when the SecurityManager is turned on and 
     * <code>Servlet.service</code>  is invoked.
     */                                                 
    private static Class<?>[] classTypeUsedInService = new Class[]{
                                                         ServletRequest.class,
                                                         ServletResponse.class};

    /**
     * File upload (multipart) support 
     */
    private boolean multipartConfigured = false;
    private String multipartLocation = null;
    private long multipartMaxFileSize = -1L;
    private long multipartMaxRequestSize = -1L;
    private int multipartFileSizeThreshold = 10240;  // 10K

    private boolean osgi;


    // ----------------------------------------------------------- Constructors


    /**
     * Create a new StandardWrapper component with the default basic Valve.
     */
    public StandardWrapper() {

        swValve = new StandardWrapperValve();
        pipeline.setBasic(swValve);
        requestTracing = Globals.getDefaultHabitat().getService(RequestTracingService.class);
        openTracing = Globals.getDefaultHabitat().getService(OpenTracingService.class);
        
        // suppress PWC6117 file not found errors
        Logger jspLog = Logger.getLogger("org.apache.jasper.servlet.JspServlet");
        if (!(jspLog.getFilter() instanceof NotFoundErrorSupressionFilter)) {
            jspLog.setFilter(new NotFoundErrorSupressionFilter(jspLog.getFilter()));
        }
    }


    // ------------------------------------------------------------- Properties

    /**
     * Return the available date/time for this servlet, in milliseconds since
     * the epoch.  If this date/time is Long.MAX_VALUE, it is considered to mean
     * that unavailability is permanent and any request for this servlet will return
     * an SC_NOT_FOUND error.  If this date/time is in the future, any request for
     * this servlet will return an SC_SERVICE_UNAVAILABLE error.  If it is zero,
     * the servlet is currently available.
     * @return 
     */
    @Override
    public long getAvailable() {

        return (this.available);

    }


    /**
     * Set the available date/time for this servlet, in milliseconds since the
     * epoch.  If this date/time is Long.MAX_VALUE, it is considered to mean
     * that unavailability is permanent and any request for this servlet will return
     * an SC_NOT_FOUND error. If this date/time is in the future, any request for
     * this servlet will return an SC_SERVICE_UNAVAILABLE error.
     *
     * @param available The new available date/time
     */
    @Override
    public void setAvailable(long available) {

        long oldAvailable = this.available;
        if (available > System.currentTimeMillis())
            this.available = available;
        else
            this.available = 0L;
        support.firePropertyChange("available", oldAvailable, this.available);

    }


    /**
     * Return the number of active allocations of this servlet, even if they
     * are all for the same instance (as will be true for servlets that do
     * not implement <code>SingleThreadModel</code>.
     * @return 
     */
    public int getCountAllocated() {

        return (this.countAllocated.get());

    }


    /**
     * Return the debugging detail level for this component.
     * @return 
     */
    @Override
    public int getDebug() {
        return (this.debug);
    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    @Override
    public void setDebug(int debug) {
        int oldDebug = this.debug;
        this.debug = debug;
        support.firePropertyChange("debug", oldDebug,
                (long) this.debug);
    }


    public String getEngineName() {
        return ((StandardContext)getParent()).getEngineName();
    }


    /**
     * Return descriptive information about this Container implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     * @return 
     */
    @Override
    public String getInfo() {
        return (info);
    }


    /**
     * Return the InstanceSupport object for this Wrapper instance.
     * @return 
     */
    public InstanceSupport getInstanceSupport() {
        return (this.instanceSupport);
    }


    /**
     * Return the context-relative URI of the JSP file for this servlet.
     * @return 
     */
    @Override
    public String getJspFile() {
        return (this.jspFile);
    }


    /**
     * Set the context-relative URI of the JSP file for this servlet.
     *
     * @param jspFile JSP file URI
     */
    @Override
    public void setJspFile(String jspFile) {

        String oldJspFile = this.jspFile;
        this.jspFile = jspFile;
        support.firePropertyChange("jspFile", oldJspFile, this.jspFile);

        // Each jsp-file needs to be represented by its own JspServlet and
        // corresponding JspMonitoring mbean, because it may be initialized
        // with its own init params
        isJspServlet = true;
    }


    /**
     * Return the load-on-startup order value (negative value means
     * load on first call).
     * @return 
     */
    @Override
    public int getLoadOnStartup() {

        if (isJspServlet && loadOnStartup < 0) {
            /*
             * JspServlet must always be preloaded, because its instance is
             * used during registerJMX (when registering the JSP
             * monitoring mbean)
             */
             return Integer.MAX_VALUE;
        } else {
            return (this.loadOnStartup);
        }
    }


    /**
     * Set the load-on-startup order value (negative value means
     * load on first call).
     *
     * @param value New load-on-startup value
     */
    @Override
    public void setLoadOnStartup(int value) {

        int oldLoadOnStartup = this.loadOnStartup;
        this.loadOnStartup = value;
        support.firePropertyChange("loadOnStartup",
                                   Integer.valueOf(oldLoadOnStartup),
                                   Integer.valueOf(this.loadOnStartup));

    }


    /**
     * Set the load-on-startup order value from a (possibly null) string.
     * Per the specification, any missing or non-numeric value is converted
     * to a zero, so that this servlet will still be loaded at startup
     * time, but in an arbitrary order.
     *
     * @param value New load-on-startup value
     */
    public void setLoadOnStartupString(String value) {

        try {
            setLoadOnStartup(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            setLoadOnStartup(0);
        }
    }


    public String getLoadOnStartupString() {
        return Integer.toString( getLoadOnStartup());
    }


    /**
     * Sets the description of this servlet.
     * @param description
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * Gets the description of this servlet.
     * @return 
     */
    @Override
    public String getDescription() {
        return description;
    }


    /**
     * Return maximum number of instances that will be allocated when a single
     * thread model servlet is used.
     */
    public int getMaxInstances() {
        return (this.maxInstances);
    }


    /**
     * Set the maximum number of instances that will be allocated when a single
     * thread model servlet is used.
     *
     * @param maxInstances New value of maxInstances
     */
    public void setMaxInstances(int maxInstances) {
        int oldMaxInstances = this.maxInstances;
        this.maxInstances = maxInstances;
        support.firePropertyChange("maxInstances", oldMaxInstances,
                                   this.maxInstances);
    }


    /**
     * Set the parent Container of this Wrapper, but only if it is a Context.
     *
     * @param container Proposed parent Container
     */
    @Override
    public void setParent(Container container) {
        if ((container != null) &&
            !(container instanceof Context))
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.PARENT_CONTAINER_MUST_BE_CONTEXT_EXCEPTION));
        if (container instanceof StandardContext) {
            unloadDelay = ((StandardContext)container).getUnloadDelay();
            notifyContainerListeners =
                ((StandardContext)container).isNotifyContainerListeners();
        }
        super.setParent(container);
    }


    /**
     * Return the run-as identity for this servlet.
     * @return 
     */
    @Override
    public String getRunAs() {
        return (this.runAs);
    }


    /**
     * Set the run-as identity for this servlet.
     *
     * @param runAs New run-as identity value
     */
    @Override
    public void setRunAs(String runAs) {
        String oldRunAs = this.runAs;
        this.runAs = runAs;
        support.firePropertyChange("runAs", oldRunAs, this.runAs);
    }


    /**
     * Marks the wrapped servlet as supporting async operations or not.
     *
     * @param isAsyncSupported true if the wrapped servlet supports async mode,
     * false otherwise
     */
    @Override
    public void setIsAsyncSupported(boolean isAsyncSupported) {
        this.isAsyncSupported = isAsyncSupported;
    }


    /**
     * Checks if the wrapped servlet has been annotated or flagged in the
     * deployment descriptor as being able to support asynchronous operations.
     *
     * @return true if the wrapped servlet supports async operations, and
     * false otherwise
     */
    @Override
    public boolean isAsyncSupported() {
        return isAsyncSupported;
    }


    /**
     * Return the fully qualified servlet class name for this servlet.
     * @return 
     */
    @Override
    public String getServletClassName() {
        return this.servletClassName;
    }


    /**
     * Set the fully qualified servlet class name for this servlet.
     *
     * @param className Servlet class name
     */
    @Override
    public void setServletClassName(String className) {
        if (className == null) {
            throw new NullPointerException("Null servlet class name");
        }
        if (servletClassName != null) {
            throw new IllegalStateException(
                "Wrapper already initialized with servlet instance, " +
                "class, or name");
        }
        servletClassName = className;
        // oldServletClassName is null
        support.firePropertyChange("servletClassName", null,
                                   servletClassName);
        if (Constants.JSP_SERVLET_CLASS.equals(servletClassName)) {
            isJspServlet = true;
        }
    }


    /**
     * @return the servlet class, or null if the servlet class has not
     * been loaded yet
     */
    public Class <? extends Servlet> getServletClass() {
        return servletClass;
    }

    /**
     * Sets the class object from which this servlet will be instantiated.
     *
     * @param clazz The class object from which this servlet will
     * be instantiated
     */
    @Override
    public void setServletClass(Class <? extends Servlet> clazz) {
        if (clazz == null) {
            throw new NullPointerException("Null servlet class");
        }
        if ((servletClass != null) ||
                servletClassName != null &&
                    !servletClassName.equals(clazz.getName())) {
            throw new IllegalStateException(
                "Wrapper already initialized with servlet instance, " +
                "class, or name");
        }
        servletClass = clazz;
        servletClassName = clazz.getName();
        if (Constants.JSP_SERVLET_CLASS.equals(servletClassName)) {
            isJspServlet = true;
        }
    }


    /**
     * @return the servlet instance, or null if the servlet has not yet
     * been instantiated
     */
    public Servlet getServlet() {
        return instance;
    }


    /**
     * Sets the servlet instance for this wrapper.
     *
     * @param instance the servlet instance
     */
    public void setServlet(Servlet instance) {
        if (instance == null) {
            throw new NullPointerException("Null servlet instance");
        }
        if (servletClassName != null) {
            throw new IllegalStateException(
                "Wrapper already initialized with servlet instance, " +
                "class, or name");
        }
        this.instance = instance;
        servletClass = instance.getClass();
        servletClassName = servletClass.getName();
        if (Constants.JSP_SERVLET_CLASS.equals(servletClassName)) {
            isJspServlet = true;
        }
    }


    /**
     * Set the name of this servlet.  This is an alias for the normal
     * <code>Container.setName()</code> method, and complements the
     * <code>getServletName()</code> method required by the
     * <code>ServletConfig</code> interface.
     *
     * @param name The new name of this servlet
     */
    public void setServletName(String name) {
        setName(name);
    }


    /**
     * Is this servlet currently unavailable?
     * @return 
     */
    @Override
    public boolean isUnavailable() {
        if (available == 0L)
            return (false);
        else if (available <= System.currentTimeMillis()) {
            available = 0L;
            return (false);
        } else
            return (true);
    }


    /**
     * Gets the names of the methods supported by the underlying servlet.
     *
     * This is the same set of methods included in the Allow response header
     * in response to an OPTIONS request method processed by the underlying
     * servlet.
     *
     * @return Array of names of the methods supported by the underlying
     * servlet
     * @throws javax.servlet.ServletException
     */
    @Override
    public String[] getServletMethods() throws ServletException {
	
        loadServletClass();

        if (!javax.servlet.http.HttpServlet.class.isAssignableFrom(
                                                        servletClass)) {
            return DEFAULT_SERVLET_METHODS;
        }

        HashSet<String> allow = new HashSet<String>();
        allow.add("TRACE");
        allow.add("OPTIONS");
	
        Method[] methods = getAllDeclaredMethods(servletClass);
        for (int i=0; methods != null && i<methods.length; i++) {
            Method m = methods[i];
            Class<?> params[] = m.getParameterTypes();

            if (!(params.length == 2 &&
                    params[0] == HttpServletRequest.class &&
                    params[1] == HttpServletResponse.class)) {
                continue;
            }

            if (m.getName().equals("doGet")) {
                allow.add("GET");
                allow.add("HEAD");
            } else if (m.getName().equals("doPost")) {
                allow.add("POST");
            } else if (m.getName().equals("doPut")) {
                allow.add("PUT");
            } else if (m.getName().equals("doDelete")) {
                allow.add("DELETE");
            }
        }

        String[] methodNames = new String[allow.size()];
        return allow.toArray(methodNames);
    }


    public boolean isMultipartConfigured() {
        return multipartConfigured;
    }

    /**
     * Sets the multipart location
     * @param location
     */
    @Override
    public void setMultipartLocation(String location) {
        multipartConfigured = true;
        multipartLocation = location;
    }


    /**
     * Gets the multipart location
     * @return 
     */
    @Override
    public String getMultipartLocation(){
        return multipartLocation;
    }


    /**
     * Sets the multipart max-file-size
     * @param maxFileSize
     */
    @Override
    public void setMultipartMaxFileSize(long maxFileSize) {
        multipartConfigured = true;
        multipartMaxFileSize = maxFileSize;
    }


    /**
     * Gets the multipart max-file-size
     * @return 
     */
    @Override
    public long getMultipartMaxFileSize() {
        return multipartMaxFileSize;
    }


    /**
     * Sets the multipart max-request-size
     * @param maxRequestSize
     */
    @Override
    public void setMultipartMaxRequestSize(long maxRequestSize) {
        multipartConfigured = true;
        multipartMaxRequestSize = maxRequestSize;
    }


    /**
     * Gets the multipart max-request-Size
     * @return 
     */
    @Override
    public long getMultipartMaxRequestSize() {
        return multipartMaxRequestSize;
    }


    /**
     * Sets the multipart file-size-threshold
     * @param fileSizeThreshold
     */
    @Override
    public void setMultipartFileSizeThreshold(int fileSizeThreshold) {
        multipartConfigured = true;
        multipartFileSizeThreshold = fileSizeThreshold;
    }


    /**
     * Gets the multipart file-size-threshol
     * @return 
     */
    @Override
    public int getMultipartFileSizeThreshold() {
        return multipartFileSizeThreshold;
    }

    /**
     * Returns whether this is running in an OSGi context
     * @return 
     */
    protected boolean isOSGi() {
        return osgi;
    }

    /**
     * Set whether this is being used in an OSGi context
     * @param osgi 
     */
    protected void setOSGi(boolean osgi) {
        this.osgi = osgi;
    }


    // --------------------------------------------------------- Public Methods


    // START GlassFish 1343
    @Override
    public synchronized void addValve(GlassFishValve valve) {
        /*
         * This exception should never be thrown in reality, because we never
         * add any valves to a StandardWrapper. 
         * This exception is added here as an alert mechanism only, should
         * there ever be a need to add valves to a StandardWrapper in the
         * future.
         * In that case, the optimization in StandardContextValve related to
         * GlassFish 1343 will need to be adjusted, by calling
         * pipeline.getValves() and checking the pipeline's length to
         * determine whether the basic valve may be invoked directly. The
         * optimization currently avoids a call to pipeline.getValves(),
         * because it is expensive.
         */
        throw new UnsupportedOperationException(
            "Adding valves to wrappers not supported");
    }
    // END GlassFish 1343


    /**
     * Extract the root cause from a servlet exception.
     * 
     * @param e The servlet exception
     */
    public static Throwable getRootCause(ServletException e) {
        Throwable rootCause = e;
        Throwable rootCauseCheck;
        // Extra aggressive rootCause finding
        int loops = 0;
        do {
            loops++;
            rootCauseCheck = rootCause.getCause();
            if (rootCauseCheck != null)
                rootCause = rootCauseCheck;
        } while (rootCauseCheck != null && (loops < 20));
        return rootCause;
    }


    /**
     * Refuse to add a child Container, because Wrappers are the lowest level
     * of the Container hierarchy.
     *
     * @param child Child container to be added
     */
    @Override
    public void addChild(Container child) {
        throw new IllegalStateException
                (rb.getString(LogFacade.WRAPPER_CONTAINER_NO_CHILD_EXCEPTION));
    }


    /**
     * Adds the initialization parameter with the given name and value
     * to this servlet.
     *
     * @param name the name of the init parameter
     * @param value the value of the init parameter
     */
    @Override
    public void addInitParameter(String name, String value) {
        setInitParameter(name, value, true);
        if (notifyContainerListeners) {
            fireContainerEvent("addInitParameter", name);
        }
    }


    /**
     * Sets the init parameter with the given name and value
     * on this servlet.
     *
     * @param name the init parameter name
     * @param value the init parameter value
     * @param override true if the given init param is supposed to
     * override an existing init param with the same name, and false
     * otherwise
     *
     * @return true if the init parameter with the given name and value
     * was set, false otherwise
     */
    public boolean setInitParameter(String name, String value, 
                                    boolean override) {
        if (null == name || null == value) {
            throw new IllegalArgumentException(
                "Null servlet init parameter name or value");
        }

        synchronized (parameters) {
            if (override || !parameters.containsKey(name)) {
                parameters.put(name, value);
                return true;
            } else {
                return false;
            }
        }
    }


    /**
     * Sets the initialization parameters contained in the given map
     * on this servlet.
     *
     * @param initParameters the map with the init params to set
     *
     * @return the (possibly empty) Set of initialization parameter names
     * that are in conflict
     */
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        if (null == initParameters) {
            throw new IllegalArgumentException("Null init parameters");
        }

        synchronized (parameters) {
            Set<String> conflicts = null;
            for (Map.Entry<String, String> e : initParameters.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    throw new IllegalArgumentException(
                        "Null parameter name or value");
                }
                if (parameters.containsKey(e.getKey())) {
                    if (conflicts == null) {
                        conflicts = new HashSet<String>();    
                    }
                    conflicts.add(e.getKey());
                }
            }

            if (conflicts != null) {
                return conflicts;
            }

            for (Map.Entry<String, String> e : initParameters.entrySet()) {
                setInitParameter(e.getKey(), e.getValue(), true);
            }
   
            return Collections.emptySet();
        }
    }


    /**
     * Add a new listener interested in InstanceEvents.
     *
     * @param listener The new listener
     */
    @Override
    public void addInstanceListener(InstanceListener listener) {
        instanceSupport.addInstanceListener(listener);
    }


    /**
     * Add a mapping associated with the {@link Wrapper}.
     *
     * @param mapping The new wrapper mapping
     */
    @Override
    public void addMapping(String mapping) {
        synchronized (mappings) {
            mappings.add(mapping);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("addMapping", mapping);
        }
    }

    /**
     * Gets all the mapping associated with the {@link Wrapper}.
     * @return an unmodifiable list
     */
    public Collection<String> getMappings() {
        synchronized (mappings) {
            return Collections.unmodifiableList(mappings);
        }
    }


    /**
     * Add a new security role reference record to the set of records for
     * this servlet.
     *
     * @param name Role name used within this servlet
     * @param link Role name used within the web application
     */
    @Override
    public void addSecurityReference(String name, String link) {
        synchronized (references) {
            references.put(name, link);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("addSecurityReference", name);
        }
    }


    /**
     * Allocate an initialized instance of this Servlet that is ready to have
     * its <code>service()</code> method called.  If the servlet class does
     * not implement <code>SingleThreadModel</code>, the (only) initialized
     * instance may be returned immediately.  If the servlet class implements
     * <code>SingleThreadModel</code>, the Wrapper implementation must ensure
     * that this instance is not allocated again until it is deallocated by a
     * call to <code>deallocate()</code>.
     *
     * @return 
     * @exception ServletException if the servlet init() method threw
     *  an exception
     * @exception ServletException if a loading error occurs
     */
    public synchronized Servlet allocate() throws ServletException {

        // If we are currently unloading this servlet, throw an exception
        if (unloading) {
            String msg = MessageFormat.format(rb.getString(LogFacade.CANNOT_ALLOCATE_SERVLET_EXCEPTION), getName());
            throw new ServletException(msg);
        }

        // If not SingleThreadedModel, return the same instance every time
        if (!singleThreadModel) {

            // Load and initialize our instance if necessary
            if (instance == null) {
                // No instance. Instantiate and initialize
                try {
                    if (log.isLoggable(Level.FINEST))
                        log.log(Level.FINEST, "Allocating non-STM instance");
                    instance = loadServlet();
                    initServlet(instance);
                } catch (ServletException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new ServletException
                            (rb.getString(LogFacade.ERROR_ALLOCATE_SERVLET_INSTANCE_EXCEPTION), e);
                }
            } else if (!instanceInitialized) {
                /*
                 * Instance not yet initialized. This is the case
                 * when the instance was registered via
                 * ServletContext#addServlet
                 */
                initServlet(instance);
            }

            if (!singleThreadModel) {
                if (log.isLoggable(Level.FINEST))
                    log.log(Level.FINEST, "Returning non-STM instance");
                countAllocated.incrementAndGet();
                return (instance);
            }
        }

        synchronized (instancePool) {

            while (countAllocated.get() >= nInstances) {
                // Allocate a new instance if possible, or else wait
                if (nInstances < maxInstances) {
                    try {
                        Servlet servlet = loadServlet();
                        initServlet(servlet);
                        instancePool.push(servlet);
                        nInstances++;
                    } catch (ServletException e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new ServletException
                                (rb.getString(LogFacade.ERROR_ALLOCATE_SERVLET_INSTANCE_EXCEPTION), e);
                    }
                } else {
                    try {
                        instancePool.wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "Returning allocated STM instance");
            }
            countAllocated.incrementAndGet();
            return instancePool.pop();
        }
    }


    /**
     * Return this previously allocated servlet to the pool of available
     * instances.  If this servlet class does not implement SingleThreadModel,
     * no action is actually required.
     *
     * @param servlet The servlet to be returned
     * @exception ServletException if a deallocation error occurs
     */
    @Override
    public void deallocate(Servlet servlet) throws ServletException {

        // If not SingleThreadModel, no action is required
        if (!singleThreadModel) {
            countAllocated.decrementAndGet();
            return;
        }

        // Unlock and free this instance
        synchronized (instancePool) {
            countAllocated.decrementAndGet();
            instancePool.push(servlet);
            instancePool.notify();
        }
    }


    /**
     * Return the value for the specified initialization parameter name,
     * if any; otherwise return <code>null</code>.
     *
     * @param name Name of the requested initialization parameter
     * @return 
     */
    @Override
    public String findInitParameter(String name) {
        synchronized (parameters) {
            return parameters.get(name);
        }
    }


    /**
     * Return the names of all defined initialization parameters for this
     * servlet.
     * @return 
     */
    @Override
    public String[] findInitParameters() {
        synchronized (parameters) {
            String results[] = new String[parameters.size()];
            return parameters.keySet().toArray(results);
        }
    }


    /**
     * Return the mappings associated with this wrapper.
     * @return 
     */
    @Override
    public String[] findMappings() {
        synchronized (mappings) {
            return mappings.toArray(new String[mappings.size()]);
        }
    }


    /**
     * Return the security role link for the specified security role
     * reference name, if any; otherwise return <code>null</code>.
     *
     * @param name Security role reference used within this servlet
     * @return 
     */
    @Override
    public String findSecurityReference(String name) {
        synchronized (references) {
            return references.get(name);
        }
    }


    /**
     * Return the set of security role reference names associated with
     * this servlet, if any; otherwise return a zero-length array.
     * @return 
     */
    @Override
    public String[] findSecurityReferences() {
        synchronized (references) {
            String results[] = new String[references.size()];
            return references.keySet().toArray(results);
        }
    }


    /**
     * FIXME: Fooling introspection ...
     * @return 
     */
    public Wrapper findMappingObject() {
        return (Wrapper) getMappingObject();
    }


    /**
     * Loads and initializes an instance of the servlet, if there is not
     * already at least one initialized instance.
     * This can be used, for example, to load servlets that are marked in
     * the deployment descriptor to be loaded at server startup time.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  Servlets whose classnames begin with
     * <code>org.apache.catalina.</code> (so-called "container" servlets)
     * are loaded by the same classloader that loaded this class, rather than
     * the classloader for the current web application.
     * This gives such classes access to Catalina internals, which are
     * prevented for classes loaded for web applications.
     *
     * @exception ServletException if the servlet init() method threw
     *  an exception or if some other loading problem occurs
     */
    @Override
    public synchronized void load() throws ServletException {
        instance = loadServlet();
        initServlet(instance);
    }


    /**
     * Creates an instance of the servlet, if there is not already
     * at least one initialized instance.
     */
    private synchronized Servlet loadServlet() throws ServletException {

        // Nothing to do if we already have an instance or an instance pool
        if (!singleThreadModel && (instance != null)) {
            return instance;
        }

        long t1 = System.currentTimeMillis();

        loadServletClass();

        // Instantiate the servlet class
        Servlet servlet = null;
        try {
            servlet = ((StandardContext)getParent()).createServletInstance(
                servletClass);
        } catch (ClassCastException e) {
            unavailable(null);
            // Restore the context ClassLoader
            String msg = MessageFormat.format(rb.getString(LogFacade.CLASS_IS_NOT_SERVLET_EXCEPTION), servletClass.getName());
            throw new ServletException(msg, e);
        } catch (Throwable e) {
            unavailable(null);
            // Restore the context ClassLoader
            String msg = MessageFormat.format(rb.getString(LogFacade.ERROR_INSTANTIATE_SERVLET_CLASS_EXCEPTION), servletClass.getName());
            throw new ServletException(msg, e);
        }

        // Check if loading the servlet in this web application should be
        // allowed
        if (!isServletAllowed(servlet)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.PRIVILEGED_SERVLET_CANNOT_BE_LOADED_EXCEPTION), servletClass.getName());
            throw new SecurityException(msg);
        }

        // Special handling for ContainerServlet instances
        if ((servlet instanceof ContainerServlet) &&
              (isContainerProvidedServlet(servletClass.getName()) ||
                ((Context)getParent()).getPrivileged() )) {
            ((ContainerServlet) servlet).setWrapper(this);
        }

        classLoadTime = (int) (System.currentTimeMillis() -t1);

        // Register our newly initialized instance
        singleThreadModel = servlet instanceof SingleThreadModel;
        if (singleThreadModel) {
            if (instancePool == null)
                instancePool = new Stack<Servlet>();
        }

        if (notifyContainerListeners) {
            fireContainerEvent("load", this);
        }

        loadTime = System.currentTimeMillis() - t1;

        return servlet;
    }


    /*
     * Loads the servlet class
     */
    private synchronized void loadServletClass() throws ServletException {
        if (servletClass != null) {
            return;
        }

        // If this "servlet" is really a JSP file, get the right class.
        String actualClass = servletClassName;
        if ((actualClass == null) && (jspFile != null)) {
            Wrapper jspWrapper = (Wrapper)
                ((Context) getParent()).findChild(Constants.JSP_SERVLET_NAME);
            if (jspWrapper != null) {
                actualClass = jspWrapper.getServletClassName();
                // Merge init parameters
                String paramNames[] = jspWrapper.findInitParameters();
                for (String paramName : paramNames) {
                    if (parameters.get(paramName) == null) {
                        parameters.put(paramName,
                                       jspWrapper.findInitParameter(paramName));
                    }
                }
            }
        }

        // Complain if no servlet class has been specified
        if (actualClass == null) {
            unavailable(null);
            String msg = MessageFormat.format(rb.getString(LogFacade.NO_SERVLET_BE_SPECIFIED_EXCEPTION), getName());
            throw new ServletException(msg);
        }

        // Acquire an instance of the class loader to be used
        Loader loader = getLoader();
        if (loader == null) {
            unavailable(null);
            String msg = MessageFormat.format(rb.getString(LogFacade.CANNOT_FIND_LOADER_EXCEPTION), getName());
            throw new ServletException(msg);
        }

        ClassLoader classLoader = loader.getClassLoader();

        // Special case class loader for a container provided servlet
        //  
        if (isContainerProvidedServlet(actualClass) && 
                ! ((Context)getParent()).getPrivileged() ) {
            // If it is a priviledged context - using its own
            // class loader will work, since it's a child of the container
            // loader
            classLoader = this.getClass().getClassLoader();
        }

        // Load the specified servlet class from the appropriate class loader
        Class clazz = null;
        try {
            if (SecurityUtil.isPackageProtectionEnabled()){
                final ClassLoader fclassLoader = classLoader;
                final String factualClass = actualClass;
                try{
                    clazz = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Class>(){
                            public Class run() throws Exception{
                                if (fclassLoader != null) {
                                    return fclassLoader.loadClass(factualClass);
                                } else {
                                    return Class.forName(factualClass);
                                }
                            }
                    });
                } catch(PrivilegedActionException pax){
                    Exception ex = pax.getException();
                    if (ex instanceof ClassNotFoundException){
                        throw (ClassNotFoundException)ex;
                    } else {
                        String msgErrorLoadingInfo = MessageFormat.format(rb.getString(LogFacade.ERROR_LOADING_INFO),
                                                          new Object[] {fclassLoader, factualClass});
                        getServletContext().log(msgErrorLoadingInfo, ex );
                    }
                }
            } else {
                if (classLoader != null) {
                    clazz = classLoader.loadClass(actualClass);
                } else {
                    clazz = Class.forName(actualClass);
                }
            }
        } catch (ClassNotFoundException e) {
            unavailable(null);
            String msgErrorLoadingInfo = MessageFormat.format(rb.getString(LogFacade.ERROR_LOADING_INFO),
                    new Object[] {classLoader, actualClass});
            getServletContext().log(msgErrorLoadingInfo, e );
            String msg = MessageFormat.format(rb.getString(LogFacade.CANNOT_FIND_SERVLET_CLASS_EXCEPTION), actualClass);
            throw new ServletException(msg, e);
        }

        if (clazz == null) {
            String msg = MessageFormat.format(rb.getString(LogFacade.CANNOT_FIND_SERVLET_CLASS_EXCEPTION), actualClass);
            unavailable(null);
            throw new ServletException(msg);
        }

        servletClass = castToServletClass(clazz);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Servlet> castToServletClass(Class<?> clazz) {
        return (Class<? extends Servlet>)clazz;
    }


    /**
     * Initializes the given servlet instance, by calling its init method.
     */
    private void initServlet(Servlet servlet) throws ServletException {
        if (instanceInitialized && !singleThreadModel) {
            // Servlet has already been initialized
            return;
        }

        try {
            instanceSupport.fireInstanceEvent(BEFORE_INIT_EVENT, servlet);
            // START SJS WS 7.0 6236329
            //if( System.getSecurityManager() != null) {
            if ( SecurityUtil.executeUnderSubjectDoAs() ){
            // END OF SJS WS 7.0 6236329
                Object[] initType = new Object[1];
                initType[0] = facade;
                SecurityUtil.doAsPrivilege("init", servlet, classType,
                                           initType);
                initType = null;
            } else {
                servlet.init(facade);
            }

            instanceInitialized = true;

            // Invoke jspInit on JSP pages
            if ((loadOnStartup >= 0) && (jspFile != null)) {
                // Invoking jspInit
                DummyRequest req = new DummyRequest();
                req.setServletPath(jspFile);
                req.setQueryString("jsp_precompile=true");

                // START PWC 4707989
                String allowedMethods = (String) parameters.get("httpMethods");
                if (allowedMethods != null
                        && allowedMethods.length() > 0) {
                    String[] s = allowedMethods.split(",");
                    if (s.length > 0) {
                        req.setMethod(s[0].trim());
                    }
                }
                // END PWC 4707989

                DummyResponse res = new DummyResponse();

                // START SJS WS 7.0 6236329
                //if( System.getSecurityManager() != null) {
                if ( SecurityUtil.executeUnderSubjectDoAs() ){
                // END OF SJS WS 7.0 6236329
                    Object[] serviceType = new Object[2];
                    serviceType[0] = req;
                    serviceType[1] = res;                
                    SecurityUtil.doAsPrivilege("service", servlet,
                                               classTypeUsedInService,
                                               serviceType);
                } else {
                    servlet.service(req, res);
                }
            }
            instanceSupport.fireInstanceEvent(AFTER_INIT_EVENT,servlet);

        } catch (UnavailableException f) {
            instanceSupport.fireInstanceEvent(AFTER_INIT_EVENT,servlet, f);
            unavailable(f);
            throw f;

        } catch (ServletException f) {
            instanceSupport.fireInstanceEvent(AFTER_INIT_EVENT,servlet, f);
            // If the servlet wanted to be unavailable it would have
            // said so, so do not call unavailable(null).
            throw f;

        } catch (Throwable f) {
            getServletContext().log("StandardWrapper.Throwable", f);
            instanceSupport.fireInstanceEvent(AFTER_INIT_EVENT,servlet, f);
            // If the servlet wanted to be unavailable it would have
            // said so, so do not call unavailable(null).
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_INIT_EXCEPTION), getName());
            throw new ServletException(msg, f);
        }
    }


    // START IASRI 4665318
    /**
     * Wrapper for the service method on the actual servlet
     * @param request The request sent
     * @param response
     * @param serv The servlet to process
     * @throws IOException
     * @throws ServletException
     * @see Servlet#service(ServletRequest, ServletResponse) 
     */
    void service(ServletRequest request, ServletResponse response,
                 Servlet serv)
             throws IOException, ServletException {

        InstanceSupport supp = getInstanceSupport();

        try {
            supp.fireInstanceEvent(BEFORE_SERVICE_EVENT,
                                   serv, request, response);
            if (!isAsyncSupported()) {
                RequestFacadeHelper reqFacHelper =
                    RequestFacadeHelper.getInstance(request);
                if (reqFacHelper != null) {
                    reqFacHelper.disableAsyncSupport();
                }
            }
            if ((request instanceof HttpServletRequest) &&
                (response instanceof HttpServletResponse)) {
                    
                if ( SecurityUtil.executeUnderSubjectDoAs() ){
                    final ServletRequest req = request;
                    final ServletResponse res = response;
                    Principal principal = 
                        ((HttpServletRequest) req).getUserPrincipal();

                    Object[] serviceType = new Object[2];
                    serviceType[0] = req;
                    serviceType[1] = res;
                    
                    SecurityUtil.doAsPrivilege("service",
                                               serv,
                                               classTypeUsedInService, 
                                               serviceType,
                                               principal);                                                   
                } else {
                    RequestTraceSpan span = null;
                    if (requestTracing.isRequestTracingEnabled()) {
                        if (serv instanceof ServletContainer) {
                            span = constructWebServiceRequestSpan((HttpServletRequest) request);
                        } else if (serv instanceof Servlet) {
                            span = constructServletRequestSpan((HttpServletRequest) request, serv);
                        }
                    }
                    
                    try {
                        if(isJspServlet) {
                            isInSuppressFFNFThread.set(true);
                        }
                        serv.service((HttpServletRequest) request, (HttpServletResponse) response);
                    }
                    finally {
                        String applicationName = openTracing.getApplicationName(
                                Globals.getDefaultBaseServiceLocator().getService(InvocationManager.class));
                                                
                        if (openTracing.getTracer(applicationName).activeSpan() != null) {
                            // Presumably held open by return being handled by another thread
                            openTracing.getTracer(applicationName).activeSpan().setTag(
                                    Tags.HTTP_STATUS.getKey(), 
                                    Integer.toString(((HttpServletResponse) response).getStatus()));
                            openTracing.getTracer(applicationName).activeSpan().finish();
                        }
                        
                        if (requestTracing.isRequestTracingEnabled() && span != null) {
                            span.addSpanTag("ResponseStatus", Integer.toString(
                                    ((HttpServletResponse) response).getStatus()));
                            requestTracing.traceSpan(span);
                        }
                        
                        isInSuppressFFNFThread.set(false);
                    }
                }
            } else {
                serv.service(request, response);
            }
            supp.fireInstanceEvent(AFTER_SERVICE_EVENT,
                                   serv, request, response);
        } catch (IOException e) {
            // Set response status before firing event, see IT 10022
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse)response).setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            supp.fireInstanceEvent(AFTER_SERVICE_EVENT,
                                   serv, request, response, e);
            throw e;
        } catch (ServletException e) {
            // Set response status before firing event, see IT 10022
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse)response).setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            supp.fireInstanceEvent(AFTER_SERVICE_EVENT,
                                   serv, request, response, e);
            throw e;
        } catch (RuntimeException e) {
            // Set response status before firing event, see IT 10022
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse)response).setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            supp.fireInstanceEvent(AFTER_SERVICE_EVENT,
                                   serv, request, response, e);
            throw e;
        } catch (Error e) {
            // Set response status before firing event, see IT 10022
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse)response).setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            supp.fireInstanceEvent(AFTER_SERVICE_EVENT,
                                   serv, request, response, e);
            throw e;
        } catch (Throwable e) {
            // Set response status before firing event, see IT 10022
            ((HttpServletResponse)response).setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            supp.fireInstanceEvent(AFTER_SERVICE_EVENT,
                                   serv, request, response, e);
            throw new ServletException(rb.getString(LogFacade.SERVLET_EXECUTION_EXCEPTION), e);
        }

    }
    // END IASRI 4665318


    private RequestTraceSpan constructWebServiceRequestSpan(HttpServletRequest httpServletRequest) {
        RequestTraceSpan span = new RequestTraceSpan("processWebserviceRequest");
        span.addSpanTag("URL", httpServletRequest.getRequestURL().toString());

        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            span.addSpanTag(headerName, Collections.list(httpServletRequest.getHeaders(headerName)).toString());
        }
        span.addSpanTag("Method", httpServletRequest.getMethod());

        return span;
    }

    private RequestTraceSpan constructServletRequestSpan(HttpServletRequest httpServletRequest, Servlet serv) {
        RequestTraceSpan span  = new RequestTraceSpan("processServletRequest");
        span.addSpanTag("URL", httpServletRequest.getRequestURL().toString());

        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            span.addSpanTag(headerName, Collections.list(httpServletRequest.getHeaders(headerName)).toString());
        }

        span.addSpanTag("Method",httpServletRequest.getMethod());
        span.addSpanTag("QueryString", httpServletRequest.getQueryString());
        span.addSpanTag("Class",serv.getClass().getCanonicalName());

        return span;
    }


    /**
     * Remove the specified initialization parameter from this servlet.
     *
     * @param name Name of the initialization parameter to remove
     */
    @Override
    public void removeInitParameter(String name) {

        synchronized (parameters) {
            parameters.remove(name);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("removeInitParameter", name);
        }
    }


    /**
     * Remove a listener no longer interested in InstanceEvents.
     *
     * @param listener The listener to remove
     */
    @Override
    public void removeInstanceListener(InstanceListener listener) {

        instanceSupport.removeInstanceListener(listener);

    }


    /**
     * Remove a mapping associated with the wrapper.
     *
     * @param mapping The pattern to remove
     */
    @Override
    public void removeMapping(String mapping) {

        synchronized (mappings) {
            mappings.remove(mapping);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("removeMapping", mapping);
        }
    }


    /**
     * Remove any security role reference for the specified role name.
     *
     * @param name Security role used within this servlet to be removed
     */
    @Override
    public void removeSecurityReference(String name) {

        synchronized (references) {
            references.remove(name);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("removeSecurityReference", name);
        }
    }


    /**
     * Return a String representation of this component.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        if (getParent() != null) {
            sb.append(getParent().toString());
            sb.append(".");
        }
        sb.append("StandardWrapper[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

    }


    /**
     * Process an UnavailableException, marking this servlet as unavailable
     * for the specified amount of time.
     *
     * @param unavailable The exception that occurred, or <code>null</code>
     *  to mark this servlet as permanently unavailable
     */
    @Override
    public void unavailable(UnavailableException unavailable) {
        String msg = MessageFormat.format(rb.getString(LogFacade.MARK_SERVLET_UNAVAILABLE), getName());
        getServletContext().log(msg);
        if (unavailable == null)
            setAvailable(Long.MAX_VALUE);
        else if (unavailable.isPermanent())
            setAvailable(Long.MAX_VALUE);
        else {
            int unavailableSeconds = unavailable.getUnavailableSeconds();
            if (unavailableSeconds <= 0)
                unavailableSeconds = 60;        // Arbitrary default
            setAvailable(System.currentTimeMillis() +
                         (unavailableSeconds * 1000L));
        }

    }


    /**
     * Unload all initialized instances of this servlet, after calling the
     * <code>destroy()</code> method for each instance.  This can be used,
     * for example, prior to shutting down the entire servlet engine, or
     * prior to reloading all of the classes from the Loader associated with
     * our Loader's repository.
     *
     * @exception ServletException if an exception is thrown by the
     *  destroy() method
     */
    @Override
    public synchronized void unload() throws ServletException {

        // Nothing to do if we have never loaded the instance
        if (!singleThreadModel && (instance == null))
            return;
        unloading = true;

        // Loaf a while if the current instance is allocated
        // (possibly more than once if non-STM)
        if (countAllocated.get() > 0) {
            int nRetries = 0;
            long delay = unloadDelay / 20;
            while ((nRetries < 21) && (countAllocated.get() > 0)) {
                if ((nRetries % 10) == 0) {
                    if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, LogFacade.WAITING_INSTANCE_BE_DEALLOCATED, new Object[] {countAllocated.toString(),
                                instance.getClass().getName()});
                    }
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    // Ignore
                }
                nRetries++;
            }
        }

        ClassLoader oldCtxClassLoader =
            Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = instance.getClass().getClassLoader();

        // Call the servlet destroy() method
        try {
            instanceSupport.fireInstanceEvent(BEFORE_DESTROY_EVENT, instance);

            Thread.currentThread().setContextClassLoader(classLoader);
            // START SJS WS 7.0 6236329
            //if( System.getSecurityManager() != null) {
            if ( SecurityUtil.executeUnderSubjectDoAs() ){
            // END OF SJS WS 7.0 6236329
                SecurityUtil.doAsPrivilege("destroy", instance);
                SecurityUtil.remove(instance);                           
            } else {
                instance.destroy();
            }

            instanceSupport.fireInstanceEvent(AFTER_DESTROY_EVENT, instance);
        } catch (Throwable t) {
            instanceSupport.fireInstanceEvent(AFTER_DESTROY_EVENT, instance, t);
            instance = null;
            instancePool = null;
            nInstances = 0;
            if (notifyContainerListeners) {
                fireContainerEvent("unload", this);
            }
            unloading = false;
            String msg = MessageFormat.format(rb.getString(LogFacade.DESTROY_SERVLET_EXCEPTION), getName());
            throw new ServletException(msg, t);
        } finally {
            // restore the context ClassLoader
            Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
        }

        // Deregister the destroyed instance
        instance = null;

        if (singleThreadModel && (instancePool != null)) {
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                while (!instancePool.isEmpty()) {
                    // START SJS WS 7.0 6236329
                    //if( System.getSecurityManager() != null) {
                    if ( SecurityUtil.executeUnderSubjectDoAs() ){
                    // END OF SJS WS 7.0 6236329
                        SecurityUtil.doAsPrivilege("destroy",
                                                   instancePool.pop());
                        SecurityUtil.remove(instance);                           
                    } else {
                        instancePool.pop().destroy();
                    }
                }
            } catch (Throwable t) {
                instancePool = null;
                nInstances = 0;
                unloading = false;
                if (notifyContainerListeners) {
                    fireContainerEvent("unload", this);
                }
                String msg = MessageFormat.format(rb.getString(LogFacade.DESTROY_SERVLET_EXCEPTION), getName());
                throw new ServletException(msg, t);
            } finally {
                // restore the context ClassLoader
                Thread.currentThread().setContextClassLoader
                    (oldCtxClassLoader);
            }
            instancePool = null;
            nInstances = 0;
        }

        singleThreadModel = false;

        unloading = false;
   
        if (notifyContainerListeners) {
            fireContainerEvent("unload", this);
        }
    }


    // -------------------------------------------------- ServletConfig Methods


    /**
     * Return the initialization parameter value for the specified name,
     * if any; otherwise return <code>null</code>.
     *
     * @param name Name of the initialization parameter to retrieve
     */
    @Override
    public String getInitParameter(String name) {
        return findInitParameter(name);
    }


    public Map<String, String> getInitParameters() {
        synchronized (parameters) {
            return Collections.unmodifiableMap(parameters);
        }
    }


    /**
     * Return the set of initialization parameter names defined for this
     * servlet.  If none are defined, an empty Enumeration is returned.
     */
    @Override
    public Enumeration<String> getInitParameterNames() {
        synchronized (parameters) {
            return (new Enumerator<String>(parameters.keySet()));
        }
    }


    /**
     * Return the servlet context with which this servlet is associated.
     */
    @Override
    public ServletContext getServletContext() {
        if (parent == null)
            return (null);
        else if (!(parent instanceof Context))
            return (null);
        else
            return (((Context) parent).getServletContext());
    }


    /**
     * Return the name of this servlet.
     */
    @Override
    public String getServletName() {
        return (getName());
    }

    /**
     * Gets how long it took for the servlet to be loaded.
     * @return length in milliseconds
     */
    public long getLoadTime() {
        return loadTime;
    }

    public void setLoadTime(long loadTime) {
        this.loadTime = loadTime;
    }

    /**
     * Gets how long it took for the servlet class to be loaded.
     * @return length in milliseconds
     */
    public int getClassLoadTime() {
        return classLoadTime;
    }

    // -------------------------------------------------------- Package Methods


    // -------------------------------------------------------- Private Methods


    /**
     * Add a default Mapper implementation if none have been configured
     * explicitly.
     *
     * @param mapperClass Java class name of the default Mapper
     */
    protected void addDefaultMapper(String mapperClass) {

        // No need for a default Mapper on a Wrapper

    }


    /**
     * Return <code>true</code> if the specified class name represents a
     * container provided servlet class that should be loaded by the
     * server class loader.
     *
     * @param classname Name of the class to be checked
     */
    private boolean isContainerProvidedServlet(String classname) {

        if (classname.startsWith("org.apache.catalina.")) {
            return (true);
        }
        try {
            Class<?> clazz =
                this.getClass().getClassLoader().loadClass(classname);
            return (ContainerServlet.class.isAssignableFrom(clazz));
        } catch (Throwable t) {
            return (false);
        }

    }


    /**
     * Return <code>true</code> if loading this servlet is allowed.
     */
    private boolean isServletAllowed(Object servlet) {

        if (servlet instanceof ContainerServlet) {
            if (((Context) getParent()).getPrivileged()
                || (servlet.getClass().getName().equals
                    ("org.apache.catalina.servlets.InvokerServlet"))) {
                return (true);
            } else {
                return (false);
            }
        }

        return (true);

    }


    /**
     * Log the abbreviated name of this Container for logging messages.
     * @return <code>StandardWrapper[&lt;parentName | null&gt; : nameOfContainer]</code>
     */
    @Override
    protected String logName() {

        StringBuilder sb = new StringBuilder("StandardWrapper[");
        if (getParent() != null)
            sb.append(getParent().getName());
        else
            sb.append("null");
        sb.append(':');
        sb.append(getName());
        sb.append(']');
        return (sb.toString());

    }


    private Method[] getAllDeclaredMethods(Class<?> c) {

        if (c.equals(javax.servlet.http.HttpServlet.class)) {
            return null;
        }

        Method[] parentMethods = getAllDeclaredMethods(c.getSuperclass());

        Method[] thisMethods = c.getDeclaredMethods();
        if (thisMethods.length == 0) {
            return parentMethods;
        }

        if ((parentMethods != null) && (parentMethods.length > 0)) {
            Method[] allMethods =
                new Method[parentMethods.length + thisMethods.length];
	    System.arraycopy(parentMethods, 0, allMethods, 0,
                             parentMethods.length);
	    System.arraycopy(thisMethods, 0, allMethods, parentMethods.length,
                             thisMethods.length);

	    thisMethods = allMethods;
	}

	return thisMethods;
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Start this component, pre-loading the servlet if the load-on-startup
     * value is set appropriately.
     *
     * @exception LifecycleException if a fatal error occurs during startup
     */
    @Override
    public void start() throws LifecycleException {
    
        // Send j2ee.state.starting notification 
        if (this.getObjectName() != null) {
            Notification notification = new Notification("j2ee.state.starting", this, sequenceNumber++);
            sendNotification(notification);
        }
        
        // Start up this component
        super.start();

        if( oname != null )
            registerJMX((StandardContext)getParent());
        
        // Load and initialize an instance of this servlet if requested
        // MOVED TO StandardContext START() METHOD

        setAvailable(0L);
        
        // Send j2ee.state.running notification 
        if (this.getObjectName() != null) {
            Notification notification = 
                new Notification("j2ee.state.running", this, sequenceNumber++);
            sendNotification(notification);
        }

    }


    /**
     * Stop this component, gracefully shutting down the servlet if it has
     * been initialized.
     *
     * @exception LifecycleException if a fatal error occurs during shutdown
     */
    @Override
    public void stop() throws LifecycleException {

        setAvailable(Long.MAX_VALUE);
        
        // Send j2ee.state.stopping notification 
        if (this.getObjectName() != null) {
            Notification notification = 
                new Notification("j2ee.state.stopping", this, sequenceNumber++);
            sendNotification(notification);
        }
        
        // Shut down our servlet instance (if it has been initialized)
        try {
            unload();
        } catch (ServletException e) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_UNLOAD_EXCEPTION), getName());
            getServletContext().log(msg, e);
        }

        // Shut down this component
        super.stop();

        // Send j2ee.state.stoppped notification 
        if (this.getObjectName() != null) {
            Notification notification = 
                new Notification("j2ee.state.stopped", this, sequenceNumber++);
            sendNotification(notification);
        }
        
        if( oname != null ) {
            
            // Send j2ee.object.deleted notification 
            Notification notification = 
                new Notification("j2ee.object.deleted", this, sequenceNumber++);
            sendNotification(notification);
        }

    }

    protected void registerJMX(StandardContext ctx) {

        String parentName = ctx.getEncodedPath();
        parentName = ("".equals(parentName)) ? "/" : parentName;

        String hostName = ctx.getParent().getName();
        hostName = (hostName==null) ? "DEFAULT" : hostName;

        String domain = ctx.getDomain();

        String webMod= "//" + hostName + parentName;
        String onameStr = domain + ":j2eeType=Servlet,name=" + getName() +
                          ",WebModule=" + webMod + ",J2EEApplication=" +
                          ctx.getJ2EEApplication() + ",J2EEServer=" +
                          ctx.getJ2EEServer();
        if (isOSGi()) {
            onameStr += ",osgi=true";
        }

        try {
            oname=new ObjectName(onameStr);
            controller=oname;
            
            // Send j2ee.object.created notification 
            if (this.getObjectName() != null) {
                Notification notification = new Notification( "j2ee.object.created", this, sequenceNumber++);
                sendNotification(notification);
            }
        } catch( Exception ex ) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO,
                    "Error registering servlet with jmx " + this, ex);
            }
        }

        if (isJspServlet) {
            // Register JSP monitoring mbean
            onameStr = domain + ":type=JspMonitor,name=" + getName()
                       + ",WebModule=" + webMod
                       + ",J2EEApplication=" + ctx.getJ2EEApplication()
                       + ",J2EEServer=" + ctx.getJ2EEServer();
            try {
                jspMonitorON = new ObjectName(onameStr);
            } catch( Exception ex ) {
                if (log.isLoggable(Level.INFO)) {
                    log.log(Level.INFO,
                        "Error registering JSP monitoring with jmx " +
                        instance, ex);
                }
            }
        }

    }

    /**
     * Sends a notification to anything listening via JMX.
     * <p>
     * This function does not have anything to do with the notification service in Payara.
     * @param notification
     * @see javax.management.NotificationBroadcasterSupport
     * @see fish.payara.nucleus.notification.NotificationService
     */
    public void sendNotification(Notification notification) {

        if (broadcaster == null) {
            broadcaster = ((StandardEngine)getParent().getParent().getParent()).getService().getBroadcaster();
        }
        if (broadcaster != null) {
            broadcaster.sendNotification(notification);
        }
        return;
    }
    

    // ------------------------------------------------------------- Attributes
        
    /** @return false */
    public boolean isEventProvider() {
        return false;
    }
    
    /** @return false */
    public boolean isStateManageable() {
        return false;
    }
    
    /** @return false */
    public boolean isStatisticsProvider() {
        return false;
    }

    private static class NotFoundErrorSupressionFilter implements java.util.logging.Filter {
        private final java.util.logging.Filter oldFilter;

        public NotFoundErrorSupressionFilter(java.util.logging.Filter oldFilter) {
            this.oldFilter = oldFilter;
        }

        @Override
        public boolean isLoggable(LogRecord record) {
            boolean rv = true;
            if(isInSuppressFFNFThread.get()) {
                rv = !record.getMessage().startsWith("PWC6117: File");
            }
            if(oldFilter != null) {
                rv &= oldFilter.isLoggable(record);
            }
            return rv;
        }
    }
}
