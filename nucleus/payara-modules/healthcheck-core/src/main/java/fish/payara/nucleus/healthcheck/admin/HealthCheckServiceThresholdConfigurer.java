/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014 C2B2 Consulting Limited. All rights reserved.

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
import fish.payara.nucleus.healthcheck.configuration.Checker;
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
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
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

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        BaseThresholdHealthCheck service = habitat.getService(BaseThresholdHealthCheck.class, serviceName);
        Config config = targetUtil.getConfig(target);

        if (service == null) {
            actionReport.appendMessage(strings.getLocalString("healthcheck.service.configure.status.error",
                    "Service with name {0} could not be found.", serviceName));
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        HealthCheckServiceConfiguration healthCheckServiceConfiguration = config.getExtensionByType
                (HealthCheckServiceConfiguration.class);
        ThresholdDiagnosticsChecker checker = healthCheckServiceConfiguration.
                <ThresholdDiagnosticsChecker>getCheckerByType(service.getCheckerType());

        Property thresholdCriticalProp = checker.getProperty(HealthCheckConstants.THRESHOLD_CRITICAL);
        Property thresholdWarningProp = checker.getProperty(HealthCheckConstants.THRESHOLD_WARNING);
        Property thresholdGoodProp = checker.getProperty(HealthCheckConstants.THRESHOLD_GOOD);

        try {
            ConfigSupport.apply(new SingleConfigCode<Property>() {
                @Override
                public Object run(final Property propertyProxy) throws PropertyVetoException, TransactionFailure {
                    if (thresholdCritical != null) {
                        propertyProxy.setValue(thresholdCritical);
                    }
                    return null;
                }
            }, thresholdCriticalProp);

            ConfigSupport.apply(new SingleConfigCode<Property>() {
                @Override
                public Object run(final Property propertyProxy) throws PropertyVetoException, TransactionFailure {
                    if (thresholdWarning != null) {
                        propertyProxy.setValue(thresholdWarning);
                    }
                    return null;
                }
            }, thresholdWarningProp);

            ConfigSupport.apply(new SingleConfigCode<Property>() {
                @Override
                public Object run(final Property propertyProxy) throws PropertyVetoException, TransactionFailure {
                    if (thresholdGood != null) {
                        propertyProxy.setValue(thresholdGood);
                    }
                    return null;
                }
            }, thresholdGoodProp);


        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (dynamic) {
            enableOnTarget(actionReport, service, thresholdCritical, thresholdWarning, thresholdGood);
        }
    }

    private void enableOnTarget(ActionReport actionReport, BaseThresholdHealthCheck service, String
            thresholdCritical, String thresholdWarning, String thresholdGood) {

        if (thresholdCritical != null) {
            service.getOptions().setThresholdCritical(Integer.valueOf(thresholdCritical));
            actionReport.appendMessage(strings.getLocalString(
                    "healthcheck.service.configure.threshold.critical.success",
                    "Critical threshold for {0} service is set with value {1}.", serviceName, thresholdCritical));
        }
        if (thresholdWarning != null) {
            service.getOptions().setThresholdWarning(Integer.valueOf(thresholdWarning));
            actionReport.appendMessage(strings.getLocalString("healthcheck.service.configure.threshold.warning.success",
                    "Warning threshold for {0} service is set with value {1}.", serviceName, thresholdWarning));
        }
        if (thresholdGood != null) {
            service.getOptions().setThresholdGood(Integer.valueOf(thresholdGood));
            actionReport.appendMessage(strings.getLocalString("healthcheck.service.configure.threshold.good.success",
                    "Good threshold for {0} service is set with value {1}.", serviceName, thresholdGood));
        }

        healthCheckService.shutdownHealthCheck();
        healthCheckService.bootstrapHealthCheck();
    }
}
