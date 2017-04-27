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

import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.Properties;
import java.util.logging.Logger;
import javax.inject.Inject;

import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * Admin command to set Request Tracing services configuration
 *
 * @author Susan Rai
 */
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "set-requesttracing-configuration")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("set.requesttracing.configuration")
@RestEndpoints({
    @RestEndpoint(configBean = RequestTracingServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-requesttracing-configuration",
            description = "Set Request Tracing Services Configuration")
})
public class SetRequestTracingConfiguration implements AdminCommand {

    @Inject
    protected Logger logger;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "thresholdUnit", optional = true)
    private String unit;

    @Param(name = "thresholdValue", optional = true)
    private String value;

    @Param(name = "notifierDynamic", optional = true, defaultValue = "false")
    private Boolean notifierDynamic;

    @Deprecated
    @Param(name = "notifierEnabled", optional = true)
    private Boolean notifierEnabled;

    @Param(name = "historicalTraceEnabled", optional = true)
    private Boolean historicalTraceEnabled;

    @Param(name = "historicalTraceStoreSize", optional = true)
    private Integer historicalTraceStoreSize;

    @Inject
    ServiceLocator serviceLocator;

    CommandRunner.CommandInvocation inv;

    @Override
    public void execute(AdminCommandContext context) {
        final AdminCommandContext theContext = context;
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        if (!validate(actionReport)) {
            return;
        }

        enableRequestTracingConfigureOnTarget(actionReport, theContext, enabled);
        enableRequestTracingNotifierConfigurerOnTarget(actionReport, theContext);
    }

    private void enableRequestTracingConfigureOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();

        inv = runner.getCommandInvocation("requesttracing-configure", subReport, context.getSubject());

        ParameterMap params = new ParameterMap();
        params.add("enabled", enabled.toString());
        params.add("target", target);
        params.add("dynamic", dynamic.toString());
        params.add("thresholdUnit", unit);
        params.add("thresholdValue", value);
        
        if (historicalTraceEnabled != null) {
            params.add("historicalTraceEnabled", historicalTraceEnabled.toString());
        }
        
        if (historicalTraceStoreSize != null) {
            params.add("historicalTraceStoreSize", historicalTraceStoreSize.toString());
        }
        
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }

    private void enableRequestTracingNotifierConfigurerOnTarget(ActionReport actionReport, AdminCommandContext context) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();

        inv = runner.getCommandInvocation("requesttracing-log-notifier-configure", subReport, context.getSubject());

        ParameterMap params = new ParameterMap();
        params.add("dynamic", notifierDynamic.toString());
        params.add("target", target);
        if (notifierEnabled != null) {
            params.add("enabled", notifierEnabled.toString());
        }
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }

    private boolean validate(ActionReport actionReport) {
        boolean result = false;
        if (value != null) {
            try {
                int thresholdValue = Integer.parseInt(value);
                if (thresholdValue <= 0 || thresholdValue > Short.MAX_VALUE * 2) {
                    actionReport.failure(logger, "Threshold Value must be greater than zero or less than " + Short.MAX_VALUE * 2 + 1);
                    return result;
                }
            } catch (NumberFormatException nfe) {
                actionReport.failure(logger, "Threshold Value is not a valid integer", nfe);
                return result;
            }

        }

        if (unit != null) {
            try {
                if (!unit.equals("NANOSECONDS")
                        && !unit.equals("MICROSECONDS")
                        && !unit.equals("MILLISECONDS")
                        && !unit.equals("SECONDS")
                        && !unit.equals("MINUTES")
                        && !unit.equals("HOURS")
                        && !unit.equals("DAYS")) {
                    actionReport.failure(logger, unit + " is an invalid time unit");
                    return result;
                }
            } catch (IllegalArgumentException iaf) {
                actionReport.failure(logger, unit + " is an invalid time unit", iaf);
                return result;
            }
        }

        return true;
    }
}
