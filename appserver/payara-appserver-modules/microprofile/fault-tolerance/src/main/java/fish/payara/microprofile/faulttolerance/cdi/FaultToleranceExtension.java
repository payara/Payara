/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017-2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.faulttolerance.cdi;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import javax.annotation.Priority;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.util.AnnotationLiteral;
import javax.interceptor.InvocationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * CDI Extension that does the setup for FT interceptor handling.
 * 
 * @author Andrew Pielage
 * @author Jan Bernitt (2.0)
 */
public class FaultToleranceExtension implements Extension {

    private static final String INTERCEPTOR_PRIORITY_PROPERTY = "mp.fault.tolerance.interceptor.priority";

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(FaultToleranceInterceptor.class), "MP-FT");
    }

    void enableInterceptor(@Observes AfterTypeDiscovery afterTypeDiscovery) {
        afterTypeDiscovery.getInterceptors().add(FaultToleranceInterceptor.class);
    }

    void installInterceptor(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        AnnotatedType<FaultToleranceInterceptor> at = afterBeanDiscovery.getAnnotatedType(FaultToleranceInterceptor.class, "MP-FT");

        ALL_BINDINGS.forEach(a -> afterBeanDiscovery.addBean(new ProgrammaticInterceptor(at, beanManager, a)));
    }

    /**
     * 
     * @param processAnnotatedType type currently processed
     */
    <T> void processAnnotatedType(@Observes @WithAnnotations({ Asynchronous.class, Bulkhead.class, CircuitBreaker.class,
        Fallback.class, Retry.class, Timeout.class }) ProcessAnnotatedType<T> processAnnotatedType) throws Exception {

        // TODO: To support alternative asynchronous annotations we need to add FT Asynchronous annotation.
        Class<? extends Annotation>[] alternativeAsynchronousAnnotations = getAlternativeAsynchronousAnnotations();
        AnnotatedType<T> type = processAnnotatedType.getAnnotatedType();
        boolean markAllMethods = FaultToleranceUtils.isAnnotatedWithFaultToleranceAnnotations(type)
                || isAnyAnnotationPresent(type, alternativeAsynchronousAnnotations);
        Class<?> targetClass = type.getJavaClass();
        for (AnnotatedMethodConfigurator<?> methodConfigurator : processAnnotatedType.configureAnnotatedType().methods()) {
            AnnotatedMethod<?> method = methodConfigurator.getAnnotated();
            if (markAllMethods || FaultToleranceUtils.isAnnotatedWithFaultToleranceAnnotations(method)
                    || isAnyAnnotationPresent(method, alternativeAsynchronousAnnotations)) {
                FaultTolerancePolicy.asAnnotated(targetClass, method.getJavaMember());
            }
        }
    }

    private static boolean isAnyAnnotationPresent(Annotated element, Class<? extends Annotation>[] annotationTypes) {
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (element.isAnnotationPresent(annotationType)) {
                return true;
            }
        }
        return false;
    }

    private static Class<? extends Annotation>[] getAlternativeAsynchronousAnnotations() {
        Optional<String> alternativeAsynchronousAnnotationNames = ConfigProvider.getConfig().getOptionalValue(
                FaultToleranceConfig.ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY, String.class);
        return alternativeAsynchronousAnnotationNames.isPresent()
                ? FaultToleranceUtils.toClassArray(alternativeAsynchronousAnnotationNames.get(),
                        FaultToleranceConfig.ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY,
                        FaultToleranceConfig.NO_ALTERNATIVE_ANNOTATIONS)
                : FaultToleranceConfig.NO_ALTERNATIVE_ANNOTATIONS;
    }

    void changeInterceptorPriority(@Observes ProcessAnnotatedType<FaultToleranceInterceptor> interceptorType) {
        Optional<Integer> priorityOverride = ConfigProvider.getConfig().getOptionalValue(INTERCEPTOR_PRIORITY_PROPERTY,
                Integer.class);
        if (priorityOverride.isPresent()) {
            interceptorType.configureAnnotatedType()
                    .remove(annotation -> annotation instanceof Priority)
                    .add(new PriorityLiteral(priorityOverride.get()));
        }
    }

    public static final class PriorityLiteral extends AnnotationLiteral<Priority> implements Priority {
        private static final long serialVersionUID = 1L;

        private final int value;

        public PriorityLiteral(int value) {
            this.value = value;
        }

        @Override
        public int value() {
            return value;
        }
    }

    static class ProgrammaticInterceptor implements Interceptor<FaultToleranceInterceptor> {

        private final BeanManager bm;
        private final Annotation binding;
        private final BeanAttributes<FaultToleranceInterceptor> beanAttributes;
        private final InjectionTarget<FaultToleranceInterceptor> injectionTarget;

        ProgrammaticInterceptor(AnnotatedType<FaultToleranceInterceptor> at, BeanManager bm, Annotation binding) {
            this.bm = bm;
            this.binding = binding;
            beanAttributes = bm.createBeanAttributes(at);
            injectionTarget = bm.createInjectionTarget(at);
        }

        @Override
        public Set<Annotation> getInterceptorBindings() {
            return Collections.singleton(binding);
        }

        @Override
        public boolean intercepts(InterceptionType type) {
            return type == InterceptionType.AROUND_INVOKE;
        }

        @Override
        public Object intercept(InterceptionType type, FaultToleranceInterceptor instance, InvocationContext ctx) throws Exception {
            return instance.intercept(ctx);
        }

        @Override
        public Class<?> getBeanClass() {
            return FaultToleranceInterceptor.class;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return injectionTarget.getInjectionPoints();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public FaultToleranceInterceptor create(CreationalContext<FaultToleranceInterceptor> creationalContext) {
            FaultToleranceInterceptor instance = injectionTarget.produce(creationalContext);
            injectionTarget.inject(instance, creationalContext);
            injectionTarget.postConstruct(instance);
            return instance;
        }

        @Override
        public void destroy(FaultToleranceInterceptor instance, CreationalContext<FaultToleranceInterceptor> creationalContext) {
            try {
                injectionTarget.preDestroy(instance);
                injectionTarget.dispose(instance);
            } finally {
                creationalContext.release();
            }
        }

        @Override
        public Set<Type> getTypes() {
            return beanAttributes.getTypes();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return beanAttributes.getQualifiers();
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return beanAttributes.getScope();
        }

        @Override
        public String getName() {
            return beanAttributes.getName();
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return beanAttributes.getStereotypes();
        }

        @Override
        public boolean isAlternative() {
            return beanAttributes.isAlternative();
        }
    }

    private static final Collection<Annotation> ALL_BINDINGS = Arrays.asList(
            new Asynchronous() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Asynchronous.class;
                }
            },
            new Bulkhead(){
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Bulkhead.class;
                }

                @Override
                public int value() {
                    return 0;
                }

                @Override
                public int waitingTaskQueue() {
                    return 0;
                }
            },
            new CircuitBreaker() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return CircuitBreaker.class;
                }

                @Override
                public Class<? extends Throwable>[] failOn() {
                    return new Class[0];
                }

                @Override
                public Class<? extends Throwable>[] skipOn() {
                    return new Class[0];
                }

                @Override
                public long delay() {
                    return 0;
                }

                @Override
                public ChronoUnit delayUnit() {
                    return null;
                }

                @Override
                public int requestVolumeThreshold() {
                    return 0;
                }

                @Override
                public double failureRatio() {
                    return 0;
                }

                @Override
                public int successThreshold() {
                    return 0;
                }
            },
            new Fallback() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Fallback.class;
                }

                @Override
                public Class<? extends FallbackHandler<?>> value() {
                    return null;
                }

                @Override
                public String fallbackMethod() {
                    return null;
                }

                @Override
                public Class<? extends Throwable>[] applyOn() {
                    return new Class[0];
                }

                @Override
                public Class<? extends Throwable>[] skipOn() {
                    return new Class[0];
                }
            },
            new Retry() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Retry.class;
                }

                @Override
                public int maxRetries() {
                    return 0;
                }

                @Override
                public long delay() {
                    return 0;
                }

                @Override
                public ChronoUnit delayUnit() {
                    return null;
                }

                @Override
                public long maxDuration() {
                    return 0;
                }

                @Override
                public ChronoUnit durationUnit() {
                    return null;
                }

                @Override
                public long jitter() {
                    return 0;
                }

                @Override
                public ChronoUnit jitterDelayUnit() {
                    return null;
                }

                @Override
                public Class<? extends Throwable>[] retryOn() {
                    return new Class[0];
                }

                @Override
                public Class<? extends Throwable>[] abortOn() {
                    return new Class[0];
                }
            },
            new Timeout() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Timeout.class;
                }

                @Override
                public long value() {
                    return 0;
                }

                @Override
                public ChronoUnit unit() {
                    return null;
                }
            }
    );
}
