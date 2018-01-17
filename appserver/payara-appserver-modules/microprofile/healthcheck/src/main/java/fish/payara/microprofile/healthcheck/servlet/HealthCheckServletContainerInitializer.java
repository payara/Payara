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
package fish.payara.microprofile.healthcheck.servlet;

import fish.payara.microprofile.healthcheck.HealthCheckService;
import java.util.Map;
import java.util.Set;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Andrew Pielage
 */
public class HealthCheckServletContainerInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        // Check if there is already a servlet for healthcheck
        Map<String, ? extends ServletRegistration> registrations = ctx.getServletRegistrations();
        for (ServletRegistration reg : registrations.values()) {
            if (reg.getClass().equals(HealthCheckServlet.class)) {
                return;
            }
        }
        
        // Register servlet
        ServletRegistration.Dynamic reg = ctx.addServlet("microprofile-healthcheck-servlet", HealthCheckServlet.class);
        reg.addMapping("/health");
        
        // Check for any Beans annotated with @Health
        BeanManager beanManager = null;
        try {
            beanManager = CDI.current().getBeanManager();
        } catch (Exception ex) {
            
        }
        
        if (beanManager != null) {
            HealthCheckService healthCheckService = Globals.getDefaultBaseServiceLocator().getService(
                    HealthCheckService.class);
            InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator().getService(
                    InvocationManager.class);
            
            Set<Bean<?>> beans = beanManager.getBeans(HealthCheck.class, new AnnotationLiteral<Health>() {});
            for (Bean<?> bean : beans) {
                healthCheckService.registerHealthCheck(invocationManager.getCurrentInvocation().getAppName(),
                        (HealthCheck) beanManager.getReference(
                                bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)));
            }
        }
    }
    
}
