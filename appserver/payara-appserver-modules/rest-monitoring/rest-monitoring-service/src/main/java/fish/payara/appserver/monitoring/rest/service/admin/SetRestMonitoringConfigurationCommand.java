/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.appserver.monitoring.rest.service.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import fish.payara.appserver.monitoring.rest.service.configuration.RestMonitoringConfiguration;
import fish.payara.appserver.monitoring.rest.service.security.RestMonitoringAuthModule;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "set-rest-monitoring-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean = RestMonitoringConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-rest-monitoring-configuration",
            description = "Sets the Rest Monitoring Configuration")
})
public class SetRestMonitoringConfigurationCommand implements AdminCommand {

    private static final String AUTH_MODULE_NAME = "RestMonitoringAuthModule";
    private static final String DEFAULT_USER_NAME = "payara";
    private static final Logger LOGGER = Logger.getLogger(SetRestMonitoringConfigurationCommand.class.getName());
    
    @Param(optional = true, defaultValue = "server-config")
    String target;
    
    @Param(optional = true)
    Boolean enabled;
    
    @Param(optional = true, alias = "contextroot")
    String contextRoot;
    
    @Param(optional = true, alias = "applicationname")
    String applicationName;
    
    @Param(optional = true, alias = "securityenabled")
    Boolean securityEnabled;
    
    @Inject
    private Target targetUtil;
    
    @Inject
    ServiceLocator habitat;
    
    @Inject
    ServerEnvironment serverEnv;
    
    @Inject
    CommandRunner commandRunner;
    
    @Inject
    Domain domain;
    
    @Override
    public void execute(AdminCommandContext context) {
        Config targetConfig = targetUtil.getConfig(target);
        RestMonitoringConfiguration restMonitoringConfiguration = targetConfig.getExtensionByType(RestMonitoringConfiguration.class);    
        
        ActionReport actionReport = context.getActionReport();
        Subject subject = context.getSubject();
        
        // If the required message security provider is not present, create it
        if (!messageSecurityProviderExists(actionReport.addSubActionsReport(), 
                context.getSubject())) {
            createRequiredMessageSecurityProvider(actionReport.addSubActionsReport(), subject);
        }
        
        // Create the default user if it doesn't exist
        if (!defaultRestMonitoringUserExists(actionReport.addSubActionsReport(), subject)) {
            createDefaultRestMonitoringUser(actionReport.addSubActionsReport(), subject);
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<RestMonitoringConfiguration>(){
                    @Override
                    public Object run(RestMonitoringConfiguration configProxy) throws PropertyVetoException {
                        if (enabled != null) {
                            configProxy.setEnabled(enabled.toString());
                        }
                        
                        if (contextRoot != null) {
                            configProxy.setContextRoot(contextRoot);
                        }
                        
                        if (applicationName != null) {
                            configProxy.setApplicationName(applicationName);
                        }
                        
                        if (securityEnabled != null) {
                            configProxy.setSecurityEnabled(securityEnabled.toString());
                        }
                        
                        return null;
                    }
            }, restMonitoringConfiguration);
        } catch (TransactionFailure ex) {
            context.getActionReport().failure(LOGGER, "Failed to update Rest Monitoring configuration", ex);
        }
        
        // If security is enabled but secure admin isn't, prompt a warning
        if (Boolean.parseBoolean(restMonitoringConfiguration.getSecurityEnabled())
                && !SecureAdmin.Util.isEnabled(domain.getSecureAdmin())) {
            actionReport.appendMessage("\n---WARNING---\nSecure Admin is not enabled! - it is highly "
                    + "recommended that you do so to properly secure the Rest Monitoring service.\n");
        }
        
        // If everything has passed, scrap the subaction reports as we don't want to print them out
        if (!actionReport.hasFailures() || !actionReport.hasWarnings()) {
            actionReport.getSubActionsReport().clear();
        }
    }
    
    private boolean messageSecurityProviderExists(ActionReport subActionReport, Subject subject) {
        boolean exists = false;
        
        CommandInvocation invocation = commandRunner.getCommandInvocation("list-message-security-providers", 
                subActionReport, subject, false);
        
        ParameterMap parameters = new ParameterMap();
        parameters.add("layer", "HttpServlet");
        
        invocation.parameters(parameters).execute();
        
        for (MessagePart message : subActionReport.getTopMessagePart().getChildren()) {
            if (message.getMessage().equals(AUTH_MODULE_NAME)) {
                exists = true;
                break;
            }
        }
        
        return exists;
    }
     
    private void createRequiredMessageSecurityProvider(ActionReport subActionReport, Subject subject) {
        CommandInvocation invocation = commandRunner.getCommandInvocation("create-message-security-provider", 
                subActionReport, subject, false);
         
        ParameterMap parameters = new ParameterMap();
        parameters.add("classname", RestMonitoringAuthModule.class.getName());
        parameters.add("isdefaultprovider", "false");
        parameters.add("layer", "HttpServlet");
        parameters.add("providertype", "server");
        parameters.add("target", target);
        parameters.add("requestauthsource", "sender");
        parameters.add("DEFAULT", AUTH_MODULE_NAME);

        
        invocation.parameters(parameters).execute();
     }
    
    private boolean defaultRestMonitoringUserExists(ActionReport subActionReport, Subject subject) {
        boolean exists = false;
        
        CommandInvocation invocation = commandRunner.getCommandInvocation("list-file-users", subActionReport, subject, 
                false);
        
        ParameterMap parameters = new ParameterMap();
        parameters.add("authrealmname", "file");
        
        invocation.parameters(parameters).execute();
        
        for (MessagePart message : subActionReport.getTopMessagePart().getChildren()) {
            if (message.getMessage().equals(DEFAULT_USER_NAME)) {
                exists = true;
                break;
            }
        }
        
        return exists;
    }
    
    private void createDefaultRestMonitoringUser(ActionReport subActionReport, Subject subject) {
        CommandInvocation invocation = commandRunner.getCommandInvocation("create-file-user", subActionReport, subject, 
                false);
         
        ParameterMap parameters = new ParameterMap();
        parameters.add("groups", "rest-monitoring");
        parameters.add("userpassword", "rest");
        parameters.add("target", target);
        parameters.add("authrealmname", "file");
        parameters.add("DEFAULT", DEFAULT_USER_NAME);
        
        invocation.parameters(parameters).execute();
    }
}
