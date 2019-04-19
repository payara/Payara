package fish.payara.microprofile.faulttolerance.cdi;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.glassfish.internal.api.Globals;

import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.service.Stereotypes;

@Interceptor
@FaultTolerance
@Priority(Interceptor.Priority.PLATFORM_AFTER + 15)
public class FaultToleranceInterceptor implements Stereotypes, Serializable {

    private static final Logger logger = Logger.getLogger(FaultToleranceInterceptor.class.getName());

    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        try {
            FaultToleranceService env =
                    Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
            FaultTolerancePolicy policy = FaultTolerancePolicy.get(context, () -> env.getConfig(context, this));
            if (policy.isPresent) {
                return policy.proceed(context, env);
            }
        } catch (FaultToleranceDefinitionException e) {
            logger.log(Level.SEVERE, "Effective FT policy contains illegal values, fault tolerance cannot be applied,"
                    + " falling back to plain method invocation.", e);
            // fall-through to normal proceed
        }
        return context.proceed();
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

