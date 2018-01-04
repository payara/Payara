package fish.payara.microprofile.metrics.cdi.interceptor;

import fish.payara.microprofile.metrics.MetricHelper;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import fish.payara.microprofile.metrics.cdi.MetricResolver;
import fish.payara.microprofile.metrics.cdi.MetricsBinding;
import fish.payara.microprofile.metrics.impl.GaugeImpl;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;

@MetricsBinding
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class MetricsInterceptor {

    @Inject
    public MetricRegistry registry;

    @Inject
    private MetricResolver resolver;
    
    @Inject
    @Intercepted
    protected Bean<?> bean;

    @AroundConstruct
    private Object constructorInvocation(InvocationContext context) throws Exception {
        Object target;
        if (MetricHelper.isMetricEnabled()) {
            Class<?> beanClass = bean.getBeanClass();
            registerMetrics(beanClass, context.getConstructor(), context.getTarget());

            target = context.proceed();

            Class<?> type = beanClass;
            do {
                for (Method method : type.getDeclaredMethods()) {
                    if (!method.isSynthetic() && !Modifier.isPrivate(method.getModifiers())) {
                        registerMetrics(beanClass, method, context.getTarget());
                    }
                }
                type = type.getSuperclass();
            } while (!Object.class.equals(type));
        } else {
            target = context.proceed();
        }
        return target;
    }

    private <E extends Member & AnnotatedElement> void registerMetrics(Class<?> bean, E element, Object target) {
        MetricResolver.Of<Counted> counted = resolver.counted(bean, element);
        if (counted.isPresent()) {
            registry.counter(counted.metadata());
        }

        MetricResolver.Of<Metered> metered = resolver.metered(bean, element);
        if (metered.isPresent()) {
            registry.meter(metered.metadata());
        }

        MetricResolver.Of<Timed> timed = resolver.timed(bean, element);
        if (timed.isPresent()) {
            registry.timer(timed.metadata());
        }
        
        if (element instanceof Method 
                && element.isAnnotationPresent(org.eclipse.microprofile.metrics.annotation.Gauge.class)) {
            MetricResolver.Of<Gauge> gauge = resolver.gauge(bean, (Method)element);
            if (gauge.isPresent()) {
                registry.register(gauge.metricName(), new GaugeImpl((Method)element, target), gauge.metadata());
            }
        }
    }

}
