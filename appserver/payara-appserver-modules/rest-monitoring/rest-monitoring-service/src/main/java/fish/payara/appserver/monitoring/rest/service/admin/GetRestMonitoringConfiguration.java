/*
DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.

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

package fish.payara.appserver.monitoring.rest.service.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.appserver.monitoring.rest.service.configuration.RestMonitoringConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
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
 * ASAdmin command to get the rest monitoring services configuration
 *
 * @author michael ranaldo <michael.ranaldo@payara.fish>
 */
@Service(name = "get-rest-monitoring-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, 
    CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = RestMonitoringConfiguration.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-rest-monitoring-configuration",
            description = "Gets the Rest Monitoring Configuration")
})
public class GetRestMonitoringConfiguration implements AdminCommand {
    
    private final String OUTPUT_HEADERS[] = {"Enabled", "Rest Monitoring Application Name", "Context Root", "Security Enabled"};

    @Inject
    private Target targetUtil;

    @Param(name = "target", defaultValue = SystemPropertyConstants.DAS_SERVER_NAME, optional = true)
    private String target;

    @Override
    public void execute(AdminCommandContext context) {
        Config config = targetUtil.getConfig(target);
               
        if (config == null) {
            context.getActionReport().setMessage("No such config name: " + targetUtil);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        RestMonitoringConfiguration restMonitoringConfiguration = config
                .getExtensionByType(RestMonitoringConfiguration.class);
        
        ColumnFormatter columnFormatter = new ColumnFormatter(OUTPUT_HEADERS);
        Object[] outputValues = {
            restMonitoringConfiguration.getEnabled(),
            restMonitoringConfiguration.getApplicationName(),
            restMonitoringConfiguration.getContextRoot(),
            restMonitoringConfiguration.getSecurityEnabled()
        };        
        columnFormatter.addRow(outputValues);
        
        ActionReport actionReport = context.getActionReport();
        actionReport.appendMessage(columnFormatter.toString());
        
        Map<String, Object> extraPropertiesMap = new HashMap<>();
        extraPropertiesMap.put("enabled", restMonitoringConfiguration.getEnabled());
        extraPropertiesMap.put("applicationname", restMonitoringConfiguration.getApplicationName());
        extraPropertiesMap.put("contextroot", restMonitoringConfiguration.getContextRoot());
        extraPropertiesMap.put("securityenabled", restMonitoringConfiguration.getSecurityEnabled());
        
        Properties extraProperties = new Properties();
        extraProperties.put("restMonitoringConfiguration", extraPropertiesMap);
        actionReport.setExtraProperties(extraProperties);
    }

}
