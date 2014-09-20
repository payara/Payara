/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.gms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.glassfish.api.logging.LogLevel;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.HealthHistory;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Clusters;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.ee.cms.core.AliveAndReadySignal;
import com.sun.enterprise.ee.cms.core.AliveAndReadyView;
import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.FailureNotificationActionFactory;
import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.FailureRecoverySignal;
import com.sun.enterprise.ee.cms.core.FailureSuspectedActionFactory;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GMSFactory;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationActionFactory;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.core.JoinNotificationActionFactory;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationActionFactory;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.PlannedShutdownActionFactory;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.ServiceProviderConfigurationKeys;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.impl.client.FailureNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.FailureRecoveryActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.FailureSuspectedActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.GroupLeadershipNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.JoinNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.JoinedAndReadyNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.MessageActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.PlannedShutdownActionFactoryImpl;
import com.sun.enterprise.mgmt.transport.NetworkUtility;
import com.sun.enterprise.mgmt.transport.grizzly.GrizzlyConfigConstants;
import com.sun.enterprise.util.io.ServerDirs;
import com.sun.logging.LogDomains;

/**
 * @author Sheetal.Vartak@Sun.COM
 */
@PerLookup
@Service()
public class GMSAdapterImpl implements GMSAdapter, PostConstruct, CallBack {

    //private static final Logger logger =
    //    LogDomains.getLogger(GMSAdapterImpl.class, LogDomains.GMS_LOGGER);
    
    private static final String BEGINS_WITH = "^";
    private static final String GMS_PROPERTY_PREFIX = "GMS_";
    private static final String GMS_PROPERTY_PREFIX_REGEXP = BEGINS_WITH + GMS_PROPERTY_PREFIX;

    private GroupManagementService gms;

    private final static String CORE = "CORE";
    private final static String SPECTATOR = "SPECTATOR";
    private final static String MEMBERTYPE_STRING = "MEMBER_TYPE";

    // all set in postConstruct
    private String instanceName = null;
    private boolean isDas = false;
    private Cluster cluster = null;
    private String clusterName = null;
    private Config clusterConfig = null;
    //private long joinTime = 0L;

    private ConcurrentHashMap<CallBack, JoinNotificationActionFactory> callbackJoinActionFactoryMapping =
            new ConcurrentHashMap<CallBack, JoinNotificationActionFactory>();
    private ConcurrentHashMap<CallBack, JoinedAndReadyNotificationActionFactory> callbackJoinedAndReadyActionFactoryMapping =
            new ConcurrentHashMap<CallBack, JoinedAndReadyNotificationActionFactory>();
    private ConcurrentHashMap<CallBack, FailureNotificationActionFactory> callbackFailureActionFactoryMapping =
            new ConcurrentHashMap<CallBack, FailureNotificationActionFactory>();
    private ConcurrentHashMap<CallBack, FailureSuspectedActionFactory> callbackFailureSuspectedActionFactoryMapping =
            new ConcurrentHashMap<CallBack, FailureSuspectedActionFactory>();
    private ConcurrentHashMap<CallBack, GroupLeadershipNotificationActionFactory> callbackGroupLeadershipActionFactoryMapping =
            new ConcurrentHashMap<CallBack, GroupLeadershipNotificationActionFactory>();
    private ConcurrentHashMap<CallBack, PlannedShutdownActionFactory> callbackPlannedShutdownActionFactoryMapping =
            new ConcurrentHashMap<CallBack, PlannedShutdownActionFactory>();
    private EventListener glassfishEventListener = null;
    private boolean aliveAndReadyLoggingEnabled = false;
    private boolean testFailureRecoveryHandler = false;

    @Inject
    Events events;

    @Inject
    ServerEnvironment env;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Server server;

    @Inject
    ServiceLocator habitat;

    @Inject
    Clusters clusters;

    @Inject
    Nodes nodes;

    @Inject
    Servers servers;

    private HealthHistory hHistory;

    @LoggerInfo(subsystem = "CLSTR", description="Group Management Service Adapter Logger", publish=true)
    private static final String GMS_LOGGER_NAME = "javax.enterprise.cluster.gms";


    @LogMessagesResourceBundle
    private static final String LOG_MESSAGES_RB = "org.glassfish.cluster.gms.LogMessages";

    static final Logger GMS_LOGGER = Logger.getLogger(GMS_LOGGER_NAME, LOG_MESSAGES_RB);

    //gmsservice.no.cluster.name=GMSAD1001: no clustername to lookup
    //GMSAD1001.diag.cause.1=Required information was not passed into method.
    //GMSAD1001.diag.check.1=File issue with all relevant information.
    @LogMessageInfo(message = "no clustername to lookup",
        level="SEVERE",
        cause="Required information was not passed into method.",
        action="File issue with all relevant information.")
    private static final String GMS_NO_CLUSTER_NAME="NCLS-CLSTR-10101";

    //gmsservice.multiple.adapter=GMSAD1002: Multiple gms-adapter service for cluster {0}
    //GMSAD1002.diag.cause.1=GMs module is being initialized more than once for the same cluster.
    //GMSAD1002.diag.check.1=File issue with all relevant information.
    @LogMessageInfo(message = "Multiple gms-adapter service for cluster {0}",
        level="SEVERE",
        cause="GMs module is being initialized more than once for the same cluster.",
        action="File issue with all relevant information.")
    private static final String GMS_MULTIPLE_ADAPTER="NCLS-CLSTR-10102";

    //gmsservice.nocluster.warning=GMSAD1003: GMS cannot initialize with unknown cluster
    //GMSAD1003.diag.cause.1=No cluster was found with this name in the domain configuration.
    //GMSAD1003.diag.check.1=Check that domain exists in domain.xml.
    @LogMessageInfo(message = "GMS cannot initialize with unknown cluster",
        level="WARNING",
        cause="No cluster was found with this name in the domain configuration.",
        action="Check that domain exists in domain.xml.")
    private static final String GMS_NO_CLUSTER_WARNING="NCLS-CLSTR-10103";

