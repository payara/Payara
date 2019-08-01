package fish.payara.docker.instance.admin;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.json.Json;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Service(name = "_stop-docker-container")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@RestEndpoints({
        @RestEndpoint(configBean= Server.class,
                opType=RestEndpoint.OpType.POST,
                path="_stop-docker-container",
                description="Stops the Docker contain that this instance exists on",
                params={
                        @RestParam(name="id", value="$parent")
                })
})
public class StopDockerContainerCommand implements AdminCommand {

    @Param(name = "nodeName", alias = "node")
    String nodeName;

    @Param(name = "instanceName", alias = "instance")
    String instanceName;

    @Inject
    private Nodes nodes;

    @Inject
    private Servers servers;

    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        Node node = nodes.getNode(nodeName);
        Server server = servers.getServer(instanceName);

        Client client = ClientBuilder.newClient();

        WebTarget webTarget;
        if (Boolean.valueOf(node.getUseTls())) {
            webTarget = client.target("https://"
                    + node.getNodeHost()
                    + ":"
                    + node.getDockerPort()
                    + "/containers/"
                    + server.getDockerContainerId()
                    + "/stop");
        } else {
            webTarget = client.target("http://"
                    + node.getNodeHost()
                    + ":"
                    + node.getDockerPort()
                    + "/containers/"
                    + server.getDockerContainerId()
                    + "/stop");
        }

        // Send the POST request
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(
                Entity.entity(Json.createObjectBuilder().build(), MediaType.APPLICATION_JSON));

        // Check status of response and act on result, ignoring 304 (NOT MODIFIED)
        Response.StatusType responseStatus = response.getStatusInfo();
        if (!responseStatus.getFamily().equals(Response.Status.Family.SUCCESSFUL)
                && responseStatus.getStatusCode() != Response.Status.NOT_MODIFIED.getStatusCode()) {
            adminCommandContext.getActionReport().failure(adminCommandContext.getLogger(),
                    "Failed to stop Docker Container: \n" + responseStatus.getReasonPhrase());
        }
    }
}
