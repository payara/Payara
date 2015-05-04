/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.net.BindException;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.tagext.JspTag;

import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Deployer;
import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.ServerInfo;
import org.apache.jasper.runtime.JspFactoryImpl;
import org.apache.jasper.xmlparser.ParserUtils;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.web.TldProvider;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.grizzly.config.ContextRootInfo;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.grizzly.ContextMapper;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.web.admin.monitor.HttpServiceStatsProviderBootstrap;
import org.glassfish.web.admin.monitor.JspProbeProvider;
import org.glassfish.web.admin.monitor.RequestProbeProvider;
import org.glassfish.web.admin.monitor.ServletProbeProvider;
import org.glassfish.web.admin.monitor.SessionProbeProvider;
import org.glassfish.web.admin.monitor.WebModuleProbeProvider;
import org.glassfish.web.admin.monitor.WebStatsProviderBootstrap;
import org.glassfish.web.config.serverbeans.SessionProperties;
import org.glassfish.web.deployment.archivist.WebArchivist;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;
import org.glassfish.web.deployment.util.WebValidatorWithoutCL;
import org.glassfish.web.valve.GlassFishValve;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.types.Property;
import org.xml.sax.EntityResolver;

import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.DasConfig;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.security.integration.RealmInitializer;
import com.sun.enterprise.server.logging.LoggingRuntime;
import com.sun.enterprise.util.Result;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.services.impl.ContainerMapper;
import com.sun.enterprise.v3.services.impl.GrizzlyService;
import com.sun.enterprise.web.connector.coyote.PECoyoteConnector;
import com.sun.enterprise.web.logger.FileLoggerHandlerFactory;
import com.sun.enterprise.web.logger.IASLogger;
import com.sun.enterprise.web.pluggable.WebContainerFeatureFactory;
import com.sun.enterprise.web.reconfig.WebConfigListener;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Web container service
 *
 * @author jluehe
 * @author amyroh
 * @author swchan2
 */
@SuppressWarnings({"StringContatenationInLoop"})
@Service(name = "com.sun.enterprise.web.WebContainer")
@Singleton
public class WebContainer implements org.glassfish.api.container.Container, PostConstruct, PreDestroy, EventListener {

    // -------------------------------------------------- Constants

    public static final String DISPATCHER_MAX_DEPTH = "dispatcher-max-depth";

    public static final String JWS_APPCLIENT_EAR_NAME = "__JWSappclients";
    public static final String JWS_APPCLIENT_WAR_NAME = "sys";
    private static final String JWS_APPCLIENT_MODULE_NAME = JWS_APPCLIENT_EAR_NAME + ":" + JWS_APPCLIENT_WAR_NAME + ".war";

    private static final String DOL_DEPLOYMENT =
            "com.sun.enterprise.web.deployment.backend";

    private static final String MONITORING_NODE_SEPARATOR = "/";

    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE =
            "com.sun.enterprise.web.LogMessages";

    @LoggerInfo(subsystem="WEB", description="Main WEB Logger", publish=true)
    public static final String WEB_MAIN_LOGGER = "javax.enterprise.web";

    public static final Logger logger =
            Logger.getLogger(WEB_MAIN_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static final ResourceBundle rb = logger.getResourceBundle();

    @LogMessageInfo(
            message = "Loading web module {0} in virtual server {1} at {2}",
            level = "INFO")
    public static final String WEB_MODULE_LOADING = "AS-WEB-GLUE-00177";

    @LogMessageInfo(
            message = "This web container has not yet been started",
            level = "INFO")
    public static final String WEB_CONTAINER_NOT_STARTED = "AS-WEB-GLUE-00178";

    @LogMessageInfo(
            message = "Property {0} is not yet supported",
            level = "INFO")
    public static final String PROPERTY_NOT_YET_SUPPORTED = "AS-WEB-GLUE-00179";

    @LogMessageInfo(
            message = "Virtual server {0} already has a web module {1} loaded at {2} therefore web module {3} cannot be loaded at this context path on this virtual server",
            level = "INFO")
    public static final String DUPLICATE_CONTEXT_ROOT = "AS-WEB-GLUE-00180";

    @LogMessageInfo(
            message = "Unable to stop web container",
            level = "SEVERE",
            cause = "Web container may not have been started",
            action = "Verify if web container is started")
    public static final String UNABLE_TO_STOP_WEB_CONTAINER = "AS-WEB-GLUE-00181";

    @LogMessageInfo(
            message = "Unable to start web container",
            level = "SEVERE",
            cause = "Web container may have already been started",
            action = "Verify if web container is not already started")
    public static final String UNABLE_TO_START_WEB_CONTAINER = "AS-WEB-GLUE-00182";

    @LogMessageInfo(
            message = "Property element in sun-web.xml has null 'name' or 'value'",
            level = "INFO")
    public static final String NULL_WEB_PROPERTY = "AS-WEB-GLUE-00183";

    @LogMessageInfo(
            message = "Web module {0} is not loaded in virtual server {1}",
            level = "SEVERE",
            cause = "Web module has failed to load",
            action = "Verify if web module is valid")
    public static final String WEB_MODULE_NOT_LOADED_TO_VS = "AS-WEB-GLUE-00184";

    @LogMessageInfo(
            message = "Unable to deploy web module {0} at root context of virtual server {1}, because this virtual server declares a default-web-module",
            level = "INFO")
    public static final String DEFAULT_WEB_MODULE_CONFLICT = "AS-WEB-GLUE-00185";

    @LogMessageInfo(
            message = "Unable to set default-web-module {0} for virtual server {1}",
            level = "SEVERE",
            cause = "There is no web context deployed on the given" +
                    "virtual server that matches the given default context path",
            action = "Verify if the default context path is deployed on the virtual server")
    public static final String DEFAULT_WEB_MODULE_ERROR= "AS-WEB-GLUE-00186";

    @LogMessageInfo(
            message = "Unable to load web module {0} at context root {1}, because it is not correctly encoded",
            level = "INFO")
    public static final String INVALID_ENCODED_CONTEXT_ROOT = "AS-WEB-GLUE-00187";

    @LogMessageInfo(
            message = "Unable to destroy web module deployed at context root {0} on virtual server {1} during undeployment",
            level = "WARNING")
    public static final String EXCEPTION_DURING_DESTROY = "AS-WEB-GLUE-00188";

    @LogMessageInfo(
            message = "Exception setting the schemas/dtds location",
            level = "SEVERE",
            cause = "A malformed URL has occurred. Either no legal protocol could be found in a specification string " +
                    "or the string could not be parsed",
            action = "Verify if the schemas and dtds")
    public static final String EXCEPTION_SET_SCHEMAS_DTDS_LOCATION = "AS-WEB-GLUE-00189";

    @LogMessageInfo(
            message = "Error loading web module {0}",
            level = "SEVERE",
            cause = "An error occurred during loading web module",
            action = "Check the Exception for the error")
    public static final String LOAD_WEB_MODULE_ERROR = "AS-WEB-GLUE-00191";

    @LogMessageInfo(
            message = "Undeployment failed for context {0}",
            level = "SEVERE",
            cause = "The context may not have been deployed",
            action = "Verify if the context is deployed on the virtual server")
    public static final String UNDEPLOY_ERROR = "AS-WEB-GLUE-00192";

    @LogMessageInfo(
            message = "Exception processing HttpService configuration change",
            level = "SEVERE",
            cause = "An error occurred during configurting http service",
            action = "Verify if the configurations are valid")
    public static final String EXCEPTION_CONFIG_HTTP_SERVICE = "AS-WEB-GLUE-00193";

    @LogMessageInfo(
            message = "Unable to set context root {0}",
            level = "WARNING")
    public static final String UNABLE_TO_SET_CONTEXT_ROOT = "AS-WEB-GLUE-00194";

    @LogMessageInfo(
            message = "Unable to disable web module at context root {0}",
            level = "WARNING")
    public static final String DISABLE_WEB_MODULE_ERROR = "AS-WEB-GLUE-00195";

    @LogMessageInfo(
            message = "Error during destruction of virtual server {0}",
            level = "WARNING")
    public static final String DESTROY_VS_ERROR = "AS-WEB-GLUE-00196";

    @LogMessageInfo(
            message = "Virtual server {0} cannot be updated, because it does not exist",
            level = "WARNING")
    public static final String CANNOT_UPDATE_NON_EXISTENCE_VS= "AS-WEB-GLUE-00197";

    @LogMessageInfo(
            message = "Created HTTP listener {0} on host/port {1}:{2}",
            level = "INFO")
    public static final String HTTP_LISTENER_CREATED = "AS-WEB-GLUE-00198";

    @LogMessageInfo(
            message = "Created JK listener {0} on host/port {1}:{2}",
            level = "INFO")
    public static final String JK_LISTENER_CREATED = "AS-WEB-GLUE-00199";

    @LogMessageInfo(
            message = "Created virtual server {0}",
            level = "INFO")
    public static final String VIRTUAL_SERVER_CREATED = "AS-WEB-GLUE-00200";

    @LogMessageInfo(
            message = "Virtual server {0} loaded default web module {1}",
            level = "INFO")
    public static final String VIRTUAL_SERVER_LOADED_DEFAULT_WEB_MODULE = "AS-WEB-GLUE-00201";

    @LogMessageInfo(
            message = "Maximum depth for nested request dispatches set to {0}",
            level = "FINE")
    public static final String MAX_DISPATCH_DEPTH_SET = "AS-WEB-GLUE-00202";

    @LogMessageInfo(
            message = "Unsupported http-service property {0} is being ignored",
            level = "WARNING")
    public static final String INVALID_HTTP_SERVICE_PROPERTY = "AS-WEB-GLUE-00203";

    @LogMessageInfo(
            message = "The host name {0} is shared by virtual servers {1} and {2}, which are both associated with the same HTTP listener {3}",
            level = "SEVERE",
            cause = "The host name is not unique",
            action = "Verify that the host name is unique")
    public static final String DUPLICATE_HOST_NAME = "AS-WEB-GLUE-00204";

    @LogMessageInfo(
            message = "Network listener {0} referenced by virtual server {1} does not exist",
            level = "SEVERE",
            cause = "Network listener {0} referenced by virtual server {1} does not exist",
            action = "Verify that the network listener is valid")
    public static final String LISTENER_REFERENCED_BY_HOST_NOT_EXIST = "AS-WEB-GLUE-00205";

    @LogMessageInfo(
            message = "Web module {0} not loaded to any virtual servers",
            level = "INFO")
    public static final String WEB_MODULE_NOT_LOADED_NO_VIRTUAL_SERVERS = "AS-WEB-GLUE-00206";

    @LogMessageInfo(
            message = "Loading web module {0} to virtual servers {1}",
            level = "FINE")
    public static final String LOADING_WEB_MODULE = "AS-WEB-GLUE-00207";

    @LogMessageInfo(
            message = "Unloading web module {0} from virtual servers {1}",
            level = "FINE")
    public static final String UNLOADING_WEB_MODULE = "AS-WEB-GLUE-00208";

    @LogMessageInfo(
            message = "Context {0} undeployed from virtual server {1}",
            level = "FINE")
    public static final String CONTEXT_UNDEPLOYED = "AS-WEB-GLUE-00209";

    @LogMessageInfo(
            message = "Context {0} disabled from virtual server {1}",
            level = "FINE")
    public static final String CONTEXT_DISABLED = "AS-WEB-GLUE-00210";

    @LogMessageInfo(
            message = "Virtual server {0}'s network listeners are updated from {1} to {2}",
            level = "FINE")
    public static final String VS_UPDATED_NETWORK_LISTENERS = "AS-WEB-GLUE-00211";

    @LogMessageInfo(
            message = "The class {0} is annotated with an invalid scope",
            level = "INFO")
    public static final String INVALID_ANNOTATION_SCOPE = "AS-WEB-GLUE-00212";

    @LogMessageInfo(
            message = "-DjvmRoute updated with {0}",
            level = "FINE")
    public static final String JVM_ROUTE_UPDATED= "AS-WEB-GLUE-00213";

    @LogMessageInfo(
            message = "Unable to parse port number {0} of network-listener {1}",
            level = "INFO")
    public static final String HTTP_LISTENER_INVALID_PORT = "AS-WEB-GLUE-00214";

    @LogMessageInfo(
            message = "Virtual server {0} set listener name {1}",
            level = "FINE")
    public static final String VIRTUAL_SERVER_SET_LISTENER_NAME = "AS-WEB-GLUE-00215";

    @LogMessageInfo(
            message = "Must not disable network-listener {0}, because it is associated with admin virtual server {1}",
            level = "INFO")
    public static final String MUST_NOT_DISABLE = "AS-WEB-GLUE-00216";

    @LogMessageInfo(
            message = "Virtual server {0} set jk listener name {1}",
            level = "FINE")
    public static final String VIRTUAL_SERVER_SET_JK_LISTENER_NAME = "AS-WEB-GLUE-00217";

    @LogMessageInfo(
            message = "virtual server {0} has an invalid docroot {1}",
            level = "INFO")
    public static final String VIRTUAL_SERVER_INVALID_DOCROOT = "AS-WEB-GLUE-00218";

    @LogMessageInfo(
            message = "{0} network listener is not included in {1} and will be updated ",
            level = "FINE")
    public static final String UPDATE_LISTENER = "AS-WEB-GLUE-00219";

    /**
     * Are we using Tomcat deployment backend or DOL?
     */
    static boolean useDOLforDeployment = true;

    // ----------------------------------------------------- Instance Variables

    @Inject
    private ApplicationRegistry appRegistry;

    @Inject
    private ClassLoaderHierarchy clh;

    @Inject
    private ComponentEnvManager componentEnvManager;

    @Inject
    private Configs configs;

    @Inject @Optional
    private DasConfig dasConfig;

    @Inject
    private Domain domain;

    @Inject
    private Events events;

    @Inject
    private FileLoggerHandlerFactory fileLoggerHandlerFactory;

    @Inject
    private GrizzlyService grizzlyService;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private JavaEEIOUtils javaEEIOUtils;

    @Inject @Optional
    private JCDIService jcdiService;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config serverConfig;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Server server;

    @Inject
    private ServerContext _serverContext;

    @Inject
    private Transactions transactions;
    
    @Inject
    private LoggingRuntime loggingRuntime;

    private HashMap<String, WebConnector> connectorMap = new HashMap<String, WebConnector>();

    private EmbeddedWebContainer _embedded;

    private Engine engine;

    private String instanceName;

    private WebConnector jkConnector;

    private String logLevel = "INFO";
    /**
     * Allow disabling accessLog mechanism
     */
    protected boolean globalAccessLoggingEnabled = true;

    /**
     * AccessLog buffer size for storing logs.
     */
    protected String globalAccessLogBufferSize = null;

    /**
     * AccessLog interval before the valve flush its buffer to the disk.
     */
    protected String globalAccessLogWriteInterval = null;

    /**
     * The default-redirect port
     */
    protected int defaultRedirectPort = -1;

    /**
     * <tt>false</tt> when the Grizzly File Cache is enabled. When disabled
     * the Servlet Container temporary Naming cache is used when loading the
     * resources.
     */
    //protected boolean catalinaCachingAllowed = true;

    @Inject
    protected ServerEnvironment instance = null;

    // TODO
    //protected WebModulesManager webModulesManager = null;
    //protected AppsManager appsManager = null;

    /**
     * The schema2beans object that represents the root node of server.xml.
     */
    private Server _serverBean = null;

    /**
     * Controls the verbosity of the web container subsystem's debug messages.
     * <p/>
     * This value is non-zero only when the iAS level is one of FINE, FINER
     * or FINEST.
     */
    protected int _debug = 0;

    /**
     * Top-level directory for files generated (compiled JSPs) by
     * standalone web modules.
     */
    private String _modulesWorkRoot = null;

    /**
     * Absolute path for location where all the deployed
     * standalone modules are stored for this Server Instance.
     */
    protected File _modulesRoot = null;

    /**
     * Top-level directory for files generated by application web modules.
     */
    private String _appsWorkRoot = null;

    // START S1AS 6178005
    /**
     * Top-level directory where ejb stubs for applications are stored.
     */
    private String appsStubRoot = null;
    // END S1AS 6178005

    /**
     * Indicates whether dynamic reloading is enabled (as specified by
     * the dynamic-reload-enabled attribute of <applications> in server.xml)
     */
    private boolean _reloadingEnabled = false;

    /**
     * The number of seconds between checks for modified classes (if
     * dynamic reloading is enabled).
     * <p/>
     * This value is specified by the reload-poll-interval attribute of
     * <applications> in server.xml.
     */
    private int _pollInterval = 2;

    /**
     * Has this component been started yet?
     */
    protected boolean _started = false;

    /**
     * The global (at the http-service level) ssoEnabled property.
     */
    protected boolean globalSSOEnabled = true;

    protected volatile WebContainerFeatureFactory webContainerFeatureFactory;

    /**
     * The value of the instance-level session property named "enableCookies"
     */
    boolean instanceEnableCookies = true;

    @Inject
    ServerConfigLookup serverConfigLookup;

    protected JspProbeProvider jspProbeProvider = null;
    protected RequestProbeProvider requestProbeProvider = null;
    protected ServletProbeProvider servletProbeProvider = null;
    protected SessionProbeProvider sessionProbeProvider = null;
    protected WebModuleProbeProvider webModuleProbeProvider = null;

    protected WebConfigListener configListener = null;

    // Indicates whether we are being shut down
    private boolean isShutdown = false;

    private final Object mapperUpdateSync = new Object();

    private SecurityService securityService = null;

    protected HttpServiceStatsProviderBootstrap httpStatsProviderBootstrap = null;

    private WebStatsProviderBootstrap webStatsProviderBootstrap = null;

    private InjectionManager injectionMgr;

    private InvocationManager invocationMgr;

    private Collection<TldProvider> tldProviders;

    private String logServiceFile = null;

    /**
     * Static initialization
     */
    static {
        if (System.getProperty(DOL_DEPLOYMENT) != null) {
            useDOLforDeployment = Boolean.valueOf(System.getProperty(DOL_DEPLOYMENT));
        }
    }
    
    private WebConfigListener addAndGetWebConfigListener() {
    	ServiceLocator locator = (ServiceLocator) habitat;
    	
    	DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
    	DynamicConfiguration config = dcs.createDynamicConfiguration();
    	
    	config.addActiveDescriptor(WebConfigListener.class);
    	
    	config.commit();
    	
    	return locator.getService(WebConfigListener.class);
    }

    public void postConstruct() {

        final ReentrantReadWriteLock mapperLock = grizzlyService.obtainMapperLock();
        mapperLock.writeLock().lock();
        
        try {
            createProbeProviders();

            injectionMgr = habitat.getService(InjectionManager.class);
            invocationMgr = habitat.getService(InvocationManager.class);
            tldProviders = habitat.getAllServices(TldProvider.class);

            createStatsProviders();

            setJspFactory();

            _appsWorkRoot =
                    instance.getApplicationCompileJspPath().getAbsolutePath();
            _modulesRoot = instance.getApplicationRepositoryPath();

            // START S1AS 6178005
            appsStubRoot = instance.getApplicationStubPath().getAbsolutePath();
            // END S1AS 6178005

            // TODO: ParserUtils should become a @Service and it should initialize itself.
            // TODO: there should be only one EntityResolver for both DigesterFactory
            // and ParserUtils
            File root = _serverContext.getInstallRoot();
            File libRoot = new File(root, "lib");
            File schemas = new File(libRoot, "schemas");
            File dtds = new File(libRoot, "dtds");

            try {
                ParserUtils.setSchemaResourcePrefix(schemas.toURI().toURL().toString());
                ParserUtils.setDtdResourcePrefix(dtds.toURI().toURL().toString());
                ParserUtils.setEntityResolver(habitat.<EntityResolver>getService(EntityResolver.class, "web"));
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, EXCEPTION_SET_SCHEMAS_DTDS_LOCATION, e);
            }

            instanceName = _serverContext.getInstanceName();

            webContainerFeatureFactory = getWebContainerFeatureFactory();

            configureDynamicReloadingSettings();
            setDebugLevel();

            String maxDepth = null;
            org.glassfish.web.config.serverbeans.WebContainer configWC =
                   serverConfig.getExtensionByType(
                   org.glassfish.web.config.serverbeans.WebContainer.class); 
            if (configWC != null)
                maxDepth = configWC.getPropertyValue(DISPATCHER_MAX_DEPTH);
            if (maxDepth != null) {
                int depth = -1;
                try {
                    depth = Integer.parseInt(maxDepth);
                } catch (NumberFormatException e) {
                }

                if (depth > 0) {
                    Request.setMaxDispatchDepth(depth);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, MAX_DISPATCH_DEPTH_SET, maxDepth);
                    }
                }
            }

