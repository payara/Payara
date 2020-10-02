package fish.payara.nucleus.microprofile.config.source.extension;

import java.lang.reflect.ParameterizedType;

import org.glassfish.hk2.api.ServiceHandle;

import fish.payara.nucleus.microprofile.config.spi.ConfigSourceConfiguration;

/**
 * A utility class for handling config source extensions
 */
public class ConfigSourceExtensions {

    private ConfigSourceExtensions() {}

    public static final String getName(ServiceHandle<? extends ExtensionConfigSource> handle) {
        final String configSourceName = handle.getService().getName();
        if (configSourceName != null && !configSourceName.isEmpty()) {
            return configSourceName;
        }
        final String serviceName = handle.getActiveDescriptor().getName();
        if (serviceName != null && !serviceName.isEmpty()) {
            return serviceName;
        }
        final String className = handle.getActiveDescriptor().getImplementationClass().getSimpleName();
        if (className != null && !className.isEmpty()) {
            return className;
        }
        return "null";
    }

    /**
     * @param <C>               a generic class of the config source configuration
     *                          class
     * @param configSourceClass the config of the config source
     * @return the class used to configure the configured config source
     */
    @SuppressWarnings("unchecked")
    public static <C extends ConfigSourceConfiguration> Class<C> getConfigurationClass(Class<?> configSourceClass) {
        final ParameterizedType genericSuperclass = (ParameterizedType) configSourceClass.getGenericSuperclass();
        return (Class<C>) genericSuperclass.getActualTypeArguments()[0];
    }

}