    //gmsservice.started=GMSAD1004: Started GMS for instance {0} in group {1}
    @LogMessageInfo(message = "Started GMS for instance {0} in group {1}", level="INFO")
    private static final String GMS_STARTED="NCLS-CLSTR-10104";


    //gmsservice.member.joined.group=GMSAD1005: Member {0} joined group {1}
    @LogMessageInfo(message = "Member {0} joined group {1}", level="INFO")
    private static final String GMS_JOINED="NCLS-CLSTR-10105";


    //gmsservice.alive.ready.signal=GMSAD1007: AliveAndReady for signal: {0} for member: {1} of group: {2} current:[{3}] previous:[{4}]
    @LogMessageInfo(message = "AliveAndReady for signal: {0} for member: {1} of group: {2} current:[{3}] previous:[{4}]", level="INFO")
    private static final String GMS_ALIVE_AND_READY="NCLS-CLSTR-10107";

    //gmsservice.server_shutdown.received=GMSAD1008: GMSAdapter for member: {0} group: {1} received GlassfishEventType: {2}
    @LogMessageInfo(message = "GMSAdapter for member: {0} group: {1} received GlassfishEventType: {2}", level="INFO")
    private static final String GMS_SERVER_SHUTDOWN_RECEIVED="NCLS-CLSTR-10108";


    //gmsexception.new.health.history=GMSAD1009: An exception occurred while creating the HealthHistory object: {0}
    //GMSAD1009.diag.cause.1=An unexpected exception occurred.
    //GMSAD1009.diag.check.1=See server log for more details.
    @LogMessageInfo(message = "An exception occurred while creating the HealthHistory object: {0}",
        level="WARNING",
        cause="An unexpected exception occurred.",
        action="See server log for more details.")
    private static final String GMS_EXCEPTION_NEW_HEALTH_HISTORY="NCLS-CLSTR-10109";


    //gmsexception.processing.config.props=GMSAD1010: An exception occurred while processing GMS configuration properties: {0}
    //GMSAD1010.diag.cause.1=An unexpected exception occurred.
    //GMSAD1010.diag.check.1=See server log for more details.
    @LogMessageInfo(message = "An exception occurred while processing GMS configuration properties: {0}",
        level="WARNING",
        cause="An unexpected exception occurred.",
        action="See server log for more details.")
    private static final String GMS_EXCEPTION_PROCESSING_CONFIG="NCLS-CLSTR-10110";


    //gmsexception.ignoring.property=GMSAD1011: Ignoring group-management-service property {0} with value of {1} due to {2}
    //# todo: can we remove this try/catch?
    //GMSAD1011.diag.cause.1=An illegal argument was passed into the Shoal GMS implementation.
    //GMSAD1011.diag.check.1=Check the server log file for more information from Shoal-GMS.
    @LogMessageInfo(message = "Ignoring group-management-service property {0} with value of {1} due to {2}",
        level="WARNING",
        cause="An illegal argument was passed into the Shoal GMS implementation.",
        action="Check the server log file for more information from Shoal-GMS.")
    private static final String GMS_EXCEPTION_IGNORING_PROPERTY="NCLS-CLSTR-10111";

    //gmsexception.cluster.property.error=GMSAD1012: Error processing cluster property:{0} value:{1} due to exception {2}
    //# todo: can we remove this try/catch?
    //GMSAD1012.diag.cause.1=An unexpected exception occurred.
    //GMSAD1012.diag.check.1=Check the server log file for more information from Shoal-GMS.
    @LogMessageInfo(message = "Error processing cluster property:{0} value:{1} due to exception {2}",
        level="WARNING",
        cause="An unexpected exception occurred.",
        action="Check the server log file for more information from Shoal-GMS.")
    private static final String GMS_EXCEPTION_CLUSTER_PROPERTY_ERROR="NCLS-CLSTR-10112";

    //gmsexception.cannot.get.group.module=GMSAD1013: Exception in getting GMS module for group {0}: {1}
    //GMSAD1013.diag.cause.1=There was a problem withing the GMS implementation.
    //GMSAD1013.diag.check.1=Check the server log file for more information from Shoal-GMS.
    @LogMessageInfo(message = "Exception in getting GMS module for group {0}: {1}",
        level="SEVERE",
        cause="An unexpected exception occurred.",
        action="Check the server log file for more information from Shoal-GMS.")
    private static final String GMS_EXCEPTION_CANNOT_GET_GROUP_MODULE="NCLS-CLSTR-10113";

    //gmsexception.update.health.history=GMSAD1014: An exception occurred while updating the instance health history table: {0}
    //GMSAD1014.diag.cause.1=An unexpected exception occurred.
    //GMSAD1014.diag.check.1=Check the log for Shoal-GMS exceptions.
    @LogMessageInfo(message = "An exception occurred while updating the instance health history table: {0}",
        level="WARNING",
        cause="An unexpected exception occurred.",
        action="Check the log file for more information from Shoal-GMS.")
    private static final String GMS_EXCEPTION_UPDATE_HEALTH_HISTORY="NCLS-CLSTR-10114";

    //gmsservice.failurerecovery.start.notification=GMSAD1015: start failure recovery callback for component: {0} failed member: {1}
    @LogMessageInfo(message = "start failure recovery callback for component: {0} failed member: {1}", level="INFO")
    private static final String GMS_FAILURERECOVERY_START="NCLS-CLSTR-10115";

