package fish.payara.docker.instance.admin;

import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.logging.Logger;

@Service(name = "_get-docker-container-id")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Server.class,
                opType = RestEndpoint.OpType.GET,
                path = "_get-docker-container-id",
                description = "Gets the Docker Container ID for an Instance")
})
public class GetDockerContainerIdCommand implements AdminCommand {

    private static final Logger logger = Logger.getLogger(SetDockerContainerIdCommand.class.getName());

    @Param(name = "instanceName", alias = "instance")
    private String instanceName;

    @Inject
    Servers servers;

    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        // Validate
        if (servers == null) {
            adminCommandContext.getActionReport().failure(logger, "Could not retrieve Servers");
            return;
        }
        if (instanceName == null || instanceName.isEmpty()) {
            adminCommandContext.getActionReport().failure(logger, "No instance name provided");
            return;
        }
        Server server = servers.getServer(instanceName);
        if (server == null) {
            adminCommandContext.getActionReport().failure(logger, "Could not find instance with name: "
                    + instanceName);
            return;
        }

        adminCommandContext.getActionReport().setMessage(server.getDockerContainerId());
    }
}
