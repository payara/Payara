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
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.domain.NotifierConfigurationExecutionOptions;
import fish.payara.nucleus.notification.service.NotifierConfigurationExecutionOptionsFactoryStore;
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
import org.jvnet.hk2.config.ConfigView;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin command to enable/disable specific notifier given with its name
 *
 * @author mertcaliskan
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "notification-configure-notifier")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("notification.configure.notifier")
@RestEndpoints({
    @RestEndpoint(configBean = NotificationServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "notification-configure-notifier",
            description = "Enables/Disables Notifier Specified With Name")
})
@Deprecated
public class NotificationNotifierConfigurer implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(NotificationNotifierConfigurer.class);

    @Inject
    NotificationService service;

    @Inject
    ServerEnvironment server;

    @Inject
    ServiceLocator habitat;

    @Inject
    protected Logger logger;

    @Inject
    protected Target targetUtil;

    @Inject
    private NotificationEventBus eventBus;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "notifierName", optional = true, defaultValue = "service-log")
    private String notifierName;

    @Param(name = "notifierEnabled", optional = false)
    private Boolean notifierEnabled;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    NotifierConfigurationExecutionOptionsFactoryStore factoryStore;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        final AdminCommandContext theContext = context;
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
                        if (notifierEnabled != null) {
                            notifierProxy.enabled(notifierEnabled);
                        }
                        notificationServiceConfigurationProxy.getNotifierConfigurationList().add(notifierProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return notificationServiceConfigurationProxy;
                    }
                }, notificationServiceConfiguration);
            } else {
                ConfigSupport.apply(new SingleConfigCode<NotifierConfiguration>() {
                    @Override
                    public Object run(final NotifierConfiguration notifierProxy) throws
                            PropertyVetoException, TransactionFailure {
                        if (notifierEnabled != null) {
                            notifierProxy.enabled(notifierEnabled);
                        }
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return notifierProxy;
                    }
                }, notifier);

            }
            
            if (dynamic) {
                NotifierConfiguration dynamicNotifier = notificationServiceConfiguration.getNotifierConfigurationByType(notifierService.getNotifierConfigType());
                ConfigView view = ConfigSupport.getImpl(dynamicNotifier);
                NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);
                NotifierConfigurationExecutionOptions build = factoryStore.get(annotation.type()).build(dynamicNotifier);

                if (server.isDas()) {
                    if (targetUtil.getConfig(target).isDas()) {
                        if (notifierEnabled) {
                            build.setEnabled(true);
                            service.getExecutionOptions().addNotifierConfigurationExecutionOption(build);
                            eventBus.register(notifierService);
                        } else {
                            service.getExecutionOptions().removeNotifierConfigurationExecutionOption(build);
                            eventBus.unregister(notifierService);
                        }                       
                    }
                } else {
                        if (notifierEnabled) {
                            build.setEnabled(true);
                            service.getExecutionOptions().addNotifierConfigurationExecutionOption(build);
                            eventBus.register(notifierService);
                        } else {
                            service.getExecutionOptions().removeNotifierConfigurationExecutionOption(build);
                            eventBus.unregister(notifierService);
                        }                       
                }
            }

            actionReport.appendMessage(strings.getLocalString("notification.configure.notifier.added.configured",
                    "Notifier with name {0} is registered and set enabled to {1}.", notifierName, notifierEnabled) + "\n");
        } catch (TransactionFailure | UnsupportedEncodingException ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
}
