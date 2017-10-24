package org.glassfish.admin.cli.resources;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Resources;
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

@Service(name = "update-resource-ref")
@I18n("update.resource.ref")
@PerLookup
@ExecuteOn(value = RuntimeType.DAS)
@TargetType(value = {CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Resources.class, opType = RestEndpoint.OpType.POST, path = "update-resource-ref", description = "Update a Resource Reference on a given target")
})
public class UpdateResourceRef implements AdminCommand {

    final private static LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(UpdateResourceRef.class);

    @Param(primary = true)
    private String name;

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(optional = true)
    private Boolean enabled = null;

    @Inject
    private Domain domain;

    /**
     * Execution method for updating the configuration of a ResourceRef. Will be
     * replicated if the target is a cluster.
     *
     * @param context context for the command.
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        // Make a list of all ResourceRefs that need to change
        List<ResourceRef> resourceRefsToChange = new ArrayList<>();

        // Add the ResourceRef from a named server if the target is a server
        Server server = domain.getServerNamed(target);
        // if the target is a server
        if (server != null) {
            ResourceRef serverResourceRef = server.getResourceRef(name);
            // if the ResourceRef doesn't exist
            if (serverResourceRef == null) {
                report.failure(logger, LOCAL_STRINGS.getLocalString("resource.ref.not.exists", "Target {1} does not have a reference to resource {0}.", name, target));
                return;
            }
            resourceRefsToChange.add(serverResourceRef);
        }

        // Add the ResourceRef from a named config if the target is a config
        Config config = domain.getConfigNamed(target);
        // if the target is a config
        if (config != null) {
            ResourceRef configResourceRef = config.getResourceRef(name);
            // if the ResourceRef doesn't exist
            if (configResourceRef == null) {
                report.failure(logger, LOCAL_STRINGS.getLocalString("resource.ref.not.exists", "Target {1} does not have a reference to resource {0}.", name, target));
                return;
            }
            resourceRefsToChange.add(configResourceRef);
        }

        // Add the ResourceRefs from a named cluster if the target is a cluster
        Cluster cluster = domain.getClusterNamed(target);
        // if the target is a cluster
        if (cluster != null) {
            ResourceRef clusterResourceRef = cluster.getResourceRef(name);
            // if the ResourceRef doesn't exist
            if (clusterResourceRef == null) {
                report.failure(logger, LOCAL_STRINGS.getLocalString("resource.ref.not.exists", "Target {1} does not have a reference to resource {0}.", name, target));
                return;
            }
            resourceRefsToChange.add(clusterResourceRef);
            for (Server instance : cluster.getInstances()) {
                ResourceRef instanceResourceRef = instance.getResourceRef(name);
                // if the server in the cluster contains the ResourceRef
                if (instanceResourceRef != null) {
                    resourceRefsToChange.add(instanceResourceRef);
                }
            }
        }

        // Apply the configuration to the listed ResourceRefs
        try {
            ConfigSupport.apply(new ConfigCode() {
                @Override
                public Object run(ConfigBeanProxy... params) throws PropertyVetoException, TransactionFailure {
                    for (ConfigBeanProxy proxy : params) {
                        if (proxy instanceof ResourceRef) {
                            ResourceRef resourceRefProxy = (ResourceRef) proxy;
                            if (enabled != null) {
                                resourceRefProxy.setEnabled(enabled.toString());
                            }
                        }
                    }
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return true;
                }
            }, resourceRefsToChange.toArray(new ResourceRef[]{}));
        } catch (TransactionFailure ex) {
            report.failure(logger, ex.getLocalizedMessage());
        }
    }

}
