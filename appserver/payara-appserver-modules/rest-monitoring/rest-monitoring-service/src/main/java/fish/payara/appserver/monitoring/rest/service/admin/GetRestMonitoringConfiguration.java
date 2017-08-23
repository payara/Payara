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
@RestEndpoints({
    @RestEndpoint(configBean = RestMonitoringConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "get-rest-monitoring-configuration",
            description = "Gets the Rest Monitoring Configuration")
})
public class GetRestMonitoringConfiguration implements AdminCommand {
    
    private final String OUTPUT_HEADERS[] = {"Enabled", "Name", "Context Root", "Security Enabled"};

    @Inject
    private Target targetUtil;

    @Param(name = "target", defaultValue = SystemPropertyConstants.DAS_SERVER_NAME, optional = true)
    private String target;

    @Override
    public void execute(AdminCommandContext context) {
        Config config = targetUtil.getConfig(target);
        RestMonitoringConfiguration restMonitoringConfiguration = config
                .getExtensionByType(RestMonitoringConfiguration.class);
        
        if (config == null) {
            context.getActionReport().setMessage("No such config name: " + targetUtil);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
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
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("enabled", restMonitoringConfiguration.getEnabled());
        map.put("name", restMonitoringConfiguration.getApplicationName());
        map.put("contextroot", restMonitoringConfiguration.getContextRoot());
        map.put("securityenabled", restMonitoringConfiguration.getSecurityEnabled());
        
        Properties extraProps = new Properties();
        extraProps.put("restMonitoringConfiguration", map);
        actionReport.setExtraProperties(extraProps);
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

}
