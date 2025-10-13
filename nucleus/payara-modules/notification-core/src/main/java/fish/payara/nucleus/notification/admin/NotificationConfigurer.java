/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016-2021 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://github.com/payara/Payara/blob/master/LICENSE.txt
 See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 */
package fish.payara.nucleus.notification.admin;

import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import fish.payara.internal.notification.admin.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.NotificationService;


/**
 * Admin command to enable/disable all notification services defined in
 * domain.xml.
 *
 * @author mertcaliskan
 */
@Service(name = "notification-configure")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("notification.configure")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = NotificationServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "notification-configure",
            description = "Enables/Disables Notification Service")
})
public class NotificationConfigurer implements AdminCommand {

    @Inject
    private NotificationService service;
    
    @Inject
    ServerEnvironment server;
    
    @Inject
    protected Logger logger;

    @Inject
    protected Target targetUtil;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Inject
    ServiceLocator serviceLocator;

    CommandRunner.CommandInvocation inv;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        Config config = targetUtil.getConfig(target);
        final NotificationServiceConfiguration notificationServiceConfiguration = config.getExtensionByType(NotificationServiceConfiguration.class);

        if (notificationServiceConfiguration != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<NotificationServiceConfiguration>() {
                    @Override
                    public Object run(final NotificationServiceConfiguration notificationServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        if (enabled != null) {
                            notificationServiceConfigurationProxy.enabled(enabled.toString());
                        }
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return notificationServiceConfigurationProxy;
                    }
                }, notificationServiceConfiguration);
            }
            catch(TransactionFailure ex){
                logger.log(Level.WARNING, "Exception during command ", ex);
                actionReport.setMessage(ex.getCause().getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        if (dynamic) {
            if (server.isDas()) {
                if (targetUtil.getConfig(target).isDas()) {
                    service.bootstrapNotificationService();
                }
            } else {
                // apply as not the DAS so implicitly it is for us
                service.bootstrapNotificationService();
            }
        }
    }
}
