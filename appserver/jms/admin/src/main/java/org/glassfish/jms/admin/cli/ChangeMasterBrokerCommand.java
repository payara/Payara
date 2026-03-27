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
// Portions Copyright 2018-2026 Payara Foundation and/or its affiliates

package org.glassfish.jms.admin.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.connectors.jms.config.JmsHost;
import com.sun.enterprise.connectors.jms.config.JmsService;
import com.sun.enterprise.connectors.jms.util.JmsRaUtil;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.util.List;


/**
 * Change JMS Master Broker Command
 *
 */
@Service(name = "change-master-broker")
@PerLookup
@I18n("change.master.broker")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
        @RestEndpoint(configBean = DeploymentGroup.class,
                opType = RestEndpoint.OpType.POST,
                path = "change-master-broker",
                description = "change-master-broker")
})
public class ChangeMasterBrokerCommand extends JMSDestination implements AdminCommand {
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ChangeMasterBrokerCommand.class);

    private enum BrokerStatusCode {
        BAD_REQUEST(400), NOT_ALLOWED(405), UNAVAILABLE(503), PRECONDITION_FAILED(412);

        private int code;

        BrokerStatusCode(int c) {
            code = c;
        }

        public int getCode() {
            return code;
        }
    }

    @Param(primary = true)
    String newMasterBroker;

    @Inject
    Domain domain;

    @Inject
    com.sun.appserv.connectors.internal.api.ConnectorRuntime connectorRuntime;

    @Inject
    ServerContext serverContext;
    Config config;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final String newMB = newMasterBroker;
        Server newMBServer = domain.getServerNamed(newMasterBroker);
        if (newMBServer == null) {
            report.setMessage(localStrings.getLocalString("change.master.broker.invalidServerName",
                    "Invalid server name specified. There is no server by this name"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        List<DeploymentGroup> deploymentGroupsList = newMBServer.getDeploymentGroup();

        if (deploymentGroupsList == null || deploymentGroupsList.isEmpty()) {
            report.setMessage(localStrings.getLocalString("change.master.broker.invalidClusterName",
                    "The server specified is not associated with a deployment group. The server associated with the master broker has to be a part of a deployment group"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (deploymentGroupsList.size() > 1) {
            report.setMessage(localStrings.getLocalString("change.master.broker.multipleGroupsNotAllowed",
                    "The server specified is associated with more than one deployment group. The server associated with the master broker can only be associated with one deployment group"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        Nodes nodes = domain.getNodes();
        DeploymentGroup deploymentGroup = deploymentGroupsList.getFirst();

        for (Server server : deploymentGroup.getInstances()) {
            config = domain.getConfigNamed(server.getConfigRef());
            JmsService jmsservice = config.getExtensionByType(JmsService.class);
            Server oldMBServer;
            // If Master broker has been set previously using this command, use that master broker as the old MB instance
            // Else use the first configured instance in the cluster list
            if (jmsservice.getMasterBroker() != null) {
                oldMBServer = domain.getServerNamed(jmsservice.getMasterBroker());
            } else {
                List<Server> serverList = deploymentGroup.getInstances();
                oldMBServer = serverList.getFirst();
            }

            String oldMasterBrokerPort = JmsRaUtil.getJMSPropertyValue(oldMBServer);
            if (oldMasterBrokerPort == null) {
                SystemProperty sp = config.getSystemProperty("JMS_PROVIDER_PORT");
                if (sp != null) {
                    oldMasterBrokerPort = sp.getValue();
                }
            }
            if (oldMasterBrokerPort == null) {
                oldMasterBrokerPort = getDefaultJmsHost(jmsservice).getPort();
            }
            String oldMasterBrokerHost = nodes.getNode(oldMBServer.getNodeRef()).getNodeHost();

            String newMasterBrokerPort = JmsRaUtil.getJMSPropertyValue(newMBServer);
            if (newMasterBrokerPort == null) {
                newMasterBrokerPort = getDefaultJmsHost(jmsservice).getPort();
            }
            String newMasterBrokerHost = nodes.getNode(newMBServer.getNodeRef()).getNodeHost();


            String oldMasterBroker = oldMasterBrokerHost + ":" + oldMasterBrokerPort;
            String newMasterBroker = newMasterBrokerHost + ":" + newMasterBrokerPort;
            try {
                CompositeData result = updateMasterBroker(oldMBServer.getName(), oldMasterBroker, newMasterBroker);
                boolean success = ((Boolean) result.get("Success")).booleanValue();
                if (!success) {
                    int statusCode = ((Integer) result.get("StatusCode")).intValue();
                    String detailMessage = (String) result.get("DetailMessage");
                    String msg = " " + detailMessage;
                    if (BrokerStatusCode.BAD_REQUEST.getCode() == statusCode || BrokerStatusCode.NOT_ALLOWED.getCode() == statusCode ||
                            BrokerStatusCode.UNAVAILABLE.getCode() == statusCode || BrokerStatusCode.PRECONDITION_FAILED.getCode() == statusCode) {
                        msg = localStrings.getLocalString("change.master.broker.errorMsg",
                                "{0}. But it didn't affect current master broker configuration.", msg);
                    } else {
                        msg = msg + ". " + localStrings.getLocalString("change.master.broker.otherErrorMsg",
                                "The cluster should be shutdown and configured with the new master broker then restarts.");
                    }

                    report.setMessage(localStrings.getLocalString("change.master.broker.CannotChangeMB",
                            "Unable to change master broker.{0}", msg));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            } catch (Exception e) {
                report.setMessage(localStrings.getLocalString("change.master.broker.CannotChangeMB",
                        "Unable to change master broker.{0}", ""));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            try {
                ConfigSupport.apply((SingleConfigCode<JmsService>) param -> {
                    param.setMasterBroker(newMB);
                    return param;
                }, jmsservice);
            } catch (Exception tfe) {
                report.setMessage(localStrings.getLocalString("change.master.broker.fail",
                        "Unable to update the domain.xml with the new master broker") +
                        " " + tfe.getLocalizedMessage());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(tfe);
            }
            report.setMessage(localStrings.getLocalString("change.master.broker.success",
                    "Master broker change has executed successfully for Cluster {0}.", deploymentGroup.getName()));
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
    }

    private JmsHost getDefaultJmsHost(JmsService jmsService) {
        JmsHost jmsHost = null;
        String defaultJmsHostName = jmsService.getDefaultJmsHost();
        List jmsHostsList = jmsService.getJmsHost();

        for (int i = 0; i < jmsHostsList.size(); i++) {
            JmsHost tmpJmsHost = (JmsHost) jmsHostsList.get(i);
            if (tmpJmsHost != null && tmpJmsHost.getName().equals(defaultJmsHostName)) {
                jmsHost = tmpJmsHost;
            }
        }
        return jmsHost;
    }

    private CompositeData updateMasterBroker(String serverName, String oldMasterBroker, String newMasterBroker) throws Exception {
        MQJMXConnectorInfo mqInfo = getMQJMXConnectorInfo(serverName, config, serverContext, domain, connectorRuntime);
        CompositeData result = null;
        try {
            MBeanServerConnection mbsc = mqInfo.getMQMBeanServerConnection();
            ObjectName on = new ObjectName(CLUSTER_CONFIG_MBEAN_NAME);
            Object[] params;

            String[] signature = new String[]{
                    "java.lang.String",
                    "java.lang.String"};
            params = new Object[]{oldMasterBroker, newMasterBroker};

            result = (CompositeData) mbsc.invoke(on, "changeMasterBroker", params, signature);
        } catch (Exception e) {
            logAndHandleException(e, "admin.mbeans.rmb.error_creating_jms_dest");
        } finally {
            try {
                if (mqInfo != null) {
                    mqInfo.closeMQMBeanServerConnection();
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
        return result;
    }
}