    //gmsservice.failurerecovery.completed.notification=GMSAD016: complete failure recovery callback for component: {0} failed member: {1}
    @LogMessageInfo(message = "complete failure recovery callback for component: {0} failed member: {1}", level="INFO")
    private static final String GMS_FAILURE_RECOVERY_COMPLETED="NCLS-CLSTR-10116";

    //gmsservice.failed.to.start=GMSAD1017: GMS failed to start. See stack trace for additional information.
    @LogMessageInfo(message = "GMS failed to start. See stack trace for additional information.",
        level="SEVERE",
        cause="An unexpected exception occurred.",
        action="Check the log file for more information")
    private static final String GMS_FAILED_TO_START="NCLS-CLSTR-10117";

    //gmsservice.failed.to.start.unexpected=GMSAD1018: GMS failed to start due to a runtime exception. See stack trace for additional information.
    @LogMessageInfo(message = "GMS failed to start due to a runtime exception. See stack trace for additional information.",
        level="SEVERE",
        cause="An unexpected exception occurred.",
        action="Check the log file for more information"
    )
    private static final String GMS_FAILED_TO_START_UNEXCEPTED="NCLS-CLSTR-10118";

    //gmsservice.bind.int.address.invalid=GMSAD1019: GMS bind interface address {0} is invalid. Will use default value instead.
    //GMSAD1019.diag.cause.1=The specified bind interface address is not an active local address, so it cannot be used on this node.
    //GMSAD1019.diag.check.1=Check that you have specified the proper address. See server log for more details from GMS subsystem.
    @LogMessageInfo(message = "GMS bind interface address {0} is invalid. Will use default value instead.",
        level="SEVERE",
        cause="The specified bind interface address is not an active local address, so it cannot be used on this node.",
        action="Check that you have specified the proper address. See server log for more details from GMS subsystem.")
    private static final String GMS_BIND_INT_ADDRESS_INVALID="NCLS-CLSTR-10119";

    //gmsservice.listener.port.required=GMSAD1020: GMS listener port is required for cluster {0}. Will attempt to use default of {1}.
    @LogMessageInfo(message = "GMS listener port is required for cluster {0}. Will attempt to use default of {1}.", level="WARNING")
    private static final String GMS_LISTENER_PORT_REQUIRED="NCLS-CLSTR-10120";

    @Override
    public void postConstruct() {
    }

    AtomicBoolean initialized = new AtomicBoolean(false);
    AtomicBoolean initializationComplete = new AtomicBoolean(false);

    @Override
    public String getClusterName() {
        return clusterName;
    }

    @Override
    public boolean initialize(String clusterName) {
        if (initialized.compareAndSet(false, true)) {
            this.clusterName = clusterName;
            if (clusterName == null) {
                GMS_LOGGER.log(LogLevel.SEVERE, GMS_NO_CLUSTER_NAME);
                return false;
            }
            try {
                gms = GMSFactory.getGMSModule(clusterName);
            } catch (GMSException ge) {
                // ignore
            }
            if (gms != null) {
                GMS_LOGGER.log(LogLevel.SEVERE, GMS_MULTIPLE_ADAPTER,
                    clusterName);
                return false;
            }

            Domain domain = habitat.getService(Domain.class);
            instanceName = env.getInstanceName();
            isDas = env.isDas();
            cluster = server.getCluster();
            if (cluster == null && clusters != null) {
                // must be the DAS since it not direclty considered a member of cluster by domain.xml.
                // iterate over all clusters to find the cluster that has name passed in.
                for (Cluster clusterI : clusters.getCluster()) {
                    if (clusterName.compareTo(clusterI.getName()) == 0) {
                        cluster = clusterI;
                        break;
                    }
                }
            }
            if (cluster == null) {
                GMS_LOGGER.log(LogLevel.WARNING, GMS_NO_CLUSTER_WARNING);
                return false;       //don't enable GMS
            } else if (isDas) {
                // only want to do this in the case of the DAS
                initializeHealthHistory(cluster);
            }

            clusterConfig = domain.getConfigNamed(clusterName + "-config");
            if (GMS_LOGGER.isLoggable(LogLevel.CONFIG)) {
                GMS_LOGGER.log(LogLevel.CONFIG,
                    "clusterName=" + clusterName +
                    " clusterConfig=" + clusterConfig);
            }
            try {
                initializeGMS();
            } catch (GMSException e) {
                GMS_LOGGER.log(LogLevel.SEVERE, GMS_FAILED_TO_START, e);
                // prevent access to a malformed gms object.
                return false;

            // also ensure for any unchecked exceptions (such as NPE during initialization) during initialization
            // that the malformed gms object is not allowed to be accesssed through the gms adapter.
            } catch (Throwable t) {
                GMS_LOGGER.log(LogLevel.SEVERE, GMS_FAILED_TO_START_UNEXCEPTED, t);
                // prevent access to a malformed gms object.
                return false;
            }
            initializationComplete.set(true);
        }
        return initialized.get();
    }

    @Override
    public void complete() {
        initialized.compareAndSet(true, false);
        initializationComplete.compareAndSet(true, false);
        gms = null;
        GMSFactory.removeGMSModule(clusterName);
    }

    @Override
    public HealthHistory getHealthHistory() {
        checkInitialized();
        return hHistory;
    }

    private void initializeHealthHistory(Cluster cluster) {
        try {
            /*
             * Should not fail, but we need to make sure it doesn't
             * affect GMS just in case.
             */
            hHistory = new HealthHistory(cluster);
            Dom.unwrap(cluster).addListener(hHistory);
        } catch (Throwable t) {
            GMS_LOGGER.log(LogLevel.WARNING, GMS_EXCEPTION_NEW_HEALTH_HISTORY,
                t.getLocalizedMessage());
        }
    }

