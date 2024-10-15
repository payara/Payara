package fish.payara.microprofile.config.extensions.toml;

import fish.payara.nucleus.microprofile.config.spi.ConfigSourceConfiguration;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;

@Configured(name = "toml-config-source-configuration")

public interface TOMLConfigSourceConfiguration extends ConfigSourceConfiguration {

    @Attribute(required = true)
    String getPath();
    void setPath(String tomlPath);

    @Attribute(required = true, dataType = Integer.class)
    String getDepth();
    void setDepth(String depth);
}
