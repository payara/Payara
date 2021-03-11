package fish.payara.microprofile.healthcheck.cdi.extension;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import fish.payara.microprofile.healthcheck.HealthCheckService;
import fish.payara.microprofile.healthcheck.HealthCheckType;

public class HealthCDIExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(HealthCDIExtension.class.getName());

    private final HealthCheckService healthService;
    private final String appName;

    private final Set<Bean<?>> healthCheckBeans;

    public HealthCDIExtension() {
        final ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
        final InvocationManager invocationManager = serviceLocator.getService(InvocationManager.class);
        this.appName = invocationManager.getCurrentInvocation().getAppName();
        this.healthService = serviceLocator.getService(HealthCheckService.class);
        this.healthCheckBeans = new HashSet<>();
    }

    void processBean(@Observes ProcessBean<?> event) {
        final Annotated annotated = event.getAnnotated();
        if (annotated.isAnnotationPresent(Readiness.class)
                || annotated.isAnnotationPresent(Liveness.class)) {
            this.healthCheckBeans.add(event.getBean());
        }
    }

    void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init, BeanManager beanManager) {
        Iterator<Bean<?>> beanIterator = healthCheckBeans.iterator();
        while (beanIterator.hasNext()) {
            registerHealthCheck(beanIterator.next(), beanManager);
            beanIterator.remove();
        }
    }

    private void registerHealthCheck(Bean<?> bean, BeanManager beanManager) {
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
