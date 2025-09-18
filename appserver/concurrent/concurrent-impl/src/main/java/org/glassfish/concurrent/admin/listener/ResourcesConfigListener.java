package org.glassfish.concurrent.admin.listener;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.util.i18n.StringManager;
import fish.payara.internal.api.PostBootRunLevel;
import jakarta.inject.Inject;
import org.glassfish.concurrent.admin.ManagedExecutorServiceBaseManager;
import org.glassfish.concurrent.config.ManagedExecutorService;
import org.glassfish.concurrent.config.ManagedScheduledExecutorService;
import org.glassfish.concurrent.config.ManagedThreadFactory;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

@Service
@RunLevel(PostBootRunLevel.VAL)
public class ResourcesConfigListener implements ConfigListener, PostConstruct {

    private static StringManager sm
            = StringManager.getManager(ManagedExecutorServiceBaseManager.class);

    @Inject
    private Domain domain;

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        List<UnprocessedChangeEvent> unprocessedChangeEvents = new ArrayList<>(1);

        for (PropertyChangeEvent propertyChangeEvent : events) {
            Object source = propertyChangeEvent.getSource();

            if (!(source instanceof ManagedExecutorService
                    || source instanceof ManagedScheduledExecutorService)
                    || source instanceof ManagedThreadFactory) {
                continue;
            }

            if ("use-virtual-threads".equals(propertyChangeEvent.getPropertyName())
                    && (!propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue()))) {
                unprocessedChangeEvents.add(new UnprocessedChangeEvent(propertyChangeEvent, sm.getString("virtual.threads.change.requires.restart")));
            }
        }
        return new UnprocessedChangeEvents(unprocessedChangeEvents);
    }

    @Override
    public void postConstruct() {
        Resources resources = domain.getResources();
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(resources);
        bean.addListener(this);
    }
}
