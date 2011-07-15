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

package org.apache.catalina;

import org.apache.catalina.deploy.*;
import org.apache.catalina.util.CharsetMapper;

import javax.servlet.*;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import org.glassfish.grizzly.http.server.util.Mapper;


/**
 * A <b>Context</b> is a Container that represents a servlet context, and
 * therefore an individual web application, in the Catalina servlet engine.
 * It is therefore useful in almost every deployment of Catalina (even if a
 * Connector attached to a web server (such as Apache) uses the web server's
 * facilities to identify the appropriate Wrapper to handle this request.
 * It also provides a convenient mechanism to use Interceptors that see
 * every request processed by this particular web application.
 * <p>
 * The parent Container attached to a Context is generally a Host, but may
 * be some other implementation, or may be omitted if it is not necessary.
 * <p>
 * The child containers attached to a Context are generally implementations
 * of Wrapper (representing individual servlet definitions).
 * <p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.4 $ $Date: 2007/05/05 05:31:51 $
 */

public interface Context extends Container {


    // ----------------------------------------------------- Manifest Constants


    /**
     * The LifecycleEvent type sent when a context is reloaded.
     */
    String RELOAD_EVENT = "reload";


    // ------------------------------------------------------------- Properties


    /**
     * @return the list of initialized application event listeners
     * of this application, in the order in which they have been specified
     * in the deployment descriptor
     */
    List<EventListener> getApplicationEventListeners();


    /**
     * Return the application available flag for this Context.
     */
    boolean getAvailable();


    /**
     * Set the application available flag for this Context.
     *
     * @param available The new application available flag
     */
    void setAvailable(boolean available);


    /**
     * Return the Locale to character set mapper for this Context.
     */
    CharsetMapper getCharsetMapper();


    /**
     * Set the Locale to character set mapper for this Context.
     *
     * @param mapper The new mapper
     */
    void setCharsetMapper(CharsetMapper mapper);


    /**
     * Return the path to a file to save this Context information.
     */
    String getConfigFile();


    /**
     * Set the path to a file to save this Context information.
     *
     * @param configFile The path to a file to save this Context information.
     */
    void setConfigFile(String configFile);


    /**
     * Return the "correctly configured" flag for this Context.
     */
    boolean getConfigured();


    /**
     * Set the "correctly configured" flag for this Context.  This can be
     * set to false by startup listeners that detect a fatal configuration
     * error to avoid the application from being made available.
     *
     * @param configured The new correctly configured flag
     */
    void setConfigured(boolean configured);


    /**
     * Return the "use cookies for session ids" flag.
     */
    boolean getCookies();


    /**
     * Set the "use cookies for session ids" flag.
     *
     * @param cookies The new flag
     */
    void setCookies(boolean cookies);


    /**
     * @return the name that will be assigned to any session tracking
     * cookies created on behalf of this context
     */
    public String getSessionCookieName();


    /**
     * @return the session tracking cookie configuration of this
     * <tt>ServletContext</tt>.
     */
    public SessionCookieConfig getSessionCookieConfig();


    /**
     * @return the name that will be assigned to any session tracking
     * parameter created on behalf of this context
     */
    public String getSessionParameterName();


    /**
     * Checks whether the rewriting of URLs with the jsessionids of
     * HTTP sessions belonging to this context is enabled or not.
     *
     * @return true if the rewriting of URLs with the jsessionids of HTTP
     * sessions belonging to this context is enabled, false otherwise
     */
    public boolean isEnableURLRewriting();


    /**
     * Enables or disables the rewriting of URLs with the jsessionids of
     * HTTP sessions belonging to this context.
     *
     * @param enableURLRewriting true if the rewriting of URLs with the
     * jsessionids of HTTP sessions belonging to this context should be
     * enabled, false otherwise
     */
    public void setEnableURLRewriting(boolean enableURLRewriting);


    /**
     * Return the "allow crossing servlet contexts" flag.
     */
    boolean getCrossContext();



