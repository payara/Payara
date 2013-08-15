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

package com.sun.enterprise.connectors.jms.system;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.rmi.Naming;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapterInternalException;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.AvailabilityService;
import com.sun.enterprise.config.serverbeans.Clusters;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.JmxConnector;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.connectors.inbound.ActiveInboundResourceAdapterImpl;
import com.sun.enterprise.connectors.jms.config.JmsAvailability;
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
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.v3.services.impl.DummyNetworkListener;
import com.sun.enterprise.v3.services.impl.GrizzlyService;
import com.sun.logging.LogDomains;
import org.glassfish.admin.mbeanserver.JMXStartupService;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.connectors.config.AdminObjectResource;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.ConnectorResource;
import org.glassfish.connectors.config.ResourceAdapterConfig;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.grizzly.LazyServiceInitializer;
import org.glassfish.resourcebase.resources.api.ResourceConstants;
import org.glassfish.server.ServerEnvironmentImpl;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Singleton;
import org.jvnet.hk2.config.types.Property;

//import com.sun.messaging.jmq.util.service.PortMapperClientHandler;


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

    static Logger _logger = LogDomains.getLogger(ActiveJmsResourceAdapter.class,  LogDomains.JMS_LOGGER);
    private final String SETTER = "setProperty";
    private static final String SEPARATOR = "#";
    private static final String MQ_PASS_FILE_PREFIX = "asmq";
    private static final String MQ_PASS_FILE_KEY = "imq.imqcmd.password=";

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
    private static final String BROKERBINDADDRESS="BrokerBindAddress";
    private static final String BROKERPORT="BrokerPort";
    private static final String BROKERARGS="BrokerArgs";
    private static final String BROKERHOMEDIR="BrokerHomeDir";
    private static final String BROKERLIBDIR ="BrokerLibDir";
    private static final String BROKERVARDIR="BrokerVarDir";
    private static final String BROKERJAVADIR="BrokerJavaDir";
    private static final String BROKERSTARTTIMEOUT="BrokerStartTimeOut";
    public static final String ADMINUSERNAME="AdminUsername";
    public static final String ADMINPASSWORD="AdminPassword";
    private static final String ADMINPASSFILE="AdminPassFile";
    private static final String USERNAME="UserName";
    private static final String PASSWORD="Password";
    private static final String MQ_PORTMAPPER_BIND = "doBind";//"imq.portmapper.bind";
    private static final String MASTERBROKER="MasterBroker";

    //JMX properties
    private static final String JMXSERVICEURL="JMXServiceURL";
    private static final String JMXSERVICEURLLIST="JMXServiceURLList";
    private static final String JMXCONNECTORENV="JMXConnectorEnv";
    private static final String USEJNDIRMISERVICEURL="useJNDIRMIServiceURL";
    private static final String RMIREGISTRYPORT="RmiRegistryPort";
    private static final String USEEXTERNALRMIREGISTRY="startRMIRegistry";
    private static final int DEFAULTRMIREGISTRYPORT =7776;
    private static final int BROKERRMIPORTOFFSET=100;


    private static final String SSLJMXCONNECTOR="SslJMXConnector";

    //Availability properties
    private static final String CONVENTIONAL_CLUSTER__OF_PEER_BROKERS_DB_PREFIX= "imq.cluster.sharecc.persist.jdbc.";
    private static final String ENHANCED_CLUSTER_DB_PREFIX= "imq.persist.jdbc.";
    private static final String HAREQUIRED = "HARequired";
    private static final String CLUSTERID = "ClusterId";
    private static final String BROKERID = "BrokerId";
    private static final String PINGINTERVAL = "PingInterval";
    private static final String DBTYPE = "DBType";
    private static final String DBTYPE_HADB="hadb";
    private static final String BROKERENABLEHA = "BrokerEnableHA";

    private static final String DB_HADB_PROPS = "DBProps";
    //properties within DB_PROPS
    private static final String DB_HADB_USER = "hadb.user";
    private static final String DB_HADB_PASSWORD = "hadb.password";
    private static final String DB_HADB_DRIVERCLASS = "hadb.driverClass";
    private static final String DS_HADB_PROPS = "DSProps";
    //properties within DS_PROPS
    private static final String DS_HADB_SERVERLIST = "hadb.serverList";

    //Not used now.
    private final String CONTAINER = "InAppClientContainer";

    //Activation config properties of MQ resource adapter.
    public static final String DESTINATION        = "destination";
    public static final String DESTINATION_TYPE   = "destinationType";
    private static String SUBSCRIPTION_NAME  = "SubscriptionName";
    private static String CLIENT_ID          = "ClientID";
    public static final String PHYSICAL_DESTINATION  = "Name";
    private static String MAXPOOLSIZE  = "EndpointPoolMaxSize";
    private static String MINPOOLSIZE  = "EndpointPoolSteadySize";
    private static String RESIZECOUNT  = "EndpointPoolResizeCount";
    private static String RESIZETIMEOUT  = "EndpointPoolResizeTimeout";
    private static String REDELIVERYCOUNT  = "EndpointExceptionRedeliveryAttempts";
    private static String LOWERCASE_REDELIVERYCOUNT  = "endpointExceptionRedeliveryAttempts";
    public static final String ADDRESSLIST  = "AddressList";
    private static String ADRLIST_BEHAVIOUR  = "AddressListBehavior";
    private static String ADRLIST_ITERATIONS  = "AddressListIterations";
    private static final String MDBIDENTIFIER = "MdbName";
    public static final String JMS_SERVICE = "mq-service";

    //MCF properties
    private static final String MCFADDRESSLIST = "MessageServiceAddressList";

    //private static final String XA_JOIN_ALLOWED= "imq.jmsra.isXAJoinAllowed";

    private StringManager sm =
        StringManager.getManager(ActiveJmsResourceAdapter.class);

    private MQAddressList urlList = null;

    private String addressList;

    private String brkrPort;

    private boolean doBind;

    //Properties in domain.xml for HADB JDBC connection pool (for HA)
    private static final String DUSERNAME = "User";
    private static final String DPASSWORD = "Password";
    private static final String DSERVERLIST = "ServerList";
    private static final String HADB_CONNECTION_URL_PREFIX = "jdbc:sun:hadb:";

    //Lifecycle properties
    public static final String EMBEDDED="EMBEDDED";
    public static final String LOCAL="LOCAL";
    public static final String REMOTE="REMOTE";
    public static final String DIRECT="DIRECT";

    private final String DEFAULT_STORE_POOL_JNDI_NAME = "jdbc/hastore";

    // Both the properties below are hacks. These will be changed later on.
    private static String MQRmiPort =
        System.getProperty("com.sun.enterprise.connectors.system.MQRmiPort");
    private static final String DASRMIPORT = "31099";

    private static final String REVERT_TO_EMBEDDED_PROPERTY =
        "com.sun.enterprise.connectors.system.RevertToEmbedded";
    private static final String BROKER_RMI_PORT =
        "com.sun.enterprise.connectors.system.mq.rmiport";

    private static final String DEFAULT_SERVER = "server";
    private static final String DEFAULT_MQ_INSTANCE = "imqbroker";
    public static final String MQ_DIR_NAME = "imq";

    public static final String GRIZZLY_PROXY_PREFIX = "JMS_PROXY_";

    private static enum ClusterMode {
      ENHANCED, CONVENTIONAL_WITH_MASTER_BROKER, CONVENTIONAL_OF_PEER_BROKERS;
    }

    private Properties dbProps = null;
    private Properties dsProps = null;
    private String brokerInstanceName = null;

    private File mqPassFile = null;
    private boolean grizzlyListenerInit;
    private Set<String> grizzlyListeners = new HashSet<String>();

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
    
    private Provider<JmsService> jmsServiceProvider;
    
    @Inject
    private Provider<ServerContext> serverContextProvider;
    
    @Inject
    private Provider<ConnectorRuntime> connectorRuntimeProvider;

    @Inject
     private ServiceLocator habitat;
    
    @Inject
    private ApplicationRegistry appRegistry;

    /**
     * Constructor for an active Jms Adapter.
     *
     */
    public ActiveJmsResourceAdapter() {
        super();
        //if(getJmsService() == null)return;

        //Now that the RA has been started, delete the temp passfile
        if (mqPassFile != null) {
            mqPassFile.delete();
        }
    }
    public void postConstruct()
    {
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
    		       JMSConfigListener jmsConfigListener=jmsConfigListenerProvider.get();
                   jmsConfigListener.setActiveResourceAdapter(this);
                   JmsRaUtil raUtil = new JmsRaUtil();
                   raUtil.upgradeIfNecessary();
           }
           catch (Throwable t) {
               //t.printStackTrace();
                   _logger.log(Level.FINE,"Cannot upgrade jmsra"+ t.getMessage());
           }
    }

    /**
     * Loads RA configuration for MQ Resource adapter.
     *
     * @throws ConnectorRuntimeException in case of an exception.
     */
    protected void loadRAConfiguration() throws ConnectorRuntimeException{
        if (connectorRuntime.isServer()) {
            // Check whether MQ has started up or not.
            try {
               // if (!JmsProviderLifecycle.shouldUseMQRAForLifecycleControl()) {
                 //   JmsProviderLifecycle.checkProviderStartup();
                //} else {
                    setLifecycleProperties();
                //}
            } catch (Exception e) {
                ConnectorRuntimeException cre = new ConnectorRuntimeException
                                                        (e.getMessage());
                throw (ConnectorRuntimeException) cre.initCause(e);
            }

            setMdbContainerProperties();
            setJmsServiceProperties(null);
            setClusterRABeanProperties();
            setAvailabilityProperties();
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
                                    _logger.log(Level.WARNING, "Failed to shut down Grizzly NetworkListener " + listenerName, e);
                                }
                            }
                        }
                    }
                }
                grizzlyListenerInit = false;
            }
        } catch (Throwable th) {
            _logger.log(Level.WARNING, "Error occurs when shutting down JMSRA: " + th.getMessage());
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
     */  
    public void initializeLazyListener(JmsService jmsService) {
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
                                grizzlyService.createNetworkProxy(dummy);
                                grizzlyListeners.add(name);
                            }
                            grizzlyListenerInit = true;
                        }
                    }
                }
            }
        }
    }

    protected void startResourceAdapter(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        try{
        if (this.moduleName_.equals(ConnectorRuntime.DEFAULT_JMS_ADAPTER)) {
            if (connectorRuntime.isServer()) {
                Domain domain = Globals.get(Domain.class);
                ServerContext serverContext = Globals.get(ServerContext.class);
                Server server = domain.getServerNamed(serverContext.getInstanceName());
                JmsService jmsService = server.getConfig().getExtensionByType(JmsService.class);
                initializeLazyListener(jmsService);
            }
		//System.setProperty("imq.jmsra.direct.clustered", "true");
                AccessController.doPrivileged
                        (new java.security.PrivilegedExceptionAction() {
                            public Object run() throws
                                    ResourceAdapterInternalException {
                                //set the JMSRA system property to enable XA JOINS
                                //disabling this due to issue - 8727
                                //System.setProperty(XA_JOIN_ALLOWED, "true");
                                resourceadapter_.start(bootStrapContextImpl);
                                return null;
                            }
                        });
                //setResourceAdapter(resourceadapter_);
            } else {
                resourceadapter_.start(bootStrapContextImpl);
            }
        }catch (PrivilegedActionException ex){
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
     */
     protected Set mergeRAConfiguration(ResourceAdapterConfig raConfig, List<Property> raConfigProps) {
   //private void hackMergedProps(Set mergedProps) {
        if (!(connectorRuntime.isServer())) {
            return super.mergeRAConfiguration(raConfig, raConfigProps);
        }
        Set mergedProps = super.mergeRAConfiguration(raConfig, raConfigProps);
        String brokerType = null;

        for (Iterator iter = mergedProps.iterator(); iter.hasNext();) {
            ConnectorConfigProperty  element = (ConnectorConfigProperty) iter.next();
            if (element.getName().equals(ActiveJmsResourceAdapter.BROKERTYPE)) {
                     brokerType = element.getValue();
            }
        }
	boolean cluster = false;
	try {
		cluster = isClustered();
	} catch (Exception e) {
		e.printStackTrace();
	}
	// hack is required only for nonclustered nonremote brokers.
	if (!cluster) {
        if (brokerType.equals(ActiveJmsResourceAdapter.LOCAL)
                || brokerType.equals(ActiveJmsResourceAdapter.EMBEDDED)
		|| brokerType.equals(ActiveJmsResourceAdapter.DIRECT))
        {
		for (Iterator iter = mergedProps.iterator(); iter.hasNext();) {
                ConnectorConfigProperty  element = (ConnectorConfigProperty ) iter.next();
                if (element.getName().equals(ActiveJmsResourceAdapter.CONNECTION_URL)) {
                    iter.remove();
                }
          }
    	}
	}
        return mergedProps;
    }
    //Overriding ActiveResourceAdapterImpl.setup() as a work around for
    //this condition - connectionDefs_.length != 1
    //Need to remove this once the original problem is fixed
     public void setup() throws ConnectorRuntimeException {
        //TODO NEED TO REMOVE ONCE THE ActiveResourceAdapterImpl.setup() is fixed
        if (connectionDefs_ == null) {
            throw new ConnectorRuntimeException("No Connection Defs defined in the RA.xml");
        }
        if (isServer() && !isSystemRar(moduleName_)) {
            createAllConnectorResources();
        }
        _logger.log(Level.FINE, "Completed Active Resource adapter setup", moduleName_);
    }

    /*

     * Set Availability related properties
     * If JMS availability true set availability properties
     * read configured pool information and set.
     */

    private void setAvailabilityProperties() throws ConnectorRuntimeException {
	    if(!isClustered()) return;
        try {
          Domain domain = Globals.get(Domain.class);
          ServerContext serverContext = Globals.get(ServerContext.class);
          Server server = domain.getServerNamed(serverContext.getInstanceName());

          JmsService jmsService = server.getConfig().getExtensionByType(JmsService.class);
          if (jmsService.getType().equals(REMOTE)) {
            //If REMOTE, the broker cluster instances already have
            //been configured with the right properties.
            return;
          }

          AvailabilityService as = server.getConfig().getAvailabilityService();
          if (as == null) {
            logFine("Availability Service is null. Not setting AvailabilityProperties.");
            return;
          }

          boolean useMasterBroker = true;
          if(as.getExtensionByType(JmsAvailability.class) != null && ! MASTERBROKER.equalsIgnoreCase(as.getExtensionByType(JmsAvailability.class).getConfigStoreType()))
            useMasterBroker = false;

          //jmsService.getUseMasterBroker() != null ? Boolean.valueOf(jmsService.getUseMasterBroker()) :true;
          boolean isJmsAvailabilityEnabled = this.isJMSAvailabilityOn(as);

          logFine("Setting AvailabilityProperties .. ");
          if (!useMasterBroker || isJmsAvailabilityEnabled) {
            // For conventional cluster of peer brokers and Enhanced Broker Cluster.
            ConnectorDescriptor cd = getDescriptor();
            String clusterName = getMQClusterName();
            ConnectorConfigProperty  envProp1 = new ConnectorConfigProperty (
                        CLUSTERID , clusterName,"Cluster Id",
                        "java.lang.String");
            setProperty(cd, envProp1);

            if(brokerInstanceName == null) {
                brokerInstanceName = getBrokerInstanceName(jmsService);
            }
            ConnectorConfigProperty  envProp2 = new ConnectorConfigProperty (
                        BROKERID , brokerInstanceName,"Broker Id",
                        "java.lang.String");
            setProperty(cd, envProp2);

            //Only if JMS availability is true - Enhanced Broker Cluster only.
            if (isJmsAvailabilityEnabled) {
              //Set HARequired as true - irrespective of whether it is REMOTE or
              //LOCAL
              ConnectorConfigProperty  envProp3 = new ConnectorConfigProperty (
                          HAREQUIRED , "true","HA Required",
                          "java.lang.String");
              setProperty(cd, envProp3);
               /* The broker has a property to control whether
               * it starts in HA mode or not and that's represented on
               * the RA by BrokerEnableHA.
               * On the MQ Client connection side it is HARequired -
               * this does not control the broker, it just is a client
               * side requirement.
               * So for AS EE, if BrokerType is LOCAL or EMBEDDED,
               * and AS HA is enabled for JMS then both these must be
               * set to true. */

              ConnectorConfigProperty  envProp4 = new ConnectorConfigProperty (
                          BROKERENABLEHA , "true",
                          "BrokerEnableHA flag","java.lang.Boolean");
              setProperty(cd, envProp4);

              String nodeHostName = domain.getNodeNamed(server.getNodeRef()).getNodeHost();
              if (nodeHostName != null) {
                ConnectorConfigProperty  envProp5 = new ConnectorConfigProperty (
                            BROKERBINDADDRESS , nodeHostName,
                            "Broker Bind Address","java.lang.String");
                setProperty(cd, envProp5);
              }
              loadDBProperties(as.getExtensionByType(JmsAvailability.class), ClusterMode.ENHANCED);
            } else {
              //  Conventional cluster of peer brokers
              JmsAvailability jmsAvailability = as.getExtensionByType(JmsAvailability.class);
              if ("jdbc".equals(jmsAvailability.getMessageStoreType()))
                loadDBProperties(jmsAvailability, ClusterMode.ENHANCED);
              loadDBProperties(jmsAvailability, ClusterMode.CONVENTIONAL_OF_PEER_BROKERS);
            }




            /*
            ConnectorConfigProperty  envProp4 = new ConnectorConfigProperty  (
                            DBTYPE , DBTYPE_HADB,"DBType",
                            "java.lang.String");
            setProperty(cd, envProp4);



            * The broker has a property to control whether
            * it starts in HA mode or not and that's represented on
            * the RA by BrokerEnableHA.
            * On the MQ Client connection side it is HARequired -
            * this does not control the broker, it just is a client
            * side requirement.
            * So for AS EE, if BrokerType is LOCAL or EMBEDDED,
            * and AS HA is enabled for JMS then both these must be
            * set to true.

            ConnectorConfigProperty  envProp5 = new ConnectorConfigProperty  (
                        BROKERENABLEHA , "true",
                        "BrokerEnableHA flag","java.lang.Boolean");
            setProperty(cd, envProp5);

            String nodeHostName = domain.getNodeNamed(server.getNode()).getNodeHost();
            if (nodeHostName != null)  {
              ConnectorConfigProperty  envProp6 = new ConnectorConfigProperty  (
                          BROKERBINDADDRESS , nodeHostName,
                          "Broker Bind Address","java.lang.String");
              setProperty(cd, envProp6);
            }
            //get pool name
            String poolJNDIName = as.getJmsAvailability().getMqStorePoolName();
            //If no MQ store pool name is specified, use default poolname
            //XXX: default pool name is jdbc/hastore but asadmin
            //configure-ha-cluster creates a resource called
            //"jdbc/<asclustername>-hastore" which needs to be used.
            if (poolJNDIName == null || poolJNDIName =="" ) {
              //get Web container's HA store's pool name
              poolJNDIName = as.getWebContainerAvailability().
                                getHttpSessionStorePoolName();
              logFine("HTTP Session store pool jndi name " +
                         "is " + poolJNDIName);
            }
            //XXX: request HADB team mq-store-pool name to be
            //populated as part of configure-ha-cluster

            JdbcConnectionPool jdbcConPool = getJDBCConnectionPoolInfo(
                                                poolJNDIName);
            //DBProps: compute values from pool object
            String userName = getPropertyFromPool(jdbcConPool, DUSERNAME);
            logFine("HA username is " + userName);

            String password = getPropertyFromPool(jdbcConPool, DPASSWORD);
            logFine("HA Password is " + password);

            String driverClass = jdbcConPool.getDatasourceClassname();
            logFine("HA driverclass" + driverClass);

            dbProps = new Properties();
            dbProps.setProperty(DB_HADB_USER, userName);
            dbProps.setProperty(DB_HADB_PASSWORD, password);
            dbProps.setProperty(DB_HADB_DRIVERCLASS, driverClass);

            //DSProps: compute values from pool object
            String serverList = getPropertyFromPool(jdbcConPool, DSERVERLIST);
            logFine("HADB server list is " + serverList);
            dsProps = new Properties();

            if (serverList != null) {
                dsProps.setProperty(DS_HADB_SERVERLIST, serverList);
            } else {
                _logger.warning("ajra.incorrect_hadb_server_list");
            }

            //set all other properties in dsProps as well.
            Properties p = getDSPropertiesFromThePool(jdbcConPool);
            Iterator iterator = p.keySet().iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String val = (String)p.get(key);
                dsProps.setProperty(key, val);
            }*/
          } else {
            // Conventional cluster with master broker.
            if ("jdbc".equals(as.getExtensionByType(JmsAvailability.class).getMessageStoreType()))
              loadDBProperties(as.getExtensionByType(JmsAvailability.class), ClusterMode.CONVENTIONAL_WITH_MASTER_BROKER);
          }
        } catch (Exception e) {
            ConnectorRuntimeException crex = new ConnectorRuntimeException(e.getMessage());
            throw (ConnectorRuntimeException)crex.initCause(e);
        }
    }

    private void loadDBProperties(JmsAvailability jmsAvailability, ClusterMode clusterMode) {
        /*imq.cluster.nomasterbroker=[true|false] default false
                imq.cluster.sharecc.persist.jdbc.dbVendor
                imq.cluster.sharecc.persist.jdbc.<dbVendor>.user
                imq.cluster.sharecc.persist.jdbc.<dbVendor>.property.<propname>=<propval>
                imq.cluster.sharecc.persist.jdbc.<dbVendor>.table.MQSHARECC45=<table-schema>

                These broker properties, including imq.cluster.clusterid, must be set to
                same values in all broker instances in the conventional cluster

                */
    String prefix = null;        
    if (ClusterMode.CONVENTIONAL_WITH_MASTER_BROKER == clusterMode) {
      prefix = ENHANCED_CLUSTER_DB_PREFIX;
    } else if (ClusterMode.CONVENTIONAL_OF_PEER_BROKERS == clusterMode) {
      prefix = CONVENTIONAL_CLUSTER__OF_PEER_BROKERS_DB_PREFIX;
    } else if (ClusterMode.ENHANCED == clusterMode) {
      prefix = ENHANCED_CLUSTER_DB_PREFIX;
    } else {
      logFine("Unknown cluster mode: " + clusterMode.name() + ", imq DB properties are not set.");
      return;
    }
    if(dbProps == null) dbProps = new Properties();
	dbProps.setProperty("imq.cluster.clusterid", getMQClusterName());
    dbProps.setProperty("imq.persist.store", jmsAvailability.getMessageStoreType());
    if(ClusterMode.CONVENTIONAL_WITH_MASTER_BROKER == clusterMode)
      dbProps.setProperty("imq.cluster.nomasterbroker", "false");
    else
      dbProps.setProperty("imq.cluster.nomasterbroker", "true");
    if(Boolean.valueOf(jmsAvailability.getAvailabilityEnabled()) == true || "jdbc".equals(jmsAvailability.getMessageStoreType()))
      dbProps.setProperty("imq.brokerid", getBrokerInstanceName(getJmsService()));

        String dbVendor = jmsAvailability.getDbVendor();
        String dbuser = jmsAvailability.getDbUsername();
        String dbPassword = jmsAvailability.getDbPassword();
        String dbJdbcUrl = jmsAvailability.getDbUrl();

        dbProps.setProperty(prefix + "dbVendor", dbVendor);

        String fullprefix = prefix + dbVendor + ".";
        if (dbuser != null) dbProps.setProperty(fullprefix + "user", dbuser);
        if (dbPassword != null) dbProps.setProperty(fullprefix + "password", dbPassword);
        List dbprops = jmsAvailability.getProperty();

        String propertyPrefix = fullprefix + "property.";

        if(dbJdbcUrl != null) {
		if ("derby".equals(dbVendor))	
			dbProps.setProperty(fullprefix + "opendburl", dbJdbcUrl);
		else
			dbProps.setProperty(propertyPrefix + "url", dbJdbcUrl);
	}

        for (Object obj : dbprops){
	    Property prop = (Property) obj;	
            String key = prop.getName();
            String value = prop.getValue();
            //don't set a prefix if the property name is already prefixed with "imq."
            if(key.startsWith("imq."))
                dbProps.setProperty(key, value);
            else
                dbProps.setProperty(propertyPrefix + key, value);
        }
    }

    /*
     * Gets all the other [apart from serverlist] DataSource properties from
     * the HADB JDBC connection pool.
     *
    private Properties getDSPropertiesFromThePool(JdbcConnectionPool jdbcConPool) {
        Properties p = new Properties();
        List elemProp = jdbcConPool.getProperty();
        Set<String> excludeList = new HashSet<String>();
        excludeList.add(DUSERNAME);
        excludeList.add(DPASSWORD);
        excludeList.add(DSERVERLIST);

        for(Property e: elemProp) {
            String propName = e.getAttributeValue("name");
            if (!excludeList.contains(propName)) {
                p.setProperty(propName, e.getAttributeValue("value"));
            }
        }
        logFine("Additional DataSource properties from pool "
                        + jdbcConPool.getName() + " are " + p);
        return p;
    }*/

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
		//if(isClustered()){
		if(dbProps == null)
			dbProps = new Properties();
		 dbProps.setProperty("imq.cluster.dynamicChangeMasterBrokerEnabled", "true");			
	//}

            if (dbProps != null) {
                Method mthds = this.resourceadapter_.getClass().getMethod("setBrokerProps", Properties.class);
            	if(mthds != null){        
	    		logFine("Setting property:" + DB_HADB_PROPS
                                        + "=" + dbProps.toString());
                        mthds.invoke(this.resourceadapter_,
                                        new Object[]{dbProps});
               		 }
            }
        } catch (Exception e) {
            ConnectorRuntimeException crex = new ConnectorRuntimeException(
                            e.getMessage());
            throw (ConnectorRuntimeException)crex.initCause(e);
        }
    }

    //todo: enable this method
   /* private String getPropertyFromPool(JdbcConnectionPool jdbcConPool,
                                       String poolPropertyName) {
        String poolPropertyValue = null;
        if(jdbcConPool == null) {
            return null;
        }
        ElementProperty[] props = jdbcConPool.getElementProperty();
        for (int i = 0; i < props.length; i++) {
            String name = props[i].getAttributeValue("name");
            String value = props[i].getAttributeValue("value");
            if (name.equalsIgnoreCase(poolPropertyName)) {
            //if (name.equalsIgnoreCase("username")) {
                poolPropertyValue = value;
            }
        }
        logFine("ActiveJMSResourceAdapter :: got property " + poolPropertyName
                        + "="+ poolPropertyValue);
        return poolPropertyValue;
    }      */

    private boolean isJMSAvailabilityOn(AvailabilityService as) {

        if (as == null) {
            return false;
      }
      JmsAvailability ja = as.getExtensionByType(JmsAvailability.class);
      boolean jmsAvailability = false;
     /* JMS Availability  should be false if its not present in
        * domain.xml,
       */
     if (ja != null) {
        jmsAvailability = Boolean.parseBoolean(ja.getAvailabilityEnabled());
    }
        _logger.log(Level.FINE, "JMS availability :: " + jmsAvailability);
        return jmsAvailability;
    }

    /**
     * Set MQ4.0 RA lifecycle properties
     */
    private void setLifecycleProperties() throws
                                      Exception, ConnectorRuntimeException {
        //ConfigContext ctx = ApplicationServer.getServerContext().getConfigContext();


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
        logFine("Default JMS Host :: " + defaultJmsHost);

        JmsHost jmsHost = getJmsHost();


        if (jmsHost != null) {//todo: && jmsHost.isEnabled()) {
            JavaConfig javaConfig = (JavaConfig) Globals.get(JavaConfig.class); ;
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
            List jmsHostProps= getJmsService().getProperty();

            String username = null;
            String password = null;
            if (jmsHostProps != null) {
            for (int i =0;i <jmsHostProps.size(); i++) {
                Property jmsProp =(Property)jmsHostProps.get(i);
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

        logFine("Broker UserName = " + username);
            createMQVarDirectoryIfNecessary();
            String brokerVarDir = getMQVarDir();

            String tmpString = getJmsService().getStartArgs();
            if (tmpString == null) {
                tmpString = "";
            }

            String brokerArgs = tmpString;


            //XX: Extract the information from the optional properties.
       List jmsProperties =    getJmsService().getProperty();
	   List jmsHostProperties = jmsHost.getProperty();
	   Properties jmsServiceProp = listToProperties(jmsProperties);
	   Properties jmsHostProp = listToProperties (jmsHostProperties);

	   jmsServiceProp.putAll(jmsHostProp);
	   if(jmsServiceProp.size() > 0) {
           if(dbProps == null)
                dbProps = new Properties();

           dbProps.putAll(jmsServiceProp);
       }
        /*
	   String jmsPropertiesStr =  null ;
	   if(jmsServiceProp.size() > 0)
	   {
	   try{
	   	StringWriter writer = new StringWriter();
	   	jmsServiceProp.store(writer, "Properties defined in JMSService and JMSHost");
	   	jmsPropertiesStr =  writer.toString();
	   }catch(Exception e){}//todo: log error;
	   } */
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
	    /*if(jmsPropertiesStr != null){
            	ConnectorConfigProperty  envProp = new ConnectorConfigProperty  (
                    "BrokerProps", jmsPropertiesStr, "Broker Props", "java.lang.String");
            	setProperty(cd, envProp);
	    }  */
            ConnectorConfigProperty  envProp1 = new ConnectorConfigProperty  (
                    BROKERTYPE, brokerType, "Broker Type", "java.lang.String");
            setProperty(cd, envProp1);
            ConnectorConfigProperty  envProp2 = new ConnectorConfigProperty  (
                    BROKERINSTANCENAME, brokerInstanceName ,
                    "Broker Instance Name", "java.lang.String");
            setProperty(cd, envProp2);
            ConnectorConfigProperty  envProp3 = new ConnectorConfigProperty  (
                    BROKERPORT , brokerPort ,
                    "Broker Port", "java.lang.String");
            setProperty(cd, envProp3);
            ConnectorConfigProperty  envProp4 = new ConnectorConfigProperty  (
                    BROKERARGS , brokerArgs ,
                    "Broker Args", "java.lang.String");
            setProperty(cd, envProp4);
            ConnectorConfigProperty  envProp5 = new ConnectorConfigProperty  (
                    BROKERHOMEDIR , brokerHomeDir ,
                    "Broker Home Dir", "java.lang.String");
            setProperty(cd, envProp5);
            ConnectorConfigProperty  envProp14 = new ConnectorConfigProperty  (
                    BROKERLIBDIR , brokerLibDir ,
                    "Broker Lib Dir", "java.lang.String");
            setProperty(cd, envProp14);
            ConnectorConfigProperty  envProp6 = new ConnectorConfigProperty  (
                    BROKERJAVADIR , java_home ,
                    "Broker Java Dir", "java.lang.String");
                    setProperty(cd, envProp6);
            ConnectorConfigProperty  envProp7 = new ConnectorConfigProperty  (
                    BROKERVARDIR , brokerVarDir ,
                    "Broker Var Dir", "java.lang.String");
            setProperty(cd, envProp7);
            ConnectorConfigProperty  envProp8 = new ConnectorConfigProperty  (
                    BROKERSTARTTIMEOUT , "" + brokerTimeOut ,
                    "Broker Start Timeout", "java.lang.String");
            setProperty(cd, envProp8);
            ConnectorConfigProperty  envProp9 = new ConnectorConfigProperty  (
                    ADMINUSERNAME , adminUserName,
                    "Broker admin username", "java.lang.String");
            setProperty(cd, envProp9);
            ConnectorConfigProperty  envProp10 = new ConnectorConfigProperty  (
                    ADMINPASSWORD , adminPassword ,
                    "Broker admin password", "java.lang.String");
            setProperty(cd, envProp10);
            ConnectorConfigProperty  envProp11 = new ConnectorConfigProperty  (
                    USERNAME , username,
                    "Broker username", "java.lang.String");
            setProperty(cd, envProp11);
            ConnectorConfigProperty  envProp12 = new ConnectorConfigProperty  (
                    PASSWORD , password,
                    "Broker password", "java.lang.String");
            setProperty(cd, envProp12);

           /* //set adminpassfile
            if (!getJmsService().getType().equals(REMOTE)) {
                //For LOCAL and EMBEDDED, we pass in the admin pass file path
                //containing the MQ admin password to enable authenticated
                //startup of the broker.
                String adminPassFilePath = getAdminPassFilePath(adminPassword);
                if (adminPassFilePath != null) {
                    ConnectorConfigProperty  envProp13 = new ConnectorConfigProperty  (
                            ADMINPASSFILE , adminPassFilePath ,
                            "Broker admin password", "java.lang.String");
                    setProperty(cd, envProp13);
                }
            }*/
        }
        //Optional
        //BrokerBindAddress, RmiRegistryPort
    }

   private Properties listToProperties(List<Property> props){
	Properties properties = new Properties();   
	if(props != null){
		for (Property prop : props){
			String key = prop.getName();
			String value = prop.getValue();

			properties.setProperty(key, value);
		}
	}

	return properties;
   }

    private String adjustForDirectMode(String brokerType) {
        if (! isClustered() && brokerType.equals(EMBEDDED)) {
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
        ServerEnvironmentImpl serverenv = Globals.get(ServerEnvironmentImpl.class);
        Domain domain = Globals.get(Domain.class);
        String asInstance = serverenv.getInstanceName();
        String domainName = null;
        if (isClustered()) {
            Server server = domain.getServerNamed(asInstance);

            domainName = server.getCluster().getName();
            /*ClusterHelper.getClusterForInstance(
                            ApplicationServer.getServerContext().getConfigContext(),
                            asInstance).getName();*/
        } else {
            domainName = serverenv.getDomainName();//ServerManager.instance().getDomainName();
        }
        String s = getBrokerInstanceName(domainName, asInstance, js);
        logFine("Got broker Instancename as " + s);
        String converted = convertStringToValidMQIdentifier(s);
        logFine("converted instance name " + converted);
        return converted;
    }

    public boolean handles(ConnectorDescriptor cd, String moduleName) {
        return ConnectorsUtil.isJMSRA(moduleName);
    }

    public void validateActivationSpec(ActivationSpec spec) {
       boolean validate =  "true".equals(System.getProperty("validate.jms.ra"));
                if (validate) {
                    try {
                        spec.validate();
                    } catch (Exception ex) {
                        _logger.log(Level.SEVERE,  "endpointfactory.as_validate_Failed", ex);
                    }
                }
    }
     /**
     * Computes the instance name for the MQ broker.
     */
    private static String getBrokerInstanceName(String asDomain,
        String asInstance, JmsService js) {

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
         // If the directory doesnt exist, create it.
         // It is necessary for windows.
         java.io.File instanceDir = new java.io.File(mqInstanceDir);
         if (!(instanceDir.exists() && instanceDir.isDirectory())) {
             instanceDir.mkdirs();
         }
    }

    private String getMQVarDir(){
        String asInstanceRoot = getServerEnvironment().getDomainRoot().getPath();
                            /*ApplicationServer.getServerContext().
                                  getInstanceEnvironment().getInstancesRoot();   */
        String mqInstanceDir =  asInstanceRoot + java.io.File.separator
                                                 + MQ_DIR_NAME;
        return mqInstanceDir;
    }
    private String getBrokerLibDir() {
        String brokerLibDir = java.lang.System.getProperty(SystemPropertyConstants.IMQ_LIB_PROPERTY);
        logFine("broker lib dir from system property " + brokerLibDir);
    return brokerLibDir;
   }

    private String getBrokerHomeDir() {
        // If the property was not specified, then look for the
        // imqRoot as defined by the com.sun.aas.imqRoot property
        String brokerHomeDir = java.lang.System.getProperty(SystemPropertyConstants.IMQ_BIN_PROPERTY);
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
            brokerHomeDir = getServerEnvironment().getDomainRoot()//ApplicationServer.getServerContext().getInstallRoot()
                                + IMQ_INSTALL_SUBDIR;
        } else {
            //hack until MQ RA changes
            brokerHomeDir = brokerHomeDir + java.io.File.separator + ".." ;
        }

        logFine("Broker Home Directory :: " + brokerHomeDir);
        logFine("broker home dir finally" + brokerHomeDir);
        return brokerHomeDir;

    }

    ////@Siva: provide an API to read JMX information from RA and return it.
    //private



    /**
     * Sets the SE/EE specific MQ-RA bean properties
     * @throws ConnectorRuntimeException
     */
    private void setClusterRABeanProperties() throws ConnectorRuntimeException {
        ConnectorDescriptor cd = super.getDescriptor();
        try {
            if (isClustered()) {
                JmsService jmsService = (JmsService)Globals.get(JmsService.class);
               /* ConfigContext ctx = ApplicationServer.getServerContext().
                    getConfigContext();
            JmsService jmsService = ServerBeansFactory.
                getConfigBean(ctx).getJmsService();    */
                String val = getGroupName();
                ConnectorConfigProperty  envProp = new ConnectorConfigProperty
                    (GROUPNAME, val, "Group Name", "java.lang.String");
                setProperty(cd, envProp);
                logFine("CLUSTERED instance - setting groupname as" + val);

		    boolean inClusteredContainer = false;
		    if(jmsService.getType().equals(EMBEDDED) || jmsService.getType().equals(LOCAL))
			    inClusteredContainer = true;

            ConnectorConfigProperty  envProp1 = new ConnectorConfigProperty
              (CLUSTERCONTAINER, Boolean.toString(inClusteredContainer), "Cluster container flag",
                "java.lang.Boolean");
            setProperty(cd, envProp1);
            logFine("CLUSTERED instance - setting inclusteredcontainer as" + inClusteredContainer);
           if (jmsService.getType().equals(REMOTE)) {

            /*
                * Do not set master broker for remote broker.
                * The RA might ignore it if we set, but we have to
                        * be certain from our end.
                */
                     return;
            } else {
               if (! isDBEnabled()){
		       
                String masterbrkr = getMasterBroker();
                    ConnectorConfigProperty  envProp2 = new ConnectorConfigProperty
                        (MASTERBROKER,masterbrkr , "Master  Broker",
                    "java.lang.String");
                    setProperty(cd, envProp2);
                    logFine("MASTERBROKER - setting master broker val"
                    + masterbrkr);
               }
        }
            } else {
                logFine("Instance not Clustered and hence not setting " +
                        "groupname");
            }
        } catch (Exception e) {
            ConnectorRuntimeException crex = new ConnectorRuntimeException(e.getMessage());
            throw (ConnectorRuntimeException)crex.initCause(e);
        }
    }

    private boolean isDBEnabled(){
        Domain domain = Globals.get(Domain.class);
        ServerContext serverContext = Globals.get(ServerContext.class);
        Server server = domain.getServerNamed(serverContext.getInstanceName());

        AvailabilityService as = server.getConfig().getAvailabilityService();

	if (as != null){
		JmsAvailability jmsAvailability = as.getExtensionByType(JmsAvailability.class);
        	if (jmsAvailability.getAvailabilityEnabled() != null && Boolean.parseBoolean(jmsAvailability.getAvailabilityEnabled())){
            		return true;
		} else
            if(jmsAvailability.getConfigStoreType() != null && ! "MASTERBROKER".equalsIgnoreCase(jmsAvailability.getConfigStoreType()))
                return true;
        }

        return false;
    }
    /**
     * Sets the SE/EE specific MQ-RA bean properties
     * @throws ConnectorRuntimeException
     */
    private void setAppClientRABeanProperties() throws ConnectorRuntimeException {
        logFine("In Appclient container!!!");
        ConnectorDescriptor cd = super.getDescriptor();
        ConnectorConfigProperty  envProp1 = new ConnectorConfigProperty  (
                        BROKERTYPE, REMOTE, "Broker Type", "java.lang.String");
                setProperty(cd, envProp1);

        ConnectorConfigProperty  envProp2 = new ConnectorConfigProperty  (
            GROUPNAME, "", "Group Name", "java.lang.String");
        cd.removeConfigProperty(envProp2);
        ConnectorConfigProperty  envProp3 = new ConnectorConfigProperty  (
            CLUSTERCONTAINER, "false", "Cluster flag", "java.lang.Boolean");
        setProperty(cd, envProp3);
    }        Domain domain = Globals.get(Domain.class);



    private static boolean isClustered()  {
        Domain domain = Globals.get(Domain.class);
        Clusters clusters = domain.getClusters();
        if (clusters == null) return false;

        List clusterList = clusters.getCluster();

        ServerContext serverctx = Globals.get(ServerContext.class);
        return JmsRaUtil.isClustered(clusterList, serverctx.getInstanceName());
    }

    private String getGroupName() throws Exception{
        return getDomainName() + SEPARATOR + getClusterName();
    }

    private String getClusterName() {

        ServerContext serverctx = Globals.get(ServerContext.class);
        String instanceName = serverctx.getInstanceName();

        Domain domain = Globals.get(Domain.class);
        Server server = domain.getServerNamed(instanceName);

        return server.getCluster() != null? server.getCluster().getName(): null;

                /*ClusterHelper.getClusterForInstance(this.serverContxt.
                       .getConfigContext(),
                        serverContxt.getInstanceName()).getName();*/
    }

    /*
     * Generates an Name for the MQ Cluster associated with the
     * application server cluster.
     */
    private String getMQClusterName() {
        return convertStringToValidMQIdentifier(getClusterName()) + "_MQ";
    }

    /**
     * Master Broker name in the cluster, assumes the first broker in
     * in the list is the master broker , and this consistency has to
     * be maintained in all the instances.
     */
     private String getMasterBroker() throws Exception {
    return urlList.getMasterBroker(getClusterName());
     }

    //All Names passed into MQ needs to be valid Java Identifiers
    //so as of now replacing all characters that are not valid
    //java identifier components with '_'
    private static String convertStringToValidMQIdentifier(String s) {
        if (s == null) return "";

        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < s.length(); i++) {
            if(Character.isLetterOrDigit(s.charAt(i))){
                            //|| s.charAt(i) == '_'){
                buf.append(s.charAt(i));
            }
        }
        return buf.toString();
    }

    private String getDomainName() throws Exception {
       // ConfigContext ctxt = ApplicationServer.getServerContext().getConfigContext();
        //computing hashcode, since the application root string could
        //be potentially large
        /*
        String domainName = "" + ServerBeansFactory.getDomainBean(ctxt).
                            getApplicationRoot().hashCode();
        return domainName;
        */
        // FIX LATER
        return "";
    }

    /**
     * Recreates the ResourceAdapter using new values from JmsSerice.
     *
     * @param js JmsService element of the domain.xml
     * @throws ConnectorRuntimeException in case of any backend error.
     */
    public void reloadRA(JmsService js) throws ConnectorRuntimeException {
        setMdbContainerProperties();
        setJmsServiceProperties(js);

        super.loadRAConfiguration();
    rebindDescriptor();
    }

    /**
     * Adds the JmsHost to the MQAddressList of the resource adapter.
     *
     * @param host JmsHost element in the domain.xml
     * @throws ConnectorRuntimeException in case of any backend error.
     */
    public void addJmsHost(JmsHost host) throws ConnectorRuntimeException {
        urlList.addMQUrl(host);
        setAddressList();
    }

    /**
     * Removes the JmsHost from the MQAddressList of the resource adapter.
     *
     * @param host JmsHost element in the domain.xml
     * @throws ConnectorRuntimeException in case of any backend error.
     */
    public void deleteJmsHost(JmsHost host) throws ConnectorRuntimeException {
        urlList.removeMQUrl(host);
        setAddressList();
    }

    protected JmsHost getJmsHost() {
        String defaultJmsHost = getJmsService().getDefaultJmsHost();
        if (defaultJmsHost == null || defaultJmsHost.equals("")) {
            return (JmsHost) Globals.get(JmsHost.class);
        }

        List jmsHostsList = getJmsService().getJmsHost();
        if (jmsHostsList == null || jmsHostsList.size() == 0) {
            return (JmsHost) Globals.get(JmsHost.class);
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
     * Updates the JmsHost information in the MQAddressList of the resource adapter.
     *
     * @param host JmsHost element in the domain.xml
     * @throws ConnectorRuntimeException in case of any backend error.
     */
    public void updateJmsHost(JmsHost host) throws ConnectorRuntimeException {
        urlList.updateMQUrl(host);
        setAddressList();
    }

    public boolean getDoBind() {
        return doBind;
    }

    private void setMdbContainerProperties() throws ConnectorRuntimeException {
        JmsRaUtil raUtil = new JmsRaUtil(null);

        ConnectorDescriptor cd = super.getDescriptor();
        raUtil.setMdbContainerProperties();

        String val = ""+MdbContainerProps.getReconnectEnabled();
        ConnectorConfigProperty  envProp2 = new ConnectorConfigProperty  (
            RECONNECTENABLED, val, val, "java.lang.Boolean");
        setProperty(cd, envProp2);

        val = ""+MdbContainerProps.getReconnectDelay();
        ConnectorConfigProperty  envProp3 = new ConnectorConfigProperty  (
            RECONNECTINTERVAL, val, val, "java.lang.Integer");
        setProperty(cd, envProp3);

        val = ""+MdbContainerProps.getReconnectMaxRetries();
        ConnectorConfigProperty  envProp4 = new ConnectorConfigProperty  (
            RECONNECTATTEMPTS, val, val, "java.lang.Integer");
        setProperty(cd, envProp4);

        String integrationMode = getJmsService().getType();
        boolean lazyInit = Boolean.valueOf(getJmsHost().getLazyInit());
        val= "true";
        if (EMBEDDED.equals(integrationMode) && lazyInit)
        val = "false";
        doBind = Boolean.valueOf(val);
        ConnectorConfigProperty  envProp5 = new ConnectorConfigProperty  (
            MQ_PORTMAPPER_BIND, val, val, "java.lang.Boolean");
        setProperty(cd, envProp5);


    // The above properties will be set in ConnectorDescriptor and
    // will be bound in JNDI. This will be available to appclient
    // and standalone client.
    }

    private void setAddressList() throws ConnectorRuntimeException {
        //@Siva: Enhance setting AddressList. [Ignore this machines jms-host while
        //constructing addresslist]
        try {
            //ConfigContext ctx = ApplicationServer.getServerContext().getConfigContext();
            JmsService jmsService = (JmsService)Globals.get(JmsService.class);
            //ServerBeansFactory.getJmsServiceBean(ctx);
            setConnectionURL(jmsService, urlList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.loadRAConfiguration();
    }

    //This is a MQ workaround. In PE, when the broker type is
    //EMBEDDED or LOCAL, do not set the addresslist, else
    //MQ RA assumes that there are two URLs and fails (EE limitation).
    private void setConnectionURL(JmsService jmsService, MQAddressList urlList) {
        ConnectorDescriptor cd = super.getDescriptor();
        String val = urlList.toString();
        if (val != null) {
            _logger.log(Level.INFO, "jms.connection.url", val);
            ConnectorConfigProperty  envProp1 = new ConnectorConfigProperty  (
               CONNECTION_URL, val, val, "java.lang.String");
            setProperty(cd, envProp1);
        }
    }

    private void setJmsServiceProperties(JmsService service) throws
                                         ConnectorRuntimeException {
        JmsRaUtil jmsraUtil = new JmsRaUtil(service);
        jmsraUtil.setupAddressList();
        urlList = jmsraUtil.getUrlList();
        addressList = urlList.toString();
        //todo: debug info need to remove log statement in production
        _logger.log(Level.INFO, "addresslist.setjmsservice.provider", addressList);
        ConnectorDescriptor cd = super.getDescriptor();
        setConnectionURL(service, urlList);

        String val = ""+jmsraUtil.getReconnectEnabled();
        ConnectorConfigProperty  envProp2 = new ConnectorConfigProperty  (
            RECONNECTENABLED, val, val, "java.lang.Boolean");
        setProperty(cd, envProp2);

        //convert to milliseconds
        int newval = (new Integer(jmsraUtil.getReconnectInterval())).intValue() * 1000;
        val = "" + newval;
        ConnectorConfigProperty  envProp3 = new ConnectorConfigProperty  (
            RECONNECTINTERVAL, val, val, "java.lang.Integer");
        setProperty(cd, envProp3);

        val = ""+jmsraUtil.getReconnectAttempts();
        ConnectorConfigProperty  envProp4 = new ConnectorConfigProperty  (
            RECONNECTATTEMPTS, val, val, "java.lang.Integer");
        setProperty(cd, envProp4);

        val = ""+jmsraUtil.getAddressListBehaviour();
        ConnectorConfigProperty  envProp5 = new ConnectorConfigProperty  (
            ADRLIST_BEHAVIOUR, val, val, "java.lang.String");
        setProperty(cd, envProp5);

        val = ""+jmsraUtil.getAddressListIterations();
        ConnectorConfigProperty  envProp6 = new ConnectorConfigProperty  (
            ADRLIST_ITERATIONS, val, val, "java.lang.Integer");
        setProperty(cd, envProp6);

        boolean useExternal = shouldUseExternalRmiRegistry(jmsraUtil);
        val = Boolean.valueOf(useExternal).toString();
        ConnectorConfigProperty  envProp7 = new ConnectorConfigProperty  (
            USEEXTERNALRMIREGISTRY, val, val, "java.lang.Boolean");
        setProperty(cd, envProp7);

        _logger.log(Level.FINE, "Start RMI registry set as "+ val);
        //If MQ RA needs to use AS RMI Registry Port, then set
        //the RMI registry port, else MQ RA uses its default RMI
        //Registry port  [as of now 1099]
        String configuredRmiRegistryPort = null ;
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
            ConnectorConfigProperty  envProp8 = new ConnectorConfigProperty  (
                RMIREGISTRYPORT, val, val, "java.lang.Integer");
            setProperty(cd, envProp8);
            _logger.log(Level.FINE, "RMI registry port set as "+ val);
        } else {
            _logger.log(Level.WARNING, "invalid.rmi.registy.port");
        }
    }

    /*
     * Checks if AS RMI registry is started and available for use.
     */
    private boolean shouldUseExternalRmiRegistry (JmsRaUtil jmsraUtil) {
        boolean useExternalRmiRegistry = ( !isASRmiRegistryPortAvailable(jmsraUtil) );
        //System.out.println("========== " + useExternalRmiRegistry);
        return useExternalRmiRegistry;
    }

    /**
     * This method should return a unique and unused port , so that
     * the broker can use this to start its Rmi registry.
     * Used only for LOCAL mode
     */
    private String getUniqueRmiRegistryPort() {
    int mqrmiport  = DEFAULTRMIREGISTRYPORT;
    try {
        String configuredport = System.getProperty(BROKER_RMI_PORT);
        if (configuredport != null) {
            mqrmiport = Integer.parseInt(configuredport);
        } else {
            mqrmiport = Integer.parseInt(brkrPort)
                                    + BROKERRMIPORTOFFSET;
        }
    } catch (Exception e) {
        ;
    }
    return "" + mqrmiport;
    }

    /**
     * Get the AS RMI registry port for MQ RA to use.
     */
    private String getRmiRegistryPort() {
        String val = null;
        if (MQRmiPort != null && !MQRmiPort.trim().equals("")){
            return MQRmiPort;
        } else {
            String configuredPort = null;
            try {
                configuredPort = getConfiguredRmiRegistryPort();
            } catch (Exception ex) {
                _logger.log(Level.WARNING, ex.getLocalizedMessage());
                _logger.log(Level.FINE, "Exception while getting configured rmi " +
                                                 "registry port", ex);
            }
            if (configuredPort != null) {
                return configuredPort;
            }

            //Finally if DAS and configured port doesn't work, return DAS'
            //RMI registry port as a fallback option.

            //if (ResourcesUtil.isDAS()) {
            if(isDAS())
                    return DASRMIPORT;
        }
        return val;
    }

    private boolean isDAS()
    {
        return SystemPropertyConstants.DAS_SERVER_NAME.equals(getServerContext().getInstanceName());
       /*List serversList =  getServers().getServer();
       for (int i =0; i < serversList.size(); i++){
           Server aserver = (Server) serversList.get(i);
           if (getServerContext().getInstanceName().equals(aserver.getName()))
                return (aserver.getNode() == null);
       } */
        //return false;
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

    private JmxConnector getJmxConnector() throws Exception{
       List jmxConnectors = getAdminService().getJmxConnector();
       String sysJmsConnectorName = getAdminService().getSystemJmxConnectorName();
      if (jmxConnectors != null)
      {
          for (int i=0; i < jmxConnectors.size(); i++){
            if (sysJmsConnectorName.equals(((JmxConnector)jmxConnectors.get(i)).getName()))
                  return (JmxConnector)jmxConnectors.get(i);
          }
      }
          return null;
    }

    private boolean isASRmiRegistryPortAvailable(JmsRaUtil jmsraUtil) {
         logFine("isASRmiRegistryPortAvailable - JMSService Type:" + jmsraUtil.getJMSServiceType());
         //If JMSServiceType is LOCAL or REMOTE, then we need not ask the MQ RA to use the
         //AS RMI Registry. So the check below is not necessary.
         if (jmsraUtil.getJMSServiceType().equals(REMOTE)
            || jmsraUtil.getJMSServiceType().equals(LOCAL)) {
             return false;
         }

        try {
            JmxConnector jmxConnector = getJmxConnector();
            if (!"true".equals(jmxConnector.getEnabled()))
                return false;

            if ("true".equals(jmxConnector.getSecurityEnabled()))
                return false;

           //Attempt to detect JMXStartupService for RMI registry
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

            String name = "rmi://"+getConfiguredRmiRegistryHost() + ":" + getConfiguredRmiRegistryPort() + "/jmxrmi";
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
            _logger.fine("Failed to detect JMX RMI Registry: " + e.getMessage());
            return false;
        }
    }

    private void setProperty(ConnectorDescriptor cd, ConnectorConfigProperty  envProp){
        cd.removeConfigProperty(envProp);
        cd.addConfigProperty(envProp);
    }


    private void rebindDescriptor() throws ConnectorRuntimeException {
        try {
            String descriptorJNDIName = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForDescriptor(super.getModuleName());
            //GlassfishNamingManager nm = connectorRuntime.getNamingManager();
            nm.publishObject(descriptorJNDIName, super.getDescriptor(), true);
        //com.sun.enterprise.Switch.getSwitch().getNamingManager().        publishObject( descriptorJNDIName, super.getDescriptor(), true);
    } catch (javax.naming.NamingException ne) {
        ConnectorRuntimeException cre = new ConnectorRuntimeException (ne.getMessage());
        throw (ConnectorRuntimeException) cre.initCause(ne);
    }
    }

    /**
     * This is a temporay solution for obtaining all the MCFs
     * corresponding to a JMS RA pool, this is to facilitate the
     * recovery process where the XA resources of all RMs in the
     * broker cluster are required. Should be removed when a permanent
     * solutuion is available from the broker.
     * @param cpr <code>ConnectorConnectionPool</code> object
     * @param loader Class Loader.
     */
   public ManagedConnectionFactory [] createManagedConnectionFactories
        (com.sun.enterprise.connectors.ConnectorConnectionPool cpr,
         ClassLoader loader) {
           _logger.log(Level.FINE,"RECOVERY : Entering createMCFS in AJMSRA");
        ArrayList mcfs = new ArrayList();
            if (getAddressListCount() < 2) {
        mcfs.add(createManagedConnectionFactory(cpr,loader));
        _logger.log(Level.FINE,"Brokers are not clustered,So doing normal recovery");
        } else {
        String addlist = null;
           Set s = cpr.getConnectorDescriptorInfo().getMCFConfigProperties();
            Iterator tmpit = s.iterator();
            while (tmpit.hasNext()) {
                ConnectorConfigProperty  prop = (ConnectorConfigProperty ) tmpit.next();
                String propName = prop.getName();
                if (propName.equalsIgnoreCase("imqAddressList") || propName.equalsIgnoreCase("Addresslist")) {
            addlist = prop.getValue();
        }
        }
        StringTokenizer tokenizer = null;
        if ((addlist == null)
        || (addlist.trim().equalsIgnoreCase("localhost"))) {
            tokenizer = new StringTokenizer(addressList, ",");
        }else {
                tokenizer = new StringTokenizer(addlist, ",");
        }
       _logger.log(Level.FINE, "No of addresses found " +
            tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
        String brokerurl = tokenizer.nextToken();
            ManagedConnectionFactory mcf = super.
                    createManagedConnectionFactory(cpr, loader);
                Iterator it = s.iterator();
                while (it.hasNext()) {
                    ConnectorConfigProperty  prop = (ConnectorConfigProperty ) it.next();
                    String propName = prop.getName();
         String propValue = prop.getValue();
                 if (propName.startsWith("imq") && propValue != "") {
                  try {
                     Method meth = mcf.getClass().getMethod
                           (SETTER, new  Class[] {java.lang.String.class,
                              java.lang.String.class});
                    if (propName.trim().equalsIgnoreCase("imqAddressList")){
                             meth.invoke(mcf, new Object[] {prop.getName(),brokerurl});
            } else {
                             meth.invoke(mcf, new Object[] {prop.getName(),prop.getValueObject()});
            }
                   } catch (NoSuchMethodException ex) {
                       _logger.log(Level.WARNING, "no.such.method",
                           new Object[] {SETTER, mcf.getClass().getName()});
                   } catch (Exception ex) {
                       _logger.log(Level.SEVERE, "error.execute.method",
                           new Object[] {SETTER, mcf.getClass().getName()});
           }
        }
              }
              ConnectorConfigProperty  addressProp3 = new ConnectorConfigProperty  (ADDRESSLIST, brokerurl,"Address List",
                            "java.lang.String");
            //todo: need to remove log statement
          _logger.log(Level.INFO, "addresslist", brokerurl);

          HashSet addressProp = new HashSet();
        addressProp.add(addressProp3);
          SetMethodAction setMethodAction =
            new SetMethodAction(mcf,addressProp);
          try {
                     setMethodAction.run();
        } catch (Exception e) {
            ;
        }
        mcfs.add(mcf);
    }
    }
    return (ManagedConnectionFactory [])mcfs.toArray(new ManagedConnectionFactory[0]);
    }

     protected ManagedConnectionFactory instantiateMCF(final String mcfClass, final ClassLoader loader)
            throws Exception {
            ManagedConnectionFactory mcf = null;
            if (moduleName_.equals(ConnectorRuntime.DEFAULT_JMS_ADAPTER)) {
                Object tmp = AccessController.doPrivileged(
                        new PrivilegedExceptionAction() {
                            public Object run() throws Exception{
                                return instantiateManagedConnectionFactory(mcfClass, loader);
                            }
                        });
                mcf = (ManagedConnectionFactory) tmp;
            }
           return mcf;
     }

    private ManagedConnectionFactory instantiateManagedConnectionFactory (final String mcfClass, final ClassLoader loader) throws Exception{
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
     */
    public ManagedConnectionFactory createManagedConnectionFactory
                  (com.sun.enterprise.connectors.ConnectorConnectionPool cpr,
                   ClassLoader loader) {
        ManagedConnectionFactory mcf =
            super.createManagedConnectionFactory(cpr, loader);
        if ( mcf != null ) {
            Set s = cpr.getConnectorDescriptorInfo().getMCFConfigProperties();
            Iterator it = s.iterator();
            while (it.hasNext()) {
                ConnectorConfigProperty  prop = (ConnectorConfigProperty ) it.next();
                String propName = prop.getName();

                // If the property has started with imq, then it should go to
                // setProperty(String,String) method.
                if (propName.startsWith("imq") && prop.getValue() != "") {
                    try {
                        Method meth = mcf.getClass().getMethod
                        (SETTER, new  Class[] {java.lang.String.class,
                                               java.lang.String.class});
                        meth.invoke(mcf, new Object[] {prop.getName(),
                                                        prop.getValueObject()});
                    } catch (NoSuchMethodException ex) {
                        _logger.log(Level.WARNING, "no.such.method",
                        new Object[] {SETTER, mcf.getClass().getName()});
                    } catch (Exception ex) {
                        _logger.log(Level.SEVERE, "error.execute.method",
                        new Object[] {SETTER, mcf.getClass().getName()});
                    }
            }
            }

        //CR 6591307- Fix for properties getting overridden when setRA is called. Resetting the  properties if the RA is the JMS RA
        String moduleName = this.getModuleName();
        if(ConnectorAdminServiceUtils.isJMSRA(moduleName))
        {
            try {
                Set configProperties = cpr.getConnectorDescriptorInfo().getMCFConfigProperties();
                Object[] array = configProperties.toArray();
                for (int i =0; i < array.length; i++)
                {
                      if (array[i] instanceof ConnectorConfigProperty )
                      {
                          ConnectorConfigProperty  property = (ConnectorConfigProperty ) array[i];
                          if (ActiveJmsResourceAdapter.ADDRESSLIST.equals(property.getName())){
                              if (property.getValue() == null || "".equals(property.getValue()) || "localhost".equals(property.getValue())){
                                  _logger.log(Level.FINE,"raraddresslist.default.value", property.getValue());
                                   configProperties.remove(property);
                              }
                          }
                      }
                }
                SetMethodAction setMethodAction = new SetMethodAction(mcf,
                        configProperties);
                setMethodAction.run();
            }catch (Exception Ex) {
             final String mcfClass = cpr.getConnectorDescriptorInfo().
                getManagedConnectionFactoryClass();
            _logger.log(Level.WARNING,"rardeployment.mcfcreation_error",
                    new Object[]{mcfClass, Ex.getMessage()});
            _logger.log(Level.FINE,"rardeployment.mcfcreation_error",
                    Ex);
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
     */
    public void updateMDBRuntimeInfo(EjbMessageBeanDescriptor descriptor_,
                                     BeanPoolDescriptor poolDescriptor) throws ConnectorRuntimeException{

        String jndiName = descriptor_.getJndiName();
        if (jndiName == null || "".equals(jndiName)) {
          MessageDestinationDescriptor destDescriptor = descriptor_.getMessageDestination();
          if (destDescriptor != null)
            jndiName = destDescriptor.getJndiName();
        }

        String destinationLookup = descriptor_.getActivationConfigValue("destinationLookup");
        String destinationProp = descriptor_.getActivationConfigValue("destination");

        if (destinationLookup == null &&  destinationProp == null && (jndiName == null || "".equals(jndiName))) {
            _logger.log(Level.SEVERE, "MDB destination not specified.");
            String msg = sm.getString("ajra.error_in_dd");
            throw new ConnectorRuntimeException(msg);
        }

        String resourceAdapterMid = ConnectorRuntime.DEFAULT_JMS_ADAPTER;

        descriptor_.setResourceAdapterMid(resourceAdapterMid);

        if (destinationLookup == null && destinationProp == null) {
            String appName = descriptor_.getApplication().getAppName();
            String moduleName = ConnectorsUtil.getModuleName(descriptor_);

            JMSDestinationDefinitionDescriptor destination = getJMSDestinationFromDescriptor(jndiName, descriptor_);
            String destName = null;
            if (isValidDestination(destination)) {
                destName = destination.getDestinationName();
            } else {
                destName = getPhysicalDestinationFromConfiguration(jndiName, appName, moduleName);
            }
    
            //1.3 jndi-name ==> 1.4 setDestination
            descriptor_.putRuntimeActivationConfigProperty(
                    new EnvironmentProperty(DESTINATION,
                            destName, null));
    
    
            //1.3 (standard) destination-type == 1.4 setDestinationType
            //XXX Do we really need this???
            if (descriptor_.getDestinationType() != null &&
                    !"".equals(descriptor_.getDestinationType())) {
                descriptor_.putRuntimeActivationConfigProperty(
                        new EnvironmentProperty(DESTINATION_TYPE,
                                descriptor_.getDestinationType(), null));
                _logger.log(Level.INFO, "endpoint.determine.destinationtype", new
                        Object[]{descriptor_.getDestinationType(), jndiName, descriptor_.getName()});
            } else if (isValidDestination(destination) &&
                    ConnectorConstants.DEFAULT_JMS_ADAPTER.equals(destination.getResourceAdapter())) {
                descriptor_.putRuntimeActivationConfigProperty(
                        new EnvironmentProperty(DESTINATION_TYPE,
                                destination.getInterfaceName(), null));
                _logger.log(Level.INFO, "endpoint.determine.destinationtype", new
                        Object[]{destination.getInterfaceName(), destination.getName(), descriptor_.getName()});
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
                    if(aor != null && ConnectorConstants.DEFAULT_JMS_ADAPTER.equals(aor.getResAdapter())){
                        descriptor_.putRuntimeActivationConfigProperty(
                                new EnvironmentProperty(DESTINATION_TYPE,
                                        aor.getResType(), null));
                        _logger.log(Level.INFO, "endpoint.determine.destinationtype", new
                                Object[]{aor.getResType() , aor.getJndiName() , descriptor_.getName()});
                    }
    
                } catch (Exception e) {
    
                }
            }
        }


        //1.3 durable-subscription-name == 1.4 setSubscriptionName
        descriptor_.putRuntimeActivationConfigProperty(
                new EnvironmentProperty(SUBSCRIPTION_NAME,
                        descriptor_.getDurableSubscriptionName(), null));

        String mdbCF = null;
    try {
        mdbCF = descriptor_.getMdbConnectionFactoryJndiName();
    } catch(NullPointerException ne ) {
        // Dont process connection factory.
    }

        if (mdbCF != null && mdbCF != "") {
        setValuesFromConfiguration(mdbCF, descriptor_);
        }

        // a null object is passes as a PoolDescriptor during recovery.
        // See com/sun/enterprise/resource/ResourceInstaller

        if (poolDescriptor != null) {
        descriptor_.putRuntimeActivationConfigProperty
            (new EnvironmentProperty (MAXPOOLSIZE, ""+
                 poolDescriptor.getMaxPoolSize(),"", "java.lang.Integer" ));
        descriptor_.putRuntimeActivationConfigProperty
            (new EnvironmentProperty (MINPOOLSIZE,""+
                 poolDescriptor.getSteadyPoolSize(),"", "java.lang.Integer"));
        descriptor_.putRuntimeActivationConfigProperty
            (new EnvironmentProperty (RESIZECOUNT,""+
                 poolDescriptor.getPoolResizeQuantity(),"", "java.lang.Integer"));
        descriptor_.putRuntimeActivationConfigProperty
            (new EnvironmentProperty (RESIZETIMEOUT,""+
                 poolDescriptor.getPoolIdleTimeoutInSeconds(),"", "java.lang.Integer"));
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
         if ((descriptor_.getActivationConfigValue
                          (REDELIVERYCOUNT) == null)
                    && (descriptor_.getActivationConfigValue
                                (LOWERCASE_REDELIVERYCOUNT) == null))  {
             descriptor_.putRuntimeActivationConfigProperty
                 (new EnvironmentProperty (REDELIVERYCOUNT,""+
                         MdbContainerProps.getMaxRuntimeExceptions(),"", "java.lang.Integer"));
         }
        }

        //Set SE/EE specific MQ-RA ActivationSpec properties
        try {
            boolean clustered = isClustered();
            logFine("Are we in a Clustered contained ? " + clustered);
            if (clustered)
                setClusterActivationSpecProperties(descriptor_);
        } catch (Exception e) {
            ConnectorRuntimeException crex = new ConnectorRuntimeException(e.getMessage());
            throw (ConnectorRuntimeException)crex.initCause(e);
        }
    }

    /**
     * Set SE/EE specific MQ-RA ActivationSpec properties
     * @param descriptor_
     * @throws Exception
     */
    private void setClusterActivationSpecProperties(EjbMessageBeanDescriptor
                    descriptor_) throws Exception {
        //Set MDB Identifier in a clustered instance.
        descriptor_.putRuntimeActivationConfigProperty(new
                        EnvironmentProperty(MDBIDENTIFIER,""+
                        getMDBIdentifier(descriptor_),"MDB Identifier",
                        "java.lang.String"));
        logFine("CLUSTERED instance - setting MDB identifier as" +
                        getMDBIdentifier(descriptor_));

    }

    /**
     * Gets the MDBIdentifier for the message bean endpoint
     * @param descriptor_
     * @return
     * @throws Exception
     */
    private String getMDBIdentifier(EjbDescriptor descriptor_) throws Exception {
        return getDomainName() + SEPARATOR + getClusterName() + SEPARATOR + descriptor_.getUniqueId() ;
    }

    private String getPhysicalDestinationFromConfiguration(String logicalDest, String appName, String moduleName)
            throws ConnectorRuntimeException {
        Property ep = null;
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
        } catch(Exception ce) {
        String msg = sm.getString("ajra.err_getting_dest", logicalDest);
        ConnectorRuntimeException cre = new ConnectorRuntimeException( msg );
        cre.initCause( ce );
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
        if ((rootDeploymentDescriptor != ejbBundleDescriptor) && (rootDeploymentDescriptor instanceof WebBundleDescriptor)) {
            return true;
        }
        return false;
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
                if (jndiName.equals(((JMSDestinationDefinitionDescriptor) descriptor).getName())) {
                    return (JMSDestinationDefinitionDescriptor) descriptor;
                }
            }
        }
        return null;
    }

    private void setValuesFromConfiguration(String cfName, EjbMessageBeanDescriptor

                                     descriptor_) throws ConnectorRuntimeException{

        //todo: need to enable
        List <Property> ep = null;
        try {
            /*Resources rbeans = getAllResources();//ServerBeansFactory.getDomainBean(ctx).getResources();
            ConnectorResource res = (ConnectorResource)
                             rbeans.getResourceByName(ConnectorResource.class, cfName);*/
            String appName = descriptor_.getApplication().getAppName();
            String moduleName = ConnectorsUtil.getModuleName(descriptor_);
            ConnectorResource res = (ConnectorResource)
                    ResourcesUtil.createInstance().getResource(cfName, appName, moduleName, ConnectorResource.class);
        if (res == null) {
            String msg = sm.getString("ajra.mdb_cf_not_created", cfName);
        throw new ConnectorRuntimeException(msg);
        }

        ConnectorConnectionPool ccp = (ConnectorConnectionPool)
             ResourcesUtil.createInstance().getResource(res.getPoolName(), appName, moduleName, ConnectorConnectionPool.class);
            //rbeans.getResourceByName(ConnectorConnectionPool.class, res.getPoolName());

        ep = ccp.getProperty();
        } catch(Exception ce) {
        String msg = sm.getString("ajra.mdb_cf_not_created", cfName);
        ConnectorRuntimeException cre = new ConnectorRuntimeException( msg );
        cre.initCause( ce );
            throw cre;
        }

        if (ep == null) {
        String msg = sm.getString("ajra.cannot_find_phy_dest");
            throw new ConnectorRuntimeException( msg );
        }

    for (int i=0; i < ep.size(); i++) {
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
        //if (_logger.isLoggable(Level.FINE)){
            _logger.fine(s);
        //}
    }

    public int getAddressListCount() {
    StringTokenizer tokenizer = null;
    int count = 1;
    if (addressList != null) {
            tokenizer = new StringTokenizer(addressList, ",");
        count = tokenizer.countTokens();
    }
    logFine("Address list count is " + count);
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
    public boolean initializeService(){
         try {
             String module = ConnectorConstants.DEFAULT_JMS_ADAPTER;
             String loc = ConnectorsUtil.getSystemModuleLocation(module);
             ConnectorRuntime connectorRuntime = connectorRuntimeProvider.get();
             connectorRuntime.createActiveResourceAdapter(loc, module, null);
             return true;
               } catch (ConnectorRuntimeException e) {
                   e.printStackTrace();
                   //_logger.log(Level.WARNING, "Failed to start JMS RA");
                   e.printStackTrace();
                return false;
             }
    }

    public void handleRequest(SelectableChannel selectableChannel){
        SocketChannel socketChannel = null;
        if (selectableChannel instanceof SocketChannel) {
             socketChannel = (SocketChannel)selectableChannel;
             //PortMapperClientHandler  handler = null;
            try{
                Class c = resourceadapter_.getClass();
                Method m = c.getMethod("getPortMapperClientHandler", null);
                //handler = (PortMapperClientHandler) m.invoke(resourceadapter_, null);
                Object handler = m.invoke(resourceadapter_, null);
                m = handler.getClass().getMethod("handleRequest", SocketChannel.class);
                        //((PortMapperClientHandler)ra_).handleRequest(socketChannel);
                m.invoke(handler, socketChannel);
            }catch (Exception ex){
                    String message = sm.getString("error.invoke.portmapper", ex.getLocalizedMessage());
                   throw new RuntimeException (message, ex);
            }

            //handler.handleRequest(socketChannel);

        } else {
                 throw new IllegalArgumentException(sm.getString("invalid.socket.channel"));
               }
    }

    public void setMasterBroker(String newMasterBroker){
        try{
                Class c = resourceadapter_.getClass();
                Method m = c.getMethod("setMasterBroker", String.class);
                m.invoke(resourceadapter_, newMasterBroker);
                _logger.log(Level.INFO, "set.master.broker.success", newMasterBroker);

            }catch (Exception ex){
                   //throw new RuntimeException ("Error invoking PortMapperClientHandler.handleRequest. Cause - " + ex.getMessage(), ex);
                _logger.log(Level.WARNING, "set.master.broker.fail", new Object[]{newMasterBroker, ex.getMessage()});
        }
    }
    protected void setClusterBrokerList(String brokerList){
        try{
            Class c = resourceadapter_.getClass();
            Method m = c.getMethod("setClusterBrokerList", String.class);
            m.invoke(resourceadapter_, brokerList);
             _logger.log(Level.INFO, "set.cluster.brokerlist.success", brokerList);

            }catch (Exception ex){
                   //throw new RuntimeException ("Error invoking PortMapperClientHandler.handleRequest. Cause - " + ex.getMessage(), ex);
                _logger.log(Level.WARNING, "set.cluster.brokerlist.fail", new Object[]{brokerList, ex.getMessage()});
        }
    }
}
