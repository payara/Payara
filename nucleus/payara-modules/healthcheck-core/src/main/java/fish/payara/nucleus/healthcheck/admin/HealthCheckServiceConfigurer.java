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
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
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
import org.jvnet.hk2.config.*;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin command to enable/disable specific health check service given with its
 * name
 *
 * @author mertcaliskan
 */
@Service(name = "healthcheck-configure-service")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure.service")
@ExecuteOn(RuntimeType.DAS)
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "healthcheck-configure-service",
            description = "Enables/Disables Health Check Service Specified With Name")
})
public class HealthCheckServiceConfigurer implements AdminCommand {

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
    private String time;

    @Param(name = "unit", optional = true)
    private String unit;

    @Param(name = "serviceName", optional = false)
    private String serviceName;

    @Param(name = "name", optional = true)
    private String name;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

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

        final BaseHealthCheck service = habitat.getService(BaseHealthCheck.class, serviceName);
        if (service == null) {
            actionReport.appendMessage(strings.getLocalString("healthcheck.service.configure.status.error",
                    "Service with name {0} could not be found.", serviceName));
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        HealthCheckServiceConfiguration healthCheckServiceConfiguration = config.getExtensionByType(HealthCheckServiceConfiguration.class);
        final Checker checker = healthCheckServiceConfiguration.getCheckerByType(service.getCheckerType());

        try {
            final Checker[] createdChecker = {null};
            if (checker == null) {
                ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                    @Override
                    public Object run(final HealthCheckServiceConfiguration healthCheckServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        Checker checkerProxy = (Checker) healthCheckServiceConfigurationProxy.createChild(service.getCheckerType());
                        applyValues(checkerProxy);
                        healthCheckServiceConfigurationProxy.getCheckerList().add(checkerProxy);
                        createdChecker[0] = checkerProxy;
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return healthCheckServiceConfigurationProxy;
                    }
                }, healthCheckServiceConfiguration);
            }
            else {
                createdChecker[0] = checker;
                ConfigSupport.apply(new SingleConfigCode<Checker>() {
                    @Override
                    public Object run(final Checker checkerProxy) throws
                            PropertyVetoException, TransactionFailure {
                        applyValues(checkerProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return checkerProxy;
                    }
                }, checker);
            }

            if (dynamic) {
                enableOnTarget(actionReport, theContext, service, createdChecker[0], enabled, time, unit);
            }
        }
        catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    private void applyValues(Checker checkerProxy) throws PropertyVetoException {
        if (enabled != null) {
            checkerProxy.setEnabled(enabled.toString());
        }
        if (time != null) {
            checkerProxy.setTime(time);
        }
        if (unit != null) {
            checkerProxy.setUnit(unit);
        }
        if (name != null) {
            checkerProxy.setName(name);
        }
    }

    private void enableOnTarget(ActionReport actionReport, AdminCommandContext context, BaseHealthCheck service, Checker checker, Boolean enabled, String time, String unit) {
        CommandRunner runner = habitat.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation inv;

        if (target.equals("server-config")) {
            inv = runner.getCommandInvocation("__enable-healthcheck-configure-service-on-das", subReport, context.getSubject());
        } else {
            inv = runner.getCommandInvocation("__enable-healthcheck-configure-service-on-instance", subReport, context.getSubject());
        }

        ParameterMap params = new ParameterMap();
        params.add("enabled", enabled.toString());
        params.add("target", target);
        params.add("time", time);
        params.add("unit", unit);
        params.add("serviceName", serviceName);
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }
}
