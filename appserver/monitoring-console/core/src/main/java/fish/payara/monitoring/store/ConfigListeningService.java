package fish.payara.monitoring.store;

import static java.lang.Boolean.parseBoolean;
import static org.jvnet.hk2.config.Dom.unwrap;

import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;

import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import com.sun.enterprise.config.serverbeans.MonitoringService;

public abstract class ConfigListeningService implements ConfigListener {

    protected static final Logger LOGGER = Logger.getLogger("monitoring-console-core");

    @Override
    public final UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        for (PropertyChangeEvent e : events) {
            if (e.getSource() instanceof ConfigBeanProxy) {
                Class<?> source = unwrap((ConfigBeanProxy)e.getSource()).getImplementationClass();
                if (source == MonitoringService.class) {
                    String property = e.getPropertyName();
                    if ("monitoring-enabled".equals(property)) {
                        changedConfig(parseBoolean(e.getNewValue().toString()));
                    }
                }
            }
        }
        return null;
    }

    abstract void changedConfig(boolean enabled);

}
