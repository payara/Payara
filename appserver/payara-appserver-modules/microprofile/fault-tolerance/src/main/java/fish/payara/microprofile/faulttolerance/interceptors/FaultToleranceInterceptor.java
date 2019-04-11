package fish.payara.microprofile.faulttolerance.interceptors;

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

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceExecution;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.cdi.CdiFaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.cdi.CdiFaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils.Stereotypes;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;

@Interceptor
@FaultToleranceBehaviour
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class FaultToleranceInterceptor implements Stereotypes, Serializable {

    private static final Logger logger = Logger.getLogger(FaultToleranceInterceptor.class.getName());

    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        try {
            FaultTolerancePolicy policy = FaultTolerancePolicy.get(context, this::getConfig);
            if (policy.isFaultToleranceEnabled) {
                FaultToleranceExecution execution =
                        Globals.getDefaultBaseServiceLocator().getService(FaultToleranceExecution.class);
                FaultToleranceMetrics metrics = policy.isMetricsEnabled ? new CdiFaultToleranceMetrics(null) : null;
                return policy.processWithFaultTolerance(context, execution, metrics);
            }
        } catch (FaultToleranceDefinitionException e) {
            logger.log(Level.SEVERE, "Effective FT policy contains illegal values, fault tolerance cannot be applied,"
                    + " falling back to plain method invocation.", e);
            // fall-through to normal proceed
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

