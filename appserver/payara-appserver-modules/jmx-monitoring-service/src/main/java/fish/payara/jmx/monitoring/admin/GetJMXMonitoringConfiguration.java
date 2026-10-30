/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.jmx.monitoring.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;

import fish.payara.internal.notification.NotifierUtils;
import fish.payara.internal.notification.PayaraNotifier;
import fish.payara.jmx.monitoring.configuration.MonitoredAttribute;
import fish.payara.jmx.monitoring.configuration.MonitoringServiceConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import jakarta.inject.Inject;
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
 * Asadmin command to get the JMX monitoring service's current configuration and
 * pretty print it to the shell.
 *
 * @author savage
 */
@Service(name = "get-jmx-monitoring-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.jmx.monitoring.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-jmx-monitoring-configuration",
            description = "List Payara JMX Monitoring Service Configuration")
})
public class GetJMXMonitoringConfiguration implements AdminCommand {

    private final static String JMX_MONITORING_HEADERS[] = {"JMX Monitoring Enabled", "JMX Log Frequency", "JMX Log Frequency Unit"};
    private final static String ATTRIBUTE_HEADERS[] = {"Object Name", "Attribute", "Description"};
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
     * Method that is invoked when the asadmin command is performed. Pretty
     * prints the Monitoring Service Configuration values.
     *
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
        ActionReport jmxMonitoringReport = actionReport.addSubActionsReport();
        ActionReport notifiersReport = actionReport.addSubActionsReport();
        ActionReport attributeReport = actionReport.addSubActionsReport();
        
        ColumnFormatter jmxMonitoringColumnFormatter = new ColumnFormatter(JMX_MONITORING_HEADERS);
        ColumnFormatter attributeColumnFormatter = new ColumnFormatter(ATTRIBUTE_HEADERS);
        ColumnFormatter notifiersColumnFormatter = new ColumnFormatter(NOTIFIER_HEADERS);

        MonitoringServiceConfiguration monitoringConfig = config.getExtensionByType(MonitoringServiceConfiguration.class);
        List<ServiceHandle<PayaraNotifier>> allNotifierServiceHandles = habitat.getAllServiceHandles(PayaraNotifier.class);

        jmxMonitoringColumnFormatter.addRow(new Object[]{monitoringConfig.getEnabled(), monitoringConfig.getLogFrequency(),
            monitoringConfig.getLogFrequencyUnit()});

        Map<String, Object> map = new HashMap<>();       
        map.put("enabled", monitoringConfig.getEnabled());
        map.put("logfrequency", monitoringConfig.getLogFrequency());
        map.put("logfrequencyunit", monitoringConfig.getLogFrequencyUnit());

        Properties extraProps = new Properties();
        extraProps.put("jmxmonitoringConfiguration", map);
        
        List<Map<String, String>> monitoredAttributes = new ArrayList<>();

        for (MonitoredAttribute monitoredBean : monitoringConfig.getMonitoredAttributes()) {
            Object values[] = new Object[3];
            values[0] = monitoredBean.getObjectName();
            values[1] = monitoredBean.getAttributeName();
            values[2] = monitoredBean.getDescription();
            Map<String, String> monitoredAttribute = new HashMap<>();
            monitoredAttribute.put(monitoredBean.getObjectName(), monitoredBean.getAttributeName());
            monitoredAttributes.add(monitoredAttribute);
            attributeColumnFormatter.addRow(values);
        }

        //Cannot change key in line below - required for admingui propertyDescTable.inc
        extraProps.put("monitored-beans", monitoredAttributes);
        actionReport.setExtraProperties(extraProps);

        if (!monitoringConfig.getNotifierList().isEmpty()) {
            
            List<String> notifiers = monitoringConfig.getNotifierList();

            Properties notifierProps = new Properties();
            for (ServiceHandle<PayaraNotifier> serviceHandle : allNotifierServiceHandles) {
                final String notifierClassName = serviceHandle.getActiveDescriptor().getImplementationClass().getSimpleName();
                final String notifierName = NotifierUtils.getNotifierName(serviceHandle.getActiveDescriptor());

                Object values[] = new Object[2];
                values[0] = notifierName;
                values[1] = notifiers.contains(notifierName);
                notifiersColumnFormatter.addRow(values);

                Map<String, Object> mapNotifiers = new HashMap<>(2);
                mapNotifiers.put("notifierName", values[0]);
                mapNotifiers.put("notifierEnabled", values[1]);

                notifierProps.put("notifierList" + notifierClassName, mapNotifiers);
            }

            actionReport.getExtraProperties().putAll(notifierProps);
        }

        jmxMonitoringReport.setMessage(jmxMonitoringColumnFormatter.toString());
        jmxMonitoringReport.appendMessage(StringUtils.EOL);
        notifiersReport.setMessage(notifiersColumnFormatter.toString());
        notifiersReport.appendMessage(StringUtils.EOL);
        attributeReport.setMessage(attributeColumnFormatter.toString());
        attributeReport.appendMessage(StringUtils.EOL);

        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    /**
     * Converts a boolean value into a UTF-8 tick or cross character if the
     * pretty param has been passed.
     *
     * @param plain
     * @return
     */
    private String prettyBool(boolean plain) {
        if (pretty == false) {
            return String.valueOf(plain);
        } else if (plain) {
            return "✓";
        } else {
            return "✗";
        }
    }

}
