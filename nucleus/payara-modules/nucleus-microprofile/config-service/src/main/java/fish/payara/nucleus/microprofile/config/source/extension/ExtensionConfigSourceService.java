package fish.payara.nucleus.microprofile.config.source.extension;

import java.beans.PropertyChangeEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import com.sun.enterprise.config.serverbeans.Config;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import fish.payara.nucleus.microprofile.config.spi.ConfigSourceConfiguration;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;

@Service(name = "config-source-extension-handler")
public class ExtensionConfigSourceService implements EventListener, ConfigListener {

    private static final Logger logger = Logger.getLogger(ExtensionConfigSourceService.class.getName());

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    private MicroprofileConfigConfiguration configuration;

    @Inject
    private ServiceLocator locator;

    @Inject
    private Events events;

    @Inject
    private Transactions transactions;

    private final Set<ExtensionConfigSourceHandler> handlers;

    private boolean isInstance;

    public ExtensionConfigSourceService() {
        this.handlers = new HashSet<>();
    }

    @PostConstruct
    void initialize() {
        // Get the config if it's not been injected
        if (configuration == null) {
            configuration = locator.getService(MicroprofileConfigConfiguration.class);
        }

        // Register an event listener
        if (events != null) {
            events.register(this);
        }

        // Is this service running on an instance?
        final ServerEnvironment env = locator.getService(ServerEnvironment.class);
        isInstance = env.isInstance();

        // Populate the config sources list
        List<ServiceHandle<ExtensionConfigSource>> configSourceHandles = locator.getAllServiceHandles(ExtensionConfigSource.class);
        for (ServiceHandle<ExtensionConfigSource> configSourceHandle : configSourceHandles) {
            Class<?> extensionClass = configSourceHandle.getActiveDescriptor().getImplementationClass();
            Class<ConfigSourceConfiguration> configClass = ConfigSourceExtensions.getConfigurationClass(extensionClass);
            if (configClass != null) {
                handlers.add(new ExtensionConfigSourceHandler(configSourceHandle, configClass, configuration.getConfigSourceConfigurationByType(configClass)));
            } else {
                handlers.add(new ExtensionConfigSourceHandler(configSourceHandle));
            }
        }
    }

    @PreDestroy
    void destroy() {
        handlers.clear();

        if (events != null) {
            events.unregister(this);
        }
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapConfigSources();
        }
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            shutdownConfigSources();
        }
        transactions.addListenerForType(MicroprofileConfigConfiguration.class, this);
    }

    public Set<ExtensionConfigSource> getExtensionSources() {
        Set<ExtensionConfigSource> sources = new HashSet<>();
        for (ExtensionConfigSourceHandler handler : handlers) {
            sources.add(handler.getProxyConfigSource());
        }
        return sources;
    }

    private void bootstrapConfigSources() {
        handlers.forEach(ExtensionConfigSourceHandler::bootstrap);
    }

    private void shutdownConfigSources() {
        handlers.forEach(ExtensionConfigSourceHandler::destroy);
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        boolean isCurrentInstanceMatchTarget = false;
        if (isInstance) {
            isCurrentInstanceMatchTarget = true;
        } else {
            for (PropertyChangeEvent pe : events) {
                ConfigBeanProxy proxy = (ConfigBeanProxy) pe.getSource();
                while (proxy != null && !(proxy instanceof Config)) {
                    proxy = proxy.getParent();
                }

                if (proxy != null && ((Config) proxy).isDas()) {
                    isCurrentInstanceMatchTarget = true;
                    break;
                }
            }
        }

        if (isCurrentInstanceMatchTarget) {
            return ConfigSupport.sortAndDispatch(events, new Changed() {
                @Override
                public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {

                    if (changedType.equals(MicroprofileConfigConfiguration.class)) {
                        configuration = (MicroprofileConfigConfiguration) changedInstance;
                    }
                    return null;
                }
            }, logger);
        }
        return null;
    }

    public void reconfigure(ConfigSourceConfiguration config) {
        for (ExtensionConfigSourceHandler handler : handlers) {
            Class<ConfigSourceConfiguration> configClass = handler.getConfigClass();
            if (configClass.isAssignableFrom(config.getClass())) {
                handler.reconfigure();
                break;
            }
        }
    }

}
