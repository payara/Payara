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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.preliminary.BaseThresholdHealthCheck;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

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

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckServiceThresholdConfigurer.class);

    @Inject
    HealthCheckService service;

    @Inject
    ServiceLocator habitat;

    @Param(name = "serviceName", optional = false)
    private String serviceName;

    @Param(name = "thresholdCritical", optional = true)
    private Integer thresholdCritical;

    @Param(name = "thresholdWarning", optional = true)
    private Integer thresholdWarning;

    @Param(name = "thresholdGood", optional = true)
    private Integer thresholdGood;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        BaseThresholdHealthCheck service = habitat.getService(BaseThresholdHealthCheck.class, serviceName);
        if (service == null) {
            report.appendMessage(strings.getLocalString("healthcheck.service.configure.status.error",
                    "Service with name {0} could not be found.", serviceName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        else {
            if (thresholdCritical != null) {
                service.getOptions().setThresholdCritical(thresholdCritical);
                report.appendMessage(strings.getLocalString(
                        "healthcheck.service.configure.threshold.critical.success",
                        "Critical threshold for {0} service is set with value {1}.", serviceName, thresholdCritical));
            }
            if (thresholdWarning != null) {
                service.getOptions().setThresholdWarning(thresholdWarning);
                report.appendMessage(strings.getLocalString("healthcheck.service.configure.threshold.warning.success",
                        "Warning threshold for {0} service is set with value {1}.", serviceName, thresholdWarning));
            }
            if (thresholdGood != null) {
                service.getOptions().setThresholdGood(thresholdGood);
                report.appendMessage(strings.getLocalString("healthcheck.service.configure.threshold.good.success",
                        "Good threshold for {0} service is set with value {1}.", serviceName, thresholdGood));
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
    }
}
