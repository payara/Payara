package fish.payara.docker.node.admin;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.util.StringUtils;
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
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.logging.Logger;

@Service(name = "update-node-docker")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@CommandLock(CommandLock.LockType.NONE)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean= Node.class,
                opType=RestEndpoint.OpType.POST,
                path="update-node-docker",
                description="Updates the configuration of a Docker Node",
                params={
                @RestParam(name="id", value="$parent")
        })
})
public class UpdateNodeDockerCommand implements AdminCommand {

    private static Logger logger = Logger.getLogger(UpdateNodeDockerCommand.class.getName());

    @Param(name = "name", primary = true)
    String name;

    @Param(name = "nodehost", optional = true)
    String nodehost;

    @Param(name = "nodedir", optional = true)
    String nodedir;

    @Param(name = "installdir", optional = true)
    String installdir;

    @Param(name = "dockerImage", optional = true, alias = "dockerimage")
    String dockerImage;

    @Param(name = "dockerPort", optional = true, alias = "dockerport")
    Integer dockerPort;

    @Param(name = "useTls", alias = "usetls", optional = true)
    Boolean useTls;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private Nodes nodes;

    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        ActionReport actionReport = adminCommandContext.getActionReport();

        Node node = nodes.getNode(name);

        if (node == null) {
            actionReport.setMessage("No node with given name: " + name);
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (node.isDefaultLocalNode()) {
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            actionReport.setMessage("Cannot update default node with this command");
            return;
        }

        if (!StringUtils.ok(nodehost) && !StringUtils.ok(node.getNodeHost())) {
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            actionReport.setMessage("A node must have a host");
            return;
        }

        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", name);

        if (nodehost != null) {
            parameterMap.add("nodehost", nodehost);
        }

        if (nodedir != null) {
            parameterMap.add("nodedir", nodedir);
        }

        if (installdir != null) {
            parameterMap.add("installdir", installdir);
        }

        if (dockerImage != null) {
            parameterMap.add("dockerImage", dockerImage);
        }

        if (dockerPort != null) {
            parameterMap.add("dockerPort", Integer.toString(dockerPort));
        }

        if (useTls != null) {
            parameterMap.add("useTls", useTls.toString());
        }

        if (parameterMap.size() > 1) {
            CommandRunner.CommandInvocation commandInvocation = commandRunner.getCommandInvocation(
                    "_update-node", actionReport, adminCommandContext.getSubject());
            commandInvocation.parameters(parameterMap);
            commandInvocation.execute();
        }
    }
}
