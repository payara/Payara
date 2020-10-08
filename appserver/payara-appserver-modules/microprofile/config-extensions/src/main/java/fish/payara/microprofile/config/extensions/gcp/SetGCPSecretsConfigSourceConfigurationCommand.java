package fish.payara.microprofile.config.extensions.gcp;

import java.beans.PropertyVetoException;
import java.io.File;

import com.sun.enterprise.util.StringUtils;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import fish.payara.nucleus.microprofile.config.source.extension.BaseSetConfigSourceConfigurationCommand;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;

@Service(name = "set-gcp-secrets-config-source-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
                opType = RestEndpoint.OpType.POST,
                path = "set-gcp-secrets-config-source-configuration",
                description = "Configures GCP Secrets Config Source")
})
public class SetGCPSecretsConfigSourceConfigurationCommand extends BaseSetConfigSourceConfigurationCommand<GCPSecretsConfigSourceConfiguration> {

    @Param(optional = true)
    protected String project;

    @Param(name = "keyJsonFile", optional = false)
    private File keyJsonFile;

    @Override
    protected void applyValues(GCPSecretsConfigSourceConfiguration configuration) throws PropertyVetoException {
        super.applyValues(configuration);
        if (StringUtils.ok(project)) {
            configuration.setProjectName(project);
        }
        if (keyJsonFile != null && keyJsonFile.exists()) {
            configuration.setClientEmail("test-account@payara-bingo.iam.gserviceaccount.com");
        }
    }
}