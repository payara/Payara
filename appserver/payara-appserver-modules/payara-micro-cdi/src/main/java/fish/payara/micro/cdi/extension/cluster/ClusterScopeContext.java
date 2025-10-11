/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import java.lang.annotation.Annotation;

import com.hazelcast.cp.exception.CPSubsystemException;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.cp.lock.exception.LockOwnershipLostException;
import fish.payara.cluster.DistributedLockType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.util.DOLUtils;

import jakarta.interceptor.InvocationContext;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.soteria.cdi.CdiUtils;

import fish.payara.cluster.Clustered;
import fish.payara.micro.cdi.extension.cluster.annotations.ClusterScoped;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @Clustered singleton CDI context implementation
 *
 * @author lprimak
 */
class ClusterScopeContext implements Context {
    private static final Logger log = Logger.getLogger(ClusterScopeContext.class.getName());
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
        final Bean<TT> bean = (Bean<TT>) contextual;
        Clustered clusteredAnnotation = getAnnotation(beanManager, bean);
        clusteredLookup.setClusteredSessionKeyIfNotSet(bean.getBeanClass(), clusteredAnnotation);
        boolean locked = lock(clusteredAnnotation, clusteredLookup.getDistributedLock());
        TT beanInstance = null;
        try {
            beanInstance = get(contextual);
            if (beanInstance == null) {
                beanInstance = getFromApplicationScoped(contextual, Optional.of(creationalContext));
                if (clusteredLookup.getClusteredSingletonMap()
                        .putIfAbsent(getBeanName(bean, clusteredAnnotation), beanInstance) != null) {
                    bean.destroy(beanInstance, creationalContext);
                    beanInstance = get(contextual);
                }
            }
        } finally {
            /**
             * If we couldn't find a bean instance we won't have unlocked in {@link ClusterScopedInterceptor#refreshAndUnlock(InvocationContext)},
             * so unlock here as a fallback
             */
            if (locked && beanInstance == null) {
                unlock(clusteredAnnotation, clusteredLookup.getDistributedLock());
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
        clusteredLookup.setClusteredSessionKeyIfNotSet(bean.getBeanClass(), clusteredAnnotation);
        boolean locked = lock(clusteredAnnotation, clusteredLookup.getDistributedLock());
        TT beanInstance = null;
        try {
            beanInstance = (TT) clusteredLookup.getClusteredSingletonMap().get(beanName);
            if (clusteredAnnotation.callPostConstructOnAttach() && beanInstance != null &&
                    getFromApplicationScoped(contextual, Optional.empty()) == null) {
                beanManager.getContext(ApplicationScoped.class).get(contextual, beanManager.createCreationalContext(contextual));
            }
        } finally {
            /**
             * If we couldn't find a bean instance we won't have unlocked in {@link ClusterScopedInterceptor#refreshAndUnlock(InvocationContext)},
             * so unlock here as a fallback
             */
            if (locked && beanInstance == null) {
                unlock(clusteredAnnotation, clusteredLookup.getDistributedLock());
            }
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

    /**
     * Get the most appropriate name for a bean. First checks the `@Clustered`
     * annotation key name property, then the bean EL name, then the bean class
     * name.
     *
     * @param bean       the bean to reference.
     * @param annotation the Clustered annotation to reference.
     * @throws IllegalArgumentException if no name can be found for the bean.
     */
    static <TT> String getBeanName(Bean<TT> bean, Clustered annotation) {
        try {
            return firstNonNull(annotation.keyName(), bean.getName(), bean.getBeanClass().getName());
        } catch (Throwable t) {
            throw new IllegalArgumentException("Could not find the name for bean: " + bean.toString(), t);
        }
    }

    static <TT> Clustered getAnnotation(BeanManager beanManager, Bean<TT> bean) {
        return getAnnotation(beanManager, bean.getBeanClass());
    }

    static <TT> Clustered getAnnotation(BeanManager beanManager, Class<?> clazz) {
        return CdiUtils.getAnnotation(beanManager, clazz, Clustered.class).get();
    }

    static String firstNonNull(String... items) {
        for (String i : items) {
            if (i != null && !i.trim().isEmpty()) {
                return i;
            }
        }
        throw new IllegalArgumentException("All elements were null.");
    }

    private static boolean lock(Clustered clusteredAnnotation, FencedLock lock) {
        if (clusteredAnnotation.lock() == DistributedLockType.LOCK) {
            if (!lock.isLockedByCurrentThread()) {
                lock.lock();
                return true;
            }
        }
        return false;
    }

    protected static void unlock(Clustered clusteredAnnotation, FencedLock lock) {
        if (clusteredAnnotation.lock() == DistributedLockType.LOCK) {
            try {
                if (lock.isLockedByCurrentThread()) {
                    lock.unlock();
                }
            } catch (CPSubsystemException | LockOwnershipLostException e) {
                log.log(Level.WARNING, "Distributed unlock failed", e);
            }
        }
    }
}
