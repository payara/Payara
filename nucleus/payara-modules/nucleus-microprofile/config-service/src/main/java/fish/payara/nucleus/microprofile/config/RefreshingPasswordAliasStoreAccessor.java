package fish.payara.nucleus.microprofile.config;


import com.sun.enterprise.security.store.DomainScopedPasswordAliasStore;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

@Service
class RefreshingPasswordAliasStoreAccessor implements EventListener {
    // HK2 doesn't inject ServiceHandle<T>
    @Inject
    ServiceLocator locator;

    @Inject
    Events events;

    private volatile DomainScopedPasswordAliasStore currentStore;

    @PostConstruct
    void postConstruct() {
        events.register(this);
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.DEPLOYMENT_START)) {
            DomainScopedPasswordAliasStore previousStore = currentStore;
            // we have no clear indication when a password alias is changed, but a good heuristics is
            // that it might get updated before deployment of an application. That way it is also
            // compatible with the previous behavior where the source would be created per application
            currentStore = null;
            if (previousStore != null) {
                locator.preDestroy(previousStore);
            }
        }
    }

    DomainScopedPasswordAliasStore getCurrentStore() {
        if (currentStore == null) {
            synchronized (this) {
                if (currentStore == null) {
                    currentStore = locator.getService(DomainScopedPasswordAliasStore.class);
                }
            }
        }
        return currentStore;
    }
}
