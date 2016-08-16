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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.preliminary.BaseThresholdHealthCheck;
import java.util.Properties;
import javax.inject.Inject;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Susan Rai
 */
@Service(name = "__edit-healthcheck-configure-service-threshold-on-das")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("__edit-healthcheck-configure-service-threshold-on-das")
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
public class EditHealthCheckServiceThresholdConfigurerOnDas implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckServiceThresholdConfigurer.class);

    @Inject
    ServiceLocator habitat;
    
    @Inject
    HealthCheckService healthCheckService;

    @Param(name = "serviceName", optional = false)
    private String serviceName;

    @Param(name = "thresholdCritical", optional = true)
    private String thresholdCritical;

    @Param(name = "thresholdWarning", optional = true)
    private String thresholdWarning;

    @Param(name = "thresholdGood", optional = true)
    private String thresholdGood;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        BaseThresholdHealthCheck service = habitat.getService(BaseThresholdHealthCheck.class, serviceName);
        if (service == null) {
            actionReport.appendMessage("No service found with name " + serviceName);
            return;
        }
        
        if (service.getOptions() == null) {
            actionReport.appendMessage("Setting the service thresholds for " + serviceName + " will require a server restart");
            return;            
        }
        
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

        healthCheckService.shutdownHealthCheck();
        healthCheckService.bootstrapHealthCheck();
    }
}
