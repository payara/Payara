/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.microprofile.healthcheck.HealthCheckType;
import static fish.payara.microprofile.healthcheck.HealthCheckType.HEALTH;
import static fish.payara.microprofile.healthcheck.HealthCheckType.LIVENESS;
import static fish.payara.microprofile.healthcheck.HealthCheckType.READINESS;
import fish.payara.microprofile.healthcheck.config.MetricsHealthCheckConfiguration;
import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import static javax.servlet.annotation.ServletSecurity.TransportGuarantee.CONFIDENTIAL;
import org.eclipse.microprofile.health.HealthCheck;
import org.glassfish.api.invocation.InvocationManager;
import static org.glassfish.common.util.StringHelper.isEmpty;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.weld.RootBeanDeploymentArchive;
import org.jboss.weld.Container;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Servlet Container Initializer that registers the HealthCheckServlet, as well
 * as the HealthChecks of a deployed application.
 *
 * @author Andrew Pielage
 */
public class HealthCheckServletContainerInitializer implements ServletContainerInitializer {

    private static final Logger LOGGER = Logger.getLogger(HealthCheckServletContainerInitializer.class.getName());

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        // Check if this context is the root one ("/")
        if (ctx.getContextPath().isEmpty()) {
            // Check if there is already a servlet for healthcheck
            Map<String, ? extends ServletRegistration> registrations = ctx.getServletRegistrations();
            MetricsHealthCheckConfiguration configuration = Globals.getDefaultHabitat().getService(MetricsHealthCheckConfiguration.class);

            if (!Boolean.parseBoolean(configuration.getEnabled())) {
                return; //MP Healthcheck disabled
            }

            for (ServletRegistration reg : registrations.values()) {
                if (reg.getClass().equals(HealthCheckServlet.class) || reg.getMappings().contains("/" + configuration.getEndpoint())) {
                    return;
                }
            }

            String virtualServers = configuration.getVirtualServers();
            if (!isEmpty(virtualServers)
                    && !asList(virtualServers.split(",")).contains(ctx.getVirtualServerName())) {
                return;
            }

            // Register servlet
            ServletRegistration.Dynamic reg = ctx.addServlet("microprofile-healthcheck-servlet", HealthCheckServlet.class);
            reg.addMapping("/" + configuration.getEndpoint() + "/*");
            if (Boolean.parseBoolean(configuration.getSecurityEnabled())) {
                String[] roles = configuration.getRoles().split(",");
                reg.setServletSecurity(new ServletSecurityElement(new HttpConstraintElement(CONFIDENTIAL, roles)));
                ctx.declareRoles(roles);
            }
        }

        // Get all BeanManagers for archives
        Collection<BeanManager> beanManagers = getBeanManagers();

        // Check for any Beans annotated with @Readiness, @Liveness or @Health
        for (BeanManager beanManager : beanManagers) {
            ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
            HealthCheckService healthCheckService = serviceLocator.getService(HealthCheckService.class);
            InvocationManager invocationManager = serviceLocator.getService(InvocationManager.class);
            String appName = invocationManager.getCurrentInvocation().getAppName();

            // For each bean annotated with @Readiness, @Liveness or @Health
            // and implementing the HealthCheck interface,
            // register it to the HealthCheckService along with the application name
            Set<Bean<?>> beans = new HashSet<>();

            beans.addAll(beanManager.getBeans(HealthCheck.class, READINESS.getLiteral()));
            beans.addAll(beanManager.getBeans(HealthCheck.class, LIVENESS.getLiteral()));
            beans.addAll(beanManager.getBeans(HealthCheck.class, HEALTH.getLiteral()));

            for (Bean<?> bean : beans) {
                HealthCheck healthCheck = (HealthCheck) beanManager.getReference(
                        bean,
                        HealthCheck.class,
                        beanManager.createCreationalContext(bean)
                );

                healthCheckService.registerHealthCheck(
                        appName, 
                        healthCheck, 
                        HealthCheckType.fromQualifiers(bean.getQualifiers())
                );
                healthCheckService.registerClassLoader(
                        appName,
                        healthCheck.getClass().getClassLoader()
                );

                LOGGER.log(INFO,
                        "Registered {0} as a HealthCheck for app: {1}",
                        new Object[]{bean.getBeanClass().getCanonicalName(), appName}
                );
            }
        }
    }
    
    /**
     * Gets all CDI bean managers that are load beans from the application.
     * @return an empty set if this is not a CDI application. A single entry for
     * a war or multiple if it is an ear.
     */
    private Collection<BeanManager> getBeanManagers() {
        Collection<BeanManager> beanManagers = new HashSet<>();
        try {
            Map<BeanDeploymentArchive, BeanManagerImpl> beanDeploymentArchives = Container.instance().beanDeploymentArchives();
            for (Map.Entry<BeanDeploymentArchive, BeanManagerImpl> entry : beanDeploymentArchives.entrySet()) {
                BeanDeploymentArchive beanDeploymentArchive = entry.getKey();
                if (beanDeploymentArchive instanceof RootBeanDeploymentArchive) {
                    RootBeanDeploymentArchive rootBeanDeploymentArchive = (RootBeanDeploymentArchive) beanDeploymentArchive;
                    ClassLoader moduleClassLoaderForBDA = rootBeanDeploymentArchive.getModuleClassLoaderForBDA();
                    //Get the bean managers that are used for a module, 
                    if (moduleClassLoaderForBDA.equals(Thread.currentThread().getContextClassLoader())) {
                        beanManagers.add(entry.getValue());
                    }
                }
            }
        } catch (IllegalStateException ignored) {
            //Not a CDI context
        }
        return beanManagers;
    }

}
