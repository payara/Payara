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

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.interceptor.AroundInvoke;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Set;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Gaurav Gupta
 */
public class WrappingInjectionTarget<T> implements InjectionTarget<T> {

    private final InjectionTarget<T> delegate;
    private final DevConsoleRegistry registry;
    private final Class<T> beanClass;
    private final BeanManager beanManager;
    private static final ThreadLocal<Long> creationStart = new ThreadLocal<>();

    private static final DevConsoleService consoleService = Globals
            .getDefaultBaseServiceLocator()
            .getService(DevConsoleService.class);

    WrappingInjectionTarget(Class<T> beanClass,
            InjectionTarget<T> delegate,
            DevConsoleRegistry registry,
            BeanManager beanManager) {
        this.beanClass = beanClass;
        this.delegate = delegate;
        this.registry = registry;
        this.beanManager = beanManager;
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        if (consoleService.isEnabled()) {
            creationStart.set(System.nanoTime());
        }
        T instance = delegate.produce(ctx);

        if (beanClass.isAnnotationPresent(jakarta.interceptor.Interceptor.class)) {
//            return wrapInterceptor(instance);
        } else if (beanClass.isAnnotationPresent(jakarta.decorator.Decorator.class)) {
            recordCreation();
//            return wrapDecorator(instance);
        }

        return instance;
    }

    @Override
    public void postConstruct(T instance) {
        delegate.postConstruct(instance);

        if (!beanClass.isAnnotationPresent(jakarta.decorator.Decorator.class)) {
            recordCreation();
        }
    }

    private void recordCreation() {
        Long start = creationStart.get();
        if (start != null) {
            long end = System.nanoTime();
            long ms = (end - start) / 1_000_000;
            registry.recordCreation(beanClass, ms);
            creationStart.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private T wrapDecorator(T instance) {

        try {
            MethodHandles.Lookup lookup
                    = MethodHandles.privateLookupIn(
                            beanClass,
                            MethodHandles.lookup()
                    );

            DynamicType.Unloaded<? extends T> unloaded
                    = new ByteBuddy()
                            .subclass(beanClass)
                            .method(ElementMatchers.isDeclaredBy(beanClass))
                            .intercept(
                                    Advice.to(DecoratorAdvice.class)
                                            .wrap(SuperMethodCall.INSTANCE)
                            )
                            .defineField("registry", DevConsoleRegistry.class, Visibility.PUBLIC)
                            .defineField("beanClass", Class.class, Visibility.PUBLIC)
                            .make();

            DynamicType.Loaded<? extends T> loaded
                    = unloaded.load(
                            beanClass.getClassLoader(),
                            ClassLoadingStrategy.UsingLookup.of(lookup)
                    );

            Class<? extends T> proxyClass = loaded.getLoaded();

            T proxy = proxyClass.getDeclaredConstructor().newInstance();

            proxyClass.getDeclaredField("registry").set(proxy, registry);
            proxyClass.getDeclaredField("beanClass").set(proxy, beanClass);

            return proxy;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Decorator proxy failed for " + beanClass.getName(), e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private T wrapInterceptor(T instance) {

        try {
            // Java 9+ compliant lookup for class definition
            MethodHandles.Lookup lookup
                    = MethodHandles.privateLookupIn(
                            beanClass,
                            MethodHandles.lookup()
                    );

            DynamicType.Unloaded<? extends T> unloaded
                    = new ByteBuddy()
                            .subclass(beanClass)
                            // Intercept only @AroundInvoke methods
                            .method(ElementMatchers.isAnnotatedWith(AroundInvoke.class))
                            .intercept(
                                    Advice.to(AroundInvokeAdvice.class)
                                            .wrap(SuperMethodCall.INSTANCE)
                            )
                            // Preserve interfaces
                            .implement(beanClass.getInterfaces())
                            .defineField("registry", DevConsoleRegistry.class)
                            .defineField("beanClass", Class.class)
                            .make();

            DynamicType.Loaded<? extends T> loaded
                    = unloaded.load(
                            beanClass.getClassLoader(),
                            ClassLoadingStrategy.UsingLookup.of(lookup)
                    );

            Class<? extends T> proxyClass = loaded.getLoaded();

            T proxy = proxyClass.getDeclaredConstructor().newInstance();

            Field registryField = proxyClass.getDeclaredField("registry");
            registryField.setAccessible(true);
            registryField.set(proxy, registry);

            Field beanClassField = proxyClass.getDeclaredField("beanClass");
            beanClassField.setAccessible(true);
            beanClassField.set(proxy, beanClass);

            return proxy;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create interceptor proxy for " + beanClass.getName(), e
            );
        }
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {
        delegate.inject(instance, ctx);
    }

    @Override
    public void preDestroy(T instance) {
        Long start = System.nanoTime();
        delegate.preDestroy(instance);
        if (consoleService.isEnabled()) {
            long end = System.nanoTime();
            long ms = (end - start) / 1_000_000;
            registry.recordDestruction(beanClass, ms);
        }
    }

    @Override
    public void dispose(T instance) {
        delegate.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return delegate.getInjectionPoints();
    }
}
