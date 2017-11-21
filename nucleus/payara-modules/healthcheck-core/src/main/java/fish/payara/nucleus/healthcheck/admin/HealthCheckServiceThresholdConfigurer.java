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
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
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
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Admin command to enable/disable specific health check service given with its name
 *
 * @author mertcaliskan
 */
@Service(name = "healthcheck-configure-service-threshold")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure.service.threshold")
@ExecuteOn({RuntimeType.DAS,RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
                opType = RestEndpoint.OpType.POST,
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

    @Param(name = "serviceName", optional = false, 
            acceptableValues = "healthcheck-cpu,healthcheck-gc,healthcheck-cpool,healthcheck-heap,healthcheck-threads,"
                    + "healthcheck-machinemem")
    private String serviceName;

    @Param(name = "thresholdCritical", optional = true, defaultValue = HealthCheckConstants.THRESHOLD_DEFAULTVAL_CRITICAL)
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String thresholdCritical;

    @Param(name = "thresholdWarning", optional = true, defaultValue = HealthCheckConstants.THRESHOLD_DEFAULTVAL_WARNING)
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String thresholdWarning;

    @Param(name = "thresholdGood", optional = true, defaultValue = HealthCheckConstants.THRESHOLD_DEFAULTVAL_GOOD)
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String thresholdGood;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;
    
    @Inject
    ServiceLocator serviceLocator;
    
    @Inject
    ServerEnvironment server;

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
                if (service.getOptions() == null) {
                    service.setOptions(service.constructOptions(checker));
                }
                if (server.isDas()) {
                    if (targetUtil.getConfig(target).isDas()) {
                        configureDynamically(actionReport, service);
                        healthCheckService.reboot();
                    }
                } else {
                    // it implicitly targeted to us as we are not the DAS
                    // restart the service
                    configureDynamically(actionReport, service);
                    healthCheckService.reboot();
                }
        }
    }

    private void configureDynamically(ActionReport actionReport, BaseThresholdHealthCheck service) {
        if (thresholdCritical != null) {
            service.getOptions().setThresholdCritical(Integer.valueOf(thresholdCritical));
            actionReport.appendMessage(strings.getLocalString(
                    "healthcheck.service.configure.threshold.critical.success",
                    "Critical threshold for {0} service is set with value {1}.", serviceName, thresholdCritical));
            actionReport.appendMessage("\n");
        }
        if (thresholdWarning != null) {
            service.getOptions().setThresholdWarning(Integer.valueOf(thresholdWarning));
            actionReport.appendMessage(strings.getLocalString("healthcheck.service.configure.threshold.warning.success",
                    "Warning threshold for {0} service is set with value {1}.", serviceName, thresholdWarning));
            actionReport.appendMessage("\n");
        }
        if (thresholdGood != null) {
            service.getOptions().setThresholdGood(Integer.valueOf(thresholdGood));
            actionReport.appendMessage(strings.getLocalString("healthcheck.service.configure.threshold.good.success",
                    "Good threshold for {0} service is set with value {1}.", serviceName, thresholdGood));
            actionReport.appendMessage("\n");
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
}
