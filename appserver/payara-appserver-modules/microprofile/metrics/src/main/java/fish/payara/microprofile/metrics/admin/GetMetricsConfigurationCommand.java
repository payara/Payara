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
package fish.payara.microprofile.metrics.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import static org.glassfish.api.admin.RestEndpoint.OpType.GET;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.DEPLOYMENT_GROUP;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * AsAdmin command to get metrics configuration
 * 
 * @author Gaurav Gupta
 */
@Service(name = "get-metrics-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@TargetType({DAS, DEPLOYMENT_GROUP, STANDALONE_INSTANCE, CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = MetricsServiceConfiguration.class,
            opType = GET,
            path = "get-metrics-configuration",
            description = "Gets the Metrics Configuration")
})
public class GetMetricsConfigurationCommand implements AdminCommand {

    private final String[] OUTPUT_HEADERS = {"Enabled", "Dynamic", "EndPoint", "VirtualServers", "Security Enabled", "Roles"};
    
    @Inject
    private Target targetUtil;
    
    @Param(optional = true, defaultValue = "server-config")
    private String target;
    
    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        Config targetConfig = targetUtil.getConfig(target);
        
        if (targetConfig == null) {
            adminCommandContext.getActionReport().setMessage("No such config name: " + targetUtil);
            adminCommandContext.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        MetricsServiceConfiguration metricsConfiguration = targetConfig
                .getExtensionByType(MetricsServiceConfiguration.class);
        
        ColumnFormatter columnFormatter = new ColumnFormatter(OUTPUT_HEADERS);
        Object[] outputValues = {
            metricsConfiguration.getEnabled(),
            metricsConfiguration.getDynamic(),
            metricsConfiguration.getEndpoint(),
            metricsConfiguration.getVirtualServers(),
            metricsConfiguration.getSecurityEnabled(),
            metricsConfiguration.getRoles()
        };        
        columnFormatter.addRow(outputValues);
        
        adminCommandContext.getActionReport().appendMessage(columnFormatter.toString());
        
        Map<String, Object> extraPropertiesMap = new HashMap<>();
        extraPropertiesMap.put("enabled", metricsConfiguration.getEnabled());
        extraPropertiesMap.put("dynamic", metricsConfiguration.getDynamic());
        extraPropertiesMap.put("endpoint", metricsConfiguration.getEndpoint());
        extraPropertiesMap.put("virtualServers", metricsConfiguration.getVirtualServers());
        extraPropertiesMap.put("securityenabled", metricsConfiguration.getSecurityEnabled());
        extraPropertiesMap.put("roles", metricsConfiguration.getRoles());

        Properties extraProperties = new Properties();
        extraProperties.put("metricsConfiguration", extraPropertiesMap);
        adminCommandContext.getActionReport().setExtraProperties(extraProperties);
    }
}
