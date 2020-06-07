/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.invoke;

import fish.payara.ejb.http.admin.EjbInvokerConfiguration;
import fish.payara.ejb.http.endpoint.EjbOverHttpApplication;
import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import static javax.servlet.annotation.ServletSecurity.TransportGuarantee.CONFIDENTIAL;
import org.glassfish.internal.api.Globals;
import static javax.servlet.http.HttpServletRequest.FORM_AUTH;
import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;

/**
 *
 * @author Gaurav Gupta
 */
public class EjbInvokerContainerInitializer  implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext ctx) throws ServletException {

        EjbInvokerConfiguration config = Globals.getDefaultBaseServiceLocator()
                .getService(EjbInvokerConfiguration.class);
        String endpoint = ctx.getContextPath();
        if(endpoint.startsWith("/")){
            endpoint = endpoint.substring(1);
        }
        if (!config.getEndpoint().equals(endpoint)) {
            return;
        }

        if (Boolean.parseBoolean(config.getSecurityEnabled())) {
            String[] roles = config.getRoles().split(",");
            ServletRegistration.Dynamic reg = (ServletRegistration.Dynamic) ctx.getServletRegistration(EjbOverHttpApplication.class.getName());
            if (reg == null) {
                new JerseyServletContainerInitializer().onStartup(new HashSet<>(asList(EjbOverHttpApplication.class)), ctx);
                reg = (ServletRegistration.Dynamic) ctx.getServletRegistration(EjbOverHttpApplication.class.getName());
            }
            reg.setServletSecurity(new ServletSecurityElement(new HttpConstraintElement(CONFIDENTIAL, roles)));
            if (FORM_AUTH.equals(config.getAuthType())) {
                ServletRegistration defaultRegistration = ctx.getServletRegistration("default");
                defaultRegistration.addMapping("/login.xhtml");
                defaultRegistration.addMapping("/error.xhtml");
            }
            ctx.declareRoles(roles);
        }
        
    }

}