    private void readGMSConfigProps(Properties configProps) {
        configProps.put(MEMBERTYPE_STRING, isDas ? SPECTATOR : CORE);
        for (ServiceProviderConfigurationKeys key : ServiceProviderConfigurationKeys.values()) {
            String keyName = key.toString();
            try {
            switch (key) {
                case MULTICASTADDRESS:
                    if (cluster != null) {
                        String value = cluster.getGmsMulticastAddress();
                        if (value != null) {
                            configProps.put(keyName, value);
                        }
                    }
                    break;

                case MULTICASTPORT:
                    if (cluster != null) {
                        String value = cluster.getGmsMulticastPort();
                        if (value != null) {
                            configProps.put(keyName, value);
                        }
                    }
                    break;

                case FAILURE_DETECTION_TIMEOUT:
                    if (clusterConfig != null) {
                        String  value = clusterConfig.getGroupManagementService().getFailureDetection().getHeartbeatFrequencyInMillis();
                        if (value != null) {
                            configProps.put(keyName, value);
                        }
                    }
                    break;

                case FAILURE_DETECTION_RETRIES:
                    if (clusterConfig != null) {
                        String  value = clusterConfig.getGroupManagementService().getFailureDetection().getMaxMissedHeartbeats();
                        if (value != null) {
                            configProps.put(keyName, value);
                        }
                    }
                    break;

                case FAILURE_VERIFICATION_TIMEOUT:
                    if (clusterConfig != null) {
                        String  value = clusterConfig.getGroupManagementService().getFailureDetection().getVerifyFailureWaittimeInMillis();
                        if (value != null) {
                            configProps.put(keyName, value);
                        }
                    }
                    break;

                case DISCOVERY_TIMEOUT:
                    if (clusterConfig != null) {
                        String  value = clusterConfig.getGroupManagementService().getGroupDiscoveryTimeoutInMillis();
                        if (value != null) {
                            configProps.put(keyName, value);
                        }
                    }
                    break;

                case IS_BOOTSTRAPPING_NODE:
                    configProps.put(keyName, isDas ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
                    break;


                case BIND_INTERFACE_ADDRESS:
                    if (cluster != null) {
                            String value = cluster.getGmsBindInterfaceAddress();
                            if (value != null) {
                                    value = value.trim();
                            }
                            if (value != null && value.length() > 1 && value.charAt(0) != '$') {

                                    // todo: remove check for value length greater than 1.
                                    // this value could be anything from IPv4 address, IPv6 address, hostname, network interface name.
                                    // Only supported IPv4 address in gf v2.
                                    if (NetworkUtility.isBindAddressValid(value)) {
                                            configProps.put(keyName, value);
                                    } else {
                                            GMS_LOGGER.log(LogLevel.SEVERE,
                                                GMS_BIND_INT_ADDRESS_INVALID,
                                    value);
                                    }
                            }
                    }
                    break;

                case FAILURE_DETECTION_TCP_RETRANSMIT_TIMEOUT:
                    if (clusterConfig != null) {
                            String  value = clusterConfig.getGroupManagementService().getFailureDetection().getVerifyFailureConnectTimeoutInMillis();
                            if (value != null) {
                                    configProps.put(keyName, value);
                            }
                    }
                    break;

                case MULTICAST_POOLSIZE:
                case INCOMING_MESSAGE_QUEUE_SIZE :
                    // case MAX_MESSAGE_LENGTH:    todo uncomment with shoal-gms.jar with this defined is promoted.
                case FAILURE_DETECTION_TCP_RETRANSMIT_PORT:

                    if (clusterConfig != null) {
                            Property prop = clusterConfig.getGroupManagementService().getProperty(keyName);
                            if (prop == null) {
                                    if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
                                            GMS_LOGGER.log(LogLevel.FINE, String.format(
                                    "No config property found for %s",
                                    keyName));
                                    }
                                    break;
                            }
                            String value = prop.getValue().trim();
                            if (value != null) {
                                    configProps.put(keyName, value);
                            }
                            /*
                            int positiveint = 0;
                            try {
                                positiveint = Integer.getInteger(value);
                            } catch (Throwable t) {}

                            // todo
                            if (positiveint > 0) {
                                configProps.put(keyName, positiveint);
                            } // todo else log event that invalid value was provided.
                            */
                    }
                    break;

                    // These Shoal GMS configuration parameters are not supported to be set.
                    // Must place here or they will get flagged as not handled.
                case LOOPBACK:
                case VIRTUAL_MULTICAST_URI_LIST:
                    break;

                    // end unsupported Shoal GMS configuration parameters.


                default:
                    if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
                            GMS_LOGGER.log(LogLevel.FINE, String.format(
                            "service provider key %s ignored", keyName));
                    }
                    break;
            }  /* end switch over ServiceProviderConfigurationKeys enum */
            } catch (Throwable t) {
                GMS_LOGGER.log(LogLevel.WARNING, GMS_EXCEPTION_PROCESSING_CONFIG, t.getLocalizedMessage());
            }
        } /* end for loop over ServiceProviderConfigurationKeys */

