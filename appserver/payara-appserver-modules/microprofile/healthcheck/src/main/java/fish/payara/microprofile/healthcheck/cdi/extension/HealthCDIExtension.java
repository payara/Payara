/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2020-2023 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.microprofile.healthcheck.cdi.extension;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import fish.payara.microprofile.healthcheck.HealthCheckService;
import fish.payara.microprofile.healthcheck.HealthCheckType;

public class HealthCDIExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(HealthCDIExtension.class.getName());

    private final HealthCheckService healthService;
    private final String appName;

    private final Set<Bean<?>> healthCheckBeans;

    public HealthCDIExtension() {
        final ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
        final InvocationManager invocationManager = serviceLocator.getService(InvocationManager.class);
        this.appName = invocationManager.getCurrentInvocation().getAppName();
        this.healthService = serviceLocator.getService(HealthCheckService.class);
        this.healthCheckBeans = new HashSet<>();
    }

    void processBean(@Observes ProcessBean<?> event) {
        Bean<?> bean = event.getBean();
        if (bean != null) {
            Set<Annotation> annotations = bean.getQualifiers();
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (Readiness.class.equals(annotationType)
                        || Liveness.class.equals(annotationType)
                        || Startup.class.equals(annotationType)) {
                    this.healthCheckBeans.add(event.getBean());
                }
            }
        }
    }

    void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init, BeanManager beanManager) {
        Iterator<Bean<?>> beanIterator = healthCheckBeans.iterator();
        while (beanIterator.hasNext()) {
            registerHealthCheck(beanIterator.next(), beanManager);
            beanIterator.remove();
        }
    }

    private void registerHealthCheck(Bean<?> bean, BeanManager beanManager) {
        HealthCheck healthCheck = (HealthCheck) beanManager.getReference(
                bean,
                HealthCheck.class,
                beanManager.createCreationalContext(bean)
        );

        healthService.registerHealthCheck(
                appName,
                healthCheck,
                HealthCheckType.fromQualifiers(bean.getQualifiers())
        );
        healthService.registerClassLoader(
                appName,
                healthCheck.getClass().getClassLoader()
        );

        LOGGER.log(Level.INFO,
                "Registered {0} as a HealthCheck for app: {1}",
                new Object[]{bean.getBeanClass().getCanonicalName(), appName}
        );
    }

}
