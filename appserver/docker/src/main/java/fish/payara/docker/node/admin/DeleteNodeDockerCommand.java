package fish.payara.docker.node.admin;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.logging.Logger;

import static org.glassfish.api.ActionReport.ExitCode.FAILURE;

@Service(name = "delete-node-docker")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@CommandLock(CommandLock.LockType.NONE)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean= Nodes.class,
                opType=RestEndpoint.OpType.DELETE,
                path="delete-node-docker",
                description="Deletes a Docker Node")
})
public class DeleteNodeDockerCommand implements AdminCommand {

    @Param(name = "name", primary = true)
    private String name;

    @Inject
    private Nodes nodes;

    @Inject
    private CommandRunner commandRunner;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values are the parameter values
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        Logger logger = context.getLogger();
        Node node = nodes.getNode(name);

        if (node == null) {
            // No node to delete nothing to do here
            actionReport.setActionExitCode(FAILURE);
            actionReport.setMessage("No node found with given name: " + name);
            return;
        }

        if (!(node.getType().equals("DOCKER"))) {
            // No node to delete nothing to do here
            actionReport.setActionExitCode(FAILURE);
            actionReport.setMessage("Node with given name is not a docker node: " + name);
            return;

        }

        CommandRunner.CommandInvocation commandInvocation = commandRunner.getCommandInvocation(
                "_delete-node", actionReport, context.getSubject());
        ParameterMap commandParameters = new ParameterMap();
        commandParameters.add("DEFAULT", name);
        commandInvocation.parameters(commandParameters);

        commandInvocation.execute();
    }
}
