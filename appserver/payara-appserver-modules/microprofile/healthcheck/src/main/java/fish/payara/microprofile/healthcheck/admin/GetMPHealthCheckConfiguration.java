/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.microprofile.healthcheck.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.microprofile.healthcheck.config.MetricsHealthCheckConfiguration;
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
 *
 * @author Susan Rai
 */
@Service(name = "get-microprofile-healthcheck-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = MetricsHealthCheckConfiguration.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-microprofile-healthcheck-configuration",
            description = "Gets the Microprofile Health Check Configuration")
})
public class GetMPHealthCheckConfiguration implements AdminCommand {

    private final String OUTPUT_HEADERS[] = {"Enabled", "EndPoint"};

    @Inject
    private Target targetUtil;

    @Param(optional = true, defaultValue = "server-config")
    private String target;

    @Override
    public void execute(AdminCommandContext context) {
        Config targetConfig = targetUtil.getConfig(target);

        if (targetConfig == null) {
            context.getActionReport().setMessage("No such config name: " + targetUtil);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        MetricsHealthCheckConfiguration healthCheckConfiguration = targetConfig
                .getExtensionByType(MetricsHealthCheckConfiguration.class);

        ColumnFormatter columnFormatter = new ColumnFormatter(OUTPUT_HEADERS);
        Object[] outputValues = {
            healthCheckConfiguration.getEnabled(),
            healthCheckConfiguration.getEndpoint()
        };
        columnFormatter.addRow(outputValues);

        context.getActionReport().appendMessage(columnFormatter.toString());

        Map<String, Object> extraPropertiesMap = new HashMap<>();
        extraPropertiesMap.put("enabled", healthCheckConfiguration.getEnabled());
        extraPropertiesMap.put("endpoint", healthCheckConfiguration.getEndpoint());

        Properties extraProperties = new Properties();
        extraProperties.put("microprofileHealthCheckConfiguration", extraPropertiesMap);
        context.getActionReport().setExtraProperties(extraProperties);
    }

}
