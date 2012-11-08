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

package com.sun.enterprise.connectors.jms.system;

import java.util.*;
import java.util.logging.*;
import java.beans.PropertyChangeEvent;

import javax.inject.Inject;
import javax.inject.Named;

import com.sun.enterprise.connectors.jms.config.JmsHost;
import com.sun.enterprise.connectors.jms.config.JmsService;
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.connectors.jms.util.JmsRaUtil;
import org.glassfish.api.admin.ServerEnvironment;

@Service
public class JMSConfigListener implements ConfigListener{
    // Injecting @Configured type triggers the corresponding change
    // events to be sent to this instance

    private JmsService jmsService;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config serverConfig;
   
    @Inject
    private Servers servers;

    //private Cluster cluster;
    private ActiveJmsResourceAdapter aresourceAdapter;

    private static final Logger _logger = LogDomains.getLogger(
            JMSConfigListener.class, LogDomains.JMS_LOGGER);

    // String Manager for Localization
    private static StringManager sm
        = StringManager.getManager(JMSConfigListener.class);

    public void setActiveResourceAdapter(ActiveJmsResourceAdapter aresourceAdapter) {
        this.aresourceAdapter = aresourceAdapter;
    }


        /** Implementation of org.jvnet.hk2.config.ConfigListener */
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        //Events that we can't process now because they require server restart.
        jmsService = serverConfig.getExtensionByType(JmsService.class);
        List<UnprocessedChangeEvent> unprocessedEvents = new ArrayList<UnprocessedChangeEvent>();
        _logger.log(Level.FINE, "In JMSConfigListener - received config event");
        Domain domain = Globals.get(Domain.class);
        String jmsProviderPort = null;
        ServerContext serverContext = Globals.get(ServerContext.class);
        Server thisServer = domain.getServerNamed(serverContext.getInstanceName());
        //if(thisServer.isDas() || thisServer.getCluster() == null)
        {
          //  _logger.log(Level.FINE,"JMSConfigListerner server is either das or a stand-alone instance - hence ignoring");
            //return null;
        }
        for (int i=0; i< events.length; i++) {
        //for (PropertyChangeEvent event : events) {
            PropertyChangeEvent event = events[i];
            String eventName = event.getPropertyName();
            Object oldValue = event.getOldValue();
            Object newValue = event.getNewValue();

         if(event.getSource().toString().indexOf("config.serverbeans.JmsService") != -1)   {
             UnprocessedChangeEvent uchangeEvent = new UnprocessedChangeEvent(event, "restart required");
             unprocessedEvents.add(uchangeEvent);
         }

         else if(event.getSource().toString().indexOf("config.serverbeans.JmsHost") != -1)   {
             UnprocessedChangeEvent uchangeEvent = new UnprocessedChangeEvent(event, "restart required");
             unprocessedEvents.add(uchangeEvent);
         }
        _logger.log(Level.FINE, "In JMSConfigListener " + eventName + oldValue + newValue);

        if (oldValue != null && oldValue.equals(newValue)) {
           _logger.log(Level.FINE, "Event " + eventName
                        + " did not change existing value of " + oldValue);
           continue;
        }
        if ("JMS_PROVIDER_PORT".equals(newValue)){
            //The value is in the next event
            PropertyChangeEvent nextevent = events[i+1] ;
            jmsProviderPort = (String) nextevent.getNewValue();
        }
        if(event.getSource() instanceof JmsService ) {
           if (eventName.equals(ServerTags.MASTER_BROKER)) {
                     String oldMB = oldValue != null ? oldValue.toString() : null;
                     String newMB = newValue != null ? newValue.toString(): null;

            _logger.log(Level.FINE, "Got JmsService Master Broker change event "
                + event.getSource() + " "
                + eventName + " " + oldMB + " " + newMB);

             if (newMB != null) {
                 Server newMBServer = domain.getServerNamed(newMB);
                 if(newMBServer != null)
                 {
                     Node node = domain.getNodeNamed(newMBServer.getNodeRef());
                     String newMasterBrokerPort = JmsRaUtil.getJMSPropertyValue(newMBServer);
                     if(newMasterBrokerPort == null) newMasterBrokerPort = getDefaultJmsHost(jmsService).getPort();
                     String newMasterBrokerHost = node.getNodeHost();
                     aresourceAdapter.setMasterBroker(newMasterBrokerHost + ":" + newMasterBrokerPort);
                 }
             }
            }
        }   if (eventName.equals(ServerTags.SERVER_REF)){
                //if(event instanceof ServerRef){
                    String oldServerRef = oldValue != null ? oldValue.toString() : null;
                    String newServerRef = newValue != null ? newValue.toString(): null;
                    if(oldServerRef  != null && newServerRef == null && !thisServer.isDas()) {//instance has been deleted
                        _logger.log(Level.FINE, "Got Cluster change event for server_ref"
                            + event.getSource() + " "
                        + eventName + " " + oldServerRef + " " + null);
                        String url = getBrokerList();
                        aresourceAdapter.setClusterBrokerList(url);
                        break;
                   }//
             } // else skip
            if (event.getSource() instanceof Server) {
               _logger.log(Level.FINE, "In JMSConfigListener - recieved cluster event " + event.getSource());
               Server changedServer = (Server) event.getSource();
               if (thisServer.isDas())return null;

               if(jmsProviderPort != null){
                    String nodeName = changedServer.getNodeRef();
                    String nodeHost = null;

                   if(nodeName != null)
                      nodeHost = domain.getNodeNamed(nodeName).getNodeHost();
                   String url = getBrokerList();
                   url = url + ",mq://" + nodeHost + ":" + jmsProviderPort;
                   aresourceAdapter.setClusterBrokerList(url);
                   break;
               }

            }

         }
            return unprocessedEvents.size() > 0 ? new UnprocessedChangeEvents(unprocessedEvents) : null;
        }
    private String getBrokerList(){
        MQAddressList addressList = new MQAddressList();
        try{
            addressList.setup(true);
        }catch(Exception ex){
            _logger.log(Level.WARNING, "failed to create addresslist " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        return addressList.toString();


    }
     private JmsHost getDefaultJmsHost(JmsService jmsService){

            JmsHost jmsHost = null;
                String defaultJmsHostName = jmsService.getDefaultJmsHost();
                List <JmsHost> jmsHostsList = jmsService.getJmsHost();

                for (int i=0; i < jmsHostsList.size(); i ++)
                {
                   JmsHost tmpJmsHost = (JmsHost) jmsHostsList.get(i);
                   if (tmpJmsHost != null && tmpJmsHost.getName().equals(defaultJmsHostName))
                         jmsHost = tmpJmsHost;
                }
            if (jmsHost == null && jmsHostsList.size() >0)
                jmsHost = jmsHostsList.get(0);
            return jmsHost;
          }
}
