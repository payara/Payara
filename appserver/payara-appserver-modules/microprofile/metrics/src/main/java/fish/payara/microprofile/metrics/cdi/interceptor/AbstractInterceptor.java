/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package fish.payara.microprofile.metrics.cdi.interceptor;

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.glassfish.internal.api.Globals;

/* package-private */ abstract class AbstractInterceptor {

    @Inject
    @Intercepted
    protected Bean<?> bean;

    private MetricsService metricsService;
    private MetricsService.MetricsContext metricsContext;

    protected static <E extends Member & AnnotatedElement, M extends Metric> M apply(E element,
            Class<?> bean, AnnotationReader<?> reader, Class<M> metricType, 
                                                                                     ThreeFunctionResolver<MetricID, Class<M>, String,M> loader) {
        MetricID metricID = reader.metricID(bean, element);
        Timed timedAnnotation = element.getAnnotation(Timed.class);
        Counted countedAnnotation = element.getAnnotation(Counted.class);
        String scope = null;
        if(timedAnnotation != null) {
            scope = timedAnnotation.scope();
        }
        
        if(countedAnnotation != null) {
            scope = countedAnnotation.scope();
        }
        
        M metric = loader.apply(metricID, metricType, scope);
        if (metric == null) {
            throw new IllegalStateException(
                    "No " + metricType.getSimpleName() + " with ID [" + metricID + "] found in application registry");
        }
        return metric;
    }

    public <T extends Metric> T getMetric(MetricID metricID, Class<T> metricType, String scope) {
        initService();
        if(scope != null) {
            return metricsContext.getOrCreateRegistry(scope).getMetric(metricID, metricType);
        }
        return metricsContext.getApplicationRegistry().getMetric(metricID, metricType);
    }

    @AroundConstruct
    private Object constructorInvocation(InvocationContext context) throws Exception {
        return preInterceptor(context, context.getConstructor());
    }

    @AroundInvoke
    private Object methodInvocation(InvocationContext context) throws Exception {
        return preInterceptor(context, context.getMethod());
    }

    @AroundTimeout
    private Object timeoutInvocation(InvocationContext context) throws Exception {
        return preInterceptor(context, context.getMethod());
    }
    
    private <E extends Member & AnnotatedElement> Object preInterceptor(InvocationContext context, E element) throws Exception {
        initService();
        if (metricsService.isEnabled()) {
            //FIXME there is an issue here: the element does not correctly reflect the updated annotations
            // to be fully correct this would need to be wrapped and based on
            // Set<Annotation> bindings = (Set<Annotation>) invocationContext.getContextData().get("org.jboss.weld.interceptor.bindings");
            // to provide AnnotatedElement implementation
            return applyInterceptor(context, element);
        }
        return context.proceed();
    }

    private void initService() {
        if (metricsService == null) {
            metricsService = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);
            if (metricsService.isEnabled()) {
                metricsContext = metricsService.getContext(true);
            }
        }
    }

    protected abstract <E extends Member & AnnotatedElement> Object applyInterceptor(InvocationContext context, E element) throws Exception;



}
