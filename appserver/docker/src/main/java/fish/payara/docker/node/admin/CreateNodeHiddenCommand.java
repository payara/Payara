package fish.payara.docker.node.admin;

import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_NODEHOST;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_TYPE;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;

@Service(name = "_create-node-hidden")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
        @RestEndpoint(configBean = Nodes.class,
                opType = POST,
                path = "_create-node-hidden",
                description = "Create Node Hidden")
})
public class CreateNodeHiddenCommand implements AdminCommand {

    @Param(name = "name", primary = true)
    String name;

    @Param(name = "nodehost")
    String nodehost;

    @Inject
    private CommandRunner commandRunner;

    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        CommandRunner.CommandInvocation commandInvocation = commandRunner.getCommandInvocation("_create-node",
                adminCommandContext.getActionReport(), adminCommandContext.getSubject());
        ParameterMap commandParameters = new ParameterMap();

        commandParameters.add("DEFAULT", name);
        if (StringUtils.ok(nodehost)) {
            commandParameters.add(PARAM_NODEHOST, nodehost);
        }
        commandParameters.add(PARAM_TYPE, "HIDDEN");

        commandInvocation.parameters(commandParameters);
        commandInvocation.execute();
    }
}
