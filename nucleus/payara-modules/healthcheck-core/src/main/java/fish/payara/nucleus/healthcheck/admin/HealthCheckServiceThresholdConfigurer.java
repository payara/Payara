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
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.healthcheck.HealthCheckConstants;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.ThresholdDiagnosticsChecker;
import fish.payara.nucleus.healthcheck.preliminary.BaseThresholdHealthCheck;
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
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin command to enable/disable specific health check service given with its name
 *
 * @author mertcaliskan
 */
@Service(name = "healthcheck-configure-service-threshold")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure.service.threshold")
@ExecuteOn(RuntimeType.DAS)
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "healthcheck-configure-service-threshold",
                description = "Configures Health Check Service Notification Threshold Specified With Name")
})
public class HealthCheckServiceThresholdConfigurer implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl
            (HealthCheckServiceThresholdConfigurer.class);

    @Inject
    ServiceLocator habitat;

    @Inject
    protected Target targetUtil;

    @Inject
    HealthCheckService healthCheckService;

    @Inject
    protected Logger logger;

    @Param(name = "serviceName", optional = false)
    private String serviceName;

    @Param(name = "thresholdCritical", optional = true)
    private String thresholdCritical;

    @Param(name = "thresholdWarning", optional = true)
    private String thresholdWarning;

    @Param(name = "thresholdGood", optional = true)
    private String thresholdGood;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;
    
    @Inject
    ServiceLocator serviceLocator;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        final AdminCommandContext theContext = context;
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        final BaseThresholdHealthCheck service = habitat.getService(BaseThresholdHealthCheck.class, serviceName);
        Config config = targetUtil.getConfig(target);

        if (service == null) {
            actionReport.appendMessage(strings.getLocalString("healthcheck.service.configure.status.error",
                    "Service with name {0} could not be found.", serviceName));
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        HealthCheckServiceConfiguration healthCheckServiceConfiguration = config.getExtensionByType
                (HealthCheckServiceConfiguration.class);
        final ThresholdDiagnosticsChecker checker = healthCheckServiceConfiguration
                .<ThresholdDiagnosticsChecker>getCheckerByType(service.getCheckerType());

        if (checker == null) {
            actionReport.appendMessage(strings.getLocalString("healthcheck.service.configure.threshold.checker.not.exists",
                    "Health Check Service Checker Configuration with name {0} could not be found.", serviceName));
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            evaluateThresholdProp(actionReport, checker, HealthCheckConstants.THRESHOLD_CRITICAL, thresholdCritical);
            evaluateThresholdProp(actionReport, checker, HealthCheckConstants.THRESHOLD_WARNING, thresholdWarning);
            evaluateThresholdProp(actionReport, checker, HealthCheckConstants.THRESHOLD_GOOD, thresholdGood);
        }
        catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (dynamic) {
            enableOnTarget(actionReport, theContext, service, thresholdCritical, thresholdWarning, thresholdGood);
        }
    }

    private void evaluateThresholdProp(final ActionReport actionReport, final ThresholdDiagnosticsChecker checker,
                                       final String name, final String value) throws TransactionFailure {

        Property thresholdProp = checker.getProperty(name);
        if (thresholdProp == null) {
            ConfigSupport.apply(new SingleConfigCode<ThresholdDiagnosticsChecker>() {
                @Override
                public Object run(final ThresholdDiagnosticsChecker checkerProxy) throws
                        PropertyVetoException, TransactionFailure {
                    Property propertyProxy = checkerProxy.createChild(Property.class);
                    applyThreshold(propertyProxy, name, value);
                    checkerProxy.getProperty().add(propertyProxy);
                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return checkerProxy;
                }
            }, checker);
        }
        else {
            ConfigSupport.apply(new SingleConfigCode<Property>() {
                @Override
                public Object run(final Property propertyProxy) throws PropertyVetoException, TransactionFailure {
                    applyThreshold(propertyProxy, name, value);
                    return propertyProxy;
                }
            }, thresholdProp);
        }
    }

    private void applyThreshold(Property propertyProxy, String name, String value) throws PropertyVetoException {
        if (value != null) {
            propertyProxy.setName(name);
            propertyProxy.setValue(value);
        }
    }

    private void enableOnTarget(ActionReport actionReport, AdminCommandContext context, BaseThresholdHealthCheck service, String
            thresholdCritical, String thresholdWarning, String thresholdGood) {
        
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation inv;

        if (target.equals("server-config")) {
            inv = runner.getCommandInvocation("__edit-healthcheck-configure-service-threshold-on-das", subReport, context.getSubject());
        } else {
            inv = runner.getCommandInvocation("__edit-healthcheck-configure-service-threshold-on-instance", subReport, context.getSubject());
        }

        ParameterMap params = new ParameterMap();
        params.add("target", target);
        params.add("serviceName", serviceName);
        params.add("thresholdCritical", thresholdCritical);
        params.add("thresholdWarning", thresholdWarning);
        params.add("thresholdGood", thresholdGood);
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }
}
