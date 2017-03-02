/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.zendesk.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.appserver.zendesk.config.ZendeskSupportConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
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
 *
 * @author Andrew Pielage
 */
@Service(name = "get-zendesk-support-configuration")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = ZendeskSupportConfiguration.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-zendesk-support-configuration",
            description = "Gets the Zendesk Support Configuration")
})
public class GetZendeskSupportConfigurationCommand implements AdminCommand {

    @Inject
    private Target targetUtil;
    private final String config = "server-config";
    
    @Override
    public void execute(AdminCommandContext acc) {
        Config configNode = targetUtil.getConfig(config);
        ZendeskSupportConfiguration zendeskSupportConfiguration = 
                configNode.getExtensionByType(ZendeskSupportConfiguration.class);
        
        ActionReport actionReport = acc.getActionReport();
   
        final String[] outputHeaders = {"Email Address"};
        ColumnFormatter columnFormatter = new ColumnFormatter(outputHeaders);
        Object[] outputValues = {zendeskSupportConfiguration.getEmailAddress()};
        columnFormatter.addRow(outputValues);
        actionReport.appendMessage(columnFormatter.toString());
        
        Map<String, Object> extraPropsMap = new HashMap<>();
        extraPropsMap.put("emailAddress", zendeskSupportConfiguration.getEmailAddress());
        
        Properties extraProps = new Properties();
        extraProps.put("zendeskSupportConfiguration", extraPropsMap);
        
        actionReport.setExtraProperties(extraProps);
    }
}
