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
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.api.admin.RuntimeType;
import java.util.ArrayList;

import javax.inject.Inject;

import org.glassfish.api.admin.*;

/**
 * List JMS Hosts command
 *
 */
@Service(name="list-jms-hosts")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.jms.hosts")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=JmsService.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-jms-hosts", 
        description="list-jms-hosts")
})
public class ListJMSHosts implements AdminCommand {
        final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListJMSHosts.class);

    @Param(name="target", optional=true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    //@Inject(name = ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    Domain domain;
    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        Config targetConfig = domain.getConfigNamed(target);
                if (targetConfig != null)
                    config = targetConfig;
                
        Server targetServer = domain.getServerNamed(target);
        //String configRef = targetServer.getConfigRef();
        if (targetServer!=null) {
            config = domain.getConfigNamed(targetServer.getConfigRef());
        }
        com.sun.enterprise.config.serverbeans.Cluster cluster =domain.getClusterNamed(target);
        if (cluster!=null) {
            config = domain.getConfigNamed(cluster.getConfigRef());
        }

        JmsService jmsService = config.getExtensionByType(JmsService.class);
            /*for (Config c : configs.getConfig()) {
                if(configRef.equals(c.getName()))
                     jmsService = c.getJmsService();
            } */

            if (jmsService == null) {
            report.setMessage(localStrings.getLocalString("list.jms.host.invalidTarget",
                            "Invalid Target specified."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
          }
           try {
            ArrayList<String> list = new ArrayList();
            for (JmsHost r : jmsService.getJmsHost()) {
                list.add(r.getName());
            }

            for (String jmsName : list) {
                final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                part.setMessage(jmsName);
            }
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("list.jms.host.fail",
                    "Unable to list JMS Hosts") + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