    /**
     * Return the alternate Deployment Descriptor name.
     */
    String getAltDDName();


    /**
     * Set an alternate Deployment Descriptor name.
     */
    void setAltDDName(String altDDName) ;


    /**
     * Set the "allow crossing servlet contexts" flag.
     *
     * @param crossContext The new cross contexts flag
     */
    void setCrossContext(boolean crossContext);


    /**
     * Return the display name of this web application.
     */
    String getDisplayName();


    /**
     * Set the display name of this web application.
     *
     * @param displayName The new display name
     */
    void setDisplayName(String displayName);


    /**
     * Return the distributable flag for this web application.
     */
    boolean getDistributable();


    /**
     * Set the distributable flag for this web application.
     *
     * @param distributable The new distributable flag
     */
    void setDistributable(boolean distributable);


    /**
     * Return the document root for this Context.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     */
    String getDocBase();


    /**
     * Set the document root for this Context.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     *
     * @param docBase The new document root
     */
    void setDocBase(String docBase);


    /**
     * Return the URL encoded context path, using UTF-8.
     */
    String getEncodedPath();


    /**
     * Return the login configuration descriptor for this web application.
     */
    LoginConfig getLoginConfig();


    /**
     * Set the login configuration descriptor for this web application.
     *
     * @param config The new login configuration
     */
    void setLoginConfig(LoginConfig config);


    /**
     * Get the request dispatcher mapper.
     */
    Mapper getMapper();


    /**
     * Return the naming resources associated with this web application.
     */
    NamingResources getNamingResources();


    /**
     * Set the naming resources for this web application.
     *
     * @param namingResources The new naming resources
     */
    void setNamingResources(NamingResources namingResources);


    /**
     * Return the context path for this web application.
     */
    String getPath();


    /**
     * Set the context path for this web application.
     *
     * @param path The new context path
     */
    void setPath(String path);


    /**
     * Return the public identifier of the deployment descriptor DTD that is
     * currently being parsed.
     */
    String getPublicId();


    /**
     * Set the public identifier of the deployment descriptor DTD that is
     * currently being parsed.
     *
     * @param publicId The public identifier
     */
    void setPublicId(String publicId);


    /**
     * Return the reloadable flag for this web application.
     */
    boolean getReloadable();


    /**
     * Set the reloadable flag for this web application.
     *
     * @param reloadable The new reloadable flag
     */
    void setReloadable(boolean reloadable);


    /**
     * Return the override flag for this web application.
     */
    boolean getOverride();


    /**
     * Set the override flag for this web application.
     *
     * @param override The new override flag
     */
    void setOverride(boolean override);


    /**
     * Return the privileged flag for this web application.
     */
    boolean getPrivileged();


    /**
     * Set the privileged flag for this web application.
     *
     * @param privileged The new privileged flag
     */
    void setPrivileged(boolean privileged);


    /**
     * Return the servlet context for which this Context is a facade.
     */
    ServletContext getServletContext();


    /**
     * Return the default session timeout (in minutes) for this
     * web application.
     */
    int getSessionTimeout();


    /**
     * Set the default session timeout (in minutes) for this
     * web application.
     *
     * @param timeout The new default session timeout
     */
    void setSessionTimeout(int timeout);


    // START IASRI 4823322
    /**
     * Get Auditors associated with this context, if any.
     *
     * @return array of Auditor objects, or null
     *
     */
    Auditor[] getAuditors();


    /**
     * Set the Auditors associated with this context.
     *
     * @param auditor array of Auditor objects
     *
     */
    void setAuditors(Auditor[] auditor);
    // END IASRI 4823322


    // --------------------------------------------------------- Public Methods

    /**
     * Adds the Listener with the given class name that is declared in the
     * deployment descriptor to the set of Listeners configured for this
     * application.
     *
     * @param listener the fully qualified class name of the Listener
     */
    void addApplicationListener(String listener);


    /**
     * Add a new application parameter for this application.
     *
     * @param parameter The new application parameter
     */
    void addApplicationParameter(ApplicationParameter parameter);


