package fish.payara.microprofile.config.extensions.gcp;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;

import fish.payara.nucleus.microprofile.config.spi.ConfigSourceConfiguration;

@Configured(name = "gcp-secrets-config-source-configuration")
public interface GCPSecretsConfigSourceConfiguration extends ConfigSourceConfiguration {

    @Attribute(required = true)
    String getProjectName();
    void setProjectName(String project);

    @Attribute(required = true)
    String getClientEmail();
    void setClientEmail(String client);
    
}
