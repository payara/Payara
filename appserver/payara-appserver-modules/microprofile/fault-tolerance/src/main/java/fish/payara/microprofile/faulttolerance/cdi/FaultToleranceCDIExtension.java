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
package fish.payara.microprofile.faulttolerance.cdi;

import fish.payara.microprofile.faulttolerance.interceptors.FaultToleranceBehaviour;
import fish.payara.microprofile.faulttolerance.interceptors.FaultToleranceInterceptor;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * CDI Extension that does the setup for FT interceptor handling.
 * 
 * @author Andrew Pielage
 * @author Jan Bernitt
 */
public class FaultToleranceCDIExtension implements Extension {

    /**
     * The {@link FaultToleranceBehaviour} "instance" we use to dynamically mark methods at runtime that should be
     * handled by the {@link FaultToleranceInterceptor} that handles all of the FT annotations.
     */
    private static final Annotation MARKER = () -> FaultToleranceBehaviour.class;

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(FaultToleranceInterceptor.class), "MP-FT");
    }

    <T> void processAnnotatedType(@Observes @WithAnnotations({ Asynchronous.class, Bulkhead.class, CircuitBreaker.class,
        Fallback.class, Retry.class, Timeout.class }) ProcessAnnotatedType<T> processAnnotatedType, 
            BeanManager beanManager) throws Exception {
        validateAndMark(processAnnotatedType);
    }

    /**
     * Marks all {@link Method}s *affected* by FT annotation with the {@link FaultToleranceBehaviour} annotation which
     * is handled by the {@link FaultToleranceInterceptor} which processes the FT annotations.
     * 
     * @param processAnnotatedType type currently processed
     */
    private static <T> void validateAndMark(ProcessAnnotatedType<T> processAnnotatedType) {
        boolean markAllMethods = FaultToleranceUtils
                .isAnnotaetdWithFaultToleranceAnnotations(processAnnotatedType.getAnnotatedType());
        Class<?> targetClass = processAnnotatedType.getAnnotatedType().getJavaClass();
        for (AnnotatedMethodConfigurator<?> methodConfigurator : processAnnotatedType.configureAnnotatedType().methods()) {
            if (markAllMethods || FaultToleranceUtils
                    .isAnnotaetdWithFaultToleranceAnnotations(methodConfigurator.getAnnotated())) {
                FaultTolerancePolicy.asAnnotated(targetClass, methodConfigurator.getAnnotated().getJavaMember());
                methodConfigurator.add(MARKER);
            }
        }
    }
}
