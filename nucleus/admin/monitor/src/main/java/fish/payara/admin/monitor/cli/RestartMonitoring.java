/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.config.serverbeans.MonitoringService;
import com.sun.enterprise.util.ColumnFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Asadmin command to restart Module Monitoring Level.
 * Sets monitoring module levels to OFF before reverting back to their previous values.
 * 
 * This was Feature request due to AMX metrics "breaking" sometimes, where restarting it manually was somewhat tedious and prone to error.
 * 
 * @author Alan Roth
 */
@Service(name = "restart-monitoring")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.DEPLOYMENT_GROUP, CommandTarget.CONFIG})
public class RestartMonitoring implements AdminCommand {

    @Param(name = "target", optional = true, defaultValue = "server-config")
    String target;

    @Param(name = "verbose", optional = true, shortName = "v", defaultValue = "false")
    private Boolean verbose;

    @Inject
    private Target targetUtil;

    @Inject
    private Logger logger;

    private MonitoringService monitoringService;
    
    private ActionReport actionReport;

    @Override
    public void execute(AdminCommandContext context) {
        actionReport = context.getActionReport();

        List<String> validModuleList = Arrays.asList(Constants.validModuleNames);
        Config config = targetUtil.getConfig(target);
        if (config != null) {
            monitoringService = config.getMonitoringService();
        } else {
            actionReport.setMessage("Cound not find target: " + target);
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        Map<String, String> enabledModules = getEnabledModules(validModuleList);
        Map<String, String> disabledModules = new HashMap<>();
        
        for(String key : enabledModules.keySet()){
            disabledModules.put(key, "OFF");
        }
        
        setModules(disabledModules);
        setModules(enabledModules);
        
        if(actionReport.getActionExitCode().equals(actionReport.getActionExitCode().WARNING)){
            actionReport.appendMessage("Restarted monitoring levels with warnings");
        }
        
        if (verbose) {
            actionReport.setMessage(getFormattedColumns(enabledModules).toString());
        } else {
            actionReport.setMessage("Restarted " +  enabledModules.size() + " modules");
        }
    }
   
    /*
    *The config is directly changed, using a supporting class (See ConfigSupport) which uses a transaction
    *to change the value of the monitoring modules config. The changes take affect immedietly after a succesful transaction.
    */
    private void setModules(Map<String, String> modules){
        for (Entry<String, String> entry : modules.entrySet()) {
            String module = entry.getKey();
            String moduleLevel = entry.getValue();
            try {
                ConfigSupport.apply((final MonitoringService monitoringServiceProxy) -> {
                    monitoringServiceProxy.setMonitoringLevel(module, moduleLevel);
                    return monitoringServiceProxy;
                }, monitoringService);
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING, "Failed to execute the command restart-monitoring, while changing the module level: {0}", ex.getCause().getMessage());
                actionReport.setMessage(ex.getCause().getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.WARNING);
            }
        }
    }
    
    private Map<String, String> getEnabledModules(List<String> validModules) {
        Map<String, String> enabledModules = new HashMap<>();
            for (String module : validModules) {
                String level = monitoringService.getMonitoringLevel(module);
                if (!"OFF".equals(level)) {
                    enabledModules.put(module, level);
                }
            }
        return enabledModules;
    }

    private ColumnFormatter getFormattedColumns(Map<String, String> enabledModules) {
        final String[] headers = {"Module", "Monitoring Level"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Map<String, Object> extraPropertiesMap = new HashMap<>();

        if (!enabledModules.isEmpty()) {
            for (String module : enabledModules.keySet()) {
                columnFormatter.addRow(new Object[]{module,
                    monitoringService.getMonitoringLevel(module)});
                extraPropertiesMap.put(module,
                        monitoringService.getMonitoringLevel(module));
            }
        }
        return columnFormatter;
    }
}