    /**
     * Add a security constraint to the set for this web application.
     */
    void addConstraint(SecurityConstraint constraint);


    /**
     * Add an EJB resource reference for this web application.
     *
     * @param ejb New EJB resource reference
     */
    void addEjb(ContextEjb ejb);


    /**
     * Add an environment entry for this web application.
     *
     * @param environment New environment entry
     */
    void addEnvironment(ContextEnvironment environment);


    /**
     * Add an error page for the specified error or Java exception.
     *
     * @param errorPage The error page definition to be added
     */
    void addErrorPage(ErrorPage errorPage);


    /**
     * Add a filter definition to this Context.
     *
     * @param filterDef The filter definition to be added
     */
    void addFilterDef(FilterDef filterDef);


    /**
     * Add a filter mapping to this Context.
     *
     * @param filterMap The filter mapping to be added
     */
    void addFilterMap(FilterMap filterMap);


    /**
     * Add the classname of an InstanceListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of an InstanceListener class
     */
    void addInstanceListener(String listener);


    /**
     * Add the given URL pattern as a jsp-property-group.  This maps
     * resources that match the given pattern so they will be passed
     * to the JSP container.  Though there are other elements in the
     * property group, we only care about the URL pattern here.  The
     * JSP container will parse the rest.
     *
     * @param pattern URL pattern to be mapped
     */
    void addJspMapping(String pattern);


    /**
     * Add a Locale Encoding Mapping (see Sec 5.4 of Servlet spec 2.4)
     *
     * @param locale locale to map an encoding for
     * @param encoding encoding to be used for a give locale
     */
    void addLocaleEncodingMappingParameter(String locale, String encoding);


    /**
     * Add a local EJB resource reference for this web application.
     *
     * @param ejb New local EJB resource reference
     */
    void addLocalEjb(ContextLocalEjb ejb);


    /**
     * Add a new MIME mapping, replacing any existing mapping for
     * the specified extension.
     *
     * @param extension Filename extension being mapped
     * @param mimeType Corresponding MIME type
     */
    void addMimeMapping(String extension, String mimeType);


    /**
     * Add a new context initialization parameter, replacing any existing
     * value for the specified name.
     *
     * @param name Name of the new parameter
     * @param value Value of the new  parameter
     */
    void addParameter(String name, String value);


    /**
     * Add a resource reference for this web application.
     *
     * @param resource New resource reference
     */
    void addResource(ContextResource resource);


    /**
     * Add a resource environment reference for this web application.
     *
     * @param name The resource environment reference name
     * @param type The resource environment reference type
     */
    void addResourceEnvRef(String name, String type);


    /**
     * Add a resource link for this web application.
     *
     * @param resourceLink New resource link
     */
    void addResourceLink(ContextResourceLink resourceLink);


    /**
     * Add a security role reference for this web application.
     *
     * @param role Security role used in the application
     * @param link Actual security role to check for
     */
    void addRoleMapping(String role, String link);


    /**
     * Add a new security role for this web application.
     *
     * @param role New security role
     */
    void addSecurityRole(String role);


    /**
     * Adds the given servlet mapping to this Context, overriding any
     * existing mapping for the specified pattern.
     *
     * @param pattern the URL pattern to be mapped
     * @param name the name of the Servlet to which to map
     */
    void addServletMapping(String pattern, String name);


    /**
     * Add a resource which will be watched for reloading by the host auto
     * deployer. Note: this will not be used in embedded mode.
     *
     * @param name Path to the resource, relative to docBase
     */
    void addWatchedResource(String name);


    /**
     * Add a new welcome file to the set recognized by this Context.
     *
     * @param name New welcome file name
     */
    void addWelcomeFile(String name);


    /**
     * Add the classname of a LifecycleListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a LifecycleListener class
     */
    void addWrapperLifecycle(String listener);


    /**
     * Add the classname of a ContainerListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a ContainerListener class
     */
    void addWrapperListener(String listener);


