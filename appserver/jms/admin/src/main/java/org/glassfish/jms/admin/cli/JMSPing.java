/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.connectors.jms.config.JmsHost;
import com.sun.enterprise.connectors.jms.config.JmsService;
import com.sun.enterprise.connectors.jms.util.JmsRaUtil;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.resource.ResourceException;
import java.util.Properties;

@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CLUSTERED_INSTANCE,CommandTarget.CONFIG})
@Service(name="jms-ping")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("jms-ping")
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.GET, 
        path="jms-ping", 
        description="Ping JMS",
        params={
            @RestParam(name="id", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.GET, 
        path="jms-ping", 
        description="Ping JMS",
        params={
            @RestParam(name="id", value="$parent")
        })
})
public class JMSPing implements AdminCommand {
    @Param(optional=true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(JMSPing.class);
    final private static String JNDINAME_APPENDER="-Connection-Pool";

    @Inject
    private ConnectorRuntime connectorRuntime;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    Domain domain;

    @Inject
    CommandRunner commandRunner;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

         Server targetServer = domain.getServerNamed(target);
         //String configRef = targetServer.getConfigRef();
        if (targetServer!=null) {
            config = domain.getConfigNamed(targetServer.getConfigRef());
        }
        com.sun.enterprise.config.serverbeans.Cluster cluster =domain.getClusterNamed(target);
        if (cluster!=null) {
            config = domain.getConfigNamed(cluster.getConfigRef());
        }

         JmsService jmsservice =  config.getExtensionByType(JmsService.class);
              /* for (Config c : configs.getConfig()) {

                      if(configRef.equals(c.getName()))
                            jmsservice = c.getJmsService();
                   } */
         String defaultJmshostStr = jmsservice.getDefaultJmsHost();
         JmsHost defaultJmsHost = null;
               for (JmsHost jmshost : jmsservice.getJmsHost()) {

                      if(defaultJmshostStr.equals(jmshost.getName()))
                            defaultJmsHost = jmshost;
                   }
         String tmpJMSResource = "test_jms_adapter";
         ActionReport subReport = report.addSubActionsReport();
         createJMSResource(defaultJmsHost, subReport, tmpJMSResource, context.getSubject());
         if (ActionReport.ExitCode.FAILURE.equals(subReport.getActionExitCode())){
                report.setMessage(localStrings.getLocalString("jms-ping.cannotCreateJMSResource",
                         "Unable to create a temporary Connection Factory to the JMS Host"));
               report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
         }
        try{
            boolean value = pingConnectionPool(tmpJMSResource + JNDINAME_APPENDER);
            
            if(!value){

                 report.setMessage(localStrings.getLocalString("jms-ping.pingConnectionPoolFailed",
                         "Pinging to the JMS Host failed."));
                 report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }else {
                  report.setMessage(localStrings.getLocalString("jms-ping.pingConnectionPoolSuccess",
                         "JMS-ping command executed successfully"));
                 report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            }
        }catch (ResourceException e)
        {
            report.setMessage(localStrings.getLocalString("jms-ping.pingConnectionPoolException",
                         "An exception occured while trying to ping the JMS Host.", e.getMessage()));
           report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        deleteJMSResource(subReport, tmpJMSResource, context.getSubject());
        if (ActionReport.ExitCode.FAILURE.equals(subReport.getActionExitCode())){
                report.setMessage(localStrings.getLocalString("jms-ping.cannotdeleteJMSResource",
                         "Unable to delete the temporary JMS Resource " + tmpJMSResource + ". Please delete this manually.", tmpJMSResource));
               report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
         }
    }

   void createJMSResource(JmsHost defaultJmsHost, ActionReport subReport, String tmpJMSResource, final Subject subject)
   {
        String port = null;
        String host = null;
        Server targetServer = domain.getServerNamed(target);
            if (targetServer != null && ! targetServer.isDas()) {
                port = JmsRaUtil.getJMSPropertyValue(targetServer);
                host = domain.getNodeNamed(targetServer.getNodeRef()).getNodeHost();
            } else{
                Cluster cluster = domain.getClusterNamed(target);
                if (cluster != null && cluster.getInstances().size() != 0) {
                    targetServer = cluster.getInstances().get(0);
                    port = JmsRaUtil.getJMSPropertyValue(targetServer);
                    host = domain.getNodeNamed(targetServer.getNodeRef()).getNodeHost();
                }
            }

        String userName = defaultJmsHost.getAdminUserName();
        String password = defaultJmsHost.getAdminPassword();
        if(host == null)
             host = defaultJmsHost.getHost();
        if(port == null)
            port = defaultJmsHost.getPort();

        ParameterMap aoAttrList = new ParameterMap();

        Properties properties = new Properties();
        properties.put("imqDefaultUsername",userName);
         if (isPasswordAlias(password)){
                       //If the string is a password alias, it needs to be escapted with another pair of quotes...
                       properties.put("imqDefaultPassword", "\"" + password + "\"");
         }else
             properties.put("imqDefaultPassword",password);

       //need to escape the addresslist property so that they get passed on correctly to the create-connector-connection-pool command
        properties.put("AddressList", "\"mq://"+host + ":"+ port +"\"");

        StringBuilder builder = new StringBuilder();
        for (java.util.Map.Entry<Object, Object>prop : properties.entrySet()) {
            builder.append(prop.getKey()).append("=").append(prop.getValue()).append(":");
        }
        String propString = builder.toString();
        int lastColonIndex = propString.lastIndexOf(":");
        if (lastColonIndex >= 0) {
            propString = propString.substring(0, lastColonIndex);
        }
        aoAttrList.set("property", propString);

        aoAttrList.set("restype",  "javax.jms.QueueConnectionFactory");
        aoAttrList.set("DEFAULT",  tmpJMSResource);
        //aoAttrList.set("target", target);
        commandRunner.getCommandInvocation("create-jms-resource", subReport, subject).parameters(aoAttrList).execute();

    }
    private boolean isPasswordAlias(String password){
        if (password != null && password.contains("${ALIAS"))
            return true;

        return false;
    }

    boolean pingConnectionPool(String tmpJMSResource) throws ResourceException
    {
        PoolInfo poolInfo = new PoolInfo(tmpJMSResource);
        return connectorRuntime.pingConnectionPool(poolInfo);
    }
    
    void deleteJMSResource(ActionReport subReport, String tmpJMSResource, final Subject subject)
    {
        ParameterMap aoAttrList = new ParameterMap();
        aoAttrList.set("DEFAULT",  tmpJMSResource);
        //aoAttrList.set("target", target);

        commandRunner.getCommandInvocation("delete-jms-resource", subReport, subject).parameters(aoAttrList).execute();
    }
}
