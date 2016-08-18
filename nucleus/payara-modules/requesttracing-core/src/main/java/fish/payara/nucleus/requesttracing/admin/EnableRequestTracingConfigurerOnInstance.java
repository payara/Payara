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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.domain.execoptions.NotifierExecutionOptions;
import java.util.Map;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 
 *
 * @author Susan Rai
 */
@ExecuteOn({RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "__enable-requesttracing-configure-instance")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("__enable-requesttracing-configure-instance")
public class EnableRequestTracingConfigurerOnInstance implements AdminCommand {

   final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(EnableRequestTracingConfigurerOnInstance.class);

    @Inject
    RequestTracingService service;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "thresholdUnit", optional = true, defaultValue = "SECONDS")
    private String unit;

    @Param(name = "thresholdValue", optional = true, defaultValue = "30")
    private String value;

    @Override
    public void execute(AdminCommandContext context) {
        final AdminCommandContext theContext = context;
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        if (enabled != null) {
            service.getExecutionOptions().setEnabled(enabled);
            // also set all notifiers
            Map<NotifierType, NotifierExecutionOptions> notifierExecutionOptionsList = service.getExecutionOptions().getNotifierExecutionOptionsList();
            if (notifierExecutionOptionsList != null) {
                for (Map.Entry<NotifierType, NotifierExecutionOptions> entry : notifierExecutionOptionsList.entrySet()) {
                    NotifierExecutionOptions value1 = entry.getValue();
                    value1.setEnabled(enabled);
                }
            }
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.status.success",
                    "request tracing service status is set to {0}.", enabled) + "\n");
        }
        if (value != null) {
            service.getExecutionOptions().setThresholdValue(Long.valueOf(value));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.thresholdvalue.success",
                    "Request Tracing Service Threshold Value is set to {0}.", value) + "\n");
        }
        if (unit != null) {
            service.getExecutionOptions().setThresholdUnit(TimeUnit.valueOf(unit));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.thresholdunit.success",
                    "Request Tracing Service Threshold Unit is set to {0}.", unit) + "\n");
        }
    }
}

