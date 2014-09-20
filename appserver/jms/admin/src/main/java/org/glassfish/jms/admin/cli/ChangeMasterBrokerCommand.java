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

package org.glassfish.jms.admin.cli;

import com.sun.enterprise.connectors.jms.config.JmsHost;
import com.sun.enterprise.connectors.jms.config.JmsService;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.*;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.RuntimeType;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.connectors.jms.util.JmsRaUtil;

import org.glassfish.internal.api.ServerContext;

import java.util.List;
import java.beans.PropertyVetoException;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;


/**
 * Change JMS Master Broker Command
 *
 */
@Service(name="change-master-broker")
@PerLookup
@I18n("change.master.broker")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.POST, 
        path="change-master-broker", 
        description="change-master-broker")
})
public class ChangeMasterBrokerCommand extends JMSDestination implements AdminCommand {
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ChangeMasterBrokerCommand.class);
    // [usemasterbroker] [availability-enabled] [dbvendor] [dbuser] [dbpassword admin] [jdbcurl] [properties props] clusterName

    private static enum BrokerStatusCode {
        BAD_REQUEST(400), NOT_ALLOWED(405), UNAVAILABLE(503), PRECONDITION_FAILED(412);

        private int code;

        private BrokerStatusCode(int c) {
            code = c;
        }

        public int getCode() {
            return code;
        }
    }

    @Param (primary=true)//(name="newmasterbroker", alias="nmb", optional=false)
    String newMasterBroker;

    //@Param(primary=true)
    //String clusterName;

    @Inject
    CommandRunner commandRunner;


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
        Cluster cluster = newMBServer.getCluster();//domain.getClusterNamed(clusterName);

        if (cluster == null) {
            report.setMessage(localStrings.getLocalString("change.master.broker.invalidClusterName",
                            "The server specified is not associated with a cluster. The server assocaited with the master broker has to be a part of the cluster"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }


        /*if(!cluster.getName().equals(newMBServer.getCluster().getName()))
        {
            report.setMessage(localStrings.getLocalString("configure.jms.cluster.invalidClusterName",
                            "{0} does not belong to the specified cluster", newMasterBroker));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        } */

       Nodes nodes = domain.getNodes();
       config = domain.getConfigNamed(cluster.getConfigRef());
       JmsService jmsservice = config.getExtensionByType(JmsService.class);
       Server oldMBServer = null;
       //If Master broker has been set previously using this command, use that master broker as the old MB instance
       //Else use the first configured instance in the cluster list
       if(jmsservice.getMasterBroker() != null){
            oldMBServer = domain.getServerNamed(jmsservice.getMasterBroker());
       }else
       {
           List<Server> serverList = cluster.getInstances();
           //if(serverList == null || serverList.size() == 0){
             //report.setMessage(localStrings.getLocalString("change.master.broker.invalidCluster",
               //             "No servers configured in cluster {0}", clusterName));
            //report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            //return;
           //}
           oldMBServer = serverList.get(0);
       }

       String oldMasterBrokerPort = JmsRaUtil.getJMSPropertyValue(oldMBServer);
       if(oldMasterBrokerPort == null) {
	      SystemProperty sp = config.getSystemProperty("JMS_PROVIDER_PORT");
	      if(sp != null) oldMasterBrokerPort = sp.getValue();
       }
       if(oldMasterBrokerPort == null) oldMasterBrokerPort = getDefaultJmsHost(jmsservice).getPort();
       String oldMasterBrokerHost = nodes.getNode(oldMBServer.getNodeRef()).getNodeHost();

       String newMasterBrokerPort = JmsRaUtil.getJMSPropertyValue(newMBServer);
       if(newMasterBrokerPort == null) newMasterBrokerPort = getDefaultJmsHost(jmsservice).getPort();
       String newMasterBrokerHost = nodes.getNode(newMBServer.getNodeRef()).getNodeHost();


       String oldMasterBroker = oldMasterBrokerHost + ":" + oldMasterBrokerPort;
       String newMasterBroker = newMasterBrokerHost + ":" + newMasterBrokerPort;
      // System.out.println("1: IN deleteinstanceCheck supplimental oldMasterBroker = " + oldMasterBroker + " newmasterBroker " + newMasterBroker);
       try{
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
       }catch(Exception e){
                      report.setMessage(localStrings.getLocalString("change.master.broker.CannotChangeMB",
                                    "Unable to change master broker.{0}", ""));
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return;
                 }

        try {
            /*String setCommandStr = cluster.getConfigRef() + "." + "jms-service" + "." +"master-Broker";
            ParameterMap parameters = new ParameterMap();
            parameters.set(setCommandStr, newMB );

            ActionReport subReport = report.addSubActionsReport();
	        commandRunner.getCommandInvocation("set", subReport, context.getSubject()).parameters(parameters).execute();

              if (ActionReport.ExitCode.FAILURE.equals(subReport.getActionExitCode())){
                    report.setMessage(localStrings.getLocalString("create.jms.resource.cannotCreateConnectionPool",
                            "Unable to create connection pool."));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
              }*/

            ConfigSupport.apply(new SingleConfigCode<JmsService>() {
                public Object run(JmsService param) throws PropertyVetoException, TransactionFailure {

                    param.setMasterBroker(newMB);
                    return param;
                  }
               }, jmsservice);
        } catch(Exception tfe) {
            report.setMessage(localStrings.getLocalString("change.master.broker.fail",
                            "Unable to update the domain.xml with the new master broker") +
                            " " + tfe.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
        }
        report.setMessage(localStrings.getLocalString("change.master.broker.success",
                "Master broker change has executed successfully for Cluster {0}.", cluster.getName()));
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

   private JmsHost getDefaultJmsHost(JmsService jmsService){

	    JmsHost jmsHost = null;
            String defaultJmsHostName = jmsService.getDefaultJmsHost();
            List jmsHostsList = jmsService.getJmsHost();

            for (int i=0; i < jmsHostsList.size(); i ++)
            {
               JmsHost tmpJmsHost = (JmsHost) jmsHostsList.get(i);
               if (tmpJmsHost != null && tmpJmsHost.getName().equals(defaultJmsHostName))
                     jmsHost = tmpJmsHost;
            }
	    return jmsHost;
      }

     private CompositeData updateMasterBroker(String serverName, String oldMasterBroker, String newMasterBroker) throws Exception {
        MQJMXConnectorInfo mqInfo = getMQJMXConnectorInfo(serverName, config,serverContext, domain, connectorRuntime);

         //MBeanServerConnection  mbsc = getMBeanServerConnection(tgtName);
         CompositeData result = null;
         try {
             MBeanServerConnection mbsc = mqInfo.getMQMBeanServerConnection();
             ObjectName on = new ObjectName(
                     CLUSTER_CONFIG_MBEAN_NAME);
             Object [] params = null;

             String []  signature = new String [] {
                      "java.lang.String",
                      "java.lang.String"};
                        params = new Object [] {oldMasterBroker, newMasterBroker};

             result = (CompositeData) mbsc.invoke(on, "changeMasterBroker", params, signature);
         } catch (Exception e) {
                     logAndHandleException(e, "admin.mbeans.rmb.error_creating_jms_dest");
         } finally {
                     try {
                         if(mqInfo != null) {
                             mqInfo.closeMQMBeanServerConnection();
                         }
                     } catch (Exception e) {
                       handleException(e);
                     }
                 }
         return result;
     }
}

