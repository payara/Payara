/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
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
package org.glassfish.concurrent.cdi;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import java.util.Set;
import java.util.logging.Logger;
import org.glassfish.enterprise.concurrent.AsynchronousInterceptor;

/**
 * CDI Extension for Jakarta Concurrent implementation in Payara.
 *
 * @author Petr Aubrecht <petr@aubrecht.net>
 */
public class ConcurrentCDIExtension implements Extension {

    static final Logger log = Logger.getLogger(ConcurrentCDIExtension.class.getName());

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        log.fine("ConcurrentCDIExtension.beforeBeanDiscovery");
        // Add each of the Concurrent interceptors
        beforeBeanDiscovery.addInterceptorBinding(Asynchronous.class);
        AnnotatedType<AsynchronousInterceptor> asynchronousInterceptor
                = beanManager.createAnnotatedType(AsynchronousInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(asynchronousInterceptor, AsynchronousInterceptor.class.getName());
    }

    <T> void processAnnotatedType(@Observes @WithAnnotations({Asynchronous.class}) ProcessAnnotatedType<T> processAnnotatedType,
            BeanManager beanManager) throws Exception {
        log.fine("ConcurrentCDIExtension.processAnnotatedType");
        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();

        // Validate the Fault Tolerance annotations for each annotated method
        Set<AnnotatedMethod<? super T>> annotatedMethods = annotatedType.getMethods();
        for (AnnotatedMethod<?> annotatedMethod : annotatedMethods) {
            // TODO: check, if the method is annodated by Transactional, other than TxType.REQUIRES_NEW or TxType.NOT_SUPPORTED must result in java.lang.UnsupportedOperationException UnsupportedOperationException
//            throw new XYDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
//                    + " annotated with " + Asynchronous.class.getCanonicalName() + " does not return a Future.");
        }
    }
}
