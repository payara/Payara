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

import com.sun.enterprise.connectors.jms.config.JmsHost;
import com.sun.enterprise.connectors.jms.config.JmsService;
import org.glassfish.api.I18n;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

import java.beans.PropertyVetoException;

import javax.inject.Inject;

import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * List Delete Jms Hosts command
 *
 */
@Service(name="delete-jms-host")
@PerLookup
@I18n("delete.jms.host")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=JmsHost.class,
        opType=RestEndpoint.OpType.DELETE, 
        path="delete-jms-host", 
        description="Delete JMS Host",
        params={
            @RestParam(name="id", value="$parent")
        })
})
public class DeleteJMSHost implements AdminCommand {
        final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteJMSHost.class);

    @Param(optional=true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    @Param(name="jms_host_name", primary=true)
    String jmsHostName;

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
        if (targetServer!=null) {
            config = domain.getConfigNamed(targetServer.getConfigRef());
        }
        com.sun.enterprise.config.serverbeans.Cluster cluster =domain.getClusterNamed(target);
        if (cluster!=null) {
            config = domain.getConfigNamed(cluster.getConfigRef());
        }

         if (jmsHostName == null) {
            report.setMessage(localStrings.getLocalString("delete.jms.host.noHostName",
                            "No JMS Host Name specified for JMS Host."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

            JmsService jmsService = config.getExtensionByType(JmsService.class);
           /* for (Config c : configs.getConfig()) {

               if(configRef.equals(c.getName()))
                     jmsService = c.getJmsService();
            }*/

            if (jmsService == null) {
            report.setMessage(localStrings.getLocalString("list.jms.host.invalidTarget",
                            "Invalid Target specified."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
            JmsHost jmsHost = null;
            for (JmsHost r : jmsService.getJmsHost()) {
                if(jmsHostName.equals(r.getName())){
                    jmsHost = r;
                    break;
                }
            }
           if (jmsHost == null) {
            report.setMessage(localStrings.getLocalString("list.jms.host.noJmsHostFound",
                            "JMS Host {0} does not exist.", jmsHostName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        final JmsHost jHost = jmsHost;
         try {
            ConfigSupport.apply(new SingleConfigCode<JmsService>() {
                public Object run(JmsService param) throws PropertyVetoException, TransactionFailure {
                    return param.getJmsHost().remove(jHost);
                }
            }, jmsService);
        } catch(TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("delete.jms.host.fail",
                            "Unable to delete jms host {0}.", jmsHostName) +
                            " " + tfe.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
