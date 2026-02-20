/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.admin.monitor.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * Asadmin command to list Module Monitoring level
 *
 * @author Susan Rai
 * @since 4.1.2.183
 */
@Service(name = "get-monitoring-level")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.monitoring.level")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-monitoring-level",
            description = "List Monitoring Service Level")
})
public class GetMonitoringLevel implements AdminCommand {

    @Inject
    private Target targetUtil;

    @Param(name = "target", defaultValue = SystemPropertyConstants.DAS_SERVER_NAME, optional = true)
    private String target;

    @Param(name = "module", optional = true)
    private String moduleName;

    @Override
    public void execute(AdminCommandContext context) {
        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        MonitoringService monitoringService = config.getMonitoringService();
        final ActionReport actionReport = context.getActionReport();
        final String[] headers= {"Module", "Monitoring Level"};

        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Map<String, Object> extraPropertiesMap = new HashMap<>();

        List<String> validModuleList = new ArrayList<>(Arrays.asList(Constants.validModuleNames));

        // If no modulename is passed by the command, display all the module and
        // their monitoring levels
        if (moduleName == null) {
            for (String validModule : validModuleList) {
                columnFormatter.addRow(new Object[]{validModule,
                    monitoringService.getMonitoringLevel(validModule)});
                extraPropertiesMap.put(validModule,
                        monitoringService.getMonitoringLevel(validModule));
            }
        }

        if (moduleName != null) {
            List<String> moduleNameList = Arrays.asList(moduleName.split(","));
            for (String module : moduleNameList) {
                boolean isValidMoudle = false;
                String selectedModule = module.trim().toLowerCase();
                for (String validModule : validModuleList) {
                    if (validModule.trim().equals(selectedModule)) {
                        columnFormatter.addRow(new Object[]{selectedModule,
                            monitoringService.getMonitoringLevel(selectedModule)});
                        extraPropertiesMap.put(selectedModule,
                                monitoringService.getMonitoringLevel(selectedModule));
                        isValidMoudle = true;
                        break;
                    }
                }

                if (!isValidMoudle) {
                    actionReport.setMessage(selectedModule + " isn't a valid Module name");
                    actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }

        Properties extraProperties = new Properties();
        extraProperties.put("getMonitoringLevel", extraPropertiesMap);

        actionReport.setExtraProperties(extraProperties);
        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
