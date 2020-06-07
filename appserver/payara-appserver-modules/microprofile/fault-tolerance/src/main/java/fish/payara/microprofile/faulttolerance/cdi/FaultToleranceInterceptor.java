/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.cdi;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.glassfish.internal.api.Globals;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceService;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.service.Stereotypes;

@Interceptor
@FaultTolerance
@Priority(Interceptor.Priority.PLATFORM_AFTER + 15)
public class FaultToleranceInterceptor implements Stereotypes, Serializable {

    private static final Logger logger = Logger.getLogger(FaultToleranceInterceptor.class.getName());

    @Inject
    private BeanManager beanManager;

    @Inject
    private Instance<RequestContextController> requestContextControllerInstance;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        try {
            FaultToleranceService env =
                    Globals.getDefaultBaseServiceLocator().getService(FaultToleranceService.class);
            AtomicReference<FaultToleranceConfig> lazyConfig = new AtomicReference<>();
            Supplier<FaultToleranceConfig> configSupplier = () -> //
                lazyConfig.updateAndGet(value -> value != null ? value : env.getConfig(context, this));
            FaultTolerancePolicy policy = FaultTolerancePolicy.get(context, configSupplier);
            if (policy.isPresent) {
                return policy.proceed(context, () -> env.getMethodContext(context, policy, getRequestContextController()));
            }
        } catch (FaultToleranceDefinitionException e) {
            logger.log(Level.SEVERE, "Effective FT policy contains illegal values, fault tolerance cannot be applied,"
                    + " falling back to plain method invocation.", e);
            // fall-through to normal proceed
        }
        return context.proceed();
    }

    private RequestContextController getRequestContextController() {
        return requestContextControllerInstance.isResolvable() ? requestContextControllerInstance.get() : null;
    }

    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType) {
        return beanManager.isStereotype(annotationType);
    }

    @Override
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype) {
        return beanManager.getStereotypeDefinition(stereotype);
    }

}

