/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.

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
package fish.payara.nucleus.requesttracing.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import fish.payara.nucleus.requesttracing.domain.execoptions.NotifierExecutionOptions;
import fish.payara.nucleus.requesttracing.domain.execoptions.NotifierExecutionOptionsFactory;
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
 * Admin command to enable/disable specific notifier given with its name
 *
 * @author mertcaliskan
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "requesttracing-configure-notifier")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("requesttracing.configure.notifier")
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "requesttracing-configure-notifier",
            description = "Enables/Disables Notifier Specified With Name")
})
public class RequestTracingNotifierConfigurer implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(RequestTracingNotifierConfigurer.class);

    @Inject
    RequestTracingService service;

    @Inject
    NotifierExecutionOptionsFactory factory;

    @Inject
    ServiceLocator habitat;

    @Inject
    protected Logger logger;

    @Inject
    protected Target targetUtil;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "notifierName", optional = true)
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
        if (service == null) {
            actionReport.appendMessage(strings.getLocalString("requesttracing.notifier.configure.status.error",
                    "Notifier with name {0} could not be found.", notifierName));
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        final RequestTracingServiceConfiguration requestTracingServiceConfiguration = config.getExtensionByType(RequestTracingServiceConfiguration.class);
        final Notifier notifier = requestTracingServiceConfiguration.getNotifierByType(notifierService.getNotifierType());

        try {
            if (notifier == null) {
                final Notifier[] createdNotifier = {null};
                ConfigSupport.apply(new SingleConfigCode<RequestTracingServiceConfiguration>() {
                    @Override
                    public Object run(final RequestTracingServiceConfiguration requestTracingServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        Notifier notifierProxy = (Notifier) requestTracingServiceConfigurationProxy.createChild(notifierService.getNotifierType());
                        if (notifierEnabled != null) {
                            notifierProxy.enabled(notifierEnabled);
                        }
                        createdNotifier[0] = notifierProxy;

                        List<Notifier> notifierList = requestTracingServiceConfigurationProxy.getNotifierList();
                        NotifierExecutionOptions executionOptions = factory.build(createdNotifier[0]);
                        if (notifierEnabled) {
                            notifierList.add(createdNotifier[0]);
                            if (dynamic) {
                                service.getExecutionOptions().getNotifierExecutionOptionsList().add(executionOptions);
                            }
                        } else {
                            notifierList.remove(createdNotifier[0]);
                            if (dynamic) {
                                service.getExecutionOptions().getNotifierExecutionOptionsList().remove(executionOptions);
                            }
                        }

                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return requestTracingServiceConfigurationProxy;
                    }
                }, requestTracingServiceConfiguration);
            } else {
                ConfigSupport.apply(new SingleConfigCode<Notifier>() {
                    @Override
                    public Object run(final Notifier notifierProxy) throws
                            PropertyVetoException, TransactionFailure {
                        if (notifierEnabled != null) {
                            notifierProxy.enabled(notifierEnabled);
                        }

                        if (dynamic) {
                            NotifierExecutionOptions executionOptions = factory.build(notifierProxy);
                            if (notifierEnabled) {
                                service.getExecutionOptions().getNotifierExecutionOptionsList().add(executionOptions);
                            } else {
                                service.getExecutionOptions().getNotifierExecutionOptionsList().remove(executionOptions);
                            }
                        }

                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return notifierProxy;
                    }
                }, notifier);

            }

            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.notifier.added.configured",
                    "Request Tracing Notifier with name {0} is registered and set enabled to {1}.", notifierName, notifierEnabled) + "\n");
        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
}