    /**
     * Factory method to create and return a new Wrapper instance, of
     * the Java implementation class appropriate for this Context
     * implementation.  The constructor of the instantiated Wrapper
     * will have been called, but no properties will have been set.
     */
    Wrapper createWrapper();


    /**
     * Adds the given servlet instance with the given name to this servlet
     * context and initializes it.
     *
     * <p>In order to add any URL patterns that will be mapped to the
     * given servlet, addServletMappings must be used. If this context
     * has already been started, the URL patterns must be passed to
     * addServlet instead.
     *
     * @param servletName the servlet name
     * @param instance the servlet instance
     * @param initParams Map containing the initialization parameters for
     * the servlet
     *
     * @return the ServletRegistration through which the servlet may be
     * further configured
     *
     * @throws ServletException if the servlet fails to be initialized
     */
    public ServletRegistration.Dynamic addServlet(String servletName,
            Servlet instance, Map<String, String> initParams)
        throws ServletException;


    /**
     * Adds the given servlet instance with the given name and URL patterns
     * to this servlet context, and initializes it.
     *
     * @param servletName the servlet name
     * @param instance the servlet instance
     * @param initParams Map containing the initialization parameters for
     * the servlet
     * @param urlPatterns the URL patterns that will be mapped to the servlet
     *
     * @return the ServletRegistration through which the servlet may be
     * further configured
     *
     * @throws ServletException if the servlet fails to be initialized
     */
    public ServletRegistration.Dynamic addServlet(String servletName,
            Servlet instance, Map<String, String> initParams,
            String... urlPatterns)
        throws ServletException;


    /**
     * Gets the (possibly empty) list of application parameters for this
     * application.
     */
    List<ApplicationParameter> findApplicationParameters();


    /**
     * Gets the (possibly empty) list of security constraints defined for
     * this web application.
     */
    List<SecurityConstraint> getConstraints();


    /**
     * Checks whether this web application has any security constraints
     * defined.
     */
    public boolean hasConstraints();


    /**
     * Removes any security constraints from this web application.
     */
    void removeConstraints();


    /**
     * Return the EJB resource reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    ContextEjb findEjb(String name);


    /**
     * Return the defined EJB resource references for this application.
     * If there are none, a zero-length array is returned.
     */
    ContextEjb[] findEjbs();


    /**
     * Return the environment entry with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired environment entry
     */
    ContextEnvironment findEnvironment(String name);


    /**
     * Return the set of defined environment entries for this web
     * application.  If none have been defined, a zero-length array
     * is returned.
     */
    ContextEnvironment[] findEnvironments();


    /**
     * Return the error page entry for the specified HTTP error code,
     * if any; otherwise return <code>null</code>.
     *
     * @param errorCode Error code to look up
     */
    ErrorPage findErrorPage(int errorCode);


    /**
     * Return the error page entry for the specified Java exception type,
     * if any; otherwise return <code>null</code>.
     *
     * @param exceptionType Exception type to look up
     */
    ErrorPage findErrorPage(String exceptionType);


    /**
     * Gets the default error page of this context.
     *
     * <p>A default error page is an error page that was declared without
     * any exception-type and error-code.
     *
     * @return the default error page of this context, or null if this
     * context does not have any default error page
     */
    public ErrorPage getDefaultErrorPage();


    /**
     * Return the filter definition for the specified filter name, if any;
     * otherwise return <code>null</code>.
     *
     * @param filterName Filter name to look up
     */
    FilterDef findFilterDef(String filterName);


    /**
     * Return the set of defined filters for this Context.
     */
    FilterDef[] findFilterDefs();


    /**
     * Gets the (possibly empty) list of filter mappings for this Context.
     */
    List<FilterMap> findFilterMaps();


    /**
     * Gets the (possibly empty) list of InstanceListener classes that
     * will be added to newly created Wrappers automatically.
     */
    List<String> findInstanceListeners();


