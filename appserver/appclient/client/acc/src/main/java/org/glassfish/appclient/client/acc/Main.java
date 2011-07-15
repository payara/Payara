/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client.acc;

//import com.sun.appserv.naming.S1ASCtxFactory;
//import com.sun.enterprise.appclient.AppContainer;
//import com.sun.enterprise.appclient.HttpAuthenticator;
//import com.sun.enterprise.appclient.jws.TemplateCache;
//import com.sun.enterprise.appclient.jws.Util;
//import com.sun.enterprise.appclient.jws.boot.ClassPathManager;
//import com.sun.enterprise.config.clientbeans.CertDb;
//import com.sun.enterprise.config.clientbeans.ClientBeansFactory;
//import com.sun.enterprise.config.clientbeans.ClientContainer;
//import com.sun.enterprise.config.clientbeans.ClientCredential;
//import com.sun.enterprise.config.clientbeans.ElementProperty;
//import com.sun.enterprise.config.clientbeans.Security;
//import com.sun.enterprise.config.clientbeans.Ssl;
//import com.sun.enterprise.config.clientbeans.TargetServer;
//import com.sun.enterprise.config.ConfigContext;
//import com.sun.enterprise.config.ConfigException;
//import com.sun.enterprise.config.ConfigFactory;
//import com.sun.enterprise.connectors.ActiveResourceAdapter;
//import com.sun.enterprise.connectors.ConnectorRegistry;
//import com.sun.enterprise.connectors.ConnectorRuntime;
//import com.sun.enterprise.deployment.*;
//import com.sun.enterprise.deployment.archivist.AppClientArchivist;
//import com.sun.enterprise.deployment.archivist.ApplicationArchivist;
//import com.sun.enterprise.deployment.archivist.Archivist;
//import com.sun.enterprise.deployment.archivist.ArchivistFactory;
//import com.sun.enterprise.deployment.deploy.shared.AbstractArchive;
//import com.sun.enterprise.deployment.deploy.shared.Archive;
//import com.sun.enterprise.deployment.deploy.shared.ArchiveFactory;
//import com.sun.enterprise.deployment.deploy.shared.FileArchive;
//import com.sun.enterprise.deployment.deploy.shared.InputJarArchive;
//import com.sun.enterprise.deployment.interfaces.SecurityRoleMapperFactory;
//import com.sun.enterprise.deployment.interfaces.SecurityRoleMapperFactoryMgr;
//import com.sun.enterprise.util.SystemPropertyConstants;
//import com.sun.enterprise.webservice.ClientPipeCloser;
//import com.sun.enterprise.InjectionManager;
//import com.sun.enterprise.J2EESecurityManager;
//import com.sun.enterprise.loader.ASURLClassLoader;
//import com.sun.enterprise.loader.InstrumentableClassLoader;
//import com.sun.enterprise.naming.ProviderManager;
//import com.sun.enterprise.security.GUIErrorDialog;
//import com.sun.enterprise.security.SSLUtils;
//import com.sun.enterprise.server.logging.ACCLogManager;
//import com.sun.enterprise.Switch;
//import com.sun.enterprise.util.FileUtil;
//import com.sun.enterprise.util.i18n.StringManager;
//import com.sun.enterprise.util.JarClassLoader;
//import com.sun.enterprise.util.ORBManager;
//import com.sun.enterprise.util.shared.ArchivistUtils;
//import com.sun.enterprise.util.Utility;
//import com.sun.logging.LogDomains;
//import com.sun.web.server.HttpsURLStreamHandlerFactory;
//import java.awt.EventQueue;
//import java.awt.Toolkit;
//
//import java.io.*;
//import java.lang.reflect.InvocationTargetException;
//import java.net.*;
//import java.text.MessageFormat;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.Set;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.jar.JarFile;
//import java.util.logging.Handler;
//import java.util.logging.Level;
//import java.util.logging.LogRecord;
//import java.util.logging.Logger;
//import java.util.logging.LogManager;
//import java.util.Properties;
//import java.util.Vector;
//import java.util.logging.MemoryHandler;
//import java.util.logging.SimpleFormatter;
//import javax.enterprise.deploy.shared.ModuleType;
//import javax.naming.InitialContext;
//
//import javax.security.auth.message.config.AuthConfigFactory;
//import com.sun.enterprise.security.jmac.config.GFAuthConfigFactory;
//import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
//import com.sun.enterprise.security.UsernamePasswordStore;
//import javax.swing.SwingUtilities;

/**
 * This is the main program invoked from the command line.
 *
 * It processes the command line arguments to prepare the Map of options, then
 * passes the Map to an instance of the embedded ACC.
 */
public class Main {
    
    private static final String CLIENT = "-client";
    private static final String NAME = "-name";
    private static final String MAIN_CLASS = "-mainclass";
    private static final String TEXT_AUTH = "-textauth";
    private static final String XML_PATH = "-xml";
    private static final String ACC_CONFIG_XML = "-configxml";    
    private static final String DEFAULT_CLIENT_CONTAINER_XML = "sun-acc.xml";
    // duplicated in com.sun.enterprise.jauth.ConfigXMLParser
    private static final String SUNACC_XML_URL = "sun-acc.xml.url";
    private static final String NO_APP_INVOKE = "-noappinvoke";
    //Added for allow user to pass user name and password through command line.
    private static final String USER = "-user";
    private static final String PASSWORD = "-password";
    private static final String PASSWORD_FILE = "-passwordfile";
    private static final String LOGIN_NAME = "j2eelogin.name";
    private static final String LOGIN_PASSWORD = "j2eelogin.password";
    private static final String DASH = "-";

