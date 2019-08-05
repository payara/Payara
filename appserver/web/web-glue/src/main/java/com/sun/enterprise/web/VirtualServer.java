/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.web;

import static com.sun.enterprise.security.common.AppservAccessController.privileged;
import static com.sun.enterprise.web.Constants.DEFAULT_WEB_MODULE_NAME;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.glassfish.api.web.Constants.ADMIN_VS;
import static org.glassfish.web.LogFacade.IGNORE_INVALID_REALM;
import static org.glassfish.web.LogFacade.INVALID_AUTH_REALM;
import static org.glassfish.web.LogFacade.INVALID_LISTENER_VIRTUAL_SERVER;
import static org.glassfish.web.LogFacade.NOT_A_VALVE;
import static org.glassfish.web.LogFacade.UNABLE_RECONFIGURE_ACCESS_LOG;
import static org.glassfish.web.LogFacade.UNABLE_TO_LOAD_EXTENSION_SEVERE;
import static org.glassfish.web.LogFacade.VS_ADDED_CONTEXT;
import static org.glassfish.web.LogFacade.VS_DEFAULT_WEB_MODULE;
import static org.glassfish.web.LogFacade.VS_DEFAULT_WEB_MODULE_DISABLED;
import static org.glassfish.web.LogFacade.VS_DEFAULT_WEB_MODULE_NOT_FOUND;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.valves.RemoteAddrValve;
import org.apache.catalina.valves.RemoteHostValve;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.ApplicationConfigInfo;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.web.ConfigException;
import org.glassfish.embeddable.web.Context;
import org.glassfish.embeddable.web.WebListener;
import org.glassfish.embeddable.web.config.VirtualServerConfig;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ConnectionProbe;
import org.glassfish.grizzly.config.GenericGrizzlyListener;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpCodecFilter;
import org.glassfish.grizzly.http.HttpPacket;
import org.glassfish.grizzly.http.HttpProbe;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.web.LogFacade;
import org.glassfish.web.admin.monitor.RequestProbeProvider;
import org.glassfish.web.deployment.archivist.WebArchivist;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.glassfish.web.loader.WebappClassLoader;
import org.glassfish.web.valve.GlassFishValve;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.security.web.GlassFishSingleSignOn;
import com.sun.enterprise.server.logging.GFFileHandler;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.admin.report.PlainTextActionReporter;
import com.sun.enterprise.v3.services.impl.GrizzlyProxy;
import com.sun.enterprise.v3.services.impl.GrizzlyService;
import com.sun.enterprise.web.logger.CatalinaLogger;
import com.sun.enterprise.web.logger.FileLoggerHandler;
import com.sun.enterprise.web.logger.FileLoggerHandlerFactory;
import com.sun.enterprise.web.pluggable.WebContainerFeatureFactory;
import com.sun.enterprise.web.session.SessionCookieConfig;
import com.sun.web.security.RealmAdapter;

/**
 * Standard implementation of a virtual server (aka virtual host) in the Payara Server.
 */
public class VirtualServer extends StandardHost implements org.glassfish.embeddable.web.VirtualServer {

    private static final String SSO_MAX_IDLE = "sso-max-inactive-seconds";
    private static final String SSO_REAP_INTERVAL = "sso-reap-interval-seconds";
    private static final String DISABLED = "disabled";
    private static final String ON = "on";
    
    /**
     * The descriptive information about this implementation.
     */
    private static final String _info = "com.sun.enterprise.web.VirtualServer/1.0";

    /**
     * The logger to use for logging this virtual server
     */
    private static final Logger DEFAULT_LOGGER = LogFacade.getLogger();

    /**
     * The resource bundle containing the message strings for _logger.
     */
    protected static final ResourceBundle rb = DEFAULT_LOGGER.getResourceBundle();

    // ------------------------------------------------------------ Constructor

    /**
     * Default constructor that simply gets a handle to the web container subsystem's logger.
     */// XXX: WebContainer.createHost is the only time many of the set methods in Virtual Server
       // are used, they might be movable to the constructor or injected
    public VirtualServer() {
        origPipeline = pipeline;
        vsPipeline = new VirtualServerPipeline(this);
        accessLogValve = new PEAccessLogValve();
        accessLogValve.setContainer(this);
    }

    // ----------------------------------------------------- Instance Variables

    /*
     * The custom pipeline of this VirtualServer, which implements the following virtual server features:
     *
     * - state (disabled/off) - redirects
     */
    private VirtualServerPipeline vsPipeline;

    /*
     * The original (standard) pipeline of this VirtualServer.
     *
     * Only one (custom or original) pipeline may be active at any given time. Any updates (such as adding or removing
     * valves) to the currently active pipeline are propagated to the other.
     */
    private Pipeline origPipeline;

    /**
     * The id of this virtual server as specified in the configuration.
     */
    private String _id;

    /**
     * The logger to use for logging this virtual server
     */
    protected Logger _logger = DEFAULT_LOGGER;

    /**
     * The config bean associated with this VirtualServer
     */
    private com.sun.enterprise.config.serverbeans.VirtualServer vsBean;

    /**
     * The mime mapping associated with this VirtualServer
     */
    private MimeMap mimeMap;

    /*
     * Indicates whether symbolic links from this virtual server's docroot are followed. This setting is inherited by all
     * web modules deployed on this virtual server, unless overridden by a web modules allowLinking property in sun-web.xml.
     */
    private boolean allowLinking;
    private String[] cacheControls;
    private ClassLoaderHierarchy classLoaderHierarchy;
    private Domain domain;
    private ServiceLocator services;

    // Is this virtual server active?
    private boolean isActive;
    private String authRealmName;

    /*
     * The accesslog valve of this VirtualServer.
     *
     * This valve is activated, that is, added to this virtual server's pipeline, only when access logging has been enabled.
     * When acess logging has been disabled, this valve is removed from this virtual server's pipeline.
     */
    private PEAccessLogValve accessLogValve;

    // The value of the ssoCookieSecure property
    private String ssoCookieSecure;

    private boolean ssoCookieHttpOnly;
    private String defaultContextPath;
    private ServerContext serverContext;
    private Config serverConfig;
    private GrizzlyService grizzlyService;
    private WebContainer webContainer;
    private boolean ssoFailoverEnabled;

    private volatile FileLoggerHandler fileLoggerHandler;
    private volatile FileLoggerHandlerFactory fileLoggerHandlerFactory;

    private Deployment deployment;
    private ArchiveFactory factory;
    private ActionReport report;

    // ------------------------------------------------------------- Properties

    /**
     * Return the virtual server identifier.
     */
    @Override
    public String getID() {
        return _id;
    }

    /**
     * Set the virtual server identifier string.
     *
     * @param id New identifier for this virtual server
     */
    @Override
    public void setID(String id) {
        _id = id;
    }

