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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package com.sun.enterprise.connectors.jms.system;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.JmxConnector;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.connectors.inbound.ActiveInboundResourceAdapterImpl;
import com.sun.enterprise.connectors.jms.JMSLoggerInfo;
import com.sun.enterprise.connectors.jms.config.JmsHost;
import com.sun.enterprise.connectors.jms.config.JmsService;
import com.sun.enterprise.connectors.jms.inflow.MdbContainerProps;
import com.sun.enterprise.connectors.jms.util.JmsRaUtil;
import com.sun.enterprise.connectors.service.ConnectorAdminServiceUtils;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.connectors.util.SetMethodAction;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.ConnectorConfigProperty;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.JMSDestinationDefinitionDescriptor;
import com.sun.enterprise.deployment.MessageDestinationDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.runtime.BeanPoolDescriptor;
import com.sun.enterprise.util.JDK;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.Result;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.v3.services.impl.DummyNetworkListener;
import com.sun.enterprise.v3.services.impl.GrizzlyService;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.rmi.Naming;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapterInternalException;
import org.glassfish.admin.mbeanserver.JMXStartupService;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.connectors.config.AdminObjectResource;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.ConnectorResource;
import org.glassfish.connectors.config.ResourceAdapterConfig;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ORBLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.grizzly.LazyServiceInitializer;
import org.glassfish.resourcebase.resources.api.ResourceConstants;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;


/**
 * Represents an active JMS resource adapter. This does
 * additional configuration to ManagedConnectionFactory
 * and ResourceAdapter java beans.
 *
 * XXX: For code management reasons, think about splitting this
 * to a preHawk and postHawk RA (with postHawk RA extending preHawk RA).
 *
 * @author Satish Kumar
 */
@Service
@Singleton
@Named(ActiveJmsResourceAdapter.JMS_SERVICE)
public class ActiveJmsResourceAdapter extends ActiveInboundResourceAdapterImpl implements LazyServiceInitializer, PostConstruct {

    private static final Logger _logger = JMSLoggerInfo.getLogger();

    private final String SETTER = "setProperty";

    //RA Javabean properties.
    public static final String CONNECTION_URL = "ConnectionURL";
    private final String RECONNECTENABLED = "ReconnectEnabled";
    private final String RECONNECTINTERVAL = "ReconnectInterval";
    private final String RECONNECTATTEMPTS = "ReconnectAttempts";
    private static final String GROUPNAME = "GroupName";
    private static final String CLUSTERCONTAINER = "InClusteredContainer";

    //Lifecycle RA JavaBean properties
    public static final String BROKERTYPE="BrokerType";
    private static final String BROKERINSTANCENAME="BrokerInstanceName";
    private static final String BROKERPORT="BrokerPort";
    private static final String BROKERARGS="BrokerArgs";
    private static final String BROKERHOMEDIR="BrokerHomeDir";
    private static final String BROKERLIBDIR ="BrokerLibDir";
    private static final String BROKERVARDIR="BrokerVarDir";
    private static final String BROKERJAVADIR="BrokerJavaDir";
    private static final String BROKERSTARTTIMEOUT="BrokerStartTimeOut";
    public static final String ADMINUSERNAME="AdminUsername";
    public static final String ADMINPASSWORD="AdminPassword";
    private static final String USERNAME="UserName";
    private static final String PASSWORD="Password";
    private static final String MQ_PORTMAPPER_BIND = "doBind";//"imq.portmapper.bind";

    //JMX properties
    private static final String RMIREGISTRYPORT="RmiRegistryPort";
    private static final String USEEXTERNALRMIREGISTRY="startRMIRegistry";
    private static final int DEFAULTRMIREGISTRYPORT =7776;
    private static final int BROKERRMIPORTOFFSET=100;

    private static final String DB_HADB_PROPS = "DBProps";

    //Activation config properties of MQ resource adapter.
    public static final String DESTINATION        = "destination";
    public static final String DESTINATION_TYPE   = "destinationType";
    private static final String SUBSCRIPTION_NAME  = "SubscriptionName";
    private static final String CLIENT_ID          = "ClientID";
    public static final String PHYSICAL_DESTINATION  = "Name";
    private static final String MAXPOOLSIZE  = "EndpointPoolMaxSize";
    private static final String MINPOOLSIZE  = "EndpointPoolSteadySize";
    private static final String RESIZECOUNT  = "EndpointPoolResizeCount";
    private static final String RESIZETIMEOUT  = "EndpointPoolResizeTimeout";
    private static final String REDELIVERYCOUNT  = "EndpointExceptionRedeliveryAttempts";
    private static final String LOWERCASE_REDELIVERYCOUNT  = "endpointExceptionRedeliveryAttempts";
    public static final String ADDRESSLIST  = "AddressList";
    private static final String ADRLIST_BEHAVIOUR  = "AddressListBehavior";
    private static final String ADRLIST_ITERATIONS  = "AddressListIterations";
    public static final String JMS_SERVICE = "mq-service";

    //MCF properties
    private static final String MCFADDRESSLIST = "MessageServiceAddressList";

    private final StringManager sm = StringManager.getManager(ActiveJmsResourceAdapter.class);

    private MQAddressList urlList = null;

    private String addressList;

    private String brkrPort;

    private boolean doBind;

    //Lifecycle properties
    public static final String EMBEDDED="EMBEDDED";
    public static final String LOCAL="LOCAL";
    public static final String REMOTE="REMOTE";
    public static final String DIRECT="DIRECT";

    // Both the properties below are hacks. These will be changed later on.
    private static final String MQRmiPort =
        System.getProperty("com.sun.enterprise.connectors.system.MQRmiPort");
    private static final String DASRMIPORT = "31099";

    private static final String REVERT_TO_EMBEDDED_PROPERTY = "com.sun.enterprise.connectors.system.RevertToEmbedded";
    private static final String BROKER_RMI_PORT = "com.sun.enterprise.connectors.system.mq.rmiport";

    private static final String DEFAULT_SERVER = "server";
    private static final String DEFAULT_MQ_INSTANCE = "imqbroker";
    public static final String MQ_DIR_NAME = "imq";

    public static final String GRIZZLY_PROXY_PREFIX = "JMS_PROXY_";

    private Properties dbProps = null;
    private String brokerInstanceName = null;

    private boolean grizzlyListenerInit;
    private final Set<String> grizzlyListeners = new HashSet<String>();

    @Inject
    private ConnectorRuntime connectorRuntime;

    @Inject
    private GlassfishNamingManager nm;

    @Inject
    private Provider<JMSConfigListener> jmsConfigListenerProvider;

    @Inject
    private Provider<ServerEnvironmentImpl> serverEnvironmentImplProvider;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Provider<AdminService> adminServiceProvider;

    @Inject
    private Provider<Servers> serversProvider;

    @Inject
    private Provider<ServerContext> serverContextProvider;

    @Inject
    private Provider<ConnectorRuntime> connectorRuntimeProvider;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private ApplicationRegistry appRegistry;

    @Inject
    InvocationManager invManager;

    /**
     * Constructor for an active Jms Adapter.
     *
     */
    public ActiveJmsResourceAdapter() {
        super();
    }

