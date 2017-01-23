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
package fish.payara.nucleus.notification.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.log.LogNotifierConfiguration;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierConfiguration;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * Admin command to list Notification Configuration
 *
 * @author Susan Rai
 */
@Service(name = "get-notification-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.notification.configuration")
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = NotificationServiceConfiguration.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-notification-configuration",
            description = "List Notification Configuration")
})
public class GetNotificationConfiguration implements AdminCommand {

    @Inject
    private Target targetUtil;

    @Inject
    ServiceLocator habitat;

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
        final NotificationServiceConfiguration notificationServiceConfiguration = config.getExtensionByType(NotificationServiceConfiguration.class);
        NotificationServiceConfiguration configuration = config.getExtensionByType(NotificationServiceConfiguration.class);
        List<ServiceHandle<BaseNotifierService>> allServiceHandles = habitat.getAllServiceHandles(BaseNotifierService.class);

        String headers[] = {"Enabled", "Notifier Enabled", "Notifier Name"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = new Object[3];
        values[0] = notificationServiceConfiguration.getEnabled();

        for (ServiceHandle<BaseNotifierService> serviceHandle : allServiceHandles) {

            NotifierConfiguration notifierConfiguration = configuration.getNotifierConfigurationByType(serviceHandle.getService().getNotifierConfigType());
            LogNotifierConfiguration logNotifierConfiguration = (LogNotifierConfiguration) notifierConfiguration;
            if (logNotifierConfiguration == null) {
                values[1] = "true";
            } else {
                values[1] = logNotifierConfiguration.getEnabled();
            }
            values[2] = serviceHandle.getActiveDescriptor().getName();
        }
        columnFormatter.addRow(values);

        Map<String, Object> map = new HashMap<String, Object>(3);
        Properties extraProps = new Properties();
        map.put("enabled", values[0]);
        map.put("notifierEnabled", values[1]);
        map.put("notifierName", values[2]);

        extraProps.put("getNotificationConfiguration", map);
        mainActionReport.setExtraProperties(extraProps);
        mainActionReport.setMessage(columnFormatter.toString());

        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
