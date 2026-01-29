/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.http.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import static fish.payara.ejb.http.admin.Constants.DEFAULT_USER_NAME;
import static fish.payara.ejb.http.admin.Constants.EJB_INVOKER_APP;
import static fish.payara.ejb.http.admin.Constants.ENDPOINTS_DIR;
import java.nio.file.Path;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.DEPLOYMENT_GROUP;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;
import org.glassfish.config.support.TargetType;
import org.glassfish.deployment.autodeploy.AutoDeployer;
import static org.glassfish.deployment.autodeploy.AutoDeployer.getNameFromFilePath;
import org.glassfish.deployment.autodeploy.AutoDeploymentOperation;
import org.glassfish.deployment.autodeploy.AutoUndeploymentOperation;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Gaurav Gupta
 */
@Service(name = "set-ejb-invoker-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@TargetType({CONFIG, DAS, DEPLOYMENT_GROUP, STANDALONE_INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean = EjbInvokerConfiguration.class,
            opType = POST,
            path = "set-ejb-invoker-configuration",
            description = "Sets the ejb-invoker configuration")
})
public class SetEjbInvokerConfigurationCommand implements AdminCommand {

    private static final Logger LOGGER = Logger.getLogger(SetEjbInvokerConfigurationCommand.class.getName());

    @Inject
    private Target targetUtil;

    @Param(optional = true)
    private Boolean enabled;

    @Param(optional = true)
    private String endpoint;

    @Param(optional = true, alias = "virtualservers")
    private String virtualServers;

    @Param(optional = true, alias = "securityenabled")
    protected Boolean securityEnabled;

    @Param(optional = true, alias = "realmname")
    protected String realmName;

    @Param(optional = true, alias = "authtype")
    protected String authType;

    @Param(optional = true, alias = "authmodule")
    protected String authModule;

    @Param(optional = true, alias = "authmoduleclass")
    protected String authModuleClass;

    @Param(optional = true)
    protected String roles;

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    @Inject
    protected CommandRunner commandRunner;

    @Inject
    private SecurityService securityService;

    @Inject
    private ServerEnvironment serverEnvironment;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Domain domain;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        Subject subject = context.getSubject();
        Config targetConfig = targetUtil.getConfig(target);
        EjbInvokerConfiguration config = targetConfig.getExtensionByType(EjbInvokerConfiguration.class);

        try {
            ConfigSupport.apply(configProxy -> {
                if (enabled != null) {
                    configProxy.setEnabled(enabled.toString());
                }
                if (endpoint != null) {
                    configProxy.setEndpoint(endpoint);
                }
                if (virtualServers != null) {
                    configProxy.setVirtualServers(virtualServers);
                }
                if (securityEnabled != null) {
                    configProxy.setSecurityEnabled(securityEnabled.toString());
                }
                if (realmName != null) {
                    configProxy.setRealmName(realmName);
                }
                if (authType != null) {
                    configProxy.setAuthType(authType);
                }
                if (authModule != null) {
                    configProxy.setAuthModule(authModule);
                }
                if (authModuleClass != null) {
                    if (StringUtils.ok(authModuleClass)
                            && authModuleClass.indexOf('.') == -1) {
                        actionReport.failure(
                                LOGGER,
                                "authModuleClass parameter value must be fully qualified class name."
                        );
                    }
                    configProxy.setAuthModuleClass(authModuleClass);
                }
                if (roles != null) {
                    configProxy.setRoles(roles);
                }
                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return configProxy;
            }, config);

        } catch (TransactionFailure ex) {
            actionReport.failure(LOGGER, "Failed to update EJB Invoker configuration", ex);
        }

