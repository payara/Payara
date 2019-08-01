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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;

@Service(name = "_set-docker-container-id")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Server.class,
                opType = RestEndpoint.OpType.POST,
                path = "_set-docker-container-id",
                description = "Sets the Docker Container ID for an Instance")
})
public class SetDockerContainerIdCommand implements AdminCommand {

    private static final Logger logger = Logger.getLogger(SetDockerContainerIdCommand.class.getName());

    @Param(name = "instanceName", alias = "instance")
    private String instanceName;

    @Param(name = "containerId", alias = "id")
    private String containerId;

    @Inject
    Servers servers;

    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        // Validate
        if (servers == null) {
            adminCommandContext.getActionReport().failure(logger, "Could not retrieve Servers");
            return;
        }
        if (instanceName == null || instanceName.isEmpty() || containerId == null || containerId.isEmpty()) {
            adminCommandContext.getActionReport().failure(logger, "Instance Name or Container ID empty");
            return;
        }
        Server server = servers.getServer(instanceName);
        if (server == null) {
            adminCommandContext.getActionReport().failure(logger, "Could not find instance with name: "
                    + instanceName);
            return;
        }

        // Attempt to set container ID
        try {
            ConfigSupport.apply(serverProxy -> {
                serverProxy.setDockerContainerId(containerId);
                return serverProxy;
            }, server);
        } catch (TransactionFailure transactionFailure) {
            adminCommandContext.getActionReport().failure(logger, "Could not set Docker Container ID",
                    transactionFailure);
        }
    }
}
