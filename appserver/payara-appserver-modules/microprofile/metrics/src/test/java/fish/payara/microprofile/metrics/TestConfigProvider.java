package fish.payara.microprofile.metrics;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

public class TestConfigProvider extends ConfigProviderResolver {

    private final Config config = new TestConfig();
    
    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        return config;
    }

    @Override
    public ConfigBuilder getBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void releaseConfig(Config config) {
        // do nothing...
    }

}
