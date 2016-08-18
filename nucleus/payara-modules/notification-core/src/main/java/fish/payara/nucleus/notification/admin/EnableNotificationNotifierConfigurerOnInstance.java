/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.notification.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.NotificationEventBus;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierConfiguration;
import fish.payara.nucleus.notification.domain.execoptions.NotifierConfigurationExecutionOptions;
import fish.payara.nucleus.notification.domain.execoptions.NotifierConfigurationExecutionOptionsFactory;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Susan Rai
 */
@ExecuteOn({RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "__enable-notification-configure-notifier-instance")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("__enable-notification-configure-notifier-instance")
public class EnableNotificationNotifierConfigurerOnInstance implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(EnableNotificationNotifierConfigurerOnInstance.class);

    @Inject
    NotificationService service;

    @Inject
    ServiceLocator habitat;

    @Inject
    protected Logger logger;

    @Inject
    protected Target targetUtil;

    @Inject
    private NotifierConfigurationExecutionOptionsFactory factory;

    @Inject
    private NotificationEventBus eventBus;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "notifierName", optional = true, defaultValue = "service-log")
    private String notifierName;

    @Param(name = "notifierEnabled", optional = false)
    private Boolean notifierEnabled;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        Config config = targetUtil.getConfig(target);

        final BaseNotifierService notifierService = habitat.getService(BaseNotifierService.class, notifierName);
        if (notifierService == null) {
            actionReport.appendMessage(strings.getLocalString("requesttracing.notifier.configure.status.error",
                    "Notifier with name {0} could not be found.", notifierName));
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        final NotificationServiceConfiguration notificationServiceConfiguration = config.getExtensionByType(NotificationServiceConfiguration.class);
        final NotifierConfiguration notifier = notificationServiceConfiguration.getNotifierConfigurationByType(notifierService.getNotifierConfigType());

        try {
            if (notifier == null) {
                final NotifierConfiguration[] createdNotifier = {null};
                ConfigSupport.apply(new SingleConfigCode<NotificationServiceConfiguration>() {
                    @Override
                    public Object run(final NotificationServiceConfiguration notificationServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        NotifierConfiguration notifierProxy = (NotifierConfiguration) notificationServiceConfigurationProxy.createChild(notifierService.getNotifierConfigType());
                        createdNotifier[0] = notifierProxy;

                        List<NotifierConfiguration> notifierConfigList = notificationServiceConfigurationProxy.getNotifierConfigurationList();
                        NotifierConfigurationExecutionOptions executionOptions = factory.build(createdNotifier[0]);
                        if (notifierEnabled) {
                            notifierConfigList.add(createdNotifier[0]);
                            service.getExecutionOptions().addNotifierConfigurationExecutionOption(executionOptions);
                            eventBus.register(notifierService);
                        } else {
                            notifierConfigList.remove(createdNotifier[0]);
                            service.getExecutionOptions().removeNotifierConfigurationExecutionOption(executionOptions);
                            eventBus.unregister(notifierService);
                        }

                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return notificationServiceConfigurationProxy;
                    }
                }, notificationServiceConfiguration);
            } else {
                ConfigSupport.apply(new SingleConfigCode<NotifierConfiguration>() {
                    @Override
                    public Object run(final NotifierConfiguration notifierProxy) throws
                            PropertyVetoException, TransactionFailure {
                        NotifierConfigurationExecutionOptions executionOptions = factory.build(notifierProxy);
                        if (notifierEnabled) {
                            service.getExecutionOptions().addNotifierConfigurationExecutionOption(executionOptions);
                            eventBus.register(notifierService);
                        } else {
                            service.getExecutionOptions().removeNotifierConfigurationExecutionOption(executionOptions);
                            eventBus.unregister(notifierService);
                        }

                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return notifierProxy;
                    }
                }, notifier);

            }

            actionReport.appendMessage(strings.getLocalString("notification.configure.notifier.added.configured",
                    "Notifier with name {0} is registered and set enabled to {1}.", notifierName, notifierEnabled) + "\n");
        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
}
