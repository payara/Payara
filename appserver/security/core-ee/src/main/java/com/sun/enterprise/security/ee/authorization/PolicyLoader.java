/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright [2017-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee.authorization;

import com.sun.enterprise.config.serverbeans.JaccProvider;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.util.i18n.StringManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyFactory;
import java.util.logging.Logger;
import org.glassfish.exousia.modules.def.DefaultPolicyFactory;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import static com.sun.enterprise.security.SecurityLoggerInfo.*;
import static java.util.logging.Level.*;
import static org.glassfish.api.admin.ServerEnvironment.DEFAULT_INSTANCE_NAME;

/**
 * Loads the default JACC Policy Provider into the system.
 *
 * @author Harpreet Singh
 * @author Jyri J. Virkki
 */
@Service
@Singleton
public class PolicyLoader {
    
    private static final Logger LOGGER = SecurityLoggerInfo.getLogger();
    private static StringManager SM = StringManager.getManager(PolicyLoader.class);
    private static final StringManager STRING_MANAGER = StringManager.getManager(PolicyLoader.class);
    
    private static final String POLICY_PROVIDER = "jakarta.security.jacc.policy.provider";
    public static final String POLICY_CONF_FACTORY = "jakarta.security.jacc.PolicyConfigurationFactory.provider";
    private static final String POLICY_PROP_PREFIX = "com.sun.enterprise.jaccprovider.property.";

    @Inject
    @Named(DEFAULT_INSTANCE_NAME)
    private SecurityService securityService;

    @Inject
    private IterableProvider<JaccProvider> authorizationModules;
   
    private boolean isPolicyInstalled;

    /**
     * Attempts to install the policy-provider. The policy-provider element in domain.xml is consulted for the class to use. Note
     * that if the jakarta.security.jacc.policy.provider system property is set it will override the domain.xml configuration. This
     * will normally not be the case in S1AS.
     *
     */
    public void loadPolicy() {
        if (isPolicyInstalled) {
            LOGGER.fine("Policy already installed. Will not re-install.");
            return;
        }
        
        // Get configuration object for the JACC provider (which handles the policies)
        JaccProvider authorizationModule = getConfiguredJakartaAuthorizationModule();
        
        // Set config properties (see method comments)
        setPolicyConfigurationFactory(authorizationModule);
        

        // Get policy class name via the "normal" ways
        String policyClassName = System.getProperty(POLICY_PROVIDER);

        if (policyClassName == null) {
            LOGGER.log(WARNING, policyProviderConfigOverrideWarning, new String[]{POLICY_PROVIDER, policyClassName});
        } else if (authorizationModule != null) {
            policyClassName = authorizationModule.getPolicyProvider();
        }

        // Set the role mapper
        // TODO: replace with standard version
        if (System.getProperty("simple.jacc.provider.JACCRoleMapper.class") == null) {
            System.setProperty("simple.jacc.provider.JACCRoleMapper.class",
                    "com.sun.enterprise.security.ee.authorization.GlassfishRoleMapper");
        }

        // Now install the policy provider if one was identified
        if (policyClassName != null) {

            try {
                LOGGER.log(INFO, policyLoading, policyClassName);

                Policy policy = loadPolicy(policyClassName);
                PolicyFactory.setPolicyFactory(new DefaultPolicyFactory()); // TMP!!!
                PolicyFactory.getPolicyFactory().setPolicy(policy);
            } catch (Exception e) {
                LOGGER.log(SEVERE, policyInstallError, e.getLocalizedMessage());
                throw new RuntimeException(e);
            }

            // Success.
            LOGGER.fine("Policy set to: " + policyClassName);
            isPolicyInstalled = true;

        } else {
            // no value for policy provider found
            LOGGER.warning(policyNotLoadingWarning);
        }
    }

    private Policy loadPolicy(String javaPolicyClassName) throws ReflectiveOperationException, SecurityException {
        Object javaPolicyInstance =
                Thread.currentThread()
                        .getContextClassLoader()
                        .loadClass(javaPolicyClassName)
                        .getDeclaredConstructor()
                        .newInstance();

        if (!(javaPolicyInstance instanceof Policy)) {
            throw new RuntimeException(SM.getString("enterprise.security.plcyload.not14"));
        }

        return (Policy) javaPolicyInstance;
    }

    /**
     * Returns an authorization module object representing the jacc element from domain.xml which is configured in security-service.
     *
     * @return The config object or null on errors.
     *
     */
    private JaccProvider getConfiguredJakartaAuthorizationModule() {
        JaccProvider authorizationModule = null;
        try {
            String name = securityService.getJacc();
            authorizationModule = getAuthorizationModuleByName(name);
            if (authorizationModule == null) {
                LOGGER.log(WARNING, policyNoSuchName, name);
            }
        } catch (Exception e) {
            LOGGER.warning(SecurityLoggerInfo.policyReadingError);
            authorizationModule = null;
        }

        return authorizationModule;
    }

    private JaccProvider getAuthorizationModuleByName(String authorizationModuleName) {
        if (authorizationModules == null || authorizationModuleName == null) {
            return null;
        }

        for (JaccProvider authorizationModule : authorizationModules) {
            if (authorizationModule.getName().equals(authorizationModuleName)) {
                return authorizationModule;
            }
        }

        return null;
    }
    
    
    /**
     * Set internal properties based on domain.xml configuration.
     *
     * <P>
     * The POLICY_CONF_FACTORY property is consumed by the jacc-api as documented in JACC specification. It's value is set
     * here to the value given in domain.xml <i>unless</i> it is already set in which case the value is not modified.
     *
     * <P>
     * Then and properties associated with this jacc provider from domain.xml are set as internal properties prefixed with
     * POLICY_PROP_PREFIX. This is currently a workaround for bug 4846938. A cleaner interface should be adopted.
     *
     */
    private void setPolicyConfigurationFactory(JaccProvider authorizationModule) {
        if (authorizationModule == null) {
            return;
        }
        
        // Handle JACC-specified property for factory
        // TODO:V3 system property being read here
        String factoryProperty = System.getProperty(POLICY_CONF_FACTORY);
        if (factoryProperty != null) {
            // Warn user of override
            LOGGER.log(WARNING, policyFactoryOverride, new String[] { POLICY_CONF_FACTORY, factoryProperty});

        } else {
            // Use domain.xml value by setting the property to it
            String factoryFromConfig = authorizationModule.getPolicyConfigurationFactoryProvider();
            if (factoryFromConfig == null) {
                LOGGER.log(WARNING, policyConfigFactoryNotDefined);
            } else {
                System.setProperty(POLICY_CONF_FACTORY, factoryFromConfig);
            }
        }

        // Next, make properties of this authorization module available to provider
        for (Property authorizationProperty : authorizationModule.getProperty()) {
            String name = POLICY_PROP_PREFIX + authorizationProperty.getName();
            String value = authorizationProperty.getValue();
            LOGGER.log(FINEST, () -> "PolicyLoader set [" + name + "] to [" + value + "]");
            
            System.setProperty(name, value);
        }
    }
    
}
