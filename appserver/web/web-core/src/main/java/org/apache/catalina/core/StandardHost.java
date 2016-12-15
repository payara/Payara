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
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.web.valve.GlassFishValve;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Standard implementation of the <b>Host</b> interface.  Each
 * child container must be a Context implementation to process the
 * requests directed to a particular web application.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.13 $ $Date: 2007/04/17 21:33:22 $
 */

public class StandardHost
    extends ContainerBase
    implements Deployer, Host  
 {
    /* Why do we implement deployer and delegate to deployer ??? */

    
    // ----------------------------------------------------------- Constructors


    /**
     * Create a new StandardHost component with the default basic Valve.
     */
    public StandardHost() {
        pipeline.setBasic(new StandardHostValve());
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The set of aliases for this Host.
     */
    protected String[] aliases = new String[0];


    /**
     * The application root for this Host.
     */
    private String appBase = ".";


    /**
     * The auto deploy flag for this Host.
     */
    private boolean autoDeploy = true;


     /**
      * The broadcaster that sends j2ee notifications.
      */
     private NotificationBroadcasterSupport broadcaster = null;


    /**
     * The Java class name of the default context configuration class
     * for deployed web applications.
     */
    private String configClass =
        "org.apache.catalina.startup.ContextConfig";


    /**
     * The Java class name of the default Context implementation class for
     * deployed web applications.
     */
    private String contextClass =
        "org.apache.catalina.core.StandardContext";

    /**
     * The default context.xml location
     */
    private String defaultContextXmlLocation;

    /**
     * The default web.xml location
     */
    private String defaultWebXmlLocation;

    /**
     * The <code>Deployer</code> to whom we delegate application
     * deployment requests.
     */
    private Deployer deployer = null;


    /**
     * The deploy on startup flag for this Host.
     */
    private boolean deployOnStartup = true;


    /**
     * deploy Context XML config files property.
     */
    private boolean deployXML = true;


    /**
     * The Java class name of the default error reporter implementation class 
     * for deployed web applications.
     */
    private String errorReportValveClass =
        "org.apache.catalina.valves.ErrorReportValve";

    /**
     * The descriptive information string for this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.StandardHost/1.0";


    /**
     * Unpack WARs property.
     */
    private boolean unpackWARs = true;


    /**
     * Work Directory base for applications.
     */
    private String workDir = null;


    /**
     * Attribute value used to turn on/off XML validation
     */
     private boolean xmlValidation = false;


    /**
     * Attribute value used to turn on/off XML namespace awareness.
     */
     private boolean xmlNamespaceAware = false;


    // START SJSAS 6324911
    /**
     * The status code error pages for this StandardHost, keyed by HTTP status
     * code.
     */
    private HashMap<Integer, ErrorPage> statusPages =
        new HashMap<Integer, ErrorPage>();
    // END SJSAS 6324911


    // BEGIN S1AS 5000999
    /**
     * The network listener names with which this StandardHost is associated
     */
    private String[] networkListenerNames = new String[0];
    // END S1AS 5000999


    /**
     * With proxy caching disabled, setting this flag to true adds 
     * Pragma and Cache-Control headers with "No-cache" as value. 
     * Setting this flag to false does not add any Pragma header,
     * but sets the Cache-Control header to "private".
     */
    private boolean securePagesWithPragma = true;
    

    private SingleSignOn sso;


     /**
      * The notification sequence number.
      */
     private long sequenceNumber = 0;

    
    // ------------------------------------------------------------- Properties


    // START SJSAS 6331392
    public void setPipeline(Pipeline pl) {
        StandardHostValve shValve = new StandardHostValve();
        configureStandardHostValve(shValve);
        pl.setBasic(shValve);
        pipeline = pl;
        hasCustomPipeline = true;
    }    
    // END SJSAS 6331392


    /**
     * Return the application root for this Host.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     */
    public String getAppBase() {

        return (this.appBase);

    }


    /**
     * Set the application root for this Host.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     *
     * @param appBase The new application root
     */
    public void setAppBase(String appBase) {

        String oldAppBase = this.appBase;
        this.appBase = appBase;
        support.firePropertyChange("appBase", oldAppBase, this.appBase);

    }


    /**
     * Return the value of the auto deploy flag.  If true, it indicates that 
     * this host's child webapps will be dynamically deployed.
     */
    public boolean getAutoDeploy() {

        return (this.autoDeploy);

    }


    /**
     * Set the auto deploy flag value for this host.
     * 
     * @param autoDeploy The new auto deploy flag
     */
    public void setAutoDeploy(boolean autoDeploy) {

        boolean oldAutoDeploy = this.autoDeploy;
        this.autoDeploy = autoDeploy;
        support.firePropertyChange("autoDeploy", oldAutoDeploy, 
                                   this.autoDeploy);

    }


    /**
     * Return the Java class name of the context configuration class
     * for new web applications.
     */
    public String getConfigClass() {

        return (this.configClass);

    }


    /**
     * Set the Java class name of the context configuration class
     * for new web applications.
     *
     * @param configClass The new context configuration class
     */
    public void setConfigClass(String configClass) {

        String oldConfigClass = this.configClass;
        this.configClass = configClass;
        support.firePropertyChange("configClass",
                                   oldConfigClass, this.configClass);

    }


    /**
     * Return the Java class name of the Context implementation class
     * for new web applications.
     */
    public String getContextClass() {

        return (this.contextClass);

    }


    /**
     * Set the Java class name of the Context implementation class
     * for new web applications.
     *
     * @param contextClass The new context implementation class
     */
    public void setContextClass(String contextClass) {

        String oldContextClass = this.contextClass;
        this.contextClass = contextClass;
        support.firePropertyChange("contextClass",
                                   oldContextClass, this.contextClass);

    }


    /**
     * Return the value of the deploy on startup flag.  If true, it indicates 
     * that this host's child webapps should be discovered and automatically 
     * deployed at startup time.
     */
    public boolean getDeployOnStartup() {

        return (this.deployOnStartup);

    }


    /**
     * Set the deploy on startup flag value for this host.
     * 
     * @param deployOnStartup The new deploy on startup flag
     */
    public void setDeployOnStartup(boolean deployOnStartup) {

        boolean oldDeployOnStartup = this.deployOnStartup;
        this.deployOnStartup = deployOnStartup;
        support.firePropertyChange("deployOnStartup", oldDeployOnStartup, 
                                   this.deployOnStartup);

    }


    /**
     * Deploy XML Context config files flag accessor.
     */
    public boolean isDeployXML() {

        return (deployXML);

    }


    /**
     * Deploy XML Context config files flag mutator.
     */
    public void setDeployXML(boolean deployXML) {

        this.deployXML = deployXML;

    }


    /**
     * Return the Java class name of the error report valve class
     * for new web applications.
     */
    public String getErrorReportValveClass() {

        return (this.errorReportValveClass);

    }


    /**
     * Set the Java class name of the error report valve class
     * for new web applications.
     *
     * @param errorReportValveClass The new error report valve class
     */
    public void setErrorReportValveClass(String errorReportValveClass) {

        String oldErrorReportValveClassClass = this.errorReportValveClass;
        this.errorReportValveClass = errorReportValveClass;
        support.firePropertyChange("errorReportValveClass",
                                   oldErrorReportValveClassClass, 
                                   this.errorReportValveClass);
    }


    /**
     * Return the canonical, fully qualified, name of the virtual host
     * this Container represents.
     */
    @Override
    public String getName() {
        return (name);
    }


    /**
     * Set the canonical, fully qualified, name of the virtual host
     * this Container represents.
     *
     * @param name Virtual host name
     *
     * @exception IllegalArgumentException if name is null
     */
    @Override
    public void setName(String name) {

        if (name == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.HOST_NAME_REQUIRED_EXCEPTION));

        // START OF PE 4989789
        // name = name.toLowerCase();      // Internally all names are lower case
        // END OF PE 4989789

        String oldName = this.name;
        this.name = name;
        support.firePropertyChange("name", oldName, this.name);
    }


    /**
     * Unpack WARs flag accessor.
     */
    public boolean isUnpackWARs() {
        return (unpackWARs);
    }


    /**
     * Unpack WARs flag mutator.
     */
    public void setUnpackWARs(boolean unpackWARs) {

        this.unpackWARs = unpackWARs;

    }

     /**
     * Set the validation feature of the XML parser used when
     * parsing xml instances.
     * @param xmlValidation true to enable xml instance validation
     */
    public void setXmlValidation(boolean xmlValidation){
        this.xmlValidation = xmlValidation;
    }

    /**
     * Get the server.xml <host> attribute's xmlValidation.
     * @return true if validation is enabled.
     *
     */
    public boolean getXmlValidation(){
        return xmlValidation;
    }

    /**
     * Get the server.xml <host> attribute's xmlNamespaceAware.
     * @return true if namespace awareness is enabled.
     *
     */
    public boolean getXmlNamespaceAware(){
        return xmlNamespaceAware;
    }


    /**
     * Set the namespace aware feature of the XML parser used when
     * parsing xml instances.
     * @param xmlNamespaceAware true to enable namespace awareness
     */
    public void setXmlNamespaceAware(boolean xmlNamespaceAware){
        this.xmlNamespaceAware=xmlNamespaceAware;
    }    
    
    /**
     * Host work directory base.
     */
    public String getWorkDir() {
        return (workDir);
    }


    /**
     * Host work directory base.
     */
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }


    // BEGIN S1AS 5000999
    /**
     * Associates this StandardHost with the given network listener names.
     *
     * @param networkListenerNames The network listener names with which to associate this StandardHost
     */
    public void setNetworkListenerNames(String[] networkListenerNames) {
        String[] oldListenerNames = this.networkListenerNames;
        this.networkListenerNames = networkListenerNames.clone();
        support.firePropertyChange("ports", oldListenerNames, this.networkListenerNames);
    }


    /**
     * Gets the network listener names with which this StandardHost is associated.
     *
     * @return The network listener names with which this StandardHost is associated,
     * or null if this StandardHost has not been associated with any ports
     */
    public String[] getNetworkListenerNames() {
        return this.networkListenerNames.clone();
    }
    // END S1AS 5000999


    public String getNetworkListeners() {
        List<String> list = Arrays.asList(networkListenerNames);
        String listeners = null;
        if (list.size() > 0) {
            listeners = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                listeners = list.get(i) + "," + listeners ;
            }
        }
        return listeners;
    }

     /**
     * Gets the default-context.xml location of web modules deployed on this
     * virtual server.
     *
     * @return default-context.xml location of web modules deployed on this
     * virtual server
     */
    public String getDefaultContextXmlLocation() {
        return defaultContextXmlLocation;
    }

    /**
     * Sets the default-context.xml location for web modules deployed on this
     * virtual server.
     *
     * @param defaultContextXmlLocation default-context.xml location for web modules
     * deployed on this virtual server
     */
    public void setDefaultContextXmlLocation(String defaultContextXmlLocation) {
        this.defaultContextXmlLocation = defaultContextXmlLocation;
    }

    /**
     * Gets the default-web.xml location of web modules deployed on this
     * virtual server.
     *
     * @return default-web.xml location of web modules deployed on this
     * virtual server
     */
    public String getDefaultWebXmlLocation() {
        return defaultWebXmlLocation;
    }

    /**
     * Sets the default-web.xml location for web modules deployed on this
     * virtual server.
     *
     * @param defaultWebXmlLocation default-web.xml location for web modules
     * deployed on this virtual server
     */
    public void setDefaultWebXmlLocation(String defaultWebXmlLocation) {
        this.defaultWebXmlLocation = defaultWebXmlLocation;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Add an alias name that should be mapped to this same Host.
     *
     * @param alias The alias to be added
     */
    public void addAlias(String alias) {

        // START OF PE 4989789
        //alias = alias.toLowerCase();
        // START OF PE 4989789
        
        // Skip duplicate aliases
        for(String name : aliases) {
            if(name.equals(alias)) {
                return;
            }
        }

        // Add this alias to the list
        String newAliases[] = new String[aliases.length + 1];
        for (int i = 0; i < aliases.length; i++)
            newAliases[i] = aliases[i];
        newAliases[aliases.length] = alias;

        aliases = newAliases;

        // Inform interested listeners
        fireContainerEvent(ADD_ALIAS_EVENT, alias);

    }


    /**
     * Add a child Container, only if the proposed child is an implementation
     * of Context.
     *
     * @param child Child container to be added
     */
    @Override
    public void addChild(Container child) {

        if (!(child instanceof Context))
            throw new IllegalArgumentException
                       (rb.getString(LogFacade.CHILD_MUST_BE_CONTEXT_EXCEPTION));
        super.addChild(child);

    }


    // START GlassFish 862
    /**
     * Returns the context deployed at the given context root.
     *
     * @param contextRoot The context root whose associated context to return
     *
     * @return The context deployed at the given context root, or null
     */
    @Override
    public Container findChild(String contextRoot) {
        return super.findChild(RequestUtil.urlDecode(contextRoot, "UTF-8"));
    }
    // END GlassFish 862


    /**
     * Return the set of alias names for this Host.  If none are defined,
     * a zero length array is returned.
     */
    public String[] findAliases() {

        return (this.aliases);

    }


    public Host findMappingObject() {
        return (Host) getMappingObject();
    }


    /**
     * Return descriptive information about this Container implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    @Override
    public String getInfo() {

        return (info);

    }


    /**
     * Return the Context that would be used to process the specified
     * host-relative request URI, if any; otherwise return <code>null</code>.
     *
     * @param uri Request URI to be mapped
     */
    public Context map(String uri) {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Mapping request URI '" + uri + "'");
        if (uri == null)
            return (null);

        // Match on the longest possible context path prefix
        if (log.isLoggable(Level.FINEST))
            log.log(Level.FINEST, "Trying the longest context path prefix");
        Context context = null;
        String mapuri = uri;
        while (true) {
            context = (Context) findChild(mapuri);
            if (context != null)
                break;
            int slash = mapuri.lastIndexOf('/');
            if (slash < 0)
                break;
            mapuri = mapuri.substring(0, slash);
        }

        // If no Context matches, select the default Context
        if (context == null) {
            if (log.isLoggable(Level.FINEST))
                log.log(Level.FINEST, "Trying the default context");
            context = (Context) findChild("");
        }

        // Complain if no Context has been selected
        if (context == null) {
            log.log(Level.SEVERE, LogFacade.MAPPING_CONF_REQUEST_URI_EXCEPTION, uri);
            return (null);
        }

        // Return the mapped Context (if any)
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, " Mapped to context '" + context.getPath() + "'");
        return (context);

    }


    /**
     * Remove the specified alias name from the aliases for this Host.
     *
     * @param alias Alias name to be removed
     */
    public void removeAlias(String alias) {

        // START OF PE 4989789
        //alias = alias.toLowerCase();
        // START OF PE 4989789
        
        // Make sure this alias is currently present
        int n = -1;
        for (int i = 0; i < aliases.length; i++) {
            if (aliases[i].equals(alias)) {
                n = i;
                break;
            }
        }
        if (n < 0) {
            return;
        }

        // Remove the specified alias
        int j = 0;
        String results[] = new String[aliases.length - 1];
        for (int i = 0; i < aliases.length; i++) {
            if (i != n) {
                results[j++] = aliases[i];
            }
        }
        aliases = results;

        // Inform interested listeners
        fireContainerEvent(REMOVE_ALIAS_EVENT, alias);

    }


    // START SJSAS 6324911
    /**
     * Adds the given error page to this StandardHost.
     *
     * @param errorPage The error page definition to be added
     */
    public void addErrorPage(ErrorPage errorPage) {
        // Validate the input parameters
        if (errorPage == null) {
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.ERROR_PAGE_CANNOT_BE_NULL_EXCEPTION));
        }

        // Add the specified error page to our internal collections
        synchronized (statusPages) {
            statusPages.put(errorPage.getErrorCode(), errorPage);
        }
        fireContainerEvent("addErrorPage", errorPage);

    }

    /**
     * Gets the error page for the specified HTTP error code.
     *
     * @param errorCode Error code to look up
     *
     * @return The error page that is mapped to the specified HTTP error
     * code, or null if no error page exists for that HTTP error code
     */
    public ErrorPage findErrorPage(int errorCode) {
        return statusPages.get(Integer.valueOf(errorCode));
    }
    // END SJSAS 6324911


    /**
     * Configures the Secure attribute of the given SSO cookie.
     *
     * @param cookie the SSO cookie to be configured
     * @param hreq the HttpServletRequest that has initiated the SSO session
     */
    public void configureSingleSignOnCookieSecure(Cookie cookie,
                                                  HttpServletRequest hreq) {
        cookie.setSecure(hreq.isSecure());
    }


    /**
     * Configures the HttpOnly attribute of the given SSO cookie.
     *
     * @param cookie the SSO ccokie to be configured
     */
    public void configureSingleSignOnCookieHttpOnly(Cookie cookie) {
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
        sb.append("StandardHost[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

    }


    /**
     * Start this host.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    @Override
    public synchronized void start() throws LifecycleException {
        if( started ) {
            return;
        }
        if( ! initialized )
            init();

        // Set error report valve
        configureStandardHostValve((StandardHostValve) pipeline.getBasic());

        // START SJSAS_PE 8.1 5034793
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, LogFacade.XML_VALIDATION_ENABLED);
        }
        // END SJSAS_PE 8.1 5034793 

        super.start();

    }

     @Override
     public synchronized void stop() throws LifecycleException {
         super.stop();

         if(oname != null) {
             // Send j2ee.object.deleted notification
             Notification notification =
                     new Notification("j2ee.object.deleted", this, sequenceNumber++);
             sendNotification(notification);
         }
     }

     public void sendNotification(Notification notification) {

         if (broadcaster == null) {
             broadcaster = ((StandardEngine)getParent()).getService().getBroadcaster();
         }
         if (broadcaster != null) {
             broadcaster.sendNotification(notification);
         }
         return;
     }


    // ------------------------------------------------------- Deployer Methods


    /**
     * Install a new web application, whose web application archive is at the
     * specified URL, into this container with the specified context path.
     * A context path of "" (the empty string) should be used for the root
     * application for this container.  Otherwise, the context path must
     * start with a slash.
     * <p>
     * If this application is successfully installed, a ContainerEvent of type
     * <code>INSTALL_EVENT</code> will be sent to all registered listeners,
     * with the newly created <code>Context</code> as an argument.
     *
     * @param contextPath The context path to which this application should
     *  be installed (must be unique)
     * @param war A URL of type "jar:" that points to a WAR file, or type
     *  "file:" that points to an unpacked directory structure containing
     *  the web application to be installed
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalStateException if the specified context path
     *  is already attached to an existing web application
     * @exception IOException if an input/output error was encountered
     *  during install
     */
    public void install(String contextPath, URL war) throws IOException {
        getDeployer().install(contextPath, war);

    }


    /**
     * <p>Install a new web application, whose context configuration file
     * (consisting of a <code>&lt;Context&gt;</code> element) and web
     * application archive are at the specified URLs.</p>
     *
     * <p>If this application is successfully installed, a ContainerEvent
     * of type <code>INSTALL_EVENT</code> will be sent to all registered
     * listeners, with the newly created <code>Context</code> as an argument.
     * </p>
     *
     * @param config A URL that points to the context configuration file to
     *  be used for configuring the new Context
     * @param war A URL of type "jar:" that points to a WAR file, or type
     *  "file:" that points to an unpacked directory structure containing
     *  the web application to be installed
     *
     * @exception IllegalArgumentException if one of the specified URLs is
     *  null
     * @exception IllegalStateException if the context path specified in the
     *  context configuration file is already attached to an existing web
     *  application
     * @exception IOException if an input/output error was encountered
     *  during installation
     */
    public synchronized void install(URL config, URL war) throws IOException {

        getDeployer().install(config, war);

    }


    /**
     * Return the Context for the deployed application that is associated
     * with the specified context path (if any); otherwise return
     * <code>null</code>.
     *
     * @param contextPath The context path of the requested web application
     */
    public Context findDeployedApp(String contextPath) {

        return (getDeployer().findDeployedApp(contextPath));

    }


    /**
     * Return the context paths of all deployed web applications in this
     * Container.  If there are no deployed applications, a zero-length
     * array is returned.
     */
    public String[] findDeployedApps() {

        return (getDeployer().findDeployedApps());

    }


    /**
     * Remove an existing web application, attached to the specified context
     * path.  If this application is successfully removed, a
     * ContainerEvent of type <code>REMOVE_EVENT</code> will be sent to all
     * registered listeners, with the removed <code>Context</code> as
     * an argument.
     *
     * @param contextPath The context path of the application to be removed
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalArgumentException if the specified context path does
     *  not identify a currently installed web application
     * @exception IOException if an input/output error occurs during
     *  removal
     */
    public void remove(String contextPath) throws IOException {

        getDeployer().remove(contextPath);

    }


    /**
     * Remove an existing web application, attached to the specified context
     * path.  If this application is successfully removed, a
     * ContainerEvent of type <code>REMOVE_EVENT</code> will be sent to all
     * registered listeners, with the removed <code>Context</code> as
     * an argument. Deletes the web application war file and/or directory
     * if they exist in the Host's appBase.
     *
     * @param contextPath The context path of the application to be removed
     * @param undeploy boolean flag to remove web application from server
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalArgumentException if the specified context path does
     *  not identify a currently installed web application
     * @exception IOException if an input/output error occurs during
     *  removal
     */
    public void remove(String contextPath, boolean undeploy) throws IOException {

        getDeployer().remove(contextPath,undeploy);

    }


    /**
     * Start an existing web application, attached to the specified context
     * path.  Only starts a web application if it is not running.
     *
     * @param contextPath The context path of the application to be started
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalArgumentException if the specified context path does
     *  not identify a currently installed web application
     * @exception IOException if an input/output error occurs during
     *  startup
     */
    public void start(String contextPath) throws IOException {

        getDeployer().start(contextPath);

    }


    /**
     * Stop an existing web application, attached to the specified context
     * path.  Only stops a web application if it is running.
     *
     * @param contextPath The context path of the application to be stopped
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalArgumentException if the specified context path does
     *  not identify a currently installed web application
     * @exception IOException if an input/output error occurs while stopping
     *  the web application
     */
    public void stop(String contextPath) throws IOException {

        getDeployer().stop(contextPath);

    }

    
    /**
     * Returns the value of the securePagesWithPragma property.
     */
    public boolean isSecurePagesWithPragma() {

        return (this.securePagesWithPragma);
    }


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
    public void setSecurePagesWithPragma(boolean securePagesWithPragma) {

        boolean oldSecurePagesWithPragma = this.securePagesWithPragma;
        this.securePagesWithPragma = securePagesWithPragma;
        support.firePropertyChange("securePagesWithPragma",
                                   Boolean.valueOf(oldSecurePagesWithPragma),
                                   Boolean.valueOf(this.securePagesWithPragma));
    }


    @Override
    public void addValve(GlassFishValve valve) {
        super.addValve(valve);
        if (valve instanceof SingleSignOn) {
            sso = (SingleSignOn) valve;
        }
    }


    @Override
    public void removeValve(GlassFishValve valve) {
        super.removeValve(valve);
        if (valve instanceof SingleSignOn) {
            sso = null;
        }
    }


    public SingleSignOn getSingleSignOn() {
        return sso;
    }


    // ------------------------------------------------------ Protected Methods


    static String STANDARD_HOST_DEPLOYER="org.apache.catalina.core.StandardHostDeployer";
    
    public Deployer getDeployer() {
        if( deployer!= null )
            return deployer;
        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.CREATE_HOST_DEPLOYER_INFO);
        }
        try {
            Class<?> c=Class.forName( STANDARD_HOST_DEPLOYER );
            deployer=(Deployer)c.newInstance();
            Method m=c.getMethod("setHost", new Class[] {Host.class} );
            m.invoke( deployer,  new Object[] { this } );
        } catch( Throwable t ) {
            log.log(Level.SEVERE, LogFacade.ERROR_CREATING_DEPLOYER_EXCEPTION, t);
        }
        return deployer;
    }
    
    public void setDeployer(Deployer d) {
        this.deployer=d;
    }

    // -------------------- JMX  --------------------
    /**
      * Return the MBean Names of the Valves associated with this Host
      *
      * @exception Exception if an MBean cannot be created or registered
      */
     public String [] getValveNames()
         throws Exception
    {
         GlassFishValve [] valves = this.getValves();
         String [] mbeanNames = new String[valves.length];
         for (int i = 0; i < valves.length; i++) {
             if( valves[i] == null ) continue;
             if( ((ValveBase)valves[i]).getObjectName() == null ) continue;
             mbeanNames[i] = ((ValveBase)valves[i]).getObjectName().toString();
         }

         return mbeanNames;

     }

    public String[] getAliases() {
        return aliases;
    }

    /* CR 6368085
    private boolean initialized=false;
    */    

    @Override
    public void init() {
        if( initialized ) return;
        /* CR 6368085
        initialized=true;
        */
        
        if( oname==null ) {
            // not registered in JMX yet - standalone mode
            try {
                StandardEngine engine=(StandardEngine)parent;
                domain=engine.getName();
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Registering host " + getName()
                            + " with domain " + domain);
                }
                oname=new ObjectName(domain + ":type=Host,host=" +
                        this.getName());
                // START CR 6368091
                controller = oname;
                // END CR 6368091
                Notification notification =
                        new Notification("j2ee.object.created", this, sequenceNumber++);
                sendNotification(notification);
            } catch(Throwable t) {
                String msg = MessageFormat.format(rb.getString(LogFacade.ERROR_REGISTERING_HOST_EXCEPTION), getName());
                log.log(Level.SEVERE, msg, t);
            }
        }
        // START CR 6368085
        initialized = true;
        // END CR 6368085
    }
    
    @Override
    public ObjectName createObjectName(String domain, ObjectName parent)
        throws Exception
    {
        if( log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Create ObjectName " + domain + " " + parent);
        return new ObjectName( domain + ":type=Host,host=" + getName());
    }

    protected Object loadInstance(String className) throws Exception {
        return Class.forName(className).newInstance();
    }


    // ------------------------------------------------------ Private Methods


    private void configureStandardHostValve(StandardHostValve host) {
        // Set error report valve
        if ((errorReportValveClass != null)
            && !"".equals(errorReportValveClass)) {
            try {
                GlassFishValve valve = (GlassFishValve)
                    loadInstance(errorReportValveClass);
                /* START SJSAS 6374691
                addValve(valve);
                */
                // START SJSAS 6374691
                host.setErrorReportValve(valve);
                // END SJSAS 6374691
            } catch (Throwable t) {
                String msg = MessageFormat.format(rb.getString(LogFacade.LOAD_SPEC_ERROR_REPORT_EXCEPTION),
                                                  errorReportValveClass);
                log.log(Level.SEVERE, msg, t);
            }
        }
    }
}
