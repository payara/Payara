/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.


 Copyright (c) 2016 Payara Foundation. All rights reserved.


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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.HoggingThreadsChecker;
import fish.payara.nucleus.healthcheck.preliminary.HoggingThreadsHealthCheck;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
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

/**
 *
 * @author steve
 */
@Service(name = "healthcheck-hoggingthreads-configure")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.hoggingthreads.configure")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "healthcheck-hoggingthreads-configure",
            description = "Configures the Hogging Threads Checker")
})
public class HoggingThreadsConfigurer implements AdminCommand {

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

    @Param(name = "time", optional = true)
    @Min(value = 1, message = "Time period must be 1 or more")
    private String time;

    @Param(name = "unit", optional = true, acceptableValues = "DAYS,HOURS,MICROSECONDS,MILLISECONDS,MINUTES,NANOSECONDS,SECONDS")
    private String unit;
    
    @Param(name = "name", optional = true)
    @Deprecated
    private String name;

    @Param(name = "checkerName", optional = true)
    private String checkerName;
    
    @Param(name = "threshold-percentage")
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String threshold;

    @Min(value = 1, message = "Retry count must be 1 or more")
    @Param(name = "retry-count")
    private String retryCount;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Inject
    ServerEnvironment server;

    @Override
    public void execute(AdminCommandContext context) {

        Config config = targetUtil.getConfig(target);
        HoggingThreadsHealthCheck service = habitat.getService(HoggingThreadsHealthCheck.class);
        final ActionReport actionReport = context.getActionReport();
        if (service == null) {
            actionReport.appendMessage(strings.getLocalString("healthcheck.hoggingthreads.configure.status.error",
                    "Hogging Threads Checker Service could not be found"));
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        // Warn about deprecated option
        if (name != null) {
            actionReport.appendMessage("\n--name parameter is decremented, please begin using the --checkerName option\n");
        }
        
        try {
            HealthCheckServiceConfiguration healthCheckServiceConfiguration = config.getExtensionByType(HealthCheckServiceConfiguration.class);
            HoggingThreadsChecker hoggingThreadConfiguration = healthCheckServiceConfiguration.getCheckerByType(HoggingThreadsChecker.class);
            if (hoggingThreadConfiguration == null) {
                ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                    @Override
                    public Object run(final HealthCheckServiceConfiguration healthCheckServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        HoggingThreadsChecker checkerProxy = healthCheckServiceConfigurationProxy.createChild(HoggingThreadsChecker.class);
                        applyValues(checkerProxy);
                        healthCheckServiceConfigurationProxy.getCheckerList().add(checkerProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return healthCheckServiceConfigurationProxy;
                    }
                }, healthCheckServiceConfiguration);
            } else {
                ConfigSupport.apply(new SingleConfigCode<HoggingThreadsChecker>() {
                    @Override
                    public Object run(final HoggingThreadsChecker hoggingThreadConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        applyValues(hoggingThreadConfigurationProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return hoggingThreadConfigurationProxy;
                    }
                }, hoggingThreadConfiguration);
            }
            
            if (dynamic) {
                if (server.isDas()) {
                    if (targetUtil.getConfig(target).isDas()) {
                        HoggingThreadsChecker checkerByType = healthCheckServiceConfiguration.getCheckerByType(HoggingThreadsChecker.class);
                        service.setOptions(service.constructOptions(checkerByType));
                        healthCheckService.registerCheck(checkerByType.getName(), service);
                        healthCheckService.reboot();
                    }
                } else {
                    // it implicitly targetted to us as we are not the DAS
                    // restart the service
                    HoggingThreadsChecker checkerByType = healthCheckServiceConfiguration.getCheckerByType(HoggingThreadsChecker.class);
                    service.setOptions(service.constructOptions(hoggingThreadConfiguration));
                    healthCheckService.registerCheck(checkerByType.getName(), service);
                    healthCheckService.reboot();
                }
            }

        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

    }
    
    private void applyValues(HoggingThreadsChecker checkerProxy) throws PropertyVetoException {
        if (enabled != null) {
            checkerProxy.setEnabled(enabled.toString());
        }
        
        if (name != null) {
            checkerProxy.setName(name);
        }
        
        // Take priority over deprecated parameter
        if (checkerName != null) {
            checkerProxy.setName(checkerName);
        }
        
        if (time != null) {
            checkerProxy.setTime(time);
        }
        
        if (unit != null) {
            checkerProxy.setUnit(unit);
        }
        
        if (threshold != null) {
            checkerProxy.setThresholdPercentage(threshold);
        }
        
        if (retryCount != null) {
            checkerProxy.setRetryCount(retryCount);
        }
    }

}
