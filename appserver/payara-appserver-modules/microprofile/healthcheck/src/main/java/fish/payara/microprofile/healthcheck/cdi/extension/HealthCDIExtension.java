package fish.payara.microprofile.healthcheck.cdi.extension;

import static fish.payara.microprofile.healthcheck.HealthCheckType.HEALTH;
import static fish.payara.microprofile.healthcheck.HealthCheckType.LIVENESS;
import static fish.payara.microprofile.healthcheck.HealthCheckType.READINESS;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.eclipse.microprofile.health.HealthCheck;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import fish.payara.microprofile.healthcheck.HealthCheckService;
import fish.payara.microprofile.healthcheck.HealthCheckType;

public class HealthCDIExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(HealthCDIExtension.class.getName());

    private final HealthCheckService healthService;
    private final String appName;

    public HealthCDIExtension() {
        final ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
        final InvocationManager invocationManager = serviceLocator.getService(InvocationManager.class);
        this.appName = invocationManager.getCurrentInvocation().getAppName();
        this.healthService = serviceLocator.getService(HealthCheckService.class);
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        final Set<Bean<?>> beans = new HashSet<>();

        beans.addAll(beanManager.getBeans(HealthCheck.class, READINESS.getLiteral()));
        beans.addAll(beanManager.getBeans(HealthCheck.class, LIVENESS.getLiteral()));
        beans.addAll(beanManager.getBeans(HealthCheck.class, HEALTH.getLiteral()));
        
        for (Bean<?> bean : beans) {
            HealthCheck healthCheck = (HealthCheck) beanManager.getReference(
                    bean,
                    HealthCheck.class,
                    beanManager.createCreationalContext(bean)
            );

            healthService.registerHealthCheck(
                    appName, 
                    healthCheck, 
                    HealthCheckType.fromQualifiers(bean.getQualifiers())
            );
            healthService.registerClassLoader(
                    appName,
                    healthCheck.getClass().getClassLoader()
            );

            LOGGER.log(Level.INFO,
                    "Registered {0} as a HealthCheck for app: {1}",
                    new Object[]{bean.getBeanClass().getCanonicalName(), appName}
            );
        }
    }
    
}
