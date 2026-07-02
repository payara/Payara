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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.inject.spi.Prioritized;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.interceptor.InvocationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

/**
 * This class implements the CDI Interceptor interface for the {@link WithSpan} annotation.
 * It intercepts method invocations and invokes the {@link WithSpanMethodInterceptor} on them.
 */
public class WithSpanMethodInterceptorBean implements Interceptor<WithSpanMethodInterceptor>, Prioritized {

    private final BeanManager bm;

    /**
     * Constructs a new instance of this class with the given {@link BeanManager}.
     *
     * @param bm the {@link BeanManager} to use
     */
    public WithSpanMethodInterceptorBean(final BeanManager bm) {
        this.bm = bm;
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return Collections.singleton(WithSpanLiteral.INSTANCE);
    }

    @Override
    public boolean intercepts(InterceptionType type) {
        return InterceptionType.AROUND_INVOKE.equals(type);
    }

    @Override
    public Object intercept(InterceptionType type, WithSpanMethodInterceptor instance, InvocationContext ctx) throws Exception {
        return instance.withSpanCdiCall(ctx);
    }

    @Override
    public Class<?> getBeanClass() {
        return WithSpanMethodInterceptorBean.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public WithSpanMethodInterceptor create(CreationalContext<WithSpanMethodInterceptor> creationalContext) {
        return new WithSpanMethodInterceptor(bm);
    }

    @Override
    public void destroy(WithSpanMethodInterceptor instance, CreationalContext<WithSpanMethodInterceptor> creationalContext) {
        // no-op
    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(this.getBeanClass());
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.emptySet();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return getBeanClass().getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public int getPriority() {
        return jakarta.interceptor.Interceptor.Priority.LIBRARY_BEFORE;
    }

    /**
     * This class provides a literal implementation of the {@link WithSpan} annotation
     * for use with the CDI interceptor binding.
     */
    public static class WithSpanLiteral extends AnnotationLiteral<WithSpan> implements WithSpan {
        public static final WithSpanLiteral INSTANCE = new WithSpanLiteral();

        @Override
        public String value() {
            return null;
        }

        @Override
        public SpanKind kind() {
            return null;
        }

        @Override
        public boolean inheritContext() {
            return false;
        }
    }
}
