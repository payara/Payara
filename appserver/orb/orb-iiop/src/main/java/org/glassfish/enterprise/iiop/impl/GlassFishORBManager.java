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

package org.glassfish.enterprise.iiop.impl;

import com.sun.corba.ee.impl.javax.rmi.CORBA.StubDelegateImpl;
import com.sun.corba.ee.impl.javax.rmi.CORBA.Util;
import com.sun.corba.ee.impl.javax.rmi.PortableRemoteObject;
import com.sun.corba.ee.impl.orb.ORBImpl;
import com.sun.corba.ee.impl.orb.ORBSingleton;
import com.sun.corba.ee.spi.oa.rfm.ReferenceFactoryManager;
import com.sun.corba.ee.spi.osgi.ORBFactory;
import com.sun.corba.ee.spi.misc.ORBConstants;
import com.sun.corba.ee.spi.orb.ORB ;
import com.sun.corba.ee.impl.folb.InitialGroupInfoService ;

import com.sun.logging.LogDomains;

import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.Orb;
import org.glassfish.orb.admin.config.IiopService;
import com.sun.enterprise.config.serverbeans.SslClientConfig;

import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.Arrays;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.enterprise.iiop.api.GlassFishORBLifeCycleListener;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.enterprise.iiop.util.IIOPUtils;

import com.sun.enterprise.util.Utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModulesRegistry;

import org.jvnet.hk2.config.types.Property;

/**
 * This class initializes the ORB with a list of (standard) properties
 * and provides a few convenience methods to get the ORB etc.
 */

public final class GlassFishORBManager {
    static final Logger logger = LogDomains.getLogger(
        GlassFishORBManager.class, LogDomains.CORBA_LOGGER);

