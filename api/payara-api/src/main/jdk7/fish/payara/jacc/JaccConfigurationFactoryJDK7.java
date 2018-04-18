/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jacc;

import java.security.Policy;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;
import javax.servlet.ServletContextListener;

/**
 *  This class that allows to install
 * a local (per application) Jacc Provider (authorization module).
 * 
 * <p>
 * Note that this only works with Payara's default PolicyConfigurationFactory and not with any replacement
 * global PolicyConfigurationFactory. It may be possible to make such replacement PolicyConfigurationFactory
 * support installing local Jacc Providers by letting it implement this interface.
 * </p>
 * 
 * <p>
 * Installing a local Jacc provider is only supported for a web module, and thus not for an EJB module. A future
 * version of this interface may support EJB modules.
 * </p>
 * 
 * <p>
 * A local Jacc provider can be installed using a {@link ServletContextListener} as follows:
 * 
 * <pre>{@code
 * &#64;WebListener
 *public class JaccInstaller implements ServletContextListener {
 * 
 *  &#64;Override
 *  public void contextInitialized(ServletContextEvent sce) {
 *      JaccConfigurationFactoryPayara4.getJaccConfigurationFactory()
 *                              .registerContextProvider(
 *                                      getAppContextId(sce.getServletContext()),
 *                                      new TestPolicyConfigurationFactory(), 
 *                                      new TestPolicy());
 *  }
 * 
 *  private String getAppContextId(ServletContext servletContext) {
 *      return servletContext.getVirtualServerName() + " " + servletContext.getContextPath();
 *  }
 * 
 *}
 *}</pre>
 *</p>
 * 
 * @author Arjan Tijms
 *
 */
public class JaccConfigurationFactoryJDK7 {

    /**
     * This static method tries to obtain the global JaccConfigurationFactory, which means
     * looking up the global PolicyConfigurationFactory and testing to see if its a 
     * JaccConfigurationFactory.
     * 
     * @return the JaccConfigurationFactory
     * @throws IllegalStateException if the underlying PolicyConfigurationFactory could not be obtained
     * or the PolicyConfigurationFactory is not a JaccConfigurationFactory
     */
    public static JaccConfigurationFactory getJaccConfigurationFactory() {
        
        PolicyConfigurationFactory policyConfigurationFactory;
        try {
            policyConfigurationFactory = PolicyConfigurationFactory.getPolicyConfigurationFactory();
        } catch (ClassNotFoundException | PolicyContextException e) {
            throw new IllegalStateException(e);
        }
        
        if (!(policyConfigurationFactory instanceof JaccConfigurationFactory)) {
            throw new IllegalStateException(
                "PolicyConfigurationFactory " + policyConfigurationFactory.getClass().getName() +
                " is not an instance of " + JaccConfigurationFactory.class.getName()
            );
        }
        
        return (JaccConfigurationFactory) policyConfigurationFactory;
    }
    
}