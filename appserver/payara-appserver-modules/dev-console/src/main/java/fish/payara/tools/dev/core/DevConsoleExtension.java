/*
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.tools.dev.core;

import fish.payara.tools.dev.admin.DevConsoleApplication;
import fish.payara.tools.dev.admin.DevConsoleServiceUtil;
import fish.payara.tools.dev.model.InjectionPointInfo;
import fish.payara.tools.dev.model.ResolutionStatus;
import fish.payara.tools.dev.model.ProducerInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Gaurav Gupta
 */
public class DevConsoleExtension implements Extension {

    private final DevConsoleRegistry registry = new DevConsoleRegistry();

    private static final DevConsoleService consoleService = Globals
            .getDefaultBaseServiceLocator()
            .getService(DevConsoleService.class);

    private String appName;

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        this.appName = DevConsoleServiceUtil.resolveAppName(cl);
    }

    <T, X> void onProcessProducer(@Observes ProcessProducer<T, X> pp, BeanManager bm) {

        AnnotatedMember<T> producerMember = pp.getAnnotatedMember();

        ProducerInfo info = new ProducerInfo(
                producerMember,
                producerMember.getBaseType(),
                (producerMember instanceof AnnotatedField) ? ProducerInfo.Kind.FIELD : ProducerInfo.Kind.METHOD,
                bm
        );

        registry.addProducer(info);

        Producer<X> delegate = pp.getProducer();

        pp.setProducer(new Producer<X>() {

            @Override
            public X produce(CreationalContext<X> ctx) {
                info.incrementProducedCount();
                return delegate.produce(ctx);
            }

            @Override
            public void dispose(X instance) {
                info.incrementDisposedCount();
                delegate.dispose(instance);
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return delegate.getInjectionPoints();
            }
        });
    }

    <T> void onProcessAnnotatedType(@Observes ProcessAnnotatedType<T> pat, BeanManager beanManager) {
        if (pat.getAnnotatedType().isAnnotationPresent(jakarta.decorator.Decorator.class)) {
            registry.addDecorator(pat.getAnnotatedType());
        }
        // Check for security annotations on REST resources
        if (pat.getAnnotatedType().isAnnotationPresent(jakarta.annotation.security.RolesAllowed.class)
                || pat.getAnnotatedType().isAnnotationPresent(jakarta.annotation.security.PermitAll.class)
                || pat.getAnnotatedType().isAnnotationPresent(jakarta.annotation.security.DenyAll.class)) {
            registry.addSecurityAnnotation(pat.getAnnotatedType());
        }

        pat.getAnnotatedType().getMethods().forEach(m -> {
            if (m.isAnnotationPresent(jakarta.annotation.security.RolesAllowed.class)
                    || m.isAnnotationPresent(jakarta.annotation.security.PermitAll.class)
                    || m.isAnnotationPresent(jakarta.annotation.security.DenyAll.class)) {
                registry.addSecurityAnnotation(m);
            }
        });

        registry.seenType(pat.getAnnotatedType());

        AnnotatedType<T> at = pat.getAnnotatedType();
        if (ExceptionMapper.class.isAssignableFrom(at.getJavaClass())) {
            System.out.println("Found REST exception-mapper: " + at.getJavaClass().getName());
            Class<? extends Throwable> exceptionType = getExceptionType(at.getJavaClass());
            System.out.printf("Found REST exception-mapper: %s (for %s)%n",
                    at.getJavaClass().getName(), exceptionType.getName());
            registry.addRestExceptionMapper(at, exceptionType);
        }

        final String classLevelPath;
        if (at.isAnnotationPresent(jakarta.ws.rs.Path.class)) {
            System.out.println("Found REST resource: " + at.getJavaClass().getName());
            jakarta.ws.rs.Path pathAnnotation = at.getAnnotation(jakarta.ws.rs.Path.class);
            if (pathAnnotation != null) {
                classLevelPath = pathAnnotation.value();
                registry.addRestResourcePath(at, classLevelPath);
            } else {
                classLevelPath = null;
            }
        } else {
            classLevelPath = null;
        }

        at.getMethods().forEach(m -> {
            if (m.isAnnotationPresent(jakarta.ws.rs.GET.class)
                    || m.isAnnotationPresent(jakarta.ws.rs.POST.class)
                    || m.isAnnotationPresent(jakarta.ws.rs.PUT.class)
                    || m.isAnnotationPresent(jakarta.ws.rs.DELETE.class)
                    || m.isAnnotationPresent(jakarta.ws.rs.OPTIONS.class)
                    || m.isAnnotationPresent(jakarta.ws.rs.PATCH.class)
                    || m.isAnnotationPresent(jakarta.ws.rs.HEAD.class)) {
                System.out.println(" u007F REST endpoint method: " + m.getJavaMember());

                // Store the rest method path if present
                jakarta.ws.rs.Path methodPath = m.getAnnotation(jakarta.ws.rs.Path.class);
                String combinedPath = null;
                if (classLevelPath != null) {
                    if (methodPath != null) {
                        combinedPath = classLevelPath + (methodPath.value().startsWith("/") ? "" : "/") + methodPath.value();
                    } else {
                        combinedPath = classLevelPath;
                    }
                } else if (methodPath != null) {
                    combinedPath = methodPath.value();
                }

                // Retrieve produces media type if present
                jakarta.ws.rs.Produces producesAnnotation = m.getAnnotation(jakarta.ws.rs.Produces.class);
                String produces = producesAnnotation != null ? String.join(",", producesAnnotation.value()) : null;

                String httpMethod = null;
                if (m.isAnnotationPresent(jakarta.ws.rs.GET.class)) {
                    httpMethod = "GET";
                } else if (m.isAnnotationPresent(jakarta.ws.rs.POST.class)) {
                    httpMethod = "POST";
                } else if (m.isAnnotationPresent(jakarta.ws.rs.PUT.class)) {
                    httpMethod = "PUT";
                } else if (m.isAnnotationPresent(jakarta.ws.rs.DELETE.class)) {
                    httpMethod = "DELETE";
                } else if (m.isAnnotationPresent(jakarta.ws.rs.OPTIONS.class)) {
                    httpMethod = "OPTIONS";
                } else if (m.isAnnotationPresent(jakarta.ws.rs.PATCH.class)) {
                    httpMethod = "PATCH";
                } else if (m.isAnnotationPresent(jakarta.ws.rs.HEAD.class)) {
                    httpMethod = "HEAD";
                }
                registry.addRestMethodPathWithProduces(m, combinedPath, produces, httpMethod);
            }
        });
    }

    void recordInterceptorsChain(
            @Observes AfterDeploymentValidation adv,
            BeanManager bm) {

        for (Bean<?> bean : bm.getBeans(Object.class, new AnnotationLiteral<Any>() {
        })) {

            Class<?> beanClass = bean.getBeanClass();

            // 1. Collect class-level interceptor bindings
            List<Annotation> classBindings = new ArrayList<>();
            for (Annotation a : beanClass.getAnnotations()) {
                if (bm.isInterceptorBinding(a.annotationType())) {
                    classBindings.add(a);
                }
            }

            // 2. Process each method separately
            for (Method method : beanClass.getDeclaredMethods()) {

                // Skip synthetic / bridge methods
                if (method.isSynthetic() || method.isBridge()) {
                    continue;
                }

                // Collect method-level bindings
                List<Annotation> methodBindings = new ArrayList<>();
                for (Annotation a : method.getAnnotations()) {
                    if (bm.isInterceptorBinding(a.annotationType())) {
                        methodBindings.add(a);
                    }
                }

                // Combine class + method
                List<Annotation> allBindings = new ArrayList<>(classBindings);
                allBindings.addAll(methodBindings);

                if (allBindings.isEmpty()) {
                    registry.getInterceptorChains().put(
                            beanClass.getName() + "#" + method.getName(),
                            List.of()
                    );
                    continue;
                }

                // Safe during AfterDeploymentValidation
                List<Interceptor<?>> resolved = bm.resolveInterceptors(
                        InterceptionType.AROUND_INVOKE,
                        allBindings.toArray(new Annotation[0])
                );

                registry.getInterceptorChains().put(
                        beanClass.getName() + "#" + method.getName(),
                        resolved.stream().map(Interceptor::getBeanClass).toList()
                );
            }
        }
    }

    public List<Class<?>> getChainFor(Class<?> beanClass) {
        return registry
                .getInterceptorChains()
                .getOrDefault(beanClass, List.of());
    }

    public static Class<? extends Throwable> getExceptionType(Class<?> mapperClass) {
        for (Type type : mapperClass.getGenericInterfaces()) {
            if (type instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType() == ExceptionMapper.class) {
                    Type arg = parameterizedType.getActualTypeArguments()[0];
                    if (arg instanceof Class<?>) {
                        return (Class<? extends Throwable>) arg;
                    }
                }
            }
        }
        // fallback if not found (e.g. if it implements indirectly)
        return Throwable.class;
    }

    <T> void onProcessBeanAttributes(@Observes ProcessBeanAttributes<T> pba) {
        if (pba.getAnnotated().isAnnotationPresent(jakarta.interceptor.Interceptor.class)) {
            registry.addInterceptor(pba.getAnnotated());
        }
    }

    <T> void onProcessBean(@Observes ProcessBean<T> pb) {
        if (pb.getAnnotated().isAnnotationPresent(jakarta.decorator.Decorator.class)) {

            // CDI guarantees exactly one delegate injection point per decorator
            for (InjectionPoint ip : pb.getBean().getInjectionPoints()) {
                if (ip.isDelegate()) {
                    Type decoratedType = ip.getType();

                    registry.recordDecoratorForBean(
                            ((Class<?>) decoratedType),
                            pb.getBean().getBeanClass()
                    );
                }
            }
        }
        registry.registerBean(pb.getBean());
    }

    <T, X> void onProcessObserver(@Observes ProcessObserverMethod<T, X> pom) {
        registry.registerObserver(pom.getObserverMethod(), pom.getAnnotatedMethod());
    }

    <T> void onProcessInjectionTarget(@Observes ProcessInjectionTarget<T> pit, BeanManager bm) {
        if (consoleService == null || !consoleService.isEnabled()) {
            return;
        }

        Class<T> beanClass = pit.getAnnotatedType().getJavaClass();  // ✔ legal here

        InjectionTarget<T> original = pit.getInjectionTarget();

        pit.setInjectionTarget(new WrappingInjectionTarget<>(beanClass, original, registry, bm));
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        if (consoleService == null || !consoleService.isEnabled()) {
            return;
        }
        EventLogger eventLogger = new EventLogger();
        eventLogger.setRegistry(registry);
        abd.addBean()
                .addType(EventLogger.class)
                .scope(ApplicationScoped.class)
                .produceWith(ctx -> eventLogger);
        abd.addBean(new RegistryBean(registry));

        DevConsoleApplication app = DevConsoleServiceUtil.resolveApplication();
        consoleService.register(app, registry);
    }

    void beforeShutdown(@Observes BeforeShutdown bs) {
//        consoleService.unregister(appName);
    }

    
    private Set<String> injectionPointSeen = new HashSet<>();
    <T, X> void onProcessInjectionPoint(
            @Observes ProcessInjectionPoint<T, X> pip,
            BeanManager bm) {
        if (consoleService == null || !consoleService.isEnabled()) {
            return;
        }
        System.out.println("onProcessInjectionPoint");
        for (Bean<?> bean : registry.getBeans()) {
            for (InjectionPoint ip : bean.getInjectionPoints()) {

                String key
                        = ip.getMember().getDeclaringClass().getName() + "#"
                        + ip.getMember().getName() + ":"
                        + ip.getType().getTypeName();

                if (injectionPointSeen.add(key)) {
                    Set<Bean<?>> candidates
                            = bm.getBeans(ip.getType(), ip.getQualifiers().toArray(new Annotation[0]));

                    InjectionPointInfo info = InjectionPointInfo.from(bean, ip);

                    if (candidates.isEmpty()) {
                        info.setResolutionStatus(ResolutionStatus.UNSATISFIED);
                        info.setFailureReason(hintUnsatisfied(ip, bm));
                    } else {
                        try {
                            Bean<?> resolved = bm.resolve(candidates);
                            info.setResolutionStatus(ResolutionStatus.RESOLVED);
                            info.setCandidateBeans(
                                    List.of(resolved.getBeanClass().getName())
                            );
                        } catch (AmbiguousResolutionException e) {
                            info.setResolutionStatus(ResolutionStatus.AMBIGUOUS);
                            info.setCandidateBeans(
                                    candidates.stream()
                                            .map(b -> b.getBeanClass().getName())
                                            .toList()
                            );
                        }
                    }

                    registry.addInjectionPointInfo(info);
                }
            }
        }
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager bm) {
        if (consoleService == null || !consoleService.isEnabled()) {
            return;
        }

        recordInterceptorsChain(adv, bm);
        registry.finishModel(bm);
    }

    private String hintUnsatisfied(InjectionPoint ip, BeanManager bm) {

        if (bm.getBeans(ip.getType()).isEmpty()) {
            return "No beans with matching type found";
        }

        if (!ip.getQualifiers().isEmpty()) {
            return "Qualifier mismatch: " + ip.getQualifiers();
        }

        return "Bean may be excluded (missing beans.xml or discovery mode)";
    }

    private boolean isFromCurrentWar(Class<?> clazz) {
        ClassLoader warLoader = Thread.currentThread().getContextClassLoader();
        return clazz.getClassLoader() == warLoader;
    }

}
