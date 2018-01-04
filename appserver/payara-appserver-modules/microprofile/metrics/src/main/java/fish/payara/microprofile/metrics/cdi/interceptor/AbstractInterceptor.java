/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.cdi.interceptor;

import fish.payara.microprofile.metrics.MetricHelper;
import fish.payara.microprofile.metrics.cdi.MetricResolver;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.InvocationContext;
import org.eclipse.microprofile.metrics.MetricRegistry;

public abstract class AbstractInterceptor {

    @Inject
    protected MetricRegistry registry;

    @Inject
    protected MetricResolver resolver;
    
    @Inject
    @Intercepted
    protected Bean<?> bean;

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
        if (MetricHelper.isMetricEnabled()) {
            return applyInterceptor(context, element);
        } else {
            return context.proceed();
        }
    }

    protected abstract <E extends Member & AnnotatedElement> Object applyInterceptor(InvocationContext context, E element) throws Exception;

}
