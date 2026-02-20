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

import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.internal.api.ServerContext;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.config.serverbeans.*;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import org.glassfish.api.admin.*;

import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;


/**
 * Flush JMS Destination
 *
 * 
 */
@Service(name="flush-jmsdest")
@PerLookup
@I18n("flush.jms.dest")
@org.glassfish.api.admin.ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.POST, 
        path="flush-jmsdest", 
        description="Flush",
        params={
            @RestParam(name="target", value="$parent")
        })
})
public class FlushJMSDestination extends JMSDestination implements AdminCommand {

    private static final Logger logger = Logger.getLogger(LogUtils.JMS_ADMIN_LOGGER);
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(FlushJMSDestination.class);
    private static final String DESTINATION_CONFIG_DOMAIN_TYPE = MBEAN_DOMAIN_NAME + ":type=Destination,subtype=Config";

    @Param(name = "destType", shortName = "T", optional = false)
    String destType;

    @Param(name = "dest_name", primary = true)
    String destName;

    @Param(optional = true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    @Inject
    com.sun.appserv.connectors.internal.api.ConnectorRuntime connectorRuntime;

    @Inject
    Domain domain;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    ServerContext serverContext;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        logger.entering(getClass().getName(), "flushJMSDestination", new Object[]{destName, destType});

        try {
            validateJMSDestName(destName);
            validateJMSDestType(destType);
        } catch (IllegalArgumentException e) {
            report.setMessage(e.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            flushJMSDestination(destName, destType, target);
        } catch (Exception e) {
            logger.throwing(getClass().getName(), "flushJMSDestination", e);
            report.setMessage(localStrings.getLocalString("flush.jms.dest.failed", "Flush JMS Destination failed", e.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    // delete-jmsdest
    private void flushJMSDestination(String destName, String destType, String tgtName) throws Exception {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "FlushJMSDestination ...");
        }

        // check and use JMX
        try {
            purgeJMSDestination(destName, destType, tgtName);
        } catch (Exception e) {
            logger.throwing(getClass().getName(), "flushJMSDestination", e);
            handleException(e);
        }
    }

    public void purgeJMSDestination(String destName, String destType, String tgtName) throws Exception {
        logger.log(Level.FINE, "purgeJMSDestination ...");
        MQJMXConnectorInfo[] mqInfos = getMQJMXConnectorInfos(target, config, serverContext, domain, connectorRuntime);

        if (mqInfos != null && mqInfos.length > 0) {
            for (MQJMXConnectorInfo mqInfo : mqInfos) {
                try {

                    MBeanServerConnection mbsc = mqInfo.getMQMBeanServerConnection();

                    if (destType.equalsIgnoreCase("topic")) {
                        destType = DESTINATION_TYPE_TOPIC;
                    } else if (destType.equalsIgnoreCase("queue")) {
                        destType = DESTINATION_TYPE_QUEUE;
                    }
                    ObjectName on = createDestinationConfig(destType, destName);

                    mbsc.invoke(on, "purge", null, null);
                } catch (Exception e) {
                    //log JMX Exception trace as WARNING
                    logAndHandleException(e, "admin.mbeans.rmb.error_purging_jms_dest");
                } finally {
                    try {
                        if (mqInfo != null) {
                            mqInfo.closeMQMBeanServerConnection();
                        }
                    } catch (Exception e) {
                        handleException(e);
                    }
                }
            }
        }
    }

    private ObjectName createDestinationConfig(String destinationType, String destinationName)
            throws MalformedObjectNameException, NullPointerException {
        String s = DESTINATION_CONFIG_DOMAIN_TYPE
                + ",desttype="
                + destinationType
                + ",name="
                + ObjectName.quote(destinationName);

        ObjectName o = new ObjectName(s);

        return (o);
    }

}
