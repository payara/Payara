package org.glassfish.deployment.admin;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

@Service(name = "update-application-ref")
@I18n("update.application.ref.command")
@PerLookup
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Cluster.class, opType = RestEndpoint.OpType.POST,
            path = "update-application-ref", description = "Update an Application Reference on a cluster target"),
    @RestEndpoint(configBean = Server.class, opType = RestEndpoint.OpType.POST,
            path = "update-application-ref", description = "Update an Application Reference on a server target")
})
public class UpdateApplicationRefCommand implements AdminCommand {

    final private static LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(UpdateApplicationRefCommand.class);

    @Param(primary = true)
    private String name;

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(optional = true, alias = "virtual-servers")
    private String virtualservers = null;

    @Param(optional = true)
    private Boolean enabled = null;

    @Param(optional = true)
    private Boolean lbenabled = null;

    @Inject
    private Domain domain;

    /**
     * Execution method for updating the configuration of an ApplicationRef.
     * Will be replicated if the target is a cluster.
     *
     * @param context context for the command.
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        // Make a list of all ApplicationRefs that need to change
        List<ApplicationRef> applicationRefsToChange = new ArrayList<>();

        // Add the ApplicationRef which is being immediately targetted
        {
            ApplicationRef primaryApplicationRef = domain.getApplicationRefInTarget(name, target);
            if (primaryApplicationRef == null) {
                report.failure(logger, LOCAL_STRINGS.getLocalString("appref.not.exists", "Target {1} does not have a reference to application {0}.", name, target));
                return;
            }
            applicationRefsToChange.add(primaryApplicationRef);
        }

        // Add the implicitly targetted ApplicationRefs if the target is in a cluster
        {
            Cluster cluster = domain.getClusterNamed(target);
            // if the target is a cluster
            if (cluster != null) {
                for (Server server : cluster.getInstances()) {
                    ApplicationRef instanceAppRef = server.getApplicationRef(name);
                    // if the server in the cluster contains the ApplicationRef
                    if (instanceAppRef != null) {
                        applicationRefsToChange.add(instanceAppRef);
                    }
                }
            }
        }

        // Apply the configuration to the listed ApplicationRefs
        try {
            ConfigSupport.apply(new ConfigCode() {
                @Override
                public Object run(ConfigBeanProxy... params) throws PropertyVetoException, TransactionFailure {
                    for (ConfigBeanProxy proxy : params) {
                        if (proxy instanceof ApplicationRef) {
                            ApplicationRef applicationRefProxy = (ApplicationRef) proxy;
                            if (enabled != null) {
                                applicationRefProxy.setEnabled(enabled.toString());
                            }
                            if (virtualservers != null) {
                                applicationRefProxy.setVirtualServers(virtualservers);
                            }
                            if (lbenabled != null) {
                                applicationRefProxy.setLbEnabled(lbenabled.toString());
                            }
                        }
                    }
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return true;
                }
            }, applicationRefsToChange.toArray(new ApplicationRef[]{}));
        } catch (TransactionFailure ex) {
            report.failure(logger, ex.getLocalizedMessage());
        }
    }

}
