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
package fish.payara.nucleus.requesttracing.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierType;
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
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 
 * @author Susan Rai
 */
@ExecuteOn({RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "__enable-requesttracing-configure-notifier-instance")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("__enable-requesttracing-configure-notifier-instance")
public class EnableRequestTracingNotifierConfigurerOnInstance implements AdminCommand {

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
        Map<NotifierType, NotifierExecutionOptions> notifierExecutionOptionsList = service.getExecutionOptions().getNotifierExecutionOptionsList();
        for (Map.Entry<NotifierType, NotifierExecutionOptions> entry : notifierExecutionOptionsList.entrySet()) {
            NotifierExecutionOptions value = entry.getValue();
            value.setEnabled(notifierEnabled);
        }

    }
}