    private static final String lineSep = System.getProperty("line.separator");
    
//    /**
//     * Property names used on the server to send these values to a Java Web Start client
//     * and by the ACC when running under Java Web Start to retrieve them
//     */
//    public static final String APPCLIENT_IIOP_DEFAULTHOST_PROPERTYNAME = "com.sun.aas.jws.iiop.defaultHost";
//    public static final String APPCLIENT_IIOP_DEFAULTPORT_PROPERTYNAME = "com.sun.aas.jws.iiop.defaultPort";
//    public static final String APPCLIENT_IIOP_FAILOVER_ENDPOINTS_PROPERTYNAME = "com.sun.aas.jws.iiop.failover.endpoints";
//    public static final String APPCLIENT_PROBE_CLASSNAME_PROPERTYNAME = "com.sun.aas.jws.probeClassName";
//
//    /** Prop name for keeping temporary files */
//    public static final String APPCLIENT_RETAIN_TEMP_FILES_PROPERTYNAME = "com.sun.aas.jws.retainTempFiles";
//
//    /** property name used to indicate that Java Web Start is active */
//    public static final String APPCLIENT_ISJWS_PROPERTYNAME = "com.sun.aas.jws.isJWS";
//
//    /** property name used to indicate the Java Web Start download host name */
//    public static final String APPCLIENT_DOWNLOAD_HOST_PROPERTYNAME = "com.sun.aas.jws.download.host";
    
//    /** Prop used when running under Java Web Start to point to a temporarily-created default file.
//     *This property appears in the template for the default sun-acc.xml content.  Logic below
//     *assigns a value to it and then uses it to substitute in the template to create the
//     *actual content.  (This is not a property set in the environment and then retrieved by Main.)
//    */
//    public static final String SUN_ACC_SECURITY_CONFIG_PROPERTY = "security.config.file";
//
//    /** Used for constructing the name of the temp file that will hold the login conf. content */
//    private static final String LOGIN_CONF_FILE_PREFIX = "login";
//    private static final String LOGIN_CONF_FILE_SUFFIX = ".conf";
//
//    /** The system property to be set that is later read by jaas */
//    private static final String LOGIN_CONF_PROPERTY_NAME = "java.security.auth.login.config";
//
//    /** Names of templates for default config for Java Web Start */
//    private static final String DEFAULT_TEMPLATE_PREFIX = "jws/templates/";
//    private static final String SUN_ACC_DEFAULT_TEMPLATE = DEFAULT_TEMPLATE_PREFIX + "default-sun-accTemplate.xml";
//    private static final String WSS_CLIENT_CONFIG_TEMPLATE = DEFAULT_TEMPLATE_PREFIX + "default-wss-client-configTemplate.xml";
//    private static final String LOGIN_CONF_TEMPLATE = DEFAULT_TEMPLATE_PREFIX + "appclientlogin.conf";
//
//    /** Naming for temporary files created under Java Web Start */
//    private static final String WSS_CLIENT_CONFIG_PREFIX = "wsscc";
//    private static final String WSS_CLIENT_CONFIG_SUFFIX = ".xml";
//    private static final String SUN_ACC_PREFIX = "sunacc";
//    private static final String SUN_ACC_SUFFIX = ".xml";
//
//    /** Paths to persistence JARs added to the classpath for the class loader created by the ACC */
//    private static final String [] LOCAL_LAUNCH_PERSISTENCE_JAR_PATHS = new String[] {
//        System.getProperty(SystemPropertyConstants.DERBY_ROOT_PROPERTY) + File.separator + "lib" + File.separator + "derbyclient.jar",
//        System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY) + File.separator + "lib" + File.separator + "toplink-essentials.jar",
//        System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY) + File.separator + "lib" + File.separator + "dbschema.jar"};
//
//    private static final String ORB_INITIAL_HOST_PROPERTYNAME = "org.omg.CORBA.ORBInitialHost";
//    private static final String ORB_INITIAL_PORT_PROPERTYNAME = "org.omg.CORBA.ORBInitialPort";
//
//    protected static Logger _logger;
//
//    protected final boolean debug = false;
//    protected StringManager localStrings =
//                            StringManager.getManager(Main.class);
//    protected boolean guiAuth = false; // unless the user sets the auth.gui prop and does not use -textauth
//    protected boolean runClient=true;
//
//    protected String host;
//
//    protected String port;
//
//    /** Saved arguments so they are accessible from the AWT thread if needed */
//    protected String [] args;
//
//    /** Records whether ACC is currently running under Java Web Start */
//    protected boolean isJWS;
//
//    /** Records whether temp config files created while running under Java Web Start should be retained */
//    protected boolean retainTempFiles = false;
//
//    protected String clientJar = null;
//    protected String displayName = null;
//    protected String mainClass = null;
//    protected String xmlPath = null;
//    protected String accConfigXml = null;
//    protected String jwsACCConfigXml = null;
//    protected Vector<String> appArgs = new Vector<String>();
//    protected String classFileFromCommandLine = null;
//    protected boolean useTextAuth = false;
//
//    private static boolean lb_enabled = false;
//
//    /**
//     * Indicates whether ORB load balancing is turned on.
//     *<p>
//     * Should not be invoked until after setTargetServerProperties has been
//     * invoked.  This method is intended for use by ORB initializers, which in
//     * turn are run during the instantiation of the InitialContext.  The
//     * InitialContext is created (in the constructor for this class) after
//     * setTargetServerProperties has been run, so this precondition should
//     * be met.
//     * @return true if load balancing was specified or implied, false otherwise
//     */
//    public static boolean isLoadBalancingEnabled() {
//        return lb_enabled;
//    }
//
//    public Main(String[] args) {
//        /*
//         *The appclient script triggers this constructor, so find the persistence
//         *JARs in the local directories.  During a Java Web Start launch the
//         *other constructor is invoked with the already-located JAR URLs.
//         */
//        this(args, locateLocalPersistenceJARs());
//    }
//
//    public Main(String[] args, URL[] persistenceJarURLs) {
//        /*
//         *Temporarily use a cheap memory logger to hold any messages created
//         *until we can configure the real logger using the configuration settings.
//         */
//        BufferedLogger tempLogger = prepareBufferedLogging();
//
//        /*
//         *Assign the temp logger to _logger so methods that need to log don't
//         *need to know if they are doing so before the real logger is set up.
//         */
//        _logger = tempLogger;
//
//        prepareJWSSettings();
//
//	try {
//            /*
//             *Handle any command line arguments intended for the ACC (as opposed to
//             *the client itself) and save the returned args intended for the client.
//             */
//            appArgs = processCommandLine(args);
//
//            /*
//             *Find the class name to be run if the user has asked to run a .class
//             *file as the client.
//             */
//            String classNameForClassFile = determineClassNameForClassFileLaunch(clientJar, isJWS, mainClass, classFileFromCommandLine);
//
//            /*
//             *Choose and validate the XML configuration file to use, which depends on the
//             *-xml and -configxml command line arguments as well as whether this
//             *is a Java Web Start launch or not.
//             */
//            String xmlPathToUse = chooseConfigFile(xmlPath, accConfigXml, isJWS);
//            validateXMLFile(xmlPathToUse);
//
//            /*
//             *The configuration file just chosen may assign logging settings.
//             *Prepare logger using those settings and also flush any records sent to the
//             *temp logger to the new, real one.
//             */
//            _logger = prepareLogging(tempLogger, xmlPathToUse, isJWS);
//
//            /*
//             *Locate the app client: in a user-specified jar or directory,
//             *an implied directory (i.e., user is running a .class file), or the
//             *Java Web Start downloaded jar file.
//             */
//            File appClientJarOrDir = locateAppclientJarOrDir(clientJar, classNameForClassFile, isJWS);
//
//            /*
//             *Set up the default login config for a Java Web Start launch.
//             */
//            prepareJWSLoginConfig();
//
//            Utility.checkJVMVersion();
//            prepareSecurity();
//
//            Throwable terminatingException = null;
//            AppClientInfo appClientInfo = null;
//
//            ClassLoader jcl = null;
//
//	    Switch.getSwitch().setProviderManager(ProviderManager.getProviderManager());
//            /*
//             * Set the container type before initializing the ORB so that ORB
//             * initializers can correctly identify which container type this is.
//             */
//            Switch.getSwitch().setContainerType(Switch.APPCLIENT_CONTAINER);
//
//	    // added for ClientContainer.xml initialization
//            // The returned properties will be null if load balancing is on or
//            // non-null and containing the initial host and port properties if
//            // load balancing is off.
//            //
//            // Note that setTargetServerProperties must be invoked before the
//            // the initial context is instantiated.
//	    Properties iiopProperties = setTargetServerProperties(xmlPathToUse);
//
//	    int exitCode = 0; // 0 for success
//            prepareURLStreamHandling();
//
//            /*
//             *The "info" object for the app client collects together common behavior
//             *regardless of how the app client was specified (as an archive file,
//             *as a directory, or as a .class file) and - if not a .class file -
//             *what type of module (an ear or an app client jar).
//             *
//             *Creating this info object also may extract the ear or jar into
//             *a temporary directory, for example for use by persistence unit
//             *handling, and may also prepare the persistence unit handling if
//             *that is required.
//             */
//            // log the endpoint address(es) to be used, per Jagadesh's request
//            _logger.log(Level.INFO, "acc.endpoints",
//                    lb_enabled ? System.getProperty(S1ASCtxFactory.IIOP_ENDPOINTS_PROPERTY) :
//                        iiopProperties.getProperty(ORB_INITIAL_HOST_PROPERTYNAME) + ":" +
//                        iiopProperties.getProperty(ORB_INITIAL_PORT_PROPERTYNAME));
//
//            //ic creation for enabling the ORB intialization with the
//	    //right host:port values (bug 6397533).  The InitialContext must
//            //not be instantiated before setTargetServerProperties has run.
//	    InitialContext ic = AppContainer.initializeNaming(iiopProperties);
//
//            appClientInfo = AppClientInfoFactory.buildAppClientInfo(isJWS, _logger, appClientJarOrDir, mainClass, displayName, classFileFromCommandLine, persistenceJarURLs);
//
//            ApplicationClientDescriptor appDesc = appClientInfo.getAppClient();
//
//            final AppContainer container = createAppContainer(appDesc, guiAuth);
//            Cleanup cleanup = prepareShutdownCleanup(container, appClientInfo);
//
//            // Set the authenticator which is called back when a
//            // protected web resource is requested and authentication data is
//            // needed.
//            Authenticator.setDefault(new HttpAuthenticator(container));
//
//            /*
//             *The container needs to use the new classloader to locate any
//             *user-specified security callback handler.
//             */
//            jcl = appClientInfo.getClassLoader();
//            String appMainClass = container.preInvoke(ic, jcl);
//            /*
//             *If the user explicitly indicated the main class to be run, use
//             *that value, not the value from the manifest.
//             */
//            if (mainClass != null) {
//                appMainClass = mainClass;
//            }
//
//            /*
//             *The new classloader must be set up as the current context class
//             *loader for injection to work.  During a Java Web Start launch
//             *the class loader in the event dispatcher thread must also be
//             *changed.
//             */
//            Thread.currentThread().setContextClassLoader(jcl);
//            if (isJWS) {
//                setClassLoaderForEDT(jcl);
//            }
//
//            /*
//             *Load the main class of the app client.
//             */
//            Class cl = loadMainClientClass(jcl, appMainClass);
//
//            //This is required for us to enable interrupt jaxws service
//            //creation calls
//            System.setProperty("javax.xml.ws.spi.Provider",
//                               "com.sun.enterprise.webservice.spi.ProviderImpl");
//
//            // Inject the application client's injectable resources.  This
//            // must be done after java:comp/env is initialized but before
//            // the application client's main class is invoked.  Also make
//            // sure the injection mgr will clean up during shutdown.
//            InjectionManager injMgr = Switch.getSwitch().getInjectionManager();
//            cleanup.setInjectionManager(injMgr, cl, appDesc);
//
//            injMgr.injectClass(cl, appDesc);
//
//            /*
//             Try to locate a script with the same name as the main class,
//             but with a file type matching the file types handled by one of the
//             scripting engines currently available.
//             */
//            ScriptingSupport scriptingSupport = new ScriptingSupport(jcl, _logger);
//            cleanup.setScriptingSupport(scriptingSupport);
//            boolean runMainClass = true;
//
//            if(runClient) {
//                /*
//                 Prefer a script instead of the main class if a script is present.
//                 */
//                runMainClass = ! scriptingSupport.startScript(appMainClass, appArgs);
//
//                /*
//                 Do not close the stream yet because the scripting
//                 engine runs on the Swing event dispatcher thread and
//                 may not be done reading it yet.
//                 */
//
//                if (runMainClass) {
//                    /*
//                     Run the main class if there is no script of the expected
//                     name or if the attempt to start the script failed.
//                     */
//                    String[] applicationArgs = appArgs.toArray(new String[appArgs.size()]);
//                    Utility.invokeApplicationMain(cl, applicationArgs);
//                    _logger.info("Application main() returned; GUI elements may be continuing to run");
//                }
//            }
//
//
//            // System.exit is not called if application main returned
//            // without error.  Registered shutdown hook will perform
//            // container cleanup
//        } catch (java.lang.reflect.InvocationTargetException ite) {
//            Throwable tt = ite.getTargetException();
//            _logger.log(Level.WARNING, "acc.app_exception", tt);
//            throw new RuntimeException(ite);
//        } catch (UserError ue) {
//            ue.displayAndExit();
//        } catch (Throwable t) {
//            if (t instanceof javax.security.auth.login.FailedLoginException){
//
//               _logger.info("acc.login_error");
//                boolean isGui =
//                    Boolean.valueOf
//                        (System.getProperty ("auth.gui","true")).booleanValue();
//                String errorMessage =
//                    localStrings.getString
//                        ("main.exception.loginError",
//                         "Incorrect login and/or password");
//
//                if (isGui) {
//                    GUIErrorDialog ged = new GUIErrorDialog (errorMessage);
//                    ged.show ();
//                }
//            }
//
//            _logger.log(Level.WARNING, "acc.app_exception", t);
//
//            if (t instanceof javax.naming.NamingException) {
//                _logger.log(Level.WARNING, "acc.naming_exception_received");
//            }
//            throw new RuntimeException(t);
//        } finally {
//	    shutDownSystemAdapters();
//        }
//    }
//
//    /**
//     *Sets the class loader for the event dispatcher thread in case of a
//     *Java Web Start launch.
//     */
//    private void setClassLoaderForEDT(final ClassLoader loader)
//            throws InterruptedException, InvocationTargetException {
//        SwingUtilities.invokeAndWait(new Runnable() {
//            private ClassLoader theCL;
//            public void run() {
//                Thread.currentThread().setContextClassLoader(loader);
//                Toolkit tk = Toolkit.getDefaultToolkit();
//                EventQueue eq = tk.getSystemEventQueue();
//                eq.push(new EventQueue());
//            }
//          }
//        );
//    }
//
//    private static URL[] locateLocalPersistenceJARs() {
//        try {
//            URL[] localPersistenceJARURLs = new URL[LOCAL_LAUNCH_PERSISTENCE_JAR_PATHS.length];
//            int slot = 0;
//            for (String jarPath : LOCAL_LAUNCH_PERSISTENCE_JAR_PATHS) {
//                File jarFile = new File(jarPath);
//                localPersistenceJARURLs[slot++] = jarFile.toURI().toURL();
//            }
//            return localPersistenceJARURLs;
//        } catch (MalformedURLException mue) {
//            /*
//             *This is not a recoverable error, so to minimize changes convert it
//             *to an unchecked runtime exception.
//             */
//            throw new RuntimeException(mue);
//        }
//    }
//
//    /**
//     *Chooses behavior of a Java Web Start launch based on property settings.
//     *<p>
//     *Sets the instance variables isJWS and retainTempFiles as side-effects
//     */
//    private void prepareJWSSettings() {
//        isJWS = Boolean.getBoolean(APPCLIENT_ISJWS_PROPERTYNAME);
//        retainTempFiles = Boolean.getBoolean(APPCLIENT_RETAIN_TEMP_FILES_PROPERTYNAME);
//    }
//
//    /**
//     *Set up the security manager and keystores/truststores.
//     */
//    private void prepareSecurity() {
//        /*
//         *If -textauth was specified, no need to look for the auth.gui
//         *setting since -textauth takes precedence.
//         */
//        if ( ! useTextAuth) {
//            guiAuth = Boolean.valueOf
//            (System.getProperty("auth.gui", "true")).booleanValue();
//        }
//
//        /* security init */
//        SecurityManager secMgr = System.getSecurityManager();
//        if (!isJWS && secMgr != null &&
//                !(J2EESecurityManager.class.equals(secMgr.getClass()))) {
//            J2EESecurityManager mgr = new J2EESecurityManager();
//            System.setSecurityManager(mgr);
//        }
//        if (_logger.isLoggable(Level.INFO)) {
//            if (secMgr != null) {
//                _logger.info("acc.secmgron");
//            } else {
//                _logger.info("acc.secmgroff");
//            }
//        }
//
//        try{
//            /* setup keystores.
//             * This is required, for appclients that want to use SSL, especially
//             * HttpsURLConnection to perform Https.
//             */
//            SSLUtils.initStoresAtStartup();
//        } catch (Exception e){
//             /* This is not necessarily an error. This will arise
//              * if the user has not specified keystore/truststore properties.
//              * and does not want to use SSL.
//              */
//            if(_logger.isLoggable(Level.FINER)){
//                // show the exact stack trace
//                _logger.log(Level.FINER, "main.ssl_keystore_init_failed", e);
//            } else{
//                // just log it as a warning.
//                _logger.log(Level.WARNING, "main.ssl_keystore_init_failed");
//            }
//        }
//
//	try {
//	    /* setup jsr 196 factory
//	     * define default factory if it is not already defined
//	     */
//	    String defaultFactory = java.security.Security.getProperty
//		(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY);
//	    if (defaultFactory == null) {
//		java.security.Security.setProperty
//		    (AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY,
//		     GFAuthConfigFactory.class.getName());
//	    }
//
//	} catch (Exception e) {
//	    //  XXX put string in catablog
//	    _logger.log(Level.WARNING, "main.jmac_default_factory");
//	}
//
//    }
//
//    /**
//     *Loads the app client's main class given its name and the loader to use.
//     *@param jcl the class loader to use for loading the client's main class
//     *@param clientMainClassName the name of the client's main class
//     *@return the Class object for the client's main class
//     *@throws RuntimeException (wraps the ClassNotFoundException) if the main class cannot be loaded
//     */
//    private Class loadMainClientClass(ClassLoader jcl, String clientMainClassName) throws UserError {
//        Class result = null;
//        try {
//            result = jcl.loadClass(clientMainClassName);
//        } catch (java.lang.ClassNotFoundException cnf) {
//            String errorMessage = localStrings.getString
//                ("appclient.mainclass.not.found", clientMainClassName);
//            throw new UserError(errorMessage, cnf);
//        }
//        _logger.log(Level.INFO, "acc.load_app_class", clientMainClassName);
//        return result;
//    }
//
//    /**
//     *Creates the appropriate logger, using any settings in the config file.
//     *<p>
//     *If any logging information has been buffered into the temporary memory-based
//     *logger, flush it to the newly-created log.
//     *@param xmlPathToUse config file path
//     *@param isJWS indicates if this is a Java Web Start launch
//     */
//    private Logger prepareLogging(BufferedLogger tempLogger, String xmlPathToUse, boolean isJWS) {
//        // make sure the default logger for ACCLogManager is set
//        Logger result = LogDomains.getLogger(LogDomains.ACC_LOGGER);
//
//        LogManager logMgr = LogManager.getLogManager();
//        if (logMgr instanceof ACCLogManager) {
//            ((ACCLogManager) logMgr).init(xmlPathToUse);
//        }
//
//        /*
//         *Transfer any records from the temporary logger to the new one.
//         */
//        tempLogger.pushTo(result);
//
//        return result;
//    }
//
//    /**
//     *Processes any command-line arguments, setting up static variables for use
//     *later in the processing.
//     *<p>
//     *As side-effects, these variables may be assigned values:
//     *<ul>
//     *<le>clientJar
//     *<le>displayName
//     *<le>mainClass
//     *<le>xmlPath
//     *<le>accConfigXml
//     *<le>guiAuth
//     *<le>runClient
//     *<le>classFileFromCommandLine
//     *<le>System property j2eelogin.name
//     *<le>System property j2eelogin.password
//     *</ul>
//     *@param args the command-line arguments passed to the ACC
//     *@return arguments to be passed to the actual client (with ACC arguments removed)
//     */
//    private Vector<String> processCommandLine(String[] args) throws UserError {
//        Vector<String> clientArgs = new Vector<String>();
//
//        AtomicInteger i = new AtomicInteger();
//        String arg = null;
//
//        // Parse command line arguments.
//        if(args.length < 1) {
//            usage();
//        } else {
//            while(i.get() < args.length) {
//                arg = args[i.getAndIncrement()];
//                if(arg.equals(CLIENT)) {
//                    clientJar = getRequiredCommandOptionValue(args, CLIENT, i, "appclient.clientWithoutValue");
//                } else if (arg.equals(NAME)) {
//                    displayName = getRequiredCommandOptionValue(args, NAME, i, "appclient.nameWithoutValue");
//                    ensureAtMostOneOfNameAndMainClass();
//                } else if(arg.equals(MAIN_CLASS)) {
//                    mainClass = getRequiredCommandOptionValue(args, MAIN_CLASS, i, "appclient.mainClassWithoutValue");
//                    ensureAtMostOneOfNameAndMainClass();
//                } else if(arg.equals(XML_PATH) ) {
//                    xmlPath = getRequiredUniqueCommandOptionValue(args, XML_PATH, xmlPath, i, "appclient.xmlWithoutValue");
//                } else if(arg.equals(ACC_CONFIG_XML) ) {
//                    accConfigXml = getRequiredUniqueCommandOptionValue(args, ACC_CONFIG_XML, accConfigXml, i, "appclient.accConfigXmlWithoutValue");
//                } else if(arg.equals(TEXT_AUTH)) {
//                    // Overrides legacy auth.gui setting.
//                    useTextAuth = true;
//                    logOption(TEXT_AUTH);
//                } else if(arg.equals(NO_APP_INVOKE)) {
//                    runClient = false;
//                    logOption(NO_APP_INVOKE);
//                } else if(arg.equals(USER)) {
//                    String userNameValue = getRequiredCommandOptionValue(args, USER, i, "appclient.userWithoutValue");
//                    System.setProperty(LOGIN_NAME, userNameValue);
//                } else if(arg.equals(PASSWORD)) {
//                    String passwordValue = getRequiredCommandOptionValue(args, PASSWORD, i, "appclient.passwordWithoutValue");
//                    System.setProperty(LOGIN_PASSWORD, passwordValue);
//                } else if (arg.equals(PASSWORD_FILE)) {
//                    String passwordFileValue = getRequiredCommandOptionValue(args, PASSWORD_FILE, i, "appclient.passwordFileWithoutValue");
//                    try {
//                        System.setProperty(LOGIN_PASSWORD,
//                            loadPasswordFromFile(passwordFileValue));
//                    } catch(IOException ex) {
//                        throw new UserError(localStrings.getString("appclient.errorReadingFromPasswordFile", passwordFileValue), ex);
//                    }
//                } else {
//                    clientArgs.add(arg);
//                    logArgument(arg);
//                }
//            }
//
//
//            String uname = System.getProperty(LOGIN_NAME);
//            String upass = System.getProperty(LOGIN_PASSWORD);
//            if( uname != null || upass != null ) {
//                UsernamePasswordStore.set(uname, upass);
//            }
//
//
//
//            /*If this is not a Java Web Start launch, the user may have asked
//             *to execute a .class file by omitting the -client argument.  In this
//             *case the user either specifies the name only of the class to run
//             *using -mainclass or omits -mainclass and specifies the path to
//             *the .class file as the first command-line argument that would
//             *otherwise be passed to the actual client.  In this second
//             *case, the first argument is removed from the list passed to the client.
//             */
//            if ((mainClass == null) && (clientJar == null) && ! isJWS) {
//                /*
//                 *Make sure there is at least one argument ready to be passed
//                 *to the client before trying
//                 *to use the first one as the class file spec.
//                 */
//                if (clientArgs.size() > 0) {
//                    classFileFromCommandLine = clientArgs.elementAt(0);
//                    clientArgs.removeElementAt(0);
//                    logClassFileArgument(classFileFromCommandLine);
//                } else {
//                    usage();
//                }
//            }
//        }
//        logClientArgs(clientArgs);
//        return clientArgs;
//    }
//
//    /**
//     *Returns the next unused argument as a String value, so long as there is
//     *a next argument and it does not begin with a dash which would indicate
//     *the next argument.
//     *@param position the mutable current position in the argument array
//     *@param errorKey the message key for looking up the correct error if the
//     *next argument cannot be used as a value
//     */
//    private String getRequiredCommandOptionValue(String [] args, String optionName, AtomicInteger position, String errorKey) throws UserError {
//        String result = null;
//        /*
//         *Make sure there is at least one more argument and that it does not
//         *start with a dash.  Either of those cases means the user omitted
//         *the required value.
//         */
//        if(position.get() < args.length && !args[position.get()].startsWith(DASH)) {
//            result = args[position.getAndIncrement()];
//        } else {
//            throw new UserError(localStrings.getString(errorKey));
//        }
//        if (_logger.isLoggable(Level.FINE)) {
//            _logger.fine(localStrings.getString("appclient.optionValueIs", optionName, result));
//        }
//        return result;
//    }
//
//    /**
//     *Returns the next unused argument (if present and not prefixed with a dash)
//     *as a string value as long as the current value of the argument expected
//     *is not already set.
//     *@param optionName the name of the option being processed
//     *@param currentValue the current value of the argument
//     *@param position the mutable current position in the argument array
//     *@param errorKey the message key for looking up the correct error if the
//     *next argument cannot be used as a value
//     *@throws IllegalArgumentException
//     */
//    private String getRequiredUniqueCommandOptionValue(String [] args, String optionName, String currentValue, AtomicInteger position, String errorKey) throws UserError {
//        if (currentValue != null) {
//            throw new UserError(localStrings.getString("appclient.duplicateValue", optionName, currentValue));
//        }
//        return getRequiredCommandOptionValue(args, optionName, position, errorKey);
//    }
//    /**
//     *Makes sure that at most one of the -name and -mainclass arguments
//     *appeared on the command line.
//     *@throws IllegalArgumentException if both appeared
//     */
//    private void ensureAtMostOneOfNameAndMainClass() throws UserError {
//        if (mainClass != null && displayName != null) {
//            throw new UserError(localStrings.getString("appclient.mainclassOrNameNotBoth"));
//        }
//    }
//
//    /**
//     *Reports that the specified option name has been processed from the command line.
//     *@param optionName the String name of the option
//     */
//    private void logOption(String optionName) {
//        if (_logger.isLoggable(Level.FINE)) {
//            _logger.fine(localStrings.getString("appclient.valuelessOptionFound", optionName));
//        }
//    }
//
//    private void logArgument(String arg) {
//        if (_logger.isLoggable(Level.FINE)) {
//            _logger.fine(localStrings.getString("appclient.argumentValueFound", arg));
//        }
//    }
//
//    private void logClassFileArgument(String classFile) {
//        if (_logger.isLoggable(Level.FINE)) {
//            _logger.fine(localStrings.getString("appclient.classFileUsed", classFile));
//        }
//    }
//
//    private void logClientArgs(Vector<String> clientArgs) {
//        if (_logger.isLoggable(Level.FINE)) {
//            _logger.fine(localStrings.getString("appclient.clientArgs", clientArgs.toString()));
//        }
//    }
//
//    /**
//     *Decides what class name the user specified IF the user has asked to run
//     *a .class file as the client.
//     *
//     *@param clientJar path to the client jar from the command line processing
//     *@param isJWS indicates if the current execution was launched via Java Web Start technology
//     *@param mainClass main class name as possibly specified on the command line
//     *@param classFileFromCommandLine class file spec from the command line (if present)
//     *@return class name to be run if the user has chosen a .class file; null otherwise
//     */
//    private String determineClassNameForClassFileLaunch(String clientJar, boolean isJWS, String mainClass, String classFileFromCommandLine) throws UserError {
//        String result = null;
//        boolean isFine = _logger.isLoggable(Level.FINE);
//        if(clientJar == null && ! isJWS) {
//            // ok, if the first parameter was the appclient class, let's check
//            // for its existence.
//            String value;
//
//            /*
//             *To run a .class file, the user omits the -client option and
//             *specifies either -mainclass <class-name> or provides
//             *the class file path as an argument.  In the second case the
//             *value will have been captured as classFileFromCommandLine during
//             *command line processing.
//             */
//            if (classFileFromCommandLine != null) {
//                value = classFileFromCommandLine;
//                if (isFine) {
//                    _logger.fine(localStrings.getString("appclient.classNameFromArg", classFileFromCommandLine));
//                }
//            } else {
//                value = mainClass;
//                if (isFine) {
//                    _logger.fine(localStrings.getString("appclient.classNameFromMainClass", mainClass));
//                }
//            }
//            if (value.endsWith(".class")) {
//                result = value.substring(0, value.length()-".class".length());
//            } else {
//                result = value;
//            }
//
//            String path = result.replace('.', File.separatorChar) + ".class";
//            File file = new File(path);
//            if (!file.isAbsolute()) {
//                file = new File(System.getProperty("user.dir"),  path);
//            }
//            /*
//             *We have tried to identify the class file to use based on either
//             *the -mainclass value (with -client omitted) or the value of the
//             *first command-line argument.  If we cannot find the class in the user's
//             *home directory (and we already know this is not a JWS launch because
//             *we are inside the "if") then the user has
//             *not entered a valid command or has specified a class we cannot find.
//             */
//            if (!file.exists()) {
//                throw new UserError(localStrings.getString("appclient.cannotFindClassFile", result, file.getAbsolutePath()));
//            } else {
//                if (isFine) {
//                    _logger.fine(localStrings.getString("appclient.usingClassFile", file.getAbsolutePath()));
//                }
//            }
//        }
//        return result;
//   }
//
//    /**
//     *Selects which config file to use, based on which are specified and whether
//     *this is a Java Web Start launch.
//     *@param xmlPath config file path as specified on the command line
//     *@param accConfigXml fall-back config file containing defaults
//     *@param isJWS indicates if this is a Java Web Start launch
//     *@return the appropriate config file path
//     */
//    private String chooseConfigFile(String xmlPath, String accConfigXml, boolean isJWS) {
//        String pathToUse = null;
//        boolean isFine = _logger.isLoggable(Level.FINE);
//
//        if (xmlPath != null) {
//            pathToUse = xmlPath;
//            if (isFine) {
//                _logger.fine(localStrings.getString("appclient.configFrom", XML_PATH, pathToUse));
//            }
//        } else if (accConfigXml != null ) {
//            pathToUse = accConfigXml; //use AS_ACC_CONFIG
//            if (isFine) {
//                _logger.fine(localStrings.getString("appclient.configFrom", ACC_CONFIG_XML, pathToUse));
//            }
//        } else if (isJWS) {
//            /*
//             *Neither -xml nor -configxml were present and this is a Java
//             *Web Start invocation.  Use
//             *the alternate mechanism to create the default config.
//             */
//            try {
//                String jwsACCConfigXml = prepareJWSConfig();
//                if (jwsACCConfigXml != null) {
//                    pathToUse = jwsACCConfigXml;
//                    if (isFine) {
//                        _logger.fine(localStrings.getString("appclient.configFromJWSTemplate"));
//                    }
//                }
//            } catch (Throwable thr) {
//                throw new RuntimeException(localStrings.getString("appclient.errorPrepConfig"), thr);
//            }
//        }
//        return pathToUse;
//    }
//
//    /**
//     *Locates the jar or directory that contains the app client main class.
//     *@param clientJar path to the client jar file (if specified)
//     *@param className the main class name for the app client
//     *@param isJWS indicates if this is a Java Web Start launch
//     *
//     *@return File object for the jar or directory containing the app client main class
//     */
//    private File locateAppclientJarOrDir(String clientJar, String className, boolean isJWS) throws ClassNotFoundException, URISyntaxException, UserError {
//        File result = null;
//        boolean isFine = _logger.isLoggable(Level.FINE);
//        /*
//         *For Java Web Start launches, locate the jar file implicitly.
//         *Otherwise, if the user omitted the clientjar argument (and the
//         *code has gotten this far) then the user must have used the
//         *first argument as the name of the class in ${user.dir} to run.  If
//         *the user actually specified the clientjar argument, then use that
//         *value as the file spec for the client jar.
//         */
//        if (isJWS) {
//            /*
//             *Java Web Start case.
//             */
//            result = findAppClientFileForJWSLaunch();
//            if (isFine) {
//                _logger.fine(localStrings.getString("appclient.JWSAppClientFile", result.getAbsolutePath()));
//            }
//        } else if (clientJar==null) {
//            /*
//             *First-argument-as-class-name case
//             */
//            File userDir = new File(System.getProperty("user.dir"));
//            File appClientClass = new File(userDir, className);
//            result = appClientClass.getParentFile();
//            if (isFine) {
//                _logger.fine(localStrings.getString("appclient.classFileAppClientFile", result.getAbsolutePath()));
//            }
//        } else {
//            /*
//             *Normal case - clientjar argument specified.
//             */
//            result = new File(clientJar);
//            if (isFine) {
//                _logger.fine(localStrings.getString("appclient.clientJarAppClientFile", result.getAbsolutePath()));
//            }
//            /*
//             *Make sure the user-specified file - client JAR or .class - exists.
//             */
//            if ( ! result.exists() ) {
//                UserError ue = UserError.formatUserError(
//                        localStrings.getString("appclient.cannotFindJarFile"),
//                        result.getAbsolutePath());
//                throw ue;
//            }
//
//        }
//        return result;
//    }
//
//    /**
//     *Creates a class loader for the app client, using the jar or directory
//     *which contains the main class.
//     *@param jarOrDir the File object for the directory or jar file containing the main class
//     *@return ClassLoader for use by the app client
//     */
//    private ClassLoader preparePreliminaryClassLoader(File jarOrDir) throws MalformedURLException {
//        ClassLoader result = null;
//        URL[] urls = new URL[1];
//        urls[0] = jarOrDir.toURI().toURL();
//        /*
//         *Set the parent of the new class loader to the current loader.
//         *The Java Web Start-managed class path is implemented in the
//         *current loader, so it must remain on the loader stack.
//         */
//        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
//        result = new URLClassLoader(urls, currentCL);
////        Thread.currentThread().setContextClassLoader(jcl);
//        return result;
//    }
//
//    private AppContainer createAppContainer(ApplicationClientDescriptor appDesc, boolean guiAuth) {
//        AppContainer result = new AppContainer(appDesc, guiAuth);
//        if(result == null) {
//            _logger.log(Level.WARNING, "acc.no_client_desc",
//                        (displayName == null) ? mainClass : displayName);
//
//            System.exit(1);
//        }
//        return result;
//    }
//
//
//    /**
//     *Adds a shutdown hook to make sure clean-up work runs at JVM exit.
//     */
//    private static Cleanup prepareShutdownCleanup(
//        AppContainer container, AppClientInfo appClientInfo) {
//        // Ensure cleanup is performed, even if
//        // application client calls System.exit().
//        Cleanup cleanup = new Cleanup();
//        Runtime runtime = Runtime.getRuntime();
//        runtime.addShutdownHook(cleanup);
//        cleanup.setAppContainer(container);
//        cleanup.setAppClientInfo(appClientInfo);
//        return cleanup;
//    }
//
//    /**
//     *Assigns the URL stream handler factory.
//     */
//    private static void prepareURLStreamHandling() {
//        // Set the HTTPS URL stream handler.
//        java.security.AccessController.doPrivileged(new
//                                       java.security.PrivilegedAction() {
//                public Object run() {
//                    URL.setURLStreamHandlerFactory(new
//                                       HttpsURLStreamHandlerFactory());
//                    return null;
//                }
//            });
//    }
//
//    private Properties setTargetServerProperties(String clientXmlLocation)
//	throws ConfigException {
//
//        boolean isEndpointPropertySpecifiedByUser = false;
//        String loadBalancingPolicy = null;
//
//        Properties result = new Properties();
//
//        StringBuilder completeEndpointList = new StringBuilder();
//
//        //FIXME: may need to set the context in switch or generic context. but later
//        try {
//            if(clientXmlLocation == null || clientXmlLocation.equals("")) {
//                clientXmlLocation = DEFAULT_CLIENT_CONTAINER_XML;
//            }
//
//	    // set for com.sun.enterprise.security.jauth.ConfigXMLParser
//	    System.setProperty(SUNACC_XML_URL, clientXmlLocation);
//             _logger.log(Level.INFO, "acc.using_xml_location", clientXmlLocation);
//
//            ConfigContext ctx = ConfigFactory.createConfigContext(
//		clientXmlLocation, true,
//		false, false,
//		ClientContainer.class,
//		new ACCEntityResolver());
//
//            ClientContainer cc = ClientBeansFactory.getClientBean(ctx);
//
//	    host = cc.getTargetServer(0).getAddress();
//	    port = cc.getTargetServer(0).getPort();
//
//	    //check for targetServerEndpoints
//	    TargetServer[] tServer = cc.getTargetServer();
//	    String targetServerEndpoints = null;
//	    for (int i = 0; i < tServer.length; i++) {
//	        if (targetServerEndpoints == null) {
//		    targetServerEndpoints = tServer[i].getAddress() +
//		    ":" + tServer[i].getPort();
//		} else {
//		    targetServerEndpoints = targetServerEndpoints + "," +
//		      tServer[i].getAddress() +
//		      ":" + tServer[i].getPort();
//                    lb_enabled = true;
//		}
//	    }
//
//            setSSLData(cc);
//
//            //FIXME: what do we do about realm
//            ClientCredential cCrd = cc.getClientCredential();
//            if(cCrd != null) {
//                String uname = null;
//                String upass = null;
//
//                // if user entered user/password from command line,
//                // it take percedence over the xml file. - y.l. 05/15/02
//                if (System.getProperty(LOGIN_NAME) == null) {
//                    _logger.config("using login name from client container xml...");
//                    //System.setProperty(LOGIN_NAME, cCrd.getUserName());
//                    uname = cCrd.getUserName();
//                }
//                if (System.getProperty(LOGIN_PASSWORD) == null) {
//                    _logger.config("using password from client container xml...");
//                    //System.setProperty(LOGIN_PASSWORD, cCrd.getPassword());
//                    upass = cCrd.getPassword();
//                }
//                if( uname != null || upass != null ) {
//                    UsernamePasswordStore.set(uname, upass);
//                }
//            }
//		String endpoints_property = null;
//	    // Check if client requires SSL to be used
//	    ElementProperty[] props = cc.getElementProperty();
//	    for ( int i=0; i<props.length; i++ ) {
//		if ( props[i].getName().equals("ssl") ) {
//		    if ( props[i].getValue().equals("required") ) {
//			(ORBManager.getCSIv2Props()).put(ORBManager.ORB_SSL_CLIENT_REQUIRED,
//				       "true");
//		    }
//		}
//		if ( props[i].getName().equals(S1ASCtxFactory.LOAD_BALANCING_PROPERTY) ) {
//                    loadBalancingPolicy = props[i].getValue();
//		    lb_enabled = true;
//		}
//		if ( props[i].getName().equals(S1ASCtxFactory.IIOP_ENDPOINTS_PROPERTY) ) {
//                    isEndpointPropertySpecifiedByUser = true;
//		    endpoints_property = props[i].getValue().trim();
//		    lb_enabled = true;
//		}
//	    }
//
//            /*
//             *If the endpoints property was not set in the XML file's property
//             *settings, try to set it from the server's assignment in the JNLP document.
//             */
//	    String jwsEndpointsProperty = System.getProperty(Main.APPCLIENT_IIOP_FAILOVER_ENDPOINTS_PROPERTYNAME);
//	    if (jwsEndpointsProperty != null) {
//	        targetServerEndpoints = jwsEndpointsProperty;
//                completeEndpointList.append(jwsEndpointsProperty);
//		lb_enabled = true;
//		_logger.fine("jwsEndpointsProperty = " + jwsEndpointsProperty);
//	    } else {
//	        /*
//		 *Suppress the warning if the endpoints_property was set
//		 *from the JNLP document, since that is in fact the preferred
//		 *way to set the endpoints.
//		 */
//	        if (isEndpointPropertySpecifiedByUser){
//                    _logger.warning("acc.targetserver.endpoints.warning");
//                }
//
//		_logger.fine("targetServerEndpoints = " + targetServerEndpoints +
//			     "endpoints_property = " +
//			     endpoints_property);
//
//                if (lb_enabled) {
//                    completeEndpointList.append(targetServerEndpoints.trim());
//                    if (endpoints_property != null) {
//                        completeEndpointList.append(",").append(endpoints_property);
//                    }
//		}
//	    }
//            if (lb_enabled) {
//                System.setProperty(S1ASCtxFactory.IIOP_ENDPOINTS_PROPERTY, completeEndpointList.toString());
//                /*
//                 * Honor any explicit setting of the load-balancing policy.
//                 * Otherwise just defer to whatever default the ORB uses.
//                 */
//                if (loadBalancingPolicy != null) {
//                    System.setProperty(S1ASCtxFactory.LOAD_BALANCING_PROPERTY, loadBalancingPolicy);
//                }
//                /*
//                 * For load-balancing the Properties object is not used.  Rather,
//                 * the ORB detects the system property settings.  So return a
//                 * null for the LB case.
//                 */
//                result = null;
//            } else {
//                /*
//                 * For the non-load-balancing case, the Properties object must
//                 * contain the initial host and port settings for the ORB.
//                 */
//                result.setProperty(ORB_INITIAL_HOST_PROPERTYNAME, host);
//                result.setProperty(ORB_INITIAL_PORT_PROPERTYNAME, port);
//            }
//            return result;
//	} catch (ConfigException t) {
//	    _logger.log(Level.WARNING,"acc.acc_xml_file_error" ,
//			new Object[] {clientXmlLocation, t.getMessage()});
//	    _logger.log(Level.FINE, "exception : " + t.toString(), t);
//	    throw t;
//	}
//    }
//
//    private static void setSSLData(ClientContainer cc) {
//        try {
//            // Set the SSL related properties for ORB
//            TargetServer tServer = cc.getTargetServer(0);
//            // TargetServer is required.
//	    //temp solution to target-server+ change in DTD
//            // assuming that multiple servers can be specified but only 1st
//	    // first one will be used.
//	    Security security = tServer.getSecurity();
//	    if (security == null) {
//		_logger.fine("No Security input set in ClientContainer.xml");
//		// do nothing
//		return;
//	    }
//	    Ssl ssl = security.getSsl();
//	    if (ssl == null) {
//		_logger.fine("No SSL input set in ClientContainer.xml");
//		// do nothing
//		return;
//
//	    }
//	    //XXX do not use NSS in this release
//	    //CertDb   certDB  = security.getCertDb();
//	    SSLUtils.setAppclientSsl(ssl);
//	} catch (Exception ex) {
//
//        }
//    }
//
//
//    private void validateXMLFile(String xmlFullName) throws UserError
//    {
//        if(xmlFullName == null ||
//           xmlFullName.startsWith("-")){ // If no file name is given after -xml argument
//            usage();
//        }
//        try {
//            File f = new File(xmlFullName);
//            if((f != null) && f.exists() && f.isFile() && f.canRead()){
//                return;
//            }else{// If given file does not exists
//                xmlMessage(xmlFullName);
//            }
//        } catch (Exception ex) {
//            xmlMessage(xmlFullName);
//        }
//    }
//
//    // Shut down system resource adapters. Currently it is
//    // only JMS.
//    private void shutDownSystemAdapters() {
//       try {
//	    com.sun.enterprise.PoolManager poolmgr =
//	        Switch.getSwitch().getPoolManager();
//	    if ( poolmgr != null ) {
//	        Switch.getSwitch().getPoolManager().killFreeConnectionsInPools();
//	    }
//	} catch( Exception e ) {
//	    //ignore
//	}
//
//	try {
//            ConnectorRegistry registry = ConnectorRegistry.getInstance();
//            ActiveResourceAdapter activeRar = registry.getActiveResourceAdapter
//                                         (ConnectorRuntime.DEFAULT_JMS_ADAPTER);
//            if (activeRar != null) {
//                activeRar.destroy();
//            }
//        } catch (Exception e) {
//            // Some thing has gone wrong. No problem
//            _logger.fine("Exception caught while shutting down system adapter:"+e.getMessage());
//        }
//    }
//
//    private String getUsage() {
//        return localStrings.getString(
//            "main.usage",
//            "appclient [ -client <appjar> | <classfile> ] [-mainclass <appClass-name>|-name <display-name>] [-xml <xml>] [-textauth] [-user <username>] [-password <password>|-passwordfile <password-file>] [app-args]");
//    }
//
//    private void usage() {
//        System.out.println(getUsage());
//	System.exit(1);
//    }
//
//    private void xmlMessage(String xmlFullName) throws UserError
//    {
//        UserError ue = new UserError(localStrings.getString("main.cannot_read_clientContainer_xml", xmlFullName,
//             "Client Container xml: " + xmlFullName + " not found or unable to read.\nYou may want to use the -xml option to locate your configuration xml."));
//        ue.setUsage(getUsage());
//        throw ue;
//
//    }
//
//    private String loadPasswordFromFile(String fileName)
//            throws IOException {
//        InputStream inputStream = null;
//        try {
//            inputStream = new BufferedInputStream(new FileInputStream(fileName));
//            Properties props = new Properties();
//            props.load(inputStream);
//            return props.getProperty("PASSWORD");
//        } finally {
//            if (inputStream != null) {
//                inputStream.close();
//            }
//        }
//    }
//
//    private static class Cleanup extends Thread {
//        private AppContainer appContainer = null;
//        private AppClientInfo appClientInfo = null;
//        private boolean cleanedUp = false;
//        private InjectionManager injectionMgr = null;
//        private ApplicationClientDescriptor appClient = null;
//        private Class cls = null;
//        private ScriptingSupport scriptingSupport = null;
//
//        public Cleanup() {
//        }
//
//        public void setAppContainer(AppContainer container) {
//            appContainer = container;
//        }
//
//        public void setAppClientInfo(AppClientInfo info) {
//            appClientInfo = info;
//        }
//
//        public void setInjectionManager(InjectionManager injMgr, Class cls, ApplicationClientDescriptor appDesc) {
//            injectionMgr = injMgr;
//            this.cls = cls;
//            appClient = appDesc;
//        }
//
//        public void setScriptingSupport(ScriptingSupport scriptingSupport) {
//            this.scriptingSupport = scriptingSupport;
//        }
//
//        public void run() {
//            _logger.info("Clean-up starting");
//            cleanUp();
//        }
//
//        public void cleanUp() {
//            if( !cleanedUp ) {
//                try {
//                    if (scriptingSupport != null) {
//                        scriptingSupport.close();
//                        scriptingSupport = null;
//                    }
//                    if( appContainer != null ) {
//                        appContainer.postInvoke();
//                    }
//                    if ( appClientInfo != null ) {
//                        appClientInfo.close();
//                    }
//                    if ( injectionMgr != null) {
//                        // inject the pre-destroy methods before shutting down
//                        injectionMgr.invokeClassPreDestroy(cls, appClient);
//                        injectionMgr = null;
//                    }
//                    if(appClient != null && appClient.getServiceReferenceDescriptors() != null) {
//                        // Cleanup client pipe line, if there were service references
//                        for (Object desc: appClient.getServiceReferenceDescriptors()) {
//                             ClientPipeCloser.getInstance()
//                                .cleanupClientPipe((ServiceReferenceDescriptor)desc);
//                        }
//                    }
//                }
//                catch(Throwable t) {
//                }
//                finally {
//                    cleanedUp = true;
//                }
//            } // End if -- cleanup required
//        }
//    }
//    /**
//     *Sets up the user-provided or default sun-acc.xml and
//     *wss-client-config.xml configurations.
//     *@return the file name of the sun-acc.xml file
//     */
//    private String prepareJWSConfig() throws IOException, FileNotFoundException {
//        return prepareJWSDefaultConfig();
//    }
//
//    /**
//     *Creates temporary files for use as default sun-acc.xml and
//     *wss-client-config.xml configurations.
//     *@return the file name of the temporary sun-acc.xml file
//     */
//    private String prepareJWSDefaultConfig() throws IOException, FileNotFoundException {
//        String result = null;
//
//        /*
//         *Retrieve the sun-acc and wss-client-config templates.
//         */
//        String sunACCTemplate = Util.loadResource(this.getClass(), SUN_ACC_DEFAULT_TEMPLATE);
//        String wssClientConfigTemplate = Util.loadResource(this.getClass(), WSS_CLIENT_CONFIG_TEMPLATE);
//
//        /*
//         *Prepare the property names and values for substitution in the templates.  Some
//         *of the properties are specified in the environment already, so use those
//         *as defaults and just add the extra ones.
//         */
//        Properties tokenValues = new Properties(System.getProperties());
//
//        /**
//         *Create the wss client config defaults, then write them to a temporary file.
//         */
//        String wssClientConfig = Util.replaceTokens(wssClientConfigTemplate, tokenValues);
//        File wssClientConfigFile = Util.writeTextToTempFile(wssClientConfig, WSS_CLIENT_CONFIG_PREFIX, WSS_CLIENT_CONFIG_SUFFIX, retainTempFiles);
//        _logger.fine("Temporary wss-client-config.xml file: " + wssClientConfigFile.getAbsolutePath() + lineSep);
////        pendingLogFine.append("Temporary wss-client-config.xml file: " + wssClientConfigFile.getAbsolutePath() + lineSep);
//
//        /*
//         *Now that the wss temp file is created, insert its name into the default
//         *sun-acc text and write that to another temp file.
//         *
//         *On Windows, the backslashes in the path will be consumed by the replaceTokens method which will
//         *interpret them as quoting the following character.  So replace each \ with \\ first.  All the slashes
//         *have to do with quoting a slash to the Java compiler, then quoting it again to the regex
//         *processor.
//         */
//        String quotedConfigFileSpec = wssClientConfigFile.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\");
//        tokenValues.setProperty(SUN_ACC_SECURITY_CONFIG_PROPERTY, quotedConfigFileSpec);
//
//        String sunaccContent = Util.replaceTokens(sunACCTemplate, tokenValues);
//        File sunaccFile = Util.writeTextToTempFile(sunaccContent, SUN_ACC_PREFIX, SUN_ACC_SUFFIX, retainTempFiles);
//        _logger.fine("Temporary sun-acc.xml file: " + sunaccFile.getAbsolutePath());
////        pendingLogFine.append("Temporary sun-acc.xml file: " + sunaccFile.getAbsolutePath());
//
//        return sunaccFile.getAbsolutePath();
//    }
//
//    /**
//     *Prepares the JAAS login configuration for a Java Web Start invocation.
//     *
//     */
//    private void prepareJWSLoginConfig() {
//        /*
//         *If this is a Java Web Start invocation, prepare the user-specified
//         *or default login configuration.
//         */
//        if (isJWS) {
//            try {
//                prepareJWSDefaultLoginConfig();
//            } catch (Throwable thr) {
//                throw new RuntimeException(localStrings.getString("appclient.errorPrepJWSLogginConfig"), thr);
//            }
//        }
//    }
//
//    /**
//     *Extracts the default login.conf file into a temporary file and assigns the
//     *java.security.auth.login.config property accordingly.
//     */
//    private void prepareJWSDefaultLoginConfig() throws IOException, FileNotFoundException {
//        /*
//         *For a Java Web Start launch, the default login configuration is in a
//         *template bundled in the app server's jws jar file.  Putting it there
//         *allows this method to locate it simply by loading it as a resource, whereas
//         *the command-line appclient invocation needs to be able to find the
//         *file on-disk somewhere.
//         *
//         *The contents of the template are loaded and then written to a temp file and
//         *that temp file location is assigned to the system property that directs
//         *JAAS to the login configuration.
//         */
//        String configContent = Util.loadResource(this.getClass(), LOGIN_CONF_TEMPLATE);
//        File configFile = Util.writeTextToTempFile(configContent, LOGIN_CONF_FILE_PREFIX, LOGIN_CONF_FILE_SUFFIX, retainTempFiles);
//        String configFilePath = configFile.getAbsolutePath();
//        _logger.fine("Temporary appclientlogin.conf file: " + configFilePath);
//        System.setProperty(LOGIN_CONF_PROPERTY_NAME, configFilePath);
//    }
//
//    /*
//     *Returns the jar or directory that contains the specified resource.
//     *@param target entry name to look for
//     *@return URI object for the jar or directory containing the entry
//     */
//    private File findContainingJar(String target) throws IllegalArgumentException, URISyntaxException {
//        File result = null;
//
//        /*
//         *Use the current class loader to find the resource.
//         */
//        URL resourceURL = getClass().getResource(target);
//        if (resourceURL != null) {
//            URI uri = resourceURL.toURI();
//            String scheme = uri.getScheme();
//            String ssp = uri.getSchemeSpecificPart();
//            if (scheme.equals("jar")) {
//                /*
//                 *The scheme-specific part will look like "file:<file-spec>!/<path-to-class>.class"
//                 *so we need to isolate the scheme and the <file-spec> part.
//                 *The subscheme (the scheme within the jar) precedes the colon
//                 *and the file spec appears after it and before the exclamation point.
//                 */
//                int colon = ssp.indexOf(':');
//                String subscheme = ssp.substring(0, colon);
//                int excl = ssp.indexOf('!');
//                String containingJarPath = ssp.substring(colon + 1, excl);
//                result = new File(containingJarPath);
//            } else if (scheme.equals("file")) {
//                /*
//                 *The URI is already a file, so the part we want is the part
//                 *up to but not including the resource name we were looking for
//                 in the first place..
//                 */
//                int resourceNamePosition = ssp.indexOf(target);
//                String containingFilePath = ssp.substring(0, resourceNamePosition);
//                result = new File(containingFilePath);
//            } else {
//                throw new IllegalArgumentException(resourceURL.toExternalForm());
//            }
//        }
//        return result;
//    }
//
//    /**
//     *Locate the app client jar file during a Java Web Start launch.
//     *@return File object for the client jar file
//     */
//    private File findAppClientFileForJWSLaunch() throws URISyntaxException {
//        /*
//         *The JWSACCMain class has already located the downloaded app client
//         *jar file and set a property pointing to it.
//         */
//       File containingJar = new File(System.getProperty("com.sun.aas.downloaded.appclient.jar"));
//        _logger.fine("Location of appclient jar file: " + containingJar.getAbsolutePath());
//        return containingJar;
//    }
//
//    /**
//     *Creates a memory-based logger for holding log messages that may occur before
//     *the user's logging set-up is read.
//     *@return a Logger that buffers its log records in memory.
//     */
//    private BufferedLogger prepareBufferedLogging() {
//        /*
//         *The buffered logger adds a handler automatically during instantiation.
//         */
//        BufferedLogger logger = new BufferedLogger();
//        return logger;
//    }
//
//    /**
//     *Logger implementation that records its log records in memory.  Normally
//     *the logger will be flushed to another logger once the second logger has
//     *been configured appropriately.
//     */
//    private class BufferedLogger extends Logger {
//
//        /**
//         *Creates a new instance of the buffered logger.
//         */
//        public BufferedLogger() {
//            super(null, null);
//            addHandler(new BufferedHandler());
//        }
//
//        /**
//         *Flushes any accumulated log messages to the specified target logger.
//         *@param target the Logger to receive any buffered messages
//         */
//        public void pushTo(Logger target) {
//            for (Handler handler : getHandlers()) {
//                if (handler instanceof BufferedHandler) {
//                    ((BufferedHandler) handler).pushTo(target);
//                }
//            }
//        }
//    }
//
//    /**
//     *Log handler that accumulates each log record sent to it into memory and
//     *flushes those records to another logger when asked.  Once this handler is
//     *flushed it can no longer be used.
//     */
//    private class BufferedHandler extends Handler {
//
//        /** holds log records until flushed to another logger */
//        private Vector<LogRecord> buffer = new Vector<LogRecord>();
//
//        /**
//         *Creates a new instance of the buffered log handler.
//         */
//        public BufferedHandler() {
//            setLevel(Level.ALL);
//            setFilter(null);
//            setFormatter(new SimpleFormatter());
//        }
//
//        /**
//         *Publishes a log record to the handler.
//         *<p>
//         *In this handler, the log record is saved in the memory buffer.
//         *@paran record the LogRecord to be written
//         */
//        public synchronized void publish(LogRecord record) {
//            if (buffer == null) {
//                throw new IllegalStateException("Handler asked to publish log record after pushTo invoked");
//            }
//            buffer.add(record);
//        }
//
//        /**
//         *Flushes any accumulated log information to the specified target Logger.
//         *@param target the Logger to receive buffered messages
//         */
//        public synchronized void pushTo(Logger target) {
//            if (buffer == null) {
//                throw new IllegalStateException("Handler asked to push to target more than once");
//            }
//            for (LogRecord record : buffer) {
//                target.log(record);
//            }
//            close();
//        }
//
//        /**
//         *Closes the handler to further use.
//         */
//        public void close() throws SecurityException {
//            buffer.clear();
//            buffer = null;
//        }
//
//        /**
//         *Does nothing because there is no intermediate buffer between the
//         *handler and its storage - the storage is itself a memory buffer.
//         */
//        public void flush() {
//        }
//    }

}