    /**
     * Sets the state of this virtual server.
     *
     * @param isActive true if this virtual server is active, false otherwise
     */
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
        if (isActive) {
            vsPipeline.setIsDisabled(false);
            vsPipeline.setIsOff(false);
            if (pipeline == vsPipeline && !vsPipeline.hasRedirects()) {
                // Restore original pipeline
                setPipeline(origPipeline);
            }
        }
    }

    /**
     * Gets the value of the allowLinking property of this virtual server.
     *
     * @return true if symbolic links from this virtual server's docroot (as well as symbolic links from archives of web
     * modules deployed on this virtual server) are followed, false otherwise
     */
    public boolean getAllowLinking() {
        return allowLinking;
    }

    /**
     * Sets the allowLinking property of this virtual server, which determines whether symblic links from this virtual
     * server's docroot are followed.
     *
     * This property is inherited by all web modules deployed on this virtual server, unless overridden by the allowLinking
     * property in a web module's sun-web.xml.
     *
     * @param allowLinking Value of allowLinking property
     */
    public void setAllowLinking(boolean allowLinking) {
        this.allowLinking = allowLinking;
    }

    /**
     * Gets the config bean associated with this VirtualServer.
     * 
     * @return
     */
    public com.sun.enterprise.config.serverbeans.VirtualServer getBean() {
        return vsBean;
    }

    /**
     * Sets the config bean for this VirtualServer
     * 
     * @param vsBean
     */
    public void setBean(com.sun.enterprise.config.serverbeans.VirtualServer vsBean) {
        this.vsBean = vsBean;
    }

    /**
     * Gets the mime map associated with this VirtualServer.
     */
    public MimeMap getMimeMap() {
        return mimeMap;
    }

    /**
     * Sets the mime map for this VirtualServer
     * 
     * @param mimeMap
     */
    public void setMimeMap(MimeMap mimeMap) {
        this.mimeMap = mimeMap;
    }

    /**
     * Gets the Cache-Control configuration of this VirtualServer.
     *
     * @return Cache-Control configuration of this VirtualServer, or null if no such configuration exists for this
     * VirtualServer
     */
    public String[] getCacheControls() {
        return cacheControls;
    }

    /**
     * Sets the Cache-Control configuration for this VirtualServer
     *
     * @param cacheControls Cache-Control configuration settings for this VirtualServer
     */
    public void setCacheControls(String[] cacheControls) {
        this.cacheControls = cacheControls;
    }

    public void setServices(ServiceLocator services) {
        this.services = services;
    }

    public String getInfo() {
        return _info;
    }

    public void setDefaultContextPath(String defaultContextPath) {
        this.defaultContextPath = defaultContextPath;
    }

    public void setFileLoggerHandlerFactory(FileLoggerHandlerFactory factory) {
        fileLoggerHandlerFactory = factory;
    }

    public void setClassLoaderHierarchy(ClassLoaderHierarchy clh) {
        this.classLoaderHierarchy = clh;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    @Override
    public Container findChild(String contextRoot) {
        if (defaultContextPath != null && "/".equals(contextRoot)) {
            return super.findChild(defaultContextPath);
        }
        
        return super.findChild(contextRoot);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Configures the Secure attribute of the given SSO cookie.
     *
     * @param ssoCookie the SSO cookie to be configured
     * @param hreq the HttpServletRequest that has initiated the SSO session
     */
    @Override
    public void configureSingleSignOnCookieSecure(Cookie ssoCookie, HttpServletRequest hreq) {
        super.configureSingleSignOnCookieSecure(ssoCookie, hreq);
        if (ssoCookieSecure != null && !ssoCookieSecure.equals(SessionCookieConfig.DYNAMIC_SECURE)) {
            ssoCookie.setSecure(Boolean.parseBoolean(ssoCookieSecure));
        }
    }

    @Override
    public void configureSingleSignOnCookieHttpOnly(Cookie ssoCookie) {
        ssoCookie.setHttpOnly(ssoCookieHttpOnly);
    }

    // ------------------------------------------------------ Lifecycle Methods

    /**
     * Adds the given valve to the currently active pipeline, keeping the pipeline that is not currently active in sync.
     */
    @Override
    public synchronized void addValve(GlassFishValve valve) {
        super.addValve(valve);
        if (pipeline == vsPipeline) {
            origPipeline.addValve(valve);
        } else {
            vsPipeline.addValve(valve);
        }
    }

    /**
     * Adds the given Tomcat-style valve to the currently active pipeline, keeping the pipeline that is not currently active
     * in sync.
     * 
     * @param valve
     */
    @Override
    public synchronized void addValve(Valve valve) {
        super.addValve(valve);
        if (pipeline == vsPipeline) {
            origPipeline.addValve(valve);
        } else {
            vsPipeline.addValve(valve);
        }
    }

    /**
     * Removes the given valve from the currently active pipeline, keeping the valve that is not currently active in sync.
     */
    @Override
    public synchronized void removeValve(GlassFishValve valve) {
        super.removeValve(valve);
        if (pipeline == vsPipeline) {
            origPipeline.removeValve(valve);
        } else {
            vsPipeline.removeValve(valve);
        }
    }

    private ConfigBeansUtilities getConfigBeansUtilities() {
        if (services == null) {
            return null;
        }
        
        return services.getService(ConfigBeansUtilities.class);
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Gets the context root of the web module that the user/configuration has designated as the default-web-module for this
     * virtual server.
     *
     * The default-web-module for a virtual server is specified via the 'default-web-module' attribute of the
     * 'virtual-server' element in server.xml. This is an optional attribute and if the configuration does not specify
     * another web module (standalone or part of a j2ee-application) that is configured at a context-root="", then a default
     * web module will be created and loaded. The value for this attribute is either "${standalone-web-module-name}" or
     * "${j2ee-app-name}:${web-module-uri}".
     *
     * @param domain
     * @param appRegistry
     * @return null if the default-web-module has not been specified or if the web module specified either could not be
     * found or is disabled or does not specify this virtual server (if it specifies a value for the virtual-servers
     * attribute) or if there was an error loading its deployment descriptors.
     */
    protected String getDefaultContextPath(Domain domain, ApplicationRegistry appRegistry) {

        String contextRoot = null;
        String defaultWebModuleId = getDefaultWebModuleID();

        if (defaultWebModuleId != null) {
            // Check if the default-web-module is part of a
            // j2ee-application
            Applications appsBean = domain.getApplications();
            WebModuleConfig webModuleConfig = findWebModuleInJ2eeApp(appsBean, defaultWebModuleId, appRegistry);
            if (webModuleConfig == null) {
                ConfigBeansUtilities configBeansUtilities = getConfigBeansUtilities();
                if (configBeansUtilities == null) {
                    contextRoot = null;
                } else {
                    contextRoot = configBeansUtilities.getContextRoot(defaultWebModuleId);
                }
            } else {
                contextRoot = webModuleConfig.getContextPath();
            }

            if (contextRoot == null) {
                _logger.log(SEVERE, VS_DEFAULT_WEB_MODULE_NOT_FOUND, new Object[] { defaultWebModuleId, getID() });
            }
        }

        return contextRoot;
    }

    protected WebModuleConfig getDefaultWebModule(Domain domain, WebArchivist webArchivist, ApplicationRegistry appRegistry) {

        WebModuleConfig webModuleConfig = null;

        String defaultWebModuleId = getDefaultWebModuleID();
        if (defaultWebModuleId != null) {
            
            // Check if the default-web-module is part of an ee-application
            webModuleConfig = findWebModuleInJ2eeApp(domain.getApplications(), defaultWebModuleId, appRegistry);
            if (webModuleConfig == null) {
                String contextRoot = null;
                String location = null;
                
                ConfigBeansUtilities configBeansUtilities = getConfigBeansUtilities();
                if (configBeansUtilities != null) {
                    contextRoot = configBeansUtilities.getContextRoot(defaultWebModuleId);
                    location = configBeansUtilities.getLocation(defaultWebModuleId);
                }

                if (contextRoot != null && location != null) {
                    WebBundleDescriptorImpl webBundleDescriptorImpl = webArchivist.getDefaultWebXmlBundleDescriptor();
                    webBundleDescriptorImpl.setName(DEFAULT_WEB_MODULE_NAME);
                    webBundleDescriptorImpl.setContextRoot(contextRoot);
                    
                    webModuleConfig = new WebModuleConfig();
                    webModuleConfig.setLocation(new File(location));
                    webModuleConfig.setDescriptor(webBundleDescriptorImpl);
                    webModuleConfig.setParentLoader(EmbeddedWebContainer.class.getClassLoader());
                    webModuleConfig.setAppClassLoader(privileged(() -> 
                        new WebappClassLoader(EmbeddedWebContainer.class.getClassLoader(), webBundleDescriptorImpl.getApplication())));
                }
            }

            if (webModuleConfig == null) {
                _logger.log(SEVERE, VS_DEFAULT_WEB_MODULE_NOT_FOUND, new Object[] { defaultWebModuleId, getID() });
            }
        }

        return webModuleConfig;
    }

    /**
     * If a default web module has not yet been configured and added to this virtual server's list of web modules then
     * return the configuration information needed in order to create a default web module for this virtual server.
     *
     * <p>
     * This method should be invoked only after all the standalone modules and the modules within j2ee-application elements
     * have been added to this virtual server's list of modules (only then will one know whether the user has already
     * configured a default web module or not).
     * 
     * @param webArchivist
     * @return
     */
    public WebModuleConfig createSystemDefaultWebModuleIfNecessary(WebArchivist webArchivist) {
        WebModuleConfig webModuleConfig = null;

        // Add a default context only if one hasn't already been loaded
        // and then too only if docroot is not null
        //
        String docroot = getAppBase();
        if (getDefaultWebModuleID() == null && findChild("") == null && docroot != null) {
            WebBundleDescriptorImpl webBundleDescriptor = webArchivist.getDefaultWebXmlBundleDescriptor();
            webBundleDescriptor.setModuleID(DEFAULT_WEB_MODULE_NAME);
            webBundleDescriptor.setContextRoot("");

            SecurityService securityService = Globals.get(SecurityService.class);
            webBundleDescriptor.getLoginConfiguration().setRealmName(securityService.getDefaultRealm());

            webModuleConfig = new WebModuleConfig();
            webModuleConfig.setLocation(new File(docroot));
            webModuleConfig.setDescriptor(webBundleDescriptor);
            webModuleConfig.setParentLoader(serverContext.getCommonClassLoader());
            
            WebappClassLoader loader = privileged(() -> new WebappClassLoader(serverContext.getCommonClassLoader(), getApplication(webBundleDescriptor)));
            loader.start();
            webModuleConfig.setAppClassLoader(loader);
        }

        return webModuleConfig;
    }
    
    private Application getApplication(WebBundleDescriptorImpl webBundleDescriptor) {
        if (webBundleDescriptor.getApplication() == null) {
            Application application = Application.createApplication();
            application.setVirtual(true);
            application.setName(DEFAULT_WEB_MODULE_NAME);
            
            webBundleDescriptor.setApplication(application);
        }
        
        return webBundleDescriptor.getApplication();
    }

    /**
     * Returns the id of the default web module for this virtual server as specified in the 'default-web-module' attribute
     * of the 'virtual-server' element.
     * 
     * @return
     */
    protected String getDefaultWebModuleID() {
        String webModuleId = vsBean.getDefaultWebModule();
        if ("".equals(webModuleId)) {
            webModuleId = null;
        }
        
        if (webModuleId != null && _logger.isLoggable(FINE)) {
            _logger.log(FINE, VS_DEFAULT_WEB_MODULE, new Object[] { webModuleId, _id });
        }

        return webModuleId;
    }

    /**
     * Finds and returns information about a web module embedded within a J2EE application, which is identified by a string
     * of the form <code>a:b</code> or <code>a#b</code>, where <code>a</code> is the name of the J2EE application and
     * <code>b</code> is the name of the embedded web module.
     *
     * @param appsBean
     * @param id
     * @return null if <code>id</code> does not identify a web module embedded within a J2EE application.
     */
    protected WebModuleConfig findWebModuleInJ2eeApp(Applications appsBean, String id, ApplicationRegistry appRegistry) {

        WebModuleConfig webModuleConfig = null;

        // Check for ':' separator
        int separatorIndex = id.indexOf(Constants.NAME_SEPARATOR);
        if (separatorIndex == -1) {
            // Check for '#' separator
            separatorIndex = id.indexOf('#');
        }
        
        if (separatorIndex != -1) {
            String appID = id.substring(0, separatorIndex);
            String moduleID = id.substring(separatorIndex + 1);

            com.sun.enterprise.config.serverbeans.Application appBean = appsBean.getModule(com.sun.enterprise.config.serverbeans.Application.class, appID);

            if ((appBean != null) && Boolean.valueOf(appBean.getEnabled())) {
                String location = appBean.getLocation();
                String moduleDir = DeploymentUtils.getRelativeEmbeddedModulePath(location, moduleID);

                ApplicationInfo appInfo = appRegistry.get(appID);
                Application app = appInfo != null ? appInfo.getMetaData(Application.class) : null;
                if (appInfo == null) {
                    // XXX ApplicaionInfo is NULL after restart
                    _logger.log(Level.SEVERE, LogFacade.VS_DEFAULT_WEB_MODULE_DISABLED, new Object[] { id, getID() });
                    return webModuleConfig;
                }

                WebBundleDescriptorImpl webBundleDescriptorImpl = app.getModuleByTypeAndUri(WebBundleDescriptorImpl.class, moduleID);
                String webUri = webBundleDescriptorImpl.getModuleDescriptor().getArchiveUri();
                String contextRoot = webBundleDescriptorImpl.getModuleDescriptor().getContextRoot();
                
                if (moduleID.equals(webUri)) {
                    webBundleDescriptorImpl.setName(moduleID);
                    webBundleDescriptorImpl.setContextRoot(contextRoot);
                    
                    webModuleConfig = new WebModuleConfig();
                    webModuleConfig.setDescriptor(webBundleDescriptorImpl);
                    webModuleConfig.setLocation(new File(new StringBuilder(location)
                                                                .append(File.separator)
                                                                .append(moduleDir)
                                                                .toString()));
                    webModuleConfig.setParentLoader(EmbeddedWebContainer.class.getClassLoader());
                    webModuleConfig.setAppClassLoader(privileged(() -> 
                        new WebappClassLoader(EmbeddedWebContainer.class.getClassLoader(), app)));

                }
            } else {
                _logger.log(SEVERE, VS_DEFAULT_WEB_MODULE_DISABLED, new Object[] { id, getID() });
            }
        }

        return webModuleConfig;
    }

    /**
     * Virtual servers are maintained in the reference contained in Server element. First, we need to find the server and
     * then get the virtual server from the correct reference
     *
     * @param appName Name of the app to get vs
     *
     * @return virtual servers as a string (separated by space or comma)
     *
     * private String getVirtualServers(String appName) { String ret = null; Server server =
     * Globals.getDefaultHabitat().forContract(Server.class).get(); for (ApplicationRef appRef : server.getApplicationRef())
     * { if (appRef.getRef().equals(appName)) { return appRef.getVirtualServers(); } }
     * 
     * return ret; }
     */

    /**
     * Delete all aliases.
     */
    public void clearAliases() {
        aliases = new String[0];
    }

    private void setIsDisabled(boolean isDisabled) {
        vsPipeline.setIsDisabled(isDisabled);
        vsPipeline.setIsOff(false);
        if (isDisabled && pipeline != vsPipeline) {
            // Enable custom pipeline
            setPipeline(vsPipeline);
        }
    }

    private void setIsOff(boolean isOff) {
        vsPipeline.setIsOff(isOff);
        vsPipeline.setIsDisabled(false);
        if (isOff && pipeline != vsPipeline) {
            // Enable custom pipeline
            setPipeline(vsPipeline);
        }
    }

    private void close(FileLoggerHandler handler) {
        if (handler != null && !handler.isAssociated()) {
            if (fileLoggerHandlerFactory != null) {
                // should always be here
                fileLoggerHandlerFactory.removeHandler(handler.getLogFile());
            }
            handler.flush();
            handler.close();
        }
    }

    private void setLogger(Logger newLogger, String logLevel) {
        _logger = newLogger;
        // wrap into a cataline logger
        CatalinaLogger catalinaLogger = new CatalinaLogger(newLogger);
        catalinaLogger.setLevel(logLevel);
        setLogger(catalinaLogger);
    }

    /**
     * @return The properties of this virtual server
     */
    List<Property> getProperties() {
        return vsBean.getProperty();
    }

    /**
     * Configures this virtual server.
     * 
     * @param vsID
     * @param vsBean
     * @param vsDocroot
     * @param vsLogFile
     * @param logServiceFile
     * @param logLevel
     */
    public void configure(String vsID, com.sun.enterprise.config.serverbeans.VirtualServer vsBean, String vsDocroot, String vsLogFile, MimeMap vsMimeMap,
            String logServiceFile, String logLevel) {
        setDebug(debug);
        setAppBase(vsDocroot);
        setName(vsID);
        setID(vsID);
        setBean(vsBean);
        setMimeMap(vsMimeMap);

        String defaultContextXmlLocation = Constants.DEFAULT_CONTEXT_XML;
        String defaultWebXmlLocation = Constants.DEFAULT_WEB_XML;

        // Begin EE: 4920692 Make the default-web.xml be relocatable
        Property prop = vsBean.getProperty("default-web-xml");
        if (prop != null) {
            defaultWebXmlLocation = prop.getValue();
        }
        // End EE: 4920692 Make the default-web.xml be relocatable

        // allowLinking
        boolean allowLinking = false;
        prop = vsBean.getProperty("allowLinking");
        if (prop != null) {
            allowLinking = Boolean.parseBoolean(prop.getValue());
        }
        setAllowLinking(allowLinking);

        prop = vsBean.getProperty("contextXmlDefault");
        if (prop != null) {
            defaultContextXmlLocation = prop.getValue();
        }
        setDefaultWebXmlLocation(defaultWebXmlLocation);
        setDefaultContextXmlLocation(defaultContextXmlLocation);

        // Set vs state
        String state = vsBean.getState();
        if (state == null) {
            state = ON;
        }
        if (DISABLED.equalsIgnoreCase(state)) {
            setIsActive(false);
        } else {
            setIsActive(Boolean.parseBoolean(state));
        }

        setLogFile(vsLogFile, logLevel, logServiceFile);
    }

    /**
     * Configures the valve_ and listener_ properties of this VirtualServer.
     */
    protected void configureCatalinaProperties() {

        List<Property> props = vsBean.getProperty();
        if (props == null) {
            return;
        }

        for (Property prop : props) {

            String propName = prop.getName();
            String propValue = prop.getValue();
            if (propName == null || propValue == null) {
                _logger.log(Level.WARNING, LogFacade.NULL_VIRTUAL_SERVER_PROPERTY, getName());
            }

            if (propName != null) {
                if (propName.startsWith("valve_")) {
                    addValve(propValue);
                } else if (propName.startsWith("listener_")) {
                    addListener(propValue);
                } else if (propName.equals("securePagesWithPragma")) {
                    setSecurePagesWithPragma(Boolean.valueOf(propValue));
                }
            }
        }
    }

    /**
     * Configures this virtual server with the specified log file.
     *
     * @param logFile the value of the virtual server's log-file attribute in the domain.xml.
     * @param logLevel the verbosity of the logger.
     * @param logServiceFile the file used for the log service.
     */
    synchronized void setLogFile(String logFile, String logLevel, String logServiceFile) {

        /*
         * Configure separate logger for this virtual server only if 'log-file' attribute of this <virtual-server> and 'file'
         * attribute of <log-service> are different (See 6189219).
         */
        boolean customLog = (logFile != null && logServiceFile != null && !new File(logFile).equals(new File(logServiceFile)));

        boolean logFileChanged = logFile != null
                && ((fileLoggerHandler != null && !logFile.equals(fileLoggerHandler.getLogFile())) || fileLoggerHandler == null);

        /*
         * Exit early if the log file isn't being changed.
         */
        if (!logFileChanged) {
            return;
        }

        // As it is being changed, close the old file handler
        if (fileLoggerHandler != null) {
            _logger.removeHandler(fileLoggerHandler);
            close(fileLoggerHandler);
            fileLoggerHandler = null;
        }

        // Store new logger to replace current one
        Logger newLogger = null;

        /*
         * If the file is being changed to the log service file, reset the logger.
         */
        if (!customLog) {
            newLogger = _logger;
            for (Handler h : _logger.getHandlers()) {
                newLogger.removeHandler(h);
            }
            newLogger.setUseParentHandlers(true);
        } else {
            // append the _logger name with "._vs.<virtual-server-id>" if it doesn't already have it
            String lname = _logger.getName();
            if (!lname.endsWith("._vs." + getID())) {
                lname = _logger.getName() + "._vs." + getID();
            }
            newLogger = LogManager.getLogManager().getLogger(lname);
            if (newLogger == null) {
                newLogger = new Logger(lname, null) {
                    // set thread id, see LogDomains.getLogger method
                    @Override
                    public void log(LogRecord record) {
                        if (record.getResourceBundle() == null) {
                            ResourceBundle bundle = getResourceBundle();
                            if (bundle != null) {
                                record.setResourceBundle(bundle);
                            }
                        }
                        record.setThreadID((int) Thread.currentThread().getId());
                        super.log(record);
                    }

                    // use the same resource bundle as default vs logger
                    @Override
                    public ResourceBundle getResourceBundle() {
                        return rb;
                    }

                    @Override
                    public synchronized void addHandler(Handler handler) {
                        super.addHandler(handler);
                        if (handler instanceof FileLoggerHandler) {
                            ((FileLoggerHandler) handler).associate();
                        }
                    }

                    @Override
                    public synchronized void removeHandler(Handler handler) {
                        if (!(handler instanceof FileLoggerHandler)) {
                            super.removeHandler(handler);
                        } else {
                            boolean hasHandler = false;
                            Handler[] hs = getHandlers();
                            if (hs != null) {
                                for (Handler h : hs) {
                                    if (h == handler) {
                                        hasHandler = true;
                                        break;
                                    }
                                }
                            }
                            if (hasHandler) {
                                super.removeHandler(handler);
                                ((FileLoggerHandler) handler).disassociate();
                            }
                        }
                    }
                };

                synchronized (Logger.class) {
                    LogManager.getLogManager().addLogger(newLogger);
                }
            }

            // remove old handlers if necessary
            Handler[] handlers = newLogger.getHandlers();
            if (handlers != null) {
                for (Handler h : handlers) {
                    newLogger.removeHandler(h);
                }
            }

            // add handlers from root that is not GFFileHandler
            Logger rootLogger = Logger.global.getParent();
            if (rootLogger != null) {
                Handler[] rootHandlers = rootLogger.getHandlers();
                if (rootHandlers != null) {
                    for (Handler h : rootHandlers) {
                        if (!(h instanceof GFFileHandler)) {
                            newLogger.addHandler(h);
                        }
                    }
                }
            }

            // create and add new handler
            fileLoggerHandler = fileLoggerHandlerFactory.getHandler(logFile);
            newLogger.addHandler(fileLoggerHandler);
            newLogger.setUseParentHandlers(false);
        }

        setLogger(newLogger, logLevel);
    }

    /**
     * Adds each host name from the 'hosts' attribute as an alias
     */
    void configureAliases() {
        List hosts = StringUtils.parseStringList(vsBean.getHosts(), ",");
        for (int i = 0; i < hosts.size(); i++) {
            String alias = hosts.get(i).toString();
            if (!alias.equalsIgnoreCase("localhost") && !alias.equalsIgnoreCase("localhost.localdomain")) {
                addAlias(alias);
            }
        }
    }

    void configureAliases(String... hosts) {
        for (String host : hosts) {
            if (!host.equalsIgnoreCase("localhost") && !host.equalsIgnoreCase("localhost.localdomain")) {
                addAlias(host);
            }
        }
    }

    /**
     * Configures this virtual server with its authentication realm.
     *
     * Checks if this virtual server specifies any authRealm property, and if so, ensures that its value identifies a valid
     * realm.
     *
     * @param securityService The security-service element from domain.xml
     */
    void configureAuthRealm(SecurityService securityService) {
        List<Property> properties = vsBean.getProperty();
        if (properties != null && properties.size() > 0) {
            for (Property property : properties) {
                if (property != null && "authRealm".equals(property.getName())) {
                    authRealmName = property.getValue();
                    if (authRealmName != null) {
                        AuthRealm realm = null;
                        List<AuthRealm> authRealms = securityService.getAuthRealm();
                        if (authRealms != null && authRealms.size() > 0) {
                            for (AuthRealm authRealm : authRealms) {
                                if (authRealm != null && authRealm.getName().equals(authRealmName)) {
                                    realm = authRealm;
                                    break;
                                }
                            }
                        }

                        if (realm == null) {
                            _logger.log(SEVERE, INVALID_AUTH_REALM, new Object[] { getID(), authRealmName });
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Gets the value of the authRealm property of this virtual server.
     *
     * @return The value of the authRealm property of this virtual server, or null of this virtual server does not have any
     * such property
     */
    String getAuthRealmName() {
        return authRealmName;
    }

    /**
     * Adds the <code>Valve</code> with the given class name to this VirtualServer.
     *
     * @param valveName The valve's fully qualified class nam
     */
    protected void addValve(String valveName) {
        Object valve = safeLoadInstance(valveName);
        if (valve instanceof Valve) {
            addValve((Valve) valve);
        } else if (valve instanceof GlassFishValve) {
            addValve((GlassFishValve) valve);
        } else {
            _logger.log(WARNING, NOT_A_VALVE, valveName);
        }
    }

    /**
     * Adds the Catalina listener with the given class name to this VirtualServer.
     *
     * @param listenerName The fully qualified class name of the listener
     */
    protected void addListener(String listenerName) {
        Object listener = safeLoadInstance(listenerName);

        if (listener == null)
            return;

        if (listener instanceof ContainerListener) {
            addContainerListener((ContainerListener) listener);
        } else if (listener instanceof LifecycleListener) {
            addLifecycleListener((LifecycleListener) listener);
        } else {
            _logger.log(SEVERE, INVALID_LISTENER_VIRTUAL_SERVER, new Object[] { listenerName, getID() });
        }
    }

    @Override
    protected Object loadInstance(String className) throws Exception {
        // See IT 11674 for why CommonClassLoader must be used
        Class clazz = serverContext.getCommonClassLoader().loadClass(className);
        return clazz.newInstance();
    }

    private Object safeLoadInstance(String className) {
        try {
            return loadInstance(className);
        } catch (Throwable ex) {
            _logger.log(SEVERE, UNABLE_TO_LOAD_EXTENSION_SEVERE, ex);
        }
        return null;
    }

    /**
     * Configures this VirtualServer with its send-error properties.
     */
    void configureErrorPage() {
        ErrorPage errorPage = null;

        List<Property> props = vsBean.getProperty();
        if (props == null) {
            return;
        }

        for (Property prop : props) {
            String propName = prop.getName();
            String propValue = prop.getValue();
            if (propName == null || propValue == null) {
                _logger.log(Level.WARNING, LogFacade.NULL_VIRTUAL_SERVER_PROPERTY, getID());
                continue;
            }

            if (!propName.startsWith("send-error_")) {
                continue;
            }

            /*
             * Validate the prop value
             */
            String path = null;
            String reason = null;
            String status = null;

            String[] errorParams = propValue.split(" ");
            for (int j = 0; j < errorParams.length; j++) {

                if (errorParams[j].startsWith("path=")) {
                    if (path != null) {
                        _logger.log(Level.WARNING, LogFacade.SEND_ERROR_MULTIPLE_ELEMENT, new Object[] { propValue, getID(), "path" });
                    }
                    path = errorParams[j].substring("path=".length());
                }

                if (errorParams[j].startsWith("reason=")) {
                    if (reason != null) {
                        _logger.log(Level.WARNING, LogFacade.SEND_ERROR_MULTIPLE_ELEMENT, new Object[] { propValue, getID(), "reason" });
                    }
                    reason = errorParams[j].substring("reason=".length());
                }

                if (errorParams[j].startsWith("code=")) {
                    if (status != null) {
                        _logger.log(Level.WARNING, LogFacade.SEND_ERROR_MULTIPLE_ELEMENT, new Object[] { propValue, getID(), "code" });
                    }
                    status = errorParams[j].substring("code=".length());
                }
            }

            if (path == null || path.length() == 0) {
                _logger.log(Level.WARNING, LogFacade.SEND_ERROR_MISSING_PATH, new Object[] { propValue, getID() });
            }

            errorPage = new ErrorPage();
            errorPage.setLocation(path);
            errorPage.setErrorCode(status);
            errorPage.setReason(reason);

            addErrorPage(errorPage);
        }

    }

    /**
     * Configures this VirtualServer with its redirect properties.
     */
    void configureRedirect() {
        vsPipeline.clearRedirects();

        List<Property> props = vsBean.getProperty();
        if (props == null) {
            return;
        }

        for (Property prop : props) {

            String propName = prop.getName();
            String propValue = prop.getValue();
            if (propName == null || propValue == null) {
                _logger.log(Level.WARNING, LogFacade.NULL_VIRTUAL_SERVER_PROPERTY, getID());
                continue;
            }

            if (!propName.startsWith("redirect_")) {
                continue;
            }

            /*
             * Validate the prop value
             */
            String from = null;
            String url = null;
            String urlPrefix = null;
            String escape = null;

            String[] redirectParams = propValue.split(" ");
            for (int j = 0; j < redirectParams.length; j++) {

                if (redirectParams[j].startsWith("from=")) {
                    if (from != null) {
                        _logger.log(Level.WARNING, LogFacade.REDIRECT_MULTIPLE_ELEMENT, new Object[] { propValue, getID(), "from" });
                    }
                    from = redirectParams[j].substring("from=".length());
                }

                if (redirectParams[j].startsWith("url=")) {
                    if (url != null) {
                        _logger.log(Level.WARNING, LogFacade.REDIRECT_MULTIPLE_ELEMENT, new Object[] { propValue, getID(), "url" });
                    }
                    url = redirectParams[j].substring("url=".length());
                }

                if (redirectParams[j].startsWith("url-prefix=")) {
                    if (urlPrefix != null) {
                        _logger.log(Level.WARNING, LogFacade.REDIRECT_MULTIPLE_ELEMENT, new Object[] { propValue, getID(), "url-prefix" });
                    }
                    urlPrefix = redirectParams[j].substring("url-prefix=".length());
                }

                if (redirectParams[j].startsWith("escape=")) {
                    if (escape != null) {
                        _logger.log(Level.WARNING, LogFacade.REDIRECT_MULTIPLE_ELEMENT, new Object[] { propValue, getID(), "escape" });
                    }
                    escape = redirectParams[j].substring("escape=".length());
                }
            }

            if (from == null || from.length() == 0) {
                _logger.log(Level.WARNING, LogFacade.REDIRECT_MULTIPLE_ELEMENT, new Object[] { propValue, getID() });
            }

            // Either url or url-prefix (but not both!) must be present
            if ((url == null || url.length() == 0) && (urlPrefix == null || urlPrefix.length() == 0)) {
                _logger.log(Level.WARNING, LogFacade.REDIRECT_MISSING_URL_OR_URL_PREFIX, new Object[] { propValue, getID() });
            }
            if (url != null && url.length() > 0 && urlPrefix != null && urlPrefix.length() > 0) {
                _logger.log(Level.WARNING, LogFacade.REDIRECT_BOTH_URL_AND_URL_PREFIX, new Object[] { propValue, getID() });
            }

            boolean escapeURI = true;
            if (escape != null) {
                if ("yes".equalsIgnoreCase(escape)) {
                    escapeURI = true;
                } else if ("no".equalsIgnoreCase(escape)) {
                    escapeURI = false;
                } else {
                    _logger.log(Level.WARNING, LogFacade.REDIRECT_INVALID_ESCAPE, new Object[] { propValue, getID() });
                }
            }

            vsPipeline.addRedirect(from, url, urlPrefix, escapeURI);
        }

        if (vsPipeline.hasRedirects()) {
            if (pipeline != vsPipeline) {
                // Enable custom pipeline
                setPipeline(vsPipeline);
            }
        } else if (isActive && pipeline != origPipeline) {
            setPipeline(origPipeline);
        }
    }

    /**
     * Configures the SSO valve of this VirtualServer.
     */
    void configureSingleSignOn(boolean globalSSOEnabled, WebContainerFeatureFactory webContainerFeatureFactory, boolean ssoFailoverEnabled) {
        if (!isSSOEnabled(globalSSOEnabled)) {
            /*
             * Disable SSO
             */
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, LogFacade.DISABLE_SSO, getID());
            }

            boolean hasExistingSSO = false;
            // Remove existing SSO valve (if any)
            GlassFishValve[] valves = getValves();
            for (int i = 0; valves != null && i < valves.length; i++) {
                if (valves[i] instanceof SingleSignOn) {
                    removeValve(valves[i]);
                    hasExistingSSO = true;
                    break;
                }
            }

            this.ssoFailoverEnabled = ssoFailoverEnabled;
            if (hasExistingSSO) {
                setSingleSignOnForChildren(null);
            }
        } else {
            /*
             * Enable SSO
             */
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, LogFacade.ENABLE_SSO, getID());
            }

            GlassFishSingleSignOn sso = null;

            // find existing SSO (if any), in case of a reconfig
            GlassFishValve[] valves = getValves();
            for (int i = 0; valves != null && i < valves.length; i++) {
                if (valves[i] instanceof GlassFishSingleSignOn) {
                    sso = (GlassFishSingleSignOn) valves[i];
                    break;
                }
            }

            if (sso != null && this.ssoFailoverEnabled != ssoFailoverEnabled) {
                removeValve(sso);
                sso = null;
                // then SSO Valve will be recreated
            }

            if (sso == null) {
                SSOFactory ssoFactory = webContainerFeatureFactory.getSSOFactory();
                sso = ssoFactory.createSingleSignOnValve(getName());
                this.ssoFailoverEnabled = ssoFailoverEnabled;
                setSingleSignOnForChildren(sso);
                addValve((GlassFishValve) sso);
            }

            // set max idle time if given
            Property idle = vsBean.getProperty(SSO_MAX_IDLE);
            if (idle != null && idle.getValue() != null) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, LogFacade.SSO_MAX_INACTIVE_SET, new Object[] { idle.getValue(), getID() });
                }
                sso.setMaxInactive(Integer.parseInt(idle.getValue()));
            }

            // set expirer thread sleep time if given
            Property expireTime = vsBean.getProperty(SSO_REAP_INTERVAL);
            if (expireTime != null && expireTime.getValue() != null) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, LogFacade.SSO_REAP_INTERVAL_SET);
                }
                sso.setReapInterval(Integer.parseInt(expireTime.getValue()));
            }

            configureSingleSignOnCookieSecure();
            configureSingleSignOnCookieHttpOnly();
        }
    }

    /**
     * Configures this VirtualServer with its state (on | off | disabled).
     */
    void configureState() {
        String stateValue = vsBean.getState();
        if (!stateValue.equalsIgnoreCase(ON) && getName().equalsIgnoreCase(ADMIN_VS)) {
            throw new IllegalArgumentException("virtual-server " + ADMIN_VS + " state property cannot be modified");
        }

        if (stateValue.equalsIgnoreCase(DISABLED)) {
            // state="disabled"
            setIsDisabled(true);
        } else if (!ConfigBeansUtilities.toBoolean(stateValue)) {
            // state="off"
            setIsOff(true);
        } else {
            setIsActive(true);
        }
    }

    /**
     * Configures the Remote Address Filter valve of this VirtualServer.
     *
     * This valve enforces request accpetance/denial based on the string representation of the remote client's IP address.
     */
    void configureRemoteAddressFilterValve() {
        Property allow = vsBean.getProperty("allowRemoteAddress");
        Property deny = vsBean.getProperty("denyRemoteAddress");
        String allowStr = null;
        String denyStr = null;
        if (allow != null) {
            allowStr = allow.getValue();
        }
        if (deny != null) {
            denyStr = deny.getValue();
        }
        configureRemoteAddressFilterValve(allowStr, denyStr);
    }

    /**
     * Configures the Remote Address Filter valve of this VirtualServer.
     *
     * This valve enforces request accpetance/denial based on the string representation of the remote client's IP address.
     */
    protected void configureRemoteAddressFilterValve(String allow, String deny) {
        RemoteAddrValve remoteAddrValve = null;

        if (allow != null || deny != null) {
            remoteAddrValve = new RemoteAddrValve();
        }

        if (allow != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, LogFacade.ALLOW_ACCESS, new Object[] { getID(), allow });
            }
            remoteAddrValve.setAllow(allow);
        }

        if (deny != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, LogFacade.DENY_ACCESS, new Object[] { getID(), deny });
            }
            remoteAddrValve.setDeny(deny);
        }

        if (remoteAddrValve != null) {
            // Remove existing RemoteAddrValve (if any), in case of a reconfig
            GlassFishValve[] valves = getValves();
            for (int i = 0; valves != null && i < valves.length; i++) {
                if (valves[i] instanceof RemoteAddrValve) {
                    removeValve(valves[i]);
                    break;
                }
            }
            addValve((GlassFishValve) remoteAddrValve);
        }
    }

    /**
     * Configures the Remote Host Filter valve of this VirtualServer.
     *
     * This valve enforces request acceptance/denial based on the name of the remote host from where the request originated.
     */
    void configureRemoteHostFilterValve() {
        Property allow = vsBean.getProperty("allowRemoteHost");
        Property deny = vsBean.getProperty("denyRemoteHost");
        String allowStr = null;
        String denyStr = null;
        if (allow != null) {
            allowStr = allow.getValue();
        }
        if (deny != null) {
            denyStr = deny.getValue();
        }
        configureRemoteHostFilterValve(allowStr, denyStr);

    }

    void configureRemoteHostFilterValve(String allow, String deny) {
        RemoteHostValve remoteHostValve = null;

        if (allow != null || deny != null) {
            remoteHostValve = new RemoteHostValve();
        }
        if (allow != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, LogFacade.ALLOW_ACCESS, new Object[] { getID(), allow });
            }
            remoteHostValve.setAllow(allow);
        }
        if (deny != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, LogFacade.DENY_ACCESS, new Object[] { getID(), deny });
            }
            remoteHostValve.setDeny(deny);
        }
        if (remoteHostValve != null) {
            // Remove existing RemoteHostValve (if any), in case of a reconfig
            GlassFishValve[] valves = getValves();
            for (int i = 0; valves != null && i < valves.length; i++) {
                if (valves[i] instanceof RemoteHostValve) {
                    removeValve(valves[i]);
                    break;
                }
            }
            addValve((GlassFishValve) remoteHostValve);
        }
    }

    /**
     * Sets all the monitoring probes used in the virtual server
     * 
     * @param globalAccessLoggingEnabled
     * @see org.glassfish.grizzly.http.HttpProbe
     */
    void addProbes(boolean globalAccessLoggingEnabled) {
        for (final NetworkListener listener : getGrizzlyNetworkListeners()) {
            try {
                final GrizzlyProxy proxy = (GrizzlyProxy) grizzlyService.lookupNetworkProxy(listener);
                if (proxy != null) {
                    GenericGrizzlyListener grizzlyListener = (GenericGrizzlyListener) proxy.getUnderlyingListener();
                    List<HttpCodecFilter> codecFilters = grizzlyListener.getFilters(HttpCodecFilter.class);
                    if (codecFilters == null || codecFilters.isEmpty()) {
                        // if it's AJP listener - it's ok if we didn't find HttpCodecFilter
                        if (grizzlyListener.isAjpEnabled()) {
                            continue;
                        }
                        _logger.log(Level.SEVERE, LogFacade.CODE_FILTERS_NULL, new Object[] { listener.getName(), codecFilters });
                    } else {
                        for (HttpCodecFilter codecFilter : codecFilters) {
                            if (codecFilter.getMonitoringConfig().getProbes().length == 0) {
                                HttpProbeImpl httpProbe = new HttpProbeImpl(listener, isAccessLoggingEnabled(globalAccessLoggingEnabled));
                                codecFilter.getMonitoringConfig().addProbes(httpProbe);
                            }
                        }
                    }
                    grizzlyListener.getTransport().getConnectionMonitoringConfig().addProbes(new ConnectionProbe.Adapter() {

                        RequestProbeProvider requestProbeProvider = webContainer.getRequestProbeProvider();

                        @Override
                        public void onReadEvent(Connection connection, Buffer data, int size) {
                            if (requestProbeProvider != null) {
                                requestProbeProvider.dataReceivedEvent(size, _id);
                            }
                        }

                        @Override
                        public void onWriteEvent(Connection connection, Buffer data, long size) {
                            if (requestProbeProvider != null) {
                                requestProbeProvider.dataSentEvent(size, _id);
                            }
                        }
                    });

                } else {
                    // check the listener is enabled before spitting out the SEVERE log
                    if (Boolean.parseBoolean(listener.getEnabled())) {
                        _logger.log(Level.SEVERE, LogFacade.PROXY_NULL, new Object[] { listener.getName() });
                    }
                }

            } catch (Exception ex) {
                _logger.log(Level.SEVERE, LogFacade.ADD_HTTP_PROBES_ERROR, ex);
            }
        }
    }

    /**
     * Reconfigures the access log of this VirtualServer with its updated access log related properties.
     */
    void reconfigureAccessLog(String globalAccessLogBufferSize, String globalAccessLogWriteInterval, ServiceLocator services, Domain domain,
            boolean globalAccessLoggingEnabled) {
        try {
            if (accessLogValve.isStarted()) {
                accessLogValve.stop();
            }
            boolean start = accessLogValve.updateVirtualServerProperties(vsBean.getId(), vsBean, domain, services, globalAccessLogBufferSize,
                    globalAccessLogWriteInterval);
            if (start && isAccessLoggingEnabled(globalAccessLoggingEnabled)) {
                enableAccessLogging();
            } else {
                disableAccessLogging();
            }
        } catch (LifecycleException le) {
            _logger.log(Level.SEVERE, LogFacade.UNABLE_RECONFIGURE_ACCESS_LOG, le);
        }
    }

    /**
     * Reconfigures the access log of this VirtualServer with the updated attributes of the access-log element from
     * domain.xml.
     */
    void reconfigureAccessLog(HttpService httpService, WebContainerFeatureFactory webcontainerFeatureFactory) {

        try {
            boolean restart = false;
            if (accessLogValve.isStarted()) {
                accessLogValve.stop();
                restart = true;
            }
            accessLogValve.updateAccessLogAttributes(httpService, webcontainerFeatureFactory);
            if (restart) {
                accessLogValve.start();
                for (HttpProbeImpl p : getHttpProbeImpl()) {
                    p.enableAccessLogging();
                }
            }
        } catch (LifecycleException le) {
            _logger.log(Level.SEVERE, LogFacade.UNABLE_RECONFIGURE_ACCESS_LOG, le);
        }
    }

    /**
     * @return the accesslog valve of this virtual server
     */
    PEAccessLogValve getAccessLogValve() {
        return accessLogValve;
    }

    /**
     * Enables access logging for this virtual server, by adding its accesslog valve to its pipeline, or starting its
     * accesslog valve if it is already present in the pipeline.
     */
    void enableAccessLogging() {
        if (!isAccessLogValveActivated()) {
            addValve((GlassFishValve) accessLogValve);
        } else {
            try {
                if (accessLogValve.isStarted()) {
                    accessLogValve.stop();
                }
                accessLogValve.start();
                for (HttpProbeImpl p : getHttpProbeImpl()) {
                    p.enableAccessLogging();
                }
            } catch (LifecycleException le) {
                _logger.log(Level.SEVERE, LogFacade.UNABLE_RECONFIGURE_ACCESS_LOG, le);
            }
        }
    }

    /**
     * Disables access logging for this virtual server, by removing its accesslog valve from its pipeline.
     */
    void disableAccessLogging() {
        removeValve(accessLogValve);
        for (HttpProbeImpl httpProbe : getHttpProbeImpl()) {
            httpProbe.disableAccessLogging();
        }
    }

    /**
     * @return true if the accesslog valve of this virtual server has been activated, that is, added to this virtual
     * server's pipeline; false otherwise
     */
    private boolean isAccessLogValveActivated() {
        Pipeline p = getPipeline();
        if (p != null) {
            GlassFishValve[] valves = p.getValves();
            if (valves != null) {
                for (int i = 0; i < valves.length; i++) {
                    if (valves[i] instanceof PEAccessLogValve) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Configures the cache control of this VirtualServer
     */
    void configureCacheControl(String cacheControl) {
        if (cacheControl != null) {
            List<String> values = StringUtils.parseStringList(cacheControl, ",");
            if (values != null && !values.isEmpty()) {
                setCacheControls(values.toArray(new String[values.size()]));
            }
        }
    }

    /**
     * Checks if SSO is enabled for this VirtualServer.
     *
     * @return The value of the sso-enabled property for this VirtualServer
     */
    private boolean isSSOEnabled(boolean globalSSOEnabled) {
        String ssoEnabled = "inherit";
        if (vsBean != null) {
            ssoEnabled = vsBean.getSsoEnabled();
        }
        return "inherit".equals(ssoEnabled) && globalSSOEnabled || ConfigBeansUtilities.toBoolean(ssoEnabled);
    }

    private void setSingleSignOnForChildren(SingleSignOn sso) {
        for (Container container : findChildren()) {
            if (container instanceof StandardContext) {
                StandardContext context = (StandardContext) container;
                for (GlassFishValve valve : context.getValves()) {
                    if (valve instanceof AuthenticatorBase) {
                        ((AuthenticatorBase) valve).setSingleSignOn(sso);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Determines whether access logging is enabled for this virtual server.
     *
     * @param globalAccessLoggingEnabled The value of the accessLoggingEnabled property of the http-service element
     *
     * @return true if access logging is enabled for this virtual server, false otherwise.
     */
    boolean isAccessLoggingEnabled(boolean globalAccessLoggingEnabled) {
        String enabled = vsBean.getAccessLoggingEnabled();
        return "inherit".equals(enabled) && globalAccessLoggingEnabled || ConfigBeansUtilities.toBoolean(enabled);
    }

    @Override
    public void setRealm(Realm realm) {
        if (!(realm instanceof RealmAdapter)) {
            _logger.log(SEVERE, IGNORE_INVALID_REALM, new Object[] { realm.getClass().getName(), RealmAdapter.class.getName() });
        } else {
            super.setRealm(realm);
        }
    }

    /**
     * Configures the security level of the SSO cookie for this virtual server, based on the value of its sso-cookie-secure
     * attribute
     */
    private void configureSingleSignOnCookieSecure() {
        String cookieSecure = vsBean.getSsoCookieSecure();
        if (!"true".equalsIgnoreCase(cookieSecure) && !"false".equalsIgnoreCase(cookieSecure)
                && !cookieSecure.equalsIgnoreCase(SessionCookieConfig.DYNAMIC_SECURE)) {
            _logger.log(Level.WARNING, LogFacade.INVALID_SSO_COOKIE_SECURE, new Object[] { cookieSecure, getID() });
        } else {
            ssoCookieSecure = cookieSecure;
        }
    }

    private void configureSingleSignOnCookieHttpOnly() {
        ssoCookieHttpOnly = Boolean.parseBoolean(vsBean.getSsoCookieHttpOnly());
    }

    /**
     * Configures the error report valve of this VirtualServer.
     *
     * <p>
     * The error report valve of a virtual server is specified through a property with name <i>errorReportValve</i>, whose
     * value is the valve's fully qualified classname. A null or empty classname disables the error report valve and
     * therefore the container's default error page mechanism for error responses.
     */
    void configureErrorReportValve() {
        Property prop = vsBean.getProperty(Constants.ERROR_REPORT_VALVE);
        if (prop != null) {
            setErrorReportValveClass(prop.getValue());
        }
    }

    void setServerContext(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    void setServerConfig(Config serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * Sets the grizzly service to be used
     * 
     * @param grizzlyService
     */
    void setGrizzlyService(GrizzlyService grizzlyService) {
        this.grizzlyService = grizzlyService;
    }

    /**
     * Sets the Web Container for the virtual server
     * 
     * @param webContainer
     */
    void setWebContainer(WebContainer webContainer) {
        this.webContainer = webContainer;
    }

    // ----------------------------------------------------- embedded methods

    private VirtualServerConfig config;

    private List<WebListener> listeners = new ArrayList<WebListener>();

    /**
     * Sets the docroot of this <tt>VirtualServer</tt>.
     *
     * @param docRoot the docroot of this <tt>VirtualServer</tt>.
     */
    @Override
    public void setDocRoot(File docRoot) {
        this.setAppBase(docRoot.getPath());
    }

    /**
     * Gets the docroot of this <tt>VirtualServer</tt>.
     */
    @Override
    public File getDocRoot() {
        return new File(getAppBase());
    }

    /**
     * Sets the collection of <tt>WebListener</tt> instances from which this <tt>VirtualServer</tt> receives requests.
     *
     * @param webListeners the collection of <tt>WebListener</tt> instances from which this <tt>VirtualServer</tt> receives
     * requests.
     */
    public void setWebListeners(WebListener... webListeners) {
        if (webListeners != null) {
            listeners = Arrays.asList(webListeners);
        }
    }

    /**
     * Gets the collection of <tt>WebListener</tt> instances from which this <tt>VirtualServer</tt> receives requests.
     *
     * @return the collection of <tt>WebListener</tt> instances from which this <tt>VirtualServer</tt> receives requests.
     */
    @Override
    public Collection<WebListener> getWebListeners() {
        return listeners;
    }

    /**
     * Registers the given <tt>Context</tt> with this <tt>VirtualServer</tt> at the given context root.
     *
     * <p>
     * If this <tt>VirtualServer</tt> has already been started, the given <tt>context</tt> will be started as well.
     * 
     * @throws org.glassfish.embeddable.GlassFishException
     */
    @Override
    public void addContext(Context context, String contextRoot) throws ConfigException, GlassFishException {
        _logger.fine(VS_ADDED_CONTEXT);

        if (!(context instanceof ContextFacade)) {
            // embedded context should always be created via ContextFacade
            return;
        }

        if (!contextRoot.startsWith("/")) {
            contextRoot = "/" + contextRoot;
        }
        
        ExtendedDeploymentContext deploymentContext = null;

        try {
            if (factory == null)
                factory = services.getService(ArchiveFactory.class);

            ContextFacade facade = (ContextFacade) context;
            File docRoot = facade.getDocRoot();
            ClassLoader classLoader = facade.getClassLoader();
            ReadableArchive archive = factory.openArchive(docRoot);

            if (report == null)
                report = new PlainTextActionReporter();

            ServerEnvironment env = services.getService(ServerEnvironment.class);

            DeployCommandParameters params = new DeployCommandParameters();
            params.contextroot = contextRoot;
            params.enabled = Boolean.FALSE;
            params.origin = OpsParams.Origin.deploy;
            params.virtualservers = getName();
            params.target = "server";

            ExtendedDeploymentContext initialContext = new DeploymentContextImpl(report, archive, params, env);

            if (deployment == null)
                deployment = services.getService(Deployment.class);

            ArchiveHandler archiveHandler = deployment.getArchiveHandler(archive);
            if (archiveHandler == null) {
                throw new RuntimeException("Cannot find archive handler for source archive");
            }

            params.name = archiveHandler.getDefaultApplicationName(archive, initialContext);

            Applications apps = domain.getApplications();
            ApplicationInfo appInfo = deployment.get(params.name);
            ApplicationRef appRef = domain.getApplicationRefInServer(params.target, params.name);

            if (appInfo != null && appRef != null) {
                if (appRef.getVirtualServers().contains(getName())) {
                    throw new ConfigException("Context with name " + params.name + " is already registered on virtual server " + getName());
                } else {
                    String virtualServers = appRef.getVirtualServers();
                    virtualServers = virtualServers + "," + getName();
                    params.virtualservers = virtualServers;
                    params.force = Boolean.TRUE;
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "Virtual server " + getName() + " added to context " + params.name);
                    }
                    return;
                }
            }

            deploymentContext = deployment.getBuilder(_logger, params, report).source(archive).archiveHandler(archiveHandler).build(initialContext);

            Properties properties = new Properties();
            deploymentContext.getAppProps().putAll(properties);

            if (classLoader != null) {
                ClassLoader parentCL = classLoaderHierarchy.createApplicationParentCL(classLoader, deploymentContext);
                ClassLoader cl = archiveHandler.getClassLoader(parentCL, deploymentContext);
                deploymentContext.setClassLoader(cl);
            }

            ApplicationConfigInfo savedAppConfig = new ApplicationConfigInfo(
                    apps.getModule(com.sun.enterprise.config.serverbeans.Application.class, params.name));

            Properties appProps = deploymentContext.getAppProps();
            String appLocation = DeploymentUtils.relativizeWithinDomainIfPossible(deploymentContext.getSource().getURI());

            appProps.setProperty(ServerTags.LOCATION, appLocation);
            appProps.setProperty(ServerTags.OBJECT_TYPE, "user");
            appProps.setProperty(ServerTags.CONTEXT_ROOT, contextRoot);

            savedAppConfig.store(appProps);

            Transaction t = deployment.prepareAppConfigChanges(deploymentContext);
            appInfo = deployment.deploy(deploymentContext);

            if (appInfo != null) {
                facade.setAppName(appInfo.getName());
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, LogFacade.VS_ADDED_CONTEXT, new Object[] { getName(), appInfo.getName() });
                }
                deployment.registerAppInDomainXML(appInfo, deploymentContext, t);
            } else {
                if (report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)) {
                    throw new ConfigException(report.getMessage());
                }
            }

            // Update web.xml with programmatically added servlets, filters, and listeners
            File file = null;
            boolean delete = true;
            com.sun.enterprise.config.serverbeans.Application appBean = apps.getApplication(params.name);
            if (appBean != null) {
                file = new File(deploymentContext.getSource().getURI().getPath(), "/WEB-INF/web.xml");
                if (file.exists()) {
                    delete = false;
                }
                updateWebXml(facade, file);
            } else {
                _logger.log(Level.SEVERE, LogFacade.APP_NOT_FOUND);
            }

            ReadableArchive source = appInfo.getSource();
            UndeployCommandParameters undeployParams = new UndeployCommandParameters(params.name);
            undeployParams.origin = UndeployCommandParameters.Origin.undeploy;
            undeployParams.target = "server";
            ExtendedDeploymentContext undeploymentContext = deployment.getBuilder(_logger, undeployParams, report).source(source).build();
            deployment.undeploy(params.name, undeploymentContext);

            params.origin = DeployCommandParameters.Origin.load;
            params.enabled = Boolean.TRUE;
            archive = factory.openArchive(docRoot);
            deploymentContext = deployment.getBuilder(_logger, params, report).source(archive).build();

            if (classLoader != null) {
                ClassLoader parentCL = classLoaderHierarchy.createApplicationParentCL(classLoader, deploymentContext);
                archiveHandler = deployment.getArchiveHandler(archive);
                ClassLoader cl = archiveHandler.getClassLoader(parentCL, deploymentContext);
                deploymentContext.setClassLoader(cl);
            }

            deployment.deploy(deploymentContext);

            // Enable the app using the modified web.xml
            // We can't use Deployment.enable since it doesn't take DeploymentContext with custom class loader
            deployment.updateAppEnabledAttributeInDomainXML(params.name, params.target, true);

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, LogFacade.VS_ENABLED_CONTEXT, new Object[] { getName(), params.name() });
            }

            if (delete) {
                if (file != null) {
                    if (file.exists() && !file.delete()) {
                        String path = file.toString();
                        _logger.log(Level.WARNING, LogFacade.UNABLE_TO_DELETE, path);
                    }
                }
            }

            if (contextRoot.equals("/")) {
                contextRoot = "";
            }
            WebModule wm = (WebModule) findChild(contextRoot);
            if (wm != null) {
                facade.setUnwrappedContext(wm);
                wm.setEmbedded(true);
                if (config != null) {
                    wm.setDefaultWebXml(config.getDefaultWebXml());
                }
            } else {
                throw new ConfigException("Deployed app not found " + contextRoot);
            }

            if (deploymentContext != null) {
                deploymentContext.postDeployClean(true);
            }

        } catch (Exception ex) {
            if (deployment != null && deploymentContext != null) {
                deploymentContext.clean();
            }
            throw new GlassFishException(ex);
        }

    }

    /**
     * Stops the given <tt>context</tt> and removes it from this <tt>VirtualServer</tt>.
     * 
     * @throws org.glassfish.embeddable.GlassFishException
     */
    @Override
    public void removeContext(Context context) throws GlassFishException {
        ActionReport report = services.getService(ActionReport.class, "plain");
        Deployment deployment = services.getService(Deployment.class);
        String name;
        if (context instanceof ContextFacade) {
            name = ((ContextFacade) context).getAppName();
        } else {
            name = context.getPath();
        }
        ApplicationInfo appInfo = deployment.get(name);

        if (appInfo == null) {
            report.setMessage("Cannot find deployed application of name " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            throw new GlassFishException("Cannot find deployed application of name " + name);
        }

        ReadableArchive source = appInfo.getSource();

        if (source == null) {
            report.setMessage("Cannot get source archive for undeployment");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            throw new GlassFishException("Cannot get source archive for undeployment");
        }

        UndeployCommandParameters params = new UndeployCommandParameters(name);
        params.origin = UndeployCommandParameters.Origin.undeploy;
        params.target = "server";
        ExtendedDeploymentContext deploymentContext = null;

        try {
            deploymentContext = deployment.getBuilder(_logger, params, report).source(source).build();
            deployment.undeploy(name, deploymentContext);
            deployment.unregisterAppFromDomainXML(name, "server");
        } catch (IOException e) {
            _logger.log(Level.SEVERE, LogFacade.REMOVE_CONTEXT_ERROR, e);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            throw new GlassFishException("Cannot create context for undeployment ", e);
        } catch (TransactionFailure e) {
            throw new GlassFishException(e);
        } finally {
            if (deploymentContext != null) {
                deploymentContext.clean();
            }
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, LogFacade.REMOVED_CONTEXT, name);
        }
    }

    /**
     * Finds the <tt>Context</tt> registered at the given context root.
     */
    @Override
    public Context getContext(String contextRoot) {
        if (!contextRoot.startsWith("/")) {
            contextRoot = "/" + contextRoot;
        }
        return (Context) findChild(contextRoot);
    }

    /**
     * Gets the collection of <tt>Context</tt> instances registered with this <tt>VirtualServer</tt>.
     */
    @Override
    public Collection<Context> getContexts() {
        Collection<Context> ctxs = new ArrayList<Context>();
        for (Container container : findChildren()) {
            if (container instanceof Context) {
                ctxs.add((Context) container);
            }
        }
        return ctxs;
    }

    /**
     * Reconfigures this <tt>VirtualServer</tt> with the given configuration.
     *
     * <p>
     * In order for the given configuration to take effect, this <tt>VirtualServer</tt> may be stopped and restarted.
     */
    @Override
    public void setConfig(VirtualServerConfig config) throws ConfigException {

        this.config = config;
        configureSingleSignOn(config.isSsoEnabled(),
                Globals.getDefaultHabitat().<PEWebContainerFeatureFactoryImpl>getService(PEWebContainerFeatureFactoryImpl.class), false);
        if (config.isAccessLoggingEnabled()) {
            enableAccessLogging();
        } else {
            disableAccessLogging();
        }
        setDefaultWebXmlLocation(config.getDefaultWebXml());
        setDefaultContextXmlLocation(config.getContextXmlDefault());
        setAllowLinking(config.isAllowLinking());
        configureRemoteAddressFilterValve(config.getAllowRemoteAddress(), config.getDenyRemoteAddress());
        configureRemoteHostFilterValve(config.getAllowRemoteHost(), config.getAllowRemoteHost());
        configureAliases(config.getHostNames());

    }

    /**
     * Gets the current configuration of this <tt>VirtualServer</tt>.
     */
    @Override
    public VirtualServerConfig getConfig() {
        return config;
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (fileLoggerHandler != null) {
            _logger.removeHandler(fileLoggerHandler);
            close(fileLoggerHandler);
            fileLoggerHandler = null;
        }
        setLogger(_logger, "INFO");

        super.stop();
    }

    public void updateWebXml(ContextFacade facade, File file) throws Exception {

        Map<String, String> servlets = facade.getAddedServlets();
        Map<String, String[]> mappings = facade.getServletMappings();
        List<String> flisteners = facade.getListeners();
        Map<String, String> filters = facade.getAddedFilters();
        Map<String, String> servletNameFilterMappings = facade.getServletNameFilterMappings();
        Map<String, String> urlPatternFilterMappings = facade.getUrlPatternFilterMappings();

        if (!filters.isEmpty() || !flisteners.isEmpty() || !servlets.isEmpty()) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, LogFacade.MODIFYING_WEB_XML, file.getAbsolutePath());
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(true);
            dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
            Element webapp;

            if ((file != null) && (file.exists())) {
                doc = dBuilder.parse(file);
                webapp = doc.getDocumentElement();
            } else {
                doc = dBuilder.newDocument();
                webapp = doc.createElement("web-app");
                webapp.setAttribute("xmln", "http://java.sun.com/xml/ns/j2ee");
                webapp.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                webapp.setAttribute("xsi:schemaLocation", "http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd");
                webapp.setAttribute("version", "2.4");
                doc.appendChild(webapp);
            }

            boolean entryFound = false;

            // Update <filter>
            for (Map.Entry entry : filters.entrySet()) {
                NodeList filterList = doc.getElementsByTagName("filter-name");
                for (int i = 0; i < filterList.getLength(); i++) {
                    Node filterNode = filterList.item(i);
                    if (entry.getKey().equals(filterNode.getTextContent()) && filterNode.getParentNode().getNodeName().equals("filter")) {
                        NodeList children = filterNode.getParentNode().getChildNodes();
                        for (int j = 0; j < children.getLength(); j++) {
                            Node filterClass = children.item(j);
                            if (filterClass.getNodeName().equals("filter-class")) {
                                // If a filter with the given filter-name is already defined,
                                // the given class name will be assigned according to the spec
                                filterClass.setTextContent(entry.getValue().toString());
                                entryFound = true;
                                break;
                            }
                        }
                    }
                }
                if (!entryFound) {
                    Element filter = doc.createElement("filter");
                    Element filterName = doc.createElement("filter-name");
                    filterName.setTextContent(entry.getKey().toString());
                    filter.appendChild(filterName);
                    Element filterClass = doc.createElement("filter-class");
                    filterClass.setTextContent(entry.getValue().toString());
                    filter.appendChild(filterClass);
                    Map<String, String> initParams = facade.getFilterRegistration(entry.getKey().toString()).getInitParameters();
                    if ((initParams != null) && (!initParams.isEmpty())) {
                        Element initParam = doc.createElement("init-param");
                        for (Map.Entry param : initParams.entrySet()) {
                            Element paramName = doc.createElement("param-name");
                            paramName.setTextContent(param.getKey().toString());
                            initParam.appendChild(paramName);
                            Element paramValue = doc.createElement("param-value");
                            paramValue.setTextContent(param.getValue().toString());
                            initParam.appendChild(paramValue);
                        }
                        filter.appendChild(initParam);
                    }
                    webapp.appendChild(filter);
                }
            }

            // Update <filter-mapping>
            for (Map.Entry mapping : servletNameFilterMappings.entrySet()) {
                Element filterMapping = doc.createElement("filter-mapping");
                Element filterName = doc.createElement("filter-name");
                filterName.setTextContent(mapping.getKey().toString());
                filterMapping.appendChild(filterName);
                Element servletName = doc.createElement("servlet-name");
                servletName.setTextContent(mapping.getValue().toString());
                filterMapping.appendChild(servletName);
                webapp.appendChild(filterMapping);
            }

            for (Map.Entry mapping : urlPatternFilterMappings.entrySet()) {
                Element filterMapping = doc.createElement("filter-mapping");
                Element filterName = doc.createElement("filter-name");
                filterName.setTextContent(mapping.getKey().toString());
                filterMapping.appendChild(filterName);
                Element urlPattern = doc.createElement("url-pattern");
                urlPattern.setTextContent(mapping.getValue().toString());
                filterMapping.appendChild(urlPattern);
                webapp.appendChild(filterMapping);
            }

            entryFound = false;

            // Update <servlet>
            for (Map.Entry entry : servlets.entrySet()) {
                NodeList servletList = doc.getElementsByTagName("servlet-name");
                for (int i = 0; i < servletList.getLength(); i++) {
                    Node servletNode = servletList.item(i);
                    if (entry.getKey().equals(servletNode.getTextContent()) && servletNode.getParentNode().getNodeName().equals("servlet")) {
                        NodeList children = servletNode.getParentNode().getChildNodes();
                        for (int j = 0; j < children.getLength(); j++) {
                            Node servletClass = children.item(j);
                            if (servletClass.getNodeName().equals("servlet-class")) {
                                // If a servlet with the given servlet-name is already defined,
                                // the given className will be assigned according to the spec
                                servletClass.setTextContent(entry.getValue().toString());
                                entryFound = true;
                                break;
                            }
                        }
                    }
                }
                if (!entryFound) {
                    Element servlet = doc.createElement("servlet");
                    Element servletName = doc.createElement("servlet-name");
                    servletName.setTextContent(entry.getKey().toString());
                    servlet.appendChild(servletName);
                    Element servletClass = doc.createElement("servlet-class");
                    servletClass.setTextContent(entry.getValue().toString());
                    servlet.appendChild(servletClass);
                    Map<String, String> initParams = facade.getServletRegistration(entry.getKey().toString()).getInitParameters();
                    if ((initParams != null) && (!initParams.isEmpty())) {
                        Element initParam = doc.createElement("init-param");
                        for (Map.Entry param : initParams.entrySet()) {
                            Element paramName = doc.createElement("param-name");
                            paramName.setTextContent(param.getKey().toString());
                            initParam.appendChild(paramName);
                            Element paramValue = doc.createElement("param-value");
                            paramValue.setTextContent(param.getValue().toString());
                            initParam.appendChild(paramValue);
                        }
                        servlet.appendChild(initParam);
                    }
                    webapp.appendChild(servlet);
                }
            }

            entryFound = false;

            // Update <servlet-mapping>
            for (Map.Entry mapping : mappings.entrySet()) {
                NodeList servletList = doc.getElementsByTagName("servlet-name");
                for (int i = 0; i < servletList.getLength(); i++) {
                    Node servletNode = servletList.item(i);
                    if (mapping.getKey().equals(servletNode.getTextContent()) && servletNode.getParentNode().getNodeName().equals("servlet-mapping")) {
                        NodeList children = servletNode.getParentNode().getChildNodes();
                        for (int j = 0; j < children.getLength(); j++) {
                            Node urlPattern = children.item(j);
                            if (urlPattern.getNodeName().equals("url-pattern")) {
                                // If any of the specified URL patterns are already mapped to a different Servlet,
                                // no updates will be performed according to the spec
                                entryFound = true;
                                break;
                            }
                        }
                    }
                }
                if (!entryFound) {
                    Element servletMapping = doc.createElement("servlet-mapping");
                    for (String pattern : mappings.get(mapping.getKey())) {
                        Element servletName = doc.createElement("servlet-name");
                        servletName.setTextContent(mapping.getKey().toString());
                        servletMapping.appendChild(servletName);
                        Element urlPattern = doc.createElement("url-pattern");
                        urlPattern.setTextContent(pattern);
                        servletMapping.appendChild(urlPattern);
                    }
                    webapp.appendChild(servletMapping);
                }
            }

            for (String listenerStr : flisteners) {
                Element listener = doc.createElement("listener");
                Element listenerClass = doc.createElement("listener-class");
                listenerClass.setTextContent(listenerStr);
                listener.appendChild(listenerClass);
                webapp.appendChild(listener);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            if (file != null) {
                DOMSource src = new DOMSource(doc);
                StreamResult result = new StreamResult(file);
                transformer.transform(src, result);
            }

        }
    }

    private List<NetworkListener> getGrizzlyNetworkListeners() {
        List<String> listenerList = StringUtils.parseStringList(vsBean.getNetworkListeners(), ",");
        String[] listeners = (listenerList != null) ? listenerList.toArray(new String[listenerList.size()]) : new String[0];
        List<NetworkListener> networkListeners = new ArrayList<NetworkListener>();

        for (String listener : listeners) {
            for (NetworkListener networkListener : serverConfig.getNetworkConfig().getNetworkListeners().getNetworkListener()) {
                if (networkListener.getName().equals(listener)) {
                    networkListeners.add(networkListener);
                }
            }
        }

        return networkListeners;
    }

    private List<HttpProbeImpl> getHttpProbeImpl() {
        List<HttpProbeImpl> httpProbes = new ArrayList<>();
        for (final NetworkListener listener : getGrizzlyNetworkListeners()) {
            final GrizzlyProxy proxy = (GrizzlyProxy) grizzlyService.lookupNetworkProxy(listener);
            if (proxy != null) {
                GenericGrizzlyListener grizzlyListener = (GenericGrizzlyListener) proxy.getUnderlyingListener();
                List<HttpCodecFilter> codecFilters = grizzlyListener.getFilters(HttpCodecFilter.class);
                if (codecFilters != null && !codecFilters.isEmpty()) {
                    for (HttpCodecFilter codecFilter : codecFilters) {
                        HttpProbe[] probes = codecFilter.getMonitoringConfig().getProbes();
                        if (probes != null) {
                            for (HttpProbe probe : probes) {
                                if (probe instanceof HttpProbeImpl) {
                                    httpProbes.add((HttpProbeImpl) probe);
                                }
                            }
                        }
                    }
                }
            }
        }

        return httpProbes;
    }

    // ---------------------------------------------------------- Nested Classes

    private final class HttpProbeImpl extends HttpProbe.Adapter {

        boolean accessLoggingEnabled = false;
        NetworkListener listener = null;

        public HttpProbeImpl(NetworkListener listener, boolean accessLoggingEnabled) {
            this.listener = listener;
            this.accessLoggingEnabled = accessLoggingEnabled;
        }

        public void enableAccessLogging() {
            accessLoggingEnabled = true;
        }

        public void disableAccessLogging() {
            accessLoggingEnabled = false;
        }

        @Override
        public void onErrorEvent(Connection connection, HttpPacket packet, Throwable error) {
            if (accessLoggingEnabled) {
                if (packet instanceof HttpRequestPacket) {

                    HttpRequestPacket requestPacket = (HttpRequestPacket) packet;
                    HttpResponsePacket responsePacket = requestPacket.getResponse();

                    // 400 should be hardcoded since the response status isn't available for bad requests
                    responsePacket.setStatus(HttpStatus.BAD_REQUEST_400);

                    org.glassfish.grizzly.http.server.Request request = org.glassfish.grizzly.http.server.Request.create();
                    org.glassfish.grizzly.http.server.Response response = request.getResponse();

                    request.initialize(/* response, */ requestPacket, FilterChainContext.create(connection), null);
                    response.initialize(request, responsePacket, FilterChainContext.create(connection), null, null);

                    Response res = new Response();
                    res.setCoyoteResponse(response);

                    WebConnector connector = webContainer.getConnectorMap().get(listener.getName());
                    if (connector != null) {
                        Request req = new Request();
                        req.setCoyoteRequest(request);
                        req.setConnector(connector);
                        try {
                            accessLogValve.postInvoke(req, res);
                        } catch (IOException ex) {
                            _logger.log(SEVERE, UNABLE_RECONFIGURE_ACCESS_LOG, ex);
                        }
                    } else {
                        _logger.log(SEVERE, UNABLE_RECONFIGURE_ACCESS_LOG);
                    }
                } else {
                    _logger.log(SEVERE, UNABLE_RECONFIGURE_ACCESS_LOG);
                }
            }
        }

    }

}