    /**
     * Return the local EJB resource reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    ContextLocalEjb findLocalEjb(String name);


    /**
     * Return the defined local EJB resource references for this application.
     * If there are none, a zero-length array is returned.
     */
    ContextLocalEjb[] findLocalEjbs();


    /**
     * Return the MIME type to which the specified extension is mapped,
     * if any; otherwise return <code>null</code>.
     *
     * @param extension Extension to map to a MIME type
     */
    String findMimeMapping(String extension);


    /**
     * Return the extensions for which MIME mappings are defined.  If there
     * are none, a zero-length array is returned.
     */
    String[] findMimeMappings();


    /**
     * Return the value for the specified context initialization
     * parameter name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the parameter to return
     */
    String findParameter(String name);


    /**
     * Return the names of all defined context initialization parameters
     * for this Context.  If no parameters are defined, a zero-length
     * array is returned.
     */
    String[] findParameters();


    /**
     * Return the resource reference with the specified name, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource reference
     */
    ContextResource findResource(String name);


    /**
     * Return the resource environment reference type for the specified
     * name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource environment reference
     */
    String findResourceEnvRef(String name);


    /**
     * Return the set of resource environment reference names for this
     * web application.  If none have been specified, a zero-length
     * array is returned.
     */
    String[] findResourceEnvRefs();


    /**
     * Return the resource link with the specified name, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource link
     */
    ContextResourceLink findResourceLink(String name);


    /**
     * Return the defined resource links for this application.  If
     * none have been defined, a zero-length array is returned.
     */
    ContextResourceLink[] findResourceLinks();


    /**
     * Return the defined resource references for this application.  If
     * none have been defined, a zero-length array is returned.
     */
    ContextResource[] findResources();


    /**
     * For the given security role (as used by an application), return the
     * corresponding role name (as defined by the underlying Realm) if there
     * is one.  Otherwise, return the specified role unchanged.
     *
     * @param role Security role to map
     */
    String findRoleMapping(String role);


    /**
     * Checks if the given security role is defined for this application.
     *
     * @param role Security role to check for
     *
     * @return true if the specified security role is defined
     * for this application, false otherwise
     */
    boolean hasSecurityRole(String role);


    /**
     * Removes any security roles defined for this application.
     */
    void removeSecurityRoles();


    /**
     * Return the servlet name mapped by the specified pattern (if any);
     * otherwise return <code>null</code>.
     *
     * @param pattern Pattern for which a mapping is requested
     */
    String findServletMapping(String pattern);


    /**
     * Return the patterns of all defined servlet mappings for this
     * Context.  If no mappings are defined, a zero-length array is returned.
     */
    String[] findServletMappings();


    /**
     * Return the context-relative URI of the error page for the specified
     * HTTP status code, if any; otherwise return <code>null</code>.
     *
     * @param status HTTP status code to look up
     */
    ErrorPage findStatusPage(int status);


    /**
     * Return the set of HTTP status codes for which error pages have
     * been specified.  If none are specified, a zero-length array
     * is returned.
     */
    int[] findStatusPages();


    /**
     * Gets the watched resources defined for this web application.
     */
    List<String> getWatchedResources();


    /**
     * Return <code>true</code> if the specified welcome file is defined
     * for this Context; otherwise return <code>false</code>.
     *
     * @param name Welcome file to verify
     */
    boolean findWelcomeFile(String name);


    /**
     * Return the set of welcome files defined for this Context.  If none are
     * defined, a zero-length array is returned.
     */
    String[] findWelcomeFiles();


    /**
     * Gets the (possibly empty) list of LifecycleListener classes that
     * will be added to newly created Wrappers automatically.
     */
    List<String> findWrapperLifecycles();


    /**
     * Gets the (possibly empty) list of ContainerListener classes that
     * will be added to newly created Wrappers automatically.
     */
    List<String> findWrapperListeners();


    /**
     * Reload this web application, if reloading is supported.
     *
     * @exception IllegalStateException if the <code>reloadable</code>
     *  property is set to <code>false</code>.
     */
    void reload();


