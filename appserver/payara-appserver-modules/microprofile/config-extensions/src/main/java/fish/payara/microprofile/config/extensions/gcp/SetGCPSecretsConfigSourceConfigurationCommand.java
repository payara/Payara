package fish.payara.microprofile.config.extensions.gcp;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.sun.enterprise.util.StringUtils;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
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

    private static final Logger LOGGER = Logger.getLogger(SetGCPSecretsConfigSourceConfigurationCommand.class.getPackage().getName());

    @Param
    protected String project;

    @Param
    private File jsonKeyFile;

    @Inject
    private ServerEnvironment env;

    @Override
    protected void applyValues(GCPSecretsConfigSourceConfiguration configuration) throws PropertyVetoException {
        super.applyValues(configuration);
        if (StringUtils.ok(project)) {
            configuration.setProjectName(project);
        }
        if (jsonKeyFile != null) {
            if (!jsonKeyFile.exists() || !jsonKeyFile.isFile()) {
                throw new PropertyVetoException("JSON Key file not found", new PropertyChangeEvent(configuration, "jsonKeyFile", null, jsonKeyFile));
            }
            try {
                Path configDirPath = env.getConfigDirPath().toPath();
                Path output = Files.copy(jsonKeyFile.toPath(), configDirPath.resolve(jsonKeyFile.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
                configuration.setTokenFilePath(configDirPath.relativize(output).toString());
                LOGGER.info("File copied to " + output);
            } catch (IOException e) {
                final String message = "Unable to find or access target file";
                LOGGER.log(Level.WARNING, message, e);
                throw new PropertyVetoException(message, new PropertyChangeEvent(configuration, "jsonKeyFile", null, jsonKeyFile));
            }
        }
    }
}