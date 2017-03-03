/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.zendesk.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.appserver.zendesk.ZendeskSupportService;
import fish.payara.appserver.zendesk.config.ZendeskSupportConfiguration;
import java.util.logging.Logger;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "set-zendesk-support-configuration")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = ZendeskSupportConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-zendesk-support-configuration",
            description = "Sets the Zendesk Support Configuration")
})
public class SetZendeskSupportConfigurationCommand implements AdminCommand {

    @Inject
    private Target targetUtil;
    
    @Inject
    ZendeskSupportService zendeskSupport;
    
    @Param(name = "emailAddress", alias = "emailaddress")
    private String emailAddress;
    
    private final String target = "server-config";
    
    @Override
    public void execute(AdminCommandContext acc) {
        Config config = targetUtil.getConfig(target);
        ActionReport actionReport = acc.getActionReport();
        
        ZendeskSupportConfiguration zendeskSupportConfiguration = 
                config.getExtensionByType(ZendeskSupportConfiguration.class);
        
        if (zendeskSupportConfiguration != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<ZendeskSupportConfiguration>(){
                    @Override
                    public Object run(ZendeskSupportConfiguration config) {
                        config.setEmailAddress(emailAddress);
                        return null;
                    }
                }, zendeskSupportConfiguration);
            } catch (TransactionFailure ex) {
                // Set failure
                actionReport.failure(Logger.getLogger(SetZendeskSupportConfigurationCommand.class.getName()), 
                        "Failed to update configuration", ex);
            }
        }
    }
}
