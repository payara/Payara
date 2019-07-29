/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.cdi.extension.cluster;

import fish.payara.micro.cdi.extension.cluster.annotations.ClusterScoped;
import fish.payara.micro.cdi.extension.cluster.annotations.ClusterScopedIntercepted;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.util.AnnotationLiteral;

/**
 * Adds @ClusteredScoped annotation to the @Clusteded beans
 *
 * @author lprimak
 */
@SuppressWarnings("unchecked")
class ClusteredAnnotatedType<TT> implements AnnotatedType<TT> {
    private static final ApplicationScopedFilter appScopedFilter = new ApplicationScopedFilter();
    private static final ClusteredAnnotationLiteral clusteredScopedLiteral = new ClusteredAnnotationLiteral();
    private static final ClusteredInterceptorAnnotationLiteral clusteredScopedInterceptorLiteral = new ClusteredInterceptorAnnotationLiteral();

    private final AnnotatedType<TT> wrapped;

    public ClusteredAnnotatedType(AnnotatedType<TT> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        Set<Annotation> annotations = new HashSet(wrapped.getAnnotations());
        annotations.removeIf(appScopedFilter);
        annotations.add(clusteredScopedLiteral);
        annotations.add(clusteredScopedInterceptorLiteral);
        return annotations;
    }
    @Override
    public Type getBaseType() {
        return wrapped.getBaseType();
    }
    @Override
    public Set<Type> getTypeClosure() {
        return wrapped.getTypeClosure();
    }
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return wrapped.getAnnotation(annotationType);
    }
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return wrapped.isAnnotationPresent(annotationType);
    }
    @Override
    public Class<TT> getJavaClass() {
        return wrapped.getJavaClass();
    }
    @Override
    public Set<AnnotatedConstructor<TT>> getConstructors() {
        return wrapped.getConstructors();
    }
    @Override
    public Set<AnnotatedMethod<? super TT>> getMethods() {
        return wrapped.getMethods();
    }
    @Override
    public Set<AnnotatedField<? super TT>> getFields() {
        return wrapped.getFields();
    }

    @SuppressWarnings("serial")
    private static class ClusteredAnnotationLiteral extends AnnotationLiteral<ClusterScoped> implements ClusterScoped {}
    @SuppressWarnings("serial")
    private static class ClusteredInterceptorAnnotationLiteral extends AnnotationLiteral<ClusterScopedIntercepted> implements ClusterScopedIntercepted {}
    private static class ApplicationScopedFilter implements Predicate<Annotation> {
        @Override
        public boolean test(Annotation input) {
            return input.annotationType().equals(ApplicationScoped.class);
        }
    }

}
