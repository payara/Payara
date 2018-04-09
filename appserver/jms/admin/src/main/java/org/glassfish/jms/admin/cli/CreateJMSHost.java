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
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.RuntimeType;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.config.serverbeans.*;
import java.util.Properties;
import java.beans.PropertyVetoException;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Create Admin Object Command
 *
 */
@Service(name="create-jms-host")
@PerLookup
@I18n("create.jms.host")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class CreateJMSHost implements AdminCommand {

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateJMSHost.class);
    //[target target] [mqhost localhost] [mqport 7676] [mquser admin] [mqpassword admin] jms_host_name

    @Param(name="mqHost", alias="host", defaultValue="localhost")
    String mqhost;

    @Param(name="mqPort", alias="port", defaultValue="7676")
    String mqport;

    @Param(name="mqUser", alias="adminUserName", defaultValue="admin")
    String mquser;

    @Param(name="mqPassword", alias="adminPassword", defaultValue="admin")
    String mqpassword;

    @Param(name="property", optional=true, separator=':')
    Properties props;

    @Param(optional=true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    @Param(optional=true, defaultValue="false")
    Boolean force;

    @Param(name="jms_host_name", alias="host", primary=true)
    String jmsHostName;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    /*@Inject
    Configs configs;*/

    @Inject
    CommandRunner commandRunner;

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

        if (jmsHostName == null) {
            report.setMessage(localStrings.getLocalString("create.jms.host.noJmsHost",
                            "No JMS Host name specified."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        JmsService jmsservice = config.getExtensionByType(JmsService.class);
        /*for (Config c : configs.getConfig()) {

               if(configRef.equals(c.getName()))
                     jmsservice = c.getJmsService();
            }*/

        // ensure we don't already have one of this name before creating it.
        for (JmsHost jmsHost : jmsservice.getJmsHost()) {
                if (jmsHostName.equals(jmsHost.getName())) {
                    if (force) {
                        ActionReport deleteReport = report.addSubActionsReport();
                        ParameterMap parameters = new ParameterMap();
                         parameters.set("DEFAULT", jmsHostName);
                         parameters.set("target", target);
                        commandRunner.getCommandInvocation("delete-jms-host", deleteReport, context.getSubject()).parameters(parameters).execute();
                        if (ActionReport.ExitCode.FAILURE.equals(deleteReport.getActionExitCode())) {
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            return;
                        }
                        break;
                    } else {
                        report.setMessage(localStrings.getLocalString("create.jms.host.duplicate",
                                "A JMS Host named {0} already exists.", jmsHostName));
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return;
                    }
                }
            }

        try {
            ConfigSupport.apply(new SingleConfigCode<JmsService>() {
                public Object run(JmsService param) throws PropertyVetoException, TransactionFailure {

                    JmsHost jmsHost = param.createChild(JmsHost.class); //TODO: need a way to create a JmsHost instance
                    jmsHost.setAdminPassword(mqpassword);
                    jmsHost.setAdminUserName(mquser);
                    jmsHost.setName(jmsHostName);
                    jmsHost.setHost(mqhost);
                    jmsHost.setPort(mqport);
		    if(props != null)
		    {
		    	for (Map.Entry e: props.entrySet()){
				Property prop = jmsHost.createChild(Property.class);
				prop.setName((String)e.getKey());
				prop.setValue((String)e.getValue());
				jmsHost.getProperty().add(prop);
			}
		    }
                    param.getJmsHost().add(jmsHost);

                    return jmsHost;
                }
            }, jmsservice);
        } catch(TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("create.jms.host.fail",
                            "Unable to create jms host {0}.", jmsHostName) +
                            " " + tfe.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
        }
        report.setMessage(localStrings.getLocalString("create.jms.host.success",
                "Jms Host {0} created.", jmsHostName));
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

}
