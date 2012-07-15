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

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.I18n;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.connectors.config.AdminObjectResource;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.ConnectorResource;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.component.BaseServiceLocator;
/**
 * List Connector Resources command
 *
 */
@Service(name="list-jms-resources")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.jms.resources")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.DOMAIN})
@RestEndpoints({
    @RestEndpoint(configBean=Resources.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-jms-resources", 
        description="list-jms-resources")
})
public class ListJMSResources implements AdminCommand {

    private static final String JMSRA = "jmsra";
    private static final String QUEUE = "javax.jms.Queue";
    private static final String TOPIC = "javax.jms.Topic";
    private static final String QUEUE_CF = "javax.jms.QueueConnectionFactory";
    private static final String TOPIC_CF = "javax.jms.TopicConnectionFactory";
    private static final String UNIFIED_CF = "javax.jms.ConnectionFactory";
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListJMSResources.class);

    @Param(name="resType", optional=true)
    String resourceType;

    @Param(primary=true, optional=true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;;


    @Inject
    Domain domain;

    @Inject
    BaseServiceLocator habitat;

    /**
        * Executes the command with the command parameters passed as Properties
        * where the keys are the paramter names and the values the parameter values
        *
        * @param context information
        */
       public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        ArrayList<String> list = new ArrayList();
        Properties extraProperties = new Properties();

        Collection adminObjectResourceList = domain.getResources().getResources(AdminObjectResource.class);
        Collection connectorResourcesList = domain.getResources().getResources(ConnectorResource.class);
        

        Object [] connectorResources = connectorResourcesList.toArray();
        Object [] adminObjectResources = adminObjectResourceList.toArray();

        if(resourceType == null){
          try{
            //list all JMS resources
            for (Object r :  adminObjectResources) {
               AdminObjectResource adminObject = (AdminObjectResource) r;
               if (JMSRA.equalsIgnoreCase(adminObject.getResAdapter()))
              //if(QUEUE.equals(r.getResType()) || TOPIC.equals(r.getResType()))
                list.add(adminObject.getJndiName());
            }

            for (Object c: connectorResources) {
                ConnectorResource cr = (ConnectorResource) c;
                ConnectorConnectionPool cp = (ConnectorConnectionPool) ConnectorsUtil.getResourceByName(
                        domain.getResources(), ConnectorConnectionPool.class, cr.getPoolName());

                if (cp  != null && JMSRA.equalsIgnoreCase(cp.getResourceAdapterName())){
                      list.add(cr.getJndiName());
                }
               //if(QUEUE_CF.equals(cp.getConnectionDefinitionName()) || TOPIC_CF.equals(cp.getConnectionDefinitionName())
                 //      || UNIFIED_CF.equals(cp.getConnectionDefinitionName()))

            }
            if (list.isEmpty()) {
                final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                part.setMessage(localStrings.getLocalString("nothingToList",
                    "Nothing to list."));
                extraProperties.put("jmsResources", list);
            } else {

               List <String> resourceList = null;

               if(CommandTarget.DOMAIN.isValid(habitat, target))
                     resourceList = list;
               else
                     resourceList =  filterListForTarget(list);
               //if(resourceList == null)
                 //   resourceList = list;


                for (String jndiName : resourceList) {
                    final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                    part.setMessage(jndiName);
                }
                extraProperties.put("jmsResources", resourceList);
            }
            report.setExtraProperties(extraProperties);
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("list.jms.resources.fail",
                    "Unable to list JMS Resources") + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
      } else {
          if(resourceType.equals(TOPIC_CF) || resourceType.equals(QUEUE_CF) || resourceType.equals(UNIFIED_CF)){


            for (Object c : connectorResources) {
               ConnectorResource cr = (ConnectorResource)c;
               ConnectorConnectionPool cp = (ConnectorConnectionPool)
                       ConnectorsUtil.getResourceByName(domain.getResources(), ConnectorConnectionPool.class, cr.getPoolName());
               if(cp != null && resourceType.equals(cp.getConnectionDefinitionName()) && JMSRA.equalsIgnoreCase(cp.getResourceAdapterName()))
                    list.add(cp.getName());
            }
          }  else if (resourceType.equals(TOPIC) || resourceType.equals(QUEUE))
          {
                for (Object r : adminObjectResources) {
                    AdminObjectResource res = (AdminObjectResource)r;
                    if(resourceType.equals(res.getResType()))                             
                        list.add(res.getJndiName());
            }

          }


        //List <String> resourceList = filterListForTarget(list);
        //if(resourceList == null)
          // resourceList = list;

        for (String jndiName : list) {
              final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
              part.setMessage(jndiName);
         }
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);


  }
    private List filterListForTarget(List <String> list){
        List <String> resourceList = new ArrayList();
            if (target != null){
                List<ResourceRef> resourceRefs = null;
                Cluster cluster = domain.getClusterNamed(target);
                if (cluster != null)
                      resourceRefs=  cluster.getResourceRef();

                else {
                    Server server = domain.getServerNamed(target);
                     if (server != null)
                         resourceRefs = server.getResourceRef();

                }
                if (resourceRefs != null && resourceRefs.size() != 0){

                    for (String jndiName : list) {
                        for (ResourceRef resource : resourceRefs)
                            if(jndiName.equals(resource.getRef()))
                                resourceList.add(jndiName);
                    }
                }


            }
        return resourceList;
    }
}