        // check for Grizzly transport specific properties in GroupManagementService property list and then cluster property list.
        // cluster property is more specific than group-mangement-service, so allow cluster property to override group-management-service proeprty
        // if a GrizzlyConfigConstant property is in both list.
        List<Property> props = null;
        if (clusterConfig != null) {
            props = clusterConfig.getGroupManagementService().getProperty();
            for (Property prop : props) {
                String name = prop.getName().trim();
                String value = prop.getValue().trim();
                if (name == null || value == null) {
                    continue;
                }
                if (GMS_LOGGER.isLoggable(LogLevel.CONFIG)) {
                    GMS_LOGGER.log(LogLevel.CONFIG,
                        "processing group-management-service property name=" +
                            name + " value= " + value);
                }
                if (value.startsWith("${")) {
                    if (GMS_LOGGER.isLoggable(LogLevel.CONFIG)) {
                        GMS_LOGGER.log(LogLevel.CONFIG,
                            "skipping group-management-service property name=" +
                                name +
                                " since value is unresolved symbolic token=" +
                                value);
                    }
                } else {
                    if (GMS_LOGGER.isLoggable(LogLevel.CONFIG)) {
                        GMS_LOGGER.log(LogLevel.CONFIG,
                            "processing group-management-service property name=" +
                                name + " value= " + value);
                    }
                    if (name.startsWith(GMS_PROPERTY_PREFIX)) {
                        name = name.replaceFirst(GMS_PROPERTY_PREFIX_REGEXP, "");
                    }
                    configProps.put(name, value);
                    if (! validateGMSProperty(name)) {
                        GMS_LOGGER.log(LogLevel.WARNING, GMS_EXCEPTION_IGNORING_PROPERTY,
                                       new Object [] {name, value, ""} );
                    }
                }
            }
        }
        if (cluster != null) {
            props = cluster.getProperty();
            for (Property prop : props) {
                String name = prop.getName().trim();
                String value = prop.getValue().trim();
                if (name == null || value == null) {
                    continue;
                }
                if (GMS_LOGGER.isLoggable(LogLevel.CONFIG)) {
                    GMS_LOGGER.log(LogLevel.CONFIG,
                        "processing cluster property name=" + name +
                        " value= " + value);
                }
                if (value.startsWith("${")) {
                    if (GMS_LOGGER.isLoggable(LogLevel.CONFIG)) {
                        GMS_LOGGER.log(LogLevel.CONFIG,
                            "skipping cluster property name=" + name +
                            " since value is unresolved symbolic token=" +
                            value);
                    }
                } else {
                        if (name.startsWith(GMS_PROPERTY_PREFIX)) {
                            name = name.replaceFirst(GMS_PROPERTY_PREFIX_REGEXP, "");
                        }
                        // undocumented property for testing purposes.
                        // impossible to register handlers in a regular app before gms starts up.
                        if (name.compareTo("ALIVEANDREADY_LOGGING") == 0){
                            aliveAndReadyLoggingEnabled = Boolean.parseBoolean(value);
                        } else if (name.compareTo("LISTENER_PORT") == 0 ) {

                            // special case mapping.  Glassfish Cluster property GMS_LISTENER_PORT maps to Grizzly Config Constants TCPSTARTPORT and TCPENDPORT.
                            configProps.put(GrizzlyConfigConstants.TCPSTARTPORT.toString(), value);
                            configProps.put(GrizzlyConfigConstants.TCPENDPORT.toString(), value);
                        } else if (name.compareTo("TEST_FAILURE_RECOVERY") == 0) {
                            testFailureRecoveryHandler = Boolean.parseBoolean(value);
                        } else if (ServiceProviderConfigurationKeys
                            .DISCOVERY_URI_LIST.name().equals(name) &&
                            "generate".equals(value)) {

                            value = generateDiscoveryUriList();
                            configProps.put(name, value);
                        } else {
                            // handle normal case.  one to one mapping.
                            configProps.put(name, value);
                            GMS_LOGGER.log(LogLevel.CONFIG,
                        "processing cluster property name=" + name +
                        " value= " + value);
                            if (! validateGMSProperty(name)) {
                                GMS_LOGGER.log(LogLevel.WARNING, GMS_EXCEPTION_CLUSTER_PROPERTY_ERROR,
                                           new Object [] {name, value, ""} );
                            }
                        }
                }
            }
        }
    }

    /*
     * Get existing nodes based on cluster element in domain.
     * Then check for DAS address in das.properties. When the
     * list is set to 'generate' then the gms listener port
     * must also be specified. So the same port is used for
     * each cluster member.
     */
    private String generateDiscoveryUriList() {
        String clusterPort = null;

        Property gmsPortProp = cluster.getProperty("GMS_LISTENER_PORT");
        if (gmsPortProp == null ||
            gmsPortProp.getValue() == null ||
            gmsPortProp.getValue().trim().charAt(0) == '$') {

            clusterPort = "9090";
            GMS_LOGGER.log(LogLevel.WARNING, GMS_LISTENER_PORT_REQUIRED,
                new Object [] {cluster.getName(), clusterPort});
        } else {
            clusterPort = gmsPortProp.getValue();
            if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
                GMS_LOGGER.log(LogLevel.FINE, "will use gms listener port: " +
                    clusterPort);
            }
        }

        // get cluster member server refs
        Set<String> instanceNames = new HashSet<String>();
        if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
            GMS_LOGGER.log(LogLevel.FINE, String.format(
                "checking cluster.getServerRef() for '%s'",
                cluster.getName()));
        }
        for (ServerRef sRef : cluster.getServerRef()) {

            /*
             * When an instance (not DAS) starts up, it will add
             * its own address to the discovery list. This is ok
             * now. If we want to skip it, here's the place to
             * check.
             */
            if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
                GMS_LOGGER.log(LogLevel.FINE, String.format(
                    "adding server ref %s to set of instance names",
                    sRef.getRef()));
            }
            instanceNames.add(sRef.getRef());
        }

        StringBuilder sb = new StringBuilder();
        final String SEP = ",";
        final String scheme = "tcp://";

        // use server refs to find matching nodes
        for (String name : instanceNames) {
            Server server = servers.getServer(name);
            if (server != null) {
                if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
                    GMS_LOGGER.log(LogLevel.FINE, String.format(
                        "found server for name %s",
                        name));
                }
                Node node = nodes.getNode(server.getNodeRef());
                if (node != null) {
                    String host = scheme + node.getNodeHost() + ":" +
                        clusterPort;
                    if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
                        GMS_LOGGER.log(LogLevel.FINE, String.format(
                            "Adding host '%s' to discovery list", host));
                    }
                    sb.append(host).append(SEP);
                }
            }
        }

        // add das location from das.properties if needed
        if (server.isInstance()) {
            try {
                ServerDirs sDirs = new ServerDirs(env.getInstanceRoot());
                File dasPropsFile = sDirs.getDasPropertiesFile();
                if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
                    GMS_LOGGER.log(LogLevel.FINE, String.format(
                        "found das.props file at %s",
                        dasPropsFile.getAbsolutePath()));
                }
                Properties dasProps = getProperties(dasPropsFile);
                String host = scheme +
                    dasProps.getProperty("agent.das.host") +
                    ":" +
                    clusterPort;
                if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
                    GMS_LOGGER.log(LogLevel.FINE, String.format(
                        "adding '%s' from das.props file", host));
                }
                sb.append(host).append(SEP);
            } catch (IOException ioe) {
                GMS_LOGGER.log(LogLevel.WARNING, ioe.toString());
            }
        }

        // trim list if needed and return
        int lastCommaIndex = sb.lastIndexOf(SEP);
        if (lastCommaIndex != -1) {
            sb.deleteCharAt(lastCommaIndex);
        }
        if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
            GMS_LOGGER.log(LogLevel.FINE, String.format(
                "returning discovery list '%s'",
                sb.toString()));
        }
        return sb.toString();
    }

    final protected Properties getProperties(File propFile) throws IOException {
        Properties props = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propFile);
            props.load(fis);
            fis.close();
            fis = null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {}
            }
        }
        return props;
    }

    private boolean validateGMSProperty(String propertyName) {
        boolean result = false;
        Object key = null;
        try {
            key = GrizzlyConfigConstants.valueOf(propertyName);
            result = true;
        } catch (Throwable ignored) {}
        if (key == null) {
            try {
                key = ServiceProviderConfigurationKeys.valueOf(propertyName);
                result = true;
            } catch (Throwable ignored) {}
        }
        return key != null && result;
    }

    private void initializeGMS() throws GMSException{
        Properties configProps = new Properties();
        int HA_MAX_GMS_MESSAGE_LENGTH =  4 * (1024 * 1024)  + (2 * 1024);  // Default to 4 MB limit in glassfish.
        configProps.put(ServiceProviderConfigurationKeys.MAX_MESSAGE_LENGTH.toString(), Integer.toString(HA_MAX_GMS_MESSAGE_LENGTH));


        // read GMS configuration from domain.xml
        readGMSConfigProps(configProps);

        printProps(configProps);

        String memberType = (String) configProps.get(MEMBERTYPE_STRING);
        gms = (GroupManagementService) GMSFactory.startGMSModule(instanceName, clusterName,
                GroupManagementService.MemberType.valueOf(memberType), configProps);
        //remove GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER).setLevel(gmsLogLevel);
        GMSFactory.setGMSEnabledState(clusterName, Boolean.TRUE);
        if (gms != null) {
            try {
                registerJoinedAndReadyNotificationListener(this);
                registerJoinNotificationListener(this);
                registerFailureNotificationListener(this);
                registerPlannedShutdownListener(this);
                registerFailureSuspectedListener(this);

                //fix gf it 12905
                if (testFailureRecoveryHandler && ! env.isDas()) {

                    // this must be here or appointed recovery server notification is not printed out for automated testing.
                    registerFailureRecoveryListener("GlassfishFailureRecoveryHandlerTest", this);
                }

                glassfishEventListener = new org.glassfish.api.event.EventListener() {
                    public void event(Event event) {
                        if (gms == null) {
                            // handle cases where gms is not set and for some reason this handler did not get unregistered.
                            return;
                        }
                        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
                            GMS_LOGGER.log(LogLevel.INFO, GMS_SERVER_SHUTDOWN_RECEIVED,
                                       new Object[]{gms.getInstanceName(), gms.getGroupName(), event.name()});

                            // todo: remove these when removing the test register ones above.
                            removeJoinedAndReadyNotificationListener(GMSAdapterImpl.this);
                            removeJoinNotificationListener(GMSAdapterImpl.this);
                            removeFailureNotificationListener(GMSAdapterImpl.this);
                            removeFailureSuspectedListener(GMSAdapterImpl.this);
                            gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
                            removePlannedShutdownListener(GMSAdapterImpl.this);
                            events.unregister(glassfishEventListener);
                        } else if (event.is(EventTypes.SERVER_READY)) {
                             // consider putting following, includding call to joinedAndReady into a timertask.
                              // this time would give instance time to get its heartbeat cache updated by all running
                              // READY cluster memebrs
//                            final long MAX_WAIT_DURATION = 4000;
//
//                            long elapsedDuration = (joinTime == 0L) ? 0 : System.currentTimeMillis() - joinTime;
//                            long waittime = MAX_WAIT_DURATION - elapsedDuration;
//                            if (waittime > 0L && waittime <= MAX_WAIT_DURATION) {
//                                try {
//                                    GMS_LOGGER.info("wait " + waittime + " ms before signaling joined and ready");
//                                    Thread.sleep(waittime);
//                                } catch(Throwable t) {}
//                            }
//                          validateCoreMembers();
                            gms.reportJoinedAndReadyState();
                        }
                    }
                };
                events.register(glassfishEventListener);
                gms.join();
                //joinTime = System.currentTimeMillis();
                GMS_LOGGER.log(LogLevel.INFO, GMS_JOINED,
                    new Object [] {instanceName, clusterName});
            } catch (GMSException e) {
                // failed to start so unregister event listener that calls GMS.
                events.unregister(glassfishEventListener);
                throw e;
            }

            GMS_LOGGER.log(LogLevel.INFO, GMS_STARTED,
                new Object[] {instanceName, clusterName});

        } else throw new GMSException("gms object is null.");
    }

    private void printProps(Properties prop) {
        if (!GMS_LOGGER.isLoggable(LogLevel.CONFIG)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String key : prop.stringPropertyNames()) {
            sb.append(key).append(" = ").append(prop.get(key)).append("  ");
        }
        GMS_LOGGER.log(LogLevel.CONFIG,
            "Printing all GMS properties: ", sb.toString());
    }

    private void checkInitialized() {
        if( ! initialized.get() || ! initializationComplete.get())  {
            throw new IllegalStateException("GMSAdapter not properly initialized.");
        }
    }
    @Override
    public GroupManagementService getModule() {
        checkInitialized();
        return gms;
    }

    public GroupManagementService getGMS(String groupName) {
        //return the gms instance for that group
        try {
            return GMSFactory.getGMSModule(groupName);
        } catch (GMSException e) {
            GMS_LOGGER.log(LogLevel.SEVERE, GMS_EXCEPTION_CANNOT_GET_GROUP_MODULE,
                new Object [] {groupName , e.getLocalizedMessage()});
            return null;
        }
    }

    @Override
    public void processNotification(Signal signal) {
        if (GMS_LOGGER.isLoggable(LogLevel.FINE)) {
            GMS_LOGGER.log(LogLevel.FINE, "GMSService: Received a notification ",
                signal.getClass().getName());
        }
        try {
            /*
             * Should not fail, but we need to make sure it doesn't
             * affect GMS just in case. In the non-DAS case, hHistory
             * will always be null so we skip it. In the DAS case,
             * it shouldn't be null unless we've already seen an
             * error logged during construction.
             */
            if (hHistory != null) {
                hHistory.updateHealth(signal);
            }
        } catch (Throwable t) {
            GMS_LOGGER.log(LogLevel.WARNING, GMS_EXCEPTION_UPDATE_HEALTH_HISTORY,
                t.getLocalizedMessage());
        }
        // testing only.  one must set cluster property GMS_TEST_FAILURE_RECOVERY to true for the following to execute. */
        if (testFailureRecoveryHandler && signal instanceof FailureRecoverySignal) {
            FailureRecoverySignal frsSignal = (FailureRecoverySignal)signal;
            GMS_LOGGER.log(LogLevel.INFO, GMS_FAILURERECOVERY_START, new Object[]{frsSignal.getComponentName(), frsSignal.getMemberToken()});
            try {
                Thread.sleep(20 * 1000); // sleep 20 seconds. simulate wait time to allow instance to restart and do self recovery before another instance does it.
            } catch (InterruptedException ignored) {
            }
            GMS_LOGGER.log(LogLevel.INFO, GMS_FAILURE_RECOVERY_COMPLETED, new Object[]{frsSignal.getComponentName(), frsSignal.getMemberToken()});
        }
        if (this.aliveAndReadyLoggingEnabled) {
            if (signal instanceof JoinedAndReadyNotificationSignal ||
                signal instanceof FailureNotificationSignal ||
                signal instanceof PlannedShutdownSignal) {
                AliveAndReadySignal arSignal = (AliveAndReadySignal)signal;
                String signalSubevent = "";
                if (signal instanceof JoinedAndReadyNotificationSignal) {
                    JoinedAndReadyNotificationSignal jrsig = (JoinedAndReadyNotificationSignal)signal;
                    if (jrsig.getEventSubType() == GMSConstants.startupType.GROUP_STARTUP) {
                        signalSubevent = " Subevent: " + GMSConstants.startupType.GROUP_STARTUP;
                    } else if (jrsig.getRejoinSubevent() != null) {
                        signalSubevent = " Subevent: " + jrsig.getRejoinSubevent();
                    }
                }
                if (signal instanceof PlannedShutdownSignal) {
                    PlannedShutdownSignal pssig = (PlannedShutdownSignal)signal;
                    if (pssig.getEventSubType() == GMSConstants.shutdownType.GROUP_SHUTDOWN) {
                        signalSubevent = " Subevent:" + GMSConstants.shutdownType.GROUP_SHUTDOWN.toString();
                    }
                }
                AliveAndReadyView current = arSignal.getCurrentView();
                AliveAndReadyView previous = arSignal.getPreviousView();
                GMS_LOGGER.log(LogLevel.INFO, GMS_ALIVE_AND_READY,
                    new Object [] {
                        signal.getClass().getSimpleName() + signalSubevent,
                        signal.getMemberToken(),
                        signal.getGroupName(),
                        current,
                        previous
                    });
            }
        }
    }

    // each of the getModule(s) methods are temporary. see class-level comment.

    /**
     * Registers a JoinNotification Listener.
     *
     * @param callback processes GMS notification JoinNotificationSignal
     */
    @Override
    public void registerJoinNotificationListener(CallBack callback) {
        if (gms != null  && callback != null) {
            JoinNotificationActionFactory jnaf =  new JoinNotificationActionFactoryImpl(callback);
            gms.addActionFactory(jnaf);
            callbackJoinActionFactoryMapping.put(callback, jnaf);
        }
    }

    /**
     * Registers a JoinAndReadyNotification Listener.
     *
     * @param callback processes GMS notification JoinAndReadyNotificationSignal
     */
    @Override
    public void registerJoinedAndReadyNotificationListener(CallBack callback) {
        if (gms != null && callback != null) {
            JoinedAndReadyNotificationActionFactory jnaf =  new JoinedAndReadyNotificationActionFactoryImpl(callback);
            gms.addActionFactory(jnaf);
            callbackJoinedAndReadyActionFactoryMapping.put(callback, jnaf);
        }
    }

    /**
     * Register a listener for all events that represent a member has left the group.
     *
     * @param callback Signal can be either PlannedShutdownSignal, FailureNotificationSignal or JoinNotificationSignal(subevent Rejoin).
     */
    @Override
    public void registerMemberLeavingListener(CallBack callback) {
        if (gms != null && callback != null) {
            registerFailureNotificationListener(callback);
            registerPlannedShutdownListener(callback);
            registerJoinNotificationListener(callback);
        }
    }

    /**
     * Registers a PlannedShutdown Listener.
     *
     * @param callback processes GMS notification PlannedShutdownSignal
     */
    @Override
    public void registerPlannedShutdownListener(CallBack callback) {
        if (gms != null && callback != null) {
            PlannedShutdownActionFactory psaf = new PlannedShutdownActionFactoryImpl(callback);
            callbackPlannedShutdownActionFactoryMapping.put(callback, psaf);
            gms.addActionFactory(psaf);
        }
    }

    /**
     * Registers a FailureSuspected Listener.
     *
     * @param callback processes GMS notification FailureSuspectedSignal
     */
    @Override
    public void registerFailureSuspectedListener(CallBack callback) {
        if (gms != null) {
            FailureSuspectedActionFactory fsaf = new FailureSuspectedActionFactoryImpl(callback);
            callbackFailureSuspectedActionFactoryMapping.put(callback, fsaf);
            gms.addActionFactory(fsaf);
        }
    }

    /**
     * Registers a FailureNotification Listener.
     *
     * @param callback processes GMS notification FailureNotificationSignal
     */
    @Override
    public void registerFailureNotificationListener(CallBack callback) {
        if (gms != null) {
            FailureNotificationActionFactory fnaf = new FailureNotificationActionFactoryImpl(callback);
            callbackFailureActionFactoryMapping.put(callback, fnaf);
            gms.addActionFactory(fnaf);
        }
    }

    /**
     * Registers a FailureRecovery Listener.
     *
     * @param callback      processes GMS notification FailureRecoverySignal
     * @param componentName The name of the parent application's component that should be notified of selected for
     *                      performing recovery operations. One or more components in the parent application may
     *                      want to be notified of such selection for their respective recovery operations.
     */
    @Override
    public void registerFailureRecoveryListener(String componentName, CallBack callback) {
        if (gms != null) {
            gms.addActionFactory(componentName, new FailureRecoveryActionFactoryImpl(callback));
        }
    }

    /**
     * Registers a Message Listener.
     *
     * @param componentName   Name of the component that would like to consume
     *                        Messages. One or more components in the parent application would want to
     *                        be notified when messages arrive addressed to them. This registration
     *                        allows GMS to deliver messages to specific components.
     * @param messageListener processes GMS MessageSignal
     */
    @Override
    public void registerMessageListener(String componentName, CallBack messageListener) {
        if (gms != null) {
            gms.addActionFactory(new MessageActionFactoryImpl(messageListener), componentName);
        }
    }

    /**
     * Registers a GroupLeadershipNotification Listener.
     *
     * @param callback processes GMS notification GroupLeadershipNotificationSignal. This event occurs when the GMS masters leaves the Group
     *                 and another member of the group takes over leadership. The signal indicates the new leader.
     */
    @Override
    public void registerGroupLeadershipNotificationListener(CallBack callback) {
        if (gms != null) {
            gms.addActionFactory(new GroupLeadershipNotificationActionFactoryImpl(callback));
        }
    }

    @Override
    public void removeFailureRecoveryListener(String componentName) {
        if (gms != null) {
            gms.removeFailureRecoveryActionFactory(componentName);
        }
    }

    @Override
    public void removeMessageListener(String componentName){
        if (gms != null) {
            gms.removeMessageActionFactory(componentName);
        }
    }

    @Override
    public void removeFailureNotificationListener(CallBack callback){
        if (gms != null) {
            FailureNotificationActionFactory fnaf = callbackFailureActionFactoryMapping.remove(callback);
            if (fnaf != null) {
                gms.removeActionFactory(fnaf);
            }
        }
    }

    @Override
    public void removeFailureSuspectedListener(CallBack callback){
         if (gms != null) {
            FailureSuspectedActionFactory fsaf = callbackFailureSuspectedActionFactoryMapping.remove(callback);
            if (fsaf != null) {
                gms.removeFailureSuspectedActionFactory(fsaf);
            }
        }
    }

    @Override
    public void removeJoinNotificationListener(CallBack callback){
        if (gms != null) {
            JoinNotificationActionFactory jaf = callbackJoinActionFactoryMapping.get(callback);
            if (jaf != null)  {
                gms.removeActionFactory(jaf);
            }
        }
    }

    @Override
    public void removeJoinedAndReadyNotificationListener(CallBack callback){
        if (gms != null) {
            JoinedAndReadyNotificationActionFactory jaf = callbackJoinedAndReadyActionFactoryMapping.get(callback);
            if (jaf != null)  {
                gms.removeActionFactory(jaf);
            }
        }
    }

    @Override
    public void removePlannedShutdownListener(CallBack callback){
        if (gms != null) {
            PlannedShutdownActionFactory psaf = callbackPlannedShutdownActionFactoryMapping.remove(callback);
            if (psaf != null) {
                gms.removeActionFactory(psaf);
            }
        }
    }

    @Override
    public void removeGroupLeadershipLNotificationistener(CallBack callback){
         if (gms != null) {
            GroupLeadershipNotificationActionFactory glnf = callbackGroupLeadershipActionFactoryMapping.get(callback);
            if (glnf != null)  {
                gms.removeActionFactory(glnf);
            }
        }
    }

    @Override
    public void removeMemberLeavingListener(CallBack callback){
        removePlannedShutdownListener(callback);
        removeFailureNotificationListener(callback);
        removeJoinNotificationListener(callback);
    }

}
