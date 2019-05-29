package fish.payara.docker.node.admin;


import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.v3.admin.cluster.NodeUtils;
import fish.payara.docker.DockerConstants;
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

@Service(name = "create-node-docker")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@CommandLock(CommandLock.LockType.NONE)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean= Nodes.class,
                opType=RestEndpoint.OpType.POST,
                path="create-node-docker",
                description="Create a Docker Node to spawn containers on")
})
public class CreateNodeDockerCommand implements AdminCommand {

    @Param(name = "name", primary = true)
    String name;

    @Param(name = NodeUtils.PARAM_NODEHOST)
    String nodehost;

    @Param(name = NodeUtils.PARAM_NODEDIR, optional = true)
    String nodedir;

    @Param(name = NodeUtils.PARAM_INSTALLDIR, optional = true, defaultValue = "/opt/payara/payara5")
    String installdir;

    @Param(name = "dockerPasswordFile", alias = "dockerpasswordfile")
    String dockerPasswordFile;

    @Param(name = "dockerImage", alias = "dockerimage", defaultValue = "payara-3314-server-full")
    String dockerImage;

    @Param(name = "dockerPort", optional = true, alias = "dockerport")
    Integer dockerPort;

    @Param(name = "useTls", alias = "usetls", optional = true)
    Boolean useTls;

    @Inject
    private CommandRunner commandRunner;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values are the parameter values
     *
     * @param adminCommandContext information
     */
    @Override
    public void execute(AdminCommandContext adminCommandContext) {
        ActionReport actionReport = adminCommandContext.getActionReport();

        CommandRunner.CommandInvocation commandInvocation = commandRunner.getCommandInvocation(
                "_create-node", actionReport, adminCommandContext.getSubject());

        ParameterMap map = new ParameterMap();
        map.add("DEFAULT", name);

        map.add(NodeUtils.PARAM_INSTALLDIR, DockerConstants.PAYARA_INSTALL_DIR);

        if (StringUtils.ok(nodehost)) {
            map.add("nodehost", nodehost);
        }

        if (StringUtils.ok(nodedir)) {
            map.add("nodedir", nodedir);
        }

        if (StringUtils.ok(installdir)) {
            map.add("installdir", installdir);
        }

        if (StringUtils.ok(dockerPasswordFile)) {
            map.add("dockerPasswordFile", dockerPasswordFile);
        }

        if (StringUtils.ok(dockerImage)) {
            map.add("dockerImage", dockerImage);
        } else {
            // Can't be added to default of parameter or attribute due to not being a constant
            dockerImage = "payara/server-full:" + Version.getMajorVersion() + "." + Version.getMinorVersion();
            map.add("dockerImage", dockerImage);
        }

        if (dockerPort != null) {
            map.add("dockerPort", Integer.toString(dockerPort));
        }

        if (useTls != null) {
            map.add("useTls", useTls.toString());
        }

        map.add("type","DOCKER");
        commandInvocation.parameters(map);
        commandInvocation.execute();
    }
}
