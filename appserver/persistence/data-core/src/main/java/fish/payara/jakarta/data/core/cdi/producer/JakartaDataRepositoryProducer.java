package fish.payara.jakarta.data.core.cdi.producer;

import jakarta.data.repository.Repository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

@ApplicationScoped
public class JakartaDataRepositoryProducer {
    
    @Produces
    @Default
    public  Repository getRepository(InjectionPoint injectionPoint) {
        Repository repository = injectionPoint.getAnnotated().getAnnotation(Repository.class);
        return null;
    }
}
