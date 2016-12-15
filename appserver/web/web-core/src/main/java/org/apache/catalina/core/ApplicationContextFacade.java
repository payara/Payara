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

import org.apache.catalina.Globals;
import org.apache.catalina.LogFacade;
import org.apache.catalina.security.SecurityUtil;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facade object which masks the internal <code>ApplicationContext</code>
 * object from the web application.
 *
 * @author Remy Maucherat
 * @author Jean-Francois Arcand
 * @version $Revision: 1.7.6.1 $ $Date: 2008/04/17 18:37:06 $
 */

public final class ApplicationContextFacade
    implements ServletContext {

    private static final Logger log = LogFacade.getLogger();
        
    // ---------------------------------------------------------- Attributes
    /**
     * Cache Class object used for reflection.
     */
    private static HashMap<String, Class<?>[]> classCache = new HashMap<String, Class<?>[]>();
    
    static {
        Class<?>[] clazz = new Class[]{String.class};
        classCache.put("addFilter", new Class[]{String.class, String.class});
        classCache.put("addListener", clazz);
        classCache.put("addServlet", new Class[]{String.class, String.class});
        classCache.put("createFilter", new Class[]{Class.class});
        classCache.put("createListener", new Class[]{Class.class});
        classCache.put("createServlet", new Class[]{Class.class});
        classCache.put("declareRoles", new Class<?>[] {(new String[0]).getClass()});
        classCache.put("getAttribute", clazz);
        classCache.put("getContext", clazz);
        classCache.put("getFilterRegistration", clazz);
        classCache.put("getInitParameter", clazz);
        classCache.put("getMimeType", clazz);
        classCache.put("getNamedDispatcher", clazz);
        classCache.put("getRealPath", clazz);
        classCache.put("getResourcePaths", clazz);
        classCache.put("getResource", clazz);
        classCache.put("getResourceAsStream", clazz);
        classCache.put("getRequestDispatcher", clazz);
        classCache.put("getServlet", clazz);
        classCache.put("getServletRegistration", clazz);
        classCache.put("log", clazz);        
        classCache.put("removeAttribute", clazz);
        classCache.put("setAttribute", new Class[]{String.class, Object.class});
        classCache.put("setInitParameter", new Class[]{String.class, String.class});
        classCache.put("setSessionTrackingModes", new Class[]{Set.class});
    }
    
    /**
     * Cache method object.
     */
    private HashMap<String, Method> objectCache;
    
    
    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new instance of this class, associated with the specified
     * Context instance.
     *
     * @param context The associated Context instance
     */
    public ApplicationContextFacade(ApplicationContext context) {
        super();
        this.context = context;
        
        objectCache = new HashMap<String, Method>();
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Wrapped application context.
     */
    private ApplicationContext context = null;
    


    // ------------------------------------------------- ServletContext Methods

    @Override
    public String getContextPath() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getContextPath", null);
        } else {
            return context.getContextPath();
        }
    }


    @Override
    public ServletContext getContext(String uripath) {
        ServletContext theContext = null;
        if (SecurityUtil.isPackageProtectionEnabled()) {
            theContext = (ServletContext)
                doPrivileged("getContext", new Object[]{uripath});
        } else {
            theContext = context.getContext(uripath);
        }
        if ((theContext != null) &&
            (theContext instanceof ApplicationContext)){
            theContext = ((ApplicationContext)theContext).getFacade();
        }
        return (theContext);
    }


    @Override
    public int getMajorVersion() {
        return context.getMajorVersion();
    }


    @Override
    public int getMinorVersion() {
        return context.getMinorVersion();
    }


    /**
     * Gets the major version of the Servlet specification that the
     * application represented by this ServletContext is based on.
     */
    @Override
    public int getEffectiveMajorVersion() {
        return context.getEffectiveMajorVersion();
    }
    
    
    /**
     * Gets the minor version of the Servlet specification that the
     * application represented by this ServletContext is based on.
     */
    @Override
    public int getEffectiveMinorVersion() {
        return context.getEffectiveMinorVersion();
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public String getMimeType(String file) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String)doPrivileged("getMimeType", new Object[]{file});
        } else {
            return context.getMimeType(file);
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Set<String> getResourcePaths(String path) {
        if (SecurityUtil.isPackageProtectionEnabled()){
            return (Set<String>)doPrivileged("getResourcePaths",
                                             new Object[]{path});
        } else {
            return context.getResourcePaths(path);
        }
    }


    @Override
    public URL getResource(String path)
        throws MalformedURLException {
        if (Globals.IS_SECURITY_ENABLED) {
            try {
                return (URL) invokeMethod(context, "getResource", 
                                          new Object[]{path});
            } catch(Throwable t) {
                if (t instanceof MalformedURLException){
                    throw (MalformedURLException)t;
                }
                return null;
            }
        } else {
            return context.getResource(path);
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public InputStream getResourceAsStream(String path) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (InputStream) doPrivileged("getResourceAsStream", 
                                              new Object[]{path});
        } else {
            return context.getResourceAsStream(path);
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public RequestDispatcher getRequestDispatcher(final String path) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (RequestDispatcher) doPrivileged("getRequestDispatcher", 
                                                    new Object[]{path});
        } else {
            return context.getRequestDispatcher(path);
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public RequestDispatcher getNamedDispatcher(String name) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (RequestDispatcher) doPrivileged("getNamedDispatcher", 
                                                    new Object[]{name});
        } else {
            return context.getNamedDispatcher(name);
        }
    }


    /**
     * @deprecated As of Java Servlet API 2.1, with no direct replacement.
     */
    @Override
    @Deprecated
    public Servlet getServlet(String name)
        throws ServletException {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            try {
                return (Servlet) invokeMethod(context, "getServlet", 
                                              new Object[]{name});
            } catch (Throwable t) {
                if (t instanceof ServletException) {
                    throw (ServletException) t;
                }
                return null;
            }
        } else {
            return context.getServlet(name);
        }
    }


    /**
     * @deprecated As of Java Servlet API 2.1, with no direct replacement.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Enumeration<Servlet>) doPrivileged("getServlets", null);
        } else {
            return context.getServlets();
        }
    }


    /**
     * @deprecated As of Java Servlet API 2.1, with no direct replacement.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    @Deprecated
    public Enumeration<String> getServletNames() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Enumeration<String>) doPrivileged("getServletNames", null);
        } else {
            return context.getServletNames();
        }
   }


    @Override
    public void log(String msg) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("log", new Object[]{msg} );
        } else {
            context.log(msg);
        }
    }


    /**
     * @deprecated As of Java Servlet API 2.1, use
     *  <code>log(String, Throwable)</code> instead
     */
    @Override
    @Deprecated
    public void log(Exception exception, String msg) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("log", new Class[]{Exception.class, String.class}, 
                         new Object[]{exception,msg});
        } else {
            context.log(exception, msg);
        }
    }


    @Override
    public void log(String message, Throwable throwable) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("log", new Class[]{String.class, Throwable.class}, 
                         new Object[]{message, throwable});
        } else {
            context.log(message, throwable);
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public String getRealPath(String path) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getRealPath", new Object[]{path});
        } else {
            return context.getRealPath(path);
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public String getServerInfo() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getServerInfo", null);
        } else {
            return context.getServerInfo();
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public String getInitParameter(String name) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getInitParameter", 
                                         new Object[]{name});
        } else {
            return context.getInitParameter(name);
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Enumeration<String> getInitParameterNames() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Enumeration<String>) doPrivileged(
                "getInitParameterNames", null);
        } else {
            return context.getInitParameterNames();
        }
    }

    /**
     * @return true if the context initialization parameter with the given
     * name and value was set successfully on this ServletContext, and false
     * if it was not set because this ServletContext already contains a
     * context initialization parameter with a matching name
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public boolean setInitParameter(String name, String value) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return ((Boolean) doPrivileged(
                "setInitParameter", new Object[]{name, value})).booleanValue();
        } else {
            return context.setInitParameter(name, value);
        }
    }


    @Override
    public Object getAttribute(String name) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return doPrivileged("getAttribute", new Object[]{name});
        } else {
            return context.getAttribute(name);
        }
     }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Enumeration<String> getAttributeNames() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Enumeration<String>) doPrivileged(
                "getAttributeNames", null);
        } else {
            return context.getAttributeNames();
        }
    }


    @Override
    public void setAttribute(String name, Object object) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("setAttribute", new Object[]{name,object});
        } else {
            context.setAttribute(name, object);
        }
    }


    @Override
    public void removeAttribute(String name) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("removeAttribute", new Object[]{name});
        } else {
            context.removeAttribute(name);
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public String getServletContextName() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getServletContextName", null);
        } else {
            return context.getServletContextName();
        }
    }


    /*
     * Adds the servlet with the given name and class name to this
     * servlet context.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public ServletRegistration.Dynamic addServlet(
            String servletName, String className) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ServletRegistration.Dynamic) doPrivileged(
                "addServlet", new Object[] {servletName, className});
        } else {
            return context.addServlet(servletName, className);
        }
    }


    /*
     * Registers the given servlet instance with this ServletContext
     * under the given <tt>servletName</tt>.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public ServletRegistration.Dynamic addServlet(
            String servletName, Servlet servlet) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ServletRegistration.Dynamic) doPrivileged(
                "addServlet", new Class[]{String.class, Servlet.class},
                new Object[] {servletName, servlet});
        } else {
            return context.addServlet(servletName, servlet);
        }
    }


    /*
     * Adds the servlet with the given name and class type to this
     * servlet context.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public ServletRegistration.Dynamic addServlet(String servletName,
            Class <? extends Servlet> servletClass) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ServletRegistration.Dynamic) doPrivileged(
                "addServlet", new Class[]{String.class, Class.class},
                new Object[] {servletName, servletClass});
        } else {
            return context.addServlet(servletName, servletClass);
        }
    }


    /**
     * Instantiates the given Servlet class and performs any required
     * resource injection into the new Servlet instance before returning
     * it.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public <T extends Servlet> T createServlet(Class<T> clazz)
            throws ServletException {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (T) doPrivileged(
                "createServlet", new Object[] {clazz});
        } else {
            return context.createServlet(clazz);
        }
    }


    /**
     * Gets the ServletRegistration corresponding to the servlet with the
     * given <tt>servletName</tt>.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public ServletRegistration getServletRegistration(String servletName) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ServletRegistration) doPrivileged(
                "getServletRegistration", new Object[] {servletName});
        } else {
            return context.getServletRegistration(servletName);
        }
    }


    /**
     * Gets a Map of the ServletRegistration objects corresponding to all
     * currently registered servlets.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Map<String, ServletRegistration>) doPrivileged(
                "getServletRegistrations", null);
        } else {
            return context.getServletRegistrations();
        }
    }


    /**
     * Adds the filter with the given name and class name to this servlet
     * context.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public FilterRegistration.Dynamic addFilter(
            String filterName, String className) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (FilterRegistration.Dynamic) doPrivileged(
                "addFilter", new Object[] {filterName, className});
        } else {
            return context.addFilter(filterName, className);
        }
    }
    

    /*
     * Registers the given filter instance with this ServletContext
     * under the given <tt>filterName</tt>.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public FilterRegistration.Dynamic addFilter(
            String filterName, Filter filter) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (FilterRegistration.Dynamic) doPrivileged(
                "addFilter", new Class[]{String.class, Filter.class},
                new Object[] {filterName, filter});
        } else {
            return context.addFilter(filterName, filter);
        }
    }


    /**
     * Adds the filter with the given name and class type to this servlet
     * context.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public FilterRegistration.Dynamic addFilter(String filterName,
            Class <? extends Filter> filterClass) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (FilterRegistration.Dynamic) doPrivileged(
                "addFilter", new Class[]{String.class, Class.class},
                new Object[] {filterName, filterClass});
        } else {
            return context.addFilter(filterName, filterClass);
        }
    }


    /**
     * Instantiates the given Filter class and performs any required
     * resource injection into the new Filter instance before returning
     * it.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public <T extends Filter> T createFilter(Class<T> clazz)
            throws ServletException {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (T) doPrivileged(
                "createFilter", new Object[] {clazz});
        } else {
            return context.createFilter(clazz);
        }
    }


    /**
     * Gets the FilterRegistration corresponding to the filter with the
     * given <tt>filterName</tt>.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public FilterRegistration getFilterRegistration(String filterName) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (FilterRegistration) doPrivileged(
                "getFilterRegistration", new Object[] {filterName});
        } else {
            return context.getFilterRegistration(filterName);
        }
    }


    /**
     * Gets a Map of the FilterRegistration objects corresponding to all
     * currently registered filters.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Map<String, FilterRegistration>) doPrivileged(
                "getFilterRegistrations", null);
        } else {
            return context.getFilterRegistrations();
        }
    }

    
    /**
     * Gets the <tt>SessionCookieConfig</tt> object through which various
     * properties of the session tracking cookies created on behalf of this
     * <tt>ServletContext</tt> may be configured.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public SessionCookieConfig getSessionCookieConfig() {        
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (SessionCookieConfig) doPrivileged(
                "getSessionCookieConfig", null);
        } else {
            return context.getSessionCookieConfig();
        }
    }
    

    /**
     * Sets the session tracking modes that are to become effective for this
     * <tt>ServletContext</tt>.
     */
    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("setSessionTrackingModes",
                    new Object[]{sessionTrackingModes});
        } else {
            context.setSessionTrackingModes(sessionTrackingModes);
        }
    }


    /**
     * Gets the session tracking modes that are supported by default for this
     * <tt>ServletContext</tt>.
     *
     * @return set of the session tracking modes supported by default for
     * this <tt>ServletContext</tt>
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Set<SessionTrackingMode>)
                doPrivileged("getDefaultSessionTrackingModes", null);
        } else {
            return context.getDefaultSessionTrackingModes();
        }
    }


    /**
     * Gets the session tracking modes that are in effect for this
     * <tt>ServletContext</tt>.
     *
     * @return set of the session tracking modes in effect for this
     * <tt>ServletContext</tt>
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Set<SessionTrackingMode>)
                doPrivileged("getEffectiveSessionTrackingModes", null);
        } else {
            return context.getEffectiveSessionTrackingModes();
        }
    }


    /**
     * Adds the listener with the given class name to this ServletContext.
     */
    @Override
    public void addListener(String className) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("addListener",
                    new Object[]{className});
        } else {
            context.addListener(className);
        }
    }


    /**
     * Adds the given listener to this ServletContext.
     */
    @Override
    public <T extends EventListener> void addListener(T t) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("addListener",
                    new Class[]{EventListener.class},
                    new Object[]{t.getClass().getName()});
        } else {
            context.addListener(t);
        }
    }


    /**
     * Adds a listener of the given class type to this ServletContext.
     */
    @Override
    public void addListener(Class <? extends EventListener> listenerClass) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("addListener",
                    new Class[]{Class.class},
                    new Object[]{listenerClass.getName()});
        } else {
            context.addListener(listenerClass);
        }
    }


    /**
     * Instantiates the given EventListener class and performs any
     * required resource injection into the new EventListener instance
     * before returning it.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public <T extends EventListener> T createListener(Class<T> clazz)
            throws ServletException {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (T) doPrivileged(
                "createListener", new Object[] {clazz});
        } else {
            return context.createListener(clazz);
        }
    }


    /**
     * Gets the <code>&lt;jsp-config&gt;</code> related configuration
     * that was aggregated from the <code>web.xml</code> and
     * <code>web-fragment.xml</code> descriptor files of the web application
     * represented by this ServletContext.
     */
    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public JspConfigDescriptor getJspConfigDescriptor() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (JspConfigDescriptor) doPrivileged("getJspConfigDescriptor",
                    null);
        } else {
            return context.getJspConfigDescriptor();
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public ClassLoader getClassLoader() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ClassLoader) doPrivileged("getClassLoader", null);
        } else {
            return context.getClassLoader();
        }
    }


    @Override
    public void declareRoles(String... roleNames) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("declareRoles", roleNames);
        } else {
            context.declareRoles(roleNames);
        }
    }

    @Override
    public String getVirtualServerName() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String)doPrivileged("getVirtualServerName", null);
        } else {
            return context.getVirtualServerName();
        }
    }


    // START PWC 1.2
    /**
     * Gets the underlying StandardContext to which this
     * ApplicationContextFacade is ultimately delegating.
     *
     * @return The underlying StandardContext
     */
    /*
    public StandardContext getUnwrappedContext() {
        return context.getUnwrappedContext();
    }
    */
    // END PWC 1.2

       
    /**
     * Use reflection to invoke the requested method. Cache the method object 
     * to speed up the process
     *                   will be invoked
     * @param methodName The method to call.
     * @param params The arguments passed to the called method.
     */
    private Object doPrivileged(final String methodName, Object[] params){
        try{
            return invokeMethod(context, methodName, params);
        }catch(Throwable t){
            throw new RuntimeException(t.getMessage());
        }
    }

    
    /**
     * Use reflection to invoke the requested method. Cache the method object 
     * to speed up the process
     * @param appContext The AppliationContext object on which the method
     *                   will be invoked
     * @param methodName The method to call.
     * @param params The arguments passed to the called method.
     */
    private Object invokeMethod(ApplicationContext appContext,
                                final String methodName, 
                                Object[] params) 
        throws Throwable{

        try{
            Method method = objectCache.get(methodName);
            if (method == null){
                method = appContext.getClass()
                    .getMethod(methodName, classCache.get(methodName));
                objectCache.put(methodName, method);
            }
            
            return executeMethod(method,appContext,params);
        } catch (Exception ex){
            handleException(ex, methodName);
            return null;
        }
    }
    
    /**
     * Use reflection to invoke the requested method. Cache the method object 
     * to speed up the process
     * @param methodName The method to call.
     * @param clazz The list of argument classes for the given method
     * @param params The arguments passed to the called method.
     */    
    private Object doPrivileged(final String methodName, 
                                final Class<?>[] clazz,
                                Object[] params){

        try{
            Method method = context.getClass().getMethod(methodName, clazz);
            return executeMethod(method,context,params);
        } catch (Exception ex){
            try{
                handleException(ex, methodName);
            }catch (Throwable t){
                throw new RuntimeException(t.getMessage());
            }
            return null;
        }
    }
    
    
    /**
     * Executes the method of the specified <code>ApplicationContext</code>
     * @param method The method object to be invoked.
     * @param context The AppliationContext object on which the method
     *                   will be invoked
     * @param params The arguments passed to the called method.
     */
    private Object executeMethod(final Method method, 
                                 final ApplicationContext context,
                                 final Object[] params) 
            throws PrivilegedActionException, 
                   IllegalAccessException,
                   InvocationTargetException {
                                     
        if (Globals.IS_SECURITY_ENABLED){
           return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>(){
                public Object run() throws IllegalAccessException, InvocationTargetException{
                    return method.invoke(context,  params);
                }
            });
        } else {
            return method.invoke(context, params);
        }        
    }

    
    /**
     * Throw the real exception.
     * @param ex The current exception
     */
    private void handleException(Exception ex, String methodName)
	    throws Throwable {

        Throwable realException;

        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "ApplicationContextFacade." + methodName, ex);
        }

	if (ex instanceof PrivilegedActionException) {
            ex = ((PrivilegedActionException) ex).getException();
	}

        if (ex instanceof InvocationTargetException) {
            realException =
		((InvocationTargetException) ex).getTargetException();
        } else {
            realException = ex;
        }   

        throw realException;
    }
}
