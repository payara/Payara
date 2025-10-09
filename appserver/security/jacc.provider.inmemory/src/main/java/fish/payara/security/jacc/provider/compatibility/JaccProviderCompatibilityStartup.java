/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.security.jacc.provider.compatibility;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.JaccProvider;
import com.sun.enterprise.config.serverbeans.SecurityService;
import fish.payara.security.jacc.provider.PolicyConfigurationFactoryImpl;
import fish.payara.security.jacc.provider.PolicyProviderImpl;
import jakarta.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Startup service for jacc compatibility.
 *
 * This service checks if any domain.xml configuration has
 * jacc provider values incompatibles with Payara since
 * version 6.
 *
 * @author luisneto
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class JaccProviderCompatibilityStartup implements PostConstruct {

    public static final String OLD_POLICY_WRAPPER = "com.sun.enterprise.security.provider.PolicyWrapper";
    public static final String OLD_POLICY_CONFIGURATION_FACTORY_IMPL =
            "com.sun.enterprise.security.provider.PolicyConfigurationFactoryImpl";

    public static final String OLD_SIMPLE_POLICY_WRAPPER = "com.sun.enterprise.security.jacc.provider.SimplePolicyProvider";
    public static final String OLD_SIMPLE_POLICY_CONFIGURATION_FACTORY_IMPL =
            "com.sun.enterprise.security.jacc.provider.SimplePolicyConfigurationFactory";

    @Inject
    private Configs configs;

    @Override
    public void postConstruct() {
        for (Config config : configs.getConfig()) {
            SecurityService securityService = config.getSecurityService();
            List<JaccProvider> jaccProviders = securityService.getJaccProvider();
            for (JaccProvider jaccProvider : jaccProviders) {
                String policyProvider = jaccProvider.getPolicyProvider();
                String policyConfigurationFactoryProvider = jaccProvider.getPolicyConfigurationFactoryProvider();

                String newPolicyProvider = policyProvider;
                String newPolicyConfigurationFactoryProvider = policyConfigurationFactoryProvider;
                if (policyProvider.equals(OLD_POLICY_WRAPPER) ||
                        policyProvider.equals(OLD_SIMPLE_POLICY_WRAPPER)) {
                    newPolicyProvider = PolicyProviderImpl.class.getCanonicalName();
                }
                if (policyConfigurationFactoryProvider.equals(OLD_POLICY_CONFIGURATION_FACTORY_IMPL) ||
                        policyConfigurationFactoryProvider.equals(OLD_SIMPLE_POLICY_CONFIGURATION_FACTORY_IMPL)) {
                    newPolicyConfigurationFactoryProvider = PolicyConfigurationFactoryImpl.class.getCanonicalName();
                }

                if (!newPolicyProvider.equals(policyProvider) ||
                        !newPolicyConfigurationFactoryProvider.equals(policyConfigurationFactoryProvider)) {
                    renameJaccProvider(jaccProvider, newPolicyProvider, newPolicyConfigurationFactoryProvider);
                }
            }
        }
    }

    private void renameJaccProvider(JaccProvider jaccProvider, String policyProvider, String policyConfigurationFactoryProvider) {
        Logger logger = Logger.getAnonymousLogger();
        try {
            ConfigSupport.apply(new SingleConfigCode<JaccProvider>() {
                public Object run(JaccProvider param) throws PropertyVetoException, TransactionFailure {
                    logger.log(Level.INFO, "Renaming jacc policy provider from " +
                            param.getPolicyProvider() + " to " + policyProvider);
                    param.setPolicyProvider(policyProvider);

                    logger.log(Level.INFO, "Renaming jacc policy configuration factory provider from " +
                            param.getPolicyConfigurationFactoryProvider() + " to " + policyConfigurationFactoryProvider);
                    param.setPolicyConfigurationFactoryProvider(policyConfigurationFactoryProvider);

                    return param;
                }
            }, jaccProvider);
        } catch (TransactionFailure tf) {
            logger.log(Level.SEVERE, "Failure while renaming jacc provider ", tf);
            throw new RuntimeException(tf);
        }
    }

}