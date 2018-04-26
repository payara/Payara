/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2018] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.google.common.base.Optional;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.util.DOLUtils;
import fish.payara.cluster.Clustered;
import fish.payara.micro.cdi.extension.cluster.annotations.ClusterScoped;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import org.glassfish.internal.deployment.Deployment;

/**
 * @Clustered singleton CDI context implementation
 *
 * @author lprimak
 */
class ClusterScopeContext implements Context {
    private final BeanManager beanManager;
    private final ClusteredSingletonLookupImpl clusteredLookup;


    public ClusterScopeContext(BeanManager beanManager, Deployment deployment) {
        this.beanManager = beanManager;
        Application app = deployment.getCurrentDeploymentContext().getModuleMetaData(Application.class);
        clusteredLookup = new ClusteredSingletonLookupImpl(beanManager, DOLUtils.getApplicationName(app));
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ClusterScoped.class;
    }

    @Override
    public <TT> TT get(Contextual<TT> contextual, CreationalContext<TT> creationalContext) {
        TT beanInstance = get(contextual);
        if (beanInstance == null) {
            beanInstance = getFromApplicationScoped(contextual, Optional.of(creationalContext));
            final Bean<TT> bean = (Bean<TT>) contextual;
            if (clusteredLookup.getClusteredSingletonMap()
                    .putIfAbsent(getBeanName(bean, getAnnotation(beanManager, bean)), beanInstance) != null) {
                bean.destroy(beanInstance, creationalContext);
                beanInstance = get(contextual);
            }
        }
        return beanInstance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <TT> TT get(Contextual<TT> contextual) {
        final Bean<TT> bean = (Bean<TT>) contextual;
        Clustered clusteredAnnotation = getAnnotation(beanManager, bean);
        String beanName = getBeanName(bean, clusteredAnnotation);
        TT beanInstance = (TT)clusteredLookup.getClusteredSingletonMap().get(beanName);
        if (clusteredAnnotation.callPostConstructOnAttach() && beanInstance != null &&
                getFromApplicationScoped(contextual, Optional.<CreationalContext<TT>>absent()) == null) {
            beanManager.getContext(ApplicationScoped.class).get(contextual, beanManager.createCreationalContext(contextual));
        }
        return beanInstance;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private<TT> TT getFromApplicationScoped(Contextual<TT> contextual, Optional<CreationalContext<TT>> creationalContext) {
        if (creationalContext.isPresent()) {
            return beanManager.getContext(ApplicationScoped.class).get(contextual, creationalContext.get());
        }
        else {
            return beanManager.getContext(ApplicationScoped.class).get(contextual);
        }
    }
    
    static <TT> String getBeanName(Bean<TT> bean, Clustered annotation) {
        return annotation.keyName().isEmpty()? bean.getName() : annotation.keyName();
    }

    static <TT> Clustered getAnnotation(BeanManager beanManager, Bean<TT> bean) {
        return getAnnotation(beanManager, bean.getBeanClass());
    }

    /**
     * Copied from Soteria's CdiUtils.getAnnotation() because it is JDK 8 only
     * This version works with JDK 7 and is specific to Clustered Singleton
     * Do NOT propagate this code into JDK 8-compliant code,
     * use CdiUtils.getAnnotation() from Soteria instead
     *
     * @param <TT>
     * @param beanManager
     * @param annotated
     * @return annotation type
     */
    static <TT> Clustered getAnnotation(BeanManager beanManager, Class<?> annotated) {

        annotated.getAnnotation(Clustered.class);

        if (annotated.getAnnotations().length == 0) {
            throw new IllegalStateException("No Clustered Annotation Present");
        }

        if (annotated.isAnnotationPresent(Clustered.class)) {
            return annotated.getAnnotation(Clustered.class);
        }

        Queue<Annotation> annotations = new LinkedList<>(Arrays.asList(annotated.getAnnotations()));

        while (!annotations.isEmpty()) {
            Annotation annotation = annotations.remove();

            if (annotation.annotationType().equals(Clustered.class)) {
                return Clustered.class.cast(annotation);
            }

            if (beanManager.isStereotype(annotation.annotationType())) {
                annotations.addAll(
                    beanManager.getStereotypeDefinition(
                        annotation.annotationType()
                    )
                );
            }
        }

        throw new IllegalStateException("No Clustered Annotation Present");
    }
}
