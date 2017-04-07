/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.fang.service.admin;

import fish.payara.appserver.fang.service.configuration.PayaraFangConfiguration;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "set-payara-fang-configuration")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = PayaraFangConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-payara-fang-configuration",
            description = "Sets the Payara Fang Configuration")
})
public class SetPayaraFangConfigurationCommand implements AdminCommand {

    @Override
    public void execute(AdminCommandContext context) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
