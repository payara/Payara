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
//Portions Copyright 2017-2026 Payara Foundation and/or its affiliates

package org.glassfish.jms.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.jms.config.JmsHost;
import com.sun.enterprise.connectors.jms.config.JmsService;
import com.sun.enterprise.connectors.jms.system.ActiveJmsResourceAdapter;
import com.sun.enterprise.connectors.jms.system.MQAddressList;
import com.sun.enterprise.connectors.jms.util.JmsRaUtil;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;

import javax.management.Attribute;
import javax.management.AttributeList;
import jakarta.resource.spi.ResourceAdapter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class JMSDestination {

    protected static final Logger logger = Logger.getLogger(LogUtils.JMS_ADMIN_LOGGER);
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateJMSDestination.class);

    // JMS destination types
    public static final String JMS_DEST_TYPE_TOPIC		= "topic";
    public static final String JMS_DEST_TYPE_QUEUE		= "queue";
    public static final String DEFAULT_MAX_ACTIVE_CONSUMERS = "-1";
    public static final String MAX_ACTIVE_CONSUMERS_ATTRIBUTE = "MaxNumActiveConsumers";
    public static final String MAX_ACTIVE_CONSUMERS_PROPERTY = "maxNumActiveConsumers";
    public static final String JMXSERVICEURLLIST = "JMXServiceURLList";
    public static final String JMXCONNECTORENV = "JMXConnectorEnv";
    // flag to enable the use of JMX for JMS destination commands
    // if false uses the old behavior
    // The value for DONT_USE_MQ_JMX can be set thru sysproperty
    private static final boolean USE_JMX =  true;//!(Boolean.getBoolean("DONT_USE_MQ_JMX"));
    //Following properties are from com.sun.messaging.jms.management.server.MQObjectName
    /*   Domain name for MQ MBeans   */
    protected static final String MBEAN_DOMAIN_NAME = "com.sun.messaging.jms.server";
    /* String representation of the ObjectName for the DestinationManager Config MBean. */
    protected static final String DESTINATION_MANAGER_CONFIG_MBEAN_NAME
            = MBEAN_DOMAIN_NAME
            + ":type=" + "DestinationManager"
            + ",subtype=Config";
    protected static final String CLUSTER_CONFIG_MBEAN_NAME
            = MBEAN_DOMAIN_NAME
            + ":type=" + "Cluster"
            + ",subtype=Config";
    // Queue destination type
    protected static final String DESTINATION_TYPE_QUEUE= "q";
    //Topic destination type
    protected static final String DESTINATION_TYPE_TOPIC = "t";

    protected void validateJMSDestName(String destName) {
        if (destName == null || destName.length() <= 0 || destName.contains("/")) {
            throw new IllegalArgumentException(localStrings.getLocalString("admin.mbeans.rmb.invalid_jms_destname", destName));
        }
    }

    protected void validateJMSDestType(String destType) {
        if (destType == null || destType.length() <= 0)
            throw new IllegalArgumentException(localStrings.getLocalString("admin.mbeans.rmb.invalid_jms_desttype", destType));
        if (!destType.equals(JMS_DEST_TYPE_QUEUE) &&
                !destType.equals(JMS_DEST_TYPE_TOPIC))
            throw new IllegalArgumentException(localStrings.getLocalString("admin.mbeans.rmb.invalid_jms_desttype", destType));
    }

    protected MQJMXConnectorInfo getMQJMXConnectorInfo(String target, Config config, ServerContext serverContext, Domain domain, ConnectorRuntime connectorRuntime)
            throws Exception {
        logger.log(Level.FINE, "getMQJMXConnectorInfo for " + target);
        MQJMXConnectorInfo mcInfo = null;

        try {
            MQJMXConnectorInfo[] cInfo =
                    getMQJMXConnectorInfos(target, config, serverContext, domain, connectorRuntime);
            if (cInfo.length < 1) {
                throw new Exception(
                        localStrings.getLocalString("admin.mbeans.rmb.error_obtaining_jms", "Error obtaining JMS Info"));
            }
            mcInfo = cInfo[0];

        } catch (Exception e) {
            handleException(e);
        }
        return mcInfo;
    }

    protected MQJMXConnectorInfo[] getMQJMXConnectorInfos(final String target, final Config config, final ServerContext serverContext, final Domain domain, ConnectorRuntime connectorRuntime)
            throws ConnectorRuntimeException {
        try {
            final JmsService jmsService = config.getExtensionByType(JmsService.class);

            ActiveJmsResourceAdapter air = getMQAdapter(connectorRuntime);
            final Class mqRAClassName = air.getResourceAdapter().getClass();
            final CommandTarget ctarget = this.getTypeForTarget(target);
            MQJMXConnectorInfo mqjmxForServer = (MQJMXConnectorInfo) java.security.AccessController.doPrivileged((PrivilegedExceptionAction) () -> {
                if (ctarget == CommandTarget.DEPLOYMENT_GROUP) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Getting JMX connector for" +
                                " cluster target " + target);
                    }
                    return _getMQJMXConnectorInfoForDeploymentGroup(target,
                            jmsService, mqRAClassName, serverContext);
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Getting JMX connector for" +
                                " standalone target " + target);
                    }
                    return _getMQJMXConnectorInfo(target,
                            jmsService, mqRAClassName, serverContext, config, domain);
                }
            });

            return new MQJMXConnectorInfo[]{mqjmxForServer};
        } catch (Exception e) {
            throw new ConnectorRuntimeException(e.getMessage(), e);
        }
    }

    protected MQJMXConnectorInfo _getMQJMXConnectorInfo(
            String targetName, JmsService jmsService, Class mqRAClassName, ServerContext serverContext, Config config, Domain domain)
            throws ConnectorRuntimeException {
        try {
            String connectionURL = null;
            MQAddressList mqadList = new MQAddressList();

            if (getTypeForTarget(targetName) == CommandTarget.DAS) {
                connectionURL = getDefaultAddressList(jmsService).toString();
            } else {
                //Standalone server instance
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "not in DAS");
                    logger.log(Level.FINE, " _getMQJMXConnectorInfo - NOT in DAS");
                }
                JmsService serverJmsService = getJmsServiceOfStandaloneServerInstance(targetName, config, domain);
                mqadList.setJmsService(serverJmsService);
                mqadList.setTargetName(targetName);
                logger.log(Level.FINE, "JMSDestination L204 NOT CLUSTERED");
                mqadList.setup();
                connectionURL = mqadList.toString();
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " _getMQJMXConnectorInfo - connection URL " + connectionURL);
            }
            String adminUserName = null;
            String adminPassword = null;
            JmsHost jmsHost = mqadList.getDefaultJmsHost(jmsService);
            if (jmsHost != null) {
                adminUserName = jmsHost.getAdminUserName();
                adminPassword = JmsRaUtil.getUnAliasedPwd(jmsHost.getAdminPassword());
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, " _getMQJMXConnectorInfo, using default jms admin user and password ");
                }
            }
            ResourceAdapter raInstance = getConfiguredRA(mqRAClassName,
                    connectionURL, adminUserName, adminPassword);
            String jmxServiceURL = null, jmxServiceURLList = null;
            Map<String, ?> jmxConnectorEnv = null;
            Method[] methds = raInstance.getClass().getMethods();
            for (int i = 0; i < methds.length; i++) {
                Method m = methds[i];
                if (m.getName().equalsIgnoreCase("get" + JMXSERVICEURLLIST)) {
                    jmxServiceURLList = (String) m.invoke(raInstance, new Object[]{});
                } else if (m.getName().equalsIgnoreCase("get" + JMXCONNECTORENV)) {
                    jmxConnectorEnv = (Map<String, ?>) m.invoke(raInstance, new Object[]{});
                }
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " _getMQJMXConnectorInfo - jmxServiceURLList " + jmxServiceURLList);
                logger.log(Level.FINE, " _getMQJMXConnectorInfo - jmxConnectorEnv " + jmxConnectorEnv);
            }
            jmxServiceURL = getFirstJMXServiceURL(jmxServiceURLList);

            return new MQJMXConnectorInfo(targetName,
                    ActiveJmsResourceAdapter.getBrokerInstanceName(jmsService),
                    jmsService.getType(), jmxServiceURL, jmxConnectorEnv);
        } catch (Exception e) {
            throw new ConnectorRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Gets the <code>MQJMXConnector</code> object for a deployment group. Since this code is
     * executed in DAS, an admin API is used to resolve hostnames and ports of
     * cluster instances for LOCAL type brokers while creating the connectionURL.
     */
    protected MQJMXConnectorInfo _getMQJMXConnectorInfoForDeploymentGroup(String target, JmsService jmsService, Class mqRAClassName, ServerContext serverContext)
            throws ConnectorRuntimeException {
        ResourceAdapter raInstance = null;
        MQAddressList list = null;
        try {
            if (jmsService.getType().equalsIgnoreCase(ActiveJmsResourceAdapter.REMOTE)) {
                list = getDefaultAddressList(jmsService);
            } else {
                list = new MQAddressList();
                List<Server> servers = list.getServersInDeploymentGroup(target);
                if (servers != null && servers.size() > 0) {
                    list.setInstanceName(servers.get(0).getName());
                }
                Map<String, JmsHost> hostMap = list.getResolvedLocalJmsHostsInDeploymentGroup(true);
                if (hostMap.size() == 0) {
                    String msg = localStrings.getLocalString("mqjmx.no_jms_hosts", "No JMS Hosts Configured");
                    throw new ConnectorRuntimeException(msg);
                }
                for (JmsHost host : hostMap.values()) {
                    list.addMQUrl(host);
                }
            }
            String connectionUrl = list.toString();
            String adminUserName = null;
            String adminPassword = null;
            JmsHost jmsHost = list.getDefaultJmsHost(jmsService);
            if (jmsHost != null) {
                adminUserName = jmsHost.getAdminUserName();
                adminPassword = JmsRaUtil.getUnAliasedPwd(jmsHost.getAdminPassword());
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, " _getMQJMXConnectorInfo, using default jms admin user and password ");
                }
            }
            raInstance = getConfiguredRA(mqRAClassName, connectionUrl,
                    adminUserName, adminPassword);
        } catch (Exception e) {
            throw new ConnectorRuntimeException(e.getMessage(), e);
        }

        try {
            String jmxServiceURL = null, jmxServiceURLList = null;
            Map<String, ?> jmxConnectorEnv = null;
            Method[] methds = raInstance.getClass().getMethods();
            for (Method m : methds) {
                if (m.getName().equalsIgnoreCase("get" + JMXSERVICEURLLIST)) {
                    jmxServiceURLList = (String) m.invoke(raInstance, new Object[]{});
                    if (jmxServiceURLList != null && !jmxServiceURLList.trim().equals("")) {
                        jmxServiceURL = getFirstJMXServiceURL(jmxServiceURLList);
                    }
                } else if (m.getName().equalsIgnoreCase("get" + JMXCONNECTORENV)) {
                    jmxConnectorEnv = (Map<String, ?>) m.invoke(raInstance, new Object[]{});
                }
            }
            MQJMXConnectorInfo mqInfo = new MQJMXConnectorInfo(target,
                    ActiveJmsResourceAdapter.getBrokerInstanceName(jmsService),
                    jmsService.getType(), jmxServiceURL, jmxConnectorEnv);
            return mqInfo;
        } catch (Exception e) {
            throw new ConnectorRuntimeException(e.getMessage(), e);
        }
    }

    /*
     *  Configures an instance of MQ-RA with the connection URL passed in.
     *  This configured RA is then used to obtain the JMXServiceURL/JMXServiceURLList
     */
    protected ResourceAdapter getConfiguredRA(Class mqRAclassname,
                                              String connectionURL, String adminuser,
                                              String adminpasswd) throws Exception {
        ResourceAdapter raInstance = (ResourceAdapter) mqRAclassname.newInstance();
        Method setConnectionURL = mqRAclassname.getMethod(
                "set" + ActiveJmsResourceAdapter.CONNECTION_URL,
                new Class[]{String.class});
        setConnectionURL.invoke(raInstance, new Object[]{connectionURL});
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, " getConfiguredRA - set connectionURL as " + connectionURL);
        }
        if (adminuser != null) {
            Method setAdminUser = mqRAclassname.getMethod(
                    "set" + ActiveJmsResourceAdapter.ADMINUSERNAME,
                    new Class[]{String.class});
            setAdminUser.invoke(raInstance, new Object[]{adminuser});
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " getConfiguredRA - set admin user as " + adminuser);
            }
        }
        if (adminpasswd != null) {
            Method setAdminPasswd = mqRAclassname.getMethod(
                    "set" + ActiveJmsResourceAdapter.ADMINPASSWORD,
                    new Class[]{String.class});
            setAdminPasswd.invoke(raInstance, new Object[]{adminpasswd});
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " getConfiguredRA - set admin passwd as *****  ");
            }
        }
        return raInstance;
    }

    private JmsService getJmsServiceOfStandaloneServerInstance(String target, Config cfg, Domain domain) throws Exception {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "getJMSServiceOfSI LL " + target);
            //ConfigContext con = com.sun.enterprise.admin.server.core.AdminService.getAdminService().getAdminContext().getAdminConfigContext();
            logger.log(Level.FINE, "cfg " + cfg);
        }
        JmsService jmsService = cfg.getExtensionByType(JmsService.class);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "jmsservice " + jmsService);
        }
        return jmsService;
    }


    protected String getFirstJMXServiceURL(String jmxServiceURLList) {
        //If type is REMOTE, MQ RA returns a null jmxServiceURL and a non-null
        //jmxServiceURLList for PE also.
        if ((jmxServiceURLList == null) || ("".equals(jmxServiceURLList))) {
            logger.log(Level.FINE, "jmxServiceURLList: " + jmxServiceURLList);
            return jmxServiceURLList;
        } else {
            StringTokenizer tokenizer = new StringTokenizer(jmxServiceURLList, " ");
            return tokenizer.nextToken();
        }
    }

    protected CommandTarget getTypeForTarget(String target) {
        Domain domain = Globals.get(Domain.class);
        Config config = domain.getConfigNamed(target);
        if (config != null)
            return CommandTarget.CONFIG;
        Server targetServer = domain.getServerNamed(target);
        if (targetServer != null) {
            if (targetServer.isDas()) {
                return CommandTarget.DAS;
            } else {
                return CommandTarget.STANDALONE_INSTANCE;
            }
        }
        DeploymentGroup deploymentGroup = domain.getDeploymentGroupNamed(target);
        if (deploymentGroup != null) {
            return CommandTarget.DEPLOYMENT_GROUP;
        }
        return CommandTarget.DAS;
    }

    /*
     *  Starts the MQ RA in the DAS, as all MQ related operations are
     *  performed in DAS.
     */
    protected ActiveJmsResourceAdapter getMQAdapter(final ConnectorRuntime connectorRuntime) throws Exception {
        //Start ActiveJMSResourceAdapter.

        ActiveJmsResourceAdapter air = (ActiveJmsResourceAdapter)
                java.security.AccessController.doPrivileged
                        (new java.security.PrivilegedExceptionAction() {
                            public java.lang.Object run() throws Exception {
                                String module = ConnectorConstants.DEFAULT_JMS_ADAPTER;
                                String loc = ConnectorsUtil.getSystemModuleLocation(module);
                                connectorRuntime.createActiveResourceAdapter(loc, module, null);
                                return (ActiveJmsResourceAdapter) ConnectorRegistry.getInstance().
                                        getActiveResourceAdapter(module);
                            }
                        });
        return air;
    }

    protected MQAddressList getDefaultAddressList(JmsService jmsService)
            throws Exception {
        MQAddressList list = new MQAddressList(jmsService);
        logger.log(Level.FINE, "JMSDestination L529: NOT CLUSTERED");
        list.setup(false);
        return list;
    }

    protected void logAndHandleException(Exception e, String errorMsg)
            throws JMSAdminException {
        //log JMX Exception trace as WARNING
        StringWriter s = new StringWriter();
        e.getCause().printStackTrace(new PrintWriter(s));
        logger.log(Level.WARNING, s.toString());
        JMSAdminException je = new JMSAdminException(localStrings.getLocalString(errorMsg, ""));
        /* Cause will be InvocationTargetException, cause of that
         * wil be  MBeanException and cause of that will be the
         * real exception we need
         */
        if ((e.getCause() != null) &&
                (e.getCause().getCause() != null)) {
            je.initCause(e.getCause().getCause().getCause());
        }
        handleException(je);
    }

    protected void handleException(Exception e)
            throws JMSAdminException {

        if (e instanceof JMSAdminException) {
            throw ((JMSAdminException) e);
        }

        String msg = e.getMessage();

        JMSAdminException jae;
        if (msg == null) {
            jae = new JMSAdminException();
        } else {
            jae = new JMSAdminException(msg);
        }

        /*
         * Don't do this for now because the CLI does not include jms.jar
         * (at least not yet) in the classpath. Sending over a JMSException
         * will cause a class not found exception to be thrown.
         */
        //jae.setLinkedException(e);

        throw jae;
    }

    //XXX: To refactor into a Generic attribute type mapper, so that it is extensible later.
    protected AttributeList convertProp2Attrs(Properties destProps) {

        AttributeList destAttrs = new AttributeList();

        String propName = null;
        String propValue = null;

        for (Enumeration e = destProps.propertyNames(); e.hasMoreElements(); ) {
            propName = (String) e.nextElement();
            if (propName.equals("AutoCreateQueueMaxNumActiveConsumers")) {
                destAttrs.add(new Attribute("AutoCreateQueueMaxNumActiveConsumers",
                        Integer.valueOf(destProps.getProperty("AutoCreateQueueMaxNumActiveConsumers"))));
            } else if (propName.equals("maxNumActiveConsumers")) {
                destAttrs.add(new Attribute("MaxNumActiveConsumers",
                        Integer.valueOf(destProps.getProperty("maxNumActiveConsumers"))));
            } else if (propName.equals("MaxNumActiveConsumers")) {
                destAttrs.add(new Attribute("MaxNumActiveConsumers",
                        Integer.valueOf(destProps.getProperty("MaxNumActiveConsumers"))));
            } else if (propName.equals("AutoCreateQueueMaxNumBackupConsumers")) {
                destAttrs.add(new Attribute("AutoCreateQueueMaxNumBackupConsumers",
                        Integer.valueOf(destProps.getProperty("AutoCreateQueueMaxNumBackupConsumers"))));
            } else if (propName.equals("AutoCreateQueues")) {
                boolean b = false;

                propValue = destProps.getProperty("AutoCreateQueues");
                if (propValue.equalsIgnoreCase("true")) {
                    b = true;
                }
                destAttrs.add(new Attribute("AutoCreateQueues",
                        Boolean.valueOf(b)));
            } else if (propName.equals("AutoCreateTopics")) {
                boolean b = false;

                propValue = destProps.getProperty("AutoCreateTopics");
                if (propValue.equalsIgnoreCase("true")) {
                    b = true;
                }
                destAttrs.add(new Attribute("AutoCreateTopics",
                        Boolean.valueOf(b)));
            } else if (propName.equals("DMQTruncateBody")) {
                boolean b = false;

                propValue = destProps.getProperty("DMQTruncateBody");
                if (propValue.equalsIgnoreCase("true")) {
                    b = true;
                }
                destAttrs.add(new Attribute("DMQTruncateBody",
                        Boolean.valueOf(b)));
            } else if (propName.equals("LogDeadMsgs")) {
                boolean b = false;

                propValue = destProps.getProperty("LogDeadMsgs");
                if (propValue.equalsIgnoreCase("true")) {
                    b = true;
                }
                destAttrs.add(new Attribute("LogDeadMsgs",
                        Boolean.valueOf(b)));
            } else if (propName.equals("MaxBytesPerMsg")) {
                destAttrs.add(new Attribute("MaxBytesPerMsg",
                        Long.valueOf(destProps.getProperty("MaxBytesPerMsg"))));
            } else if (propName.equals("MaxNumMsgs")) {
                destAttrs.add(new Attribute("MaxNumMsgs",
                        Long.valueOf(destProps.getProperty("MaxNumMsgs"))));
            } else if (propName.equals("MaxTotalMsgBytes")) {
                destAttrs.add(new Attribute("MaxTotalMsgBytes",
                        Long.valueOf(destProps.getProperty("MaxTotalMsgBytes"))));
            } else if (propName.equals("NumDestinations")) {
                destAttrs.add(new Attribute("NumDestinations",
                        Integer.valueOf(destProps.getProperty("NumDestinations"))));
            } else if (propName.equals("ConsumerFlowLimit")) {
                destAttrs.add(new Attribute("ConsumerFlowLimit",
                        Long.valueOf(destProps.getProperty("ConsumerFlowLimit"))));
            } else if (propName.equals("LocalDeliveryPreferred")) {
                destAttrs.add(new Attribute("LocalDeliveryPreferred",
                        getBooleanValue(destProps.getProperty("LocalDeliveryPreferred"))));
            } else if (propName.equals("ValidateXMLSchemaEnabled")) {
                destAttrs.add(new Attribute("ValidateXMLSchemaEnabled",
                        getBooleanValue(destProps.getProperty("ValidateXMLSchemaEnabled"))));
            } else if (propName.equals("UseDMQ")) {
                destAttrs.add(new Attribute("UseDMQ",
                        getBooleanValue(destProps.getProperty("UseDMQ"))));
            } else if (propName.equals("LocalOnly")) {
                destAttrs.add(new Attribute("LocalOnly",
                        getBooleanValue(destProps.getProperty("LocalOnly"))));
            } else if (propName.equals("ReloadXMLSchemaOnFailure")) {
                destAttrs.add(new Attribute("ReloadXMLSchemaOnFailure",
                        getBooleanValue(destProps.getProperty("ReloadXMLSchemaOnFailure"))));
            } else if (propName.equals("MaxNumProducers")) {
                destAttrs.add(new Attribute("MaxNumProducers",
                        Integer.valueOf(destProps.getProperty("MaxNumProducers"))));
            } else if (propName.equals("MaxNumBackupConsumers")) {
                destAttrs.add(new Attribute("MaxNumBackupConsumers",
                        Integer.valueOf(destProps.getProperty("MaxNumBackupConsumers"))));
            } else if (propName.equals("LimitBehavior")) {
                destAttrs.add(new Attribute("LimitBehavior",
                        destProps.getProperty("LimitBehavior")));
            }
        }
        return destAttrs;
    }

    private Boolean getBooleanValue(String propValue) {
        return propValue.equalsIgnoreCase("true") ? Boolean.TRUE : Boolean.FALSE;
    }
}
