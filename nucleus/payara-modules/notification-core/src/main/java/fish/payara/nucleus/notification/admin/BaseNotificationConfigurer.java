/*
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package fish.payara.nucleus.notification.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierConfiguration;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.lang.reflect.ParameterizedType;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 */
public abstract class BaseNotificationConfigurer<C extends NotifierConfiguration, S extends BaseNotifierService> implements AdminCommand {

    @Inject
    protected Target targetUtil;

    @Inject
    protected ServerEnvironment server;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    protected Logger logger;

    @Inject
    protected NotificationService notificationService;

    @Inject
    private S service;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    @Param(name = "enabled")
    protected Boolean enabled;

    @Param(name = "noisy", optional = true, defaultValue = "true")
    protected Boolean noisy;

    private Class<C> notifierConfigurationClass;

    protected abstract void applyValues(C c) throws PropertyVetoException;
    protected abstract String getHealthCheckNotifierCommandName();
    protected abstract String getRequestTracingNotifierCommandName();

    @Override
    public void execute(final AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        Config configuration = targetUtil.getConfig(target);
        final NotificationServiceConfiguration notificationServiceConfiguration = configuration.getExtensionByType(NotificationServiceConfiguration.class);
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        notifierConfigurationClass = (Class<C>) genericSuperclass.getActualTypeArguments()[0];

        C c = notificationServiceConfiguration.getNotifierConfigurationByType(notifierConfigurationClass);

        try {
            if (c == null) {
                ConfigSupport.apply(new SingleConfigCode<NotificationServiceConfiguration>() {
                    @Override
                    public Object run(final NotificationServiceConfiguration notificationServiceConfigurationProxy)
                            throws PropertyVetoException, TransactionFailure {
                        C c = notificationServiceConfigurationProxy.createChild(notifierConfigurationClass);
                        applyValues(c);
                        notificationServiceConfigurationProxy.getNotifierConfigurationList().add(c);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);

                        ParameterMap params = new ParameterMap();
                        params.add("enabled", Boolean.TRUE.toString());
                        params.add("noisy", noisy.toString());
                        params.add("dynamic", Boolean.TRUE.toString());
                        params.add("target", target);

                        ActionReport healthCheckSubReport = actionReport.addSubActionsReport();
                        CommandRunner.CommandInvocation healthCheckCommandInvocation =
                                commandRunner.getCommandInvocation(getHealthCheckNotifierCommandName(), healthCheckSubReport, context.getSubject());
                        healthCheckCommandInvocation.parameters(params);
                        healthCheckCommandInvocation.execute();
                        if (healthCheckSubReport.hasFailures()) {
                            logger.log(Level.SEVERE, "Error occurred while configuring notifier with command: " + getHealthCheckNotifierCommandName());
                        }

                        ActionReport requestTracingSubReport = actionReport.addSubActionsReport();
                        CommandRunner.CommandInvocation requestTracingCommandInvocation = commandRunner.getCommandInvocation(getRequestTracingNotifierCommandName(), requestTracingSubReport, context.getSubject());
                        params.remove("noisy");
                        requestTracingCommandInvocation.parameters(params);
                        requestTracingCommandInvocation.execute();
                        if (requestTracingSubReport.hasFailures()) {
                            logger.log(Level.SEVERE, "Error occurred while configuring notifier with command: " + getRequestTracingNotifierCommandName());
                        }

                        return notificationServiceConfigurationProxy;
                    }
                }, notificationServiceConfiguration);
            }
            else {
                ConfigSupport.apply(new SingleConfigCode<C>() {
                    public Object run(C cProxy) throws PropertyVetoException, TransactionFailure {
                        applyValues(cProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        // If noisy attribute has changed must refresh HealthCheck notificatonList
                        ParameterMap params = new ParameterMap();
                        params.add("enabled", enabled.toString());
                        params.add("dynamic", Boolean.TRUE.toString());
                        params.add("noisy", noisy.toString());
                        params.add("target", target);

                        ActionReport healthCheckSubReport = actionReport.addSubActionsReport();
                        CommandRunner.CommandInvocation healthCheckCommandInvocation = commandRunner.getCommandInvocation(getHealthCheckNotifierCommandName(), healthCheckSubReport, context.getSubject());
                        healthCheckCommandInvocation.parameters(params);
                        healthCheckCommandInvocation.execute();
                        if (healthCheckSubReport.hasFailures()) {
                            logger.log(Level.SEVERE, "Error occurred while configuring notifier with command: " + getHealthCheckNotifierCommandName());
                        }
                        return cProxy;
                    }
                }, c);
            }

            if (dynamic) {
                if (server.isDas()) {
                    if (targetUtil.getConfig(target).isDas()) {
                         configureDynamically();
                    }
                }
                else {
                    configureDynamically();
                }
            }
        }
        catch(TransactionFailure ex){
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    protected void configureDynamically() {
        notificationService.bootstrapNotificationService();
        service.shutdown();
        service.bootstrap();
    }
}