            File currentLogFile = loggingRuntime.getCurrentLogFile();
            if (currentLogFile != null) {
                logServiceFile = currentLogFile.getAbsolutePath();            
            }

            Level level = Logger.getLogger("org.apache.catalina.level").getLevel();
            if (level != null) {
                logLevel = level.getName();
            }
            _embedded = habitat.getService(EmbeddedWebContainer.class);
            _embedded.setWebContainer(this);
            _embedded.setLogServiceFile(logServiceFile);
            _embedded.setLogLevel(logLevel);
            _embedded.setFileLoggerHandlerFactory(fileLoggerHandlerFactory);
            _embedded.setWebContainerFeatureFactory(webContainerFeatureFactory);

            _embedded.setCatalinaHome(instance.getDomainRoot().getAbsolutePath());
            _embedded.setCatalinaBase(instance.getDomainRoot().getAbsolutePath());
            _embedded.setUseNaming(false);
            if (_debug > 1)
                _embedded.setDebug(_debug);
            _embedded.setLogger(new IASLogger(logger));
            engine = _embedded.createEngine();
            engine.setParentClassLoader(EmbeddedWebContainer.class.getClassLoader());
            engine.setService(_embedded);
            _embedded.addEngine(engine);
            ((StandardEngine) engine).setDomain(_serverContext.getDefaultDomainName());
            engine.setName(_serverContext.getDefaultDomainName());

            /*
            * Set the server info.
            * By default, the server info is taken from Version#getVersion.
            * However, customers may override it via the product.name system
            * property.
            * Some customers prefer not to disclose the server info
            * for security reasons, in which case they would set the value of the
            * product.name system property to the empty string. In this case,
            * the server name will not be publicly disclosed via the "Server"
            * HTTP response header (which will be suppressed) or any container
            * generated error pages. However, it will still appear in the
            * server logs (see IT 6900).
            */
            String serverInfo = System.getProperty("product.name");
            if (serverInfo == null) {
                ServerInfo.setServerInfo(Version.getVersion());
                ServerInfo.setPublicServerInfo(Version.getVersion());
            } else if (serverInfo.isEmpty()) {
                ServerInfo.setServerInfo(Version.getVersion());
                ServerInfo.setPublicServerInfo(serverInfo);
            } else {
                ServerInfo.setServerInfo(serverInfo);
                ServerInfo.setPublicServerInfo(serverInfo);
            }

            initInstanceSessionProperties();

            configListener = addAndGetWebConfigListener();

            ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(
                    serverConfig.getHttpService());
            bean.addListener(configListener);

            bean = (ObservableBean) ConfigSupport.getImpl(
                    serverConfig.getNetworkConfig().getNetworkListeners());
            bean.addListener(configListener);

            if (serverConfig.getAvailabilityService() != null) {
                bean = (ObservableBean) ConfigSupport.getImpl(
                        serverConfig.getAvailabilityService());
                bean.addListener(configListener);
            }

            transactions.addListenerForType(SystemProperty.class, configListener);

            configListener.setNetworkConfig(serverConfig.getNetworkConfig());

            // embedded mode does not have manager-propertie in domain.xml
            if (configListener.managerProperties != null) {
                ObservableBean managerBean = (ObservableBean) ConfigSupport.getImpl(
                        configListener.managerProperties);
                managerBean.addListener(configListener);
            }

            if (serverConfig.getJavaConfig() != null) {
                ((ObservableBean)ConfigSupport.getImpl(
                        serverConfig.getJavaConfig())).addListener(configListener);
            }

            configListener.setContainer(this);
            configListener.setLogger(logger);

            events.register(this);

            grizzlyService.addMapperUpdateListener(configListener);

            HttpService httpService = serverConfig.getHttpService();
            NetworkConfig networkConfig = serverConfig.getNetworkConfig();
            if (networkConfig != null) {
                //continue;
                securityService = serverConfig.getSecurityService();

                // Configure HTTP listeners
                NetworkListeners networkListeners = networkConfig.getNetworkListeners();
                if (networkListeners != null) {
                    List<NetworkListener> listeners = networkListeners.getNetworkListener();
                    for (NetworkListener listener : listeners) {
                        createHttpListener(listener, httpService);
                    }
                }

                setDefaultRedirectPort(defaultRedirectPort);

                // Configure virtual servers
                createHosts(httpService, securityService);
            }

            loadSystemDefaultWebModules();

            //_lifecycle.fireLifecycleEvent(START_EVENT, null);
            _started = true;

