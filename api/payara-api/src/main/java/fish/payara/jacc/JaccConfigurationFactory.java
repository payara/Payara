/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;
import jakarta.servlet.ServletContextListener;

/**
 * An alternative interface for Payara's {@link PolicyConfigurationFactory} that allows to install
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
 *      JaccConfigurationFactory.getJaccConfigurationFactory()
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
public interface JaccConfigurationFactory {

    /**
     * This static method tries to obtain the global JaccConfigurationFactory, which means
     * looking up the global PolicyConfigurationFactory and testing to see if its a 
     * JaccConfigurationFactory.
     * 
     * @return the JaccConfigurationFactory
     * @throws IllegalStateException if the underlying PolicyConfigurationFactory could not be obtained
     * or the PolicyConfigurationFactory is not a JaccConfigurationFactory
     */
    static JaccConfigurationFactory getJaccConfigurationFactory() {
        
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
    
    /**
     * @see PolicyConfigurationFactory#getPolicyConfiguration(String, boolean)
     */
    PolicyConfiguration getPolicyConfiguration(String policyContextId, boolean remove) throws PolicyContextException;
    
    /**
     * @see PolicyConfigurationFactory#inService(String)
     */
    boolean inService(String policycontextId) throws PolicyContextException;
    
    /**
     * Registers a context (local) Jacc provider, consisting of its two elements.
     * 
     * <p>
     * See the JACC spec for the requirements and behavior of the {@link PolicyConfigurationFactory} 
     * and the {@link Policy}.
     * </p>
     * 
     * <p>
     * Note that this uses an <code>applicationContextId<code> for registration. This is a Servlet
     * based ID to identify the current application. It's defined as follows:
     * 
     * <pre>{@code
     * private String getAppContextId(ServletContext servletContext) {
     *     return servletContext.getVirtualServerName() + " " + servletContext.getContextPath();
     * }
     * }
     * </pre>
     * </p>
     * 
     * @param applicationContextId an ID identifying the application for which the Jacc provider is installed
     * @param factory the PolicyConfigurationFactory element of the Jacc Provider
     * @param policy the Policy element of the Jacc Provider
     * @throws SecurityException when the calling code has not been granted the "setPolicy" SecurityPermission.
     */
    void registerContextProvider(String applicationContextId, PolicyConfigurationFactory factory, Policy policy);
    
    /**
     * Gets the context JACC provider that was set by registerContextProvider.
     * 
     * <p>
     * Note that this uses the <code>policyContextId</code>, which is a JACC native ID instead of the
     * Servlet based application ID that's used for registering. The mapping from the Servlet based ID to
     * the JACC based ID is made known to the factory by the <code>addContextIdMapping</code> method. 
     * </p>
     * 
     * @param policyContextId the identifier of the JACC policy context
     * @return the bundled PolicyConfigurationFactory and Policy if previously set, otherwise null
     */
    ContextProvider getContextProviderByPolicyContextId(String policyContextId);
    
    /**
     * Removes any context JACC provider that was set by registerContextProvider.
     * 
     * <p>
     * Note that this uses the <code>policyContextId</code>, which is a JACC native ID instead of the
     * Servlet based application ID that's used for registering. The mapping from the Servlet based ID to
     * the JACC based ID is made known to the factory by the <code>addContextIdMapping</code> method. 
     * </p>
     * 
     * @param policyContextId the identifier of the JACC policy context
     * @return the bundled PolicyConfigurationFactory and Policy if previously set, otherwise null
     */
    ContextProvider removeContextProviderByPolicyContextId(String policyContextId);
    
    /**
     * Makes the mapping from the Servlet based context ID to the JACC based context ID
     * known to the factory. This method should normally only be called by the container. 
     * 
     * @param applicationContextId Servlet based identifier for an application context
     * @param policyContextId JACC based identifier for an application context
     */
    void addContextIdMapping(String applicationContextId, String policyContextId);
    
    /**
     * Removes the mapping from the Servlet based context ID to the JACC based context ID
     * known to the factory.
     * 
     * @param policyContextId JACC based identifier for an application context
     * @return true if one or more mappings were removed, false otherwise
     */
    boolean removeContextIdMappingByPolicyContextId(String policyContextId);
}