    @Override
    public void postConstruct() {
        /*
         * If any special handling is required for the system resource
         * adapter, then ActiveResourceAdapter implementation for that
         * RA should implement additional functionality by extending
         * ActiveInboundResourceAdapter or ActiveOutboundResourceAdapter.
         *
         * For example ActiveJmsResourceAdapter extends
         * ActiveInboundResourceAdapter.
         */
        //if (moduleName.equals(ConnectorConstants.DEFAULT_JMS_ADAPTER)) {
        // Upgrade jms resource adapter, if necessary before starting
        // the RA.
        try {
            JMSConfigListener jmsConfigListener = jmsConfigListenerProvider.get();
            jmsConfigListener.setActiveResourceAdapter(this);
            JmsRaUtil raUtil = new JmsRaUtil();
            raUtil.upgradeIfNecessary();
        } catch (Throwable t) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Cannot upgrade jmsra" + t.getMessage());
            }
        }
    }

    /**
     * Loads RA configuration for MQ Resource adapter.
     *
     * @throws ConnectorRuntimeException in case of an exception.
     */
    @Override
    protected void loadRAConfiguration() throws ConnectorRuntimeException{
        JmsService jmsService = getJmsService();
        if (jmsService != null && jmsService.getType().equals("DISABLED")) {
            throw new ConnectorRuntimeException("JMS Broker is Disabled");
        }

        if (connectorRuntime.isServer()) {
            try {
                setLifecycleProperties();
            } catch (Exception e) {
                ConnectorRuntimeException cre = new ConnectorRuntimeException (e.getMessage(), e);
                throw cre;
            }

            setMdbContainerProperties();
            setJmsServiceProperties(null);
        } else {
            setAppClientRABeanProperties();
        }
        super.loadRAConfiguration();
        postRAConfiguration();
    }

    @Override
    public void destroy() {
        try {
            JmsService jmsService = getJmsService();

            if (jmsService != null && jmsService.getType().equals("DISABLED")) {
                return;
            }

            if (connectorRuntime.isServer() && grizzlyListenerInit && jmsService != null
                    && EMBEDDED.equalsIgnoreCase(jmsService.getType())) {
                GrizzlyService grizzlyService = null;
                try {
                    grizzlyService = Globals.get(GrizzlyService.class);
                } catch (MultiException rle) {
                    // if GrizzlyService was shut down already, skip removing the proxy.
                }
                if (grizzlyService != null) {
                    synchronized (grizzlyListeners) {
                        if (grizzlyListeners.size() > 0) {
                            String[] listeners = grizzlyListeners.toArray(new String[grizzlyListeners.size()]);
                            for (String listenerName : listeners) {
                                try {
                                    grizzlyService.removeNetworkProxy(listenerName);
                                    grizzlyListeners.remove(listenerName);
                                } catch (Exception e) {
                                    LogHelper.log(_logger, Level.WARNING,
                                            JMSLoggerInfo.SHUTDOWN_FAIL_GRIZZLY, e, listenerName);
                                }
                            }
                        }
                    }
                }
                grizzlyListenerInit = false;
            }
        } catch (Throwable th) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, JMSLoggerInfo.SHUTDOWN_FAIL_JMSRA,
                        new Object[]{th.getMessage()});
            }
            throw new RuntimeException(th);
        }
        super.destroy();
    }

    public Set<String> getGrizzlyListeners() {
        return grizzlyListeners;
    }

    /**
     * Start Grizzly based JMS lazy listener, which is going to initialize
     * JMS container on first request.
     * @param jmsService
     * @throws com.sun.enterprise.connectors.jms.system.JmsInitialisationException
     */
    public void initializeLazyListener(JmsService jmsService) throws JmsInitialisationException {
        if (jmsService != null && jmsService.getType().equals("DISABLED")) {
            return;
        }

        if (jmsService != null) {
            if (EMBEDDED.equalsIgnoreCase(jmsService.getType()) && !grizzlyListenerInit) {
                GrizzlyService grizzlyService = Globals.get(GrizzlyService.class);
                if (grizzlyService != null) {
                    List<JmsHost> jmsHosts = jmsService.getJmsHost();
                    for (JmsHost oneHost : jmsHosts) {
                        if (Boolean.valueOf(oneHost.getLazyInit()) && !doBind) {
                            String jmsHost = null;
                            if (oneHost.getHost() != null && "localhost".equals(oneHost.getHost())) {
                                jmsHost = "0.0.0.0";
                            } else {
                                jmsHost = oneHost.getHost();
                            }
                            NetworkListener dummy = new DummyNetworkListener();
                            dummy.setPort(oneHost.getPort());
                            dummy.setAddress(jmsHost);
                            dummy.setType("proxy");
                            dummy.setProtocol(JMS_SERVICE);
                            dummy.setTransport("tcp");
                            String name = GRIZZLY_PROXY_PREFIX + oneHost.getName();
                            dummy.setName(name);
                            synchronized (grizzlyListeners) {
                                Future<Result<Thread>> createNetworkProxy = grizzlyService.createNetworkProxy(dummy);
                                try {
                                    Result<Thread> result = createNetworkProxy.get();
                                    if (result.exception() != null) {
                                        throw new JmsInitialisationException(MessageFormat.format("Cannot initialise JMS broker listener on {0}:{1}", oneHost.getHost(), oneHost.getPort()), result.exception());
                                    }
                                } catch (InterruptedException | ExecutionException ex) {
                                    Logger.getLogger(ActiveJmsResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                grizzlyListeners.add(name);
                            }
                            grizzlyListenerInit = true;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void startResourceAdapter(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        JmsService jmsService = getJmsService();

        if (jmsService != null && jmsService.getType().equals("DISABLED")) {
            return;
        }

        try {
            if (this.moduleName_.equals(ConnectorRuntime.DEFAULT_JMS_ADAPTER)) {
                if (connectorRuntime.isServer()) {
                    Domain domain = Globals.get(Domain.class);
                    ServerContext serverContext = Globals.get(ServerContext.class);
                    Server server = domain.getServerNamed(serverContext.getInstanceName());
                    try {
                        initializeLazyListener(jmsService);
                    } catch (Throwable ex) {
                        Logger.getLogger(ActiveJmsResourceAdapter.class.getName()).log(Level.SEVERE, null, ex);
                        throw new ResourceAdapterInternalException(ex);
                    }
                }
                AccessController.doPrivileged
                        (new java.security.PrivilegedExceptionAction() {
                            public Object run() throws
                                    ResourceAdapterInternalException {
                                //set the JMSRA system property to enable XA JOINS
                                //disabling this due to issue - 8727
                                //System.setProperty(XA_JOIN_ALLOWED, "true");

                                // to prevent classloader leaks in new threads clear invocation manager before bootstrapping JMS
                                resourceadapter_.start(bootStrapContextImpl);
                                return null;
                            }
                        });
                //setResourceAdapter(resourceadapter_);
            } else {
                resourceadapter_.start(bootStrapContextImpl);
            }
        } catch (PrivilegedActionException ex) {
            throw new ResourceAdapterInternalException(ex);
        }
    }

    /**
     * This is a HACK to remove the connection URL
     * in the case of PE LOCAL/EMBEDDED before setting the properties
     * to the RA. If this was not done, MQ RA incorrectly assumed
     * that the passed in connection URL is one additional
     * URL, apart from the default URL derived from brokerhost:brokerport
     * and reported a PE connection url limitation.
     *
     * @return
     */
    @Override
    protected Set mergeRAConfiguration(ResourceAdapterConfig raConfig, List<Property> raConfigProps) {
        if (!(connectorRuntime.isServer())) {
            return super.mergeRAConfiguration(raConfig, raConfigProps);
        }
        Set mergedProps = super.mergeRAConfiguration(raConfig, raConfigProps);
        String brokerType = null;

        for (Iterator iter = mergedProps.iterator(); iter.hasNext(); ) {
            ConnectorConfigProperty element = (ConnectorConfigProperty) iter.next();
            if (element.getName().equals(ActiveJmsResourceAdapter.BROKERTYPE)) {
                brokerType = element.getValue();
            }
        }
        if (brokerType.equals(ActiveJmsResourceAdapter.LOCAL) || brokerType.equals(ActiveJmsResourceAdapter.EMBEDDED)
                || brokerType.equals(ActiveJmsResourceAdapter.DIRECT)) {
            for (Iterator iter = mergedProps.iterator(); iter.hasNext();) {
                ConnectorConfigProperty element = (ConnectorConfigProperty) iter.next();
                if (element.getName().equals(ActiveJmsResourceAdapter.CONNECTION_URL)) {
                    iter.remove();
                }
            }
        }
        return mergedProps;
    }

    //Overriding ActiveResourceAdapterImpl.setup() as a work around for
    //this condition - connectionDefs_.length != 1
    //Need to remove this once the original problem is fixed
    @Override
    public void setup() throws ConnectorRuntimeException {
        //TODO NEED TO REMOVE ONCE THE ActiveResourceAdapterImpl.setup() is fixed
        JmsService jmsService = getJmsService();

        if (jmsService != null && jmsService.getType().equals("DISABLED")) {
            return;
        }


        if (connectionDefs_ == null) {
            throw new ConnectorRuntimeException("No Connection Defs defined in the RA.xml");
        }
        if (isServer() && !isSystemRar(moduleName_)) {
            createAllConnectorResources();
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Completed Active Resource adapter setup", moduleName_);
        }
    }

    /**
     * Method to perform any post RA configuration action by derivative subclasses.
     * For example, this method is used by <code>ActiveJMSResourceAdapter</code>
     * to set unsupported javabean property types on its RA JavaBean runtime
     * instance.
     * @throws ConnectorRuntimeException
     */
    protected void postRAConfiguration() throws ConnectorRuntimeException {
        //Set all non-supported javabean property types in the JavaBean
        try {
            if(dbProps == null)
                dbProps = new Properties();
            dbProps.setProperty("imq.cluster.dynamicChangeMasterBrokerEnabled", "true");

            Method mthds = this.resourceadapter_.getClass().getMethod("setBrokerProps", Properties.class);
            if (_logger.isLoggable(Level.FINE))
                logFine("Setting property:" + DB_HADB_PROPS + "=" + dbProps.toString());
            mthds.invoke(this.resourceadapter_, dbProps);
        } catch (Exception e) {
            ConnectorRuntimeException crex = new ConnectorRuntimeException(
                            e.getMessage(), e);
            throw crex;
        }
    }


    /**
     * Set MQ4.0 RA lifecycle properties
     */
    private void setLifecycleProperties() {
        //If PE:
        //EMBEDDED/LOCAL goto jms-service, get defaultjmshost info and set
        //accordingly
        //if EE:
        //EMBEDDED/LOCAL get this instance and cluster name, search for a
        //jms-host wth this this name in jms-service gets its proeprties
        //and set
        //@siva As of now use default JMS host. As soon as changes for modifying EE
        //cluster to LOCAL is brought in, change this to use system properties
        //for EE to get port, host, adminusername, adminpassword.
        //JmsService jmsService = ServerBeansFactory.getJmsServiceBean(ctx);
        String defaultJmsHost = getJmsService().getDefaultJmsHost();
        if (_logger.isLoggable(Level.FINE)) {
            logFine("Default JMS Host :: " + defaultJmsHost);
        }

        JmsHost jmsHost = getJmsHost();


        if (jmsHost != null) {//todo: && jmsHost.isEnabled()) {
            JavaConfig javaConfig = Globals.get(JavaConfig.class);
            String java_home = javaConfig.getJavaHome();

            //Get broker type from JMS Service.
            // String brokerType = jmsService.getType();
            /*
             * XXX: adjust the brokertype for the new DIRECT mode in 4.1
             * uncomment the line below once we have an MQ integration
             * that has DIRECT mode support
             */
            String brokerType = adjustForDirectMode(getJmsService().getType());

            String brokerPort = jmsHost.getPort();
            brkrPort = brokerPort;
            String adminUserName = jmsHost.getAdminUserName();
            String adminPassword = JmsRaUtil.getUnAliasedPwd(jmsHost.getAdminPassword());
            List jmsHostProps = getJmsService().getProperty();

            String username = null;
            String password = null;
            if (jmsHostProps != null) {
                for (int i = 0; i < jmsHostProps.size(); i++) {
                    Property jmsProp = (Property) jmsHostProps.get(i);
                    String propName = jmsProp.getName();
                    String propValue = jmsProp.getValue();
                    if ("user-name".equals(propName)) {
                        username = propValue;
                    } else if ("password".equals(propName)) {
                        password = propValue;
                    }
                    // Add more properties as and when you want.
                }
            }

            if (_logger.isLoggable(Level.FINE))
                logFine("Broker UserName = " + username);
            createMQVarDirectoryIfNecessary();
            String brokerVarDir = getMQVarDir();

            String tmpString = getJmsService().getStartArgs();
            if (tmpString == null) {
                tmpString = "";
            }
            String brokerArgs = tmpString;

            //condition to evaluate the JDK and if version greater than 8 then omit the jre folder
            //by adding the start-args for the broker configuration
            if (!tmpString.contains("-jrehome") && availableJDKForStartArgs()) {
                brokerArgs = brokerArgs + buildStartArgsForJREHome(java_home);
            }

            //XX: Extract the information from the optional properties.
            List jmsProperties = getJmsService().getProperty();
            List jmsHostProperties = jmsHost.getProperty();
            Properties jmsServiceProp = listToProperties(jmsProperties);
            Properties jmsHostProp = listToProperties(jmsHostProperties);

            jmsServiceProp.putAll(jmsHostProp);
            if (jmsServiceProp.size() > 0) {
                if (dbProps == null)
                    dbProps = new Properties();

                dbProps.putAll(jmsServiceProp);
            }

            String brokerHomeDir = getBrokerHomeDir();
            String brokerLibDir = getBrokerLibDir();
            if (brokerInstanceName == null) {
                brokerInstanceName = getBrokerInstanceName(getJmsService());
            }

            long brokerTimeOut = getBrokerTimeOut(getJmsService());

            //Need to set the following properties
            //BrokerType, BrokerInstanceName, BrokerPort,
            //BrokerArgs, BrokerHomeDir, BrokerVarDir, BrokerStartTimeout
            //adminUserName, adminPassword
            ConnectorDescriptor cd = getDescriptor();
            ConnectorConfigProperty envProp1 = new ConnectorConfigProperty(
                    BROKERTYPE, brokerType, "Broker Type", "java.lang.String");
            setProperty(cd, envProp1);
            ConnectorConfigProperty envProp2 = new ConnectorConfigProperty(
                    BROKERINSTANCENAME, brokerInstanceName,
                    "Broker Instance Name", "java.lang.String");
            setProperty(cd, envProp2);
            ConnectorConfigProperty envProp3 = new ConnectorConfigProperty(
                    BROKERPORT, brokerPort,
                    "Broker Port", "java.lang.String");
            setProperty(cd, envProp3);
            ConnectorConfigProperty envProp4 = new ConnectorConfigProperty(
                    BROKERARGS, brokerArgs,
                    "Broker Args", "java.lang.String");
            setProperty(cd, envProp4);
            ConnectorConfigProperty envProp5 = new ConnectorConfigProperty(
                    BROKERHOMEDIR, brokerHomeDir,
                    "Broker Home Dir", "java.lang.String");
            setProperty(cd, envProp5);
            ConnectorConfigProperty envProp14 = new ConnectorConfigProperty(
                    BROKERLIBDIR, brokerLibDir,
                    "Broker Lib Dir", "java.lang.String");
            setProperty(cd, envProp14);
            ConnectorConfigProperty envProp6 = new ConnectorConfigProperty(
                    BROKERJAVADIR, java_home,
                    "Broker Java Dir", "java.lang.String");
            setProperty(cd, envProp6);
            ConnectorConfigProperty envProp7 = new ConnectorConfigProperty(
                    BROKERVARDIR, brokerVarDir,
                    "Broker Var Dir", "java.lang.String");
            setProperty(cd, envProp7);
            ConnectorConfigProperty envProp8 = new ConnectorConfigProperty(
                    BROKERSTARTTIMEOUT, "" + brokerTimeOut,
                    "Broker Start Timeout", "java.lang.String");
            setProperty(cd, envProp8);
            ConnectorConfigProperty envProp9 = new ConnectorConfigProperty(
                    ADMINUSERNAME, adminUserName,
                    "Broker admin username", "java.lang.String");
            setProperty(cd, envProp9);
            ConnectorConfigProperty envProp10 = new ConnectorConfigProperty(
                    ADMINPASSWORD, adminPassword,
                    "Broker admin password", "java.lang.String");
            setProperty(cd, envProp10);
            ConnectorConfigProperty envProp11 = new ConnectorConfigProperty(
                    USERNAME, username,
                    "Broker username", "java.lang.String");
            setProperty(cd, envProp11);
            ConnectorConfigProperty envProp12 = new ConnectorConfigProperty(
                    PASSWORD, password,
                    "Broker password", "java.lang.String");
            setProperty(cd, envProp12);
        }
    }

    /**
     * This method evaluate the current JDK and return true if the version is greater than 8. This indicator
     * is used to include the start-args attribute for the broker configuration with the jrehome path attribute
     *
     * @return boolean indicator to create the start-args attribute for the broker
     */
    private boolean availableJDKForStartArgs() {
        return JDK.getMajor() > 8;
    }

    /**
     * This method build the start-args attribute with the value -jrehome for the jms broker configuration
     *
     * @param javaHome String with the path to use for the -jrehome attribute
     * @return String with the formed start-args attribute
     */
    private String buildStartArgsForJREHome(String javaHome) {
        return " -jrehome "+"\""+javaHome+"\"";
    }

    private Properties listToProperties(List<Property> props) {
        Properties properties = new Properties();
        if (props != null) {
            for (Property prop : props) {
                String key = prop.getName();
                String value = prop.getValue();

                properties.setProperty(key, value);
            }
        }

        return properties;
    }

    private String adjustForDirectMode(String brokerType) {
        if (brokerType.equals(EMBEDDED)) {
            String revertToEmbedded = System.getProperty(REVERT_TO_EMBEDDED_PROPERTY);
            if ((revertToEmbedded != null) && (revertToEmbedded.equals("true"))){
                return EMBEDDED;
            }
            return DIRECT;
        }
        return brokerType;
    }

    private long getBrokerTimeOut(JmsService jmsService) {
        //@@remove
        long defaultTimeout = 30 * 1000; //30 seconds
        long timeout = defaultTimeout;

        String specifiedTimeOut = jmsService.getInitTimeoutInSeconds();
        if (specifiedTimeOut != null)
            timeout = Integer.parseInt(specifiedTimeOut) * 1000L;
        return timeout;
    }

    public static String getBrokerInstanceName(JmsService js){
        ServerEnvironmentImpl serverEnv = Globals.get(ServerEnvironmentImpl.class);
        String asInstance = serverEnv.getInstanceName();
        String domainName = serverEnv.getDomainName();
        String s = getBrokerInstanceName(domainName, asInstance, js);
        if (_logger.isLoggable(Level.FINE)) {
            logFine("Got broker Instancename as " + s);
        }
        String converted = convertStringToValidMQIdentifier(s);
        if (_logger.isLoggable(Level.FINE)) {
            logFine("converted instance name " + converted);
        }
        return converted;
    }

    @Override
    public boolean handles(ConnectorDescriptor cd, String moduleName) {
        return ConnectorsUtil.isJMSRA(moduleName);
    }

    @Override
    public void validateActivationSpec(ActivationSpec spec) {
        boolean validate =  "true".equals(System.getProperty("validate.jms.ra"));
        if (validate) {
            try {
                spec.validate();
            } catch (Exception ex) {
                LogHelper.log(_logger, Level.SEVERE, JMSLoggerInfo.ENDPOINT_VALIDATE_FAILED, ex);
            }
        }
    }

     /**
     * Computes the instance name for the MQ broker.
     */
    private static String getBrokerInstanceName(String asDomain, String asInstance, JmsService js) {
        List jmsProperties = js.getProperty();

        String instanceName = null;
        String suffix = null;

        if (jmsProperties != null) {
            for (int ii=0; ii < jmsProperties.size(); ii++) {
                Property p = (Property)jmsProperties.get(ii);
                String name = p.getName();

                if (name.equals("instance-name"))
                    instanceName = p.getValue();
                if (name.equals("instance-name-suffix"))
                    suffix = p.getValue();
                if (name.equals("append-version") &&
                    Boolean.valueOf(p.getValue()).booleanValue()) {
                    suffix = Version.getMajorVersion() + "_" +
                        Version.getMinorVersion();
                }
            }
        }

        if (instanceName != null)
            return instanceName;

        if (asInstance.equals(DEFAULT_SERVER)) {
            instanceName = DEFAULT_MQ_INSTANCE;
        } else {
            instanceName = asDomain + "_" + asInstance;
        }

        if (suffix != null)
            instanceName = instanceName + "_" + suffix;

        return instanceName;
    }

    private void createMQVarDirectoryIfNecessary(){
        String asInstanceRoot = getServerEnvironment().getInitFilePath().getPath();
                /*ApplicationServer.getServerContext().
                                   getInstanceEnvironment().getInstancesRoot();   */
        String mqInstanceDir =  asInstanceRoot + java.io.File.separator
                                                  + MQ_DIR_NAME;
         // If the directory doesn't exist, create it.
         // It is necessary for windows.
         java.io.File instanceDir = new java.io.File(mqInstanceDir);
         if (!(instanceDir.exists() && instanceDir.isDirectory())) {
             if (!instanceDir.mkdirs()) {
                 if (_logger.isLoggable(Level.FINE)) {
                     _logger.log(Level.FINE, "Failed to create dir: " + instanceDir);
                 }
             }
         }
    }

    private String getMQVarDir(){
        String asInstanceRoot = getServerEnvironment().getInstanceRoot().getPath();
        String mqInstanceDir =  asInstanceRoot + java.io.File.separator
                                                 + MQ_DIR_NAME;
        return mqInstanceDir;
    }

    private String getBrokerLibDir() {
        String brokerLibDir = java.lang.System.getProperty(SystemPropertyConstants.IMQ_LIB_PROPERTY);
        if (_logger.isLoggable(Level.FINE))
            logFine("broker lib dir from system property " + brokerLibDir);
        return brokerLibDir;
    }

    private String getBrokerHomeDir() {
        // If the property was not specified, then look for the
        // imqRoot as defined by the com.sun.aas.imqRoot property
        String brokerHomeDir = java.lang.System.getProperty(SystemPropertyConstants.IMQ_BIN_PROPERTY);
        if (_logger.isLoggable(Level.FINE))
            logFine("broker home dir from system property " + brokerHomeDir);

        // Finally if all else fails (though this should never happen)
        // look for IMQ relative to the installation directory
        //@todo reget brokerHomeDir
        if (brokerHomeDir == null) {
            String IMQ_INSTALL_SUBDIR = java.io.File.separator +
                ".." + java.io.File.separator + ".." +
                java.io.File.separator + "imq" ;
                //java.io.File.separator + "bin"; hack until MQ RA changes
            //XXX: This doesn't work in clustered instances.
            brokerHomeDir = getServerEnvironment().getInstanceRoot() + IMQ_INSTALL_SUBDIR;
        } else {
            //hack until MQ RA changes
            brokerHomeDir = brokerHomeDir + java.io.File.separator + ".." ;
        }

        if (_logger.isLoggable(Level.FINE)) {
            logFine("Broker Home Directory :: " + brokerHomeDir);
            logFine("broker home dir finally" + brokerHomeDir);
        }
        return brokerHomeDir;

    }

    /**
     * Sets the SE/EE specific MQ-RA bean properties
     * @throws ConnectorRuntimeException
     */
    private void setAppClientRABeanProperties() throws ConnectorRuntimeException {
        if (_logger.isLoggable(Level.FINE))
            logFine("In Appclient container!!!");
        ConnectorDescriptor cd = super.getDescriptor();

        // if the CONNECTION_URL is localhost, in JMSRA,
        // use the ORB hostname so multi-homed connections work properly
        ORBLocator orbHelper = habitat.getService(ORBLocator.class);
        if(orbHelper != null && orbHelper.getORB() != null) {
            String jmsHost = null;
            String orbHost = orbHelper.getORBHost(orbHelper.getORB());
            String jmsPort = "7676";
            Set<?> props = cd.getConfigProperties();
            for (Object prop_ : props) {
                if (prop_ instanceof ConnectorConfigProperty prop) {
                    if (prop.getName().equals(CONNECTION_URL)) {
                        try {
                            URI url = new URI(prop.getValue());
                            jmsPort = Integer.toString(url.getPort());
                            if("localhost".equalsIgnoreCase(url.getHost())) {
                                jmsHost = orbHost;
                            }
                        } catch (URISyntaxException ex) {
                            _logger.fine(String.format("Invalid Connection URL: %s", prop.getValue()));
                        }
                    }
                }
            }
            if(jmsHost != null) {
                setProperty(cd, new ConnectorConfigProperty(CONNECTION_URL, String.format("mq://%s:%s", orbHost, jmsPort),
                        "ORB Address List", "java.lang.String"));
            }
        }
        ConnectorConfigProperty  envProp1 = new ConnectorConfigProperty  (
                        BROKERTYPE, REMOTE, "Broker Type", "java.lang.String");
                setProperty(cd, envProp1);

        ConnectorConfigProperty  envProp2 = new ConnectorConfigProperty  (
            GROUPNAME, "", "Group Name", "java.lang.String");
        cd.removeConfigProperty(envProp2);
        ConnectorConfigProperty  envProp3 = new ConnectorConfigProperty  (
            CLUSTERCONTAINER, "false", "Cluster flag", "java.lang.Boolean");
        setProperty(cd, envProp3);
    }

    //All Names passed into MQ needs to be valid Java Identifiers
    //so as of now replacing all characters that are not valid
    //java identifier components with '_'
    private static String convertStringToValidMQIdentifier(String s) {
        if (s == null) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetterOrDigit(s.charAt(i))) {
                buf.append(s.charAt(i));
            }
        }
        return buf.toString();
    }

    protected JmsHost getJmsHost() {
        String defaultJmsHost = getJmsService().getDefaultJmsHost();
        if (defaultJmsHost == null || defaultJmsHost.equals("")) {
            return Globals.get(JmsHost.class);
        }

        List jmsHostsList = getJmsService().getJmsHost();
        if (jmsHostsList == null || jmsHostsList.size() == 0) {
            return Globals.get(JmsHost.class);
        }

        JmsHost jmsHost = null;
        for (int i=0; i < jmsHostsList.size(); i ++) {
            JmsHost tmpJmsHost = (JmsHost) jmsHostsList.get(i);
            if (tmpJmsHost != null && tmpJmsHost.getName().equals(defaultJmsHost)) {
                jmsHost = tmpJmsHost;
                break;
            }
        }
        if (jmsHost == null) {
            jmsHost = (JmsHost) jmsHostsList.get(0);
        }
        return jmsHost;
    }

    /**
     * Whether JMS should bind to a port
     * @return false in embedded mode, true otherwise
     */
    public boolean getDoBind() {
        return doBind;
    }

    private void setMdbContainerProperties() throws ConnectorRuntimeException {
        JmsRaUtil raUtil = new JmsRaUtil(null);

        ConnectorDescriptor cd = super.getDescriptor();
        raUtil.setMdbContainerProperties();

        String val = "" + MdbContainerProps.getReconnectEnabled();
        ConnectorConfigProperty envProp2 = new ConnectorConfigProperty(
                RECONNECTENABLED, val, val, "java.lang.Boolean");
        setProperty(cd, envProp2);

        val = "" + MdbContainerProps.getReconnectDelay();
        ConnectorConfigProperty envProp3 = new ConnectorConfigProperty(
                RECONNECTINTERVAL, val, val, "java.lang.Integer");
        setProperty(cd, envProp3);

        val = "" + MdbContainerProps.getReconnectMaxRetries();
        ConnectorConfigProperty envProp4 = new ConnectorConfigProperty(
                RECONNECTATTEMPTS, val, val, "java.lang.Integer");
        setProperty(cd, envProp4);

        String integrationMode = getJmsService().getType();
        boolean lazyInit = Boolean.valueOf(getJmsHost().getLazyInit());
        val = "true";
        if (EMBEDDED.equals(integrationMode) && lazyInit) {
            val = "false";
        }
        doBind = Boolean.valueOf(val);
        ConnectorConfigProperty envProp5 = new ConnectorConfigProperty(
                MQ_PORTMAPPER_BIND, val, val, "java.lang.Boolean");
        setProperty(cd, envProp5);

        // The above properties will be set in ConnectorDescriptor and
        // will be bound in JNDI. This will be available to appclient
        // and standalone client.
    }

    //This is a MQ workaround. In PE, when the broker type is
    //EMBEDDED or LOCAL, do not set the addresslist, else
    //MQ RA assumes that there are two URLs and fails (EE limitation).
    private void setConnectionURL(MQAddressList urlList) {
        ConnectorDescriptor cd = super.getDescriptor();
        String val = urlList.toString();

        if(_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, JMSLoggerInfo.JMS_CONNECTION_URL,
                    new Object[]{val});
        }

        ConnectorConfigProperty envProp1 = new ConnectorConfigProperty(CONNECTION_URL, val, val, "java.lang.String");
        setProperty(cd, envProp1);
    }

    private void setJmsServiceProperties(JmsService service) throws ConnectorRuntimeException {
        JmsRaUtil jmsraUtil = new JmsRaUtil(service);
        jmsraUtil.setupAddressList();
        urlList = jmsraUtil.getUrlList();
        addressList = urlList.toString();

        if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, JMSLoggerInfo.ADDRESSLIST_JMSPROVIDER,
                    new Object[]{addressList});
        }

        ConnectorDescriptor cd = super.getDescriptor();
        setConnectionURL(urlList);

        String val = "" + jmsraUtil.getReconnectEnabled();
        ConnectorConfigProperty envProp2 = new ConnectorConfigProperty(
                RECONNECTENABLED, val, val, "java.lang.Boolean");
        setProperty(cd, envProp2);

        //convert to milliseconds
        int newval = Integer.parseInt(jmsraUtil.getReconnectInterval()) * 1000;
        val = "" + newval;
        ConnectorConfigProperty envProp3 = new ConnectorConfigProperty(
                RECONNECTINTERVAL, val, val, "java.lang.Integer");
        setProperty(cd, envProp3);

        val = jmsraUtil.getReconnectAttempts();
        ConnectorConfigProperty envProp4 = new ConnectorConfigProperty(
                RECONNECTATTEMPTS, val, val, "java.lang.Integer");
        setProperty(cd, envProp4);

        val = jmsraUtil.getAddressListBehaviour();
        ConnectorConfigProperty envProp5 = new ConnectorConfigProperty(
                ADRLIST_BEHAVIOUR, val, val, "java.lang.String");
        setProperty(cd, envProp5);

        val = jmsraUtil.getAddressListIterations();
        ConnectorConfigProperty envProp6 = new ConnectorConfigProperty(
                ADRLIST_ITERATIONS, val, val, "java.lang.Integer");
        setProperty(cd, envProp6);

        boolean useExternal = shouldUseExternalRmiRegistry(jmsraUtil);
        val = Boolean.valueOf(useExternal).toString();
        ConnectorConfigProperty envProp7 = new ConnectorConfigProperty(
                USEEXTERNALRMIREGISTRY, val, val, "java.lang.Boolean");
        setProperty(cd, envProp7);

        _logger.log(Level.FINE, "Start RMI registry set as " + val);
        //If MQ RA needs to use AS RMI Registry Port, then set
        //the RMI registry port, else MQ RA uses its default RMI
        //Registry port  [as of now 1099]
        String configuredRmiRegistryPort = null;
        if (!useExternal) {
            configuredRmiRegistryPort = getRmiRegistryPort();
        } else {
            /* We will be here if we are LOCAL or REMOTE, standalone
             * or clustered. We could set the Rmi registry port.
             * The RA should ignore the port if REMOTE and use it only
             * for LOCAL cases.
             */
            configuredRmiRegistryPort = getUniqueRmiRegistryPort();
        }
        val = configuredRmiRegistryPort;
        if (val != null) {
            ConnectorConfigProperty envProp8 = new ConnectorConfigProperty(
                    RMIREGISTRYPORT, val, val, "java.lang.Integer");
            setProperty(cd, envProp8);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "RMI registry port set as " + val);
            }
        } else if (_logger.isLoggable(Level.WARNING)) {
            _logger.log(Level.WARNING, JMSLoggerInfo.INVALID_RMI_PORT);
        }
    }

    /*
     * Checks if AS RMI registry is started and available for use.
     */
    private boolean shouldUseExternalRmiRegistry (JmsRaUtil jmsraUtil) {
        boolean useExternalRmiRegistry = !isASRmiRegistryPortAvailable(jmsraUtil);
        return useExternalRmiRegistry;
    }

    /**
     * This method should return a unique and unused port , so that
     * the broker can use this to start its Rmi registry.
     * Used only for LOCAL mode
     */
    private String getUniqueRmiRegistryPort() {
        int mqrmiport = DEFAULTRMIREGISTRYPORT;
        try {
            String configuredport = System.getProperty(BROKER_RMI_PORT);
            if (configuredport != null) {
                mqrmiport = Integer.parseInt(configuredport);
            } else {
                mqrmiport = Integer.parseInt(brkrPort)
                        + BROKERRMIPORTOFFSET;
            }
        } catch (Exception e) {
        }
        return Integer.toString(mqrmiport);
    }

    /**
     * Get the AS RMI registry port for MQ RA to use.
     */
    private String getRmiRegistryPort() {
        String val = null;
        if (MQRmiPort != null && !MQRmiPort.trim().equals("")) {
            return MQRmiPort;
        } else {
            String configuredPort = null;
            try {
                configuredPort = getConfiguredRmiRegistryPort();
            } catch (Exception ex) {
                if (_logger.isLoggable(Level.WARNING)) {
                    _logger.log(Level.WARNING, JMSLoggerInfo.GET_RMIPORT_FAIL,
                            new Object[]{ex.getLocalizedMessage()});
                }
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Exception while getting configured rmi registry port", ex);
                }
            }
            if (configuredPort != null) {
                return configuredPort;
            }

            //Finally if DAS and configured port doesn't work, return DAS'
            //RMI registry port as a fallback option.

            if (isDAS()) {
                return DASRMIPORT;
            }
        }
        return val;
    }

    private boolean isDAS() {
        return SystemPropertyConstants.DAS_SERVER_NAME.equals(getServerContext().getInstanceName());
    }

    private String getConfiguredRmiRegistryHost() throws Exception {
        String hostName = getJmxConnector().getAddress();
        if (hostName.equals("") || hostName.equals("0.0.0.0")) {
            try {
                hostName = java.net.InetAddress.getLocalHost().getCanonicalHostName();
            } catch (java.net.UnknownHostException e) {
                hostName = "localhost";
            }
        } else if (hostName.contains(":") && !hostName.startsWith("[")) {
            return "["+hostName+"]";
        }
        return hostName;
    }

    private String getConfiguredRmiRegistryPort() throws Exception {
        return getJmxConnector().getPort();
    }

    private JmxConnector getJmxConnector() {
        List jmxConnectors = getAdminService().getJmxConnector();
        String sysJmsConnectorName = getAdminService().getSystemJmxConnectorName();
        if (jmxConnectors != null) {
            for (int i = 0; i < jmxConnectors.size(); i++) {
                if (sysJmsConnectorName.equals(((JmxConnector) jmxConnectors.get(i)).getName())) {
                    return (JmxConnector) jmxConnectors.get(i);
                }
            }
        }
        return null;
    }

    private boolean isASRmiRegistryPortAvailable(JmsRaUtil jmsraUtil) {
        if (_logger.isLoggable(Level.FINE)) {
            logFine("isASRmiRegistryPortAvailable - JMSService Type:" + jmsraUtil.getJMSServiceType());
        }
        //If JMSServiceType is LOCAL or REMOTE, then we need not ask the MQ RA to use the
        //AS RMI Registry. So the check below is not necessary.
        if (jmsraUtil.getJMSServiceType().equals(REMOTE) || jmsraUtil.getJMSServiceType().equals(LOCAL)) {
            return false;
        }

        try {
            JmxConnector jmxConnector = getJmxConnector();
            if (!"true".equals(jmxConnector.getEnabled()))
                return false;

            if ("true".equals(jmxConnector.getSecurityEnabled()))
                return false;

            // Attempt to detect JMXStartupService for RMI registry
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Detecting JMXStartupService...");
            }
            JMXStartupService jmxservice = Globals.get(JMXStartupService.class);
            if (jmxservice == null)
                return false;

            jmxservice.waitUntilJMXConnectorStarted();

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Found JMXStartupService");
            }

            String name = "rmi://" + getConfiguredRmiRegistryHost() + ":" + getConfiguredRmiRegistryPort() + "/jmxrmi";
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Attempting to list " + name);
            }
            Naming.list(name);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("List on " + name + " succeeded");
            }

            //return configured port only if RMI registry is available
            return true;
        } catch (Exception e) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Failed to detect JMX RMI Registry: " + e.getMessage());
            }
            return false;
        }
    }

    private void setProperty(ConnectorDescriptor cd, ConnectorConfigProperty  envProp){
        cd.removeConfigProperty(envProp);
        cd.addConfigProperty(envProp);
    }

    /**
     * This is a temporay solution for obtaining all the MCFs
     * corresponding to a JMS RA pool, this is to facilitate the
     * recovery process where the XA resources of all RMs in the
     * broker cluster are required. Should be removed when a permanent
     * solutuion is available from the broker.
     * @param cpr <code>ConnectorConnectionPool</code> object
     * @param loader Class Loader.
     * @return
     */
    @Override
    public ManagedConnectionFactory[] createManagedConnectionFactories(
            com.sun.enterprise.connectors.ConnectorConnectionPool cpr, ClassLoader loader) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "RECOVERY : Entering createMCFS in AJMSRA");
        }
        ArrayList mcfs = new ArrayList();
        if (getAddressListCount() < 2) {
            mcfs.add(createManagedConnectionFactory(cpr, loader));
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Brokers are not clustered,So doing normal recovery");
            }
        } else {
            String addlist = null;
            Set s = cpr.getConnectorDescriptorInfo().getMCFConfigProperties();
            Iterator tmpit = s.iterator();
            while (tmpit.hasNext()) {
                ConnectorConfigProperty prop = (ConnectorConfigProperty) tmpit.next();
                String propName = prop.getName();
                if (propName.equalsIgnoreCase("imqAddressList") || propName.equalsIgnoreCase("Addresslist")) {
                    addlist = prop.getValue();
                }
            }
            StringTokenizer tokenizer;
            if ((addlist == null) || (addlist.trim().equalsIgnoreCase("localhost"))) {
                tokenizer = new StringTokenizer(addressList, ",");
            } else {
                tokenizer = new StringTokenizer(addlist, ",");
            }
            _logger.log(Level.FINE, "No of addresses found " + tokenizer.countTokens());
            while (tokenizer.hasMoreTokens()) {
                String brokerurl = tokenizer.nextToken();
                ManagedConnectionFactory mcf = super.createManagedConnectionFactory(cpr, loader);
                Iterator it = s.iterator();
                while (it.hasNext()) {
                    ConnectorConfigProperty prop = (ConnectorConfigProperty) it.next();
                    String propName = prop.getName();
                    String propValue = prop.getValue();
                    if (propName.startsWith("imq") && !"".equals(propValue)) {
                        try {
                            Method meth = mcf.getClass().getMethod
                                    (SETTER, String.class,
                                            String.class);
                            if (propName.trim().equalsIgnoreCase("imqAddressList")) {
                                meth.invoke(mcf, prop.getName(), brokerurl);
                            } else {
                                meth.invoke(mcf, prop.getName(), prop.getValueObject());
                            }
                        } catch (NoSuchMethodException ex) {
                            if (_logger.isLoggable(Level.WARNING)) {
                                _logger.log(Level.WARNING, JMSLoggerInfo.NO_SUCH_METHOD,
                                        new Object[]{SETTER, mcf.getClass().getName()});
                            }
                        } catch (Exception ex) {
                            LogHelper.log(_logger, Level.SEVERE, JMSLoggerInfo.ERROR_EXECUTE_METHOD,
                                    ex, SETTER, mcf.getClass().getName());
                        }
                    }
                }
                ConnectorConfigProperty addressProp3 = new ConnectorConfigProperty(ADDRESSLIST, brokerurl, "Address List",
                        "java.lang.String");
                //todo: need to remove log statement
                if (_logger.isLoggable(Level.INFO)) {
                    _logger.log(Level.INFO, JMSLoggerInfo.ADDRESSLIST,
                            new Object[]{brokerurl});
                }

                HashSet addressProp = new HashSet();
                addressProp.add(addressProp3);
                SetMethodAction setMethodAction = new SetMethodAction(mcf, addressProp);
                try {
                    setMethodAction.run();
                } catch (Exception e) {
                }
                mcfs.add(mcf);
            }
        }
        return (ManagedConnectionFactory[]) mcfs.toArray(new ManagedConnectionFactory[mcfs.size()]);
    }

    @Override
    protected ManagedConnectionFactory instantiateMCF(final String mcfClass, final ClassLoader loader) throws Exception {
        ManagedConnectionFactory mcf = null;
        if (moduleName_.equals(ConnectorRuntime.DEFAULT_JMS_ADAPTER)) {
            Object tmp = AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        @Override
                        public Object run() throws Exception {
                            return instantiateManagedConnectionFactory(mcfClass, loader);
                        }
                    });
            mcf = (ManagedConnectionFactory) tmp;
        }
        return mcf;
    }

    private ManagedConnectionFactory instantiateManagedConnectionFactory (final String mcfClass, final ClassLoader loader) throws Exception {
        return  super.instantiateMCF(mcfClass, loader);
    }
    /**
     * Creates ManagedConnection Factory instance. For any property that is
     * for supporting AS7 imq properties, resource adapter has a set method
     * setProperty(String,String). All as7 properties starts with "imq".
     * MQ Adapter supports this only for backward compatibility.
     *
     * @param cpr <code>ConnectorConnectionPool</code> object
     * @param loader Class Loader.
     * @return
     */
    @Override
    public ManagedConnectionFactory createManagedConnectionFactory(com.sun.enterprise.connectors.ConnectorConnectionPool cpr, ClassLoader loader) {
        ManagedConnectionFactory mcf = super.createManagedConnectionFactory(cpr, loader);
        if (mcf != null) {
            Set s = cpr.getConnectorDescriptorInfo().getMCFConfigProperties();
            Iterator it = s.iterator();
            while (it.hasNext()) {
                ConnectorConfigProperty prop = (ConnectorConfigProperty) it.next();
                String propName = prop.getName();

                // If the property has started with imq, then it should go to
                // setProperty(String,String) method.
                if (propName.startsWith("imq") && !"".equals(prop.getValue())) {
                    try {
                        Method meth = mcf.getClass().getMethod(SETTER, String.class, String.class);
                        meth.invoke(mcf, prop.getName(), prop.getValueObject());
                    } catch (NoSuchMethodException ex) {
                        if (_logger.isLoggable(Level.WARNING)) {
                            _logger.log(Level.WARNING, JMSLoggerInfo.NO_SUCH_METHOD, new Object[]{SETTER, mcf.getClass().getName()});
                        }
                    } catch (Exception ex) {
                        LogHelper.log(_logger, Level.SEVERE, JMSLoggerInfo.ERROR_EXECUTE_METHOD, ex, SETTER, mcf.getClass().getName());
                    }
                }
            }

            //CR 6591307- Fix for properties getting overridden when setRA is called. Resetting the  properties if the RA is the JMS RA
            String moduleName = this.getModuleName();
            if (ConnectorAdminServiceUtils.isJMSRA(moduleName)) {
                try {
                    Set configProperties = cpr.getConnectorDescriptorInfo().getMCFConfigProperties();
                    Object[] array = configProperties.toArray();
                    for (int i = 0; i < array.length; i++) {
                        if (array[i] instanceof ConnectorConfigProperty property) {
                            if (ActiveJmsResourceAdapter.ADDRESSLIST.equals(property.getName())) {
                                if (property.getValue() == null || "".equals(property.getValue()) || "localhost".equals(property.getValue())) {
                                    _logger.log(Level.FINE, "raraddresslist.default.value : " + property.getValue());
                                    configProperties.remove(property);
                                }
                            }
                        }
                    }
                    SetMethodAction setMethodAction = new SetMethodAction(mcf, configProperties);
                    setMethodAction.run();
                } catch (Exception Ex) {
                    final String mcfClass = cpr.getConnectorDescriptorInfo().getManagedConnectionFactoryClass();
                    if (_logger.isLoggable(Level.WARNING)) {
                        _logger.log(Level.WARNING, JMSLoggerInfo.RARDEPLOYMENT_MCF_ERROR, new Object[]{mcfClass, Ex.getMessage()});
                    }
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "rardeployment.mcfcreation_error", Ex);
                    }
                }
            }
        }
        return mcf;
    }

    /**
     * This is the most appropriate time (??) to update the runtime
     * info of a 1.3 MDB into 1.4 MDB.  <p>
     *
     * Assumptions : <p>
     * 0. Assume it is a 1.3 MDB if no RA mid is specified.
     * 1. Use the default system JMS resource adapter. <p>
     * 2. The ActivationSpec of the default JMS RA will provide the
     *    setDestination, setDestinationType, setSubscriptionName methods.
     * 3. The jndi-name of the 1.3 MDB is the value for the Destination
     *    property for the ActivationSpec.
     * 4. The ActivationSpec provides setter methods for the properties
     *    defined in the CF that corresponds to the mdb-connection-factory
     *    JNDI name.
     *
     * @param descriptor_
     * @param poolDescriptor
     * @throws com.sun.appserv.connectors.internal.api.ConnectorRuntimeException
     */
    @Override
    public void updateMDBRuntimeInfo(EjbMessageBeanDescriptor descriptor_, BeanPoolDescriptor poolDescriptor) throws ConnectorRuntimeException {
        String jndiName = descriptor_.getJndiName();
        if (jndiName == null || "".equals(jndiName)) {
            MessageDestinationDescriptor destDescriptor = descriptor_.getMessageDestination();
            if (destDescriptor != null) {
                jndiName = destDescriptor.getJndiName();
            }
        }

        String destinationLookup = descriptor_.getActivationConfigValue("destinationLookup");
        String destinationProp = descriptor_.getActivationConfigValue("destination");

        if (destinationLookup == null && destinationProp == null && (jndiName == null || "".equals(jndiName))) {
            if (_logger.isLoggable(Level.SEVERE)) {
                _logger.log(Level.SEVERE, JMSLoggerInfo.ERROR_IN_DD);
            }
            String msg = sm.getString("ajra.error_in_dd");
            throw new ConnectorRuntimeException(msg);
        }

        String resourceAdapterMid = ConnectorRuntime.DEFAULT_JMS_ADAPTER;

        descriptor_.setResourceAdapterMid(resourceAdapterMid);

        if (destinationLookup == null && destinationProp == null) {
            String appName = descriptor_.getApplication().getAppName();
            String moduleName = ConnectorsUtil.getModuleName(descriptor_);

            JMSDestinationDefinitionDescriptor destination = getJMSDestinationFromDescriptor(jndiName, descriptor_);
            String destName;
            if (isValidDestination(destination)) {
                destName = destination.getDestinationName();
            } else {
                destName = getPhysicalDestinationFromConfiguration(jndiName, appName, moduleName);
            }

            //1.3 jndi-name ==> 1.4 setDestination
            descriptor_.putRuntimeActivationConfigProperty(new EnvironmentProperty(DESTINATION, destName, null));

            //1.3 (standard) destination-type == 1.4 setDestinationType
            //XXX Do we really need this???
            if (descriptor_.getDestinationType() != null && !"".equals(descriptor_.getDestinationType())) {
                descriptor_.putRuntimeActivationConfigProperty(new EnvironmentProperty(DESTINATION_TYPE, descriptor_.getDestinationType(), null));
                if (_logger.isLoggable(Level.INFO)) {
                    _logger.log(Level.INFO, JMSLoggerInfo.ENDPOINT_DEST_NAME,
                            new Object[]{descriptor_.getDestinationType(), jndiName, descriptor_.getName()});
                }
            } else if (isValidDestination(destination) && ConnectorConstants.DEFAULT_JMS_ADAPTER.equals(destination.getResourceAdapter())) {
                descriptor_.putRuntimeActivationConfigProperty(new EnvironmentProperty(DESTINATION_TYPE, destination.getInterfaceName(), null));
                if (_logger.isLoggable(Level.INFO)) {
                    _logger.log(Level.INFO, JMSLoggerInfo.ENDPOINT_DEST_NAME,
                            new Object[]{destination.getInterfaceName(), destination.getName(), descriptor_.getName()});
                }
            } else {
                /*
                 * If destination type is not provided by the MDB component
                 * [typically used by EJB3.0 styled MDBs which create MDBs without
                 * a destination type activation-config property] and the MDB is for
                 * the default JMS RA, attempt to infer the destination type by trying
                 * to find out if there has been any JMS destination resource already
                 * defined for default JMS RA. This is a best attempt guess and if there
                 * are no JMS destination resources/admin-objects defined, AS would pass
                 * the properties as defined by the MDB.
                 */
                try {
                    AdminObjectResource aor = (AdminObjectResource)
                            ResourcesUtil.createInstance().getResource(jndiName, appName, moduleName, AdminObjectResource.class);
                    if (aor != null && ConnectorConstants.DEFAULT_JMS_ADAPTER.equals(aor.getResAdapter())) {
                        descriptor_.putRuntimeActivationConfigProperty(
                                new EnvironmentProperty(DESTINATION_TYPE,
                                        aor.getResType(), null));
                        if (_logger.isLoggable(Level.INFO)) {
                            _logger.log(Level.INFO, JMSLoggerInfo.ENDPOINT_DEST_NAME,
                                    new Object[]{aor.getResType(), aor.getJndiName(), descriptor_.getName()});
                        }
                    }

                } catch (Exception e) {

                }
            }
        }


        //1.3 durable-subscription-name == 1.4 setSubscriptionName
        descriptor_.putRuntimeActivationConfigProperty(new EnvironmentProperty(SUBSCRIPTION_NAME, descriptor_.getDurableSubscriptionName(), null));

        String mdbCF = null;
        try {
            mdbCF = descriptor_.getMdbConnectionFactoryJndiName();
        } catch (NullPointerException ne) {
            // Dont process connection factory.
        }

        if (mdbCF != null && !"".equals(mdbCF)) {
            setValuesFromConfiguration(mdbCF, descriptor_);
        }

        // a null object is passes as a PoolDescriptor during recovery.
        // See com/sun/enterprise/resource/ResourceInstaller

        if (poolDescriptor != null) {
            descriptor_.putRuntimeActivationConfigProperty(new EnvironmentProperty(MAXPOOLSIZE, "" +
                    poolDescriptor.getMaxPoolSize(), "", "java.lang.Integer"));
            descriptor_.putRuntimeActivationConfigProperty(new EnvironmentProperty(MINPOOLSIZE, "" +
                    poolDescriptor.getSteadyPoolSize(), "", "java.lang.Integer"));
            descriptor_.putRuntimeActivationConfigProperty(new EnvironmentProperty(RESIZECOUNT, "" +
                    poolDescriptor.getPoolResizeQuantity(), "", "java.lang.Integer"));
            descriptor_.putRuntimeActivationConfigProperty(new EnvironmentProperty(RESIZETIMEOUT, "" +
                    poolDescriptor.getPoolIdleTimeoutInSeconds(), "", "java.lang.Integer"));
            /**
             * The runtime activation config property holds all the
             * vendor specific properties, unfortunately the vendor
             * specific way of configuring exception count and the
             * standard way of configuring redelivery attempts is
             * through the same property REDELIVERYCOUNT . So, we first
             * check if the user (MDB assember) has configured a value
             * if not we set the one from mdb-container props
             * We have to check for both cases here because it has been
             * documented as "endpointExceptionRedeliveryAttempts" but
             * used in the code as "EndpointExceptionRedeliveryAttempts"
             */
            if ((descriptor_.getActivationConfigValue(REDELIVERYCOUNT) == null)
                    && (descriptor_.getActivationConfigValue(LOWERCASE_REDELIVERYCOUNT) == null)) {
                descriptor_.putRuntimeActivationConfigProperty(new EnvironmentProperty(REDELIVERYCOUNT, "" +
                        MdbContainerProps.getMaxRuntimeExceptions(), "", "java.lang.Integer"));
            }
        }
    }

    private String getPhysicalDestinationFromConfiguration(String logicalDest, String appName, String moduleName)
            throws ConnectorRuntimeException {
        Property ep;
        try {
            //ServerContext sc = ApplicationServer.getServerContext();
            //ConfigContext ctx = sc.getConfigContext();
            //Resources rbeans =                           ServerBeansFactory.getDomainBean(ctx).getResources();
            AdminObjectResource res = null;
            res = (AdminObjectResource)
                    ResourcesUtil.createInstance().getResource(logicalDest, appName, moduleName, AdminObjectResource.class);
            //AdminObjectResource res = (AdminObjectResource)   allResources.getAdminObjectResourceByJndiName(logicalDest);
            if (res == null) {
                String msg = sm.getString("ajra.err_getting_dest", logicalDest);
                throw new ConnectorRuntimeException(msg);
            }

            ep = res.getProperty(PHYSICAL_DESTINATION); //getElementPropertyByName(PHYSICAL_DESTINATION);
        } catch (Exception ce) {
            String msg = sm.getString("ajra.err_getting_dest", logicalDest);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(msg, ce);
            throw cre;
        }

        if (ep == null) {
            String msg = sm.getString("ajra.cannot_find_phy_dest", null);
            throw new ConnectorRuntimeException(msg);
        }

        return ep.getValue();
    }

    private JMSDestinationDefinitionDescriptor getJMSDestinationFromDescriptor(String jndiName, EjbMessageBeanDescriptor ejbMessageBeanDescriptor) {
        JMSDestinationDefinitionDescriptor destination = null;
        if (jndiName.startsWith(ResourceConstants.JAVA_COMP_SCOPE_PREFIX)
                || !jndiName.startsWith(ResourceConstants.JAVA_SCOPE_PREFIX)) {
            if (isEjbInWar(ejbMessageBeanDescriptor)) {
                destination = getJMSDestination(jndiName, ejbMessageBeanDescriptor.getEjbBundleDescriptor().getModuleDescriptor());
            } else {
                destination = getJMSDestination(jndiName, ejbMessageBeanDescriptor);
            }
        } else if (jndiName.startsWith(ResourceConstants.JAVA_MODULE_SCOPE_PREFIX)) {
            if (isEjbInWar(ejbMessageBeanDescriptor)) {
                destination = getJMSDestination(jndiName, ejbMessageBeanDescriptor.getEjbBundleDescriptor().getModuleDescriptor());
            } else {
                destination = getJMSDestination(jndiName, ejbMessageBeanDescriptor.getEjbBundleDescriptor());
            }
        } else if (jndiName.startsWith(ResourceConstants.JAVA_APP_SCOPE_PREFIX)) {
            destination = getJMSDestination(jndiName, ejbMessageBeanDescriptor.getApplication());
        } else if (jndiName.startsWith(ResourceConstants.JAVA_GLOBAL_SCOPE_PREFIX)) {
            destination = getJMSDestination(jndiName, ejbMessageBeanDescriptor.getApplication());
            if (!isValidDestination(destination)) {
                destination = getJMSDestination(jndiName);
            }
        }
        if (isValidDestination(destination)) {
            return destination;
        }
        return null;
    }

    private boolean isValidDestination(JMSDestinationDefinitionDescriptor descriptor) {
        return (descriptor != null) && (descriptor.getName() != null) && !"".equals(descriptor.getName());
    }

    private boolean isEjbInWar(EjbBundleDescriptor ejbBundleDescriptor) {
        Object rootDeploymentDescriptor = ejbBundleDescriptor.getModuleDescriptor().getDescriptor();
        return (rootDeploymentDescriptor != ejbBundleDescriptor) && (rootDeploymentDescriptor instanceof WebBundleDescriptor);
    }

    private boolean isEjbInWar(EjbMessageBeanDescriptor ejbMessageBeanDescriptor) {
        return isEjbInWar(ejbMessageBeanDescriptor.getEjbBundleDescriptor());
    }

    /*
     * Get JMS destination resource from component
     */
    private JMSDestinationDefinitionDescriptor getJMSDestination(String logicalDestination, EjbMessageBeanDescriptor ejbMessageBeanDescriptor) {
        return getJMSDestination(logicalDestination, ejbMessageBeanDescriptor.getResourceDescriptors(JavaEEResourceType.JMSDD));
    }

    /*
     * Get JMS destination resource from ejb module
     */
    private JMSDestinationDefinitionDescriptor getJMSDestination(String logicalDestination, EjbBundleDescriptor ejbBundleDescriptor) {
        JMSDestinationDefinitionDescriptor destination =
                getJMSDestination(logicalDestination, ejbBundleDescriptor.getResourceDescriptors(JavaEEResourceType.JMSDD));
        if (isValidDestination(destination)) {
            return destination;
        }

        Set<EjbDescriptor> ejbDescriptors = (Set<EjbDescriptor>) ejbBundleDescriptor.getEjbs();
        for (EjbDescriptor ejbDescriptor : ejbDescriptors) {
            destination = getJMSDestination(logicalDestination, ejbDescriptor.getResourceDescriptors(JavaEEResourceType.JMSDD));
            if (isValidDestination(destination)) {
                return destination;
            }
        }

        return null;
    }

    /*
     * Get JMS destination resource from web module
     */
    private JMSDestinationDefinitionDescriptor getJMSDestination(String logicalDestination, ModuleDescriptor moduleDescriptor) {
        WebBundleDescriptor webBundleDescriptor = (WebBundleDescriptor) moduleDescriptor.getDescriptor();
        JMSDestinationDefinitionDescriptor destination =
                getJMSDestination(logicalDestination, webBundleDescriptor.getResourceDescriptors(JavaEEResourceType.JMSDD));
        if (isValidDestination(destination)) {
            return destination;
        }

        Collection<EjbBundleDescriptor> ejbBundleDescriptors = moduleDescriptor.getDescriptor().getExtensionsDescriptors(EjbBundleDescriptor.class);
        for (EjbBundleDescriptor ejbBundleDescriptor : ejbBundleDescriptors) {
            destination = getJMSDestination(logicalDestination, ejbBundleDescriptor);
            if (isValidDestination(destination)) {
                return destination;
            }
        }

        return null;
    }

    /*
     * Get JMS destination resource from application
     */
    private JMSDestinationDefinitionDescriptor getJMSDestination(String logicalDestination, Application application) {
        if (application == null) {
            return null;
        }

        JMSDestinationDefinitionDescriptor destination =
                getJMSDestination(logicalDestination, application.getResourceDescriptors(JavaEEResourceType.JMSDD));
        if (isValidDestination(destination)) {
            return destination;
        }

        Set<WebBundleDescriptor> webBundleDescriptors = application.getBundleDescriptors(WebBundleDescriptor.class);
        for (WebBundleDescriptor webBundleDescriptor : webBundleDescriptors) {
            destination = getJMSDestination(logicalDestination, webBundleDescriptor.getResourceDescriptors(JavaEEResourceType.JMSDD));
            if (isValidDestination(destination)) {
                return destination;
            }
        }

        Set<EjbBundleDescriptor> ejbBundleDescriptors = application.getBundleDescriptors(EjbBundleDescriptor.class);
        for (EjbBundleDescriptor ejbBundleDescriptor : ejbBundleDescriptors) {
            destination = getJMSDestination(logicalDestination, ejbBundleDescriptor);
            if (isValidDestination(destination)) {
                return destination;
            }
        }

        Set<ApplicationClientDescriptor> appClientDescriptors = application.getBundleDescriptors(ApplicationClientDescriptor.class);
        for (ApplicationClientDescriptor appClientDescriptor : appClientDescriptors) {
            destination = getJMSDestination(logicalDestination, appClientDescriptor.getResourceDescriptors(JavaEEResourceType.JMSDD));
            if (isValidDestination(destination)) {
                return destination;
            }
        }

        return null;
    }

    /*
     * Get JMS destination resource from deployed applications
     */
    private JMSDestinationDefinitionDescriptor getJMSDestination(String logicalDestination) {
        Domain domain = Globals.get(Domain.class);
        Applications applications = domain.getApplications();
        for (com.sun.enterprise.config.serverbeans.Application app : applications.getApplications()) {
            ApplicationInfo appInfo = appRegistry.get(app.getName());
            if (appInfo != null) {
                Application application = appInfo.getMetaData(Application.class);
                JMSDestinationDefinitionDescriptor destination = getJMSDestination(logicalDestination, application);
                if (isValidDestination(destination)) {
                    return destination;
                }
            }
        }
        return null;
    }

    /*
     * Get JMS destination resource from descriptor set
     */
    private JMSDestinationDefinitionDescriptor getJMSDestination(String jndiName, Set<? extends Descriptor> descriptors) {
        for (Descriptor descriptor : descriptors) {
            if (descriptor instanceof JMSDestinationDefinitionDescriptor) {
                if (jndiName.equals(descriptor.getName())) {
                    return (JMSDestinationDefinitionDescriptor) descriptor;
                }
            }
        }
        return null;
    }

    private void setValuesFromConfiguration(String cfName, EjbMessageBeanDescriptor descriptor_) throws ConnectorRuntimeException {
        List <Property> ep;
        try {
            String appName = descriptor_.getApplication().getAppName();
            String moduleName = ConnectorsUtil.getModuleName(descriptor_);
            ConnectorResource res = (ConnectorResource) ResourcesUtil.createInstance().getResource(cfName, appName, moduleName, ConnectorResource.class);
            if (res == null) {
                String msg = sm.getString("ajra.mdb_cf_not_created", cfName);
                throw new ConnectorRuntimeException(msg);
            }

            ConnectorConnectionPool ccp = (ConnectorConnectionPool) ResourcesUtil.createInstance().getResource(
                    res.getPoolName(), appName, moduleName, ConnectorConnectionPool.class);

            ep = ccp.getProperty();
        } catch (Exception ce) {
            String msg = sm.getString("ajra.mdb_cf_not_created", cfName);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(msg, ce);
            throw cre;
        }

        if (ep == null) {
            String msg = sm.getString("ajra.cannot_find_phy_dest");
            throw new ConnectorRuntimeException(msg);
        }

        for (int i = 0; i < ep.size(); i++) {
            Property prop = ep.get(i);
            String name = prop.getName();
            if (name.equals(MCFADDRESSLIST)) {
                name = ADDRESSLIST;
            }
            String val = prop.getValue();
            if (val == null || val.equals("")) {
                continue;
            }
            descriptor_.putRuntimeActivationConfigProperty(
                    new EnvironmentProperty(name, val, null));
        }
    }

    private static void logFine(String s) {
        if (_logger.isLoggable(Level.FINE)){
            _logger.fine(s);
        }
    }

    public int getAddressListCount() {
        StringTokenizer tokenizer;
        int count = 1;
        if (addressList != null) {
            tokenizer = new StringTokenizer(addressList, ",");
            count = tokenizer.countTokens();
        }
        if (_logger.isLoggable(Level.FINE)) {
            logFine("Address list count is " + count);
        }
        return count;
    }

    private ServerEnvironmentImpl getServerEnvironment(){
        return serverEnvironmentImplProvider.get();
    }

    private AdminService getAdminService() {
        return adminServiceProvider.get();
    }

    private JmsService getJmsService(){
        return habitat.getService(JmsService.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
    }

    private ServerContext getServerContext(){
        return serverContextProvider.get();
    }

    //methods from LazyServiceIntializer
    @Override
    public boolean initializeService() {
        try {
            String module = ConnectorConstants.DEFAULT_JMS_ADAPTER;
            String loc = ConnectorsUtil.getSystemModuleLocation(module);
            ConnectorRuntime connectorRuntime = connectorRuntimeProvider.get();
            connectorRuntime.createActiveResourceAdapter(loc, module, null);
            return true;
        } catch (ConnectorRuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void handleRequest(SelectableChannel selectableChannel) {
        SocketChannel socketChannel;
        if (selectableChannel instanceof SocketChannel) {
            socketChannel = (SocketChannel) selectableChannel;
            try {
                Class c = resourceadapter_.getClass();
                Method m = c.getMethod("getPortMapperClientHandler", null);
                Object handler = m.invoke(resourceadapter_, null);
                m = handler.getClass().getMethod("handleRequest", SocketChannel.class);
                m.invoke(handler, socketChannel);
            } catch (Exception ex) {
                String message = sm.getString("error.invoke.portmapper", ex.getLocalizedMessage());
                throw new RuntimeException(message, ex);
            }
        } else {
            throw new IllegalArgumentException(sm.getString("invalid.socket.channel"));
        }
    }

    protected void setClusterBrokerList(String brokerList) {
        try {
            Class c = resourceadapter_.getClass();
            Method m = c.getMethod("setClusterBrokerList", String.class);
            m.invoke(resourceadapter_, brokerList);
            if (_logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, JMSLoggerInfo.CLUSTER_BROKER_SUCCESS, new Object[]{brokerList});
            }
        } catch (Exception ex) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, JMSLoggerInfo.CLUSTER_BROKER_FAILURE,
                        new Object[]{brokerList, ex.getMessage()});
            }
        }
    }
}
