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
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.ServerInfo;

import javax.naming.directory.DirContext;
import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Standard implementation of <code>ServletContext</code> that represents
 * a web application's execution environment.  An instance of this class is
 * associated with each instance of <code>StandardContext</code>.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.15.2.1 $ $Date: 2008/04/17 18:37:06 $
 */
public class ApplicationContext implements ServletContext {

    // ----------------------------------------------------------- Constructors

    private static final Logger log = LogFacade.getLogger();
    private static final ResourceBundle rb = log.getResourceBundle();


    /**
     * Construct a new instance of this class, associated with the specified
     * Context instance.
     *
     * @param context The associated Context instance
     */
    public ApplicationContext(StandardContext context) {
        super();
        this.context = context;

        setAttribute("com.sun.faces.useMyFaces",
                     Boolean.valueOf(context.isUseMyFaces()));
    }

    public StandardContext getStandardContext() {
        return context;
    }


    // ----------------------------------------------------- Class Variables

    // START PWC 1.2
    /*
    private static final SecurityPermission GET_UNWRAPPED_CONTEXT_PERMISSION =
        new SecurityPermission("getUnwrappedContext");
    */
    // END PWC 1.2


    // ----------------------------------------------------- Instance Variables

    /**
     * The context attributes for this context.
     */
    private Map<String, Object> attributes =
        new ConcurrentHashMap<String, Object>();

    /**
     * List of read only attributes for this context.
     */
    private HashMap<String, String> readOnlyAttributes =
        new HashMap<String, String>();

    /**
     * Lock for synchronizing attributes and readOnlyAttributes
     */
    private Object attributesLock = new Object();

    /**
     * The Context instance with which we are associated.
     */
    private StandardContext context = null;

    /**
     * Empty String collection to serve as the basis for empty enumerations.
     * <strong>DO NOT ADD ANY ELEMENTS TO THIS COLLECTION!</strong>
     */
    private static final List<String> emptyString = Collections.emptyList();

    /**
     * Empty Servlet collection to serve as the basis for empty enumerations.
     * <strong>DO NOT ADD ANY ELEMENTS TO THIS COLLECTION!</strong>
     */
    private static final List<Servlet> emptyServlet = Collections.emptyList();

    /**
     * The facade around this object.
     */
    private ServletContext facade = new ApplicationContextFacade(this);

    /**
     * The merged context initialization parameters for this Context.
     */
    private ConcurrentMap<String, String> parameters =
        new ConcurrentHashMap<String, String>();

    private boolean isRestricted;

    
    // --------------------------------------------------------- Public Methods

    /**
     * Return the resources object that is mapped to a specified path.
     * The path must begin with a "/" and is interpreted as relative to the
     * current context root.
     */
    public DirContext getResources() {
        return context.getResources();
    }


    // ------------------------------------------------- ServletContext Methods