            /*
             * Start the embedded container.
             * Make sure to set the thread's context classloader to the
             * classloader of this class (see IT 8866 for details)
             */
            ClassLoader current = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(
                    getClass().getClassLoader());
            try {
                /*
                 * Trigger a call to sun.awt.AppContext.getAppContext().
                 * This will pin the classloader of this class in memory
                 * and fix a memory leak affecting instances of WebappClassLoader
                 * that was caused by a JRE implementation change in 1.6.0_15
                 * onwards. See IT 11110
                 */
                ImageIO.getCacheDirectory();
                _embedded.start();
            } catch (LifecycleException le) {
                logger.log(Level.SEVERE, UNABLE_TO_START_WEB_CONTAINER, le);
                return;
            } finally {
                // Restore original context classloader
                Thread.currentThread().setContextClassLoader(current);
            }
        } finally {
            mapperLock.writeLock().unlock();
        }
    }

    public void event(Event event) {
        if (event.is(Deployment.ALL_APPLICATIONS_PROCESSED)) {
            // configure default web modules for virtual servers after all
            // applications are processed
            loadDefaultWebModulesAfterAllAppsProcessed();
        } else if (event.is(EventTypes.PREPARE_SHUTDOWN)) {
            isShutdown = true;
        }
    }

    /**
     * Notifies any interested listeners that all ServletContextListeners
     * of the web module represented by the given WebBundleDescriptor
     * have been invoked at their contextInitialized method
     */
    void afterServletContextInitializedEvent(WebBundleDescriptor wbd) {
        events.send(new Event<WebBundleDescriptor>(
                WebBundleDescriptor.AFTER_SERVLET_CONTEXT_INITIALIZED_EVENT, wbd),
                false);
    }

    public void preDestroy() {
        try {
            for (Connector con : _embedded.findConnectors()) {
                deleteConnector((WebConnector)con);
            }
            _embedded.removeEngine(getEngine());
            _embedded.destroy();
        } catch (LifecycleException le) {
            logger.log(Level.SEVERE, UNABLE_TO_STOP_WEB_CONTAINER, le);
            return;
        }
    }

    JavaEEIOUtils getJavaEEIOUtils() {
        return javaEEIOUtils;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    Collection<TldProvider> getTldProviders() {
        return tldProviders;
    }

    /**
     * Gets the probe provider for servlet related events.
     */
    public ServletProbeProvider getServletProbeProvider() {
        return servletProbeProvider;
    }


    /**
     * Gets the probe provider for jsp related events.
     */
    public JspProbeProvider getJspProbeProvider() {
        return jspProbeProvider;
    }


    /**
     * Gets the probe provider for session related events.
     */
    public SessionProbeProvider getSessionProbeProvider() {
        return sessionProbeProvider;
    }

    /**
     * Gets the probe provider for request/response related events.
     */
    public RequestProbeProvider getRequestProbeProvider() {
        return requestProbeProvider;
    }

    /**
     * Gets the probe provider for web module related events.
     */
    public WebModuleProbeProvider getWebModuleProbeProvider() {
        return webModuleProbeProvider;
    }

    public String getName() {
        return "Web";
    }

    public Class<? extends WebDeployer> getDeployer() {
        return WebDeployer.class;
    }

    InvocationManager getInvocationManager() {
        return invocationMgr;
    }

    public WebConnector getJkConnector() {
        return jkConnector;
    }

    public HashMap<String, WebConnector> getConnectorMap() {
        return connectorMap;
    }

    /**
     * Instantiates and injects the given Servlet class for the given
     * WebModule
     */
    <T extends Servlet> T createServletInstance(WebModule module,
                                                Class<T> clazz) throws Exception {
        validateJSR299Scope(clazz);
        WebComponentInvocation inv = new WebComponentInvocation(module);
        try {
            invocationMgr.preInvoke(inv);
            return injectionMgr.createManagedObject(clazz);
        } finally {
            invocationMgr.postInvoke(inv);
        }
    }

    /**
     * Instantiates and injects the given Filter class for the given
     * WebModule
     */
    <T extends Filter> T createFilterInstance(WebModule module,
                                              Class<T> clazz) throws Exception {
        validateJSR299Scope(clazz);
        WebComponentInvocation inv = new WebComponentInvocation(module);
        try {
            invocationMgr.preInvoke(inv);
            return injectionMgr.createManagedObject(clazz);
        } finally {
            invocationMgr.postInvoke(inv);
        }
    }

    /**
     * Instantiates and injects the given EventListener class for the
     * given WebModule
     */
    <T extends java.util.EventListener> T createListenerInstance(
            WebModule module, Class<T> clazz) throws Exception {
        validateJSR299Scope(clazz);
        WebComponentInvocation inv = new WebComponentInvocation(module);
        try {
            invocationMgr.preInvoke(inv);
            return injectionMgr.createManagedObject(clazz);
        } finally {
            invocationMgr.postInvoke(inv);
        }
    }

    /**
     * Instantiates and injects the given HttpUpgradeHandler class for the
     * given WebModule
     */
    <T extends HttpUpgradeHandler> T createHttpUpgradeHandlerInstance(
            WebModule module, Class<T> clazz) throws Exception {
        validateJSR299Scope(clazz);
        WebComponentInvocation inv = new WebComponentInvocation(module);
        try {
            invocationMgr.preInvoke(inv);
            return injectionMgr.createManagedObject(clazz);
        } finally {
            invocationMgr.postInvoke(inv);
        }
    }

    /**
     * Instantiates and injects the given tag handler class for the given
     * WebModule
     */
    public <T extends JspTag> T createTagHandlerInstance(WebModule module,
                                                         Class<T> clazz) throws Exception {
        WebComponentInvocation inv = new WebComponentInvocation(module);
        try {
            invocationMgr.preInvoke(inv);
            return injectionMgr.createManagedObject(clazz);
        } finally {
            invocationMgr.postInvoke(inv);
        }
    }

    /**
     * Use an network-listener subelements and creates a corresponding
     * Tomcat Connector for each.
     *
     * @param httpService The http-service element
     * @param listener    the configuration element.
     */
    protected WebConnector createHttpListener(NetworkListener listener,
                                              HttpService httpService) {
        return createHttpListener(listener, httpService, null);
    }


    protected WebConnector createHttpListener(NetworkListener listener,
                                              HttpService httpService,
                                              Mapper mapper) {

        if (!Boolean.valueOf(listener.getEnabled())) {
            return null;
        }

        int port = 8080;
        WebConnector connector;

        checkHostnameUniqueness(listener.getName(), httpService);

        try {
            port = Integer.parseInt(listener.getPort());
        } catch (NumberFormatException nfe) {
            String msg = rb.getString(HTTP_LISTENER_INVALID_PORT);
            msg = MessageFormat.format(msg, listener.getPort(),
                    listener.getName());
            throw new IllegalArgumentException(msg);
        }

        if (mapper == null) {
            for (Mapper m : habitat.<Mapper>getAllServices(Mapper.class)) {
                if (m.getPort() == port && m instanceof ContextMapper) {
                    ContextMapper cm = (ContextMapper) m;
                    if (listener.getName().equals(cm.getId())) {
                        mapper = m;
                        break;
                    }
                }
            }
        }

        String defaultVS = listener.findHttpProtocol().getHttp().getDefaultVirtualServer();
        if (!defaultVS.equals(org.glassfish.api.web.Constants.ADMIN_VS)) {
            // Before we start a WebConnector, let's makes sure there is
            // not another Container already listening on that port
            DataChunk host = DataChunk.newInstance();
            char[] c = defaultVS.toCharArray();
            host.setChars(c, 0, c.length);

            DataChunk mb = DataChunk.newInstance();
            mb.setChars(new char[]{'/'}, 0, 1);

            MappingData md = new MappingData();
            try {
                mapper.map(host, mb, md);
            } catch (Exception e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "", e);
                }
            }

            if (md.context != null && md.context instanceof ContextRootInfo) {
                ContextRootInfo r = (ContextRootInfo) md.context;
                if (!(r.getHttpHandler() instanceof ContainerMapper)){
                    new BindException("Port " + port + " is already used by Container: "
                            + r.getHttpHandler() +
                            " and will not get started.").printStackTrace();
                    return null;
                }
            }
        }

        /*
         * Create Connector. Connector is SSL-enabled if
         * 'security-enabled' attribute in <http-listener>
         * element is set to TRUE.
         */
        boolean isSecure = Boolean.valueOf(listener.findHttpProtocol().getSecurityEnabled());
        if (isSecure && defaultRedirectPort == -1) {
            defaultRedirectPort = port;
        }
        String address = listener.getAddress();
        if ("any".equals(address) || "ANY".equals(address)
                || "INADDR_ANY".equals(address)) {
            address = null;
            /*
             * Setting 'address' to NULL will cause Tomcat to pass a
             * NULL InetAddress argument to the java.net.ServerSocket
             * constructor, meaning that the server socket will accept
             * connections on any/all local addresses.
             */
        }

        connector = (WebConnector) _embedded.createConnector(
                address, port, isSecure);

        connector.setMapper(mapper);
        connector.setJvmRoute(engine.getJvmRoute());

        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, HTTP_LISTENER_CREATED,
                    new Object[]{listener.getName(), listener.getAddress(), listener.getPort()});
        }

        connector.setDefaultHost(listener.findHttpProtocol().getHttp().getDefaultVirtualServer());
        connector.setName(listener.getName());
        connector.setInstanceName(instanceName);
        connector.configure(listener, isSecure, httpService);

        _embedded.addConnector(connector);

        connectorMap.put(listener.getName(), connector);

        // If we already know the redirect port, then set it now
        // This situation will occurs when dynamic reconfiguration occurs
        String redirectPort = listener.findHttpProtocol().getHttp().getRedirectPort();
        if (redirectPort != null) {
            connector.setRedirectPort(Integer.parseInt(redirectPort));
        } else if (defaultRedirectPort != -1) {
            connector.setRedirectPort(defaultRedirectPort);
        }

        ObservableBean httpListenerBean = (ObservableBean) ConfigSupport.getImpl(
                listener);
        httpListenerBean.addListener(configListener);

        return connector;
    }

    /**
     * Starts the AJP connector that will listen to call from Apache using
     * mod_jk, mod_jk2 or mod_ajp.
     */
    protected WebConnector createJKConnector(NetworkListener listener,
                                             HttpService httpService) {

        int port = 8009;
        boolean isSecure = false;
        String address = null;

        if (listener == null) {
            String portString =
                    System.getProperty("com.sun.enterprise.web.connector.enableJK");
            if (portString == null) {
                // do not create JK Connector if property is not set
                return null;
            } else {
                try {
                    port = Integer.parseInt(portString);
                } catch (NumberFormatException ex) {
                    // use default port 8009
                    port = 8009;
                }
            }
        } else {
            port = Integer.parseInt(listener.getPort());
            isSecure = Boolean.valueOf(listener.findHttpProtocol().getSecurityEnabled());
            address = listener.getAddress();
        }

        if (isSecure && defaultRedirectPort == -1) {
            defaultRedirectPort = port;
        }

        if ("any".equals(address) || "ANY".equals(address)
                || "INADDR_ANY".equals(address)) {
            address = null;
            /*
             * Setting 'address' to NULL will cause Tomcat to pass a
             * NULL InetAddress argument to the java.net.ServerSocket
             * constructor, meaning that the server socket will accept
             * connections on any/all local addresses.
             */
        }

        jkConnector = (WebConnector) _embedded.createConnector(address,
                port, "ajp");
        jkConnector.configureJKProperties(listener);

        String defaultHost = "server";
        String jkConnectorName = "jk-connector";
        if (listener != null) {
            defaultHost = listener.findHttpProtocol().getHttp().getDefaultVirtualServer();
            jkConnectorName = listener.getName();
        }
        jkConnector.setDefaultHost(defaultHost);
        jkConnector.setName(jkConnectorName);
        jkConnector.setDomain(_serverContext.getDefaultDomainName());
        jkConnector.setInstanceName(instanceName);
        if (listener != null) {
            jkConnector.configure(listener, isSecure, httpService);
            connectorMap.put(listener.getName(), jkConnector);
        }

        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, JK_LISTENER_CREATED,
                    new Object[]{listener.getName(), listener.getAddress(), listener.getPort()});
        }

        for (Mapper m : habitat.<Mapper>getAllServices(Mapper.class)) {
            if (m.getPort() == port && m instanceof ContextMapper) {
                ContextMapper cm = (ContextMapper) m;
                if (listener.getName().equals(cm.getId())) {
                    jkConnector.setMapper(m);
                    break;
                }
            }
        }

        _embedded.addConnector(jkConnector);


        return jkConnector;

    }

    /**
     * Assigns the given redirect port to each Connector whose corresponding
     * http-listener element in domain.xml does not specify its own
     * redirect-port attribute.
     * <p/>
     * The given defaultRedirectPort corresponds to the port number of the
     * first security-enabled http-listener in domain.xml.
     * <p/>
     * This method does nothing if none of the http-listener elements is
     * security-enabled, in which case Tomcat's default redirect port (443)
     * will be used.
     *
     * @param defaultRedirectPort The redirect port to be assigned to any
     *                            Connector object that doesn't specify its own
     */
    private void setDefaultRedirectPort(int defaultRedirectPort) {
        if (defaultRedirectPort != -1) {
            Connector[] connectors = _embedded.getConnectors();
            for (Connector connector : connectors) {
                if (connector.getRedirectPort() == -1) {
                    connector.setRedirectPort(defaultRedirectPort);
                }
            }
        }
    }

    /**
     * Configure http-service properties.
     *
     * @deprecated most of these properties are handled elsewhere.  validate and remove outdated properties checks
     */
    public void configureHttpServiceProperties(HttpService httpService,
                                               PECoyoteConnector connector) {
        // Configure Connector with <http-service> properties
        List<Property> httpServiceProps = httpService.getProperty();

        // Set default ProxyHandler impl, may be overriden by
        // proxyHandler property
        connector.setProxyHandler(new ProxyHandlerImpl());

        globalSSOEnabled = ConfigBeansUtilities.toBoolean(httpService.getSsoEnabled());
        globalAccessLoggingEnabled = ConfigBeansUtilities.toBoolean(httpService.getAccessLoggingEnabled());
        globalAccessLogWriteInterval = httpService.getAccessLog().getWriteIntervalSeconds();
        globalAccessLogBufferSize = httpService.getAccessLog().getBufferSizeBytes();
        if (httpServiceProps != null) {
            for (Property httpServiceProp : httpServiceProps) {
                String propName = httpServiceProp.getName();
                String propValue = httpServiceProp.getValue();

                if (connector.configureHttpListenerProperty(propName, propValue)) {
                    continue;
                }

                if ("connectionTimeout".equals(propName)) {
                    connector.setConnectionTimeout(Integer.parseInt(propValue));
                } else if ("tcpNoDelay".equals(propName)) {
                    connector.setTcpNoDelay(ConfigBeansUtilities.toBoolean(propValue));
                } else if ("traceEnabled".equals(propName)) {
                    connector.setAllowTrace(ConfigBeansUtilities.toBoolean(propValue));
                } else if ("ssl-session-timeout".equals(propName)) {
                    connector.setSslSessionTimeout(propValue);
                } else if ("ssl3-session-timeout".equals(propName)) {
                    connector.setSsl3SessionTimeout(propValue);
                } else if ("ssl-cache-entries".equals(propName)) {
                    connector.setSslSessionCacheSize(propValue);
                } else if ("proxyHandler".equals(propName)) {
                    connector.setProxyHandler(propValue);
                } else {
                    String msg = rb.getString(INVALID_HTTP_SERVICE_PROPERTY);
                    logger.log(Level.WARNING, MessageFormat.format(msg, httpServiceProp.getName()));;
                }
            }
        }
    }

    /*
     * Ensures that the host names of all virtual servers associated with the
     * HTTP listener with the given listener id are unique.
     *
     * @param listenerId The id of the HTTP listener whose associated virtual
     * servers are checked for uniqueness of host names
     * @param httpService The http-service element whose virtual servers are
     * checked
     */

    private void checkHostnameUniqueness(String listenerId,
                                         HttpService httpService) {

        List<com.sun.enterprise.config.serverbeans.VirtualServer> listenerVses = null;

        // Determine all the virtual servers associated with the given listener
        for (com.sun.enterprise.config.serverbeans.VirtualServer vse : httpService.getVirtualServer()) {
            List<String> vsListeners = StringUtils.parseStringList(vse.getNetworkListeners(), ",");
            for (int j = 0; vsListeners != null && j < vsListeners.size(); j++) {
                if (listenerId.equals(vsListeners.get(j))) {
                    if (listenerVses == null) {
                        listenerVses = new ArrayList<com.sun.enterprise.config.serverbeans.VirtualServer>();
                    }
                    listenerVses.add(vse);
                    break;
                }
            }
        }
        if (listenerVses == null) {
            return;
        }

        for (int i = 0; i < listenerVses.size(); i++) {
            com.sun.enterprise.config.serverbeans.VirtualServer vs
                    = listenerVses.get(i);
            List hosts = StringUtils.parseStringList(vs.getHosts(), ",");
            for (int j = 0; hosts != null && j < hosts.size(); j++) {
                String host = (String) hosts.get(j);
                for (int k = 0; k < listenerVses.size(); k++) {
                    if (k <= i) {
                        continue;
                    }
                    com.sun.enterprise.config.serverbeans.VirtualServer otherVs
                            = listenerVses.get(k);
                    List otherHosts = StringUtils.parseStringList(otherVs.getHosts(), ",");
                    for (int l = 0; otherHosts != null && l < otherHosts.size(); l++) {
                        if (host.equals(otherHosts.get(l))) {
                            logger.log(Level.SEVERE,
                                    DUPLICATE_HOST_NAME,
                                    new Object[]{host, vs.getId(),
                                            otherVs.getId(),
                                            listenerId});
                        }
                    }
                }
            }
        }
    }


    /**
     * Enumerates the virtual-server subelements of the given http-service
     * element, and creates a corresponding Host for each.
     *
     * @param httpService     The http-service element
     * @param securityService The security-service element
     */
    protected void createHosts(HttpService httpService, SecurityService securityService) {

        List<com.sun.enterprise.config.serverbeans.VirtualServer> virtualServers = httpService.getVirtualServer();
        for (com.sun.enterprise.config.serverbeans.VirtualServer vs : virtualServers) {
            createHost(vs, httpService, securityService);
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, VIRTUAL_SERVER_CREATED, vs.getId());
            }

        }
    }


    /**
     * Creates a Host from a virtual-server config bean.
     *
     * @param vsBean          The virtual-server configuration bean
     * @param httpService     The http-service element.
     * @param securityService The security-service element
     */
    public VirtualServer createHost(
            com.sun.enterprise.config.serverbeans.VirtualServer vsBean,
            HttpService httpService,
            SecurityService securityService) {

        String vs_id = vsBean.getId();

        String docroot = vsBean.getPropertyValue("docroot");
        if (docroot == null) {
            docroot = vsBean.getDocroot();
        }

        validateDocroot(docroot,
                vs_id,
                vsBean.getDefaultWebModule());

        VirtualServer vs = createHost(vs_id, vsBean, docroot, null
        );

        // cache control
        Property cacheProp = vsBean.getProperty("setCacheControl");
        if (cacheProp != null) {
            vs.configureCacheControl(cacheProp.getValue());
        }

        PEAccessLogValve accessLogValve = vs.getAccessLogValve();
        boolean startAccessLog = accessLogValve.configure(
                vs_id, vsBean, httpService, domain,
                habitat, webContainerFeatureFactory,
                globalAccessLogBufferSize, globalAccessLogWriteInterval);
        if (startAccessLog
                && vs.isAccessLoggingEnabled(globalAccessLoggingEnabled)) {
            vs.addValve((GlassFishValve) accessLogValve);
        }

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, VIRTUAL_SERVER_CREATED, vs_id);
        }

        /*
         * We must configure the Host with its associated port numbers and
         * alias names before adding it as an engine child and thereby
         * starting it, because a MapperListener, which is associated with
         * an HTTP listener and receives notifications about Host
         * registrations, relies on these Host properties in order to determine
         * whether a new Host needs to be added to the HTTP listener's Mapper.
         */
        configureHost(vs, securityService);
        vs.setDomain(domain);
        vs.setServices(habitat);
        vs.setClassLoaderHierarchy(clh);

        // Add Host to Engine
        engine.addChild(vs);

        ObservableBean virtualServerBean = (ObservableBean) ConfigSupport.getImpl(vsBean);
        virtualServerBean.addListener(configListener);

        return vs;
    }


    /**
     * Validate the docroot properties of a virtual-server.
     */
    protected void validateDocroot(String docroot, String vs_id,
                                   String defaultWebModule) {
        if (docroot == null) {
            return;
        }

        boolean isValid = new File(docroot).exists();
        if (!isValid) {
            String msg = rb.getString(VIRTUAL_SERVER_INVALID_DOCROOT);
            msg = MessageFormat.format(msg, vs_id, docroot);
            throw new IllegalArgumentException(msg);
        }
    }


    /**
     * Configures the given virtual server.
     *
     * @param vs              The virtual server to be configured
     * @param securityService The security-service element
     */
    protected void configureHost(VirtualServer vs, SecurityService securityService) {

        com.sun.enterprise.config.serverbeans.VirtualServer vsBean = vs.getBean();

        vs.configureAliases();

        // Set the ports with which this virtual server is associated
        List<String> listeners = StringUtils.parseStringList(
                vsBean.getNetworkListeners(), ",");
        if (listeners == null) {
            return;
        }

        HashSet<NetworkListener> httpListeners = new HashSet<NetworkListener>();
        for (String listener : listeners) {
            boolean found = false;
            for (NetworkListener httpListener :
                    serverConfig.getNetworkConfig().getNetworkListeners().getNetworkListener()) {
                if (httpListener.getName().equals(listener)) {
                    httpListeners.add(httpListener);
                    found = true;
                    break;
                }
            }
            if (!found) {
                String msg = rb.getString(LISTENER_REFERENCED_BY_HOST_NOT_EXIST);
                msg = MessageFormat.format(msg, listener, vs.getName());
                logger.log(Level.SEVERE, msg);
            }
        }

        configureHostPortNumbers(vs, httpListeners);
        vs.configureCatalinaProperties();
        vs.configureAuthRealm(securityService);
        vs.addProbes(globalAccessLoggingEnabled);
    }


    /**
     * Configures the given virtual server with the port numbers of its
     * associated http listeners.
     *
     * @param vs        The virtual server to configure
     * @param listeners The http listeners with which the given virtual
     *                  server is associated
     */
    protected void configureHostPortNumbers(VirtualServer vs,
                                            HashSet<NetworkListener> listeners) {

        boolean addJkListenerName = jkConnector != null &&
                !vs.getName().equalsIgnoreCase(
                        org.glassfish.api.web.Constants.ADMIN_VS);

        List<String> listenerNames = new ArrayList<String>();
        for (NetworkListener listener : listeners) {
            if (Boolean.valueOf(listener.getEnabled())) {
                listenerNames.add(listener.getName());
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, VIRTUAL_SERVER_SET_LISTENER_NAME);
                }
            } else {
                if (vs.getName().equalsIgnoreCase(
                        org.glassfish.api.web.Constants.ADMIN_VS)) {
                    String msg = rb.getString(MUST_NOT_DISABLE);
                    msg = MessageFormat.format(msg, listener.getName(),
                            vs.getName());
                    throw new IllegalArgumentException(msg);
                }
            }
        }

        if (addJkListenerName && (!listenerNames.contains(jkConnector.getName()))) {
            listenerNames.add(jkConnector.getName());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        VIRTUAL_SERVER_SET_JK_LISTENER_NAME,
                        new Object[]{vs.getID(), jkConnector.getName()});
            }
        }

        vs.setNetworkListenerNames(
                listenerNames.toArray(new String[listenerNames.size()]));
    }


    // ------------------------------------------------------ Public Methods

    /**
     * Create a virtual server/host.
     */
    public VirtualServer createHost(String vsID,
                                    com.sun.enterprise.config.serverbeans.VirtualServer vsBean,
                                    String docroot, MimeMap mimeMap) {

        // Initialize the docroot
        VirtualServer vs = (VirtualServer) _embedded.createHost(vsID,
                vsBean, docroot, vsBean.getLogFile(), mimeMap);
        vs.configureState();
        vs.configureRemoteAddressFilterValve();
        vs.configureRemoteHostFilterValve();
        vs.configureSingleSignOn(globalSSOEnabled, webContainerFeatureFactory, isSsoFailoverEnabled());
        vs.configureRedirect();
        vs.configureErrorPage();
        vs.configureErrorReportValve();
        vs.setServerContext(getServerContext());
        vs.setServerConfig(serverConfig);
        vs.setGrizzlyService(grizzlyService);
        vs.setWebContainer(this);

        return vs;
    }


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.
     *
     * @throws IllegalStateException if this component has not been started
     * @throws LifecycleException    if this component detects a fatal error
     *                               that needs to be reported
     */
    public void stop() throws LifecycleException {
        // Validate and update our current component state
        if (!_started) {
            String msg = rb.getString(WEB_CONTAINER_NOT_STARTED);
            throw new LifecycleException(msg);
        }

        _started = false;

        // stop the embedded container
        try {
            _embedded.stop();
        } catch (LifecycleException ex) {
            if (!ex.getMessage().contains("has not been started")) {
                throw ex;
            }
        }
    }


    // ------------------------------------------------------ Private Methods


    /**
     * Configures a default web module for each virtual server based on the
     * virtual server's docroot if a virtual server does not specify
     * any default-web-module, and none of its web modules are loaded at "/"
     * <p/>
     * Needed in postConstruct before Deployment.ALL_APPLICATIONS_PROCESSED
     * for "jsp from docroot before web container start" scenario
     */

    public void loadSystemDefaultWebModules() {

        WebModuleConfig wmInfo = null;
        String defaultPath = null;

        Container[] vsArray = getEngine().findChildren();
        for (Container aVsArray : vsArray) {
            if (aVsArray instanceof VirtualServer) {
                VirtualServer vs = (VirtualServer) aVsArray;
                /*
                * Let AdminConsoleAdapter handle any requests for
                * the root context of the '__asadmin' virtual-server, see
                * https://glassfish.dev.java.net/issues/show_bug.cgi?id=5664
                */
                if (org.glassfish.api.web.Constants.ADMIN_VS.equals(
                        vs.getName())) {
                    continue;
                }

                // Create default web module off of virtual
                // server's docroot if necessary
                wmInfo = vs.createSystemDefaultWebModuleIfNecessary(
                        habitat.<WebArchivist>getService(
                                WebArchivist.class));
                if (wmInfo != null) {
                    defaultPath = wmInfo.getContextPath();
                    loadStandaloneWebModule(vs, wmInfo);
                }
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO,
                            VIRTUAL_SERVER_LOADED_DEFAULT_WEB_MODULE,
                            new Object[]{vs.getName(), defaultPath});
                }

            }
        }

    }


    /**
     * Configures a default web module for each virtual server
     * if default-web-module is defined.
     */
    public void loadDefaultWebModulesAfterAllAppsProcessed() {

        String defaultPath = null;

        Container[] vsArray = getEngine().findChildren();
        for (Container aVsArray : vsArray) {
            if (aVsArray instanceof VirtualServer) {
                VirtualServer vs = (VirtualServer) aVsArray;
                /*
                * Let AdminConsoleAdapter handle any requests for
                * the root context of the '__asadmin' virtual-server, see
                * https://glassfish.dev.java.net/issues/show_bug.cgi?id=5664
                */
                if (org.glassfish.api.web.Constants.ADMIN_VS.equals(
                        vs.getName())) {
                    continue;
                }
                WebModuleConfig wmInfo = vs.getDefaultWebModule(domain,
                        habitat.<WebArchivist>getService(
                                WebArchivist.class),
                        appRegistry);
                if (wmInfo != null) {
                    defaultPath = wmInfo.getContextPath();
                    // Virtual server declares default-web-module
                    try {
                        updateDefaultWebModule(vs, vs.getNetworkListenerNames(), wmInfo);
                    } catch (LifecycleException le) {
                        String msg = rb.getString(DEFAULT_WEB_MODULE_ERROR);
                        msg = MessageFormat.format(msg, defaultPath,
                                vs.getName());
                        logger.log(Level.SEVERE, msg, le);
                    }
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, VIRTUAL_SERVER_LOADED_DEFAULT_WEB_MODULE,
                                new Object[]{vs.getName(), defaultPath});
                    }

                } else {
                    // No need to create default web module off of virtual
                    // server's docroot since system web modules are already
                    // created in WebContainer.postConstruct
                }
            }
        }
    }


    /**
     * Load a default-web-module on the specified virtual server.
     */
    public void loadDefaultWebModule(
            com.sun.enterprise.config.serverbeans.VirtualServer vsBean) {

        VirtualServer virtualServer = (VirtualServer)
                getEngine().findChild(vsBean.getId());

        if (virtualServer != null) {
            loadDefaultWebModule(virtualServer);
        }
    }


    /**
     * Load a default-web-module on the specified virtual server.
     */
    public void loadDefaultWebModule(VirtualServer vs) {

        String defaultPath = null;
        WebModuleConfig wmInfo = vs.getDefaultWebModule(domain,
                habitat.<WebArchivist>getService(
                        WebArchivist.class),
                appRegistry);
        if (wmInfo != null) {
            defaultPath = wmInfo.getContextPath();
            // Virtual server declares default-web-module
            try {
                updateDefaultWebModule(vs, vs.getNetworkListenerNames(), wmInfo);
            } catch (LifecycleException le) {
                String msg = rb.getString(DEFAULT_WEB_MODULE_ERROR);
                msg = MessageFormat.format(msg, defaultPath, vs.getName());
                logger.log(Level.SEVERE, msg, le);
            }

        } else {
            // Create default web module off of virtual
            // server's docroot if necessary
            wmInfo = vs.createSystemDefaultWebModuleIfNecessary(
                    habitat.<WebArchivist>getService(
                            WebArchivist.class));
            if (wmInfo != null) {
                defaultPath = wmInfo.getContextPath();
                loadStandaloneWebModule(vs, wmInfo);
            }
        }

        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, VIRTUAL_SERVER_LOADED_DEFAULT_WEB_MODULE,
                    new Object[]{vs.getName(), defaultPath});
        }
    }


    /**
     * Load the specified web module as a standalone module on the specified
     * virtual server.
     */
    protected void loadStandaloneWebModule(VirtualServer vs,
                                           WebModuleConfig wmInfo) {
        try {
            loadWebModule(vs, wmInfo, "null", null);
        } catch (Throwable t) {
            String msg = rb.getString(LOAD_WEB_MODULE_ERROR);
            msg = MessageFormat.format(msg, wmInfo.getName());
            logger.log(Level.SEVERE, msg, t);
        }
    }


    /**
     * Whether or not a component (either an application or a module) should be
     * enabled is defined by the "enable" attribute on both the
     * application/module element and the application-ref element.
     *
     * @param moduleName The name of the component (application or module)
     * @return boolean
     */
    protected boolean isEnabled(String moduleName) {
        // TODO dochez : optimize
        /*
        Domain domain = habitat.getService(Domain.class);
        applications = domain.getApplications().getLifecycleModuleOrJ2EeApplicationOrEjbModuleOrWebModuleOrConnectorModuleOrAppclientModuleOrMbeanOrExtensionModule();
        com.sun.enterprise.config.serverbeans.WebModule webModule = null;
        for (Object module : applications) {
            if (module instanceof WebModule) {
                if (moduleName.equals(((com.sun.enterprise.config.serverbeans.WebModule) module).getName())) {
                    webModule = (com.sun.enterprise.config.serverbeans.WebModule) module;
                }
            }
        }    em
        ServerContext env = habitat.getService(ServerContext.class);
        List<Server> servers = domain.getServers().getServer();
        Server thisServer = null;
        for (Server server : servers) {
            if (env.getInstanceName().equals(server.getName())) {
                thisServer = server;
            }
        }
        List<ApplicationRef> appRefs = thisServer.getApplicationRef();
        ApplicationRef appRef = null;
        for (ApplicationRef ar : appRefs) {
            if (ar.getRef().equals(moduleName)) {
                appRef = ar;
            }
        }

        return ((webModule != null && Boolean.valueOf(webModule.getEnabled())) &&
                (appRef != null && Boolean.valueOf(appRef.getEnabled())));
         */
        return true;
    }

    /**
     * Creates and configures a web module for each virtual server
     * that the web module is hosted under.
     * <p/>
     * If no virtual servers have been specified, then the web module will
     * not be loaded.
     */
    public List<Result<WebModule>> loadWebModule(
            WebModuleConfig wmInfo, String j2eeApplication,
            Properties deploymentProperties) {
        List<Result<WebModule>> results = new ArrayList<Result<WebModule>>();
        String vsIDs = wmInfo.getVirtualServers();
        List<String> vsList = StringUtils.parseStringList(vsIDs, " ,");
        if (vsList == null || vsList.isEmpty()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO,
                        WEB_MODULE_NOT_LOADED_NO_VIRTUAL_SERVERS,
                        wmInfo.getName());
            }
            return results;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LOADING_WEB_MODULE, vsIDs);
        }

        List<String> nonProcessedVSList = new ArrayList<String>(vsList);
        Container[] vsArray = getEngine().findChildren();
        for (Container aVsArray : vsArray) {
            if (aVsArray instanceof VirtualServer) {
                VirtualServer vs = (VirtualServer) aVsArray;
                boolean eqVS = vsList.contains(vs.getID());
                if (eqVS) {
                    nonProcessedVSList.remove(vs.getID());
                }
                Set<String> matchedAliases = matchAlias(vsList, vs);
                boolean hasMatchedAlias = (matchedAliases.size() > 0);
                if (hasMatchedAlias) {
                    nonProcessedVSList.removeAll(matchedAliases);
                }
                if (eqVS || hasMatchedAlias) {
                    WebModule ctx = null;
                    try {
                        ctx = loadWebModule(vs, wmInfo, j2eeApplication,
                                deploymentProperties);
                        results.add(new Result<WebModule>(ctx));
                    } catch (Throwable t) {
                        if (ctx != null) {
                            ctx.setAvailable(false);
                        }
                        results.add(new Result<WebModule>(t));
                    }
                }
            }
        }

        if (nonProcessedVSList.size() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean follow = false;
            for (String alias : nonProcessedVSList) {
                 if (follow) {
                     sb.append(",");
                 }
                 sb.append(alias);
                 follow = true;
            }
            Object[] params = {wmInfo.getName(), sb.toString()};
            logger.log(Level.SEVERE, WEB_MODULE_NOT_LOADED_TO_VS,
                            params);
        }
        return results;
    }


    /**
     * Deploy on aliases as well as host.
     */
    private boolean verifyAlias(List<String> vsList, VirtualServer vs) {
        for (int i = 0; i < vs.getAliases().length; i++) {
            if (vsList.contains(vs.getAliases()[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find all matched aliases.
     * This is more expensive than verifyAlias.
     */
    private Set<String> matchAlias(List<String> vsList, VirtualServer vs) {
        Set<String> matched = new HashSet<String>();
        for (String alias : vs.getAliases()) {
            if (vsList.contains(alias)) {
                matched.add(alias);
            }
        }

        return matched;
    }


    /**
     * Creates and configures a web module and adds it to the specified
     * virtual server.
     */
    private WebModule loadWebModule(
            VirtualServer vs,
            WebModuleConfig wmInfo,
            String j2eeApplication,
            Properties deploymentProperties)
            throws Exception {

        String wmName = wmInfo.getName();
        String wmContextPath = wmInfo.getContextPath();

        if (wmContextPath.indexOf('%') != -1) {
            try {
                RequestUtil.urlDecode(wmContextPath, "UTF-8");
            } catch (Exception e) {
                String msg = rb.getString(INVALID_ENCODED_CONTEXT_ROOT);
                msg = MessageFormat.format(msg, wmName, wmContextPath);
                throw new Exception(msg);
            }
        }

        if (wmContextPath.length() == 0 &&
                vs.getDefaultWebModuleID() != null) {
            String msg = rb.getString(DEFAULT_WEB_MODULE_CONFLICT);
            msg = MessageFormat.format(msg,
                    new Object[]{wmName, vs.getID()});
            throw new Exception(msg);
        }

        wmInfo.setWorkDirBase(_appsWorkRoot);
        // START S1AS 6178005
        wmInfo.setStubBaseDir(appsStubRoot);
        // END S1AS 6178005

        String displayContextPath = null;
        if (wmContextPath.length() == 0)
            displayContextPath = "/";
        else
            displayContextPath = wmContextPath;

        Map<String, AdHocServletInfo> adHocPaths = null;
        Map<String, AdHocServletInfo> adHocSubtrees = null;
        WebModule ctx = (WebModule) vs.findChild(wmContextPath);
        if (ctx != null) {
            if (ctx instanceof AdHocWebModule) {
                /*
                 * Found ad-hoc web module which has been created by web
                 * container in order to store mappings for ad-hoc paths
                 * and subtrees.
                 * All these mappings must be propagated to the context
                 * that is being deployed.
                 */
                if (ctx.hasAdHocPaths()) {
                    adHocPaths = ctx.getAdHocPaths();
                }
                if (ctx.hasAdHocSubtrees()) {
                    adHocSubtrees = ctx.getAdHocSubtrees();
                }
                vs.removeChild(ctx);
            } else if (Constants.DEFAULT_WEB_MODULE_NAME
                    .equals(ctx.getModuleName())) {
                /*
                 * Dummy context that was created just off of a docroot,
                 * (see
                 * VirtualServer.createSystemDefaultWebModuleIfNecessary()).
                 * Unload it so it can be replaced with the web module to be
                 * loaded
                 */
                unloadWebModule(wmContextPath,
                        ctx.getWebBundleDescriptor().getApplication().getRegistrationName(),
                        vs.getName(),
                        true,
                        null);
            } else if (!ctx.getAvailable()) {
                /*
                 * Context has been marked unavailable by a previous
                 * call to disableWebModule. Mark the context as available and
                 * return
                 */
                ctx.setAvailable(true);
                return ctx;
            } else {
                String msg = rb.getString(DUPLICATE_CONTEXT_ROOT);
                throw new Exception(MessageFormat.format(msg, vs.getID(),
                        ctx.getModuleName(), displayContextPath, wmName));
            }
        }

        if (logger.isLoggable(Level.FINEST)) {
            Object[] params = {wmName, vs.getID(), displayContextPath};
            logger.log(Level.FINEST, WEB_MODULE_LOADING, params);
        }

        File docBase = null;
        if (JWS_APPCLIENT_MODULE_NAME.equals(wmName)) {
            docBase = new File(System.getProperty("com.sun.aas.installRoot"));
        } else {
            docBase = wmInfo.getLocation();
        }

        ctx = (WebModule) _embedded.createContext(
                wmName,
                wmContextPath,
                docBase,
                vs.getDefaultContextXmlLocation(),
                vs.getDefaultWebXmlLocation(),
                useDOLforDeployment,
                wmInfo);

        // for now disable JNDI
        ctx.setUseNaming(false);

        // Set JSR 77 object name and attributes
        Engine engine = (Engine) vs.getParent();
        if (engine != null) {
            ctx.setEngineName(engine.getName());
            ctx.setJvmRoute(engine.getJvmRoute());
        }
        String j2eeServer = _serverContext.getInstanceName();
        String domain = _serverContext.getDefaultDomainName();
        // String[] javaVMs = J2EEModuleUtil.getjavaVMs();
        ctx.setDomain(domain);

        ctx.setJ2EEServer(j2eeServer);
        ctx.setJ2EEApplication(j2eeApplication);
        //turn on container internal cache by default as in v2
        //ctx.setCachingAllowed(false);
        ctx.setCacheControls(vs.getCacheControls());
        ctx.setBean(wmInfo.getBean());

        if (adHocPaths != null) {
            ctx.addAdHocPaths(adHocPaths);
        }
        if (adHocSubtrees != null) {
            ctx.addAdHocSubtrees(adHocSubtrees);
        }

        // Object containing web.xml information
        WebBundleDescriptor wbd = wmInfo.getDescriptor();

        // Set the context root
        if (wbd != null) {
            ctx.setContextRoot(wbd.getContextRoot());
        } else {
            // Should never happen.
            logger.log(Level.WARNING, UNABLE_TO_SET_CONTEXT_ROOT, wmInfo);
        }

        //
        // Ensure that the generated directory for JSPs in the document root
        // (i.e. those that are serviced by a system default-web-module)
        // is different for each virtual server.
        String wmInfoWorkDir = wmInfo.getWorkDir();
        if (wmInfoWorkDir != null) {
            StringBuilder workDir = new StringBuilder(wmInfo.getWorkDir());
            if (wmName.equals(Constants.DEFAULT_WEB_MODULE_NAME)) {
                workDir.append("-");
                workDir.append(FileUtils.makeFriendlyFilename(vs.getID()));
            }
            ctx.setWorkDir(workDir.toString());
        }

        ClassLoader parentLoader = wmInfo.getParentLoader();
        if (parentLoader == null) {
            // Use the shared classloader as the parent for all
            // standalone web-modules
            parentLoader = _serverContext.getSharedClassLoader();
        }
        ctx.setParentClassLoader(parentLoader);

        if (wbd != null) {
            // Determine if an alternate DD is set for this web-module in
            // the application
            ctx.configureAlternateDD(wbd);
            ctx.configureWebServices(wbd);
        }

        // Object containing sun-web.xml information
        SunWebAppImpl iasBean = null;

        // The default context is the only case when wbd == null
        if (wbd != null) {
            iasBean = (SunWebAppImpl) wbd.getSunDescriptor();
        }

        // set the sun-web config bean
        ctx.setIasWebAppConfigBean(iasBean);

        // Configure SingleThreadedServletPools, work/tmp directory etc
        ctx.configureMiscSettings(iasBean, vs, displayContextPath);

        // Configure alternate docroots if dummy web module
        if (ctx.getID().startsWith(Constants.DEFAULT_WEB_MODULE_NAME)) {
            ctx.setAlternateDocBases(vs.getProperties());
        }

        // Configure the class loader delegation model, classpath etc
        Loader loader = ctx.configureLoader(iasBean);

        // Set the class loader on the DOL object
        if (wbd != null && wbd.hasWebServices()) {
            wbd.addExtraAttribute("WEBLOADER", loader);
        }

        for (LifecycleListener listener : ctx.findLifecycleListeners()) {
            if (listener instanceof ContextConfig) {
                ((ContextConfig) listener).setClassLoader(wmInfo.getAppClassLoader());
            }
        }

        // Configure the session manager and other related settings
        ctx.configureSessionSettings(wbd, wmInfo);

        // set i18n info from locale-charset-info tag in sun-web.xml
        ctx.setI18nInfo();

        if (wbd != null) {
            String resourceType = wmInfo.getObjectType();
            boolean isSystem = resourceType != null &&
                    resourceType.startsWith("system-");
            // security will generate policy for system default web module
            if (!wmName.startsWith(Constants.DEFAULT_WEB_MODULE_NAME)) {
                // TODO : v3 : dochez Need to remove dependency on security
                Realm realm = habitat.getService(Realm.class);
                if ("null".equals(j2eeApplication)) {
                    /*
                     * Standalone webapps inherit the realm referenced by
                     * the virtual server on which they are being deployed,
                     * unless they specify their own
                     */
                    if (realm != null && realm instanceof RealmInitializer) {
                        ((RealmInitializer) realm).initializeRealm(
                                wbd, isSystem, vs.getAuthRealmName());
                        ctx.setRealm(realm);
                    }
                } else {
                    if (realm != null && realm instanceof RealmInitializer) {
                        ((RealmInitializer) realm).initializeRealm(
                                wbd, isSystem, null);
                        ctx.setRealm(realm);
                    }
                }
            }

            // post processing DOL object for standalone web module
            if (wbd.getApplication() != null &&
                    wbd.getApplication().isVirtual()) {
                wbd.visit(new WebValidatorWithoutCL());
            }
        }

        // Add virtual server mime mappings, if present
        addMimeMappings(ctx, vs.getMimeMap());

        String moduleName = Constants.DEFAULT_WEB_MODULE_NAME;
        String monitoringNodeName = moduleName;
        if (wbd != null && wbd.getApplication() != null) {
            // Not a dummy web module
            com.sun.enterprise.deployment.Application app =
                    wbd.getApplication();
            ctx.setStandalone(app.isVirtual());
            // S1AS BEGIN WORKAROUND FOR 6174360
            if (app.isVirtual()) {
                // Standalone web module
                moduleName = app.getRegistrationName();
                monitoringNodeName = wbd.getModuleID();
            } else {
                // Nested (inside EAR) web module
                moduleName = wbd.getModuleDescriptor().getArchiveUri();
                StringBuilder sb = new StringBuilder();
                sb.append(app.getRegistrationName()).
                        append(MONITORING_NODE_SEPARATOR).append(moduleName);
                monitoringNodeName = sb.toString().replaceAll("\\.", "\\\\.").
                        replaceAll("_war", "\\\\.war");
            }
            // S1AS END WORKAROUND FOR 6174360
        }
        ctx.setModuleName(moduleName);
        ctx.setMonitoringNodeName(monitoringNodeName);

        List<String> servletNames = new ArrayList<String>();
        if (wbd != null) {
            for (WebComponentDescriptor webCompDesc : wbd.getWebComponentDescriptors()) {
                if (webCompDesc.isServlet()) {
                    servletNames.add(webCompDesc.getCanonicalName());
                }
            }
        }

        webStatsProviderBootstrap.registerApplicationStatsProviders(monitoringNodeName,
                vs.getName(), servletNames);

        vs.addChild(ctx);

        ctx.loadSessions(deploymentProperties);

        return ctx;
    }


    /*
     * Updates the given virtual server with the given default path.
     *
     * The given default path corresponds to the context path of one of the
     * web contexts deployed on the virtual server that has been designated
     * as the virtual server's new default-web-module.
     *
     * @param virtualServer The virtual server to update
     * @param ports The port numbers of the HTTP listeners with which the
     * given virtual server is associated
     * @param defaultContextPath The context path of the web module that has
     * been designated as the virtual server's new default web module, or null
     * if the virtual server no longer has any default-web-module
     */

    protected void updateDefaultWebModule(VirtualServer virtualServer,
                                          String[] listenerNames,
                                          WebModuleConfig wmInfo)
            throws LifecycleException {

        String defaultContextPath = null;
        if (wmInfo != null) {
            defaultContextPath = wmInfo.getContextPath();
        }
        if (defaultContextPath != null
                && !defaultContextPath.startsWith("/")) {
            defaultContextPath = "/" + defaultContextPath;
            wmInfo.getDescriptor().setContextRoot(defaultContextPath);
        }

        Connector[] connectors = _embedded.findConnectors();
        for (Connector connector : connectors) {
            PECoyoteConnector conn = (PECoyoteConnector) connector;
            String name = conn.getName();
            for (String listenerName : listenerNames) {
                if (name.equals(listenerName)) {
                    Mapper mapper = conn.getMapper();
                    try {
                        mapper.setDefaultContextPath(virtualServer.getName(),
                                defaultContextPath);
                        for (String alias : virtualServer.findAliases()) {
                            mapper.setDefaultContextPath(alias,
                                    defaultContextPath);
                        }
                        virtualServer.setDefaultContextPath(defaultContextPath);
                    } catch (Exception e) {
                        throw new LifecycleException(e);
                    }
                }
            }
        }
    }


    /**
     * Utility Method to access the ServerContext
     */
    public ServerContext getServerContext() {
        return _serverContext;
    }


    ServerConfigLookup getServerConfigLookup() {
        return serverConfigLookup;
    }


    File getLibPath() {
        return instance.getLibPath();
    }


    /**
     * The application id for this web module
     * HERCULES:add
     */
    public String getApplicationId(WebModule wm) {
        return wm.getID();
    }


    /**
     * Return the Absolute path for location where all the deployed
     * standalone modules are stored for this Server Instance.
     */
    public File getModulesRoot() {
        return _modulesRoot;
    }

    /**
     * Undeploy a web application.
     *
     * @param contextRoot    the context's name to undeploy
     * @param appName        the J2EE appname used at deployment time
     * @param virtualServers List of current virtual-server object.
     */
    public void unloadWebModule(String contextRoot,
                                String appName,
                                String virtualServers,
                                Properties props) {
        unloadWebModule(contextRoot, appName, virtualServers, false, props);
    }

    /**
     * Undeploy a web application.
     *
     * @param contextRoot    the context's name to undeploy
     * @param appName        the J2EE appname used at deployment time
     * @param virtualServers List of current virtual-server object.
     * @param dummy          true if the web module to be undeployed is a dummy web
     *                       module, that is, a web module created off of a virtual server's
     *                       docroot
     */
    public void unloadWebModule(String contextRoot,
                                String appName,
                                String virtualServers,
                                boolean dummy,
                                Properties props) {

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, UNLOADING_WEB_MODULE, new Object[]{contextRoot, virtualServers});
        }

        // tomcat contextRoot starts with "/"
        if (contextRoot.length() != 0 && !contextRoot.startsWith("/")) {
            contextRoot = "/" + contextRoot;
        } else if ("/".equals(contextRoot)) {
            // Make corresponding change as in WebModuleConfig.getContextPath()
            contextRoot = "";
        }

        List<String> hostList = StringUtils.parseStringList(virtualServers, " ,");
        boolean unloadFromAll = hostList == null || hostList.isEmpty();
        boolean hasBeenUndeployed = false;
        VirtualServer host = null;
        WebModule context = null;
        Container[] hostArray = getEngine().findChildren();
        for (Container aHostArray : hostArray) {
            host = (VirtualServer) aHostArray;
            if (unloadFromAll || hostList.contains(host.getName())
                    || verifyAlias(hostList, host)) {
                context = (WebModule) host.findChild(contextRoot);
                if (context != null &&
                        context.getWebBundleDescriptor().getApplication().getRegistrationName().equals(appName)) {
                    context.saveSessions(props);
                    host.removeChild(context);

                    webStatsProviderBootstrap.unregisterApplicationStatsProviders(
                            context.getMonitoringNodeName(), host.getName());

                    try {
                        /*
                         * If the webapp is being undeployed as part of a
                         * domain shutdown, we don't want to destroy it,
                         * as that would remove any sessions persisted to
                         * file. Any active sessions need to survive the
                         * domain shutdown, so that they may be resumed
                         * after the domain has been restarted.
                         */
                        if (!isShutdown) {
                            context.destroy();
                        }
                    } catch (Exception ex) {
                        String msg = rb.getString(EXCEPTION_DURING_DESTROY);
                        msg = MessageFormat.format(msg, contextRoot,
                                host.getName());
                        logger.log(Level.WARNING, msg, ex);
                    }
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, CONTEXT_UNDEPLOYED, new Object[]{contextRoot, host});
                    }
                    hasBeenUndeployed = true;
                    host.fireContainerEvent(Deployer.REMOVE_EVENT, context);
                    /*
                     * If the web module that has been unloaded
                     * contained any mappings for ad-hoc paths,
                     * those mappings must be preserved by registering an
                     * ad-hoc web module at the same context root
                     */
                    if (context.hasAdHocPaths()
                            || context.hasAdHocSubtrees()) {
                        WebModule wm = createAdHocWebModule(
                                context.getID(),
                                host,
                                contextRoot,
                                context.getJ2EEApplication());
                        wm.addAdHocPaths(context.getAdHocPaths());
                        wm.addAdHocSubtrees(context.getAdHocSubtrees());
                    }
                    // START GlassFish 141
                    if (!dummy && !isShutdown) {
                        WebModuleConfig wmInfo =
                                host.createSystemDefaultWebModuleIfNecessary(
                                        habitat.<WebArchivist>getService(
                                                WebArchivist.class));
                        if (wmInfo != null) {
                            loadStandaloneWebModule(host, wmInfo);
                        }
                    }
                    // END GlassFish 141
                }
            }
        }

        if (!hasBeenUndeployed) {
            logger.log(Level.SEVERE, UNDEPLOY_ERROR, contextRoot);
        }
    }


    /**
     * Suspends the web application with the given appName that has been
     * deployed at the given contextRoot on the given virtual servers.
     *
     * @param contextRoot the context root
     * @param appName     the J2EE appname used at deployment time
     * @param hosts       the list of virtual servers
     */
    public boolean suspendWebModule(String contextRoot,
                                    String appName,
                                    String hosts) {
        boolean hasBeenSuspended = false;
        List<String> hostList = StringUtils.parseStringList(hosts, " ,");
        if (hostList == null || hostList.isEmpty()) {
            return hasBeenSuspended;
        }

        // tomcat contextRoot starts with "/"
        if (contextRoot.length() != 0 && !contextRoot.startsWith("/")) {
            contextRoot = "/" + contextRoot;
        }
        VirtualServer host = null;
        Context context = null;
        for (Container aHostArray : getEngine().findChildren()) {
            host = (VirtualServer) aHostArray;
            if (hostList.contains(host.getName()) ||
                    verifyAlias(hostList, host)) {
                context = (Context) host.findChild(contextRoot);
                if (context != null) {
                    context.setAvailable(false);
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, CONTEXT_DISABLED, new Object[]{contextRoot, host});
                    }
                    hasBeenSuspended = true;
                }
            }
        }

        if (!hasBeenSuspended) {
            logger.log(Level.WARNING, DISABLE_WEB_MODULE_ERROR, contextRoot);
        }

        return hasBeenSuspended;
    }


    /**
     * Save the server-wide dynamic reloading settings for use when
     * configuring each web module.
     */
    private void configureDynamicReloadingSettings() {
        if (dasConfig != null) {
            _reloadingEnabled = Boolean.parseBoolean(dasConfig.getDynamicReloadEnabled());
            String seconds = dasConfig.getDynamicReloadPollIntervalInSeconds();
            if (seconds != null) {
                try {
                    _pollInterval = Integer.parseInt(seconds);
                } catch (NumberFormatException e) {
                }
            }
        }
    }


    /**
     * Sets the debug level for Catalina's containers based on the logger's
     * log level.
     */
    private void setDebugLevel() {
        Level logLevel = logger.getLevel() != null ?
                logger.getLevel() : Level.INFO;
        if (logLevel.equals(Level.FINE))
            _debug = 1;
        else if (logLevel.equals(Level.FINER))
            _debug = 2;
        else if (logLevel.equals(Level.FINEST))
            _debug = 5;
        else
            _debug = 0;
    }


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return new LifecycleListener[0];
    }


    /**
     * Gets all the virtual servers whose http-listeners attribute value
     * contains the given http-listener id.
     */
    List<VirtualServer> getVirtualServersForHttpListenerId(
            HttpService httpService, String httpListenerId) {

        if (httpListenerId == null) {
            return null;
        }

        List<VirtualServer> result = new ArrayList<VirtualServer>();

        for (com.sun.enterprise.config.serverbeans.VirtualServer vs : httpService.getVirtualServer()) {
            List<String> listeners = StringUtils.parseStringList(vs.getNetworkListeners(), ",");
            if (listeners != null) {
                ListIterator<String> iter = listeners.listIterator();
                while (iter.hasNext()) {
                    if (httpListenerId.equals(iter.next())) {
                        VirtualServer match = (VirtualServer)
                                getEngine().findChild(vs.getId());
                        if (match != null) {
                            result.add(match);
                        }
                        break;
                    }
                }
            }
        }

        return result;
    }


    /**
     * Adds the given mime mappings to those of the specified context, unless
     * they're already present in the context (that is, the mime mappings of
     * the specified context, which correspond to those in default-web.xml,
     * can't be overridden).
     *
     * @param ctx     The StandardContext to whose mime mappings to add
     * @param mimeMap The mime mappings to be added
     */
    private void addMimeMappings(StandardContext ctx, MimeMap mimeMap) {
        if (mimeMap == null) {
            return;
        }

        for (Iterator<String> itr = mimeMap.getExtensions(); itr.hasNext();) {
            String extension = itr.next();
            if (ctx.findMimeMapping(extension) == null) {
                ctx.addMimeMapping(extension, mimeMap.getType(extension));
            }
        }
    }

    /**
     * Return the parent/top-level container in _embedded for virtual
     * servers.
     */
    public Engine getEngine() {
        return _embedded.getEngines()[0];
    }

    public HttpService getHttpService() {
        return serverConfig.getHttpService();
    }


    /**
     * Registers the given ad-hoc path at the given context root.
     *
     * @param path        The ad-hoc path to register
     * @param ctxtRoot    The context root at which to register
     * @param appName     The name of the application with which the ad-hoc path is
     *                    associated
     * @param servletInfo Info about the ad-hoc servlet that will service
     *                    requests on the given path
     */
    public void registerAdHocPath(
            String path,
            String ctxtRoot,
            String appName,
            AdHocServletInfo servletInfo) {

        registerAdHocPathAndSubtree(path, null, ctxtRoot, appName,
                servletInfo);
    }


    /**
     * Registers the given ad-hoc path and subtree at the given context root.
     *
     * @param path        The ad-hoc path to register
     * @param subtree     The ad-hoc subtree path to register
     * @param ctxtRoot    The context root at which to register
     * @param appName     The name of the application with which the ad-hoc path
     *                    and subtree are associated
     * @param servletInfo Info about the ad-hoc servlet that will service
     *                    requests on the given ad-hoc path and subtree
     */
    public void registerAdHocPathAndSubtree(
            String path,
            String subtree,
            String ctxtRoot,
            String appName,
            AdHocServletInfo servletInfo) {

        WebModule wm = null;

        Container[] vsList = getEngine().findChildren();
        for (Container aVsList : vsList) {
            VirtualServer vs = (VirtualServer) aVsList;
            if (vs.getName().equalsIgnoreCase(
                    org.glassfish.api.web.Constants.ADMIN_VS)) {
                // Do not deploy on admin vs
                continue;
            }
            wm = (WebModule) vs.findChild(ctxtRoot);
            if (wm == null) {
                wm = createAdHocWebModule(vs, ctxtRoot, appName);
            }
            wm.addAdHocPathAndSubtree(path, subtree, servletInfo);
        }
    }


    /**
     * Unregisters the given ad-hoc path from the given context root.
     *
     * @param path     The ad-hoc path to unregister
     * @param ctxtRoot The context root from which to unregister
     */
    public void unregisterAdHocPath(String path, String ctxtRoot) {
        unregisterAdHocPathAndSubtree(path, null, ctxtRoot);
    }


    /**
     * Unregisters the given ad-hoc path and subtree from the given context
     * root.
     *
     * @param path     The ad-hoc path to unregister
     * @param subtree  The ad-hoc subtree to unregister
     * @param ctxtRoot The context root from which to unregister
     */
    public void unregisterAdHocPathAndSubtree(String path,
                                              String subtree,
                                              String ctxtRoot) {

        WebModule wm = null;

        Container[] vsList = getEngine().findChildren();
        for (Container aVsList : vsList) {
            VirtualServer vs = (VirtualServer) aVsList;
            if (vs.getName().equalsIgnoreCase(
                    org.glassfish.api.web.Constants.ADMIN_VS)) {
                // Do not undeploy from admin vs, because we never
                // deployed onto it
                continue;
            }
            wm = (WebModule) vs.findChild(ctxtRoot);
            if (wm == null) {
                continue;
            }
            /*
            * If the web module was created by the container for the
            * sole purpose of mapping ad-hoc paths and subtrees,
            * and does no longer contain any ad-hoc paths or subtrees,
            * remove the web module.
            */
            wm.removeAdHocPath(path);
            wm.removeAdHocSubtree(subtree);
            if (wm instanceof AdHocWebModule && !wm.hasAdHocPaths()
                    && !wm.hasAdHocSubtrees()) {
                vs.removeChild(wm);
                try {
                    wm.destroy();
                } catch (Exception ex) {
                    String msg = rb.getString(EXCEPTION_DURING_DESTROY);
                    msg = MessageFormat.format(msg, wm.getPath(), vs.getName());
                    logger.log(Level.WARNING, msg, ex);
                }
            }
        }
    }

    /*
     * Creates an ad-hoc web module and registers it on the given virtual
     * server at the given context root.
     *
     * @param vs The virtual server on which to add the ad-hoc web module
     * @param ctxtRoot The context root at which to register the ad-hoc
     * web module
     * @param appName The name of the application to which the ad-hoc module
     * being generated belongs
     *
     * @return The newly created ad-hoc web module
     */

    private WebModule createAdHocWebModule(
            VirtualServer vs,
            String ctxtRoot,
            String appName) {
        return createAdHocWebModule(appName, vs, ctxtRoot, appName);
    }

    /*
     * Creates an ad-hoc web module and registers it on the given virtual
     * server at the given context root.
     *
     * @param id the id of the ad-hoc web module
     * @param vs The virtual server on which to add the ad-hoc web module
     * @param ctxtRoot The context root at which to register the ad-hoc
     * web module
     * @param appName The name of the application to which the ad-hoc module
     * being generated belongs
     *
     * @return The newly created ad-hoc web module
     */

    private WebModule createAdHocWebModule(
            String id,
            VirtualServer vs,
            String ctxtRoot,
            String j2eeApplication) {

        AdHocWebModule wm = new AdHocWebModule();
        wm.setID(id);
        wm.setWebContainer(this);

        wm.restrictedSetPipeline(new WebPipeline(wm));

        // The Parent ClassLoader of the AdhocWebModule was null
        // [System ClassLoader]. With the new hierarchy, the thread context
        // classloader needs to be set.
        //if (Boolean.getBoolean(com.sun.enterprise.server.PELaunch.USE_NEW_CLASSLOADER_PROPERTY)) {
        wm.setParentClassLoader(
                Thread.currentThread().getContextClassLoader());
        //}

        wm.setContextRoot(ctxtRoot);
        wm.setJ2EEApplication(j2eeApplication);
        wm.setName(ctxtRoot);
        wm.setDocBase(vs.getAppBase());
        wm.setEngineName(vs.getParent().getName());

        String domain = _serverContext.getDefaultDomainName();
        wm.setDomain(domain);

        String j2eeServer = _serverContext.getInstanceName();
        wm.setJ2EEServer(j2eeServer);

        wm.setCrossContext(true);
        //wm.setJavaVMs(J2EEModuleUtil.getjavaVMs());

        vs.addChild(wm);

        return wm;
    }


    /**
     * Removes the dummy module (the module created off of a virtual server's
     * docroot) from the given virtual server if such a module exists.
     *
     * @param vs The virtual server whose dummy module is to be removed
     */
    void removeDummyModule(VirtualServer vs) {
        WebModule ctx = (WebModule) vs.findChild("");
        if (ctx != null
                && Constants.DEFAULT_WEB_MODULE_NAME.equals(
                ctx.getModuleName())) {
            unloadWebModule("", ctx.getWebBundleDescriptor().getApplication().getRegistrationName(),
                    vs.getName(), true, null);
        }
    }


    /**
     * Initializes the instance-level session properties (read from
     * config.web-container.session-config.session-properties in domain.xml).
     */
    private void initInstanceSessionProperties() {

        SessionProperties spBean =
                serverConfigLookup.getInstanceSessionProperties();

        if (spBean == null || spBean.getProperty() == null) {
            return;
        }

        List<Property> props = spBean.getProperty();
        if (props == null) {
            return;
        }

        for (Property prop : props) {
            String propName = prop.getName();
            String propValue = prop.getValue();
            if (propName == null || propValue == null) {
                throw new IllegalArgumentException(
                        rb.getString(NULL_WEB_PROPERTY));
            }

            if (propName.equalsIgnoreCase("enableCookies")) {
                instanceEnableCookies = ConfigBeansUtilities.toBoolean(propValue);
            } else if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, PROPERTY_NOT_YET_SUPPORTED, propName);
            }
        }
    }

    private static synchronized void setJspFactory() {
        if (JspFactory.getDefaultFactory() == null) {
            JspFactory.setDefaultFactory(new JspFactoryImpl());
        }
    }


    /**
     * Delete virtual-server.
     *
     * @param httpService element which contains the configuration info.
     */
    public void deleteHost(HttpService httpService) throws LifecycleException {

        VirtualServer virtualServer;

        // First we need to find which virtual-server was deleted. In
        // reconfig/VirtualServerReconfig, it is impossible to lookup
        // the vsBean because the element is removed from domain.xml
        // before handleDelete is invoked.
        Container[] virtualServers = getEngine().findChildren();
        for (int i = 0; i < virtualServers.length; i++) {
            for (com.sun.enterprise.config.serverbeans.VirtualServer vse : httpService.getVirtualServer()) {
                if (virtualServers[i].getName().equals(vse.getId())) {
                    virtualServers[i] = null;
                    break;
                }
            }
        }
        for (Container virtualServer1 : virtualServers) {
            virtualServer = (VirtualServer) virtualServer1;
            if (virtualServer != null) {
                if (virtualServer.getID().equals(
                        org.glassfish.api.web.Constants.ADMIN_VS)) {
                    throw new LifecycleException(
                            "Cannot delete admin virtual-server.");
                }
                Container[] webModules = virtualServer.findChildren();
                for (Container webModule : webModules) {
                    String appName = webModule.getName();
                    if (webModule instanceof WebModule) {
                        appName = ((WebModule)webModule).getWebBundleDescriptor().getApplication().getRegistrationName();
                    }
                    unloadWebModule(webModule.getName(),
                            appName,
                            virtualServer.getID(),
                            null);
                }
                try {
                    virtualServer.destroy();
                } catch (Exception e) {
                    String msg = rb.getString(DESTROY_VS_ERROR);
                    msg = MessageFormat.format(msg, virtualServer.getID());
                    logger.log(Level.WARNING, msg, e);
                }
            }
        }
    }


    /**
     * Updates a virtual-server element.
     *
     * @param vsBean the virtual-server config bean.
     */
    public void updateHost(com.sun.enterprise.config.serverbeans.VirtualServer vsBean) throws LifecycleException {

        if (org.glassfish.api.web.Constants.ADMIN_VS.equals(vsBean.getId())) {
            return;
        }
        final VirtualServer vs = (VirtualServer) getEngine().findChild(vsBean.getId());

        if (vs == null) {
            logger.log(Level.WARNING, CANNOT_UPDATE_NON_EXISTENCE_VS, vsBean.getId());
            return;
        }

        boolean updateListeners = false;

        // Only update connectors if virtual-server.http-listeners is changed dynamically
        if (vs.getNetworkListeners() == null) {
            if (vsBean.getNetworkListeners() == null) {
                updateListeners = false;
            } else {
                updateListeners = true;
            }
        } else if (vs.getNetworkListeners().equals(vsBean.getNetworkListeners())) {
            updateListeners = false;
        } else {
            List<String> vsList = StringUtils.parseStringList(
                vs.getNetworkListeners(), ",");
            List<String> vsBeanList = StringUtils.parseStringList(
                vsBean.getNetworkListeners(), ",");
            for (String vsBeanName : vsBeanList) {
                if (!vsList.contains(vsBeanName)) {
                    updateListeners = true;
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, UPDATE_LISTENER, new Object[]{vsBeanName, vs.getNetworkListeners()});
                    }
                    break;
                }
            }
        }

        // Must retrieve the old default-web-module before updating the
        // virtual server with the new vsBean, because default-web-module is
        // read from vsBean
        String oldDefaultWebModule = vs.getDefaultWebModuleID();

        vs.setBean(vsBean);

        String vsLogFile = vsBean.getLogFile();
        vs.setLogFile(vsLogFile, logLevel, logServiceFile);

        vs.configureState();

        vs.clearAliases();
        vs.configureAliases();

        // support both docroot property and attribute
        String docroot = vsBean.getPropertyValue("docroot");
        if (docroot == null) {
            docroot = vsBean.getDocroot();
        }
        if (docroot != null) {
            // Only update docroot if it is modified
            if (!vs.getDocRoot().getAbsolutePath().equals(docroot)) {
                updateDocroot(docroot, vs, vsBean);
            }
        }

        List<Property> props = vs.getProperties();
        for (Property prop : props) {
            updateHostProperties(vsBean, prop.getName(), prop.getValue(), securityService, vs);
        }
        vs.configureSingleSignOn(globalSSOEnabled, webContainerFeatureFactory, isSsoFailoverEnabled());
        vs.reconfigureAccessLog(globalAccessLogBufferSize, globalAccessLogWriteInterval, habitat, domain,
                globalAccessLoggingEnabled);

        // old listener names
        List<String> oldListenerList = StringUtils.parseStringList(
                vsBean.getNetworkListeners(), ",");
        String[] oldListeners = (oldListenerList != null) ?
                oldListenerList.toArray(new String[oldListenerList.size()]) :
                new String[0];
        // new listener config
        HashSet<NetworkListener> networkListeners = new HashSet<NetworkListener>();
        if (oldListenerList != null) {
            for (String listener : oldListeners) {
                boolean found = false;
                for (NetworkListener httpListener :
                        serverConfig.getNetworkConfig().getNetworkListeners().getNetworkListener()) {
                    if (httpListener.getName().equals(listener)) {
                        networkListeners.add(httpListener);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    String msg = rb.getString(LISTENER_REFERENCED_BY_HOST_NOT_EXIST);
                    msg = MessageFormat.format(msg, listener, vs.getName());
                    logger.log(Level.SEVERE, msg);
                }
            }
            // Update the port numbers with which the virtual server is
            // associated
            configureHostPortNumbers(vs, networkListeners);
        } else {
            // The virtual server is not associated with any http listeners
            vs.setNetworkListenerNames(new String[0]);
        }

        // Disassociate the virtual server from all http listeners that
        // have been removed from its http-listeners attribute
        for (String oldListener : oldListeners) {
            boolean found = false;
            for (NetworkListener httpListener : networkListeners) {
                if (httpListener.getName().equals(oldListener)) {
                    found = true;
                }
            }
            if (!found) {
                // http listener was removed
                Connector[] connectors = _embedded.findConnectors();
                for (Connector connector : connectors) {
                    WebConnector conn = (WebConnector) connector;
                    if (oldListener.equals(conn.getName())) {
                        try {
                            conn.getMapperListener().unregisterHost(
                                    vs.getJmxName());
                        } catch (Exception e) {
                            throw new LifecycleException(e);
                        }
                    }
                }

            }
        }

        // Associate the virtual server with all http listeners that
        // have been added to its http-listeners attribute
        for (NetworkListener httpListener : networkListeners) {
            boolean found = false;
            for (String oldListener : oldListeners) {
                if (httpListener.getName().equals(oldListener)) {
                    found = true;
                }
            }
            if (!found) {
                // http listener was added
                Connector[] connectors = _embedded.findConnectors();
                for (Connector connector : connectors) {
                    WebConnector conn = (WebConnector)
                            connector;
                    if (httpListener.getName().equals(conn.getName())) {
                        if (!conn.isAvailable()) {
                            conn.start();
                        }
                        try {
                            conn.getMapperListener().registerHost(vs);
                        } catch (Exception e) {
                            throw new LifecycleException(e);
                        }
                    }
                }
            }
        }

        // Remove the old default web module if one was configured, by
        // passing in "null" as the default context path
        if (oldDefaultWebModule != null) {
            updateDefaultWebModule(vs, oldListeners, null);
        }

        /*
         * Add default web module if one has been configured for the
         * virtual server. If the module declared as the default web module
         * has already been deployed at the root context, we don't have
         * to do anything.
         */
        WebModuleConfig wmInfo = vs.getDefaultWebModule(domain,
                habitat.<WebArchivist>getService(WebArchivist.class),
                appRegistry);
        if ((wmInfo != null) && (wmInfo.getContextPath() != null) &&
                !"".equals(wmInfo.getContextPath()) &&
                !"/".equals(wmInfo.getContextPath())) {
            // Remove dummy context that was created off of docroot, if such
            // a context exists
            removeDummyModule(vs);
            updateDefaultWebModule(vs,
                    vs.getNetworkListenerNames(),
                    wmInfo);
        } else {
            WebModuleConfig wmc =
                    vs.createSystemDefaultWebModuleIfNecessary(
                            habitat.<WebArchivist>getService(WebArchivist.class));
            if (wmc != null) {
                loadStandaloneWebModule(vs, wmc);
            }
        }

        if (updateListeners) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        VS_UPDATED_NETWORK_LISTENERS,
                        new Object[]{vs.getName(), vs.getNetworkListeners(), vsBean.getNetworkListeners()});
            }
            /*
             * Need to update connector and mapper restart is required
             * when virtual-server.http-listeners is changed dynamically
             */
            List<NetworkListener> httpListeners =
                    serverConfig.getNetworkConfig().getNetworkListeners().getNetworkListener();
            if (httpListeners != null) {
                for (NetworkListener httpListener : httpListeners) {
                    updateConnector(httpListener, habitat.<HttpService>getService(HttpService.class));
                }
            }
        }

    }


    /**
     * Update virtual-server properties.
     */
    public void updateHostProperties(
            com.sun.enterprise.config.serverbeans.VirtualServer vsBean,
            String name, String value, SecurityService securityService, final VirtualServer vs) {
        if (vs == null) {
            return;
        }
        vs.setBean(vsBean);

        if (name == null) {
            return;
        }

        if (name.startsWith("alternatedocroot_")) {
            updateAlternateDocroot(vs);
        } else if ("setCacheControl".equals(name)) {
            vs.configureCacheControl(value);
        } else if (Constants.ACCESS_LOGGING_ENABLED.equals(name)) {
            vs.reconfigureAccessLog(globalAccessLogBufferSize,
                    globalAccessLogWriteInterval, habitat, domain,
                    globalAccessLoggingEnabled);
        } else if (Constants.ACCESS_LOG_PROPERTY.equals(name)) {
            vs.reconfigureAccessLog(globalAccessLogBufferSize,
                    globalAccessLogWriteInterval, habitat, domain,
                    globalAccessLoggingEnabled);
        } else if (Constants.ACCESS_LOG_WRITE_INTERVAL_PROPERTY.equals(name)) {
            vs.reconfigureAccessLog(globalAccessLogBufferSize,
                    globalAccessLogWriteInterval,
                    habitat,
                    domain,
                    globalAccessLoggingEnabled);
        } else if (Constants.ACCESS_LOG_BUFFER_SIZE_PROPERTY.equals(name)) {
            vs.reconfigureAccessLog(globalAccessLogBufferSize,
                    globalAccessLogWriteInterval,
                    habitat,
                    domain,
                    globalAccessLoggingEnabled);
        } else if ("allowRemoteHost".equals(name)
                || "denyRemoteHost".equals(name)) {
            vs.configureRemoteHostFilterValve();
        } else if ("allowRemoteAddress".equals(name)
                || "denyRemoteAddress".equals(name)) {
            vs.configureRemoteAddressFilterValve();
        } else if (Constants.SSO_ENABLED.equals(name)) {
            vs.configureSingleSignOn(globalSSOEnabled, webContainerFeatureFactory, isSsoFailoverEnabled());
        } else if ("authRealm".equals(name)) {
            vs.configureAuthRealm(securityService);
        } else if (name.startsWith("send-error")) {
            vs.configureErrorPage();
        } else if (Constants.ERROR_REPORT_VALVE.equals(name)) {
            vs.setErrorReportValveClass(value);
        } else if (name.startsWith("redirect")) {
            vs.configureRedirect();
        } else if (name.startsWith("contextXmlDefault")) {
            vs.setDefaultContextXmlLocation(value);
        }
    }

    private boolean isSsoFailoverEnabled() {
        boolean webContainerAvailabilityEnabled =
            serverConfigLookup.calculateWebAvailabilityEnabledFromConfig();
        boolean isSsoFailoverEnabled =
            serverConfigLookup.isSsoFailoverEnabledFromConfig();
        return isSsoFailoverEnabled && webContainerAvailabilityEnabled;
    }

    /**
     * Processes an update to the http-service element
     */
    public void updateHttpService(HttpService httpService) throws LifecycleException {

        if (httpService == null) {
            return;
        }

        /*
         * Update each virtual server with the sso-enabled and
         * access logging related properties of the updated http-service
         */
        globalSSOEnabled = ConfigBeansUtilities.toBoolean(httpService.getSsoEnabled());
        globalAccessLogWriteInterval = httpService.getAccessLog().getWriteIntervalSeconds();
        globalAccessLogBufferSize = httpService.getAccessLog().getBufferSizeBytes();
        globalAccessLoggingEnabled = ConfigBeansUtilities.toBoolean(httpService.getAccessLoggingEnabled());

        // for availability-service.web-container-availability
        webContainerFeatureFactory = getWebContainerFeatureFactory();

        List<com.sun.enterprise.config.serverbeans.VirtualServer> virtualServers =
                httpService.getVirtualServer();
        for (com.sun.enterprise.config.serverbeans.VirtualServer virtualServer : virtualServers) {
            final VirtualServer vs = (VirtualServer) getEngine().findChild(virtualServer.getId());
            if (vs != null) {
                vs.configureSingleSignOn(globalSSOEnabled, webContainerFeatureFactory, isSsoFailoverEnabled());
                vs.reconfigureAccessLog(globalAccessLogBufferSize, globalAccessLogWriteInterval, habitat, domain,
                        globalAccessLoggingEnabled);
                updateHost(virtualServer);
            }
        }

    }


    /**
     * Update an http-listener property
     *
     * @param listener  the configuration bean.
     * @param propName  the property name
     * @param propValue the property value
     */
    public void updateConnectorProperty(NetworkListener listener,
                                        String propName,
                                        String propValue)
            throws LifecycleException {

        WebConnector connector = connectorMap.get(listener.getName());
        if (connector != null) {
            connector.configHttpProperties(listener.findHttpProtocol().getHttp(),
                    listener.findTransport(), listener.findHttpProtocol().getSsl());
            connector.configureHttpListenerProperty(propName, propValue);
        }
    }


    /**
     * Update an network-listener
     *
     * @param httpService the configuration bean.
     */
    public void updateConnector(NetworkListener networkListener,
                                HttpService httpService)
            throws LifecycleException {

        synchronized (mapperUpdateSync) {
            // Disable dynamic reconfiguration of the http listener at which
            // the admin related webapps (including the admingui) are accessible.
            // Notice that in GlassFish v3, we support a domain.xml configuration
            // that does not declare any admin-listener, in which case the
            // admin-related webapps are accessible on http-listener-1.
            if (networkListener.findHttpProtocol().getHttp().getDefaultVirtualServer().equals(
                    org.glassfish.api.web.Constants.ADMIN_VS) ||
                    "http-listener-1".equals(networkListener.getName()) &&
                            connectorMap.get("admin-listener") == null) {
                return;
            }

            WebConnector connector = connectorMap.get(networkListener.getName());
            if (connector != null) {
                deleteConnector(connector);
            }

            if (!Boolean.valueOf(networkListener.getEnabled())) {
                return;
            }

            connector = addConnector(networkListener, httpService, false);

            // Update the list of listener names of all associated virtual servers with
            // the listener's new listener name , so that the associated virtual
            // servers will be registered with the listener's request mapper when
            // the listener is started
            List<VirtualServer> virtualServers =
                    getVirtualServersForHttpListenerId(httpService, networkListener.getName());
            if (virtualServers != null) {
                for (VirtualServer vs : virtualServers) {
                    boolean found = false;
                    String[] listenerNames = vs.getNetworkListenerNames();
                    String name = connector.getName();
                    for (String listenerName : listenerNames) {
                        if (listenerName.equals(name)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        String[] newListenerNames = new String[listenerNames.length + 1];
                        System.arraycopy(listenerNames, 0, newListenerNames, 0, listenerNames.length);
                        newListenerNames[listenerNames.length] = connector.getName();
                        vs.setNetworkListenerNames(newListenerNames);
                    }
                }
            }
            connector.start();
            // GLASSFISH-20932
            // Check if virtual server has default-web-module configured,
            // and if so, configure the http listener's mapper with this
            // information
            if (virtualServers != null) {
                Mapper mapper = connector.getMapper();
                for (VirtualServer vs : virtualServers) {
                    String defaultWebModulePath = vs.getDefaultContextPath(
                            domain, appRegistry);
                    if (defaultWebModulePath != null) {
                        try {
                            mapper.setDefaultContextPath(vs.getName(),
                                    defaultWebModulePath);
                            vs.setDefaultContextPath(defaultWebModulePath);
                        } catch (Exception e) {
                            throw new LifecycleException(e);
                        }
                    }
                }
            }
        }
    }
    /**
     * Method gets called, when GrizzlyService changes HTTP Mapper, associated
     * with specific port.
     *
     * @param httpService  {@link HttpService}
     * @param httpListener {@link NetworkListener}, which {@link Mapper} was changed
     * @param mapper       new {@link Mapper} value
     */
    public void updateMapper(HttpService httpService, NetworkListener httpListener,
                             Mapper mapper) {
        synchronized (mapperUpdateSync) {
            WebConnector connector = connectorMap.get(httpListener.getName());
            if (connector != null && connector.getMapper() != mapper) {
                try {
                    updateConnector(httpListener, httpService);
                } catch (LifecycleException le) {
                    logger.log(Level.SEVERE, EXCEPTION_CONFIG_HTTP_SERVICE, le);
                }
            }
        }
    }


    public WebConnector addConnector(NetworkListener httpListener,
                                     HttpService httpService,
                                     boolean start)
            throws LifecycleException {

        synchronized (mapperUpdateSync) {
            int port = Integer.parseInt(httpListener.getPort());

            // Add the listener name of the new http-listener to its
            // default-virtual-server, so that when the new http-listener
            // and its MapperListener are started, they will recognize the
            // default-virtual-server as one of their own, and add it to the
            // Mapper
            String virtualServerName = httpListener.findHttpProtocol().getHttp().getDefaultVirtualServer();
            VirtualServer vs =
                    (VirtualServer) getEngine().findChild(virtualServerName);
            List<String> list = Arrays.asList(vs.getNetworkListenerNames());
            // Avoid adding duplicate network-listener name
            if (!list.contains(httpListener.getName())) {
                String[] oldListenerNames = vs.getNetworkListenerNames();
                String[] newListenerNames = new String[oldListenerNames.length + 1];
                System.arraycopy(oldListenerNames, 0, newListenerNames, 0, oldListenerNames.length);
                newListenerNames[oldListenerNames.length] = httpListener.getName();
                vs.setNetworkListenerNames(newListenerNames);
            }

            Mapper mapper = null;
            for (Mapper m : habitat.<Mapper>getAllServices(Mapper.class)) {
                if (m.getPort() == port && m instanceof ContextMapper) {
                    ContextMapper cm = (ContextMapper) m;
                    if (httpListener.getName().equals(cm.getId())) {
                        mapper = m;
                        break;
                    }
                }
            }

            WebConnector connector =
                    createHttpListener(httpListener, httpService, mapper);

            if (connector.getRedirectPort() == -1) {
                connector.setRedirectPort(defaultRedirectPort);
            }

            if (start) {
                connector.start();
            }
            return connector;
        }
    }


    /**
     * Stops and deletes the specified http listener.
     */
    public void deleteConnector(WebConnector connector)
            throws LifecycleException {

        String name = connector.getName();

        Connector[] connectors = _embedded.findConnectors();
        for (Connector conn : connectors) {
            if (name.equals(conn.getName())) {
                _embedded.removeConnector(conn);
                connectorMap.remove(connector.getName());
            }
        }
    }


    /**
     * Stops and deletes the specified http listener.
     */
    public void deleteConnector(NetworkListener httpListener)
            throws LifecycleException {

        Connector[] connectors = _embedded.findConnectors();
        String name = httpListener.getName();
        for (Connector conn : connectors) {
            if (name.equals(conn.getName())) {
                _embedded.removeConnector(conn);
                connectorMap.remove(name);
            }
        }

    }


    /**
     * Reconfigures the access log valve of each virtual server with the
     * updated attributes of the <access-log> element from domain.xml.
     */
    public void updateAccessLog(HttpService httpService) {
        Container[] virtualServers = getEngine().findChildren();
        for (Container virtualServer : virtualServers) {
            ((VirtualServer) virtualServer).reconfigureAccessLog(
                    httpService, webContainerFeatureFactory);
        }
    }


    /**
     * Updates the docroot of the given virtual server
     */
    private void updateDocroot(
            String docroot,
            VirtualServer vs,
            com.sun.enterprise.config.serverbeans.VirtualServer vsBean) {

        validateDocroot(docroot, vsBean.getId(),
                vsBean.getDefaultWebModule());
        vs.setAppBase(docroot);
        removeDummyModule(vs);
        WebModuleConfig wmInfo =
                vs.createSystemDefaultWebModuleIfNecessary(
                        habitat.<WebArchivist>getService(WebArchivist.class));
        if (wmInfo != null) {
            loadStandaloneWebModule(vs, wmInfo);
        }
    }


    private void updateAlternateDocroot(VirtualServer vs) {
        removeDummyModule(vs);
        WebModuleConfig wmInfo = vs.createSystemDefaultWebModuleIfNecessary(
                habitat.<WebArchivist>getService(WebArchivist.class));
        if (wmInfo != null) {
            loadStandaloneWebModule(vs, wmInfo);
        }
    }

    public void updateJvmRoute(HttpService httpService, String jvmOption) {
        String jvmRoute = null;
        if (jvmOption.contains("{") && jvmOption.contains("}")) {
            // Look up system-property
            jvmOption = jvmOption.substring(jvmOption.indexOf("{")+1,jvmOption.indexOf("}"));
            jvmRoute = server.getSystemPropertyValue(jvmOption);
            if  (jvmRoute == null){
                // Try to get it from System property if it exists
                jvmRoute = System.getProperty(jvmOption);
            }
        } else if (jvmOption.contains("=")) {
            jvmRoute = jvmOption.substring(jvmOption.indexOf("=")+1);
        }
        engine.setJvmRoute(jvmRoute);
        for (com.sun.enterprise.config.serverbeans.VirtualServer vsBean :
                httpService.getVirtualServer()) {
            VirtualServer vs = (VirtualServer) engine.findChild(vsBean.getId());
            for (Container context : vs.findChildren()) {
                if (context instanceof StandardContext) {
                    ((StandardContext)context).setJvmRoute(jvmRoute);
                }
            }
        }
        for (Connector connector : _embedded.getConnectors()) {
            connector.setJvmRoute(jvmRoute);
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, JVM_ROUTE_UPDATED, jvmRoute);

        }
    }


    /**
     * is Tomcat using default domain name as its domain
     */
    protected boolean isTomcatUsingDefaultDomain() {
        // need to be careful and make sure tomcat jmx mapping works
        // since setting this to true might result in undeployment problems
        return true;
    }


    /**
     * Creates probe providers for Servlet, JSP, Session, and
     * Request/Response related events.
     * <p/>
     * While the Servlet, JSP, and Session related probe providers are
     * shared by all web applications (where every web application
     * qualifies its probe events with its application name), the
     * Request/Response related probe provider is shared by all HTTP
     * listeners.
     */
    private void createProbeProviders() {
        webModuleProbeProvider = new WebModuleProbeProvider();
        servletProbeProvider = new ServletProbeProvider();
        jspProbeProvider = new JspProbeProvider();
        sessionProbeProvider = new SessionProbeProvider();
        requestProbeProvider = new RequestProbeProvider();
    }


    /**
     * Creates statistics providers for Servlet, JSP, Session, and
     * Request/Response related events.
     */
    private void createStatsProviders() {
        httpStatsProviderBootstrap =
                habitat.getService(HttpServiceStatsProviderBootstrap.class);
        webStatsProviderBootstrap =
                habitat.getService(WebStatsProviderBootstrap.class);
    }


    /*
     * Loads the class with the given name using the common classloader,
     * which is responsible for loading any classes from the domain's
     * lib directory
     *
     * @param className the name of the class to load
     */

    public Class loadCommonClass(String className) throws Exception {
        return clh.getCommonClassLoader().loadClass(className);
    }

    /**
     * According to SRV 15.5.15, Servlets, Filters, Listeners can only be
     * without any scope annotation or are annotated with
     *
     * @Dependent scope. All other scopes are invalid and must be rejected.
     */
    private void validateJSR299Scope(Class<?> clazz) {
        if (jcdiService != null && jcdiService.isCDIScoped(clazz)) {
            String msg = rb.getString(INVALID_ANNOTATION_SCOPE);
            msg = MessageFormat.format(msg, clazz.getName());
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Return the WebContainerFeatureFactory according to the configuration.
     * @return WebContainerFeatuerFactory
     */
    private WebContainerFeatureFactory getWebContainerFeatureFactory() {
        String featureFactoryName =
                    (serverConfigLookup.calculateWebAvailabilityEnabledFromConfig() ? "ha" : "pe");
        return webContainerFeatureFactory = habitat.getService(
                    WebContainerFeatureFactory.class, featureFactoryName);
    }
}
