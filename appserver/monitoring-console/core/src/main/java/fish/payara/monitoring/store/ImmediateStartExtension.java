package fish.payara.monitoring.store;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.glassfish.internal.api.Globals;

/**
 * The sole purpose of this extension is to eagerly create the {@link MonitoringDataRepository}.
 *
 * @author Jan Bernitt
 */
public class ImmediateStartExtension implements Extension {

    void beforeBeanDiscovery(@SuppressWarnings("unused") @Observes BeforeBeanDiscovery beforeBeanDiscovery) {
        Globals.getDefaultBaseServiceLocator().getService(MonitoringDataRepository.class);
    }
}
