package fish.payara.jakarta.data.core.cdi.extension;

import fish.payara.jakarta.data.core.cdi.producer.JakartaDataRepositoryProducer;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

/**
 * 
 * @param <E>
 */
public class JakartaDataCDIExtension<E extends Member & AnnotatedElement> implements Extension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager manager) {
        addAnnotatedType(JakartaDataRepositoryProducer.class, manager, beforeBeanDiscovery);
    }

    private static <T extends Object> void addAnnotatedType(Class<T> type, BeanManager manager, BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.addAnnotatedType(manager.createAnnotatedType(type), type.getName());
    }
}
