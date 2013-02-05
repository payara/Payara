package org.glassfish.concurrent.runtime.deployer;

import org.glassfish.concurrent.config.ManagedScheduledExecutorService;

/**
 * Contains configuration information for a ManagedScheduledExecutorService object
 */
public class ManagedScheduledExecutorServiceConfig extends ManagedExecutorServiceConfig {
    public ManagedScheduledExecutorServiceConfig(ManagedScheduledExecutorService config) {
        super(config);
    }

    @Override
    public TYPE getType() {
        return TYPE.MANAGED_SCHEDULED_EXECUTOR_SERVICE;
    }
}