    /**
     * Remove the application parameter with the specified name from
     * the set for this application.
     *
     * @param name Name of the application parameter to remove
     */
    void removeApplicationParameter(String name);


    /**
     * Remove any EJB resource reference with the specified name.
     *
     * @param name Name of the EJB resource reference to remove
     */
    void removeEjb(String name);


    /**
     * Remove any environment entry with the specified name.
     *
     * @param name Name of the environment entry to remove
     */
    void removeEnvironment(String name);


    /**
     * Removes any error page declarations from this Context.
     */
    void removeErrorPages();


    /**
     * Remove the specified filter definition from this Context, if it exists;
     * otherwise, no action is taken.
     *
     * @param filterDef Filter definition to be removed
     */
    void removeFilterDef(FilterDef filterDef);


    /**
     * Removes any filter mappings from this Context.
     */
    void removeFilterMaps();


    /**
     * Remove a class name from the set of InstanceListener classes that
     * will be added to newly created Wrappers.
     *
     * @param listener Class name of an InstanceListener class to be removed
     */
    void removeInstanceListener(String listener);


    /**
     * Remove any local EJB resource reference with the specified name.
     *
     * @param name Name of the EJB resource reference to remove
     */
    void removeLocalEjb(String name);


    /**
     * Remove the MIME mapping for the specified extension, if it exists;
     * otherwise, no action is taken.
     *
     * @param extension Extension to remove the mapping for
     */
    void removeMimeMapping(String extension);


    /**
     * Remove the context initialization parameter with the specified
     * name, if it exists; otherwise, no action is taken.
     *
     * @param name Name of the parameter to remove
     */
    void removeParameter(String name);


    /**
     * Remove any resource reference with the specified name.
     *
     * @param name Name of the resource reference to remove
     */
    void removeResource(String name);


    /**
     * Remove any resource environment reference with the specified name.
     *
     * @param name Name of the resource environment reference to remove
     */
    void removeResourceEnvRef(String name);


    /**
     * Remove any resource link with the specified name.
     *
     * @param name Name of the resource link to remove
     */
    void removeResourceLink(String name);


    /**
     * Remove any security role reference for the specified name
     *
     * @param role Security role (as used in the application) to remove
     */
    void removeRoleMapping(String role);


    /**
     * Remove any servlet mapping for the specified pattern, if it exists;
     * otherwise, no action is taken.
     *
     * @param pattern URL pattern of the mapping to remove
     */
    void removeServletMapping(String pattern);


    /**
     * Checks whether this web application has any watched resources
     * defined.
     */
    public boolean hasWatchedResources();


    /**
     * Clears any watched resources defined for this web application.
     */
    public void removeWatchedResources();


    /**
     * Removes any Wrapper lifecycle listeners from this Context
     */
    void removeWrapperLifecycles();


    /**
     * Removes any Wrapper listeners from this Context
     */
    void removeWrapperListeners();


    public void removeWelcomeFiles();


    // START S1AS8PE 4817642
    /**
     * Return the "reuse session IDs when creating sessions" flag
     */
    boolean getReuseSessionID();

    /**
     * Set the "reuse session IDs when creating sessions" flag
     *
     * @param reuse The new value for the flag
     */
    void setReuseSessionID(boolean reuse);
    // END S1AS8PE 4817642


    // START RIMOD 4642650
    /**
     * Return whether this context allows sendRedirect() to redirect
     * to a relative URL.
     *
     * The default value for this property is 'false'.
     */
    boolean getAllowRelativeRedirect();


    /**
     * Set whether this context allows sendRedirect() to redirect
     * to a relative URL.
     *
     * @param allowRelativeURLs The new value for this property. The
     *                          default value for this flag is 'false'.
     */
    void setAllowRelativeRedirect(boolean allowRelativeURLs);


