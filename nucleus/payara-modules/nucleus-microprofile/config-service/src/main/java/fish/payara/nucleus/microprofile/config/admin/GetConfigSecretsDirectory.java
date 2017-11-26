/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.microprofile.config.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * asAdmin command to the set the directory for the Secrets Dir Config Source
 *
 * @since 4.1.2.181
 * @author Steve Millidge (Payara Foundation)
 */
@Service(name = "get-config-secrets-dir") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
@ExecuteOn()
@TargetType()
@RestEndpoints({ // creates a REST endpoint needed for integration with the admin interface
    
    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.POST, // must be POST as it is doing an update
            path = "get-config-secrets-dir",
            description = "Gets the Secrets Directory for the Secrets Config Source")
})
public class GetConfigSecretsDirectory implements AdminCommand {

    @Param(optional = true, defaultValue = "server") // if no target is specified it will be the DAS
    String target;

    @Inject
    Target targetUtil;    

    @Override
    public void execute(AdminCommandContext context) {
        String result = "Not Found";
        Config configVal = targetUtil.getConfig(target);
        MicroprofileConfigConfiguration serviceConfig = configVal.getExtensionByType(MicroprofileConfigConfiguration.class);
        if (serviceConfig != null) {
            result = serviceConfig.getSecretDir();
        }
        context.getActionReport().setMessage(result);
    }
}
