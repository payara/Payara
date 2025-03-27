package org.glassfish.jdbcruntime.service;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

@Service
@RunLevel(StartupRunLevel.VAL)
public class JdbcServiceConfigListener implements ConfigListener {

    @Inject
    private JdbcConnectionPool jdbcConnectionPool;

    @Inject
    private Transactions transactions;

    @PostConstruct
    public void postConstruct() {
        transactions.addListenerForType(JdbcConnectionPool.class, this);
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        List<UnprocessedChangeEvent> unprocessedChanges = new ArrayList<>();
        for (PropertyChangeEvent pce : events) {
            if (pce.getPropertyName().equalsIgnoreCase("skip-client-info-validation")
                    && Boolean.parseBoolean(pce.getOldValue().toString()) != Boolean.parseBoolean(pce.getNewValue().toString())) {
                unprocessedChanges.add(new UnprocessedChangeEvent(pce, "JDBC Skip Client Info Validation Changed"));
            }
        }

        if (unprocessedChanges.isEmpty()) {
            return null;
        }
        return new UnprocessedChangeEvents(unprocessedChanges);
    }
}
