package com.sun.common.util.logging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

@Service
@Singleton
public class LoggingConfigFactory implements Factory<LoggingConfig> {

    private static final Logger LOGGER = Logger.getLogger(LoggingConfigFactory.class.getName());

    @Inject
    private ServiceLocator locator;

    private final Map<String, LoggingConfig> configs;

    public LoggingConfigFactory() {
        configs = new HashMap<>();
    }

    @Override
    public void dispose(LoggingConfig config) {
        if (config == null) {
            return;
        }
        Iterator<Entry<String, LoggingConfig>> configIterator = configs.entrySet().iterator();
        while (configIterator.hasNext()) {
            Entry<String, LoggingConfig> configEntry = configIterator.next();
            if (configEntry.getValue().equals(config)) {
                configIterator.remove();
            }
        }
    }

    public LoggingConfig provide(String target) {
        // Try and fetch from the map
        LoggingConfig config = configs.get(target);
        if (config != null) {
            return config;
        }

        // Create a new one
        config = locator.createAndInitialize(LoggingConfigImpl.class);
        try {
            config.initialize(target);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "An error occurred while reading from the logs for the target '" + target + "'.", ex);
            return null;
        }
        configs.put(target, config);
        return config;
    }

    @Override
    public LoggingConfig provide() {
        return provide(null);
    }

}