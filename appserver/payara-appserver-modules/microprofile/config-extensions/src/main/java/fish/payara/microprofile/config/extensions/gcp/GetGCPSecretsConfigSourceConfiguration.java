package fish.payara.microprofile.config.extensions.gcp;

import java.util.Map;

import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import fish.payara.nucleus.microprofile.config.source.extension.BaseGetConfigSourceConfigurationCommand;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;

@Service(name = "get-gcp-secrets-config-source-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
                opType = RestEndpoint.OpType.POST,
                path = "get-gcp-secrets-config-source-configuration",
                description = "List GCP Secrets Config Source Configuration")
})
public class GetGCPSecretsConfigSourceConfiguration extends BaseGetConfigSourceConfigurationCommand<GCPSecretsConfigSourceConfiguration> {

    @Override
    protected Map<String, Object> getConfigSourceConfiguration(GCPSecretsConfigSourceConfiguration configuration) {
        Map<String, Object> config = super.getConfigSourceConfiguration(configuration);
        if (config != null) {
            config.put("Project Name", configuration.getProjectName());
            config.put("Client Email", configuration.getClientEmail());
        }
        return config;
    }
}
