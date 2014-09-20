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

import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.internal.api.ServerContext;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

import com.sun.enterprise.connectors.jms.system.ActiveJmsResourceAdapter;
import com.sun.enterprise.config.serverbeans.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

/**
 * delete JMS Destination
 *
 */
@Service(name="delete-jmsdest")
@PerLookup
@I18n("delete.jms.dest")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.DELETE, 
        path="delete-jmsdest", 
        description="Delete JMS Destination",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.DELETE, 
        path="delete-jmsdest", 
        description="Delete JMS Destination",
        params={
            @RestParam(name="target", value="$parent")
        })
})

public class DeleteJMSDestination extends JMSDestination implements AdminCommand {

        private final Logger logger = Logger.getLogger(LogUtils.JMS_ADMIN_LOGGER);
        final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteJMSDestination.class);

        @Param(name="destType", shortName="T", optional=false)
        String destType;

        @Param(name="dest_name", primary=true)
        String destName;

        @Param(optional=true)
        String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

        @Inject
        com.sun.appserv.connectors.internal.api.ConnectorRuntime connectorRuntime;

        @Inject
        Domain domain;

        @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
        Config config;

        @Inject
        ServerContext serverContext;


     public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        logger.entering(getClass().getName(), "deleteJMSDestination",
        new Object[] {destName, destType});

		 try{
            validateJMSDestName(destName);
            validateJMSDestType(destType);
        }catch (IllegalArgumentException e){
            report.setMessage(e.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
                deleteJMSDestination(destName, destType, target);
                return;
        } catch (Exception e) {
            logger.throwing(getClass().getName(), "deleteJMSDestination", e);
            //e.printStackTrace();//handleException(e);
            report.setMessage(localStrings.getLocalString("delete.jms.dest.noJmsDelete",
                            "Delete JMS Destination failed. Please verify if the JMS Destination specified for deletion exists"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
     }

       // delete-jmsdest
	private Object deleteJMSDestination(String destName, String destType,
		String tgtName)
		throws Exception {

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "deleteJMSDestination ...");
        }
        MQJMXConnectorInfo mqInfo = getMQJMXConnectorInfo(target, config, serverContext, domain, connectorRuntime);

		//MBeanServerConnection  mbsc = getMBeanServerConnection(tgtName);

		try {
			MBeanServerConnection mbsc = mqInfo.getMQMBeanServerConnection();
			ObjectName on = new ObjectName(
				DESTINATION_MANAGER_CONFIG_MBEAN_NAME);
			String [] signature = null;
			Object [] params = null;

			signature = new String [] {
				"java.lang.String",
				"java.lang.String"};

			if (destType.equalsIgnoreCase("topic")) {
				destType = DESTINATION_TYPE_TOPIC;
			} else if (destType.equalsIgnoreCase("queue")) {
				destType = DESTINATION_TYPE_QUEUE;
			}
			params = new Object [] {destType, destName};
			return mbsc.invoke(on, "destroy", params, signature);

        } catch (Exception e) {
                   //log JMX Exception trace as WARNING
                   logAndHandleException(e, "admin.mbeans.rmb.error_deleting_jms_dest");
                } finally {
                    try {
                        if(mqInfo != null) {
                            mqInfo.closeMQMBeanServerConnection();
                        }
                    } catch (Exception e) {
                      handleException(e);
                    }
                }
           return null;
    }
}
