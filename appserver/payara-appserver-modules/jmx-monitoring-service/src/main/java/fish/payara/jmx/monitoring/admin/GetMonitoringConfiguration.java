/*
DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://github.com/payara/Payara/blob/master/LICENSE.txt
    See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at glassfish/legal/LICENSE.txt.

    GPL Classpath Exception:
    The Payara Foundation designates this particular file as subject to the "Classpath"
    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
    file that accompanied this code.

    Modifications:
    If applicable, add the following below the License Header, with the fields
    enclosed by brackets [] replaced by your own identifying information:
    "Portions Copyright [year] [name of copyright owner]"

    Contributor(s):
    If you wish your version of this file to be governed by only the CDDL or
    only the GPL Version 2, indicate your decision by adding "[Contributor]
    elects to include this software in this distribution under the [CDDL or GPL
    Version 2] license."  If you don't indicate a single choice of license, a
    recipient has the option to distribute your version of this file under
    either the CDDL, the GPL Version 2 or to extend the choice of license to
    its licensees as provided above.  However, if you add GPL Version 2 code
    and therefore, elected the GPL Version 2 license, then the option applies
    only if the new code is made subject to such option by the copyright
    holder.
 */
package fish.payara.jmx.monitoring.admin;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.jmx.monitoring.configuration.MonitoringServiceConfiguration;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigView;
import org.jvnet.hk2.config.types.Property;

/**
 * Asadmin command to get the monitoring service's current configuration and pretty print it to the shell.
 *
 * @author savage
 */
@Service(name = "get-monitoring-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.monitoring.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-monitoring-configuration",
            description = "List Payara Monitoring Service Configuration")
})
public class GetMonitoringConfiguration implements AdminCommand {

    private final String ATTRIBUTE_HEADERS[] = {"|Name|", "|Value|", "|Description|"};
    private final static String NOTIFIER_HEADERS[] = {"Name", "Notifier Enabled"};

    @Inject
    private Target targetUtil;

    @Param(name = "pretty", defaultValue = "false", optional = true)
    private Boolean pretty;

    @Param(name = "target", defaultValue = SystemPropertyConstants.DAS_SERVER_NAME, optional = true)
    private String target;
    
    @Inject
    ServiceLocator habitat;

    /**
     * Method that is invoked when the asadmin command is performed.
     *  Pretty prints the Monitoring Service Configuration values.
     * @param context 
     */
    @Override
    public void execute(AdminCommandContext context) {
        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config name: " + targetUtil);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        ActionReport actionReport = context.getActionReport();
        ActionReport notifiersReport = actionReport.addSubActionsReport();
        ActionReport attributeReport = actionReport.addSubActionsReport();
        ColumnFormatter attributeColumnFormatter = new ColumnFormatter(ATTRIBUTE_HEADERS);
        ColumnFormatter notifiersColumnFormatter = new ColumnFormatter(NOTIFIER_HEADERS);

        MonitoringServiceConfiguration monitoringConfig = config.getExtensionByType(MonitoringServiceConfiguration.class);
        List<ServiceHandle<BaseNotifierService>> allNotifierServiceHandles = habitat.getAllServiceHandles(BaseNotifierService.class);

        actionReport.appendMessage("Monitoring Service Configuration is enabled? " + prettyBool(Boolean.valueOf(monitoringConfig.getEnabled())) + "\n");
        actionReport.appendMessage("Monitoring Service Configuration has AMX enabled? " + prettyBool(Boolean.valueOf(monitoringConfig.getAmx())) + "\n");
        actionReport.appendMessage("Monitoring Service Configuration log frequency? " + monitoringConfig.getLogFrequency() + " " + monitoringConfig.getLogFrequencyUnit());
        actionReport.appendMessage(StringUtils.EOL);

         Map<String, Object> map = new HashMap<String, Object>();
        Properties extraProps = new Properties();
        map.put("enabled", monitoringConfig.getEnabled());
        map.put("amx", monitoringConfig.getAmx());
        map.put("logfrequency", monitoringConfig.getLogFrequency());
        map.put("logfrequencyunit", monitoringConfig.getLogFrequencyUnit());
        
        extraProps.put("jmxmonitoringConfiguration", map);
        
        
        Map<String, Object> propMap = new HashMap<String, Object>();
        
        for (Property property : monitoringConfig.getProperty()) {
            Object values[] = new Object[3];
            values[0] = property.getName();
            values[1] = property.getValue();
            values[2] = property.getDescription();
            propMap.put(property.getName(), property.getValue());
            attributeColumnFormatter.addRow(values);
        }
        
        //Cannot change key in line below - required for admingui propertyDescTable.inc
        extraProps.put("properties", propMap);
        
        actionReport.setExtraProperties(extraProps);
        
        if (!monitoringConfig.getNotifierList().isEmpty()) {
            List<Class<Notifier>> notifierClassList = Lists.transform(monitoringConfig.getNotifierList(), new Function<Notifier, Class<Notifier>>() {
                @Override
                public Class<Notifier> apply(Notifier input) {
                    return resolveNotifierClass(input);
                }
            });

            Properties notifierProps = new Properties();
            for (ServiceHandle<BaseNotifierService> serviceHandle : allNotifierServiceHandles) {
                Notifier notifier = monitoringConfig.getNotifierByType(serviceHandle.getService().getNotifierType());
                if (notifier != null) {
                    ConfigView view = ConfigSupport.getImpl(notifier);
                    NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);

                    if (notifierClassList.contains(view.<Notifier>getProxyType())) {
                        Object values[] = new Object[2];
                        values[0] = annotation.type();
                        values[1] = notifier.getEnabled();
                        notifiersColumnFormatter.addRow(values);

                        Map<String, Object> mapNotifiers = new HashMap<>(2);
                        mapNotifiers.put("notifierName", values[0]);
                        mapNotifiers.put("notifierEnabled", values[1]);

                        notifierProps.put("notifierList" + annotation.type(), mapNotifiers);
                    }
                }
                
                actionReport.getExtraProperties().putAll(notifierProps);
            }
        }

        notifiersReport.setMessage(notifiersColumnFormatter.toString());
        notifiersReport.appendMessage(StringUtils.EOL);
        attributeReport.setMessage(attributeColumnFormatter.toString());
        attributeReport.appendMessage(StringUtils.EOL);

        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    /**
     * Converts a boolean value into a UTF-8 tick or cross character if the pretty param has been passed. 
     *
     * @param plain 
     * @return 
     */
    private String prettyBool(boolean plain) {
        if (pretty == false) {
            return String.valueOf(plain);
        }
        else if (plain) {
            return "✓";
        } else {
            return "✗";
        }
    }
    
    /**
     * 
     * @since 4.1.2.174
     * @param input
     * @return 
     */
    private Class<Notifier> resolveNotifierClass(Notifier input) {
        ConfigView view = ConfigSupport.getImpl(input);
        return view.getProxyType();
    }

}
