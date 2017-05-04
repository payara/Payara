/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.nucleus.requesttracing.admin;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
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
    @RestEndpoint(configBean = RequestTracingServiceConfiguration.class,
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
        
        mainActionReport.appendMessage("Request Tracing Service enabled?: " + configuration.getEnabled() + "\n");
        
        if (Boolean.parseBoolean(configuration.getEnabled())) {
            mainActionReport.appendMessage("Historical Tracing Enabled?: " + configuration.getHistoricalTraceEnabled() 
                    + "\n");
            if (Boolean.parseBoolean(configuration.getHistoricalTraceEnabled())) {
                mainActionReport.appendMessage("Historical Tracing Store Size: " 
                        + configuration.getHistoricalTraceStoreSize() + "\n");
            }
        }
        
        // Create the extraProps for the general request tracing configuration
        Properties mainExtraProps = new Properties();
        Map<String, Object> mainExtraPropsMap = new HashMap<>();
        
        mainExtraPropsMap.put("enabled", configuration.getEnabled());
        mainExtraPropsMap.put("historicalTraceEnabled", configuration.getHistoricalTraceEnabled());
        mainExtraPropsMap.put("historicalTraceStoreSize", configuration.getHistoricalTraceStoreSize());
        mainExtraPropsMap.put("thresholdUnit", configuration.getThresholdUnit());
        mainExtraPropsMap.put("thresholdValue", configuration.getThresholdValue());
        
        mainExtraProps.put("requestTracingConfiguration", mainExtraPropsMap);
        mainActionReport.setExtraProperties(mainExtraProps);
        
        mainActionReport.appendMessage("Below are the configuration details of each notifier listed by its name.");
        mainActionReport.appendMessage(StringUtils.EOL);
        
        ActionReport notifiersActionReport = mainActionReport.addSubActionsReport();
                
        List<ServiceHandle<BaseNotifierService>> allServiceHandles = habitat.getAllServiceHandles(BaseNotifierService.class);
        
        if (configuration.getNotifierList().isEmpty()) {
            notifiersActionReport.setMessage("No notifier defined");
        }
        else {
            String headers[] = {"Notifier Name", "Notifier Enabled"};
            ColumnFormatter columnFormatter = new ColumnFormatter(headers);
            
            List<Class<Notifier>> notifierClassList = Lists.transform(configuration.getNotifierList(), new Function<Notifier, Class<Notifier>>() {
                @Override
                public Class<Notifier> apply(Notifier input) {
                    return resolveNotifierClass(input);
                }
            });

            Properties notifierExtraProps = new Properties();
            for (ServiceHandle<BaseNotifierService> serviceHandle : allServiceHandles) {
                Notifier notifier = configuration.getNotifierByType(serviceHandle.getService().getNotifierType());
                if (notifier != null) {
                    ConfigView view = ConfigSupport.getImpl(notifier);
                    NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);

                    if (notifierClassList.contains(view.<Notifier>getProxyType())) {
                        Object values[] = new Object[2];
                        values[0] = serviceHandle.getActiveDescriptor().getName();
                        values[1] = notifier.getEnabled();
                        columnFormatter.addRow(values);

                        Map<String, Object> notifierExtraPropsMap = new HashMap<>();
                        notifierExtraPropsMap.put("notifierName", values[0]);
                        notifierExtraPropsMap.put("notifierEnabled", values[1]);

                        notifierExtraProps.put("getRequesttracingConfiguration" + annotation.type(), 
                                notifierExtraPropsMap);
                        notifiersActionReport.setExtraProperties(notifierExtraProps);
                    }
                }
            }
            
            notifiersActionReport.setMessage(columnFormatter.toString());
        }

        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private Class<Notifier> resolveNotifierClass(Notifier input) {
        ConfigView view = ConfigSupport.getImpl(input);
        return view.getProxyType();
    }
}
