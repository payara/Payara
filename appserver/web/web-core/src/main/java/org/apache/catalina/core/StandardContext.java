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
// Portions Copyright [2016-2017] [Payara Foundation and/or its affiliates]

package org.apache.catalina.core;

import static java.text.MessageFormat.format;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static org.apache.catalina.ContainerEvent.AFTER_CONTEXT_INITIALIZER_ON_STARTUP;
import static org.apache.catalina.ContainerEvent.BEFORE_CONTEXT_INITIALIZER_ON_STARTUP;
import static org.apache.catalina.LogFacade.INVOKING_SERVLET_CONTAINER_INIT_EXCEPTION;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.naming.Binding;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.SingleThreadModel;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpUpgradeHandler;

import org.apache.catalina.Auditor;
import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.LogFacade;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Server;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.MappingImpl;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextLocalEjb;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.FilterMaps;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.MessageDestination;
import org.apache.catalina.deploy.MessageDestinationRef;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.ResourceParams;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.deploy.ServletMap;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.PersistentManagerBase;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.util.CustomObjectInputStream;
import org.apache.catalina.util.ExtensionValidator;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.URLEncoder;
import org.apache.naming.ContextBindings;
import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.WARDirContext;
import org.apache.naming.resources.WebDirContext;
import org.glassfish.grizzly.http.server.util.AlternateDocBase;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.grizzly.http.util.MessageBytes;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.web.loader.ServletContainerInitializerUtil;
import org.glassfish.web.loader.WebappClassLoader;
import org.glassfish.web.valve.GlassFishValve;

/**
 * Standard implementation of the <b>Context</b> interface.  Each
 * child container must be a Wrapper implementation to process the
 * requests directed to a particular servlet.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.48 $ $Date: 2007/07/25 00:52:04 $
 */

// Portions Copyright [2016] [Payara Foundation and/or its affiliates]