        if (Boolean.parseBoolean(config.getSecurityEnabled())) {

            // If the required message security provider is not present, create it
            if (StringUtils.ok(config.getAuthModuleClass())) {
                String moduleClass = config.getAuthModuleClass();
                String moduleId = moduleClass.substring(moduleClass.lastIndexOf('.') + 1);
                ActionReport checkSecurityProviderReport = actionReport.addSubActionsReport();
                ActionReport createSecurityProviderReport = actionReport.addSubActionsReport();
                if (!messageSecurityProviderExists(moduleId, checkSecurityProviderReport,
                        context.getSubject())) {
                    createRequiredMessageSecurityProvider(moduleId, moduleClass, createSecurityProviderReport, subject);
                }
                if (checkSecurityProviderReport.hasFailures() || createSecurityProviderReport.hasFailures()) {
                    actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }

            // Create the default user if it doesn't exist
            if (!StringUtils.ok(config.getRealmName()) || config.getRealmName().equals("file")) {
                ActionReport checkUserReport = actionReport.addSubActionsReport();
                ActionReport createUserReport = actionReport.addSubActionsReport();
                if (!defaultUserExists(config, checkUserReport, subject)
                        && !checkUserReport.hasFailures()) {
                    createDefaultUser(config, createUserReport, subject);
                }
                if (checkUserReport.hasFailures() || createUserReport.hasFailures()) {
                    actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }

        if (enabled != null) {
            if (enabled) {
                enableEjbInvoker(actionReport);
            } else {
                disableEjbInvoker(actionReport);
            }
        } else if(Boolean.parseBoolean(config.getEnabled())) {
            actionReport.setMessage("Restart server or re-enable the ejb-invoker service for the change to take effect.");
        }

        // If everything has passed, scrap the subaction reports as we don't want to print them out
        if (!actionReport.hasFailures() && !actionReport.hasWarnings()) {
            actionReport.getSubActionsReport().clear();
        }
    }

    public void disableEjbInvoker(ActionReport report) {
        Path endPointsPath = serverEnvironment.getInstanceRoot().toPath().resolve(ENDPOINTS_DIR);
        Path ejbInvokerPath = endPointsPath.resolve(EJB_INVOKER_APP);

        AutoUndeploymentOperation autoUndeploymentOperation = AutoUndeploymentOperation.newInstance(
                serviceLocator,
                ejbInvokerPath.toFile(),
                getNameFromFilePath(endPointsPath.toFile(), ejbInvokerPath.toFile()),
                target);

        AutoDeployer.AutodeploymentStatus deploymentStatus = autoUndeploymentOperation.run();

        report.setActionExitCode(deploymentStatus.getExitCode());

        if (deploymentStatus.getExitCode().equals(ActionReport.ExitCode.FAILURE)) {
            if (domain.getApplications().getApplication(EJB_INVOKER_APP) == null) {
                report.appendMessage("\nEJB Invoker is not enabled on any target");
            } else {
                report.appendMessage("\nFailed to disable Ejb Invoker - was it enabled on the specified target?");
            }
        }
    }

    public void enableEjbInvoker(ActionReport report) {
        Path endPointsPath = serverEnvironment.getInstanceRoot().toPath().resolve(ENDPOINTS_DIR);
        Path ejbInvokerPath = endPointsPath.resolve(EJB_INVOKER_APP);
        Config targetConfig = targetUtil.getConfig(target);
        EjbInvokerConfiguration configuration = targetConfig.getExtensionByType(EjbInvokerConfiguration.class);

        AutoDeploymentOperation autoDeploymentOperation = AutoDeploymentOperation.newInstance(
                serviceLocator,
                ejbInvokerPath.toFile(),
                configuration.getVirtualServers(),
                target,
                configuration.getEndpoint()
        );

        if (domain.getApplications().getApplication(EJB_INVOKER_APP) == null) {
            AutoDeployer.AutodeploymentStatus deploymentStatus = autoDeploymentOperation.run();
            report.setActionExitCode(deploymentStatus.getExitCode());
        } else {
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
            report.setMessage("EJB Invoker is already deployed on at least one target, please edit it as you would a "
                    + "normal application using the create-application-ref, delete-application-ref, "
                    + "or update-application-ref commands");
        }
    }

    private boolean messageSecurityProviderExists(String authModule, ActionReport subActionReport, Subject subject) {
        boolean exists = false;

        CommandRunner.CommandInvocation invocation
                = commandRunner.getCommandInvocation(
                        "list-message-security-providers",
                        subActionReport,
                        subject,
                        false
                );

        ParameterMap parameters = new ParameterMap();
        parameters.add("layer", "HttpServlet");

        invocation.parameters(parameters).execute();

        for (ActionReport.MessagePart message : subActionReport.getTopMessagePart().getChildren()) {
            if (message.getMessage().equals(authModule)) {
                exists = true;
                break;
            }
        }

        return exists;
    }

    private void createRequiredMessageSecurityProvider(String authModule, String authModuleClass, ActionReport subActionReport, Subject subject) {
        CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("create-message-security-provider",
                subActionReport, subject, false);
        ParameterMap parameters = new ParameterMap();
        parameters.add("classname", authModuleClass);
        parameters.add("isdefaultprovider", "false");
        parameters.add("layer", "HttpServlet");
        parameters.add("providertype", "server");
        parameters.add("target", target);
        parameters.add("requestauthsource", "sender");
        parameters.add("DEFAULT", authModule);
        invocation.parameters(parameters).execute();
    }

    protected boolean defaultUserExists(EjbInvokerConfiguration config, ActionReport subActionReport, Subject subject) {
        CommandRunner.CommandInvocation invocation
                = commandRunner.getCommandInvocation(
                        "list-file-users",
                        subActionReport,
                        subject,
                        false
                );

        ParameterMap parameters = new ParameterMap();
        parameters.add("authrealmname",
                StringUtils.ok(config.getRealmName()) ? config.getRealmName() : securityService.getDefaultRealm());

        invocation.parameters(parameters).execute();

        for (ActionReport.MessagePart message : subActionReport.getTopMessagePart().getChildren()) {
            if (message.getMessage().equals(DEFAULT_USER_NAME)) {
                return true;
            }
        }

        return false;
    }

    protected void createDefaultUser(EjbInvokerConfiguration config, ActionReport subActionReport, Subject subject) {
        CommandRunner.CommandInvocation invocation
                = commandRunner.getCommandInvocation(
                        "create-file-user",
                        subActionReport,
                        subject,
                        false
                );

        ParameterMap parameters = new ParameterMap();
        parameters.add("groups", config.getRoles().replace(',', ':'));
        parameters.add("userpassword", DEFAULT_USER_NAME);
        parameters.add("target", target);
        parameters.add("authrealmname",
                StringUtils.ok(config.getRealmName()) ? config.getRealmName() : securityService.getDefaultRealm());
        parameters.add("DEFAULT", DEFAULT_USER_NAME);

        invocation.parameters(parameters).execute();
    }

}