    /**
     * Return the value of the specified context attribute, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the context attribute to return
     */
    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Return an enumeration of the names of the context attributes
     * associated with this context.
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        return new Enumerator<String>(attributes.keySet(), true);
    }

    /**
     * Returns the context path of the web application.
     *
     * <p>The context path is the portion of the request URI that is used
     * to select the context of the request. The context path always comes
     * first in a request URI. The path starts with a "/" character but does
     * not end with a "/" character. For servlets in the default (root)
     * context, this method returns "".
     *
     * <p>It is possible that a servlet container may match a context by
     * more than one context path. In such cases the
     * {@link javax.servlet.http.HttpServletRequest#getContextPath()}
     * will return the actual context path used by the request and it may
     * differ from the path returned by this method.
     * The context path returned by this method should be considered as the
     * prime or preferred context path of the application.
     *
     * @see javax.servlet.http.HttpServletRequest#getContextPath()
     */
    @Override
    public String getContextPath() {
        return context.getPath();
    }

    /**
     * Return a <code>ServletContext</code> object that corresponds to a
     * specified URI on the server.  This method allows servlets to gain
     * access to the context for various parts of the server, and as needed
     * obtain <code>RequestDispatcher</code> objects or resources from the
     * context.  The given path must be absolute (beginning with a "/"),
     * and is interpreted based on our virtual host's document root.
     *
     * @param uri Absolute URI of a resource on the server
     */
    @Override
    public ServletContext getContext(String uri) {
        return context.getContext(uri);
    }

    /**
     * Return the value of the specified initialization parameter, or
     * <code>null</code> if this parameter does not exist.
     *
     * @param name Name of the initialization parameter to retrieve
     */
    @Override
    public String getInitParameter(final String name) {
        return parameters.get(name);
    }

    /**
     * Return the names of the context's initialization parameters, or an
     * empty enumeration if the context has no initialization parameters.
     */
    @Override
    public Enumeration<String> getInitParameterNames() {
        return (new Enumerator<String>(parameters.keySet()));
    }

    /**
     * @return true if the context initialization parameter with the given
     * name and value was set successfully on this ServletContext, and false
     * if it was not set because this ServletContext already contains a
     * context initialization parameter with a matching name
     */
    @Override
    public boolean setInitParameter(String name, String value) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }

        return parameters.putIfAbsent(name, value) == null;
    }

    /**
     * Return the major version of the Java Servlet API that we implement.
     */
    @Override
    public int getMajorVersion() {
        return (Constants.MAJOR_VERSION);
    }

    /**
     * Return the minor version of the Java Servlet API that we implement.
     */
    @Override
    public int getMinorVersion() {
        return (Constants.MINOR_VERSION);
    }

    /**
     * Gets the major version of the Servlet specification that the
     * application represented by this ServletContext is based on.
     */
    @Override
    public int getEffectiveMajorVersion() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getEffectiveMajorVersion();
    }
        
    /**
     * Gets the minor version of the Servlet specification that the
     * application represented by this ServletContext is based on.
     */
    @Override
    public int getEffectiveMinorVersion() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getEffectiveMinorVersion();
    }

    /**
     * Return the MIME type of the specified file, or <code>null</code> if
     * the MIME type cannot be determined.
     *
     * @param file Filename for which to identify a MIME type
     */
    @Override
    public String getMimeType(String file) {
        return context.getMimeType(file);
    }

    /**
     * Return a <code>RequestDispatcher</code> object that acts as a
     * wrapper for the named servlet.
     *
     * @param name Name of the servlet for which a dispatcher is requested
     */
    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return context.getNamedDispatcher(name);
    }

    /**
     * @param path The virtual path to be translated
     *
     * @return the real path corresponding to the given virtual path, or
     * <code>null</code> if the container was unable to perform the
     * translation
     */
    @Override
    public String getRealPath(String path) {
        return context.getRealPath(path);
    }

    /**
     * Return a <code>RequestDispatcher</code> instance that acts as a
     * wrapper for the resource at the given path.  The path must begin
     * with a "/" or be empty, and is interpreted as relative to the current
     * context root.
     *
     * @param path The path to the desired resource.
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return context.getRequestDispatcher(path);
    }

    /**
     * Return the URL to the resource that is mapped to a specified path.
     * The path must begin with a "/" and is interpreted as relative to the
     * current context root.
     *
     * @param path The path to the desired resource
     *
     * @exception MalformedURLException if the path is not given
     *  in the correct form
     */
    @Override
    public URL getResource(String path)
        throws MalformedURLException {
        return context.getResource(path);
    }

    /**
     * Return the requested resource as an <code>InputStream</code>.  The
     * path must be specified according to the rules described under
     * <code>getResource</code>.  If no such resource can be identified,
     * return <code>null</code>.
     *
     * @param path The path to the desired resource.
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        return context.getResourceAsStream(path);
    }

    /**
     * Return a Set containing the resource paths of resources member of the
     * specified collection. Each path will be a String starting with
     * a "/" character. The returned set is immutable.
     *
     * @param path Collection path
     */
    @Override
    public Set<String> getResourcePaths(String path) {
        return context.getResourcePaths(path);
    }

    /**
     * Return the name and version of the servlet container.
     */
    @Override
    public String getServerInfo() {
        return (ServerInfo.getServerInfo());
    }

    /**
     * @deprecated As of Java Servlet API 2.1, with no direct replacement.
     */
    @Override
    public Servlet getServlet(String name) {
        return (null);
    }

    /**
     * Return the display name of this web application.
     */
    @Override
    public String getServletContextName() {
        return (context.getDisplayName());
    }

    /**
     * @deprecated As of Java Servlet API 2.1, with no direct replacement.
     */
    @Override
    public Enumeration<String> getServletNames() {
        return (new Enumerator<String>(emptyString));
    }

    /**
     * @deprecated As of Java Servlet API 2.1, with no direct replacement.
     */
    @Override
    public Enumeration<Servlet> getServlets() {
        return (new Enumerator<Servlet>(emptyServlet));
    }

    /**
     * Writes the specified message to a servlet log file.
     *
     * @param message Message to be written
     */
    @Override
    public void log(String message) {
        context.log(message);
    }

    /**
     * Writes the specified exception and message to a servlet log file.
     *
     * @param exception Exception to be reported
     * @param message Message to be written
     *
     * @deprecated As of Java Servlet API 2.1, use
     *  <code>log(String, Throwable)</code> instead
     */
    @Override
    public void log(Exception exception, String message) {   
        context.log(exception, message);
    }

    /**
     * Writes the specified message and exception to a servlet log file.
     *
     * @param message Message to be written
     * @param throwable Exception to be reported
     */
    @Override
    public void log(String message, Throwable throwable) {
        context.log(message, throwable);
    }

    /**
     * Remove the context attribute with the specified name, if any.
     *
     * @param name Name of the context attribute to be removed
     */
    @Override
    public void removeAttribute(String name) {
        Object value = null;
        boolean found = false;

        // Remove the specified attribute
        synchronized (attributesLock) {
            // Check for read only attribute
            if (readOnlyAttributes.containsKey(name))
                return;
            value = attributes.remove(name);
            if (value == null)
                return;
        }

        // Notify interested application event listeners
        List<EventListener> listeners = context.getApplicationEventListeners();
        if (listeners.isEmpty()) {
            return;
        }

        ServletContextAttributeEvent event =
            new ServletContextAttributeEvent(context.getServletContext(),
                                             name, value);
        Iterator<EventListener> iter = listeners.iterator();
        while (iter.hasNext()) {
            EventListener eventListener = iter.next();
            if (!(eventListener instanceof ServletContextAttributeListener)) {
                continue;
            }
            ServletContextAttributeListener listener =
                (ServletContextAttributeListener) eventListener;
            try {
                context.fireContainerEvent(
                    ContainerEvent.BEFORE_CONTEXT_ATTRIBUTE_REMOVED,
                    listener);
                listener.attributeRemoved(event);
                context.fireContainerEvent(
                    ContainerEvent.AFTER_CONTEXT_ATTRIBUTE_REMOVED,
                    listener);
            } catch (Throwable t) {
                context.fireContainerEvent(
                    ContainerEvent.AFTER_CONTEXT_ATTRIBUTE_REMOVED,
                    listener);
                // FIXME - should we do anything besides log these?
                log.log(Level.WARNING,
                        LogFacade.ATTRIBUTES_EVENT_LISTENER_EXCEPTION, t);
            }
        }

    }

    /**
     * Bind the specified value with the specified context attribute name,
     * replacing any existing value for that name.
     *
     * @param name Attribute name to be bound
     * @param value New attribute value to be bound
     */
    @Override
    public void setAttribute(String name, Object value) {

        // Name cannot be null
        if (name == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.ILLEGAL_ARGUMENT_EXCEPTION));

        // Null value is the same as removeAttribute()
        if (value == null) {
            removeAttribute(name);
            return;
        }

        Object oldValue = null;
        boolean replaced = false;

        // Add or replace the specified attribute
        synchronized (attributesLock) {
            // Check for read only attribute
            if (readOnlyAttributes.containsKey(name))
                return;
            oldValue = attributes.get(name);
            if (oldValue != null)
                replaced = true;
            attributes.put(name, value);
        }
        
        if (name.equals(Globals.CLASS_PATH_ATTR) ||
                name.equals(Globals.JSP_TLD_URI_TO_LOCATION_MAP)) {
            setAttributeReadOnly(name);
        }
        
        // Notify interested application event listeners
        List<EventListener> listeners =
            context.getApplicationEventListeners();
        if (listeners.isEmpty()) {
            return;
        }

        ServletContextAttributeEvent event = null;
        if (replaced) {
            event =
                new ServletContextAttributeEvent(context.getServletContext(),
                                                 name, oldValue);
        } else {
            event =
                new ServletContextAttributeEvent(context.getServletContext(),
                                                 name, value);
        }

        Iterator<EventListener> iter = listeners.iterator(); 
        while (iter.hasNext()) {
            EventListener eventListener = iter.next();
            if (!(eventListener instanceof ServletContextAttributeListener)) {
                continue;
	    }
            ServletContextAttributeListener listener =
                (ServletContextAttributeListener) eventListener;
            try {
                if (replaced) {
                    context.fireContainerEvent(
                        ContainerEvent.BEFORE_CONTEXT_ATTRIBUTE_REPLACED,
                        listener);
                    listener.attributeReplaced(event);
                    context.fireContainerEvent(
                        ContainerEvent.AFTER_CONTEXT_ATTRIBUTE_REPLACED,
                        listener);
                } else {
                    context.fireContainerEvent(
                        ContainerEvent.BEFORE_CONTEXT_ATTRIBUTE_ADDED,
                        listener);
                    listener.attributeAdded(event);
                    context.fireContainerEvent(
                        ContainerEvent.AFTER_CONTEXT_ATTRIBUTE_ADDED,
                        listener);
                }
            } catch (Throwable t) {
                if (replaced) {
                    context.fireContainerEvent(
                        ContainerEvent.AFTER_CONTEXT_ATTRIBUTE_REPLACED,
                        listener);
                } else {
                    context.fireContainerEvent(
                        ContainerEvent.AFTER_CONTEXT_ATTRIBUTE_ADDED,
                        listener);
                }
                // FIXME - should we do anything besides log these?
                log.log(Level.WARNING, LogFacade.ATTRIBUTES_EVENT_LISTENER_EXCEPTION, t);
            }
        }
    }

    /*
     * Adds the servlet with the given name and class name to this
     * servlet context.
     */
    @Override
    public ServletRegistration.Dynamic addServlet(
            String servletName, String className) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.addServlet(servletName, className);
    }

    /*
     * Registers the given servlet instance with this ServletContext
     * under the given <tt>servletName</tt>.
     */
    @Override
    public ServletRegistration.Dynamic addServlet(
            String servletName, Servlet servlet) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.addServlet(servletName, servlet);
    }

    /*
     * Adds the servlet with the given name and class type to this
     * servlet context.
     */
    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Class <? extends Servlet> servletClass) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.addServlet(servletName, servletClass);
    }

    /**
     * Instantiates the given Servlet class and performs any required
     * resource injection into the new Servlet instance before returning
     * it.
     */
    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz)
            throws ServletException {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.createServlet(clazz);
    }

    /**
     * Gets the ServletRegistration corresponding to the servlet with the
     * given <tt>servletName</tt>.
     */
    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getServletRegistration(servletName);
    }

    /**
     * Gets a Map of the ServletRegistration objects corresponding to all
     * currently registered servlets.
     */
    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getServletRegistrations();
    }

    /**
     * Adds the filter with the given name and class name to this servlet
     * context.
     */
    @Override
    public FilterRegistration.Dynamic addFilter(
            String filterName, String className) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.addFilter(filterName, className);
    }
    
    /*
     * Registers the given filter instance with this ServletContext
     * under the given <tt>filterName</tt>.
     */
    @Override
    public FilterRegistration.Dynamic addFilter(
            String filterName, Filter filter) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.addFilter(filterName, filter);
    }

    /**
     * Adds the filter with the given name and class type to this servlet
     * context.
     */
    @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            Class <? extends Filter> filterClass) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.addFilter(filterName, filterClass);
    }
    
    /**
     * Instantiates the given Filter class and performs any required
     * resource injection into the new Filter instance before returning
     * it.
     */
    @Override
    public <T extends Filter> T createFilter(Class<T> clazz)
            throws ServletException {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.createFilter(clazz);
    }

    /**
     * Gets the FilterRegistration corresponding to the filter with the
     * given <tt>filterName</tt>.
     */
    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getFilterRegistration(filterName);
    }

    /**
     * Gets a Map of the FilterRegistration objects corresponding to all
     * currently registered filters.
     */
    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getFilterRegistrations();
    }

    /**
     * Gets the <tt>SessionCookieConfig</tt> object through which various
     * properties of the session tracking cookies created on behalf of this
     * <tt>ServletContext</tt> may be configured.
     */
    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getSessionCookieConfig();        
    }
  
    /**
     * Sets the session tracking modes that are to become effective for this
     * <tt>ServletContext</tt>.
     */
    @Override
    public void setSessionTrackingModes(
            Set<SessionTrackingMode> sessionTrackingModes) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        context.setSessionTrackingModes(sessionTrackingModes);
    }

    /**
     * Gets the session tracking modes that are supported by default for this
     * <tt>ServletContext</tt>.
     *
     * @return set of the session tracking modes supported by default for
     * this <tt>ServletContext</tt>
     */
    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getDefaultSessionTrackingModes();
    }

    /**
     * Gets the session tracking modes that are in effect for this
     * <tt>ServletContext</tt>.
     *
     * @return set of the session tracking modes in effect for this
     * <tt>ServletContext</tt>
     */
    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getEffectiveSessionTrackingModes();
    }

    /**
     * Adds the listener with the given class name to this ServletContext.
     */
    @Override
    public void addListener(String className) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        context.addListener(className);
    }

    /**
     * Adds the given listener to this ServletContext.
     */
    @Override
    public <T extends EventListener> void addListener(T t) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        context.addListener(t);
    }

    /**
     * Adds a listener of the given class type to this ServletContext.
     */
    @Override
    public void addListener(Class <? extends EventListener> listenerClass) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        context.addListener(listenerClass);
    }

    /**
     * Instantiates the given EventListener class and performs any
     * required resource injection into the new EventListener instance
     * before returning it.
     */
    @Override
    public <T extends EventListener> T createListener(Class<T> clazz)
            throws ServletException {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.createListener(clazz);
    }

    /**
     * Gets the <code>&lt;jsp-config&gt;</code> related configuration
     * that was aggregated from the <code>web.xml</code> and
     * <code>web-fragment.xml</code> descriptor files of the web application
     * represented by this ServletContext.
     */
    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getJspConfigDescriptor();
    }

    @Override
    public ClassLoader getClassLoader() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        context.declareRoles(roleNames);
    }

    @Override
    public String getVirtualServerName() {
        if (isRestricted) {
            throw new UnsupportedOperationException(
                    rb.getString(LogFacade.UNSUPPORTED_OPERATION_EXCEPTION));
        }
        return context.getVirtualServerName();
    }

    // START PWC 1.2
    /**
     * Gets the underlying StandardContext to which this ApplicationContext is
     * delegating.
     *
     * @return The underlying StandardContext
     */
    /*
    public StandardContext getUnwrappedContext() {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(GET_UNWRAPPED_CONTEXT_PERMISSION);
        }

        return this.context;        
    }
    */
    // END PWC 1.2


    // -------------------------------------------------------- Package Methods

    /**
     * Clear all application-created attributes.
     */
    void clearAttributes() {

        // Create list of attributes to be removed
        ArrayList<String> list = new ArrayList<String>();
        synchronized (attributesLock) {
            Iterator<String> iter = attributes.keySet().iterator();
            while (iter.hasNext()) {
                list.add(iter.next());
            }
        }

        // Remove application originated attributes
        // (read only attributes will be left in place)
        Iterator<String> keys = list.iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            removeAttribute(key);
        }        
    }
    
    /**
     * Return the facade associated with this ApplicationContext.
     */
    protected ServletContext getFacade() {
        return this.facade;
    }

    /**
     * Set an attribute as read only.
     */
    void setAttributeReadOnly(String name) {
        synchronized (attributesLock) {
            if (attributes.containsKey(name))
                readOnlyAttributes.put(name, name);
        }
    }

    void setRestricted(boolean isRestricted) {
        this.isRestricted = isRestricted;
    }

}
