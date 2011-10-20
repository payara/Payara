package org.glassfish.virtualization.bootstrap;

import org.glassfish.api.Startup;
import org.glassfish.hk2.Factory;;
import org.glassfish.hk2.PostConstruct;
import org.glassfish.hk2.Services;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.spi.IAAS;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;

/**
 * IAAS bootstrap code to avoid loading the IMS implementation (and its dependencies)
 * until there is some virtualization related config in the configuration.
 *
 * @author Jerome Dochez
 */
@Service
public class IAASBootstrap implements Startup, PostConstruct {

    @Inject(optional=true)
    Virtualizations virtualizations=null;

    @Inject
    Transactions transactions;

    @Inject
    Services services;

    @Override
    public Lifecycle getLifecycle() {
        return Lifecycle.SERVER;
    }

    @Override
    public void postConstruct() {
        if (virtualizations==null) {
            transactions.addListenerForType(Virtualizations.class, new ConfigListener() {
                    @Override
                    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
                        services.forContract(IAAS.class).get();
                        return null;
                    }
                });
        } else {
            services.forContract(IAAS.class).get();
        }
    }
}