    // END RIMOD 4642650
       /**
     * Get the server.xml <context> attribute's xmlNamespaceAware.
     * @return true if namespace awareness is enabled.
     *
     */
    boolean getXmlNamespaceAware();


    /**
     * Get the server.xml <context> attribute's xmlValidation.
     * @return true if validation is enabled.
     *
     */
    boolean getXmlValidation();


    /**
     * Set the validation feature of the XML parser used when
     * parsing xml instances.
     * @param xmlValidation true to enable xml instance validation
     */
    void setXmlValidation(boolean xmlValidation);


   /**
     * Set the namespace aware feature of the XML parser used when
     * parsing xml instances.
     * @param xmlNamespaceAware true to enable namespace awareness
     */
   void setXmlNamespaceAware(boolean xmlNamespaceAware);
    /**
     * Get the server.xml <context> attribute's xmlValidation.
     * @return true if validation is enabled.
     */


    /**
     * Set the validation feature of the XML parser used when
     * parsing tlds files.
     * @param tldValidation true to enable xml instance validation
     */
    void setTldValidation(boolean tldValidation);


    /**
     * Get the server.xml <context> attribute's webXmlValidation.
     * @return true if validation is enabled.
     *
     */
    boolean getTldValidation();


    /**
     * Get the server.xml <host> attribute's xmlNamespaceAware.
     * @return true if namespace awareness is enabled.
     */
    boolean getTldNamespaceAware();


    /**
     * Set the namespace aware feature of the XML parser used when
     * parsing xml instances.
     * @param tldNamespaceAware true to enable namespace awareness
     */
    void setTldNamespaceAware(boolean tldNamespaceAware);


    // START SJSAS 8.1 5049111
    /**
     * Return <code>true</code> if this context contains the JSF servlet.
     */
    boolean isJsfApplication();
    // END SJSAS 8.1 5049111


    // START SJSAS 6253524
    /**
     * Indicates whether this web module contains any ad-hoc paths.
     *
     * An ad-hoc path is a servlet path that is mapped to a servlet
     * not declared in the web module's deployment descriptor.
     *
     * A web module all of whose mappings are for ad-hoc paths is called an
     * ad-hoc web module.
     *
     * @return true if this web module contains any ad-hoc paths, false
     * otherwise
     */
    boolean hasAdHocPaths();

    /**
     * Returns the name of the ad-hoc servlet responsible for servicing the
     * given path.
     *
     * @param path The path to service
     *
     * @return The name of the ad-hoc servlet responsible for servicing the
     * given path, or null if the given path is not an ad-hoc path
     */
    String getAdHocServletName(String path);
    // END SJSAS 6253524

    /**
     * Indicates whether the Pragma and Cache-Control headers will be set
     * to "No-cache" if proxy caching has been disabled.
     *
     * @return true if Pragma and Cache-Control headers will be set to
     * "No-cache" if proxy caching has been disabled; false otherwise.
     */
    boolean isSecurePagesWithPragma();

    /**
     * Sets the securePagesWithPragma property of this Context.
     *
     * Setting this property to true will result in Pragma and Cache-Control
     * headers with a value of "No-cache" if proxy caching has been disabled.
     *
     * Setting this property to false will not add any Pragma header,
     * but will set the Cache-Control header to "private".
     *
     * @param securePagesWithPragma true if Pragma and Cache-Control headers
     * are to be set to "No-cache" if proxy caching has been disabled, false
     * otherwise
     */
    void setSecurePagesWithPragma(boolean securePagesWithPragma);

    /**
     * Gets the Authenticator of this Context.
     *
     * @return the Authenticator of this Context
     */
    Authenticator getAuthenticator();

    /**
     * Notifies all ServletRequestListener instances configured for this Context
     * of the requestInitialized event.
     *
     * @param request
     */
    public void fireRequestInitializedEvent(ServletRequest request);

    /**
     * Notifies all ServletRequestListener instances configured for this Context
     * of the requestDestroyed event.
     *
     * @param request
     */
    public void fireRequestDestroyedEvent(ServletRequest request);
}
