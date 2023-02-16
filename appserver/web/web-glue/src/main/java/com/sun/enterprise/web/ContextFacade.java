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
 */
// Portions Copyright [2022-2023] Payara Foundation and/or affiliates
package com.sun.enterprise.web;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.catalina.core.ApplicationFilterRegistration;
import org.apache.catalina.core.ApplicationServletRegistration;
import org.apache.catalina.core.StandardWrapper;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.glassfish.embeddable.web.config.SecurityConfig;
import org.glassfish.web.LogFacade;

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
    public void log(String msg) {
        context.log(msg);
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
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        if (context != null) {
            return context.addServlet(servletName, className);
        } else {
            return addServletFacade(servletName, className);
        }
    }

    public ServletRegistration.Dynamic addServletFacade(String servletName, String className) {
        if (servletName == null || className == null) {
            throw new NullPointerException("Null servlet instance or name");
        }
        ApplicationServletRegistration servletRegis = (ApplicationServletRegistration) context.getServletRegistration(servletName);
        if (servletRegis == null) {
            StandardWrapper wrapper = new StandardWrapper();
            wrapper.setName(servletName);
            wrapper.setServletClass(className);

            servletRegis = (ApplicationServletRegistration) createDynamicServletRegistrationImpl(wrapper);

            ApplicationServletRegistration tmpServletRegis =
                    (ApplicationServletRegistration) context.addServlet(servletName, servletRegis.getClassName());

            if (tmpServletRegis != null) {
                servletRegis = tmpServletRegis;
            }
            servlets.put(servletName, className);
        }

        return servletRegis;
    }

    public Map<String, String[]> getServletMappings() {
        return servletMappings;
    }

    protected ServletRegistration createServletRegistrationImpl(StandardWrapper wrapper) {
        return new ApplicationServletRegistration(wrapper, this);
    }
    protected ServletRegistration.Dynamic createDynamicServletRegistrationImpl(StandardWrapper wrapper) {
        return new ApplicationServletRegistration(wrapper, this);
    }

    public ServletRegistration.Dynamic addServlet(String servletName, Class <? extends Servlet> servletClass) {
        if (context != null) {
            return context.addServlet(servletName, servletClass);
        } else {
            return addServletFacade(servletName, servletClass.getName());
        }
    }

    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        if (context != null) {
            return context.addServlet(servletName, servlet);
        } else {
            return addServletFacade(servletName, servlet.getClass().getName());
        }
    }

    public Set<String> addServletMapping(String name, String[] urlPatterns) {
        servletMappings.put(name, urlPatterns);
        return servletMappings.keySet();
    }

    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        if (context != null) {
            return context.createServlet(clazz);
        } else {
            try {
                return super.createServlet(clazz);
            } catch (Throwable t) {
                throw new ServletException("Unable to create Servlet from class " + clazz.getName(), t);
            }
        }
    }

    public ServletRegistration getServletRegistration(String servletName) {
        return context.getServletRegistration(servletName);
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

    public FilterRegistration.Dynamic addFilterFacade(String filterName, String className) {
        FilterDef filterDef = context.findFilterDef(filterName);
        if (null == filterDef) {
            filterDef = new FilterDef();
        }
        filterDef.setFilterName(filterName);
        filterDef.setFilterClass(className);

        ApplicationFilterRegistration appFilterRegis = new ApplicationFilterRegistration(filterDef, this);
        context.addFilterDef(filterDef);
        filters.put(filterName, className);

        return appFilterRegis;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        if (context != null) {
            return context.addFilter(filterName, className);
        } else {
            return addFilterFacade(filterName, className);
        }
    }

    @Override
    public void addFilterMap(FilterMap filterMap) {
        if (filterMap.getServletNames() != null) {
            for (String servletName : filterMap.getServletNames()) {
                servletNameFilterMappings.put(filterMap.getFilterName(), servletName);
            }
        } else if (filterMap.getURLPatterns() != null) {
            for (String urlPattern : filterMap.getURLPatterns()) {
                urlPatternFilterMappings.put(filterMap.getFilterName(), urlPattern);
            }
        }
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        if (context != null) {
            return context.addFilter(filterName, filter);
        } else {
            return addFilterFacade(filterName, filter.getClass().getName());
        }
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Class <? extends Filter> filterClass) {
        if (context != null) {
            return context.addFilter(filterName, filterClass);
        } else {
            return addFilterFacade(filterName, filterClass.getName());
        }
    }

    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        if (context != null) {
            return context.createFilter(clazz);
        } else {
            try {
                return super.createFilter(clazz);
            } catch (Throwable t) {
                throw new ServletException("Unable to create Filter from " +
                        "class " + clazz.getName(), t);
            }
        }
    }

    public FilterRegistration getFilterRegistration(String filterName) {
        return context.getFilterRegistration(filterName);
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

    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
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
                String msg = rb.getString(LogFacade.INVALID_LISTENER_TYPE);
                msg = MessageFormat.format(msg, clazz.getName());
                throw new IllegalArgumentException(msg);
            }
            try {
                return super.createListener(clazz);
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
        throw new UnsupportedOperationException("No longer supported - unused within Payara");
    }

    public boolean isDirectoryListing() {
        throw new UnsupportedOperationException("No longer supported - unused within Payara");
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
