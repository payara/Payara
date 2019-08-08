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

import com.hazelcast.core.IAtomicLong;
import fish.payara.cluster.Clustered;
import fish.payara.cluster.DistributedLockType;
import static fish.payara.micro.cdi.extension.cluster.ClusterScopeContext.getAnnotation;
import static fish.payara.micro.cdi.extension.cluster.ClusterScopeContext.getBeanName;
import fish.payara.micro.cdi.extension.cluster.annotations.ClusterScoped;
import fish.payara.micro.cdi.extension.cluster.annotations.ClusterScopedIntercepted;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 * Intercepts every method call to refresh the cluster
 *
 * @author lprimak
 */
@Interceptor @ClusterScopedIntercepted @Priority(Interceptor.Priority.PLATFORM_AFTER)
public class ClusterScopedInterceptor implements Serializable {
    private final BeanManager beanManager = CDI.current().getBeanManager();
    private transient ClusteredSingletonLookupImpl clusteredLookup;
    private static final long serialVersionUID = 1L;


    public ClusterScopedInterceptor() {
        init();
    }

    @AroundInvoke
    public Object lockAndRefresh(InvocationContext invocationContext) throws Exception {
        Class<?> beanClass = invocationContext.getMethod().getDeclaringClass();
        Clustered clusteredAnnotation = getAnnotation(beanManager, beanClass);
        try {
            lock(beanClass, clusteredAnnotation);
            return invocationContext.proceed();
        }
        finally {
            refresh(beanClass);
            unlock(beanClass, clusteredAnnotation);
        }
    }

    @PostConstruct
    Object postConstruct(InvocationContext invocationContext) throws Exception {
        Class<?> beanClass = invocationContext.getTarget().getClass().getSuperclass();
        clusteredLookup.setClusteredSessionKey(beanClass);
        clusteredLookup.getClusteredUsageCount().incrementAndGet();
        return invocationContext.proceed();
    }

    @PreDestroy
    Object preDestroy(InvocationContext invocationContext) throws Exception {
        Class<?> beanClass = invocationContext.getTarget().getClass().getSuperclass();
        Clustered clusteredAnnotation = getAnnotation(beanManager, beanClass);
        clusteredLookup.setClusteredSessionKey(beanClass);
        IAtomicLong count = clusteredLookup.getClusteredUsageCount();
        if (count.decrementAndGet() <= 0) {
            clusteredLookup.getClusteredSingletonMap().delete(clusteredLookup.getClusteredSessionKey());
            count.destroy();
        } else if (!clusteredAnnotation.callPreDestoyOnDetach()) {
            return null;
        }

        return invocationContext.proceed();
    }

    private void lock(Class<?> beanClass, Clustered clusteredAnnotation) {
        if (clusteredAnnotation.lock() == DistributedLockType.LOCK) {
            clusteredLookup.setClusteredSessionKey(beanClass);
            clusteredLookup.getDistributedLock().lock();
        }
    }

    private void unlock(Class<?> beanClass, Clustered clusteredAnnotation) {
        if (clusteredAnnotation.lock() == DistributedLockType.LOCK) {
            clusteredLookup.setClusteredSessionKey(beanClass);
            clusteredLookup.getDistributedLock().unlock();
        }
    }

    private void refresh(Class<?> beanClass) {
        Set<Bean<?>> managedBeans = beanManager.getBeans(beanClass);
        if (managedBeans.size() > 1) {
            throw new IllegalArgumentException("Multiple beans found for " + beanClass);
        }
        Bean<?> bean = managedBeans.iterator().next();
        String beanName = getBeanName(bean, getAnnotation(beanManager, bean));
        Context ctx = beanManager.getContext(ClusterScoped.class);
        clusteredLookup.getClusteredSingletonMap().put(beanName, ctx.get(bean));
    }

    private void init() {
        String moduleName = Globals.getDefaultHabitat().getService(InvocationManager.class).getCurrentInvocation().getAppName();
        clusteredLookup = new ClusteredSingletonLookupImpl(beanManager, moduleName);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }
}