public class StandardContext
    extends ContainerBase
    implements Context, ServletContext
{

    // have two similar messages
    // have two similar messages
    // have to similar messages

	private static final String DEFAULT_RESPONSE_CHARACTER_ENCODING = "ISO-8859-1";

    private static final ClassLoader standardContextClassLoader =
        StandardContext.class.getClassLoader();

    private static final Set<SessionTrackingMode> DEFAULT_SESSION_TRACKING_MODES =
        EnumSet.of(SessionTrackingMode.COOKIE);

    /**
     * Array containing the safe characters set.
     */
    protected static final URLEncoder urlEncoder;

    /**
     * The descriptive information string for this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.StandardContext/1.0";

    private static final RuntimePermission GET_CLASSLOADER_PERMISSION =
        new RuntimePermission("getClassLoader");

    /**
     * GMT timezone - all HTTP dates are on GMT
     */
    static {
        urlEncoder = new URLEncoder();
        urlEncoder.addSafeCharacter('~');
        urlEncoder.addSafeCharacter('-');
        urlEncoder.addSafeCharacter('_');
        urlEncoder.addSafeCharacter('.');
        urlEncoder.addSafeCharacter('*');
        urlEncoder.addSafeCharacter('/');
    }


    // ----------------------------------------------------------- Constructors

    /**
     * Create a new StandardContext component with the default basic Valve.
     */
    public StandardContext() {
        pipeline.setBasic(new StandardContextValve());
        namingResources.setContainer(this);
        if (Globals.IS_SECURITY_ENABLED) {
            mySecurityManager = AccessController.doPrivileged(
                    new PrivilegedCreateSecurityManager());
        }
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * The alternate deployment descriptor name.
     */
    private String altDDName = null;

    /**
     * The antiJARLocking flag for this Context.
     */
    private boolean antiJARLocking = false;

    /**
     * Associated host name.
     */
    private String hostName;

    /**
     * The list of instantiated application event listeners
     */
    private List<EventListener> eventListeners =
        new ArrayList<EventListener>();

    /**
     * The list of ServletContextListeners
     */
    protected ArrayList<ServletContextListener> contextListeners =
        new ArrayList<ServletContextListener>();

    /**
     * The list of HttpSessionListeners
     */
    private List<HttpSessionListener> sessionListeners =
        new ArrayList<HttpSessionListener>();

    /**
     * The set of application parameters defined for this application.
     */
    private List<ApplicationParameter> applicationParameters =
        new ArrayList<ApplicationParameter>();

    /**
     * The application available flag for this Context.
     */
    private boolean available = false;

    /**
     * The broadcaster that sends j2ee notifications.
     */
    private NotificationBroadcasterSupport broadcaster = null;

    /**
     * The Locale to character set mapper for this application.
     */
    private CharsetMapper charsetMapper = null;

    /**
     * The Java class name of the CharsetMapper class to be created.
     */
    private String charsetMapperClass = CharsetMapper.class.getName();

	/**
	 * The request character encoding.
	 */
	private String requestCharacterEncoding;

	/**
	 * The response character encoding.
	 */
	private String responseCharacterEncoding;

    /**
     * The path to a file to save this Context information.
     */
    private String configFile = null;

    /**
     * The "correctly configured" flag for this Context.
     */
    private boolean configured = false;

    /**
     * The security constraints for this web application.
     */
    private List<SecurityConstraint> constraints =
        new ArrayList<SecurityConstraint>();

    /**
     * The ServletContext implementation associated with this Context.
     */
    // START RIMOD 4894300
    /*
    private transient ApplicationContext context = null;
    */
    protected ApplicationContext context = null;
    // END RIMOD 4894300

    /**
     *  Is the context initialized.
     */
    private boolean isContextInitializedCalled = false;

    /**
     * Compiler classpath to use.
     */
    private String compilerClasspath = null;

    /**
     * Should we attempt to use cookies for session id communication?
     */
    private boolean cookies = true;

    /**
     * true if the rewriting of URLs with the jsessionids of HTTP
     * sessions belonging to this context is enabled, false otherwise
     */
    private boolean enableURLRewriting = true;

    /**
     * Should we allow the <code>ServletContext.getContext()</code> method
     * to access the context of other web applications in this server?
     */
    private boolean crossContext = false;

    /**
     * The "follow standard delegation model" flag that will be used to
     * configure our ClassLoader.
     */
    private boolean delegate = false;

    /**
     * The display name of this web application.
     */
    private String displayName = null;

    /**
     * Override the default web xml location. ContextConfig is not configurable
     * so the setter is not used.
     */
    private String defaultWebXml;

    /**
     * The distributable flag for this web application.
     */
    private boolean distributable = false;

    /**
     * Thread local data used during request dispatch.
     */
    private ThreadLocal<DispatchData> dispatchData =
        new ThreadLocal<DispatchData>();

    /**
     * The document root for this web application.
     */
    private String docBase = null;

    /**
     * The exception pages for this web application, keyed by fully qualified
     * class name of the Java exception.
     */
    private Map<String, ErrorPage> exceptionPages =
        new HashMap<String, ErrorPage>();

    /**
     * The default error page (error page that was declared
     * without any exception-type and error-code).
     */
    private ErrorPage defaultErrorPage;

    /**
     * The set of filter configurations (and associated filter instances) we
     * have initialized, keyed by filter name.
     */
    private Map<String, FilterConfig> filterConfigs =
        new HashMap<String, FilterConfig>();

    /**
     * The set of filter definitions for this application, keyed by
     * filter name.
     */
    private Map<String, FilterDef> filterDefs = new HashMap<String, FilterDef>();

    /**
     * The list of filter mappings for this application, in the order
     * they were defined in the deployment descriptor.
     */
    private List<FilterMap> filterMaps = new ArrayList<FilterMap>();

    /**
     * The list of classnames of InstanceListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private ArrayList<String> instanceListeners = new ArrayList<String>();

    /**
     * The set of already instantiated InstanceListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private List<InstanceListener> instanceListenerInstances = new ArrayList<InstanceListener>();

    /**
     * The login configuration descriptor for this web application.
     */
    private LoginConfig loginConfig = null;

    /**
     * The mapper associated with this context.
     */
    private Mapper mapper = new Mapper();

    /**
     * The naming context listener for this web application.
     */
    private NamingContextListener namingContextListener = null;

    /**
     * The naming resources for this web application.
     */
    private NamingResources namingResources = new NamingResources();

    /**
     * The message destinations for this web application.
     */
    private Map<String, MessageDestination> messageDestinations = new HashMap<String, MessageDestination>();

    /**
     * The MIME mappings for this web application, keyed by extension.
     */
    private Map<String,String> mimeMappings = new HashMap<String,String>();

    /**
     * The context initialization parameters for this web application,
     * keyed by name.
     */
    private HashMap<String, String> parameters = new HashMap<String, String>();

    /**
     * The request processing pause flag (while reloading occurs)
     */
    private boolean paused = false;

    /**
     * The public identifier of the DTD for the web application deployment
     * descriptor version we are currently parsing.  This is used to support
     * relaxed validation rules when processing version 2.2 web.xml files.
     */
    private String publicId = null;

    /**
     * The reloadable flag for this web application.
     */
    private boolean reloadable = false;

    /**
     * Unpack WAR property.
     */
    private boolean unpackWAR = true;

    /**
     * The DefaultContext override flag for this web application.
     */
    private boolean override = false;

    /**
     * The original document root for this web application.
     */
    private String originalDocBase = null;

    /**
     * The privileged flag for this web application.
     */
    private boolean privileged = false;

    /**
     * Should the next call to <code>addWelcomeFile()</code> cause replacement
     * of any existing welcome files?  This will be set before processing the
     * web application's deployment descriptor, so that application specified
     * choices <strong>replace</strong>, rather than append to, those defined
     * in the global descriptor.
     */
    private boolean replaceWelcomeFiles = false;

    /**
     * With proxy caching disabled, setting this flag to true adds
     * Pragma and Cache-Control headers with "No-cache" as value.
     * Setting this flag to false does not add any Pragma header,
     * but sets the Cache-Control header to "private".
     */
    private boolean securePagesWithPragma = true;

    /**
     * The security role mappings for this application, keyed by role
     * name (as used within the application).
     */
    private Map<String, String> roleMappings = new HashMap<String, String>();

    /**
     * The security roles for this application
     */
    private List<String> securityRoles = new ArrayList<String>();

    /**
     * The servlet mappings for this web application, keyed by
     * matching pattern.
     */
    private final Map<String, String> servletMappings = new HashMap<String, String>();

    /**
     * The session timeout (in minutes) for this web application.
     */
    private int sessionTimeout = 30;

    /**
     * Has the session timeout (in minutes) for this web application
     * been over-ridden by web-xml
     * HERCULES:add
     */
    private boolean sessionTimeoutOveridden = false;

    /**
     * The notification sequence number.
     */
    private long sequenceNumber = 0;

    /**
     * The status code error pages for this web application, keyed by
     * HTTP status code (as an Integer).
     */
    private final Map<Integer, ErrorPage> statusPages =
        new HashMap<Integer, ErrorPage>();

    /**
     * Amount of ms that the container will wait for servlets to unload.
     */
    private long unloadDelay = 2000;

    /**
     * The watched resources for this application.
     */
    private List<String> watchedResources =
            Collections.synchronizedList(new ArrayList<String>());

    /**
     * The welcome files for this application.
     */
    private String[] welcomeFiles = new String[0];

    /**
     * The list of classnames of LifecycleListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private ArrayList<String> wrapperLifecycles = new ArrayList<String>();

    /**
     * The list of classnames of ContainerListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private List<String> wrapperListeners = new ArrayList<String>();

    /**
     * The pathname to the work directory for this context (relative to
     * the server's home if not absolute).
     */
    private String workDir = null;

    /**
     * JNDI use flag.
     */
    private boolean useNaming = true;

    /**
     * Filesystem based flag.
     */
    private boolean filesystemBased = false;

    /**
     * Name of the associated naming context.
     */
    private String namingContextName = null;

    /**
     * Frequency of the session expiration, and related manager operations.
     * Manager operations will be done once for the specified amount of
     * backgrondProcess calls (ie, the lower the amount, the most often the
     * checks will occur).
     */
    private int managerChecksFrequency = 6;

    /**
     * Iteration count for background processing.
     */
    private int count = 0;

    /**
     * Caching allowed flag.
     */
    private boolean cachingAllowed = true;

    /**
     * Case sensitivity.
     */
    protected boolean caseSensitive = true;

    /**
     * Allow linking.
     */
    protected boolean allowLinking = false;

    /**
     * Cache max size in KB.
     */
    protected int cacheMaxSize = 10240; // 10 MB

    /**
     * Cache TTL in ms.
     */
    protected int cacheTTL = 5000;

    /**
     * Non proxied resources.
     */
    private DirContext webappResources = null;

    /*
     * Time (in milliseconds) it took to start this context
     */
    private long startupTime;

    /*
     * Time (in milliseconds since January 1, 1970, 00:00:00) when this
     * context was started
     */
    private long startTimeMillis;

    private long tldScanTime;

    // START SJSWS 6324431
    // Should the filter and security mapping be done
    // in a case sensitive manner
    protected boolean caseSensitiveMapping = true;
    // END SJSWS 6324431

    // START S1AS8PE 4817642
    /**
     * The flag that specifies whether to reuse the session id (if any) from
     * the request for newly created sessions
     */
    private boolean reuseSessionID = false;
    // END S1AS8PE 4817642

    // START RIMOD 4642650
    /**
     * The flag that specifies whether this context allows sendRedirect() to
     * redirect to a relative URL.
     */
    private boolean allowRelativeRedirect = false;

    // END RIMOD 4642650

    /** Name of the engine. If null, the domain is used.
     */
    private String engineName = null;
    /* SJSAS 6340499
    private String j2EEApplication="none";
     */
    // START SJSAS 6340499
    private String j2EEApplication="null";
    // END SJSAS 6340499
    private String j2EEServer="none";

    // START IASRI 4823322
    /**
     * List of configured Auditors for this context.
     */
    private Auditor[] auditors = null;
    // END IASRI 4823322

    // START RIMOD 4868393
    /**
     * used to create unique id for each app instance.
     */
    private static AtomicInteger instanceIDCounter = new AtomicInteger(1);
    // END RIMOD 4868393

    /**
     * Attribute value used to turn on/off XML validation
     */
    private boolean webXmlValidation = false;

    private String jvmRoute;

    /**
     * Attribute value used to turn on/off XML namespace validation
     */
    private boolean webXmlNamespaceAware = false;

    /**
     * Attribute value used to turn on/off XML validation
     */
    private boolean tldValidation = false;

    /**
     * Attribute value used to turn on/off TLD XML namespace validation
     */
    private boolean tldNamespaceAware = false;

    /**
     * Is the context contains the JSF servlet.
     */
    protected boolean isJsfApplication = false;

    // START S1AS8PE 4965017
    private boolean isReload = false;
    // END S1AS8PE 4965017

    /**
     * Alternate doc base resources
     */
    private ArrayList<AlternateDocBase> alternateDocBases = null;

    private boolean useMyFaces;

    private Set<SessionTrackingMode> sessionTrackingModes;

    /**
     * Encoded path.
     */
    private String encodedPath = null;

    /**
     * Session cookie config
     */
    private SessionCookieConfig sessionCookieConfig;

    /**
     * The name of the session tracking cookies created by this context
     * Cache the name here as the getSessionCookieConfig() is synchronized.
     */
    private String sessionCookieName = Globals.SESSION_COOKIE_NAME;

    private boolean sessionCookieNameInitialized = false;

    protected ConcurrentMap<String, ServletRegistrationImpl> servletRegisMap =
        new ConcurrentHashMap<String, ServletRegistrationImpl>();

    protected ConcurrentMap<String, FilterRegistrationImpl> filterRegisMap =
        new ConcurrentHashMap<String, FilterRegistrationImpl>();

    /**
     * The list of ordered libs, which is used as the value of the
     * ServletContext attribute with name javax.servlet.context.orderedLibs
     */
    private List<String> orderedLibs;

    // <jsp-config> related info aggregated from web.xml and web-fragment.xml
    private JspConfigDescriptor jspConfigDesc;

    // ServletContextListeners may be registered (via
    // ServletContext#addListener) only within the scope of
    // ServletContainerInitializer#onStartup
    private boolean isProgrammaticServletContextListenerRegistrationAllowed = false;

    /*
     * Security manager responsible for enforcing permission check on
     * ServletContext#getClassLoader
     */
    private MySecurityManager mySecurityManager;

    // Iterable over all ServletContainerInitializers that were discovered
    private Iterable<ServletContainerInitializer> servletContainerInitializers = null;

    // The major Servlet spec version of the web.xml
    private int effectiveMajorVersion = 0;

    // The minor Servlet spec version of the web.xml
    private int effectiveMinorVersion = 0;

    // Created via embedded API
    private boolean isEmbedded = false;

    protected boolean directoryDeployed = false;

    protected boolean showArchivedRealPathEnabled = true;

    protected int servletReloadCheckSecs = 1;

    // ----------------------------------------------------- Context Properties

    public String getEncodedPath() {
        return encodedPath;
    }

    @Override
    public void setName( String name ) {
        super.setName( name );
        encodedPath = urlEncoder.encode(name);
    }

    /**
     * Is caching allowed ?
     */
    public boolean isCachingAllowed() {
        return cachingAllowed;
    }

    /**
     * Set caching allowed flag.
     */
    public void setCachingAllowed(boolean cachingAllowed) {
        this.cachingAllowed = cachingAllowed;
    }

    /**
     * Set case sensitivity.
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Is case sensitive ?
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    // START SJSWS 6324431
    /**
     * Set case sensitivity for filter and security constraint mappings.
     */
    public void setCaseSensitiveMapping(boolean caseSensitiveMap) {
        caseSensitiveMapping = caseSensitiveMap;
    }

    /**
     * Are filters and security constraints mapped in a case sensitive manner?
     */
    public boolean isCaseSensitiveMapping() {
        return caseSensitiveMapping;
    }
    // END SJSWS 6324431

    /**
     * Set allow linking.
     */
    public void setAllowLinking(boolean allowLinking) {
        this.allowLinking = allowLinking;
    }

    /**
     * Is linking allowed.
     */
    public boolean isAllowLinking() {
        return allowLinking;
    }

    /**
     * Set cache TTL.
     */
    public void setCacheTTL(int cacheTTL) {
        this.cacheTTL = cacheTTL;
    }

    /**
     * Get cache TTL.
     */
    public int getCacheTTL() {
        return cacheTTL;
    }

    /**
     * Return the maximum size of the cache in KB.
     */
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }

    /**
     * Set the maximum size of the cache in KB.
     */
    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    /**
     * Return the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     */
    public boolean getDelegate() {
        return delegate;
    }

    /**
     * Set the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     *
     * @param delegate The new flag
     */
    public void setDelegate(boolean delegate) {
        boolean oldDelegate = this.delegate;
        this.delegate = delegate;
        support.firePropertyChange("delegate", Boolean.valueOf(oldDelegate),
                                   Boolean.valueOf(this.delegate));
    }

    /**
     * Returns true if the internal naming support is used.
     */
    public synchronized boolean isUseNaming() {
        return useNaming;
    }

    /**
     * Enables or disables naming.
     */
    public synchronized void setUseNaming(boolean useNaming) {
        this.useNaming = useNaming;
    }

    /**
     * @return true if the resources associated with this context are
     * filesystem based, false otherwise
     */
    public boolean isFilesystemBased() {
        return filesystemBased;
    }

    /**
     * @return the list of initialized application event listeners
     * of this application, in the order in which they have been specified
     * in the deployment descriptor
     */
    @Override
    public List<EventListener> getApplicationEventListeners() {
        return eventListeners;
    }

    public List<HttpSessionListener> getSessionListeners() {
        return sessionListeners;
    }

    /**
     * Return the application available flag for this Context.
     */
    @Override
    public boolean getAvailable() {
        return available;
    }

    /**
     * Set the application available flag for this Context.
     *
     * @param available The new application available flag
     */
    @Override
    public void setAvailable(boolean available) {
        boolean oldAvailable = this.available;
        this.available = available;
        support.firePropertyChange("available", Boolean.valueOf(oldAvailable),
            Boolean.valueOf(this.available));
    }

    /**
     * Return the antiJARLocking flag for this Context.
     */
    public boolean getAntiJARLocking() {

        return (this.antiJARLocking);

    }

    /**
     * Set the antiJARLocking feature for this Context.
     *
     * @param antiJARLocking The new flag value
     */
    public void setAntiJARLocking(boolean antiJARLocking) {

        boolean oldAntiJARLocking = this.antiJARLocking;
        this.antiJARLocking = antiJARLocking;
        support.firePropertyChange("antiJARLocking",
                oldAntiJARLocking,
                this.antiJARLocking);

    }

    /**
     * Return the Locale to character set mapper for this Context.
     */
    @Override
    public CharsetMapper getCharsetMapper() {

        // Create a mapper the first time it is requested
        if (this.charsetMapper == null) {
            try {
                Class clazz = Class.forName(charsetMapperClass);
                this.charsetMapper =
                  (CharsetMapper) clazz.newInstance();
            } catch (Throwable t) {
                this.charsetMapper = new CharsetMapper();
            }
        }

        return (this.charsetMapper);
    }

    /**
     * Set the Locale to character set mapper for this Context.
     *
     * @param mapper The new mapper
     */
    @Override
    public void setCharsetMapper(CharsetMapper mapper) {
        CharsetMapper oldCharsetMapper = this.charsetMapper;
        this.charsetMapper = mapper;
        if( mapper != null )
            this.charsetMapperClass= mapper.getClass().getName();
        support.firePropertyChange("charsetMapper", oldCharsetMapper,
                                   this.charsetMapper);
    }

	@Override
	public String getRequestCharacterEncoding() {
		return requestCharacterEncoding;
	}

	@Override
	public void setRequestCharacterEncoding(String encoding) {
		this.requestCharacterEncoding = encoding;
	}

	@Override
	public String getResponseCharacterEncoding() {
		return responseCharacterEncoding;
	}

	@Override
	public void setResponseCharacterEncoding(String encoding) {
		responseCharacterEncoding = encoding;
	}

    /**
     * @return the path to a file to save this Context information
     */
    @Override
    public String getConfigFile() {
        return configFile;
    }

    /**
     * Set the path to a file to save this Context information.
     *
     * @param configFile The path to a file to save this Context information
     */
    @Override
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    /**
     * @return the "correctly configured" flag for this Context
     */
    @Override
    public boolean getConfigured() {
        return configured;
    }

    /**
     * Sets the "correctly configured" flag for this Context.  This can be
     * set to false by startup listeners that detect a fatal configuration
     * error to avoid the application from being made available.
     *
     * @param configured The new correctly configured flag
     */
    @Override
    public void setConfigured(boolean configured) {
        boolean oldConfigured = this.configured;
        this.configured = configured;
        support.firePropertyChange("configured",
            Boolean.valueOf(oldConfigured), Boolean.valueOf(this.configured));
    }

    /**
     * @return the "use cookies for session ids" flag
     */
    @Override
    public boolean getCookies() {
        return cookies;
    }

    /**
     * Set the "use cookies for session ids" flag.
     *
     * @param cookies The new flag
     */
    @Override
    public void setCookies(boolean cookies) {
        boolean oldCookies = this.cookies;
        this.cookies = cookies;
        support.firePropertyChange("cookies",
                                   Boolean.valueOf(oldCookies),
                                   Boolean.valueOf(this.cookies));
    }

    /**
     * Checks whether the rewriting of URLs with the jsessionids of
     * HTTP sessions belonging to this context is enabled or not.
     *
     * @return true if the rewriting of URLs with the jsessionids of HTTP
     * sessions belonging to this context is enabled, false otherwise
     */
    @Override
    public boolean isEnableURLRewriting() {
        return enableURLRewriting;
    }

    /**
     * Enables or disables the rewriting of URLs with the jsessionids of
     * HTTP sessions belonging to this context.
     *
     * @param enableURLRewriting true if the rewriting of URLs with the
     * jsessionids of HTTP sessions belonging to this context should be
     * enabled, false otherwise
     */
    @Override
    public void setEnableURLRewriting(boolean enableURLRewriting) {
        boolean oldEnableURLRewriting = this.enableURLRewriting;
        this.enableURLRewriting = enableURLRewriting;
        support.firePropertyChange("enableURLRewriting",
                                   Boolean.valueOf(oldEnableURLRewriting),
                                   Boolean.valueOf(this.enableURLRewriting));
    }


    /**
     * @return the "allow crossing servlet contexts" flag
     */
    @Override
    public boolean getCrossContext() {
        return (this.crossContext);
    }

    /**
     * Sets the "allow crossing servlet contexts" flag.
     *
     * @param crossContext The new cross contexts flag
     */
    @Override
    public void setCrossContext(boolean crossContext) {

        boolean oldCrossContext = this.crossContext;
        this.crossContext = crossContext;
        support.firePropertyChange("crossContext",
                                   Boolean.valueOf(oldCrossContext),
                                   Boolean.valueOf(this.crossContext));
    }

    public String getDefaultWebXml() {
        return defaultWebXml;
    }

    /** Set the location of the default web xml that will be used.
     * If not absolute, it'll be made relative to the engine's base dir
     * ( which defaults to catalina.base system property ).
     *
     * XXX  If a file is not found - we can attempt a getResource()
     *
     * @param defaultWebXml
     */
    public void setDefaultWebXml(String defaultWebXml) {
        this.defaultWebXml = defaultWebXml;
    }

    /**
     * Gets the time (in milliseconds) it took to start this context.
     *
     * @return Time (in milliseconds) it took to start this context.
     */
    public long getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(long startupTime) {
        this.startupTime = startupTime;
    }

    public long getTldScanTime() {
        return tldScanTime;
    }

    public void setTldScanTime(long tldScanTime) {
        this.tldScanTime = tldScanTime;
    }

    /**
     * Return the display name of this web application.
     */
    @Override
    public String getDisplayName() {

        return (this.displayName);

    }

    /**
     * Return the alternate Deployment Descriptor name.
     */
    @Override
    public String getAltDDName(){
        return altDDName;
    }

    /**
     * Set an alternate Deployment Descriptor name.
     */
    @Override
    public void setAltDDName(String altDDName) {
        this.altDDName = altDDName;
        if (context != null) {
            context.setAttribute(Globals.ALT_DD_ATTR,altDDName);
            context.setAttributeReadOnly(Globals.ALT_DD_ATTR);
        }
    }

    /**
     * Return the compiler classpath.
     */
    public String getCompilerClasspath(){
        return compilerClasspath;
    }

    /**
     * Set the compiler classpath.
     */
    public void setCompilerClasspath(String compilerClasspath) {
        this.compilerClasspath = compilerClasspath;
    }

    /**
     * Set the display name of this web application.
     *
     * @param displayName The new display name
     */
    @Override
    public void setDisplayName(String displayName) {
        String oldDisplayName = this.displayName;
        this.displayName = displayName;
        support.firePropertyChange("displayName", oldDisplayName,
                                   this.displayName);
    }

    /**
     * Return the distributable flag for this web application.
     */
    @Override
    public boolean getDistributable() {
        return distributable;
    }

    /**
     * Set the distributable flag for this web application.
     *
     * @param distributable The new distributable flag
     */
    @Override
    public void setDistributable(boolean distributable) {
        boolean oldDistributable = this.distributable;
        this.distributable = distributable;
        support.firePropertyChange("distributable",
                                   Boolean.valueOf(oldDistributable),
                                   Boolean.valueOf(this.distributable));

        // Bugzilla 32866
        if(getManager() != null) {
            if(log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Propagating distributable=" + distributable
                        + " to manager");
            }
            getManager().setDistributable(distributable);
        }
    }

    /**
     * Return the document root for this Context.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     */
    @Override
    public String getDocBase() {
        synchronized (this) {
            return docBase;
        }
    }

    /**
     * Set the document root for this Context.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     *
     * @param docBase The new document root
     */
    @Override
    public void setDocBase(String docBase) {
        synchronized (this) {
            this.docBase = docBase;
        }
    }

    /**
     * Configures this context's alternate doc base mappings.
     *
     * @param urlPattern
     * @param docBase
     */
    public void addAlternateDocBase(String urlPattern, String docBase) {

        if (urlPattern == null || docBase == null) {
            throw new IllegalArgumentException(rb.getString(LogFacade.MISS_PATH_OR_URL_PATTERN_EXCEPTION));
        }

        AlternateDocBase alternateDocBase = new AlternateDocBase();
        alternateDocBase.setUrlPattern(urlPattern);
        alternateDocBase.setDocBase(docBase);
        alternateDocBase.setBasePath(getBasePath(docBase));

        if (alternateDocBases == null) {
            alternateDocBases = new ArrayList<AlternateDocBase>();
        }
        alternateDocBases.add(alternateDocBase);
    }

    /**
     * Gets this context's configured alternate doc bases.
     *
     * @return This context's configured alternate doc bases
     */
    public ArrayList<AlternateDocBase> getAlternateDocBases() {
        return alternateDocBases;
    }

    /**
     * Return the frequency of manager checks.
     */
    public int getManagerChecksFrequency() {
        return managerChecksFrequency;
    }

    /**
     * Set the manager checks frequency.
     *
     * @param managerChecksFrequency the new manager checks frequency
     */
    public void setManagerChecksFrequency(int managerChecksFrequency) {

        if (managerChecksFrequency <= 0) {
            return;
        }

        int oldManagerChecksFrequency = this.managerChecksFrequency;
        this.managerChecksFrequency = managerChecksFrequency;
        support.firePropertyChange("managerChecksFrequency",
            Integer.valueOf(oldManagerChecksFrequency),
            Integer.valueOf(this.managerChecksFrequency));
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

    public void setJvmRoute(String jvmRoute) {
        this.jvmRoute = jvmRoute;
    }

    public String getJvmRoute() {
        return jvmRoute;
    }

    public String getEngineName() {
        if( engineName != null ) return engineName;
        return domain;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getJ2EEApplication() {
        return j2EEApplication;
    }

    public void setJ2EEApplication(String j2EEApplication) {
        this.j2EEApplication = j2EEApplication;
    }

    public String getJ2EEServer() {
        return j2EEServer;
    }

    public void setJ2EEServer(String j2EEServer) {
        this.j2EEServer = j2EEServer;
    }

    /**
     * Return the login configuration descriptor for this web application.
     */
    @Override
    public LoginConfig getLoginConfig() {
        return (this.loginConfig);
    }

    /**
     * Set the login configuration descriptor for this web application.
     *
     * @param config The new login configuration
     */
    @Override
    public void setLoginConfig(LoginConfig config) {

        // Validate the incoming property value
        if (config == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.LOGIN_CONFIG_REQUIRED_EXCEPTION));
        String loginPage = config.getLoginPage();
        if ((loginPage != null) && !loginPage.startsWith("/")) {
            if (isServlet22()) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, LogFacade.FORM_LOGIN_PAGE_FINE, loginPage);
                }
                config.setLoginPage("/" + loginPage);
            } else {
                String msg = MessageFormat.format(rb.getString(LogFacade.LOGIN_CONFIG_LOGIN_PAGE_EXCEPTION), loginPage);
                throw new IllegalArgumentException(msg);
            }
        }
        String errorPage = config.getErrorPage();
        if ((errorPage != null) && !errorPage.startsWith("/")) {
            if (isServlet22()) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, LogFacade.FORM_ERROR_PAGE_FINE, errorPage);
                }
                config.setErrorPage("/" + errorPage);
            } else {
                String msg = MessageFormat.format(rb.getString(LogFacade.LOGIN_CONFIG_ERROR_PAGE_EXCEPTION), errorPage);
                throw new IllegalArgumentException(msg);
            }
        }

        // Process the property setting change
        LoginConfig oldLoginConfig = this.loginConfig;
        this.loginConfig = config;
        support.firePropertyChange("loginConfig",
                                   oldLoginConfig, this.loginConfig);
    }

    /**
     * Get the mapper associated with the context.
     */
    @Override
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * Sets a new pipeline
     */
    public void restrictedSetPipeline(Pipeline pl) {
        synchronized (this) {
            pl.setBasic(new StandardContextValve());
            pipeline = pl;
            hasCustomPipeline = true;
        }
    }

    /**
     * Return the naming resources associated with this web application.
     */
    @Override
    public NamingResources getNamingResources() {
        return namingResources;
    }

    /**
     * Set the naming resources for this web application.
     *
     * @param namingResources The new naming resources
     */
    @Override
    public void setNamingResources(NamingResources namingResources) {

        // Process the property setting change
        NamingResources oldNamingResources = this.namingResources;
        this.namingResources = namingResources;
        support.firePropertyChange("namingResources",
                                   oldNamingResources, this.namingResources);
    }

    /**
     * Return the context path for this Context.
     */
    @Override
    public String getPath() {
        return (getName());
    }

    /**
     * Set the context path for this Context.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  The context path is used as the "name" of
     * a Context, because it must be unique.
     *
     * @param path The new context path
     */
    @Override
    public void setPath(String path) {
        // XXX  Use host in name
        /* GlassFish Issue 2339
        setName(RequestUtil.URLDecode(path));
         */
        // START GlassFish Issue 2339
        setName(RequestUtil.urlDecode(path, "UTF-8"));
        // END GlassFish Issue 2339
    }

    /**
     * Return the public identifier of the deployment descriptor DTD that is
     * currently being parsed.
     */
    @Override
    public String getPublicId() {
        return publicId;
    }

    /**
     * Set the public identifier of the deployment descriptor DTD that is
     * currently being parsed.
     *
     * @param publicId The public identifier
     */
    @Override
    public void setPublicId(String publicId) {
        if (log.isLoggable(Level.FINEST))
            log.log(Level.FINEST, "Setting deployment descriptor public ID to '" +
                    publicId + "'");

        String oldPublicId = this.publicId;
        this.publicId = publicId;
        support.firePropertyChange("publicId", oldPublicId, publicId);
    }

    /**
     * Return the reloadable flag for this web application.
     */
    @Override
    public boolean getReloadable() {
        return reloadable;
    }

    /**
     * Return the DefaultContext override flag for this web application.
     */
    @Override
    public boolean getOverride() {
        return override;
    }

    /**
     * Gets the original document root for this Context, which can be an
     * absolute pathname, a relative pathname, or a URL.
     *
     * Is only set as deployment has change docRoot!
     */
    public String getOriginalDocBase() {
        return (this.originalDocBase);
    }

    /**
     * Set the original document root for this Context, which can be an
     * absolute pathname, a relative pathname, or a URL.
     *
     * @param docBase The original document root
     */
    public void setOriginalDocBase(String docBase) {
        this.originalDocBase = docBase;
    }

    /**
     * Return the privileged flag for this web application.
     */
    @Override
    public boolean getPrivileged() {
        return (this.privileged);
    }

    /**
     * Set the privileged flag for this web application.
     *
     * @param privileged The new privileged flag
     */
    @Override
    public void setPrivileged(boolean privileged) {
        boolean oldPrivileged = this.privileged;
        this.privileged = privileged;
        support.firePropertyChange("privileged",
                                   Boolean.valueOf(oldPrivileged),
                                   Boolean.valueOf(this.privileged));
    }

    /**
     * Set the reloadable flag for this web application.
     *
     * @param reloadable The new reloadable flag
     */
    @Override
    public void setReloadable(boolean reloadable) {
        boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
        support.firePropertyChange("reloadable",
                                   Boolean.valueOf(oldReloadable),
                                   Boolean.valueOf(this.reloadable));
    }

    /**
     * Set the DefaultContext override flag for this web application.
     *
     * @param override The new override flag
     */
    @Override
    public void setOverride(boolean override) {
        boolean oldOverride = this.override;
        this.override = override;
        support.firePropertyChange("override",
                                   Boolean.valueOf(oldOverride),
                                   Boolean.valueOf(this.override));
    }

    // START SJSAS 8.1 5049111
    /**
     * Scan the parent when searching for TLD listeners.
     */
    @Override
    public boolean isJsfApplication(){
        return isJsfApplication;
    }
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
    @Override
    public boolean hasAdHocPaths() {
        return false;
    }

    /**
     * Returns the name of the ad-hoc servlet responsible for servicing the
     * given path.
     *
     * @param path The path to service
     *
     * @return The name of the ad-hoc servlet responsible for servicing the
     * given path, or null if the given path is not an ad-hoc path
     */
    @Override
    public String getAdHocServletName(String path) {
        return null;
    }
    // END SJSAS 6253524

    /**
     * Return the "replace welcome files" property.
     */
    public boolean isReplaceWelcomeFiles() {
        return replaceWelcomeFiles;
    }

    /**
     * Set the "replace welcome files" property.
     *
     * @param replaceWelcomeFiles The new property value
     */
    public void setReplaceWelcomeFiles(boolean replaceWelcomeFiles) {

        boolean oldReplaceWelcomeFiles = this.replaceWelcomeFiles;
        this.replaceWelcomeFiles = replaceWelcomeFiles;
        support.firePropertyChange("replaceWelcomeFiles",
                                   Boolean.valueOf(oldReplaceWelcomeFiles),
                                   Boolean.valueOf(this.replaceWelcomeFiles));
    }

    /**
     * Returns the value of the securePagesWithPragma property.
     */
    public boolean isSecurePagesWithPragma() {
        return securePagesWithPragma;
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
    @Override
    public void setSecurePagesWithPragma(boolean securePagesWithPragma) {

        boolean oldSecurePagesWithPragma = this.securePagesWithPragma;
        this.securePagesWithPragma = securePagesWithPragma;
        support.firePropertyChange("securePagesWithPragma",
                                   Boolean.valueOf(oldSecurePagesWithPragma),
                                   Boolean.valueOf(this.securePagesWithPragma));
    }

    public void setUseMyFaces(boolean useMyFaces) {
        this.useMyFaces = useMyFaces;
    }

    public boolean isUseMyFaces() {
        return useMyFaces;
    }

    /**
     * Return the servlet context for which this Context is a facade.
     */
    @Override
    public ServletContext getServletContext() {
        if (context == null) {
            context = new ApplicationContext(this);
            if (altDDName != null
                    && context.getAttribute(Globals.ALT_DD_ATTR) == null){
                context.setAttribute(Globals.ALT_DD_ATTR,altDDName);
                context.setAttributeReadOnly(Globals.ALT_DD_ATTR);
            }
        }

        return context.getFacade();
    }

    /**
     * Return the default session timeout (in minutes) for this
     * web application.
     */
    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Is the session timeout (in minutes) for this
     * web application over-ridden from the default
     * HERCULES:add
     */
    public boolean isSessionTimeoutOveridden() {
        return sessionTimeoutOveridden;
    }

    /**
     * Set the default session timeout (in minutes) for this
     * web application.
     *
     * @param timeout The new default session timeout
     */
    @Override
    public void setSessionTimeout(int timeout) {

        int oldSessionTimeout = this.sessionTimeout;

        /*
         * SRV.13.4 ("Deployment Descriptor"):
         * If the timeout is 0 or less, the container ensures the default
         * behaviour of sessions is never to time out.
         */
        this.sessionTimeout = (timeout == 0) ? -1 : timeout;
        support.firePropertyChange("sessionTimeout",
                                   Integer.valueOf(oldSessionTimeout),
                                   Integer.valueOf(this.sessionTimeout));
        //HERCULES:add
        sessionTimeoutOveridden = true;
        //end HERCULES:add
    }

    /**
     * Return the value of the unloadDelay flag.
     */
    public long getUnloadDelay() {

        return (this.unloadDelay);

    }

    /**
     * Set the value of the unloadDelay flag, which represents the amount
     * of ms that the container will wait when unloading servlets.
     * Setting this to a small value may cause more requests to fail
     * to complete when stopping a web application.
     *
     * @param unloadDelay The new value
     */
    public void setUnloadDelay(long unloadDelay) {

        long oldUnloadDelay = this.unloadDelay;
        this.unloadDelay = unloadDelay;
        support.firePropertyChange("unloadDelay",
                                   Long.valueOf(oldUnloadDelay),
                                   Long.valueOf(this.unloadDelay));

    }

    /**
     * Unpack WAR flag accessor.
     */
    public boolean getUnpackWAR() {
        return unpackWAR;
    }

    /**
     * Unpack WAR flag mutator.
     */
    public void setUnpackWAR(boolean unpackWAR) {
        this.unpackWAR = unpackWAR;
    }

    /**
     * Set the resources DirContext object with which this Container is
     * associated.
     *
     * @param resources The newly associated DirContext
     */
    @Override
    public synchronized void setResources(DirContext resources) {

        if (started) {
            throw new IllegalStateException(rb.getString(LogFacade.RESOURCES_STARTED));
        }

        DirContext oldResources = this.webappResources;
        if (oldResources == resources)
            return;

        if (resources instanceof BaseDirContext) {
            BaseDirContext baseDirContext = (BaseDirContext)resources;
            baseDirContext.setCached(isCachingAllowed());
            baseDirContext.setCacheTTL(getCacheTTL());
            baseDirContext.setCacheMaxSize(getCacheMaxSize());
        }
        if (resources instanceof FileDirContext) {
            filesystemBased = true;
            FileDirContext fileDirContext = (FileDirContext)resources;
            fileDirContext.setCaseSensitive(isCaseSensitive());
            fileDirContext.setAllowLinking(isAllowLinking());
        }
        this.webappResources = resources;

        // The proxied resources will be refreshed on start
        this.resources = null;

        support.firePropertyChange("resources", oldResources,
                                   this.webappResources);

    }

    private synchronized void setAlternateResources(
                            AlternateDocBase alternateDocBase,
                            DirContext resources) {

        if (started) {
            throw new IllegalStateException(rb.getString(LogFacade.RESOURCES_STARTED));
        }

        final DirContext oldResources = ContextsAdapterUtility.unwrap(
                alternateDocBase.getWebappResources());

        if (oldResources == resources)
            return;

        if (resources instanceof BaseDirContext) {
            ((BaseDirContext) resources).setCached(isCachingAllowed());
            ((BaseDirContext) resources).setCacheTTL(getCacheTTL());
            ((BaseDirContext) resources).setCacheMaxSize(getCacheMaxSize());
        }
        if (resources instanceof FileDirContext) {
            filesystemBased = true;
            ((FileDirContext) resources).setCaseSensitive(isCaseSensitive());
            ((FileDirContext) resources).setAllowLinking(isAllowLinking());
        }
        alternateDocBase.setWebappResources(ContextsAdapterUtility.wrap(resources));
        // The proxied resources will be refreshed on start
        alternateDocBase.setResources(null);
    }

    // START S1AS8PE 4817642
    /**
     * Return the "reuse session IDs when creating sessions" flag
     */
    @Override
    public boolean getReuseSessionID() {
        return reuseSessionID;
    }

    /**
     * Set the "reuse session IDs when creating sessions" flag
     *
     * @param reuse The new value for the flag
     */
    @Override
    public void setReuseSessionID(boolean reuse) {
        reuseSessionID = reuse;
    }
    // END S1AS8PE 4817642



    // START RIMOD 4642650
    /**
     * Return whether this context allows sendRedirect() to redirect
     * to a relative URL.
     *
     * The default value for this property is 'false'.
     */
    @Override
    public boolean getAllowRelativeRedirect() {

        return allowRelativeRedirect;

    }


    /**
     * Set whether this context allows sendRedirect() to redirect
     * to a relative URL.
     *
     * @param allowRelativeURLs The new value for this property.
     *                          The default value for this property is
     *                          'false'.
     */
    @Override
    public void setAllowRelativeRedirect(boolean allowRelativeURLs) {

        allowRelativeRedirect = allowRelativeURLs;

    }


    // END RIMOD 4642650

    // START IASRI 4823322
    /**
     * Get Auditors associated with this context, if any.
     *
     * @return array of Auditor objects, or null
     *
     */
    @Override
    public Auditor[] getAuditors() {
        return auditors;
    }


    /**
     * Set the Auditors associated with this context.
     *
     * @param auditor array of Auditor objects
     *
     */
    @Override
    public void setAuditors(Auditor[] auditor) {
        this.auditors=auditor;
    }
    // END IASRI 4823322


    // START S1AS8PE 4965017
    public void setReload(boolean isReload) {
        this.isReload = isReload;
    }

    public boolean isReload() {
        return isReload;
    }
    // END S1AS8PE 4965017

    public void setEmbedded(boolean isEmbedded) {
        this.isEmbedded = isEmbedded;
    }

    public boolean isEmbedded() {
        return isEmbedded;
    }

    /**
     * Should we generate directory listings?
     */
    protected boolean directoryListing = false;

    /**
     * Enables or disables directory listings on this <tt>Context</tt>.
     */
    public void setDirectoryListing(boolean directoryListing) {
        this.directoryListing = directoryListing;
        Wrapper wrapper = (Wrapper) findChild(
                org.apache.catalina.core.Constants.DEFAULT_SERVLET_NAME);
        if (wrapper !=null) {
            Servlet servlet = ((StandardWrapper)wrapper).getServlet();
            if (servlet instanceof DefaultServlet) {
                ((DefaultServlet)servlet).setListings(directoryListing);
            }
        }
    }

    /**
     * Checks whether directory listings are enabled or disabled on this
     * <tt>Context</tt>.
     */
    public boolean isDirectoryListing() {
        return directoryListing;
    }

    // ------------------------------------------------------ Public Properties


    /**
     * Return the Locale to character set mapper class for this Context.
     */
    public String getCharsetMapperClass() {

        return (this.charsetMapperClass);

    }


    /**
     * Set the Locale to character set mapper class for this Context.
     *
     * @param mapper The new mapper class
     */
    public void setCharsetMapperClass(String mapper) {

        String oldCharsetMapperClass = this.charsetMapperClass;
        this.charsetMapperClass = mapper;
        support.firePropertyChange("charsetMapperClass",
                                   oldCharsetMapperClass,
                                   this.charsetMapperClass);

    }


    /**
     * Get the absolute path to the work dir.
     *
     * @return the absolute path to the work dir
     */
    public String getWorkPath() {
        if (getWorkDir() == null) {
            return null;
        }
        File workDir = new File(getWorkDir());
        if (!workDir.isAbsolute()) {
            File catalinaHome = engineBase();
            String catalinaHomePath = null;
            try {
                catalinaHomePath = catalinaHome.getCanonicalPath();
                workDir = new File(catalinaHomePath,
                        getWorkDir());
            } catch (IOException e) {
            }
        }
              return workDir.getAbsolutePath();
    }

    /**
     * Return the work directory for this Context.
     */
    public String getWorkDir() {
        synchronized (this) {
            return (this.workDir);
        }
    }


    /**
     * Set the work directory for this Context.
     *
     * @param workDir The new work directory
     */
    public void setWorkDir(String workDir) {
        synchronized (this) {
            this.workDir = workDir;
            if (started) {
                postWorkDirectory();
            }
        }
    }


    // -------------------------------------------------------- Context Methods


    /**
     * Adds the Listener with the given class name that is declared in the
     * deployment descriptor to the set of Listeners configured for this
     * application.
     *
     * @param listener the fully qualified class name of the Listener
     */
    @Override
    public void addApplicationListener(String listener) {
        addListener(listener, false);
    }

    /**
     * Add a new application parameter for this application.
     *
     * @param parameter The new application parameter
     */
    @Override
    public void addApplicationParameter(ApplicationParameter parameter) {
        String newName = parameter.getName();
        Iterator<ApplicationParameter> i =
            applicationParameters.iterator();
        while (i.hasNext()) {
            ApplicationParameter applicationParameter = i.next();
            if (newName.equals(applicationParameter.getName())) {
                if (applicationParameter.getOverride()) {
                    applicationParameter.setValue(parameter.getValue());
                }
                return;
            }
        }

        applicationParameters.add(parameter);

        if (notifyContainerListeners) {
            fireContainerEvent("addApplicationParameter", parameter);
        }
    }

    /**
     * Adds the given child Container to this context.
     *
     * @param child the child Container to add
     *
     * @exception IllegalArgumentException if the given child Container is
     * not an instance of Wrapper
     */
    @Override
    public void addChild(Container child) {
        addChild(child, false, true);
    }

    /**
     * Adds the given child (Servlet) to this context.
     *
     * @param child the child (Servlet) to add
     * @param isProgrammatic true if the given child (Servlet) is being
     * added via one of the programmatic interfaces, and false if it is
     * declared in the deployment descriptor
     * @param createRegistration true if a ServletRegistration needs to be
     * created for the given child, and false if a (preliminary)
     * ServletRegistration had already been created (which would be the
     * case if the Servlet had been declared in the deployment descriptor
     * without any servlet-class, and the servlet-class was later provided
     * via ServletContext#addServlet)
     *
     * @exception IllegalArgumentException if the given child Container is
     * not an instance of Wrapper
     */
    protected void addChild(Container child, boolean isProgrammatic,
            boolean createRegistration) {

        if (!(child instanceof Wrapper)) {
            throw new IllegalArgumentException(rb.getString(LogFacade.NO_WRAPPER_EXCEPTION));
        }

        Wrapper wrapper = (Wrapper) child;
        String wrapperName = child.getName();

        if (createRegistration) {
            ServletRegistrationImpl regis = null;
            if (isProgrammatic ||
                    (null == wrapper.getServletClassName() &&
                        null == wrapper.getJspFile())) {
                regis = createDynamicServletRegistrationImpl(
                    (StandardWrapper) wrapper);
            } else {
                regis = createServletRegistrationImpl(
                    (StandardWrapper) wrapper);
            }
            servletRegisMap.put(wrapperName, regis);
            if (null == wrapper.getServletClassName() &&
                    null == wrapper.getJspFile()) {
                /*
                 * Preliminary registration for Servlet that was declared
                 * without any servlet-class. Once the registration is
                 * completed via ServletContext#addServlet, addChild will
                 * be called again, and 'wrapper' will have been configured
                 * with a proper class name at that time
                 */
                return;
            }
        }

        if ("javax.faces.webapp.FacesServlet".equals(
                wrapper.getServletClassName())) {
            isJsfApplication = true;
        }

        // Global JspServlet
        Wrapper oldJspServlet = null;

        // Allow webapp to override JspServlet inherited from global web.xml.
        boolean isJspServlet = "jsp".equals(wrapperName);
        if (isJspServlet) {
            oldJspServlet = (Wrapper) findChild("jsp");
            if (oldJspServlet != null) {
                removeChild(oldJspServlet);
            }
        }

        String jspFile = wrapper.getJspFile();
        if ((jspFile != null) && !jspFile.startsWith("/")) {
            if (isServlet22()) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, LogFacade.JSP_FILE_FINE, jspFile);
                }
                wrapper.setJspFile("/" + jspFile);
            } else {
                String msg = MessageFormat.format(rb.getString(LogFacade.WRAPPER_ERROR_EXCEPTION), jspFile);
                throw new IllegalArgumentException(msg);
            }
        }

        super.addChild(child);

        // START SJSAS 6342808
        /* SJSWS 6362207
        if (started) {
        */
        // START SJSWS 6362207
        if (getAvailable()) {
        // END SJSWS 6362207
            /*
             * If this StandardContext has already been started, we need to
             * register the newly added child with JMX. Any children that were
             * added before this StandardContext was started have already been
             * registered with JMX (as part of StandardContext.start()).
             */
            if (wrapper instanceof StandardWrapper) {
                ((StandardWrapper) wrapper).registerJMX( this );
            }
        }
        // END SJSAS 6342808

        if (isJspServlet && oldJspServlet != null) {
            /*
             * The webapp-specific JspServlet inherits all the mappings
             * specified in the global web.xml, and may add additional ones.
             */
            String[] jspMappings = oldJspServlet.findMappings();
            for (int i=0; jspMappings!=null && i<jspMappings.length; i++) {
                addServletMapping(jspMappings[i], wrapperName);
            }
        }
    }

    protected ServletRegistrationImpl createServletRegistrationImpl(
            StandardWrapper wrapper) {
        return new ServletRegistrationImpl(wrapper, this);
    }

    protected ServletRegistrationImpl createDynamicServletRegistrationImpl(
            StandardWrapper wrapper) {
        return new DynamicServletRegistrationImpl(wrapper, this);
    }

    /**
     * Add a security constraint to the set for this web application.
     */
    @Override
    public void addConstraint(SecurityConstraint constraint) {

        // Validate the proposed constraint
        SecurityCollection collections[] = constraint.findCollections();
        for(SecurityCollection collection : collections) {
            String patterns[] = collection.findPatterns();
            for(int j = 0; j < patterns.length; j++) {
                patterns[j] = adjustURLPattern(patterns[j]);
                if(!validateURLPattern(patterns[j])) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.SECURITY_CONSTRAINT_PATTERN_EXCEPTION),
                                                      patterns[j]);
                    throw new IllegalArgumentException(msg);
                }
            }
        }

        // Add this constraint to the set for our web application
        constraints.add(constraint);
    }

    /**
     * Add an EJB resource reference for this web application.
     *
     * @param ejb New EJB resource reference
     */
    @Override
    public void addEjb(ContextEjb ejb) {
        namingResources.addEjb(ejb);
        if (notifyContainerListeners) {
           fireContainerEvent("addEjb", ejb.getName());
        }
    }

    /**
     * Add an environment entry for this web application.
     *
     * @param environment New environment entry
     */
    @Override
    public void addEnvironment(ContextEnvironment environment) {

        ContextEnvironment env = findEnvironment(environment.getName());
        if ((env != null) && !env.getOverride())
            return;
        namingResources.addEnvironment(environment);

        if (notifyContainerListeners) {
            fireContainerEvent("addEnvironment", environment.getName());
        }
    }


    /**
     * Add resource parameters for this web application.
     *
     * @param resourceParameters New resource parameters
     */
    public void addResourceParams(ResourceParams resourceParameters) {
        namingResources.addResourceParams(resourceParameters);
        if (notifyContainerListeners) {
            fireContainerEvent("addResourceParams",
                               resourceParameters.getName());
        }
    }

    /**
     * Add an error page for the specified error or Java exception.
     *
     * @param errorPage The error page definition to be added
     */
    @Override
    public void addErrorPage(ErrorPage errorPage) {
        // Validate the input parameters
        if (errorPage == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.ERROR_PAGE_REQUIRED_EXCEPTION));
        String location = errorPage.getLocation();
        if ((location != null) && !location.startsWith("/")) {
            if (isServlet22()) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, LogFacade.ERROR_PAGE_LOCATION_EXCEPTION);
                }
                errorPage.setLocation("/" + location);
            } else {
                String msg = MessageFormat.format(rb.getString(LogFacade.ERROR_PAGE_LOCATION_EXCEPTION), location);
                throw new IllegalArgumentException(msg);
            }
        }

        // Add the specified error page to our internal collections
        String exceptionType = errorPage.getExceptionType();
        if (exceptionType != null) {
            synchronized (exceptionPages) {
                exceptionPages.put(exceptionType, errorPage);
            }
        } else if (errorPage.getErrorCode() > 0) {
            synchronized (statusPages) {
                int errorCode = errorPage.getErrorCode();
                if ((errorCode >= 400) && (errorCode < 600)) {
                    statusPages.put(errorCode, errorPage);
                } else {
                    log.log(Level.SEVERE, LogFacade.INVALID_ERROR_PAGE_CODE_EXCEPTION, errorCode);
                }
            }
        } else {
            defaultErrorPage = errorPage;
        }

        if (notifyContainerListeners) {
            fireContainerEvent("addErrorPage", errorPage);
        }
    }

    /**
     * Add a filter definition to this Context.
     *
     * @param filterDef The filter definition to be added
     */
    @Override
    public void addFilterDef(FilterDef filterDef) {
        addFilterDef(filterDef, false, true);
    }

    /**
     * Add a filter definition to this Context.
     * @param filterDef The filter definition to be added
     * @param isProgrammatic
     * @param createRegistration
     */
    public void addFilterDef(FilterDef filterDef, boolean isProgrammatic,
            boolean createRegistration) {
        if (createRegistration) {
            FilterRegistrationImpl regis = null;
            if (isProgrammatic || null == filterDef.getFilterClassName()) {
                regis = new DynamicFilterRegistrationImpl(filterDef, this);
            } else {
                regis = new FilterRegistrationImpl(filterDef, this);
            }
            filterRegisMap.put(filterDef.getFilterName(), regis);
            if (null == filterDef.getFilterClassName()) {
                /*
                 * Preliminary registration for Filter that was declared
                 * without any filter-class. Once the registration is
                 * completed via ServletContext#addFilter, addFilterDef will
                 * be called again, and 'filterDef' will have been configured
                 * with a proper class name at that time
                 */
                return;
            }
        }

        synchronized (filterDefs) {
            filterDefs.put(filterDef.getFilterName(), filterDef);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("addFilterDef", filterDef);
        }
    }

    /**
     * Add multiple filter mappings to this Context.
     *
     * @param filterMaps The filter mappings to be added
     *
     * @exception IllegalArgumentException if the specified filter name
     *  does not match an existing filter definition, or the filter mapping
     *  is malformed
     */
    public void addFilterMaps(FilterMaps filterMaps) {
        String[] servletNames = filterMaps.getServletNames();
        String[] urlPatterns = filterMaps.getURLPatterns();
        for (String servletName : servletNames) {
            FilterMap fmap = new FilterMap();
            fmap.setFilterName(filterMaps.getFilterName());
            fmap.setServletName(servletName);
            fmap.setDispatcherTypes(filterMaps.getDispatcherTypes());
            addFilterMap(fmap);
        }
        for (String urlPattern : urlPatterns) {
            FilterMap fmap = new FilterMap();
            fmap.setFilterName(filterMaps.getFilterName());
            fmap.setURLPattern(urlPattern);
            fmap.setDispatcherTypes(filterMaps.getDispatcherTypes());
            addFilterMap(fmap);
        }
    }

    /**
     * Add a filter mapping to this Context.
     *
     * @param filterMap The filter mapping to be added
     *
     * @exception IllegalArgumentException if the specified filter name
     *  does not match an existing filter definition, or the filter mapping
     *  is malformed
     */
    @Override
    public void addFilterMap(FilterMap filterMap) {
        addFilterMap(filterMap, true);
    }


    /**
     * Add a filter mapping to this Context.
     *
     * @param filterMap The filter mapping to be added
     *
     * @param isMatchAfter true if the given filter mapping should be matched
     * against requests after any declared filter mappings of this servlet
     * context, and false if it is supposed to be matched before any declared
     * filter mappings of this servlet context
     *
     * @exception IllegalArgumentException if the specified filter name
     *  does not match an existing filter definition, or the filter mapping
     *  is malformed
     *
     */
    public void addFilterMap(FilterMap filterMap, boolean isMatchAfter) {

        // Validate the proposed filter mapping
        String filterName = filterMap.getFilterName();
        String servletName = filterMap.getServletName();
        String urlPattern = filterMap.getURLPattern();
        if (null == filterRegisMap.get(filterName)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.FILTER_MAPPING_NAME_EXCEPTION), filterName);
            throw new IllegalArgumentException(msg);
        }
        if ((servletName == null) && (urlPattern == null)) {
            throw new IllegalArgumentException(rb.getString(LogFacade.FILTER_MAPPING_EITHER_EXCEPTION));
        }
        if ((servletName != null) && (urlPattern != null)) {
            throw new IllegalArgumentException(rb.getString(LogFacade.FILTER_MAPPING_EITHER_EXCEPTION));
        }
        // Because filter-pattern is new in 2.3, no need to adjust
        // for 2.2 backwards compatibility
        if ((urlPattern != null) && !validateURLPattern(urlPattern)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.FILTER_MAPPING_INVALID_URL_EXCEPTION), urlPattern);
            throw new IllegalArgumentException(msg);
        }

        // Add this filter mapping to our registered set
        if (isMatchAfter) {
            filterMaps.add(filterMap);
        } else {
            filterMaps.add(0, filterMap);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("addFilterMap", filterMap);
        }
    }

    /**
     * Gets the current servlet name mappings of the Filter with
     * the given name.
     */
    public Collection<String> getServletNameFilterMappings(String filterName) {
        HashSet<String> mappings = new HashSet<String>();
        synchronized (filterMaps) {
            for (FilterMap fm : filterMaps) {
                if (filterName.equals(fm.getFilterName()) &&
                        fm.getServletName() != null) {
                    mappings.add(fm.getServletName());
                }
            }
        }
        return mappings;
    }

    /**
     * Gets the current URL pattern mappings of the Filter with the given
     * name.
     */
    public Collection<String> getUrlPatternFilterMappings(String filterName) {
        HashSet<String> mappings = new HashSet<String>();
        synchronized (filterMaps) {
            for (FilterMap fm : filterMaps) {
                if (filterName.equals(fm.getFilterName()) &&
                        fm.getURLPattern() != null) {
                    mappings.add(fm.getURLPattern());
                }
            }
        }
        return mappings;
    }

    /**
     * Adds the filter with the given name and class name to this servlet
     * context.
     */
    public FilterRegistration.Dynamic addFilter(
            String filterName, String className) {

        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                                              new Object[] {"addFilter", getName()});
            throw new IllegalStateException(msg);
        }

        if (filterName == null || filterName.length() == 0) {
            throw new IllegalArgumentException(rb.getString(LogFacade.NULL_EMPTY_FILTER_NAME_EXCEPTION));
        }

        synchronized (filterDefs) {
            // Make sure filter name is unique for this context
            if (findFilterDef(filterName) != null) {
                return null;
            }

            DynamicFilterRegistrationImpl regis =
                (DynamicFilterRegistrationImpl) filterRegisMap.get(
                    filterName);
            FilterDef filterDef = null;
            if (null == regis) {
                filterDef = new FilterDef();
            } else {
                // Complete preliminary filter registration
                filterDef = regis.getFilterDefinition();
            }

            filterDef.setFilterName(filterName);
            filterDef.setFilterClassName(className);

            addFilterDef(filterDef, true, (regis == null));
            if (null == regis) {
                regis = (DynamicFilterRegistrationImpl)
                    filterRegisMap.get(filterName);
            }

            return regis;
        }
    }

    /*
     * Registers the given filter instance with this ServletContext
     * under the given <tt>filterName</tt>.
     */
    @Override
    public FilterRegistration.Dynamic addFilter(
            String filterName, Filter filter) {

        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                                              new Object[] {"addFilter", getName()});
            throw new IllegalStateException(msg);
        }

        if (filterName == null || filterName.length() == 0) {
            throw new IllegalArgumentException(rb.getString(LogFacade.NULL_EMPTY_FILTER_NAME_EXCEPTION));
        }

        if (filter == null) {
            throw new IllegalArgumentException(rb.getString(LogFacade.NULL_FILTER_INSTANCE_EXCEPTION));
        }

        /*
         * Make sure the given Filter instance is unique across all deployed
         * contexts
         */
        Container host = getParent();
        if (host != null) {
            for (Container child : host.findChildren()) {
                if (child == this) {
                    // Our own context will be checked further down
                    continue;
                }
                if (((StandardContext) child).hasFilter(filter)) {
                    return null;
                }
            }
        }

        /*
         * Make sure the given Filter name and instance are unique within
         * this context
         */
        synchronized (filterDefs) {
            for (Map.Entry<String, FilterDef> e : filterDefs.entrySet()) {
                if (filterName.equals(e.getKey()) ||
                        filter == e.getValue().getFilter()) {
                    return null;
                }
            }

            DynamicFilterRegistrationImpl regis =
                (DynamicFilterRegistrationImpl) filterRegisMap.get(
                    filterName);
            FilterDef filterDef = null;
            if (null == regis) {
                filterDef = new FilterDef();
            } else {
                // Complete preliminary filter registration
                filterDef = regis.getFilterDefinition();
            }

            filterDef.setFilterName(filterName);
            filterDef.setFilter(filter);

            addFilterDef(filterDef, true, (regis == null));
            if (null == regis) {
                regis = (DynamicFilterRegistrationImpl)
                    filterRegisMap.get(filterName);
            }

            return regis;
        }
    }

    /**
     * Checks whether this context contains the given Filter instance
     */
    public boolean hasFilter(Filter filter) {
        for (Map.Entry<String, FilterDef> e : filterDefs.entrySet()) {
            if (filter == e.getValue().getFilter()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the filter with the given name and class type to this servlet
     * context.
     */
    @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            Class <? extends Filter> filterClass) {

        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                                             new Object[] {"addFilter", getName()});
            throw new IllegalStateException(msg);
        }

        if (filterName == null || filterName.length() == 0) {
            throw new IllegalArgumentException(rb.getString(LogFacade.NULL_EMPTY_FILTER_NAME_EXCEPTION));
        }

        synchronized (filterDefs) {
            if (findFilterDef(filterName) != null) {
                return null;
            }

            DynamicFilterRegistrationImpl regis =
                (DynamicFilterRegistrationImpl) filterRegisMap.get(
                    filterName);
            FilterDef filterDef = null;
            if (null == regis) {
                filterDef = new FilterDef();
            } else {
                // Complete preliminary filter registration
                filterDef = regis.getFilterDefinition();
            }

            filterDef.setFilterName(filterName);
            filterDef.setFilterClass(filterClass);

            addFilterDef(filterDef, true, (regis == null));
            if (null == regis) {
                regis = (DynamicFilterRegistrationImpl)
                    filterRegisMap.get(filterName);
            }

            return regis;
        }
    }

    /**
     * Instantiates the given Filter class and performs any required
     * resource injection into the new Filter instance before returning
     * it.
     */
    @Override
    public <T extends Filter> T createFilter(Class<T> clazz)
            throws ServletException {
        try {
            return createFilterInstance(clazz);
        } catch (Throwable t) {
            throw new ServletException("Unable to create Filter from " +
                                       "class " + clazz.getName(), t);
        }
    }

    /**
     * Gets the FilterRegistration corresponding to the filter with the
     * given <tt>filterName</tt>.
     */
    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return filterRegisMap.get(filterName);
    }

    /**
     * Gets a Map of the FilterRegistration objects corresponding to all
     * currently registered filters.
     */
    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Collections.unmodifiableMap(filterRegisMap);
    }

    /**
     * Gets the session tracking cookie configuration of this
     * <tt>ServletContext</tt>.
     */
    @Override
    public synchronized SessionCookieConfig getSessionCookieConfig() {
        if (sessionCookieConfig == null) {
            sessionCookieConfig = new SessionCookieConfigImpl(this);
        }
        return sessionCookieConfig;
    }

    /**
     * Sets the name that will be assigned to any session tracking
     * cookies created on behalf of this context
     */
    void setSessionCookieName(String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
        sessionCookieNameInitialized = true;
    }

    /**
     * Gets the name that will be assigned to any session tracking
     * cookies created on behalf of this context
     */
    @Override
    public String getSessionCookieName() {
        return sessionCookieName;
    }

    /**
     * @return the name that will be assigned to any session tracking
     * parameter created on behalf of this context
     */
    @Override
    public String getSessionParameterName() {
        if (sessionCookieNameInitialized) {
            if (sessionCookieName != null && (!sessionCookieName.isEmpty())) {
               return sessionCookieName;
            }
        }

        return Globals.SESSION_PARAMETER_NAME;
    }

    /**
     * Sets the session tracking modes that are to become effective for this
     * <tt>ServletContext</tt>.
     */
    @Override
    public void setSessionTrackingModes(
            Set<SessionTrackingMode> sessionTrackingModes) {

        if (sessionTrackingModes.contains(SessionTrackingMode.SSL)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.UNSUPPORTED_TRACKING_MODE_EXCEPTION),
                                              new Object[] {SessionTrackingMode.SSL, getName()});
            throw new IllegalArgumentException(msg);
        }

        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                                              new Object[] {"setSessionTrackingModes", getName()});
            throw new IllegalStateException(msg);
        }

        this.sessionTrackingModes =
            Collections.unmodifiableSet(sessionTrackingModes);

        if (sessionTrackingModes.contains(SessionTrackingMode.COOKIE)) {
            setCookies(true);
        } else {
            setCookies(false);
        }

        if (sessionTrackingModes.contains(SessionTrackingMode.URL)) {
            setEnableURLRewriting(true);
        } else {
            setEnableURLRewriting(false);
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
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return EnumSet.copyOf(DEFAULT_SESSION_TRACKING_MODES);
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
        return sessionTrackingModes != null ?
            new HashSet<>(sessionTrackingModes) :
            getDefaultSessionTrackingModes();
    }

    /**
     * Adds the listener with the given class name to this ServletContext.
     *
     * @param className the fully qualified class name of the listener
     */
    @Override
    public void addListener(String className) {
        addListener(className, true);
    }


    /**
     * Adds the listener with the given class name to this ServletContext.
     *
     * @param className the fully qualified class name of the listener
     * @param isProgrammatic true if the listener is being added
     * programmatically, and false if it has been declared in the deployment
     * descriptor
     */
    private void addListener(String className, boolean isProgrammatic) {
        EventListener listener = null;
        try {
            listener = loadListener(getClassLoader(), className);
        } catch(Throwable t) {
            throw new IllegalArgumentException(t);
        }

        addListener(listener, isProgrammatic);
    }

    /**
     * Adds the given listener instance to this ServletContext.
     *
     * @param t the listener to be added
     */
    @Override
    public <T extends EventListener> void addListener(T t) {
        addListener(t, true);
    }

    /**
     * Adds the given listener instance to this ServletContext.
     *
     * @param t the listener to be added
     * @param isProgrammatic true if the listener is being added
     * programmatically, and false if it has been declared in the deployment
     * descriptor
     */
    private <T extends EventListener> void addListener(T t,
            boolean isProgrammatic) {
        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                                              new Object[] {"addListener", getName()});
            throw new IllegalStateException(msg);
        }

        if ((t instanceof ServletContextListener) && isProgrammatic &&
                !isProgrammaticServletContextListenerRegistrationAllowed) {
            throw new IllegalArgumentException("Not allowed to register " +
                "ServletContextListener programmatically");
        }

        boolean added = false;

        if (t instanceof ServletContextAttributeListener ||
                t instanceof ServletRequestAttributeListener ||
                t instanceof ServletRequestListener ||
                t instanceof HttpSessionAttributeListener ||
                t instanceof HttpSessionIdListener) {
            eventListeners.add(t);
            added = true;
        }

        if (t instanceof HttpSessionListener) {
            sessionListeners.add((HttpSessionListener) t);
            if (!added) {
                added = true;
            }
        }

        if (t instanceof ServletContextListener) {
            ServletContextListener proxy = (ServletContextListener) t;
            if (isProgrammatic) {
                proxy = new RestrictedServletContextListener(
                    (ServletContextListener) t);
            }
            // Always add the JSF listener as the first element,
            // see GlassFish Issue 2563 for details
            boolean isFirst =
                "com.sun.faces.config.ConfigureListener".equals(
                    t.getClass().getName());
            if (isFirst) {
                contextListeners.add(0, proxy);
            } else {
                contextListeners.add(proxy);
            }
            if (!added) {
                added = true;
            }
        }

        if (!added) {
            throw new IllegalArgumentException("Invalid listener type " +
                t.getClass().getName());
        }
    }

    /**
     * Adds a listener of the given class type to this ServletContext.
     */
    @Override
    public void addListener(Class <? extends EventListener> listenerClass) {
        EventListener listener = null;
        try {
            listener = createListenerInstance(listenerClass);
        } catch(Throwable t) {
             throw new IllegalArgumentException(t);
        }

        addListener(listener);
    }

    /**
     * Instantiates the given EventListener class and performs any
     * required resource injection into the new EventListener instance
     * before returning it.
     */
    @Override
    public <T extends EventListener> T createListener(Class<T> clazz)
            throws ServletException {
        if (!ServletContextListener.class.isAssignableFrom(clazz) &&
                !ServletContextAttributeListener.class.isAssignableFrom(clazz) &&
                !ServletRequestListener.class.isAssignableFrom(clazz) &&
                !ServletRequestAttributeListener.class.isAssignableFrom(clazz) &&
                !HttpSessionAttributeListener.class.isAssignableFrom(clazz) &&
                !HttpSessionIdListener.class.isAssignableFrom(clazz) &&
                !HttpSessionListener.class.isAssignableFrom(clazz)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.UNABLE_ADD_LISTENER_EXCEPTION),
                                              new Object[] {clazz.getName()});
            throw new IllegalArgumentException(msg);
        }

        try {
            return createListenerInstance(clazz);
        } catch (Throwable t) {
            throw new ServletException(t);
        }
    }

    public void setJspConfigDescriptor(JspConfigDescriptor jspConfigDesc) {
        this.jspConfigDesc = jspConfigDesc;
    }

    /**
     * Gets the <code>&lt;jsp-config&gt;</code> related configuration
     * that was aggregated over the <code>web.xml</code> and
     * <code>web-fragment.xml</code> resources of the web application
     * represented by this ServletContext.
     */
    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return jspConfigDesc;
    }

    /**
     * Gets the class loader of the web application represented by this
     * ServletContext.
     */
    @Override
    public ClassLoader getClassLoader() {
        ClassLoader webappLoader = (getLoader() != null) ?
            getLoader().getClassLoader() : null;
        if (webappLoader == null) {
            return null;
        }
        if (mySecurityManager != null) {
            mySecurityManager.checkGetClassLoaderPermission(webappLoader);
        }
        return webappLoader;
    }

    @Override
    public void declareRoles(String... roleNames) {
        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                                              new Object[] {"declareRoles", getName()});
            throw new IllegalStateException(msg);
        }

        for (String roleName : roleNames) {
            addSecurityRole(roleName);
        }
    }

    public void setEffectiveMajorVersion(int effectiveMajorVersion) {
        this.effectiveMajorVersion = effectiveMajorVersion;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return effectiveMajorVersion;
    }

    public void setEffectiveMinorVersion(int effectiveMinorVersion) {
        this.effectiveMinorVersion = effectiveMinorVersion;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return effectiveMinorVersion;
    }

    @Override
    public String getVirtualServerName() {
        String virtualServerName = null;
        Container parent = getParent();
        if (parent != null) {
            virtualServerName = parent.getName();
        }
        return virtualServerName;
    }

    /**
     * Add the classname of an InstanceListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of an InstanceListener class
     */
    @Override
    public void addInstanceListener(String listener) {
        instanceListeners.add(listener);
        if (notifyContainerListeners) {
            fireContainerEvent("addInstanceListener", listener);
        }
    }

    public void addInstanceListener(InstanceListener listener) {
        instanceListenerInstances.add(listener);
        if (notifyContainerListeners) {
            fireContainerEvent("addInstanceListener", listener);
        }
    }

    /**
     * Add the given URL pattern as a jsp-property-group.  This maps
     * resources that match the given pattern so they will be passed
     * to the JSP container.  Though there are other elements in the
     * property group, we only care about the URL pattern here.  The
     * JSP container will parse the rest.
     *
     * @param pattern URL pattern to be mapped
     */
    @Override
    public void addJspMapping(String pattern) {
        String servletName = findServletMapping("*.jsp");
        if (servletName == null) {
            servletName = "jsp";
        }

        if( findChild(servletName) != null) {
            addServletMapping(pattern, servletName, true);
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Skipping " + pattern + " , no servlet "
                        + servletName);
            }
        }
    }

    /**
     * Add a Locale Encoding Mapping (see Sec 5.4 of Servlet spec 2.4)
     *
     * @param locale locale to map an encoding for
     * @param encoding encoding to be used for a give locale
     */
    @Override
    public void addLocaleEncodingMappingParameter(String locale, String encoding){
        getCharsetMapper().addCharsetMappingFromDeploymentDescriptor(locale, encoding);
    }

    /**
     * Add a local EJB resource reference for this web application.
     *
     * @param ejb New EJB resource reference
     */
    @Override
    public void addLocalEjb(ContextLocalEjb ejb) {
        namingResources.addLocalEjb(ejb);
        if (notifyContainerListeners) {
            fireContainerEvent("addLocalEjb", ejb.getName());
        }
    }

    /**
     * Add a message destination for this web application.
     *
     * @param md New message destination
     */
    public void addMessageDestination(MessageDestination md) {
        synchronized (messageDestinations) {
            messageDestinations.put(md.getName(), md);
        }
        if (notifyContainerListeners) {
            fireContainerEvent("addMessageDestination", md.getName());
        }
    }

    /**
     * Add a message destination reference for this web application.
     *
     * @param mdr New message destination reference
     */
    public void addMessageDestinationRef(MessageDestinationRef mdr) {
        namingResources.addMessageDestinationRef(mdr);
        if (notifyContainerListeners) {
            fireContainerEvent("addMessageDestinationRef", mdr.getName());
        }
    }

    /**
     * Add a new MIME mapping, replacing any existing mapping for
     * the specified extension.
     *
     * @param extension Filename extension being mapped
     * @param mimeType Corresponding MIME type
     */
    public void addMimeMapping(String extension, String mimeType) {
        mimeMappings.put(extension.toLowerCase(Locale.ENGLISH), mimeType);
        if (notifyContainerListeners) {
            fireContainerEvent("addMimeMapping", extension);
        }
    }

    /**
     * Add a new context initialization parameter.
     *
     * @param name Name of the new parameter
     * @param value Value of the new  parameter
     *
     * @exception IllegalArgumentException if the name or value is missing,
     *  or if this context initialization parameter has already been
     *  registered
     */
    public void addParameter(String name, String value) {
        // Validate the proposed context initialization parameter
        if ((name == null) || (value == null)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.PARAMETER_REQUIRED_EXCEPTION), name);
            throw new IllegalArgumentException(msg);
        }
        if (parameters.get(name) != null) {
            String msg = MessageFormat.format(rb.getString(LogFacade.DUPLICATE_PARAMETER_EXCEPTION), name);
            throw new IllegalArgumentException(msg);
        }

        // Add this parameter to our defined set
        synchronized (parameters) {
            parameters.put(name, value);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("addParameter", name);
        }
    }


    /**
     * Add a resource reference for this web application.
     *
     * @param resource New resource reference
     */
    public void addResource(ContextResource resource) {
        namingResources.addResource(resource);
        if (notifyContainerListeners) {
            fireContainerEvent("addResource", resource.getName());
        }
    }


    /**
     * Add a resource environment reference for this web application.
     *
     * @param name The resource environment reference name
     * @param type The resource environment reference type
     */
    public void addResourceEnvRef(String name, String type) {
        namingResources.addResourceEnvRef(name, type);
        if (notifyContainerListeners) {
            fireContainerEvent("addResourceEnvRef", name);
        }
    }

    /**
     * Add a resource link for this web application.
     *
     * @param resourceLink New resource link
     */
    public void addResourceLink(ContextResourceLink resourceLink) {
        namingResources.addResourceLink(resourceLink);
        if (notifyContainerListeners) {
            fireContainerEvent("addResourceLink", resourceLink.getName());
        }
    }


    /**
     * Add a security role reference for this web application.
     *
     * @param role Security role used in the application
     * @param link Actual security role to check for
     */
    public void addRoleMapping(String role, String link) {
        synchronized (roleMappings) {
            roleMappings.put(role, link);
        }
        if (notifyContainerListeners) {
            fireContainerEvent("addRoleMapping", role);
        }
    }

    /**
     * Add a new security role for this web application.
     *
     * @param role New security role
     */
    public void addSecurityRole(String role) {
        securityRoles.add(role);
        if (notifyContainerListeners) {
            fireContainerEvent("addSecurityRole", role);
        }
    }

    /**
     * Adds the given servlet mappings to this Context.
     *
     * <p>If any of the specified URL patterns are already mapped to a
     * different Servlet, no updates will be performed.
     *
     * @param servletMap the Servlet mappings containing the Servlet name
     * and URL patterns
     *
     * @return the (possibly empty) Set of URL patterns that are already
     * mapped to a different Servlet
     *
     * @exception IllegalArgumentException if the specified servlet name
     * is not known to this Context
     */
     public Set<String> addServletMapping(ServletMap servletMap) {
         return addServletMapping(servletMap.getServletName(),
                                  servletMap.getURLPatterns());
     }

    /**
     * Adds the given servlet mappings to this Context.
     *
     * <p>If any of the specified URL patterns are already mapped to a
     * different Servlet, no updates will be performed.
     *
     * @param name the Servlet name
     * @param urlPatterns the URL patterns
     *
     * @return the (possibly empty) Set of URL patterns that are already
     * mapped to a different Servlet
     *
     * @exception IllegalArgumentException if the specified servlet name
     * is not known to this Context
     */
    public Set<String> addServletMapping(String name,
                                         String[] urlPatterns) {
        Set<String> conflicts = null;

        synchronized (servletMappings) {
            for (String pattern : urlPatterns) {
                pattern = adjustURLPattern(RequestUtil.urlDecode(pattern));
                if (!validateURLPattern(pattern)) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_MAPPING_INVALID_URL_EXCEPTION), pattern);
                    throw new IllegalArgumentException(msg);
                }

                // Ignore any conflicts with the container provided
                // Default- and JspServlet
                String existing = servletMappings.get(pattern);
                if (existing != null &&
                        !existing.equals(Constants.DEFAULT_SERVLET_NAME) &&
                        !existing.equals(Constants.JSP_SERVLET_NAME) &&
                        !name.equals(Constants.DEFAULT_SERVLET_NAME) &&
                        !name.equals(Constants.JSP_SERVLET_NAME)) {
                    if (conflicts == null) {
                        conflicts = new HashSet<String>();
                    }
                    conflicts.add(pattern);
                }
            }

            if (conflicts == null) {
                for (String urlPattern : urlPatterns) {
                    addServletMapping(urlPattern, name, false);
                }
                return Collections.emptySet();
            } else {
                return conflicts;
            }
        }
    }

    /**
     * Adds the given servlet mapping to this Context, overriding any
     * existing mapping for the specified pattern.
     *
     * @param pattern URL pattern to be mapped
     * @param name Name of the corresponding servlet to execute
     *
     * @exception IllegalArgumentException if the specified servlet name
     *  is not known to this Context
     */
    @Override
    public void addServletMapping(String pattern, String name) {
        addServletMapping(pattern, name, false);
    }

    /**
     * Adds the given servlet mapping to this Context, overriding any
     * existing mapping for the specified pattern.
     *
     * @param pattern URL pattern to be mapped
     * @param name Name of the corresponding servlet to execute
     * @param jspWildCard true if name identifies the JspServlet
     * and pattern contains a wildcard; false otherwise
     *
     * @exception IllegalArgumentException if the specified servlet name
     * is not known to this Context
     */
    public void addServletMapping(String pattern, String name,
                                  boolean jspWildCard) {
        // Validate the proposed mapping
        ServletRegistrationImpl regis = servletRegisMap.get(name);
        if (null == regis) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_MAPPING_UNKNOWN_NAME_EXCEPTION), name);
            throw new IllegalArgumentException(msg);
        }

        pattern = adjustURLPattern(RequestUtil.urlDecode(pattern));
        if (!validateURLPattern(pattern)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_MAPPING_INVALID_URL_EXCEPTION), pattern);
            throw new IllegalArgumentException(msg);
        }

        /*
         * Add this mapping to our registered set. Make sure that it is
         * possible to override the mappings of the container provided
         * Default- and JspServlet, and that these servlets are prevented
         * from overriding any user-defined mappings (depending on the order
         * in which the contents of the default-web.xml are merged with those
         * of the app's deployment descriptor).
         * This is to prevent the DefaultServlet from hijacking '/', and the
         * JspServlet from hijacking *.jsp(x).
         */
        synchronized (servletMappings) {
            String existing = servletMappings.get(pattern);
            if (existing != null) {
                if (!existing.equals(Constants.DEFAULT_SERVLET_NAME) &&
                        !existing.equals(Constants.JSP_SERVLET_NAME) &&
                        !name.equals(Constants.DEFAULT_SERVLET_NAME) &&
                        !name.equals(Constants.JSP_SERVLET_NAME)) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.DUPLICATE_SERVLET_MAPPING_EXCEPTION),
                                                      new Object[] {name, pattern, existing});
                    throw new IllegalArgumentException(msg);
                }
                if (existing.equals(Constants.DEFAULT_SERVLET_NAME) ||
                        existing.equals(Constants.JSP_SERVLET_NAME)) {
                    // Override the mapping of the container provided
                    // Default- or JspServlet
                    Wrapper wrapper = (Wrapper) findChild(existing);
                    removePatternFromServlet(wrapper, pattern);
                    mapper.removeWrapper(pattern);
                    servletMappings.put(pattern, name);
                }
            } else {
                servletMappings.put(pattern, name);
            }
        }

        Wrapper wrapper = regis.getWrapper();
        wrapper.addMapping(pattern);

        // Update context mapper
        mapper.addWrapper(pattern, wrapper, jspWildCard, true);

        if (notifyContainerListeners) {
            fireContainerEvent("addServletMapping", pattern);
        }
    }

    /*
     * Adds the servlet with the given name and class name to this servlet
     * context.
     */
    @Override
    public ServletRegistration.Dynamic addServlet(
            String servletName, String className) {

        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                                                      new Object[] {"addServlet", getName()});
            throw new IllegalStateException(msg);
        }

        if (servletName == null || servletName.length() == 0) {
            throw new IllegalArgumentException(rb.getString(LogFacade.NULL_EMPTY_SERVLET_NAME_EXCEPTION));
        }

        synchronized (children) {
            if (findChild(servletName) == null) {
                DynamicServletRegistrationImpl regis =
                    (DynamicServletRegistrationImpl)
                        servletRegisMap.get(servletName);
                Wrapper wrapper = null;
                if (regis == null) {
                    wrapper = createWrapper();
                    wrapper.setServletClassName(className);
                } else {
                    // Complete preliminary servlet registration
                    wrapper = regis.getWrapper();
                    regis.setServletClassName(className);
                }
                wrapper.setName(servletName);
                addChild(wrapper, true, (null == regis));
                if (null == regis) {
                    regis = (DynamicServletRegistrationImpl)
                        servletRegisMap.get(servletName);
                }
                return regis;
            } else {
                return null;
            }
        }
    }

    /*
     * Registers the given servlet instance with this ServletContext
     * under the given <tt>servletName</tt>.
     */
    @Override
    public ServletRegistration.Dynamic addServlet(
            String servletName, Servlet servlet) {
        return addServlet(servletName, servlet, null, null);
    }

    /*
     * Adds the servlet with the given name and class type to this servlet
     * context.
     */
    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Class <? extends Servlet> servletClass) {

        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                                              new Object[] {"addServlet", getName()});
            throw new IllegalStateException(msg);
        }

        if (servletName == null || servletName.length() == 0) {
            throw new IllegalArgumentException(rb.getString(LogFacade.NULL_EMPTY_SERVLET_NAME_EXCEPTION));
        }

        // Make sure servlet name is unique for this context
        synchronized (children) {
            if (findChild(servletName) == null) {
                DynamicServletRegistrationImpl regis =
                    (DynamicServletRegistrationImpl)
                        servletRegisMap.get(servletName);
                Wrapper wrapper = null;
                if (regis == null) {
                    wrapper = createWrapper();
                    wrapper.setServletClass(servletClass);
                } else {
                    // Complete preliminary servlet registration
                    wrapper = regis.getWrapper();
                    regis.setServletClass(servletClass);
                }
                wrapper.setName(servletName);
                addChild(wrapper, true, (null == regis));
                if (null == regis) {
                    regis = (DynamicServletRegistrationImpl)
                        servletRegisMap.get(servletName);
                }
                return regis;
            } else {
                return null;
            }
        }
    }

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
     * @throws ServletException if the servlet fails to be initialized
     */
    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Servlet instance, Map<String, String> initParams) {
        return addServlet(servletName, instance, initParams, null);
    }

    /**
     * Adds the given servlet instance with the given name and URL patterns
     * to this servlet context, and initializes it.
     *
     * @param servletName the servlet name
     * @param servlet the servlet instance
     * @param initParams Map containing the initialization parameters for
     * the servlet
     * @param urlPatterns the URL patterns that will be mapped to the servlet
     *
     * @return the ServletRegistration through which the servlet may be
     * further configured
     */
    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Servlet servlet, Map<String, String> initParams,
            String... urlPatterns) {

        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                   new Object[] {"addServlet", getName()});
            throw new IllegalStateException(msg);
        }

        if (servletName == null || servletName.length() == 0) {
            throw new IllegalArgumentException(rb.getString(LogFacade.NULL_EMPTY_SERVLET_NAME_EXCEPTION));
        }

        if (servlet == null) {
            throw new NullPointerException(rb.getString(LogFacade.NULL_SERVLET_INSTANCE_EXCEPTION));
        }

        if (servlet instanceof SingleThreadModel) {
            throw new IllegalArgumentException("Servlet implements " +
                SingleThreadModel.class.getName());
        }

        /*
         * Make sure the given Servlet instance is unique across all deployed
         * contexts
         */
        Container host = getParent();
        if (host != null) {
            for (Container child : host.findChildren()) {
                if (child == this) {
                    // Our own context will be checked further down
                    continue;
                }
                if (((StandardContext) child).hasServlet(servlet)) {
                    return null;
                }
            }
        }

        /*
         * Make sure the given Servlet name and instance are unique within
         * this context
         */
        synchronized (children) {
            for (Map.Entry<String, Container> e : children.entrySet()) {
                if (servletName.equals(e.getKey()) ||
                        servlet == ((StandardWrapper)e.getValue()).getServlet()) {
                    return null;
                }
            }

            DynamicServletRegistrationImpl regis =
                (DynamicServletRegistrationImpl)
                    servletRegisMap.get(servletName);
            StandardWrapper wrapper = null;
            if (regis == null) {
                wrapper = (StandardWrapper) createWrapper();
            } else {
                // Complete preliminary servlet registration
                wrapper = regis.getWrapper();
            }

            wrapper.setName(servletName);
            wrapper.setServlet(servlet);
            if (initParams != null) {
                for (Map.Entry<String, String> e : initParams.entrySet()) {
                    wrapper.addInitParameter(e.getKey(), e.getValue());
                }
            }

            addChild(wrapper, true, (null == regis));
            if (null == regis) {
                regis = (DynamicServletRegistrationImpl)
                    servletRegisMap.get(servletName);
            }

            if (urlPatterns != null) {
                for (String urlPattern : urlPatterns) {
                    addServletMapping(urlPattern, servletName, false);
                }
            }

            return regis;
        }
    }

    /*
     * Adds the servlet with the given name and jsp file to this servlet
     * context.
     */
    /*
     * Adds the servlet with the given name and jsp file to this servlet
     * context.
     */
    @Override
	public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {

		if (isContextInitializedCalled) {
			String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
			        new Object[] { "addJspFile", getName() });
			throw new IllegalStateException(msg);
		}

		if (servletName == null || servletName.length() == 0) {
			throw new IllegalArgumentException(rb.getString(LogFacade.NULL_EMPTY_SERVLET_NAME_EXCEPTION));
		}

		synchronized (children) {
			if (findChild(servletName) == null) {
				DynamicServletRegistrationImpl regis = (DynamicServletRegistrationImpl) servletRegisMap.get(servletName);
				Wrapper wrapper = null;
				if (regis == null) {
					wrapper = createWrapper();
				} else {
					// Override an existing registration
					wrapper = regis.getWrapper();
				}
				wrapper.setJspFile(jspFile);
				wrapper.setName(servletName);
				addChild(wrapper, true, (null == regis));
				if (null == regis) {
					regis = (DynamicServletRegistrationImpl) servletRegisMap.get(servletName);
				}
				return regis;
			} else {
				return null;
			}
		}
	}

    /**
     * This method is overridden in web-glue to also remove the given
     * mapping from the deployment backend's WebBundleDescriptor.
     */
    protected void removePatternFromServlet(Wrapper wrapper, String pattern) {
        wrapper.removeMapping(pattern);
    }

    /**
     * Checks whether this context contains the given Servlet instance
     * @param servlet
     * @return
     */
    public boolean hasServlet(Servlet servlet) {
        for (Map.Entry<String, Container> e : children.entrySet()) {
            if (servlet == ((StandardWrapper)e.getValue()).getServlet()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Instantiates the given Servlet class and performs any required
     * resource injection into the new Servlet instance before returning
     * it.
     */
    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz)
            throws ServletException {
        try {
            return createServletInstance(clazz);
        } catch (Throwable t) {
            throw new ServletException("Unable to create Servlet from " +
                                       "class " + clazz.getName(), t);
        }
    }

    /**
     * Gets the ServletRegistration corresponding to the servlet with the
     * given <tt>servletName</tt>.
     * @param servletName
     */
    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return servletRegisMap.get(servletName);
    }

    /**
     * Gets a Map of the ServletRegistration objects corresponding to all
     * currently registered servlets.
     */
    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Collections.unmodifiableMap(servletRegisMap);
    }

    /**
     * Add a new watched resource to the set recognized by this Context.
     *
     * @param name New watched resource file name
     */
    @Override
    public void addWatchedResource(String name) {
        watchedResources.add(name);
        fireContainerEvent("addWatchedResource", name);
    }

    /**
     * Add a new welcome file to the set recognized by this Context.
     *
     * @param name New welcome file name
     */
    @Override
    public void addWelcomeFile(String name) {

        // Welcome files from the application deployment descriptor
        // completely replace those from the default conf/web.xml file
        if (replaceWelcomeFiles) {
            welcomeFiles = new String[0];
            setReplaceWelcomeFiles(false);
        }
        String results[] = new String[welcomeFiles.length + 1];
        for (int i = 0; i < welcomeFiles.length; i++)
            results[i] = welcomeFiles[i];
        results[welcomeFiles.length] = name;
        welcomeFiles = results;

        if (notifyContainerListeners) {
            fireContainerEvent("addWelcomeFile", name);
        }
    }

    /**
     * Add the classname of a LifecycleListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a LifecycleListener class
     */
    @Override
    public void addWrapperLifecycle(String listener) {
        wrapperLifecycles.add(listener);
        if (notifyContainerListeners) {
            fireContainerEvent("addWrapperLifecycle", listener);
        }
    }

    /**
     * Add the classname of a ContainerListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a ContainerListener class
     */
    @Override
    public void addWrapperListener(String listener) {
        wrapperListeners.add(listener);
        if (notifyContainerListeners) {
            fireContainerEvent("addWrapperListener", listener);
        }
    }

    /**
     * Factory method to create and return a new Wrapper instance, of
     * the Java implementation class appropriate for this Context
     * implementation.  The constructor of the instantiated Wrapper
     * will have been called, but no properties will have been set.
     * @return
     */
    @Override
    public Wrapper createWrapper() {
        Wrapper wrapper = new StandardWrapper();

        synchronized (instanceListeners) {
            for (String instanceListener : instanceListeners) {
                try {
                    Class clazz = Class.forName(instanceListener);
                    wrapper.addInstanceListener((InstanceListener)clazz.newInstance());
                } catch(Throwable t) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.CREATING_INSTANCE_LISTENER_EXCEPTION),
                                                      instanceListener);
                    log.log(Level.SEVERE, msg, t);
                    return (null);
                }
            }
        }

        synchronized (instanceListenerInstances) {
            for(InstanceListener instanceListenerInstance : instanceListenerInstances) {
                wrapper.addInstanceListener(instanceListenerInstance);
            }
        }

        Iterator<String> i = wrapperLifecycles.iterator();
        while (i.hasNext()) {
            String wrapperLifecycle = i.next();
            try {
                Class clazz = Class.forName(wrapperLifecycle);
                if(wrapper instanceof Lifecycle) {
                    ((Lifecycle)wrapper).addLifecycleListener(
                        (LifecycleListener)clazz.newInstance());
                }
            } catch(Throwable t) {
                String msg = MessageFormat.format(rb.getString(LogFacade.CREATING_LIFECYCLE_LISTENER_EXCEPTION),
                                                  wrapperLifecycle);
                log.log(Level.SEVERE, msg, t);
                return (null);
            }
        }

        i = wrapperListeners.iterator();
        while (i.hasNext()) {
            String wrapperListener = i.next();
            try {
                Class clazz = Class.forName(wrapperListener);
                wrapper.addContainerListener((ContainerListener)
                    clazz.newInstance());
            } catch(Throwable t) {
                String msg = MessageFormat.format(rb.getString(LogFacade.CREATING_CONTAINER_LISTENER_EXCEPTION),
                                                  wrapperListener);
                log.log(Level.SEVERE, msg, t);
                return (null);
            }
        }

        return (wrapper);
    }

    /**
     * Return the set of application parameters for this application.
     * @return
     */
    @Override
    public List<ApplicationParameter> findApplicationParameters() {
        return applicationParameters;
    }

    /**
     * Gets the security constraints defined for this web application.
     * @return
     */
    @Override
    public List<SecurityConstraint> getConstraints() {
        return constraints;
    }

    /**
     * Checks whether this web application has any security constraints
     * defined.
     * @return
     */
    @Override
    public boolean hasConstraints() {
        return !constraints.isEmpty();
    }

    /**
     * Return the EJB resource reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     * @return
     */
    @Override
    public ContextEjb findEjb(String name) {
        return namingResources.findEjb(name);
    }

    /**
     * Return the defined EJB resource references for this application.
     * If there are none, a zero-length array is returned.
     * @return
     */
    @Override
    public ContextEjb[] findEjbs() {
        return namingResources.findEjbs();
    }

    /**
     * Return the environment entry with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired environment entry
     * @return
     */
    @Override
    public ContextEnvironment findEnvironment(String name) {
        return namingResources.findEnvironment(name);
    }

    /**
     * Return the set of defined environment entries for this web
     * application.  If none have been defined, a zero-length array
     * is returned.
     * @return
     */
    @Override
    public ContextEnvironment[] findEnvironments() {
        return namingResources.findEnvironments();
    }

    /**
     * Return the error page entry for the specified HTTP error code,
     * if any; otherwise return <code>null</code>.
     *
     * @param errorCode Error code to look up
     * @return
     */
    @Override
    public ErrorPage findErrorPage(int errorCode) {
        if ((errorCode >= 400) && (errorCode < 600)) {
            return statusPages.get(errorCode);
        }

        return null;
    }

    /**
     * Return the error page entry for the specified Java exception type,
     * if any; otherwise return <code>null</code>.
     *
     * @param exceptionType Exception type to look up
     * @return
     */
    @Override
    public ErrorPage findErrorPage(String exceptionType) {
        synchronized (exceptionPages) {
            return exceptionPages.get(exceptionType);
        }
    }

    /**
     * Gets the default error page of this context.
     *
     * <p>A default error page is an error page that was declared without
     * any exception-type and error-code.
     *
     * @return the default error page of this context, or null if this
     * context does not have any default error page
     */
    @Override
    public ErrorPage getDefaultErrorPage() {
        return defaultErrorPage;
    }

    /**
     * Return the filter definition for the specified filter name, if any;
     * otherwise return <code>null</code>.
     *
     * @param filterName Filter name to look up
     * @return
     */
    @Override
    public FilterDef findFilterDef(String filterName) {
        synchronized (filterDefs) {
            return filterDefs.get(filterName);
        }
    }

    /**
     * Return the set of defined filters for this Context.
     */
    @Override
    public FilterDef[] findFilterDefs() {
        synchronized (filterDefs) {
            FilterDef results[] = new FilterDef[filterDefs.size()];
            return filterDefs.values().toArray(results);
        }
    }

    /**
     * Return the list of filter mappings for this Context.
     * @return
     */
    @Override
    public List<FilterMap> findFilterMaps() {
        return filterMaps;
    }

    /**
     * Return the list of InstanceListener classes that will be added to
     * newly created Wrappers automatically.
     */
    @Override
    public List<String> findInstanceListeners() {
        return instanceListeners;
    }

    /**
     * Return the local EJB resource reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    @Override
    public ContextLocalEjb findLocalEjb(String name) {
        return namingResources.findLocalEjb(name);
    }

    /**
     * Return the defined local EJB resource references for this application.
     * If there are none, a zero-length array is returned.
     */
    @Override
    public ContextLocalEjb[] findLocalEjbs() {
        return namingResources.findLocalEjbs();
    }

    /**
     * FIXME: Fooling introspection ...
     */
    public Context findMappingObject() {
        return (Context) getMappingObject();
    }

    /**
     * Return the message destination with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired message destination
     */
    public MessageDestination findMessageDestination(String name) {
        synchronized (messageDestinations) {
            return messageDestinations.get(name);
        }
    }

    /**
     * Return the set of defined message destinations for this web
     * application.  If none have been defined, a zero-length array
     * is returned.
     */
    public MessageDestination[] findMessageDestinations() {
        synchronized (messageDestinations) {
            return messageDestinations.values().toArray(
                new MessageDestination[messageDestinations.size()]);
        }
    }

    /**
     * Return the message destination ref with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired message destination ref
     */
    public MessageDestinationRef findMessageDestinationRef(String name) {
        return namingResources.findMessageDestinationRef(name);
    }

    /**
     * Return the set of defined message destination refs for this web
     * application.  If none have been defined, a zero-length array
     * is returned.
     */
    public MessageDestinationRef[] findMessageDestinationRefs() {
        return namingResources.findMessageDestinationRefs();
    }

    /**
     * Return the MIME type to which the specified extension is mapped,
     * if any; otherwise return <code>null</code>.
     *
     * @param extension Extension to map to a MIME type
     */
    @Override
    public String findMimeMapping(String extension) {

        return mimeMappings.get(extension.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Return the extensions for which MIME mappings are defined.  If there
     * are none, a zero-length array is returned.
     */
    @Override
    public String[] findMimeMappings() {
        return mimeMappings.keySet().toArray(
            new String[mimeMappings.size()]);
    }

    /**
     * Return the value for the specified context initialization
     * parameter name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the parameter to return
     */
    @Override
    public String findParameter(String name) {
        synchronized (parameters) {
            return parameters.get(name);
        }
    }

    /**
     * Return the names of all defined context initialization parameters
     * for this Context.  If no parameters are defined, a zero-length
     * array is returned.
     */
    @Override
    public String[] findParameters() {
        synchronized (parameters) {
            return parameters.keySet().toArray(new String[parameters.size()]);
        }
    }

    /**
     * Return the resource reference with the specified name, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource reference
     */
    @Override
    public ContextResource findResource(String name) {
        return namingResources.findResource(name);
    }

    /**
     * Return the resource environment reference type for the specified
     * name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource environment reference
     */
    @Override
    public String findResourceEnvRef(String name) {
        return namingResources.findResourceEnvRef(name);
    }

    /**
     * Return the set of resource environment reference names for this
     * web application.  If none have been specified, a zero-length
     * array is returned.
     */
    @Override
    public String[] findResourceEnvRefs() {
        return namingResources.findResourceEnvRefs();
    }

    /**
     * Return the resource link with the specified name, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource link
     */
    @Override
    public ContextResourceLink findResourceLink(String name) {
        return namingResources.findResourceLink(name);
    }

    /**
     * Return the defined resource links for this application.  If
     * none have been defined, a zero-length array is returned.
     */
    @Override
    public ContextResourceLink[] findResourceLinks() {
        return namingResources.findResourceLinks();
    }

    /**
     * Return the defined resource references for this application.  If
     * none have been defined, a zero-length array is returned.
     */
    @Override
    public ContextResource[] findResources() {
        return namingResources.findResources();
    }

    /**
     * For the given security role (as used by an application), return the
     * corresponding role name (as defined by the underlying Realm) if there
     * is one.  Otherwise, return the specified role unchanged.
     *
     * @param role Security role to map
     */
    @Override
    public String findRoleMapping(String role) {

        String realRole = null;
        synchronized (roleMappings) {
            realRole = roleMappings.get(role);
        }
        if (realRole != null)
            return (realRole);
        else
            return (role);
    }

    /**
     * Checks if the given security role is defined for this application.
     *
     * @param role Security role to check for
     *
     * @return true if the specified security role is defined
     * for this application, false otherwise
     */
    @Override
    public boolean hasSecurityRole(String role) {
        return securityRoles.contains(role);
    }

    /**
     * Removes any security roles defined for this application.
     */
    @Override
    public void removeSecurityRoles() {
        // Inform interested listeners
        if (notifyContainerListeners) {
            Iterator<String> i = securityRoles.iterator();
            while (i.hasNext()) {
                fireContainerEvent("removeSecurityRole", i.next());
            }
        }
        securityRoles.clear();
    }

    /**
     * Return the servlet name mapped by the specified pattern (if any);
     * otherwise return <code>null</code>.
     *
     * @param pattern Pattern for which a mapping is requested
     */
    @Override
    public String findServletMapping(String pattern) {
        synchronized (servletMappings) {
            return servletMappings.get(pattern);
        }
    }

    /**
     * Return the patterns of all defined servlet mappings for this
     * Context.  If no mappings are defined, a zero-length array is returned.
     */
    @Override
    public String[] findServletMappings() {
        synchronized (servletMappings) {
            String results[] = new String[servletMappings.size()];
            return
                servletMappings.keySet().toArray(results);
        }
    }

    /**
     * Return the context-relative URI of the error page for the specified
     * HTTP status code, if any; otherwise return <code>null</code>.
     *
     * @param status HTTP status code to look up
     */
    @Override
    public ErrorPage findStatusPage(int status) {
        return statusPages.get(status);
    }

    /**
     * Return the set of HTTP status codes for which error pages have
     * been specified.  If none are specified, a zero-length array
     * is returned.
     */
    @Override
    public int[] findStatusPages() {
        synchronized (statusPages) {
            int results[] = new int[statusPages.size()];
            Iterator<Integer> elements = statusPages.keySet().iterator();
            int i = 0;
            while (elements.hasNext())
                results[i++] = elements.next();
            return results;
        }
    }

    /**
     * Return <code>true</code> if the specified welcome file is defined
     * for this Context; otherwise return <code>false</code>.
     *
     * @param name Welcome file to verify
     */
    @Override
    public boolean findWelcomeFile(String name) {
        synchronized (welcomeFiles) {
            for(String welcomeFile : welcomeFiles) {
                if(name.equals(welcomeFile)) {
                    return true;
                }
            }
        }
        return (false);
    }

    /**
     * Gets the watched resources defined for this web application.
     */
    @Override
    public List<String> getWatchedResources() {
        return watchedResources;
    }

    /**
     * Return the set of welcome files defined for this Context.  If none are
     * defined, a zero-length array is returned.
     */
    @Override
    public String[] findWelcomeFiles() {
        return (welcomeFiles);
    }

    /**
     * Return the list of LifecycleListener classes that will be added to
     * newly created Wrappers automatically.
     */
    @Override
    public List<String> findWrapperLifecycles() {
        return wrapperLifecycles;
    }

    /**
     * Return the list of ContainerListener classes that will be added to
     * newly created Wrappers automatically.
     */
    @Override
    public List<String> findWrapperListeners() {
        return wrapperListeners;
    }

    /**
     * Gets the Authenticator of this Context.
     *
     * @return the Authenticator of this Context
     */
    @Override
    public Authenticator getAuthenticator() {
        Pipeline p = getPipeline();
        if (p != null) {
            for (GlassFishValve valve : p.getValves()) {
                if (valve instanceof Authenticator) {
                    return (Authenticator) valve;
                }
            }
        }
        return null;
    }

    /**
     * Reload this web application, if reloading is supported.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  This method is designed to deal with
     * reloads required by changes to classes in the underlying repositories
     * of our class loader.  It does not handle changes to the web application
     * deployment descriptor.  If that has occurred, you should stop this
     * Context and create (and start) a new Context instance instead.
     *
     * @exception IllegalStateException if the <code>reloadable</code>
     *  property is set to <code>false</code>.
     */
    @Override
    public synchronized void reload() {

        // Validate our current component state
        if (!started) {
            String msg = MessageFormat.format(rb.getString(LogFacade.CONTAINER_NOT_STARTED_EXCEPTION), logName());
            throw new IllegalStateException(msg);
        }
        // Make sure reloading is enabled
        //      if (!reloadable)
        //          throw new IllegalStateException
        //              (sm.getString("standardContext.notReloadable"));
        //standardContext.notReloadable=PWC1287: Reloading is disabled on this Context
        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.RELOADING_STARTED);
        }

        // Stop accepting requests temporarily
        setPaused(true);

        try {
            stop();
        } catch (LifecycleException e) {
            String msg = MessageFormat.format(rb.getString(LogFacade.STOPPING_CONTEXT_EXCEPTION), this);
            log.log(Level.SEVERE, msg, e);
        }

        try {
            start();
        } catch (LifecycleException e) {
            String msg = MessageFormat.format(rb.getString(LogFacade.STARTING_CONTEXT_EXCEPTION), this);
            log.log(Level.SEVERE, msg, e);
        }

        setPaused(false);

    }

    /**
     * Remove the application parameter with the specified name from
     * the set for this application.
     *
     * @param name Name of the application parameter to remove
     */
    @Override
    public void removeApplicationParameter(String name) {
        ApplicationParameter match = null;
        Iterator<ApplicationParameter> i =
            applicationParameters.iterator();
        while (i.hasNext()) {
            ApplicationParameter applicationParameter = i.next();
            // Make sure this parameter is currently present
            if (name.equals(applicationParameter.getName())) {
                match = applicationParameter;
                break;
            }
        }
        if (match != null) {
            applicationParameters.remove(match);
            // Inform interested listeners
            if (notifyContainerListeners) {
                fireContainerEvent("removeApplicationParameter", name);
            }
        }
    }

    /**
     * Removes the given child container.
     *
     * @param child the child container to be removed
     *
     * @exception IllegalArgumentException if the given child container is
     * not an implementation of Wrapper
     */
    @Override
    public void removeChild(Container child) {

        if (!(child instanceof Wrapper))
            throw new IllegalArgumentException(rb.getString(LogFacade.NO_WRAPPER_EXCEPTION));

        super.removeChild(child);
    }

    /**
     * Removes any security constraints from this web application.
     */
    @Override
    public void removeConstraints() {
        // Inform interested listeners
        if (notifyContainerListeners) {
            Iterator<SecurityConstraint> i = constraints.iterator();
            while (i.hasNext()) {
                fireContainerEvent("removeConstraint", i.next());
            }
        }
        constraints.clear();
    }

    /**
     * Remove any EJB resource reference with the specified name.
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeEjb(String name) {

        namingResources.removeEjb(name);

        if (notifyContainerListeners) {
            fireContainerEvent("removeEjb", name);
        }
    }

    /**
     * Remove any environment entry with the specified name.
     *
     * @param name Name of the environment entry to remove
     */
    @Override
    public void removeEnvironment(String name) {
        if (namingResources == null) {
            return;
        }
        ContextEnvironment env = namingResources.findEnvironment(name);
        if (env == null) {
            throw new IllegalArgumentException
                ("Invalid environment name '" + name + "'");
        }

        namingResources.removeEnvironment(name);

        if (notifyContainerListeners) {
            fireContainerEvent("removeEnvironment", name);
        }
    }

    /**
     * Removes any error page declarations.
     */
    @Override
    public void removeErrorPages() {
        synchronized (exceptionPages) {
            if (notifyContainerListeners) {
                for (ErrorPage errorPage : exceptionPages.values()) {
                    fireContainerEvent("removeErrorPage", errorPage);
                }
            }
            exceptionPages.clear();
        }
        synchronized (statusPages) {
            if (notifyContainerListeners) {
                for (ErrorPage statusPage : statusPages.values()) {
                    fireContainerEvent("removeErrorPage", statusPage);
                }
            }
            statusPages.clear();
        }
    }

    /**
     * Remove the specified filter definition from this Context, if it exists;
     * otherwise, no action is taken.
     *
     * @param filterDef Filter definition to be removed
     */
    @Override
    public void removeFilterDef(FilterDef filterDef) {

        synchronized (filterDefs) {
            filterDefs.remove(filterDef.getFilterName());
        }

        if (notifyContainerListeners) {
            fireContainerEvent("removeFilterDef", filterDef);
        }
    }

    /**
     * Removes any filter mappings from this Context.
     */
    @Override
    public void removeFilterMaps() {
        // Inform interested listeners
        if (notifyContainerListeners) {
            Iterator<FilterMap> i = filterMaps.iterator();
            while (i.hasNext()) {
                fireContainerEvent("removeFilterMap", i.next());
            }
        }
        filterMaps.clear();
    }

    /**
     * Remove a class name from the list of InstanceListener classes that
     * will be added to newly created Wrappers.
     *
     * @param listener Class name of an InstanceListener class to be removed
     */
    @Override
    public void removeInstanceListener(String listener) {
        instanceListeners.remove(listener);
        // Inform interested listeners
        if (notifyContainerListeners) {
            fireContainerEvent("removeInstanceListener", listener);
        }
    }

    /**
     * Remove any local EJB resource reference with the specified name.
     *
     * @param name Name of the EJB resource reference to remove
     */
    @Override
    public void removeLocalEjb(String name) {

        namingResources.removeLocalEjb(name);

        if (notifyContainerListeners) {
            fireContainerEvent("removeLocalEjb", name);
        }
    }

    /**
     * Remove any message destination with the specified name.
     *
     * @param name Name of the message destination to remove
     */
    public void removeMessageDestination(String name) {

        synchronized (messageDestinations) {
            messageDestinations.remove(name);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("removeMessageDestination", name);
        }
    }

    /**
     * Remove any message destination ref with the specified name.
     *
     * @param name Name of the message destination ref to remove
     */
    public void removeMessageDestinationRef(String name) {

        namingResources.removeMessageDestinationRef(name);

        if (notifyContainerListeners) {
            fireContainerEvent("removeMessageDestinationRef", name);
        }
    }

    /**
     * Remove the MIME mapping for the specified extension, if it exists;
     * otherwise, no action is taken.
     *
     * @param extension Extension to remove the mapping for
     */
    @Override
    public void removeMimeMapping(String extension) {

        mimeMappings.remove(extension.toLowerCase(Locale.ENGLISH));

        if (notifyContainerListeners) {
            fireContainerEvent("removeMimeMapping", extension);
        }
    }

    /**
     * Remove the context initialization parameter with the specified
     * name, if it exists; otherwise, no action is taken.
     *
     * @param name Name of the parameter to remove
     */
    @Override
    public void removeParameter(String name) {

        synchronized (parameters) {
            parameters.remove(name);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("removeParameter", name);
        }
    }

    /**
     * Remove any resource reference with the specified name.
     *
     * @param resourceName Name of the resource reference to remove
     */
    @Override
    public void removeResource(String resourceName) {
        String decoded = URLDecoder.decode(resourceName);
        if (namingResources == null) {
            return;
        }
        ContextResource resource = namingResources.findResource(decoded);
        if (resource == null) {
            throw new IllegalArgumentException
                ("Invalid resource name '" + decoded + "'");
        }

        namingResources.removeResource(decoded);

        if (notifyContainerListeners) {
            fireContainerEvent("removeResource", decoded);
        }
    }

    /**
     * Remove any resource environment reference with the specified name.
     *
     * @param name Name of the resource environment reference to remove
     */
    @Override
    public void removeResourceEnvRef(String name) {

        namingResources.removeResourceEnvRef(name);

        if (notifyContainerListeners) {
            fireContainerEvent("removeResourceEnvRef", name);
        }
    }

    /**
     * Remove any resource link with the specified name.
     *
     * @param link Name of the resource link to remove
     */
    @Override
    public void removeResourceLink(String link) {
        String decoded = URLDecoder.decode(link);
        if (namingResources == null) {
            return;
        }
        ContextResourceLink resource = namingResources.findResourceLink(decoded);
        if (resource == null) {
            throw new IllegalArgumentException
                ("Invalid resource name '" + decoded + "'");
        }

        namingResources.removeResourceLink(decoded);

        if (notifyContainerListeners) {
            fireContainerEvent("removeResourceLink", decoded);
        }
    }

    /**
     * Remove any security role reference for the specified name
     *
     * @param role Security role (as used in the application) to remove
     */
    @Override
    public void removeRoleMapping(String role) {
        synchronized (roleMappings) {
            roleMappings.remove(role);
        }

        if (notifyContainerListeners) {
            fireContainerEvent("removeRoleMapping", role);
        }
    }

    /**
     * Remove any servlet mapping for the specified pattern, if it exists;
     * otherwise, no action is taken.
     *
     * @param pattern URL pattern of the mapping to remove
     */
    @Override
    public void removeServletMapping(String pattern) {

        String name = null;
        synchronized (servletMappings) {
            name = servletMappings.remove(pattern);
        }
        Wrapper wrapper = (Wrapper) findChild(name);
        if( wrapper != null ) {
            wrapper.removeMapping(pattern);
        }
        mapper.removeWrapper(pattern);

        if (notifyContainerListeners) {
            fireContainerEvent("removeServletMapping", pattern);
        }
    }

    /**
     * Checks whether this web application has any watched resources
     * defined.
     */
    @Override
    public boolean hasWatchedResources() {
        return !watchedResources.isEmpty();
    }

    /**
     * Clears any watched resources defined for this web application.
     */
    @Override
    public void removeWatchedResources() {
        synchronized (watchedResources) {
            // Inform interested listeners
            if (notifyContainerListeners) {
                Iterator<String> i = watchedResources.iterator();
                while (i.hasNext()) {
                    fireContainerEvent("removeWatchedResource", i.next());
                }
            }
            watchedResources.clear();
        }
    }

    @Override
    public void removeWelcomeFiles() {
        if (notifyContainerListeners) {
            for (String welcomeFile : welcomeFiles) {
                fireContainerEvent("removeWelcomeFile", welcomeFile);
            }
        }
        welcomeFiles = new String[0];
    }

    @Override
    public void removeWrapperLifecycles() {
        // Inform interested listeners
        if (notifyContainerListeners) {
            Iterator<String> i = wrapperLifecycles.iterator();
            while (i.hasNext()) {
                fireContainerEvent("removeWrapperLifecycle", i.next());
            }
        }
        wrapperLifecycles.clear();
    }

    @Override
    public void removeWrapperListeners() {
        // Inform interested listeners
        if (notifyContainerListeners) {
            Iterator<String> i = wrapperListeners.iterator();
            while (i.hasNext()) {
                fireContainerEvent("removeWrapperListener", i.next());
            }
        }
        wrapperListeners.clear();
    }

    @Override
    public void fireRequestInitializedEvent(ServletRequest request) {
        List<EventListener> listeners = getApplicationEventListeners();
        ServletRequestEvent event = null;
        if (!listeners.isEmpty()) {
            event = new ServletRequestEvent(getServletContext(), request);
            // create pre-service event
            Iterator<EventListener> iter = listeners.iterator();
            while (iter.hasNext()) {
                EventListener eventListener = iter.next();
                if (!(eventListener instanceof ServletRequestListener)) {
                    continue;
                }
                ServletRequestListener listener =
                    (ServletRequestListener) eventListener;
                // START SJSAS 6329662
                fireContainerEvent(ContainerEvent.BEFORE_REQUEST_INITIALIZED,
                    listener);
                // END SJSAS 6329662
                try {
                    listener.requestInitialized(event);
                } catch (Throwable t) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.REQUEST_INIT_EXCEPTION),
                                                      listener.getClass().getName());
                    log.log(Level.WARNING, msg, t);
                    request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, t);
                // START SJSAS 6329662
                } finally {
                    fireContainerEvent(ContainerEvent.AFTER_REQUEST_INITIALIZED,
                        listener);
                // END SJSAS 6329662
                }
            }
        }
    }

    @Override
    public void fireRequestDestroyedEvent(ServletRequest request) {
        List<EventListener> listeners = getApplicationEventListeners();
        if (!listeners.isEmpty()) {
            // create post-service event
            ServletRequestEvent event = new ServletRequestEvent(getServletContext(),
                request);
            int len = listeners.size();
            for (int i = 0; i < len; i++) {
                EventListener eventListener = listeners.get((len - 1) - i);
                if (!(eventListener instanceof ServletRequestListener)) {
                    continue;
                }
                ServletRequestListener listener =
                    (ServletRequestListener) eventListener;
                // START SJSAS 6329662
                fireContainerEvent(ContainerEvent.BEFORE_REQUEST_DESTROYED,
                    listener);
                // END SJSAS 6329662
                try {
                    listener.requestDestroyed(event);
                } catch (Throwable t) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.REQUEST_DESTROY_EXCEPTION),
                            listener.getClass().getName());
                    log.log(Level.WARNING, msg, t);
                    request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, t);
                // START SJSAS 6329662
                } finally {
                    fireContainerEvent(ContainerEvent.AFTER_REQUEST_DESTROYED,
                        listener);
                // END SJSAS 6329662
                }
            }
        }
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Configure and initialize the set of filters for this Context.
     * Return <code>true</code> if all filter initialization completed
     * successfully, or <code>false</code> otherwise.
     */
    public boolean filterStart() {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Starting filters");
        // Instantiate and record a FilterConfig for each defined filter
        boolean ok = true;
        synchronized (filterConfigs) {
            filterConfigs.clear();
            for (String name : filterDefs.keySet()) {
                if(log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, " Starting filter '" + name + "'");
                }
                try {
                    filterConfigs.put(name,
                        new ApplicationFilterConfig(this,
                                                    filterDefs.get(name)));
                } catch(Throwable t) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.STARTING_FILTER_EXCEPTION), name);
                    getServletContext().log(msg, t);
                    ok = false;
                }
            }
        }

        return (ok);

    }

    /**
     * Finalize and release the set of filters for this Context.
     * Return <code>true</code> if all filter finalization completed
     * successfully, or <code>false</code> otherwise.
     */
    public boolean filterStop() {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Stopping filters");

        // Release all Filter and FilterConfig instances
        synchronized (filterConfigs) {
            for (String filterName : filterConfigs.keySet()) {
                if(log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, " Stopping filter '" + filterName + "'");
                }
                ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)filterConfigs.get(filterName);
                filterConfig.release();
            }
            filterConfigs.clear();
        }
        return (true);
    }

    /**
     * Find and return the initialized <code>FilterConfig</code> for the
     * specified filter name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the desired filter
     */
    public FilterConfig findFilterConfig(String name) {
        return filterConfigs.get(name);
    }

    /**
     * Notifies all ServletContextListeners at their contextInitialized
     * method.
     */
    protected void contextListenerStart() {
        ServletContextEvent event = new ServletContextEvent(
            getServletContext());
        for (ServletContextListener listener : contextListeners) {
            if (listener instanceof RestrictedServletContextListener) {
                listener = ((RestrictedServletContextListener) listener).
                    getNestedListener();
                context.setRestricted(true);
            }
            try {
                fireContainerEvent(ContainerEvent.BEFORE_CONTEXT_INITIALIZED,
                                   listener);
                listener.contextInitialized(event);
            } finally {
                context.setRestricted(false);
                fireContainerEvent(ContainerEvent.AFTER_CONTEXT_INITIALIZED,
                                   listener);
            }
        }

        /*
         * Make sure there are no preliminary servlet or filter
         * registrations left after all listeners have been notified
         */
        Collection<ServletRegistrationImpl> servletRegistrations =
            servletRegisMap.values();
        for (ServletRegistrationImpl regis : servletRegistrations) {
            if (null == regis.getClassName() && null == regis.getJspFile()) {
                String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_WITHOUT_ANY_CLASS_OR_JSP), regis.getName());
                throw new IllegalStateException(msg);
            }
        }
        Collection<FilterRegistrationImpl> filterRegistrations =
            filterRegisMap.values();
        for (FilterRegistrationImpl regis : filterRegistrations) {
            if (null == regis.getClassName()) {
                String msg = MessageFormat.format(rb.getString(LogFacade.FILTER_WITHOUT_ANY_CLASS), regis.getName());
                throw new IllegalStateException(msg);
            }
        }

        isContextInitializedCalled = true;
    }


    /**
     * Loads and instantiates the listener with the specified classname.
     *
     * @param loader the classloader to use
     * @param listenerClassName the fully qualified classname to instantiate
     *
     * @return the instantiated listener
     *
     * @throws Exception if the specified classname fails to be loaded or
     * instantiated
     */
    @SuppressWarnings("unchecked")
    protected EventListener loadListener(ClassLoader loader,
                                         String listenerClassName)
            throws Exception {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Configuring event listener class '" +
                    listenerClassName + "'");
        }
        return createListener((Class<EventListener>)
            loader.loadClass(listenerClassName));
    }

    /**
     * Notifies all ServletContextListeners at their contextDestroyed
     * method.
     *
     * @return <code>true</code> if the event was processed successfully,
     * <code>false</code> otherwise.
     */
    private boolean contextListenerStop() {

        boolean ok = true;

        if (contextListeners.isEmpty()) {
            return ok;
        }

        ServletContextEvent event = new ServletContextEvent(
            getServletContext());
        int len = contextListeners.size();
        for (int i = 0; i < len; i++) {
            // Invoke in reverse order of declaration
            ServletContextListener listener =
                contextListeners.get((len - 1) - i);
            if (listener instanceof RestrictedServletContextListener) {
                listener = ((RestrictedServletContextListener) listener).
                    getNestedListener();
                context.setRestricted(true);
            }
            try {
                fireContainerEvent(ContainerEvent.BEFORE_CONTEXT_DESTROYED,
                                   listener);
                listener.contextDestroyed(event);
                fireContainerEvent(ContainerEvent.AFTER_CONTEXT_DESTROYED,
                                   listener);
            } catch (Throwable t) {
                context.setRestricted(false);
                fireContainerEvent(ContainerEvent.AFTER_CONTEXT_DESTROYED,
                                   listener);
                String msg = MessageFormat.format(rb.getString(LogFacade.LISTENER_STOP_EXCEPTION),
                                                  listener.getClass().getName());
                getServletContext().log(msg, t);
                ok = false;
            }
        }

        contextListeners.clear();

        return ok;
    }

    private void sessionListenerStop() {
        for (HttpSessionListener listener : sessionListeners) {
            // ServletContextListeners already had their PreDestroy called
            if (!(listener instanceof ServletContextListener)) {
                fireContainerEvent(ContainerEvent.PRE_DESTROY, listener);
            }
        }
        sessionListeners.clear();
    }

    private boolean eventListenerStop() {
        if (eventListeners.isEmpty()) {
            return true;
        }

        Iterator<EventListener> iter = eventListeners.iterator();
        while (iter.hasNext()) {
            EventListener listener = iter.next();
            // ServletContextListeners and HttpSessionListeners
            // already had their PreDestroy called
            if (listener instanceof ServletContextListener ||
                    listener instanceof HttpSessionListener) {
                continue;
            }
            fireContainerEvent(ContainerEvent.PRE_DESTROY, listener);
        }

        eventListeners.clear();

        return true;
    }

    /**
     * Merge the context initialization parameters specified in the application
     * deployment descriptor with the application parameters described in the
     * server configuration, respecting the <code>override</code> property of
     * the application parameters appropriately.
     */
    private void mergeParameters() {
        Map<String,String> mergedParams = new HashMap<>();

        for (String name : findParameters()) {
            mergedParams.put(name, findParameter(name));
        }

        for (ApplicationParameter param : findApplicationParameters()) {
            if (param.getOverride()) {
                if (mergedParams.get(param.getName()) == null)
                    mergedParams.put(param.getName(), param.getValue());
            } else {
                mergedParams.put(param.getName(), param.getValue());
            }
        }

        ServletContext sc = getServletContext();
        for (Map.Entry<String,String> entry : mergedParams.entrySet()) {
            sc.setInitParameter(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Allocate resources, including proxy.
     * Return <code>true</code> if initialization was successfull,
     * or <code>false</code> otherwise.
     */
    public boolean resourcesStart() {

        boolean ok = true;

        Hashtable<String, String> env = new Hashtable<String, String>();
        if(getParent() != null) {
            env.put(ProxyDirContext.HOST, getParent().getName());
        }
        env.put(ProxyDirContext.CONTEXT, getName());
        try {
            ProxyDirContext proxyDirContext = new ProxyDirContext(env, webappResources);
            if(webappResources instanceof BaseDirContext) {
                ((BaseDirContext)webappResources).setDocBase(getBasePath(getDocBase()));
                ((BaseDirContext)webappResources).allocate();
            }
            this.resources = proxyDirContext;
        } catch(Throwable t) {
            if(log.isLoggable(Level.FINE)) {
                String msg = MessageFormat.format(rb.getString(LogFacade.STARTING_RESOURCES_EXCEPTION), getName());
                log.log(Level.SEVERE, msg, t);
            } else {
                log.log(Level.SEVERE, LogFacade.STARTING_RESOURCE_EXCEPTION_MESSAGE,
                        new Object[] {getName(), t.getMessage()});
            }
            ok = false;
        }

        return ok;
    }

    /**
     * Starts this context's alternate doc base resources.
     */
    public void alternateResourcesStart() throws LifecycleException {

        if (alternateDocBases == null || alternateDocBases.isEmpty()) {
            return;
        }

        Hashtable<String, String> env = new Hashtable<String, String>();
        if (getParent() != null) {
            env.put(ProxyDirContext.HOST, getParent().getName());
        }
        env.put(ProxyDirContext.CONTEXT, getName());
        for(AlternateDocBase alternateDocBase : alternateDocBases) {
            String basePath = alternateDocBase.getBasePath();
            DirContext alternateWebappResources = ContextsAdapterUtility.unwrap(
                    alternateDocBase.getWebappResources());
            try {
                ProxyDirContext proxyDirContext = new ProxyDirContext(env, alternateWebappResources);
                if(alternateWebappResources instanceof BaseDirContext) {
                    ((BaseDirContext)alternateWebappResources).setDocBase(basePath);
                    ((BaseDirContext)alternateWebappResources).allocate();
                }
                alternateDocBase.setResources(ContextsAdapterUtility.wrap(proxyDirContext));
            } catch(Throwable t) {
                if(log.isLoggable(Level.FINE)) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.STARTING_RESOURCES_EXCEPTION), getName());
                    throw new LifecycleException(msg, t);
                } else {
                    String msg = MessageFormat.format(rb.getString(LogFacade.STARTING_RESOURCE_EXCEPTION_MESSAGE),
                                                      new Object[] {getName(), t.getMessage()});
                    throw new LifecycleException(msg);
                }
            }
        }
    }

    /**
     * Deallocate resources and destroy proxy.
     */
    public boolean resourcesStop() {

        boolean ok = true;

        try {
            if (resources != null) {
                if (resources instanceof Lifecycle) {
                    ((Lifecycle) resources).stop();
                }
                if (webappResources instanceof BaseDirContext) {
                    ((BaseDirContext) webappResources).release();
                }
            }
        } catch (Throwable t) {
            log.log(Level.SEVERE, LogFacade.STOPPING_RESOURCES_EXCEPTION, t);
            ok = false;
        }

        this.resources = null;

        return (ok);

    }

    /**
     * Stops this context's alternate doc base resources.
     */
    public boolean alternateResourcesStop() {

        boolean ok = true;

        if (alternateDocBases == null || alternateDocBases.isEmpty()) {
            return ok;
        }
        for(AlternateDocBase alternateDocBase : alternateDocBases) {
            final DirContext alternateResources = ContextsAdapterUtility.unwrap(
                    alternateDocBase.getResources());
            if(alternateResources instanceof Lifecycle) {
                try {
                    ((Lifecycle)alternateResources).stop();
                } catch(Throwable t) {
                    log.log(Level.SEVERE, LogFacade.STOPPING_RESOURCES_EXCEPTION, t);
                    ok = false;
                }
            }
            final DirContext alternateWebappResources = ContextsAdapterUtility.unwrap(
                alternateDocBase.getWebappResources());
            if(alternateWebappResources instanceof BaseDirContext) {
                try {
                    ((BaseDirContext)alternateWebappResources).release();
                } catch(Throwable t) {
                    log.log(Level.SEVERE, LogFacade.STOPPING_RESOURCES_EXCEPTION, t);
                    ok = false;
                }
            }
        }

        this.alternateDocBases = null;

        return (ok);
    }

    /**
     * Load and initialize all servlets marked "load on startup" in the
     * web application deployment descriptor.
     *
     * @param children Array of wrappers for all currently defined
     *  servlets (including those not declared load on startup)
     */
    /* SJSAS 6377790
    public void loadOnStartup(Container children[]){
    */
    // START SJSAS 6377790
    public void loadOnStartup(Container children[]) throws LifecycleException {
    // END SJSAS 6377790
        // Collect "load on startup" servlets that need to be initialized
        Map<Integer, List<Wrapper>> map =
            new TreeMap<Integer, List<Wrapper>>();
        for (Container aChildren : children) {
            Wrapper wrapper = (Wrapper)aChildren;
            int loadOnStartup = wrapper.getLoadOnStartup();
            if(loadOnStartup < 0) {
                continue;
            }
            Integer key = loadOnStartup;
            List<Wrapper> list = map.get(key);
            if(list == null) {
                list = new ArrayList<Wrapper>();
                map.put(key, list);
            }
            list.add(wrapper);
        }

        // Load the collected "load on startup" servlets
        for (List<Wrapper> list : map.values()) {
            for(Wrapper wrapper : list) {
                try {
                    wrapper.load();
                } catch(ServletException e) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_LOAD_EXCEPTION), getName());
                    getServletContext().log(msg, StandardWrapper.getRootCause(e));
                    // NOTE: load errors (including a servlet that throws
                    // UnavailableException from the init() method) are NOT
                    // fatal to application startup
                    // START SJSAS 6377790
                    throw new LifecycleException(
                        StandardWrapper.getRootCause(e));
                    // END SJSAS 6377790
                }
            }
        }
    }

    /**
     * Starts the session manager of this Context.
     */
    protected void managerStart() throws LifecycleException {
        if ((manager != null) && (manager instanceof Lifecycle)) {
            ((Lifecycle) getManager()).start();
        }
    }


    /**
     * Stops the session manager of this Context.
     */
    protected void managerStop() throws LifecycleException {
        if ((manager != null) && (manager instanceof Lifecycle)) {
            ((Lifecycle) manager).stop();
        }
    }

    /**
     * Start this Context component.
     *
     * @exception LifecycleException if a startup error occurs
     */
    @Override
    public synchronized void start() throws LifecycleException {

        if (started) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, LogFacade.CONTAINER_ALREADY_STARTED_EXCEPTION, logName());
            }
            return;
        }

        long startupTimeStart = System.currentTimeMillis();

        if(!initialized) {
            try {
                init();
            } catch( Exception ex ) {
                throw new LifecycleException("Error initializaing ", ex);
            }
        }
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Starting " +
                    ("".equals(getName()) ? "ROOT" : getName()));
        }

        // Set JMX object name for proper pipeline registration
        preRegisterJMX();

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        setAvailable(false);
        setConfigured(false);

        // Add missing components as necessary
        if (webappResources == null) {   // (1) Required by Loader
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Configuring default Resources");
            }
            try {
                if ((docBase != null) && (docBase.endsWith(".war")) &&
                        (!(new File(docBase).isDirectory())))
                    setResources(new WARDirContext());
                else
                    setResources(new WebDirContext());
            } catch (IllegalArgumentException e) {
                throw new LifecycleException(rb.getString(LogFacade.INIT_RESOURCES_EXCEPTION), e);
            }
        }

        resourcesStart();

        // Add alternate resources
        if (alternateDocBases != null && !alternateDocBases.isEmpty()) {
            for(AlternateDocBase alternateDocBase : alternateDocBases) {
                String docBase = alternateDocBase.getDocBase();
                if(log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Configuring alternate resources");
                }
                try {
                    if(docBase != null && docBase.endsWith(".war") &&
                        (!(new File(docBase).isDirectory()))) {
                        setAlternateResources(alternateDocBase,
                            new WARDirContext());
                    } else {
                        setAlternateResources(alternateDocBase,
                            new FileDirContext());
                    }
                } catch(IllegalArgumentException e) {
                    throw new LifecycleException(rb.getString(LogFacade.INIT_RESOURCES_EXCEPTION), e);
                }
            }

            alternateResourcesStart();
        }

        if (getLoader() == null) {
            createLoader();
        }

        // Initialize character set mapper
        getCharsetMapper();

        // Post work directory
        postWorkDirectory();

        // Validate required extensions
        try {
            ExtensionValidator.validateApplication(getResources(), this);
        } catch (IOException ioe) {
            String msg = MessageFormat.format(rb.getString(LogFacade.DEPENDENCY_CHECK_EXCEPTION), this);
            throw new LifecycleException(msg, ioe);
        }

        // Reading the "catalina.useNaming" environment variable
        String useNamingProperty = System.getProperty("catalina.useNaming");
        if ((useNamingProperty != null) &&
                ("false".equals(useNamingProperty))) {
            useNaming = false;
        }

        if (isUseNaming()) {
            if (namingContextListener == null) {
                namingContextListener = new NamingContextListener();
                namingContextListener.setDebug(getDebug());
                namingContextListener.setName(getNamingContextName());
                addLifecycleListener(namingContextListener);
            }
        }

        // Binding thread
        // START OF SJSAS 8.1 6174179
        //ClassLoader oldCCL = bindThread();
        ClassLoader oldCCL = null;
        // END OF SJSAS 8.1 6174179

        try {
            started = true;

            // Start our subordinate components, if any
            if ((loader != null) && (loader instanceof Lifecycle))
                ((Lifecycle) loader).start();
            if ((logger != null) && (logger instanceof Lifecycle))
                ((Lifecycle) logger).start();

            // Unbinding thread
            // START OF SJSAS 8.1 6174179
            //unbindThread(oldCCL);
            // END OF SJSAS 8.1 6174179

            // Binding thread
            oldCCL = bindThread();

            if ((realm != null) && (realm instanceof Lifecycle))
                ((Lifecycle) realm).start();
            if ((resources != null) && (resources instanceof Lifecycle))
                ((Lifecycle) resources).start();

            // Start our child containers, if any
            for (Container child : findChildren()) {
                if(child instanceof Lifecycle) {
                    ((Lifecycle)child).start();
                }
            }

            // Start the Valves in our pipeline (including the basic),
            // if any
            if (pipeline instanceof Lifecycle)
                ((Lifecycle) pipeline).start();

            // START SJSAS 8.1 5049111
            // Notify our interested LifecycleListeners
            lifecycle.fireLifecycleEvent(START_EVENT, null);
            // END SJSAS 8.1 5049111
        } catch (Throwable t) {
            throw new LifecycleException(t);
        } finally {
            // Unbinding thread
            unbindThread(oldCCL);
        }

        if (!getConfigured()) {
            String msg = MessageFormat.format(rb.getString(LogFacade.STARTUP_CONTEXT_FAILED_EXCEPTION), getName());
            throw new LifecycleException(msg);
        }

        // Store some required info as ServletContext attributes
        postResources();
        if (orderedLibs != null && !orderedLibs.isEmpty()) {
            getServletContext().setAttribute(ServletContext.ORDERED_LIBS,
                orderedLibs);
            context.setAttributeReadOnly(ServletContext.ORDERED_LIBS);
        }

        // Initialize associated mapper
        mapper.setContext(getPath(), welcomeFiles, ContextsAdapterUtility.wrap(resources));

        // Binding thread
        oldCCL = bindThread();

        try {
            // Set up the context init params
            mergeParameters();

            // Notify our interested LifecycleListeners
            lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);

            // Support for pluggability : this has to be done before
            // listener events are fired
            callServletContainerInitializers();

            // Configure and call application event listeners
            contextListenerStart();

            // Start manager
            if ((manager != null) && (manager instanceof Lifecycle)) {
                ((Lifecycle) getManager()).start();
            }

            // Start ContainerBackgroundProcessor thread
            super.threadStart();

            // Configure and call application filters
            filterStart();

            // Load and initialize all "load on startup" servlets
            loadOnStartup(findChildren());
        } catch (Throwable t) {
            log.log(Level.SEVERE, LogFacade.STARTUP_CONTEXT_FAILED_EXCEPTION, getName());
            try {
                stop();
            } catch (Throwable tt) {
                log.log(Level.SEVERE, LogFacade.CLEANUP_FAILED_EXCEPTION, tt);
            }
            throw new LifecycleException(t);
        } finally {
            // Unbinding thread
            unbindThread(oldCCL);
        }

        // Set available status depending upon startup success
        if (log.isLoggable(Level.FINEST)) {
            log.log(Level.FINEST, "Startup successfully completed");
        }

        setAvailable(true);

        // JMX registration
        registerJMX();

        startTimeMillis = System.currentTimeMillis();
        startupTime = startTimeMillis - startupTimeStart;

        // Send j2ee.state.running notification
        if (getObjectName() != null) {
            Notification notification =
                new Notification("j2ee.state.running", this, sequenceNumber++);
            sendNotification(notification);
        }

        // Close all JARs right away to avoid always opening a peak number
        // of files on startup
        if (getLoader() instanceof WebappLoader) {
            ((WebappLoader) getLoader()).closeJARs(true);
        }
    }

    protected Types getTypes() {
        return null;
    }

    protected boolean isStandalone() {
        return true;
    }

    protected void callServletContainerInitializers()
            throws LifecycleException {

        // Get the list of ServletContainerInitializers and the classes
        // they are interested in
        Map<Class<?>, List<Class<? extends ServletContainerInitializer>>> interestList =
            ServletContainerInitializerUtil.getInterestList(
                servletContainerInitializers);
        Map<Class<? extends ServletContainerInitializer>, Set<Class<?>>> initializerList =
            ServletContainerInitializerUtil.getInitializerList(
                servletContainerInitializers, interestList,
                getTypes(),
                getClassLoader(), isStandalone());
        if (initializerList == null) {
            return;
        }

        // Allow programmatic registration of ServletContextListeners, but
        // only within the scope of ServletContainerInitializer#onStartup
        isProgrammaticServletContextListenerRegistrationAllowed = true;

        // We have the list of initializers and the classes that satisfy the condition.
        // Time to call the initializers
        ServletContext ctxt = this.getServletContext();
        try {
            for (Map.Entry<Class<? extends ServletContainerInitializer>, Set<Class<?>>> e : initializerList.entrySet()) {
                Class<? extends ServletContainerInitializer> initializer = e.getKey();

                try {
                    if (log.isLoggable(FINE)) {
                        log.log(FINE,
                            "Calling ServletContainerInitializer [" + initializer + "] onStartup with classes " + e.getValue());
                    }
                    ServletContainerInitializer iniInstance = initializer.newInstance();

                    fireContainerEvent(BEFORE_CONTEXT_INITIALIZER_ON_STARTUP, iniInstance);

                    iniInstance.onStartup(initializerList.get(initializer), ctxt);

                    fireContainerEvent(AFTER_CONTEXT_INITIALIZER_ON_STARTUP, iniInstance);
                } catch (Throwable t) {
                    log.log(SEVERE,
                        format(rb.getString(INVOKING_SERVLET_CONTAINER_INIT_EXCEPTION), initializer.getCanonicalName()),
                        t
                    );
                    throw new LifecycleException(t);
                }
            }
        } finally {
            isProgrammaticServletContextListenerRegistrationAllowed = false;
        }
    }

    public void setServletContainerInitializerInterestList(Iterable<ServletContainerInitializer> initializers) {
        servletContainerInitializers = initializers;
    }

    /**
     * Creates a classloader for this context.
     */
    public void createLoader() {
        ClassLoader parent = null;
        if (getPrivileged()) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Configuring privileged default Loader");
            }
            parent = this.getClass().getClassLoader();
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Configuring non-privileged default Loader");
            }
            parent = getParentClassLoader();
        }
        WebappLoader webappLoader = new WebappLoader(parent);
        webappLoader.setDelegate(getDelegate());
        webappLoader.setUseMyFaces(useMyFaces);
        setLoader(webappLoader);
    }

    /**
     * Stop this Context component.
     *
     * @exception LifecycleException if a shutdown error occurs
     */
    @Override
    public synchronized void stop() throws LifecycleException {
        stop(false);
    }

    /**
     * Stop this Context component.
     *
     * @param isShutdown true if this Context is being stopped as part
     * of a domain shutdown (as opposed to an undeployment), and false otherwise
     * @exception LifecycleException if a shutdown error occurs
     */
    public synchronized void stop(boolean isShutdown)
            throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            if(log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, LogFacade.CONTAINER_NOT_STARTED_EXCEPTION, logName());
            }
            return;
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // Send j2ee.state.stopping notification
        if (this.getObjectName() != null) {
            Notification notification =
                new Notification("j2ee.state.stopping", this, sequenceNumber++);
            sendNotification(notification);
        }

        // Mark this application as unavailable while we shut down
        setAvailable(false);

        // Binding thread
        ClassLoader oldCCL = bindThread();

        try {
            // Stop our child containers, if any
            for (Container child : findChildren()) {
                if(child instanceof Lifecycle) {
                    ((Lifecycle)child).stop();
                }
            }

            // Stop our filters
            filterStop();

            // Stop ContainerBackgroundProcessor thread
            super.threadStop();

            if ((manager != null) && (manager instanceof Lifecycle)) {
                if(manager instanceof StandardManager) {
                    ((StandardManager)manager).stop(isShutdown);
                } else {
                    ((Lifecycle)manager).stop();
                }
            }

            /*
             * Stop all ServletContextListeners. It is important that they
             * are passed a ServletContext to their contextDestroyed() method
             * that still has all its attributes set. In other words, it is
             * important that we invoke these listeners before calling
             * context.clearAttributes()
             */
            contextListenerStop();

            sessionListenerStop();

            // Clear all application-originated servlet context attributes
            if (context != null) {
                context.clearAttributes();
            }

            /*
             * Stop all event listeners, including those of type
             * ServletContextAttributeListener. For the latter, it is
             * important that we invoke them after calling
             * context.clearAttributes, so that they receive the corresponding
             * attribute removal events
             */
            eventListenerStop();

            // Notify our interested LifecycleListeners
            lifecycle.fireLifecycleEvent(STOP_EVENT, null);
            started = false;

            // Stop the Valves in our pipeline (including the basic), if any
            if (pipeline instanceof Lifecycle) {
                ((Lifecycle) pipeline).stop();
            }

            // Finalize our character set mapper
            setCharsetMapper(null);

            // Stop resources
            resourcesStop();
            alternateResourcesStop();

            if ((realm != null) && (realm instanceof Lifecycle)) {
                ((Lifecycle) realm).stop();
            }
            if ((logger != null) && (logger instanceof Lifecycle)) {
                ((Lifecycle) logger).stop();
            }
            /* SJSAS 6347606
            if ((loader != null) && (loader instanceof Lifecycle)) {
                ((Lifecycle) loader).stop();
            }
            */
        } catch(Throwable t) {
            // started was "true" when it first enters the try block.
            // Note that it is set to false after STOP_EVENT is fired.
            // One need to fire STOP_EVENT to clean up naming information
            // if START_EVENT is processed successfully.
            if (started) {
                lifecycle.fireLifecycleEvent(STOP_EVENT, null);
            }

            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            } else if (t instanceof LifecycleException) {
                throw (LifecycleException)t;
            } else {
                throw new LifecycleException(t);
            }
        } finally {

            // Unbinding thread
            unbindThread(oldCCL);

            // START SJSAS 6347606
            /*
             * Delay the stopping of the webapp classloader until this point,
             * because unbindThread() calls the security-checked
             * Thread.setContextClassLoader(), which may ask the current thread
             * context classloader (i.e., the webapp classloader) to load
             * Principal classes specified in the security policy file
             */
            if ((loader != null) && (loader instanceof Lifecycle)) {
                ((Lifecycle) loader).stop();
            }
            // END SJSAS 6347606
        }

        // Send j2ee.state.stopped notification
        if (this.getObjectName() != null) {
            Notification notification =
                new Notification("j2ee.state.stopped", this ,sequenceNumber++);
            sendNotification(notification);
        }

        // Reset application context
        context = null;

        // This object will no longer be visible or used.
        try {
            resetContext();
        } catch( Exception ex ) {
            String msg = MessageFormat.format(rb.getString(LogFacade.RESETTING_CONTEXT_EXCEPTION), this);
            log.log(Level.SEVERE, msg, ex);
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Stopping complete");

        if(oname != null) {
            // Send j2ee.object.deleted notification
            Notification notification =
                    new Notification("j2ee.object.deleted", this, sequenceNumber++);
            sendNotification(notification);
        }

    }

    /**
     * Destroys this context by cleaning it up completely.
     *
     * The problem is that undoing all the config in start() and restoring
     * a 'fresh' state is impossible. After stop()/destroy()/init()/start()
     * we should have the same state as if a fresh start was done - i.e
     * read modified web.xml, etc. This can only be done by completely
     * removing the context object and remapping a new one, or by cleaning
     * up everything.
     *
     * XXX  Should this be done in stop() ?
     */
    @Override
    public void destroy() throws Exception {
        super.destroy();

        // START SJASAS 6359401
        // super.destroy() will stop session manager and cause it to unload
        // all its active sessions into a file. Delete this file, because this
        // context is being destroyed and must not leave any traces.
        if (getManager() instanceof ManagerBase) {
            ((ManagerBase)getManager()).release();
        }
        // END SJSAS 6359401

        instanceListeners.clear();
        instanceListenerInstances.clear();
    }

    private void resetContext() throws Exception, MBeanRegistrationException {
        // Restore the original state (pre reading web.xml in start)
        // If you extend this - override this method and make sure to clean up
		children = new HashMap<String, Container>();
        startupTime = 0;
        startTimeMillis = 0;
        tldScanTime = 0;

        // Bugzilla 32867
        distributable = false;

        eventListeners.clear();
        contextListeners.clear();
        sessionListeners.clear();

        requestCharacterEncoding = null;
        responseCharacterEncoding = DEFAULT_RESPONSE_CHARACTER_ENCODING;

        if (log.isLoggable(FINE)) {
            log.log(FINE, "resetContext " + oname);
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
        sb.append("StandardContext[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

    }

    /**
     * Execute a periodic task, such as reloading, etc. This method will be
     * invoked inside the classloading context of this container. Unexpected
     * throwables will be caught and logged.
     */
    @Override
    public void backgroundProcess() {

        if (!started)
            return;

        count = (count + 1) % managerChecksFrequency;

        if ((getManager() != null) && (count == 0)) {
            if (getManager() instanceof StandardManager) {
                ((StandardManager) getManager()).processExpires();
            } else if (getManager() instanceof PersistentManagerBase) {
                PersistentManagerBase pManager =
                    (PersistentManagerBase) getManager();
                pManager.backgroundProcess();
            }
        }

        // START S1AS8PE 4965017
        if (isReload()) {
            if (getLoader() != null) {
                if (reloadable && (getLoader().modified())) {
                    try {
                        Thread.currentThread().setContextClassLoader
                            (standardContextClassLoader);
                        reload();
                    } finally {
                        if (getLoader() != null) {
                            Thread.currentThread().setContextClassLoader
                                (getClassLoader());
                        }
                    }
                }
                if (getLoader() instanceof WebappLoader) {
                    ((WebappLoader) getLoader()).closeJARs(false);
                }
            }
        }
        // END S1AS8PE 4965017
    }


    // ------------------------------------------------------ Protected Methods

    /**
     * Adjust the URL pattern to begin with a leading slash, if appropriate
     * (i.e. we are running a servlet 2.2 application).  Otherwise, return
     * the specified URL pattern unchanged.
     *
     * @param urlPattern The URL pattern to be adjusted (if needed)
     *  and returned
     */
    protected String adjustURLPattern(String urlPattern) {

        if (urlPattern == null)
            return (urlPattern);
        if (urlPattern.startsWith("/") || urlPattern.startsWith("*."))
            return (urlPattern);
        if (!isServlet22())
            return (urlPattern);
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, LogFacade.URL_PATTERN_WARNING, urlPattern);
        }
        return ("/" + urlPattern);

    }

    /**
     * Are we processing a version 2.2 deployment descriptor?
     */
    protected boolean isServlet22() {
        return publicId != null && publicId.equals(
            org.apache.catalina.startup.Constants.WebDtdPublicId_22);
    }

    /**
     * Return a File object representing the base directory for the
     * entire servlet container (i.e. the Engine container if present).
     */
    protected File engineBase() {
        String base=System.getProperty("catalina.base");
        if( base == null ) {
            StandardEngine eng=(StandardEngine)this.getParent().getParent();
            base=eng.getBaseDir();
        }
        return (new File(base));
    }

    // -------------------------------------------------------- Private Methods


    /**
     * Bind current thread, both for CL purposes and for JNDI ENC support
     * during : startup, shutdown and realoading of the context.
     *
     * @return the previous context class loader
     */
    private ClassLoader bindThread() {
        ClassLoader oldContextClassLoader =
            Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        if (isUseNaming()) {
            try {
                ContextBindings.bindThread(this, this);
            } catch (Throwable e) {
                log.log(Level.WARNING, LogFacade.BIND_THREAD_EXCEPTION, e);
            }
        }

        return oldContextClassLoader;

    }

    /**
     * Unbind thread.
     */
    private void unbindThread(ClassLoader oldContextClassLoader) {
        Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        if (isUseNaming()) {
            ContextBindings.unbindThread(this, this);
        }
    }

    /**
     * Get base path.
     */
    private String getBasePath(String docBase) {
        String basePath = null;
        Container container = this;
        while (container != null) {
            if (container instanceof Host)
                break;
            container = container.getParent();
        }
        File file = new File(docBase);
        if (!file.isAbsolute()) {
            if (container == null) {
                basePath = (new File(engineBase(), docBase)).getPath();
            } else {
                // Use the "appBase" property of this container
                String appBase = ((Host) container).getAppBase();
                file = new File(appBase);
                if (!file.isAbsolute())
                    file = new File(engineBase(), appBase);
                basePath = (new File(file, docBase)).getPath();
            }
        } else {
            basePath = file.getPath();
        }
        return basePath;
    }

    /**
     * Get app base.
     *
    private String getAppBase() {
        String appBase = null;
        Container container = this;
        while (container != null) {
            if (container instanceof Host)
                break;
            container = container.getParent();
        }
        if (container != null) {
            appBase = ((Host) container).getAppBase();
        }
        return appBase;
    }

    /**
     * Get config base.
     *
    private File getConfigBase() {
        File configBase =
            new File(System.getProperty("catalina.base"), "conf");
        if (!configBase.exists()) {
            return null;
        }
        Container container = this;
        Container host = null;
        Container engine = null;
        while (container != null) {
            if (container instanceof Host)
                host = container;
            if (container instanceof Engine)
                engine = container;
            container = container.getParent();
        }
        if (engine != null) {
            configBase = new File(configBase, engine.getName());
        }
        if (host != null) {
            configBase = new File(configBase, host.getName());
        }
        configBase.mkdirs();
        return configBase;
    }
    */

    /**
     * Given a context path, get the config file name.
     */
    protected String getDefaultConfigFile() {
        String basename = null;
        String path = getPath();
        if ("".equals(path)) {
            basename = "ROOT";
        } else {
            basename = path.substring(1).replace('/', '_');
        }
        return (basename + ".xml");
    }

    /**
     * Copy a file.
     *
    private boolean copy(File src, File dest) {
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(src);
            os = new FileOutputStream(dest);
            byte[] buf = new byte[4096];
            while (true) {
                int len = is.read(buf);
                if (len < 0)
                    break;
                os.write(buf, 0, len);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                // Ignore
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return true;
    }
    */


    /**
     * Get naming context full name.
     */
    public String getNamingContextName() {
        if (namingContextName == null) {
            Container parent = getParent();
            if (parent == null) {
                namingContextName = getName();
            } else {
                Stack<String> stk = new Stack<String>();
                StringBuilder buff = new StringBuilder();
                while (parent != null) {
                    stk.push(parent.getName());
                    parent = parent.getParent();
                }
                while (!stk.empty()) {
                    buff.append("/").append(stk.pop());
                }
                buff.append(getName());
                namingContextName = buff.toString();
            }
            // START RIMOD 4868393
            // append an id to make the name unique to the instance.
            namingContextName += instanceIDCounter.getAndIncrement();
            // END RIMOD 4868393
        }
        return namingContextName;
    }

    /**
     * @return the request processing paused flag for this Context
     */
    public boolean getPaused() {
        return paused;
    }

    /**
     * Stores the resources of this application as ServletContext
     * attributes.
     */
    private void postResources() {
        getServletContext().setAttribute(
            Globals.RESOURCES_ATTR, getResources());
        context.setAttributeReadOnly(Globals.RESOURCES_ATTR);
        getServletContext().setAttribute(
            Globals.ALTERNATE_RESOURCES_ATTR, getAlternateDocBases());
        context.setAttributeReadOnly(Globals.ALTERNATE_RESOURCES_ATTR);
    }

    public String getHostname() {
        Container parentHost = getParent();
        if (parentHost != null) {
            hostName = parentHost.getName();
        }
        if ((hostName == null) || (hostName.length() < 1))
            hostName = "_";
        return hostName;
    }

    /**
     * Set the appropriate context attribute for our work directory.
     */
    private void postWorkDirectory() {

        // Acquire (or calculate) the work directory path
        String workDir = getWorkDir();
        if (workDir == null || workDir.length() == 0) {

            // Retrieve our parent (normally a host) name
            String hostName = null;
            String engineName = null;
            String hostWorkDir = null;
            Container parentHost = getParent();
            if (parentHost != null) {
                hostName = parentHost.getName();
                if (parentHost instanceof StandardHost) {
                    hostWorkDir = ((StandardHost)parentHost).getWorkDir();
                }
                Container parentEngine = parentHost.getParent();
                if (parentEngine != null) {
                   engineName = parentEngine.getName();
                }
            }
            if ((hostName == null) || (hostName.length() < 1))
                hostName = "_";
            if ((engineName == null) || (engineName.length() < 1))
                engineName = "_";

            String temp = getPath();
            if (temp.startsWith("/"))
                temp = temp.substring(1);
            temp = temp.replace('/', '_');
            temp = temp.replace('\\', '_');
            if (temp.length() < 1)
                temp = "_";
            if (hostWorkDir != null ) {
                workDir = hostWorkDir + File.separator + temp;
            } else {
                workDir = "work" + File.separator + engineName +
                    File.separator + hostName + File.separator + temp;
            }
            setWorkDir(workDir);
        }

        // Create this directory if necessary
        File dir = new File(workDir);
        if (!dir.isAbsolute()) {
            File catalinaHome = engineBase();
            String catalinaHomePath = null;
            try {
                catalinaHomePath = catalinaHome.getCanonicalPath();
                dir = new File(catalinaHomePath, workDir);
            } catch (IOException e) {
            }
        }
        if (!dir.mkdirs() && !dir.isDirectory()) {
            log.log(Level.SEVERE, LogFacade.CREATE_WORK_DIR_EXCEPTION, dir.getAbsolutePath());
        }

        // Set the appropriate servlet context attribute
        getServletContext().setAttribute(ServletContext.TEMPDIR, dir);
        context.setAttributeReadOnly(ServletContext.TEMPDIR);

    }

    /**
     * Set the request processing paused flag for this Context.
     *
     * @param paused The new request processing paused flag
     */
    private void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * Validate the syntax of a proposed <code>&lt;url-pattern&gt;</code>
     * for conformance with specification requirements.
     *
     * @param urlPattern URL pattern to be validated
     */
    protected boolean validateURLPattern(String urlPattern) {
        if (urlPattern == null) {
            return false;
        }
        if (urlPattern.isEmpty()) {
            return true;
        }
        if (urlPattern.indexOf('\n') >= 0 || urlPattern.indexOf('\r') >= 0) {
            log.log(Level.WARNING, LogFacade.URL_PATTERN_CANNOT_BE_MATCHED_EXCEPTION, urlPattern);
            return false;
        }
        if (urlPattern.startsWith("*.")) {
            if (urlPattern.indexOf('/') < 0) {
                checkUnusualURLPattern(urlPattern);
                return true;
            } else
                return false;
        }
        if ( (urlPattern.startsWith("/")) &&
                (!urlPattern.contains("*."))) {
            checkUnusualURLPattern(urlPattern);
            return true;
        } else
            return false;

    }


    /**
     * Check for unusual but valid <code>&lt;url-pattern&gt;</code>s.
     * See Bugzilla 34805, 43079 &amp; 43080
     */
    private void checkUnusualURLPattern(String urlPattern) {
        if (log.isLoggable(Level.INFO)) {
            if(urlPattern.endsWith("*") && (urlPattern.length() < 2 ||
                    urlPattern.charAt(urlPattern.length()-2) != '/')) {
                String msg = "Suspicious url pattern: \"" + urlPattern + "\"" +
                             " in context [" + getName() + "] - see" +
                             " section SRV.11.2 of the Servlet specification";
                log.log(Level.INFO, msg);
            }
        }
    }


    // -------------------- JMX methods  --------------------

    /**
     * Return the MBean Names of the set of defined environment entries for
     * this web application
     */
    public String[] getEnvironments() {
        ContextEnvironment[] envs = getNamingResources().findEnvironments();
        List<String> results = new ArrayList<String>();
        for(ContextEnvironment env : envs) {
            try {
                ObjectName oname = createObjectName(env);
                results.add(oname.toString());
            } catch(MalformedObjectNameException e) {
                IllegalArgumentException iae = new IllegalArgumentException
                    ("Cannot create object name for environment " + env);
                iae.initCause(e);
                throw iae;
            }
        }
        return results.toArray(new String[results.size()]);

    }


    /**
     * Return the MBean Names of all the defined resource references for this
     * application.
     */
    public String[] getResourceNames() {

        ContextResource[] resources = getNamingResources().findResources();
        List<String> results = new ArrayList<String>();
        for(ContextResource resource : resources) {
            try {
                ObjectName oname = createObjectName(resource);
                results.add(oname.toString());
            } catch(MalformedObjectNameException e) {
                IllegalArgumentException iae = new IllegalArgumentException
                    ("Cannot create object name for resource " + resource);
                iae.initCause(e);
                throw iae;
            }
        }
        return results.toArray(new String[results.size()]);

    }

    /**
     * Return the MBean Names of all the defined resource links for this
     * application
     */
    public String[] getResourceLinks() {

        ContextResourceLink[] links = getNamingResources().findResourceLinks();
        List<String> results = new ArrayList<String>();
        for(ContextResourceLink link : links) {
            try {
                ObjectName oname = createObjectName(link);
                results.add(oname.toString());
            } catch(MalformedObjectNameException e) {
                IllegalArgumentException iae = new IllegalArgumentException
                    ("Cannot create object name for resource " + link);
                iae.initCause(e);
                throw iae;
            }
        }
        return results.toArray(new String[results.size()]);

    }

    // ------------------------------------------------------------- Operations


    /**
     * Add an environment entry for this web application.
     *
     * @param envName New environment entry name
     */
    public String addEnvironment(String envName, String type)
        throws MalformedObjectNameException {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return null;
        }
        ContextEnvironment env = nresources.findEnvironment(envName);
        if (env != null) {
            throw new IllegalArgumentException
                ("Invalid environment name - already exists '" + envName + "'");
        }
        env = new ContextEnvironment();
        env.setName(envName);
        env.setType(type);
        nresources.addEnvironment(env);

        // Return the corresponding MBean name
        return createObjectName(env).toString();

    }


    /**
     * Add a resource reference for this web application.
     *
     * @param resourceName New resource reference name
     */
    public String addResource(String resourceName, String type)
        throws MalformedObjectNameException {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return null;
        }
        ContextResource resource = nresources.findResource(resourceName);
        if (resource != null) {
            throw new IllegalArgumentException
                ("Invalid resource name - already exists'" + resourceName + "'");
        }
        resource = new ContextResource();
        resource.setName(resourceName);
        resource.setType(type);
        nresources.addResource(resource);

        // Return the corresponding MBean name
        return createObjectName(resource).toString();
    }

    /**
     * Add a resource link for this web application.
     *
     * @param resourceLinkName New resource link name
     */
    public String addResourceLink(String resourceLinkName, String global,
                String name, String type) throws MalformedObjectNameException {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return null;
        }
        ContextResourceLink resourceLink =
                                nresources.findResourceLink(resourceLinkName);
        if (resourceLink != null) {
            throw new IllegalArgumentException
                ("Invalid resource link name - already exists'" +
                                                        resourceLinkName + "'");
        }
        resourceLink = new ContextResourceLink();
        resourceLink.setGlobal(global);
        resourceLink.setName(resourceLinkName);
        resourceLink.setType(type);
        nresources.addResourceLink(resourceLink);

        // Return the corresponding MBean name
        return createObjectName(resourceLink).toString();
    }

    @Override
    public ObjectName createObjectName(String hostDomain, ObjectName parentName)
            throws MalformedObjectNameException
    {
        String onameStr;
        StandardHost hst=(StandardHost)getParent();

        String hostName=getParent().getName();
        String name= "//" + ((hostName==null)? "DEFAULT" : hostName) +
                (("".equals(encodedPath)) ? "/" : encodedPath);

        String suffix=",J2EEApplication=" +
                getJ2EEApplication() + ",J2EEServer=" +
                getJ2EEServer();

        onameStr="j2eeType=WebModule,name=" + name + suffix;
        if( log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Registering " + onameStr + " for " + oname);

        // default case - no domain explictely set.
        if( getDomain() == null ) domain=hst.getDomain();
        return new ObjectName(getDomain() + ":" + onameStr);
    }



    private void preRegisterJMX() {
        try {
            StandardHost host = (StandardHost) getParent();
              if ((oname == null)
                      || (oname.getKeyProperty("j2eeType") == null)) {
                  oname = createObjectName(host.getDomain(), host.getJmxName());
                  controller = oname;
              }
        } catch(Exception ex) {
            if (log.isLoggable(Level.INFO)) {
                String msg = MessageFormat.format(rb.getString(LogFacade.ERROR_UPDATING_CTX_INFO),
                        new Object[] {this, oname, ex.toString()});
                log.log(Level.INFO, msg, ex);
            }
        }
    }

    private void registerJMX() {
        try {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Checking for " + oname);
            }
            controller = oname;
             // Send j2ee.object.created notification
             if (this.getObjectName() != null) {
                 Notification notification = new Notification(
                         "j2ee.object.created", this, sequenceNumber++);
                 sendNotification(notification);
            }
            for (Container child : findChildren()) {
                ((StandardWrapper)child).registerJMX( this );
            }
        } catch (Exception ex) {

            String msg = MessageFormat.format(rb.getString(LogFacade.ERROR_REGISTERING_WRAPPER_INFO),
                                              new Object[] {this, oname, ex.toString()});
            log.log(Level.INFO, msg, ex);
        }
    }


    public void sendNotification(Notification notification) {

        if (broadcaster == null) {
            broadcaster = ((StandardEngine)getParent().getParent()).getService().getBroadcaster();
        }
        if (broadcaster != null) {
            broadcaster.sendNotification(notification);
        }
        return;
    }


    @Override
    public void init() throws Exception {

        if( this.getParent() == null ) {

            ContextConfig config = new ContextConfig();
            this.addLifecycleListener(config);
        }

        // It's possible that addChild may have started us
        if( initialized ) {
            return;
        }

        super.init();

        // START GlassFish 2439
        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(INIT_EVENT, null);
        // END GlassFish 2439

        // Send j2ee.state.starting notification
        if (this.getObjectName() != null) {
            Notification notification = new Notification("j2ee.state.starting", this, sequenceNumber++);
            sendNotification(notification);
        }

    }

    @Override
    public ObjectName getParentName() throws MalformedObjectNameException {
        // "Life" update
        String path=oname.getKeyProperty("name");
        if( path == null ) {
            log.log(Level.SEVERE, LogFacade.MISSING_ATTRIBUTE, getName());
            return null;
        }
        if( ! path.startsWith( "//")) {
            log.log(Level.SEVERE, LogFacade.MALFORMED_NAME, getName());
        }
        path=path.substring(2);
        int delim=path.indexOf( "/" );
        hostName="localhost"; // Should be default...
        if( delim > 0 ) {
            hostName=path.substring(0, delim);
            path = path.substring(delim);
            if ("/".equals(path)) {
                this.setName("");
            } else {
                this.setName(path);
            }
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Setting path " + path);
            }
            this.setName( path );
        }
        // XXX  The service and domain should be the same.
        String parentDomain=getEngineName();
        if( parentDomain == null ) parentDomain=domain;
        return new ObjectName( parentDomain + ":" +
                "type=Host,host=" + hostName);
    }

    public void create() throws Exception{
        init();
    }



    /**
     * Create an <code>ObjectName</code> for <code>ContextEnvironment</code> object.
     *
     * @param environment The ContextEnvironment to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    public ObjectName createObjectName(ContextEnvironment environment)
            throws MalformedObjectNameException {

        ObjectName name = null;
        Object container =
                environment.getNamingResources().getContainer();
        if (container instanceof Server) {
            name = new ObjectName(domain + ":type=Environment" +
                    ",resourcetype=Global,name=" + environment.getName());
        } else if (container instanceof Context) {
            String path = ((Context)container).getPath();
            if (path.length() < 1)
                path = "/";
            Host host = (Host) ((Context)container).getParent();
            name = new ObjectName(domain + ":type=Environment" +
                    ",resourcetype=Context,path=" + path +
                    ",host=" + host.getName() +
                    ",name=" + environment.getName());
        }

        return (name);

    }

    /**
     * Create an <code>ObjectName</code> for <code>ContextResource</code> object.
     *
     * @param resource The ContextResource to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    public ObjectName createObjectName(ContextResource resource)
            throws MalformedObjectNameException {

        ObjectName name = null;
        String encodedResourceName = urlEncoder.encode(resource.getName());
        Object container =
                resource.getNamingResources().getContainer();
        if (container instanceof Server) {
            name = new ObjectName(domain + ":type=Resource" +
                    ",resourcetype=Global,class=" + resource.getType() +
                    ",name=" + encodedResourceName);
        } else if (container instanceof Context) {
            String path = ((Context)container).getPath();
            if (path.length() < 1)
                path = "/";
            Host host = (Host) ((Context)container).getParent();
            name = new ObjectName(domain + ":type=Resource" +
                    ",resourcetype=Context,path=" + path +
                    ",host=" + host.getName() +
                    ",class=" + resource.getType() +
                    ",name=" + encodedResourceName);
        }

        return (name);

    }


    /**
     * Create an <code>ObjectName</code> for <code>ContextResourceLink</code> object.
     *
     * @param resourceLink The ContextResourceLink to be named
     *
     * @exception MalformedObjectNameException if a name cannot be created
     */
    public ObjectName createObjectName(ContextResourceLink resourceLink)
            throws MalformedObjectNameException {

        ObjectName name = null;
        String encodedResourceLinkName = urlEncoder.encode(resourceLink.getName());
        Object container =
                resourceLink.getNamingResources().getContainer();
        if (container instanceof Server) {
            name = new ObjectName(domain + ":type=ResourceLink" +
                    ",resourcetype=Global" +
                    ",name=" + encodedResourceLinkName);
        } else if (container instanceof Context) {
            String path = ((Context)container).getPath();
            if (path.length() < 1)
                path = "/";
            Host host = (Host) ((Context)container).getParent();
            name = new ObjectName(domain + ":type=ResourceLink" +
                    ",resourcetype=Context,path=" + path +
                    ",host=" + host.getName() +
                    ",name=" + encodedResourceLinkName);
        }

        return (name);

    }

    // ------------------------------------------------- ServletContext Methods

    /**
     * Return the value of the specified context attribute, if any;
     * otherwise return <code>null</code>.
     */
    @Override
    public Object getAttribute(String name) {
        return context.getAttribute(name);
    }

    /**
     * Return an enumeration of the names of the context attributes
     * associated with this context.
     */@Override
    public Enumeration<String> getAttributeNames() {
        return context.getAttributeNames();
    }

    /**
     * Returns the context path of the web application.
     */
    public String getContextPath() {
        return getPath();
    }

    /**
     * Return a <code>ServletContext</code> object that corresponds to a
     * specified URI on the server.
     */
    public ServletContext getContext(String uri) {

        // Validate the format of the specified argument
        if ((uri == null) || (!uri.startsWith("/"))) {
            return (null);
        }

        Context child = null;
        try {
            Host host = (Host) getParent();
            String mapuri = uri;
            while (true) {
                child = (Context) host.findChild(mapuri);
                if (child != null)
                    break;
                int slash = mapuri.lastIndexOf('/');
                if (slash < 0)
                    break;
                mapuri = mapuri.substring(0, slash);
            }
        } catch (Throwable t) {
            return (null);
        }

        if (child == null) {
            return (null);
        }

        if (getCrossContext()) {
            // If crossContext is enabled, can always return the context
            return child.getServletContext();
        } else if (child == this) {
            // Can still return the current context
            return getServletContext();
        } else {
            // Nothing to return
            return (null);
        }
    }

    /**
     * Return the value of the specified initialization parameter, or
     * <code>null</code> if this parameter does not exist.
     */
    public String getInitParameter(final String name) {
        return context.getInitParameter(name);
    }

    /**
     * Return the names of the context's initialization parameters, or an
     * empty enumeration if the context has no initialization parameters.
     */
    public Enumeration<String> getInitParameterNames() {
        return context.getInitParameterNames();
    }

    /**
     * @return true if the context initialization parameter with the given
     * name and value was set successfully on this ServletContext, and false
     * if it was not set because this ServletContext already contains a
     * context initialization parameter with a matching name
     */
    public boolean setInitParameter(String name, String value) {
        if (isContextInitializedCalled) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION),
                                              new Object[] {"setInitParameter", getName()});
            throw new IllegalStateException(msg);
        }
        return context.setInitParameter(name, value);
    }

    /**
     * Return the major version of the Java Servlet API that we implement.
     */
    @Override
    public int getMajorVersion() {
        return context.getMajorVersion();
    }

    /**
     * Return the minor version of the Java Servlet API that we implement.
     */
    @Override
    public int getMinorVersion() {
        return context.getMinorVersion();
    }

    /**
     * Return the MIME type of the specified file, or <code>null</code> if
     * the MIME type cannot be determined.
     */
    @Override
    public String getMimeType(String file) {

        if (file == null)
            return (null);
        int period = file.lastIndexOf(".");
        if (period < 0)
            return (null);
        String extension = file.substring(period + 1);
        if (extension.length() < 1)
            return (null);
        return (findMimeMapping(extension));

    }

    /**
     * Return a <code>RequestDispatcher</code> object that acts as a
     * wrapper for the named servlet.
     */
    @Override
    public RequestDispatcher getNamedDispatcher(String name) {

        // Validate the name argument
        if (name == null)
            return null;

        // Create and return a corresponding request dispatcher
        Wrapper wrapper = (Wrapper) findChild(name);
        if (wrapper == null)
            return null;

        return new ApplicationDispatcher(wrapper, null, null, null, null, null, name);

    }

    /**
     * Return the display name of this web application.
     */
    @Override
    public String getServletContextName() {
        return getDisplayName();
    }

    /**
     * Remove the context attribute with the specified name, if any.
     */
    @Override
    public void removeAttribute(String name) {
        context.removeAttribute(name);
    }

    /**
     * Bind the specified value with the specified context attribute name,
     * replacing any existing value for that name.
     */
    @Override
    public void setAttribute(String name, Object value) {
        context.setAttribute(name, value);
    }

    /**
     * Return the name and version of the servlet container.
     */
    @Override
    public String getServerInfo() {
        return context.getServerInfo();
    }

    /**
     * Return the real path corresponding to the given virtual path, or
     * <code>null</code> if the container was unable to perform the
     * translation
     */
    @Override
    public String getRealPath(String path) {
        if (!(showArchivedRealPathEnabled || directoryDeployed)) {
            return null;
        }

        if (!isFilesystemBased())
            return null;

        if (path == null) {
            return null;
        }

        File file = null;
        if (alternateDocBases == null
                || alternateDocBases.size() == 0) {
            file = new File(getBasePath(getDocBase()), path);
        } else {
            AlternateDocBase match = AlternateDocBase.findMatch(
                                                path, alternateDocBases);
            if (match != null) {
                file = new File(match.getBasePath(), path);
            } else {
                // None of the url patterns for alternate doc bases matched
                file = new File(getBasePath(getDocBase()), path);
            }
        }

        if (!file.exists()) {
            try {
                // Try looking up resource in
                // WEB-INF/lib/[*.jar]/META-INF/resources
                File f = getExtractedMetaInfResourcePath(path);
                if (f != null && f.exists()) {
                    file = f;
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (!file.exists()) {
            return null;
        } else {
            return file.getAbsolutePath();
        }
    }

    /**
     * Writes the specified message to a servlet log file.
     */
    @Override
    public void log(String message) {
        org.apache.catalina.Logger logger = getLogger();
        if (logger != null) {
            /* PWC 6403328
            logger.log(context.logName() + message, Logger.INFORMATION);
            */
            //START PWC 6403328
            logger.log(logName() + " ServletContext.log():" + message, org.apache.catalina.Logger.INFORMATION);
            //END PWC 6403328
        }
    }

    /**
     * Writes the specified exception and message to a servlet log file.
     */
    @Override
    public void log(Exception exception, String message) {
        org.apache.catalina.Logger logger = getLogger();
        if (logger != null)
            logger.log(exception, logName() + message);
    }

    /**
     * Writes the specified message and exception to a servlet log file.
     */
    @Override
    public void log(String message, Throwable throwable) {
        org.apache.catalina.Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + message, throwable);
    }

    @Override
    @Deprecated
    public Servlet getServlet(String name) {
        return context.getServlet(name);
    }

    @Deprecated
    @Override
    public Enumeration<String> getServletNames() {
        return context.getServletNames();
    }

    @Deprecated
    @Override
    public Enumeration<Servlet> getServlets() {
        return context.getServlets();
    }

    /**
     * Return the requested resource as an <code>InputStream</code>.  The
     * path must be specified according to the rules described under
     * <code>getResource</code>.  If no such resource can be identified,
     * return <code>null</code>.
     */
    @Override
    public InputStream getResourceAsStream(String path) {

        if (path == null || !path.startsWith("/"))
            return (null);

        path = RequestUtil.normalize(path);
        if (path == null)
            return (null);

        DirContext resources = null;

        if (alternateDocBases == null
                || alternateDocBases.isEmpty()) {
            resources = getResources();
        } else {
            AlternateDocBase match = AlternateDocBase.findMatch(
                                path, alternateDocBases);
            if (match != null) {
                resources = ContextsAdapterUtility.unwrap(match.getResources());
            } else {
                // None of the url patterns for alternate doc bases matched
                resources = getResources();
            }
        }

        if (resources != null) {
            try {
                Object resource = resources.lookup(path);
                if (resource instanceof Resource)
                    return (((Resource) resource).streamContent());
            } catch (Exception e) {
                // do nothing
            }
        }
        return (null);
    }

    /**
     * Return the URL to the resource that is mapped to a specified path.
     * The path must begin with a "/" and is interpreted as relative to the
     * current context root.
     */
    @Override
    public java.net.URL getResource(String path)
        throws MalformedURLException {

        if (path == null || !path.startsWith("/")) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INCORRECT_PATH), path);
            throw new MalformedURLException(msg);
        }

        path = RequestUtil.normalize(path);
        if (path == null)
            return (null);

        String libPath = "/WEB-INF/lib/";
        if ((path.startsWith(libPath)) && (path.endsWith(".jar"))) {
            File jarFile = null;
            if (isFilesystemBased()) {
                jarFile = new File(getBasePath(docBase), path);
            } else {
                jarFile = new File(getWorkPath(), path);
            }
            if (jarFile.exists()) {
                return jarFile.toURL();
            } else {
                return null;
            }

        } else {

            DirContext resources = null;
            if (alternateDocBases == null
                    || alternateDocBases.isEmpty()) {
                resources = context.getResources();
            } else {
                AlternateDocBase match = AlternateDocBase.findMatch(
                                                path, alternateDocBases);
                if (match != null) {
                    resources = ContextsAdapterUtility.unwrap(match.getResources());
                } else {
                    // None of the url patterns for alternate doc bases matched
                    resources = getResources();
                }
            }

            if (resources != null) {
                String fullPath = getName() + path;
                String hostName = getParent().getName();
                try {
                    resources.lookup(path);
                    return new java.net.URL
                        /* SJSAS 6318494
                        ("jndi", null, 0, getJNDIUri(hostName, fullPath),
                         */
                        // START SJAS 6318494
                        ("jndi", "", 0, getJNDIUri(hostName, fullPath),
                        // END SJSAS 6318494
		         new DirContextURLStreamHandler(resources));
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        return (null);
    }

    /**
     * Return a Set containing the resource paths of resources member of the
     * specified collection. Each path will be a String starting with
     * a "/" character. The returned set is immutable.
     */
    @Override
    public Set<String> getResourcePaths(String path) {
        // Validate the path argument
        if (path == null) {
            return null;
        }
        if (!path.startsWith("/")) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INCORRECT_PATH), path);
            throw new IllegalArgumentException(msg);
        }

        path = RequestUtil.normalize(path);
        if (path == null)
            return (null);

        DirContext resources = null;

        if (alternateDocBases == null
                || alternateDocBases.isEmpty()) {
            resources = getResources();
        } else {
            AlternateDocBase match = AlternateDocBase.findMatch(
                                path, alternateDocBases);
            if (match != null) {
                resources = ContextsAdapterUtility.unwrap(match.getResources());
            } else {
                // None of the url patterns for alternate doc bases matched
                resources = getResources();
            }
        }

        if (resources != null) {
            return (getResourcePathsInternal(resources, path));
        }

        return (null);
    }

    /**
     * Internal implementation of getResourcesPath() logic.
     *
     * @param resources Directory context to search
     * @param path Collection path
     */
    private Set<String> getResourcePathsInternal(DirContext resources,
                                                 String path) {
        HashSet<String> set = new HashSet<String>();
        try {
            listCollectionPaths(set, resources, path);
        } catch (NamingException e) {
            // Ignore, need to check for resource paths underneath
            // WEB-INF/lib/[*.jar]/META-INF/resources, see next
        }
        try {
            // Trigger expansion of bundled JAR files
            File file = getExtractedMetaInfResourcePath(path);
            if (file != null) {
                File[] children = file.listFiles();
                StringBuilder sb = null;
                for (File child : children) {
                    sb = new StringBuilder(path);
                    if (!path.endsWith("/")) {
                        sb.append("/");
                    }
                    sb.append(child.getName());
                    if (child.isDirectory()) {
                        sb.append("/");
                    }
                    set.add(sb.toString());
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Return a <code>RequestDispatcher</code> instance that acts as a
     * wrapper for the resource at the given path.  The path must begin
     * with a "/" and is interpreted as relative to the current context root.
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {

        // Validate the path argument
        if (path == null) {
            return null;
        }

        if (!path.startsWith("/") && !path.isEmpty()) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INCORRECT_OR_NOT_EMPTY_PATH), path);
            throw new IllegalArgumentException(msg);
        }

        // Get query string
        String queryString = null;
        int pos = path.indexOf('?');
        if (pos >= 0) {
            queryString = path.substring(pos + 1);
            path = path.substring(0, pos);
        }

        path = RequestUtil.normalize(path);
        if (path == null)
            return (null);

        pos = path.length();

        // Use the thread local URI and mapping data
        DispatchData dd = dispatchData.get();
        if (dd == null) {
            dd = new DispatchData();
            dispatchData.set(dd);
        }

        MessageBytes uriMB = dd.uriMB;
        uriMB.recycle();

        // Retrieve the thread local mapping data
        MappingData mappingData = dd.mappingData;

        // Map the URI
        CharChunk uriCC = uriMB.getCharChunk();
        try {
            uriCC.append(getPath(), 0, getPath().length());
            /*
             * Ignore any trailing path params (separated by ';') for mapping
             * purposes
             */
            int semicolon = path.indexOf(';');
            if (pos >= 0 && semicolon > pos) {
                semicolon = -1;
            }
            uriCC.append(path, 0, semicolon > 0 ? semicolon : pos);
            getMapper().map(uriMB, mappingData);
            if (mappingData.wrapper == null) {
                return (null);
            }
            /*
             * Append any trailing path params (separated by ';') that were
             * ignored for mapping purposes, so that they're reflected in the
             * RequestDispatcher's requestURI
             */
            if (semicolon > 0) {
                uriCC.append(path, semicolon, pos - semicolon);
            }
        } catch (Exception e) {
            // Should never happen
            log.log(Level.WARNING, LogFacade.MAPPING_ERROR_EXCEPTION, e);
            return (null);
        }

        Wrapper wrapper = (Wrapper) mappingData.wrapper;
        String wrapperPath = mappingData.wrapperPath.toString();
        String pathInfo = mappingData.pathInfo.toString();
        HttpServletMapping mappingForDispatch = new MappingImpl(mappingData);

        mappingData.recycle();

        // Construct a RequestDispatcher to process this request
        return new ApplicationDispatcher
            (wrapper, mappingForDispatch, uriCC.toString(), wrapperPath, pathInfo,
             queryString, null);
    }


    // ------------------------------------------------------------- Attributes


    /**
     * Return the naming resources associated with this web application.
     */
    public DirContext getStaticResources() {
        return getResources();
    }

    /**
     * Return the naming resources associated with this web application.
     * FIXME: Fooling introspection ...
     */
    public DirContext findStaticResources() {
        return getResources();
    }

    /**
     * Return the naming resources associated with this web application.
     */
    public String[] getWelcomeFiles() {
        return findWelcomeFiles();
    }

    /**
     * Set the validation feature of the XML parser used when
     * parsing xml instances.
     * @param webXmlValidation true to enable xml instance validation
     */
    @Override
    public void setXmlValidation(boolean webXmlValidation){
        this.webXmlValidation = webXmlValidation;
    }

    /**
     * Get the server.xml <context> attribute's xmlValidation.
     * @return true if validation is enabled.
     *
     */
    @Override
    public boolean getXmlValidation(){
        return webXmlValidation;
    }

    /**
     * Get the server.xml <context> attribute's xmlNamespaceAware.
     * @return true if namespace awareness is enabled.
     */
    @Override
    public boolean getXmlNamespaceAware(){
        return webXmlNamespaceAware;
    }

    /**
     * Set the namespace aware feature of the XML parser used when
     * parsing xml instances.
     * @param webXmlNamespaceAware true to enable namespace awareness
     */
    @Override
    public void setXmlNamespaceAware(boolean webXmlNamespaceAware){
        this.webXmlNamespaceAware= webXmlNamespaceAware;
    }

    /**
     * Set the validation feature of the XML parser used when
     * parsing tlds files.
     * @param tldValidation true to enable xml instance validation
     */
    @Override
    public void setTldValidation(boolean tldValidation){
        this.tldValidation = tldValidation;
    }

    /**
     * Get the server.xml <context> attribute's webXmlValidation.
     * @return true if validation is enabled.
     *
     */
    @Override
    public boolean getTldValidation(){
        return tldValidation;
    }

    /**
     * Get the server.xml <host> attribute's xmlNamespaceAware.
     * @return true if namespace awarenes is enabled.
     */
    @Override
    public boolean getTldNamespaceAware(){
        return tldNamespaceAware;
    }

    /**
     * Set the namespace aware feature of the XML parser used when
     * parsing xml instances.
     * @param tldNamespaceAware true to enable namespace awareness
     */
    @Override
    public void setTldNamespaceAware(boolean tldNamespaceAware){
        this.tldNamespaceAware= tldNamespaceAware;
    }

    /**
     * Sets the list of ordered libs, which will be used as the value of the
     * ServletContext attribute with name javax.servlet.context.orderedLibs
     */
    public void setOrderedLibs(List<String> orderedLibs) {
        this.orderedLibs = orderedLibs;
    }

    public void startRecursive() throws LifecycleException {
        // nothing to start recursively, the servlets will be started by
        // load-on-startup
        start();
    }

    /**
     * Returns the state of the server.
     * <p>
     * It will return 0, 1 3 or 4.
     * @return 0 = STARTING; 1= RUNNING; 3 = STOPPED, 4 = FAILED
     */
    public int getState() {
        if( started ) {
            return 1; // RUNNING
        }
        if( initialized ) {
            return 0; // starting ?
        }
        if( ! available ) {
            return 4; //FAILED
        }
        // 2 - STOPPING
        return 3; // STOPPED
    }

    /**
     * Checks if the context has been initialised.
     * If it has, then no servlets or servlet filters can be added to it.
     * @return
     */
    boolean isContextInitializedCalled() {
        return isContextInitializedCalled;
    }

    /**
     * Creates an ObjectInputStream that provides special deserialization
     * logic for classes that are normally not serializable (such as
     * javax.naming.Context).
     * @param is
     */
    public ObjectInputStream createObjectInputStream(InputStream is)
            throws IOException {

        ObjectInputStream ois = null;

        Loader loader = getLoader();
        if (loader != null) {
            ClassLoader classLoader = loader.getClassLoader();
            if (classLoader != null) {
                try {
                    ois = new CustomObjectInputStream(is, classLoader);
                } catch (IOException ioe) {
                    log.log(Level.SEVERE, LogFacade.CANNOT_CREATE_OBJECT_INPUT_STREAM, ioe);
                }
            }
        }

        if (ois == null) {
            ois = new ObjectInputStream(is);
        }

        return ois;
    }

    /**
     * Creates an ObjectOutputStream that provides special serialization
     * logic for classes that are normally not serializable (such as
     * javax.naming.Context).
     * @param os
     * @return
     */
    public ObjectOutputStream createObjectOutputStream(OutputStream os)
            throws IOException {
        return new ObjectOutputStream(os);
    }

    /**
     * Gets the time this context was started.
     *
     * @return Time (in milliseconds since January 1, 1970, 00:00:00) when this
     * context was started
     */
    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    /**
     * ???
     * @return false
     */
    public boolean isEventProvider() {
        return false;
    }

    /**
     * ???MBean stats? unused
     * @return false
     */
    public boolean isStatisticsProvider() {
        return false;
    }

    /*
     * HTTP session related monitoring events
     */

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider
     * @param session
     */
    public void sessionCreatedEvent(HttpSession session) {
        // Deliberate noop
    }

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider#sessionDestroyedEvent
     * @param session
     */
    public void sessionDestroyedEvent(HttpSession session) {
        // Deliberate noop
    }

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider#sessionRejectedEvent
     * @param maxSessions
     */
    public void sessionRejectedEvent(int maxSessions) {
        // Deliberate noop
    }

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider#sessionExpiredEvent
     * @param session
     */
    public void sessionExpiredEvent(HttpSession session) {
        // Deliberate noop
    }

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider#sessionPersistedStartEvent
     * @param session
     */
    public void sessionPersistedStartEvent(HttpSession session) {
        // Deliberate noop
    }

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider#sessionPersistedEndEvent
     * @param session
     */
    public void sessionPersistedEndEvent(HttpSession session) {
        // Deliberate noop
    }

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider#sessionActivatedStartEvent
     * @param session
     */
    public void sessionActivatedStartEvent(HttpSession session) {
        // Deliberate noop
    }

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider#sessionActivatedEndEvent
     * @param session
     */
    public void sessionActivatedEndEvent(HttpSession session) {
        // Deliberate noop
    }

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider#sessionPassivatedStartEvent
     * @param session
     */
    public void sessionPassivatedStartEvent(HttpSession session) {
        // Deliberate noop
    }

    /**
     * Trigger for monitoring
     * @see org.glassfish.web.admin.monitor.SessionStatsProvider#sessionPassivatedEndEvent
     * @param session
     */
    public void sessionPassivatedEndEvent(HttpSession session) {
        // Deliberate noop
    }

    /**
     *
     * @return 0
     */
    public long getUniqueId() {
        return 0L;
    }

    public static class RestrictedServletContextListener
            implements ServletContextListener {

        /*
         * The ServletContextListener to which to delegate
         */
        private ServletContextListener delegate;

        /**
         * Constructor
         * @param delegate
         */
        public RestrictedServletContextListener(
                ServletContextListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            delegate.contextInitialized(sce);
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            delegate.contextDestroyed(sce);
        }

        public ServletContextListener getNestedListener() {
            return delegate;
        }
    }

    /**
     * Instantiates the given Servlet class.
     *
     * @return the new Servlet instance
     */
    protected <T extends Servlet> T createServletInstance(Class<T> clazz)
            throws Exception{
        return clazz.newInstance();
    }

    /**
     * Instantiates the given Filter class.
     *
     * @return the new Filter instance
     */
    protected <T extends Filter> T createFilterInstance(Class<T> clazz)
            throws Exception{
        return clazz.newInstance();
    }

    /**
     * Instantiates the given EventListener class.
     *
     * @return the new EventListener instance
     */
    public <T extends EventListener> T createListenerInstance(
                Class<T> clazz) throws Exception{
        return clazz.newInstance();
    }

    /**
     * Instantiates the given HttpUpgradeHandler class.
     *
     * @param clazz
     * @param <T>
     * @return a new T instance
     * @throws Exception
     */
    public <T extends HttpUpgradeHandler> T createHttpUpgradeHandlerInstance(Class<T> clazz)
            throws Exception {
        return clazz.newInstance();
    }

    /**
     * Custom security manager responsible for enforcing permission
     * check on ServletContext#getClassLoader if necessary.
     */
    private static class MySecurityManager extends SecurityManager {

        /*
         * @return true if the specified class loader <code>cl</code>
         * can be found in the class loader delegation chain of the
         * <code>start</code> class loader, false otherwise
         */
        boolean isAncestor(ClassLoader start, ClassLoader cl) {
            ClassLoader acl = start;
            do {
	        acl = acl.getParent();
                if (cl == acl) {
                    return true;
                }
            } while (acl != null);
            return false;
        }

        /*
         * Checks whether access to the webapp class loader associated
         * with this Context should be granted to the caller of
         * ServletContext#getClassLoader.
         *
         * If no security manager exists, this method returns immediately.
         *
         * Otherwise, it calls the security manager's checkPermission
         * method with the getClassLoader permission if the class loader
         * of the caller of ServletContext#getClassLoader is not the same as,
         * or an ancestor of the webapp class loader associated with this
         * Context.
         */
        void checkGetClassLoaderPermission(ClassLoader webappLoader) {
            SecurityManager sm = System.getSecurityManager();
            if (sm == null) {
                return;
            }

            // Get the current execution stack as an array of classes
            Class[] classContext = getClassContext();

            /*
             * Determine the caller of ServletContext#getClassLoader:
             *
             * classContext[0]:
             *   org.apache.catalina.core.StandardContext$MySecurityManager
             * classContext[1]:
             *   org.apache.catalina.core.StandardContext
             * classContext[2]:
             *   org.apache.catalina.core.StandardContext
             * classContext[3]:
             *   org.apache.catalina.core.ApplicationContext
             * classContext[4]:
             *  org.apache.catalina.core.ApplicationContextFacade
             * classContext[5]:
             *  Caller whose classloader to check
             *
             * NOTE: INDEX MUST BE ADJUSTED WHENEVER EXECUTION STACK
             * CHANGES, E.G., DUE TO CODE BEING REORGANIZED
             */
            ClassLoader ccl = classContext[5].getClassLoader();
            if (ccl != null && ccl != webappLoader &&
                    !isAncestor(webappLoader, ccl)) {
                sm.checkPermission(GET_CLASSLOADER_PERMISSION);
            }
        }
    }

    private static class PrivilegedCreateSecurityManager
            implements PrivilegedAction<MySecurityManager> {

        public MySecurityManager run() {
            return new MySecurityManager();
        }
    }

    /**
     * List resource paths (recursively), and store all of them in the given
     * Set.
     */
    private static void listCollectionPaths
        (Set<String> set, DirContext resources, String path)
        throws NamingException {

        Enumeration<Binding> childPaths = resources.listBindings(path);
        while (childPaths.hasMoreElements()) {
            Binding binding = childPaths.nextElement();
            String name = binding.getName();
            StringBuilder childPath = new StringBuilder(path);
            if (!"/".equals(path) && !path.endsWith("/"))
                childPath.append("/");
            childPath.append(name);
            Object object = binding.getObject();
            if (object instanceof DirContext &&
                    childPath.charAt(childPath.length() -1) != '/') {
                childPath.append("/");
            }
            set.add(childPath.toString());
        }
    }

    /**
     * Get full path, based on the host name and the context path.
     */
    private static String getJNDIUri(String hostName, String path) {
        if (!path.startsWith("/"))
            return "/" + hostName + "/" + path;
        else
            return "/" + hostName + path;
    }

    /**
     * Internal class used as thread-local storage when doing path
     * mapping during dispatch.
     */
    private static final class DispatchData {
        public MessageBytes uriMB;
        public MappingData mappingData;

        public DispatchData() {
            uriMB = MessageBytes.newInstance();
            CharChunk uriCC = uriMB.getCharChunk();
            uriCC.setLimit(-1);
            mappingData = new MappingData();
        }
    }

    /**
     * Get resource from META-INF/resources/ in jars.
     */
    private File getExtractedMetaInfResourcePath(String path) {
        path = Globals.META_INF_RESOURCES + path;

        ClassLoader cl = getLoader().getClassLoader();
        if (cl instanceof WebappClassLoader) {
            return ((WebappClassLoader)cl).getExtractedResourcePath(path);
        }
        return null;
    }
}
