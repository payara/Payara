/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.


 Copyright (c) 2017 Payara Foundation. All rights reserved.


 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html.
 See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * Helper command that combines the HealthCheckServiceConfigurer and HealthCheckServiceThresholdsConfigurer commands.
 * @author Andrew Pielage
 */
@Service(name = "healthcheck-service-configure-checker-with-thresholds")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.service.configure.checker.with.thresholds")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE,
        CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "healthcheck-service-configure-checker-with-thresholds",
            description = "Configures a Heakthcheck Service Checker with Thresholds")
})
public class HealthCheckServiceConfigureCheckerWithThresholdsCommand implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckServiceConfigurer.class);

    @Inject
    ServiceLocator habitat;

    @Inject
    protected Target targetUtil;

    @Inject
    protected Logger logger;

    @Inject
    HealthCheckService healthCheckService;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Param(name = "time", optional = true)
    @Min(value = 1, message = "Time period must be 1 or more")
    private String time;

    @Param(name = "unit", optional = true,
            acceptableValues = "DAYS,HOURS,MICROSECONDS,MILLISECONDS,MINUTES,NANOSECONDS,SECONDS")
    private String unit;
    
    @Param(name = "checkerName", optional = true)
    private String checkerName;
    
    @Param(name = "serviceName", optional = false, 
            acceptableValues = "healthcheck-cpu,healthcheck-gc,healthcheck-cpool,healthcheck-heap,healthcheck-threads,"
                    + "healthcheck-machinemem")
    private String serviceName;

    @Param(name = "thresholdCritical", optional = true)
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String thresholdCritical;

    @Param(name = "thresholdWarning", optional = true)
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String thresholdWarning;

    @Param(name = "thresholdGood", optional = true)
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String thresholdGood;
    
    @Inject
    ServerEnvironment server;
    
    @Inject
    CommandRunner commandRunner;
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport mainActionReport = context.getActionReport();
        final ActionReport checkerConfigureReport = mainActionReport.addSubActionsReport();
        
        // Configure the checker with the provided parameters
        CommandRunner.CommandInvocation checkerConfigureInvocation = commandRunner.getCommandInvocation(
                "healthcheck-configure-service", checkerConfigureReport, context.getSubject());
        
        ParameterMap checkerConfigureParameters = new ParameterMap();
        checkerConfigureParameters.add("enabled", enabled.toString());
        checkerConfigureParameters.add("dynamic", dynamic.toString());
        checkerConfigureParameters.add("time", time);
        checkerConfigureParameters.add("unit", unit);
        checkerConfigureParameters.add("checkerName", checkerName);
        checkerConfigureParameters.add("serviceName", serviceName);
        checkerConfigureParameters.add("target", target);
        
        checkerConfigureInvocation.parameters(checkerConfigureParameters);
        checkerConfigureInvocation.execute();
        
        // Only carry on if the first command succeeds
        if (checkerConfigureReport.hasFailures()) {
            mainActionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            checkerConfigureReport.setMessage("Failed to configure checker");
        } else {
            final ActionReport thresholdConfigureReport = mainActionReport.addSubActionsReport();
            
            // Configure the checker thresholds
            CommandRunner.CommandInvocation thresholdConfigureInvocation = commandRunner.getCommandInvocation(
                "healthcheck-configure-service-threshold", thresholdConfigureReport, context.getSubject());
        
            ParameterMap thresholdConfigureParameters = new ParameterMap();
            thresholdConfigureParameters.add("dynamic", dynamic.toString());
            thresholdConfigureParameters.add("serviceName", serviceName);
            thresholdConfigureParameters.add("target", target);
            thresholdConfigureParameters.add("thresholdCritical", thresholdCritical);
            thresholdConfigureParameters.add("thresholdWarning", thresholdWarning);
            thresholdConfigureParameters.add("thresholdGood", thresholdGood);
            
            thresholdConfigureInvocation.parameters(thresholdConfigureParameters);
            thresholdConfigureInvocation.execute();
            
            // Check the command result to determine if the whole command succeeds or fails
            if (thresholdConfigureReport.hasFailures()) {
                mainActionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                checkerConfigureReport.setMessage("Failed to configure thresholds");
            }
        }
    }
}
