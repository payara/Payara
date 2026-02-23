/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023-2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */
package fish.payara.microprofile.telemetry.tracing;

import fish.payara.opentracing.OpenTelemetryService;
import fish.payara.telemetry.service.PayaraTelemetryBootstrapFactoryServiceImpl;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.Nonbinding;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.internal.api.Globals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Extension class that adds a Producer for OpenTracing Tracers, allowing injection.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class TelemetryCdiExtension implements Extension {
    private final OpenTelemetryService openTelemetryService;
    
    private final PayaraTelemetryBootstrapFactoryServiceImpl payaraTelemetryBootstrapFactoryServiceImpl;

    private boolean appManagedOtel;

    public TelemetryCdiExtension() {
        openTelemetryService = Globals.getDefaultBaseServiceLocator().getService(OpenTelemetryService.class);
        payaraTelemetryBootstrapFactoryServiceImpl = Globals.getDefaultBaseServiceLocator().getService(PayaraTelemetryBootstrapFactoryServiceImpl.class);
    }

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        addAnnotatedType(bbd, bm, OpenTracingTracerProducer.class);
        addAnnotatedType(bbd, bm, OpenTelemetryTracerProducer.class);
        bbd.addInterceptorBinding(new WithSpanAnnotatedType(bm.createAnnotatedType(WithSpan.class)));
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        abd.addBean(new WithSpanMethodInterceptorBean(bm));
    }

    static void addAnnotatedType(BeforeBeanDiscovery bbd, BeanManager bm, Class<?> beanClass) {
        var at = bm.createAnnotatedType(beanClass);
        bbd.addAnnotatedType(at, beanClass.getName());
    }

    void initializeOpenTelemetry(@Observes AfterDeploymentValidation adv) {
        try {
            //verify if global config is available
            if (payaraTelemetryBootstrapFactoryServiceImpl.getAvailableRuntimeReference().isEmpty()) {
                var config = ConfigProvider.getConfig();
                config.getConfigSources().forEach(g -> System.out.println(g.getName()+ " -> values" +g.getProperties()));
                if (config.getOptionalValue("otel.sdk.disabled", Boolean.class).orElse(true)) {
                    // app-specific OpenTelemetry is not configured
                    return;
                }
                // This is potentially expensive, but Autoconfigure SDK does not have lazy per-key provider, needs a map
                var otelProps = StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                        .filter(key -> key.startsWith("otel."))
                        .collect(Collectors.toMap(k -> k, k -> config.getValue(k, String.class)));
                appManagedOtel = true;
                openTelemetryService.initializeCurrentApplication(otelProps);
            }
        } catch (Exception e) {
            adv.addDeploymentProblem(e);
        }
    }

    void shutdownAppScopedTelemetry(@Observes BeforeShutdown beforeShutdown) {
        if (appManagedOtel) {
            // application may have installed CDI-based components, like the TCK does
            // these need to be shutdown while application scope exists
            openTelemetryService.shutdownCurrentApplication();
        }
    }

    /**
     * This class is an implementation of the AnnotatedType interface for the WithSpan annotation.
     * It wraps an existing AnnotatedType object and overrides its getMethods() method to return a Set
     * of AnnotatedMethod objects with the Nonbinding annotation added.
     *
     */
    static class WithSpanAnnotatedType implements AnnotatedType<WithSpan> {
        private final AnnotatedType<WithSpan> delegate;
        private final Set<AnnotatedMethod<? super WithSpan>> methods;

        /**
         * Constructs a new WithSpanAnnotatedType object with the specified delegate AnnotatedType.
         * The constructor creates a new set of AnnotatedMethod objects that are copies of the methods
         * in the delegate AnnotatedType, with the Nonbinding annotation added.
         * @param delegate the delegate AnnotatedType to wrap
         */
        WithSpanAnnotatedType(final AnnotatedType<WithSpan> delegate) {
            this.delegate = delegate;
            this.methods = new HashSet<>();

            for (AnnotatedMethod<? super WithSpan> method : delegate.getMethods()) {
                methods.add(new AnnotatedMethod<WithSpan>() {
                    private final AnnotatedMethod<WithSpan> delegate = (AnnotatedMethod<WithSpan>) method;
                    private final Set<Annotation> annotations = Collections.singleton(Nonbinding.Literal.INSTANCE);

                    @Override
                    public Method getJavaMember() {
                        return delegate.getJavaMember();
                    }

                    @Override
                    public List<AnnotatedParameter<WithSpan>> getParameters() {
                        return delegate.getParameters();
                    }

                    @Override
                    public boolean isStatic() {
                        return delegate.isStatic();
                    }

                    @Override
                    public AnnotatedType<WithSpan> getDeclaringType() {
                        return delegate.getDeclaringType();
                    }

                    @Override
                    public Type getBaseType() {
                        return delegate.getBaseType();
                    }

                    @Override
                    public Set<Type> getTypeClosure() {
                        return delegate.getTypeClosure();
                    }

                    @Override
                    public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
                        if (annotationType.equals(Nonbinding.class)) {
                            return (T) annotations.iterator().next();
                        }
                        return null;
                    }

                    @Override
                    public Set<Annotation> getAnnotations() {
                        return annotations;
                    }

                    @Override
                    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
                        return annotationType.equals(Nonbinding.class);
                    }
                });
            }
        }

        @Override
        public Class<WithSpan> getJavaClass() {
            return delegate.getJavaClass();
        }

        @Override
        public Set<AnnotatedConstructor<WithSpan>> getConstructors() {
            return delegate.getConstructors();
        }

        @Override
        public Set<AnnotatedMethod<? super WithSpan>> getMethods() {
            return this.methods;
        }

        @Override
        public Set<AnnotatedField<? super WithSpan>> getFields() {
            return delegate.getFields();
        }

        @Override
        public Type getBaseType() {
            return delegate.getBaseType();
        }

        @Override
        public Set<Type> getTypeClosure() {
            return delegate.getTypeClosure();
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
            return delegate.getAnnotation(annotationType);
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return delegate.getAnnotations();
        }

        @Override
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
            return delegate.isAnnotationPresent(annotationType);
        }
    }
}
