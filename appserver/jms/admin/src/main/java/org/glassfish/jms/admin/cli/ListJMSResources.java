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
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.naming.DefaultResourceProxy;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.connectors.config.AdminObjectResource;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.ConnectorResource;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * List Connector Resources command
 *
 */
@Service(name="list-jms-resources")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.jms.resources")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.DOMAIN,CommandTarget.CLUSTERED_INSTANCE})
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
    ServiceLocator habitat;

    /**
        * Executes the command with the command parameters passed as Properties
        * where the keys are the paramter names and the values the parameter values
        *
        * @param context information
        */
    @Override
       public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        ArrayList<Map<String,String>> list = new ArrayList<>();
        Properties extraProperties = new Properties();

        Collection adminObjectResourceList = domain.getResources().getResources(AdminObjectResource.class);
        Collection connectorResourcesList = domain.getResources().getResources(ConnectorResource.class);       

        Object [] connectorResources = connectorResourcesList.toArray();
        Object [] adminObjectResources = adminObjectResourceList.toArray();

        if (resourceType == null){
          try {
            //list all JMS resources
            for (Object r :  adminObjectResources) {
                AdminObjectResource adminObject = (AdminObjectResource) r;
                if (JMSRA.equals(adminObject.getResAdapter())) {
                    Map<String,String> m = new HashMap<>();
                    m.put("name", adminObject.getJndiName());
                    list.add(m);
                }
            }

            for (Object c: connectorResources) {
                ConnectorResource cr = (ConnectorResource) c;
                ConnectorConnectionPool cp = (ConnectorConnectionPool) ConnectorsUtil.getResourceByName(
                        domain.getResources(), ConnectorConnectionPool.class, cr.getPoolName());

                if (cp  != null && JMSRA.equals(cp.getResourceAdapterName())){
                    Map<String,String> m = new HashMap<>();
                    m.put("name", cr.getJndiName());
                    list.add(m);
                }
            }
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("list.jms.resources.fail",
                    "Unable to list JMS Resources") + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
      } else {
            switch (resourceType) {
                case TOPIC_CF:
                case QUEUE_CF:
                case UNIFIED_CF:
                    for (Object c : connectorResources) {
                       ConnectorResource cr = (ConnectorResource)c;
                       ConnectorConnectionPool cp = (ConnectorConnectionPool)
                               ConnectorsUtil.getResourceByName(domain.getResources(), ConnectorConnectionPool.class, cr.getPoolName());
                       if(cp != null && resourceType.equals(cp.getConnectionDefinitionName()) && JMSRA.equals(cp.getResourceAdapterName())) {
                            Map<String,String> m = new HashMap<>();
                            m.put("name", cr.getJndiName());
                            list.add(m);
                        }
                    }
                    break;
                case TOPIC:
                case QUEUE:
                    for (Object r : adminObjectResources) {
                        AdminObjectResource res = (AdminObjectResource)r;
                        if(resourceType.equals(res.getResType()) && JMSRA.equals(res.getResAdapter())) {
                            Map<String,String> m = new HashMap<>();
                            m.put("name", res.getJndiName());
                            list.add(m);
                        }
                    }
                    break;
            }
      }
        if (!list.isEmpty()) {
            List<Map<String,String>> resourceList = 
                CommandTarget.DOMAIN.isValid(habitat, target) ? list : filterListForTarget(list);

            List<DefaultResourceProxy> drps = habitat.getAllServices(DefaultResourceProxy.class);
            
            for (Map<String,String> m : resourceList) {
                String jndiName = m.get("name");
                final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                part.setMessage(jndiName);
                String logicalName = DefaultResourceProxy.Util.getLogicalName(drps, jndiName);
                if (logicalName != null) {
                    m.put("logical-jndi-name", logicalName);
                } 
            }
            extraProperties.put("jmsResources", resourceList);
        }
        report.setExtraProperties(extraProperties);
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
  }

    private List filterListForTarget(List<Map<String,String>> list){
        List<Map<String,String>> resourceList = new ArrayList<>();
        if (target != null){
            List<ResourceRef> resourceRefs = null;
            Cluster cluster = domain.getClusterNamed(target);
            if (cluster != null) {
                resourceRefs=  cluster.getResourceRef();
            } else {
                Server server = domain.getServerNamed(target);
                if (server != null) {
                    resourceRefs = server.getResourceRef();
                }
            }
            if (resourceRefs != null && !resourceRefs.isEmpty()) {
                for (Map<String,String> m : list) {
                    String jndiName = m.get("name"); 
                    for (ResourceRef resource : resourceRefs) {
                        if (jndiName.equals(resource.getRef())) {
                            resourceList.add(m);
                        }
                    }
                }
            }
        }
        return resourceList;
    }
}



