/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.nucleus.notification.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.log.LogNotifierConfiguration;
import fish.payara.nucleus.notification.service.BaseNotifierService;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

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

        String headers[] = {"Enabled", "Notifier Enabled", "Notifier Noisy"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);

        if (configuration.getNotifierConfigurationList().isEmpty()) {
            mainActionReport.setMessage("No notifier defined");
        }
        else {
            List<Class<NotifierConfiguration>> notifierConfigurationClassList = configuration.getNotifierConfigurationList().stream().map((input) -> {
                return resolveNotifierConfigurationClass(input);
            }).collect(Collectors.toList());

            Properties extraProps = new Properties();
            for (ServiceHandle<BaseNotifierService> serviceHandle : allServiceHandles) {
                NotifierConfiguration notifierConfiguration = configuration.getNotifierConfigurationByType(serviceHandle.getService().getNotifierConfigType());

                if (notifierConfiguration != null) {
                    ConfigView view = ConfigSupport.getImpl(notifierConfiguration);
                    NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);

                    if (notifierConfigurationClassList.contains(view.<NotifierConfiguration>getProxyType())) {

                        Object values[] = new Object[3];
                        values[0] = notificationServiceConfiguration.getEnabled();
                        values[1] = notifierConfiguration.getEnabled();
                        values[2] = notifierConfiguration.getNoisy();
                        columnFormatter.addRow(values);

                        Map<String, Object> map;
                        if (NotifierType.LOG.equals(annotation.type())) {
                            map = new HashMap<>(4);
                            map.put("enabled", values[0]);
                            map.put("notifierEnabled", values[1]);
                            map.put("noisy", values[2]);
                            LogNotifierConfiguration logNotifierConfiguration = (LogNotifierConfiguration) notifierConfiguration;
                            map.put("useSeparateLogFile", logNotifierConfiguration.getUseSeparateLogFile());
                        }
                        else {
                            map = new HashMap<>(3);
                            map.put("enabled", values[0]);
                            map.put("notifierEnabled", values[1]);
                            map.put("noisy", values[2]);
                        }

                        extraProps.put("getNotificationConfiguration" + annotation.type(), map);
                    }
                }
            }
            mainActionReport.setExtraProperties(extraProps);
            mainActionReport.setMessage(columnFormatter.toString());
        }

        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private Class<NotifierConfiguration> resolveNotifierConfigurationClass(NotifierConfiguration input) {
        ConfigView view = ConfigSupport.getImpl(input);
        return view.getProxyType();
    }
}
