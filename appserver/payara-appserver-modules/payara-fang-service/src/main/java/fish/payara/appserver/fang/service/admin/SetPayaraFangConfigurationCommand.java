/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.fang.service.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.appserver.fang.service.configuration.PayaraFangConfiguration;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;
import javax.inject.Inject;
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

    private final static Logger LOGGER = Logger.getLogger(SetPayaraFangConfigurationCommand.class.getName());
    
    @Param(optional = true, defaultValue = "server-config")
    String target;
    
    @Param(optional = true)
    Boolean enabled;
    
    @Param(optional = true, alias = "contextroot")
    String contextRoot;
    
    @Param(optional = true)
    String name;
    
    @Inject
    private Target targetUtil;
    
    @Override
    public void execute(AdminCommandContext context) {
        Config targetConfig = targetUtil.getConfig(target);
        PayaraFangConfiguration fangConfiguration = targetConfig.getExtensionByType(PayaraFangConfiguration.class);    
        
        try {
            ConfigSupport.apply(new SingleConfigCode<PayaraFangConfiguration>(){
                    @Override
                    public Object run(PayaraFangConfiguration configProxy) throws PropertyVetoException {
                        if (enabled != null) {
                            configProxy.setEnabled(enabled.toString());
                        }
                        
                        if (contextRoot != null) {
                            configProxy.setContextRoot(contextRoot);
                        }
                        
                        if (name != null) {
                            configProxy.setApplicationName(name);
                        }
                        
                        return null;
                    }
            }, fangConfiguration);
        } catch (TransactionFailure ex) {
            context.getActionReport().failure(LOGGER, "Failed to update Payara Fang configuration", ex);
        }
    }
    
}