    private static void fineLog( String fmt, Object... args ) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, fmt, args ) ;
        }
    }

    private static void finestLog( String fmt, Object... args ) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, fmt, args ) ;
        }
    }

    private static final Properties EMPTY_PROPERTIES = new Properties();

    // Various pluggable classes defined in the app server that are used
    // by the ORB.
    private static final String ORB_CLASS =
            ORBImpl.class.getName() ;
    private static final String ORB_SINGLETON_CLASS =
            ORBSingleton.class.getName() ;

    private static final String ORB_SE_CLASS =
            "com.sun.corba.se.impl.orb.ORBImpl";
    private static final String ORB_SE_SINGLETON_CLASS =
            "com.sun.corba.se.impl.orb.ORBSingleton";

    private static final String PEORB_CONFIG_CLASS =
            PEORBConfigurator.class.getName() ;
    private static final String IIOP_SSL_SOCKET_FACTORY_CLASS =
            IIOPSSLSocketFactory.class.getName() ;
    private static final String RMI_UTIL_CLASS =
            Util.class.getName() ;
    private static final String RMI_STUB_CLASS =
            StubDelegateImpl.class.getName() ;
    private static final String RMI_PRO_CLASS =
            PortableRemoteObject.class.getName() ;

    // JNDI constants
    public static final String JNDI_PROVIDER_URL_PROPERTY =
            "java.naming.provider.url";
    public static final String JNDI_CORBA_ORB_PROPERTY =
            "java.naming.corba.orb";

    // RMI-IIOP delegate constants
    public static final String ORB_UTIL_CLASS_PROPERTY =
            "javax.rmi.CORBA.UtilClass";
    public static final String RMIIIOP_STUB_DELEGATE_CLASS_PROPERTY =
            "javax.rmi.CORBA.StubClass";
    public static final String RMIIIOP_PRO_DELEGATE_CLASS_PROPERTY =
            "javax.rmi.CORBA.PortableRemoteObjectClass";

    // ORB constants: OMG standard
    public static final String OMG_ORB_CLASS_PROPERTY =
            "org.omg.CORBA.ORBClass";
    public static final String OMG_ORB_SINGLETON_CLASS_PROPERTY =
            "org.omg.CORBA.ORBSingletonClass";

    // ORB constants: Sun specific
    public static final String SUN_ORB_SOCKET_FACTORY_CLASS_PROPERTY =
            ORBConstants.SOCKET_FACTORY_CLASS_PROPERTY;

    // ORB configuration constants
    private static final String DEFAULT_SERVER_ID = "100";
    private static final String ACC_DEFAULT_SERVER_ID = "101";
    private static final String USER_DEFINED_ORB_SERVER_ID_PROPERTY =
        "org.glassfish.orb.iiop.orbserverid";

    private static final String DEFAULT_MAX_CONNECTIONS = "1024";
    private static final String GLASSFISH_INITIALIZER =
            GlassFishORBInitializer.class.getName() ;

    private static final String SUN_GIOP_DEFAULT_FRAGMENT_SIZE = "1024";
    private static final String SUN_GIOP_DEFAULT_BUFFER_SIZE = "1024";

    public static final String DEFAULT_ORB_INIT_HOST = "localhost";

    // This will only apply for stand-alone java clients, since
    // in the server the orb port comes from domain.xml, and in an appclient
    // the port is set from the sun-acc.xml.  It's set to the same 
    // value as the default orb port in domain.xml as a convenience.
    // That way the code only needs to do a "new InitialContext()"
    // without setting any jvm properties and the naming service will be
    // found.  Of course, if the port was changed in domain.xml for some
    // reason the code will still have to set org.omg.CORBA.ORBInitialPort.
    public static final String DEFAULT_ORB_INIT_PORT = "3700";

    private static final String ORB_SSL_STANDALONE_CLIENT_REQUIRED =
            "com.sun.CSIV2.ssl.standalone.client.required";

     // We need this to get the ORB monitoring set up correctly
    public static final String S1AS_ORB_ID = "S1AS-ORB";

    // Set in constructor
    private ServiceLocator services;
    private IIOPUtils iiopUtils;

    // the ORB instance
    private ORB orb = null;

    // The ReferenceFactoryManager from the orb.
    private ReferenceFactoryManager rfm = null;

    private int orbInitialPort = -1;

    private List<IiopListener> iiopListeners = null;
    private Orb orbBean = null;
    private IiopService iiopService = null;

    private Properties csiv2Props = new Properties();

    private ProcessType processType;

    private IiopFolbGmsClient gmsClient ;

    /**
     * Keep this class private to the package.  Eventually we need to
     * move all public statics or change them to package private.
     * All external orb/iiop access should go through orb-connector module
     */
    GlassFishORBManager(ServiceLocator h ) {
        fineLog( "GlassFishORBManager: Constructing GlassFishORBManager: h {0}",
            h ) ;
        services = h;

        iiopUtils = services.getService(IIOPUtils.class);

        ProcessEnvironment processEnv = services.getService(
            ProcessEnvironment.class);

        processType = processEnv.getProcessType();

        initProperties();
    }

    /**
     * Returns whether an adapterName (from ServerRequestInfo.adapter_name)
     * represents an EJB or not.
     * @param adapterName The adapter name
     * @return whether this adapter is an EJB or not
     */
    public boolean isEjbAdapterName(String[] adapterName) {
        boolean result = false;
        if (rfm != null) {
            result = rfm.isRfmName(adapterName);
        }

        return result;
    }

    /**
     * Returns whether the operationName corresponds to an "is_a" call
     * or not (used to implement PortableRemoteObject.narrow.
     */
    boolean isIsACall(String operationName) {
        return operationName.equals("_is_a");
    }

    /**
     * Return the shared ORB instance for the app server.
     * If the ORB is not already initialized, it is created
     * with the standard server properties, which can be
     * overridden by Properties passed in the props argument.
     */
    synchronized ORB getORB(Properties props) {

        try {
            finestLog( "GlassFishORBManager.getORB->: {0}", orb);

            if (orb == null) {
                initORB(props);
            }

            return orb;
        } finally {
            finestLog( "GlassFishORBManager.getORB<-: {0}", orb);
        }
    }

    Properties getCSIv2Props() {
        // Return a copy of the CSIv2Props
        return new Properties(csiv2Props);
    }

    void setCSIv2Prop(String name, String value) {
        csiv2Props.setProperty(name, value);
    }

    int getORBInitialPort() {
        return orbInitialPort;
    }

    private void initProperties() {
        fineLog( "GlassFishORBManager: initProperties: processType {0}",
            processType ) ;

        if (processType != ProcessType.ACC) {
            String sslClientRequired = System.getProperty(
                    ORB_SSL_STANDALONE_CLIENT_REQUIRED);
            if (sslClientRequired != null
                    && sslClientRequired.equals("true")) {
                csiv2Props.put(
                        GlassFishORBHelper.ORB_SSL_CLIENT_REQUIRED, "true");
            }
        }

        if(!processType.isServer()) {
            // No access to domain.xml.  Just init properties.
            // In this case iiopListener beans will be null.
            checkORBInitialPort(EMPTY_PROPERTIES);


        } else {
            iiopService = iiopUtils.getIiopService();
            iiopListeners = iiopService.getIiopListener() ;
            assert iiopListeners != null ;

            // checkORBInitialPort looks at iiopListenerBeans, if present
            checkORBInitialPort(EMPTY_PROPERTIES);

            orbBean = iiopService.getOrb();
            assert (orbBean != null);

            // Initialize IOR security config for non-EJB CORBA objects
            //iiopServiceBean.isClientAuthenticationRequired()));
            csiv2Props.put(GlassFishORBHelper.ORB_CLIENT_AUTH_REQUIRED,
                String.valueOf(
                    iiopService.getClientAuthenticationRequired()));

            // If there is at least one non-SSL listener, then it means
            // SSL is not required for CORBA objects.
            boolean corbaSSLRequired = true;
            for (IiopListener bean : iiopListeners) {
                if (bean.getSsl() == null) {
                    corbaSSLRequired = false ;
                    break ;
                }
            }

            csiv2Props.put(GlassFishORBHelper.ORB_SSL_SERVER_REQUIRED, 
                String.valueOf( corbaSSLRequired));
        }

    }

    /**
     * Set ORB-related system properties that are required in case
     * user code in the app server or app client container creates a
     * new ORB instance.  The default result of calling
     * ORB.init( String[], Properties ) must be a fully usuable, consistent
     * ORB.  This avoids difficulties with having the ORB class set
     * to a different ORB than the RMI-IIOP delegates.
     */
    private void setORBSystemProperties() {
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Object>() {
                @Override
                public java.lang.Object run() {
                    if (System.getProperty(OMG_ORB_CLASS_PROPERTY) == null) {
                        // set ORB based on JVM vendor
                        if (System.getProperty("java.vendor").equals(
                            "Sun Microsystems Inc.")) {
                            System.setProperty(OMG_ORB_CLASS_PROPERTY,
                                ORB_SE_CLASS);
                        } else {
                            // if not Sun, then set to EE class
                            System.setProperty(OMG_ORB_CLASS_PROPERTY,
                                ORB_CLASS);
                        }
                    }

                    if (System.getProperty(
                        OMG_ORB_SINGLETON_CLASS_PROPERTY) == null) {
                        // set ORBSingleton based on JVM vendor
                        if (System.getProperty("java.vendor").equals(
                            "Sun Microsystems Inc.")) {
                            System.setProperty(
                                OMG_ORB_SINGLETON_CLASS_PROPERTY,
                                ORB_SE_SINGLETON_CLASS);
                        } else {
                            // if not Sun, then set to EE class
                            System.setProperty(
                                OMG_ORB_SINGLETON_CLASS_PROPERTY,
                                ORB_SINGLETON_CLASS);
                        }
                    }

                    System.setProperty(ORB_UTIL_CLASS_PROPERTY,
                            RMI_UTIL_CLASS);

                    System.setProperty(RMIIIOP_STUB_DELEGATE_CLASS_PROPERTY,
                            RMI_STUB_CLASS);

                    System.setProperty(RMIIIOP_PRO_DELEGATE_CLASS_PROPERTY,
                            RMI_PRO_CLASS);

                    return null;
                }
            }
        );
    }

    /**
     * Set the ORB properties for IIOP failover and load balancing.
     */
    private void setFOLBProperties(Properties orbInitProperties) {

        orbInitProperties.put(ORBConstants.RFM_PROPERTY, "dummy");

        orbInitProperties.put(ORBConstants.SOCKET_FACTORY_CLASS_PROPERTY,
                IIOP_SSL_SOCKET_FACTORY_CLASS);

        // ClientGroupManager.
        // Registers itself as
        //   ORBInitializer (that registers ClientRequestInterceptor)
        //   IIOPPrimaryToContactInfo
        //   IORToSocketInfo
        orbInitProperties.setProperty(
            ORBConstants.USER_CONFIGURATOR_PREFIX
                + "com.sun.corba.ee.impl.folb.ClientGroupManager",
            "dummy");
         
        // This configurator registers the CSIv2SSLTaggedComponentHandler
        orbInitProperties.setProperty(
            ORBConstants.USER_CONFIGURATOR_PREFIX
                + CSIv2SSLTaggedComponentHandlerImpl.class.getName(),"dummy");
       

        if (processType.isServer()) {
            gmsClient = new IiopFolbGmsClient( services ) ;

            if (gmsClient.isGMSAvailable()) {
                fineLog( "GMS available and enabled - doing EE initialization");

                // Register ServerGroupManager.
                // Causes it to register itself as an ORBInitializer
                // that then registers it as
                // IOR and ServerRequest Interceptors.
                orbInitProperties.setProperty(
                        ORBConstants.USER_CONFIGURATOR_PREFIX
                                + "com.sun.corba.ee.impl.folb.ServerGroupManager",
                        "dummy");

                fineLog( "Did EE property initialization");
            }
        }
    }

    private void initORB(Properties props) {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, ".initORB->: ");
            }

            setORBSystemProperties();

            Properties orbInitProperties = new Properties();
            orbInitProperties.putAll(props);

            orbInitProperties.put(ORBConstants.APPSERVER_MODE, "true");

            // The main configurator.
            orbInitProperties.put(ORBConstants.USER_CONFIGURATOR_PREFIX
                    + PEORB_CONFIG_CLASS, "dummy");

            setFOLBProperties(orbInitProperties);

            // Standard OMG Properties.
            String orbDefaultServerId = DEFAULT_SERVER_ID;
            if (!processType.isServer()) {
                orbDefaultServerId = ACC_DEFAULT_SERVER_ID;               
            }

            orbDefaultServerId = System.getProperty(
                USER_DEFINED_ORB_SERVER_ID_PROPERTY, orbDefaultServerId);

            orbInitProperties.put(ORBConstants.ORB_SERVER_ID_PROPERTY,
                    orbDefaultServerId);

            orbInitProperties.put(OMG_ORB_CLASS_PROPERTY, ORB_CLASS);

            orbInitProperties.put( ORBConstants.PI_ORB_INITIALIZER_CLASS_PREFIX
                + GLASSFISH_INITIALIZER, "");

            orbInitProperties.put(ORBConstants.ALLOW_LOCAL_OPTIMIZATION,
                    "true");

            orbInitProperties.put(
                ORBConstants.GET_SERVICE_CONTEXT_RETURNS_NULL, "true");

            orbInitProperties.put(ORBConstants.ORB_ID_PROPERTY, S1AS_ORB_ID);
            orbInitProperties.put(ORBConstants.SHOW_INFO_MESSAGES, "true");

            // Do this even if propertiesInitialized, since props may override
            // ORBInitialHost and port.
            String initialPort = checkORBInitialPort(orbInitProperties);

            String orbInitialHost = checkORBInitialHost(orbInitProperties);
            String[] orbInitRefArgs;
            if (System.getProperty(IIOP_ENDPOINTS_PROPERTY) != null &&
                    !System.getProperty(IIOP_ENDPOINTS_PROPERTY).isEmpty()) {
                orbInitRefArgs = getORBInitRef(
                    System.getProperty(IIOP_ENDPOINTS_PROPERTY));
            } else {
                // Add -ORBInitRef for INS to work
                orbInitRefArgs = getORBInitRef(orbInitialHost, initialPort);
            }

            // In a server, don't configure any default acceptors so that lazy init
            // can be used.  Actual lazy init setup takes place in PEORBConfigurator
            if (processType.isServer()) {
                validateIiopListeners();
                orbInitProperties.put(ORBConstants.NO_DEFAULT_ACCEPTORS, "true");
                // 14734893 - IIOP ports don't bind to the network address set for the cluster instance
                // GLASSFISH-17469   IIOP Listener Network Address Setting Ignored
                checkORBServerHost(orbInitProperties);
            }

            checkConnectionSettings(orbInitProperties);
            checkMessageFragmentSize(orbInitProperties);
            checkServerSSLOutboundSettings(orbInitProperties);
            checkForOrbPropertyValues(orbInitProperties);

            Collection<GlassFishORBLifeCycleListener> lcListeners =
                    iiopUtils.getGlassFishORBLifeCycleListeners();

            List<String> argsList = new ArrayList<String>();
            argsList.addAll(Arrays.asList(orbInitRefArgs));

            for (GlassFishORBLifeCycleListener listener : lcListeners) {
                listener.initializeORBInitProperties(argsList, orbInitProperties);
            }

            String[] args = argsList.toArray(new String[argsList.size()]);

            // The following is done only on the Server Side to set the
            // ThreadPoolManager in the ORB. ThreadPoolManager on the server
            // is initialized based on configuration parameters found in
            // domain.xml. On the client side this is not done

            if (processType.isServer()) {
                PEORBConfigurator.setThreadPoolManager();
            }

            // orb MUST be set before calling getFVDCodeBaseIOR, or we can
            // recurse back into initORB due to interceptors that run
            // when the TOA supporting the FVD is created!
            // DO NOT MODIFY initORB to return ORB!!!

            /**
             * we can't create object adapters inside the ORB init path, 
             * or else we'll get this same problem in slightly different ways.
             * (address in use exception) Having an IORInterceptor
             * (TxSecIORInterceptor) get called during ORB init always
             * results in a nested ORB.init call because of the call to getORB
             * in the IORInterceptor.i
             */
                
            // TODO Right now we need to explicitly set useOSGI flag.  If it's set to
            // OSGI mode and we're not in OSGI mode, orb initialization fails.  
            boolean useOSGI = false;

            final ClassLoader prevCL = Utility.getClassLoader();
            try {
                Utility.setContextClassLoader(GlassFishORBManager.class.getClassLoader());

                if( processType.isServer()) {

                    Module corbaOrbModule = null;

                    // start glassfish-corba-orb bundle
                    ModulesRegistry modulesRegistry = services.getService(ModulesRegistry.class);

                    for(Module m : modulesRegistry.getModules()) {
                        if( m.getName().equals("glassfish-corba-orb") ) {
                            corbaOrbModule = m;
                            break;
                        }
                    }

                    if( corbaOrbModule != null) {
                        useOSGI = true;
                        corbaOrbModule.start();
                    }
                }
            } finally {
                Utility.setContextClassLoader(prevCL);
            }

            // Can't run with GlassFishORBManager.class.getClassLoader() as the context ClassLoader
            orb = ORBFactory.create() ;
            ORBFactory.initialize( orb, args, orbInitProperties, useOSGI);

            // Done to indicate this is a server and
            // needs to create listen ports.
            try {
                org.omg.CORBA.Object obj =
                        orb.resolve_initial_references("RootPOA");
            } catch (org.omg.CORBA.ORBPackage.InvalidName in) {
                logger.log(Level.SEVERE, "enterprise.orb_reference_exception", in);
            }

            if (processType.isServer()) {
                // This MUST happen before new InitialGroupInfoService,
                // or the ServerGroupManager will get initialized before the
                // GIS is available.
                gmsClient.setORB(orb) ;

                // J2EEServer's persistent server port is same as ORBInitialPort.
                orbInitialPort = getORBInitialPort();

                for (GlassFishORBLifeCycleListener listener : lcListeners) {
                    listener.orbCreated(orb);
                }

                // TODO: The following statement can be moved to
                // some GlassFishORBLifeCycleListeners

                rfm = (ReferenceFactoryManager) orb.resolve_initial_references(
                        ORBConstants.REFERENCE_FACTORY_MANAGER);

                new InitialGroupInfoService( orb ) ;

                iiopUtils.setORB(orb);
            }

            // SeeBeyond fix for 6325988: needs testing.
            // Still do not know why this might make any difference.
            // Invoke this for its side-effects: ignore returned IOR.
            orb.getFVDCodeBaseIOR();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "enterprise_util.excep_in_createorb", ex);
            throw new RuntimeException(ex);
        } finally {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, ".initORB<-: ");
            }
        }
    }

    private String checkForAddrAny(Properties props, String orbInitialHost) {
        if ((orbInitialHost.equals("0.0.0.0")) || (orbInitialHost.equals("::"))
                || (orbInitialHost.equals("::ffff:0.0.0.0"))) {
            try {
                String localAddress = java.net.InetAddress.getLocalHost().getHostAddress();
                return localAddress;
            } catch (java.net.UnknownHostException uhe) {
                logger.log(Level.WARNING,
                    "Unknown host exception - Setting host to localhost");
                return DEFAULT_ORB_INIT_HOST;
            }
        } else {
            return orbInitialHost;
        }
    }

    // Returns the first IiopListenerBean which represents a clear text endpoint
    // Note: it is questionable whether the system actually support multiple
    // endpoints of the same type, or no clear text endpoint at all in the 
    // configuration.
    private IiopListener getClearTextIiopListener() {
        if (iiopListeners != null)  {
            for (IiopListener il : iiopListeners) {
                if (il.getSsl() == null) {
                    return il ;
                }
            }
        }

        return null ;
    }

    private String checkORBInitialHost(Properties props) {
        // Host setting in system properties always takes precedence.
        String initialHost = System.getProperty(
            ORBConstants.INITIAL_HOST_PROPERTY);

        if (initialHost == null) {
            initialHost = props.getProperty(
                ORBConstants.INITIAL_HOST_PROPERTY );
        }

        if (initialHost == null) {
            IiopListener il = getClearTextIiopListener() ;
            if (il != null) {
                initialHost = il.getAddress();
            }
        }

        if (initialHost == null) {
            initialHost = DEFAULT_ORB_INIT_HOST;
        }

        initialHost = checkForAddrAny(props, initialHost);

        props.setProperty(ORBConstants.INITIAL_HOST_PROPERTY, initialHost);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Setting orb initial host to {0}",
                initialHost);
        }

        return initialHost;
    }

    private String checkORBInitialPort(Properties props) {
        // Port setting in system properties always takes precedence.
        String initialPort = System.getProperty(
            ORBConstants.INITIAL_PORT_PROPERTY );

        if (initialPort == null) {
            initialPort = props.getProperty(
                ORBConstants.INITIAL_PORT_PROPERTY);
        }

        if (initialPort == null) {
            IiopListener il = getClearTextIiopListener() ;
            if (il != null) {
                initialPort = il.getPort();
            }
        }


        if (initialPort == null) {
            initialPort = DEFAULT_ORB_INIT_PORT;
        }

        // Make sure we set initial port in System properties so that
        // any instantiations of com.sun.jndi.cosnaming.CNCtxFactory
        // use same port.
        props.setProperty(ORBConstants.INITIAL_PORT_PROPERTY, initialPort);


        // Done to initialize the Persistent Server Port, before any
        // POAs are created. This was earlier done in POAEJBORB
        // Do it only in the appserver, not on appclient.  
        if (processType.isServer()) {
            props.setProperty(ORBConstants.PERSISTENT_SERVER_PORT_PROPERTY,
                    initialPort);
        }
       
        fineLog( "Setting orb initial port to {0}", initialPort);

        orbInitialPort = new Integer(initialPort).intValue();

        return initialPort;
    }

    // Server host property is used only for ORB running in server mode
    // Return host name (or ip address string) if the SERVER_HOST PROPERTY is set or
    // network-address attribute is specified in iiop-listener element
    // Return null otherwise.
    private String checkORBServerHost(Properties props) {
        // Host setting in system properties always takes precedence.
        String serverHost = System.getProperty(ORBConstants.SERVER_HOST_PROPERTY);

        if (serverHost == null) {
            serverHost = props.getProperty(ORBConstants.SERVER_HOST_PROPERTY );
        }


        if (serverHost == null) {
            IiopListener il = getClearTextIiopListener() ;
            if (il != null) {
                // For this case, use same value as ORBInitialHost,
                serverHost = il.getAddress();
            }
        }

        if (serverHost != null) {
            // set the property, to be used during ORB initialization
            // Bug 14734893 - IIOP ports don't bind to the network address set for the cluster instance
            props.setProperty(ORBConstants.SERVER_HOST_PROPERTY, serverHost);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Setting orb server host to {0}", serverHost);
            }
        }

        return serverHost;
    }

    private void validateIiopListeners() {
        if (iiopListeners != null) {
            int lazyCount = 0 ;
            for (IiopListener ilb : iiopListeners) {
                boolean securityEnabled = Boolean.valueOf( ilb.getSecurityEnabled() ) ;
                boolean isLazy = Boolean.valueOf( ilb.getLazyInit() ) ;
                if( isLazy ) {
                    lazyCount++;
                }

                if (lazyCount > 1) {
                    throw new IllegalStateException(
                        "Invalid iiop-listener " + ilb.getId() +
                        ". Only one iiop-listener can be configured with lazy-init=true");
                }

                if (securityEnabled || ilb.getSsl() == null) {
                    // no-op
                } else {
                    if (isLazy) {
                        throw new IllegalStateException("Invalid iiop-listener " + ilb.getId() +
                                ". Lazy-init not supported for SSL iiop-listeners");
                    }

                    Ssl sslBean = ilb.getSsl() ;
                    assert sslBean != null ;
                }
            }
        }
    }

    private void checkConnectionSettings(Properties props) {
        if (orbBean != null) {
            String maxConnections;

            try {
                maxConnections = orbBean.getMaxConnections();

                // Validate number formats
                Integer.parseInt(maxConnections);
            } catch (NumberFormatException nfe) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING,
                        "enterprise_util.excep_orbmgr_numfmt", nfe);
                }

                maxConnections = DEFAULT_MAX_CONNECTIONS;
            }

            props.setProperty(ORBConstants.HIGH_WATER_MARK_PROPERTY,
                maxConnections);
        }
        return;
    }

    private void checkMessageFragmentSize(Properties props) {
        if (orbBean != null) {
            String fragmentSize, bufferSize;
            try {
                int fsize = ((Integer.parseInt(orbBean.getMessageFragmentSize().trim())) / 8) * 8;
                if (fsize < 32) {
                    fragmentSize = "32";
                    logger.log(Level.INFO, 
                        "Setting ORB Message Fragment size to {0}",
                            fragmentSize);
                } else {
                    fragmentSize = String.valueOf(fsize);
                }
                bufferSize = fragmentSize;
            } catch (NumberFormatException nfe) {
                // Print stack trace and use default values
                logger.log(Level.WARNING,
                    "enterprise_util.excep_in_reading_fragment_size", nfe);
                logger.log(Level.INFO,
                    "Setting ORB Message Fragment size to Default " +
                    SUN_GIOP_DEFAULT_FRAGMENT_SIZE);
                fragmentSize = SUN_GIOP_DEFAULT_FRAGMENT_SIZE;
                bufferSize = SUN_GIOP_DEFAULT_BUFFER_SIZE;
            }
            props.setProperty(ORBConstants.GIOP_FRAGMENT_SIZE,
                fragmentSize);
            props.setProperty(ORBConstants.GIOP_BUFFER_SIZE,
                bufferSize);
        }
    }

    private void checkServerSSLOutboundSettings(Properties props) {
        if (iiopService != null) {
            SslClientConfig sslClientConfigBean =
                iiopService.getSslClientConfig();
            if (sslClientConfigBean != null) {
                Ssl ssl = sslClientConfigBean.getSsl();
                assert (ssl != null);
            }
        }
    }

    private void checkForOrbPropertyValues(Properties props) {
        if (orbBean != null) {
            List<Property> orbBeanProps = orbBean.getProperty();
            if (orbBeanProps != null) {
                for (int i = 0; i < orbBeanProps.size(); i++) {
                    props.setProperty(orbBeanProps.get(i).getName(),
                    orbBeanProps.get(i).getValue());
                }
            }
        }
    }

    private String[] getORBInitRef(String orbInitialHost,
                                          String initialPort) {
        // Add -ORBInitRef NameService=....
        // This ensures that INS will be used to talk with the NameService.
        String[] newArgs = new String[]{
                "-ORBInitRef",
                "NameService=corbaloc:iiop:1.2@"
                        + orbInitialHost + ":"
                        + initialPort + "/NameService"
        };

        return newArgs;
    }

    private String[] getORBInitRef(String endpoints) {

        String[] list = endpoints.split(",");
        String corbalocURL = getCorbalocURL(list);
        logger.log(Level.FINE, "GlassFishORBManager.getORBInitRef = {0}",
            corbalocURL);

        // Add -ORBInitRef NameService=....
        // This ensures that INS will be used to talk with the NameService.
        String[] newArgs = new String[]{
                "-ORBInitRef",
                "NameService=corbaloc:" + corbalocURL + "/NameService"
        };

        return newArgs;
    }

    // TODO : Move this to naming  NOT needed for V3 FCS

    public static final String IIOP_ENDPOINTS_PROPERTY =
            "com.sun.appserv.iiop.endpoints";

    private static final String IIOP_URL = "iiop:1.2@";

    private String getCorbalocURL(Object[] list) {

        String corbalocURL = "";
        //convert list into corbaloc url
        for (int i = 0; i < list.length; i++) {
            logger.log(Level.INFO, "list[i] ==> {0}", list[i]);
            if (corbalocURL.equals("")) {
                corbalocURL = IIOP_URL + ((String) list[i]).trim();
            } else {
                corbalocURL = corbalocURL + "," +
                        IIOP_URL + ((String) list[i]).trim();
            }
        }
        logger.log(Level.INFO, "corbaloc url ==> {0}", corbalocURL);
        return corbalocURL;
    }

    String getIIOPEndpoints() {
        return gmsClient.getIIOPEndpoints() ;
    }
}
