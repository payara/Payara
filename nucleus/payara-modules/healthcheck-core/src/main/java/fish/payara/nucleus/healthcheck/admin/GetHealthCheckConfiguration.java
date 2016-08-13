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
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.StringUtils;
import fish.payara.nucleus.healthcheck.HealthCheckConstants;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.HoggingThreadsChecker;
import fish.payara.nucleus.healthcheck.configuration.ThresholdDiagnosticsChecker;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import java.util.List;

/**
 * @author mertcaliskan
 */
@Service(name = "get-healthcheck-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.healthcheck.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "get-healthcheck-configuration",
                description = "List HealthCheck Configuration")
})
public class GetHealthCheckConfiguration implements AdminCommand, HealthCheckConstants {

    final static String baseHeaders[] = {"Name", "Enabled", "Time", "Unit"};
    final static String hoggingThreadsHeaders[] = {"Name", "Enabled", "Time", "Unit", "Threshold Percentage",
            "Retry Count"};
    final static String thresholdDiagnosticsHeaders[] = {"Name", "Enabled", "Time", "Unit", "Critical Threshold",
            "Warning Threshold", "Good Threshold"};

    @Inject
    ServiceLocator habitat;

    @Inject
    private Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = "server")
    private String target;

    @Override
    public void execute(AdminCommandContext context) {

        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        ActionReport mainActionReport = context.getActionReport();
        ActionReport baseActionReport = mainActionReport.addSubActionsReport();
        ActionReport hoggingThreadsActionReport = mainActionReport.addSubActionsReport();
        ActionReport thresholdDiagnosticsActionReport = mainActionReport.addSubActionsReport();

        ColumnFormatter baseColumnFormatter = new ColumnFormatter(baseHeaders);
        ColumnFormatter hoggingThreadsColumnFormatter = new ColumnFormatter(hoggingThreadsHeaders);
        ColumnFormatter thresholdDiagnosticsColumnFormatter = new ColumnFormatter(thresholdDiagnosticsHeaders);

        HealthCheckServiceConfiguration configuration = config.getExtensionByType(HealthCheckServiceConfiguration
                .class);
        List<ServiceHandle<BaseHealthCheck>> allServiceHandles = habitat.getAllServiceHandles(BaseHealthCheck.class);

        mainActionReport.appendMessage("Health Check Service Configuration is enabled?: " + configuration.getEnabled() + "\n");
        mainActionReport.appendMessage("Below are the list of configuration details of each checker listed by its name.");
        mainActionReport.appendMessage(StringUtils.EOL);

        for (ServiceHandle<BaseHealthCheck> serviceHandle : allServiceHandles) {
            Checker checker = configuration.getCheckerByType(serviceHandle.getService().getCheckerType());

            if (checker instanceof HoggingThreadsChecker) {
                HoggingThreadsChecker hoggingThreadsChecker = (HoggingThreadsChecker) checker;

                Object values[] = new Object[6];
                values[0] = hoggingThreadsChecker.getName();
                values[1] = hoggingThreadsChecker.getEnabled();
                values[2] = hoggingThreadsChecker.getTime();
                values[3] = hoggingThreadsChecker.getUnit();
                values[4] = hoggingThreadsChecker.getThresholdPercentage();
                values[5] = hoggingThreadsChecker.getRetryCount();
                hoggingThreadsColumnFormatter.addRow(values);
            }
            else if (checker instanceof ThresholdDiagnosticsChecker) {
                ThresholdDiagnosticsChecker thresholdDiagnosticsChecker = (ThresholdDiagnosticsChecker) checker;

                Object values[] = new Object[7];
                values[0] = thresholdDiagnosticsChecker.getName();
                values[1] = thresholdDiagnosticsChecker.getEnabled();
                values[2] = thresholdDiagnosticsChecker.getTime();
                values[3] = thresholdDiagnosticsChecker.getUnit();
                Property thresholdCriticalProperty = thresholdDiagnosticsChecker.getProperty(THRESHOLD_CRITICAL);
                values[4] = thresholdCriticalProperty != null ? thresholdCriticalProperty.getValue() : "-";
                Property thresholdWarningProperty = thresholdDiagnosticsChecker.getProperty(THRESHOLD_WARNING);
                values[5] = thresholdWarningProperty != null ? thresholdWarningProperty.getValue() : "-";
                Property thresholdGoodProperty = thresholdDiagnosticsChecker.getProperty(THRESHOLD_GOOD);
                values[6] = thresholdGoodProperty != null ? thresholdGoodProperty.getValue() : "-";
                thresholdDiagnosticsColumnFormatter.addRow(values);
            }
            else if (checker != null) {
                Object values[] = new Object[4];
                values[0] = checker.getName();
                values[1] = checker.getEnabled();
                values[2] = checker.getTime();
                values[3] = checker.getUnit();
                baseColumnFormatter.addRow(values);
            }
        }

        if (!baseColumnFormatter.getContent().isEmpty()) {
            baseActionReport.setMessage(baseColumnFormatter.toString());
            baseActionReport.appendMessage(StringUtils.EOL);
        }
        if (!hoggingThreadsColumnFormatter.getContent().isEmpty()) {
            hoggingThreadsActionReport.setMessage(hoggingThreadsColumnFormatter.toString());
            hoggingThreadsActionReport.appendMessage(StringUtils.EOL);
        }
        if (!thresholdDiagnosticsColumnFormatter.getContent().isEmpty()) {
            thresholdDiagnosticsActionReport.setMessage(thresholdDiagnosticsColumnFormatter.toString());
            thresholdDiagnosticsActionReport.appendMessage(StringUtils.EOL);
        }

        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
