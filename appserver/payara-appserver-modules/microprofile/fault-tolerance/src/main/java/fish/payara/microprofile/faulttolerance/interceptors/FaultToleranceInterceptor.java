package fish.payara.microprofile.faulttolerance.interceptors;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.glassfish.internal.api.Globals;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceExecution;
import fish.payara.microprofile.faulttolerance.cdi.CdiFaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils.Stereotypes;
import fish.payara.microprofile.faulttolerance.model.FaultToleranceBehaviour;
import fish.payara.microprofile.faulttolerance.model.FaultTolerancePolicy;

@Interceptor
@FaultToleranceBehaviour
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class FaultToleranceInterceptor implements Stereotypes {

    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        try {
            FaultToleranceExecution execution =
                    Globals.getDefaultBaseServiceLocator().getService(FaultToleranceExecution.class);
            FaultTolerancePolicy info = FaultTolerancePolicy.get(context, this::getConfig);
            if (info.isFaultToleranceEnabled) {
                System.out.println("yes");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return context.proceed();
    }

    private FaultToleranceConfig getConfig() {
        return new CdiFaultToleranceConfig(null, this);
    }

    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType) {
        return beanManager.isStereotype(annotationType);
    }

    @Override
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype) {
        return beanManager.getStereotypeDefinition(stereotype);
    }
}

