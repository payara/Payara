package fish.payara.nucleus.microprofile.config.source.extension;

import org.glassfish.config.support.GlassFishStubBean;
import org.jvnet.hk2.annotations.Contract;

import fish.payara.nucleus.microprofile.config.spi.ConfigSourceConfiguration;

/**
 * A config source that is backed by a configuration in the domain.xml
 * 
 * @param <C> the configuration class for the config course
 */
@Contract
public abstract class ConfiguredExtensionConfigSource<C extends ConfigSourceConfiguration> implements ExtensionConfigSource {

    private final Class<C> configClass;

    protected C configuration;

    public ConfiguredExtensionConfigSource() {
        this.configClass = ConfigSourceExtensions.getConfigurationClass(getClass());
    }

    public final void setConfiguration(C configuration) {
        this.configuration = GlassFishStubBean.cloneBean(configuration, configClass);
    }

    public final C getConfiguration() {
        return configuration;
    }

}