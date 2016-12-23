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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.log.LogNotifier;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import java.util.HashMap;
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

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Admin command to list Request Tracing Configuration
 *
 * @author mertcaliskan
 * @author Susan Rai
 */

@Service(name = "get-requesttracing-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.requesttracing.configuration")
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-requesttracing-configuration",
            description = "List Request Tracing Configuration")
})
public class GetRequestTracingConfiguration implements AdminCommand {
    @Inject
    ServiceLocator habitat;

    @Inject
    private Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Override
    public void execute(AdminCommandContext context) {

        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        ActionReport mainActionReport = context.getActionReport();


        RequestTracingServiceConfiguration configuration = config.getExtensionByType(RequestTracingServiceConfiguration.class);
        List<ServiceHandle<BaseNotifierService>> allNotifierHandles = habitat.getAllServiceHandles(BaseNotifierService.class);

        String headers[] = {"Enabled", "ThresholdUnit", "ThresholdValue", "Notifier Name", "Notifier Enabled"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = new Object[5];
        values[0] = configuration.getEnabled();
        values[1] = configuration.getThresholdUnit();
        values[2] = configuration.getThresholdValue();

        for (ServiceHandle<BaseNotifierService> notifierHandle : allNotifierHandles) {
            Notifier notifier = configuration.getNotifierByType(notifierHandle.getService().getNotifierType());

            if (notifier instanceof LogNotifier) {
                LogNotifier logNotifier = (LogNotifier) notifier;
                values[3] = notifierHandle.getActiveDescriptor().getName();
                values[4] = logNotifier.getEnabled();
            }
        }
        columnFormatter.addRow(values);

        Map<String, Object> map = new HashMap<String, Object>(5);
        Properties extraProps = new Properties();
        map.put("enabled", values[0]);
        map.put("thresholdUnit", values[1]);
        map.put("thresholdValue", values[2]);
        map.put("notifierName", values[3]);
        map.put("notifierEnabled", values[4]);
        
        extraProps.put("getRequesttracingConfiguration", map);
        mainActionReport.setExtraProperties(extraProps);
        mainActionReport.setMessage(columnFormatter.toString());
        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
