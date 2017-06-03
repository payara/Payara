package fish.payara.deployment.admin;

import com.sun.enterprise.config.serverbeans.Applications;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service(name="initialize-all-applications")
@PerLookup
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Applications.class, opType = RestEndpoint.OpType.POST, path = "initialize-all-applications"),
})
public class InitializeAllApplicationsCommand implements AdminCommand {
    @Override
    public void execute(AdminCommandContext context) {
        for(String appName : appRegistry.getAllApplicationNames()) {
            Deployment.ApplicationDeployment depl = appRegistry.getTransient(appName);
            if(depl != null) {
                deployment.initialize(depl.appInfo, depl.appInfo.getSniffers(), depl.context);
            }
        }
    }


    private @Inject ApplicationRegistry appRegistry;
    private @Inject Deployment deployment;
    private final static Logger logger = Logger.getLogger(InitializeAllApplicationsCommand.class.getName());
}
