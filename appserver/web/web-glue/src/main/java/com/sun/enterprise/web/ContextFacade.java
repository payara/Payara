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
 */
package com.sun.enterprise.web;


import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;
import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.core.*;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.glassfish.embeddable.web.Context;
import org.glassfish.embeddable.web.config.SecurityConfig;

/**
 * Facade object which masks the internal <code>Context</code>
 * object from the web application.
 *
 * @author Amy Roh
 */
public class ContextFacade extends WebModule {

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new instance of this class, associated with the specified
     * Context instance.
     *
     * @param docRoot
     * @param contextRoot
     * @param classLoader
     *
     */
    public ContextFacade(File docRoot, String contextRoot, ClassLoader classLoader) {
        this.docRoot = docRoot;
        this.contextRoot = contextRoot;
        this.classLoader = classLoader;
    }

     /**
     * The name of the deployed application
     */
    private String appName = null;

    private SecurityConfig config = null;

    /**
     * Wrapped web module.
     */
    private WebModule context = null;

    private File docRoot;

    private String contextRoot;

    private ClassLoader classLoader;

    private Map<String, String> filters = new HashMap<String, String>();

    private Map<String, String> servletNameFilterMappings = new HashMap<String, String>();

    private Map<String, String> urlPatternFilterMappings = new HashMap<String, String>();

    private Map<String, String> servlets = new HashMap<String, String>();

    private Map<String, String[]> servletMappings = new HashMap<String, String[]>();

    protected ArrayList<String> listenerNames = new ArrayList<String>();

    // ------------------------------------------------------------- Properties

    public String getAppName() {
        return appName ;
    }

    public void setAppName(String name) {
        appName = name;
    }

    @Override
    public String getContextRoot() {
        return contextRoot;
    }

    public File getDocRoot() {
        return docRoot;
    }

    // ------------------------------------------------- ServletContext Methods
    @Override
    public String getContextPath() {
        return context.getContextPath();
    }

    @Override
    public ServletContext getContext(String uripath) {
        return context.getContext(uripath);
    }

    @Override
    public int getMajorVersion() {
        return context.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return context.getMinorVersion();
    }

