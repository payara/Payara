package fish.payara.nucleus.microprofile.config.spi;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

@Configured
public interface ConfigSourceConfiguration extends ConfigBeanProxy {

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getEnabled();
    void setEnabled(String value);

}