    @Override
    public int getEffectiveMajorVersion() {
        return context.getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() {
        return context.getEffectiveMinorVersion();
    }

    @Override
    public String getMimeType(String file) {
        return context.getMimeType(file);
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return context.getResourcePaths(path);
    }

    @Override
    public URL getResource(String path)
        throws MalformedURLException {
        return context.getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return context.getResourceAsStream(path);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(final String path) {
        return context.getRequestDispatcher(path);
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return context.getNamedDispatcher(name);
    }

    @Override
    public Servlet getServlet(String name) {
        return context.getServlet(name);
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return context.getServlets();
    }

    @Override
    public Enumeration<String> getServletNames() {
        return context.getServletNames();
    }

    @Override
    public void log(String msg) {
        context.log(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        context.log(exception, msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        context.log(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        return context.getRealPath(path);
    }

    @Override
    public String getServerInfo() {
        return context.getServerInfo();
    }

    @Override
    public String getInitParameter(String name) {
        return context.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return context.getInitParameterNames();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return context.setInitParameter(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return context.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return context.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object object) {
        context.setAttribute(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        context.removeAttribute(name);
    }

    @Override
    public String getServletContextName() {
        return context.getServletContextName();
    }

    /**
     * Returns previously added servlets
     */
    public Map<String, String> getAddedServlets() {
        return servlets;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(
            String servletName, String className) {
        if (context != null) {
            return context.addServlet(servletName, className);
        } else {
            return addServletFacade(servletName, className);
        }
    }

    /*
    public Servlet findServlet(String name) {
        if (name == null)
            return (null);
        synchronized (servlets) {       // Required by post-start changes
            return servlets.get(name);
        }
    }*/

    public ServletRegistration.Dynamic addServletFacade(String servletName,
            String className) {
        if (servletName == null || className == null) {
            throw new NullPointerException("Null servlet instance or name");
        }

        DynamicServletRegistrationImpl regis =
                (DynamicServletRegistrationImpl)
                        servletRegisMap.get(servletName);
        if (regis == null) {
            StandardWrapper wrapper = new StandardWrapper();
            wrapper.setName(servletName);
            wrapper.setServletClassName(className);

            regis = (DynamicServletRegistrationImpl)
                    createDynamicServletRegistrationImpl(wrapper);

            DynamicServletRegistrationImpl tmpRegis =
                    (DynamicServletRegistrationImpl)
                            servletRegisMap.putIfAbsent(servletName, regis);
            if (tmpRegis != null) {
                regis = tmpRegis;
            }
            servlets.put(servletName, className);
        }

        return regis;
    }

    public Map<String, String[]> getServletMappings() {
        return servletMappings;
    }

    protected ServletRegistrationImpl createServletRegistrationImpl(
            StandardWrapper wrapper) {
        return new ServletRegistrationImpl(wrapper, this);
    }
    protected ServletRegistrationImpl createDynamicServletRegistrationImpl(
            StandardWrapper wrapper) {
        return new DynamicServletRegistrationImpl(wrapper, this);
    }

    public ServletRegistration.Dynamic addServlet(String servletName,
            Class <? extends Servlet> servletClass) {
        if (context != null) {
            return context.addServlet(servletName, servletClass);
        } else {
            return addServletFacade(servletName, servletClass.getName());
        }
    }

    public ServletRegistration.Dynamic addServlet(
            String servletName, Servlet servlet) {
        if (context != null) {
            return context.addServlet(servletName, servlet);
        } else {
            return addServletFacade(servletName, servlet.getClass().getName());
        }
    }

    @Override
    public Set<String> addServletMapping(String name,
                                         String[] urlPatterns) {
        servletMappings.put(name, urlPatterns);
        return servletMappings.keySet();
    }

    public <T extends Servlet> T createServlet(Class<T> clazz)
            throws ServletException {
        if (context != null) {
            return context.createServlet(clazz);
        } else {
            try {
                return createServletInstance(clazz);
            } catch (Throwable t) {
                throw new ServletException("Unable to create Servlet from " +
                        "class " + clazz.getName(), t);
            }
        }
    }

    public ServletRegistration getServletRegistration(String servletName) {
        return servletRegisMap.get(servletName);
    }

    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return context.getServletRegistrations();
    }

    public Map<String, String> getAddedFilters() {
        return filters;
    }

    public Map<String, String> getServletNameFilterMappings() {
        return servletNameFilterMappings;
    }

    public Map<String, String> getUrlPatternFilterMappings() {
        return urlPatternFilterMappings;
    }

    public FilterRegistration.Dynamic addFilterFacade(
            String filterName, String className) {
        DynamicFilterRegistrationImpl regis =
                (DynamicFilterRegistrationImpl) filterRegisMap.get(
                        filterName);
        FilterDef filterDef = null;
        if (null == regis) {
            filterDef = new FilterDef();
        } else {
            filterDef = regis.getFilterDefinition();
        }
        filterDef.setFilterName(filterName);
        filterDef.setFilterClassName(className);

        regis = new DynamicFilterRegistrationImpl(filterDef, this);
        filterRegisMap.put(filterDef.getFilterName(), regis);
        filters.put(filterName, className);

        return regis;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(
            String filterName, String className) {
        if (context != null) {
            return context.addFilter(filterName, className);
        } else {
            return addFilterFacade(filterName, className);
        }
    }

    @Override
    public void addFilterMap(FilterMap filterMap, boolean isMatchAfter) {
        if (filterMap.getServletName() != null) {
            servletNameFilterMappings.put(filterMap.getFilterName(), filterMap.getServletName());
        } else if (filterMap.getURLPattern() != null) {
            urlPatternFilterMappings.put(filterMap.getFilterName(), filterMap.getURLPattern());
        }
    }

    public FilterRegistration.Dynamic addFilter(
            String filterName, Filter filter) {
        if (context != null) {
            return context.addFilter(filterName, filter);
        } else {
            return addFilterFacade(filterName, filter.getClass().getName());
        }
    }

    public FilterRegistration.Dynamic addFilter(String filterName,
            Class <? extends Filter> filterClass) {
        if (context != null) {
            return context.addFilter(filterName, filterClass);
        } else {
            return addFilterFacade(filterName, filterClass.getName());
        }
    }

    public <T extends Filter> T createFilter(Class<T> clazz)
            throws ServletException {
        if (context != null) {
            return context.createFilter(clazz);
        } else {
            try {
                return createFilterInstance(clazz);
            } catch (Throwable t) {
                throw new ServletException("Unable to create Filter from " +
                        "class " + clazz.getName(), t);
            }
        }
    }

    public FilterRegistration getFilterRegistration(String filterName) {
        return filterRegisMap.get(filterName);
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return context.getFilterRegistrations();
    }

    public SessionCookieConfig getSessionCookieConfig() {        
        return context.getSessionCookieConfig();
    }
    
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        context.setSessionTrackingModes(sessionTrackingModes);
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return context.getDefaultSessionTrackingModes();
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return context.getEffectiveSessionTrackingModes();
    }

    public void addListener(String className) {
        if (context != null) {
            context.addListener(className);
        } else {
            listenerNames.add(className);
        }
    }

    public List<String> getListeners() {
        return listenerNames;
    }

    public <T extends EventListener> void addListener(T t) {
        if (context != null) {
            context.addListener(t);
        } else {
            listenerNames.add(t.getClass().getName());
        }
    }

    public void addListener(Class <? extends EventListener> listenerClass) {
        if (context != null) {
            context.addListener(listenerClass);
        } else {
            listenerNames.add(listenerClass.getName());
        }
    }

    public <T extends EventListener> T createListener(Class<T> clazz)
            throws ServletException {
        if (context != null) {
            return context.createListener(clazz);
        } else {
            if (!ServletContextListener.class.isAssignableFrom(clazz) &&
                    !ServletContextAttributeListener.class.isAssignableFrom(clazz) &&
                    !ServletRequestListener.class.isAssignableFrom(clazz) &&
                    !ServletRequestAttributeListener.class.isAssignableFrom(clazz) &&
                    !HttpSessionListener.class.isAssignableFrom(clazz) &&
                    !HttpSessionAttributeListener.class.isAssignableFrom(clazz) &&
                    !HttpSessionIdListener.class.isAssignableFrom(clazz)) {
                String msg = rb.getString(INVALID_LISTENER_TYPE);
                msg = MessageFormat.format(msg, clazz.getName());
                throw new IllegalArgumentException(msg);
            }
            try {
                return createListenerInstance(clazz);
            } catch (Throwable t) {
                throw new ServletException(t);
            }
        }
    }

    public JspConfigDescriptor getJspConfigDescriptor() {
        return context.getJspConfigDescriptor();
    }

    public ClassLoader getClassLoader() {
        if (classLoader != null) {
            return classLoader;
        } else if (context != null) {
            return context.getClassLoader();
        } else {
            return null;
        }
    }

    public void declareRoles(String... roleNames) {
        context.declareRoles(roleNames);
    }

    public String getVirtualServerName() {
        return context.getVirtualServerName();
    }

    public String getPath() {
        return context.getPath();
    }

    public void setPath(String path) {
        context.setPath(path);
    }

    public String getDefaultWebXml() {
        return context.getDefaultWebXml();
    }

    public void setDefaultWebXml(String defaultWebXml) {
        context.setDefaultWebXml(defaultWebXml);
    }

    /**
     * Gets the underlying StandardContext to which this
     * ContextFacade is ultimately delegating.
     *
     * @return The underlying StandardContext
     */
    public WebModule getUnwrappedContext() {
        return context;
    }

    public void setUnwrappedContext(WebModule wm) {
        context = wm;
    }

    // --------------------------------------------------------- embedded Methods

    /**
     * Enables or disables directory listings on this <tt>Context</tt>.
     */
    public void setDirectoryListing(boolean directoryListing) {
        context.setDirectoryListing(directoryListing);
    }

    public boolean isDirectoryListing() {
        return context.isDirectoryListing();
    }

    /**
     * Set the security related configuration for this context
     */
    public void setSecurityConfig(SecurityConfig config) {
        this.config = config;
        if (config == null) {
            return;
        } else if (context != null) {
            context.setSecurityConfig(config);
        }
    }

    /**
     * Gets the security related configuration for this context
     */
    public SecurityConfig getSecurityConfig() {
        return config;
    }


}
